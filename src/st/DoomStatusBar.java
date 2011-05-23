package st;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomStatusBar.java,v 1.4 2011/05/23 16:59:02 velktron Exp $
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
//	Status bar code.
//	Does the face/direction indicator animatin.
//	Does palette indicators as well (red pain/berserk, bright pickup)
//
//-----------------------------------------------------------------------------

import doom.event_t;

public interface DoomStatusBar {

/** Points to "screen 4" which is treated as a buffer */
static final int BG =4;

/** Points to "screen 0" which is what you actually see */
 static final int FG =0;

//
// STATUS BAR
//

/** Called by main loop. */
public boolean ST_Responder (event_t ev);

/** Called by main loop. */
public void ST_Ticker ();

/** Called by main loop.*/
public void ST_Drawer (boolean fullscreen, boolean refresh);

/** Called when the console player is spawned on each level. */
public void ST_Start ();

/** Called by startup code. */
public void ST_Init ();

}