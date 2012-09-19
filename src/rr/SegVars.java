package rr;

import data.Limits;
import utils.C2JUtils;

public class SegVars {
	// /// FROM BSP /////////
	
	protected int MAXDRAWSEGS = Limits.MAXDRAWSEGS;

	/** pointer to drawsegs */
	protected int ds_p;

	protected drawseg_t[] drawsegs;



	/**
	 * R_ClearDrawSegs
	 * 
	 * The drawseg list is reset by pointing back at 0.
	 * 
	 */
	public void ClearDrawSegs() {
		ds_p = 0;
	}

	protected final void ResizeDrawsegs() {
		drawsegs = C2JUtils.resize(drawsegs[0], drawsegs, drawsegs.length*2);
	}
}
