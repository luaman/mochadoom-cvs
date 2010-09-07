package rr;

import static utils.C2JUtils.toUnsignedByte;
import static p.SpriteAnimations.NUMPSPRITES;
import static data.Defines.*;
import static data.Limits.*;

import static p.mobj_t.*;
import static data.SineCosine.finecosine;
import static data.SineCosine.finesine;
import static data.Tables.*;
import static m.fixed_t.*;
import static m.BBox.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import i.system;
import p.LevelLoader;
import p.mobj_t;
import p.pspdef_t;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import w.WadLoader;
import w.name8;
import m.BBox;
import m.fixed_t;
import data.doomstat;
import data.Defines.GameMode_t;
import doom.player_t;

public class UnifiedRenderer {

    private doomstat DS;
    private LevelLoader LL;
    private RendererData RD;
    private WadLoader W;
    private Segs MySegs;
    private BSP MyBSP;
    private Planes MyPlanes;
    private Things MyThings;
    private DoomVideoRenderer V;
    
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

    // Color tables for different players,
    // translate a limited part to another
    // (color ramps used for suit colors).
    //
    byte[][] translations = new byte[3][256];

    //
    // R_DrawColumn
    // Source is the top of the column to scale.
    //
    
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

   /** increment every time a check is made */
   public static int           validcount = 1;     

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
        class cliprange_t {

            public cliprange_t(int first, int last) {
                this.first = first;
                this.last = last;
            }

            public int first;
            public int last;
        }

  /** newend is one past the last valid seg (cliprange_t) */
  int newend;
  cliprange_t[]   solidsegs= new cliprange_t[MAXSEGS];

  /**
   * R_ClipSolidWallSegment
   * Does handle solid walls,
   *  e.g. single sided LineDefs (middle texture)
   *  that entirely block the view.
   * @throws IOException 
   */ 

