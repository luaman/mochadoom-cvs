package rr.drawfuns;

import i.IDoomSystem;

/**
	 * Framebuffer postprocessing. Creates a fuzzy image by copying pixels from
	 * adjacent ones to left and right. Used with an all black colormap, this
	 * could create the SHADOW effect, i.e. spectres and invisible players.
	 */

	public final class R_DrawFuzzColumn extends DoomColumnFunction<byte[],short[]>  {
		
	    public R_DrawFuzzColumn(int SCREENWIDTH, int SCREENHEIGHT,
                int[] ylookup, int[] columnofs, ColVars<byte[],short[]> dcvars,
                short[] screen, IDoomSystem I,short[] BLURRY_MAP) {
            super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I,BLURRY_MAP);
        
            this.flags=DcFlags.FUZZY;
        
        FUZZOFF = SCREENWIDTH;
        
        // Recompute fuzz table
        
        fuzzoffset= new int[]{ FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
                -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF,
                -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF };
        
        FUZZTABLE=fuzzoffset.length;
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
			int dest;

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
				    super.performRangeCheck();
			}

			// Does not work with blocky mode.
			dest = computeScreenDest();

			// Looks like an attempt at dithering,
			// using the colormap #6 (of 0-31, a bit
			// brighter than average).
			if (count>4) // MAES: unroll by 4
			do {
				// Lookup framebuffer, and retrieve
				// a pixel that is either one column
				// left or right of the current one.
				// Add index from colormap to index.
				screen[dest] = screen[dest
						+ fuzzoffset[fuzzpos]];

				// Clamp table lookup index.
				if (++fuzzpos == FUZZTABLE)
					fuzzpos = 0;

				dest += SCREENWIDTH;				
                
				screen[dest] = screen[dest
										+ fuzzoffset[fuzzpos]];
                if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                dest += SCREENWIDTH;
                
				screen[dest] = screen[dest
										+ fuzzoffset[fuzzpos]];
                if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                dest += SCREENWIDTH;
                
				screen[dest] = screen[dest
										+ fuzzoffset[fuzzpos]];
                if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                dest += SCREENWIDTH;
				
			} while ((count-=4) > 4);
			
			if (count>0)
	         do {
	                // Lookup framebuffer, and retrieve
	                // a pixel that is either one column
	                // left or right of the current one.
	                // Add index from colormap to index.
					screen[dest] = screen[dest
											+ fuzzoffset[fuzzpos]];

	                // Clamp table lookup index.
	                if (++fuzzpos == FUZZTABLE)
	                    fuzzpos = 0;

	                dest += SCREENWIDTH;
	            } while (count-- > 0);
			
		}


	}