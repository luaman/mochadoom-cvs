package w;

import java.io.IOException;

public class wadinfo_t implements IReadableDoomObject{
        // Should be "IWAD" or "PWAD".
        String       identification;      
        long         numlumps;
        long         infotableofs;
             
        /** Reads the wadinfo_t from the file.*/
        public void read(DoomFile f) throws IOException {
            
            identification = f.readString(4);
            numlumps=f.readLEInt();
            infotableofs=f.readLEInt();
        }
        
    }