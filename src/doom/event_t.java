package doom;

// Event structure.

public class event_t {
    
        public event_t(evtype_t type,int data){
            this.type=type;
            this.data1=data;
        }
        
        
        public event_t(char c){
            this.type=evtype_t.ev_keydown;
            this.data1=c;
        }
        
        public evtype_t    type;
        public int     data1;      // keys / mouse/joystick buttons
        public int     data2;      // mouse/joystick x move
        public int     data3;      // mouse/joystick y move
    }; 
