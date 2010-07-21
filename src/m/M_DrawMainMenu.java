package m;

import static data.Defines.PU_CACHE;
import rr.patch_t;
import doom.DoomContext;
import v.DoomVideoRenderer;

/**
 *    
 // M_DrawMainMenu
 //
 * @author admin
 *
 */
public class M_DrawMainMenu implements DrawRoutine{

    private DoomContext DC;
    
    public M_DrawMainMenu(DoomContext DC){
        this.DC=DC;
    }
    
    @Override
    public void invoke() {
     DC.V.DrawPatchDirect (94,2,0,(patch_t)(DC.W.CacheLumpName("M_DOOM",PU_CACHE,patch_t.class)));
    }
}