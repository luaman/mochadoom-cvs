package data;

//import m.define;
import static data.doomtype.MAXINT;
import static data.doomtype.MININT;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import doom.weapontype_t;
import w.anim_t;
import w.point_t;
import w.animenum_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Defines.java,v 1.9 2010/09/01 15:53:42 velktron Exp $
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
//  Internally used data structures for virtually everything,
//   key definitions, lots of other stuff.
//
//-----------------------------------------------------------------------------

//#ifndef __DOOMDEF__
//#define __DOOMDEF__

//#include <stdio.h>
//#include <string.h>

//
// Global parameters/defines.
//
// DOOM version
public class Defines{
public static int VERSION =  110 ;


// Game mode handling - identify IWAD version
//  to handle IWAD dependend animations etc.
public enum GameMode_t 
{
  shareware,    // DOOM 1 shareware, E1, M9
  registered,   // DOOM 1 registered, E3, M27
  commercial,   // DOOM 2 retail, E1 M34
  // DOOM 2 german edition not handled
  retail,   // DOOM 1 retail, E4, M36
  indetermined  // Well, no IWAD found.
  
} ;


// Mission packs - might be useful for TC stuff?
public enum GameMission_t 
{
  doom,     // DOOM 1
  doom2,    // DOOM 2
  pack_tnt, // TNT mission pack
  pack_plut,    // Plutonia pack
  none

} ;


// Identify language to use, software localization.
public enum Language_t 
{
  english,
  french,
  german,
  unknown

} ;


// If rangecheck is undefined,
// most parameter validation debugging code will not be compiled
//#define RANGECHECK

// Do or do not use external soundserver.
// The sndserver binary to be run separately
//  has been introduced by Dave Taylor.
// The integrated sound support is experimental,
//  and unfinished. Default is synchronous.
// Experimental asynchronous timer based is
//  handled by SNDINTR. 
//#define SNDSERV  1
//#define SNDINTR  1


// This one switches between MIT SHM (no proper mouse)
// and XFree86 DGA (mickey sampling). The original
// linuxdoom used SHM, which is default.
//#define X11_DGA       1


//
// For resize of screen, at start of game.
// It will not work dynamically, see visplanes.
//
public static int BASE_WIDTH =     320;

// It is educational but futile to change this
//  scaling e.g. to 2. Drawing of status bar,
//  menues etc. is tied to the scale implied
//  by the graphics.
public static int SCREEN_MUL =1;
public static double INV_ASPECT_RATIO =   0.625; // 0.75, ideally

// Defines suck. C sucks.
// C++ might sucks for OOP, but it sure is a better C.
// So there.

public static int SCREENWIDTH = 320;
//SCREEN_MUL*BASE_WIDTH //320
public static short SCREENHEIGHT= 200;
//(int)(SCREEN_MUL*BASE_WIDTH*INV_ASPECT_RATIO) //200

// The maximum number of players, multiplayer/networking.
public static int MAXPLAYERS =     4;

// State updates, number of tics / second.
public static int TICRATE = 35;

// The current state of the game: whether we are
// playing, gazing at the intermission screen,
// the game final animation, or a demo. 
public static enum gamestate_t
{
    GS_LEVEL,
    GS_INTERMISSION,
    GS_FINALE,
    GS_DEMOSCREEN,
    GS_MINUS_ONE // hack used for the "-1" state
};

//
// Difficulty/skill settings/filters.
//

// Skill flags.
public static int MTF_EASY =       1;
public static int MTF_NORMAL =     2;
public static int MTF_HARD =       4;

// Deaf monsters/do not react to sound.
public static int MTF_AMBUSH =8;

public static enum skill_t
{
    sk_baby,
    sk_easy,
    sk_medium,
    sk_hard,
    sk_nightmare
};




//
// Key cards.
//
public static enum card_t
{
    it_bluecard,
    it_yellowcard,
    it_redcard,
    it_blueskull,
    it_yellowskull,
    it_redskull,
    
