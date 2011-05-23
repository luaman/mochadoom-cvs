package f;

import doom.DoomMain;
import v.DoomVideoRenderer;
import v.IVideoScale;
import v.IVideoScaleAware;
import m.random;

public class Wiper implements IVideoScaleAware {

    static final String rcsid = "$Id: Wiper.java,v 1.9 2011/05/23 16:56:56 velktron Exp $";
    
    random RND;
    DoomVideoRenderer V;
    
    /** These don't seem to be used anywhere */
    
    public static enum wipe
    {
        // simple gradual pixel change for 8-bit only
    	// MAES: this transition isn't guaranteed to always terminate
    	// see Chocolate Strife develpment. Unused in Doom anyway.
        ColorXForm,
        
        // weird screen melt
        Melt,  

        NUMWIPES
    };
    
    protected wipefun[] wipes;
    
    public Wiper(DoomMain DC){
        this.RND=DC.RND;
        this.V=DC.V;
        wipes=new wipefun[]{
        new wipe_initColorXForm(), new wipe_doColorXForm(), new wipe_exitColorXForm(),
        new wipe_initMelt(), new wipe_doMelt(), new wipe_exitMelt()
        };
    }
    //
//                           SCREEN WIPE PACKAGE
    //

    /** when false, stop the wipe */
    protected volatile boolean  go = false;

    protected byte[]    wipe_scr_start;
    protected byte[]    wipe_scr_end;
    protected byte[]    wipe_scr;


    /** They sure have an obsession with shit...this is supposed to do some
     * lame-ass transpose.
     * 
     * @param array
     * @param width
     * @param height
     */
    
    public void
    shittyColMajorXform
    ( short[]    array,
      int       width,
      int       height )
    {
        int     x;
        int     y;
        short[]  dest;

        dest = new short[width*height];

        for(y=0;y<height;y++)
        for(x=0;x<width;x++)
            dest[y*width+x]=array[x*height+y];

        System.arraycopy(dest, 0, array, 0, width*height);

        //Z_Free(dest);

    }
    
    /** Those guys sure have an obsession with shit...this is supposed to do some
     * lame-ass transpose.
     * 
     * @param array
     * @param width
     * @param height
     */
    
    public void
    shittyColMajorXform
    ( byte[]    array,
      int       width,
      int       height )
    {
        int     x;
        int     y;
        byte[]  dest;

        dest = new byte[width*height];

        for(y=0;y<height;y++)
        for(x=0;x<width;x++){
            dest[x*height+y] = array[y*width+x];
            //dest[(1+x)*height+y] = array[y*width+(1+x)];
        }
        System.arraycopy(dest, 0, array, 0, width*height);

        //Z_Free(dest);

    }
    
    class wipe_initColorXForm implements wipefun{
        public boolean
        invoke
    ( int   width,
      int   height,
      int   ticks )
    {
        System.arraycopy(wipe_scr_start,0 ,wipe_scr, 0,width*height);
        return false;
    }
    
    }
    class wipe_doColorXForm implements wipefun{
    
    public boolean
    invoke
    ( int   width,
      int   height,
      int   ticks )
    {
        boolean changed;
        byte[]   w=wipe_scr;
        byte[]   e=wipe_scr_end;
        int     newval;

        changed = false;
        int pw =0;// wipe_scr;
        int pe = 0; //wipe_scr_end;
        
        while (pw!=width*height)
        {
        if (w[pw] != e[pe])
        {
            if (w[pw] > e[pe])
            {
            newval = w[pw] - ticks;
            if (newval < e[pe])
                w[pw] = e[pe];
            else
                w[pw] = (byte) newval;
            changed = true;
            }
            else if (w[pw] < e[pe])
            {
            newval = w[pw] + ticks;
            if (newval > e[pe])
                w[pw] = e[pe];
            else
                w[pw] = (byte) newval;
            changed = true;
            }
        }
        pw++;
        pe++;
        }

        return !changed;

    }
    }
    
    class wipe_exitColorXForm implements wipefun{
    public boolean
    invoke
    ( int   width,
      int   height,
      int   ticks )
    {
        return false;
    }
    }


    protected int[] y;

    class wipe_initMelt implements wipefun{
        public boolean
        invoke
    ( int   width,
      int   height,
      int   ticks )
    {
        int i, r;
        
        System.out.println("Called!");
        // copy start screen to main screen
        System.arraycopy(wipe_scr_start, 0,wipe_scr, 0,width*height);
        
        // makes this wipe faster (in theory)
        // to have stuff in column-major format
        shittyColMajorXform(wipe_scr_start, width, height);
        shittyColMajorXform(wipe_scr_end, width, height);
        
        // setup initial column positions
        // (y<0 => not ready to scroll yet)
        y = new int[width];
        y[0] = -(RND.M_Random()%16);
        for (i=1;i<width;i++)
        {
        r = (RND.M_Random()%3) - 1;
        y[i] = y[i-1] + r;
        if (y[i] > 0) y[i] = 0;
        else if (y[i] == -16) y[i] = -15;
        }

        return false;
    }
    }

