package p;

import static p.DoorDefines.GLOWSPEED;
import rr.SectorAction;

public class glow_t extends SectorAction{
        public int     minlight;
        public int     maxlight;
        public int     direction;

        //
        // Spawn glowing light
        //

        public void Glow() {
            switch (direction) {
            case -1:
                // DOWN
                sector.lightlevel -= GLOWSPEED;
                if (sector.lightlevel <= minlight) {
                    sector.lightlevel += GLOWSPEED;
                    direction = 1;
                }
                break;

            case 1:
                // UP
                sector.lightlevel += GLOWSPEED;
                if (sector.lightlevel >= maxlight) {
                    sector.lightlevel -= GLOWSPEED;
                    direction = -1;
                }
                break;
            }
        }
        
    }