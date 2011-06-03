package rr;

import static data.Tables.finetangent;
import static m.fixed_t.*;
import static rr.RendererState.*;
import java.util.concurrent.CyclicBarrier;

import data.Tables;

import v.IVideoScale;
import v.IVideoScaleAware;

/** This is what actual executes the RenderWallInstruction.
 *  Essentially it's a self-contained column rendering function.
 * 
 * @author admin
 *
 */

public class RenderSegExecutor implements Runnable, IVideoScaleAware {

	// This needs to be set by the partitioner.
	private int start, end,numthreads=1;
	// These need to be set on creation, and are unchangeable.
	private final IGetColumn GC;
	private final TextureManager TM;
	private final CyclicBarrier barrier;
	private final RenderSegInstruction[] RSI;
	private final long[] xtoviewangle;
	private final int[] ylookup,columnofs;
	private final byte[] screen;
	private final byte[][] walllights;
	private final short[] ceilingclip, floorclip;
	
	// All that stuff (yikes!) must come from RSIs.
	// It's a reduced set of what RenderSegLoop uses,
	// as we don't actually need to do everything it did,
	// just draw the walls.
	private int     rw_x,rw_stopx;
	private int toptexture,midtexture,bottomtexture;
	private int  pixhigh,pixlow,pixhighstep,pixlowstep,
	topfrac,    topstep,bottomfrac, bottomstep;	
	private boolean markceiling,markfloor,segtextured;
	private long     rw_centerangle; // angle_t
	/** fixed_t */
	private int     rw_offset,rw_distance,rw_scale,
	rw_scalestep,rw_midtexturemid,rw_toptexturemid,rw_bottomtexturemid;

	// These are needed to communicate with the column drawer.
	// But are not required to be supplied externally.
	
	private int dc_x,dc_yl,dc_yh,dc_iscale;
	private byte[] dc_source;
	private byte[] dc_colormap;
	private int dc_source_ofs;
	private int dc_texheight, dc_texturemid;
	private int centery;

	
	protected static final int HEIGHTBITS   =   12;
	protected static final int HEIGHTUNIT   =   (1<<HEIGHTBITS);

	public RenderSegExecutor(byte[] screen, 
			IGetColumn  gc,
			TextureManager texman,
			RenderSegInstruction[] RSI,
			short[] ceilingclip,
			short[] floorclip,
			int[] columnofs,
			long[] xtoviewangle,
			int[] ylookup,
			byte[][] walllights,
			CyclicBarrier barrier){
		this.GC=gc;
		this.TM=texman;
		this.screen=screen;
		this.RSI=RSI;
		this.barrier=barrier;		
		this.ceilingclip=ceilingclip;
		this.floorclip=floorclip;
		this.xtoviewangle=xtoviewangle;
		this.columnofs=columnofs;
		this.ylookup=ylookup;
		this.walllights=walllights;
	}

	public void setRange(int start, int end){
		this.end=end;
		this.start=start;
	}

	public void setRange(int start, int end,int numthreads){
		this.end=end;
		this.start=start;
		this.numthreads=numthreads;
	}

	class seginfo_t{
		int rw_x;
		int rw_stopx;
		int topfrac;
		int[] ceilingclip,floorclip;
		boolean markceiling,markfloor,segtextured;
		int bottomfrac;
	}

	seginfo_t s;
	
