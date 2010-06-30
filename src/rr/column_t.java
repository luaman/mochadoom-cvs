package rr;

import java.io.IOException;
import java.nio.ByteBuffer;

import utils.C2JUtils;
import w.CacheableDoomObject;
import w.DoomFile;
import w.ReadableDoomObject;

//column_t is a list of 0 or more post_t, (byte)-1 terminated
//typedef post_t  column_t;

public class column_t implements CacheableDoomObject, ReadableDoomObject{
    
    public short        topdelta;   // -1 is the last post in a column (actually 0xFF, since this was unsigned???)
    public short        length;     // length data bytes follows (actually add +2)
	public column_t[]      posts;    // This is quite tricky to read.
	public byte[] data;
    
    @Override
		public void unpack(ByteBuffer buf) throws IOException {
        // This is the only "easy stuff". The rest needs some guesswork.
    	this.topdelta=(short) C2JUtils.toUnsignedByte(buf.get());
    	this.length=(short) C2JUtils.toUnsignedByte(buf.get()); // There are actually +2 bytes of data to read.
    	this.data=new byte[2+this.length];
    	
	}

    @Override
    public void read(DoomFile f)
            throws IOException {
        this.topdelta=(short)f.readUnsignedByte();
        this.length=(short)f.readUnsignedByte();
        
    }
}


