package m;

public class menuitem_t {

	    public menuitem_t(short status, String name, MenuRoutine routine, char alphaKey) {
	    	this.status=status;
	    	this.name=name;
	    	this.routine= routine;
	    	this.alphaKey=alphaKey;
		// TODO Auto-generated constructor stub
	}	

		// 0 = no cursor here, 1 = ok, 2 = arrows ok
	    short	status;
	    
	    String	name=new String[10];
	    
	    // choice = menu item #.
	    // if status = 2,
	    //   choice=0:leftarrow,1:rightarrow
	    // MAES: OK... to probably we need some sort of "MenuRoutine" class for this one.
	    // void	(*routine)(int choice);
	    MenuRoutine routine;
	    
	    // hotkey in menu
	    char	alphaKey;			
	} 