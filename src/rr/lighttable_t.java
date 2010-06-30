package rr;

//
//OTHER TYPES
//

//This could be wider for >8 bit display.
//Indeed, true color support is posibble
//precalculating 24bpp lightmap/colormap LUT.
//from darkening PLAYPAL to all black.
//Could even us emore than 32 levels.
// typedef byte    lighttable_t;   

public class lighttable_t {
    
public lighttable_t(byte val) {
        super();
        this.val = val;
    }

public byte val;    
}
