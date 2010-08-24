// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: BSP.java,v 1.4 2010/08/24 14:57:42 velktron Exp $
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
// $Log: BSP.java,v $
// Revision 1.4  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
//
// Revision 1.3  2010/07/06 12:54:50  velktron
// A lot of work thrown in the renderer, but don't get too excited...
//
// Revision 1.2  2010/07/05 16:18:40  velktron
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
//	BSP traversal, handling of LineSegs for rendering.
//
//-----------------------------------------------------------------------------
package rr;

import m.fixed_t;
import data.doomstat;

/*
#include "doomdef.h"

#include "m_bbox.h"

#include "i_system.h"

#include "r_main.h"
#include "r_plane.h"
#include "r_things.h"

// State.
#include "doomstat.h"
#include "r_state.h"
*/

public class BSP{

// static const char rcsid[] = "$Id: BSP.java,v 1.4 2010/08/24 14:57:42 velktron Exp $";

    private doomstat ds;
    
    public seg_t       curline;
    public side_t      sidedef;
    public line_t      linedef;
    public sector_t    frontsector;
    public sector_t    backsector;

    public int      rw_x;
    public int      rw_stopx;

    public boolean      segtextured;

    // false if the back side is the same plane
    public boolean      markfloor;      
    public boolean      markceiling;

    public boolean      skymap;

    public drawseg_t[]    drawsegs;
    /** pointer to drawseg */
    public int   ds_p;

    public lighttable_t[][]   hscalelight;
    public lighttable_t[][]   vscalelight;
    public lighttable_t[][]   dscalelight;

    /*
    public interface drawfunc_t {

        // MAES: This is defined but apparently never used. So let's not bother...
       //  typedef void (*drawfunc_t) (int start, int stop);

           public void draw(int start, int stop);
           
       } */
 
    /* An example of what it would look like
class R_StoreWallRange implements drawfunc_t{
 public void draw(int	start,
  int	stop ){
}
}*/
    
