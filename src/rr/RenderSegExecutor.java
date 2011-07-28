package rr;

import static data.Tables.finetangent;
import static m.fixed_t.*;
import static rr.RendererState.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import data.Tables;

import v.IVideoScale;
import v.IVideoScaleAware;

/** This is what actual executes the RenderSegInstructions.
 *   *  
 *  Each thread actually operates on a FIXED PORTION OF THE SCREEN
 *  (e.g. half-width, third-width etc.) and only renders the portions
 *  of the RenderSegInstructions that are completely contained
 *  within its own screen area. For this reason, all threads
 *  check out all RenderSegInstructions of the list, and render any 
 *  and all portions that are within their responsability domain, so
 *  to speak.
 *  
 *  FIXME there's a complex data dependency with ceilingclip/floorclip 
 *  I was not quite able to fix yet. Practically, in the serial renderer,
 *  calls to RenderSegLoop are done in a correct, non-overlapping order,
 *  and certain parts are drawn before others in order to set current
 *  floor/ceiling markers and visibility e.g. think of a wall visible
 *  through windows.
 *  
 *  FIXME 7/6/2011 Data dependencies and per-thread clipping are now 
 *  fixed, however there is still visible "jitter" or "noise" on some 
 *  of the walls, probably related to column offsets.
 * 
 * @author velktron
 *
 */

public class RenderSegExecutor implements Runnable, IVideoScaleAware {

	// This needs to be set by the partitioner.
	private int rw_start, rw_end,rsiend;
	// These need to be set on creation, and are unchangeable.
	private final IGetColumn GC;
	private final TextureManager TM;
	private final CyclicBarrier barrier;
	private RenderSegInstruction[] RSI;
	private final long[] xtoviewangle;
	private final int[] ylookup,columnofs;
	private final byte[] screen;
	private final short[] ceilingclip, floorclip;

	// Each thread should do its own ceiling/floor blanking
    private final short[] BLANKFLOORCLIP;
    private final short[] BLANKCEILINGCLIP;
	
	// These are needed to communicate with the column drawer.
	// But are not required to be supplied externally.

	private int dc_x,dc_yl,dc_yh,dc_iscale;
	private byte[] dc_source;
	private byte[] dc_colormap;
	//private volatile int dc_source_ofs;
	private volatile int dc_texheight, dc_texturemid;
	private int centery;


	protected static final int HEIGHTBITS   =   12;
	protected static final int HEIGHTUNIT   =   (1<<HEIGHTBITS);
	private final int id;

	public RenderSegExecutor(int id,byte[] screen, 
			IGetColumn  gc,
			TextureManager texman,
			RenderSegInstruction[] RSI,
			short[] BLANKCEILINGCLIP,
			short[] BLANKFLOORCLIP,
			short[] ceilingclip,
			short[] floorclip,
			int[] columnofs,
			long[] xtoviewangle,
			int[] ylookup,
			visplane_t[] visplanes,
			byte[][] walllights,
			CyclicBarrier barrier){
		this.id=id;
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
		this.BLANKCEILINGCLIP=BLANKCEILINGCLIP;
		this.BLANKFLOORCLIP=BLANKFLOORCLIP;
	}

	
	/** Only called once per screen width change */
	public void setScreenRange(int rwstart, int rwend){
		this.rw_end=rwend;
		this.rw_start=rwstart;
	}

	
	/** How many instructions TOTAL are there to wade through.
	 *  Not all will be executed on one thread, except in some rare
	 *  circumstances. 
	 *  
	 * @param rsiend
	 */
	public void setRSIEnd(int rsiend){
		this.rsiend=rsiend;
	}

