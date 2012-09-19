package rr;

/**
 *   Lighting LUT.
 *   Used for z-depth cuing per column/row,
 *   and other lighting effects (sector ambient, flash).
 *   
 * @author velktron
 *
 * @param <V>
 */

public class Colormaps<V> {
    
    // For HiColor, this is, effectively, a 555 RGB palette.

    /** "peg" this to the one from RendererData */
    public V[] colormaps;

    /** lighttable_t** */
    public V[] walllights;

    /** Use in conjunction with pfixedcolormap */
    public V fixedcolormap;
    /** Use in conjunction with fixedcolormap[] */
    public int pfixedcolormap;
    // lighttable_t[][] walllights;
}
