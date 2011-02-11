package rr;

import static data.Defines.*;
import static data.Limits.*;
import static data.Tables.*;
import static m.fixed_t.*;
import static m.BBox.*;


import java.io.IOException;

import st.StatusBar;
import utils.C2JUtils;
import data.Defines;
import data.Tables;
import doom.DoomMain;
import doom.player_t;

public class UnifiedRenderer extends RendererState{
    
    // HACK: An all zeroes array used for fast clearing of certain visplanes.
    private static int[]           BLANKCACHEDHEIGHT=new int[SCREENHEIGHT];
    
    protected BSP MyBSP;
    protected Planes MyPlanes;
    
    
    public UnifiedRenderer(DoomMain DM) {
      this.DM=DM;
      this.LL=DM.LL;
      this.W=DM.W;
      this.MySegs=new Segs();
      this.MyBSP=new BSP();
      this.MyPlanes=new Planes();
      this.MyThings=(Things) new Things();
      // We must also connect screen to V. Don't forget it. Do it in Init(), OK?      
      this.V=DM.V;
      this.I=DM.I;
      // Span functions
      DrawSpan=new R_DrawSpanUnrolled2();
      DrawSpanLow=new R_DrawSpanLow();
      DrawTranslatedColumn=new R_DrawTranslatedColumn();
      DrawTLColumn=new R_DrawTLColumn();
      DrawFuzzColumn=new R_DrawFuzzColumn();
      DrawColumn=new R_DrawColumnBoom();//new R_DrawColumnBoom();
      DrawColumnPlayer=DrawColumn;//new R_DrawColumn();
      DrawColumnLow=DrawColumn;
    
  }

    
    
    public Things getThings(){
        return (Things) this.MyThings;
    }
    
  
  
  private final class Segs extends SegDrawer{


