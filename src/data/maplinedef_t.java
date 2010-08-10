package data;

/**
 * A LineDef, as used for editing, and as input
 * to the BSP builder.
 *
 */
public class maplinedef_t {

   public maplinedef_t(){
       this.sidenum=new int[2];
   }
   
   public short     v1;
   public short     v2;
   public short     flags;
   public short     special;
   public short     tag;
   /** sidenum[1] will be -1 if one sided */
   int[]     sidenum;
 } 
