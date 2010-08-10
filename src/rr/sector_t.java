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

    public sector_t(){
        
    }
    
        /** (fixed_t) */
     public int floorheight, ceilingheight;
     public short   floorpic;
     public short   ceilingpic;
     public short   lightlevel;
     public short   special;
     public short   tag;

     /** 0 = untraversed, 1,2 = sndlines -1 */
     public int     soundtraversed;

     /** thing that made a sound (or null) (MAES: single pointer) */
     public mobj_t soundtarget;

     /** mapblock bounding box for height changes */
     public int[]  blockbox=new int[4];

     /** origin for any sounds played by the sector */
     public degenmobj_t soundorg;

     /** if == validcount, already checked */
     public int     validcount;

     /** list of mobjs in sector (MAES: made * into array) */
     public mobj_t[] thinglist;

     /** thinker_t for reversable actions */
     public thinker_t specialdata;

     public int         linecount;
     //struct line_s** lines;  // [linecount] size
     // MAES: make this line_t[] for now?
     public line_t[] lines;
     
 }