      public Segs(){
          super();
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
        	  //try{
        	  // MAES: the sum of angles here must not exceed the tangent threshold, aka
        	  // roughly 90 degrees. No idea how they guaranteed that, however it may lead 
        	  // to the occasional overflow at certain view angles. Safe range for
        	  // tangents is just 12 bits, not 13. Therefore we either use
        	  // a special shifting by 20 positions instead of 19, or we catch
        	  // these errors. It's extremely hard to replicate, as well.
              // Another anomalous spot, the computation are ok-ish if performed as signed numbers.
              // xtoviewangle is normall -/+ 45 degrees 
        	      
              angle = Tables.toBAMIndex((int)rw_centerangle + (int)xtoviewangle[rw_x]);
              //angle = (int) (((rw_centerangle + (int)xtoviewangle[rw_x])&BITS32)>>>ANGLETOFINESHIFT);
              texturecolumn = rw_offset-FixedMul(finetangent[angle],rw_distance);
              
        	  //} catch (ArrayIndexOutOfBoundsException e){
        		//  System.err.println(">>>>>>>>>>>> Texture offset off bounds! <<<<<<<<<<<<");
        	  //}
    
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
              dc_texheight = TexMan.getTextureheight(midtexture)>>FRACBITS; // killough
              dc_texturemid = rw_midtexturemid;              
              dc_source = GetColumn(midtexture,texturecolumn);
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
                  dc_texheight=TexMan.getTextureheight(toptexture)>>FRACBITS;
                  if (DEBUG); 
                      //System.out.println("Drawing column"+(texturecolumn&127)+" of top texture "+textures[toptexture].name+ " at "+dc_yl+" "+dc_yh+" middle of texture at "+(dc_texturemid>>FRACBITS));
                  dc_source = GetColumn(toptexture,texturecolumn);
                  //dc_source_ofs=0;
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
                  dc_texheight=TexMan.getTextureheight(bottomtexture)>>FRACBITS;
              if (TexMan.CheckTextureNameForNum(bottomtexture).equalsIgnoreCase("REDWALL")){
                  System.out.println("REDWALL!");
              }
                  dc_source = GetColumn(bottomtexture,
                              texturecolumn);
                  //System.out.println("Max data length:"+dc_source.length);
                  try {
                  colfunc.invoke();
                  } catch (ArrayIndexOutOfBoundsException e){
                      System.err.printf("Error: %s on max len of %d for texture %s\n",e.getMessage(),dc_source.length, TexMan.CheckTextureNameForNum(bottomtexture));
                  }
                  
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
          if (ds_p == MAXDRAWSEGS){
              ResizeDrawsegs();
              //return;     
          }
          
              
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
           * Getting their abs value is optional.
           */  
          offsetangle = (int)rw_normalangle-(int)rw_angle1;
          
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
          midtexture = TexMan.getTextureTranslation(sidedef.midtexture);
          // a single sided line is terminal, so it must mark ends
          markfloor = markceiling = true;
          if ((linedef.flags & ML_DONTPEGBOTTOM)!=0)
          {
              vtop = frontsector.floorheight +
              TexMan.getTextureheight(sidedef.midtexture);
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
          if (frontsector.ceilingpic == TexMan.getSkyFlatNum() 
              && backsector.ceilingpic == TexMan.getSkyFlatNum())
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
              toptexture = TexMan.getTextureTranslation(sidedef.toptexture);
              if ((linedef.flags & ML_DONTPEGTOP)!=0)
              {
              // top of texture at top
              rw_toptexturemid = worldtop;
              }
              else
              {
              vtop =
                  backsector.ceilingheight
                  + TexMan.getTextureheight(sidedef.toptexture);
              
              // bottom of texture
              rw_toptexturemid = vtop - viewz;    
              }
          }
          if (worldlow > worldbottom)
          {
              // bottom texture
              bottomtexture = TexMan.getTextureTranslation(sidedef.bottomtexture);

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
          && frontsector.ceilingpic != TexMan.getSkyFlatNum())
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
          // FIXME: problem: certain ranges of visplanes are not checked at all.
          if (markceiling){
              //System.out.println("Markceiling");
          ceilingplane = MyPlanes.CheckPlane(ceilingplane, rw_x, rw_stopx-1);
          }
          
          if (markfloor){
              //System.out.println("Markfloor");
          floorplane = MyPlanes.CheckPlane (floorplane, rw_x, rw_stopx-1);
          }

          RenderSegLoop ();

          
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
  
  protected final class Planes implements PlaneDrawer{

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
      @Override
    public  void InitPlanes ()
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
      
      @Override
      public final void
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

      boolean render_precise=true;
      
      if (!render_precise){
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
      } else {
          // Accuratized version, from Boom/PrBoom+
          float slope, realy;
          
          distance = FixedMul (planeheight, yslope[y]);
          slope = (float)(planeheight / 65535.0f / Math.abs(centery - y));
          realy = (float)distance / 65536.0f;

          ds_xstep = (int) (viewsin * slope * viewfocratio);
          ds_ystep = (int) (viewcos * slope * viewfocratio);

          ds_xfrac =  viewx + (int)(viewcos * realy) + (x1 - centerx) * ds_xstep;
          ds_yfrac = -viewy - (int)(viewsin * realy) + (x1 - centerx) * ds_ystep;
          
      }
          // FIXME: alternate, more FPU-friendly implementation.
          //dlength = (distance);//*distscalef[x1];
          //dangle = (float) (2*Math.PI*(double)((viewangle +xtoviewangle[x1])&BITS32)/((double)0xFFFFFFFFL));
          // = viewx + (int)(Math.cos(dangle)* dlength);
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
      
      @Override
      public final void ClearPlanes ()
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
        
      @Override
      public final void DrawPlanes () 
      {
    	  if(DEBUG) System.out.println(" >>>>>>>>>>>>>>>>>>>>>   DrawPlanes: "+ lastvisplane);
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
             if (DEBUG2) System.out.println(pln);
              
          if (pln.minx > pln.maxx)
              continue;
          // sky flat
          if (pln.picnum == TexMan.getSkyFlatNum())
          {
              // Cache skytexture stuff here. They aren't going to change while
              // being drawn, after all, are they?
              int skytexture=TexMan.getSkyTexture();
              dc_texheight=TexMan.getTextureheight(skytexture)>>FRACBITS;
              
              dc_iscale = pspriteiscale>>detailshift;
              
              /* Sky is allways drawn full bright,
               * i.e. colormaps[0] is used.
               * Because of this hack, sky is not affected
               * by INVUL inverse mapping.
               */    
              dc_colormap = colormaps[0];
              dc_texturemid = TexMan.getSkyTextureMid();
              for (x=pln.minx ; x <= pln.maxx ; x++)
              {
            
              dc_yl = pln.getTop(x);
              dc_yh = pln.getBottom(x);
              
              if (dc_yl <= dc_yh)
              {
                  angle = (int) (addAngles(viewangle, xtoviewangle[x])>>>ANGLETOSKYSHIFT);
                  dc_x = x;
                  // Optimized: texheight is going to be the same during normal skies drawing...right? 
                  dc_source = GetColumn(skytexture,angle);
                  colfunc.invoke();
              }
              }
              continue;
          }
          
          // regular flat
          ds_source = ((flat_t)W.CacheLumpNum(TexMan.getFlatTranslation(pln.picnum),
                         PU_STATIC,flat_t.class)).data;
          
          
          if (ds_source.length==0){
              System.err.printf("YOU READ SHIT %s %d %d %d\n ", W.GetNameForNum(TexMan.getFlatTranslation(pln.picnum)),TexMan.getFlatTranslation(pln.picnum),pln.picnum, ds_source.length);
          }
          
          planeheight = Math.abs(pln.height-viewz);
          light = (pln.lightlevel >> LIGHTSEGSHIFT)+extralight;

          if (light >= LIGHTLEVELS)
              light = LIGHTLEVELS-1;

          if (light < 0)
              light = 0;

          planezlight = zlight[light];

          // We set those values at the border of a plane's top to a "sentinel" value...ok.
          pln.setTop(pln.maxx+1,visplane_t.SENTINEL);
          pln.setTop(pln.minx-1, visplane_t.SENTINEL);
          
          stop = pln.maxx + 1;

          
          for (x=pln.minx ; x<= stop ; x++) {
        	  MakeSpans(x,pln.getTop(x-1),
              pln.getBottom(x-1),
              pln.getTop(x),
              pln.getBottom(x));
          	}
          
          //Z_ChangeTag (ds_source, PU_CACHE);
          }
      }

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
      
  } // End Plane class
      

  
 
 


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
  
