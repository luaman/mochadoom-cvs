package rr.drawfuns;

import i.IDoomSystem;

/**
 * Low detail version. Jesus.
 */

public final class R_DrawFuzzColumnLow
        extends DoomColumnFunction<byte[],short[]> {

    public R_DrawFuzzColumnLow(int SCREENWIDTH, int SCREENHEIGHT,
            int[] ylookup, int[] columnofs, ColVars<byte[],short[]> dcvars,
            short[] screen, IDoomSystem I, short[] BLURRY_MAP) {
        super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I,
                BLURRY_MAP);
        
        this.flags=DcFlags.LOW_DETAIL|DcFlags.FUZZY;

        FUZZOFF = SCREENWIDTH;

        // Recompute fuzz table

        fuzzoffset =
            new int[] { FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
                    -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
                    FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
                    -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF,
                    FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF,
                    -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
                    FUZZOFF, -FUZZOFF, -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
                    FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
                    -FUZZOFF, FUZZOFF };

        FUZZTABLE = fuzzoffset.length;
    }

    private int fuzzpos;

    private final int FUZZTABLE;

    //
    // Spectre/Invisibility.
    //

    private final int FUZZOFF;

    private final int[] fuzzoffset;

    public void invoke() {
        int count;
        int dest, dest2;

        // Adjust borders. Low...
        if (dcvars.dc_yl == 0)
            dcvars.dc_yl = 1;

        // .. and high.
        if (dcvars.dc_yh == dcvars.viewheight - 1)
            dcvars.dc_yh = dcvars.viewheight - 2;

        count = dcvars.dc_yh - dcvars.dc_yl;

        // Zero length.
        if (count < 0)
            return;

        if (RANGECHECK) {
            performRangeCheck();
        }

        // The idea is to draw more than one pixel at a time.
        dest = blockyDest1();
        dest2 = blockyDest2();

        // Looks like an attempt at dithering,
        // using the colormap #6 (of 0-31, a bit
        // brighter than average).
        if (count>4)
        do {
            // Lookup framebuffer, and retrieve
            // a pixel that is either one column
            // left or right of the current one.
            // Add index from colormap to index.
			screen[dest] = screen[dest
									+ fuzzoffset[fuzzpos]];
            screen[dest2] = screen[dest];

            // Ironically, "low detail" fuzziness was not really low-detail,
            // as it normally did full-precision calculations.
            // BLURRY_MAP[0x00FF & screen[dest2+ fuzzoffset[fuzzpos]]];

            // Clamp table lookup index.
            if (++fuzzpos == FUZZTABLE)
                fuzzpos = 0;

            dest += SCREENWIDTH;
            dest2 += SCREENWIDTH;
            
			screen[dest] = screen[dest
									+ fuzzoffset[fuzzpos]];
            screen[dest2] = screen[dest];
            if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
            dest += SCREENWIDTH;
            dest2 += SCREENWIDTH;
                
			screen[dest] = screen[dest
									+ fuzzoffset[fuzzpos]];
            screen[dest2] = screen[dest];
            if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
            dest += SCREENWIDTH;
            dest2 += SCREENWIDTH;

			screen[dest] = screen[dest
									+ fuzzoffset[fuzzpos]];
            screen[dest2] = screen[dest];
            if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
            dest += SCREENWIDTH;
            dest2 += SCREENWIDTH;            
        } while ((count-=4) > 4);
        
        if (count>0)
        do {
			screen[dest] = screen[dest
									+ fuzzoffset[fuzzpos]];
            screen[dest2] = screen[dest];
            
            if (++fuzzpos == FUZZTABLE) fuzzpos = 0;

            dest += SCREENWIDTH;
            dest2 += SCREENWIDTH;
        } while (count-- != 0);
        
    }
}