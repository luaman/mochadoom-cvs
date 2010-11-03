package doom;

import f.Finale;
import f.Wiper;
import hu.HU;
import i.DoomNetworkInterface;
import i.DoomSystemInterface;
import i.DoomVideoInterface;
import m.DoomMenu;
import m.random;
import p.Actions;
import p.LevelLoader;
import rr.UnifiedRenderer;
import s.DoomSoundInterface;
import st.DoomStatusBarInterface;
import v.DoomVideoRenderer;
import w.EndLevel;
import w.WadLoader;
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
    public WadLoader W;
    public random RND;
    public DoomSystemInterface I;
    public DoomSoundInterface S;
    public DoomVideoInterface VI;
    public DoomStatusBarInterface ST;
    public DoomVideoRenderer V;
    public DoomNetworkInterface DNI;
    public DoomMain.DoomNet DN;    
    public LevelLoader LL;
    public DoomMenu M;
    public Actions P;
    public UnifiedRenderer R;
    public HU HU;
    public UnifiedRenderer RD;
    public DoomAutoMap AM;
    public Finale F;
    public EndLevel WI;
    public Wiper WIPE;
}
