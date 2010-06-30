package doom;

public class ticcmd_t {
        byte    forwardmove;    // *2048 for move
        byte    sidemove;   // *2048 for move
        short   angleturn;  // <<16 for angle delta
        short   consistancy;    // checks for net game
        public char    chatchar; // MAES: these are unsigned bytes :-/
        char    buttons;
    } ;