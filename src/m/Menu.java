package m;

import data.doomstat;
import data.Defines.GameMode_t;
import data.sounds.sfxenum_t;
import doom.DoomContext;
import rr.patch_t;
import v.DoomVideoRenderer;
import w.WadLoader;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Menu.java,v 1.4 2010/07/22 15:37:53 velktron Exp $
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
// $Log: Menu.java,v $
// Revision 1.4  2010/07/22 15:37:53  velktron
// MAJOR changes in Menu system.
//
// Revision 1.3  2010/07/21 11:41:47  velktron
// Work on menus...
//
// Revision 1.2  2010/07/06 15:20:23  velktron
// Several changes in the WAD loading routine. Now lumps are directly unpacked as "CacheableDoomObjects" and only defaulting will result in "raw" DoomBuffer reads.
//
// Makes caching more effective.
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
//	DOOM selection menu, options, episode etc.
//	Sliders and icons. Kinda widget stuff.
//
//-----------------------------------------------------------------------------

/*

#include "doomdef.h"
#include "dstrings.h"

#include "d_main.h"

#include "i_system.h"
#include "i_video.h"
#include "z_zone.h"
#include "v_video.h"
#include "w_wad.h"

#include "r_local.h"


#include "hu_stuff.h"

#include "g_game.h"

#include "m_argv.h"
#include "m_swap.h"

#include "s_sound.h"

#include "doomstat.h"

// Data.
#include "sounds.h"

#include "m_menu.h"
*/
import static data.Defines.*;
import static doom.englsh.NEWGAME;
import static doom.englsh.NIGHTMARE;
import static doom.englsh.SWSTRING;
import static data.dstrings.*;

public class Menu{