	public void run()
	{

		RenderSegInstruction rsi;

		// Each worker blanks its own portion of the floor/ceiling clippers.
		System.arraycopy(BLANKFLOORCLIP,rw_start,floorclip, rw_start,rw_end-rw_start);
		System.arraycopy(BLANKCEILINGCLIP,rw_start,ceilingclip, rw_start,rw_end-rw_start);
		

		int count=0;
		// For each "SegDraw" instruction...
		for (int i=0;i<rsiend;i++){
			rsi=RSI[i];
			centery=RSI[i].centery;
			int startx,endx;
			// Does a wall actually start in our screen zone?
			// If yes, we need no bias, since it was meant for it.
			// If the wall started BEFORE our zone, then we
			// will need to add a bias to it (see ProcessRSI).
			// If its entirely non-contained, ProcessRSI won't be
			// called anyway, so we don't need to check for the end.
			
			boolean contained=(rsi.rw_x>=rw_start);
			// Keep to your part of the screen. It's possible that several
			// threads will process the same RSI, but different parts of it.
			
				// Trim stuff that starts before our rw_start position.
				startx=Math.max(rsi.rw_x,rw_start);
				// Similarly, trim stuff after our rw_end position.
				endx=Math.min(rsi.rw_stopx,rw_end);
				// Is there anything to actually draw?
				if ((endx-startx)>0) {
					//System.out.printf("RSI %d from %d to %d trimmed to %d %d (%d %d)\n",i,rsi.rw_x,rsi.rw_stopx,startx,endx,rw_start,rw_end);
					ProcessRSI(rsi,startx,endx,contained);
					count++;
					}
		} // end-instruction

		//System.out.printf("Thread %s %d %d executed rsi %d\n",this.toString(),rw_start,rw_end,count);
		
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
	protected final void CompleteColumn(){
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

					screen[dest] = colormap[0x00FF&source[((frac>>FRACBITS))]];
					dest += SCREENWIDTH; 
					if ((frac += fracstep) >= heightmask)
						frac -= heightmask;
				} 
				while (--count>0);
			}
			else
				while (count>=4)   // texture height is a power of 2 -- killough
				{
					screen[dest] = colormap[0x00FF&source[((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					screen[dest] = colormap[0x00FF&source[((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					screen[dest] = colormap[0x00FF&source[((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					screen[dest] = colormap[0x00FF&source[((frac>>FRACBITS) & heightmask)]];
					dest += SCREENWIDTH; 
					frac += fracstep;
					count-=4;
				}

			while (count>0){
				screen[dest] = colormap[0x00FF&source[((frac>>FRACBITS) & heightmask)]];
				dest += SCREENWIDTH; 
				frac += fracstep;
				count--;
			}
		}
	}
	
	protected final void ProcessRSI(RenderSegInstruction rsi, int startx,int endx,boolean contained){
		int     angle; // angle_t
		int     index;
		int     yl; // low
		int     yh; // hight
		int     mid;
		int pixlow,pixhigh,pixhighstep,pixlowstep;
		int rw_scale,topfrac,bottomfrac,bottomstep;
		// These are going to be modified A LOT, so we cache them here.
		pixhighstep=rsi.pixhighstep;
		pixlowstep=rsi.pixlowstep;
		bottomstep=rsi.bottomstep;
		// 	We must re-scale it.
		int rw_scalestep= rsi.rw_scalestep; 
		int topstep=rsi.topstep;
		int     texturecolumn=0; // fixed_t
		final int bias;
		// Well is entirely contained in our screen zone 
		// (or the very least it starts in it).
		if (contained) bias=0; 
			// We are continuing a wall that started in another 
		    // screen zone.
			else bias=(startx-rsi.rw_x);
		// PROBLEM: these must be pre-biased when multithreading.
		    rw_scale=rsi.rw_scale+bias*rw_scalestep;
		    topfrac = rsi.topfrac+bias*topstep;
		    bottomfrac = rsi.bottomfrac+ bias*bottomstep;
			pixlow=rsi.pixlow+bias*pixlowstep;
			pixhigh=rsi.pixhigh+bias*pixhighstep;

        {
           
            for ( int rw_x=startx; rw_x < endx ; rw_x++)
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
                dc_source = GC.GetCachedColumn(rsi.midtexture,texturecolumn);
                //dc_source_ofs=0;
                CompleteColumn();
                ceilingclip[rw_x] = (short) rsi.viewheight;
                floorclip[rw_x] = -1;
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
                    dc_source = GC.GetCachedColumn(rsi.toptexture,texturecolumn);
                    //dc_source_ofs=0;
                    CompleteColumn();
    				ceilingclip[rw_x] = (short) mid;
				}
				else
					ceilingclip[rw_x] = (short) (yl-1);
				}  // if toptexture
				else
				{
					// no top wall
					if (rsi.markceiling)
						ceilingclip[rw_x] = (short) (yl-1);
				} 
                    
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
                    dc_source = GC.GetCachedColumn(rsi.bottomtexture,texturecolumn);
                    // dc_source_ofs=0;
                    CompleteColumn();
					floorclip[rw_x] = (short) mid;
				}
				else
					 floorclip[rw_x] = (short) (yh+1);

            } // end-bottomtexture
			else
			{
				// no bottom wall
				if (rsi.markfloor)
					floorclip[rw_x] = (short) (yh+1);
			}
                
           } // end-else (two-sided line)
                rw_scale += rw_scalestep;
                topfrac += topstep;
                bottomfrac += bottomstep;
            } // end-rw 
        } // end-block
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
}

// $Log: RenderSegExecutor.java,v $
// Revision 1.7.2.4  2011/07/28 08:18:47  velktron
// Merged in v1.5 release cleanups and some last-minute fixes.
//
// Revision 1.10  2011/07/25 11:39:10  velktron
// Optimized to work without dc_source_ofs (uses only cached, solid textures)
//
// Revision 1.9  2011/07/12 16:29:35  velktron
// Now using GetCachedColumn
//
// Revision 1.8  2011/07/12 16:25:02  velktron
// Removed dependency on per-thread column pointers.
//
// Revision 1.7  2011/06/07 21:21:15  velktron
// Definitively fixed jitter bug, which was due to dc_offset_contention. Now the alternative parallel renderer is just as good as the original one.
//
// Revision 1.6  2011/06/07 13:35:38  velktron
// Definitively fixed drawing priority/zones. Now to solve the jitter :-/
//
// Revision 1.5  2011/06/07 01:32:32  velktron
// Parallel Renderer 2 still buggy :-(
//
// Revision 1.4  2011/06/07 00:50:47  velktron
// Alternate Parallel Renderer fixed.
//
// Revision 1.3  2011/06/07 00:11:11  velktron
// Fixed alternate parallel renderer (seg based). No longer deprecated.
//