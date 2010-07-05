// Emacs style mode select   -*- C++ -*-
//-----------------------------------------------------------------------------
//
// $Id: am_map.java,v 1.3 2010/07/05 16:18:40 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This source is available for distribution and/or modification
// only under the terms of the DOOM Source Code License as
// published by id Software. All rights reserved.
//
// The source is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// FITNESS FOR A PARTICULAR PURPOSE. See the DOOM Source Code License
// for more details.
//
//
// $Log: am_map.java,v $
// Revision 1.3  2010/07/05 16:18:40  velktron
// YOU DON'T WANNA KNOW
//
// Revision 1.2  2010/07/03 23:24:13  velktron
// Added a LOT of stuff, like Status bar code & objects. Now we're cooking with gas!
//
//
// DESCRIPTION:  the automap code
//
//-----------------------------------------------------------------------------

/** Partially derived from my vintage "JavaDOOM 2007" code.
 * 
 */

package am;
import static data.Defines.*;
import doom.player_t;
import rr.patch_t;
import m.cheatseq_t;
import m.fixed_t;
import static m.fixed_t.*;

public class am_map {
public static String rcsid = "$Id: am_map.java,v 1.3 2010/07/05 16:18:40 velktron Exp $";

/*
#include <stdio.h>
#include "z_zone.h"
#include "doomdef.h"
#include "st_stuff.h"
#include "p_local.h"
#include "w_wad.h"

#include "m_cheat.h"
#include "i_system.h"

// Needs access to LFB.
#include "v_video.h"

// State.
#include "doomstat.h"
#include "r_state.h"

// Data.
#include "dstrings.h"

#include "am_map.h"
*/

// For use if I do walls with outsides/insides
static final int REDS=(256-5*16);
static final int REDRANGE=16;
static final int BLUES=(256-4*16+8);
static final int BLUERANGE=8;
static final int GREENS=(7*16);
static final int GREENRANGE=16;
static final int GRAYS=(6*16);
static final int GRAYSRANGE=16;
static final int BROWNS=(4*16);
static final int BROWNRANGE=16;
static final int YELLOWS=(256-32+7);
static final int YELLOWRANGE=1;
static final int BLACK=0;
static final int WHITE=(256-47);

// Automap colors
static final int BACKGROUND=BLACK;
static final int YOURCOLORS=WHITE;
static final int YOURRANGE=0;
static final int WALLCOLORS=REDS;
static final int WALLRANGE=	REDRANGE;
static final int TSWALLCOLORS=	GRAYS;
static final int TSWALLRANGE=GRAYSRANGE;
static final int FDWALLCOLORS=BROWNS;
static final int FDWALLRANGE=BROWNRANGE;
static final int CDWALLCOLORS=YELLOWS;
static final int CDWALLRANGE=YELLOWRANGE;
static final int THINGCOLORS=GREENS;
static final int THINGRANGE=GREENRANGE;
static final int SECRETWALLCOLORS =WALLCOLORS;
static final int SECRETWALLRANGE= WALLRANGE;
static final int GRIDCOLORS=(GRAYS + GRAYSRANGE/2);
static final int GRIDRANGE=0;
static final int XHAIRCOLORS=GRAYS;

// drawing stuff
static final int	FB	=0;

static final char AM_PANDOWNKEY=KEY_DOWNARROW;
static final char AM_PANUPKEY=KEY_UPARROW;
static final char AM_PANRIGHTKEY=KEY_RIGHTARROW;
static final char AM_PANLEFTKEY=KEY_LEFTARROW;
static final char AM_ZOOMINKEY='=';
static final char AM_ZOOMOUTKEY	='-';
static final char AM_STARTKEY=KEY_TAB;
static final char AM_ENDKEY=	KEY_TAB;
static final char AM_GOBIGKEY=	'0';
static final char AM_FOLLOWKEY	='f';
static final char AM_GRIDKEY	='g';
static final char AM_MARKKEY	='m';
static final char AM_CLEARMARKKEY='c';

static final int AM_NUMMARKPOINTS=10;

// scale on entry
static final float INITSCALEMTOF=(float) (.2*FRACUNIT);
// how much the automap moves window per tic in frame-buffer coordinates
// moves 140 pixels in 1 second
static final int F_PANINC=4;
// how much zoom-in per tic
// goes to 2x in 1 second
static final int M_ZOOMIN=((int) (1.02*FRACUNIT));
// how much zoom-out per tic
// pulls out to 0.5x in 1 second
static final int M_ZOOMOUT=((int) (FRACUNIT/1.02));

static final int FTOM( int x){
  return  FixedMul(((x)<<16),scale_ftom);
}


// translates between frame-buffer and map distances
//#define FTOM(x) FixedMul(((x)<<16),scale_ftom)
//#define MTOF(x) (FixedMul((x),scale_mtof)>>16)
// translates between frame-buffer and map coordinates
//#define CXMTOF(x)  (f_x + MTOF((x)-m_x))
//#define CYMTOF(y)  (f_y + (f_h - MTOF((y)-m_y)))

// the following is crap
//#define LINE_NEVERSEE ML_DONTDRAW


//
// The vector graphics for the automap.
//  A line drawing of the player pointing right,
//   starting from the middle.
//

private static float R=((8*PLAYERRADIUS)/7);
mline_t player_arrow[] = {
    new { { -R+R/8, 0 }, { R, 0 } }, // -----
    { { R, 0 }, { R-R/2, R/4 } },  // ----->
    { { R, 0 }, { R-R/2, -R/4 } },
    { { -R+R/8, 0 }, { -R-R/8, R/4 } }, // >---->
    { { -R+R/8, 0 }, { -R-R/8, -R/4 } },
    { { -R+3*R/8, 0 }, { -R+R/8, R/4 } }, // >>--->
    { { -R+3*R/8, 0 }, { -R+R/8, -R/4 } }
};

static final int NUMPLYRLINES=(player_arrow.length/mline_t.length);

//#define R ((8*PLAYERRADIUS)/7)
mline_t cheat_player_arrow[] = {
    { { -R+R/8, 0 }, { R, 0 } }, // -----
    { { R, 0 }, { R-R/2, R/6 } },  // ----->
    { { R, 0 }, { R-R/2, -R/6 } },
    { { -R+R/8, 0 }, { -R-R/8, R/6 } }, // >----->
    { { -R+R/8, 0 }, { -R-R/8, -R/6 } },
    { { -R+3*R/8, 0 }, { -R+R/8, R/6 } }, // >>----->
    { { -R+3*R/8, 0 }, { -R+R/8, -R/6 } },
    { { -R/2, 0 }, { -R/2, -R/6 } }, // >>-d--->
    { { -R/2, -R/6 }, { -R/2+R/6, -R/6 } },
    { { -R/2+R/6, -R/6 }, { -R/2+R/6, R/4 } },
    { { -R/6, 0 }, { -R/6, -R/6 } }, // >>-dd-->
    { { -R/6, -R/6 }, { 0, -R/6 } },
    { { 0, -R/6 }, { 0, R/4 } },
    { { R/6, R/4 }, { R/6, -R/7 } }, // >>-ddt->
    { { R/6, -R/7 }, { R/6+R/32, -R/7-R/32 } },
    { { R/6+R/32, -R/7-R/32 }, { R/6+R/10, -R/7 } }
};

static final int NUMCHEATPLYRLINES=(cheat_player_arrow.length/mline_t.length);

float R=FRACUNIT;
mline_t triangle_guy[] = {
    { { -.867*R, -.5*R }, { .867*R, -.5*R } },
    { { .867*R, -.5*R } , { 0, R } },
    { { 0, R }, { -.867*R, -.5*R } }
};

static final int NUMTRIANGLEGUYLINES=(triangle_guy.length/mline_t.length);

float R=FRACUNIT;
mline_t thintriangle_guy[] = {
    { { -.5*R, -.7*R }, { R, 0 } },
    { { R, 0 }, { -.5*R, .7*R } },
    { { -.5*R, .7*R }, { -.5*R, -.7*R } }
};

static final int NUMTHINTRIANGLEGUYLINES=(thintriangle_guy.length/mline_t.length);

static int 	cheating = 0;
static int 	grid = 0;

static int 	leveljuststarted = 1; 	// kluge until AM_LevelInit() is called

boolean    	automapactive = false;
static int 	finit_width = SCREENWIDTH;
static int 	finit_height = SCREENHEIGHT - 32;

// location of window on screen
static int 	f_x;
static int	f_y;

// size of window on screen
static int 	f_w;
static int	f_h;

static int 	lightlev; 		// used for funky strobing effect
static byte[]	fb; 			// pseudo-frame buffer
static int 	amclock;

static mpoint_t m_paninc; // how far the window pans each tic (map coords)
static fixed_t 	mtof_zoommul; // how far the window zooms in each tic (map coords)
static fixed_t 	ftom_zoommul; // how far the window zooms in each tic (fb coords)

static fixed_t 	m_x, m_y;   // LL x,y where the window is on the map (map coords)
static fixed_t 	m_x2, m_y2; // UR x,y where the window is on the map (map coords)

//
// width/height of window on map (map coords)
//
static fixed_t 	m_w;
static fixed_t	m_h;

// based on level size
static fixed_t 	min_x;
static fixed_t	min_y;
static fixed_t 	max_x;
static fixed_t  max_y;

static fixed_t 	max_w; // max_x-min_x,
static fixed_t  max_h; // max_y-min_y

// based on player size
static fixed_t 	min_w;
static fixed_t  min_h;


static fixed_t 	min_scale_mtof; // used to tell when to stop zooming out
static fixed_t 	max_scale_mtof; // used to tell when to stop zooming in

// old stuff for recovery later
static fixed_t old_m_w, old_m_h;
static fixed_t old_m_x, old_m_y;

// old location used by the Follower routine
static mpoint_t f_oldloc;

// used by MTOF to scale from map-to-frame-buffer coords
static fixed_t scale_mtof = INITSCALEMTOF;
// used by FTOM to scale from frame-buffer-to-map coords (=1/scale_mtof)
static fixed_t scale_ftom;

static player_t plr; // the player represented by an arrow

static patch_t[] marknums=new patch_t[10]; // numbers used for marking by the automap
static mpoint_t[] markpoints=new mpoint_t[AM_NUMMARKPOINTS]; // where the points are
static int markpointnum = 0; // next point to be assigned

static int followplayer = 1; // specifies whether to follow the player around

static char[] cheat_amap_seq = { 0xb2, 0x26, 0x26, 0x2e, 0xff };
static cheatseq_t cheat_amap =  new cheatseq_t(cheat_amap_seq, 0 );

static boolean stopped = true;

//extern
boolean viewactive;
//extern byte screens[][SCREENWIDTH*SCREENHEIGHT];



static void
V_MarkRect
( int	x,
  int	y,
  int	width,
  int	height );

// Calculates the slope and slope according to the x-axis of a line
// segment in map coordinates (with the upright y-axis n' all) so
// that it can be used with the brain-dead drawing stuff.

static void
AM_getIslope
( mline_t	ml,
  islope_t	is )
{
    int dx, dy;

    dy = ml.a.y - ml.b.y;
    dx = ml.b.x - ml.a.x;
    if (!dy) is.islp = (dx<0?-MAXINT:MAXINT);
    else is.islp = FixedDiv(dx, dy);
    if (!dx) is.slp = (dy<0?-MAXINT:MAXINT);
    else is.slp = FixedDiv(dy, dx);

}

//
//
//
static void AM_activateNewScale()
{
    m_x += m_w/2;
    m_y += m_h/2;
    m_w = FTOM(f_w);
    m_h = FTOM(f_h);
    m_x -= m_w/2;
    m_y -= m_h/2;
    m_x2 = m_x + m_w;
    m_y2 = m_y + m_h;
}

//
//
//
static void AM_saveScaleAndLoc()
{
    old_m_x = m_x;
    old_m_y = m_y;
    old_m_w = m_w;
    old_m_h = m_h;
}

//
//
//
static void AM_restoreScaleAndLoc()
{

    m_w = old_m_w;
    m_h = old_m_h;
    if (!followplayer)
    {
	m_x = old_m_x;
	m_y = old_m_y;
    } else {
	m_x = plr.mo.x - m_w/2;
	m_y = plr.mo.y - m_h/2;
    }
    m_x2 = m_x + m_w;
    m_y2 = m_y + m_h;

    // Change the scaling multipliers
    scale_mtof = FixedDiv(f_w<<FRACBITS, m_w);
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
}

//
// adds a marker at the current location
//
 static void AM_addMark()
{
    markpoints[markpointnum].x = m_x + m_w/2;
    markpoints[markpointnum].y = m_y + m_h/2;
    markpointnum = (markpointnum + 1) % AM_NUMMARKPOINTS;

}

//
// Determines bounding box of all vertices,
// sets global variables controlling zoom range.
//
void AM_findMinMaxBoundaries()
{
    int i;
    fixed_t a;
    fixed_t b;

    min_x = min_y =  MAXINT;
    max_x = max_y = -MAXINT;

    for (i=0;i<numvertexes;i++)
    {
	if (vertexes[i].x < min_x)
	    min_x = vertexes[i].x;
	else if (vertexes[i].x > max_x)
	    max_x = vertexes[i].x;

	if (vertexes[i].y < min_y)
	    min_y = vertexes[i].y;
	else if (vertexes[i].y > max_y)
	    max_y = vertexes[i].y;
    }

    max_w = max_x - min_x;
    max_h = max_y - min_y;

    min_w = 2*PLAYERRADIUS; // const? never changed?
    min_h = 2*PLAYERRADIUS;

    a = FixedDiv(f_w<<FRACBITS, max_w);
    b = FixedDiv(f_h<<FRACBITS, max_h);

    min_scale_mtof = a < b ? a : b;
    max_scale_mtof = FixedDiv(f_h<<FRACBITS, 2*PLAYERRADIUS);

}


//
//
//
void AM_changeWindowLoc()
{
    if (m_paninc.x || m_paninc.y)
    {
	followplayer = 0;
	f_oldloc.x = MAXINT;
    }

    m_x += m_paninc.x;
    m_y += m_paninc.y;

    if (m_x + m_w/2 > max_x)
	m_x = max_x - m_w/2;
    else if (m_x + m_w/2 < min_x)
	m_x = min_x - m_w/2;

    if (m_y + m_h/2 > max_y)
	m_y = max_y - m_h/2;
    else if (m_y + m_h/2 < min_y)
	m_y = min_y - m_h/2;

    m_x2 = m_x + m_w;
    m_y2 = m_y + m_h;
}


//
//
//
private static event_t st_notify = { ev_keyup, AM_MSGENTERED };

void AM_initVariables()
{
    int pnum;


    automapactive = true;
    fb = screens[0];

    f_oldloc.x = MAXINT;
    amclock = 0;
    lightlev = 0;

    m_paninc.x = m_paninc.y = 0;
    ftom_zoommul = FRACUNIT;
    mtof_zoommul = FRACUNIT;

    m_w = FTOM(f_w);
    m_h = FTOM(f_h);

    // find player to center on initially
    if (!playeringame[pnum = consoleplayer])
	for (pnum=0;pnum<MAXPLAYERS;pnum++)
	    if (playeringame[pnum])
		break;

    plr = players[pnum];
    m_x = plr.mo.x - m_w/2;
    m_y = plr.mo.y - m_h/2;
    AM_changeWindowLoc();

    // for saving & restoring
    old_m_x = m_x;
    old_m_y = m_y;
    old_m_w = m_w;
    old_m_h = m_h;

    // inform the status bar of the change
    ST_Responder(st_notify);

}

//
//
//
void AM_loadPics()
{
    int i;
    char namebuf=new char[9];

    for (i=0;i<10;i++)
    {
	sprintf(namebuf, "AMMNUM%d", i);
	marknums[i] = W_CacheLumpName(namebuf, PU_STATIC);
    }

}

void AM_unloadPics()
{
    int i;

    for (i=0;i<10;i++)
	Z_ChangeTag(marknums[i], PU_CACHE);

}

void AM_clearMarks()
{
    int i;

    for (i=0;i<AM_NUMMARKPOINTS;i++)
	markpoints[i].x = -1; // means empty
    markpointnum = 0;
}

//
// should be called at the start of every level
// right now, i figure it out myself
//
void AM_LevelInit()
{
    leveljuststarted = 0;

    f_x = f_y = 0;
    f_w = finit_width;
    f_h = finit_height;

    AM_clearMarks();

    AM_findMinMaxBoundaries();
    scale_mtof = FixedDiv(min_scale_mtof, (int) (0.7*FRACUNIT));
    if (scale_mtof > max_scale_mtof)
	scale_mtof = min_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
}




//
//
//

static event_t st_notify = { 0, ev_keyup, AM_MSGEXITED };

void AM_Stop ()
{


    AM_unloadPics();
    automapactive = false;
    ST_Responder(st_notify);
    stopped = true;
}

//
//
//
static int lastlevel = -1, lastepisode = -1;

void AM_Start ()
{
    if (!stopped) AM_Stop();
    stopped = false;
    if (lastlevel != gamemap || lastepisode != gameepisode)
    {
	AM_LevelInit();
	lastlevel = gamemap;
	lastepisode = gameepisode;
    }
    AM_initVariables();
    AM_loadPics();
}

//
// set the window scale to the maximum size
//
void AM_minOutWindowScale()
{
    scale_mtof = min_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
    AM_activateNewScale();
}

//
// set the window scale to the minimum size
//
void AM_maxOutWindowScale()
{
    scale_mtof = max_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
    AM_activateNewScale();
}


//
// Handle events (user inputs) in automap mode
//
  static int cheatstate=0;
  static int bigstate=0;
  static char buffer=new char[20];

boolean
AM_Responder
( event_t	ev )
{

    int rc;

    rc = false;

    if (!automapactive)
    {
	if (ev.type == ev_keydown && ev.data1 == AM_STARTKEY)
	{
	    AM_Start ();
	    viewactive = false;
	    rc = true;
	}
    }

    else if (ev.type == ev_keydown)
    {

	rc = true;
	switch(ev.data1)
	{
	  case AM_PANRIGHTKEY: // pan right
	    if (!followplayer) m_paninc.x = FTOM(F_PANINC);
	    else rc = false;
	    break;
	  case AM_PANLEFTKEY: // pan left
	    if (!followplayer) m_paninc.x = -FTOM(F_PANINC);
	    else rc = false;
	    break;
	  case AM_PANUPKEY: // pan up
	    if (!followplayer) m_paninc.y = FTOM(F_PANINC);
	    else rc = false;
	    break;
	  case AM_PANDOWNKEY: // pan down
	    if (!followplayer) m_paninc.y = -FTOM(F_PANINC);
	    else rc = false;
	    break;
	  case AM_ZOOMOUTKEY: // zoom out
	    mtof_zoommul = M_ZOOMOUT;
	    ftom_zoommul = M_ZOOMIN;
	    break;
	  case AM_ZOOMINKEY: // zoom in
	    mtof_zoommul = M_ZOOMIN;
	    ftom_zoommul = M_ZOOMOUT;
	    break;
	  case AM_ENDKEY:
	    bigstate = 0;
	    viewactive = true;
	    AM_Stop ();
	    break;
	  case AM_GOBIGKEY:
	    bigstate = !bigstate;
	    if (bigstate)
	    {
		AM_saveScaleAndLoc();
		AM_minOutWindowScale();
	    }
	    else AM_restoreScaleAndLoc();
	    break;
	  case AM_FOLLOWKEY:
	    followplayer = !followplayer;
	    f_oldloc.x = MAXINT;
	    plr.message = followplayer ? AMSTR_FOLLOWON : AMSTR_FOLLOWOFF;
	    break;
	  case AM_GRIDKEY:
	    grid = !grid;
	    plr.message = grid ? AMSTR_GRIDON : AMSTR_GRIDOFF;
	    break;
	  case AM_MARKKEY:
	    sprintf(buffer, "%s %d", AMSTR_MARKEDSPOT, markpointnum);
	    plr.message = buffer;
	    AM_addMark();
	    break;
	  case AM_CLEARMARKKEY:
	    AM_clearMarks();
	    plr.message = AMSTR_MARKSCLEARED;
	    break;
	  default:
	    cheatstate=0;
	    rc = false;
	}
	if (!deathmatch && cht_CheckCheat(cheat_amap, ev.data1))
	{
	    rc = false;
	    cheating = (cheating+1) % 3;
	}
    }

    else if (ev.type == ev_keyup)
    {
	rc = false;
	switch (ev.data1)
	{
	  case AM_PANRIGHTKEY:
	    if (!followplayer) m_paninc.x = 0;
	    break;
	  case AM_PANLEFTKEY:
	    if (!followplayer) m_paninc.x = 0;
	    break;
	  case AM_PANUPKEY:
	    if (!followplayer) m_paninc.y = 0;
	    break;
	  case AM_PANDOWNKEY:
	    if (!followplayer) m_paninc.y = 0;
	    break;
	  case AM_ZOOMOUTKEY:
	  case AM_ZOOMINKEY:
	    mtof_zoommul = FRACUNIT;
	    ftom_zoommul = FRACUNIT;
	    break;
	}
    }

    return rc;

}


//
// Zooming
//
void AM_changeWindowScale()
{

    // Change the scaling multipliers
    scale_mtof = FixedMul(scale_mtof, mtof_zoommul);
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);

