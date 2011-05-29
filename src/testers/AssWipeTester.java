package testers;

import static data.Defines.PU_STATIC;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JFrame;

import m.DoomRandom;
import m.IRandom;
import rr.patch_t;
import v.BufferedRenderer;
import w.DoomBuffer;
import w.WadLoader;
import data.Defines;
import doom.DoomMain;
import f.Wiper;

/** This is a very simple tester for the Automap. Combined with status bar + Level loader. */

public class AssWipeTester {
	
	public static int WIDTH=320;

    public static void main(String[] argv) {
        try {
    DoomMain DC=new DoomMain();
    
    WadLoader W=new WadLoader(DC.I);
    
    IRandom RND=new DoomRandom();
    DC.RND=RND;        
    DC.W=W;
    
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);

    BufferedRenderer V=new BufferedRenderer(640,200);   
    DC.V=V;
    
    // Get the palettes
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    byte[] pal=palette.getBuffer().array();
    IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);    
    V.setIcm(icm);
    V.Init();
    
    
    patch_t titlepic=W.CachePatchName("TITLEPIC", PU_STATIC);
    patch_t credit=W.CachePatchName("HELP1", PU_STATIC);
    
    
    

    
    
    Wiper wipe=new Wiper(DC);
    
    Defines.SCREENWIDTH=640;
    Defines.SCREENHEIGHT=200;
    
    
    
    
    // "Hook" on screen 0.
    BufferedImage bi=((BufferedRenderer)V).screenbuffer[0];
    //BufferedImage[] pals=V.getBufferedScreens(0, icms);
    CrappyDisplay frame = new CrappyDisplay(bi);
    frame.setTitle("MochaDoom");
    

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    //frame.setUndecorated(true);
    frame.setVisible(true);
    frame.setBounds(frame.getX(), frame.getY(), WIDTH, 240);

    int tck=DC.I.GetTime();
    long a=System.nanoTime();
    int TICKS=10;
    int frames=0;
    for (int i=0;i<TICKS;i++){
        V.DrawPatch(0, 0, 0, titlepic);
        wipe.StartScreen(0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT);
        V.DrawPatch(0, 0, 0, credit);
        wipe.EndScreen(0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT);
        
        int wipestart = DC.I.GetTime () - 1;
        int nowtime;
        int tics;
        boolean done;
        
        patch_t tmp=credit;
        credit=titlepic;
        titlepic=tmp;
    //wipe.ScreenWipe(Wiper.wipe.Melt.ordinal(), 0, 0,  Defines.SCREENWIDTH, Defines.SCREENHEIGHT, TICKS-i);

    //ST.Drawer(false,true);
    //Thread.sleep(200);    
    
    do
    {
    do
    {
        nowtime = DC.I.GetTime ();
        tics = nowtime - wipestart;
    } while (tics<1);
    wipestart = nowtime;
    //V.DrawPatch(0, 0, 0, titlepic);
    //V.DrawPatch(320, 0, 0, titlepic);
    done = wipe.ScreenWipe(Wiper.wipe.Melt.ordinal()
                   , 0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT, tics);
    //I_UpdateNoBlit ();
    //M_Drawer ();                            // menu is drawn even on top of wipes
    //System.out.println(i);
    frame.update(null);
    frames++;
   
    
    } while (!done);
    
    long b=System.nanoTime();
    
    System.out.println(frames +" frames in " +((b-a)/1e09) +" = "+frames/((b-a)/1e09) + " fps");
    
    }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
      
}
