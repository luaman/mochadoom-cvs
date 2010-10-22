package rr;

import static data.Defines.RANGECHECK;
import static data.Defines.SCREENHEIGHT;
import static data.Defines.SCREENWIDTH;
import static data.Limits.MAXHEIGHT;
import static data.Limits.MAXOPENINGS;
import static data.Limits.MAXVISPLANES;
import static data.Limits.MAXWIDTH;
import static data.SineCosine.finesine;
import static data.Tables.ANG180;
import static data.Tables.ANG270;
import static data.Tables.ANG90;
import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.BITS32;
import static data.Tables.DBITS;
import static data.Tables.FINEANGLES;
import static data.Tables.SLOPERANGE;
import static data.Tables.SlopeDiv;
import static data.Tables.tantoangle;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import p.LevelLoader;
import p.UnifiedGameMap;
import i.DoomStatusAware;
import i.DoomSystemInterface;
import m.fixed_t;
import rr.UnifiedRenderer.BSP;
import rr.UnifiedRenderer.Planes;
import rr.UnifiedRenderer.Segs;
import rr.UnifiedRenderer.Things;
import rr.UnifiedRenderer.colfunc_t;
import v.DoomVideoRenderer;
import w.WadLoader;
import doom.DoomContext;
import doom.DoomMain;
import doom.player_t;

public abstract class RendererState implements DoomStatusAware{

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

    // This one is related to the 3-screen display mode.
    // ANG90 = left side, ANG270 = right
    public int viewangleoffset;
    
    //////////////////////////////////From r_sky.c /////////////////////////////////////

    public int skyflatnum;
    // This is also in DM, but one is enough, really.
    public int skytexture;
    int skytexturemid;
    
    public void updateStatus(DoomContext DC){
        this.DM=DC.DM;
        this.LL=DC.LL;
        this.W=DC.W;
        this.V=DC.V;
        this.P=DC.P;
        this.I=DC.I;
    }
    
}
