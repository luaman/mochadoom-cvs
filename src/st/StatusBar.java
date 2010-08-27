package st;

// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: StatusBar.java,v 1.7 2010/08/27 23:46:57 velktron Exp $
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// $Log: StatusBar.java,v $
// Revision 1.7  2010/08/27 23:46:57  velktron
// Introduced Buffered renderer, which makes tapping directly into byte[] screen buffers mapped to BufferedImages possible.
//
// Revision 1.6  2010/08/26 16:43:42  velktron
// Automap functional, biatch.
//
// Revision 1.5  2010/08/25 15:16:07  velktron
// Status bar functional.
//
// Revision 1.4 2010/08/25 00:50:59 velktron
// Some more work...
//
// Revision 1.3 2010/08/13 14:06:36 velktron
// Endlevel screen fully functional!
//
// Revision 1.2 2010/07/20 15:52:56 velktron
// LOTS of changes, Automap almost complete. Use of fixed_t inside methods
// severely limited.
//
// Revision 1.1 2010/07/03 23:24:13 velktron
// Added a LOT of stuff, like Status bar code & objects. Now we're cooking with
// gas!
//
// Revision 1.1 2010/06/30 08:58:51 velktron
// Let's see if this stuff will finally commit....
//
//
// Most stuff is still being worked on. For a good place to start and get an
// idea of what is being done, I suggest checking out the "testers" package.
//
// Revision 1.1 2010/06/29 11:07:34 velktron
// Release often, release early they say...
//
// Commiting ALL stuff done so far. A lot of stuff is still broken/incomplete,
// and there's still mixed C code in there. I suggest you load everything up in
// Eclpise and see what gives from there.
//
// A good place to start is the testers/ directory, where you can get an idea of
// how a few of the implemented stuff works.
//
//
// DESCRIPTION:
// Status bar code.
// Does the face/direction indicator animatin.
// Does palette indicators as well (red pain/berserk, bright pickup)
//
// -----------------------------------------------------------------------------

import static data.Defines.*;
import static doom.englsh.*;
import static automap.DoomAutoMap.*;
import static doom.player_t.*;
import static doom.items.*;
import static data.Tables.*;
import p.mobj_t;

import i.system;
import m.cheatseq_t;
import m.random;
import data.doomstat;
import data.sounds.musicenum_t;
import doom.DoomContext;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;
import doom.weapontype_t;

import rr.Renderer;
import rr.patch_t;
import v.DoomVideoRenderer;
import v.SimpleRenderer;
import w.WadLoader;

public class StatusBar implements DoomStatusBarInterface {
    public static final String rcsid =
        "$Id: StatusBar.java,v 1.7 2010/08/27 23:46:57 velktron Exp $";

    // /// STATUS //////////

    protected DoomVideoRenderer V;

    protected WadLoader W;

    protected Renderer R;

    protected doomstat DS;

    protected random RND;

    // Size of statusbar.
    // Now sensitive for scaling.
    public static int ST_HEIGHT = 32 * SCREEN_MUL;

    public static int ST_WIDTH = SCREENWIDTH;

    public static int ST_Y = (SCREENHEIGHT - ST_HEIGHT);

    /** Points to "screen 4" which is treated as a buffer */
    public static int BG = 4;
    
    /** Points to "screen 0" which is what you actually see */
    public static int FG = 0;

    //
    // STATUS BAR DATA
    //

    // Palette indices.
    // For damage/bonus red-/gold-shifts
    private static int STARTREDPALS = 1;

    private static int STARTBONUSPALS = 9;

    private static int NUMREDPALS = 8;

    private static int NUMBONUSPALS = 4;

    // Radiation suit, green shift.
    private static int RADIATIONPAL = 13;

    // N/256*100% probability
    // that the normal face state will change
    private static int ST_FACEPROBABILITY = 96;

    // For Responder
    private static int ST_TOGGLECHAT = KEY_ENTER;

    // Location of status bar
    private static int ST_X = 0;

    private static int ST_X2 = 104;

    private static int ST_FX = 143;

    private static int ST_FY = 169;

    // Should be set to patch width
    // for tall numbers later on
    // TODO: private static int ST_TALLNUMWIDTH = (tallnum[0].width);

    // Number of status faces.
    private static int ST_NUMPAINFACES = 5;

    private static int ST_NUMSTRAIGHTFACES = 3;

    private static int ST_NUMTURNFACES = 2;

    private static int ST_NUMSPECIALFACES = 3;

    private static int ST_FACESTRIDE =
        (ST_NUMSTRAIGHTFACES + ST_NUMTURNFACES + ST_NUMSPECIALFACES);

    private static int ST_NUMEXTRAFACES = 2;

    private static int ST_NUMFACES =
        (ST_FACESTRIDE * ST_NUMPAINFACES + ST_NUMEXTRAFACES);

    private static int ST_TURNOFFSET = (ST_NUMSTRAIGHTFACES);

    private static int ST_OUCHOFFSET = (ST_TURNOFFSET + ST_NUMTURNFACES);

    private static int ST_EVILGRINOFFSET = (ST_OUCHOFFSET + 1);

    private static int ST_RAMPAGEOFFSET = (ST_EVILGRINOFFSET + 1);

    private static int ST_GODFACE = (ST_NUMPAINFACES * ST_FACESTRIDE);

    private static int ST_DEADFACE = (ST_GODFACE + 1);

    private static int ST_FACESX = 143;

    private static int ST_FACESY = 168;

    private static int ST_EVILGRINCOUNT = (2 * TICRATE);

    private static int ST_STRAIGHTFACECOUNT = (TICRATE / 2);

    private static int ST_TURNCOUNT = (1 * TICRATE);

    private static int ST_OUCHCOUNT = (1 * TICRATE);

    private static int ST_RAMPAGEDELAY = (2 * TICRATE);

    private static int ST_MUCHPAIN = 20;

    // Location and size of statistics,
    // justified according to widget type.
    // Problem is, within which space? STbar? Screen?
    // Note: this could be read in by a lump.
    // Problem is, is the stuff rendered
    // into a buffer,
    // or into the frame buffer?

    // AMMO number pos.
    private static int ST_AMMOWIDTH = 3;

    private static int ST_AMMOX = 44;

    private static int ST_AMMOY = 171;

    // HEALTH number pos.
    private static int ST_HEALTHWIDTH = 3;

    private static int ST_HEALTHX = 90;

    private static int ST_HEALTHY = 171;

