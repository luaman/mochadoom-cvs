package rr;

import static data.Defines.*;
import static data.Limits.*;
import static data.Tables.*;
import static m.fixed_t.*;
import static m.BBox.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import p.mobj_t;
import st.StatusBar;
import utils.C2JUtils;
import w.DoomBuffer;
import data.Defines;
import data.Tables;
import doom.DoomMain;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;

public class ParallelRenderer extends RendererState  {
	///// PARALLEL OBJECTS /////
	private final static int NUMWALLTHREADS=2;
	private final static int NUMFLOORTHREADS=1;
	private Executor tp;
	private VisplaneWorker[] vpw;
	
	private CyclicBarrier visplanebarrier;
	
	/** Stored wall rendering instructions. They can be at most 3*SCREENWIDTH
	 * (if there are low, mid and high textures on every column of the screen)
	 * Remember to init them and set screen and ylookup for all of them.
	 * 
	 * Their max number is static and work partitioning can be done in any way,
	 * as long as you keep track of how many there are in any given frame.
	 * This value is stored inside RWIcount. 
	 * 
	 * TODO: there are cases when more than 3*SCREENWIDTH instructions need to be stored.
	 *       therefore we really need a resizeable array here, but ArrayList is way too
	 *       slow for our needs. Storing a whole wall is not an option, as,
	 *       once again, a wall may have a variable number of columns and an irregular
	 *       height profile -> we'd need to look into visplanes ugh...
	 *       
	 * 
	 */
		
	private RenderWallInstruction[] RWI=new RenderWallInstruction[3*SCREENWIDTH];
	
	/** Increment this as you submit RWI to the "queue". Remember to reset to 0 when you have drawn everything!
	 * 
	 */
	private int RWIcount=0;
	
	private RenderWallExecutor[] RWIExec=new RenderWallExecutor[NUMWALLTHREADS];
	

	
    private static final boolean DEBUG=false;
    private static final boolean DEBUG2=false;
    // HACK: An all zeroes array used for fast clearing of certain visplanes.
    private static int[]           BLANKCACHEDHEIGHT=new int[SCREENHEIGHT];
    
    
    public ParallelRenderer(DoomMain DM) {
      
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
      DrawSpan=new R_DrawSpanUnrolled();
      DrawSpanLow=new R_DrawSpanLow();
      DrawTranslatedColumn=new R_DrawTranslatedColumn();
      DrawTLColumn=new R_DrawTLColumn();
      DrawFuzzColumn=new R_DrawFuzzColumn();
      DrawColumn=new R_DrawColumnBoom();//new R_DrawColumnBoom();
      DrawColumnPlayer=new R_DrawColumn();
      DrawColumnLow=DrawColumn;
      
      
      // Prepare parallel stuff
      tp=   Executors.newFixedThreadPool(NUMWALLTHREADS+NUMFLOORTHREADS);
      // Prepare the barrier for MAXTHREADS + main thread.
      visplanebarrier=new CyclicBarrier(NUMWALLTHREADS+NUMFLOORTHREADS+1);
      
      vpw=new VisplaneWorker[NUMFLOORTHREADS];
      
      for (int i=0;i<NUMFLOORTHREADS;i++){
      vpw[i]=new VisplaneWorker(visplanebarrier);
      vpw[i].id=i;
      

      }
      
      
  }
   
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
      

  /**
   * R_ClearDrawSegs
   * 
   * The drawseg list is reset by pointing back at 0.
   * 
   */
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
   * 
   * Does handle solid walls, single sided LineDefs (middle texture)
   * that entirely block the view VERTICALLY.
   *  
   *  Handles "clipranges" for a solid wall, aka
   *  where it blocks the view.
   *  
   * @param first starting y coord?  
   * @param last ending y coord?
   */ 

