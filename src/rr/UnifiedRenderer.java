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
import doom.DoomMain;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;

public class UnifiedRenderer extends RendererState{
    
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
   * @throws IOException 
   */ 

  public void ClipSolidWallSegment (int   first,
          int   last ) {
      

      int next;
      int start;
      int maxlast=Integer.MIN_VALUE;
      
      // Find the first range that touches the range
      //  (adjacent pixels are touching).
      start = 0;
      
      try{
      while (solidsegs[start].last < first-1){
          if (solidsegs[start].last>maxlast)
              maxlast=solidsegs[start].last;
      start++;
      }

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
                  solidsegs[++start] = solidsegs[next];
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
              solidsegs[++start] = solidsegs[next];
          }

          newend = start+1;
          return;
      }
      } catch (Exception e){
          System.out.println("ERROR!! Was looking for stuff bigger than "+last);
          System.out.println("Maximum found was"+maxlast);
          System.out.println("Was looking for stuff bigger than "+last);
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
      solidsegs[1].first = DM.viewwidth;
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
      System.out.println("Entered AddLine for "+line);
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
          System.out.println("Entering ClipSolidWallSegment SS");
          ClipSolidWallSegment (x1, x2-1); // to clipsolid
          System.out.println("Exiting ClipSolidWallSegment");
          return;
          }
          

      // Closed door.
      if (backsector.ceilingheight <= frontsector.floorheight
      || backsector.floorheight >= frontsector.ceilingheight) {
          System.out.println("Entering ClipSolidWallSegment Closed door");
          ClipSolidWallSegment (x1, x2-1);; // to clipsolid    
          return;
          }

      // Window.
      if (backsector.ceilingheight != frontsector.ceilingheight
      || backsector.floorheight != frontsector.floorheight) {
          System.out.println("Entering ClipSolidWallSegment window");
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
      System.out.println("Exiting AddLine for "+line);
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
   * Determine floor/ceiling planes.
   * Add sprites of things in sector.
   * Draw one or more line segments.
 * @throws IOException 
   */
  
  public void Subsector (int num)  
  {
      System.out.println("SubSector " + num);
      int         count;
      int        line; // pointer into a list of segs
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

              dc_colormap = walllights;
              dco=index;
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
     * @throws IOException 
       */
      
      public void RenderSegLoop () 
      {
          long     angle; // angle_t
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
              texturecolumn = rw_offset-FixedMul(finetangent[(int) angle],rw_distance);
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
              dc_source = GetColumn(midtexture,texturecolumn);
              colfunc.invoke();
              ceilingclip[rw_x] = DM.viewheight;
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
          if (start >=DM.viewwidth || start > stop)
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
         // FIXME: System.arraycopy(ceilingclip, start, openings, lastopening,  rw_stopx-start);
              
          seg.setSprTopClipPointer(lastopening - start);
          lastopening += rw_stopx - start;
          }
          // no floor clipping?
          if ( ((seg.silhouette & SIL_BOTTOM)!=0 || maskedtexture)
           && seg.nullSprBottomClip())
          {
          //memcpy (lastopening, floorclip+start, 2*(rw_stopx-start));
  //System.arraycopy(floorclip, start, openings, lastopening,  rw_stopx-start);
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
          long angle;
          // 
          int distance;
          int length;
          int index;
          
      if (RANGECHECK){
          if (x2 < x1
          || x1<0
          || x2>=DM.DM.viewwidth
          || y>DM.viewheight)
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

          ds_colormap = planezlight;
          pds_colormap=index;
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
          for (int i=0 ; i<DM.DM.viewwidth ; i++)
          {
          floorclip[i] = DM.viewheight;
          ceilingclip[i] = -1;
          }

          lastvisplane = 1;
          lastopening = openings[0];
          
          // texture calculation
          //memset (cachedheight, 0, sizeof(cachedheight));

          // left to right mapping
          // FIXME: If viewangle is ever < ANG90, you're fucked. How can this be prevented?
          // Answer: 32-bit unsigned are supposed to roll over. You can & with 0xFFFFFFFFL.
          angle = (int) ((viewangle-ANG90&BITS32)>>>ANGLETOFINESHIFT);
          
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
              && lightlevel ==chk.lightlevel) break;
          }
                  
          if (check < lastvisplane)
          return check;
              
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
              dc_colormap = colormaps;
              dc_texturemid = skytexturemid;
              for (x=pln.minx ; x <= pln.maxx ; x++)
              {
              dc_yl = pln.getTop(x);
              dc_yh = pln.getBottom(x);

              if (dc_yl <= dc_yh)
              {
                  angle = (int) ((viewangle + xtoviewangle[x])>>ANGLETOSKYSHIFT);
                  dc_x = x;
                  dc_source = GetColumn(skytexture, angle);
                  // TODO: Until you fix texture compositing, this can't work.
                  //colfunc.invoke();
              }
              }
              continue;
          }
          
          // regular flat
          ds_source = ((flat_t)W.CacheLumpNum(firstflat +
                         flattranslation[pln.picnum],
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

      byte[]  spritelights;

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
      
      public void
      InstallSpriteLump
      ( int       lump,
        int  frame,
        int  rotation,
        boolean   flipped )
      {
          
          System.out.println("Trying to install "+spritename+" Frame "+ (char)('A'+frame)+" rot "+(rotation) +" . Should have rotations: "+sprtemp[frame].rotate); 
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
              /* FIXME: notice how comparisons are done with strict literals         
               * ( .rotate == false and .rotate == true) which are actually 0 and 1,
               * and true isn't just "any nonzero value" in this context.
               * This happens because normally rotate should be -1 at this point (!),
               * as nothing else can change it.
               * 
               * Therefore both are actually cases of "should not happen"
               */
              
              // the lump should be used for all rotations
          if (sprtemp[frame].rotate == 0){
              // MAES: Explanation: we stumbled upon this lump before, and decided that this frame should have no more
              // rotations, hence we bomb.
              I.Error ("R_InitSprites: Sprite %s frame %c has multiple rot=0 lump", spritename, 'A'+frame);
          }

          // This should NEVER happen!
          if (sprtemp[frame].rotate == 1) {
              //MAES: This can only happen if we decided that a sprite's frame was already decided to have
              // rotations, but now we stumble upon another occurence of "rotation 0". Or if you use naive
              // true/false evaluation for .rotate ( -1 is also an admissible value).
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
          DrawMaskedColumn (column);
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
          if (x1 > DM.viewwidth)
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
          vis.x2 = x2 >= DM.viewwidth ? DM.viewwidth-1 : x2;
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
              // TODO: these are probably meant to be array aliases/arrays in the middle of other arrays.
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
          if (x1 > DM.viewwidth)
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
          vis.x2 = x2 >= DM.viewwidth ? DM.viewwidth-1 : x2;   
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
              System.out.println("Drawseg "+ds+"of "+(ds_p-1));
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
              clipbot[x] = (short) DM.viewheight;
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
          t = DM.DM.viewwidth+1;
      else
      {
          t = FixedMul (finetangent[i], focallength);
          t = (centerxfrac - t+FRACUNIT-1)>>FRACBITS;

          if (t < -1)
          t = -1;
          else if (t>DM.DM.viewwidth+1)
          t = DM.DM.viewwidth+1;
      }
      viewangletox[i] = t;
      }
      
      // Scan viewangletox[] to generate xtoviewangle[]:
      //  xtoviewangle will give the smallest view angle
      //  that maps to x. 
      for (x=0;x<=DM.DM.viewwidth;x++)
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
      else if (viewangletox[i] == DM.DM.viewwidth+1)
          viewangletox[i]  = DM.DM.viewwidth;
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

          zlight[i][j] = colormaps[level*256];
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

     if (DM.scaledviewwidth == SCREENWIDTH)
         return;

     top = ((SCREENHEIGHT - SBARHEIGHT) - DM.viewheight) / 2;
     side = (SCREENWIDTH - DM.scaledviewwidth) / 2;

     // copy top and one line of left side
     this.VideoErase(0, top * SCREENWIDTH + side);

     // copy one line of right side and bottom
     ofs = (DM.viewheight + top) * SCREENWIDTH - side;
     this.VideoErase(ofs, top * SCREENWIDTH + side);

     // copy sides using wraparound
     ofs = top * SCREENWIDTH + SCREENWIDTH - side;
     side <<= 1;

     for (i = 1; i < DM.viewheight; i++) {
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

     if (DM.scaledviewwidth == 320)
         return;

     if (DM.gamemode == GameMode_t.commercial)
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

     patch = (patch_t) W.CachePatchName("BRDR_T", PU_CACHE);

     for (x = 0; x < DM.scaledviewwidth; x += 8)
         V.DrawPatch(DM.viewwindowx + x, DM.viewwindowy - 8, 1, patch);
     patch = (patch_t) W.CachePatchName("BRDR_B", PU_CACHE);

     for (x = 0; x < DM.scaledviewwidth; x += 8)
         V.DrawPatch(DM.viewwindowx + x, DM.viewwindowy + DM.viewheight, 1, patch);
     patch = (patch_t) W.CachePatchName("BRDR_L", PU_CACHE);

     for (y = 0; y < DM.viewheight; y += 8)
         V.DrawPatch(DM.viewwindowx - 8, DM.viewwindowy + y, 1, patch);
     patch = (patch_t) W.CachePatchName("BRDR_R", PU_CACHE);

     for (y = 0; y < DM.viewheight; y += 8)
         V.DrawPatch(DM.viewwindowx + DM.scaledviewwidth, DM.viewwindowy + y, 1,
             patch);

     // Draw beveled edge. Top-left
     V.DrawPatch(DM.viewwindowx - 8, DM.viewwindowy - 8, 1, (patch_t) W
             .CachePatchName("BRDR_TL", PU_CACHE));

     // Top-right.
     V.DrawPatch(DM.viewwindowx + DM.scaledviewwidth, DM.viewwindowy - 8, 1,
         (patch_t) W.CachePatchName("BRDR_TR", PU_CACHE));

     // Bottom-left
     V.DrawPatch(DM.viewwindowx - 8, DM.viewwindowy + DM.viewheight, 1,
         (patch_t) W.CachePatchName("BRDR_BL", PU_CACHE));
     // Bottom-right.
     V.DrawPatch(DM.viewwindowx + DM.scaledviewwidth, DM.viewwindowy + DM.viewheight, 1,
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
     DM.viewwindowx = (SCREENWIDTH - width) >> 1;

     // Column offset. For windows.
     for (i = 0; i < width; i++)
         columnofs[i] = DM.viewwindowx + i;

     // Samw with base row offset.
     if (width == SCREENWIDTH)
    	 DM.viewwindowy = 0;
     else
    	 DM.viewwindowy = (SCREENHEIGHT - SBARHEIGHT - height) >> 1;

     // Preclaculate all row offsets.
     for (i = 0; i < height; i++)
         ylookup[i] = /* screens[0] + */(i + DM.viewwindowy) * SCREENWIDTH;
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
         screen[dest++] = ds_colormap[pds_colormap+ds_source[pds_source+spot]];

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
            // TODO: proper colormaps.
            //screen[dest++] = ds_colormap[pds_colormap+ds_source[pds_source+spot]];
            //screen[dest++] = ds_colormap[pds_colormap+ds_source[pds_source+spot]];
            
            screen[dest++] = ds_source[pds_source+spot];
            screen[dest++] = ds_source[pds_source+spot];
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

// Who can set this? A: The Menu.
public boolean      setsizeneeded;
int     setblocks;
int     setdetail;


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
    int viewheight=DM.viewheight;
    
    setsizeneeded = false;

    
    
    if (setblocks == 11)
    {
    DM.scaledviewwidth = SCREENWIDTH;
    viewheight = SCREENHEIGHT;
    }
    else
    {
        DM.scaledviewwidth = setblocks*32;
        viewheight = (short) ((setblocks*168/10)&~7);
    }
    
    detailshift = setdetail;
    DM.viewwidth = DM.scaledviewwidth>>detailshift;
    
    centery = viewheight/2;
    centerx = DM.viewwidth/2;
    centerxfrac=(centerx<<FRACBITS);
    centeryfrac=(centery<<FRACBITS);
    projection=centerxfrac;

    
    if (detailshift!=0)
    {
        
    colfunc = basecolfunc =DrawColumn;
    fuzzcolfunc = DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    spanfunc = DrawSpan;
    }
    else {
    
    colfunc = basecolfunc = DrawColumnLow;
    fuzzcolfunc =DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    spanfunc = DrawSpanLow;
    
    }

    InitBuffer (DM.scaledviewwidth, DM.viewheight);
    
    InitTextureMapping ();
    
    // psprite scales
    pspritescale=(FRACUNIT*DM.viewwidth/SCREENWIDTH);
    pspriteiscale=(FRACUNIT*SCREENWIDTH/DM.viewwidth);
    
    // thing clipping
    for (i=0 ; i<DM.viewwidth ; i++)
    screenheightarray[i] = (short) DM.viewheight;
    
    // planes
    for (i=0 ; i<viewheight ; i++)
    {
    dy = ((i-viewheight/2)<<FRACBITS)+FRACUNIT/2;
    dy = Math.abs(dy);
    // MAES: yslope is a field in "r_plane.c" so it should really be in the Rendering Context.
    MyPlanes.yslope[i] = FixedDiv ( (DM.viewwidth<<detailshift)/2*FRACUNIT, dy);
    }
    
    for (i=0 ; i<DM.viewwidth ; i++)
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
        level = startmap - j*SCREENWIDTH/(DM.viewwidth<<detailshift)/DISTMAP;
        
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
  public texture_t[] textures;


  int[]            texturewidthmask;
  /** fixed_t[] needed for texture pegging */
  public int[]        textureheight;      
  int[]            texturecompositesize;
  /** Tells us which patch lump covers which column of which texture */
  short[][]         texturecolumnlump;
  /** This is supposed to store indexes into a patch_t lump which point to the columns themselves 
   *  Instead, we're going to return indexes to columns inside a particular patch.
   * */
  short[][]    texturecolumnofs;
  short[][]    texturecolumnindexes;
  byte[][]          texturecomposite;

  // for global animation
  public int[]        flattranslation;
  public int[]        texturetranslation;

  /** needed for pre rendering (fixed_t[]) */
  int[]    spritewidth,spriteoffset,spritetopoffset;

  /** The underlying data type would be unsigned bytes. Promote to char/short? */
  public byte[]    colormaps;


  /**
  // MAPTEXTURE_T CACHING
  // When a texture is first needed,
  //  it counts the number of composite columns
  //  required in the texture and allocates space
  //  for a column directory and any new columns.
  // The directory will simply point inside other patches
  //  if there is only one patch in a given column,
  //  but any columns with multiple patches
  //  will have new column_ts generated.
  //

  /**
   *  R_DrawColumnInCache
   *  Clip and draw a column
   *  from a patch into a cached post.
   *  
   *  This means that columns are effectively "uncompressed" into cache, here,
   *  and that composite textures are uncompressed...right?
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
      
      // Iterate inside column
      for (int i=0;i<patch.posts;i++){

          source += 3; // Relative to patch's data.
          count = patch.postlen[i]; // in this particular post
      position = originy + patch.postdeltas[i];

      // Post would be drawn outside of screen. Fuck it.
      if (position < 0)
      {
          count += position;
          position = 0;
      }

      if (position + count > cacheheight)
          count = cacheheight - position;

      if (count > 0) // Draw this post.
          //memcpy (cache + position, source, count)
          System.arraycopy( patch.data, source, cache, offset+position,count);
          
      // patch = (column_t *)(  (byte *)patch + patch.length + 4);
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
      byte[]       block;
      texture_t      texture;
      texpatch_t[]     patch;  
      patch_t        realpatch;
      int         x;
      int         x1;
      int         x2;
      column_t       patchcol;
      short[]      collump;
      short[] colofs; // unsigned short
      short[] colidxs; // unsigned short
      
      texture = textures[texnum];

      // Allocate both the composite and assign it to block.
      // Z_Malloc (texturecompositesize[texnum], PU_STATIC, &texturecomposite[texnum]);
      block = texturecomposite[texnum]=new byte[texturecompositesize[texnum]];
   
      collump = texturecolumnlump[texnum];
      colofs = texturecolumnofs[texnum];
      colidxs = texturecolumnindexes[texnum];
      
      // Composite the columns together.
      patch = texture.patches;
          
      for (int i=0 ;i<texture.patchcount; i++)
      {
      realpatch = (patch_t) W.CacheLumpNum(patch[i].patch, PU_CACHE, patch_t.class);
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
          
          patchcol=realpatch.columns[x-x1];
          DrawColumnInCache (patchcol,
                   block, colofs[x],
                   patch[i].originy,
                   texture.height);
      }
                          
      }
  }

  /**
   * R_GenerateLookup
 * @throws IOException 
   */
  
  protected void GenerateLookup (int texnum) throws IOException
  {
      texture_t      texture;
      short[]       patchcount; // patchcount[texture.width]
      texpatch_t[]     patch;  
      patch_t        realpatch;
      int         x;
      int         x1;
      int         x2;

      short[]      collump;
       short[] colofs;
      
      texture = textures[texnum];

      // Composited texture not created yet.
      texturecomposite[texnum] = null;
      
      texturecompositesize[texnum] = 0;
      collump = texturecolumnlump[texnum];
      colofs = texturecolumnofs[texnum];
      
      // Now count the number of columns
      //  that are covered by more than one patch.
      // Fill in the lump / offset, so columns
      //  with only a single patch are all done.

      patchcount = new short[texture.width];
      patch = texture.patches;
          
      // for each patch in a texture...
      for (int i=0; i<texture.patchcount;i++)
      {
      realpatch = (patch_t) W.CacheLumpNum (patch[i].patch, PU_CACHE,patch_t.class);
      x1 = patch[i].originx;
      x2 = x1 + realpatch.width;
      
      if (x1 < 0)
          x = 0;
      else
          x = x1;

      if (x2 > texture.width)
          x2 = texture.width;
      for ( ; x<x2 ; x++)
      {
          // Obviously, if a patch starts at x it does cover the x-th column
          // of a texture, even if transparent.
          patchcount[x]++;
          // Column "x" of composite texture "texnum" is covered by this patch.
          collump[x] = (short) patch[i].patch;
          // This is supposed to be a raw pointer to the beginning of the column
          // data, as it appears inside the PATCH.
          // 
          // Instead, we can return the actual column index (x-x1)
          colofs[x] = (short) (realpatch.columnofs[x-x1]+3);
          // This implies that colofs[x] is 0 for a void column?
              
      }
      }
      
      for ( x=0 ; x<texture.width ; x++)
      {
      if (patchcount[x]==0)
      {
          System.err.print ("R_GenerateLookup: column without a patch ("+texture.name+")\n");
          return;
      }
      // I_Error ("R_GenerateLookup: column without a patch");
      
      if (patchcount[x] > 1)
      {
          // Use the cached block.
          collump[x] = -1;    
          colofs[x] = (short) texturecompositesize[texnum];
          
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
      int     lump;
      int     ofs,idx;
      
      col &= texturewidthmask[tex];
      lump = texturecolumnlump[tex][col];
      ofs = texturecolumnofs[tex][col];
      idx = texturecolumnindexes[tex][col];
      
      // So if this is zero, texture is not composite?
      if (lump > 0)
          // This will actually return a pointer to a patch's columns.
          // That is, to the ONE column exactly.
      return ((patch_t)W.CacheLumpNum(lump,PU_CACHE,patch_t.class)).columns[idx].data;

      if (texturecomposite[tex]==null)
      GenerateComposite (tex);

      // This implies that texturecomposite actually stores raw, compressed columns,
      // or else those "ofs" would go in-between.
      return null;// TODO: texturecomposite[tex] + ofs;
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
      texturecolumnofs = new short[numtextures][];
      texturecolumnindexes = new short[numtextures][];
      texturecomposite = new byte[numtextures][];
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
      texturecolumnofs[i] = new short[texture.width];
      texturecolumnindexes[i] = new short[texture.width];
      
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
      length = W.LumpLength (lump) + 255; 
      colormaps = new byte[length];
      ByteBuffer b=ByteBuffer.wrap(colormaps);
      W.ReadLump (lump,b);
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
      texturepresent[DM.skytexture] = true;
      
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