    if (scale_mtof < min_scale_mtof)
	AM_minOutWindowScale();
    else if (scale_mtof > max_scale_mtof)
	AM_maxOutWindowScale();
    else
	AM_activateNewScale();
}


//
//
//
void AM_doFollowPlayer()
{

    if (f_oldloc.x != plr.mo.x || f_oldloc.y != plr.mo.y)
    {
	m_x = FTOM(MTOF(plr.mo.x)) - m_w/2;
	m_y = FTOM(MTOF(plr.mo.y)) - m_h/2;
	m_x2 = m_x + m_w;
	m_y2 = m_y + m_h;
	f_oldloc.x = plr.mo.x;
	f_oldloc.y = plr.mo.y;

	//  m_x = FTOM(MTOF(plr->mo->x - m_w/2));
	//  m_y = FTOM(MTOF(plr->mo->y - m_h/2));
	//  m_x = plr->mo->x - m_w/2;
	//  m_y = plr->mo->y - m_h/2;

    }

}

//
//
//
static int nexttic = 0;
//static int litelevels[] = { 0, 3, 5, 6, 6, 7, 7, 7 };
static int litelevels[] = { 0, 4, 7, 10, 12, 14, 15, 15 };
static int litelevelscnt = 0;

void AM_updateLightLev()
{


    // Change light level
    if (amclock>nexttic)
    {
	lightlev = litelevels[litelevelscnt++];
	if (litelevelscnt == litelevels.length) litelevelscnt = 0;
	nexttic = amclock + 6 - (amclock % 6);
    }

}


