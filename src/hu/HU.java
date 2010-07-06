package hu;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: HU.java,v 1.2 2010/07/06 15:20:23 velktron Exp $
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
// $Log: HU.java,v $
// Revision 1.2  2010/07/06 15:20:23  velktron
// Several changes in the WAD loading routine. Now lumps are directly unpacked as "CacheableDoomObjects" and only defaulting will result in "raw" DoomBuffer reads.
//
// Makes caching more effective.
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
// DESCRIPTION:  Heads-up displays
//
//-----------------------------------------------------------------------------

import static data.Defines.*;
import static doom.englsh.*;
import utils.C2JUtils;
import utils.PrintfFormat;

import java.io.IOException;

import m.Swap;
import rr.patch_t;
import w.WadLoader;
import data.doomstat;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;

/*#include <ctype.h>

#include "doomdef.h"

#include "z_zone.h"

#include "m_swap.h"

#include "hu_stuff.h"
#include "hu_lib.h"
#include "w_wad.h"

#include "s_sound.h"

#include "doomstat.h"


// Data.
#include "dstrings.h"
#include "sounds.h"
*/

public class HU{
public final static String rcsid = "$Id: HU.java,v 1.2 2010/07/06 15:20:23 velktron Exp $";

// MAES: Status and wad data.
WadLoader wd;
doomstat ds;

//
// Locally used constants, shortcuts.
// MAES: Some depend on STATE, so moved into constructor.

String  HU_TITLE, HU_TITLE2, HU_TITLEP,HU_TITLET;

protected final static int HU_TITLEHEIGHT	=1;
protected final static int HU_TITLEX	=0;
protected int HU_TITLEY;//=	(167 - Swap.SHORT(hu_font[0].height));

protected final static char  HU_INPUTTOGGLE	='t';
protected final static int HU_INPUTX	=HU_MSGX;
protected int HU_INPUTY;//	= (HU_MSGY + HU_MSGHEIGHT*(Swap.SHORT(hu_font[0].height) +1));
protected final static int  HU_INPUTWIDTH	=64;
protected final static int  HU_INPUTHEIGHT	=1;



protected String[]	chat_macros =
{
    HUSTR_CHATMACRO0,
    HUSTR_CHATMACRO1,
    HUSTR_CHATMACRO2,
    HUSTR_CHATMACRO3,
    HUSTR_CHATMACRO4,
    HUSTR_CHATMACRO5,
    HUSTR_CHATMACRO6,
    HUSTR_CHATMACRO7,
    HUSTR_CHATMACRO8,
    HUSTR_CHATMACRO9
};

protected String[]	player_names =
{
    HUSTR_PLRGREEN,
    HUSTR_PLRINDIGO,
    HUSTR_PLRBROWN,
    HUSTR_PLRRED
};


char			chat_char; // remove later.

player_t	plr;

// MAES: a whole lot of "static" stuff which really would be HU instance status.
patch_t[] hu_font=new patch_t[HU_FONTSIZE];
char[]      chat_dest=new char[MAXPLAYERS];

//MAES: these used to be defined in hu_lib. We're going 100$ OO here...

hu_itext_t[] w_inputbuffer=new hu_itext_t[MAXPLAYERS];
hu_textline_t	w_title=new hu_textline_t();
hu_itext_t	w_chat=new hu_itext_t();

boolean	[]	always_off = {false};

boolean []        chat_on=new boolean[1];


// MAES: Ugly hack which allows it to be passed as reference. Sieg heil!
boolean[]		message_on=new boolean[1];
boolean			message_dontfuckwithme;
boolean		message_nottobefuckedwith;

hu_stext_t	w_message;
int		message_counter;

int		showMessages;
// MAES: I think this is supposed to be visible by the various hu_ crap...
boolean		automapactive;

boolean		headsupactive = false;

//
// Builtin map names.
// The actual names can be found in DStrings.h.
//

protected String[]	mapnames =	// DOOM shareware/registered/retail (Ultimate) names.
{

    HUSTR_E1M1,
    HUSTR_E1M2,
    HUSTR_E1M3,
    HUSTR_E1M4,
    HUSTR_E1M5,
    HUSTR_E1M6,
    HUSTR_E1M7,
    HUSTR_E1M8,
    HUSTR_E1M9,

    HUSTR_E2M1,
    HUSTR_E2M2,
    HUSTR_E2M3,
    HUSTR_E2M4,
    HUSTR_E2M5,
    HUSTR_E2M6,
    HUSTR_E2M7,
    HUSTR_E2M8,
    HUSTR_E2M9,

    HUSTR_E3M1,
    HUSTR_E3M2,
    HUSTR_E3M3,
    HUSTR_E3M4,
    HUSTR_E3M5,
    HUSTR_E3M6,
    HUSTR_E3M7,
    HUSTR_E3M8,
    HUSTR_E3M9,

    HUSTR_E4M1,
    HUSTR_E4M2,
    HUSTR_E4M3,
    HUSTR_E4M4,
    HUSTR_E4M5,
    HUSTR_E4M6,
    HUSTR_E4M7,
    HUSTR_E4M8,
    HUSTR_E4M9,

    "NEWLEVEL",
    "NEWLEVEL",
    "NEWLEVEL",
    "NEWLEVEL",
    "NEWLEVEL",
    "NEWLEVEL",
    "NEWLEVEL",
    "NEWLEVEL",
    "NEWLEVEL"
};

protected String[]	mapnames2 =	// DOOM 2 map names.
{
    HUSTR_1,
    HUSTR_2,
    HUSTR_3,
    HUSTR_4,
    HUSTR_5,
    HUSTR_6,
    HUSTR_7,
    HUSTR_8,
    HUSTR_9,
    HUSTR_10,
    HUSTR_11,
	
    HUSTR_12,
    HUSTR_13,
    HUSTR_14,
    HUSTR_15,
    HUSTR_16,
    HUSTR_17,
    HUSTR_18,
    HUSTR_19,
    HUSTR_20,
	
    HUSTR_21,
    HUSTR_22,
    HUSTR_23,
    HUSTR_24,
    HUSTR_25,
    HUSTR_26,
    HUSTR_27,
    HUSTR_28,
    HUSTR_29,
    HUSTR_30,
    HUSTR_31,
    HUSTR_32
};


protected String[]	mapnamesp =	// Plutonia WAD map names.
{
    PHUSTR_1,
    PHUSTR_2,
    PHUSTR_3,
    PHUSTR_4,
    PHUSTR_5,
    PHUSTR_6,
    PHUSTR_7,
    PHUSTR_8,
    PHUSTR_9,
    PHUSTR_10,
    PHUSTR_11,
	
    PHUSTR_12,
    PHUSTR_13,
    PHUSTR_14,
    PHUSTR_15,
    PHUSTR_16,
    PHUSTR_17,
    PHUSTR_18,
    PHUSTR_19,
    PHUSTR_20,
	
    PHUSTR_21,
    PHUSTR_22,
    PHUSTR_23,
    PHUSTR_24,
    PHUSTR_25,
    PHUSTR_26,
    PHUSTR_27,
    PHUSTR_28,
    PHUSTR_29,
    PHUSTR_30,
    PHUSTR_31,
    PHUSTR_32
};


protected String[] mapnamest =	// TNT WAD map names.
{
    THUSTR_1,
    THUSTR_2,
    THUSTR_3,
    THUSTR_4,
    THUSTR_5,
    THUSTR_6,
    THUSTR_7,
    THUSTR_8,
    THUSTR_9,
    THUSTR_10,
    THUSTR_11,
	
    THUSTR_12,
    THUSTR_13,
    THUSTR_14,
    THUSTR_15,
    THUSTR_16,
    THUSTR_17,
    THUSTR_18,
    THUSTR_19,
    THUSTR_20,
	
    THUSTR_21,
    THUSTR_22,
    THUSTR_23,
    THUSTR_24,
    THUSTR_25,
    THUSTR_26,
    THUSTR_27,
    THUSTR_28,
    THUSTR_29,
    THUSTR_30,
    THUSTR_31,
    THUSTR_32
};


char[]	shiftxform;

public static final char[] french_shiftxform =
{
    0,
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
    11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
    21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
    31,
    ' ', '!', '"', '#', '$', '%', '&',
    '"', // shift-'
    '(', ')', '*', '+',
    '?', // shift-,
    '_', // shift--
    '>', // shift-.
    '?', // shift-/
    '0', // shift-0
    '1', // shift-1
    '2', // shift-2
    '3', // shift-3
    '4', // shift-4
    '5', // shift-5
    '6', // shift-6
    '7', // shift-7
    '8', // shift-8
    '9', // shift-9
    '/',
    '.', // shift-;
    '<',
    '+', // shift-=
    '>', '?', '@',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    '[', // shift-[
    '!', // shift-backslash - OH MY GOD DOES WATCOM SUCK
    ']', // shift-]
    '"', '_',
    '\'', // shift-`
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    '{', '|', '}', '~', 127

};

public static final char[] english_shiftxform =
{

    0,
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
    11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
    21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
    31,
    ' ', '!', '"', '#', '$', '%', '&',
    '"', // shift-'
    '(', ')', '*', '+',
    '<', // shift-,
    '_', // shift--
    '>', // shift-.
    '?', // shift-/
    ')', // shift-0
    '!', // shift-1
    '@', // shift-2
    '#', // shift-3
    '$', // shift-4
    '%', // shift-5
    '^', // shift-6
    '&', // shift-7
    '*', // shift-8
    '(', // shift-9
    ':',
    ':', // shift-;
    '<',
    '+', // shift-=
    '>', '?', '@',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    '[', // shift-[
    '!', // shift-backslash - OH MY GOD DOES WATCOM SUCK
    ']', // shift-]
    '"', '_',
    '\'', // shift-`
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    '{', '|', '}', '~', 127
};

//Maes: char?
char[] frenchKeyMap=
{
    0,
    1,2,3,4,5,6,7,8,9,10,
    11,12,13,14,15,16,17,18,19,20,
    21,22,23,24,25,26,27,28,29,30,
    31,
    ' ','!','"','#','$','%','&','%','(',')','*','+',';','-',':','!',
    '0','1','2','3','4','5','6','7','8','9',':','M','<','=','>','?',
    '@','Q','B','C','D','E','F','G','H','I','J','K','L',',','N','O',
    'P','A','R','S','T','U','V','Z','X','Y','W','^','\\','$','^','_',
    '@','Q','B','C','D','E','F','G','H','I','J','K','L',',','N','O',
    'P','A','R','S','T','U','V','Z','X','Y','W','^','\\','$','^',127
};

char ForeignTranslation(char ch)
{
    return ch < 128 ? frenchKeyMap[ch] : ch;
}

public HU(doomstat ds, WadLoader wd){
    this.ds=ds;
    this.wd=wd;
    this.HU_TITLE=mapnames[(ds.gameepisode-1)*9+ds.gamemap-1];
    this.HU_TITLE2=mapnames2[ds.gamemap-1];
    this.HU_TITLE=mapnamesp[ds.gamemap-1];
    this.HU_TITLET=mapnamest[ds.gamemap-1];
    
    /*
    #define HU_TITLE    
    #define HU_TITLE2   (mapnames2[gamemap-1])
    #define HU_TITLEP   (mapnamesp[gamemap-1])
    #define HU_TITLET   (mapnamest[gamemap-1])
    */
    
}

/** Loads a bunch of STCFNx fonts from WAD, and sets some of the remaining constants.
 * 
 * @throws Exception
 */

public void Init() throws Exception
{
    PrintfFormat xxx=new PrintfFormat("STCFN%.3d");
    int		i;
    int		j;
    String	buffer;

    if (ds.language==Language_t.french)
	shiftxform = french_shiftxform;
    else
	shiftxform = english_shiftxform;

    // load the heads-up font
    j = HU_FONTSTART;
    
    // So it basically loads a bunch of patch_t's from memory.
    C2JUtils.initArrayOfObjects(hu_font,patch_t.class);
    
    
    for (i=0;i<HU_FONTSIZE;i++)
    {
	buffer=xxx.sprintf(j++);
	//hu_font[i] = ((patch_t[]) wd.CacheLumpName(buffer, PU_STATIC);
	hu_font[i]=(patch_t)( wd.CacheLumpName(buffer, PU_STATIC,patch_t.class));
    }
    
    // MAES: Doom's SC had a really fucked up endianness change for height.
    // I don't really see the point in that, as in the WAD patches appear
    // to be all Little Endian... mystery :-S
    //HU_TITLEY =   (167 - Swap.SHORT(hu_font[0].height));
    HU_TITLEY =   (167 - hu_font[0].height);
    HU_INPUTY = (HU_MSGY + HU_MSGHEIGHT*hu_font[0].height +1);

}

public void Stop()
{
    headsupactive = false;
}

public void Start()
{

    int		i;
    String	s;

    if (headsupactive)
	this.Stop();

    plr = ds.players[ds.consoleplayer];
    message_on[0] = false;
    message_dontfuckwithme = false;
    message_nottobefuckedwith = false;
    chat_on[0] = false;

    // create the message widget
    this.w_message.initSText(
		    HU_MSGX, HU_MSGY, HU_MSGHEIGHT,
		    hu_font,
		    HU_FONTSTART, this.message_on);

    // create the map title widget
    this.w_title.initTextLine(
		       HU_TITLEX, HU_TITLEY,
		       hu_font,
		       HU_FONTSTART);
    
    switch ( ds.gamemode )
    {
      case shareware:
      case registered:
      case retail:
	s = HU_TITLE;
	break;

/* FIXME
      case pack_plut:
	s = HU_TITLEP;
	break;
      case pack_tnt:
	s = HU_TITLET;
	break;
*/
	
      case commercial:
      default:
	 s = HU_TITLE2;
	 break;
    }
    
    //MAES: oh great, more pointer-char magic... oh no you don't, you ugly cow horse and reindeer lover. 
    
    //while (*s)    this.w_title.addCharToTextLine(*(s++));
    
    this.w_title.addStringToTextLine(s);

    // create the chat widget
    this.w_chat.initIText(
		    HU_INPUTX, HU_INPUTY,
		    hu_font,
		    HU_FONTSTART, chat_on);

    // create the inputbuffer widgets
    for (i=0 ; i<MAXPLAYERS ; i++){
        w_inputbuffer[i]=new hu_itext_t();
        w_inputbuffer[i].initIText( 0, 0, null, 0, always_off);
    }
    headsupactive = true;

}

public void Drawer()
{

    this.w_message.drawSText();
    this.w_chat.drawIText();
    if (automapactive)
	this.w_title.drawTextLine(false);

}

public void Erase()
{

    this.w_message.eraseSText();
    this.w_chat.eraseIText();
    this.w_title.eraseTextLine();

}

public void Ticker()
{

    int i;
    boolean rc;
    char c;

    // tick down message counter if message is up
    if ((message_counter!=0) && !((--message_counter)!=0))
    {
	message_on[0] = false;
	message_nottobefuckedwith = false;
    }

    if ((showMessages!=0) || message_dontfuckwithme)
    {

	// display message if necessary
	if (((plr.message!=null) && !message_nottobefuckedwith)
	    || ((plr.message!=null) && message_dontfuckwithme))
	{
	    this.w_message.addMessageToSText(null, plr.message);
	    plr.message = null;
	    message_on[0] = true;
	    message_counter = HU_MSGTIMEOUT;
	    message_nottobefuckedwith = message_dontfuckwithme;
	    message_dontfuckwithme = false;
	}

    } // else message_on = false;

    // check for incoming chat characters
    if (ds.netgame)
    {
	for (i=0 ; i<MAXPLAYERS; i++)
	{
	    if (!ds.playeringame[i])
		continue;
	    if ((i != ds.consoleplayer)
		&& ((c = ds.players[i].cmd.chatchar)!=0))
	    {
		if (c <= HU_BROADCAST)
		    chat_dest[i] = c;
		else
		{
		    if (c >= 'a' && c <= 'z')
			c = (char) shiftxform[c];
		    rc = w_inputbuffer[i].keyInIText(c);
		    if (rc && c == KEY_ENTER)
		    {
			if ((w_inputbuffer[i].l.len!=0)
			    && (chat_dest[i] == ds.consoleplayer+1)
				|| (chat_dest[i] == HU_BROADCAST))
			{
			    w_message.addMessageToSText(
						    player_names[i].toCharArray(),
						    w_inputbuffer[i].l.l);
			    
			    message_nottobefuckedwith = true;
			    message_on[0] = true;
			    message_counter = HU_MSGTIMEOUT;
			    if ( ds.gamemode == GameMode_t.commercial )
			     // TODO: S_StartSound(0, sfx_radio);
			        ;
			    else
			    //TODO:  S_StartSound(0, sfx_tink);
			        ;
			}
			w_inputbuffer[i].resetIText();
		    }
		}
		ds.players[i].cmd.chatchar = 0;
	    }
	}
    }

}

protected final int QUEUESIZE	=	128;

protected char[]	chatchars=new char[QUEUESIZE];
protected int	head = 0;
protected int	tail = 0;


public void queueChatChar(char c)
{
    if (((head + 1) & (QUEUESIZE-1)) == tail)
    {
	plr.message = HUSTR_MSGU;
    }
    else
    {
	chatchars[head] = c;
	head = (head + 1) & (QUEUESIZE-1);
    }
}

public char dequeueChatChar()
{
    char c;

    if (head != tail)
    {
	c = chatchars[tail];
	tail = (tail + 1) & (QUEUESIZE-1);
    }
    else
    {
	c = 0;
    }

    return c;
}

// MAES: These were "static" inside HU_Responder, since they were meant to represent state.
protected char[]     lastmessage=new char[HU_MAXLINELENGTH+1];
//protected char[]     lastmessage=new char[HU_MAXLINELENGTH+1];
protected boolean  shiftdown = false;

protected boolean  altdown = false;


protected char[]     destination_keys =
{
HUSTR_KEYGREEN,
HUSTR_KEYINDIGO,
HUSTR_KEYBROWN,
HUSTR_KEYRED
};

protected int      num_nobrainers = 0;

public boolean Responder(event_t ev)
{


    char[]		macromessage;
    boolean		eatkey = false;
    

    char 	c;
    int			i;
    int			numplayers;


    numplayers = 0;
    // MAES: Adding BOOLEANS to ints, are we ?!
    for (i=0 ; i<MAXPLAYERS ; i++){
	numplayers += (ds.playeringame[i])?1:0;
    }

    if (ev.data1 == KEY_RSHIFT)
    {
	shiftdown = (ev.type == evtype_t.ev_keydown);
	return false;
    }
    else if (ev.data1 == KEY_RALT || ev.data1 == KEY_LALT)
    {
	altdown = (ev.type == evtype_t.ev_keydown);
	return false;
    }

    if (ev.type != evtype_t.ev_keydown)
	return false;

    if (!chat_on[0])
    {
	if (ev.data1 == HU_MSGREFRESH)
	{
	    message_on[0] = true;
	    message_counter = HU_MSGTIMEOUT;
	    eatkey = true;
	}
	else if (ds.netgame && ev.data1 == HU_INPUTTOGGLE)
	{
	    eatkey = chat_on[0] = true;
	    w_chat.resetIText();
	    this.queueChatChar(HU_BROADCAST);
	}
	else if (ds.netgame && numplayers > 2)
	{
	    for (i=0; i<MAXPLAYERS ; i++)
	    {
		if (ev.data1 == destination_keys[i])
		{
		    if (ds.playeringame[i] && i!=ds.consoleplayer)
		    {
			eatkey = chat_on[0] = true;
			w_chat.resetIText();
			this.queueChatChar((char)(i+1));
			break;
		    }
		    else if (i == ds.consoleplayer)
		    {
			num_nobrainers++;
			if (num_nobrainers < 3)
			    plr.message = HUSTR_TALKTOSELF1;
			else if (num_nobrainers < 6)
			    plr.message = HUSTR_TALKTOSELF2;
			else if (num_nobrainers < 9)
			    plr.message = HUSTR_TALKTOSELF3;
			else if (num_nobrainers < 32)
			    plr.message = HUSTR_TALKTOSELF4;
			else
			    plr.message = HUSTR_TALKTOSELF5;
		    }
		}
	    }
	}
    }
    else
    {
	c = (char) ev.data1;
	// send a macro
	if (altdown)
	{
	    c = (char) (c - '0');
	    if (c > 9)
		return false;
	    // fprintf(stderr, "got here\n");
	    macromessage = chat_macros[c].toCharArray();
	    
	    // kill last message with a '\n'
	    this.queueChatChar(KEY_ENTER); // DEBUG!!!
	    
	    // send the macro message
	    int index=0;
	    while (macromessage[index]!=0){        
		this.queueChatChar(macromessage[index]);
	    }
	    this.queueChatChar(KEY_ENTER);
	    	    
	    // leave chat mode and notify that it was sent
	    chat_on[0] = false;
	    C2JUtils.strcpy(lastmessage, chat_macros[c]);
	    plr.message = new String(lastmessage);
	    eatkey = true;
	}
	else
	{
	    if (ds.language==Language_t.french)
		c = ForeignTranslation(c);
	    if (shiftdown || (c >= 'a' && c <= 'z'))
		c = shiftxform[c];
	    eatkey = w_chat.keyInIText( c);
	    if (eatkey)
	    {
		// static unsigned char buf[20]; // DEBUG
		this.queueChatChar(c);
		
		// sprintf(buf, "KEY: %d => %d", ev->data1, c);
		//      plr->message = buf;
	    }
	    if (c == KEY_ENTER)
	    {
		chat_on[0] = false;
		if ((w_chat.l.len!=0))
		{
		    C2JUtils.strcpy(lastmessage, w_chat.l.l);
		    plr.message = new String(lastmessage);
		}
	    }
	    else if (c == KEY_ESCAPE)
		chat_on[0] = false;
	}
    }

    return eatkey;

}
}