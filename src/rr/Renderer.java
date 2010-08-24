package rr;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Renderer.java,v 1.8 2010/08/24 14:57:42 velktron Exp $
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
// $Log: Renderer.java,v $
// Revision 1.8  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
//
// Revision 1.7  2010/08/13 14:06:36  velktron
// Endlevel screen fully functional!
//
// Revision 1.6  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
// Revision 1.5  2010/07/29 15:28:59  velktron
// More work on menus...and digging some dependencies..
//
// Revision 1.4  2010/07/15 14:01:49  velktron
// Added reflector Method stuff for function pointers.
//
// Revision 1.3  2010/07/14 16:12:20  velktron
// A new direction has been taken for the Renderer: instead of making a single, omnipotent "Renderer" object, the functionality will remain split into at least Renderer, Things, Planes and Draw, with a new DoomRendererContext object keeping everything glued together.
//
// Revision 1.2  2010/07/06 12:54:50  velktron
// A lot of work thrown in the renderer, but don't get too excited...
//
// Revision 1.1  2010/07/05 16:18:40  velktron
// YOU DON'T WANNA KNOW
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
//	Rendering main loop and setup functions,
//	 utility functions (BSP, geometry, trigonometry).
//	See tables.c, too.
//  Merges r_data and r_main.
//-----------------------------------------------------------------------------

/*
#include "doomdef.h"
#include "d_net.h"

#include "m_bbox.h"

#include "r_local.h"
#include "r_sky.h"
 */
import java.lang.reflect.Method;

import p.Playfield;

import i.system;
import w.DoomBuffer;
import w.WadLoader;
import data.doomstat;
import doom.player_t;
import doom.thinker_t;
import m.fixed_t;
import static m.fixed_t.*;
import static data.Defines.*;
import static rr.sky.*;
import static data.Tables.*;
import static data.SineCosine.*;
import m.bbox;
public class Renderer{

    private doomstat ds;
    private WadLoader W;
    private Playfield P;
    private BSP BSP;
    
    //
 // Lighting LUT.
 // Used for z-depth cuing per column/row,
 //  and other lighting effects (sector ambient, flash).
 //

 // Lighting constants.
 // Now why not 32 levels here?
 public static int LIGHTLEVELS=16;
 public static int LIGHTSEGSHIFT=4;

