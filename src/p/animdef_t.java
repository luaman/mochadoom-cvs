package p;

/** source animation definition
 * 
 * @author admin
 *
 */
public class animdef_t {


public animdef_t(boolean istexture, String endname, String startname,
            int speed) {
        super();
        this.istexture = istexture;
        this.endname = endname;
        this.startname = startname;
        this.speed = speed;
    }
public boolean istexture;  // if false, it is a flat
public String  endname; // MAES: used to be char[9].
public String  startname; // MAES: used to be char[9].
public int     speed;
}
