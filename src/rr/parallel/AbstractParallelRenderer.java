package rr.parallel;

import static data.Defines.ANGLETOSKYSHIFT;
import static data.Defines.PU_STATIC;
import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.BITS32;
import static data.Tables.addAngles;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.Tables.finetangent;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FixedMul;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;

import data.Tables;
import doom.DoomStatus;

import rr.IDetailAware;
import rr.PlaneDrawer;
import rr.Renderer;
import rr.RendererState;
import rr.flat_t;
import rr.visplane_t;
import rr.drawfuns.ColVars;
import rr.drawfuns.DcFlags;
import rr.drawfuns.DoomColumnFunction;
import rr.drawfuns.DoomSpanFunction;
import rr.drawfuns.R_DrawColumnBoom;
import rr.drawfuns.R_DrawColumnBoomLow;
import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawColumnBoomOptLow;
import rr.drawfuns.R_DrawFuzzColumn;
import rr.drawfuns.R_DrawFuzzColumnLow;
import rr.drawfuns.R_DrawSpanLow;
import rr.drawfuns.R_DrawSpanUnrolled;
import rr.drawfuns.R_DrawTranslatedColumn;
import rr.drawfuns.R_DrawTranslatedColumnLow;
import rr.drawfuns.SpanVars;
import rr.parallel.RenderWallExecutor.HiColor;
import utils.C2JUtils;

/** Features and funcitonality which is common among parallel renderers
 * 
 * @author velktron
 *
 */

