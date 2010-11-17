package automap;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Map.java,v 1.19 2010/11/17 23:55:06 velktron Exp $
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
// Revision 1.19  2010/11/17 23:55:06  velktron
// Kind of playable/controllable.
//
// Revision 1.18  2010/11/12 13:37:25  velktron
// Rationalized the LUT system - now it's 100% procedurally generated.
//
// Revision 1.17  2010/10/01 16:47:51  velktron
// Fixed tab interception.
//
// Revision 1.16  2010/09/27 15:07:44  velktron
// meh
//
// Revision 1.15  2010/09/27 02:27:29  velktron
// BEASTLY update
//
// Revision 1.14  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.13  2010/09/13 15:39:17  velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.12  2010/09/08 21:09:01  velktron
// Better display "driver".
//
// Revision 1.11  2010/09/08 15:22:18  velktron
// x,y coords in some structs as value semantics. Possible speed increase?
//
// Revision 1.10  2010/09/06 16:02:59  velktron
// Implementation of palettes.
//
// Revision 1.9  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.8  2010/09/01 15:53:42  velktron
// Graphics data loader implemented....still need to figure out how column caching works, though.
//
// Revision 1.7  2010/08/27 23:46:57  velktron
// Introduced Buffered renderer, which makes tapping directly into byte[] screen buffers mapped to BufferedImages possible.
//
// Revision 1.6  2010/08/26 16:43:42  velktron
// Automap functional, biatch.
//
// Revision 1.5  2010/08/25 00:50:59  velktron
// Some more work...
//
// Revision 1.4  2010/08/22 18:04:21  velktron
// Automap
//
// Revision 1.3  2010/08/19 23:14:49  velktron
// Automap
//
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
import static data.Limits.*;
import static m.fixed_t.*;
import static doom.englsh.*;
import static data.Tables.*;
import p.LevelLoader;
import p.mobj_t;
import doom.DoomContext;
import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;
import rr.patch_t;
import st.DoomStatusBarInterface;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import w.WadLoader;
import m.cheatseq_t;

