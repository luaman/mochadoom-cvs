package testers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import p.LevelLoader;

import rr.SimpleTextureManager;
import rr.TextureManager;
import s.DummySoundDriver;
import s.IDoomSound;
import savegame.VanillaDSG;
import savegame.VanillaDSGHeader;

import m.MenuMisc;
import defines.GameMission_t;
import defines.GameMode_t;
import defines.skill_t;
import demo.VanillaDoomDemo;
import doom.DoomMain;
import doom.DoomStatus;
import w.*;

public class SaveGameHeaderTester {

    public static void main(String[] argv) throws Exception {

    ByteBuffer buf=MenuMisc.ReadFile("doomsav0.dsg");
    DoomFile f=new DoomFile("doomsav0.dsg","r");
    DoomStatus DS=new DoomStatus();
    
    VanillaDSGHeader vdsg=new VanillaDSGHeader();
    try {
        vdsg.read(f);
    
    
    f.seek(0);
    
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"C:\\DOOMS\\doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    System.out.println("NUm for E1M1: "+W.GetNumForName("E1M1"));
    DS.gameepisode=1;
    DS.gamemap=1;
    DS.gamemission=GameMission_t.doom;
    DS.setGameMode(GameMode_t.shareware);
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

    
    VanillaDSG DSG=new VanillaDSG();
    DSG.updateStatus(DS);    
    
    DS.playeringame[0]=true;
    DS.players[0].updateStatus(DS);
    DSG.doLoad(f);
    
    
    //    W.AddFile("bitter.wad");

    System.out.println(vdsg);
    
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    }
    
}
