package rr;
import java.util.Arrays;

import utils.C2JUtils;
import v.IVideoScale;
import v.IVideoScaleAware;
/** Now what is a visplane, anyway?
 *  Basically, it's a big buffer representing
 *  a top and a bottom boundary of a region to be filled with a
 *  specific kind of flat. Think of it as an arbitrary boundary.
 *  
 *  These are refreshed continuously during rendering, and mark 
 *  the limits between flat regions. Special values mean "do not 
 *  render this column at all", while clipping out of the map bounds
 *  results in well-known bleeding effects.
 * 
 * @author admin
 *
 */

public class visplane_t{
    
    public static final int TOPOFFSET=1;
    public static final int MIDDLEPADDING=2;
    public static int BOTTOMOFFSET;
    public static final char SENTINEL=Character.MAX_VALUE;
    
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
protected static char[] clearvisplane;


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


// HACK: the resolution awareness is shared between all visplanes.
// Change this if you ever plan on running multiple renderers with
// different resolution or something.
protected static int SCREENWIDTH;
protected static int SCREENHEIGHT;
protected static IVideoScale vs;

public static void setVideoScale(IVideoScale vs) {
    visplane_t.vs=vs;
}

public static void initScaling() {
    SCREENHEIGHT=vs.getScreenHeight();
    SCREENWIDTH=vs.getScreenWidth();

    // Pre-scale stuff.
    BOTTOMOFFSET=SCREENWIDTH+TOPOFFSET+MIDDLEPADDING;

    if (clearvisplane==null || clearvisplane.length<SCREENWIDTH) {
        clearvisplane=new char[SCREENWIDTH];
        Arrays.fill(clearvisplane,Character.MAX_VALUE);
        }
}


};
