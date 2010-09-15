package p;

import i.system;
import m.random;
import data.doomstat;
import doom.player_t;
import static data.info.*;
import static data.Defines.*;
import static data.Limits.*;
import static m.fixed_t.*;
import static p.mobj_t.*;
import rr.RendererState;
import rr.line_t;
import rr.sector_t;
import rr.side_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Enemies.java,v 1.4 2010/09/15 16:17:38 velktron Exp $
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
// $Log: Enemies.java,v $
// Revision 1.4  2010/09/15 16:17:38  velktron
// Arithmetic
//
// Revision 1.3  2010/09/14 15:34:01  velktron
// The enormity of this commit is incredible (pun intended)
//
// Revision 1.2  2010/09/13 15:39:17  velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.1  2010/09/12 22:38:37  velktron
// Some work
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
//  Enemy thinking, AI.
//  Action Pointer Functions
//  that are associated with states/frames. 
//
//-----------------------------------------------------------------------------


public class Enemies {

  public static final String rcsid = "$Id: Enemies.java,v 1.4 2010/09/15 16:17:38 velktron Exp $";

  private static int DI_EAST=0;
  private static int    DI_NORTHEAST=1;
  private static int    DI_NORTH=2;
  private static int    DI_NORTHWEST=3;
  private static int    DI_WEST=4;
  private static int    DI_SOUTHWEST=5;
  private static int    DI_SOUTH=6;
  private static int    DI_SOUTHEAST=7;
  private static int    DI_NODIR=8;
  private static int    NUMDIR=9;

  //
  // P_NewChaseDir related LUT.
  //
  private static final int opposite[] =
  {
          DI_WEST, DI_SOUTHWEST, DI_SOUTH, DI_SOUTHEAST,
          DI_EAST, DI_NORTHEAST, DI_NORTH, DI_NORTHWEST, DI_NODIR
  };

  private static final int diags[] =
  {
          DI_NORTHWEST, DI_NORTHEAST, DI_SOUTHWEST, DI_SOUTHEAST
  };

  // void A_Fall (mobj_t *actor);


  //
  // ENEMY THINKING
  // Enemies are allways spawned
  // with targetplayer = -1, threshold = 0
  // Most monsters are spawned unaware of all players,
  // but some can be made preaware
  //


  //
  // Called by P_NoiseAlert.
  // Recursively traverse adjacent sectors,
  // sound blocking lines cut off traversal.
  //

  mobj_t     soundtarget;

  public void
  P_RecursiveSound
  ( sector_t sec,
    int       soundblocks )
  {
      int     i;
      line_t check;
      sector_t   other;
      
      // wake up all monsters in this sector
      if (sec.validcount == R.validcount
      && sec.soundtraversed <= soundblocks+1)
      {
      return;     // already flooded
      }
      
      sec.validcount = R.validcount;
      sec.soundtraversed = soundblocks+1;
      sec.soundtarget = soundtarget;
      
      // "peg" to the level loader for syntactic sugar
      side_t[] sides=L.sides;
      
      for (i=0 ;i<sec.linecount ; i++)
      {
      check = sec.lines[i];
      if ((check.flags & ML_TWOSIDED) ==0)
          continue;
      
      P_LineOpening (check);

      if (openrange <= 0)
          continue;   // closed door
      
      if ( sides[ check.sidenum[0] ].sector == sec)
          other = sides[ check.sidenum[1] ] .sector;
      else
          other = sides[ check.sidenum[0] ].sector;
      
      if ((check.flags & ML_SOUNDBLOCK)!=0)
      {
          if (soundblocks==0)
          P_RecursiveSound (other, 1);
      }
      else
          P_RecursiveSound (other, soundblocks);
      }
  }



  //
  // P_NoiseAlert
  // If a monster yells at a player,
  // it will alert other monsters to the player.
  //
  void
  P_NoiseAlert
  ( mobj_t   target,
    mobj_t   emmiter )
  {
      soundtarget = target;
      R.validcount++;
      P_RecursiveSound (emmiter.subsector.sector, 0);
  }




  


  //
  // P_Move
  // Move in the current direction,
  // returns false if the move is blocked.
  //
  int[] xspeed = {FRACUNIT,47000,0,-47000,-FRACUNIT,-47000,0,47000}; //all fixed
  int[] yspeed = {0,47000,FRACUNIT,47000,0,-47000,-FRACUNIT,-47000}; //all fixed


  // Peg to map movement 
    line_t[] spechitp=new line_t[MAXSPECIALCROSS];
    int numspechit;

