package hu;

// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: HU.java,v 1.19 2011/05/10 10:39:18 velktron Exp $
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
// $Log: HU.java,v $
// Revision 1.19  2011/05/10 10:39:18  velktron
// Semi-playable Techdemo v1.3 milestone
//
// Revision 1.18  2011/02/11 00:11:13  velktron
// A MUCH needed update to v1.3.
//
// Revision 1.17  2010/12/20 17:15:08  velktron
// Made the renderer more OO -> TextureManager and other changes as well.
//
// Revision 1.16  2010/11/22 21:41:21  velktron
// Parallel rendering...sort of.It works, but either  the barriers are broken or it's simply not worthwhile at this point :-/
//
// Revision 1.15  2010/11/22 14:54:53  velktron
// Greater objectification of sectors etc.
//
// Revision 1.14  2010/10/07 15:42:16  velktron
// *** empty log message ***
//
// Revision 1.13  2010/10/01 16:47:51  velktron
// Fixed tab interception.
//
// Revision 1.12  2010/09/27 15:07:44  velktron
// meh
//
// Revision 1.11  2010/09/27 02:27:29  velktron
// BEASTLY update
//
// Revision 1.10  2010/09/23 15:11:57  velktron
// A bit closer...
//
// Revision 1.9  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.8  2010/09/22 16:40:02  velktron
// MASSIVE changes in the status passing model.
// DoomMain and DoomGame unified.
// Doomstat merged into DoomMain (now status and game functions are one).
//
// Most of DoomMain implemented. Possible to attempt a "classic type" start but will stop when reading sprites.
//
// Revision 1.7  2010/09/07 16:23:00  velktron
// *** empty log message ***
//
// Revision 1.6  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.5  2010/08/30 15:53:19  velktron
// Screen wipes work...Finale coded but untested.
// GRID.WAD included for testing.
//
// Revision 1.4  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
// Revision 1.3 2010/07/29 15:28:59 velktron
// More work on menus...and digging some dependencies..
//
// Revision 1.2 2010/07/06 15:20:23 velktron
// Several changes in the WAD loading routine. Now lumps are directly unpacked
// as "CacheableDoomObjects" and only defaulting will result in "raw" DoomBuffer
// reads.
//
// Makes caching more effective.
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
// DESCRIPTION: Heads-up displays
//
// -----------------------------------------------------------------------------

import static data.Defines.*;

import java.text.Format;

import defines.*;
import static data.Limits.*;
import static doom.englsh.*;
import i.DoomStatusAware;
import utils.C2JUtils;
import v.DoomVideoRenderer;

import m.DoomMenu;
import m.Menu;
import m.Swap;
import rr.RendererState;
import rr.patch_t;
import s.DoomSoundInterface;
import w.IWadLoader;
import data.sounds.sfxenum_t;
import doom.DoomContext;
import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;


public class HU implements DoomStatusAware {
    public final static String rcsid =
        "$Id: HU.java,v 1.19 2011/05/10 10:39:18 velktron Exp $";

    // MAES: Status and wad data.
    IWadLoader W;

    DoomMain DM;

    DoomMenu M;

    RendererState R;
    
    DoomVideoRenderer V;

    DoomSoundInterface S;
    //
    // Locally used constants, shortcuts.
    // MAES: Some depend on STATE, so moved into constructor.

    String HU_TITLE, HU_TITLE2, HU_TITLEP, HU_TITLET;

    protected final static int HU_TITLEHEIGHT = 1;

    protected final static int HU_TITLEX = 0;

    protected int HU_TITLEY;// = (167 - Swap.SHORT(hu_font[0].height));

    protected final static char HU_INPUTTOGGLE = 't';

    protected final static int HU_INPUTX = HU_MSGX;

    protected int HU_INPUTY;// = (HU_MSGY +

    // HU_MSGHEIGHT*(Swap.SHORT(hu_font[0].height) +1));

    protected final static int HU_INPUTWIDTH = 64;

    protected final static int HU_INPUTHEIGHT = 1;

    protected static final String[] chat_macros =
        { HUSTR_CHATMACRO0, HUSTR_CHATMACRO1, HUSTR_CHATMACRO2,
                HUSTR_CHATMACRO3, HUSTR_CHATMACRO4, HUSTR_CHATMACRO5,
                HUSTR_CHATMACRO6, HUSTR_CHATMACRO7, HUSTR_CHATMACRO8,
                HUSTR_CHATMACRO9 };

