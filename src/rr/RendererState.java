package rr;

import static data.Tables.ANG90;
import p.LevelLoader;
import p.UnifiedGameMap;
import i.DoomStatusAware;
import i.DoomSystemInterface;
import rr.UnifiedRenderer.BSP;
import rr.UnifiedRenderer.Planes;
import rr.UnifiedRenderer.Segs;
import rr.UnifiedRenderer.Things;
import v.DoomVideoRenderer;
import w.WadLoader;
import doom.DoomContext;
import doom.DoomMain;

public abstract class RendererState implements DoomStatusAware, Renderer{

    //////////////////////////////// STATUS ////////////////

    protected DoomMain DM;
    protected LevelLoader LL;
    protected WadLoader W;
    protected Segs MySegs;
    protected BSP MyBSP;
    protected Planes MyPlanes;
    public Things MyThings;
    protected DoomVideoRenderer V;
    protected UnifiedGameMap P;
    protected DoomSystemInterface I;
 

    // Found in draw_c. Only ever used in renderer.
    
    public int viewwindowx;
    public int viewwindowy;
    public int viewwidth;
    public int viewheight;

    public int scaledviewwidth;

    /** killough: viewangleoffset is a legacy from the pre-v1.2 days, when Doom
     *  had Left/Mid/Right viewing. +/-ANG90 offsets were placed here on each
     *  node, by d_net.c, to set up a L/M/R session. */
    
    public static final long viewangleoffset=0;

    
    //////////////////////////////////From r_sky.c /////////////////////////////////////

    public int skyflatnum;
    // This is also in DM, but one is enough, really.
    public int skytexture;
    int skytexturemid;


    /** Increment every time a check is made 
     *  For some reason, this needs to be visible even by enemies thinking :-S*/
    public int validcount = 1;     
    

 /** Who can set this? A: The Menu. */
 public boolean      setsizeneeded;
 protected int     setblocks;
 protected int     setdetail;

 /**
  * R_SetViewSize
  * Do not really change anything here,
  * because it might be in the middle of a refresh.
  * The change will take effect next refresh.
  * 
  * @param blocks 11 is full screen, 9 default.
  * @param detail 0= high, 1 =low.
  */

 public void SetViewSize
 ( int       blocks,
 int       detail )
 {
     System.out.println("SetViewSize");
  setsizeneeded = true;
  setblocks = blocks;
  setdetail = detail;
 }
    
    public void updateStatus(DoomContext DC){
        this.DM=DC.DM;
        this.LL=DC.LL;
        this.W=DC.W;
        this.V=DC.V;
        this.P=DC.P;
        this.I=DC.I;
    }
    
}
