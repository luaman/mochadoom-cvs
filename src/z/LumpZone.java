package z;

import i.IDoomSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Hashtable;

import w.CacheableDoomObject;
import w.lumpinfo_t;

import static data.Defines.*;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: LumpZone.java,v 1.1 2011/06/02 13:58:52 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This source is available for distribution and/or modification
// only under the terms of the DOOM Source Code License as
// published by id Software. All rights reserved.
//
// The source is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// FITNESS FOR A PARTICULAR PURPOSE. See the DOOM Source Code License
// for more details.
//
// $Log: LumpZone.java,v $
// Revision 1.1  2011/06/02 13:58:52  velktron
// Lame pseudo-zone system.
//
// Revision 1.6  2011/05/18 17:00:25  velktron
// Removed redundant/useless files.
//
// Revision 1.5  2010/11/12 13:37:25  velktron
// Rationalized the LUT system - now it's 100% procedurally generated.
//
// Revision 1.4  2010/09/23 20:36:45  velktron
// *** empty log message ***
//
// Revision 1.3  2010/09/23 15:11:57  velktron
// A bit closer...
//
// Revision 1.2  2010/08/13 14:06:36  velktron
// Endlevel screen fully functional!
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
// DESCRIPTION:
//	A crude way to force unloading of CacheableDoomObjects. Level-static
//  stuff will be garbage collected anyway so we don't need to worry about those.
//  Instead, we can force unloading of stuff read from disk such as textures,
//  sprites, etc. by nulling their references in the wadloader. Of course,
//  we need somewhere to keep track of all that stuff.
//
//-----------------------------------------------------------------------------

public class LumpZone implements IZone{

	public static String rcsid = "$Id: LumpZone.java,v 1.1 2011/06/02 13:58:52 velktron Exp $";

	Hashtable<CacheableDoomObject, Integer> zone;

	// This mirrors the one in the Wadloader.
	CacheableDoomObject[] disklumps; 

	public LumpZone(){
		zone= new Hashtable<CacheableDoomObject, Integer>();
	}

	@Override
	public void setLumps(CacheableDoomObject[] lumps){
		this.disklumps=lumps;
	}

	/** Add a lump to the tracking */

	public void Track(CacheableDoomObject lump, int index){
		zone.put(lump, index);
	}


	/** Null the disk lump associated with a particular object,
	 *  in any. This will NOT induce a garbage collection, unless
	 *  you also null any references you have to that object.  
	 * 
	 * @param lump
	 */
	public void Free(CacheableDoomObject lump){
		// Remove it from the reference
		Integer lumpno=zone.remove(lump);
		

		// Force nulling. This should trigger garbage collection,
		// and reclaim some memory, provided you also
		// nulled any other reference to a certain lump.
		// Therefore, make sure you null stuff right after
		// calling this method, if you want to make sure that they
		// won't be referenced anywhere else.
		if (lumpno!=null) {
			disklumps[lumpno]=null;
			System.out.printf("Lump %d %d freed\n",lump.hashCode(),lumpno);
		}
	}

}

//$Log: LumpZone.java,v $
//Revision 1.1  2011/06/02 13:58:52  velktron
//Lame pseudo-zone system.
//