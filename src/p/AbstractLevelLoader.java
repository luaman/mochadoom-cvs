package p;

import i.IDoomSystem;
import rr.RendererState;
import rr.TextureManager;
import rr.line_t;
import rr.node_t;
import rr.sector_t;
import rr.seg_t;
import rr.side_t;
import rr.subsector_t;
import rr.vertex_t;
import s.IDoomSound;
import st.IDoomStatusBar;
import v.DoomVideoRenderer;
import w.IWadLoader;
import data.Limits;
import data.mapthing_t;
import doom.DoomStatus;

/** The idea is to lump common externally readable properties that need 
 *  DIRECT ACCESS (aka not behindsetters/getters) here, as well as some common 
 *  shared internal structures/status objects. If you need access to stuff like
 *  the blockmap/reject table etc. then you should ask for this class. If you
 *  only need access to some methods like e.g. SetupLevel, you can simply use the
 *  ILevelLoader interface.
 * 
 * 
 * @author velktron
 *
 */

public abstract class AbstractLevelLoader  implements ILevelLoader {

/////////////////// Status objects ///////////////////
    
    IDoomStatusBar ST;
    IDoomSystem I;
    IWadLoader W;
    DoomStatus DM;
    DoomVideoRenderer V;
    RendererState R;
    TextureManager TM;
    Actions P;
    IDoomSound S;
    
    //  
    // MAP related Lookup tables.
    // Store VERTEXES, LINEDEFS, SIDEDEFS, etc.
    //
    public int     numvertexes;
    public vertex_t[]   vertexes;

    public int     numsegs;
    public seg_t[]      segs;

    public int     numsectors;
    public sector_t[]   sectors;

    public int     numsubsectors;
    public subsector_t[]    subsectors;

    public int     numnodes;
    public node_t[]     nodes;

    public int     numlines;
    public line_t[]     lines;

    public int     numsides;
    public side_t[]     sides;
    
    // BLOCKMAP
    // Created from axis aligned bounding box
    // of the map, a rectangular array of
    // blocks of size ...
    // Used to speed up collision detection
    // by spatial subdivision in 2D.
    //
    /** Blockmap size. */
    protected int     bmapwidth, bmapheight; // size in mapblocks
    
	/** killough 3/1/98: remove blockmap limit internally: */
	int[]     blockmap;              // was short -- killough

	/** offsets in blockmap are from here */
	int[]      blockmaplump;          // was short -- killough
    /** (fixed_t) origin of block map */
    public int     bmaporgx, bmaporgy;
    /** for thing chains */
    public mobj_t[]    blocklinks; 
    
    
    /** REJECT
       For fast sight rejection.
       Speeds up enemy AI by skipping detailed
       LineOf Sight calculation.
       Without special effect, this could be
       used as a PVS lookup as well.
    */
    
    protected byte[]       rejectmatrix;
    
	// Maintain single and multi player starting spots.

	// 1/11/98 killough: Remove limit on deathmatch starts
    protected mapthing_t[] deathmatchstarts;      // killough
    protected int     num_deathmatchstarts;   // killough

    //mapthing_t* deathmatch_p;
	protected int deathmatch_p;
	protected mapthing_t[] playerstarts=new mapthing_t[Limits.MAXPLAYERS];
    
    public AbstractLevelLoader(DoomStatus DC){
  	  this.updateStatus(DC);
    }
    
    @Override
    public void updateStatus(DoomStatus DC){
        this.W=DC.W;
        this.DM=DC;
        this.P=DC.P;
        this.R=DC.R;
        this.I=DC.I;
        this.S=DC.S;
        this.TM=DC.TM;
  	  
    }
    
	
}
