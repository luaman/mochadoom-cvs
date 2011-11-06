package rr;

import static data.Limits.*;
import static data.Tables.*;
import static m.fixed_t.*;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import utils.C2JUtils;
import v.DoomVideoRenderer;
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
					
					dcvars.dc_source = GetCachedColumn(midtexture,texturecolumn);
					dc_m=dcvars.dc_source_ofs;
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
						    dcvars.dc_source = GetCachedColumn(toptexture,texturecolumn);
			                dc_t=dcvars.dc_source_ofs;
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
						    dcvars.dc_source = GetCachedColumn(bottomtexture,texturecolumn);
		                    dc_b=dcvars.dc_source_ofs;
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
				ResizeRSIBuffer();
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
			RSIExec[i]=new RenderSegExecutor(SCREENWIDTH,SCREENHEIGHT,i,screen,
					this, TexMan, RSI,MySegs.getBLANKCEILINGCLIP(),MySegs.getBLANKFLOORCLIP(), MySegs.getCeilingClip(), MySegs.getFloorClip(), columnofs, 
					xtoviewangle, ylookup, this.visplanes,walllights, this.visplanebarrier);
			RSIExec[i].setVideoScale(this.vs);
			RSIExec[i].initScaling();
			// Each SegExecutor sticks to its own half (or 1/nth) of the screen.
			RSIExec[i].setScreenRange(i*(SCREENWIDTH/NUMWALLTHREADS),(i+1)*(SCREENWIDTH/NUMWALLTHREADS));
			detailaware.add(RSIExec[i]);
			
		}
		

        for (int i=0;i<NUMFLOORTHREADS;i++){
            vpw[i]=new VisplaneWorker(i,SCREENWIDTH,SCREENHEIGHT,columnofs,ylookup, screen,visplanebarrier,NUMFLOORTHREADS);
            detailaware.add(vpw[i]);
        }
	}

	/** Resizes RWI buffer, updates executors. Sorry for the hackish implementation
	 *  but ArrayList and pretty much everything in Collections is way too slow
	 *  for what we're trying to accomplish.
	 * 
	 */

	private void ResizeRSIBuffer() {
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
		MySegs.ClearClips();
		VIS.ClearSprites ();

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

		colfunc=basecolfunc;

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
		this.screen=V.getScreen(DoomVideoRenderer.SCREEN_FG);

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
		
		System.out.print("\nR_InitDrawingFunctions: ");
        R_InitDrawingFunctions();       		

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
