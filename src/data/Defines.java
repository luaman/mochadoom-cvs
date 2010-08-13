package data;

//import m.define;
import static data.doomtype.MAXINT;
import static data.doomtype.MININT;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import w.anim_t;
import w.point_t;
import w.animenum_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Defines.java,v 1.4 2010/08/13 14:06:36 velktron Exp $
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

// The defined weapons,
//  including a marker indicating
//  user has not changed weapon.
public static enum weapontype_t
{
    wp_fist,
    wp_pistol,
    wp_shotgun,
    wp_chaingun,
    wp_missile,
    wp_plasma,
    wp_bfg,
    wp_chainsaw,
    wp_supershotgun,

    NUMWEAPONS,
    
    // No pending weapon change.
    wp_nochange
};
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
public static int pw_invulnerability=0;
public static int    pw_strength=1;
public static int    pw_invisibility=2;
public static int    pw_ironfeet=3;
public static int    pw_allmap=4;
public static int    pw_infrared=5;
public static int    NUMPOWERS=6;


//
// Power up durations,
//  how many seconds till expiration,
//  assuming TICRATE is 35 ticks/second.
//
public enum powerduration_t
{
    INVULNTICS(30*TICRATE),
    INVISTICS(60*TICRATE),
    INFRATICS(120*TICRATE),
    IRONTICS (60*TICRATE);

    powerduration_t(int val) {
            duration = val;
        }

        protected int duration;

        public int getDuration() {
            return duration;
        }
    
};




//
// DOOM keyboard definition.
// This is the stuff configured by Setup.Exe.
// Most key data are simple ascii (uppercased).
//
// Maes: using chars here instead of bytes.

public static char KEY_RIGHTARROW  =0xae;
public static char KEY_LEFTARROW   =0xac;
public static char KEY_UPARROW =0xad;
public static char KEY_DOWNARROW   =0xaf;
public static char KEY_ESCAPE = 27;
public static char KEY_ENTER =  13;
public static char KEY_TAB  =   9;
public static char KEY_F1      =(0x80+0x3b);
public static char KEY_F2      =(0x80+0x3c);
public static char KEY_F3      =(0x80+0x3d);
public static char KEY_F4      =(0x80+0x3e);
public static char KEY_F5      =(0x80+0x3f);
public static char KEY_F6      =(0x80+0x40);
public static char KEY_F7      =(0x80+0x41);
public static char KEY_F8      =(0x80+0x42);
public static char KEY_F9      =(0x80+0x43);
public static char KEY_F10     =(0x80+0x44);
public static char KEY_F11     =(0x80+0x57);
public static char KEY_F12     =(0x80+0x58);

public static char KEY_BACKSPACE  = 127;
public static char KEY_PAUSE   =0xff;

public static char KEY_EQUALS  =0x3d;
public static char KEY_MINUS   =0x2d;

public static char KEY_RSHIFT = (0x80+0x36);
public static char KEY_RCTRL  = (0x80+0x1d);
public static final char KEY_RALT   = (0x80+0x38);

public static final char KEY_LALT  =  KEY_RALT;

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
static public final int ANGLETOSKYSHIFT   = 22;


// From r_draw.c

//?
static public final int  MAXWIDTH      =      1120;
static public final int  MAXHEIGHT      =     832;

// status bar height at bottom of screen
static public final int  SBARHEIGHT     = 32;


// From wi_stuff

static public final int NoState = -1;
static public final int StatCount =0;
static public final int ShowNextLoc=1; 

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
static public final int NUMEPISODES=4;
static public final int NUMMAPS     =9;


//in tics
//U #define PAUSELEN        (TICRATE*2) 
//U #define SCORESTEP       100
//U #define ANIMPERIOD      32
//pixel distance from "(YOU)" to "PLAYER N"
//U #define STARDIST        10 
//U #define WK 1


//GLOBAL LOCATIONS
static public final int WI_TITLEY       =2;
static public final int WI_SPACINGY         =3;

//SINGPLE-PLAYER STUFF
static public final int SP_STATSX       =50;
static public final int SP_STATSY      = 50;

static public final int SP_TIMEX      =  16;
static public final int SP_TIMEY      =  (SCREENHEIGHT-32);


//NET GAME STUFF
static public final int NG_STATSY       =50;


static public final int NG_SPACINGX        = 64;


//DEATHMATCH STUFF
static public final int DM_MATRIXX = 42;
static public final int DM_MATRIXY =     68;

static public final int DM_SPACINGX =    40;

static public final int DM_TOTALSX =     269;

static public final int DM_KILLERSX  =   10;
static public final int DM_KILLERSY   =  100;
static public final int DM_VICTIMSX    =     5;
static public final int DM_VICTIMSY    = 50;

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
static public final int PU_STATIC   =    1;   // static entire execution time
static public final int PU_SOUND    =    2;   // static while playing
static public final int PU_MUSIC   =     3;   // static while playing
static public final int PU_DAVE   =  4;   // anything else Dave wants static
static public final int PU_LEVEL   =     50;  // static until level exited
static public final int PU_LEVSPEC  =    51;    // a special thinker in a level
//Tags >= 100 are purgable whenever needed.
static public final int PU_PURGELEVEL =  100;
static public final int PU_CACHE     =   101;


// From hu_lib.h:

//background and foreground screen numbers
//different from other modules.
static public final int BG     =     1;
static public final int FG     =     0;

//font stuff
static public final char HU_CHARERASE  =  KEY_BACKSPACE;

static public final int HU_MAXLINES  =   4;
static public final int HU_MAXLINELENGTH  =  80;

// From hu_stuff.h

//
//Globally visible constants.
//
static public final byte HU_FONTSTART  =  '!'; // the first font characters
static public final byte HU_FONTEND  ='_'; // the last font characters

//Calculate # of glyphs in font.
static public final int HU_FONTSIZE = (HU_FONTEND - HU_FONTSTART + 1); 

static public final char HU_BROADCAST   = 5;

static public final char HU_MSGREFRESH =  KEY_ENTER;
static public final char HU_MSGX     =0;
static public final char HU_MSGY     =0;
static public final char HU_MSGWIDTH =64;  // in characters
static public final char HU_MSGHEIGHT  =  1;   // in lines

static public final int HU_MSGTIMEOUT =  (4*TICRATE);

static public final int SAVESTRINGSIZE = 24;

//
// Button/action code definitions.
// From d_event.h

     // Press "Fire".
    static public final int BT_ATTACK       = 1;
     // Use button, to open doors, activate switches.
    static public final int BT_USE      = 2;

     // Flag: game events, not really buttons.
    static public final int BT_SPECIAL      = 128;
    static public final int BT_SPECIALMASK  = 3;
     
     // Flag, weapon change pending.
     // If true, the next 3 bits hold weapon num.
    static public final int  BT_CHANGE       = 4;
     // The 3bit weapon mask and shift, convenience.
    static public final int  BT_WEAPONMASK   = (8+16+32);
    static public final int   BT_WEAPONSHIFT  = 3;

     // Pause the game.
    static public final int  BTS_PAUSE       = 1;
     // Save the game at each console.
    static public final int  BTS_SAVEGAME    = 2;

     // Savegame slot numbers
     //  occupy the second byte of buttons.    
    static public final int BTS_SAVEMASK    = (4+8+16);
    static public final int BTS_SAVESHIFT   = 2;
   
    static public final int MAXEVENTS       =64;
    
    
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

static final char[]
rcsid = ("$Id: Defines.java,v 1.4 2010/08/13 14:06:36 velktron Exp $").toCharArray();
}
