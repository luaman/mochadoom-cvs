package p;

import data.doomstat;
import doom.ActionType;
import doom.thinker_t;
import static data.Defines.*;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Tick.java,v 1.2 2010/08/25 00:50:59 velktron Exp $
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
// $Log: Tick.java,v $
// Revision 1.2  2010/08/25 00:50:59  velktron
// Some more work...
//
// Revision 1.1  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
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
// DESCRIPTION:
//	Archiving: SaveGame I/O.
//	Thinker, Ticker.
//
//-----------------------------------------------------------------------------

public class Tick{

public static final String rcsid = "$Id: Tick.java,v 1.2 2010/08/25 00:50:59 velktron Exp $";

doomstat DS;
spec SPEC;
int	leveltime;

//
// THINKERS
// All thinkers should be allocated by Z_Malloc
// so they can be operated on uniformly.
// The actual structures will vary in size,
// but the first element must be thinker_t.
//

/** Both the head and the tail of the thinkers list */
public thinker_t    thinkercap; 

//
// P_InitThinkers
//
public void InitThinkers ()
{
    thinkercap.setNext(thinkercap);
    thinkercap.setPrev(thinkercap);
}




//
// P_AddThinker
// Adds a new thinker at the end of the list.
//
public void AddThinker (thinker_t thinker)
{
    thinkercap.getPrev().setNext(thinker);
    thinker.setNext(thinkercap);
    thinker.setPrev(thinkercap.getPrev());
    thinkercap.setPrev(thinker);
}



//
// P_RemoveThinker
// Deallocation is lazy -- it will not actually be freed
// until its thinking turn comes up.
//
public void RemoveThinker (thinker_t thinker)
{
  // FIXME: NOP.
  thinker.setFunction(null);
}



//
// P_AllocateThinker
// Allocates memory and adds a new thinker at the end of the list.
//
public void AllocateThinker (thinker_t	thinker)
{
}



//
// P_RunThinkers
//
public void RunThinkers ()
{
    thinker_t	currentthinker;

    currentthinker = thinkercap.getNext();
    while (currentthinker != thinkercap)
    {
	if ( currentthinker.getFunction() == null )
	{
	    // time to remove it
	    currentthinker.getNext().setPrev( currentthinker.getPrev());
	    currentthinker.getNext().setNext(currentthinker.getNext());
	}
	else
	{
	    if (currentthinker.getFunction().getType()==ActionType.acp1)
		currentthinker.getFunction().acp1(currentthinker);
	}
	currentthinker = currentthinker.getNext();
    }
}



//
// P_Ticker
//

public void Ticker ()
{
    int		i;
    
    // run the tic
    if (DS.paused)
	return;
		
    // pause if in menu and at least one tic has been run
    if ( !DS.netgame
	 && DS.menuactive
	 && !DS.demoplayback
	 && DS.players[DS.consoleplayer].viewz != 1)
    {
	return;
    }
    
		
    for (i=0 ; i<MAXPLAYERS ; i++)
	if (DS.playeringame[i])
	    PlayerThink (DS.players[i]);
			
    RunThinkers ();
    SPEC.UpdateSpecials (); // In specials. Merge?
    SPEC.RespawnSpecials ();

    // for par times
    leveltime++;	
}
