package p;

import static data.Defines.*;
import static data.Limits.*;
import static p.mobj.*;
import static m.BBox.*;
import m.random;
import automap.DoomAutoMap;
import data.doomstat;

import rr.Renderer;
import rr.UnifiedRenderer;
import rr.line_t;
import rr.subsector_t;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: MapMovement.java,v 1.3 2010/09/13 15:39:17 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// $Log: MapMovement.java,v $
// Revision 1.3  2010/09/13 15:39:17  velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.2  2010/09/12 22:38:37  velktron
// Some work
//
// Revision 1.1  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
//
// Revision 1.1  2010/08/19 23:14:49  velktron
// Automap
//
//
// DESCRIPTION:
//	Movement, collision handling.
//	Shooting and aiming.
//
//-----------------------------------------------------------------------------
import w.WadLoader;
import static p.MapUtils.flags;

public class MapMovement{

public static final String rcsid = "$Id: MapMovement.java,v 1.3 2010/09/13 15:39:17 velktron Exp $";


/////////////////// STATUS ///////////////////

WadLoader W;
doomstat DS;
DoomAutoMap AM;
random RND;
UnifiedRenderer R;
LevelLoader LL;

//////////////////////////////////////////////

/** fixed_t */
int[]		tmbbox=new int[4];
mobj_t		tmthing;
int		tmflags;
/** fixed_t */
int		tmx,	tmy;

/** If "floatok" true, move would be ok
    if within "tmfloorz - tmceilingz". */
boolean		floatok;

/** fixed_t */
int		tmfloorz, tmceilingz,	tmdropoffz;

// keep track of the line that lowers the ceiling,
// so missiles don't explode against sky hack walls
line_t		ceilingline;



line_t[]		spechit=new line_t[MAXSPECIALCROSS];
int		numspechit;



//
// TELEPORT MOVE
// 



//
// P_TeleportMove
//
private boolean
TeleportMove
( mobj_t	thing,
  int	x, //fixed
  int	y )
{
    int			xl;
    int			xh;
    int			yl;
    int			yh;
    int			bx;
    int			by;
    
    subsector_t	newsubsec;
    
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

interface PIT_LineFunction {
    public boolean invoke(line_t ld);
}


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
	return false;		// one sided line
		
    if (!flags(tmthing.flags, MF_MISSILE) )
    {
	if ( flags(ld.flags, ML_BLOCKING) )
	    return false;	// explicitly blocking everything

	if ( (tmthing.player==null) && flags(ld.flags, ML_BLOCKMONSTERS ))
	    return false;	// block monsters only
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



interface PIT_MobjFunction {
    public boolean invoke(mobj_t thing);
}


/**PIT_CheckThing  */

class PIT_CheckThing implements PIT_MobjFunction {
    @Override
    public boolean invoke(mobj_t thing) {
    int		blockdist; // fixed_t
    boolean		solid;
    int			damage;
		
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
	
	return false;		// stop moving
    }

    
    // missiles can hit other things
    if (tmthing.flags & MF_MISSILE)
    {
	// see if it went over / under
	if (tmthing.z > thing.z + thing.height)
	    return true;		// overhead
	if (tmthing.z+tmthing.height < thing.z)
	    return true;		// underneath
		
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
( mobj_t	thing,
  int	x,
  int	y )
{
    int			xl;
    int			xh;
    int			yl;
    int			yh;
    int			bx;
    int			by;
    subsector_t	newsubsec;

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
			
    validcount++;
    numspechit = 0;

    if ( tmflags & MF_NOCLIP )
	return true;
    
    // Check things first, possibly picking things up.
    // The bounding box is extended by MAXRADIUS
    // because mobj_ts are grouped into mapblocks
    // based on their origin point, and can overlap
    // into adjacent blocks by up to MAXRADIUS units.
    xl = (tmbbox[BOXLEFT] - bmaporgx - MAXRADIUS)>>MAPBLOCKSHIFT;
    xh = (tmbbox[BOXRIGHT] - bmaporgx + MAXRADIUS)>>MAPBLOCKSHIFT;
    yl = (tmbbox[BOXBOTTOM] - bmaporgy - MAXRADIUS)>>MAPBLOCKSHIFT;
    yh = (tmbbox[BOXTOP] - bmaporgy + MAXRADIUS)>>MAPBLOCKSHIFT;

    for (bx=xl ; bx<=xh ; bx++)
	for (by=yl ; by<=yh ; by++)
	    if (!P_BlockThingsIterator(bx,by,PIT_CheckThing))
		return false;
    
    // check lines
    xl = (tmbbox[BOXLEFT] - bmaporgx)>>MAPBLOCKSHIFT;
    xh = (tmbbox[BOXRIGHT] - bmaporgx)>>MAPBLOCKSHIFT;
    yl = (tmbbox[BOXBOTTOM] - bmaporgy)>>MAPBLOCKSHIFT;
    yh = (tmbbox[BOXTOP] - bmaporgy)>>MAPBLOCKSHIFT;

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
( mobj_t	thing,
  int	x,
  int	y )
{
    int	oldx, oldy; // fixed_t    
    int		side;
    int		oldside;
    line_t	ld;

    floatok = false;
    if (!CheckPosition (thing, x, y))
	return false;		// solid wall or thing
    
    if ( !(thing.flags & MF_NOCLIP) )
    {
	if (tmceilingz - tmfloorz < thing.height)
	    return false;	// doesn't fit

	floatok = true;
	
	if ( !(thing.flags&MF_TELEPORT) 
	     &&tmceilingz - thing.z < thing.height)
	    return false;	// mobj must lower itself to fit

	if ( !(thing.flags&MF_TELEPORT)
	     && tmfloorz - thing.z > 24*FRACUNIT )
	    return false;	// too big a step up

	if ( !(thing.flags&(MF_DROPOFF|MF_FLOAT))
	     && tmfloorz - tmdropoffz > 24*FRACUNIT )
	    return false;	// don't stand over a dropoff
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
    if (! (thing.flags&(MF_TELEPORT|MF_NOCLIP)) )
    {
	while (numspechit--)
	{
	    // see if the line was crossed
	    ld = spechit[numspechit];
	    side = P_PointOnLineSide (thing.x, thing.y, ld);
	    oldside = P_PointOnLineSide (oldx, oldy, ld);
	    if (side != oldside)
	    {
		if (ld.special)
		    P_CrossSpecialLine (ld-lines, oldside, thing);
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
boolean P_ThingHeightClip (mobj_t* thing)
{
    boolean		onfloor;
	
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
fixed_t		bestslidefrac;
fixed_t		secondslidefrac;

line_t*		bestslideline;
line_t*		secondslideline;

mobj_t*		slidemo;

fixed_t		tmxmove;
fixed_t		tmymove;



//
// P_HitSlideLine
// Adjusts the xmove / ymove
// so that the next move will slide along the wall.
//
void P_HitSlideLine (line_t* ld)
{
    int			side;

    angle_t		lineangle;
    angle_t		moveangle;
    angle_t		deltaangle;
    
    fixed_t		movelen;
    fixed_t		newlen;
	
	
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
    //	I_Error ("SlideLine: ang>ANG180");

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
boolean PTR_SlideTraverse (intercept_t* in)
{
    line_t*	li;
	
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
	goto isblocking;		// doesn't fit
		
    if (opentop - slidemo.z < slidemo.height)
	goto isblocking;		// mobj is too high

    if (openbottom - slidemo.z > 24*FRACUNIT )
	goto isblocking;		// too big a step up

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
	
    return false;	// stop
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
void P_SlideMove (mobj_t* mo)
{
    fixed_t		leadx;
    fixed_t		leady;
    fixed_t		trailx;
    fixed_t		traily;
    fixed_t		newx;
    fixed_t		newy;
    int			hitcount;
		
    slidemo = mo;
    hitcount = 0;
    
  retry:
    if (++hitcount == 3)
	goto stairstep;		// don't loop forever

    
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

    P_HitSlideLine (bestslideline);	// clip the moves

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
mobj_t*		linetarget;	// who got hit (or NULL)
mobj_t*		shootthing;

// Height if not aiming up or down
// ???: use slope for monsters?
fixed_t		shootz;	

int		la_damage;
fixed_t		attackrange;

fixed_t		aimslope;

// slopes to top and bottom of target
extern fixed_t	topslope;
extern fixed_t	bottomslope;	


//
// PTR_AimTraverse
// Sets linetaget and aimslope when a target is aimed at.
//
boolean
PTR_AimTraverse (intercept_t* in)
{
    line_t*		li;
    mobj_t*		th;
    fixed_t		slope;
    fixed_t		thingtopslope;
    fixed_t		thingbottomslope;
    fixed_t		dist;
		
    if (in.isaline)
    {
	li = in.d.line;
	
	if ( !(li.flags & ML_TWOSIDED) )
	    return false;		// stop
	
	// Crosses a two sided line.
	// A two sided line will restrict
	// the possible target ranges.
	P_LineOpening (li);
	
	if (openbottom >= opentop)
	    return false;		// stop
	
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
	    return false;		// stop
			
	return true;			// shot continues
    }
    
    // shoot a thing
    th = in.d.thing;
    if (th == shootthing)
	return true;			// can't shoot self
    
    if (!(th.flags&MF_SHOOTABLE))
	return true;			// corpse or something

    // check angles to see if the thing can be aimed at
    dist = FixedMul (attackrange, in.frac);
    thingtopslope = FixedDiv (th.z+th.height - shootz , dist);

    if (thingtopslope < bottomslope)
	return true;			// shot over the thing

    thingbottomslope = FixedDiv (th.z - shootz, dist);

    if (thingbottomslope > topslope)
	return true;			// shot under the thing
    
    // this thing can be hit!
    if (thingtopslope > topslope)
	thingtopslope = topslope;
    
    if (thingbottomslope < bottomslope)
	thingbottomslope = bottomslope;

    aimslope = (thingtopslope+thingbottomslope)/2;
    linetarget = th;

    return false;			// don't go any farther
}


//
// PTR_ShootTraverse
//
boolean PTR_ShootTraverse (intercept_t* in)
{
    fixed_t		x;
    fixed_t		y;
    fixed_t		z;
    fixed_t		frac;
    
    line_t*		li;
    
    mobj_t*		th;

    fixed_t		slope;
    fixed_t		dist;
    fixed_t		thingtopslope;
    fixed_t		thingbottomslope;
		
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
	    if	(li.backsector && li.backsector.ceilingpic == skyflatnum)
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
	return true;		// can't shoot self
    
    if (!(th.flags&MF_SHOOTABLE))
	return true;		// corpse or something
		
    // check angles to see if the thing can be aimed at
    dist = FixedMul (attackrange, in.frac);
    thingtopslope = FixedDiv (th.z+th.height - shootz , dist);

    if (thingtopslope < aimslope)
	return true;		// shot over the thing

    thingbottomslope = FixedDiv (th.z - shootz, dist);

    if (thingbottomslope > aimslope)
	return true;		// shot under the thing

    
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
// P_AimLineAttack
//
fixed_t
P_AimLineAttack
( mobj_t*	t1,
  angle_t	angle,
  fixed_t	distance )
{
    fixed_t	x2;
    fixed_t	y2;
	
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
 

//
// P_LineAttack
// If damage == 0, it is just a test trace
// that will leave linetarget set.
//
void
P_LineAttack
( mobj_t*	t1,
  angle_t	angle,
  fixed_t	distance,
  fixed_t	slope,
  int		damage )
{
    fixed_t	x2;
    fixed_t	y2;
	
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
mobj_t*		usething;

boolean	PTR_UseTraverse (intercept_t* in)
{
    int		side;
	
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
    
    //	return false;		// don't use back side
	
    P_UseSpecialLine (usething, in.d.line, side);

    // can't use for than one special line in a row
    return false;
}


//
// P_UseLines
// Looks for special lines in front of the player to activate.
//
void P_UseLines (player_t*	player) 
{
    int		angle;
    fixed_t	x1;
    fixed_t	y1;
    fixed_t	x2;
    fixed_t	y2;
	
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
mobj_t*		bombsource;
mobj_t*		bombspot;
int		bombdamage;


//
// PIT_RadiusAttack
// "bombsource" is the creature
// that caused the explosion at "bombspot".
//
boolean PIT_RadiusAttack (mobj_t* thing)
{
    fixed_t	dx;
    fixed_t	dy;
    fixed_t	dist;
	
    if (!(thing.flags & MF_SHOOTABLE) )
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
	return true;	// out of range

    if ( P_CheckSight (thing, bombspot) )
    {
	// must be in direct path
	P_DamageMobj (thing, bombspot, bombsource, bombdamage - dist);
    }
    
    return true;
}


//
// P_RadiusAttack
// Source is the creature that caused the explosion at spot.
//
void
P_RadiusAttack
( mobj_t*	spot,
  mobj_t*	source,
  int		damage )
{
    int		x;
    int		y;
    
    int		xl;
    int		xh;
    int		yl;
    int		yh;
    
    fixed_t	dist;
	
    dist = (damage+MAXRADIUS)<<FRACBITS;
    yh = (spot.y + dist - bmaporgy)>>MAPBLOCKSHIFT;
    yl = (spot.y - dist - bmaporgy)>>MAPBLOCKSHIFT;
    xh = (spot.x + dist - bmaporgx)>>MAPBLOCKSHIFT;
    xl = (spot.x - dist - bmaporgx)>>MAPBLOCKSHIFT;
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
boolean		crushchange;
boolean		nofit;


//
// PIT_ChangeSector
//
boolean PIT_ChangeSector (mobj_t*	thing)
{
    mobj_t*	mo;
	
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

    if (! (thing.flags & MF_SHOOTABLE) )
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
( sector_t*	sector,
  boolean	crunch )
{
    int		x;
    int		y;
	
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
protected int opentop,  openbottom, openrange; lowfloor;


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

 if ( ! (thing.flags & MF_NOSECTOR) )
 {
 // inert things don't need to be in blockmap?
 // unlink from subsector
 if (thing.snext)
     thing.snext.sprev = thing.sprev;

 if (thing.sprev)
     thing.sprev.snext = thing.snext;
 else
     thing.subsector.sector.thinglist = thing.snext;
 }
 
 if ( ! (thing.flags & MF_NOBLOCKMAP) )
 {
 // inert things don't need to be in blockmap
 // unlink from block map
 if (thing.bnext)
     thing.bnext.bprev = thing.bprev;
 
 if (thing.bprev)
     thing.bprev.bnext = thing.bnext;
 else
 {
     blockx = (thing.x - bmaporgx)>>MAPBLOCKSHIFT;
     blocky = (thing.y - bmaporgy)>>MAPBLOCKSHIFT;

     if (blockx>=0 && blockx < bmapwidth
     && blocky>=0 && blocky <bmapheight)
     {
     blocklinks[blocky*bmapwidth+blockx] = thing.bnext;
     }
 }
 }
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
 mobj_t[]        link;

 
 // link into subsector
 ss = PointInSubsector (thing.x,thing.y);
 thing.subsector = ss;
 
 if ( ! (thing.flags & MF_NOSECTOR) )
 {
 // invisible things don't go into the sector links
 sec = ss.sector;
 
 thing.sprev = NULL;
 thing.snext = sec.thinglist;

 if (sec.thinglist)
     sec.thinglist.sprev = thing;

 sec.thinglist = thing;
 }

 
 // link into blockmap
 if ( ! (thing.flags & MF_NOBLOCKMAP) )
 {
 // inert things don't need to be in blockmap        
 blockx = (thing.x - bmaporgx)>>MAPBLOCKSHIFT;
 blocky = (thing.y - bmaporgy)>>MAPBLOCKSHIFT;

 if (blockx>=0
     && blockx < bmapwidth
     && blocky>=0
     && blocky < bmapheight)
 {
     link = &blocklinks[blocky*bmapwidth+blockx];
     thing.bprev = NULL;
     thing.bnext = *link;
     if (*link)
     (*link).bprev = thing;

     *link = thing;
 }
 else
 {
     // thing is off the map
     thing.bnext = thing.bprev = NULL;
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
 
 offset = *(blockmap+offset);

 for ( list = blockmaplump+offset ; *list != -1 ; list++)
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
boolean(*func)(mobj_t*) )
{
 mobj_t*     mobj;
 
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
boolean PIT_AddThingIntercepts (mobj_t* thing)
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
( fixed_t       x1,
fixed_t       y1,
fixed_t       x2,
fixed_t       y2,
int           flags,
boolean (*trav) (intercept_t *))
{
 fixed_t xt1;
 fixed_t yt1;
 fixed_t xt2;
 fixed_t yt2;
 
 fixed_t xstep;
 fixed_t ystep;
 
 fixed_t partial;
 
 fixed_t xintercept;
 fixed_t yintercept;
 
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