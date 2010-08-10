package st;

import i.system;
import rr.patch_t;
import v.DoomVideoRenderer;
import static st.DoomStatusBarInterface.*;

public class st_multicon_t {
	


	private DoomVideoRenderer V;
	private StatusBar ST;
	
	     // center-justified location of icons
	    int			x;
	    int			y;

	    // last icon number
	    int			oldinum;

	    // pointer to current icon
	    int[]		inum;

	    // pointer to boolean stating
	    //  whether to update icon
	    boolean[]		on;

	    // list of icons
	    patch_t[]		p;
	    
	    // user data
	    int			data;
	    
	    void
	    initMultIcon
	    ( int			x,
	      int			y,
	      patch_t[]		il,
	      int[]			inum,
	      boolean[]		on )
	    {
	        this.x	= x;
	        this.y	= y;
	        this.oldinum 	= -1;
	        this.inum	= inum;
	        this.on	= on;
	        this.p	= il;
	    }



	    void
	    STlib_updateMultIcon
	    ( 
	      boolean		refresh )
	    {
	    	st_multicon_t	mi=this;
	    	int			w;
	        int			h;
	        int			x;
	        int			y;

	        if (mi.on[0]
	    	&& ((mi.oldinum!=0) != ((mi.inum[0]!=0) || refresh))
	    	&& (mi.inum[0]!=-1))
	        {
	    	if (mi.oldinum != -1)
	    	{
	    	    x = mi.x - mi.p[mi.oldinum].leftoffset;
	    	    y = mi.y - mi.p[mi.oldinum].topoffset;
	    	    w = mi.p[mi.oldinum].width;
	    	    h = mi.p[mi.oldinum].height;

	    	    if (y - ST_Y < 0)
	    		system.Error("updateMultIcon: y - ST_Y < 0");

	    	    V.CopyRect(x, y-ST_Y, BG, w, h, x, y, FG);
	    	}
	    	V.DrawPatch(mi.x, mi.y, FG, mi.p[mi.inum[0]]);
	    	mi.oldinum = mi.inum[0];
	        }
	    }
	    
	    
	} 
