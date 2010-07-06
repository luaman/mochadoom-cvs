package doom;

import data.doomstat;
import m.random;
import rr.Renderer;
import v.DoomVideoRenderer;
import w.WadLoader;


/** Since a lot of stuff requires shared/global access to 
 *  the WadLoader, the Renderer, the Video system etc. and
 *  we're trying to depart from the global/static mentality,
 *  a common sharing is required.
 * 
 * @author admin
 *
 */

public class DoomContext {
    public doomstat DS;
    public WadLoader W;
    public Renderer R;
    public DoomVideoRenderer V;
    public random RND;
    
    
    
}
