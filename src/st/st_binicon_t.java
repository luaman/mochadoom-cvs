package st;

import i.system;
import rr.patch_t;
import v.DoomVideoRenderer;
import static st.DoomStatusBarInterface.*;

public class st_binicon_t {
	
	private DoomVideoRenderer V;
	private StatusBar ST;
	// Binary Icon widget

	    // center-justified location of icon
	    int			x;
	    int			y;

	    // last icon value
	    boolean			oldval;

	    // pointer to current icon status
	    boolean[]		val;

	    // pointer to boolean
	    //  stating whether to update icon
	    boolean[]		on;  


	    patch_t		p;	// icon
	    int			data;   // user data
	    
	 // Binary Icon widget routines

	    void
	    STlib_initBinIcon
	    ( 
	      int			x,
	      int			y,
	      patch_t		i,
	      boolean[]		val,
	      boolean[]		on )
	    {
	        this.x	= x;
	        this.y	= y;
	        this.oldval	= false;
	        this.val	= val;
	        this.on	= on;
	        this.p	= i;
	    }



	    void
	    updateBinIcon
	    ( 
	      boolean		refresh )
	    {
	    	st_binicon_t		bi=this;
	    	int			x;
	        int			y;
	        int			w;
	        int			h;

	        if (bi.on[0]
	    	&& (bi.oldval != ((bi.val[0] || refresh))))
	        {
	    	x = bi.x - bi.p.leftoffset;
	    	y = bi.y - bi.p.topoffset;
	    	w = bi.p.width;
	    	h = bi.p.height;

	    	if (y - ST_Y < 0)
	    	    system.Error("updateBinIcon: y - ST_Y < 0");

	    	if (bi.val[0])
	    	    V.DrawPatch(bi.x, bi.y, FG, bi.p);
	    	else
	    	    V.CopyRect(x, y-ST_Y, BG, w, h, x, y, FG);

	    	bi.oldval = bi.val[0];
	        }

	    }

	}