 public static int MAXLIGHTSCALE=       48;
 public static int LIGHTSCALESHIFT =    12;
 public static int MAXLIGHTZ      =    128;
 public static int LIGHTZSHIFT    = 20;

// Fineangles in the SCREENWIDTH wide window.
public static int FIELDOFVIEW	=	2048;	
public static int			viewangleoffset;

// increment every time a check is made
public static int			validcount = 1;		

// TODO
lighttable_t[]		fixedcolormap;
//lighttable_t[][]	walllights;

public int			centerx;
public int			centery;

/** fixed_t */
public int centerxfrac,centeryfrac,	projection;

// just for profiling purposes
public  int			framecount;	

public  int			sscount;
public  int			linecount;
public  int			loopcount;

/** fixed_t */
public  int			viewx, viewy,viewz;

//MAES: an exception to strict type safety. These are used only in here, anyway (?) and have no special functions.
//Plus I must use them as indexes. angle_t
public int	viewangle;

public fixed_t			viewcos;
public fixed_t			viewsin;

public player_t		viewplayer;

// 0 = high, 1 = low
public int			detailshift;	

//
// precalculated math tables
//
public static int			clipangle;

// The viewangletox[viewangle + FINEANGLES/4] lookup
// maps the visible view angles to screen X coordinates,
// flattening the arc to a flat projection plane.
// There will be many angles mapped to the same X. 

public static int[]			viewangletox=new int[FINEANGLES/2];

// The xtoviewangleangle[] table maps a screen pixel
// to the lowest viewangle that maps back to x ranges
// from clipangle to -clipangle.
public static int[]			xtoviewangle=new int[SCREENWIDTH+1];


// UNUSED.
// The finetangentgent[angle+FINEANGLES/4] table
// holds the fixed_t tangent values for view angles,
// ranging from MININT to 0 to MAXINT.
// fixed_t		finetangent[FINEANGLES/2];

// fixed_t		finesine[5*FINEANGLES/4];
// MAES: uh oh. So now all these ints must become finesines? fuck that.
// Also wtf @ this hack....this points to approx 1/4th of the finesine table, but what happens if I read past it?
// int[]		finecosine = finesine[FINEANGLES/4];

// TODO:
public lighttable_t[][]		scalelight=new lighttable_t[LIGHTLEVELS][MAXLIGHTSCALE];
public lighttable_t[]		scalelightfixed=new lighttable_t[MAXLIGHTSCALE];
public lighttable_t[][]		zlight=new lighttable_t[LIGHTLEVELS][MAXLIGHTZ];

// bumped light from gun blasts
public static int			extralight;			

/* MAES: Uhm...

void (*colfunc) (void);
void (*basecolfunc) (void);
void (*fuzzcolfunc) (void);
void (*transcolfunc) (void);
void (*spanfunc) (void);
*/

// MAES: HIGHLY experimental. Could it be that easy? :-S
Method colfunc;
Method basecolfunc;
Method fuzzcolfunc;
Method transcolfunc;
Method spanfunc;

// MAES: More renderer fields from segs.

//OPTIMIZE: closed two sided lines as single sided

//True if any of the segs textures might be visible.
boolean     segtextured;    

//False if the back side is the same plane.
boolean     markfloor;  
boolean     markceiling;

boolean     maskedtexture;
int     toptexture;
int     bottomtexture;
int     midtexture;


int     rw_normalangle;
//angle to line origin
int     rw_angle1;  

//
//regular wall
//
int     rw_x;
int     rw_stopx;
int     rw_centerangle;
fixed_t     rw_offset;
fixed_t     rw_distance;
fixed_t     rw_scale;
fixed_t     rw_scalestep;
fixed_t     rw_midtexturemid;
fixed_t     rw_toptexturemid;
fixed_t     rw_bottomtexturemid;

int     worldtop;
int     worldbottom;
int     worldhigh;
int     worldlow;

fixed_t     pixhigh;
fixed_t     pixlow;
fixed_t     pixhighstep;
fixed_t     pixlowstep;

fixed_t     topfrac;
fixed_t     topstep;

fixed_t     bottomfrac;
fixed_t     bottomstep;

// MAES: was **
lighttable_t[]  walllights;

// MAES: was *
short[]      maskedtexturecol;


// MAES: globish-status vars from r_data

int     firstflat;
int     lastflat;
int     numflats;

int     firstpatch;
int     lastpatch;
int     numpatches;

int     firstspritelump;
int     lastspritelump;
int     numspritelumps;

int     numtextures;
texture_t[] textures;

int            texturewidthmask;
// needed for texture pegging
fixed_t        textureheight;      
int[]            texturecompositesize;
short[][]         texturecolumnlump;
char[]    texturecolumnofs;
byte[][]          texturecomposite;

// for global animation
int        flattranslation;
int        texturetranslation;

// needed for pre rendering
fixed_t    spritewidth;    
fixed_t    spriteoffset;
fixed_t    spritetopoffset;

lighttable_t[]    colormaps;


// MAES: Shit taken from things

public static final int MAXVISSPRITES = 128;
public static final int MINZ=(FRACUNIT*4);
public static final int BASEYCENTER     =   100;

//
// Sprite rotation 0 is facing the viewer,
//  rotation 1 is one angle turn CLOCKWISE around the axis.
// This is not the same as the angle,
//  which increases counter clockwise (protractor).
// There was a lot of stuff grabbed wrong, so I changed it...
//

/** fixed_t */
int     pspritescale,pspriteiscale;

lighttable_t[][]    spritelights;

 
/** constant arrays
 *  used for psprite clipping and initializing clipping 
 */
short[]     negonearray=new short[SCREENWIDTH];
short[]     screenheightarray=new short[SCREENWIDTH];

//
// INITIALIZATION FUNCTIONS
//

/** variables used to look up
  *    and range check thing_t sprites patches
  */
spritedef_t[]   sprites;
int     numsprites;

/** variables used to look up
* and range check thing_t sprites patches
*/

spriteframe_t[] sprtemp=new spriteframe_t[29];
int     maxframe;
String      spritename;


// R_AddPointToBox
// Expand a given bbox
// so that it encloses a given point.
// Maes: moved this to bbox.java. Too fugly to keep here!

/*
void
R_AddPointToBox
( int		x,
  int		y,
  fixed_t[]	box )
{
    if (x< box[BOXLEFT])
	box[BOXLEFT] = x;
    if (x> box[BOXRIGHT])
	box[BOXRIGHT] = x;
    if (y< box[BOXBOTTOM])
	box[BOXBOTTOM] = y;
    if (y> box[BOXTOP])
	box[BOXTOP] = y;
}
*/

/**
 * R_PointOnSide
 * Traverse BSP (sub) tree,
 *  check point against partition plane.
 * Returns side 0 (front) or 1 (back).
 * @param x fixed
 * @param y fixed
 * 
 */

public int PointOnSide
( int	x,
  int	y,
  node_t	node )
{
    // MAES: These are used mainly as ints, no need to use fixed_t internally.
    // fixed_t will only be used as a "pass type", but calculations will be done with ints, preferably.
    int	dx; 
    int	dy;
    int	left;
    int	right;
	
    if (node.dx==0)
    {
    if (x <= node.x)
        return (node.dy > 0)?1:0;
    
    return (node.dy < 0)?1:0;
    }
    if (node.dy==0)
    {
    if (y <= node.y)
        return (node.dx < 0)?1:0;
    
    return (node.dx > 0)?1:0;
    }
    
    dx = (x - node.x);
    dy = (y - node.y);
    
    // Try to quickly decide by looking at sign bits.
    if ( ((node.dy ^ node.dx ^ dx ^ dy)&0x80000000 )!=0)
    {
    if  ( ((node.dy ^ dx) & 0x80000000 )!=0)
    {
        // (left is negative)
        return 1;
    }
    return 0;
    }

    left = FixedMul ( node.dy>>FRACBITS , dx );
    right = FixedMul ( dy , node.dx>>FRACBITS );
    
    if (right < left)
    {
    // front side
    return 0;
    }
    // back side
    return 1;           
}


public int
PointOnSegSide
( fixed_t	x,
  fixed_t	y,
  seg_t	line )
{
    int	lx;
    int	ly;
    int	ldx;
    int	ldy;
    int	dx;
    int	dy;
    int left;
    int right;
	
    lx = line.v1.x.val;
    ly = line.v1.y.val;
	
    ldx = line.v2.x.val-lx;
    ldy = line.v2.y.val-ly;
	
    if (ldx==0)
    {
	if (x.val <= lx)
	    return (ldy>0)?1:0;
	
	return (ldy<0)?1:0;
    }
    if (ldy==0)
    {
	if (y.val <= ly)
	    return (ldx < 0)?1:0;
	
	return (ldx > 0)?1:0;
    }
	
    dx=x.val-lx;
    dy=y.val-ly;
	
    // Try to quickly decide by looking at sign bits.
    if ( ((ldy ^ ldx ^ dx ^ dy)&0x80000000 )!=0)
    {
	if  ( ((ldy ^ dx) & 0x80000000 )!=0)
	{
	    // (left is negative)
	    return 1;
	}
	return 0;
    }

    left = FixedMul ( ldy>>FRACBITS , dx );
    right = FixedMul ( dy , ldx>>FRACBITS );
	
    if (right<left)
    {
	// front side
	return 0;
    }
    // back side
    return 1;			
}



/**
 * R_PointToAngle
 *  To get a global angle from cartesian coordinates,
 *  the coordinates are flipped until they are in
 *  the first octant of the coordinate system, then
 *  the y (<=x) is scaled and divided by x to get a
 *  tangent (slope) value which is looked up in the
 *   tantoangle[] table.
 *   
 *   @param xx (fixed_t)
 *   @param yy (fixed_t)
 */

public int
PointToAngle
( int	xx,
  int	yy )
{	
    int x=xx- viewx;
    int y=yy- viewy;
    
    if ( (x==0) && (y==0) )
	return 0;

    if (x>= 0)
    {
	// x >=0
	if (y>= 0)
	{
	    // y>= 0

	    if (x>y)
	    {
		// octant 0
		return tantoangle[ SlopeDiv(y,x)];
	    }
	    else
	    {
		// octant 1
		return ANG90-1-tantoangle[ SlopeDiv(x,y)];
	    }
	}
	else
	{
	    // y<0
	    y = -y;

	    if (x>y)
	    {
		// octant 8
		return -tantoangle[SlopeDiv(y,x)];
	    }
	    else
	    {
		// octant 7
		return ANG270+tantoangle[ SlopeDiv(x,y)];
	    }
	}
    }
    else
    {
	// x<0
	x = -x;

	if (y>= 0)
	{
	    // y>= 0
	    if (x>y)
	    {
		// octant 3
		return ANG180-1-tantoangle[ SlopeDiv(y,x)];
	    }
	    else
	    {
		// octant 2
		return ANG90+ tantoangle[ SlopeDiv(x,y)];
	    }
	}
	else
	{
	    // y<0
	    y = -y;

	    if (x>y)
	    {
		// octant 4
		return ANG180+tantoangle[ SlopeDiv(y,x)];
	    }
	    else
	    {
		 // octant 5
		return ANG270-1-tantoangle[ SlopeDiv(x,y)];
	    }
	}
    }
    // FIXME: Unreachable?!
    //return 0;
}


public int
PointToAngle2
( fixed_t	x1,
  fixed_t	y1,
  fixed_t	x2,
  fixed_t	y2 )
{	
    // Careful with assignments...
    viewx=x1.val;
    viewy=y1.val;
    
    return PointToAngle (x2.val, y2.val);
}

public int
PointToAngle2
( int   x1,
  int   y1,
  int   x2,
  int   y2 )
{   
    // Careful with assignments...
    viewx=x1;
    viewy=y1;
    
    return PointToAngle (x2, y2);
}



public int
PointToDist
( fixed_t	x,
  fixed_t	y )
{
    int		angle;
    int	dx;
    int	dy;
    int	temp;
    int	dist;
	
    dx = Math.abs(x.val - viewx);
    dy = Math.abs(y.val - viewy);
	
    if (dy>dx)
    {
	temp = dx;
	dx = dy;
	dy = temp;
    }
	
    angle = (tantoangle[ FixedDiv(dy,dx)>>DBITS ]+ANG90) >> ANGLETOFINESHIFT;

    // use as cosine
    dist = FixedDiv (dx, finesine[angle] );	
	
    return dist;
}




//
// R_InitPointToAngle
//
public void InitPointToAngle ()
{
    // UNUSED - now getting from tables.c
if (false){
    int	i;
    long	t;
    float	f;
//
// slope (tangent) to angle lookup
//
    for (i=0 ; i<=SLOPERANGE ; i++)
    {
	f = (float) Math.atan( (double)(i/SLOPERANGE )/(3.141592657*2));
	t = (long) (0xffffffffL*f);
	tantoangle[i] = (int) t;
    }
}
}


//
// R_ScaleFromGlobalAngle
// Returns the texture mapping scale
//  for the current line (horizontal span)
//  at the given angle.
// rw_distance must be calculated first.
//
public int ScaleFromGlobalAngle (int visangle)
{
    int		    scale;
    int			anglea;
    int			angleb;
    int			sinea;
    int			sineb;
    int		    num;
    int			den;

    // UNUSED
/*
{
    fixed_t		dist;
    fixed_t		z;
    fixed_t		sinv;
    fixed_t		cosv;
	
    sinv = finesine[(visangle-rw_normalangle)>>ANGLETOFINESHIFT];	
    dist = FixedDiv (rw_distance, sinv);
    cosv = finecosine[(viewangle-visangle)>>ANGLETOFINESHIFT];
    z = abs(FixedMul (dist, cosv));
    scale = FixedDiv(projection, z);
    return scale;
}
*/

    anglea = ANG90 + (visangle-viewangle);
    angleb = ANG90 + (visangle-rw_normalangle);

    // both sines are allways positive
    sinea = finesine[anglea>>ANGLETOFINESHIFT];	
    sineb = finesine[angleb>>ANGLETOFINESHIFT];
    num = FixedMul(projection.val,sineb)<<detailshift;
    den = FixedMul(rw_distance.val,sinea);

    if (den > num>>16)
    {
	scale = FixedDiv (num, den);

	if (scale > 64*FRACUNIT)
	    scale = 64*FRACUNIT;
	else if (scale < 256)
	    scale = 256;
    }
    else
	scale = 64*FRACUNIT;
	
    return scale;
}



//
// R_InitTables
//
public void InitTables ()
{
    // UNUSED: now getting from tables.c
/*
    int		i;
    float	a;
    float	fv;
    int		t;
    
    // viewangle tangent table
    for (i=0 ; i<FINEANGLES/2 ; i++)
    {
	a = (i-FINEANGLES/4+0.5)*PI*2/FINEANGLES;
	fv = FRACUNIT*tan (a);
	t = fv;
	finetangent[i] = t;
    }
    
    // finesine table
    for (i=0 ; i<5*FINEANGLES/4 ; i++)
    {
	// OPTIMIZE: mirror...
	a = (i+0.5)*PI*2/FINEANGLES;
	t = FRACUNIT*sin (a);
	finesine[i] = t;
    }
*/

}



//
// R_InitTextureMapping
//
public void InitTextureMapping ()
{
    int			i;
    int			x;
    int			t;
    int		focallength;
    
    // Use tangent table to generate viewangletox:
    //  viewangletox will give the next greatest x
    //  after the view angle.
    //
    // Calc focallength
    //  so FIELDOFVIEW angles covers SCREENWIDTH.
    focallength = FixedDiv (centerxfrac.val,
			    finetangent[FINEANGLES/4+FIELDOFVIEW/2] );
	
    for (i=0 ; i<FINEANGLES/2 ; i++)
    {
	if (finetangent[i] > FRACUNIT*2)
	    t = -1;
	else if (finetangent[i] < -FRACUNIT*2)
	    t = ds.viewwidth+1;
	else
	{
	    t = FixedMul (finetangent[i], focallength);
	    t = (centerxfrac.val - t+FRACUNIT-1)>>FRACBITS;

	    if (t < -1)
		t = -1;
	    else if (t>ds.viewwidth+1)
		t = ds.viewwidth+1;
	}
	viewangletox[i] = t;
    }
    
    // Scan viewangletox[] to generate xtoviewangle[]:
    //  xtoviewangle will give the smallest view angle
    //  that maps to x.	
    for (x=0;x<=ds.viewwidth;x++)
    {
	i = 0;
	while (viewangletox[i]>x)
	    i++;
	xtoviewangle[x] = (i<<ANGLETOFINESHIFT)-ANG90;
    }
    
    // Take out the fencepost cases from viewangletox.
    for (i=0 ; i<FINEANGLES/2 ; i++)
    {
	t = FixedMul (finetangent[i], focallength);
	t = centerx - t;
	
	if (viewangletox[i] == -1)
	    viewangletox[i] = 0;
	else if (viewangletox[i] == ds.viewwidth+1)
	    viewangletox[i]  = ds.viewwidth;
    }
	
    clipangle = xtoviewangle[0];
}



//
// R_InitLightTables
// Only inits the zlight table,
//  because the scalelight table changes with view size.
//
protected static int DISTMAP	=	2;

public void InitLightTables ()
{
    int		i;
    int		j;
    int		level;
    int		startmap; 	
    int		scale;
    
    // Calculate the light levels to use
    //  for each level / distance combination.
    for (i=0 ; i< LIGHTLEVELS ; i++)
    {
	startmap = ((LIGHTLEVELS-1-i)*2)*NUMCOLORMAPS/LIGHTLEVELS;
	for (j=0 ; j<MAXLIGHTZ ; j++)
	{
	    scale = FixedDiv ((SCREENWIDTH/2*FRACUNIT), (j+1)<<LIGHTZSHIFT);
	    scale >>= LIGHTSCALESHIFT;
	    level = startmap - scale/DISTMAP;
	    
	    if (level < 0)
		level = 0;

	    if (level >= NUMCOLORMAPS)
		level = NUMCOLORMAPS-1;

	    zlight[i][j] = colormaps[level*256];
	}
    }
}



//
// R_SetViewSize
// Do not really change anything here,
//  because it might be in the middle of a refresh.
// The change will take effect next refresh.
//
public boolean		setsizeneeded;
int		setblocks;
int		setdetail;


void
SetViewSize
( int		blocks,
  int		detail )
{
    setsizeneeded = true;
    setblocks = blocks;
    setdetail = detail;
}


//
// R_ExecuteSetViewSize
//

public void ExecuteSetViewSize ()
{
    int	cosadj;
    int	dy;
    int		i;
    int		j;
    int		level;
    int		startmap; 	

    setsizeneeded = false;

    if (setblocks == 11)
    {
	ds.scaledviewwidth = SCREENWIDTH;
	ds.viewheight = SCREENHEIGHT;
    }
    else
    {
	ds.scaledviewwidth = setblocks*32;
	ds.viewheight = (short) ((setblocks*168/10)&~7);
    }
    
    detailshift = setdetail;
    ds.viewwidth = ds.scaledviewwidth>>detailshift;
	
    centery = ds.viewheight/2;
    centerx = ds.viewwidth/2;
    centerxfrac.set(centerx<<FRACBITS);
    centeryfrac.set(centery<<FRACBITS);
    projection.copy(centerxfrac);

    
    if (detailshift!=0)
    {
	colfunc = basecolfunc = Draw.class.getDeclaredMethod("DrawColumn", Void.class);
	fuzzcolfunc = Draw.class.getDeclaredMethod("DrawFuzzColumn", Void.class);
	transcolfunc = Draw.class.getDeclaredMethod("DrawTranslatedColumn", Void.class);
	spanfunc = Draw.class.getDeclaredMethod("DrawSpan;", Void.class);
    }
    else
    {
	colfunc = basecolfunc = Draw.class.getDeclaredMethod("DrawColumnLow",Void.class);
	fuzzcolfunc = Draw.class.getDeclaredMethod("DrawFuzzColumn",Void.class);
	transcolfunc = Draw.class.getDeclaredMethod("DrawTranslatedColumn",Void.class);
	spanfunc = Draw.class.getDeclaredMethod("DrawSpanLow",Void.class);
    }

    this.InitBuffer (ds.scaledviewwidth, ds.viewheight);
	
    this.InitTextureMapping ();
    
    // psprite scales
    pspritescale.set(FRACUNIT*ds.viewwidth/SCREENWIDTH);
    pspriteiscale.set(FRACUNIT*SCREENWIDTH/ds.viewwidth);
    
    // thing clipping
    for (i=0 ; i<ds.viewwidth ; i++)
	screenheightarray[i] = (short) ds.viewheight;
    
    // planes
    for (i=0 ; i<ds.viewheight ; i++)
    {
	dy = ((i-ds.viewheight/2)<<FRACBITS)+FRACUNIT/2;
	dy = Math.abs(dy);
	// MAES: yslope is a field in "r_plane.c" so it should really be in the Rendering Context.
	ds.yslope[i] = FixedDiv ( (ds.viewwidth<<detailshift)/2*FRACUNIT, dy);
    }
	
    for (i=0 ; i<ds.viewwidth ; i++)
    {
	cosadj = abs(finecosine[xtoviewangle[i]>>ANGLETOFINESHIFT]);
	distscale[i] = FixedDiv (FRACUNIT,cosadj);
    }
    
    // Calculate the light levels to use
    //  for each level / scale combination.
    for (i=0 ; i< LIGHTLEVELS ; i++)
    {
	startmap = ((LIGHTLEVELS-1-i)*2)*NUMCOLORMAPS/LIGHTLEVELS;
	for (j=0 ; j<MAXLIGHTSCALE ; j++)
	{
	    level = startmap - j*SCREENWIDTH/(ds.viewwidth<<detailshift)/DISTMAP;
	    
	    if (level < 0)
		level = 0;

	    if (level >= NUMCOLORMAPS)
		level = NUMCOLORMAPS-1;

	    scalelight[i][j] = colormaps + level*256;
	}
    }
}



//
// R_Init
//
public int	detailLevel;
public int	screenblocks;

void Init ()
{
    InitData ();
    System.out.print("\nR_InitData");
    InitPointToAngle ();
    System.out.print("\nR_InitPointToAngle");
    InitTables ();
    // ds.viewwidth / ds.viewheight / detailLevel are set by the defaults
    System.out.print ("\nR_InitTables");

    SetViewSize (screenblocks, detailLevel);
    InitPlanes ();
    System.out.print ("\nR_InitPlanes");
    InitLightTables ();
    System.out.print("\nR_InitLightTables");
    InitSkyMap ();
    System.out.print("\nR_InitSkyMap");
    InitTranslationTables ();
    System.out.print("\nR_InitTranslationsTables");
	
    framecount = 0;
}


/**
 * R_PointInSubsector
 * 
 * @param x fixed
 * @param y fixed
 * 
 */
public subsector_t
PointInSubsector
( int	x,
  int	y )
{
    node_t	node;
    int		side;
    int		nodenum;

    // single subsector is a special case
    if (P.numnodes==0)				
	return P.subsectors[0];
		
    nodenum = P.numnodes-1;

    while ((nodenum & NF_SUBSECTOR)==0 )
    {
	node = P.nodes[nodenum];
	side = PointOnSide (x, y, node);
	nodenum = node.children[side];
    }
	
    return P.subsectors[nodenum & ~NF_SUBSECTOR];
}



//
// R_SetupFrame
//
public void SetupFrame (player_t player)
{		
    int		i;
    
    viewplayer = player;
    viewx = player.mo.x;
    viewy = player.mo.y;
    viewangle = player.mo.angle + viewangleoffset;
    extralight = player.extralight;

    viewz = player.viewz;
    
    viewsin = finesine[viewangle>>ANGLETOFINESHIFT];
    viewcos = finecosine[viewangle>>ANGLETOFINESHIFT];
	
    sscount = 0;
	
    if (player.fixedcolormap)
    {
	fixedcolormap =
	    colormaps
	    + player.fixedcolormap*256*sizeof(lighttable_t);
	
	walllights = scalelightfixed;

	for (i=0 ; i<MAXLIGHTSCALE ; i++)
	    scalelightfixed[i] = fixedcolormap;
    }
    else
	fixedcolormap = 0;
		
    framecount++;
    validcount++;
}



//
// R_RenderView
//
public void RenderPlayerView (player_t player)
{	
    SetupFrame (player);

    // Clear buffers.
    ClearClipSegs ();
    ClearDrawSegs ();
    ClearPlanes ();
    ClearSprites ();
    
    // TODO: check for new console commands.
    //NetUpdate ();

    // The head node is the last node output.
    RenderBSPNode (numnodes-1);
    
    // Check for new console commands.
    //NetUpdate ();
    
    DrawPlanes ();
    
    // Check for new console commands.
    //NetUpdate ();
    
    DrawMasked ();

    // Check for new console commands.
   // NetUpdate ();				
}




//
// R_PrecacheLevel
// Preloads all relevant graphics for the level.
//
int     flatmemory;
int     texturememory;
int     spritememory;

public void PrecacheLevel ()
{
    boolean[]       flatpresent;
    boolean[]       texturepresent;
    boolean[]       spritepresent;

    int         i;
    int         j;
    int         k;
    int         lump;
    
    texture_t      texture;
    thinker_t      th;
    spriteframe_t  sf;

    if (ds.demoplayback)
    return;
    
    // Precache flats.
    flatpresent = new boolean[numflats];        

    for (i=0 ; i<P.numsectors ; i++)
    {
    flatpresent[P.sectors[i].floorpic] = true;
    flatpresent[P.sectors[i].ceilingpic] = true;
    }
    
    flatmemory = 0;

    for (i=0 ; i<numflats ; i++)
    {
    if (flatpresent[i])
    {
        lump = firstflat + i;
        flatmemory += W.lumpinfo[lump].size;
        // Force reading from disk and placement into a doombuffer.
        W.CacheLumpNum(lump, PU_CACHE, null);
    }
    }
    
    // Precache textures.
    texturepresent = new boolean[numtextures];
    
    for (i=0 ; i<P.numsides ; i++)
    {
    texturepresent[P.sides[i].toptexture] = true;
    texturepresent[P.sides[i].midtexture] = true;
    texturepresent[P.sides[i].bottomtexture] = true;
    }

    // Sky texture is always present.
    // Note that F_SKY1 is the name used to
    //  indicate a sky floor/ceiling as a flat,
    //  while the sky texture is stored like
    //  a wall texture, with an episode dependend
    //  name.
    texturepresent[skytexture] = true;
    
    texturememory = 0;
    for (i=0 ; i<numtextures ; i++)
    {
    if (!texturepresent[i])
        continue;

    texture = textures[i];
    
    for (j=0 ; j<texture.patchcount ; j++)
    {
        lump = texture.patches[j].patch;
        texturememory += W.lumpinfo[lump].size;
        // We're supposed to be reading patches here.
        W.CacheLumpNum(lump , PU_CACHE,patch_t.class);
    }
    }
    
    // Precache sprites.
    spritepresent = new boolean[numsprites];
    
    for (th = thinkercap.next ; th != &thinkercap ; th=th.next)
    {
    if (th.function.acp1 == (actionf_p1)P_MobjThinker)
        spritepresent[((mobj_t *)th).sprite] = 1;
    }
    
    spritememory = 0;
    for (i=0 ; i<numsprites ; i++)
    {
    if (!spritepresent[i])
        continue;

    for (j=0 ; j<sprites[i].numframes ; j++)
    {
        sf = &sprites[i].spriteframes[j];
        for (k=0 ; k<8 ; k++)
        {
        lump = firstspritelump + sf.lump[k];
        spritememory += lumpinfo[lump].size;
        W_CacheLumpNum(lump , PU_CACHE);
        }
    }
    }
}


/** R_GetColumn
 *  MAES: This should probably return column data instead of byte* or byte[]...I think.
 *  Must see how it's actually used. 
 */

public column_t
GetColumn
( int       tex,
int       col )
{
 int     lump;
 int     ofs;
 
 col &= texturewidthmask[tex];
 lump = texturecolumnlump[tex][col];
 ofs = texturecolumnofs[tex][col];
 
 if (lump > 0)
 return (byte *)W_CacheLumpNum(lump,PU_CACHE)+ofs;

 if (!texturecomposite[tex])
 R_GenerateComposite (tex);

 return texturecomposite[tex] + ofs;
}




//
//R_InitTextures
//Initializes the texture list
//with the textures from the world map.
//
public void InitTextures ()
{
 maptexture_t   mtexture;
 texture_t      texture;
 mappatch_t     mpatch;
 texpatch_t     patch;

 int         i;
 int         j;

 // MAES: these were pointers.
 int        maptex;
 int        maptex2
 int        maptex1;
 
 char[]        name=new char[9];
 
 // MAES: more pointers.
 char       names;
 char       name_p;
 int        patchlookup;
 
 int         totalwidth;
 int         nummappatches;
 int         offset;
 int         maxoff;
 int         maxoff2;
 int         numtextures1;
 int         numtextures2;

 // MAES: also a pointer...
 int        directory;
 
 int         temp1;
 int         temp2;
 int         temp3;

 
 // Load the patch names from pnames.lmp.
 name[8] = 0;    
 names = W.CacheLumpName ("PNAMES", PU_STATIC);
 nummappatches = LONG ( *((int *)names) );
 name_p = names+4;
 patchlookup = alloca (nummappatches*sizeof(*patchlookup));
 
 for (i=0 ; i<nummappatches ; i++)
 {
 strncpy (name,name_p+i*8, 8);
 patchlookup[i] = W_CheckNumForName (name);
 }
 Z_Free (names);
 
 // Load the map texture definitions from textures.lmp.
 // The data is contained in one or two lumps,
 //  TEXTURE1 for shareware, plus TEXTURE2 for commercial.
 maptex = maptex1 = W_CacheLumpName ("TEXTURE1", PU_STATIC);
 numtextures1 = LONG(*maptex);
 maxoff = W_LumpLength (W_GetNumForName ("TEXTURE1"));
 directory = maptex+1;
 
 if (W_CheckNumForName ("TEXTURE2") != -1)
 {
 maptex2 = W_CacheLumpName ("TEXTURE2", PU_STATIC);
 numtextures2 = LONG(*maptex2);
 maxoff2 = W_LumpLength (W_GetNumForName ("TEXTURE2"));
 }
 else
 {
 maptex2 = NULL;
 numtextures2 = 0;
 maxoff2 = 0;
 }
 numtextures = numtextures1 + numtextures2;
 
 textures = Z_Malloc (numtextures*4, PU_STATIC, 0);
 texturecolumnlump = Z_Malloc (numtextures*4, PU_STATIC, 0);
 texturecolumnofs = Z_Malloc (numtextures*4, PU_STATIC, 0);
 texturecomposite = Z_Malloc (numtextures*4, PU_STATIC, 0);
 texturecompositesize = Z_Malloc (numtextures*4, PU_STATIC, 0);
 texturewidthmask = Z_Malloc (numtextures*4, PU_STATIC, 0);
 textureheight = Z_Malloc (numtextures*4, PU_STATIC, 0);

 totalwidth = 0;
 
 //  Really complex printing shit...
 temp1 = W_GetNumForName ("S_START");  // P_???????
 temp2 = W_GetNumForName ("S_END") - 1;
 temp3 = ((temp2-temp1+63)/64) + ((numtextures+63)/64);
 printf("[");
 for (i = 0; i < temp3; i++)
 printf(" ");
 printf("         ]");
 for (i = 0; i < temp3; i++)
 printf("\x8");
 printf("\x8\x8\x8\x8\x8\x8\x8\x8\x8\x8");   
 
 for (i=0 ; i<numtextures ; i++, directory++)
 {
 if (!(i&63))
     printf (".");

 if (i == numtextures1)
 {
     // Start looking in second texture file.
     maptex = maptex2;
     maxoff = maxoff2;
     directory = maptex+1;
 }
     
 offset = LONG(*directory);

 if (offset > maxoff)
     I_Error ("R_InitTextures: bad texture directory");
 
 mtexture = (maptexture_t *) ( (byte *)maptex + offset);

 texture = textures[i] =
     Z_Malloc (sizeof(texture_t)
           + sizeof(texpatch_t)*(SHORT(mtexture.patchcount)-1),
           PU_STATIC, 0);
 
 texture.width = SHORT(mtexture.width);
 texture.height = SHORT(mtexture.height);
 texture.patchcount = SHORT(mtexture.patchcount);

 memcpy (texture.name, mtexture.name, sizeof(texture.name));
 mpatch = &mtexture.patches[0];
 patch = &texture.patches[0];

 for (j=0 ; j<texture.patchcount ; j++, mpatch++, patch++)
 {
     patch.originx = SHORT(mpatch.originx);
     patch.originy = SHORT(mpatch.originy);
     patch.patch = patchlookup[SHORT(mpatch.patch)];
     if (patch.patch == -1)
     {
     I_Error ("R_InitTextures: Missing patch in texture %s",
          texture.name);
     }
 }       
 texturecolumnlump[i] = Z_Malloc (texture.width*2, PU_STATIC,0);
 texturecolumnofs[i] = Z_Malloc (texture.width*2, PU_STATIC,0);

 j = 1;
 while (j*2 <= texture.width)
     j<<=1;

 texturewidthmask[i] = j-1;
 textureheight[i] = texture.height<<FRACBITS;
     
 totalwidth += texture.width;
 }

 Z_Free (maptex1);
 if (maptex2)
 Z_Free (maptex2);
 
 // Precalculate whatever possible.  
 for (i=0 ; i<numtextures ; i++)
 R_GenerateLookup (i);
 
 // Create translation table for global animation.
 texturetranslation = Z_Malloc ((numtextures+1)*4, PU_STATIC, 0);
 
 for (i=0 ; i<numtextures ; i++)
 texturetranslation[i] = i;
}

//
//R_FlatNumForName
//Retrieval, get a flat number for a flat name.
//
public int FlatNumForName (String name)
{
int     i;
i = W.CheckNumForName (name);

if (i == -1)
{
system.Error ("R_FlatNumForName: %s not found",name);
}
return i - firstflat;
}

/*

//
//R_InitFlats
//
void R_InitFlats (void)
{
 int     i;
 
 firstflat = W_GetNumForName ("F_START") + 1;
 lastflat = W_GetNumForName ("F_END") - 1;
 numflats = lastflat - firstflat + 1;
 
 // Create translation table for global animation.
 flattranslation = Z_Malloc ((numflats+1)*4, PU_STATIC, 0);
 
 for (i=0 ; i<numflats ; i++)
 flattranslation[i] = i;
}


//
//R_InitSpriteLumps
//Finds the width and hoffset of all sprites in the wad,
//so the sprite does not need to be cached completely
//just for having the header info ready during rendering.
//
void R_InitSpriteLumps (void)
{
 int     i;
 patch_t *patch;
 
 firstspritelump = W_GetNumForName ("S_START") + 1;
 lastspritelump = W_GetNumForName ("S_END") - 1;
 
 numspritelumps = lastspritelump - firstspritelump + 1;
 spritewidth = Z_Malloc (numspritelumps*4, PU_STATIC, 0);
 spriteoffset = Z_Malloc (numspritelumps*4, PU_STATIC, 0);
 spritetopoffset = Z_Malloc (numspritelumps*4, PU_STATIC, 0);
 
 for (i=0 ; i< numspritelumps ; i++)
 {
 if (!(i&63))
     printf (".");

 patch = W_CacheLumpNum (firstspritelump+i, PU_CACHE);
 spritewidth[i] = SHORT(patch.width)<<FRACBITS;
 spriteoffset[i] = SHORT(patch.leftoffset)<<FRACBITS;
 spritetopoffset[i] = SHORT(patch.topoffset)<<FRACBITS;
 }
}



//
//R_InitColormaps
//
void R_InitColormaps (void)
{
 int lump, length;
 
 // Load in the light tables, 
 //  256 byte align tables.
 lump = W_GetNumForName("COLORMAP"); 
 length = W_LumpLength (lump) + 255; 
 colormaps = Z_Malloc (length, PU_STATIC, 0); 
 colormaps = (byte *)( ((int)colormaps + 255)&~0xff); 
 W_ReadLump (lump,colormaps); 
}



//
//R_InitData
//Locates all the lumps
//that will be used by all views
//Must be called after W_Init.
//
void R_InitData (void)
{
 R_InitTextures ();
 printf ("\nInitTextures");
 R_InitFlats ();
 printf ("\nInitFlats");
 R_InitSpriteLumps ();
 printf ("\nInitSprites");
 R_InitColormaps ();
 printf ("\nInitColormaps");
}








//
//R_CheckTextureNumForName
//Check whether texture is available.
//Filter out NoTexture indicator.
//
int R_CheckTextureNumForName (char *name)
{
 int     i;

 // "NoTexture" marker.
 if (name[0] == '-')     
 return 0;
     
 for (i=0 ; i<numtextures ; i++)
 if (!strncasecmp (textures[i].name, name, 8) )
     return i;
     
 return -1;
}



//
//R_TextureNumForName
//Calls R_CheckTextureNumForName,
//aborts with error message.
//
int R_TextureNumForName (char* name)
{
 int     i;
 
 i = R_CheckTextureNumForName (name);

 if (i==-1)
 {
 I_Error ("R_TextureNumForName: %s not found",
      name);
 }
 return i;
}

//
//R_GenerateLookup
//
void R_GenerateLookup (int texnum)
{
 texture_t*      texture;
 byte*       patchcount; // patchcount[texture.width]
 texpatch_t*     patch;  
 patch_t*        realpatch;
 int         x;
 int         x1;
 int         x2;
 int         i;
 short*      collump;
 unsigned short* colofs;
 
 texture = textures[texnum];

 // Composited texture not created yet.
 texturecomposite[texnum] = 0;
 
 texturecompositesize[texnum] = 0;
 collump = texturecolumnlump[texnum];
 colofs = texturecolumnofs[texnum];
 
 // Now count the number of columns
 //  that are covered by more than one patch.
 // Fill in the lump / offset, so columns
 //  with only a single patch are all done.
 patchcount = (byte *)alloca (texture.width);
 memset (patchcount, 0, texture.width);
 patch = texture.patches;
     
 for (i=0 , patch = texture.patches;
  i<texture.patchcount;
  i++, patch++)
 {
 realpatch = W_CacheLumpNum (patch.patch, PU_CACHE);
 x1 = patch.originx;
 x2 = x1 + SHORT(realpatch.width);
 
 if (x1 < 0)
     x = 0;
 else
     x = x1;

 if (x2 > texture.width)
     x2 = texture.width;
 for ( ; x<x2 ; x++)
 {
     patchcount[x]++;
     collump[x] = patch.patch;
     colofs[x] = LONG(realpatch.columnofs[x-x1])+3;
 }
 }
 
 for (x=0 ; x<texture.width ; x++)
 {
 if (!patchcount[x])
 {
     printf ("R_GenerateLookup: column without a patch (%s)\n",
         texture.name);
     return;
 }
 // I_Error ("R_GenerateLookup: column without a patch");
 
 if (patchcount[x] > 1)
 {
     // Use the cached block.
     collump[x] = -1;    
     colofs[x] = texturecompositesize[texnum];
     
     if (texturecompositesize[texnum] > 0x10000-texture.height)
     {
     I_Error ("R_GenerateLookup: texture %i is >64k",
          texnum);
     }
     
     texturecompositesize[texnum] += texture.height;
 }
 }   
}



//
// MAPTEXTURE_T CACHING
// When a texture is first needed,
//  it counts the number of composite columns
//  required in the texture and allocates space
//  for a column directory and any new columns.
// The directory will simply point inside other patches
//  if there is only one patch in a given column,
//  but any columns with multiple patches
//  will have new column_ts generated.
//



//
// R_DrawColumnInCache
// Clip and draw a column
//  from a patch into a cached post.
//
void
R_DrawColumnInCache
( column_t* patch,
  byte*     cache,
  int       originy,
  int       cacheheight )
{
    int     count;
    int     position;
    byte*   source;
    byte*   dest;
    
    dest = (byte *)cache + 3;
    
    while (patch.topdelta != 0xff)
    {
    source = (byte *)patch + 3;
    count = patch.length;
    position = originy + patch.topdelta;

    if (position < 0)
    {
        count += position;
        position = 0;
    }

    if (position + count > cacheheight)
        count = cacheheight - position;

    if (count > 0)
        memcpy (cache + position, source, count);
        
    patch = (column_t *)(  (byte *)patch + patch.length + 4); 
    }
}

*/

//
// R_GenerateComposite
// Using the texture definition,
//  the composite texture is created from the patches,
//  and each column is cached.
//
public void GenerateComposite (int texnum)
{
    byte[]       block;
    texture_t      texture;
    texpatch_t     patch;  
    patch_t        realpatch;
    int         x;
    int         x1;
    int         x2;
    int         i;
    column_t       patchcol; // MAES: does this really store COMPRESSED columns?
    short[]      collump;
    char[] colofs; // unsigned short
    
    texture = textures[texnum];

    block = new Z_Malloc (texturecompositesize[texnum],
              PU_STATIC, 
              texturecomposite[texnum]);   

    collump = texturecolumnlump[texnum];
    colofs = texturecolumnofs[texnum];
    
    // Composite the columns together.
    patch = texture.patches;
        
    for (i=0 , patch = texture.patches;
     i<texture.patchcount;
     i++, patch++)
    {
    realpatch = W_CacheLumpNum (patch.patch, PU_CACHE);
    x1 = patch.originx;
    x2 = x1 + SHORT(realpatch.width);

    if (x1<0)
        x = 0;
    else
        x = x1;
    
    if (x2 > texture.width)
        x2 = texture.width;

    for ( ; x<x2 ; x++)
    {
        // Column does not have multiple patches?
        if (collump[x] >= 0)
        continue;
        
        patchcol = (column_t *)((byte *)realpatch
                    + LONG(realpatch.columnofs[x-x1]));
        R_DrawColumnInCache (patchcol,
                 block + colofs[x],
                 patch.originy,
                 texture.height);
    }
                        
    }

    // Now that the texture has been built in column cache,
    //  it is purgable from zone memory.
    Z_ChangeTag (block, PU_CACHE);
}

// From r_sky.c



int skyflatnum;
int skytexture;
int skytexturemid;


/**
 * R_InitSkyMap
 * Called whenever the view size changes.
 */

public void InitSkyMap ()
{
    skyflatnum = FlatNumForName ( SKYFLATNAME );
    skytexturemid = 100*FRACUNIT;
}

////////////////////////////////// from r_segs ///////////////////////////////////

//OPTIMIZE: closed two sided lines as single sided

//True if any of the segs textures might be visible.
boolean     segtextured;    

//False if the back side is the same plane.
boolean     markfloor;  
boolean     markceiling;

boolean     maskedtexture;
int     toptexture;
int     bottomtexture;
int     midtexture;


angle_t     rw_normalangle;
//angle to line origin
int     rw_angle1;  

//
//regular wall
//
int     rw_x;
int     rw_stopx;
angle_t     rw_centerangle;
fixed_t     rw_offset;
fixed_t     rw_distance;
fixed_t     rw_scale;
fixed_t     rw_scalestep;
fixed_t     rw_midtexturemid;
fixed_t     rw_toptexturemid;
fixed_t     rw_bottomtexturemid;

int     worldtop;
int     worldbottom;
int     worldhigh;
int     worldlow;

fixed_t     pixhigh;
fixed_t     pixlow;
fixed_t     pixhighstep;
fixed_t     pixlowstep;

fixed_t     topfrac;
fixed_t     topstep;

fixed_t     bottomfrac;
fixed_t     bottomstep;


lighttable_t**  walllights;

short*      maskedtexturecol;



//
//R_RenderMaskedSegRange
//
void
R_RenderMaskedSegRange
( drawseg_t*    ds,
int       x1,
int       x2 )
{
 unsigned    index;
 column_t*   col;
 int     lightnum;
 int     texnum;
 
 // Calculate light table.
 // Use different light tables
 //   for horizontal / vertical / diagonal. Diagonal?
 // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
 curline = ds.curline;
 frontsector = curline.frontsector;
 backsector = curline.backsector;
 texnum = texturetranslation[curline.sidedef.midtexture];
 
 lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT)+extralight;

