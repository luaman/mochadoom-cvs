package rr;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import rr.drawfuns.ColVars;
import rr.drawfuns.DcFlags;
import rr.drawfuns.DoomColumnFunction;

import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawColumnBoomOptLow;

/**
 * This is what actual executes the RenderWallInstruction. Essentially it's a
 * self-contained column rendering function.
 * 
 * @author admin
 */

public class RenderMaskedExecutor
        implements Runnable,IDetailAware {

    private CyclicBarrier barrier;

    private ColVars<byte[],short[]>[] RMI;
    
    private int rmiend;

    private boolean lowdetail=false;
    
    private int start, end;

    private final DoomColumnFunction<byte[],short[]> colfunchi, colfunclow;
    private final DoomColumnFunction<byte[],short[]> fuzzfunchi, fuzzfunclow;
    private final DoomColumnFunction<byte[],short[]> transfunchi, transfunclow;
    
    private DoomColumnFunction<byte[],short[]> colfunc;

    public RenderMaskedExecutor(int SCREENWIDTH, int SCREENHEIGHT,
            int[] columnofs, int[] ylookup, short[] screen,
            ColVars<byte[],short[]>[] RMI, CyclicBarrier barrier,
            DoomColumnFunction<byte[],short[]> colfunc,
            DoomColumnFunction<byte[],short[]> colfunclo,
            DoomColumnFunction<byte[],short[]> fuzzfunc,
            DoomColumnFunction<byte[],short[]> fuzzfunclo,
            DoomColumnFunction<byte[],short[]> transfunc,
            DoomColumnFunction<byte[],short[]> transfunclo
            ) {
        this.RMI = RMI;
        this.barrier = barrier;
        this.SCREENWIDTH = SCREENWIDTH;
        this.SCREENHEIGHT = SCREENHEIGHT;
        this.colfunc =  colfunchi = colfunc;
        this.fuzzfunchi=fuzzfunc;
        this.colfunclow = colfunclo;
        this.fuzzfunclow =fuzzfunclo;
        this.transfunchi=transfunc;
        this.transfunclow=transfunclo;
    }

    public void setRange(int start, int end) {
        this.end = end;
        this.start = start;
    }

    public void setDetail(int detailshift) {
        if (detailshift == 0)
            lowdetail=false;
        else
            lowdetail=true;
    }

    public void run() {

        // System.out.println("Wall executor from "+start +" to "+ end);
        int dc_flags=0;
        
        // Check out ALL valid RMIs, but only draw those on YOUR side of the screen.
        for (int i = 0; i < rmiend; i++) {
            
            if (RMI[i].dc_x>=start && RMI[i].dc_x<=end){
            // Change function type according to flags.
            // No flag change means reusing the last used type
            dc_flags=RMI[i].dc_flags;
                //System.err.printf("Flags transition %d\n",dc_flags);
                if (lowdetail){
                    if ((dc_flags&DcFlags.FUZZY)!=0)                        
                        colfunc=fuzzfunclow;
                    else
                    if ((dc_flags&DcFlags.TRANSLATED)!=0)
                            colfunc=transfunclow;
                    else
                        colfunc=colfunclow;
                } else {
                    if ((dc_flags&DcFlags.FUZZY)!=0)
                        colfunc=fuzzfunchi;
                    else
                    if ((dc_flags&DcFlags.TRANSLATED)!=0)
                        colfunc=transfunchi;
                    else
                        colfunc=colfunchi;
                    }
            
            colfunc.invoke(RMI[i]);
            }
        }

        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
        
    public void setRMIEnd(int rmiend){
        this.rmiend=rmiend;
    }         

    public void updateRMI(ColVars<byte[],short[]>[] RMI) {
        this.RMI = RMI;

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
