package automap;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Map.java,v 1.2 2010/08/10 16:41:57 velktron Exp $
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
//
//
// $Log: Map.java,v $
// Revision 1.2  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
// Revision 1.1  2010/07/20 15:52:56  velktron
// LOTS of changes, Automap almost complete. Use of fixed_t inside methods severely limited.
//
// Revision 1.1  2010/06/30 08:58:51  velktron
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
// DESCRIPTION:  the automap code
//
//-----------------------------------------------------------------------------

import static data.Defines.*;
import static data.doomtype.*;
import static m.fixed_t.*;
import data.doomstat;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;
import rr.patch_t;
import st.DoomStatusBarInterface;
import v.DoomVideoRenderer;
import w.WadLoader;
import m.cheatseq_t;
import m.fixed_t;

public class Map implements DoomAutoMap{

/////////////////// Status objects ///////////////////
    
DoomStatusBarInterface ST;
WadLoader W;
doomstat DS;
DoomVideoRenderer V;
    
    
    
public final String rcsid = "$Id: Map.java,v 1.2 2010/08/10 16:41:57 velktron Exp $";

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

public static final int REDS    =   (256-5*16);
public static final int REDRANGE=   16;
public static final int BLUES   =   (256-4*16+8);
public static final int BLUERANGE   =8;
public static final int GREENS      =(7*16);
public static final int GREENRANGE  =16;
public static final int GRAYS       =(6*16);
public static final int GRAYSRANGE  =16;
public static final int BROWNS      =(4*16);
public static final int BROWNRANGE  =16;
public static final int YELLOWS     =(256-32+7);
public static final int YELLOWRANGE =1;
public static final int BLACK       =0;
public static final int WHITE       =(256-47);

// Automap colors
public static final int BACKGROUND  =BLACK;
public static final int YOURCOLORS  =WHITE;
public static final int YOURRANGE   =0;
public static final int WALLCOLORS  =REDS;
public static final int WALLRANGE   =REDRANGE;
public static final int TSWALLCOLORS=   GRAYS;
public static final int TSWALLRANGE =GRAYSRANGE;
public static final int FDWALLCOLORS=   BROWNS;
public static final int FDWALLRANGE =BROWNRANGE;
public static final int CDWALLCOLORS=   YELLOWS;
public static final int CDWALLRANGE =YELLOWRANGE;
public static final int THINGCOLORS =GREENS;
public static final int THINGRANGE  =GREENRANGE;
public static final int SECRETWALLCOLORS =WALLCOLORS;
public static final int SECRETWALLRANGE =WALLRANGE;
public static final int GRIDCOLORS  =(GRAYS + GRAYSRANGE/2);
public static final int GRIDRANGE   =0;
public static final int XHAIRCOLORS =GRAYS;

// drawing stuff
public static final int FB  =   0;

public static final char AM_PANDOWNKEY  =KEY_DOWNARROW;
public static final char AM_PANUPKEY    =KEY_UPARROW;
public static final char AM_PANRIGHTKEY =KEY_RIGHTARROW;
public static final char AM_PANLEFTKEY  =KEY_LEFTARROW;
public static final char AM_ZOOMINKEY=  '=';
public static final char AM_ZOOMOUTKEY= '-';
public static final char AM_STARTKEY=   KEY_TAB;
public static final char AM_ENDKEY  =KEY_TAB;
public static final char AM_GOBIGKEY=   '0';
public static final char AM_FOLLOWKEY=  'f';
public static final char AM_GRIDKEY=    'g';
public static final char AM_MARKKEY=    'm';
public static final char AM_CLEARMARKKEY=   'c';

public static final int AM_NUMMARKPOINTS= 10;

// (fixed_t) scale on entry
public static final int INITSCALEMTOF =(int) (.2*FRACUNIT);
// how much the automap moves window per tic in frame-buffer coordinates
// moves 140 pixels in 1 second
public static final int F_PANINC    =4;
// how much zoom-in per tic
// goes to 2x in 1 second
public static final int M_ZOOMIN =       ((int) (1.02*FRACUNIT));
// how much zoom-out per tic
// pulls out to 0.5x in 1 second
public static final int M_ZOOMOUT    =   ((int) (FRACUNIT/1.02));

/** translates between frame-buffer and map distances */
private int FTOM(int x){return FixedMul(((x)<<16),scale_ftom);}
/** translates between frame-buffer and map distances */
private int MTOF(int x) {return FixedMul((x),scale_mtof)>>16;}
/** translates between frame-buffer and map coordinates */
private int CXMTOF(int x){ return (f_x + MTOF((x)-m_x));}
/** translates between frame-buffer and map coordinates */
private int CYMTOF(int y) {return (f_y + (f_h - MTOF((y)-m_y)));}

// the following is crap
//public static final int LINE_NEVERSEE =ML_DONTDRAW;

//
// The vector graphics for the automap.
/** A line drawing of the player pointing right,
 *  starting from the middle. */
private static mline_t[] player_arrow;
public static int NUMPLYRLINES;
private static mline_t[] cheat_player_arrow;
public static int NUMCHEATPLYRLINES;
private static mline_t[] triangle_guy;
public static int NUMTRIANGLEGUYLINES;
private static mline_t[] thintriangle_guy;
private static int NUMTHINTRIANGLEGUYLINES;

public static void initVectorGraphics(){

int R =((8*PLAYERRADIUS)/7);
player_arrow = new mline_t[]{
     new mline_t( -R+R/8, 0,  R, 0 ), // -----
     new mline_t( R, 0 , R-R/2, R/4),  // ----
     new mline_t( R, 0 ,  R-R/2, -R/4 ), 
      new mline_t(-R+R/8, 0 , -R-R/8, R/4 ),  // >---
      new mline_t(-R+R/8, 0 , -R-R/8, -R/4 ), 
      new mline_t( -R+3*R/8, 0 , -R+R/8, R/4 ),  // >>--
      new mline_t(-R+3*R/8, 0 , -R+R/8, -R/4 ) 
 };

NUMPLYRLINES =player_arrow.length;

cheat_player_arrow = new mline_t[]{
    new mline_t( -R+R/8, 0 , R, 0 ), // -----
    new mline_t( R, 0 , R-R/2, R/6 ),  // ----
    new mline_t( R, 0 , R-R/2, -R/6 ),
    new mline_t( -R+R/8, 0 , -R-R/8, R/6 ), // >----
    new mline_t( -R+R/8, 0 , -R-R/8, -R/6 ),
    new mline_t( -R+3*R/8, 0 , -R+R/8, R/6 ), // >>----
    new mline_t( -R+3*R/8, 0 , -R+R/8, -R/6 ),
    new mline_t( -R/2, 0 , -R/2, -R/6 ), // >>-d--
    new mline_t(-R/2, -R/6 , -R/2+R/6, -R/6 ),
    new mline_t( -R/2+R/6, -R/6 , -R/2+R/6, R/4 ),
    new mline_t(-R/6, 0 , -R/6, -R/6 ), // >>-dd-
    new mline_t( -R/6, -R/6 , 0, -R/6 ),
    new mline_t( 0, -R/6 , 0, R/4 ),
    new mline_t( R/6, R/4 , R/6, -R/7 ), // >>-ddt
    new mline_t( R/6, -R/7 , R/6+R/32, -R/7-R/32 ),
    new mline_t( R/6+R/32, -R/7-R/32 , R/6+R/10, -R/7 ) 
};

NUMCHEATPLYRLINES =cheat_player_arrow.length;

R=(FRACUNIT);
triangle_guy = new mline_t[]{
    new mline_t(-.867*R, -.5*R , .867*R, -.5*R ),
    new mline_t( .867*R, -.5*R  , 0, R ),
    new mline_t( 0, R , -.867*R, -.5*R )
};

NUMTRIANGLEGUYLINES =triangle_guy.length;

thintriangle_guy = new mline_t[]{
        new mline_t(-.5*R, -.7*R , R, 0 ),
        new mline_t( R, 0 , -.5*R, .7*R ),
        new mline_t( -.5*R, .7*R , -.5*R, -.7*R)
        };

NUMTHINTRIANGLEGUYLINES= thintriangle_guy.length;
}

private int  cheating = 0;
private int  grid = 0;

private int  leveljuststarted = 1;   // kluge until AM_LevelInit() is called

boolean     automapactive = false;
private int  finit_width = SCREENWIDTH;
private int  finit_height = SCREENHEIGHT - 32;

// location of window on screen
private int  f_x;
private int  f_y;

// size of window on screen
private int  f_w;
private int  f_h;

/** used for funky strobing effect */
private int  lightlev;       
/** pseudo-frame buffer */
private byte[]    fb;             
private int  amclock;

/** (fixed_t) how far the window pans each tic (map coords) */
private mpoint_t m_paninc; 

/** (fixed_t) how far the window zooms in each tic (map coords) */
private int  mtof_zoommul;

/** (fixed_t) how far the window zooms in each tic (fb coords)*/
private int  ftom_zoommul; 

/** (fixed_t) LL x,y where the window is on the map (map coords) */
private int  m_x, m_y;   

/** (fixed_t) UR x,y where the window is on the map (map coords) */
private int  m_x2, m_y2; 

/** (fixed_t) width/height of window on map (map coords) */ 
private int  m_w, m_h;

/** (fixed_t) based on level size */
private int  min_x, min_y, max_x,max_y;

/** (fixed_t) max_x-min_x */
private int  max_w; // 
/** (fixed_t) max_y-min_y */
private int  max_h;

/** (fixed_t) based on player size */
private int  min_w, min_h;

/** (fixed_t) used to tell when to stop zooming out */
private int  min_scale_mtof; 

/** (fixed_t) used to tell when to stop zooming in */
private int  max_scale_mtof; 

/** (fixed_t) old stuff for recovery later */
private int old_m_w, old_m_h, old_m_x, old_m_y;

/** old location used by the Follower routine */
private mpoint_t f_oldloc;

/** (fixed_t) used by MTOF to scale from map-to-frame-buffer coords */
private int scale_mtof = INITSCALEMTOF;
/** used by FTOM to scale from frame-buffer-to-map coords (=1/scale_mtof) */
private int scale_ftom;

/** the player represented by an arrow */
private player_t plr;

/** numbers used for marking by the automap */
private patch_t[] marknums=new patch_t[10]; 
/** where the points are */
private mpoint_t[] markpoints=new mpoint_t[AM_NUMMARKPOINTS]; 

/** next point to be assigned */
private int markpointnum = 0;  

/** specifies whether to follow the player around */
private boolean followplayer = true; 

private char[] cheat_amap_seq = { 0xb2, 0x26, 0x26, 0x2e, 0xff };
private cheatseq_t cheat_amap = new cheatseq_t( cheat_amap_seq, 0 );

private boolean stopped = true;

//extern boolean viewactive;
//extern byte screens[][SCREENWIDTH*SCREENHEIGHT];

/** Calculates the slope and slope according to the x-axis of a line
 *  segment in map coordinates (with the upright y-axis n' all) so
 *  that it can be used with the brain-dead drawing stuff.
 *   
 * @param ml
 * @param is
 */

public void
getIslope
( mline_t  ml,
islope_t is )
{
 int dx, dy;

 dy = ml.a.y - ml.b.y;
 dx = ml.b.x - ml.a.x;
 if (dy==0) is.islp = (dx<0?-MAXINT:MAXINT);
 else is.islp = FixedDiv(dx, dy);
 if (dx==0) is.slp = (dy<0?-MAXINT:MAXINT);
 else is.slp = FixedDiv(dy, dx);

}

//
//
//
public void activateNewScale()
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
public void saveScaleAndLoc()
{
    old_m_x = m_x;
    old_m_y = m_y;
    old_m_w = m_w;
    old_m_h = m_h;
}

public void restoreScaleAndLoc()
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

public void MarkRect
( int   x,
  int   y,
  int   width,
  int   height ){
    
}

/** 
 * adds a marker at the current location
 */

public void addMark()
{
    markpoints[markpointnum].x = m_x + m_w/2;
    markpoints[markpointnum].y = m_y + m_h/2;
    markpointnum = (markpointnum + 1) % AM_NUMMARKPOINTS;

}

/** Determines bounding box of all vertices,
 *  sets global variables controlling zoom range.
 */

public void findMinMaxBoundaries()
{
    int a;
    int b;

    min_x = min_y =  MAXINT;
    max_x = max_y = -MAXINT;
  
    for (int i=0;i<numvertexes;i++)
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


public void changeWindowLoc()
{
    if (m_paninc.x!=0 || m_paninc.y!=0)
    {
    followplayer = false;
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



public void initVariables()
{
    int pnum;

    automapactive = true;
    fb = V.getScreen(0);

    f_oldloc.x = MAXINT;
    amclock = 0;
    lightlev = 0;

    m_paninc.x = m_paninc.y = 0;
    ftom_zoommul = FRACUNIT;
    mtof_zoommul = FRACUNIT;

    m_w = FTOM(f_w);
    m_h = FTOM(f_h);

    // find player to center on initially
    if (!DS.playeringame[pnum = DS.consoleplayer])
    for (pnum=0;pnum<MAXPLAYERS;pnum++)
        if (DS.playeringame[pnum])
        break;
  
    plr = DS.players[pnum];
    m_x = plr.mo.x - m_w/2;
    m_y = plr.mo.y - m_h/2;
    this.changeWindowLoc();

    // for saving & restoring
    old_m_x = m_x;
    old_m_y = m_y;
    old_m_w = m_w;
    old_m_h = m_h;

    // inform the status bar of the change
    ST.Responder(st_notify);

}

//
// 
//
public void loadPics()
{
    int i;
    String namebuf;
  
    for (i=0;i<10;i++)
    {
    namebuf= ("AMMNUM"+ i);
    marknums[i] = W.CachePatchName(namebuf);
    }

}

public void unloadPics()
{
    int i;
  
    for (i=0;i<10;i++){
   // TODO: Z_ChangeTag(marknums[i], PU_CACHE);
    }
}

public void clearMarks()
{
    int i;

    for (i=0;i<AM_NUMMARKPOINTS;i++)
    markpoints[i].x = -1; // means empty
    markpointnum = 0;
}

/**
 * should be called at the start of every level
 * right now, i figure it out myself
 */
public void LevelInit()
{
    leveljuststarted = 0;

    f_x = f_y = 0;
    f_w = finit_width;
    f_h = finit_height;

    this.clearMarks();

    this.findMinMaxBoundaries();
    scale_mtof = FixedDiv(min_scale_mtof, (int) (0.7*FRACUNIT));
    if (scale_mtof > max_scale_mtof)
    scale_mtof = min_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
}

//
//
//

//private event_t st_notify = new event_t(evtype_t.ev_keyup, AM_MSGENTERED );


public void Stop ()
{
    // MAES: Was a "method static variable"...but what's the point? It's never modified.
    event_t st_notify = new event_t( evtype_t.ev_keyup, AM_MSGEXITED );

    this.unloadPics();
    automapactive = false;
    // TODO: could it be modified by the Responder?
    ST.Responder(st_notify);
    stopped = true;
}

//
//
//

// More "static" stuff.
protected int lastlevel = -1, lastepisode = -1;

public void Start ()
{

    if (!stopped) AM_Stop();
    stopped = false;
    if (lastlevel != gamemap || lastepisode != gameepisode)
    {
    this.LevelInit();
    lastlevel = gamemap;
    lastepisode = gameepisode;
    }
    this.initVariables();
    this.loadPics();
}

/**
 * set the window scale to the maximum size
 */
public void minOutWindowScale()
{
    scale_mtof = min_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
    this.activateNewScale();
}

/**
 * set the window scale to the minimum size
 */

public void maxOutWindowScale()
{
    scale_mtof = max_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
    this.activateNewScale();
}


/** These belong to AM_Responder */
protected int cheatstate=0,bigstate=0;

/**     static char buffer[20] in AM_Responder */
protected byte[] buffer=new byte[20];

 /**
 * Handle events (user inputs) in automap mode
 */

public boolean Responder ( event_t  ev )
{

    int rc;

    rc = false;

    if (!automapactive)
    {
    if (ev.type == evtype_t.ev_keydown && ev.data1 == AM_STARTKEY)
    {
        this.Start ();
        DS.viewactive = false;
        rc = true;
    }
    }

    else if (ev.type == evtype_t.ev_keydown)
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
        this.Stop ();
        break;
      case AM_GOBIGKEY:
        bigstate = !bigstate;
        if (bigstate)
        {
        this.saveScaleAndLoc();
        this.minOutWindowScale();
        }
        else this.restoreScaleAndLoc();
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
    if (!deathmatch && cht_CheckCheat(&cheat_amap, ev.data1))
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


/**
 * Zooming
 */
public void changeWindowScale()
{

    // Change the scaling multipliers
    scale_mtof = FixedMul(scale_mtof, mtof_zoommul);
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);

    if (scale_mtof < min_scale_mtof)
    this.minOutWindowScale();
    else if (scale_mtof > max_scale_mtof)
        this.maxOutWindowScale();
    else
        this.activateNewScale();
}


//
//
//
public void doFollowPlayer()
{

    if (f_oldloc.x != plr.mo.x || f_oldloc.y != plr.mo.y)
    {
    m_x = FTOM(MTOF(plr.mo.x)) - m_w/2;
    m_y = FTOM(MTOF(plr.mo.y)) - m_h/2;
    m_x2 = m_x + m_w;
    m_y2 = m_y + m_h;
    f_oldloc.x = plr.mo.x;
    f_oldloc.y = plr.mo.y;

    //  m_x = FTOM(MTOF(plr.mo.x - m_w/2));
    //  m_y = FTOM(MTOF(plr.mo.y - m_h/2));
    //  m_x = plr.mo.x - m_w/2;
    //  m_y = plr.mo.y - m_h/2;

    }

}

//
//
//

protected int nexttic=0;
protected int[] litelevels = { 0, 4, 7, 10, 12, 14, 15, 15 };
protected int litelevelscnt = 0;


public void updateLightLev()
{
   
    // Change light level
    if (amclock>nexttic)
    {
    lightlev = litelevels[litelevelscnt++];
    if (litelevelscnt == litelevels.length) litelevelscnt = 0;
    nexttic = amclock + 6 - (amclock % 6);
    }

}


/**
* Updates on Game Tick
*/
public void Ticker ()
{

    if (!automapactive)
    return;

    amclock++;

    if (followplayer)
    this.doFollowPlayer();

    // Change the zoom if necessary
    if (ftom_zoommul != FRACUNIT)
        this.changeWindowScale();

    // Change x,y location
    if (m_paninc.x || m_paninc.y)
        this.changeWindowLoc();

    // Update light level
    // AM_updateLightLev();

}


//
// Clear automap frame buffer.
//
public void AM_clearFB(int color)
{
    
    // Buffer a whole scanline.
    byte[] scanline=new byte[];
    for (int i=0;i<f_w;i++){
        scanline[i]=(byte)color;
    }
    for (int i=0;i<f_h;i++){
    System.arraycopy(scanline, 0, fb, i*f_w, f_w);
    }
 //   memset(fb, color, f_w*f_h);
}




/**
* Automap clipping of lines.
*
* Based on Cohen-Sutherland clipping algorithm but with a slightly
* faster reject and precalculated slopes.  If the speed is needed,
* use a hash algorithm to handle  the common cases.
*/

public boolean
clipMline
( mline_t ml,
  fline_t fl )
{
    // These were supposed to be "registers", so they exhibit by-ref properties.
    int    outcode1 = 0;
    int    outcode2 = 0;
    int    outside;
    
    fpoint_t    tmp=new fpoint_t();
    int     dx;
    int     dy;

    
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

    outcode1= DOOUTCODE(fl.a.x, fl.a.y);
    outcode2 =DOOUTCODE(fl.b.x, fl.b.y);

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
        outcode1=DOOUTCODE( fl.a.x, fl.a.y);
    }
    else
    {
        fl.b = tmp;
        outcode2=DOOUTCODE(fl.b.x, fl.b.y);
    }
    
    if (outcode1 & outcode2)
        return false; // trivially outside
    }

    return true;
}

protected static int LEFT    =1,
RIGHT   =2,
BOTTOM  =4,
TOP =8;

/**MAES: the result was supposed to be passed in an "oc" parameter by reference.
 * Not convenient, so I made some changes...
 * 
 * @param mx
 * @param my
 */

protected int DOOUTCODE(int mx,int my){
    int oc = 0;
    if ((my) < 0) (oc) |= TOP;
    else if ((my) >= f_h) (oc) |= BOTTOM;
    if ((mx) < 0) (oc) |= LEFT;
    else if ((mx) >= f_w) (oc) |= RIGHT;
    return oc;
}

/** Not my idea ;-) */
protected int fuck = 0;

//
// Classic Bresenham w/ whatever optimizations needed for speed
//
public void drawFline
( fline_t  fl,
  int       color )
{
    
    // MAES: wish they were registers...
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
    System.err.println("fuck "+(fuck++)+" \r");
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
    while (true)
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
    while (true)
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

protected void PUTDOT(int xx,int yy, byte cc) {
    fb[(yy)*f_w+(xx)]=(cc);
}


//
// Clip lines, draw visible part sof lines.
//
public void
drawMline
( mline_t  ml,
  int       color )
{


    if (this.clipMline(ml, fl))
    this.drawFline(fl, color); // draws it on frame buffer using fb coords
}

protected fline_t fl=new fline_t();

/**
 * Draws flat (floor/ceiling tile) aligned grid lines.
 */
public void drawGrid(int color)
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
    AM_drawMline(&ml, color);
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
    AM_drawMline(&ml, color);
    }

}

protected  mline_t l=new mline_t();

/**
 * Determines visible lines, draws them.
 * This is LineDef based, not LineSeg based.
 */

public void drawWalls()
{

    for (int i=0;i<numlines;i++)
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
        AM_drawMline(&l, WALLCOLORS+lightlev);
        }
        else
        {
        if (lines[i].special == 39)
        { // teleporters
            AM_drawMline(&l, WALLCOLORS+WALLRANGE/2);
        }
        else if (lines[i].flags & ML_SECRET) // secret door
        {
            if (cheating) AM_drawMline(&l, SECRETWALLCOLORS + lightlev);
            else AM_drawMline(&l, WALLCOLORS+lightlev);
        }
        else if (lines[i].backsector.floorheight
               != lines[i].frontsector.floorheight) {
            AM_drawMline(&l, FDWALLCOLORS + lightlev); // floor level change
        }
        else if (lines[i].backsector.ceilingheight
               != lines[i].frontsector.ceilingheight) {
            AM_drawMline(&l, CDWALLCOLORS+lightlev); // ceiling level change
        }
        else if (cheating) {
            AM_drawMline(&l, TSWALLCOLORS+lightlev);
        }
        }
    }
    else if (plr.powers[pw_allmap])
    {
        if (!(lines[i].flags & LINE_NEVERSEE)) AM_drawMline(&l, GRAYS+3);
    }
    }
}


