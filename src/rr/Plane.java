package rr;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Plane.java,v 1.3 2010/08/24 14:57:42 velktron Exp $
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
// $Log: Plane.java,v $
// Revision 1.3  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
//
// Revision 1.2  2010/07/15 14:01:49  velktron
// Added reflector Method stuff for function pointers.
//
// Revision 1.1  2010/07/14 16:12:20  velktron
// A new direction has been taken for the Renderer: instead of making a single, omnipotent "Renderer" object, the functionality will remain split into at least Renderer, Things, Planes and Draw, with a new DoomRendererContext object keeping everything glued together.
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
//	Here is a core component: drawing the floors and ceilings,
//	 while maintaining a per column clipping list only.
//	Moreover, the sky areas have to be determined.
//
//-----------------------------------------------------------------------------

import static data.Defines.*;
import data.doomstat;
import doom.DoomContext;
import i.system;
import m.fixed_t;
import static m.fixed_t.*;
import static data.SineCosine.*;
import static rr.Renderer.*;
import static data.Tables.*;

public class Plane{
public static final String
rcsid = "$Id: Plane.java,v 1.3 2010/08/24 14:57:42 velktron Exp $";

private Draw DR;
private Renderer R;
private doomstat DS;
private BSP BSP;

public Plane (DoomRenderingContext DRC){
    this.DR=DRC.DR;
    this.R=DRC.R;
}

doomstat ds;

planefunction_t		floorfunc;
planefunction_t		ceilingfunc;

//
// opening
//

// Here comes the obnoxious "visplane".
public static final int  MAXVISPLANES	=128;
protected visplane_t[]		visplanes=new visplane_t[MAXVISPLANES];
/** visplane_t*,  treat as indexes into visplanes */
protected int		lastvisplane, floorplane, 	ceilingplane;

// ?
public static final int MAXOPENINGS	=SCREENWIDTH*64;

private static final boolean RANGECHECK = false;
short[]			openings=new short[MAXOPENINGS];
// Maes: pointer hack.
short			lastopening;//=new Short((short) 0);


//
// Clip values are the solid pixel bounding the range.
//  floorclip starts out SCREENHEIGHT
//  ceilingclip starts out -1
//
short[]			floorclip=new short[SCREENWIDTH];
short[]			ceilingclip=new short[SCREENWIDTH];

//
// spanstart holds the start of a plane span
// initialized to 0 at start
//
int[]			spanstart=new int[SCREENHEIGHT];
int[]			spanstop=new int [SCREENHEIGHT];

//
// texture mapping
//
lighttable_t[][]		planezlight;
/** To treat as fixed_t */
int			planeheight;
/** To treat at fixed_t */
int[]			yslope=new int[SCREENHEIGHT];
/** To treat as fixed_t */
int[]			distscale=new int[SCREENWIDTH];
/** To treat as fixed_t */
int			basexscale, baseyscale;

/** To treat as fixed_t */
int[]			cachedheight=new int[SCREENHEIGHT];
/** To treat as fixed_t */
int[]			cacheddistance=new int[SCREENHEIGHT];
/** To treat as fixed_t */
int[]			cachedxstep=new int[SCREENHEIGHT];
/** To treat as fixed_t */
int[]			cachedystep=new int[SCREENHEIGHT];



//
// R_InitPlanes
// Only at game startup.
//
void InitPlanes ()
{
  // Doh!
}


//
// R_MapPlane
//
// Uses global vars:
//  planeheight
//  ds_source
//  basexscale
//  baseyscale
//  viewx
//  viewy
//
// BASIC PRIMITIVE
//
public void
MapPlane
( int		y,
  int		x1,
  int		x2 )
{
    // MAES: angle_t
    int	angle;
    // 
    int	distance;
    int	length;
    int	index;
	
if (RANGECHECK){
    if (x2 < x1
	|| x1<0
	|| x2>=ds.viewwidth
	|| y>ds.viewheight)
    {
	system.Error ("R_MapPlane: %i, %i at %i",x1,x2,y);
    }
}

    if (planeheight != cachedheight[y])
    {
	cachedheight[y] = planeheight;
	distance = cacheddistance[y] = FixedMul (planeheight, yslope[y]);
	DR.ds_xstep = cachedxstep[y] = FixedMul (distance,basexscale);
	DR.ds_ystep = cachedystep[y] = FixedMul (distance,baseyscale);
    }
    else
    {
	distance = cacheddistance[y];
	DR.ds_xstep = cachedxstep[y];
	DR.ds_ystep = cachedystep[y];
    }
	
    length = FixedMul (distance,distscale[x1]);
    angle = (R.viewangle + R.xtoviewangle[x1])>>ANGLETOFINESHIFT;
    DR.ds_xfrac = R.viewx + FixedMul(finecosine[angle], length);
    DR.ds_yfrac = -R.viewy - FixedMul(finesine[angle], length);

    if (R.fixedcolormap!=null)
	DR.ds_colormap = R.fixedcolormap;
    else
    {
	index = distance >> LIGHTZSHIFT;
	
	if (index >= MAXLIGHTZ )
	    index = MAXLIGHTZ-1;

	DR.ds_colormap = planezlight[index];
    }
	
    DR.ds_y = y;
    DR.ds_x1 = x1;
    DR.ds_x2 = x2;

    // high or low detail
    R.spanfunc.invoke(this, void.class);	
}


//
// R_ClearPlanes
// At begining of frame.
//
public void ClearPlanes ()
{
    int	angle;
    
    // opening / clipping determination
    for (int i=0 ; i<DS.viewwidth ; i++)
    {
	floorclip[i] = DS.viewheight;
	ceilingclip[i] = -1;
    }

    lastvisplane = 0;
    lastopening = openings[0];
    
    // texture calculation
    //memset (cachedheight, 0, sizeof(cachedheight));

    // left to right mapping
    angle = (R.viewangle-ANG90)>>ANGLETOFINESHIFT;
	
    // scale will be unit scale at SCREENWIDTH/2 distance
    basexscale = FixedDiv (finecosine[angle],R.centerxfrac);
    baseyscale = -FixedDiv (finesine[angle],R.centerxfrac);
}


/**
 * R_FindPlane
 * 
 * @param height (fixed_t)
 * @param picnum
 * @param lightlevel
 * @return
 */

public visplane_t
FindPlane
( int	height,
  int		picnum,
  int		lightlevel )
{
    int check; // visplane_t* 
    visplane_t chk=null;
	
    if (picnum == DS.skyflatnum)
    {
	height = 0;			// all skys map together
	lightlevel = 0;
    }
	
    for (check=0; check<lastvisplane; check++, chk=visplanes[check])
    {
	if (height == chk.height
	    && picnum == chk.picnum
	    && lightlevel ==chk.lightlevel)
	{
	    break;
	}
    }
    
			
    if (check < lastvisplane)
	return chk;
		
    if (lastvisplane == MAXVISPLANES)
	system.Error ("R_FindPlane: no more visplanes");
		
    lastvisplane++;

    chk.height = height;
    chk.picnum = picnum;
    chk.lightlevel = lightlevel;
    chk.minx = SCREENWIDTH;
    chk.maxx = -1;
    
    //memset (chk.top,0xff,sizeof(chk.top));
    chk.clearTop();
		
    return chk;
}


//
// R_CheckPlane
//
public visplane_t
CheckPlane
( visplane_t	pl,
  int		start,
  int		stop )
{
    int		intrl;
    int		intrh;
    int		unionl;
    int		unionh;
    int x;
	
    if (start < pl.minx)
    {
	intrl = pl.minx;
	unionl = start;
    }
    else
    {
	unionl = pl.minx;
	intrl = start;
    }
	
    if (stop > pl.maxx)
    {
	intrh = pl.maxx;
	unionh = stop;
    }
    else
    {
	unionh = pl.maxx;
	intrh = stop;
    }

    for (x=intrl ; x<= intrh ; x++)
	if (pl.top[x] != 0xff)
	    break;

    if (x > intrh)
    {
	pl.minx = unionl;
	pl.maxx = unionh;

	// use the same one
	return pl;		
    }
	
    // make a new visplane
    visplanes[lastvisplane].height = pl.height;
    visplanes[lastvisplane].picnum = pl.picnum;
    visplanes[lastvisplane].lightlevel = pl.lightlevel;
    
    pl = visplanes[lastvisplane++];
    pl.minx = start;
    pl.maxx = stop;

    //memset (pl->top,0xff,sizeof(pl->top));
    pl.clearTop();
		
    return pl;
}


/**
 * R_MakeSpans
 */

public void MakeSpans
( int		x,
  int		t1,
  int		b1,
  int		t2,
  int		b2 )
{
    while (t1 < t2 && t1<=b1)
    {
	this.MapPlane (t1,spanstart[t1],x-1);
	t1++;
    }
    while (b1 > b2 && b1>=t1)
    {
	this.MapPlane (b1,spanstart[b1],x-1);
	b1--;
    }
	
    while (t2 < t1 && t2<=b2)
    {
	spanstart[t2] = x;
	t2++;
    }
    while (b2 > b1 && b2>=t2)
    {
	spanstart[b2] = x;
	b2--;
    }
}



/**
 * R_DrawPlanes
 * At the end of each frame.
 */
public void DrawPlanes ()
{
    visplane_t		pln; //visplane_t
    int			light;
    int			x;
    int			stop;
    int			angle;
				
if (RANGECHECK){
    if (BSP.ds_p > MAXDRAWSEGS)
	system.Error("R_DrawPlanes: drawsegs overflow (%i)",
	    BSP.ds_p );
    
    if (lastvisplane > MAXVISPLANES)
        system.Error(" R_DrawPlanes: visplane overflow (%i)",
		 lastvisplane);
    
    if (lastopening  > MAXOPENINGS)
        system.Error( "R_DrawPlanes: opening overflow (%i)",
		 lastopening );
}

    for (int pl = 0 ; pl < lastvisplane ;  pln=visplanes[pl++])
    {
	if (pln.minx > pln.maxx)
	    continue;

	
	// sky flat
	if (pln.picnum == DS.skyflatnum)
	{
	    DR.dc_iscale = R.pspriteiscale>>R.detailshift;
	    
	    // Sky is allways drawn full bright,
	    //  i.e. colormaps[0] is used.
	    // Because of this hack, sky is not affected
	    //  by INVUL inverse mapping.
	    DR.dc_colormap = R.colormaps;
	    DR.dc_texturemid = R.kytexturemid;
	    for (x=pln.minx ; x <= pln.maxx ; x++)
	    {
		DR.dc_yl = pln.top[x];
		DR.dc_yh = pln.bottom[x];

		if (DR.dc_yl <= DR.dc_yh)
		{
		    angle = (R.viewangle + xtoviewangle[x])>>ANGLETOSKYSHIFT;
		    DR.dc_x = x;
		    DR.dc_source = R.GetColumn(R.skytexture, angle);
		    colfunc ();
		}
	    }
	    continue;
	}
	
	// regular flat
	ds_source = W_CacheLumpNum(firstflat +
				   flattranslation[pl.picnum],
				   PU_STATIC);
	
	planeheight = abs(pl.height-viewz);
	light = (pl.lightlevel >> LIGHTSEGSHIFT)+extralight;

	if (light >= LIGHTLEVELS)
	    light = LIGHTLEVELS-1;

	if (light < 0)
	    light = 0;

	planezlight = zlight[light];

	pl.top[pl.maxx+1] = 0xff;
	pl.top[pl.minx-1] = 0xff;
		
	stop = pl.maxx + 1;

	for (x=pl.minx ; x<= stop ; x++)
	{
	    R_MakeSpans(x,pl.top[x-1],
			pl.bottom[x-1],
			pl.top[x],
			pl.bottom[x]);
	}
	
	Z_ChangeTag (ds_source, PU_CACHE);
    }
}

}
