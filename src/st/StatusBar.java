package st;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: StatusBar.java,v 1.2 2010/07/20 15:52:56 velktron Exp $
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
// $Log: StatusBar.java,v $
// Revision 1.2  2010/07/20 15:52:56  velktron
// LOTS of changes, Automap almost complete. Use of fixed_t inside methods severely limited.
//
// Revision 1.1  2010/07/03 23:24:13  velktron
// Added a LOT of stuff, like Status bar code & objects. Now we're cooking with gas!
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
// DESCRIPTION:
//	Status bar code.
//	Does the face/direction indicator animatin.
//	Does palette indicators as well (red pain/berserk, bright pickup)
//
//-----------------------------------------------------------------------------

import static data.Defines.*;
import static data.dstrings.*;
import static doom.englsh.*;
import static st.DoomStatusBar.*;
import static st.DoomStatusBar.BG;
import static st.DoomStatusBar.FG;
import m.cheatseq_t;
import data.doomstat;
import doom.player_t;
import rr.patch_t;
import v.DoomVideoRenderer;
import v.SimpleRenderer;
import w.WadLoader;
public class StatusBar{
public static final String rcsid = "$Id: StatusBar.java,v 1.2 2010/07/20 15:52:56 velktron Exp $";

/*
#include <stdio.h>

#include "i_system.h"
#include "i_video.h"
#include "z_zone.h"
#include "m_random.h"
#include "w_wad.h"

#include "doomdef.h"

#include "g_game.h"

#include "st_stuff.h"
#include "st_lib.h"
#include "r_local.h"

#include "p_local.h"
#include "p_inter.h"

#include "am_map.h"
#include "m_cheat.h"

#include "s_sound.h"

// Needs access to LFB.
#include "v_video.h"

// State.
#include "doomstat.h"

// Data.
#include "dstrings.h"
#include "sounds.h"
*/

protected DoomVideoRenderer V;
protected doomstat ds;
protected WadLoader W;
//
// STATUS BAR DATA
//


// Palette indices.
// For damage/bonus red-/gold-shifts
private static int  STARTREDPALS	=	1;
private static int STARTBONUSPALS	=	9;
private static int NUMREDPALS		=	8;
private static int NUMBONUSPALS	=	4;
// Radiation suit, green shift.
private static int RADIATIONPAL		=13;

// N/256*100% probability
//  that the normal face state will change
private static int ST_FACEPROBABILITY	=	96;

// For Responder
private static int ST_TOGGLECHAT	=	KEY_ENTER;

// Location of status bar
private static int ST_X				= 0;
private static int ST_X2			=	104;

private static int ST_FX  		=	143;
private static int ST_FY  		=	169;

// Should be set to patch width
//  for tall numbers later on
//TODO: private static int ST_TALLNUMWIDTH	=	(tallnum[0].width);

// Number of status faces.
private static int ST_NUMPAINFACES	=	5;
private static int ST_NUMSTRAIGHTFACES	=3;
private static int ST_NUMTURNFACES		=2;
private static int ST_NUMSPECIALFACES	=	3;

private static int ST_FACESTRIDE =
          (ST_NUMSTRAIGHTFACES+ST_NUMTURNFACES+ST_NUMSPECIALFACES);

private static int ST_NUMEXTRAFACES	=	2;

private static int ST_NUMFACES =
          (ST_FACESTRIDE*ST_NUMPAINFACES+ST_NUMEXTRAFACES);


private static int ST_TURNOFFSET		=(ST_NUMSTRAIGHTFACES);
private static int ST_OUCHOFFSET		=(ST_TURNOFFSET + ST_NUMTURNFACES);
private static int ST_EVILGRINOFFSET=		(ST_OUCHOFFSET + 1);
private static int ST_RAMPAGEOFFSET	=	(ST_EVILGRINOFFSET + 1);
private static int ST_GODFACE		=	(ST_NUMPAINFACES*ST_FACESTRIDE);
private static int ST_DEADFACE		=	(ST_GODFACE+1);

private static int ST_FACESX		=	143;
private static int ST_FACESY		=	168;

private static int ST_EVILGRINCOUNT	=	(2*TICRATE);
private static int ST_STRAIGHTFACECOUNT	=(TICRATE/2);
private static int ST_TURNCOUNT		=(1*TICRATE);
private static int ST_OUCHCOUNT		=(1*TICRATE);
private static int ST_RAMPAGEDELAY	=	(2*TICRATE);

private static int ST_MUCHPAIN		=	20;


// Location and size of statistics,
//  justified according to widget type.
// Problem is, within which space? STbar? Screen?
// Note: this could be read in by a lump.
//       Problem is, is the stuff rendered
//       into a buffer,
//       or into the frame buffer?

// AMMO number pos.
private static int ST_AMMOWIDTH	=	3	;
private static int ST_AMMOX		=	44;
private static int ST_AMMOY		=	171;

// HEALTH number pos.
private static int ST_HEALTHWIDTH	=	3	;
private static int ST_HEALTHX		=	90;
private static int ST_HEALTHY		=	171;

// Weapon pos.
private static int ST_ARMSX		=	111;
private static int ST_ARMSY		=	172;
private static int ST_ARMSBGX	=		104;
private static int ST_ARMSBGY	=		168;
private static int ST_ARMSXSPACE=		12;
private static int ST_ARMSYSPACE=		10;

// Frags pos.
private static int ST_FRAGSX	=		138;
private static int ST_FRAGSY	=		171	;
private static int ST_FRAGSWIDTH=		2;

// ARMOR number pos.
private static int ST_ARMORWIDTH=		3;
private static int ST_ARMORX	=		221;
private static int ST_ARMORY	=		171;

// Key icon positions.
private static int ST_KEY0WIDTH	=	8;
private static int ST_KEY0HEIGHT=		5;
private static int ST_KEY0X		=	239;
private static int ST_KEY0Y		=	171;
private static int ST_KEY1WIDTH	=	ST_KEY0WIDTH;
private static int ST_KEY1X		=	239;
private static int ST_KEY1Y		=	181;
private static int ST_KEY2WIDTH	=	ST_KEY0WIDTH;
private static int ST_KEY2X		=	239;
private static int ST_KEY2Y		=	191;

// Ammunition counter.
private static int ST_AMMO0WIDTH	=	3;
private static int ST_AMMO0HEIGHT	=	6;
private static int ST_AMMO0X		=	288;
private static int ST_AMMO0Y		=	173;
private static int ST_AMMO1WIDTH	=	ST_AMMO0WIDTH;
private static int ST_AMMO1X		=	288;
private static int ST_AMMO1Y		=	179;
private static int ST_AMMO2WIDTH	=	ST_AMMO0WIDTH;
private static int ST_AMMO2X		=	288;
private static int ST_AMMO2Y		=	191;
private static int ST_AMMO3WIDTH	=	ST_AMMO0WIDTH;
private static int ST_AMMO3X		=	288;
private static int ST_AMMO3Y		=	185;

// Indicate maximum ammunition.
// Only needed because backpack exists.
private static int ST_MAXAMMO0WIDTH	=	3;
private static int ST_MAXAMMO0HEIGHT=		5;
private static int ST_MAXAMMO0X	=	314;
private static int ST_MAXAMMO0Y	=	173;
private static int ST_MAXAMMO1WIDTH	=	ST_MAXAMMO0WIDTH;
private static int ST_MAXAMMO1X	=	314;
private static int ST_MAXAMMO1Y	=	179;
private static int ST_MAXAMMO2WIDTH	=	ST_MAXAMMO0WIDTH;
private static int ST_MAXAMMO2X	=	314;
private static int ST_MAXAMMO2Y	=	191;
private static int ST_MAXAMMO3WIDTH	=	ST_MAXAMMO0WIDTH;
private static int ST_MAXAMMO3X=		314;
private static int ST_MAXAMMO3Y	=	185;

// pistol
private static int ST_WEAPON0X			=110 ;
private static int ST_WEAPON0Y			=172;

// shotgun
private static int ST_WEAPON1X			=122 ;
private static int ST_WEAPON1Y			=172;

// chain gun
private static int ST_WEAPON2X			=134 ;
private static int ST_WEAPON2Y			=172;

// missile launcher
private static int ST_WEAPON3X			=110 ;
private static int ST_WEAPON3Y			=181;

// plasma gun
private static int ST_WEAPON4X		=	122 ;
private static int ST_WEAPON4Y		=	181;

