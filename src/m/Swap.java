package m;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Swap.java,v 1.1 2010/06/30 08:58:50 velktron Exp $
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
// $Log: Swap.java,v $
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
//	Endianess handling, swapping 16bit and 32bit.
//
//-----------------------------------------------------------------------------

public class Swap{
// Endianess handling.
// WAD files are stored little endian.

/*#ifdef __BIG_ENDIAN__
short   SwapSHORT(short);
long    SwapLONG(long);
#define SHORT(x)    ((short)SwapSHORT((unsigned short) (x)))
#define LONG(x)         ((long)SwapLONG((unsigned long) (x)))
#else
#define SHORT(x)    (x)
#define LONG(x)         (x)
#endif
*/


// Swap 16bit, that is, MSB and LSB byte.
public static short SHORT(short x)
{
    // No masking with 0xFF should be necessary. 
    // MAES: necessary with java due to sign trailing.
    
    return (short) ((short) ((x>>>8)&0xFF) | (x<<8));
}

//Swap 16bit, that is, MSB and LSB byte.
public static short SHORT(char x)
{
    // No masking with 0xFF should be necessary. 
    // MAES: necessary with java due to sign trailing.
    
    return (short) ((short) ((x>>>8)&0xFF) | (x<<8));
}

//Swap 16bit, that is, MSB and LSB byte.
public static char USHORT(char x)
{
    // No masking with 0xFF should be necessary. 
    // MAES: necessary with java due to sign trailing.
    
    return (char) ((char) ((x>>>8)&0xFF) | (x<<8));
}


// Swapping 32bit.
// Maes: the "long" here is really 32-bit.
public static int LONG( int x)
{
    return
	(x>>>24)
	| ((x>>>8) & 0xff00)
	| ((x<<8) & 0xff0000)
	| (x<<24);
}
}