	doomstat DS;
	DoomContext DC;
	WadLoader W;
	DoomVideoRenderer V;
	
/** The fonts
 * 
 */
patch_t[]		hu_font= new patch_t[HU_FONTSIZE];

// WTF?!

boolean		message_dontfuckwithme;

boolean		chat_on;		// in heads-up code

//
// defaulted values
//
int			mouseSensitivity;       // has default

/** Show messages has default, 0 = off, 1 = on */
int			showMessages;
	

/** Blocky mode, has default, 0 = high, 1 = normal */
int			detailLevel;		
int			screenblocks;		// has default

/** temp for screenblocks (0-9) */
int			screenSize;		

/** -1 = no quicksave slot picked! */
int			quickSaveSlot;          

/** 1 = message to be printed */
int			messageToPrint;

/** ...and here is the message string! */
String			messageString;		

/** message x & y */
int			messx, messy;
int			messageLastMenuActive;

/** timed message = no input from user */
boolean			messageNeedsInput;     

//TODO: probably I need some MessageRoutine interface at this point?
//void    (*messageRoutine)(int response);

//char gammamsg[5][26] =

String[] gammamsg =
{
    "GAMMALVL0",
    
    "GAMMALVL1",
    "GAMMALVL2",
    "GAMMALVL3",
    "GAMMALVL4"
};

/** we are going to be entering a savegame string */
int			saveStringEnter;              
int           	saveSlot;	// which slot to save in
int			saveCharIndex;	// which char we're editing
/** old save description before edit */
char[]			saveOldString=new char[SAVESTRINGSIZE];  

boolean			inhelpscreens;
boolean			menuactive;

protected static final int SKULLXOFF	=	-32;
protected static final int LINEHEIGHT =		16;

public boolean		sendpause;
char[][]			savegamestrings=new char[10][SAVESTRINGSIZE];

char[]	endstring=new char[160];


//
// MENU TYPEDEFS
//


/** menu item skull is on */
short		itemOn;			

/** skull animation counter */
short		skullAnimCounter;	
/** which skull to draw */
short		whichSkull;		 

/** graphic name of skulls
   warning: initializer-string for array of chars is too long
   */
String[]    skullName= {"M_SKULL1","M_SKULL2"};

/**  current menudef */
// MAES: pointer? array?
menu_t	currentMenu;                          


//
// DOOM MENU
//

// MAES: was an enum called "main_e" used purely as numerals. No need for strong typing.
/**
 *  main_e enum;
 */
    private static int newgame = 0,
    options=1,
    loadgam=2,
    savegame=3,
    readthis=4,
    quitdoom=5,
    main_end=6;

MenuRoutine NewGame=new M_NewGame();
MenuRoutine Options =new M_Options();
MenuRoutine ChooseSkill=new M_ChooseSkill();
MenuRoutine Episode=new M_Episode();
MenuRoutine VerifyNightmare=new M_VerifyNightmare();

DrawRoutine DrawEpisode=new M_DrawEpisode();
DrawRoutine DrawNewGame=new M_DrawNewGame(); 
DrawRoutine DrawReadThis1=new M_DrawReadThis1();
DrawRoutine DrawReadThis2=new M_DrawReadThis2();
DrawRoutine DrawOptions=new M_DrawOptions();
//MenuRoutine NewGame=new NewGame(this.DC,this);
//MenuRoutine NewGame=new NewGame(this.DC,this);
//MenuRoutine NewGame=new NewGame(this.DC,this);

DrawRoutine DrawMainMenu=new M_DrawMainMenu();

    
menuitem_t MainMenu[]=
{
    new menuitem_t((short)1,"M_NGAME",NewGame,'n')
    /*new menuitem_t(1,"M_OPTION",M_Options,'o'),
    new menuitem_t(1,"M_LOADG",M_LoadGame,'l'),
    new menuitem_t(1,"M_SAVEG",M_SaveGame,'s'),
    // Another hickup with Special edition.
    new menuitem_t(1,"M_RDTHIS",M_ReadThis,'r'),
    new menuitem_t(1,"M_QUITG",M_QuitDOOM,'q')*/
};

menu_t  MainDef = new menu_t(
    main_end,
    null,
    MainMenu,
    DrawMainMenu,
    97,64,
    0);


//
// EPISODE SELECT
//
 
/** episodes_e enum */
private int ep1=0, 
            ep2=1,
            ep3=2,
            ep4=3,
            ep_end=4;


menuitem_t EpisodeMenu[]=
{
    new menuitem_t(1,"M_EPI1", Episode,'k'),
    new menuitem_t(1,"M_EPI2", Episode,'t'),
    new menuitem_t(1,"M_EPI3", Episode,'i'),
    new menuitem_t(1,"M_EPI4", Episode,'t')
};

menu_t  EpiDef =
new menu_t(
    ep_end,		// # of menu items
    MainDef,		// previous menu
    EpisodeMenu,	// menuitem_t ->
    DrawEpisode,	// drawing routine ->
    48,63,              // x,y
    ep1			// lastOn
);

//
// NEW GAME
//
/**
 * newgame_e enum;
 */
static int killthings=0,
                   toorough=1,
                   hurtme=2,
                   violence=3,
                   nightmare=4,
                   newg_end=5;




menuitem_t NewGameMenu[]=
{
    new menuitem_t(1,"M_JKILL",	ChooseSkill, 'i'),
    new menuitem_t(1,"M_ROUGH",	ChooseSkill, 'h'),
        new menuitem_t(1,"M_HURT",	ChooseSkill, 'h'),
            new menuitem_t(1,"M_ULTRA",	ChooseSkill, 'u'),
                new menuitem_t(1,"M_NMARE",	ChooseSkill, 'n')
};

menu_t  NewDef =
new menu_t(
    newg_end,		// # of menu items
    EpiDef,		// previous menu
    NewGameMenu,	// menuitem_t ->
    DrawNewGame,	// drawing routine ->
    48,63,              // x,y
    hurtme		// lastOn
);



//
// OPTIONS MENU
//


/** options_e enum;
 * 
 */

static int endgame=0,
   messages=1,
  detail=2,
scrnsize=3,
option_empty1=4,
mousesens=5,
option_empty2=6,
soundvol=7, opt_end=8;

menuitem_t[] OptionsMenu=
{
    new menuitem_t(1,"M_ENDGAM",	M_EndGame,'e'),
    new menuitem_t(1,"M_MESSG",	M_ChangeMessages,'m'),
    new menuitem_t(1,"M_DETAIL",	M_ChangeDetail,'g'),
    new menuitem_t(2,"M_SCRNSZ",	M_SizeDisplay,'s'),
    new menuitem_t(-1,"",null),
    new menuitem_t(2,"M_MSENS",	M_ChangeSensitivity,'m'),
    new menuitem_t(-1,"",null),
    new menuitem_t(1,"M_SVOL",	M_Sound,'s')
};

public menu_t  OptionsDef =
new menu_t(
    opt_end,
    this.MainDef,
    OptionsMenu,
    DrawOptions,
    60,37,
    0
);

//
// Read This! MENU 1 & 2
//

/**
 * read_e enum
 */
private static int  rdthsempty1=0,
    read1_end=1;

menuitem_t[] ReadMenu1[] 
{
    new menuitem_t(1,"",ReadThis2,0)
};

menu_t  ReadDef1 = new menu_t(
    read1_end,
    MainDef,
    ReadMenu1,
    DrawReadThis1,
    280,185,
    0
);

enum
{
    rdthsempty2,
    read2_end
} read_e2;

menuitem_t ReadMenu2[]=
{
    {1,"",M_FinishReadThis,0}
};

menu_t  ReadDef2 =
{
    read2_end,
    &ReadDef1,
    ReadMenu2,
    M_DrawReadThis2,
    330,175,
    0
};

//
// SOUND VOLUME MENU
//

/** sound_e enum */
static int sfx_vol=0,
    sfx_empty1=1,
    music_vol=2,
    sfx_empty2=3,
    sound_end=4;

menuitem_t SoundMenu[]=
{
    {2,"M_SFXVOL",M_SfxVol,'s'},
    {-1,"",0},
    {2,"M_MUSVOL",M_MusicVol,'m'},
    {-1,"",0}
};

menu_t  SoundDef =
{
    sound_end,
    &OptionsDef,
    SoundMenu,
    M_DrawSound,
    80,64,
    0
};

//
// LOAD GAME MENU
//

/**load_e enum */
    static int load1=0
    load2=1,
    load3=2,
    load4=3,
    load5=4,
    load6=5,
    load_end=6;

menuitem_t LoadMenu[]=
{
    {1,"", M_LoadSelect,'1'},
    {1,"", M_LoadSelect,'2'},
    {1,"", M_LoadSelect,'3'},
    {1,"", M_LoadSelect,'4'},
    {1,"", M_LoadSelect,'5'},
    {1,"", M_LoadSelect,'6'}
};

menu_t  LoadDef =
{
    load_end,
    &MainDef,
    LoadMenu,
    M_DrawLoad,
    80,54,
    0
};

//
// SAVE GAME MENU
//
menuitem_t SaveMenu[]=
{
    {1,"", M_SaveSelect,'1'},
    {1,"", M_SaveSelect,'2'},
    {1,"", M_SaveSelect,'3'},
    {1,"", M_SaveSelect,'4'},
    {1,"", M_SaveSelect,'5'},
    {1,"", M_SaveSelect,'6'}
};

menu_t  SaveDef =
{
    load_end,
    &MainDef,
    SaveMenu,
    M_DrawSave,
    80,54,
    0
};


//
// M_ReadSaveStrings
//  read the strings from the savegame files
//
void M_ReadSaveStrings(void)
{
    int             handle;
    int             count;
    int             i;
    char    name[256];
	
    for (i = 0;i < load_end;i++)
    {
	if (M_CheckParm("-cdrom"))
	    sprintf(name,"c:\\doomdata\\"SAVEGAMENAME"%d.dsg",i);
	else
	    sprintf(name,SAVEGAMENAME"%d.dsg",i);

	handle = open (name, O_RDONLY | 0, 0666);
	if (handle == -1)
	{
	    strcpy(&savegamestrings[i][0],EMPTYSTRING);
	    LoadMenu[i].status = 0;
	    continue;
	}
	count = read (handle, &savegamestrings[i], SAVESTRINGSIZE);
	close (handle);
	LoadMenu[i].status = 1;
    }
}


//
// M_LoadGame & Cie.
//
void M_DrawLoad(void)
{
    int             i;
	
    V_DrawPatchDirect (72,28,0,W_CacheLumpName("M_LOADG",PU_CACHE));
    for (i = 0;i < load_end; i++)
    {
	M_DrawSaveLoadBorder(LoadDef.x,LoadDef.y+LINEHEIGHT*i);
	M_WriteText(LoadDef.x,LoadDef.y+LINEHEIGHT*i,savegamestrings[i]);
    }
}



//
// Draw border for the savegame description
//
void M_DrawSaveLoadBorder(int x,int y)
{
    int             i;
	
    V_DrawPatchDirect (x-8,y+7,0,W_CacheLumpName("M_LSLEFT",PU_CACHE));
	
    for (i = 0;i < 24;i++)
    {
	V_DrawPatchDirect (x,y+7,0,W_CacheLumpName("M_LSCNTR",PU_CACHE));
	x += 8;
    }

    V_DrawPatchDirect (x,y+7,0,W_CacheLumpName("M_LSRGHT",PU_CACHE));
}



//
// User wants to load this game
//
void M_LoadSelect(int choice)
{
    char    name[256];
	
    if (M_CheckParm("-cdrom"))
	sprintf(name,"c:\\doomdata\\"SAVEGAMENAME"%d.dsg",choice);
    else
	sprintf(name,SAVEGAMENAME"%d.dsg",choice);
    G_LoadGame (name);
    M_ClearMenus ();
}

//
// Selected from DOOM menu
//
void M_LoadGame (int choice)
{
    if (netgame)
    {
	M_StartMessage(LOADNET,NULL,false);
	return;
    }
	
    M_SetupNextMenu(&LoadDef);
    M_ReadSaveStrings();
}


//
//  M_SaveGame & Cie.
//
void M_DrawSave(void)
{
    int             i;
	
    V_DrawPatchDirect (72,28,0,W_CacheLumpName("M_SAVEG",PU_CACHE));
    for (i = 0;i < load_end; i++)
    {
	M_DrawSaveLoadBorder(LoadDef.x,LoadDef.y+LINEHEIGHT*i);
	M_WriteText(LoadDef.x,LoadDef.y+LINEHEIGHT*i,savegamestrings[i]);
    }
	
    if (saveStringEnter)
    {
	i = M_StringWidth(savegamestrings[saveSlot]);
	M_WriteText(LoadDef.x + i,LoadDef.y+LINEHEIGHT*saveSlot,"_");
    }
}

//
// M_Responder calls this when user is finished
//
void M_DoSave(int slot)
{
    G_SaveGame (slot,savegamestrings[slot]);
    M_ClearMenus ();

    // PICK QUICKSAVE SLOT YET?
    if (quickSaveSlot == -2)
	quickSaveSlot = slot;
}

//
// User wants to save. Start string input for M_Responder
//
void M_SaveSelect(int choice)
{
    // we are going to be intercepting all chars
    saveStringEnter = 1;
    
    saveSlot = choice;
    strcpy(saveOldString,savegamestrings[choice]);
    if (!strcmp(savegamestrings[choice],EMPTYSTRING))
	savegamestrings[choice][0] = 0;
    saveCharIndex = strlen(savegamestrings[choice]);
}

//
// Selected from DOOM menu
//
void M_SaveGame (int choice)
{
    if (!usergame)
    {
	M_StartMessage(SAVEDEAD,NULL,false);
	return;
    }
	
    if (gamestate != GS_LEVEL)
	return;
	
    M_SetupNextMenu(&SaveDef);
    M_ReadSaveStrings();
}



//
//      M_QuickSave
//
char    tempstring[80];

void M_QuickSaveResponse(int ch)
{
    if (ch == 'y')
    {
	M_DoSave(quickSaveSlot);
	S_StartSound(NULL,sfx_swtchx);
    }
}

public void QuickSave()
{
    if (!DS.usergame)
    {
	S_StartSound(NULL,sfx_oof);
	return;
    }

    if (gamestate != GS_LEVEL)
	return;
	
    if (quickSaveSlot < 0)
    {
	StartControlPanel();
	ReadSaveStrings();
	SetupNextMenu(SaveDef);
	quickSaveSlot = -2;	// means to pick a slot now
	return;
    }
    //TODO: sprintf(tempstring,QSPROMPT,savegamestrings[quickSaveSlot]);
    //TODO:M_StartMessage(tempstring,M_QuickSaveResponse,true);
}



//
// M_QuickLoad
//
void M_QuickLoadResponse(int ch)
{
    if (ch == 'y')
    {
	M_LoadSelect(quickSaveSlot);
	S_StartSound(NULL,sfx_swtchx);
    }
}


public void QuickLoad()
{
    if (DS.netgame)
    {
	StartMessage(QLOADNET,NULL,false);
	return;
    }
	
    if (quickSaveSlot < 0)
    {
	StartMessage(QSAVESPOT,NULL,false);
	return;
    }
    //TODO: sprintf(tempstring,QLPROMPT,savegamestrings[quickSaveSlot]);
    //TODO: StartMessage(tempstring,M_QuickLoadResponse,true);
}


//
// Read This Menus
// Had a "quick hack to fix romero bug"
//
class M_DrawReadThis1 implements DrawRoutine{

