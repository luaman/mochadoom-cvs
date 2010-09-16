package p;

import rr.sector_t;
import doom.thinker_t;

//
// P_LIGHTS
//

public class fireflicker_t {
	
	public fireflicker_t(){
		this.thinker=new thinker_t();
	}
		
     public thinker_t   thinker;
     public sector_t   sector;
     public int     count;
     public int     maxlight;
     public int     minlight;
     
 } 
