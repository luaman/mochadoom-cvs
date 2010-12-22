package rr;

import static data.Defines.FF_FRAMEMASK;
import static data.Defines.FF_FULLBRIGHT;
import static data.Defines.NF_SUBSECTOR;
import static data.Defines.NUMCOLORMAPS;
import static data.Defines.PU_CACHE;
import static data.Defines.SCREENHEIGHT;
import static data.Defines.SCREENWIDTH;
import static data.Defines.SIL_BOTTOM;
import static data.Defines.SIL_TOP;
import static data.Defines.pw_invisibility;
import static data.Limits.MAXHEIGHT;
import static data.Limits.MAXOPENINGS;
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
import static data.Tables.finesine;
import static data.Tables.finetangent;
import static data.Tables.tantoangle;
import static doom.player_t.NUMPSPRITES;
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
import java.util.Arrays;
import java.util.Hashtable;

import m.DoomMenu;
import m.fixed_t;

import p.LevelLoader;
import p.UnifiedGameMap;
import p.mobj_t;
import p.pspdef_t;
import rr.ParallelRenderer.BSP;
import i.DoomStatusAware;
import i.DoomSystemInterface;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import w.WadLoader;
import w.name8;
import data.Defines;
import data.Limits;
import defines.GameMode_t;
import doom.DoomContext;
import doom.DoomMain;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;

/** Most shared -essential- status information, methods and classes related
 *  to the rendering subsystem are found here, shared between the various 
 *  implementations of the Doom's renderer.
 *  
 *  Not the cleanest or more OO way possible, but still a good way to avoid
 *  duplicating common code.
 *  
 *  Some stuff like Texture, Flat and Sprite management are also found -or at 
 *  least implemented temporarily- here, until a cleaner split can be made.
 *  This is a kind of "Jack of all trades" class.
 *  
 * @author velktron
 *
 */

public abstract class RendererState implements DoomStatusAware, Renderer, SpriteManager{

    protected static final boolean DEBUG=false;
    protected static final boolean DEBUG2=false;

    
    //////////////////////////////// STATUS ////////////////

    protected DoomMain DM;
    protected LevelLoader LL;
    protected WadLoader W;
    protected SegDrawer MySegs;
    protected DoomMenu Menu;
    protected BSP MyBSP;
    protected PlaneDrawer MyPlanes;    
    protected Things MyThings;
    protected DoomVideoRenderer V;
    protected UnifiedGameMap P;
    protected DoomSystemInterface I;
    protected TextureManager TexMan;
 

    // Found in draw_c. Only ever used in renderer.
    
    public int viewwindowx;
    public int viewwindowy;
    public int viewwidth;
    public int viewheight;

    public int scaledviewwidth;

    /** killough: viewangleoffset is a legacy from the pre-v1.2 days, when Doom
     *  had Left/Mid/Right viewing. +/-ANG90 offsets were placed here on each
     *  node, by d_net.c, to set up a L/M/R session. */
    
    public static final long viewangleoffset=0;
    
    ///// FROM PLANES //////
    
    // initially.
    protected int MAXVISPLANES = Limits.MAXVISPLANES;
    
    /**
     * Clip values are the solid pixel bounding the range.
     *  floorclip starts out SCREENHEIGHT
     *  ceilingclip starts out -1
     */
    protected short[]         floorclip=new short[SCREENWIDTH],   ceilingclip=new short[SCREENWIDTH];
    
    /** visplane_t*,  treat as indexes into visplanes */
    protected int       lastvisplane, floorplane,   ceilingplane;
    

    protected visplane_t[]      visplanes=new visplane_t[MAXVISPLANES];

    /** openings is supposed to show where "openings" in visplanes start and end e.g.
     *  due to sprites, windows etc.
     */
    protected short[]         openings=new short[MAXOPENINGS];
    /** Maes: this is supposed to be a pointer inside openings */
    protected int           lastopening;//=new Short((short) 0);

    /** Increment every time a check is made 
     *  For some reason, this needs to be visible even by enemies thinking :-S*/
    public int validcount = 1;     
    

 /** Who can set this? A: The Menu. */
 public boolean      setsizeneeded;
 protected int     setblocks;
 protected int     setdetail;

 /**
  * R_SetViewSize
  * Do not really change anything here,
  * because it might be in the middle of a refresh.
  * The change will take effect next refresh.
  * 
  * @param blocks 11 is full screen, 9 default.
  * @param detail 0= high, 1 =low.
  */

 public void SetViewSize
 ( int       blocks,
 int       detail )
 {
     System.out.println("SetViewSize");
  setsizeneeded = true;
  setblocks = blocks;
  setdetail = detail;
 }
    
 /**
  * R_FindPlane
  * 
  * Checks whether a visplane with the specified height, picnum and light 
  * level exists among those already created. This looks like a half-assed 
  * attempt at reusing already existing visplanes, rather than creating new 
  * ones. The tricky part is understanding what happens if one DOESN'T exist.
  * Called only from within R_Subsector (so while we're still trasversing stuff).
  * 
  * @param height (fixed_t)
  * @param picnum
  * @param lightlevel
  * @return was visplane_t*, returns index into visplanes[]
  */

 protected final int
 FindPlane
 ( int   height,
   int       picnum,
   int       lightlevel )
 {
     //System.out.println("\tChecking for visplane merging...");
     int check=0; // visplane_t* 
     visplane_t chk=null;
     
     if (picnum == TexMan.getSkyFlatNum())
     {
     height = 0;         // all skys map together
     lightlevel = 0;
     }     
          
     chk=visplanes[0];
     
     // Find visplane with the desired attributes
     for (check=0; check<lastvisplane; check++)
     {
     
         chk=visplanes[check];
     if (height == chk.height
         && picnum == chk.picnum
         && lightlevel ==chk.lightlevel) {
         //  Found a visplane with the desired specs.
         break;
         }
     }
             
     if (check < lastvisplane){
     return check;
     }          
     
     // Found a visplane, but we can't add anymore.
     if (lastvisplane == MAXVISPLANES){
     //I.Error ("R_FindPlane: no more visplanes");
         ResizeVisplanes();
     
     }
     
     /* FIXED: we need to add this post-fix here because of the way
      * the original was structured (pointer hacks, too lengthy to
      * explain). We need to make sure that when no visplane is found
      * a "failed check" will actually result in a pointer to the
      * next "free" visplane, and that we always have a valid pointer
      * to visplane 0, even if the loop never ran.
      *  This fixes the "blinking visplane bug", which manifested 
      *  itself when sector lighting effects changed the light level
      */
     
     chk=visplanes[check];
     // Add a visplane
     lastvisplane++;
     chk.height = height;
     chk.picnum = picnum;
     chk.lightlevel = lightlevel;
     chk.minx = SCREENWIDTH;
     chk.maxx = -1;
     //memset (chk.top,0xff,sizeof(chk.top));
     chk.clearTop();
         
     return check;
 }
 
 /** A hashtable used to retrieve planes with particular attributes
  *  faster -hopefully-. The planes are still stored in the visplane array
  *  for convenience, but we can search them in the hashtable too -as a bonus,
  *  we can reuse previously created planes that match newer ones-. 
  */
 Hashtable<visplane_t,Integer> planehash=new Hashtable<visplane_t,Integer>(128);
 visplane_t check=new visplane_t();
 
 protected final int
 FindPlane2
 ( int   height,
   int       picnum,
   int       lightlevel )
 {
     //System.out.println("\tChecking for visplane merging...");
    // int check=0; // visplane_t* 
     visplane_t chk=null;
     Integer checknum; 
     
     if (picnum == TexMan.getSkyFlatNum())
     {
     height = 0;         // all skys map together
     lightlevel = 0;
     }
     
     // Try and find this.
     check.lightlevel=lightlevel;
     check.picnum=picnum;
     check.height=height;
     check.updateHashCode();
         
     /*     
     chk=visplanes[0];
     
     // Find visplane with the desired attributes
     for (check=0; check<lastvisplane; check++)
     {
     
         chk=visplanes[check];
     if (height == chk.height
         && picnum == chk.picnum
         && lightlevel ==chk.lightlevel) {
         //  Found a visplane with the desired specs.
         break;
         }
     }
     */
     
     checknum=planehash.get(check);
    
     // Something found, get it.
     
     if (!(checknum==null)){
             
     // Visplane exists and is within those allocated in the current tic.    
     if (checknum < lastvisplane){
         return checknum;
         }
     
     // Found a visplane, but we can't add anymore.
     // Resize right away. This shouldn't take too long.
     if (lastvisplane == MAXVISPLANES){
     //I.Error ("R_FindPlane: no more visplanes");
         ResizeVisplanes();     
         }
     }
     
     /* FIXED: we need to add this post-fix here because of the way
      * the original was structured (pointer hacks, too lengthy to
      * explain). We need to make sure that when no visplane is found
      * a "failed check" will actually result in a pointer to the
      * next "free" visplane, and that we always have a valid pointer
      * to visplane 0, even if the loop never ran.
      *  This fixes the "blinking visplane bug", which manifested 
      *  itself when sector lighting effects changed the light level
      */
     
     // We found a visplane (possibly one allocated on a previous tic)
     // but we can't link directly to it, we need to copy its data
     // around. 
     
     checknum=new Integer(Math.max(0,lastvisplane));
     
     chk=visplanes[checknum];
     // Add a visplane
     lastvisplane++;
     chk.height = height;
     chk.picnum = picnum;
     chk.lightlevel = lightlevel;
     chk.minx = SCREENWIDTH;
     chk.maxx = -1;
     chk.updateHashCode();
     planehash.put(chk, checknum);
     //memset (chk.top,0xff,sizeof(chk.top));
     chk.clearTop();
         
     return checknum;
 }
 
 protected final void ResizeVisplanes() {
     visplane_t[] tmp=new visplane_t[MAXVISPLANES*2];
     System.arraycopy(visplanes, 0, tmp, 0, MAXVISPLANES);
     
     C2JUtils.initArrayOfObjects(tmp,MAXVISPLANES,tmp.length);
     
     // Bye bye, old visplanes.
     visplanes=tmp;   
     MAXVISPLANES*=2;
    
     System.out.println("Visplane buffer resized. Actual capacity "+visplanes.length);
 }
 
    public void updateStatus(DoomContext DC){
        this.DM=DC.DM;
        this.LL=DC.LL;
        this.W=DC.W;
        this.V=DC.V;
        this.P=DC.P;
        this.I=DC.I;
    }
    
//////////////////////////////// THINGS ////////////////////////////////
    
    // MAES: Shit taken from things

    public int     firstspritelump;
    public int     lastspritelump;
    public int     numspritelumps;
    
    public static final int MINZ=(FRACUNIT*4);
    public static final int BASEYCENTER     =   100;

    //
    // Sprite rotation 0 is facing the viewer,
    //  rotation 1 is one angle turn CLOCKWISE around the axis.
    // This is not the same as the angle,
    //  which increases counter clockwise (protractor).
    // There was a lot of stuff grabbed wrong, so I changed it...
    //

    /** fixed_t */
    protected int     pspritescale,pspriteiscale,skyscale;

    protected byte[][]  spritelights;
    
    protected int WEAPONADJUST;
    protected int BOBADJUST;
     
    /** constant arrays
     *  used for psprite clipping and initializing clipping 
     */
    protected short[]     negonearray=new short[SCREENWIDTH];
    protected short[]     screenheightarray=new short[SCREENWIDTH];

    protected int     spryscale;
    protected int     sprtopscreen;
    protected short[]      mfloorclip;
    protected int p_mfloorclip;
    protected short[]      mceilingclip;
    protected int p_mceilingclip;
    
    //
    // INITIALIZATION FUNCTIONS
    //

    /** variables used to look up
      *    and range check thing_t sprites patches
      */
    private spritedef_t[]   sprites;
    protected int     numsprites;

    /* variables used to look up
    * and range check thing_t sprites patches
    */

    /** Temporarily contains the frames of a given sprite before they are registered
     *  with the rendering system. Apparently, a maximum of 29 frames per sprite
     *  is allowed.
     */
    protected spriteframe_t[] sprtemp=new spriteframe_t[29];
    protected int     maxframe;
    protected String      spritename;
    
    //
    // Spectre/Invisibility.
    //
    protected static final int FUZZTABLE = 50;

    protected static final int FUZZOFF = SCREENWIDTH;

    protected final int[] fuzzoffset =
        { FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF,
                -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF,
                -FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
                -FUZZOFF, -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF };

    protected int fuzzpos = 0;
    
    
    /** Refresh of things, i.e. objects represented by sprites. */
    
    protected final class Things{

        protected static final int MINZ    =            (FRACUNIT*4);
        protected static final int BASEYCENTER         =100;

        public Things(){
            negonearray=new short[SCREENWIDTH];
            screenheightarray=new short[SCREENWIDTH];
            sprtemp=new spriteframe_t[MAX_SPRITEFRAMES];
            C2JUtils.initArrayOfObjects(sprtemp);
            vissprites=new vissprite_t[MAXVISSPRITES];
            C2JUtils.initArrayOfObjects(vissprites);
            vsprsortedhead=new vissprite_t();
            unsorted=new vissprite_t();
        }
        
        //void R_DrawColumn (void);
        //void R_DrawFuzzColumn (void);