 // bfg
private static int ST_WEAPON5X		=	134;
private static int ST_WEAPON5Y	=		181;

// WPNS title
private static int ST_WPNSX	=		109 ;
private static int ST_WPNSY	=		191;

 // DETH title
private static int ST_DETHX	=		109;
private static int ST_DETHY	=		191;

//Incoming messages window location
//UNUSED
// #define ST_MSGTEXTX	   (viewwindowx)
// #define ST_MSGTEXTY	   (viewwindowy+viewheight-18)
private static int ST_MSGTEXTX		=	0;
private static int ST_MSGTEXTY		=	0;
// Dimensions given in characters.
private static int ST_MSGWIDTH		=	52;
// Or shall I say, in lines?
private static int ST_MSGHEIGHT	=	1;

private static int ST_OUTTEXTX		=	0;
private static int ST_OUTTEXTY		=	6;

// Width, in characters again.
private static int ST_OUTWIDTH		=	52;
 // Height, in lines. 
private static int ST_OUTHEIGHT	=	1;

// TODO private static int ST_MAPWIDTH	= (mapnames[(gameepisode-1)*9+(gamemap-1)].length));

// TODO private static int ST_MAPTITLEX  = (SCREENWIDTH - ST_MAPWIDTH * ST_CHATFONTWIDTH);

private static int ST_MAPTITLEY		=0;
private static int ST_MAPHEIGHT		=1;

// MAES: most of this shit was "static".
// main player in game
private player_t	plyr; 

// ST_Start() has just been called
private boolean		st_firsttime;

// used to execute ST_Init() only once
private int		veryfirsttime = 1;

// lump number for PLAYPAL
private int		lu_palette;

// used for timing (unsigned int .. maybe long !)
private long	st_clock;

// used for making messages go away
 int		st_msgcounter=0;

// used when in chat 
 private st_chatstateenum_t	st_chatstate;

// whether in automap or first-person
 private st_stateenum_t	st_gamestate;

// whether left-side main status bar is active
 private boolean		st_statusbaron;

// whether status bar chat is active
 private boolean		st_chat;

// value of st_chat before message popped up
 private boolean		st_oldchat;

// whether chat window has the cursor on
 private boolean		st_cursoron;

// !deathmatch
 private boolean		st_notdeathmatch; 

// !deathmatch && st_statusbaron
 private boolean		st_armson;

// !deathmatch
 private boolean		st_fragson; 

// main bar left
 private patch_t		sbar;

// 0-9, tall numbers
 private patch_t[]		tallnum=new patch_t[10];

// tall % sign
 private patch_t		tallpercent;

// 0-9, short, yellow (,different!) numbers
 private patch_t[]		shortnum= new patch_t[10];

// 3 key-cards, 3 skulls
 private patch_t[]		keys= new patch_t[NUMCARDS]; 

// face status patches
 private patch_t[]		faces= new patch_t[ST_NUMFACES];

// face background
 private patch_t		faceback;

