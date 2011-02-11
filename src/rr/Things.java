package rr;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Things.java,v 1.3 2011/02/11 00:11:13 velktron Exp $
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
// $Log: Things.java,v $
// Revision 1.3  2011/02/11 00:11:13  velktron
// A MUCH needed update to v1.3.
//
// Revision 1.1  2010/07/14 16:12:20  velktron
// A new direction has been taken for the Renderer: instead of making a single, omnipotent "Renderer" object, the functionality will remain split into at least Renderer, Things, Planes and Draw, with a new DoomRendererContext object keeping everything glued together.
//
// Revision 1.2  2010/07/02 14:26:16  velktron
// Now basic video rendering code should be fine (in SimpleRenderer)
// Defined DoomVideoSystem interface for SimpleRenderer.
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
//	Refresh of things, i.e. objects represented by sprites.
//
//-----------------------------------------------------------------------------

import static data.Defines.*;
import i.system;
import m.fixed_t;
import m.Swap.*;
import static m.fixed_t.*;
//#include "i_system.h"
//#include "z_zone.h"
//#include "w_wad.h"

//#include "r_local.h"

import data.doomstat.*;


public class Things{
    

}