package rr.parallel;

import static data.Defines.ANGLETOSKYSHIFT;
import static data.Defines.PU_STATIC;
import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.BITS32;
import static data.Tables.addAngles;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FixedMul;
import static rr.LightsAndColors.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import rr.PlaneDrawer;
import rr.flat_t;
import rr.visplane_t;
import rr.drawfuns.ColVars;
import rr.drawfuns.DoomColumnFunction;
import rr.drawfuns.DoomSpanFunction;
import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawColumnBoomOptLow;
import rr.drawfuns.R_DrawSpanLow;
import rr.drawfuns.R_DrawSpanUnrolled;
import rr.drawfuns.SpanVars;

/** Visplane worker which shares work in an equal screen-portions strategy.
 * 
 * More balanced, but requires careful synchronization to avoid overdrawing and
 * stomping.
 *  
 *  
 * @author vepitrop.
 *
 */

public abstract class VisplaneWorker2<T,V> extends PlaneDrawer<T,V> implements Runnable{

    protected final int id;
    protected final int NUMFLOORTHREADS;
    protected int startvp;  
    protected int endvp;
    protected int vpw_planeheight;
    protected V[] vpw_planezlight;
    protected int vpw_basexscale,vpw_baseyscale;
    protected final SpanVars<T,V> vpw_dsvars;
    protected final ColVars<T,V> vpw_dcvars;
    protected DoomSpanFunction<T,V> vpw_spanfunc;
    protected DoomColumnFunction<T,V> vpw_skyfunc;
    protected final DoomSpanFunction<T,V> vpw_spanfunchi;
    protected final DoomSpanFunction<T,V> vpw_spanfunclow;
    protected final DoomColumnFunction<T,V> vpw_skyfunchi;
    protected final DoomColumnFunction<T,V> vpw_skyfunclow;
    protected visplane_t pln;
    
    public VisplaneWorker2(int id,int sCREENWIDTH, int sCREENHEIGHT, int[] columnofs,
            short[] screen,CyclicBarrier visplanebarrier,int NUMFLOORTHREADS) {
        super(null);
        this.barrier=visplanebarrier;
        this.id=id;
        // Alias to those of Planes.
        vpw_dsvars=new SpanVars<T,V>();
        vpw_dcvars=new ColVars<T,V>();
        this.NUMFLOORTHREADS=NUMFLOORTHREADS;
    }

    public static class HiColor extends VisplaneWorker2<byte[],short[]>{

        public HiColor(int id, int sCREENWIDTH, int sCREENHEIGHT,
                int[] columnofs, int[] ylookup, short[] screen,
                CyclicBarrier visplanebarrier, int NUMFLOORTHREADS) {
            super(id, sCREENWIDTH, sCREENHEIGHT, columnofs, ylookup, screen,
                    visplanebarrier, NUMFLOORTHREADS);
            vpw_spanfunc=vpw_spanfunchi=new R_DrawSpanUnrolled.HiColor(sCREENWIDTH,sCREENHEIGHT,ylookup,columnofs,vpw_dsvars,screen,I);
            vpw_spanfunclow=new R_DrawSpanLow.HiColor(sCREENWIDTH,sCREENHEIGHT,ylookup,columnofs,vpw_dsvars,screen,I);
            vpw_skyfunc=vpw_skyfunchi=new R_DrawColumnBoomOpt.HiColor(sCREENWIDTH,sCREENHEIGHT,ylookup,columnofs,vpw_dcvars,screen,I);
            vpw_skyfunclow=new R_DrawColumnBoomOptLow.HiColor(sCREENWIDTH,sCREENHEIGHT,ylookup,columnofs,vpw_dcvars,screen,I);

        }
        
    }
    
