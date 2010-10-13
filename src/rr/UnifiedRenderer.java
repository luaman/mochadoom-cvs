package rr;

import static utils.C2JUtils.toUnsignedByte;
import static data.Defines.*;
import static data.Limits.*;
import static doom.player_t.*;

import static p.mobj_t.*;
import static data.SineCosine.finecosine;
import static data.SineCosine.finesine;
import static data.Tables.*;
import static m.fixed_t.*;
import static m.BBox.*;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import m.fixed_t;

import i.DoomSystemInterface;
import p.LevelLoader;
import p.UnifiedGameMap;
import p.mobj_t;
import p.pspdef_t;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import w.DoomBuffer;
import w.WadLoader;
import w.name8;
import data.Defines.GameMode_t;
import data.Tables;
import doom.DoomMain;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;

public class UnifiedRenderer extends RendererState{
    
    private static final boolean DEBUG=false;
    
    public UnifiedRenderer(DoomMain DM) {
      this.DM=DM;
      this.LL=DM.LL;
      this.W=DM.W;
      this.MySegs=new Segs();
      this.MyBSP=new BSP();
      this.MyPlanes=new Planes();
      this.MyThings=new Things();
      // We must also connect screen to V. Don't forget it. Do it in Init(), OK?      
      this.V=DM.V;
      this.I=DM.I;
      // Span functions
      DrawSpan=new R_DrawSpan();
      DrawSpanLow=new R_DrawSpanLow();
     
  }

    
    
    public Things getThings(){
        return this.MyThings;
    }
    
    ///////////////// COLORMAPS ///////////////////////////////
    
    /** use paired with dcto */
    byte[] dc_translation;
    /** DC Translation offset */
    int dcto;

    /** used paired with tto */
    byte[] translationtables;
    /** translation tables offset */
    int tto;
    
    
    ///////////////// COMMON RENDERING GLOBALS ////////////////
    
    /* ON COLORMAPS: they are supposed to be "from this color to some other color" mappings.
     * "Color" means an index in the palette, like those used in screen. Their contents
     * should be bytes, although their indexing has to be "unsigned bytes" the very least.
     * 
     * Lengths: for some reas
     * 
     */
    
    
    /** "peg" this to the one from RendererData */
    byte[][] colormaps;
    
    //// FROM SEGS ////
    /** angle_t */
    long     rw_normalangle;
    
 // OPTIMIZE: closed two sided lines as single sided

    /** True if any of the segs textures might be visible. */
    boolean     segtextured;    

    /** False if the back side is the same plane. */
    boolean     markfloor, markceiling;

    boolean     maskedtexture;
    int     toptexture;
    int     bottomtexture;
    int     midtexture;

    /** angle to line origin */
    long     rw_angle1;  

    //
    // regular wall
    //
    int     rw_x;
    int     rw_stopx;
    long     rw_centerangle; // angle_t
    /** fixed_t */
    int     rw_offset,rw_distance,rw_scale,
    rw_scalestep,rw_midtexturemid,rw_toptexturemid,rw_bottomtexturemid;

    int     worldtop;
    int     worldbottom;
    int     worldhigh;
    int     worldlow;

    /** fixed_t */
    int     pixhigh,pixlow,pixhighstep,pixlowstep,
    topfrac,    topstep,bottomfrac, bottomstep;

    /** lighttable_t** */
    byte[][]   walllights;

    short[]     maskedtexturecol;
    
    ///// FROM PLANES //////
    
    /**
     * Clip values are the solid pixel bounding the range.
     *  floorclip starts out SCREENHEIGHT
     *  ceilingclip starts out -1
     */
    short[]         floorclip=new short[SCREENWIDTH],   ceilingclip=new short[SCREENWIDTH];
    
    /** visplane_t*,  treat as indexes into visplanes */
    public int       lastvisplane, floorplane,   ceilingplane;
    

    protected visplane_t[]      visplanes=new visplane_t[MAXVISPLANES];

    short[]         openings=new short[MAXOPENINGS];
    /** Maes: this is supposed to be a pointer inside openings */
    int           lastopening;//=new Short((short) 0);

    
    ///// FROM BSP /////////
    
    /** pointer to drawsegs */
    public int   ds_p;
    
    public drawseg_t[]    drawsegs;

    /** The sectors of the line currently being considered */
    public sector_t    frontsector,backsector;

    public seg_t       curline;
    public side_t      sidedef;
    public line_t      linedef;
    
    
    ///// FROM R_DATA, R_MAIN ////
    
    ///// FROM R_DRAW //////////
    
    /** OK< this is supposed to "peg" into screen buffer 0. It will work AS LONG AS SOMEONE FUCKING ACTUALLY SETS IT !!!! */
    byte[] screen;

    // status bar height at bottom of screen
    public static final int SBARHEIGHT = 32;

    private static final boolean RANGECHECK = false;


    byte[] viewimage;

     /** These are actually offsets inside screen 0 (or any screen). Therefore anything using
     * them should "draw" inside screen 0 */
    int[] ylookup = new int[MAXHEIGHT];

    /** Columns offset to set where?! */
    int[] columnofs = new int[MAXWIDTH];

    /** Color tables for different players,
       translate a limited part to another
       (color ramps used for suit colors).
       */
    
    byte[][] translations = new byte[3][256];
    
    /** MAES: this was a typedef for unsigned bytes, called "lighttable_t". It makes more sense
     * to make it primitive, since it's performance-critical in the renderer. Now, whether this should be made
     * bytes or shorts or chars is debatable.
     */
    byte[] dc_colormap;
    
    /** Offset. use as dc_colormap[dco+<shit>]. Also, when you set dc_colormap = crap[index],
     *  set dc_colormap=crap and  dco=index */
    int dco;

    int dc_x;

    int dc_yl;

    int dc_yh;

    /** fixed_t */
    int dc_iscale;

    /** fixed_t */
    int dc_texturemid;

    /** first pixel in a column (possibly virtual). Set dc_source_ofs to simulate pointer aliasing */
    byte[] dc_source;
    /** when passing dc_source around, also set this */ 
    int dc_source_ofs;
    
    // byte[] dc_data;

    // just for profiling
    int dccount;
    
    //
    // Lighting LUT.
    // Used for z-depth cuing per column/row,
    //  and other lighting effects (sector ambient, flash).
    //

    // Lighting constants.
    // Now why not 32 levels here?
    public static final int LIGHTLEVELS=16;
    public static final int LIGHTSEGSHIFT=4;

    public static final int MAXLIGHTSCALE=       48;
    public static final int LIGHTSCALESHIFT =    12;
    public static final int MAXLIGHTZ      =    128;
    public static final int LIGHTZSHIFT    = 20;

   /** Fineangles in the SCREENWIDTH wide window. */
   public static final int FIELDOFVIEW   =   2048;   
   
   /** killough: viewangleoffset is a legacy from the pre-v1.2 days, when Doom
    *  had Left/Mid/Right viewing. +/-ANG90 offsets were placed here on each
    *  node, by d_net.c, to set up a L/M/R session. */
   public static final int viewangleoffset=0;

   /** Increment every time a check is made 
    *  For some reason, this needs to be visible even by enemies thinking :-S*/
   public int validcount = 1;     

   /** Use in conjunction with pfixedcolormap */
   byte[]      fixedcolormap;
   /** Use in conjunction with fixedcolormap[] */
   int pfixedcolormap;
   //lighttable_t[][]  walllights;

   public int          centerx;
   public int          centery;

   /** Used to determind the view center and projection in view units fixed_t */
   public int centerxfrac,centeryfrac, projection;

   /** just for profiling purposes */
   public  int         framecount; 

   public  int         sscount;
   public  int         linecount;
   public  int         loopcount;

   /** fixed_t */
   public  int         viewx, viewy,viewz;

   //MAES: an exception to strict type safety. These are used only in here, anyway (?) and have no special functions.
   //Plus I must use them as indexes. angle_t
   public long  viewangle;

   /** fixed */
   public int          viewcos,viewsin;

   public player_t     viewplayer;

   // 0 = high, 1 = low. Normally only the menu and the interface can change that.
   public int          detailshift;    

   //
   // precalculated math tables
   //
   public  long           clipangle;

   // The viewangletox[viewangle + FINEANGLES/4] lookup
   // maps the visible view angles to screen X coordinates,
   // flattening the arc to a flat projection plane.
   // There will be many angles mapped to the same X. 

   public final int[]         viewangletox=new int[FINEANGLES/2];

   /** The xtoviewangleangle[] table maps a screen pixel
    * to the lowest viewangle that maps back to x ranges
    * from clipangle to -clipangle. */
   public final long[]         xtoviewangle=new long[SCREENWIDTH+1];


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
   
   
   public byte[][][]     scalelight=new byte[LIGHTLEVELS][MAXLIGHTSCALE][];
   public byte[][]       scalelightfixed=new byte[MAXLIGHTSCALE][];
   public byte[][][]     zlight=new byte[LIGHTLEVELS][MAXLIGHTZ][];

   // bumped light from gun blasts
   public static int           extralight;         

   /* MAES: Uhm...

   void (*colfunc) (void);
   void (*basecolfunc) (void);
   void (*fuzzcolfunc) (void);
   void (*transcolfunc) (void);
   void (*spanfunc) (void);
   */

   // Fuck that shit. Amma gonna do it the fastest way possible.
   
   
   protected colfunc_t colfunc;
   protected colfunc_t basecolfunc;
   protected colfunc_t fuzzcolfunc;
   protected colfunc_t transcolfunc;
   protected colfunc_t spanfunc;

   protected colfunc_t DrawTranslatedColumn=new R_DrawTranslatedColumn();
   protected colfunc_t DrawFuzzColumn=new R_DrawFuzzColumn();
   protected colfunc_t DrawColumnLow=new R_DrawColumnLow();
   protected colfunc_t DrawColumn=new R_DrawColumn();
   /** to be set in UnifiedRenderer */
   protected colfunc_t DrawSpan,DrawSpanLow;
   
   class R_DrawTranslatedColumn implements colfunc_t{

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

   class R_DrawColumn implements colfunc_t{
       public void invoke(){ 
       int count;
       // byte* dest;
       int dest; // As pointer
       // fixed_t
       int frac, fracstep;
       
       // How much we should draw
       count = dc_yh - dc_yl;

       // Zero length, column does not exceed a pixel.
       if (count < 0)
           return;

       if (RANGECHECK) {
           if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT)
               I.Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
                           dc_x);
       }

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
           if (DEBUG) System.out.println((frac >> FRACBITS)&127);
           screen[dest] = dc_colormap[0x00FF&dc_source[dc_source_ofs+((frac >> FRACBITS) & 127)]];

           
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
   
   
   class R_DrawColumnLow implements colfunc_t{
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
   
 class R_DrawFuzzColumn implements colfunc_t{
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
 
 
   
   
   // MAES: More renderer fields from segs.

   //OPTIMIZE: closed two sided lines as single sided

   // MAES: Shit taken from things


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
   int     pspritescale,pspriteiscale;

   lighttable_t[][]    spritelights;
    
   /** constant arrays
    *  used for psprite clipping and initializing clipping 
    */
   short[]     negonearray=new short[SCREENWIDTH];
   short[]     screenheightarray=new short[SCREENWIDTH];

   int     spryscale;
   int     sprtopscreen;
   short[]      mfloorclip;
   short[]      mceilingclip;
   
   //
   // INITIALIZATION FUNCTIONS
   //

   /** variables used to look up
     *    and range check thing_t sprites patches
     */
   public spritedef_t[]   sprites;
   int     numsprites;

   /* variables used to look up
   * and range check thing_t sprites patches
   */

   /** Temporarily contains the frames of a given sprite before they are registered
    *  with the rendering system. Apparently, a maximum of 29 frames per
    *  sprites is allowed.
    */
   spriteframe_t[] sprtemp=new spriteframe_t[29];
   int     maxframe;
   String      spritename;
   
   //
   // Spectre/Invisibility.
   //
   public static final int FUZZTABLE = 50;

   public static final int FUZZOFF = SCREENWIDTH;

   int[] fuzzoffset =
       { FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
               FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF,
               -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF,
               -FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
               FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF,
               FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
               -FUZZOFF, -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
               FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF };

   int fuzzpos = 0;
    

///// DRAWSPAN ///////////
   
   int ds_y;

   int ds_x1;

   int ds_x2;