  private void ClipSolidWallSegment (int   first,
          int   last ) {

      int next;
      int start;
     // int maxlast=Integer.MIN_VALUE;
      
      start = 0; // within solidsegs

      // Find the first cliprange that touches the range.
      // Actually, the first one not completely hiding it (its last must be lower than first.

      while (solidsegs[start].last < first-1)
      start++;
      
      // If the post begins above the lastly found cliprange...
      if (first < solidsegs[start].first)
      {
      // ..and ends above it, too (no overlapping)
      if (last < solidsegs[start].first-1)
      {
          // ... then the post is entirely visible (above start),
          //  so insert a new clippost. Calling this function 
          // tells the renderer that there is an obstruction.
          // TODO: determine WHERE this info is actually saved.
          MySegs.StoreWallRange (first, last);
          
          // Newend should have a value of 2 if we are at the beginning of a new frame.
          next = newend;
          newend++;
          
          while (next != start)
          {
           // *next=*(next-1);
          /*  MAES: I think this is supposed to copy the structs
           * 
           * solidsegs[next] = solidsegs[next-1].clone();
           *
           * OK, so basically the last solidseg copies its previous,
           * and so on until we reach the start. This means that at some
           * point, the value of the start solidseg is duplicated.
           */
              
              solidsegs[next].copy(solidsegs[next-1]);
              
          next--;
          }
          
          // At this point, next points at start.
          // Therefore, start
          solidsegs[next].first = first;
          solidsegs[next].last = last;
          return;
      }
          
      // There is a fragment above *start. This can occur if it a
      // post does start before another, but its lower edge overlaps (partial, upper occlusion)
      MySegs.StoreWallRange (first, solidsegs[start].first - 1);
      // Now adjust the clip size.
      solidsegs[start].first = first; 
      }

      // We can reach this only if a post starts AFTER another 

      // Bottom contained in start? Obviously it won't be visible.
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
  private void  ClipPassWallSegment (int   first,
          int   last ) {
     
      // Find the first range that touches the range
      //  (adjacent pixels are touching).
      int start=0;
      
      while (solidsegs[start].last < first-1)
      start++;
      
      
      if (first < solidsegs[start].first)
      {
      if (last < solidsegs[start].first-1)
      {
          // Post is entirely visible (above start).
          MySegs.StoreWallRange (first, last);
          return;
      }
          
      // There is a fragment above *start.
      MySegs.StoreWallRange (first, solidsegs[start].first - 1);
      }

      // Bottom contained in start?
      if (last <= solidsegs[start].last)
      return;         
       
      //MAES: Java absolutely can't do without a sanity check here.
      //if (startptr>=MAXSEGS-2) return;
      
      while (last >= solidsegs[start+1].first-1)
      {
      // There is a fragment between two posts.
      MySegs.StoreWallRange (solidsegs[start].last + 1, solidsegs[start+1].first - 1);
      start++;
      //if (startptr>=MAXSEGS-2) return;
      //start=solidsegs[startptr];
      
      if (last <= solidsegs[start].last)
          return;
      }
      
      // There is a fragment after *next.
      MySegs.StoreWallRange (solidsegs[start].last + 1, last);
  }



  /**
   * R_ClearClipSegs
   * 
   * Clears the clipping segs list. The list is actually fixed size
   * for efficiency reasons, so it just tells Doom to use the first
   * two solidsegs, which are "neutered". It's interesting to note
   * how the solidsegs begin and end just "outside" the visible borders of
   * the screen. 
   * 
   * 
   */

  private void ClearClipSegs ()
  {
      solidsegs[0].first = -0x7fffffff;
      solidsegs[0].last = -1;
      solidsegs[1].first = viewwidth;
      solidsegs[1].last = 0x7fffffff;
      newend = 2; // point so solidsegs[2];
  }

  /**
   * R_AddLine
   * 
   *  Called after a SubSector BSP trasversal ends up in a "final" subsector.
   *  
   *  Clips the given segment and adds any visible pieces to the line list.
   *  It also determines what kind of boundary (line) visplane clipping
   *  should be performed. E.g. window, final 1-sided line, closed door etc.)
   *  CAREFUL: was the source of much frustration with visplanes...
   *  
   */
  private void AddLine (seg_t  line) 
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
      angle1 = PointToAngle (line.v1x, line.v1y);
      angle2 = PointToAngle (line.v2x, line.v2y);
      
      // Clip to view edges.
      // OPTIMIZE: make constant out of 2*clipangle (FIELDOFVIEW).
            
      span = addAngles(angle1,- angle2);
      
      // Back side? I.e. backface culling?
      if (span >= ANG180)
      return;     

      // Global angle needed by segcalc.
      rw_angle1 = angle1;
      angle1 -= viewangle;
      angle2 -= viewangle;
      
      angle1&=BITS32;
      angle2&=BITS32;
      
      tspan = addAngles(angle1, clipangle);
      
      if (tspan >CLIPANGLE2)
      {
      tspan -= CLIPANGLE2;
      tspan&=BITS32;

      // Totally off the left edge?
      if (tspan >= span)
          return;
      
      angle1 = clipangle;
      }
      tspan = addAngles(clipangle, - angle2);

      if (tspan > CLIPANGLE2)
      {
      tspan -=CLIPANGLE2;
      tspan&=BITS32;

      // Totally off the left edge?
      if (tspan >= span)
          return; 
      angle2 = -clipangle;
      angle2 &=BITS32;
      }
      
      // The seg is in the view range,
      // but not necessarily visible.
      
      angle1 = ((angle1+ANG90)&BITS32)>>>ANGLETOFINESHIFT;
      angle2 = ((angle2+ANG90)&BITS32)>>>ANGLETOFINESHIFT;
      x1 = viewangletox[(int) angle1];
      x2 = viewangletox[(int) angle2];

      // Does not cross a pixel?
      if (x1 == x2)
      return;             
      
      backsector = line.backsector;

      // Single sided line?
      
      if (backsector==null) {
          if (DEBUG) System.out.println("Entering ClipSolidWallSegment SS with params " + x1 +" " + (x2-1));
          ClipSolidWallSegment (x1, x2-1); // to clipsolid
          if (DEBUG) System.out.println("Exiting ClipSolidWallSegment");
          return;
          }
          

      // Closed door.
      if (backsector.ceilingheight <= frontsector.floorheight
      || backsector.floorheight >= frontsector.ceilingheight) {
          if (DEBUG)  System.out.println("Entering ClipSolidWallSegment Closed door with params " + x1 +" " + (x2-1));
          ClipSolidWallSegment (x1, x2-1);; // to clipsolid    
          return;
          }

      // Window. This includes same-level floors with different textures
      if (backsector.ceilingheight != frontsector.ceilingheight
      || backsector.floorheight != frontsector.floorheight) {
          if (DEBUG) System.out.println("Entering ClipSolidWallSegment window with params " + x1 +" " + (x2-1));
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
      
      // If nothing of the previous holds, then we are
      // treating the case of same-level, differently
      // textured floors. ACHTUNG, this caused the "bleeding floor"
      // bug, which is now fixed.
      // Fucking GOTOs....
      ClipPassWallSegment (x1, x2-1); // to clippass
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
      
      if (tspan > CLIPANGLE2)
      {
      tspan -= CLIPANGLE2;
      tspan&=BITS32;
      // Totally off the left edge?
      if (tspan >= span)
          return false;   

      angle1 = clipangle;
      }
      tspan = (clipangle - angle2)&BITS32;;
      if (tspan > CLIPANGLE2)
      {
      tspan -= CLIPANGLE2;
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
   * It also alters the visplane list! 
   * 
   * 
   * @param num Subsector from subsector_t list in Lever Loader.
   *      
   */
  
  private void Subsector (int num)  
  {
      if(DEBUG)System.out.println("\t\tSubSector " + num + " to render");
      int         count;
      int        line; // pointer into a list of segs instead of seg_t
      subsector_t    sub;
            
  if (RANGECHECK){
      if (num>=LL.numsubsectors)
      I.Error ("R_Subsector: ss %i with numss = %i",
           num,
           LL.numsubsectors);
          }

      sscount++;
      sub = LL.subsectors[num];
      
      frontsector = sub.sector;
      if(DEBUG) System.out.println("Frontsector to render :"+frontsector);
      count = sub.numlines;
      //line = LL.segs[sub.firstline];
      line=sub.firstline;

      if (DEBUG) System.out.println("Trying to find an existing FLOOR visplane...");
      if (frontsector.floorheight < viewz)
      {
      floorplane = FindPlane (frontsector.floorheight,
                    frontsector.floorpic,
                    frontsector.lightlevel);
      }
      else
          // FIXME: unclear what would happen with a null visplane used
          // It's never checked explicitly for either condition, just called straight.
          floorplane = -1; // in lieu of NULL
      
     // System.out.println("Trying to find an existing CEILING visplane...");
      
      if (frontsector.ceilingheight > viewz 
      || frontsector.ceilingpic == skyflatnum)
      {
          ceilingplane = FindPlane (frontsector.ceilingheight,
                      frontsector.ceilingpic,
                      frontsector.lightlevel);
      }
      else
          ceilingplane = -1; // In lieu of NULL. Will bomb if actually used.
          
      MyThings.AddSprites (frontsector); 

      if (DEBUG) System.out.println("Enter Addline for SubSector " + num+" count "+count);
      while (count-->0)
      {
      AddLine (LL.segs[line]);
      line++;
      }
      if (DEBUG) System.out.println("Exit Addline for SubSector " + num);
  }




  /**
   * RenderBSPNode
   * Renders all subsectors below a given node,
   *  traversing subtree recursively.
   * Just call with BSP root.
   */
  public void RenderBSPNode (int bspnum)
  {
      if (DEBUG)  System.out.println("Processing BSP Node "+bspnum);

      node_t  bsp;
      int     side;

      // Found a subsector? Then further decisions are taken, in, well, SubSector.
      if (C2JUtils.flags(bspnum ,NF_SUBSECTOR))
      {
          if (DEBUG)  System.out.println("Subsector found.");
      if (bspnum == -1)           
          Subsector (0);
      else
          Subsector (bspnum&(~NF_SUBSECTOR));
      return;
      }
          
      bsp = LL.nodes[bspnum];
      
      // Decide which side the view point is on.
      side = bsp.PointOnSide (viewx, viewy);
      if (DEBUG)  System.out.println("\tView side: "+ side);

      // Recursively divide front space.
      if (DEBUG)  System.out.println("\tEnter Front space of "+ bspnum);
      RenderBSPNode (bsp.children[side]); 
      if (DEBUG) System.out.println("\tReturn Front space of "+ bspnum);
      
      // Possibly divide back space.
      
      if (CheckBBox (bsp.bbox[side^1].bbox)){
          if (DEBUG) System.out.println("\tEnter Back space of "+bspnum);
          RenderBSPNode (bsp.children[side^1]);
          if (DEBUG) System.out.println("\tReturn Back space of "+bspnum);
      }
  }

  
  }
  
  class Segs implements SegDrawer{

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
          
          //System.out.print("RenderMaskedSegRange from "+x1 +" to "+ x2);
          
          // Calculate light table.
          // Use different light tables
          //   for horizontal / vertical / diagonal. Diagonal?
          // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
          curline = ds.curline;
          frontsector = curline.frontsector;
          backsector = curline.backsector;
          texnum = texturetranslation[curline.sidedef.midtexture];
          //System.out.print(" for texture "+textures[texnum].name+"\n:");
          lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT)+extralight;

          if (curline.v1y == curline.v2y)
          lightnum--;
          else if (curline.v1x == curline.v2x)
          lightnum++;

          // Killough code.
          walllights = lightnum >= LIGHTLEVELS ? scalelight[LIGHTLEVELS-1] :
              lightnum <  0           ? scalelight[0] : scalelight[lightnum];

          // Get the list
          maskedtexturecol = ds.getMaskedTextureColList();
          // And this is the pointer.
          pmaskedtexturecol = ds.getMaskedTextureColPointer();
                    
          rw_scalestep = ds.scalestep;        
          spryscale = ds.scale1 + (x1 - ds.x1)*rw_scalestep;
          
          // TODO: add the pointers for those somewhere
          mfloorclip = ds.getSprBottomClipList();
          p_mfloorclip=ds.getSprBottomClipPointer();
          mceilingclip = ds.getSprTopClipList();
          p_mceilingclip=ds.getSprTopClipPointer();
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
          if (maskedtexturecol[pmaskedtexturecol+dc_x] != Short.MAX_VALUE)
          {
              if (fixedcolormap==null)
              {
              index = spryscale>>>LIGHTSCALESHIFT;

              if (index >=  MAXLIGHTSCALE )
                  index = MAXLIGHTSCALE-1;

              dc_colormap = walllights[index];
              }
                  
              sprtopscreen = centeryfrac - FixedMul(dc_texturemid, spryscale);
              dc_iscale = (int) (0xffffffffL / spryscale);
              
              // draw the texture
              col.data = GetColumn(texnum,maskedtexturecol[pmaskedtexturecol+dc_x]);// -3);
              //col.setFromData();
                  
              DrawMaskedColumn (col.data);
              maskedtexturecol[pmaskedtexturecol+dc_x] = Short.MAX_VALUE;
          }
          spryscale += rw_scalestep;
          }
          
      }