    public void invoke(){
    inhelpscreens = true;
    switch ( gamemode )
    {
      case commercial:
	V.DrawPatchDirect (0,0,0,W_CacheLumpName("HELP",PU_CACHE));
	break;
      case shareware:
      case registered:
      case retail:
	V.DrawPatchDirect (0,0,0,W_CacheLumpName("HELP1",PU_CACHE));
	break;
      default:
	break;
    }
    return;
}
}


//
// Read This Menus - optional second page.
//
class M_DrawReadThis2 implements DrawRoutine{

    public void invoke()
{
    inhelpscreens = true;
    switch ( gamemode )
    {
      case retail:
      case commercial:
	// This hack keeps us from having to change menus.
	V.DrawPatchDirect (0,0,0,(patch_t)W.CacheLumpName("CREDIT",PU_CACHE,patch_t.class));
	break;
      case shareware:
      case registered:
	V.DrawPatchDirect (0,0,0,(patch_t)W.CacheLumpName("HELP2",PU_CACHE,patch_t.class));
	break;
      default:
	break;
    }
    return;
}
}


//
// Change Sfx & Music volumes
//
class M_DrawSound implements DrawRoutine{

    public void invoke(){
    V.DrawPatchDirect (60,38,0,(patch_t)W.CacheLumpName("M_SVOL",PU_CACHE,patch_t.class));

    DrawThermo(SoundDef.x,SoundDef.y+LINEHEIGHT*(sfx_vol+1),
		 16,snd_SfxVolume);

    DrawThermo(SoundDef.x,SoundDef.y+LINEHEIGHT*(music_vol+1),
		 16,snd_MusicVolume);
}
}

class M_Sound implements MenuRoutine{

