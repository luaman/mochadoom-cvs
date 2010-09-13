package p;

import static data.Defines.*;
import static data.Limits.*;
import static data.SineCosine.finecosine;
import static data.SineCosine.finesine;
import static data.Tables.ANG180;
import static data.Tables.ANGLETOFINESHIFT;
import static data.info.mobjinfo;
import static data.info.states;
import static m.BBox.*;
import static m.fixed_t.*;
import static p.MapUtils.flags;
import static p.mobj.*;
import i.system;

import m.random;
import rr.UnifiedRenderer;
import rr.line_t;
import rr.node_t;
import rr.sector_t;
import rr.seg_t;
import rr.subsector_t;
import rr.vertex_t;
import w.WadLoader;
import automap.DoomAutoMap;
import data.doomstat;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.state_t;
import data.Defines.skill_t;
import data.Defines.statenum_t;
import doom.ActionType;
import doom.acp1;
import doom.player_t;
import doom.thinker_t;
import doom.weapontype_t;

//// FROM SIGHT


public class UnifiedGameMap {
	

	
	 /////////////////// STATUS ///////////////////

    WadLoader W;
    doomstat DS;
    DoomAutoMap AM;
    random RND;
    UnifiedRenderer R;
    LevelLoader LL;
    
    ////////////// Internal singletons //////////////
    
    Specials MySpec;
	
    ////////////// PIT INTERFACES ///////////////////
	
    interface PIT_LineFunction {
        public boolean invoke(line_t ld);
    }
 
    interface PIT_MobjFunction {
        public boolean invoke(mobj_t thing);
    }
    
    //////////////////////////////////////////////


	public int		topslope;
	public int		bottomslope;		// slopes to top and bottom of target
	
	int		attackrange;
class Sight{
	//
	// P_CheckSight
	//
	int		sightzstart;		// eye z of looker


	divline_t	strace;			// from t1 to t2
	int		t2x;
	int		t2y;

	int[]		sightcounts=new int[2];

	//
	// P_InterceptVector2
	// Returns the fractional intercept point
	// along the first divline.
	// This is only called by the addthings and addlines traversers.
	//
	private int
	P_InterceptVector2
	( divline_t	v2,
	  divline_t	v1 )
	{
	    int	frac; 	// fixed_t
	    int	num;	// fixed_t
	    int	den;	// fixed_t
		
	    den = FixedMul (v1.dy>>8,v2.dx) - FixedMul(v1.dx>>8,v2.dy);

	    if (den == 0)
		return 0;
	    //	I_Error ("P_InterceptVector: parallel");
	    
	    num = FixedMul ( (v1.x - v2.x)>>8 ,v1.dy) + 
		FixedMul ( (v2.y - v1.y)>>8 , v1.dx);
	    frac = FixedDiv (num , den);

	    return frac;
	}

	//
	// P_CrossSubsector
	// Returns true
	//  if strace crosses the given subsector successfully.
	//
	boolean P_CrossSubsector (int num)
	{
	    int		seg; // pointer inside segs
	    line_t		line;
	    int			s1;
	    int			s2;
	    int			count;
	    subsector_t	sub;
	    sector_t		front;
	    sector_t		back;
	    int		opentop; //fixed_t
	    int		openbottom;
	    divline_t		divl;
	    vertex_t		v1;
	    vertex_t		v2;
	    int		frac; //fixed_t
	    int		slope;
		
	if (RANGECHECK){
	    if (num>=LL.numsubsectors)
		system.Error ("P_CrossSubsector: ss %i with numss = %i",
			 num,
			 LL.numsubsectors);
	}

	    sub = LL.subsectors[num];
	    
	    // check lines
	    count = sub.numlines;
	    seg = sub.firstline;//LL.segs[sub.firstline];

	    for ( ; count>0 ; seg++, count--)
	    {
		line = LL.segs[seg].linedef;

		// allready checked other side?
		if (line.validcount == R.validcount)
		    continue;
		
		line.validcount = R.validcount;
			
		v1 = line.v1;
		v2 = line.v2;
		s1 = strace.DivlineSide (v1.x,v1.y);
		s2 = strace.DivlineSide (v2.x, v2.y);

		// line isn't crossed?
		if (s1 == s2)
		    continue;
		
		divl.x = v1.x;
		divl.y = v1.y;
		divl.dx = v2.x - v1.x;
		divl.dy = v2.y - v1.y;
		s1 = divl.DivlineSide (strace.x, strace.y);
		s2 = divl.DivlineSide (t2x, t2y);

		// line isn't crossed?
		if (s1 == s2)
		    continue;	

		// stop because it is not two sided anyway
		// might do this after updating validcount?
		if ( !flags(line.flags, ML_TWOSIDED) )
		    return false;
		
		// crosses a two sided line
		front = LL.segs[seg].frontsector;
		back = LL.segs[seg].backsector;

		// no wall to block sight with?
		if (front.floorheight == back.floorheight
		    && front.ceilingheight == back.ceilingheight)
		    continue;	

		// possible occluder
		// because of ceiling height differences
		if (front.ceilingheight < back.ceilingheight)
		    opentop = front.ceilingheight;
		else
		    opentop = back.ceilingheight;

		// because of ceiling height differences
		if (front.floorheight > back.floorheight)
		    openbottom = front.floorheight;
		else
		    openbottom = back.floorheight;
			
		// quick test for totally closed doors
		if (openbottom >= opentop)	
		    return false;		// stop
		
		frac = P_InterceptVector2 (strace, divl);
			
		if (front.floorheight != back.floorheight)
		{
		    slope = FixedDiv (openbottom - sightzstart , frac);
		    if (slope > bottomslope)
			bottomslope = slope;
		}
			
		if (front.ceilingheight != back.ceilingheight)
		{
		    slope = FixedDiv (opentop - sightzstart , frac);
		    if (slope < topslope)
			topslope = slope;
		}
			
		if (topslope <= bottomslope)
		    return false;		// stop				
	    }
	    // passed the subsector ok
	    return true;		
	}



	//
	// P_CrossBSPNode
	// Returns true
	//  if strace crosses the given node successfully.
	//
	boolean P_CrossBSPNode (int bspnum)
	{
	    node_t	bsp;
	    int		side;

	    if (flags(bspnum, NF_SUBSECTOR))
	    {
		if (bspnum == -1)
		    return P_CrossSubsector (0);
		else
		    return P_CrossSubsector (bspnum&(~NF_SUBSECTOR));
	    }
			
	    bsp = LL.nodes[bspnum];
	    
	    // decide which side the start point is on
	    side = bsp.DivlineSide (strace.x, strace.y);
	    if (side == 2)
		side = 0;	// an "on" should cross both sides

	    // cross the starting side
	    if (!P_CrossBSPNode (bsp.children[side]) )
		return false;
		
	    // the partition plane is crossed here
	    if (side == bsp.DivlineSide (t2x, t2y))
	    {
		// the line doesn't touch the other side
		return true;
	    }
	    
	    // cross the ending side		
	    return P_CrossBSPNode (bsp.children[side^1]);
	}


	//
	// P_CheckSight
	// Returns true
	//  if a straight line between t1 and t2 is unobstructed.
	// Uses REJECT.
	//
	private boolean
	P_CheckSight
	( mobj_t	t1,
	  mobj_t	t2 )
	{
	    int		s1;
	    int		s2;
	    int		pnum;
	    int		bytenum;
	    int		bitnum;
	    
	    // First check for trivial rejection.

	    // Determine subsector entries in REJECT table.
	    s1 = t1.subsector.sector.id; //(t1.subsector.sector - sectors);
	    s2 = t2.subsector.sector.id;// - sectors);
	    pnum = s1*LL.numsectors + s2;
	    bytenum = pnum>>3;
	    bitnum = 1 << (pnum&7);

	    // Check in REJECT table.
	    if (flags(LL.rejectmatrix[bytenum],bitnum))
	    {
		sightcounts[0]++;

		// can't possibly be connected
		return false;	
	    }

	    // An unobstructed LOS is possible.
	    // Now look from eyes of t1 to any part of t2.
	    sightcounts[1]++;

	    R.validcount++;
		
	    sightzstart = t1.z + t1.height - (t1.height>>2);
	    topslope = (t2.z+t2.height) - sightzstart;
	    bottomslope = (t2.z) - sightzstart;
		
	    strace.x = t1.x;
	    strace.y = t1.y;
	    t2x = t2.x;
	    t2y = t2.y;
	    strace.dx = t2.x - t1.x;
	    strace.dy = t2.y - t1.y;

	    // the head node is the last node output
	    return P_CrossBSPNode (LL.numnodes-1);	
	}
}

public class Specials{
    public static enum typedef
    {
        ok,
        crushed,
        pastdest
        
    };

    public static final int MAXANIMS =32;
    //
//  Animating line specials
//
    public static final int MAXLINEANIMS=64;

    public static short   numlinespecials;
    
    public static anim_t[]   anims=new anim_t[MAXANIMS];
    // MAES: was a pointer
    public static anim_t  lastanim;

    
    
 //
 // P_InitPicAnims
 //

