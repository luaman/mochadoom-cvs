// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: WadLoader.java,v 1.25 2010/12/22 01:23:15 velktron Exp $
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
// $Log: WadLoader.java,v $
// Revision 1.25  2010/12/22 01:23:15  velktron
// Definitively fixed plain DrawColumn.
// Fixed PATCH/TEXTURE and filelump/wadloader capitalization.
// Brought back some testers.
//
// Revision 1.24  2010/12/14 17:55:59  velktron
// Fixed weapon bobbing, added translucent column drawing, separated rendering commons.
//
// Revision 1.23  2010/12/13 16:03:20  velktron
// More fixes  in the wad loading code
//
// Revision 1.22  2010/12/12 21:27:17  velktron
// Fixed hashtable bug. Now using Java's one, faster AND easier to follow.
//
// Revision 1.21  2010/10/08 16:55:50  velktron
// Duh
//
// Revision 1.20  2010/09/27 02:27:29  velktron
// BEASTLY update
//
// Revision 1.19  2010/09/24 17:58:39  velktron
// Menus and HU  functional -mostly.
//
// Revision 1.18  2010/09/23 20:36:45  velktron
// *** empty log message ***
//
// Revision 1.17  2010/09/23 15:11:57  velktron
// A bit closer...
//
// Revision 1.16  2010/09/22 16:40:02  velktron
// MASSIVE changes in the status passing model.
// DoomMain and DoomGame unified.
// Doomstat merged into DoomMain (now status and game functions are one).
//
// Most of DoomMain implemented. Possible to attempt a "classic type" start but will stop when reading sprites.
//
// Revision 1.15  2010/09/13 15:39:17  velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.14  2010/09/09 01:13:19  velktron
// MUCH better rendering and testers.
//
// Revision 1.13  2010/09/07 16:23:00  velktron
// *** empty log message ***
//
// Revision 1.12  2010/09/03 15:30:34  velktron
// More work on unified renderer
//
// Revision 1.11  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.10  2010/08/30 15:53:19  velktron
// Screen wipes work...Finale coded but untested.
// GRID.WAD included for testing.
//
// Revision 1.9  2010/08/23 14:36:08  velktron
// Menu mostly working, implemented Killough's fast hash-based GetNumForName, although it can probably be finetuned even more.
//
// Revision 1.8  2010/08/13 14:06:36  velktron
// Endlevel screen fully functional!
//
// Revision 1.7  2010/08/11 16:31:34  velktron
// Map loading works! Check out LevelLoaderTester for more.
//
// Revision 1.6  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
// Revision 1.5  2010/07/22 15:37:53  velktron
// MAJOR changes in Menu system.
//
// Revision 1.4  2010/07/15 14:01:49  velktron
// Added reflector Method stuff for function pointers.
//
// Revision 1.3  2010/07/06 15:20:23  velktron
// Several changes in the WAD loading routine. Now lumps are directly unpacked as "CacheableDoomObjects" and only defaulting will result in "raw" DoomBuffer reads.
//
// Makes caching more effective.
//
// Revision 1.2 2010/06/30 11:44:40 velktron
// Added a tester for patches (one of the most loosely-coupled structs in Doom!)
// and fixed some minor stuff all around.
//
// Revision 1.1 2010/06/30 08:58:50 velktron
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
// DESCRIPTION:
// Handles WAD file header, directory, lump I/O.
//
// -----------------------------------------------------------------------------

package w;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import rr.patch_t;

import utils.C2JUtils;
import z.memblock_t;

import static data.Defines.*;
import i.*;

public class WadLoader {

	DoomSystemInterface I;

	//
	// GLOBALS
	//

	/** Location of each lump on disk. */
	public lumpinfo_t[] lumpinfo;

	public WadLoader(DoomSystemInterface I) {
		this();
		this.I = I;
	}

	public WadLoader() {
		// this.I=I;
		lumpinfo = new lumpinfo_t[0];
	}

	public int numlumps;

	/**
	 * MAES: probably array of byte[]??? void** lumpcache;
	 * 
	 * Actually, loaded objects will be deserialized here as the general type
	 * "CacheableDoomObject" (in the worst case they will be byte[] or
	 * ByteBuffer).
	 * 
	 * Not to brag, but this system is FAR superior to the inline unmarshaling
	 * used in other projects ;-)
	 */

