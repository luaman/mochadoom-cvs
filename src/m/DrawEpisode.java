package m;

import rr.patch_t;
import doom.DoomContext;
import v.DoomVideoRenderer;
import w.WadLoader;
import static data.Defines.*;

/** M_DrawEpisode
 * 
 * @author MAES
 *
 */

public class DrawEpisode
        implements DrawRoutine {

    DoomVideoRenderer V;
    WadLoader W;
    
    public DrawEpisode(DoomContext DC){
    this.V=DC.V;
    this.W=DC.W;
    
    }
    
    @Override
    public void invoke() {
            V.DrawPatchDirect (54,38,0,(patch_t)W.CacheLumpName("M_EPISOD",PU_CACHE,patch_t.class));
        }


    }