//
// Updates on Game Tick
//
void AM_Ticker ()
{

    if (!automapactive)
	return;

    amclock++;

    if (followplayer)
	AM_doFollowPlayer();

    // Change the zoom if necessary
    if (ftom_zoommul != FRACUNIT)
	AM_changeWindowScale();

    // Change x,y location
    if (m_paninc.x || m_paninc.y)
	AM_changeWindowLoc();

    // Update light level
    // AM_updateLightLev();

}


//
// Clear automap frame buffer.
//
void AM_clearFB(int color)
{
    memset(fb, color, f_w*f_h);
}

static int LEFT=1,RIGHT=2,BOTTOM=4,TOP=8;

private static int DOOUTCODE( int mx, int my){
      oc = 0;
      if ((my) < 0) (oc) |= TOP;
      else if ((my) >= f_h) (oc) |= BOTTOM;
      if ((mx) < 0) (oc) |= LEFT;
    else if ((mx) >= f_w) (oc) |= RIGHT;
    return oc;
}

//
// Automap clipping of lines.
//
// Based on Cohen-Sutherland clipping algorithm but with a slightly
// faster reject and precalculated slopes.  If the speed is needed,
// use a hash algorithm to handle  the common cases.
//
boolean
AM_clipMline
( mline_t	ml,
  fline_t	fl )
{



    int	outcode1 = 0;
    int	outcode2 = 0;
    int	outside;

    fpoint_t	tmp;
    int		dx;
    int		dy;





    // do trivial rejects and outcodes
    if (ml.a.y > m_y2)
	outcode1 = TOP;
    else if (ml.a.y < m_y)
	outcode1 = BOTTOM;

    if (ml.b.y > m_y2)
	outcode2 = TOP;
    else if (ml.b.y < m_y)
	outcode2 = BOTTOM;

    if (outcode1 & outcode2)
	return false; // trivially outside

    if (ml.a.x < m_x)
	outcode1 |= LEFT;
    else if (ml.a.x > m_x2)
	outcode1 |= RIGHT;

    if (ml.b.x < m_x)
	outcode2 |= LEFT;
    else if (ml.b.x > m_x2)
	outcode2 |= RIGHT;

    if (outcode1 & outcode2)
	return false; // trivially outside

    // transform to frame-buffer coordinates.
    fl.a.x = CXMTOF(ml.a.x);
    fl.a.y = CYMTOF(ml.a.y);
    fl.b.x = CXMTOF(ml.b.x);
    fl.b.y = CYMTOF(ml.b.y);

    outcode1=DOOUTCODE(fl.a.x, fl.a.y);
    outcode2=DOOUTCODE(fl.b.x, fl.b.y);

    if (outcode1 & outcode2)
	return false;

    while (outcode1 | outcode2)
    {
	// may be partially inside box
	// find an outside point
	if (outcode1)
	    outside = outcode1;
	else
	    outside = outcode2;

	// clip to each side
	if (outside & TOP)
	{
	    dy = fl.a.y - fl.b.y;
	    dx = fl.b.x - fl.a.x;
	    tmp.x = fl.a.x + (dx*(fl.a.y))/dy;
	    tmp.y = 0;
	}
	else if (outside & BOTTOM)
	{
	    dy = fl.a.y - fl.b.y;
	    dx = fl.b.x - fl.a.x;
	    tmp.x = fl.a.x + (dx*(fl.a.y-f_h))/dy;
	    tmp.y = f_h-1;
	}
	else if (outside & RIGHT)
	{
	    dy = fl.b.y - fl.a.y;
	    dx = fl.b.x - fl.a.x;
	    tmp.y = fl.a.y + (dy*(f_w-1 - fl.a.x))/dx;
	    tmp.x = f_w-1;
	}
	else if (outside & LEFT)
	{
	    dy = fl.b.y - fl.a.y;
	    dx = fl.b.x - fl.a.x;
	    tmp.y = fl.a.y + (dy*(-fl.a.x))/dx;
	    tmp.x = 0;
	}

	if (outside == outcode1)
	{
	    fl.a = tmp;
	    DOOUTCODE(outcode1, fl.a.x, fl.a.y);
	}
	else
	{
	    fl.b = tmp;
	    DOOUTCODE(outcode2, fl.b.x, fl.b.y);
	}

	if (outcode1 & outcode2)
	    return false; // trivially outside
    }

    return true;
}