    NUMCARDS
    
} ;

//Maes: this makes it a bit less retarded.
public static final int NUMCARDS=card_t.NUMCARDS.ordinal();


//Maes: this makes it a bit less retarded.
public static final int NUMWEAPONS=weapontype_t.NUMWEAPONS.ordinal();

// Ammunition types defined.
public static enum ammotype_t
{
    am_clip,    // Pistol / chaingun ammo.
    am_shell,   // Shotgun / double barreled shotgun.
    am_cell,    // Plasma rifle, BFG.
    am_misl,    // Missile launcher.
    NUMAMMO,
    am_noammo   // Unlimited for chainsaw / fist.   

} ;

//Maes: this makes it a bit less retarded.
public static final int NUMAMMO=ammotype_t.NUMAMMO.ordinal();



// Power up artifacts.
public static final int pw_invulnerability=0;
public static final int    pw_strength=1;
public static final int    pw_invisibility=2;
public static final int    pw_ironfeet=3;
public static final int    pw_allmap=4;
public static final int    pw_infrared=5;
public static final int    NUMPOWERS=6;


/** Power up durations,
 *  how many seconds till expiration,
 *  assuming TICRATE is 35 ticks/second.
 */
    public static final int INVULNTICS=(30*TICRATE),
    INVISTICS=(60*TICRATE),
    INFRATICS=(120*TICRATE),
    IRONTICS =(60*TICRATE);

//
// DOOM keyboard definition.
// This is the stuff configured by Setup.Exe.
// Most key data are simple ascii (uppercased).
//
// Maes: using chars here instead of bytes.

public final static char KEY_RIGHTARROW  =0xae;
public final static char KEY_LEFTARROW   =0xac;
public final static char KEY_UPARROW =0xad;
public final static char KEY_DOWNARROW   =0xaf;
public final static char KEY_ESCAPE = 27;
public final static char KEY_ENTER =  13;
public final static char KEY_TAB  =   9;
public final static char KEY_F1      =(0x80+0x3b);
public final static char KEY_F2      =(0x80+0x3c);
public final static char KEY_F3      =(0x80+0x3d);
public final static char KEY_F4      =(0x80+0x3e);
public final static char KEY_F5      =(0x80+0x3f);
public final static char KEY_F6      =(0x80+0x40);
public final static char KEY_F7      =(0x80+0x41);
public final static char KEY_F8      =(0x80+0x42);
public final static char KEY_F9      =(0x80+0x43);
public final static char KEY_F10     =(0x80+0x44);
public final static char KEY_F11     =(0x80+0x57);
public final static char KEY_F12     =(0x80+0x58);

public final static char KEY_BACKSPACE  = 127;
public final static char KEY_PAUSE   =0xff;

public final static char KEY_EQUALS  =0x3d;
public final static char KEY_MINUS   =0x2d;

public final static char KEY_RSHIFT = (0x80+0x36);
public final static char KEY_RCTRL  = (0x80+0x1d);
public final static char KEY_RALT   = (0x80+0x38);

public final static char KEY_LALT  =  KEY_RALT;

// from r_defs.h:

//Silhouette, needed for clipping Segs (mainly)
//and sprites representing things.
public static final int SIL_NONE =0;
public static final int SIL_BOTTOM     =1;
public static final int SIL_TOP  =       2;
public static final int SIL_BOTH    =    3;

public static final int MAXDRAWSEGS  =   256;

//
//Move clipping aid for LineDefs.
//
public static enum slopetype_t
{
 ST_HORIZONTAL,
 ST_VERTICAL,
 ST_POSITIVE,
 ST_NEGATIVE

} ;


//Number of diminishing brightness levels.
//There a 0-31, i.e. 32 LUT in the COLORMAP lump.
public static final int  NUMCOLORMAPS   =     32;

//SKY, store the number for name.
static public final String SKYFLATNAME  ="F_SKY1";

// The sky map is 256*128*4 maps.
public static final int ANGLETOSKYSHIFT   = 22;


// From r_draw.c

//?
public static final int  MAXWIDTH      =      1120;
public static final int  MAXHEIGHT      =     832;

// status bar height at bottom of screen
public static final int  SBARHEIGHT     = 32;


// From wi_stuff

public static final int NoState = -1;
public static final int StatCount =0;
public static final int ShowNextLoc=1; 

// From stuff

//
//Data needed to add patches to full screen intermission pics.
//Patches are statistics messages, and animations.
//Loads of by-pixel layout and placement, offsets etc.
//


//
//Different vetween registered DOOM (1994) and
//Ultimate DOOM - Final edition (retail, 1995?).
//This is supposedly ignored for commercial
//release (aka DOOM II), which had 34 maps
//in one episode. So there.
public static final int NUMEPISODES=4;
public static final int NUMMAPS     =9;


//in tics
//U #define PAUSELEN        (TICRATE*2) 
//U #define SCORESTEP       100
//U #define ANIMPERIOD      32
//pixel distance from "(YOU)" to "PLAYER N"
//U #define STARDIST        10 
//U #define WK 1


//GLOBAL LOCATIONS
public static final int WI_TITLEY       =2;
public static final int WI_SPACINGY         =3;

//SINGPLE-PLAYER STUFF
public static final int SP_STATSX       =50;
public static final int SP_STATSY      = 50;

public static final int SP_TIMEX      =  16;
public static final int SP_TIMEY      =  (SCREENHEIGHT-32);


//NET GAME STUFF
public static final int NG_STATSY       =50;


public static final int NG_SPACINGX        = 64;

public static final int    BACKUPTICS     = 12;

//Max computers/players in a game.
public final static int    MAXNETNODES   =  8;

//DEATHMATCH STUFF
public static final int DM_MATRIXX = 42;
public static final int DM_MATRIXY =     68;

public static final int DM_SPACINGX =    40;

public static final int DM_TOTALSX =     269;

public static final int DM_KILLERSX  =   10;
public static final int DM_KILLERSY   =  100;
public static final int DM_VICTIMSX    =     5;
public static final int DM_VICTIMSY    = 50;

// static point_t lnodes[NUMEPISODES][NUMMAPS] 
final static public point_t[][] lnodes =
{
    // Episode 0 World Map
    {
    new point_t( 185, 164 ),   // location of level 0 (CJ)
    new point_t( 148, 143 ),   // location of level 1 new point_t(CJ)
    new point_t( 69, 122 ),    // location of level 2 new point_t(CJ)
    new point_t( 209, 102 ),   // location of level 3 new point_t(CJ)
    new point_t( 116, 89 ),    // location of level 4 new point_t(CJ)
    new point_t( 166, 55 ),    // location of level 5 new point_t(CJ)
    new point_t( 71, 56 ), // location of level 6 new point_t(CJ)
    new point_t( 135, 29 ),    // location of level 7 new point_t(CJ)
    new point_t( 71, 24 )  // location of level 8 new point_t(CJ)
    },

    // Episode 1 World Map should go here
    {
    new point_t( 254, 25 ),    // location of level 0 new point_t(CJ)
    new point_t( 97, 50 ), // location of level 1 new point_t(CJ)
    new point_t( 188, 64 ),    // location of level 2 new point_t(CJ)
    new point_t( 128, 78 ),    // location of level 3 new point_t(CJ)
    new point_t( 214, 92 ),    // location of level 4 new point_t(CJ)
    new point_t( 133, 130 ),   // location of level 5 new point_t(CJ)
    new point_t( 208, 136 ),   // location of level 6 new point_t(CJ)
    new point_t( 148, 140 ),   // location of level 7 new point_t(CJ)
    new point_t( 235, 158 )    // location of level 8 new point_t(CJ)
    },

    // Episode 2 World Map should go here
    {
    new point_t( 156, 168 ),   // location of level 0 new point_t(CJ)
    new point_t( 48, 154 ),    // location of level 1 new point_t(CJ)
    new point_t( 174, 95 ),    // location of level 2 new point_t(CJ)
    new point_t( 265, 75 ),    // location of level 3 new point_t(CJ)
    new point_t( 130, 48 ),    // location of level 4 new point_t(CJ)
    new point_t( 279, 23 ),    // location of level 5 new point_t(CJ)
    new point_t( 198, 48 ),    // location of level 6 new point_t(CJ)
    new point_t( 140, 25 ),    // location of level 7 new point_t(CJ)
    new point_t( 281, 136 )    // location of level 8 new point_t(CJ)
    }

};



//
//Animation locations for episode 0 (1).
//Using patches saves a lot of space,
//as they replace 320x200 full screen frames.
//
static public anim_t[] epsd0animinfo =
{
 new anim_t(animenum_t.ANIM_ALWAYS, TICRATE/3, 3, new point_t( 224, 104 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 184, 160 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 112, 136 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 72, 112 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3, new point_t( 88, 96 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 64, 48 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 192, 40 ) ),
  new anim_t(animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 136, 16 ) ),
  new anim_t(animenum_t. ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 80, 16 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 64, 24 ) )
};

static public anim_t[] epsd1animinfo =
{
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1,  new point_t( 128, 136 ), 1 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1,  new point_t( 128, 136 ), 2 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1,  new point_t( 128, 136 ), 3 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1, new point_t( 128, 136 ), 4 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1,  new point_t(128, 136 ), 5 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1,  new point_t( 128, 136 ), 6 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1,  new point_t( 128, 136 ), 7 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 3, new point_t( 192, 144 ), 8 ),
  new anim_t( animenum_t.ANIM_LEVEL, TICRATE/3, 1,  new point_t( 128, 136 ), 8 )
};

