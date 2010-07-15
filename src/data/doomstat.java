package data;

//We need globally shared data structures,
//for defining the global state variables.
//MAES: in pure OO style, this should be a global "Doom state" object to be passed along
//various modules. No ugly globals here!!!
import static data.Defines.*;
import doom.player_t;
import doom.wbstartstruct_t;

//#include "doomdata.h"
//#include "d_net.h"

// We need the playr data structure as well.
//import d.player.*;

public class doomstat{

// ------------------------
// Command line parameters.
//
public boolean nomonsters; // checkparm of -nomonsters
public boolean respawnparm;    // checkparm of -respawn
public boolean fastparm;   // checkparm of -fast
public  boolean devparm;    // DEBUG: launched with -devparm



// -----------------------------------------------------
// Game Mode - identify IWAD as shareware, retail etc.
//
public GameMode_t  gamemode;
public GameMission_t  gamemission;

// Set if homebrew PWAD stuff has been added.
public boolean modifiedgame;


// -------------------------------------------
// Language.
public Language_t   language;


// -------------------------------------------
// Selected skill type, map etc.
//

// Defaults for menu, methinks.
public skill_t     startskill;
public int             startepisode;
public int     startmap;

public   boolean     autostart;

// Selected by user. 
public skill_t         gameskill;
public int     gameepisode;
public int     gamemap;

// Nightmare mode flag, single player.
public boolean         respawnmonsters;

// Netgame? Only true if >1 player.
public boolean netgame;

// Flag: true only if started as net deathmatch.
// An enum might handle altdeath/cooperative better.
public boolean deathmatch; 
    
// -------------------------
// Internal parameters for sound rendering.
// These have been taken from the DOS version,
//  but are not (yet) supported with Linux
//  (e.g. no sound volume adjustment with menu.

// These are not used, but should be (menu).
// From m_menu.c:
//  Sound FX volume has default, 0 - 15
//  Music volume has default, 0 - 15
// These are multiplied by 8.
public  int snd_SfxVolume;      // maximum volume for sound
public  int snd_MusicVolume;    // maximum volume for music

// Current music/sfx card - index useless
//  w/o a reference LUT in a sound module.
// Ideally, this would use indices found
//  in: /usr/include/linux/soundcard.h
public  int snd_MusicDevice;
public  int snd_SfxDevice;
// Config file? Same disclaimer as above.
public  int snd_DesiredMusicDevice;
public  int snd_DesiredSfxDevice;


// -------------------------
// Status flags for refresh.
//

// Depending on view size - no status bar?
// Note that there is no way to disable the
//  status bar explicitely.
public   boolean statusbaractive;

public   boolean automapactive;  // In AutoMap mode?
public   boolean menuactive; // Menu overlayed?
public   boolean paused;     // Game Pause?


public   boolean     viewactive;

public   boolean     nodrawers;
public   boolean     noblit;

public   int     viewwindowx;
public   int     viewwindowy;
public   short     viewheight;
public   int     viewwidth;
public   int     scaledviewwidth;






// This one is related to the 3-screen display mode.
// ANG90 = left side, ANG270 = right
public   int viewangleoffset;

// Player taking events, and displaying.
public   int consoleplayer;  
public   int displayplayer;


// -------------------------------------
// Scores, rating.
// Statistics on a given map, for intermission.
//
public   int totalkills;
public   int totalitems;
public   int totalsecret;

// Timer, for scores.
public   int levelstarttic;  // gametic at level start
public   int leveltime;  // tics in game play for par



// --------------------------------------
// DEMO playback/recording related stuff.
// No demo, there is a human player in charge?
// Disable save/end game?
public   boolean usergame;

//?
public   boolean demoplayback;
public   boolean demorecording;

// Quit after playing a demo from cmdline.
public   boolean     singledemo; 

//?
public   gamestate_t     gamestate;

//-----------------------------
// Internal parameters, fixed.
// These are set by the engine, and not changed
//  according to user inputs. Partly load from
//  WAD, partly set at startup time.

public   int     gametic;


// Bookkeeping on players - state.
public   player_t[]    players= new player_t[MAXPLAYERS];

// Alive? Disconnected?
public   boolean[]  playeringame=new boolean[MAXPLAYERS];


// Player spawn spots for deathmatch.
public  int MAX_DM_STARTS =  10;
public   mapthing_t[] deathmatchstarts=new mapthing_t[MAX_DM_STARTS];
// Maes: we need no pointer/struct dichotomy here.
public   mapthing_t deathmatch_p;

// Player spawn spots.
public   mapthing_t[] playerstarts=new mapthing_t[MAXPLAYERS];

// Intermission stats.
// Parameters for world map / intermission.
public   wbstartstruct_t     wminfo; 


// LUT of ammunition limits for each kind.
// This doubles with BackPack powerup item.
public   int[] maxammo= new int[NUMAMMO];





//-----------------------------------------
// Internal parameters, used for engine.
//

// File handling stuff.
public   char[]  basedefault=new char[1024];
//public   System.File       debugfile;

// if true, load all graphics at level load
public   boolean         precache;


// wipegamestate can be set to -1
//  to force a wipe on the next draw
public   gamestate_t     wipegamestate;

public   int             mouseSensitivity;
//?
// debug flag to cancel adaptiveness
public   boolean         singletics; 

public   int             bodyqueslot;



// Needed to store the number of the dummy sky flat.
// Used for rendering,
//  as well as tracking projectiles etc.
public  int      skyflatnum;



// TODO: Netgame stuff (buffers and pointers, i.e. indices).

// TODO: This is ???
//public   doomcom_t*  doomcom;

// TODO: This points inside doomcom.
//public   doomdata_t* netbuffer;  
/*

public   ticcmd_t[] localcmds=ticcmd_t[BACKUPTICS];
public   int     rndindex;

public   int     maketic;
public   int             nettics[MAXNETNODES];

public   ticcmd_t        netcmds[MAXPLAYERS][BACKUPTICS];
public   int     ticdup;
*/

}