 // main bar right
 private patch_t		armsbg;

// weapon ownership patches
 private patch_t[][]		arms= new patch_t[6][2]; 

// ready-weapon widget
 private st_number_t	w_ready;

 // in deathmatch only, summary of frags stats
 private st_number_t	w_frags;

// health widget
 private st_percent_t	w_health;

// arms background
 private st_binicon_t	w_armsbg; 


// weapon ownership widgets
 private st_multicon_t[]	w_arms=new st_multicon_t[6];

// face status widget
 private st_multicon_t	w_faces; 

// keycard widgets
private st_multicon_t[]	w_keyboxes=new st_multicon_t[3];

// armor widget
private st_percent_t	w_armor;

// ammo widgets
private st_number_t[]	w_ammo=new st_number_t[4];

// max ammo widgets
private st_number_t[]	w_maxammo=new st_number_t[4]; 



 // number of frags so far in deathmatch
private int	st_fragscount;

// used to use appopriately pained face
private int	st_oldhealth = -1;

// used for evil grin
private boolean[]	oldweaponsowned=new boolean[NUMWEAPONS]; 

 // count until face changes
private int	st_facecount = 0;

// current face index, used by w_faces
private int	st_faceindex = 0;

// holds key-type for each key box on bar
private int[] 	keyboxes= new int[3]; 

// a random number per tick
private int	st_randomnumber;  



// Massive bunches of cheat shit
//  to keep it from being easy to figure them out.
// Yeah, right...
private char	cheat_mus_seq[] =
{
    0xb2, 0x26, 0xb6, 0xae, 0xea, 1, 0, 0, 0xff
};

private char	cheat_choppers_seq[] =
{
    0xb2, 0x26, 0xe2, 0x32, 0xf6, 0x2a, 0x2a, 0xa6, 0x6a, 0xea, 0xff // id...
};

private char	cheat_god_seq[] =
{
    0xb2, 0x26, 0x26, 0xaa, 0x26, 0xff  // iddqd
};

private char	cheat_ammo_seq[] =
{
    0xb2, 0x26, 0xf2, 0x66, 0xa2, 0xff	// idkfa
};

private char	cheat_ammonokey_seq[] =
{
    0xb2, 0x26, 0x66, 0xa2, 0xff	// idfa
};


// Smashing Pumpkins Into Samml Piles Of Putried Debris. 
private char	cheat_noclip_seq[] =
{
    0xb2, 0x26, 0xea, 0x2a, 0xb2,	// idspispopd
    0xea, 0x2a, 0xf6, 0x2a, 0x26, 0xff
};

//
private char	cheat_commercial_noclip_seq[] =
{
    0xb2, 0x26, 0xe2, 0x36, 0xb2, 0x2a, 0xff	// idclip
}; 



private char	cheat_powerup_seq[][] =
{
    { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x6e, 0xff }, 	// beholdv
    { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xea, 0xff }, 	// beholds
    { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xb2, 0xff }, 	// beholdi
    { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x6a, 0xff }, 	// beholdr
    { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xa2, 0xff }, 	// beholda
    { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x36, 0xff }, 	// beholdl
    { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xff }		// behold
};


private char	cheat_clev_seq[] =
{
    0xb2, 0x26,  0xe2, 0x36, 0xa6, 0x6e, 1, 0, 0, 0xff	// idclev
};


// my position cheat
private char	cheat_mypos_seq[] =
{
    0xb2, 0x26, 0xb6, 0xba, 0x2a, 0xf6, 0xea, 0xff	// idmypos
}; 


// Now what?
cheatseq_t	cheat_mus = new cheatseq_t(cheat_mus_seq, 0 );
cheatseq_t	cheat_god = new cheatseq_t( cheat_god_seq, 0 );
cheatseq_t	cheat_ammo = new cheatseq_t( cheat_ammo_seq, 0 );
cheatseq_t	cheat_ammonokey = new cheatseq_t( cheat_ammonokey_seq, 0 );
cheatseq_t	cheat_noclip = new cheatseq_t( cheat_noclip_seq, 0 );
cheatseq_t	cheat_commercial_noclip = new cheatseq_t( cheat_commercial_noclip_seq, 0 );

cheatseq_t[]	cheat_powerup =
{
		new cheatseq_t( cheat_powerup_seq[0], 0 ),
		new cheatseq_t( cheat_powerup_seq[1], 0 ),
		new cheatseq_t( cheat_powerup_seq[2], 0 ),
		new cheatseq_t( cheat_powerup_seq[3], 0 ),
		new cheatseq_t( cheat_powerup_seq[4], 0 ),
		new cheatseq_t( cheat_powerup_seq[5], 0 ),
		new cheatseq_t( cheat_powerup_seq[6], 0 )
};

cheatseq_t	cheat_choppers = new cheatseq_t( cheat_choppers_seq, 0 );
cheatseq_t	cheat_clev = new cheatseq_t( cheat_clev_seq, 0 );
cheatseq_t	cheat_mypos = new cheatseq_t( cheat_mypos_seq, 0 );


// 
String[]	mapnames;


//
// STATUS BAR CODE
//

public void refreshBackground()
{

    if (st_statusbaron)
    {
	V.DrawPatch(ST_X, 0, BG, sbar);

	if (ds.netgame)
	    V.DrawPatch(ST_FX, 0, BG, faceback);

	V.CopyRect(ST_X, 0, BG, ST_WIDTH, ST_HEIGHT, ST_X, ST_Y, FG);
    }

}
void Init ()
{
    veryfirsttime = 0;
    loadData();
    // MAES: this actually Z_Mallocated 4 screens just for the status bar,
    // so I presume they meant it to have its own screen buffers (?) 
    this.V=new SimpleRenderer(ST_WIDTH,ST_HEIGHT);
}

protected boolean	st_stopped = true;


void Start ()
{

    if (!st_stopped)
	Stop();

    initData();
    createWidgets();
    st_stopped = false;

}

public void Stop ()
{
	if (st_stopped)
	return;

    // TODO: I_SetPalette (W.CacheLumpNum (lu_palette, PU_CACHE));

    st_stopped = true;
}

public void loadData()
{
    lu_palette = W.GetNumForName ("PLAYPAL");
    loadGraphics();
}


}


