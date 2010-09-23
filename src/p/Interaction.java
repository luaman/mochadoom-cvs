package p;

import static data.Defines.*;
import static data.Limits.*;
import static p.MapUtils.flags;
import static doom.items.*;
import static p.mobj.*;
import static m.fixed_t.*;
import static data.Tables.*;
import static doom.englsh.*;
import static data.info.*;
import m.random;
import automap.DoomAutoMap;
import rr.Renderer;
import rr.RendererState;
import st.DoomStatusBarInterface;
import v.DoomVideoRenderer;
import w.WadLoader;
import i.system;
import data.mobjtype_t;
import data.sounds.sfxenum_t;
import doom.DoomStatus;
import doom.player_t;
import doom.weapontype_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Interaction.java,v 1.6 2010/09/23 07:31:11 velktron Exp $
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
// $Log: Interaction.java,v $
// Revision 1.6  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.5  2010/09/16 00:16:27  velktron
// Velvet FM 96.8
//
// Revision 1.4  2010/09/13 23:09:51  velktron
// More "unified game state" work
//
// Revision 1.3  2010/09/13 15:39:17  velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.2  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.1  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
//
//
// DESCRIPTION:
//  Handling interactions (i.e., collisions).
//
//-----------------------------------------------------------------------------


public class Interaction {


  public static final String rcsid = "$Id: Interaction.java,v 1.6 2010/09/23 07:31:11 velktron Exp $";

/////////////////// STATUS ///////////////////
  
  WadLoader W;
  DoomStatus DS;
  DoomAutoMap AM;
  random RND;
  RendererState R;
  
/*
  // Data.
  #include "doomdef.h"
  #include "dstrings.h"
  #include "sounds.h"

  #include "doomstat.h"

  #include "m_random.h"
  #include "i_system.h"

  #include "am_map.h"

  #include "p_local.h"

  #include "s_sound.h"

  #ifdef __GNUG__
  #pragma implementation "p_inter.h"
  #endif
  #include "p_inter.h" */

  


  




 


}