public abstract class AbstractParallelRenderer<T,V>
        extends RendererState<T,V> {

    public AbstractParallelRenderer(DoomStatus<T,V> DS, int wallthread,
            int floorthreads, int nummaskedthreads) {
        super(DS);
        this.NUMWALLTHREADS = wallthread;
        this.NUMFLOORTHREADS = floorthreads;
        this.NUMMASKEDTHREADS = nummaskedthreads;
                
        
    }

    public AbstractParallelRenderer(DoomStatus<T,V> DS, int wallthread,
            int floorthreads) {
        super(DS);
        this.NUMWALLTHREADS = wallthread;
        this.NUMFLOORTHREADS = floorthreads;
        this.NUMMASKEDTHREADS = 1;
    }

    // //////// PARALLEL OBJECTS /////////////
    protected final int NUMWALLTHREADS;

    protected final int NUMMASKEDTHREADS;

    protected int NUMFLOORTHREADS;

    protected Executor tp;

    protected Runnable[] vpw;

    protected MaskedWorker<V>[] maskedworkers;

    protected CyclicBarrier drawsegsbarrier;

    protected CyclicBarrier visplanebarrier;

    protected CyclicBarrier maskedbarrier;



    protected static final boolean DEBUG = false;

    /**
     * Parallel Things drawing class, column based, using RMI pipeline.
     * For N threads, each thread only draws those columns of sprites that
     * are in its own 1/N portion of the screen.
     * 
     * Overrides only the terminal drawing methods from things, using a
     * mechanism very similar to column-based wall threading. It's not very
     * efficient, since some of the really heavy parts (such as visibility
     * priority) are still done serially, and actually do take up a lot of the
     * actual rendering time, and the number of columns generated is REALLY
     * enormous (100K+ for something like nuts.wad), and the thing chokes on
     * synchronization, more than anything. The only appropriate thing to do
     * would be to have a per-vissprite renderer, which would actually move much
     * of the brunt work away from the main thread. Some interesting benchmarks
     * on nuts.wad timedemo: Normal things serial renderer: 60-62 fps "Dummy"
     * completeColumns: 72 fps "Dummy" things renderer without final drawing: 80
     * fps "Dummy" things renderer without ANY calculations: 90 fps. This means
     * that even a complete parallelization will likely have a quite limited
     * impact.
     * 
     * @author velktron
     */

    public final class ParallelThings
            extends AbstractThings {

        @Override
        public void DrawMasked() {
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
        public void completeColumn() {

            if (view.detailshift == 1)
                flags = DcFlags.LOW_DETAIL;
            // Don't wait to go over
            if (RMIcount >= RMI.length) {
                ResizeRMIBuffer();
            }

            // A deep copy is still necessary, as well as setting dc_flags
            RMI[RMIcount].copyFrom(maskedcvars, colfunc.getFlags());

            // We only need to point to the next one in the list.
            RMIcount++;
        }

        int flags;

    }

    /**  Alternate parallel sprite renderer using a split-screen strategy.
     *  For N threads, each thread gets to render only the sprites that are entirely
     *  in its own 1/Nth portion of the screen.
     *  
     *  Sprites that span more than one section, are drawn partially. Each thread
     *  only has to worry with the priority of its own sprites. Similar to the 
     *  split-seg parallel drawer.
     * 
     *  Uses the "masked workers" subsystem, there is no column pipeline: workers
     *  "tap" directly in the sprite sorted table and act accordingly (draw entirely,
     *  draw nothing, draw partially).
     * 
     * @author velktron
     *
     */
    
    public final class ParallelThings2
            extends AbstractThings {

        public ParallelThings2() {

        }

        @Override
        public void DrawMasked() {

            VIS.SortVisSprites();

            for (int i = 0; i < maskedworkers.length; i++) {
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
        public void setPspriteScale(int scale) {
            for (int i = 0; i < maskedworkers.length; i++)
                maskedworkers[i].setPspriteScale(scale);
        }

        @Override
        public void setPspriteIscale(int scale) {
            for (int i = 0; i < maskedworkers.length; i++)
                maskedworkers[i].setPspriteIscale(scale);
        }

    }

    protected final class ParallelSegs
            extends SegDrawer {

        public ParallelSegs(Renderer<?, ?> R) {
            super(R);        }

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

    protected final class ParallelPlanes<T,V>
            extends PlaneDrawer<T,V> {

        protected ParallelPlanes(Renderer<T, V> R) {
            super(R);
        }

        /**
         * R_DrawPlanes At the end of each frame. This also means that visplanes
         * must have been set BEFORE we called this function. Therefore, look
         * for errors behind.
         * 
         * @throws IOException
         */
        public void DrawPlanes() {

            if (RANGECHECK) {
                rangeCheckErrors();
            }

            // vpw[0].setRange(0,lastvisplane/2);
            // vpw[1].setRange(lastvisplane/2,lastvisplane);

            for (int i = 0; i < NUMFLOORTHREADS; i++)
                tp.execute(vpw[i]);
        }

    } // End Plane class

    protected final class ParallelSegs2 extends SegDrawer{


        public ParallelSegs2(Renderer<?, ?> R) {
            super(R);
        }

        @Override
        protected void RenderSegLoop () 
        {

            // Generate Seg rendering instruction BEFORE the looping start
            // and anything is modified. The loop will be repeated in the
            // threads, but without marking ceilings/floors etc.
            GenerateRSI();
            
            // I think it's EXACTLY the same.
            super.RenderSegLoop();

/*            for ( ; rw_x < rw_stopx ; rw_x++)
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
                    vp_vars.visplanes[vp_vars.ceilingplane].setTop(rw_x,(char) top);
                    vp_vars.visplanes[vp_vars.ceilingplane].setBottom(rw_x, (char) bottom);
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
                    ceilingclip[rw_x] = (short) view.height;
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
    */
        }

        protected final void GenerateRSI(){

            if (RSIcount>=RSI.length){
                ResizeRSIBuffer();
            }

            RenderSegInstruction<V> rsi=RSI[RSIcount];
            rsi.centery=view.centery;
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
            rsi.walllights=colormaps.walllights;
            rsi.viewheight=view.height;
            //rsi.floorplane=floorplane;
            //rsi.ceilingplane=ceilingplane;
            RSIcount++;
        }


        @Override
        protected void CompleteColumn() {
            // TODO Auto-generated method stub

        }



    }

    protected final class ParallelPlanes2<T,V> extends PlaneDrawer<T,V>{

        protected ParallelPlanes2(Renderer<T, V> R) {
            super(R);
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

            if (RANGECHECK){
                rangeCheckErrors();
            }

            //      vpw[0].setRange(0,lastvisplane/2);
            //      vpw[1].setRange(lastvisplane/2,lastvisplane);

            for (int i=0;i<NUMFLOORTHREADS;i++)
                tp.execute(vpw[i]);
        }

    } // End Plane class
    
  
    
    



  

    protected void RenderRWIPipeline() {

        for (int i = 0; i < NUMWALLTHREADS; i++) {

            RWIExec[i].setRange((i * RWIcount) / NUMWALLTHREADS,
                ((i + 1) * RWIcount) / NUMWALLTHREADS);
            // RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
            tp.execute(RWIExec[i]);
        }

        // System.out.println("RWI count"+RWIcount);
        RWIcount = 0;
    }

    
   
    
    /// RWI SUBSYSTEM
    /// AKA "Render Wall Instruction": instructions that store only a single column
    // from a wall
    
    /* Just what are "RWIs"?
     * 
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
    
    
    protected RenderWallExecutor<T,V>[] RWIExec;
    
    /** Array of "wall" (actually, column) instructions */
    
    protected ColVars<T, V>[] RWI;
    
    /**
     * Increment this as you submit RWIs to the "queue". Remember to reset to 0
     * when you have drawn everything!
     */
    
    protected int RWIcount = 0;
    
    /**
     * R_InitRWISubsystem Initialize RWIs and RWI Executors. Pegs them to the
     * RWI, ylookup and screen[0].
     */

    protected abstract void InitRWISubsystem();
    
    /*
    {
        // CATCH: this must be executed AFTER screen is set, and
        // AFTER we initialize the RWI themselves,
        // before V is set (right?)
        for (int i = 0; i < NUMWALLTHREADS; i++) {
            RWIExec[i] =
                new RenderWallExecutor.HiColor(SCREENWIDTH, SCREENHEIGHT, columnofs,
                        ylookup, screen, RWI, drawsegsbarrier);
            
            detailaware.add(RWIExec[i]);
        }
    }
    */
 
   
    
    /**
     * Resizes RWI buffer, updates executors. Sorry for the hackish
     * implementation but ArrayList and pretty much everything in Collections is
     * way too slow for what we're trying to accomplish.
     */

    protected abstract void ResizeRWIBuffer();
    /*
    {
        ColVars<byte[], V> fake = new ColVars<byte[], V>();

        // Bye bye, old RWI.
        RWI = C2JUtils.resize(fake, RWI, RWI.length * 2);

        for (int i = 0; i < NUMWALLTHREADS; i++) {
            RWIExec[i].updateRWI(RWI);
        }
        // System.err.println("RWI Buffer resized. Actual capacity " +
        // RWI.length);
    }*/
    
    /** RenderSeg subsystem. Similar concept to RWI, but stores "Render Seg Instructions" instead.
     * More complex to build, but potentially faster in some situations, as it allows distributing
     * load per-wall, rather than per-screen portion. Requires careful concurrency considerations.
     * 
     */
    protected RenderSegInstruction<V>[] RSI;

    /** Increment this as you submit RSIs to the "queue". Remember to reset to 0 when you have drawn everything!
     * 
     */
    protected int RSIcount=0;

    protected RenderSegExecutor<byte[],V>[] RSIExec;

    /**
     * R_InitRSISubsystem
     * 
     * Initialize RSIs and RSI Executors.
     * Pegs them to the RSI, ylookup and screen[0].
     */

    protected abstract void InitRSISubsystem();
    
    /*
    {
        // CATCH: this must be executed AFTER screen is set, and
        // AFTER we initialize the RWI themselves,
        // before V is set (right?) 
        
        //offsets=new int[NUMWALLTHREADS];
        for (int i=0;i<NUMWALLTHREADS;i++){
            RSIExec[i]=new RenderSegExecutor.HiColor(
                SCREENWIDTH,
                SCREENHEIGHT,
                i,
                screen,
                    this,
                    TexMan, 
                    RSI,
                    MySegs.getBLANKCEILINGCLIP(),
                    MySegs.getBLANKFLOORCLIP(), 
                    MySegs.getCeilingClip(), 
                    MySegs.getFloorClip(), 
                    columnofs, 
                    xtoviewangle, 
                    ylookup, 
                    this.visplanes,
                    this.visplanebarrier);
            RSIExec[i].setVideoScale(this.vs);
            RSIExec[i].initScaling();
            // Each SegExecutor sticks to its own half (or 1/nth) of the screen.
            RSIExec[i].setScreenRange(i*(SCREENWIDTH/NUMWALLTHREADS),(i+1)*(SCREENWIDTH/NUMWALLTHREADS));
            detailaware.add(RSIExec[i]);
            
        }
        

        for (int i=0;i<NUMFLOORTHREADS;i++){
            vpw[i]=new VisplaneWorker(i,SCREENWIDTH,SCREENHEIGHT,columnofs,ylookup, screen,visplanebarrier,NUMFLOORTHREADS);
            detailaware.add((IDetailAware) vpw[i]);
        }
    }
    */

    /** Resizes RWI buffer, updates executors. Sorry for the hackish implementation
     *  but ArrayList and pretty much everything in Collections is way too slow
     *  for what we're trying to accomplish.
     * 
     */

    protected void ResizeRSIBuffer() {
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


    protected void RenderRSIPipeline() {

        for (int i=0;i<NUMWALLTHREADS;i++){
            RSIExec[i].setRSIEnd(RSIcount);
            //RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
            tp.execute(RSIExec[i]);
        }

        //System.out.println("RWI count"+RWIcount);
        RSIcount=0;
    }
    

    /** Render Masked Instuction subsystem. Essentially, a way to split sprite work
     *  between threads on a column-basis.
     */

    protected ColVars<T, V>[] RMI;

    /**
     * Increment this as you submit RMIs to the "queue". Remember to reset to 0
     * when you have drawn everything!
     */
    protected int RMIcount = 0;

    

    protected RenderMaskedExecutor[] RMIExec;
    
    /** Creates RMI Executors */
    
    protected abstract void InitRMISubsystem();
    
    /*
    {
        for (int i = 0; i < NUMMASKEDTHREADS; i++) {
            // Each masked executor gets its own set of column functions.
            
            RMIExec[i] =
                new RenderMaskedExecutor(SCREENWIDTH, SCREENHEIGHT, columnofs,
                        ylookup, screen, RMI, maskedbarrier,
                        // Regular masked columns
                        new R_DrawColumnBoom(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),
                        new R_DrawColumnBoomLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),
                        
                        // Fuzzy columns
                        new R_DrawFuzzColumn.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),
                        new R_DrawFuzzColumnLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),

                        // Translated columns
                        new R_DrawTranslatedColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I),
                        new R_DrawTranslatedColumnLow(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I)
                        );
                
            detailaware.add(RMIExec[i]);
            }
        }
    */
    
    
    protected void ResizeRMIBuffer() {
        ColVars<T, V> fake = new ColVars<T, V>();
        ColVars<T, V>[] tmp =
            C2JUtils.createArrayOfObjects(fake, RMI.length * 2);
        System.arraycopy(RMI, 0, tmp, 0, RMI.length);

        // Bye bye, old RMI.
        RMI = tmp;

        for (int i = 0; i < NUMMASKEDTHREADS; i++) {
            RMIExec[i].updateRMI(RMI);
        }

        System.err.println("RMI Buffer resized. Actual capacity " + RMI.length);
    }

    /** Any scaling and implementation-specific stuff to do for parallel stuff should go here.
     * This method is called internally by the public Init(). The idea is that the
     * renderer should be up & running after you finally called this.
     * 
     */
    protected abstract void InitParallelStuff();

    protected void RenderRMIPipeline() {

        for (int i = 0; i < NUMMASKEDTHREADS; i++) {

            RMIExec[i].setRange((i * this.SCREENWIDTH) / NUMMASKEDTHREADS,
                ((i + 1) * this.SCREENWIDTH) / NUMMASKEDTHREADS);
            RMIExec[i].setRMIEnd(RMIcount);
            // RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
            tp.execute(RMIExec[i]);
        }

        // System.out.println("RWI count"+RWIcount);
        RMIcount = 0;
    }

    protected void InitMaskedWorkers() {
        for (int i = 0; i < NUMMASKEDTHREADS; i++) {
            maskedworkers[i] =
                new MaskedWorker(i, SCREENWIDTH, SCREENHEIGHT, ylookup,
                        columnofs, NUMMASKEDTHREADS, screen, maskedbarrier);
            detailaware.add(maskedworkers[i]);
            // "Peg" to sprite manager.
            maskedworkers[i].cacheSpriteManager(SM);
        }
    
}
