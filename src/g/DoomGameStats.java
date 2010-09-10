package g;

import static data.Defines.BACKUPTICS;
import static data.Limits.MAXPLAYERS;
import f.Finale;
import hu.HU;
import m.Menu;
import m.random;
import automap.DoomAutoMap;
import p.LevelLoader;
import p.mobj_t;
import rr.RendererData;
import rr.UnifiedRenderer;
import st.StatusBar;
import w.EndLevel;
import w.WadLoader;
import data.doomstat;
import data.mapthing_t;
import doom.DoomContext;
import doom.gameaction_t;
import doom.player_t;
import doom.wbstartstruct_t;



public abstract class DoomGameStats{    
    
    ////////////////////////// STATUS /////////////////////////
    
    doomstat DS;
    HU HU;
    RendererData RD;
    LevelLoader LL;
    Menu M;
    StatusBar ST;
    DoomAutoMap AM;
    Finale F;
    WadLoader W;
    UnifiedRenderer R;
    random RND;
    EndLevel WI;
    
    // Fields specific to DoomGame.
    public gameaction_t    gameaction; 
    public boolean         sendpause;              // send a pause event next tic 
    
    // Most of these are actually in doomstat. Some can be "pegged" (those in arrays) but
    // in order to keep references correctly updated, we must use them in DS.
    // Objects not in arrays can be read correctly if "pegged", fields can be written to,  
    // but not if they have value semantics.
        
    boolean paused;
    boolean         sendsave; // send a save event next tic 
    int starttime;
    boolean         timingdemo;             // if true, exit with report on completion 
    /*
    public boolean     respawnmonsters;
    public int             gameepisode; 
    public int             gamemap; 
     
    public boolean         paused; 
    public boolean         usergame;               // ok to save / end game 
     
    
    public boolean         nodrawers;              // for comparative timing purposes 
    public boolean         noblit;                 // for comparative timing purposes 
    public int             starttime;              // for comparative timing purposes       
     
    public boolean         viewactive; 
     
    public boolean         deathmatch;             // only if started as net death 
    public boolean         netgame;                // only true if packets are broadcast 
    */
    
    public boolean getPaused() {
      return paused;    
  }

  public void setPaused(boolean paused) {
      this.paused = paused;
  }

  // These can be pegged to doomstat, because they are arrays.
  protected boolean[]         playeringame; 
  protected player_t[]        players; 
  protected mapthing_t[] playerstarts; 
     
    /*
    int             consoleplayer;          // player taking events and displaying 
    int             displayplayer;          // view being displayed 
    int             gametic; 
    int             levelstarttic;          // gametic at level start 
    int             totalkills, totalitems, totalsecret;    // for intermission 
    */
    
    String            demoname; 
    boolean         demorecording; 
    public boolean         demoplayback; 
    boolean     netdemo; 
    byte[]       demobuffer;
    /** pointers */
    int       demo_p, demoend; 
    boolean         singledemo;             // quit after playing a demo from cmdline 
     
    boolean         precache = true;        // if true, load all graphics at start 
     
    /** parms for world map / intermission, peg to doomstat */
    wbstartstruct_t wminfo;                 
     
    short[][]       consistancy=new short[MAXPLAYERS][BACKUPTICS]; 
     
    byte[]       savebuffer;
     
     
    /** 
     * controls (have defaults) 
     */
    
    public int             key_right;
    public int     key_left;

    public int     key_up;
    public int     key_down; 
    public int             key_strafeleft;
    public int     key_straferight; 
    public int             key_fire;
    public int     key_use;
    public int     key_strafe;
    public int     key_speed; 
     
    public int             mousebfire; 
    public int             mousebstrafe; 
    public int             mousebforward; 
     
    public int             joybfire; 
    public int             joybstrafe; 
    public int             joybuse;
    public int             joybspeed;
     
     
     
    protected int MAXPLMOVE(){
        return forwardmove[1];
    }
     
    protected static final int TURBOTHRESHOLD = 0x32;

     /** fixed_t */
    int[]     forwardmove = {0x19, 0x32},
                sidemove = {0x18, 0x28}, 
     angleturn = {640, 1280, 320};    // + slow turn 

    protected static final int SLOWTURNTICS    =6; 
     
    protected static final int NUMKEYS    = 256; 

    boolean[]         gamekeydown=new boolean[NUMKEYS]; 
    int             turnheld;               // for accelerative turning 
     
    boolean[]     mousearray=new boolean[4];
    
    /** This is an alias for mousearray [1+i] */
    protected boolean mousebuttons(int i){
        return mousearray[1+i];      // allow [-1]
    }
    
    protected void mousebuttons(int i, boolean value){
        mousearray[1+i]=value;      // allow [-1]
    }
    
    protected void mousebuttons(int i, int value){
        mousearray[1+i]=value!=0;      // allow [-1]
    }

    /** mouse values are used once */ 
    int     mousex, mousey, dclicktime, dclickstate,
            dclicks,  dclicktime2, dclickstate2, dclicks2;

    /** joystick values are repeated */ 
    int             joyxmove, joyymove;
    boolean[]         joyarray=new boolean[5]; 

    protected boolean joybuttons(int i){
        return joyarray[1+i];      // allow [-1]
    }

    protected void joybuttons(int i, boolean value){
        joyarray[1+i]=value;      // allow [-1]
    }
    
    protected void joybuttons(int i, int value){
        joyarray[1+i]=value!=0;      // allow [-1]
    }
    
    int     savegameslot; 
    String        savedescription; 
     
     
    protected static final int BODYQUESIZE= 32;

    mobj_t[]     bodyque=new mobj_t[BODYQUESIZE]; 
    int     bodyqueslot; 
     
    public byte[]       statcopy;               // for statistics driver
    
}