        class maskdraw_t
        {
            int     x1;
            int     x2;
            
            int     column;
            int     topclip;
            int     bottomclip;

        };

        /**
         * R_InstallSpriteLump
         * Local function for R_InitSprites.
         */
        
        protected final void
        InstallSpriteLump
        ( int       lump,
          int  frame,
          int  rotation,
          boolean   flipped )
        {
            
           // System.out.println("Trying to install "+spritename+" Frame "+ (char)('A'+frame)+" rot "+(rotation) +" . Should have rotations: "+sprtemp[frame].rotate); 
            int     r;
            
            if (frame >= 29 || rotation > 8)
            I.Error("R_InstallSpriteLump: Bad frame characters in lump %i", lump);
            
            if ((int)frame > maxframe)
            maxframe = frame;
            
            /* A rotation value of 0 means that we are either checking the first frame of a sprite
             * that HAS rotations, or something that nas no rotations at all. The value
             * of rotate doesn't really help us discern here, unless set to "false"
             * a-priori...which can't happen ?!
             */
            
            if (rotation == 0)
            {
                /* MAES: notice how comparisons are done with strict literals
                 * (true and alse) which are actually defined to be 0 and 1,
                 * rather than assuming that true is "any nonzero value".
                 * This happens because rotate's value could be -1 at this point (!),
                 * if a series of circumstances occur. Therefore it's actually 
                 * a "tri-state", and the comparion 0=false and "anything else"=true
                 * was not good enough in this case. A value of -1 doesn't yield either
                 * true or false here. 
                 * 
                 */
                
                // the lump should be used for all rotations
            if (sprtemp[frame].rotate == 0){
                /* MAES: Explanation: we stumbled upon this lump before, and decided that this frame should have no more
                 * rotations, hence we bomb.
                 */
                I.Error ("R_InitSprites: Sprite %s frame %c has multiple rot=0 lump", spritename, 'A'+frame);
            }

            // This should NEVER happen!
            if (sprtemp[frame].rotate == 1) {
                /* MAES: This can only happen if we decided that a sprite's frame was already decided to have
                 * rotations, but now we stumble upon another occurence of "rotation 0". Or if you use naive
                 * true/false evaluation for .rotate ( -1 is also an admissible value).
                 */
                I.Error ("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump", spritename, 'A'+frame);          
            }
            
            // Rotation is acknowledged to be totally false at this point.
            sprtemp[frame].rotate = 0;
            for (r=0 ; r<8 ; r++)
            {
                sprtemp[frame].lump[r] = (short) (lump - firstspritelump);
                sprtemp[frame].flip[r] = (byte) (flipped?1:0);
            }
            return;
            }
            
            // the lump is only used for one rotation
            if (sprtemp[frame].rotate == 0)
                I.Error  ("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump", spritename, 'A'+frame);
                
            sprtemp[frame].rotate = 1;

            // make 0 based
            rotation--;     
            if (sprtemp[frame].lump[rotation] != -1)
                I.Error  ("R_InitSprites: Sprite %s : %c : %c has two lumps mapped to it",
                 spritename, 'A'+frame, '1'+rotation);
                
            // Everything is OK, we can bless the temporary sprite's frame's rotation.
            sprtemp[frame].lump[rotation] = (short) (lump - firstspritelump);
            sprtemp[frame].flip[rotation] = (byte) (flipped?1:0);
        }




       /**
        * R_InitSpriteDefs
        * Pass a null terminated list of sprite names
        *  (4 chars exactly) to be used.
        * Builds the sprite rotation matrixes to account
        *  for horizontally flipped sprites.
        * Will report an error if the lumps are inconsistant. 
        * Only called at startup.
        *
        * Sprite lump names are 4 characters for the actor,
        *  a letter for the frame, and a number for the rotation.
        * A sprite that is flippable will have an additional
        *  letter/number appended.
        * The rotation character can be 0 to signify no rotations.
        */
        public void InitSpriteDefs (String[] namelist) 
        { 

            int     intname;
            int     frame;
            int     rotation;
            int     start;
            int     end;
            int     patched;
                
            if (namelist==null) return;
            numsprites = namelist.length;
            
            if (numsprites==0)
            return;
                
            sprites=new spritedef_t[numsprites];
            C2JUtils.initArrayOfObjects(sprites);
            
            start = firstspritelump-1;
            end = lastspritelump+1;
            
            // scan all the lump names for each of the names,
            //  noting the highest frame letter.
            // Just compare 4 characters as ints
            for (int i=0 ; i<numsprites ; i++)
            {
            System.out.println("Preparing sprite "+i);
            spritename = namelist[i];
            
            // FIXME: the original code actually set everything to "-1" here, including the "boolean" 
            // value. The idea was to create a "tristate" of sorts. Goto InstallSpriteLumps for more.
            for (int j=0;j<sprtemp.length;j++){
                Arrays.fill(sprtemp[j].flip,(byte)-1);
                Arrays.fill(sprtemp[j].lump,(short)-1);
                // This should be INDETERMINATE at this point.
                sprtemp[j].rotate=-1;
                }
                
            maxframe = -1;
            intname = name8.getIntName(namelist[i].toUpperCase());
            
            // scan the lumps,
            //  filling in the frames for whatever is found
            for (int l=start+1 ; l<end ; l++)
            {
                // We HOPE it has 8 characters.
                char[] cname=W.lumpinfo[l].name.toCharArray();
                if (cname.length==6 || cname.length==8) // Sprite names must be this way
                
                /* If the check is successful, we keep looking for more frames
                 * for a particular sprite e.g. TROOAx, TROOHxHy etc.    
                 */
                if (W.lumpinfo[l].intname == intname)
                {
                frame = cname[4] - 'A';
                rotation = cname[5] - '0';

                if (DM.modifiedgame)
                    patched = W.GetNumForName (W.lumpinfo[l].name);
                else
                    patched = l;

                InstallSpriteLump (patched, frame, rotation, false);
                
                // Second set of rotations?
                if (cname.length>6 && cname[6]!=0)
                {
                    frame = cname[6] - 'A';
                    rotation = cname[7] - '0';
                    InstallSpriteLump (l, frame, rotation, true);
                }
                }
            }
            
            /* check the frames that were found for completeness
             * This can only be -1 at this point if we didn't install
             * a single frame successfuly.
             */
            if (maxframe == -1)
            {
                System.out.println("Sprite "+spritename+" has no frames!");
                getSprites()[i].numframes = 0;
                // We move on to the next sprite with this one.
                continue;
            }
                
            maxframe++;
            
            for (frame = 0 ; frame < maxframe ; frame++)
            {
                switch ((int)sprtemp[frame].rotate)
                {
                  case -1:
                // no rotations were found for that frame at all
                I.Error ("R_InitSprites: No patches found for %s frame %c", namelist[i], frame+'A');
                break;
                
                  case 0:
                // only the first rotation is needed
                break;
                    
                  case 1:
                // must have all 8 frames
                for (rotation=0 ; rotation<8 ; rotation++)
                    if (sprtemp[frame].lump[rotation] == -1)
                    I.Error ("R_InitSprites: Sprite %s frame %c is missing rotations",
                         namelist[i], frame+'A');
                break;
                }
            }
            
            // allocate space for the frames present and copy sprtemp to it
            //  MAES: we can do that elegantly in one line.
            
            getSprites()[i].copy(sprtemp,maxframe);
            
            
            //sprites[i].numframes = maxframe;
            //sprites[i].spriteframes = new spriteframe_t[maxframe];
            //C2JUtils.initArrayOfObjects(sprites[i].spriteframes,spriteframe_t.class);
                
            //for (int j=0;j<)
            //System.arraycopy(src, srcPos, dest, destPos, length)
            //memcpy (sprites[i].spriteframes, sprtemp, maxframe*sizeof(spriteframe_t));
            }

        }

        //
        // GAME FUNCTIONS
        //
        vissprite_t[] vissprites;
        int    vissprite_p;
        int     newvissprite;


        /**
         * R_ClearSprites
         * Called at frame start.
         */
        public void ClearSprites ()
        {
           // vissprite_p = vissprites;
            vissprite_p=0;
        }


        //
        // R_NewVisSprite
        //
        vissprite_t overflowsprite;

        public vissprite_t NewVisSprite ()
        {
            if (vissprite_p == (MAXVISSPRITES-1))
            return overflowsprite;
            
            vissprite_p++;
            return vissprites[vissprite_p-1];
        }
      
        /**
         * R_DrawVisSprite
         *  mfloorclip and mceilingclip should also be set.
         *  
         * Sprites are actually drawn here.
         *
         */ 
        public final void
        DrawVisSprite
        ( vissprite_t      vis,
          int           x1,
          int           x2 )
        {
            //System.out.println("Drawing vissprite "+vis);
            column_t       column;
            int         texturecolumn;
            int     frac; // fixed_t
            patch_t        patch;
            
            // At this point, the view angle (and patch) has already been chosen. Go back.
            patch = W.CachePatchNum (vis.patch+firstspritelump,PU_CACHE);
            
            
            dc_colormap = vis.colormap;
            //colfunc=glasscolfunc;
            if (dc_colormap==null)
            {
            // NULL colormap = shadow draw
            colfunc = fuzzcolfunc;
            }
            else if ((vis.mobjflags & MF_TRANSLATION)!=0)
            {
            colfunc = DrawTranslatedColumn;
            dc_translation = translationtables;
            dcto=          - 256 +
                ( (vis.mobjflags & MF_TRANSLATION) >> (MF_TRANSSHIFT-8) );
            }
            
            dc_iscale = Math.abs(vis.xiscale)>>detailshift;
            dc_texturemid = vis.texturemid;
            frac = vis.startfrac;
            spryscale = vis.scale;
            sprtopscreen = centeryfrac - FixedMul(dc_texturemid,spryscale);
            
            for (dc_x=vis.x1 ; dc_x<=vis.x2 ; dc_x++, frac += vis.xiscale)
            {
            texturecolumn = frac>>FRACBITS;
        if(RANGECHECK){
            if (texturecolumn < 0 || texturecolumn >= patch.width)
                I.Error ("R_DrawSpriteRange: bad texturecolumn");
        }
            column = patch.columns[texturecolumn];
            //System.out.println(">>>>>>>>>>>>>>>>>>   Drawing column "+texturecolumn+" of  "+W.lumpinfo[vis.patch+firstspritelump].name +" at scale "+Integer.toHexString(vis.xiscale));
              
            //colfunc.invoke();
            DrawMaskedColumn(column);
            }

            colfunc = basecolfunc;
         }
       

        /**
         * R_ProjectSprite
         * Generates a vissprite for a thing
         * if it might be visible.
         * 
         * @param thing
         */
        public void ProjectSprite (mobj_t thing)
        {
            int     tr_x,tr_y;          
            int     gxt,gyt;          
            int     tx,tz;

            int     xscale,x1,x2;

            spritedef_t    sprdef;
            spriteframe_t  sprframe;
            int         lump;
            
            int        rot;
            boolean     flip;
            
            int         index;

            vissprite_t    vis;
            
            long     ang;
            int     iscale;
            
            // transform the origin point
            tr_x = thing.x - viewx;
            tr_y = thing.y - viewy;
            
            gxt = FixedMul(tr_x,viewcos); 
            gyt = -FixedMul(tr_y,viewsin);
            
            tz = gxt-gyt; 

            // thing is behind view plane?
            if (tz < MINZ)
            return;
            /* MAES: so projection/tz gives horizontal scale */
            xscale = FixedDiv(projection, tz);
            
            gxt = -FixedMul(tr_x,viewsin); 
            gyt = FixedMul(tr_y,viewcos); 
            tx = -(gyt+gxt); 

            // too far off the side?
            if (Math.abs(tx)>(tz<<2))
            return;
            
            // decide which patch to use for sprite relative to player
        if(RANGECHECK){
            if (thing.sprite.ordinal() >= numsprites)
            I.Error ("R_ProjectSprite: invalid sprite number %i ",
                 thing.sprite);
        }
            sprdef = getSprites()[thing.sprite.ordinal()];
        if(RANGECHECK){
            if ( (thing.frame&FF_FRAMEMASK) >= sprdef.numframes )
            I.Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
                 thing.sprite, thing.frame);
        }
            sprframe = sprdef.spriteframes[ thing.frame & FF_FRAMEMASK];

            if (sprframe.rotate!=0)
            {
            // choose a different rotation based on player view
            // FIXME: this is obviously wrong.
            ang = PointToAngle (thing.x, thing.y);
            rot = (int) ((ang-thing.angle+(ANG45*9)/2)&BITS32)>>>29;
            lump = sprframe.lump[rot];
            flip = (boolean)(sprframe.flip[rot]!=0);
            }
            else
            {
            // use single rotation for all views
            lump = sprframe.lump[0];
            flip = (boolean)(sprframe.flip[0]!=0);
            }
            
            // calculate edges of the shape
            tx -= spriteoffset[lump];   
            x1 = (centerxfrac + FixedMul (tx,xscale) ) >>FRACBITS;

            // off the right side?
            if (x1 > viewwidth)
            return;
            
            tx +=  spritewidth[lump];
            x2 = ((centerxfrac + FixedMul (tx,xscale) ) >>FRACBITS) - 1;

            // off the left side
            if (x2 < 0)
            return;
            
