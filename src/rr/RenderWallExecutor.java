package rr;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import rr.drawfuns.ColVars;
import rr.drawfuns.DoomColumnFunction;

import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawColumnBoomOptLow;

/**
 * This is what actual executes the RenderWallInstruction. Essentially it's a
 * self-contained column rendering function.
 * 
 * @author admin
 */

public class RenderWallExecutor
        implements Runnable,IDetailAware {

    private CyclicBarrier barrier;

    private ColVars<byte[]>[] RWI;

    private int start, end;

    private final DoomColumnFunction<byte[]> colfunchi, colfunclow;

    private DoomColumnFunction<byte[]> colfunc;

    public RenderWallExecutor(int SCREENWIDTH, int SCREENHEIGHT,
            int[] columnofs, int[] ylookup, byte[] screen,
            ColVars<byte[]>[] RWI, CyclicBarrier barrier) {
        this.RWI = RWI;
        this.barrier = barrier;
        this.SCREENWIDTH = SCREENWIDTH;
        this.SCREENHEIGHT = SCREENHEIGHT;
        colfunc =
            colfunchi =
                new R_DrawColumnBoomOpt(SCREENWIDTH, SCREENHEIGHT, ylookup,
                        columnofs, null, screen, null);
        colfunclow =
            new R_DrawColumnBoomOptLow(SCREENWIDTH, SCREENHEIGHT, ylookup,
                    columnofs, null, screen, null);
    }

    public void setRange(int start, int end) {
        this.end = end;
        this.start = start;
    }

    public void setDetail(int detailshift) {
        if (detailshift == 0)
            colfunc = colfunchi;
        else
            colfunc = colfunclow;
    }

    public void run() {

        // System.out.println("Wall executor from "+start +" to "+ end);

        for (int i = start; i < end; i++) {
            colfunc.invoke(RWI[i]);
        }

        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    public void updateRWI(ColVars<byte[]>[] RWI) {
        this.RWI = RWI;

    }

    /////////////// VIDEO SCALE STUFF//////////////////////

    protected final int SCREENWIDTH;

    protected final int SCREENHEIGHT;
    /*
     * protected IVideoScale vs;
     * @Override public void setVideoScale(IVideoScale vs) { this.vs=vs; }
     * @Override public void initScaling() {
     * this.SCREENHEIGHT=vs.getScreenHeight();
     * this.SCREENWIDTH=vs.getScreenWidth(); }
     */

}
