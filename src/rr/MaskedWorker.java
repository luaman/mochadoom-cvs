package rr;

import i.IDoomSystem;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import doom.player_t;

import p.pspdef_t;
import static rr.Lights.*;
import rr.drawfuns.ColVars;
import rr.drawfuns.DoomColumnFunction;
import rr.drawfuns.R_DrawColumnBoom;
import rr.drawfuns.R_DrawColumnBoomLow;
import rr.drawfuns.R_DrawFuzzColumn;
import rr.drawfuns.R_DrawFuzzColumnLow;
import rr.drawfuns.R_DrawTranslatedColumn;
import rr.drawfuns.R_DrawTranslatedColumnLow;
import w.IWadLoader;
import static p.mobj_t.MF_TRANSLATION;
import static p.mobj_t.MF_TRANSSHIFT;
import static m.fixed_t.*;
import static rr.line_t.*;
import static data.Defines.FF_FRAMEMASK;
import static data.Defines.FF_FULLBRIGHT;
import static data.Defines.pw_invisibility;
import static data.Defines.SIL_BOTTOM;
import static data.Defines.SIL_TOP;
import static rr.Renderer.BASEYCENTER;

public abstract class MaskedWorker<V> implements Runnable, IDetailAware{
    
    private final static boolean DEBUG=false;
    private final static boolean RANGECHECK=false;
	
    protected final CyclicBarrier barrier;
    protected final int id;
    protected final int numthreads;
    protected final ViewVars view;
    protected final Colormaps<V> colormap;
    protected final Lights<V> lights;
    protected final ISpriteManager SM;
    protected final TextureManager TexMan;
    protected final IDoomSystem I;
    protected final SegVars seg_vars;
    protected final IWadLoader W;
    protected final IVisSpriteManagement<V> VIS;
    protected final IGetSmpColumn<V> GSC;
    
    protected DoomColumnFunction<byte[],V> colfunc;
    protected DoomColumnFunction<byte[],V> transcolfunc;
    protected DoomColumnFunction<byte[],V> maskedcolfunc;
    protected DoomColumnFunction<byte[],V> fuzzcolfunc;
    protected DoomColumnFunction<byte[],V> playercolfunc;
    protected DoomColumnFunction<byte[],V> maskedcolfunchi;
    protected DoomColumnFunction<byte[],V> maskedcolfunclow;
    protected DoomColumnFunction<byte[],V> fuzzcolfunchi;
    protected DoomColumnFunction<byte[],V> fuzzcolfunclow;
    protected DoomColumnFunction<byte[],V> transcolhigh;
    protected DoomColumnFunction<byte[],V> transcollow;
    
    protected ColVars<byte[],V> maskedcvars;
    
    // MAES: Scale to SCREENWIDTH
    private final short[] clipbot;
    private final short[] cliptop;
    
    @SuppressWarnings("unchecked")
	public MaskedWorker(RendererState<V> R,int id,int SCREENWIDTH, int SCREENHEIGHT,int numthreads,CyclicBarrier barrier){
        this.id=id;
        this.numthreads=numthreads;
        this.barrier=barrier;
        this.clipbot=new short[SCREENWIDTH];
        this.cliptop=new short[SCREENWIDTH];
        this.view=R.view;
        this.seg_vars=R.seg_vars;
        this.lights=R.lights;
        this.colormap=R.colormap;
        this.W=R.W;
        this.TexMan=R.TexMan;
        this.SM=R.SM;
        this.I=R.I; 
        this.VIS=R.VIS;
        this.GSC=(IGetSmpColumn<V>) R;
        }
    
    public static final class HiColor extends MaskedWorker<short[]>{

		public HiColor(RendererState<short[]> R,int id, int SCREENWIDTH, int SCREENHEIGHT,
				int[] ylookup, int[] columnofs, int numthreads, short[] screen,
				CyclicBarrier barrier) {
			super(R,id, SCREENWIDTH, SCREENHEIGHT,numthreads, barrier);
	        maskedcvars=new ColVars<byte[],short[]>();
	        // Non-optimized stuff for masked.
	        playercolfunc=colfunc=maskedcolfunc=maskedcolfunchi=new R_DrawColumnBoom.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
	        maskedcolfunclow=new R_DrawColumnBoomLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);

