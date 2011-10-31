package rr.drawfuns;

import i.IDoomSystem;

/**
 * Drawspan loop unrolled by 4. However it has low rendering quality and bad
 * distortion. However it does actually does give a small speed boost (120
 * -> 130 fps with a Mul of 3.0)
 * 
 */

public final class R_DrawSpanUnrolled extends DoomSpanFunction<byte[]> {

	public R_DrawSpanUnrolled(int SCREENWIDTH, int SCREENHEIGHT,
            int[] ylookup, int[] columnofs, ColVars<byte[]> dcvars,
            byte[] screen, IDoomSystem I) {
        super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I);
    }

	public void invoke() {
		int position, step;
		byte[] source;
		byte[] colormap;
		int dest;
		int count;
		int spot;
		int xtemp;
		int ytemp;

		position = ((dsvars.ds_xfrac << 10) & 0xffff0000)
				| ((dsvars.ds_yfrac >> 6) & 0xffff);
		step = ((dsvars.ds_xstep << 10) & 0xffff0000) | ((dsvars.ds_ystep >> 6) & 0xffff);
		source = dsvars.ds_source;
		colormap = dsvars.ds_colormap;
		dest = dsvars.ylookup[dsvars.ds_y] + dsvars.columnofs[dsvars.ds_x1];
		count = dsvars.ds_x2 - dsvars.ds_x1 + 1;
		//int rolls = 0;
		while (count >= 4) {
			ytemp = position >> 4;
			ytemp = ytemp & 0xfc0;
			xtemp = position >>> 26;
			spot = xtemp | ytemp;
			position += step;
			screen[dest] = colormap[0x00FF & source[spot]];
			ytemp = position >> 4;
			ytemp = ytemp & 0xfc0;
			xtemp = position >>> 26;
			spot = xtemp | ytemp;
			position += step;
			screen[dest + 1] = colormap[0x00FF & source[spot]];
			ytemp = position >> 4;
			ytemp = ytemp & 0xfc0;
			xtemp = position >>> 26;
			spot = xtemp | ytemp;
			position += step;
			screen[dest + 2] = colormap[0x00FF & source[spot]];
			ytemp = position >> 4;
			ytemp = ytemp & 0xfc0;
			xtemp = position >>> 26;
			spot = xtemp | ytemp;
			position += step;
			screen[dest + 3] = colormap[0x00FF & source[spot]];
			count -= 4;
			dest += 4;

			// Half-assed attempt to fix precision by forced periodic
			// realignment.

			/*
			 * if ((rolls++)%64==0){ position =
			 * ((((rolls*4)*ds_xstep+ds_xfrac) << 10) & 0xffff0000) |
			 * ((((rolls*4)*ds_ystep+ds_yfrac) >> 6) & 0xffff); }
			 */

		}

		while (count > 0) {
			ytemp = position >> 4;
			ytemp = ytemp & 4032;
			xtemp = position >>> 26;
			spot = xtemp | ytemp;
			position += step;
			screen[dest++] = colormap[0x00FF & source[spot]];
			count--;
		}
	}

}