    @Override
    public void invoke(int choice){

    SetupNextMenu(SoundDef);
}
    }

class M_SfxVol implements MenuRoutine{

    @Override
    public void invoke(int choice){
    switch(choice)
    {
      case 0:
	if (snd_SfxVolume)
	    snd_SfxVolume--;
	break;
      case 1:
	if (snd_SfxVolume < 15)
	    snd_SfxVolume++;
	break;
    }
	
    // TODO: S_SetSfxVolume(snd_SfxVolume /* *8 */);
}
}

class M_MusicVol implements MenuRoutine{

    @Override
    public void invoke(int choice){
    switch(choice)
    {
      case 0:
	if (DS.snd_MusicVolume!=0)
	    DS.snd_MusicVolume--;
	break;
      case 1:
	if (DS.snd_MusicVolume < 15)
	    DS.snd_MusicVolume++;
	break;
    }
	
    //TODO: S_SetMusicVolume(DS.snd_MusicVolume /* *8 */);
}
}






//
//      M_Episode
//
int     epi;



class M_Verify  implements MenuRoutine{

    @Override
    public void invoke(int ch)
    {
    if (ch != 'y')
	return;
		
    G_DeferedInitNew(nightmare,epi+1,1);
    M_ClearMenus ();
}
}

/**
 *      Toggle messages on/off
 */
class M_ChangeMessages implements MenuRoutine{

    @Override
    public void invoke(int choice)
    {
    // warning: unused parameter `int choice'
    choice = 0;
    showMessages = 1 - showMessages;
	
    if (showMessages==0)
	DS.players[DS.consoleplayer].message = MSGOFF;
    else
	DS.players[DS.consoleplayer].message = MSGON ;

    message_dontfuckwithme = true;
}
}

//
// M_EndGame
//
class M_EndGameResponse implements MenuRoutine{

    @Override
    public void invoke(int ch)
    {
    if (ch != 'y')
	return;
		
    currentMenu.lastOn = itemOn;
    M_ClearMenus ();
    D_StartTitle ();
}
}

class M_EndGame implements MenuRoutine{

    @Override
    public void invoke(int choice)
    {
    choice = 0;
    if (!usergame)
    {
	S_StartSound(NULL,sfx_oof);
	return;
    }
	
    if (netgame)
    {
	M_StartMessage(NETEND,NULL,false);
	return;
    }
	
    M_StartMessage(ENDGAME,M_EndGameResponse,true);
}
}



/**
* M_ReadThis
*/

class M_ReadThis implements MenuRoutine{

    @Override
    public void invoke(int choice)
    {
    choice = 0;
    SetupNextMenu(ReadDef1);
    }
}

class M_ReadThis2 implements MenuRoutine{

    @Override
    public void invoke(int choice)
    {
    choice = 0;
    SetupNextMenu(ReadDef2);
}
}

class M_FinishReadThis implements MenuRoutine{

    @Override
    public void invoke(int choice)
    {
    choice = 0;
    SetupNextMenu(MainDef);
}
}


class M_ChooseSkill implements MenuRoutine{

