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

public class M_DrawOptions
        implements DrawRoutine {

    DoomVideoRenderer V;
    WadLoader W;
    Menu M;
    
    public M_DrawOptions(DoomContext DC, Menu M){
    this.V=DC.V;
    this.W=DC.W;
    this.M=M;
    
    }
    
 private static String    detailNames[]   = {"M_GDHIGH","M_GDLOW"};
 private  static String  msgNames[]      = {"M_MSGOFF","M_MSGON"};

    
    
    @Override
    public void invoke() {
        menu_t OptionsDef =M.OptionsDef;
        
        V.DrawPatchDirect (108,15,0,(patch_t) W.CacheLumpName("M_OPTTTL",PU_CACHE,patch_t.class));
        
        V.DrawPatchDirect (OptionsDef.x + 175,OptionsDef.y+LINEHEIGHT*detail,0,
                   (patch_t) W.CacheLumpName(detailNames[M.detailLevel],PU_CACHE,patch_t.class));

        V.DrawPatchDirect (OptionsDef.x + 120,OptionsDef.y+LINEHEIGHT*messages,0,
                   (patch_t) W.CacheLumpName(msgNames[M.showMessages],PU_CACHE,patch_t.class));

        M.DrawThermo(OptionsDef.x,OptionsDef.y+LINEHEIGHT*(mousesens+1),
             10,M.mouseSensitivity);
        
        M.DrawThermo(OptionsDef.x,OptionsDef.y+LINEHEIGHT*(scrnsize+1),
             9,M.screenSize);

        }


    }