    // Weapon pos.
    private static int ST_ARMSX = 111;

    private static int ST_ARMSY = 172;

    private static int ST_ARMSBGX = 104;

    private static int ST_ARMSBGY = 168;

    private static int ST_ARMSXSPACE = 12;

    private static int ST_ARMSYSPACE = 10;

    // Frags pos.
    private static int ST_FRAGSX = 138;

    private static int ST_FRAGSY = 171;

    private static int ST_FRAGSWIDTH = 2;

    // ARMOR number pos.
    private static int ST_ARMORWIDTH = 3;

    private static int ST_ARMORX = 221;

    private static int ST_ARMORY = 171;

    // Key icon positions.
    private static int ST_KEY0WIDTH = 8;

    private static int ST_KEY0HEIGHT = 5;

    private static int ST_KEY0X = 239;

    private static int ST_KEY0Y = 171;

    private static int ST_KEY1WIDTH = ST_KEY0WIDTH;

    private static int ST_KEY1X = 239;

    private static int ST_KEY1Y = 181;

    private static int ST_KEY2WIDTH = ST_KEY0WIDTH;

    private static int ST_KEY2X = 239;

    private static int ST_KEY2Y = 191;

    // Ammunition counter.
    private static int ST_AMMO0WIDTH = 3;

    private static int ST_AMMO0HEIGHT = 6;

    private static int ST_AMMO0X = 288;

    private static int ST_AMMO0Y = 173;

    private static int ST_AMMO1WIDTH = ST_AMMO0WIDTH;

    private static int ST_AMMO1X = 288;

    private static int ST_AMMO1Y = 179;

    private static int ST_AMMO2WIDTH = ST_AMMO0WIDTH;

    private static int ST_AMMO2X = 288;

    private static int ST_AMMO2Y = 191;

    private static int ST_AMMO3WIDTH = ST_AMMO0WIDTH;

    private static int ST_AMMO3X = 288;

    private static int ST_AMMO3Y = 185;

    // Indicate maximum ammunition.
    // Only needed because backpack exists.
    private static int ST_MAXAMMO0WIDTH = 3;

    private static int ST_MAXAMMO0HEIGHT = 5;

    private static int ST_MAXAMMO0X = 314;

    private static int ST_MAXAMMO0Y = 173;

    private static int ST_MAXAMMO1WIDTH = ST_MAXAMMO0WIDTH;

    private static int ST_MAXAMMO1X = 314;

    private static int ST_MAXAMMO1Y = 179;

    private static int ST_MAXAMMO2WIDTH = ST_MAXAMMO0WIDTH;

    private static int ST_MAXAMMO2X = 314;

    private static int ST_MAXAMMO2Y = 191;

    private static int ST_MAXAMMO3WIDTH = ST_MAXAMMO0WIDTH;

    private static int ST_MAXAMMO3X = 314;

    private static int ST_MAXAMMO3Y = 185;

    // pistol
    private static int ST_WEAPON0X = 110;

    private static int ST_WEAPON0Y = 172;

    // shotgun
    private static int ST_WEAPON1X = 122;

    private static int ST_WEAPON1Y = 172;

    // chain gun
    private static int ST_WEAPON2X = 134;

    private static int ST_WEAPON2Y = 172;

    // missile launcher
    private static int ST_WEAPON3X = 110;

    private static int ST_WEAPON3Y = 181;

    // plasma gun
    private static int ST_WEAPON4X = 122;

    private static int ST_WEAPON4Y = 181;

    // bfg
    private static int ST_WEAPON5X = 134;

    private static int ST_WEAPON5Y = 181;

    // WPNS title
    private static int ST_WPNSX = 109;

    private static int ST_WPNSY = 191;

    // DETH title
    private static int ST_DETHX = 109;

    private static int ST_DETHY = 191;

    // Incoming messages window location
    // UNUSED
    // #define ST_MSGTEXTX (viewwindowx)
    // #define ST_MSGTEXTY (viewwindowy+viewheight-18)
    private static int ST_MSGTEXTX = 0;

    private static int ST_MSGTEXTY = 0;

    // Dimensions given in characters.
    private static int ST_MSGWIDTH = 52;

    // Or shall I say, in lines?
    private static int ST_MSGHEIGHT = 1;

    private static int ST_OUTTEXTX = 0;

    private static int ST_OUTTEXTY = 6;

    // Width, in characters again.
    private static int ST_OUTWIDTH = 52;

    // Height, in lines.
    private static int ST_OUTHEIGHT = 1;

    // TODO private static int ST_MAPWIDTH =
    // (mapnames[(gameepisode-1)*9+(gamemap-1)].length));

    // TODO private static int ST_MAPTITLEX = (SCREENWIDTH - ST_MAPWIDTH *
    // ST_CHATFONTWIDTH);

    private static int ST_MAPTITLEY = 0;

    private static int ST_MAPHEIGHT = 1;

    // MAES: most of this shit was "static".
    // main player in game
    private player_t plyr;

    // ST_Start() has just been called
    private boolean st_firsttime;

    // used to execute ST_Init() only once
    private int veryfirsttime = 1;

    // lump number for PLAYPAL
    private int lu_palette;

    // used for timing (unsigned int .. maybe long !)
    private long st_clock;

    // used for making messages go away
    int st_msgcounter = 0;

    // used when in chat
    private st_chatstateenum_t st_chatstate;

    // whether in automap or first-person
    private st_stateenum_t st_gamestate;

    // whether left-side main status bar is active
    private Boolean st_statusbaron=false;

    // whether status bar chat is active
    private boolean st_chat;

    // value of st_chat before message popped up
    private boolean st_oldchat;

    // whether chat window has the cursor on
    private boolean st_cursoron;

    /** !deathmatch */
    private Boolean st_notdeathmatch=true;

    /** !deathmatch && st_statusbaron */
    private Boolean st_armson=true;

    /** !deathmatch */
    private Boolean st_fragson=false;

    // main bar left
    private patch_t sbar;

    // 0-9, tall numbers
    private patch_t[] tallnum = new patch_t[10];

    // tall % sign
    private patch_t tallpercent;

    // 0-9, short, yellow (,different!) numbers
    private patch_t[] shortnum = new patch_t[10];

    // 3 key-cards, 3 skulls
    private patch_t[] keys = new patch_t[NUMCARDS];

    // face status patches
    private patch_t[] faces = new patch_t[ST_NUMFACES];

    // face background
    private patch_t faceback;

    // main bar right
    private patch_t armsbg;