    class wipe_doMelt implements wipefun{
        public boolean
        invoke
    ( int   width,
      int   height,
      int   ticks )
    {
            
        //   int w2=2*width;
        //    int w3=3*width;
        //    int w4=4*width;

        int     dy;
        int     idx;
        
        // Indexex to short* ?! WTF... 
        int  ps;
        int  pd;
        
        byte[] s;//=wipe_scr_end;
        byte[] d=wipe_scr;
        
        boolean done = true;

        //width=2;

        while (ticks-->0)
        {
        for (int i=0;i<width;i++)
        {
            // Column won't start yet.
            if (y[i]<0)
            {
            y[i]++; done = false;
            }
            else if (y[i] < height)
            {
                
                
            dy = (y[i] < 16) ? y[i]+1 : 8;
            if (y[i]+dy >= height) dy = height - y[i];
            ps = i*height+y[i];// &((short *)wipe_scr_end)[i*height+y[i]];
            pd = y[i]*width+i;//&((short *)wipe_scr)[y[i]*width+i];
            idx = 0;

            s=wipe_scr_end;
            

            
            // MAES: this part should draw the END SCREEN "behind" the melt.
            for (int j=dy;j>0;j--)
            {
    		    d[pd+idx] = s[ps++];
    		    idx += width;
            }
            y[i] += dy;
            s=wipe_scr_start;
            ps = i*height; //&((short *)wipe_scr_start)[i*height];
            pd = y[i]*width+i; //&((short *)wipe_scr)[y[i]*width+i];
            idx = 0;

            for (int j=height-y[i];j>0;j--)
            {
                d[pd+idx] = s[ps++];
                idx+=width;
            }
            done = false;
            }
        }
        }

        return done;

    }
    }
    
    class wipe_exitMelt implements wipefun{
        public boolean
        invoke
    ( int   width,
      int   height,
      int   ticks )

    {
        y=null; //Z_Free(y);
        return false;
    }
    }

    /** Sets "from" screen and stores it in "screen 2"*/
    
    public boolean
    StartScreen
    ( int   x,
      int   y,
      int   width,
      int   height )
    {
        wipe_scr_start = V.getScreen(2);
        //
        
        byte[] screen_zero=V.getScreen(0);
        //I_ReadScreen(wipe_scr_start);
        
        System.arraycopy(screen_zero,0,wipe_scr_start, 0, SCREENWIDTH*SCREENHEIGHT);
        return false;
    }

    /** Sets "to" screen and stores it to "screen 3" */

    public boolean
    EndScreen
    ( int   x,
      int   y,
      int   width,
      int   height )
    {
        wipe_scr_end = V.getScreen(3);
        //I_ReadScreen(wipe_scr_end);
        byte[] screen_zero=V.getScreen(0);
        
        System.arraycopy(screen_zero,0,wipe_scr_end, 0, SCREENWIDTH*SCREENHEIGHT);
        System.arraycopy(wipe_scr_start,0,screen_zero, 0, SCREENWIDTH*SCREENHEIGHT);
        //V.DrawBlock(x, y, 0, width, height, wipe_scr_start); // restore start scr.
        return false;
    }


    
    public boolean
    ScreenWipe
    ( int   wipeno,
      int   x,
      int   y,
      int   width,
      int   height,
      int   ticks )
    {
        boolean rc;
  
        //System.out.println("Ticks do "+ticks);

        // initial stuff
        if (!go)
        {
        go = true;
        //wipe_scr = new byte[width*height]; // DEBUG
        wipe_scr = V.getScreen(0);
        // HOW'S THAT FOR A FUNCTION POINTER, BIATCH?!
        (wipes[wipeno*3]).invoke(width, height, ticks);
        }

        // do a piece of wipe-in
        V.MarkRect(0, 0, width, height);
        rc = (wipes[wipeno*3+1]).invoke(width, height, ticks);
        // V.DrawBlock(x, y, 0, width, height, wipe_scr); // DEBUG

        // final stuff
        if (rc)
        {
        go = false;
        (wipes[wipeno*3+2]).invoke(width, height, ticks);
        }

        return !go;

    }

    
    /** Interface for ASS-WIPING functions */
    
    interface wipefun{
        public boolean invoke(int   width,
                int   height,
                int   ticks );
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

        // Pre-scale stuff.
    }

    
}