// Respond to keyboard input events,
//  intercept cheats.
public boolean
Responder (event_t ev)
{
  int		i;
    
  // Filter automap on/off.
  if (ev.type == ev_keyup
      && ((ev.data1 & 0xffff0000) == AM_MSGHEADER))
  {
    switch(ev.data1)
    {
      case AM_MSGENTERED:
	st_gamestate = AutomapState;
	st_firsttime = true;
	break;
	
      case AM_MSGEXITED:
	//	fprintf(stderr, "AM exited\n");
	st_gamestate = FirstPersonState;
	break;
    }
  }

  // if a user keypress...
  else if (ev.type == evtype_t.ev_keydown)
  {
    if (!netgame)
    {
      // b. - enabled for more debug fun.
      // if (gameskill != sk_nightmare) {
      
      // 'dqd' cheat for toggleable god mode
      if (cht_CheckCheat(&cheat_god, ev->data1))
      {
	plyr.cheats ^= CF_GODMODE;
	if (plyr.cheats & CF_GODMODE !=0)
	{
	  if (plyr.mo!=null)
	    plyr.mo.health = 100;
	  
	  plyr.health = 100;
	  plyr.message = STSTR_DQDON;
	}
	else 
	  plyr.message = STSTR_DQDOFF;
      }
      // 'fa' cheat for killer fucking arsenal
      else if (cht_CheckCheat(cheat_ammonokey, ev.data1))
      {
	plyr.armorpoints = 200;
	plyr.armortype = 2;
	
	for (i=0;i<NUMWEAPONS;i++)
	  plyr.weaponowned[i] = true;
	
	for (i=0;i<NUMAMMO;i++)
	  plyr.ammo[i] = plyr.maxammo[i];
	
	plyr.message = STSTR_FAADDED;
      }
      // 'kfa' cheat for key full ammo
      else if (cht_CheckCheat(cheat_ammo, ev.data1))
      {
	plyr.armorpoints = 200;
	plyr.armortype = 2;
	
	for (i=0;i<NUMWEAPONS;i++)
	  plyr.weaponowned[i] = true;
	
	for (i=0;i<NUMAMMO;i++)
	  plyr.ammo[i] = plyr.maxammo[i];
	
	for (i=0;i<NUMCARDS;i++)
	  plyr.cards[i] = true;
	
	plyr.message = STSTR_KFAADDED;
      }
      // 'mus' cheat for changing music
      else if (cht_CheckCheat(&cheat_mus, ev->data1))
      {
	
	char[]	buf=new char[3];
	int		musnum;
	
	plyr.message = STSTR_MUS;
	cheat_mus.cht_GetParam(nuf);
	
	
	if (gamemode == commercial)
	{
	  musnum = mus_runnin + (buf[0]-'0')*10 + buf[1]-'0' - 1;
	  
	  if (((buf[0]-'0')*10 + buf[1]-'0') > 35)
	    plyr->message = STSTR_NOMUS;
	  else
	    S_ChangeMusic(musnum, 1);
	}
	else
	{
	  musnum = mus_e1m1 + (buf[0]-'1')*9 + (buf[1]-'1');
	  
	  if (((buf[0]-'1')*9 + buf[1]-'1') > 31)
	    plyr->message = STSTR_NOMUS;
	  else
	    S_ChangeMusic(musnum, 1);
	}
      }
      // Simplified, accepting both "noclip" and "idspispopd".
      // no clipping mode cheat
      else if ( cht_CheckCheat(&cheat_noclip, ev->data1) 
		|| cht_CheckCheat(&cheat_commercial_noclip,ev->data1) )
      {	
	plyr->cheats ^= CF_NOCLIP;
	
	if (plyr->cheats & CF_NOCLIP)
	  plyr->message = STSTR_NCON;
	else
	  plyr->message = STSTR_NCOFF;
      }
      // 'behold?' power-up cheats
      for (i=0;i<6;i++)
      {
	if (cht_CheckCheat(&cheat_powerup[i], ev->data1))
	{
	  if (!plyr->powers[i])
	    P_GivePower( plyr, i);
	  else if (i!=pw_strength)
	    plyr->powers[i] = 1;
	  else
	    plyr->powers[i] = 0;
	  
	  plyr->message = STSTR_BEHOLDX;
	}
      }
      
      // 'behold' power-up menu
      if (cht_CheckCheat(&cheat_powerup[6], ev->data1))
      {
	plyr->message = STSTR_BEHOLD;
      }
      // 'choppers' invulnerability & chainsaw
      else if (cht_CheckCheat(&cheat_choppers, ev->data1))
      {
	plyr->weaponowned[wp_chainsaw] = true;
	plyr->powers[pw_invulnerability] = true;
	plyr->message = STSTR_CHOPPERS;
      }
      // 'mypos' for player position
      else if (cht_CheckCheat(&cheat_mypos, ev->data1))
      {
	static char	buf[ST_MSGWIDTH];
	sprintf(buf, "ang=0x%x;x,y=(0x%x,0x%x)",
		players[consoleplayer].mo->angle,
		players[consoleplayer].mo->x,
		players[consoleplayer].mo->y);
	plyr->message = buf;
      }
    }
    
    // 'clev' change-level cheat
    if (cht_CheckCheat(&cheat_clev, ev->data1))
    {
      char		buf[3];
      int		epsd;
      int		map;
      
      cht_GetParam(&cheat_clev, buf);
      
      if (gamemode == commercial)
      {
	epsd = 0;
	map = (buf[0] - '0')*10 + buf[1] - '0';
      }
      else
      {
	epsd = buf[0] - '0';
	map = buf[1] - '0';
      }

      // Catch invalid maps.
      if (epsd < 1)
	return false;

      if (map < 1)
	return false;
      
      // Ohmygod - this is not going to work.
      if ((gamemode == retail)
	  && ((epsd > 4) || (map > 9)))
	return false;

      if ((gamemode == registered)
	  && ((epsd > 3) || (map > 9)))
	return false;

      if ((gamemode == shareware)
	  && ((epsd > 1) || (map > 9)))
	return false;

      if ((gamemode == commercial)
	&& (( epsd > 1) || (map > 34)))
	return false;

      // So be it.
      plyr->message = STSTR_CLEV;
      G_DeferedInitNew(gameskill, epsd, map);
    }    
  }
  return false;
}

