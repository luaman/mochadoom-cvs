package i;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
//$Id: IVideo.java,v 1.3 2010/09/25 17:37:13 velktron Exp $
//
//Copyright (C) 1993-1996 by id Software, Inc.
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//$Log: IVideo.java,v $
//Revision 1.3  2010/09/25 17:37:13  velktron
//Lots of changes.
//
//The most important is the creation of the AWTDoom frame handling I/O.
//
//Revision 1.2  2010/09/24 17:58:39  velktron
//Menus and HU  functional -mostly.
//
//Revision 1.1  2010/07/03 23:24:13  velktron
//Added a LOT of stuff, like Status bar code & objects. Now we're cooking with gas!
//
//
//DESCRIPTION:
//	DOOM graphics stuff for X11, UNIX.
//
//-----------------------------------------------------------------------------


/*#include "doomstat.h"
#include "i_system.h"
#include "v_video.h"
#include "m_argv.h"
#include "d_main.h"
*/

import static data.Defines.*;

import java.awt.AWTEvent;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import doom.event_t;

abstract class IVideo implements DoomVideoInterface, DoomEventInterface{
static final public String rcsid = "$Id: IVideo.java,v 1.3 2010/09/25 17:37:13 velktron Exp $";


protected int POINTER_WARP_COUNTDOWN	=1;

Graphics	X_display;
Window		X_mainWindow;
ColorModel	X_cmap;
//Visual		X_visual;
//GC		X_gc;
AWTEvent		X_event;//= AWTEvent. AWTEvent(lasttic, POINTER_WARP_COUNTDOWN, lasttic);
int		X_screen;
//XVisualInfo	X_visualinfo;
BufferedImage	image;
int		X_width;
int		X_height;

//MIT SHared Memory extension.
boolean		doShm;

//XShmSegmentInfo	X_shminfo;
int		X_shmeventtype;

//Fake mouse handling.
//This cannot work properly w/o DGA.
//Needs an invisible mouse cursor at least.
boolean		grabMouse;
int		doPointerWarp = POINTER_WARP_COUNTDOWN;

//Blocky mode,
//replace each 320x200 pixel with multiply*multiply pixels.
//According to Dave Taylor, it still is a bonehead thing
//to use ....
static int	multiply=1;


//
//Translates the key currently in X_event
//

public int xlatekey()
{

  int rc;

  switch(rc = 	  X_event.getID())
	  //Event.XKeycodeToKeysym(X_display, X_event.xkey.keycode, 0))

  {
    case Event.LEFT:	rc = KEY_LEFTARROW;	break;
    case Event.RIGHT:	rc = KEY_RIGHTARROW;	break;
    case Event.DOWN:	rc = KEY_DOWNARROW;	break;
    case Event.UP:	rc = KEY_UPARROW;	break;
    case Event.ESCAPE:	rc = KEY_ESCAPE;	break;
    case Event.ENTER:	rc = KEY_ENTER;		break;
    case Event.TAB:	rc = KEY_TAB;		break;
    case Event.F1:	rc = KEY_F1;		break;
    case Event.F2:	rc = KEY_F2;		break;
    case Event.F3:	rc = KEY_F3;		break;
    case Event.F4:	rc = KEY_F4;		break;
    case Event.F5:	rc = KEY_F5;		break;
    case Event.F6:	rc = KEY_F6;		break;
    case Event.F7:	rc = KEY_F7;		break;
    case Event.F8:	rc = KEY_F8;		break;
    case Event.F9:	rc = KEY_F9;		break;
    case Event.F10:	rc = KEY_F10;		break;
    case Event.F11:	rc = KEY_F11;		break;
    case Event.F12:	rc = KEY_F12;		break;
	
    //case Event.BACK_SPACE:
    case Event.DELETE:	rc = KEY_BACKSPACE;	break;

    case Event.PAUSE:	rc = KEY_PAUSE;		break;

    /*case Event.KP_Equal:
    case Event.equal:	rc = KEY_EQUALS;	break;*/

    /*case Event.KP_Subtract:
    case Event.minus:	rc = KEY_MINUS;		break;*/

    case Event.SHIFT_MASK:
	rc = KEY_RSHIFT;
	break;
	
    case Event.CTRL_MASK:
	rc = KEY_RCTRL;
	break;
	
    //case Event.ALT_MASK:
    case Event.META_MASK:
	rc = KEY_RALT;
	break;
	
    default:
	if (rc >= ' ' && rc <= '~')
	    rc = rc - Event.GOT_FOCUS + ' ';
	if (rc >= 'A' && rc <= 'Z')
	    rc = rc - 'A' + 'a';
	break;
  }

  return rc;

}
/*
public void I_ShutdownGraphics()
{
// Detach from X server
if (!XShmDetach(X_display, &X_shminfo))
	    I_Error("XShmDetach() failed in I_ShutdownGraphics()");

// Release shared memory.
shmdt(X_shminfo.shmaddr);
shmctl(X_shminfo.shmid, IPC_RMID, 0);

// Paranoia.
image->data = NULL;
}
*/



/**
 *  I_StartFrame 
 *  
 *  Doesn't do anything.
 */
public void StartFrame ()
{
  // er?

}

protected int	lastmousex = 0;
protected int	lastmousey = 0;
boolean		mousemoved = false;
boolean		shmFinished;

/** Does event handling and processing */

@Override
public void GetEvent()
{
/*
  event_t event;

  // put event-grabbing stuff in here
  XNextEvent(X_display, &X_event);
  switch (X_event.type)
  {
    case KeyPress:
	event.type = ev_keydown;
	event.data1 = xlatekey();
	D_PostEvent(&event);
	// fprintf(stderr, "k");
	break;
    case KeyRelease:
	event.type = ev_keyup;
	event.data1 = xlatekey();
	D_PostEvent(&event);
	// fprintf(stderr, "ku");
	break;
    case ButtonPress:
	event.type = ev_mouse;
	event.data1 =
	    (X_event.xbutton.state & Button1Mask)
	    | (X_event.xbutton.state & Button2Mask ? 2 : 0)
	    | (X_event.xbutton.state & Button3Mask ? 4 : 0)
	    | (X_event.xbutton.button == Button1)
	    | (X_event.xbutton.button == Button2 ? 2 : 0)
	    | (X_event.xbutton.button == Button3 ? 4 : 0);
	event.data2 = event.data3 = 0;
	D_PostEvent(&event);
	// fprintf(stderr, "b");
	break;
    case ButtonRelease:
	event.type = ev_mouse;
	event.data1 =
	    (X_event.xbutton.state & Button1Mask)
	    | (X_event.xbutton.state & Button2Mask ? 2 : 0)
	    | (X_event.xbutton.state & Button3Mask ? 4 : 0);
	// suggest parentheses around arithmetic in operand of |
	event.data1 =
	    event.data1
	    ^ (X_event.xbutton.button == Button1 ? 1 : 0)
	    ^ (X_event.xbutton.button == Button2 ? 2 : 0)
	    ^ (X_event.xbutton.button == Button3 ? 4 : 0);
	event.data2 = event.data3 = 0;
	D_PostEvent(&event);
	// fprintf(stderr, "bu");
	break;
    case MotionNotify:
	event.type = ev_mouse;
	event.data1 =
	    (X_event.xmotion.state & Button1Mask)
	    | (X_event.xmotion.state & Button2Mask ? 2 : 0)
	    | (X_event.xmotion.state & Button3Mask ? 4 : 0);
	event.data2 = (X_event.xmotion.x - lastmousex) << 2;
	event.data3 = (lastmousey - X_event.xmotion.y) << 2;

	if (event.data2 || event.data3)
	{
	    lastmousex = X_event.xmotion.x;
	    lastmousey = X_event.xmotion.y;
	    if (X_event.xmotion.x != X_width/2 &&
		X_event.xmotion.y != X_height/2)
	    {
		D_PostEvent(&event);
		// fprintf(stderr, "m");
		mousemoved = false;
	    } else
	    {
		mousemoved = true;
	    }
	}
	break;
	
    case Expose:
    case ConfigureNotify:
	break;
	
    default:
	if (doShm && X_event.type == X_shmeventtype) shmFinished = true;
	break;
  }
*/
}

/*
Cursor
createnullcursor
( Display	display,
Window	root )
{
  Pixmap cursormask;
  XGCValues xgc;
  GC gc;
  XColor dummycolour;
  Cursor cursor;

  cursormask = XCreatePixmap(display, root, 1, 1, 1 );
  xgc.function = GXclear;
  gc =  XCreateGC(display, cursormask, GCFunction, &xgc);
  XFillRectangle(display, cursormask, gc, 0, 0, 1, 1);
  dummycolour.pixel = 0;
  dummycolour.red = 0;
  dummycolour.flags = 04;
  cursor = XCreatePixmapCursor(display, cursormask, cursormask,
				 &dummycolour,&dummycolour, 0,0);
  XFreePixmap(display,cursormask);
  XFreeGC(display,gc);
  return cursor;
}
*/

/**
 *  I_StartTic
 *  <p>
 *  Its purpose is to prepare for the next frame.
 *  Calls GetEvent (thus intercepting new events),
 *  gaining back windows focus (if applicable).
 *  </p>
 *  <br>
 *  From X's docs:
 *  <br>
 *  <p>The XPending() function returns the number of events 
 *  that have been received from the X server but have not 
 *  been removed from the event queue. XPending() is identical 
 *  to XEventsQueued() with the mode QueuedAfterFlush specified. 
 *  </p>
 * <br>
 * <p>Therefore, the call to XPending should verify a Queue of events
 * to see if there are more events. So IVideo should generally go into
 * whatever "video and keyboard" driver you happen to use.
 * </p> 
 */


public void StartTic ()
{

  if (!X_display)
	return;

  while (XPending(X_display))
	GetEvent();

  // Warp the pointer back to the middle of the window
  //  or it will wander off - that is, the game will
  //  loose input focus within X11.
  if (grabMouse)
  {
	if (!--doPointerWarp)
	{
	    XWarpPointer( X_display,
			  None,
			  X_mainWindow,
			  0, 0,
			  0, 0,
			  X_width/2, X_height/2);

	    doPointerWarp = POINTER_WARP_COUNTDOWN;
	}
  }

  mousemoved = false;

}


/**
* I_UpdateNoBlit
* 
* Presumably transfers video data from doom's buffer to the windowing/GUI
* susbystem before actually drawing it. Dummy.
* 
*/
public void UpdateNoBlit ()
{
  // what is this?
}

//
//I_FinishUpdate
//
protected long lasttic=0;
/*

public void I_FinishUpdate ()
{

  int		tics;
  int		i;
  // UNUSED static unsigned char *bigscreen=0;

  // draws little dots on the bottom of the screen
  if (devparm)
  {

	i = I_GetTime();
	tics = i - lasttic;
	lasttic = i;
	if (tics > 20) tics = 20;

	for (i=0 ; i<tics*2 ; i+=2)
	    screens[0][ (SCREENHEIGHT-1)*SCREENWIDTH + i] = 0xff;
	for ( ; i<20*2 ; i+=2)
	    screens[0][ (SCREENHEIGHT-1)*SCREENWIDTH + i] = 0x0;
  
  }

  // scales the screen size before blitting it
  if (multiply == 2)
  {
	unsigned int *olineptrs[2];
	unsigned int *ilineptr;
	int x, y, i;
	unsigned int twoopixels;
	unsigned int twomoreopixels;
	unsigned int fouripixels;

	ilineptr = (unsigned int *) (screens[0]);
	for (i=0 ; i<2 ; i++)
	    olineptrs[i] = (unsigned int *) &image->data[i*X_width];

	y = SCREENHEIGHT;
	while (y--)
	{
	    x = SCREENWIDTH;
	    do
	    {
		fouripixels = *ilineptr++;
		twoopixels =	(fouripixels & 0xff000000)
		    |	((fouripixels>>8) & 0xffff00)
		    |	((fouripixels>>16) & 0xff);
		twomoreopixels =	((fouripixels<<16) & 0xff000000)
		    |	((fouripixels<<8) & 0xffff00)
		    |	(fouripixels & 0xff);
#ifdef __BIG_ENDIAN__
		*olineptrs[0]++ = twoopixels;
		*olineptrs[1]++ = twoopixels;
		*olineptrs[0]++ = twomoreopixels;
		*olineptrs[1]++ = twomoreopixels;
#else
		*olineptrs[0]++ = twomoreopixels;
		*olineptrs[1]++ = twomoreopixels;
		*olineptrs[0]++ = twoopixels;
		*olineptrs[1]++ = twoopixels;
#endif
	    } while (x-=4);
	    olineptrs[0] += X_width/4;
	    olineptrs[1] += X_width/4;
	}

  }
  else if (multiply == 3)
  {
	unsigned int *olineptrs[3];
	unsigned int *ilineptr;
	int x, y, i;
	unsigned int fouropixels[3];
	unsigned int fouripixels;

	ilineptr = (unsigned int *) (screens[0]);
	for (i=0 ; i<3 ; i++)
	    olineptrs[i] = (unsigned int *) &image->data[i*X_width];

	y = SCREENHEIGHT;
	while (y--)
	{
	    x = SCREENWIDTH;
	    do
	    {
		fouripixels = *ilineptr++;
		fouropixels[0] = (fouripixels & 0xff000000)
		    |	((fouripixels>>8) & 0xff0000)
		    |	((fouripixels>>16) & 0xffff);
		fouropixels[1] = ((fouripixels<<8) & 0xff000000)
		    |	(fouripixels & 0xffff00)
		    |	((fouripixels>>8) & 0xff);
		fouropixels[2] = ((fouripixels<<16) & 0xffff0000)
		    |	((fouripixels<<8) & 0xff00)
		    |	(fouripixels & 0xff);
#ifdef __BIG_ENDIAN__
		*olineptrs[0]++ = fouropixels[0];
		*olineptrs[1]++ = fouropixels[0];
		*olineptrs[2]++ = fouropixels[0];
		*olineptrs[0]++ = fouropixels[1];
		*olineptrs[1]++ = fouropixels[1];
		*olineptrs[2]++ = fouropixels[1];
		*olineptrs[0]++ = fouropixels[2];
		*olineptrs[1]++ = fouropixels[2];
		*olineptrs[2]++ = fouropixels[2];
#else
		*olineptrs[0]++ = fouropixels[2];
		*olineptrs[1]++ = fouropixels[2];
		*olineptrs[2]++ = fouropixels[2];
		*olineptrs[0]++ = fouropixels[1];
		*olineptrs[1]++ = fouropixels[1];
		*olineptrs[2]++ = fouropixels[1];
		*olineptrs[0]++ = fouropixels[0];
		*olineptrs[1]++ = fouropixels[0];
		*olineptrs[2]++ = fouropixels[0];
#endif
	    } while (x-=4);
	    olineptrs[0] += 2*X_width/4;
	    olineptrs[1] += 2*X_width/4;
	    olineptrs[2] += 2*X_width/4;
	}

  }
  else if (multiply == 4)
  {
	// Broken. Gotta fix this some day.
	void Expand4(unsigned *, double *);
	Expand4 ((unsigned *)(screens[0]), (double *) (image->data));
  }

  if (doShm)
  {

	if (!XShmPutImage(	X_display,
				X_mainWindow,
				X_gc,
				image,
				0, 0,
				0, 0,
				X_width, X_height,
				True ))
	    I_Error("XShmPutImage() failed\n");

	// wait for it to finish and processes all input events
	shmFinished = false;
	do
	{
	    I_GetEvent();
	} while (!shmFinished);

  }
  else
  {

	// draw the image
	XPutImage(	X_display,
			X_mainWindow,
			X_gc,
			image,
			0, 0,
			0, 0,
			X_width, X_height );

	// sync up with server
	XSync(X_display, False);

  }

}


//
//I_ReadScreen
//
void I_ReadScreen (byte* scr)
{
  memcpy (scr, screens[0], SCREENWIDTH*SCREENHEIGHT);
}


//
//Palette stuff.
//
static XColor	colors[256];

void UploadNewPalette(Colormap cmap, byte *palette)
{

  register int	i;
  register int	c;
  static boolean	firstcall = true;

#ifdef __cplusplus
  if (X_visualinfo.c_class == PseudoColor && X_visualinfo.depth == 8)
#else
  if (X_visualinfo.class == PseudoColor && X_visualinfo.depth == 8)
#endif
	{
	    // initialize the colormap
	    if (firstcall)
	    {
		firstcall = false;
		for (i=0 ; i<256 ; i++)
		{
		    colors[i].pixel = i;
		    colors[i].flags = DoRed|DoGreen|DoBlue;
		}
	    }

	    // set the X colormap entries
	    for (i=0 ; i<256 ; i++)
	    {
		c = gammatable[usegamma][*palette++];
		colors[i].red = (c<<8) + c;
		c = gammatable[usegamma][*palette++];
		colors[i].green = (c<<8) + c;
		c = gammatable[usegamma][*palette++];
		colors[i].blue = (c<<8) + c;
	    }

	    // store the colors to the current colormap
	    XStoreColors(X_display, cmap, colors, 256);

	}
}

//
//I_SetPalette
//
void I_SetPalette (byte* palette)
{
  UploadNewPalette(X_cmap, palette);
}


//
//This function is probably redundant,
//if XShmDetach works properly.
//ddt never detached the XShm memory,
//thus there might have been stale
//handles accumulating.
//

/*
void grabsharedmemory(int size)
{

int			key = ('d'<<24) | ('o'<<16) | ('o'<<8) | 'm';
struct shmid_ds	shminfo;
int			minsize = 320*200;
int			id;
int			rc;
// UNUSED int done=0;
int			pollution=5;

// try to use what was here before
do
{
  id = shmget((key_t) key, minsize, 0777); // just get the id
  if (id != -1)
  {
    rc=shmctl(id, IPC_STAT, &shminfo); // get stats on it
    if (!rc) 
    {
	if (shminfo.shm_nattch)
	{
	  fprintf(stderr, "User %d appears to be running "
		  "DOOM.  Is that wise?\n", shminfo.shm_cpid);
	  key++;
	}
	else
	{
	  if (getuid() == shminfo.shm_perm.cuid)
	  {
	    rc = shmctl(id, IPC_RMID, 0);
	    if (!rc)
	      fprintf(stderr,
		      "Was able to kill my old shared memory\n");
	    else
	      I_Error("Was NOT able to kill my old shared memory");
	    
	    id = shmget((key_t)key, size, IPC_CREAT|0777);
	    if (id==-1)
	      I_Error("Could not get shared memory");
	    
	    rc=shmctl(id, IPC_STAT, &shminfo);
	    
	    break;
	    
	  }
	  if (size >= shminfo.shm_segsz)
	  {
	    fprintf(stderr,
		    "will use %d's stale shared memory\n",
		    shminfo.shm_cpid);
	    break;
	  }
	  else
	  {
	    fprintf(stderr,
		    "warning: can't use stale "
		    "shared memory belonging to id %d, "
		    "key=0x%x\n",
		    shminfo.shm_cpid, key);
	    key++;
	  }
	}
    }
    else
    {
	I_Error("could not get stats on key=%d", key);
    }
  }
  else
  {
    id = shmget((key_t)key, size, IPC_CREAT|0777);
    if (id==-1)
    {
	extern int errno;
	fprintf(stderr, "errno=%d\n", errno);
	I_Error("Could not get any shared memory");
    }
    break;
  }
} while (--pollution);

if (!pollution)
{
  I_Error("Sorry, system too polluted with stale "
	    "shared memory segments.\n");
  }	

X_shminfo.shmid = id;

// attach to the shared memory segment
image->data = X_shminfo.shmaddr = shmat(id, 0, 0);

fprintf(stderr, "shared memory id=%d, addr=0x%x\n", id,
	  (int) (image->data));
}

/*

// MAES: was unsigned
int[] exptable= new int[256];

public void InitExpand ()
{
  int		i;
	
  for (i=0 ; i<256 ; i++)
	exptable[i] = i | (i<<8) | (i<<16) | (i<<24);
}

// Ehhh???? WTF is that for?

double[]		exptable2=new double[256*256];

public void InitExpand2 ()
{
  int		i;
  int		j;
  // UNUSED unsigned	iexp, jexp;
  double[]	exp;
  
  /*
  union
  {
	double 		d;
	unsigned	u[2];
  } pixel;
  */
  int u0,u1;
  
