// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: cheat.java,v 1.2 2010/07/20 15:52:56 velktron Exp $
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
// $Log: cheat.java,v $
// Revision 1.2  2010/07/20 15:52:56  velktron
// LOTS of changes, Automap almost complete. Use of fixed_t inside methods severely limited.
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
//	Cheat sequence checking.
//
//-----------------------------------------------------------------------------

public class cheat{
    

//
//CHEAT SEQUENCE PACKAGE
//

public static char SCRAMBLE(char a){
    return
(char) ((((a)&1)<<7) + (((a)&2)<<5) + ((a)&4) + (((a)&8)<<1) 
    + (((a)&16)>>1) + ((a)&32) + (((a)&64)>>5) + (((a)&128)>>7));
}

public static boolean		firsttime = true;
public static char[] cheat_xlate_table=new char[256];


//
// Called in st_stuff module, which handles the input.
// Returns a 1 if the cheat was successful, 0 if failed.
//
public static int cht_CheckCheat
( cheatseq_t	cht,
  char		key )
{
    int rc = 0;

    if (firsttime)
    {
	firsttime = false;
	for (char i=0;i<256;i++) cheat_xlate_table[i] = SCRAMBLE(i);
    }

    if (cht.p==0)
	cht.p = cht.sequence; // initialize if first time

    if (cht.p == 0)
	*(cht->p++) = key;
    else if
	(cheat_xlate_table[(unsigned char)key] == *cht->p) cht->p++;
    else
	cht->p = cht->sequence;

    if (*cht->p == 1)
	cht->p++;
    else if (*cht->p == 0xff) // end of sequence character
    {
	cht->p = cht->sequence;
	rc = 1;
    }

    return rc;
}

public static void
cht_GetParam
( cheatseq_t	cht,
  char[]		buffer )
{

    unsigned char *p, c;

    p = cht->sequence;
    while (*(p++) != 1);
    
    do
    {
	c = *p;
	*(buffer++) = c;
	*(p++) = 0;
    }
    while (c && *p!=0xff );

    if (*p==0xff)
	*buffer = 0;

}


}