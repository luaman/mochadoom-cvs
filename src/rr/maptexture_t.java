package rr;

/** Texture definition.
 *  A DOOM wall texture is a list of patches
 *  which are to be combined in a predefined order.
 *    
 *  @author MAES
 *
 */

public class maptexture_t {
    String        name;
    boolean     masked; 
    short       width;
    short       height;
    //void        **columndirectory;  // OBSOLETE
    short       patchcount;
    mappatch_t[]  patches;
};