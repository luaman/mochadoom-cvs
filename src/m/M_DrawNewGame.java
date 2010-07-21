package m;

import rr.patch_t;
import doom.DoomContext;
import v.DoomVideoRenderer;
import w.WadLoader;
import static data.Defines.*;
import static m.Menu.*;

/** M_DrawEpisode
 * 
 * @author MAES
 *
 */

public class M_DrawNewGame
        implements DrawRoutine {

    DoomVideoRenderer V;
    WadLoader W;
    Menu M;
    
    public M_DrawNewGame(DoomContext DC, Menu M){
    this.V=DC.V;
    this.W=DC.W;
    this.M=M;
    
    }
    
    @Override
    public void invoke() {
            V.DrawPatchDirect (96,14,0,(patch_t) W.CacheLumpName("M_NEWG",PU_CACHE,patch_t.class));
            V.DrawPatchDirect (54,38,0,(patch_t) W.CacheLumpName("M_SKILL",PU_CACHE,patch_t.class));
        }
    }
