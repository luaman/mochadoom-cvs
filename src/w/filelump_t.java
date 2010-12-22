package w;

import java.io.IOException;

import m.Swap;

/** filelumps are on-disk structures. lumpinfos are almost the same, but are memory only.
 * 
 * @author Maes
 *
 */

public class filelump_t  implements ReadableDoomObject {
        int         filepos;
        long         size; // Is INT 32-bit in file!
        String        name;
        
        public void read(DoomFile f) throws IOException{
            // MAES: Byte Buffers actually make it convenient changing byte order on-the-fly.
            // But RandomAccessFiles don't :-S

            filepos=Swap.LONG(f.readInt());
            size=Swap.LONG(f.readInt());
            // Names used in the reading subsystem should be upper case.
            name=f.readNullTerminatedString(8).toUpperCase();
        }

        public void write(DoomFile f) throws IOException{
            f.write(filepos);
            f.write((int)this.size);
            f.writeString(this.name,8);
        }
        
        public static int sizeof(){
            return (4+4+8);
        }
        
    }
