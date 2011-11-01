package rr.drawfuns;

import i.IDoomSystem;

/** Prototype for 
 * 
 * @author velktron
 *
 * @param <T>
 */

public abstract class DoomColumnFunction<T> implements ColumnFunction<T>{
    
    protected final boolean RANGECHECK=false;
    protected final int SCREENWIDTH;
    protected final int SCREENHEIGHT;
    protected ColVars<T> dcvars;
    protected final T screen;
    protected final IDoomSystem I;
    /** Only for fuzzy functions. It's ok to leave it null if not used */
    protected final T BLURRY_MAP; 
    protected final int[] ylookup;
    protected final int[] columnofs;
    
    public DoomColumnFunction(int sCREENWIDTH, int sCREENHEIGHT,int[] ylookup,
            int[] columnofs, ColVars<T> dcvars, T screen,IDoomSystem I) {
        SCREENWIDTH = sCREENWIDTH;
        SCREENHEIGHT = sCREENHEIGHT;
        this.ylookup=ylookup;
        this.columnofs=columnofs;
        this.dcvars = dcvars;
        this.screen = screen;
        this.I=I;
        this.BLURRY_MAP=null;
    }
    
    public DoomColumnFunction(int sCREENWIDTH, int sCREENHEIGHT,int[] ylookup,
            int[] columnofs,ColVars<T> dcvars, T screen,IDoomSystem I,T BLURRY_MAP) {
        SCREENWIDTH = sCREENWIDTH;
        SCREENHEIGHT = sCREENHEIGHT;
        this.ylookup=ylookup;
        this.columnofs=columnofs;
        this.dcvars = dcvars;
        this.screen = screen;
        this.I=I;
        this.BLURRY_MAP=BLURRY_MAP;
    }
    
    protected final void performRangeCheck(){
        if (dcvars.dc_x >= SCREENWIDTH || dcvars.dc_yl < 0 || dcvars.dc_yh >= SCREENHEIGHT)
            I.Error("R_DrawColumn: %i to %i at %i", dcvars.dc_yl, dcvars.dc_yh, dcvars.dc_x);
    }
    
    /**
     * 
     * Use ylookup LUT to avoid multiply with ScreenWidth.
     * Use columnofs LUT for subwindows?
     * 
     * @return Framebuffer destination address.
     */
    
    protected final int computeScreenDest() {
        return ylookup[dcvars.dc_yl] + columnofs[dcvars.dc_x];
    }

    protected final int blockyDest1() {
        return ylookup[dcvars.dc_yl] + columnofs[dcvars.dc_x<<1];
    }

    protected final int blockyDest2() {
        return  ylookup[dcvars.dc_yl] + columnofs[(dcvars.dc_x<<1)+1];
    }

    @Override
    public final void invoke(ColVars<T> dcvars) {
        this.dcvars=dcvars;
        invoke();
    }
    
}
