package m;

public class menu_t {
	    public menu_t(int numitems, menu_t prev, menuitem_t[] menuitems,
            DrawRoutine drawroutine, int x, int y, int lastOn) {
	        this.numitems=numitems;
	        this.prevMenu=prev;
	        this.menuitems=menuitems;
	        this.routine=drawroutine;
	        this.x=x;
	        this.y=y;
	        this.lastOn=lastOn;
	        
    }
	    /** # of menu items */	    
        int		numitems;
        
	    /**  previous menu */
	    menu_t	prevMenu;

	    /** menu items */
	    menuitem_t[]		menuitems;	
	    /** draw routine */
	    DrawRoutine routine;
	    /**  x,y of menu */
	    int		x,y;
	    /** last item user was on in menu */
	    int		lastOn;
	} 
