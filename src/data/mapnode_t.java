package data;

/** BSP Node structure */
public class mapnode_t {
    
    public mapnode_t(){
        this.bbox=new short[2][4];
        this.children=new int[2];
    }

      // Partition line from (x,y) to x+dx,y+dy)
      public short     x;
      public short     y;
      public short     dx;
      public short     dy;

      /** Bounding box for each child, clip against view frustum. */
      public short[][]     bbox;

      /** If NF_SUBSECTOR its a subsector,  else it's a node of another subtree. */
      public int[] children=new int[2]; // MAES: used to be unsigned short.      
    } ;
