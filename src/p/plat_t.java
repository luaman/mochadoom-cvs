package p;

import m.fixed_t;
import rr.sector_t;
import doom.thinker_t;

public class plat_t {
        thinker_t   thinker;
        sector_t   sector;
        fixed_t speed;
        fixed_t low;
        fixed_t high;
        int     wait;
        int     count;
        plat_e  status;
        plat_e  oldstatus;
        boolean crush;
        int     tag;
        plattype_e  type;
        
    } 
