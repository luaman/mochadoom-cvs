package p;

import rr.sector_t;
import m.fixed_t;
import doom.thinker_t;

public class vldoor_t {
        thinker_t   thinker;
        vldoor_e    type;
        sector_t   sector;
        fixed_t topheight;
        fixed_t speed;

        // 1 = up, 0 = waiting at top, -1 = down
        int             direction;
        
        // tics to wait at the top
        int             topwait;
        // (keep in case a door going down is reset)
        // when it reaches 0, start going down
        int             topcountdown;
        
    } ;