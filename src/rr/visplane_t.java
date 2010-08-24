package rr;
import static data.Defines.*;
//
//Now what is a visplane, anyway?
//

public class visplane_t{
/** (fixed_t) */
public int       height;
public int           picnum;
public int           lightlevel;
public int           minx;
public int           maxx;

// leave pads for [minx-1]/[maxx+1]

public byte      pad1;
// Here lies the rub for all
//  dynamic resize/change of resolution.
public byte[]      top=new byte[SCREENWIDTH];
public byte      pad2;
public byte      pad3;
// See above.
public byte[]      bottom=new byte [SCREENWIDTH];
public byte      pad4;

protected static byte[] clearvisplane=new byte[SCREENWIDTH];


/** "Clear" the top with FF's.  */
public void clearTop(){
    System.arraycopy(clearvisplane, 0, this.top, 0, this.top.length);
      
}

/** "Clear" the bottom with FF's.  */
public void clearBottom(){
    System.arraycopy(clearvisplane, 0, this.top, 0, this.top.length);
      
}

static{
    for (int i=0;i<clearvisplane.length;i++)
        clearvisplane[i]=(byte) 0xFF;
}

};