   /** DrawSpan colormap. */
   byte[] ds_colormap;
   /* pointer into colormap
   int pds_colormap; */

   /** fixed_t */
   int ds_xfrac;

   /** fixed_t */
   int ds_yfrac;

   /** fixed_t */
   int ds_xstep;

   /** fixed_t */
   int ds_ystep;

   /** start of a 64*64 tile image (treat as pointer inside ds_source) */
   int pds_source;

   byte[] ds_source;

   /** just for profiling */
   int dscount;

   
   ////////////// SOME UTILITY METHODS /////////////

   public long
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
   
   public long
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



   public int
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
   public void InitPointToAngle ()
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
   }
   
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

   public long
   PointToAngle
   ( int   xx,
     int   yy )
   {   
       int x=xx- viewx;
       int y=yy- viewy;
       
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
           return (ANG90-1-tantoangle[ SlopeDiv(x,y)])&BITS32;
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
           return (ANG270+tantoangle[ SlopeDiv(x,y)])&BITS32;
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
           return (ANG180-1-tantoangle[ SlopeDiv(y,x)])&BITS32;
           }
           else
           {
           // octant 2
           return (ANG90+ tantoangle[ SlopeDiv(x,y)])&BITS32;
           }
       }
       else
       {
           // y<0
           y = -y;

           if (x>y)
           {
           // octant 4
           return (ANG180+tantoangle[ SlopeDiv(y,x)])&BITS32;
           }
           else
           {
            // octant 5
           return (ANG270-1-tantoangle[ SlopeDiv(x,y)])&BITS32;
           }
       }
       }
       // FIXME: Unreachable?!
       //return 0;
   }
   
   //
   // R_ScaleFromGlobalAngle
   // Returns the texture mapping scale
   //  for the current line (horizontal span)
   //  at the given angle.
   // rw_distance must be calculated first.
   //
   public int ScaleFromGlobalAngle (long visangle)
   {
       int         scale;
       int         anglea;
       int         angleb;
       int         sinea;
       int         sineb;
       int         num;
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

       anglea = (int) (ANG90 + (visangle-viewangle));
       angleb = (int) (ANG90 + (visangle-rw_normalangle));

       // both sines are allways positive
       sinea = finesine[anglea>>>ANGLETOFINESHIFT]; 
       sineb = finesine[angleb>>>ANGLETOFINESHIFT];
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
   public void InitTables ()
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
   
   public int
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
       
       if (dy>dx)
       {
       temp = dx;
       dx = dy;
       dy = temp;
       }
       
       angle = (tantoangle[ FixedDiv(dy,dx)>>DBITS ]+(int)ANG90) >>> ANGLETOFINESHIFT;

       // use as cosine
       dist = FixedDiv (dx, finesine[angle] ); 
       
       return dist;
   }

    
  class BSP{
      public int      rw_x;
      public int      rw_stopx;

      public boolean      segtextured;

      /** false if the back side is the same plane */
      public boolean      markfloor;      
      public boolean      markceiling;

      public boolean      skymap;

      /** light tables */
      public short[][]   hscalelight,vscalelight,dscalelight;
      
      /** newend is one past the last valid seg (cliprange_t) */
      int newend;
      cliprange_t[]   solidsegs;

      public BSP(){
          solidsegs= new cliprange_t[MAXSEGS+1];
          C2JUtils.initArrayOfObjects(solidsegs);
          
      }
      

  //
  // R_ClearDrawSegs
  //
  public void ClearDrawSegs ()
  {
      ds_p = 0;
  }
  //
  // ClipWallSegment
  // Clips the given range of columns
  // and includes it in the new clip list.
  //




  /**
   * R_ClipSolidWallSegment
   * Does handle solid walls,
   *  e.g. single sided LineDefs (middle texture)
   *  that entirely block the view.
   *  
   *  Handles "clipranges" for a solid wall, aka
   *  where it blocks the view.
   *  
   * @param first starting y coord?  
   * @param last ending y coord?
   */ 

  public void ClipSolidWallSegment (int   first,
          int   last ) {

      int next;
      int start;
     // int maxlast=Integer.MIN_VALUE;
      
      start = 0; // within solidsegs

      // Find the first cliprange that touches the range.
      // Actually, the first one not completely hiding it (its last must be lower than first.

      while (solidsegs[start].last < first-1)
      start++;
      
      // The post begins above the last found cliprange...
      if (first < solidsegs[start].first)
      {
      // ..and ends before it:
      if (last < solidsegs[start].first-1)
      {
          // Post is entirely visible (above start),
          //  so insert a new clippost.
          MySegs.StoreWallRange (first, last);
          next = newend;
          newend++;
          
          while (next != start)
          {
           // *next=*(next-1);
          //  FIXME: MAES: I think this is supposed to copy the structs
          //  solidsegs[next] = solidsegs[next-1].clone();
              solidsegs[next].copy(solidsegs[next-1]);
              
          next--;
          }
          solidsegs[next].first = first;
          solidsegs[next].last = last;
          return;
      }
          
      // There is a fragment above *start.
      MySegs.StoreWallRange (first, solidsegs[start].first - 1);
      // Now adjust the clip size.
      solidsegs[start].first = first; 
      }

      // Bottom contained in start?
      if (last <= solidsegs[start].last)
      return;         
          
      next = start;
      while (last >= solidsegs[(next+1)].first-1)
      {
      // There is a fragment between two posts.
      MySegs.StoreWallRange (solidsegs[next].last + 1, solidsegs[next+1].first - 1);
      next++;
      
      if (last <= solidsegs[next].last)
      {
          // Bottom is contained in next.
          // Adjust the clip size.
          solidsegs[start].last = solidsegs[next].last; 
          //goto crunch;
          
          { // crunch code
              if (next == start)
              {
              // Post just extended past the bottom of one post.
              return;
              }
              

              while (next++ != newend)
              {
              // Remove a post.
              // MAES: this is a struct copy.
                  solidsegs[++start].copy(solidsegs[next]);
              }

              newend = start+1;
              return;
          }
      }
      }
      
      // There is a fragment after *next.
      MySegs.StoreWallRange (solidsegs[next].last + 1, last);
      // Adjust the clip size.
      solidsegs[start].last = last;
      
      // Remove start+1 to next from the clip list,
      // because start now covers their area.
      
      { // crunch code
          if (next == start)
          {
          // Post just extended past the bottom of one post.
          return;
          }
          

          while (next++ != newend)
          {
          // Remove a post.
           // MAES: this is a struct copy.
              solidsegs[++start].copy(solidsegs[next]);
          }

          newend = start+1;
          return;
      }      
  }



  //
  // R_ClipPassWallSegment
  // Clips the given range of columns,
  //  but does not includes it in the clip list.
  // Does handle windows,
  //  e.g. LineDefs with upper and lower texture.
  //
  public void  ClipPassWallSegment (int   first,
          int   last ) {
      cliprange_t start;

      // Find the first range that touches the range
      //  (adjacent pixels are touching).
      int startptr=0;
      start = solidsegs[startptr];
      while (start.last < first-1){
      start = solidsegs[startptr++];
      }
      
      
      if (first < start.first)
      {
      if (last < start.first-1)
      {
          // Post is entirely visible (above start).
          MySegs.StoreWallRange (first, last);
          return;
      }
          
      // There is a fragment above *start.
      MySegs.StoreWallRange (first, start.first - 1);
      }

      // Bottom contained in start?
      if (last <= start.last)
      return;         
       
      //MAES: Java absolutely can't do without a sanity check here.
      if (startptr>=MAXSEGS-2) return;
      
      while (last >= solidsegs[startptr+1].first-1)
      {
      // There is a fragment between two posts.
      MySegs.StoreWallRange (start.last + 1, solidsegs[startptr+1].first - 1);
      startptr++;
      if (startptr>=MAXSEGS-2) return;
      start=solidsegs[startptr];
      
      if (last <= start.last)
          return;
      }
      
      // There is a fragment after *next.
      MySegs.StoreWallRange (start.last + 1, last);
  }



  /**
   * R_ClearClipSegs
   */

  public void ClearClipSegs ()
  {
      solidsegs[0].first = -0x7fffffff;
      solidsegs[0].last = -1;
      solidsegs[1].first = viewwidth;
      solidsegs[1].last = 0x7fffffff;
      newend = 2; // point so solidsegs[2];
  }

  //
  // R_AddLine
  // Clips the given segment
  // and adds any visible pieces to the line list.
  //
  public void AddLine (seg_t  line) 
  {
      if (DEBUG) System.out.println("Entered AddLine for "+line);
      int         x1;
      int         x2;
      long     angle1;
      long     angle2;
      long     span;
      long     tspan;
      
      curline = line;

      // OPTIMIZE: quickly reject orthogonal back sides.
      angle1 = PointToAngle (line.v1.x, line.v1.y);
      angle2 = PointToAngle (line.v2.x, line.v2.y);
      
      // Clip to view edges.
      // OPTIMIZE: make constant out of 2*clipangle (FIELDOFVIEW).
      span = angle1 - angle2;
      span&=BITS32;
      
      // Back side? I.e. backface culling?
      if (span >= ANG180)
      return;     

      // Global angle needed by segcalc.
      rw_angle1 = angle1;
      angle1 -= viewangle;
      angle2 -= viewangle;
      
      angle1&=BITS32;
      angle2&=BITS32;
      
      tspan = angle1 + clipangle;
      tspan&=BITS32;
      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;
      tspan&=BITS32;

      // Totally off the left edge?
      if (tspan >= span)
          return;
      
      angle1 = clipangle;
      }
      tspan = clipangle - angle2;
      tspan&=BITS32;
      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;
      tspan&=BITS32;

      // Totally off the left edge?
      if (tspan >= span)
          return; 
      angle2 = -clipangle;
      angle2 &=BITS32;
      }
      
      // The seg is in the view range,
      // but not necessarily visible.
      
      //System.out.println(Long.toHexString(angle1));
      //System.out.println(Long.toHexString(angle2));
      angle1&=BITS32;
      angle2&=BITS32;
      //System.out.println(Long.toHexString(angle1));
      //System.out.println(Long.toHexString(angle2));
      angle1 = ((angle1+ANG90)&BITS32)>>>ANGLETOFINESHIFT;
      angle2 = ((angle2+ANG90)&BITS32)>>>ANGLETOFINESHIFT;
      //System.out.println(Long.toHexString(angle1));
      //System.out.println(Long.toHexString(angle2));
      x1 = viewangletox[(int) angle1];
      x2 = viewangletox[(int) angle2];

      // Does not cross a pixel?
      if (x1 == x2)
      return;             
      
      backsector = line.backsector;

      // Single sided line?
      
      if (backsector==null) {
          if (DEBUG) System.out.println("Entering ClipSolidWallSegment SS");
          ClipSolidWallSegment (x1, x2-1); // to clipsolid
          if (DEBUG) System.out.println("Exiting ClipSolidWallSegment");
          return;
          }
          

      // Closed door.
      if (backsector.ceilingheight <= frontsector.floorheight
      || backsector.floorheight >= frontsector.ceilingheight) {
          if (DEBUG)  System.out.println("Entering ClipSolidWallSegment Closed door");
          ClipSolidWallSegment (x1, x2-1);; // to clipsolid    
          return;
          }

      // Window.
      if (backsector.ceilingheight != frontsector.ceilingheight
      || backsector.floorheight != frontsector.floorheight) {
          if (DEBUG) System.out.println("Entering ClipSolidWallSegment window");
          ClipPassWallSegment (x1, x2-1); // to clippass
          return;
          }
          
      // Reject empty lines used for triggers
      //  and special events.
      // Identical floor and ceiling on both sides,
      // identical light levels on both sides,
      // and no middle texture.
      if (backsector.ceilingpic == frontsector.ceilingpic
      && backsector.floorpic == frontsector.floorpic
      && backsector.lightlevel == frontsector.lightlevel
      && curline.sidedef.midtexture == 0)
      {
      return;
      }
      if (DEBUG) System.out.println("Exiting AddLine for "+line);
  }


  //
  // R_CheckBBox
  // Checks BSP node/subtree bounding box.
  // Returns true
  //  if some part of the bbox might be visible.
  //
  private int[][] checkcoord =
  {
      {3,0,2,1},
      {3,0,2,0},
      {3,1,2,0},
      {0},
      {2,0,2,1},
      {0,0,0,0},
      {3,1,3,0},
      {0},
      {2,0,3,1},
      {2,1,3,1},
      {2,1,3,0}
  };


  /**
   * 
   * @param bspcoord (fixed_t* as bbox)
   * @return
   */
  public boolean CheckBBox (int[]    bspcoord)
  {
      int         boxx;
      int         boxy;
      int         boxpos;

      // fixed_t
      int     x1;
      int     y1;
      int     x2;
      int     y2;
      
      //angle_t
      long     angle1;
      long     angle2;
      long     span;
      long     tspan;
      
      cliprange_t start;

      int         sx1;
      int         sx2;
      
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
          
      boxpos = (boxy<<2)+boxx;
      if (boxpos == 5)
      return true;
      
      x1 = bspcoord[checkcoord[boxpos][0]];
      y1 = bspcoord[checkcoord[boxpos][1]];
      x2 = bspcoord[checkcoord[boxpos][2]];
      y2 = bspcoord[checkcoord[boxpos][3]];
      
      // check clip list for an open space
      angle1 = PointToAngle (x1, y1) - viewangle;
      angle2 = PointToAngle (x2, y2) - viewangle;
      
      angle1&=BITS32;
      angle2&=BITS32;
      
      span = angle1 - angle2;

      span&=BITS32;
      
      // Sitting on a line?
      if (span >= ANG180)
      return true;
      
      tspan = angle1 + clipangle;
      tspan&=BITS32;
      
      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;
      tspan&=BITS32;
      // Totally off the left edge?
      if (tspan >= span)
          return false;   

      angle1 = clipangle;
      }
      tspan = (clipangle - angle2)&BITS32;;
      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;
      tspan&=BITS32;

      // Totally off the left edge?
      if (tspan >= span)
          return false;
      
      angle2 = -clipangle;
      angle2&=BITS32;
      }


      
      // Find the first clippost
      //  that touches the source post
      //  (adjacent pixels are touching).
      angle1 = ((angle1+ANG90)&BITS32)>>>ANGLETOFINESHIFT;
      angle2 = ((angle2+ANG90)&BITS32)>>>ANGLETOFINESHIFT;
      sx1 = viewangletox[(int) angle1];
      sx2 = viewangletox[(int) angle2];

      // Does not cross a pixel.
      if (sx1 == sx2)
      return false;           
      sx2--;
      
      int pstart = 0;
      start=solidsegs[pstart];
      // FIXME: this overflows
      while (start.last < sx2 && pstart<MAXSEGS)
      start=solidsegs[pstart++];
      
      if (sx1 >= start.first
      && sx2 <= start.last)
      {
      // The clippost contains the new span.
      return false;
      }

      return true;
  }



  /**
   * R_Subsector
   *  
   * Determine floor/ceiling planes.
   * Add sprites of things in sector.
   * Draw one or more line segments.
   * 
   * @param num Subsector from subsector_t list in Lever Loader.
   *      
   */
  
  public void Subsector (int num)  
  {
      System.out.println("SubSector " + num);
      int         count;
      int        line; // pointer into a list of segs instead of seg_t
      subsector_t    sub;
      int psub=0;
      
  if (RANGECHECK){
      if (num>=LL.numsubsectors)
      I.Error ("R_Subsector: ss %i with numss = %i",
           num,
           LL.numsubsectors);
  }

      sscount++;
      sub = LL.subsectors[num];
      frontsector = sub.sector;
      count = sub.numlines;
      //line = LL.segs[sub.firstline];
      line=sub.firstline;

      if (frontsector.floorheight < viewz)
      {
      floorplane = MyPlanes.FindPlane (frontsector.floorheight,
                    frontsector.floorpic,
                    frontsector.lightlevel);
      }
      else
          // FIXME: unclear what would happen with a null visplane used
          floorplane = -1; // in lieu of NULL
      
      if (frontsector.ceilingheight > viewz 
      || frontsector.ceilingpic == skyflatnum)
      {
          ceilingplane = MyPlanes.FindPlane (frontsector.ceilingheight,
                      frontsector.ceilingpic,
                      frontsector.lightlevel);
      }
      else
          ceilingplane = -1;
          
      MyThings.AddSprites (frontsector); 

      System.out.println("Enter Addline for SubSector " + num+" count "+count);
      while (count-->0)
      {
      AddLine (LL.segs[line]);
      line++;
      }
      System.out.println("Exit Addline for SubSector " + num);
  }




  /**
   * RenderBSPNode
   * Renders all subsectors below a given node,
   *  traversing subtree recursively.
   * Just call with BSP root.
   */
  public void RenderBSPNode (int bspnum)
  {
      System.out.println("Processing BSP Node "+bspnum);
      if (bspnum==33001){
          System.out.println("shit");
      }
      node_t  bsp;
      int     side;

      // Found a subsector?
      if (C2JUtils.flags(bspnum ,NF_SUBSECTOR))
      {
          System.out.println("Subsector found.");
      if (bspnum == -1)           
          Subsector (0);
      else
          Subsector (bspnum&(~NF_SUBSECTOR));
      return;
      }
          
      bsp = LL.nodes[bspnum];
      
      // Decide which side the view point is on.
      side = bsp.PointOnSide (viewx, viewy);

      // Recursively divide front space.
      System.out.println("Enter Front space of "+ bspnum);
      RenderBSPNode (bsp.children[side]); 
      System.out.println("Return Front space of "+ bspnum);
      
      // Possibly divide back space.
      
      if (CheckBBox (bsp.bbox[side^1].bbox)){
          System.out.println("Enter Back space of "+bspnum);
          RenderBSPNode (bsp.children[side^1]);
          System.out.println("Return Back space of "+bspnum);
      }
  }

  
  }
  
  class Segs{

      protected static final int HEIGHTBITS   =   12;
      protected static final int HEIGHTUNIT   =   (1<<HEIGHTBITS);
      
      public Segs(){
       col=new column_t();          
      }

      //
      // R_RenderMaskedSegRange
      //
      
      column_t    col;
      
      public void
      RenderMaskedSegRange
      ( drawseg_t ds,
        int       x1,
        int       x2 ) 
      {
          int index;
          
          int     lightnum;
          int     texnum;
          
          // Calculate light table.
          // Use different light tables
          //   for horizontal / vertical / diagonal. Diagonal?
          // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
          curline = ds.curline;
          frontsector = curline.frontsector;
          backsector = curline.backsector;
          texnum = texturetranslation[curline.sidedef.midtexture];
          
          lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT)+extralight;

          if (curline.v1.y == curline.v2.y)
          lightnum--;
          else if (curline.v1.x == curline.v2.x)
          lightnum++;

          if (lightnum < 0)       
          walllights = scalelight[0];
          else if (lightnum >= LIGHTLEVELS)
          walllights = scalelight[LIGHTLEVELS-1];
          else
          walllights = scalelight[lightnum];

          // Get the list
          maskedtexturecol = ds.getMaskedTextureColList();
          // And this is the pointer.
          int pmtc=ds.getMaskedTextureColPointer();

          rw_scalestep = ds.scalestep;        
          spryscale = ds.scale1 + (x1 - ds.x1)*rw_scalestep;
          
          // TODO: add the pointers for those somewhere
          mfloorclip = ds.getSprBottomClipList();
          
          mceilingclip = ds.getSprTopClipList();
          
          // find positioning
          if ((curline.linedef.flags & ML_DONTPEGBOTTOM)!=0)
          {
          dc_texturemid = frontsector.floorheight > backsector.floorheight
              ? frontsector.floorheight : backsector.floorheight;
          dc_texturemid = dc_texturemid + textureheight[texnum] - viewz;
          }
          else
          {
          dc_texturemid =frontsector.ceilingheight<backsector.ceilingheight
              ? frontsector.ceilingheight : backsector.ceilingheight;
          dc_texturemid = dc_texturemid - viewz;
          }
          dc_texturemid += curline.sidedef.rowoffset;
                  
          if (fixedcolormap!=null)
          dc_colormap = fixedcolormap;
          dco=0;
          
          // draw the columns
          for (dc_x = x1 ; dc_x <= x2 ; dc_x++)
          {
          // calculate lighting
          if (maskedtexturecol[dc_x] != Short.MAX_VALUE)
          {
              if (fixedcolormap==null)
              {
              index = spryscale>>LIGHTSCALESHIFT;

              if (index >=  MAXLIGHTSCALE )
                  index = MAXLIGHTSCALE-1;

              dc_colormap = walllights[index];
              //dco=index;
              }
                  
              sprtopscreen = centeryfrac - FixedMul(dc_texturemid, spryscale);
              dc_iscale = (int) (0xffffffffL / spryscale);
              
              // draw the texture
              col.data = GetColumn(texnum,maskedtexturecol[dc_x]);// -3);
              col.setFromData();
                  
              DrawMaskedColumn (col);
              maskedtexturecol[dc_x] = Short.MAX_VALUE;
          }
          spryscale += rw_scalestep;
          }
          
      }




      /**
       * R_RenderSegLoop
       * Draws zero, one, or two textures (and possibly a masked
       *  texture) for walls.
       * Can draw or mark the starting pixel of floor and ceiling
       *  textures.
       * CALLED: CORE LOOPING ROUTINE.
       *
       */
      
      public void RenderSegLoop () 
      {
          int     angle; // angle_t
          int     index;
          int         yl; // low
          int         yh; // hight
          int         mid;
          int     texturecolumn=0; // fixed_t
          int         top;
          int         bottom;
         
          for ( ; rw_x < rw_stopx ; rw_x++)
          {
          // mark floor / ceiling areas
          yl = (topfrac+HEIGHTUNIT-1)>>HEIGHTBITS;

          // no space above wall?
          if (yl < ceilingclip[rw_x]+1)
              yl = ceilingclip[rw_x]+1;
          
          if (markceiling)
          {
              top = ceilingclip[rw_x]+1;
              bottom = yl-1;

              if (bottom >= floorclip[rw_x])
              bottom = floorclip[rw_x]-1;

              if (top <= bottom)
              {
                  visplanes[ceilingplane].setTop(rw_x,(byte) top);
                  visplanes[ceilingplane].setBottom(rw_x, (byte) bottom);
              }
          }
              
          yh = bottomfrac>>HEIGHTBITS;

          if (yh >= floorclip[rw_x])
              yh = floorclip[rw_x]-1;

          if (markfloor)
          {
              top = yh+1;
              bottom = floorclip[rw_x]-1;
              if (top <= ceilingclip[rw_x])
              top = ceilingclip[rw_x]+1;
              if (top <= bottom)
              {
              visplanes[floorplane].setTop(rw_x, (byte) top);
              visplanes[floorplane].setBottom(rw_x, (byte) bottom);
              }
          }
          
          // texturecolumn and lighting are independent of wall tiers
          if (segtextured)
          {
              // calculate texture offset
              angle = Tables.toBAMIndex(rw_centerangle + xtoviewangle[rw_x]);
              texturecolumn = rw_offset-FixedMul(finetangent[(int) angle],rw_distance);
              texturecolumn >>= FRACBITS;
              // calculate lighting
              index = rw_scale>>LIGHTSCALESHIFT;

              if (index >=  MAXLIGHTSCALE )
              index = MAXLIGHTSCALE-1;

              dc_colormap = walllights[index];
              //dco=index;
              dc_x = rw_x;
              dc_iscale = (int) (0xffffffffL / (long)(0xFFFFFFFFL&rw_scale));
          }
          
          // draw the wall tiers
          if (midtexture!=0)
          {
              // single sided line
              dc_yl = yl;
              dc_yh = yh;
              dc_texturemid = rw_midtexturemid;              
              dc_source = GetColumn(midtexture,texturecolumn);
              System.out.println("Drawing column"+(texturecolumn&127)+" of mid texture "+textures[midtexture].name+ " at "+dc_yl+" "+dc_yh+" middle of texture at "+(dc_texturemid>>FRACBITS));
              colfunc.invoke();
              ceilingclip[rw_x] = (short) viewheight;
              floorclip[rw_x] = -1;
          }
          else
          {
              // two sided line
              if (toptexture!=0)
              {
              // top wall
              mid = pixhigh>>HEIGHTBITS;
              pixhigh += pixhighstep;

              if (mid >= floorclip[rw_x])
                  mid = floorclip[rw_x]-1;

              if (mid >= yl)
              {
                  dc_yl = yl;
                  dc_yh = mid;
                  dc_texturemid = rw_toptexturemid;
                  System.out.println("Drawing column"+(texturecolumn&127)+" of top texture "+textures[toptexture].name+ " at "+dc_yl+" "+dc_yh+" middle of texture at "+(dc_texturemid>>FRACBITS));
                  dc_source = GetColumn(toptexture,texturecolumn);
                  colfunc.invoke();
                  ceilingclip[rw_x] = (short) mid;
              }
              else
                  ceilingclip[rw_x] = (short) (yl-1);
              }
              else
              {
              // no top wall
              if (markceiling)
                  ceilingclip[rw_x] = (short) (yl-1);
              }
                  
              if (bottomtexture!=0)
              {
              // bottom wall
              mid = (pixlow+HEIGHTUNIT-1)>>HEIGHTBITS;
              pixlow += pixlowstep;

              // no space above wall?
              if (mid <= ceilingclip[rw_x])
                  mid = ceilingclip[rw_x]+1;
              
              if (mid <= yh)
              {
                  dc_yl = mid;
                  dc_yh = yh;
                  dc_texturemid = rw_bottomtexturemid;
                  dc_source = GetColumn(bottomtexture,
                              texturecolumn);
                  colfunc.invoke();
                  floorclip[rw_x] = (short) mid;
              }
              else
                  floorclip[rw_x] = (short) (yh+1);
              }
              else
              {
              // no bottom wall
              if (markfloor)
                  floorclip[rw_x] = (short) (yh+1);
              }
                  
              if (maskedtexture)
              {
              // save texturecol
              //  for backdrawing of masked mid texture
              maskedtexturecol[rw_x] = (short) texturecolumn;
              }
          }
              
          rw_scale += rw_scalestep;
          topfrac += topstep;
          bottomfrac += bottomstep;
          }
      }




      /**
       * R_StoreWallRange
       * A wall segment will be drawn
       *  between start and stop pixels (inclusive).
     * @throws IOException 
       */
      
      public void
      StoreWallRange
      ( int   start,
        int   stop ) 
      {
          int     hyp; //fixed_t
          int     sineval; //fixed_t
          long     distangle, offsetangle; // angle_t
          int     vtop; // fixed_t
          int         lightnum;
          drawseg_t seg;

          // don't overflow and crash
          if (ds_p == MAXDRAWSEGS)
          return;     
              
      if( RANGECHECK){
          if (start >=viewwidth || start > stop)
          I.Error ("Bad R_RenderWallRange: %i to %i", start , stop);
      }
          
          seg=drawsegs[ds_p];
      
          sidedef = curline.sidedef;
          linedef = curline.linedef;

          // mark the segment as visible for auto map
          linedef.flags |= ML_MAPPED;
          
          // calculate rw_distance for scale calculation
          rw_normalangle = curline.angle + ANG90;
          offsetangle = Math.abs(rw_normalangle-rw_angle1);
          
          if (offsetangle > ANG90)
          offsetangle = ANG90;

          distangle = ANG90 - offsetangle;
          hyp = PointToDist (curline.v1.x, curline.v1.y);
          sineval = finesine[(int) (distangle>>>ANGLETOFINESHIFT)];
          rw_distance = FixedMul (hyp, sineval);
              
          
          seg.x1 = rw_x = start;
          seg.x2 = stop;
          seg.curline = curline;
          rw_stopx = stop+1;
          
          // calculate scale at both ends and step
          seg.scale1 = rw_scale = 
          ScaleFromGlobalAngle (viewangle + xtoviewangle[start]);
          
          if (stop > start )
          {
          seg.scale2 = ScaleFromGlobalAngle (viewangle + xtoviewangle[stop]);
          seg.scalestep = rw_scalestep = 
              (seg.scale2 - rw_scale) / (stop-start);
          }
          else
          {
          // UNUSED: try to fix the stretched line bug
      /*#if 0
          if (rw_distance < FRACUNIT/2)
          {
              fixed_t     trx,try;
              fixed_t     gxt,gyt;

              trx = curline.v1.x - viewx;
              try = curline.v1.y - viewy;
                  
              gxt = FixedMul(trx,viewcos); 
              gyt = -FixedMul(try,viewsin); 
              seg.scale1 = FixedDiv(projection, gxt-gyt)<<detailshift;
          }
      #endif*/
          seg.scale2 = seg.scale1;
          }
          
          // calculate texture boundaries
          //  and decide if floor / ceiling marks are needed
          worldtop = frontsector.ceilingheight - viewz;
          worldbottom = frontsector.floorheight - viewz;
          
          midtexture = toptexture = bottomtexture = 0;
          maskedtexture = false;
          seg.setMaskedTextureCol(null, 0);
          //seg.maskedtexturecol = null;
          
          if (backsector==null)
          {
          // single sided line
          midtexture = texturetranslation[sidedef.midtexture];
          // a single sided line is terminal, so it must mark ends
          markfloor = markceiling = true;
          if ((linedef.flags & ML_DONTPEGBOTTOM)!=0)
          {
              vtop = frontsector.floorheight +
              textureheight[sidedef.midtexture];
              // bottom of texture at bottom
              rw_midtexturemid = vtop - viewz;    
          }
          else
          {
              // top of texture at top
              rw_midtexturemid = worldtop;
          }
          rw_midtexturemid += sidedef.rowoffset;

          seg.silhouette = SIL_BOTH;
          seg.setSprTopClip(screenheightarray, 0);
          seg.setSprBottomClip(negonearray, 0);
          seg.bsilheight = Integer.MAX_VALUE;
          seg.tsilheight = Integer.MIN_VALUE;
          }
          else
          {
          // two sided line
          seg.setSprTopClip(null,0);
          seg.setSprBottomClip(null,0);
          seg.silhouette = 0;
          
          if (frontsector.floorheight > backsector.floorheight)
          {
              seg.silhouette = SIL_BOTTOM;
              seg.bsilheight = frontsector.floorheight;
          }
          else if (backsector.floorheight > viewz)
          {
              seg.silhouette = SIL_BOTTOM;
              seg.bsilheight = Integer.MAX_VALUE;
              // seg.sprbottomclip = negonearray;
          }
          
          if (frontsector.ceilingheight < backsector.ceilingheight)
          {
              seg.silhouette |= SIL_TOP;
              seg.tsilheight = frontsector.ceilingheight;
          }
          else if (backsector.ceilingheight < viewz)
          {
              seg.silhouette |= SIL_TOP;
              seg.tsilheight = Integer.MIN_VALUE;
              // seg.sprtopclip = screenheightarray;
          }
              
          if (backsector.ceilingheight <= frontsector.floorheight)
          {
              seg.setSprBottomClip(negonearray,0);
              seg.bsilheight = Integer.MAX_VALUE;
              seg.silhouette |= SIL_BOTTOM;
          }
          
          if (backsector.floorheight >= frontsector.ceilingheight)
          {
              seg.setSprTopClip(screenheightarray, 0);
              seg.tsilheight = Integer.MIN_VALUE;
              seg.silhouette |= SIL_TOP;
          }
          
          worldhigh = backsector.ceilingheight - viewz;
          worldlow = backsector.floorheight - viewz;
              
          // hack to allow height changes in outdoor areas
          if (frontsector.ceilingpic == skyflatnum 
              && backsector.ceilingpic == skyflatnum)
          {
              worldtop = worldhigh;
          }
          
                  
          if (worldlow != worldbottom 
              || backsector.floorpic != frontsector.floorpic
              || backsector.lightlevel != frontsector.lightlevel)
          {
              markfloor = true;
          }
          else
          {
              // same plane on both sides
              markfloor = false;
          }
          
                  
          if (worldhigh != worldtop 
              || backsector.ceilingpic != frontsector.ceilingpic
              || backsector.lightlevel != frontsector.lightlevel)
          {
              markceiling = true;
          }
          else
          {
              // same plane on both sides
              markceiling = false;
          }
          
          if (backsector.ceilingheight <= frontsector.floorheight
              || backsector.floorheight >= frontsector.ceilingheight)
          {
              // closed door
              markceiling = markfloor = true;
          }
          

          if (worldhigh < worldtop)
          {
              // top texture
              toptexture = texturetranslation[sidedef.toptexture];
              if ((linedef.flags & ML_DONTPEGTOP)!=0)
              {
              // top of texture at top
              rw_toptexturemid = worldtop;
              }
              else
              {
              vtop =
                  backsector.ceilingheight
                  + textureheight[sidedef.toptexture];
              
              // bottom of texture
              rw_toptexturemid = vtop - viewz;    
              }
          }
          if (worldlow > worldbottom)
          {
              // bottom texture
              bottomtexture = texturetranslation[sidedef.bottomtexture];

              if ((linedef.flags & ML_DONTPEGBOTTOM )!=0)
              {
              // bottom of texture at bottom
              // top of texture at top
              rw_bottomtexturemid = worldtop;
              }
              else    // top of texture at top
              rw_bottomtexturemid = worldlow;
          }
          rw_toptexturemid += sidedef.rowoffset;
          rw_bottomtexturemid += sidedef.rowoffset;
          
          // allocate space for masked texture tables
          if (sidedef.midtexture!=0)
          {
              // masked midtexture
              maskedtexture = true;
              seg.setMaskedTextureCol(maskedtexturecol, lastopening - rw_x);
              lastopening += rw_stopx - rw_x;
          }
          }
          
          // calculate rw_offset (only needed for textured lines)
          segtextured =(((midtexture | toptexture | bottomtexture)!=0) | maskedtexture);

          if (segtextured)
          {
          offsetangle = (rw_normalangle-rw_angle1)&BITS32;
          
          if (offsetangle > ANG180)
              offsetangle = (-offsetangle)&BITS32;

          if (offsetangle > ANG90)
              offsetangle = ANG90;

          sineval = finesine(offsetangle);
          rw_offset = FixedMul (hyp, sineval);

          if (rw_normalangle-rw_angle1 < ANG180)
              rw_offset = -rw_offset;

          rw_offset += sidedef.textureoffset + curline.offset;
          rw_centerangle = ANG90 + viewangle - rw_normalangle;
          
          // calculate light table
          //  use different light tables
          //  for horizontal / vertical / diagonal
          // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
          if (fixedcolormap==null)
          {
              lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT)+extralight;

              if (curline.v1.y == curline.v2.y)
              lightnum--;
              else if (curline.v1.x == curline.v2.x)
              lightnum++;

              if (lightnum < 0)       
              walllights = scalelight[0];
              else if (lightnum >= LIGHTLEVELS)
              walllights = scalelight[LIGHTLEVELS-1];
              else
              walllights = scalelight[lightnum];
          }
          }
          
          // if a floor / ceiling plane is on the wrong side
          //  of the view plane, it is definitely invisible
          //  and doesn't need to be marked.
          
        
          if (frontsector.floorheight >= viewz)
          {
          // above view plane
          markfloor = false;
          }
          
          if (frontsector.ceilingheight <= viewz 
          && frontsector.ceilingpic != skyflatnum)
          {
          // below view plane
          markceiling = false;
          }

          
          // calculate incremental stepping values for texture edges
          worldtop >>= 4;
          worldbottom >>= 4;
          
          topstep = -FixedMul (rw_scalestep, worldtop);
          topfrac = (centeryfrac>>4) - FixedMul (worldtop, rw_scale);

          bottomstep = -FixedMul (rw_scalestep,worldbottom);
          bottomfrac = (centeryfrac>>4) - FixedMul (worldbottom, rw_scale);
          
          if (backsector!=null)
          {   
          worldhigh >>= 4;
          worldlow >>= 4;

          if (worldhigh < worldtop)
          {
              pixhigh = (centeryfrac>>4) - FixedMul (worldhigh, rw_scale);
              pixhighstep = -FixedMul (rw_scalestep,worldhigh);
          }
          
          if (worldlow > worldbottom)
          {
              pixlow = (centeryfrac>>4) - FixedMul (worldlow, rw_scale);
              pixlowstep = -FixedMul (rw_scalestep,worldlow);
          }
          }
          
          // render it
          if (markceiling)
          ceilingplane = MyPlanes.CheckPlane(ceilingplane, rw_x, rw_stopx-1);
          
          if (markfloor)
          floorplane = MyPlanes.CheckPlane (floorplane, rw_x, rw_stopx-1);

          RenderSegLoop ();

          
          // save sprite clipping info ... no top clipping?
          if ( ((seg.silhouette & SIL_TOP)!=0 || maskedtexture)
           && seg.nullSprTopClip())
          {
              
          //memcpy (lastopening, ceilingclip+start, 2*(rw_stopx-start));
          System.arraycopy(ceilingclip, start, openings, lastopening,  rw_stopx-start);
              
          seg.setSprTopClipPointer(lastopening - start);
          lastopening += rw_stopx - start;
          }
          // no floor clipping?
          if ( ((seg.silhouette & SIL_BOTTOM)!=0 || maskedtexture)
           && seg.nullSprBottomClip())
          {
          //memcpy (lastopening, floorclip+start, 2*(rw_stopx-start));
          System.arraycopy(floorclip, start, openings, lastopening,  rw_stopx-start);
          seg.setSprBottomClipPointer(lastopening - start);
          lastopening += rw_stopx - start;    
          }

          if (maskedtexture && (seg.silhouette&SIL_TOP)==0)
          {
          seg.silhouette |= SIL_TOP;
          seg.tsilheight = Integer.MIN_VALUE;
          }
          if (maskedtexture && (seg.silhouette&SIL_BOTTOM)==0)
          {
          seg.silhouette |= SIL_BOTTOM;
          seg.bsilheight = Integer.MAX_VALUE;
          }
          ds_p++;
      }
      }
  
  class Planes{

      public Planes (){
          C2JUtils.initArrayOfObjects(visplanes);
      }

      planefunction_t     floorfunc;
      planefunction_t     ceilingfunc;

      private final boolean RANGECHECK = false;
      
      //
      // spanstart holds the start of a plane span
      // initialized to 0 at start
      //
      int[]           spanstart=new int[SCREENHEIGHT];
      int[]           spanstop=new int [SCREENHEIGHT];

      //
      // texture mapping
      //
      byte[][]       planezlight;
      /** To treat as fixed_t */
      int         planeheight;
      /** To treat at fixed_t */
      int[]           yslope=new int[SCREENHEIGHT];
      /** To treat as fixed_t */
      int[]           distscale=new int[SCREENWIDTH];
      /** To treat as fixed_t */
      int         basexscale, baseyscale;

      /** To treat as fixed_t */
      int[]           cachedheight=new int[SCREENHEIGHT];
      /** To treat as fixed_t */
      int[]           cacheddistance=new int[SCREENHEIGHT];
      /** To treat as fixed_t */
      int[]           cachedxstep=new int[SCREENHEIGHT];
      /** To treat as fixed_t */
      int[]           cachedystep=new int[SCREENHEIGHT];



      //
      // R_InitPlanes
      // Only at game startup.
      //
      void InitPlanes ()
      {
        // Doh!
      }


      //
      // R_MapPlane
      //
      // Uses global vars:
      //  planeheight
      //  ds_source
      //  basexscale
      //  baseyscale
      //  viewx
      //  viewy
      //
      // BASIC PRIMITIVE
      //
      public void
      MapPlane
      ( int       y,
        int       x1,
        int       x2 )
      {
          // MAES: angle_t
          long angle;
          // 
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

          if (planeheight != cachedheight[y])
          {
          cachedheight[y] = planeheight;
          distance = cacheddistance[y] = FixedMul (planeheight, yslope[y]);
          ds_xstep = cachedxstep[y] = FixedMul (distance,basexscale);
          ds_ystep = cachedystep[y] = FixedMul (distance,baseyscale);
          }
          else
          {
          distance = cacheddistance[y];
          ds_xstep = cachedxstep[y];
          ds_ystep = cachedystep[y];
          }
          
          length = FixedMul (distance,distscale[x1]);
          angle = (viewangle + xtoviewangle[x1])&BITS32;
          ds_xfrac = viewx + FixedMul(finecosine(angle), length);
          ds_yfrac = -viewy - FixedMul(finesine(angle), length);

          if (fixedcolormap!=null)
          ds_colormap = fixedcolormap;
          else
          {
          index = distance >> LIGHTZSHIFT;
          
          if (index >= MAXLIGHTZ )
              index = MAXLIGHTZ-1;

          ds_colormap = planezlight[index];
          //pds_colormap=index;
          }
          
          ds_y = y;
          ds_x1 = x1;
          ds_x2 = x2;

          // high or low detail
          spanfunc.invoke();    
      }


      //
      // R_ClearPlanes
      // At begining of frame.
      //
      public void ClearPlanes ()
      {
          int angle;
          
          // opening / clipping determination
          for (int i=0 ; i<viewwidth ; i++)
          {
          floorclip[i] =(short) viewheight;
          ceilingclip[i] = -1;
          }

          lastvisplane = 1;
          lastopening = openings[0];
          
          // texture calculation
          //memset (cachedheight, 0, sizeof(cachedheight));

          // left to right mapping
          // FIXME: If viewangle is ever < ANG90, you're fucked. How can this be prevented?
          // Answer: 32-bit unsigned are supposed to roll over. You can & with 0xFFFFFFFFL.
          angle = (int) Tables.toBAMIndex(viewangle-ANG90);
          
          // scale will be unit scale at SCREENWIDTH/2 distance
          basexscale = FixedDiv (finecosine[angle],centerxfrac);
          baseyscale = -FixedDiv (finesine[angle],centerxfrac);
      }


      /**
       * R_FindPlane
       * 
       * Finds a visplane with the specified height, picnum and light level.
       * 
       * @param height (fixed_t)
       * @param picnum
       * @param lightlevel
       * @return was visplane_t*, returns index into visplanes[]
       */

      public int
      FindPlane
      ( int   height,
        int       picnum,
        int       lightlevel )
      {
          int check; // visplane_t* 
          visplane_t chk=null;
          
          if (picnum == DM.skyflatnum)
          {
          height = 0;         // all skys map together
          lightlevel = 0;
          }
          
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
                  
          if (check < lastvisplane)
          return check;
          
          
          // Found a visplane, but we can't add anymore.
          if (lastvisplane == MAXVISPLANES)
          I.Error ("R_FindPlane: no more visplanes");
              
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


      /**
       * R_CheckPlane
       */
      
      public int
      CheckPlane
      ( int index,
        int       start,
        int       stop )
      {
          int     intrl;
          int     intrh;
          int     unionl;
          int     unionh;
          visplane_t pl=visplanes[index];
          int x;
          
          if (start < pl.minx)
          {
          intrl = pl.minx;
          unionl = start;
          }
          else
          {
          unionl = pl.minx;
          intrl = start;
          }
          
          if (stop > pl.maxx)
          {
          intrh = pl.maxx;
          unionh = stop;
          }
          else
          {
          unionh = pl.maxx;
          intrh = stop;
          }

          for (x=intrl ; x<= intrh ; x++)
          if (pl.getTop(x) != 0xff)
              break;

          if (x > intrh)
          {
          pl.minx = unionl;
          pl.maxx = unionh;

          // use the same one
          return index;      
          }
          
          // make a new visplane
          visplanes[lastvisplane].height = pl.height;
          visplanes[lastvisplane].picnum = pl.picnum;
          visplanes[lastvisplane].lightlevel = pl.lightlevel;
          
          pl = visplanes[lastvisplane++];
          pl.minx = start;
          pl.maxx = stop;

          //memset (pl.top,0xff,sizeof(pl.top));
          pl.clearTop();
              
          //return pl;
          
          return lastvisplane;
      }


      /**
       * R_MakeSpans
       */

      public void MakeSpans
      ( int       x,
        int       t1,
        int       b1,
        int       t2,
        int       b2 )
      {
    	  //t1=C2JUtils.toUnsignedByte(t1);
          while (t1 < t2 && t1<=b1)
          {
          this.MapPlane (t1,spanstart[t1],x-1);
          t1++;
          }
          while (b1 > b2 && b1>=t1)
          {
          this.MapPlane (b1,spanstart[b1],x-1);
          b1--;
          }
          
          while (t2 < t1 && t2<=b2)
          {
          spanstart[t2] = x;
          t2++;
          }
          while (b2 > b1 && b2>=t2)
          {
          spanstart[b2] = x;
          b2--;
          }
      }



      /**
       * R_DrawPlanes
       * At the end of each frame.
       * @throws IOException 
       */
      public void DrawPlanes () 
      {
          System.out.println("DrawPlanes");
          visplane_t      pln=null; //visplane_t
          int         light;
          int         x;
          int         stop;
          int         angle;
                      
      if (RANGECHECK){
          if (ds_p > MAXDRAWSEGS)
          I.Error("R_DrawPlanes: drawsegs overflow (%i)",
              ds_p );
          
          if (lastvisplane > MAXVISPLANES)
              I.Error(" R_DrawPlanes: visplane overflow (%i)",
               lastvisplane);
          
          if (lastopening  > MAXOPENINGS)
              I.Error( "R_DrawPlanes: opening overflow (%i)",
               lastopening );
      }

          for (int pl = 0 ; pl < lastvisplane ;  pl++)
          {
              pln=visplanes[pl];
          if (pln.minx > pln.maxx)
              continue;

          
          // sky flat
          if (pln.picnum == DM.skyflatnum)
          {
              dc_iscale = pspriteiscale>>detailshift;
              
              // Sky is allways drawn full bright,
              //  i.e. colormaps[0] is used.
              // Because of this hack, sky is not affected
              //  by INVUL inverse mapping.
              dc_colormap = colormaps[0];
              dc_texturemid = skytexturemid;
              for (x=pln.minx ; x <= pln.maxx ; x++)
              {
            
              dc_yl = pln.getTop(x);
              dc_yh = pln.getBottom(x);
              
              if (dc_yl <= dc_yh)
              {
                  angle = (int) ((viewangle + xtoviewangle[x])>>>ANGLETOSKYSHIFT);
                  dc_x = x;
                  dc_source = GetColumn(skytexture, angle);
                  // TODO: Until you fix texture compositing, this can't work.
                  colfunc.invoke();
              }
              }
              continue;
          }
          
          // regular flat
          ds_source = ((flat_t)W.CacheLumpNum(firstflat +
                         flattranslation[pln.picnum],
                         PU_STATIC,flat_t.class)).data;
          
          planeheight = Math.abs(pln.height-viewz);
          light = (pln.lightlevel >>> LIGHTSEGSHIFT)+extralight;

          if (light >= LIGHTLEVELS)
              light = LIGHTLEVELS-1;

          if (light < 0)
              light = 0;

          planezlight = zlight[light];

          pln.setTop(pln.maxx+1,(byte) 0xff);
          pln.setTop(pln.minx-1, (byte) 0xff);
          
          stop = pln.maxx + 1;

          for (x=pln.minx ; x<= stop ; x++)
          {
              MakeSpans(x,pln.getTop(x-1),
                  pln.getBottom(x-1),
                  pln.getTop(x),
                  pln.getBottom(x));
          }
          
          //Z_ChangeTag (ds_source, PU_CACHE);
          }
      }

  } // End Plane class
      
/** Refresh of things, i.e. objects represented by sprites. */
  
  class Things{

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
       * Sprite rotation 0 is facing the viewer,
       *  rotation 1 is one angle turn CLOCKWISE around the axis.
       * This is not the same as the angle,
       * which increases counter clockwise (protractor).
       * There was a lot of stuff grabbed wrong, so I changed it...
       * 
       * (fixed_t)
       */
      
      int     pspritescale;
      int     pspriteiscale;

      byte[][]  spritelights;

      /** constant arrays
         used for psprite clipping and initializing clipping */
      short[]       negonearray;
      short[]       screenheightarray;


      //
      // INITIALIZATION FUNCTIONS
      //

      /**
       * R_InstallSpriteLump
       * Local function for R_InitSprites.
       */
      
      private void
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
              
          sprites = new spritedef_t[numsprites];
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
              sprites[i].numframes = 0;
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
          
          sprites[i].copy(sprtemp,maxframe);
          
          
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

    private boolean shadow;

      /**
       * R_DrawVisSprite
       *  mfloorclip and mceilingclip should also be set.
       *  
       * Sprites are actually drawn here.
       *
       */ 
      public void
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
          
          
          patch = W.CachePatchNum (vis.patch+firstspritelump,PU_CACHE);

          dc_colormap = vis.colormap;
          
          if (dc_colormap==null)
          {
          // NULL colormap = shadow draw
          shadow=true;
          colfunc = fuzzcolfunc;
          }
          else if ((vis.mobjflags & MF_TRANSLATION)!=0)
          {
          shadow=false;
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
          sprdef = sprites[thing.sprite.ordinal()];
      if(RANGECHECK){
          if ( (thing.frame&FF_FRAMEMASK) >= sprdef.numframes )
          I.Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
               thing.sprite, thing.frame);
      }
          sprframe = sprdef.spriteframes[ thing.frame & FF_FRAMEMASK];

          if (sprframe.rotate!=0)
          {
          // choose a different rotation based on player view
          ang = PointToAngle (thing.x, thing.y);
          rot = (int) ((ang-thing.angle+(ANG45/2)*9)&BITS32>>>29);
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
       * During BSP traversal, this adds sprites by sector.
       */
      public void AddSprites (sector_t sec)
      {
          System.out.println("AddSprites");
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
       */
      
      public void DrawPSprite (pspdef_t psp)
      {
          int     tx;
          int         x1;
          int         x2;
          spritedef_t    sprdef;
          spriteframe_t  sprframe;
          int         lump;
          boolean     flip;
          
          // vissprite_t     avis; ?
          
          // decide which patch to use
      if(RANGECHECK){
          if ( psp.state.sprite.ordinal() >= numsprites)
          I.Error ("R_ProjectSprite: invalid sprite number %i ",
               psp.state.sprite);
          }
          sprdef = sprites[psp.state.sprite.ordinal()];
      if(RANGECHECK){
          if ( (psp.state.frame & FF_FRAMEMASK)  >= sprdef.numframes)
          I.Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
               psp.state.sprite, psp.state.frame);
      }
          sprframe = sprdef.spriteframes[psp.state.frame & FF_FRAMEMASK ];

          lump = sprframe.lump[0];
          flip = (boolean)(sprframe.flip[0]!=0);
          
          // calculate edges of the shape
          tx = psp.sx-160*FRACUNIT;
          
          tx -= spriteoffset[lump];   
          x1 = (centerxfrac + FixedMul (tx,pspritescale) ) >>FRACBITS;

          // off the right side
          if (x1 > viewwidth)
          return;     

          tx +=  spritewidth[lump];
          x2 = ((centerxfrac + FixedMul (tx, pspritescale) ) >>FRACBITS) - 1;

          // off the left side
          if (x2 < 0)
          return;
          
          // store information in a vissprite
          //vis = avis;
          vis.mobjflags = 0;
          vis.texturemid = (BASEYCENTER<<FRACBITS)+FRACUNIT/2-(psp.sy-spritetopoffset[lump]);
          vis.x1 = x1 < 0 ? 0 : x1;
          vis.x2 = x2 >= viewwidth ? viewwidth-1 : x2;   
          vis.scale = pspritescale<<detailshift;
          
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
          
          DrawVisSprite (vis, vis.x1, vis.x2);
      }

      /** used inside DrawPSprite, better make this static */
      protected vissprite_t    vis=new vissprite_t();

      /** R_DrawPlayerSprites */
      
      public void DrawPlayerSprites ()
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
          mceilingclip = negonearray;
          
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
                  
          if (silhouette == 1)
          {
              // bottom sil
              for (x=r1 ; x<=r2 ; x++)
              if (clipbot[x] == -2)
                  // clipbot[x] = ds->sprbottomclip[x];
                  if (dss.nullSprBottomClip()){
                      System.out.println("Bottom clipping requested but clip list not present for "+dss);
                      
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
          mceilingclip = cliptop;
          DrawVisSprite (spr, spr.x1, spr.x2);
      }




      //
      // R_DrawMasked
      //
      public void DrawMasked ()
      {
          vissprite_t    spr;
         int ds;
         drawseg_t dss;
          
          SortVisSprites ();

          // Sprite "0" not visible?
          if (vissprite_p > 0)
          {
          // draw all vissprites back to front
          for (spr = MyThings.vsprsortedhead.next ;
               spr != vsprsortedhead ;
               spr=spr.next)
          {
              
              DrawSprite (spr);
          }
          }
          
          // render any remaining masked mid textures
          for (ds=ds_p-1 ; ds >= 0 ; ds--) {
              dss=drawsegs[ds];
                        if (!dss.nullMaskedTextureCol())
              MySegs.RenderMaskedSegRange (dss, dss.x1, dss.x2);
          
          // draw the psprites on top of everything
          //  but does not draw on side views
          if (viewangleoffset==0)       
          DrawPlayerSprites ();
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
  
  //
  // R_InitTextureMapping
  //
  public void InitTextureMapping ()
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
                  finetangent[FINEANGLES/4+FIELDOFVIEW/2] );
      
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
      xtoviewangle[x] = (i<<ANGLETOFINESHIFT)-ANG90;
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
  }



  //
  // R_InitLightTables
  // Only inits the zlight table,
  //  because the scalelight table changes with view size.
  //
  protected static int DISTMAP    =   2;

  public void InitLightTables ()
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
  
    
  

//
// R_InitTranslationTables
// Creates the translation tables to map
// the green color ramp to gray, brown, red.
// Assumes a given structure of the PLAYPAL.
// Could be read from a lump instead.
//
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

/**
 * R_DrawMaskedColumn
 * Used for sprites and masked mid textures.
 * Masked means: partly transparent, i.e. stored
 *  in posts/runs of opaque pixels.
 */

public void DrawMaskedColumn (column_t column)
{
    int     topscreen;
    int     bottomscreen;
    int basetexturemid; // fixed_t
    
    basetexturemid = dc_texturemid;
    // That's true for the whole column.
    dc_source = column.data;
    
    // for each post...
    for (int i=0;i<column.posts;i++ ) 
    {
    // calculate unclipped screen coordinates
    //  for post
    topscreen = sprtopscreen + spryscale*column.postdeltas[i];
    bottomscreen = topscreen + spryscale*column.postlen[i];

    dc_yl = (topscreen+FRACUNIT-1)>>FRACBITS;
    dc_yh = (bottomscreen-1)>>FRACBITS;
        
    if (dc_yh >= mfloorclip[dc_x])
        dc_yh = mfloorclip[dc_x]-1;
    if (dc_yl <= mceilingclip[dc_x])
        dc_yl = mceilingclip[dc_x]+1;

    if (dc_yl <= dc_yh)
    {
        // Set pointer inside column to current post's data
        // Rremember, it goes {postlen}{postdelta}{pad}[data]{pad} 
        dc_source_ofs = column.postofs[i] + 3;
        dc_texturemid = basetexturemid - (column.postdeltas[i]<<FRACBITS);
        // dc_source = (byte *)column + 3 - column.topdelta;

        // Drawn by either R_DrawColumn
        //  or (SHADOW) R_DrawFuzzColumn.
        if (MyThings.shadow){
            DrawFuzzColumn.invoke();
        } else {
            DrawColumn.invoke();
        }
        
         colfunc.invoke(); 
    }
    //column = (column_t *)(  (byte *)column + column.length + 4);
    }
    
    dc_texturemid = basetexturemid;
}

/**
 * R_DrawMaskedColumn
 * Used for sprites and masked mid textures.
 * Masked means: partly transparent, i.e. stored
 *  in posts/runs of opaque pixels.
 *  
 *  NOTE: this version accepts raw bytes, in case you  know what you're doing.
 */

public void DrawMaskedColumn (byte[] column)
{
    int     topscreen;
    int     bottomscreen;
    int basetexturemid; // fixed_t
    int topdelta;
    
    basetexturemid = dc_texturemid;
    // That's true for the whole column.
    dc_source = column;
    int pointer=0;
    
    // for each post...
    while(column[pointer]!=0xFF)
    {
    // calculate unclipped screen coordinates
    //  for post
        topdelta=toUnsignedByte(column[pointer+1]);
    topscreen = sprtopscreen + spryscale*topdelta;
    bottomscreen = topscreen + spryscale*toUnsignedByte(column[pointer+2]);

    dc_yl = (topscreen+FRACUNIT-1)>>FRACBITS;
    dc_yh = (bottomscreen-1)>>FRACBITS;
        
    if (dc_yh >= mfloorclip[dc_x])
        dc_yh = mfloorclip[dc_x]-1;
    if (dc_yl <= mceilingclip[dc_x])
        dc_yl = mceilingclip[dc_x]+1;

    if (dc_yl <= dc_yh)
    {
        // Set pointer inside column to current post's data
        // Rremember, it goes {postlen}{postdelta}{pad}[data]{pad} 
        dc_source_ofs = pointer+3;
        dc_texturemid = basetexturemid - (topdelta<<FRACBITS);
        // dc_source = (byte *)column + 3 - column.topdelta;

        // Drawn by either R_DrawColumn
        //  or (SHADOW) R_DrawFuzzColumn.
        if (MyThings.shadow){
            DrawFuzzColumn.invoke();
        } else {
            DrawColumn.invoke();
        }
        
        // colfunc (); 
    }
    //column = (column_t *)(  (byte *)column + column.length + 4);
    }
    
    dc_texturemid = basetexturemid;
}

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

     if (scaledviewwidth == 320)
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

 public void InitBuffer(int width, int height) {
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
 
 
 /**
  * Draws the actual span.
  */
 
 class R_DrawSpan implements colfunc_t {
     public void invoke(){
     int f_xfrac; // fixed_t
     int f_yfrac; // fixed_t
     int dest;
     int count;
     int spot;

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

     } while (count-- != 0);
 }
 }
 
 class R_DrawSpanLow implements colfunc_t{

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
        ds_x1 <<= 1;
        ds_x2 <<= 1;

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

        } while (count-- != 0);
        
    }
     
 }
 
 
 /** UNUSED.
  * Loop unrolled by 4.
  */
 
 public void R_DrawSpan() {
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
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest] = colormap[0x00FF&source[spot]];
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest+1] = colormap[0x00FF&source[spot]];
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest+2] = colormap[0x00FF&source[spot]];
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest+3] = colormap[0x00FF&source[spot]];
         count -= 4;
         dest += 4;
     }
     
     while (count > 0) {
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest++] = colormap[0x00FF&source[spot]];
         count--;
     }
 }
 
 