	private CacheableDoomObject[] lumpcache;

	private boolean[] preloaded;

	/**
	 * #define strcmpi strcasecmp MAES: this is just capitalization. However we
	 * can't manipulate String object in Java directly like this, so this must
	 * be a return type.
	 * 
	 * TODO: maybe move this in utils?
	 */

	public String strupr(String s) {
		return s.toUpperCase();
	}

	/* ditto */
	public void strupr(char[] s) {
		for (int i = 0; i < s.length; i++) {
			s[i] = Character.toUpperCase(s[i]);
		}
	}

	/** Maes: File intead of "inthandle" */

	public long filelength(File handle) {
		try {/*
			 * stat fileinfo; if (fstat (handle,&fileinfo) == -1) I_Error
			 * ("Error fstating");
			 */

			return handle.length();
		} catch (Exception e) {
			I.Error("Error fstating");
			return -1;
		}

	}

	/**
	 * MAES: same problem here. I'd rather return a new String than doing stupid
	 * shit like passing an array of Strings as dest and modifying the first
	 * item.
	 */

	protected String ExtractFileBase(String path) {
		StringBuffer dest = new StringBuffer(8);

		int length = 0;
		int src = path.length() - 1;
		// Duh???

		// back up until a \ or the start
		// MAES: hideously not OO or cross-platform :-p
		/*
		 * while ((src >=0) && path.charAt(src) != '\\' && path.charAt(src) !=
		 * '/') { src--; }
		 */

		// Maes: better use this, I think.

		src = path.lastIndexOf('\\');
		if (src < 0)
			src = path.lastIndexOf('/');
		if (src < 0)
			src = 0;

		// copy UP to eight characters.
		int pos = 0;
		while ((pos < path.length()) && (path.charAt(pos) != '.')) {
			if (++length == 9)
				I.Error("Filename base of %s >8 chars", path);

			dest.append(path.charAt(pos));
			pos++;
		}
		// Uppercase ALL strings used in the retrieval system.
		return new String(dest.substring(0,pos).toUpperCase());
	}

	//
	// LUMP BASED ROUTINES.
	//

	//
	// W_AddFile
	// All files are optional, but at least one file must be
	// found (PWAD, if all required lumps are present).
	// Files with a .wad extension are wadlink files
	// with multiple lumps.
	// Other files are single lumps with the base filename
	// for the lump name.
	//
	// If filename starts with a tilde, the file is handled
	// specially to allow map reloads.
	// But: the reload feature is a fragile hack...

	int reloadlump;

	// MAES: was char*
	String reloadname;

	/**
	 * This is where lumps are actually read + loaded from a file.
	 * 
	 * @param filename
	 * @throws Exception
	 */

