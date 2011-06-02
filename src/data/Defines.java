package data;

//import m.define;
import static data.Limits.*;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import defines.ammotype_t;
import defines.card_t;
import doom.weapontype_t;
import f.anim_t;
import f.point_t;
import w.animenum_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Defines.java,v 1.40 2011/06/02 14:52:48 velktron Exp $
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
public final class Defines{
    
    /** Seems to be 109 for shareware 1.9, wtf is this*/
public static final int VERSION =  109 ;

/** Some parts of the code may actually be better used as if in a UNIX environment */

public static final boolean NORMALUNIX =false;


/** If rangecheck is undefined,  ost parameter validation debugging code will not be compiled */
public static final boolean RANGECHECK=false;

// Do or do not use external soundserver.
// The sndserver binary to be run separately
//  has been introduced by Dave Taylor.
// The integrated sound support is experimental,
//  and unfinished. Default is synchronous.
// Experimental asynchronous timer based is
//  handled by SNDINTR. 
//#define SNDSERV  1
//#define SNDINTR  1

// Defines suck. C sucks.
// C++ might sucks for OOP, but it sure is a better C.
// So there.

// MAES: moved static defines out of here and into VideoScaleInfo.

// State updates, number of tics / second.
public static final int TICRATE = 35;



//
// Difficulty/skill settings/filters.
//

// Skill flags.
public static int MTF_EASY =       1;
public static int MTF_NORMAL =     2;
public static int MTF_HARD =       4;

// Deaf monsters/do not react to sound.
public static int MTF_AMBUSH =8;

//Maes: this makes it a bit less retarded.
public static final int NUMCARDS=card_t.NUMCARDS.ordinal();


//Maes: this makes it a bit less retarded.
public static final int NUMWEAPONS=weapontype_t.NUMWEAPONS.ordinal();

 ;

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
// NOTE: the codes used here are arbitrary, however they must
// match what's used in the default.cfg files.

public final static char KEY_RIGHTARROW  =0x00ae;
public final static char KEY_LEFTARROW   =0x00ac;
public final static char KEY_UPARROW =0x00ad;
public final static char KEY_DOWNARROW   =0x00af;
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
public final static char KEY_PAUSE   =0x00ff;

public final static char KEY_EQUALS  =0x003d;
public final static char KEY_MINUS   =0x002d;

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

//Number of diminishing brightness levels.
//There a 0-31, i.e. 32 LUT in the COLORMAP lump.
public static final int  NUMCOLORMAPS   =     32;

//SKY, store the number for name.
static public final String SKYFLATNAME  ="F_SKY1";

// The sky map is 256*128*4 maps.
public static final int ANGLETOSKYSHIFT   = 22;


// From r_draw.c

// status bar height at bottom of screen
public static final int  SBARHEIGHT     = 32;

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


// MAES 23/5/2011: moved SP_... stuff to EndLevel

public static final int    BACKUPTICS     = 12;




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
   

    
    
    //==================== Stuff from r_local.c =========================================

    
    public static final int FLOATSPEED        =(FRACUNIT*4);

    public static final int VIEWHEIGHT    =   (41*FRACUNIT);

    // mapblocks are used to check movement
    // against lines and things
    public static final int MAPBLOCKUNITS=    128;
    public static final int MAPBLOCKSIZE  =(MAPBLOCKUNITS*FRACUNIT);
    public static final int MAPBLOCKSHIFT =(FRACBITS+7);
    public static final int MAPBMASK      =(MAPBLOCKSIZE-1);
    public static final int MAPBTOFRAC=       (MAPBLOCKSHIFT-FRACBITS);


    // player radius for movement checking
    public static final int PLAYERRADIUS  =16*FRACUNIT;



    public static final int GRAVITY   =   FRACUNIT;


    public static int USERANGE      =(64*FRACUNIT);
    public static int MELEERANGE    =   (64*FRACUNIT);
    public static int MISSILERANGE=(32*64*FRACUNIT);

    // follow a player exlusively for 3 seconds
    public static int   BASETHRESHOLD=      100;


    
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
 
 ;
	
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
    public static final int NF_SUBSECTOR = 0x8000;
	
    
    /** Player states. */
    
    public static final int   PST_LIVE=0,     // Playing or camping.    
        PST_DEAD=1,        // Dead on the ground, view follows killer.

        PST_REBORN=2;            // Ready to restart/respawn???
 
    public static final int  FF_FULLBRIGHT =  0x8000;  // flag in thing->frame
    public static final int  FF_FRAMEMASK =   0x7fff;

    public static final int acpv=0;
    public static final int acp1=1;
    public static final int acp2=2;
 
static final String
rcsid = "$Id: Defines.java,v 1.40 2011/06/02 14:52:48 velktron Exp $";
}

