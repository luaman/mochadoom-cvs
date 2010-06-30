package rr;

import m.fixed_t;


//A vissprite_t is a thing
//that will be drawn during a refresh.
//I.e. a sprite object that is partly visible.
public class vissprite_t {

// Doubly linked list.
public vissprite_t prev;
public vissprite_t next;

public int         x1;
public int         x2;

// for line side calculation
public fixed_t     gx;
public fixed_t     gy;     

// global bottom / top for silhouette clipping
public fixed_t     gz;
public fixed_t     gzt;

// horizontal position of x1
public fixed_t     startfrac;

public fixed_t     scale;

// negative if flipped
public fixed_t     xiscale;    

public fixed_t     texturemid;
public int         patch;

// for color translation and shadow draw,
//  maxbright frames as well MAES: was *, make []?
// 
// 
public lighttable_t[]   colormap;

public int         mobjflags;

}