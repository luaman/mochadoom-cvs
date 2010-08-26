package testers;

import static data.Defines.PU_STATIC;
import static data.Defines.pw_allmap;
import static m.fixed_t.FRACBITS;
import hu.HU;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.nio.ByteBuffer;

import automap.DoomAutoMap;
import automap.Map;

import p.Playfield;
import p.mobj_t;

import m.FixedFloat;
import m.random;

import rr.vertex_t;
import st.StatusBar;

import data.Defines;
import data.doomstat;
import data.mapvertex_t;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import data.Defines.skill_t;
import doom.DoomContext;
import doom.event_t;
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
        
    doomstat ds = new doomstat();
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    ds.gamemode=GameMode_t.shareware;
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
    ds.players[0].weaponowned[0]=true;
    ds.players[0].weaponowned[1]=true;
    ds.players[0].weaponowned[2]=true;
    ds.players[0].weaponowned[3]=true;
    ds.players[0].readyweapon=weapontype_t.wp_pistol;
    ds.players[0].health=666;
    ds.players[0].armorpoints=666;
    ds.players[0].ammo[0]=666;
    ds.players[0].maxammo[0]=666;
    ds.players[0].ammo[1]=666;
    ds.players[0].maxammo[1]=666;
    ds.players[0].ammo[2]=666;
    ds.players[0].maxammo[2]=666;
    ds.players[0].ammo[3]=666;
    ds.players[0].maxammo[3]=666;

    
    ds.players[0].cards[0]=true;
    ds.players[0].cards[2]=true;
    ds.players[0].cards[4]=true;
    ds.players[0].mo=new mobj_t();
    ds.players[0].mo.x=1056<<FRACBITS;
    ds.players[0].mo.y=-3616<<FRACBITS;
    
    ds.players[0].powers[pw_allmap]=100;
    ds.deathmatch=false;
    ds.statusbaractive=true;
    
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

    StatusBar ST=new StatusBar(DC);
    ST.Start();
    Playfield PL=new Playfield(DC);
    PL.SetupLevel(1, 1, 0, skill_t.sk_hard);
    DC.P=PL;
    DC.ST=ST;
    DoomAutoMap AM=new Map(DC);
    AM.Start();
    
    ST.Responder(new event_t('i'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('t'));
    
    AM.Responder(new event_t(Map.AM_FOLLOWKEY));
    AM.Responder(new event_t(Map.AM_ZOOMINKEY));
    AM.Responder(new event_t(Map.AM_GRIDKEY));
    
    for (int i=0;i<100;i++){
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
    /*
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
        } */
       
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
