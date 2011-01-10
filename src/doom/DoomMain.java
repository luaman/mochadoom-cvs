package doom;

import i.AWTDoom;
import i.DoomSystem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import n.DummyNetworkDriver;
import static data.dstrings.*;
import p.Actions;
import p.LevelLoader;
import p.mobj_t;
import automap.Map;
import f.Wiper;
import hu.HU;
import m.Menu;
import m.random;
import static doom.NetConsts.*;
import static doom.englsh.*;
import data.Tables;
import data.dstrings;
import data.mapthing_t;
import data.mobjtype_t;
import defines.*;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import static data.Defines.BACKUPTICS;
import static data.Defines.KEY_ESCAPE;
import static data.Defines.NORMALUNIX;
import static data.Defines.VERSION;
import rr.ParallelRenderer;
import rr.SimpleTextureManager;
import rr.TextureManager;
import rr.UnifiedRenderer;
import rr.subsector_t;
import s.DummySoundDriver;
import st.StatusBar;
import utils.C2JUtils;
import utils.PrintfFormat;
import v.BufferedRenderer;
import w.DoomFile;
import w.WadLoader;
import static data.Defines.*;
import static data.Limits.*;
import static data.Tables.*;
import static data.dstrings.SAVEGAMENAME;
import static data.info.mobjinfo;
import static data.info.states;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static utils.C2JUtils.*;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomMain.java,v 1.31 2011/01/10 16:40:54 velktron Exp $
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
// $Log: DoomMain.java,v $
// Revision 1.31  2011/01/10 16:40:54  velktron
// Some v1.3 commits: OSX fix, limit-removing flat management (to fix),
//
// Revision 1.30  2010/12/20 17:15:08  velktron
// Made the renderer more OO -> TextureManager and other changes as well.
//
// Revision 1.29  2010/12/15 16:12:19  velktron
// Changes in Wiper code and alternate timing method, hoping to fix the Athlon X2
//
// Revision 1.28  2010/12/13 16:03:20  velktron
// More fixes  in the wad loading code
//
// Revision 1.27  2010/12/11 15:08:59  velktron
// Techdemo release.
//
// Revision 1.26  2010/12/10 17:38:57  velktron
// pspritescale fixed, weapon actions won't crash (but not work either).
//
// Revision 1.25  2010/11/26 17:16:48  velktron
// Sprite sorting, solid block drawing.
//
// Revision 1.24  2010/11/25 20:12:44  velktron
// Fixed blockmap bug and viewangletox overflow bug.
//
// Revision 1.23  2010/11/25 15:59:21  velktron
// Fixed visplane overflows and findplane blinking.
//
// Revision 1.22  2010/11/24 17:34:08  velktron
// Parallel Renderer complete, almost 100% gains on a Quad ;-)
//
// Revision 1.21  2010/11/24 14:51:10  velktron
// Parallel Renderer complete, almost 100% gains on a Quad ;-)
//
// Revision 1.20  2010/11/22 21:41:21  velktron
// Parallel rendering...sort of.It works, but either  the barriers are broken or it's simply not worthwhile at this point :-/
//
// Revision 1.19  2010/11/22 01:17:16  velktron
// Fixed blockmap (for the most part), some actions implemented and functional, ambient animation/lighting functional.
//
// Revision 1.18  2010/11/17 23:55:06  velktron
// Kind of playable/controllable.
//
// Revision 1.17  2010/11/12 13:37:25  velktron
// Rationalized the LUT system - now it's 100% procedurally generated.
//
// Revision 1.16  2010/11/11 15:31:28  velktron
// Fixed "warped floor" error.
//
// Revision 1.15  2010/10/25 15:57:19  velktron
// Work on netcode...needs moar though.
//
// Revision 1.14  2010/10/22 16:22:44  velktron
// Renderer works stably enough but a ton of bleeding. Started working on netcode.
//
// Revision 1.13  2010/10/14 18:37:14  velktron
// Rendering kinda works. Wow.
//
// Revision 1.12  2010/10/08 16:55:50  velktron
// Duh
//
// Revision 1.11  2010/10/07 15:42:16  velktron
// *** empty log message ***
//
// Revision 1.10  2010/09/27 15:07:44  velktron
// meh
//
// Revision 1.9  2010/09/27 02:27:29  velktron
// BEASTLY update
//
// Revision 1.8  2010/09/25 17:37:13  velktron
// Lots of changes.
//
// The most important is the creation of the AWTDoom frame handling I/O.
//
// Revision 1.7  2010/09/23 20:36:45  velktron
// *** empty log message ***
//
// Revision 1.6  2010/09/23 15:11:57  velktron
// A bit closer...
//
// Revision 1.5  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.4  2010/09/22 16:40:02  velktron
// MASSIVE changes in the status passing model.
// DoomMain and DoomGame unified.
// Doomstat merged into DoomMain (now status and game functions are one).
//
// Most of DoomMain implemented. Possible to attempt a "classic type" start but will stop when reading sprites.
//
// Revision 1.3  2010/09/07 16:23:00  velktron
// *** empty log message ***
//
// Revision 1.2  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.1  2010/07/29 15:28:59  velktron
// More work on menus...and digging some dependencies..
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
//	DOOM main program (D_DoomMain) and game loop (D_DoomLoop),
//	plus functions to determine game mode (shareware, registered),
//	parse command line parameters, configure game parameters (turbo),
//	and call the startup functions.
//
//  In Mocha Doom, this was unified with d_game and doomstat.c
//
//-----------------------------------------------------------------------------

