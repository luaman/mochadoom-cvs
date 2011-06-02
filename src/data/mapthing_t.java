package data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import m.Swap;

import w.CacheableDoomObject;
import w.DoomFile;
import w.IPackableDoomObject;
import w.IReadableDoomObject;
import w.IWritableDoomObject;

/** mapthing_t ... same on disk AND in memory, wow?! */

public class mapthing_t implements CacheableDoomObject,IPackableDoomObject,IWritableDoomObject,Cloneable{
    public short x;

    public short y;

    public short angle;

    public short type;

    public short options;

    public mapthing_t() {
    }

    public static int sizeOf() {
        return 10;
    }

    @Override
    public void unpack(ByteBuffer buf)
            throws IOException {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        this.x = buf.getShort();
        this.y = buf.getShort();
        this.angle = buf.getShort();
        this.type = buf.getShort();
        this.options = buf.getShort();
        
    }
    
    public mapthing_t clone(){

    	try {
			return (mapthing_t)super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }

    @Override
    public void write(DoomFile f)
            throws IOException {
        
        // More efficient, avoids duplicating code and
        // handles little endian better.
        iobuffer.position(0);
        iobuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.pack(iobuffer);
        f.write(iobuffer.array());
        
    }

    public void pack(ByteBuffer b) {
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putShort(x);
        b.putShort(y);
        b.putShort(angle);
        b.putShort(type);
        b.putShort(options);
    }
    
    private static ByteBuffer iobuffer=ByteBuffer.allocate(10);
}