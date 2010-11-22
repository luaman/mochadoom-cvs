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

public String toString(){
	sb.setLength(0);
	sb.append("animdef_t");	
	sb.append(" texture ");
	sb.append(istexture);
	sb.append(" start ");
	sb.append(startname);
	sb.append(" end ");
	sb.append(endname);
	sb.append(" speed " );
	sb.append(speed);
	return sb.toString();
	
}

private static StringBuilder sb=new StringBuilder();

}