//
// Classic Bresenham w/ whatever optimizations needed for speed
//
private static int fuck = 0;

private static final void PUTDOT(int xx,int yy,int cc){
      fb[(yy)*f_w+(xx)]=(cc);
}

void
AM_drawFline
( fline_t	fl,
  int		color )
{
     int x;
     int y;
     int dx;
     int dy;
     int sx;
     int sy;
     int ax;
     int ay;
     int d;



    // For debugging only
    if (      fl.a.x < 0 || fl.a.x >= f_w
	   || fl.a.y < 0 || fl.a.y >= f_h
	   || fl.b.x < 0 || fl.b.x >= f_w
	   || fl.b.y < 0 || fl.b.y >= f_h)
    {
	System.err.print("fuck \r"+ fuck++);
	return;
    }

    dx = fl.b.x - fl.a.x;
    ax = 2 * (dx<0 ? -dx : dx);
    sx = dx<0 ? -1 : 1;

    dy = fl.b.y - fl.a.y;
    ay = 2 * (dy<0 ? -dy : dy);
    sy = dy<0 ? -1 : 1;

    x = fl.a.x;
    y = fl.a.y;

    if (ax > ay)
    {
	d = ay - ax/2;
	while (1)
	{
	    PUTDOT(x,y,color);
	    if (x == fl.b.x) return;
	    if (d>=0)
	    {
		y += sy;
		d -= ax;
	    }
	    x += sx;
	    d += ay;
	}
    }
    else
    {
	d = ax - ay/2;
	while (1)
	{
	    PUTDOT(x, y, color);
	    if (y == fl.b.y) return;
	    if (d >= 0)
	    {
		x += sx;
		d -= ay;
	    }
	    y += sy;
	    d += ax;
	}
    }
}