 if (curline.v1.y == curline.v2.y)
 lightnum--;
 else if (curline.v1.x == curline.v2.x)
 lightnum++;

 if (lightnum < 0)       
 walllights = scalelight[0];
 else if (lightnum >= LIGHTLEVELS)
 walllights = scalelight[LIGHTLEVELS-1];
 else
 walllights = scalelight[lightnum];

 maskedtexturecol = ds.maskedtexturecol;

 rw_scalestep = ds.scalestep;       
 spryscale = ds.scale1 + (x1 - ds.x1)*rw_scalestep;
 mfloorclip = ds.sprbottomclip;
 mceilingclip = ds.sprtopclip;
 
 // find positioning
 if (curline.linedef.flags & ML_DONTPEGBOTTOM)
 {
 dc_texturemid = frontsector.floorheight > backsector.floorheight
     ? frontsector.floorheight : backsector.floorheight;
 dc_texturemid = dc_texturemid + textureheight[texnum] - viewz;
 }
 else
 {
 dc_texturemid =frontsector.ceilingheight<backsector.ceilingheight
     ? frontsector.ceilingheight : backsector.ceilingheight;
 dc_texturemid = dc_texturemid - viewz;
 }
 dc_texturemid += curline.sidedef.rowoffset;
         
 if (fixedcolormap)
 dc_colormap = fixedcolormap;
 