 // Floor/ceiling animation sequences,
 //  defined by first and last frame,
 //  i.e. the flat (64x64 tile) name to
 //  be used.
 // The full animation sequence is given
 //  using all the flats between the start
 //  and end entry, in the order found in
 //  the WAD file.
 //
protected static animdef_t[]       animdefs =
 {
     new animdef_t(false, "NUKAGE3",  "NUKAGE1",  8),
     new animdef_t(false, "FWATER4",  "FWATER1",  8),
     new animdef_t(false, "SWATER4",  "SWATER1",  8),
     new animdef_t(false, "LAVA4",    "LAVA1",    8),
     new animdef_t(false, "BLOOD3",   "BLOOD1",   8),

     // DOOM II flat animations.
     new animdef_t(false, "RROCK08",  "RROCK05",  8),     
         new animdef_t(false, "SLIME04",  "SLIME01",  8),
             new animdef_t(false, "SLIME08",  "SLIME05",  8),
                 new animdef_t(false, "SLIME12",  "SLIME09",  8),

     new animdef_t(true,  "BLODGR4",  "BLODGR1",  8),
     new animdef_t(true,  "SLADRIP3", "SLADRIP1", 8),

     new animdef_t(true,  "BLODRIP4", "BLODRIP1", 8),
     new animdef_t(true,  "FIREWALL", "FIREWALA", 8),
     new animdef_t(true,  "GSTFONT3", "GSTFONT1", 8),
     new animdef_t(true,  "FIRELAVA", "FIRELAV3", 8),
     new animdef_t(true,  "FIREMAG3", "FIREMAG1", 8),
     new animdef_t(true,  "FIREBLU2", "FIREBLU1", 8),
     new animdef_t(true,  "ROCKRED3", "ROCKRED1", 8),

     new animdef_t(true,  "BFALL4",   "BFALL1",   8),
     new animdef_t(true,  "SFALL4",   "SFALL1",   8),
     new animdef_t(true,  "WFALL4",   "WFALL1",   8),
     new animdef_t(true,  "DBRAIN4",  "DBRAIN1",  8),
     // Maes: what bullshit. {-1}? Really?!
     new animdef_t(false, "","",0)
 };

public void InitPicAnims ()
{
    int     i;

    
    //  Init animation. MAES: sneaky base pointer conversion ;-)
    lastanim = anims[0];
    //MAES: for (i=0 ; animdefs[i].istexture != -1 ; i++)
    for (i=0 ; animdefs[i].istexture ; i++)
    {
    if (animdefs[i].istexture)
    {
        // different episode ?
        // TODO:
        //if (R_CheckTextureNumForName(animdefs[i].startname) == -1)
        //continue;   

        lastanim.picnum = R_TextureNumForName (animdefs[i].endname);
        lastanim.picnum = R_TextureNumForName (animdefs[i].startname);
    }
    else
    {
        if (W_CheckNumForName(animdefs[i].startname) == -1)
        continue;

        lastanim.picnum = R_FlatNumForName (animdefs[i].endname);
        lastanim.basepic = R_FlatNumForName (animdefs[i].startname);
    }

    lastanim.istexture = animdefs[i].istexture;
    lastanim.numpics = lastanim.picnum - lastanim.basepic + 1;

    if (lastanim.numpics < 2)
        I_Error ("P_InitPicAnims: bad cycle from %s to %s",
             animdefs[i].startname,
             animdefs[i].endname);
    
    lastanim.speed = animdefs[i].speed;
    lastanim++;
    }
    
}



//
// UTILITIES
//



//
// getSide()
// Will return a side_t*
//  given the number of the current sector,
//  the line number, and the side (0/1) that you want.
//
side_t
getSide
( int       currentSector,
  int       line,
  int       side )
{
    return sides[ (sectors[currentSector].lines[line]).sidenum[side] ];
}


//
// getSector()
// Will return a sector_t*
//  given the number of the current sector,
//  the line number and the side (0/1) that you want.
//
sector_t*
getSector
( int       currentSector,
  int       line,
  int       side )
{
    return sides[ (sectors[currentSector].lines[line]).sidenum[side] ].sector;
}


//
// twoSided()
// Given the sector number and the line number,
//  it will tell you whether the line is two-sided or not.
//
int
twoSided
( int   sector,
  int   line )
{
    return (sectors[sector].lines[line]).flags & ML_TWOSIDED;
}




//
// getNextSector()
// Return sector_t * of sector next to current.
// NULL if not two-sided line
//
sector_t*
getNextSector
( line_t*   line,
  sector_t* sec )
{
    if (!(line.flags & ML_TWOSIDED))
    return NULL;
        
    if (line.frontsector == sec)
    return line.backsector;
    
    return line.frontsector;
}



//
// P_FindLowestFloorSurrounding()
// FIND LOWEST FLOOR HEIGHT IN SURROUNDING SECTORS
//
fixed_t P_FindLowestFloorSurrounding(sector_t* sec)
{
    int         i;
    line_t*     check;
    sector_t*       other;
    fixed_t     floor = sec.floorheight;
    
    for (i=0 ;i < sec.linecount ; i++)
    {
    check = sec.lines[i];
    other = getNextSector(check,sec);

    if (!other)
        continue;
    
    if (other.floorheight < floor)
        floor = other.floorheight;
    }
    return floor;
}



//
// P_FindHighestFloorSurrounding()
// FIND HIGHEST FLOOR HEIGHT IN SURROUNDING SECTORS
//
fixed_t P_FindHighestFloorSurrounding(sector_t *sec)
{
    int         i;
    line_t*     check;
    sector_t*       other;
    fixed_t     floor = -500*FRACUNIT;
    
    for (i=0 ;i < sec.linecount ; i++)
    {
    check = sec.lines[i];
    other = getNextSector(check,sec);
    
    if (!other)
        continue;
    
    if (other.floorheight > floor)
        floor = other.floorheight;
    }
    return floor;
}



//
// P_FindNextHighestFloor
// FIND NEXT HIGHEST FLOOR IN SURROUNDING SECTORS
// Note: this should be doable w/o a fixed array.

// 20 adjoining sectors max!
#define MAX_ADJOINING_SECTORS       20

fixed_t
P_FindNextHighestFloor
( sector_t* sec,
  int       currentheight )
{
    int         i;
    int         h;
    int         min;
    line_t*     check;
    sector_t*       other;
    fixed_t     height = currentheight;

    
    fixed_t     heightlist[MAX_ADJOINING_SECTORS];      

    for (i=0, h=0 ;i < sec.linecount ; i++)
    {
    check = sec.lines[i];
    other = getNextSector(check,sec);

    if (!other)
        continue;
    
    if (other.floorheight > height)
        heightlist[h++] = other.floorheight;

    // Check for overflow. Exit.
    if ( h >= MAX_ADJOINING_SECTORS )
    {
        fprintf( stderr,
             "Sector with more than 20 adjoining sectors\n" );
        break;
    }
    }
    
    // Find lowest height in list
    if (!h)
    return currentheight;
        
    min = heightlist[0];
    
    // Range checking? 
    for (i = 1;i < h;i++)
    if (heightlist[i] < min)
        min = heightlist[i];
            
    return min;
}


//
// FIND LOWEST CEILING IN THE SURROUNDING SECTORS
//
fixed_t
P_FindLowestCeilingSurrounding(sector_t* sec)
{
    int         i;
    line_t*     check;
    sector_t*       other;
    fixed_t     height = MAXINT;
    
    for (i=0 ;i < sec.linecount ; i++)
    {
    check = sec.lines[i];
    other = getNextSector(check,sec);

    if (!other)
        continue;

    if (other.ceilingheight < height)
        height = other.ceilingheight;
    }
    return height;
}


//
// FIND HIGHEST CEILING IN THE SURROUNDING SECTORS
//
fixed_t P_FindHighestCeilingSurrounding(sector_t* sec)
{
    int     i;
    line_t* check;
    sector_t*   other;
    fixed_t height = 0;
    
    for (i=0 ;i < sec.linecount ; i++)
    {
    check = sec.lines[i];
    other = getNextSector(check,sec);

    if (!other)
        continue;

    if (other.ceilingheight > height)
        height = other.ceilingheight;
    }
    return height;
}



//
// RETURN NEXT SECTOR # THAT LINE TAG REFERS TO
//
int
P_FindSectorFromLineTag
( line_t*   line,
  int       start )
{
    int i;
    
    for (i=start+1;i<numsectors;i++)
    if (sectors[i].tag == line.tag)
        return i;
    
    return -1;
}




//
// Find minimum light from an adjacent sector
//
int
P_FindMinSurroundingLight
( sector_t* sector,
  int       max )
{
    int     i;
    int     min;
    line_t* line;
    sector_t*   check;
    
    min = max;
    for (i=0 ; i < sector.linecount ; i++)
    {
    line = sector.lines[i];
    check = getNextSector(line,sector);

    if (!check)
        continue;

    if (check.lightlevel < min)
        min = check.lightlevel;
    }
    return min;
}



//
// EVENTS
// Events are operations triggered by using, crossing,
// or shooting special lines, or by timed thinkers.
//

//
// P_CrossSpecialLine - TRIGGER
// Called every time a thing origin is about
//  to cross a line with a non 0 special.
//
void
P_CrossSpecialLine
( int       linenum,
  int       side,
  mobj_t  thing )
{
    line_t line;
    int     ok;

    line = &lines[linenum];
    
    //  Triggers that other things can activate
    if (!thing.player)
    {
    // Things that should NOT trigger specials...
    switch(thing.type)
    {
      case MT_ROCKET:
      case MT_PLASMA:
      case MT_BFG:
      case MT_TROOPSHOT:
      case MT_HEADSHOT:
      case MT_BRUISERSHOT:
        return;
        break;
        
      default: break;
    }
        
    ok = 0;
    switch(line.special)
    {
      case 39:  // TELEPORT TRIGGER
      case 97:  // TELEPORT RETRIGGER
      case 125: // TELEPORT MONSTERONLY TRIGGER
      case 126: // TELEPORT MONSTERONLY RETRIGGER
      case 4:   // RAISE DOOR
      case 10:  // PLAT DOWN-WAIT-UP-STAY TRIGGER
      case 88:  // PLAT DOWN-WAIT-UP-STAY RETRIGGER
        ok = 1;
        break;
    }
    if (!ok)
        return;
    }

    
    // Note: could use some const's here.
    switch (line.special)
    {
    // TRIGGERS.
    // All from here to RETRIGGERS.
      case 2:
    // Open Door
    EV_DoDoor(line,open);
    line.special = 0;
    break;

      case 3:
    // Close Door
    EV_DoDoor(line,close);
    line.special = 0;
    break;

      case 4:
    // Raise Door
    EV_DoDoor(line,normal);
    line.special = 0;
    break;
    
      case 5:
    // Raise Floor
    EV_DoFloor(line,raiseFloor);
    line.special = 0;
    break;
    
      case 6:
    // Fast Ceiling Crush & Raise
    EV_DoCeiling(line,fastCrushAndRaise);
    line.special = 0;
    break;
    
      case 8:
    // Build Stairs
    EV_BuildStairs(line,build8);
    line.special = 0;
    break;
    
      case 10:
    // PlatDownWaitUp
    EV_DoPlat(line,downWaitUpStay,0);
    line.special = 0;
    break;
    
      case 12:
    // Light Turn On - brightest near
    EV_LightTurnOn(line,0);
    line.special = 0;
    break;
    
      case 13:
    // Light Turn On 255
    EV_LightTurnOn(line,255);
    line.special = 0;
    break;
    
      case 16:
    // Close Door 30
    EV_DoDoor(line,close30ThenOpen);
    line.special = 0;
    break;
    
      case 17:
    // Start Light Strobing
    EV_StartLightStrobing(line);
    line.special = 0;
    break;
    
      case 19:
    // Lower Floor
    EV_DoFloor(line,lowerFloor);
    line.special = 0;
    break;
    
      case 22:
    // Raise floor to nearest height and change texture
    EV_DoPlat(line,raiseToNearestAndChange,0);
    line.special = 0;
    break;
    
      case 25:
    // Ceiling Crush and Raise
    EV_DoCeiling(line,crushAndRaise);
    line.special = 0;
    break;
    
      case 30:
    // Raise floor to shortest texture height
    //  on either side of lines.
    EV_DoFloor(line,raiseToTexture);
    line.special = 0;
    break;
    
      case 35:
    // Lights Very Dark
    EV_LightTurnOn(line,35);
    line.special = 0;
    break;
    
      case 36:
    // Lower Floor (TURBO)
    EV_DoFloor(line,turboLower);
    line.special = 0;
    break;
    
      case 37:
    // LowerAndChange
    EV_DoFloor(line,lowerAndChange);
    line.special = 0;
    break;
    
      case 38:
    // Lower Floor To Lowest
    EV_DoFloor( line, lowerFloorToLowest );
    line.special = 0;
    break;
    
      case 39:
    // TELEPORT!
    EV_Teleport( line, side, thing );
    line.special = 0;
    break;

      case 40:
    // RaiseCeilingLowerFloor
    EV_DoCeiling( line, raiseToHighest );
    EV_DoFloor( line, lowerFloorToLowest );
    line.special = 0;
    break;
    
      case 44:
    // Ceiling Crush
    EV_DoCeiling( line, lowerAndCrush );
    line.special = 0;
    break;
    
      case 52:
    // EXIT!
    G_ExitLevel ();
    break;
    
      case 53:
    // Perpetual Platform Raise
    EV_DoPlat(line,perpetualRaise,0);
    line.special = 0;
    break;
    
      case 54:
    // Platform Stop
    EV_StopPlat(line);
    line.special = 0;
    break;

      case 56:
    // Raise Floor Crush
    EV_DoFloor(line,raiseFloorCrush);
    line.special = 0;
    break;

      case 57:
    // Ceiling Crush Stop
    EV_CeilingCrushStop(line);
    line.special = 0;
    break;
    
      case 58:
    // Raise Floor 24
    EV_DoFloor(line,raiseFloor24);
    line.special = 0;
    break;

      case 59:
    // Raise Floor 24 And Change
    EV_DoFloor(line,raiseFloor24AndChange);
    line.special = 0;
    break;
    
      case 104:
    // Turn lights off in sector(tag)
    EV_TurnTagLightsOff(line);
    line.special = 0;
    break;
    
      case 108:
    // Blazing Door Raise (faster than TURBO!)
    EV_DoDoor (line,blazeRaise);
    line.special = 0;
    break;
    
      case 109:
    // Blazing Door Open (faster than TURBO!)
    EV_DoDoor (line,blazeOpen);
    line.special = 0;
    break;
    
      case 100:
    // Build Stairs Turbo 16
    EV_BuildStairs(line,turbo16);
    line.special = 0;
    break;
    
      case 110:
    // Blazing Door Close (faster than TURBO!)
    EV_DoDoor (line,blazeClose);
    line.special = 0;
    break;

      case 119:
    // Raise floor to nearest surr. floor
    EV_DoFloor(line,raiseFloorToNearest);
    line.special = 0;
    break;
    
      case 121:
    // Blazing PlatDownWaitUpStay
    EV_DoPlat(line,blazeDWUS,0);
    line.special = 0;
    break;
    
      case 124:
    // Secret EXIT
    G_SecretExitLevel ();
    break;
        
      case 125:
    // TELEPORT MonsterONLY
    if (!thing.player)
    {
        EV_Teleport( line, side, thing );
        line.special = 0;
    }
    break;
    
      case 130:
    // Raise Floor Turbo
    EV_DoFloor(line,raiseFloorTurbo);
    line.special = 0;
    break;
    
      case 141:
    // Silent Ceiling Crush & Raise
    EV_DoCeiling(line,silentCrushAndRaise);
    line.special = 0;
    break;
    
    // RETRIGGERS.  All from here till end.
      case 72:
    // Ceiling Crush
    EV_DoCeiling( line, lowerAndCrush );
    break;

      case 73:
    // Ceiling Crush and Raise
    EV_DoCeiling(line,crushAndRaise);
    break;

      case 74:
    // Ceiling Crush Stop
    EV_CeilingCrushStop(line);
    break;
    
      case 75:
    // Close Door
    EV_DoDoor(line,close);
    break;
    
      case 76:
    // Close Door 30
    EV_DoDoor(line,close30ThenOpen);
    break;
    
      case 77:
    // Fast Ceiling Crush & Raise
    EV_DoCeiling(line,fastCrushAndRaise);
    break;
    
      case 79:
    // Lights Very Dark
    EV_LightTurnOn(line,35);
    break;
    
      case 80:
    // Light Turn On - brightest near
    EV_LightTurnOn(line,0);
    break;
    
      case 81:
    // Light Turn On 255
    EV_LightTurnOn(line,255);
    break;
    
      case 82:
    // Lower Floor To Lowest
    EV_DoFloor( line, lowerFloorToLowest );
    break;
    
      case 83:
    // Lower Floor
    EV_DoFloor(line,lowerFloor);
    break;

      case 84:
    // LowerAndChange
    EV_DoFloor(line,lowerAndChange);
    break;

      case 86:
    // Open Door
    EV_DoDoor(line,open);
    break;
    
      case 87:
    // Perpetual Platform Raise
    EV_DoPlat(line,perpetualRaise,0);
    break;
    
      case 88:
    // PlatDownWaitUp
    EV_DoPlat(line,downWaitUpStay,0);
    break;
    
      case 89:
    // Platform Stop
    EV_StopPlat(line);
    break;
    
      case 90:
    // Raise Door
    EV_DoDoor(line,normal);
    break;
    
      case 91:
    // Raise Floor
    EV_DoFloor(line,raiseFloor);
    break;
    
      case 92:
    // Raise Floor 24
    EV_DoFloor(line,raiseFloor24);
    break;
    
      case 93:
    // Raise Floor 24 And Change
    EV_DoFloor(line,raiseFloor24AndChange);
    break;
    
      case 94:
    // Raise Floor Crush
    EV_DoFloor(line,raiseFloorCrush);
    break;
    
      case 95:
    // Raise floor to nearest height
    // and change texture.
    EV_DoPlat(line,raiseToNearestAndChange,0);
    break;
    
      case 96:
    // Raise floor to shortest texture height
    // on either side of lines.
    EV_DoFloor(line,raiseToTexture);
    break;
    
      case 97:
    // TELEPORT!
    EV_Teleport( line, side, thing );
    break;
    
      case 98:
    // Lower Floor (TURBO)
    EV_DoFloor(line,turboLower);
    break;

      case 105:
    // Blazing Door Raise (faster than TURBO!)
    EV_DoDoor (line,blazeRaise);
    break;
    
      case 106:
    // Blazing Door Open (faster than TURBO!)
    EV_DoDoor (line,blazeOpen);
    break;

      case 107:
    // Blazing Door Close (faster than TURBO!)
    EV_DoDoor (line,blazeClose);
    break;

      case 120:
    // Blazing PlatDownWaitUpStay.
    EV_DoPlat(line,blazeDWUS,0);
    break;
    
      case 126:
    // TELEPORT MonsterONLY.
    if (!thing.player)
        EV_Teleport( line, side, thing );
    break;
    
      case 128:
    // Raise To Nearest Floor
    EV_DoFloor(line,raiseFloorToNearest);
    break;
    
      case 129:
    // Raise Floor Turbo
    EV_DoFloor(line,raiseFloorTurbo);
    break;
    }
}



/**
 * P_ShootSpecialLine - IMPACT SPECIALS
 * Called when a thing shoots a special line.
 */
public void
ShootSpecialLine
( mobj_t   thing,
  line_t   line )
{
    int     ok;
    
    //  Impacts that other things can activate.
    if (thing.player==null)
    {
    ok = 0;
    switch(line.special)
    {
      case 46:
        // OPEN DOOR IMPACT
        ok = 1;
        break;
    }
    if (!ok)
        return;
    }

    switch(line.special)
    {
      case 24:
    // RAISE FLOOR
    EV_DoFloor(line,raiseFloor);
    P_ChangeSwitchTexture(line,0);
    break;
    
      case 46:
    // OPEN DOOR
    EV_DoDoor(line,open);
    P_ChangeSwitchTexture(line,1);
    break;
    
      case 47:
    // RAISE FLOOR NEAR AND CHANGE
    EV_DoPlat(line,raiseToNearestAndChange,0);
    P_ChangeSwitchTexture(line,0);
    break;
    }
}



//
// P_PlayerInSpecialSector
// Called every tic frame
//  that the player origin is in a special sector
//
public void PlayerInSpecialSector (player_t player)
{
    sector_t   sector;
    
    sector = player.mo.subsector.sector;

    // Falling, not all the way down yet?
    if (player.mo.z != sector.floorheight)
    return; 

    // Has hitten ground.
    switch (sector.special)
    {
      case 5:
    // HELLSLIME DAMAGE
    if (!player.powers[pw_ironfeet])
        if (!(leveltime&0x1f))
        DamageMobj (player.mo, NULL, NULL, 10);
    break;
    
      case 7:
    // NUKAGE DAMAGE
    if (!player.powers[pw_ironfeet])
        if (!(leveltime&0x1f))
        P_DamageMobj (player.mo, NULL, NULL, 5);
    break;
    
      case 16:
    // SUPER HELLSLIME DAMAGE
      case 4:
    // STROBE HURT
    if (!player.powers[pw_ironfeet]
        || (P_Random()<5) )
    {
        if (!(leveltime&0x1f))
        P_DamageMobj (player.mo, NULL, NULL, 20);
    }
    break;
            
      case 9:
    // SECRET SECTOR
    player.secretcount++;
    sector.special = 0;
    break;
            
      case 11:
    // EXIT SUPER DAMAGE! (for E1M8 finale)
    player.cheats &= ~CF_GODMODE;

    if (!(leveltime&0x1f))
        P_DamageMobj (player.mo, NULL, NULL, 20);

    if (player.health <= 10)
        G_ExitLevel();
    break;
            
      default:
    I_Error ("P_PlayerInSpecialSector: "
         "unknown special %i",
         sector.special);
    break;
    };
}




//
// P_UpdateSpecials
// Animate planes, scroll walls, etc.
//
boolean     levelTimer;
int     levelTimeCount;

void P_UpdateSpecials ()
{
    anim_t* anim;
    int     pic;
    int     i;
    line_t* line;

    
    //  LEVEL TIMER
    if (levelTimer == true)
    {
    levelTimeCount--;
    if (!levelTimeCount)
        G_ExitLevel();
    }
    
    //  ANIMATE FLATS AND TEXTURES GLOBALLY
    for (anim = anims ; anim < lastanim ; anim++)
    {
    for (i=anim.basepic ; i<anim.basepic+anim.numpics ; i++)
    {
        pic = anim.basepic + ( (leveltime/anim.speed + i)%anim.numpics );
        if (anim.istexture)
        texturetranslation[i] = pic;
        else
        flattranslation[i] = pic;
    }
    }

    
    //  ANIMATE LINE SPECIALS
    for (i = 0; i < numlinespecials; i++)
    {
    line = linespeciallist[i];
    switch(line.special)
    {
      case 48:
        // EFFECT FIRSTCOL SCROLL +
        sides[line.sidenum[0]].textureoffset += FRACUNIT;
        break;
    }
    }

    
    //  DO BUTTONS
    for (i = 0; i < MAXBUTTONS; i++)
    if (buttonlist[i].btimer)
    {
        buttonlist[i].btimer--;
        if (!buttonlist[i].btimer)
        {
        switch(buttonlist[i].where)
        {
          case top:
            sides[buttonlist[i].line.sidenum[0]].toptexture =
            buttonlist[i].btexture;
            break;
            
          case middle:
            sides[buttonlist[i].line.sidenum[0]].midtexture =
            buttonlist[i].btexture;
            break;
            
          case bottom:
            sides[buttonlist[i].line.sidenum[0]].bottomtexture =
            buttonlist[i].btexture;
            break;
        }
        S_StartSound((mobj_t *)&buttonlist[i].soundorg,sfx_swtchn);
        memset(&buttonlist[i],0,sizeof(button_t));
        }
    }
    
}



//
// Special Stuff that can not be categorized
//
int EV_DoDonut(line_t*  line)
{
    sector_t*       s1;
    sector_t*       s2;
    sector_t*       s3;
    int         secnum;
    int         rtn;
    int         i;
    floormove_t*    floor;
    
    secnum = -1;
    rtn = 0;
    while ((secnum = P_FindSectorFromLineTag(line,secnum)) >= 0)
    {
    s1 = &sectors[secnum];
        
    // ALREADY MOVING?  IF SO, KEEP GOING...
    if (s1.specialdata)
        continue;
            
    rtn = 1;
    s2 = getNextSector(s1.lines[0],s1);
    for (i = 0;i < s2.linecount;i++)
    {
        if ((!s2.lines[i].flags & ML_TWOSIDED) ||
        (s2.lines[i].backsector == s1))
        continue;
        s3 = s2.lines[i].backsector;
        
        //  Spawn rising slime
        floor = Z_Malloc (sizeof(*floor), PU_LEVSPEC, 0);
        P_AddThinker (&floor.thinker);
        s2.specialdata = floor;
        floor.thinker.function.acp1 = (actionf_p1) T_MoveFloor;
        floor.type = donutRaise;
        floor.crush = false;
        floor.direction = 1;
        floor.sector = s2;
        floor.speed = FLOORSPEED / 2;
        floor.texture = s3.floorpic;
        floor.newspecial = 0;
        floor.floordestheight = s3.floorheight;
        
        //  Spawn lowering donut-hole
        floor = Z_Malloc (sizeof(*floor), PU_LEVSPEC, 0);
        P_AddThinker (&floor.thinker);
        s1.specialdata = floor;
        floor.thinker.function.acp1 = (actionf_p1) T_MoveFloor;
        floor.type = lowerFloor;
        floor.crush = false;
        floor.direction = -1;
        floor.sector = s1;
        floor.speed = FLOORSPEED / 2;
        floor.floordestheight = s3.floorheight;
        break;
    }
    }
    return rtn;
}



//
// SPECIAL SPAWNING
//

//
// P_SpawnSpecials
// After the map has been loaded, scan for specials
//  that spawn thinkers
//
short       numlinespecials;
line_t*     linespeciallist[MAXLINEANIMS];


// Parses command line parameters.
void P_SpawnSpecials ()
{
    sector_t*   sector;
    int     i;
    int     episode;

    episode = 1;
    if (W_CheckNumForName("texture2") >= 0)
    episode = 2;

    
    // See if -TIMER needs to be used.
    levelTimer = false;
    
    i = M_CheckParm("-avg");
    if (i && deathmatch)
    {
    levelTimer = true;
    levelTimeCount = 20 * 60 * 35;
    }
    
    i = M_CheckParm("-timer");
    if (i && deathmatch)
    {
    int time;
    time = atoi(myargv[i+1]) * 60 * 35;
    levelTimer = true;
    levelTimeCount = time;
    }
    
    //  Init special SECTORs.
    sector = sectors;
    for (i=0 ; i<numsectors ; i++, sector++)
    {
    if (!sector.special)
        continue;
    
    switch (sector.special)
    {
      case 1:
        // FLICKERING LIGHTS
        P_SpawnLightFlash (sector);
        break;

      case 2:
        // STROBE FAST
        P_SpawnStrobeFlash(sector,FASTDARK,0);
        break;
        
      case 3:
        // STROBE SLOW
        P_SpawnStrobeFlash(sector,SLOWDARK,0);
        break;
        
      case 4:
        // STROBE FAST/DEATH SLIME
        P_SpawnStrobeFlash(sector,FASTDARK,0);
        sector.special = 4;
        break;
        
      case 8:
        // GLOWING LIGHT
        P_SpawnGlowingLight(sector);
        break;
      case 9:
        // SECRET SECTOR
        totalsecret++;
        break;
        
      case 10:
        // DOOR CLOSE IN 30 SECONDS
        P_SpawnDoorCloseIn30 (sector);
        break;
        
      case 12:
        // SYNC STROBE SLOW
        P_SpawnStrobeFlash (sector, SLOWDARK, 1);
        break;

      case 13:
        // SYNC STROBE FAST
        P_SpawnStrobeFlash (sector, FASTDARK, 1);
        break;

      case 14:
        // DOOR RAISE IN 5 MINUTES
        P_SpawnDoorRaiseIn5Mins (sector, i);
        break;
        
      case 17:
        P_SpawnFireFlicker(sector);
        break;
    }
    }

    
    //  Init line EFFECTs
    numlinespecials = 0;
    for (i = 0;i < numlines; i++)
    {
    switch(lines[i].special)
    {
      case 48:
        // EFFECT FIRSTCOL SCROLL+
        linespeciallist[numlinespecials] = &lines[i];
        numlinespecials++;
        break;
    }
    }

    
    //  Init other misc stuff
    for (i = 0;i < MAXCEILINGS;i++)
    activeceilings[i] = NULL;

    for (i = 0;i < MAXPLATS;i++)
    activeplats[i] = NULL;
    
    for (i = 0;i < MAXBUTTONS;i++)
    memset(&buttonlist[i],0,sizeof(button_t));

    // UNUSED: no horizonal sliders.
    //  P_InitSlidingDoorFrames();
}


}

class Ceilings{

    //
    // CEILINGS
    //


    ceiling_t[]  activeceilings=new ceiling_t[MAXCEILINGS];


    //
    // T_MoveCeiling
    //

