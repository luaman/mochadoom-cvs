package testers;

import static data.Defines.PU_STATIC;
import static data.Defines.pw_allmap;
import static m.fixed_t.FRACBITS;

import i.system;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JFrame;

import m.random;
import p.Playfield;
import p.mobj_t;
import rr.patch_t;
import st.StatusBar;
import utils.C2JUtils;
import v.BufferedRenderer;
import w.DoomBuffer;
import w.WadLoader;
import automap.DoomAutoMap;
import automap.Map;
import data.Defines;
import data.doomstat;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import data.Defines.skill_t;
import doom.DoomContext;
import doom.event_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;
import doom.weapontype_t;
import f.Wiper;

/** This is a very simple tester for the Automap. Combined with status bar + Level loader. */

public class AssWipeTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad","GRID.WAD"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);

    
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();
    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
    
    BufferedRenderer V=new BufferedRenderer(640,200,icm);
    V.Init();
    
    patch_t titlepic=W.CachePatchName("TITLEPIC", PU_STATIC);
    patch_t credit=W.CachePatchName("HELP1", PU_STATIC);
    random RND=new random();
    DoomContext DC=new DoomContext();
    
    DC.RND=RND;
    DC.V=V;
    
    
    Wiper wipe=new Wiper(DC);
    
    Defines.SCREENWIDTH=640;
    Defines.SCREENHEIGHT=200;
    
    
    
    
    // "Hook" on screen 0.
    BufferedImage bi=((BufferedRenderer)V).screenbuffer[0];
    
    JFrame frame = new JFrame("MochaDoom");
    CrappyDisplay shit = new CrappyDisplay(bi);
    frame.add(shit);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    //frame.setUndecorated(true);
    frame.setVisible(true);
    frame.setBounds(frame.getX(), frame.getY(), 640, 240);
    
    int tck=system.GetTime();
    long a=System.nanoTime();
    int TICKS=10;
    int frames=0;
    for (int i=0;i<TICKS;i++){
        V.DrawPatch(0, 0, 0, credit);
        V.DrawPatch(320, 0, 0, credit);
        wipe.StartScreen(0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT);
                V.DrawPatch(0, 0, 0, titlepic);
                V.DrawPatch(320, 0, 0, titlepic);
        wipe.EndScreen(0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT);
        
        int wipestart = system.GetTime () - 1;
        int nowtime;
        int tics;
        boolean done;
    //wipe.ScreenWipe(Wiper.wipe.Melt.ordinal(), 0, 0,  Defines.SCREENWIDTH, Defines.SCREENHEIGHT, TICKS-i);

    //ST.Drawer(false,true);
    //Thread.sleep(200);    
    
    do
    {
    do
    {
        nowtime = system.GetTime ();
        tics = nowtime - wipestart;
    } while (tics==0);
    wipestart = nowtime;
    V.DrawPatch(0, 0, 0, titlepic);
    V.DrawPatch(320, 0, 0, titlepic);
    done = wipe.ScreenWipe(Wiper.wipe.Melt.ordinal()
                   , 0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT, tics);
    //I_UpdateNoBlit ();
    //M_Drawer ();                            // menu is drawn even on top of wipes
    //System.out.println(i);
    
    frame.update(frame.getGraphics());
    
    } while (!done);
    
   // wipe.ScreenWipe(Wiper.wipe.Melt.ordinal(), 0, 0,  Defines.SCREENWIDTH, Defines.SCREENHEIGHT, TICKS-i);
    
    
    
    /*File outputFile =
        new File(
            "tic"+i+".png");
    ImageIO.write(bi, "PNG", outputFile); */
//    V.takeScreenShot(0, "tic"+i,icm);    
    //AM.Responder(new event_t(Map.AM_PANLEFTKEY));

    }
    
    long b=System.nanoTime();
    
    System.out.println(frames +" frames in " +((b-a)/1e09) +" = "+TICKS/((b-a)/1e09) + " fps");
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