 // draw the columns
 for (dc_x = x1 ; dc_x <= x2 ; dc_x++)
 {
 // calculate lighting
 if (maskedtexturecol[dc_x] != MAXSHORT)
 {
     if (!fixedcolormap)
     {
     index = spryscale>>LIGHTSCALESHIFT;

     if (index >=  MAXLIGHTSCALE )
         index = MAXLIGHTSCALE-1;

     dc_colormap = walllights[index];
     }
         
     sprtopscreen = centeryfrac - FixedMul(dc_texturemid, spryscale);
     dc_iscale = 0xffffffffu / (unsigned)spryscale;
     
     // draw the texture
     col = (column_t *)( 
     (byte *)R_GetColumn(texnum,maskedtexturecol[dc_x]) -3);
         
     R_DrawMaskedColumn (col);
     maskedtexturecol[dc_x] = MAXSHORT;
 }
 spryscale += rw_scalestep;
 }
 
}




//
//R_RenderSegLoop
//Draws zero, one, or two textures (and possibly a masked
//texture) for walls.
//Can draw or mark the starting pixel of floor and ceiling
//textures.
//CALLED: CORE LOOPING ROUTINE.
//
#define HEIGHTBITS      12
#define HEIGHTUNIT      (1<<HEIGHTBITS)