            // store information in a vissprite
            vis = NewVisSprite ();
            vis.mobjflags = thing.flags;
            vis.scale = xscale<<detailshift;
            vis.gx = thing.x;
            vis.gy = thing.y;
            vis.gz = thing.z;
            vis.gzt = thing.z + spritetopoffset[lump];
            vis.texturemid = vis.gzt - viewz;
            vis.x1 = x1 < 0 ? 0 : x1;
            vis.x2 = x2 >= viewwidth ? viewwidth-1 : x2;
            /* This actually determines the general sprite scale) 
             * iscale = 1/xscale, if this was floating point. */
            iscale = FixedDiv (FRACUNIT, xscale);

            if (flip)
            {
            vis.startfrac = spritewidth[lump]-1;
            vis.xiscale = -iscale;
            }
            else
            {
            vis.startfrac = 0;
            vis.xiscale = iscale;
            }

            if (vis.x1 > x1)
            vis.startfrac += vis.xiscale*(vis.x1-x1);
            vis.patch = lump;
            
            // get light level
            if ((thing.flags & MF_SHADOW)!=0)
            {
            // shadow draw
            vis.colormap = null;
            }
            else if (fixedcolormap!=null)
            {
            // fixed map
            vis.colormap = fixedcolormap;
            //vis.pcolormap=0;
            }
            else if ((thing.frame & FF_FULLBRIGHT)!=0)
            {
            // full bright
            vis.colormap = colormaps[0];
            //vis.pcolormap=0;
            }
            
            else
            {
            // diminished light
            index = xscale>>(LIGHTSCALESHIFT-detailshift);

            if (index >= MAXLIGHTSCALE) 
                index = MAXLIGHTSCALE-1;

            vis.colormap = spritelights[index];
            //vis.pcolormap=index;
            }   
        }




        /**
         * R_AddSprites
         * 
         * During BSP traversal, this adds sprites by sector.
         * 
         * 
         */
        public void AddSprites (sector_t sec)
        {
            if (DEBUG) System.out.println("AddSprites");
            mobj_t     thing;
            int         lightnum;

            // BSP is traversed by subsector.
            // A sector might have been split into several
            //  subsectors during BSP building.
            // Thus we check whether its already added.
            if (sec.validcount == validcount)
            return;     

            // Well, now it will be done.
            sec.validcount = validcount;
            
            lightnum = (sec.lightlevel >> LIGHTSEGSHIFT)+extralight;

            if (lightnum < 0)       
            spritelights = scalelight[0];
            else if (lightnum >= LIGHTLEVELS)
            spritelights = scalelight[LIGHTLEVELS-1];
            else
            spritelights = scalelight[lightnum];

            // Handle all things in sector.
            for (thing = sec.thinglist ; thing!=null ; thing = (mobj_t) thing.snext)
            ProjectSprite (thing);
        }

       
        
        /**
         * R_DrawPSprite
         * 
         * Draws a "player sprite" with slighly different rules than normal sprites.
         * This is actually a PITA, at best :-/
         * 
         */
        
        public final void DrawPSprite (pspdef_t psp)
        {
        	
            int     tx;
            int         x1;
            int         x2;
            spritedef_t    sprdef;
            spriteframe_t  sprframe;
            vissprite_t vis;
            int         lump;
            boolean     flip;
            
            // 
            
            // decide which patch to use (in terms of angle?)
            if(RANGECHECK){
            if ( psp.state.sprite.ordinal() >= numsprites)
            I.Error ("R_ProjectSprite: invalid sprite number %i ",
                 psp.state.sprite);
            }
        
            sprdef = getSprites()[psp.state.sprite.ordinal()];
        
            if(RANGECHECK){
            if ( (psp.state.frame & FF_FRAMEMASK)  >= sprdef.numframes)
            I.Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
                 psp.state.sprite, psp.state.frame);
        	}
            sprframe = sprdef.spriteframes[psp.state.frame & FF_FRAMEMASK ];

            // Base frame for "angle 0" aka viewed from dead-front.
            lump = sprframe.lump[0];
            // TODO: where can this be set?
            flip = (boolean)(sprframe.flip[0]!=0);
           
            // calculate edges of the shape. tx is expressed in "view units".

            // OPTIMIZE: if weaponadjust is computed in-place, noticeable slowdown occurs.
            tx = (int) (FixedMul(psp.sx,BOBADJUST)-WEAPONADJUST);
            
            tx -= spriteoffset[lump];
            
            // So...centerxfrac is the center of the screen (pixel coords in fixed point).
            // 
            x1 = (centerxfrac + FixedMul (tx,pspritescale) ) >>FRACBITS;
            
            // off the right side
            if (x1 > viewwidth)
            return;     

            tx +=  spritewidth[lump];
            x2 = ((centerxfrac + FixedMul (tx, pspritescale) ) >>FRACBITS) - 1;
            
            // off the left side
            if (x2 < 0)
            return;
            
            // store information in a vissprite ?
            vis = avis;
            vis.mobjflags = 0;
            vis.texturemid = (BASEYCENTER<<FRACBITS)+FRACUNIT/2-(psp.sy-spritetopoffset[lump]);
            vis.x1 = x1 < 0 ? 0 : x1;
            vis.x2 = x2 >= viewwidth ? viewwidth-1 : x2;   
            vis.scale =  (pspritescale)<<detailshift;
            
            if (flip)
            {
            vis.xiscale = -pspriteiscale;
            vis.startfrac = spritewidth[lump]-1;
            }
            else
            {
            vis.xiscale = pspriteiscale;
            vis.startfrac = 0;
            }
            
            if (vis.x1 > x1)
            vis.startfrac += vis.xiscale*(vis.x1-x1);

            vis.patch = lump;

