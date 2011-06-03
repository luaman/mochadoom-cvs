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
 * @author velktron
 *
 */

@Deprecated
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
		int top,bottom;
		
		RenderSegInstruction rsi;

		// For each "SegDraw" instruction...
		for (int i=end-1;i>=start;i--){
			rsi=RSI[i];
			//System.out.printf("Executing RSI %d from %d to %d\n",i,rsi.rw_x,rsi.rw_stopx);
			int     texturecolumn=0; // fixed_t
			for ( ; rsi.rw_x < rsi.rw_stopx ; rsi.rw_x++)
            {
            // mark floor / ceiling areas
            yl = (rsi.topfrac+HEIGHTUNIT-1)>>HEIGHTBITS;

            // no space above wall?
            if (yl < ceilingclip[rsi.rw_x]+1)
                yl = ceilingclip[rsi.rw_x]+1;
            
            if (rsi.markceiling)
            {
                /*top = ceilingclip[rsi.rw_x]+1;
                bottom = yl-1;

                if (bottom >= floorclip[rw_x])
                bottom = floorclip[rw_x]-1;

                if (top <= bottom)
                {
                    visplanes[ceilingplane].setTop(rw_x,(char) top);
                    visplanes[ceilingplane].setBottom(rw_x, (char) bottom);
                } */
            }
                
            yh = rsi.bottomfrac>>HEIGHTBITS;

            if (yh >= floorclip[rsi.rw_x])
                yh = floorclip[rsi.rw_x]-1;

            // A particular seg has been identified as a floor marker.
            
            if (rsi.markfloor)
            {
                top = yh+1;
                bottom = floorclip[rsi.rw_x]-1;
                if (top <= ceilingclip[rsi.rw_x])
                top = ceilingclip[rsi.rw_x]+1;

                if (top <= bottom)
                {
                visplanes[rsi.floorplane].setTop(rsi.rw_x, (char) top);
                visplanes[rsi.floorplane].setBottom(rsi.rw_x,  (char) bottom);
                }
            }
            
            // texturecolumn and lighting are independent of wall tiers
            if (rsi.segtextured)
            {
                // calculate texture offset
               
                angle = Tables.toBAMIndex(rsi.rw_centerangle + (int)xtoviewangle[rsi.rw_x]);

              
              texturecolumn = rsi.rw_offset-FixedMul(finetangent[angle],rsi.rw_distance);
              texturecolumn >>= FRACBITS;
              // calculate lighting
              index = rsi.rw_scale>>LIGHTSCALESHIFT;
      

                if (index >=  MAXLIGHTSCALE )
                index = MAXLIGHTSCALE-1;

                dc_colormap = rsi.walllights[index];
                dc_x = rsi.rw_x;
                dc_iscale = (int) (0xffffffffL / rsi.rw_scale);
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
                ceilingclip[rsi.rw_x] = (short) rsi.viewheight;
                floorclip[rsi.rw_x] = -1;
            }
            else
            {
                // two sided line
                if (rsi.toptexture!=0)
                {
                // top wall
                mid = rsi.pixhigh>>HEIGHTBITS;
                rsi.pixhigh += rsi.pixhighstep;

                if (mid >= floorclip[rsi.rw_x])
                    mid = floorclip[rsi.rw_x]-1;

                if (mid >= yl)
                {
                    dc_yl = yl;
                    dc_yh = mid;
                    dc_texturemid = rsi.rw_toptexturemid;
                    dc_texheight=TM.getTextureheight(rsi.toptexture)>>FRACBITS;
                    dc_source = GC.GetColumn(rsi.toptexture,texturecolumn);
                    dc_source_ofs=GC.getDCSourceOffset();
                    CompleteColumn();
                    ceilingclip[rsi.rw_x] = (short) mid;
                }
                else
                    ceilingclip[rsi.rw_x] = (short) (yl-1);
                }
                else
                {
                // no top wall
                if (rsi.markceiling)
                    ceilingclip[rsi.rw_x] = (short) (yl-1);
                }
                    
                if (rsi.bottomtexture!=0)
                {
                // bottom wall
                mid = (rsi.pixlow+HEIGHTUNIT-1)>>HEIGHTBITS;
                rsi.pixlow += rsi.pixlowstep;

                // no space above wall?
                if (mid <= ceilingclip[rsi.rw_x])
                    mid = ceilingclip[rsi.rw_x]+1;
                
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
        
                    
                    floorclip[rsi.rw_x] = (short) mid;
                }
                else
                    floorclip[rsi.rw_x] = (short) (yh+1);
                }
                else
                {
                // no bottom wall
                if (rsi.markfloor)
                    floorclip[rsi.rw_x] = (short) (yh+1);
                }
                    
            }
                
            rsi.rw_scale += rsi.rw_scalestep;
            rsi.topfrac += rsi.topstep;
            rsi.bottomfrac +=rsi.bottomstep;
            }
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
