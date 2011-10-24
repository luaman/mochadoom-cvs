package w;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import m.Swap;

/** filelumps are on-disk structures. lumpinfos are almost the same, but are memory only.
 * 
 * @author Maes
 *
 */

public class filelump_t  implements IReadableDoomObject {
        int         filepos;
        long         size; // Is INT 32-bit in file!
        String        name;
        
        public void read(DataInputStream f) throws IOException{
            // MAES: Byte Buffers actually make it convenient changing byte order on-the-fly.
            // But RandomAccessFiles don't :-S

            filepos=DoomIO.readLEInt(f);
            size=DoomIO.readLEInt(f);
            // Names used in the reading subsystem should be upper case.
            name=DoomIO.readNullTerminatedString(f,8).toUpperCase();
        }

        public void write(DataOutputStream f) throws IOException{
            f.writeInt(filepos);
            f.write((int)this.size);
            DoomIO.writeString(f,this.name,8);
        }
        
        public static int sizeof(){
            return (4+4+8);
        }
        
    }
