package rr;

public class Lights<V> {
	// Lighting constants.
	// Now why not 32 levels here?
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
	public static final int LIGHTLEVELS = 32, LIGHTSEGSHIFT = 3;

	// These are a bit more tricky to figure out though.

	public static final int MAXLIGHTSCALE = 48;
	public static final int LIGHTSCALESHIFT = 12;
	public static final int MAXLIGHTZ = 128;
	public static final int LIGHTZSHIFT = 20;

    public V[][] scalelight;
    public V[] scalelightfixed;
    public V[][] zlight;
    public V[] spritelights;

    // bumped light from gun blasts
    public int extralight;
}