public class DoomMain extends DoomStatus implements DoomGameNetworking, DoomGame {
	
public static final String rcsid = "$Id: DoomMain.java,v 1.31 2011/01/10 16:40:54 velktron Exp $";

//
// EVENT HANDLING
//
// Events are asynchronous inputs generally generated by the game user.
// Events can be discarded if no responder claims them
//
public event_t[]         events=new event_t[MAXEVENTS];
public int             eventhead;
public int 		eventtail;


/**
 * D_PostEvent
 * Called by the I/O functions when input is detected
 */

public void PostEvent (event_t ev)
{
    events[eventhead] = ev;
    eventhead = (++eventhead)&(MAXEVENTS-1);
}


/**
 * D_ProcessEvents
 * Send all the events of the given timestamp down the responder chain
 */ 
public void ProcessEvents ()
{
    event_t	ev;
	
    // IF STORE DEMO, DO NOT ACCEPT INPUT

     if ( ( gamemode == GameMode_t.commercial )
	 && (W.CheckNumForName("MAP01")<0) )
      return; 
	
    for ( ; eventtail != eventhead ; eventtail = (++eventtail)&(MAXEVENTS-1) )
    {
	ev = events[eventtail];
	if (M.Responder (ev))
	    continue;               // menu ate the event
	Responder (ev);
    }
}

// "static" to Display, don't move.
private  boolean		viewactivestate = false;
private  boolean		menuactivestate = false;
private  boolean		inhelpscreensstate = false;
private  boolean		fullscreen = false;
private  gamestate_t	oldgamestate = gamestate_t.GS_MINUS_ONE;
private  int			borderdrawcount;

/**
 * D_Display
 * draw current display, possibly wiping it from the previous
 */

public void Display ()
{
    int				nowtime;
    int				tics;
    int				wipestart;
    int				y;
    boolean			done;
    boolean			wipe;
    boolean			redrawsbar;

    if (nodrawers)
	return;                    // for comparative timing / profiling
		
    redrawsbar = false;
    
    // change the view size if needed
    if (R.setsizeneeded)
    {
	R.ExecuteSetViewSize ();
	oldgamestate = gamestate_t.GS_MINUS_ONE;                      // force background redraw
	borderdrawcount = 3;
    }

    // save the current screen if about to wipe
    if (wipe=(gamestate != wipegamestate))
    {
	wipe = true;
	WIPE.StartScreen(0, 0, SCREENWIDTH, SCREENHEIGHT);
    }
    else
	wipe = false;

    if (gamestate == gamestate_t.GS_LEVEL && eval(gametic))
	HU.Erase();
    
    // do buffered drawing
    switch (gamestate)
    {
      case GS_LEVEL:
	if (!eval(gametic))
	    break;
	if (automapactive)
	    AM.Drawer ();
	if (wipe || (R.viewheight != SCREENHEIGHT && fullscreen) )
	    redrawsbar = true;
	if (inhelpscreensstate && !inhelpscreens)
	    redrawsbar = true;              // just put away the help screen
	ST.Drawer (R.viewheight == SCREENHEIGHT, redrawsbar );
	fullscreen = R.viewheight == SCREENHEIGHT;
	break;

      case GS_INTERMISSION:
	WI.Drawer ();
	break;

      case GS_FINALE:
	F.Drawer ();
	break;

      case GS_DEMOSCREEN:
	PageDrawer ();
	break;
    }
    
    // draw buffered stuff to screen
    VI.UpdateNoBlit ();
    
    // draw the view directly
    if (gamestate == gamestate_t.GS_LEVEL && !automapactive && eval(gametic))
	R.RenderPlayerView (players[displayplayer]);    
    
    // Automap was active, update only HU.    
    if (gamestate == gamestate_t.GS_LEVEL && eval(gametic))
	HU.Drawer ();
    
    // clean up border stuff
    if (gamestate != oldgamestate && gamestate != gamestate_t.GS_LEVEL)
        VI.SetPalette (0);

    // see if the border needs to be initially drawn
    if (gamestate == gamestate_t.GS_LEVEL && oldgamestate != gamestate_t.GS_LEVEL)
    {
	viewactivestate = false;        // view was not active
	R.FillBackScreen ();    // draw the pattern into the back screen
    }

    // see if the border needs to be updated to the screen
    if (gamestate == gamestate_t.GS_LEVEL && !automapactive && R.scaledviewwidth != SCREENWIDTH)
    {
	if (menuactive || menuactivestate || !viewactivestate)
	    borderdrawcount = 3;
	if (eval(borderdrawcount))
	{
	    R.DrawViewBorder ();    // erase old menu stuff
	    borderdrawcount--;
	}

    }

    menuactivestate = menuactive;
    viewactivestate = viewactive;
    inhelpscreensstate = inhelpscreens;
    oldgamestate = wipegamestate = gamestate;
    
    // draw pause pic
    if (paused)
    {
	if (automapactive)
	    y = 4;
	else
	    y = R.viewwindowy+4;
	    V.DrawPatchDirect(R.viewwindowx+(R.scaledviewwidth-68)/2,
			  y,0,W.CachePatchName ("M_PAUSE", PU_CACHE));
    }


    // menus go directly to the screen
    M.Drawer ();          // menu is drawn even on top of everything
    NetUpdate ();         // send out any new accumulation


    // normal update
    if (!wipe)
    {
    //System.out.print("Tick "+DM.gametic+"\t");
	//System.out.print(DM.players[0]);
    VI.FinishUpdate ();              // page flip or blit buffer
	return;
    }
    
    // wipe update
    WIPE.EndScreen(0, 0, SCREENWIDTH, SCREENHEIGHT);

    wipestart = I.GetTime () - 1;

    do
    {
	do
	{
		nowtime = I.GetTime ();
	    tics = nowtime - wipestart;
	} while (tics==0); // Wait until a single tic has passed.
	wipestart = nowtime;
	done = WIPE.ScreenWipe(Wiper.wipe.Melt.ordinal()
			       , 0, 0, SCREENWIDTH, SCREENHEIGHT, tics);
	VI.UpdateNoBlit ();
	M.Drawer ();                            // menu is drawn even on top of wipes
	VI.FinishUpdate ();                      // page flip or blit buffer
    } while (!done);
    //wipestart = I.GetTime ();
    
    // Fixme: lame way to limit speed :-/
    /*while (wipestart-I.GetTime()>-1){
    	try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }*/
}



/**
 * D-DoomLoop()
 * Not a globally visible function,
 *  just included for source reference,
 *  called by D_DoomMain, never exits.
 * Manages timing and IO,
 *  calls all ?_Responder, ?_Ticker, and ?_Drawer,
 *  calls I_GetTime, I_StartFrame, and I_StartTic
 */

public void DoomLoop ()
{
    if (demorecording)
	BeginRecording ();
		
    if (eval(CheckParm ("-debugfile")))
    {
	String    filename="debug"+consoleplayer+".txt";
	System.out.println("debug output to: "+filename);
	try {
        debugfile = new DoomFile(filename,"w");
    } catch (FileNotFoundException e) {
        System.err.println("Couldn't open debugfile. Now, that sucks some putrid shit out of John Romero's asshole!");
        e.printStackTrace();
    }
    }
	
	AM.Start();
    while (true)
    {
	// frame syncronous IO operations
	VI.StartFrame ();                
	
	// process one or more tics
	if (singletics)
	{
	    VI.StartTic ();
	    ProcessEvents ();
  	    BuildTiccmd (netcmds[consoleplayer][maketic%BACKUPTICS]);
	    if (advancedemo)
		DoAdvanceDemo ();
	    M.Ticker ();
	    Ticker ();
	    gametic++;
	    maketic++;
	}
	else
	{
	    TryRunTics (); // will run at least one tic (in NET)
	}
		
	S.UpdateSounds (players[consoleplayer].mo);// move positional sounds

	// Update display, next frame, with current state.
	Display ();
/*
#ifndef SNDSERV
	// Sound mixing for the buffer is snychronous.
	I_UpdateSound();
#endif	
	// Synchronous sound output is explicitly called.
#ifndef SNDINTR
	// Update sound output.
	I_SubmitSound();
#endif
*/
    }
}



//
//  DEMO LOOP
//
int             demosequence;
int             pagetic;
String pagename;


/**
* D_PageTicker
* Handles timing for warped projection
*/
private final void PageTicker ()
{
    if (--pagetic < 0)
	AdvanceDemo ();
}



/**
 * D_PageDrawer
 */

private void PageDrawer ()
{
    V.DrawPatchSolidScaled (0,0, SAFE_SCALE, SAFE_SCALE,0,W.CachePatchName(pagename, PU_CACHE));
}


/**
 * D_AdvanceDemo
 * Called after each demo or intro demosequence finishes
 */

public void AdvanceDemo ()
{
    advancedemo = true;
}


//
// This cycles through the demo sequences.
// FIXME - version dependant demo numbers?
//
public void DoAdvanceDemo ()
{
    players[consoleplayer].playerstate = PST_LIVE;  // not reborn
    advancedemo = false;
    usergame = false;               // no save / end game here
    paused = false;
    gameaction = gameaction_t.ga_nothing;

    if ( gamemode == GameMode_t.retail )
      demosequence = (demosequence+1)%7;
    else
      demosequence = (demosequence+1)%6;
    
    switch (demosequence)
    {
      case 0:
	if ( gamemode == GameMode_t.commercial )
	    pagetic = 35 * 11;
	else
	    pagetic = 170;
	gamestate = gamestate_t.GS_DEMOSCREEN;
	pagename = "TITLEPIC";
	if ( gamemode == GameMode_t.commercial )
	  S.StartMusic(musicenum_t.mus_dm2ttl);
	    
	else
	    S.StartMusic (musicenum_t.mus_intro); 
	break;
      case 1:
	DeferedPlayDemo ("demo1");
	break;
      case 2:
	pagetic = 200;
	gamestate = gamestate_t.GS_DEMOSCREEN;
	pagename = "CREDIT";
	break;
      case 3:
	DeferedPlayDemo ("demo2");
	break;
      case 4:
	gamestate = gamestate_t.GS_DEMOSCREEN;
	if ( gamemode == GameMode_t.commercial)
	{
	    pagetic = 35 * 11;
	    pagename = "TITLEPIC";
	    S.StartMusic(musicenum_t.mus_dm2ttl);
	}
	else
	{
	    pagetic = 200;

	    if ( gamemode == GameMode_t.retail )
	      pagename = "CREDIT";
	    else
	      pagename = "HELP1";
	}
	break;
      case 5:
	DeferedPlayDemo ("demo3");
	break;
        // THE DEFINITIVE DOOM Special Edition demo
      case 6:
	DeferedPlayDemo ("demo4");
	break;
    }
}



/**
 * D_StartTitle
 */

public void StartTitle ()
{
    gameaction = gameaction_t.ga_nothing;
    demosequence = -1;
    AdvanceDemo ();
}




//      print title for every printed line
StringBuffer            title=new StringBuffer();



/**
 * D_AddFile
 * 
 * Adds file to the end of the wadfiles[] list.
 * Quite crude, we could use a listarray instead.
 * 
 * @param file
 */
void AddFile (String file)
{
    int     numwadfiles;
    String newfile;
	
    for (numwadfiles = 0 ; eval(wadfiles[numwadfiles]) ; numwadfiles++)
	;

    newfile = new String(file);
	
    wadfiles[numwadfiles] = newfile;
}

//
// IdentifyVersion
// Checks availability of IWAD files by name,
// to determine whether registered/commercial features
// should be executed (notably loading PWAD's).
//
public void IdentifyVersion ()
{

    String	doom1wad=null,
            doomwad=null,
            doomuwad=null,
            doom2wad=null,
            doom2fwad=null,
            plutoniawad=null,
            tntwad=null;

    // By default.
    language=Language_t.english;
    
// Unix-like checking. Might come in handy sometimes.   
// This should ALWAYS be activated, else doomwaddir etc. won't be defined.
    

    String home;
    String doomwaddir;
    doomwaddir = System.getenv("DOOMWADDIR");
    home = System.getenv("HOME");
    if (NORMALUNIX){
    if (!eval(home))
      I.Error("Please set $HOME to your home directory");
    }
    basedefault=home+"/.doomrc";   

if (!eval(doomwaddir))
    doomwaddir = ".";

    // Commercial.
    doom2wad = (doomwaddir+ "/doom2.wad");

    // Retail.
    doomuwad = (doomwaddir+ "/doomu.wad");    
    
    // Registered.
    doomwad = (doomwaddir+ "/doom.wad");
    
    // Shareware.
    doom1wad = (doomwaddir+ "/doom1.wad");

    // Bug, dear Shawn.
    // Insufficient malloc, caused spurious realloc errors.
    plutoniawad = (doomwaddir+ "/plutonia.wad");
    
    tntwad = (doomwaddir+ "/tnt.wad");


    // French stuff.
    doom2fwad=(doomwaddir+ "/doom2f.wad");

    // MAES: Interesting. I didn't know of that :-o
    if (eval(CheckParm ("-shdev")))
    {
	gamemode = GameMode_t.shareware;
	devparm = true;
	AddFile (dstrings.DEVDATA+"doom1.wad");
	AddFile (dstrings.DEVMAPS+"data_se/texture1.lmp");
	AddFile (dstrings.DEVMAPS+"data_se/pnames.lmp");
	basedefault=dstrings.DEVDATA+"default.cfg";
	return;
    }

    if (eval(CheckParm ("-regdev")))
    {
	gamemode = GameMode_t.registered;
	devparm = true;
	AddFile (dstrings.DEVDATA+"doom.wad");
	AddFile (dstrings.DEVMAPS+"data_se/texture1.lmp");
	AddFile (dstrings.DEVMAPS+"data_se/texture2.lmp");
	AddFile (dstrings.DEVMAPS+"data_se/pnames.lmp");
	basedefault=dstrings.DEVDATA+"default.cfg";
	return;
    }

    if (eval(CheckParm ("-comdev")))
    {
	gamemode = GameMode_t.commercial;
	devparm = true;
	/* I don't bother
	if(plutonia)
	    D_AddFile (DEVDATA"plutonia.wad");
	else if(tnt)
	    D_AddFile (DEVDATA"tnt.wad");
	else*/
	    
	AddFile (dstrings.DEVDATA+"doom2.wad");	    
	AddFile (dstrings.DEVMAPS+"cdata/texture1.lmp");
	AddFile (dstrings.DEVMAPS+"cdata/pnames.lmp");
	   basedefault=dstrings.DEVDATA+"default.cfg";
	return;
    }

      
    if ( testAccess(doom2fwad,"r" ))
    {
	gamemode = GameMode_t.commercial;
	// C'est ridicule!
	// Let's handle languages in config files, okay?
	language = Language_t.french;
	System.out.println("French version\n");
	AddFile (doom2fwad);
	return;
    }

        
    if ( testAccess(doom2wad,"r" ))
    {
	gamemode = GameMode_t.commercial;
	AddFile (doom2wad);
	return;
    }

    if ( testAccess (plutoniawad, "r" ) )
    {
      gamemode = GameMode_t.pack_plut;
      AddFile (plutoniawad);
      return;
    }

    if ( testAccess ( tntwad, "r" ) )
    {
      gamemode = GameMode_t.pack_tnt;
      AddFile (tntwad);
      return;
    }

    if ( testAccess (doomuwad,"r") )
    {
      gamemode = GameMode_t.retail;
      AddFile (doomuwad);
      return;
    }

    if ( testAccess (doomwad,"r") )
    {
      gamemode = GameMode_t.registered;
      AddFile (doomwad);
      return;
    }

    if ( testAccess (doom1wad,"r") )
    {
      gamemode = GameMode_t.shareware;
      AddFile (doom1wad);
      return;
    }

    // MAES: Maybe we should add FreeDoom here later.
    
    System.out.println("Game mode indeterminate.\n");
    gamemode = GameMode_t.indetermined;

    // We don't abort. Let's see what the PWAD contains.
    //exit(1);
    //I_Error ("Game mode indeterminate\n");
}

/**
 * Find a Response File
 * 
 * Not very well documented, but Doom apparently could use a sort of 
 * script file with command line arguments inside, if you prepend @ to
 * the command-like argument itself. The arguments themselves could
 * be separated by any sort of whitespace or ASCII characters exceeding "z"
 * in value.
 * 
 * E.g. doom @crap
 * 
 * would load a file named "crap".
 * 
 * Now, the original function is crap for several reasons: for one,
 * it will bomb if more than 100 arguments <i>total</i> are formed.
 * Memory allocation will also fail because the tokenizer used only
 * stops at file size limit, not at maximum parsed arguments limit
 * (MACARGVS = 100).
 * 
 * This is the wiki's entry:
 * 
 * doom @<response>
 * This parameter tells the Doom engine to read from a response file, 
 * a text file that may store additional command line parameters. 
 * The file may have any name that is valid to the system, optionally 
 * with an extension. The parameters are typed as in the command line 
 * (-episode 2, for example), but one per line, where up to 100 lines
 *  may be used. The additional parameters may be disabled for later 
 *  use by placing a vertical bar (the | character) between the 
 *  prefixing dash (-) and the rest of the parameter name.
 * 
 * 
 */
public void FindResponseFile ()
{
    try{
    
    for (int i = 1;i < myargc;i++)
	if (myargv[i].charAt(0)=='@')
	{
	    DoomFile        handle;
	    // save o	    
	    int             size;
	    int             indexinfile;
	    char[]    infile=null;
	    char[]    file=null;
	    // Fuck that, we're doing it properly.
	    ArrayList<String>  parsedargs=new ArrayList<String>();
	    ArrayList<String>    moreargs=new ArrayList<String>();
	    String    firstargv;
			
	    // READ THE RESPONSE FILE INTO MEMORY
	    handle = new DoomFile(myargv[i].substring(1),"rb");
	    if (!eval(handle))
	    {
		System.out.print ("\nNo such response file!");
		System.exit(1);
	    }
	    System.out.println("Found response file "+myargv[i].substring(1));
	    size = (int) handle.length();
	    
	    file = new char[size];
	    handle.readNonUnicodeCharArray(file, file.length);
	    handle.close();

	    // Save first argument.
        firstargv = myargv[0];
	    
	    // KEEP ALL CMDLINE ARGS FOLLOWING @RESPONSEFILE ARG
	    // This saves the old references.
	    for (int k = i+1; k < myargc; k++)
	        moreargs.add(myargv[k]);

	    infile = file;
	    indexinfile = 0;
	    indexinfile++;  // SKIP PAST ARGV[0] (KEEP IT)
	    StringBuffer build=new StringBuffer();
	    
	    /* MAES: the code here looked like some primitive tokenizer.
	       that assigned C-strings to memory locations.
	       Instead, we'll tokenize the file input correctly here.
	       */
	    
        StringTokenizer tk=new StringTokenizer(String.copyValueOf(infile));

        
        
        //myargv = new String[tk.countTokens()+argc];
        parsedargs.add(firstargv);
        
        while(tk.hasMoreTokens())
	    {
            parsedargs.add(tk.nextToken());
	    }
			
        // Append the other args to the end.
	    parsedargs.addAll(moreargs);

        /* NOW the original myargv is reset, but the old values still survive in 
         * the listarray.*/
	    
		myargv= new String[parsedargs.size()];
		myargv=parsedargs.toArray(myargv);
	    myargc = myargv.length;
	
	    // DISPLAY ARGS
	    System.out.println(myargc+" command-line args:");
	    for (int k=0;k<myargc;k++)
	        System.out.println(myargv[k]);

	    // Stops at the first one. Pity, because we could do funky recursive stuff with that :-p
	    break;
	}
    } catch (Exception e){
        e.printStackTrace();
    }
}


//
// D_DoomMain
//
public void Start ()
{
    int             p;
    StringBuffer                    file=new StringBuffer();

    FindResponseFile ();
	
    IdentifyVersion ();
	
    // Sets unbuffered output in C. Not needed here. setbuf (stdout, NULL);
    modifiedgame = false;
	
    nomonsters = eval(CheckParm ("-nomonsters"));
    respawnparm = eval(CheckParm ("-respawn"));
    fastparm = eval(CheckParm ("-fast"));
    devparm = eval(CheckParm ("-devparm"));
    if (eval(CheckParm ("-altdeath")))
	//deathmatch = 2;
        altdeath=true;
    else if (eval(CheckParm ("-deathmatch")))
	deathmatch = true;

    switch ( gamemode )
    {
      case retail:
	title.append("                         ");
	title.append("The Ultimate DOOM Startup v");
	title.append(VERSION/100);
	title.append(".");
	title.append(VERSION%100);
	title.append("                           ");
	break;
      case shareware:
          title.append("                            ");
          title.append("DOOM Shareware Startup v");
	    title.append(VERSION/100);
    title.append(".");
    title.append(VERSION%100);
    title.append("                           ");
	break;
      case registered:
          title.append("                            ");
          title.append("DOOM Registered Startup v");
          title.append(VERSION/100);
          title.append(".");
          title.append(VERSION%100);
          title.append("                           ");
	break;
      case commercial:
          title.append("                            ");
          title.append("DOOM 2: Hell on Earth v");
	    title.append(VERSION/100);
    title.append(".");
    title.append(VERSION%100);
    title.append("                           ");
	break;
       case pack_plut:
           title.append("                            ");
           title.append("DOOM 2: Plutonia Experiment v");
        title.append(VERSION/100);
    title.append(".");
    title.append(VERSION%100);
    title.append("                           ");
	break;
      case pack_tnt:
          title.append("                            ");
          title.append("DOOM 2: TNT - Evilution v");
        title.append(VERSION/100);
    title.append(".");
    title.append(VERSION%100);
    title.append("                           ");
	break;

      default:
          title.append("                            ");
          title.append("Public DOOM - v");
        title.append(VERSION/100);
    title.append(".");
    title.append(VERSION%100);
    title.append("                           ");
	break;
    }
    
    System.out.println(title.toString());

    if (devparm)
        System.out.println(D_DEVSTR);
    
    if (eval(CheckParm("-cdrom")))
    {
        System.out.println(D_CDROM);
	//System.get("c:\\doomdata",0);
	System.out.println (basedefault+"c:/doomdata/default.cfg");
    }	
    
    // turbo option
    if ( eval(p=CheckParm ("-turbo")) )
    {
	int     scale = 200;
	//int forwardmove[2];
	// int sidemove[2];
	
	if (p<myargc-1)
	    scale = Integer.parseInt(myargv[p+1]);
	if (scale < 10)
	    scale = 10;
	if (scale > 400)
	    scale = 400;
	System.out.println("turbo scale: "+scale);
	forwardmove[0] = forwardmove[0]*scale/100;
	forwardmove[1] = forwardmove[1]*scale/100;
	sidemove[0] = sidemove[0]*scale/100;
	sidemove[1] = sidemove[1]*scale/100;
    }
    
    // add any files specified on the command line with -file wadfile
    // to the wad list
    //
    // convenience hack to allow -wart e m to add a wad file
    // prepend a tilde to the filename so wadfile will be reloadable
    p = CheckParm ("-wart");
    if (eval(p))
    {
    char[] tmp=myargv[p].toCharArray();
    tmp[4]= 'p';// big hack, change to -warp
	myargv[p]=new String(tmp);    
		
	// Map name handling.
	switch (gamemode )
	{
	  case shareware:
	  case retail:
	  case registered:
	    file.append("~");
	    file.append(DEVMAPS);
	    file.append(PrintfFormat.sprintf("E%cM%c.wad", myargv[p+1], myargv[p+2]));
	    file.append(PrintfFormat.sprintf("Warping to Episode %s, Map %s.\n",
		   myargv[p+1],myargv[p+2]));
	    break;
	    
	  case commercial:
	  default:
	    p = Integer.parseInt(myargv[p+1]);
	    if (p<10){
	        file.append("~");
	        file.append(DEVMAPS);
	        file.append(PrintfFormat.sprintf("cdata/map0%i.wad", p));
	    }
	    else
	      {
	           file.append("~");
	            file.append(DEVMAPS);
	            file.append(PrintfFormat.sprintf("cdata/map%i.wad", p));
	      }
	    break;
	}
	AddFile (file.toString());
    }
	
    p = CheckParm ("-file");
    if (eval(p))
    {
	// the parms after p are wadfile/lump names,
	// until end of parms or another - preceded parm
	modifiedgame = true;            // homebrew levels
	while (++p != myargc && myargv[p].charAt(0) != '-')
	    AddFile (myargv[p]);
    }

    p = CheckParm ("-playdemo");

    if (!eval(p))
	p = CheckParm ("-timedemo");

    if (eval(p) && p < myargc-1)
    {
	//filesprintf (file,"%s.lmp", myargv[p+1]);
	AddFile (myargv[p+1]+".lmp");
	System.out.println("Playing demo "+myargv[p+1]+".lmp.");
    }
    
    // get skill / episode / map from parms
    startskill = skill_t.sk_medium;
    startepisode = 1;
    startmap = 1;
    autostart = false;

		
    p = CheckParm ("-skill");
    if (eval(p) && p < myargc-1)
    {
	startskill = skill_t.values()[myargv[p+1].charAt(0)-'1'];
	autostart = true;
    }

    p = CheckParm ("-episode");
    if (eval(p) && p < myargc-1)
    {
	startepisode = myargv[p+1].charAt(0)-'0';
	startmap = 1;
	autostart = true;
    }
	
    p = CheckParm ("-timer");
    if (eval(p) && p < myargc-1 && deathmatch)
    {
	int     time;
	time = Integer.parseInt(myargv[p+1]);
	System.out.print("Levels will end after "+time+" minute");
	if (time>1)
	    System.out.print("s");
	System.out.print(".\n");
    }

    p = CheckParm ("-avg");
    if (eval(p) && p < myargc-1 && deathmatch)
	System.out.print("Austin Virtual Gaming: Levels will end after 20 minutes\n");

    p = CheckParm ("-warp");
    if (eval(p) && p < myargc-1)
    {
	if (gamemode == GameMode_t.commercial)
	    startmap = Integer.parseInt(myargv[p+1]);
	else
	{
		int eval=11;
		try {
		    eval=Integer.parseInt(myargv[p+1]);
		} catch (Exception e){
			// swallow exception. No warp.
		}
		
		if (eval>99) eval%=100;
		
		if (eval<10) {
	    startepisode = 1;
	    startmap = 1;
		}
		else {
		    startepisode = eval/10;
		    startmap = eval%10;
		}
	}
	autostart = true;
    }
    
    // init subsystems
    System.out.print ("V_Init: allocate screens.\n");
    V.Init ();
    
    System.out.print ("M_LoadDefaults: Load system defaults.\n");
    //TODO: M.LoadDefaults ();              // load before initing other systems

    System.out.print ("Z_Init: Init zone memory allocation daemon. \n");
    // DUMMY: Z_Init ();

    System.out.print ("W_Init: Init WADfiles.\n");
    try {
        W.InitMultipleFiles (wadfiles);
    } catch (Exception e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
    }
    
    //
    System.out.print ("VI_Init: set colormaps.\n");
    byte[] pal=W.CacheLumpName("PLAYPAL", PU_STATIC).getBuffer().array();
    // set it, create it, but don't make it visible yet.
    VI=new AWTDoom(this,(BufferedRenderer) V,pal);
    VI.InitGraphics ();

    this.ST.updateStatus(this);

    // Check for -file in shareware
    if (modifiedgame)
    {
	// These are the lumps that will be checked in IWAD,
	// if any one is not present, execution will be aborted.
	String[] name=
	{
	    "e2m1","e2m2","e2m3","e2m4","e2m5","e2m6","e2m7","e2m8","e2m9",
	    "e3m1","e3m3","e3m3","e3m4","e3m5","e3m6","e3m7","e3m8","e3m9",
	    "dphoof","bfgga0","heada1","cybra1","spida1d1"
	};
	int i;
	
	// Oh yes I can.
	if ( gamemode == GameMode_t.shareware)
	    System.out.println("\nYou cannot -file with the shareware version. Register!");

	// Check for fake IWAD with right name,
	// but w/o all the lumps of the registered version. 
	if (gamemode == GameMode_t.registered)
	    for (i = 0;i < 23; i++)
		if (W.CheckNumForName(name[i].toUpperCase())<0)
		    I.Error("\nThis is not the registered version: "+name[i]);
    }
    
    // Iff additonal PWAD files are used, print modified banner
    if (modifiedgame)
    {
	
        System.out.print ("===========================================================================\n");
        System.out.print ("ATTENTION:  This version of DOOM has been modified.  If you would like to\n");
        System.out.print ("get a copy of the original game, call 1-800-IDGAMES or see the readme file.\n");
        System.out.print ("        You will not receive technical support for modified games.\n");
        System.out.print ("                      press enter to continue\n");
        System.out.print ("===========================================================================\n");
	    try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	

    // Check and print which version is executed.
    switch ( gamemode )
    {
      case shareware:
      case indetermined:
          System.out.print ("===========================================================================\n");
          System.out.print ("                                Shareware!\n");
          System.out.print ("===========================================================================\n");
	break;
      case registered:
      case retail:
      case commercial:
          System.out.print ("===========================================================================\n");
          System.out.print ("                 Commercial product - do not distribute!\n");
          System.out.print ("         Please report software piracy to the SPA: 1-800-388-PIR8\n");
          System.out.print ("===========================================================================\n");
	break;
	
      default:
	// Ouch.
	break;
    }

    System.out.print ("Tables.InitTables: Init trigonometric LUTs.\n");
    Tables.InitTables();
    
    System.out.print ("M_Init: Init miscellaneous info.\n");
    M.Init ();

    System.out.print ("R_Init: Init DOOM refresh daemon - ");
    R.Init ();

    System.out.print ("\nP_Init: Init Playloop state.\n");
    P.Init ();

    System.out.print ("I_Init: Setting up machine state.\n");
    I.Init ();

    System.out.print ("D_CheckNetGame: Checking network game status.\n");
    CheckNetGame ();

    System.out.print ("S_Init: Setting up sound.\n");
    S.Init (snd_SfxVolume *8, snd_MusicVolume *8 );

    System.out.print ("HU_Init: Setting up heads up display.\n");
    HU.Init();

    System.out.print ("ST_Init: Init status bar.\n");
    ST.Init ();

    // check for a driver that wants intermission stats
    p = CheckParm ("-statcopy");
    if (eval(p) && p<myargc-1)
    {
	
	// TODO: statcopy = (void*)atoi(myargv[p+1]);
        System.out.print ("External statistics registered.\n");
    }
    
    // start the apropriate game based on parms
    p = CheckParm ("-record");

    if (eval(p) && p < myargc-1)
    {
	RecordDemo (myargv[p+1]);
	autostart = true;
    }
	
    p = CheckParm ("-playdemo");
    if (eval(p) && p < myargc-1)
    {
	singledemo = true;              // quit after one demo
	DeferedPlayDemo (myargv[p+1]);
	DoomLoop ();  // never returns
    }
	
    p = CheckParm ("-timedemo");
    if (eval(p) && p < myargc-1)
    {
	TimeDemo (myargv[p+1]);
	DoomLoop ();  // never returns
    }
	
    p = CheckParm ("-loadgame");
    if (eval(p) && p < myargc-1)
    {
        file.delete(0, file.length());
	if (eval(CheckParm("-cdrom"))){
	    file.append("c:\\doomdata\\");
	    file.append(SAVEGAMENAME);
	    file.append("%c.dsg");
	    file.append(myargv[p+1].charAt(0));
	    }
	else
	{
	        file.append(SAVEGAMENAME);
	        file.append("%c.dsg");
	        file.append(myargv[p+1].charAt(0));

	}
	LoadGame(file.toString());
    }
	

    if ( gameaction != gameaction_t.ga_loadgame )
    {
	if (autostart || netgame)
	    InitNew (startskill, startepisode, startmap);
	else
	    StartTitle ();                // start up intro loop

    }
    
    // MAES: at this point everything should be set and initialized, so it's
    // time to make the players aware of the general status of Doom.
	for (int i=0;i<MAXPLAYERS;i++){
		players[i].updateStatus(this);
	}

    DoomLoop ();  // never returns
}

// Used in BuildTiccmd.
protected ticcmd_t   base=new ticcmd_t();

/**
 * G_BuildTiccmd
 * Builds a ticcmd from all of the available inputs
 * or reads it from the demo buffer. 
 * If recording a demo, write it out 
 */ 

 private void BuildTiccmd (ticcmd_t cmd) 
 { 
     int     i; 
     boolean strafe;
     boolean bstrafe; 
     int     speed;
     int     tspeed; 
     int     forward;
     int     side;
     

     //base = I_BaseTiccmd ();     // empty, or external driver
           // memcpy (cmd,base,sizeof(*cmd));
     base.copyTo(cmd);
     
     cmd.consistancy = 
     consistancy[consoleplayer][maketic%BACKUPTICS]; 

  
     strafe = gamekeydown[key_strafe] || mousebuttons(mousebstrafe) 
     || joybuttons(joybstrafe); 
     speed = (gamekeydown[key_speed] || joybuttons(joybspeed))?1:0;
  
     forward = side = 0;
     
     // use two stage accelerative turning
     // on the keyboard and joystick
     if (joyxmove < 0
     || joyxmove > 0  
     || gamekeydown[key_right]
     || gamekeydown[key_left]) 
     turnheld += ticdup; 
     else 
     turnheld = 0; 

     if (turnheld < SLOWTURNTICS) 
     tspeed = 2;             // slow turn 
     else 
     tspeed = speed;
     
     // let movement keys cancel each other out
     if (strafe) 
     { 
     if (gamekeydown[key_right]) 
     {
         // fprintf(stderr, "strafe right\n");
         side += sidemove[speed]; 
     }
     if (gamekeydown[key_left]) 
     {
         //  fprintf(stderr, "strafe left\n");
         side -= sidemove[speed]; 
     }
     if (joyxmove > 0) 
         side += sidemove[speed]; 
     if (joyxmove < 0) 
         side -= sidemove[speed]; 
  
     } 
     else 
     { 
     if (gamekeydown[key_right]) 
         cmd.angleturn -= angleturn[tspeed]; 
     if (gamekeydown[key_left]) 
         cmd.angleturn += angleturn[tspeed]; 
     if (joyxmove > 0) 
         cmd.angleturn -= angleturn[tspeed]; 
     if (joyxmove < 0) 
         cmd.angleturn += angleturn[tspeed]; 
     } 
  
     if (gamekeydown[key_up]) 
     {
     //System.err.print("up\n");
     forward += forwardmove[speed]; 
     }
     if (gamekeydown[key_down]) 
     {
     //System.err.print("down\n");
     forward -= forwardmove[speed]; 
     }
     if (joyymove < 0) 
     forward += forwardmove[speed]; 
     if (joyymove > 0) 
     forward -= forwardmove[speed]; 
     if (gamekeydown[key_straferight]) 
     side += sidemove[speed]; 
     if (gamekeydown[key_strafeleft]) 
     side -= sidemove[speed];
     
     // buttons
     cmd.chatchar = HU.dequeueChatChar(); 
  
     if (gamekeydown[key_fire] || mousebuttons(mousebfire) 
     || joybuttons(joybfire)) 
     cmd.buttons |= BT_ATTACK; 
  
     if (gamekeydown[key_use] || joybuttons(joybuse) ) 
     { 
     cmd.buttons |= BT_USE;
     // clear double clicks if hit use button 
     dclicks = 0;                   
     } 

     // chainsaw overrides 
     for (i=0 ; i<NUMWEAPONS-1 ; i++)        
     if (gamekeydown['1'+i]) 
     { 
         System.out.println("Attempting weapon change (building ticcmd)");
         cmd.buttons |= BT_CHANGE; 
         cmd.buttons |= i<<BT_WEAPONSHIFT; 
         break; 
     }
     
     // mouse
     if (mousebuttons(mousebforward)) 
     forward += forwardmove[speed];
     
     // forward double click (operator precedence? && over >
     if (mousebuttons(mousebforward) != (dclickstate!=0) && (dclicktime > 1) ) 
     { 
     dclickstate = mousebuttons(mousebforward)?1:0; 
     if (dclickstate!=0) 
         dclicks++; 
     if (dclicks == 2) 
     { 
         cmd.buttons |= BT_USE; 
         dclicks = 0; 
     } 
     else 
         dclicktime = 0; 
     } 
     else 
     { 
     dclicktime += ticdup; 
     if (dclicktime > 20) 
     { 
         dclicks = 0; 
         dclickstate = 0; 
     } 
     }
     
     // strafe double click
     bstrafe = mousebuttons(mousebstrafe) || joybuttons(joybstrafe); 
     if ((bstrafe != (dclickstate2!=0)) && dclicktime2 > 1 ) 
     { 
     dclickstate2 = bstrafe?1:0; 
     if (dclickstate2!=0) 
         dclicks2++; 
     if (dclicks2 == 2) 
     { 
         cmd.buttons |= BT_USE; 
         dclicks2 = 0; 
     } 
     else 
         dclicktime2 = 0; 
     } 
     else 
     { 
     dclicktime2 += ticdup; 
     if (dclicktime2 > 20) 
     { 
         dclicks2 = 0; 
         dclickstate2 = 0; 
     } 
     } 
  
     forward += mousey; 
     if (strafe) 
     side += mousex*2; 
     else 
     cmd.angleturn -= mousex*0x8; 

     mousex = mousey = 0; 
      
     if (forward > MAXPLMOVE()) 
     forward = MAXPLMOVE(); 
     else if (forward < -MAXPLMOVE()) 
     forward = -MAXPLMOVE(); 
     if (side > MAXPLMOVE()) 
     side = MAXPLMOVE(); 
     else if (side < -MAXPLMOVE()) 
     side = -MAXPLMOVE(); 
  
     cmd.forwardmove += forward; 
     cmd.sidemove += side;
     
     // special buttons
     if (sendpause) 
     { 
     sendpause = false; 
     cmd.buttons = BT_SPECIAL | BTS_PAUSE; 
     } 
  
     if (sendsave) 
     { 
     sendsave = false; 
     cmd.buttons = (char) (BT_SPECIAL | BTS_SAVEGAME | (savegameslot<<BTS_SAVESHIFT)); 
     } 
 } 
  

 //
 // G_DoLoadLevel 
 //
 //extern  gamestate_t     wipegamestate; 
  
 public void DoLoadLevel () 
 { 
     int             i; 

     // Set the sky map.
     // First thing, we have a dummy sky texture name,
     //  a flat. The data is in the WAD only because
     //  we look for an actual index, instead of simply
     //  setting one.
     TM.setSkyFlatNum(TM.FlatNumForName ( SKYFLATNAME ));

     // DOOM determines the sky texture to be used
     // depending on the current episode, and the game version.
     if (( gamemode == GameMode_t.commercial)
      || ( gamemission == GameMission_t.pack_tnt )
      || ( gamemission == GameMission_t.pack_plut ) )
     {
     TM.setSkyTexture(TM.TextureNumForName ("SKY3"));
     if (gamemap < 12)
         TM.setSkyTexture(TM.TextureNumForName ("SKY1"));
     else
         if (gamemap < 21)
             TM.setSkyTexture(TM.TextureNumForName ("SKY2"));
     }

     levelstarttic = gametic;        // for time calculation
     
     if (wipegamestate == gamestate_t.GS_LEVEL) 
     wipegamestate = gamestate_t.GS_MINUS_ONE;             // force a wipe 

     gamestate = gamestate_t.GS_LEVEL; 

     for (i=0 ; i<MAXPLAYERS ; i++) 
     { 
     if (playeringame[i] && players[i].playerstate == PST_DEAD) 
         players[i].playerstate = PST_REBORN; 
         // I don't give a shit if it's not super-duper optimal. 
     Arrays.fill(players[i].frags, 0);

     } 
          
     LL.SetupLevel (gameepisode, gamemap, 0, gameskill);    
     displayplayer = consoleplayer;      // view the guy you are playing    
     starttime = I.GetTime (); 
     gameaction = gameaction_t.ga_nothing; 
     //Z_CheckHeap ();
     
     // clear cmd building stuff
     Arrays.fill(gamekeydown, false); 
     joyxmove = joyymove = 0; 
     mousex = mousey = 0; 
     sendpause = sendsave = paused = false; 
     Arrays.fill (mousearray, false);
     Arrays.fill(joyarray, false); 
 } 
  
  
 /**
  * G_Responder  
  * Get info needed to make ticcmd_ts for the players.
  */
 
 public boolean Responder (event_t ev) 
 { 
     // allow spy mode changes even during the demo
     if (gamestate == gamestate_t.GS_LEVEL && ev.type == evtype_t.ev_keydown 
     && ev.data1 == KEY_F12 && (singledemo || !deathmatch) )
     {
     // spy mode 
     do 
     { 
         displayplayer++; 
         if (displayplayer == MAXPLAYERS) 
         displayplayer = 0; 
     } while (!playeringame[displayplayer] && displayplayer != consoleplayer); 
     return true; 
     }
     
     // any other key pops up menu if in demos
     if (gameaction == gameaction_t.ga_nothing && !singledemo && 
     (demoplayback || gamestate == gamestate_t.GS_DEMOSCREEN) 
     ) 
     { 
     if (ev.type == evtype_t.ev_keydown ||  
         (ev.type == evtype_t.ev_mouse && ev.data1!=0) || 
         (ev.type == evtype_t.ev_joystick && ev.data1!=0) ) 
     { 
         M.StartControlPanel (); 
         return true; 
     } 
     return false; 
     } 
  
     if (gamestate == gamestate_t.GS_LEVEL) 
     { 
 
     if (devparm && ev.type == evtype_t.ev_keydown && ev.data1 == ';') 
     { 
         DeathMatchSpawnPlayer (0); 
         return true; 
     } 
  
//automapactive=true;

     if (HU.Responder (ev)) 
         return true;    // chat ate the event 
     if (ST.Responder (ev)) 
         return true;    // status window ate it
     if (AM.Responder (ev)) 
         return true;    // automap ate it 

     } 
      
     if (gamestate == gamestate_t.GS_FINALE) 
     { 
     if (F.Responder (ev)) 
         return true;    // finale ate the event 
     } 
      
     switch (ev.type) 
     { 
       case ev_keydown: 
     if (ev.data1 == KEY_PAUSE) 
     { 
         sendpause = true; 
         return true; 
     } 
     if (ev.data1 <NUMKEYS) 
         gamekeydown[ev.data1] = true; 
     return true;    // eat key down events 
  
       case ev_keyup: 
     if (ev.data1 <NUMKEYS) 
         gamekeydown[ev.data1] = false; 
     return false;   // always let key up events filter down 
          
       case ev_mouse: 
     mousebuttons(0, ev.data1 & 1); 
     mousebuttons(1, ev.data1 & 2); 
     mousebuttons(2, ev.data1 & 4); 
     mousex = ev.data2*(mouseSensitivity+5)/10; 
     mousey = ev.data3*(mouseSensitivity+5)/10; 
     return true;    // eat events 
  
       case ev_joystick: 
     joybuttons(0, ev.data1 & 1); 
     joybuttons(1, ev.data1 & 2); 
     joybuttons(2,ev.data1 & 4); 
     joybuttons(3,ev.data1 & 8); 
     joyxmove = ev.data2; 
     joyymove = ev.data3; 
     return true;    // eat events 
  
       default: 
     break; 
     } 
  
     return false; 
 } 
  
  
 private final String turbomessage="is turbo!"; 
 
 /**
  * G_Ticker
  * Make ticcmd_ts for the players.
  */
 
 public void Ticker () 
 { 
     int     i;
     int     buf; 
     ticcmd_t   cmd;
     
     // do player reborns if needed
     for (i=0 ; i<MAXPLAYERS ; i++) 
     if (playeringame[i] && players[i].playerstate == PST_REBORN) 
         DoReborn (i);
     
     // do things to change the game state
     while (gameaction != gameaction_t.ga_nothing) 
     { 
     switch (gameaction) 
     { 
       case ga_loadlevel: 
         DoLoadLevel (); 
         break; 
       case ga_newgame: 
         DoNewGame (); 
         break; 
       case ga_loadgame: 
         DoLoadGame (); 
         break; 
       case ga_savegame: 
         DoSaveGame (); 
         break; 
       case ga_playdemo: 
         DoPlayDemo (); 
         break; 
       case ga_completed: 
         DoCompleted (); 
         break; 
       case ga_victory: 
         F.StartFinale (); 
         break; 
       case ga_worlddone: 
         DoWorldDone (); 
         break; 
       case ga_screenshot: 
         ScreenShot (); 
         gameaction = gameaction_t.ga_nothing; 
         break; 
       case ga_nothing: 
         break; 
     } 
     }
     
     // get commands, check consistancy,
     // and build new consistancy check
     buf = (gametic/ticdup)%BACKUPTICS; 
  
     for (i=0 ; i<MAXPLAYERS ; i++)
     {
     if (playeringame[i]) 
     { 
         cmd = players[i].cmd; 
         //System.out.println("Current command:"+cmd);
         
         //memcpy (cmd, &netcmds[i][buf], sizeof(ticcmd_t));
         netcmds[i][buf].copyTo(cmd);
  
         if (demoplayback) 
         ReadDemoTiccmd (cmd); 
         if (demorecording) 
         WriteDemoTiccmd (cmd);
         
         // check for turbo cheats
         if (cmd.forwardmove > TURBOTHRESHOLD 
         && ((gametic&31)==0) && ((gametic>>5)&3) == i )
         {
         
         //extern char *player_names[4];
         //sprintf (turbomessage, "%s is turbo!",player_names[i]);
         players[consoleplayer].message = hu.HU.player_names[i]+turbomessage;
         }
             
         if (netgame && !netdemo && (gametic%ticdup)==0 ) 
         { 
         if (gametic > BACKUPTICS 
             && consistancy[i][buf] != cmd.consistancy) 
         { 
             I.Error ("consistency failure (%i should be %i)",
                  cmd.consistancy, consistancy[i][buf]); 
         } 
         if (players[i].mo!=null) 
             consistancy[i][buf] = (short) players[i].mo.x; 
         else 
             consistancy[i][buf] = (short) RND.rndindex; 
         } 
     }
     }
     
     // check for special buttons
     for (i=0 ; i<MAXPLAYERS ; i++)
     {
     if (playeringame[i]) 
     { 
         if ((players[i].cmd.buttons & BT_SPECIAL)!=0) 
         { 
         switch (players[i].cmd.buttons & BT_SPECIALMASK) 
         { 
           case BTS_PAUSE: 
             paused ^= paused; 
             if (paused)
             S.PauseSound ();
             else
             S.ResumeSound (); 
             break; 
                      
           case BTS_SAVEGAME: 
             if (savedescription!=null) 
             savedescription=new String( "NET GAME"); 
             savegameslot =  
             (players[i].cmd.buttons & BTS_SAVEMASK)>>BTS_SAVESHIFT; 
             gameaction = gameaction_t.ga_savegame; 
             break; 
         } 
         } 
     }
     }
     
     // do main actions
     switch (gamestate) 
     { 
       case GS_LEVEL: 
     P.Ticker (); 
     ST.Ticker (); 
     AM.Ticker (); 
     HU.Ticker ();            
     break; 
      
       case GS_INTERMISSION: 
     WI.Ticker (); 
     break; 
              
       case GS_FINALE: 
     F.Ticker (); 
     break; 
  
       case GS_DEMOSCREEN: 
     PageTicker (); 
     break; 
     }        
 } 
  
  
 //
 // PLAYER STRUCTURE FUNCTIONS
 // also see P_SpawnPlayer in P_Things
 //

 /**
  * G_InitPlayer
  * Called at the start.
  * Called by the game initialization functions.
  *
  * MAES: looks like dead code. It's never called.
  *
  */
 
 private void InitPlayer (int player) 
 { 
     player_t   p; 
  
     // set up the saved info         
     p = players[player]; 
      
     // clear everything else to defaults 
     PlayerReborn (player); 
      
 } 
  
  

 /**
  * G_PlayerFinishLevel
  * Can when a player completes a level.
  */
 
 private final void PlayerFinishLevel (int player) 
 { 
     player_t   p; 
      
     p = players[player]; 
      
     Arrays.fill(p.powers, 0);
     Arrays.fill(p.cards,false);       
     p.mo.flags &= ~mobj_t.MF_SHADOW;     // cancel invisibility 
     p.extralight = 0;          // cancel gun flashes 
     p.fixedcolormap = 0;       // cancel ir gogles 
     p.damagecount = 0;         // no palette changes 
     p.bonuscount = 0; 
 } 
  

 /**
  * G_PlayerReborn
  * Called after a player dies 
  * almost everything is cleared and initialized 
  *
  *@param player
  */
 
 public void PlayerReborn (int player) 
 { 
     player_t   p; 
     int     i; 
     int[]     frags=new int [MAXPLAYERS]; 
     int     killcount;
     int     itemcount;
     int     secretcount; 
     
    // System.arraycopy(players[player].frags, 0, frags, 0, frags.length);
     // We save the player's frags here...
     C2JUtils.memcpy (frags,players[player].frags,frags.length); 
     killcount = players[player].killcount; 
     itemcount = players[player].itemcount; 
     secretcount = players[player].secretcount; 

     //MAES: we need to simulate an erasure, possibly without making
     // a new object.memset (p, 0, sizeof(*p));
     //players[player]=(player_t) player_t.nullplayer.clone();
     // players[player]=new player_t();
     p=players[player];
     p.reset();
  
     // And we copy the old frags into the "new" player. 
     C2JUtils.memcpy(players[player].frags, frags, players[player].frags.length); 
     
     players[player].killcount = killcount; 
     players[player].itemcount = itemcount; 
     players[player].secretcount = secretcount; 
  
     p.usedown = p.attackdown = true;  // don't do anything immediately 
     p.playerstate = PST_LIVE;       
     p.health[0] = MAXHEALTH; 
     p.readyweapon = p.pendingweapon = weapontype_t.wp_pistol; 
     p.weaponowned[weapontype_t.wp_fist.ordinal()] = true; 
     p.weaponowned[weapontype_t.wp_pistol.ordinal()] = true; 
     p.ammo[ammotype_t.am_clip.ordinal()] = 50; 
      
     for (i=0 ; i<NUMAMMO ; i++) 
     p.maxammo[i] = maxammo[i]; 
          
 }

 //
 // G_CheckSpot  
 // Returns false if the player cannot be respawned
 // at the given mapthing_t spot  
 // because something is occupying it 
 //
 //void P_SpawnPlayer (mapthing_t* mthing); 
  
 private boolean
 CheckSpot
 ( int       playernum,
   mapthing_t   mthing ) 
 { 
     int     x,y; // fixed_t 
     subsector_t    ss; 
     int        an; // angle 
     mobj_t     mo; 
     
     
     if (players[playernum].mo==null)
     {
     // first spawn of level, before corpses
     for (int i=0 ; i<playernum ; i++)
         if (players[i].mo.x == mthing.x << FRACBITS
         && players[i].mo.y == mthing.y << FRACBITS)
         return false;   
     return true;
     }
         
     x = mthing.x << FRACBITS; 
     y = mthing.y << FRACBITS; 
      
     if (!P.CheckPosition (players[playernum].mo, x, y) )
         return false;
     
     // flush an old corpse if needed 
     if (bodyqueslot >= BODYQUESIZE) 
     P.RemoveMobj (bodyque[bodyqueslot%BODYQUESIZE]); 
     bodyque[bodyqueslot%BODYQUESIZE] = players[playernum].mo; 
     bodyqueslot++; 
     
     // spawn a teleport fog 
     ss = R.PointInSubsector (x,y); 
     // Angles stored in things are supposed to be "sanitized" against rollovers.
     an = (int) (( ANG45 * (mthing.angle/45) ) >>> ANGLETOFINESHIFT); 
  
     mo = P.SpawnMobj (x+20*finecosine[an], y+20*finesine[an] 
               , ss.sector.floorheight 
               , mobjtype_t.MT_TFOG); 
      
     if (players[consoleplayer].viewz != 1) ; 
     S.StartSound (mo, sfxenum_t.sfx_telept);  // don't start sound on first frame 
  
     return true; 
 } 


 //
 // G_DeathMatchSpawnPlayer 
 // Spawns a player at one of the random death match spots 
 // called at level load and each death 
 //
 public void DeathMatchSpawnPlayer (int playernum) 
 { 
     int             i,j; 
     int             selections; 
      
     selections = deathmatch_p; 
     if (selections < 4) 
     I.Error ("Only %i deathmatch spots, 4 required", selections); 
  
     for (j=0 ; j<20 ; j++) 
     { 
     i = RND.P_Random() % selections; 
     if (CheckSpot (playernum, deathmatchstarts[i]) ) 
     { 
         deathmatchstarts[i].type = (short) (playernum+1); 
         P.SpawnPlayer (deathmatchstarts[i]); 
         return; 
     } 
     } 
  
     // no good spot, so the player will probably get stuck
     // MAES: seriously, fuck him.
     P.SpawnPlayer (playerstarts[playernum]); 
 } 

 //
 // G_DoReborn 
 // 
 
 public void DoReborn (int playernum) 
 { 
     int                             i; 
      
     if (!netgame)
     {
     // reload the level from scratch
     gameaction = gameaction_t.ga_loadlevel;  
     }
     else 
     {
     // respawn at the start

     // first dissasociate the corpse 
     players[playernum].mo.player = null;   
          
     // spawn at random spot if in death match 
     if (deathmatch) 
     { 
         DeathMatchSpawnPlayer (playernum); 
         return; 
     } 
          
     if (CheckSpot (playernum, playerstarts[playernum]) ) 
     { 
         P.SpawnPlayer (playerstarts[playernum]); 
         return; 
     }
     
     // try to spawn at one of the other players spots 
     for (i=0 ; i<MAXPLAYERS ; i++)
     {
         if (CheckSpot (playernum, playerstarts[i]) ) 
         { 
         playerstarts[i].type = (short) (playernum+1); // fake as other player 
         P.SpawnPlayer (playerstarts[i]); 
         playerstarts[i].type = (short) (i+1);     // restore 
         return; 
         }       
         // he's going to be inside something.  Too bad.
         // MAES: Yeah, they're like, fuck him.
     }
     P.SpawnPlayer (playerstarts[playernum]); 
     } 
 } 
  
  
 public void ScreenShot () 
 { 
     gameaction = gameaction_t.ga_screenshot; 
 } 
  


/** DOOM Par Times [4][10] */
 final int[][] pars = 
 { 
     {0}, 
     {0,30,75,120,90,165,180,180,30,165}, 
     {0,90,90,90,120,90,360,240,30,170}, 
     {0,90,45,90,150,90,90,165,30,135} 
 }; 

 /** DOOM II Par Times */
 final int[] cpars =
 {
     30,90,120,120,90,150,120,120,270,90,    //  1-10
     210,150,150,150,210,150,420,150,210,150,    // 11-20
     240,150,180,150,150,300,330,420,300,180,    // 21-30
     120,30                  // 31-32
 };
  

 //
 // G_DoCompleted 
 //
 boolean     secretexit; 
  
 public final void ExitLevel () 
 { 
     secretexit = false; 
     gameaction = gameaction_t.ga_completed; 
 } 

 // Here's for the german edition.
 public void SecretExitLevel () 
 { 
     // IF NO WOLF3D LEVELS, NO SECRET EXIT!
     if ( (gamemode == GameMode_t.commercial)
       && (W.CheckNumForName("map31")<0))
     secretexit = false;
     else
     secretexit = true; 
     gameaction =  gameaction_t.ga_completed; 
 } 
  
 private void DoCompleted () 
 { 
     int             i; 
      
     gameaction =  gameaction_t.ga_nothing; 
  
     for (i=0 ; i<MAXPLAYERS ; i++) 
     if (playeringame[i]) 
         PlayerFinishLevel (i);        // take away cards and stuff 
      
     if (automapactive) 
     AM.Stop (); 
     
     if ( gamemode != GameMode_t.commercial)
     switch(gamemap)
     {
       case 8:
         gameaction =  gameaction_t.ga_victory;
         return;
       case 9: 
         for (i=0 ; i<MAXPLAYERS ; i++) 
         players[i].didsecret = true; 
         break;
     }
         
 //#if 0  Hmmm - why?
     if ( (gamemap == 8)
      && (gamemode != GameMode_t.commercial) ) 
     {
     // victory 
     gameaction =  gameaction_t.ga_victory; 
     return; 
     } 
      
     if ( (gamemap == 9)
      && (gamemode != GameMode_t.commercial) ) 
     {
     // exit secret level 
     for (i=0 ; i<MAXPLAYERS ; i++) 
         players[i].didsecret = true; 
     } 
 //#endif
     
      
     wminfo.didsecret = players[consoleplayer].didsecret; 
     wminfo.epsd = gameepisode -1; 
     wminfo.last = gamemap -1;
     
     // wminfo.next is 0 biased, unlike gamemap
     if ( gamemode == GameMode_t.commercial)
     {
     if (secretexit)
         switch(gamemap)
         {
           case 15: wminfo.next = 30; break;
           case 31: wminfo.next = 31; break;
         }
     else
         switch(gamemap)
         {
           case 31:
           case 32: wminfo.next = 15; break;
           default: wminfo.next = gamemap;
         }
     }
     else
     {
     if (secretexit) 
         wminfo.next = 8;    // go to secret level 
     else if (gamemap == 9) 
     {
         // returning from secret level 
         switch (gameepisode) 
         { 
           case 1: 
         wminfo.next = 3; 
         break; 
           case 2: 
         wminfo.next = 5; 
         break; 
           case 3: 
         wminfo.next = 6; 
         break; 
           case 4:
         wminfo.next = 2;
         break;
         }                
     } 
     else 
         wminfo.next = gamemap;          // go to next level 
     }
          
     wminfo.maxkills = totalkills; 
     wminfo.maxitems = totalitems; 
     wminfo.maxsecret = totalsecret; 
     wminfo.maxfrags = 0; 
     if ( gamemode == GameMode_t.commercial )
     wminfo.partime = 35*cpars[gamemap-1]; 
     else
     wminfo.partime = 35*pars[gameepisode][gamemap]; 
     wminfo.pnum = consoleplayer; 
  
     for (i=0 ; i<MAXPLAYERS ; i++) 
     { 
     wminfo.plyr[i].in = playeringame[i]; 
     wminfo.plyr[i].skills = players[i].killcount; 
     wminfo.plyr[i].sitems = players[i].itemcount; 
     wminfo.plyr[i].ssecret = players[i].secretcount; 
     wminfo.plyr[i].stime = leveltime;
     C2JUtils.memcpy (wminfo.plyr[i].frags, players[i].frags 
         , wminfo.plyr[i].frags.length); 
     } 
  
     gamestate = gamestate_t.GS_INTERMISSION; 
     viewactive = false; 
     automapactive = false; 
  
     if (statcopy!=null)
         C2JUtils.memcpy (statcopy, wminfo,1);
     
     WI.Start (wminfo); 
 } 


 //
 // G_WorldDone 
 //
 private void WorldDone () 
 { 
     gameaction = gameaction_t.ga_worlddone; 

     if (secretexit) 
     players[consoleplayer].didsecret = true; 

     if ( gamemode == GameMode_t.commercial )
     {
     switch (gamemap)
     {
       case 15:
       case 31:
         if (!secretexit)
         break;
       case 6:
       case 11:
       case 20:
       case 30:
         F.StartFinale ();
         break;
     }
     }
 } 
  
 public void DoWorldDone () 
 {        
     gamestate = gamestate_t.GS_LEVEL; 
     gamemap = wminfo.next+1; 
     DoLoadLevel (); 
     gameaction = gameaction_t.ga_nothing; 
     viewactive = true; 
 } 
  


 //
 // G_InitFromSavegame
 // Can be called by the startup code or the menu task. 
 //
 //extern boolean setsizeneeded;
 //void R_ExecuteSetViewSize (void);

 String    savename;

 public void LoadGame (String name) 
 { 
     savename=new String(name); 
     gameaction = gameaction_t.ga_loadgame; 
 } 
  
 

 /** This is fugly. Making a "savegame object" will make at least certain comparisons
  *  easier, and avoid writing code twice.
  */

 private void DoLoadGame () 
 { 
     /*
     int     length; 
     int     i; 
     int     a,b,c; 
     char[]    vcheck=new char[VERSIONSIZE]; 
     StringBuffer buf=new StringBuffer();
     DoomSaveGame dsg;
     
     gameaction = gameaction_t.ga_nothing; 
      
     length = M.ReadFile (savename, savebuffer); 
     save_p = SAVESTRINGSIZE;
     
     // skip the description field 
     //memset (vcheck,0,sizeof(vcheck));
     //sprintf (vcheck,"version %i",VERSION);
     buf.append("version ");
     buf.append(VERSION);
     buf.getChars(0, buf.length(), vcheck, 0);

     if (C2JUtils.strcmp (save_p, vcheck)) 
     return;             // bad version 
     save_p += VERSIONSIZE; 
              
     gameskill = *save_p++; 
     gameepisode = *save_p++; 
     gamemap = *save_p++; 
     for (i=0 ; i<MAXPLAYERS ; i++) 
     playeringame[i] = *save_p++; 

     // load a base level 
     G_InitNew (gameskill, gameepisode, gamemap); 
  
     // get the times 
     a = *save_p++; 
     b = *save_p++; 
     c = *save_p++; 
     leveltime = (a<<16) + (b<<8) + c; 
      
     // dearchive all the modifications
     P_UnArchivePlayers (); 
     P_UnArchiveWorld (); 
     P_UnArchiveThinkers (); 
     P_UnArchiveSpecials (); 
  
     if (*save_p != 0x1d) 
     I_Error ("Bad savegame");
     
     // done 
     Z_Free (savebuffer); 
  
     if (setsizeneeded)
     R_ExecuteSetViewSize ();
     
     // draw the pattern into the back screen
     R_FillBackScreen ();
     */   
 } 
  

 //
 // G_SaveGame
 // Called by the menu task.
 // Description is a 24 byte text string 
 //
 public void
 SaveGame
 ( int   slot,
   String description ) 
 { 
     savegameslot = slot; 
     savedescription=new String(description); 
     sendsave = true; 
 } 
  
 private void DoSaveGame () 
 { 
     /*
     String    name; 
     char[]    name2=new char[VERSIONSIZE]; 
     String   description; 
     int     length; 
     int     i; 
     
     if (M_CheckParm("-cdrom"))
     name="c:\\doomdata\\"+SAVEGAMENAME+"%d.dsg");
     else
     sprintf (name,SAVEGAMENAME"%d.dsg",savegameslot); 
     description = savedescription; 
      
     save_p = savebuffer = screens[1]+0x4000; 
      
     memcpy (save_p, description, SAVESTRINGSIZE); 
     save_p += SAVESTRINGSIZE; 
     memset (name2,0,sizeof(name2)); 
     sprintf (name2,"version %i",VERSION); 
     memcpy (save_p, name2, VERSIONSIZE); 
     save_p += VERSIONSIZE; 
      
     *save_p++ = gameskill; 
     *save_p++ = gameepisode; 
     *save_p++ = gamemap; 
     for (i=0 ; i<MAXPLAYERS ; i++) 
     *save_p++ = playeringame[i]; 
     *save_p++ = leveltime>>16; 
     *save_p++ = leveltime>>8; 
     *save_p++ = leveltime; 
  
     P_ArchivePlayers (); 
     P_ArchiveWorld (); 
     P_ArchiveThinkers (); 
     P_ArchiveSpecials (); 
      
     *save_p++ = 0x1d;       // consistancy marker 
      
     length = save_p - savebuffer; 
     if (length > SAVEGAMESIZE) 
     I_Error ("Savegame buffer overrun"); 
     M_WriteFile (name, savebuffer, length); 
     gameaction = ga_nothing; 
     savedescription[0] = 0;      
      
     players[consoleplayer].message = GGSAVED; 

     // draw the pattern into the back screen
     R_FillBackScreen ();    
     */
 } 
  


 skill_t d_skill; 
 int     d_episode; 
 int     d_map; 
  
 public void
 DeferedInitNew
 ( skill_t   skill,
   int       episode,
   int       map) 
 { 
     d_skill = skill; 
     d_episode = episode; 
     d_map = map; 
     gameaction = gameaction_t.ga_newgame; 
 } 


 public void DoNewGame () 
 {
     demoplayback = false; 
     netdemo = false;
     netgame = false;
     deathmatch = false;
     playeringame[1] = playeringame[2] = playeringame[3] = false;
     respawnparm = false;
     fastparm = false;
     nomonsters = false;
     consoleplayer = 0;
     InitNew (d_skill, d_episode, d_map); 
     gameaction = gameaction_t.ga_nothing; 
 } 

 
 /**
  * G_InitNew
  * Can be called by the startup code or the menu task,
  * consoleplayer, displayplayer, playeringame[] should be set. 
  */
 
 public void InitNew
 ( skill_t   skill,
   int       episode,
   int       map ) 
 { 
     int             i; 
      
     if (paused) 
     { 
     paused = false; 
     S.ResumeSound (); 
     } 
     

     if (skill.ordinal() > skill_t.sk_nightmare.ordinal()) 
     skill = skill_t.sk_nightmare;


     // This was quite messy with SPECIAL and commented parts.
     // Supposedly hacks to make the latest edition work.
     // It might not work properly.
     if (episode < 1)
       episode = 1; 

     if ( gamemode == GameMode_t.retail )
     {
       if (episode > 4)
     episode = 4;
     }
     else if ( gamemode == GameMode_t.shareware )
     {
       if (episode > 1) 
        episode = 1; // only start episode 1 on shareware
     }  
     else
     {
       if (episode > 3)
     episode = 3;
     }
     

   
     if (map < 1) 
     map = 1;
     
     if ( (map > 9)
      && ( gamemode != GameMode_t.commercial) )
       map = 9; 
          
     RND.ClearRandom (); 
      
     if (skill == skill_t.sk_nightmare || respawnparm )
     respawnmonsters = true;
     else
     respawnmonsters = false;
         
     // If on nightmare/fast monsters make everything MOAR pimp.
     
     if (fastparm || (skill == skill_t.sk_nightmare && gameskill != skill_t.sk_nightmare) )
     { 
     for (i=statenum_t.S_SARG_RUN1.ordinal() ; i<=statenum_t.S_SARG_PAIN2.ordinal() ; i++) 
         states[i].tics >>= 1; 
     mobjinfo[mobjtype_t.MT_BRUISERSHOT.ordinal()].speed = 20*FRACUNIT; 
     mobjinfo[mobjtype_t.MT_HEADSHOT.ordinal()].speed = 20*FRACUNIT; 
     mobjinfo[mobjtype_t.MT_TROOPSHOT.ordinal()].speed = 20*FRACUNIT; 
     } 
     else if (skill != skill_t.sk_nightmare && gameskill == skill_t.sk_nightmare) 
     { 
     for (i=statenum_t.S_SARG_RUN1.ordinal() ; i<=statenum_t.S_SARG_PAIN2.ordinal() ; i++) 
         states[i].tics <<= 1; 
     mobjinfo[mobjtype_t.MT_BRUISERSHOT.ordinal()].speed = 15*FRACUNIT; 
     mobjinfo[mobjtype_t.MT_HEADSHOT.ordinal()].speed = 10*FRACUNIT; 
     mobjinfo[mobjtype_t.MT_TROOPSHOT.ordinal()].speed = 10*FRACUNIT; 
     } 
      
              
     // force players to be initialized upon first level load         
     for (i=0 ; i<MAXPLAYERS ; i++) 
     players[i].playerstate = PST_REBORN; 
  
     usergame = true;                // will be set false if a demo 
     paused = false; 
     demoplayback = false; 
     automapactive = false; 
     viewactive = true; 
     gameepisode = episode; 
     gamemap = map; 
     gameskill = skill; 
  
     viewactive = true;
     
     // set the sky map for the episode
     if ( gamemode == GameMode_t.commercial)
     {
     TM.setSkyTexture(TM.TextureNumForName ("SKY3"));
     if (gamemap < 12)
         TM.setSkyTexture(TM.TextureNumForName ("SKY1"));
     else
         if (gamemap < 21)
             TM.setSkyTexture(TM.TextureNumForName ("SKY2"));
     }
     else
     switch (episode) 
     { 
       case 1: 
           TM.setSkyTexture(TM.TextureNumForName ("SKY1")); 
         break; 
       case 2: 
           TM.setSkyTexture(TM.TextureNumForName ("SKY2"));
         break; 
       case 3: 
           TM.setSkyTexture(TM.TextureNumForName ("SKY3")); 
         break; 
       case 4:   // Special Edition sky
           TM.setSkyTexture(TM.TextureNumForName ("SKY4"));
         break;
     } 
  
     DoLoadLevel (); 
 } 
  

 //
 // DEMO RECORDING 
 // 
 protected static final int DEMOMARKER =0x80;


public  void ReadDemoTiccmd (ticcmd_t cmd) 
 { 
     if (demobuffer[demo_p] == DEMOMARKER) 
     {
     // end of demo data stream 
     CheckDemoStatus (); 
     return; 
     } 
     cmd.forwardmove = (demobuffer[demo_p++]); 
     cmd.sidemove = (demobuffer[demo_p++]); 
     cmd.angleturn = (short) (C2JUtils.toUnsignedByte(demobuffer[demo_p++])<<8); 
     cmd.buttons = (char) (C2JUtils.toUnsignedByte(demobuffer[demo_p++])); 
 } 


 public void WriteDemoTiccmd (ticcmd_t cmd) 
 { 
     if (gamekeydown['q'])           // press q to end demo recording 
     CheckDemoStatus (); 
     demobuffer[demo_p++] = cmd.forwardmove; 
     demobuffer[demo_p++] = cmd.sidemove; 
     demobuffer[demo_p++] = (byte) ((cmd.angleturn+128)>>8); 
     demobuffer[demo_p++] = (byte) cmd.buttons; 
     demo_p -= 4; 
     if (demo_p > demoend - 16)
     {
     // no more space 
     CheckDemoStatus (); 
     return; 
     } 
     
     ReadDemoTiccmd (cmd);         // make SURE it is exactly the same 
 } 
  
  
  
 /**
 * G_RecordDemo 
 */ 
 public void RecordDemo (String name) 
 { 
     int             i; 
     int             maxsize;
     
     StringBuffer buf=new StringBuffer();
     usergame = false; 
     buf.append(name); 
     buf.append(".lmp");
     demoname=buf.toString();
     maxsize = 0x20000;
     i = CheckParm ("-maxdemo");
     if (i!=0 && i<myargc-1)
     maxsize = Integer.parseInt(myargv[i+1])*1024;
     demobuffer = new byte[maxsize]; 
     demoend = maxsize;
      
     demorecording = true; 
 } 
  
  
 public void BeginRecording () 
 { 
     int             i; 
         
     demo_p = 0;
     
     demobuffer[demo_p++] = (byte) VERSION;
     demobuffer[demo_p++] = (byte) gameskill.ordinal(); 
     demobuffer[demo_p++] = (byte) gameepisode; 
     demobuffer[demo_p++] = (byte) gamemap; 
     demobuffer[demo_p++] = (byte) ((deathmatch)?1:0); 
     demobuffer[demo_p++] = (byte) (respawnparm?1:0);
     demobuffer[demo_p++] = (byte) (fastparm?1:0);
     demobuffer[demo_p++] = (byte) (nomonsters?1:0);
     demobuffer[demo_p++] = (byte) consoleplayer;
      
     for (i=0 ; i<MAXPLAYERS ; i++) 
     demobuffer[demo_p++] = (byte) (playeringame[i]?1:0);         
 } 
  



 String   defdemoname;



  
 /**
  * G_PlayDemo 
  */
 
 public void DeferedPlayDemo (String name) 
 { 
     defdemoname = name; 
     // TODO: set to nothing for now.
     gameaction = gameaction_t.ga_nothing; 
 } 
  
 public void DoPlayDemo () 
  { 
	 /*
	 skill_t skill; 
     int             i, episode, map; 
      
     gameaction = gameaction_t.ga_nothing; 
     demobuffer = W.CacheLumpNameAsRawBytes(defdemoname.toUpperCase(), PU_STATIC);
     demo_p = 0;
     if ( demobuffer[demo_p] != VERSION)
     {
       System.err.println("Demo is from a different game version!\n");
       System.err.println("Read "+demobuffer[demo_p]);
       gameaction = gameaction_t.ga_nothing;
       return;
     }
     demo_p++;
     skill = skill_t.values()[demobuffer[demo_p++]]; 
     episode = demobuffer[demo_p++]; 
     map = demobuffer[demo_p++]; 
     deathmatch = demobuffer[demo_p++]==0;
     respawnparm = demobuffer[demo_p++]==0;
     fastparm = demobuffer[demo_p++]==0;
     nomonsters = demobuffer[demo_p++]==0;
     consoleplayer = demobuffer[demo_p++];
     
     for (i=0 ; i<MAXPLAYERS ; i++) 
     playeringame[i] = demobuffer[demo_p++]==0; 
     if (playeringame[1]) 
     { 
     netgame = true; 
     netdemo = true; 
     }

     // don't spend a lot of time in loadlevel 
     precache = false;
     InitNew (skill, episode, map); 
     precache = true; 

     usergame = false; 
     demoplayback = true; 
     */
 } 

 //
 // G_TimeDemo 
 //
 public void TimeDemo (String name) 
 {    
     nodrawers = CheckParm ("-nodraw")!=0; 
     noblit = CheckParm ("-noblit")!=0; 
     timingdemo = true; 
     singletics = true; 

     defdemoname = name; 
     
     gameaction = gameaction_t.ga_playdemo; 
 } 
  
  
 /**
 =================== 
 = 
 = G_CheckDemoStatus 
 = 
 = Called after a death or level completion to allow demos to be cleaned up 
 = Returns true if a new demo loop action will take place 
 =================== 
 */ 
  
 boolean CheckDemoStatus () 
 { 
     int             endtime; 
      
     if (timingdemo) 
     { 
     endtime = I.GetTime (); 
     I.Error ("timed %i gametics in %i realtics",gametic 
          , endtime-starttime); 
     } 
      
     if (demoplayback) 
     { 
     if (singledemo) 
         I.Quit (); 
              
    // Z_ChangeTag (demobuffer, PU_CACHE); 
     demoplayback = false; 
     netdemo = false;
     netgame = false;
     deathmatch = false;
     playeringame[1] = playeringame[2] = playeringame[3] = false;
     respawnparm = false;
     fastparm = false;
     nomonsters = false;
     consoleplayer = 0;
     AdvanceDemo (); 
     return true; 
     } 
  
     if (demorecording) 
     { 
     demobuffer[demo_p++] = (byte) DEMOMARKER; 
     // TODO: M.WriteFile (demoname, demobuffer, demo_p); 
     //Z_Free (demobuffer); 
     demorecording = false; 
     I.Error ("Demo %s recorded",demoname); 
     } 
      
     return false; 
 } 
  

public DoomMain(){
	// Init game status...
	super();
    this.I=new DoomSystem();
    I.Init();
    gamestate=gamestate_t.GS_DEMOSCREEN;
}

/**
 * Since this is a fully OO implementation, we need a way to create
 * the instances of the Refresh daemon, the Playloop, the Wadloader 
 * etc. which however are now completely independent of each other,
 * and are typically only passed context when instantiated.
 * 
 *  If you instantiate one too early, it will have null context.
 *  
 *  The trick is to construct objects in the correct order
 *  
 *  FIXME: Probably I should add a sort of deferred status update?
 * 
 * */

public void Init(){
    // Random number generator.
    
	this.DM=this;
	this.DNI=new DummyNetworkDriver(this);
	this.RND=new random();    
    // In primis, the video renderer.
    this.V=new BufferedRenderer(SCREENWIDTH,SCREENHEIGHT);
    this.S=new DummySoundDriver();
    this.W=new WadLoader(this.I);
    // It's better if these are supplied externally.
    // In secundis, the Wad Loader

    
    this.WIPE=new Wiper(this);   
    
    // Then the menu...
    this.HU=new HU(this);
    this.M=new Menu(this);
    this.LL=new LevelLoader(this);
    this.R=new UnifiedRenderer(this);
    this.P=new Actions(this);

    this.ST=new StatusBar(this);
    this.AM=new Map(this);
    this.TM=new SimpleTextureManager(this);
    this.SM=this.R;

    this.LL.updateStatus(this);
    this.P.updateStatus(this);
    this.M.updateStatus(this);
    this.HU.updateStatus(this);
    this.R.updateStatus(this);
    
    
}



/* Since it's so intimately tied, it's less troublesome to merge the "main" and "network"
 *  code. 
 * 
 */


/** To be initialized by the DoomNetworkingInterface via a setter */
//private  doomcom_t   doomcom;   
//private  doomdata_t  netbuffer;      // points inside doomcom
private StringBuilder sb=new StringBuilder();


//
// NETWORKING
//
// gametic is the tic about to (or currently being) run
// maketic is the tick that hasn't had control made for it yet
// nettics[] has the maketics for all players 
//
// a gametic cannot be run until nettics[] > gametic for all players
//

//ticcmd_t[]  localcmds= new ticcmd_t[BACKUPTICS];

//ticcmd_t [][]       netcmds=new ticcmd_t [MAXPLAYERS][BACKUPTICS];
int[]           nettics=new int[MAXNETNODES];
boolean[]       nodeingame=new boolean[MAXNETNODES];        // set false as nodes leave game
boolean[]       remoteresend=new boolean[MAXNETNODES];      // set when local needs tics
int[]       resendto=new int[MAXNETNODES];          // set when remote needs tics
int[]       resendcount=new int[MAXNETNODES];

int[]       nodeforplayer=new int[MAXPLAYERS];

int             maketic;
int     lastnettic;
int     skiptics;
private int     ticdup;


public int getTicdup() {
	return ticdup;
}


public void setTicdup(int ticdup) {
	this.ticdup = ticdup;
}






int     maxsend; // BACKUPTICS/(2*ticdup)-1;


//void D_ProcessEvents (void); 
//void G_BuildTiccmd (ticcmd_t *cmd); 
//void D_DoAdvanceDemo (void);
 
boolean     reboundpacket;
doomdata_t  reboundstore;



// 
//
//123

/** MAES: interesting. After testing it was found to return the following size:
 *  (8*(netbuffer.numtics+1));
 */

int NetbufferSize ()
{
//    return (int)(((doomdata_t)0).cmds[netbuffer.numtics]);
    return (8*(netbuffer.numtics+1));
}


protected long NetbufferChecksum ()
{
    long        c;
    int     i,l;

    c = 0x1234567L;

    // FIXME -endianess?
if (NORMALUNIX)
    return 0;           // byte order problems


    /* Here it was trying to get the length of a doomdata_t struct up to retransmit from.
     * l = (NetbufferSize () - (int)&(((doomdata_t *)0)->retransmitfrom))/4;
     * (int)&(((doomdata_t *)0)->retransmitfrom) evaluates to "4"
     * Therefore, l= (netbuffersize - 4)/4
     * 
     */
    l = (NetbufferSize () - 4)/4;
    for (i=0 ; i<l ; i++)
            // TODO: checksum would be better computer in the netbuffer itself.
            // The C code actually takes all fields into account.
    c += 0;// TODO: (netbuffer->retransmitfrom)[i] * (i+1);

    return c & NCMD_CHECKSUM;
}
//
//
//
protected int ExpandTics (int low)
{
    int delta;
    
    delta = low - (maketic&0xff);
    
    if (delta >= -64 && delta <= 64)
    return (maketic&~0xff) + low;
    if (delta > 64)
    return (maketic&~0xff) - 256 + low;
    if (delta < -64)
    return (maketic&~0xff) + 256 + low;
        
    I.Error ("ExpandTics: strange value %i at maketic %i",low,maketic);
    return 0;
}



//
// HSendPacket
//
void
HSendPacket
 (int   node,
  int   flags ) 
{
    netbuffer.checksum = (int) (NetbufferChecksum () | flags);

    if (node==0)
    {
    reboundstore = netbuffer;
    reboundpacket = true;
    return;
    }

    if (DM.demoplayback)
    return;

    if (!DM.netgame)
    I.Error ("Tried to transmit to another node");
        
    doomcom.command = CMD_SEND;
    doomcom.remotenode = (short) node;
    doomcom.datalength = (short) NetbufferSize ();
    
    if (DM.debugfile!=null)
    {
    int     i;
    int     realretrans;
    if (flags(netbuffer.checksum , NCMD_RETRANSMIT))
        realretrans = ExpandTics (netbuffer.retransmitfrom);
    else
        realretrans = -1;

    DM.debugfile.writeString("send ("+ExpandTics(netbuffer.starttic)+", "+netbuffer.numtics + ", R "+
        realretrans+ "["+ doomcom.datalength+"]");
    
    for (i=0 ; i<doomcom.datalength ; i++)
        
        // TODO: get a serialized string representation.
        DM.debugfile.writeString(netbuffer.toString()+"\n");
        }

    DNI.NetCmd ();
}

//
// HGetPacket
// Returns false if no packet is waiting
//
private boolean HGetPacket () 
{   
    // Fugly way of "clearing" the buffer.
    sb.setLength(0);
    if (reboundpacket)
    {
    // FIXME: MAES: this looks like a struct copy 
    netbuffer.copyFrom(reboundstore);
    doomcom.remotenode = 0;
    reboundpacket = false;
    return true;
    }

    // If not actually a netgame (e.g. single player, demo) return.
    if (!DM.netgame)
    return false;

    if (DM.demoplayback)
    return false;
        
    doomcom.command = CMD_GET;
    DNI.NetCmd ();
    
    // Invalid node?
    if (doomcom.remotenode == -1)
    return false;

    if (doomcom.datalength != NetbufferSize ())
    {
    if (eval(debugfile))
        debugfile.writeString("bad packet length "+doomcom.datalength+"\n");
        return false;
    }
    
    if (NetbufferChecksum () != (netbuffer.checksum&NCMD_CHECKSUM) )
    {
    if (eval(debugfile))
        debugfile.writeString("bad packet checksum\n");
    return false;
    }

    if (eval(debugfile))
    {
    int     realretrans;
    int i;
            
    if (flags(netbuffer.checksum , NCMD_SETUP))
        debugfile.writeString("setup packet\n");
    else
    {
        if (flags(netbuffer.checksum , NCMD_RETRANSMIT))
        realretrans = ExpandTics (netbuffer.retransmitfrom);
        else
        realretrans = -1;
        
        sb.append("get ");
        sb.append(doomcom.remotenode);
        sb.append(" = (");
        sb.append(ExpandTics(netbuffer.starttic));
        sb.append(" + ");
        sb.append(netbuffer.numtics);
        sb.append(", R ");
        sb.append(realretrans);
        sb.append(")[");
        sb.append(doomcom.datalength);
        sb.append("]");
        
        debugfile.writeString(sb.toString());
        
        // Trick: force update of internal buffer.
        netbuffer.pack();
        
        /* TODO: Could it be actually writing stuff beyond the boundaries of a single doomdata object?
         * A doomcom object has a lot of header info, and a single "raw" data placeholder, which by now
         * should be inside netbuffer....right?
         * 
         * 
         */
        
        try{
        for (i=0 ; i<doomcom.datalength ; i++) {
            debugfile.writeString(Integer.toHexString(netbuffer.cached()[i]));
            debugfile.writeChar( '\n');
            }
            }
        catch( IOException e){
        // "Drown" IOExceptions here.
        }
    }
    }
    return true;    
}


//
// GetPackets
//
StringBuilder exitmsg=new StringBuilder(80);

public void GetPackets () 
{
    int     netconsole;
    int     netnode;
    ticcmd_t    src, dest;
    int     realend;
    int     realstart;
             
    while ( HGetPacket() )
    {
    if (flags(netbuffer.checksum , NCMD_SETUP))
        continue;       // extra setup packet
            
    netconsole = netbuffer.player & ~PL_DRONE;
    netnode = doomcom.remotenode;
    
    // to save bytes, only the low byte of tic numbers are sent
    // Figure out what the rest of the bytes are
    realstart = ExpandTics (netbuffer.starttic);        
    realend = (realstart+netbuffer.numtics);
    
    // check for exiting the game
    if (flags(netbuffer.checksum , NCMD_EXIT))
    {
        if (!nodeingame[netnode])
        continue;
        nodeingame[netnode] = false;
        playeringame[netconsole] = false;
        exitmsg.insert(0, "Player 1 left the game");
        exitmsg.setCharAt(7,(char) (exitmsg.charAt(7)+netconsole));
        players[consoleplayer].message = exitmsg.toString();
        if (demorecording)
        DM.CheckDemoStatus ();
        continue;
    }
    
    // check for a remote game kill
    if (flags(netbuffer.checksum , NCMD_KILL))
        I.Error ("Killed by network driver");

    nodeforplayer[netconsole] = netnode;
    
    // check for retransmit request
    if ( resendcount[netnode] <= 0 
         && flags(netbuffer.checksum , NCMD_RETRANSMIT) )
    {
        resendto[netnode] = ExpandTics(netbuffer.retransmitfrom);
        if (eval(debugfile)){
        sb.setLength(0);
        sb.append("retransmit from ");
        sb.append(resendto[netnode]);
        sb.append('\n');
        debugfile.writeString(sb.toString());
        resendcount[netnode] = RESENDCOUNT;
        }
    }
    else
        resendcount[netnode]--;
    
    // check for out of order / duplicated packet       
    if (realend == nettics[netnode])
        continue;
            
    if (realend < nettics[netnode])
    {
        if (eval(debugfile)){
            sb.setLength(0);
            sb.append("out of order packet (");
            sb.append(realstart);
            sb.append(" + ");
            sb.append(netbuffer.numtics);
           sb.append(")\n");
           debugfile.writeString(sb.toString());
           }
           continue;
    }
    
    // check for a missed packet
    if (realstart > nettics[netnode])
    {
        // stop processing until the other system resends the missed tics
        if (eval(debugfile)) {
            sb.setLength(0);
            sb.append("missed tics from ");
            sb.append(netnode);
            sb.append(" (");
            sb.append(realstart);
            sb.append(" - ");
            sb.append(nettics[netnode]);
           sb.append(")\n");
           debugfile.writeString(sb.toString());
        }
        remoteresend[netnode] = true;
        continue;
    }

    // update command store from the packet
        {
        int     start;

        remoteresend[netnode] = false;
        
        start = nettics[netnode] - realstart;       
        src = netbuffer.cmds[start];

        while (nettics[netnode] < realend)
        {
        dest = netcmds[netconsole][nettics[netnode]%BACKUPTICS];
        nettics[netnode]++;
        // MAES: this is a struct copy.
        src.copyTo(dest);
        // Advance src
        start++;
        }
    }
    }
}

int      gametime;

@Override
public void NetUpdate ()
{
    int             nowtime;
    int             newtics;
    int             i,j;
    int             realstart;
    int             gameticdiv;
    
    // check time
    nowtime = I.GetTime ()/ticdup;
    newtics = nowtime - gametime;
    gametime = nowtime;
    
    if (newtics <= 0)   // nothing new to update
    {
        // listen for other packets
        GetPackets ();
        return;
    } else {
 

    if (skiptics <= newtics)
    {
    newtics -= skiptics;
    skiptics = 0;
    }
    else
    {
    skiptics -= newtics;
    newtics = 0;
    }
    
        
    netbuffer.player = (byte) consoleplayer;
    
    // build new ticcmds for console player
    gameticdiv = gametic/ticdup;
    for (i=0 ; i<newtics ; i++)
    {
    VI.StartTic ();
    ProcessEvents ();
    if (maketic - gameticdiv >= BACKUPTICS/2-1)
        break;          // can't hold any more
    
    //printf ("mk:%i ",maketic);
    BuildTiccmd (localcmds[maketic%BACKUPTICS]);
    maketic++;
    }


    if (singletics)
    return;         // singletic update is syncronous
    
    // send the packet to the other nodes
    for (i=0 ; i<doomcom.numnodes ; i++)
    if (nodeingame[i])
    {
        netbuffer.starttic = (byte) (realstart = resendto[i]);
        netbuffer.numtics = (byte) (maketic - realstart);
        if (netbuffer.numtics > BACKUPTICS)
        I.Error ("NetUpdate: netbuffer.numtics > BACKUPTICS");

        resendto[i] = maketic - doomcom.extratics;

        for (j=0 ; j< netbuffer.numtics ; j++)
        netbuffer.cmds[j] = 
            localcmds[(realstart+j)%BACKUPTICS];
                    
        if (remoteresend[i])
        {
        netbuffer.retransmitfrom = (byte) nettics[i];
        HSendPacket (i, NCMD_RETRANSMIT);
        }
        else
        {
        netbuffer.retransmitfrom = 0;
        HSendPacket (i, 0);
        }
    }
    }

}



//
// CheckAbort
//
private void CheckAbort ()
{
    event_t ev;
    int     stoptic;
    
    stoptic = I.GetTime () + 2; 
    while (I.GetTime() < stoptic) 
    VI.StartTic (); 
    
    VI.StartTic ();
    for ( ; eventtail != eventhead 
          ; eventtail = (++eventtail)&(MAXEVENTS-1) ) 
    { 
    ev = events[eventtail]; 
    if (ev.type == evtype_t.ev_keydown && ev.data1 == KEY_ESCAPE)
        I.Error ("Network game synchronization aborted.");
    } 
}

boolean[] gotinfo=new boolean[MAXNETNODES];

//
// D_ArbitrateNetStart
//
public void ArbitrateNetStart ()
{
    int     i;
    autostart = true;
    
    // Clear it up...
    Arrays.fill(gotinfo,false);
    
    if (doomcom.consoleplayer!=0)
    {
    // listen for setup info from key player
    System.out.println("listening for network start info...\n");
    while (true)
    {
        CheckAbort ();
        if (!HGetPacket ())
        continue;
        if (flags(netbuffer.checksum , NCMD_SETUP))
        {
        if (netbuffer.player != VERSION)
            I.Error ("Different DOOM versions cannot play a net game!");
        startskill = skill_t.values()[netbuffer.retransmitfrom & 15];
        
        // Deathmatch
        if (((netbuffer.retransmitfrom & 0xc0) >> 6)==1) 
        deathmatch = true;
        else
            // Cooperative
              if (((netbuffer.retransmitfrom & 0xc0) >> 6)==2) 
                  altdeath = true;

        nomonsters = (netbuffer.retransmitfrom & 0x20) > 0;
        respawnparm = (netbuffer.retransmitfrom & 0x10) > 0;
        startmap = netbuffer.starttic & 0x3f;
        startepisode = netbuffer.starttic >> 6;
        return;
        }
    }
    }
    else
    {
    // key player, send the setup info
    System.out.println("sending network start info...\n");
    do
    {
        CheckAbort ();
        for (i=0 ; i<doomcom.numnodes ; i++)
        {
        netbuffer.retransmitfrom = (byte) startskill.ordinal();
        if (deathmatch)
            netbuffer.retransmitfrom |= (1<<6);
        else
          if (altdeath)
                netbuffer.retransmitfrom |= (2<<6);
        if (nomonsters)
            netbuffer.retransmitfrom |= 0x20;
        if (respawnparm)
            netbuffer.retransmitfrom |= 0x10;
        netbuffer.starttic = (byte) (startepisode * 64 + startmap);
        netbuffer.player = VERSION;
        netbuffer.numtics = 0;
        HSendPacket (i, NCMD_SETUP);
        }

//#if 1
        for(i = 10 ; (i>0)  &&  HGetPacket(); --i)
        {
        if((netbuffer.player&0x7f) < MAXNETNODES)
            gotinfo[netbuffer.player&0x7f] = true;
        }
/*
        while (HGetPacket ())
        {
        gotinfo[netbuffer.player&0x7f] = true;
        }
 */

        for (i=1 ; i<doomcom.numnodes ; i++)
        if (!gotinfo[i])
            break;
    } while (i < doomcom.numnodes);
    }
}

//
// D_CheckNetGame
// Works out player numbers among the net participants
//

private void CheckNetGame ()
{
    int             i;
    
    for (i=0 ; i<MAXNETNODES ; i++)
    {
    nodeingame[i] = false;
        nettics[i] = 0;
    remoteresend[i] = false;    // set when local needs tics
    resendto[i] = 0;        // which tic to start sending
    }
    
    // I_InitNetwork sets doomcom and netgame
    DNI.InitNetwork ();
    if (doomcom.id != DOOMCOM_ID)
    I.Error ("Doomcom buffer invalid!");
    
    netbuffer = doomcom.data;
    consoleplayer = displayplayer = doomcom.consoleplayer;
    if (netgame)
    ArbitrateNetStart ();

//    printf ("startskill %i  deathmatch: %i  startmap: %i  startepisode: %i\n",
//        startskill, deathmatch, startmap, startepisode);

    {
        sb.setLength(0);
        sb.append("startskill ");
        sb.append(startskill);
        sb.append("  deathmatch: ");
        sb.append(deathmatch);
        sb.append("  startmap: ");
        sb.append(startmap);
        sb.append("  startepisode: ");
        sb.append(startepisode);
        sb.append('\n');
        System.out.println(sb);
    }
    
    
    // read values out of doomcom
    ticdup = doomcom.ticdup;
    // MAES: ticdup must not be zero at this point. Obvious, no?
    maxsend = BACKUPTICS/(2*ticdup)-1;
    if (maxsend<1)
    maxsend = 1;
            
    for (i=0 ; i<doomcom.numplayers ; i++)
    playeringame[i] = true;
    for (i=0 ; i<doomcom.numnodes ; i++)
    nodeingame[i] = true;
    
    //printf ("player %i of %i (%i nodes)\n",
    //    consoleplayer+1, doomcom.numplayers, doomcom.numnodes);

    {
        sb.setLength(0);
        sb.append("startskill ");
        sb.append(startskill);
        sb.append("  deathmatch: ");
        sb.append(deathmatch);
        sb.append("  startmap: ");
        sb.append(startmap);
        sb.append("  startepisode: ");
        sb.append(startepisode);
        sb.append('\n');
        System.out.println(sb);
    }
    
}


//
// D_QuitNetGame
// Called before quitting to leave a net game
// without hanging the other players
//
public void QuitNetGame ()
{
    int             i, j;
    
    if (eval(debugfile))
        try {
            debugfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    if (!netgame || !usergame || consoleplayer == -1 || demoplayback)
    return;
    
    // send a bunch of packets for security
    netbuffer.player = (byte) consoleplayer;
    netbuffer.numtics = 0;
    for (i=0 ; i<4 ; i++)
    {
    for (j=1 ; j<doomcom.numnodes ; j++)
        if (nodeingame[j])
        HSendPacket (j, NCMD_EXIT);
    I.WaitVBL (1);
    }
}



//
// TryRunTics
//
int[] frametics=new int[4];
int frameon;
boolean[] frameskip=new boolean[4];
int oldnettics;
int  oldentertics;

@Override
public void TryRunTics ()
{
    int     i;
    int     lowtic;
    int     entertic;

    int     realtics;
    int     availabletics;
    int     counts;
    int     numplaying;
    
    // get real tics        
    entertic = I.GetTime ()/ticdup;
    realtics = entertic - oldentertics;
    oldentertics = entertic;
    
    // get available tics
    NetUpdate ();
    
    lowtic = MAXINT;
    numplaying = 0;
    for (i=0 ; i<doomcom.numnodes ; i++)
    {
    if (nodeingame[i])
    {
        numplaying++;
        if (nettics[i] < lowtic)
        lowtic = nettics[i];
    }
    }
    availabletics = lowtic - gametic/ticdup;
    
    // decide how many tics to run
    if (realtics < availabletics-1)
    counts = realtics+1;
    else if (realtics < availabletics)
    counts = realtics;
    else
    counts = availabletics;
    
    if (counts < 1)
    counts = 1;
        
    frameon++;

    if (eval(debugfile)){
        sb.setLength(0);
        sb.append( "=======real: ");
        sb.append(realtics);
        sb.append("  avail: ");
        sb.append(availabletics);
        sb.append("  game: ");
        sb.append(counts);
        sb.append("\n");
        debugfile.writeString(sb.toString());
    }

    if (!demoplayback)
    {   
    // ideally nettics[0] should be 1 - 3 tics above lowtic
    // if we are consistantly slower, speed up time
    for (i=0 ; i<MAXPLAYERS ; i++)
        if (playeringame[i])
        break;
    if (consoleplayer == i)
    {
        // the key player does not adapt
    }
    else
    {
        if (nettics[0] <= nettics[nodeforplayer[i]])
        {
        gametime--;
        // printf ("-");
        }
        frameskip[frameon&3] = oldnettics > nettics[nodeforplayer[i]];
        oldnettics = nettics[0];
        if (frameskip[0] && frameskip[1] && frameskip[2] && frameskip[3])
        {
        skiptics = 1;
        // printf ("+");
        }
    }
    }// demoplayback
    
    // wait for new tics if needed
    while (lowtic < gametic/ticdup + counts)    
    {
    NetUpdate ();   
    lowtic = MAXINT;
    
    for (i=0 ; i<doomcom.numnodes ; i++)
        if (nodeingame[i] && nettics[i] < lowtic)
        lowtic = nettics[i];
    
    if (lowtic < gametic/ticdup)
        I.Error ("TryRunTics: lowtic < gametic");
                
    // don't stay in here forever -- give the menu a chance to work
    int time=I.GetTime();
    if (time/ticdup - entertic >= 20)
    {
        M.Ticker ();
        return;
    }
    }
    
    // run the count * ticdup dics
    while (counts-->0)
    {
    for (i=0 ; i<ticdup ; i++)
    {
        if (gametic/ticdup > lowtic)
        I.Error ("gametic>lowtic");
        if (advancedemo)
        DM.DoAdvanceDemo ();
        M.Ticker ();
        Ticker ();
        gametic++;
        
        // modify command for duplicated tics
        if (i != ticdup-1)
        {
        ticcmd_t    cmd;
        int         buf;
        int         j;
                
        buf = (gametic/ticdup)%BACKUPTICS; 
        for (j=0 ; j<MAXPLAYERS ; j++)
        {
            cmd = netcmds[j][buf];
            cmd.chatchar = 0;
            if (flags(cmd.buttons , BT_SPECIAL))
            cmd.buttons = 0;
        }
        }
    }
    NetUpdate ();   // check for new console commands
    }
}


@Override
public doomcom_t getDoomCom() {
	return this.doomcom;
}


@Override
public void setDoomCom(doomcom_t doomcom) {
	this.doomcom=doomcom;
}
}