//
// Clip lines, draw visible part sof lines.
//
static fline_t fl;

void
AM_drawMline
( mline_t	ml,
  int		color )
{

    if (AM_clipMline(ml, fl))
	AM_drawFline(fl, color); // draws it on frame buffer using fb coords
}



//
// Draws flat (floor/ceiling tile) aligned grid lines.
//
void AM_drawGrid(int color)
{
    fixed_t x, y;
    fixed_t start, end;
    mline_t ml;

    // Figure out start of vertical gridlines
    start = m_x;
    if ((start-bmaporgx)%(MAPBLOCKUNITS<<FRACBITS))
	start += (MAPBLOCKUNITS<<FRACBITS)
	    - ((start-bmaporgx)%(MAPBLOCKUNITS<<FRACBITS));
    end = m_x + m_w;

    // draw vertical gridlines
    ml.a.y = m_y;
    ml.b.y = m_y+m_h;
    for (x=start; x<end; x+=(MAPBLOCKUNITS<<FRACBITS))
    {
	ml.a.x = x;
	ml.b.x = x;
	AM_drawMline(ml, color);
    }

    // Figure out start of horizontal gridlines
    start = m_y;
    if ((start-bmaporgy)%(MAPBLOCKUNITS<<FRACBITS))
	start += (MAPBLOCKUNITS<<FRACBITS)
	    - ((start-bmaporgy)%(MAPBLOCKUNITS<<FRACBITS));
    end = m_y + m_h;

    // draw horizontal gridlines
    ml.a.x = m_x;
    ml.b.x = m_x + m_w;
    for (y=start; y<end; y+=(MAPBLOCKUNITS<<FRACBITS))
    {
	ml.a.y = y;
	ml.b.y = y;
	AM_drawMline(ml, color);
    }

}

