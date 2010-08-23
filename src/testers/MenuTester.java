package testers;

import static data.Defines.KEY_F1;
import static data.Defines.PU_STATIC;

import java.awt.image.IndexColorModel;

import m.DoomMenu;
import m.Menu;
import m.random;
import utils.C2JUtils;
import v.SimpleRenderer;
import w.DoomBuffer;
import w.WadLoader;
import data.Defines;
import data.doomstat;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import doom.DoomContext;
import doom.event_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;

/** This is a very simple tester for Menu module  */

public class MenuTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    System.out.println("NUm for E1M1: "+W.GetNumForName("E1M1"));
   int lump=W.GetNumForName("VERTEXES");
   System.out.println("NUm for VERTEXES: "+W.GetNumForName("VERTEXES"));
    doomstat ds = new doomstat();
    
    SimpleRenderer V=new SimpleRenderer();
    V.Init();
    
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();
    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
        
    DoomContext DC=new DoomContext();
    DC.DS=ds;
    DC.W=W;
    DC.V=V;
    
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    ds.gamemode=GameMode_t.shareware;
    ds.wminfo=new wbstartstruct_t();
    C2JUtils.initArrayOfObjects(ds.players,player_t.class);
    
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
   
    DoomMenu M=new Menu(DC);
    M.Init();
   
    
    for (int i=0;i<20;i++){
        M.Ticker();
        M.Drawer();
        }
   
    V.takeScreenShot(0, "menutic19",icm);
    ds.menuactive=true;        
        for (int i=20;i<150;i++){
            M.Ticker();
            M.Drawer();

            if (i==40){
                M.Responder(new event_t(Defines.KEY_DOWNARROW));
            }

            if (i==60){
                M.Responder(new event_t(Defines.KEY_DOWNARROW));
            }
            
            if (i==80){
                M.Responder(new event_t(Defines.KEY_ESCAPE));
            }
            
            if (i==100){
                M.Responder(new event_t(KEY_F1));
            }
            
            if (i==120){
                M.Responder(new event_t(Defines.KEY_ESCAPE));
            }
            V.takeScreenShot(0,( "menutic"+i),icm);
            }
           
            } catch (Exception e){
                e.printStackTrace();
            }
    
    }
    
}
