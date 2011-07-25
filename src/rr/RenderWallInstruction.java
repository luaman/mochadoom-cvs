package rr;

/** This is all the information needed to draw a particular column. Really.
 *  So if we store all of this crap somewhere instead of drawing, we can do the
 *  drawing when it's more convenient, and since they are non-overlapping we can 
 *  parallelize them. Any questions?
 *  
 */

public class RenderWallInstruction {    
    int centery;
    int dc_iscale;
    //int dc_source_ofs;
    int dc_texturemid;
    int dc_texheight; // Boom enhancement
    int dc_x;
    int dc_yh;
    int dc_yl;
    //int[] columnofs;
    byte[] dc_colormap;
    byte[] dc_source;
}