//
// Determines visible lines, draws them.
// This is LineDef based, not LineSeg based.
//
private static mline_t l;

void AM_drawWalls()
{
    int i;

    for (i=0;i<numlines;i++)
    {
	l.a.x = lines[i].v1.x;
	l.a.y = lines[i].v1.y;
	l.b.x = lines[i].v2.x;
	l.b.y = lines[i].v2.y;
	if (cheating || (lines[i].flags & ML_MAPPED))
	{
	    if ((lines[i].flags & LINE_NEVERSEE) && !cheating)
		continue;
	    if (!lines[i].backsector)
	    {
		AM_drawMline(l, WALLCOLORS+lightlev);
	    }
	    else
	    {
		if (lines[i].special == 39)
		{ // teleporters
		    AM_drawMline(l, WALLCOLORS+WALLRANGE/2);
		}
		else if (lines[i].flags & ML_SECRET) // secret door
		{
		    if (cheating) AM_drawMline(l, SECRETWALLCOLORS + lightlev);
		    else AM_drawMline(l, WALLCOLORS+lightlev);
		}
		else if (lines[i].backsector.floorheight
			   != lines[i].frontsector.floorheight) {
		    AM_drawMline(l, FDWALLCOLORS + lightlev); // floor level change
		}
		else if (lines[i].backsector.ceilingheight
			   != lines[i].frontsector.ceilingheight) {
		    AM_drawMline(l, CDWALLCOLORS+lightlev); // ceiling level change
		}
		else if (cheating) {
		    AM_drawMline(l, TSWALLCOLORS+lightlev);
		}
	    }
	}
	else if (plr.powers[pw_allmap])
	{
	    if (!(lines[i].flags & LINE_NEVERSEE)) AM_drawMline(l, GRAYS+3);
	}
    }
}


