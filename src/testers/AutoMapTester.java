package testers;

import static data.Defines.PU_STATIC;
import static data.Defines.pw_allmap;
import static m.fixed_t.FRACBITS;
import java.awt.image.IndexColorModel;
import automap.DoomAutoMap;
import automap.Map;

import p.LevelLoader;
import p.mobj_t;

import m.DoomRandom;

import st.StatusBar;

import data.Defines;
import defines.*;
import doom.DoomContext;
import doom.DoomMain;
import doom.DoomStatus;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;
import doom.weapontype_t;

import utils.C2JUtils;
import v.SimpleRenderer;
import w.*;

/** This is a very simple tester for the Automap. Combined with status bar + Level loader. */

public class AutoMapTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    SimpleRenderer V=new SimpleRenderer(320,200);
    V.Init();
    
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();
    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
        
    DoomMain DM=new DoomMain();
    DM.gameepisode=1;
    DM.gamemap=1;
    DM.gamemission=GameMission_t.doom;
    DM.gamemode=GameMode_t.shareware;
    DM.wminfo=new wbstartstruct_t();
    C2JUtils.initArrayOfObjects(DM.players,player_t.class);
    Defines.SCREENWIDTH=320;
    Defines.SCREENHEIGHT=200;
    
    DM.DM=(DoomMain) DM;
    DM.W=W;
    DM.V=V;
    DM.RND=new DoomRandom();
    DM.players[0].cmd=new ticcmd_t();
    DM.players[0].itemcount=1337;
    DM.players[0].killcount=1337;
    DM.players[0].secretcount=1337;
    DM.players[0].weaponowned[0]=true;
    DM.players[0].weaponowned[1]=true;
    DM.players[0].weaponowned[2]=true;
    DM.players[0].weaponowned[3]=true;
    DM.players[0].readyweapon=weapontype_t.wp_pistol;
    DM.players[0].health[0]=100;
    DM.players[0].armorpoints[0]=666;
    DM.players[0].ammo[0]=666;
    DM.players[0].maxammo[0]=666;
    DM.players[0].ammo[1]=666;
    DM.players[0].maxammo[1]=666;
    DM.players[0].ammo[2]=666;
    DM.players[0].maxammo[2]=666;
    DM.players[0].ammo[3]=666;
    DM.players[0].maxammo[3]=666;

    
    DM.players[0].cards[0]=true;
    DM.players[0].cards[2]=true;
    DM.players[0].cards[4]=true;
    DM.players[0].mo=new mobj_t();
    DM.players[0].mo.x=1056<<FRACBITS;
    DM.players[0].mo.y=-3616<<FRACBITS;
    
    DM.players[0].powers[pw_allmap]=100;
    DM.deathmatch=false;
    DM.statusbaractive=true;
    
    DM.wminfo.plyr[0].in=true;
    DM.wminfo.plyr[0].sitems=1337;
    DM.wminfo.plyr[0].skills=1337;
    DM.wminfo.plyr[0].stime=28595;
    DM.wminfo.plyr[0].ssecret=1337;
    DM.playeringame[0]=true;
    DM.wminfo.last=6;
    DM.wminfo.epsd=0;
    DM.wminfo.maxitems=100;
    DM.wminfo.maxkills=100;
    DM.wminfo.maxsecret=100;
    DM.wminfo.partime=28595;

    StatusBar ST=(StatusBar) (DM.ST=new StatusBar(DM));
    ST.Start();

    LevelLoader PL=new LevelLoader(DM);
    PL.SetupLevel(1, 1, 0, skill_t.sk_hard);
    DM.LL=PL;
    DM.ST=ST;
    DoomAutoMap AM=DM.AM=new Map(DM);
    AM.Start();
    
    ST.Responder(new event_t('i'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('t'));

    AM.Responder(new event_t(Map.AM_FOLLOWKEY));
    AM.Responder(new event_t(Map.AM_ZOOMINKEY));
    AM.Responder(new event_t(Map.AM_GRIDKEY));
	AM.updateStatus(DM);
	ST.updateStatus(DM);
    for (int i=0;i<100;i++){
    	DM.players[0].health[0]--;
    	DM.players[0].ammo[0]=i;
    	DM.players[0].damagecount=1; // if zero, it won't update.
        AM.Responder(new event_t(evtype_t.ev_keyup,Map.AM_MSGENTERED));
    if ((i%20)<10)
        AM.Responder(new event_t(Map.AM_ZOOMINKEY));
       else
       AM.Responder(new event_t(Map.AM_ZOOMOUTKEY));

    if ((i%25)<12) {
        AM.Responder(new event_t(Map.AM_PANUPKEY));
        AM.Responder(new event_t(Map.AM_PANRIGHTKEY));
    }
       else {
       AM.Responder(new event_t(Map.AM_PANDOWNKEY));
       AM.Responder(new event_t(Map.AM_PANLEFTKEY));
       }

    
    AM.Ticker();
    AM.Drawer();
    ST.Ticker();
    ST.Drawer(false,true);
    V.takeScreenShot(0, "tic"+i,icm);    
    //AM.Responder(new event_t(Map.AM_PANLEFTKEY));

    }
    
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
