// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomSystem.java,v 1.3 2010/09/24 17:58:39 velktron Exp $
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
// $Log: DoomSystem.java,v $
// Revision 1.3  2010/09/24 17:58:39  velktron
// Menus and HU  functional -mostly.
//
// Revision 1.2  2010/09/23 20:36:45  velktron
// *** empty log message ***
//
// Revision 1.1  2010/09/23 15:11:57  velktron
// A bit closer...
//
// Revision 1.3  2010/09/07 16:23:00  velktron
// *** empty log message ***
//
// Revision 1.2  2010/08/30 15:53:19  velktron
// Screen wipes work...Finale coded but untested.
// GRID.WAD included for testing.
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
//
//-----------------------------------------------------------------------------

package i;

import doom.DoomInterface;
import doom.DoomMain;
import doom.event_t;
import doom.ticcmd_t;
import utils.PrintfFormat;
import static data.Defines.TICRATE;

public class DoomSystem implements DoomSystemInterface{
 


static int	mb_used = 6;

// Even the SYSTEM needs to know about DOOM!!!!
protected DoomMain DM;

@Override
public void
Tactile
( int	on,
  int	off,
  int	total )
{
  // UNUSED.
  on = off = total = 0;
}


public ticcmd_t	emptycmd;

@Override
public ticcmd_t	BaseTiccmd()
{
    return emptycmd;
}


@Override
public int  GetHeapSize ()
{
    return mb_used*1024*1024;
}

@Override
public byte[] ZoneBase (int	size)
{
    return (new byte[mb_used*1024*1024]);
}




protected long basetime=0;

/**
 * I_GetTime
 * returns time in 1/70th second tics
 */

@Override
public int  GetTime ()
{
    long	tp;
    //struct timezone	tzp;
    int			newtics;
    tp=System.nanoTime();
    if (basetime==0)
	basetime = tp;
    newtics = (int) (((tp-basetime)*TICRATE)/1000000000);// + tp.tv_usec*TICRATE/1000000;
    return newtics;
}

//
//I_Quit
//
@Override
public void Quit ()
{
    // TODO:
 //DM.QuitNetGame ();
 //I_ShutdownSound();
 //I_ShutdownMusic();
 //M.SaveDefaults ();
 //I_ShutdownGraphics();
 System.exit(0);
}


/**
 * I_Init
 */
@Override
public void Init ()
{
    //TODO: InitSound();
    //TODO: InitGraphics();
}


@Override
public void WaitVBL(int count)
{
    try {
        Thread.sleep(count*1000/70);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }                                
}
@Override
public void BeginRead()
{
}

@Override
public void EndRead()
{
}

@Override
public void	AllocLow(int length)
{
 ; // Dummy
}

//
// I_Error
//
@Override
public void Error (String error, Object ... args)
{
    //va_list	argptr;

    // Message first.
    //va_start (argptr,error);
    System.err.print("Error: ");
    System.err.print(new PrintfFormat(error).sprintf(args));
    System.err.print("\n");
    //va_end (argptr);

    //fflush( stderr );

    // Shutdown. Here might be other errors.
    //if (demorecording)
	//G_CheckDemoStatus();

    //D_QuitNetGame ();
    //I_ShutdownGraphics();
    
    System.exit(-1);
}

@Override
public void Error (String error)
{
    //va_list	argptr;

    // Message first.
    //va_start (argptr,error);
    System.err.print("Error: ");
    System.err.print(new PrintfFormat(error).sprintf());
    System.err.print("\n");
    //va_end (argptr);

    //fflush( stderr );

    // Shutdown. Here might be other errors.
    //if (demorecording)
	//G_CheckDemoStatus();

    //D_QuitNetGame ();
    //I_ShutdownGraphics();
    
    System.exit(-1);
}

public DoomSystem(){
emptycmd=new ticcmd_t();
}

}