      /**
       * R_RenderSegLoop
       * Draws zero, one, or two textures (and possibly a masked texture) 
       * for walls. Can draw or mark the starting pixel of floor and ceiling 
       * textures. Also sets the actual sprite clipping info (where sprites should be cut)
       * 
       * Since rw_x ranges are non-overlapping, rendering all walls means completing
       * the clipping list as well.
       * 
       * CALLED: CORE LOOPING ROUTINE.
       *
       */
      
      public void RenderSegLoop () 
      {
          int     angle = 0; // angle_t
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
                  visplanes[ceilingplane].setTop(rw_x,(char) top);
                  visplanes[ceilingplane].setBottom(rw_x, (char) bottom);
              }
          }
              
          yh = bottomfrac>>HEIGHTBITS;

          if (yh >= floorclip[rw_x])
              yh = floorclip[rw_x]-1;

          // A particular seg has been identified as a floor marker.
          
          if (markfloor)
          {
              top = yh+1;
              bottom = floorclip[rw_x]-1;
              if (top <= ceilingclip[rw_x])
              top = ceilingclip[rw_x]+1;
              if (top <= bottom)
              {
              visplanes[floorplane].setTop(rw_x, (char) top);
              visplanes[floorplane].setBottom(rw_x,  (char) bottom);
              }
          }
          
          // texturecolumn and lighting are independent of wall tiers
          if (segtextured)
          {
              // calculate texture offset
        	 
        		  
        		// CAREFUL: a VERY anomalous point in the code. Their sum is supposed
        		// to give an angle not exceeding 45 degrees (or 0x0FFF after shifting).
        		// If added with pure unsigned rules, this doesn't hold anymore,
        		// not even if accounting for overflow.
                angle = (int) (((rw_centerangle + xtoviewangle[rw_x])&BITS31)>>>ANGLETOFINESHIFT);
        		//angle&=0x1FFF;
                
              // FIXME: We are accessing finetangent here, the code seems pretty confident
              // in that angle won't exceed 4K no matter what. But xtoviewangle
              // alone can yield 8K when shifted.
              // This usually only overflows if we idclip and look at certain directions 
        	 // (probably angles get fucked up), however it seems rare enough to just 
             // "swallow" the exception. You can eliminate it by anding with 0x1FFF if you're so inclined. 
              
              // try{ 
              texturecolumn = rw_offset-FixedMul(finetangent[angle],rw_distance);
        	/*  } catch (ArrayIndexOutOfBoundsException e){
        	      System.err.println("Error! Angle "+angle +" rw_centerangle "+ Long.toHexString(rw_centerangle)+"xtoviewangle[rw_x]"+Long.toHexString(xtoviewangle[rw_x]));
        		 // e.printStackTrace();
        	  }*/
    
              texturecolumn >>= FRACBITS;
              // calculate lighting
              index = rw_scale>>LIGHTSCALESHIFT;

              if (index >=  MAXLIGHTSCALE )
              index = MAXLIGHTSCALE-1;

              dc_colormap = walllights[index];
              dc_x = rw_x;
              dc_iscale = (int) (0xffffffffL / rw_scale);
          }
          
          // draw the wall tiers
          if (midtexture!=0)
          {
              // single sided line
              dc_yl = yl;
              dc_yh = yh;
              
              //dc_texheight = textureheight[midtexture]>>FRACBITS; // killough
              dc_texheight = Math.min(textureheight[midtexture]>>FRACBITS,128); // killough
              dc_texturemid = rw_midtexturemid;              
              dc_source = GetColumn(midtexture,texturecolumn);

              
              // Tutti-frutti effect on short or masked textures.              
//              dc_texheight = Math.min(textureheight[midtexture],128)>>FRACBITS; // killough
//              dc_texturemid = rw_midtexturemid;              
//              dc_source = GetColumn(midtexture,texturecolumn);

              StoreRenderingInstruction();
              /*centery, dc_iscale, dc_source_ofs, dc_texturemid, dc_x, dc_yh, dc_yl, 
                                        columnofs, dc_colormap, dc_source);*/
              
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
                  dc_texheight=textureheight[toptexture]>>FRACBITS;
                  dc_source = GetColumn(toptexture,texturecolumn);

                  StoreRenderingInstruction();
                  
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
                  dc_texheight=textureheight[bottomtexture]>>FRACBITS;
                  dc_source = GetColumn(bottomtexture,
                              texturecolumn);

                  StoreRenderingInstruction();
                  
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
              maskedtexturecol[pmaskedtexturecol+rw_x] = (short) texturecolumn;
              }
          }
              
          rw_scale += rw_scalestep;
          topfrac += topstep;
          bottomfrac += bottomstep;
          }
      }

      /** Parallel version. Since there's so much crap to take into account when rendering, the number of
       * walls to render is unknown a-priori and the BSP trasversal itself is not worth parallelizing,
       * it makes more sense to store "rendering instructions" as quickly as the BSP can be transversed,
       * and then execute those in parallel. Also saves on having to duplicate way too much status.
       *  
       *  
       */
      
      private final void StoreRenderingInstruction(){
          /*(int centery, int dc_iscale, int dc_source_ofs, int dc_texturemid,
          int dc_x, int dc_yh, int dc_yl, int[] columnofs, byte[] dc_colormap, byte[] dc_source){*/
          if (RWIcount>=RWI.length){
              ResizeRWIBuffer();
          }
              
          RWI[RWIcount].centery=centery;
          RWI[RWIcount].dc_iscale=dc_iscale;
          RWI[RWIcount].dc_x=dc_x;
          RWI[RWIcount].dc_yh=dc_yh;
          RWI[RWIcount].dc_yl=dc_yl;
          RWI[RWIcount].columnofs=columnofs;
          RWI[RWIcount].dc_colormap=dc_colormap;
          RWI[RWIcount].dc_source=dc_source;
          RWI[RWIcount].dc_source_ofs=dc_source_ofs;
          RWI[RWIcount].dc_texturemid=dc_texturemid;
          RWI[RWIcount].dc_texheight=dc_texheight;
          RWIcount++;
      }


      /**
       * R_StoreWallRange
       * A wall segment will be drawn  between start and stop pixels (inclusive).
       * This is the only place where markceiling/markfloor can be set.
       * Can only be called from ClipSolidWallSegment and ClipPassWallSegment.
       * 
       * 
       * @throws IOException 
       */
      
      public void
      StoreWallRange
      ( int   start,
        int   stop ) 
      {
          
          if (DEBUG2)      
          System.out.println("\t\t\t\tStorewallrange called between "+start+" and "+stop);
          
          int     hyp; //fixed_t
          int     sineval; //fixed_t
          int     distangle;
          long offsetangle; // angle_t
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
          rw_normalangle = addAngles(curline.angle,ANG90);
          
          /* MAES: ok, this is a tricky spot. angle_t's are supposed to be always positive
           * 32-bit unsigned integers, so a subtraction should be always positive by definition, right?
           * WRONG: this fucking spot caused "blind spots" at certain angles because ONLY HERE angles are
           * supposed to be treated as SIGNED and result in differences <180 degrees -_-
           * 
           * The only way to coerce this behavior is to cast both as signed ints. 
           * 
           */  
          offsetangle = Math.abs((int)rw_normalangle-(int)rw_angle1);
          
          if (offsetangle > ANG90)
          offsetangle = ANG90;

          // It should fit even in a signed int, by now.
          distangle = (int) (ANG90 - offsetangle);
          hyp = PointToDist (curline.v1x, curline.v1y);
          sineval = finesine(distangle);
          rw_distance = FixedMul (hyp, sineval);
          
          seg.x1 = rw_x = start;
          seg.x2 = stop;
          seg.curline = curline;
          /* This is the only place it's ever explicitly assigned.
           * Therefore it always starts at stop+1.
           */   
          rw_stopx = stop+1;
          
          // calculate scale at both ends and step
          // FIXME: this is the ONLY place where rw_scale is set.
          seg.scale1 = rw_scale = 
          ScaleFromGlobalAngle ((viewangle + xtoviewangle[start]));
          
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
              maskedtexturecol = openings;
              pmaskedtexturecol=lastopening - rw_x;
              seg.setMaskedTextureCol(maskedtexturecol, pmaskedtexturecol);
              lastopening += rw_stopx - rw_x;
          }
          }
          
          // calculate rw_offset (only needed for textured lines)
          segtextured =(((midtexture | toptexture | bottomtexture)!=0) | maskedtexture);

          if (segtextured)
          {
          offsetangle = addAngles(rw_normalangle,-rw_angle1);
          
          // Another "tricky spot": negative of an unsigned number? 
          if (offsetangle >ANG180)
              offsetangle = (-(int)offsetangle)&BITS32;

          if (offsetangle > ANG90)
              offsetangle = ANG90;

          sineval = finesine(offsetangle);
          rw_offset = FixedMul (hyp, sineval);

          // Another bug: we CAN'T assume that the result won't wrap around.
          // If that assumption is made, then texture alignment issues appear
          if (((rw_normalangle-rw_angle1)&BITS32) < ANG180)
              rw_offset = -rw_offset;

          rw_offset += sidedef.textureoffset + curline.offset;
          // This is OK, however: we can add as much shit as we want,
          // as long as we trim it to the 32 LSB. Proof as to why
          // this is always true is left as an exercise to the reader.
          rw_centerangle = (ANG90 + viewangle - rw_normalangle)&BITS32;
          
          // calculate light table
          //  use different light tables
          //  for horizontal / vertical / diagonal
          // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
          if (fixedcolormap==null)
          {
              lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT)+extralight;

              if (curline.v1y == curline.v2y)
              lightnum--;
              else if (curline.v1x == curline.v2x)
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
          if (markceiling){
              //System.out.println("Markceiling");
          ceilingplane = MyPlanes.CheckPlane(ceilingplane, rw_x, rw_stopx-1);
          }
          
         
          if (markfloor){
              //System.out.println("Markfloor");
          floorplane = MyPlanes.CheckPlane (floorplane, rw_x, rw_stopx-1);
          }

          RenderSegLoop ();

          // After rendering is actually performed, clipping is set.
          
          // save sprite clipping info ... no top clipping?
          if ( (C2JUtils.flags(seg.silhouette , SIL_TOP) || maskedtexture)
           && seg.nullSprTopClip())
          {

          //memcpy (lastopening, ceilingclip+start, 2*(rw_stopx-start));
          System.arraycopy(ceilingclip, start, openings, lastopening,  rw_stopx-start);
              
          seg.setSprTopClip(openings, lastopening - start);
          //seg.setSprTopClipPointer();
          lastopening += rw_stopx - start;
          }
          // no floor clipping?
          if ( (C2JUtils.flags(seg.silhouette,SIL_BOTTOM) || maskedtexture)
           && seg.nullSprBottomClip())
         {
          //memcpy (lastopening, floorclip+start, 2*(rw_stopx-start));
          System.arraycopy(floorclip, start, openings, lastopening,  rw_stopx-start);
          seg.setSprBottomClip(openings, lastopening - start);
          lastopening += rw_stopx - start;
              }
          

          if (maskedtexture && C2JUtils.flags(seg.silhouette,SIL_TOP))
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
      float[]           yslopef=new float[SCREENHEIGHT];
      /** To treat as fixed_t */
      int[]           distscale=new int[SCREENWIDTH];
      float[]           distscalef=new float[SCREENWIDTH];
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
          float dangle;
          // fixed_t
          int distance;
          int length;
          float dlength;
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
          distance = cacheddistance[y] = FixedMul (planeheight , yslope[y]);
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
          angle = (int)(((viewangle +xtoviewangle[x1])&BITS32)>>>ANGLETOFINESHIFT);
          ds_xfrac = viewx + FixedMul(finecosine[angle], length);
          ds_yfrac = -viewy - FixedMul(finesine[angle], length);

          // FIXME: alternate, more FPU-friendly implementation.
          //dlength = (distance);//*distscalef[x1];
          //dangle = (float) (2*Math.PI*(double)((viewangle +xtoviewangle[x1])&BITS32)/((double)0xFFFFFFFFL));
          //ds_xfrac = viewx + (int)(Math.cos(dangle)* dlength);
          //ds_yfrac = -viewy -(int)(Math.sin(dangle)* dlength);

          
          if (fixedcolormap!=null)
          ds_colormap = fixedcolormap;
          else
          {
          index = distance >>> LIGHTZSHIFT;
          
          if (index >= MAXLIGHTZ )
              index = MAXLIGHTZ-1;

          ds_colormap = planezlight[index];
          }
          
          ds_y = y;
          ds_x1 = x1;
          ds_x2 = x2;

          // high or low detail
          spanfunc.invoke();    
      }


      /**
       * R_ClearPlanes
       * At begining of frame.
       * 
       */
      
      
      private void ClearPlanes ()
      {              
          int angle;
          
          /* View planes are cleared at the beginning of 
           * every plane, by setting them "just outside"
           * the borders of the screen (-1 and viewheight).
           * 
           */
          
          for (int i=0 ; i<viewwidth ; i++)
          {
          floorclip[i] =(short) viewheight;
          ceilingclip[i] = -1;
          }

          // Point to #1 in visplane list? OK... ?!
          lastvisplane = 0;          
          
          // We point back to the first opening of the list openings[0], again.
          lastopening = 0;
          
          // texture calculation
          System.arraycopy(BLANKCACHEDHEIGHT,0,cachedheight, 0, BLANKCACHEDHEIGHT.length);

          // left to right mapping
          // FIXME: If viewangle is ever < ANG90, you're fucked. How can this be prevented?
          // Answer: 32-bit unsigned are supposed to roll over. You can & with 0xFFFFFFFFL.
          angle = (int) Tables.toBAMIndex(viewangle-ANG90);
          
          // scale will be unit scale at SCREENWIDTH/2 distance
          basexscale = FixedDiv (finecosine[angle],centerxfrac);
          baseyscale = -FixedDiv (finesine[angle],centerxfrac);
      }


      


      /**
       * R_CheckPlane
       * 
       * Called from within StoreWallRange
       * 
       * Presumably decides if a visplane should be split or not?
       * 
       */
      
      public int
      CheckPlane
      ( int index,
        int       start,
        int       stop )
      {
          
          if (DEBUG2) System.out.println("Checkplane "+index+" between "+start+" and "+stop);
          
          // Interval ?
          int     intrl;
          int     intrh;
          
          // Union?
          int     unionl;          
          int     unionh;
          // OK, so we check out ONE particular visplane.
          visplane_t pl=visplanes[index];
          
          if (DEBUG2) System.out.println("Checking out plane "+pl);
          
          int x;
          
          /* If start is smaller than the plane's min... 
           * 
           * start     minx         maxx       stop
           *   |       |            |          |
           *   --------PPPPPPPPPPPPPP-----------
           * 
           */
          if (start < pl.minx)
          {
          intrl = pl.minx;
          unionl = start;
          /* Then we will have this: 
           * 
           * unionl    intrl        maxx       stop
           *   |       |            |          |
           *   --------PPPPPPPPPPPPPP-----------
           */
          
          }
          else
          {              
          unionl = pl.minx;
          intrl = start;
          
          /* else we will have this: 
           * 
           *      union1 intrl      maxx       stop
           *           |      |     |          |
           *   --------PPPPPPPPPPPPPP-----------
           *   
           * unionl comes before intrl in any case.  
           *   
           */          
          }
          
          /* Same as before, for for stop and maxx.
           * This time, intrh comes before unionh.
           */
          
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

          /* An interval is now defined, which is entirely contained in the
           * visplane. 
           */

          // If the value FF is NOT stored ANYWWHERE inside it, we bail out early
          for (x=intrl ; x<= intrh ; x++)
              if (pl.getTop(x) != Character.MAX_VALUE)
              break;

          // This can only occur if the loop above completes,
          // else the visplane we were checking has non-visible/clipped
          // portions within that range: we must split.
          
          if (x > intrh)
          {
          // Merge the visplane
          pl.minx = unionl;
          pl.maxx = unionh;
          //System.out.println("Plane modified as follows "+pl);
          // use the same one
          return index;      
          }
          
          // SPLIT: make a new visplane at "last" position, copying materials and light.
          // TODO: visplane overflows could occur at this point.
          
          if (lastvisplane==MAXVISPLANES){
              ResizeVisplanes();
          }
                    
          visplanes[lastvisplane].height = pl.height;
          visplanes[lastvisplane].picnum = pl.picnum;
          visplanes[lastvisplane].lightlevel = pl.lightlevel;
          
          // The old visplane is left as-is, we now assign new start-stop to
          // the "split off".
          
          pl = visplanes[lastvisplane++];          
          pl.minx = start;
          pl.maxx = stop;

          
          //memset (pl.top,0xff,sizeof(pl.top));
          pl.clearTop();
              
          //return pl;
          //System.out.println("New plane created: "+pl);
          return lastvisplane-1;
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

        private void MakeSpans(int x, int t1, int b1, int t2, int b2) {
            
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
       * R_DrawPlanes
       * At the end of each frame.
       * 
       * This also means that visplanes must have been set BEFORE we called this
       * function. Therefore, look for errors behind.
       * 
       * @throws IOException 
       */
      public void DrawPlanes () 
      {
    	  if(DEBUG) System.out.println(" >>>>>>>>>>>>>>>>>>>>>   DrawPlanes: "+ lastvisplane);

                      
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

      		
   //   	vpw[0].setRange(0,lastvisplane/2);
   //   	vpw[1].setRange(lastvisplane/2,lastvisplane);
      	
      	for (int i=0;i<NUMFLOORTHREADS;i++)
      		tp.execute(vpw[i]);
      }

  } // End Plane class
      
  class VisplaneWorker implements Runnable{

	public int id;
	int startvp;  
	int endvp;
	int vpw_planeheight;
	byte[][] vpw_planezlight;
	int vpw_basexscale;
	int vpw_baseyscale;
    int[] cachedheight;
    int[] cacheddistance;
    int[] cachedxstep;
    int[] cachedystep;
    int[] distscale;
    int[] yslope;
	int vpw_dc_texturemid;
	int vpw_dc_texheight;
	int vpw_dc_iscale;
	byte[] vpw_dc_colormap, vpw_dc_source, vpw_ds_source,vpw_ds_colormap;
    int vpw_dc_yl;
    int vpw_dc_yh;
    int vpw_dc_x;
    int vpw_dc_y;
    int vpw_ds_x;
    int vpw_ds_y;
    int ds_x1;
    int ds_x2;
    int ds_xstep;
    int ds_xfrac;
    int ds_ystep;
    int ds_yfrac;
    int dc_source_ofs;
    
	public VisplaneWorker(CyclicBarrier barrier){
		  this.barrier=barrier;
	      // Alias to those of Planes.
	      cachedheight=MyPlanes.cachedheight;
	      cacheddistance=MyPlanes.cacheddistance;
	      cachedxstep=MyPlanes.cachedxstep;
	      cachedystep=MyPlanes.cachedystep;
	      distscale=MyPlanes.distscale;
	      yslope=MyPlanes.yslope;

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
        vpw_basexscale=MyPlanes.basexscale;
        vpw_baseyscale=MyPlanes.baseyscale;
        
			 for (int pl= this.id; pl <lastvisplane; pl+=NUMFLOORTHREADS) {
             pln=visplanes[pl];
            // System.out.println(id +" : "+ pl);
             
         if (pln.minx > pln.maxx)
             continue;

         
         // sky flat
         if (pln.picnum == skyflatnum)
         {
             dc_iscale = skyscale>>detailshift;
             
             /* Sky is allways drawn full bright,
              * i.e. colormaps[0] is used.
              * Because of this hack, sky is not affected
              * by INVUL inverse mapping.
              */    
             dc_colormap = colormaps[0];
             dc_texturemid = skytexturemid;
             for (x=pln.minx ; x <= pln.maxx ; x++)
             {
           
             dc_yl = pln.getTop(x);
             dc_yh = pln.getBottom(x);
             
             if (dc_yl <= dc_yh)
             {
                 angle = (int) (addAngles(viewangle, xtoviewangle[x])>>>ANGLETOSKYSHIFT);
                 dc_x = x;
                 dc_texheight=textureheight[skytexture]>>FRACBITS;
                 dc_source = GetColumn(skytexture, angle);
                 colfunc.invoke();
             }
             }
             continue;
         }
         
         // regular flat
         vpw_ds_source = ((flat_t)W.CacheLumpNum(firstflat +
                        flattranslation[pln.picnum],
                        PU_STATIC,flat_t.class)).data;
         
         
         if (vpw_ds_source.length<4096){
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

      private void MakeSpans(int x, int t1, int b1, int t2, int b2) {
          
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
          ds_xstep = cachedxstep[y] = FixedMul (distance,vpw_basexscale);
          ds_ystep = cachedystep[y] = FixedMul (distance,vpw_baseyscale);
          }
          else
          {
          distance = cacheddistance[y];
          ds_xstep = cachedxstep[y];
          ds_ystep = cachedystep[y];
          }
          
          length = FixedMul (distance,distscale[x1]);
          angle = (int)(((viewangle +xtoviewangle[x1])&BITS32)>>>ANGLETOFINESHIFT);
          ds_xfrac = viewx + FixedMul(finecosine[angle], length);
          ds_yfrac = -viewy - FixedMul(finesine[angle], length);

          if (fixedcolormap!=null)
          vpw_ds_colormap = fixedcolormap;
          else
          {
          index = distance >>> LIGHTZSHIFT;
          
          if (index >= MAXLIGHTZ )
              index = MAXLIGHTZ-1;

          vpw_ds_colormap = vpw_planezlight[index];
          }
          
          vpw_ds_y = y;
          ds_x1 = x1;
          ds_x2 = x2;

          // high or low detail
          spanfunc();    
      }
      
      // Each thread has its own copy of a colfun.
      private void spanfunc(){
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
    	     source = vpw_ds_source;
    	     colormap = vpw_ds_colormap;
    	     dest = ylookup[vpw_ds_y] + columnofs[ds_x1];
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
      
      // Private to each thread.
      int[]           spanstart=new int[SCREENHEIGHT];
      int[]           spanstop=new int [SCREENHEIGHT];
      CyclicBarrier barrier;
      
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
  

/**
 * R_InitRWISubsystem
 * 
 * Initialize RWIs and RWI Executors.
 * Pegs them to the RWI, ylookup and screen[0].
 */

private void InitRWISubsystem() {

    C2JUtils.initArrayOfObjects(RWI);
    
    for (int i=0;i<NUMWALLTHREADS;i++){
        RWIExec[i]=new RenderWallExecutor(ylookup, screen,RWI,visplanebarrier);
    }
}

/** Resizes RWI buffer, updates executors. Sorry for the hackish implementation
 *  but ArrayList and pretty much everything in Collections is way too slow
 *  for what we're trying to accomplish.
 * 
 */

private void ResizeRWIBuffer() {
    RenderWallInstruction[] tmp=new RenderWallInstruction[RWI.length*2];
    System.arraycopy(RWI, 0, tmp, 0, RWI.length);
    
    C2JUtils.initArrayOfObjects(tmp,RWI.length,tmp.length);
    
    // Bye bye, old RWI.
    RWI=tmp;   
    
    for (int i=0;i<NUMWALLTHREADS;i++){
        RWIExec[i].updateRWI(RWI);
    }
    
    System.out.println("RWI Buffer resized. Actual capacity "+RWI.length);
}


private void RenderRWIPipeline() {

    for (int i=0;i<NUMWALLTHREADS;i++){
        RWIExec[i].setRange((i*RWIcount)/NUMWALLTHREADS, ((i+1)*RWIcount)/NUMWALLTHREADS);
        tp.execute(RWIExec[i]);
    }
    
    //System.out.println("RWI count"+RWIcount);
    RWIcount=0;
}


///////////////////////// The actual rendering calls ///////////////////////
 
 /**
  * R_RenderView
  * 
  * As you can guess, this renders the player view of a particular player object.
  * In practice, it could render the view of any mobj too, provided you adapt the
  * SetupFrame method (where the viewing variables are set).
  * 
  */
 
public void RenderPlayerView (player_t player)
{   
    
  // Viewing variables are set according to the player's mobj. Interesting hacks like
  // free cameras or monster views can be done.
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
  
  RenderRWIPipeline();
  // Check for new console commands.
  //NetUpdate ();
  
  // "Warped floor" fixed, same-height visplane merging fixed.
  MyPlanes.DrawPlanes ();
  
  try {
    visplanebarrier.await();
} catch (Exception e){
    e.printStackTrace();
}

  
  // Check for new console commands.
  //NetUpdate ();
  
  MyThings.DrawMasked ();

    
  // Check for new console commands.
  //NetUpdate ();             
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
 viewangle = addAngles(player.mo.angle , viewangleoffset);
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
 * R_Init
 */
  
//public int  detailLevel;
//public int  screenblocks=9; // has default

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

   SetViewSize (DM.M.getScreenBlocks(), DM.M.getDetailLevel());
   MyPlanes.InitPlanes ();
   System.out.print ("\nR_InitPlanes");
   InitLightTables ();
   System.out.print("\nR_InitLightTables");
   
   System.out.print("\nR_InitSkyMap: "+InitSkyMap ());
   InitTranslationTables ();
   System.out.print("\nR_InitTranslationsTables");
   
   System.out.print("\nR_InitRWISubsystem: ");
   InitRWISubsystem();
   
   System.out.print("\nR_InitTranMap: ");
   R_InitTranMap(0);
   
   framecount = 0;
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
        scaledviewwidth = setblocks*(SCREENWIDTH/10);
        // Height can only be a multiple of 8.
        viewheight = (short) ((setblocks*(SCREENHEIGHT- StatusBar.ST_HEIGHT)/10)&~7);
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
    glasscolfunc=DrawTLColumn;
    playercolfunc=glasscolfunc;//DrawColumnPlayer;
    spanfunc = DrawSpan;
    }
    else {
    // Low detail
    colfunc = basecolfunc = DrawColumnLow;
    fuzzcolfunc =DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    glasscolfunc=DrawTLColumn;
    playercolfunc=DrawColumnPlayer;
    spanfunc = DrawSpanLow;
    
    }

    InitBuffer (scaledviewwidth, viewheight);
    
    InitTextureMapping ();
    
    // psprite scales
    //pspritescale = FRACUNIT*viewwidth/SCREENWIDTH;
    //pspriteiscale = FRACUNIT*SCREENWIDTH/viewwidth;
    
    
    pspritescale=(int) (FRACUNIT*((float)SCREEN_MUL*viewwidth)/SCREENWIDTH);
    pspriteiscale = (int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));
    skyscale=(int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));

    BOBADJUST=(int)(Defines.SCREEN_MUL*65536.0);
    WEAPONADJUST=(int) ((SCREENWIDTH/(2*Defines.SCREEN_MUL))*FRACUNIT);
    
    // thing clipping
    for (i=0 ; i<viewwidth ; i++)
    screenheightarray[i] = (short) viewheight;
    
    // planes
    for (i=0 ; i<viewheight ; i++)
    {
    dy = ((i-viewheight/2)<<FRACBITS)+FRACUNIT/2;
    dy = Math.abs(dy);
    MyPlanes.yslope[i] = FixedDiv ( (viewwidth<<detailshift)/2*FRACUNIT, dy);
    MyPlanes.yslopef[i] = ((viewwidth<<detailshift)/2)/ dy;
    }
    
    double cosadjf;
    for (i=0 ; i<viewwidth ; i++)
    {
    // MAES: In this spot we must interpet it as SIGNED, else it's pointless, right?
    // MAES: this spot caused the "warped floor bug", now fixed. Don't forget xtoviewangle[i]!    
    cosadj = Math.abs(finecosine(xtoviewangle[i]));
    cosadjf = Math.abs(Math.cos((double)xtoviewangle[i]/(double)0xFFFFFFFFL));
    MyPlanes.distscale[i] = FixedDiv (FRACUNIT,cosadj);
    MyPlanes.distscalef[i] = (float) (1.0/cosadjf);
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
  
}
