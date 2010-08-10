package rr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import w.CacheableDoomObject;
import m.fixed_t;

/**The LineSeg. Must be built from on-disk mapsegs_t, which are much 
 * simpler.
 *  
 * @author admin
 *
 */
public class seg_t{

   /** To be used as references */
   public vertex_t   v1, v2;

   
   /** (fixed_t) */
   public int offset;

   /** (angle_t) */
   public int angle;

   // MAES: all were single pointers.
   
   public side_t sidedef;
   public line_t linedef;

   /** Sector references.
    Could be retrieved from linedef, too.
    backsector is NULL for one sided lines */
   public sector_t   frontsector,backsector;
   
}
