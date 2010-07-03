package st;

import java.io.IOException;

import i.system;
import rr.patch_t;
import v.DoomVideoRenderer;
import w.WadLoader;
import static st.DoomStatusBar.*;
import static data.Defines.PU_STATIC;
// Number widget

public class st_number_t {

	private DoomVideoRenderer V;
	private StatusBar ST;
	
	//
	// Hack display negative frags.
	//  Loads and store the stminus lump.
	// MAES: this is only ever used here (AFAICT), so no need to have a "STLib"
	// just for that.
	
	private static patch_t		sttminus;

	private static void STlib_init(WadLoader W) throws IOException
	{
		sttminus=new patch_t();
		
	    sttminus.unpack(W.CacheLumpName("STTMINUS", PU_STATIC));
	}
	


	    // upper right-hand corner
	    //  of the number (right-justified)
	    int		x;
	    int		y;

	    // max # of digits in number
	    int width;    

	    // last number value
	    int		oldnum;
	    
	    // pointer to current value
	    // int*	num;
	    
	    int[] num;
	    // pointer to boolean stating
	    //  whether to update number
	    boolean[]	on;

	    // list of patches for 0-9
	    patch_t[]	p;

	    // user data
	    int data;
	    
	 // Number widget routines
	    public void
	    STlib_initNum
	    ( st_number_t		n,
	      int			x,
	      int			y,
	      patch_t[]		pl,
	      int[]			num,
	      boolean[]		on,
	      int			width ) {
	}

	    public void
	    STlib_updateNum
	    ( st_number_t		n,
	      boolean		refresh ) {
		}
	    
	    
	 // ?
	    void
	    initNum
	    ( int			x,
	      int			y,
	      patch_t[]		pl,
	      int[]			num,
	      boolean[]		on,
	      int			width )
	    {
	        this.x	= x;
	        this.y	= y;
	        this.oldnum	= 0;
	        this.width	= width;
	        this.num	= num;
	        this.on	= on;
	        this.p	= pl;
	    }


	    // 
	    // A fairly efficient way to draw a number
	    //  based on differences from the old number.
	    // Note: worth the trouble?
	    //
	    void
	    drawNum
	    ( 
	      boolean	refresh )
	    {

	    	st_number_t n =this;
	        int		numdigits = this.width;
	        int		num = this.num[0];
	        
	        int		w = this.p[0].width;
	        int		h = this.p[0].height;
	        int		x = this.x;
	        
	        boolean		neg;

	        this.oldnum=this.num[0];

	        neg = num < 0;

	        if (neg)
	        {
	    	if (numdigits == 2 && num < -9)
	    	    num = -9;
	    	else if (numdigits == 3 && num < -99)
	    	    num = -99;
	    	
	    	num = -num;
	        }

	        // clear the area
	        x = this.x - numdigits*w;

	        if (this.y - ST_Y < 0) {
	    	system.Error("drawNum: n->y - ST_Y < 0");
	        }

	        V.CopyRect(x, this.y - ST_Y, BG, w*numdigits, h, x, n.y, FG);

	        // if non-number, do not draw it
	        if (num == 1994)
	    	return;

	        x = n.x;

	        // in the special case of 0, you draw 0
	        if (num==0)
	    	V.DrawPatch(x - w, n.y, FG, n.p[ 0 ]);

	        // draw the new number
	        while (((num!=0) && (numdigits--!=0)))
	        {
	    	x -= w;
	    	V.DrawPatch(x, n.y, FG, n.p[ num % 10 ]);
	    	num /= 10;
	        }

	        // draw a minus sign if necessary
	        if (neg)
	    	V.DrawPatch(x - 8, n.y, FG, sttminus);
	    }


	    //
	    public void
	    updateNum
	    ( boolean		refresh )
	    {
	        if (this.on[0]) drawNum(refresh);
	    }
	    
	} 
