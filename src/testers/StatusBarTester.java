package testers;

import static data.Defines.PU_STATIC;
import static m.fixed_t.FRACBITS;
import hu.HU;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.nio.ByteBuffer;

import p.Playfield;

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

/** This is a very simple tester for the End Level screen drawer.
 * 
 * 
 */

public class StatusBarTester {

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
    ds.players[0].weaponowned[0]=true;
    ds.players[0].weaponowned[1]=true;
    ds.players[0].weaponowned[2]=true;
    ds.players[0].weaponowned[3]=true;
    ds.players[0].readyweapon=weapontype_t.wp_pistol;
    ds.players[0].health=100;
    ds.players[0].armorpoints=600;
    ds.players[0].ammo[0]=200;
    ds.players[0].cards[0]=true;
    ds.players[0].cards[2]=true;
    ds.players[0].cards[4]=true;
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
   
    for (int i=0;i<20;i++){
    ST.Ticker();
    ST.Drawer(false,true);
    V.takeScreenShot(0, "tic"+i,icm);
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
