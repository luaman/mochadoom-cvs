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
import rr.RendererState.R_DrawColumnBoom;
import rr.RendererState.R_DrawFuzzColumn;
import rr.RendererState.R_DrawSpanLow;
import rr.RendererState.R_DrawSpanUnrolled;
import rr.RendererState.R_DrawTLColumn;
import rr.RendererState.R_DrawTranslatedColumn;
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
      this.MySegs=new ParallelSegs();
      this.MyBSP=new BSP();
      this.MyPlanes=new ParallelPlanes();
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
  
  private final class ParallelSegs extends SegDrawer{

      public ParallelSegs(){
          super();
      }

      //
      // R_RenderMaskedSegRange
      //
      

      
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
          texnum = TexMan.getTextureTranslation(curline.sidedef.midtexture);
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
          dc_texturemid = dc_texturemid + TexMan.getTextureheight(texnum) - viewz;
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
                  
              DrawMaskedColumn (col);
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
                angle = Tables.toBAMIndex(rw_centerangle + (int)xtoviewangle[rw_x]);
                //angle = (int) (((rw_centerangle + xtoviewangle[rw_x])&BITS31)>>>ANGLETOFINESHIFT);
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
              dc_texheight = Math.min(TexMan.getTextureheight(midtexture)>>FRACBITS,128); // killough
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
                  dc_texheight=TexMan.getTextureheight(toptexture)>>FRACBITS;
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
                  dc_texheight=TexMan.getTextureheight(bottomtexture)>>FRACBITS;
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


      
      }
  
  protected final class ParallelPlanes implements PlaneDrawer{
   
      
      public ParallelPlanes (){
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
      public void InitPlanes ()
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
      
      @Override
      public void ClearPlanes ()
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
	      cachedheight=MyPlanes.getCachedHeight();
	      cacheddistance=MyPlanes.getCachedDistance();
	      cachedxstep=MyPlanes.getCachedXStep();
	      cachedystep=MyPlanes.getCachedYStep();
	      distscale=MyPlanes.getDistScale();
	      yslope=MyPlanes.getYslope();

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
        
			 for (int pl= this.id; pl <lastvisplane; pl+=NUMFLOORTHREADS) {
             pln=visplanes[pl];
            // System.out.println(id +" : "+ pl);
             
         if (pln.minx > pln.maxx)
             continue;

         
         // sky flat
         if (pln.picnum == TexMan.getSkyFlatNum() )
         {
             dc_iscale = skyscale>>detailshift;
             
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
                 dc_texheight=TexMan.getTextureheight(TexMan.getSkyTexture())>>FRACBITS;
                 dc_source = GetColumn(TexMan.getSkyTexture(), angle);
                 colfunc.invoke();
             }
             }
             continue;
         }
         
         // regular flat
         vpw_ds_source = ((flat_t)W.CacheLumpNum(TexMan.getFlatTranslation(pln.picnum),
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
    MyPlanes.getYslope()[i] = FixedDiv ( (viewwidth<<detailshift)/2*FRACUNIT, dy);
    //MyPlanes.yslopef[i] = ((viewwidth<<detailshift)/2)/ dy;
    }
    
    //double cosadjf;
    for (i=0 ; i<viewwidth ; i++)
    {
    // MAES: In this spot we must interpet it as SIGNED, else it's pointless, right?
    // MAES: this spot caused the "warped floor bug", now fixed. Don't forget xtoviewangle[i]!    
    cosadj = Math.abs(finecosine(xtoviewangle[i]));
    //cosadjf = Math.abs(Math.cos((double)xtoviewangle[i]/(double)0xFFFFFFFFL));
    MyPlanes.getDistScale()[i] = FixedDiv (FRACUNIT,cosadj);
    //MyPlanes.distscalef[i] = (float) (1.0/cosadjf);
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
