package rr;

import java.io.IOException;

import w.DoomFile;
import w.IReadableDoomObject;
import m.fixed_t;

/** The SideDef.
 * 
 * @author admin
 *
 */
public class side_t implements IReadableDoomObject{
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

     public int sectorid;
     
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

    @Override
    public void read(DoomFile f)
            throws IOException {
        this.textureoffset = f.readLEShort() << fixed_t.FRACBITS;
        this.rowoffset = f.readLEShort() << fixed_t.FRACBITS;
        this.toptexture = f.readLEShort();
        this.bottomtexture = f.readLEShort();
        this.midtexture = f.readLEShort();
        //this.sectorid=f.readLEInt();
        
    }
    

    
     
 }
