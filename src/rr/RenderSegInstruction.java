package rr;

/** This is all the information needed to draw a particular SEG. It's quite
 *  a lot, actually. 27 entries!
 *  
 *  It's deprecated, like the class that use it.
 *  
 */

public class RenderSegInstruction {    
	public int     rw_x,rw_stopx;
	public int toptexture,midtexture,bottomtexture;
	public int  pixhigh,pixlow,pixhighstep,pixlowstep,
	topfrac,    topstep,bottomfrac, bottomstep;	
	public boolean segtextured,markfloor,markceiling;
	public long     rw_centerangle; // angle_t
	/** fixed_t */
	public int     rw_offset,rw_distance,rw_scale,
	rw_scalestep,rw_midtexturemid,rw_toptexturemid,rw_bottomtexturemid;
	public int viewheight;
	byte[][] walllights;
	public int ceilingplane;
	public int floorplane;
	public int centery;
}
