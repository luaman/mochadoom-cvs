package testers;

import static data.Defines.*;
import static data.Limits.*;
import static m.fixed_t.FRACBITS;

import i.InputListener;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JFrame;

import m.DoomRandom;
import p.LevelLoader;
import p.mobj_t;
import st.StatusBar;
import utils.C2JUtils;
import v.BufferedRenderer;
import w.DoomBuffer;
import w.WadLoader;
import automap.DoomAutoMap;
import automap.Map;
import data.Defines;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import data.Defines.skill_t;
import doom.DoomContext;
import doom.DoomStatus;
import doom.event_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;
import doom.weapontype_t;

/** This is a very simple tester for the Automap. Combined with status bar + Level loader. */

public class AutoMapTester3 {
    
    public static final int WIDTH=320;

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);

    
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();
    BufferedRenderer V=new BufferedRenderer(WIDTH,200);
    V.Init();
    BufferedImage bi=new BufferedImage(V.getWidth(),V.getHeight(),BufferedImage.TYPE_INT_RGB);
    V.setPalette(pal);
    V.mapInternalRasterToBufferedImage(bi);
    
    DoomStatus ds = new DoomStatus();
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    ds.gamemode=GameMode_t.shareware;
    ds.wminfo=new wbstartstruct_t();
    C2JUtils.initArrayOfObjects(ds.players,player_t.class);
    Defines.SCREENWIDTH=WIDTH;
    Defines.SCREENHEIGHT=200;
    
    DoomContext DC=new DoomContext();
    DC.DS=ds;
    DC.W=W;
    DC.V=V;
    DC.RND=new DoomRandom();
    ds.players[0].cmd=new ticcmd_t();
    ds.players[0].itemcount=1337;
    ds.players[0].killcount=1337;
    ds.players[0].secretcount=1337;
    ds.players[0].weaponowned[0]=true;
    ds.players[0].weaponowned[1]=true;
    ds.players[0].weaponowned[2]=true;
    ds.players[0].weaponowned[3]=true;
    ds.players[0].readyweapon=weapontype_t.wp_pistol;
    ds.players[0].health[0]=100;
    ds.players[0].armorpoints[0]=100;
    ds.players[0].ammo[0]=400;
    ds.players[0].maxammo[0]=400;
    ds.players[0].ammo[1]=100;
    ds.players[0].maxammo[1]=100;
    ds.players[0].ammo[2]=100;
    ds.players[0].maxammo[2]=100;
    ds.players[0].ammo[3]=600;
    ds.players[0].maxammo[3]=600;

    
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
    LevelLoader PL=new LevelLoader(DC);
    PL.SetupLevel(1, 1, 0, skill_t.sk_hard);
    DC.LL=PL;
    DC.ST=ST;
    DoomAutoMap AM=new Map(DC);
    AM.Start();
    
    ST.Responder(new event_t('i'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('t'));
    
    AM.Responder(new event_t(Map.AM_FOLLOWKEY));
    AM.Responder(new event_t(Map.AM_ZOOMOUTKEY));
    AM.Responder(new event_t(Map.AM_GRIDKEY));
    
    CrappyDisplay frame = new CrappyDisplay(bi);
    frame.setTitle("MochaDoom");
    InputListener in = new InputListener();
    frame.addComponentListener(in);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    //frame.setUndecorated(true);
    frame.setVisible(true);
    frame.setBounds(frame.getX(), frame.getY(), WIDTH, 240);
    
    
    long a=System.nanoTime();
    int TICKS=10000;
    for (int i=0;i<TICKS;i++){
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
    V.changePalette((i/(2000/14))%14);
    V.remap(0);
    frame.update(null);
    /*File outputFile =
        new File(
            "tic"+i+".png");
    ImageIO.write(bi, "PNG", outputFile); */
//    V.takeScreenShot(0, "tic"+i,icm);    
    //AM.Responder(new event_t(Map.AM_PANLEFTKEY));

    }
    
    long b=System.nanoTime();
    
    System.out.println(TICKS +" tics in " +((b-a)/1e09) +" = "+TICKS/((b-a)/1e09) + " fps");
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
    
    public static void initFullScreen(GraphicsDevice gd, Frame gf) {
        // initialize the main app frame
        gf = new Frame("Game Frame");
        gf.setUndecorated(true);
        // disable repaint mechanism
        gf.setIgnoreRepaint(true);
        // the next call shows the window
        gd.setFullScreenWindow(gf);
       }
        
    
}
