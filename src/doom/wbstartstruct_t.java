package doom;

import static data.Defines.MAXPLAYERS;
import utils.C2JUtils;

public class wbstartstruct_t {

        public wbstartstruct_t(){
            plyr=new wbplayerstruct_t[MAXPLAYERS];
            C2JUtils.initArrayOfObjects(plyr, wbplayerstruct_t.class);
        }
    
        public int      epsd;   // episode # (0-2)

        // if true, splash the secret level
        public boolean  didsecret;
        
        // previous and next levels, origin 0
        public int      last;
        public int      next;   
        
        public int      maxkills;
        public int      maxitems;
        public int      maxsecret;
        public int      maxfrags;

        // the par time
        public int      partime;
        
        // index of this player in game
        public int      pnum;   
        /** meant to be treated as a "struct", therefore assignments should be deep copies */
        public wbplayerstruct_t[]   plyr;

    } 
