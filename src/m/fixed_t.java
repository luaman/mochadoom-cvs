package m;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: fixed_t.java,v 1.12 2010/12/11 15:08:59 velktron Exp $
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
// $Log: fixed_t.java,v $
// Revision 1.12  2010/12/11 15:08:59  velktron
// Techdemo release.
//
// Revision 1.11  2010/12/10 17:38:57  velktron
// pspritescale fixed, weapon actions won't crash (but not work either).
//
// Revision 1.10  2010/11/11 23:39:53  velktron
// Fixed texture alignment bug
//
// Revision 1.9  2010/11/03 16:48:04  velktron
// "Bling" view angles fixed (perhaps related to the "bleeding line bug"?)
//
// Revision 1.8  2010/08/26 16:43:42  velktron
// Automap functional, biatch.
//
// Revision 1.7  2010/07/22 15:37:53  velktron
// MAJOR changes in Menu system.
//
// Revision 1.6  2010/07/07 16:17:37  velktron
// *** empty log message ***
//
// Revision 1.5  2010/07/06 12:54:50  velktron
// A lot of work thrown in the renderer, but don't get too excited...
//
// Revision 1.4  2010/07/05 16:18:40  velktron
// YOU DON'T WANNA KNOW
//
// Revision 1.3  2010/07/03 23:24:13  velktron
// Added a LOT of stuff, like Status bar code & objects. Now we're cooking with gas!
//
// Revision 1.2  2010/07/01 18:38:09  velktron
// Video "rendering" completed, columns_t parsing completed. Play around with testers :-p
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
//	Fixed point implementation.
//
//-----------------------------------------------------------------------------

//
// Fixed point, 32bit as 16.16.
//

//#include "doomtype.h"
//#include "i_system.h"


public class fixed_t implements Comparable<fixed_t>{

    public static final int FRACBITS =       16;
    public static final int FRACUNIT =       (1<<FRACBITS);
    public int val;
    
    public fixed_t(){
        this.set(0);
    }
    
    public int get(){
        return val;
    }
    
    public void set(int val){
        this.val=val;
    }

    public void copy(fixed_t a){
        this.set(a.get());
    }

    
    public boolean equals(fixed_t a){
        return (this.get()==a.get())?true:false;
    }

    public static boolean equals(fixed_t a, fixed_t b){
        return (a.get()==b.get())?true:false;
    }    
    
    public fixed_t(int val){
        this.val=val;
    }
    
public fixed_t(fixed_t x) {
        this.val=x.val;
    }

public static final String rcsid = "$Id: fixed_t.java,v 1.12 2010/12/11 15:08:59 velktron Exp $";

/** Creates a new fixed_t object for the result a*b
 * 
 * @param a
 * @param b
 * @return
 */

public static int FixedMul
( fixed_t	a,
  fixed_t	b )
{
    return (int)(((long) a.val * (long ) b.val) >>> FRACBITS);
}

public static int FixedMul
( int   a,
  fixed_t   b )
{
    return (int)(((long) a * (long ) b.val) >>> FRACBITS);
}

public static final int FixedMul
( int   a,
  int   b )
{
    return (int)(((long) a * (long ) b) >>> FRACBITS);
}


/** Returns result straight as an int..
 * 
 * @param a
 * @param b
 * @return
 */

public static int FixedMulInt
( fixed_t   a,
        fixed_t   b )
{
    return (int)(((long) a.val * (long ) b.val) >> FRACBITS);
}

/** In-place c=a*b
 * 
 * @param a
 * @param b
 * @param c
 */

public final static void FixedMul
( fixed_t   a,
        fixed_t   b,
        fixed_t c)
{
    c.set((int)(((long) a.val * (long ) b.val) >> FRACBITS));
}


/** In-place this=this*a
 * 
 * @param a
 * @param b
 * @param c
 */

public final void FixedMul
( fixed_t   a)
{
    this.set((int)(((long) a.val * (long ) this.val) >> FRACBITS));
}


public final static int
FixedDiv
( int   a,
  int   b )
{
	  if ((Math.abs(a) >> 14) >= Math.abs(b))
	    {
		return (a^b) < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
	    }
	    else
	    {
		long result;

		result = ((long) a << 16) / b;

		return (int) result;
	    }
}


public final static int
FixedDiv2
( int   a,
  int   b )
{

    
    int c;
    c = (int)(((long)a<<16) / (long)b);
    return c;
    
    /*
    double c;

    c = ((double)a) / ((double)b) * FRACUNIT;

  if (c >= 2147483648.0 || c < -2147483648.0)
      throw new ArithmeticException("FixedDiv: divide by zero");
 
 return (int)c;*/
}

@Override
public int compareTo(fixed_t o) {
    if (o.getClass()!=fixed_t.class) return -1;
    if (this.val==((fixed_t)(o)).val) return 0;
    if (this.val>((fixed_t)(o)).val) return 1;
    else return -1;
    }

public int compareTo(int o) {
    if (this.val==o) return 0;
    if (this.val>o) return 1;
    else return -1;
    }

public void add(fixed_t a){
    this.val+=a.val;
}

public void sub(fixed_t a){
    this.val-=a.val;
}

public void add(int a){
    this.val+=a;
}

public void sub(int a){
    this.val-=a;
}

/** a+b
 * 
 * @param a
 * @param b
 * @return
 */

public static int add(fixed_t a,fixed_t b){
    return a.val+b.val;
}

/** a-b
 * 
 * @param a
 * @param b
 * @return
 */

public static int sub(fixed_t a,fixed_t b){
    return a.val-b.val;
}

/** c=a+b
 * 
 * @param c
 * @param a
 * @param b
 */

public static void add(fixed_t c, fixed_t a,fixed_t b){
    c.val= a.val+b.val;
}

/** c=a-b
 * 
 * @param c
 * @param a
 * @param b
 */

public static void sub(fixed_t c,fixed_t a,fixed_t b){
    c.val= a.val-b.val;
}


/** Equals Zero
 * 
 * @return
 */

public boolean isEZ() {
    return (this.val==0);
    }

/** Greater than Zero
 * 
 * @return
 */

public boolean isGZ() {
    return (this.val>0);
    }

/** Less than Zero
 * 
 * @return
 */
public boolean isLZ() {
    return (this.val<0);
    }

// These are here to make easier handling all those methods in R 
// that return "1" or "0" based on one result.

public int oneEZ(){
    return (this.val==0)?1:0;
}

public int oneGZ(){
    return (this.val>0)?1:0;
}

public int oneLZ(){
    return (this.val<0)?1:0;
}


}