package doom;

import static data.Defines.BACKUPTICS;
import static data.Defines.BTS_PAUSE;
import static data.Defines.BTS_SAVEGAME;
import static data.Defines.BTS_SAVEMASK;
import static data.Defines.BTS_SAVESHIFT;
import static data.Defines.BT_ATTACK;
import static data.Defines.BT_CHANGE;
import static data.Defines.BT_SPECIAL;
import static data.Defines.BT_SPECIALMASK;
import static data.Defines.BT_USE;
import static data.Defines.BT_WEAPONSHIFT;
import static data.Defines.KEY_CAPSLOCK;
import static data.Defines.KEY_ESCAPE;
import static data.Defines.KEY_F12;
import static data.Defines.KEY_PAUSE;
import static data.Defines.NORMALUNIX;
import static data.Defines.NUMAMMO;
import static data.Defines.NUMWEAPONS;
import static data.Defines.PST_DEAD;
import static data.Defines.PST_LIVE;
import static data.Defines.PST_REBORN;
import static data.Defines.PU_CACHE;
import static data.Defines.PU_STATIC;
import static data.Defines.SKYFLATNAME;
import static data.Defines.TICRATE;
import static data.Defines.VERSION;
import static data.Limits.MAXEVENTS;
import static data.Limits.MAXHEALTH;
import static data.Limits.MAXINT;
import static data.Limits.MAXNETNODES;
import static data.Limits.MAXPLAYERS;
import static data.Tables.ANG45;
import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.dstrings.DEVMAPS;
import static data.dstrings.SAVEGAMENAME;
import static data.info.mobjinfo;
import static data.info.states;
import static doom.NetConsts.CMD_GET;
import static doom.NetConsts.CMD_SEND;
import static doom.NetConsts.DOOMCOM_ID;
import static doom.NetConsts.NCMD_CHECKSUM;
import static doom.NetConsts.NCMD_EXIT;
import static doom.NetConsts.NCMD_KILL;
import static doom.NetConsts.NCMD_RETRANSMIT;
import static doom.NetConsts.NCMD_SETUP;
import static doom.englsh.D_CDROM;
import static doom.englsh.D_DEVSTR;
import static doom.englsh.GGSAVED;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static utils.C2JUtils.eval;
import static utils.C2JUtils.flags;
import static utils.C2JUtils.testAccess;
import hu.HU;
import i.DoomStatusAware;
import i.DoomSystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import m.DoomRandom;
import m.Menu;
import m.MenuMisc;
import n.DummyNetworkDriver;
import p.Actions;
import p.DanmakuPatterns;
import p.DanmakuShape;
import p.LevelLoader;
import p.mobj_t;
import rr.ParallelRenderer;
import rr.SimpleTextureManager;
import rr.UnifiedRenderer;
import rr.subsector_t;
import s.AbstractDoomAudio;
import s.ClassicDoomSoundDriver;
import s.DavidMusicModule;
import s.DavidSFXModule;
import s.DummyMusic;
import s.DummySFX;
import s.DummySoundDriver;
import s.SpeakerDoomSoundDriver;
import savegame.IDoomSaveGame;
import savegame.IDoomSaveGameHeader;
import savegame.VanillaDSG;
import savegame.VanillaDSGHeader;
import st.StatusBar;
import timing.FastTicker;
import timing.ITicker;
import timing.MilliTicker;
import timing.NanoTicker;
import utils.C2JUtils;
import v.BufferedRenderer;
import v.IVideoScale;
import v.IVideoScaleAware;
import v.VisualSettings;
import w.DoomFile;
import w.WadLoader;
import automap.Map;
import awt.AWTDoom;
import awt.OldAWTDoom;
import data.Tables;
import data.dstrings;
import data.mapthing_t;
import data.mobjtype_t;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import defines.GameMission_t;
import defines.GameMode_t;
import defines.Language_t;
import defines.ammotype_t;
import defines.gamestate_t;
import defines.skill_t;
import defines.statenum_t;
import demo.IDemoTicCmd;
import demo.IDoomDemo;
import demo.VanillaDoomDemo;
import demo.VanillaTiccmd;
import f.EndLevel;
import f.Finale;
import f.Wiper;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomMain.java,v 1.72.2.6 2011/07/31 11:46:32 velktron Exp $
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
//	DOOM main program (D_DoomMain) and game loop (D_DoomLoop),
//	plus functions to determine game mode (shareware, registered),
//	parse command line parameters, configure game parameters (turbo),
//	and call the startup functions.
//
//  In Mocha Doom, this was unified with d_game and doomstat.c
//
//-----------------------------------------------------------------------------

public class DoomMain extends DoomStatus implements IDoomGameNetworking, IDoomGame, IDoom, IVideoScaleAware{

    public static final String rcsid = "$Id: DoomMain.java,v 1.72.2.6 2011/07/31 11:46:32 velktron Exp $";

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
        // TODO create a pool of reusable messages?
        // NEVERMIND we can use the original system.
        events[eventhead].setFrom(ev);
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

        if ( ( isCommercial() )
                && (W.CheckNumForName("MAP01")<0) )
            return; 

        for ( ; eventtail != eventhead ; eventtail = (++eventtail)&(MAXEVENTS-1) )
        {
            ev = events[eventtail];
            if (M.Responder (ev)){
                //epool.checkIn(ev);
                continue;               // menu ate the event
            }
            Responder (ev);
            // We're done with it, return it to the pool.
            //epool.checkIn(ev);
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
            if (wipe || (!R.isFullHeight() && fullscreen) )
                redrawsbar = true;
            if (inhelpscreensstate && !inhelpscreens)
                redrawsbar = true;              // just put away the help screen
            ST.Drawer (R.isFullHeight(), redrawsbar );
            fullscreen = R.isFullHeight();
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


        // draw the view directly
        if (gamestate == gamestate_t.GS_LEVEL && !automapactive && eval(gametic)){
            R.RenderPlayerView (players[displayplayer]);
        	}

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
        if (gamestate == gamestate_t.GS_LEVEL && !automapactive && !R.isFullScreen())
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

        // wipe update. At this point, AT LEAST one frame of the game must have been
        // rendered for this to work. 22/5/2011: Fixed a vexing bug with the wiper.
        // Jesus Christ with a Super Shotgun!
        WIPE.EndScreen(0, 0, SCREENWIDTH, SCREENHEIGHT);

        wipestart = TICK.GetTime () - 1;

        do
        {
            do
            {
                nowtime = TICK.GetTime ();
                tics = nowtime - wipestart;
            } while (tics==0); // Wait until a single tic has passed.
            wipestart = nowtime;
            done = WIPE.ScreenWipe(Wiper.wipe.Melt.ordinal()
                , 0, 0, SCREENWIDTH, SCREENHEIGHT, tics);
            ISND.UpdateSound();
            ISND.SubmitSound();             // update sounds after one wipe tic.
            VI.UpdateNoBlit ();
            M.Drawer ();                    // menu is drawn even on top of wipes
            VI.FinishUpdate ();             // page flip or blit buffer
        } while (!done);

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

        if (eval(CM.CheckParm ("-debugfile")))
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

        //AM.Start(); //_D_: not suposed to be here, see linuxdoom source
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
                DGN.TryRunTics (); // will run at least one tic (in NET)

            }

            S.UpdateSounds (players[consoleplayer].mo);// move positional sounds

            // Update display, next frame, with current state.
            Display ();
            
//#ifndef SNDSERV
	// Sound mixing for the buffer is snychronous.
	ISND.UpdateSound();
//#endif	
	// Synchronous sound output is explicitly called.
//#ifndef SNDINTR
	// Update sound output.
	ISND.SubmitSound();
//#endif
             
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
    public final void PageTicker ()
    {
        if (--pagetic < 0)
            AdvanceDemo ();
    }



    /**
     * D_PageDrawer
     */