public int calcPainOffset()
{
    int		health;
    static int	lastcalc;
    static int	oldhealth = -1;
    
    health = plyr.health > 100 ? 100 : plyr.health;

    if (health != oldhealth)
    {
	lastcalc = ST_FACESTRIDE * (((100 - health) * ST_NUMPAINFACES) / 101);
	oldhealth = health;
    }
    return lastcalc;
}


//
// This is a not-very-pretty routine which handles
//  the face states and their timing.
// the precedence of expressions is:
//  dead > evil grin > turned head > straight ahead
//
void updateFaceWidget()
{
    int		i;
    angle_t	badguyangle;
    angle_t	diffang;
    static int	lastattackdown = -1;
    static int	priority = 0;
    boolean	doevilgrin;

    if (priority < 10)
    {
	// dead
	if (!plyr->health)
	{
	    priority = 9;
	    st_faceindex = ST_DEADFACE;
	    st_facecount = 1;
	}
    }

    if (priority < 9)
    {
	if (plyr->bonuscount)
	{
	    // picking up bonus
	    doevilgrin = false;

	    for (i=0;i<NUMWEAPONS;i++)
	    {
		if (oldweaponsowned[i] != plyr->weaponowned[i])
		{
		    doevilgrin = true;
		    oldweaponsowned[i] = plyr->weaponowned[i];
		}
	    }
	    if (doevilgrin) 
	    {
		// evil grin if just picked up weapon
		priority = 8;
		st_facecount = ST_EVILGRINCOUNT;
		st_faceindex = ST_calcPainOffset() + ST_EVILGRINOFFSET;
	    }
	}

    }
  
    if (priority < 8)
    {
	if (plyr->damagecount
	    && plyr->attacker
	    && plyr->attacker != plyr->mo)
	{
	    // being attacked
	    priority = 7;
	    
	    if (plyr->health - st_oldhealth > ST_MUCHPAIN)
	    {
		st_facecount = ST_TURNCOUNT;
		st_faceindex = ST_calcPainOffset() + ST_OUCHOFFSET;
	    }
	    else
	    {
		badguyangle = R_PointToAngle2(plyr->mo->x,
					      plyr->mo->y,
					      plyr->attacker->x,
					      plyr->attacker->y);
		
		if (badguyangle > plyr->mo->angle)
		{
		    // whether right or left
		    diffang = badguyangle - plyr->mo->angle;
		    i = diffang > ANG180; 
		}
		else
		{
		    // whether left or right
		    diffang = plyr->mo->angle - badguyangle;
		    i = diffang <= ANG180; 
		} // confusing, aint it?

		
		st_facecount = ST_TURNCOUNT;
		st_faceindex = ST_calcPainOffset();
		
		if (diffang < ANG45)
		{
		    // head-on    
		    st_faceindex += ST_RAMPAGEOFFSET;
		}
		else if (i)
		{
		    // turn face right
		    st_faceindex += ST_TURNOFFSET;
		}
		else
		{
		    // turn face left
		    st_faceindex += ST_TURNOFFSET+1;
		}
	    }
	}
    }
  
    if (priority < 7)
    {
	// getting hurt because of your own damn stupidity
	if (plyr->damagecount)
	{
	    if (plyr->health - st_oldhealth > ST_MUCHPAIN)
	    {
		priority = 7;
		st_facecount = ST_TURNCOUNT;
		st_faceindex = ST_calcPainOffset() + ST_OUCHOFFSET;
	    }
	    else
	    {
		priority = 6;
		st_facecount = ST_TURNCOUNT;
		st_faceindex = ST_calcPainOffset() + ST_RAMPAGEOFFSET;
	    }

	}

    }
  
    if (priority < 6)
    {
	// rapid firing
	if (plyr->attackdown)
	{
	    if (lastattackdown==-1)
		lastattackdown = ST_RAMPAGEDELAY;
	    else if (!--lastattackdown)
	    {
		priority = 5;
		st_faceindex = ST_calcPainOffset() + ST_RAMPAGEOFFSET;
		st_facecount = 1;
		lastattackdown = 1;
	    }
	}
	else
	    lastattackdown = -1;

    }
  
    if (priority < 5)
    {
	// invulnerability
	if ((plyr->cheats & CF_GODMODE)
	    || plyr->powers[pw_invulnerability])
	{
	    priority = 4;

	    st_faceindex = ST_GODFACE;
	    st_facecount = 1;

	}

    }

    // look left or look right if the facecount has timed out
    if (!st_facecount)
    {
	st_faceindex = ST_calcPainOffset() + (st_randomnumber % 3);
	st_facecount = ST_STRAIGHTFACECOUNT;
	priority = 0;
    }

    st_facecount--;

}

