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
import data.Tables;
import doom.DoomMain;
import doom.player_t;

/** This is a second attempt at building a seg-focused parallel renderer, instead of
 * column-based. It does function, but is broken and has unsolved data dependencies.
 * It's therefore not used in official releases, and I chose to deprecate it.
 * If you still want to develop it, be my guest.
 * 
 * @author velktron
 *
 */

public class ParallelRenderer2 extends RendererState  {

	////////// PARALLEL OBJECTS /////////////
	private final int NUMWALLTHREADS;
	private int NUMFLOORTHREADS;
	private Executor tp;
	private VisplaneWorker[] vpw;

	// private CyclicBarrier wallbarrier;
	private CyclicBarrier visplanebarrier;

	private RenderSegInstruction[] RSI;

	/** Increment this as you submit RWI to the "queue". Remember to reset to 0 when you have drawn everything!
	 * 
	 */
	private int RSIcount=0;

	private RenderSegExecutor[] RSIExec;



	private static final boolean DEBUG=false;

	public ParallelRenderer2(DoomMain DM, int wallthread, int floorthreads) {

		super(DM);
		this.MySegs=new ParallelSegs();
		this.MyPlanes=new ParallelPlanes();		

		this.NUMWALLTHREADS=wallthread;
		this.NUMFLOORTHREADS=floorthreads;

	}

	/** Default constructor, 2 wall threads and one floor thread.
	 * 
	 * @param DM
	 */
	public ParallelRenderer2(DoomMain DM) {
		this(DM,2,1);
	}

	private void initializeParallelStuff() {
		// Prepare parallel stuff
		RSIExec=new RenderSegExecutor[NUMWALLTHREADS];
		tp=   Executors.newFixedThreadPool(NUMWALLTHREADS+NUMFLOORTHREADS);
		// Prepare the barrier for MAXTHREADS + main thread.
		//wallbarrier=new CyclicBarrier(NUMWALLTHREADS+1);
		visplanebarrier=new CyclicBarrier(NUMFLOORTHREADS+NUMWALLTHREADS+1);

		vpw=new VisplaneWorker[NUMFLOORTHREADS];

		for (int i=0;i<NUMFLOORTHREADS;i++){
			vpw[i]=new VisplaneWorker(visplanebarrier);
			vpw[i].id=i;
		}
	}

	private final class ParallelSegs extends SegDrawer{

		public ParallelSegs(){
			super();
		}     

		@Override
		protected void RenderSegLoop () 
		{
			int     angle; // angle_t
			//int     index;
			int         yl; // low
			int         yh; // hight
			int         mid;
			int     texturecolumn=0; // fixed_t
			int         top;
			int         bottom;
			int dc_b=0, dc_m=0,dc_t = 0;

			// Generate Seg rendering instruction BEFORE the looping start
			// and anything is modified. The loop will be repeated in the
			// threads, but without marking ceilings/floors etc.
			GenerateRSI();

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

				//System.out.printf("Precompute: rw %d yl %d yh %d\n",rw_x,yl,yh);

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
					// calculate texture offset. Still important to do because of masked

					angle = Tables.toBAMIndex(rw_centerangle + (int)xtoviewangle[rw_x]);
					texturecolumn = rw_offset-FixedMul(finetangent[angle],rw_distance);
					texturecolumn >>= FRACBITS;
				}

				// Don't to any drawing, only compute bounds.
				if (midtexture!=0)
				{
					
					dc_source = GetCachedColumn(midtexture,texturecolumn);
					dc_m=dc_source_ofs;
					// single sided line
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
			                dc_source = GetCachedColumn(toptexture,texturecolumn);
			                dc_t=dc_source_ofs;
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
		                    dc_source = GetCachedColumn(bottomtexture,texturecolumn);
		                    dc_b=dc_source_ofs;
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

		protected final void GenerateRSI(){

			if (RSIcount>=RSI.length){
				ResizeRWIBuffer();
			}

			RenderSegInstruction rsi=RSI[RSIcount];
			rsi.centery=centery;
			rsi.bottomfrac=bottomfrac;
			rsi.bottomstep=bottomstep;
			rsi.bottomtexture=bottomtexture;
			rsi.markceiling=markceiling;
			rsi.markfloor=markfloor;
			rsi.midtexture=midtexture;
			rsi.pixhigh=pixhigh;
			rsi.pixhighstep=pixhighstep;
			rsi.pixlow=pixlow;
			rsi.pixlowstep=pixlowstep;
			rsi.rw_bottomtexturemid=rw_bottomtexturemid;
			rsi.rw_centerangle=rw_centerangle;
			rsi.rw_distance=rw_distance;
			rsi.rw_midtexturemid=rw_midtexturemid;
			rsi.rw_offset=rw_offset;
			rsi.rw_scale=rw_scale;
			rsi.rw_scalestep=rw_scalestep;
			rsi.rw_stopx=rw_stopx;
			rsi.rw_toptexturemid=rw_toptexturemid;
			rsi.rw_x=rw_x;
			rsi.segtextured=segtextured;
			rsi.topfrac=topfrac;
			rsi.topstep=topstep;
			rsi.toptexture=toptexture;
			rsi.walllights=walllights;
			rsi.viewheight=viewheight;
			//rsi.floorplane=floorplane;
			//rsi.ceilingplane=ceilingplane;
			RSIcount++;
		}


		@Override
		protected void CompleteColumn() {
			// TODO Auto-generated method stub

		}



	}

