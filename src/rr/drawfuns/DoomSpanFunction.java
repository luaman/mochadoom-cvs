package rr.drawfuns;

import i.IDoomSystem;

public abstract class DoomSpanFunction<T> implements DrawFunction {
    
    protected final boolean RANGECHECK=false;
    protected final int SCREENWIDTH;
    protected final int SCREENHEIGHT;
    protected final SpanVars<T> dsvars;
    protected final T screen;
    protected final IDoomSystem I;
    
    public DoomSpanFunction(int sCREENWIDTH, int sCREENHEIGHT,
            SpanVars<T> dsvars, T screen,IDoomSystem I) {
        SCREENWIDTH = sCREENWIDTH;
        SCREENHEIGHT = sCREENHEIGHT;
        this.dsvars = dsvars;
        this.screen = screen;
        this.I=I;
    }

}
