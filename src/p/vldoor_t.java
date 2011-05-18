package p;

import java.io.IOException;

import rr.SectorAction;
import w.DoomFile;
import w.IReadableDoomObject;

public class vldoor_t extends SectorAction implements IReadableDoomObject{
        
        public vldoor_e    type;
        /** fixed_t */
        public int topheight, speed;

        /** 1 = up, 0 = waiting at top, -1 = down */
        public int direction;
        
        /** tics to wait at the top */
        public int             topwait;
        
        /**(keep in case a door going down is reset)
           when it reaches 0, start going down */        
        public int             topcountdown;
        
        @Override
        public void read(DoomFile f) throws IOException{

            super.read(f); // Call thinker reader first            
            type=vldoor_e.values()[f.readLEInt()];
            super.sectorid=f.readLEInt(); // Sector index (or pointer?)
            topheight=f.readLEInt();
            speed=f.readLEInt();
            direction=f.readLEInt();
            topwait=f.readLEInt();
            topcountdown=f.readLEInt();            
            }
        
    }