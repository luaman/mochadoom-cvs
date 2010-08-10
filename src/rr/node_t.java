package rr;

import m.BBox;
import m.fixed_t;

/** BSP node.
 * 
 * @author admin
 *
 */
public class node_t {

    
     public node_t(fixed_t x, fixed_t y, fixed_t dx, fixed_t dy, BBox[] bbox,
            char[] children) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.BBox = bbox;
        this.children = children;
    }


    // Partition line.
     public fixed_t x;
     public fixed_t y;
     public fixed_t dx;
     public fixed_t dy;

     // Bounding box for each child.
     //public fixed_t bbox[2][4];
     // Maes: make this two proper bboxes
     
     public BBox[] BBox=new BBox[2];
     

     // If NF_SUBSECTOR its a subsector. MAES: was unsigned short...so char.
     public char[] children=new char[2];
     
 }

