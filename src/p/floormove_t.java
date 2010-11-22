package p;

import rr.SectorAction;
import rr.sector_t;
import doom.thinker_t;

public class floormove_t extends SectorAction{

    public floor_e type;
    public boolean crush;

    public int     direction;
    public int     newspecial;
    public short   texture;
    /** fixed_t */
    public int floordestheight;
    /** fixed_t */
    public int speed;

}