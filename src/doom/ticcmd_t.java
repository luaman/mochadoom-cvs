package doom;

public class ticcmd_t {
    /** *2048 for move */
    public byte    forwardmove;
    /** *2048 for move */
    public byte    sidemove;   
    /** <<16 for angle delta */
    public short   angleturn;  
    /** checks for net game */
    public short   consistancy;    
        public char    chatchar, buttons; // MAES: these are unsigned bytes :-/
    } ;