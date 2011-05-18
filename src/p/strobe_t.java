package p;

import java.io.IOException;

import rr.SectorAction;
import w.DoomFile;

public class strobe_t extends SectorAction{
		
        public int     count;
        public int     minlight;
        public int     maxlight;
        public int     darktime;
        public int     brighttime;
        
        //
        // T_StrobeFlash
        //
        public void StrobeFlash() {
            if (--count != 0)
                return;

            if (sector.lightlevel == minlight) {
                sector.lightlevel = (short) maxlight;
                count = brighttime;
            } else {
                sector.lightlevel = (short) minlight;
                count = darktime;
            }

        }
        
        @Override
        public void read(DoomFile f) throws IOException{

            super.read(f); // Call thinker reader first            
            super.sectorid=f.readLEInt(); // Sector index
            count=f.readLEInt();
            maxlight=f.readLEInt();
            minlight=f.readLEInt();
            darktime=f.readLEInt();
            brighttime=f.readLEInt();       
            }
        
    };