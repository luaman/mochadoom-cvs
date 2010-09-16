package p;

import rr.SectorAction;
import rr.sector_t;
import doom.thinker_t;

public class glow_t extends SectorAction{
    
        public glow_t(){
            this.thinker=new thinker_t();
        }
    
        public sector_t   sector;
        public int     minlight;
        public int     maxlight;
        public int     direction;

    }