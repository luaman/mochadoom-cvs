package doom;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomNet.java,v 1.6 2010/11/17 23:55:06 velktron Exp $
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
// $Log: DoomNet.java,v $
// Revision 1.6  2010/11/17 23:55:06  velktron
// Kind of playable/controllable.
//
// Revision 1.5  2010/10/25 15:57:19  velktron
// Work on netcode...needs moar though.
//
// Revision 1.4  2010/10/22 16:22:44  velktron
// Renderer works stably enough but a ton of bleeding. Started working on netcode.
//
// Revision 1.3  2010/09/27 02:27:29  velktron
// BEASTLY update
//
// Revision 1.2  2010/09/25 17:37:13  velktron
// Lots of changes.
//
// The most important is the creation of the AWTDoom frame handling I/O.
//
// Revision 1.1  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.3  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.2  2010/09/01 15:53:42  velktron
// Graphics data loader implemented....still need to figure out how column caching works, though.
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
//	DOOM Network game communication and protocol,
//	all OS independend parts.
//
//-----------------------------------------------------------------------------


//static const char rcsid[] = "$Id: DoomNet.java,v 1.6 2010/11/17 23:55:06 velktron Exp $";


//#include "m_menu.h"
//#include "i_system.h"
//#include "i_video.h"
//#include "i_net.h"
//#include "g_game.h"
import static data.Defines.*;
import static data.Limits.*;
import static doom.NetConsts.*;
import static utils.C2JUtils.*;

import i.DoomSystemInterface;

import java.util.Arrays;

import n.DoomNetworkInterface;
//
//Network play related stuff.
//There is a data struct that stores network
//communication related stuff, and another
//one that defines the actual packets to
//be transmitted.
//
import w.DoomFile;


