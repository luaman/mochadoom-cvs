package rr;
import static data.Defines.*;
import utils.C2JUtils;
/** Now what is a visplane, anyway?
 *  Basically, it's a big buffer representing
 *  a top and a bottom ro
 * 
 * @author admin
 *
 */
//
//

public class visplane_t{
    
    public static final int TOPOFFSET=1;
    public static final int MIDDLEPADDING=2;
    public static final int BOTTOMOFFSET=SCREENWIDTH+TOPOFFSET+MIDDLEPADDING;
    
    public visplane_t(){
        this.data=new char[4+2*SCREENWIDTH];
        this.updateHashCode();
    }
    
    
    public visplane_t(int height, int picnum, int lightlevel){
        this.height=height;
        this.picnum=picnum;
        this.lightlevel=lightlevel;
        this.updateHashCode();
        this.data=new char[4+2*SCREENWIDTH];
        }
    
    
/** (fixed_t) */
public int       height;
public int           picnum;
public int           lightlevel;
public int           minx;
public int           maxx;

// leave pads for [minx-1]/[maxx+1]

/*
public byte      pad1;
// Here lies the rub for all
//  dynamic resize/change of resolution.
public byte[]      top=new byte[SCREENWIDTH];
public byte      pad2;
public byte      pad3;
// See above.
public byte[]      bottom=new byte [SCREENWIDTH];
public byte      pad4;*/

char data[];

// Hack to allow quick clearing of visplanes.
protected static char[] clearvisplane=new char[SCREENWIDTH];


/** "Clear" the top with FF's.  */
public void clearTop(){
    System.arraycopy(clearvisplane, 0, this.data, TOPOFFSET, SCREENWIDTH);
      
}

/** "Clear" the bottom with FF's.  */
public void clearBottom(){
    System.arraycopy(clearvisplane, 0, this.data, BOTTOMOFFSET, SCREENWIDTH);      
}

public void setTop(int index, char value){
    this.data[TOPOFFSET+index]=value;    
}

public char getTop(int index){
    return this.data[TOPOFFSET+index];
    
}

public void setBottom(int index, char value){
    this.data[BOTTOMOFFSET+index]=value;
    
}

public int getBottom(int index){
    return this.data[BOTTOMOFFSET+index];
    
}

static{
    for (int i=0;i<clearvisplane.length;i++)
        clearvisplane[i]=Character.MAX_VALUE;
}


public String toString(){
    sb.setLength(0);
    sb.append("Visplane\n");
    sb.append('\t');
    sb.append("Height: ");
    sb.append(this.height);
    sb.append('\t');
    sb.append("Min-Max: ");
    sb.append(this.minx);
    sb.append('-');
    sb.append(this.maxx);
    sb.append('\t');
    sb.append("Picnum: ");    
    sb.append(this.picnum);
    sb.append('\t');
    sb.append("Lightlevel: ");
    sb.append(this.lightlevel);

    return sb.toString();
    
    
}

protected int hash;

/** Call this upon any changed in height, picnum or lightlevel */

public void updateHashCode(){
    this.hash=height^picnum^lightlevel;
}

public int hashCode(){
    return this.hash;
}

public static int visplaneHash(int height, int picnum, int lightlevel){
    return height^picnum^lightlevel;
    
}

protected static StringBuilder sb=new StringBuilder();


};