  boolean P_Move (mobj_t actor)
  {
	  // fixed_t
      int tryx;
      int tryy;
      
      line_t ld;
      
      // warning: 'catch', 'throw', and 'try'
      // are all C++ reserved words
      boolean try_ok;
      boolean good;
          
      if (actor.movedir == DI_NODIR)
      return false;
          
      if (actor.movedir >= 8)
      system.Error ("Weird actor.movedir!");
          
      tryx = actor.x + actor.info.speed*xspeed[actor.movedir];
      tryy = actor.y + actor.info.speed*yspeed[actor.movedir];

      try_ok = P_TryMove (actor, tryx, tryy);

      if (!try_ok)
      {
      // open any specials
      if (actor.flags & MF_FLOAT && floatok)
      {
          // must adjust height
          if (actor.z < tmfloorz)
          actor.z += FLOATSPEED;
          else
          actor.z -= FLOATSPEED;

          actor.flags |= MF_INFLOAT;
          return true;
      }
          
      if (numspechit==0)
          return false;
              
      actor.movedir = DI_NODIR;
      good = false;
      while ((numspechit--)>0)
      {
          ld = spechit[numspechit];
          // if the special is not a door
          // that can be opened,
          // return false
          if (P_UseSpecialLine (actor, ld,0))
          good = true;
      }
      return good;
      }
      else
      {
      actor.flags &= ~MF_INFLOAT;
      }
      
      
      if (! (actor.flags & MF_FLOAT) )   
      actor.z = actor.floorz;
      return true; 
  }


  //
  // TryWalk
  // Attempts to move actor on
  // in its current (ob.moveangle) direction.
  // If blocked by either a wall or an actor
  // returns FALSE
  // If move is either clear or blocked only by a door,
  // returns TRUE and sets...
  // If a door is in the way,
  // an OpenDoor call is made to start it opening.
  //
  boolean P_TryWalk (mobj_t actor)
  {   
      if (!P_Move (actor))
      {
      return false;
      }

      actor.movecount = RND.P_Random()&15;
      return true;
  }




  void P_NewChaseDir (mobj_t actor)
  {
      fixed_t deltax;
      fixed_t deltay;
      
      dirtype_t   d[3];
      
      int     tdir;
      dirtype_t   olddir;
      
      dirtype_t   turnaround;

      if (actor.target==null)
      system.Error ("P_NewChaseDir: called with no target");
          
      olddir = actor.movedir;
      turnaround=opposite[olddir];

      deltax = actor.target.x - actor.x;
      deltay = actor.target.y - actor.y;

      if (deltax>10*FRACUNIT)
      d[1]= DI_EAST;
      else if (deltax<-10*FRACUNIT)
      d[1]= DI_WEST;
      else
      d[1]=DI_NODIR;

      if (deltay<-10*FRACUNIT)
      d[2]= DI_SOUTH;
      else if (deltay>10*FRACUNIT)
      d[2]= DI_NORTH;
      else
      d[2]=DI_NODIR;

      // try direct route
      if (d[1] != DI_NODIR
      && d[2] != DI_NODIR)
      {
      actor.movedir = diags[((deltay<0)<<1)+(deltax>0)];
      if (actor.movedir != turnaround && P_TryWalk(actor))
          return;
      }

      // try other directions
      if (P_Random() > 200
      ||  abs(deltay)>abs(deltax))
      {
      tdir=d[1];
      d[1]=d[2];
      d[2]=tdir;
      }

      if (d[1]==turnaround)
      d[1]=DI_NODIR;
      if (d[2]==turnaround)
      d[2]=DI_NODIR;
      
      if (d[1]!=DI_NODIR)
      {
      actor.movedir = d[1];
      if (P_TryWalk(actor))
      {
          // either moved forward or attacked
          return;
      }
      }

      if (d[2]!=DI_NODIR)
      {
      actor.movedir =d[2];

      if (P_TryWalk(actor))
          return;
      }

      // there is no direct path to the player,
      // so pick another direction.
      if (olddir!=DI_NODIR)
      {
      actor.movedir =olddir;

      if (P_TryWalk(actor))
          return;
      }

      // randomly determine direction of search
      if (RND.P_Random()&1)   
      {
      for ( tdir=DI_EAST;
            tdir<=DI_SOUTHEAST;
            tdir++ )
      {
          if (tdir!=turnaround)
          {
          actor.movedir =tdir;
          
          if ( P_TryWalk(actor) )
              return;
          }
      }
      }
      else
      {
      for ( tdir=DI_SOUTHEAST;
            tdir != (DI_EAST-1);
            tdir-- )
      {
          if (tdir!=turnaround)
          {
          actor.movedir =tdir;
          
          if ( P_TryWalk(actor) )
              return;
          }
      }
      }

      if (turnaround !=  DI_NODIR)
      {
      actor.movedir =turnaround;
      if ( P_TryWalk(actor) )
          return;
      }

      actor.movedir = DI_NODIR;  // can not move
  }





}
