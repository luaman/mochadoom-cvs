package rr;

import static data.Tables.finetangent;
import static m.fixed_t.*;
import static rr.RendererState.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import data.Tables;

import v.IVideoScale;
import v.IVideoScaleAware;

/** This is what actual executes the RenderWallInstruction.
 *  Essentially it's a self-contained column rendering function.
 *  
 *  FIXME there's a complex data dependency with visplanes and 
 *  ceilingclip/floorclip I was not quite able to fix, and probably
 *  it's not worth it. So deprecated it is.
 *  
 * 
 * @author velktron
 *
 */

public class RenderSegExecutor implements Runnable, IVideoScaleAware {

	// This needs to be set by the partitioner.
	private int start, end,numthreads=1;
	// These need to be set on creation, and are unchangeable.
	private final IGetColumn GC;
	private final TextureManager TM;
	private final CyclicBarrier barrier;
	private RenderSegInstruction[] RSI;
	private final long[] xtoviewangle;
	private final int[] ylookup,columnofs;
	private final byte[] screen;
	private final short[] ceilingclip, floorclip;
	private visplane_t visplanes[];
	
	// All that stuff (yikes!) must come from RSIs.
	// It's a reduced set of what RenderSegLoop uses,
	// as we don't actually need to do everything it did,
	// just draw the walls.
	/*private int     rw_x,rw_stopx;
	private int toptexture,midtexture,bottomtexture;
	private int  pixhigh,pixlow,pixhighstep,pixlowstep,
	topfrac,    topstep,bottomfrac, bottomstep;	
	private boolean markceiling,markfloor,segtextured;
	private long     rw_centerangle; // angle_t	
	private int     rw_offset,rw_distance,rw_scale,
	rw_scalestep,rw_midtexturemid,rw_toptexturemid,rw_bottomtexturemid; */

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
			visplane_t[] visplanes,
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
		this.visplanes=visplanes;
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
		
		RenderSegInstruction rsi;

		// For each "SegDraw" instruction...
		for (int i=start;i<end;i++){
			rsi=RSI[i];
			centery=rsi.centery;
			
			// These are going to be modified A LOT, so we cache them here.
			int pixlow=rsi.pixlow;
			int pixhigh=rsi.pixhigh;
			int pixhighstep=rsi.pixhighstep;
			int pixlowstep=rsi.pixlowstep;
			int rw_scale=rsi.rw_scale;
			int topfrac=rsi.topfrac;
			int bottomfrac=rsi.bottomfrac;
			int bottomstep=rsi.bottomstep;
			int rw_scalestep=rsi.rw_scalestep;
			int topstep=rsi.topstep;
			//System.out.printf("Executing RSI %d from %d to %d\n",i,rsi.rw_x,rsi.rw_stopx);
			int     texturecolumn=0; // fixed_t

	        {
	           
	            for ( int rw_x=rsi.rw_x; rw_x < rsi.rw_stopx ; rw_x++)
	            {
	            // mark floor / ceiling areas
	            yl = (topfrac+HEIGHTUNIT-1)>>HEIGHTBITS;

	            // no space above wall?
	            if (yl < ceilingclip[rw_x]+1)
	                yl = ceilingclip[rw_x]+1;
	                
	            yh = bottomfrac>>HEIGHTBITS;

	            if (yh >= floorclip[rw_x])
	                yh = floorclip[rw_x]-1;
	            
	          //  System.out.printf("Thread: rw %d yl %d yh %d\n",rw_x,yl,yh);

	            // A particular seg has been identified as a floor marker.
	            
	            
	            // texturecolumn and lighting are independent of wall tiers
	            if (rsi.segtextured)
	            {
	                // calculate texture offset
	               
	                
	              // CAREFUL: a VERY anomalous point in the code. Their sum is supposed
	              // to give an angle not exceeding 45 degrees (or 0x0FFF after shifting).
	              // If added with pure unsigned rules, this doesn't hold anymore,
	              // not even if accounting for overflow.
	                angle = Tables.toBAMIndex(rsi.rw_centerangle + (int)xtoviewangle[rw_x]);
	                //angle = (int) (((rw_centerangle + xtoviewangle[rw_x])&BITS31)>>>ANGLETOFINESHIFT);
	              //angle&=0x1FFF;
	                
	              // FIXME: We are accessing finetangent here, the code seems pretty confident
	              // in that angle won't exceed 4K no matter what. But xtoviewangle
	              // alone can yield 8K when shifted.
	              // This usually only overflows if we idclip and look at certain directions 
	             // (probably angles get fucked up), however it seems rare enough to just 
	             // "swallow" the exception. You can eliminate it by anding with 0x1FFF if you're so inclined. 
	              
	              texturecolumn = rsi.rw_offset-FixedMul(finetangent[angle],rsi.rw_distance);
	               texturecolumn >>= FRACBITS;
	              // calculate lighting
	              index = rw_scale>>LIGHTSCALESHIFT;
	      

	                if (index >=  MAXLIGHTSCALE )
	                index = MAXLIGHTSCALE-1;

	                dc_colormap = rsi.walllights[index];
	                dc_x = rw_x;
	                dc_iscale = (int) (0xffffffffL / rw_scale);
	            }
	            
	            // draw the wall tiers
	            if (rsi.midtexture!=0)
	            {
	                // single sided line
	                dc_yl = yl;
	                dc_yh = yh;
	                dc_texheight = TM.getTextureheight(rsi.midtexture)>>FRACBITS; // killough
	                dc_texturemid = rsi.rw_midtexturemid;    
	                dc_source = GC.GetColumn(rsi.midtexture,texturecolumn);
	                dc_source_ofs=GC.getDCSourceOffset();
	                CompleteColumn();
	            }
	            else
	            {
	                // two sided line
	                if (rsi.toptexture!=0)
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
	                    dc_texturemid = rsi.rw_toptexturemid;
	                    dc_texheight=TM.getTextureheight(rsi.toptexture)>>FRACBITS;
	                    dc_source = GC.GetColumn(rsi.toptexture,texturecolumn);
	                    dc_source_ofs=GC.getDCSourceOffset();
	                    CompleteColumn();
	                } 
	                } // if toptexture

	                // no top wall
	                    
	                if (rsi.bottomtexture!=0)
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
	                    dc_texturemid = rsi.rw_bottomtexturemid;
	                    dc_texheight=TM.getTextureheight(rsi.bottomtexture)>>FRACBITS;
	                    dc_source = GC.GetColumn(rsi.bottomtexture,
	                                texturecolumn);
	                    dc_source_ofs=GC.getDCSourceOffset();
	                    CompleteColumn();
	                }

	            } // end-bottomtexture
	           } // end-else (two-sided line)
	                rw_scale += rw_scalestep;
	                topfrac += topstep;
	                bottomfrac += bottomstep;
	            } // end-rw 
	        } // end-block
		} // end-instruction

		try {
			barrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		//System.out.printf("Completing column %d %d\n", dc_yh,dc_yl);

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

	public void updateRSI(RenderSegInstruction[] rsi) {
		this.RSI=rsi;
		}
	
	public void updateVisplanes(visplane_t[] vpo) {
		this.visplanes=vpo;
		}
}

// $Log: RenderSegExecutor.java,v $
// Revision 1.3  2011/06/07 00:11:11  velktron
// Fixed alternate parallel renderer (seg based). No longer deprecated.
//