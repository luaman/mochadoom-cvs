package z;

import i.DoomSystemInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import static data.Defines.*;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: zone.java,v 1.5 2010/11/12 13:37:25 velktron Exp $
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
// $Log: zone.java,v $
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
//	Zone Memory Allocation. Neat.
//
//-----------------------------------------------------------------------------

public class zone{

public static String rcsid = "$Id: zone.java,v 1.5 2010/11/12 13:37:25 velktron Exp $";

//
// ZONE MEMORY ALLOCATION
//
// There is never any space between memblocks,
//  and there will never be two contiguous free memblocks.
// The rover can be left pointing at a non-empty block.
//
// It is of no value to free a cachable block,
//  because it will get overwritten automatically if needed.
// 
 
public static int ZONEID	=0x1d4a11;


class memzone_t
{
    /** total bytes malloced, including header */
    int		size;

    /** start / end cap for linked list */
    memblock_t	blocklist,rover;
    
}

memzone_t	mainzone;

/**
* Z_ClearZone
*/
public void ClearZone (memzone_t zone)
{
// Dummy.
}



//
// Z_Init
//
public void Init ()
{
   // Dummy
}


//
// Z_Free
//
public void Free (memblock_t ptr)
{
    memblock_t      block;
    memblock_t     other;
    
    block = ptr;

    if (block.id != ZONEID)
    I.Error ("Z_Free: freed a pointer without ZONEID");
   
    /*
    if (block.user > 0x100)
    {
    // smaller values are not pointers
    // Note: OS-dependend?
    
    // clear the user's mark
    *block->user = 0;
    }
    */
    
    // mark as free
    block.user = null; 
    block.tag = 0;
    block.id = 0;
    
    other = block.prev;

    if (other.user==null)
    {
    // merge with previous free block
    other.size += block.size;
    other.next = block.next;
    other.next.prev = other;

    if (block == mainzone.rover)
        mainzone.rover = other;

    block = other;
    }
    
    other = block.next;
    if (other.user==null)
    {
    // merge the next free block onto the end
    block.size += other.size;
    block.next = other.next;
    block.next.prev = block;

    if (other == mainzone.rover)
        mainzone.rover = block;
    }
    }



//
// Z_Malloc
// You can pass a NULL user if the tag is < PU_PURGELEVEL.
//
public static int MINFRAGMENT	=	64;


public void Malloc
( int		size,
  int		tag,
  Void		user )
{
    // Dummy
}



//
// Z_FreeTags
//
public void
FreeTags
( int		lowtag,
  int		hightag )
{
    memblock_t	block;
    memblock_t	next;
	
    for (block = mainzone.blocklist.next ;
	 block != mainzone.blocklist ;
	 block = next)
    {
	// get link before freeing
	next = block.next;

	// free block?
	if (block.user==null)
	    continue;
	
	if (block.tag >= lowtag && block.tag <= hightag)
	    Free (block);
    }
}



//
// Z_DumpHeap
// Note: TFileDumpHeap( stdout ) ?
//
void
Z_DumpHeap
( int		lowtag,
  int		hightag )
{
    memblock_t	block;
	
    System.out.println ("zone size: "+mainzone.size+" location: "+mainzone);
    
    System.out.println  ("tag range: "+lowtag+ " to "+hightag);
	
    for (block = mainzone.blocklist.next ; ; block = block.next)
    {
	if (block.tag >= lowtag && block.tag <= hightag)
	    System.out.println  ("block:"+block+"    size:"+block.size+"    user:"+block.user+"    tag:"+block.tag);
		
	if (block.next == mainzone.blocklist)
	{
	    // all blocks have been hit
	    break;
	}
	
/*	if ( (byte *)block + block.size != (byte *)block.next)
	    printf ("ERROR: block size does not touch the next block\n");*/

	if ( block.next.prev != block)
	    System.err.println("ERROR: next block doesn't have proper back link");

	if ((block.user==null) && (block.next.user==null))
	    System.err.println("ERROR: two consecutive free blocks\n");
    }
}


//
// Z_FileDumpHeap
//
public void FileDumpHeap (File f) throws FileNotFoundException
{
    memblock_t	block;
    PrintWriter   pw=new PrintWriter (f);
	
    pw.printf("zone size: %i  location: %p\n",mainzone.size,mainzone);
	
    for (block = mainzone.blocklist.next ; ; block = block.next)
    {
        pw.printf("block:%p    size:%7i    user:%p    tag:%3i\n",
		 block, block.size, block.user, block.tag);
		
	if (block.next == mainzone.blocklist)
	{
	    // all blocks have been hit
	    break;
	}
	
	/*if ( (byte *)block + block.size != (byte *)block.next)
	    fprintf (f,"ERROR: block size does not touch the next block\n");*/

	if ( block.next.prev != block)
	    pw.printf("ERROR: next block doesn't have proper back link\n");

	if ((block.user==null) && (block.next.user==null))
	    pw.printf("ERROR: two consecutive free blocks\n");
    }
    pw.close();
}



//
// Z_CheckHeap
//
public void CheckHeap ()
{
    memblock_t	block;
	
    for (block = mainzone.blocklist.next ; ; block = block.next)
    {
	if (block.next == mainzone.blocklist)
	{
	    // all blocks have been hit
	    break;
	}
	
/*	if ( block + block.size != (byte *)block.next)
	    system.Error ("Z_CheckHeap: block size does not touch the next block\n");*/

	if ( block.next.prev != block)
	    I.Error ("Z_CheckHeap: next block doesn't have proper back link\n");

	if (block.user == null && block.next.user==null)
	    I.Error ("Z_CheckHeap: two consecutive free blocks\n");
    }
}




//
// Z_ChangeTag
//
public void
ChangeTag2
( memblock_t	ptr,
  int		tag )
{
    memblock_t	block;
	
    block = ptr;

    if (block.id != ZONEID)
	I.Error ("Z_ChangeTag: freed a pointer without ZONEID");

/*    if (tag >= PU_PURGELEVEL)
	system.Error ("Z_ChangeTag: an owner is required for purgable blocks");*/

    block.tag = tag;
}



//
// Z_FreeMemory
//
public int FreeMemory ()
{
    memblock_t		block;
    int			free;
	
    free = 0;
    
    for (block = mainzone.blocklist.next ;
	 block != mainzone.blocklist;
	 block = block.next)
    {
	if (block.user == null|| block.tag >= PU_PURGELEVEL)
	    free += block.size;
    }
    return free;
}

}
