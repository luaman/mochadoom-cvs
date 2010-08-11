package rr;

import m.fixed_t;

/** The SideDef.
 * 
 * @author admin
 *
 */
public class side_t {
     /** (fixed_t) add this to the calculated texture column */
     public int textureoffset;
     
     /** (fixed_t) add this to the calculated texture top */
     public int rowoffset;

     /** Texture indices.
      * We do not maintain names here.  
      */
     public short   toptexture;
     public short   bottomtexture;
     public short   midtexture;

     /** Sector the SideDef is facing. MAES: pointer */
     public sector_t   sector;

     public side_t() {
     }
     
    public side_t(int textureoffset, int rowoffset, short toptexture,
            short bottomtexture, short midtexture, sector_t sector) {
        super();
        this.textureoffset = textureoffset;
        this.rowoffset = rowoffset;
        this.toptexture = toptexture;
        this.bottomtexture = bottomtexture;
        this.midtexture = midtexture;
        this.sector = sector;
    }
     
 }
