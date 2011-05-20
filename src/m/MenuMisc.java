package m;

import hu.HU;
import i.DoomSystem;
import i.IDoomSystem;

import java.nio.ByteBuffer;
import doom.DoomStatus;
import doom.IDoomGame;
import rr.RendererState;
import s.IDoomSound;
import v.DoomVideoRenderer;
import w.DoomFile;
import w.IWadLoader;
import w.IWritableDoomObject;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: MenuMisc.java,v 1.17 2011/05/20 18:24:51 velktron Exp $
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
//
// $Log: MenuMisc.java,v $
// Revision 1.17  2011/05/20 18:24:51  velktron
// Changes Manu to expose messages ON/OFF as booleans, as it should have.
//
// Revision 1.16  2011/05/20 14:51:10  velktron
// Added more DoomGame compliance.
//
// Revision 1.15  2011/05/18 16:54:03  velktron
// Duh?
//
// Revision 1.14  2011/05/17 16:51:20  velktron
// Switched to DoomStatus
//
// Revision 1.13  2011/05/13 17:44:57  velktron
// Fixed ReadFile and WriteFile so that they are actually useful.
//
// Revision 1.12  2011/05/10 10:39:18  velktron
// Semi-playable Techdemo v1.3 milestone
//
// Revision 1.11  2010/11/22 21:41:22  velktron
// Parallel rendering...sort of.It works, but either  the barriers are broken or it's simply not worthwhile at this point :-/
//
// Revision 1.10  2010/09/27 02:27:29  velktron
// BEASTLY update
//
// Revision 1.9  2010/09/24 17:58:39  velktron
// Menus and HU  functional -mostly.
//
// Revision 1.8  2010/09/23 20:36:45  velktron
// *** empty log message ***
//
// Revision 1.7  2010/09/23 15:11:57  velktron
// A bit closer...
//
// Revision 1.6  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.5  2010/09/22 16:40:02  velktron
// MASSIVE changes in the status passing model.
// DoomMain and DoomGame unified.
// Doomstat merged into DoomMain (now status and game functions are one).
//
// Most of DoomMain implemented. Possible to attempt a "classic type" start but will stop when reading sprites.
//
// Revision 1.4  2010/09/10 17:42:14  velktron
// Added Applet
//
// Revision 1.3  2010/09/10 17:35:49  velktron
// DoomGame, Menu, renderers
//
// Revision 1.2  2010/09/01 15:53:42  velktron
// Graphics data loader implemented....still need to figure out how column caching works, though.
//
// Revision 1.3  2010/08/23 14:36:08  velktron
// Menu mostly working, implemented Killough's fast hash-based GetNumForName, although it can probably be finetuned even more.
//
// Revision 1.2  2010/07/29 15:29:00  velktron
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
//	Main loop menu stuff.
//	Default Config File.
//	PCX Screenshots.
//
//-----------------------------------------------------------------------------

