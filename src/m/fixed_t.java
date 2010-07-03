package m;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: fixed_t.java,v 1.3 2010/07/03 23:24:13 velktron Exp $
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


public class fixed_t implements Comparable{

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
    
public static final String rcsid = "$Id: fixed_t.java,v 1.3 2010/07/03 23:24:13 velktron Exp $";

/** Creates a new fixed_t object for the result a*b
 * 
 * @param a
 * @param b
 * @return
 */

public static fixed_t FixedMul
( fixed_t	a,
  fixed_t	b )
{
    return new fixed_t((int)(((long) a.val * (long ) b.val) >>> FRACBITS));
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

public static void FixedMul
( fixed_t   a,
        fixed_t   b,
        fixed_t c)
{
    c.set((int)(((long) a.val * (long ) b.val) >> FRACBITS));
}

public static fixed_t
FixedDiv
( fixed_t	a,
  fixed_t	b )
{
    if ( (Math.abs(a.val)>>14) >= Math.abs(b.val))
	return (a.val^b.val)<0 ? new fixed_t(Integer.MIN_VALUE) : new fixed_t(Integer.MAX_VALUE);
    return FixedDiv2 (a,b);
}

public static void
FixedDiv
( fixed_t   a,
        fixed_t b, fixed_t c)
{
    if ( (Math.abs(a.val)>>14) >= Math.abs(b.val)){
        if ((a.val^b.val)<0) {
            c.set(Integer.MIN_VALUE);
        }
        else { 
            c.set(Integer.MAX_VALUE);
        } 
        return;
    }
    FixedDiv2 (a,b,c);
}


public static void
FixedDiv2
( fixed_t   a,
        fixed_t b, fixed_t c )
{
	
//    c.set((int)((((long)a.val)<<16) / (long)b.val));

      
c.set((int)(((double)a.val) / ((double)b.val) * FRACUNIT));

//if (c >= 2147483648.0 || c < -2147483648.0)
//I_Error("FixedDiv: divide by zero");
//return new fixed_t((int)c);*/
}


public static fixed_t
FixedDiv2
( fixed_t	a,
  fixed_t	b )
{

    /*
    int c;
    c = (int)(((long)a.val<<16) / (long)b.val);
    return new fixed_t(c);
     */
    
    double c;

    c = ((double)a.val) / ((double)b.val) * FRACUNIT;

//    if (c >= 2147483648.0 || c < -2147483648.0)
//	I_Error("FixedDiv: divide by zero");
    return new fixed_t((int)c);
}

@Override
public int compareTo(Object o) {
    if (o.getClass()!=fixed_t.class) return -1;
    if (this.get()==((fixed_t)(o)).get()) return 0;
    if (this.get()>((fixed_t)(o)).get()) return 1;
    else return -1;
    }

}