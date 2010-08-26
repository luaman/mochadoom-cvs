package st;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomStatusBarInterface.java,v 1.3 2010/08/26 16:43:42 velktron Exp $
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

import static data.Defines.*;
import doom.event_t;

public interface DoomStatusBarInterface {

//
// STATUS BAR
//

// Called by main loop.
public boolean Responder (event_t ev);

// Called by main loop.
public void Ticker ();

// Called by main loop.
public void Drawer (boolean fullscreen, boolean refresh);

// Called when the console player is spawned on each level.
public void Start ();

// Called by startup code.
public void Init ();

public void Stop();


}