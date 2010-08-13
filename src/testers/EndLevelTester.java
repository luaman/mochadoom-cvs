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

import utils.C2JUtils;
import v.SimpleRenderer;
import w.*;

/** This is a very simple tester for the End Level screen drawer.
 * 
 * 
 */

public class EndLevelTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    SimpleRenderer V=new SimpleRenderer();
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
    
    DoomContext DC=new DoomContext();
    DC.DS=ds;
    DC.W=W;
    DC.V=V;
    DC.RND=new random();
    ds.players[0].cmd=new ticcmd_t();
    ds.playeringame[0]=true;
    EndLevel EL=new EndLevel(DC);
    EL.wbs.last=8;
    EL.wbs.epsd=0;
    
    EL.loadData();
    EL.initStats();
    for (int i=0;i<20;i++){
    EL.Ticker();
    EL.Drawer();
    }
    V.takeScreenShot(0, "tic1",icm);
    for (int i=1;i<200;i++){
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