    void T_MoveCeiling (ceiling_t ceiling)
    {
        result_e    res;
        
        switch(ceiling.direction)
        {
          case 0:
        // IN STASIS
        break;
          case 1:
        // UP
        res = T_MovePlane(ceiling.sector,
                  ceiling.speed,
                  ceiling.topheight,
                  false,1,ceiling.direction);
        
        if (!(leveltime&7))
        {
            switch(ceiling.type)
            {
              case silentCrushAndRaise:
            break;
              default:
            S_StartSound((mobj_t *)&ceiling.sector.soundorg,
                     sfx_stnmov);
            // ?
            break;
            }
        }
        
        if (res == pastdest)
        {
            switch(ceiling.type)
            {
              case raiseToHighest:
            P_RemoveActiveCeiling(ceiling);
            break;
            
              case silentCrushAndRaise:
            S_StartSound((mobj_t *)&ceiling.sector.soundorg,
                     sfx_pstop);
              case fastCrushAndRaise:
              case crushAndRaise:
            ceiling.direction = -1;
            break;
            
              default:
            break;
            }
            
        }
        break;
        
          case -1:
        // DOWN
        res = T_MovePlane(ceiling.sector,
                  ceiling.speed,
                  ceiling.bottomheight,
                  ceiling.crush,1,ceiling.direction);
        
        if (!(leveltime&7))
        {
            switch(ceiling.type)
            {
              case silentCrushAndRaise: break;
              default:
            S_StartSound((mobj_t *)&ceiling.sector.soundorg,
                     sfx_stnmov);
            }
        }
        
        if (res == pastdest)
        {
            switch(ceiling.type)
            {
              case silentCrushAndRaise:
            S_StartSound((mobj_t *)&ceiling.sector.soundorg,
                     sfx_pstop);
              case crushAndRaise:
            ceiling.speed = CEILSPEED;
              case fastCrushAndRaise:
            ceiling.direction = 1;
            break;

              case lowerAndCrush:
              case lowerToFloor:
            P_RemoveActiveCeiling(ceiling);
            break;

              default:
            break;
            }
        }
        else // ( res != pastdest )
        {
            if (res == crushed)
            {
            switch(ceiling.type)
            {
              case silentCrushAndRaise:
              case crushAndRaise:
              case lowerAndCrush:
                ceiling.speed = CEILSPEED / 8;
                break;

              default:
                break;
            }
            }
        }
        break;
        }
    }


    //
    // EV_DoCeiling
    // Move a ceiling up/down and all around!
    //
    int
    EV_DoCeiling
    ( line_t   line,
      ceiling_e type )
    {
        int     secnum;
        int     rtn;
        sector_t   sec;
        ceiling_t  ceiling;
        
        secnum = -1;
        rtn = 0;
        
        //  Reactivate in-stasis ceilings...for certain types.
        switch(type)
        {
          case fastCrushAndRaise:
          case silentCrushAndRaise:
          case crushAndRaise:
        P_ActivateInStasisCeiling(line);
          default:
        break;
        }
        
        while ((secnum = P_FindSectorFromLineTag(line,secnum)) >= 0)
        {
        sec = &sectors[secnum];
        if (sec.specialdata)
            continue;
        
        // new door thinker
        rtn = 1;
        ceiling = Z_Malloc (sizeof(*ceiling), PU_LEVSPEC, 0);
        P_AddThinker (&ceiling.thinker);
        sec.specialdata = ceiling;
        ceiling.thinker.function.acp1 = (actionf_p1)T_MoveCeiling;
        ceiling.sector = sec;
        ceiling.crush = false;
        
        switch(type)
        {
          case fastCrushAndRaise:
            ceiling.crush = true;
            ceiling.topheight = sec.ceilingheight;
            ceiling.bottomheight = sec.floorheight + (8*FRACUNIT);
            ceiling.direction = -1;
            ceiling.speed = CEILSPEED * 2;
            break;

          case silentCrushAndRaise:
          case crushAndRaise:
            ceiling.crush = true;
            ceiling.topheight = sec.ceilingheight;
          case lowerAndCrush:
          case lowerToFloor:
            ceiling.bottomheight = sec.floorheight;
            if (type != lowerToFloor)
            ceiling.bottomheight += 8*FRACUNIT;
            ceiling.direction = -1;
            ceiling.speed = CEILSPEED;
            break;

          case raiseToHighest:
            ceiling.topheight = P_FindHighestCeilingSurrounding(sec);
            ceiling.direction = 1;
            ceiling.speed = CEILSPEED;
            break;
        }
            
        ceiling.tag = sec.tag;
        ceiling.type = type;
        P_AddActiveCeiling(ceiling);
        }
        return rtn;
    }


    //
    // Add an active ceiling
    //
    void P_AddActiveCeiling(ceiling_t c)
    {
        int     i;
        
        for (i = 0; i < MAXCEILINGS;i++)
        {
        if (activeceilings[i] == NULL)
        {
            activeceilings[i] = c;
            return;
        }
        }
    }



    //
    // Remove a ceiling's thinker
    //
    void P_RemoveActiveCeiling(ceiling_t c)
    {
        int     i;
        
        for (i = 0;i < MAXCEILINGS;i++)
        {
        if (activeceilings[i] == c)
        {
            activeceilings[i].sector.specialdata = NULL;
            P_RemoveThinker (&activeceilings[i].thinker);
            activeceilings[i] = NULL;
            break;
        }
        }
    }



    //
    // Restart a ceiling that's in-stasis
    //
    void P_ActivateInStasisCeiling(line_t line)
    {
        int     i;
        
        for (i = 0;i < MAXCEILINGS;i++)
        {
        if (activeceilings[i]
            && (activeceilings[i].tag == line.tag)
            && (activeceilings[i].direction == 0))
        {
            activeceilings[i].direction = activeceilings[i].olddirection;
            activeceilings[i].thinker.function.acp1
              = (actionf_p1)T_MoveCeiling;
        }
        }
    }



    //
    // EV_CeilingCrushStop
    // Stop a ceiling from crushing!
    //
    int EV_CeilingCrushStop(line_t  line)
    {
        int     i;
        int     rtn;
        
        rtn = 0;
        for (i = 0;i < MAXCEILINGS;i++)
        {
        if (activeceilings[i]
            && (activeceilings[i].tag == line.tag)
            && (activeceilings[i].direction != 0))
        {
            activeceilings[i].olddirection = activeceilings[i].direction;
            activeceilings[i].thinker.function.acv = (actionf_v)NULL;
            activeceilings[i].direction = 0;       // in-stasis
            rtn = 1;
        }
        }
        

        return rtn;
    }

    }


    class MapMovement{

        public static final String rcsid = "$Id: UnifiedGameMap.java,v 1.2 2010/09/13 23:09:51 velktron Exp $";

       
        /** fixed_t */
        int[]       tmbbox=new int[4];
        mobj_t      tmthing;
        int     tmflags;
        /** fixed_t */
        int     tmx,    tmy;

        /** If "floatok" true, move would be ok
            if within "tmfloorz - tmceilingz". */
        boolean     floatok;

        /** fixed_t */
        int     tmfloorz, tmceilingz,   tmdropoffz;

        // keep track of the line that lowers the ceiling,
        // so missiles don't explode against sky hack walls
        line_t      ceilingline;



        line_t[]        spechit=new line_t[MAXSPECIALCROSS];
        int     numspechit;



        //
        // TELEPORT MOVE
        // 



        //
        // P_TeleportMove
        //
        private boolean
        TeleportMove
        ( mobj_t    thing,
          int   x, //fixed
          int   y )
        {
            int         xl;
            int         xh;
            int         yl;
            int         yh;
            int         bx;
            int         by;
            
            subsector_t newsubsec;
            
            // kill anything occupying the position
            tmthing = thing;
            tmflags = thing.flags;
            
            tmx = x;
            tmy = y;
            
            tmbbox[BOXTOP] = y + tmthing.radius;
            tmbbox[BOXBOTTOM] = y - tmthing.radius;
            tmbbox[BOXRIGHT] = x + tmthing.radius;
            tmbbox[BOXLEFT] = x - tmthing.radius;

            newsubsec = R.PointInSubsector (x,y);
            ceilingline = null;
            
            // The base floor/ceiling is from the subsector
            // that contains the point.
            // Any contacted lines the step closer together
            // will adjust them.
            tmfloorz = tmdropoffz = newsubsec.sector.floorheight;
            tmceilingz = newsubsec.sector.ceilingheight;
                    
             // TODO: validcount++; // This is r_main's ?
            numspechit = 0;
            
            // stomp on any things contacted
            xl = (tmbbox[BOXLEFT] - LL.bmaporgx - MAXRADIUS)>>MAPBLOCKSHIFT;
            xh = (tmbbox[BOXRIGHT] - LL.bmaporgx + MAXRADIUS)>>MAPBLOCKSHIFT;
            yl = (tmbbox[BOXBOTTOM] - LL.bmaporgy - MAXRADIUS)>>MAPBLOCKSHIFT;
            yh = (tmbbox[BOXTOP] - LL.bmaporgy + MAXRADIUS)>>MAPBLOCKSHIFT;

            for (bx=xl ; bx<=xh ; bx++)
            for (by=yl ; by<=yh ; by++)
                if (!P_BlockThingsIterator(bx,by,PIT_StompThing))
                return false;
            
            // the move is ok,
            // so link the thing into its new position
            UnsetThingPosition (thing);

            thing.floorz = tmfloorz;
            thing.ceilingz = tmceilingz;    
            thing.x = x;
            thing.y = y;

            SetThingPosition (thing);
            
            return true;
        }


        //
        // MOVEMENT ITERATOR FUNCTIONS
        //




        /** PIT_CheckLine
         * Adjusts tmfloorz and tmceilingz as lines are contacted
         */

        class PIT_CheckLine implements PIT_LineFunction {
            
            @Override
            public boolean invoke(line_t ld){
            if (tmbbox[BOXRIGHT] <= ld.bbox[BOXLEFT]
            || tmbbox[BOXLEFT] >= ld.bbox[BOXRIGHT]
            || tmbbox[BOXTOP] <= ld.bbox[BOXBOTTOM]
            || tmbbox[BOXBOTTOM] >= ld.bbox[BOXTOP] )
            return true;

            if (ld.P_BoxOnLineSide (tmbbox) != -1)
            return true;
                
            // A line has been hit
            
            // The moving thing's destination position will cross
            // the given line.
            // If this should not be allowed, return false.
            // If the line is special, keep track of it
            // to process later if the move is proven ok.
            // NOTE: specials are NOT sorted by order,
            // so two special lines that are only 8 pixels apart
            // could be crossed in either order.
            
            if (ld.backsector==null)
            return false;       // one sided line
                
            if (!flags(tmthing.flags, MF_MISSILE) )
            {
            if ( flags(ld.flags, ML_BLOCKING) )
                return false;   // explicitly blocking everything

            if ( (tmthing.player==null) && flags(ld.flags, ML_BLOCKMONSTERS ))
                return false;   // block monsters only
            }

            // set openrange, opentop, openbottom
            P_LineOpening (ld); 
            
            // adjust floor / ceiling heights
            if (opentop < tmceilingz)
            {
            tmceilingz = opentop;
            ceilingline = ld;
            }

            if (openbottom > tmfloorz)
            tmfloorz = openbottom;  

            if (lowfloor < tmdropoffz)
            tmdropoffz = lowfloor;
                
            // if contacted a special line, add it to the list
            if (ld.special)
            {
            spechit[numspechit] = ld;
            numspechit++;
            }

            return true;
            }
        }






        /**PIT_CheckThing  */

        class PIT_CheckThing implements PIT_MobjFunction {
            @Override
            public boolean invoke(mobj_t thing) {
            int     blockdist; // fixed_t
            boolean     solid;
            int         damage;
                
            if ((thing.flags & (MF_SOLID|MF_SPECIAL|MF_SHOOTABLE) )==0)
            return true;
            
            blockdist = thing.radius + tmthing.radius;

            if ( Math.abs(thing.x - tmx) >= blockdist
             || Math.abs(thing.y - tmy) >= blockdist )
            {
            // didn't hit it
            return true;    
            }
            
            // don't clip against self
            if (thing == tmthing)
            return true;
            
            // check for skulls slamming into things
            if ((tmthing.flags & MF_SKULLFLY)!=0)
            {
            damage = ((RND.P_Random()%8)+1)*tmthing.info.damage;
            
            DamageMobj (thing, tmthing, tmthing, damage);
            
            tmthing.flags &= ~MF_SKULLFLY;
            tmthing.momx = tmthing.momy = tmthing.momz = 0;
            
            SetMobjState (tmthing, tmthing.info.spawnstate);
            
            return false;       // stop moving
            }

            
            // missiles can hit other things
            if (tmthing.flags & MF_MISSILE)
            {
            // see if it went over / under
            if (tmthing.z > thing.z + thing.height)
                return true;        // overhead
            if (tmthing.z+tmthing.height < thing.z)
                return true;        // underneath
                
            if (tmthing.target && (
                tmthing.target.type == thing.type || 
                (tmthing.target.type == MT_KNIGHT && thing.type == MT_BRUISER)||
                (tmthing.target.type == MT_BRUISER && thing.type == MT_KNIGHT) ) )
            {
                // Don't hit same species as originator.
                if (thing == tmthing.target)
                return true;

                if (thing.type != MT_PLAYER)
                {
                // Explode, but do no damage.
                // Let players missile other players.
                return false;
                }
            }
            
            if (! (thing.flags & MF_SHOOTABLE) )
            {
                // didn't do any damage
                return !(thing.flags & MF_SOLID);   
            }
            
            // damage / explode
            damage = ((P_Random()%8)+1)*tmthing.info.damage;
            DamageMobj (thing, tmthing, tmthing.target, damage);

            // don't traverse any more
            return false;               
            }
            
            // check for special pickup
            if (thing.flags & MF_SPECIAL)
            {
            solid = thing.flags&MF_SOLID;
            if (tmflags&MF_PICKUP)
            {
                // can remove thing
                TouchSpecialThing (thing, tmthing);
            }
            return !solid;
            }
            
            return !(thing.flags & MF_SOLID);
        }
        }

        //
        //PIT_StompThing
        //

        class PIT_StompThing implements PIT_Iterator{

        public boolean invoke (mobj_t thing)
        {
         int blockdist; // fixed_t
             
         if ((thing.flags & MF_SHOOTABLE)==0 )
         return true;
             
         blockdist = thing.radius + tmthing.radius;
         
         if ( Math.abs(thing.x - tmx) >= blockdist
          || Math.abs(thing.y - tmy) >= blockdist )
         {
         // didn't hit it
         return true;
         }
         
         // don't clip against self
         if (thing == tmthing)
         return true;
         
         // monsters don't stomp things except on boss level
         if ( (tmthing.player==null) && (DS.gamemap != 30))
         return false;   
             
         // TODO: DamageMobj (thing, tmthing, tmthing, 10000); // in interaction
         
         return true;
        }

        }


        //
        // MOVEMENT CLIPPING
        //

        /**
         * P_CheckPosition
         *  This is purely informative, nothing is modified
         *  (except things picked up).
         *  
         *  in:
         *   a mobj_t (can be valid or invalid)
         *   a position to be checked
         *    (doesn't need to be related to the mobj_t.x,y)
         * 
         *  during:
         *   special things are touched if MF_PICKUP
         *   early out on solid lines?
         * 
         *  out:
         *   newsubsec
         *   floorz
         *   ceilingz
         *   tmdropoffz
         *    the lowest point contacted
         *    (monsters won't move to a dropoff)
         *   speciallines[]
         *   numspeciallines
         * @param thing
         * @param x fixed_t
         * @param y fixed_t
        */
        public boolean
        CheckPosition
        ( mobj_t    thing,
          int   x,
          int   y )
        {
            int         xl;
            int         xh;
            int         yl;
            int         yh;
            int         bx;
            int         by;
            subsector_t newsubsec;

            tmthing = thing;
            tmflags = thing.flags;
            
            tmx = x;
            tmy = y;
            
            tmbbox[BOXTOP] = y + tmthing.radius;
            tmbbox[BOXBOTTOM] = y - tmthing.radius;
            tmbbox[BOXRIGHT] = x + tmthing.radius;
            tmbbox[BOXLEFT] = x - tmthing.radius;

            newsubsec = PointInSubsector (x,y);
            ceilingline = null;
            
            // The base floor / ceiling is from the subsector
            // that contains the point.
            // Any contacted lines the step closer together
            // will adjust them.
            tmfloorz = tmdropoffz = newsubsec.sector.floorheight;
            tmceilingz = newsubsec.sector.ceilingheight;
                    
            R.validcount++;
            numspechit = 0;

            if ( flags(tmflags ,MF_NOCLIP ))
            return true;
            
            // Check things first, possibly picking things up.
            // The bounding box is extended by MAXRADIUS
            // because mobj_ts are grouped into mapblocks
            // based on their origin point, and can overlap
            // into adjacent blocks by up to MAXRADIUS units.
            xl = (tmbbox[BOXLEFT] - LL.bmaporgx - MAXRADIUS)>>MAPBLOCKSHIFT;
            xh = (tmbbox[BOXRIGHT] - LL.bmaporgx + MAXRADIUS)>>MAPBLOCKSHIFT;
            yl = (tmbbox[BOXBOTTOM] - LL.bmaporgy - MAXRADIUS)>>MAPBLOCKSHIFT;
            yh = (tmbbox[BOXTOP] - LL.bmaporgy + MAXRADIUS)>>MAPBLOCKSHIFT;

            for (bx=xl ; bx<=xh ; bx++)
            for (by=yl ; by<=yh ; by++)
                if (!P_BlockThingsIterator(bx,by,PIT_CheckThing))
                return false;
            
            // check lines
            xl = (tmbbox[BOXLEFT] - LL.bmaporgx)>>MAPBLOCKSHIFT;
            xh = (tmbbox[BOXRIGHT] - LL.bmaporgx)>>MAPBLOCKSHIFT;
            yl = (tmbbox[BOXBOTTOM] - LL.bmaporgy)>>MAPBLOCKSHIFT;
            yh = (tmbbox[BOXTOP] - LL.bmaporgy)>>MAPBLOCKSHIFT;

            for (bx=xl ; bx<=xh ; bx++)
            for (by=yl ; by<=yh ; by++)
                if (!BlockLinesIterator (bx,by,PIT_CheckLine))
                return false;

            return true;
        }


        /**
        // P_TryMove
        // Attempt to move to a new position,
        // crossing special lines unless MF_TELEPORT is set.
         * @param x fixed_t
         * @param y fixed_t
         * 
        */
        boolean
        TryMove
        ( mobj_t    thing,
          int   x,
          int   y )
        {
            int oldx, oldy; // fixed_t    
            boolean     side, oldside; // both were int
            line_t  ld;

            floatok = false;
            if (!CheckPosition (thing, x, y))
            return false;       // solid wall or thing
            
            if ( !flags(thing.flags, MF_NOCLIP) )
            {
            if (tmceilingz - tmfloorz < thing.height)
                return false;   // doesn't fit

            floatok = true;
            
            if ( !flags(thing.flags,MF_TELEPORT) 
                 &&tmceilingz - thing.z < thing.height)
                return false;   // mobj must lower itself to fit

            if ( !flags(thing.flags,MF_TELEPORT)
                 && tmfloorz - thing.z > 24*FRACUNIT )
                return false;   // too big a step up

            if ( !flags(thing.flags,(MF_DROPOFF|MF_FLOAT))
                 && tmfloorz - tmdropoffz > 24*FRACUNIT )
                return false;   // don't stand over a dropoff
            }
            
            // the move is ok,
            // so link the thing into its new position
            UnsetThingPosition (thing);

            oldx = thing.x;
            oldy = thing.y;
            thing.floorz = tmfloorz;
            thing.ceilingz = tmceilingz;    
            thing.x = x;
            thing.y = y;

            SetThingPosition (thing);
            
            // if any special lines were hit, do the effect
            if (! flags(thing.flags,(MF_TELEPORT|MF_NOCLIP)) )
            {
            while (numspechit-.0)
            {
                // see if the line was crossed
                ld = spechit[numspechit];
                side = ld.PointOnLineSide (thing.x, thing.y );
                oldside = ld.PointOnLineSide (oldx, oldy );
                if (side != oldside)
                {
                if (ld.special!=0)
                    CrossSpecialLine (ld, oldside, thing);
                }
            }
            }

            return true;
        }


