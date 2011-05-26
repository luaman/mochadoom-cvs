package doom;

import n.DoomSystemNetworking;
import f.Finale;
import f.Wiper;
import hu.HU;
import i.IDoomSystem;
import i.DoomVideoInterface;
import m.IDoomMenu;
import m.random;
import p.Actions;
import p.LevelLoader;
import rr.RendererState;
import rr.SpriteManager;
import rr.TextureManager;
import rr.UnifiedRenderer;
import s.IDoomSound;
import st.IDoomStatusBar;
import timing.ITicker;
import v.DoomVideoRenderer;
import w.EndLevel;
import w.IWadLoader;
import automap.DoomAutoMap;


/** Since a lot of stuff requires shared/global access to 
 *  the WadLoader, the Renderer, the Video system etc. and
 *  we're trying to depart from the global/static mentality,
 *  a common sharing is required. Ideally, this would be a perfect
 *  example of where multiple inheritance could be adopted, since most
 *  stuff needs to share this status anyway. The next best thing is
 *  to have local references of any used fields in the classes that use them.
 * 
 * @author admin
 *
 */

public class DoomContext {
	
    public DoomMain DM;
    public IDoomGame DG;
    public IWadLoader W;
    public random RND;
    public IDoomSystem I;
    public IDoomSound S;
    public DoomVideoInterface VI;
    public IDoomStatusBar ST;
    public DoomVideoRenderer V;
    public DoomSystemNetworking DNI;
    public IDoomGameNetworking DGN;
    public LevelLoader LL;
    public IDoomMenu M;
    public Actions P;
    public RendererState R;
    public HU HU;
    public UnifiedRenderer RD;
    public DoomAutoMap AM;
    public Finale F;
    public EndLevel WI;
    public Wiper WIPE;
    public TextureManager TM;
    public SpriteManager SM;
    public ICommandLineManager CM;
    public ITicker TICK;
}
