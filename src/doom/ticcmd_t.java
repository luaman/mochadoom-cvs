package doom;

public class ticcmd_t{
    /** *2048 for move */
    public byte forwardmove;

    /** *2048 for move */
    public byte sidemove;

    /** <<16 for angle delta */
    public short angleturn;

    /** checks for net game */
    public short consistancy;
    
    /** MAES: these are unsigned bytes :-( */

    public char chatchar, buttons; 

    /** replaces G_CmdChecksum (ticcmd_t cmd) */

    public int getChecksum(ticcmd_t cmd) {
        int sum = 0;
        sum += forwardmove;
        sum += sidemove;
        sum += angleturn;
        sum += consistancy;
        sum += chatchar;
        sum += buttons;
        return sum;
        }

    /** because Cloneable is bullshit */
    
     public void copyTo(ticcmd_t dest){
         dest.forwardmove = forwardmove;
         dest.sidemove = sidemove;
         dest.angleturn= angleturn;
         dest.consistancy= consistancy;
         dest.chatchar= chatchar;
         dest.buttons= buttons;
     }
     
     public String toString(){
         StringBuilder str=new StringBuilder();
         str.append("Buttons" );
         str.append(Integer.toHexString(this.buttons));
         return str.toString();
     }
    
};