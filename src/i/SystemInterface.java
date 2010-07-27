package i;

import doom.ticcmd_t;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: SystemInterface.java,v 1.1 2010/07/27 14:27:16 velktron Exp $
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
//	System specific interface stuff.
//
//-----------------------------------------------------------------------------

public interface SystemInterface {
	

	/** Called by DoomMain. 
	 * @return */
	public void Init();

	/** Called by startup code
	* to get the ammount of memory to malloc
	* for the zone management.*/
	public byte[]	ZoneBase (int size);


	/** Called by D_DoomLoop,
	 *returns current time in tics.
	 */
	public int GetTime ();


	/**
	 * Called by D_DoomLoop,
	 * called before processing any tics in a frame
	 * (just after displaying a frame).
	 * Time consuming syncronous operations
	 * are performed here (joystick reading).
	 * Can call D_PostEvent.
	 */
	
	public void StartFrame ();

	/** Called by D_DoomLoop,
	 * called before processing each tic in a frame.
	 * Quick syncronous operations are performed here.
	 * Can call D_PostEvent.
	 */
	public void StartTic ();

	// Asynchronous interrupt functions should maintain private queues
	// that are read by the synchronous functions
	// to be converted into events.

	// Either returns a null ticcmd,
	// or calls a loadable driver to build it.
	// This ticcmd will then be modified by the gameloop
	// for normal input.
	public ticcmd_t BaseTiccmd ();


	// Called by M_Responder when quit is selected.
	// Clean exit, displays sell blurb.
	public void Quit ();


	// 
	
	/** Allocates from low memory under dos,
	 * just mallocs under unix.
	 * Dummy function in Java. 
	 */
	
	public byte[] AllocLow (int length);

	public void Tactile (int on, int off, int total);

	public void Error (String error, String ... args);	
}
