package rr;

import p.Interceptable;
import p.divline_t;
import static data.Defines.ML_TWOSIDED;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static m.fixed_t.*;
import static utils.C2JUtils.flags;
import defines.slopetype_t;
import doom.thinker_t;

/** This is the actual linedef */

public class line_t implements Interceptable{

        public line_t(){
            sidenum=new int[2];
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
        public int[]   sidenum;         

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
/**
 *    P_PointOnLineSide   
 *           
 * @param x fixed_t
 * @param y fixed_t
 * @return 0 or 1 (false, true) - (front, back)
 */
      public boolean
      PointOnLineSide
      ( int   x,
      int   y)

      {
       int dx,dy,left,right;
       if (this.dx==0)
       {
       if (x <= this.v1.x)
           return this.dy > 0;
       
       return this.dy < 0;
       }
       if (this.dy==0)
       {
       if (y <= this.v1.y)
           return this.dx < 0;
       
       return this.dx > 0;
       }
       
       dx = (x - this.v1.x);
       dy = (y - this.v1.y);
       
       left = FixedMul ( this.dy>>FRACBITS , dx );
       right = FixedMul ( dy , this.dx>>FRACBITS );
       
       if (right < left)
       return false;       // front side
       return true;           // back side
      }
       
      /**
       * P_BoxOnLineSide
       * Considers the line to be infinite
       *Returns side 0 or 1, -1 if box crosses the line.
       *
       *@param tmbox fixed_t[]
       *@param line_t
       */
      public int
      BoxOnLineSide
      ( int[]  tmbox)
      {
       boolean     p1=false;
       boolean     p2=false;
       
       switch (this.slopetype)
       {
         case ST_HORIZONTAL:
       p1 = tmbox[BOXTOP] > v1.y;
       p2 = tmbox[BOXBOTTOM] > v1.y;
       if (dx < 0)
       {
           p1 ^= true;
           p2 ^= true;
       }
       break;
       
         case ST_VERTICAL:
       p1 = tmbox[BOXRIGHT] < v1.x;
       p2 = tmbox[BOXLEFT] < v1.x;
       if (dy < 0)
       {
           p1 ^= true;
           p2 ^= true;
       }
       break;
       
         case ST_POSITIVE:
       p1 = PointOnLineSide (tmbox[BOXLEFT], tmbox[BOXTOP]);
       p2 = PointOnLineSide (tmbox[BOXRIGHT], tmbox[BOXBOTTOM]);
       break;
       
         case ST_NEGATIVE:
       p1 = PointOnLineSide (tmbox[BOXRIGHT], tmbox[BOXTOP]);
       p2 = PointOnLineSide (tmbox[BOXLEFT], tmbox[BOXBOTTOM]);
       break;
       }

       if (p1 == p2)
       return p1?1:0;
       return -1;
      }

      /**
       * getNextSector()
       * Return sector_t * of sector next to current.
       * NULL if not two-sided line
       */
      
      public sector_t getNextSector(sector_t sec) {
          if (!flags(flags, ML_TWOSIDED))
              return null;

          if (frontsector == sec)
              return backsector;

          return frontsector;
      }
      
      public String toString(){
          return (String.format("Flags: %d Tag: %d Special %d",this.flags, this.tag, this.special));   
      }
      
      
    }
