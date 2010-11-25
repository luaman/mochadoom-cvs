package p;

import m.random;
import rr.SectorAction;

//
// BROKEN LIGHT EFFECT
//

public class lightflash_t extends SectorAction {
	
    private random RND;
    
    public int     count;
    public int     maxlight;
    public int     minlight;
    public int     maxtime;
    public int     mintime;
    
    public lightflash_t(random RND){
        this.RND=RND;
    }
    
    /**
     * T_LightFlash
     * Do flashing lights.
     */
    
    public void LightFlash() {
        if (--count != 0)
            return;

        if (sector.lightlevel == maxlight) {
            sector.lightlevel = (short)minlight;
            count = (RND.P_Random() & mintime) + 1;
        } else {
            sector.lightlevel = (short)maxlight;
            count = (RND.P_Random() & maxtime) + 1;
        }

    }
    
}