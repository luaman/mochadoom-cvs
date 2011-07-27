package p;

import rr.line_t;

public class button_t implements Resettable{

        public line_t line;
        public bwhere_e    where;
        public int     btexture;
        public int     btimer;
        public mobj_t soundorg;
        
        public button_t(){	
        	this.btexture=0;
        	this.btimer=0;
        	this.where=bwhere_e.top;
        }
 
        public void reset(){
            this.line=null;
            this.where=bwhere_e.top;
            this.btexture=0;
            this.btimer=0;
            this.soundorg=null;
            
        }

    }