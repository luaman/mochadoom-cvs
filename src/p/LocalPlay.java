package p;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: LocalPlay.java,v 1.2 2010/09/12 22:38:37 velktron Exp $
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


import static data.Defines.*;
import static data.Limits.*;	
import rr.line_t;
import data.mapthing_t;
import doom.thinker_t;
	
public abstract class LocalPlay{

//
// P_TICK
//

// both the head and tail of the thinker list
public thinker_t	thinkercap;	
public mapthing_t[]	itemrespawnque=new mapthing_t[ITEMQUESIZE];
public int[]		itemrespawntime=new int [ITEMQUESIZE];
public int		iquehead;
public int		iquetail;


// Actually in maputl.
public intercept_t[]	intercepts=new intercept_t[MAXINTERCEPTS];
public int	intercept_p;

// typedef boolean (*traverser_t) (intercept_t *in);

// Actually in maputil
public int	opentop;
public int		openbottom;
public int		openrange;
public int	lowfloor;
divline_t	trace;

//
// P_MAP
//

// If "floatok" true, move would be ok
// if within "tmfloorz - tmceilingz".
public boolean		floatok;
/** fixed_t */
public int		tmfloorz,tmceilingz;
public line_t		ceilingline;

public mobj_t	linetarget;	// who got hit (or NULL)


//
// P_SETUP
//
public char[]		rejectmatrix;	// for fast sight rejection
public short[]		blockmaplump;	// offsets in blockmap are from here
public short[]		blockmap;
public int		bmapwidth;
public int		bmapheight;	// in mapblocks
/** origin of block map (fixed_t) */
public int		bmaporgx, bmaporgy;	// 
public mobj_t[]		blocklinks;	// for thing chains



//
// P_INTER
//
public int[]		maxammo=new int[NUMAMMO];
public int[]		clipammo=new int[NUMAMMO];
}