package testers;

import hu.HU;
import p.LevelLoader;
import rr.SimpleTextureManager;
import rr.TextureManager;
import s.DummySoundDriver;
import s.IDoomSound;
import utils.C2JUtils;
import w.WadLoader;
import defines.*;
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
    W.InitMultipleFiles(new String[] {"C:\\DOOMS\\doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    System.out.println("NUm for E1M1: "+W.GetNumForName("E1M1"));
    DoomStatus DS = new DoomStatus();
    DS.gameepisode=1;
    DS.gamemap=1;
    DS.gamemission=GameMission_t.doom;
    DS.gamemode=GameMode_t.shareware;
    IDoomSound S=new DummySoundDriver();            
    DS.S=S;
    DS.W=W;
    LevelLoader LL=new LevelLoader(DS);
    DS.LL=LL;
    TextureManager TM=new SimpleTextureManager(DS);
    DS.TM=TM;
    LL.updateStatus(DS);
    TM.InitFlats();
    TM.InitTextures();
    
    //HU hu=new HU(DS);
    //hu.Init();
    
    LL.SetupLevel(1, 1, 0, skill_t.sk_hard);
    
    
    
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