	protected final class ParallelPlanes extends PlaneDrawer{

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
		      spanstart=new int[SCREENHEIGHT];
		      spanstop=new int [SCREENHEIGHT];
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
	                 dc_source = GetCachedColumn(TexMan.getSkyTexture(), angle);
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

	      private final void MakeSpans(int x, int t1, int b1, int t2, int b2) {
	          
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
	          if (detailshift==0)
	        	  spanfunc();
	          else
	        	  spanfunclow();          
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
	      
	   // Each thread has its own copy of a colfun.
	      private void spanfunclow(){
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
	    	     
	    	     count = ds_x2 - ds_x1 + 1;
	    	     
	 			// Blocky mode, need to multiply by 2.
	 			ds_x1 <<= 1; 			
	    	     
	 			dest = ylookup[vpw_ds_y] + columnofs[ds_x1];
	 			
	    	     while (count >= 4) {
	    	         ytemp = position >> 4;
	    	         ytemp = ytemp & 4032;
	    	         xtemp = position >>> 26;
	    	         spot = xtemp | ytemp;
	    	         position += step;         
	    	         screen[dest] = colormap[0x00FF&source[spot]];
	    	         screen[dest+1] = colormap[0x00FF&source[spot]];
	    	         ytemp = position >> 4;
	    	         ytemp = ytemp & 4032;
	    	         xtemp = position >>> 26;
	    	         spot = xtemp | ytemp;
	    	         position += step;
	    	         screen[dest+2] = colormap[0x00FF&source[spot]];
	    	         screen[dest+3] = colormap[0x00FF&source[spot]];
	    	         ytemp = position >> 4;
	    	         ytemp = ytemp & 4032;
	    	         xtemp = position >>> 26;
	    	         spot = xtemp | ytemp;
	    	         position += step;
	    	         screen[dest+4] = colormap[0x00FF&source[spot]];
	    	         screen[dest+5] = colormap[0x00FF&source[spot]];
	    	         ytemp = position >> 4;
	    	         ytemp = ytemp & 4032;
	    	         xtemp = position >>> 26;
	    	         spot = xtemp | ytemp;
	    	         position += step;
	    	         screen[dest+6] = colormap[0x00FF&source[spot]];
	    	         screen[dest+7] = colormap[0x00FF&source[spot]];
	    	         count -= 4;
	    	         dest += 8;
	    	     }
	    	     
	    	     while (count > 0) {
	    	         ytemp = position >> 4;
	    	         ytemp = ytemp & 4032;
	    	         xtemp = position >>> 26;
	    	         spot = xtemp | ytemp;
	    	         position += step;
	    	         screen[dest++] = colormap[0x00FF&source[spot]];
	    	         screen[dest++] = colormap[0x00FF&source[spot]];
	    	         count--;
	    	     }
	    	 }
	      
	      // Private to each thread.
	      int[]           spanstart;
	      int[]           spanstop;
	      CyclicBarrier barrier;
	      
	  }
	
	////////////////// Generic rendering methods /////////////////////


	/**
	 * R_InitRWISubsystem
	 * 
	 * Initialize RSIs and RSI Executors.
	 * Pegs them to the RSI, ylookup and screen[0].
	 */

