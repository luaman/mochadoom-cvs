package data;

import doom.think_t;
import data.Defines.statenum_t;

public class state_t {

	public state_t(){
		
	}
       
        public state_t(spritenum_t sprite, int frame, long tics, think_t action,
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
        /** was "unsigned" but wtf..*/
        public int          frame;
        public long          tics;
        //TODO: proper implementation of (*action)
        // MAES: was actionp_t... which is typedeffed to think_t anyway,
        // and this is the only place it's invoked explicitly.
        /** OK...this is the most infamous part of Doom to implement in Java.
         *  We can't have proper "function pointers" in java without either losing a LOT
         *  of speed (through reflection) or cluttering syntax and heap significantly
         *  (callback objects, which also need to be aware of context).
         *  Therefore, I decided to implement an "action dispatcher".
         *  This a
         *  
         */
        public think_t         action;
        public statenum_t            nextstate;
        public long          misc1, misc2;
        /** relative index in state array. Needed sometimes. */
        public int id;
        
        public String toString(){
            sb.setLength(0);
            sb.append(this.getClass().getName());
            sb.append(" sprite ");
            sb.append(this.sprite.name());
            sb.append(" frame ");
            sb.append(this.frame);

            return sb.toString();
            
        }
        
        protected static StringBuilder sb=new StringBuilder();
}