    @Override
    public void run() {
        pln=null; //visplane_t
        // These must override the global ones

        int         light;
        int         x;
        int         stop;
        int         angle;
        int minx,maxx;
        
        // Now it's a good moment to set them.
        vpw_basexscale=vpvars.getBaseXScale();
        vpw_baseyscale=vpvars.getBaseYScale();
        
        startvp=((id*view.width)/NUMFLOORTHREADS);
        endvp=(((id+1)*view.width)/NUMFLOORTHREADS);
        
        // TODO: find a better way to split work. As it is, it's very uneven
        // and merged visplanes in particular are utterly dire.
        
         for (int pl= 0; pl <vpvars.lastvisplane; pl++) {
             pln=vpvars.visplanes[pl];
            // System.out.println(id +" : "+ pl);

        // Trivial rejection.
         if ((pln.minx > endvp) || (pln.maxx <startvp)) 
                 continue;

         // Reject non-visible  
         if (pln.minx > pln.maxx)
             continue;

         // Trim to zone
         minx=Math.max(pln.minx,startvp);
         maxx=Math.min(pln.maxx,endvp);
         
         // sky flat
         if (pln.picnum == TexMan.getSkyFlatNum() )
         {
             // Cache skytexture stuff here. They aren't going to change while
             // being drawn, after all, are they?
             int skytexture=TexMan.getSkyTexture();
             // MAES: these must be updated to keep up with screen size changes.
             vpw_dcvars.viewheight=view.height;
             vpw_dcvars.centery=view.centery;
             vpw_dcvars.dc_texheight=TexMan.getTextureheight(skytexture)>>FRACBITS;                 
             vpw_dcvars.dc_iscale = vpvars.getSkyScale()>>view.detailshift;
             
             vpw_dcvars.dc_colormap = colormap.colormaps[0];
             vpw_dcvars.dc_texturemid = TexMan.getSkyTextureMid();
             for (x=minx ; x <= maxx ; x++)
             {
           
                 vpw_dcvars.dc_yl = pln.getTop(x);
                 vpw_dcvars.dc_yh = pln.getBottom(x);
             
             if (vpw_dcvars.dc_yl <= vpw_dcvars.dc_yh)
             {
                 angle = (int) (addAngles(view.angle, view.xtoviewangle[x])>>>ANGLETOSKYSHIFT);
                 vpw_dcvars.dc_x = x;
                 vpw_dcvars.dc_texheight=TexMan.getTextureheight(TexMan.getSkyTexture())>>FRACBITS;
                 vpw_dcvars.dc_source = TexMan.GetCachedColumn(TexMan.getSkyTexture(), angle);
                 vpw_skyfunc.invoke();
             }
             }
             continue;
         }
         
         // regular flat
         vpw_dsvars.ds_source = TexMan.getSafeFlat(pln.picnum);
         vpw_planeheight = Math.abs(pln.height-view.z);
         light = (pln.lightlevel >>> LIGHTSEGSHIFT)+lights.extralight;

         if (light >= LIGHTLEVELS)
             light = LIGHTLEVELS-1;

         if (light < 0)
             light = 0;

         vpw_planezlight = lights.zlight[light];

         // Some tinkering required to make sure visplanes
         // don't end prematurely on each other's stop markers
         
         char value=pln.getTop(maxx+1);
         if (!isMarker(value)) { // is it a marker?
                value|=visplane_t.SENTINEL; // Mark it so.
                value&=visplane_t.THREADIDCLEAR; //clear id bits
                value|=(id<<visplane_t.THREADIDSHIFT); // set our own id.
                } // Otherwise, it was set by another thread.
                  // Leave it be.
         
         pln.setTop(maxx+1,value);
         
         value=pln.getTop(minx-1);
         if (!isMarker(value)) { // is it a marker?
                value|=visplane_t.SENTINEL; // Mark it so.
                value&=visplane_t.THREADIDCLEAR; //clear id bits
                value|=(id<<visplane_t.THREADIDSHIFT); // set our own id.
                } // Otherwise, it was set by another thread.
                  // Leave it be.
         
         pln.setTop(minx-1, value);
         
         stop = maxx+1;
         
         for (x=minx ; x<= stop ; x++) {
          MakeSpans(x,pln.getTop(x-1),
             pln.getBottom(x-1),
             pln.getTop(x),
             pln.getBottom(x));
            }
         
         }
         // We're done, wait.

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
        
    private final boolean isMarker(int t1){
        return ((t1&visplane_t.SENTINEL)!=0);
        }
    
    private final int decodeID(int t1){
        return (t1&visplane_t.THREADIDBITS)>>visplane_t.THREADIDSHIFT;
        }
    
    private final int decodeValue(int t1){
        return t1&visplane_t.THREADVALUEBITS;
        }
    
    public void setDetail(int detailshift) {
        if (detailshift == 0){
            vpw_spanfunc = vpw_spanfunchi;
            vpw_skyfunc= vpw_skyfunchi;
        }
        else{
            vpw_spanfunc = vpw_spanfunclow;
            vpw_skyfunc =vpw_skyfunclow;
        }
    }
    
    /**
     * R_MakeSpans
     * 
     * Called only by DrawPlanes.
     * If you wondered where the actual boundaries for the visplane
     * flood-fill are laid out, this is it.
     * 
     * The system of coords seems to be defining a sort of cone.          
     *          
     * 
     * @param x Horizontal position
     * @param t1 Top-left y coord?
     * @param b1 Bottom-left y coord?
     * @param t2 Top-right y coord ?
     * @param b2 Bottom-right y coord ?
     * 
     */

      private final void MakeSpans(int x, int t1, int b1, int t2, int b2) {
      
          // Top 1 sentinel encountered.
          if (isMarker(t1))
              if (decodeID(t1)!=id) // We didn't put it here.
                  t1=decodeValue(t1);

        // Top 2 sentinel encountered.
          if (isMarker(t2))
              if (decodeID(t2)!=id)
                  t2=decodeValue(t2);
              
          // If t1 = [sentinel value] then this part won't be executed.
          while (t1 < t2 && t1 <= b1) {
              this.MapPlane(t1, spanstart[t1], x - 1);
              t1++;
          }
          while (b1 > b2 && b1 >= t1) {
              this.MapPlane(b1, spanstart[b1], x - 1);
              b1--;
          }

          // So...if t1 for some reason is < t2, we increase t2 AND store the current x
          // at spanstart [t2] :-S
          while (t2 < t1 && t2 <= b2) {
              //System.out.println("Increasing t2");
              spanstart[t2] = x;
              t2++;
          }

          // So...if t1 for some reason b2 > b1, we decrease b2 AND store the current x
          // at spanstart [t2] :-S

          while (b2 > b1 && b2 >= t2) {
              //System.out.println("Decreasing b2");
              spanstart[b2] = x;
              b2--;
          }
      }
      
      /**
       * R_MapPlane
       *
       * Called only by R_MakeSpans.
       * 
       * This is where the actual span drawing function is called.
       * 
       * Uses global vars:
       * planeheight
       *  ds_source -> flat data has already been set.
       *  basexscale -> actual drawing angle and position is computed from these
       *  baseyscale
       *  viewx
       *  viewy
       *
       * BASIC PRIMITIVE
       */
      
      private void
      MapPlane
      ( int       y,
        int       x1,
        int       x2 )
      {
          // MAES: angle_t
          int angle;
          // fixed_t
          int distance;
          int length;
          int index;
          
      if (RANGECHECK){
          if (x2 < x1
          || x1<0
          || x2>=view.width
          || y>view.height)
          {
          I.Error ("R_MapPlane: %d, %d at %d",x1,x2,y);
          }
      }

          if (vpw_planeheight != cachedheight[y])
          {
          cachedheight[y] = vpw_planeheight;
          distance = cacheddistance[y] = FixedMul (vpw_planeheight , yslope[y]);
          vpw_dsvars.ds_xstep = cachedxstep[y] = FixedMul (distance,vpw_basexscale);
          vpw_dsvars.ds_ystep = cachedystep[y] = FixedMul (distance,vpw_baseyscale);
          }
          else
          {
          distance = cacheddistance[y];
          vpw_dsvars.ds_xstep = cachedxstep[y];
          vpw_dsvars.ds_ystep = cachedystep[y];
          }
          
          length = FixedMul (distance,distscale[x1]);
          angle = (int)(((view.angle +xtoviewangle[x1])&BITS32)>>>ANGLETOFINESHIFT);
          vpw_dsvars.ds_xfrac = view.x + FixedMul(finecosine[angle], length);
          vpw_dsvars.ds_yfrac = -view.y - FixedMul(finesine[angle], length);

          if (colormap.fixedcolormap!=null)
              vpw_dsvars.ds_colormap = colormap.fixedcolormap;
          else
          {
          index = distance >>> LIGHTZSHIFT;
          
          if (index >= MAXLIGHTZ )
              index = MAXLIGHTZ-1;

          vpw_dsvars.ds_colormap = vpw_planezlight[index];
          }
          
          vpw_dsvars.ds_y = y;
          vpw_dsvars.ds_x1 = x1;
          vpw_dsvars.ds_x2 = x2;

          // high or low detail
          if (view.detailshift==0)
              vpw_spanfunc.invoke();
          else
              vpw_spanfunclow.invoke();         
      }
      
      
      // Private to each thread.
      int[]           spanstart;
      int[]           spanstop;
      CyclicBarrier barrier;
      
  }