    /** Needs to be seen by DoomGame */
    public final static String[] player_names =
        { HUSTR_PLRGREEN, HUSTR_PLRINDIGO, HUSTR_PLRBROWN, HUSTR_PLRRED };

    char chat_char; // remove later.

    player_t plr;

    // MAES: a whole lot of "static" stuff which really would be HU instance
    // status.
    patch_t[] hu_font = new patch_t[HU_FONTSIZE];

    char[] chat_dest = new char[MAXPLAYERS];

    // MAES: these used to be defined in hu_lib. We're going 100$ OO here...

    hu_itext_t[] w_inputbuffer;

    hu_textline_t w_title ;

    hu_itext_t w_chat;

    boolean[] always_off = { false };

    // Needs to be referenced by one of the widgets.
    public boolean[] chat_on = new boolean[1];

    // MAES: Ugly hack which allows it to be passed as reference. Sieg heil!
    boolean[] message_on = new boolean[1];

    boolean message_dontfuckwithme;

    boolean message_nottobefuckedwith;

    hu_stext_t w_message;

    int message_counter;

    // This is actually an "extern" pointing inside m_menu (Menu.java). So we
    // need to share Menu context.
    // int showMessages;
    // MAES: I think this is supposed to be visible by the various hu_ crap...
    boolean automapactive;

    boolean headsupactive = false;

    //
    // Builtin map names.
    // The actual names can be found in DStrings.h.
    //

    protected String[] mapnames = // DOOM shareware/registered/retail (Ultimate)
        // names.
        {

        HUSTR_E1M1, HUSTR_E1M2, HUSTR_E1M3, HUSTR_E1M4, HUSTR_E1M5, HUSTR_E1M6,
                HUSTR_E1M7, HUSTR_E1M8, HUSTR_E1M9,

                HUSTR_E2M1, HUSTR_E2M2, HUSTR_E2M3, HUSTR_E2M4, HUSTR_E2M5,
                HUSTR_E2M6, HUSTR_E2M7, HUSTR_E2M8, HUSTR_E2M9,

                HUSTR_E3M1, HUSTR_E3M2, HUSTR_E3M3, HUSTR_E3M4, HUSTR_E3M5,
                HUSTR_E3M6, HUSTR_E3M7, HUSTR_E3M8, HUSTR_E3M9,

                HUSTR_E4M1, HUSTR_E4M2, HUSTR_E4M3, HUSTR_E4M4, HUSTR_E4M5,
                HUSTR_E4M6, HUSTR_E4M7, HUSTR_E4M8, HUSTR_E4M9,

                "NEWLEVEL", "NEWLEVEL", "NEWLEVEL", "NEWLEVEL", "NEWLEVEL",
                "NEWLEVEL", "NEWLEVEL", "NEWLEVEL", "NEWLEVEL" };

    protected String[] mapnames2 = // DOOM 2 map names.
        { HUSTR_1, HUSTR_2, HUSTR_3, HUSTR_4, HUSTR_5, HUSTR_6, HUSTR_7,
                HUSTR_8, HUSTR_9, HUSTR_10, HUSTR_11,

                HUSTR_12, HUSTR_13, HUSTR_14, HUSTR_15, HUSTR_16, HUSTR_17,
                HUSTR_18, HUSTR_19, HUSTR_20,

                HUSTR_21, HUSTR_22, HUSTR_23, HUSTR_24, HUSTR_25, HUSTR_26,
                HUSTR_27, HUSTR_28, HUSTR_29, HUSTR_30, HUSTR_31, HUSTR_32 };

    protected String[] mapnamesp = // Plutonia WAD map names.
        { PHUSTR_1, PHUSTR_2, PHUSTR_3, PHUSTR_4, PHUSTR_5, PHUSTR_6, PHUSTR_7,
                PHUSTR_8, PHUSTR_9, PHUSTR_10, PHUSTR_11,

                PHUSTR_12, PHUSTR_13, PHUSTR_14, PHUSTR_15, PHUSTR_16,
                PHUSTR_17, PHUSTR_18, PHUSTR_19, PHUSTR_20,

                PHUSTR_21, PHUSTR_22, PHUSTR_23, PHUSTR_24, PHUSTR_25,
                PHUSTR_26, PHUSTR_27, PHUSTR_28, PHUSTR_29, PHUSTR_30,
                PHUSTR_31, PHUSTR_32 };

