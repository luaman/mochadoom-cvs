package p;

import rr.SectorAction;

public class ceiling_t extends SectorAction{

        public ceiling_e   type;
        /** fixed_t */
        public int bottomheight;
        /** fixed_t */
        public int topheight;
        /** fixed_t */
        public int speed;
        boolean crush;

        // 1 = up, 0 = waiting, -1 = down
        public int     direction;

        // ID
        public int     tag;                   
        public int     olddirection;
        
    }

