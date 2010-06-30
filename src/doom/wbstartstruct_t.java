package doom;

import static data.Defines.MAXPLAYERS;

public class wbstartstruct_t {

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

        public wbstartstruct_t[]   plyr=new wbstartstruct_t[MAXPLAYERS];

    } 
