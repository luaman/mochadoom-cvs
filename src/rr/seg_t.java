package rr;

import static m.fixed_t.FRACBITS;
import static m.fixed_t.FixedMul;

/**The LineSeg. Must be built from on-disk mapsegs_t, which are much 
 * simpler.
 *  
 * @author Maes
 *
 */

public class seg_t{

   /** To be used as references */
   public vertex_t   v1, v2;

   /** Local caching. Spares us using one extra reference level */
   public int v1x,v1y,v2x,v2y;
   
   /** (fixed_t) */
   public int offset;

   /** (angle_t) */
   public long angle;

   // MAES: all were single pointers.
   
   public side_t sidedef;
   public line_t linedef;

   /** Sector references.
    Could be retrieved from linedef, too.
    backsector is NULL for one sided lines */
   public sector_t   frontsector,backsector;
   
   
   /** R_PointOnSegSide
   *  
   * @param x
   * @param y
   * @param line
   * @return
   */
  public static int
  PointOnSegSide
  ( int   x,
    int   y,
    seg_t line )
  {
      int lx;
      int ly;
      int ldx;
      int ldy;
      int dx;
      int dy;
      int left;
      int right;
      
      lx = line.v1.x;
      ly = line.v1.y;
      
      ldx = line.v2.x-lx;
      ldy = line.v2.y-ly;
      
      if (ldx==0)
      {
      if (x <= lx)
          return (ldy>0)?1:0;
      
      return (ldy<0)?1:0;
      }
      if (ldy==0)
      {
      if (y <= ly)
          return (ldx < 0)?1:0;
      
      return (ldx > 0)?1:0;
      }
      
      dx=x-lx;
      dy=y-ly;
      
      // Try to quickly decide by looking at sign bits.
      if ( ((ldy ^ ldx ^ dx ^ dy)&0x80000000 )!=0)
      {
      if  ( ((ldy ^ dx) & 0x80000000 )!=0)
      {
          // (left is negative)
          return 1;
      }
      return 0;
      }

      left = FixedMul ( ldy>>FRACBITS , dx );
      right = FixedMul ( dy , ldx>>FRACBITS );
      
      if (right<left)
      {
      // front side
      return 0;
      }
      // back side
      return 1;           
  }
   
  /** R_PointOnSegSide
   *  
   * @param x
   * @param y
   * @param line
   * @return
   */
  public int
  PointOnSegSide
  ( int   x,
    int   y
    )
  {
      int lx;
      int ly;
      int ldx;
      int ldy;
      int dx;
      int dy;
      int left;
      int right;
      
      lx = this.v1.x;
      ly = this.v1.y;
      
      ldx = this.v2.x-lx;
      ldy = this.v2.y-ly;
      
      if (ldx==0)
      {
      if (x <= lx)
          return (ldy>0)?1:0;
      
      return (ldy<0)?1:0;
      }
      if (ldy==0)
      {
      if (y <= ly)
          return (ldx < 0)?1:0;
      
      return (ldx > 0)?1:0;
      }
      
      dx=x-lx;
      dy=y-ly;
      
      // Try to quickly decide by looking at sign bits.
      if ( ((ldy ^ ldx ^ dx ^ dy)&0x80000000 )!=0)
      {
      if  ( ((ldy ^ dx) & 0x80000000 )!=0)
      {
          // (left is negative)
          return 1;
      }
      return 0;
      }

      left = FixedMul ( ldy>>FRACBITS , dx );
      right = FixedMul ( dy , ldx>>FRACBITS );
      
      if (right<left)
      {
      // front side
      return 0;
      }
      // back side
      return 1;           
  }
  
  public String toString(){
      sb.setLength(0);
      sb.append("Seg\n");
      //sb.append(id);
      sb.append("Frontsector:\t ");
      sb.append(this.frontsector);
      sb.append("\n\t");
      sb.append("Backsector:\t ");
      sb.append(this.backsector);
      sb.append("\n\t");
      sb.append(Integer.toHexString(this.v1.x));
      sb.append('\t');
      sb.append(Integer.toHexString(this.v1.y));
      sb.append('\t');
      sb.append(Integer.toHexString(this.v2.x));
      sb.append('\t');
      sb.append(Integer.toHexString(this.v2.y));
      sb.append('\n');

      return sb.toString();
      
  }


  static StringBuilder sb=new StringBuilder();
   
}