  // Check for new console commands.
  //NetUpdate ();
  
  // FIXME: "Warped floor" fixed, now to fix same-height visplane bleeding.
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
 //viewangle = addAngles(player.mo.angle , viewangleoffset);
 viewangle = player.mo.angle&BITS32;
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
  
public void Init ()

{
	drawsegs=new drawseg_t[MAXDRAWSEGS];
	C2JUtils.initArrayOfObjects(drawsegs);
	
	// DON'T FORGET ABOUT MEEEEEE!!!11!!!
	this.screen=V.getScreen(0);
	
	InitData ();
   System.out.print("\nR_InitData");
   //InitPointToAngle ();
   System.out.print("\nR_InitPointToAngle");
   InitTables ();
   // ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
   System.out.print ("\nR_InitTables");

   SetViewSize (DM.M.getScreenBlocks(), DM.M.getDetailLevel());
   MyPlanes.InitPlanes ();
   System.out.print ("\nR_InitPlanes");
   InitLightTables ();
   System.out.print("\nR_InitLightTables");
   
   System.out.print("\nR_InitSkyMap: "+TexMan.InitSkyMap ());
   InitTranslationTables ();
   System.out.print("\nR_InitTranslationsTables");
   
   framecount = 0;
}

/**
 * R_ExecuteSetViewSize
 */

public void ExecuteSetViewSize ()
{
    int cosadj;
    int dy;
    int     i,j,cheight;
    int     level, startmap;   
    
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
    
    if (C2JUtils.flags(wide_ratio,4))
    {
      wide_centerx = centerx;
      cheight = SCREENHEIGHT * BaseRatioSizes[wide_ratio].multiplier / 48;
    }
    else
    {
      wide_centerx = centerx * BaseRatioSizes[wide_ratio].multiplier / 48;
      cheight = SCREENHEIGHT;
    }
    
    // e6y: wide-res
    projection = wide_centerx<<FRACBITS;

  // proff 11/06/98: Added for high-res
    projectiony = ((cheight * centerx * 320) / 200) / SCREENWIDTH * FRACUNIT;
    // e6y: this is a precalculated value for more precise flats drawing (see R_MapPlane)
    viewfocratio = (1.6f * centerx / wide_centerx) / ((float)SCREENWIDTH / (float)cheight);
    

    // High detail
    if (detailshift==0)
    {
        
    colfunc = basecolfunc =DrawColumn;
    fuzzcolfunc = DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    glasscolfunc=DrawTLColumn;
    playercolfunc=DrawColumnPlayer;
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
    
    
    /*pspritescale=(int) (FRACUNIT*((float)SCREEN_MUL*viewwidth)/SCREENWIDTH);
    pspriteiscale = (int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));*/
    
    // psprite scales
    // proff 08/17/98: Changed for high-res
    // proff 11/06/98: Added for high-res
    // e6y: wide-res TODO: they won't work correctly for now. Fuck this.
    //pspritexscale = (wide_centerx << FRACBITS) / 160;
    //pspriteyscale = (((cheight*viewwidth)/SCREENWIDTH) << FRACBITS) / 200;
    //pspriteiscale = FixedDiv (FRACUNIT, pspritexscale);
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
    {  // killough 5/2/98: reformatted
    dy = Math.abs((i-viewheight/2)<<FRACBITS)+FRACUNIT/2;    
    MyPlanes.yslope[i] = FixedDiv(projectiony, dy);
 // proff 08/17/98: Changed for high-res
    MyPlanes.yslopef[i] = (projectiony/*(viewwidth<<detailshift)/2*/)/ dy;
    }
    
    for (i=0 ; i<viewwidth ; i++)
    {
    // MAES: In this spot we must interpet it as SIGNED, else it's pointless, right?
    // MAES: this spot caused the "warped floor bug", now fixed. Don't forget xtoviewangle[i]!    
    cosadj = Math.abs(finecosine(xtoviewangle[i]));
    MyPlanes.distscale[i] = FixedDiv (FRACUNIT,cosadj);
    }
    
    // Calculate the light levels to use
    //  for each level / scale combination.
    for (i=0 ; i< LIGHTLEVELS ; i++)
    {
    startmap = ((LIGHTLEVELS-1-i)*2)*NUMCOLORMAPS/LIGHTLEVELS;
    for (j=0 ; j<MAXLIGHTSCALE ; j++)
    {
        level = startmap - j/**SCREENWIDTH/(viewwidth<<detailshift)*//DISTMAP;
        
        if (level < 0)
        level = 0;

        if (level >= NUMCOLORMAPS)
        level = NUMCOLORMAPS-1;

        scalelight[i][j] = colormaps[level];
    }
    }
}


 
  
  
}