///////////////////////// The actual rendering calls ///////////////////////
 
 /**
  * R_RenderView
  */
 
public void RenderPlayerView (player_t player)
{   
  SetupFrame (player);

  // Clear buffers.
  MyBSP.ClearClipSegs ();
  MyBSP.ClearDrawSegs ();
  MyPlanes.ClearPlanes ();
  MyThings.ClearSprites ();
  
  // TODO: check for new console commands.
  //NetUpdate ();

  // The head node is the last node output.
  MyBSP.RenderBSPNode (LL.numnodes-1);
  
  // Check for new console commands.
  //NetUpdate ();
  
  MyPlanes.DrawPlanes ();
  
  // Check for new console commands.
  //NetUpdate ();
  
  MyThings.DrawMasked ();

  // Check for new console commands.
 // NetUpdate ();             
}

//
//R_SetupFrame
//
public void SetupFrame (player_t player)
{       
 int     i;
 
 viewplayer = player;
 viewx = player.mo.x;
 viewy = player.mo.y;
 viewangle = player.mo.angle + viewangleoffset;
 extralight = player.extralight;

 viewz = player.viewz;
 
 viewsin = finesine[(int) (viewangle>>>ANGLETOFINESHIFT)];
 viewcos = finecosine[(int) (viewangle>>>ANGLETOFINESHIFT)];
 
 sscount = 0;
 
 if (player.fixedcolormap!=0)
 {
 fixedcolormap =colormaps[player.fixedcolormap];
 // Offset by fixedcolomap
 //pfixedcolormap =player.fixedcolormap*256;
 
 walllights = scalelightfixed;

 for (i=0 ; i<MAXLIGHTSCALE ; i++)
     scalelightfixed[i] = fixedcolormap;
 }
 else
 fixedcolormap = null;
     
 framecount++;
 validcount++;
}