    protected String[] mapnamest = // TNT WAD map names.
        { THUSTR_1, THUSTR_2, THUSTR_3, THUSTR_4, THUSTR_5, THUSTR_6, THUSTR_7,
                THUSTR_8, THUSTR_9, THUSTR_10, THUSTR_11,

                THUSTR_12, THUSTR_13, THUSTR_14, THUSTR_15, THUSTR_16,
                THUSTR_17, THUSTR_18, THUSTR_19, THUSTR_20,

                THUSTR_21, THUSTR_22, THUSTR_23, THUSTR_24, THUSTR_25,
                THUSTR_26, THUSTR_27, THUSTR_28, THUSTR_29, THUSTR_30,
                THUSTR_31, THUSTR_32 };

    char[] shiftxform;

    public static final char[] french_shiftxform =
        {
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10,
                11,
                12,
                13,
                14,
                15,
                16,
                17,
                18,
                19,
                20,
                21,
                22,
                23,
                24,
                25,
                26,
                27,
                28,
                29,
                30,
                31,
                ' ',
                '!',
                '"',
                '#',
                '$',
                '%',
                '&',
                '"', // shift-'
                '(',
                ')',
                '*',
                '+',
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
                '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
                'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W',
                'X',
                'Y',
                'Z',
                '[', // shift-[
                '!', // shift-backslash - OH MY GOD DOES WATCOM SUCK
                ']', // shift-]
                '"',
                '_',
                '\'', // shift-`
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
                'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                'Y', 'Z', '{', '|', '}', '~', 127

        };

    public static final char[] english_shiftxform =
        {

                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10,
                11,
                12,
                13,
                14,
                15,
                16,
                17,
                18,
                19,
                20,
                21,
                22,
                23,
                24,
                25,
                26,
                27,
                28,
                29,
                30,
                31,
                ' ',
                '!',
                '"',
                '#',
                '$',
                '%',
                '&',
                '"', // shift-'
                '(',
                ')',
                '*',
                '+',
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
                '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
                'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W',
                'X',
                'Y',
                'Z',
                '[', // shift-[
                '!', // shift-backslash - OH MY GOD DOES WATCOM SUCK
                ']', // shift-]
                '"',
                '_',
                '\'', // shift-`
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
                'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                'Y', 'Z', '{', '|', '}', '~', 127 };

    // Maes: char?
    char[] frenchKeyMap =
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, ' ', '!', '"',
                '#', '$', '%', '&', '%', '(', ')', '*', '+', ';', '-', ':',
                '!', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':',
                'M', '<', '=', '>', '?', '@', 'Q', 'B', 'C', 'D', 'E', 'F',
                'G', 'H', 'I', 'J', 'K', 'L', ',', 'N', 'O', 'P', 'A', 'R',
                'S', 'T', 'U', 'V', 'Z', 'X', 'Y', 'W', '^', '\\', '$', '^',
                '_', '@', 'Q', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', ',', 'N', 'O', 'P', 'A', 'R', 'S', 'T', 'U', 'V',
                'Z', 'X', 'Y', 'W', '^', '\\', '$', '^', 127 };

    char ForeignTranslation(char ch) {
        return ch < 128 ? frenchKeyMap[ch] : ch;
    }

    public HU(DoomContext DM) {
    	this.updateStatus(DM);
    	
    	this.w_message=new hu_stext_t();

    	this.w_inputbuffer=new hu_itext_t[MAXPLAYERS];
    	for (int i=0;i<MAXPLAYERS;i++){
    		this.w_inputbuffer[i]=new hu_itext_t();
    	}
    	this.w_title=new hu_textline_t();
    	this.w_chat=new hu_itext_t();
    }

    /**
     * Loads a bunch of STCFNx fonts from WAD, and sets some of the remaining
     * constants.
     * 
     * @throws Exception
     */

