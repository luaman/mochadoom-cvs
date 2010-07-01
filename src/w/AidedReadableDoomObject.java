package w;

import java.io.IOException;

/** This is for objects that can be read from disk, but cannot
 *  self-determine their own length for some reason.
 * 
 * @author Maes
 *
 */

public interface AidedReadableDoomObject {
    
    public void read(DoomFile f, int len) throws IOException ;
}
