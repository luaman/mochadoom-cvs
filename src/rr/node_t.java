package rr;

import utils.C2JUtils;
import m.BBox;
import m.fixed_t;

/** BSP node.
 * 
 * @author admin
 *
 */
public class node_t {

    
    public node_t(){
        bbox=new BBox[2];
        children= new char[2];
        C2JUtils.initArrayOfObjects(bbox, BBox.class);
    }
    
     public node_t(int x, int y, int dx, int dy, BBox[] bbox,
            char[] children) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.bbox = bbox;
        this.children = children;
    }


    /** (fixed_t) Partition line. */
     public int x, y, dx, dy;

     /** Bounding box for each child. */
     //public fixed_t bbox[2][4];
     // Maes: make this into two proper bboxes?
          
     public BBox[] bbox;
     

     /** If NF_SUBSECTOR its a subsector. MAES: was unsigned short...so char. */
     public char[] children;
     
 }

