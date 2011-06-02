package testers;
import static data.Defines.KEY_F1;
import static data.Defines.PU_STATIC;
import static data.Defines.pw_allmap;
import static m.fixed_t.FRACBITS;

import i.InputListener;
import i.IDoomSystem;

import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;

import javax.imageio.ImageIO;

import m.IDoomMenu;
import m.Menu;
import m.DoomRandom;
import p.LevelLoader;
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
import f.EndLevel;
import f.Wiper;

public class DoomApplet
        extends Applet {
    
    public static final int WIDTH=320;
    public static final int HEIGHT=200;
    public static final int TICKS=5000;
    
    IndexColorModel[] icms;
    Canvas drawhere;
    BufferedImage[] bi;
    int palette=0;
    Dimension size;
    Image crap;
    InputListener in;
    Graphics2D g2d;
    BufferedImage[] pals;
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    IWadLoader W;
    BufferedRenderer V;
    DoomStatus ds;
    DoomContext DC;
    StatusBar ST;
    Map AM;
    Menu M;
    EndLevel EL;

    
    private patch_t titlepic;

    private patch_t help1;

    private Wiper wipe;
    
    public void start(){
        
    }
    
    public void doStuff(){
        
       // Do a screen wipe 
       WipeYoAss();
       /*
       DoMenu();
       doAutoMap();
       doEndLevel();*/
    }
    
    private void doAutoMap() {
        
        V.screens[0]=((DataBufferByte)pals[0].getRaster().getDataBuffer()).getData();
        ST.Responder(new event_t('i'));
        ST.Responder(new event_t('d'));
        ST.Responder(new event_t('d'));
        ST.Responder(new event_t('t'));
        
        AM.Responder(new event_t(Map.AM_FOLLOWKEY));
        AM.Responder(new event_t(Map.AM_ZOOMOUTKEY));
        AM.Responder(new event_t(Map.AM_GRIDKEY));
        long a=System.nanoTime();
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


            V.DrawPatch(0, 0, 0, titlepic);
            AM.Ticker();
            AM.Drawer();
            ST.Ticker();
            ST.Drawer(false,true);

            this.setPalette((i/(TICKS/14))%14);
            this.processEvents();
            //System.out.println(V.isRasterNull(0));
            this.update();
        
            }
    }

    private void dump(int i) {
        try {
            V.takeScreenShot(0, "C:\\SHIT"+i+".PNG",icms[0]);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public void init(){
        
        // size
        size = new Dimension();
        size.width = WIDTH;
        size.height = HEIGHT;
        this.setSize(size);
        
        // Canvas
        drawhere=new Canvas();
        drawhere.setSize(size);
        drawhere.setBounds(0, 0, drawhere.getWidth(),drawhere.getHeight());
        drawhere.setBackground(Color.black);            
        this.add(drawhere);
        drawhere.setVisible(true);
        
        // Listener
        in = new InputListener();
        this.addComponentListener(in);
        
        // Graphics 
        g2d = (Graphics2D)drawhere.getGraphics();
        
        //this.setSize(new Dimension(WIDTH,));
        this.setVisible(true);

        W=new WadLoader();
        try {
            W.InitMultipleFiles(new String[] {"doom.wad"});
        } catch (Exception e) {
            e.printStackTrace();
        }
        help1=W.CachePatchName("HELP1", PU_STATIC);
        System.out.println(help1.height);
        System.out.println(help1.width);        
        System.out.println(help1);
        System.out.println(W.GetNumForName("TITLEPIC"));
        System.out.println(W.lumpinfo[W.GetNumForName("TITLEPIC")].size);
        titlepic=W.CachePatchName("HELP1", PU_STATIC);
        System.out.println(titlepic.height);
        System.out.println(titlepic.width);

        System.out.println(help1);
        System.out.println(W.GetNumForName("HELP1"));
        System.out.println(W.lumpinfo[W.GetNumForName("HELP1")].size);
        DoomBuffer palette = W.CacheLumpName("PLAYPAL", PU_STATIC);
        byte[] pal=palette.getBuffer().array();

        IndexColorModel icm=new IndexColorModel(8, 256,pal, 0, false);
        Defines.SCREENWIDTH=WIDTH;
        Defines.SCREENHEIGHT=HEIGHT;
        V=new BufferedRenderer(WIDTH,HEIGHT,icm);
        V.Init();
        icms=new IndexColorModel[palette.getBuffer().limit()/768];
        for (int i=0;i<icms.length;i++){
            icms[i]=new IndexColorModel(8, 256,pal, i*768, false);
            }
        pals=new BufferedImage[icms.length];
        pals=V.getBufferedScreens(0, icms);
        
   /*     for (int i=0;i<icms.length;i++){
         icms[i]=new IndexColorModel(8, 256,pal, i*768, false);
             pals[i]=new BufferedImage(icms[i],V.screenbuffer[0].getRaster(), false, null);
            }
            */
        this.Init(pals);
        
        ds=new DoomStatus();
        initDoomState(ds);

        DC=new DoomContext();
        DC.DS=ds;
        DC.W=W;
        DC.V=V;
        DC.RND=new DoomRandom();
        ST=new StatusBar(DC);
        ST.Start();
        LevelLoader PL=new LevelLoader(DC);
        try {
            PL.SetupLevel(1, 1, 0, skill_t.sk_hard);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DC.LL=PL;
        DC.ST=ST;
        AM=new Map(DC);
        AM.Start();
        M= new Menu(DC);
        M.Init();
        wipe=new Wiper(DC);
        EL=new EndLevel(DC);
        EL.Start(DC.DS.wminfo);
        
        doStuff();
    }

    private static void initDoomState(DoomStatus ds) {
        C2JUtils.initArrayOfObjects(ds.players,player_t.class);
        ds.gameepisode=1;
        ds.gamemap=1;
        ds.gamemission=GameMission_t.doom;
        ds.gamemode=GameMode_t.shareware;
        ds.wminfo=new wbstartstruct_t();
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
    }
       
    /*
    public void paint(Graphics g){
        g2d=(Graphics2D) g;
        g2d.drawImage(bi[palette],0,0,this);        
    }*/
    
    
    public void paint(Graphics g){
        /*
        if (g==null)
            this.update(null);
        else super.paint(g);
        //g2d.drawImage(bi[palette],0,0,this);
         *         
         */
    }

    

    public void Init(BufferedImage[] bi) {
        this.setBackground(Color.black);
        this.bi=bi;            


        
    }
    
    public void Init(BufferedImage bi) {
        this.bi=new BufferedImage[]{bi};
        size = new Dimension();
        size.width = bi.getWidth();
        size.height = bi.getHeight();
        this.setSize(size);
        drawhere=new Canvas();
        drawhere.setSize(size);
        drawhere.setBounds(0, 0, drawhere.getWidth(),drawhere.getHeight());
        drawhere.setBackground(Color.black);
        this.add(drawhere);
        in = new InputListener();
        this.addComponentListener(in);
        g2d = (Graphics2D)drawhere.getGraphics();
    }
    
    public void setPalette(int pal){
        this.palette=pal;
    }
    
    public void update() {
        g2d=(Graphics2D) drawhere.getGraphics();
        g2d.drawImage(bi[palette],0,0,this);
        
        //drawhere.repaint();
        //super.update(this.getGraphics());
      }   
    
    public void update(Graphics g) {
      // g2d.drawImage(bi[palette],0,0,this);       
    }
    
    public String processEvents(){
        StringBuffer tmp=new StringBuffer();
        event_t event;
        while ( (event=InputListener.nextEvent()) != null ) {
            tmp.append(event.type.ordinal()+"\n");
        }
        return tmp.toString();
    }

    public void WipeYoAss(){
        
        int WIPES=1;
        for (int i=0;i<WIPES;i++){
            V.DrawPatch(0, 0, 0, help1);
            // Grab "help"
            wipe.StartScreen(0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT);
                    V.DrawPatch(0, 0, 0, titlepic);
            wipe.EndScreen(0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT);
            
            int wipestart = IDoomSystem.GetTime () - 1;
            int nowtime;
            int tics;
            boolean done;
        
        do
        {
        do
        {
            nowtime = IDoomSystem.GetTime ();
            tics = nowtime - wipestart;
        } while (tics<1);
        wipestart = nowtime;
        //V.DrawPatch(0, 0, 0, titlepic);
        V.DrawPatch(0, 0, 0, help1);
        //V.DrawPatch(320, 0, 0, titlepic);
        done = wipe.ScreenWipe(Wiper.wipe.Melt.ordinal()
                       , 0, 0, Defines.SCREENWIDTH, Defines.SCREENHEIGHT, tics);
        //I_UpdateNoBlit ();
        //M_Drawer ();                            // menu is drawn even on top of wipes
        //System.out.println(V.isRasterNull(0));
        this.update();
       
        
        } while (!done);
    }
}
    public void DoMenu(){
        ds.menuactive=true;        
        for (int i=0;i<600;i++){


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
            try {
                Thread.sleep(14);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        V.DrawPatch(0,0,0,help1);
        M.Ticker();
        M.Drawer();
        System.out.println(V.isRasterNull(0));
        this.update();
        //System.out.print(frame.processEvents());
        }
    }
    
    public void doEndLevel(){
        int a,b;
        a=IDoomSystem.GetTime();
        b=a;
        for (int i=0;i<2000;i++){
            
        EL.Ticker();
        EL.Drawer();
        this.update();
        if (i==100){
                ds.players[0].cmd.buttons=1; // simulate attack
                ds.players[0].attackdown=false; // simulate attack
            }
            
            if (i==120){
                ds.players[0].cmd.buttons=1; // simulate attack
                ds.players[0].attackdown=false; // simulate attack
            }
       // Do we still have time>
            
            while((b-a)<1) {
                b=IDoomSystem.GetTime();
                }
           a=b;
        }
    }
    
}
