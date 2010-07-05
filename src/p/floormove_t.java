package p;

import m.fixed_t;
import rr.sector_t;
import doom.thinker_t;

public class floormove_t {

    thinker_t   thinker;
    floor_e type;
    boolean crush;
    // MAES: was *
    sector_t[]   sector;
    int     direction;
    int     newspecial;
    short   texture;
    fixed_t floordestheight;
    fixed_t speed;

}