public abstract class MenuMisc{

public static final String rcsid = "$Id: MenuMisc.java,v 1.17 2011/05/20 18:24:51 velktron Exp $";
////////////////////// CONTEXT ///////////////////

DoomStatus DM;
IDoomGame DG;
IWadLoader W;
DoomVideoRenderer V;
HU HU;
RendererState R;
IDoomSystem I;
IDoomSound S;


//
// M_DrawText
// Returns the final X coordinate
// HU_Init must have been called to init the font
//

//
// M_WriteFile
//

//
// DEFAULTS
//
int		usemouse;
int		usejoystick;

/* These are all -or almost- in doomstat.c or game.c
extern int	key_right;
extern int	key_left;
extern int	key_up;
extern int	key_down;

extern int	key_strafeleft;
extern int	key_straferight;

extern int	key_fire;
extern int	key_use;
extern int	key_strafe;
extern int	key_speed;

extern int	mousebfire;
extern int	mousebstrafe;
extern int	mousebforward;

extern int	joybfire;
extern int	joybstrafe;
extern int	joybuse;
extern int	joybspeed;

extern int	viewwidth;
extern int	viewheight;

extern int	mouseSensitivity;
extern int	showMessages;

extern int	detailLevel;

extern int	screenblocks;

extern int	showMessages;

// machine-independent sound params
extern	int	numChannels;

*/


// UNIX hack, to be removed.
/*#ifdef SNDSERV
extern char*	sndserver_filename;
extern int	mb_used;
#endif
*/

/*
#ifdef LINUX
char*		mousetype;
char*		mousedev;
#endif
*/

//extern char*	chat_macros[];


/** We really need a better way to do this... */
/*
public void applyDefaults(){
    for (default_t d:defaults){
        switch (d.name){
        
        
        
        }
    
            
    }
}

*/

/*
default_t[]	defaults =
{
    new default_t("mouse_sensitivity",DS.mouseSensitivity, 5),
    new default_t("sfx_volume",DS.snd_SfxVolume, 8),
    new default_t("music_volume",DS.snd_MusicVolume, 8),
    new default_t("show_messages",DS.showMessages, 1),
// ifdef linux    
}
*/
/*
new default_t("key_right",&key_right, KEY_RIGHTARROW),
new default_t("key_left",&key_left, KEY_LEFTARROW),
new default_t("key_up",&key_up, KEY_UPARROW),
new default_t("key_down",&key_down, KEY_DOWNARROW),
new default_t("key_strafeleft",&key_strafeleft, ','),
new default_t("key_straferight",&key_straferight, '.'),

    new default_t("key_fire",&key_fire, KEY_RCTRL),
    new default_t("key_use",&key_use, ' '),
    new default_t("key_strafe",&key_strafe, KEY_RALT),
    new default_t("key_speed",&key_speed, KEY_RSHIFT),

// UNIX hack, to be removed. 
//#ifdef SNDSERV
    new default_t("sndserver", (int *) &sndserver_filename, (int) "sndserver"),
    new default_t("mb_used", &mb_used, 2),
//#endif
    
//#endif

//#ifdef LINUX
    new default_t("mousedev", (int*)&mousedev, (int)"/dev/ttyS0"),
    new default_t("mousetype", (int*)&mousetype, (int)"microsoft"),
//#endif

    new default_t("use_mouse",&usemouse, 1),
    new default_t("mouseb_fire",&mousebfire,0),
    new default_t("mouseb_strafe",&mousebstrafe,1),
    new default_t("mouseb_forward",&mousebforward,2),

    new default_t("use_joystick",&usejoystick, 0),
    new default_t("joyb_fire",&joybfire,0),
    new default_t("joyb_strafe",&joybstrafe,1),
    new default_t("joyb_use",&joybuse,3),
    new default_t("joyb_speed",&joybspeed,2),

    new default_t("screenblocks",&screenblocks, 9),
    new default_t("detaillevel",&detailLevel, 0),

    new default_t("snd_channels",&numChannels, 3),



    new default_t("usegamma",&usegamma, 0),

    new default_t("chatmacro0", (int *) &chat_macros[0], (int) HUSTR_CHATMACRO0 ),
    new default_t("chatmacro1", (int *) &chat_macros[1], (int) HUSTR_CHATMACRO1 ),
    new default_t("chatmacro2", (int *) &chat_macros[2], (int) HUSTR_CHATMACRO2 ),
    new default_t("chatmacro3", (int *) &chat_macros[3], (int) HUSTR_CHATMACRO3 ),
    new default_t("chatmacro4", (int *) &chat_macros[4], (int) HUSTR_CHATMACRO4 ),
    new default_t("chatmacro5", (int *) &chat_macros[5], (int) HUSTR_CHATMACRO5 ),
    new default_t("chatmacro6", (int *) &chat_macros[6], (int) HUSTR_CHATMACRO6 ),
    new default_t("chatmacro7", (int *) &chat_macros[7], (int) HUSTR_CHATMACRO7 ),
    new default_t("chatmacro8", (int *) &chat_macros[8], (int) HUSTR_CHATMACRO8 ),
    new default_t("chatmacro9", (int *) &chat_macros[9], (int) HUSTR_CHATMACRO9 )

};
*/

/*
int	numdefaults;
char*	defaultfile;


//
// M_SaveDefaults
//
public void M_SaveDefaults ()
{
    int		i;
    int		v;
    FILE*	f;
	
    f = fopen (defaultfile, "w");
    if (!f)
	return; // can't write the file, but don't complain
		
    for (i=0 ; i<numdefaults ; i++)
    {
	if (defaults[i].defaultvalue > -0xfff
	    && defaults[i].defaultvalue < 0xfff)
	{
	    v = *defaults[i].location;
	    fprintf (f,"%s\t\t%i\n",defaults[i].name,v);
	} else {
	    fprintf (f,"%s\t\t\"%s\"\n",defaults[i].name,
		     * (char **) (defaults[i].location));
	}
    }
	
    fclose (f);
}


//
// M_LoadDefaults
//
extern byte	scantokey[128];

public void LoadDefaults ()
{
    int		i;
    int		len;
    FILE*	f;
    char	def[80];
    char	strparm[100];
    char*	newstring;
    int		parm;
    boolean	isstring;
    
    // set everything to base values
    numdefaults = sizeof(defaults)/sizeof(defaults[0]);
    for (i=0 ; i<numdefaults ; i++)
	*defaults[i].location = defaults[i].defaultvalue;
    
    // check for a custom default file
    i = M_CheckParm ("-config");
    if (i && i<myargc-1)
    {
	defaultfile = myargv[i+1];
	printf ("	default file: %s\n",defaultfile);
    }
    else
	defaultfile = basedefault;
    
    // read the file in, overriding any set defaults
    f = fopen (defaultfile, "r");
    if (f)
    {
	while (!feof(f))
	{
	    isstring = false;
	    if (fscanf (f, "%79s %[^\n]\n", def, strparm) == 2)
	    {
		if (strparm[0] == '"')
		{
		    // get a string default
		    isstring = true;
		    len = strlen(strparm);
		    newstring = (char *) malloc(len);
		    strparm[len-1] = 0;
		    strcpy(newstring, strparm+1);
		}
		else if (strparm[0] == '0' && strparm[1] == 'x')
		    sscanf(strparm+2, "%x", &parm);
		else
		    sscanf(strparm, "%i", &parm);
		for (i=0 ; i<numdefaults ; i++)
		    if (!strcmp(def, defaults[i].name))
		    {
			if (!isstring)
			    *defaults[i].location = parm;
			else
			    *defaults[i].location =
				(int) newstring;
			break;
		    }
	    }
	}
		
	fclose (f);
    }
}


//
// SCREEN SHOTS
//

/*
typedef struct
{
    char		manufacturer;
    char		version;
    char		encoding;
    char		bits_per_pixel;

    unsigned short	xmin;
    unsigned short	ymin;
    unsigned short	xmax;
    unsigned short	ymax;
    
    unsigned short	hres;
    unsigned short	vres;

    unsigned char	palette[48];
    
    char		reserved;
    char		color_planes;
    unsigned short	bytes_per_line;
    unsigned short	palette_type;
    
    char		filler[58];
    unsigned char	data;		// unbounded
} pcx_t;
*/

//
// WritePCXfile
//

/*
public void
WritePCXfile
( String		filename,
  byte[]		data,
  int		width,
  int		height,
  byte[]		palette )
{
    int		i;
    int		length;
    pcx_t	pcx;
    byte[]	pack=new byte[width*height*2];
    int packlen;
	
//    pcx = new byteZ_Malloc (width*height*2+1000, PU_STATIC, NULL);

    pcx.manufacturer = 0x0a;		// PCX id
    pcx.version = 5;			// 256 color
    pcx.encoding = 1;			// uncompressed
    pcx.bits_per_pixel = 8;		// 256 color
    pcx.xmin = 0;
    pcx.ymin = 0;
    pcx.xmax = (char) (width-1);
    pcx.ymax = (char) (height-1);
    pcx.hres = (char) width;
    pcx.vres = (char) height;
    Arrays.fill(pcx.palette, (char)0);
    //memset (pcx.palette,0,sizeof(pcx->palette));
    pcx.color_planes = 1;		// chunky image
    pcx.bytes_per_line = (char) width;
    pcx.palette_type = 2;	// not a grey scale
    Arrays.fill(pcx.filler,(char) 0);
    //memset (pcx.filler,0,sizeof(pcx->filler));


    // pack the image

	
    for (i=0 ; i<width*height ; i++)
    {
	if ( (data[i] & 0xc0) != 0xc0)
	    pack[packlen++] = data[i];
	else
	{
	    pack[packlen++] = (byte) 0xc1;
	    pack[packlen++] = data[i];
	}
    }
    
    // write the palette
    pack[packlen++] = 0x0c;	// palette ID byte
    for (i=0 ; i<768 ; i++)
        pack[packlen++] = palette[i];
    
    // write output file
    length = packlen;
    WriteFile (filename, pcx, length);

    //Z_Free (pcx);
}
*/

//
// M_ScreenShot
//
/*

public void ScreenShot ()
{
    int		i;
    byte[]	linear;
    String	lbmname;
    
    // munge planar buffer to linear
    linear = V.screens[2];
    I_ReadScreen (linear);
    
    // find a file name to save it to
    strcpy(lbmname,"DOOM00.pcx");
		
    for (i=0 ; i<=99 ; i++)
    {
	lbmname[4] = i/10 + '0';
	lbmname[5] = i%10 + '0';
	if (access(lbmname,0) == -1)
	    break;	// file doesn't exist
    }
    if (i==100)
	I_Error ("M_ScreenShot: Couldn't create a PCX");
    
    // save the pcx file
    WritePCXfile (lbmname, linear,
		  SCREENWIDTH, SCREENHEIGHT,
		  W_CacheLumpName ("PLAYPAL",PU_CACHE));
	
    players[consoleplayer].message = "screen shot";
}
*/

public static boolean WriteFile(String name, byte[] source, int length) {
    DoomFile handle;
    try {
        handle = new DoomFile(name, "rw");

        if (handle == null)
            return false;

        handle.write(source, 0, length);
        handle.close();
    } catch (Exception e) {
        DoomSystem.MiscError("Couldn't write file %s (%s)", name, e.getMessage());
        return false;
    }

    return true;
}

public static boolean WriteFile(String name, IWritableDoomObject source) {
    DoomFile handle;
    try {
        handle = new DoomFile(name, "rw");

        if (handle == null) return false;

        source.write(handle);
        handle.close();
    } catch (Exception e) {
        DoomSystem.MiscError("Couldn't write file %s (%s)", name, e.getMessage());
        return false;
    }

    return true;
}


/** M_ReadFile 
 *  This version returns a variable-size ByteBuffer, so
 *  we don't need to know a-priori how much stuff to read.
 * 
 */
public static ByteBuffer ReadFile(String name) {
    DoomFile handle;
    int count, length;
    // struct stat fileinfo;
    ByteBuffer buf;
    try {
        handle = new DoomFile(name, "r");
        length = (int) handle.length();
        buf = ByteBuffer.allocate(length);
        count = handle.read(buf.array());
        handle.close();
    } catch (Exception e) {
        DoomSystem.MiscError("Couldn't read file %s (%s)", name, e.getMessage());
        return null;
    }

    return buf;
}

/** M_ReadFile */
public static int ReadFile(String name, byte[] buffer) {
    DoomFile handle;
    int count, length;
    // struct stat fileinfo;
    byte[] buf;
    try {
        handle = new DoomFile(name, "r");
        length = (int) handle.length();
        buf = new byte[length];
        count = handle.read(buf);
        handle.close();

        if (count < length)
            throw new Exception("Read only " + count + " bytes out of "
                    + length);

    } catch (Exception e) {
        DoomSystem.MiscError("Couldn't read file %s (%s)", name, e.getMessage());
        return -1;
    }
    System.arraycopy(buf, 0, buffer, 0, length);
    return length;
}

public abstract boolean getShowMessages();

public abstract void setShowMessages(boolean val);

}
