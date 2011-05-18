package p;

import java.io.IOException;

import rr.SectorAction;
import rr.sector_t;
import w.DoomFile;
import w.IReadableDoomObject;

public class plat_t extends SectorAction implements IReadableDoomObject{
        
        public sector_t   sector;
        /** fixed_t */
        public int speed,low,high;
        int     wait;
        int     count;
        public plat_e  status;
        public plat_e  oldstatus;
        boolean crush;
        public int     tag;
        public plattype_e  type;
        
        @Override
        public void read(DoomFile f) throws IOException{

            super.read(f); // Call thinker reader first            
            super.sectorid=f.readLEInt(); // Sector index
            speed=f.readLEInt();
            low=f.readLEInt();
            high=f.readLEInt();
            wait=f.readLEInt();
            count=f.readLEInt();
            status=plat_e.values()[f.readInt()];
            oldstatus=plat_e.values()[f.readInt()];
            crush=f.readIntBoolean();
            tag=f.readLEInt();
            type=plattype_e.values()[f.readLEInt()];        
            }
        
    } 
