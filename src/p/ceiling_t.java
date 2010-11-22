package p;

import rr.SectorAction;
import rr.sector_t;
import doom.thinker_t;
import m.fixed_t;

public class ceiling_t extends SectorAction{

        public ceiling_e   type;
        /** fixed_t */
        int bottomheight;
        /** fixed_t */
        int topheight;
        /** fixed_t */
        int speed;
        boolean crush;

        // 1 = up, 0 = waiting, -1 = down
        public int     direction;

        // ID
        public int     tag;                   
        public int     olddirection;
        
    }

