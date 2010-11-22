package p;

import rr.SectorAction;

public class vldoor_t extends SectorAction{
        
        public vldoor_e    type;
        // fixed_t
        public int topheight;
        public int speed;

        // 1 = up, 0 = waiting at top, -1 = down
        public int             direction;
        
        // tics to wait at the top
        public int             topwait;
        // (keep in case a door going down is reset)
        // when it reaches 0, start going down
        public int             topcountdown;
        
    }