        //
        // P_ThingHeightClip
        // Takes a valid thing and adjusts the thing.floorz,
        // thing.ceilingz, and possibly thing.z.
        // This is called for all nearby monsters
        // whenever a sector changes height.
        // If the thing doesn't fit,
        // the z will be set to the lowest value
        // and false will be returned.
        //
        boolean P_ThingHeightClip (mobj_t thing)
        {
            boolean     onfloor;
            
            onfloor = (thing.z == thing.floorz);
            
            P_CheckPosition (thing, thing.x, thing.y);  
            // what about stranding a monster partially off an edge?
            
            thing.floorz = tmfloorz;
            thing.ceilingz = tmceilingz;
            
            if (onfloor)
            {
            // walking monsters rise and fall with the floor
            thing.z = thing.floorz;
            }
            else
            {
            // don't adjust a floating monster unless forced to
            if (thing.z+thing.height > thing.ceilingz)
                thing.z = thing.ceilingz - thing.height;
            }
            
            if (thing.ceilingz - thing.floorz < thing.height)
            return false;
                
            return true;
        }



        //
        // SLIDE MOVE
        // Allows the player to slide along any angled walls.
        //
        int     bestslidefrac; // fixed
        int     secondslidefrac;

        line_t     bestslideline;
        line_t     secondslideline;

        mobj_t     slidemo;

        int     tmxmove; //fixed_t
        int     tmymove;



        //
        // P_HitSlideLine
        // Adjusts the xmove / ymove
        // so that the next move will slide along the wall.
        //
        private void P_HitSlideLine (line_t ld)
        {
            int         side;

            angle_t     lineangle;
            angle_t     moveangle;
            angle_t     deltaangle;
            
            fixed_t     movelen;
            fixed_t     newlen;
            
            
            if (ld.slopetype == ST_HORIZONTAL)
            {
            tmymove = 0;
            return;
            }
            
            if (ld.slopetype == ST_VERTICAL)
            {
            tmxmove = 0;
            return;
            }
            
            side = P_PointOnLineSide (slidemo.x, slidemo.y, ld);
            
            lineangle = R_PointToAngle2 (0,0, ld.dx, ld.dy);

            if (side == 1)
            lineangle += ANG180;

            moveangle = R_PointToAngle2 (0,0, tmxmove, tmymove);
            deltaangle = moveangle-lineangle;

            if (deltaangle > ANG180)
            deltaangle += ANG180;
            //  I_Error ("SlideLine: ang>ANG180");

            lineangle >>= ANGLETOFINESHIFT;
            deltaangle >>= ANGLETOFINESHIFT;
            
            movelen = P_AproxDistance (tmxmove, tmymove);
            newlen = FixedMul (movelen, finecosine[deltaangle]);

            tmxmove = FixedMul (newlen, finecosine[lineangle]); 
            tmymove = FixedMul (newlen, finesine[lineangle]);   
        }


        //
        // PTR_SlideTraverse
        //
        boolean PTR_SlideTraverse (intercept_t in)
        {
            line_t li;
            
            if (!in.isaline)
            I_Error ("PTR_SlideTraverse: not a line?");
                
            li = in.d.line;
            
            if ( ! (li.flags & ML_TWOSIDED) )
            {
            if (P_PointOnLineSide (slidemo.x, slidemo.y, li))
            {
                // don't hit the back side
                return true;        
            }
            goto isblocking;
            }

            // set openrange, opentop, openbottom
            P_LineOpening (li);
            
            if (openrange < slidemo.height)
            goto isblocking;        // doesn't fit
                
            if (opentop - slidemo.z < slidemo.height)
            goto isblocking;        // mobj is too high

            if (openbottom - slidemo.z > 24*FRACUNIT )
            goto isblocking;        // too big a step up

            // this line doesn't block movement
            return true;        
            
            // the line does block movement,
            // see if it is closer than best so far
          isblocking:       
            if (in.frac < bestslidefrac)
            {
            secondslidefrac = bestslidefrac;
            secondslideline = bestslideline;
            bestslidefrac = in.frac;
            bestslideline = li;
            }
            
            return false;   // stop
        }



        //
        // P_SlideMove
        // The momx / momy move is bad, so try to slide
        // along a wall.
        // Find the first line hit, move flush to it,
        // and slide along it
        //
        // This is a kludgy mess.
        //
        void P_SlideMove (mobj_t mo)
        {
            fixed_t     leadx;
            fixed_t     leady;
            fixed_t     trailx;
            fixed_t     traily;
            fixed_t     newx;
            fixed_t     newy;
            int         hitcount;
                
            slidemo = mo;
            hitcount = 0;
            
          retry:
            if (++hitcount == 3)
            goto stairstep;     // don't loop forever

            
            // trace along the three leading corners
            if (mo.momx > 0)
            {
            leadx = mo.x + mo.radius;
            trailx = mo.x - mo.radius;
            }
            else
            {
            leadx = mo.x - mo.radius;
            trailx = mo.x + mo.radius;
            }
            
            if (mo.momy > 0)
            {
            leady = mo.y + mo.radius;
            traily = mo.y - mo.radius;
            }
            else
            {
            leady = mo.y - mo.radius;
            traily = mo.y + mo.radius;
            }
                
            bestslidefrac = FRACUNIT+1;
            
            P_PathTraverse ( leadx, leady, leadx+mo.momx, leady+mo.momy,
                     PT_ADDLINES, PTR_SlideTraverse );
            P_PathTraverse ( trailx, leady, trailx+mo.momx, leady+mo.momy,
                     PT_ADDLINES, PTR_SlideTraverse );
            P_PathTraverse ( leadx, traily, leadx+mo.momx, traily+mo.momy,
                     PT_ADDLINES, PTR_SlideTraverse );
            
            // move up to the wall
            if (bestslidefrac == FRACUNIT+1)
            {
            // the move most have hit the middle, so stairstep
              stairstep:
            if (!P_TryMove (mo, mo.x, mo.y + mo.momy))
                P_TryMove (mo, mo.x + mo.momx, mo.y);
            return;
            }

            // fudge a bit to make sure it doesn't hit
            bestslidefrac -= 0x800; 
            if (bestslidefrac > 0)
            {
            newx = FixedMul (mo.momx, bestslidefrac);
            newy = FixedMul (mo.momy, bestslidefrac);
            
            if (!P_TryMove (mo, mo.x+newx, mo.y+newy))
                goto stairstep;
            }
            
            // Now continue along the wall.
            // First calculate remainder.
            bestslidefrac = FRACUNIT-(bestslidefrac+0x800);
            
            if (bestslidefrac > FRACUNIT)
            bestslidefrac = FRACUNIT;
            
            if (bestslidefrac <= 0)
            return;
            
            tmxmove = FixedMul (mo.momx, bestslidefrac);
            tmymove = FixedMul (mo.momy, bestslidefrac);

            P_HitSlideLine (bestslideline); // clip the moves

            mo.momx = tmxmove;
            mo.momy = tmymove;
                
            if (!P_TryMove (mo, mo.x+tmxmove, mo.y+tmymove))
            {
            goto retry;
            }
        }


        //
        // P_LineAttack
        //
        mobj_t     linetarget; // who got hit (or NULL)
        mobj_t     shootthing;

        // Height if not aiming up or down
        // ???: use slope for monsters?
        int     shootz; // fixed_t

        int     la_damage;
        int     attackrange; // fixed_t

        int     aimslope; // fixed_t

        // slopes to top and bottom of target
        extern fixed_t  topslope;
        extern fixed_t  bottomslope;    


        //
        // PTR_AimTraverse
        // Sets linetaget and aimslope when a target is aimed at.
        //
        boolean
        PTR_AimTraverse (intercept_t* in)
        {
            line_t*     li;
            mobj_t     th;
            fixed_t     slope;
            fixed_t     thingtopslope;
            fixed_t     thingbottomslope;
            fixed_t     dist;
                
            if (in.isaline)
            {
            li = in.d.line;
            
            if ( !(li.flags & ML_TWOSIDED) )
                return false;       // stop
            
            // Crosses a two sided line.
            // A two sided line will restrict
            // the possible target ranges.
            P_LineOpening (li);
            
            if (openbottom >= opentop)
                return false;       // stop
            
            dist = FixedMul (attackrange, in.frac);

            if (li.frontsector.floorheight != li.backsector.floorheight)
            {
                slope = FixedDiv (openbottom - shootz , dist);
                if (slope > bottomslope)
                bottomslope = slope;
            }
                
            if (li.frontsector.ceilingheight != li.backsector.ceilingheight)
            {
                slope = FixedDiv (opentop - shootz , dist);
                if (slope < topslope)
                topslope = slope;
            }
                
            if (topslope <= bottomslope)
                return false;       // stop
                    
            return true;            // shot continues
            }
            
            // shoot a thing
            th = in.d.thing;
            if (th == shootthing)
            return true;            // can't shoot self
            
            if (!(th.flags&MF_SHOOTABLE))
            return true;            // corpse or something

            // check angles to see if the thing can be aimed at
            dist = FixedMul (attackrange, in.frac);
            thingtopslope = FixedDiv (th.z+th.height - shootz , dist);

            if (thingtopslope < bottomslope)
            return true;            // shot over the thing

            thingbottomslope = FixedDiv (th.z - shootz, dist);

            if (thingbottomslope > topslope)
            return true;            // shot under the thing
            
            // this thing can be hit!
            if (thingtopslope > topslope)
            thingtopslope = topslope;
            
            if (thingbottomslope < bottomslope)
            thingbottomslope = bottomslope;

            aimslope = (thingtopslope+thingbottomslope)/2;
            linetarget = th;

            return false;           // don't go any farther
        }


        //
        // PTR_ShootTraverse
        //
        boolean PTR_ShootTraverse (intercept_t* in)
        {
            fixed_t     x;
            fixed_t     y;
            fixed_t     z;
            fixed_t     frac;
            
            line_t*     li;
            
            mobj_t     th;

            fixed_t     slope;
            fixed_t     dist;
            fixed_t     thingtopslope;
            fixed_t     thingbottomslope;
                
            if (in.isaline)
            {
            li = in.d.line;
            
            if (li.special)
                P_ShootSpecialLine (shootthing, li);

            if ( !(li.flags & ML_TWOSIDED) )
                goto hitline;
            
            // crosses a two sided line
            P_LineOpening (li);
                
            dist = FixedMul (attackrange, in.frac);

            if (li.frontsector.floorheight != li.backsector.floorheight)
            {
                slope = FixedDiv (openbottom - shootz , dist);
                if (slope > aimslope)
                goto hitline;
            }
                
            if (li.frontsector.ceilingheight != li.backsector.ceilingheight)
            {
                slope = FixedDiv (opentop - shootz , dist);
                if (slope < aimslope)
                goto hitline;
            }

            // shot continues
            return true;
            
            
            // hit line
              hitline:
            // position a bit closer
            frac = in.frac - FixedDiv (4*FRACUNIT,attackrange);
            x = trace.x + FixedMul (trace.dx, frac);
            y = trace.y + FixedMul (trace.dy, frac);
            z = shootz + FixedMul (aimslope, FixedMul(frac, attackrange));

            if (li.frontsector.ceilingpic == skyflatnum)
            {
                // don't shoot the sky!
                if (z > li.frontsector.ceilingheight)
                return false;
                
                // it's a sky hack wall
                if  (li.backsector && li.backsector.ceilingpic == skyflatnum)
                return false;       
            }

            // Spawn bullet puffs.
            P_SpawnPuff (x,y,z);
            
            // don't go any farther
            return false;   
            }
            
            // shoot a thing
            th = in.d.thing;
            if (th == shootthing)
            return true;        // can't shoot self
            
            if (!(th.flags&MF_SHOOTABLE))
            return true;        // corpse or something
                
            // check angles to see if the thing can be aimed at
            dist = FixedMul (attackrange, in.frac);
            thingtopslope = FixedDiv (th.z+th.height - shootz , dist);

            if (thingtopslope < aimslope)
            return true;        // shot over the thing

            thingbottomslope = FixedDiv (th.z - shootz, dist);

            if (thingbottomslope > aimslope)
            return true;        // shot under the thing

            
            // hit thing
            // position a bit closer
            frac = in.frac - FixedDiv (10*FRACUNIT,attackrange);

            x = trace.x + FixedMul (trace.dx, frac);
            y = trace.y + FixedMul (trace.dy, frac);
            z = shootz + FixedMul (aimslope, FixedMul(frac, attackrange));

            // Spawn bullet puffs or blod spots,
            // depending on target type.
            if (in.d.thing.flags & MF_NOBLOOD)
            P_SpawnPuff (x,y,z);
            else
            P_SpawnBlood (x,y,z, la_damage);

            if (la_damage)
            P_DamageMobj (th, shootthing, shootthing, la_damage);

            // don't go any farther
            return false;
            
        }


        //
        // 
        //
        /** P_AimLineAttack
         * @param t1
         * @param angle int
         * @param distance int
         */
       int
       AimLineAttack
        ( mobj_t   t1,
          int   angle,
          int   distance )
        {
            int x2,y2;
            
            angle >>= ANGLETOFINESHIFT;
            shootthing = t1;
            
            x2 = t1.x + (distance>>FRACBITS)*finecosine[angle];
            y2 = t1.y + (distance>>FRACBITS)*finesine[angle];
            shootz = t1.z + (t1.height>>1) + 8*FRACUNIT;

            // can't shoot outside view angles
            topslope = 100*FRACUNIT/160;    
            bottomslope = -100*FRACUNIT/160;
            
            attackrange = distance;
            linetarget = NULL;
            
            P_PathTraverse ( t1.x, t1.y,
                     x2, y2,
                     PT_ADDLINES|PT_ADDTHINGS,
                     PTR_AimTraverse );
                
            if (linetarget)
            return aimslope;

            return 0;
        }
         

        /**
         * P_LineAttack
         * If damage == 0, it is just a test trace
         * that will leave linetarget set.
         * 
         * @param t1
         * @param angle angle_t
         * @param distance fixed_t
         * @param slope fixed_t
         * @param damage
         */
       
        void
        P_LineAttack
        ( mobj_t   t1,
          int   angle,
          int   distance,
          int   slope,
          int       damage )
        {
        	int x2,y2;
            
            angle >>= ANGLETOFINESHIFT;
            shootthing = t1;
            la_damage = damage;
            x2 = t1.x + (distance>>FRACBITS)*finecosine[angle];
            y2 = t1.y + (distance>>FRACBITS)*finesine[angle];
            shootz = t1.z + (t1.height>>1) + 8*FRACUNIT;
            attackrange = distance;
            aimslope = slope;
                
            P_PathTraverse ( t1.x, t1.y,
                     x2, y2,
                     PT_ADDLINES|PT_ADDTHINGS,
                     PTR_ShootTraverse );
        }
         


        //
        // USE LINES
        //
        mobj_t     usething;

        boolean PTR_UseTraverse (intercept_t in)
        {
            int     side;
            
            if (!in.d.line.special)
            {
            P_LineOpening (in.d.line);
            if (openrange <= 0)
            {
                S_StartSound (usething, sfx_noway);
                
                // can't use through a wall
                return false;   
            }
            // not a special line, but keep checking
            return true ;       
            }
            
            side = 0;
            if (P_PointOnLineSide (usething.x, usething.y, in.d.line) == 1)
            side = 1;
            
            //  return false;       // don't use back side
            
            P_UseSpecialLine (usething, in.d.line, side);

            // can't use for than one special line in a row
            return false;
        }


        /**
         * P_UseLines
         * Looks for special lines in front of the player to activate.
         */
        void P_UseLines (player_t  player) 
        {
            int     angle;
            int x1,y1,x2,y2;
            
            usething = player.mo;
                
            angle = player.mo.angle >> ANGLETOFINESHIFT;

            x1 = player.mo.x;
            y1 = player.mo.y;
            x2 = x1 + (USERANGE>>FRACBITS)*finecosine[angle];
            y2 = y1 + (USERANGE>>FRACBITS)*finesine[angle];
            
            P_PathTraverse ( x1, y1, x2, y2, PT_ADDLINES, PTR_UseTraverse );
        }


        //
        // RADIUS ATTACK
        //
        mobj_t     bombsource;
        mobj_t     bombspot;
        int     bombdamage;


        //
        // PIT_RadiusAttack
        // "bombsource" is the creature
        // that caused the explosion at "bombspot".
        //
        boolean PIT_RadiusAttack (mobj_t thing)
        {
            int dx,dy,dist; // fixed_t
            
            if (!flags(thing.flags , MF_SHOOTABLE) )
            return true;

            // Boss spider and cyborg
            // take no damage from concussion.
            if (thing.type == MT_CYBORG
            || thing.type == MT_SPIDER)
            return true;    
                
            dx = abs(thing.x - bombspot.x);
            dy = abs(thing.y - bombspot.y);
            
            dist = dx>dy ? dx : dy;
            dist = (dist - thing.radius) >> FRACBITS;

            if (dist < 0)
            dist = 0;

            if (dist >= bombdamage)
            return true;    // out of range

            if ( P_CheckSight (thing, bombspot) )
            {
            // must be in direct path
            P_DamageMobj (thing, bombspot, bombsource, bombdamage - dist);
            }
            
            return true;
        }


        /**
         * P_RadiusAttack
         * Source is the creature that caused the explosion at spot.
         */
        
        void
        P_RadiusAttack
        ( mobj_t   spot,
          mobj_t   source,
          int       damage )
        {
            int     x;
            int     y;
            
            int     xl;
            int     xh;
            int     yl;
            int     yh;
            
            int dist; // fixed_t
            
            dist = (damage+MAXRADIUS)<<FRACBITS;
            yh = (spot.y + dist - LL.bmaporgy)>>MAPBLOCKSHIFT;
            yl = (spot.y - dist - LL.bmaporgy)>>MAPBLOCKSHIFT;
            xh = (spot.x + dist - LL.bmaporgx)>>MAPBLOCKSHIFT;
            xl = (spot.x - dist - LL.bmaporgx)>>MAPBLOCKSHIFT;
            bombspot = spot;
            bombsource = source;
            bombdamage = damage;
            
            for (y=yl ; y<=yh ; y++)
            for (x=xl ; x<=xh ; x++)
                P_BlockThingsIterator (x, y, PIT_RadiusAttack );
        }



        //
        // SECTOR HEIGHT CHANGING
        // After modifying a sectors floor or ceiling height,
        // call this routine to adjust the positions
        // of all things that touch the sector.
        //
        // If anything doesn't fit anymore, true will be returned.
        // If crunch is true, they will take damage
        //  as they are being crushed.
        // If Crunch is false, you should set the sector height back
        //  the way it was and call P_ChangeSector again
        //  to undo the changes.
        //
        boolean     crushchange;
        boolean     nofit;