            if ((viewplayer.powers[pw_invisibility] > 4*32)
            || (viewplayer.powers[pw_invisibility] & 8)!=0)
            {
            // shadow draw
            vis.colormap = null;
            
            }
            else if (fixedcolormap!=null)
            {
            // fixed color
            vis.colormap = fixedcolormap;
            //vis.pcolormap=0;
            }
            else if ((psp.state.frame & FF_FULLBRIGHT)!=0)
            {
            // full bright
            vis.colormap = colormaps[0];
            //vis.pcolormap=0;
            }
            else
            {
            // local light
            vis.colormap = spritelights[MAXLIGHTSCALE-1];
            }
            
//            System.out.println("Weapon draw "+vis);
             DrawVisSprite (vis, vis.x1, vis.x2);
        }

        /** used inside DrawPSprite, better make this static */
            //vis=new vissprite_t();
            protected vissprite_t avis=new vissprite_t();

        /** R_DrawPlayerSprites 
         * 
         *  This is where stuff like guns is drawn...right?
         * 
         * 
         * */
        
        protected final void DrawPlayerSprites ()
        {
            int     i;
            int     lightnum;
            pspdef_t   psp;
            
            // get light level
            lightnum =
            (viewplayer.mo.subsector.sector.lightlevel >> LIGHTSEGSHIFT) 
            +extralight;

            if (lightnum < 0)       
            spritelights = scalelight[0];
            else if (lightnum >= LIGHTLEVELS)
            spritelights = scalelight[LIGHTLEVELS-1];
            else
            spritelights = scalelight[lightnum];
            
            // clip to screen bounds
            mfloorclip = screenheightarray;
            p_mfloorclip=0;
            mceilingclip = negonearray;
            p_mceilingclip=0;
            
            // add all active psprites
            for (i=0, psp=viewplayer.psprites[i];
             i<NUMPSPRITES;
             i++)
            {
            if (psp.state!=null)
                DrawPSprite (psp);
            }
        }

        vissprite_t vsprsortedhead;
        // A dummy used as temp during sorting.
        vissprite_t     unsorted;
        
        /**
         * R_SortVisSprites
         * 
         * UNUSED more efficient Comparable
         * sorting + built-in Arrays.sort function used.
         * 
         */

        public void SortVisSprites ()
        {
            int         count;
            vissprite_t    best;
            int     bestscale; // fixed_t

            count = vissprite_p; //- vissprites;
            
            // "breaks" unsorted from any previous linking.          
            unsorted.next = unsorted.prev = unsorted;

            if (count==0)
            return;

            // Tranverses the master visspite list, and "links"
            // each sprite to one another in order of appearance. This is O(n)
            // FIXME: it can't start at 0.
            
            vissprites[0].next=vissprites[Math.min(1,vissprite_p)];
            for (int ds=1 ; ds<vissprite_p ; ds++)
            {
                vissprites[ds].next = vissprites[ds+1];
                // This is bullshit. Dunno how they got away with it in C, it would cause
                // a garbage pointer, which was not a problem because unsorted was "poked in"
                // anyway. Fuck that shit, John Romero. 
                
                vissprites[ds].prev = vissprites[ds-1];
            }
            
            // "unsorted" vissprite is inserted at the head.
            vissprites[0].prev = unsorted;
            unsorted.next = vissprites[0];
            // ...and at the end.
            vissprites[vissprite_p-1].next = unsorted;
            unsorted.prev = vissprites[vissprite_p-1];
            
            // pull the vissprites out by scale
            best = null;     // shut up the compiler warning
            // "breaks" vsprsortedhead from any previous linking. 
            vsprsortedhead.next = vsprsortedhead.prev = vsprsortedhead;
            
            // For each vissprite...
            for (int i=0 ; i<count ; i++)
            {
            bestscale = Integer.MAX_VALUE;
            
            // Transverses the linked list.
            for (vissprite_t ds=unsorted.next ; ds!= unsorted ; ds=ds.next)
            {
                if (ds.scale < bestscale)
                {
                bestscale = ds.scale;
                best = ds;
                }
            }
            
            // Once the element with the SMALLER scale is found..
            
            // best is unlinked
            best.next.prev = best.prev;
            best.prev.next = best.next;
            
            // best is placed at the head of the list, sandwiched between whatever was before the head
            best.next = vsprsortedhead;
            best.prev = vsprsortedhead.prev;
            vsprsortedhead.prev.next = best;
            vsprsortedhead.prev = best;
            }
        }





        protected short[]       clipbot=new short[SCREENWIDTH];
        protected short[]       cliptop=new short[SCREENWIDTH];
        
        /**
         * R_DrawSprite
         */
        
        public void DrawSprite (vissprite_t spr)
        {
            int      ds;
            drawseg_t dss;

            int         x;
            int         r1;
            int         r2;
            int     scale; // fixed
            int     lowscale; // fixed
            int         silhouette;
                
            for (x = spr.x1 ; x<=spr.x2 ; x++)
            clipbot[x] = cliptop[x] = -2;
            
            // Scan drawsegs from end to start for obscuring segs.
            // The first drawseg that has a greater scale
            //  is the clip seg.
            for (ds=ds_p-1 ; ds >= 0 ; ds--)
            {
                // determine if the drawseg obscures the sprite
                //System.out.println("Drawseg "+ds+"of "+(ds_p-1));
                dss=drawsegs[ds];
            if (dss.x1 > spr.x2
                || dss.x2 < spr.x1
                || ((dss.silhouette==0)
                && (dss.nullMaskedTextureCol())))
            {
                // does not cover sprite
                continue;
            }
                    
            r1 = dss.x1 < spr.x1 ? spr.x1 : dss.x1;
            r2 = dss.x2 > spr.x2 ? spr.x2 : dss.x2;

            if (dss.scale1 > dss.scale2)
            {
                lowscale = dss.scale2;
                scale = dss.scale1;
            }
            else
            {
                lowscale = dss.scale1;
                scale = dss.scale2;
            }
                
            if (scale < spr.scale
                || ( lowscale < spr.scale
                 && (dss.curline.PointOnSegSide (spr.gx, spr.gy)==0) ) )
            {
                // masked mid texture?
                if (!dss.nullMaskedTextureCol())   
                MySegs.RenderMaskedSegRange (dss, r1, r2);
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
            if (silhouette == 1)
            {
                // bottom sil
                for (x=r1 ; x<=r2 ; x++)
                if (clipbot[x] == -2)
                    // clipbot[x] = ds->sprbottomclip[x];
                    if (dss.nullSprBottomClip()){
                        if (DEBUG) System.out.println("Bottom clipping requested but clip list not present for "+dss);
                        
                    } else clipbot[x] = dss.getSprBottomClip(x);
                    
                
            }
            else if (silhouette == 2)
            {
                // top sil
                for (x=r1 ; x<=r2 ; x++)
                if (cliptop[x] == -2)
                    cliptop[x] = dss.getSprTopClip(x);
            }
            else if (silhouette == 3)
            {
                // both
                for (x=r1 ; x<=r2 ; x++)
                {
                if (clipbot[x] == -2)
                    clipbot[x] = dss.getSprBottomClip(x);
                if (cliptop[x] == -2)
                    cliptop[x] = dss.getSprTopClip(x);
                }
            }
                
            }
            
            // all clipping has been performed, so draw the sprite

            // check for unclipped columns
            for (x = spr.x1 ; x<=spr.x2 ; x++)
            {
            if (clipbot[x] == -2)       
                clipbot[x] = (short) viewheight;
            // ?? What's this bullshit?
            if (cliptop[x] == -2)
                cliptop[x] = -1;
            }
                
            mfloorclip = clipbot;
            p_mfloorclip=0;
            mceilingclip = cliptop;
            p_mceilingclip=0;
            DrawVisSprite (spr, spr.x1, spr.x2);
        }
        
        /** An alternate comb sort implementation. It doesn't perform as well
         *  as a generic quicksort/mergesort, but performs pretty close.
         *  
         * @param input
         * @param length
         */

        protected final void combSort(vissprite_t[] input, int length) {
            int gap=length;
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
         * Shellsort, using a sequence suggested by Gonnet.
         * @param a an array of Comparable items.
         */
        protected final void shellsort( Comparable [ ] a , int length)
        {
            for( int gap = length / 2; gap > 0;
                         gap = gap == 2 ? 1 : (int) ( gap / 2.2 ) )
                for( int i = gap; i < length; i++ )
                {
                    Comparable tmp = a[ i ];
                    int j = i;

                    for( ; j >= gap && tmp.compareTo( a[ j - gap ] ) < 0; j -= gap )
                        a[ j ] = a[ j - gap ];
                    a[ j ] = tmp;
                }
        }

        
        /**
         * R_DrawMasked
         * 
         * Sorts and draws vissprites (room for optimization in sorting func.)
         * Draws masked textures.
         * Draws player weapons and overlays (psprites).
         * 
         * Sorting function can be swapped for almost anything, and it will work better,
         * in-place and be simpler to draw, too.
         * 
         * 
         */
        protected final void DrawMasked ()
        {
           //vissprite_t    spr;
           int ds;
           drawseg_t dss;
            
           // Well, it sorts visspite objects.
           // It actually IS faster to sort with comparators, but you need to go into NUTS.WAD-like wads.
           // numbers. The built-in sort if about as good as it gets. In fact, it's hardly slower
           // to draw sprites without sorting them when using the built-in modified mergesort, while
           // the original algorithm is so dreadful it actually does slow things down.
           
           Arrays.sort(vissprites,0,vissprite_p);
          
           // If you are feeling adventurous, try these ones. They *might* perform
           // better in very extreme situations where all sprites are always on one side
           // of your view, but I hardly see any benefits in that. They are both
           // much better than the original anyway.
           
           // combSort(vissprites,vissprite_p);           
           //shellsort(vissprites,vissprite_p);
           
           // pQuickSprite.sort(vissprites);
           
           // The original sort. It's incredibly bad on so many levels (uses a separate
           // linked list for the sorted sequence, which is pointless since the vissprite_t
           // array is gonna be changed all over in the next frame anyway, it's not like
           // it helps preseving or anything. It does work in Java too, but I'd
           // say to Keep Away. No srsly.
           
          /* SortVisSprites ();

            // Sprite "0" not visible?
           /*if (vissprite_p > 0)
            {
            // draw all vissprites back to front
            for (spr = vsprsortedhead.next ;
                 spr != vsprsortedhead ;
                 spr=spr.next)
            {
                
                DrawSprite (spr);
            }
            }*/
            
           // After using in-place sorts, sprites can be drawn as simply as that.
           
           for (int i=0;i<vissprite_p;i++){
               DrawSprite (vissprites[i]);
           }
               
           
            // render any remaining masked mid textures
            for (ds=ds_p-1 ; ds >= 0 ; ds--) {
                dss=drawsegs[ds];
                          if (!dss.nullMaskedTextureCol())
                MySegs.RenderMaskedSegRange (dss, dss.x1, dss.x2);
                }
            // draw the psprites on top of everything
            //  but does not draw on side views
            //if (viewangleoffset==0)
            
            colfunc=playercolfunc;            
            DrawPlayerSprites ();
            colfunc=basecolfunc;
    }
        
    }
    
    protected interface SegDrawer{

        abstract void RenderMaskedSegRange(drawseg_t dss, int r1, int r2);

        abstract void StoreWallRange(int first, int last);
        
    }
    
    protected interface PlaneDrawer{

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
    
    
    ///////////////////////// LIGHTS, POINTERS, COLORMAPS ETC. ////////////////
    
   ///// FROM R_DATA, R_MAIN , R_DRAW //////////
    
    /** OK< this is supposed to "peg" into screen buffer 0. It will work AS LONG AS SOMEONE FUCKING ACTUALLY SETS IT !!!! */
    protected byte[] screen;

    // status bar height at bottom of screen
    protected static final int SBARHEIGHT = 32;

    protected static final boolean RANGECHECK = false;


    protected byte[] viewimage;

     /** These are actually offsets inside screen 0 (or any screen). Therefore anything using
     * them should "draw" inside screen 0 */
    protected int[] ylookup = new int[MAXHEIGHT];

    /** Columns offset to set where?! */
    protected int[] columnofs = new int[MAXWIDTH];

    /** Color tables for different players,
       translate a limited part to another
       (color ramps used for suit colors).
       */
    
    protected byte[][] translations = new byte[3][256];
    
    /** MAES: this was a typedef for unsigned bytes, called "lighttable_t". It makes more sense
     * to make it primitive, since it's performance-critical in the renderer. Now, whether this should be made
     * bytes or shorts or chars is debatable.
     */
    protected byte[] dc_colormap;
    
    /** Adapted from R. Killough's Boom code. Used to accelerate drawing of power-of-two textures,
     *  and to prevent tutti-frutti.  
     */
    protected int dc_texheight;
    
    /** Offset. use as dc_colormap[dco+<shit>]. Also, when you set dc_colormap = crap[index],
     *  set dc_colormap=crap and  dco=index */
    protected int dco;

    protected int dc_x;

    protected int dc_yl;

    protected int dc_yh;

    /** fixed_t */
    protected int dc_iscale;

    /** fixed_t */
    protected int dc_texturemid;

    /** first pixel in a column (possibly virtual). Set dc_source_ofs to simulate pointer aliasing */
    protected byte[] dc_source;
    /** when passing dc_source around, also set this */ 
    protected int dc_source_ofs;
    
    // byte[] dc_data;

    // just for profiling
    protected int dccount;
    
    //
    // Lighting LUT.
    // Used for z-depth cuing per column/row,
    //  and other lighting effects (sector ambient, flash).
    //

    // Lighting constants.
    // Now why not 32 levels here?
    /**  These two are tied by an inverse relationship.
     *  E.g. 256 levels, 0 shift
     *  128 levels, 1 shift
     *  ...etc...
     *  16 levels, 4 shift (default).
     *  Or even less, if you want.
     *  
     *  By setting it to the max however 
     *  you get smoother light and get rid of 
     *  lightsegshift globally, too.
     */
    protected  static final int LIGHTLEVELS=32, LIGHTSEGSHIFT=3;

    // These are a bit more tricky to figure out though.
    
    protected  static final int MAXLIGHTSCALE=       48;
    protected  static final int LIGHTSCALESHIFT =    12;
    protected static final int MAXLIGHTZ      =    128;
    protected  static final int LIGHTZSHIFT    = 20;

   /** Fineangles in the SCREENWIDTH wide window. */
    protected  static final int FIELDOFVIEW   =   FINEANGLES/4;   
   
   /** Use in conjunction with pfixedcolormap */
   protected byte[]      fixedcolormap;
   /** Use in conjunction with fixedcolormap[] */
   protected int pfixedcolormap;
   //lighttable_t[][]  walllights;

   protected int          centerx;
   protected int          centery;

   /** Used to determind the view center and projection in view units fixed_t */
   protected int centerxfrac,centeryfrac, projection;

   /** just for profiling purposes */
   protected  int         framecount; 

   protected  int         sscount;
   protected  int         linecount;
   protected  int         loopcount;

   /** fixed_t */
   protected  int         viewx, viewy,viewz;

   //MAES: an exception to strict type safety. These are used only in here, anyway (?) and have no special functions.
   //Plus I must use them as indexes. angle_t
   protected long  viewangle;

   /** fixed */
   protected int          viewcos,viewsin;

   protected player_t     viewplayer;

   // 0 = high, 1 = low. Normally only the menu and the interface can change that.
   protected int          detailshift;    

   //
   // precalculated math tables
   //
   protected  long           clipangle;
   
   // Set to 2*clipangle later.
   protected  long CLIPANGLE2;

   // The viewangletox[viewangle + FINEANGLES/4] lookup
   // maps the visible view angles to screen X coordinates,
   // flattening the arc to a flat projection plane.
   // There will be many angles mapped to the same X. 

   protected final int[]         viewangletox=new int[FINEANGLES/2];

   /** The xtoviewangleangle[] table maps a screen pixel
    * to the lowest viewangle that maps back to x ranges
    * from clipangle to -clipangle. */
   protected final long[]         xtoviewangle=new long[SCREENWIDTH+1];


   // UNUSED.
   // The finetangentgent[angle+FINEANGLES/4] table
   // holds the fixed_t tangent values for view angles,
   // ranging from MININT to 0 to MAXINT.
   // fixed_t      finetangent[FINEANGLES/2];

   // fixed_t      finesine[5*FINEANGLES/4];
   // MAES: uh oh. So now all these ints must become finesines? fuck that.
   // Also wtf @ this hack....this points to approx 1/4th of the finesine table, but what happens if I read past it?
   // int[]        finecosine = finesine[FINEANGLES/4];

   /* MAES: what's going on with light tables here. OK...so these should be
    * "unsigned bytes", since, after all, they'll be used as pointers inside an
    * array to finally pick a color, so they should be expanded to shorts.
    * 
    */
   
   
   protected byte[][][]     scalelight=new byte[LIGHTLEVELS][MAXLIGHTSCALE][];
   protected byte[][]       scalelightfixed=new byte[MAXLIGHTSCALE][];
   protected byte[][][]     zlight=new byte[LIGHTLEVELS][MAXLIGHTZ][];

   // bumped light from gun blasts
   protected static int           extralight;         
    
   ////////////// SOME UTILITY METHODS /////////////

   public final long
   PointToAngle2
   ( fixed_t    x1,
     fixed_t    y1,
     fixed_t    x2,
     fixed_t    y2 )
   {    
       // Careful with assignments...
       viewx=x1.val;
       viewy=y1.val;
       
       return PointToAngle (x2.val, y2.val);
   }

   /** Assigns a point of view before calling PointToAngle */
   
   public final long
   PointToAngle2
   ( int   x1,
     int   y1,
     int   x2,
     int   y2 )
   {   
       // Careful with assignments...
       viewx=x1;
       viewy=y1;
       
       return PointToAngle (x2, y2);
   }



   protected final int
   PointToDist
   ( fixed_t    x,
     fixed_t    y )
   {
       int      angle;
       int  dx;
       int  dy;
       int  temp;
       int  dist;
    
       dx = Math.abs(x.val - viewx);
       dy = Math.abs(y.val - viewy);
    
       if (dy>dx)
       {
    temp = dx;
    dx = dy;
    dy = temp;
       }
    
       angle = (int) ((tantoangle[ FixedDiv(dy,dx)>>DBITS ]+ANG90) >> ANGLETOFINESHIFT);

       // use as cosine
       dist = FixedDiv (dx, finesine[angle] );  
    
       return dist;
   }




   //
   // R_InitPointToAngle
   //
/*   protected final void InitPointToAngle ()
   {
       // UNUSED - now getting from tables.c
   if (false){
       int  i;
       long t;
       float    f;
   //
   // slope (tangent) to angle lookup
   //
       for (i=0 ; i<=SLOPERANGE ; i++)
       {
    f = (float) Math.atan( (double)(i/SLOPERANGE )/(3.141592657*2));
    t = (long) (0xffffffffL*f);
    tantoangle[i] = (int) t;
       }
   }
   } */
   
   /**
    * R_PointToAngle
    *  To get a global angle from cartesian coordinates,
    *  the coordinates are flipped until they are in
    *  the first octant of the coordinate system, then
    *  the y (<=x) is scaled and divided by x to get a
    *  tangent (slope) value which is looked up in the
    *   tantoangle[] table.
    *   
    *   @param xx (fixed_t)
    *   @param yy (fixed_t)
    */

   protected final long
   PointToAngle
   ( int   x,
     int   y )
   {   
       // MAES: note how we don't use &BITS32 here. That is because we know that the maximum possible
       // value of tantoangle is angle
       // This way, we are actually working with vectors emanating from our current position.
       x-= viewx;
       y-= viewy;
       
       if ( (x==0) && (y==0) )
       return 0;

       if (x>= 0)
       {
       // x >=0
       if (y>= 0)
       {
           // y>= 0

           if (x>y)
           {
           // octant 0
           return tantoangle[ SlopeDiv(y,x)];
           }
           else
           {
           // octant 1
           return (ANG90-1-tantoangle[ SlopeDiv(x,y)]);
           }
       }
       else
       {
           // y<0
           y = -y;

           if (x>y)
           {
           // octant 8
           return (-tantoangle[SlopeDiv(y,x)])&BITS32;
           }
           else
           {
           // octant 7
           return ANG270+tantoangle[ SlopeDiv(x,y)];
           }
       }
       }
       else
       {
       // x<0
       x = -x;

       if (y>= 0)
       {
           // y>= 0
           if (x>y)
           {
           // octant 3
           return (ANG180-1-tantoangle[ SlopeDiv(y,x)]);
           }
           else
           {
           // octant 2
           return (ANG90+ tantoangle[ SlopeDiv(x,y)]);
           }
       }
       else
       {
           // y<0
           y = -y;

           if (x>y)
           {
           // octant 4
           return (ANG180+tantoangle[ SlopeDiv(y,x)]);
           }
           else
           {
            // octant 5
           return (ANG270-1-tantoangle[ SlopeDiv(x,y)]);
           }
       }
       }
       // This is actually unreachable.
      // return 0;
   }
   
   /**
    * R_ScaleFromGlobalAngle
    * Returns the texture mapping scale
    *  for the current line (horizontal span)
    *  at the given angle.
    * rw_distance must be calculated first.
    */
   
   protected final int ScaleFromGlobalAngle (long visangle)
   {
       int         scale; // fixed_t
       long         anglea;
       long         angleb;
       int         sinea;
       int         sineb;
       int         num; // fixed_t
       int         den;

       // UNUSED
   /*
   {
       fixed_t     dist;
       fixed_t     z;
       fixed_t     sinv;
       fixed_t     cosv;
       
       sinv = finesine[(visangle-rw_normalangle)>>ANGLETOFINESHIFT];   
       dist = FixedDiv (rw_distance, sinv);
       cosv = finecosine[(viewangle-visangle)>>ANGLETOFINESHIFT];
       z = abs(FixedMul (dist, cosv));
       scale = FixedDiv(projection, z);
       return scale;
   }
   */

       anglea = (ANG90 +visangle-viewangle)&BITS32;
       angleb = (ANG90 +visangle-rw_normalangle)&BITS32;

       // both sines are allways positive
       sinea = finesine(anglea); 
       sineb = finesine(angleb);
       num = FixedMul(projection,sineb)<<detailshift;
       den = FixedMul(rw_distance,sinea);

       if (den > num>>16)
       {
       scale = FixedDiv (num, den);

       if (scale > 64*FRACUNIT)
           scale = 64*FRACUNIT;
       else if (scale < 256)
           scale = 256;
       }
       else
       scale = 64*FRACUNIT;
       
       return scale;
   }



   //
   // R_InitTables
   //
   protected final void InitTables ()
   {
       // UNUSED: now getting from tables.c
   /*
       int     i;
       float   a;
       float   fv;
       int     t;
       
       // viewangle tangent table
       for (i=0 ; i<FINEANGLES/2 ; i++)
       {
       a = (i-FINEANGLES/4+0.5)*PI*2/FINEANGLES;
       fv = FRACUNIT*tan (a);
       t = fv;
       finetangent[i] = t;
       }
       
       // finesine table
       for (i=0 ; i<5*FINEANGLES/4 ; i++)
       {
       // OPTIMIZE: mirro..
       a = (i+0.5)*PI*2/FINEANGLES;
       t = FRACUNIT*sin (a);
       finesine[i] = t;
       }
   */

   }

   /** R_PointToDist
    * 
    * @param x fixed_t
    * @param y fixed_t
    * @return
    */
   
   protected final int
   PointToDist
   ( int   x,
     int   y )
   {
       int     angle;
       int dx;
       int dy;
       int temp;
       int dist;
       
       dx = Math.abs(x - viewx);
       dy = Math.abs(y - viewy);
       
       // Sanity check, else it's going to bomb.
       /* if (dx==0){
           //
           if (dy>0) angle= Tables.toBAMIndex(ANG90);
           else           
           if (dy<0) angle= Tables.toBAMIndex(ANG270);
           else
                     angle= 0;     
       }
       
       else { */
       
       // If something is farther north/south than west/east, it gets swapped. 
       // Probably as a crude way to avoid divisions by zero. This divides
       // the field into octants, rather than quadrants, where the biggest angle to consider is 45...right?
       
       if (dy>dx)
       {
       temp = dx;
       dx = dy;
       dy = temp;
       }
       
       
       /* If both dx and dy are zero, this is going to bomb.
          Fixeddiv will return MAXINT aka 7FFFFFFF, >> DBITS will make it 3FFFFFF,
          which is enough to break tantoangle[]. 
          
          In the original C code, this probably didn't matter: there would probably be garbage orientations
          thrown all around. However this is unacceptable in Java however.
       */
       
       angle=Math.max( FixedDiv(dy,dx), 2048)>>DBITS;
           
       angle = (int) (((tantoangle[angle ]+ANG90)&BITS32) >> ANGLETOFINESHIFT);
       
       // use as cosine
       dist = FixedDiv (dx, finesine[angle] ); 
       
       return dist;
   }  
   
   ///////////////// COLORMAPS ///////////////////////////////
   
   /** use paired with dcto */
   protected byte[] dc_translation;
   /** DC Translation offset */
   protected int dcto;

   /** used paired with tto */
   protected byte[] translationtables;
   /** translation tables offset */
   protected int tto;
   
   
   ///////////////// COMMON RENDERING GLOBALS ////////////////
   
   /* ON COLORMAPS: they are supposed to be "from this color to some other color" mappings.
    * "Color" means an index in the palette, like those used in screen. Their contents
    * should be bytes, although their indexing has to be "unsigned bytes" the very least.
    * 
    * Lengths: for some reas
    * 
    */
   
   
   /** "peg" this to the one from RendererData */
   protected byte[][] colormaps;
   
   //// FROM SEGS ////
   /** angle_t, used after adding ANG90 in StoreWallRange */
   protected long     rw_normalangle;
   
// OPTIMIZE: closed two sided lines as single sided

   /** True if any of the segs textures might be visible. */
   protected boolean     segtextured;    

   /** False if the back side is the same plane. */
   protected boolean     markfloor, markceiling;

   protected boolean     maskedtexture;
   protected int     toptexture;
   protected int     bottomtexture;
   protected int     midtexture;

   /** angle to line origin */
   protected long     rw_angle1;  

   //
   // regular wall
   //
   protected int     rw_x;
   protected int     rw_stopx;
   protected long     rw_centerangle; // angle_t
   /** fixed_t */
   protected int     rw_offset,rw_distance,rw_scale,
   rw_scalestep,rw_midtexturemid,rw_toptexturemid,rw_bottomtexturemid;

   protected int     worldtop;
   protected int     worldbottom;
   protected int     worldhigh;
   protected int     worldlow;

   /** fixed_t */
   protected int     pixhigh,pixlow,pixhighstep,pixlowstep,
   topfrac,    topstep,bottomfrac, bottomstep;

   /** lighttable_t** */
   protected byte[][]   walllights;

   protected short[]     maskedtexturecol;
   protected int pmaskedtexturecol=0;
       
   ///// FROM BSP /////////
   
   /** pointer to drawsegs */
   protected int   ds_p;
   
   protected drawseg_t[]    drawsegs;

   /** The sectors of the line currently being considered */
   protected sector_t    frontsector,backsector;

   protected seg_t       curline;
   protected side_t      sidedef;
   protected line_t      linedef;
   
   ///////////////////// Renderer Data ////////////////////////
   
  

   /** needed for pre rendering (fixed_t[]) */
   protected int[]    spritewidth,spriteoffset,spritetopoffset;
   


     protected interface colfunc_t {
         public void invoke();
     }
     
     
 /////////////////////// COLUMN AND SPAN FUNCTION ////////////////////////////////////////
     
     // Fuck that shit. Amma gonna do it the fastest way possible.
     
     protected colfunc_t colfunc;
     protected colfunc_t basecolfunc;
     protected colfunc_t fuzzcolfunc;
     protected colfunc_t transcolfunc;
     protected colfunc_t glasscolfunc;
     protected colfunc_t playercolfunc;
     protected colfunc_t spanfunc;

     protected colfunc_t DrawTranslatedColumn;
     protected colfunc_t DrawColumnPlayer;
     protected colfunc_t DrawFuzzColumn;
     protected colfunc_t DrawColumnLow;
     protected colfunc_t DrawColumn;
     protected colfunc_t DrawTLColumn;
     
     /** to be set in UnifiedRenderer */
     protected colfunc_t DrawSpan,DrawSpanLow;
     
     

/////////////////////////////// r_draw methods //////////////////////////////////////

     /**
      * Copy a screen buffer.
      * Actually, it's hardcoded to copy stuff from screen 1 to screen 0.
      * Used to overlay stuff like beveled edges that don't need to be updated that often. * 
      * 
      *   LFB copy.
      *   This might not be a good idea if memcpy
      *   is not optiomal, e.g. byte by byte on
      *   a 32bit CPU, as GNU GCC/Linux libc did
      *    at one point.
      */
     public void VideoErase(int ofs, int count) {

         // memcpy (screens[0]+ofs, screens[1]+ofs, count);
         System.arraycopy(V.getScreen(1), ofs, V.getScreen(0), ofs, count);

     }

     /*
      * R_DrawViewBorder
      * Draws the border around the view
      * for different size windows?
      */

      public void DrawViewBorder() {
          int top;
          int side;
          int ofs;
          int i;

          if (scaledviewwidth == SCREENWIDTH)
              return;

          top = ((SCREENHEIGHT - SBARHEIGHT) - viewheight) / 2;
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
          V.MarkRect(0, 0, SCREENWIDTH, SCREENHEIGHT - SBARHEIGHT);
      }

      /**
       * R_FillBackScreen
       * Fills the back screen with a pattern for variable screen sizes
       * Also draws a beveled edge. This is actually stored in screen 1, 
       * and is only OCCASIONALLY written to screen 1 (the visible one)
       * by calling R_VideoErase.
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

          if (DM.gamemode == GameMode_t.commercial)
              name = name2;
          else
              name = name1;

          /* This is a flat we're reading here */
          src = (flat_t) (W.CacheLumpName(name, PU_CACHE,flat_t.class));
          dest = V.getScreen(1);
          int destPos = 0;

          /* This part actually draws the border itself, without bevels */
          
          for (y = 0; y < SCREENHEIGHT - SBARHEIGHT; y++) {
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
          V.DrawPatch(viewwindowx - 8, viewwindowy - 8, 1, (patch_t) W
                  .CachePatchName("BRDR_TL", PU_CACHE));

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
              viewwindowy = (SCREENHEIGHT - SBARHEIGHT - height) >> 1;

          // Preclaculate all row offsets.
          for (i = 0; i < height; i++)
              ylookup[i] = /* screens[0] + */(i + viewwindowy) * SCREENWIDTH;
      }
      

      
      
////////INIT STUFF /////////////////

      /**
       * R_InitTextureMapping
       * 
       * Not moved into the TextureManager because it's tighly
       * coupled to the visuals, rather than textures. Perhaps the
       * name is not the most appropriate.
       * 
       */
      public final void InitTextureMapping ()
      {
          int         i;
          int         x;
          int         t;
          int     focallength;
          
          // Use tangent table to generate viewangletox:
          //  viewangletox will give the next greatest x
          //  after the view angle.
          //
          // Calc focallength
          //  so FIELDOFVIEW angles covers SCREENWIDTH.
          focallength = FixedDiv (centerxfrac,
                      finetangent[QUARTERMARK+FIELDOFVIEW/2] );
          
          for (i=0 ; i<FINEANGLES/2 ; i++)
          {
          if (finetangent[i] > FRACUNIT*2)
              t = -1;
          else if (finetangent[i] < -FRACUNIT*2)
              t = viewwidth+1;
          else
          {
              t = FixedMul (finetangent[i], focallength);
              t = (centerxfrac - t+FRACUNIT-1)>>FRACBITS;

              if (t < -1)
              t = -1;
              else if (t>viewwidth+1)
              t = viewwidth+1;
          }
          viewangletox[i] = t;
          }
          
          // Scan viewangletox[] to generate xtoviewangle[]:
          //  xtoviewangle will give the smallest view angle
          //  that maps to x. 
          for (x=0;x<=viewwidth;x++)
          {
          i = 0;
          while (viewangletox[i]>x)
              i++;
          xtoviewangle[x] = addAngles((i<<ANGLETOFINESHIFT),-ANG90);
          }
          
          // Take out the fencepost cases from viewangletox.
          for (i=0 ; i<FINEANGLES/2 ; i++)
          {
          t = FixedMul (finetangent[i], focallength);
          t = centerx - t;
          
          if (viewangletox[i] == -1)
              viewangletox[i] = 0;
          else if (viewangletox[i] == viewwidth+1)
              viewangletox[i]  = viewwidth;
          }
          
          clipangle = xtoviewangle[0];
          // OPTIMIZE: assign constant for optimization.
          CLIPANGLE2=(2*clipangle)&BITS32;
      }

      //
      // R_InitLightTables
      // Only inits the zlight table,
      //  because the scalelight table changes with view size.
      //
      protected final static int DISTMAP    =   2;

      protected final void InitLightTables ()
      {
          int     i;
          int     j;
          int     level;
          int     startmap;   
          int     scale;
          
          // Calculate the light levels to use
          //  for each level / distance combination.
          for (i=0 ; i< LIGHTLEVELS ; i++)
          {
          startmap = ((LIGHTLEVELS-1-i)*2)*NUMCOLORMAPS/LIGHTLEVELS;
          for (j=0 ; j<MAXLIGHTZ ; j++)
          {
              scale = FixedDiv ((SCREENWIDTH/2*FRACUNIT), (j+1)<<LIGHTZSHIFT);
              scale >>= LIGHTSCALESHIFT;
              level = startmap - scale/DISTMAP;
              
              if (level < 0)
              level = 0;

              if (level >= NUMCOLORMAPS)
              level = NUMCOLORMAPS-1;

            //zlight[i][j] = colormaps + level*256;
              zlight[i][j] = colormaps[level];
          }
          }
      }
      
      protected static final int TSC= 12;        /* number of fixed point digits in filter percent */
      byte[] main_tranmap;
      
      // Maes: a "cleaner" way to construct a transparency map.
      // Essentially we average each color's RGB values vs each other
      // and then  remap the blend to the closest existing color.
      // E.g. for finding mix of colors 0 and 100, we avg them, and
      // find that e.g. their mix is closest to color 139.            
      // Then we create the colormap table by putting 139 at position 0,100.
      
      protected void R_InitTranMap(int progress)
      {
        int lump = W.CheckNumForName("TRANMAP");

        // If a tranlucency filter map lump is present, use it
/*
        if (lump != -1)  // Set a pointer to the translucency filter maps.
          main_tranmap = W.CacheLumpNumAsRawBytes(lump, Defines.PU_STATIC);   // killough 4/11/98
        else
          {   // Compose a default transparent filter map based on PLAYPAL.
            byte[] playpal = W.CacheLumpNameAsRawBytes("PLAYPAL", Defines.PU_STATIC);
            String fname;
            String DoomExeDir=System.getProperty("user.dir");
*/
            
            byte[] playpal = W.CacheLumpNameAsRawBytes("PLAYPAL", Defines.PU_STATIC);
            main_tranmap = new byte[256*256];  // killough 4/11/98
            Color[] basepal=new Color[256];
            Color[] mixedpal=new Color[256*256];
            float[] distmap=new float[256*256];
            main_tranmap=new byte[256*256];
            
            // Init array of base colors.
            for (int i=0;i<256;i++){
                basepal[i]=new Color(0x00FF&playpal[i*3],0x00FF&playpal[i*3+1],0x00FF&playpal[i*3+2]);                   
                }

            // Init array of mixed colors. These are true RGB.
            // The diagonal of this array will be the original colors.
            for (int i=0;i<256;i++){
                for (int j=0;j<256;j++){
                    mixedpal[i*256+j]=mixColors(basepal[i],basepal[j]);                   
                }
            }
            
            // Init distance map. Every original palette colour has a
            // certain distance from all the others. The diagonal is zero.
            // The interpretation that e.g. the mixture of color 2 and 8 will
            // have a RGB value, which is closest to euclidean distance to
            // e.g. original color 9. Therefore we should put "9" in there.
            // a distance of 0, so it shoul
            
            final float[] tmpdist=new float[256];
            
            for (int a=0;a<256;a++){
                for (int b=0;b<256;b++){
                    // We evaluate the mixture of a and b
                    // Construct distance table vs all of the ORIGINAL colors.
                    for (int k=0;k<256;k++){
                        tmpdist[k]=colorDistance(mixedpal[a*256+b],basepal[k]);
                        }
                    
                    main_tranmap[(a<<8)|b]=(byte) findMin(tmpdist);
                }
            }            
            
      }

    /** Mixes two RGB colors. Nuff said */

    protected final Color mixColors(Color a, Color b){
        int red,green,blue;
        red=(a.getRed()+b.getRed())/2;
        green=(a.getGreen()+b.getGreen())/2;
        blue=(a.getBlue()+b.getBlue())/2;
        
        return new Color(red,green,blue);        
        }
    
    /** Returns the euclidean distance of two RGB colors. Nuff said */
    
    protected final float colorDistance(Color a, Color b){
        return (float) Math.sqrt(Math.pow((a.getRed()-b.getRed()),2)+
            Math.pow((a.getGreen()-b.getGreen()),2)+
            Math.pow((a.getBlue()-b.getBlue()),2));
        }

    protected final int findMin(float[] a){
        int minindex=0;
        float min=Float.POSITIVE_INFINITY;
        
        for (int i=0;i<a.length;i++)
            if (a[i]<min){
                min=a[i];
                minindex=i;
            }
        
        return minindex;
        
    }
    
      
       /**
       * R_DrawMaskedColumn
       * Used for sprites and masked mid textures.
       * Masked means: partly transparent, i.e. stored
       *  in posts/runs of opaque pixels.
       *  
       *  NOTE: this version accepts raw bytes, in case you  know what you're doing.
       */

/*      protected final  void DrawMaskedColumn (byte[] column)
      {
          int     topscreen;
          int     bottomscreen;
          int basetexturemid; // fixed_t
          int topdelta;
          int length;
          
          basetexturemid = dc_texturemid;
          // That's true for the whole column.
          dc_source = column;
          int pointer=0;
          
          // for each post...
          while((topdelta=toUnsignedByte(column[pointer]))!=0xFF)
          {
          // calculate unclipped screen coordinates
          //  for post
          topscreen = sprtopscreen + spryscale*topdelta;
          length=toUnsignedByte(column[pointer+1]);
          bottomscreen = topscreen + spryscale*length;

          dc_yl = (topscreen+FRACUNIT-1)>>FRACBITS;
          dc_yh = (bottomscreen-1)>>FRACBITS;
              
          if (dc_yh >= mfloorclip[p_mfloorclip+dc_x])
              dc_yh = mfloorclip[p_mfloorclip+dc_x]-1;
          
          if (dc_yl <= mceilingclip[p_mceilingclip+dc_x])
              dc_yl = mceilingclip[p_mceilingclip+dc_x]+1;

          // killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
          if (dc_yl <= dc_yh && dc_yh < viewheight)
          {
              // Set pointer inside column to current post's data
              // Rremember, it goes {postlen}{postdelta}{pad}[data]{pad} 
              dc_source_ofs = pointer+3;
              dc_texturemid = basetexturemid - (topdelta<<FRACBITS);

              // Drawn by either R_DrawColumn
              //  or (SHADOW) R_DrawFuzzColumn.
              dc_texheight=0; // Killough
                  
              colfunc.invoke();
          }
          pointer+=length + 4;
          }
          
          dc_texturemid = basetexturemid;
      }*/
        
      /**
       * R_DrawMaskedColumn
       * Used for sprites and masked mid textures.
       * Masked means: partly transparent, i.e. stored
       *  in posts/runs of opaque pixels.
       *  
       *  FIXME: while it does work with "raw columns", if the initial
       *  post is drawn outside of the screen the rest appear screwed up.
       *  
       *  SOLUTION: use the version taking raw byte[] arguments.
       *  
       */

      protected final void DrawMaskedColumn (column_t column)
      {
          int     topscreen;
          int     bottomscreen;
          int basetexturemid; // fixed_t
          
          basetexturemid = dc_texturemid;
          // That's true for the whole column.
          dc_source = column.data;
          //dc_source_ofs=0;
          
          // for each post...
          for (int i=0;i<column.posts;i++ ) 
          {
              dc_source_ofs=column.postofs[i];
              // calculate unclipped screen coordinates
          //  for post
          topscreen = sprtopscreen + spryscale*column.postdeltas[i];
          bottomscreen = topscreen + spryscale*column.postlen[i];

          dc_yl = (topscreen+FRACUNIT-1)>>FRACBITS;
          dc_yh = (bottomscreen-1)>>FRACBITS;
              
          if (dc_yh >= mfloorclip[p_mfloorclip+dc_x])
              dc_yh = mfloorclip[p_mfloorclip+dc_x]-1;
          if (dc_yl <= mceilingclip[p_mceilingclip+dc_x])
              dc_yl = mceilingclip[p_mceilingclip+dc_x]+1;

          // killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
          if (dc_yl <= dc_yh && dc_yh < viewheight)
          {
              // Set pointer inside column to current post's data
              // Remember, it goes {postlen}{postdelta}{pad}[data]{pad} 
              
              dc_texturemid = basetexturemid - (column.postdeltas[i]<<FRACBITS);
              
              // Drawn by either R_DrawColumn
              //  or (SHADOW) R_DrawFuzzColumn.
              dc_source_ofs+=3; // This makes us point into the first valid pixel of a post. I hope.
              dc_texheight=0; // killough. Sprites and masked textures get no tiling safeguard, obviously.
              
               colfunc.invoke();
          }
          //column = (column_t *)(  (byte *)column + column.length + 4);
          
          }
          
          dc_texturemid = basetexturemid;
      }
      
      protected final class R_DrawTLColumn implements colfunc_t{
          
          public void invoke() 
          { 
            int              count; 
            int dest;            // killough
            int  frac;            // killough
            int fracstep;     
            
            count = dc_yh - dc_yl + 1; 

            if (count <= 0)    // Zero length, column does not exceed a pixel.
              return; 
                                           
          if (RANGECHECK) {
            if (dc_x >= SCREENWIDTH
                || dc_yl < 0
                || dc_yh >= SCREENHEIGHT) 
              I.Error ("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh, dc_x); 
          } 

            // Framebuffer destination address.
            // Use ylookup LUT to avoid multiply with ScreenWidth.
            // Use columnofs LUT for subwindows? 

            dest = ylookup[dc_yl] + columnofs[dc_x];  

            // Determine scaling, which is the only mapping to be done.

            fracstep = dc_iscale; 
            frac = dc_texturemid + (dc_yl-centery)*fracstep; 

            // Inner loop that does the actual texture mapping,
            //  e.g. a DDA-lile scaling.
            // This is as fast as it gets.       (Yeah, right!!! -- killough)
            //
            // killough 2/1/98: more performance tuning

            {
              final byte[] source = dc_source;  
               final byte[] colormap = dc_colormap; 
              int heightmask = dc_texheight-1;
              if ((dc_texheight & heightmask)!=0)   // not a power of 2 -- killough
                {
                  heightmask++;
                  heightmask <<= FRACBITS;
                    
                  if (frac < 0)
                    while ((frac += heightmask) <  0);
                  else
                    while (frac >= heightmask)
                      frac -= heightmask;
                    
                  do
                    {
                      // Re-map color indices from wall texture column
                      //  using a lighting/special effects LUT.
                      
                      // heightmask is the Tutti-Frutti fix -- killough
                      
                      screen[dest] = main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]])];
                      dest += SCREENWIDTH; 
                      if ((frac += fracstep) >= heightmask)
                        frac -= heightmask;
                    } 
                  while (--count>0);
                }
             else
                {
                  while ((count-=4)>=0)   // texture height is a power of 2 -- killough
                    {
                      //screen[dest] = main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]])];
                      screen[dest] = main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]])];
                      dest += SCREENWIDTH; 
                      frac += fracstep;
                      screen[dest] = main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]])];
                      dest += SCREENWIDTH; 
                      frac += fracstep;
                      screen[dest] = main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]])];
                      dest += SCREENWIDTH; 
                      frac += fracstep;
                      screen[dest] = main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]])];
                      dest += SCREENWIDTH; 
                      frac += fracstep;     
                    }
                  if ((count & 1)!=0)
                      screen[dest] = main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]])];
                } 
            }
          }
          }
   
      protected final class R_DrawTranslatedColumn implements colfunc_t{

          public void invoke() {
              int count;
              // MAES: you know the deal by now...
              int dest;
              int frac;
              int fracstep;

              count = dc_yh - dc_yl;
              if (count < 0)
                  return;

              if (RANGECHECK) {
                  if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT) {
                      I.Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh, dc_x);
                  }
              }

              // WATCOM VGA specific.
              /*
               * Keep for fixing. if (detailshift) { if (dc_x & 1) outp
               * (SC_INDEX+1,12); else outp (SC_INDEX+1,3); dest = destview + dc_yl*80
               * + (dc_x>>1); } else { outp (SC_INDEX+1,1<<(dc_x&3)); dest = destview
               * + dc_yl*80 + (dc_x>>2); }
               */

              // FIXME. As above.
              dest = ylookup[dc_yl] + columnofs[dc_x];

              // Looks familiar.
              fracstep = dc_iscale;
              frac = dc_texturemid + (dc_yl - centery) * fracstep;

              // Here we do an additional index re-mapping.
              do {
                  // Translation tables are used
                  // to map certain colorramps to other ones,
                  // used with PLAY sprites.
                  // Thus the "green" ramp of the player 0 sprite
                  // is mapped to gray, red, black/indigo.
                  screen[dest] =
                      dc_colormap[0x00FF&dc_translation[dc_source[dc_source_ofs+(frac >> FRACBITS)]]];
                  dest += SCREENWIDTH;

                  frac += fracstep;
              } while (count-- != 0);
          }

          }
      
      
      /**
       * A column is a vertical slice/span from a wall texture that, given the
       * DOOM style restrictions on the view orientation, will always have
       * constant z depth. Thus a special case loop for very fast rendering can be
       * used. It has also been used with Wolfenstein 3D. MAES: this is called
       * mostly from inside Draw and from an external "Renderer"
       */

      protected final class R_DrawColumn implements colfunc_t{
          public void invoke(){ 
          int count;
          // byte* dest;
          int dest; // As pointer
          // fixed_t
          int frac, fracstep;
          // Something gross happens.
          boolean gross=false;
          byte colmask=127;
          count = dc_yh - dc_yl;
          // How much we should draw
          //count = Math.min(dc_yh - dc_yl,dc_source.length-dc_source_ofs-1);
          //colmask = (byte) Math.min(dc_source.length-dc_source_ofs-1,127);

          // Zero length, column does not exceed a pixel.
          if (count <=0)
              return;

          if (RANGECHECK) {
              if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT)
                  I.Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
                              dc_x);
          }

          // Trying to draw a masked column? Then something gross will happen.
          /*if (count>=dc_source.length-dc_source_ofs) {
              int diff=count-(dc_source.length-dc_source_ofs);
              count=dc_source.length-dc_source_ofs-1;
              dc_source_ofs=0;
              //dc_yl=dc_yh-count;
              gross=true;
          }*/
          
          // Framebuffer destination address.
          // Use ylookup LUT to avoid multiply with ScreenWidth.
          // Use columnofs LUT for subwindows?
          dest = ylookup[dc_yl] + columnofs[dc_x];

          // Determine scaling,
          // which is the only mapping to be done.
          fracstep = dc_iscale;
          frac = dc_texturemid + (dc_yl - centery) * fracstep;
          
          // Inner loop that does the actual texture mapping,
          // e.g. a DDA-lile scaling.
          // This is as fast as it gets.
          do {
              /* Re-map color indices from wall texture column
               * using a lighting/special effects LUT.
               * TODO: determine WHERE the fuck "*dest" is supposed to be
               * pointing.
               * DONE: it's pointing inside screen[0] (implicitly).
               * dc_source was probably just a pointer to a decompressed
               *  column...right? Right.
               */  
             // if (gross) System.out.println(frac >> FRACBITS);
              screen[dest] = dc_colormap[0x00FF&dc_source[(dc_source_ofs+(frac >> FRACBITS)) & colmask]];

              
              /* MAES: ok, so we have (from inside out):
               * 
               * frac is a fixed-point number representing a pointer inside a column. It gets shifted to an integer,
               * and AND-ed with 128 (this causes vertical column tiling).
               * 
               * 
               */
              dest += SCREENWIDTH;
              frac += fracstep;

          } while (count-- > 0);
      }
      }
      
      /** EI VITTU, this gives a clean 25% boost. Da fack...
       * 
       * 
       * @author admin
       *
       */
      
      protected final class R_DrawColumnUnrolled implements colfunc_t{
          
          /* That's shit, doesn't help.
          private final int SCREENWIDTH2=SCREENWIDTH*2;
          private final int SCREENWIDTH3=SCREENWIDTH*3;
          private final int SCREENWIDTH4=SCREENWIDTH*4;
          private final int SCREENWIDTH5=SCREENWIDTH*5;
          private final int SCREENWIDTH6=SCREENWIDTH*6;
          private final int SCREENWIDTH7=SCREENWIDTH*7;
          private final int SCREENWIDTH8=SCREENWIDTH*8;
          */
          
          public void invoke(){ 
          int         count; 
          byte[]       source;
          int       dest;
          byte[]       colormap;
          
          // These are all "unsigned". Watch out for bit shifts!
          int        frac, fracstep, fracstep2,fracstep3, fracstep4;   
       
          count = dc_yh - dc_yl + 1; 

          source = dc_source;
          dc_source_ofs+=15;
          colormap = dc_colormap;      
          dest = ylookup[dc_yl] + columnofs[dc_x];  
           
          fracstep = dc_iscale<<9; 
          frac = (dc_texturemid + (dc_yl-centery)*dc_iscale)<<9; 
       
          fracstep2 = fracstep+fracstep;
          fracstep3 = fracstep2+fracstep;
          fracstep4 = fracstep3+fracstep;
          
          while (count >= 8) 
          { 
              screen[dest] = colormap[0x00FF&source[dc_source_ofs+frac>>>25]]; 
          screen[dest +SCREENWIDTH] = colormap[0x00FF&source[dc_source_ofs+(frac+fracstep)>>>25]]; 
          screen[dest + SCREENWIDTH*2] = colormap[0x00FF&source[dc_source_ofs+(frac+fracstep2)>>>25]]; 
          screen[dest + SCREENWIDTH*3] = colormap[0x00FF&source[dc_source_ofs+(frac+fracstep3)>>>25]];
          
          frac += fracstep4; 

          screen[dest + SCREENWIDTH*4] = colormap[0x00FF&source[dc_source_ofs+frac>>>25]]; 
          screen[dest + SCREENWIDTH*5] = colormap[0x00FF&source[dc_source_ofs+(frac+fracstep)>>>25]]; 
          screen[dest + SCREENWIDTH*6] = colormap[0x00FF&source[dc_source_ofs+(frac+fracstep2)>>>25]]; 
          screen[dest + SCREENWIDTH*7] = colormap[0x00FF&source[dc_source_ofs+(frac+fracstep3)>>>25]]; 

          frac += fracstep4; 
          dest += SCREENWIDTH*8; 
          count -= 8;
          } 
          
          while (count > 0)
          { 
              screen[dest] = colormap[0x00FF&source[dc_source_ofs+frac>>>25]]; 
          dest += SCREENWIDTH; 
          frac += fracstep; 
          count--;
          } 
      }
      }
      
      /** Adapted from Killough's Boom code.
       * 
       * 
       * @author admin
       *
       */
      
      protected final class R_DrawColumnBoom implements colfunc_t{
          
      public void invoke() 
      { 
        int              count; 
        int dest;            // killough
        int  frac;            // killough
        int fracstep;     
        
        count = dc_yh - dc_yl + 1; 

        if (count <= 0)    // Zero length, column does not exceed a pixel.
          return; 
                                       
      if (RANGECHECK) {
        if (dc_x >= SCREENWIDTH
            || dc_yl < 0
            || dc_yh >= SCREENHEIGHT) 
          I.Error ("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh, dc_x); 
      } 

        // Framebuffer destination address.
        // Use ylookup LUT to avoid multiply with ScreenWidth.
        // Use columnofs LUT for subwindows? 

        dest = ylookup[dc_yl] + columnofs[dc_x];  

        // Determine scaling, which is the only mapping to be done.

        fracstep = dc_iscale; 
        frac = dc_texturemid + (dc_yl-centery)*fracstep; 

        // Inner loop that does the actual texture mapping,
        //  e.g. a DDA-lile scaling.
        // This is as fast as it gets.       (Yeah, right!!! -- killough)
        //
        // killough 2/1/98: more performance tuning

        {
          final byte[] source = dc_source;       
           final byte[] colormap = dc_colormap; 
          int heightmask = dc_texheight-1;
          if ((dc_texheight & heightmask)!=0)   // not a power of 2 -- killough
            {
              heightmask++;
              heightmask <<= FRACBITS;
                
              if (frac < 0)
                while ((frac += heightmask) <  0);
              else
                while (frac >= heightmask)
                  frac -= heightmask;
                
              do
                {
                  // Re-map color indices from wall texture column
                  //  using a lighting/special effects LUT.
                  
                  // heightmask is the Tutti-Frutti fix -- killough
                  
                  screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS))]];
                  dest += SCREENWIDTH; 
                  if ((frac += fracstep) >= heightmask)
                    frac -= heightmask;
                } 
              while (--count>0);
            }
         else
            {
              while ((count-=4)>=0)   // texture height is a power of 2 -- killough
                {
                  screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
                  dest += SCREENWIDTH; 
                  frac += fracstep;
                  screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
                  dest += SCREENWIDTH; 
                  frac += fracstep;
                  screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
                  dest += SCREENWIDTH; 
                  frac += fracstep;
                  screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
                  dest += SCREENWIDTH; 
                  frac += fracstep;     
                }
              if ((count & 1)!=0)
                  screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
            } 
        }
      }
      }
      
      protected final class R_DrawColumnLow implements colfunc_t{
          public void invoke(){
          int count;
          // MAES: were pointers. Of course...
          int dest;
          int dest2;
          // Maes: fixed_t never used as such.
          int frac;
          int fracstep;

          count = dc_yh - dc_yl;

          // Zero length.
          if (count < 0)
              return;

          if (RANGECHECK) {
              if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT) {

                  I.Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
                              dc_x);
              }
              // dccount++;
          }
          // Blocky mode, need to multiply by 2.
          dc_x <<= 1;

          dest = ylookup[dc_yl] + columnofs[dc_x];
          dest2 = ylookup[dc_yl] + columnofs[dc_x + 1];

          fracstep = dc_iscale;
          frac = dc_texturemid + (dc_yl - centery) * fracstep;
          //int spot=(frac >>> FRACBITS) & 127;
          do {
              
              // Hack. Does not work correctly.
              // MAES: that's good to know.
              screen[dest2] =
                  screen[dest] =
                      dc_colormap[0x00FF&dc_source[dc_source_ofs+((frac >> FRACBITS) & 127)]];
              
             // System.out.println("Drawing "+(dest2%SCREENWIDTH)+" , "+(dest2/SCREENWIDTH));
              dest += SCREENWIDTH;
              dest2 += SCREENWIDTH;
              frac += fracstep;
          } while (count-- != 0);
      }
      }

    /**
     * Framebuffer postprocessing.
     * Creates a fuzzy image by copying pixels
     * from adjacent ones to left and right.
     * Used with an all black colormap, this
     * could create the SHADOW effect,
     * i.e. spectres and invisible players.
     */
      
      protected final class R_DrawFuzzColumn implements colfunc_t{
        public void invoke()
    { 
       int         count; 
       int       dest; 
       int     frac;
       int     fracstep;    

       // Adjust borders. Low... 
       if (dc_yl==0) 
       dc_yl = 1;

       // .. and high.
       if (dc_yh == viewheight-1) 
       dc_yh = viewheight - 2; 
            
       count = dc_yh - dc_yl; 

       // Zero length.
       if (count < 0) 
       return; 

       
    if(RANGECHECK){ 
       if (dc_x >= SCREENWIDTH
       || dc_yl < 0 || dc_yh >= SCREENHEIGHT)
       {
       I.Error ("R_DrawFuzzColumn: %i to %i at %i",
            dc_yl, dc_yh, dc_x);
       }
    }


       // Keep till detailshift bug in blocky mode fixed,
       //  or blocky mode removed.
       /* WATCOM code 
       if (detailshift)
       {
       if (dc_x & 1)
       {
           outpw (GC_INDEX,GC_READMAP+(2<<8) ); 
           outp (SC_INDEX+1,12); 
       }
       else
       {
           outpw (GC_INDEX,GC_READMAP); 
           outp (SC_INDEX+1,3); 
       }
       dest = destview + dc_yl*80 + (dc_x>>1); 
       }
       else
       {
       outpw (GC_INDEX,GC_READMAP+((dc_x&3)<<8) ); 
       outp (SC_INDEX+1,1<<(dc_x&3)); 
       dest = destview + dc_yl*80 + (dc_x>>2); 
       }*/

       
       // Does not work with blocky mode.
       dest = ylookup[dc_yl] + columnofs[dc_x];

       // Looks familiar.
       fracstep = dc_iscale; 
       frac = dc_texturemid + (dc_yl-centery)*fracstep; 

       // Looks like an attempt at dithering,
       //  using the colormap #6 (of 0-31, a bit
       //  brighter than average).
       do 
       {
       // Lookup framebuffer, and retrieve
       //  a pixel that is either one column
       //  left or right of the current one.
       // Add index from colormap to index.
       screen[dest] = colormaps[6][0x00FF&screen[dest+fuzzoffset[fuzzpos]]]; 

       // Clamp table lookup index.
       if (++fuzzpos == FUZZTABLE) 
           fuzzpos = 0;
       
       dest += SCREENWIDTH;

       frac += fracstep; 
       } while (count-->0); 
    } 
    }
    
    ////////////////////////////////// TEXTURE MANAGEMENT /////////////////////////
      
      /**
       * R_GetColumn
       * @throws IOException 
       */
      public byte[] GetColumn
      ( int       tex,
        int       col ) 
      {
          int     lump,ofs;
          
          col &= TexMan.getTexturewidthmask(tex);
          lump = TexMan.getTextureColumnLump(tex, col);
          ofs=TexMan.getTextureColumnOfs(tex, col);
          
          // If pointing inside a non-zero, positive lump, then it's not a composite texture.
          // Read from disk.
          if (lump > 0){
              // This will actually return a pointer to a patch's columns.
              // That is, to the ONE column exactly.{
              // If the caller needs access to a raw column, we must point 3 bytes "ahead".
              dc_source_ofs=3;
              patch_t r=W.CachePatchNum(lump,PU_CACHE);
          return r.columns[ofs].data;
      }
          // Texture should be composite, but it doesn't yet exist. Create it. 
          if (TexMan.getTextureComposite(tex)==null) TexMan.GenerateComposite (tex);

          // This implies that texturecomposite actually stores raw, compressed columns,
          // or else those "ofs" would go in-between.
          // The source offset int this case is 0, else we'll skip over stuff.
          this.dc_source_ofs=0;
          return TexMan.getTextureComposite(tex,col);
      }

      /**
       * R_GetColumn variation: returns a pointer to a column_t
       * rather than raw data.
       * 
       * 
       * @throws IOException 
       * 
       * 
       */
      public column_t GetActualColumn
      ( int       tex,
        int       col ) 
      {
          int     lump,ofs;
          
          col &= TexMan.getTexturewidthmask(tex);
          lump = TexMan.getTextureColumnLump(tex, col);
          ofs=TexMan.getTextureColumnOfs(tex, col);
          
          // If pointing inside a non-zero, positive lump, then it's not a composite texture.
          // Read from disk.
          if (lump > 0){
              // This will actually return a pointer to a patch's columns.
              // That is, to the ONE column exactly.{
              // If the caller needs access to a raw column, we must point 3 bytes "ahead".
              dc_source_ofs=3;
              patch_t r=W.CachePatchNum(lump,PU_CACHE);
          return r.columns[ofs];
      }
          // Texture should be composite, but it doesn't yet exist. Create it. 
          if (TexMan.getTextureComposite(tex)==null) TexMan.GenerateComposite (tex);

          // This implies that texturecomposite actually stores raw, compressed columns,
          // or else those "ofs" would go in-between.
          // The source offset int this case is 0, else we'll skip over stuff.
          this.dc_source_ofs=0;
          shitty.data=TexMan.getTextureComposite(tex,col);
          
          // Return a default column with the data thrown in. Literally.
          return shitty;
      }
      
      protected column_t shitty;
      
      /**
       * R_InitSpriteLumps
       * Finds the width and hoffset of all sprites in the wad,
       *  so the sprite does not need to be cached completely
       *  just for having the header info ready during rendering.
       */
      
      protected void InitSpriteLumps () throws IOException
      {
          int     i;
          patch_t patch;
          
          firstspritelump = W.GetNumForName ("S_START") + 1;
          lastspritelump = W.GetNumForName ("S_END") - 1;
          
          numspritelumps = lastspritelump - firstspritelump + 1;
          spritewidth = new int[numspritelumps];
          spriteoffset = new int[numspritelumps];
          spritetopoffset = new int[numspritelumps];
          
          for (i=0 ; i< numspritelumps ; i++)
          {
          if ((i&63)==0)
              System.out.print (".");

          patch = (patch_t)W.CacheLumpNum (firstspritelump+i, PU_CACHE,patch_t.class);
          spritewidth[i] = patch.width<<FRACBITS;
          spriteoffset[i] = patch.leftoffset<<FRACBITS;
          spritetopoffset[i] = patch.topoffset<<FRACBITS;
          }
      }


      /**
      * R_InitColormaps
     * @throws IOException 
      */
      protected void InitColormaps () throws IOException
      {
          int lump, length;
          
          // Load in the light tables, 
          //  256 byte align tables.
          lump = W.GetNumForName("COLORMAP"); 
          length = W.LumpLength (lump) + 256;
          colormaps = new byte[(length/256)][256];
          byte[] tmp=new byte[length];
          System.out.println("Colomaps: "+colormaps.length);
          ByteBuffer b=ByteBuffer.wrap(tmp);
          W.ReadLump (lump,b);
          
          for (int i=0;i<colormaps.length;i++){
              System.arraycopy(tmp, i*256, colormaps[i], 0, 256);
          }
         // colormaps = (byte *)( ((int)colormaps + 255)&~0xff); 
           
      }



      /**
       * R_InitData
       * Locates all the lumps
       *  that will be used by all views
       * Must be called after W_Init.
       */
      
      public void InitData ()
      {
          try {
          System.out.print ("\nInit Texture and Flat Manager");
          TexMan=this.DM.TM;
          
            TexMan.InitTextures ();
          System.out.print ("\nInitTextures");
          TexMan.InitFlats ();
          System.out.print ("\nInitFlats");
          InitSpriteLumps ();
          System.out.print ("\nInitSprites");
          InitColormaps ();
          System.out.print ("\nInitColormaps");
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }

      }
      ////////////////////////// FLAT AND TEXTURE MANAGEMENT //////////////////////////
      

      

         
         
         
         /* UNUSED: older, sequential search method.
          
           public int CheckTextureNumForName(String name) {
             int i;
             // "NoTexture" marker.
             if (name.charAt(0) == '-')  return 0;
             

              for (i = 0; i < numtextures; i++)
                 if (textures[i].name.compareToIgnoreCase(name) == 0)
                     return i;

             return -1;
         } */

       protected  int     spritememory;
        
       
      
       

      
      /**
       * R_InitSprites
       * Called at program start.
       *
       */
      
      public void InitSprites (String[] namelist)
      {
          int     i;
          
          for (i=0 ; i<SCREENWIDTH ; i++)
          {
          negonearray[i] = -1;
          }
          
          MyThings.InitSpriteDefs (namelist);
      }

      /** To be called right after PrecacheLevel.
       *  It's an ugly hack, in that it must communicate
       *  with the "Game map" class and determine what kinds
       *  of monsters are actually in the level and whether it should load
       *  their graphics or not.
       *  
       *  Whenever we implement it, it's going to be ugly and
       *  not neatly separated anyway.  
       * 
       * @return
       */
      public void PreCacheThinkers(){
          
          boolean []     spritepresent;
          thinker_t      th;
          spriteframe_t  sf;
          int         i,j,k;
          int lump;

          
          spritepresent = new boolean[numsprites];
       
       
       for (th = P.getThinkerCap().next ; th != P.getThinkerCap() ; th=th.next)
       {
       if (th.function==think_t.P_MobjThinker)
           spritepresent[((mobj_t )th).sprite.ordinal()] = true;
       }
       
       spritememory = 0;
       for (i=0 ; i<numsprites ; i++)
       {
       if (!spritepresent[i])
           continue;

       for (j=0 ; j<getSprites()[i].numframes ; j++)
       {
           sf = getSprites()[i].spriteframes[j];
           for (k=0 ; k<8 ; k++)
           {
           lump = firstspritelump + sf.lump[k];
           spritememory += W.lumpinfo[lump].size;
           W.CacheLumpNum(lump , PU_CACHE,patch_t.class);
           }
       }
       }
      }


      /**
       * R_InitTranslationTables
       * Creates the translation tables to map
       * the green color ramp to gray, brown, red.
       * Assumes a given structure of the PLAYPAL.
       * Could be read from a lump instead.
       */
        
      public void InitTranslationTables() {
          int i;

          // translationtables = Z_Malloc (256*3+255, PU_STATIC, 0);
          // translationtables = (byte *)(( (int)translationtables + 255 )& ~255);
          translationtables = new byte[256 * 3 + 255];

          // translate just the 16 green colors
          for (i = 0; i < 256; i++) {
              if (i >= 0x70 && i <= 0x7f) {
                  // map green ramp to gray, brown, red
                  translationtables[i] = (byte) (0x60 + (i & 0xf));
                  translationtables[i + 256] = (byte) (0x40 + (i & 0xf));
                  translationtables[i + 512] = (byte) (0x20 + (i & 0xf));
              } else {
                  // Keep all other colors as is.
                  translationtables[i] =
                      translationtables[i + 256] =
                          translationtables[i + 512] = (byte) i;
              }
          }
      }

      /////////////////// Generic rendering methods /////////////////////
      
      
      

      /**
         * R_PointInSubsector
         * 
         * @param x fixed
         * @param y fixed
         * 
         */
        public subsector_t
        PointInSubsector
        ( int   x,
          int   y )
        {
            node_t  node;
            int     side;
            int     nodenum;

            // single subsector is a special case
            if (LL.numnodes==0)              
            return LL.subsectors[0];
                
            nodenum = LL.numnodes-1;

            while ((nodenum & NF_SUBSECTOR)==0 )
            {
            node = LL.nodes[nodenum];
            side = node.PointOnSide (x, y);
            nodenum = node.children[side];
            }
            
            return LL.subsectors[nodenum & ~NF_SUBSECTOR];
        }
        
    ///// DRAWSPAN ///////////
        @Override
        public int getFirstSpriteLump(){
            return this.firstspritelump;        
        }
        
    @Override
    public spritedef_t[] getSprites() {
        return sprites;
    }

        protected int ds_y;

        protected int ds_x1;

        protected int ds_x2;

        /** DrawSpan colormap. */
        protected  byte[] ds_colormap;
        /* pointer into colormap
        int pds_colormap; */

        /** fixed_t */
        protected  int ds_xfrac;

        /** fixed_t */
        protected  int ds_yfrac;

        /** fixed_t */
        protected  int ds_xstep;

        /** fixed_t */
        protected int ds_ystep;

        /** start of a 64*64 tile image (treat as pointer inside ds_source) */
        protected  int pds_source;

        protected  byte[] ds_source;

        /** just for profiling */
        protected  int dscount;
        

       /**
        * Draws the actual span.
        * 
        * ds_frac, ds_yfrac, ds_x2, ds_x1, ds_xstep and ds_ystep must be set.
        * 
        */
       
        protected final class R_DrawSpan implements colfunc_t {
           

           
           public void invoke(){

               
           int f_xfrac; // fixed_t
           int f_yfrac; // fixed_t
           int dest;
           int count;
           int spot;
           
           //System.out.println("R_DrawSpan: "+ds_x1+" to "+ds_x2+" at "+ ds_y);
               
           if (RANGECHECK) {
               if (ds_x2 < ds_x1 || ds_x1 < 0 || ds_x2 >= SCREENWIDTH
                       || ds_y > SCREENHEIGHT) {
                   I.Error("R_DrawSpan: %i to %i at %i", ds_x1, ds_x2, ds_y);
               }
               // dscount++;
           }

           f_xfrac = ds_xfrac;
           f_yfrac = ds_yfrac;

           dest = ylookup[ds_y] + columnofs[ds_x1];

           // We do not check for zero spans here?
           count = ds_x2 - ds_x1;

           do {
               // Current texture index in u,v.
               spot = ((f_yfrac >> (16 - 6)) & (63 * 64)) + ((f_xfrac >> 16) & 63);

               // Lookup pixel from flat texture tile,
               // re-index using light/colormap.
               screen[dest++] = ds_colormap[0x00FF&ds_source[spot]];

               // Next step in u,v.
               f_xfrac += ds_xstep;
               f_yfrac += ds_ystep;

           } while (count-- > 0);
           }
       }
       
        protected final class R_DrawSpanLow implements colfunc_t{

          @Override
          public void invoke() {
              int f_xfrac;
              int f_yfrac;
              int dest;
              int count;
              int spot;

              if (RANGECHECK) {
                  if ((ds_x2 < ds_x1) || (ds_x1 < 0) || ds_x2 >= SCREENWIDTH
                          || ds_y > SCREENHEIGHT) {
                      I.Error("R_DrawSpan: %i to %i at %i", ds_x1, ds_x2, ds_y);
                  }
                  // dscount++;
              }

              f_xfrac = ds_xfrac;
              f_yfrac = ds_yfrac;

              // Blocky mode, need to multiply by 2.
              //ds_x1 <<= 1;
              //ds_x2 <<= 1;

              dest = ylookup[ds_y] + columnofs[ds_x1];

              count = ds_x2 - ds_x1;
              do {
                  spot = ((f_yfrac >> (16 - 6)) & (63 * 64)) + ((f_xfrac >> 16) & 63);
                  // Lowres/blocky mode does it twice,
                  // while scale is adjusted appropriately.

                  screen[dest++] = ds_colormap[0x00FF&ds_source[spot]];
                  screen[dest++] = ds_colormap[0x00FF&ds_source[spot]];
                  
                  f_xfrac += ds_xstep;
                  f_yfrac += ds_ystep;

              } while (count-- >0);
              
          }
           
       }
       
       
       /** Drawspan loop unrolled by 4.
        *  
        *  MAES: it actually does give a small speed boost (120 -> 130 fps with a Mul of 3.0)
        * 
        */
       
        protected  final class R_DrawSpanUnrolled implements colfunc_t {
           
           public void invoke(){
           int position, step;
           byte[] source;
           byte[] colormap;
           int dest;
           int count;
           int spot;
           int xtemp;
           int ytemp;
           
           position = ((ds_xfrac << 10) & 0xffff0000) | ((ds_yfrac >> 6) & 0xffff);
           step = ((ds_xstep << 10) & 0xffff0000) | ((ds_ystep >> 6) & 0xffff);
           source = ds_source;
           colormap = ds_colormap;
           dest = ylookup[ds_y] + columnofs[ds_x1];
           count = ds_x2 - ds_x1 + 1;
           while (count >= 4) {
               ytemp = position >> 4;
               ytemp = ytemp & 4032;
               xtemp = position >>> 26;
               spot = xtemp | ytemp;
               position += step;         
               screen[dest] = colormap[0x00FF&source[spot]];
               ytemp = position >> 4;
               ytemp = ytemp & 4032;
               xtemp = position >>> 26;
               spot = xtemp | ytemp;
               position += step;
               screen[dest+1] = colormap[0x00FF&source[spot]];
               ytemp = position >> 4;
               ytemp = ytemp & 4032;
               xtemp = position >>> 26;
               spot = xtemp | ytemp;
               position += step;
               screen[dest+2] = colormap[0x00FF&source[spot]];
               ytemp = position >> 4;
               ytemp = ytemp & 4032;
               xtemp = position >>> 26;
               spot = xtemp | ytemp;
               position += step;
               screen[dest+3] = colormap[0x00FF&source[spot]];
               count -= 4;
               dest += 4;
           }
           
           while (count > 0) {
               ytemp = position >> 4;
               ytemp = ytemp & 4032;
               xtemp = position >>> 26;
               spot = xtemp | ytemp;
               position += step;
               screen[dest++] = colormap[0x00FF&source[spot]];
               count--;
           }
       }
           
       }
       
      
}
