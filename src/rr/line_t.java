package rr;

import p.Interceptable;
import data.Defines.slopetype_t;
import doom.thinker_t;

/** This is the actual linedef */

public class line_t implements Interceptable{

        public line_t(){
            sidenum=new short[2];
            bbox=new int[4];
            slopetype=slopetype_t.ST_HORIZONTAL;
        }
    
        /** Vertices, from v1 to v2. NOTE: these are almost never passed as-such, nor linked to 
         * Maybe we can get rid of them and only use the value semantics? */
        public vertex_t   v1, v2;
        
        /** remapped vertex coords, for quick lookup with value semantics */        
        public int v1x,v1y,v2x,v2y;

        /** (fixed_t) Precalculated v2 - v1 for side checking. */
        public int dx, dy;

        /** Animation related. */
        public short   flags, special, tag;

        /** Visual appearance: SideDefs.
           sidenum[1] will be -1 if one sided */
        public short[]   sidenum;         

        /** Neat. Another bounding box, for the extent
         of the LineDef.
         MAES: make this a proper bbox?
         fixed_t bbox[4]; */
        public int[] bbox;

        /** To aid move clipping. */
        public slopetype_t slopetype;

        /** Front and back sector.
           Note: redundant? Can be retrieved from SideDefs.
           MAES: pointers */
        public sector_t   frontsector,  backsector;

        /** if == validcount, already checked */
        public int     validcount;

        /** thinker_t for reversable actions MAES: (void*) */
        public thinker_t   specialdata;
        
        public void assignVertexValues(){
            this.v1x=v1.x;
            this.v1y=v1.y;
            this.v2x=v2.x;
            this.v2y=v2.y;
            
        }
    }
