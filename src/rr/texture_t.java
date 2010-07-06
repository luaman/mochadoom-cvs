package rr;

/** A maptexturedef_t describes a rectangular texture,
 *  which is composed of one or more mappatch_t structures
 *  that arrange graphic patches.
 *  
 * @author MAES
 *
 */

public class texture_t {
    // Keep name for switch changing, etc.
    // MAES: should this be mutable?
    String    name;        
    short   width;
    short   height;
    
    // All the patches[patchcount]
    //  are drawn back to front into the cached texture.
    short   patchcount;
    texpatch_t[]  patches;     
    
}