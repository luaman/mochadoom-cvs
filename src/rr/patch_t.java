package rr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import utils.C2JUtils;
import w.CacheableDoomObject;
import w.DoomBuffer;
import w.DoomFile;
import w.IReadableDoomObject;

//Patches.
//A patch holds one or more columns.
//Patches are used for sprites and all masked pictures,
//and we compose textures from the TEXTURE1/2 lists
//of patches.

public class patch_t implements /*IReadableDoomObject,*/CacheableDoomObject{

    /** bounding box size */ 
    public short       width,   height;  
    /** pixels to the left of origin */
    public short       leftoffset;  
    /** pixels below the origin */
    public short       topoffset;   
    /** This used to be an implicit array pointing to raw posts of data. 
     * 
     * only [width] used the [0] is &columnofs[width] */
    public int[]         columnofs;     
    /** The ACTUAL data is here, nicely deserialized (well, almost) */
    public column_t[] columns;
    
    /** Added for debug aid purposes */
    public String name;
    
  /*  @Override
    public void read(DoomFile f) throws IOException{

        long pos=f.getFilePointer();
        this.width=f.readLEShort();
        this.height=f.readLEShort();
        this.leftoffset=f.readLEShort();
        this.topoffset=f.readLEShort();
        // As many columns as width...right???
        this.columnofs=new int[this.width];
        this.columns=new column_t[this.width];
        C2JUtils.initArrayOfObjects( this.columns, column_t.class);
        
        // Read the column offsets.
        f.readIntArray(this.columnofs, this.columnofs.length, ByteOrder.LITTLE_ENDIAN);
        for (int i=0;i<this.width;i++){
            // Go to offset.
            //f.seek(pos+this.columnofs[i]);
            this.columns[i].read(f);
        }
        
    }*/
    
    /** In the C code, reading is "aided", aka they know how long the header + all
     *  posts/columns actually are on disk, and only "deserialize" them when using them.
     *  Here, we strive to keep stuff as elegant and OO as possible, so each column will get 
     *  deserialized one by one. I thought about reading ALL column data as raw data, but
     *  IMO that's shit in the C code, and would be utter shite here too. Ergo, I cleanly 
     *  separate columns at the patch level (an advantage is that it's now easy to address
     *  individual columns). However, column data is still read "raw".
     */
    
    @Override
    public void unpack(ByteBuffer b)
            throws IOException {
        // Remember to reset the ByteBuffer position each time.
        b.position(0);
        // In ByteBuffers, the order can be conveniently set beforehand :-o
        b.order(ByteOrder.LITTLE_ENDIAN);
        
        this.width=b.getShort();
        
        this.height=b.getShort();
        this.leftoffset=b.getShort();
        this.topoffset=b.getShort();
        // As many columns as width...right???
        this.columnofs=new int[this.width];
        this.columns=new column_t[this.width];
        C2JUtils.initArrayOfObjects( this.columns, column_t.class);
        
        // Compute the ACTUAL full-column sizes.
        int[] actualsizes=new int[columns.length];
        
        for (int i=0;i<actualsizes.length-1;i++){
            actualsizes[i]=columnofs[i+1]-columnofs[i];
        }
        
        // The offsets.
        DoomBuffer.readIntArray(b, this.columnofs, this.columnofs.length);
        for (int i=0;i<this.width;i++){
        	// Go to offset.
        	b.position(this.columnofs[i]);
        	try {
        	this.columns[i].unpack(b);
        	} catch (Exception e){
        		// Error during loading. Column will be set to a special
        		// error column rather than breaking stuff later on.
        		this.columns[i]=invalid_column;
        	}
        }

    }
    
    private final static column_t invalid_column;
    
    // Temporary safeguard vs badly computed stuff.
    static{
    	invalid_column=new column_t();
    	invalid_column.data=new byte[133];
    for (int i=4;i<133;i++){
    	invalid_column.data[i]=(byte) (i-4);
    }
    invalid_column.posts=1;
    invalid_column.length=128;
    invalid_column.topdelta=0;
   
    }
}