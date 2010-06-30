package data;

import data.info.spritenum_t;
import data.info.statenum_t;

public class state_t {

       
        public state_t(spritenum_t sprite, long frame, long tics, actionf_t action,
            statenum_t nextstate, long misc1, long misc2) {
        this.sprite = sprite;
        this.frame = frame;
        this.tics = tics;
        this.action = action;
        this.nextstate = nextstate;
        this.misc1 = misc1;
        this.misc2 = misc2;
    }
        public spritenum_t   sprite;
        public long          frame;
        public long          tics;
        //TODO: proper implementation of (*action)
        public actionf_t         action;
        public statenum_t            nextstate;
        public long          misc1, misc2;
}
