package testers;

import static data.Defines.KEY_F1;
import static data.Defines.PU_STATIC;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JFrame;

import m.IDoomMenu;
import m.Menu;
import m.DoomRandom;
import utils.C2JUtils;
import v.BufferedRenderer;
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

/** This is a very simple tester for the End Level screen drawer.
 * 
 * 
 */

public class MenuVideoTester {

    public static final int WIDTH=320;
    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();

    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
    Defines.SCREENWIDTH=WIDTH;
    Defines.SCREENHEIGHT=200;
    BufferedRenderer V=new BufferedRenderer(WIDTH,200,icm);
    V.Init();
    
    IndexColorModel[] icms=new IndexColorModel[pal.length/768];
    BufferedImage[] pals=new BufferedImage[icms.length];
    
    for (int i=0;i<icms.length;i++){
        icms[i]=new IndexColorModel(8, 256,pal, i*768, false);
            pals[i]=new BufferedImage(icms[i],V.screenbuffer[0].getRaster(), false, null);
           }
    
    DoomStatus ds = new DoomStatus();
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
    DC.RND=new DoomRandom();
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
    JFrame frame = new JFrame("MochaDoom");
    CrappyDisplay shit = new CrappyDisplay(pals);
    frame.add(shit);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    //frame.setUndecorated(true);
    frame.setVisible(true);


    frame.setBounds(frame.getX(), frame.getY(), WIDTH, 240);
    IDoomMenu M=new Menu(DC);
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
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