//
// Rotation in 2D.
// Used to rotate player arrow line character.
//
void
AM_rotate
( fixed_t*  x,
  fixed_t*  y,
  angle_t   a )
{
    fixed_t tmpx;

    tmpx =
    FixedMul(*x,finecosine[a>>ANGLETOFINESHIFT])
    - FixedMul(*y,finesine[a>>ANGLETOFINESHIFT]);
    
    *y   =
    FixedMul(*x,finesine[a>>ANGLETOFINESHIFT])
    + FixedMul(*y,finecosine[a>>ANGLETOFINESHIFT]);

    *x = tmpx;
}

void
AM_drawLineCharacter
( mline_t*  lineguy,
  int       lineguylines,
  fixed_t   scale,
  angle_t   angle,
  int       color,
  fixed_t   x,
  fixed_t   y )
{
    int     i;
    mline_t l;

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
        AM_rotate(&l.a.x, &l.a.y, angle);

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
        AM_rotate(&l.b.x, &l.b.y, angle);
    
    l.b.x += x;
    l.b.y += y;

    AM_drawMline(&l, color);
    }
}

void AM_drawPlayers(void)
{
    int     i;
    player_t*   p;
    static int  their_colors[] = { GREENS, GRAYS, BROWNS, REDS };
    int     their_color = -1;
    int     color;

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
    p = &players[i];

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

public void drawThings
( int   colors,
  int   colorrange)
{
    int     i;
    mobj_t t;

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

public void AM_drawMarks()
{
    int i, fx, fy, w, h;

    for (i=0;i<AM_NUMMARKPOINTS;i++)
    {
    if (markpoints[i].x != -1)
    {
        //      w = SHORT(marknums[i].width);
        //      h = SHORT(marknums[i].height);
        w = 5; // because something's wrong with the wad, i guess
        h = 6; // because something's wrong with the wad, i guess
        fx = CXMTOF(markpoints[i].x);
        fy = CYMTOF(markpoints[i].y);
        if (fx >= f_x && fx <= f_w - w && fy >= f_y && fy <= f_h - h)
        V_DrawPatch(fx, fy, FB, marknums[i]);
    }
    }

}

public void drawCrosshair(int color)
{
    fb[(f_w*(f_h+1))/2] = color; // single point for now

}

public void Drawer ()
{
    if (!automapactive) return;

    clearFB(BACKGROUND);
    if (grid)
    drawGrid(GRIDCOLORS);
    drawWalls();
    drawPlayers();
    if (cheating==2)
    drawThings(THINGCOLORS, THINGRANGE);
    drawCrosshair(XHAIRCOLORS);

    drawMarks();

    V_MarkRect(f_x, f_y, f_w, f_h);

}

}