package rr;

import static data.Tables.ANG180;
import static data.Tables.ANG270;
import static data.Tables.ANG90;
import static data.Tables.SlopeDiv;
import static data.Tables.tantoangle;
import utils.C2JUtils;
import doom.player_t;

public class ViewVars {
    
    // Found in draw_c. Only ever used in renderer.
    public int windowx;
    public int windowy;
    public int width;
    public int height;
    
    // MAES: outsiders have no business peeking into this.
    // Or...well..maybe they do. It's only used to center the "pause" X
    // position.
    // TODO: get rid of this?
    public int scaledwidth;
    public int centerx;
    public int centery;
    
    /** Used to determine the view center and projection in view units fixed_t */
    public int centerxfrac, centeryfrac, projection;

    /** fixed_t */
    protected int x, y, z;

    // MAES: an exception to strict type safety. These are used only in here,
    // anyway (?) and have no special functions.
    // Plus I must use them as indexes. angle_t
    protected long angle;

    /** fixed */
    protected int cos, sin;

    protected player_t player;

    /** Heretic/freeview stuff? */

	protected int lookdir;
    
    // 0 = high, 1 = low. Normally only the menu and the interface can change
    // that.
    protected int detailshift;
    
	protected int WEAPONADJUST;
	protected int BOBADJUST;
	
	/**
	 * constant arrays used for psprite clipping and initializing clipping
	 */
	protected short[] negonearray; // MAES: in scaling
	protected short[] screenheightarray;// MAES: in scaling
	
	protected final void initNegOneArray(int screenwidth){
	       C2JUtils.memset(negonearray, (short)-1,screenwidth);
		}
    
    protected final long PointToAngle(int x, int y) {
        // MAES: note how we don't use &BITS32 here. That is because
        // we know that the maximum possible value of tantoangle is angle
        // This way, we are actually working with vectors emanating
        // from our current position.
        x -= this.x;
        y -= this.y;

        if ((x == 0) && (y == 0))
            return 0;

        if (x >= 0) {
            // x >=0
            if (y >= 0) {
                // y>= 0

                if (x > y) {
                    // octant 0
                    return tantoangle[SlopeDiv(y, x)];
                } else {
                    // octant 1
                    return (ANG90 - 1 - tantoangle[SlopeDiv(x, y)]);
                }
            } else {
                // y<0
                y = -y;

                if (x > y) {
                    // octant 8
                    return (-tantoangle[SlopeDiv(y, x)]);
                } else {
                    // octant 7
                    return (ANG270 + tantoangle[SlopeDiv(x, y)]);
                }
            }
        } else {
            // x<0
            x = -x;

            if (y >= 0) {
                // y>= 0
                if (x > y) {
                    // octant 3
                    return (ANG180 - 1 - tantoangle[SlopeDiv(y, x)]);
                } else {
                    // octant 2
                    return (ANG90 + tantoangle[SlopeDiv(x, y)]);
                }
            } else {
                // y<0
                y = -y;

                if (x > y) {
                    // octant 4
                    return (ANG180 + tantoangle[SlopeDiv(y, x)]);
                } else {
                    // octant 5
                    return (ANG270 - 1 - tantoangle[SlopeDiv(x, y)]);
                }
            }
        }
        // This is actually unreachable.
        // return 0;
    }
    
}
