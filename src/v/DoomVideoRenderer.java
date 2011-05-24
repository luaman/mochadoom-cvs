package v;

import java.awt.image.IndexColorModel;
import java.io.IOException;

import rr.patch_t;

/** DoomVideoSystem is now an interface, that all "video drivers" (wheter do screen, disk, etc.)
 *  must implement.
 * 
 * @author Maes
 *
 */

public interface DoomVideoRenderer extends IVideoScaleAware {
    
    //flags hacked in scrn (not supported by all functions (see src))
    // Added by _D_. Unsure if I should use VSI objects instead, as they
    // already carry scaling information which doesn't need to be repacked...
    public static final int V_NOSCALESTART =      0x010000;   // dont scale x,y, start coords
    public static final int V_SCALESTART        = 0x020000;   // scale x,y, start coords
    public static final int V_SCALEPATCH        = 0x040000;   // scale patch
    public static final int V_NOSCALEPATCH      = 0x080000;   // don't scale patch
    public static final int V_WHITEMAP          = 0x100000;   // draw white (for v_drawstring)
    public static final int V_FLIPPEDPATCH      = 0x200000;   // flipped in y
    public static final int V_TRANSLUCENTPATCH  = 0x400000;   // draw patch translucent    
    
    // Allocates buffer screens, call before R_Init.
    public void Init();

    public void CopyRect(int srcx, int srcy, int srcscrn, int width,
            int height, int destx, int desty, int destscrn);

    public void DrawPatch(int x, int y, int scrn, patch_t patch);
    
    public void DrawPatchFlipped ( int      x,   int        y,    int       scrn,  patch_t  patch );

    public void DrawPatchDirect(int x, int y, int scrn, patch_t patch);

    /** V_DrawPatch
     * Draws a SOLID (non-masked) patch to the screen with integer scaling
     * m and n.
     * Useful for stuff such as help screens, titlepic and status bar. Not 
     * very useful for menus, though.
     * desttop, dest and source were byte
     */ 

    public void DrawPatchSolidScaled ( int x, int y,int m, int n, int scrn, patch_t patch );
    
    // Draw a linear block of pixels into the view buffer.
    public void DrawBlock(int x, int y, int scrn, int width, int height,
            byte[] src);

    // Reads a linear block of pixels into the view buffer.
    public void GetBlock(int x, int y, int scrn, int width, int height,
            byte[] dest);

    public void MarkRect(int x, int y, int width, int height);

    public byte[] getScreen(int index);
    
    public void setScreen(int index, int width, int height);
    
    public int getUsegamma();

    public void setUsegamma(int gamma);
    
    public void takeScreenShot(int screen, String imagefile, IndexColorModel icm) throws IOException;
    
    public int getWidth();
    
    public int getHeight();
    
    /** Shamelessly ripped from Doom Legacy (for menus, etc) by _D_ ;-)
     * It uses FLAGS (see above) hacked into the scrn parameter, to be
     * parsed afterwards.
     */
    public void DrawScaledPatch(int x, int y, int scrn, IVideoScale VSI,  // hacked flags in it...
            patch_t patch);
    
}
