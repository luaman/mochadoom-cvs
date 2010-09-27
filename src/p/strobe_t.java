package p;

import rr.sector_t;
import doom.thinker_t;

public class strobe_t {
	
		public strobe_t(){
			thinker=new thinker_t();
		}
		
        public thinker_t   thinker;
        public sector_t   sector;
        public int     count;
        public int     minlight;
        public int     maxlight;
        public int     darktime;
        public int     brighttime;
        
    };