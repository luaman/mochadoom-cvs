package rr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import utils.C2JUtils;
import w.CacheableDoomObject;
import w.DoomBuffer;
import w.DoomFile;
import w.ReadableDoomObject;

//Patches.
//A patch holds one or more columns.
//Patches are used for sprites and all masked pictures,
//and we compose textures from the TEXTURE1/2 lists
//of patches.

public class patch_t implements ReadableDoomObject,CacheableDoomObject{

    public short       width;      // bounding box size 
    public short       height; 
    public short       leftoffset; // pixels to the left of origin 
    public short       topoffset;  // pixels below the origin 
    public int[]         columnofs;   // only [width] used
 //                                 the [0] is &columnofs[width] 
    public column_t[] columns;
    
    @Override
    public void read(DoomFile f) throws IOException{
        // OK, for some fucked up reason these appear to be big endian in the WAD disk, and
        // whenever they are accessed their endianness is changed on-the-fly.
        this.width=f.readShort();
        this.height=f.readShort();
        this.leftoffset=f.readShort();
        this.topoffset=f.readShort();
        // As many columns as width...right???
        this.columnofs=new int[this.width];
        f.readIntArray(this.columnofs, this.columnofs.length);
        // TODO Auto-generated method stub
        
    }
    @Override
    public void unpack(ByteBuffer b)
            throws Exception {
        // Remember to reset the ByteBuffer position each time.
        b.position(0);
        // In ByteBuffers, the order can be conveniently set beforehand :-o
        b.order(ByteOrder.LITTLE_ENDIAN);
        // TODO Auto-generated method stub
        this.width=b.getShort();
        this.height=b.getShort();
        this.leftoffset=b.getShort();
        this.topoffset=b.getShort();
        // As many columns as width...right???
        this.columnofs=new int[this.width];
        this.columns=new column_t[this.width];
        C2JUtils.initArrayOfObjects( this.columns, column_t.class);
        
        // The offsets.
        DoomBuffer.readIntArray(b, this.columnofs, this.columnofs.length);
        for (int i=0;i<this.width;i++){
        	// Go to offset.
        	b.position(this.columnofs[i]);
        	this.columns[i].unpack(b);
        }

    }
}