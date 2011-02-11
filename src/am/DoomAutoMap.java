package am;
import doom.event_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomAutoMap.java,v 1.3 2011/02/11 00:11:13 velktron Exp $
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
//  AutoMap module.
//
//-----------------------------------------------------------------------------

public interface DoomAutoMap {

// Used by ST StatusBar stuff.
public final int AM_MSGHEADER =(('a'<<24)+('m'<<16));
public final int AM_MSGENTERED= (AM_MSGHEADER | ('e'<<8));
public final int AM_MSGEXITED= (AM_MSGHEADER | ('x'<<8));


// Called by main loop.
public boolean AM_Responder (event_t ev);

// Called by main loop.
public void AM_Ticker ();

// Called by main loop,
// called instead of view drawer if automap active.
public void  AM_Drawer ();

// Called to force the automap to quit
// if the level is completed while it is up.
public void  AM_Stop ();


}