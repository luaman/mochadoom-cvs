package m;

public class menu_t {
	    short		numitems;	// # of menu items
	    // Maes: was menu_s*... just make it menu_t already.
	    menu_t	prevMenu;	// previous menu
	    menuitem_t[]		menuitems;	// menu items
	    // MAES: probably we need some kind of "DrawRoutine" interface here.
	    //void		(*routine)();	// draw routine
	    DrawRoutine routine;
	    short		x;
	    short		y;		// x,y of menu
	    short		lastOn;		// last item user was on in menu
	} 