void R_RenderSegLoop (void)
{
 angle_t     angle;
 unsigned        index;
 int         yl;
 int         yh;
 int         mid;
 fixed_t     texturecolumn;
 int         top;
 int         bottom;

 //texturecolumn = 0;                // shut up compiler warning
 
 for ( ; rw_x < rw_stopx ; rw_x++)
 {
 // mark floor / ceiling areas
 yl = (topfrac+HEIGHTUNIT-1)>>HEIGHTBITS;

 // no space above wall?
 if (yl < ceilingclip[rw_x]+1)
     yl = ceilingclip[rw_x]+1;
 
 if (markceiling)
 {
     top = ceilingclip[rw_x]+1;
     bottom = yl-1;

     if (bottom >= floorclip[rw_x])
     bottom = floorclip[rw_x]-1;

     if (top <= bottom)
     {
     ceilingplane.top[rw_x] = top;
     ceilingplane.bottom[rw_x] = bottom;
     }
 }
     
 yh = bottomfrac>>HEIGHTBITS;

 if (yh >= floorclip[rw_x])
     yh = floorclip[rw_x]-1;

 if (markfloor)
 {
     top = yh+1;
     bottom = floorclip[rw_x]-1;
     if (top <= ceilingclip[rw_x])
     top = ceilingclip[rw_x]+1;
     if (top <= bottom)
     {
     floorplane.top[rw_x] = top;
     floorplane.bottom[rw_x] = bottom;
     }
 }
 
 // texturecolumn and lighting are independent of wall tiers
 if (segtextured)
 {
     // calculate texture offset
     angle = (rw_centerangle + xtoviewangle[rw_x])>>ANGLETOFINESHIFT;
     texturecolumn = rw_offset-FixedMul(finetangent[angle],rw_distance);
     texturecolumn >>= FRACBITS;
     // calculate lighting
     index = rw_scale>>LIGHTSCALESHIFT;

     if (index >=  MAXLIGHTSCALE )
     index = MAXLIGHTSCALE-1;

     dc_colormap = walllights[index];
     dc_x = rw_x;
     dc_iscale = 0xffffffffu / (unsigned)rw_scale;
 }
 
 // draw the wall tiers
 if (midtexture)
 {
     // single sided line
     dc_yl = yl;
     dc_yh = yh;
     dc_texturemid = rw_midtexturemid;
     dc_source = R_GetColumn(midtexture,texturecolumn);
     colfunc ();
     ceilingclip[rw_x] = viewheight;
     floorclip[rw_x] = -1;
 }
 else
 {
     // two sided line
     if (toptexture)
     {
     // top wall
     mid = pixhigh>>HEIGHTBITS;
     pixhigh += pixhighstep;

     if (mid >= floorclip[rw_x])
         mid = floorclip[rw_x]-1;

     if (mid >= yl)
     {
         dc_yl = yl;
         dc_yh = mid;
         dc_texturemid = rw_toptexturemid;
         dc_source = R_GetColumn(toptexture,texturecolumn);
         colfunc ();
         ceilingclip[rw_x] = mid;
     }
     else
         ceilingclip[rw_x] = yl-1;
     }
     else
     {
     // no top wall
     if (markceiling)
         ceilingclip[rw_x] = yl-1;
     }
         
     if (bottomtexture)
     {
     // bottom wall
     mid = (pixlow+HEIGHTUNIT-1)>>HEIGHTBITS;
     pixlow += pixlowstep;

     // no space above wall?
     if (mid <= ceilingclip[rw_x])
         mid = ceilingclip[rw_x]+1;
     
     if (mid <= yh)
     {
         dc_yl = mid;
         dc_yh = yh;
         dc_texturemid = rw_bottomtexturemid;
         dc_source = R_GetColumn(bottomtexture,
                     texturecolumn);
         colfunc ();
         floorclip[rw_x] = mid;
     }
     else
         floorclip[rw_x] = yh+1;
     }
     else
     {
     // no bottom wall
     if (markfloor)
         floorclip[rw_x] = yh+1;
     }
         
     if (maskedtexture)
     {
     // save texturecol
     //  for backdrawing of masked mid texture
     maskedtexturecol[rw_x] = texturecolumn;
     }
 }
     
 rw_scale += rw_scalestep;
 topfrac += topstep;
 bottomfrac += bottomstep;
 }
}