    @Override
    public void invoke(int choice)
    {
            if (choice == nightmare)
            {
            StartMessage(NIGHTMARE,VerifyNightmare,true);
            return;            }
            
            G.DeferedInitNew(choice,epi+1,1);
            ClearMenus ();
        }

}


//
// M_QuitDOOM
//
sfxenum_t[]    quitsounds =
{
        sfxenum_t.sfx_pldeth,
        sfxenum_t.sfx_dmpain,
        sfxenum_t.sfx_popain,
        sfxenum_t.sfx_slop,
        sfxenum_t.sfx_telept,
        sfxenum_t.sfx_posit1,
        sfxenum_t.sfx_posit3,
        sfxenum_t.sfx_sgtatk
};

sfxenum_t[]    quitsounds2 =
{
        sfxenum_t.sfx_vilact,
        sfxenum_t.sfx_getpow,
        sfxenum_t.sfx_boscub,
        sfxenum_t.sfx_slop,
        sfxenum_t.sfx_skeswg,
        sfxenum_t.sfx_kntdth,
        sfxenum_t.sfx_bspact,
        sfxenum_t.sfx_sgtatk
};


class M_QuitGame implements MenuRoutine{
    @Override
    public void invoke(int ch){
    if (ch != 'y')
	return;
    if (!DS.netgame)
    {
	if (DS.gamemode == GameMode_t.commercial);
	    // TODO:S_StartSound(NULL,quitsounds2[(gametic>>2)&7]);
	else;
	    //TODO:S_StartSound(NULL,quitsounds[(gametic>>2)&7]);
	//TODO:I_WaitVBL(105);
    }
    //TODO:I_Quit ();
}
}



class M_QuitDOOM implements MenuRoutine{
    @Override
    public void invoke(int choice){
  // We pick index 0 which is language sensitive,
  //  or one at random, between 1 and maximum number.
  if (DS.language != Language_t.english )
    endstring=endmsg[0]+"\n\n"+DOSY;
  else
    endstring=endmsg[ (DS.gametic%(NUM_QUITMESSAGES-2))+1 ]+"\n\n"+DOSY;
  
  StartMessage(endstring,M_QuitResponse,true);
}
}



class M_ChangeSensitivity implements MenuRoutine{
    @Override
    public void invoke(int choice)
{
    switch(choice)
    {
      case 0:
	if (mouseSensitivity)
	    mouseSensitivity--;
	break;
      case 1:
	if (mouseSensitivity < 9)
	    mouseSensitivity++;
	break;
    }
}
}


class M_ChangeDetail implements MenuRoutine{
    
    
    @Override
    public void invoke(int choice)
{
    choice = 0;
    detailLevel = 1 - detailLevel;

    // FIXME - does not work. Remove anyway?
    System.err.print("M_ChangeDetail: low detail mode n.a.\n");

    return;
    
    /*R_SetViewSize (screenblocks, detailLevel);

    if (!detailLevel)
	players[consoleplayer].message = DETAILHI;
    else
	players[consoleplayer].message = DETAILLO;*/
}
}

class M_SizeDisplay implements MenuRoutine {
    
    @Override
    public void invoke(int choice){
    switch(choice)
    {
      case 0:
	if (screenSize > 0)
	{
	    screenblocks--;
	    screenSize--;
	}
	break;
      case 1:
	if (screenSize < 8)
	{
	    screenblocks++;
	    screenSize++;
	}
	break;
    }
	

    R.SetViewSize (screenblocks, detailLevel);
}

}

class M_Options implements MenuRoutine{

    @Override
    public void invoke(int choice)
        {
            SetupNextMenu(OptionsDef);
        }

}
    
    class M_NewGame implements MenuRoutine{

        
        @Override
        public void invoke(int choice)
        {
            if (DS.netgame && !DS.demoplayback)
            {
            StartMessage(NEWGAME,null,false);
            return;
            }
            
            if ( DS.gamemode == GameMode_t.commercial )
            SetupNextMenu(NewDef);
            else
            SetupNextMenu(EpiDef);
        }

    }
    
    class M_Episode implements MenuRoutine{
        
        @Override
        public void invoke(int choice)
            {
            
                if ( (DS.gamemode == GameMode_t.shareware)
                 && (choice!=0))
                {
                StartMessage(SWSTRING,null,false);
                SetupNextMenu(M.ReadDef1);
                return;
                }

                // Yet another hack...
                if ((DS.gamemode == GameMode_t. registered)
                 && (choice > 2))
                {
                  System.err.print("M_Episode: 4th episode requires UltimateDOOM\n");
                  choice = 0;
                }
                 
                M.epi = choice;
                M.SetupNextMenu(M.NewDef);
             //TODO: ?   M.SetupNextMenu(M.OptionsDef);
            }

    }
    
    class M_DrawEpisode
    implements DrawRoutine {

@Override
public void invoke() {
        V.DrawPatchDirect (54,38,0,W.CachePatchName("M_EPISOD"));
    }


}

    
    class M_DrawMainMenu implements DrawRoutine{
        @Override
        public void invoke() {
         V.DrawPatchDirect (94,2,0,(patch_t)(W.CachePatchName("M_DOOM")));
        }
    }    
    
