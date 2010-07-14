package rr;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Renderer.java,v 1.3 2010/07/14 16:12:20 velktron Exp $
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
//
//-----------------------------------------------------------------------------

/*

#include "doomdef.h"
#include "d_net.h"

#include "m_bbox.h"

#include "r_local.h"
#include "r_sky.h"
 */
import i.system;
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
lighttable_t		fixedcolormap;
//extern lighttable_t**	walllights;

public int			centerx;
public int			centery;

fixed_t			centerxfrac;
fixed_t			centeryfrac;
fixed_t			projection;

// just for profiling purposes
public  int			framecount;	

public  int			sscount;
public  int			linecount;
public  int			loopcount;

public  int			viewx;
public  int			viewy;
public  int			viewz;

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
fixed_t     pspritescale;
fixed_t     pspriteiscale;

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

//
// R_PointOnSide
// Traverse BSP (sub) tree,
//  check point against partition plane.
// Returns side 0 (front) or 1 (back).
//
public int
PointOnSide
( fixed_t	x,
  fixed_t	y,
  node_t	node )
{
    // MAES: These are used mainly as ints, no need to use fixed_t internally.
    // fixed_t will only be used as a "pass type", but calculations will be done with ints, preferably.
    int	dx; 
    int	dy;
    int	left;
    int	right;
	
    if (node.dx.isEZ())
    {
	if (x.compareTo(node.x)<0)
	    return node.dy.oneGZ();
	
	return node.dy.oneLZ();
    }
    
    if (node.dy.isEZ())
    {
	if (y.val <= node.y.val)
	    return node.dx.oneLZ();
	
	return node.dx.oneGZ();
    }
	
    dx = (x.val - node.x.val);
    dy = (y.val - node.y.val);
	
    // Try to quickly decide by looking at sign bits.
    if ( ((node.dy.val ^ node.dx.val ^ dx ^ dy)&0x80000000)!=0 )
    {
	if  ( ((node.dy.val ^ dx) & 0x80000000)!=0 )
	{
	    // (left is negative)
	    return 1;
	}
	return 0;
    }

    left = FixedMul ( node.dy.val>>FRACBITS , dx );
    right = FixedMul ( dy , node.dx.val>>FRACBITS );
	
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
 */


public int
PointToAngle
( fixed_t	xx,
  fixed_t	yy )
{	
    int x=sub(xx, viewx);
    int y=sub(yy, viewy);
    
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


int
PointToAngle2
( fixed_t	x1,
  fixed_t	y1,
  fixed_t	x2,
  fixed_t	y2 )
{	
    // Careful with assignments...
    viewx.copy(x1);
    viewy.copy(y1);
    
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
	
    dx = Math.abs(x.val - viewx.val);
    dy = Math.abs(y.val - viewy.val);
	
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
boolean		setsizeneeded;
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
	ds.viewheight = (setblocks*168/10)&~7;
    }
    
    detailshift = setdetail;
    ds.viewwidth = ds.scaledviewwidth>>detailshift;
	
    centery = ds.viewheight/2;
    centerx = ds.viewwidth/2;
    centerxfrac.set(centerx<<FRACBITS);
    centeryfrac.set(centery<<FRACBITS);
    projection.copy(centerxfrac);

    /*
    if (!detailshift)
    {
	colfunc = basecolfunc = R_DrawColumn;
	fuzzcolfunc = R_DrawFuzzColumn;
	transcolfunc = R_DrawTranslatedColumn;
	spanfunc = R_DrawSpan;
    }
    else
    {
	colfunc = basecolfunc = R_DrawColumnLow;
	fuzzcolfunc = R_DrawFuzzColumn;
	transcolfunc = R_DrawTranslatedColumn;
	spanfunc = R_DrawSpanLow;
    }*/

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


//
// R_PointInSubsector
//
public subsector_t
PointInSubsector
( fixed_t	x,
  fixed_t	y )
{
    node_t	node;
    int		side;
    int		nodenum;

    // single subsector is a special case
    if (!numnodes)				
	return subsectors;
		
    nodenum = numnodes-1;

    while (! (nodenum & NF_SUBSECTOR) )
    {
	node = &nodes[nodenum];
	side = R_PointOnSide (x, y, node);
	nodenum = node->children[side];
    }
	
    return &subsectors[nodenum & ~NF_SUBSECTOR];
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

    viewz = player->viewz;
    
    viewsin = finesine[viewangle>>ANGLETOFINESHIFT];
    viewcos = finecosine[viewangle>>ANGLETOFINESHIFT];
	
    sscount = 0;
	
    if (player->fixedcolormap)
    {
	fixedcolormap =
	    colormaps
	    + player->fixedcolormap*256*sizeof(lighttable_t);
	
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
    R_RenderBSPNode (numnodes-1);
    
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

public void R_PrecacheLevel ()
{
    String       flatpresent;
    String       texturepresent;
    String       spritepresent;

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
    flatpresent = alloca(numflats);
    memset (flatpresent,0,numflats);    

    for (i=0 ; i<numsectors ; i++)
    {
    flatpresent[sectors[i].floorpic] = 1;
    flatpresent[sectors[i].ceilingpic] = 1;
    }
    
    flatmemory = 0;

    for (i=0 ; i<numflats ; i++)
    {
    if (flatpresent[i])
    {
        lump = firstflat + i;
        flatmemory += lumpinfo[lump].size;
        W_CacheLumpNum(lump, PU_CACHE);
    }
    }
    
    // Precache textures.
    texturepresent = alloca(numtextures);
    memset (texturepresent,0, numtextures);
    
    for (i=0 ; i<numsides ; i++)
    {
    texturepresent[sides[i].toptexture] = 1;
    texturepresent[sides[i].midtexture] = 1;
    texturepresent[sides[i].bottomtexture] = 1;
    }

    // Sky texture is always present.
    // Note that F_SKY1 is the name used to
    //  indicate a sky floor/ceiling as a flat,
    //  while the sky texture is stored like
    //  a wall texture, with an episode dependend
    //  name.
    texturepresent[skytexture] = 1;
    
    texturememory = 0;
    for (i=0 ; i<numtextures ; i++)
    {
    if (!texturepresent[i])
        continue;

    texture = textures[i];
    
    for (j=0 ; j<texture->patchcount ; j++)
    {
        lump = texture->patches[j].patch;
        texturememory += lumpinfo[lump].size;
        W_CacheLumpNum(lump , PU_CACHE);
    }
    }
    
    // Precache sprites.
    spritepresent = alloca(numsprites);
    memset (spritepresent,0, numsprites);
    
    for (th = thinkercap.next ; th != &thinkercap ; th=th->next)
    {
    if (th->function.acp1 == (actionf_p1)P_MobjThinker)
        spritepresent[((mobj_t *)th)->sprite] = 1;
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
        lump = firstspritelump + sf->lump[k];
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
public void R_InitTextures ()
{
 maptexture_t   mtexture;
 texture_t      texture;
 mappatch_t     mpatch;
 texpatch_t     patch;

 int         i;
 int         j;

 // MAES: there were pointers.
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
 names = W_CacheLumpName ("PNAMES", PU_STATIC);
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
           + sizeof(texpatch_t)*(SHORT(mtexture->patchcount)-1),
           PU_STATIC, 0);
 
 texture->width = SHORT(mtexture->width);
 texture->height = SHORT(mtexture->height);
 texture->patchcount = SHORT(mtexture->patchcount);

 memcpy (texture->name, mtexture->name, sizeof(texture->name));
 mpatch = &mtexture->patches[0];
 patch = &texture->patches[0];

 for (j=0 ; j<texture->patchcount ; j++, mpatch++, patch++)
 {
     patch->originx = SHORT(mpatch->originx);
     patch->originy = SHORT(mpatch->originy);
     patch->patch = patchlookup[SHORT(mpatch->patch)];
     if (patch->patch == -1)
     {
     I_Error ("R_InitTextures: Missing patch in texture %s",
          texture->name);
     }
 }       
 texturecolumnlump[i] = Z_Malloc (texture->width*2, PU_STATIC,0);
 texturecolumnofs[i] = Z_Malloc (texture->width*2, PU_STATIC,0);

 j = 1;
 while (j*2 <= texture->width)
     j<<=1;

 texturewidthmask[i] = j-1;
 textureheight[i] = texture->height<<FRACBITS;
     
 totalwidth += texture->width;
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
 spritewidth[i] = SHORT(patch->width)<<FRACBITS;
 spriteoffset[i] = SHORT(patch->leftoffset)<<FRACBITS;
 spritetopoffset[i] = SHORT(patch->topoffset)<<FRACBITS;
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
//R_FlatNumForName
//Retrieval, get a flat number for a flat name.
//
int R_FlatNumForName (char* name)
{
 int     i;
 char    namet[9];

 i = W_CheckNumForName (name);

 if (i == -1)
 {
 namet[8] = 0;
 memcpy (namet, name,8);
 I_Error ("R_FlatNumForName: %s not found",namet);
 }
 return i - firstflat;
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
 if (!strncasecmp (textures[i]->name, name, 8) )
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
 byte*       patchcount; // patchcount[texture->width]
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
 patchcount = (byte *)alloca (texture->width);
 memset (patchcount, 0, texture->width);
 patch = texture->patches;
     
 for (i=0 , patch = texture->patches;
  i<texture->patchcount;
  i++, patch++)
 {
 realpatch = W_CacheLumpNum (patch->patch, PU_CACHE);
 x1 = patch->originx;
 x2 = x1 + SHORT(realpatch->width);
 
 if (x1 < 0)
     x = 0;
 else
     x = x1;

 if (x2 > texture->width)
     x2 = texture->width;
 for ( ; x<x2 ; x++)
 {
     patchcount[x]++;
     collump[x] = patch->patch;
     colofs[x] = LONG(realpatch->columnofs[x-x1])+3;
 }
 }
 
 for (x=0 ; x<texture->width ; x++)
 {
 if (!patchcount[x])
 {
     printf ("R_GenerateLookup: column without a patch (%s)\n",
         texture->name);
     return;
 }
 // I_Error ("R_GenerateLookup: column without a patch");
 
 if (patchcount[x] > 1)
 {
     // Use the cached block.
     collump[x] = -1;    
     colofs[x] = texturecompositesize[texnum];
     
     if (texturecompositesize[texnum] > 0x10000-texture->height)
     {
     I_Error ("R_GenerateLookup: texture %i is >64k",
          texnum);
     }
     
     texturecompositesize[texnum] += texture->height;
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
    
    while (patch->topdelta != 0xff)
    {
    source = (byte *)patch + 3;
    count = patch->length;
    position = originy + patch->topdelta;

    if (position < 0)
    {
        count += position;
        position = 0;
    }

    if (position + count > cacheheight)
        count = cacheheight - position;

    if (count > 0)
        memcpy (cache + position, source, count);
        
    patch = (column_t *)(  (byte *)patch + patch->length + 4); 
    }
}



//
// R_GenerateComposite
// Using the texture definition,
//  the composite texture is created from the patches,
//  and each column is cached.
//
void R_GenerateComposite (int texnum)
{
    byte*       block;
    texture_t*      texture;
    texpatch_t*     patch;  
    patch_t*        realpatch;
    int         x;
    int         x1;
    int         x2;
    int         i;
    column_t*       patchcol;
    short*      collump;
    unsigned short* colofs;
    
    texture = textures[texnum];

    block = Z_Malloc (texturecompositesize[texnum],
              PU_STATIC, 
              &texturecomposite[texnum]);   

    collump = texturecolumnlump[texnum];
    colofs = texturecolumnofs[texnum];
    
    // Composite the columns together.
    patch = texture->patches;
        
    for (i=0 , patch = texture->patches;
     i<texture->patchcount;
     i++, patch++)
    {
    realpatch = W_CacheLumpNum (patch->patch, PU_CACHE);
    x1 = patch->originx;
    x2 = x1 + SHORT(realpatch->width);

    if (x1<0)
        x = 0;
    else
        x = x1;
    
    if (x2 > texture->width)
        x2 = texture->width;

    for ( ; x<x2 ; x++)
    {
        // Column does not have multiple patches?
        if (collump[x] >= 0)
        continue;
        
        patchcol = (column_t *)((byte *)realpatch
                    + LONG(realpatch->columnofs[x-x1]));
        R_DrawColumnInCache (patchcol,
                 block + colofs[x],
                 patch->originy,
                 texture->height);
    }
                        
    }

    // Now that the texture has been built in column cache,
    //  it is purgable from zone memory.
    Z_ChangeTag (block, PU_CACHE);
}

planefunction_t     floorfunc;
planefunction_t     ceilingfunc;

//
// opening
//

// Here comes the obnoxious "visplane".
#define MAXVISPLANES    128
visplane_t      visplanes[MAXVISPLANES];
visplane_t*     lastvisplane;
visplane_t*     floorplane;
visplane_t*     ceilingplane;

// ?
#define MAXOPENINGS SCREENWIDTH*64
short           openings[MAXOPENINGS];
short*          lastopening;


//
// Clip values are the solid pixel bounding the range.
//  floorclip starts out SCREENHEIGHT
//  ceilingclip starts out -1
//
short           floorclip[SCREENWIDTH];
short           ceilingclip[SCREENWIDTH];

//
// spanstart holds the start of a plane span
// initialized to 0 at start
//
int         spanstart[SCREENHEIGHT];
int         spanstop[SCREENHEIGHT];

//
// texture mapping
//
lighttable_t**      planezlight;
fixed_t         planeheight;

fixed_t         yslope[SCREENHEIGHT];
fixed_t         distscale[SCREENWIDTH];
fixed_t         basexscale;
fixed_t         baseyscale;

fixed_t         cachedheight[SCREENHEIGHT];
fixed_t         cacheddistance[SCREENHEIGHT];
fixed_t         cachedxstep[SCREENHEIGHT];
fixed_t         cachedystep[SCREENHEIGHT];



//
// R_InitPlanes
// Only at game startup.
//
public void InitPlanes ()
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
( int       y,
  int       x1,
  int       x2 )
{
    angle_t angle;
    fixed_t distance;
    fixed_t length;
    unsigned    index;
    
#ifdef RANGECHECK
    if (x2 < x1
    || x1<0
    || x2>=ds.viewwidth
    || (unsigned)y>ds.viewheight)
    {
    I_Error ("R_MapPlane: %i, %i at %i",x1,x2,y);
    }
#endif

    if (planeheight != cachedheight[y])
    {
    cachedheight[y] = planeheight;
    distance = cacheddistance[y] = FixedMul (planeheight, yslope[y]);
    ds_xstep = cachedxstep[y] = FixedMul (distance,basexscale);
    ds_ystep = cachedystep[y] = FixedMul (distance,baseyscale);
    }
    else
    {
    distance = cacheddistance[y];
    ds_xstep = cachedxstep[y];
    ds_ystep = cachedystep[y];
    }
    
    length = FixedMul (distance,distscale[x1]);
    angle = (viewangle + xtoviewangle[x1])>>ANGLETOFINESHIFT;
    ds_xfrac = viewx + FixedMul(finecosine[angle], length);
    ds_yfrac = -viewy - FixedMul(finesine[angle], length);

    if (fixedcolormap)
    ds_colormap = fixedcolormap;
    else
    {
    index = distance >> LIGHTZSHIFT;
    
    if (index >= MAXLIGHTZ )
        index = MAXLIGHTZ-1;

    ds_colormap = planezlight[index];
    }
    
    ds_y = y;
    ds_x1 = x1;
    ds_x2 = x2;

    // high or low detail
    spanfunc ();    
}


//
// R_ClearPlanes
// At begining of frame.
//
public void ClearPlanes ()
{
    int     i;
    angle_t angle;
    
    // opening / clipping determination
    for (i=0 ; i<ds.viewwidth ; i++)
    {
    floorclip[i] = ds.viewheight;
    ceilingclip[i] = -1;
    }

    lastvisplane = visplanes;
    lastopening = openings;
    
    // texture calculation
    memset (cachedheight, 0, sizeof(cachedheight));

    // left to right mapping
    angle = (viewangle-ANG90)>>ANGLETOFINESHIFT;
    
    // scale will be unit scale at SCREENWIDTH/2 distance
    basexscale = FixedDiv (finecosine[angle],centerxfrac);
    baseyscale = -FixedDiv (finesine[angle],centerxfrac);
}




//
// R_FindPlane
//
public visplane_t
FindPlane
( fixed_t   height,
  int       picnum,
  int       lightlevel )
{
    visplane_t* check;
    
    if (picnum == skyflatnum)
    {
    height = 0;         // all skys map together
    lightlevel = 0;
    }
    
    for (check=visplanes; check<lastvisplane; check++)
    {
    if (height == check->height
        && picnum == check->picnum
        && lightlevel == check->lightlevel)
    {
        break;
    }
    }
    
            
    if (check < lastvisplane)
    return check;
        
    if (lastvisplane - visplanes == MAXVISPLANES)
    I_Error ("R_FindPlane: no more visplanes");
        
    lastvisplane++;

    check->height = height;
    check->picnum = picnum;
    check->lightlevel = lightlevel;
    check->minx = SCREENWIDTH;
    check->maxx = -1;
    
    memset (check->top,0xff,sizeof(check->top));
        
    return check;
}


//
// R_CheckPlane
//
visplane_t*
R_CheckPlane
( visplane_t*   pl,
  int       start,
  int       stop )
{
    int     intrl;
    int     intrh;
    int     unionl;
    int     unionh;
    int     x;
    
    if (start < pl->minx)
    {
    intrl = pl->minx;
    unionl = start;
    }
    else
    {
    unionl = pl->minx;
    intrl = start;
    }
    
    if (stop > pl->maxx)
    {
    intrh = pl->maxx;
    unionh = stop;
    }
    else
    {
    unionh = pl->maxx;
    intrh = stop;
    }

    for (x=intrl ; x<= intrh ; x++)
    if (pl->top[x] != 0xff)
        break;

    if (x > intrh)
    {
    pl->minx = unionl;
    pl->maxx = unionh;

    // use the same one
    return pl;      
    }
    
    // make a new visplane
    lastvisplane->height = pl->height;
    lastvisplane->picnum = pl->picnum;
    lastvisplane->lightlevel = pl->lightlevel;
    
    pl = lastvisplane++;
    pl->minx = start;
    pl->maxx = stop;

    memset (pl->top,0xff,sizeof(pl->top));
        
    return pl;
}


//
// R_MakeSpans
//
void
R_MakeSpans
( int       x,
  int       t1,
  int       b1,
  int       t2,
  int       b2 )
{
    while (t1 < t2 && t1<=b1)
    {
    R_MapPlane (t1,spanstart[t1],x-1);
    t1++;
    }
    while (b1 > b2 && b1>=t1)
    {
    R_MapPlane (b1,spanstart[b1],x-1);
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



//
// R_DrawPlanes
// At the end of each frame.
//
void R_DrawPlanes (void)
{
    visplane_t*     pl;
    int         light;
    int         x;
    int         stop;
    int         angle;
                
#ifdef RANGECHECK
    if (ds_p - drawsegs > MAXDRAWSEGS)
    I_Error ("R_DrawPlanes: drawsegs overflow (%i)",
         ds_p - drawsegs);
    
    if (lastvisplane - visplanes > MAXVISPLANES)
    I_Error ("R_DrawPlanes: visplane overflow (%i)",
         lastvisplane - visplanes);
    
    if (lastopening - openings > MAXOPENINGS)
    I_Error ("R_DrawPlanes: opening overflow (%i)",
         lastopening - openings);
#endif

    for (pl = visplanes ; pl < lastvisplane ; pl++)
    {
    if (pl->minx > pl->maxx)
        continue;

    
    // sky flat
    if (pl->picnum == skyflatnum)
    {
        dc_iscale = pspriteiscale>>detailshift;
        
        // Sky is allways drawn full bright,
        //  i.e. colormaps[0] is used.
        // Because of this hack, sky is not affected
        //  by INVUL inverse mapping.
        dc_colormap = colormaps;
        dc_texturemid = skytexturemid;
        for (x=pl->minx ; x <= pl->maxx ; x++)
        {
        dc_yl = pl->top[x];
        dc_yh = pl->bottom[x];

        if (dc_yl <= dc_yh)
        {
            angle = (viewangle + xtoviewangle[x])>>ANGLETOSKYSHIFT;
            dc_x = x;
            dc_source = R_GetColumn(skytexture, angle);
            colfunc ();
        }
        }
        continue;
    }
    
    // regular flat
    ds_source = W_CacheLumpNum(firstflat +
                   flattranslation[pl->picnum],
                   PU_STATIC);
    
    planeheight = abs(pl->height-viewz);
    light = (pl->lightlevel >> LIGHTSEGSHIFT)+extralight;

    if (light >= LIGHTLEVELS)
        light = LIGHTLEVELS-1;

    if (light < 0)
        light = 0;

    planezlight = zlight[light];

    pl->top[pl->maxx+1] = 0xff;
    pl->top[pl->minx-1] = 0xff;
        
    stop = pl->maxx + 1;

    for (x=pl->minx ; x<= stop ; x++)
    {
        R_MakeSpans(x,pl->top[x-1],
            pl->bottom[x-1],
            pl->top[x],
            pl->bottom[x]);
    }
    
    Z_ChangeTag (ds_source, PU_CACHE);
    }
}





//
// R_InstallSpriteLump
// Local function for R_InitSprites.
//
private void
R_InstallSpriteLump
( int       lump,
  int   frame,
  int   rotation,
  boolean   flipped )
{
    int     r;
    
    if (frame >= 29 || rotation > 8)
        system.Error("R_InstallSpriteLump: Bad frame characters in lump %i", lump);
    
    if ((int)frame > maxframe)
    maxframe = frame;
        
    if (rotation == 0)
    {
    // the lump should be used for all rotations
    if (sprtemp[frame].rotate == false)
        system.Error("R_InitSprites: Sprite %s frame %c has "+
             "multip rot=0 lump", spritename, 'A'+frame);

    if (sprtemp[frame].rotate == true)
        system.Error("R_InitSprites: Sprite %s frame %c has rotations "+
             "and a rot=0 lump", spritename, 'A'+frame);
            
    sprtemp[frame].rotate = false;
    for (r=0 ; r<8 ; r++)
    {
        sprtemp[frame].lump[r] = lump - firstspritelump;
        sprtemp[frame].flip[r] = (byte)flipped;
    }
    return;
    }
    
    // the lump is only used for one rotation
    if (sprtemp[frame].rotate == false)
        system.Error("R_InitSprites: Sprite %s frame %c has rotations "+
         "and a rot=0 lump", spritename, 'A'+frame);
        
    sprtemp[frame].rotate = true;

    // make 0 based
    rotation--;     
    if (sprtemp[frame].lump[rotation] != -1)
        system.Error("R_InitSprites: Sprite %s : %c : %c "+
         "has two lumps mapped to it",
         spritename, 'A'+frame, '1'+rotation);
        
    sprtemp[frame].lump[rotation] = lump - firstspritelump;
    sprtemp[frame].flip[rotation] = (byte) (flipped?0:1);
}




//
// R_InitSpriteDefs
// Pass a null terminated list of sprite names
//  (4 chars exactly) to be used.
// Builds the sprite rotation matrixes to account
//  for horizontally flipped sprites.
// Will report an error if the lumps are inconsistant. 
// Only called at startup.
//
// Sprite lump names are 4 characters for the actor,
//  a letter for the frame, and a number for the rotation.
// A sprite that is flippable will have an additional
//  letter/number appended.
// The rotation character can be 0 to signify no rotations.
//
public static void R_InitSpriteDefs (String[] namelist) 
{ 
    String[]    check;
    int     i;
    int     l;
    int     intname;
    int     frame;
    int     rotation;
    int     start;
    int     end;
    int     patched;
        
    // count the number of sprite names
    check = namelist;
    while (*check != NULL)
    check++;

    numsprites = check-namelist;
    
    if (!numsprites)
    return;
        
    //sprites = Z_Malloc(numsprites *sizeof(*sprites), PU_STATIC, NULL);
    sprites = new sprites[numsprites];
    
    start = firstspritelump-1;
    end = lastspritelump+1;
    
    // scan all the lump names for each of the names,
    //  noting the highest frame letter.
    // Just compare 4 characters as ints
    for (i=0 ; i<numsprites ; i++)
    {
    spritename = namelist[i];
    memset (sprtemp,-1, sizeof(sprtemp));
        
    maxframe = -1;
    intname = *(int *)namelist[i];
    
    // scan the lumps,
    //  filling in the frames for whatever is found
    for (l=start+1 ; l<end ; l++)
    {
        if (*(int *)lumpinfo[l].name == intname)
        {
        frame = lumpinfo[l].name[4] - 'A';
        rotation = lumpinfo[l].name[5] - '0';

        if (modifiedgame)
            patched = W_GetNumForName (lumpinfo[l].name);
        else
            patched = l;

        R_InstallSpriteLump (patched, frame, rotation, false);

        if (lumpinfo[l].name[6])
        {
            frame = lumpinfo[l].name[6] - 'A';
            rotation = lumpinfo[l].name[7] - '0';
            R_InstallSpriteLump (l, frame, rotation, true);
        }
        }
    }
    
    // check the frames that were found for completeness
    if (maxframe == -1)
    {
        sprites[i].numframes = 0;
        continue;
    }
        
    maxframe++;
    
    for (frame = 0 ; frame < maxframe ; frame++)
    {
        switch ((int)sprtemp[frame].rotate)
        {
          case -1:
        // no rotations were found for that frame at all
        I_Error ("R_InitSprites: No patches found "
             "for %s frame %c", namelist[i], frame+'A');
        break;
        
          case 0:
        // only the first rotation is needed
        break;
            
          case 1:
        // must have all 8 frames
        for (rotation=0 ; rotation<8 ; rotation++)
            if (sprtemp[frame].lump[rotation] == -1)
            I_Error ("R_InitSprites: Sprite %s frame %c "
                 "is missing rotations",
                 namelist[i], frame+'A');
        break;
        }
    }
    
    // allocate space for the frames present and copy sprtemp to it
    sprites[i].numframes = maxframe;
    sprites[i].spriteframes = 
        Z_Malloc (maxframe * sizeof(spriteframe_t), PU_STATIC, NULL);
    memcpy (sprites[i].spriteframes, sprtemp, maxframe*sizeof(spriteframe_t));
    }

}




//
// GAME FUNCTIONS
//
public vissprite_t[]    vissprites=new vissprite_t[MAXVISSPRITES];
// MAES: was used as pointer.
private int vissprite_p;




//
// R_InitSprites
// Called at program start.
//
public void R_InitSprites (String[] namelist)
{
    int     i;
    
    for (i=0 ; i<SCREENWIDTH ; i++)
    {
    negonearray[i] = -1;
    }
    
    R_InitSpriteDefs (namelist);
}



//
// R_ClearSprites
// Called at frame start.
//
public void R_ClearSprites ()
{
    // Maes: set to point back to zero.
    vissprite_p = 0;
}


//
// R_NewVisSprite
//
private vissprite_t overflowsprite;

public vissprite_t R_NewVisSprite ()
{
    // MAES: we probably need a struct comparison here.
    if (vissprites[vissprite_p] == vissprites[MAXVISSPRITES])
    return overflowsprite;
    
    vissprite_p++;
    return vissprites[vissprite_p-1];
}



//
// R_DrawMaskedColumn
// Used for sprites and masked mid textures.
// Masked means: partly transparent, i.e. stored
//  in posts/runs of opaque pixels.
//

// MAES: were pointers to arrays.
private short[]     mfloorclip;
private  short[]        mceilingclip;

private fixed_t     spryscale;
private fixed_t     sprtopscreen;

public  void R_DrawMaskedColumn (column_t column)
{
    int     topscreen;
    int     bottomscreen;
    fixed_t basetexturemid;
    
    basetexturemid = dc_texturemid;
    
    
    // For each post in column...
    for (int i=0;i<column.posts;i++){
    // calculate unclipped screen coordinates
    //  for post
    topscreen = sprtopscreen + spryscale*column.postdeltas[i];
    bottomscreen = topscreen + spryscale*column.postlen[i];

    dc_yl = (topscreen+FRACUNIT-1)>>FRACBITS;
    dc_yh = (bottomscreen-1)>>FRACBITS;
        
    if (dc_yh >= mfloorclip[dc_x])
        dc_yh = mfloorclip[dc_x]-1;
    if (dc_yl <= mceilingclip[dc_x])
        dc_yl = mceilingclip[dc_x]+1;

    if (dc_yl <= dc_yh)
    {
        dc_source = (byte *)column + 3;
        dc_texturemid = basetexturemid - (column->topdelta<<FRACBITS);
        // dc_source = (byte *)column + 3 - column->topdelta;

        // Drawn by either R_DrawColumn
        //  or (SHADOW) R_DrawFuzzColumn.
        colfunc (); 
    }
    column = (column_t *)(  (byte *)column + column->length + 4);
    }
    
    dc_texturemid = basetexturemid;
}
}

/*

//
// R_DrawVisSprite
//  mfloorclip and mceilingclip should also be set.
//
public void
R_DrawVisSprite
( vissprite_t*      vis,
  int           x1,
  int           x2 )
{
    column_t*       column;
    int         texturecolumn;
    fixed_t     frac;
    patch_t*        patch;
    
    
    patch = W_CacheLumpNum (vis->patch+firstspritelump, PU_CACHE);

    dc_colormap = vis->colormap;
    
    if (!dc_colormap)
    {
    // NULL colormap = shadow draw
    colfunc = fuzzcolfunc;
    }
    else if (vis->mobjflags & MF_TRANSLATION)
    {
    colfunc = R_DrawTranslatedColumn;
    dc_translation = translationtables - 256 +
        ( (vis->mobjflags & MF_TRANSLATION) >> (MF_TRANSSHIFT-8) );
    }
    
    dc_iscale = abs(vis->xiscale)>>detailshift;
    dc_texturemid = vis->texturemid;
    frac = vis->startfrac;
    spryscale = vis->scale;
    sprtopscreen = centeryfrac - FixedMul(dc_texturemid,spryscale);
    
    for (dc_x=vis->x1 ; dc_x<=vis->x2 ; dc_x++, frac += vis->xiscale)
    {
    texturecolumn = frac>>FRACBITS;
#ifdef RANGECHECK
    if (texturecolumn < 0 || texturecolumn >= SHORT(patch->width))
        I_Error ("R_DrawSpriteRange: bad texturecolumn");
#endif
    column = (column_t *) ((byte *)patch +
                   LONG(patch->columnofs[texturecolumn]));
    R_DrawMaskedColumn (column);
    }

    colfunc = basecolfunc;
}



//
// R_ProjectSprite
// Generates a vissprite for a thing
//  if it might be visible.
//
void R_ProjectSprite (mobj_t* thing)
{
    fixed_t     tr_x;
    fixed_t     tr_y;
    
    fixed_t     gxt;
    fixed_t     gyt;
    
    fixed_t     tx;
    fixed_t     tz;

    fixed_t     xscale;
    
    int         x1;
    int         x2;

    spritedef_t*    sprdef;
    spriteframe_t*  sprframe;
    int         lump;
    
    unsigned        rot;
    boolean     flip;
    
    int         index;

    vissprite_t*    vis;
    
    angle_t     ang;
    fixed_t     iscale;
    
    // transform the origin point
    tr_x = thing->x - viewx;
    tr_y = thing->y - viewy;
    
    gxt = FixedMul(tr_x,viewcos); 
    gyt = -FixedMul(tr_y,viewsin);
    
    tz = gxt-gyt; 

    // thing is behind view plane?
    if (tz < MINZ)
    return;
    
    xscale = FixedDiv(projection, tz);
    
    gxt = -FixedMul(tr_x,viewsin); 
    gyt = FixedMul(tr_y,viewcos); 
    tx = -(gyt+gxt); 

    // too far off the side?
    if (abs(tx)>(tz<<2))
    return;
    
    // decide which patch to use for sprite relative to player
#ifdef RANGECHECK
    if ((unsigned)thing->sprite >= numsprites)
    I_Error ("R_ProjectSprite: invalid sprite number %i ",
         thing->sprite);
#endif
    sprdef = &sprites[thing->sprite];
#ifdef RANGECHECK
    if ( (thing->frame&FF_FRAMEMASK) >= sprdef->numframes )
    I_Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
         thing->sprite, thing->frame);
#endif
    sprframe = &sprdef->spriteframes[ thing->frame & FF_FRAMEMASK];

    if (sprframe->rotate)
    {
    // choose a different rotation based on player view
    ang = R_PointToAngle (thing->x, thing->y);
    rot = (ang-thing->angle+(unsigned)(ANG45/2)*9)>>29;
    lump = sprframe->lump[rot];
    flip = (boolean)sprframe->flip[rot];
    }
    else
    {
    // use single rotation for all views
    lump = sprframe->lump[0];
    flip = (boolean)sprframe->flip[0];
    }
    
    // calculate edges of the shape
    tx -= spriteoffset[lump];   
    x1 = (centerxfrac + FixedMul (tx,xscale) ) >>FRACBITS;

    // off the right side?
    if (x1 > ds.viewwidth)
    return;
    
    tx +=  spritewidth[lump];
    x2 = ((centerxfrac + FixedMul (tx,xscale) ) >>FRACBITS) - 1;

    // off the left side
    if (x2 < 0)
    return;
    
    // store information in a vissprite
    vis = R_NewVisSprite ();
    vis->mobjflags = thing->flags;
    vis->scale = xscale<<detailshift;
    vis->gx = thing->x;
    vis->gy = thing->y;
    vis->gz = thing->z;
    vis->gzt = thing->z + spritetopoffset[lump];
    vis->texturemid = vis->gzt - viewz;
    vis->x1 = x1 < 0 ? 0 : x1;
    vis->x2 = x2 >= ds.viewwidth ? ds.viewwidth-1 : x2;   
    iscale = FixedDiv (FRACUNIT, xscale);

    if (flip)
    {
    vis->startfrac = spritewidth[lump]-1;
    vis->xiscale = -iscale;
    }
    else
    {
    vis->startfrac = 0;
    vis->xiscale = iscale;
    }

    if (vis->x1 > x1)
    vis->startfrac += vis->xiscale*(vis->x1-x1);
    vis->patch = lump;
    
    // get light level
    if (thing->flags & MF_SHADOW)
    {
    // shadow draw
    vis->colormap = NULL;
    }
    else if (fixedcolormap)
    {
    // fixed map
    vis->colormap = fixedcolormap;
    }
    else if (thing->frame & FF_FULLBRIGHT)
    {
    // full bright
    vis->colormap = colormaps;
    }
    
    else
    {
    // diminished light
    index = xscale>>(LIGHTSCALESHIFT-detailshift);

    if (index >= MAXLIGHTSCALE) 
        index = MAXLIGHTSCALE-1;

    vis->colormap = spritelights[index];
    }   
}




//
// R_AddSprites
// During BSP traversal, this adds sprites by sector.
//
void R_AddSprites (sector_t* sec)
{
    mobj_t*     thing;
    int         lightnum;

    // BSP is traversed by subsector.
    // A sector might have been split into several
    //  subsectors during BSP building.
    // Thus we check whether its already added.
    if (sec->validcount == validcount)
    return;     

    // Well, now it will be done.
    sec->validcount = validcount;
    
    lightnum = (sec->lightlevel >> LIGHTSEGSHIFT)+extralight;

    if (lightnum < 0)       
    spritelights = scalelight[0];
    else if (lightnum >= LIGHTLEVELS)
    spritelights = scalelight[LIGHTLEVELS-1];
    else
    spritelights = scalelight[lightnum];

    // Handle all things in sector.
    for (thing = sec->thinglist ; thing ; thing = thing->snext)
    R_ProjectSprite (thing);
}


//
// R_DrawPSprite
//
void R_DrawPSprite (pspdef_t* psp)
{
    fixed_t     tx;
    int         x1;
    int         x2;
    spritedef_t*    sprdef;
    spriteframe_t*  sprframe;
    int         lump;
    boolean     flip;
    vissprite_t*    vis;
    vissprite_t     avis;
    
    // decide which patch to use
#ifdef RANGECHECK
    if ( (unsigned)psp->state->sprite >= numsprites)
    I_Error ("R_ProjectSprite: invalid sprite number %i ",
         psp->state->sprite);
#endif
    sprdef = &sprites[psp->state->sprite];
#ifdef RANGECHECK
    if ( (psp->state->frame & FF_FRAMEMASK)  >= sprdef->numframes)
    I_Error ("R_ProjectSprite: invalid sprite frame %i : %i ",
         psp->state->sprite, psp->state->frame);
#endif
    sprframe = &sprdef->spriteframes[ psp->state->frame & FF_FRAMEMASK ];

    lump = sprframe->lump[0];
    flip = (boolean)sprframe->flip[0];
    
    // calculate edges of the shape
    tx = psp->sx-160*FRACUNIT;
    
    tx -= spriteoffset[lump];   
    x1 = (centerxfrac + FixedMul (tx,pspritescale) ) >>FRACBITS;

    // off the right side
    if (x1 > ds.viewwidth)
    return;     

    tx +=  spritewidth[lump];
    x2 = ((centerxfrac + FixedMul (tx, pspritescale) ) >>FRACBITS) - 1;

    // off the left side
    if (x2 < 0)
    return;
    
    // store information in a vissprite
    vis = &avis;
    vis->mobjflags = 0;
    vis->texturemid = (BASEYCENTER<<FRACBITS)+FRACUNIT/2-(psp->sy-spritetopoffset[lump]);
    vis->x1 = x1 < 0 ? 0 : x1;
    vis->x2 = x2 >= ds.viewwidth ? ds.viewwidth-1 : x2;   
    vis->scale = pspritescale<<detailshift;
    
    if (flip)
    {
    vis->xiscale = -pspriteiscale;
    vis->startfrac = spritewidth[lump]-1;
    }
    else
    {
    vis->xiscale = pspriteiscale;
    vis->startfrac = 0;
    }
    
    if (vis->x1 > x1)
    vis->startfrac += vis->xiscale*(vis->x1-x1);

    vis->patch = lump;

    if (viewplayer->powers[pw_invisibility] > 4*32
    || viewplayer->powers[pw_invisibility] & 8)
    {
    // shadow draw
    vis->colormap = NULL;
    }
    else if (fixedcolormap)
    {
    // fixed color
    vis->colormap = fixedcolormap;
    }
    else if (psp->state->frame & FF_FULLBRIGHT)
    {
    // full bright
    vis->colormap = colormaps;
    }
    else
    {
    // local light
    vis->colormap = spritelights[MAXLIGHTSCALE-1];
    }
    
    R_DrawVisSprite (vis, vis->x1, vis->x2);
}



//
// R_DrawPlayerSprites
//
void R_DrawPlayerSprites (void)
{
    int     i;
    int     lightnum;
    pspdef_t*   psp;
    
    // get light level
    lightnum =
    (viewplayer->mo->subsector->sector->lightlevel >> LIGHTSEGSHIFT) 
    +extralight;

    if (lightnum < 0)       
    spritelights = scalelight[0];
    else if (lightnum >= LIGHTLEVELS)
    spritelights = scalelight[LIGHTLEVELS-1];
    else
    spritelights = scalelight[lightnum];
    
    // clip to screen bounds
    mfloorclip = screenheightarray;
    mceilingclip = negonearray;
    
    // add all active psprites
    for (i=0, psp=viewplayer->psprites;
     i<NUMPSPRITES;
     i++,psp++)
    {
    if (psp->state)
        R_DrawPSprite (psp);
    }
}




//
// R_SortVisSprites
//
vissprite_t vsprsortedhead;


void R_SortVisSprites (void)
{
    int         i;
    int         count;
    vissprite_t*    ds;
    vissprite_t*    best;
    vissprite_t     unsorted;
    fixed_t     bestscale;

    count = vissprite_p - vissprites;
    
    unsorted.next = unsorted.prev = &unsorted;

    if (!count)
    return;
        
    for (ds=vissprites ; ds<vissprite_p ; ds++)
    {
    ds->next = ds+1;
    ds->prev = ds-1;
    }
    
    vissprites[0].prev = &unsorted;
    unsorted.next = &vissprites[0];
    (vissprite_p-1)->next = &unsorted;
    unsorted.prev = vissprite_p-1;
    
    // pull the vissprites out by scale
    //best = 0;     // shut up the compiler warning
    vsprsortedhead.next = vsprsortedhead.prev = &vsprsortedhead;
    for (i=0 ; i<count ; i++)
    {
    bestscale = MAXINT;
    for (ds=unsorted.next ; ds!= &unsorted ; ds=ds->next)
    {
        if (ds->scale < bestscale)
        {
        bestscale = ds->scale;
        best = ds;
        }
    }
    best->next->prev = best->prev;
    best->prev->next = best->next;
    best->next = &vsprsortedhead;
    best->prev = vsprsortedhead.prev;
    vsprsortedhead.prev->next = best;
    vsprsortedhead.prev = best;
    }
}



//
// R_DrawSprite
//
void R_DrawSprite (vissprite_t* spr)
{
    drawseg_t*      ds;
    short       clipbot[SCREENWIDTH];
    short       cliptop[SCREENWIDTH];
    int         x;
    int         r1;
    int         r2;
    fixed_t     scale;
    fixed_t     lowscale;
    int         silhouette;
        
    for (x = spr->x1 ; x<=spr->x2 ; x++)
    clipbot[x] = cliptop[x] = -2;
    
    // Scan drawsegs from end to start for obscuring segs.
    // The first drawseg that has a greater scale
    //  is the clip seg.
    for (ds=ds_p-1 ; ds >= drawsegs ; ds--)
    {
    // determine if the drawseg obscures the sprite
    if (ds->x1 > spr->x2
        || ds->x2 < spr->x1
        || (!ds->silhouette
        && !ds->maskedtexturecol) )
    {
        // does not cover sprite
        continue;
    }
            
    r1 = ds->x1 < spr->x1 ? spr->x1 : ds->x1;
    r2 = ds->x2 > spr->x2 ? spr->x2 : ds->x2;

    if (ds->scale1 > ds->scale2)
    {
        lowscale = ds->scale2;
        scale = ds->scale1;
    }
    else
    {
        lowscale = ds->scale1;
        scale = ds->scale2;
    }
        
    if (scale < spr->scale
        || ( lowscale < spr->scale
         && !R_PointOnSegSide (spr->gx, spr->gy, ds->curline) ) )
    {
        // masked mid texture?
        if (ds->maskedtexturecol)   
        R_RenderMaskedSegRange (ds, r1, r2);
        // seg is behind sprite
        continue;           
    }

    
    // clip this piece of the sprite
    silhouette = ds->silhouette;
    
    if (spr->gz >= ds->bsilheight)
        silhouette &= ~SIL_BOTTOM;

    if (spr->gzt <= ds->tsilheight)
        silhouette &= ~SIL_TOP;
            
    if (silhouette == 1)
    {
        // bottom sil
        for (x=r1 ; x<=r2 ; x++)
        if (clipbot[x] == -2)
            clipbot[x] = ds->sprbottomclip[x];
    }
    else if (silhouette == 2)
    {
        // top sil
        for (x=r1 ; x<=r2 ; x++)
        if (cliptop[x] == -2)
            cliptop[x] = ds->sprtopclip[x];
    }
    else if (silhouette == 3)
    {
        // both
        for (x=r1 ; x<=r2 ; x++)
        {
        if (clipbot[x] == -2)
            clipbot[x] = ds->sprbottomclip[x];
        if (cliptop[x] == -2)
            cliptop[x] = ds->sprtopclip[x];
        }
    }
        
    }
    
    // all clipping has been performed, so draw the sprite

    // check for unclipped columns
    for (x = spr->x1 ; x<=spr->x2 ; x++)
    {
    if (clipbot[x] == -2)       
        clipbot[x] = ds.viewheight;

    if (cliptop[x] == -2)
        cliptop[x] = -1;
    }
        
    mfloorclip = clipbot;
    mceilingclip = cliptop;
    R_DrawVisSprite (spr, spr->x1, spr->x2);
}




//
// R_DrawMasked
//
void R_DrawMasked (void)
{
    vissprite_t*    spr;
    drawseg_t*      ds;
    
    R_SortVisSprites ();

    if (vissprite_p > vissprites)
    {
    // draw all vissprites back to front
    for (spr = vsprsortedhead.next ;
         spr != &vsprsortedhead ;
         spr=spr->next)
    {
        
        R_DrawSprite (spr);
    }
    }
    
    // render any remaining masked mid textures
    for (ds=ds_p-1 ; ds >= drawsegs ; ds--)
    if (ds->maskedtexturecol)
        R_RenderMaskedSegRange (ds, ds->x1, ds->x2);
    
    // draw the psprites on top of everything
    //  but does not draw on side views
    if (!viewangleoffset)       
    R_DrawPlayerSprites ();
}

*/


*/
}