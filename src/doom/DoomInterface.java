package doom;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomInterface.java,v 1.3 2010/09/23 15:11:57 velktron Exp $
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
// Revision 1.3  2010/09/23 15:11:57  velktron
// A bit closer...
//
// Revision 1.2  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.1  2010/07/29 15:28:59  velktron
// More work on menus...and digging some dependencies..
//
//
// DESCRIPTION:
//	System specific interface stuff.
//
//-----------------------------------------------------------------------------


public interface DoomInterface {


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