void ST_updateWidgets(void)
{
    static int	largeammo = 1994; // means "n/a"
    int		i;

    // must redirect the pointer if the ready weapon has changed.
    //  if (w_ready.data != plyr->readyweapon)
    //  {
    if (weaponinfo[plyr->readyweapon].ammo == am_noammo)
	w_ready.num = &largeammo;
    else
	w_ready.num = &plyr->ammo[weaponinfo[plyr->readyweapon].ammo];
    //{
    // static int tic=0;
    // static int dir=-1;
    // if (!(tic&15))
    //   plyr->ammo[weaponinfo[plyr->readyweapon].ammo]+=dir;
    // if (plyr->ammo[weaponinfo[plyr->readyweapon].ammo] == -100)
    //   dir = 1;
    // tic++;
    // }
    w_ready.data = plyr->readyweapon;

    // if (*w_ready.on)
    //  STlib_updateNum(&w_ready, true);
    // refresh weapon change
    //  }

    // update keycard multiple widgets
    for (i=0;i<3;i++)
    {
	keyboxes[i] = plyr->cards[i] ? i : -1;

	if (plyr->cards[i+3])
	    keyboxes[i] = i+3;
    }

    // refresh everything if this is him coming back to life
    ST_updateFaceWidget();

    // used by the w_armsbg widget
    st_notdeathmatch = !deathmatch;
    
    // used by w_arms[] widgets
    st_armson = st_statusbaron && !deathmatch; 

    // used by w_frags widget
    st_fragson = deathmatch && st_statusbaron; 
    st_fragscount = 0;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (i != consoleplayer)
	    st_fragscount += plyr->frags[i];
	else
	    st_fragscount -= plyr->frags[i];
    }

    // get rid of chat window if up because of message
    if (!--st_msgcounter)
	st_chat = st_oldchat;

}

void ST_Ticker (void)
{

    st_clock++;
    st_randomnumber = M_Random();
    ST_updateWidgets();
    st_oldhealth = plyr->health;

}

static int st_palette = 0;

void ST_doPaletteStuff(void)
{

    int		palette;
    byte*	pal;
    int		cnt;
    int		bzc;

    cnt = plyr->damagecount;

    if (plyr->powers[pw_strength])
    {
	// slowly fade the berzerk out
  	bzc = 12 - (plyr->powers[pw_strength]>>6);

	if (bzc > cnt)
	    cnt = bzc;
    }
	
    if (cnt)
    {
	palette = (cnt+7)>>3;
	
	if (palette >= NUMREDPALS)
	    palette = NUMREDPALS-1;

	palette += STARTREDPALS;
    }

    else if (plyr->bonuscount)
    {
	palette = (plyr->bonuscount+7)>>3;

	if (palette >= NUMBONUSPALS)
	    palette = NUMBONUSPALS-1;

	palette += STARTBONUSPALS;
    }

    else if ( plyr->powers[pw_ironfeet] > 4*32
	      || plyr->powers[pw_ironfeet]&8)
	palette = RADIATIONPAL;
    else
	palette = 0;

    if (palette != st_palette)
    {
	st_palette = palette;
	pal = (byte *) W_CacheLumpNum (lu_palette, PU_CACHE)+palette*768;
	I_SetPalette (pal);
    }

}

