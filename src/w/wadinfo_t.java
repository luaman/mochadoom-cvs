package w;

import java.io.IOException;
import java.io.RandomAccessFile;

public class wadinfo_t {
        // Should be "IWAD" or "PWAD".
        String       identification;      
        long         numlumps;
        long         infotableofs;
     

        
        /** Reads the wadinfo_t from the file.*/
        public void load(DoomFile f) throws IOException {
            
            identification = f.readString(4);
            numlumps=f.readLEInt();
            infotableofs=f.readLEInt();
        }
        
    }
