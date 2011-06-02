package testers;

import static data.Defines.PU_STATIC;

import i.IDoomSystem;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import awt.OldAWTDoom;

import m.DoomRandom;
import s.IDoomSound;
import s.DummySoundDriver;
import utils.C2JUtils;
import v.BufferedRenderer;
import w.DoomBuffer;
import w.WadLoader;
import data.Defines;
import defines.*;
import doom.DoomMain;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;
import f.EndLevel;

/** This is a very simple tester for the End Level screen drawer.
 *  MAES: this is mostly historical. Too many changes have occured
 *  and it's no longer easy to operate stand-alone.
 *  
 *  Edit: well, maybe it is...
 * 
 */

public class EndLevelTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();

    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
    BufferedRenderer V=new BufferedRenderer(Defines.SCREENWIDTH,Defines.SCREENHEIGHT,icm);
    V.Init();
    
    IndexColorModel[] icms=new IndexColorModel[pal.length/768];
    BufferedImage[] pals=new BufferedImage[icms.length];
    
    for (int i=0;i<icms.length;i++){
        icms[i]=new IndexColorModel(8, 256,pal, i*768, false);
            pals[i]=new BufferedImage(icms[i],V.screenbuffer[0].getRaster(), false, null);
           }
    
    DoomMain DC=new DoomMain();
    DC.DM=DC;
    IDoomSystem I=new i.DoomSystem();
    IDoomSound S=new DummySoundDriver();
    
    DC.S=S;
    DC.I=I;
    DC.gameepisode=1;
    DC.gamemap=1;
    DC.gamemission=GameMission_t.doom;
    DC.gamemode=GameMode_t.shareware;
    DC.wminfo=new wbstartstruct_t();
    C2JUtils.initArrayOfObjects(DC.players,player_t.class);

    
    DC.DM=DC;
    DC.W=W;
    DC.V=V;
    DC.RND=new DoomRandom();
    DC.players[0].cmd=new ticcmd_t();
    DC.players[0].itemcount=1337;
    DC.players[0].killcount=1337;
    DC.players[0].secretcount=1337;
    
    DC.wminfo.plyr[0].in=true;
    DC.wminfo.plyr[0].sitems=1337;
    DC.wminfo.plyr[0].skills=1337;
    DC.wminfo.plyr[0].stime=28595;
    DC.wminfo.plyr[0].ssecret=1337;
    DC.playeringame[0]=true;
    DC.wminfo.last=6;
    DC.wminfo.epsd=0;
    DC.wminfo.maxitems=100;
    DC.wminfo.maxkills=100;
    DC.wminfo.maxsecret=100;
    DC.wminfo.partime=28595;
    //JFrame frame = new JFrame("MochaDoom");
    OldAWTDoom shit = new OldAWTDoom(DC, V, pal);
    shit.InitGraphics();
    
    //frame.setBounds(frame.getX(), frame.getY(), WIDTH, 240);
    EndLevel EL=new EndLevel(DC);
    

   // EL.Start(wbstartstruct);
    int a,b;
    a=I.GetTime();
    b=a;
    for (int i=0;i<2000;i++){
    	
    EL.Ticker();
    EL.Drawer();
    shit.update(shit.getGraphics());
    if (i==100){
            DC.players[0].cmd.buttons=1; // simulate attack
            DC.players[0].attackdown=false; // simulate attack
        }
        
        if (i==120){
            DC.players[0].cmd.buttons=1; // simulate attack
            DC.players[0].attackdown=false; // simulate attack
        }
   // Do we still have time>
        
        while((b-a)==0) {
        	b=I.GetTime();
        	}
       a=b;
    }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
