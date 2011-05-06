package rr;

import static data.Defines.*;
import static data.Limits.*;
import static data.Tables.*;
import static m.fixed_t.*;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import utils.C2JUtils;
import doom.DoomMain;
import doom.player_t;

/** This is Mocha Doom's famous parallel software renderer. It builds
 *  on the basic software renderer, but adds specialized handling for 
 *  drawing segs (walls) and spans (floors) in parallel. There's inherent
 *  parallelism between walls and floor, and internal parallelism between
 *  walls and between floors. However, visplane limits and openings
 *  need to be pre-computed before any actual drawing starts, that's why
 *  rendering of walls is stored in "RWI"s or "Render Wall Instructions",
 *  and then rendered once they are all in place and the can be parallelized
 *  between rendering threads. 
 *  
 *  Rendering of sprites is NOT parallelized yet (and probably not worth it,
 *  at this point).
 * 
 * @author admin
 *
 */

public class ParallelRenderer extends RendererState  {
	////////// PARALLEL OBJECTS /////////////
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
      
      /** Parallel version. Since there's so much crap to take into account when rendering, the number of
       * walls to render is unknown a-priori and the BSP trasversal itself is not worth parallelizing,
       * it makes more sense to store "rendering instructions" as quickly as the BSP can be transversed,
       * and then execute those in parallel. Also saves on having to duplicate way too much status.
       */
      
      @Override
      protected final void CompleteColumn(){
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
  
  protected final class ParallelPlanes extends PlaneDrawer{
   
      
      public ParallelPlanes (){
          C2JUtils.initArrayOfObjects(visplanes);
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

  
}
