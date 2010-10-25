package doom;

import java.nio.ByteBuffer;

import utils.C2JUtils;
import w.DoomBuffer;

public class doomdata_t implements DatagramSerializable {

    public static final int DOOMDATALEN=8+data.Defines.BACKUPTICS*ticcmd_t.TICCMDLEN;
    
     // High bit is retransmit request.
     /** MAES: was "unsigned" */
     public int        checksum; 
     
     /* CAREFUL!!! Those "bytes" are actually unsigned
      * 
      */
     
     /** Only valid if NCMD_RETRANSMIT. */     
     public byte        retransmitfrom;
     
     public byte        starttic;
     public byte        player;
     public byte        numtics;
     public ticcmd_t[]        cmds;
     
    public doomdata_t(){
        cmds=new ticcmd_t[data.Defines.BACKUPTICS];
        C2JUtils.initArrayOfObjects(cmds);
        // Enough space for its own header + the ticcmds;
        buffer=new byte[DOOMDATALEN];
        bbuf=ByteBuffer.wrap(buffer);
    }
    
    // Used for datagram serialization.
    public byte[] buffer;
    private ByteBuffer bbuf;

    @Override
    public byte[] pack() {        
        
        bbuf.rewind();
        
        // Why making it harder?
        bbuf.putInt(checksum);
        bbuf.put(retransmitfrom);
        bbuf.put(starttic);
        bbuf.put(player);
        bbuf.put(numtics);
        
        // FIXME: it's probably more efficient to use System.arraycopy ? 
        // Or are the packets too small anyway? At most we'll be sending "doomdata_t's"
        
        for (int i=0;i<cmds.length;i++){
            bbuf.put(cmds[i].pack());
        }
        
        return bbuf.array();
    
    }

    @Override
    public void pack(byte[] buf, int offset) {
        
        // No need to make it harder...just pack it and slap it in.
        byte[] tmp=this.pack();
        System.arraycopy(tmp, 0, buf, offset, tmp.length);        
    }

    @Override
    public void unpack(byte[] buf) {
        unpack(buf,0);
    }

    @Override
    public void unpack(byte[] buf, int offset) {
        checksum=DoomBuffer.getBEInt(buf);
        offset=+4;
        retransmitfrom=buf[offset++];
        starttic=buf[offset++];
        player=buf[offset++];
        numtics=buf[offset++];
        
        for (int i=0;i<cmds.length;i++){
            cmds[i].unpack(buf,offset);
            offset+=ticcmd_t.TICCMDLEN;
            }
        
    }
    
    public void selfUnpack(){
        unpack(this.buffer);
    }

 }
