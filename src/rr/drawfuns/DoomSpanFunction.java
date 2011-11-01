package rr.drawfuns;

import i.IDoomSystem;

public abstract class DoomSpanFunction<T> implements SpanFunction<T> {
    
    protected final boolean RANGECHECK=false;
    protected final int SCREENWIDTH;
    protected final int SCREENHEIGHT;
    protected SpanVars<T> dsvars;
    protected final int[] ylookup;
    protected final int[] columnofs;
    protected final T screen;
    protected final IDoomSystem I;
    
    public DoomSpanFunction(int sCREENWIDTH, int sCREENHEIGHT,
            int[] ylookup, int[] columnofs,SpanVars<T> dsvars, T screen,IDoomSystem I) {
        SCREENWIDTH = sCREENWIDTH;
        SCREENHEIGHT = sCREENHEIGHT;
        this.ylookup=ylookup;
        this.columnofs=columnofs;
        this.dsvars = dsvars;
        this.screen = screen;
        this.I=I;
    }
    
    protected final void doRangeCheck(){
        if (dsvars.ds_x2 < dsvars.ds_x1 || dsvars.ds_x1 < 0 || dsvars.ds_x2 >= SCREENWIDTH
                || dsvars.ds_y > SCREENHEIGHT) {
            I.Error("R_DrawSpan: %i to %i at %i", dsvars.ds_x1, dsvars.ds_x2, dsvars.ds_y);
        }
    }

    @Override
    public final void invoke(SpanVars<T> dsvars) {
        this.dsvars=dsvars;
        invoke();
    }
    
}
