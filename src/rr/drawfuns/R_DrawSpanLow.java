package rr.drawfuns;

import i.IDoomSystem;

public final class R_DrawSpanLow extends DoomSpanFunction<byte[],short[]> {

		public R_DrawSpanLow(int sCREENWIDTH, int sCREENHEIGHT, int[] ylookup,
            int[] columnofs, SpanVars<byte[],short[]> dsvars, short[] screen,
            IDoomSystem I) {
        super(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I);
        // TODO Auto-generated constructor stub
    }

        @Override
		public void invoke() {
		    final byte[] ds_source= dsvars.ds_source;
		    final short[] ds_colormap= dsvars.ds_colormap;
		    final int ds_xstep=dsvars.ds_xstep;
		    final int ds_ystep=dsvars.ds_ystep;
			int f_xfrac=dsvars.ds_xfrac;
			int f_yfrac=dsvars.ds_yfrac;
			int dest;
			int count;
			int spot;

			if (RANGECHECK) {
			    doRangeCheck();
				// dscount++;
			}

			// MAES: count must be performed before shifting.
			count = dsvars.ds_x2 - dsvars.ds_x1;
			
			// Blocky mode, need to multiply by 2.
			dsvars.ds_x1 <<= 1;
			dsvars.ds_x2 <<= 1;

			dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1];			
			
			do {
				spot = ((f_yfrac >> (16 - 6)) & (63 * 64))
						+ ((f_xfrac >> 16) & 63);
				// Lowres/blocky mode does it twice,
				// while scale is adjusted appropriately.

				screen[dest++] = ds_colormap[0x00FF & ds_source[spot]];
				screen[dest++] = ds_colormap[0x00FF & ds_source[spot]];

				f_xfrac += ds_xstep;
				f_yfrac += ds_ystep;

			} while (count-- > 0);

		}

	}