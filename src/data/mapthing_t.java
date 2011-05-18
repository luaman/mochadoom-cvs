package data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import m.Swap;

import w.CacheableDoomObject;
import w.DoomFile;
import w.IReadableDoomObject;
import w.IWritableDoomObject;

/** mapthing_t ... same on disk AND in memory, wow?! */

public class mapthing_t implements CacheableDoomObject,IReadableDoomObject,IWritableDoomObject,Cloneable{
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
        // TODO: Little endian OK?
        f.writeShort(Swap.SHORT(x));
        f.writeShort(Swap.SHORT(y));
        f.writeShort(Swap.SHORT(angle));
        f.writeShort(Swap.SHORT(type));
        f.writeShort(Swap.SHORT(options));
        
    }

    @Override
    public void read(DoomFile f)
            throws IOException {
        x=f.readLEShort();
        y=f.readLEShort();
        angle=f.readLEShort();
        type=f.readLEShort();
        options=x=f.readLEShort();
        }
}