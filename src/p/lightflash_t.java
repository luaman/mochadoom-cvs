package p;

import rr.sector_t;
import doom.thinker_t;

//
// P_LIGHTS
//

public class lightflash_t {

	public lightflash_t(){
		thinker=new thinker_t();
	}
	
    public thinker_t   thinker;
    public sector_t   sector;
    int     count;
    int     maxlight;
    int     minlight;
    int     maxtime;
    int     mintime;
    
} ;