//
// Rotation in 2D.
// Used to rotate player arrow line character.
//
void
AM_rotate
( fixed_t	x,
  fixed_t	y,
  angle_t	a )
{
    fixed_t tmpx;

    tmpx =
	FixedMul(x,finecosine[a>>ANGLETOFINESHIFT])
	- FixedMul(y,finesine[a>>ANGLETOFINESHIFT]);

    y   =
	FixedMul(x,finesine[a>>ANGLETOFINESHIFT])
	+ FixedMul(y,finecosine[a>>ANGLETOFINESHIFT]);

    x = tmpx;
}

void
AM_drawLineCharacter
( mline_t	lineguy,
  int		lineguylines,
  fixed_t	scale,
  angle_t	angle,
  int		color,
  fixed_t	x,
  fixed_t	y )
{
    int		i;
    mline_t	l;

    for (i=0;i<lineguylines;i++)
    {
	l.a.x = lineguy[i].a.x;
	l.a.y = lineguy[i].a.y;

	if (scale)
	{
	    l.a.x = FixedMul(scale, l.a.x);
	    l.a.y = FixedMul(scale, l.a.y);
	}

	if (angle)
	    AM_rotate(l.a.x, l.a.y, angle);

	l.a.x += x;
	l.a.y += y;

	l.b.x = lineguy[i].b.x;
	l.b.y = lineguy[i].b.y;

	if (scale)
	{
	    l.b.x = FixedMul(scale, l.b.x);
	    l.b.y = FixedMul(scale, l.b.y);
	}

	if (angle)
	    AM_rotate(l.b.x, l.b.y, angle);

	l.b.x += x;
	l.b.y += y;

	AM_drawMline(l, color);
    }
}