    public void Init()
             {
        String xxx = new String("STCFN%03d");
        int i;
        int j;
        String buffer;

        if (DM.language == Language_t.french)
            shiftxform = french_shiftxform;
        else
            shiftxform = english_shiftxform;

        // load the heads-up font
        j = HU_FONTSTART;

        // So it basically loads a bunch of patch_t's from memory.
        C2JUtils.initArrayOfObjects(hu_font, patch_t.class);

        for (i = 0; i < HU_FONTSIZE; i++) {
            buffer = String.format(xxx,j++);
            // hu_font[i] = ((patch_t[]) wd.CacheLumpName(buffer, PU_STATIC);
            hu_font[i] = (W.CachePatchName(buffer, PU_STATIC));
        }

        // MAES: Doom's SC had a really fucked up endianness change for height.
        // I don't really see the point in that, as in the WAD patches appear
        // to be all Little Endian... mystery :-S
        // HU_TITLEY = (167 - Swap.SHORT(hu_font[0].height));
        HU_TITLEY = (167 - hu_font[0].height);
        HU_INPUTY = (HU_MSGY + HU_MSGHEIGHT * hu_font[0].height + 1);

    }

    public void Stop() {
        headsupactive = false;
    }

    public void Start() {

        int i;
        String s;
        
        // MAES: fugly hax. These were compile-time inlines,
        // so they can either work as functions, or be set whenever the HU is started
        // (typically once per level). They need to be aware of game progress,
        // and episode numbers <1 will cause it to bomb.
        this.HU_TITLE = mapnames[(DM.gameepisode - 1) * 9 + DM.gamemap - 1];
        this.HU_TITLE2 = mapnames2[DM.gamemap - 1];
        this.HU_TITLE = mapnamesp[DM.gamemap - 1];
        this.HU_TITLET = mapnamest[DM.gamemap - 1];

        if (headsupactive)
            this.Stop();

        plr = DM.players[DM.consoleplayer];
        message_on[0] = false;
        message_dontfuckwithme = false;
        message_nottobefuckedwith = false;
        chat_on[0] = false;

        // create the message widget
        this.w_message.initSText(HU_MSGX, HU_MSGY, HU_MSGHEIGHT, hu_font,
            HU_FONTSTART, this.message_on);

        // create the map title widget
        this.w_title.initTextLine(HU_TITLEX, HU_TITLEY, hu_font, HU_FONTSTART);

        switch (DM.gamemode) {
        case shareware:
        case registered:
        case retail:
            s = HU_TITLE;
            break;

       case pack_plut: s = HU_TITLEP; 
           break; 
       case pack_tnt: s = HU_TITLET;
       break;
       
        case commercial:
        default:
            s = HU_TITLE2;
            break;
        }

        // MAES: oh great, more pointer-char magic... oh no you don't, you ugly
        // cow horse and reindeer lover.

        // while (*s) this.w_title.addCharToTextLine(*(s++));

        this.w_title.addStringToTextLine(s);

        // create the chat widget
        this.w_chat.initIText(HU_INPUTX, HU_INPUTY, hu_font, HU_FONTSTART,
            chat_on);

        // create the inputbuffer widgets
        for (i = 0; i < MAXPLAYERS; i++) {
            w_inputbuffer[i] = new hu_itext_t();
            w_inputbuffer[i].initIText(0, 0, null, 0, always_off);
        }
        headsupactive = true;

    }

    public void Drawer() {

        this.w_message.drawSText();
        this.w_chat.drawIText();
        if (automapactive)
            this.w_title.drawTextLine(false);

    }

    public void Erase() {

        this.w_message.eraseSText();
        this.w_chat.eraseIText();
        this.w_title.eraseTextLine();

    }

