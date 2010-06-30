package rr;

import data.Defines.slopetype_t;
import doom.thinker_t;
import m.bbox;
import m.fixed_t;

public class line_t {

       // Vertices, from v1 to v2.
       // MAES pointers
        vertex_t   v1;
        vertex_t   v2;

        // Precalculated v2 - v1 for side checking.
        fixed_t dx;
        fixed_t dy;

        // Animation related.
        short   flags;
        short   special;
        short   tag;

        // Visual appearance: SideDefs.
        //  sidenum[1] will be -1 if one sided
        short[]   sidenum=new short[2];         

        // Neat. Another bounding box, for the extent
        //  of the LineDef.
        // MAES: make this a proper bbox?
        //fixed_t bbox[4];
        bbox bbox;

        // To aid move clipping.
        slopetype_t slopetype;

        // Front and back sector.
        // Note: redundant? Can be retrieved from SideDefs.
        // MAES: pointers
        sector_t   frontsector;
        sector_t   backsector;

        // if == validcount, already checked
        int     validcount;

        // thinker_t for reversable actions MAES: (void*)
        thinker_t   specialdata;        
    }
