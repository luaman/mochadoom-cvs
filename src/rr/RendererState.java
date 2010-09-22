package rr;

import static data.Defines.RANGECHECK;
import static data.Defines.SCREENHEIGHT;
import static data.Defines.SCREENWIDTH;
import static data.Limits.MAXHEIGHT;
import static data.Limits.MAXOPENINGS;
import static data.Limits.MAXVISPLANES;
import static data.Limits.MAXWIDTH;
import static data.SineCosine.finesine;
import static data.Tables.ANG180;
import static data.Tables.ANG270;
import static data.Tables.ANG90;
import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.DBITS;
import static data.Tables.FINEANGLES;
import static data.Tables.SLOPERANGE;
import static data.Tables.SlopeDiv;
import static data.Tables.tantoangle;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import i.system;
import m.fixed_t;
import rr.UnifiedRenderer.colfunc_t;
import doom.player_t;

public abstract class RendererState {

    
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
    
    /** "peg" this to the one from RendererData */
    byte[] colormaps;
    
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

    /** lighttable_t */
    byte[]   walllights;

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
    
    //////////////////////////////////From r_sky.c /////////////////////////////////////

    int skyflatnum;
    int skytexture;
    int skytexturemid;
    
    ///// FROM R_DRAW //////////
    
    byte[] screen;

    // status bar height at bottom of screen
    public static final int SBARHEIGHT = 32;

    private static final boolean RANGECHECK = false;


    byte[] viewimage;

    int viewwidth;

    int scaledviewwidth;

    int viewheight;

    int viewwindowx;

    int viewwindowy;

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

   /** fixed_t */
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

   // 0 = high, 1 = low
   public int          detailshift;    

   //
   // precalculated math tables
   //
   public static long           clipangle;

   // The viewangletox[viewangle + FINEANGLES/4] lookup
   // maps the visible view angles to screen X coordinates,
   // flattening the arc to a flat projection plane.
   // There will be many angles mapped to the same X. 

   public static final int[]         viewangletox=new int[FINEANGLES/2];

   /** The xtoviewangleangle[] table maps a screen pixel
    * to the lowest viewangle that maps back to x ranges
    * from clipangle to -clipangle. */
   public static final long[]         xtoviewangle=new long[SCREENWIDTH+1];


   // UNUSED.
   // The finetangentgent[angle+FINEANGLES/4] table
   // holds the fixed_t tangent values for view angles,
   // ranging from MININT to 0 to MAXINT.
   // fixed_t      finetangent[FINEANGLES/2];

   // fixed_t      finesine[5*FINEANGLES/4];
   // MAES: uh oh. So now all these ints must become finesines? fuck that.
   // Also wtf @ this hack....this points to approx 1/4th of the finesine table, but what happens if I read past it?
   // int[]        finecosine = finesine[FINEANGLES/4];

   // TODO:
   public byte[][]     scalelight=new byte[LIGHTLEVELS][MAXLIGHTSCALE];
   public byte[]       scalelightfixed=new byte[MAXLIGHTSCALE];
   public byte[][]     zlight=new byte[LIGHTLEVELS][MAXLIGHTZ];

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
                   system
                           .Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
                               dc_x);
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
                   dc_colormap[dc_translation[dc_source[dc_source_ofs+(frac >> FRACBITS)]]];
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
               system
                       .Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
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
           // Re-map color indices from wall texture column
           // using a lighting/special effects LUT.
           // TODO: determine WHERE the fuck "*dest" is supposed to be
           // pointing.
           // DONE: it's pointing inside screen[0] (implicitly).
           // dc_source was probably just a pointer to a decompressed
           // column...right? Right.
           screen[dest] = (byte) dc_colormap[dc_source[dc_source_ofs+((frac >> FRACBITS) & 127)]];

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

               system
                       .Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
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

       do {
           // Hack. Does not work correctly.
           // MAES: that's good to know.
           screen[dest2] =
               screen[dest] =
                   dc_colormap[dc_source[dc_source_ofs+(frac >> FRACBITS) & 127]];
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
    system.Error ("R_DrawFuzzColumn: %i to %i at %i",
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
    screen[dest] = colormaps[6*256+screen[dest+fuzzoffset[fuzzpos]]]; 

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

   /** variables used to look up
   * and range check thing_t sprites patches
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

   /** DrawSpan colormap. Use along with dso */
   byte[] ds_colormap;
   /** pointer into colormap */
   int pds_colormap; // its index.

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
   ( fixed_t	x1,
     fixed_t	y1,
     fixed_t	x2,
     fixed_t	y2 )
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
   ( fixed_t	x,
     fixed_t	y )
   {
       int		angle;
       int	dx;
       int	dy;
       int	temp;
       int	dist;
   	
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
       int	i;
       long	t;
       float	f;
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
           return ANG90-1-tantoangle[ SlopeDiv(x,y)];
           }
       }
       else
       {
           // y<0
           y = -y;

           if (x>y)
           {
           // octant 8
           return -tantoangle[SlopeDiv(y,x)];
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
           return ANG180-1-tantoangle[ SlopeDiv(y,x)];
           }
           else
           {
           // octant 2
           return ANG90+ tantoangle[ SlopeDiv(x,y)];
           }
       }
       else
       {
           // y<0
           y = -y;

           if (x>y)
           {
           // octant 4
           return ANG180+tantoangle[ SlopeDiv(y,x)];
           }
           else
           {
            // octant 5
           return ANG270-1-tantoangle[ SlopeDiv(x,y)];
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
   

   
}
