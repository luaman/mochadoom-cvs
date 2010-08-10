package st;

import rr.patch_t;
import v.DoomVideoRenderer;
import static st.DoomStatusBarInterface.*;

public class st_percent_t {

	private DoomVideoRenderer V;
	private StatusBar ST;
	
	// Percent widget ("child" of number widget,
//  or, more precisely, contains a number widget.)
    // number information
    st_number_t		n;

    // percent sign graphic
    patch_t		p;
    
    //
    void
    initPercent
    ( 
      int			x,
      int			y,
      patch_t[]		pl,
      int[]			num,
      boolean[]		on,
      patch_t		percent )
    {
        n.initNum(x, y, pl, num, on, 3);
        p = percent;
    }




    void
    updatePercent
    ( 
      boolean			refresh )
    {
        if (refresh && this.n.on[0])
    	V.DrawPatch(n.x, n.y, FG, p);
        
        n.updateNum(refresh);
    }

} 

