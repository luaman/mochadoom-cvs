package rr;


/** A vissprite_t is a thing
 * that will be drawn during a refresh.
 * I.e. a sprite object that is partly visible.
 */

public class vissprite_t {

// Doubly linked list.
public vissprite_t prev;
public vissprite_t next;

public int         x1;
public int         x2;

// for line side calculation
public int     gx;
public int     gy;     

// global bottom / top for silhouette clipping
public int     gz;
public int     gzt;

// horizontal position of x1
public int     startfrac;

public int     scale;

// negative if flipped
public int     xiscale;    

public int     texturemid;
public int         patch;

/** for color translation and shadow draw,
 * maxbright frames as well.
 * 
 * Use paired with colormapO;
 */ 
public byte[]   colormap;

/** pointer into colormap */
public int pcolormap;

public int         mobjflags;

}