        //
        // PIT_ChangeSector
        //
        boolean PIT_ChangeSector (mobj_t   thing)
        {
            mobj_t mo;
            
            if (P_ThingHeightClip (thing))
            {
            // keep checking
            return true;
            }
            

            // crunch bodies to giblets
            if (thing.health <= 0)
            {
            P_SetMobjState (thing, S_GIBS);

            thing.flags &= ~MF_SOLID;
            thing.height = 0;
            thing.radius = 0;

            // keep checking
            return true;        
            }

            // crunch dropped items
            if (thing.flags & MF_DROPPED)
            {
            P_RemoveMobj (thing);
            
            // keep checking
            return true;        
            }

            if (! flags(thing.flags , MF_SHOOTABLE) )
            {
            // assume it is bloody gibs or something
            return true;            
            }
            
            nofit = true;

            if (crushchange && !(leveltime&3) )
            {
            P_DamageMobj(thing,NULL,NULL,10);

            // spray blood in a random direction
            mo = P_SpawnMobj (thing.x,
                      thing.y,
                      thing.z + thing.height/2, MT_BLOOD);
            
            mo.momx = (P_Random() - P_Random ())<<12;
            mo.momy = (P_Random() - P_Random ())<<12;
            }

            // keep checking (crush other things)   
            return true;    
        }



        //
        // P_ChangeSector
        //
        boolean
        P_ChangeSector
        ( sector_t sector,
          boolean   crunch )
        {
            int     x;
            int     y;
            
            nofit = false;
            crushchange = crunch;
            
            // re-check heights for all things near the moving sector
            for (x=sector.blockbox[BOXLEFT] ; x<= sector.blockbox[BOXRIGHT] ; x++)
            for (y=sector.blockbox[BOXBOTTOM];y<= sector.blockbox[BOXTOP] ; y++)
                P_BlockThingsIterator (x, y, PIT_ChangeSector);
            
            
            return nofit;
        }


        ////////////////////// FROM p_maputl.c ////////////////////

        /** fixed_t  */
        protected int opentop,  openbottom, openrange, lowfloor;


        /**
         * P_LineOpening
         * Sets opentop and openbottom to the window
         * through a two sided line.
         * OPTIMIZE: keep this precalculated
         */

        public void P_LineOpening (line_t linedef)
        {
         sector_t   front;
         sector_t   back;
         
         if (linedef.sidenum[1] == -1)
         {
         // single sided line
         openrange = 0;
         return;
         }
          
         front = linedef.frontsector;
         back = linedef.backsector;
         
         if (front.ceilingheight < back.ceilingheight)
         opentop = front.ceilingheight;
         else
         opentop = back.ceilingheight;

         if (front.floorheight > back.floorheight)
         {
         openbottom = front.floorheight;
         lowfloor = back.floorheight;
         }
         else
         {
         openbottom = back.floorheight;
         lowfloor = front.floorheight;
         }
         
         openrange = opentop - openbottom;
        }


        //
        //THING POSITION SETTING
        //


        /**
         * P_UnsetThingPosition
         * Unlinks a thing from block map and sectors.
         * On each position change, BLOCKMAP and other
         * lookups maintaining lists ot things inside
         * these structures need to be updated.
         *
         */

        public void UnsetThingPosition (mobj_t thing)
        {
         int     blockx;
         int     blocky;

         if ( ! flags(thing.flags , MF_NOSECTOR) )
         {
         // inert things don't need to be in blockmap?
         // unlink from subsector
         if (thing.snext!=null)
             ((mobj_t)thing.snext).sprev = thing.sprev;

         if (thing.sprev!=null)
        	 ((mobj_t)thing.sprev).snext = thing.snext;
         else
             thing.subsector.sector.thinglist = (mobj_t) thing.snext;
         }
         
         if ( ! flags(thing.flags,MF_NOBLOCKMAP) )
         {
         // inert things don't need to be in blockmap
         // unlink from block map
         if (thing.bnext!=null)
             ((mobj_t)thing.bnext).bprev = thing.bprev;
         
         if (thing.bprev!=null)
        	 ((mobj_t)thing.bprev).bnext = thing.bnext;
         else
         {
             blockx = (thing.x - LL.bmaporgx)>>MAPBLOCKSHIFT;
             blocky = (thing.y - LL.bmaporgy)>>MAPBLOCKSHIFT;

             if (blockx>=0 && blockx < LL.bmapwidth
             && blocky>=0 && blocky <LL.bmapheight)
             {
             LL.blocklinks[blocky*LL.bmapwidth+blockx] = (mobj_t) thing.bnext;
             }
         }
         }
        }


        
        }



        //
        //BLOCK MAP ITERATORS
        //For each line/thing in the given mapblock,
        //call the passed PIT_* function.
        //If the function returns false,
        //exit with false without checking anything else.
        //


        /**
         * P_BlockLinesIterator
         * The validcount flags are used to avoid checking lines
         * that are marked in multiple mapblocks,
         * so increment validcount before the first call
         * to P_BlockLinesIterator, then make one or more calls
         * to it.
         */

        public boolean BlockLinesIterator ( int           x,int           y,PIT_LineFunction func )
        {
         int         offset;
         short[]      list;
         line_t     ld;
         
         if (x<0
         || y<0
         || x>=bmapwidth
         || y>=bmapheight)
         {
         return true;
         }
         
         offset = y*bmapwidth+x;
         
         //offset = *(blockmap+offset);

         for ( list = blockmaplump[offset] ; *list != -1 ; list++)
         {
         ld = lines[*list];

         if (ld.validcount == validcount)
             continue;   // line has already been checked

         ld.validcount = validcount;
             
         if ( !func.invoke(ld) )
             return false;
         }
         return true;    // everything was checked
        }


        //
        //P_BlockThingsIterator
        //
        boolean
        P_BlockThingsIterator
        ( int           x,
        int           y,
        boolean(*func)(mobj_t) )
        {
         mobj_t     mobj;
         
         if ( x<0
          || y<0
          || x>=bmapwidth
          || y>=bmapheight)
         {
         return true;
         }
         

         for (mobj = blocklinks[y*bmapwidth+x] ;
          mobj ;
          mobj = mobj.bnext)
         {
         if (!func( mobj ) )
             return false;
         }
         return true;
        }



        //
        //INTERCEPT ROUTINES
        //
        intercept_t intercepts[MAXINTERCEPTS];
        intercept_t*    intercept_p;

        divline_t   trace;
        boolean     earlyout;
        int     ptflags;

        //
        //PIT_AddLineIntercepts.
        //Looks for lines in the given block
        //that intercept the given trace
        //to add to the intercepts list.
        //
        //A line is crossed if its endpoints
        //are on opposite sides of the trace.
        //Returns true if earlyout and a solid line hit.
        //
        boolean
        PIT_AddLineIntercepts (line_t* ld)
        {
         int         s1;
         int         s2;
         fixed_t     frac;
         divline_t       dl;
         
         // avoid precision problems with two routines
         if ( trace.dx > FRACUNIT*16
          || trace.dy > FRACUNIT*16
          || trace.dx < -FRACUNIT*16
          || trace.dy < -FRACUNIT*16)
         {
         s1 = P_PointOnDivlineSide (ld.v1.x, ld.v1.y, &trace);
         s2 = P_PointOnDivlineSide (ld.v2.x, ld.v2.y, &trace);
         }
         else
         {
         s1 = P_PointOnLineSide (trace.x, trace.y, ld);
         s2 = P_PointOnLineSide (trace.x+trace.dx, trace.y+trace.dy, ld);
         }
         
         if (s1 == s2)
         return true;    // line isn't crossed
         
         // hit the line
         P_MakeDivline (ld, &dl);
         frac = P_InterceptVector (&trace, &dl);

         if (frac < 0)
         return true;    // behind source
         
         // try to early out the check
         if (earlyout
         && frac < FRACUNIT
         && !ld.backsector)
         {
         return false;   // stop checking
         }
         
         
         intercept_p.frac = frac;
         intercept_p.isaline = true;
         intercept_p.d.line = ld;
         intercept_p++;

         return true;    // continue
        }



        //
        //PIT_AddThingIntercepts
        //
        boolean PIT_AddThingIntercepts (mobj_t thing)
        {
         fixed_t     x1;
         fixed_t     y1;
         fixed_t     x2;
         fixed_t     y2;
         
         int         s1;
         int         s2;
         
         boolean     tracepositive;

         divline_t       dl;
         
         fixed_t     frac;
         
         tracepositive = (trace.dx ^ trace.dy)>0;
             
         // check a corner to corner crossection for hit
         if (tracepositive)
         {
         x1 = thing.x - thing.radius;
         y1 = thing.y + thing.radius;
             
         x2 = thing.x + thing.radius;
         y2 = thing.y - thing.radius;          
         }
         else
         {
         x1 = thing.x - thing.radius;
         y1 = thing.y - thing.radius;
             
         x2 = thing.x + thing.radius;
         y2 = thing.y + thing.radius;          
         }
         
         s1 = P_PointOnDivlineSide (x1, y1, &trace);
         s2 = P_PointOnDivlineSide (x2, y2, &trace);

         if (s1 == s2)
         return true;        // line isn't crossed
         
         dl.x = x1;
         dl.y = y1;
         dl.dx = x2-x1;
         dl.dy = y2-y1;
         
         frac = P_InterceptVector (&trace, &dl);

         if (frac < 0)
         return true;        // behind source

         intercept_p.frac = frac;
         intercept_p.isaline = false;
         intercept_p.d.thing = thing;
         intercept_p++;

         return true;        // keep going
        }


        //
        //P_TraverseIntercepts
        //Returns true if the traverser function returns true
        //for all lines.
        //
        boolean
        P_TraverseIntercepts
        ( traverser_t   func,
        fixed_t   maxfrac )
        {
         int         count;
         fixed_t     dist;
         intercept_t*    scan;
         intercept_t*    in;
         
         count = intercept_p - intercepts;
         
         in = 0;         // shut up compiler warning
         
         while (count--)
         {
         dist = MAXINT;
         for (scan = intercepts ; scan<intercept_p ; scan++)
         {
             if (scan.frac < dist)
             {
             dist = scan.frac;
             in = scan;
             }
         }
         
         if (dist > maxfrac)
             return true;    // checked everything in range      

        #if 0  // UNUSED
         {
         // don't check these yet, there may be others inserted
         in = scan = intercepts;
         for ( scan = intercepts ; scan<intercept_p ; scan++)
             if (scan.frac > maxfrac)
             *in++ = *scan;
         intercept_p = in;
         return false;
         }
        #endif

             if ( !func (in) )
             return false;   // don't bother going farther

         in.frac = MAXINT;
         }
         
         return true;        // everything was traversed
        }




