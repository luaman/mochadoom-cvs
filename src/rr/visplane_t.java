package rr;

import m.fixed_t;
import static data.Defines.*;;
//
//Now what is a visplane, anyway?
//

public class visplane_t{
public fixed_t       height;
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

};
