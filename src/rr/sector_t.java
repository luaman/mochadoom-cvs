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
        soundorg =new degenmobj_t();
        blockbox=new int[4];
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
     public int[]  blockbox;

     /** origin for any sounds played by the sector */
     public degenmobj_t soundorg;

     /** if == validcount, already checked */
     public int     validcount;

     /** list of mobjs in sector (MAES: it's used as a linked list) */
     public mobj_t thinglist;

     /** thinker_t for reversable actions. This actually was a void*, 
      *  and in practice it could store doors, plats, floors and ceiling objects. */
     public SectorAction specialdata;

     public int         linecount;
     //struct line_s** lines;  // [linecount] size
     // MAES: make this line_t[] for now?
     public line_t[] lines;
     
     /** Use for internal identification */
     public int id;
     
 }
