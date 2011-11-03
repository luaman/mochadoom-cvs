package testers;

import static data.Defines.PU_STATIC;
import static data.Limits.MAXEVENTS;
import n.DummyNetworkHandler;

import hu.HU;
import i.DoomSystem;
import i.IDoomSystem;

import p.Actions;
import p.ILevelLoader;
import p.LevelLoader;

import automap.Map;
import awt.AWTDoom;
import rr.ParallelRenderer2;
import rr.SimpleTextureManager;
import rr.SpriteManager;
import s.DummySoundDriver;
import st.StatusBar;
import timing.MilliTicker;

import m.IDoomMenu;
import m.Menu;
import m.DoomRandom;
import v.BufferedRenderer;
import v.DoomVideoRenderer;
import v.GammaTables;
import v.IVideoScale;
import v.VideoScaleInfo;
import w.DoomBuffer;
import w.WadLoader;
import defines.*;
import data.Tables;
import doom.CommandLine;
import doom.DoomMain;
import doom.event_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;

/** This is a very simple tester for Menu module  */

public class AWTParallelRenderTester2 {

    static IVideoScale VSI=new VideoScaleInfo(4.0f);
    
    public static void main(String[] argv) {
        try {
    
            Tables.InitTables();
            
    // Create a Wad file loader.
    
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    
    System.out.println("Total lumps read: "+W.numlumps);

    // Read the palette.
    DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
    // Create a video renderer
    DoomVideoRenderer<?> V=new BufferedRenderer(VSI.getScreenWidth(),VSI.getScreenHeight());
    V.setVideoScale(VSI);
    V.initScaling();
    V.Init();
    byte[] pal=palette.getBuffer().array();
    V.createPalettes(pal, GammaTables.gammatables, 14, 256, 3, 5);
    

    
    IDoomSystem I=new DoomSystem();
    DoomMain DM=new DoomMain();
    CommandLine CM=new CommandLine(argv);
    DM.CM=CM;
    DM.TICK=new MilliTicker();    
    DM.setVideoScale(VSI);
    DM.initScaling();
    DM.singletics=true;
    DM.setTicdup(1);
    DM.DGN=new DummyNetworkHandler();
    
    // Create the frame.
    AWTDoom frame = new AWTDoom(DM,(DoomVideoRenderer<byte[]>) V);
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
    DM.setGameMode(GameMode_t.shareware);
    DM.wminfo=new wbstartstruct_t();
    // Simulate being in the mid of a level.
    DM.usergame=true;
    DM.gamestate=gamestate_t.GS_LEVEL;
    DM.gameskill=skill_t.sk_hard;
    //C2JUtils.initArrayOfObjects(DM.players,player_t.class);
    
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
    Map AM=new Map(DM);
    DM.AM=AM;
    StatusBar ST=(StatusBar) (DM.ST=new StatusBar(DM));
    ILevelLoader LL=DM.LL=new LevelLoader(DM);
    DM.P=new Actions(DM);
    DM.SM=new SpriteManager(DM);
    DM.R=new ParallelRenderer2(DM,2,1); 
    DM.TM=new SimpleTextureManager(DM);
    
    DM.P.updateStatus(DM);
    LL.updateStatus(DM);
    M.updateStatus(DM);
    ST.updateStatus(DM);
    AM.updateStatus(DM);
    DM.initializeVideoScaleStuff();
    DM.R.Init();
    DM.P.Init();
    DM.players[0].updateStatus(DM);
    DM.players[0].PlayerReborn();
    
    ST.Init();
    M.Init();
    ST.Start();
    LL.SetupLevel(1, 1, 0, skill_t.sk_hard);
    AM.LevelInit();
    AM.Start();
    
    DM.R.SetViewSize(11, 0);
    DM.R.ExecuteSetViewSize();
    DM.TM.setSkyTexture(DM.TM.CheckTextureNumForName("SKY1"));
    long a=System.nanoTime();
    
    DM.menuactive=false;
    DM.automapactive=true;
    DM.R.FillBackScreen();
    DM.R.DrawViewBorder();
    
    
    // Center on "bloody mess" in E1M1
    DM.players[0].mo.y+=420<<16;
    //DM.players[0].mo.x+=1650<<16;
        int pl=0;
        for (int i=0;i<20000;i++){
   
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
        DM.players[0].viewz=(40)<<16;
        //DM.players[0].mo.x=ox+(int) ((12864<<16)*Math.cos(2*Math.PI*i/500.0));
        //DM.players[0].mo.y=oy+(int) ((64<<16)*Math.sin(2*Math.PI*i/500.0));
        //DM.players[0].mo.angle= ((long)(0xFFFFFFFFL*(Math.atan2(DM.players[0].mo.y-oy,DM.players[0].mo.x-ox)+Math.PI)/(2*Math.PI)))&0xFFFFFFFFL;
        DM.players[0].mo.angle=(DM.players[0].mo.angle-0x800000)&0xFFFFFFFFL;
        //System.out.println(">>>>>>>>>>>>>>>>>> VIEW ANGLE "+360.0*(DM.players[0].mo.angle>>19)/8192.0); 
       
        DM.R.RenderPlayerView(DM.players[0]);
        //ST.Drawer(false,true);
        //System.out.println("Rendered"+DM.gametic);
        
        DM.gametic++;
        frame.FinishUpdate();
        if (i%200==0){
        	   long b=System.nanoTime();
        	    //frame.setPalette((pl++)%14);
        	    System.out.println((200) +" frames in " +((b-a)/1e09) +" = "+200/((b-a)/1e09) + " fps");
        	    a=b;
        }
        
       // System.out.print(frame.processEvents());
        /*while (ba-DM.I.GetTime()>-1){
            Thread.sleep(10);               
        }*/
        }
            } catch (Exception e){
                e.printStackTrace();
            }
    
    }
    
}