          public void    R_StoreWallRange(int   start, int   stop ){
          }

//
// R_ClearDrawSegs
//
public void ClearDrawSegs ()
{
    ds_p = 0;
}



//
// ClipWallSegment
// Clips the given range of columns
// and includes it in the new clip list.
//
class cliprange_t
{
    public int	first;
    public int last;
    
} 

public static final int MAXSEGS=32;

// newend is one past the last valid seg
cliprange_t	newend;
cliprange_t[]	solidsegs= new cliprange_t[MAXSEGS];

//
// R_ClipSolidWallSegment
// Does handle solid walls,
//  e.g. single sided LineDefs (middle texture)
//  that entirely block the view.
// 

class R_ClipSolidWallSegment implements drawfunc_t{
public void draw(int   first,
        int   last ){
    int	next;
    int	start;

    // Find the first range that touches the range
    //  (adjacent pixels are touching).
    start = 0;
    
    while (solidsegs[start].last < first-1)
	start++;

    if (first < solidsegs[start].first)
    {
	if (last < solidsegs[start].first-1)
	{
	    // Post is entirely visible (above start),
	    //  so insert a new clippost.
	    R_StoreWallRange (first, last);
	    next = newend;
	    newend++;
	    
	    while (next != start)
	    {
	        solidsegs[next] = solidsegs[next-1];
		next--;
	    }
	    solidsegs[next].first = first;
	    solidsegs[next].last = last;
	    return;
	}
		
	// There is a fragment above *start.
	R_StoreWallRange (first, solidsegs[start].first - 1);
	// Now adjust the clip size.
	solidsegs[start].first = first;	
    }

    // Bottom contained in start?
    if (last <= solidsegs[start].last)
	return;			
		
    next = start;
    while (last >= solidsegs[(next+1)].first-1)
    {
	// There is a fragment between two posts.
	R_StoreWallRange (solidsegs[next].last + 1, solidsegs[next+1].first - 1);
	next++;
	
	if (last <= next.last)
	{
	    // Bottom is contained in next.
	    // Adjust the clip size.
	    start->last = next->last;	
	    goto crunch;
	}
    }
	
    // There is a fragment after *next.
    R_StoreWallRange (next->last + 1, last);
    // Adjust the clip size.
    start->last = last;
	
    // Remove start+1 to next from the clip list,
    // because start now covers their area.
  crunch:
    if (next == start)
    {
	// Post just extended past the bottom of one post.
	return;
    }
    

    while (next++ != newend)
    {
	// Remove a post.
	*++start = *next;
    }

    newend = start+1;
}



//
// R_ClipPassWallSegment
// Clips the given range of columns,
//  but does not includes it in the clip list.
// Does handle windows,
//  e.g. LineDefs with upper and lower texture.
//
void
R_ClipPassWallSegment
( int	first,
  int	last )
{
    cliprange_t	start;

    // Find the first range that touches the range
    //  (adjacent pixels are touching).
    int startptr=0;
    start = solidsegs[startptr];
    while (start.last < first-1)
	startptr++;

    start = solidsegs[startptr];
    
    
    if (first < start.first)
    {
	if (last < start.first-1)
	{
	    // Post is entirely visible (above start).
	    R_StoreWallRange (first, last);
	    return;
	}
		
	// There is a fragment above *start.
	R_StoreWallRange (first, start.first - 1);
    }

    // Bottom contained in start?
    if (last <= start.last)
	return;			
		
    while (last >= solidsegs[startptr+1].first-1)
    {
	// There is a fragment between two posts.
	R_StoreWallRange (start.last + 1, solidsegs[startptr+1].first - 1);
	startptr++;
	start=solidsegs[startptr];
	
	if (last <= start.last)
	    return;
    }
	
    // There is a fragment after *next.
    R_StoreWallRange (start.last + 1, last);
}



//
// R_ClearClipSegs
//
public void R_ClearClipSegs ()
{
    solidsegs[0].first = -0x7fffffff;
    solidsegs[0].last = -1;
    solidsegs[1].first = ds.viewwidth;
    solidsegs[1].last = 0x7fffffff;
    newend = solidsegs[2];
}

//
// R_AddLine
// Clips the given segment
// and adds any visible pieces to the line list.
//
void R_AddLine (seg_t	line)
{
    int			x1;
    int			x2;
    int		angle1;
    int		angle2;
    int		span;
    int		tspan;
    
    curline = line;

    // OPTIMIZE: quickly reject orthogonal back sides.
    angle1 = R_PointToAngle (line.v1.x.val, line.v1.y.val);
    angle2 = R_PointToAngle (line.v2.x.val, line.v2.y.val);
    
    // Clip to view edges.
    // OPTIMIZE: make constant out of 2*clipangle (FIELDOFVIEW).
    span = angle1 - angle2;
    
    // Back side? I.e. backface culling?
    if (span >= ANG180)
	return;		

    // Global angle needed by segcalc.
    rw_angle1 = angle1;
    angle1 -= viewangle;
    angle2 -= viewangle;
	
    tspan = angle1 + clipangle;
    if (tspan > 2*clipangle)
    {
	tspan -= 2*clipangle;

	// Totally off the left edge?
	if (tspan >= span)
	    return;
	
	angle1 = clipangle;
    }
    tspan = clipangle - angle2;
    if (tspan > 2*clipangle)
    {
	tspan -= 2*clipangle;

	// Totally off the left edge?
	if (tspan >= span)
	    return;	
	angle2 = -clipangle;
    }
    
    // The seg is in the view range,
    // but not necessarily visible.
    angle1 = (angle1+ANG90)>>ANGLETOFINESHIFT;
    angle2 = (angle2+ANG90)>>ANGLETOFINESHIFT;
    x1 = viewangletox[angle1];
    x2 = viewangletox[angle2];

    // Does not cross a pixel?
    if (x1 == x2)
	return;				
	
    backsector = line->backsector;

    // Single sided line?
    if (!backsector)
	goto clipsolid;		

    // Closed door.
    if (backsector->ceilingheight <= frontsector->floorheight
	|| backsector->floorheight >= frontsector->ceilingheight)
	goto clipsolid;		

    // Window.
    if (backsector->ceilingheight != frontsector->ceilingheight
	|| backsector->floorheight != frontsector->floorheight)
	goto clippass;	
		
    // Reject empty lines used for triggers
    //  and special events.
    // Identical floor and ceiling on both sides,
    // identical light levels on both sides,
    // and no middle texture.
    if (backsector->ceilingpic == frontsector->ceilingpic
	&& backsector->floorpic == frontsector->floorpic
	&& backsector->lightlevel == frontsector->lightlevel
	&& curline->sidedef->midtexture == 0)
    {
	return;
    }
    
				
  clippass:
    R_ClipPassWallSegment (x1, x2-1);	
    return;
		
  clipsolid:
    R_ClipSolidWallSegment (x1, x2-1);
}


//
// R_CheckBBox
// Checks BSP node/subtree bounding box.
// Returns true
//  if some part of the bbox might be visible.
//
private int[][]	checkcoord =
{
    {3,0,2,1},
    {3,0,2,0},
    {3,1,2,0},
    {0},
    {2,0,2,1},
    {0,0,0,0},
    {3,1,3,0},
    {0},
    {2,0,3,1},
    {2,1,3,1},
    {2,1,3,0}
};


boolean R_CheckBBox (fixed_t	bspcoord)
{
    int			boxx;
    int			boxy;
    int			boxpos;

    // fixed_t
    int		x1;
    int		y1;
    int		x2;
    int		y2;
    
    //angle_t
    int		angle1;
    int		angle2;
    int		span;
    int		tspan;
    
    cliprange_t	start;

    int			sx1;
    int			sx2;
    
    // Find the corners of the box
    // that define the edges from current viewpoint.
    if (viewx <= bspcoord[BOXLEFT])
	boxx = 0;
    else if (viewx < bspcoord[BOXRIGHT])
	boxx = 1;
    else
	boxx = 2;
		
    if (viewy >= bspcoord[BOXTOP])
	boxy = 0;
    else if (viewy > bspcoord[BOXBOTTOM])
	boxy = 1;
    else
	boxy = 2;
		
    boxpos = (boxy<<2)+boxx;
    if (boxpos == 5)
	return true;
	
    x1 = bspcoord[checkcoord[boxpos][0]];
    y1 = bspcoord[checkcoord[boxpos][1]];
    x2 = bspcoord[checkcoord[boxpos][2]];
    y2 = bspcoord[checkcoord[boxpos][3]];
    
    // check clip list for an open space
    angle1 = R_PointToAngle (x1, y1) - viewangle;
    angle2 = R_PointToAngle (x2, y2) - viewangle;
	
    span = angle1 - angle2;

    // Sitting on a line?
    if (span >= ANG180)
	return true;
    
    tspan = angle1 + clipangle;

    if (tspan > 2*clipangle)
    {
	tspan -= 2*clipangle;

	// Totally off the left edge?
	if (tspan >= span)
	    return false;	

	angle1 = clipangle;
    }
    tspan = clipangle - angle2;
    if (tspan > 2*clipangle)
    {
	tspan -= 2*clipangle;

	// Totally off the left edge?
	if (tspan >= span)
	    return false;
	
	angle2 = -clipangle;
    }


    // Find the first clippost
    //  that touches the source post
    //  (adjacent pixels are touching).
    angle1 = (angle1+ANG90)>>ANGLETOFINESHIFT;
    angle2 = (angle2+ANG90)>>ANGLETOFINESHIFT;
    sx1 = viewangletox[angle1];
    sx2 = viewangletox[angle2];

    // Does not cross a pixel.
    if (sx1 == sx2)
	return false;			
    sx2--;
	
    start = solidsegs;
    while (start->last < sx2)
	start++;
    
    if (sx1 >= start->first
	&& sx2 <= start->last)
    {
	// The clippost contains the new span.
	return false;
    }

    return true;
}



//
// R_Subsector
// Determine floor/ceiling planes.
// Add sprites of things in sector.
// Draw one or more line segments.
//
void R_Subsector (int num)
{
    int			count;
    seg_t*		line;
    subsector_t*	sub;
	
#ifdef RANGECHECK
    if (num>=numsubsectors)
	I_Error ("R_Subsector: ss %i with numss = %i",
		 num,
		 numsubsectors);
#endif

    sscount++;
    sub = &subsectors[num];
    frontsector = sub->sector;
    count = sub->numlines;
    line = &segs[sub->firstline];

    if (frontsector->floorheight < viewz)
    {
	floorplane = R_FindPlane (frontsector->floorheight,
				  frontsector->floorpic,
				  frontsector->lightlevel);
    }
    else
	floorplane = NULL;
    
    if (frontsector->ceilingheight > viewz 
	|| frontsector->ceilingpic == skyflatnum)
    {
	ceilingplane = R_FindPlane (frontsector->ceilingheight,
				    frontsector->ceilingpic,
				    frontsector->lightlevel);
    }
    else
	ceilingplane = NULL;
		
    R_AddSprites (frontsector);	

    while (count--)
    {
	R_AddLine (line);
	line++;
    }
}




//
// RenderBSPNode
// Renders all subsectors below a given node,
//  traversing subtree recursively.
// Just call with BSP root.
void R_RenderBSPNode (int bspnum)
{
    node_t	bsp;
    int		side;

    // Found a subsector?
    if (bspnum & NF_SUBSECTOR)
    {
	if (bspnum == -1)			
	    R_Subsector (0);
	else
	    R_Subsector (bspnum&(~NF_SUBSECTOR));
	return;
    }
		
    bsp = nodes[bspnum];
    
    // Decide which side the view point is on.
    side = R_PointOnSide (viewx, viewy, bsp);

    // Recursively divide front space.
    R_RenderBSPNode (bsp.children[side]); 

    // Possibly divide back space.
    if (R_CheckBBox (bsp.bbox[side^1]))	
	R_RenderBSPNode (bsp.children[side^1]);
}


}