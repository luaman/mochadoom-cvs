package rr;

import static rr.line_t.*;
import static data.Defines.ANGLETOSKYSHIFT;
import static data.Defines.FF_FRAMEMASK;
import static data.Defines.FF_FULLBRIGHT;
import static data.Defines.NF_SUBSECTOR;
import static data.Defines.NUMCOLORMAPS;
import static data.Defines.PU_CACHE;
import static data.Defines.PU_STATIC;
import static data.Defines.SIL_BOTH;
import static data.Defines.SIL_BOTTOM;
import static data.Defines.SIL_TOP;
import static data.Defines.pw_invisibility;
import static data.Limits.MAXHEIGHT;
import static data.Limits.MAXSEGS;
import static data.Limits.MAXVISSPRITES;
import static data.Limits.MAXWIDTH;
import static data.Limits.MAX_SPRITEFRAMES;
import static data.Tables.ANG180;
import static data.Tables.ANG270;
import static data.Tables.ANG45;
import static data.Tables.ANG90;
import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.BITS32;
import static data.Tables.DBITS;
import static data.Tables.FINEANGLES;
import static data.Tables.QUARTERMARK;
import static data.Tables.SlopeDiv;
import static data.Tables.addAngles;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.Tables.finetangent;
import static data.Tables.tantoangle;
import static doom.player_t.NUMPSPRITES;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static p.mobj_t.MF_SHADOW;
import static p.mobj_t.MF_TRANSLATION;
import static p.mobj_t.MF_TRANSSHIFT;
import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import m.FixedFloat;
import m.IDoomMenu;
import m.MenuMisc;
import p.AbstractLevelLoader;
import p.UnifiedGameMap;
import p.mobj_t;
import p.pspdef_t;
import rr.drawfuns.ColVars;
import rr.drawfuns.DoomColumnFunction;
import rr.drawfuns.DoomSpanFunction;
import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawSpanLow;
import rr.drawfuns.R_DrawSpanUnrolled;
import rr.drawfuns.SpanVars;
import i.DoomStatusAware;
import i.IDoomSystem;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import v.IVideoScale;
import v.IVideoScaleAware;
import w.IWadLoader;
import w.lumpinfo_t;
import w.name8;
import data.Defines;
import data.Limits;
import data.Tables;
import doom.DoomMain;
import doom.DoomStatus;
import doom.IDoomGameNetworking;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;
import rr.drawfuns.*;

/**
 * Most shared -essential- status information, methods and classes related to
 * the software rendering subsystem are found here, shared between the various
 * implementations of the Doom's renderer.
 * 
 * Not the cleanest or more OO way possible, but still a good way to avoid
 * duplicating common code.
 * 
 * Some stuff like Texture, Flat and Sprite management are also found -or at
 * least implemented temporarily- here, until a cleaner split can be made. This
 * is a kind of "Jack of all trades" class, but hopefully not for long.
 * 
 * @author velktron
 * 
 */

