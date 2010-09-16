package rr;

import doom.thinker_t;

/** Used for doors, ceilings etc. that have a thinker */
 
public abstract class SectorAction {
    
    public SectorAction(){
        this.thinker=new thinker_t();
    }
    
    public thinker_t   thinker;
}