void ST_drawWidgets(boolean refresh)
{
    int		i;

    // used by w_arms[] widgets
    st_armson = st_statusbaron && !deathmatch;

    // used by w_frags widget
    st_fragson = deathmatch && st_statusbaron; 

    STlib_updateNum(&w_ready, refresh);

    for (i=0;i<4;i++)
    {
	STlib_updateNum(&w_ammo[i], refresh);
	STlib_updateNum(&w_maxammo[i], refresh);
    }

    STlib_updatePercent(&w_health, refresh);
    STlib_updatePercent(&w_armor, refresh);

    STlib_updateBinIcon(&w_armsbg, refresh);

    for (i=0;i<6;i++)
	STlib_updateMultIcon(&w_arms[i], refresh);

    STlib_updateMultIcon(&w_faces, refresh);

    for (i=0;i<3;i++)
	STlib_updateMultIcon(&w_keyboxes[i], refresh);

    STlib_updateNum(&w_frags, refresh);

}

void ST_doRefresh(void)
{

    st_firsttime = false;

    // draw status bar background to off-screen buff
    ST_refreshBackground();

    // and refresh all widgets
    ST_drawWidgets(true);

}

void ST_diffDraw(void)
{
    // update all widgets
    ST_drawWidgets(false);
}

void ST_Drawer (boolean fullscreen, boolean refresh)
{
  
    st_statusbaron = (!fullscreen) || automapactive;
    st_firsttime = st_firsttime || refresh;

    // Do red-/gold-shifts from damage/items
    ST_doPaletteStuff();

    // If just after ST_Start(), refresh all
    if (st_firsttime) ST_doRefresh();
    // Otherwise, update as little as possible
    else ST_diffDraw();

}

void ST_loadGraphics(void)
{

    int		i;
    int		j;
    int		facenum;
    
    char	namebuf[9];

    // Load the numbers, tall and short
    for (i=0;i<10;i++)
    {
	sprintf(namebuf, "STTNUM%d", i);
	tallnum[i] = (patch_t *) W_CacheLumpName(namebuf, PU_STATIC);

	sprintf(namebuf, "STYSNUM%d", i);
	shortnum[i] = (patch_t *) W_CacheLumpName(namebuf, PU_STATIC);
    }

    // Load percent key.
    //Note: why not load STMINUS here, too?
    tallpercent = (patch_t *) W_CacheLumpName("STTPRCNT", PU_STATIC);

    // key cards
    for (i=0;i<NUMCARDS;i++)
    {
	sprintf(namebuf, "STKEYS%d", i);
	keys[i] = (patch_t *) W_CacheLumpName(namebuf, PU_STATIC);
    }

    // arms background
    armsbg = (patch_t *) W_CacheLumpName("STARMS", PU_STATIC);

    // arms ownership widgets
    for (i=0;i<6;i++)
    {
	sprintf(namebuf, "STGNUM%d", i+2);

	// gray #
	arms[i][0] = (patch_t *) W_CacheLumpName(namebuf, PU_STATIC);

	// yellow #
	arms[i][1] = shortnum[i+2]; 
    }

    // face backgrounds for different color players
    sprintf(namebuf, "STFB%d", consoleplayer);
    faceback = (patch_t *) W_CacheLumpName(namebuf, PU_STATIC);

    // status bar background bits
    sbar = (patch_t *) W_CacheLumpName("STBAR", PU_STATIC);

    // face states
    facenum = 0;
    for (i=0;i<ST_NUMPAINFACES;i++)
    {
	for (j=0;j<ST_NUMSTRAIGHTFACES;j++)
	{
	    sprintf(namebuf, "STFST%d%d", i, j);
	    faces[facenum++] = W_CacheLumpName(namebuf, PU_STATIC);
	}
	sprintf(namebuf, "STFTR%d0", i);	// turn right
	faces[facenum++] = W_CacheLumpName(namebuf, PU_STATIC);
	sprintf(namebuf, "STFTL%d0", i);	// turn left
	faces[facenum++] = W_CacheLumpName(namebuf, PU_STATIC);
	sprintf(namebuf, "STFOUCH%d", i);	// ouch!
	faces[facenum++] = W_CacheLumpName(namebuf, PU_STATIC);
	sprintf(namebuf, "STFEVL%d", i);	// evil grin ;)
	faces[facenum++] = W_CacheLumpName(namebuf, PU_STATIC);
	sprintf(namebuf, "STFKILL%d", i);	// pissed off
	faces[facenum++] = W_CacheLumpName(namebuf, PU_STATIC);
    }
    faces[facenum++] = W_CacheLumpName("STFGOD0", PU_STATIC);
    faces[facenum++] = W_CacheLumpName("STFDEAD0", PU_STATIC);

}



void ST_unloadGraphics(void)
{

    int i;

    // unload the numbers, tall and short
    for (i=0;i<10;i++)
    {
	Z_ChangeTag(tallnum[i], PU_CACHE);
	Z_ChangeTag(shortnum[i], PU_CACHE);
    }
    // unload tall percent
    Z_ChangeTag(tallpercent, PU_CACHE); 

    // unload arms background
    Z_ChangeTag(armsbg, PU_CACHE); 

    // unload gray #'s
    for (i=0;i<6;i++)
	Z_ChangeTag(arms[i][0], PU_CACHE);
    
    // unload the key cards
    for (i=0;i<NUMCARDS;i++)
	Z_ChangeTag(keys[i], PU_CACHE);

    Z_ChangeTag(sbar, PU_CACHE);
    Z_ChangeTag(faceback, PU_CACHE);

    for (i=0;i<ST_NUMFACES;i++)
	Z_ChangeTag(faces[i], PU_CACHE);

    // Note: nobody ain't seen no unloading
    //   of stminus yet. Dude.
    

}

