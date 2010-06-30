package m;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: bbox.java,v 1.1 2010/06/30 08:58:50 velktron Exp $
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
// $Log: bbox.java,v $
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
//	Random number LUT.
//	Default Config File.
//	PCX Screenshots.
//
//-----------------------------------------------------------------------------
// static const char rcsid[] = "$Id: bbox.java,v 1.1 2010/06/30 08:58:50 velktron Exp $";

import static data.doomtype.*;

public class bbox {

	public static final int BOXTOP = 0;
	public static final int BOXBOTTOM = 1;
	public static final int BOXLEFT = 2;
	public static final int BOXRIGHT = 3;
	fixed_t[] pts;

	// Points of the bbox as an object

	public bbox() {
		pts = new fixed_t[4];
	}

	// Static method

	public static void ClearBox(fixed_t[] box) {
		box[BOXRIGHT].set(MININT);
		box[BOXTOP].set(MININT);
		box[BOXLEFT].set(MAXINT);
		box[BOXBOTTOM].set(MAXINT);
	}

	// Instance method

	public void ClearBox() {
		pts[BOXRIGHT].set(MININT);
		pts[BOXTOP].set(MININT);
		pts[BOXLEFT].set(MAXINT);
		pts[BOXBOTTOM].set(MAXINT);
	}

	public static void AddToBox(fixed_t[] box, fixed_t x, fixed_t y) {
		if (x.compareTo(box[BOXLEFT]) < 0)
			box[BOXLEFT].copy(x);
		else if (x.compareTo(box[BOXRIGHT]) > 0)
			box[BOXRIGHT].copy(x);
		if (y.compareTo(box[BOXBOTTOM]) < 0)
			box[BOXBOTTOM] = y;
		else if (y.compareTo(box[BOXTOP]) > 0)
			box[BOXTOP] = y;
	}

	public void AddToBox(fixed_t x, fixed_t y) {
		if (x.compareTo(pts[BOXLEFT]) < 0)
			pts[BOXLEFT].copy(x);
		else if (x.compareTo(pts[BOXRIGHT]) > 0)
			pts[BOXRIGHT].copy(x);
		if (y.compareTo(pts[BOXBOTTOM]) < 0)
			pts[BOXBOTTOM] = y;
		else if (y.compareTo(pts[BOXTOP]) > 0)
			pts[BOXTOP] = y;
	}

	/**
	 * MAES: Keeping with C's type (in)consistency, we also allow to input ints
	 * -_-
	 * 
	 * @param x
	 * @param y
	 */
	public void AddToBox(int x, int y) {
		if (x < pts[BOXLEFT].val)
			pts[BOXLEFT].set(x);
		if (x > pts[BOXRIGHT].val)
			pts[BOXRIGHT].set(x);
		if (y < pts[BOXBOTTOM].val)
			pts[BOXBOTTOM].set(y);
		if (y > pts[BOXTOP].val)
			pts[BOXTOP].set(y);
	}

	/**
	 * R_AddPointToBox Expand a given bbox so that it encloses a given point.
	 * 
	 * @param x
	 * @param y
	 * @param box
	 */

	public static void AddPointToBox(int x, int y, fixed_t[] box) {
		if (x < box[BOXLEFT].val)
			box[BOXLEFT].set(x);
		if (x > box[BOXRIGHT].val)
			box[BOXRIGHT].set(x);
		if (y < box[BOXBOTTOM].val)
			box[BOXBOTTOM].set(y);
		if (y > box[BOXTOP].val)
			box[BOXTOP].set(y);
	}

	/**
	 * R_AddPointToBox Expand this bbox so that it encloses a given point.
	 * 
	 * @param x
	 * @param y
	 * @param box
	 */

	public void AddPointToBox(int x, int y) {
		if (x < pts[BOXLEFT].val)
			pts[BOXLEFT].set(x);
		if (x > pts[BOXRIGHT].val)
			pts[BOXRIGHT].set(x);
		if (y < pts[BOXBOTTOM].val)
			pts[BOXBOTTOM].set(y);
		if (y > pts[BOXTOP].val)
			pts[BOXTOP].set(y);
	}

}
