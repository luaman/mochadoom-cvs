package v;

import rr.patch_t;

/** DoomVideoSystem is now an interface, that all "video drivers" (wheter do screen, disk, etc.)
 *  must implement.
 * 
 * @author Maes
 *
 */

public interface DoomVideoSystem {
    // Allocates buffer screens, call before R_Init.
    public void Init();

    public void CopyRect(int srcx, int srcy, int srcscrn, int width,
            int height, int destx, int desty, int destscrn);

    public void DrawPatch(int x, int y, int scrn, patch_t patch);
    
    public void DrawPatchFlipped ( int      x,   int        y,    int       scrn,  patch_t  patch );

    public void DrawPatchDirect(int x, int y, int scrn, patch_t patch);

    // Draw a linear block of pixels into the view buffer.
    public void DrawBlock(int x, int y, int scrn, int width, int height,
            byte[] src);

    // Reads a linear block of pixels into the view buffer.
    public void GetBlock(int x, int y, int scrn, int width, int height,
            byte[] dest);

    public void MarkRect(int x, int y, int width, int height);

}
