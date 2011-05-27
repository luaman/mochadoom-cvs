package p;

import java.io.IOException;
import java.nio.ByteBuffer;

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
        
        @Override
        public void pack(ByteBuffer b) throws IOException{
            super.pack(b); //12            
            b.putInt(super.sectorid); // 16
            b.putInt(count); //20
            b.putInt(maxlight);//24
            b.putInt(minlight);//28
            b.putInt(darktime);//32
            b.putInt(brighttime);//36
        }
        
    };