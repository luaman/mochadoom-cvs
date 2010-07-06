package rr;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Renderer.java,v 1.2 2010/07/06 12:54:50 velktron Exp $
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

public  fixed_t			viewx;
public  fixed_t			viewy;
public  fixed_t			viewz;

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
public static int
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


int
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


//
// R_PointToAngle
// To get a global angle from cartesian coordinates,
//  the coordinates are flipped until they are in
//  the first octant of the coordinate system, then
//  the y (<=x) is scaled and divided by x to get a
//  tangent (slope) value which is looked up in the
//  tantoangle[] table.

//




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
    return 0;
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

    R_InitBuffer (scaledviewwidth, viewheight);
	
    R_InitTextureMapping ();
    
    // psprite scales
    pspritescale = FRACUNIT*viewwidth/SCREENWIDTH;
    pspriteiscale = FRACUNIT*SCREENWIDTH/viewwidth;
    
    // thing clipping
    for (i=0 ; i<viewwidth ; i++)
	screenheightarray[i] = viewheight;
    
    // planes
    for (i=0 ; i<viewheight ; i++)
    {
	dy = ((i-viewheight/2)<<FRACBITS)+FRACUNIT/2;
	dy = abs(dy);
	yslope[i] = FixedDiv ( (viewwidth<<detailshift)/2*FRACUNIT, dy);
    }
	
    for (i=0 ; i<viewwidth ; i++)
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
	    level = startmap - j*SCREENWIDTH/(viewwidth<<detailshift)/DISTMAP;
	    
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
    // viewwidth / viewheight / detailLevel are set by the defaults
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

//
//R_GetColumn
//
byte*
R_GetColumn
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
void R_InitTextures (void)
{
 maptexture_t*   mtexture;
 texture_t*      texture;
 mappatch_t*     mpatch;
 texpatch_t*     patch;

 int         i;
 int         j;

 int*        maptex;
 int*        maptex2;
 int*        maptex1;
 
 char        name[9];
 char*       names;
 char*       name_p;
 
 int*        patchlookup;
 
 int         totalwidth;
 int         nummappatches;
 int         offset;
 int         maxoff;
 int         maxoff2;
 int         numtextures1;
 int         numtextures2;

 int*        directory;
 
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

}