    public void Ticker() {

        int i;
        boolean rc;
        char c;

        // tick down message counter if message is up
        if ((message_counter != 0) && !((--message_counter) != 0)) {
            message_on[0] = false;
            message_nottobefuckedwith = false;
        }

        if ((M.getShowMessages() != 0) || message_dontfuckwithme) {

            // display message if necessary
            if (((plr.message != null) && !message_nottobefuckedwith)
                    || ((plr.message != null) && message_dontfuckwithme)) {
                this.w_message.addMessageToSText(null, plr.message);
                plr.message = null;
                message_on[0] = true;
                message_counter = HU_MSGTIMEOUT;
                message_nottobefuckedwith = message_dontfuckwithme;
                message_dontfuckwithme = false;
            }

        } // else message_on = false;

        // check for incoming chat characters
        if (DM.netgame) {
            for (i = 0; i < MAXPLAYERS; i++) {
                if (!DM.playeringame[i])
                    continue;
                if ((i != DM.consoleplayer)
                        && ((c = DM.players[i].cmd.chatchar) != 0)) {
                    if (c <= HU_BROADCAST)
                        chat_dest[i] = c;
                    else {
                        if (c >= 'a' && c <= 'z')
                            c = (char) shiftxform[c];
                        rc = w_inputbuffer[i].keyInIText(c);
                        if (rc && c == KEY_ENTER) {
                            if ((w_inputbuffer[i].l.len != 0)
                                    && (chat_dest[i] == DM.consoleplayer + 1)
                                    || (chat_dest[i] == HU_BROADCAST)) {
                                w_message.addMessageToSText(player_names[i]
                                        , w_inputbuffer[i].l.l.toString());

                                message_nottobefuckedwith = true;
                                message_on[0] = true;
                                message_counter = HU_MSGTIMEOUT;
                                if (DM.gamemode == GameMode_t.commercial)
                                    S.StartSound(null, sfxenum_t.sfx_radio);
                                    
                                else
                                    S.StartSound(null, sfxenum_t.sfx_tink);
                                    
                            }
                            w_inputbuffer[i].resetIText();
                        }
                    }
                    DM.players[i].cmd.chatchar = 0;
                }
            }
        }

    }

    protected final int QUEUESIZE = 128;

    protected char[] chatchars = new char[QUEUESIZE];

    protected int head = 0;

    protected int tail = 0;

    public void queueChatChar(char c) {
        if (((head + 1) & (QUEUESIZE - 1)) == tail) {
            plr.message = HUSTR_MSGU;
        } else {
            chatchars[head] = c;
            head = (head + 1) & (QUEUESIZE - 1);
        }
    }

    public char dequeueChatChar() {
        char c;

        if (head != tail) {
            c = chatchars[tail];
            tail = (tail + 1) & (QUEUESIZE - 1);
        } else {
            c = 0;
        }

        return c;
    }

    // MAES: These were "static" inside HU_Responder, since they were meant to
    // represent state.
    protected StringBuilder lastmessage = new StringBuilder(HU_MAXLINELENGTH + 1);

    // protected char[] lastmessage=new char[HU_MAXLINELENGTH+1];
    protected boolean shiftdown = false;

    protected boolean altdown = false;

    protected char[] destination_keys =
        { HUSTR_KEYGREEN, HUSTR_KEYINDIGO, HUSTR_KEYBROWN, HUSTR_KEYRED };

    protected int num_nobrainers = 0;