    class M_DrawNewGame
    implements DrawRoutine {

@Override
public void invoke() {
        V.DrawPatchDirect (96,14,0,(patch_t) W.CachePatchName("M_NEWG",PU_CACHE,patch_t.class));
        V.DrawPatchDirect (54,38,0,(patch_t) W.CachePatchName("M_SKILL",PU_CACHE,patch_t.class));
    }
}
    class M_DrawOptions
    implements DrawRoutine {

private static String    detailNames[]   = {"M_GDHIGH","M_GDLOW"};
private  static String  msgNames[]      = {"M_MSGOFF","M_MSGON"};


@Override
public void invoke() {
    menu_t OptionsDef =M.OptionsDef;
    
    V.DrawPatchDirect (108,15,0, W.CachePatchName("M_OPTTTL"));
    
    V.DrawPatchDirect (OptionsDef.x + 175,OptionsDef.y+LINEHEIGHT*detail,0,
        W.CachePatchName(detailNames[M.detailLevel]));

    V.DrawPatchDirect (OptionsDef.x + 120,OptionsDef.y+LINEHEIGHT*messages,0,
        W.CachePatchName(msgNames[M.showMessages]));

    M.DrawThermo(OptionsDef.x,OptionsDef.y+LINEHEIGHT*(mousesens+1),
         10,M.mouseSensitivity);
    
    M.DrawThermo(OptionsDef.x,OptionsDef.y+LINEHEIGHT*(scrnsize+1),
         9,M.screenSize);

    }


}
    

//
//      Menu Functions
//
public void
DrawThermo
( int	x,
  int	y,
  int	thermWidth,
  int	thermDot )
{
    int		xx;
    int		i;

    xx = x;
    V.DrawPatchDirect (xx,y,0,W.CachePatchName("M_THERML"));
    xx += 8;
    for (i=0;i<thermWidth;i++)
    {
	V.DrawPatchDirect (xx,y,0,W.CachePatchName("M_THERMM"));
	xx += 8;
    }
    V.DrawPatchDirect (xx,y,0,W.CachePatchName("M_THERMR"));

    V.DrawPatchDirect ((x+8) + thermDot*8,y,
		       0,W.CachePatchName("M_THERMO"));
}



public void
DrawEmptyCell
( menu_t	menu,
  int		item )
{
    V.DrawPatchDirect (menu.x - 10,        menu.y+item*LINEHEIGHT - 1, 0,
		       (patch_t)W.CacheLumpName("M_CELL1",PU_CACHE,patch_t.class));
}

public void
DrawSelCell
( menu_t	menu,
  int		item )
{
    V.DrawPatchDirect (menu.x - 10,        menu.y+item*LINEHEIGHT - 1, 0,
        (patch_t)W.CacheLumpName("M_CELL2",PU_CACHE,patch_t.class));
}


public void
StartMessage
( String		string,
  MenuRoutine		routine,
  boolean	input )
{
    messageLastMenuActive = menuactive;
    messageToPrint = 1;
    messageString = string;
    messageRoutine = routine;
    messageNeedsInput = input;
    menuactive = true;
    return;
}



public void StopMessage()
{
    menuactive = messageLastMenuActive;
    messageToPrint = 0;
}



/**
 * Find string width from hu_font chars
 */
public int StringWidth(char[] string)
{
    int             i;
    int             w = 0;
    int             c;
	
    for (i = 0;i < strlen(string);i++)
    {
	c = toupper(string[i]) - HU_FONTSTART;
	if (c < 0 || c >= HU_FONTSIZE)
	    w += 4;
	else
	    w += SHORT (hu_font[c].width);
    }
		
    return w;
}



/**
 *     Find string height from hu_font chars
 */
int StringHeight(char[] string)
{
    int             i;
    int             h;
    int             height = SHORT(hu_font[0].height);
	
    h = height;
    for (i = 0;i < strlen(string);i++)
	if (string[i] == '\n')
	    h += height;
		
    return h;
}


//
//      Write a string using the hu_font
//
void
M_WriteText
( int		x,
  int		y,
  char[]		string)
{
    /*int		w;
    char*	ch;
    int		c;
    int		cx;
    int		cy;
		

    ch = string;
    cx = x;
    cy = y;
	
    while(1)
    {
	c = *ch++;
	if (!c)
	    break;
	if (c == '\n')
	{
	    cx = x;
	    cy += 12;
	    continue;
	}
		
	c = toupper(c) - HU_FONTSTART;
	if (c < 0 || c>= HU_FONTSIZE)
	{
	    cx += 4;
	    continue;
	}
		
	w = SHORT (hu_font[c]->width);
	if (cx+w > SCREENWIDTH)
	    break;
	V_DrawPatchDirect(cx, cy, 0, hu_font[c]);
	cx+=w;
    }
    */
}



//
// CONTROL PANEL
//

//
// M_Responder
//
public boolean M_Responder (event_t ev)
{
    /*
    int             ch;
    int             i;
    static  int     joywait = 0;
    static  int     mousewait = 0;
    static  int     mousey = 0;
    static  int     lasty = 0;
    static  int     mousex = 0;
    static  int     lastx = 0;
	
    ch = -1;
	
    if (ev->type == ev_joystick && joywait < I_GetTime())
    {
	if (ev->data3 == -1)
	{
	    ch = KEY_UPARROW;
	    joywait = I_GetTime() + 5;
	}
	else if (ev->data3 == 1)
	{
	    ch = KEY_DOWNARROW;
	    joywait = I_GetTime() + 5;
	}
		
	if (ev->data2 == -1)
	{
	    ch = KEY_LEFTARROW;
	    joywait = I_GetTime() + 2;
	}
	else if (ev->data2 == 1)
	{
	    ch = KEY_RIGHTARROW;
	    joywait = I_GetTime() + 2;
	}
		
	if (ev->data1&1)
	{
	    ch = KEY_ENTER;
	    joywait = I_GetTime() + 5;
	}
	if (ev->data1&2)
	{
	    ch = KEY_BACKSPACE;
	    joywait = I_GetTime() + 5;
	}
    }
    else
    {
	if (ev->type == ev_mouse && mousewait < I_GetTime())
	{
	    mousey += ev->data3;
	    if (mousey < lasty-30)
	    {
		ch = KEY_DOWNARROW;
		mousewait = I_GetTime() + 5;
		mousey = lasty -= 30;
	    }
	    else if (mousey > lasty+30)
	    {
		ch = KEY_UPARROW;
		mousewait = I_GetTime() + 5;
		mousey = lasty += 30;
	    }
		
	    mousex += ev->data2;
	    if (mousex < lastx-30)
	    {
		ch = KEY_LEFTARROW;
		mousewait = I_GetTime() + 5;
		mousex = lastx -= 30;
	    }
	    else if (mousex > lastx+30)
	    {
		ch = KEY_RIGHTARROW;
		mousewait = I_GetTime() + 5;
		mousex = lastx += 30;
	    }
		
	    if (ev->data1&1)
	    {
		ch = KEY_ENTER;
		mousewait = I_GetTime() + 15;
	    }
			
	    if (ev->data1&2)
	    {
		ch = KEY_BACKSPACE;
		mousewait = I_GetTime() + 15;
	    }
	}
	else
	    if (ev->type == ev_keydown)
	    {
		ch = ev->data1;
	    }
    }
    
    if (ch == -1)
	return false;

    
    // Save Game string input
    if (saveStringEnter)
    {
	switch(ch)
	{
	  case KEY_BACKSPACE:
	    if (saveCharIndex > 0)
	    {
		saveCharIndex--;
		savegamestrings[saveSlot][saveCharIndex] = 0;
	    }
	    break;
				
	  case KEY_ESCAPE:
	    saveStringEnter = 0;
	    strcpy(&savegamestrings[saveSlot][0],saveOldString);
	    break;
				
	  case KEY_ENTER:
	    saveStringEnter = 0;
	    if (savegamestrings[saveSlot][0])
		M_DoSave(saveSlot);
	    break;
				
	  default:
	    ch = toupper(ch);
	    if (ch != 32)
		if (ch-HU_FONTSTART < 0 || ch-HU_FONTSTART >= HU_FONTSIZE)
		    break;
	    if (ch >= 32 && ch <= 127 &&
		saveCharIndex < SAVESTRINGSIZE-1 &&
		M_StringWidth(savegamestrings[saveSlot]) <
		(SAVESTRINGSIZE-2)*8)
	    {
		savegamestrings[saveSlot][saveCharIndex++] = ch;
		savegamestrings[saveSlot][saveCharIndex] = 0;
	    }
	    break;
	}
	return true;
    }
    
    // Take care of any messages that need input
    if (messageToPrint)
    {
	if (messageNeedsInput == true &&
	    !(ch == ' ' || ch == 'n' || ch == 'y' || ch == KEY_ESCAPE))
	    return false;
		
	menuactive = messageLastMenuActive;
	messageToPrint = 0;
	if (messageRoutine)
	    messageRoutine(ch);
			
	menuactive = false;
	S_StartSound(NULL,sfx_swtchx);
	return true;
    }
	
    if (devparm && ch == KEY_F1)
    {
	G_ScreenShot ();
	return true;
    }
		
    
    // F-Keys
    if (!menuactive)
	switch(ch)
	{
	  case KEY_MINUS:         // Screen size down
	    if (automapactive || chat_on)
		return false;
	    M_SizeDisplay(0);
	    S_StartSound(NULL,sfx_stnmov);
	    return true;
				
	  case KEY_EQUALS:        // Screen size up
	    if (automapactive || chat_on)
		return false;
	    M_SizeDisplay(1);
	    S_StartSound(NULL,sfx_stnmov);
	    return true;
				
	  case KEY_F1:            // Help key
	    M_StartControlPanel ();

	    if ( gamemode == retail )
	      currentMenu = &ReadDef2;
	    else
	      currentMenu = &ReadDef1;
	    
	    itemOn = 0;
	    S_StartSound(NULL,sfx_swtchn);
	    return true;
				
	  case KEY_F2:            // Save
	    M_StartControlPanel();
	    S_StartSound(NULL,sfx_swtchn);
	    M_SaveGame(0);
	    return true;
				
	  case KEY_F3:            // Load
	    M_StartControlPanel();
	    S_StartSound(NULL,sfx_swtchn);
	    M_LoadGame(0);
	    return true;
				
	  case KEY_F4:            // Sound Volume
	    M_StartControlPanel ();
	    currentMenu = &SoundDef;
	    itemOn = sfx_vol;
	    S_StartSound(NULL,sfx_swtchn);
	    return true;
				
	  case KEY_F5:            // Detail toggle
	    M_ChangeDetail(0);
	    S_StartSound(NULL,sfx_swtchn);
	    return true;
				
	  case KEY_F6:            // Quicksave
	    S_StartSound(NULL,sfx_swtchn);
	    M_QuickSave();
	    return true;
				
	  case KEY_F7:            // End game
	    S_StartSound(NULL,sfx_swtchn);
	    M_EndGame(0);
	    return true;
				
	  case KEY_F8:            // Toggle messages
	    M_ChangeMessages(0);
	    S_StartSound(NULL,sfx_swtchn);
	    return true;
				
	  case KEY_F9:            // Quickload
	    S_StartSound(NULL,sfx_swtchn);
	    M_QuickLoad();
	    return true;
				
	  case KEY_F10:           // Quit DOOM
	    S_StartSound(NULL,sfx_swtchn);
	    M_QuitDOOM(0);
	    return true;
				
	  case KEY_F11:           // gamma toggle
	    usegamma++;
	    if (usegamma > 4)
		usegamma = 0;
	    players[consoleplayer].message = gammamsg[usegamma];
	    I_SetPalette (W_CacheLumpName ("PLAYPAL",PU_CACHE));
	    return true;
				
	}

    
    // Pop-up menu?
    if (!menuactive)
    {
	if (ch == KEY_ESCAPE)
	{
	    M_StartControlPanel ();
	    S_StartSound(NULL,sfx_swtchn);
	    return true;
	}
	return false;
    }

    
    // Keys usable within menu
    switch (ch)
    {
      case KEY_DOWNARROW:
	do
	{
	    if (itemOn+1 > currentMenu->numitems-1)
		itemOn = 0;
	    else itemOn++;
	    S_StartSound(NULL,sfx_pstop);
	} while(currentMenu->menuitems[itemOn].status==-1);
	return true;
		
      case KEY_UPARROW:
	do
	{
	    if (!itemOn)
		itemOn = currentMenu->numitems-1;
	    else itemOn--;
	    S_StartSound(NULL,sfx_pstop);
	} while(currentMenu->menuitems[itemOn].status==-1);
	return true;

      case KEY_LEFTARROW:
	if (currentMenu->menuitems[itemOn].routine &&
	    currentMenu->menuitems[itemOn].status == 2)
	{
	    S_StartSound(NULL,sfx_stnmov);
	    currentMenu->menuitems[itemOn].routine(0);
	}
	return true;
		
      case KEY_RIGHTARROW:
	if (currentMenu->menuitems[itemOn].routine &&
	    currentMenu->menuitems[itemOn].status == 2)
	{
	    S_StartSound(NULL,sfx_stnmov);
	    currentMenu->menuitems[itemOn].routine(1);
	}
	return true;

      case KEY_ENTER:
	if (currentMenu->menuitems[itemOn].routine &&
	    currentMenu->menuitems[itemOn].status)
	{
	    currentMenu->lastOn = itemOn;
	    if (currentMenu->menuitems[itemOn].status == 2)
	    {
		currentMenu->menuitems[itemOn].routine(1);      // right arrow
		S_StartSound(NULL,sfx_stnmov);
	    }
	    else
	    {
		currentMenu->menuitems[itemOn].routine(itemOn);
		S_StartSound(NULL,sfx_pistol);
	    }
	}
	return true;
		
      case KEY_ESCAPE:
	currentMenu->lastOn = itemOn;
	M_ClearMenus ();
	S_StartSound(NULL,sfx_swtchx);
	return true;
		
      case KEY_BACKSPACE:
	currentMenu->lastOn = itemOn;
	if (currentMenu->prevMenu)
	{
	    currentMenu = currentMenu->prevMenu;
	    itemOn = currentMenu->lastOn;
	    S_StartSound(NULL,sfx_swtchn);
	}
	return true;
	
      default:
	for (i = itemOn+1;i < currentMenu->numitems;i++)
	    if (currentMenu->menuitems[i].alphaKey == ch)
	    {
		itemOn = i;
		S_StartSound(NULL,sfx_pstop);
		return true;
	    }
	for (i = 0;i <= itemOn;i++)
	    if (currentMenu->menuitems[i].alphaKey == ch)
	    {
		itemOn = i;
		S_StartSound(NULL,sfx_pstop);
		return true;
	    }
	break;
	
    }

    return false;
}



//
// M_StartControlPanel
//
void M_StartControlPanel (void)
{
    // intro might call this repeatedly
    if (menuactive)
	return;
    
    menuactive = 1;
    currentMenu = &MainDef;         // JDC
    itemOn = currentMenu->lastOn;   // JDC
    */
}


//
// M_Drawer
// Called after the view has been rendered,
// but before it has been blitted.
//
public void M_Drawer ()
{
 
    /*static short	x;
    static short	y;
    short		i;
    short		max;
    char		string[40];
    int			start;

    inhelpscreens = false;

    
    // Horiz. & Vertically center string and print it.
    if (messageToPrint)
    {
	start = 0;
	y = 100 - M_StringHeight(messageString)/2;
	while(*(messageString+start))
	{
	    for (i = 0;i < strlen(messageString+start);i++)
		if (*(messageString+start+i) == '\n')
		{
		    memset(string,0,40);
		    strncpy(string,messageString+start,i);
		    start += i+1;
		    break;
		}
				
	    if (i == strlen(messageString+start))
	    {
		strcpy(string,messageString+start);
		start += i;
	    }
				
	    x = 160 - M_StringWidth(string)/2;
	    M_WriteText(x,y,string);
	    y += SHORT(hu_font[0]->height);
	}
	return;
    }

    if (!menuactive)
	return;

    if (currentMenu->routine)
	currentMenu->routine();         // call Draw routine
    
    // DRAW MENU
    x = currentMenu->x;
    y = currentMenu->y;
    max = currentMenu->numitems;

    for (i=0;i<max;i++)
    {
	if (currentMenu->menuitems[i].name[0])
	    V_DrawPatchDirect (x,y,0,
			       W_CacheLumpName(currentMenu->menuitems[i].name ,PU_CACHE));
	y += LINEHEIGHT;
    }

    
    // DRAW SKULL
    V_DrawPatchDirect(x + SKULLXOFF,currentMenu->y - 5 + itemOn*LINEHEIGHT, 0,
		      W_CacheLumpName(skullName[whichSkull],PU_CACHE));
		      */

}


//
// M_ClearMenus
//
public void ClearMenus ()
{
    menuactive = 0;
    // if (!netgame && usergame && paused)
    //       sendpause = true;
}


/**
 *  M_SetupNextMenu
 */
public void SetupNextMenu(menu_t menudef)
{
    currentMenu = menudef;
    itemOn = currentMenu.lastOn;
}


/**
* M_Ticker
*/
public void Ticker ()
{
    if (--skullAnimCounter <= 0)
    {
	whichSkull ^= 1;
	skullAnimCounter = 8;
    }
}


/**
 * M_Init
 */
public void Init ()
{
    currentMenu = MainDef;
    menuactive = 0;
    itemOn = currentMenu.lastOn;
    whichSkull = 0;
    skullAnimCounter = 10;
    screenSize = screenblocks - 3;
    messageToPrint = 0;
    messageString = NULL;
    messageLastMenuActive = menuactive;
    quickSaveSlot = -1;

    // Here we could catch other version dependencies,
    //  like HELP1/2, and four episodes.

  
    switch ( gamemode )
    {
      case commercial:
	// This is used because DOOM 2 had only one HELP
        //  page. I use CREDIT as second page now, but
	//  kept this hack for educational purposes.
	MainMenu[readthis] = MainMenu[quitdoom];
	MainDef.numitems--;
	MainDef.y += 8;
	NewDef.prevMenu = &MainDef;
	ReadDef1.routine = M_DrawReadThis1;
	ReadDef1.x = 330;
	ReadDef1.y = 165;
	ReadMenu1[0].routine = M_FinishReadThis;
	break;
      case shareware:
	// Episode 2 and 3 are handled,
	//  branching to an ad screen.
      case registered:
	// We need to remove the fourth episode.
	EpiDef.numitems--;
	break;
      case retail:
	// We are fine.
      default:
	break;
    }
    
}



}