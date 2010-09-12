package rr;

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
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import m.fixed_t;
import rr.UnifiedRenderer.colfunc_t;
import doom.player_t;

public abstract class RendererState {

    
    ///////////////// COMMON RENDERING GLOBALS ////////////////
    
    /** "peg" this to the one from RendererData */
    byte[] colormaps;
    
    //// FROM SEGS ////
    /** angle_t */
    int     rw_normalangle;
    
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
    int     rw_angle1;  

    //
    // regular wall
    //
    int     rw_x;
    int     rw_stopx;
    int     rw_centerangle; // angle_t
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
    public static int LIGHTLEVELS=16;
    public static int LIGHTSEGSHIFT=4;

    public static int MAXLIGHTSCALE=       48;
    public static int LIGHTSCALESHIFT =    12;
    public static int MAXLIGHTZ      =    128;
    public static int LIGHTZSHIFT    = 20;

   /** Fineangles in the SCREENWIDTH wide window. */
   public static int FIELDOFVIEW   =   2048;   
   public static int           viewangleoffset;

   /** Increment every time a check is made 
    *  For some reason, this needs to be visible even by enemies thinking :-S*/
   public int           validcount = 1;     

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
   public int  viewangle;

   /** fixed */
   public int          viewcos,viewsin;

   public player_t     viewplayer;

   // 0 = high, 1 = low
   public int          detailshift;    

   //
   // precalculated math tables
   //
   public static int           clipangle;

   // The viewangletox[viewangle + FINEANGLES/4] lookup
   // maps the visible view angles to screen X coordinates,
   // flattening the arc to a flat projection plane.
   // There will be many angles mapped to the same X. 

   public static int[]         viewangletox=new int[FINEANGLES/2];

   // The xtoviewangleangle[] table maps a screen pixel
   // to the lowest viewangle that maps back to x ranges
   // from clipangle to -clipangle.
   public static int[]         xtoviewangle=new int[SCREENWIDTH+1];


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
   
   
   colfunc_t colfunc;
   colfunc_t basecolfunc;
   colfunc_t fuzzcolfunc;
   colfunc_t transcolfunc;
   colfunc_t spanfunc;

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
   spritedef_t[]   sprites;
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

   
   ////////////// SOME UTILITY METHODS, THAT DON'T DEPEND ON STATE /////////////

   public int
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

   public int
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
   	
       angle = (tantoangle[ FixedDiv(dy,dx)>>DBITS ]+ANG90) >> ANGLETOFINESHIFT;

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

   public int
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
   public int ScaleFromGlobalAngle (int visangle)
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

       anglea = ANG90 + (visangle-viewangle);
       angleb = ANG90 + (visangle-rw_normalangle);

       // both sines are allways positive
       sinea = finesine[anglea>>ANGLETOFINESHIFT]; 
       sineb = finesine[angleb>>ANGLETOFINESHIFT];
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
       
       angle = (tantoangle[ FixedDiv(dy,dx)>>DBITS ]+ANG90) >> ANGLETOFINESHIFT;

       // use as cosine
       dist = FixedDiv (dx, finesine[angle] ); 
       
       return dist;
   }
   
}
