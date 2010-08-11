package data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import w.CacheableDoomObject;
import w.DoomBuffer;

/**
 * A LineDef, as used for editing, and as input to the BSP builder.
 */
public class maplinedef_t implements CacheableDoomObject{

    public maplinedef_t() {
        this.sidenum = new short[2];
    }

    public short v1;

    public short v2;

    public short flags;

    public short special;

    public short tag;

    /** sidenum[1] will be -1 if one sided */
    public short[] sidenum;

    public static int sizeOf() {
        return 14;
    }

    @Override
    public void unpack(ByteBuffer buf)
            throws IOException {
    buf.order(ByteOrder.LITTLE_ENDIAN);
    this.v1 = buf.getShort();
    this.v2 = buf.getShort();
    this.flags = buf.getShort();
    this.special = buf.getShort();
    this.tag = buf.getShort();
    DoomBuffer.readShortArray(buf, this.sidenum, 2);
    }
}
