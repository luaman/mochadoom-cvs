package rr;

import p.Interceptable;
import data.Defines.slopetype_t;
import doom.thinker_t;
import m.BBox;
import m.fixed_t;

public class line_t implements Interceptable{

       // Vertices, from v1 to v2.
       // MAES pointers
        public vertex_t   v1, v2;

        /** (fixed_t) Precalculated v2 - v1 for side checking. */
        public int dx, dy;

        /** Animation related. */
        public short   flags, special, tag;

        // Visual appearance: SideDefs.
        //  sidenum[1] will be -1 if one sided
        public short[]   sidenum=new short[2];         

        /** Neat. Another bounding box, for the extent
         of the LineDef.
         MAES: make this a proper bbox?
         fixed_t bbox[4]; */
        public BBox BBox;

        // To aid move clipping.
        public slopetype_t slopetype;

        /** Front and back sector.
           Note: redundant? Can be retrieved from SideDefs.
           MAES: pointers */
        public sector_t   frontsector,  backsector;

        // if == validcount, already checked
        int     validcount;

        // thinker_t for reversable actions MAES: (void*)
        public thinker_t   specialdata;        
    }
