package p;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: local.java,v 1.2 2010/07/03 23:24:13 velktron Exp $
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
// DESCRIPTION:
//	Play functions, animation, global header.
//
//-----------------------------------------------------------------------------


import static m.fixed_t.*;	
import static data.doomtype.*;
import doom.thinker_t;
	
public class local{

public static int FLOATSPEED		=(FRACUNIT*4);

public static int MAXHEALTH		=100;

public static int VIEWHEIGHT	=	(41*FRACUNIT);

// mapblocks are used to check movement
// against lines and things
public static int MAPBLOCKUNITS=	128;
public static int MAPBLOCKSIZE	=(MAPBLOCKUNITS*FRACUNIT);
public static int MAPBLOCKSHIFT	=(FRACBITS+7);
public static int MAPBMASK		=(MAPBLOCKSIZE-1);
public static int MAPBTOFRAC=		(MAPBLOCKSHIFT-FRACBITS);


// player radius for movement checking
public static int PLAYERRADIUS	=16*FRACUNIT;

// MAXRADIUS is for precalculated sector block boxes
// the spider demon is larger,
// but we do not have any moving sectors nearby
public static int MAXRADIUS	=	32*FRACUNIT;

public static int GRAVITY	=	FRACUNIT;
public static int MAXMOVE	=	(30*FRACUNIT);

public static int USERANGE		=(64*FRACUNIT);
public static int MELEERANGE	=	(64*FRACUNIT);
public static int MISSILERANGE=(32*64*FRACUNIT);

// follow a player exlusively for 3 seconds
public static int	BASETHRESHOLD=	 	100;

//
// P_TICK
//

// both the head and tail of the thinker list
public thinker_t	thinkercap;	


void P_InitThinkers (void);
void P_AddThinker (thinker_t* thinker);
void P_RemoveThinker (thinker_t* thinker);


//
// P_PSPR
//
void P_SetupPsprites (player_t* curplayer);
void P_MovePsprites (player_t* curplayer);
void P_DropWeapon (player_t* player);


//
// P_USER
//
void	P_PlayerThink (player_t* player);


//
// P_MOBJ
//
public static int ONFLOORZ	=	MININT;
public static int ONCEILINGZ	=	MAXINT;

// Time interval for item respawning.
#define ITEMQUESIZE		128

extern mapthing_t	itemrespawnque[ITEMQUESIZE];
extern int		itemrespawntime[ITEMQUESIZE];
extern int		iquehead;
extern int		iquetail;


void P_RespawnSpecials (void);

mobj_t*
P_SpawnMobj
( fixed_t	x,
  fixed_t	y,
  fixed_t	z,
  mobjtype_t	type );

void 	P_RemoveMobj (mobj_t* th);
boolean	P_SetMobjState (mobj_t* mobj, statenum_t state);
void 	P_MobjThinker (mobj_t* mobj);

void	P_SpawnPuff (fixed_t x, fixed_t y, fixed_t z);
void 	P_SpawnBlood (fixed_t x, fixed_t y, fixed_t z, int damage);
mobj_t* P_SpawnMissile (mobj_t* source, mobj_t* dest, mobjtype_t type);
void	P_SpawnPlayerMissile (mobj_t* source, mobjtype_t type);


//
// P_ENEMY
//
void P_NoiseAlert (mobj_t* target, mobj_t* emmiter);


//
// P_MAPUTL
//
typedef struct
{
    fixed_t	x;
    fixed_t	y;
    fixed_t	dx;
    fixed_t	dy;
    
} divline_t;

typedef struct
{
    fixed_t	frac;		// along trace line
    boolean	isaline;
    union {
	mobj_t*	thing;
	line_t*	line;
    }			d;
} intercept_t;

#define MAXINTERCEPTS	128

extern intercept_t	intercepts[MAXINTERCEPTS];
extern intercept_t*	intercept_p;

typedef boolean (*traverser_t) (intercept_t *in);

fixed_t P_AproxDistance (fixed_t dx, fixed_t dy);
int 	P_PointOnLineSide (fixed_t x, fixed_t y, line_t* line);
int 	P_PointOnDivlineSide (fixed_t x, fixed_t y, divline_t* line);
void 	P_MakeDivline (line_t* li, divline_t* dl);
fixed_t P_InterceptVector (divline_t* v2, divline_t* v1);
int 	P_BoxOnLineSide (fixed_t* tmbox, line_t* ld);

extern fixed_t		opentop;
extern fixed_t 		openbottom;
extern fixed_t		openrange;
extern fixed_t		lowfloor;

void 	P_LineOpening (line_t* linedef);

boolean P_BlockLinesIterator (int x, int y, boolean(*func)(line_t*) );
boolean P_BlockThingsIterator (int x, int y, boolean(*func)(mobj_t*) );

#define PT_ADDLINES		1
#define PT_ADDTHINGS	2
#define PT_EARLYOUT		4

extern divline_t	trace;

boolean
P_PathTraverse
( fixed_t	x1,
  fixed_t	y1,
  fixed_t	x2,
  fixed_t	y2,
  int		flags,
  boolean	(*trav) (intercept_t *));

void P_UnsetThingPosition (mobj_t* thing);
void P_SetThingPosition (mobj_t* thing);


//
// P_MAP
//

// If "floatok" true, move would be ok
// if within "tmfloorz - tmceilingz".
extern boolean		floatok;
extern fixed_t		tmfloorz;
extern fixed_t		tmceilingz;


extern	line_t*		ceilingline;

boolean P_CheckPosition (mobj_t *thing, fixed_t x, fixed_t y);
boolean P_TryMove (mobj_t* thing, fixed_t x, fixed_t y);
boolean P_TeleportMove (mobj_t* thing, fixed_t x, fixed_t y);
void	P_SlideMove (mobj_t* mo);
boolean P_CheckSight (mobj_t* t1, mobj_t* t2);
void 	P_UseLines (player_t* player);

boolean P_ChangeSector (sector_t* sector, boolean crunch);

extern mobj_t*	linetarget;	// who got hit (or NULL)





//
// P_SETUP
//
extern byte*		rejectmatrix;	// for fast sight rejection
extern short*		blockmaplump;	// offsets in blockmap are from here
extern short*		blockmap;
extern int		bmapwidth;
extern int		bmapheight;	// in mapblocks
extern fixed_t		bmaporgx;
extern fixed_t		bmaporgy;	// origin of block map
extern mobj_t**		blocklinks;	// for thing chains



//
// P_INTER
//
extern int		maxammo[NUMAMMO];
extern int		clipammo[NUMAMMO];



//
// P_SPEC
//
//#include "p_spec.h"


//-----------------------------------------------------------------------------
//
// $Log: local.java,v $
// Revision 1.2  2010/07/03 23:24:13  velktron
// Added a LOT of stuff, like Status bar code & objects. Now we're cooking with gas!
//
// Revision 1.1  2010/06/30 08:58:50  velktron
// Let's see if this stuff will finally commit....
//
//
// Most stuff is still  being worked on. For a good place to start and get an idea of what is being done, I suggest checking out the "testers" package.
//
// Revision 1.1  2010/06/29 11:07:34  velktron
// Release often, release early they say...
//
// Commiting ALL stuff done so far. A lot of stuff is still broken/incomplete, and there's still mixed C code in there. I suggest you load everything up in Eclpise and see what gives from there.
//
// A good place to start is the testers/ directory, where you  can get an idea of how a few of the implemented stuff works.
//
//
//-----------------------------------------------------------------------------