	public void run()
	{
		int     angle; // angle_t
		int     index;
		int         yl; // low
		int         yh; // hight
		int         mid;
		int     texturecolumn=0; // fixed_t
		int         top;
		int         bottom;

		// For each "SegDraw" instruction...
		for (int i=start;i<end;i++){

			for ( ; rw_x < rw_stopx ; rw_x++)
			{
				// mark floor / ceiling areas
				yl = (topfrac+HEIGHTUNIT-1)>>HEIGHTBITS;

			// no space above wall?
			if (yl < ceilingclip[rw_x]+1)
				yl = ceilingclip[rw_x]+1;

			if (markceiling)
			{
				top = ceilingclip[rw_x]+1;
				bottom = yl-1;

				if (bottom >= floorclip[rw_x])
					bottom = floorclip[rw_x]-1;

				/*if (top <= bottom)
            {
                visplanes[ceilingplane].setTop(rw_x,(char) top);
                visplanes[ceilingplane].setBottom(rw_x, (char) bottom);
            } */
			}

			yh = bottomfrac>>HEIGHTBITS;

	if (yh >= floorclip[rw_x])
		yh = floorclip[rw_x]-1;

	// A particular seg has been identified as a floor marker.

	if (markfloor)
	{
		top = yh+1;
		bottom = floorclip[rw_x]-1;
		if (top <= ceilingclip[rw_x])
			top = ceilingclip[rw_x]+1;

		// We don't need to set visplanes here.
	}

	// texturecolumn and lighting are independent of wall tiers
	if (segtextured)
	{
		// calculate texture offset

		angle = Tables.toBAMIndex(rw_centerangle + (int)xtoviewangle[rw_x]); 

		texturecolumn = rw_offset-FixedMul(finetangent[angle],rw_distance);
		texturecolumn >>= FRACBITS;
		// calculate lighting
		index = rw_scale>>LIGHTSCALESHIFT;


		if (index >=  MAXLIGHTSCALE )
			index = MAXLIGHTSCALE-1;

		dc_colormap = walllights[index];
		dc_x = rw_x;
		dc_iscale = (int) (0xffffffffL / rw_scale);
	}

	// draw the wall tiers
	if (midtexture!=0)
	{
		// single sided line
		dc_yl = yl;
		dc_yh = yh;
		dc_texheight = TM.getTextureheight(midtexture)>>FRACBITS; // killough
		dc_texturemid = rw_midtexturemid;              
		dc_source = GC.GetColumn(midtexture,texturecolumn);
		dc_source_ofs=GC.getDCSourceOffset();
		CompleteColumn();
	}
	else
	{
		// two sided line
		if (toptexture!=0)
		{
			// top wall
			mid = pixhigh>>HEIGHTBITS;
			pixhigh += pixhighstep;

			if (mid >= floorclip[rw_x])
				mid = floorclip[rw_x]-1;

			if (mid >= yl)
			{
				dc_yl = yl;
				dc_yh = mid;
				dc_texturemid = rw_toptexturemid;
				dc_texheight=TM.getTextureheight(toptexture)>>FRACBITS;
				dc_source = GC.GetColumn(toptexture,texturecolumn);
				dc_source_ofs=GC.getDCSourceOffset();
				CompleteColumn();
			}
		}

		if (bottomtexture!=0)
		{
			// bottom wall
			mid = (pixlow+HEIGHTUNIT-1)>>HEIGHTBITS;
			pixlow += pixlowstep;

			// no space above wall?
			if (mid <= ceilingclip[rw_x])
				mid = ceilingclip[rw_x]+1;

			if (mid <= yh)
			{
				dc_yl = mid;
				dc_yh = yh;
				dc_texturemid = rw_bottomtexturemid;
				dc_texheight=TM.getTextureheight(bottomtexture)>>FRACBITS;
			dc_source = GC.GetColumn(bottomtexture,
					texturecolumn);
			dc_source_ofs=GC.getDCSourceOffset();
			CompleteColumn();
			}
		}
	}

	rw_scale += rw_scalestep;
	topfrac += topstep;
	bottomfrac += bottomstep;
			}
		} // end-instruction
	}


	// Essentially, it's the Boom/Killough column drawing function.
	protected void CompleteColumn(){
		int              count; 
		int dest;            // killough
		int  frac;            // killough
		int fracstep;     

		count = dc_yh - dc_yl + 1; 

		if (count <= 0)    // Zero length, column does not exceed a pixel.
			return; 

		// Framebuffer destination address.
		// Use ylookup LUT to avoid multiply with ScreenWidth.
		// Use columnofs LUT for subwindows? 

		dest = ylookup[dc_yl] + columnofs[dc_x];  

		// Determine scaling, which is the only mapping to be done.

		fracstep = dc_iscale; 
		frac = dc_texturemid + (dc_yl-centery)*fracstep; 

		// Inner loop that does the actual texture mapping,
		//  e.g. a DDA-lile scaling.
		// This is as fast as it gets.       (Yeah, right!!! -- killough)
		//
		// killough 2/1/98: more performance tuning

		{
			final byte[] source = dc_source;       
			final byte[] colormap = dc_colormap; 
			int heightmask = dc_texheight-1;
			if ((dc_texheight & heightmask)!=0)   // not a power of 2 -- killough
			{
				heightmask++;
				heightmask <<= FRACBITS;

				if (frac < 0)
					while ((frac += heightmask) <  0);
				else
					while (frac >= heightmask)
						frac -= heightmask;

				do
				{
					// Re-map color indices from wall texture column
					//  using a lighting/special effects LUT.

					// heightmask is the Tutti-Frutti fix -- killough

					screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS))]];
					dest += SCREENWIDTH; 
					if ((frac += fracstep) >= heightmask)
						frac -= heightmask;
				} 
				while (--count>0);
			}
			else
				while (count>=4)   // texture height is a power of 2 -- killough
				{
					screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					count-=4;
				}

			while (count>0){
				screen[dest] = colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS) & heightmask)]];
				dest += SCREENWIDTH; 
				frac += fracstep;
				count--;
			}
		}
	}

	////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

	protected int SCREENWIDTH;
	protected int SCREENHEIGHT;
	protected IVideoScale vs;

	@Override
	public void setVideoScale(IVideoScale vs) {
		this.vs=vs;
	}

	@Override
	public void initScaling() {
		this.SCREENHEIGHT=vs.getScreenHeight();
		this.SCREENWIDTH=vs.getScreenWidth();
	}

}
