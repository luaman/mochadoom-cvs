package p;

import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import rr.line_t;
import m.fixed_t;

//
// P_MAPUTL
//

public class divline_t {

    /** fixed_t */
     public int x, y, dx, dy;
     

     /**
      *P_PointOnDivlineSide
      *Returns 0 or 1. (false or true)
      *@param x fixed
      *@param y fixed
      *@param divline_t
      */
     public boolean
     PointOnDivlineSide
     ( int   x,
     int   y
     )
     {
      int dx;
      int dy;
      int left;
      int right;
      
      if (this.dx==0)
      {
      if (x <= this.x)
          return this.dy > 0;
      
      return this.dy < 0;
      }
      if (this.dy==0)
      {
      if (y <= this.y)
          return this.dx < 0;

      return this.dx > 0;
      }
      
      dx = (x - this.x);
      dy = (y - this.y);
      
      // try to quickly decide by looking at sign bits
      if ( ((this.dy ^ this.dx ^ dx ^ dy)&0x80000000) !=0)
      {
      if (((this.dy ^ dx) & 0x80000000) !=0)
          return true;       // (left is negative)
      return false;
      }
      
      left = FixedMul ( this.dy>>8, dx>>8 );
      right = FixedMul ( dy>>8 , this.dx>>8 );
      
      if (right < left)
      return false;       // front side
      return true;           // back side
     }



     //
     //P_MakeDivline
     //
     public void
     MakeDivline
     ( line_t   li)
     {
      this.x = li.v1.x;
      this.y = li.v1.y;
      this.dx = li.dx;
      this.dy = li.dy;
     }

     public divline_t(line_t   li)
     {
      this.x = li.v1.x;
      this.y = li.v1.y;
      this.dx = li.dx;
      this.dy = li.dy;
     }


     
     
 }