  System.out.print ("building exptable2...\n");
  exp = exptable2;
  int count=0;
  for (i=0 ; i<256 ; i++)
  {
	u0 = i | (i<<8) | (i<<16) | (i<<24);
	for (j=0 ; j<256 ; j++)
	{
	    u1 = j | (j<<8) | (j<<16) | (j<<24);
	    exp[count++] = Double.longBitsToDouble(u0|u1);
	}
  }
  System.out.print ("done.\n");
}

int	inited;
/*
void
Expand4
( unsigned*	lineptr,
double*	xline )
{
  double	dpixel;
  unsigned	x;
  unsigned 	y;
  unsigned	fourpixels;
  unsigned	step;
  double*	exp;
	
  exp = exptable2;
  if (!inited)
  {
	inited = 1;
	InitExpand2 ();
  }
		
		
  step = 3*SCREENWIDTH/2;
	
  y = SCREENHEIGHT-1;
  do
  {
	x = SCREENWIDTH;

	do
	{
	    fourpixels = lineptr[0];
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff0000)>>13) );
	    xline[0] = dpixel;
	    xline[160] = dpixel;
	    xline[320] = dpixel;
	    xline[480] = dpixel;
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff)<<3 ) );
	    xline[1] = dpixel;
	    xline[161] = dpixel;
	    xline[321] = dpixel;
	    xline[481] = dpixel;

	    fourpixels = lineptr[1];
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff0000)>>13) );
	    xline[2] = dpixel;
	    xline[162] = dpixel;
	    xline[322] = dpixel;
	    xline[482] = dpixel;
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff)<<3 ) );
	    xline[3] = dpixel;
	    xline[163] = dpixel;
	    xline[323] = dpixel;
	    xline[483] = dpixel;

	    fourpixels = lineptr[2];
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff0000)>>13) );
	    xline[4] = dpixel;
	    xline[164] = dpixel;
	    xline[324] = dpixel;
	    xline[484] = dpixel;
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff)<<3 ) );
	    xline[5] = dpixel;
	    xline[165] = dpixel;
	    xline[325] = dpixel;
	    xline[485] = dpixel;

	    fourpixels = lineptr[3];
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff0000)>>13) );
	    xline[6] = dpixel;
	    xline[166] = dpixel;
	    xline[326] = dpixel;
	    xline[486] = dpixel;
			
	    dpixel = *(double *)( (int)exp + ( (fourpixels&0xffff)<<3 ) );
	    xline[7] = dpixel;
	    xline[167] = dpixel;
	    xline[327] = dpixel;
	    xline[487] = dpixel;

	    lineptr+=4;
	    xline+=8;
	} while (x-=16);
	xline += step;
  } while (y--);
}
*/

}