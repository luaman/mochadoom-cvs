package rr;

/** This is all the information needed to draw a particular SEG. It's quite
 *  a lot, actually. 24 entries!
 *  
 */

public class RenderSegInstruction {    
	public int     rw_x,rw_stopx;
	public int toptexture,midtexture,bottomtexture;
	public int  pixhigh,pixlow,pixhighstep,pixlowstep,
	topfrac,    topstep,bottomfrac, bottomstep;	
	public boolean markceiling,markfloor,segtextured;
	public long     rw_centerangle; // angle_t
	/** fixed_t */
	public int     rw_offset,rw_distance,rw_scale,
	rw_scalestep,rw_midtexturemid,rw_toptexturemid,rw_bottomtexturemid;
}
