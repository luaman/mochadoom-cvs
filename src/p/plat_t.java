package p;

import rr.SectorAction;
import rr.sector_t;

public class plat_t extends SectorAction{
        
        public sector_t   sector;
        /** fixed_t */
        public int speed,low,high;
        int     wait;
        int     count;
        public plat_e  status;
        public plat_e  oldstatus;
        boolean crush;
        public int     tag;
        public plattype_e  type;
        
    } 
