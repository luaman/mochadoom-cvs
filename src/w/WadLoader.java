// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: WadLoader.java,v 1.2 2010/06/30 11:44:40 velktron Exp $
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
// Revision 1.2  2010/06/30 11:44:40  velktron
// Added a tester for patches (one of the most loosely-coupled structs in Doom!) and fixed some minor stuff all around.
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
//	Handles WAD file header, directory, lump I/O.
//
//-----------------------------------------------------------------------------

package w;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import utils.C2JUtils;
import z.memblock_t;

import static data.Defines.*;
import i.*;

public class WadLoader{


//#include "doomtype.h"
//#include "m_swap.h"
//#include "i_system.h"
//#include "z_zone.h"



//
// GLOBALS
//

// Location of each lump on disk.
public  lumpinfo_t[]		lumpinfo;		
public WadLoader() {
    lumpinfo=new lumpinfo_t[0];
}





public int			numlumps;

//MAES:  probably array of byte[]???
//void**			lumpcache;
private ByteBuffer[] lumpcache;


//#define strcmpi	strcasecmp

//MAES: this is just capitalization. 
// However we can't manipulate String object in Java directly like this, 
// so this must be a return type.

public String strupr (String s)
{
    return s.toUpperCase();
}

public void strupr (char[] s)
{
    for (int i=0;i<s.length;i++){
        s[i]=Character.toUpperCase(s[i]);
    }
}


// Maes: File intead of "inthandle"

public long filelength (File handle) 
{ 
    try {/*stat	fileinfo;
    
    if (fstat (handle,&fileinfo) == -1)
	I_Error ("Error fstating");*/

    return handle.length();
    } catch (Exception e){
            system.Error ("Error fstating");
            return -1;
    }
    
}

// MAES: same problem here. I'd rather return a new String than doing stupid
// shit like passing an array of Strings as dest and modifying the first item.


public String
ExtractFileBase
( String		path)
{
    byte[] dest=new byte[8];

    int		length=0;
    int src = path.length()-1;
    // Duh???
    
    
    // back up until a \ or the start
    // MAES: hideously not OO or cross-platform :-p
/*    while ((src >=0)
	   && path.charAt(src) != '\\'
	   && path.charAt(src) != '/')
    {
	src--;
    }*/
    
    // Maes: better use this, I think.
    
    src=path.lastIndexOf('\\');
    if (src<0) src=path.lastIndexOf('/');
    if (src<0) src=0;
    
    // copy up to eight characters
    int pos=0;
    while ((pos<path.length()) && (path.charAt(pos) != '.'))
    {
	if (++length == 9)
	    system.Error ("Filename base of %s >8 chars",path);
	    
	    dest[pos]=(byte)path.charAt(pos);
	    pos++;
    }
    return new String(dest);
}





//
// LUMP BASED ROUTINES.
//

//
// W_AddFile
// All files are optional, but at least one file must be
//  found (PWAD, if all required lumps are present).
// Files with a .wad extension are wadlink files
//  with multiple lumps.
// Other files are single lumps with the base filename
//  for the lump name.
//
// If filename starts with a tilde, the file is handled
//  specially to allow map reloads.
// But: the reload feature is a fragile hack...

int			reloadlump;
// MAES: was char*
String			reloadname;


@SuppressWarnings("null")
public void AddFile (String filename) throws Exception
{
    wadinfo_t		header = new wadinfo_t();
    int		lump_p; // MAES: was lumpinfo_t* , but we can use it as an array pointer.
    DoomFile			handle;
    long			length;
    int			startlump;
    filelump_t[]		fileinfo = new filelump_t[1]; //MAES: was *
    filelump_t		singleinfo = new filelump_t();
    DoomFile			storehandle;
    
    // open the file and add to directory

    // handle reload indicator.
    if (filename.charAt(0) == '~')
    {
	filename=filename.substring(1);
	reloadname = filename;
	reloadlump = numlumps;
    }
    
    
    try {
        handle= new DoomFile(filename, "r");
    } catch (Exception e){
        system.Error(" couldn't open %s \n",filename);
        return;
    }

    System.out.println  (" adding "+filename+"\n");

    // We start at the number of lumps?  :-S
    startlump = this.numlumps;
	
    if (filename.substring(filename.length()-3).compareToIgnoreCase("wad")!=0 )
    {
	// single lump file
	fileinfo[0] = singleinfo;
	singleinfo.filepos = 0;
	singleinfo.size = (long)(handle.length());
	singleinfo.name=ExtractFileBase (filename);
	numlumps++;
    }
    else 
    {
	// MAES: 14/06/10 this is historical, for this is the first time I implement reading something
    // from RAF into Doom's structs. Kudos to the JAKE2 team who solved this problem before me.
    // Check out how reading is delegated to each class's "load" method.
	//read (handle, &header, sizeof(header));

        header.load(handle);
        
        if (header.identification.compareTo("IWAD")!=0)
	{
	    // Homebrew levels?
	    if (header.identification.compareTo("PWAD")!=0)
	    {
		system.Error ("Wad file %s doesn't have IWAD or PWAD id\n", filename);
	    }
	    
	    // TODO: modifiedgame = true;		
	}

    // MAES: I don't think the following are needed. Casting to long? :-S    
    //header.numlumps = header.numlumps;
	//header.infotableofs = header.infotableofs;

    length = header.numlumps;
	fileinfo = new filelump_t[(int) length];
	// Init everything:
	C2JUtils.initArrayOfObjects(fileinfo, filelump_t.class);
	
	handle.seek(header.infotableofs);
	//MAES: we can't read raw structs here, and even less BLOCKS of structs.
	
	handle.readObjectArray(fileinfo, (int)length);
	
	/*
	for (int j=0;j<length;j++){
	    fileinfo[j].load (handle);
	    }*/
	numlumps += header.numlumps;
    
    
    // Fill in lumpinfo
	// MAES: this was a realloc(lumpinfo, numlumps*sizeof(lumpinfo_t)), so we have to increase size and copy over. Maybe this should be an ArrayList?
	
	int oldsize=lumpinfo.length;
	lumpinfo_t[] newlumpinfo= new lumpinfo_t[numlumps];
		
	try {
	C2JUtils.initArrayOfObjects(newlumpinfo,lumpinfo_t.class);
	System.arraycopy(lumpinfo, 0,newlumpinfo,0,oldsize);
	} catch (Exception e){
	    //if (!lumpinfo)
	    i.system.Error("Couldn't realloc lumpinfo");
	}
	
	// Bye bye, old lumpinfo!
    lumpinfo = newlumpinfo;

    // MAES: lump_p was an alias for lumpinfo[startlump]. I know it's a bit crude as an approximation but heh...
    
   lump_p = startlump;
    
   // MAES: if reloadname is null, handle is stored...else an invalid handle?
    storehandle = (reloadname!=null) ? null : handle;
    
    int fileinfo_p=0;
    
    for (int i=startlump ; i<numlumps ; i++,lump_p++, fileinfo_p++)
    {
        lumpinfo[lump_p].handle=storehandle;
        lumpinfo[lump_p].position=fileinfo[fileinfo_p].filepos;
        lumpinfo[lump_p].size=fileinfo[fileinfo_p].size;
	//strncpy (lumpinfo[lump_p].name, fileinfo[fileinfo_p].name, 8);
        lumpinfo[lump_p].name=fileinfo[fileinfo_p].name.substring(0,8);
    }
	
    if (reloadname!=null)
	handle.close();
    }   
}


/**
 *
 * W_Reload
 * Flushes any of the reloadable lumps in memory
 *  and reloads the directory.
 * @throws IOException 
 */
@SuppressWarnings("null")
public void Reload () throws IOException
{
    wadinfo_t		header = null;
    int			lumpcount;
    int		lump_p; // Maes: same as in W_WADload
    int		i;
    DoomFile			handle = null;
    int			length;
    filelump_t[]		fileinfo;
	
    if (reloadname==null)
	return;
	
    try{
    handle=new DoomFile(reloadname,"r");
    } catch (Exception e){
        system.Error ("W_Reload: couldn't open %s",reloadname);
        }

    header.load(handle);
    lumpcount = (int) header.numlumps;
    header.infotableofs = header.infotableofs;
    length = lumpcount;
    fileinfo = new filelump_t[length];
    handle.seek(header.infotableofs);
    
    //MAES: we can't read raw structs here, and even less BLOCKS of structs. 

    handle.readObjectArray(fileinfo, (int)length);
    
    /*
    for (int j=0;j<length;j++){
        fileinfo[j].load (handle);
        }*/
    
    numlumps += header.numlumps;
    //read (handle, fileinfo, length);
    
    // Fill in lumpinfo
    lump_p = reloadlump;
    int fileinfo_p=0;
    for (i=reloadlump ; i<reloadlump+lumpcount ; i++,lump_p++, fileinfo_p++)
    {
        if ((lumpcache[i]!=null)&&(lumpcache[i].hasArray()))
            //Z_Free (lumpcache[i]);
        
        lumpinfo[lump_p].position=fileinfo[fileinfo_p].filepos;
        lumpinfo[lump_p].size=fileinfo[fileinfo_p].size;
    }

}



//
// W_InitMultipleFiles
// Pass a null terminated list of files to use.
// All files are optional, but at least one file
//  must be found.
// Files with a .wad extension are idlink files
//  with multiple lumps.
// Other files are single lumps with the base filename
//  for the lump name.
// Lump names can appear multiple times.
// The name searcher looks backwards, so a later file
//  does override all earlier ones.
//
public void InitMultipleFiles (String[] filenames) throws Exception
{	
    int		size;
    
    // open all the files, load headers, and count lumps
    numlumps = 0;

    // will be realloced as lumps are added
    lumpinfo = new lumpinfo_t[0];	

    for (String s:filenames)
	this.AddFile (s);

    if (numlumps==0)
	system.Error("W_InitFiles: no files found");
    
    // set up caching
    size = numlumps;
    lumpcache = new ByteBuffer[size];
    
    if (lumpcache==null)
	system.Error ("Couldn't allocate lumpcache");

    //memset (lumpcache,0, size);
}




//
// W_InitFile
// Just initialize from a single file.
//
public void InitFile (String filename) throws Exception
{
    String[] names=new String[2];

    names[0] = filename;
    names[1] = null;
    InitMultipleFiles (names);
}



//
// W_NumLumps
//
public int NumLumps ()
{
    return numlumps;
}



//
// W_CheckNumForName
// Returns -1 if name not found.
//

public int CheckNumForName (String name)
{
  
	
    int		v1;
    int		v2;
    //lumpinfo_t lump_p;	
    
    int lump_p;
    // make the name into two integers for easy compares
    // case insensitive
    name8 union=new name8(strupr (name));

    v1 = union.x[0];
    v2 = union.x[1];


    // scan backwards so patch lump files take precedence
    lump_p = numlumps;

    while (lump_p-- != 0)
    {
        int a=name8.stringToInt(lumpinfo[lump_p].name,0);
        int b=name8.stringToInt(lumpinfo[lump_p].name,4);
	if ( ( a== v1)&&(
	        b == v2))
	{
	    return lump_p;
	}
    }

    // TFB. Not found.
    return -1;
}

/** Returns actual lumpinfo_t object for a given name.
 *  Useful if you want to access something on a file, I guess?
 * 
 * @param name
 * @return
 */
public lumpinfo_t GetLumpinfoForName (String name)
{

 
 int     v1;
 int     v2;
 //lumpinfo_t lump_p;    
 
 int lump_p;
 // make the name into two integers for easy compares
 // case insensitive
 name8 union=new name8(strupr (name));

 v1 = union.x[0];
 v2 = union.x[1];


 // scan backwards so patch lump files take precedence
 lump_p = numlumps;

 while (lump_p-- != 0)
 {
     int a=name8.stringToInt(lumpinfo[lump_p].name,0);
     int b=name8.stringToInt(lumpinfo[lump_p].name,4);
 if ( ( a== v1)&&(
         b == v2))
 {
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
public int GetNumForName (String name)
{
    int	i;

    i = CheckNumForName (name);
    
    if (i == -1)
      system.Error ("W_GetNumForName: %s not found!", name);
      
    return i;
}


//
// W_LumpLength
// Returns the buffer size needed to load the given lump.
//
public int LumpLength (int lump)
{
    if (lump >= numlumps)
	i.system.Error ("W_LumpLength: %i >= numlumps",lump);

    return (int)lumpinfo[lump].size;
}



/**
* W_ReadLump
* Loads the lump into the given buffer,
*  which must be >= W_LumpLength().
 * @throws IOException 
*/
public void
ReadLump
( int		lump,
  ByteBuffer		dest ) throws IOException
{
    int		c;
    lumpinfo_t	l; // Maes: was *..probably  not array.
    DoomFile		handle = null;
	
    if (lump >= this.numlumps) {
	system.Error("W_ReadLump: %i >= numlumps",lump);
        return;
    }

    l = lumpinfo[lump];
	
    // ??? I_BeginRead ();
	
    if (l.handle == null)
    {
	// reloadable file, so use open / read / close
        try {
	 handle = new DoomFile (this.reloadname,"r");
        } catch (Exception e){
	    system.Error("W_ReadLump: couldn't open %s",reloadname);
        }
    }
    else
	handle = l.handle;
		
    handle.seek(l.position);
    byte[] buf=new byte[(int) l.size];
    c=handle.read(buf);
    dest.put(buf);

    if (c < l.size)
        system.Error("W_ReadLump: only read %i of %i on lump %i",c,l.size,lump);

    if (l.handle == null)
	handle.close();
		
    // ??? I_EndRead ();
  
}




//
// W_CacheLumpNum
//
public ByteBuffer
CacheLumpNum
( int		lump,
  int		tag ) throws IOException
{
   // byte*	ptr;

    if (lump >= numlumps){
        system.Error ("W_CacheLumpNum: %i >= numlumps",lump);
        }
		
    if ((lumpcache[lump]==null)/*&&(!lumpcache[lump].hasArray())*/)
    {
    
	// read the lump in
	
	//printf ("cache miss on lump %i\n",lump);
	//ptr = Z_Malloc (W_LumpLength (lump), tag, &lumpcache[lump]);
    lumpcache[lump]=ByteBuffer.allocate(this.LumpLength(lump));
	this.ReadLump (lump, lumpcache[lump]);
    }
    else
    {
	//printf ("cache hit on lump %i\n",lump);
	//Z_ChangeTag (lumpcache[lump],tag);
    }
	
    return lumpcache[lump];
}



//
// W_CacheLumpName
//
public ByteBuffer
CacheLumpName
( String		name,
  int		tag ) throws IOException
{
    return this.CacheLumpNum (this.GetNumForName(name), tag);
}


//
// W_Profile
//
char[][]		info= new char[2500][10];
int		profilecount;

void Profile () throws IOException
{
    int		i;
    memblock_t	block = null;
    ByteBuffer	ptr;
    char	ch;
    FileWriter	f;
    int		j;
    String	name;
	
	
    for (i=0 ; i<numlumps ; i++)
    {	
        ptr = lumpcache[i];
	if ((ptr==null)||(!ptr.hasArray()))
	{
	    ch = ' ';
	    continue;
	}
	else
	{
	    //block = (memblock_t *) ( (byte *)ptr - sizeof(memblock_t));
	    if (block.tag < PU_PURGELEVEL)
		ch = 'S';
	    else
		ch = 'P';
	}
	info[i][profilecount] = ch;
    }
    profilecount++;
	
    f = new FileWriter(new File("waddump.txt"));
    //name[8] = 0;

    for (i=0 ; i<numlumps ; i++)
    {
	name=lumpinfo[i].name;

	f.write(name);

	for (j=0 ; j<profilecount ; j++)
	    f.write("    "+info[i][j]);

	f.write("\n");
    }
    f.close();
}



}