public class Map implements DoomAutoMap{

/////////////////// Status objects ///////////////////
    
DoomStatusBarInterface ST;
WadLoader W;
DoomMain DM;
DoomVideoRenderer V;
LevelLoader LL;    
    
    
public final String rcsid = "$Id: Map.java,v 1.19 2010/11/17 23:55:06 velktron Exp $";

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

public static final char AM_PANDOWNKEY  =0xaf;
public static final char AM_PANUPKEY    =0xad;
public static final char AM_PANRIGHTKEY =0xae;
public static final char AM_PANLEFTKEY  =0xac;
public static final char AM_ZOOMINKEY=  '=';
public static final char AM_ZOOMOUTKEY= '-';
public static final char AM_STARTKEY=   9;	// KEY_TAB
public static final char AM_ENDKEY  =9;	// KEY_TAB
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

public Map(DoomContext DC) {

    this.updateStatus(DC);
    // Some initializing...
    this.markpoints=new mpoint_t[AM_NUMMARKPOINTS];
    C2JUtils.initArrayOfObjects(markpoints, mpoint_t.class);
    
    f_oldloc=new mpoint_t();
    m_paninc=new mpoint_t();
    
    this.plr=DM.players[DM.displayplayer];
    this.scanline=new byte[V.getHeight()*V.getWidth()];
}


/** translates between frame-buffer and map distances */
private final int FTOM(int x){return FixedMul(((x)<<16),scale_ftom);}
/** translates between frame-buffer and map distances */
private final int MTOF(int x) {return FixedMul((x),scale_mtof)>>16;}
/** translates between frame-buffer and map coordinates */
private final int CXMTOF(int x){ return (f_x + MTOF((x)-m_x));}
/** translates between frame-buffer and map coordinates */
private final int CYMTOF(int y) {return (f_y + (f_h - MTOF((y)-m_y)));}

// the following is crap
public static final short LINE_NEVERSEE =ML_DONTDRAW;

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

public void initVectorGraphics(){

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

protected int  cheating = 0;
protected boolean  grid = false;

private int  leveljuststarted = 1;   // kluge until AM_LevelInit() is called

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
private mpoint_t[] markpoints; 

/** next point to be assigned */
private int markpointnum = 0;  

/** specifies whether to follow the player around */
private boolean followplayer = true; 

private char[] cheat_amap_seq = { 0xb2, 0x26, 0x26, 0x2e, 0xff }; // iddt
private cheatseq_t cheat_amap = new cheatseq_t( cheat_amap_seq, 0 );

// MAES: STROBE cheat. It's not even cheating, strictly speaking.

private char cheat_strobe_seq[] =
{ 0x6e, 0xa6, 0xea, 0x2e, 0x6a, 0xf6, 0x62,0xa6, 0xff // vestrobe
};

private cheatseq_t cheat_strobe = new cheatseq_t( cheat_strobe_seq, 0 );


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

public final  void
getIslope
( mline_t  ml,
islope_t is )
{
 int dx, dy;

 dy = ml.ay - ml.by;
 dx = ml.bx - ml.ax;
 if (dy==0) is.islp = (dx<0?-MAXINT:MAXINT);
 else is.islp = FixedDiv(dx, dy);
 if (dx==0) is.slp = (dy<0?-MAXINT:MAXINT);
 else is.slp = FixedDiv(dy, dx);

}

//
//
//
public final  void activateNewScale()
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
public final  void saveScaleAndLoc()
{
    old_m_x = m_x;
    old_m_y = m_y;
    old_m_w = m_w;
    old_m_h = m_h;
}

private void restoreScaleAndLoc()
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

/** 
 * adds a marker at the current location
 */

public final  void addMark()
{
    markpoints[markpointnum].x = m_x + m_w/2;
    markpoints[markpointnum].y = m_y + m_h/2;
    markpointnum = (markpointnum + 1) % AM_NUMMARKPOINTS;

}

/** Determines bounding box of all vertices,
 *  sets global variables controlling zoom range.
 */

public final  void findMinMaxBoundaries()
{
    int a; // fixed_t
    int b;

    min_x = min_y =  MAXINT;
    max_x = max_y = -MAXINT;
  
    for (int i=0;i<LL.numvertexes;i++)
    {
    if (LL.vertexes[i].x < min_x)
        min_x = LL.vertexes[i].x;
    else if (LL.vertexes[i].x > max_x)
        max_x = LL.vertexes[i].x;
    
    if (LL.vertexes[i].y < min_y)
        min_y = LL.vertexes[i].y;
    else if (LL.vertexes[i].y > max_y)
        max_y = LL.vertexes[i].y;
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


public final  void changeWindowLoc()
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



public final  void initVariables()
{
    int pnum=0;

    DM.automapactive = true;
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
    if (!DM.playeringame[pnum = DM.consoleplayer])
    for (pnum=0;pnum<MAXPLAYERS;pnum++){
    	System.out.println(pnum);
        if (DM.playeringame[pnum])
        break;
    	}
    plr = DM.players[pnum];
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
public final  void loadPics()
{
    int i;
    String namebuf;
  
    for (i=0;i<10;i++)
    {
    namebuf= ("AMMNUM"+ i);
    marknums[i] = W.CachePatchName(namebuf);
    }

}

public final  void unloadPics()
{
    int i;
  
    for (i=0;i<10;i++){
   // TODO: Z_ChangeTag(marknums[i], PU_CACHE);
    }
}

public final  void clearMarks()
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
public final  void LevelInit()
{
    leveljuststarted = 0;

    f_x = f_y = 0;
    f_w = finit_width;
    f_h = finit_height;
    
    scanline=new byte[f_h*f_w];

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

public final  event_t st_notify = new event_t(evtype_t.ev_keyup, AM_MSGENTERED );


public final  void Stop ()
{
    // MAES: Was a "method static variable"...but what's the point? It's never modified.
    event_t st_notify_ex = new event_t( evtype_t.ev_keyup, AM_MSGEXITED );

    this.unloadPics();
    DM.automapactive = false;
    // This is the only way to notify the status bar responder that we're exiting the automap.
    ST.Responder(st_notify_ex);
    stopped = true;
}

//
//
//

// More "static" stuff.
protected int lastlevel = -1, lastepisode = -1;

public final  void Start ()
{

    if (!stopped) Stop();
    stopped = false;
    if (lastlevel != DM.gamemap || lastepisode != DM.gameepisode)
    {
    this.LevelInit();
    lastlevel = DM.gamemap;
    lastepisode = DM.gameepisode;
    }
    this.initVectorGraphics();
    this.LevelInit();
    this.initVariables();
    this.loadPics();
}

/**
 * set the window scale to the maximum size
 */
public final  void minOutWindowScale()
{
    scale_mtof = min_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
    this.activateNewScale();
}

/**
 * set the window scale to the minimum size
 */

public final  void maxOutWindowScale()
{
    scale_mtof = max_scale_mtof;
    scale_ftom = FixedDiv(FRACUNIT, scale_mtof);
    this.activateNewScale();
}


/** These belong to AM_Responder */
protected boolean cheatstate=false,bigstate=false;

/**     static char buffer[20] in AM_Responder */
protected String buffer;

/** MAES: brought back strobe effect */
private boolean strobe=false;

 /**
 * Handle events (user inputs) in automap mode
 */

public final  boolean Responder ( event_t  ev )
{

    boolean rc;

    rc = false;

    //System.out.println(ev.data1==AM_STARTKEY);
    if (!DM.automapactive)
    {
    if (ev.data1 == AM_STARTKEY)
    {
        this.Start ();
        System.out.println("Automap started");
        DM.viewactive = false;
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
        bigstate = false;
        DM.viewactive = true;
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
        buffer=(AMSTR_MARKEDSPOT+" "+ markpointnum);
        plr.message = buffer;
        this.addMark();
        break;
      case AM_CLEARMARKKEY:
        this.clearMarks();
        plr.message = AMSTR_MARKSCLEARED;
        break;
      default:
        cheatstate=false;
        rc = false;
    }
    if (!DM.deathmatch && cheat_amap.CheckCheat((char) ev.data1))
    {
        rc = false;
        cheating = (cheating+1) % 3;
    }
    if (cheat_strobe.CheckCheat((char) ev.data1))
    {
        strobe=!strobe;
    }
    }

    else if (ev.type == evtype_t.ev_keyup)
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
private final void changeWindowScale()
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
private final  void doFollowPlayer()
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


private final  void updateLightLev()
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
public final  void Ticker ()
{

    if (!DM.automapactive)
    return;

    amclock++;

    if (followplayer)
    this.doFollowPlayer();

    // Change the zoom if necessary
    if (ftom_zoommul != FRACUNIT)
        this.changeWindowScale();

    // Change x,y location
    if ((m_paninc.x | m_paninc.y)!=0)
        this.changeWindowLoc();

    // Update light level
    if (strobe)updateLightLev();

}

//private static int BUFFERSIZE=f_h*f_w;


private int lastcolor=-1;

/**
 * Clear automap frame buffer.
 * MAES: optimized for efficiency, seen the lack of a proper "memset" in Java.
 * 
 */

private byte[] scanline;

public final  void clearFB(byte color)
{
    if (lastcolor==-1 || lastcolor !=color){
    // Buffer a whole scanline with the appropriate color.
    
    for (int i=0;i<scanline.length;i++){
        scanline[i]=color;
    }
    lastcolor=color;
    }
    /*
    for (int i=1;i<(f_h*f_w)/BUFFERSIZE;i++){
    System.arraycopy(fb, (i-1)*BUFFERSIZE, fb, i*BUFFERSIZE, BUFFERSIZE);
    }*/
    System.arraycopy(scanline, 0, fb, 0, f_h*f_w);
    
 //   memset(fb, color, f_w*f_h);
}




/**
* Automap clipping of lines.
*
* Based on Cohen-Sutherland clipping algorithm but with a slightly
* faster reject and precalculated slopes.  If the speed is needed,
* use a hash algorithm to handle  the common cases.
*/
private int    tmpx,tmpy;//=new fpoint_t();

private final  boolean
clipMline
( mline_t ml
  ,fline_t fl)
{
    
    //System.out.print("Asked to clip from "+FixedFloat.toFloat(ml.a.x)+","+FixedFloat.toFloat(ml.a.y));
   // System.out.print(" to clip "+FixedFloat.toFloat(ml.b.x)+","+FixedFloat.toFloat(ml.b.y)+"\n");
    // These were supposed to be "registers", so they exhibit by-ref properties.
    int    outcode1 = 0;
    int    outcode2 = 0;
    int    outside;
    

    int     dx;
    int     dy;
    /*fl.a.x=0;
    fl.a.y=0;
    fl.b.x=0;
    fl.b.y=0; */
    
    // do trivial rejects and outcodes
    if (ml.ay > m_y2)
    outcode1 = TOP;
    else if (ml.ay < m_y)
    outcode1 = BOTTOM;

    if (ml.by > m_y2)
    outcode2 = TOP;
    else if (ml.by < m_y)
    outcode2 = BOTTOM;
    
    if ((outcode1 & outcode2)!=0)
    return false; // trivially outside

    if (ml.ax < m_x)
    outcode1 |= LEFT;
    else if (ml.ax > m_x2)
    outcode1 |= RIGHT;
    
    if (ml.bx < m_x)
    outcode2 |= LEFT;
    else if (ml.bx > m_x2)
    outcode2 |= RIGHT;
    
    if ((outcode1 & outcode2)!=0)
    return false; // trivially outside

    // transform to frame-buffer coordinates.
    fl.ax = CXMTOF(ml.ax);
    fl.ay = CYMTOF(ml.ay);
    fl.bx = CXMTOF(ml.bx);
    fl.by = CYMTOF(ml.by);

    //System.out.println(">>>>>> ("+fl.a.x+" , "+fl.a.y+" ),("+fl.b.x+" , "+fl.b.y+" )");
    outcode1= DOOUTCODE(fl.ax, fl.ay);
    outcode2 =DOOUTCODE(fl.bx, fl.by);

    if ((outcode1 & outcode2)!=0)
    return false;

    while ((outcode1 | outcode2)!=0)
    {
    // may be partially inside box
    // find an outside point
    if (outcode1!=0)
        outside = outcode1;
    else
        outside = outcode2;
    
    // clip to each side
    if ((outside & TOP)!=0)
    {
        dy = fl.ay - fl.by;
        dx = fl.bx - fl.ax;
        tmpx = fl.ax + (dx*(fl.ay))/dy;
        tmpy = 0;
    }
    else if ((outside & BOTTOM)!=0)
    {
        dy = fl.ay - fl.by;
        dx = fl.bx - fl.ax;
        tmpx = fl.ax + (dx*(fl.ay-f_h))/dy;
        tmpy = f_h-1;
    }
    else if ((outside & RIGHT)!=0)
    {
        dy = fl.by - fl.ay;
        dx = fl.bx - fl.ax;
        tmpy = fl.ay + (dy*(f_w-1 - fl.ax))/dx;
        tmpx = f_w-1;
    }
    else if ((outside & LEFT)!=0)
    {
        dy = fl.by - fl.ay;
        dx = fl.bx - fl.ax;
        tmpy = fl.ay + (dy*(-fl.ax))/dx;
        tmpx = 0;
    }

    if (outside == outcode1)
    {
        fl.ax = tmpx;
        fl.ay=tmpy;
        outcode1=DOOUTCODE( fl.ax, fl.ay);
    }
    else
    {
        fl.bx = tmpx;
        fl.by = tmpy;
        outcode2=DOOUTCODE(fl.bx, fl.by);
    }
    
    if ((outcode1 & outcode2)!=0)
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

private final int DOOUTCODE(int mx,int my){
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
private final  void drawFline
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

 /*   if (      fl.a.x < 0 || fl.a.x >= f_w
       || fl.a.y < 0 || fl.a.y >= f_h
       || fl.b.x < 0 || fl.b.x >= f_w
       || fl.b.y < 0 || fl.b.y >= f_h)
=======
    /*if (      fl.ax < 0 || fl.ax >= f_w
       || fl.ay < 0 || fl.ay >= f_h
       || fl.bx < 0 || fl.bx >= f_w
       || fl.by < 0 || fl.by >= f_h)
>>>>>>> 1.11
    {
    System.err.println("fuck "+(fuck++)+" \r");
    return;
<<<<<<< Map.java
    } */


    dx = fl.bx - fl.ax;
    ax = 2 * (dx<0 ? -dx : dx);
    sx = dx<0 ? -1 : 1;

    dy = fl.by - fl.ay;
    ay = 2 * (dy<0 ? -dy : dy);
    sy = dy<0 ? -1 : 1;

    x = fl.ax;
    y = fl.ay;
    byte c=(byte)color;
    
    if (ax > ay)
    {
    d = ay - ax/2;
    

    
    while (true)
    {
        PUTDOT(x,y,c);
        if (x == fl.bx) return;
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
        PUTDOT(x, y, c);
        if (y == fl.by) return;
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

private final void PUTDOT(int xx,int yy, byte cc) {
    fb[(yy)*f_w+(xx)]=(cc);
}


/**
 * Clip lines, draw visible parts of lines.
 */
protected int singlepixel=0;

private final  void
drawMline
( mline_t  ml,
  int       color )
{

   //fl.reset();

    if (this.clipMline(ml,fl)){
      //  if ((fl.a.x==fl.b.x)&&(fl.a.y==fl.b.y)) singlepixel++;
    this.drawFline(fl, color); // draws it on frame buffer using fb coords
    }
}

private final fline_t fl=new fline_t();
private final mline_t ml=new mline_t();

/**

 * Draws flat (floor/ceiling tile) aligned grid lines.
 */
private final  void drawGrid(int color)
{
    int x, y; // fixed_t
    int start, end; // fixed_t


    // Figure out start of vertical gridlines
    start = m_x;
    if (((start-LL.bmaporgx)%(MAPBLOCKUNITS<<FRACBITS))!=0)
    start += (MAPBLOCKUNITS<<FRACBITS)
        - ((start-LL.bmaporgx)%(MAPBLOCKUNITS<<FRACBITS));
    end = m_x + m_w;

    // draw vertical gridlines
    ml.ay = m_y;
    ml.by = m_y+m_h;
    for (x=start; x<end; x+=(MAPBLOCKUNITS<<FRACBITS))
    {
    ml.ax = x;
    ml.bx = x;
    drawMline(ml, color);
    }

    // Figure out start of horizontal gridlines
    start = m_y;
    if (((start-LL.bmaporgy)%(MAPBLOCKUNITS<<FRACBITS))!=0)
    start += (MAPBLOCKUNITS<<FRACBITS)
        - ((start-LL.bmaporgy)%(MAPBLOCKUNITS<<FRACBITS));
    end = m_y + m_h;

    // draw horizontal gridlines
    ml.ax = m_x;
    ml.bx = m_x + m_w;
    for (y=start; y<end; y+=(MAPBLOCKUNITS<<FRACBITS))
    {
    ml.ay = y;
    ml.by = y;
    drawMline(ml, color);
    }

}

protected  mline_t l=new mline_t();

/**
 * Determines visible lines, draws them.
 * This is LineDef based, not LineSeg based.
 */

private final  void drawWalls()
{

    for (int i=0;i<LL.numlines;i++)
    {
    l.ax = LL.lines[i].v1x;
    l.ay = LL.lines[i].v1y;
    l.bx = LL.lines[i].v2x;
    l.by = LL.lines[i].v2y;
    if ((cheating | (LL.lines[i].flags & ML_MAPPED))!=0)
    {
        if (((LL.lines[i].flags & LINE_NEVERSEE) & ~cheating)!=0)
        continue;
        if (LL.lines[i].backsector==null)
        {
        drawMline(l, WALLCOLORS+lightlev);
        }
        else
        {
        if (LL.lines[i].special == 39)
        { // teleporters
            drawMline(l, WALLCOLORS+WALLRANGE/2);
        }
        else if ((LL.lines[i].flags & ML_SECRET)!=0) // secret door
        {
            if (cheating!=0) drawMline(l, SECRETWALLCOLORS + lightlev);
            else drawMline(l, WALLCOLORS+lightlev);
        }
        else if (LL.lines[i].backsector.floorheight
               != LL.lines[i].frontsector.floorheight) {
            drawMline(l, FDWALLCOLORS + lightlev); // floor level change
        }
        else if (LL.lines[i].backsector.ceilingheight
               != LL.lines[i].frontsector.ceilingheight) {
            drawMline(l, CDWALLCOLORS+lightlev); // ceiling level change
        }
        else if (cheating!=0) {
            drawMline(l, TSWALLCOLORS+lightlev);
        }
        }
    }
    // If we have allmap...
    else if (plr.powers[pw_allmap]!=0)
    {
    	// Some are never seen even with that!
        if ((LL.lines[i].flags & LINE_NEVERSEE)==0) drawMline(l, GRAYS+3);
    }
    }
    
    //System.out.println("Single pixel draws: "+singlepixel+" out of "+P.lines.length);
    //singlepixel=0;
}


//
// Rotation in 2D.
// Used to rotate player arrow line character.
//
private int rotx, roty; 

/**
* Rotation in 2D.
* Used to rotate player arrow line character.
* 
 * @param x fixed_t
 * @param y fixed_t
 * @param a angle_t -> this should be a LUT-ready BAM.
 */

private final  void rotate
( int  x,
  int y,
  int   a )
{
    //int tmpx;

    rotx =
    FixedMul(x,finecosine[a])
    - FixedMul(y,finesine[a]);
    
    roty   =
    FixedMul(x,finesine[a])
    + FixedMul(y,finecosine[a]);

    //rotx.val = tmpx;
}

private final void
drawLineCharacter
( mline_t[]  lineguy,
  int       lineguylines,
  int   scale, // fixed_t
  int   angle, // This should be a LUT-ready angle.
  int       color,
  int   x, // fixed_t
  int   y // fixed_t
  )
{
    int     i;
    mline_t l=new mline_t();

    for (i=0;i<lineguylines;i++)
    {
    l.ax = lineguy[i].ax;
    l.ay = lineguy[i].ay;

    if (scale!=0)
    {
        l.ax = FixedMul(scale, l.ax);
        l.ay = FixedMul(scale, l.ay);
    }
    
   // System.out.print("BAM Angle "+angle+ " for "+FixedFloat.toFloat(l.ax)+ " "+FixedFloat.toFloat(l.ay));

    if (angle!=0)
        rotate(l.ax, l.ay, angle);
//    System.out.print(" rotated by "+90*(angle/ANG90)+"\n" );

    
    // MAES: assign rotations
    	l.ax=rotx;
    	l.ay=roty;

    l.ax += x;
    l.ay += y;

    l.bx = lineguy[i].bx;
    l.by = lineguy[i].by;

    if (scale!=0)
    {
        l.bx = FixedMul(scale, l.bx);
        l.by = FixedMul(scale, l.by);
    }

    if (angle!=0)
        rotate(l.bx, l.by, angle);
    // MAES: assign rotations
	l.bx=rotx;
	l.by=roty;

    
    l.bx += x;
    l.by += y;

    drawMline(l, color);
    }
}

protected static int  their_colors[] = { GREENS, GRAYS, BROWNS, REDS };

public final  void drawPlayers()
{
    player_t   p;
    
    int     their_color = -1;
    int     color;

    //System.out.println(Long.toHexString(plr.mo.angle));
    
    if (!DM.netgame)
    {
    if (cheating!=0)
        drawLineCharacter
        (cheat_player_arrow, NUMCHEATPLYRLINES, 0,
          toBAMIndex((int)plr.mo.angle), WHITE, plr.mo.x, plr.mo.y);
    else
        drawLineCharacter
        (player_arrow, NUMPLYRLINES, 0,  toBAMIndex((int)plr.mo.angle),
         WHITE, plr.mo.x, plr.mo.y);
    return;
    }

    for (int i=0;i<MAXPLAYERS;i++)
    {
    their_color++;
    p = DM.players[i];

    if ( (DM.deathmatch && !DM.singledemo) && p != plr)
        continue;

    if (!DM.playeringame[i])
        continue;

    if (p.powers[pw_invisibility]!=0)
        color = 246; // *close* to black
    else
        color = their_colors[their_color];
    
    drawLineCharacter
        (player_arrow, NUMPLYRLINES, 0, (int) p.mo.angle,
         color, p.mo.x, p.mo.y);
    }

}

public final  void drawThings
( int   colors,
  int   colorrange)
{
    mobj_t t;

    for (int i=0;i<LL.numsectors;i++)
    {
    // MAES: get first on the list.
    t = LL.sectors[i].thinglist;
    while(t!=null)
    {
        drawLineCharacter
        (thintriangle_guy, NUMTHINTRIANGLEGUYLINES,
         16<<FRACBITS, toBAMIndex(t.angle), colors+lightlev, t.x, t.y);
        t = (mobj_t)t.snext;
    }
    }
}

public final  void drawMarks()
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
        V.DrawPatch(fx, fy, FB, marknums[i]);
    }
    }

}

public final  void drawCrosshair(int color)
{
    fb[(f_w*(f_h+1))/2] = (byte)color; // single point for now

}

public final  void Drawer ()
{
    if (!DM.automapactive) return;
    //System.out.println("Drawing map");
    clearFB((byte)BACKGROUND); // BACKGROUND
    if (grid)
    drawGrid(GRIDCOLORS);
    drawWalls();
    drawPlayers();
    //if (cheating==2)
    drawThings(THINGCOLORS, THINGRANGE);
    drawCrosshair(XHAIRCOLORS);

    drawMarks();

    V.MarkRect(f_x, f_y, f_w, f_h);

}


@Override
public void updateStatus(DoomContext DC) {
    this.V=DC.V;
    this.W=DC.W;
    this.LL=DC.LL;
    this.DM=DC.DM;
    this.ST=DC.ST;    
}

}