  public void ClipSolidWallSegment (int   first,
          int   last ) {
      int next;
      int start;

      // Find the first range that touches the range
      //  (adjacent pixels are touching).
      start = 0;
      
      while (solidsegs[start].last < first-1)
      start++;

      if (first < solidsegs[start].first)
      {
      if (last < solidsegs[start].first-1)
      {
          // Post is entirely visible (above start),
          //  so insert a new clippost.
          MySegs.StoreWallRange (first, last);
          next = newend;
          newend++;
          
          while (next != start)
          {
              solidsegs[next] = solidsegs[next-1];
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
          
          crunch:
              if (next == start)
              {
              // Post just extended past the bottom of one post.
              return;
              }
              

              while (next++ != newend)
              {
              // Remove a post.
                  solidsegs[++start] = solidsegs[next];
              }

              newend = start+1;
              return;
      }
      }
      
      // There is a fragment after *next.
      MySegs.StoreWallRange (solidsegs[next].last + 1, last);
      // Adjust the clip size.
      solidsegs[start].last = last;
      
      // Remove start+1 to next from the clip list,
      // because start now covers their area.
    
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
      while (start.last < first-1)
      startptr++;

      start = solidsegs[startptr];
      
      
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
          
      while (last >= solidsegs[startptr+1].first-1)
      {
      // There is a fragment between two posts.
      MySegs.StoreWallRange (start.last + 1, solidsegs[startptr+1].first - 1);
      startptr++;
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
      solidsegs[1].first = DS.viewwidth;
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
      int         x1;
      int         x2;
      int     angle1;
      int     angle2;
      int     span;
      int     tspan;
      
      curline = line;

      // OPTIMIZE: quickly reject orthogonal back sides.
      angle1 = PointToAngle (line.v1.x, line.v1.y);
      angle2 = PointToAngle (line.v2.x, line.v2.y);
      
      // Clip to view edges.
      // OPTIMIZE: make constant out of 2*clipangle (FIELDOFVIEW).
      span = angle1 - angle2;
      
      // Back side? I.e. backface culling?
      if (span >= ANG180)
      return;     

      // Global angle needed by segcalc.
      rw_angle1 = angle1;
      angle1 -= viewangle;
      angle2 -= viewangle;
      
      tspan = angle1 + clipangle;
      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;

      // Totally off the left edge?
      if (tspan >= span)
          return;
      
      angle1 = clipangle;
      }
      tspan = clipangle - angle2;
      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;

      // Totally off the left edge?
      if (tspan >= span)
          return; 
      angle2 = -clipangle;
      }
      
      // The seg is in the view range,
      // but not necessarily visible.
      angle1 = (angle1+ANG90)>>ANGLETOFINESHIFT;
      angle2 = (angle2+ANG90)>>ANGLETOFINESHIFT;
      x1 = viewangletox[angle1];
      x2 = viewangletox[angle2];

      // Does not cross a pixel?
      if (x1 == x2)
      return;             
      
      backsector = line.backsector;

      // Single sided line?
      
      if (backsector==null) {
          ClipSolidWallSegment (x1, x2-1); // to clipsolid
          return;
          }
          

      // Closed door.
      if (backsector.ceilingheight <= frontsector.floorheight
      || backsector.floorheight >= frontsector.ceilingheight) {
          ClipSolidWallSegment (x1, x2-1);; // to clipsolid    
          return;
          }

      // Window.
      if (backsector.ceilingheight != frontsector.ceilingheight
      || backsector.floorheight != frontsector.floorheight) {
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
      int     angle1;
      int     angle2;
      int     span;
      int     tspan;
      
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
      
      span = angle1 - angle2;

      // Sitting on a line?
      if (span >= ANG180)
      return true;
      
      tspan = angle1 + clipangle;

      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;

      // Totally off the left edge?
      if (tspan >= span)
          return false;   

      angle1 = clipangle;
      }
      tspan = clipangle - angle2;
      if (tspan > 2*clipangle)
      {
      tspan -= 2*clipangle;

      // Totally off the left edge?
      if (tspan >= span)
          return false;
      
      angle2 = -clipangle;
      }


      // Find the first clippost
      //  that touches the source post
      //  (adjacent pixels are touching).
      angle1 = (angle1+ANG90)>>ANGLETOFINESHIFT;
      angle2 = (angle2+ANG90)>>ANGLETOFINESHIFT;
      sx1 = viewangletox[angle1];
      sx2 = viewangletox[angle2];

      // Does not cross a pixel.
      if (sx1 == sx2)
      return false;           
      sx2--;
      
      int pstart = 0;
      start=solidsegs[pstart];
      while (start.last < sx2)
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
   * Determine floor/ceiling planes.
   * Add sprites of things in sector.
   * Draw one or more line segments.
 * @throws IOException 
   */
  
  public void Subsector (int num)
  {
      int         count;
      int        line; // pointer into a list of segs
      subsector_t    sub;
      int psub=0;
      
  if (RANGECHECK){
      if (num>=LL.numsubsectors)
      system.Error ("R_Subsector: ss %i with numss = %i",
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
          
      // TODO: it's in THINGS AddSprites (frontsector); 

      while (count-->0)
      {
      AddLine (LL.segs[line]);
      line++;
      }
  }




  /**
   * RenderBSPNode
   * Renders all subsectors below a given node,
   *  traversing subtree recursively.
   * Just call with BSP root.
   */
  public void RenderBSPNode (int bspnum)
  {
      node_t  bsp;
      int     side;

      // Found a subsector?
      if ((bspnum & NF_SUBSECTOR)!=0)
      {
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
      RenderBSPNode (bsp.children[side]); 

      // Possibly divide back space.
      if (CheckBBox (bsp.bbox[side^1].bbox)) 
      RenderBSPNode (bsp.children[side^1]);
  }

  
  }
  
  class Segs{

      public static final String rcsid = "$Id: UnifiedRenderer.java,v 1.4 2010/09/07 16:23:00 velktron Exp $";

      //
      // R_RenderMaskedSegRange
      //
      
      column_t    col=new column_t();
      
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
          texnum = RD.texturetranslation[curline.sidedef.midtexture];
          
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
          dc_texturemid = dc_texturemid + RD.textureheight[texnum] - viewz;
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

              dc_colormap = walllights;
              dco=index;
              }
                  
              sprtopscreen = centeryfrac - FixedMul(dc_texturemid, spryscale);
              dc_iscale = (int) (0xffffffffL / spryscale);
              
              // draw the texture
              col.data = RD.GetColumn(texnum,maskedtexturecol[dc_x]);// -3);
              col.setFromData();
                  
              DrawMaskedColumn (col);
              maskedtexturecol[dc_x] = Short.MAX_VALUE;
          }
          spryscale += rw_scalestep;
          }
          
      }


      protected static final int HEIGHTBITS   =   12;
      protected static final int HEIGHTUNIT   =   (1<<HEIGHTBITS);

      /**
       * R_RenderSegLoop
       * Draws zero, one, or two textures (and possibly a masked
       *  texture) for walls.
       * Can draw or mark the starting pixel of floor and ceiling
       *  textures.
       * CALLED: CORE LOOPING ROUTINE.
     * @throws IOException 
       */
      
      public void RenderSegLoop () 
      {
          int     angle; // angle_t
          int     index;
          int         yl;
          int         yh;
          int         mid;
          int     texturecolumn=0; // fixed_t
          int         top;
          int         bottom;

          //texturecolumn = 0;                // shut up compiler warning
          
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
              angle = (rw_centerangle + xtoviewangle[rw_x])>>ANGLETOFINESHIFT;
              texturecolumn = rw_offset-FixedMul(finetangent[angle],rw_distance);
              texturecolumn >>= FRACBITS;
              // calculate lighting
              index = rw_scale>>LIGHTSCALESHIFT;

              if (index >=  MAXLIGHTSCALE )
              index = MAXLIGHTSCALE-1;

              dc_colormap = walllights;
              dco=index;
              dc_x = rw_x;
              dc_iscale = (int) (0xffffffffL / rw_scale);
          }
          