static public anim_t[] epsd2animinfo =
{
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 104, 168 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 40, 136 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t( 160, 96 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3,  new point_t(104, 80 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/3, 3, new point_t( 120, 32 ) ),
  new anim_t( animenum_t.ANIM_ALWAYS, TICRATE/4, 3,  new point_t( 40, 0 ) )
};

/*static int NUMANIMS[NUMEPISODES] =
{
 sizeof(epsd0animinfo)/sizeof(anim_t),
 sizeof(epsd1animinfo)/sizeof(anim_t),
 sizeof(epsd2animinfo)/sizeof(anim_t)
};*/

// MAES: cute, but we can do it in a more Java-friendly way :-p

public static int[] NUMANIMS={epsd0animinfo.length,epsd1animinfo.length,epsd2animinfo.length};

public static anim_t[][] anims =
{
 epsd0animinfo,
 epsd1animinfo,
 epsd2animinfo
};


// From Zone:

//
//ZONE MEMORY
//PU - purge tags.
//Tags < 100 are not overwritten until freed.
public static final int PU_STATIC   =    1;   // static entire execution time
public static final int PU_SOUND    =    2;   // static while playing
public static final int PU_MUSIC   =     3;   // static while playing
public static final int PU_DAVE   =  4;   // anything else Dave wants static
public static final int PU_LEVEL   =     50;  // static until level exited
public static final int PU_LEVSPEC  =    51;    // a special thinker in a level
//Tags >= 100 are purgable whenever needed.
public static final int PU_PURGELEVEL =  100;
public static final int PU_CACHE     =   101;


// From hu_lib.h:

//background and foreground screen numbers
//different from other modules.
public static final int BG     =     1;
public static final int FG     =     0;

//font stuff
static public final char HU_CHARERASE  =  KEY_BACKSPACE;

public static final int HU_MAXLINES  =   4;
public static final int HU_MAXLINELENGTH  =  80;

// From hu_stuff.h

//
//Globally visible constants.
//
static public final byte HU_FONTSTART  =  '!'; // the first font characters
static public final byte HU_FONTEND  ='_'; // the last font characters

//Calculate # of glyphs in font.
public static final int HU_FONTSIZE = (HU_FONTEND - HU_FONTSTART + 1); 

static public final char HU_BROADCAST   = 5;

static public final char HU_MSGREFRESH =  KEY_ENTER;
static public final char HU_MSGX     =0;
static public final char HU_MSGY     =0;
static public final char HU_MSGWIDTH =64;  // in characters
static public final char HU_MSGHEIGHT  =  1;   // in lines

public static final int HU_MSGTIMEOUT =  (4*TICRATE);

public static final int SAVESTRINGSIZE = 24;

//
// Button/action code definitions.
// From d_event.h

     // Press "Fire".
    public static final int BT_ATTACK       = 1;
     // Use button, to open doors, activate switches.
    public static final int BT_USE      = 2;

     // Flag: game events, not really buttons.
    public static final int BT_SPECIAL      = 128;
    public static final int BT_SPECIALMASK  = 3;
     
     // Flag, weapon change pending.
     // If true, the next 3 bits hold weapon num.
    public static final int  BT_CHANGE       = 4;
     // The 3bit weapon mask and shift, convenience.
    public static final int  BT_WEAPONMASK   = (8+16+32);
    public static final int   BT_WEAPONSHIFT  = 3;

     // Pause the game.
    public static final int  BTS_PAUSE       = 1;
     // Save the game at each console.
    public static final int  BTS_SAVEGAME    = 2;

     // Savegame slot numbers
     //  occupy the second byte of buttons.    
    public static final int BTS_SAVEMASK    = (4+8+16);
    public static final int BTS_SAVESHIFT   = 2;
   
    public static final int MAXEVENTS       =64;
    
    
    //==================== Stuff from r_local.c =========================================

    
    public static int FLOATSPEED        =(FRACUNIT*4);

    public static int MAXHEALTH     =100;

    public static int VIEWHEIGHT    =   (41*FRACUNIT);

    // mapblocks are used to check movement
    // against lines and things
    public static int MAPBLOCKUNITS=    128;
    public static int MAPBLOCKSIZE  =(MAPBLOCKUNITS*FRACUNIT);
    public static int MAPBLOCKSHIFT =(FRACBITS+7);
    public static int MAPBMASK      =(MAPBLOCKSIZE-1);
    public static int MAPBTOFRAC=       (MAPBLOCKSHIFT-FRACBITS);


    // player radius for movement checking
    public static int PLAYERRADIUS  =16*FRACUNIT;

    // MAXRADIUS is for precalculated sector block boxes
    // the spider demon is larger,
    // but we do not have any moving sectors nearby
    public static int MAXRADIUS =   32*FRACUNIT;

    public static int GRAVITY   =   FRACUNIT;
    public static int MAXMOVE   =   (30*FRACUNIT);

    public static int USERANGE      =(64*FRACUNIT);
    public static int MELEERANGE    =   (64*FRACUNIT);
    public static int MISSILERANGE=(32*64*FRACUNIT);

    // follow a player exlusively for 3 seconds
    public static int   BASETHRESHOLD=      100;

    public static int MAXINTERCEPTS   =128;
    
    public static int PT_ADDLINES     =1;
    public static int PT_ADDTHINGS    =2;
    public static int PT_EARLYOUT     =4;
    
 //
 // P_MOBJ
 //
 public static int ONFLOORZ  =   MININT;
 public static int ONCEILINGZ    =   MAXINT;

 // Time interval for item respawning.
 public static int ITEMQUESIZE       =128;
    
    
    
    //
 // GLOBAL VARIABLES
 //
 //#define MAXEVENTS       64

 //extern  event_t     events[MAXEVENTS];
 //extern  int             eventhead;
 //extern  int     eventtail;

 //extern  gameaction_t    gameaction;
    
// DOOM basic types (boolean),
//  and max/min values.
//#include "doomtype.h"

// Fixed point.
//#include "m_fixed.h"

// Endianess handling.
//#include "m_swap.h"


// Binary Angles, sine/cosine/atan lookups.
//#include "tables.h"

// Event type.
//#include "d_event.h"

// Game function, skills.
//#include "g_game.h"

// All external data is defined here.
//#include "doomdata.h"

// All important printed strings.
// Language selection (message strings).
//#include "dstrings.h"

// Player is a special actor.
//struct player_s;


//#include "d_items.h"
//#include "d_player.h"
//#include "p_mobj.h"
//#include "d_net.h"

// PLAY
//#include "p_tick.h"

 public static enum statenum_t {
	    S_NULL,
	    S_LIGHTDONE,
	    S_PUNCH,
	    S_PUNCHDOWN,
	    S_PUNCHUP,
	    S_PUNCH1,
	    S_PUNCH2,
	    S_PUNCH3,
	    S_PUNCH4,
	    S_PUNCH5,
	    S_PISTOL,
	    S_PISTOLDOWN,
	    S_PISTOLUP,
	    S_PISTOL1,
	    S_PISTOL2,
	    S_PISTOL3,
	    S_PISTOL4,
	    S_PISTOLFLASH,
	    S_SGUN,
	    S_SGUNDOWN,
	    S_SGUNUP,
	    S_SGUN1,
	    S_SGUN2,
	    S_SGUN3,
	    S_SGUN4,
	    S_SGUN5,
	    S_SGUN6,
	    S_SGUN7,
	    S_SGUN8,
	    S_SGUN9,
	    S_SGUNFLASH1,
	    S_SGUNFLASH2,
	    S_DSGUN,
	    S_DSGUNDOWN,
	    S_DSGUNUP,
	    S_DSGUN1,
	    S_DSGUN2,
	    S_DSGUN3,
	    S_DSGUN4,
	    S_DSGUN5,
	    S_DSGUN6,
	    S_DSGUN7,
	    S_DSGUN8,
	    S_DSGUN9,
	    S_DSGUN10,
	    S_DSNR1,
	    S_DSNR2,
	    S_DSGUNFLASH1,
	    S_DSGUNFLASH2,
	    S_CHAIN,
	    S_CHAINDOWN,
	    S_CHAINUP,
	    S_CHAIN1,
	    S_CHAIN2,
	    S_CHAIN3,
	    S_CHAINFLASH1,
	    S_CHAINFLASH2,
	    S_MISSILE,
	    S_MISSILEDOWN,
	    S_MISSILEUP,
	    S_MISSILE1,
	    S_MISSILE2,
	    S_MISSILE3,
	    S_MISSILEFLASH1,
	    S_MISSILEFLASH2,
	    S_MISSILEFLASH3,
	    S_MISSILEFLASH4,
	    S_SAW,
	    S_SAWB,
	    S_SAWDOWN,
	    S_SAWUP,
	    S_SAW1,
	    S_SAW2,
	    S_SAW3,
	    S_PLASMA,
	    S_PLASMADOWN,
	    S_PLASMAUP,
	    S_PLASMA1,
	    S_PLASMA2,
	    S_PLASMAFLASH1,
	    S_PLASMAFLASH2,
	    S_BFG,
	    S_BFGDOWN,
	    S_BFGUP,
	    S_BFG1,
	    S_BFG2,
	    S_BFG3,
	    S_BFG4,
	    S_BFGFLASH1,
	    S_BFGFLASH2,
	    S_BLOOD1,
	    S_BLOOD2,
	    S_BLOOD3,
	    S_PUFF1,
	    S_PUFF2,
	    S_PUFF3,
	    S_PUFF4,
	    S_TBALL1,
	    S_TBALL2,
	    S_TBALLX1,
	    S_TBALLX2,
	    S_TBALLX3,
	    S_RBALL1,
	    S_RBALL2,
	    S_RBALLX1,
	    S_RBALLX2,
	    S_RBALLX3,
	    S_PLASBALL,
	    S_PLASBALL2,
	    S_PLASEXP,
	    S_PLASEXP2,
	    S_PLASEXP3,
	    S_PLASEXP4,
	    S_PLASEXP5,
	    S_ROCKET,
	    S_BFGSHOT,
	    S_BFGSHOT2,
	    S_BFGLAND,
	    S_BFGLAND2,
	    S_BFGLAND3,
	    S_BFGLAND4,
	    S_BFGLAND5,
	    S_BFGLAND6,
	    S_BFGEXP,
	    S_BFGEXP2,
	    S_BFGEXP3,
	    S_BFGEXP4,
	    S_EXPLODE1,
	    S_EXPLODE2,
	    S_EXPLODE3,
	    S_TFOG,
	    S_TFOG01,
	    S_TFOG02,
	    S_TFOG2,
	    S_TFOG3,
	    S_TFOG4,
	    S_TFOG5,
	    S_TFOG6,
	    S_TFOG7,
	    S_TFOG8,
	    S_TFOG9,
	    S_TFOG10,
	    S_IFOG,
	    S_IFOG01,
	    S_IFOG02,
	    S_IFOG2,
	    S_IFOG3,
	    S_IFOG4,
	    S_IFOG5,
	    S_PLAY,
	    S_PLAY_RUN1,
	    S_PLAY_RUN2,
	    S_PLAY_RUN3,
	    S_PLAY_RUN4,
	    S_PLAY_ATK1,
	    S_PLAY_ATK2,
	    S_PLAY_PAIN,
	    S_PLAY_PAIN2,
	    S_PLAY_DIE1,
	    S_PLAY_DIE2,
	    S_PLAY_DIE3,
	    S_PLAY_DIE4,
	    S_PLAY_DIE5,
	    S_PLAY_DIE6,
	    S_PLAY_DIE7,
	    S_PLAY_XDIE1,
	    S_PLAY_XDIE2,
	    S_PLAY_XDIE3,
	    S_PLAY_XDIE4,
	    S_PLAY_XDIE5,
	    S_PLAY_XDIE6,
	    S_PLAY_XDIE7,
	    S_PLAY_XDIE8,
	    S_PLAY_XDIE9,
	    S_POSS_STND,
	    S_POSS_STND2,
	    S_POSS_RUN1,
	    S_POSS_RUN2,
	    S_POSS_RUN3,
	    S_POSS_RUN4,
	    S_POSS_RUN5,
	    S_POSS_RUN6,
	    S_POSS_RUN7,
	    S_POSS_RUN8,
	    S_POSS_ATK1,
	    S_POSS_ATK2,
	    S_POSS_ATK3,
	    S_POSS_PAIN,
	    S_POSS_PAIN2,
	    S_POSS_DIE1,
	    S_POSS_DIE2,
	    S_POSS_DIE3,
	    S_POSS_DIE4,
	    S_POSS_DIE5,
	    S_POSS_XDIE1,
	    S_POSS_XDIE2,
	    S_POSS_XDIE3,
	    S_POSS_XDIE4,
	    S_POSS_XDIE5,
	    S_POSS_XDIE6,
	    S_POSS_XDIE7,
	    S_POSS_XDIE8,
	    S_POSS_XDIE9,
	    S_POSS_RAISE1,
	    S_POSS_RAISE2,
	    S_POSS_RAISE3,
	    S_POSS_RAISE4,
	    S_SPOS_STND,
	    S_SPOS_STND2,
	    S_SPOS_RUN1,
	    S_SPOS_RUN2,
	    S_SPOS_RUN3,
	    S_SPOS_RUN4,
	    S_SPOS_RUN5,
	    S_SPOS_RUN6,
	    S_SPOS_RUN7,
	    S_SPOS_RUN8,
	    S_SPOS_ATK1,
	    S_SPOS_ATK2,
	    S_SPOS_ATK3,
	    S_SPOS_PAIN,
	    S_SPOS_PAIN2,
	    S_SPOS_DIE1,
	    S_SPOS_DIE2,
	    S_SPOS_DIE3,
	    S_SPOS_DIE4,
	    S_SPOS_DIE5,
	    S_SPOS_XDIE1,
	    S_SPOS_XDIE2,
	    S_SPOS_XDIE3,
	    S_SPOS_XDIE4,
	    S_SPOS_XDIE5,
	    S_SPOS_XDIE6,
	    S_SPOS_XDIE7,
	    S_SPOS_XDIE8,
	    S_SPOS_XDIE9,
	    S_SPOS_RAISE1,
	    S_SPOS_RAISE2,
	    S_SPOS_RAISE3,
	    S_SPOS_RAISE4,
	    S_SPOS_RAISE5,
	    S_VILE_STND,
	    S_VILE_STND2,
	    S_VILE_RUN1,
	    S_VILE_RUN2,
	    S_VILE_RUN3,
	    S_VILE_RUN4,
	    S_VILE_RUN5,
	    S_VILE_RUN6,
	    S_VILE_RUN7,
	    S_VILE_RUN8,
	    S_VILE_RUN9,
	    S_VILE_RUN10,
	    S_VILE_RUN11,
	    S_VILE_RUN12,
	    S_VILE_ATK1,
	    S_VILE_ATK2,
	    S_VILE_ATK3,
	    S_VILE_ATK4,
	    S_VILE_ATK5,
	    S_VILE_ATK6,
	    S_VILE_ATK7,
	    S_VILE_ATK8,
	    S_VILE_ATK9,
	    S_VILE_ATK10,
	    S_VILE_ATK11,
	    S_VILE_HEAL1,
	    S_VILE_HEAL2,
	    S_VILE_HEAL3,
	    S_VILE_PAIN,
	    S_VILE_PAIN2,
	    S_VILE_DIE1,
	    S_VILE_DIE2,
	    S_VILE_DIE3,
	    S_VILE_DIE4,
	    S_VILE_DIE5,
	    S_VILE_DIE6,
	    S_VILE_DIE7,
	    S_VILE_DIE8,
	    S_VILE_DIE9,
	    S_VILE_DIE10,
	    S_FIRE1,
	    S_FIRE2,
	    S_FIRE3,
	    S_FIRE4,
	    S_FIRE5,
	    S_FIRE6,
	    S_FIRE7,
	    S_FIRE8,
	    S_FIRE9,
	    S_FIRE10,
	    S_FIRE11,
	    S_FIRE12,
	    S_FIRE13,
	    S_FIRE14,
	    S_FIRE15,
	    S_FIRE16,
	    S_FIRE17,
	    S_FIRE18,
	    S_FIRE19,
	    S_FIRE20,
	    S_FIRE21,
	    S_FIRE22,
	    S_FIRE23,
	    S_FIRE24,
	    S_FIRE25,
	    S_FIRE26,
	    S_FIRE27,
	    S_FIRE28,
	    S_FIRE29,
	    S_FIRE30,
	    S_SMOKE1,
	    S_SMOKE2,
	    S_SMOKE3,
	    S_SMOKE4,
	    S_SMOKE5,
	    S_TRACER,
	    S_TRACER2,
	    S_TRACEEXP1,
	    S_TRACEEXP2,
	    S_TRACEEXP3,
	    S_SKEL_STND,
	    S_SKEL_STND2,
	    S_SKEL_RUN1,
	    S_SKEL_RUN2,
	    S_SKEL_RUN3,
	    S_SKEL_RUN4,
	    S_SKEL_RUN5,
	    S_SKEL_RUN6,
	    S_SKEL_RUN7,
	    S_SKEL_RUN8,
	    S_SKEL_RUN9,
	    S_SKEL_RUN10,
	    S_SKEL_RUN11,
	    S_SKEL_RUN12,
	    S_SKEL_FIST1,
	    S_SKEL_FIST2,
	    S_SKEL_FIST3,
	    S_SKEL_FIST4,
	    S_SKEL_MISS1,
	    S_SKEL_MISS2,
	    S_SKEL_MISS3,
	    S_SKEL_MISS4,
	    S_SKEL_PAIN,
	    S_SKEL_PAIN2,
	    S_SKEL_DIE1,
	    S_SKEL_DIE2,
	    S_SKEL_DIE3,
	    S_SKEL_DIE4,
	    S_SKEL_DIE5,
	    S_SKEL_DIE6,
	    S_SKEL_RAISE1,
	    S_SKEL_RAISE2,
	    S_SKEL_RAISE3,
	    S_SKEL_RAISE4,
	    S_SKEL_RAISE5,
	    S_SKEL_RAISE6,
	    S_FATSHOT1,
	    S_FATSHOT2,
	    S_FATSHOTX1,
	    S_FATSHOTX2,
	    S_FATSHOTX3,
	    S_FATT_STND,
	    S_FATT_STND2,
	    S_FATT_RUN1,
	    S_FATT_RUN2,
	    S_FATT_RUN3,
	    S_FATT_RUN4,
	    S_FATT_RUN5,
	    S_FATT_RUN6,
	    S_FATT_RUN7,
	    S_FATT_RUN8,
	    S_FATT_RUN9,
	    S_FATT_RUN10,
	    S_FATT_RUN11,
	    S_FATT_RUN12,
	    S_FATT_ATK1,
	    S_FATT_ATK2,
	    S_FATT_ATK3,
	    S_FATT_ATK4,
	    S_FATT_ATK5,
	    S_FATT_ATK6,
	    S_FATT_ATK7,
	    S_FATT_ATK8,
	    S_FATT_ATK9,
	    S_FATT_ATK10,
	    S_FATT_PAIN,
	    S_FATT_PAIN2,
	    S_FATT_DIE1,
	    S_FATT_DIE2,
	    S_FATT_DIE3,
	    S_FATT_DIE4,
	    S_FATT_DIE5,
	    S_FATT_DIE6,
	    S_FATT_DIE7,
	    S_FATT_DIE8,
	    S_FATT_DIE9,
	    S_FATT_DIE10,
	    S_FATT_RAISE1,
	    S_FATT_RAISE2,
	    S_FATT_RAISE3,
	    S_FATT_RAISE4,
	    S_FATT_RAISE5,
	    S_FATT_RAISE6,
	    S_FATT_RAISE7,
	    S_FATT_RAISE8,
	    S_CPOS_STND,
	    S_CPOS_STND2,
	    S_CPOS_RUN1,
	    S_CPOS_RUN2,
	    S_CPOS_RUN3,
	    S_CPOS_RUN4,
	    S_CPOS_RUN5,
	    S_CPOS_RUN6,
	    S_CPOS_RUN7,
	    S_CPOS_RUN8,
	    S_CPOS_ATK1,
	    S_CPOS_ATK2,
	    S_CPOS_ATK3,
	    S_CPOS_ATK4,
	    S_CPOS_PAIN,
	    S_CPOS_PAIN2,
	    S_CPOS_DIE1,
	    S_CPOS_DIE2,
	    S_CPOS_DIE3,
	    S_CPOS_DIE4,
	    S_CPOS_DIE5,
	    S_CPOS_DIE6,
	    S_CPOS_DIE7,
	    S_CPOS_XDIE1,
	    S_CPOS_XDIE2,
	    S_CPOS_XDIE3,
	    S_CPOS_XDIE4,
	    S_CPOS_XDIE5,
	    S_CPOS_XDIE6,
	    S_CPOS_RAISE1,
	    S_CPOS_RAISE2,
	    S_CPOS_RAISE3,
	    S_CPOS_RAISE4,
	    S_CPOS_RAISE5,
	    S_CPOS_RAISE6,
	    S_CPOS_RAISE7,
	    S_TROO_STND,
	    S_TROO_STND2,
	    S_TROO_RUN1,
	    S_TROO_RUN2,
	    S_TROO_RUN3,
	    S_TROO_RUN4,
	    S_TROO_RUN5,
	    S_TROO_RUN6,
	    S_TROO_RUN7,
	    S_TROO_RUN8,
	    S_TROO_ATK1,
	    S_TROO_ATK2,
	    S_TROO_ATK3,
	    S_TROO_PAIN,
	    S_TROO_PAIN2,
	    S_TROO_DIE1,
	    S_TROO_DIE2,
	    S_TROO_DIE3,
	    S_TROO_DIE4,
	    S_TROO_DIE5,
	    S_TROO_XDIE1,
	    S_TROO_XDIE2,
	    S_TROO_XDIE3,
	    S_TROO_XDIE4,
	    S_TROO_XDIE5,
	    S_TROO_XDIE6,
	    S_TROO_XDIE7,
	    S_TROO_XDIE8,
	    S_TROO_RAISE1,
	    S_TROO_RAISE2,
	    S_TROO_RAISE3,
	    S_TROO_RAISE4,
	    S_TROO_RAISE5,
	    S_SARG_STND,
	    S_SARG_STND2,
	    S_SARG_RUN1,
	    S_SARG_RUN2,
	    S_SARG_RUN3,
	    S_SARG_RUN4,
	    S_SARG_RUN5,
	    S_SARG_RUN6,
	    S_SARG_RUN7,
	    S_SARG_RUN8,
	    S_SARG_ATK1,
	    S_SARG_ATK2,
	    S_SARG_ATK3,
	    S_SARG_PAIN,
	    S_SARG_PAIN2,
	    S_SARG_DIE1,
	    S_SARG_DIE2,
	    S_SARG_DIE3,
	    S_SARG_DIE4,
	    S_SARG_DIE5,
	    S_SARG_DIE6,
	    S_SARG_RAISE1,
	    S_SARG_RAISE2,
	    S_SARG_RAISE3,
	    S_SARG_RAISE4,
	    S_SARG_RAISE5,
	    S_SARG_RAISE6,
	    S_HEAD_STND,
	    S_HEAD_RUN1,
	    S_HEAD_ATK1,
	    S_HEAD_ATK2,
	    S_HEAD_ATK3,
	    S_HEAD_PAIN,
	    S_HEAD_PAIN2,
	    S_HEAD_PAIN3,
	    S_HEAD_DIE1,
	    S_HEAD_DIE2,
	    S_HEAD_DIE3,
	    S_HEAD_DIE4,
	    S_HEAD_DIE5,
	    S_HEAD_DIE6,
	    S_HEAD_RAISE1,
	    S_HEAD_RAISE2,
	    S_HEAD_RAISE3,
	    S_HEAD_RAISE4,
	    S_HEAD_RAISE5,
	    S_HEAD_RAISE6,
	    S_BRBALL1,
	    S_BRBALL2,
	    S_BRBALLX1,
	    S_BRBALLX2,
	    S_BRBALLX3,
	    S_BOSS_STND,
	    S_BOSS_STND2,
	    S_BOSS_RUN1,
	    S_BOSS_RUN2,
	    S_BOSS_RUN3,
	    S_BOSS_RUN4,
	    S_BOSS_RUN5,
	    S_BOSS_RUN6,
	    S_BOSS_RUN7,
	    S_BOSS_RUN8,
	    S_BOSS_ATK1,
	    S_BOSS_ATK2,
	    S_BOSS_ATK3,
	    S_BOSS_PAIN,
	    S_BOSS_PAIN2,
	    S_BOSS_DIE1,
	    S_BOSS_DIE2,
	    S_BOSS_DIE3,
	    S_BOSS_DIE4,
	    S_BOSS_DIE5,
	    S_BOSS_DIE6,
	    S_BOSS_DIE7,
	    S_BOSS_RAISE1,
	    S_BOSS_RAISE2,
	    S_BOSS_RAISE3,
	    S_BOSS_RAISE4,
	    S_BOSS_RAISE5,
	    S_BOSS_RAISE6,
	    S_BOSS_RAISE7,
	    S_BOS2_STND,
	    S_BOS2_STND2,
	    S_BOS2_RUN1,
	    S_BOS2_RUN2,
	    S_BOS2_RUN3,
	    S_BOS2_RUN4,
	    S_BOS2_RUN5,
	    S_BOS2_RUN6,
	    S_BOS2_RUN7,
	    S_BOS2_RUN8,
	    S_BOS2_ATK1,
	    S_BOS2_ATK2,
	    S_BOS2_ATK3,
	    S_BOS2_PAIN,
	    S_BOS2_PAIN2,
	    S_BOS2_DIE1,
	    S_BOS2_DIE2,
	    S_BOS2_DIE3,
	    S_BOS2_DIE4,
	    S_BOS2_DIE5,
	    S_BOS2_DIE6,
	    S_BOS2_DIE7,
	    S_BOS2_RAISE1,
	    S_BOS2_RAISE2,
	    S_BOS2_RAISE3,
	    S_BOS2_RAISE4,
	    S_BOS2_RAISE5,
	    S_BOS2_RAISE6,
	    S_BOS2_RAISE7,
	    S_SKULL_STND,
	    S_SKULL_STND2,
	    S_SKULL_RUN1,
	    S_SKULL_RUN2,
	    S_SKULL_ATK1,
	    S_SKULL_ATK2,
	    S_SKULL_ATK3,
	    S_SKULL_ATK4,
	    S_SKULL_PAIN,
	    S_SKULL_PAIN2,
	    S_SKULL_DIE1,
	    S_SKULL_DIE2,
	    S_SKULL_DIE3,
	    S_SKULL_DIE4,
	    S_SKULL_DIE5,
	    S_SKULL_DIE6,
	    S_SPID_STND,
	    S_SPID_STND2,
	    S_SPID_RUN1,
	    S_SPID_RUN2,
	    S_SPID_RUN3,
	    S_SPID_RUN4,
	    S_SPID_RUN5,
	    S_SPID_RUN6,
	    S_SPID_RUN7,
	    S_SPID_RUN8,
	    S_SPID_RUN9,
	    S_SPID_RUN10,
	    S_SPID_RUN11,
	    S_SPID_RUN12,
	    S_SPID_ATK1,
	    S_SPID_ATK2,
	    S_SPID_ATK3,
	    S_SPID_ATK4,
	    S_SPID_PAIN,
	    S_SPID_PAIN2,
	    S_SPID_DIE1,
	    S_SPID_DIE2,
	    S_SPID_DIE3,
	    S_SPID_DIE4,
	    S_SPID_DIE5,
	    S_SPID_DIE6,
	    S_SPID_DIE7,
	    S_SPID_DIE8,
	    S_SPID_DIE9,
	    S_SPID_DIE10,
	    S_SPID_DIE11,
	    S_BSPI_STND,
	    S_BSPI_STND2,
	    S_BSPI_SIGHT,
	    S_BSPI_RUN1,
	    S_BSPI_RUN2,
	    S_BSPI_RUN3,
	    S_BSPI_RUN4,
	    S_BSPI_RUN5,
	    S_BSPI_RUN6,
	    S_BSPI_RUN7,
	    S_BSPI_RUN8,
	    S_BSPI_RUN9,
	    S_BSPI_RUN10,
	    S_BSPI_RUN11,
	    S_BSPI_RUN12,
	    S_BSPI_ATK1,
	    S_BSPI_ATK2,
	    S_BSPI_ATK3,
	    S_BSPI_ATK4,
	    S_BSPI_PAIN,
	    S_BSPI_PAIN2,
	    S_BSPI_DIE1,
	    S_BSPI_DIE2,
	    S_BSPI_DIE3,
	    S_BSPI_DIE4,
	    S_BSPI_DIE5,
	    S_BSPI_DIE6,
	    S_BSPI_DIE7,
	    S_BSPI_RAISE1,
	    S_BSPI_RAISE2,
	    S_BSPI_RAISE3,
	    S_BSPI_RAISE4,
	    S_BSPI_RAISE5,
	    S_BSPI_RAISE6,
	    S_BSPI_RAISE7,
	    S_ARACH_PLAZ,
	    S_ARACH_PLAZ2,
	    S_ARACH_PLEX,
	    S_ARACH_PLEX2,
	    S_ARACH_PLEX3,
	    S_ARACH_PLEX4,
	    S_ARACH_PLEX5,
	    S_CYBER_STND,
	    S_CYBER_STND2,
	    S_CYBER_RUN1,
	    S_CYBER_RUN2,
	    S_CYBER_RUN3,
	    S_CYBER_RUN4,
	    S_CYBER_RUN5,
	    S_CYBER_RUN6,
	    S_CYBER_RUN7,
	    S_CYBER_RUN8,
	    S_CYBER_ATK1,
	    S_CYBER_ATK2,
	    S_CYBER_ATK3,
	    S_CYBER_ATK4,
	    S_CYBER_ATK5,
	    S_CYBER_ATK6,
	    S_CYBER_PAIN,
	    S_CYBER_DIE1,
	    S_CYBER_DIE2,
	    S_CYBER_DIE3,
	    S_CYBER_DIE4,
	    S_CYBER_DIE5,
	    S_CYBER_DIE6,
	    S_CYBER_DIE7,
	    S_CYBER_DIE8,
	    S_CYBER_DIE9,
	    S_CYBER_DIE10,
	    S_PAIN_STND,
	    S_PAIN_RUN1,
	    S_PAIN_RUN2,
	    S_PAIN_RUN3,
	    S_PAIN_RUN4,
	    S_PAIN_RUN5,
	    S_PAIN_RUN6,
	    S_PAIN_ATK1,
	    S_PAIN_ATK2,
	    S_PAIN_ATK3,
	    S_PAIN_ATK4,
	    S_PAIN_PAIN,
	    S_PAIN_PAIN2,
	    S_PAIN_DIE1,
	    S_PAIN_DIE2,
	    S_PAIN_DIE3,
	    S_PAIN_DIE4,
	    S_PAIN_DIE5,
	    S_PAIN_DIE6,
	    S_PAIN_RAISE1,
	    S_PAIN_RAISE2,
	    S_PAIN_RAISE3,
	    S_PAIN_RAISE4,
	    S_PAIN_RAISE5,
	    S_PAIN_RAISE6,
	    S_SSWV_STND,
	    S_SSWV_STND2,
	    S_SSWV_RUN1,
	    S_SSWV_RUN2,
	    S_SSWV_RUN3,
	    S_SSWV_RUN4,
	    S_SSWV_RUN5,
	    S_SSWV_RUN6,
	    S_SSWV_RUN7,
	    S_SSWV_RUN8,
	    S_SSWV_ATK1,
	    S_SSWV_ATK2,
	    S_SSWV_ATK3,
	    S_SSWV_ATK4,
	    S_SSWV_ATK5,
	    S_SSWV_ATK6,
	    S_SSWV_PAIN,
	    S_SSWV_PAIN2,
	    S_SSWV_DIE1,
	    S_SSWV_DIE2,
	    S_SSWV_DIE3,
	    S_SSWV_DIE4,
	    S_SSWV_DIE5,
	    S_SSWV_XDIE1,
	    S_SSWV_XDIE2,
	    S_SSWV_XDIE3,
	    S_SSWV_XDIE4,
	    S_SSWV_XDIE5,
	    S_SSWV_XDIE6,
	    S_SSWV_XDIE7,
	    S_SSWV_XDIE8,
	    S_SSWV_XDIE9,
	    S_SSWV_RAISE1,
	    S_SSWV_RAISE2,
	    S_SSWV_RAISE3,
	    S_SSWV_RAISE4,
	    S_SSWV_RAISE5,
	    S_KEENSTND,
	    S_COMMKEEN,
	    S_COMMKEEN2,
	    S_COMMKEEN3,
	    S_COMMKEEN4,
	    S_COMMKEEN5,
	    S_COMMKEEN6,
	    S_COMMKEEN7,
	    S_COMMKEEN8,
	    S_COMMKEEN9,
	    S_COMMKEEN10,
	    S_COMMKEEN11,
	    S_COMMKEEN12,
	    S_KEENPAIN,
	    S_KEENPAIN2,
	    S_BRAIN,
	    S_BRAIN_PAIN,
	    S_BRAIN_DIE1,
	    S_BRAIN_DIE2,
	    S_BRAIN_DIE3,
	    S_BRAIN_DIE4,
	    S_BRAINEYE,
	    S_BRAINEYESEE,
	    S_BRAINEYE1,
	    S_SPAWN1,
	    S_SPAWN2,
	    S_SPAWN3,
	    S_SPAWN4,
	    S_SPAWNFIRE1,
	    S_SPAWNFIRE2,
	    S_SPAWNFIRE3,
	    S_SPAWNFIRE4,
	    S_SPAWNFIRE5,
	    S_SPAWNFIRE6,
	    S_SPAWNFIRE7,
	    S_SPAWNFIRE8,
	    S_BRAINEXPLODE1,
	    S_BRAINEXPLODE2,
	    S_BRAINEXPLODE3,
	    S_ARM1,
	    S_ARM1A,
	    S_ARM2,
	    S_ARM2A,
	    S_BAR1,
	    S_BAR2,
	    S_BEXP,
	    S_BEXP2,
	    S_BEXP3,
	    S_BEXP4,
	    S_BEXP5,
	    S_BBAR1,
	    S_BBAR2,
	    S_BBAR3,
	    S_BON1,
	    S_BON1A,
	    S_BON1B,
	    S_BON1C,
	    S_BON1D,
	    S_BON1E,
	    S_BON2,
	    S_BON2A,
	    S_BON2B,
	    S_BON2C,
	    S_BON2D,
	    S_BON2E,
	    S_BKEY,
	    S_BKEY2,
	    S_RKEY,
	    S_RKEY2,
	    S_YKEY,
	    S_YKEY2,
	    S_BSKULL,
	    S_BSKULL2,
	    S_RSKULL,
	    S_RSKULL2,
	    S_YSKULL,
	    S_YSKULL2,
	    S_STIM,
	    S_MEDI,
	    S_SOUL,
	    S_SOUL2,
	    S_SOUL3,
	    S_SOUL4,
	    S_SOUL5,
	    S_SOUL6,
	    S_PINV,
	    S_PINV2,
	    S_PINV3,
	    S_PINV4,
	    S_PSTR,
	    S_PINS,
	    S_PINS2,
	    S_PINS3,
	    S_PINS4,
	    S_MEGA,
	    S_MEGA2,
	    S_MEGA3,
	    S_MEGA4,
	    S_SUIT,
	    S_PMAP,
	    S_PMAP2,
	    S_PMAP3,
	    S_PMAP4,
	    S_PMAP5,
	    S_PMAP6,
	    S_PVIS,
	    S_PVIS2,
	    S_CLIP,
	    S_AMMO,
	    S_ROCK,
	    S_BROK,
	    S_CELL,
	    S_CELP,
	    S_SHEL,
	    S_SBOX,
	    S_BPAK,
	    S_BFUG,
	    S_MGUN,
	    S_CSAW,
	    S_LAUN,
	    S_PLAS,
	    S_SHOT,
	    S_SHOT2,
	    S_COLU,
	    S_STALAG,
	    S_BLOODYTWITCH,
	    S_BLOODYTWITCH2,
	    S_BLOODYTWITCH3,
	    S_BLOODYTWITCH4,
	    S_DEADTORSO,
	    S_DEADBOTTOM,
	    S_HEADSONSTICK,
	    S_GIBS,
	    S_HEADONASTICK,
	    S_HEADCANDLES,
	    S_HEADCANDLES2,
	    S_DEADSTICK,
	    S_LIVESTICK,
	    S_LIVESTICK2,
	    S_MEAT2,
	    S_MEAT3,
	    S_MEAT4,
	    S_MEAT5,
	    S_STALAGTITE,
	    S_TALLGRNCOL,
	    S_SHRTGRNCOL,
	    S_TALLREDCOL,
	    S_SHRTREDCOL,
	    S_CANDLESTIK,
	    S_CANDELABRA,
	    S_SKULLCOL,
	    S_TORCHTREE,
	    S_BIGTREE,
	    S_TECHPILLAR,
	    S_EVILEYE,
	    S_EVILEYE2,
	    S_EVILEYE3,
	    S_EVILEYE4,
	    S_FLOATSKULL,
	    S_FLOATSKULL2,
	    S_FLOATSKULL3,
	    S_HEARTCOL,
	    S_HEARTCOL2,
	    S_BLUETORCH,
	    S_BLUETORCH2,
	    S_BLUETORCH3,
	    S_BLUETORCH4,
	    S_GREENTORCH,
	    S_GREENTORCH2,
	    S_GREENTORCH3,
	    S_GREENTORCH4,
	    S_REDTORCH,
	    S_REDTORCH2,
	    S_REDTORCH3,
	    S_REDTORCH4,
	    S_BTORCHSHRT,
	    S_BTORCHSHRT2,
	    S_BTORCHSHRT3,
	    S_BTORCHSHRT4,
	    S_GTORCHSHRT,
	    S_GTORCHSHRT2,
	    S_GTORCHSHRT3,
	    S_GTORCHSHRT4,
	    S_RTORCHSHRT,
	    S_RTORCHSHRT2,
	    S_RTORCHSHRT3,
	    S_RTORCHSHRT4,
	    S_HANGNOGUTS,
	    S_HANGBNOBRAIN,
	    S_HANGTLOOKDN,
	    S_HANGTSKULL,
	    S_HANGTLOOKUP,
	    S_HANGTNOBRAIN,
	    S_COLONGIBS,
	    S_SMALLPOOL,
	    S_BRAINSTEM,
	    S_TECHLAMP,
	    S_TECHLAMP2,
	    S_TECHLAMP3,
	    S_TECHLAMP4,
	    S_TECH2LAMP,
	    S_TECH2LAMP2,
	    S_TECH2LAMP3,
	    S_TECH2LAMP4,
	    NUMSTATES;

	    
	};
	
	// From "doomdata.h"
	
	   //
    // Map level types.
    // The following data structures define the persistent format
    // used in the lumps of the WAD files.
    //

    // Lump order in a map WAD: each map needs a couple of lumps
    // to provide a complete scene geometry description.

    public static final int ML_LABEL = 0;

    /** A separator, name, ExMx or MAPxx */
    public static final int ML_THINGS = 1;

    /** Monsters, items.. */
    public static final int ML_LINEDEFS = 2;

    /** LineDefs, from editing */
    public static final int ML_SIDEDEFS = 3;

    /** SideDefs, from editing */
    public static final int ML_VERTEXES = 4;

    /** Vertices, edited and BSP splits generated */
    public static final int ML_SEGS = 5;

    /** LineSegs, from LineDefs split by BSP */
    public static final int ML_SSECTORS = 6;

    /** SubSectors, list of LineSegs */
    public static final int ML_NODES = 7;

    /** BSP nodes */
    public static final int ML_SECTORS = 8;

    /** Sectors, from editing */
    public static final int ML_REJECT = 9;

    /** LUT, sector-sector visibility */
    public static final int ML_BLOCKMAP = 10;

    /**
     * LUT, motion clipping, walls/grid element // // LineDef attributes. // /**
     * Solid, is an obstacle.
     */
    public static final int ML_BLOCKING = 1;

    /** Blocks monsters only. */
    public static final int ML_BLOCKMONSTERS = 2;

    /** Backside will not be present at all if not two sided. */
    public static final int ML_TWOSIDED = 4;

    // If a texture is pegged, the texture will have
    // the end exposed to air held constant at the
    // top or bottom of the texture (stairs or pulled
    // down things) and will move with a height change
    // of one of the neighbor sectors.
    // Unpegged textures allways have the first row of
    // the texture at the top pixel of the line for both
    // top and bottom textures (use next to windows).

    /** upper texture unpegged */
    public static final int ML_DONTPEGTOP = 8;

    /** lower texture unpegged */
    public static final int ML_DONTPEGBOTTOM = 16;

    /** In AutoMap: don't map as two sided: IT'S A SECRET! */
    public static final int ML_SECRET = 32;

    /** Sound rendering: don't let sound cross two of these. */
    public static final int ML_SOUNDBLOCK = 64;

    /** Don't draw on the automap at all. */
    public static final int ML_DONTDRAW = 128;

    /** Set if already seen, thus drawn in automap. */
    public static final int ML_MAPPED = 256;

    /** Indicate a leaf. */
    public static int NF_SUBSECTOR = 0x8000;
	
    
    /** Player states. */
    
    public static int   PST_LIVE=0,     // Playing or camping.    
        PST_DEAD=1,        // Dead on the ground, view follows killer.

        PST_REBORN=2;            // Ready to restart/respawn???
    
 
static final String
rcsid = "$Id: Defines.java,v 1.9 2010/09/01 15:53:42 velktron Exp $";
}
