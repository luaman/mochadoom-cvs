package m;

public class menu_t {
	    public menu_t(int numitems, menu_t prev, menuitem_t[] readMenu1,
            DrawRoutine drawroutine, int x, int y, int lastOn) {
	        this.numitems=numitems;
	        this.prevMenu=prev;
	        this.menuitems=readMenu1;
	        this.routine=drawroutine;
	        this.x=x;
	        this.y=y;
	        this.lastOn=lastOn;
	        
    }
	    /** # of menu items */	    
        public int		numitems;
        
	    /**  previous menu */
	    public menu_t	prevMenu;

	    /** menu items */
	    public menuitem_t[]		menuitems;	
	    /** draw routine */
	    public DrawRoutine routine;
	    /**  x,y of menu */
	    public int		x,y;
	    /** last item user was on in menu */
	    public int		lastOn;
	} 