	private void InitRSISubsystem() {
		// CATCH: this must be executed AFTER screen is set, and
		// AFTER we initialize the RWI themselves,
		// before V is set (right?) 
		
		//offsets=new int[NUMWALLTHREADS];
		for (int i=0;i<NUMWALLTHREADS;i++){
			RSIExec[i]=new RenderSegExecutor(i,screen,
					this, TexMan, RSI,this.BLANKCEILINGCLIP,this.BLANKFLOORCLIP, ceilingclip, floorclip, columnofs, 
					xtoviewangle, ylookup, this.visplanes,walllights, this.visplanebarrier);
			RSIExec[i].setVideoScale(this.vs);
			RSIExec[i].initScaling();
			// Each SegExecutor sticks to its own half (or 1/nth) of the screen.
			RSIExec[i].setScreenRange(i*(SCREENWIDTH/NUMWALLTHREADS),(i+1)*(SCREENWIDTH/NUMWALLTHREADS));
		}
	}

	/** Resizes RWI buffer, updates executors. Sorry for the hackish implementation
	 *  but ArrayList and pretty much everything in Collections is way too slow
	 *  for what we're trying to accomplish.
	 * 
	 */

	private void ResizeRWIBuffer() {
		RenderSegInstruction[] tmp=new RenderSegInstruction[RSI.length*2];
		System.arraycopy(RSI, 0, tmp, 0, RSI.length);

		C2JUtils.initArrayOfObjects(tmp,RSI.length,tmp.length);

		// Bye bye, old RWI.
		RSI=tmp;   

		for (int i=0;i<NUMWALLTHREADS;i++){
			RSIExec[i].updateRSI(RSI);
		}

		System.out.println("RWI Buffer resized. Actual capacity "+RSI.length);
	}


	private void RenderRSIPipeline() {

		for (int i=0;i<NUMWALLTHREADS;i++){
			RSIExec[i].setRSIEnd(RSIcount);
			//RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
			tp.execute(RSIExec[i]);
		}

		//System.out.println("RWI count"+RWIcount);
		RSIcount=0;
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



		/* Uncommenting this will result in a very existential experience
  if (Math.random()>0.999){
	  thinker_t shit=P.getRandomThinker();
	  try {
	  mobj_t crap=(mobj_t)shit;
	  player.mo=crap;
	  } catch (ClassCastException e){

	  }
  	}*/

		// Clear buffers. 
		MyBSP.ClearClipSegs ();
		MyBSP.ClearDrawSegs ();
		MyPlanes.ClearPlanes ();
		MyThings.ClearSprites ();

		// Check for new console commands.
		DGN.NetUpdate ();

		// The head node is the last node output.
		MyBSP.RenderBSPNode (LL.numnodes-1);
		
		RenderRSIPipeline();
		// Check for new console commands.
		DGN.NetUpdate ();

		// "Warped floor" fixed, same-height visplane merging fixed.
		MyPlanes.DrawPlanes ();

		try {
			visplanebarrier.await();
		} catch (Exception e){
			e.printStackTrace();
		}



		// Check for new console commands.
		DGN.NetUpdate ();

		MyThings.DrawMasked ();


		// Check for new console commands.
		DGN.NetUpdate ();           
	}

	/**
	 * R_Init
	 */

	//public int  detailLevel;
	//public int  screenblocks=9; // has defa7ult

	public void Init ()

	{
		// Any good reason for this to be here?
		//drawsegs=new drawseg_t[MAXDRAWSEGS];
		//C2JUtils.initArrayOfObjects(drawsegs);

		// DON'T FORGET ABOUT MEEEEEE!!!11!!!
		this.screen=V.getScreen(0);

		System.out.print("\nR_InitData");
		InitData ();
		//InitPointToAngle ();
		System.out.print("\nR_InitPointToAngle");

		// ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
		System.out.print ("\nR_InitTables");
		InitTables ();

		SetViewSize (DM.M.getScreenBlocks(), DM.M.getDetailLevel());

		System.out.print ("\nR_InitPlanes");
		MyPlanes.InitPlanes ();

		System.out.print("\nR_InitLightTables");
		InitLightTables ();

		System.out.print("\nR_InitSkyMap: "+TexMan.InitSkyMap ());

		System.out.print("\nR_InitTranslationsTables");
		InitTranslationTables ();

		System.out.print("\nR_InitRWISubsystem: ");
		InitRSISubsystem();

		System.out.print("\nR_InitTranMap: ");
		R_InitTranMap(0);

		framecount = 0;
	}

	@Override
	public void initScaling(){
		super.initScaling();
		this.RSI=new RenderSegInstruction[MAXSEGS*3];
		C2JUtils.initArrayOfObjects(RSI);

		initializeParallelStuff();
	}


}