        //
        //P_PathTraverse
        //Traces a line from x1,y1 to x2,y2,
        //calling the traverser function for each.
        //Returns true if the traverser function returns true
        //for all lines.
        //
        boolean
        P_PathTraverse
        ( int       x1,
        		int       y1,
        		int       x2,
        		int       y2,
        int           flags,
        boolean (*trav) (intercept_t *))
        {
         int xt1,yt1;
         int xt2, yt2;
         
         int xstep,ystep;
         
         int partial;
         
         int xintercept, yintercept;
         
         int     mapx;
         int     mapy;
         
         int     mapxstep;
         int     mapystep;

         int     count;
             
         earlyout = flags & PT_EARLYOUT;
             
         validcount++;
         intercept_p = intercepts;
         
         if ( ((x1-bmaporgx)&(MAPBLOCKSIZE-1)) == 0)
         x1 += FRACUNIT; // don't side exactly on a line
         
         if ( ((y1-bmaporgy)&(MAPBLOCKSIZE-1)) == 0)
         y1 += FRACUNIT; // don't side exactly on a line

         trace.x = x1;
         trace.y = y1;
         trace.dx = x2 - x1;
         trace.dy = y2 - y1;

         x1 -= bmaporgx;
         y1 -= bmaporgy;
         xt1 = x1>>MAPBLOCKSHIFT;
         yt1 = y1>>MAPBLOCKSHIFT;

         x2 -= bmaporgx;
         y2 -= bmaporgy;
         xt2 = x2>>MAPBLOCKSHIFT;
         yt2 = y2>>MAPBLOCKSHIFT;

         if (xt2 > xt1)
         {
         mapxstep = 1;
         partial = FRACUNIT - ((x1>>MAPBTOFRAC)&(FRACUNIT-1));
         ystep = FixedDiv (y2-y1,abs(x2-x1));
         }
         else if (xt2 < xt1)
         {
         mapxstep = -1;
         partial = (x1>>MAPBTOFRAC)&(FRACUNIT-1);
         ystep = FixedDiv (y2-y1,abs(x2-x1));
         }
         else
         {
         mapxstep = 0;
         partial = FRACUNIT;
         ystep = 256*FRACUNIT;
         }   

         yintercept = (y1>>MAPBTOFRAC) + FixedMul (partial, ystep);

         
         if (yt2 > yt1)
         {
         mapystep = 1;
         partial = FRACUNIT - ((y1>>MAPBTOFRAC)&(FRACUNIT-1));
         xstep = FixedDiv (x2-x1,abs(y2-y1));
         }
         else if (yt2 < yt1)
         {
         mapystep = -1;
         partial = (y1>>MAPBTOFRAC)&(FRACUNIT-1);
         xstep = FixedDiv (x2-x1,abs(y2-y1));
         }
         else
         {
         mapystep = 0;
         partial = FRACUNIT;
         xstep = 256*FRACUNIT;
         }   
         xintercept = (x1>>MAPBTOFRAC) + FixedMul (partial, xstep);
         
         // Step through map blocks.
         // Count is present to prevent a round off error
         // from skipping the break.
         mapx = xt1;
         mapy = yt1;
         
         for (count = 0 ; count < 64 ; count++)
         {
         if (flags & PT_ADDLINES)
         {
             if (!P_BlockLinesIterator (mapx, mapy,PIT_AddLineIntercepts))
             return false;   // early out
         }
         
         if (flags & PT_ADDTHINGS)
         {
             if (!P_BlockThingsIterator (mapx, mapy,PIT_AddThingIntercepts))
             return false;   // early out
         }
             
         if (mapx == xt2
             && mapy == yt2)
         {
             break;
         }
         
         if ( (yintercept >> FRACBITS) == mapy)
         {
             yintercept += ystep;
             mapx += mapxstep;
         }
         else if ( (xintercept >> FRACBITS) == mapx)
         {
             xintercept += xstep;
             mapy += mapystep;
         }
             
         }
         // go through the sorted list
         return P_TraverseIntercepts ( trav, FRACUNIT );
        }


        }
    
    class MapObjects {


int test;

//
// P_SetMobjState
// Returns true if the mobj is still present.
//

public boolean
SetMobjState
(mobj_t mobj, statenum_t    state )
{
state_t st;

do
{
if (state == statenum_t.S_NULL)
{
  mobj.state = null;
  // TODO:P_RemoveMobj (mobj);
  return false;
}

st = states[state.ordinal()];
mobj.state = st;
mobj.tics = st.tics;
mobj.sprite = st.sprite;
mobj.frame = (int) st.frame;

// Modified handling.
// Call action functions when the state is set
if (st.action.getType()==ActionType.acp1)       
  ((acp1)st.action).invoke(mobj); 

state = st.nextstate;
} while (mobj.tics==0);
          
return true;
}






//
// P_ExplodeMissile  
//
public void P_ExplodeMissile (mobj_t mo)
{
mo.momx = mo.momy = mo.momz = 0;

SetMobjState (mo, mobjinfo[mo.type.ordinal()].deathstate);

mo.tics -= RND.P_Random()&3;

if (mo.tics < 1)
mo.tics = 1;

mo.flags &= ~MF_MISSILE;

if (mo.info.deathsound!=null) ;
// TODO: S_StartSound (mo, mo.info.deathsound);
}


//
// P_XYMovement  
//
protected final static int STOPSPEED =      0x1000;
protected final static int FRICTION =       0xe800;

public void XYMovement (mobj_t mo) 
{   
int     ptryx, ptryy; // pointers to fixed_t ???
player_t   player;
int  xmove, ymove; // fixed_t
      
if ((mo.momx==0) && (mo.momy==0))
{
if ((mo.flags & MF_SKULLFLY)!=0)
{
  // the skull slammed into something
  mo.flags &= ~MF_SKULLFLY;
  mo.momx = mo.momy = mo.momz = 0;

  SetMobjState (mo, mo.info.spawnstate);
}
return;
}

player = mo.player;
  
if (mo.momx > MAXMOVE)
mo.momx = MAXMOVE;
else if (mo.momx < -MAXMOVE)
mo.momx = -MAXMOVE;

if (mo.momy > MAXMOVE)
mo.momy = MAXMOVE;
else if (mo.momy < -MAXMOVE)
mo.momy = -MAXMOVE;
  
xmove = mo.momx;
ymove = mo.momy;

do
{
if (xmove > MAXMOVE/2 || ymove > MAXMOVE/2)
{
  ptryx = mo.x + xmove/2;
  ptryy = mo.y + ymove/2;
  xmove >>= 1;
  ymove >>= 1;
}
else
{
  ptryx = mo.x + xmove;
  ptryy = mo.y + ymove;
  xmove = ymove = 0;
}
  
if (!P_TryMove (mo, ptryx, ptryy))
{
  // blocked move
  if (mo.player)
  {   // try to slide along it
  P_SlideMove (mo);
  }
  else if (mo.flags & MF_MISSILE)
  {
  // explode a missile
  if (ceilingline &&
      ceilingline.backsector &&
      ceilingline.backsector.ceilingpic == skyflatnum)
  {
      // Hack to prevent missiles exploding
      // against the sky.
      // Does not handle sky floors.
      P_RemoveMobj (mo);
      return;
  }
  P_ExplodeMissile (mo);
  }
  else
  mo.momx = mo.momy = 0;
}
} while ((xmove | ymove)!=0);

// slow down
if (player && player.cheats & CF_NOMOMENTUM)
{
// debug option for no sliding at all
mo.momx = mo.momy = 0;
return;
}

if (mo.flags & (MF_MISSILE | MF_SKULLFLY) )
return;     // no friction for missiles ever
  
if (mo.z > mo.floorz)
return;     // no friction when airborne

if (mo.flags & MF_CORPSE)
{
// do not stop sliding
//  if halfway off a step with some momentum
if (mo.momx > FRACUNIT/4
  || mo.momx < -FRACUNIT/4
  || mo.momy > FRACUNIT/4
  || mo.momy < -FRACUNIT/4)
{
  if (mo.floorz != mo.subsector.sector.floorheight)
  return;
}
}

if (mo.momx > -STOPSPEED
&& mo.momx < STOPSPEED
&& mo.momy > -STOPSPEED
&& mo.momy < STOPSPEED
&& (!player
  || (player.cmd.forwardmove== 0
  && player.cmd.sidemove == 0 ) ) )
{
// if in a walking frame, stop moving
if ( player&&(unsigned)((player.mo.state - states)- S_PLAY_RUN1) < 4)
  P_SetMobjState (player.mo, S_PLAY);

mo.momx = 0;
mo.momy = 0;
}
else
{
mo.momx = FixedMul (mo.momx, FRICTION);
mo.momy = FixedMul (mo.momy, FRICTION);
}
}

//
// P_NightmareRespawn
//
void
P_NightmareRespawn (mobj_t mobj)
{
fixed_t     x;
fixed_t     y;
fixed_t     z; 
subsector_t*    ss; 
mobj_t     mo;
mapthing_t*     mthing;
  
x = mobj.spawnpoint.x << FRACBITS; 
y = mobj.spawnpoint.y << FRACBITS; 

// somthing is occupying it's position?
if (!P_CheckPosition (mobj, x, y) ) 
return; // no respwan

// spawn a teleport fog at old spot
// because of removal of the body?
mo = P_SpawnMobj (mobj.x,
        mobj.y,
        mobj.subsector.sector.floorheight , MT_TFOG); 
// initiate teleport sound
S_StartSound (mo, sfx_telept);

// spawn a teleport fog at the new spot
ss = R_PointInSubsector (x,y); 

mo = P_SpawnMobj (x, y, ss.sector.floorheight , MT_TFOG); 

S_StartSound (mo, sfx_telept);

// spawn the new monster
mthing = &mobj.spawnpoint;

// spawn it
if (mobj.info.flags & MF_SPAWNCEILING)
z = ONCEILINGZ;
else
z = ONFLOORZ;

// inherit attributes from deceased one
mo = P_SpawnMobj (x,y,z, mobj.type);
mo.spawnpoint = mobj.spawnpoint;  
mo.angle = ANG45 * (mthing.angle/45);

if (mthing.options & MTF_AMBUSH)
mo.flags |= MF_AMBUSH;

mo.reactiontime = 18;

// remove the old monster,
P_RemoveMobj (mobj);
}


/** Place ALL action functions here. Kinda hacky, but workable.
 *  If you feel fancy, use attribute maps, hashtables or whatever.
 *  Instantiate this as "A" and you have a sugary syntax, too.
 *    
 * @author Kaptain Zyklon
 *
 */
class ActionFunctions {

//
// P_MobjThinker
//
}



class P_MobjThinker implements acp1 {
	
		@Override
		public void invoke (mobj_t mobj) {
// momentum movement
if (mobj.momx!=0
|| mobj.momy!=0
|| (flags(mobj.flags,MF_SKULLFLY)) )
{
	mobj.XYMovement ();

// FIXME: decent NOP/NULL/Nil function pointer please.
if (mobj.thinker.function == null)
  return;     // mobj was removed
}
if ( (mobj.z != mobj.floorz)
|| mobj.momz!=0 )
{
	mobj.ZMovement ();

// FIXME: decent NOP/NULL/Nil function pointer please.
if (mobj.thinker.function == null)
  return;     // mobj was removed
}


// cycle through states,
// calling action functions at transitions
if (mobj.tics != -1)
{
mobj.tics--;
  
// you can cycle through multiple states in a tic
if (!mobj.tics)
  if (!P_SetMobjState (mobj, mobj.state.nextstate) )
  return;     // freed itself
}
else
{
// check for nightmare respawn
if (! (mobj.flags & MF_COUNTKILL) )
  return;

if (!respawnmonsters)
  return;

mobj.movecount++;

if (mobj.movecount < 12*35)
  return;

if ( leveltime&31 )
  return;

if (P_Random () > 4)
  return;

P_NightmareRespawn (mobj);
}

}

@Override
public ActionType getType() {
	return ActionType.acp1;
}

}


//
// P_RemoveMobj
//
mapthing_t  itemrespawnque[ITEMQUESIZE];
int     itemrespawntime[ITEMQUESIZE];
int     iquehead;
int     iquetail;


void P_RemoveMobj (mobj_t mobj)
{
if ((mobj.flags & MF_SPECIAL)
&& !(mobj.flags & MF_DROPPED)
&& (mobj.type != MT_INV)
&& (mobj.type != MT_INS))
{
itemrespawnque[iquehead] = mobj.spawnpoint;
itemrespawntime[iquehead] = leveltime;
iquehead = (iquehead+1)&(ITEMQUESIZE-1);

// lose one off the end?
if (iquehead == iquetail)
  iquetail = (iquetail+1)&(ITEMQUESIZE-1);
}

// unlink from sector and block lists
P_UnsetThingPosition (mobj);

// stop any playing sound
S_StopSound (mobj);

// free block
P_RemoveThinker ((thinker_t*)mobj);
}




//
// P_RespawnSpecials
//
void P_RespawnSpecials (void)
{
fixed_t     x;
fixed_t     y;
fixed_t     z;

subsector_t*    ss; 
mobj_t     mo;
mapthing_t*     mthing;

int         i;

// only respawn items in deathmatch
if (deathmatch != 2)
return; // 

// nothing left to respawn?
if (iquehead == iquetail)
return;     

// wait at least 30 seconds
if (leveltime - itemrespawntime[iquetail] < 30*35)
return;         

mthing = &itemrespawnque[iquetail];

x = mthing.x << FRACBITS; 
y = mthing.y << FRACBITS; 

// spawn a teleport fog at the new spot
ss = R_PointInSubsector (x,y); 
mo = P_SpawnMobj (x, y, ss.sector.floorheight , MT_IFOG); 
S_StartSound (mo, sfx_itmbk);

// find which type to spawn
for (i=0 ; i< NUMMOBJTYPES ; i++)
{
if (mthing.type == mobjinfo[i].doomednum)
  break;
}

// spawn it
if (mobjinfo[i].flags & MF_SPAWNCEILING)
z = ONCEILINGZ;
else
z = ONFLOORZ;

mo = P_SpawnMobj (x,y,z, i);
mo.spawnpoint = *mthing;   
mo.angle = ANG45 * (mthing.angle/45);

// pull it from the que
iquetail = (iquetail+1)&(ITEMQUESIZE-1);
}




//
// P_SpawnPlayer
// Called when a player is spawned on the level.
// Most of the player structure stays unchanged
//  between levels.
//
void P_SpawnPlayer (mapthing_t* mthing)
{
player_t*       p;
fixed_t     x;
fixed_t     y;
fixed_t     z;

mobj_t     mobj;

int         i;

// not playing?
if (!playeringame[mthing.type-1])
return;                 
  
p = &players[mthing.type-1];

if (p.playerstate == PST_REBORN)
G_PlayerReborn (mthing.type-1);

x       = mthing.x << FRACBITS;
y       = mthing.y << FRACBITS;
z       = ONFLOORZ;
mobj    = P_SpawnMobj (x,y,z, MT_PLAYER);

// set color translations for player sprites
if (mthing.type > 1)       
mobj.flags |= (mthing.type-1)<<MF_TRANSSHIFT;
  
mobj.angle = ANG45 * (mthing.angle/45);
mobj.player = p;
mobj.health = p.health;

p.mo = mobj;
p.playerstate = PST_LIVE;  
p.refire = 0;
p.message = NULL;
p.damagecount = 0;
p.bonuscount = 0;
p.extralight = 0;
p.fixedcolormap = 0;
p.viewheight = VIEWHEIGHT;

// setup gun psprite
P_SetupPsprites (p);

// give all cards in death match mode
if (deathmatch)
for (i=0 ; i<NUMCARDS ; i++)
  p.cards[i] = true;
      
if (mthing.type-1 == consoleplayer)
{
// wake up the status bar
ST_Start ();
// wake up the heads up text
HU_Start ();        
}
}


//
// P_SpawnMapThing
// The fields of the mapthing should
// already be in host byte order.
//
void P_SpawnMapThing (mapthing_t* mthing)
{
int         i;
int         bit;
mobj_t     mobj;
fixed_t     x;
fixed_t     y;
fixed_t     z;
  
// count deathmatch start positions
if (mthing.type == 11)
{
if (deathmatch_p < &deathmatchstarts[10])
{
  memcpy (deathmatch_p, mthing, sizeof(*mthing));
  deathmatch_p++;
}
return;
}

// check for players specially
if (mthing.type <= 4)
{
// save spots for respawning in network games
playerstarts[mthing.type-1] = *mthing;
if (!deathmatch)
  P_SpawnPlayer (mthing);

return;
}

// check for apropriate skill level
if (!netgame && (mthing.options & 16) )
return;
  
if (gameskill == sk_baby)
bit = 1;
else if (gameskill == sk_nightmare)
bit = 4;
else
bit = 1<<(gameskill-1);

if (!(mthing.options & bit) )
return;

// find which type to spawn
for (i=0 ; i< NUMMOBJTYPES ; i++)
if (mthing.type == mobjinfo[i].doomednum)
  break;

if (i==NUMMOBJTYPES)
I_Error ("P_SpawnMapThing: Unknown type %i at (%i, %i)",
   mthing.type,
   mthing.x, mthing.y);
  
// don't spawn keycards and players in deathmatch
if (deathmatch && mobjinfo[i].flags & MF_NOTDMATCH)
return;
  
// don't spawn any monsters if -nomonsters
if (nomonsters
&& ( i == MT_SKULL
   || (mobjinfo[i].flags & MF_COUNTKILL)) )
{
return;
}

// spawn it
x = mthing.x << FRACBITS;
y = mthing.y << FRACBITS;

if (mobjinfo[i].flags & MF_SPAWNCEILING)
z = ONCEILINGZ;
else
z = ONFLOORZ;

mobj = P_SpawnMobj (x,y,z, i);
mobj.spawnpoint = *mthing;

if (mobj.tics > 0)
mobj.tics = 1 + (P_Random () % mobj.tics);
if (mobj.flags & MF_COUNTKILL)
totalkills++;
if (mobj.flags & MF_COUNTITEM)
totalitems++;
  
mobj.angle = ANG45 * (mthing.angle/45);
if (mthing.options & MTF_AMBUSH)
mobj.flags |= MF_AMBUSH;
}



//
// GAME SPAWN FUNCTIONS
//


//
// P_SpawnPuff
//
extern fixed_t attackrange;

void
P_SpawnPuff
( fixed_t   x,
fixed_t   y,
fixed_t   z )
{
mobj_t th;

z += ((P_Random()-P_Random())<<10);

th = P_SpawnMobj (x,y,z, MT_PUFF);
th.momz = FRACUNIT;
th.tics -= P_Random()&3;

if (th.tics < 1)
th.tics = 1;

// don't make punches spark on the wall
if (attackrange == MELEERANGE)
P_SetMobjState (th, S_PUFF3);
}



//
// P_SpawnBlood
// 
void
P_SpawnBlood
( fixed_t   x,
fixed_t   y,
fixed_t   z,
int       damage )
{
mobj_t th;

z += ((P_Random()-P_Random())<<10);
th = P_SpawnMobj (x,y,z, MT_BLOOD);
th.momz = FRACUNIT*2;
th.tics -= P_Random()&3;

if (th.tics < 1)
th.tics = 1;
  
if (damage <= 12 && damage >= 9)
P_SetMobjState (th,S_BLOOD2);
else if (damage < 9)
P_SetMobjState (th,S_BLOOD3);
}



//
// P_CheckMissileSpawn
// Moves the missile forward a bit
//  and possibly explodes it right there.
//
void P_CheckMissileSpawn (mobj_t th)
{
th.tics -= P_Random()&3;
if (th.tics < 1)
th.tics = 1;

// move a little forward so an angle can
// be computed if it immediately explodes
th.x += (th.momx>>1);
th.y += (th.momy>>1);
th.z += (th.momz>>1);

if (!P_TryMove (th, th.x, th.y))
P_ExplodeMissile (th);
}


//
// P_SpawnMissile
//
private mobj_t
P_SpawnMissile
( mobj_t   source,
mobj_t   dest,
mobjtype_t    type )
{
mobj_t th;
int an; // angle_t
int     dist;

th = P_SpawnMobj (source.x,
        source.y,
        source.z + 4*8*FRACUNIT, type);

if (th.info.seesound)
S_StartSound (th, th.info.seesound);

th.target = source;    // where it came from
an = R_PointToAngle2 (source.x, source.y, dest.x, dest.y);  

// fuzzy player
if (dest.flags & MF_SHADOW)
an += (P_Random()-P_Random())<<20;  

th.angle = an;
an >>= ANGLETOFINESHIFT;
th.momx = FixedMul (th.info.speed, finecosine[an]);
th.momy = FixedMul (th.info.speed, finesine[an]);

dist = P_AproxDistance (dest.x - source.x, dest.y - source.y);
dist = dist / th.info.speed;

if (dist < 1)
dist = 1;

th.momz = (dest.z - source.z) / dist;
P_CheckMissileSpawn (th);

return th;
}


/**
* P_SpawnPlayerMissile
* Tries to aim at a nearby monster
*/

public void
SpawnPlayerMissile
( mobj_t   source,
mobjtype_t    type )
{
mobj_t th;
int an; // angle_t
int x, y, z,slope; // think_t

// see which target is to be aimed at
an = source.angle;
slope = AimLineAttack (source, an, 16*64*FRACUNIT);

if (!linetarget)
{
an += 1<<26;
slope = AimLineAttack (source, an, 16*64*FRACUNIT);

if (!linetarget)
{
  an -= 2<<26;
  slope = AimLineAttack (source, an, 16*64*FRACUNIT);
}

if (!linetarget)
{
  an = source.angle;
  slope = 0;
}
}
  
x = source.x;
y = source.y;
z = source.z + 4*8*FRACUNIT;

th = P_SpawnMobj (x,y,z, type);

if (th.info.seesound)
S_StartSound (th, th.info.seesound);

th.target = source;
th.angle = an;
th.momx = FixedMul( th.info.speed,
       finecosine[an>>ANGLETOFINESHIFT]);
th.momy = FixedMul( th.info.speed,
       finesine[an>>ANGLETOFINESHIFT]);
th.momz = FixedMul( th.info.speed, slope);

P_CheckMissileSpawn (th);
}

}

    //
    // P_DamageMobj
    // Damages both enemies and players
    // "inflictor" is the thing that caused the damage
    //  creature or missile, can be NULL (slime, etc)
    // "source" is the thing to target after taking damage
    //  creature or NULL
    // Source and inflictor are the same for melee attacks.
    // Source can be NULL for slime, barrel explosions
    // and other environmental stuff.
    //
    public void
    P_DamageMobj
    ( mobj_t   target,
      mobj_t   inflictor,
      mobj_t   source,
      int       damage )
    {
        int    ang; // unsigned
        int     saved;
        player_t   player;
        int thrust; // fixed_t
        int     temp;
        
        if ( !flags(target.flags, MF_SHOOTABLE))
        return; // shouldn't happen...
            
        if (target.health <= 0)
        return;

        if ( flags(target.flags , MF_SKULLFLY ))
        {
        target.momx = target.momy = target.momz = 0;
        }
        
        player = target.player;
        if ((player!=null) && DS.gameskill == skill_t.sk_baby)
        damage >>= 1;   // take half damage in trainer mode
            

        // Some close combat weapons should not
        // inflict thrust and push the victim out of reach,
        // thus kick away unless using the chainsaw.
        if ((inflictor !=null)
        && !flags(target.flags, MF_NOCLIP)
        && (source==null
            || source.player==null
            || source.player.readyweapon != weapontype_t.wp_chainsaw))
        {
        ang = R.PointToAngle2 ( inflictor.x,
                    inflictor.y,
                    target.x,
                    target.y);
            
        thrust = damage*(FRACUNIT>>3)*100/target.info.mass;

        // make fall forwards sometimes
        if ( (damage < 40)
             && (damage > target.health)
             && (target.z - inflictor.z > 64*FRACUNIT)
             && flags(RND.P_Random(),1) )
        {
            ang += ANG180;
            thrust *= 4;
        }
            
        ang >>= ANGLETOFINESHIFT;
        target.momx += FixedMul (thrust, finecosine[ang]);
        target.momy += FixedMul (thrust, finesine[ang]);
        }
        
        // player specific
        if (player!=null)
        {
        // end of game hell hack
        if (target.subsector.sector.special == 11
            && damage >= target.health)
        {
            damage = target.health - 1;
        }
        

        // Below certain threshold,
        // ignore damage in GOD mode, or with INVUL power.
        if ( damage < 1000
             && ( flags(player.cheats,player_t.CF_GODMODE))
              || player.powers[pw_invulnerability]!=0 ) 
        {
            return;
        }
        
        if (player.armortype!=0)
        {
            if (player.armortype == 1)
            saved = damage/3;
            else
            saved = damage/2;
            
            if (player.armorpoints[0] <= saved)
            {
            // armor is used up
            saved = player.armorpoints[0];
            player.armortype = 0;
            }
            player.armorpoints[0] -= saved;
            damage -= saved;
        }
        player.health[0] -= damage;   // mirror mobj health here for Dave
        if (player.health[0] < 0)
            player.health[0] = 0;
        
        player.attacker = source;
        player.damagecount += damage;  // add damage after armor / invuln

        if (player.damagecount > 100)
            player.damagecount = 100;  // teleport stomp does 10k points...
        
        temp = damage < 100 ? damage : 100;

        if (player == DS.players[DS.consoleplayer]) ;
            // TODO: I_Tactile (40,10,40+temp*2);
        }
        
        // do the damage    
        target.health -= damage;   
        if (target.health <= 0)
        {
        KillMobj (source, target);
        return;
        }

        if ( (RND.P_Random () < target.info.painchance)
         && !flags(target.flags,MF_SKULLFLY) )
        {
        target.flags |= MF_JUSTHIT;    // fight back!
        
        target.SetMobjState (target.info.painstate);
        }
                
        target.reactiontime = 0;       // we're awake now...   

        if ( ((target.threshold==0) || (target.type == mobjtype_t.MT_VILE))
         && (source!=null) && (source != target)
         && (source.type != mobjtype_t.MT_VILE))
        {
        // if not intent on another player,
        // chase after this one
        target.target = source;
        target.threshold = BASETHRESHOLD;
        if (target.state == states[target.info.spawnstate.ordinal()]
            && target.info.seestate != statenum_t.S_NULL)
            target.SetMobjState (target.info.seestate);
        }
                
    }
    
    //
    // KillMobj
    //
    public void
    KillMobj
    ( mobj_t   source,
      mobj_t   target )
    {
        mobjtype_t  item;
        mobj_t mo;
        
        target.flags &= ~(MF_SHOOTABLE|MF_FLOAT|MF_SKULLFLY);

        if (target.type != mobjtype_t.MT_SKULL)
        target.flags &= ~MF_NOGRAVITY;

        target.flags |= MF_CORPSE|MF_DROPOFF;
        target.height >>= 2;

        if (source!=null && source.player!=null)
        {
        // count for intermission
        if ((target.flags & MF_COUNTKILL)!=0)
            source.player.killcount++;    

        if (target.player!=null) ;
           // TODO: source.player.frags[target.player-DS.players]++;
           // It's probably intended to increment the frags of source player vs target player. Lookup? 
        }
        else if (!DS.netgame && ((target.flags & MF_COUNTKILL)!=0) )
        {
        // count all monster deaths,
        // even those caused by other monsters
        DS.players[0].killcount++;
        }
        
        if (target.player!=null)
        {
        // count environment kills against you
        if (source==null)    
            // TODO: some way to indentify which one of the 
            // four possiblelayers is the current player
            
            target.player.frags[target.player.identify()]++;
                
        target.flags &= ~MF_SOLID;
        target.player.playerstate = PST_DEAD;
        //TODO: DropWeapon (target.player); // in PSPR

        if (target.player == DS.players[DS.consoleplayer]
            && DS.automapactive)
        {
            // don't die in auto map,
            // switch view prior to dying
            AM.Stop ();
        }
        
        }

        if (target.health < -target.info.spawnhealth 
        && target.info.xdeathstate!=null)
        {
            target.SetMobjState(target.info.xdeathstate);
        }
        else
            target.SetMobjState (target.info.deathstate);
        target.tics -= RND.P_Random()&3;

        if (target.tics < 1)
        target.tics = 1;
            
        //  I_StartSound (&actor.r, actor.info.deathsound);


        // Drop stuff.
        // This determines the kind of object spawned
        // during the death frame of a thing.
        switch (target.type)
        {
          case MT_WOLFSS:
          case MT_POSSESSED:
        item = mobjtype_t.MT_CLIP;
        break;
        
          case MT_SHOTGUY:
        item = mobjtype_t.MT_SHOTGUN;
        break;
        
          case MT_CHAINGUY:
        item = mobjtype_t.MT_CHAINGUN;
        break;
        
          default:
        return;
        }

        mo = P_SpawnMobj (target.x,target.y,ONFLOORZ, item);
        mo.flags |= MF_DROPPED;    // special versions of items
    }
    
 /** P_SpawnMobj
  * 
  * @param x fixed
  * @param y fixed
  * @param z fixed
  * @param type
  * @return
  */

 public mobj_t
 P_SpawnMobj
 ( int   x,
		 int   y,
		 int   z,
 mobjtype_t    type )
 {
 mobj_t mobj;
 state_t    st;
 mobjinfo_t info;

 mobj = new mobj_t();
 info = mobjinfo[type.ordinal()];

 mobj.type = type;
 mobj.info = info;
 mobj.x = x;
 mobj.y = y;
 mobj.radius = info.radius;
 mobj.height = info.height;
 mobj.flags = info.flags;
 mobj.health = info.spawnhealth;

 if (DS.gameskill != skill_t.sk_nightmare)
 mobj.reactiontime = info.reactiontime;

 mobj.lastlook = RND.P_Random () % MAXPLAYERS;
 // do not set the state with P_SetMobjState,
 // because action routines can not be called yet
 st = states[info.spawnstate.ordinal()];

 mobj.state = st;
 mobj.tics = st.tics;
 mobj.sprite = st.sprite;
 mobj.frame = st.frame;

 // set subsector and/or block links
 SetThingPosition (mobj);

 mobj.floorz = mobj.subsector.sector.floorheight;
 mobj.ceilingz = mobj.subsector.sector.ceilingheight;

 if (z == ONFLOORZ)
 mobj.z = mobj.floorz;
 else if (z == ONCEILINGZ)
 mobj.z = mobj.ceilingz - mobj.info.height;
 else 
 mobj.z = z;

 mobj.thinker.function=P_MobjThinker;

 AddThinker (mobj.thinker);

 return mobj;
 }
 
 /** P_SetThingPosition
  * Links a thing into both a block and a subsector
  * based on it's x y.
  * Sets thing.subsector properly
  */

 public void SetThingPosition (mobj_t thing)
 {
  subsector_t    ss;
  sector_t       sec;
  int         blockx;
  int         blocky;
  mobj_t        link;

  
  // link into subsector
  ss = R.PointInSubsector (thing.x,thing.y);
  thing.subsector = ss;
  
  if ( ! flags(thing.flags ,MF_NOSECTOR) )
  {
  // invisible things don't go into the sector links
  sec = ss.sector;
  
  thing.sprev = null;
  thing.snext = sec.thinglist;

  if (sec.thinglist!=null)
      sec.thinglist.sprev = thing;

  sec.thinglist = thing;
  }

  
  // link into blockmap
  if ( ! flags(thing.flags ,MF_NOBLOCKMAP) )
  {
  // inert things don't need to be in blockmap        
  blockx = (thing.x - LL.bmaporgx)>>MAPBLOCKSHIFT;
  blocky = (thing.y - LL.bmaporgy)>>MAPBLOCKSHIFT;

  if (blockx>=0
      && blockx < LL.bmapwidth
      && blocky>=0
      && blocky < LL.bmapheight)
  {
 	 
      link = LL.blocklinks[blocky*LL.bmapwidth+blockx];
      thing.bprev = null;
      thing.bnext = link; // FIXME: will this work?
      if (link!=null)
      // This will work
      link.bprev = thing;

      // link=thing won't work, assignment should be made directly
      LL.blocklinks[blocky*LL.bmapwidth+blockx]=thing;
  }
  else
  {
      // thing is off the map
      thing.bnext = thing.bprev = null;
  }
  }

    
 }
 
 public class Doors{

	 /**
	 //
	 // Sliding door frame information
	 //
	 slidename_t	slideFrameNames[MAXSLIDEDOORS] =
	 {
	     {"GDOORF1","GDOORF2","GDOORF3","GDOORF4",	// front
	      "GDOORB1","GDOORB2","GDOORB3","GDOORB4"},	// back
	 	 
	     {"\0","\0","\0","\0"}
	 };
	 */


	 //
	 // VERTICAL DOORS
	 //

	 //
	 // T_VerticalDoor
	 //
	 public void T_VerticalDoor (vldoor_t door)
	 {
	     result_e	res;
	 	
	     switch(door.direction)
	     {
	       case 0:
	 	// WAITING
	 	if (!--door.topcountdown)
	 	{
	 	    switch(door.type)
	 	    {
	 	      case blazeRaise:
	 		door.direction = -1; // time to go back down
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_bdcls);
	 		break;
	 		
	 	      case normal:
	 		door.direction = -1; // time to go back down
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_dorcls);
	 		break;
	 		
	 	      case close30ThenOpen:
	 		door.direction = 1;
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_doropn);
	 		break;
	 		
	 	      default:
	 		break;
	 	    }
	 	}
	 	break;
	 	
	       case 2:
	 	//  INITIAL WAIT
	 	if (!--door.topcountdown)
	 	{
	 	    switch(door.type)
	 	    {
	 	      case raiseIn5Mins:
	 		door.direction = 1;
	 		door.type = normal;
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_doropn);
	 		break;
	 		
	 	      default:
	 		break;
	 	    }
	 	}
	 	break;
	 	
	       case -1:
	 	// DOWN
	 	res = T_MovePlane(door.sector,
	 			  door.speed,
	 			  door.sector.floorheight,
	 			  false,1,door.direction);
	 	if (res == pastdest)
	 	{
	 	    switch(door.type)
	 	    {
	 	      case blazeRaise:
	 	      case blazeClose:
	 		door.sector.specialdata = NULL;
	 		P_RemoveThinker (&door.thinker);  // unlink and free
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_bdcls);
	 		break;
	 		
	 	      case normal:
	 	      case close:
	 		door.sector.specialdata = NULL;
	 		P_RemoveThinker (&door.thinker);  // unlink and free
	 		break;
	 		
	 	      case close30ThenOpen:
	 		door.direction = 0;
	 		door.topcountdown = 35*30;
	 		break;
	 		
	 	      default:
	 		break;
	 	    }
	 	}
	 	else if (res == crushed)
	 	{
	 	    switch(door.type)
	 	    {
	 	      case blazeClose:
	 	      case close:		// DO NOT GO BACK UP!
	 		break;
	 		
	 	      default:
	 		door.direction = 1;
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_doropn);
	 		break;
	 	    }
	 	}
	 	break;
	 	
	       case 1:
	 	// UP
	 	res = T_MovePlane(door.sector,
	 			  door.speed,
	 			  door.topheight,
	 			  false,1,door.direction);
	 	
	 	if (res == pastdest)
	 	{
	 	    switch(door.type)
	 	    {
	 	      case blazeRaise:
	 	      case normal:
	 		door.direction = 0; // wait at top
	 		door.topcountdown = door.topwait;
	 		break;
	 		
	 	      case close30ThenOpen:
	 	      case blazeOpen:
	 	      case open:
	 		door.sector.specialdata = NULL;
	 		P_RemoveThinker (&door.thinker);  // unlink and free
	 		break;
	 		
	 	      default:
	 		break;
	 	    }
	 	}
	 	break;
	     }
	 }


	 //
	 // EV_DoLockedDoor
	 // Move a locked door up/down
	 //

	 int
	 EV_DoLockedDoor
	 ( line_t*	line,
	   vldoor_e	type,
	   mobj_t*	thing )
	 {
	     player_t*	p;
	 	
	     p = thing.player;
	 	
	     if (!p)
	 	return 0;
	 		
	     switch(line.special)
	     {
	       case 99:	// Blue Lock
	       case 133:
	 	if ( !p )
	 	    return 0;
	 	if (!p.cards[it_bluecard] && !p.cards[it_blueskull])
	 	{
	 	    p.message = PD_BLUEO;
	 	    S_StartSound(NULL,sfx_oof);
	 	    return 0;
	 	}
	 	break;
	 	
	       case 134: // Red Lock
	       case 135:
	 	if ( !p )
	 	    return 0;
	 	if (!p.cards[it_redcard] && !p.cards[it_redskull])
	 	{
	 	    p.message = PD_REDO;
	 	    S_StartSound(NULL,sfx_oof);
	 	    return 0;
	 	}
	 	break;
	 	
	       case 136:	// Yellow Lock
	       case 137:
	 	if ( !p )
	 	    return 0;
	 	if (!p.cards[it_yellowcard] &&
	 	    !p.cards[it_yellowskull])
	 	{
	 	    p.message = PD_YELLOWO;
	 	    S_StartSound(NULL,sfx_oof);
	 	    return 0;
	 	}
	 	break;	
	     }

	     return EV_DoDoor(line,type);
	 }


	 int
	 EV_DoDoor
	 ( line_t*	line,
	   vldoor_e	type )
	 {
	     int		secnum,rtn;
	     sector_t*	sec;
	     vldoor_t*	door;
	 	
	     secnum = -1;
	     rtn = 0;
	     
	     while ((secnum = P_FindSectorFromLineTag(line,secnum)) >= 0)
	     {
	 	sec = &sectors[secnum];
	 	if (sec.specialdata)
	 	    continue;
	 		
	 	
	 	// new door thinker
	 	rtn = 1;
	 	door = Z_Malloc (sizeof(*door), PU_LEVSPEC, 0);
	 	P_AddThinker (&door.thinker);
	 	sec.specialdata = door;

	 	door.thinker.function.acp1 = (actionf_p1) T_VerticalDoor;
	 	door.sector = sec;
	 	door.type = type;
	 	door.topwait = VDOORWAIT;
	 	door.speed = VDOORSPEED;
	 		
	 	switch(type)
	 	{
	 	  case blazeClose:
	 	    door.topheight = P_FindLowestCeilingSurrounding(sec);
	 	    door.topheight -= 4*FRACUNIT;
	 	    door.direction = -1;
	 	    door.speed = VDOORSPEED * 4;
	 	    S_StartSound((mobj_t *)&door.sector.soundorg,
	 			 sfx_bdcls);
	 	    break;
	 	    
	 	  case close:
	 	    door.topheight = P_FindLowestCeilingSurrounding(sec);
	 	    door.topheight -= 4*FRACUNIT;
	 	    door.direction = -1;
	 	    S_StartSound((mobj_t *)&door.sector.soundorg,
	 			 sfx_dorcls);
	 	    break;
	 	    
	 	  case close30ThenOpen:
	 	    door.topheight = sec.ceilingheight;
	 	    door.direction = -1;
	 	    S_StartSound((mobj_t *)&door.sector.soundorg,
	 			 sfx_dorcls);
	 	    break;
	 	    
	 	  case blazeRaise:
	 	  case blazeOpen:
	 	    door.direction = 1;
	 	    door.topheight = P_FindLowestCeilingSurrounding(sec);
	 	    door.topheight -= 4*FRACUNIT;
	 	    door.speed = VDOORSPEED * 4;
	 	    if (door.topheight != sec.ceilingheight)
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_bdopn);
	 	    break;
	 	    
	 	  case normal:
	 	  case open:
	 	    door.direction = 1;
	 	    door.topheight = P_FindLowestCeilingSurrounding(sec);
	 	    door.topheight -= 4*FRACUNIT;
	 	    if (door.topheight != sec.ceilingheight)
	 		S_StartSound((mobj_t *)&door.sector.soundorg,
	 			     sfx_doropn);
	 	    break;
	 	    
	 	  default:
	 	    break;
	 	}
	 		
	     }
	     return rtn;
	 }


	 //
	 // EV_VerticalDoor : open a door manually, no tag value
	 //
	 void
	 EV_VerticalDoor
	 ( line_t*	line,
	   mobj_t*	thing )
	 {
	     player_t*	player;
	     int		secnum;
	     sector_t*	sec;
	     vldoor_t*	door;
	     int		side;
	 	
	     side = 0;	// only front sides can be used

	     //	Check for locks
	     player = thing.player;
	 		
	     switch(line.special)
	     {
	       case 26: // Blue Lock
	       case 32:
	 	if ( !player )
	 	    return;
	 	
	 	if (!player.cards[it_bluecard] && !player.cards[it_blueskull])
	 	{
	 	    player.message = PD_BLUEK;
	 	    S_StartSound(NULL,sfx_oof);
	 	    return;
	 	}
	 	break;
	 	
	       case 27: // Yellow Lock
	       case 34:
	 	if ( !player )
	 	    return;
	 	
	 	if (!player.cards[it_yellowcard] &&
	 	    !player.cards[it_yellowskull])
	 	{
	 	    player.message = PD_YELLOWK;
	 	    S_StartSound(NULL,sfx_oof);
	 	    return;
	 	}
	 	break;
	 	
	       case 28: // Red Lock
	       case 33:
	 	if ( !player )
	 	    return;
	 	
	 	if (!player.cards[it_redcard] && !player.cards[it_redskull])
	 	{
	 	    player.message = PD_REDK;
	 	    S_StartSound(NULL,sfx_oof);
	 	    return;
	 	}
	 	break;
	     }
	 	
	     // if the sector has an active thinker, use it
	     sec = sides[ line.sidenum[side^1]] .sector;
	     secnum = sec-sectors;

	     if (sec.specialdata)
	     {
	 	door = sec.specialdata;
	 	switch(line.special)
	 	{
	 	  case	1: // ONLY FOR "RAISE" DOORS, NOT "OPEN"s
	 	  case	26:
	 	  case	27:
	 	  case	28:
	 	  case	117:
	 	    if (door.direction == -1)
	 		door.direction = 1;	// go back up
	 	    else
	 	    {
	 		if (!thing.player)
	 		    return;		// JDC: bad guys never close doors
	 		
	 		door.direction = -1;	// start going down immediately
	 	    }
	 	    return;
	 	}
	     }
	 	
	     // for proper sound
	     switch(line.special)
	     {
	       case 117:	// BLAZING DOOR RAISE
	       case 118:	// BLAZING DOOR OPEN
	 	S_StartSound((mobj_t *)&sec.soundorg,sfx_bdopn);
	 	break;
	 	
	       case 1:	// NORMAL DOOR SOUND
	       case 31:
	 	S_StartSound((mobj_t *)&sec.soundorg,sfx_doropn);
	 	break;
	 	
	       default:	// LOCKED DOOR SOUND
	 	S_StartSound((mobj_t *)&sec.soundorg,sfx_doropn);
	 	break;
	     }
	 	
	     
	     // new door thinker
	     door = Z_Malloc (sizeof(*door), PU_LEVSPEC, 0);
	     P_AddThinker (&door.thinker);
	     sec.specialdata = door;
	     door.thinker.function.acp1 = (actionf_p1) T_VerticalDoor;
	     door.sector = sec;
	     door.direction = 1;
	     door.speed = VDOORSPEED;
	     door.topwait = VDOORWAIT;

	     switch(line.special)
	     {
	       case 1:
	       case 26:
	       case 27:
	       case 28:
	 	door.type = normal;
	 	break;
	 	
	       case 31:
	       case 32:
	       case 33:
	       case 34:
	 	door.type = open;
	 	line.special = 0;
	 	break;
	 	
	       case 117:	// blazing door raise
	 	door.type = blazeRaise;
	 	door.speed = VDOORSPEED*4;
	 	break;
	       case 118:	// blazing door open
	 	door.type = blazeOpen;
	 	line.special = 0;
	 	door.speed = VDOORSPEED*4;
	 	break;
	     }
	     
	     // find the top and bottom of the movement range
	     door.topheight = P_FindLowestCeilingSurrounding(sec);
	     door.topheight -= 4*FRACUNIT;
	 }


	 //
	 // Spawn a door that closes after 30 seconds
	 //
	 public void P_SpawnDoorCloseIn30 (sector_t sec)
	 {
	     vldoor_t	door;
	 	
	     door = new vldoor_t();

	     P_AddThinker (door.thinker);

	     sec.specialdata = door;
	     sec.special = 0;

	     door.thinker.function.acp1 = (actionf_p1)T_VerticalDoor;
	     door.sector = sec;
	     door.direction = 0;
	     door.type = normal;
	     door.speed = VDOORSPEED;
	     door.topcountdown = 30 * 35;
	 }

	 //
	 // Spawn a door that opens after 5 minutes
	 //
	 public void
	 P_SpawnDoorRaiseIn5Mins
	 ( sector_t	sec,
	   int		secnum )
	 {
	     vldoor_t	door;
	 	
	     door = new vldoor_t();
	     
	     P_AddThinker (door.thinker);

	     sec.specialdata = door;
	     sec.special = 0;

	     door.thinker.function.acp1 = (actionf_p1)T_VerticalDoor;
	     door.sector = sec;
	     door.direction = 2;
	     door.type = raiseIn5Mins;
	     door.speed = VDOORSPEED;
	     door.topheight = P_FindLowestCeilingSurrounding(sec);
	     door.topheight -= 4*FRACUNIT;
	     door.topwait = VDOORWAIT;
	     door.topcountdown = 5 * 60 * 35;
	 }



	 // UNUSED
	 // Separate into p_slidoor.c?

	 /*
	 // ABANDONED TO THE MISTS OF TIME!!!
	 //
	 // EV_SlidingDoor : slide a door horizontally
	 // (animate midtexture, then set noblocking line)
	 //


	 slideframe_t slideFrames[MAXSLIDEDOORS];

	 void P_InitSlidingDoorFrames(void)
	 {
	     int		i;
	     int		f1;
	     int		f2;
	     int		f3;
	     int		f4;
	 	
	     // DOOM II ONLY...
	     if ( gamemode != commercial)
	 	return;
	 	
	     for (i = 0;i < MAXSLIDEDOORS; i++)
	     {
	 	if (!slideFrameNames[i].frontFrame1[0])
	 	    break;
	 			
	 	f1 = R_TextureNumForName(slideFrameNames[i].frontFrame1);
	 	f2 = R_TextureNumForName(slideFrameNames[i].frontFrame2);
	 	f3 = R_TextureNumForName(slideFrameNames[i].frontFrame3);
	 	f4 = R_TextureNumForName(slideFrameNames[i].frontFrame4);

	 	slideFrames[i].frontFrames[0] = f1;
	 	slideFrames[i].frontFrames[1] = f2;
	 	slideFrames[i].frontFrames[2] = f3;
	 	slideFrames[i].frontFrames[3] = f4;
	 		
	 	f1 = R_TextureNumForName(slideFrameNames[i].backFrame1);
	 	f2 = R_TextureNumForName(slideFrameNames[i].backFrame2);
	 	f3 = R_TextureNumForName(slideFrameNames[i].backFrame3);
	 	f4 = R_TextureNumForName(slideFrameNames[i].backFrame4);

	 	slideFrames[i].backFrames[0] = f1;
	 	slideFrames[i].backFrames[1] = f2;
	 	slideFrames[i].backFrames[2] = f3;
	 	slideFrames[i].backFrames[3] = f4;
	     }
	 }


	 //
	 // Return index into "slideFrames" array
	 // for which door type to use
	 //
	 int P_FindSlidingDoorType(line_t*	line)
	 {
	     int		i;
	     int		val;
	 	
	     for (i = 0;i < MAXSLIDEDOORS;i++)
	     {
	 	val = sides[line.sidenum[0]].midtexture;
	 	if (val == slideFrames[i].frontFrames[0])
	 	    return i;
	     }
	 	
	     return -1;
	 }

	 void T_SlidingDoor (slidedoor_t*	door)
	 {
	     switch(door.status)
	     {
	       case sd_opening:
	 	if (!door.timer--)
	 	{
	 	    if (++door.frame == SNUMFRAMES)
	 	    {
	 		// IF DOOR IS DONE OPENING...
	 		sides[door.line.sidenum[0]].midtexture = 0;
	 		sides[door.line.sidenum[1]].midtexture = 0;
	 		door.line.flags &= ML_BLOCKING^0xff;
	 					
	 		if (door.type == sdt_openOnly)
	 		{
	 		    door.frontsector.specialdata = NULL;
	 		    P_RemoveThinker (&door.thinker);
	 		    break;
	 		}
	 					
	 		door.timer = SDOORWAIT;
	 		door.status = sd_waiting;
	 	    }
	 	    else
	 	    {
	 		// IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
	 		door.timer = SWAITTICS;
	 					
	 		sides[door.line.sidenum[0]].midtexture =
	 		    slideFrames[door.whichDoorIndex].
	 		    frontFrames[door.frame];
	 		sides[door.line.sidenum[1]].midtexture =
	 		    slideFrames[door.whichDoorIndex].
	 		    backFrames[door.frame];
	 	    }
	 	}
	 	break;
	 			
	       case sd_waiting:
	 	// IF DOOR IS DONE WAITING...
	 	if (!door.timer--)
	 	{
	 	    // CAN DOOR CLOSE?
	 	    if (door.frontsector.thinglist != NULL ||
	 		door.backsector.thinglist != NULL)
	 	    {
	 		door.timer = SDOORWAIT;
	 		break;
	 	    }

	 	    //door.frame = SNUMFRAMES-1;
	 	    door.status = sd_closing;
	 	    door.timer = SWAITTICS;
	 	}
	 	break;
	 			
	       case sd_closing:
	 	if (!door.timer--)
	 	{
	 	    if (--door.frame < 0)
	 	    {
	 		// IF DOOR IS DONE CLOSING...
	 		door.line.flags |= ML_BLOCKING;
	 		door.frontsector.specialdata = NULL;
	 		P_RemoveThinker (&door.thinker);
	 		break;
	 	    }
	 	    else
	 	    {
	 		// IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
	 		door.timer = SWAITTICS;
	 					
	 		sides[door.line.sidenum[0]].midtexture =
	 		    slideFrames[door.whichDoorIndex].
	 		    frontFrames[door.frame];
	 		sides[door.line.sidenum[1]].midtexture =
	 		    slideFrames[door.whichDoorIndex].
	 		    backFrames[door.frame];
	 	    }
	 	}
	 	break;
	     }
	 }



	 void
	 EV_SlidingDoor
	 ( line_t*	line,
	   mobj_t*	thing )
	 {
	     sector_t*		sec;
	     slidedoor_t*	door;
	 	
	     // DOOM II ONLY...
	     if (gamemode != commercial)
	 	return;
	     
	     // Make sure door isn't already being animated
	     sec = line.frontsector;
	     door = NULL;
	     if (sec.specialdata)
	     {
	 	if (!thing.player)
	 	    return;
	 			
	 	door = sec.specialdata;
	 	if (door.type == sdt_openAndClose)
	 	{
	 	    if (door.status == sd_waiting)
	 		door.status = sd_closing;
	 	}
	 	else
	 	    return;
	     }
	     
	     // Init sliding door vars
	     if (!door)
	     {
	 	door = Z_Malloc (sizeof(*door), PU_LEVSPEC, 0);
	 	P_AddThinker (&door.thinker);
	 	sec.specialdata = door;
	 		
	 	door.type = sdt_openAndClose;
	 	door.status = sd_opening;
	 	door.whichDoorIndex = P_FindSlidingDoorType(line);

	 	if (door.whichDoorIndex < 0)
	 	    I_Error("EV_SlidingDoor: Can't use texture for sliding door!");
	 			
	 	door.frontsector = sec;
	 	door.backsector = line.backsector;
	 	door.thinker.function = T_SlidingDoor;
	 	door.timer = SWAITTICS;
	 	door.frame = 0;
	 	door.line = line;
	     }
	 }
	 */
}
 
 class Switches {

	 //
	 // CHANGE THE TEXTURE OF A WALL SWITCH TO ITS OPPOSITE
	 //
	 switchlist_t[] alphSwitchList =
	 {
	     // Doom shareware episode 1 switches
	     new switchlist_t("SW1BRCOM",	"SW2BRCOM",	1),
	     new switchlist_t("SW1BRN1",	"SW2BRN1",	1),
	     new switchlist_t("SW1BRN2",	"SW2BRN2",	1),
	     new switchlist_t("SW1BRNGN",	"SW2BRNGN",	1),
	     new switchlist_t("SW1BROWN",	"SW2BROWN",	1),
	     new switchlist_t("SW1COMM",	"SW2COMM",	1),
	     new switchlist_t("SW1COMP",	"SW2COMP",	1),
	     new switchlist_t("SW1DIRT",	"SW2DIRT",	1),
	     new switchlist_t("SW1EXIT",	"SW2EXIT",	1),
	     new switchlist_t("SW1GRAY",	"SW2GRAY",	1),
	     new switchlist_t("SW1GRAY1",	"SW2GRAY1",	1),
	     new switchlist_t("SW1METAL",	"SW2METAL",	1),
	     new switchlist_t("SW1PIPE",	"SW2PIPE",	1),
	     new switchlist_t("SW1SLAD",	"SW2SLAD",	1),
	     new switchlist_t("SW1STARG",	"SW2STARG",	1),
	     new switchlist_t("SW1STON1",	"SW2STON1",	1),
	     new switchlist_t("SW1STON2",	"SW2STON2",	1),
	     new switchlist_t("SW1STONE",	"SW2STONE",	1),
	     new switchlist_t("SW1STRTN",	"SW2STRTN",	1),
	     
	     // Doom registered episodes 2&3 switches
	     new switchlist_t("SW1BLUE",	"SW2BLUE",	2),
	     new switchlist_t("SW1CMT",		"SW2CMT",	2),
	     new switchlist_t("SW1GARG",	"SW2GARG",	2),
	     new switchlist_t("SW1GSTON",	"SW2GSTON",	2),
	     new switchlist_t("SW1HOT",		"SW2HOT",	2),
	     new switchlist_t("SW1LION",	"SW2LION",	2),
	     new switchlist_t("SW1SATYR",	"SW2SATYR",	2),
	     new switchlist_t("SW1SKIN",	"SW2SKIN",	2),
	     new switchlist_t("SW1VINE",	"SW2VINE",	2),
	     new switchlist_t("SW1WOOD",	"SW2WOOD",	2),
	     
	     // Doom II switches
	     new switchlist_t("SW1PANEL",	"SW2PANEL",	3),
	     new switchlist_t("SW1ROCK",	"SW2ROCK",	3),
	     new switchlist_t("SW1MET2",	"SW2MET2",	3),
	     new switchlist_t("SW1WDMET",	"SW2WDMET",	3),
	     new switchlist_t("SW1BRIK",	"SW2BRIK",	3),
	     new switchlist_t("SW1MOD1",	"SW2MOD1",	3),
	     new switchlist_t("SW1ZIM",		"SW2ZIM",	3),
	     new switchlist_t("SW1STON6",	"SW2STON6",	3),
	     new switchlist_t("SW1TEK",		"SW2TEK",	3),
	     new switchlist_t("SW1MARB",	"SW2MARB",	3),
	     new switchlist_t("SW1SKULL",	"SW2SKULL",	3),
	 	
	     new switchlist_t("\0",		"\0",		0)
	 };

	 int[]		switchlist=new int[MAXSWITCHES * 2];
	 int		numswitches;
	 button_t[]        buttonlist=button_t[MAXBUTTONS];

	 //
	 // P_InitSwitchList
	 // Only called at game initialization.
	 //
	 public void P_InitSwitchList()
	 {
	     int		i;
	     int		index;
	     int		episode;
	 	
	     episode = 1;

	     if (gamemode == registered)
	 	episode = 2;
	     else
	 	if ( gamemode == commercial )
	 	    episode = 3;
	 		
	     for (index = 0,i = 0;i < MAXSWITCHES;i++)
	     {
	 	if (!alphSwitchList[i].episode)
	 	{
	 	    numswitches = index/2;
	 	    switchlist[index] = -1;
	 	    break;
	 	}
	 		
	 	if (alphSwitchList[i].episode <= episode)
	 	{
	 /*	// UNUSED - debug?
	 	    int		value;
	 			
	 	    if (R_CheckTextureNumForName(alphSwitchList[i].name1) < 0)
	 	    {
	 		I_Error("Can't find switch texture '%s'!",
	 			alphSwitchList[i].name1);
	 		continue;
	 	    }
	 	    
	 	    value = R_TextureNumForName(alphSwitchList[i].name1);
	 */
	 	    switchlist[index++] = R.TextureNumForName(alphSwitchList[i].name1);
	 	    switchlist[index++] = R.TextureNumForName(alphSwitchList[i].name2);
	 	}
	     }
	 }


	 //
	 // Start a button counting down till it turns off.
	 //
	 void
	 P_StartButton
	 ( line_t line,
	   bwhere_e	w,
	   int		texture,
	   int		time )
	 {
	     int		i;
	     
	     // See if button is already pressed
	     for (i = 0;i < MAXBUTTONS;i++)
	     {
	 	if (buttonlist[i].btimer!=0
	 	    && buttonlist[i].line == line)
	 	{
	 	    
	 	    return;
	 	}
	     }
	     

	     
	     for (i = 0;i < MAXBUTTONS;i++)
	     {
	 	if (!buttonlist[i].btimer)
	 	{
	 	    buttonlist[i].line = line;
	 	    buttonlist[i].where = w;
	 	    buttonlist[i].btexture = texture;
	 	    buttonlist[i].btimer = time;
	 	    buttonlist[i].soundorg = (mobj_t *)&line.frontsector.soundorg;
	 	    return;
	 	}
	     }
	     
	     system.Error("P_StartButton: no button slots left!");
	 }

	 //
	 // Function that changes wall texture.
	 // Tell it if switch is ok to use again (1=yes, it's a button).
	 //
	 void
	 P_ChangeSwitchTexture
	 ( line_t	line,
	   int 		useAgain )
	 {
	     int     texTop;
	     int     texMid;
	     int     texBot;
	     int     i;
	     int     sound;
	 	
	     if (!useAgain)
	 	line.special = 0;

	     texTop = sides[line.sidenum[0]].toptexture;
	     texMid = sides[line.sidenum[0]].midtexture;
	     texBot = sides[line.sidenum[0]].bottomtexture;
	 	
	     sound = sfx_swtchn;

	     // EXIT SWITCH?
	     if (line.special == 11)                
	 	sound = sfx_swtchx;
	 	
	     for (i = 0;i < numswitches*2;i++)
	     {
	 	if (switchlist[i] == texTop)
	 	{
	 	    S_StartSound(buttonlist.soundorg,sound);
	 	    sides[line.sidenum[0]].toptexture = switchlist[i^1];

	 	    if (useAgain)
	 		P_StartButton(line,top,switchlist[i],BUTTONTIME);

	 	    return;
	 	}
	 	else
	 	{
	 	    if (switchlist[i] == texMid)
	 	    {
	 		S_StartSound(buttonlist.soundorg,sound);
	 		sides[line.sidenum[0]].midtexture = switchlist[i^1];

	 		if (useAgain)
	 		    P_StartButton(line, middle,switchlist[i],BUTTONTIME);

	 		return;
	 	    }
	 	    else
	 	    {
	 		if (switchlist[i] == texBot)
	 		{
	 		    S_StartSound(buttonlist.soundorg,sound);
	 		    sides[line.sidenum[0]].bottomtexture = switchlist[i^1];

	 		    if (useAgain)
	 			P_StartButton(line, bottom,switchlist[i],BUTTONTIME);

	 		    return;
	 		}
	 	    }
	 	}
	     }
	 }






	 //
	 // P_UseSpecialLine
	 // Called when a thing uses a special line.
	 // Only the front sides of lines are usable.
	 //
	 boolean
	 P_UseSpecialLine
	 ( mobj_t*	thing,
	   line_t*	line,
	   int		side )
	 {               

	     // Err...
	     // Use the back sides of VERY SPECIAL lines...
	     if (side)
	     {
	 	switch(line.special)
	 	{
	 	  case 124:
	 	    // Sliding door open&close
	 	    // UNUSED?
	 	    break;

	 	  default:
	 	    return false;
	 	    break;
	 	}
	     }

	     
	     // Switches that other things can activate.
	     if (!thing.player)
	     {
	 	// never open secret doors
	 	if (line.flags & ML_SECRET)
	 	    return false;
	 	
	 	switch(line.special)
	 	{
	 	  case 1: 	// MANUAL DOOR RAISE
	 	  case 32:	// MANUAL BLUE
	 	  case 33:	// MANUAL RED
	 	  case 34:	// MANUAL YELLOW
	 	    break;
	 	    
	 	  default:
	 	    return false;
	 	    break;
	 	}
	     }

	     
	     // do something  
	     switch (line.special)
	     {
	 	// MANUALS
	       case 1:		// Vertical Door
	       case 26:		// Blue Door/Locked
	       case 27:		// Yellow Door /Locked
	       case 28:		// Red Door /Locked

	       case 31:		// Manual door open
	       case 32:		// Blue locked door open
	       case 33:		// Red locked door open
	       case 34:		// Yellow locked door open

	       case 117:		// Blazing door raise
	       case 118:		// Blazing door open
	 	EV_VerticalDoor (line, thing);
	 	break;
	 	
	 	//UNUSED - Door Slide Open&Close
	 	// case 124:
	 	// EV_SlidingDoor (line, thing);
	 	// break;

	 	// SWITCHES
	       case 7:
	 	// Build Stairs
	 	if (EV_BuildStairs(line,build8))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;

	       case 9:
	 	// Change Donut
	 	if (EV_DoDonut(line))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 11:
	 	// Exit level
	 	P_ChangeSwitchTexture(line,0);
	 	G_ExitLevel ();
	 	break;
	 	
	       case 14:
	 	// Raise Floor 32 and change texture
	 	if (EV_DoPlat(line,raiseAndChange,32))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 15:
	 	// Raise Floor 24 and change texture
	 	if (EV_DoPlat(line,raiseAndChange,24))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 18:
	 	// Raise Floor to next highest floor
	 	if (EV_DoFloor(line, raiseFloorToNearest))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 20:
	 	// Raise Plat next highest floor and change texture
	 	if (EV_DoPlat(line,raiseToNearestAndChange,0))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 21:
	 	// PlatDownWaitUpStay
	 	if (EV_DoPlat(line,downWaitUpStay,0))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 23:
	 	// Lower Floor to Lowest
	 	if (EV_DoFloor(line,lowerFloorToLowest))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 29:
	 	// Raise Door
	 	if (EV_DoDoor(line,normal))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 41:
	 	// Lower Ceiling to Floor
	 	if (EV_DoCeiling(line,lowerToFloor))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 71:
	 	// Turbo Lower Floor
	 	if (EV_DoFloor(line,turboLower))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 49:
	 	// Ceiling Crush And Raise
	 	if (EV_DoCeiling(line,crushAndRaise))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 50:
	 	// Close Door
	 	if (EV_DoDoor(line,close))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 51:
	 	// Secret EXIT
	 	P_ChangeSwitchTexture(line,0);
	 	G_SecretExitLevel ();
	 	break;
	 	
	       case 55:
	 	// Raise Floor Crush
	 	if (EV_DoFloor(line,raiseFloorCrush))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 101:
	 	// Raise Floor
	 	if (EV_DoFloor(line,raiseFloor))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 102:
	 	// Lower Floor to Surrounding floor height
	 	if (EV_DoFloor(line,lowerFloor))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 103:
	 	// Open Door
	 	if (EV_DoDoor(line,open))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 111:
	 	// Blazing Door Raise (faster than TURBO!)
	 	if (EV_DoDoor (line,blazeRaise))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 112:
	 	// Blazing Door Open (faster than TURBO!)
	 	if (EV_DoDoor (line,blazeOpen))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 113:
	 	// Blazing Door Close (faster than TURBO!)
	 	if (EV_DoDoor (line,blazeClose))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 122:
	 	// Blazing PlatDownWaitUpStay
	 	if (EV_DoPlat(line,blazeDWUS,0))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 127:
	 	// Build Stairs Turbo 16
	 	if (EV_BuildStairs(line,turbo16))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 131:
	 	// Raise Floor Turbo
	 	if (EV_DoFloor(line,raiseFloorTurbo))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 133:
	 	// BlzOpenDoor BLUE
	       case 135:
	 	// BlzOpenDoor RED
	       case 137:
	 	// BlzOpenDoor YELLOW
	 	if (EV_DoLockedDoor (line,blazeOpen,thing))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	       case 140:
	 	// Raise Floor 512
	 	if (EV_DoFloor(line,raiseFloor512))
	 	    P_ChangeSwitchTexture(line,0);
	 	break;
	 	
	 	// BUTTONS
	       case 42:
	 	// Close Door
	 	if (EV_DoDoor(line,close))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 43:
	 	// Lower Ceiling to Floor
	 	if (EV_DoCeiling(line,lowerToFloor))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 45:
	 	// Lower Floor to Surrounding floor height
	 	if (EV_DoFloor(line,lowerFloor))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 60:
	 	// Lower Floor to Lowest
	 	if (EV_DoFloor(line,lowerFloorToLowest))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 61:
	 	// Open Door
	 	if (EV_DoDoor(line,open))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 62:
	 	// PlatDownWaitUpStay
	 	if (EV_DoPlat(line,downWaitUpStay,1))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 63:
	 	// Raise Door
	 	if (EV_DoDoor(line,normal))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 64:
	 	// Raise Floor to ceiling
	 	if (EV_DoFloor(line,raiseFloor))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 66:
	 	// Raise Floor 24 and change texture
	 	if (EV_DoPlat(line,raiseAndChange,24))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 67:
	 	// Raise Floor 32 and change texture
	 	if (EV_DoPlat(line,raiseAndChange,32))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 65:
	 	// Raise Floor Crush
	 	if (EV_DoFloor(line,raiseFloorCrush))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 68:
	 	// Raise Plat to next highest floor and change texture
	 	if (EV_DoPlat(line,raiseToNearestAndChange,0))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 69:
	 	// Raise Floor to next highest floor
	 	if (EV_DoFloor(line, raiseFloorToNearest))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 70:
	 	// Turbo Lower Floor
	 	if (EV_DoFloor(line,turboLower))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 114:
	 	// Blazing Door Raise (faster than TURBO!)
	 	if (EV_DoDoor (line,blazeRaise))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 115:
	 	// Blazing Door Open (faster than TURBO!)
	 	if (EV_DoDoor (line,blazeOpen))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 116:
	 	// Blazing Door Close (faster than TURBO!)
	 	if (EV_DoDoor (line,blazeClose))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 123:
	 	// Blazing PlatDownWaitUpStay
	 	if (EV_DoPlat(line,blazeDWUS,0))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 132:
	 	// Raise Floor Turbo
	 	if (EV_DoFloor(line,raiseFloorTurbo))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 99:
	 	// BlzOpenDoor BLUE
	       case 134:
	 	// BlzOpenDoor RED
	       case 136:
	 	// BlzOpenDoor YELLOW
	 	if (EV_DoLockedDoor (line,blazeOpen,thing))
	 	    P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 138:
	 	// Light Turn On
	 	EV_LightTurnOn(line,255);
	 	P_ChangeSwitchTexture(line,1);
	 	break;
	 	
	       case 139:
	 	// Light Turn Off
	 	EV_LightTurnOn(line,35);
	 	P_ChangeSwitchTexture(line,1);
	 	break;
	 			
	     }
	 	
	     return true;
	 }
 }


