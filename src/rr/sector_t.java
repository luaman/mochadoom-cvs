package rr;

import doom.thinker_t;
import p.mobj_t;
import m.fixed_t;

/**
 * The SECTORS record, at runtime.
 * Stores things/mobjs. 
 * @author admin
 *
 */
public class sector_t {

 

     fixed_t floorheight;
     fixed_t ceilingheight;
     short   floorpic;
     short   ceilingpic;
     short   lightlevel;
     short   special;
     short   tag;

     // 0 = untraversed, 1,2 = sndlines -1
     int     soundtraversed;

     // thing that made a sound (or null) (MAES: single pointer)
     mobj_t soundtarget;

     // mapblock bounding box for height changes
     int[]  blockbox=new int[4];

     // origin for any sounds played by the sector
     degenmobj_t soundorg;

     // if == validcount, already checked
     int     validcount;

     // list of mobjs in sector (MAES: made * into array)
     mobj_t[] thinglist;

     // thinker_t for reversable actions
     thinker_t specialdata;

     int         linecount;
     //struct line_s** lines;  // [linecount] size
     // MAES: make this line_t[] for now?
     line_t[] lines;
     
 }
