package p;

import rr.SectorAction;

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
        
    };