          // draw the wall tiers
          if (midtexture!=0)
          {
              // single sided line
              dc_yl = yl;
              dc_yh = yh;
              dc_texturemid = rw_midtexturemid;
              dc_source = RD.GetColumn(midtexture,texturecolumn);
              colfunc.invoke();
              ceilingclip[rw_x] = DS.viewheight;
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
                  dc_source = RD.GetColumn(toptexture,texturecolumn);
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
                  dc_source = RD.GetColumn(bottomtexture,
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
          int     distangle, offsetangle; // angle_t
          int     vtop; // fixed_t
          int         lightnum;
          drawseg_t seg;

          // don't overflow and crash
          if (ds_p == MAXDRAWSEGS)
          return;     
              
      if( RANGECHECK){
          if (start >=viewwidth || start > stop)
          system.Error ("Bad R_RenderWallRange: %i to %i", start , stop);
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
          sineval = finesine[distangle>>ANGLETOFINESHIFT];
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
          midtexture = RD.texturetranslation[sidedef.midtexture];
          // a single sided line is terminal, so it must mark ends
          markfloor = markceiling = true;
          if ((linedef.flags & ML_DONTPEGBOTTOM)!=0)
          {
              vtop = frontsector.floorheight +
              RD.textureheight[sidedef.midtexture];
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
              toptexture = RD.texturetranslation[sidedef.toptexture];
              if ((linedef.flags & ML_DONTPEGTOP)!=0)
              {
              // top of texture at top
              rw_toptexturemid = worldtop;
              }
              else
              {
              vtop =
                  backsector.ceilingheight
                  + RD.textureheight[sidedef.toptexture];
              
              // bottom of texture
              rw_toptexturemid = vtop - viewz;    
              }
          }
          if (worldlow > worldbottom)
          {
              // bottom texture
              bottomtexture = RD.texturetranslation[sidedef.bottomtexture];

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
          offsetangle = rw_normalangle-rw_angle1;
          
          if (offsetangle > ANG180)
              offsetangle = -offsetangle;

          if (offsetangle > ANG90)
              offsetangle = ANG90;

          sineval = finesine[offsetangle >>ANGLETOFINESHIFT];
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
          System.arraycopy(ceilingclip, start, openings, lastopening,  2*(rw_stopx-start));
              
          seg.setSprTopClipPointer(lastopening - start);
          lastopening += rw_stopx - start;
          }
          // no floor clipping?
          if ( ((seg.silhouette & SIL_BOTTOM)!=0 || maskedtexture)
           && seg.nullSprBottomClip())
          {
          //memcpy (lastopening, floorclip+start, 2*(rw_stopx-start));
              System.arraycopy(floorclip, start, openings, lastopening,  2*(rw_stopx-start));
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
      byte[]       planezlight;
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
          int angle;
          // 
          int distance;
          int length;
          int index;
          
      if (RANGECHECK){
          if (x2 < x1
          || x1<0
          || x2>=DS.viewwidth
          || y>DS.viewheight)
          {
          system.Error ("R_MapPlane: %i, %i at %i",x1,x2,y);
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
          angle = (viewangle + xtoviewangle[x1])>>ANGLETOFINESHIFT;
          ds_xfrac = viewx + FixedMul(finecosine[angle], length);
          ds_yfrac = -viewy - FixedMul(finesine[angle], length);

          if (fixedcolormap!=null)
          ds_colormap = fixedcolormap;
          else
          {
          index = distance >> LIGHTZSHIFT;
          
          if (index >= MAXLIGHTZ )
              index = MAXLIGHTZ-1;

          ds_colormap = planezlight;
          pds_colormap=index;
          }
          
          ds_y = y;
          ds_x1 = x1;
          ds_x2 = x2;

          // high or low detail
          // TODO:spanfunc.invoke();    
      }


      //
      // R_ClearPlanes
      // At begining of frame.
      //
      public void ClearPlanes ()
      {
          int angle;
          
          // opening / clipping determination
          for (int i=0 ; i<DS.viewwidth ; i++)
          {
          floorclip[i] = DS.viewheight;
          ceilingclip[i] = -1;
          }

          lastvisplane = 0;
          lastopening = openings[0];
          
          // texture calculation
          //memset (cachedheight, 0, sizeof(cachedheight));

          // left to right mapping
          angle = (viewangle-ANG90)>>ANGLETOFINESHIFT;
          
          // scale will be unit scale at SCREENWIDTH/2 distance
          basexscale = FixedDiv (finecosine[angle],centerxfrac);
          baseyscale = -FixedDiv (finesine[angle],centerxfrac);
      }


      /**
       * R_FindPlane
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
          
          if (picnum == DS.skyflatnum)
          {
          height = 0;         // all skys map together
          lightlevel = 0;
          }
          
          for (check=0; check<lastvisplane; check++, chk=visplanes[check])
          {
          if (height == chk.height
              && picnum == chk.picnum
              && lightlevel ==chk.lightlevel)
          {
              break;
          }
          }
          
                  
          if (check < lastvisplane)
          return check;
              
          if (lastvisplane == MAXVISPLANES)
          system.Error ("R_FindPlane: no more visplanes");
              
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
          visplane_t      pln=null; //visplane_t
          int         light;
          int         x;
          int         stop;
          int         angle;
                      
      if (RANGECHECK){
          if (ds_p > MAXDRAWSEGS)
          system.Error("R_DrawPlanes: drawsegs overflow (%i)",
              ds_p );
          
          if (lastvisplane > MAXVISPLANES)
              system.Error(" R_DrawPlanes: visplane overflow (%i)",
               lastvisplane);
          
          if (lastopening  > MAXOPENINGS)
              system.Error( "R_DrawPlanes: opening overflow (%i)",
               lastopening );
      }

          for (int pl = 0 ; pl < lastvisplane ;  pln=visplanes[pl++])
          {
          if (pln.minx > pln.maxx)
              continue;

          
          // sky flat
          if (pln.picnum == DS.skyflatnum)
          {
              dc_iscale = pspriteiscale>>detailshift;
              
              // Sky is allways drawn full bright,
              //  i.e. colormaps[0] is used.
              // Because of this hack, sky is not affected
              //  by INVUL inverse mapping.
              dc_colormap = colormaps;
              dc_texturemid = skytexturemid;
              for (x=pln.minx ; x <= pln.maxx ; x++)
              {
              dc_yl = pln.getTop(x);
              dc_yh = pln.getBottom(x);

              if (dc_yl <= dc_yh)
              {
                  angle = (viewangle + xtoviewangle[x])>>ANGLETOSKYSHIFT;
                  dc_x = x;
                  dc_source = RD.GetColumn(skytexture, angle);
                  colfunc.invoke();
              }
              }
              continue;
          }
          
          // regular flat
          ds_source = ((flat_t)W.CacheLumpNum(RD.firstflat +
                         RD.flattranslation[pln.picnum],
                         PU_STATIC,flat_t.class)).data;
          
          planeheight = Math.abs(pln.height-viewz);
          light = (pln.lightlevel >> LIGHTSEGSHIFT)+extralight;

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

      byte[]  spritelights;

      /** constant arrays
         used for psprite clipping and initializing clipping */
      short[]       negonearray=new short[SCREENWIDTH];
      short[]       screenheightarray=new short[SCREENWIDTH];


      //
      // INITIALIZATION FUNCTIONS
      //

      // variables used to look up
      //  and range check thing_t sprites patches
      spritedef_t[]    sprites;
      int     numsprites;

      spriteframe_t[]   sprtemp=new spriteframe_t[MAX_SPRITEFRAMES];
      int     maxframe;
      String       spritename;

      /**
       * R_InstallSpriteLump
       * Local function for R_InitSprites.
       */
      
      public void
      InstallSpriteLump
      ( int       lump,
        int  frame,
        int  rotation,
        boolean   flipped )
      {
          int     r;
          
          if (frame >= 29 || rotation > 8)
          system.Error("R_InstallSpriteLump: Bad frame characters in lump %i", lump);
          
          if ((int)frame > maxframe)
          maxframe = frame;
              
          if (rotation == 0)
          {
          // the lump should be used for all rotations
          if (sprtemp[frame].rotate == false)
              system.Error ("R_InitSprites: Sprite %s frame %c has multiple rot=0 lump", spritename, 'A'+frame);

          if (sprtemp[frame].rotate == true)
              system.Error ("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump", spritename, 'A'+frame);
                  
          sprtemp[frame].rotate = false;
          for (r=0 ; r<8 ; r++)
          {
              sprtemp[frame].lump[r] = (short) (lump - RD.firstspritelump);
              sprtemp[frame].flip[r] = (byte) (flipped?1:0);
          }
          return;
          }
          
          // the lump is only used for one rotation
          if (sprtemp[frame].rotate == false)
              system.Error  ("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump", spritename, 'A'+frame);
              
          sprtemp[frame].rotate = true;

          // make 0 based
          rotation--;     
          if (sprtemp[frame].lump[rotation] != -1)
              system.Error  ("R_InitSprites: Sprite %s : %c : %c has two lumps mapped to it",
               spritename, 'A'+frame, '1'+rotation);
              
          sprtemp[frame].lump[rotation] = (short) (lump - RD.firstspritelump);
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
          
          start = RD.firstspritelump-1;
          end = RD.lastspritelump+1;
          
          // scan all the lump names for each of the names,
          //  noting the highest frame letter.
          // Just compare 4 characters as ints
          for (int i=0 ; i<numsprites ; i++)
          {
          spritename = namelist[i];
          
          // FIXME: memset (sprtemp,-1, sizeof(sprtemp));
          for (int j=0;j<sprtemp.length;j++){
              Arrays.fill(sprtemp[j].flip,(byte)-1);
              Arrays.fill(sprtemp[j].lump,(short)-1);
              sprtemp[1].lrotate=-1;
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
              
              if (W.lumpinfo[l].intname == intname)
              {
              frame = cname[4] - 'A';
              rotation = cname[5] - '0';

              if (DS.modifiedgame)
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
          
          // check the frames that were found for completeness
          if (maxframe == -1)
          {
              sprites[i].numframes = 0;
              continue;
          }
              
          maxframe++;
          
          for (frame = 0 ; frame < maxframe ; frame++)
          {
              switch ((int)sprtemp[frame].lrotate)
              {
                case -1:
              // no rotations were found for that frame at all
              system.Error ("R_InitSprites: No patches found for %s frame %c", namelist[i], frame+'A');
              break;
              
                case 0:
              // only the first rotation is needed
              break;
                  
                case 1:
              // must have all 8 frames
              for (rotation=0 ; rotation<8 ; rotation++)
                  if (sprtemp[frame].lump[rotation] == -1)
                  system.Error ("R_InitSprites: Sprite %s frame %c is missing rotations",
                       namelist[i], frame+'A');
              break;
              }
          }
          
          // allocate space for the frames present and copy sprtemp to it
          //  MAES: we can do that elegantly in one line.
          
          sprites[i]=new spritedef_t(sprtemp,maxframe);
          
          
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
      vissprite_t[] vissprites=new vissprite_t[MAXVISSPRITES];
      int    vissprite_p;
      int     newvissprite;



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
          
          InitSpriteDefs (namelist);
      }



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

      //
      // R_DrawVisSprite
      //  mfloorclip and mceilingclip should also be set.
      //
      public void
      DrawVisSprite
      ( vissprite_t      vis,
        int           x1,
        int           x2 )
      {
          column_t       column;
          int         texturecolumn;
          int     frac; // fixed_t
          patch_t        patch;
          
          
          patch = W.CachePatchNum (vis.patch+RD.firstspritelump);

          dc_colormap = vis.colormap;
          
          if (dc_colormap==null)
          {
          // NULL colormap = shadow draw
          shadow=true;
          //colfunc = fuzzcolfunc;
          }
          else if ((vis.mobjflags & MF_TRANSLATION)!=0)
          {
          shadow=false;
          //colfunc = R_DrawTranslatedColumn;
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
              system.Error ("R_DrawSpriteRange: bad texturecolumn");
      }
          column = patch.columns[texturecolumn];
          DrawMaskedColumn (column);
          }

          colfunc = basecolfunc;
      }



      //
      // R_ProjectSprite
      // Generates a vissprite for a thing
      //  if it might be visible.
      //
      public void ProjectSprite (mobj_t thing)
      {
          int     tr_x;
          int     tr_y;
          
          int     gxt;
          int     gyt;
          
          int     tx;
          int     tz;

          int     xscale;
          
          int         x1;
          int         x2;

          spritedef_t    sprdef;
          spriteframe_t  sprframe;
          int         lump;
          
          int        rot;
          boolean     flip;
          
          int         index;

          vissprite_t    vis;
          
          int     ang;
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
          system.Error ("R_ProjectSprite: invalid sprite number %i ",
               thing.sprite);
      }
          sprdef = sprites[thing.sprite.ordinal()];
      if(RANGECHECK){
          if ( (thing.frame&FF_FRAMEMASK) >= sprdef.numframes )
          system.Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
               thing.sprite, thing.frame);
      }
          sprframe = sprdef.spriteframes[ thing.frame & FF_FRAMEMASK];

          if (sprframe.rotate)
          {
          // choose a different rotation based on player view
          ang = PointToAngle (thing.x, thing.y);
          rot = (ang-thing.angle+(ANG45/2)*9)>>29;
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
          tx -= RD.spriteoffset[lump];   
          x1 = (centerxfrac + FixedMul (tx,xscale) ) >>FRACBITS;

          // off the right side?
          if (x1 > viewwidth)
          return;
          
          tx +=  RD.spritewidth[lump];
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
          vis.gzt = thing.z + RD.spritetopoffset[lump];
          vis.texturemid = vis.gzt - viewz;
          vis.x1 = x1 < 0 ? 0 : x1;
          vis.x2 = x2 >= viewwidth ? viewwidth-1 : x2;   
          iscale = FixedDiv (FRACUNIT, xscale);

          if (flip)
          {
          vis.startfrac = RD.spritewidth[lump]-1;
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
          vis.pcolormap=0;
          }
          else if ((thing.frame & FF_FULLBRIGHT)!=0)
          {
          // full bright
          vis.colormap = colormaps;
          vis.pcolormap=0;
          }
          
          else
          {
          // diminished light
          index = xscale>>(LIGHTSCALESHIFT-detailshift);

          if (index >= MAXLIGHTSCALE) 
              index = MAXLIGHTSCALE-1;

          vis.colormap = spritelights;
          vis.pcolormap=index;
          }   
      }




      /**
       * R_AddSprites
       * During BSP traversal, this adds sprites by sector.
       */
      public void AddSprites (sector_t sec)
      {
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
              // TODO: these are probably meant to be array aliases/arrays in the middle of other arrays.
          spritelights = scalelight[0];
          else if (lightnum >= LIGHTLEVELS)
          spritelights = scalelight[LIGHTLEVELS-1];
          else
          spritelights = scalelight[lightnum];

          // Handle all things in sector.
          for (thing = sec.thinglist ; thing!=null ; thing = (mobj_t) thing.getSNext())
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
          system.Error ("R_ProjectSprite: invalid sprite number %i ",
               psp.state.sprite);
          }
          sprdef = sprites[psp.state.sprite.ordinal()];
      if(RANGECHECK){
          if ( (psp.state.frame & FF_FRAMEMASK)  >= sprdef.numframes)
          system.Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
               psp.state.sprite, psp.state.frame);
      }
          sprframe = sprdef.spriteframes[psp.state.frame & FF_FRAMEMASK ];

          lump = sprframe.lump[0];
          flip = (boolean)(sprframe.flip[0]!=0);
          
          // calculate edges of the shape
          tx = psp.sx-160*FRACUNIT;
          
          tx -= RD.spriteoffset[lump];   
          x1 = (centerxfrac + FixedMul (tx,pspritescale) ) >>FRACBITS;

          // off the right side
          if (x1 > viewwidth)
          return;     

          tx +=  RD.spritewidth[lump];
          x2 = ((centerxfrac + FixedMul (tx, pspritescale) ) >>FRACBITS) - 1;

          // off the left side
          if (x2 < 0)
          return;
          
          // store information in a vissprite
          //vis = avis;
          vis.mobjflags = 0;
          vis.texturemid = (BASEYCENTER<<FRACBITS)+FRACUNIT/2-(psp.sy-RD.spritetopoffset[lump]);
          vis.x1 = x1 < 0 ? 0 : x1;
          vis.x2 = x2 >= viewwidth ? viewwidth-1 : x2;   
          vis.scale = pspritescale<<detailshift;
          
          if (flip)
          {
          vis.xiscale = -pspriteiscale;
          vis.startfrac = RD.spritewidth[lump]-1;
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
          vis.pcolormap=0;
          }
          else if ((psp.state.frame & FF_FULLBRIGHT)!=0)
          {
          // full bright
          vis.colormap = colormaps;
          vis.pcolormap=0;
          }
          else
          {
          // local light
          vis.colormap = spritelights;
          vis.pcolormap=MAXLIGHTSCALE-1;
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
      vissprite_t     unsorted=new vissprite_t();
      
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
          for (int ds=0 ; ds<vissprite_p ; ds++)
          {
              vissprites[ds].next = vissprites[ds+1];
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
                  clipbot[x] = dss.getSprBottomClip(x);
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
  // R_InitPointToAngle
  //
  public void InitPointToAngle ()
  {
      // UNUSED - now getting from tables.c
  if (false){
      int i;
      long    t;
      float   f;
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
          t = DS.viewwidth+1;
      else
      {
          t = FixedMul (finetangent[i], focallength);
          t = (centerxfrac - t+FRACUNIT-1)>>FRACBITS;

          if (t < -1)
          t = -1;
          else if (t>DS.viewwidth+1)
          t = DS.viewwidth+1;
      }
      viewangletox[i] = t;
      }
      
      // Scan viewangletox[] to generate xtoviewangle[]:
      //  xtoviewangle will give the smallest view angle
      //  that maps to x. 
      for (x=0;x<=DS.viewwidth;x++)
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
      else if (viewangletox[i] == DS.viewwidth+1)
          viewangletox[i]  = DS.viewwidth;
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

          zlight[i][j] = RD.colormaps[level*256];
      }
      }
  }
  
    
  /**
   * A column is a vertical slice/span from a wall texture that, given the
   * DOOM style restrictions on the view orientation, will always have
   * constant z depth. Thus a special case loop for very fast rendering can be
   * used. It has also been used with Wolfenstein 3D. MAES: this is called
   * mostly from inside Draw and from an external "Renderer"
   */

  public void DrawColumn() {
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
  
  
  public void DrawColumnLow() {
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

/**
 * Framebuffer postprocessing.
 * Creates a fuzzy image by copying pixels
 * from adjacent ones to left and right.
 * Used with an all black colormap, this
 * could create the SHADOW effect,
 * i.e. spectres and invisible players.
 */
  
public void DrawFuzzColumn () 
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
  
/** use paired with dcto */
byte[] dc_translation;
/** DC Translation offset */
int dcto;

/** used paired with tto */
byte[] translationtables;
/** translation tables offset */
int tto;

public void DrawTranslatedColumn() {
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
            DrawFuzzColumn();
        } else {
            DrawColumn();
        }
        
        // colfunc (); 
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
            DrawFuzzColumn();
        } else {
            DrawColumn();
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
 *
 *   LFB copy.
 *   This might not be a good idea if memcpy
 *   is not optiomal, e.g. byte by byte on
 *   a 32bit CPU, as GNU GCC/Linux libc did
 *    at one point.
 */
public void VideoErase(int ofs, int count) {

    // memcpy (screens[0]+ofs, screens[1]+ofs, count);
    System.arraycopy(V.getScreen(0), ofs, V.getScreen(1), ofs, count);

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
  * Fills the back screen with a pattern
  * for variable screen sizes
  * Also draws a beveled edge.
  */
 
 public void FillBackScreen() {
     byte[] src;
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

     if (DS.gamemode == GameMode_t.commercial)
         name = name2;
     else
         name = name1;

     // MAES: do a RAW get here? :-S
     src = W.CacheLumpName(name, PU_CACHE).getBuffer().array();
     dest = V.getScreen(1);
     int destPos = 0;

     for (y = 0; y < SCREENHEIGHT - SBARHEIGHT; y++) {
         for (x = 0; x < SCREENWIDTH / 64; x++) {
             // memcpy (dest, src+((y&63)<<6), 64);
             System.arraycopy(src, ((y & 63) << 6), dest, destPos, 64);
             destPos += 64;
         }

         if ((SCREENWIDTH & 63) != 0) {
             // memcpy (dest, src+((y&63)<<6), SCREENWIDTH&63);
             System.arraycopy(src, ((y & 63) << 6), dest, destPos,
                 SCREENWIDTH & 63);

             destPos += (SCREENWIDTH & 63);
         }
     }

     patch = (patch_t) W.CacheLumpName("brdr_t", PU_CACHE, patch_t.class);

     for (x = 0; x < scaledviewwidth; x += 8)
         V.DrawPatch(viewwindowx + x, viewwindowy - 8, 1, patch);
     patch = (patch_t) W.CacheLumpName("brdr_b", PU_CACHE, patch_t.class);

     for (x = 0; x < scaledviewwidth; x += 8)
         V.DrawPatch(viewwindowx + x, viewwindowy + viewheight, 1, patch);
     patch = (patch_t) W.CacheLumpName("brdr_l", PU_CACHE, patch_t.class);

     for (y = 0; y < viewheight; y += 8)
         V.DrawPatch(viewwindowx - 8, viewwindowy + y, 1, patch);
     patch = (patch_t) W.CacheLumpName("brdr_r", PU_CACHE, patch_t.class);

     for (y = 0; y < viewheight; y += 8)
         V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy + y, 1,
             patch);

     // Draw beveled edge. Top-left
     V.DrawPatch(viewwindowx - 8, viewwindowy - 8, 1, (patch_t) W
             .CacheLumpName("brdr_tl", PU_CACHE, patch_t.class));

     // Top-right.
     V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy - 8, 1,
         (patch_t) W.CacheLumpName("brdr_tr", PU_CACHE, patch_t.class));

     // Bottom-left
     V.DrawPatch(viewwindowx - 8, viewwindowy + viewheight, 1,
         (patch_t) W.CacheLumpName("brdr_bl", PU_CACHE, patch_t.class));
     // Bottom-right.
     V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy + viewheight, 1,
         (patch_t) W.CacheLumpName("brdr_br", PU_CACHE, patch_t.class));
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

     // Samw with base row offset.
     if (width == SCREENWIDTH)
         viewwindowy = 0;
     else
         viewwindowy = (SCREENHEIGHT - SBARHEIGHT - height) >> 1;

     // Preclaculate all row offsets.
     for (i = 0; i < height; i++)
         ylookup[i] = /* screens[0] + */(i + viewwindowy) * SCREENWIDTH;
 }
 
 /**
  * Again..
  */
 
 public void DrawSpanLow() {
     int f_xfrac;
     int f_yfrac;
     int dest;
     int count;
     int spot;

     if (RANGECHECK) {
         if ((ds_x2 < ds_x1) || (ds_x1 < 0) || ds_x2 >= SCREENWIDTH
                 || ds_y > SCREENHEIGHT) {
             system.Error("R_DrawSpan: %i to %i at %i", ds_x1, ds_x2, ds_y);
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
         screen[dest++] = ds_colormap[pds_colormap+ds_source[pds_source+spot]];
         screen[dest++] = ds_colormap[pds_colormap+ds_source[pds_source+spot]];

         f_xfrac += ds_xstep;
         f_yfrac += ds_ystep;

     } while (count-- != 0);
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
         screen[dest] = colormap[source[spot]];
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest+1] = colormap[source[spot]];
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest+2] = colormap[source[spot]];
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest+3] = colormap[source[spot]];
         count -= 4;
         dest += 4;
     }
     
     while (count > 0) {
         ytemp = position >> 4;
         ytemp = ytemp & 4032;
         xtemp = position >> 26;
         spot = xtemp | ytemp;
         position += step;
         screen[dest++] = colormap[source[spot]];
         count--;
     }
 }
 
 /**
  * Draws the actual span.
  */
 public void DrawSpan() {
     int f_xfrac; // fixed_t
     int f_yfrac; // fixed_t
     int dest;
     int count;
     int spot;

     if (RANGECHECK) {
         if (ds_x2 < ds_x1 || ds_x1 < 0 || ds_x2 >= SCREENWIDTH
                 || ds_y > SCREENHEIGHT) {
             system.Error("R_DrawSpan: %i to %i at %i", ds_x1, ds_x2, ds_y);
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
         screen[dest++] = ds_colormap[pds_colormap+ds_source[pds_source+spot]];

         // Next step in u,v.
         f_xfrac += ds_xstep;
         f_yfrac += ds_ystep;

     } while (count-- != 0);
 }


///////////////////////// The actual rendering calls ///////////////////////
 
 //
//R_RenderView
//
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
 
 viewsin = finesine[viewangle>>ANGLETOFINESHIFT];
 viewcos = finecosine[viewangle>>ANGLETOFINESHIFT];
 
 sscount = 0;
 
 if (player.fixedcolormap!=0)
 {
 fixedcolormap =colormaps;
 // Offset by fixedcolomap
 pfixedcolormap =player.fixedcolormap*256;
 
 walllights = scalelightfixed;

 for (i=0 ; i<MAXLIGHTSCALE ; i++)
     scalelightfixed[i] = fixedcolormap[pfixedcolormap];
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

public boolean      setsizeneeded;
int     setblocks;
int     setdetail;


public void SetViewSize
( int       blocks,
int       detail )
{
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
    int viewheight=DS.viewheight;
    
    setsizeneeded = false;

    
    
    if (setblocks == 11)
    {
    DS.scaledviewwidth = SCREENWIDTH;
    viewheight = SCREENHEIGHT;
    }
    else
    {
        DS.scaledviewwidth = setblocks*32;
        viewheight = (short) ((setblocks*168/10)&~7);
    }
    
    detailshift = setdetail;
    viewwidth = DS.scaledviewwidth>>detailshift;
    
    centery = viewheight/2;
    centerx = viewwidth/2;
    centerxfrac=(centerx<<FRACBITS);
    centeryfrac=(centery<<FRACBITS);
    projection=centerxfrac;

    
    if (detailshift!=0)
    {
        /* TODO:
    colfunc = basecolfunc = Draw.class.getDeclaredMethod("DrawColumn", Void.class);
    fuzzcolfunc = Draw.class.getDeclaredMethod("DrawFuzzColumn", Void.class);
    transcolfunc = Draw.class.getDeclaredMethod("DrawTranslatedColumn", Void.class);
    spanfunc = Draw.class.getDeclaredMethod("DrawSpan;", Void.class);
    */
    }
    else
    {/*
    colfunc = basecolfunc = Draw.class.getDeclaredMethod("DrawColumnLow",Void.class);
    fuzzcolfunc = Draw.class.getDeclaredMethod("DrawFuzzColumn",Void.class);
    transcolfunc = Draw.class.getDeclaredMethod("DrawTranslatedColumn",Void.class);
    spanfunc = Draw.class.getDeclaredMethod("DrawSpanLow",Void.class);
    */
    }

    InitBuffer (DS.scaledviewwidth, DS.viewheight);
    
    InitTextureMapping ();
    
    // psprite scales
    pspritescale=(FRACUNIT*DS.viewwidth/SCREENWIDTH);
    pspriteiscale=(FRACUNIT*SCREENWIDTH/DS.viewwidth);
    
    // thing clipping
    for (i=0 ; i<DS.viewwidth ; i++)
    screenheightarray[i] = (short) DS.viewheight;
    
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
    cosadj = Math.abs(finecosine[xtoviewangle[i]>>ANGLETOFINESHIFT]);
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

        scalelight[i][j] = colormaps[level*256];
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
    skyflatnum = RD.FlatNumForName ( SKYFLATNAME );
    skytexturemid = 100*FRACUNIT;
}


  interface colfunc_t {
      public void invoke();
  }
 
  
  
}