	public void AddFile(String filename) throws Exception {
		wadinfo_t header = new wadinfo_t();
		int lump_p; // MAES: was lumpinfo_t* , but we can use it as an array
		// pointer.
		DoomFile handle;
		long length;
		int startlump;
		filelump_t[] fileinfo = new filelump_t[1]; // MAES: was *
		filelump_t singleinfo = new filelump_t();
		DoomFile storehandle;

		// open the file and add to directory

		// handle reload indicator.
		if (filename.charAt(0) == '~') {
			filename = filename.substring(1);
			reloadname = filename;
			reloadlump = numlumps;
		}

		try {
			handle = new DoomFile(filename, "r");
		} catch (Exception e) {
			I.Error(" couldn't open %s \n", filename);
			return;
		}

		// System.out.println(" adding " + filename + "\n");

		// We start at the number of lumps. This allows appending stuff.
		startlump = this.numlumps;

		if (filename.substring(filename.length() - 3)
				.compareToIgnoreCase("wad") != 0) {
		    System.out.println("Single lump");
			// single lump file
			fileinfo[0] = singleinfo;
			singleinfo.filepos = 0;
			singleinfo.size = (long) (handle.length());
			singleinfo.name = ExtractFileBase(filename);
			numlumps++;
		} else {
			// MAES: 14/06/10 this is historical, for this is the first time I
			// implement reading something
			// from RAF into Doom's structs. Kudos to the JAKE2 team who solved
			// this problem before me.
			// Check out how reading is delegated to each class's "load" method.
			// read (handle, &header, sizeof(header));

			header.load(handle);

			if (header.identification.compareTo("IWAD") != 0) {
				// Homebrew levels?
				if (header.identification.compareTo("PWAD") != 0) {
					I.Error("Wad file %s doesn't have IWAD or PWAD id\n",
							filename);
				}

				// modifiedgame = true;
			}

			// MAES: I don't think the following are needed. Casting to long?
			// :-S
			// header.numlumps = header.numlumps;
			// header.infotableofs = header.infotableofs;

			length = header.numlumps;
			fileinfo = new filelump_t[(int) length];
			// Init everything:
			C2JUtils.initArrayOfObjects(fileinfo, filelump_t.class);

			handle.seek(header.infotableofs);
			// MAES: we can't read raw structs here, and even less BLOCKS of
			// structs.

			handle.readObjectArray(fileinfo, (int) length);

			/*
			 * for (int j=0;j<length;j++){ fileinfo[j].load (handle); }
			 */
			numlumps += header.numlumps;
		    } // end loading wad
		
			// Fill in lumpinfo
			// MAES: this was a realloc(lumpinfo, numlumps*sizeof(lumpinfo_t)),
			// so we have to increase size and copy over. Maybe this should be
			// an ArrayList?

			int oldsize = lumpinfo.length;
			lumpinfo_t[] newlumpinfo = new lumpinfo_t[numlumps];

			try {
				C2JUtils.initArrayOfObjects(newlumpinfo, lumpinfo_t.class);
				System.arraycopy(lumpinfo, 0, newlumpinfo, 0, oldsize);
			} catch (Exception e) {
				// if (!lumpinfo)
				I.Error("Couldn't realloc lumpinfo");
			}

			// Bye bye, old lumpinfo!
			lumpinfo = newlumpinfo;

			// MAES: lump_p was an alias for lumpinfo[startlump]. I know it's a
			// bit crude as an approximation but heh...

			lump_p = startlump;

			// MAES: if reloadname is null, handle is stored...else an invalid
			// handle?
			storehandle = (reloadname != null) ? null : handle;

			// This iterates through single files.
			int fileinfo_p = 0;

			for (int i = startlump; i < numlumps; i++, lump_p++, fileinfo_p++) {
				lumpinfo[lump_p].handle = storehandle;
				lumpinfo[lump_p].position = fileinfo[fileinfo_p].filepos;
				lumpinfo[lump_p].size = fileinfo[fileinfo_p].size;
				// Make all lump names uppercase. Searches should also be uppercase only.
				lumpinfo[lump_p].name = fileinfo[fileinfo_p].name.toUpperCase();
				lumpinfo[lump_p].hash =lumpinfo[lump_p].name.hashCode();
				lumpinfo[lump_p].stringhash = name8
						.getLongHash(strupr(lumpinfo[lump_p].name));
				// LumpNameHash(lumpinfo[lump_p].name);
				lumpinfo[lump_p].intname = name8
						.getIntName(strupr(lumpinfo[lump_p].name));
				System.out.println(lumpinfo[lump_p]);
			}
			if (reloadname != null)
				handle.close();
		
	}

	/**
	 * W_Reload Flushes any of the reloadable lumps in memory and reloads the
	 * directory.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("null")
	public void Reload() throws Exception {
		wadinfo_t header = new wadinfo_t();
		int lumpcount;
		int lump_p; // Maes: same as in W_WADload
		int i;
		DoomFile handle = null;
		int length;
		filelump_t[] fileinfo;

		if (reloadname == null)
			return;

		try {
			handle = new DoomFile(reloadname, "r");
		} catch (Exception e) {
			I.Error("W_Reload: couldn't open %s", reloadname);
		}

		header.load(handle);
		// Actual number of lumps in file...
		lumpcount = (int) header.numlumps;
		header.infotableofs = header.infotableofs;
		length = lumpcount;
		fileinfo = new filelump_t[length];
		handle.seek(header.infotableofs);

		// MAES: we can't read raw structs here, and even less BLOCKS of
		// structs.

		handle.readObjectArrayWithReflection(fileinfo, (int) length);

		/*
		 * for (int j=0;j<length;j++){ fileinfo[j].load (handle); }
		 */

