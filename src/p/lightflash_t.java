package p;

import rr.SectorAction;
import rr.sector_t;
import doom.thinker_t;

//
// P_LIGHTS
//

public class lightflash_t extends SectorAction {

	public lightflash_t(){
		super();
	}
	
    int     count;
    int     maxlight;
    int     minlight;
    int     maxtime;
    int     mintime;
    
} ;