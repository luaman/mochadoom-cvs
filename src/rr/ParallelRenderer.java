package rr;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rr.drawfuns.ColVars;
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

    private int NUMFLOORTHREADS;

    private Executor tp;

    private VisplaneWorker[] vpw;

    private CyclicBarrier visplanebarrier;

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

    private ColVars<byte[]>[] RWI;

    /**
     * Increment this as you submit RWI to the "queue". Remember to reset to 0
     * when you have drawn everything!
     */
    private int RWIcount = 0;

    private RenderWallExecutor[] RWIExec;

    private static final boolean DEBUG = false;

    public ParallelRenderer(DoomMain DM, int wallthread, int floorthreads) {
        super(DM);

        this.MySegs = new ParallelSegs();
        this.MyPlanes = new ParallelPlanes();

        this.NUMWALLTHREADS = wallthread;
        this.NUMFLOORTHREADS = floorthreads;

    }

    /**
     * Default constructor, 2 wall threads and one floor thread.
     * 
     * @param DM
     */
    public ParallelRenderer(DoomMain DM) {
        this(DM, 2, 1);
    }

    private void initializeParallelStuff() {
        // Prepare parallel stuff
        RWIExec = new RenderWallExecutor[NUMWALLTHREADS];
        tp = Executors.newFixedThreadPool(NUMWALLTHREADS + NUMFLOORTHREADS);
        // Prepare the barrier for MAXTHREADS + main thread.
        visplanebarrier =
            new CyclicBarrier(NUMWALLTHREADS + NUMFLOORTHREADS + 1);

        vpw = new VisplaneWorker[NUMFLOORTHREADS];

        RWIcount = 0;
    }

    boolean[] colchecks;

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

            colchecks[dcvars.dc_x] = true;

            // Don't wait to go over
            if (RWIcount >= RWI.length) {
                ResizeRWIBuffer();
            }

            // A deep copy is still necessary, as dc
            RWI[RWIcount].copyFrom(dcvars);

            // We only need to point to the next one in the list.
            RWIcount++;

            // dcvars=RWI[RWIcount];
            /*
             * (int centery, int dc_iscale, int dc_source_ofs, int
             * dc_texturemid, int dc_x, int dc_yh, int dc_yl, int[] columnofs,
             * byte[] dc_colormap, byte[] dc_source){
             */

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
                    I.Error("R_DrawPlanes: drawsegs overflow (%i)", ds_p);

                if (lastvisplane > MAXVISPLANES)
                    I.Error(" R_DrawPlanes: visplane overflow (%i)",
                        lastvisplane);

                if (lastopening > MAXOPENINGS)
                    I.Error("R_DrawPlanes: opening overflow (%i)", lastopening);
            }

            // vpw[0].setRange(0,lastvisplane/2);
            // vpw[1].setRange(lastvisplane/2,lastvisplane);

            for (int i = 0; i < NUMFLOORTHREADS; i++)
                tp.execute(vpw[i]);
        }

    } // End Plane class

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
                        ylookup, screen, RWI, visplanebarrier);
            
            detailaware.add(RWIExec[i]);
            
            // RWIExec[i].setVideoScale(this.vs);
            // RWIExec[i].initScaling();
        }

        for (int i = 0; i < NUMFLOORTHREADS; i++) {
            vpw[i] =
                new VisplaneWorker(SCREENWIDTH, SCREENHEIGHT, columnofs,
                        ylookup, screen, visplanebarrier, NUMFLOORTHREADS);
            vpw[i].id = i;
            
            detailaware.add(vpw[i]);
        }

    }

    /**
     * Resizes RWI buffer, updates executors. Sorry for the hackish
     * implementation but ArrayList and pretty much everything in Collections is
     * way too slow for what we're trying to accomplish.
     */

    private void ResizeRWIBuffer() {
        ColVars<byte[]> fake = new ColVars<byte[]>();
        ColVars<byte[]>[] tmp =
            C2JUtils.createArrayOfObjects(fake, RWI.length * 2);
        System.arraycopy(RWI, 0, tmp, 0, RWI.length);

        // Bye bye, old RWI.
        RWI = tmp;

        for (int i = 0; i < NUMWALLTHREADS; i++) {
            RWIExec[i].updateRWI(RWI);
        }

        System.err.println("RWI Buffer resized. Actual capacity " + RWI.length);
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

        colchecks = new boolean[SCREENWIDTH];

        // The head node is the last node output.
        MyBSP.RenderBSPNode(LL.numnodes - 1);

        // System.out.printf("Submitted %d RWIs\n",RWIcount);

        RenderRWIPipeline();
        // Check for new console commands.
        DGN.NetUpdate();

        // "Warped floor" fixed, same-height visplane merging fixed.
        MyPlanes.DrawPlanes();

        try {
            visplanebarrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Check for new console commands.
        DGN.NetUpdate();

        MyThings.DrawMasked();

        // Check for new console commands.
        DGN.NetUpdate();
    }

    /**
     * R_Init
     */

    // public int detailLevel;
    // public int screenblocks=9; // has defa7ult

    public void Init()

    {
        // Any good reason for this to be here?
        // drawsegs=new drawseg_t[MAXDRAWSEGS];
        // C2JUtils.initArrayOfObjects(drawsegs);

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

        System.out.print("\nR_InitTranMap: ");
        R_InitTranMap(0);

        framecount = 0;
    }

    @Override
    public void initScaling() {
        super.initScaling();
        ColVars<byte[]> fake = new ColVars<byte[]>();
        RWI = C2JUtils.createArrayOfObjects(fake, SCREENWIDTH * 3);
        initializeParallelStuff();
    }

}
