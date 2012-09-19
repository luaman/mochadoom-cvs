package doom;

import n.DoomSystemNetworking;
import f.EndLevel;
import f.Finale;
import f.Wiper;
import hu.HU;
import i.IDiskDrawer;
import i.IDoomSystem;
import i.DoomVideoInterface;
import m.IDoomMenu;
import m.IRandom;
import m.IVariablesManager;
import p.AbstractLevelLoader;
import p.Actions;
import rr.ISpriteManager;
import rr.Renderer;
import rr.TextureManager;
import s.IDoomSound;
import s.IMusic;
import s.ISound;
import st.AbstractStatusBar;
import timing.ITicker;
import v.DoomVideoRenderer;
import w.IWadLoader;
import automap.IAutoMap;

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

public class DoomContext<T,V>{
	
    public DoomMain DM;
    public IDoomGame DG;
    public IWadLoader W;
    public IRandom RND;
    public IDoomSystem I;
    public IDoomSound S;
    public ISound ISND;
    public IMusic IMUS;
    public DoomVideoInterface<V> VI;
    public AbstractStatusBar ST;
    public DoomVideoRenderer<V> V;
    public DoomSystemNetworking DNI;
    public IDoomGameNetworking DGN;
    public AbstractLevelLoader LL;
    public IDoomMenu M;
    public Actions P;
    public Renderer<T,V> R;
    public HU HU;
    public IAutoMap AM;
    public Finale<V> F;
    public EndLevel WI;
    public Wiper WIPE;
    public TextureManager TM;
    public ISpriteManager SM;
    public ICommandLineManager CM;
    public ITicker TICK; 
    public IDiskDrawer DD;
    public IVariablesManager VM;
}
