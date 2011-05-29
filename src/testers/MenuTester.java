package testers;

import static data.Defines.KEY_F1;
import static data.Defines.PU_STATIC;

import hu.HU;
import i.InputListener;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JFrame;

import rr.patch_t;

import m.IDoomMenu;
import m.Menu;
import m.DoomRandom;
import utils.C2JUtils;
import v.BufferedRenderer;
import v.SimpleRenderer;
import w.DoomBuffer;
import w.WadLoader;
import data.Defines;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import data.Defines.Language_t;
import doom.DoomContext;
import doom.DoomMain;
import doom.DoomStatus;
import doom.event_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;

/** This is a very simple tester for Menu module  */

public class MenuTester {
    public static final int WIDTH=320;
    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);

    patch_t help1=W.CachePatchName("TITLEPIC", PU_STATIC);
    
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();

    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
    Defines.SCREENWIDTH=320;
    Defines.SCREENHEIGHT=200;
    BufferedRenderer V=new BufferedRenderer(320,200,icm);
    V.Init();

    IndexColorModel[] icms=new IndexColorModel[palette.getBuffer().limit()/768];
    BufferedImage[] pals=new BufferedImage[icms.length];
    for (int i=0;i<icms.length;i++){
        icms[i]=new IndexColorModel(8, 256,pal, i*768, false);
            pals[i]=new BufferedImage(icms[i],V.screenbuffer[0].getRaster(), false, null);
           }
        
    DoomMain DM=new DoomMain();


    DM.W=W;
    DM.V=V;
    HU HU=new HU(DM);
    HU.Init();
    DM.HU=HU;
    DM.gameepisode=1;
    DM.gamemap=1;
    DM.gamemission=GameMission_t.doom;
    DM.gamemode=GameMode_t.shareware;
    DM.wminfo=new wbstartstruct_t();
    DM.usergame=true;
    C2JUtils.initArrayOfObjects(DM.players,player_t.class);
    
    DM.RND=new DoomRandom();
    DM.players[0].cmd=new ticcmd_t();
    DM.players[0].itemcount=1337;
    DM.players[0].killcount=1337;
    DM.players[0].secretcount=1337;
    
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
   
    IDoomMenu M=DM.M=new Menu(DM);
    DM.language=Language_t.english;
    M.Init();
    CrappyDisplay frame = new CrappyDisplay(pals);
    frame.setTitle("MochaDoom");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    //frame.setUndecorated(true);
    frame.setVisible(true);
    frame.setBounds(frame.getX(), frame.getY(), WIDTH, 240);
    
    DM.menuactive=true;        
        for (int i=0;i<10000;i++){
            int a=DM.I.GetTime();
            while (a-DM.I.GetTime()==0){
                frame.setVisible(true);
                Thread.sleep(1);               
            }
            
           
            
            event_t ev=CrappyDisplay.nextEvent();
            //System.out.println(ev);
            if (ev!=null)
            ((Menu)M).Responder(ev);
            
          /*  if (i==40){
            	System.out.println("Pressing enter");
                M.Responder(new event_t(Defines.KEY_DOWNARROW));
            }

            if (i==60){
            	System.out.println("Pressing down");
                M.Responder(new event_t(Defines.KEY_DOWNARROW));
            }
            
            if (i==80){
            	System.out.println("Pressing escape");
                M.Responder(new event_t(Defines.KEY_ESCAPE));
            }
            if (i==100){
            	System.out.println("Pressing up");
                M.Responder(new event_t(Defines.KEY_UPARROW));
            }
            
            if (i==120){
            	System.out.println("Pressing up");
                M.Responder(new event_t(Defines.KEY_UPARROW));
            }
            
            if (i==140){
            	System.out.println("Pressing escape");
                M.Responder(new event_t(Defines.KEY_ESCAPE));
            }
            
            if (i==160){
            	System.out.println("Pressing up");
                M.Responder(new event_t(Defines.KEY_UPARROW));
            }
            
            if (i==160){
            	System.out.println("Pressing up");
                M.Responder(new event_t(Defines.KEY_UPARROW));
            }
            
            if (i==300 || i==500|i==550|i==600){
            	System.out.println("Pressing F1");
                M.Responder(new event_t(KEY_F1));
                System.out.println("pressed ");
            }
            
            if (i==400 || i==650){
            	System.out.println("Pressing escape");
                M.Responder(new event_t(Defines.KEY_ESCAPE));
            } */
            
            //V.takeScreenShot(0,( "menutic"+i),icm);
         
        V.DrawPatch(0,0,0,help1);
        M.Ticker();
        M.Drawer();
        DM.gametic++;
        frame.update(null);
        System.out.print(frame.processEvents());
        }
            } catch (Exception e){
                e.printStackTrace();
            }
    
    }
    
}