void ST_unloadData(void)
{
    ST_unloadGraphics();
}

void ST_initData(void)
{

    int		i;

    st_firsttime = true;
    plyr = &players[consoleplayer];

    st_clock = 0;
    st_chatstate = StartChatState;
    st_gamestate = FirstPersonState;

    st_statusbaron = true;
    st_oldchat = st_chat = false;
    st_cursoron = false;

    st_faceindex = 0;
    st_palette = -1;

    st_oldhealth = -1;

    for (i=0;i<NUMWEAPONS;i++)
	oldweaponsowned[i] = plyr->weaponowned[i];

    for (i=0;i<3;i++)
	keyboxes[i] = -1;

    STlib_init();

}



void ST_createWidgets(void)
{

    int i;

    // ready weapon ammo
    STlib_initNum(&w_ready,
		  ST_AMMOX,
		  ST_AMMOY,
		  tallnum,
		  &plyr->ammo[weaponinfo[plyr->readyweapon].ammo],
		  &st_statusbaron,
		  ST_AMMOWIDTH );

    // the last weapon type
    w_ready.data = plyr->readyweapon; 

    // health percentage
    STlib_initPercent(&w_health,
		      ST_HEALTHX,
		      ST_HEALTHY,
		      tallnum,
		      &plyr->health,
		      &st_statusbaron,
		      tallpercent);

    // arms background
    STlib_initBinIcon(&w_armsbg,
		      ST_ARMSBGX,
		      ST_ARMSBGY,
		      armsbg,
		      &st_notdeathmatch,
		      &st_statusbaron);

    // weapons owned
    for(i=0;i<6;i++)
    {
	STlib_initMultIcon(&w_arms[i],
			   ST_ARMSX+(i%3)*ST_ARMSXSPACE,
			   ST_ARMSY+(i/3)*ST_ARMSYSPACE,
			   arms[i], (int *) &plyr->weaponowned[i+1],
			   &st_armson);
    }

    // frags sum
    STlib_initNum(&w_frags,
		  ST_FRAGSX,
		  ST_FRAGSY,
		  tallnum,
		  &st_fragscount,
		  &st_fragson,
		  ST_FRAGSWIDTH);

    // faces
    STlib_initMultIcon(&w_faces,
		       ST_FACESX,
		       ST_FACESY,
		       faces,
		       &st_faceindex,
		       &st_statusbaron);

    // armor percentage - should be colored later
    STlib_initPercent(&w_armor,
		      ST_ARMORX,
		      ST_ARMORY,
		      tallnum,
		      &plyr->armorpoints,
		      &st_statusbaron, tallpercent);

    // keyboxes 0-2
    STlib_initMultIcon(&w_keyboxes[0],
		       ST_KEY0X,
		       ST_KEY0Y,
		       keys,
		       &keyboxes[0],
		       &st_statusbaron);
    
    STlib_initMultIcon(&w_keyboxes[1],
		       ST_KEY1X,
		       ST_KEY1Y,
		       keys,
		       &keyboxes[1],
		       &st_statusbaron);

    STlib_initMultIcon(&w_keyboxes[2],
		       ST_KEY2X,
		       ST_KEY2Y,
		       keys,
		       &keyboxes[2],
		       &st_statusbaron);

    // ammo count (all four kinds)
    STlib_initNum(&w_ammo[0],
		  ST_AMMO0X,
		  ST_AMMO0Y,
		  shortnum,
		  &plyr->ammo[0],
		  &st_statusbaron,
		  ST_AMMO0WIDTH);

    STlib_initNum(&w_ammo[1],
		  ST_AMMO1X,
		  ST_AMMO1Y,
		  shortnum,
		  &plyr->ammo[1],
		  &st_statusbaron,
		  ST_AMMO1WIDTH);

    STlib_initNum(&w_ammo[2],
		  ST_AMMO2X,
		  ST_AMMO2Y,
		  shortnum,
		  &plyr->ammo[2],
		  &st_statusbaron,
		  ST_AMMO2WIDTH);
    
    STlib_initNum(&w_ammo[3],
		  ST_AMMO3X,
		  ST_AMMO3Y,
		  shortnum,
		  &plyr->ammo[3],
		  &st_statusbaron,
		  ST_AMMO3WIDTH);

    // max ammo count (all four kinds)
    STlib_initNum(&w_maxammo[0],
		  ST_MAXAMMO0X,
		  ST_MAXAMMO0Y,
		  shortnum,
		  &plyr->maxammo[0],
		  &st_statusbaron,
		  ST_MAXAMMO0WIDTH);

    STlib_initNum(&w_maxammo[1],
		  ST_MAXAMMO1X,
		  ST_MAXAMMO1Y,
		  shortnum,
		  &plyr->maxammo[1],
		  &st_statusbaron,
		  ST_MAXAMMO1WIDTH);

    STlib_initNum(&w_maxammo[2],
		  ST_MAXAMMO2X,
		  ST_MAXAMMO2Y,
		  shortnum,
		  &plyr->maxammo[2],
		  &st_statusbaron,
		  ST_MAXAMMO2WIDTH);
    
    STlib_initNum(&w_maxammo[3],
		  ST_MAXAMMO3X,
		  ST_MAXAMMO3Y,
		  shortnum,
		  &plyr->maxammo[3],
		  &st_statusbaron,
		  ST_MAXAMMO3WIDTH);

}






}
