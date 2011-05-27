package doom;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static utils.C2JUtils.pointer;
import w.DoomFile;
import w.IPackableDoomObject;
import w.IReadableDoomObject;

public class thinker_t implements IReadableDoomObject,IPackableDoomObject{
   
   public thinker_t prev;
   public thinker_t next;
   public think_t     function;
   
   /** extra fields, to use when archiving/unarchiving for
    * identification. Also in blocklinks, etc.
    */
   public int id,previd, nextid;
   
@Override
public void read(DoomFile f)
        throws IOException {
    // We are supposed to archive pointers to other thinkers,
    // but they are rather useless once on disk.
    previd=f.readLEInt();
    nextid=f.readLEInt();
   int tmp=f.readLEInt();
  // tmp&=0x00ff;
   //this.function=think_t.values()[tmp];
    
}

/** This adds 12 bytes */

@Override
public void pack(ByteBuffer b)
        throws IOException {
    // It's possible to reconstruct even by hashcodes.
    // As for the function, that should be implied by the mobj_t type.
    b.putInt(pointer(prev));
    b.putInt(pointer(next));
    b.putInt(pointer(function));    
}

}
