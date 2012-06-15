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
        extends AbstractParallelRenderer {

    public ParallelRenderer(DoomMain DM, int wallthread, int floorthreads,int nummaskedthreads) {
        super(DM, wallthread, floorthreads,nummaskedthreads);

        this.MyThings=new ParallelThings();
        this.MySegs = new ParallelSegs();
        this.MyPlanes = new ParallelPlanes();
        
        
    }

    /**
     * Default constructor, 1 seg, 1 span and two masked threads.
     * 
     * @param DM
     */
    public ParallelRenderer(DoomMain DM) {
        this(DM, 1, 1,2);
    }

    @Override
    protected void InitParallelStuff() {

        // This actually only creates the necessary arrays and
        // barriers. Things aren't "wired" yet.
        
        // Using "render wall instruction" subsystem
        RWIExec = new RenderWallExecutor[NUMWALLTHREADS];
        
        // Using masked sprites
        RMIExec = new RenderMaskedExecutor[NUMMASKEDTHREADS];
        
        // Using
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
        
        InitRWISubsystem();
        InitRMISubsystem();
        InitPlaneWorkers();
        InitMaskedWorkers();

        
        // If using masked threads, set these too.
        
        smp_composite=new boolean[NUMMASKEDTHREADS];// = false;
        smp_lasttex=new int[NUMMASKEDTHREADS];// = -1;
        smp_lastlump=new int[NUMMASKEDTHREADS];// = -1;
        smp_lastpatch=new patch_t[NUMMASKEDTHREADS];// = null;

        
    }

    

    
    
    // ///////////////// Generic rendering methods /////////////////////

  
    
    private void InitPlaneWorkers(){
        for (int i = 0; i < NUMFLOORTHREADS; i++) {
            vpw[i] =
                new VisplaneWorker2(i,SCREENWIDTH, SCREENHEIGHT, columnofs,
                        ylookup, screen, visplanebarrier, NUMFLOORTHREADS);
            //vpw[i].id = i;
            
            detailaware.add((IDetailAware) vpw[i]);
        }
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

        System.out.print("\nR_InitParallelStuff: ");
        InitParallelStuff();

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

    }

}