//
//R_StoreWallRange
//A wall segment will be drawn
//between start and stop pixels (inclusive).
//
public void StoreWallRange ( int   start,
int   stop )
{
 int     hyp,sineval; // fixed
 int     distangle, offsetangle; // angle_t
 int     vtop; // fixed
 int         lightnum;

 // don't overflow and crash
 if (ds_p == drawsegs[MAXDRAWSEGS])
 return;     
     
if (RANGECHECK) {
 if (start >=viewwidth || start > stop)
 system.Error ("Bad R_RenderWallRange: %i to %i", start , stop);
}
 
 sidedef = curline.sidedef;
 linedef = curline.linedef;

 // mark the segment as visible for auto map
 linedef.flags |= ML_MAPPED;
 
 // calculate rw_distance for scale calculation
 rw_normalangle = curline.angle + ANG90;
 offsetangle = abs(rw_normalangle-rw_angle1);
 
 if (offsetangle > ANG90)
 offsetangle = ANG90;

 distangle = ANG90 - offsetangle;
 hyp = R_PointToDist (curline.v1.x, curline.v1.y);
 sineval = finesine[distangle>>ANGLETOFINESHIFT];
 rw_distance = FixedMul (hyp, sineval);
     
 
 BSP.ds_p.x1 = rw_x = start;
 BSP.ds_p.x2 = stop;
 BSP.ds_p.curline = curline;
 rw_stopx = stop+1;
 
 // calculate scale at both ends and step
 BSP.ds_p.scale1 = rw_scale = 
 R_ScaleFromGlobalAngle (viewangle + xtoviewangle[start]);
 
 if (stop > start )
 {
 BSP.ds_p.scale2 = R_ScaleFromGlobalAngle (viewangle + xtoviewangle[stop]);
 BSP.ds_p.scalestep = rw_scalestep = 
     (BSP.ds_p.scale2 - rw_scale) / (stop-start);
 }
 else
 {
 // UNUSED: try to fix the stretched line bug
#if 0
 if (rw_distance < FRACUNIT/2)
 {
     fixed_t     trx,try;
     fixed_t     gxt,gyt;

     trx = curline.v1.x - viewx;
     try = curline.v1.y - viewy;
         
     gxt = FixedMul(trx,viewcos); 
     gyt = -FixedMul(try,viewsin); 
     BSP.ds_p.scale1 = FixedDiv(projection, gxt-gyt)<<detailshift;
 }
#endif
 BSP.ds_p.scale2 = BSP.ds_p.scale1;
 }
 
 // calculate texture boundaries
 //  and decide if floor / ceiling marks are needed
 worldtop = frontsector.ceilingheight - viewz;
 worldbottom = frontsector.floorheight - viewz;
 
 midtexture = toptexture = bottomtexture = maskedtexture = 0;
 BSP.ds_p.maskedtexturecol = NULL;
 
 if (!backsector)
 {
 // single sided line
 midtexture = texturetranslation[sidedef.midtexture];
 // a single sided line is terminal, so it must mark ends
 markfloor = markceiling = true;
 if (linedef.flags & ML_DONTPEGBOTTOM)
 {
     vtop = frontsector.floorheight +
     textureheight[sidedef.midtexture];
     // bottom of texture at bottom
     rw_midtexturemid = vtop - viewz;    
 }
 else
 {
     // top of texture at top
     rw_midtexturemid = worldtop;
 }
 rw_midtexturemid += sidedef.rowoffset;

 BSP.ds_p.silhouette = SIL_BOTH;
 BSP.ds_p.sprtopclip = screenheightarray;
 BSP.ds_p.sprbottomclip = negonearray;
 BSP.ds_p.bsilheight = MAXINT;
 BSP.ds_p.tsilheight = MININT;
 }
 else
 {
 // two sided line
 BSP.ds_p.sprtopclip = BSP.ds_p.sprbottomclip = NULL;
 BSP.ds_p.silhouette = 0;
 
 if (frontsector.floorheight > backsector.floorheight)
 {
     BSP.ds_p.silhouette = SIL_BOTTOM;
     BSP.ds_p.bsilheight = frontsector.floorheight;
 }
 else if (backsector.floorheight > viewz)
 {
     BSP.ds_p.silhouette = SIL_BOTTOM;
     BSP.ds_p.bsilheight = MAXINT;
     // BSP.ds_p.sprbottomclip = negonearray;
 }
 
 if (frontsector.ceilingheight < backsector.ceilingheight)
 {
     BSP.ds_p.silhouette |= SIL_TOP;
     BSP.ds_p.tsilheight = frontsector.ceilingheight;
 }
 else if (backsector.ceilingheight < viewz)
 {
     BSP.ds_p.silhouette |= SIL_TOP;
     BSP.ds_p.tsilheight = MININT;
     // BSP.ds_p.sprtopclip = screenheightarray;
 }
     
 if (backsector.ceilingheight <= frontsector.floorheight)
 {
     BSP.ds_p.sprbottomclip = negonearray;
     BSP.ds_p.bsilheight = MAXINT;
     BSP.ds_p.silhouette |= SIL_BOTTOM;
 }
 
 if (backsector.floorheight >= frontsector.ceilingheight)
 {
     BSP.ds_p.sprtopclip = screenheightarray;
     BSP.ds_p.tsilheight = MININT;
     BSP.ds_p.silhouette |= SIL_TOP;
 }
 
 worldhigh = backsector.ceilingheight - viewz;
 worldlow = backsector.floorheight - viewz;
     
 // hack to allow height changes in outdoor areas
 if (frontsector.ceilingpic == skyflatnum 
     && backsector.ceilingpic == skyflatnum)
 {
     worldtop = worldhigh;
 }
 
         
 if (worldlow != worldbottom 
     || backsector.floorpic != frontsector.floorpic
     || backsector.lightlevel != frontsector.lightlevel)
 {
     markfloor = true;
 }
 else
 {
     // same plane on both sides
     markfloor = false;
 }
 
         
 if (worldhigh != worldtop 
     || backsector.ceilingpic != frontsector.ceilingpic
     || backsector.lightlevel != frontsector.lightlevel)
 {
     markceiling = true;
 }
 else
 {
     // same plane on both sides
     markceiling = false;
 }
 
 if (backsector.ceilingheight <= frontsector.floorheight
     || backsector.floorheight >= frontsector.ceilingheight)
 {
     // closed door
     markceiling = markfloor = true;
 }
 

 if (worldhigh < worldtop)
 {
     // top texture
     toptexture = texturetranslation[sidedef.toptexture];
     if (linedef.flags & ML_DONTPEGTOP)
     {
     // top of texture at top
     rw_toptexturemid = worldtop;
     }
     else
     {
     vtop =
         backsector.ceilingheight
         + textureheight[sidedef.toptexture];
     
     // bottom of texture
     rw_toptexturemid = vtop - viewz;    
     }
 }
 if (worldlow > worldbottom)
 {
     // bottom texture
     bottomtexture = texturetranslation[sidedef.bottomtexture];

     if (linedef.flags & ML_DONTPEGBOTTOM )
     {
     // bottom of texture at bottom
     // top of texture at top
     rw_bottomtexturemid = worldtop;
     }
     else    // top of texture at top
     rw_bottomtexturemid = worldlow;
 }
 rw_toptexturemid += sidedef.rowoffset;
 rw_bottomtexturemid += sidedef.rowoffset;
 
 // allocate space for masked texture tables
 if (sidedef.midtexture)
 {
     // masked midtexture
     maskedtexture = true;
     BSP.ds_p.maskedtexturecol = maskedtexturecol = lastopening - rw_x;
     lastopening += rw_stopx - rw_x;
 }
 }
 
 // calculate rw_offset (only needed for textured lines)
 segtextured = midtexture | toptexture | bottomtexture | maskedtexture;

 if (segtextured)
 {
 offsetangle = rw_normalangle-rw_angle1;
 
 if (offsetangle > ANG180)
     offsetangle = -offsetangle;

 if (offsetangle > ANG90)
     offsetangle = ANG90;

 sineval = finesine[offsetangle >>ANGLETOFINESHIFT];
 rw_offset = FixedMul (hyp, sineval);

 if (rw_normalangle-rw_angle1 < ANG180)
     rw_offset = -rw_offset;

 rw_offset += sidedef.textureoffset + curline.offset;
 rw_centerangle = ANG90 + viewangle - rw_normalangle;
 
 // calculate light table
 //  use different light tables
 //  for horizontal / vertical / diagonal
 // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
 if (!fixedcolormap)
 {
     lightnum = (frontsector.lightlevel >> LIGHTSEGSHIFT)+extralight;

     if (curline.v1.y == curline.v2.y)
     lightnum--;
     else if (curline.v1.x == curline.v2.x)
     lightnum++;

     if (lightnum < 0)       
     walllights = scalelight[0];
     else if (lightnum >= LIGHTLEVELS)
     walllights = scalelight[LIGHTLEVELS-1];
     else
     walllights = scalelight[lightnum];
 }
 }
 
 // if a floor / ceiling plane is on the wrong side
 //  of the view plane, it is definitely invisible
 //  and doesn't need to be marked.
 

 if (frontsector.floorheight >= viewz)
 {
 // above view plane
 markfloor = false;
 }
 
 if (frontsector.ceilingheight <= viewz 
 && frontsector.ceilingpic != skyflatnum)
 {
 // below view plane
 markceiling = false;
 }

 
 // calculate incremental stepping values for texture edges
 worldtop >>= 4;
 worldbottom >>= 4;
 
 topstep = -FixedMul (rw_scalestep, worldtop);
 topfrac = (centeryfrac>>4) - FixedMul (worldtop, rw_scale);

 bottomstep = -FixedMul (rw_scalestep,worldbottom);
 bottomfrac = (centeryfrac>>4) - FixedMul (worldbottom, rw_scale);
 
 if (backsector)
 {   
 worldhigh >>= 4;
 worldlow >>= 4;

 if (worldhigh < worldtop)
 {
     pixhigh = (centeryfrac>>4) - FixedMul (worldhigh, rw_scale);
     pixhighstep = -FixedMul (rw_scalestep,worldhigh);
 }
 
 if (worldlow > worldbottom)
 {
     pixlow = (centeryfrac>>4) - FixedMul (worldlow, rw_scale);
     pixlowstep = -FixedMul (rw_scalestep,worldlow);
 }
 }
 
 // render it
 if (markceiling)
 ceilingplane = R_CheckPlane (ceilingplane, rw_x, rw_stopx-1);
 
 if (markfloor)
 floorplane = R_CheckPlane (floorplane, rw_x, rw_stopx-1);

 R_RenderSegLoop ();

 
 // save sprite clipping info
 if ( ((BSP.ds_p.silhouette & SIL_TOP) || maskedtexture)
  && !BSP.ds_p.sprtopclip)
 {
 memcpy (lastopening, ceilingclip+start, 2*(rw_stopx-start));
 BSP.ds_p.sprtopclip = lastopening - start;
 lastopening += rw_stopx - start;
 }
 
 if ( ((BSP.ds_p.silhouette & SIL_BOTTOM) || maskedtexture)
  && !BSP.ds_p.sprbottomclip)
 {
 memcpy (lastopening, floorclip+start, 2*(rw_stopx-start));
 BSP.ds_p.sprbottomclip = lastopening - start;
 lastopening += rw_stopx - start;    
 }

 if (maskedtexture && !(BSP.ds_p.silhouette&SIL_TOP))
 {
 BSP.ds_p.silhouette |= SIL_TOP;
 BSP.ds_p.tsilheight = MININT;
 }
 if (maskedtexture && !(BSP.ds_p.silhouette&SIL_BOTTOM))
 {
 BSP.ds_p.silhouette |= SIL_BOTTOM;
 BSP.ds_p.bsilheight = MAXINT;
 }
 BSP.ds_p++;
}




}