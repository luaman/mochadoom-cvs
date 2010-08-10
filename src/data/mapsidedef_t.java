package data;

/**
 * A SideDef, defining the visual appearance of a wall,
 * by setting textures and offsets.
 */

public class mapsidedef_t {

    public mapsidedef_t(){
       toptexture=new char[8];
       bottomtexture=new char[8];
       midtexture=new char[8];
    }
    
   public short     textureoffset;
   public short     rowoffset;
   public char[]        toptexture;
   public char[]        bottomtexture;
   public char[]        midtexture;
   /** Front sector, towards viewer. */
   public short     sector;
 }
