package rr;

import m.fixed_t;

/**The LineSeg.
 *  
 * @author admin
 *
 */
public class seg_t {

   // MAES: pointers
   vertex_t   v1;
   vertex_t   v2;
   
   fixed_t offset;

   // MAES: Well, type safety above everything...or not? Was angle_t
   int angle;

   // MAES: all were single pointers.
   
   side_t sidedef;
   line_t linedef;

   // Sector references.
   // Could be retrieved from linedef, too.
   // backsector is NULL for one sided lines
   sector_t   frontsector;
   sector_t   backsector;
   
}