private static int their_colors[] = { GREENS, GRAYS, BROWNS, REDS };

void AM_drawPlayers()
{
    int		i;
    player_t	p;
    int		their_color = -1;
    int		color;

    if (!netgame)
    {
	if (cheating)
	    AM_drawLineCharacter
		(cheat_player_arrow, NUMCHEATPLYRLINES, 0,
		 plr.mo.angle, WHITE, plr.mo.x, plr.mo.y);
	else
	    AM_drawLineCharacter
		(player_arrow, NUMPLYRLINES, 0, plr.mo.angle,
		 WHITE, plr.mo.x, plr.mo.y);
	return;
    }

    for (i=0;i<MAXPLAYERS;i++)
    {
	their_color++;
	p = players[i];

	if ( (deathmatch && !singledemo) && p != plr)
	    continue;

	if (!playeringame[i])
	    continue;

	if (p.powers[pw_invisibility])
	    color = 246; // *close* to black
	else
	    color = their_colors[their_color];

	AM_drawLineCharacter
	    (player_arrow, NUMPLYRLINES, 0, p.mo.angle,
	     color, p.mo.x, p.mo.y);
    }

}

void
AM_drawThings
( int	colors,
  int 	colorrange)
{
    int		i;
    mobj_t	t;

    for (i=0;i<numsectors;i++)
    {
	t = sectors[i].thinglist;
	while (t)
	{
	    AM_drawLineCharacter
		(thintriangle_guy, NUMTHINTRIANGLEGUYLINES,
		 16<<FRACBITS, t.angle, colors+lightlev, t.x, t.y);
	    t = t.snext;
	}
    }
}

void AM_drawMarks()
{
    int i, fx, fy, w, h;

    for (i=0;i<AM_NUMMARKPOINTS;i++)
    {
	if (markpoints[i].x != -1)
	{
	    //      w = SHORT(marknums[i]->width);
	    //      h = SHORT(marknums[i]->height);
	    w = 5; // because something's wrong with the wad, i guess
	    h = 6; // because something's wrong with the wad, i guess
	    fx = CXMTOF(markpoints[i].x);
	    fy = CYMTOF(markpoints[i].y);
	    if (fx >= f_x && fx <= f_w - w && fy >= f_y && fy <= f_h - h)
		V_DrawPatch(fx, fy, FB, marknums[i]);
	}
    }

}

void AM_drawCrosshair(int color)
{
    fb[(f_w*(f_h+1))/2] = color; // single point for now

}

void AM_Drawer ()
{
    if (!automapactive) return;

    AM_clearFB(BACKGROUND);
    if (grid)
	AM_drawGrid(GRIDCOLORS);
    AM_drawWalls();
    AM_drawPlayers();
    if (cheating==2)
	AM_drawThings(THINGCOLORS, THINGRANGE);
    AM_drawCrosshair(XHAIRCOLORS);

    AM_drawMarks();

    V_MarkRect(f_x, f_y, f_w, f_h);

}
}