    public boolean Responder(event_t ev) {

    	//System.out.println("Player "+DM.players[0].mo.x);
        char[] macromessage;
        boolean eatkey = false;

        char c;
        int i;
        int numplayers;

        numplayers = 0;
        // MAES: Adding BOOLEANS to ints, are we ?!
        for (i = 0; i < MAXPLAYERS; i++) {
            numplayers += (DM.playeringame[i]) ? 1 : 0;
        }

        if (ev.data1 == KEY_RSHIFT) {
            shiftdown = (ev.type == evtype_t.ev_keydown);
            return false;
        } else if (ev.data1 == KEY_RALT || ev.data1 == KEY_LALT) {
            altdown = (ev.type == evtype_t.ev_keydown);
            return false;
        }

        if (ev.type != evtype_t.ev_keydown)
            return false;

        if (!chat_on[0]) {
            if (ev.data1 == HU_MSGREFRESH) {
                message_on[0] = true;
                message_counter = HU_MSGTIMEOUT;
                eatkey = true;
            } else if (DM.netgame && ev.data1 == HU_INPUTTOGGLE) {
                eatkey = chat_on[0] = true;
                w_chat.resetIText();
                this.queueChatChar(HU_BROADCAST);
            } else if (DM.netgame && numplayers > 2) {
                for (i = 0; i < MAXPLAYERS; i++) {
                    if (ev.data1 == destination_keys[i]) {
                        if (DM.playeringame[i] && i != DM.consoleplayer) {
                            eatkey = chat_on[0] = true;
                            w_chat.resetIText();
                            this.queueChatChar((char) (i + 1));
                            break;
                        } else if (i == DM.consoleplayer) {
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
        } else {
            c = (char) ev.data1;
            // send a macro
            if (altdown) {
                c = (char) (c - '0');
                if (c > 9)
                    return false;
                // fprintf(stderr, "got here\n");
                macromessage = chat_macros[c].toCharArray();

                // kill last message with a '\n'
                this.queueChatChar(KEY_ENTER); // DEBUG!!!

                // send the macro message
                int index = 0;
                while (macromessage[index] != 0) {
                    this.queueChatChar(macromessage[index]);
                }
                this.queueChatChar(KEY_ENTER);

                // leave chat mode and notify that it was sent
                chat_on[0] = false;
                lastmessage.setLength(0);
                lastmessage.append(chat_macros[c]);
                plr.message = lastmessage.toString();
                eatkey = true;
            } else {
                if (DM.language == Language_t.french)
                    c = ForeignTranslation(c);
                if (shiftdown || (c >= 'a' && c <= 'z'))
                    c = shiftxform[c];
                eatkey = w_chat.keyInIText(c);
                if (eatkey) {
                    // static unsigned char buf[20]; // DEBUG
                    this.queueChatChar(c);

                    // sprintf(buf, "KEY: %d => %d", ev->data1, c);
                    // plr->message = buf;
                }
                if (c == KEY_ENTER) {
                    chat_on[0] = false;
                    if ((w_chat.l.len != 0)) {
                        lastmessage.setLength(0);
                        lastmessage.append( w_chat.l.l);
                        plr.message = new String(lastmessage);
                    }
                } else if (c == KEY_ESCAPE)
                    chat_on[0] = false;
            }
        }

        return eatkey;

    }

    // ///////////////////////////////// STRUCTS
    // ///////////////////////////////////

    /**
     *  Input Text Line widget
     *  (child of Text Line widget)
     */
    
    class hu_itext_t {


        hu_textline_t l; // text line to input on

        // left margin past which I am not to delete characters
        int lm;

        // pointer to boolean stating whether to update window
        boolean[] on;

        boolean laston; // last value of *->on;

        public hu_itext_t(){
        	
        }
        
        public void initIText(int x, int y, patch_t[] font, int startchar,
                boolean[] on) {
            this.lm = 0; // default left margin is start of text
            this.on = on;
            this.laston = true;
            l = new hu_textline_t(x, y, font, startchar);
        }

        // The following deletion routines adhere to the left margin restriction
        public void delCharFromIText() {
            if (this.l.len != this.lm)
                this.l.delCharFromTextLine();
        }

        public void eraseLineFromIText() {
            while (this.lm != this.l.len)
                l.delCharFromTextLine();
        }

        // Resets left margin as well
        public void resetIText() {
            this.lm = 0;
            this.l.clearTextLine();
        }

        public void addPrefixToIText(char[] str) {
            int ptr = 0;
            while (str[ptr] > 0) {
                l.addCharToTextLine(str[ptr++]);
                this.lm = this.l.len;
            }
        }

        // Maes: String overload
        public void addPrefixToIText(String str) {
            int ptr = 0;
            while (str.charAt(ptr) > 0) {
                l.addCharToTextLine(str.charAt(ptr++));
                this.lm = this.l.len;
            }
        }

        // wrapper function for handling general keyed input.
        // returns true if it ate the key
        public boolean keyInIText(char ch) {

            if (ch >= ' ' && ch <= '_')
                this.l.addCharToTextLine((char) ch);
            else if (ch == KEY_BACKSPACE)
                this.delCharFromIText();
            else if (ch != KEY_ENTER)
                return false; // did not eat key

            return true; // ate the key

        }

        public void drawIText() {

            if (!this.on[0])
                return;
            this.l.drawTextLine(true); // draw the line w/ cursor

        }

        void eraseIText() {
            if (this.laston && !this.on[0])
                this.l.needsupdate = 4;
            this.l.eraseTextLine();
            this.laston = this.on[0];
        }

    }

    /** Scrolling Text window widget
     *  (child of Text Line widget)
     */  

    class hu_stext_t {

        hu_textline_t[] l = new hu_textline_t[HU_MAXLINES]; // text lines to

        // draw

        int h; // height in lines

        int cl; // current line number

        // pointer to boolean stating whether to update window
        boolean[] on;

        boolean laston; // last value of *->on.

        public hu_stext_t(){
        	
        }
        
        public hu_stext_t(int x, int y, int h, patch_t[] font, int startchar,
                boolean[] on) {
            this.initSText(x, y, h, font, startchar, on);
        }

        public void initSText(int x, int y, int h, patch_t[] font,
                int startchar, boolean[] on) {

        	for (int i=0;i<HU_MAXLINES;i++){
        		this.l[i]=new hu_textline_t();
        	}
            this.h = h;
            this.on = on;
            this.laston = true;
            this.cl = 0;
            for (int i = 0; i < h; i++)
                this.l[i].initTextLine(x, y - i
                        * (Swap.SHORT(font[0].height) + 1), font, startchar);

        }

        public void addLineToSText() {

            // add a clear line
            if (++this.cl == this.h)
                this.cl = 0;
            this.l[this.cl].clearTextLine();

            // everything needs updating
            for (int i = 0; i < this.h; i++)
                this.l[i].needsupdate = 4;

        }

        public void addMessageToSText(char[] prefix, char[] msg) {
            this.addLineToSText();
            if ((prefix != null) && (prefix.length > 0)) {
                int ptr = 0;
                while ((ptr < prefix.length) && (prefix[ptr] > 0))
                    this.l[this.cl].addCharToTextLine(prefix[ptr++]);
                ptr = 0;
                while ((ptr < msg.length) && (msg[ptr] > 0))
                    this.l[this.cl].addCharToTextLine(msg[ptr++]);
            }
        }

        public void addMessageToSText(String prefix, String msg) {
            this.addLineToSText();
            if ((prefix != null) && (prefix.length() > 0)) {
                for (int i = 0; i < prefix.length(); i++)
                    this.l[this.cl].addCharToTextLine(prefix.charAt(i));
                for (int i = 0; i < msg.length(); i++)
                    this.l[this.cl].addCharToTextLine(msg.charAt(i));
            }
        }

        public void drawSText() {
            int i, idx;
            hu_textline_t l;

            if (!this.on[0])
                return; // if not on, don't draw

            // draw everything
            for (i = 0; i < this.h; i++) {
                idx = this.cl - i;
                if (idx < 0)
                    idx += this.h; // handle queue of lines

                l = this.l[idx];

                // need a decision made here on whether to skip the draw
                l.drawTextLine(false); // no cursor, please
            }

        }

        public void eraseSText() {
            for (int i = 0; i < this.h; i++) {
                if (laston && !on[0])
                    l[i].needsupdate = 4;
                this.l[i].eraseTextLine();
            }
            laston = on[0];

        }

        /**
         * MAES: this was the only variable in HUlib.c, and only instances of
         * hu_textline_t ever use it. For this reason, it makes sense to have it
         * common (?) between all instances of hu_textline_t and set it
         * somewhere else. Of course, if could be made an instance method or a
         * HUlib object could be defined.
         */
        protected boolean automapactive; // in AM_map.c

        public boolean isAutomapactive() {
            return automapactive;
        }

        public void setAutomapactive(boolean automapactive) {
            this.automapactive = automapactive;
        }

        /**
         * Same here.
         */

        // TODO: boolean : whether the screen is always erased
        protected boolean noterased; // =viewwindowx;

        public boolean isNoterased() {
            return noterased;
        }

        public void setNoterased(boolean noterased) {
            this.noterased = noterased;
        }

    }

    // Text Line widget
    // (parent of Scrolling Text and Input Text widgets)

    class hu_textline_t {

        // left-justified position of scrolling text window
        int x;

        int y;

        // MAES: was **
        patch_t[] f; // font

        int sc; // start character

        StringBuilder l = new StringBuilder(2*HU_MAXLINELENGTH); // line of text

        int len; // current line length

        // whether this line needs to be udpated
        int needsupdate;

        public hu_textline_t(){
        	
        }
        
        public void clearTextLine() {
            this.len = 0;
            this.l.setLength(0);
            // It's actually used as a status, go figure.
            this.needsupdate = 1;
        }

        // Maes: this could as well be the contructor

        public void initTextLine(int x, int y, patch_t[] f, int sc) {
            this.x = x;
            this.y = y;
            this.f = f;
            this.sc = sc;
            this.clearTextLine();
        }

        public hu_textline_t(int x, int y, patch_t[] f, int sc) {
            this.x = x;
            this.y = y;
            this.f = f;
            this.sc = sc;
            this.clearTextLine();
        }

        public boolean addCharToTextLine(char ch) {

            if (len == HU_MAXLINELENGTH)
                return false;
            else {
                this.l.setCharAt(len++,ch);
                // this.l[this.len] = 0;
                // MAES: for some reason this is set as "4", so this is a status
                // rather than a boolean.
                this.needsupdate = 4;
                return true;
            }

        }

        /**
         * MAES: This is much better than cluttering up the syntax everytime a
         * STRING must be added.
         * 
         * @param s
         * @return
         */

        public boolean addStringToTextLine(String s) {
            int index = 0;
            if (this.len == HU_MAXLINELENGTH)
                return false;
            else
                while ((index<s.length())&&(this.len < HU_MAXLINELENGTH)) {

                    this.l.append(s.charAt(index++));
                    this.len++;
                }
            this.l.append((char) 0);// final padding.

            // MAES: for some reason this is set as "4", so this is a
            // status rather than a boolean.

            this.needsupdate = 4;
            return true;
        }

        boolean delCharFromTextLine() {

            if (this.len == 0)
                return false;
            else {
                this.l.setCharAt(--this.len, (char)0);
                this.needsupdate = 4;
                return true;
            }

        }

        void drawTextLine(boolean drawcursor) {

            int i;
            int w;
            int x;
            char c;

            // draw the new stuff
            x = this.x;
            for (i = 0; i < this.len; i++) {
                c = Character.toUpperCase(l.charAt(i));
                if (c != ' ' && c >= this.sc && c <= '_') {
                    w = Swap.SHORT(this.f[c - this.sc].width);
                    if (x + w > SCREENWIDTH)
                        break;
                    // TODO: must implement renderer!
                    V.DrawPatchDirect(x, y, FG, f[c - sc]);
                    x += w;
                } else {
                    x += 4;
                    if (x >= SCREENWIDTH)
                        break;
                }
            }

            // draw the cursor if requested
            if (drawcursor
                    && x + Swap.SHORT(this.f['_' - this.sc].width) <= SCREENWIDTH) {
                // TODO: Implement V_ stuff!
                V.DrawPatchDirect(x, this.y, FG, this.f['_' - this.sc]);
            }
        }

        // MAES: was "static" in C within HUlib. Which may mean it's instance
        // specific or global-ish. Or both.
        protected boolean lastautomapactive = true;

        // sorta called by HU_Erase and just better darn get things straight
        public void eraseTextLine() {
            int lh;

            // Only erases when NOT in automap and the screen is reduced,
            // and the text must either need updating or refreshing
            // (because of a recent change back from the automap)

            if (!automapactive && (R.viewwindowx != 0)
                    && (this.needsupdate > 0)) {
                lh = this.f[0].height + 1;

                for (int y = this.y, yoffset = y * SCREENWIDTH; y < this.y + lh; y++, yoffset +=
                    SCREENWIDTH) {
                    // Stuff is probably in am_map??
                    if (y < R.viewwindowy
                            || y >= R.viewwindowy + R.viewheight)
                        R.VideoErase(yoffset, SCREENWIDTH); // erase entire
                    // line
                    else {
                        R.VideoErase(yoffset, R.viewwindowx); // erase left
                        // border
                        R.VideoErase(yoffset + R.viewwindowx + R.viewwidth,
                            R.viewwindowx);
                        // erase right border
                    }
                }
            }

            lastautomapactive = automapactive;
            if (this.needsupdate != 0)
                this.needsupdate--;

        }

    }

    public patch_t[] getHUFonts() {
        
        return this.hu_font;
    }

	@Override
	public void updateStatus(DoomContext DM) {
        this.DM = DM.DM;
        this.W = DM.W;
        this.R = (RendererState) DM.R;
        this.V=DM.V;
        this.S=DM.S;
        this.M=(Menu) DM.M;
		
	}
}