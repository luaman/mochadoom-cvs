package rr;

/**
 *   Combined colormap and light LUTs.
 *   Used for z-depth cuing per column/row,
 *   and other lighting effects (sector ambient, flash).
 *   
 * @author velktron
 *
 * @param <V>
 */

public class LightsAndColors<V> {
    
    // For HiColor, this is, effectively, a 555 RGB palette.

    /** "peg" this to the one from RendererData */
    public V[] colormaps;

    /** lighttable_t** */
    public V[] walllights;

    /** Use in conjunction with pfixedcolormap */
    public V fixedcolormap;
    /** Use in conjunction with fixedcolormap[] */
    public int pfixedcolormap;
	
	/**
	 * Color tables for different players, translate a limited part to another
	 * (color ramps used for suit colors).
	 */

	public byte[][] translationtables;
	
	
	/** Bits representing color levels. 5 for 32. */
	public static final int LBITS= 8;
	
    /**
     * These two are tied by an inverse relationship. E.g. 256 levels, 0 shift
     * 128 levels, 1 shift ...etc... 16 levels, 4 shift (default). Or even less,
     * if you want.
     * 
     * By setting it to the max however you get smoother light and get rid of
     * lightsegshift globally, too. Of course, by increasing the number of light
     * levels, you also put more memory pressure, and due to their being only
     * 256 colors to begin with, visually, there won't be many differences.
     */
	
	 
    public static final int LIGHTLEVELS = 1<<LBITS, LIGHTSEGSHIFT = 8-LBITS;

  /** Number of diminishing brightness levels.
     There a 0-31, i.e. 32 LUT in the COLORMAP lump. 
     TODO: how can those be distinct from the light levels???
     */    
    
  public static final int  NUMCOLORMAPS   =     LIGHTLEVELS;

    
    // These are a bit more tricky to figure out though.

    /** Maximum index used for light levels of sprites. In practice,
     *  it's capped by the number of light levels???
     *  
     *  Normally set to 48 (32 +16???)
     */
    
    public static final int MAXLIGHTSCALE = LIGHTLEVELS+32;
    
    /** Used to scale brightness of walls and sprites. Their "scale" is shifted by
     *  this amount, and this results in an index, which is capped by MAXLIGHTSCALE.
     *  Normally it's 12 for 32 levels, so 11 for 64, 10 for 128, ans 9 for 256.
     *  
     */
    public static final int LIGHTSCALESHIFT = 17-LBITS;
    
    /** This one seems arbitrary. Will auto-fit to 128 possible levels? */
    public static final int MAXLIGHTZ = 128;
    
    /** Normally 20, applied to distance. Assuming the maximum is 32K units
     * (31 usable bits), then a shift of 20 will leave you with 15. This is further
     * capped to 128.
     *  
     */
    public static final int LIGHTZSHIFT = 17;

    public V[][] scalelight;
    public V[] scalelightfixed;
    public V[][] zlight;
    public V[] spritelights;

    // bumped light from gun blasts
    public int extralight;
}
