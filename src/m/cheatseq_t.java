package m;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
//$Id: cheatseq_t.java,v 1.2 2010/07/03 23:24:13 velktron Exp $
//
//Copyright (C) 1993-1996 by id Software, Inc.
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//$Log: cheatseq_t.java,v $
//Revision 1.2  2010/07/03 23:24:13  velktron
//Added a LOT of stuff, like Status bar code & objects. Now we're cooking with gas!
//
//Revision 1.1  2010/06/30 08:58:50  velktron
//Let's see if this stuff will finally commit....
//
//
//Most stuff is still  being worked on. For a good place to start and get an idea of what is being done, I suggest checking out the "testers" package.
//
//Revision 1.1  2010/06/29 11:07:34  velktron
//Release often, release early they say...
//
//Commiting ALL stuff done so far. A lot of stuff is still broken/incomplete, and there's still mixed C code in there. I suggest you load everything up in Eclpise and see what gives from there.
//
//A good place to start is the testers/ directory, where you  can get an idea of how a few of the implemented stuff works.
//
//
//DESCRIPTION:
//	Cheat sequence checking.
//
//-----------------------------------------------------------------------------

/** Cheat sequence checking.
 *  MAES: all of this stuff used to be in cheat.h and cheat.c, but seeing
 *  how all manipulation is done on "cheatseq_t" objects, it makes more
 *  sense to move this functionality in here.
 * 
 */

public class cheatseq_t {

	// This holds the actual data (was a char*).
	 public char[]  sequence;
	// This is used as a pointer inside sequence.
	// Was a char*, but in Java it makes more sense to have it as an int.
	 public int  p;

	 public cheatseq_t(char[] sequence, int p) {
		this.sequence=sequence;
		this.p=p;
	}

	/** This was in cheat.c, but makes more sense to be used as an initializer/constructor.
	  * 
	  */
	 public void
	 cht_GetParam
	 ( 
	   char[]		buffer )
	 {

	     //char[] p;
	     char c;
	     int ptr=0;

	     //p = this.sequence;
	     // Increments pointer until the sequence reaches its first internal "1" ???
	     while (this.sequence[ptr++]!=1);
	     int bptr=0;
	     // Now it copies the contents of this cheatseq_t into buffer...and nils it???
	     do
	     {
	 	c = this.sequence[ptr];
	 	buffer[bptr++]=c;
	 	this.sequence[ptr++]=0;
	     }
	     while ((c!=0) && (this.sequence[ptr]!=0xff ));

	     if (this.sequence[ptr]==0xff)
	 	buffer[bptr] = 0;

	 }
	 
	 /** Called in st_stuff module, which handles the input.
	  *  Returns a 1 if the cheat was successful, 0 if failed.
	  *  
	  * MAES: Let's make this boolean.
	  * 
	  * @param cht
	  * @param key
	  * @return
	  */

	 public boolean cht_CheckCheat
	 ( cheatseq_t	cht,
	   char		key )
	 {
	     boolean rc = false;

	     if (firsttime)
	     {
	 	firsttime = false;
	 	for (char i=0;i<256;i++) cheat_xlate_table[i] = SCRAMBLE(i);
	     }

	     if (cht.p<0)
	 	cht.p = 0; // initialize if first time

	     if (cht.p == 0)
	     	// This actually points inside "sequence"
	 	//*(cht->p++) = key;
	     	cht.sequence[cht.p++]=key;
	     else if
	 	(cheat_xlate_table[(char)key] == cht.sequence[cht.p]) cht.p++;
	     else
	 	cht.p= 0;

	     if (cht.sequence[cht.p]== 1)
	 	cht.p++;
	     else if (cht.sequence[cht.p] == 0xff) // end of sequence character
	     {
	 	cht.p=0;
	 	rc = true;
	     }

	     return rc;
	 }
	 
	 //
	//CHEAT SEQUENCE PACKAGE
	//

	public static char SCRAMBLE(char a){
	    return
	 (char) ((((a)&1)<<7) + (((a)&2)<<5) + ((a)&4) + (((a)&8)<<1) 
		    + (((a)&16)>>1) + ((a)&32) + (((a)&64)>>5) + (((a)&128)>>7));
	}


	
	/** These should be common among all instances, unless for soooome
	 *  reason you need multiple different such tables.
	 * 
	 */
	public static boolean		firsttime = true;
	public static char[] cheat_xlate_table=new char[256];
} 
