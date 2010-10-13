package testers;

import static data.Defines.KEY_F1;
import static data.Defines.PU_STATIC;
import static data.Limits.MAXEVENTS;

import hu.HU;
import i.AWTDoom;
import i.DoomSystem;
import i.DoomSystemInterface;
import i.InputListener;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JFrame;

import p.Actions;
import p.LevelLoader;

import automap.Map;

import rr.UnifiedRenderer;
import rr.patch_t;
import s.DummySoundDriver;
import st.StatusBar;

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
import data.Defines.Language_t;
import data.Defines.gamestate_t;
import data.Defines.skill_t;
import doom.DoomContext;
import doom.DoomMain;
import doom.DoomStatus;
import doom.event_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;

/** This is a very simple tester for Menu module  */

public class AWTRenderViewTester {
    public static final int WIDTH=320;
    public static final int HEIGHT=200;
    
    public static void main(String[] argv) {
        try {
        	
    // Create a Wad file loader.
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    
    System.out.println("Total lumps read: "+W.numlumps);
    patch_t help1=W.CachePatchName("TITLEPIC", PU_STATIC);

    // Read the paletter.
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    // Create a video renderer
    BufferedRenderer V=new BufferedRenderer(WIDTH,HEIGHT);
    V.Init();
    byte[] pal=palette.getBuffer().array();
    

    
    DoomSystemInterface I=new DoomSystem();
    DoomMain DM=new DoomMain();
    // Create the frame.
    AWTDoom frame = new AWTDoom(DM,V,pal);
    frame.InitGraphics();
    DM.I=I;
    DM.VI=frame;
    DM.S=new DummySoundDriver();
    frame.DM=DM;
    DM.W=W;
    DM.V=V;
    DM.DM=DM;
    HU HU=new HU(DM);    
    DM.language=Language_t.english;
    HU.Init();
    DM.HU=HU;
    DM.gameepisode=1;
    DM.gamemap=1;
    DM.gamemission=GameMission_t.doom;
    DM.gamemode=GameMode_t.shareware;
    DM.wminfo=new wbstartstruct_t();
    // Simulate being in the mid of a level.
    DM.usergame=true;
    DM.gamestate=gamestate_t.GS_LEVEL;
    DM.gameskill=skill_t.sk_hard;
    //C2JUtils.initArrayOfObjects(DM.players,player_t.class);
    
    DM.RND=new random();
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
    DM.PlayerReborn(0);
   
    DoomMenu M=DM.M=new Menu(DM);
    Map AM=(Map) (DM.AM=new Map(DM));
    StatusBar ST=(StatusBar) (DM.ST=new StatusBar(DM));
    LevelLoader LL=DM.LL=new LevelLoader(DM);
    DM.P=new Actions(DM);
    DM.R=new UnifiedRenderer(DM); 
    
    DM.P.updateStatus(DM);
    LL.updateStatus(DM);
    M.updateStatus(DM);
    ST.updateStatus(DM);
    AM.updateStatus(DM);
    DM.R.Init();
    DM.P.Init();
    LL.SetupLevel(1, 1, 0, skill_t.sk_hard);
    ST.Init();
    M.Init();
    
    ST.Start();
    AM.LevelInit();
    AM.Start();
    DM.R.SetViewSize(10, 0);
    DM.R.ExecuteSetViewSize();
    DM.R.skytexture=DM.R.TextureNumForName("SKY1");
    long a=System.nanoTime();
    
    DM.menuactive=false;
    DM.automapactive=true;
    DM.R.FillBackScreen();
    DM.R.DrawViewBorder();
        
        for (int i=0;i<100000;i++){
            int ba=DM.I.GetTime();
            while (ba-DM.I.GetTime()==0){
                //frame.setVisible(true);
                Thread.sleep(1);               
            }
           frame.GetEvent();
           
           for ( ; DM.eventtail != DM.eventhead ; DM.eventtail = (++DM.eventtail)&(MAXEVENTS-1) )
           {
       	event_t ev = DM.events[DM.eventtail];

            //System.out.println(ev);
            if (ev!=null){
            
            AM.Responder(ev);
            M.Responder(ev);
            ST.Responder(ev);
           }
           }
            
       
           
        //V.DrawPatch(0,0,0,help1);
        //M.Ticker();
        //M.Drawer();
        AM.Ticker();
        //AM.Drawer();
        ST.Ticker();        
        DM.players[0].viewheight=100;
        DM.R.viewheight=121;
        
        DM.R.RenderPlayerView(DM.players[0]);
        ST.Drawer(false,true);
        System.out.println("Rendered"+DM.gametic);
        DM.gametic++;
        frame.FinishUpdate();
        if (i%100==0){
        	   long b=System.nanoTime();
        	    
        	    System.out.println(i +" frames in " +((b-a)/1e09) +" = "+i/((b-a)/1e09) + " fps");
        }
        System.out.print(frame.processEvents());
        }
            } catch (Exception e){
                e.printStackTrace();
            }
    
    }
    
}
