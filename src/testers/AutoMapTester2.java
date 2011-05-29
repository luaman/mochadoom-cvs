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

import m.IDoomMenu;
import m.Menu;
import m.DoomRandom;
import p.Actions;
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
import doom.DoomMain;
import doom.DoomStatus;
import doom.event_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;
import doom.weapontype_t;

/** This is a very simple tester for the Automap. Combined with status bar + Level loader. */

public class AutoMapTester2 {
    
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

    IndexColorModel[] icms=new IndexColorModel[palette.getBuffer().limit()/768];
    BufferedImage[] pals=new BufferedImage[icms.length];


    for (int i=0;i<icms.length;i++){
     icms[i]=new IndexColorModel(8, 256,pal, i*768, false);
    }/*
         pals[i]=new BufferedImage(icms[i],V.screenbuffer[0].getRaster(), false, null);
        }*/
    
    pals=V.getBufferedScreens(0, icms);
    
    //=V.getBufferedScreens(0,icm);>= numlumps
    
    DoomMain ds = new DoomMain();
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    ds.gamemode=GameMode_t.shareware;
    ds.wminfo=new wbstartstruct_t();
    C2JUtils.initArrayOfObjects(ds.players,player_t.class);

    
    ds.DM=ds;
    ds.W=W;
    ds.V=V;
    ds.RND=new DoomRandom();
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

    StatusBar ST=new StatusBar(ds);
    ds.ST=ST;
    ST.Start();
    LevelLoader LL=new LevelLoader(ds);
    LL.SetupLevel(1, 1, 0, skill_t.sk_hard);
    ds.LL=LL;
    ds.ST=ST;
    DoomAutoMap AM=new Map(ds);
    ds.AM=AM;
    AM.Start();
    Actions P=new Actions(ds);
    ds.P=P;
    
    ST.Responder(new event_t('i'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('d'));
    ST.Responder(new event_t('t'));
    
    AM.Responder(new event_t(Map.AM_FOLLOWKEY));
    AM.Responder(new event_t(Map.AM_ZOOMOUTKEY));
    AM.Responder(new event_t(Map.AM_GRIDKEY));
    //BufferedImage bi=((BufferedRenderer)V).screenbuffer[0];
    //BufferedImage bi2=((BufferedRenderer)V).cloneScreen(0, icm2);

    CrappyDisplay frame = new CrappyDisplay(pals);
    frame.setTitle("MochaDoom");
    

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
    frame.setPalette((i/(10000/14))%14);
    frame.processEvents();
    frame.update();
    //frame.update();
    //frame.update(shit.getGraphics());
 
    }
    
    long b=System.nanoTime();
    
    System.out.println(TICKS +" tics in " +((b-a)/1e09) +" = "+TICKS/((b-a)/1e09) + " fps");
    
       
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
