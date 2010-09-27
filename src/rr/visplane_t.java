package rr;
import static data.Defines.*;
import utils.C2JUtils;
//
//Now what is a visplane, anyway?
//

public class visplane_t{
    
    public static final int TOPOFFSET=1;
    public static final int MIDDLEPADDING=2;
    public static final int BOTTOMOFFSET=SCREENWIDTH+TOPOFFSET+MIDDLEPADDING;
    
    public visplane_t(){
        this.data=new byte[4+2*SCREENWIDTH];
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

byte data[];


protected static byte[] clearvisplane=new byte[SCREENWIDTH];


/** "Clear" the top with FF's.  */
public void clearTop(){
    System.arraycopy(clearvisplane, 0, this.data, TOPOFFSET, SCREENWIDTH);
      
}

/** "Clear" the bottom with FF's.  */
public void clearBottom(){
    System.arraycopy(clearvisplane, 0, this.data, BOTTOMOFFSET, SCREENWIDTH);
      
}

public void setTop(int index, byte value){
    this.data[TOPOFFSET+index]=value;
    
}

public int getTop(int index){
    return C2JUtils.toUnsignedByte(this.data[TOPOFFSET+index]);
    
}

public void setBottom(int index, byte value){
    this.data[BOTTOMOFFSET+index]=value;
    
}

public int getBottom(int index){
    return C2JUtils.toUnsignedByte(this.data[BOTTOMOFFSET+index]);
    
}

static{
    for (int i=0;i<clearvisplane.length;i++)
        clearvisplane[i]=(byte) 0xFF;
}

};
