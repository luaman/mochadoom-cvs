package rr.drawfuns;

import i.IDoomSystem;
import static m.fixed_t.FRACBITS;

    /**
	 * Adapted from Killough's Boom code. Specially optimized version assuming
	 * that dc_source_ofs is always 0. This eliminates it from expressions.
	 * 
	 * @author admin
	 * 
	 */

	public final class R_DrawColumnBoomOpt extends DoomColumnFunction<byte[]> {

		public R_DrawColumnBoomOpt(int SCREENWIDTH, int SCREENHEIGHT,
	            int[] ylookup, int[] columnofs, ColVars<byte[]> dcvars,
	            byte[] screen, IDoomSystem I) {
	        super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I);
	    }

        public void invoke() {
			int count;
			int dest; // killough
			int frac; // killough
			final int fracstep;

			count = dcvars.dc_yh - dcvars.dc_yl + 1;

			if (count <= 0) // Zero length, column does not exceed a pixel.
				return;

			if (RANGECHECK) {				
			    performRangeCheck();
			}

			// Framebuffer destination address.
			// Use ylookup LUT to avoid multiply with ScreenWidth.
			// Use columnofs LUT for subwindows?

			dest = computeScreenDest();

			// Determine scaling, which is the only mapping to be done.

			fracstep = dcvars.dc_iscale;
			frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * fracstep;

			// Inner loop that does the actual texture mapping,
			// e.g. a DDA-lile scaling.
			// This is as fast as it gets. (Yeah, right!!! -- killough)
			//
			// killough 2/1/98: more performance tuning

			{
				final byte[] source = dcvars.dc_source;
				final byte[] colormap = dcvars.dc_colormap;
				int heightmask = dcvars.dc_texheight - 1;
				if ((dcvars.dc_texheight & heightmask) != 0) // not a power of 2 --
														// killough
				{
					heightmask++;
					heightmask <<= FRACBITS;

					if (frac < 0)
						while ((frac += heightmask) < 0)
							;
					else
						while (frac >= heightmask)
							frac -= heightmask;

					do {
						// Re-map color indices from wall texture column
						// using a lighting/special effects LUT.

						// heightmask is the Tutti-Frutti fix -- killough

						screen[dest] = colormap[0x00FF & source[((frac >> FRACBITS))]];
						dest += SCREENWIDTH;
						if ((frac += fracstep) >= heightmask)
							frac -= heightmask;
					} while (--count > 0);
				} else {
					while (count >= 4) // texture height is a power of 2 --
										// killough
					{
						// System.err.println(dest);
						screen[dest] = colormap[0x00FF & source[((frac >> FRACBITS) & heightmask)]];
						dest += SCREENWIDTH;
						frac += fracstep;
						screen[dest] = colormap[0x00FF & source[((frac >> FRACBITS) & heightmask)]];
						dest += SCREENWIDTH;
						frac += fracstep;
						screen[dest] = colormap[0x00FF & source[((frac >> FRACBITS) & heightmask)]];
						dest += SCREENWIDTH;
						frac += fracstep;
						screen[dest] = colormap[0x00FF & source[((frac >> FRACBITS) & heightmask)]];
						dest += SCREENWIDTH;
						frac += fracstep;
						count -= 4;
					}

					while (count > 0) {
						screen[dest] = colormap[0x00FF & source[((frac >> FRACBITS) & heightmask)]];
						dest += SCREENWIDTH;
						frac += fracstep;
						count--;
					}
				}
			}
		}
	}