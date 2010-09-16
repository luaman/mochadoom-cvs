package data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import w.CacheableDoomObject;

/** mapthing_t ... same on disk AND in memory, wow?! */

public class mapthing_t implements CacheableDoomObject,Cloneable{
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
}