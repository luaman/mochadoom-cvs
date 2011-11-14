package rr;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rr.drawfuns.ColVars;
import rr.drawfuns.DcFlags;
import rr.drawfuns.R_DrawColumnBoom;
import rr.drawfuns.R_DrawColumnBoomLow;
import rr.drawfuns.R_DrawFuzzColumn;
import rr.drawfuns.R_DrawFuzzColumnLow;
import rr.drawfuns.R_DrawTranslatedColumn;
import rr.drawfuns.R_DrawTranslatedColumnLow;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import doom.DoomMain;
import doom.player_t;

/**
 * This is Mocha Doom's famous parallel software renderer. It builds on the
 * basic software renderer, but adds specialized handling for drawing segs
 * (walls) and spans (floors) in parallel. There's inherent parallelism between
 * walls and floor, and internal parallelism between walls and between floors.
 * However, visplane limits and openings need to be pre-computed before any
 * actual drawing starts, that's why rendering of walls is stored in "RWI"s or
 * "Render Wall Instructions", and then rendered once they are all in place and
 * the can be parallelized between rendering threads. Rendering of sprites is
 * NOT parallelized yet (and probably not worth it, at this point).
 * 
 * @author admin
 */

public class ParallelRenderer
        extends RendererState {

    // //////// PARALLEL OBJECTS /////////////
    private final int NUMWALLTHREADS;

    private final int NUMMASKEDTHREADS;
    
    private int NUMFLOORTHREADS;

    private Executor tp;

    private Runnable[] vpw;
    
    private MaskedWorker[] maskedworkers;

    private CyclicBarrier drawsegsbarrier;
    private CyclicBarrier visplanebarrier;
    private CyclicBarrier maskedbarrier;

    /**
     * Stored wall rendering instructions. They can be at most 3*SCREENWIDTH (if
     * there are low, mid and high textures on every column of the screen)
     * Remember to init them and set screen and ylookup for all of them. Their
     * max number is static and work partitioning can be done in any way, as
     * long as you keep track of how many there are in any given frame. This
     * value is stored inside RWIcount. TODO: there are cases when more than
     * 3*SCREENWIDTH instructions need to be stored. therefore we really need a
     * resizeable array here, but ArrayList is way too slow for our needs.
     * Storing a whole wall is not an option, as, once again, a wall may have a
     * variable number of columns and an irregular height profile -> we'd need
     * to look into visplanes ugh...
     */

    private ColVars<byte[],short[]>[] RWI,RMI;

    /**
     * Increment this as you submit RWI to the "queue". Remember to reset to 0
     * when you have drawn everything!
     */
    private int RWIcount = 0, RMIcount=0;

    private RenderWallExecutor[] RWIExec;
    
    private RenderMaskedExecutor[] RMIExec;

    private static final boolean DEBUG = false;

    public ParallelRenderer(DoomMain DM, int wallthread, int floorthreads,int nummaskedthreads) {
        super(DM);

        this.MyThings=new ParallelThings2();
        this.MySegs = new ParallelSegs();
        this.MyPlanes = new ParallelPlanes();

        this.NUMWALLTHREADS = wallthread;
        this.NUMFLOORTHREADS = floorthreads;
        this.NUMMASKEDTHREADS = nummaskedthreads;
        
    	smp_composite=new boolean[nummaskedthreads];// = false;
    	smp_lasttex=new int[nummaskedthreads];// = -1;
    	smp_lastlump=new int[nummaskedthreads];// = -1;
    	smp_lastpatch=new patch_t[nummaskedthreads];;// = null;
        
    }

    /**
     * Default constructor, 1 seg, 1 span and two masked threads.
     * 
     * @param DM
     */
    public ParallelRenderer(DoomMain DM) {
        this(DM, 1, 1,2);
    }

    private void initializeParallelStuff() {
        // Prepare parallel stuff
        RWIExec = new RenderWallExecutor[NUMWALLTHREADS];
        RMIExec = new RenderMaskedExecutor[NUMMASKEDTHREADS];        
        vpw = new Runnable[NUMFLOORTHREADS];
        maskedworkers=new MaskedWorker[NUMMASKEDTHREADS];
        
        tp = Executors.newCachedThreadPool();
        // Prepare the barrier for MAXTHREADS + main thread.
        drawsegsbarrier =
            new CyclicBarrier(NUMWALLTHREADS + 1);
        
        visplanebarrier =
            new CyclicBarrier(NUMFLOORTHREADS + 1);

        maskedbarrier =
            new CyclicBarrier(NUMMASKEDTHREADS + 1);

        
        RWIcount = 0;
    }

    private final class ParallelSegs
            extends SegDrawer {

        public ParallelSegs() {
            super();
        }

        /**
         * Parallel version. Since there's so much crap to take into account
         * when rendering, the number of walls to render is unknown a-priori and
         * the BSP trasversal itself is not worth parallelizing, it makes more
         * sense to store "rendering instructions" as quickly as the BSP can be
         * transversed, and then execute those in parallel. Also saves on having
         * to duplicate way too much status.
         */

        @Override
        protected final void CompleteColumn() {

            // Don't wait to go over
            if (RWIcount >= RWI.length) {
                ResizeRWIBuffer();
            }

            // A deep copy is still necessary, as dc
            RWI[RWIcount].copyFrom(dcvars);

            // We only need to point to the next one in the list.
            RWIcount++;
        }

    }

    protected final class ParallelPlanes
            extends PlaneDrawer {

        /**
         * R_DrawPlanes At the end of each frame. This also means that visplanes
         * must have been set BEFORE we called this function. Therefore, look
         * for errors behind.
         * 
         * @throws IOException
         */
        public void DrawPlanes() {
            if (DEBUG)
                System.out.println(" >>>>>>>>>>>>>>>>>>>>>   DrawPlanes: "
                        + lastvisplane);

            if (RANGECHECK) {
                if (ds_p > MAXDRAWSEGS)
                    I.Error("R_DrawPlanes: drawsegs overflow (%d)", ds_p);

                if (lastvisplane > MAXVISPLANES)
                    I.Error(" R_DrawPlanes: visplane overflow (%d)",
                        lastvisplane);

                if (lastopening > MAXOPENINGS)
                    I.Error("R_DrawPlanes: opening overflow (%d)", lastopening);
            }

            // vpw[0].setRange(0,lastvisplane/2);
            // vpw[1].setRange(lastvisplane/2,lastvisplane);

            for (int i = 0; i < NUMFLOORTHREADS; i++)
                tp.execute(vpw[i]);
        }

    } // End Plane class

    /** Overrides only the terminal drawing methods from things, 
     * using a mechanism very similar to column-based wall threading.
     * It's not very efficient, since some of the really heavy parts 
     * (such as visibility priority) are still done serially, and actually do 
     * take up a lot of the actual rendering time, and the number of columns generated 
     * is REALLY enormous (100K+ for something like nuts.wad), and the thing chokes 
     * on synchronization, more than anything.
     *  
     *  The only appropriate thing to do would be to have a per-vissprite 
     *  renderer, which would actually move much of the brunt work away from 
     *  the main thread.
     *  
     *  Some interesting benchmarks on nuts.wad timedemo:
     *  Normal things serial renderer: 60-62 fps
     *  "Dummy" completeColumns: 72 fps
     *  "Dummy" things renderer without final drawing: 80 fps
     *  "Dummy" things renderer without ANY calculations: 90 fps. 
     *  
     *  This means that even a complete parallelization will likely have 
     *  a quite limited impact.
     *  
     * @author velktron
     *
     */
    
    protected final class ParallelThings extends AbstractThings{
        
    	@Override
    	public void DrawMasked(){
    		super.DrawMasked();
    		RenderRMIPipeline();
    		
    		try {
				maskedbarrier.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
        @Override
        public void completeColumn(){
            
                if (detailshift==1)
                    flags=DcFlags.LOW_DETAIL;
                // Don't wait to go over
                if (RMIcount >= RMI.length) {
                    ResizeRMIBuffer();
                }

                // A deep copy is still necessary, as well as setting dc_flags
                RMI[RMIcount].copyFrom(maskedcvars,colfunc.getFlags());

                // We only need to point to the next one in the list.
                RMIcount++;
        }
        
        int flags;
        
    }  
    
    protected final class ParallelThings2 extends AbstractThings {
        
        
        public ParallelThings2(){
            
        }
        
         @Override
         public void DrawMasked() {

             VIS.SortVisSprites();

             for (int i=0;i<maskedworkers.length;i++){
                 tp.execute(maskedworkers[i]);                   
             }
             
             try {
                 maskedbarrier.await();
             } catch (InterruptedException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
             } catch (BrokenBarrierException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
             }
             
         }
         
         @Override
         public void completeColumn() {
             // Does nothing. Dummy.
         }
         
         @Override
         public void setPspriteScale(int scale){
             for (int i=0;i<maskedworkers.length;i++)
                 maskedworkers[i].setPspriteScale(scale);                
         }
         
         @Override
         public void setPspriteIscale(int scale){
             for (int i=0;i<maskedworkers.length;i++)
                 maskedworkers[i].setPspriteIscale(scale);                
         }
         
    }
    
    // ///////////////// Generic rendering methods /////////////////////

    /**
     * R_InitRWISubsystem Initialize RWIs and RWI Executors. Pegs them to the
     * RWI, ylookup and screen[0].
     */

    private void InitRWISubsystem() {
        // CATCH: this must be executed AFTER screen is set, and
        // AFTER we initialize the RWI themselves,
        // before V is set (right?)
        for (int i = 0; i < NUMWALLTHREADS; i++) {
            RWIExec[i] =
                new RenderWallExecutor(SCREENWIDTH, SCREENHEIGHT, columnofs,
                        ylookup, screen, RWI, drawsegsbarrier);
            
            detailaware.add(RWIExec[i]);
        }
    }
 
    private void InitRMISubsystem() {
    for (int i = 0; i < NUMMASKEDTHREADS; i++) {
        // Each masked executor gets its own set of column functions.
        
        RMIExec[i] =
            new RenderMaskedExecutor(SCREENWIDTH, SCREENHEIGHT, columnofs,
                    ylookup, screen, RMI, maskedbarrier,
                    // Regular masked columns
                    new R_DrawColumnBoom(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),
                    new R_DrawColumnBoomLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),
                    
                    // Fuzzy columns
                    new R_DrawFuzzColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I,BLURRY_MAP),
                    new R_DrawFuzzColumnLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I,BLURRY_MAP),

                    // Translated columns
                    new R_DrawTranslatedColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),
                    new R_DrawTranslatedColumnLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I)
                    );
            
        detailaware.add(RMIExec[i]);
    	}
    }
    
    private void InitPlaneWorkers(){
        for (int i = 0; i < NUMFLOORTHREADS; i++) {
            vpw[i] =
                new VisplaneWorker2(i,SCREENWIDTH, SCREENHEIGHT, columnofs,
                        ylookup, screen, visplanebarrier, NUMFLOORTHREADS);
            //vpw[i].id = i;
            
            detailaware.add((IDetailAware) vpw[i]);
        }
    }
    
    private void InitMaskedWorkers(){
        for (int i=0;i<NUMMASKEDTHREADS;i++){
            maskedworkers[i]=new MaskedWorker(i,SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, NUMMASKEDTHREADS,  screen,maskedbarrier);
            detailaware.add(maskedworkers[i]);
            // "Peg" to sprite manager.
            maskedworkers[i].cacheSpriteManager(SM);
        }

    }

    /**
     * Resizes RWI buffer, updates executors. Sorry for the hackish
     * implementation but ArrayList and pretty much everything in Collections is
     * way too slow for what we're trying to accomplish.
     */

    private void ResizeRWIBuffer() {
        ColVars<byte[],short[]> fake = new ColVars<byte[],short[]>();

        // Bye bye, old RWI.
        RWI = C2JUtils.resize(fake, RWI, RWI.length * 2);

        for (int i = 0; i < NUMWALLTHREADS; i++) {
            RWIExec[i].updateRWI(RWI);            
        	}
        //System.err.println("RWI Buffer resized. Actual capacity " + RWI.length);
    }
    
    private void ResizeRMIBuffer() {
        ColVars<byte[],short[]> fake = new ColVars<byte[],short[]>();
        ColVars<byte[],short[]>[] tmp =
            C2JUtils.createArrayOfObjects(fake, RMI.length * 2);
        System.arraycopy(RMI, 0, tmp, 0, RMI.length);

        // Bye bye, old RMI.
        RMI = tmp;

          for (int i = 0; i < NUMMASKEDTHREADS; i++) {
            RMIExec[i].updateRMI(RMI);
        }
        
        System.err.println("RMI Buffer resized. Actual capacity " + RMI.length);
    }

    private void RenderRWIPipeline() {

        for (int i = 0; i < NUMWALLTHREADS; i++) {

            RWIExec[i].setRange((i * RWIcount) / NUMWALLTHREADS,
                ((i + 1) * RWIcount) / NUMWALLTHREADS);
            // RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
            tp.execute(RWIExec[i]);
        }

        // System.out.println("RWI count"+RWIcount);
        RWIcount = 0;
    }
    
    private void RenderRMIPipeline() {

        for (int i = 0; i < NUMMASKEDTHREADS; i++) {

            RMIExec[i].setRange((i*this.SCREENWIDTH)/ NUMMASKEDTHREADS,
                ((i + 1) * this.SCREENWIDTH) / NUMMASKEDTHREADS);
            RMIExec[i].setRMIEnd(RMIcount);
            // RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
            tp.execute(RMIExec[i]);
        }

        // System.out.println("RWI count"+RWIcount);
        RMIcount = 0;
    }

    // /////////////////////// The actual rendering calls
    // ///////////////////////

    /**
     * R_RenderView As you can guess, this renders the player view of a
     * particular player object. In practice, it could render the view of any
     * mobj too, provided you adapt the SetupFrame method (where the viewing
     * variables are set).
     * 
     * @throws IOException
     */

    public void RenderPlayerView(player_t player) {

        // Viewing variables are set according to the player's mobj. Interesting
        // hacks like
        // free cameras or monster views can be done.
        SetupFrame(player);

        /*
         * Uncommenting this will result in a very existential experience if
         * (Math.random()>0.999){ thinker_t shit=P.getRandomThinker(); try {
         * mobj_t crap=(mobj_t)shit; player.mo=crap; } catch (ClassCastException
         * e){ } }
         */

        // Clear buffers.
        MyBSP.ClearClipSegs();
        MyBSP.ClearDrawSegs();
        MyPlanes.ClearPlanes();
        MySegs.ClearClips();
        VIS.ClearSprites();

        // Check for new console commands.
        DGN.NetUpdate();
        
        // The head node is the last node output.
        MyBSP.RenderBSPNode(LL.numnodes - 1);

        // System.out.printf("Submitted %d RWIs\n",RWIcount);

        RenderRWIPipeline();
        
        // Check for new console commands.
        DGN.NetUpdate();

        // "Warped floor" fixed, same-height visplane merging fixed.
        MyPlanes.DrawPlanes();
        
        // Check for new console commands.
        DGN.NetUpdate();

        
        try {
            drawsegsbarrier.await();
            visplanebarrier.await();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (BrokenBarrierException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        MyThings.DrawMasked();
        
        //RenderRMIPipeline();
        /*
        try {

            maskedbarrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        } */

        // Check for new console commands.
        DGN.NetUpdate();
    }

    /**
     * R_Init
     */


    public void Init()

    {
        // DON'T FORGET ABOUT MEEEEEE!!!11!!!
        this.screen = V.getScreen(DoomVideoRenderer.SCREEN_FG);

        System.out.print("\nR_InitData");
        InitData();
        // InitPointToAngle ();
        System.out.print("\nR_InitPointToAngle");

        // ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
        System.out.print("\nR_InitTables");
        InitTables();

        SetViewSize(DM.M.getScreenBlocks(), DM.M.getDetailLevel());

        System.out.print("\nR_InitPlanes");
        MyPlanes.InitPlanes();

        System.out.print("\nR_InitLightTables");
        InitLightTables();

        System.out.print("\nR_InitSkyMap: " + TexMan.InitSkyMap());

        System.out.print("\nR_InitTranslationsTables");
        InitTranslationTables();

        System.out.print("\nR_InitDrawingFunctions: ");
        R_InitDrawingFunctions();

        System.out.print("\nR_InitRWISubsystem: ");
        InitRWISubsystem();
        InitRMISubsystem();
        InitPlaneWorkers();
        InitMaskedWorkers();

        System.out.print("\nR_InitTranMap: ");
        R_InitTranMap(0);

        framecount = 0;
    }

    @Override
    public void initScaling() {
        super.initScaling();
        ColVars<byte[],short[]> fake = new ColVars<byte[],short[]>();
        RWI = C2JUtils.createArrayOfObjects(fake, SCREENWIDTH * 3);
        // Be MUCH more generous with this one.
        RMI = C2JUtils.createArrayOfObjects(fake, SCREENWIDTH * 6);
        initializeParallelStuff();
    }

}
