package p;

import rr.sector_t;
import doom.thinker_t;
import m.fixed_t;

public class ceiling_t {

        thinker_t   thinker;
        ceiling_e   type;
        // MAES: was *
        sector_t[]   sector;
        fixed_t bottomheight;
        fixed_t topheight;
        fixed_t speed;
        boolean crush;

        // 1 = up, 0 = waiting, -1 = down
        int     direction;

        // ID
        int     tag;                   
        int     olddirection;
        
    }

