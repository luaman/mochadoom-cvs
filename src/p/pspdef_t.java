package p;

import data.state_t;

public class pspdef_t {
	
	public pspdef_t(){
		state=new state_t();
	}
	
	/** a NULL state means not active */
	    public state_t	state;	
	    public int		tics;
	    /** fixed_t */
	    public int	sx, sy;

	}
