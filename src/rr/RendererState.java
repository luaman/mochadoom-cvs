package rr;

import static data.Defines.SCREENWIDTH;
import static data.Limits.MAXOPENINGS;
import static data.Limits.MAXVISPLANES;
import static data.Tables.ANG90;
import p.LevelLoader;
import p.UnifiedGameMap;
import i.DoomStatusAware;
import i.DoomSystemInterface;
import rr.UnifiedRenderer.BSP;
import rr.UnifiedRenderer.Planes;
import rr.UnifiedRenderer.Segs;
import rr.UnifiedRenderer.Things;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import w.WadLoader;
import data.Defines;
import data.Limits;
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

    ///// FROM PLANES //////
    
    // initially.
    protected int MAXVISPLANES = Limits.MAXVISPLANES;
    
    /**
     * Clip values are the solid pixel bounding the range.
     *  floorclip starts out SCREENHEIGHT
     *  ceilingclip starts out -1
     */
    protected short[]         floorclip=new short[SCREENWIDTH],   ceilingclip=new short[SCREENWIDTH];
    
    /** visplane_t*,  treat as indexes into visplanes */
    protected int       lastvisplane, floorplane,   ceilingplane;
    

    protected visplane_t[]      visplanes=new visplane_t[MAXVISPLANES];

    /** openings is supposed to show where "openings" in visplanes start and end e.g.
     *  due to sprites, windows etc.
     */
    protected short[]         openings=new short[MAXOPENINGS];
    /** Maes: this is supposed to be a pointer inside openings */
    protected int           lastopening;//=new Short((short) 0);
    
    
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
    
 /**
  * R_FindPlane
  * 
  * Checks whether a visplane with the specified height, picnum and light 
  * level exists among those already created. This looks like a half-assed 
  * attempt at reusing already existing visplanes, rather than creating new 
  * ones. The tricky part is understanding what happens if one DOESN'T exist.
  * Called only from within R_Subsector (so while we're still trasversing stuff).
  * 
  * @param height (fixed_t)
  * @param picnum
  * @param lightlevel
  * @return was visplane_t*, returns index into visplanes[]
  */

 protected final int
 FindPlane
 ( int   height,
   int       picnum,
   int       lightlevel )
 {
     //System.out.println("\tChecking for visplane merging...");
     int check=0; // visplane_t* 
     visplane_t chk=null;
     
     if (picnum == skyflatnum)
     {
     height = 0;         // all skys map together
     lightlevel = 0;
     }
     chk=visplanes[0];
     
     // Find visplane with the desired attributes
     for (check=0; check<lastvisplane; check++)
     {
     
         chk=visplanes[check];
     if (height == chk.height
         && picnum == chk.picnum
         && lightlevel ==chk.lightlevel) {
         //  Found a visplane with the desired specs.
         break;
         }
     }
             
     if (check < lastvisplane){
     return check;
     }          
     
     // Found a visplane, but we can't add anymore.
     if (lastvisplane == MAXVISPLANES){
     //I.Error ("R_FindPlane: no more visplanes");
         ResizeVisplanes();
     
     }
     
     /* FIXED: we need to add this post-fix here because of the way
      * the original was structured (pointer hacks, too lengthy to
      * explain). We need to make sure that when no visplane is found
      * a "failed check" will actually result in a pointer to the
      * next "free" visplane.
      *  This fixes the "blinking visplane bug", which manifested 
      *  itself when sector lighting effects changed the light level
      */
     
     chk=visplanes[check];
     // Add a visplane
     lastvisplane++;
     chk.height = height;
     chk.picnum = picnum;
     chk.lightlevel = lightlevel;
     chk.minx = SCREENWIDTH;
     chk.maxx = -1;
     //memset (chk.top,0xff,sizeof(chk.top));
     chk.clearTop();
         
     return check;
 }
 
 protected final void ResizeVisplanes() {
     visplane_t[] tmp=new visplane_t[MAXVISPLANES*2];
     System.arraycopy(visplanes, 0, tmp, 0, MAXVISPLANES);
     
     C2JUtils.initArrayOfObjects(tmp,MAXVISPLANES,tmp.length);
     
     // Bye bye, old visplanes.
     visplanes=tmp;   
     MAXVISPLANES*=2;
    
     System.out.println("Visplane buffer resized. Actual capacity "+visplanes.length);
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