    public final void PageDrawer ()
    {

        // FIXME: this check wasn't necessary in vanilla, since pagename was 
        // guaranteed(?) not to be null or had a safe default value.  
        if (pagename != null)
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
        
        if ( isRetail()) // Allows access to a 4th demo.
            demosequence = (demosequence+1)%7;
        else
            demosequence = (demosequence+1)%6;

        switch (demosequence)
        {
        case 0:
            if ( isCommercial() )
                pagetic = 35 * 11;
            else
                pagetic = 170;
            gamestate = gamestate_t.GS_DEMOSCREEN;
            pagename = "TITLEPIC";
            if ( isCommercial() )
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
            if ( isCommercial())
            {
                pagetic = 35 * 11;
                pagename = "TITLEPIC";
                S.StartMusic(musicenum_t.mus_dm2ttl);
            }
            else
            {
                pagetic = 200;

                if ( isRetail() )
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
    

    /**
     * IdentifyVersion
     * Checks availability of IWAD files by name,
     * to determine whether registered/commercial features
     * should be executed (notably loading PWAD's).
     */

    public String IdentifyVersion ()
    {

        String home;
        String doomwaddir;
        DoomVersions vcheck=new DoomVersions();
        
        // By default.
        language=Language_t.english;
        
        // First, check for -iwad parameter.
        // If valid, then it trumps all others.
        
        int p;
        p=CM.CheckParm("-iwad");
        if (eval(p))
        {
        	System.out.println("-iwad specified. Will be used with priority\n");
            String test=CM.getArgv(p+1);
            
            // It might be quoted.
            test=C2JUtils.unquoteIfQuoted(test,'"');
            
            String separator=System.getProperty("file.separator");
            doomwaddir=test.substring(0, 1+test.lastIndexOf(separator));
            String iwad=test.substring( 1+test.lastIndexOf(separator));
            GameMode_t attempt=vcheck.tryOnlyOne(iwad,doomwaddir);
            // Note: at this point we can't distinguish between "doom" retail
            // and "doom" ultimate yet.
            if (attempt!=null) {
            	AddFile(doomwaddir+iwad);
            	this.setGameMode(attempt);
            	return (doomwaddir+iwad);
            }
        } else {
        // Unix-like checking. Might come in handy sometimes.   
        // This should ALWAYS be activated, else doomwaddir etc. won't be defined.

        doomwaddir = System.getenv("DOOMWADDIR");
        if (doomwaddir!=null){
                System.out.println("DOOMWADDIR found. Will be used with priority\n");
        }
        
        home = System.getenv("HOME");
        if (NORMALUNIX){
            if (!eval(home))
                I.Error("Please set $HOME to your home directory");
        }
        basedefault=home+"/.doomrc";   

        // None found, using current.
        if (!eval(doomwaddir))
            doomwaddir = ".";
    
        	vcheck.tryThemAll(doomwaddir);
        }

        // MAES: Interesting. I didn't know of that :-o
        if (eval(CM.CheckParm ("-shdev")))
        {
            setGameMode(GameMode_t.shareware);
            devparm = true;
            AddFile (dstrings.DEVDATA+"doom1.wad");
            AddFile (dstrings.DEVMAPS+"data_se/texture1.lmp");
            AddFile (dstrings.DEVMAPS+"data_se/pnames.lmp");
            basedefault=dstrings.DEVDATA+"default.cfg";
            return (dstrings.DEVDATA+"doom1.wad");
        }

        if (eval(CM.CheckParm ("-regdev")))
        {
            setGameMode(GameMode_t.registered);
            devparm = true;
            AddFile (dstrings.DEVDATA+"doom.wad");
            AddFile (dstrings.DEVMAPS+"data_se/texture1.lmp");
            AddFile (dstrings.DEVMAPS+"data_se/texture2.lmp");
            AddFile (dstrings.DEVMAPS+"data_se/pnames.lmp");
            basedefault=dstrings.DEVDATA+"default.cfg";
            return (dstrings.DEVDATA+"doom.wad");
        }

        if (eval(CM.CheckParm ("-comdev")))
        {
            setGameMode(GameMode_t.commercial);
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
            return (dstrings.DEVDATA+"doom2.wad");
        }


        if ( testAccess(vcheck.doom2fwad,"r" ))
        {
            setGameMode(GameMode_t.commercial);
            // C'est ridicule!
            // Let's handle languages in config files, okay?
            language = Language_t.french;
            System.out.println("French version\n");
            AddFile (vcheck.doom2fwad);
            return vcheck.doom2fwad;
        }


        if ( testAccess(vcheck.doom2wad,"r" ))
        {
            setGameMode(GameMode_t.commercial);
            AddFile (vcheck.doom2wad);
            return vcheck.doom2wad;
        }

        if ( testAccess (vcheck.plutoniawad, "r" ) )
        {
            setGameMode(GameMode_t.pack_plut);
            AddFile (vcheck.plutoniawad);
            return vcheck.plutoniawad;
        }

        if ( testAccess ( vcheck.tntwad, "r" ) )
        {
            setGameMode(GameMode_t.pack_tnt);
            AddFile (vcheck.tntwad);
            return vcheck.tntwad;
        }
        
        if ( testAccess ( vcheck.tntwad, "r" ) )
        {
            setGameMode(GameMode_t.pack_xbla);
            AddFile (vcheck.xblawad);
            return vcheck.xblawad;
        }

        if ( testAccess (vcheck.doomuwad,"r") )
        {
            // TODO auto-detect ultimate Doom even from doom.wad
        	// Maes: this is done later on.
            setGameMode(GameMode_t.retail);
            AddFile (vcheck.doomuwad);
            return vcheck.doomuwad;
        }

        if ( testAccess (vcheck.doomwad,"r") )
        {
            setGameMode(GameMode_t.registered);
            AddFile (vcheck.doomwad);
            return vcheck.doomwad;
        }

        if ( testAccess (vcheck.doom1wad,"r") )
        {
            setGameMode(GameMode_t.shareware);
            AddFile (vcheck.doom1wad);
            return vcheck.doom1wad;
        }

        // MAES: Maybe we should add FreeDoom here later.

        System.out.println("Game mode indeterminate.\n");
        setGameMode(GameMode_t.indetermined);

        return null;
        // We don't abort. Let's see what the PWAD contains.
        //exit(1);
        //I_Error ("Game mode indeterminate\n");
    }




    //
    // D_DoomMain
    //
    @SuppressWarnings("deprecation")
	public void Start ()
    {
        int             p;
        StringBuffer file=new StringBuffer();

        // TODO: This may modify the command line by appending more stuff
        // from an external file. Since it can affect other stuff too,
        // maybe it should be outside of Start() and into i.Main ?
        CM.FindResponseFile ();

        String iwadfilename=IdentifyVersion ();
        
        // Sets unbuffered output in C. Not needed here. setbuf (stdout, NULL);
        modifiedgame = false;
        angle=eval(CM.CheckParm ("-angle"));
        if (angle){
        	int ap=CM.CheckParm ("-angle");
        	try{
        		this.anglespread=Integer.parseInt(CM.getArgv(ap+1));
        	} catch (Exception e){
        		System.err.println("Sorry Joddo, couldn't parse angle!");
        	}
        	 
        }
        
        nomonsters = eval(CM.CheckParm ("-nomonsters"));
        respawnparm = eval(CM.CheckParm ("-respawn"));
        fastparm = eval(CM.CheckParm ("-fast"));
        devparm = eval(CM.CheckParm ("-devparm"));
        if (eval(CM.CheckParm ("-altdeath")))
            //deathmatch = 2;
            altdeath=true;
        else if (eval(CM.CheckParm ("-deathmatch")))
            deathmatch = true;

        // MAES: Check for Ultimate Doom in "doom.wad" filename.
        WadLoader tmpwad=new WadLoader();
        try {
			tmpwad.InitFile(iwadfilename);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		// Check using a reloadable hack.
        CheckForUltimateDoom(tmpwad);    
       
        // MAES: better extract a method for this.
        GenerateTitle();

        // Print ticker info. It has already been set at Init() though.
        if (eval(CM.CheckParm("-millis"))){
            System.out.println("ITicker: Using millisecond accuracy timer.");
        } else if (eval(CM.CheckParm("-fasttic"))){
                System.out.println("ITicker: Using fastest possible timer.");
            }
        else {
            System.out.println("ITicker: Using nanosecond accuracy timer.");
        }

        System.out.println(title.toString());

        if (devparm)
            System.out.println(D_DEVSTR);

        // Running from CDROM?
        if (eval(CM.CheckParm("-cdrom")))
        {
            System.out.println(D_CDROM);
            //System.get("c:\\doomdata",0);
            System.out.println (basedefault+"c:/doomdata/default.cfg");
        }	

        // turbo option
        if ( eval(p=CM.CheckParm ("-turbo")) )
        {
            int     scale = 200;
            //int forwardmove[2];
            // int sidemove[2];

            if (p<CM.getArgc()-1)
                scale = Integer.parseInt(CM.getArgv(p+1));
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
        p = CM.CheckParm ("-wart");
        if (eval(p))
        {
            char[] tmp=CM.getArgv(p).toCharArray();
            tmp[4]= 'p';// big hack, change to -warp
            CM.setArgv(p,new String(tmp));    
            GameMode_t gamemode=getGameMode();
            // Map name handling.
            switch (gamemode )
            {
            case shareware:
            case retail:
            case registered:
                file.append("~");
                file.append(DEVMAPS);
                file.append(String.format("E%cM%c.wad", CM.getArgv(p+1), CM.getArgv(p+2)));
                file.append(String.format("Warping to Episode %s, Map %s.\n",
                    CM.getArgv(p+1),CM.getArgv(p+2)));
                break;

            case commercial:
            default:
                p = Integer.parseInt(CM.getArgv(p+1));
                if (p<10){
                    file.append("~");
                    file.append(DEVMAPS);
                    file.append(String.format("cdata/map0%i.wad", p));
                }
                else
                {
                    file.append("~");
                    file.append(DEVMAPS);
                    file.append(String.format("cdata/map%i.wad", p));
                }
                break;
            }
            AddFile (file.toString());
        }

        p = CM.CheckParm ("-file");
        if (eval(p))
        {
            // the parms after p are wadfile/lump names,
            // until end of parms or another - preceded parm
            modifiedgame = true;            // homebrew levels
            // MAES 1/6/2011: Avoid + to avoid clashing with +map
            while (++p != CM.getArgc() && CM.getArgv(p).charAt(0) != '-' && CM.getArgv(p).charAt(0) != '+')
                AddFile (C2JUtils.unquoteIfQuoted(CM.getArgv(p),'"'));
        }
        
        p = CM.CheckParm ("-playdemo");
                
        if (eval(p)) normaldemo=true;
        else {
            p=CM.CheckParm ("-fastdemo");        
            if (eval(p)){
            System.out.println("Fastdemo mode. Boundless clock!");
            fastdemo=true;
            this.TICK=new FastTicker();            
            }
            else        
                if (!eval(p)) {
                    p = CM.CheckParm ("-timedemo");
                    if (eval(p)) singletics=true;
                }
        }

        // If any of the previous succeeded, try grabbing the filename.
        if ((normaldemo||fastdemo||singletics) && p < CM.getArgc()-1)
        {   
            loaddemo=CM.getArgv(p+1);
            AddFile (loaddemo+".lmp");
            System.out.printf("Playing demo %s.lmp.\n",loaddemo);
            autostart=true;
        }

        // get skill / episode / map from parms
        // FIXME: should get them FROM THE DEMO itself.
        startskill = skill_t.sk_medium;
        startepisode = 1;
        startmap = 1;
        //autostart = false;


        p = CM.CheckParm ("-novert");
        if (eval(p) && p < CM.getArgc()-1)
        {
            novert=!(CM.getArgv(p+1).toLowerCase().compareTo("disable")==0);
            if (!novert) System.out.println("-novert ENABLED (default)");
            else System.out.println("-novert DISABLED. Hope you know what you're doing...");
        }

        p = CM.CheckParm ("-skill");
        if (eval(p) && p < CM.getArgc()-1)
        {
            startskill = skill_t.values()[CM.getArgv(p+1).charAt(0)-'1'];
            autostart = true;
        }

        p = CM.CheckParm ("-episode");
        if (eval(p) && p < CM.getArgc()-1)
        {
            startepisode = CM.getArgv(p+1).charAt(0)-'0';
            startmap = 1;
            autostart = true;
        }

        p = CM.CheckParm ("-timer");
        if (eval(p) && p < CM.getArgc()-1 && deathmatch)
        {
            int     time;
            time = Integer.parseInt(CM.getArgv(p+1));
            System.out.print("Levels will end after "+time+" minute");
            if (time>1)
                System.out.print("s");
            System.out.print(".\n");
        }

        // OK, and exactly how is this enforced?
        p = CM.CheckParm ("-avg");
        if (eval(p) && p < CM.getArgc()-1 && deathmatch)
            System.out.print("Austin Virtual Gaming: Levels will end after 20 minutes\n");

        // MAES 31/5/2011: added support for +map variation.
        p = CM.CheckParm ("-warp");
        if (eval(p) && p < CM.getArgc()-1)
        {
            if (isCommercial())
                startmap = Integer.parseInt(CM.getArgv(p+1));
            else
            {
                int eval=11;
                try {
                    eval=Integer.parseInt(CM.getArgv(p+1));
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

        // Maes: 1/6/2011 Added +map support
        p = CM.CheckParm ("+map");
        if (eval(p)) 
        {
            if (isCommercial()) {
                startmap = parseAsMapXX(CM.getArgv(p+1));
                if (startmap!=-1){
                	autostart = true;
            	}
            }
            else
            {
                int eval=parseAsExMx(CM.getArgv(p+1));
                if (eval!=-1){

                    startepisode = Math.max(1,eval/10);
                    startmap = Math.max(1,eval%10);
                    autostart = true;
                }
            }
            
        }
        
        // init subsystems
        System.out.print ("V_Init: allocate screens.\n");
        V.Init ();

        System.out.print ("M_LoadDefaults: Load system defaults.\n");
        MenuMisc.LoadDefaults (this);              // load before initing other systems

        System.out.print ("Z_Init: Init zone memory allocation daemon. \n");
        // DUMMY: Z_Init ();

        System.out.print ("W_Init: Init WADfiles.\n");
        try {
        	W.setZone(this);
            W.InitMultipleFiles (wadfiles);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        //
        System.out.print ("VI_Init: set colormaps.\n");
        byte[] tmppal=W.CacheLumpNameAsRawBytes("PLAYPAL", PU_STATIC);
        // set it, create it, but don't make it visible yet.
        
        // MAES: FIX for incomplete palette lumps such as those in EGADOOM.
        // If PLAYPAL lump is too short, some palettes will be all-black.
        //byte[] pal=new byte[14*768];
        //System.arraycopy(tmppal, 0, pal, 0, tmppal.length);
        
        p = CM.CheckParm ("-oldawtevents");
        if (!eval(p)) {
        	// This is the "mochaevents" interface, now default.
        	VI=new AWTDoom(this,(BufferedRenderer) V,tmppal);
        }
        else {
        	System.out.print("Using OLDER AWT inteface. Some stuff may be broken!\n");
        	VI=new OldAWTDoom(this,(BufferedRenderer) V,tmppal);
        }
        
        VI.InitGraphics();
        
        

        // MAES: Before we begin calling the various Init() stuff,
        // we need to make sure that objects that support the IVideoScaleAware
        // interface get set and initialized.

        initializeVideoScaleStuff();

        // MAES: The status bar needs this update because it can "force"
        // the video renderer to assign it a scratchpad screen (Screen 4).
        // Since we're at it, let's update everything, it's easy!
        
        this.updateStatusHolders(this);

        // Check for -file in shareware
        CheckForPWADSInShareware();
        

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
        switch ( getGameMode() )
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
        case pack_tnt:
        case pack_plut:
        case pack_xbla:
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
        
      // Sound "drivers" before the game sound controller.
        
        if (CM.CheckParmBool("-nomusic") || CM.CheckParmBool("-nosound"))
            this.IMUS=new DummyMusic();
        else
            this.IMUS=new DavidMusicModule();
        
        if (CM.CheckParmBool("-nosfx") ||  CM.CheckParmBool("-nosound"))
            this.ISND=new DummySFX();
        else {
            // Switch between possible sound drivers.
            // Crudish.
            if (CM.CheckParmBool("-audiolines"))
                this.ISND=	new DavidSFXModule(this,numChannels);
            else // PC Speaker emulation 
            if (CM.CheckParmBool("-speakersound"))
                this.ISND=  new SpeakerDoomSoundDriver(this,numChannels);
            else  // This is the default
                this.ISND=  new ClassicDoomSoundDriver(this,numChannels);
            }
        
        if (!CM.CheckParmBool("-nosound"))// Obviously, nomusic && nosfx = nosound.
        	this.S=new AbstractDoomAudio(this,numChannels);
        else
        	// Saves a lot of distance calculations, if we're not to output
        	// any sound at all.
            // TODO: create a Dummy that can handle music alone.
        	this.S=new DummySoundDriver();
        
        ISND.InitSound();
        IMUS.InitMusic();
        S.Init (snd_SfxVolume *8, snd_MusicVolume *8 );

        // Hook audio to users.
        this.updateStatusHolders(this);
        
        System.out.print ("HU_Init: Setting up heads up display.\n");
        HU.Init();

        System.out.print ("ST_Init: Init status bar.\n");
        ST.Init ();

        // check for a driver that wants intermission stats
        p = CM.CheckParm ("-statcopy");
        if (eval(p) && p<CM.getArgc()-1)
        {

            // TODO: statcopy = (void*)atoi(myargv[p+1]);
            System.out.print ("External statistics registered.\n");
        }

        // start the apropriate game based on parms
        p = CM.CheckParm ("-record");

        if (eval(p) && p < CM.getArgc()-1)
        {
            RecordDemo (CM.getArgv(p+1));
            autostart = true;
        }

        // MAES: at this point everything should be set and initialized, so it's
        // time to make the players aware of the general status of Doom.
        //_D_ gonna try to initialize here, because it is needed to play a demo
        for (int i=0;i<MAXPLAYERS;i++){
            players[i].updateStatus(this);
        }

        //p = CM.CheckParm ("-timedemo");
        if (singletics)
        {
            TimeDemo (loaddemo);
            autostart = true;
            DoomLoop ();  // never returns
        }

        p = CM.CheckParm ("-loadgame");
        if (eval(p) && p < CM.getArgc()-1)
        {
            file.delete(0, file.length());
            if (eval(CM.CheckParm("-cdrom"))){
                file.append("c:\\doomdata\\");
                file.append(SAVEGAMENAME);
                file.append("%c.dsg");
                file.append(CM.getArgv(p+1).charAt(0));
            }
            else
            {
                file.append(SAVEGAMENAME);
                file.append("%c.dsg");
                file.append(CM.getArgv(p+1).charAt(0));

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


        if (fastdemo||normaldemo)
        {
            singledemo = true;              // quit after one demo
            if (fastdemo) timingdemo=true;
            InitNew (startskill, startepisode, startmap);
            gamestate=gamestate_t.GS_DEMOSCREEN;
            DeferedPlayDemo (loaddemo);
            DoomLoop ();  // never returns
        }

        DoomLoop ();  // never returns
    }


    private int parseAsMapXX(String argv) {
    	
    	if (argv.length()!=5) return -1; // Nah.
    	if (argv.toLowerCase().lastIndexOf("map")!=0) return -1; // Meh.
    	int map;
    	try {
    		map=Integer.parseInt(argv.substring(3));
    	} catch (NumberFormatException e){
    		return -1; // eww
    	}
    	
		return map;
	}

    private int parseAsExMx(String argv) {
    	
    	if (argv.length()!=4) return -1; // Nah.
    	if (argv.toLowerCase().lastIndexOf("e")!=0) return -1; // Meh.
    	if (argv.toLowerCase().lastIndexOf("m")!=2) return -1; // Meh.
    	int episode,mission;
    	try {
    		episode=Integer.parseInt(argv.substring(1,2));
    		mission=Integer.parseInt(argv.substring(3,4));
    	} catch (NumberFormatException e){
    		return -1; // eww
    	}
    	
		return episode*10+mission;
	}
    
	List<IVideoScaleAware> videoScaleChildren;

    public  void initializeVideoScaleStuff() {

        videoScaleChildren=new ArrayList<IVideoScaleAware>();

        // The automap...
        videoScaleChildren.add(this.AM);
        // The finale...
        videoScaleChildren.add(this.F);
        // The wiper...
        videoScaleChildren.add(this.WIPE);
        // The heads up...
        videoScaleChildren.add(this.HU);
        // The menu...
        videoScaleChildren.add(this.M);
        // The renderer (also has dependent children!)
        videoScaleChildren.add(this.R);
        // The Status Bar
        videoScaleChildren.add(this.ST);
        // Even the video renderer needs some initialization?
        videoScaleChildren.add(this.V);
        // Even the video renderer needs some initialization?
        videoScaleChildren.add(this.WI);

        for(IVideoScaleAware i:videoScaleChildren){
            if (i!=null){
                i.setVideoScale(this.vs);
                i.initScaling();
            }
        }


    }


    /**
     * 
     */
    protected void CheckForPWADSInShareware() {
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
            if ( isShareware())
                System.out.println("\nYou cannot -file with the shareware version. Register!");

            // Check for fake IWAD with right name,
            // but w/o all the lumps of the registered version. 
            if (isRegistered())
                for (i = 0;i < 23; i++)
                    if (W.CheckNumForName(name[i].toUpperCase())<0)
                        I.Error("\nThis is not the registered version: "+name[i]);
        }
    }

    /** Check whether the "doom.wad" we actually loaded
     *  is ultimate Doom's, by checking if it contains 
     *  e4m1 - e4m9.
     * 
     */
    protected void CheckForUltimateDoom(WadLoader W) {
        if (isRegistered())
        {
            // These are the lumps that will be checked in IWAD,
            // if any one is not present, execution will be aborted.
            String[] lumps=
            {
                    "e4m1","e4m2","e4m3","e4m4","e4m5","e4m6","e4m7","e4m8","e4m9"
            };

            // Check for fake IWAD with right name,
            // but w/o all the lumps of the registered version. 
            if (!CheckForLumps(lumps,W)) return;
            // Checks passed, so we can set the mode to Ultimate
            setGameMode(GameMode_t.retail);
        }

    }


    /** Check if ALL of the lumps exist.
     * 
     * @param name
     * @return
     */
    protected boolean CheckForLumps(String[] name, WadLoader W) {
        for (int i = 0;i < name.length; i++)
            if (W.CheckNumForName(name[i].toUpperCase())<0) {
                // Even one is missing? Not OK.
                return false; 
            }
        return true;
    }


    /**
     * 
     */
    protected void GenerateTitle() {
        switch ( getGameMode() )
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
        case pack_xbla:
            title.append("                            ");
            title.append("DOOM 2: No Rest for the Living v");
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
    }

    // Used in BuildTiccmd.
    protected ticcmd_t   base=new ticcmd_t();

    /**
     * G_BuildTiccmd
     * Builds a ticcmd from all of the available inputs
     * or reads it from the demo buffer. 
     * If recording a demo, write it out .
     * 
     * The CURRENT event to process is written to the various 
     * gamekeydown etc. arrays by the Responder method.
     * So look there for any fuckups in constructing them.
     * 
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

        cmd.consistancy =  consistancy[consoleplayer][maketic%BACKUPTICS]; 

        strafe = gamekeydown[key_strafe] || mousebuttons(mousebstrafe) 
        || joybuttons(joybstrafe); 
        speed = ((gamekeydown[key_speed]^alwaysrun) || joybuttons(joybspeed))?1:0;

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
                //System.out.println("Attempting weapon change (building ticcmd)");
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

        // By default, no vertical mouse movement
        if (!novert) forward += mousey; 

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
        if (isCommercial()
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
        gameaction = gameaction_t.ga_nothing; 
        //Z_CheckHeap ();

        // clear cmd building stuff
        Arrays.fill(gamekeydown, false);
        keysCleared = true;
        joyxmove = joyymove = 0; 
        mousex = mousey = 0; 
        sendpause = sendsave = paused = false; 
        Arrays.fill (mousearray, false);
        Arrays.fill(joyarray, false); 
        
        
        // killough 5/13/98: in case netdemo has consoleplayer other than green
        ST.Start();
        HU.Start();
        
        // killough: make -timedemo work on multilevel demos
        // Move to end of function to minimize noise -- killough 2/22/98:

        if (timingdemo)
          {
            if (first)
              {
                starttime = RealTime.GetTime();
                first=false;
              }
          }
        
        // Try reclaiming some memory from limit-expanded buffers.
        R.resetLimits();
        
    } 

    protected boolean first=true;
    
    // Maes: needed because a caps lock down signal is issued
    // as soon as the app gains focus. This causes lock keys to respond.
    // With this hack, they are ignored (?) the first time this happens.
    public boolean justfocused=true;

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
        case ev_clear:
        	// PAINFULLY and FORCEFULLY clear the buttons.
            Arrays.fill(gamekeydown, false);
            keysCleared = true;
            return false; // Nothing more to do here. 
        case ev_keydown: 
            if (ev.data1 == KEY_PAUSE) 
            { 
                sendpause = true; 
                return true; 
            }
            
            /* CAPS lock will only go through as a keyup event
            if (ev.data1 == KEY_CAPSLOCK) 
            { 
                if (justfocused) justfocused=false;
                    else
                // If caps are turned on, turn autorun on anyway.
                if (!alwaysrun) {
                    alwaysrun=true;
                    players[consoleplayer].message=String.format("Always run: %s",alwaysrun);
                    }
                return true; 
            } */
            
            if (ev.data1 <NUMKEYS) 
                gamekeydown[ev.data1] = true;
            return true;    // eat key down events 

        case ev_keyup:
            if (ev.data1 == KEY_CAPSLOCK) 
            { 
                if (justfocused) justfocused=false;
                    else
                    {
                    // Just toggle it. It's too hard to read the state.
                    alwaysrun=!alwaysrun;
                    players[consoleplayer].message=String.format("Always run: %s",alwaysrun);
                    }
            }
            
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

                // MAES: this is where actual demo commands are being issued or created!
                // Essentially, a demo is a sequence of stored ticcmd_t with a header.
                // Knowing that, it's possible to objectify it.
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
                        consistancy[i][buf] = (short) RND.getIndex(); 
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
                        // MAES: fixed stupid ^pause bug.
                        paused = !paused; 
                        if (paused)
                            S.PauseSound ();
                        else
                            S.ResumeSound (); 
                        break; 

                    case BTS_SAVEGAME: 
                        if (savedescription==null) 
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

    @Override
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
        ss = LL.PointInSubsector (x,y); 
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

/**
 *  M_Screenshot
 *  
 *  Currently saves PCX screenshots, and only in devparm.
 *  Very oldschool ;-)
 *  
 *  TODO: add non-devparm hotkey for screenshots, sequential screenshot
 *  messages, option to save as either PCX or PNG. Also, request
 *  current palette from VI (otherwise gamma settings and palette effects
 *  don't show up).
 *  
 */

public void ScreenShot ()
{
    int     i;
    byte[]  linear;
    String format=new String("DOOM%c%c%c%c.pcx");
    String lbmname = null;
    
    // munge planar buffer to linear
    linear = V.getScreen(2);
    VI.ReadScreen (linear);

    // find a file name to save it to
    
    char[] digit=new char[4];
    
    for (i=0 ; i<=9999 ; i++)
    {
    digit[0] = (char) (i/1000 + '0');
    digit[1] = (char) (i/100 + '0');
    digit[2] = (char) (i/10 + '0');
    digit[3] =  (char) (i%10 + '0');
    lbmname=String.format(format, digit[0],digit[1],digit[2],digit[3]);
    if (!C2JUtils.testAccess(lbmname,"r"))
        break;  // file doesn't exist
    }
    if (i==10000)
    I.Error ("M_ScreenShot: Couldn't create a PCX");

    // save the pcx file
    MenuMisc.WritePCXfile (lbmname, linear,
          SCREENWIDTH, SCREENHEIGHT,
          W.CacheLumpNameAsRawBytes ("PLAYPAL",PU_CACHE));

    players[consoleplayer].message = "screen shot";
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
        if ( isCommercial()
                && (W.CheckNumForName("MAP31")<0))
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

        if ( !isCommercial())
            switch(gamemap)
            {
            case 8:
                // MAES: end of episode
                gameaction =  gameaction_t.ga_victory;
                return;
            case 9:
                // MAES: end of secret level
                for (i=0 ; i<MAXPLAYERS ; i++) 
                    players[i].didsecret = true; 
                break;
            }

        /*  Hmmm - why? MAES: Clearly redundant.
        if ( (gamemap == 8)
                && (!isCommercial()) ) 
        {
            // victory 
            gameaction =  gameaction_t.ga_victory; 
            return; 
        } 

        if ( (gamemap == 9)
                && !isCommercial() ) 
        {
            // exit secret level 
            for (i=0 ; i<MAXPLAYERS ; i++) 
                players[i].didsecret = true; 
        } 
         */


        wminfo.didsecret = players[consoleplayer].didsecret; 
        wminfo.epsd = gameepisode -1; 
        wminfo.last = gamemap -1;

        // wminfo.next is 0 biased, unlike gamemap
        if ( isCommercial())
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
        if ( isCommercial() )
            wminfo.partime = 35*cpars[gamemap-1]; 
        else if (gameepisode >= pars.length)
            wminfo.partime = 0;
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


    /**
     * G_WorldDone 
     */

    public void WorldDone () 
    { 
        gameaction = gameaction_t.ga_worlddone; 

        if (secretexit) 
            players[consoleplayer].didsecret = true; 

        if ( isCommercial() )
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

    protected void DoLoadGame () 
    { 
        try{ 
            int     i;  
            StringBuffer vcheck=new StringBuffer();
            VanillaDSGHeader header=new VanillaDSGHeader();
            IDoomSaveGame dsg=new VanillaDSG();
            dsg.updateStatus(this.DM);
            gameaction = gameaction_t.ga_nothing; 

            DoomFile f=new DoomFile(savename, "r"); 

            header.read(f);
            f.seek(0);


            // skip the description field 
            vcheck.append("version ");
            vcheck.append(VERSION);

            if (vcheck.toString().compareTo(header.getVersion())!=0) 
                return;             // bad version 


            gameskill = header.getGameskill(); 
            gameepisode = header.getGameepisode(); 
            gamemap = header.getGamemap(); 
            for (i=0 ; i<MAXPLAYERS ; i++) 
                playeringame[i] = header.getPlayeringame()[i]; 

            // load a base level 
            InitNew (gameskill, gameepisode, gamemap); 

            // get the times 
            leveltime = header.getLeveltime(); 

            // dearchive all the modifications
            boolean ok=dsg.doLoad(f);
            f.close();

            // MAES: this will cause a forced exit.
            // The problem is that the status will have already been altered 
            // (perhaps VERY badly) so it makes no sense to progress.
            // If you want it bullet-proof, you could implement
            // a "tentative loading" subsystem, which will only alter the game
            // if everything works out without errors. But who cares :-p
            if (!ok) 
                I.Error("Bad savegame");

            // done 
            //Z_Free (savebuffer); 

            if (R.setsizeneeded)
                R.ExecuteSetViewSize ();

            // draw the pattern into the back screen
            R.FillBackScreen ();   
        } catch (Exception e){
            e.printStackTrace();
        }
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
     
        try{
     String    name; 
     //char[]    name2=new char[VERSIONSIZE]; 
     String   description;
     StringBuffer build=new StringBuffer();
     IDoomSaveGameHeader header=new VanillaDSGHeader();
     IDoomSaveGame dsg=new VanillaDSG();
     dsg.updateStatus(this.DM);
     int     length; 
     int     i; 

     if (eval(CM.CheckParm("-cdrom"))) {
         build.append("c:\\doomdata\\");
         build.append(SAVEGAMENAME);
         build.append("%d.dsg");
     } else {         
      build.append(SAVEGAMENAME);
      build.append("%d.dsg"); 
     }
     
     name=String.format(build.toString(), savegameslot);
     
     description = savedescription; 

     header.setName(description);
     header.setVersion(String.format("version %d",VERSION));
     header.setGameskill(gameskill);
     header.setGameepisode(gameepisode);
     header.setGamemap(gamemap);
     header.setPlayeringame(playeringame);
     header.setLeveltime(leveltime);
     dsg.setHeader(header);
     
     // Try opening a save file. No intermediate buffer (performance?)
     DoomFile f=new DoomFile(name, "rw");
     boolean ok=dsg.doSave(f);
     f.close();
        } catch (Exception e){
            e.printStackTrace();
        }
     // Saving is not as destructive as loading.
     
     gameaction = gameaction_t.ga_nothing; 
        savedescription = "";      

        players[consoleplayer].message = GGSAVED; 

        // draw the pattern into the back screen
        R.FillBackScreen ();
     

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

        if ( isRetail() )
        {
            if (episode > 4)
                episode = 4;
        }
        else if ( isShareware() )
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
                && ( !isCommercial()))
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
        if ( isCommercial())
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
    public  void ReadDemoTiccmd (ticcmd_t cmd) 
    { 
        IDemoTicCmd democmd=demobuffer.getNextTic();
        if (democmd == null) 
        {
            // end of demo data stream 
            CheckDemoStatus ();

            // Force status resetting
            this.demobuffer.resetDemo();
            return; 
        }

        democmd.decode(cmd); 
    } 

    public void WriteDemoTiccmd (ticcmd_t cmd) 
    { 
        if (gamekeydown['q'])           // press q to end demo recording 
            CheckDemoStatus ();
        IDemoTicCmd reccmd=new VanillaTiccmd();
        reccmd.encode(cmd);     
        demobuffer.putTic(reccmd);

        // MAES: Useless, we can't run out of space anymore (at least not in theory).

        /*   demobuffer[demo_p++] = cmd.forwardmove; 
     demobuffer[demo_p++] = cmd.sidemove; 
     demobuffer[demo_p++] = (byte) ((cmd.angleturn+128)>>8); 
     demobuffer[demo_p++] = (byte) cmd.buttons; 
     demo_p -= 4; 
     if (demo_p > demoend - 16)
     {
     // no more space 
     CheckDemoStatus (); 
     return; 
     } */ 

        //ReadDemoTiccmd (cmd);         // make SURE it is exactly the same
        // MAES: this is NOT the way to do in Mocha, because we are not manipulating
        // the demo index directly anymore. Instead, decode what we have just saved.     
        reccmd.decode(cmd);

    } 



    /**
     * G_RecordDemo 
     */ 
    public void RecordDemo (String name) 
    { 
        int             i; 
        // int             maxsize;

        StringBuffer buf=new StringBuffer();
        usergame = false; 
        buf.append(name); 
        buf.append(".lmp");
        demoname=buf.toString();
        // maxsize = 0x20000;
        i = CM.CheckParm ("-maxdemo");
        // if (i!=0 && i<CM.getArgc()-1)
        // maxsize = Integer.parseInt(myargv[i+1])*1024;
        demobuffer = new VanillaDoomDemo(); 
        //demoend = maxsize;

        demorecording = true; 
    } 


    public void BeginRecording () 
    {         
        demobuffer.setVersion(VERSION);
        demobuffer.setSkill(gameskill); 
        demobuffer.setEpisode(gameepisode);
        demobuffer.setMap(gamemap);
        demobuffer.setDeathmatch(deathmatch);
        demobuffer.setRespawnparm(respawnparm);
        demobuffer.setFastparm(fastparm);
        demobuffer.setNomonsters(nomonsters);
        demobuffer.setConsoleplayer(consoleplayer);
        demobuffer.setPlayeringame(playeringame);
    }
    
    String   defdemoname;
    
    /**
     * G_PlayDemo 
     */

    public void DeferedPlayDemo (String name) 
    { 
        defdemoname = name; 
        gameaction = gameaction_t.ga_playdemo; 
    } 

    public void DoPlayDemo () 
    { 

        skill_t skill; 
        int             i, episode, map; 

        gameaction = gameaction_t.ga_nothing;
        // MAES: Yeah, it's OO all the way now, baby ;-)
        demobuffer = (IDoomDemo) W.CacheLumpName(defdemoname.toUpperCase(), PU_STATIC,VanillaDoomDemo.class);

        if (demobuffer.getVersion()!= VERSION)
        {
            System.err.println("Demo is from a different game version!\n");
            System.err.println("Read "+demobuffer.getVersion());
            gameaction = gameaction_t.ga_nothing;
            return;
        }

        skill = demobuffer.getSkill(); 
        episode = demobuffer.getEpisode(); 
        map = demobuffer.getMap(); 
        deathmatch = demobuffer.isDeathmatch();
        respawnparm = demobuffer.isRespawnparm();
        fastparm = demobuffer.isFastparm();
        nomonsters = demobuffer.isNomonsters();
        consoleplayer = demobuffer.getConsoleplayer();
        // Do this, otherwise previously loaded demos will be stuck at their end.
        demobuffer.resetDemo();
        
        boolean[] pigs=demobuffer.getPlayeringame();
        for (i=0 ; i<MAXPLAYERS ; i++) 
            playeringame[i] = pigs[i]; 
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

    }

    //
    // G_TimeDemo 
    //
    public void TimeDemo (String name) 
    {    
        nodrawers = CM.CheckParm ("-nodraw")!=0; 
        noblit = CM.CheckParm ("-noblit")!=0; 
        timingdemo = true; 
        singletics = true;
        defdemoname = name;
        gameaction = gameaction_t.ga_playdemo; 
    } 

    
    /** G_CheckDemoStatus
     * 
     * Called after a death or level completion to allow demos to be cleaned up
     * Returns true if a new demo loop action will take place
     *  
     */ 

    public boolean CheckDemoStatus () 
    { 
        int endtime; 
        
        if (timingdemo) 
        {
            endtime = RealTime.GetTime ();
            // killough -- added fps information and made it work for longer demos:
            long realtics=endtime-starttime;    
            
            MenuMisc.SaveDefaults(this);
            I.Error ("timed %d gametics in %d realtics = %f frames per second",gametic 
                , realtics, gametic*(double)(TICRATE)/realtics); 
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
            //demobuffer[demo_p++] = (byte) DEMOMARKER; 

            MenuMisc.WriteFile(demoname, demobuffer); 
            //Z_Free (demobuffer); 
            demorecording = false; 
            I.Error ("Demo %s recorded",demoname); 
        } 

        return false; 
    } 

    /** This should always be available for real timing */
    private ITicker RealTime;
    
    public DoomMain(){
        // Init game status...
        super();
        C2JUtils.initArrayOfObjects(events,event_t.class);
        this.I=new DoomSystem();
        //_D_: this needed to be removed, and isnt here in the linuxdoom source
        // this is because some variables are null at this point so we cant init the doomsystem yet
        //I.Init();
        gamestate=gamestate_t.GS_DEMOSCREEN;        
        
        this.RealTime=new MilliTicker();
    }

    /**
     * Since this is a fully OO implementation, we need a way to create
     * the instances of the Refresh daemon, the Playloop, the Wadloader 
     * etc. which however are now completely independent of each other
     * (well, ALMOST), and are typically only passed context when 
     * instantiated.
     * 
     *  If you instantiate one too early, it will have null context.
     *  
     *  The trick is to construct objects in the correct order. Some of
     *  them have Init() methods which are NOT yet safe to call.
     *  
     *  FIXME: Probably I should add a sort of deferred status update?
     * 
     * */

    public void Init(){
       
        // The various objects that need to "sense" the global status
        // end up here. This allows one-call updates.
        status_holders=new ArrayList<DoomStatusAware>();
        
        // Doommain is both "main" and handles most of the game status.
        this.DM=this;
        this.DG = this;
        this.DGN=this; // DoomMain also handles its own Game Networking.
             
        // Set ticker. It is a shared status object, but not a holder itself.
        if (eval(CM.CheckParm("-millis"))){

            TICK=new MilliTicker();
        }
        else if (eval(CM.CheckParm("-fasttic"))){
            TICK=new FastTicker();
        } else {
            TICK=new NanoTicker();
        }
        
        // Network "driver"
        status_holders.add((DoomStatusAware) (this.DNI=new DummyNetworkDriver(this)));
        
        // Random number generator, but we can have others too.
        this.RND=new DoomRandom();
        
        // Sound can be left until later, in Start

        this.W=new WadLoader(this.I); // The wadloader is a "weak" status holder.
        status_holders.add(this.WIPE=new Wiper(this));   

        // Then the menu...
        status_holders.add(this.HU=new HU(this));
        status_holders.add(this.M=new Menu(this));
        status_holders.add(this.LL=new LevelLoader(this));
        
        // This will set R.
        selectRenderer();
        status_holders.add(this.R);
        status_holders.add((this.P=new Actions(this)));
        status_holders.add(this.ST=new StatusBar(this));
        status_holders.add(this.AM=new Map(this)); // Call Init later.
        this.TM=new SimpleTextureManager(this);
        this.SM=this.R;

        //_D_: well, for EndLevel and Finale to work, they need to be instanciated somewhere!
        // it seems to fit perfectly here
        status_holders.add(this.WI = new EndLevel(this));    
        status_holders.add(this.F = new Finale(this));
        
        // TODO: find out if we have requests for a specific resolution,
        // and try honouring them as closely as possible.       

        // 23/5/2011: Experimental dynamic resolution subsystem
        vs=VisualSettings.parse(CM);
        
        // Initializing actually sets drawing positions, constants,
        // etc. for main. Children will be set later in Start().
        this.initScaling();
        
        this.V=new BufferedRenderer(SCREENWIDTH,SCREENHEIGHT);

        status_holders.add((DoomStatusAware) this.I);
        status_holders.add((DoomStatusAware) this.V);
        updateStatusHolders(this);

        DanmakuShape.InitShapes();
        DanmakuPatterns.InitPatterns();
    }
    
    private void updateStatusHolders(DoomStatus DS){
        for (DoomStatusAware dsa : status_holders){
            dsa.updateStatus(DS);
        }
    }

    /** A list with objects that do hold status and need to be aware of 
     *  it.
     */
    private List<DoomStatusAware> status_holders;

    protected void selectRenderer() {
        // Serial or parallel renderer (serial is default, but can be forced)
        if (eval(CM.CheckParm("-serialrenderer"))){
            this.R=new UnifiedRenderer(this);    
        } else 

            // Parallel. Either with default values (2,1) or user-specified.
            if (eval(CM.CheckParm("-parallelrenderer"))){        
                int p = CM.CheckParm ("-parallelrenderer");
                if (eval(p) && p < CM.getArgc()-1)
                {
                    // Next two args must be numbers.
                    int walls=2, floors=1;
                    startmap = Integer.parseInt(CM.getArgv(p+1));
                    // Try parsing walls.
                    try {
                        walls=Integer.parseInt(CM.getArgv(p+1));
                    } catch (Exception e){
                        // OK, move on anyway.
                    }

                    // Try parsing floors. If wall succeeded, but floors
                    // not, it will default to 1.
                    try {
                        floors=Integer.parseInt(CM.getArgv(p+2));
                    } catch (Exception e){
                        // OK, move on anyway.
                    }

                    // In the worst case, we will use the defaults.
                    this.R=new ParallelRenderer(this,walls,floors);
                }
            } else {
                // Force serial
                this.R=new UnifiedRenderer(this);   
            }
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

    // _D_
    boolean     reboundpacket = false;
    doomdata_t  reboundstore = new doomdata_t();


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



    /**
     * HSendPacket
     *
     * Will send out a packet to all involved parties. A special
     * case is the rebound storage, which acts as a local "echo"
     * which is then picked up by the host itself. This is
     * necessary to simulate a 1-node network.
     *
     */
    void
    HSendPacket
    (int   node,
            int   flags ) 
    {
        netbuffer.checksum = (int) (NetbufferChecksum () | flags);

        // Local node's comms are sent to rebound packet, which is 
        // then picked up again. THIS IS VITAL FOR SINGLE-PLAYER
        // SPEED THROTTLING TOO, AS IT RELIES ON NETWORK ACKS/BUSY
        // WAITING.
        if (node==0)
        {
            // _D_
            reboundstore.copyFrom(netbuffer);
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

        // This should execute a "send" command for the current stuff in doomcom.
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


    ////    GetPackets

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

                    //_D_: had to add this (see linuxdoom source). That fixed that damn consistency failure!!!
                    if (start < netbuffer.cmds.length)
                        src = netbuffer.cmds[start];

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
        nowtime = TICK.GetTime ()/ticdup;
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

                //System.out.printf ("mk:%d ",maketic);
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
                        localcmds[(realstart+j)%BACKUPTICS].copyTo(netbuffer.cmds[j]);
                    // MAES: one of _D_ fixes.
                    //netbuffer.cmds[j] = localcmds[(realstart+j)%BACKUPTICS];

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
            GetPackets();
        }

    }



    //
    // CheckAbort
    //
    private void CheckAbort ()
    {
        event_t ev;
        int     stoptic;

        stoptic = TICK.GetTime () + 2; 
        while (TICK.GetTime() < stoptic) 
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

    /**
     * D_ArbitrateNetStart
     *
     * 
     */
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

        // Maes: This is the only place where netbuffer is definitively set to something
        netbuffer = doomcom.data;
        consoleplayer = displayplayer = doomcom.consoleplayer;
        if (netgame)
            ArbitrateNetStart ();

        System.out.printf ("startskill %s  deathmatch: %s  startmap: %d  startepisode: %d\n",
            startskill.toString(), Boolean.toString(deathmatch), startmap, startepisode);

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

        System.out.printf ("player %d of %d (%d nodes)\n",(consoleplayer+1), doomcom.numplayers, doomcom.numnodes);

    }


    //
    // D_QuitNetGame
    // Called before quitting to leave a net game
    // without hanging the other players
    //
    @Override
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
        entertic = TICK.GetTime ()/ticdup;
        realtics = entertic - oldentertics;
        oldentertics = entertic;

        //System.out.printf("Entertic %d, realtics %d, oldentertics %d\n",entertic,realtics,oldentertics);

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
                    System.out.print("-");
                }
                frameskip[frameon&3] = oldnettics > nettics[nodeforplayer[i]];
                oldnettics = nettics[0];
                if (frameskip[0] && frameskip[1] && frameskip[2] && frameskip[3])
                {
                    skiptics = 1;
                    System.out.print("+");
                }
            }
        }// demoplayback

        // wait for new tics if needed
        while (lowtic < gametic/ticdup + counts)    
        {
            NetUpdate ();   
            lowtic = MAXINT;

            // Finds the node with the lowest number of tics.
            for (i=0 ; i<doomcom.numnodes ; i++)
                if (nodeingame[i] && nettics[i] < lowtic)
                    lowtic = nettics[i];

            if (lowtic < gametic/ticdup)
                I.Error ("TryRunTics: lowtic < gametic");

            // don't stay in here forever -- give the menu a chance to work
            int time=TICK.GetTime();
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


    @Override
    public void setGameAction(gameaction_t action) {
        this.gameaction=action;
    }


    @Override
    public gameaction_t getGameAction() {       
        return this.gameaction;
    }

    ////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

    protected int SCREENWIDTH;
    protected int SCREENHEIGHT;
    protected int SAFE_SCALE;
    protected IVideoScale vs;

    @Override
    public void setVideoScale(IVideoScale vs) {
        this.vs=vs;
    }

    @Override
    public void initScaling() {
        this.SCREENHEIGHT=vs.getScreenHeight();
        this.SCREENWIDTH=vs.getScreenWidth();
        this.SAFE_SCALE=vs.getSafeScaling();
    }


    public void setCommandLineArgs(ICommandLineManager cM) {
        this.CM=cM;

    }


    public boolean shouldPollLockingKeys() {
        if (keysCleared) {
            keysCleared = false;
            return true;
        }
        return false;
    }

}

//$Log: DoomMain.java,v $
//Revision 1.72.2.6  2011/07/31 11:46:32  velktron
//Added new autorun checks and sound driver params.
//
//Revision 1.72.2.5  2011/07/26 17:53:34  jodwin
//MAJOR AWESOME DANMAKU UPDATE
//
//Revision 1.72.2.4  2011/07/23 18:50:28  jodwin
//I'M AWESOME AND YOU ARE NOT
//
//Revision 1.72.2.3  2011/07/23 12:41:41  velktron
//Brought up-to-date with Callbacks version. Major changes in Actions, look in ActionFunctions.java for A_ stuff. Minor changes in mobj_t. Includes -angle specific stuff
//
//Revision 1.77  2011/07/18 21:45:00  velktron
//Sound driver changes. -nosound loads Dummy instead of Abstract (saves CPU cycles).
//
//Revision 1.76  2011/07/17 12:43:18  velktron
//Merged in finnw's Ultimate Doom par times fixes, locking keys method.
//
//Revision 1.75  2011/07/15 13:56:55  velktron
//Screenshots functional (with devparm, in PCX format).
//
//Revision 1.74  2011/07/13 16:38:42  velktron
//Sound updates through wipe fixed.
//
//Revision 1.73  2011/07/05 13:27:58  velktron
//Added more solid Ultimate Doom detection.
//
//Revision 1.72  2011/06/23 15:43:01  velktron
//Palette defaulting mechanism in place.
//
//Revision 1.71  2011/06/14 20:59:47  velktron
//Channel settings now read from default.cfg. Changes in sound creation order.
//
//Revision 1.70  2011/06/14 10:06:37  velktron
//-nosound -> - nomusic
//
//Revision 1.69  2011/06/14 10:01:13  velktron
//Removed use of DummySoundDriver, used SFX/Music dummies for nosound.
//
//Revision 1.68  2011/06/12 21:55:18  velktron
//Defaulting/testing new sound "drivers"
//
//Revision 1.67  2011/06/10 17:03:26  velktron
//We don't need to be so verbose anymore ;-)
//
//Revision 1.66  2011/06/08 17:43:29  velktron
//VI -> V
//
//Revision 1.65  2011/06/08 17:41:57  velktron
//Added VI to list of status holders.
//
//Revision 1.64  2011/06/04 11:05:09  velktron
//Added use of _D_'s sound system. Consider VERY experimental for now.
//
//Revision 1.63  2011/06/02 14:54:53  velktron
//MochaEvents is default. IZone connector for IWadloader.
//
//Revision 1.62  2011/06/01 17:40:17  velktron
//Techdemo v1.4a level. Default novert and experimental mochaevents interface.
//
//Revision 1.61  2011/06/01 17:35:56  velktron
//Techdemo v1.4a level. Default novert and experimental mochaevents interface.
//
//Revision 1.60  2011/06/01 00:46:15  velktron
//-keycode debug.
//
//Revision 1.59  2011/06/01 00:37:59  velktron
//Changed default keys to WASD.
//
//Revision 1.58  2011/06/01 00:08:07  velktron
//Added +map command line parameter.
//
//Revision 1.57  2011/05/31 22:43:18  velktron
//Added support for quoted IWAD and PWAD args.
//
//Revision 1.56  2011/05/31 21:45:51  velktron
//Added XBLA version as explicitly supported.
//
//Revision 1.55  2011/05/31 17:10:57  velktron
//Fixed demo autostart
//
//Revision 1.54  2011/05/31 16:26:10  velktron
//Sprite buffer reset.
//
//Revision 1.53  2011/05/30 15:50:58  velktron
//Status holders and -fastdemo introduced.
//
//Revision 1.52  2011/05/30 02:26:29  velktron
//DOOMWADDIR message.
//
//Revision 1.51  2011/05/29 22:15:32  velktron
//Introduced IRandom interface.
//
//Revision 1.50  2011/05/29 21:07:09  velktron
//Added FORCED and PAINFUL event clearing!
//
//Revision 1.49  2011/05/27 19:44:39  velktron
//More fooling around with savegames.
//
//Revision 1.48  2011/05/27 13:26:56  velktron
//A slightly better, though not perfect, way to handle input, partially based on_D_'s work.
//
//Revision 1.47  2011/05/26 17:52:52  velktron
//Several fixes. Pause bug, added more command-line options, started saving subsystem.
//
//Revision 1.46  2011/05/25 18:46:24  velktron
//Implemented event_t pooling/reuse.
//
//Revision 1.45  2011/05/25 17:56:52  velktron
//Introduced some fixes for mousebuttons etc.
//
//Revision 1.44  2011/05/23 17:00:23  velktron
//Migrated to VideoScaleInfo, DoomMain now is IGN.
//
//Revision 1.43  2011/05/22 21:10:38  velktron
//Fixed an INCREDIBLY stupid bug in the wiper code, which prevented it from working correctly all this time -_-
//
//Revision 1.42  2011/05/21 16:58:38  velktron
//Added automatic detection of Ultimate Doom in doom.wad file.
//
//Revision 1.41  2011/05/21 14:40:56  velktron
//Hid gamemode behind specific getters.
//
//Revision 1.40  2011/05/20 14:49:01  velktron
//Added more DoomGame compliance, implemented loading savegames.
//
//Revision 1.39  2011/05/17 16:54:34  velktron
//Now adaptiveness/throttling works
//
//Revision 1.38  2011/05/13 18:26:42  velktron
//Added demo reset feature.
//
//Revision 1.37  2011/05/13 17:43:02  velktron
//Improved demo handling, aka they actually do work (sort of).
//
//Revision 1.36  2011/05/13 11:15:09  velktron
//Demo preliminaries
//
//Revision 1.35  2011/05/11 14:09:40  velktron
//Implements IDoomGame
//
//Revision 1.34  2011/05/10 19:10:37  velktron
//Some crazy and wild stuff.
//
//Revision 1.33  2011/05/06 14:00:54  velktron
//More of _D_'s changes committed.