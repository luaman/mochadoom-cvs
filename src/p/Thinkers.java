package p;

import data.doomstat;
import doom.ActionType;
import doom.thinker_t;
import static data.Defines.*;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Thinkers.java,v 1.1 2010/09/14 15:34:01 velktron Exp $
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
// $Log: Thinkers.java,v $
// Revision 1.1  2010/09/14 15:34:01  velktron
// The enormity of this commit is incredible (pun intended)
//
// Revision 1.3  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
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

import static data.Limits.*;

public class Thinkers{

public static final String rcsid = "$Id: Thinkers.java,v 1.1 2010/09/14 15:34:01 velktron Exp $";

doomstat DS;
spec SPEC;
int	leveltime;
UnifiedGameMap GAME;

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
    thinkercap.next=thinkercap;
    thinkercap.prev=thinkercap;
}




//
// P_AddThinker
// Adds a new thinker at the end of the list.
//
public void AddThinker (thinker_t thinker)
{
    thinkercap.prev.next=thinker;
    thinker.next=thinkercap;
    thinker.prev=thinkercap.prev;
    thinkercap.prev=thinker;
}



//
// P_RemoveThinker
// Deallocation is lazy -- it will not actually be freed
// until its thinking turn comes up.
//
public void RemoveThinker (thinker_t thinker)
{
  // FIXME: NOP.
  thinker.function=null;
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
//
//P_RunThinkers
//
public void RunThinkers ()
{
 thinker_t   currentthinker;

 currentthinker = thinkercap.next;
 while (currentthinker != thinkercap)
 {
 if ( currentthinker.function == null )
 {
     // time to remove it
     currentthinker.next.prev=currentthinker.prev;
     currentthinker.prev.next=currentthinker.next;
 }
 else
 {
     if (currentthinker.function.getType()==ActionType.acp1)
     currentthinker.function.acp1(currentthinker);
 }
 currentthinker = currentthinker.next;
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
	    GAME.PlayerThink (DS.players[i]);
			
    RunThinkers ();
    GAME.UpdateSpecials (); // In specials. Merge?
    GAME.RespawnSpecials ();

    // for par times
    leveltime++;	
}
