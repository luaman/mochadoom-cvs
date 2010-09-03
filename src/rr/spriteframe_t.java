package rr;

/** Sprites are patches with a special naming convention
 * so they can be recognized by R_InitSprites.
 * The base name is NNNNFx or NNNNFxFx, with
 * x indicating the rotation, x = 0, 1-7.
 * The sprite and frame specified by a thing_t
 * is range checked at run time.
 * A sprite is a patch_t that is assumed to represent
 * a three dimensional object and may have multiple
 * rotations pre drawn.
 * Horizontal flipping is used to save space,
 * thus NNNNF2F5 defines a mirrored patch.
 * Some sprites will only have one picture used
 * for all views: NNNNF0
 */
public class spriteframe_t implements Cloneable{

 public spriteframe_t(){
     lump=new short[8];
     flip=new byte[8];
 }
    
    /** NOTE: due to an ugly hack this can be set to -1 during loading time,
    * so we can't declare it boolean :-/ */
    
 public byte lrotate;

 /** If false use 0 for any position.
  * Note: as eight entries are available,
  * we might as well insert the same name eight times. */
 public boolean rotate;

 /** Lump to use for view angles 0-7. */
 public short[]    lump;

 /** Flip bit (1 = flip) to use for view angles 0-7. */
 public byte[]    flip;
 
 public spriteframe_t clone(){
     spriteframe_t response=new spriteframe_t();
     response.lrotate=lrotate;
     response.rotate=rotate;     
     System.arraycopy(this.lump, 0, response.lump, 0, lump.length);
     System.arraycopy(this.flip, 0, response.flip, 0, flip.length);
     return response;
     
 }
 

}
