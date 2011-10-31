package rr;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import rr.drawfuns.ColVars;
import rr.drawfuns.DoomColumnFunction;
import rr.drawfuns.R_DrawColumnBoom;
import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawColumnBoomOptLow;

/** This is what actual executes the RenderWallInstruction.
 *  Essentially it's a self-contained column rendering function.
 * 
 * @author admin
 *
 */

public class RenderWallExecutor implements Runnable {
    
    private CyclicBarrier barrier;
    private ColVars<byte[]>[] RWI;
    private int start, end;
    private final DoomColumnFunction<byte[]> colfunchi, colfunclow;
    private DoomColumnFunction<byte[]> colfunc;
    
    
    public RenderWallExecutor(int SCREENWIDTH,int SCREENHEIGHT,int[] columnofs,int[] ylookup, byte[] screen, ColVars<byte[]>[] RWI, CyclicBarrier barrier){
        this.RWI=RWI;
        this.barrier=barrier;
        this.SCREENWIDTH=SCREENWIDTH;
        this.SCREENHEIGHT=SCREENHEIGHT;
        
        colfunc=colfunchi=new R_DrawColumnBoomOpt(SCREENWIDTH, SCREENHEIGHT,ylookup,columnofs,null,screen,null );
        colfunclow=new R_DrawColumnBoomOptLow(SCREENWIDTH, SCREENHEIGHT,ylookup,columnofs,null,screen,null );
        
        
    }
    
    public void setRange(int start, int end){
        this.end=end;
        this.start=start;
    }
    
    public void setDetail(int detailshift){
        if (detailshift==0) colfunc=colfunchi;
            else colfunc=colfunclow;
        }

    public void run(){

        //System.out.println("Wall executor from "+start +" to "+ end);
        
        for (int i=start;i<end;i++){
            colfunc.invoke(RWI[i]);
            
            
        // Copy shit over from current RWIs...     
        /*centery=RWI[i].centery;
        dc_iscale=RWI[i].dc_iscale;
        //dc_source_ofs=RWI[i].dc_source_ofs;
        dc_texturemid=RWI[i].dc_texturemid;
        dc_x=RWI[i].dc_x;
        dc_yh=RWI[i].dc_yh;
        dc_yl=RWI[i].dc_yl;
        dc_texheight=RWI[i].dc_texheight;
        dc_colormap=RWI[i].dc_colormap; 
        dc_source=RWI[i].dc_source;
        
        int              count; 
        int dest;            // killough
        int  frac;            // killough
        int fracstep;     
        
        count = dc_yh - dc_yl + 1; 

        if (count <= 0)    // Zero length, column does not exceed a pixel.
          continue; 
                                       
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
        if (dc_colormap!=null)
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
        	     try{
               screen[dest] = colormap[0x00FF&source[((frac>>FRACBITS) & heightmask)]];
               dest += SCREENWIDTH; 
               frac += fracstep;
        	     } catch (Exception e){
        	         System.err.printf("RWI %d bad!\n",i);
        	     }
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
        }*/
        }
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

    public void updateRWI(ColVars<byte[]>[] RWI) {
        this.RWI=RWI;
        
    }
    
////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

    protected final int SCREENWIDTH;
    protected final int SCREENHEIGHT;
    /*
    protected IVideoScale vs;

    @Override
    public void setVideoScale(IVideoScale vs) {
        this.vs=vs;
    }

    @Override
    public void initScaling() {
        this.SCREENHEIGHT=vs.getScreenHeight();
        this.SCREENWIDTH=vs.getScreenWidth();
    }*/

}