    // weapon ownership patches
    private patch_t[][] arms = new patch_t[6][2];
   
    // // WIDGETS /////

    // ready-weapon widget
    private st_number_t w_ready;

    // in deathmatch only, summary of frags stats
    private st_number_t w_frags;

    // health widget
    private st_percent_t w_health;

    // arms background
    private st_binicon_t w_armsbg;

    // weapon ownership widgets
    private st_multicon_t[] w_arms = new st_multicon_t[6];

    // face status widget
    private st_multicon_t w_faces;

    // keycard widgets
    private st_multicon_t[] w_keyboxes = new st_multicon_t[3];

    // armor widget
    private st_percent_t w_armor;

    // ammo widgets
    private st_number_t[] w_ammo = new st_number_t[4];

    // max ammo widgets
    private st_number_t[] w_maxammo = new st_number_t[4];

    // / END WIDGETS ////

    // number of frags so far in deathmatch
    private Integer st_fragscount=0;

    // used to use appopriately pained face
    private int st_oldhealth = -1;

    // used for evil grin
    private boolean[] oldweaponsowned = new boolean[NUMWEAPONS];

    // count until face changes
    private int st_facecount = 0;

    // current face index, used by w_faces
    private int[] st_faceindex = new int[1];

    // holds key-type for each key box on bar
    private int[] keyboxes = new int[3];

    // a random number per tick
    private int st_randomnumber;

    // Massive bunches of cheat shit
    // to keep it from being easy to figure them out.
    // Yeah, right...
    private char cheat_mus_seq[] =
        { 0xb2, 0x26, 0xb6, 0xae, 0xea, 1, 0, 0, 0xff };

    private char cheat_choppers_seq[] =
        { 0xb2, 0x26, 0xe2, 0x32, 0xf6, 0x2a, 0x2a, 0xa6, 0x6a, 0xea, 0xff // id...
        };

    private char cheat_god_seq[] = { 0xb2, 0x26, 0x26, 0xaa, 0x26, 0xff // iddqd
        };

    private char cheat_ammo_seq[] = { 0xb2, 0x26, 0xf2, 0x66, 0xa2, 0xff // idkfa
        };

    private char cheat_ammonokey_seq[] = { 0xb2, 0x26, 0x66, 0xa2, 0xff // idfa
        };

    // Smashing Pumpkins Into Samml Piles Of Putried Debris.
    private char cheat_noclip_seq[] = { 0xb2, 0x26, 0xea, 0x2a, 0xb2, // idspispopd
            0xea, 0x2a, 0xf6, 0x2a, 0x26, 0xff };

    //
    private char cheat_commercial_noclip_seq[] =
        { 0xb2, 0x26, 0xe2, 0x36, 0xb2, 0x2a, 0xff // idclip
        };

