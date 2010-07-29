package doom;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomInterface.java,v 1.1 2010/07/29 15:28:59 velktron Exp $
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
// $Log: DoomInterface.java,v $
// Revision 1.1  2010/07/29 15:28:59  velktron
// More work on menus...and digging some dependencies..
//
//
// DESCRIPTION:
//	System specific interface stuff.
//
//-----------------------------------------------------------------------------


public interface DoomInterface {
	
	public static final int MAXWADFILES=             20;
	//extern char*		wadfiles[MAXWADFILES];

	public void D_AddFile (String file);



	/**
	 * D_DoomMain()
	 * Not a globally visible function, just included for source reference,
	 * calls all startup code, parses command line options.
	 * If not overrided by user input, calls N_AdvanceDemo.
	 */
	public void DoomMain ();

	/** Called by IO functions when input is detected. */
	public void PostEvent (event_t ev);

		

	//
	// BASE LEVEL
	//
	public void PageTicker ();
	public void PageDrawer ();
	public void AdvanceDemo ();
	public void StartTitle ();


}