public abstract class RendererState implements Renderer<byte[]>,
		SpriteManager, ILimitResettable, IGetColumn {

	protected static final boolean DEBUG = false;
	protected static final boolean DEBUG2 = false;
	// HACK: An all zeroes array used for fast clearing of certain visplanes.
	protected int[] BLANKCACHEDHEIGHT;
	protected short[] BLANKFLOORCLIP;
	protected short[] BLANKCEILINGCLIP;
	
	// ////////////////////////////// STATUS ////////////////

	protected DoomMain DM;
	protected IDoomGameNetworking DGN;
	protected AbstractLevelLoader LL;
	protected IWadLoader W;
	protected SegDrawer MySegs;
	protected IDoomMenu Menu;
	protected BSP MyBSP;
	protected PlaneDrawer MyPlanes;
	protected Things MyThings;
	protected DoomVideoRenderer<byte[]> V;
	protected UnifiedGameMap P;
	protected IDoomSystem I;
	protected TextureManager TexMan;

	// Found in draw_c. Only ever used in renderer.

	public int viewwindowx;
	public int viewwindowy;
	public int viewwidth;
	public int viewheight;

	// MAES: outsiders have no business peeking into this.
	// Or...well..maybe they do. It's only used to center the "pause" X
	// position.
	// TODO: get rid of this?
	public int scaledviewwidth;

	// The only reason to query scaledviewwidth from outside the renderer, is
	// this.
	public boolean isFullHeight() {
		return (viewheight == SCREENHEIGHT);
	}

	public boolean isFullWidth() {
		return (scaledviewwidth == SCREENWIDTH);
	}

	public boolean isFullScreen() {
		return isFullWidth() && isFullHeight();
	}



	// /// FROM PLANES //////

	// initially.
	protected int MAXVISPLANES = Limits.MAXVISPLANES;
	protected int MAXDRAWSEGS = Limits.MAXDRAWSEGS;

	/**
	 * Clip values are the solid pixel bounding the range. floorclip starts out
	 * SCREENHEIGHT ceilingclip starts out -1
	 */
	protected short[] floorclip, ceilingclip;

	/** visplane_t*, treat as indexes into visplanes */
	protected int lastvisplane, floorplane, ceilingplane;

	protected visplane_t[] visplanes = new visplane_t[MAXVISPLANES];

	/**
	 * openings is supposed to show where "openings" in visplanes start and end
	 * e.g. due to sprites, windows etc.
	 */
	protected short[] openings;
	/** Maes: this is supposed to be a pointer inside openings */
	protected int lastopening;// =new Short((short) 0);

	/**
	 * Increment every time a check is made For some reason, this needs to be
	 * visible even by enemies thinking :-S
	 */
	protected int validcount = 1;

	protected int MAXOPENINGS;

	/** Who can set this? A: The Menu. */
	protected boolean setsizeneeded;
	protected int setblocks;
	protected int setdetail;

	/**
	 * R_SetViewSize Do not really change anything here, because it might be in
	 * the middle of a refresh. The change will take effect next refresh.
	 * 
	 * @param blocks
	 *            11 is full screen, 9 default.
	 * @param detail
	 *            0= high, 1 =low.
	 */

	public void SetViewSize(int blocks, int detail) {
		// System.out.println("SetViewSize");
		setsizeneeded = true;
		setblocks = blocks;
		setdetail = detail;
	}

	/**
	 * R_SetupFrame
	 */
	public void SetupFrame(player_t player) {
		int i;

		viewplayer = player;
		viewx = player.mo.x;
		viewy = player.mo.y;
		// viewangle = addAngles(player.mo.angle , viewangleoffset);
		viewangle = player.mo.angle & BITS32;
		extralight = player.extralight;

		viewz = player.viewz;
		lookdir=(int) player.lookdir;
		int tempCentery;
		
		// MAES: hacks based on Heretic. Weapon movement needs to be compensated
		if (setblocks==11)
			tempCentery= (viewheight/2)+(int)(lookdir*SCREEN_MUL*setblocks)/11;
		else
			tempCentery =  (viewheight/2)+(int)(lookdir*SCREEN_MUL*setblocks)/10;
		
		if(centery != tempCentery)
		{			
		    centery = tempCentery;
			centeryfrac = centery<<FRACBITS;
			int yslope[]=MyPlanes.getYslope();
			for(i = 0; i < viewheight; i++)
			{
				yslope[i] = FixedDiv ((viewwidth<<detailshift)/2*FRACUNIT,
					Math.abs(((i-centery)<<FRACBITS)+FRACUNIT/2));
			}
			
			skydcvars.centery=maskedcvars.centery=dcvars.centery=centery;
		}
		
		viewsin = Tables.finesine(viewangle);
		viewcos = Tables.finecosine(viewangle);

		sscount = 0;

		if (player.fixedcolormap != 0) {
			fixedcolormap = colormaps[player.fixedcolormap];
			// Offset by fixedcolomap
			// pfixedcolormap =player.fixedcolormap*256;

			walllights = scalelightfixed;

			for (i = 0; i < MAXLIGHTSCALE; i++)
				scalelightfixed[i] = fixedcolormap;
		} else
			fixedcolormap = null;

		framecount++;
		validcount++;
	}

	protected int lookdir;
	
	/**
	 * R_SetupFrame for a particular actor.
	 * 
	 */
	public void SetupFrame(mobj_t actor) {

		// viewplayer = player;
		viewx = actor.x;
		viewy = actor.y;
		// viewangle = addAngles(player.mo.angle , viewangleoffset);
		viewangle = actor.angle & BITS32;
		// extralight = actor.extralight;

		viewz = actor.z + actor.height;

		viewsin = finesine(viewangle);
		viewcos = finecosine(viewangle);

		sscount = 0;

		framecount++;
		validcount++;
	}

	/**
	 * R_FindPlane
	 * 
	 * Checks whether a visplane with the specified height, picnum and light
	 * level exists among those already created. This looks like a half-assed
	 * attempt at reusing already existing visplanes, rather than creating new
	 * ones. The tricky part is understanding what happens if one DOESN'T exist.
	 * Called only from within R_Subsector (so while we're still trasversing
	 * stuff).
	 * 
	 * @param height
	 *            (fixed_t)
	 * @param picnum
	 * @param lightlevel
	 * @return was visplane_t*, returns index into visplanes[]
	 */

	protected final int FindPlane(int height, int picnum, int lightlevel) {
		// System.out.println("\tChecking for visplane merging...");
		int check = 0; // visplane_t*
		visplane_t chk = null;

		if (picnum == TexMan.getSkyFlatNum()) {
			height = 0; // all skys map together
			lightlevel = 0;
		}

		chk = visplanes[0];

		// Find visplane with the desired attributes
		for (check = 0; check < lastvisplane; check++) {

			chk = visplanes[check];
			if (height == chk.height && picnum == chk.picnum
					&& lightlevel == chk.lightlevel) {
				// Found a visplane with the desired specs.
				break;
			}
		}

		if (check < lastvisplane) {
			return check;
		}

		// Found a visplane, but we can't add anymore.
		if (lastvisplane == visplanes.length) {
			// I.Error ("R_FindPlane: no more visplanes");
			ResizeVisplanes();

		}

		/*
		 * FIXED: we need to add this post-fix here because of the way the
		 * original was structured (pointer hacks, too lengthy to explain). We
		 * need to make sure that when no visplane is found a "failed check"
		 * will actually result in a pointer to the next "free" visplane, and
		 * that we always have a valid pointer to visplane 0, even if the loop
		 * never ran. This fixes the "blinking visplane bug", which manifested
		 * itself when sector lighting effects changed the light level
		 */

		chk = visplanes[check];
		// Add a visplane
		lastvisplane++;
		chk.height = height;
		chk.picnum = picnum;
		chk.lightlevel = lightlevel;
		chk.minx = SCREENWIDTH;
		chk.maxx = -1;
		// memset (chk.top,0xff,sizeof(chk.top));
		chk.clearTop();

		return check;
	}

	/**
	 * A hashtable used to retrieve planes with particular attributes faster
	 * -hopefully-. The planes are still stored in the visplane array for
	 * convenience, but we can search them in the hashtable too -as a bonus, we
	 * can reuse previously created planes that match newer ones-.
	 */
	Hashtable<visplane_t, Integer> planehash = new Hashtable<visplane_t, Integer>(
			128);
	visplane_t check = new visplane_t();

	protected final int FindPlane2(int height, int picnum, int lightlevel) {
		// System.out.println("\tChecking for visplane merging...");
		// int check=0; // visplane_t*
		visplane_t chk = null;
		Integer checknum;

		if (picnum == TexMan.getSkyFlatNum()) {
			height = 0; // all skys map together
			lightlevel = 0;
		}

		// Try and find this.
		check.lightlevel = lightlevel;
		check.picnum = picnum;
		check.height = height;
		check.updateHashCode();

		/*
		 * chk=visplanes[0];
		 * 
		 * // Find visplane with the desired attributes for (check=0;
		 * check<lastvisplane; check++) {
		 * 
		 * chk=visplanes[check]; if (height == chk.height && picnum ==
		 * chk.picnum && lightlevel ==chk.lightlevel) { // Found a visplane with
		 * the desired specs. break; } }
		 */

		checknum = planehash.get(check);

		// Something found, get it.

		if (!(checknum == null)) {

			// Visplane exists and is within those allocated in the current tic.
			if (checknum < lastvisplane) {
				return checknum;
			}

			// Found a visplane, but we can't add anymore.
			// Resize right away. This shouldn't take too long.
			if (lastvisplane == MAXVISPLANES) {
				// I.Error ("R_FindPlane: no more visplanes");
				ResizeVisplanes();
			}
		}

		/*
		 * FIXED: we need to add this post-fix here because of the way the
		 * original was structured (pointer hacks, too lengthy to explain). We
		 * need to make sure that when no visplane is found a "failed check"
		 * will actually result in a pointer to the next "free" visplane, and
		 * that we always have a valid pointer to visplane 0, even if the loop
		 * never ran. This fixes the "blinking visplane bug", which manifested
		 * itself when sector lighting effects changed the light level
		 */

		// We found a visplane (possibly one allocated on a previous tic)
		// but we can't link directly to it, we need to copy its data
		// around.

		checknum = new Integer(Math.max(0, lastvisplane));

		chk = visplanes[checknum];
		// Add a visplane
		lastvisplane++;
		chk.height = height;
		chk.picnum = picnum;
		chk.lightlevel = lightlevel;
		chk.minx = SCREENWIDTH;
		chk.maxx = -1;
		chk.updateHashCode();
		planehash.put(chk, checknum);
		// memset (chk.top,0xff,sizeof(chk.top));
		chk.clearTop();

		return checknum;
	}

	public RendererState(DoomStatus DS){
		  this.updateStatus(DS);
		
		  // These don't change between implementations, yet.
		  this.MyThings=new Things();
		  this.MyBSP=new BSP();
		
		  // Set rendering functions only after screen sizes 
		  // and stuff have been set.
		
		}
	
	protected final void ResizeVisplanes() {
		visplane_t[] tmp = new visplane_t[visplanes.length * 2];
		System.arraycopy(visplanes, 0, tmp, 0, visplanes.length);

		C2JUtils.initArrayOfObjects(tmp, visplanes.length, tmp.length);

		// Bye bye, old visplanes.
		visplanes = tmp;

		System.out.println("Visplane buffer resized. Actual capacity "
				+ visplanes.length);
	}

	protected final void ResizeDrawsegs() {
		drawseg_t[] tmp = new drawseg_t[drawsegs.length * 2];
		System.arraycopy(drawsegs, 0, tmp, 0, drawsegs.length);

		C2JUtils.initArrayOfObjects(tmp, drawsegs.length, tmp.length);

		// Bye bye, old drawsegs.
		drawsegs = tmp;
		// MAXDRAWSEGS*=2;

		System.out.println("Drawseg buffer resized. Actual capacity "
				+ drawsegs.length);
	}

	@Override
	public void updateStatus(DoomStatus DC) {
		this.DM = DC.DM;
		this.DGN = DC.DGN;
		this.LL = DC.LL;
		this.W = DC.W;
		this.P = DC.P;
		// We must also connect screen to V. Don't forget it. Do it in Init(), OK?      
	    this.V=(DoomVideoRenderer<byte[]>) DC.V;
	    this.I=DC.I;
	}

	// ////////////////////////////// THINGS ////////////////////////////////

	// MAES: Shit taken from things

	protected int firstspritelump;
	protected int lastspritelump;
	protected int numspritelumps;

	//
	// Sprite rotation 0 is facing the viewer,
	// rotation 1 is one angle turn CLOCKWISE around the axis.
	// This is not the same as the angle,
	// which increases counter clockwise (protractor).
	// There was a lot of stuff grabbed wrong, so I changed it...
	//

	/** fixed_t */
	protected int pspritescale, pspriteiscale, pspritexscale, pspriteyscale,
			skyscale;

	protected byte[][] spritelights;

	protected int WEAPONADJUST;
	protected int BOBADJUST;

	/**
	 * constant arrays used for psprite clipping and initializing clipping
	 */
	protected short[] negonearray; // MAES: in scaling
	protected short[] screenheightarray;// MAES: in scaling

	protected int spryscale;
	protected int sprtopscreen;
	protected short[] mfloorclip;
	protected int p_mfloorclip;
	protected short[] mceilingclip;
	protected int p_mceilingclip;

	//
	// INITIALIZATION FUNCTIONS
	//

	/**
	 * variables used to look up and range check thing_t sprites patches
	 */
	private spritedef_t[] sprites;
	protected int numsprites;

	/*
	 * variables used to look up and range check thing_t sprites patches
	 */

	/**
	 * Temporarily contains the frames of a given sprite before they are
	 * registered with the rendering system. Apparently, a maximum of 29 frames
	 * per sprite is allowed.
	 */
	protected spriteframe_t[] sprtemp = new spriteframe_t[29];
	protected int maxframe;
	protected String spritename;

	/** Refresh of things, i.e. objects represented by sprites. */

	protected final class Things implements IVideoScaleAware, ILimitResettable {

		private static final int MAX_SPRITE_FRAMES = 29; 
		
		public Things() {
			sprtemp = new spriteframe_t[MAX_SPRITEFRAMES];
			C2JUtils.initArrayOfObjects(sprtemp);
			vissprites = new vissprite_t[MAXVISSPRITES];
			C2JUtils.initArrayOfObjects(vissprites);
			vsprsortedhead = new vissprite_t();
			unsorted = new vissprite_t();
			// ts=new ThreadSort<vissprite_t>(vissprites);
		}

		public void resetLimits() {
			vissprite_t[] tmp = new vissprite_t[MAXVISSPRITES];
			System.arraycopy(vissprites, 0, tmp, 0, MAXVISSPRITES);

			// Now, that was quite a haircut!.
			vissprites = tmp;

			// System.out.println("Vispprite buffer cut back to original limit of "+MAXVISSPRITES);
		}
		
		

		/////////////// VIDEO SCALE STUFF /////////////////////

		protected int SCREENWIDTH;
		protected int SCREENHEIGHT;
		protected IVideoScale vs;

		@Override
		public void setVideoScale(IVideoScale vs) {
			this.vs = vs;
		}

		@Override
		public void initScaling() {
			this.SCREENHEIGHT = vs.getScreenHeight();
			this.SCREENWIDTH = vs.getScreenWidth();

			// Pre-scale stuff.

			clipbot = new short[SCREENWIDTH];
			cliptop = new short[SCREENWIDTH];
		}

		// //////////////////////////////////////////////////////////////////////////

		/**
		 * R_InstallSpriteLump Local function for R_InitSprites.
		 */

		protected final void InstallSpriteLump(int lump, int frame,
				int rotation, boolean flipped) {

			// System.out.println("Trying to install "+spritename+" Frame "+
			// (char)('A'+frame)+" rot "+(rotation)
			// +" . Should have rotations: "+sprtemp[frame].rotate);
			int r;

			if (frame >= 29 || rotation > 8)
				I.Error("R_InstallSpriteLump: Bad frame characters in lump %i",
						lump);

			if ((int) frame > maxframe)
				maxframe = frame;

			/*
			 * A rotation value of 0 means that we are either checking the first
			 * frame of a sprite that HAS rotations, or something that has no
			 * rotations at all. The value of rotate doesn't really help us
			 * discern here, unless set to "false" a-priori...which can't happen
			 * ?!
			 */

			if (rotation == 0) {
				/*
				 * MAES: notice how comparisons are done with strict literals
				 * (true and false) which are actually defined to be 0 and 1,
				 * rather than assuming that true is "any nonzero value". This
				 * happens because rotate's value could be -1 at this point (!),
				 * if a series of circumstances occur. Therefore it's actually a
				 * "tri-state", and the comparison 0==false and
				 * "anything else"==true was not good enough in this case. A
				 * value of -1 doesn't yield either true or false here.
				 */

				// the lump should be used for all rotations
				if (sprtemp[frame].rotate == 0) {
					/*
					 * MAES: Explanation: we stumbled upon this lump before, and
					 * decided that this frame should have no more rotations,
					 * hence we found an error and we bomb everything.
					 */
					I.Error("R_InitSprites: Sprite %s frame %c has multiple rot=0 lump",
							spritename, 'A' + frame);
				}

				// This should NEVER happen!
				if (sprtemp[frame].rotate == 1) {
					/*
					 * MAES: This can only happen if we decided that a sprite's
					 * frame was already decided to have rotations, but now we
					 * stumble upon another occurence of "rotation 0". Or if you
					 * use naive true/false evaluation for .rotate ( -1 is also
					 * an admissible value).
					 */
					I.Error("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump",
							spritename, 'A' + frame);
				}

				// Rotation is acknowledged to be totally false at this point.
				sprtemp[frame].rotate = 0;
				for (r = 0; r < 8; r++) {
					sprtemp[frame].lump[r] = (short) (lump - firstspritelump);
					sprtemp[frame].flip[r] = (byte) (flipped ? 1 : 0);
				}
				return;
			}

			// the lump is only used for one rotation
			if (sprtemp[frame].rotate == 0)
				I.Error("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump",
						spritename, 'A' + frame);

			sprtemp[frame].rotate = 1;

			// make 0 based
			rotation--;
			if (sprtemp[frame].lump[rotation] == -1) {
				// FUN FACT: with resource coalesing, this is no longer an
				// error.
				// I.Error
				// ("R_InitSprites: Sprite %s : %c : %c has two lumps mapped to it",
				// spritename, 'A'+frame, '1'+rotation);

				// Everything is OK, we can bless the temporary sprite's frame's
				// rotation.
				sprtemp[frame].lump[rotation] = (short) (lump - firstspritelump);
				sprtemp[frame].flip[rotation] = (byte) (flipped ? 1 : 0);
				sprtemp[frame].rotate = 1; // jff 4/24/98 only change if rot
											// used
			}
		}

		/**
		 * R_InstallSpriteLump Local function for R_InitSprites.
		 * 
		 * Boom function, more suited to resource coalescing.
		 * 
		 */

		protected final void InstallSpriteLump2(int lump, int frame,
				int rotation, boolean flipped) {
			if (frame >= MAX_SPRITE_FRAMES || rotation > 8)
				I.Error("R_InstallSpriteLump: Bad frame characters in lump %i",
						lump);

			if ((int) frame > maxframe)
				maxframe = frame;

			if (rotation == 0) { // the lump should be used for all rotations
				int r;
				for (r = 0; r < 8; r++)
					if (sprtemp[frame].lump[r] == -1) {
						sprtemp[frame].lump[r] = lump - firstspritelump;
						sprtemp[frame].flip[r] = (byte) (flipped ? 1 : 0);
						sprtemp[frame].rotate = 0; // jff 4/24/98 if any subbed,
													// rotless
					}
				return;
			}

			// the lump is only used for one rotation

			if (sprtemp[frame].lump[--rotation] == -1) {
				sprtemp[frame].lump[rotation] = lump - firstspritelump;
				sprtemp[frame].flip[rotation] = (byte) (flipped ? 1 : 0);
				sprtemp[frame].rotate = 1; // jff 4/24/98 only change if rot
											// used
			}
		}

		//
		// R_InitSpriteDefs
		// Pass a null terminated list of sprite names
		// (4 chars exactly) to be used.
		//
		// Builds the sprite rotation matrixes to account
		// for horizontally flipped sprites.
		//
		// Will report an error if the lumps are inconsistent.
		// Only called at startup.
		//
		// Sprite lump names are 4 characters for the actor,
		// a letter for the frame, and a number for the rotation.
		//
		// A sprite that is flippable will have an additional
		// letter/number appended.
		//
		// The rotation character can be 0 to signify no rotations.
		//
		// 1/25/98, 1/31/98 killough : Rewritten for performance
		//
		// Empirically verified to have excellent hash
		// properties across standard Doom sprites:

		private final void InitSpriteDefs2(String[] namelist) {
			int numentries = lastspritelump - firstspritelump + 1;
			Hashtable<Integer, List<Integer>> hash;
			int i;

			if (numentries == 0 || namelist == null)
				return;

			// count the number of sprite names
			i = namelist.length;

			numsprites = i;

			sprites = new spritedef_t[numsprites];
			C2JUtils.initArrayOfObjects(sprites);

			// Create hash table based on just the first four letters of each
			// sprite
			// killough 1/31/98
			// Maes: the idea is to have a chained hastable which can handle
			// multiple
			// entries (sprites) on the same primary key (the 4 first chars of
			// the
			// sprite name)

			hash = new Hashtable<Integer, List<Integer>>(numentries); // allocate
																		// hash
																		// table

			// We have to trasverse this in the opposite order, so that later
			// lumps
			// trump previous ones in order.
			for (i = numentries - 1; i >= 0; i--) {
				int hashcode = SpriteNameHash(W
						.GetLumpInfo(i + firstspritelump).name);
				// Create chain list for each sprite class (e.g. TROO, POSS,
				// etc.)
				//
				if (!hash.containsKey(hashcode)) {
					hash.put(hashcode, new ArrayList<Integer>());
				}

				// Store (yet another) lump index for this sprite.
				hash.get(hashcode).add(i);
			}

			// scan all the lump names for each of the names,
			// noting the highest frame letter.

			for (i = 0; i < numsprites; i++) {

				// We only look for entries that are known to be sprites.
				// The hashtable may contain a lot of other shit, at this point
				// which will be hopefully ignored.
				String spritename = namelist[i];
				List<Integer> list = hash.get(SpriteNameHash(spritename));

				// Well, it may have been something else. Fuck it.
				if (list != null && !list.isEmpty()) {

					// Maes: the original code actually set everything to "-1"
					// here, including the
					// "boolean" rotate value. The idea was to create a
					// "tristate" of sorts, where -1
					// means a sprite of uncertain status. Goto
					// InstallSpriteLumps for more.
					for (int k = 0; k < sprtemp.length; k++) {
						Arrays.fill(sprtemp[k].flip, (byte) -1);
						Arrays.fill(sprtemp[k].lump, (short) -1);
						// This should be INDETERMINATE at this point.
						sprtemp[k].rotate = -1;
					}
					maxframe = -1;

					// What is stored in the lists are all actual lump numbers
					// relative
					// to e.g. TROO. In coalesced lumps, there will be overlap.
					// This procedure should, in theory, trump older ones.

					for (Integer j : list) {

						lumpinfo_t lump = W.GetLumpInfo(j + firstspritelump);

						// We don't know a-priori which frames exist.
						// However, we do know how to interpret existing ones,
						// and have an implicit maximum sequence of 29 Frames.
						// A frame can also hame multiple rotations.

						if (lump.name.substring(0, 4).equalsIgnoreCase(
								spritename.substring(0, 4))) {

							int frame = lump.name.charAt(4) - 'A';
							int rotation = lump.name.charAt(5) - '0';

							if (sprtemp[frame].rotate != -1) {
								// We already encountered this sprite, but we
								// may need to trump it with something else

							}

							InstallSpriteLump2(j + firstspritelump, frame,
									rotation, false);
							if (lump.name.length() >= 7) {
								frame = lump.name.charAt(6) - 'A';
								rotation = lump.name.charAt(7) - '0';
								InstallSpriteLump2(j + firstspritelump, frame,
										rotation, true);
							}
						}
					}

					// check the frames that were found for completeness
					if ((sprites[i].numframes = ++maxframe) != 0) // killough
																	// 1/31/98
					{
						int frame;
						for (frame = 0; frame < maxframe; frame++)
							switch ((int) sprtemp[frame].rotate) {
							case -1:
								// no rotations were found for that frame at all
								I.Error("R_InitSprites: No patches found for %s frame %c",
										namelist[i], frame + 'A');
								break;

							case 0:
								// only the first rotation is needed
								break;

							case 1:
								// must have all 8 frames
							{
								int rotation;
								for (rotation = 0; rotation < 8; rotation++)
									if (sprtemp[frame].lump[rotation] == -1)
										I.Error("R_InitSprites: Sprite %s frame %c is missing rotations",
												namelist[i], frame + 'A');
								break;
							}
							}
						// allocate space for the frames present and copy
						// sprtemp to it
						// MAES: we can do that elegantly in one line.

						sprites[i].copy(sprtemp, maxframe);
					}

				}
			}

		}

		private final int SpriteNameHash(String ss) {
			return ss.substring(0, 4).hashCode();
		}

		/**
		 * R_InitSpriteDefs Pass a null terminated list of sprite names (4 chars
		 * exactly) to be used. Builds the sprite rotation matrixes to account
		 * for horizontally flipped sprites. Will report an error if the lumps
		 * are inconsistent. Only called at startup.
		 * 
		 * Sprite lump names are 4 characters for the actor, a letter for the
		 * frame, and a number for the rotation. A sprite that is flippable will
		 * have an additional letter/number appended. The rotation character can
		 * be 0 to signify no rotations.
		 */

		public void InitSpriteDefs(String[] namelist) {

			int intname;
			int frame;
			int rotation;
			int start;
			int end;
			int patched;

			if (namelist == null)
				return;
			numsprites = namelist.length;

			if (numsprites == 0)
				return;

			sprites = new spritedef_t[numsprites];
			C2JUtils.initArrayOfObjects(sprites);

			start = firstspritelump - 1;
			end = lastspritelump + 1;

			// scan all the lump names for each of the names,
			// noting the highest frame letter.
			// Just compare 4 characters as ints
			for (int i = 0; i < numsprites; i++) {
				// System.out.println("Preparing sprite "+i);
				spritename = namelist[i];

				// The original code actually set everything to "-1"
				// here, including the "boolean" rotate value. The idea was 
				// to create a "tristate" of sorts, where -1 means a 
				// sprite of uncertain status. Goto InstallSpriteLumps
				// for more.
				for (int j = 0; j < sprtemp.length; j++) {
					Arrays.fill(sprtemp[j].flip, (byte) -1);
					Arrays.fill(sprtemp[j].lump, (short) -1);
					// This should be INDETERMINATE at this point.
					sprtemp[j].rotate = -1;
				}

				maxframe = -1;
				intname = name8.getIntName(namelist[i].toUpperCase());

				// scan the lumps,
				// filling in the frames for whatever is found
				for (int l = start + 1; l < end; l++) {
					// We HOPE it has 8 characters.
					char[] cname = W.GetLumpInfo(l).name.toCharArray();
					if (cname.length == 6 || cname.length == 8) // Sprite names
																// must be this
																// way

						// If the check is successful, we keep looking for more
						// frames
						// for a particular sprite e.g. TROOAx, TROOHxHy etc.
						//
						if (W.GetLumpInfo(l).intname == intname) {
							frame = cname[4] - 'A';
							rotation = cname[5] - '0';

							if (DM.modifiedgame)
								patched = W
										.GetNumForName(W.GetLumpInfo(l).name);
							else
								patched = l;

							InstallSpriteLump2(patched, frame, rotation, false);

							// Second set of rotations?
							if (cname.length > 6 && cname[6] != 0) {
								frame = cname[6] - 'A';
								rotation = cname[7] - '0';
								InstallSpriteLump2(l, frame, rotation, true);
							}
						}
				}

				// check the frames that were found for completeness
				// This can only be -1 at this point if we didn't install
				// a single frame successfuly.
				//
				if (maxframe == -1) {
					// System.out.println("Sprite "+spritename+" has no frames!");
					getSprites()[i].numframes = 0;
					// We move on to the next sprite with this one.
					continue;
				}

				maxframe++;

				for (frame = 0; frame < maxframe; frame++) {
					switch ((int) sprtemp[frame].rotate) {
					case -1:
						// no rotations were found for that frame at all
						I.Error("R_InitSprites: No patches found for %s frame %c",
								namelist[i], frame + 'A');
						break;

					case 0:
						// only the first rotation is needed
						break;

					case 1:
						// must have all 8 frames
						for (rotation = 0; rotation < 8; rotation++)
							if (sprtemp[frame].lump[rotation] == -1)
								I.Error("R_InitSprites: Sprite %s frame %c is missing rotations",
										namelist[i], frame + 'A');
						break;
					}
				}

				// allocate space for the frames present and copy sprtemp to it
				// MAES: we can do that elegantly in one line.

				sprites[i].copy(sprtemp, maxframe);

				// sprites[i].numframes = maxframe;
				// sprites[i].spriteframes = new spriteframe_t[maxframe];
				// C2JUtils.initArrayOfObjects(sprites[i].spriteframes,spriteframe_t.class);

				// for (int j=0;j<)
				// System.arraycopy(src, srcPos, dest, destPos, length)
				// memcpy (sprites[i].spriteframes, sprtemp,
				// maxframe*sizeof(spriteframe_t));
			}

		}

		//
		// GAME FUNCTIONS
		//
		vissprite_t[] vissprites;
		int vissprite_p;
		int newvissprite;

		/**
		 * R_ClearSprites Called at frame start.
		 */
		public void ClearSprites() {
			// vissprite_p = vissprites;
			vissprite_p = 0;
		}

		private final vissprite_t overflowsprite = new vissprite_t();

		/**
		 * R_NewVisSprite
		 * 
		 * Returns either a "new" sprite (actually, reuses a pool), or a special
		 * "overflow sprite" which just gets overwritten with bogus data.
		 * 
		 * FIXME: It's a bit of dumb thing to do, since the overflow sprite is
		 * never rendered but we have to copy data over it anyway. Would make
		 * more sense to check for it specifically and avoiding copying data,
		 * which should be more time consuming.
		 * 
		 * TODO: definitive fix for this would be to make it fully
		 * limit-removing.
		 * 
		 * @return
		 */
		public vissprite_t NewVisSprite() {
			if (vissprite_p == (vissprites.length - 1)) {
				ResizeSprites();
			}
			// return overflowsprite;

			vissprite_p++;
			return vissprites[vissprite_p - 1];
		}

		protected final void ResizeSprites() {
			vissprite_t[] tmp = new vissprite_t[vissprites.length * 2];
			System.arraycopy(vissprites, 0, tmp, 0, vissprites.length);

			C2JUtils.initArrayOfObjects(tmp, vissprites.length, tmp.length);

			// Bye bye, old vissprites.
			vissprites = tmp;

			System.out.println("Vissprites resized. Actual capacity "
					+ vissprites.length);
		}

		/**
		 * R_DrawVisSprite mfloorclip and mceilingclip should also be set.
		 * 
		 * Sprites are actually drawn here.
		 * 
		 */
		public final void DrawVisSprite(vissprite_t vis, int x1, int x2) {
			column_t column;
			int texturecolumn;
			int frac; // fixed_t
			patch_t patch;

			// At this point, the view angle (and patch) has already been
			// chosen. Go back.
			patch = W.CachePatchNum(vis.patch + firstspritelump, PU_CACHE);

			maskedcvars.dc_colormap = vis.colormap;
			// colfunc=glasscolfunc;
			if (maskedcvars.dc_colormap == null) {
				// NULL colormap = shadow draw
				colfunc = fuzzcolfunc;
			} else if ((vis.mobjflags & MF_TRANSLATION) != 0) {
				colfunc = DrawTranslatedColumn;
				
				maskedcvars.dc_translation = translationtables[(vis.mobjflags & MF_TRANSLATION)>>MF_TRANSSHIFT];
				// Let's interpret this:
				// MF_TRANSSHIFT -8 = 18 Shifting 0xc000000>>18 gives 0x300 or 768
				// It could also give 512 or 256. Essentially, the mask gives 4 
				// possible translations. 0= no change, and 1,2,3 : different tables. 
			}

			maskedcvars.dc_iscale = Math.abs(vis.xiscale) >> detailshift;
			maskedcvars.dc_texturemid = vis.texturemid;
			frac = vis.startfrac;
			spryscale = vis.scale;
			sprtopscreen = centeryfrac - FixedMul(maskedcvars.dc_texturemid, spryscale);

			// A texture height of 0 means "not tiling" and holds for
			// all sprite/masked renders.
			maskedcvars.dc_texheight=0;
			
			for (maskedcvars.dc_x = vis.x1; maskedcvars.dc_x <= vis.x2; maskedcvars.dc_x++, frac += vis.xiscale) {
				texturecolumn = frac >> FRACBITS;
				if (RANGECHECK) {
					if (texturecolumn < 0 || texturecolumn >= patch.width)
						I.Error("R_DrawSpriteRange: bad texturecolumn");
				}
				column = patch.columns[texturecolumn];
				//try {
				DrawMaskedColumn(column);
				//} catch (Exception e){
				    //System.err.printf("Trouble with column %d of patch %s",texturecolumn,patch.name,
				      //  vis.patch + firstspritelump);
				//}
			}

			colfunc = maskedcolfunc;
		}

		/**
		 * R_ProjectSprite Generates a vissprite for a thing if it might be
		 * visible.
		 * 
		 * @param thing
		 */
		public void ProjectSprite(mobj_t thing) {
			int tr_x, tr_y;
			int gxt, gyt;
			int tx, tz;

			int xscale, x1, x2;

			spritedef_t sprdef;
			spriteframe_t sprframe;
			int lump;

			int rot;
			boolean flip;

			int index;

			vissprite_t vis;

			long ang;
			int iscale;

			// transform the origin point
			tr_x = thing.x - viewx;
			tr_y = thing.y - viewy;

			gxt = FixedMul(tr_x, viewcos);
			gyt = -FixedMul(tr_y, viewsin);

			tz = gxt - gyt;

			// thing is behind view plane?
			if (tz < MINZ)
				return;
			/* MAES: so projection/tz gives horizontal scale */
			xscale = FixedDiv(projection, tz);

			gxt = -FixedMul(tr_x, viewsin);
			gyt = FixedMul(tr_y, viewcos);
			tx = -(gyt + gxt);

			// too far off the side?
			if (Math.abs(tx) > (tz << 2))
				return;

			// decide which patch to use for sprite relative to player
			if (RANGECHECK) {
				if (thing.sprite.ordinal() >= numsprites)
					I.Error("R_ProjectSprite: invalid sprite number %i ",
							thing.sprite);
			}
			sprdef = getSprites()[thing.sprite.ordinal()];
			if (RANGECHECK) {
				if ((thing.frame & FF_FRAMEMASK) >= sprdef.numframes)
					I.Error("R_ProjectSprite: invalid sprite frame %i : %i ",
							thing.sprite, thing.frame);
			}
			sprframe = sprdef.spriteframes[thing.frame & FF_FRAMEMASK];

			if (sprframe.rotate != 0) {
				// choose a different rotation based on player view
				ang = PointToAngle(thing.x, thing.y);
				rot = (int) ((ang - thing.angle + (ANG45 * 9) / 2) & BITS32) >>> 29;
				lump = sprframe.lump[rot];
				flip = (boolean) (sprframe.flip[rot] != 0);
			} else {
				// use single rotation for all views
				lump = sprframe.lump[0];
				flip = (boolean) (sprframe.flip[0] != 0);
			}

			// calculate edges of the shape
			tx -= spriteoffset[lump];
			x1 = (centerxfrac + FixedMul(tx, xscale)) >> FRACBITS;

			// off the right side?
			if (x1 > viewwidth)
				return;

			tx += spritewidth[lump];
			x2 = ((centerxfrac + FixedMul(tx, xscale)) >> FRACBITS) - 1;

			// off the left side
			if (x2 < 0)
				return;

			// store information in a vissprite
			vis = NewVisSprite();
			vis.mobjflags = thing.flags;
			vis.scale = xscale << detailshift;
			vis.gx = thing.x;
			vis.gy = thing.y;
			vis.gz = thing.z;
			vis.gzt = thing.z + spritetopoffset[lump];
			vis.texturemid = vis.gzt - viewz;
			vis.x1 = x1 < 0 ? 0 : x1;
			vis.x2 = x2 >= viewwidth ? viewwidth - 1 : x2;
			/*
			 * This actually determines the general sprite scale) iscale =
			 * 1/xscale, if this was floating point.
			 */
			iscale = FixedDiv(FRACUNIT, xscale);

			if (flip) {
				vis.startfrac = spritewidth[lump] - 1;
				vis.xiscale = -iscale;
			} else {
				vis.startfrac = 0;
				vis.xiscale = iscale;
			}

			if (vis.x1 > x1)
				vis.startfrac += vis.xiscale * (vis.x1 - x1);
			vis.patch = lump;

			// get light level
			if ((thing.flags & MF_SHADOW) != 0) {
				// shadow draw
				vis.colormap = null;
			} else if (fixedcolormap != null) {
				// fixed map
				vis.colormap = fixedcolormap;
				// vis.pcolormap=0;
			} else if ((thing.frame & FF_FULLBRIGHT) != 0) {
				// full bright
				vis.colormap = colormaps[0];
				// vis.pcolormap=0;
			}

			else {
				// diminished light
				index = xscale >> (LIGHTSCALESHIFT - detailshift);

				if (index >= MAXLIGHTSCALE)
					index = MAXLIGHTSCALE - 1;

				vis.colormap = spritelights[index];
				// vis.pcolormap=index;
			}
		}

		/**
		 * R_AddSprites
		 * 
		 * During BSP traversal, this adds sprites by sector.
		 * 
		 * 
		 */
		public void AddSprites(sector_t sec) {
			if (DEBUG)
				System.out.println("AddSprites");
			mobj_t thing;
			int lightnum;

			// BSP is traversed by subsector.
			// A sector might have been split into several
			// subsectors during BSP building.
			// Thus we check whether its already added.
			if (sec.validcount == validcount)
				return;

			// Well, now it will be done.
			sec.validcount = validcount;

			lightnum = (sec.lightlevel >> LIGHTSEGSHIFT) + extralight;

			if (lightnum < 0)
				spritelights = scalelight[0];
			else if (lightnum >= LIGHTLEVELS)
				spritelights = scalelight[LIGHTLEVELS - 1];
			else
				spritelights = scalelight[lightnum];

			// Handle all things in sector.
			for (thing = sec.thinglist; thing != null; thing = (mobj_t) thing.snext)
				ProjectSprite(thing);
		}

		/**
		 * R_DrawPSprite
		 * 
		 * Draws a "player sprite" with slighly different rules than normal
		 * sprites. This is actually a PITA, at best :-/
		 * 
		 */

		public final void DrawPSprite(pspdef_t psp) {

			int tx;
			int x1;
			int x2;
			spritedef_t sprdef;
			spriteframe_t sprframe;
			vissprite_t vis;
			int lump;
			boolean flip;

			//

			// decide which patch to use (in terms of angle?)
			if (RANGECHECK) {
				if (psp.state.sprite.ordinal() >= numsprites)
					I.Error("R_ProjectSprite: invalid sprite number %i ",
							psp.state.sprite);
			}

			sprdef = getSprites()[psp.state.sprite.ordinal()];
			
			if (RANGECHECK) {
				if ((psp.state.frame & FF_FRAMEMASK) >= sprdef.numframes)
					I.Error("R_ProjectSprite: invalid sprite frame %i : %i ",
							psp.state.sprite, psp.state.frame);
			}
			
			sprframe = sprdef.spriteframes[psp.state.frame & FF_FRAMEMASK];

			// Base frame for "angle 0" aka viewed from dead-front.
			lump = sprframe.lump[0];
			// Q: where can this be set? A: at sprite loadtime.
			flip = (boolean) (sprframe.flip[0] != 0);

			// calculate edges of the shape. tx is expressed in "view units".
			tx = (int) (FixedMul(psp.sx, BOBADJUST) - WEAPONADJUST);

			tx -= spriteoffset[lump];

			// So...centerxfrac is the center of the screen (pixel coords in
			// fixed point).
			x1 = (centerxfrac + FixedMul(tx, pspritescale)) >> FRACBITS;

			// off the right side
			if (x1 > viewwidth)
				return;

			tx += spritewidth[lump];
			x2 = ((centerxfrac + FixedMul(tx, pspritescale)) >> FRACBITS) - 1;

			// off the left side
			if (x2 < 0)
				return;

			// store information in a vissprite ?
			vis = avis;
			vis.mobjflags = 0;
			// FIXME: I need to compensate for freelook here, otherwise the weapon wanders 
			// around. Currently, only one screen scaling looks correct (fixed weapons),
			// all others exhibit some degree of wandering.
			vis.texturemid = ((BASEYCENTER+lookdir) << FRACBITS) + FRACUNIT / 2
					- (psp.sy - spritetopoffset[lump]);
			vis.x1 = x1 < 0 ? 0 : x1;
			vis.x2 = x2 >= viewwidth ? viewwidth - 1 : x2;
			vis.scale = (pspritescale) << detailshift;

			if (flip) {
				vis.xiscale = -pspriteiscale;
				vis.startfrac = spritewidth[lump] - 1;
			} else {
				vis.xiscale = pspriteiscale;
				vis.startfrac = 0;
			}

			if (vis.x1 > x1)
				vis.startfrac += vis.xiscale * (vis.x1 - x1);

			vis.patch = lump;

			if ((viewplayer.powers[pw_invisibility] > 4 * 32)
					|| (viewplayer.powers[pw_invisibility] & 8) != 0) {
				// shadow draw
				vis.colormap = null;

			} else if (fixedcolormap != null) {
				// fixed color
				vis.colormap = fixedcolormap;
				// vis.pcolormap=0;
			} else if ((psp.state.frame & FF_FULLBRIGHT) != 0) {
				// full bright
				vis.colormap = colormaps[0];
				// vis.pcolormap=0;
			} else {
				// local light
				vis.colormap = spritelights[MAXLIGHTSCALE - 1];
			}

			// System.out.println("Weapon draw "+vis);
			DrawVisSprite(vis, vis.x1, vis.x2);
		}

		/*
		========================
		=
		= R_DrawPSprite
		=
		========================
		*/

		protected int PSpriteSY[] =
		{
			0,				// staff
			5*FRACUNIT,		// goldwand
			15*FRACUNIT,	// crossbow
			15*FRACUNIT,	// blaster
			15*FRACUNIT,	// skullrod
			15*FRACUNIT,	// phoenix rod
			15*FRACUNIT,	// mace
			15*FRACUNIT,	// gauntlets
			15*FRACUNIT		// beak
		};
		
		/** used inside DrawPSprite, better make this static */
		protected vissprite_t avis = new vissprite_t();

		/**
		 * R_DrawPlayerSprites
		 * 
		 * This is where stuff like guns is drawn...right?
		 * 
		 * 
		 * */

		protected final void DrawPlayerSprites() {
			int i;
			int lightnum;
			pspdef_t psp;

			// get light level
			lightnum = (viewplayer.mo.subsector.sector.lightlevel >> LIGHTSEGSHIFT)
					+ extralight;

			if (lightnum < 0)
				spritelights = scalelight[0];
			else if (lightnum >= LIGHTLEVELS)
				spritelights = scalelight[LIGHTLEVELS - 1];
			else
				spritelights = scalelight[lightnum];

			// clip to screen bounds
			mfloorclip = screenheightarray;
			p_mfloorclip = 0;
			mceilingclip = negonearray;
			p_mceilingclip = 0;

			// add all active psprites
			// MAES 25/5/2011 Fixed another stupid bug that prevented
			// PSP from actually being updated. This in turn uncovered
			// other bugs in the way psp and state were treated, and the way
			// flash states were set. It should be OK now.
			for (i = 0; i < NUMPSPRITES; i++) {
				psp = viewplayer.psprites[i];
				if (psp.state != null && psp.state.id != 0) {
					DrawPSprite(psp);
				}
			}
		}

		vissprite_t vsprsortedhead;
		// A dummy used as temp during sorting.
		vissprite_t unsorted;

		/**
		 * R_SortVisSprites
		 * 
		 * UNUSED more efficient Comparable sorting + built-in Arrays.sort
		 * function used.
		 * 
		 */

		protected final void SortVisSprites() {
			Arrays.sort(vissprites, 0, vissprite_p);

			// Maes: got rid of old vissprite sorting code. Java's is better
			// Hell, almost anything was better than that.
			
		}

		// MAES: Scale to SCREENWIDTH
		protected short[] clipbot;
		protected short[] cliptop;

		/**
		 * R_DrawSprite
		 */

		public void DrawSprite(vissprite_t spr) {
			int ds;
			drawseg_t dss;

			int x;
			int r1;
			int r2;
			int scale; // fixed
			int lowscale; // fixed
			int silhouette;

			for (x = spr.x1; x <= spr.x2; x++)
				clipbot[x] = cliptop[x] = -2;

			// Scan drawsegs from end to start for obscuring segs.
			// The first drawseg that has a greater scale
			// is the clip seg.
			for (ds = ds_p - 1; ds >= 0; ds--) {
				// determine if the drawseg obscures the sprite
				// System.out.println("Drawseg "+ds+"of "+(ds_p-1));
				dss = drawsegs[ds];
				if (dss.x1 > spr.x2
						|| dss.x2 < spr.x1
						|| ((dss.silhouette == 0) && (dss
								.nullMaskedTextureCol()))) {
					// does not cover sprite
					continue;
				}

				r1 = dss.x1 < spr.x1 ? spr.x1 : dss.x1;
				r2 = dss.x2 > spr.x2 ? spr.x2 : dss.x2;

				if (dss.scale1 > dss.scale2) {
					lowscale = dss.scale2;
					scale = dss.scale1;
				} else {
					lowscale = dss.scale1;
					scale = dss.scale2;
				}

				if (scale < spr.scale
						|| (lowscale < spr.scale && (dss.curline
								.PointOnSegSide(spr.gx, spr.gy) == 0))) {
					// masked mid texture?
					if (!dss.nullMaskedTextureCol())
						MySegs.RenderMaskedSegRange(dss, r1, r2);
					// seg is behind sprite
					continue;
				}

				// clip this piece of the sprite
				silhouette = dss.silhouette;

				if (spr.gz >= dss.bsilheight)
					silhouette &= ~SIL_BOTTOM;

				if (spr.gzt <= dss.tsilheight)
					silhouette &= ~SIL_TOP;

				// BOTTOM clipping
				if (silhouette == 1) {
					// bottom sil
					for (x = r1; x <= r2; x++)
						if (clipbot[x] == -2)
							// clipbot[x] = ds->sprbottomclip[x];
							if (dss.nullSprBottomClip()) {
								if (DEBUG)
									System.out
											.println("Bottom clipping requested but clip list not present for "
													+ dss);

							} else
								clipbot[x] = dss.getSprBottomClip(x);

				} else if (silhouette == 2) {
					// top sil
					for (x = r1; x <= r2; x++)
						if (cliptop[x] == -2)
							cliptop[x] = dss.getSprTopClip(x);
				} else if (silhouette == 3) {
					// both
					for (x = r1; x <= r2; x++) {
						if (clipbot[x] == -2)
							clipbot[x] = dss.getSprBottomClip(x);
						if (cliptop[x] == -2)
							cliptop[x] = dss.getSprTopClip(x);
					}
				}

			}

			// all clipping has been performed, so draw the sprite

			// check for unclipped columns
			for (x = spr.x1; x <= spr.x2; x++) {
				if (clipbot[x] == -2)
					clipbot[x] = (short) viewheight;
				// ?? What's this bullshit?
				if (cliptop[x] == -2)
					cliptop[x] = -1;
			}

			mfloorclip = clipbot;
			p_mfloorclip = 0;
			mceilingclip = cliptop;
			p_mceilingclip = 0;
			DrawVisSprite(spr, spr.x1, spr.x2);
		}

		/**
		 * An alternate comb sort implementation. It doesn't perform as well as
		 * a generic quicksort/mergesort, but performs pretty close.
		 * 
		 * @param input
		 * @param length
		 */

		protected final void combSort(vissprite_t[] input, int length) {
			int gap = length;
			boolean swapped = true;
			while (gap > 1 || swapped) {
				if (gap > 1)
					gap = (int) (gap / 1.247330950103979);

				int i = 0;
				swapped = false;
				while (i + gap < length) {
					if (input[i].compareTo(input[i + gap]) > 0) {
						vissprite_t t = input[i];
						input[i] = input[i + gap];
						input[i + gap] = t;
						swapped = true;
					}
					i++;
				}
			}
		}

		/**
		 * R_DrawMasked
		 * 
		 * Sorts and draws vissprites (room for optimization in sorting func.)
		 * Draws masked textures. Draws player weapons and overlays (psprites).
		 * 
		 * Sorting function can be swapped for almost anything, and it will work
		 * better, in-place and be simpler to draw, too.
		 * 
		 * 
		 */
		protected final void DrawMasked() {
			// vissprite_t spr;
			int ds;
			drawseg_t dss;

			// Well, it sorts visspite objects.
			// It actually IS faster to sort with comparators, but you need to
			// go into NUTS.WAD-like wads.
			// numbers. The built-in sort if about as good as it gets. In fact,
			// it's hardly slower
			// to draw sprites without sorting them when using the built-in
			// modified mergesort, while
			// the original algorithm is so dreadful it actually does slow
			// things down.

			SortVisSprites();

			// If you are feeling adventurous, try these ones. They *might*
			// perform
			// better in very extreme situations where all sprites are always on
			// one side
			// of your view, but I hardly see any benefits in that. They are
			// both
			// much better than the original anyway.

			// combSort(vissprites,vissprite_p);
			// shellsort(vissprites,vissprite_p);

			// pQuickSprite.sort(vissprites);

			// The original sort. It's incredibly bad on so many levels (uses a
			// separate
			// linked list for the sorted sequence, which is pointless since the
			// vissprite_t
			// array is gonna be changed all over in the next frame anyway, it's
			// not like
			// it helps preseving or anything. It does work in Java too, but I'd
			// say to Keep Away. No srsly.

			/*
			 * SortVisSprites ();
			 * 
			 * // Sprite "0" not visible? /*if (vissprite_p > 0) { // draw all
			 * vissprites back to front for (spr = vsprsortedhead.next ; spr !=
			 * vsprsortedhead ; spr=spr.next) {
			 * 
			 * DrawSprite (spr); } }
			 */

			// After using in-place sorts, sprites can be drawn as simply as
			// that.

			colfunc = maskedcolfunc; // Sprites use fully-masked capable
										// function.

			for (int i = 0; i < vissprite_p; i++) {
				DrawSprite(vissprites[i]);
			}

			// render any remaining masked mid textures
			for (ds = ds_p - 1; ds >= 0; ds--) {
				dss = drawsegs[ds];
				if (!dss.nullMaskedTextureCol())
					MySegs.RenderMaskedSegRange(dss, dss.x1, dss.x2);
			}
			// draw the psprites on top of everything
			// but does not draw on side views
			// if (viewangleoffset==0)

			colfunc = playercolfunc;
			DrawPlayerSprites();
			colfunc = maskedcolfunc;
		}

	}

	protected final class BSP {

		/** newend is one past the last valid seg (cliprange_t) */
		int newend;
		cliprange_t[] solidsegs;

		public BSP() {
			solidsegs = new cliprange_t[MAXSEGS + 1];
			C2JUtils.initArrayOfObjects(solidsegs);

		}

		/**
		 * R_ClearDrawSegs
		 * 
		 * The drawseg list is reset by pointing back at 0.
		 * 
		 */
		public void ClearDrawSegs() {
			ds_p = 0;
		}

		/**
		 * R_ClipSolidWallSegment
		 * 
		 * Does handle solid walls, single sided LineDefs (middle texture) that
		 * entirely block the view VERTICALLY.
		 * 
		 * Handles "clipranges" for a solid wall, aka where it blocks the view.
		 * 
		 * @param first
		 *            starting y coord?
		 * @param last
		 *            ending y coord?
		 */

		private void ClipSolidWallSegment(int first, int last) {

			int next;
			int start;
			// int maxlast=Integer.MIN_VALUE;

			start = 0; // within solidsegs

			// Find the first cliprange that touches the range.
			// Actually, the first one not completely hiding it (its last must
			// be lower than first.

			while (solidsegs[start].last < first - 1)
				start++;

			// If the post begins above the lastly found cliprange...
			if (first < solidsegs[start].first) {
				// ..and ends above it, too (no overlapping)
				if (last < solidsegs[start].first - 1) {
					// ... then the post is entirely visible (above start),
					// so insert a new clippost. Calling this function
					// tells the renderer that there is an obstruction.
					// TODO: determine WHERE this info is actually saved.
					MySegs.StoreWallRange(first, last);

					// Newend should have a value of 2 if we are at the
					// beginning of a new frame.
					next = newend;
					newend++;

					if (next >= solidsegs.length)
						ResizeSolidSegs();
					while (next != start) {
						// *next=*(next-1);
						/*
						 * MAES: I think this is supposed to copy the structs
						 * 
						 * solidsegs[next] = solidsegs[next-1].clone();
						 * 
						 * OK, so basically the last solidseg copies its
						 * previous, and so on until we reach the start. This
						 * means that at some point, the value of the start
						 * solidseg is duplicated.
						 */

						solidsegs[next].copy(solidsegs[next - 1]);

						next--;
					}

					// At this point, next points at start.
					// Therefore, start
					solidsegs[next].first = first;
					solidsegs[next].last = last;
					return;
				}

				// There is a fragment above *start. This can occur if it a
				// post does start before another, but its lower edge overlaps
				// (partial, upper occlusion)
				MySegs.StoreWallRange(first, solidsegs[start].first - 1);
				// Now adjust the clip size.
				solidsegs[start].first = first;
			}

			// We can reach this only if a post starts AFTER another

			// Bottom contained in start? Obviously it won't be visible.
			if (last <= solidsegs[start].last)
				return;

			next = start;
			while (last >= solidsegs[(next + 1)].first - 1) {
				// There is a fragment between two posts.
				MySegs.StoreWallRange(solidsegs[next].last + 1,
						solidsegs[next + 1].first - 1);
				next++;

				if (last <= solidsegs[next].last) {
					// Bottom is contained in next.
					// Adjust the clip size.
					solidsegs[start].last = solidsegs[next].last;
					// goto crunch;

					{ // crunch code
						if (next == start) {
							// Post just extended past the bottom of one post.
							return;
						}

						while (next++ != newend) {
							// Remove a post.
							// MAES: this is a struct copy.
							if (next >= solidsegs.length)
								ResizeSolidSegs();
							solidsegs[++start].copy(solidsegs[next]);
						}

						newend = start + 1;
						return;
					}
				}
			}

			// There is a fragment after *next.
			MySegs.StoreWallRange(solidsegs[next].last + 1, last);
			// Adjust the clip size.
			solidsegs[start].last = last;

			// Remove start+1 to next from the clip list,
			// because start now covers their area.

			{ // crunch code
				if (next == start) {
					// Post just extended past the bottom of one post.
					return;
				}

				while (next++ != newend) {
					// Remove a post.
					// MAES: this is a struct copy.
					// MAES: this can overflow, breaking e.g. MAP30 of Final
					// Doom.
					if (next >= solidsegs.length)
						ResizeSolidSegs();
					solidsegs[++start].copy(solidsegs[next]);
				}

				newend = start + 1;
				return;
			}
		}

		protected final void ResizeSolidSegs() {
			cliprange_t[] tmp = new cliprange_t[solidsegs.length * 2];
			System.arraycopy(solidsegs, 0, tmp, 0, solidsegs.length);

			C2JUtils.initArrayOfObjects(tmp, solidsegs.length, tmp.length);

			// Bye bye, old solidsegs.
			solidsegs = tmp;
			// MAXDRAWSEGS*=2;

			System.out.println("Solidseg buffer resized. Actual capacity: "
					+ solidsegs.length);
		}

		//
		// R_ClipPassWallSegment
		// Clips the given range of columns,
		// but does not includes it in the clip list.
		// Does handle windows,
		// e.g. LineDefs with upper and lower texture.
		//
		private void ClipPassWallSegment(int first, int last) {

			// Find the first range that touches the range
			// (adjacent pixels are touching).
			int start = 0;

			while (solidsegs[start].last < first - 1)
				start++;

			if (first < solidsegs[start].first) {
				if (last < solidsegs[start].first - 1) {
					// Post is entirely visible (above start).
					MySegs.StoreWallRange(first, last);
					return;
				}

				// There is a fragment above *start.
				MySegs.StoreWallRange(first, solidsegs[start].first - 1);
			}

			// Bottom contained in start?
			if (last <= solidsegs[start].last)
				return;

			// MAES: Java absolutely can't do without a sanity check here.
			// if (startptr>=MAXSEGS-2) return;

			while (last >= solidsegs[start + 1].first - 1) {
				// There is a fragment between two posts.
				MySegs.StoreWallRange(solidsegs[start].last + 1,
						solidsegs[start + 1].first - 1);
				start++;
				// if (startptr>=MAXSEGS-2) return;
				// start=solidsegs[startptr];

				if (last <= solidsegs[start].last)
					return;
			}

			// There is a fragment after *next.
			MySegs.StoreWallRange(solidsegs[start].last + 1, last);
		}

		/**
		 * R_ClearClipSegs
		 * 
		 * Clears the clipping segs list. The list is actually fixed size for
		 * efficiency reasons, so it just tells Doom to use the first two
		 * solidsegs, which are "neutered". It's interesting to note how the
		 * solidsegs begin and end just "outside" the visible borders of the
		 * screen.
		 * 
		 * 
		 */

		void ClearClipSegs() {
			solidsegs[0].first = -0x7fffffff;
			solidsegs[0].last = -1;
			solidsegs[1].first = viewwidth;
			solidsegs[1].last = 0x7fffffff;
			newend = 2; // point so solidsegs[2];
		}

		/**
		 * R_AddLine
		 * 
		 * Called after a SubSector BSP trasversal ends up in a "final"
		 * subsector.
		 * 
		 * Clips the given segment and adds any visible pieces to the line list.
		 * It also determines what kind of boundary (line) visplane clipping
		 * should be performed. E.g. window, final 1-sided line, closed door
		 * etc.) CAREFUL: was the source of much frustration with visplanes...
		 * 
		 */
		private void AddLine(seg_t line) {
			if (DEBUG)
				System.out.println("Entered AddLine for " + line);
			int x1;
			int x2;
			long angle1;
			long angle2;
			long span;
			long tspan;

			curline = line;

			// OPTIMIZE: quickly reject orthogonal back sides.
			angle1 = PointToAngle(line.v1x, line.v1y);
			angle2 = PointToAngle(line.v2x, line.v2y);

			// Clip to view edges.
			// OPTIMIZE: make constant out of 2*clipangle (FIELDOFVIEW).

			span = addAngles(angle1, -angle2);

			// Back side? I.e. backface culling?
			if (span >= ANG180)
				return;

			// Global angle needed by segcalc.
			rw_angle1 = angle1;
			angle1 -= viewangle;
			angle2 -= viewangle;

			angle1 &= BITS32;
			angle2 &= BITS32;

			tspan = addAngles(angle1, clipangle);

			if (tspan > CLIPANGLE2) {
				tspan -= CLIPANGLE2;
				tspan &= BITS32;

				// Totally off the left edge?
				if (tspan >= span)
					return;

				angle1 = clipangle;
			}
			tspan = addAngles(clipangle, -angle2);

			if (tspan > CLIPANGLE2) {
				tspan -= CLIPANGLE2;
				tspan &= BITS32;

				// Totally off the left edge?
				if (tspan >= span)
					return;
				angle2 = -clipangle;
				angle2 &= BITS32;
			}

			// The seg is in the view range,
			// but not necessarily visible.

			angle1 = ((angle1 + ANG90) & BITS32) >>> ANGLETOFINESHIFT;
			angle2 = ((angle2 + ANG90) & BITS32) >>> ANGLETOFINESHIFT;
			x1 = viewangletox[(int) angle1];
			x2 = viewangletox[(int) angle2];

			// Does not cross a pixel?
			if (x1 == x2)
				return;

			backsector = line.backsector;

			// Single sided line?

			if (backsector == null) {
				if (DEBUG)
					System.out
							.println("Entering ClipSolidWallSegment SS with params "
									+ x1 + " " + (x2 - 1));
				ClipSolidWallSegment(x1, x2 - 1); // to clipsolid
				if (DEBUG)
					System.out.println("Exiting ClipSolidWallSegment");
				return;
			}

			// Closed door.
			if (backsector.ceilingheight <= frontsector.floorheight
					|| backsector.floorheight >= frontsector.ceilingheight) {
				if (DEBUG)
					System.out
							.println("Entering ClipSolidWallSegment Closed door with params "
									+ x1 + " " + (x2 - 1));
				ClipSolidWallSegment(x1, x2 - 1);
				; // to clipsolid
				return;
			}

			// Window. This includes same-level floors with different textures
			if (backsector.ceilingheight != frontsector.ceilingheight
					|| backsector.floorheight != frontsector.floorheight) {
				if (DEBUG)
					System.out
							.println("Entering ClipSolidWallSegment window with params "
									+ x1 + " " + (x2 - 1));
				ClipPassWallSegment(x1, x2 - 1); // to clippass
				return;
			}

			// Reject empty lines used for triggers
			// and special events.
			// Identical floor and ceiling on both sides,
			// identical light levels on both sides,
			// and no middle texture.
			if (backsector.ceilingpic == frontsector.ceilingpic
					&& backsector.floorpic == frontsector.floorpic
					&& backsector.lightlevel == frontsector.lightlevel
					&& curline.sidedef.midtexture == 0) {
				return;
			}

			// If nothing of the previous holds, then we are
			// treating the case of same-level, differently
			// textured floors. ACHTUNG, this caused the "bleeding floor"
			// bug, which is now fixed.
			// Fucking GOTOs....
			ClipPassWallSegment(x1, x2 - 1); // to clippass
			if (DEBUG)
				System.out.println("Exiting AddLine for " + line);
		}

		//
		// R_CheckBBox
		// Checks BSP node/subtree bounding box.
		// Returns true
		// if some part of the bbox might be visible.
		//
		private int[][] checkcoord = { { 3, 0, 2, 1 }, { 3, 0, 2, 0 },
				{ 3, 1, 2, 0 }, { 0 }, { 2, 0, 2, 1 }, { 0, 0, 0, 0 },
				{ 3, 1, 3, 0 }, { 0 }, { 2, 0, 3, 1 }, { 2, 1, 3, 1 },
				{ 2, 1, 3, 0 } };

		/**
		 * 
		 * @param bspcoord
		 *            (fixed_t* as bbox)
		 * @return
		 */
		public boolean CheckBBox(int[] bspcoord) {
			int boxx;
			int boxy;
			int boxpos;

			// fixed_t
			int x1;
			int y1;
			int x2;
			int y2;

			// angle_t
			long angle1;
			long angle2;
			long span;
			long tspan;

			cliprange_t start;

			int sx1;
			int sx2;

			// Find the corners of the box
			// that define the edges from current viewpoint.
			if (viewx <= bspcoord[BOXLEFT])
				boxx = 0;
			else if (viewx < bspcoord[BOXRIGHT])
				boxx = 1;
			else
				boxx = 2;

			if (viewy >= bspcoord[BOXTOP])
				boxy = 0;
			else if (viewy > bspcoord[BOXBOTTOM])
				boxy = 1;
			else
				boxy = 2;

			boxpos = (boxy << 2) + boxx;
			if (boxpos == 5)
				return true;

			x1 = bspcoord[checkcoord[boxpos][0]];
			y1 = bspcoord[checkcoord[boxpos][1]];
			x2 = bspcoord[checkcoord[boxpos][2]];
			y2 = bspcoord[checkcoord[boxpos][3]];

			// check clip list for an open space
			angle1 = PointToAngle(x1, y1) - viewangle;
			angle2 = PointToAngle(x2, y2) - viewangle;

			angle1 &= BITS32;
			angle2 &= BITS32;

			span = angle1 - angle2;

			span &= BITS32;

			// Sitting on a line?
			if (span >= ANG180)
				return true;

			tspan = angle1 + clipangle;
			tspan &= BITS32;

			if (tspan > CLIPANGLE2) {
				tspan -= CLIPANGLE2;
				tspan &= BITS32;
				// Totally off the left edge?
				if (tspan >= span)
					return false;

				angle1 = clipangle;
			}
			tspan = (clipangle - angle2) & BITS32;
			;
			if (tspan > CLIPANGLE2) {
				tspan -= CLIPANGLE2;
				tspan &= BITS32;

				// Totally off the left edge?
				if (tspan >= span)
					return false;

				angle2 = -clipangle;
				angle2 &= BITS32;
			}

			// Find the first clippost
			// that touches the source post
			// (adjacent pixels are touching).
			angle1 = ((angle1 + ANG90) & BITS32) >>> ANGLETOFINESHIFT;
			angle2 = ((angle2 + ANG90) & BITS32) >>> ANGLETOFINESHIFT;
			sx1 = viewangletox[(int) angle1];
			sx2 = viewangletox[(int) angle2];

			// Does not cross a pixel.
			if (sx1 == sx2)
				return false;
			sx2--;

			int pstart = 0;
			start = solidsegs[pstart];
			// FIXME: possible solidseg overflow here overflows
			while (start.last < sx2 && pstart < MAXSEGS)
				start = solidsegs[pstart++];

			if (sx1 >= start.first && sx2 <= start.last) {
				// The clippost contains the new span.
				return false;
			}

			return true;
		}

		/**
		 * R_Subsector
		 * 
		 * Determine floor/ceiling planes. Add sprites of things in sector. Draw
		 * one or more line segments. It also alters the visplane list!
		 * 
		 * 
		 * @param num
		 *            Subsector from subsector_t list in Lever Loader.
		 * 
		 */

		private void Subsector(int num) {
			if (DEBUG)
				System.out.println("\t\tSubSector " + num + " to render");
			int count;
			int line; // pointer into a list of segs instead of seg_t
			subsector_t sub;

			if (RANGECHECK) {
				if (num >= LL.numsubsectors)
					I.Error("R_Subsector: ss %i with numss = %i", num,
							LL.numsubsectors);
			}

			sscount++;
			sub = LL.subsectors[num];

			frontsector = sub.sector;
			if (DEBUG)
				System.out.println("Frontsector to render :" + frontsector);
			count = sub.numlines;
			// line = LL.segs[sub.firstline];
			line = sub.firstline;

			if (DEBUG)
				System.out
						.println("Trying to find an existing FLOOR visplane...");
			if (frontsector.floorheight < viewz) {
				floorplane = FindPlane(frontsector.floorheight,
						frontsector.floorpic, frontsector.lightlevel);
			} else
				// FIXME: unclear what would happen with a null visplane used
				// It's never checked explicitly for either condition, just
				// called straight.
				floorplane = -1; // in lieu of NULL

			// System.out.println("Trying to find an existing CEILING visplane...");

			if (frontsector.ceilingheight > viewz
					|| frontsector.ceilingpic == TexMan.getSkyFlatNum()) {
				ceilingplane = FindPlane(frontsector.ceilingheight,
						frontsector.ceilingpic, frontsector.lightlevel);
			} else
				ceilingplane = -1; // In lieu of NULL. Will bomb if actually
									// used.

			MyThings.AddSprites(frontsector);

			if (DEBUG)
				System.out.println("Enter Addline for SubSector " + num
						+ " count " + count);
			while (count-- > 0) {
				AddLine(LL.segs[line]);
				line++;
			}
			if (DEBUG)
				System.out.println("Exit Addline for SubSector " + num);
		}

		/**
		 * RenderBSPNode Renders all subsectors below a given node, traversing
		 * subtree recursively. Just call with BSP root.
		 */
		public void RenderBSPNode(int bspnum) {
			if (DEBUG)
				System.out.println("Processing BSP Node " + bspnum);

			node_t bsp;
			int side;

			// Found a subsector? Then further decisions are taken, in, well,
			// SubSector.
			if (C2JUtils.flags(bspnum, NF_SUBSECTOR)) {
				if (DEBUG)
					System.out.println("Subsector found.");
				if (bspnum == -1)
					Subsector(0);
				else
					Subsector(bspnum & (~NF_SUBSECTOR));
				return;
			}

			bsp = LL.nodes[bspnum];

			// Decide which side the view point is on.
			side = bsp.PointOnSide(viewx, viewy);
			if (DEBUG)
				System.out.println("\tView side: " + side);

			// Recursively divide front space.
			if (DEBUG)
				System.out.println("\tEnter Front space of " + bspnum);
			RenderBSPNode(bsp.children[side]);
			if (DEBUG)
				System.out.println("\tReturn Front space of " + bspnum);

			// Possibly divide back space.

			if (CheckBBox(bsp.bbox[side ^ 1].bbox)) {
				if (DEBUG)
					System.out.println("\tEnter Back space of " + bspnum);
				RenderBSPNode(bsp.children[side ^ 1]);
				if (DEBUG)
					System.out.println("\tReturn Back space of " + bspnum);
			}
		}

	}

	protected abstract class SegDrawer implements ILimitResettable {

		protected static final int HEIGHTBITS = 12;
		protected static final int HEIGHTUNIT = (1 << HEIGHTBITS);

		public void resetLimits() {
			drawseg_t[] tmp = new drawseg_t[MAXDRAWSEGS];
			System.arraycopy(drawsegs, 0, tmp, 0, MAXDRAWSEGS);

			// Now, that was quite a haircut!.
			drawsegs = tmp;

			// System.out.println("Drawseg buffer cut back to original limit of "+MAXDRAWSEGS);
		}

		/**
		 * R_RenderMaskedSegRange
		 * 
		 * @param ds
		 * @param x1
		 * @param x2
		 */
		public final void RenderMaskedSegRange(drawseg_t ds, int x1, int x2) {
			int index;

			int lightnum;
			int texnum;

			// System.out.printf("RenderMaskedSegRange from %d to %d\n",x1,x2);

			// Calculate light table.
			// Use different light tables
			// for horizontal / vertical / diagonal. Diagonal?
			// OPTIMIZE: get rid of LIGHTSEGSHIFT globally
			curline = ds.curline;
			frontsector = curline.frontsector;
			backsector = curline.backsector;
			texnum = TexMan.getTextureTranslation(curline.sidedef.midtexture);
			// System.out.print(" for texture "+textures[texnum].name+"\n:");
			lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT) + extralight;

			if (curline.v1y == curline.v2y)
				lightnum--;
			else if (curline.v1x == curline.v2x)
				lightnum++;

			// Killough code.
			walllights = lightnum >= LIGHTLEVELS ? scalelight[LIGHTLEVELS - 1]
					: lightnum < 0 ? scalelight[0] : scalelight[lightnum];

			// Get the list
			maskedtexturecol = ds.getMaskedTextureColList();
			// And this is the pointer.
			pmaskedtexturecol = ds.getMaskedTextureColPointer();

			rw_scalestep = ds.scalestep;
			spryscale = ds.scale1 + (x1 - ds.x1) * rw_scalestep;

			// HACK to get "pointers" inside clipping lists
			mfloorclip = ds.getSprBottomClipList();
			p_mfloorclip = ds.getSprBottomClipPointer();
			mceilingclip = ds.getSprTopClipList();
			p_mceilingclip = ds.getSprTopClipPointer();
			// find positioning
			if ((curline.linedef.flags & ML_DONTPEGBOTTOM) != 0) {
			    maskedcvars.dc_texturemid = frontsector.floorheight > backsector.floorheight ? frontsector.floorheight
						: backsector.floorheight;
			    maskedcvars.dc_texturemid = maskedcvars.dc_texturemid + TexMan.getTextureheight(texnum)
						- viewz;
			} else {
			    maskedcvars.dc_texturemid = frontsector.ceilingheight < backsector.ceilingheight ? frontsector.ceilingheight
						: backsector.ceilingheight;
			    maskedcvars.dc_texturemid = maskedcvars.dc_texturemid - viewz;
			}
			maskedcvars.dc_texturemid += curline.sidedef.rowoffset;

			if (fixedcolormap != null)
			    maskedcvars.dc_colormap = fixedcolormap;

			// Texture height must be set at this point. This will trigger
			// tiling. For sprites, it should be set to 0.
			maskedcvars.dc_texheight = TexMan.getTextureheight(texnum) >> FRACBITS;

			// draw the columns
			for (maskedcvars.dc_x = x1; maskedcvars.dc_x <= x2; maskedcvars.dc_x++) {
				// calculate lighting
				if (maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x] != Short.MAX_VALUE) {
					if (fixedcolormap == null) {
						index = spryscale >>> LIGHTSCALESHIFT;

						if (index >= MAXLIGHTSCALE)
							index = MAXLIGHTSCALE - 1;

						maskedcvars.dc_colormap = walllights[index];
					}

					sprtopscreen = centeryfrac
							- FixedMul(maskedcvars.dc_texturemid, spryscale);
					maskedcvars.dc_iscale = (int) (0xffffffffL / spryscale);

					// draw the texture
					byte[] data = GetColumn(texnum,
							maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x]);// -3);
					
					DrawMaskedColumn(data);
		
					maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x] = Short.MAX_VALUE;
				}
				spryscale += rw_scalestep;
			}

		}

		/**
		 * R_StoreWallRange A wall segment will be drawn between start and stop
		 * pixels (inclusive). This is the only place where
		 * markceiling/markfloor can be set. Can only be called from
		 * ClipSolidWallSegment and ClipPassWallSegment.
		 * 
		 * 
		 * @throws IOException
		 */

		public void StoreWallRange(int start, int stop) {

			if (DEBUG2)
				System.out.println("\t\t\t\tStorewallrange called between "
						+ start + " and " + stop);

			int hyp; // fixed_t
			int sineval; // fixed_t
			int distangle;
			long offsetangle; // angle_t
			int vtop; // fixed_t
			int lightnum;
			drawseg_t seg;

			// don't overflow and crash
			if (ds_p == drawsegs.length)
				ResizeDrawsegs();

			if (RANGECHECK) {
				if (start >= viewwidth || start > stop)
					I.Error("Bad R_RenderWallRange: %i to %i", start, stop);
			}

			seg = drawsegs[ds_p];

			sidedef = curline.sidedef;
			linedef = curline.linedef;

			// mark the segment as visible for auto map
			linedef.flags |= ML_MAPPED;

			// calculate rw_distance for scale calculation
			rw_normalangle = addAngles(curline.angle, ANG90);

			/*
			 * MAES: ok, this is a tricky spot. angle_t's are supposed to be
			 * always positive 32-bit unsigned integers, so a subtraction should
			 * be always positive by definition, right? WRONG: this fucking spot
			 * caused "blind spots" at certain angles because ONLY HERE angles
			 * are supposed to be treated as SIGNED and result in differences
			 * <180 degrees -_-
			 * 
			 * The only way to coerce this behavior is to cast both as signed
			 * ints.
			 */
			offsetangle = Math.abs((int) rw_normalangle - (int) rw_angle1);

			if (offsetangle > ANG90)
				offsetangle = ANG90;

			// It should fit even in a signed int, by now.
			distangle = (int) (ANG90 - offsetangle);
			hyp = PointToDist(curline.v1x, curline.v1y);
			sineval = finesine(distangle);
			rw_distance = FixedMul(hyp, sineval);

			seg.x1 = rw_x = start;
			seg.x2 = stop;
			seg.curline = curline;
			/*
			 * This is the only place it's ever explicitly assigned. Therefore
			 * it always starts at stop+1.
			 */
			rw_stopx = stop + 1;

			// calculate scale at both ends and step
			// this is the ONLY place where rw_scale is set.
			seg.scale1 = rw_scale = ScaleFromGlobalAngle((viewangle + xtoviewangle[start]));

			if (stop > start) {
				seg.scale2 = ScaleFromGlobalAngle(viewangle
						+ xtoviewangle[stop]);
				seg.scalestep = rw_scalestep = (seg.scale2 - rw_scale)
						/ (stop - start);
			} else {
				// UNUSED: try to fix the stretched line bug
				/*
				 * #if 0 if (rw_distance < FRACUNIT/2) { fixed_t trx,try;
				 * fixed_t gxt,gyt;
				 * 
				 * trx = curline.v1.x - viewx; try = curline.v1.y - viewy;
				 * 
				 * gxt = FixedMul(trx,viewcos); gyt = -FixedMul(try,viewsin);
				 * seg.scale1 = FixedDiv(projection, gxt-gyt)<<detailshift; }
				 * #endif
				 */
				seg.scale2 = seg.scale1;
			}

			// calculate texture boundaries
			// and decide if floor / ceiling marks are needed
			worldtop = frontsector.ceilingheight - viewz;
			worldbottom = frontsector.floorheight - viewz;

			midtexture = toptexture = bottomtexture = 0;
			maskedtexture = false;
			seg.setMaskedTextureCol(null, 0);
			// seg.maskedtexturecol = null;

			if (backsector == null) {
				// single sided line
				midtexture = TexMan.getTextureTranslation(sidedef.midtexture);
				// a single sided line is terminal, so it must mark ends
				markfloor = markceiling = true;
				if ((linedef.flags & ML_DONTPEGBOTTOM) != 0) {
					vtop = frontsector.floorheight
							+ TexMan.getTextureheight(sidedef.midtexture);
					// bottom of texture at bottom
					rw_midtexturemid = vtop - viewz;
				} else {
					// top of texture at top
					rw_midtexturemid = worldtop;
				}
				rw_midtexturemid += sidedef.rowoffset;

				seg.silhouette = SIL_BOTH;
				seg.setSprTopClip(screenheightarray, 0);
				seg.setSprBottomClip(negonearray, 0);
				seg.bsilheight = Integer.MAX_VALUE;
				seg.tsilheight = Integer.MIN_VALUE;
			} else {
				// two sided line
				seg.setSprTopClip(null, 0);
				seg.setSprBottomClip(null, 0);
				seg.silhouette = 0;

				if (frontsector.floorheight > backsector.floorheight) {
					seg.silhouette = SIL_BOTTOM;
					seg.bsilheight = frontsector.floorheight;
				} else if (backsector.floorheight > viewz) {
					seg.silhouette = SIL_BOTTOM;
					seg.bsilheight = Integer.MAX_VALUE;
					// seg.sprbottomclip = negonearray;
				}

				if (frontsector.ceilingheight < backsector.ceilingheight) {
					seg.silhouette |= SIL_TOP;
					seg.tsilheight = frontsector.ceilingheight;
				} else if (backsector.ceilingheight < viewz) {
					seg.silhouette |= SIL_TOP;
					seg.tsilheight = Integer.MIN_VALUE;
					// seg.sprtopclip = screenheightarray;
				}

				if (backsector.ceilingheight <= frontsector.floorheight) {
					seg.setSprBottomClip(negonearray, 0);
					seg.bsilheight = Integer.MAX_VALUE;
					seg.silhouette |= SIL_BOTTOM;
				}

				if (backsector.floorheight >= frontsector.ceilingheight) {
					seg.setSprTopClip(screenheightarray, 0);
					seg.tsilheight = Integer.MIN_VALUE;
					seg.silhouette |= SIL_TOP;
				}

				worldhigh = backsector.ceilingheight - viewz;
				worldlow = backsector.floorheight - viewz;

				// hack to allow height changes in outdoor areas
				if (frontsector.ceilingpic == TexMan.getSkyFlatNum()
						&& backsector.ceilingpic == TexMan.getSkyFlatNum()) {
					worldtop = worldhigh;
				}

				if (worldlow != worldbottom
						|| backsector.floorpic != frontsector.floorpic
						|| backsector.lightlevel != frontsector.lightlevel) {
					markfloor = true;
				} else {
					// same plane on both sides
					markfloor = false;
				}

				if (worldhigh != worldtop
						|| backsector.ceilingpic != frontsector.ceilingpic
						|| backsector.lightlevel != frontsector.lightlevel) {
					markceiling = true;
				} else {
					// same plane on both sides
					markceiling = false;
				}

				if (backsector.ceilingheight <= frontsector.floorheight
						|| backsector.floorheight >= frontsector.ceilingheight) {
					// closed door
					markceiling = markfloor = true;
				}

				if (worldhigh < worldtop) {
					// top texture
					toptexture = TexMan
							.getTextureTranslation(sidedef.toptexture);
					if ((linedef.flags & ML_DONTPEGTOP) != 0) {
						// top of texture at top
						rw_toptexturemid = worldtop;
					} else {
						vtop = backsector.ceilingheight
								+ TexMan.getTextureheight(sidedef.toptexture);

						// bottom of texture
						rw_toptexturemid = vtop - viewz;
					}
				}
				if (worldlow > worldbottom) {
					// bottom texture
					bottomtexture = TexMan
							.getTextureTranslation(sidedef.bottomtexture);

					if ((linedef.flags & ML_DONTPEGBOTTOM) != 0) {
						// bottom of texture at bottom
						// top of texture at top
						rw_bottomtexturemid = worldtop;
					} else
						// top of texture at top
						rw_bottomtexturemid = worldlow;
				}
				rw_toptexturemid += sidedef.rowoffset;
				rw_bottomtexturemid += sidedef.rowoffset;

				// allocate space for masked texture tables
				if (sidedef.midtexture != 0) {
					// masked midtexture
					maskedtexture = true;
					maskedtexturecol = openings;
					pmaskedtexturecol = lastopening - rw_x;
					seg.setMaskedTextureCol(maskedtexturecol, pmaskedtexturecol);
					lastopening += rw_stopx - rw_x;
				}
			}

			// calculate rw_offset (only needed for textured lines)
			segtextured = (((midtexture | toptexture | bottomtexture) != 0) | maskedtexture);

			if (segtextured) {
				offsetangle = addAngles(rw_normalangle, -rw_angle1);

				// Another "tricky spot": negative of an unsigned number?
				if (offsetangle > ANG180)
					offsetangle = (-(int) offsetangle) & BITS32;

				if (offsetangle > ANG90)
					offsetangle = ANG90;

				sineval = finesine(offsetangle);
				rw_offset = FixedMul(hyp, sineval);

				// Another bug: we CAN'T assume that the result won't wrap
				// around.
				// If that assumption is made, then texture alignment issues
				// appear
				if (((rw_normalangle - rw_angle1) & BITS32) < ANG180)
					rw_offset = -rw_offset;

				rw_offset += sidedef.textureoffset + curline.offset;
				// This is OK, however: we can add as much shit as we want,
				// as long as we trim it to the 32 LSB. Proof as to why
				// this is always true is left as an exercise to the reader.
				rw_centerangle = (ANG90 + viewangle - rw_normalangle) & BITS32;

				// calculate light table
				// use different light tables
				// for horizontal / vertical / diagonal
				// OPTIMIZE: get rid of LIGHTSEGSHIFT globally
				if (fixedcolormap == null) {
					lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT)
							+ extralight;

					if (curline.v1y == curline.v2y)
						lightnum--;
					else if (curline.v1x == curline.v2x)
						lightnum++;

					if (lightnum < 0)
						walllights = scalelight[0];
					else if (lightnum >= LIGHTLEVELS)
						walllights = scalelight[LIGHTLEVELS - 1];
					else
						walllights = scalelight[lightnum];
				}
			}

			// if a floor / ceiling plane is on the wrong side
			// of the view plane, it is definitely invisible
			// and doesn't need to be marked.

			if (frontsector.floorheight >= viewz) {
				// above view plane
				markfloor = false;
			}

			if (frontsector.ceilingheight <= viewz
					&& frontsector.ceilingpic != TexMan.getSkyFlatNum()) {
				// below view plane
				markceiling = false;
			}

			// calculate incremental stepping values for texture edges
			worldtop >>= 4;
			worldbottom >>= 4;

			topstep = -FixedMul(rw_scalestep, worldtop);
			topfrac = (centeryfrac >> 4) - FixedMul(worldtop, rw_scale);

			bottomstep = -FixedMul(rw_scalestep, worldbottom);
			bottomfrac = (centeryfrac >> 4) - FixedMul(worldbottom, rw_scale);

			if (backsector != null) {
				worldhigh >>= 4;
				worldlow >>= 4;

				if (worldhigh < worldtop) {
					pixhigh = (centeryfrac >> 4)
							- FixedMul(worldhigh, rw_scale);
					pixhighstep = -FixedMul(rw_scalestep, worldhigh);
				}

				if (worldlow > worldbottom) {
					pixlow = (centeryfrac >> 4) - FixedMul(worldlow, rw_scale);
					pixlowstep = -FixedMul(rw_scalestep, worldlow);
				}
			}

			// render it
			if (markceiling) {
				// System.out.println("Markceiling");
				ceilingplane = MyPlanes.CheckPlane(ceilingplane, rw_x,
						rw_stopx - 1);
			}

			if (markfloor) {
				// System.out.println("Markfloor");
				floorplane = MyPlanes
						.CheckPlane(floorplane, rw_x, rw_stopx - 1);
			}

			RenderSegLoop();

			// After rendering is actually performed, clipping is set.

			// save sprite clipping info ... no top clipping?
			if ((C2JUtils.flags(seg.silhouette, SIL_TOP) || maskedtexture)
					&& seg.nullSprTopClip()) {

				// memcpy (lastopening, ceilingclip+start, 2*(rw_stopx-start));
				System.arraycopy(ceilingclip, start, openings, lastopening,
						rw_stopx - start);

				seg.setSprTopClip(openings, lastopening - start);
				// seg.setSprTopClipPointer();
				lastopening += rw_stopx - start;
			}
			// no floor clipping?
			if ((C2JUtils.flags(seg.silhouette, SIL_BOTTOM) || maskedtexture)
					&& seg.nullSprBottomClip()) {
				// memcpy (lastopening, floorclip+start, 2*(rw_stopx-start));
				System.arraycopy(floorclip, start, openings, lastopening,
						rw_stopx - start);
				seg.setSprBottomClip(openings, lastopening - start);
				lastopening += rw_stopx - start;
			}

			if (maskedtexture && C2JUtils.flags(seg.silhouette, SIL_TOP)) {
				seg.silhouette |= SIL_TOP;
				seg.tsilheight = Integer.MIN_VALUE;
			}
			if (maskedtexture && (seg.silhouette & SIL_BOTTOM) == 0) {
				seg.silhouette |= SIL_BOTTOM;
				seg.bsilheight = Integer.MAX_VALUE;
			}
			ds_p++;
		}

		/**
		 * R_RenderSegLoop Draws zero, one, or two textures (and possibly a
		 * masked texture) for walls. Can draw or mark the starting pixel of
		 * floor and ceiling textures. Also sets the actual sprite clipping info
		 * (where sprites should be cut)
		 * 
		 * Since rw_x ranges are non-overlapping, rendering all walls means
		 * completing the clipping list as well.
		 * 
		 * The only difference between the parallel and the non-parallel
		 * version is that the parallel doesn't draw immediately but rather,
		 * generates RWIs. This can surely be unified to avoid replicating code.
		 * 
		 * CALLED: CORE LOOPING ROUTINE.
		 * 
		 * 
		 */

		protected void RenderSegLoop() {
			int angle; // angle_t
			int index;
			int yl; // low
			int yh; // hight
			int mid;
			int texturecolumn = 0; // fixed_t
			int top;
			int bottom;

			for (; rw_x < rw_stopx; rw_x++) {
				// mark floor / ceiling areas
				yl = (topfrac + HEIGHTUNIT - 1) >> HEIGHTBITS;

				// no space above wall?
				if (yl < ceilingclip[rw_x] + 1)
					yl = ceilingclip[rw_x] + 1;

				if (markceiling) {
					top = ceilingclip[rw_x] + 1;
					bottom = yl - 1;

					if (bottom >= floorclip[rw_x])
						bottom = floorclip[rw_x] - 1;

					if (top <= bottom) {
						visplanes[ceilingplane].setTop(rw_x, (char) top);
						visplanes[ceilingplane].setBottom(rw_x, (char) bottom);
					}
				}

				yh = bottomfrac >> HEIGHTBITS;

				if (yh >= floorclip[rw_x])
					yh = floorclip[rw_x] - 1;

				// A particular seg has been identified as a floor marker.

				if (markfloor) {
					top = yh + 1;
					bottom = floorclip[rw_x] - 1;
					if (top <= ceilingclip[rw_x])
						top = ceilingclip[rw_x] + 1;
					if (top <= bottom) {
						visplanes[floorplane].setTop(rw_x, (char) top);
						visplanes[floorplane].setBottom(rw_x, (char) bottom);
					}
				}

				// texturecolumn and lighting are independent of wall tiers
				if (segtextured) {
					// calculate texture offset

					// CAREFUL: a VERY anomalous point in the code. Their sum is supposed
					// to give an angle not exceeding 45 degrees (or an index of 0x0FFF after 
					// shifting). If added with pure unsigned rules, this doesn't hold anymore,
					// not even if accounting for overflow.
					angle = Tables.toBAMIndex(rw_centerangle
							+ (int) xtoviewangle[rw_x]);

					// FIXME: We are accessing finetangent here, the code seems
					// pretty confident in that angle won't exceed 4K no matter what. 
					// But xtoviewangle alone can yield 8K when shifted.
					// This usually only overflows if we idclip and look at
					// certain directions (probably angles get fucked up), however it seems rare
					// enough to just "swallow" the exception. You can eliminate it by anding
					// with 0x1FFF if you're so inclined.
					// FIXED by allowing overflow. See Tables for details.

					texturecolumn = rw_offset
							- FixedMul(finetangent[angle], rw_distance);
					texturecolumn >>= FRACBITS;
					// calculate lighting
					index = rw_scale >> LIGHTSCALESHIFT;

					if (index >= MAXLIGHTSCALE)
						index = MAXLIGHTSCALE - 1;
					
					dcvars.dc_colormap = walllights[index];
					dcvars.dc_x = rw_x;
					dcvars.dc_iscale = (int) (0xffffffffL / rw_scale);
				}

				// draw the wall tiers
				if (midtexture != 0) {
					// single sided line
				    dcvars.dc_yl = yl;
				    dcvars.dc_yh = yh;
				    dcvars.dc_texheight = TexMan.getTextureheight(midtexture) >> FRACBITS; // killough
				    dcvars.dc_texturemid = rw_midtexturemid;
				    dcvars.dc_source = GetCachedColumn(midtexture, texturecolumn);
					CompleteColumn();
					ceilingclip[rw_x] = (short) viewheight;
					floorclip[rw_x] = -1;
				} else {
					// two sided line
					if (toptexture != 0) {
						// top wall
						mid = pixhigh >> HEIGHTBITS;
						pixhigh += pixhighstep;

						if (mid >= floorclip[rw_x])
							mid = floorclip[rw_x] - 1;

						if (mid >= yl) {
						    dcvars.dc_yl = yl;
						    dcvars.dc_yh = mid;
						    dcvars.dc_texturemid = rw_toptexturemid;
						    dcvars.dc_texheight = TexMan.getTextureheight(toptexture) >> FRACBITS;
						    dcvars.dc_source = GetCachedColumn(toptexture,
									texturecolumn);
							// dc_source_ofs=0;
						    if (dcvars.dc_colormap==null) System.out.println("Two-sided");
							CompleteColumn();
							ceilingclip[rw_x] = (short) mid;
						} else
							ceilingclip[rw_x] = (short) (yl - 1);
					} else {
						// no top wall
						if (markceiling)
							ceilingclip[rw_x] = (short) (yl - 1);
					}

					if (bottomtexture != 0) {
						// bottom wall
						mid = (pixlow + HEIGHTUNIT - 1) >> HEIGHTBITS;
						pixlow += pixlowstep;

						// no space above wall?
						if (mid <= ceilingclip[rw_x])
							mid = ceilingclip[rw_x] + 1;

						if (mid <= yh) {
						    dcvars.dc_yl = mid;
						    dcvars.dc_yh = yh;
						    dcvars.dc_texturemid = rw_bottomtexturemid;
						    dcvars.dc_texheight = TexMan
									.getTextureheight(bottomtexture) >> FRACBITS;
						dcvars.dc_source = GetCachedColumn(bottomtexture,
									texturecolumn);
							CompleteColumn();

							floorclip[rw_x] = (short) mid;
						} else
							floorclip[rw_x] = (short) (yh + 1);
					} else {
						// no bottom wall
						if (markfloor)
							floorclip[rw_x] = (short) (yh + 1);
					}

					if (maskedtexture) {
						// save texturecol
						// for backdrawing of masked mid texture
						maskedtexturecol[pmaskedtexturecol + rw_x] = (short) texturecolumn;
					}
				}

				rw_scale += rw_scalestep;
				topfrac += topstep;
				bottomfrac += bottomstep;
			}
		}

		/**
		 * Called from RenderSegLoop. This should either invoke the column
		 * function, or store a wall rendering instruction in the parallel
		 * version. It's the only difference between the parallel and serial
		 * renderer, BTW. So override and implement accordingly.
		 * 
		 */
		protected abstract void CompleteColumn();

		protected column_t col;

		public SegDrawer() {
			col = new column_t();
			drawsegs = new drawseg_t[MAXDRAWSEGS];
			C2JUtils.initArrayOfObjects(drawsegs);
		}

	}

	protected interface IPlaneDrawer {

		void InitPlanes();

		void MapPlane(int y, int x1, int x2);

		void ClearPlanes();

		int CheckPlane(int plane, int x, int i);

		int[] getCachedHeight();

		int[] getCachedDistance();

		int[] getCachedXStep();

		int[] getCachedYStep();

		int[] getDistScale();

		int[] getYslope();

		int getBaseXScale();

		int getBaseYScale();

		void DrawPlanes();
	}

	public abstract class PlaneDrawer implements IPlaneDrawer, IVideoScaleAware {

		// protected planefunction_t floorfunc;
		// protected planefunction_t ceilingfunc;

		protected final boolean RANGECHECK = false;

		//
		// spanstart holds the start of a plane span
		// initialized to 0 at start
		//
		protected int[] spanstart, spanstop;

		//
		// texture mapping
		//
		protected byte[][] planezlight; // The distance lighting effect you see
		/** To treat as fixed_t */
		protected int planeheight;
		/** To treat at fixed_t */
		protected int[] yslope;
		protected float[] yslopef;
		/** To treat as fixed_t */
		protected int[] distscale;
		protected float[] distscalef;
		/** To treat as fixed_t */
		protected int basexscale, baseyscale;

		/** To treat as fixed_t */
		protected int[] cachedheight, cacheddistance, cachedxstep, cachedystep;

		/**
		 * Call only after visplanes have been properly resized for resolution.
		 * In case of dynamic resolution changes, the old ones should just be
		 * discarded, as they would be nonsensical.
		 */

		protected void initVisplanes() {
			C2JUtils.initArrayOfObjects(visplanes);
			}

		/**
		 * R_CheckPlane
		 * 
		 * Called from within StoreWallRange
		 * 
		 * Presumably decides if a visplane should be split or not?
		 * 
		 */

		public int CheckPlane(int index, int start, int stop) {

			if (DEBUG2)
				System.out.println("Checkplane " + index + " between " + start
						+ " and " + stop);

			// Interval ?
			int intrl;
			int intrh;

			// Union?
			int unionl;
			int unionh;
			// OK, so we check out ONE particular visplane.
			visplane_t pl = visplanes[index];

			if (DEBUG2)
				System.out.println("Checking out plane " + pl);

			int x;

			// If start is smaller than the plane's min...
			//
			// start minx maxx stop
			// | | | |
			// --------PPPPPPPPPPPPPP-----------
			//
			//
			if (start < pl.minx) {
				intrl = pl.minx;
				unionl = start;
				// Then we will have this:
				//
				// unionl intrl maxx stop
				// | | | |
				// --------PPPPPPPPPPPPPP-----------
				//

			} else {
				unionl = pl.minx;
				intrl = start;

				// else we will have this:
				//
				// union1 intrl maxx stop
				// | | | |
				// --------PPPPPPPPPPPPPP-----------
				//
				// unionl comes before intrl in any case.
				//
				//
			}

			// Same as before, for for stop and maxx.
			// This time, intrh comes before unionh.
			//

			if (stop > pl.maxx) {
				intrh = pl.maxx;
				unionh = stop;
			} else {
				unionh = pl.maxx;
				intrh = stop;
			}

			// An interval is now defined, which is entirely contained in the
			// visplane.
			//

			// If the value FF is NOT stored ANYWWHERE inside it, we bail out
			// early
			for (x = intrl; x <= intrh; x++)
				if (pl.getTop(x) != Character.MAX_VALUE)
					break;

			// This can only occur if the loop above completes,
			// else the visplane we were checking has non-visible/clipped
			// portions within that range: we must split.

			if (x > intrh) {
				// Merge the visplane
				pl.minx = unionl;
				pl.maxx = unionh;
				// System.out.println("Plane modified as follows "+pl);
				// use the same one
				return index;
			}

			// SPLIT: make a new visplane at "last" position, copying materials
			// and light.

			if (lastvisplane == visplanes.length) {
				//  visplane overflows could occur at this point.
				ResizeVisplanes();
			}

			visplanes[lastvisplane].height = pl.height;
			visplanes[lastvisplane].picnum = pl.picnum;
			visplanes[lastvisplane].lightlevel = pl.lightlevel;

			pl = visplanes[lastvisplane++];
			pl.minx = start;
			pl.maxx = stop;

			// memset (pl.top,0xff,sizeof(pl.top));
			pl.clearTop();

			// return pl;

			// System.out.println("New plane created: "+pl);
			return lastvisplane - 1;
		}

		/**
		 * R_ClearPlanes At begining of frame.
		 * 
		 */

		@Override
		public void ClearPlanes() {
			int angle;

			/*
			 * View planes are cleared at the beginning of every plane, by
			 * setting them "just outside" the borders of the screen (-1 and
			 * viewheight).
			 */

			// Maes: this should be faster, since it's called so often.
			System.arraycopy(BLANKFLOORCLIP, 0, floorclip, 0, viewwidth);
			System.arraycopy(BLANKCEILINGCLIP, 0, ceilingclip, 0, viewwidth);

			// Point to #1 in visplane list? OK... ?!
			lastvisplane = 0;

			// We point back to the first opening of the list openings[0],
			// again.
			lastopening = 0;

			// texture calculation
			System.arraycopy(BLANKCACHEDHEIGHT, 0, cachedheight, 0,
					BLANKCACHEDHEIGHT.length);

			// left to right mapping
			// FIXME: If viewangle is ever < ANG90, you're fucked. How can this
			// be prevented?
			// Answer: 32-bit unsigned are supposed to roll over. You can & with
			// 0xFFFFFFFFL.
			angle = (int) Tables.toBAMIndex(viewangle - ANG90);

			// scale will be unit scale at SCREENWIDTH/2 distance
			basexscale = FixedDiv(finecosine[angle], centerxfrac);
			baseyscale = -FixedDiv(finesine[angle], centerxfrac);
		}

		public void ClearClips() {

			System.arraycopy(BLANKFLOORCLIP, 0, floorclip, 0, viewwidth);
			System.arraycopy(BLANKCEILINGCLIP, 0, ceilingclip, 0, viewwidth);
		}

		/**
		 * R_MapPlane
		 * 
		 * Called only by R_MakeSpans.
		 * 
		 * This is where the actual span drawing function is called.
		 * 
		 * Uses global vars: planeheight ds_source -> flat data has already been
		 * set. basexscale -> actual drawing angle and position is computed from
		 * these baseyscale viewx viewy
		 * 
		 * BASIC PRIMITIVE
		 */

		public void MapPlane(int y, int x1, int x2) {
			// MAES: angle_t
			int angle;
			// fixed_t
			int distance;
			int length;
			int index;

			if (RANGECHECK) {
				if (x2 < x1 || x1 < 0 || x2 >= viewwidth || y > viewheight) {
					I.Error("R_MapPlane: %i, %i at %i", x1, x2, y);
				}
			}

			if (planeheight != cachedheight[y]) {
				cachedheight[y] = planeheight;
				distance = cacheddistance[y] = FixedMul(planeheight, yslope[y]);
				dsvars.ds_xstep = cachedxstep[y] = FixedMul(distance, basexscale);
				dsvars.ds_ystep = cachedystep[y] = FixedMul(distance, baseyscale);
			} else {
				distance = cacheddistance[y];
				dsvars.ds_xstep = cachedxstep[y];
				dsvars.ds_ystep = cachedystep[y];
			}

			length = FixedMul(distance, distscale[x1]);
			angle = (int) (((viewangle + xtoviewangle[x1]) & BITS32) >>> ANGLETOFINESHIFT);
			dsvars.ds_xfrac = viewx + FixedMul(finecosine[angle], length);
			dsvars.ds_yfrac = -viewy - FixedMul(finesine[angle], length);

			if (fixedcolormap != null)
			    dsvars.ds_colormap = fixedcolormap;
			else {
				index = distance >>> LIGHTZSHIFT;

				if (index >= MAXLIGHTZ)
					index = MAXLIGHTZ - 1;

				dsvars.ds_colormap = planezlight[index];
			}

			dsvars.ds_y = y;
			dsvars.ds_x1 = x1;
			dsvars.ds_x2 = x2;

			// high or low detail
			spanfunc.invoke();
		}

		/**
		 * R_MakeSpans
		 * 
		 * Called only by DrawPlanes. If you wondered where the actual
		 * boundaries for the visplane flood-fill are laid out, this is it.
		 * 
		 * The system of coords seems to be defining a sort of cone.
		 * 
		 * 
		 * @param x
		 *            Horizontal position
		 * @param t1
		 *            Top-left y coord?
		 * @param b1
		 *            Bottom-left y coord?
		 * @param t2
		 *            Top-right y coord ?
		 * @param b2
		 *            Bottom-right y coord ?
		 * 
		 */

		protected void MakeSpans(int x, int t1, int b1, int t2, int b2) {

			// If t1 = [sentinel value] then this part won't be executed.
			while (t1 < t2 && t1 <= b1) {
				this.MapPlane(t1, spanstart[t1], x - 1);
				t1++;
			}
			while (b1 > b2 && b1 >= t1) {
				this.MapPlane(b1, spanstart[b1], x - 1);
				b1--;
			}

			// So...if t1 for some reason is < t2, we increase t2 AND store the
			// current x
			// at spanstart [t2] :-S
			while (t2 < t1 && t2 <= b2) {
				// System.out.println("Increasing t2");
				spanstart[t2] = x;
				t2++;
			}

			// So...if t1 for some reason b2 > b1, we decrease b2 AND store the
			// current x
			// at spanstart [t2] :-S

			while (b2 > b1 && b2 >= t2) {
				// System.out.println("Decreasing b2");
				spanstart[b2] = x;
				b2--;
			}
		}

		/**
		 * R_InitPlanes Only at game startup.
		 */

		public void InitPlanes() {
			// Doh!
		}

		// //////////////////////////VIDEO SCALE STUFF
		// ////////////////////////////////

		protected int SCREENWIDTH;
		protected int SCREENHEIGHT;
		protected IVideoScale vs;

		@Override
		public void setVideoScale(IVideoScale vs) {
			this.vs = vs;
		}

		@Override
		public void initScaling() {
			this.SCREENHEIGHT = vs.getScreenHeight();
			this.SCREENWIDTH = vs.getScreenWidth();

			// Pre-scale stuff.

			spanstart = new int[SCREENHEIGHT];
			spanstop = new int[SCREENHEIGHT];
			yslope = new int[SCREENHEIGHT];
			yslopef = new float[SCREENHEIGHT];
			distscale = new int[SCREENWIDTH];
			distscalef = new float[SCREENWIDTH];
			cachedheight = new int[SCREENHEIGHT];
			cacheddistance = new int[SCREENHEIGHT];
			cachedxstep = new int[SCREENHEIGHT];
			cachedystep = new int[SCREENHEIGHT];

			// HACK: visplanes are initialized globally.
			visplane_t.setVideoScale(vs);
			visplane_t.initScaling();
			initVisplanes();

		}

		// ///////////// VARIOUS BORING GETTERS ////////////////////

		@Override
		public int[] getCachedHeight() {
			return this.cachedheight;
		}

		@Override
		public int[] getCachedDistance() {
			return this.cacheddistance;
		}

		@Override
		public int[] getCachedXStep() {
			return cachedxstep;
		}

		@Override
		public int[] getCachedYStep() {
			return cachedystep;
		}

		@Override
		public int[] getDistScale() {
			return distscale;
		}

		@Override
		public int[] getYslope() {
			return yslope;
		}

		@Override
		public int getBaseXScale() {
			return basexscale;
		}

		@Override
		public int getBaseYScale() {
			return baseyscale;
		}

	}

	protected class VisplaneWorker implements Runnable{

        public int id;
        private final int NUMFLOORTHREADS;
        int startvp;  
        int endvp;
        int vpw_planeheight;
        byte[][] vpw_planezlight;
        int vpw_basexscale,vpw_baseyscale;
        int[] cachedheight;
        int[] cacheddistance;
        int[] cachedxstep;
        int[] cachedystep;
        int[] distscale;
        int[] yslope;
        private final SpanVars<byte[]> vpw_dsvars;
        private final ColVars<byte[]> vpw_dcvars;
        private final DoomSpanFunction<byte[]> vpw_spanfunc;
        private final DoomSpanFunction<byte[]> vpw_spanfunclow;
        private final DoomColumnFunction<byte[]> vpw_skyfunc;
        
        
        public VisplaneWorker(int sCREENWIDTH, int sCREENHEIGHT, int[] columnofs,
                int[] ylookup, byte[] screen,CyclicBarrier visplanebarrier,int NUMFLOORTHREADS) {
            this.barrier=visplanebarrier;
            // Alias to those of Planes.
            cachedheight=MyPlanes.getCachedHeight();
            cacheddistance=MyPlanes.getCachedDistance();
            cachedxstep=MyPlanes.getCachedXStep();
            cachedystep=MyPlanes.getCachedYStep();
            distscale=MyPlanes.getDistScale();
            yslope=MyPlanes.getYslope();
            spanstart=new int[SCREENHEIGHT];
            spanstop=new int [SCREENHEIGHT];
            vpw_dsvars=new SpanVars<byte[]>();
            vpw_dcvars=new ColVars<byte[]>();
            vpw_spanfunc=new R_DrawSpanUnrolled(sCREENWIDTH,sCREENHEIGHT,ylookup,columnofs,vpw_dsvars,screen,I);
            vpw_spanfunclow=new R_DrawSpanLow(sCREENWIDTH,sCREENHEIGHT,ylookup,columnofs,vpw_dsvars,screen,I);
            vpw_skyfunc=new R_DrawColumnBoomOpt(sCREENWIDTH,sCREENHEIGHT,ylookup,columnofs,vpw_dcvars,screen,I);
            this.NUMFLOORTHREADS=NUMFLOORTHREADS;
        }

        @Override
        public void run() {
            visplane_t      pln=null; //visplane_t
            // These must override the global ones


            int         light;
            int         x;
            int         stop;
            int         angle;
            
            // Now it's a good moment to set them.
            vpw_basexscale=MyPlanes.getBaseXScale();
            vpw_baseyscale=MyPlanes.getBaseYScale();
            
            // TODO: find a better way to split work. As it is, it's very uneven
            // and merged visplanes in particular are utterly dire.
            
                for (int pl= this.id; pl <lastvisplane; pl+=NUMFLOORTHREADS) {
                 pln=visplanes[pl];
                // System.out.println(id +" : "+ pl);
                 
             if (pln.minx > pln.maxx)
                 continue;

             
             // sky flat
             if (pln.picnum == TexMan.getSkyFlatNum() )
             {
                 vpw_dcvars.dc_iscale = skyscale>>detailshift;
                 vpw_dcvars.viewheight=viewheight;
                 /* Sky is allways drawn full bright,
                  * i.e. colormaps[0] is used.
                  * Because of this hack, sky is not affected
                  * by INVUL inverse mapping.
                  */    
                vpw_dcvars.dc_colormap = colormaps[0];
                vpw_dcvars.dc_texturemid = TexMan.getSkyTextureMid();
                 for (x=pln.minx ; x <= pln.maxx ; x++)
                 {
               
                     vpw_dcvars.dc_yl = pln.getTop(x);
                     vpw_dcvars.dc_yh = pln.getBottom(x);
                 
                 if (vpw_dcvars.dc_yl <= vpw_dcvars.dc_yh)
                 {
                     angle = (int) (addAngles(viewangle, xtoviewangle[x])>>>ANGLETOSKYSHIFT);
                     vpw_dcvars.dc_x = x;
                     vpw_dcvars.dc_texheight=TexMan.getTextureheight(TexMan.getSkyTexture())>>FRACBITS;
                     vpw_dcvars.dc_source = GetCachedColumn(TexMan.getSkyTexture(), angle);
                     vpw_skyfunc.invoke();
                 }
                 }
                 continue;
             }
             
             // regular flat
             vpw_dsvars.ds_source = ((flat_t)W.CacheLumpNum(TexMan.getFlatTranslation(pln.picnum),
                            PU_STATIC,flat_t.class)).data;
             
             
             if (vpw_dsvars.ds_source.length<4096){
                 System.err.println("vpw_ds_source size <4096 ");
                 new Exception().printStackTrace();
             }
             
             vpw_planeheight = Math.abs(pln.height-viewz);
             light = (pln.lightlevel >>> LIGHTSEGSHIFT)+extralight;

             if (light >= LIGHTLEVELS)
                 light = LIGHTLEVELS-1;

             if (light < 0)
                 light = 0;

             vpw_planezlight = zlight[light];

             // We set those values at the border of a plane's top to a "sentinel" value...ok.
             pln.setTop(pln.maxx+1,(char) 0xffff);
             pln.setTop(pln.minx-1, (char) 0xffff);
             
             stop = pln.maxx + 1;

             
             for (x=pln.minx ; x<= stop ; x++) {
              MakeSpans(x,pln.getTop(x-1),
                 pln.getBottom(x-1),
                 pln.getTop(x),
                 pln.getBottom(x));
                }
             
             }
             // We're done, wait.

                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

         }
            

        public void setRange(int startvp, int endvp) {
            this.startvp=startvp;
            this.endvp=endvp;
            
        }
        
        /**
         * R_MakeSpans
         * 
         * Called only by DrawPlanes.
         * If you wondered where the actual boundaries for the visplane
         * flood-fill are laid out, this is it.
         * 
         * The system of coords seems to be defining a sort of cone.          
         *          
         * 
         * @param x Horizontal position
         * @param t1 Top-left y coord?
         * @param b1 Bottom-left y coord?
         * @param t2 Top-right y coord ?
         * @param b2 Bottom-right y coord ?
         * 
         */

          private final void MakeSpans(int x, int t1, int b1, int t2, int b2) {
              
              // If t1 = [sentinel value] then this part won't be executed.
              while (t1 < t2 && t1 <= b1) {
                  this.MapPlane(t1, spanstart[t1], x - 1);
                  t1++;
              }
              while (b1 > b2 && b1 >= t1) {
                  this.MapPlane(b1, spanstart[b1], x - 1);
                  b1--;
              }

              // So...if t1 for some reason is < t2, we increase t2 AND store the current x
              // at spanstart [t2] :-S
              while (t2 < t1 && t2 <= b2) {
                  //System.out.println("Increasing t2");
                  spanstart[t2] = x;
                  t2++;
              }

              // So...if t1 for some reason b2 > b1, we decrease b2 AND store the current x
              // at spanstart [t2] :-S

              while (b2 > b1 && b2 >= t2) {
                  //System.out.println("Decreasing b2");
                  spanstart[b2] = x;
                  b2--;
              }
          }
          
          /**
           * R_MapPlane
           *
           * Called only by R_MakeSpans.
           * 
           * This is where the actual span drawing function is called.
           * 
           * Uses global vars:
           * planeheight
           *  ds_source -> flat data has already been set.
           *  basexscale -> actual drawing angle and position is computed from these
           *  baseyscale
           *  viewx
           *  viewy
           *
           * BASIC PRIMITIVE
           */
          
          private void
          MapPlane
          ( int       y,
            int       x1,
            int       x2 )
          {
              // MAES: angle_t
              int angle;
              // fixed_t
              int distance;
              int length;
              int index;
              
          if (RANGECHECK){
              if (x2 < x1
              || x1<0
              || x2>=viewwidth
              || y>viewheight)
              {
              I.Error ("R_MapPlane: %i, %i at %i",x1,x2,y);
              }
          }

              if (vpw_planeheight != cachedheight[y])
              {
              cachedheight[y] = vpw_planeheight;
              distance = cacheddistance[y] = FixedMul (vpw_planeheight , yslope[y]);
              vpw_dsvars.ds_xstep = cachedxstep[y] = FixedMul (distance,vpw_basexscale);
              vpw_dsvars.ds_ystep = cachedystep[y] = FixedMul (distance,vpw_baseyscale);
              }
              else
              {
              distance = cacheddistance[y];
              vpw_dsvars.ds_xstep = cachedxstep[y];
              vpw_dsvars.ds_ystep = cachedystep[y];
              }
              
              length = FixedMul (distance,distscale[x1]);
              angle = (int)(((viewangle +xtoviewangle[x1])&BITS32)>>>ANGLETOFINESHIFT);
              vpw_dsvars.ds_xfrac = viewx + FixedMul(finecosine[angle], length);
              vpw_dsvars.ds_yfrac = -viewy - FixedMul(finesine[angle], length);

              if (fixedcolormap!=null)
                  vpw_dsvars.ds_colormap = fixedcolormap;
              else
              {
              index = distance >>> LIGHTZSHIFT;
              
              if (index >= MAXLIGHTZ )
                  index = MAXLIGHTZ-1;

              vpw_dsvars.ds_colormap = vpw_planezlight[index];
              }
              
              vpw_dsvars.ds_y = y;
              vpw_dsvars.ds_x1 = x1;
              vpw_dsvars.ds_x2 = x2;

              // high or low detail
              if (detailshift==0)
                  vpw_spanfunc.invoke();
              else
                  vpw_spanfunclow.invoke();         
          }
          
          
          // Private to each thread.
          int[]           spanstart;
          int[]           spanstop;
          CyclicBarrier barrier;
          
      }
	
	// /////////////////////// LIGHTS, POINTERS, COLORMAPS ETC. ////////////////

	// /// FROM R_DATA, R_MAIN , R_DRAW //////////

	/**
	 * OK< this is supposed to "peg" into screen buffer 0. It will work AS LONG
	 * AS SOMEONE FUCKING ACTUALLY SETS IT !!!!
	 */
	protected byte[] screen;

	protected static final boolean RANGECHECK = false;

	/**
	 * These are actually offsets inside screen 0 (or any screen). Therefore
	 * anything using them should "draw" inside screen 0
	 */
	protected int[] ylookup = new int[MAXHEIGHT];

	/** Columns offset to set where?! */
	protected int[] columnofs = new int[MAXWIDTH];
	
	/** General purpose. Used for solid walls and as an intermediary for threading */
	
	protected ColVars<byte[]> dcvars;
	
	// Used for sky drawer, to avoid clashing with shared dcvars
	protected ColVars<byte[]> skydcvars;
 
	// Used by parallel renderers to finish up some business
	protected ColVars<byte[]> maskedcvars;
	
	/** Used for spans */
	
	protected SpanVars<byte[]> dsvars;
	
	/**
	 * Color tables for different players, translate a limited part to another
	 * (color ramps used for suit colors).
	 */

	protected byte[][] translations = new byte[3][256];

	//
	// Lighting LUT.
	// Used for z-depth cuing per column/row,
	// and other lighting effects (sector ambient, flash).
	//

	/** Use in conjunction with pfixedcolormap */
	protected byte[] fixedcolormap;
	/** Use in conjunction with fixedcolormap[] */
	protected int pfixedcolormap;
	// lighttable_t[][] walllights;

	protected int centerx;
	protected int centery;

	/**
	 * e6y: wide-res Borrowed from PrBoom+;
	 */

	protected int wide_centerx, wide_ratio, wide_offsetx, wide_offset2x,
			wide_offsety, wide_offset2y;

	protected final base_ratio_t[] BaseRatioSizes = {
			new base_ratio_t(960, 600, 0, 48, 1.333333f), // 4:3
			new base_ratio_t(1280, 450, 0, 48 * 3 / 4, 1.777777f), // 16:9
			new base_ratio_t(1152, 500, 0, 48 * 5 / 6, 1.6f), // 16:10
			new base_ratio_t(960, 600, 0, 48, 1.333333f),
			new base_ratio_t(960, 640, (int) (6.5 * FRACUNIT), 48 * 15 / 16,
					1.25f) // 5:4
	};

	/** Used to determine the view center and projection in view units fixed_t */
	protected int centerxfrac, centeryfrac, projection;

	/** just for profiling purposes */
	protected int framecount;

	protected int sscount;
	protected int linecount;
	protected int loopcount;

	/** fixed_t */
	protected int viewx, viewy, viewz;

	// MAES: an exception to strict type safety. These are used only in here,
	// anyway (?) and have no special functions.
	// Plus I must use them as indexes. angle_t
	protected long viewangle;

	/** fixed */
	protected int viewcos, viewsin;

	protected player_t viewplayer;

	// 0 = high, 1 = low. Normally only the menu and the interface can change
	// that.
	protected int detailshift;

	//
	// precalculated math tables
	//
	protected long clipangle;

	// Set to 2*clipangle later.
	protected long CLIPANGLE2;

	// The viewangletox[viewangle + FINEANGLES/4] lookup
	// maps the visible view angles to screen X coordinates,
	// flattening the arc to a flat projection plane.
	// There will be many angles mapped to the same X.

	protected final int[] viewangletox = new int[FINEANGLES / 2];

	/**
	 * The xtoviewangle[] table maps a screen pixel to the lowest viewangle that
	 * maps back to x ranges from clipangle to -clipangle.
	 */
	protected long[] xtoviewangle;// MAES: to resize

	// UNUSED.
	// The finetangentgent[angle+FINEANGLES/4] table
	// holds the fixed_t tangent values for view angles,
	// ranging from MININT to 0 to MAXINT.
	// fixed_t finetangent[FINEANGLES/2];

	// fixed_t finesine[5*FINEANGLES/4];
	// MAES: uh oh. So now all these ints must become finesines? fuck that.
	// Also wtf @ this hack....this points to approx 1/4th of the finesine
	// table, but what happens if I read past it?
	// int[] finecosine = finesine[FINEANGLES/4];

	/*
	 * MAES: what's going on with light tables here. OK...so these should be
	 * "unsigned bytes", since, after all, they'll be used as pointers inside an
	 * array to finally pick a color, so they should be expanded to shorts.
	 */

	protected byte[][][] scalelight = new byte[LIGHTLEVELS][MAXLIGHTSCALE][];
	protected byte[][] scalelightfixed = new byte[MAXLIGHTSCALE][];
	protected byte[][][] zlight = new byte[LIGHTLEVELS][MAXLIGHTZ][];

	// bumped light from gun blasts
	protected static int extralight;

	// //////////// SOME UTILITY METHODS /////////////

	/**
	 * Assigns a point of view before calling PointToAngle
	 * 
	 * CAREFUL: this isn't a pure function, as it alters the renderer's state!
	 * 
	 * */

	public final long PointToAngle2(int x1, int y1, int x2, int y2) {
		// Careful with assignments...
		viewx = x1;
		viewy = y1;

		return PointToAngle(x2, y2);
	}

	//
	// R_InitPointToAngle
	//
	/*
	 * protected final void InitPointToAngle () { // UNUSED - now getting from
	 * tables.c if (false){ int i; long t; float f; // // slope (tangent) to
	 * angle lookup // for (i=0 ; i<=SLOPERANGE ; i++) { f = (float) Math.atan(
	 * (double)(i/SLOPERANGE )/(3.141592657*2)); t = (long) (0xffffffffL*f);
	 * tantoangle[i] = (int) t; } } }
	 */

	/**
	 * Public, static, stateless version of PointToAngle2. Call this one when
	 * "renderless" use of PointToAngle2 is required.
	 * 
	 */

	public final static long PointToAngle(int viewx, int viewy, int x, int y) {
		// MAES: note how we don't use &BITS32 here. That is because
		// we know that the maximum possible value of tantoangle is angle
		// This way, we are actually working with vectors emanating
		// from our current position.
		x -= viewx;
		y -= viewy;

		if ((x == 0) && (y == 0))
			return 0;

		if (x >= 0) {
			// x >=0
			if (y >= 0) {
				// y>= 0

				if (x > y) {
					// octant 0
					return tantoangle[SlopeDiv(y, x)];
				} else {
					// octant 1
					return (ANG90 - 1 - tantoangle[SlopeDiv(x, y)]);
				}
			} else {
				// y<0
				y = -y;

				if (x > y) {
					// octant 8
					return (-tantoangle[SlopeDiv(y, x)]);
				} else {
					// octant 7
					return (ANG270 + tantoangle[SlopeDiv(x, y)]);
				}
			}
		} else {
			// x<0
			x = -x;

			if (y >= 0) {
				// y>= 0
				if (x > y) {
					// octant 3
					return (ANG180 - 1 - tantoangle[SlopeDiv(y, x)]);
				} else {
					// octant 2
					return (ANG90 + tantoangle[SlopeDiv(x, y)]);
				}
			} else {
				// y<0
				y = -y;

				if (x > y) {
					// octant 4
					return (ANG180 + tantoangle[SlopeDiv(y, x)]);
				} else {
					// octant 5
					return (ANG270 - 1 - tantoangle[SlopeDiv(x, y)]);
				}
			}
		}
		// This is actually unreachable.
		// return 0;
	}

	protected final long PointToAngle(int x, int y) {
		// MAES: note how we don't use &BITS32 here. That is because
		// we know that the maximum possible value of tantoangle is angle
		// This way, we are actually working with vectors emanating
		// from our current position.
		x -= viewx;
		y -= viewy;

		if ((x == 0) && (y == 0))
			return 0;

		if (x >= 0) {
			// x >=0
			if (y >= 0) {
				// y>= 0

				if (x > y) {
					// octant 0
					return tantoangle[SlopeDiv(y, x)];
				} else {
					// octant 1
					return (ANG90 - 1 - tantoangle[SlopeDiv(x, y)]);
				}
			} else {
				// y<0
				y = -y;

				if (x > y) {
					// octant 8
					return (-tantoangle[SlopeDiv(y, x)]);
				} else {
					// octant 7
					return (ANG270 + tantoangle[SlopeDiv(x, y)]);
				}
			}
		} else {
			// x<0
			x = -x;

			if (y >= 0) {
				// y>= 0
				if (x > y) {
					// octant 3
					return (ANG180 - 1 - tantoangle[SlopeDiv(y, x)]);
				} else {
					// octant 2
					return (ANG90 + tantoangle[SlopeDiv(x, y)]);
				}
			} else {
				// y<0
				y = -y;

				if (x > y) {
					// octant 4
					return (ANG180 + tantoangle[SlopeDiv(y, x)]);
				} else {
					// octant 5
					return (ANG270 - 1 - tantoangle[SlopeDiv(x, y)]);
				}
			}
		}
		// This is actually unreachable.
		// return 0;
	}

	/**
	 * R_ScaleFromGlobalAngle Returns the texture mapping scale for the current
	 * line (horizontal span) at the given angle. rw_distance must be calculated
	 * first.
	 */

	protected final int ScaleFromGlobalAngle(long visangle) {
		int scale; // fixed_t
		long anglea;
		long angleb;
		int sinea;
		int sineb;
		int num; // fixed_t
		int den;

		// UNUSED
		/*
		 * { fixed_t dist; fixed_t z; fixed_t sinv; fixed_t cosv;
		 * 
		 * sinv = finesine[(visangle-rw_normalangle)>>ANGLETOFINESHIFT]; dist =
		 * FixedDiv (rw_distance, sinv); cosv =
		 * finecosine[(viewangle-visangle)>>ANGLETOFINESHIFT]; z = abs(FixedMul
		 * (dist, cosv)); scale = FixedDiv(projection, z); return scale; }
		 */

		anglea = (ANG90 + visangle - viewangle) & BITS32;
		angleb = (ANG90 + visangle - rw_normalangle) & BITS32;

		// both sines are allways positive
		sinea = finesine(anglea);
		sineb = finesine(angleb);
		num = FixedMul(projection, sineb) << detailshift;
		den = FixedMul(rw_distance, sinea);

		if (den > num >> 16) {
			scale = FixedDiv(num, den);

			if (scale > 64 * FRACUNIT)
				scale = 64 * FRACUNIT;
			else if (scale < 256)
				scale = 256;
		} else
			scale = 64 * FRACUNIT;

		return scale;
	}

	//
	// R_InitTables
	//
	protected final void InitTables() {
		// UNUSED: now getting from tables.c
		/*
		 * int i; float a; float fv; int t;
		 * 
		 * // viewangle tangent table for (i=0 ; i<FINEANGLES/2 ; i++) { a =
		 * (i-FINEANGLES/4+0.5)*PI*2/FINEANGLES; fv = FRACUNIT*tan (a); t = fv;
		 * finetangent[i] = t; }
		 * 
		 * // finesine table for (i=0 ; i<5*FINEANGLES/4 ; i++) { // OPTIMIZE:
		 * mirro.. a = (i+0.5)*PI*2/FINEANGLES; t = FRACUNIT*sin (a);
		 * finesine[i] = t; }
		 */

	}

	/**
	 * R_PointToDist
	 * 
	 * @param x
	 *            fixed_t
	 * @param y
	 *            fixed_t
	 * @return
	 */

	protected final int PointToDist(int x, int y) {
		int angle;
		int dx;
		int dy;
		int temp;
		int dist;

		dx = Math.abs(x - viewx);
		dy = Math.abs(y - viewy);

		// If something is farther north/south than west/east, it gets swapped.
		// Probably as a crude way to avoid divisions by zero. This divides
		// the field into octants, rather than quadrants, where the biggest
		// angle to
		// consider is 45...right? So dy/dx can never exceed 1.0, in theory.

		if (dy > dx) {
			temp = dx;
			dx = dy;
			dy = temp;
		}

		// If one or both of the distances are *exactly* zero at this point,
		// then this means that the wall is in your face anyway, plus we want to
		// avoid a division by zero. So you get zero.
		if (dx == 0)
			return 0;

		/*
		 * If dx is zero, this is going to bomb. Fixeddiv will return MAXINT aka
		 * 7FFFFFFF, >> DBITS will make it 3FFFFFF, which is more than enough to
		 * break tantoangle[].
		 * 
		 * In the original C code, this probably didn't matter: there would
		 * probably be garbage orientations thrown all around. However this is
		 * unacceptable in Java. OK, so the safeguard above prevents that.
		 * Still... this method is only called once per visible wall per frame,
		 * so one check more or less at this point won't change much. It's
		 * better to be safe than sorry.
		 */

		// This effectively limits the angle to
		angle = Math.max(FixedDiv(dy, dx), 2048) >> DBITS;
		// angle=FixedDiv(dy,dx)>>DBITS;

		// Since the division will be 0xFFFF at most, DBITS will restrict
		// the maximum angle index to 7FF, about 45, so adding ANG90 with
		// no other safeguards is OK.
		angle = (int) ((tantoangle[angle] + ANG90) >> ANGLETOFINESHIFT);

		// use as cosine
		dist = FixedDiv(dx, finesine[angle]);

		return dist;
	}

	// /////////////// COLORMAPS ///////////////////////////////

	protected byte[][] translationtables;

	// /////////////// COMMON RENDERING GLOBALS ////////////////

	/*
	 * ON COLORMAPS: they are supposed to be
	 * "from this color to some other color" mappings. "Color" means an index in
	 * the palette, like those used in screen. Their contents should be bytes,
	 * although their indexing has to be "unsigned bytes" the very least.
	 * 
	 * Lengths: for some reas
	 */

	/** "peg" this to the one from RendererData */
	protected byte[][] colormaps;

	// // FROM SEGS ////
	/** angle_t, used after adding ANG90 in StoreWallRange */
	protected long rw_normalangle;

	// OPTIMIZE: closed two sided lines as single sided

	/** True if any of the segs textures might be visible. */
	protected boolean segtextured;

	/** False if the back side is the same plane. */
	protected boolean markfloor, markceiling;

	protected boolean maskedtexture;
	protected int toptexture;
	protected int bottomtexture;
	protected int midtexture;

	/** angle to line origin */
	protected long rw_angle1;

	//
	// regular wall
	//
	protected int rw_x;
	protected int rw_stopx;
	protected long rw_centerangle; // angle_t
	/** fixed_t */
	protected int rw_offset, rw_distance, rw_scale, rw_scalestep,
			rw_midtexturemid, rw_toptexturemid, rw_bottomtexturemid;

	protected int worldtop;
	protected int worldbottom;
	protected int worldhigh;
	protected int worldlow;

	/** fixed_t */
	protected int pixhigh, pixlow, pixhighstep, pixlowstep, topfrac, topstep,
			bottomfrac, bottomstep;

	/** lighttable_t** */
	protected byte[][] walllights;

	protected short[] maskedtexturecol;
	protected int pmaskedtexturecol = 0;

	// /// FROM BSP /////////

	/** pointer to drawsegs */
	protected int ds_p;

	protected drawseg_t[] drawsegs;

	/** The sectors of the line currently being considered */
	protected sector_t frontsector, backsector;

	protected seg_t curline;
	protected side_t sidedef;
	protected line_t linedef;

	// /////////////////// Renderer Data ////////////////////////

	/** needed for pre rendering (fixed_t[]) */
	protected int[] spritewidth, spriteoffset, spritetopoffset;

	protected interface colfunc_t {
		public void invoke();
	}

	// ///////////////////// COLUMN AND SPAN FUNCTION
	// ////////////////////////////////////////

	// Fuck that shit. Amma gonna do it the fastest way possible.

	protected DoomColumnFunction<byte[]> colfunc;
	protected DoomColumnFunction<byte[]>  basecolfunc;
	protected DoomColumnFunction<byte[]>  maskedcolfunc;
	protected DoomColumnFunction<byte[]>  fuzzcolfunc;
	protected DoomColumnFunction<byte[]>  transcolfunc;
	protected DoomColumnFunction<byte[]>  glasscolfunc;
	protected DoomColumnFunction<byte[]>  playercolfunc;
	protected DoomColumnFunction<byte[]> skycolfunc;
	protected DoomSpanFunction<byte[]>  spanfunc;

	protected DoomColumnFunction<byte[]>  DrawTranslatedColumn;
	protected DoomColumnFunction<byte[]>  DrawColumnPlayer;
	protected DoomColumnFunction<byte[]>  DrawColumnSkies;
	protected DoomColumnFunction<byte[]>  DrawColumnSkiesLow;
	protected DoomColumnFunction<byte[]>  DrawFuzzColumn;
	protected DoomColumnFunction<byte[]>  DrawFuzzColumnLow;
	protected DoomColumnFunction<byte[]>  DrawColumn;
	protected DoomColumnFunction<byte[]>  DrawColumnLow;	
	protected DoomColumnFunction<byte[]>  DrawColumnMasked;
	protected DoomColumnFunction<byte[]>  DrawColumnMaskedLow;
	protected DoomColumnFunction<byte[]>  DrawTLColumn;

	/** to be set in UnifiedRenderer */
	protected DoomSpanFunction<byte[]> DrawSpan, DrawSpanLow;

	//////////////// r_draw methods //////////////

	/**
	 * Copy a screen buffer. Actually, it's hardcoded to copy stuff from screen
	 * 1 to screen 0. Used to overlay stuff like beveled edges that don't need
	 * to be updated that often. *
	 * 
	 * LFB copy. This might not be a good idea if memcpy is not optiomal, e.g.
	 * byte by byte on a 32bit CPU, as GNU GCC/Linux libc did at one point.
	 */
	public void VideoErase(int ofs, int count) {

		// memcpy (screens[0]+ofs, screens[1]+ofs, count);
		System.arraycopy(V.getScreen(DoomVideoRenderer.SCREEN_BG), ofs, V.getScreen(DoomVideoRenderer.SCREEN_FG), ofs, count);

	}

	/*
	 * R_DrawViewBorder Draws the border around the view for different size
	 * windows?
	 */

	public void DrawViewBorder() {
		int top;
		int side;
		int ofs;
		int i;

		if (scaledviewwidth == SCREENWIDTH)
			return;

		top = ((SCREENHEIGHT - DM.ST.getHeight()) - viewheight) / 2;
		side = (SCREENWIDTH - scaledviewwidth) / 2;

		// copy top and one line of left side
		this.VideoErase(0, top * SCREENWIDTH + side);

		// copy one line of right side and bottom
		ofs = (viewheight + top) * SCREENWIDTH - side;
		this.VideoErase(ofs, top * SCREENWIDTH + side);

		// copy sides using wraparound
		ofs = top * SCREENWIDTH + SCREENWIDTH - side;
		side <<= 1;

		for (i = 1; i < viewheight; i++) {
			this.VideoErase(ofs, side);
			ofs += SCREENWIDTH;
		}

		// ?
		V.MarkRect(0, 0, SCREENWIDTH, SCREENHEIGHT - DM.ST.getHeight());
	}

	public void ExecuteSetViewSize() {
		int cosadj;
		int dy;
		int i;
		int j;
		int level;
		int startmap;

		setsizeneeded = false;

		// 11 Blocks means "full screen"

		if (setblocks == 11) {
			scaledviewwidth = SCREENWIDTH;
			viewheight = SCREENHEIGHT;
		} else {
			scaledviewwidth = setblocks * (SCREENWIDTH / 10);
			// Height can only be a multiple of 8.
			viewheight = (short) ((setblocks
					* (SCREENHEIGHT - DM.ST.getHeight()) / 10) & ~7);
		}

		skydcvars.viewheight=maskedcvars.viewheight=dcvars.viewheight=viewheight;
		
		detailshift = setdetail;
		viewwidth = scaledviewwidth >> detailshift;

		centery = viewheight / 2;
		centerx = viewwidth / 2;
		centerxfrac = (centerx << FRACBITS);
		centeryfrac = (centery << FRACBITS);
		projection = centerxfrac;

		skydcvars.centery=maskedcvars.centery=dcvars.centery=centery;
		
		// High detail
		if (detailshift == 0) {

			colfunc = basecolfunc = DrawColumn;
			maskedcolfunc = DrawColumnMasked;
			fuzzcolfunc = DrawFuzzColumn;
			transcolfunc = DrawTranslatedColumn;
			glasscolfunc = DrawTLColumn;
			playercolfunc = DrawColumnPlayer;
			skycolfunc= DrawColumnSkies;
			spanfunc = DrawSpan;
		} else {
			// Low detail
			colfunc = basecolfunc = DrawColumnLow;
			maskedcolfunc = DrawColumnMaskedLow;
			fuzzcolfunc = DrawFuzzColumnLow;
			transcolfunc = DrawTranslatedColumn;
			glasscolfunc = DrawTLColumn;
			playercolfunc = DrawColumnLow;
			skycolfunc= DrawColumnSkiesLow;
			spanfunc = DrawSpanLow;

		}

		InitBuffer(scaledviewwidth, viewheight);

		InitTextureMapping();

		// psprite scales
		// pspritescale = FRACUNIT*viewwidth/SCREENWIDTH;
		// pspriteiscale = FRACUNIT*SCREENWIDTH/viewwidth;

		pspritescale = (int) (FRACUNIT * ((float) SCREEN_MUL * viewwidth) / SCREENWIDTH);
		pspriteiscale = (int) (FRACUNIT * (SCREENWIDTH / (viewwidth * (float) SCREEN_MUL)));
		skyscale = (int) (FRACUNIT * (SCREENWIDTH / (viewwidth * (float) SCREEN_MUL)));

		BOBADJUST = (int) (this.vs.getSafeScaling() << 15);
		WEAPONADJUST = (int) ((SCREENWIDTH / (2 * SCREEN_MUL)) * FRACUNIT);

		// thing clipping
		for (i = 0; i < viewwidth; i++)
			screenheightarray[i] = (short) viewheight;

		// planes
		for (i = 0; i < viewheight; i++) {			
			dy = ((i - viewheight / 2) << FRACBITS) + FRACUNIT / 2;
			dy = Math.abs(dy);
			MyPlanes.getYslope()[i] = FixedDiv((viewwidth << detailshift) / 2
					* FRACUNIT, dy);
			// MyPlanes.yslopef[i] = ((viewwidth<<detailshift)/2)/ dy;
		}

		// double cosadjf;
		for (i = 0; i < viewwidth; i++) {
			// MAES: In this spot we must interpet it as SIGNED, else it's
			// pointless, right?
			// MAES: this spot caused the "warped floor bug", now fixed. Don't
			// forget xtoviewangle[i]!
			cosadj = Math.abs(finecosine(xtoviewangle[i]));
			// cosadjf =
			// Math.abs(Math.cos((double)xtoviewangle[i]/(double)0xFFFFFFFFL));
			MyPlanes.getDistScale()[i] = FixedDiv(FRACUNIT, cosadj);
			// MyPlanes.distscalef[i] = (float) (1.0/cosadjf);
		}

		// Calculate the light levels to use
		// for each level / scale combination.
		for (i = 0; i < LIGHTLEVELS; i++) {
			startmap = ((LIGHTLEVELS - 1 - i) * 2) * NUMCOLORMAPS / LIGHTLEVELS;
			for (j = 0; j < MAXLIGHTSCALE; j++) {
				level = startmap - j * SCREENWIDTH / (viewwidth << detailshift)
						/ DISTMAP;
				if (level < 0) level = 0;
				if (level >= NUMCOLORMAPS)level = NUMCOLORMAPS - 1;
				scalelight[i][j] = colormaps[level];
			}
		}

		for (i = 0; i < viewwidth; i++) {
			BLANKFLOORCLIP[i] = (short) viewheight;
			BLANKCEILINGCLIP[i] = -1;
		}

	}

	/**
	 * R_FillBackScreen Fills the back screen with a pattern for variable screen
	 * sizes Also draws a beveled edge. This is actually stored in screen 1, and
	 * is only OCCASIONALLY written to screen 0 (the visible one) by calling
	 * R_VideoErase.
	 */
	
	public void FillBackScreen() {
		flat_t src;
		byte[] dest;
		int x;
		int y;
		patch_t patch;

		// DOOM border patch.
		String name1 = "FLOOR7_2";

		// DOOM II border patch.
		String name2 = "GRNROCK";

		String name;

		if (scaledviewwidth == SCREENWIDTH)
			return;

		if (DM.isCommercial())
			name = name2;
		else
			name = name1;

		/* This is a flat we're reading here */
		src = (flat_t) (W.CacheLumpName(name, PU_CACHE, flat_t.class));
		dest = V.getScreen(DoomVideoRenderer.SCREEN_BG);
		int destPos = 0;

		/* This part actually draws the border itself, without bevels */

		for (y = 0; y < SCREENHEIGHT - DM.ST.getHeight(); y++) {
			for (x = 0; x < SCREENWIDTH / 64; x++) {
				// memcpy (dest, src+((y&63)<<6), 64);
				System.arraycopy(src.data, ((y & 63) << 6), dest, destPos, 64);
				destPos += 64;
			}

			if ((SCREENWIDTH & 63) != 0) {
				// memcpy (dest, src+((y&63)<<6), SCREENWIDTH&63);
				System.arraycopy(src.data, ((y & 63) << 6), dest, destPos,
						SCREENWIDTH & 63);

				destPos += (SCREENWIDTH & 63);
			}
		}

		patch = (patch_t) W.CachePatchName("BRDR_T", PU_CACHE);

		for (x = 0; x < scaledviewwidth; x += 8)
			V.DrawPatch(viewwindowx + x, viewwindowy - 8, 1, patch);
		patch = (patch_t) W.CachePatchName("BRDR_B", PU_CACHE);

		for (x = 0; x < scaledviewwidth; x += 8)
			V.DrawPatch(viewwindowx + x, viewwindowy + viewheight, 1, patch);
		patch = (patch_t) W.CachePatchName("BRDR_L", PU_CACHE);

		for (y = 0; y < viewheight; y += 8)
			V.DrawPatch(viewwindowx - 8, viewwindowy + y, 1, patch);
		patch = (patch_t) W.CachePatchName("BRDR_R", PU_CACHE);

		for (y = 0; y < viewheight; y += 8)
			V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy + y, 1,
					patch);

		// Draw beveled edge. Top-left
		V.DrawPatch(viewwindowx - 8, viewwindowy - 8, 1,
				(patch_t) W.CachePatchName("BRDR_TL", PU_CACHE));

		// Top-right.
		V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy - 8, 1,
				(patch_t) W.CachePatchName("BRDR_TR", PU_CACHE));

		// Bottom-left
		V.DrawPatch(viewwindowx - 8, viewwindowy + viewheight, 1,
				(patch_t) W.CachePatchName("BRDR_BL", PU_CACHE));
		// Bottom-right.
		V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy + viewheight, 1,
				(patch_t) W.CachePatchName("BRDR_BR", PU_CACHE));
	}

	/**
	 * R_InitBuffer Creates lookup tables that avoid multiplies and other
	 * hazzles for getting the framebuffer address of a pixel to draw. MAES:
	 * this is "pinned" to screen[0] of a Video Renderer. We will handle this
	 * differently elsewhere...
	 */

	protected final void InitBuffer(int width, int height) {
		int i;

		// Handle resize,
		// e.g. smaller view windows
		// with border and/or status bar.
		viewwindowx = (SCREENWIDTH - width) >> 1;

		// Column offset. For windows.
		for (i = 0; i < width; i++)
			columnofs[i] = viewwindowx + i;

		// SamE with base row offset.
		if (width == SCREENWIDTH)
			viewwindowy = 0;
		else
			viewwindowy = (SCREENHEIGHT - DM.ST.getHeight() - height) >> 1;

		// Preclaculate all row offsets.
		for (i = 0; i < height; i++)
			ylookup[i] = /* screens[0] + */(i + viewwindowy) * SCREENWIDTH;
	}

	// //////INIT STUFF /////////////////

	/**
	 * R_InitTextureMapping
	 * 
	 * Not moved into the TextureManager because it's tighly coupled to the
	 * visuals, rather than textures. Perhaps the name is not the most
	 * appropriate.
	 * 
	 */
	public final void InitTextureMapping() {
		int i, x, t;
		int focallength; // fixed_t
		int fov = FIELDOFVIEW;

		// For widescreen displays, increase the FOV so that the middle part of
		// the
		// screen that would be visible on a 4:3 display has the requested FOV.
		if (wide_centerx != centerx) { // wide_centerx is what centerx would be
										// if the display was not widescreen
			fov = (int) (Math.atan((double) centerx
					* Math.tan((double) fov * Math.PI / FINEANGLES)
					/ (double) wide_centerx)
					* FINEANGLES / Math.PI);
			if (fov > 130 * FINEANGLES / 360)
				fov = 130 * FINEANGLES / 360;
		}

		// Use tangent table to generate viewangletox:
		// viewangletox will give the next greatest x
		// after the view angle.
		//
		// Calc focallength
		// so FIELDOFVIEW angles covers SCREENWIDTH.
		focallength = FixedDiv(centerxfrac, finetangent[QUARTERMARK
				+ FIELDOFVIEW / 2]);

		for (i = 0; i < FINEANGLES / 2; i++) {
			if (finetangent[i] > FRACUNIT * 2)
				t = -1;
			else if (finetangent[i] < -FRACUNIT * 2)
				t = viewwidth + 1;
			else {
				t = FixedMul(finetangent[i], focallength);
				t = (centerxfrac - t + FRACUNIT - 1) >> FRACBITS;

				if (t < -1)
					t = -1;
				else if (t > viewwidth + 1)
					t = viewwidth + 1;
			}
			viewangletox[i] = t;
		}

		// Scan viewangletox[] to generate xtoviewangle[]:
		// xtoviewangle will give the smallest view angle
		// that maps to x.
		for (x = 0; x <= viewwidth; x++) {
			i = 0;
			while (viewangletox[i] > x)
				i++;
			xtoviewangle[x] = addAngles((i << ANGLETOFINESHIFT), -ANG90);
		}

		// Take out the fencepost cases from viewangletox.
		for (i = 0; i < FINEANGLES / 2; i++) {
			t = FixedMul(finetangent[i], focallength);
			t = centerx - t;

			if (viewangletox[i] == -1)
				viewangletox[i] = 0;
			else if (viewangletox[i] == viewwidth + 1)
				viewangletox[i] = viewwidth;
		}

		clipangle = xtoviewangle[0];
		// OPTIMIZE: assign constant for optimization.
		CLIPANGLE2 = (2 * clipangle) & BITS32;
	}

	//
	// R_InitLightTables
	// Only inits the zlight table,
	// because the scalelight table changes with view size.
	//
	protected final static int DISTMAP = 2;

	protected final void InitLightTables() {
		int i;
		int j;
		int level;
		int startmap;
		int scale;

		// Calculate the light levels to use
		// for each level / distance combination.
		for (i = 0; i < LIGHTLEVELS; i++) {
			startmap = ((LIGHTLEVELS - 1 - i) * 2) * NUMCOLORMAPS / LIGHTLEVELS;
			for (j = 0; j < MAXLIGHTZ; j++) {
				scale = FixedDiv((SCREENWIDTH / 2 * FRACUNIT),
						(j + 1) << LIGHTZSHIFT);
				scale >>= LIGHTSCALESHIFT;
				level = startmap - scale / DISTMAP;

				if (level < 0)
					level = 0;

				if (level >= NUMCOLORMAPS)
					level = NUMCOLORMAPS - 1;

				// zlight[i][j] = colormaps + level*256;
				zlight[i][j] = colormaps[level];
			}
		}
	}

	protected static final int TSC = 12; /*
										 * number of fixed point digits in
										 * filter percent
										 */
	byte[] main_tranmap;
	
	/** A faster implementation of the tranmap calculations. Almost
	 * 10x faster than the old one!
	 * 
	 * @param progress
	 */

	protected void R_InitTranMap(int progress) {
		int lump = W.CheckNumForName("TRANMAP");

		long ta=System.nanoTime();
		int p;
		String tranmap;

		// PRIORITY: a map file has been specified from commandline. Try to read
		// it. If OK, this trumps even those specified in lumps.
		
		if ((p = DM.CM.CheckParm("-tranmap")) != 0) {
			if ((tranmap = DM.CM.getArgv(p + 1)) != null) {
				if (C2JUtils.testAccess(tranmap, "r")) {
					System.out
							.printf("Translucency map file %s specified in -tranmap arg. Attempting to use...",
									tranmap);
					main_tranmap = new byte[256 * 256]; // killough 4/11/98
					int result = MenuMisc.ReadFile(tranmap, main_tranmap);
					if (result > 0)
						return;
					System.out.print("...failure.\n");
				}
			}
		}

		// Next, if a tranlucency filter map lump is present, use it
		if (lump != -1) { // Set a pointer to the translucency filter maps.
			System.out
					.print("Translucency map found in lump. Attempting to use...");
			// main_tranmap=new byte[256*256]; // killough 4/11/98
			main_tranmap = W.CacheLumpNumAsRawBytes(lump, Defines.PU_STATIC); // killough
																				// 4/11/98
			// Tolerate 64K or more.
			if (main_tranmap.length >= 0x10000)
				return;
			System.out.print("...failure.\n"); // Not good, try something else.
		}

		// A default map file already exists. Try to read it.
		if (C2JUtils.testAccess("tranmap.dat", "r")) {
			System.out
					.print("Translucency map found in default tranmap.dat file. Attempting to use...");
			main_tranmap = new byte[256 * 256]; // killough 4/11/98
			int result = MenuMisc.ReadFile("tranmap.dat", main_tranmap);
			if (result > 0)
				return; // Something went wrong, so fuck that.
		}

		// Nothing to do, so we must synthesize it from scratch. And, boy, is it
		// slooow.
		{ // Compose a default transparent filter map based on PLAYPAL.
			System.out
					.print("Computing translucency map from scratch...that's gonna be SLOW...");
			byte[] playpal = W.CacheLumpNameAsRawBytes("PLAYPAL",
					Defines.PU_STATIC);
			main_tranmap = new byte[256 * 256]; // killough 4/11/98
			int[] basepal = new int[3*256];
			int[] mixedpal = new int[3*256 * 256];

			main_tranmap = new byte[256 * 256];

			// Init array of base colors.
			for (int i = 0; i < 256; i++) {
				basepal[3*i] = 0Xff&playpal[i * 3];
				basepal[1+3*i] = 0Xff&playpal[1+i * 3];
				basepal[2+3*i] = 0Xff&playpal[2+i * 3];
			}

			// Init array of mixed colors. These are true RGB.
			// The diagonal of this array will be the original colors.
			
			for (int i = 0; i < 256*3; i+=3) {
				for (int j = 0; j < 256*3; j+=3) {
					mixColors(basepal, basepal,mixedpal,i,j, j*256+i);
					
				}
			}

			// Init distance map. Every original palette colour has a
			// certain distance from all the others. The diagonal is zero.
			// The interpretation is that e.g. the mixture of color 2 and 8 will
			// have a RGB value, which is closest to euclidean distance to
			// e.g. original color 9. Therefore we should put "9" in the (2,8)
			// and (8,2) cells of the tranmap.

			final float[] tmpdist = new float[256];

			for (int a = 0; a < 256; a++) {
				for (int b = a; b < 256; b++) {
					// We evaluate the mixture of a and b
					// Construct distance table vs all of the ORIGINAL colors.
					for (int k = 0; k < 256; k++) {
						tmpdist[k] = colorDistance(mixedpal, basepal,3*(a  + b*256),
								k*3);
						}

					main_tranmap[(a << 8) | b] = (byte) findMin(tmpdist);
					main_tranmap[(b << 8) | a] = main_tranmap[(a << 8) | b];
				}
			}
			System.out.print("...done\n");
			if (MenuMisc.WriteFile("tranmap.dat", main_tranmap,
					main_tranmap.length))
				System.out
						.print("TRANMAP.DAT saved to disk for your convenience! Next time will be faster.\n");
		}
		long b=System.nanoTime();

		System.out.printf("Tranmap %d\n",(b-ta)/1000000);
	}

	
	/** Mixes two RGB colors. Nuff said */

	protected final void mixColors(int[] a, int[] b, int[] c,int pa,int pb, int pc) {
		c[pc] = (a[pa] + b[pb]) / 2;
		c[pc+1] = (a[pa+1] + b[pb+1]) / 2;
		c[pc+2] = (a[pa+2] + b[pb+2]) / 2;

	}
	
	/** Returns the euclidean distance of two RGB colors. Nuff said */
	
	protected final float colorDistance(int[] a, int[] b, int pa, int pb) {
		return (float) Math.sqrt((a[pa]-b[pb])*(a[pa]-b[pb])+
				(a[pa+1]-b[pb+1])*(a[pa+1]-b[pb+1])+
				(a[pa+2]-b[pb+2])*(a[pa+2]-b[pb+2]));
	}

	protected final int findMin(float[] a) {
		int minindex = 0;
		float min = Float.POSITIVE_INFINITY;

		for (int i = 0; i < a.length; i++)
			if (a[i] < min) {
				min = a[i];
				minindex = i;
			}

		return minindex;

	}

	/**
	 * R_DrawMaskedColumn Used for sprites and masked mid textures. Masked
	 * means: partly transparent, i.e. stored in posts/runs of opaque pixels.
	 * 
	 * NOTE: this version accepts raw bytes, in case you know what you're doing.
	 */

	protected final void DrawMaskedColumn(byte[] column) {
		int topscreen;
		int bottomscreen;
		int basetexturemid; // fixed_t
		int topdelta;
		int length;

		basetexturemid = maskedcvars.dc_texturemid;
		// That's true for the whole column.
		maskedcvars.dc_source = column;
		int pointer = 0;

		// for each post...
		while ((topdelta = 0xFF & column[pointer]) != 0xFF) {
			// calculate unclipped screen coordinates
			// for post
			topscreen = sprtopscreen + spryscale * topdelta;
			length = 0xff & column[pointer + 1];
			bottomscreen = topscreen + spryscale * length;

			maskedcvars.dc_yl = (topscreen + FRACUNIT - 1) >> FRACBITS;
			maskedcvars.dc_yh = (bottomscreen - 1) >> FRACBITS;

			if (maskedcvars.dc_yh >= mfloorclip[p_mfloorclip + maskedcvars.dc_x])
			    maskedcvars.dc_yh = mfloorclip[p_mfloorclip + maskedcvars.dc_x] - 1;

			if (maskedcvars.dc_yl <= mceilingclip[p_mceilingclip + maskedcvars.dc_x])
			    maskedcvars.dc_yl = mceilingclip[p_mceilingclip + maskedcvars.dc_x] + 1;

			// killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
			if (maskedcvars.dc_yl <= maskedcvars.dc_yh && maskedcvars.dc_yh < viewheight) {
				// Set pointer inside column to current post's data
				// Rremember, it goes {postlen}{postdelta}{pad}[data]{pad}
			    maskedcvars.dc_source_ofs = pointer + 3;
			    maskedcvars.dc_texturemid = basetexturemid - (topdelta << FRACBITS);

				// Drawn by either R_DrawColumn
				// or (SHADOW) R_DrawFuzzColumn.
			    maskedcvars.dc_texheight=0; // Killough

				colfunc.invoke();
			}
			pointer += length + 4;
		}

		maskedcvars.dc_texturemid = basetexturemid;
	}

	/**
	 * R_DrawMaskedColumnSinglePost. Used to handle some special cases where 
	 * cached columns get used as "masked" middle textures. Will be treated
	 * as a single-run post of capped length.
	 * 
	 */

	/*
	protected final void DrawCompositeColumnPost(byte[] column) {
			int topscreen;
			int bottomscreen;
			int basetexturemid; // fixed_t
			int topdelta=0; // Fixed value
			int length;

			basetexturemid = dc_texturemid;
			// That's true for the whole column.
			dc_source = column;

			// for each post...
			while (topdelta==0) {
				// calculate unclipped screen coordinates
				// for post
				topscreen = sprtopscreen + spryscale * 0;
				length = column.length;
				bottomscreen = topscreen + spryscale * length;

				dc_yl = (topscreen + FRACUNIT - 1) >> FRACBITS;
				dc_yh = (bottomscreen - 1) >> FRACBITS;

				if (dc_yh >= mfloorclip[p_mfloorclip + dc_x])
					dc_yh = mfloorclip[p_mfloorclip + dc_x] - 1;

				if (dc_yl <= mceilingclip[p_mceilingclip + dc_x])
					dc_yl = mceilingclip[p_mceilingclip + dc_x] + 1;

				// killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
				if (dc_yl <= dc_yh && dc_yh < viewheight) {
					// Set pointer inside column to current post's data
					// Rremember, it goes {postlen}{postdelta}{pad}[data]{pad}
					dc_source_ofs = 0; // pointer + 3;
					dc_texturemid = basetexturemid - (topdelta << FRACBITS);

					// Drawn by either R_DrawColumn
					// or (SHADOW) R_DrawFuzzColumn.
					dc_texheight=0; // Killough

					try {
					maskedcolfunc.invoke();
					} catch (Exception e){
						System.err.printf("Error rendering %d %d %d\n",
								dc_yl,dc_yh,dc_yh-dc_yl);
					}
				}
				topdelta--;
			}

			dc_texturemid = basetexturemid;
		} */
	
	/**
	 * R_DrawMaskedColumn Used for sprites and masked mid textures. Masked
	 * means: partly transparent, i.e. stored in posts/runs of opaque pixels.
	 * 
	 * FIXME: while it does work with "raw columns", if the initial post is
	 * drawn outside of the screen the rest appear screwed up.
	 * 
	 * SOLUTION: use the version taking raw byte[] arguments.
	 * 
	 */

	protected final void DrawMaskedColumn(column_t column) {
		int topscreen;
		int bottomscreen;
		int basetexturemid; // fixed_t

		basetexturemid = maskedcvars.dc_texturemid;
		// That's true for the whole column.
		maskedcvars.dc_source = column.data;
		// dc_source_ofs=0;

		// for each post...
		for (int i = 0; i < column.posts; i++) {
		    maskedcvars.dc_source_ofs = column.postofs[i];
			// calculate unclipped screen coordinates
			// for post
			topscreen = sprtopscreen + spryscale * column.postdeltas[i];
			bottomscreen = topscreen + spryscale * column.postlen[i];

			maskedcvars.dc_yl = (topscreen + FRACUNIT - 1) >> FRACBITS;
		maskedcvars.dc_yh = (bottomscreen - 1) >> FRACBITS;

			if (maskedcvars.dc_yh >= mfloorclip[p_mfloorclip + maskedcvars.dc_x])
			    maskedcvars.dc_yh = mfloorclip[p_mfloorclip + maskedcvars.dc_x] - 1;
			if (maskedcvars.dc_yl <= mceilingclip[p_mceilingclip + maskedcvars.dc_x])
			    maskedcvars.dc_yl = mceilingclip[p_mceilingclip + maskedcvars.dc_x] + 1;

			// killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
			if (maskedcvars.dc_yl <= maskedcvars.dc_yh && maskedcvars.dc_yh < maskedcvars.viewheight) {
				
				// Set pointer inside column to current post's data
				// Remember, it goes {postlen}{postdelta}{pad}[data]{pad}

			    maskedcvars.dc_texturemid = basetexturemid
						- (column.postdeltas[i] << FRACBITS);

				// Drawn by either R_DrawColumn or (SHADOW) R_DrawFuzzColumn.
				// MAES: when something goes bad here, it means that the following:
				//
				// fracstep = dc_iscale;
				// frac = dc_texturemid + (dc_yl - centery) * fracstep;
				// 
				// results in a negative initial frac number.				
				
				//try {
					maskedcolfunc.invoke();
				/*} catch (Exception e) {
					int fracstep=dc_iscale;
					int frac = dc_texturemid + (dc_yl - centery) * fracstep;
					System.err.printf("Problem! %d = %x + (%x - %x) * %x\n",
							frac,dc_texturemid, dc_yl, centery,
							dc_iscale);
					e.printStackTrace();
				} */
			}
		}

		maskedcvars.dc_texturemid = basetexturemid;
	}

	


	
	

	
	// //////////////////////////////// TEXTURE MANAGEMENT
	// /////////////////////////

	/**
	 * R_GetColumn original version: returns raw pointers to byte-based column
	 * data. Works for both masked and unmasked columns, but is not
	 * tutti-frutti-safe.
	 * 
	 * Use GetCachedColumn instead, if rendering non-masked stuff, which is also
	 * faster.
	 * 
	 * @throws IOException
	 * 
	 * 
	 */

	public byte[] GetColumn(int tex, int col) {
		int lump,ofs;

		col &= TexMan.getTexturewidthmask(tex);
		lump = TexMan.getTextureColumnLump(tex, col);
		ofs = TexMan.getTextureColumnOfs(tex, col);

		// It's always 0 for this kind of access.

		// Speed-increasing trick: speed up repeated accesses to the same
		// texture or patch, if they come from the same lump
		
		if (tex == lasttex && lump == lastlump) {
		    if (composite)
		        return lastpatch.columns[col].data;
		    else
		        return lastpatch.columns[ofs].data;
		    }

		// If pointing inside a non-zero, positive lump, then it's not a
		// composite texture. Read it from disk.
		if (lump > 0) {
			// This will actually return a pointer to a patch's columns.
			// That is, to the ONE column exactly.{
			// If the caller needs access to a raw column, we must point 3 bytes
			// "ahead".
			lastpatch = W.CachePatchNum(lump, PU_CACHE);
			lasttex = tex;
			lastlump=lump;
			composite=false;
			// If the column was a disk lump, use ofs.
			return lastpatch.columns[ofs].data;
		}
		
		// Problem. Composite texture requested as if it was masked
		// but it doesn't yet exist. Create it.
		if (TexMan.getMaskedComposite(tex) == null){
		    System.err.printf("Forced generation of composite %s\n",TexMan.CheckTextureNameForNum(tex),composite,col,ofs);
			TexMan.GenerateMaskedComposite(tex);
			System.err.printf("Composite patch %s %d\n",TexMan.getMaskedComposite(tex).name,TexMan.getMaskedComposite(tex).columns.length);
		}
		
		// Last resort. 
	    lastpatch = TexMan.getMaskedComposite(tex);
	    lasttex=tex;
	    composite=true;
	    lastlump=0;
		
		return lastpatch.columns[col].data;
	}

    // False: disk-mirrored patch. True: improper "transparent composite".
	private boolean composite = false;
	private int lasttex = -1;
	private int lastlump = -1;
	private patch_t lastpatch = null;

	/**
	 * R_GetColumn variation which is tutti-frutti proof. It only returns cached
	 * columns, and even pre-caches single-patch textures intead of trashing the
	 * WAD manager (should be faster, in theory).
	 * 
	 * Cannot be used for drawing masked textures, use classic GetColumn
	 * instead.
	 * 
	 * 
	 * @throws IOException
	 */
	@Override
	public final byte[] GetCachedColumn(int tex, int col) {
		int lump, ofs;

		col &= TexMan.getTexturewidthmask(tex);
		lump = TexMan.getTextureColumnLump(tex, col);
		ofs = TexMan.getTextureColumnOfs(tex, col);

		// In the case of cached columns, this is always 0.
		dcvars.dc_source_ofs = 0;

		// If pointing inside a non-zero, positive lump, then it's not a
		// composite texture.
		// Read from disk, and safeguard vs tutti frutti.
		if (lump > 0) {
			// This will actually return a pointer to a patch's columns.
			return TexMan.getRogueColumn(lump, ofs);
		}

		// Texture should be composite, but it doesn't yet exist. Create it.
		if (TexMan.getTextureComposite(tex) == null)
			TexMan.GenerateComposite(tex);

		return TexMan.getTextureComposite(tex, col);
	}

	/**
	 * Special version of GetColumn meant to be called concurrently by different
	 * seg rendering threads, identfiex by index. This allows to preserve global
	 * offsets (in offsets[index]) and avoid the phenomenon of getting "jittery"
	 * textures on the walls.
	 * 
	 */

	public byte[] GetColumn(int tex, int col, int index) {
		int lump, ofs;

		col &= TexMan.getTexturewidthmask(tex);
		lump = TexMan.getTextureColumnLump(tex, col);
		ofs = TexMan.getTextureColumnOfs(tex, col);

		// If pointing inside a non-zero, positive lump, then it's not a
		// composite texture.
		// Read from disk.
		dcvars.dc_source_ofs = 0;

		// If pointing inside a non-zero, positive lump, then it's not a
		// composite texture.
		// Read from disk, and safeguard vs tutti frutti.
		if (lump > 0) {
			// This will actually return a pointer to a patch's columns.
			return TexMan.getRogueColumn(lump, ofs);
		}

		// this.offsets[index]=0;
		// Texture should be composite, but it doesn't yet exist. Create it.
		if (TexMan.getTextureComposite(tex) == null)
			TexMan.GenerateComposite(tex);

		// This implies that texturecomposite actually stores raw, compressed
		// columns,
		// or else those "ofs" would go in-between.
		// The source offset int this case is 0, else we'll skip over stuff.

		return TexMan.getTextureComposite(tex, col);
	}

	/*
	 * @Override public final int getDCSourceOffset(int index){ return
	 * offsets[index]; }
	 */

	// int[] offsets;

	/**
	 * R_InitSpriteLumps Finds the width and hoffset of all sprites in the wad,
	 * so the sprite does not need to be cached completely just for having the
	 * header info ready during rendering.
	 */

	protected void InitSpriteLumps() throws IOException {
		int i;
		patch_t patch;

		firstspritelump = W.GetNumForName("S_START") + 1;
		lastspritelump = W.GetNumForName("S_END") - 1;

		numspritelumps = lastspritelump - firstspritelump + 1;
		spritewidth = new int[numspritelumps];
		spriteoffset = new int[numspritelumps];
		spritetopoffset = new int[numspritelumps];

		for (i = 0; i < numspritelumps; i++) {
			if ((i & 63) == 0)
				System.out.print(".");

			patch = (patch_t) W.CacheLumpNum(firstspritelump + i, PU_CACHE,
					patch_t.class);
			spritewidth[i] = patch.width << FRACBITS;
			spriteoffset[i] = patch.leftoffset << FRACBITS;
			spritetopoffset[i] = patch.topoffset << FRACBITS;
		}
	}

	/**
	 * R_InitColormaps
	 * 
	 * @throws IOException
	 */
	protected void InitColormaps() throws IOException {
		int lump, length;

		// Load in the light tables,
		// 256 byte align tables.
		lump = W.GetNumForName("COLORMAP");
		length = W.LumpLength(lump) + 256;
		colormaps = new byte[(length / 256)][256];
		System.out.println("Colormaps: " + colormaps.length);

		byte[] tmp = new byte[length];
		W.ReadLump(lump,tmp);

		for (int i = 0; i < colormaps.length; i++) {
			System.arraycopy(tmp, i * 256, colormaps[i], 0, 256);
		}
		
		// MAES: blurry effect is hardcoded to this colormap.
		BLURRY_MAP=colormaps[6];
		// colormaps = (byte *)( ((int)colormaps + 255)&~0xff);		

		
	}
	
	protected byte[] BLURRY_MAP;

	/**
	 * R_InitData Locates all the lumps that will be used by all views Must be
	 * called after W_Init.
	 */

	public void InitData() {
		try {
			System.out.print("\nInit Texture and Flat Manager");
			TexMan = this.DM.TM;
			System.out.print("\nInitTextures");
			TexMan.InitTextures();
			System.out.print("\nInitFlats");
			TexMan.InitFlats();
			System.out.print("\nInitSprites");
			InitSpriteLumps();
			System.out.print("\nInitColormaps");
			InitColormaps();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// //////////////////////// FLAT AND TEXTURE MANAGEMENT
	// //////////////////////////

	/*
	 * UNUSED: older, sequential search method.
	 * 
	 * public int CheckTextureNumForName(String name) { int i; // "NoTexture"
	 * marker. if (name.charAt(0) == '-') return 0;
	 * 
	 * 
	 * for (i = 0; i < numtextures; i++) if
	 * (textures[i].name.compareToIgnoreCase(name) == 0) return i;
	 * 
	 * return -1; }
	 */

	protected int spritememory;

	/**
	 * R_InitSprites Called at program start.
	 * 
	 */

	public void InitSprites(String[] namelist) {
		int i;

		for (i = 0; i < SCREENWIDTH; i++) {
			negonearray[i] = -1;
		}

		MyThings.InitSpriteDefs2(namelist);

	}

	/**
	 * To be called right after PrecacheLevel from SetupLevel in LevelLoader.
	 * It's an ugly hack, in that it must communicate with the "Game map" class
	 * and determine what kinds of monsters are actually in the level and
	 * whether it should load their graphics or not.
	 * 
	 * Whenever we implement it, it's going to be ugly and not neatly separated
	 * anyway.
	 * 
	 * @return
	 */
	public void PreCacheThinkers() {

		boolean[] spritepresent;
		thinker_t th;
		spriteframe_t sf;
		int i, j, k;
		int lump;

		spritepresent = new boolean[numsprites];

		for (th = P.getThinkerCap().next; th != P.getThinkerCap(); th = th.next) {
			if (th.function == think_t.P_MobjThinker)
				spritepresent[((mobj_t) th).sprite.ordinal()] = true;
		}

		spritememory = 0;
		for (i = 0; i < numsprites; i++) {
			if (!spritepresent[i])
				continue;

			for (j = 0; j < getSprites()[i].numframes; j++) {
				sf = getSprites()[i].spriteframes[j];
				for (k = 0; k < 8; k++) {
					lump = firstspritelump + sf.lump[k];
					spritememory += W.GetLumpInfo(lump).size;
					W.CacheLumpNum(lump, PU_CACHE, patch_t.class);
				}
			}
		}
	}

	/**
	 * R_InitTranslationTables Creates the translation tables to map the green
	 * color ramp to gray, brown, red. Assumes a given structure of the PLAYPAL.
	 * Could be read from a lump instead.
	 */

	public void InitTranslationTables() {
		int i;

		// translationtables = Z_Malloc (256*3+255, PU_STATIC, 0);
		// translationtables = (byte *)(( (int)translationtables + 255 )& ~255);
		translationtables = new byte[4][256];

		// translate just the 16 green colors
		for (i = 0; i < 256; i++) {
			if (i >= 0x70 && i <= 0x7f) {
				// map green ramp to gray, brown, red
				translationtables[1][i] = (byte) (0x60 + (i & 0xf));
				translationtables[2][i] = (byte) (0x40 + (i & 0xf));
				translationtables[3][i] = (byte) (0x20 + (i & 0xf));
			} else {
				// Keep all other colors as is.
				translationtables[1][i] = translationtables[2][i] = translationtables[3][i] = (byte) i;
			}
		}
	}

	// ///////////////// Generic rendering methods /////////////////////

	// /// DRAWSPAN ///////////
	@Override
	public int getFirstSpriteLump() {
		return this.firstspritelump;
	}

	@Override
	public spritedef_t[] getSprites() {
		return sprites;
	}

	public Things getThings() {
		return (Things) this.MyThings;
	}

	/**
	 * e6y: this is a precalculated value for more precise flats drawing (see
	 * R_MapPlane) "Borrowed" from PrBoom+
	 * 
	 * */
	protected float viewfocratio;
	protected int projectiony;

	// Some more isolation methods....
	
	public final int getValidCount(){
		return validcount;
	}
	
	public final void increaseValidCount(int amount){
		validcount+=amount;
	}
	
	public boolean getSetSizeNeeded(){
		return setsizeneeded;
	}

	public final int getViewWindowX(){
		return viewwindowx;
	}

	public final int getViewWindowY(){
		return viewwindowy;
	}
	
	public final int getScaledViewWidth(){
		return scaledviewwidth;
	}

    @Override
    public final int getScaledViewHeight() {
        return viewheight;
    }

	
	////////////////VIDEO SCALE STUFF ///////////////////////

	protected int SCREENWIDTH;
	protected int SCREENHEIGHT;
	protected float SCREEN_MUL;
	protected IVideoScale vs;

	@Override
	public void setVideoScale(IVideoScale vs) {
		this.vs = vs;
	}

	@Override
	public void initScaling() {
		this.SCREENHEIGHT = vs.getScreenHeight();
		this.SCREENWIDTH = vs.getScreenWidth();
		this.SCREEN_MUL = vs.getScreenMul();

		// Pre-scale stuff.
		BLANKCACHEDHEIGHT = new int[SCREENHEIGHT];
		BLANKFLOORCLIP = new short[SCREENWIDTH];
		BLANKCEILINGCLIP = new short[SCREENWIDTH];

		floorclip = new short[SCREENWIDTH];
		ceilingclip = new short[SCREENWIDTH];
		negonearray = new short[SCREENWIDTH]; // MAES: in scaling
		screenheightarray = new short[SCREENWIDTH];// MAES: in scaling
		xtoviewangle = new long[SCREENWIDTH + 1];
		
		MAXOPENINGS = SCREENWIDTH * 64;

		negonearray = new short[SCREENWIDTH];
		screenheightarray = new short[SCREENWIDTH];
		openings = new short[MAXOPENINGS];
		// Initialize children objects
		this.MyPlanes.setVideoScale(vs);
		this.MyThings.setVideoScale(vs);
		MyPlanes.initScaling();
		MyThings.initScaling();
		
	}
	
	/** Initializes the various drawing functions. They are all "pegged" to the
	 *  same dcvars/dsvars object. Any initializations of e.g. parallel renderers
	 *  and their supporting subsystems should occur here. 
	 */
	
	protected void R_InitDrawingFunctions(){
	    
	    dcvars=new ColVars<byte[]>();	    
	    maskedcvars=new ColVars<byte[]>();
	    maskedcvars.dc_translation=translationtables[0];
	    skydcvars=new ColVars<byte[]>();
	    
	    dsvars=new SpanVars<byte[]>();
	    
        // Span functions. Common to all renderers unless overriden
        // or unused e.g. parallel renderers ignore them.
        DrawSpan=new R_DrawSpanUnrolled(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dsvars,screen,I);
        DrawSpanLow=new R_DrawSpanLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dsvars,screen,I);
        
        
        // Translated columns are usually sprites-only.
        DrawTranslatedColumn=new R_DrawTranslatedColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
        DrawTLColumn=new R_DrawTLColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
        
        // Fuzzy columns. These are also masked.
        DrawFuzzColumn=new R_DrawFuzzColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I,BLURRY_MAP);
        DrawFuzzColumnLow=new R_DrawFuzzColumnLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I,BLURRY_MAP);
        
        // Regular draw for solid columns/walls. Full optimizations.
        DrawColumn=new R_DrawColumnBoomOpt(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dcvars,screen,I);
        DrawColumnLow=new R_DrawColumnBoomOptLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dcvars,screen,I);
        
        // Non-optimized stuff for masked.
        DrawColumnMasked=new R_DrawColumnBoom(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
        DrawColumnMaskedLow=new R_DrawColumnBoomLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
        
        // Player uses masked
        DrawColumnPlayer=DrawColumnMasked; // Player normally uses masked.
        
        // Skies use their own. This is done in order not to stomp parallel threads.
        
        DrawColumnSkies=new R_DrawColumnBoomOpt(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,skydcvars,screen,I);
        DrawColumnSkiesLow=new R_DrawColumnBoomOptLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,skydcvars,screen,I);
	}

	// //////////////////////////// LIMIT RESETTING //////////////////
	@Override
	public void resetLimits() {
		// Call it only at the beginning of new levels.
		MyThings.resetLimits();
		MySegs.resetLimits();
	}

}