	        // Fuzzy columns. These are also masked.
	        fuzzcolfunc=fuzzcolfunchi=new R_DrawFuzzColumn.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
	        fuzzcolfunclow=new R_DrawFuzzColumnLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);

	        // Translated columns are usually sprites-only.
	        transcolfunc=transcolhigh=new R_DrawTranslatedColumn.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
	        transcollow=new R_DrawTranslatedColumnLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
	  
	        avis=new vissprite_t<short[]>();
		}
    	
    }
    
    protected int startx, endx;
    
    // Cache those you get from the sprite manager
    protected int[] spritewidth, spriteoffset, spritetopoffset;
    
    /** fixed_t */
    protected int pspritescale, pspriteiscale;
    
    // Used for masked segs. Use private for each thread.
    protected int rw_scalestep;
    protected int pmaskedtexturecol;
    protected short[] maskedtexturecol;
    
    protected int spryscale;
    protected int sprtopscreen;
    protected short[] mfloorclip;
    protected int p_mfloorclip;
    protected short[] mceilingclip;
    protected int p_mceilingclip;
    
    protected sector_t frontsector;
    protected sector_t backsector;
    protected seg_t curline;
    
    public void cacheSpriteManager(ISpriteManager SM){
          this.spritewidth=SM.getSpriteWidth();
            this.spriteoffset=SM.getSpriteOffset();
            this.spritetopoffset=SM.getSpriteTopOffset();
    }

    /**
     * R_DrawVisSprite mfloorclip and mceilingclip should also be set.
     * 
     * Sprites are actually drawn here.
     * 
     * 
     */
    protected final void DrawVisSprite(vissprite_t<V> vis) {
        column_t column;
        int texturecolumn;
        int frac; // fixed_t
        patch_t patch;
        // The sprite may have been partially drawn on another portion of the
        // screen.
        int bias=startx-vis.x1;
            if (bias<0) bias=0; // nope, it ain't.

        // Trim bounds to zone NOW
        int x1=Math.max(startx, vis.x1);
        int x2=Math.min(endx,vis.x2);
            
        // At this point, the view angle (and patch) has already been
        // chosen. Go back.
        patch = W.CachePatchNum(vis.patch + SM.getFirstSpriteLump());
        
        maskedcvars.dc_colormap = vis.colormap;
        // colfunc=glasscolfunc;
        if (maskedcvars.dc_colormap == null) {
            // NULL colormap = shadow draw
            colfunc = fuzzcolfunc;
        } else if ((vis.mobjflags & MF_TRANSLATION) != 0) {
            colfunc = transcolfunc;
            maskedcvars.dc_translation = colormap.translationtables[(vis.mobjflags & MF_TRANSLATION)>>MF_TRANSSHIFT];
        }

        maskedcvars.dc_iscale = Math.abs(vis.xiscale) >> view.detailshift;
        maskedcvars.dc_texturemid = vis.texturemid;
        // Add bias to compensate for partially drawn sprite which has not been rejected.
        frac = vis.startfrac+vis.xiscale*bias;
        spryscale = vis.scale;
        sprtopscreen = view.centeryfrac - FixedMul(maskedcvars.dc_texturemid, spryscale);

        // A texture height of 0 means "not tiling" and holds for
        // all sprite/masked renders.
        maskedcvars.dc_texheight=0;
        
        for (maskedcvars.dc_x = x1; maskedcvars.dc_x <= x2; maskedcvars.dc_x++, frac += vis.xiscale) {
            texturecolumn = frac >> FRACBITS;
            if (true) {
                if (texturecolumn < 0 || texturecolumn >= patch.width)
                    I.Error("R_DrawSpriteRange: bad texturecolumn %d vs %d %d %d",texturecolumn,patch.width,x1,x2);
            }
            column = patch.columns[texturecolumn];
            
            if (column==null)
                System.err.printf("Null column for texturecolumn %d\n",texturecolumn,x1,x2);
            else
            DrawMaskedColumn(column);
        }

        colfunc = maskedcolfunc;
    }

    /**
     * R_RenderMaskedSegRange
     * 
     * @param ds
     * @param x1
     * @param x2
     */
    
    protected final void RenderMaskedSegRange(drawseg_t ds, int x1, int x2) {
    	
    	// Trivial rejection
        if (ds.x1>endx || ds.x2<startx) return;
        
        // Trim bounds to zone NOW
        x1=Math.max(startx, x1);
        x2=Math.min(endx,x2);
    	
        int index;

        int lightnum;
        int texnum;
        int bias=startx-ds.x1; // Correct for starting outside
        if (bias<0) bias=0; // nope, it ain't.
        
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
        lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT) + lights.extralight;

        if (curline.v1y == curline.v2y)
            lightnum--;
        else if (curline.v1x == curline.v2x)
            lightnum++;

        // Killough code.
        colormap.walllights = lightnum >= LIGHTLEVELS ? lights.scalelight[LIGHTLEVELS - 1]
                : lightnum < 0 ? lights.scalelight[0] : lights.scalelight[lightnum];

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
                    - view.z;
        } else {
            maskedcvars.dc_texturemid = frontsector.ceilingheight < backsector.ceilingheight ? frontsector.ceilingheight
                    : backsector.ceilingheight;
            maskedcvars.dc_texturemid = maskedcvars.dc_texturemid - view.z;
        }
        maskedcvars.dc_texturemid += curline.sidedef.rowoffset;

        if (colormap.fixedcolormap != null)
            maskedcvars.dc_colormap = colormap.fixedcolormap;

        // Texture height must be set at this point. This will trigger
        // tiling. For sprites, it should be set to 0.
        maskedcvars.dc_texheight = TexMan.getTextureheight(texnum) >> FRACBITS;

        // draw the columns
        for (maskedcvars.dc_x = x1; maskedcvars.dc_x <= x2; maskedcvars.dc_x++) {
            // calculate lighting
            if (maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x] != Short.MAX_VALUE) {
                if (colormap.fixedcolormap == null) {
                    index = spryscale >>> LIGHTSCALESHIFT;

                    if (index >= MAXLIGHTSCALE)
                        index = MAXLIGHTSCALE - 1;

                    maskedcvars.dc_colormap = colormap.walllights[index];
                }

                sprtopscreen = view.centeryfrac
                        - FixedMul(maskedcvars.dc_texturemid, spryscale);
                maskedcvars.dc_iscale = (int) (0xffffffffL / spryscale);

                // draw the texture
                byte[] data = (byte[]) GSC.GetSmpColumn(texnum,
                        maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x],id);// -3);
                
                DrawMaskedColumn(data);
                maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x] = Short.MAX_VALUE;
            }
            spryscale += rw_scalestep;
        }

    }		
    
    /**
     * R_DrawPSprite
     * 
     * Draws a "player sprite" with slighly different rules than normal
     * sprites. This is actually a PITA, at best :-/
     * 
     */

    protected final void DrawPSprite(pspdef_t psp) {

        int tx;
        int x1;
        int x2;
        spritedef_t sprdef;
        spriteframe_t sprframe;
        vissprite_t<V> vis;
        int lump;
        boolean flip;

        //

        // decide which patch to use (in terms of angle?)
        if (RANGECHECK) {
            if (psp.state.sprite.ordinal() >= SM.getNumSprites())
                I.Error("R_ProjectSprite: invalid sprite number %d ",
                        psp.state.sprite);
        }

        sprdef = SM.getSprite(psp.state.sprite.ordinal());
        
        if (RANGECHECK) {
            if ((psp.state.frame & FF_FRAMEMASK) >= sprdef.numframes)
                I.Error("R_ProjectSprite: invalid sprite frame %d : %d ",
                        psp.state.sprite, psp.state.frame);
        }
        
        sprframe = sprdef.spriteframes[psp.state.frame & FF_FRAMEMASK];

        // Base frame for "angle 0" aka viewed from dead-front.
        lump = sprframe.lump[0];
        // Q: where can this be set? A: at sprite loadtime.
        flip = (boolean) (sprframe.flip[0] != 0);

        // calculate edges of the shape. tx is expressed in "view units".
        tx = (int) (FixedMul(psp.sx, view.BOBADJUST) - view.WEAPONADJUST);

        tx -= spriteoffset[lump];

        // So...centerxfrac is the center of the screen (pixel coords in
        // fixed point).
        x1 = (view.centerxfrac + FixedMul(tx, pspritescale)) >> FRACBITS;

        // off the right side
        if (x1 > endx)
            return;

        tx += spritewidth[lump];
        x2 = ((view.centerxfrac + FixedMul(tx, pspritescale)) >> FRACBITS) - 1;

        // off the left side
        if (x2 < startx)
            return;

        // store information in a vissprite ?
        vis = avis;
        vis.mobjflags = 0;
        vis.texturemid = ((BASEYCENTER+view.lookdir) << FRACBITS) + FRACUNIT / 2
                - (psp.sy - spritetopoffset[lump]);
        vis.x1 = x1 < startx ? startx : x1;
        vis.x2 = x2 >= endx ? endx - 1 : x2;
        vis.scale = (pspritescale) << view.detailshift;

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

        if ((view.player.powers[pw_invisibility] > 4 * 32)
                || (view.player.powers[pw_invisibility] & 8) != 0) {
            // shadow draw
            vis.colormap = null;

        } else if (colormap.fixedcolormap != null) {
            // fixed color
            vis.colormap = colormap.fixedcolormap;
            // vis.pcolormap=0;
        } else if ((psp.state.frame & FF_FULLBRIGHT) != 0) {
            // full bright
            vis.colormap = colormap.colormaps[0];
            // vis.pcolormap=0;
        } else {
            // local light
            vis.colormap = lights.spritelights[MAXLIGHTSCALE - 1];
        }

        //System.out.printf("Weapon draw from %d to %d\n",vis.x1,vis.x2);
        DrawVisSprite(vis);
    }
    
    /** used inside DrawPSprite, better make this static */
    protected vissprite_t<V> avis;

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
        lightnum = (view.player.mo.subsector.sector.lightlevel >> LIGHTSEGSHIFT)
                + lights.extralight;

        if (lightnum < 0)
            lights.spritelights = lights.scalelight[0];
        else if (lightnum >= LIGHTLEVELS)
        	lights.spritelights = lights.scalelight[LIGHTLEVELS - 1];
        else
        	lights.spritelights = lights.scalelight[lightnum];

        // clip to screen bounds
        mfloorclip = view.screenheightarray;
        p_mfloorclip = 0;
        mceilingclip = view.negonearray;
        p_mceilingclip = 0;

        // add all active psprites
        // MAES 25/5/2011 Fixed another stupid bug that prevented
        // PSP from actually being updated. This in turn uncovered
        // other bugs in the way psp and state were treated, and the way
        // flash states were set. It should be OK now.
        for (i = 0; i < player_t.NUMPSPRITES; i++) {
            psp = view.player.psprites[i];
            if (psp.state != null && psp.state.id != 0) {
                DrawPSprite(psp);
            }
        }
    }


    /**
     * R_DrawSprite
     */

    protected final void DrawSprite(vissprite_t<V> spr) {
        int ds;
        drawseg_t dss;

        int x;
        int r1;
        int r2;
        int scale; // fixed
        int lowscale; // fixed
        int silhouette;

        // Trivially outside. Don't bother computing anything for it.
        //if (spr.x1>=endx || spr.x2<=startx) return;
        
        // Trimmed boundaries. Only do masked seg computations on these.
        int sprx1=spr.x1;//Math.max(startx,spr.x1);
        int sprx2=spr.x2;//Math.min(endx,spr.x2);
        
        
        // Trim bounds if necessary.
        for (x = sprx1; x <=  sprx2; x++)
            clipbot[x] = cliptop[x] = -2;

        // Scan drawsegs from end to start for obscuring segs.
        // The first drawseg that has a greater scale
        // is the clip seg.
        // We can do that without interefering, since we'll limit to our portions.
        for (ds = seg_vars.ds_p - 1; ds >= 0; ds--) {
            // determine if the drawseg obscures the sprite
            // System.out.println("Drawseg "+ds+"of "+(ds_p-1));
            dss = seg_vars.drawsegs[ds];
            if (dss.x1 > sprx2
                    || dss.x2 < sprx1
                    || ((dss.silhouette == 0) && (dss
                            .nullMaskedTextureCol()))) {
                // does not cover sprite
                continue;
            }

            r1 = dss.x1 < sprx1 ? sprx1 : dss.x1;
            r2 = dss.x2 > sprx2 ? sprx2 : dss.x2;

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
                if (!(dss.x1>endx || dss.x2<startx) && !dss.nullMaskedTextureCol())
                    RenderMaskedSegRange(dss, r1, r2);
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
        for (x = sprx1; x <= sprx2; x++) {
            if (clipbot[x] == -2)
                clipbot[x] = (short) view.height;
            // ?? What's this bullshit?
            if (cliptop[x] == -2)
                cliptop[x] = -1;
        }

        mfloorclip = clipbot;
        p_mfloorclip = 0;
        mceilingclip = cliptop;
        p_mceilingclip = 0;
        // Let DrawVisSprite do the final trimming and biasing.
        DrawVisSprite(spr);
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
    
    @Override
    public void run() {
        // vissprite_t spr;
        int ds;
        drawseg_t dss;

        // Sprites should already be sorted for distance 

        colfunc = maskedcolfunc; // Sprites use fully-masked capable
                                 // function.

        // Update view height
        
        this.maskedcvars.viewheight=view.height;
        this.maskedcvars.centery=view.centery;
        this.startx=((id*view.width)/numthreads);
        this.endx=(((id+1)*view.width)/numthreads);
        
        // Update thread's own vissprites
        
        final vissprite_t<V>[] vissprites=VIS.getVisSprites();
        final int numvissprites=VIS.getNumVisSprites();
        
        //System.out.printf("Sprites to render: %d\n",numvissprites);
        
        // Try drawing all sprites that are on your side of
        // the screen. Limit by x1 and x2, if you have to.
        for (int i = 0; i < numvissprites; i++) {
            DrawSprite(vissprites[i]);
        }
        
        //System.out.printf("Segs to render: %d\n",ds_p);

        // render any remaining masked mid textures
        for (ds = seg_vars.ds_p - 1; ds >= 0; ds--) {
            dss = seg_vars.drawsegs[ds];
            if (!(dss.x1>endx || dss.x2<startx)&&!dss.nullMaskedTextureCol())
                RenderMaskedSegRange(dss, dss.x1,dss.x2);
        }
        // draw the psprites on top of everything
        // but does not draw on side views
        // if (viewangleoffset==0)

        colfunc = playercolfunc;
        DrawPlayerSprites();
        colfunc = maskedcolfunc;
        
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
            if (maskedcvars.dc_yl <= maskedcvars.dc_yh && maskedcvars.dc_yh < view.height) {
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
     * R_DrawMaskedColumn Used for sprites and masked mid textures. Masked
     * means: partly transparent, i.e. stored in posts/runs of opaque pixels.
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
                colfunc.invoke();

            }
        }

        maskedcvars.dc_texturemid = basetexturemid;
    }
    
    public final void setPspriteIscale(int i) {
        pspriteiscale=i;
        
    }

    public final void setPspriteScale(int i) {
        pspritescale=i;            
    }
    

    @Override
    public void setDetail(int detailshift) {
        if (detailshift==0){
            this.playercolfunc=this.maskedcolfunc=this.maskedcolfunchi;
            this.fuzzcolfunc=this.fuzzcolfunchi;
            this.transcolfunc=this.transcolhigh;
        } else {                
            this.playercolfunc=this.maskedcolfunc=this.maskedcolfunclow;
            this.fuzzcolfunc=this.fuzzcolfunclow;
            this.transcolfunc=this.transcollow;
        }
    }
    
}