    private char cheat_powerup_seq[][] =
        { { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x6e, 0xff }, // beholdv
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xea, 0xff }, // beholds
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xb2, 0xff }, // beholdi
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x6a, 0xff }, // beholdr
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xa2, 0xff }, // beholda
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x36, 0xff }, // beholdl
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xff } // behold
        };

    private char cheat_clev_seq[] =
        { 0xb2, 0x26, 0xe2, 0x36, 0xa6, 0x6e, 1, 0, 0, 0xff // idclev
        };

    // my position cheat
    private char cheat_mypos_seq[] =
        { 0xb2, 0x26, 0xb6, 0xba, 0x2a, 0xf6, 0xea, 0xff // idmypos
        };

    // Now what?
    cheatseq_t cheat_mus = new cheatseq_t(cheat_mus_seq, 0);

    cheatseq_t cheat_god = new cheatseq_t(cheat_god_seq, 0);

    cheatseq_t cheat_ammo = new cheatseq_t(cheat_ammo_seq, 0);

    cheatseq_t cheat_ammonokey = new cheatseq_t(cheat_ammonokey_seq, 0);

    cheatseq_t cheat_noclip = new cheatseq_t(cheat_noclip_seq, 0);

    cheatseq_t cheat_commercial_noclip =
        new cheatseq_t(cheat_commercial_noclip_seq, 0);

    cheatseq_t[] cheat_powerup =
        { new cheatseq_t(cheat_powerup_seq[0], 0),
                new cheatseq_t(cheat_powerup_seq[1], 0),
                new cheatseq_t(cheat_powerup_seq[2], 0),
                new cheatseq_t(cheat_powerup_seq[3], 0),
                new cheatseq_t(cheat_powerup_seq[4], 0),
                new cheatseq_t(cheat_powerup_seq[5], 0),
                new cheatseq_t(cheat_powerup_seq[6], 0) };

    cheatseq_t cheat_choppers = new cheatseq_t(cheat_choppers_seq, 0);

    cheatseq_t cheat_clev = new cheatseq_t(cheat_clev_seq, 0);

    cheatseq_t cheat_mypos = new cheatseq_t(cheat_mypos_seq, 0);

    // 
    String[] mapnames;

    //
    // STATUS BAR CODE
    //

    public StatusBar(DoomContext dC) {
        this.V=dC.V;
        this.W=dC.W;
        this.DS=dC.DS;
        this.RND=dC.RND;
    }

    public void refreshBackground() {

        if (st_statusbaron) {
            V.DrawPatch(ST_X, 0, BG, sbar);

            if (DS.netgame)
                V.DrawPatch(ST_FX, 0, BG, faceback);

            V.CopyRect(ST_X, 0, BG, ST_WIDTH, ST_HEIGHT, ST_X, ST_Y, FG);
        }

    }

    public void Init() {
        veryfirsttime = 0;
        loadData();
        // MAES: screen(4) of the Video Renderer is actually reserved for the status bar.       
        
        this.V.setScreen(4,ST_WIDTH,ST_HEIGHT);
    }

    protected boolean st_stopped = true;

    public void Start() {

        if (!st_stopped)
            Stop();

        initData();
        createWidgets();
        st_stopped = false;

    }

    public void Stop() {
        if (st_stopped)
            return;

        // TODO: I_SetPalette (W.CacheLumpNum (lu_palette, PU_CACHE));

        st_stopped = true;
    }

    public void loadData() {
        lu_palette = W.GetNumForName("PLAYPAL");
        loadGraphics();
    }

    // Respond to keyboard input events,
    // intercept cheats.

    public boolean Responder(event_t ev) {
        int i;

        // Filter automap on/off.
        if (ev.type == evtype_t.ev_keyup
                && ((ev.data1 & 0xffff0000) == AM_MSGHEADER)) {
            switch (ev.data1) {
            case AM_MSGENTERED:
                st_gamestate = st_stateenum_t.AutomapState;
                st_firsttime = true;
                break;

            case AM_MSGEXITED:
                // fprintf(stderr, "AM exited\n");
                st_gamestate = st_stateenum_t.FirstPersonState;
                break;
            }
        }

        // if a user keypress...
        else if (ev.type == evtype_t.ev_keydown) {
            if (!DS.netgame) {
                // b. - enabled for more debug fun.
                // if (gameskill != sk_nightmare) {

                // 'dqd' cheat for toggleable god mode
                if (cheat_god.CheckCheat((char) ev.data1)) {
                    plyr.cheats ^= CF_GODMODE;
                    if ((plyr.cheats & CF_GODMODE) != 0) {
                        if (plyr.mo != null)
                            plyr.mo.health = 100;

                        plyr.health[0] = 100;
                        plyr.message = STSTR_DQDON;
                    } else
                        plyr.message = STSTR_DQDOFF;
                }
                // 'fa' cheat for killer fucking arsenal
                else if (cheat_ammonokey.CheckCheat((char) ev.data1)) {
                    plyr.armorpoints[0] = 200;
                    plyr.armortype = 2;

                    for (i = 0; i < NUMWEAPONS; i++)
                        plyr.weaponowned[i] = true; // true

                    for (i = 0; i < NUMAMMO; i++)
                        plyr.ammo[i] = plyr.maxammo[i];

                    plyr.message = STSTR_FAADDED;
                }
                // 'kfa' cheat for key full ammo
                else if (cheat_ammo.CheckCheat((char) ev.data1)) {
                    plyr.armorpoints[0] = 200;
                    plyr.armortype = 2;

                    for (i = 0; i < NUMWEAPONS; i++)
                        plyr.weaponowned[i] = true; // true

                    for (i = 0; i < NUMAMMO; i++)
                        plyr.ammo[i] = plyr.maxammo[i];

                    for (i = 0; i < NUMCARDS; i++)
                        plyr.cards[i] = true;

                    plyr.message = STSTR_KFAADDED;
                }
                // 'mus' cheat for changing music
                else if (cheat_mus.CheckCheat((char) ev.data1)) {

                    char[] buf = new char[3];
                    int musnum;

                    plyr.message = STSTR_MUS;
                    cheat_mus.GetParam(buf);

                    if (DS.gamemode == GameMode_t.commercial) {
                        musnum =
                            musicenum_t.mus_runnin.ordinal() + (buf[0] - '0')
                                    * 10 + buf[1] - '0' - 1;

                        if (((buf[0] - '0') * 10 + buf[1] - '0') > 35)
                            plyr.message = STSTR_NOMUS;
                        else
                            ;
                        // TODO: S_ChangeMusic(musnum, 1);
                    } else {
                        musnum =
                            musicenum_t.mus_e1m1.ordinal() + (buf[0] - '1') * 9
                                    + (buf[1] - '1');

                        if (((buf[0] - '1') * 9 + buf[1] - '1') > 31)
                            plyr.message = STSTR_NOMUS;
                        else
                            ;
                        // TODO: S_ChangeMusic(musnum, 1);
                    }
                }
                // Simplified, accepting both "noclip" and "idspispopd".
                // no clipping mode cheat
                else if (cheat_noclip.CheckCheat((char) ev.data1)
                        || cheat_commercial_noclip.CheckCheat((char) ev.data1)) {
                    plyr.cheats ^= CF_NOCLIP;

                    if ((plyr.cheats & CF_NOCLIP) != 0)
                        plyr.message = STSTR_NCON;
                    else
                        plyr.message = STSTR_NCOFF;
                }
                // 'behold?' power-up cheats
                for (i = 0; i < 6; i++) {
                    if (cheat_powerup[i].CheckCheat((char) ev.data1)) {
                        if (plyr.powers[i] == 0)
                            ;
                        // P_GivePower( plyr, i);
                        else if (i != pw_strength)
                            plyr.powers[i] = 1;
                        else
                            plyr.powers[i] = 0;

                        plyr.message = STSTR_BEHOLDX;
                    }
                }

                // 'behold' power-up menu
                if (cheat_powerup[6].CheckCheat((char) ev.data1)) {
                    plyr.message = STSTR_BEHOLD;
                }
                // 'choppers' invulnerability & chainsaw
                else if (cheat_choppers.CheckCheat((char) ev.data1)) {
                    plyr.weaponowned[weapontype_t.wp_chainsaw.ordinal()] = true;
                    plyr.powers[pw_invulnerability] = 1; // true
                    plyr.message = STSTR_CHOPPERS;
                }
                // 'mypos' for player position
                else if (cheat_mypos.CheckCheat((char) ev.data1)) {
                    /*
                     * (static char buf[ST_MSGWIDTH]; sprintf(buf,
                     * "ang=0x%x;x,y=(0x%x,0x%x)",
                     * players[consoleplayer].mo.angle,
                     * players[consoleplayer].mo.x,
                     * players[consoleplayer].mo.y);
                     */
                    mobj_t mo = DS.players[DS.consoleplayer].mo;
                    plyr.message =
                        "ang=0x" + Integer.toHexString(mo.angle) + "0x"
                                + Integer.toHexString(mo.x) + "0x"
                                + Integer.toHexString(mo.y);
                }
            }

            // 'clev' change-level cheat
            if (cheat_clev.CheckCheat((char) ev.data1)) {
                char[] buf = new char[3];
                int epsd;
                int map;

                cheat_clev.GetParam(buf);

                if (DS.gamemode == GameMode_t.commercial) {
                    epsd = 0;
                    map = (buf[0] - '0') * 10 + buf[1] - '0';
                } else {
                    epsd = buf[0] - '0';
                    map = buf[1] - '0';
                }

                // Catch invalid maps.
                if (epsd < 1)
                    return false;

                if (map < 1)
                    return false;

                // Ohmygod - this is not going to work.
                if ((DS.gamemode == GameMode_t.retail)
                        && ((epsd > 4) || (map > 9)))
                    return false;

                if ((DS.gamemode == GameMode_t.registered)
                        && ((epsd > 3) || (map > 9)))
                    return false;

                if ((DS.gamemode == GameMode_t.shareware)
                        && ((epsd > 1) || (map > 9)))
                    return false;

                if ((DS.gamemode == GameMode_t.commercial)
                        && ((epsd > 1) || (map > 34)))
                    return false;

                // So be it.
                plyr.message = STSTR_CLEV;
                // TODO: G.DeferedInitNew(gameskill, epsd, map);
            }
        }
        return false;
    }

    protected int lastcalc;

    protected int oldhealth = -1;

    public int calcPainOffset() {
        int health = 0;

        health = plyr.health[0] > 100 ? 100 : plyr.health[0];

        if (health != oldhealth) {
            lastcalc =
                ST_FACESTRIDE * (((100 - health) * ST_NUMPAINFACES) / 101);
            oldhealth = health;
        }
        return lastcalc;
    }

    protected int lastattackdown = -1;

    protected int priority = 0;

    /**
     * This is a not-very-pretty routine which handles the face states and their
     * timing. the precedence of expressions is: dead > evil grin > turned head
     * > straight ahead
     */
    public void updateFaceWidget() {
        int badguyangle; // angle_t
        int diffang;

        boolean doevilgrin;

        if (priority < 10) {
            // dead
            if (plyr.health[0] == 0) {
                priority = 9;
                st_faceindex[0] = ST_DEADFACE;
                st_facecount = 1;
            }
        }

        if (priority < 9) {
            if (plyr.bonuscount != 0) {
                // picking up bonus
                doevilgrin = false;

                for (int i = 0; i < NUMWEAPONS; i++) {
                    if (oldweaponsowned[i] != plyr.weaponowned[i]) {
                        doevilgrin = true;
                        oldweaponsowned[i] = plyr.weaponowned[i];
                    }
                }
                if (doevilgrin) {
                    // evil grin if just picked up weapon
                    priority = 8;
                    st_facecount = ST_EVILGRINCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_EVILGRINOFFSET;
                }
            }

        }

        if (priority < 8) {
            if ((plyr.damagecount != 0) && (plyr.attacker != null)
                    && (plyr.attacker != plyr.mo)) {
                // being attacked
                priority = 7;

                if (plyr.health[0] - st_oldhealth > ST_MUCHPAIN) {
                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_OUCHOFFSET;
                } else {
                    badguyangle =
                        R.PointToAngle2(plyr.mo.x, plyr.mo.y, plyr.attacker.x,
                            plyr.attacker.y);
                    boolean obtuse; // that's another "i"

                    if (badguyangle > plyr.mo.angle) {
                        // whether right or left
                        diffang = badguyangle - plyr.mo.angle;
                        obtuse = diffang > ANG180;
                    } else {
                        // whether left or right
                        diffang = plyr.mo.angle - badguyangle;
                        obtuse = diffang <= ANG180;
                    } // confusing, aint it?

                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset();

                    if (diffang < ANG45) {
                        // head-on
                        st_faceindex[0] += ST_RAMPAGEOFFSET;
                    } else if (obtuse) {
                        // turn face right
                        st_faceindex[0] += ST_TURNOFFSET;
                    } else {
                        // turn face left
                        st_faceindex[0] += ST_TURNOFFSET + 1;
                    }
                }
            }
        }

        if (priority < 7) {
            // getting hurt because of your own damn stupidity
            if (plyr.damagecount != 0) {
                if (plyr.health[0] - st_oldhealth > ST_MUCHPAIN) {
                    priority = 7;
                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_OUCHOFFSET;
                } else {
                    priority = 6;
                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_RAMPAGEOFFSET;
                }

            }

        }

        if (priority < 6) {
            // rapid firing
            if (plyr.attackdown) {
                if (lastattackdown == -1)
                    lastattackdown = ST_RAMPAGEDELAY;
                else if (--lastattackdown == 0) {
                    priority = 5;
                    st_faceindex[0] = calcPainOffset() + ST_RAMPAGEOFFSET;
                    st_facecount = 1;
                    lastattackdown = 1;
                }
            } else
                lastattackdown = -1;

        }

        if (priority < 5) {
            // invulnerability
            if (((plyr.cheats & CF_GODMODE) != 0)
                    || (plyr.powers[pw_invulnerability] != 0)) {
                priority = 4;

                st_faceindex[0] = ST_GODFACE;
                st_facecount = 1;

            }

        }

        // look left or look right if the facecount has timed out
        if (st_facecount == 0) {
            st_faceindex[0] = calcPainOffset() + (st_randomnumber % 3);
            st_facecount = ST_STRAIGHTFACECOUNT;
            priority = 0;
        }

        st_facecount--;

    }

    protected int largeammo = 1994; // means "n/a"

    /**
     * MAES: this code updated the widgets. Now, due to the way they are
     * constructed, they originally were "hooked" to actual variables using
     * pointers so that they could tap into them directly and self-update.
     * Clearly we can't do that in Java unless said variables are inside an
     * array and we provide both the array AND an index. For other cases, we
     * must simply build ad-hoc hacks.
     */

    public void updateWidgets() {

        int i;

        // must redirect the pointer if the ready weapon has changed.
        // if (w_ready.data != plyr.readyweapon)
        // {
        if (weaponinfo[plyr.readyweapon.ordinal()].ammo == ammotype_t.am_noammo)
            w_ready.numindex = largeammo;
        else
            w_ready.numindex =
                weaponinfo[plyr.readyweapon.ordinal()].ammo.ordinal();
        // {
        // static int tic=0;
        // static int dir=-1;
        // if (!(tic&15))
        // plyr.ammo[weaponinfo[plyr.readyweapon].ammo]+=dir;
        // if (plyr.ammo[weaponinfo[plyr.readyweapon].ammo] == -100)
        // dir = 1;
        // tic++;
        // }
        w_ready.data = plyr.readyweapon.ordinal();

        // if (*w_ready.on)
        // STlib_updateNum(&w_ready, true);
        // refresh weapon change
        // }

        // update keycard multiple widgets
        for (i = 0; i < 3; i++) {
            keyboxes[i] = plyr.cards[i] ? i : -1;

            if (plyr.cards[i + 3])
                keyboxes[i] = i + 3;
        }

        // refresh everything if this is him coming back to life
        updateFaceWidget();

        // used by the w_armsbg widget
        st_notdeathmatch = !DS.deathmatch;

        // used by w_arms[] widgets
        st_armson = st_statusbaron && !DS.deathmatch;

        // used by w_frags widget
        st_fragson = DS.deathmatch && st_statusbaron;
        st_fragscount = 0;

        for (i = 0; i < MAXPLAYERS; i++) {
            if (i != DS.consoleplayer)
                st_fragscount += plyr.frags[i];
            else
                st_fragscount -= plyr.frags[i];
        }

        // get rid of chat window if up because of message
        if (--st_msgcounter == 0)
            st_chat = st_oldchat;

    }

    public void Ticker() {

        st_clock++;
        st_randomnumber = RND.M_Random();
        updateWidgets();
        st_oldhealth = plyr.health[0];

    }

    static int st_palette = 0;

    public void doPaletteStuff() {

        int palette;
        byte[] pal;
        int cnt;
        int bzc;

        cnt = plyr.damagecount;

        if (plyr.powers[pw_strength] != 0) {
            // slowly fade the berzerk out
            bzc = 12 - (plyr.powers[pw_strength] >> 6);

            if (bzc > cnt)
                cnt = bzc;
        }

        if (cnt != 0) {
            palette = (cnt + 7) >> 3;

            if (palette >= NUMREDPALS)
                palette = NUMREDPALS - 1;

            palette += STARTREDPALS;
        }

        else if (plyr.bonuscount != 0) {
            palette = (plyr.bonuscount + 7) >> 3;

            if (palette >= NUMBONUSPALS)
                palette = NUMBONUSPALS - 1;

            palette += STARTBONUSPALS;
        }

        else if (plyr.powers[pw_ironfeet] > 4 * 32
                || (plyr.powers[pw_ironfeet] & 8) != 0)
            palette = RADIATIONPAL;
        else
            palette = 0;

        if (palette != st_palette) {
            st_palette = palette;
            // TODO: pal = (byte *) W_CacheLumpNum (lu_palette,
            // PU_CACHE)+palette*768;
            // TODO: I_SetPalette (pal);
        }

    }

    public void drawWidgets(boolean refresh) {
        int i;

        // used by w_arms[] widgets
        st_armson = st_statusbaron && !DS.deathmatch;

        // used by w_frags widget
        st_fragson = DS.deathmatch && st_statusbaron;

        w_ready.update(refresh);

        for (i = 0; i < 4; i++) {
            w_ammo[i].update(refresh);
            w_maxammo[i].update(refresh);
        }

        w_health.update(refresh);
        w_armor.update(refresh);

        w_armsbg.update(refresh);

        for (i = 0; i < 6; i++)
            w_arms[i].update(refresh);

        w_faces.update(refresh);

        for (i = 0; i < 3; i++)
            w_keyboxes[i].update(refresh);

        w_frags.update(refresh);

    }

    public void doRefresh() {

        st_firsttime = false;

        // draw status bar background to off-screen buff
        refreshBackground();

        // and refresh all widgets
        drawWidgets(true);

    }

    public void diffDraw() {
        // update all widgets
        drawWidgets(false);
    }

    public void Drawer(boolean fullscreen, boolean refresh) {

        st_statusbaron = (!fullscreen) || DS.automapactive;
        st_firsttime = st_firsttime || refresh;

        // Do red-/gold-shifts from damage/items
        doPaletteStuff();

        // If just after ST_Start(), refresh all
        if (st_firsttime)
            doRefresh();
        // Otherwise, update as little as possible
        else
            diffDraw();

    }

    public void loadGraphics() {

        int i;
        int j;
        int facenum;

        String namebuf;

        // Load the numbers, tall and short
        for (i = 0; i < 10; i++) {
            namebuf = ("STTNUM" + i);
            tallnum[i] = W.CachePatchName(namebuf, PU_STATIC);

            namebuf = ("STYSNUM" + i);
            shortnum[i] = W.CachePatchName(namebuf, PU_STATIC);

        }

        // Load percent key.
        // Note: why not load STMINUS here, too?
        tallpercent = W.CachePatchName("STTPRCNT", PU_STATIC);
        // MAES: in fact, I do this for sanity. Fuck them. Seriously.
        sttminus= W.CachePatchName("STTMINUS");

        // key cards
        for (i = 0; i < NUMCARDS; i++) {
            namebuf = ("STKEYS" + i);
            keys[i] = W.CachePatchName(namebuf, PU_STATIC);
        }

        // arms background
        armsbg = W.CachePatchName("STARMS", PU_STATIC);

        // arms ownership widgets
        for (i = 0; i < 6; i++) {
            namebuf = ("STGNUM" + (i + 2));

            // gray #
            arms[i][0] = W.CachePatchName(namebuf, PU_STATIC);

            // yellow #
            arms[i][1] = shortnum[i + 2];
        }

        // face backgrounds for different color players
        namebuf = ("STFB" + DS.consoleplayer);
        faceback = W.CachePatchName(namebuf, PU_STATIC);

        // status bar background bits
        sbar = W.CachePatchName("STBAR", PU_STATIC);

        // face states
        facenum = 0;
        for (i = 0; i < ST_NUMPAINFACES; i++) {
            for (j = 0; j < ST_NUMSTRAIGHTFACES; j++) {
                namebuf = ("STFST" + (i) + (j));
                faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            }
            namebuf = "STFTR" + i + "0"; // turn right
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFTL" + i + "0"; // turn left
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFOUCH" + i; // ouch!
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFEVL" + i; // evil grin ;)
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFKILL" + i; // pissed off
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
        }
        faces[facenum++] = W.CachePatchName("STFGOD0", PU_STATIC);
        faces[facenum++] = W.CachePatchName("STFDEAD0", PU_STATIC);

    }

    public void unloadGraphics() {
        /*
         * int i; // unload the numbers, tall and short for (i=0;i<10;i++) {
         * Z_ChangeTag(tallnum[i], PU_CACHE); Z_ChangeTag(shortnum[i],
         * PU_CACHE); } // unload tall percent Z_ChangeTag(tallpercent,
         * PU_CACHE); // unload arms background Z_ChangeTag(armsbg, PU_CACHE);
         * // unload gray #'s for (i=0;i<6;i++) Z_ChangeTag(arms[i][0],
         * PU_CACHE); // unload the key cards for (i=0;i<NUMCARDS;i++)
         * Z_ChangeTag(keys[i], PU_CACHE); Z_ChangeTag(sbar, PU_CACHE);
         * Z_ChangeTag(faceback, PU_CACHE); for (i=0;i<ST_NUMFACES;i++)
         * Z_ChangeTag(faces[i], PU_CACHE);
         */

        // Note: nobody ain't seen no unloading
        // of stminus yet. Dude.

    }

    public void unloadData() {
        unloadGraphics();
    }

    public void initData() {

        int i;

        st_firsttime = true;
        plyr = DS.players[DS.consoleplayer];

        st_clock = 0;
        st_chatstate = st_chatstateenum_t.StartChatState;
        st_gamestate = st_stateenum_t.FirstPersonState;

        st_statusbaron = true;
        st_oldchat = st_chat = false;
        st_cursoron = false;

        st_faceindex[0] = 0;
        st_palette = -1;

        st_oldhealth = -1;

        for (i = 0; i < NUMWEAPONS; i++)
            oldweaponsowned[i] = plyr.weaponowned[i];

        for (i = 0; i < 3; i++)
            keyboxes[i] = -1;

        Init();

    }

    /**
     * Widgets are created here. Be careful, because their "constructors" used
     * reference to boolean or int variables so that they could be auto-updated
     * by the global refresh functions. We can only do this with some
     * limitations in Java (e.g. passing an array AND an index).
     */

    public void createWidgets() {

        int i;

        // ready weapon ammo

        w_ready =
            new st_number_t(ST_AMMOX, ST_AMMOY, tallnum, plyr.ammo,
                    weaponinfo[plyr.readyweapon.ordinal()].ammo.ordinal(),
                    st_statusbaron, ST_AMMOWIDTH);

        // the last weapon type
        w_ready.data = plyr.readyweapon.ordinal();

        // health percentage
        w_health =
            new st_percent_t(ST_HEALTHX, ST_HEALTHY, tallnum, plyr.health,
                    0, st_statusbaron, tallpercent);

        // arms background
        w_armsbg =
            new st_binicon_t(ST_ARMSBGX, ST_ARMSBGY, armsbg, st_notdeathmatch,
                    st_statusbaron);

        // weapons owned
        for (i = 0; i < 6; i++) {
            w_arms[i] =
                new st_multicon_t(ST_ARMSX + (i % 3) * ST_ARMSXSPACE, ST_ARMSY
                        + (i / 3) * ST_ARMSYSPACE, arms[i], plyr.weaponowned,
                        i + 1, st_armson);
        }

        // frags sum
        w_frags =
            new st_number_t(ST_FRAGSX, ST_FRAGSY, tallnum, st_fragscount, 0, // dummy,
                                                                             // we're
                                                                             // passing
                                                                             // an
                                                                             // integer.
                    st_fragson, ST_FRAGSWIDTH);

        // faces
        w_faces =
            new st_multicon_t(ST_FACESX, ST_FACESY, faces, st_faceindex, 0,
                    st_statusbaron);

        // armor percentage - should be colored later
        w_armor =
            new st_percent_t(ST_ARMORX, ST_ARMORY, tallnum, plyr.armorpoints,
                    0, st_statusbaron, tallpercent);

        // keyboxes 0-2
        w_keyboxes[0] =
            new st_multicon_t(ST_KEY0X, ST_KEY0Y, keys, keyboxes, 0,
                    st_statusbaron);

        w_keyboxes[1] =
            new st_multicon_t(ST_KEY1X, ST_KEY1Y, keys, keyboxes, 1,
                    st_statusbaron);

        w_keyboxes[2] =
            new st_multicon_t(ST_KEY2X, ST_KEY2Y, keys, keyboxes, 2,
                    st_statusbaron);

        // ammo count (all four kinds)

        w_ammo[0] =
            new st_number_t(ST_AMMO0X, ST_AMMO0Y, shortnum, plyr.ammo, 0,
                    st_statusbaron, ST_AMMO0WIDTH);

        w_ammo[1] =
            new st_number_t(ST_AMMO1X, ST_AMMO1Y, shortnum, plyr.ammo, 1,
                    st_statusbaron, ST_AMMO1WIDTH);

        w_ammo[2] =
            new st_number_t(ST_AMMO2X, ST_AMMO2Y, shortnum, plyr.ammo, 2,
                    st_statusbaron, ST_AMMO2WIDTH);

        w_ammo[3] =
            new st_number_t(ST_AMMO3X, ST_AMMO3Y, shortnum, plyr.ammo, 3,
                    st_statusbaron, ST_AMMO3WIDTH);

        // max ammo count (all four kinds)
        w_maxammo[0] =
            new st_number_t(ST_MAXAMMO0X, ST_MAXAMMO0Y, shortnum, plyr.maxammo,
                    0, st_statusbaron, ST_MAXAMMO0WIDTH);

        w_maxammo[1] =
            new st_number_t(ST_MAXAMMO1X, ST_MAXAMMO1Y, shortnum, plyr.maxammo,
                    1, st_statusbaron, ST_MAXAMMO1WIDTH);

        w_maxammo[2] =
            new st_number_t(ST_MAXAMMO2X, ST_MAXAMMO2Y, shortnum, plyr.maxammo,
                    2, st_statusbaron, ST_MAXAMMO2WIDTH);

        w_maxammo[3] =
            new st_number_t(ST_MAXAMMO3X, ST_MAXAMMO3Y, shortnum, plyr.maxammo,
                    3, st_statusbaron, ST_MAXAMMO3WIDTH);

    }

    /** Binary Icon widget */

    class st_binicon_t
            implements StatusBarWidget {

        // center-justified location of icon
        int x;

        int y;

        // last icon value
        boolean oldval;

        // pointer to current icon status
        Boolean val;

        // pointer to boolean
        // stating whether to update icon
        Boolean on;

        patch_t p; // icon

        int data; // user data

        // Binary Icon widget routines

        public st_binicon_t(int x, int y, patch_t i, Boolean val, Boolean on) {
            this.x = x;
            this.y = y;
            this.oldval = false;
            this.val = val;
            this.on = on;
            this.p = i;
            this.val=false;
        }

        @Override
        public void update(boolean refresh) {
            st_binicon_t bi = this;
            int x;
            int y;
            int w;
            int h;

            if (bi.on && (bi.oldval != ((bi.val || refresh)))) {
                x = bi.x - bi.p.leftoffset;
                y = bi.y - bi.p.topoffset;
                w = bi.p.width;
                h = bi.p.height;

                if (y - ST_Y < 0)
                    system.Error("updateBinIcon: y - ST_Y < 0");

                if (bi.val)
                    V.DrawPatch(bi.x, bi.y, FG, bi.p);
                else
                    V.CopyRect(x, y - ST_Y, BG, w, h, x, y, FG);

                bi.oldval = bi.val;
            }

        }

    }

    /** Icon widget */

    class st_multicon_t
            implements StatusBarWidget {

        // center-justified location of icons
        int x;

        int y;

        // last icon number
        int oldinum;

        /** pointer to current icon, if not an array type. */
        Object iarray;

        int inum;

        // pointer to boolean stating
        // whether to update icon
        Boolean on;

        // list of icons
        patch_t[] p;

        // user data
        int data;

        /** special status 0=boolean[] 1=integer[] 2=Integer -1= unspecified */
        int status = -1;

        protected boolean[] asboolean;

        protected int[] asint;

        public st_multicon_t(int x, int y, patch_t[] il, Object iarray,
                int inum, Boolean on) {
            this.x = x;
            this.y = y;
            this.oldinum = -1;
            this.inum = inum;
            this.on = on;
            this.p = il;
            if (iarray instanceof boolean[]) {
                status = 0;
                asboolean = (boolean[]) iarray;
            } else 
                if (iarray instanceof int[]){
                    status = 1;
                asint = (int[]) iarray;               
            }  else
            if (iarray instanceof Integer) {
                    status = 2;
                this.iarray = (Integer) iarray;

            }
        }

        @Override
        public void update(boolean refresh) {

            int w;
            int h;
            int x;
            int y;

            // Actual value to be considered. Poor man's generics!
            int thevalue = -1;
            switch (status) {
            case 0:
                thevalue = asboolean[inum] ? 1 : 0;
                break;
            case 1:
                thevalue = asint[inum];
                break;
            case 2:
                thevalue =((Integer)iarray).intValue();
                break;
            }

            // Unified treatment of boolean and integer references
            if (this.on && ((this.oldinum != thevalue) || refresh)
                    && (thevalue != -1)) {
                if (this.oldinum != -1) {
                    x = this.x - this.p[this.oldinum].leftoffset;
                    y = this.y - this.p[this.oldinum].topoffset;
                    w = this.p[this.oldinum].width;
                    h = this.p[this.oldinum].height;

                    if (y - ST_Y < 0)
                        system.Error("updateMultIcon: y - ST_Y < 0");

                    V.CopyRect(x, y - ST_Y, BG, w, h, x, y, FG);
                }
                V.DrawPatch(this.x, this.y, FG, this.p[thevalue]);
                this.oldinum = thevalue;
            }
        }
    }

    protected patch_t sttminus;

    /** Number widget */

    class st_number_t
            implements StatusBarWidget {

        /** upper right-hand corner of the number (right-justified) */
        int x, y;

        /** max # of digits in number */
        int width;

        /** last number value */
        int oldnum;

        /**
         * Array in which to point with num. Alternatively, Integer object that
         * carries over
         */
        Object numarray;

        /** pointer to current value. Of course makes sense only for arrays. */
        int numindex;

        /** pointer to boolean stating whether to update number */
        Boolean on;

        /** list of patches for 0-9 */
        patch_t[] p;

        /** user data */
        int data;

        protected int status = -1;

        // Number widget routines

        public st_number_t(int x, int y, patch_t[] pl, Object numarray,
                int numindex, Boolean on, int width) {
            if (numarray instanceof int[]) {
                // int array passed.
                status = 0;
                init(x, y, pl, (int[]) numarray, numindex, on, width);
            } else if (numarray instanceof Integer) {
                // Reference is to Integer.
                status = 1;
                init(x, y, pl, (Integer) numarray, on, width);
            }
        }

        public void init(int x, int y, patch_t[] pl, Integer numarray,
                Boolean on, int width) {
            this.x = x;
            this.y = y;
            this.oldnum = 0;
            this.width = width;
            this.numarray = numarray;
            this.on = on;
            this.p = pl;
        }

        public void init(int x, int y, patch_t[] pl, int[] numarray,
                int numindex, Boolean on, int width) {
            this.x = x;
            this.y = y;
            this.oldnum = 0;
            this.width = width;
            this.numarray = numarray;
            this.numindex = numindex;
            this.on = on;
            this.p = pl;
        }

        // 
        // A fairly efficient way to draw a number
        // based on differences from the old number.
        // Note: worth the trouble?
        //
        public void drawNum(boolean refresh) {

            st_number_t n = this;
            int numdigits = this.width;
            int num = 0;

            switch (status) {
            case 0:
                num = ((int[]) this.numarray)[this.numindex];
                break;
            case 1:
                num = ((Integer) this.numarray).intValue();
                break;
            }

            int w = this.p[0].width;
            int h = this.p[0].height;
            int x = this.x;

            boolean neg;

            // In this way, num and oldnum are exactly the same. Maybe this
            // should go in the end?
            this.oldnum = num;

            neg = num < 0;

            if (neg) {
                if (numdigits == 2 && num < -9)
                    num = -9;
                else if (numdigits == 3 && num < -99)
                    num = -99;

                num = -num;
            }

            // clear the area
            x = this.x - numdigits * w;

            if (this.y - ST_Y < 0) {
                system.Error("drawNum: n.y - ST_Y < 0");
            }

            V.CopyRect(x, this.y - ST_Y, BG, w * numdigits, h, x, n.y, FG);

            // if non-number, do not draw it
            if (num == 1994)
                return;

            x = n.x;

            // in the special case of 0, you draw 0
            if (num == 0)
                V.DrawPatch(x - w, n.y, FG, n.p[0]);

            // draw the new number
            while (((num != 0) && (numdigits-- != 0))) {
                x -= w;
                V.DrawPatch(x, n.y, FG, n.p[num % 10]);
                num /= 10;
            }

            // draw a minus sign if necessary
            if (neg)
                V.DrawPatch(x - 8, n.y, FG, sttminus);
        }

        @Override
        public void update(boolean refresh) {
            if (this.on)
                drawNum(refresh);
        }

    }

    class st_percent_t
            implements StatusBarWidget {

        // Percent widget ("child" of number widget,
        // or, more precisely, contains a number widget.)
        // number information
        st_number_t n;

        // percent sign graphic
        patch_t p;

        public st_percent_t(int x, int y, patch_t[] pl, Object numarray,
                int numindex, Boolean on, patch_t percent) {
            n = new st_number_t(x, y, pl, numarray, numindex, on, 3);
            p = percent;
        }

        @Override
        public void update(boolean refresh) {
            if (refresh && this.n.on)
                V.DrawPatch(n.x, n.y, FG, p);

            n.update(refresh);
        }

    }

    interface StatusBarWidget {
        public void update(boolean refresh);
    }

}
