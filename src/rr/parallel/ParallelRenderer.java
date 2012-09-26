package rr.parallel;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rr.IDetailAware;
import rr.patch_t;
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

public abstract class ParallelRenderer<T, V>
        extends AbstractParallelRenderer<T, V> {

    public ParallelRenderer(DoomMain<T, V> DM, int wallthread,
            int floorthreads, int nummaskedthreads) {
        super(DM, wallthread, floorthreads, nummaskedthreads);
        this.MySegs = new ParallelSegs(DM.R);
        this.MyPlanes = new ParallelPlanes<T, V>(DM.R);

    }

    /**
     * Default constructor, 1 seg, 1 span and two masked threads.
     * 
     * @param DM
     */
    public ParallelRenderer(DoomMain<T, V> DM) {
        this(DM, 1, 1, 2);
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
        seg_vars.ClearDrawSegs();
        vp_vars.ClearPlanes();
        MySegs.ClearClips();
        VIS.ClearSprites();
        // Check for new console commands.
        DGN.NetUpdate();

        // The head node is the last node output.
        MyBSP.RenderBSPNode(LL.numnodes - 1);

        // System.out.printf("Submitted %d RWIs\n",RWIcount);

        MySegs.CompleteRendering();

        // Check for new console commands.
        DGN.NetUpdate();

        // "Warped floor" fixed, same-height visplane merging fixed.
        MyPlanes.DrawPlanes();

        // Check for new console commands.
        DGN.NetUpdate();

        MySegs.sync();
        MyPlanes.sync();

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

        // RenderRMIPipeline();
        /*
         * try { maskedbarrier.await(); } catch (Exception e) {
         * e.printStackTrace(); }
         */

        // Check for new console commands.
        DGN.NetUpdate();
    }

    public static class Indexed
            extends ParallelRenderer<byte[], byte[]> {

        public Indexed(DoomMain<byte[], byte[]> DM, int wallthread,
                int floorthreads, int nummaskedthreads) {
            super(DM, wallthread, floorthreads, nummaskedthreads);
            this.MyThings =
                new ParallelThings.Indexed(this, tp, NUMMASKEDTHREADS);
        }
        
        /**
         * R_InitColormaps
         * 
         * @throws IOException
         */
        protected void InitColormaps() throws IOException {
            int lump, length;

            // Load in the light tables,
            // 256 byte align tables.
            lump = W.GetNumForName("COLORMAP");
            length = W.LumpLength(lump) + 256;
            colormaps.colormaps = new byte[(length / 256)][256];
            System.out.println("Colormaps: " + colormaps.colormaps.length);

            byte[] tmp = new byte[length];
            W.ReadLump(lump,tmp);

            for (int i = 0; i < colormaps.colormaps.length; i++) {
                System.arraycopy(tmp, i * 256, colormaps.colormaps[i], 0, 256);
            }
            
            // MAES: blurry effect is hardcoded to this colormap.
            BLURRY_MAP=colormaps.colormaps[6];
            // colormaps = (byte *)( ((int)colormaps + 255)&~0xff);     

            
        }

        protected void InitMaskedWorkers() {
            for (int i = 0; i < NUMMASKEDTHREADS; i++) {
                maskedworkers[i] =
                    new MaskedWorker.Indexed(this, i, SCREENWIDTH,
                            SCREENHEIGHT, ylookup, columnofs, NUMMASKEDTHREADS,
                            screen, maskedbarrier, BLURRY_MAP);
                detailaware.add(maskedworkers[i]);
                // "Peg" to sprite manager.
                maskedworkers[i].cacheSpriteManager(SM);
            }
        }

    }

    @Override
    protected void InitParallelStuff() {

        // This actually only creates the necessary arrays and
        // barriers. Things aren't "wired" yet.

        // Using "render wall instruction" subsystem
        RWIExec = new RenderWallExecutor.Indexed[NUMWALLTHREADS];

        // Using masked sprites
        RMIExec = new RenderMaskedExecutor[NUMMASKEDTHREADS];

        // Using
        vpw = new Runnable[NUMFLOORTHREADS];
        maskedworkers = new MaskedWorker.Indexed[NUMMASKEDTHREADS];

        tp = Executors.newCachedThreadPool();
        // Prepare the barrier for MAXTHREADS + main thread.
        drawsegsbarrier = new CyclicBarrier(NUMWALLTHREADS + 1);

        visplanebarrier = new CyclicBarrier(NUMFLOORTHREADS + 1);

        maskedbarrier = new CyclicBarrier(NUMMASKEDTHREADS + 1);

        // RWIcount = 0;

        // InitRWISubsystem();
        // InitRMISubsystem();
        // InitPlaneWorkers();
        // InitMaskedWorkers();

        // If using masked threads, set these too.
        TexMan.setSMPVars(NUMMASKEDTHREADS);

    }

    /*
     * private void InitPlaneWorkers(){ for (int i = 0; i < NUMFLOORTHREADS;
     * i++) { vpw[i] = new VisplaneWorker2(i,SCREENWIDTH, SCREENHEIGHT,
     * columnofs, ylookup, screen, visplanebarrier, NUMFLOORTHREADS);
     * //vpw[i].id = i; detailaware.add((IDetailAware) vpw[i]); } }
     */

    @Override
    public void initScaling() {
        /*
         * TODO: relay to dependent objects. super.initScaling();
         * ColVars<byte[],byte[]> fake = new ColVars<byte[],byte[]>(); RWI =
         * C2JUtils.createArrayOfObjects(fake, SCREENWIDTH * 3); // Be MUCH more
         * generous with this one. RMI = C2JUtils.createArrayOfObjects(fake,
         * SCREENWIDTH * 6);
         */

    }

    protected abstract void InitMaskedWorkers();

    public static class HiColor
            extends ParallelRenderer<byte[], short[]> {

        public HiColor(DoomMain<byte[], short[]> DM, int wallthread,
                int floorthreads, int nummaskedthreads) {
            super(DM, wallthread, floorthreads, nummaskedthreads);
        }

        protected void InitMaskedWorkers() {
            for (int i = 0; i < NUMMASKEDTHREADS; i++) {
                maskedworkers[i] =
                    new MaskedWorker.HiColor(this, i, SCREENWIDTH,
                            SCREENHEIGHT, ylookup, columnofs, NUMMASKEDTHREADS,
                            screen, maskedbarrier);
                detailaware.add(maskedworkers[i]);
                // "Peg" to sprite manager.
                maskedworkers[i].cacheSpriteManager(SM);
            }
        }

        /**
         * R_InitColormaps This is VERY different for hicolor.
         * 
         * @throws IOException
         */
        protected void InitColormaps()
                throws IOException {

            colormaps.colormaps = V.getColorMaps();
            System.out.println("COLORS15 Colormaps: "
                    + colormaps.colormaps.length);

            // MAES: blurry effect is hardcoded to this colormap.
            // Pointless, since we don't use indexes. Instead, a half-brite
            // processing works just fine.
            BLURRY_MAP = null;// colormaps[0];
        }

    }

    public static class TrueColor
            extends ParallelRenderer<byte[], int[]> {

        public TrueColor(DoomMain<byte[], int[]> DM, int wallthread,
                int floorthreads, int nummaskedthreads) {
            super(DM, wallthread, floorthreads, nummaskedthreads);
        }

        protected void InitMaskedWorkers() {
            for (int i = 0; i < NUMMASKEDTHREADS; i++) {
                maskedworkers[i] =
                    new MaskedWorker.TrueColor(i, SCREENWIDTH, SCREENHEIGHT,
                            ylookup, columnofs, NUMMASKEDTHREADS, screen,
                            maskedbarrier);
                detailaware.add(maskedworkers[i]);
                // "Peg" to sprite manager.
                maskedworkers[i].cacheSpriteManager(SM);
            }
        }

    }

}
