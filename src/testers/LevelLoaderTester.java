package testers;

import hu.HU;
import p.LevelLoader;
import utils.C2JUtils;
import w.WadLoader;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import data.Defines.skill_t;
import doom.DoomContext;
import doom.DoomStatus;
import doom.player_t;
import doom.wbstartstruct_t;

/** This is a very simple tester for the WadLoader and HU modules.
 *  We use the same exact methods used in the C source code, only
 *  with a more OO approach.
 * 
 * 
 */

public class LevelLoaderTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    System.out.println("NUm for E1M1: "+W.GetNumForName("E1M1"));
   int lump=W.GetNumForName("VERTEXES");
   System.out.println("NUm for VERTEXES: "+W.GetNumForName("VERTEXES"));
    DoomStatus ds = new DoomStatus();
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    ds.gamemode=GameMode_t.shareware;
    ds.wminfo=new wbstartstruct_t();
    C2JUtils.initArrayOfObjects(ds.players,player_t.class);
    
    DoomContext DC=new DoomContext();
    DC.DS=ds;
    DC.W=W;
    
    LevelLoader PF=new LevelLoader(DC);
    
    HU hu=new HU(DC);
    hu.Init();
    
    PF.SetupLevel(1, 1, 0, skill_t.sk_hard);
    
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
