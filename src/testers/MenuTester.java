package testers;

import static data.Defines.KEY_F1;
import static data.Defines.PU_STATIC;

import i.InputListener;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JFrame;

import rr.patch_t;

import m.DoomMenu;
import m.Menu;
import m.random;
import utils.C2JUtils;
import v.BufferedRenderer;
import v.SimpleRenderer;
import w.DoomBuffer;
import w.WadLoader;
import data.Defines;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import doom.DoomContext;
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

    DoomStatus ds = new DoomStatus();
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
    CrappyDisplay frame = new CrappyDisplay(pals);
    frame.setTitle("MochaDoom");
    
    InputListener in = new InputListener();
    frame.addComponentListener(in);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    //frame.setUndecorated(true);
    frame.setVisible(true);
    frame.setBounds(frame.getX(), frame.getY(), WIDTH, 240);
    //V.takeScreenShot(0, "menutic19",icm);
    ds.menuactive=true;        
        for (int i=0;i<1500;i++){


            if (i==40){
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
            }
            //V.takeScreenShot(0,( "menutic"+i),icm);
            Thread.sleep(35);
        V.DrawPatch(0,0,0,help1);
        M.Ticker();
        M.Drawer();
        
        frame.update(null);
        System.out.print(frame.processEvents());
        }
            } catch (Exception e){
                e.printStackTrace();
            }
    
    }
    
}