		// numlumps += header.numlumps;
		// read (handle, fileinfo, length);

		// Fill in lumpinfo
		lump_p = reloadlump;
		int fileinfo_p = 0;
		for (i = reloadlump; i < reloadlump + lumpcount; i++, lump_p++, fileinfo_p++) {
			if (lumpcache[i] != null) {
				// That's like "freeing" it, right?
				lumpcache[i] = null;
				preloaded[i] = false;
			}

			lumpinfo[lump_p].position = fileinfo[fileinfo_p].filepos;
			lumpinfo[lump_p].size = fileinfo[fileinfo_p].size;
		}

	}

	/**
	 * W_InitMultipleFiles
	 *
	 * Pass a null terminated list of files to use (actually
	 * a String[] array in Java).
	 * 
	 * All files are optional, but at least one file
	 * must be found. 
	 * 
	 * Files with a .wad extension are idlink files
	 * with multiple lumps.
	 * 
	 * Other files are single lumps with the base filename
	 * for the lump name.
	 * 
	 * Lump names can appear multiple times.
	 * The name searcher looks backwards, so a later file
	 * does override all earlier ones.
	 * 
	 * @param filenames
	 * 
	 */
	
	public void InitMultipleFiles(String[] filenames) throws Exception {
		int size;

		// open all the files, load headers, and count lumps
		numlumps = 0;

		// will be realloced as lumps are added
		lumpinfo = new lumpinfo_t[0];

		for (String s : filenames) {
			if (s != null)
				this.AddFile(s);
		}

		if (numlumps == 0)
			I.Error("W_InitFiles: no files found");

		// set up caching
		size = numlumps;
		lumpcache = new CacheableDoomObject[size];
		preloaded = new boolean[size];

		if (lumpcache == null)
			I.Error("Couldn't allocate lumpcache");

		this.InitLumpHash();
	}

	/**
	 * W_InitFile
	 * 
	 * Just initialize from a single file.
	 * 
	 * @param filename 
	 * 
	 */
	public void InitFile(String filename) throws Exception {
		String[] names = new String[1];

		names[0] = filename;
		// names[1] = null;
		InitMultipleFiles(names);
	}

	/**
	 * W_NumLumps
	 * 
	 * Returns the total number of lumps loaded in this Wad manager. Awesome. 
	 * 
	 */
	public int NumLumps() {
		return numlumps;
	}

	/**
	 * W_CheckNumForName2 Returns -1 if name not found.
	 * 
	 * A slightly better implementation, uses string hashes
	 * as direct comparators (though 64-bit long descriptors
	 * could be used). It's faster than the old method, but
	 * still short from the Hashtable's performance by 
	 * an order of magnitude. 
	 * 
     * @param name
     * @return
	 */

	public int CheckNumForName2(String name) {

		// scan backwards so patch lump files take precedence
		int lump_p = numlumps;

		// make the name into two integers for easy compares
		// case insensitive

		long hash = name8.getLongHash(name);
		// System.out.print("Looking for "+name + " with hash "
		// +Long.toHexString(hash));
		while (lump_p-- != 0)
			if (lumpinfo[lump_p].stringhash == hash) {
				// System.out.print(" found "+lumpinfo[lump_p]+"\n" );
				return lump_p;
			}

		// TFB. Not found.
		return -1;
	}

	/**
	 * Old, shitty method for CheckNumForName. It's an overly literal
	 * translation of how the C original worked, which was none too good 
	 * even without the overhead of converting a string to
	 * its integer representation. It's so bad, that it's two orders
	 * of magnitude slower than a Hashtable implemetation, and one from
	 * a direct hash/longname comparison with linear search.
	 * 
	 * @param name
	 * @return
	 */

	public int CheckNumForName3(String name) {

		int v1;
		int v2;
		// lumpinfo_t lump_p;

		int lump_p;
		// make the name into two integers for easy compares
		// case insensitive
		name8 union = new name8(strupr(name));

		v1 = union.x[0];
		v2 = union.x[1];

		// scan backwards so patch lump files take precedence
		lump_p = numlumps;

		while (lump_p-- != 0) {
			int a = name8.stringToInt(lumpinfo[lump_p].name, 0);
			int b = name8.stringToInt(lumpinfo[lump_p].name, 4);
			if ((a == v1) && (b == v2)) {
				return lump_p;
			}
		}

		// TFB. Not found.
		return -1;
	}

	/**
	 * Returns actual lumpinfo_t object for a given name. Useful if you want to
	 * access something on a file, I guess?
	 * 
	 * @param name
	 * @return
	 */

	public lumpinfo_t GetLumpinfoForName(String name) {

		int v1;
		int v2;
		// lumpinfo_t lump_p;

		int lump_p;
		// make the name into two integers for easy compares
		// case insensitive
		name8 union = new name8(strupr(name));

		v1 = union.x[0];
		v2 = union.x[1];

		// scan backwards so patch lump files take precedence
		lump_p = numlumps;

		while (lump_p-- != 0) {
			int a = name8.stringToInt(lumpinfo[lump_p].name, 0);
			int b = name8.stringToInt(lumpinfo[lump_p].name, 4);
			if ((a == v1) && (b == v2)) {
				return lumpinfo[lump_p];
			}
		}

		// TFB. Not found.
		return null;
	}

	//
	// W_GetNumForName
	// Calls W_CheckNumForName, but bombs out if not found.
	//
	public int GetNumForName(String name) {
		int i;

		i = CheckNumForName(name);

		if (i == -1) {
			Exception e = new Exception();
			e.printStackTrace();
			System.err.println("Error:" + name + "not found");
			System.err.println("Hash:"
					+ Long.toHexString(name8.getLongHash(name)));
			I.Error("W_GetNumForName: %s not found!", name);
		}

		return i;
	}

	//
	// W_LumpLength
	// Returns the buffer size needed to load the given lump.
	//
	public int LumpLength(int lump) {
		if (lump >= numlumps)
			I.Error("W_LumpLength: %i >= numlumps", lump);

		return (int) lumpinfo[lump].size;
	}

	/**
	 * W_ReadLump Loads the lump into the given buffer, which must be >=
	 * W_LumpLength().
	 * 
	 * @throws IOException
	 */

	public void ReadLump(int lump, ByteBuffer dest) {
		int c;
		lumpinfo_t l; // Maes: was *..probably not array.
		DoomFile handle = null;

		if (lump >= this.numlumps) {
			I.Error("W_ReadLump: %i >= numlumps", lump);
			return;
		}

		l = lumpinfo[lump];

		// ??? I_BeginRead ();

		if (l.handle == null) {
			// reloadable file, so use open / read / close
			try {
				handle = new DoomFile(this.reloadname, "r");
			} catch (Exception e) {
				e.printStackTrace();
				I.Error("W_ReadLump: couldn't open %s", reloadname);
			}
		} else
			handle = l.handle;

		try {
			handle.seek(l.position);
			byte[] buf = new byte[(int) l.size];
			c = handle.read(buf);
			dest.put(buf);

			if (c < l.size)
				I.Error("W_ReadLump: only read %i of %i on lump %i", c, l.size,
						lump);

			if (l.handle == null)
				handle.close();

			// ??? I_EndRead ();
		} catch (Exception e) {
			I.Error("W_ReadLump: could not read lump " + lump);
			e.printStackTrace();
		}

	}

	/**
	 * W_CacheLumpNum Modified to read a lump as a specific type of
	 * CacheableDoomObject. If the class is not identified or is null, then a
	 * generic DoomBuffer object is left in the lump cache and returned.
	 */
	public CacheableDoomObject CacheLumpNum(int lump, int tag, Class what) {
		// byte* ptr;

		if (lump >= numlumps) {
			I.Error("W_CacheLumpNum: %i >= numlumps", lump);
		}

		// Nothing cached here...
		if ((lumpcache[lump] == null)) {

			// read the lump in

			// System.out.println("cache miss on lump "+lump);
			// ptr = Z_Malloc (W_LumpLength (lump), tag, &lumpcache[lump]);
			// Read as a byte buffer anyway.
			ByteBuffer thebuffer = ByteBuffer.allocate(this.LumpLength(lump));
			ReadLump(lump, thebuffer);

			// Class type specified

			if (what != null) {
				try {
					// Can it be uncached? If so, deserialize it.

					if (implementsInterface(what, w.CacheableDoomObject.class)) {
						lumpcache[lump] = (CacheableDoomObject) what
								.newInstance();

						((CacheableDoomObject) lumpcache[lump])
								.unpack((ByteBuffer) thebuffer);

						if (what == patch_t.class) {
							((patch_t) lumpcache[lump]).name = this.lumpinfo[lump].name;
						}
						// replace lump with parsed object.
					} else {
						lumpcache[lump] = (CacheableDoomObject) thebuffer;
					}
				} catch (Exception e) {
					System.err.println("Could not auto-instantiate lump "
							+ lump + " of class " + what);
					e.printStackTrace();
				}

			} else {
				// Class not specified? Then gimme a containing DoomBuffer!
				DoomBuffer db = new DoomBuffer(thebuffer);
				lumpcache[lump] = db;
			}
		} else {
			// System.out.println("cache hit on lump " + lump);
			// Z.ChangeTag (lumpcache[lump],tag);
		}

		return lumpcache[lump];
	}

	/**
	 * Read a lump into an object array, if possible. The binary blob lump will
	 * still be cached as usual, but as a ByteBuffer this time, and
	 * deserialization will be performed into the given Object[] array.
	 * 
	 * Upon a cache hit however, the objects will be deserialized a second time,
	 * thus there will be some time penalty (this, unless I devise a container
	 * class for arrays of CacheableDoomObjects).
	 * 
	 * Helps keep syntax compact.
	 * 
	 */
	public void CacheLumpNumIntoArray(int lump, int tag, Object[] array,
			Class what) throws IOException {

		if (lump >= numlumps) {
			I.Error("W_CacheLumpNum: %i >= numlumps", lump);
		}

		// Nothing cached here...
		if ((lumpcache[lump] == null)) {

			// read the lump in

			System.out.println("cache miss on lump " + lump);
			// Read as a byte buffer anyway.
			ByteBuffer thebuffer = ByteBuffer.allocate(this.LumpLength(lump));
			ReadLump(lump, thebuffer);
			// Store the buffer anyway (as a DoomBuffer)
			lumpcache[lump] = new DoomBuffer(thebuffer);

		} else {
			System.out.println("cache hit on lump " + lump);
			// Z.ChangeTag (lumpcache[lump],tag);
		}

		// Class type specified. If the previously cached stuff is a
		// "DoomBuffer" we can go on.

		if ((what != null) && (lumpcache[lump].getClass() == DoomBuffer.class)) {
			try {
				// Can it be uncached? If so, deserialize it. FOR EVERY OBJECT.
				ByteBuffer b = ((DoomBuffer) (lumpcache[lump])).getBuffer();
				b.rewind();

				for (int i = 0; i < array.length; i++) {
					if (implementsInterface(what, w.CacheableDoomObject.class)) {
						((CacheableDoomObject) array[i]).unpack(b);
					}
				}
				// lumpcache[lump]=array;
			} catch (Exception e) {
				System.err.println("Could not auto-unpack lump " + lump
						+ " into an array of objects of class " + what);
				e.printStackTrace();
			}

		}

		return;
	}

	/** Tells us if a class implements a certain interface.
	 *  If you know of a better way, be my guest.
	 * 
	 * @param what
	 * @param which
	 * @return
	 */
	
	protected boolean implementsInterface(Class what, Class which) {
		Class[] shit = what.getInterfaces();
		for (int i = 0; i < shit.length; i++) {
			if (shit[i].equals(which))
				return true;
		}
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Return a cached lump based on its name, as raw bytes, no matter what.
	 * It's rare, but has its uses.
	 * 
	 * @param name
	 * @param tag
	 * @param what
	 * @return
	 */

	public byte[] CacheLumpNameAsRawBytes(String name, int tag) {
		return ((DoomBuffer) this.CacheLumpNum(this.GetNumForName(name), tag,
				null)).getBuffer().array();

	}
	
	 /**
     * Return a cached lump based on its num, as raw bytes, no matter what.
     * It's rare, but has its uses.
     * 
     * @param name
     * @param tag
     * @param what
     * @return
     */

    public byte[] CacheLumpNumAsRawBytes(int num, int tag) {
        return ((DoomBuffer) this.CacheLumpNum(num, tag,
                null)).getBuffer().array();

    }
	

	/**
	 * Return a cached lump based on its name, and for a specificc class. This
	 * will autoload it too, and should be the preferred method of loading
	 * stuff.
	 * 
	 * @param name
	 * @param tag
	 * @param what
	 * @return
	 */

	public DoomBuffer CacheLumpName(String name, int tag) {
		return (DoomBuffer) this.CacheLumpNum(this.GetNumForName(name), tag,
				DoomBuffer.class);

	}

	/**
	 * Specific method for loading cached patches by name, since it's by FAR the
	 * most common operation.
	 * 
	 * @param name
	 * @return
	 */

	public patch_t CachePatchName(String name) {

		return (patch_t) this.CacheLumpNum(this.GetNumForName(name), PU_CACHE,
				patch_t.class);

	}

	/**
	 * Specific method for loading cached patches, since it's by FAR the most
	 * common operation.
	 * 
	 * @param name
	 * @param tag
	 * @return
	 */

	public patch_t CachePatchName(String name, int tag) {
		return (patch_t) this.CacheLumpNum(this.GetNumForName(name), tag,
				patch_t.class);
	}

	/**
	 * Specific method for loading cached patches by number.
	 * 
	 * @param name
	 * @return
	 */

	public patch_t CachePatchNum(int num, int tag) {
		return (patch_t) this.CacheLumpNum(num, tag, patch_t.class);
	}

	public Object CacheLumpName(String name, int tag, Class what) {
		return this.CacheLumpNum(this.GetNumForName(name.toUpperCase()), tag,
				what);
	}

	//
	// W_Profile
	//
	char[][] info = new char[2500][10];

	int profilecount;

	void Profile() throws IOException {
		int i;
		memblock_t block = null;
		Object ptr;
		char ch;
		FileWriter f;
		int j;
		String name;

		for (i = 0; i < numlumps; i++) {
			ptr = lumpcache[i];
			if ((ptr == null)) {
				ch = ' ';
				continue;
			} else {
				// block = (memblock_t *) ( (byte *)ptr - sizeof(memblock_t));
				if (block.tag < PU_PURGELEVEL)
					ch = 'S';
				else
					ch = 'P';
			}
			info[i][profilecount] = ch;
		}
		profilecount++;

		f = new FileWriter(new File("waddump.txt"));
		// name[8] = 0;

		for (i = 0; i < numlumps; i++) {
			name = lumpinfo[i].name;

			f.write(name);

			for (j = 0; j < profilecount; j++)
				f.write("    " + info[i][j]);

			f.write("\n");
		}
		f.close();
	}

	// /////////////////// HASHTABLE SYSTEM ///////////////////

	//
	// killough 1/31/98: Initialize lump hash table
	//

	/**
	 * Maes 12/12/2010: Some recognition must go to Killough for first
	 * Introducing the hashtable system into Boom. On early releases I had
	 * copied his implementation, but it proved troublesome later on and slower
	 * than just using the language's built-in hash table. Lesson learned, kids:
	 * don't reinvent the wheel.
	 * 
	 * TO get an idea of how superior using a hashtable is, on 1000000 random
	 * lump searches the original takes 48 seconds, searching for precomputed
	 * hashes takes 2.84, and using a Hashtable takes 0.2 sec.
	 * 
	 */

	Hashtable<String, Integer> doomhash;

	protected void InitLumpHash() {

		doomhash = new Hashtable<String, Integer>(numlumps);

		for (int i = 0; i < numlumps; i++)
			lumpinfo[i].index = -1; // mark slots empty

		// Insert nodes to the beginning of each chain, in first-to-last
		// lump order, so that the last lump of a given name appears first
		// in any chain, observing pwad ordering rules. killough

		for (int i = 0; i < numlumps; i++) { // hash function:
			doomhash.put(lumpinfo[i].name.toUpperCase(), new Integer(i));
		    }
	}

	public int CheckNumForName(String name/* , int namespace */)

	{
		Integer r = doomhash.get(name);
		// System.out.print("Found "+r);

		if (r != null)
			return r.intValue();

		// System.out.print(" found "+lumpinfo[i]+"\n" );
		return -1;
	}

}