/**
 * R_SetViewSize
 * Do not really change anything here,
 * because it might be in the middle of a refresh.
 * The change will take effect next refresh.
 */

// Who can set this? A: The Menu.
public boolean      setsizeneeded;
int     setblocks;
int     setdetail;

/**
 * 
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
 * R_ExecuteSetViewSize
 */

public void ExecuteSetViewSize ()
{
    int cosadj;
    int dy;
    int     i;
    int     j;
    int     level;
    int     startmap;   
    
    setsizeneeded = false;

    // 11 Blocks means "full screen"
    
    if (setblocks == 11)
    {
    scaledviewwidth = SCREENWIDTH;
    viewheight = SCREENHEIGHT;
    }
    else
    {
        scaledviewwidth = setblocks*32;
        viewheight = (short) ((setblocks*168/10)&~7);
    }
    
    detailshift = setdetail;
    viewwidth = scaledviewwidth>>detailshift;
    
    centery = viewheight/2;
    centerx = viewwidth/2;
    centerxfrac=(centerx<<FRACBITS);
    centeryfrac=(centery<<FRACBITS);
    projection=centerxfrac;

    // High detail
    if (detailshift==0)
    {
        
    colfunc = basecolfunc =DrawColumn;
    fuzzcolfunc = DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    spanfunc = DrawSpan;
    }
    else {
    // Low detail
    colfunc = basecolfunc = DrawColumnLow;
    fuzzcolfunc =DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    spanfunc = DrawSpanLow;
    
    }

    InitBuffer (scaledviewwidth, viewheight);
    
    InitTextureMapping ();
    
    // psprite scales
    pspritescale=(FRACUNIT*viewwidth/SCREENWIDTH);
    pspriteiscale=(FRACUNIT*SCREENWIDTH/viewwidth);
    
    // thing clipping
    for (i=0 ; i<viewwidth ; i++)
    screenheightarray[i] = (short) viewheight;
    
    // planes
    for (i=0 ; i<viewheight ; i++)
    {
    dy = ((i-viewheight/2)<<FRACBITS)+FRACUNIT/2;
    dy = Math.abs(dy);
    // MAES: yslope is a field in "r_plane.c" so it should really be in the Rendering Context.
    MyPlanes.yslope[i] = FixedDiv ( (viewwidth<<detailshift)/2*FRACUNIT, dy);
    }
    
    for (i=0 ; i<viewwidth ; i++)
    {
    cosadj = Math.abs(finecosine(i));
    MyPlanes.distscale[i] = FixedDiv (FRACUNIT,cosadj);
    }
    
    // Calculate the light levels to use
    //  for each level / scale combination.
    for (i=0 ; i< LIGHTLEVELS ; i++)
    {
    startmap = ((LIGHTLEVELS-1-i)*2)*NUMCOLORMAPS/LIGHTLEVELS;
    for (j=0 ; j<MAXLIGHTSCALE ; j++)
    {
        level = startmap - j*SCREENWIDTH/(viewwidth<<detailshift)/DISTMAP;
        
        if (level < 0)
        level = 0;

        if (level >= NUMCOLORMAPS)
        level = NUMCOLORMAPS-1;

        scalelight[i][j] = colormaps[level];
    }
    }
}
 
 
 
