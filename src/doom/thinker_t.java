package doom;

import java.io.IOException;

import w.DoomFile;
import w.IReadableDoomObject;

public class thinker_t implements IReadableDoomObject{
   
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
  // this.function=think_t.values()[tmp];
    
}

}
