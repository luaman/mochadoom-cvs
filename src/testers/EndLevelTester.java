package testers;

import static data.Defines.PU_STATIC;

import java.awt.image.IndexColorModel;

import m.random;
import utils.C2JUtils;
import v.SimpleRenderer;
import w.DoomBuffer;
import w.EndLevel;
import w.WadLoader;
import data.Defines;
import data.doomstat;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import doom.DoomContext;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;

/** This is a very simple tester for the End Level screen drawer.
 * 
 * 
 */

public class EndLevelTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"C:\\games\\iwads\\doom2.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    SimpleRenderer V=new SimpleRenderer(320,200);
    V.Init();
    
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();
    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
        
    doomstat ds = new doomstat();
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    ds.gamemode=GameMode_t.commercial;
    ds.wminfo=new wbstartstruct_t();
    C2JUtils.initArrayOfObjects(ds.players,player_t.class);
    Defines.SCREENWIDTH=320;
    Defines.SCREENHEIGHT=200;
    
    DoomContext DC=new DoomContext();
    DC.DS=ds;
    DC.W=W;
    DC.V=V;
    DC.RND=new random();
    ds.players[0].cmd=new ticcmd_t();
    ds.players[0].itemcount=1337;
    ds.players[0].killcount=1337;
    ds.players[0].secretcount=1337;
    
    ds.wminfo.plyr[0].in=true;
    ds.wminfo.plyr[0].sitems=1337;
    ds.wminfo.plyr[0].skills=1337;
    ds.wminfo.plyr[0].stime=28595;
    ds.wminfo.plyr[0].ssecret=1337;
    ds.playeringame[0]=true;
    ds.wminfo.last=6;
    ds.wminfo.epsd=0;
    ds.wminfo.maxitems=100;
    ds.wminfo.maxkills=100;
    ds.wminfo.maxsecret=100;
    ds.wminfo.partime=28595;

    EndLevel EL=new EndLevel(DC);
    

   // EL.Start(wbstartstruct);
    
    for (int i=0;i<20;i++){
    EL.Ticker();
    EL.Drawer();
    }
    V.takeScreenShot(0, "tic1",icm);
    for (int i=20;i<150;i++){
        EL.Ticker();
        EL.Drawer();
        if (i==100){
            ds.players[0].cmd.buttons=1; // simulate attack
            ds.players[0].attackdown=false; // simulate attack
        }
        
        if (i==120){
            ds.players[0].cmd.buttons=1; // simulate attack
            ds.players[0].attackdown=false; // simulate attack
        }
        V.takeScreenShot(0,( "tic"+i),icm);
        }
       
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
