package p;

import rr.sector_t;
import doom.thinker_t;

public class floormove_t {

    public floormove_t(){
        
    }
    
    public thinker_t   thinker;
    public floor_e type;
    public boolean crush;
    // MAES: was *
    public sector_t   sector;
    public int     direction;
    public int     newspecial;
    public short   texture;
    /** fixed_t */
    public int floordestheight;
    /** fixed_t */
    public int speed;

}