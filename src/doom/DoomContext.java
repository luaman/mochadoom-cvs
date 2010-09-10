package doom;

import p.LevelLoader;
import i.SystemInterface;
import data.doomstat;
import m.DoomMenu;
import m.random;
import rr.DoomRenderingContext;
import rr.Renderer;
import s.DoomSoundInterface;
import st.DoomStatusBarInterface;
import v.DoomVideoRenderer;
import w.WadLoader;


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
    public doomstat DS;
    public WadLoader W;
    public DoomRenderingContext DRC;
    public random RND;
    public SystemInterface I;
    public DoomSoundInterface S;
    public DoomStatusBarInterface ST;
    public DoomVideoRenderer V;
    public LevelLoader LL;
    public DoomMenu M;
}
