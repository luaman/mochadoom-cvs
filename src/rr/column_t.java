package rr;

import java.io.IOException;
import java.nio.ByteBuffer;

import w.CacheableDoomObject;
import w.DoomFile;
import w.ReadableDoomObject;

//column_t is a list of 0 or more post_t, (byte)-1 terminated
//typedef post_t  column_t;

public class column_t implements CacheableDoomObject, ReadableDoomObject{
    
    public short        topdelta;   // -1 is the last post in a column
    public short        length;     // length data bytes follows
	public byte[] data;
    
    @Override
		public void unpack(ByteBuffer buf) throws IOException {
    	this.topdelta=buf.get();
    	this.length=buf.get();
	}

    @Override
    public void read(DoomFile f)
            throws IOException {
        this.topdelta=f.readByte();
        this.length=f.readByte();
        
    }
}


