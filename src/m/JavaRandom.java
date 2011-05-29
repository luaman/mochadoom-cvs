package m;

import java.util.Random;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: JavaRandom.java,v 1.1 2011/05/29 22:15:32 velktron Exp $
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
// $Log: JavaRandom.java,v $
// Revision 1.1  2011/05/29 22:15:32  velktron
// Introduced IRandom interface.
//
// Revision 1.4  2010/09/22 16:40:02  velktron
// MASSIVE changes in the status passing model.
// DoomMain and DoomGame unified.
// Doomstat merged into DoomMain (now status and game functions are one).
//
// Most of DoomMain implemented. Possible to attempt a "classic type" start but will stop when reading sprites.
//
// Revision 1.3  2010/09/10 17:35:49  velktron
// DoomGame, Menu, renderers
//
// Revision 1.2  2010/07/06 16:32:38  velktron
// Threw some work in WI, now EndLevel. YEAH THERE'S GONNA BE A SEPARATE EndLevel OBJECT THAT'S HOW PIMP THE PROJECT IS!!!!11!!!
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
//	Random number LUT.
//
//-----------------------------------------------------------------------------

public class JavaRandom implements IRandom{

protected int	rndindex = 0;
protected int	prndindex = 0;

// Which one is deterministic?
public int P_Random ()
{
	rndindex++;
	return (0xFF&r.nextInt());
	
}

public int M_Random ()
{
	prndindex++;
	return (0xFF&m.nextInt());
}

public void ClearRandom ()
{
	rndindex=prndindex=0;
    r.setSeed(666);
}

public JavaRandom(){
	r=new Random(666);
	m=new Random(666);
    this.ClearRandom();
}

public int getIndex(){
	return rndindex;
}

private Random r;
private Random m;

}