//////////////////////////////////From r_sky.c /////////////////////////////////////

/**
 * R_InitSkyMap
 * Called whenever the view size changes.
 */

public void InitSkyMap ()
{
    skyflatnum = FlatNumForName ( SKYFLATNAME );
    skytexturemid = 100*FRACUNIT;
}


  interface colfunc_t {
      public void invoke();
  }
  
  ///////////////////// Renderer Data ////////////////////////
  
  //
  // Graphics.
  // DOOM graphics for walls and sprites
  // is stored in vertical runs of opaque pixels (posts).
  // A column is composed of zero or more posts,
  // a patch or sprite is composed of zero or more columns.
  // 

  public int     firstflat;
  public int     lastflat;
  public int     numflats;
  /** HACK */
  public flat_t[] flats;

  
  public int     firstpatch;
  public int     lastpatch;
  public int     numpatches;

  public int     firstspritelump;
  public int     lastspritelump;
  public int     numspritelumps;

  public int     numtextures;
  
  /* The unchached textures themselves, stored just as patch lists and various properties */
  public texture_t[] textures;

  /** Width per texture? */
  int[]            texturewidthmask;
  /** fixed_t[] needed for texture pegging */
  
  /** How tall each composite texture is supposed to be */
  public int[]        textureheight;    
  
  /** How large each composite texture is supposed to be */
  int[]            texturecompositesize;
  /** Tells us which patch lump covers which column of which texture */
  short[][]         texturecolumnlump;
  /** This is supposed to store indexes into a patch_t lump which point to the columns themselves 
   *  Instead, we're going to return indexes to columns inside a particular patch.
   *  In the case of patches inside a non-cached multi-patch texture (e.g. those made of non-overlapping
   *  patches), we're storing indexes INSIDE A PARTICULAR PATCH. E.g. for STARTAN1, which is made of two
   *  32-px wide patches, it should go something like 0, 1,2 ,3...31, 0,1,2,....31.
   *  
   * */
  char[][]    texturecolumnofs;
  
  /** couple with texturecomposite */
  char texturecoloffset;
  //short[][]    texturecolumnindexes;
  /** Stores [textures][columns][data]. */
  byte[][][]          texturecomposite;

  // for global animation
  public int[]        flattranslation;
  public int[]        texturetranslation;

  /** needed for pre rendering (fixed_t[]) */
  int[]    spritewidth,spriteoffset,spritetopoffset;
  
  /* MAPTEXTURE_T CACHING
   * When a texture is first needed, it counts the number of 
   * composite columns required in the texture and allocates space
   * for a column directory and any new columns.
   * 
   *  The directory will simply point inside other patches if there 
   *  is only one patch in a given column but any columns 
   *  with multiple patches will have new column_ts generated.
   */

  /**
   *  R_DrawColumnInCache
   *  Clip and draw a column from a patch into a cached post.
   *  
   *  This means that columns are effectively "uncompressed" into cache, here,
   *  and that composite textures are generally uncompressed...right?
   *  
   *  Actually: "compressed" or "masked" textures are retrieved in the same way.
   *  There are both "masked" and "unmasked" drawing methods. If a masked
   *  column is passed to a method that expects a full, dense column...well,
   *  it will look fugly/overflow/crash. Vanilla Doom tolerated this, 
   *  we're probably going to have more problems.
   *  
   *  @param patch Actually it's a single column to be drawn. May overdraw existing ones or void space.
   *  @param cache the column cache itself. Actually it's the third level [texture][column]->data.
   *  @param offset an offset inside the column cache.
   *  @param originy vertical offset. Caution with masked stuff!
   *  @param cacheheight the maximum height it's supposed to reach when drawing?
   *  
   */
  
  public void
  DrawColumnInCache
  ( column_t patch,
    byte[]     cache,
    int offset,
    int       originy,
    int       cacheheight )
  {
      int     count;
      int     position;
      int  source=0; // treat as pointers
      int   dest=3; // Inside raw data cache
      
       /* Iterate inside column. This is starkly different from the C code,
        * because posts positions are already precomputed at load time
        */
      
      for (int i=0;i<patch.posts;i++){

          source += 3; // Relative to patch's data.
          count = patch.postlen[i]; // length of this particular post
          position = originy + patch.postdeltas[i]; // Position to draw inside cache.

      // Post would be drawn outside of screen. Fuck it.
      // FIXME: this could very well end up "between" posts.
      // Probably the cause of fucked up tiling in vanilla.
          
      if (position < 0)
      {
          count += position; // Consider that we have a "drawing debt".
          position = 0;
      }

      // Post will go too far outside.
      if (position + count > cacheheight)
          count = cacheheight - position;

      if (count > 0) // Draw this post. Won't draw posts that start "outside"

          // Will start at post's start, but will only draw enough pixels not to overdraw.
          System.arraycopy( patch.data, source, cache, position,count);
          
          // This should position us at the beginning of the next post
          source=patch.postofs[i]+patch.postlen[i]+4;
      }
  }



  /**
   * R_GenerateComposite
   * Using the texture definition,
   *  the composite texture is created from the patches,
   *  and each column is cached.
   * @throws IOException 
   */
  
  public void GenerateComposite (int texnum) 
  {
      byte[][]       block;
      texture_t      texture;
      texpatch_t[]     patch;  
      patch_t        realpatch=null;
      int         x;
      int         x1;
      int         x2;
      column_t       patchcol;
      short[]      collump;
      char[] colofs; // unsigned short
     // short[] colidxs; // unsigned short
      
      texture = textures[texnum];

      // Allocate both the composite and assign it to block.
      // texturecompositesize indicates a size in BYTES. We need a number of columns, though.
      // Z_Malloc (texturecompositesize[texnum], PU_STATIC, &texturecomposite[texnum]);
      block = texturecomposite[texnum]=new byte[texture.width][];
   
      // Now block is divided into columns. We need to allocate enough data for each column
      for (int i=0;i<texture.width;i++)
          block[i]=new byte[texture.height];
      

      collump = texturecolumnlump[texnum];
      colofs = texturecolumnofs[texnum];
      
     // colidxs = texturecolumnindexes[texnum];
      
      // Composite the columns together.
      patch = texture.patches;
     
      
      
      // For each patch in the texture...
      for (int i=0 ;i<texture.patchcount; i++)
      {

      realpatch = W.CachePatchNum(patch[i].patch, PU_CACHE);
          if (realpatch.name=="SKY1"){
              System.out.println("Sky found!");
          }
      x1 = patch[i].originx;
      x2 = x1 + realpatch.width;

      if (x1<0)
          x = 0;
      else
          x = x1;
      
      if (x2 > texture.width)
          x2 = texture.width;

      for ( ; x<x2 ; x++)
      {
          // Column does not have multiple patches?
          if (collump[x] >= 0)
          continue;
          
         // patchcol = (column_t *)((byte *)realpatch
          //            + LONG(realpatch.columnofs[x-x1]));
          
          
          // We can look this up cleanly in Java. Ha!
          patchcol=realpatch.columns[x-x1];
          DrawColumnInCache (patchcol,
                   block[x], colofs[x],
                   patch[i].originy,
                   texture.height);
      }
                          
      }
  }

  /**
   * R_GenerateLookup
   * 
   * Creates the lookup tables for a given texture (aka, where inside the texture cache
   * is the offset for particular column... I think.
   * 
   * @throws IOException 
   */
  
  protected void GenerateLookup (int texnum) throws IOException
  {
      texture_t      texture;
      short[]       patchcount; //Keeps track of how many patches overlap a column.
      texpatch_t[]     patch;  
      patch_t        realpatch;
      int         x;
      int         x1;
      int         x2;

      short[]      collump;
       char[] colofs;
      
      texture = textures[texnum];

      // Composited texture not created yet.
      texturecomposite[texnum] = null;
      
      // We don't know ho large the texture will be, yet, but it will be a multiple of its height.
      texturecompositesize[texnum] = 0;

      // This is the only place where those can be actually modified.
      // They are still null at this point.
      collump = texturecolumnlump[texnum];
      colofs = texturecolumnofs[texnum];
      
      /* Now count the number of columns  that are covered by more 
       * than one patch. Fill in the lump / offset, so columns
       * with only a single patch are all done.
       */

      patchcount = new short[texture.width];
      patch = texture.patches;
          
      // for each patch in a texture...
      for (int i=0; i<texture.patchcount;i++)
      {
      realpatch = (patch_t) W.CacheLumpNum (patch[i].patch, PU_CACHE,patch_t.class);
      x1 = patch[i].originx;
      x2 = x1 + realpatch.width;
      
      // Where does the patch start, inside the compositetexture?
      if (x1 < 0)
          x = 0;
      else
          x = x1;

      // Correct, starts at originx. Where does it end?
      
      if (x2 > texture.width)
          x2 = texture.width;
      for ( ; x<x2 ; x++)
      {
          /* Obviously, if a patch starts at x it does cover the x-th column
           *  of a texture, even if transparent. 
           */
          patchcount[x]++;
          // Column "x" of composite texture "texnum" is covered by this patch.
          collump[x] = (short) patch[i].patch;
          
          /* This is supposed to be a raw pointer to the beginning of the column
           * data, as it appears inside the PATCH.
           * 
           * Instead, we can return the actual column index (x-x1)
           * As an example, the second patch of STARTAN1 (width 64) starts
           * at column 32. Therefore colofs should be something like
           * 0,1,2,...,31,0,1,....31, indicating that the 32-th column of
           * STARTAN1 is the 0-th column of the patch that is assigned to that column
           * (the latter can be looked up in texturecolumnlump[texnum].
           * 
           * Any questions?
           * 
           */
          colofs[x] = (char) (x-x1);
          // This implies that colofs[x] is 0 for a void column?
              
      } // end column of patch.
      } // end patch
      
      // Now check all columns again.
      for ( x=0 ; x<texture.width ; x++)
      {
      // Can only occur if a column isn't covered by a patch at all, not even a transparent one.
      if (patchcount[x]==0)
      {
          System.err.print ("R_GenerateLookup: column without a patch ("+texture.name+")\n");
          return;
      }
      // I_Error ("R_GenerateLookup: column without a patch");
      
      
      // Columns where more than one patch overlaps.
      if (patchcount[x] > 1)
      {
          // Use the cached block. This column won't be read from the wad system.
          collump[x] = -1;    
          colofs[x] = (char) texturecompositesize[texnum];
          
          if (texturecompositesize[texnum] > 0x10000-texture.height)
          {
          I.Error ("R_GenerateLookup: texture %i is >64k",
               texnum);
          }
          
          texturecompositesize[texnum] += texture.height;
      }
      }   
  }




  /**
   * R_GetColumn
 * @throws IOException 
   */
  public byte[] GetColumn
  ( int       tex,
    int       col ) 
  {
      int     lump,ofs;
      
      col &= texturewidthmask[tex];
      lump = texturecolumnlump[tex][col];
      ofs=texturecolumnofs[tex][col];
      
      // If pointing inside a non-zero, positive lump, then it's not a composite texture.
      // Read from disk.
      if (lump > 0){
          // This will actually return a pointer to a patch's columns.
          // That is, to the ONE column exactly.{
         // this.dc_source_ofs=3;
          patch_t r=W.CachePatchNum(lump,PU_CACHE);
      return r.columns[ofs].data;
  }
      // Texture should be composite, but it doesn't yet exist. Create it. 
      if (texturecomposite[tex]==null) GenerateComposite (tex);

      // This implies that texturecomposite actually stores raw, compressed columns,
      // or else those "ofs" would go in-between.
      this.dc_source_ofs=0;
      return texturecomposite[tex][col];
  }




  //
  // R_InitTextures
  // Initializes the texture list
  //  with the textures from the world map.
  //
  public void InitTextures () throws IOException
  {
      maptexture_t   mtexture=new maptexture_t();
      texture_t      texture;
      mappatch_t[]     mpatch;
      texpatch_t[]    patch;

      ByteBuffer        maptex, maptex2, maptex1;
      
      String        name;
      ByteBuffer       names;
      int       name_p;
      
      int[]        patchlookup;
      
      int         totalwidth;
      int         nummappatches;
      int         offset;
      int         maxoff;
      int         maxoff2;
      int         numtextures1;
      int         numtextures2;

      int        directory;
      
      int         temp1;
      int         temp2;
      int         temp3;

      
      // Load the patch names from pnames.lmp.
      //name[8] = 0;    
      names = W.CacheLumpName ("PNAMES", PU_STATIC).getBuffer();
      names.order(ByteOrder.LITTLE_ENDIAN);
      
      // Number of patches.
      names.rewind();
      nummappatches = names.getInt();
      
      patchlookup = new int[nummappatches];
      
      for (int i=0 ; i<nummappatches ; i++)
      {
      // Get a size limited string;
      name=DoomBuffer.getString(names, 8).toUpperCase();
      patchlookup[i] = W.CheckNumForName (name);
      }
      
      names=null;
      
      // Load the map texture definitions from textures.lmp.
      // The data is contained in one or two lumps,
      //  TEXTURE1 for shareware, plus TEXTURE2 for commercial.
      maptex = maptex1 = W.CacheLumpName ("TEXTURE1", PU_STATIC).getBuffer();
      maptex.rewind();
      maptex.order(ByteOrder.LITTLE_ENDIAN);
      numtextures1 = maptex.getInt();
      maxoff = W.LumpLength (W.GetNumForName ("TEXTURE1"));
      directory = 1;
      
      if (W.CheckNumForName ("TEXTURE2") != -1)
      {
      maptex2 = W.CacheLumpName ("TEXTURE2", PU_STATIC).getBuffer();
      maptex2.order(ByteOrder.LITTLE_ENDIAN);
      maptex2.rewind();
      numtextures2 = maptex2.getInt();
      maxoff2 = W.LumpLength (W.GetNumForName ("TEXTURE2"));
      }
      else
      {
      maptex2 = null;
      numtextures2 = 0;
      maxoff2 = 0;
      }
      
      numtextures = numtextures1 + numtextures2;
      
      textures = new texture_t[numtextures];
      texturecolumnlump = new short[numtextures][];
      texturecolumnofs = new char[numtextures][];
      //texturecolumnindexes = new short[numtextures][];
      texturecomposite = new byte[numtextures][][];
      texturecompositesize = new int[numtextures];
      texturewidthmask = new int[numtextures];
      textureheight = new int[numtextures];

      totalwidth = 0;
      
      //  Really complex printing shit...
      temp1 = W.GetNumForName ("S_START");  // P_???????
      temp2 = W.GetNumForName ("S_END") - 1;
      temp3 = ((temp2-temp1+63)/64) + ((numtextures+63)/64);
      System.out.print("[");
      for (int i = 0; i < temp3; i++)
      System.out.print(" ");
      System.out.print("         ]");
      
      char BKSPC=0x08;
      for (int i = 0; i < temp3; i++)
          
      for (int bs=0;bs<11;bs++){
          System.out.print(BKSPC);
      }
      
      for (int i=0 ; i<numtextures ; i++,directory++)
      {
      if ((i&63)==0)
          System.out.print ('.');

      if (i == numtextures1)
      {
          // Start looking in second texture file.
          maptex = maptex2;
          maxoff = maxoff2;
          directory = 0; // offset "1" inside maptex buffer
      }
      //System.out.print("Directory "+directory);
      offset = maptex.getInt(directory*4);
      if (offset > maxoff)
          I.Error("R_InitTextures: bad texture directory");
     // System.out.print("offset "+offset+" \n");
      
      maptex.position(offset);
      // Read "maptexture", which is the on-disk form.
      mtexture.unpack(maptex);
      System.out.println(mtexture.name+ " @"+offset);
      
      // We don't need to manually copy trivial fields.
      textures[i]=new texture_t();
      textures[i].copyFromMapTexture(mtexture);
      texture = textures[i];
      //System.out.println("Patches: "+textures[i].patchcount);
      
      // However we do need to correct the "patch.patch" field through the patchlookup
      mpatch = mtexture.patches;
      patch = texture.patches;

      for (int j=0 ; j<texture.patchcount ; j++)
      {
          patch[j].patch = patchlookup[mpatch[j].patch];
          if (patch[j].patch == -1)
          {
          I.Error ("R_InitTextures: Missing patch in texture %s",
               texture.name);
          }
      }       
      
      // Columns and offsets of taxture = textures[i]
      texturecolumnlump[i] = new short[texture.width];
      //C2JUtils.initArrayOfObjects( texturecolumnlump[i], column_t.class);
      texturecolumnofs[i] = new char[texture.width];
      
      int j = 1;
      while (j*2 <= texture.width)
          j<<=1;

      texturewidthmask[i] = j-1;
      textureheight[i] = texture.height<<FRACBITS;
          
      totalwidth += texture.width;
      }

      maptex1=null;
      if (maptex2!=null)
      maptex2=null;
      
      // Precalculate whatever possible.  
      for (int i=0 ; i<numtextures ; i++)
      GenerateLookup (i);
      
      // Create translation table for global animation.
      texturetranslation = new int[numtextures];
      
      for (int i=0 ; i<numtextures ; i++)
          texturetranslation[i] = i;
  }



  //
  // R_InitFlats
  //
  protected void InitFlats ()
  {
      int     i;
      
      firstflat = W.GetNumForName ("F_START") + 1;
      lastflat = W.GetNumForName ("F_END") - 1;
      numflats = lastflat - firstflat + 1;
      
      // Create translation table for global animation.
      flattranslation = new int[numflats+1];
      
      for (i=0 ; i<numflats ; i++)
          flattranslation[i] = i;
  }


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
        InitTextures ();
      System.out.print ("\nInitTextures");
      InitFlats ();
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



    /**
     * R_FlatNumForName
     * Retrieval, get a flat number for a flat name.
     */
     
    public int FlatNumForName(String name) {
        int i;

        i = W.CheckNumForName(name);

        if (i == -1) {
            I.Error("R_FlatNumForName: %s not found", name);
        }
        return i - firstflat;
    }




    /**
     * R_CheckTextureNumForName Check whether texture is available. Filter out
     * NoTexture indicator. Seems shit-slow to me...
     */
    public int CheckTextureNumForName(String name) {
        int i;
        // "NoTexture" marker.
        if (name.charAt(0) == '-')
            return 0;

        for (i = 0; i < numtextures; i++)
            if (textures[i].name.compareToIgnoreCase(name) == 0)
                return i;

        return -1;
    }



    /**
     * R_TextureNumForName
     * Calls R_CheckTextureNumForName,
     * aborts with error message.
     */
  
    public int TextureNumForName(String name) {
        int i;

        i = CheckTextureNumForName(name);

        if (i == -1) {
            I.Error("R_TextureNumForName: %s not found", name);
        }
        return i;
    }




  //
  // R_PrecacheLevel
  // Preloads all relevant graphics for the level.
  //
  int     flatmemory;
  int     texturememory;
  int     spritememory;

  public void PrecacheLevel () throws IOException
  {
      boolean[]       flatpresent;
      boolean []      texturepresent;
      boolean []     spritepresent;

      int         i;
      int         j;
      int         k;
      int         lump;
      
      texture_t      texture;
      thinker_t      th;
      spriteframe_t  sf;

      if (DM.demoplayback)
      return;
      
      // Precache flats.
      flatpresent = new boolean[numflats];
      flats=new flat_t[numflats];
      
      for (i=0 ; i<LL.numsectors ; i++)
      {
      flatpresent[LL.sectors[i].floorpic] = true;
      flatpresent[LL.sectors[i].ceilingpic] = true;
      }
      
      flatmemory = 0;

      for (i=0 ; i<numflats ; i++)
      {
      if (flatpresent[i])
      {
          lump = firstflat + i;
          flatmemory += W.lumpinfo[lump].size;
          flats[i]=(flat_t) W.CacheLumpNum(lump, PU_CACHE,flat_t.class);
      }
      }
      
      // Precache textures.
      texturepresent = new boolean[numtextures];
      
      for (i=0 ; i<LL.numsides ; i++)
      {
      texturepresent[LL.sides[i].toptexture] = true;
      texturepresent[LL.sides[i].midtexture] = true;
      texturepresent[LL.sides[i].bottomtexture] = true;
      }

      // Sky texture is always present.
      // Note that F_SKY1 is the name used to
      //  indicate a sky floor/ceiling as a flat,
      //  while the sky texture is stored like
      //  a wall texture, with an episode dependend
      //  name.
      texturepresent[skytexture] = true;
      
      texturememory = 0;
      for (i=0 ; i<numtextures ; i++)
      {
      if (!texturepresent[i])
          continue;

      texture = textures[i];
      
      for (j=0 ; j<texture.patchcount ; j++)
      {
          lump = texture.patches[j].patch;
          texturememory += W.lumpinfo[lump].size;
          W.CacheLumpNum(lump , PU_CACHE,patch_t.class);
      }
      }
      
      // recache sprites.
      spritepresent = new boolean[numsprites];
      
      
      for (th = P.thinkercap.next ; th != P.thinkercap ; th=th.next)
      {
      if (th.function==think_t.P_MobjThinker)
          spritepresent[((mobj_t )th).sprite.ordinal()] = true;
      }
      
      spritememory = 0;
      for (i=0 ; i<numsprites ; i++)
      {
      if (!spritepresent[i])
          continue;

      for (j=0 ; j<sprites[i].numframes ; j++)
      {
          sf = sprites[i].spriteframes[j];
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

/**
 * R_Init
 */
  
public int  detailLevel;
public int  screenblocks=9; // has default

public void Init ()

{
	drawsegs=new drawseg_t[MAXDRAWSEGS];
	C2JUtils.initArrayOfObjects(drawsegs);
	
	// DON'T FORGET ABOUT MEEEEEE!!!11!!!
	this.screen=V.getScreen(0);
	
	InitData ();
   System.out.print("\nR_InitData");
   InitPointToAngle ();
   System.out.print("\nR_InitPointToAngle");
   InitTables ();
   // ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
   System.out.print ("\nR_InitTables");

   SetViewSize (screenblocks, detailLevel);
   MyPlanes.InitPlanes ();
   System.out.print ("\nR_InitPlanes");
   InitLightTables ();
   System.out.print("\nR_InitLightTables");
   InitSkyMap ();
   System.out.print("\nR_InitSkyMap");
   InitTranslationTables ();
   System.out.print("\nR_InitTranslationsTables");
   
   framecount = 0;
}
 
  
  
}
