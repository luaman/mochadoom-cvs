package w;

/* Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: EndLevel.java,v 1.1 2010/07/06 16:32:38 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This source is available for distribution and/or modification
// only under the terms of the DOOM Source Code License as
// published by id Software. All rights reserved.
//
// The source is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// FITNESS FOR A PARTICULAR PURPOSE. See the DOOM Source Code License
// for more details.
//
// $Log: EndLevel.java,v $
// Revision 1.1  2010/07/06 16:32:38  velktron
// Threw some work in WI, now EndLevel. YEAH THERE'S GONNA BE A SEPARATE EndLevel OBJECT THAT'S HOW PIMP THE PROJECT IS!!!!11!!!
//
// Revision 1.1  2010/06/30 08:58:51  velktron
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
//	Intermission screens.
//
//-----------------------------------------------------------------------------*/


import static data.Defines.*;
import data.Defines.GameMode_t;
import doom.DoomContext;
import doom.event_t;
import doom.wbplayerstruct_t;
import doom.wbstartstruct_t;
import m.random;
import rr.*;
import utils.C2JUtils;
import utils.PrintfFormat;

/** This class (stuff.c) seems to implement the endlevel screens.
 *  If we want to go truly OO, it should be made non-static.
 * 
 * @author Maes
 *
 */

public class EndLevel{

    
    private DoomContext DC;
    /*
#include <stdio.h>

#include "z_zone.h"

#include "m_random.h"
#include "m_swap.h"

#include "i_system.h"

#include "w_wad.h"

#include "g_game.h"

#include "r_local.h"
#include "s_sound.h"

#include "doomstat.h"

// Data.
#include "sounds.h"

// Needs access to LFB.
#include "v_video.h"

#include "wi_stuff.h"


*/


//
// GENERAL DATA
//

//
// Locally used stuff.
//
public final int FB= 0;


// States for single-player
static int SP_KILLS	=	0;
static int SP_ITEMS	=	2;
static int SP_SECRET	=	4;
static int SP_FRAGS	=	6 ;
static int SP_TIME		=	8 ;
static int SP_PAR		=	SP_TIME;

static int SP_PAUSE	=	1;

// in seconds
static  int SHOWNEXTLOCDELAY	=4;
static  int SHOWLASTLOCDELAY	=SHOWNEXTLOCDELAY;


// used to accelerate or skip a stage
int		acceleratestage;

// wbs->pnum
int		me;

 // specifies current state )
int	state;

// contains information passed into intermission
wbstartstruct_t	wbs;

wbplayerstruct_t[] plrs;  // wbs->plyr[]

// used for general timing
int 		cnt;  

// used for timing of background animation
int 		bcnt;

// signals to refresh everything for one frame
int 		firstrefresh; 

int[]		cnt_kills=new int[MAXPLAYERS];
int[]		cnt_items=new int[MAXPLAYERS];
int[]		cnt_secret=new int[MAXPLAYERS];
int		cnt_time;
static int		cnt_par;
static int		cnt_pause;

// # of commercial levels
int		NUMCMAPS; 


//
//	GRAPHICS
//

// background (map of levels).
static patch_t		bg;

// You Are Here graphic
static patch_t[]		yah=new patch_t[2]; 

// splat
static patch_t		splat;

// %, : graphics
static patch_t		percent;
static patch_t		colon;

// 0-9 graphic
static patch_t[]		num=new patch_t[10];

// minus sign
static patch_t	wiminus;

// "Finished!" graphics
static patch_t		finished;

// "Entering" graphic
static patch_t		entering; 

// "secret"
static patch_t		sp_secret;

 // "Kills", "Scrt", "Items", "Frags"
static patch_t		kills;
static patch_t		secret;
static patch_t		items;
static patch_t		frags;

// Time sucks.
static patch_t		time;
static patch_t		par;
static patch_t		sucks;

// "killers", "victims"
static patch_t		killers;
static patch_t		victims; 

// "Total", your face, your dead face
static patch_t		total;
static patch_t		star;
static patch_t		bstar;

// "red P[1..MAXPLAYERS]"
static patch_t[]		p=new patch_t[MAXPLAYERS];

// "gray P[1..MAXPLAYERS]"
static patch_t[] bp=new patch_t[MAXPLAYERS];

 // Name graphics of each level (centered)
static patch_t[]	lnames;

//
// CODE
//

// slam background
// UNUSED static unsigned char *background=0;


public void slamBackground()
{
    System.arraycopy(DC.V.getScreen(0), 0 ,DC.V.getScreen(1),0, SCREENWIDTH * SCREENHEIGHT);
    DC.V.MarkRect (0, 0, SCREENWIDTH, SCREENHEIGHT);
}

// The ticker is used to detect keys
//  because of timing issues in netgames.
boolean Responder(event_t ev)
{
    return false;
}


// Draws "<Levelname> Finished!"
public void drawLF()
{
    int y = WI_TITLEY;

    // draw <LevelName> 
    DC.V.DrawPatch((SCREENWIDTH - lnames[wbs.last].width)/2,
		y, FB, lnames[wbs.last]);

    // draw "Finished!"
    y += (5*lnames[wbs.last].height)/4;
    
    DC.V.DrawPatch((SCREENWIDTH - finished.width)/2,
		y, FB, finished);
}



// Draws "Entering <LevelName>"
public void WI_drawEL()
{
    int y = WI_TITLEY;

    // draw "Entering"
    DC.V.DrawPatch((SCREENWIDTH - entering.width/2),
		y, FB, entering);

    // draw level
    y += (5*lnames[wbs.next].height)/4;

    DC.V.DrawPatch((SCREENWIDTH - lnames[wbs.next].width)/2,
		y, FB, lnames[wbs.next]);

}

public void
WI_drawOnLnode
( int		n,
  patch_t[]	c )
{

    int		i;
    int		left;
    int		top;
    int		right;
    int		bottom;
    boolean	fits = false;

    i = 0;
    do
    {
	left = lnodes[wbs.epsd][n].x - c[i].leftoffset;
	top = lnodes[wbs.epsd][n].y - c[i].topoffset;
	right = left + c[i].width;
	bottom = top + c[i].height;

	if (left >= 0
	    && right < SCREENWIDTH
	    && top >= 0
	    && bottom < SCREENHEIGHT)
	{
	    fits = true;
	}
	else
	{
	    i++;
	}
    } while (!fits && i!=2);

    if (fits && i<2)
    {
	DC.V.DrawPatch(lnodes[wbs.epsd][n].x, lnodes[wbs.epsd][n].y,
		    FB, c[i]);
    }
    else
    {
	// DEBUG
	System.out.println("Could not place patch on level "+ n+1); 
    }
}



public void initAnimatedBack()
{
    int		i;
    anim_t	a;

    if (DC.DS.gamemode == GameMode_t.commercial)
	return;

    if (wbs.epsd > 2)
	return;

    for (i=0;i<NUMANIMS[wbs.epsd];i++)
    {
	a = anims[wbs.epsd][i];

	// init variables
	a.ctr = -1;

	// specify the next time to draw it
	if (a.type == animenum_t.ANIM_ALWAYS)
	    a.nexttic = bcnt + 1 + (DC.RND.M_Random()%a.period);
	else if (a.type == animenum_t.ANIM_RANDOM)
	    a.nexttic = bcnt + 1 + a.data2+(DC.RND.M_Random()%a.data1);
	else if (a.type == animenum_t.ANIM_LEVEL)
	    a.nexttic = bcnt + 1;
    }

}

public void updateAnimatedBack()
{
    int		i;
    anim_t	a;

    if (DC.DS.gamemode == GameMode_t.commercial)
	return;

    if (wbs.epsd > 2)
	return;

    int aaptr=wbs.epsd;
    
    for (i=0;i<NUMANIMS[wbs.epsd];i++)
    {
	a = anims[aaptr][i];
	
	if (bcnt == a.nexttic)
	{
	    switch (a.type)
	    {
	      case ANIM_ALWAYS:
		if (anims[++aaptr][i].ctr >= a.nanims) a.ctr = 0;
		a.nexttic = bcnt + a.period;
		break;

	      case ANIM_RANDOM:
		a.ctr++;
		if (a.ctr == a.nanims)
		{
		    a.ctr = -1;
		    a.nexttic = bcnt+a.data2+(DC.RND.M_Random()%a.data1);
		}
		else a.nexttic = bcnt + a.period;
		break;
		
	      case ANIM_LEVEL:
		// gawd-awful hack for level anims
		if (!(state == StatCount && i == 7)
		    && wbs.next == a.data1)
		{
		    a.ctr++;
		    if (a.ctr == a.nanims) a.ctr--;
		    a.nexttic = bcnt + a.period;
		}
		break;
	    }
	}

    }

}

public void drawAnimatedBack()
{
    int			i;
    anim_t		a;

    if (DC.DS.gamemode==GameMode_t.commercial)
	return;

    if (wbs.epsd > 2)
	return;

    for (i=0 ; i<NUMANIMS[wbs.epsd] ; i++)
    {
	a = anims[wbs.epsd][i];

	if (a.ctr >= 0)
	    DC.V.DrawPatch(a.loc.x, a.loc.y, FB, a.p[a.ctr]);
    }

}

//
// Draws a number.
// If digits > 0, then use that many digits minimum,
//  otherwise only use as many as necessary.
// Returns new x position.
//

public int
drawNum
( int		x,
  int		y,
  int		n,
  int		digits )
{

    int		fontwidth = num[0].width;
    boolean		neg;
    int		temp;

    if (digits < 0)
    {
	if (n==0)
	{
	    // make variable-length zeros 1 digit long
	    digits = 1;
	}
	else
	{
	    // figure out # of digits in #
	    digits = 0;
	    temp = n;

	    while (temp!=0)
	    {
		temp /= 10;
		digits++;
	    }
	}
    }

    neg = (n < 0);
    if (neg)
	n = -n;

    // if non-number, do not draw it
    if (n == 1994)
	return 0;

    // draw the new number
    while ((digits--)!=0)
    {
	x -= fontwidth;
	DC.V.DrawPatch(x, y, FB, num[ n % 10 ]);
	n /= 10;
    }

    // draw a minus sign if necessary
    if (neg)
	DC.V.DrawPatch(x-=8, y, FB, wiminus);

    return x;

}

public 

void
drawPercent
( int		x,
  int		y,
  int		p )
{
    if (p < 0)
	return;

    DC.V.DrawPatch(x, y, FB, percent);
    drawNum(x, y, p, -1);
}



//
// Display level completion time and par,
//  or "sucks" message if overflow.
//
public void drawTime
( int		x,
  int		y,
  int		t )
{

    int		div;
    int		n;

    if (t<0)
	return;

    if (t <= 61*59)
    {
	div = 1;

	do
	{
	    n = (t / div) % 60;
	    x = drawNum(x, y, n, 2) - colon.width;
	    div *= 60;

	    // draw
	    if ((div==60) || (t / div)>0)
		DC.V.DrawPatch(x, y, FB, colon);
	    
	} while ((t / div)>0);
    }
    else
    {
	// "sucks"
	DC.V.DrawPatch(x - sucks.width, y, FB, sucks); 
    }
}


public void End()
{
    //unloadData();
}


public void initNoState()
{
    state = NoState;
    acceleratestage = 0;
    cnt = 10;
}

public void updateNoState() {

    updateAnimatedBack();

    if (--cnt==00)
    {
	End();
	//WorldDone();
    }

}

boolean		snl_pointeron = false;


public void WI_initShowNextLoc()
{
    state = ShowNextLoc;
    acceleratestage = 0;
    cnt = SHOWNEXTLOCDELAY * TICRATE;

    initAnimatedBack();
}

void updateShowNextLoc()
{
    updateAnimatedBack();

    if ((--cnt==0) || (acceleratestage!=0))
	initNoState();
    else
	snl_pointeron = (cnt & 31) < 20;
}

void drawShowNextLoc()
{

    int		i;
    int		last;

    slamBackground();

    // draw animated background
    drawAnimatedBack(); 

    if ( DC.DS.gamemode != GameMode_t.commercial)
    {
  	if (wbs.epsd > 2)
	{
	    WI_drawEL();
	    return;
	}
	
	last = (wbs.last == 8) ? wbs.next - 1 : wbs.last;

	// draw a splat on taken cities.
	for (i=0 ; i<=last ; i++)
	    drawOnLnode(i, splat);

	// splat the secret level?
	if (wbs.didsecret)
	    drawOnLnode(8, splat);

	// draw flashing ptr
	if (snl_pointeron)
	    WI_drawOnLnode(wbs.next, yah); 
    }

    // draws which level you are entering..
    if ( (gamemode != commercial)
	 || wbs.next != 30)
	WI_drawEL();  

}

public void drawNoState()
{
    snl_pointeron = true;
    drawShowNextLoc();
}

int fragSum(int playernum)
{
    int		i;
    int		frags = 0;
    
    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (DC.DS.playeringame[i]
	    && i!=playernum)
	{
	    frags += plrs[playernum].frags[i];
	}
    }

	
    // JDC hack - negative frags.
    frags -= plrs[playernum].frags[playernum];
    // UNUSED if (frags < 0)
    // 	frags = 0;

    return frags;
}



int		dm_state;
int[][]		dm_frags=new int[MAXPLAYERS][MAXPLAYERS];
int	[]	dm_totals=new int[MAXPLAYERS];



public void initDeathmatchStats()
{

    int		i;
    int		j;

    state = StatCount;
    acceleratestage = 0;
    dm_state = 1;

    cnt_pause = TICRATE;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (DC.DS.playeringame[i])
	{
	    for (j=0 ; j<MAXPLAYERS ; j++)
		if (DC.DS.playeringame[j])
		    dm_frags[i][j] = 0;

	    dm_totals[i] = 0;
	}
    }
    
    initAnimatedBack();
}



void updateDeathmatchStats()
{

    int		i;
    int		j;
    
    boolean	stillticking;

    updateAnimatedBack();

    if ((acceleratestage!=0) && (dm_state != 4))
    {
	acceleratestage = 0;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (DC.DS.playeringame[i])
	    {
		for (j=0 ; j<MAXPLAYERS ; j++)
		    if (DC.DS.playeringame[j])
			dm_frags[i][j] = plrs[i].frags[j];

		dm_totals[i] = fragSum(i);
	    }
	}
	

	// TODO:S_StartSound(0, sfx_barexp);
	dm_state = 4;
    }

    
    if (dm_state == 2)
    {
	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);
	
	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (playeringame[i])
	    {
		for (j=0 ; j<MAXPLAYERS ; j++)
		{
		    if (playeringame[j]
			&& dm_frags[i][j] != plrs[i].frags[j])
		    {
			if (plrs[i].frags[j] < 0)
			    dm_frags[i][j]--;
			else
			    dm_frags[i][j]++;

			if (dm_frags[i][j] > 99)
			    dm_frags[i][j] = 99;

			if (dm_frags[i][j] < -99)
			    dm_frags[i][j] = -99;
			
			stillticking = true;
		    }
		}
		dm_totals[i] = WI_fragSum(i);

		if (dm_totals[i] > 99)
		    dm_totals[i] = 99;
		
		if (dm_totals[i] < -99)
		    dm_totals[i] = -99;
	    }
	    
	}
	if (!stillticking)
	{
	    S_StartSound(0, sfx_barexp);
	    dm_state++;
	}

    }
    else if (dm_state == 4)
    {
	if (acceleratestage)
	{
	    S_StartSound(0, sfx_slop);

	    if ( gamemode == commercial)
		WI_initNoState();
	    else
		WI_initShowNextLoc();
	}
    }
    else if (dm_state & 1)
    {
	if (!--cnt_pause)
	{
	    dm_state++;
	    cnt_pause = TICRATE;
	}
    }
}



void WI_drawDeathmatchStats(void)
{

    int		i;
    int		j;
    int		x;
    int		y;
    int		w;
    
    int		lh;	// line height

    lh = WI_SPACINGY;

    WI_slamBackground();
    
    // draw animated background
    WI_drawAnimatedBack(); 
    WI_drawLF();

    // draw stat titles (top line)
    V_DrawPatch(DM_TOTALSX-SHORT(total.width)/2,
		DM_MATRIXY-WI_SPACINGY+10,
		FB,
		total);
    
    V_DrawPatch(DM_KILLERSX, DM_KILLERSY, FB, killers);
    V_DrawPatch(DM_VICTIMSX, DM_VICTIMSY, FB, victims);

    // draw P?
    x = DM_MATRIXX + DM_SPACINGX;
    y = DM_MATRIXY;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (playeringame[i])
	{
	    V_DrawPatch(x-SHORT(p[i].width)/2,
			DM_MATRIXY - WI_SPACINGY,
			FB,
			p[i]);
	    
	    V_DrawPatch(DM_MATRIXX-SHORT(p[i].width)/2,
			y,
			FB,
			p[i]);

	    if (i == me)
	    {
		V_DrawPatch(x-SHORT(p[i].width)/2,
			    DM_MATRIXY - WI_SPACINGY,
			    FB,
			    bstar);

		V_DrawPatch(DM_MATRIXX-SHORT(p[i].width)/2,
			    y,
			    FB,
			    star);
	    }
	}
	else
	{
	    // V_DrawPatch(x-SHORT(bp[i].width)/2,
	    //   DM_MATRIXY - WI_SPACINGY, FB, bp[i]);
	    // V_DrawPatch(DM_MATRIXX-SHORT(bp[i].width)/2,
	    //   y, FB, bp[i]);
	}
	x += DM_SPACINGX;
	y += WI_SPACINGY;
    }

    // draw stats
    y = DM_MATRIXY+10;
    w = SHORT(num[0].width);

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	x = DM_MATRIXX + DM_SPACINGX;

	if (playeringame[i])
	{
	    for (j=0 ; j<MAXPLAYERS ; j++)
	    {
		if (playeringame[j])
		    WI_drawNum(x+w, y, dm_frags[i][j], 2);

		x += DM_SPACINGX;
	    }
	    WI_drawNum(DM_TOTALSX+w, y, dm_totals[i], 2);
	}
	y += WI_SPACINGY;
    }
}

static int	cnt_frags[MAXPLAYERS];
static int	dofrags;
static int	ng_state;

void WI_initNetgameStats(void)
{

    int i;

    state = StatCount;
    acceleratestage = 0;
    ng_state = 1;

    cnt_pause = TICRATE;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (!playeringame[i])
	    continue;

	cnt_kills[i] = cnt_items[i] = cnt_secret[i] = cnt_frags[i] = 0;

	dofrags += WI_fragSum(i);
    }

    dofrags = !!dofrags;

    WI_initAnimatedBack();
}



void WI_updateNetgameStats(void)
{

    int		i;
    int		fsum;
    
    boolean	stillticking;

    WI_updateAnimatedBack();

    if (acceleratestage && ng_state != 10)
    {
	acceleratestage = 0;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!playeringame[i])
		continue;

	    cnt_kills[i] = (plrs[i].skills * 100) / wbs.maxkills;
	    cnt_items[i] = (plrs[i].sitems * 100) / wbs.maxitems;
	    cnt_secret[i] = (plrs[i].ssecret * 100) / wbs.maxsecret;

	    if (dofrags)
		cnt_frags[i] = WI_fragSum(i);
	}
	S_StartSound(0, sfx_barexp);
	ng_state = 10;
    }

    if (ng_state == 2)
    {
	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!playeringame[i])
		continue;

	    cnt_kills[i] += 2;

	    if (cnt_kills[i] >= (plrs[i].skills * 100) / wbs.maxkills)
		cnt_kills[i] = (plrs[i].skills * 100) / wbs.maxkills;
	    else
		stillticking = true;
	}
	
	if (!stillticking)
	{
	    S_StartSound(0, sfx_barexp);
	    ng_state++;
	}
    }
    else if (ng_state == 4)
    {
	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!playeringame[i])
		continue;

	    cnt_items[i] += 2;
	    if (cnt_items[i] >= (plrs[i].sitems * 100) / wbs.maxitems)
		cnt_items[i] = (plrs[i].sitems * 100) / wbs.maxitems;
	    else
		stillticking = true;
	}
	if (!stillticking)
	{
	    S_StartSound(0, sfx_barexp);
	    ng_state++;
	}
    }
    else if (ng_state == 6)
    {
	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!playeringame[i])
		continue;

	    cnt_secret[i] += 2;

	    if (cnt_secret[i] >= (plrs[i].ssecret * 100) / wbs.maxsecret)
		cnt_secret[i] = (plrs[i].ssecret * 100) / wbs.maxsecret;
	    else
		stillticking = true;
	}
	
	if (!stillticking)
	{
	    S_StartSound(0, sfx_barexp);
	    ng_state += 1 + 2*!dofrags;
	}
    }
    else if (ng_state == 8)
    {
	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!playeringame[i])
		continue;

	    cnt_frags[i] += 1;

	    if (cnt_frags[i] >= (fsum = WI_fragSum(i)))
		cnt_frags[i] = fsum;
	    else
		stillticking = true;
	}
	
	if (!stillticking)
	{
	    S_StartSound(0, sfx_pldeth);
	    ng_state++;
	}
    }
    else if (ng_state == 10)
    {
	if (acceleratestage)
	{
	    S_StartSound(0, sfx_sgcock);
	    if ( gamemode == commercial )
		WI_initNoState();
	    else
		WI_initShowNextLoc();
	}
    }
    else if (ng_state & 1)
    {
	if (!--cnt_pause)
	{
	    ng_state++;
	    cnt_pause = TICRATE;
	}
    }
}



void WI_drawNetgameStats(void)
{
    int		i;
    int		x;
    int		y;
    int		pwidth = SHORT(percent.width);

    WI_slamBackground();
    
    // draw animated background
    WI_drawAnimatedBack(); 

    WI_drawLF();

    // draw stat titles (top line)
    V_DrawPatch(NG_STATSX+NG_SPACINGX-SHORT(kills.width),
		NG_STATSY, FB, kills);

    V_DrawPatch(NG_STATSX+2*NG_SPACINGX-SHORT(items.width),
		NG_STATSY, FB, items);

    V_DrawPatch(NG_STATSX+3*NG_SPACINGX-SHORT(secret.width),
		NG_STATSY, FB, secret);
    
    if (dofrags)
	V_DrawPatch(NG_STATSX+4*NG_SPACINGX-SHORT(frags.width),
		    NG_STATSY, FB, frags);

    // draw stats
    y = NG_STATSY + SHORT(kills.height);

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (!playeringame[i])
	    continue;

	x = NG_STATSX;
	V_DrawPatch(x-SHORT(p[i].width), y, FB, p[i]);

	if (i == me)
	    V_DrawPatch(x-SHORT(p[i].width), y, FB, star);

	x += NG_SPACINGX;
	WI_drawPercent(x-pwidth, y+10, cnt_kills[i]);	x += NG_SPACINGX;
	WI_drawPercent(x-pwidth, y+10, cnt_items[i]);	x += NG_SPACINGX;
	WI_drawPercent(x-pwidth, y+10, cnt_secret[i]);	x += NG_SPACINGX;

	if (dofrags)
	    WI_drawNum(x, y+10, cnt_frags[i], -1);

	y += WI_SPACINGY;
    }

}

static int	sp_state;

void WI_initStats(void)
{
    state = StatCount;
    acceleratestage = 0;
    sp_state = 1;
    cnt_kills[0] = cnt_items[0] = cnt_secret[0] = -1;
    cnt_time = cnt_par = -1;
    cnt_pause = TICRATE;

    WI_initAnimatedBack();
}

void WI_updateStats(void)
{

    WI_updateAnimatedBack();

    if (acceleratestage && sp_state != 10)
    {
	acceleratestage = 0;
	cnt_kills[0] = (plrs[me].skills * 100) / wbs.maxkills;
	cnt_items[0] = (plrs[me].sitems * 100) / wbs.maxitems;
	cnt_secret[0] = (plrs[me].ssecret * 100) / wbs.maxsecret;
	cnt_time = plrs[me].stime / TICRATE;
	cnt_par = wbs.partime / TICRATE;
	S_StartSound(0, sfx_barexp);
	sp_state = 10;
    }

    if (sp_state == 2)
    {
	cnt_kills[0] += 2;

	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	if (cnt_kills[0] >= (plrs[me].skills * 100) / wbs.maxkills)
	{
	    cnt_kills[0] = (plrs[me].skills * 100) / wbs.maxkills;
	    S_StartSound(0, sfx_barexp);
	    sp_state++;
	}
    }
    else if (sp_state == 4)
    {
	cnt_items[0] += 2;

	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	if (cnt_items[0] >= (plrs[me].sitems * 100) / wbs.maxitems)
	{
	    cnt_items[0] = (plrs[me].sitems * 100) / wbs.maxitems;
	    S_StartSound(0, sfx_barexp);
	    sp_state++;
	}
    }
    else if (sp_state == 6)
    {
	cnt_secret[0] += 2;

	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	if (cnt_secret[0] >= (plrs[me].ssecret * 100) / wbs.maxsecret)
	{
	    cnt_secret[0] = (plrs[me].ssecret * 100) / wbs.maxsecret;
	    S_StartSound(0, sfx_barexp);
	    sp_state++;
	}
    }

    else if (sp_state == 8)
    {
	if (!(bcnt&3))
	    S_StartSound(0, sfx_pistol);

	cnt_time += 3;

	if (cnt_time >= plrs[me].stime / TICRATE)
	    cnt_time = plrs[me].stime / TICRATE;

	cnt_par += 3;

	if (cnt_par >= wbs.partime / TICRATE)
	{
	    cnt_par = wbs.partime / TICRATE;

	    if (cnt_time >= plrs[me].stime / TICRATE)
	    {
		S_StartSound(0, sfx_barexp);
		sp_state++;
	    }
	}
    }
    else if (sp_state == 10)
    {
	if (acceleratestage)
	{
	    S_StartSound(0, sfx_sgcock);

	    if (gamemode == commercial)
		WI_initNoState();
	    else
		WI_initShowNextLoc();
	}
    }
    else if (sp_state & 1)
    {
	if (!--cnt_pause)
	{
	    sp_state++;
	    cnt_pause = TICRATE;
	}
    }

}

/*
public void drawStats()
{
    // line height
    int lh;	

    lh = (3*SHORT(num[0].height))/2;

    WI_slamBackground();

    // draw animated background
    WI_drawAnimatedBack();
    
    WI_drawLF();

    V_DrawPatch(SP_STATSX, SP_STATSY, FB, kills);
    WI_drawPercent(SCREENWIDTH - SP_STATSX, SP_STATSY, cnt_kills[0]);

    V_DrawPatch(SP_STATSX, SP_STATSY+lh, FB, items);
    WI_drawPercent(SCREENWIDTH - SP_STATSX, SP_STATSY+lh, cnt_items[0]);

    V_DrawPatch(SP_STATSX, SP_STATSY+2*lh, FB, sp_secret);
    WI_drawPercent(SCREENWIDTH - SP_STATSX, SP_STATSY+2*lh, cnt_secret[0]);

    V_DrawPatch(SP_TIMEX, SP_TIMEY, FB, time);
    WI_drawTime(SCREENWIDTH/2 - SP_TIMEX, SP_TIMEY, cnt_time);

    if (wbs.epsd < 3)
    {
	V_DrawPatch(SCREENWIDTH/2 + SP_TIMEX, SP_TIMEY, FB, par);
	WI_drawTime(SCREENWIDTH - SP_TIMEX, SP_TIMEY, cnt_par);
    }

}
*/
/*

void WI_checkForAccelerate(void)
{
    int   i;
    player_t  *player;

    // check for button presses to skip delays
    for (i=0, player = players ; i<MAXPLAYERS ; i++, player++)
    {
	if (playeringame[i])
	{
	    if (player.cmd.buttons & BT_ATTACK)
	    {
		if (!player.attackdown)
		    acceleratestage = 1;
		player.attackdown = true;
	    }
	    else
		player.attackdown = false;
	    if (player.cmd.buttons & BT_USE)
	    {
		if (!player.usedown)
		    acceleratestage = 1;
		player.usedown = true;
	    }
	    else
		player.usedown = false;
	}
    }
}
*/

/*

// Updates stuff each tick
public void WI_Ticker()
{
    // counter for general background animation
    bcnt++;  

    if (bcnt == 1)
    {
	// intermission music
  	if ( gamemode == commercial )
	  S_ChangeMusic(mus_dm2int, true);
	else
	  S_ChangeMusic(mus_inter, true); 
    }

    WI_checkForAccelerate();

    switch (state)
    {
      case StatCount:
	if (deathmatch) WI_updateDeathmatchStats();
	else if (netgame) WI_updateNetgameStats();
	else WI_updateStats();
	break;
	
      case ShowNextLoc:
	WI_updateShowNextLoc();
	break;
	
      case NoState:
	WI_updateNoState();
	break;
    }

}
*/

public void loadData()
{
    int		i;
    int		j;
    String	name;
    anim_t	a;

    if (DC.DS.gamemode == GameMode_t.commercial)
	name= "INTERPIC";
    else 
	//sprintf(name, "WIMAP%d", wbs.epsd);
        name=("WIMAP"+Integer.toString(wbs.epsd));
    
    if ( DC.DS.gamemode == GameMode_t.retail )
    {
      if (wbs.epsd == 3)
          name= "INTERPIC";
    }

    // background
    bg = (patch_t) DC.W.CacheLumpName(name, PU_CACHE,patch_t.class);    
    DC.V.DrawPatch(0, 0, 1, bg);


    // UNUSED unsigned char *pic = screens[1];
    // if (gamemode == commercial)
    // {
    // darken the background image
    // while (pic != screens[1] + SCREENHEIGHT*SCREENWIDTH)
    // {
    //   *pic = colormaps[256*25 + *pic];
    //   pic++;
    // }
    //}

    if (DC.DS.gamemode == GameMode_t.commercial)
    {
	NUMCMAPS = 32;								
//	lnames = (patch_t **) Z_Malloc(sizeof(patch_t*) * NUMCMAPS,
//				       PU_STATIC, 0);
	
	lnames=new patch_t[NUMCMAPS];
    PrintfFormat xxx=new PrintfFormat("CWILV%2.2d");
    String buffer;
	for (i=0 ; i<NUMCMAPS ; i++)
	{								
	    name=xxx.sprintf(i);
	    lnames[i] = (patch_t) DC.W.CacheLumpName(name, PU_STATIC,patch_t.class);
	}					
    }
    else
    {
	lnames = new patch_t[NUMMAPS];
    PrintfFormat xxx=new PrintfFormat("WILV%d%d");

	for (i=0 ; i<NUMMAPS ; i++)
	{
	    name=xxx.sprintf(new Object[]{wbs.epsd, i});
	    lnames[i] = (patch_t) DC.W.CacheLumpName(name, PU_STATIC,patch_t.class);
	}

	// you are here
	yah[0] = (patch_t) DC.W.CacheLumpName("WIURH0", PU_STATIC,patch_t.class);

	// you are here (alt.)
	yah[1] = (patch_t) DC.W.CacheLumpName("WIURH1", PU_STATIC,patch_t.class);

	// splat
	splat = (patch_t) DC.W.CacheLumpName("WISPLAT", PU_STATIC,patch_t.class); 
	
	if (wbs.epsd < 3)
	{
	    xxx=new PrintfFormat("WIA%d%.2d%.2d");
	    for (j=0;j<NUMANIMS[wbs.epsd];j++)
	    {
		a = anims[wbs.epsd][j];
		for (i=0;i<a.nanims;i++)
		{
		    // MONDO HACK!
		    if (wbs.epsd != 1 || j != 8) 
		    {
			// animations
			name=xxx.sprintf(new Object[] {wbs.epsd, j, i});  
			a.p[i] = (patch_t)DC.W.CacheLumpName(name, PU_STATIC,patch_t.class);
		    }
		    else
		    {
			// HACK ALERT!
			a.p[i] = anims[1][4].p[i]; 
		    }
		}
	    }
	}
    }

    // More hacks on minus sign.
    wiminus = (patch_t)DC.W.CacheLumpName("WIMINUS", PU_STATIC,patch_t.class); 

    PrintfFormat xxx=new PrintfFormat("WINUM%d");
    for (i=0;i<10;i++)
    {
	 // numbers 0-9
	name=xxx.sprintf(i);     
	num[i] = (patch_t)DC.W.CacheLumpName(name, PU_STATIC,patch_t.class);
    }

    // percent sign
    percent = (patch_t)DC.W.CacheLumpName("WIPCNT", PU_STATIC,patch_t.class);

    // "finished"
    finished = (patch_t)DC.W.CacheLumpName("WIF", PU_STATIC,patch_t.class);

    // "entering"
    entering = (patch_t)DC.W.CacheLumpName("WIENTER", PU_STATIC,patch_t.class);

    // "kills"
    kills = (patch_t)DC.W.CacheLumpName("WIOSTK", PU_STATIC,patch_t.class);   

    // "scrt"
    secret = (patch_t)DC.W.CacheLumpName("WIOSTS", PU_STATIC,patch_t.class);

     // "secret"
    sp_secret =(patch_t)DC.W.CacheLumpName("WISCRT2", PU_STATIC,patch_t.class);

    // Yuck. 
    if (DC.DS.language==Language_t.french)
    {
	// "items"
	if (DC.DS.netgame && !DC.DS.deathmatch)
	    items = (patch_t)DC.W.CacheLumpName("WIOBJ", PU_STATIC,patch_t.class);    
  	else
	    items = (patch_t)DC.W.CacheLumpName("WIOSTI", PU_STATIC,patch_t.class);
    } else
	items = (patch_t)DC.W.CacheLumpName("WIOSTI", PU_STATIC,patch_t.class);

    // "frgs"
    frags = (patch_t)DC.W.CacheLumpName("WIFRGS", PU_STATIC,patch_t.class);    

    // ":"
    colon = (patch_t)DC.W.CacheLumpName("WICOLON", PU_STATIC,patch_t.class); 

    // "time"
    time = (patch_t)DC.W.CacheLumpName("WITIME", PU_STATIC,patch_t.class);  

    // "sucks"
    sucks = (patch_t)DC.W.CacheLumpName("WISUCKS", PU_STATIC,patch_t.class);  

    // "par"
    par = (patch_t)DC.W.CacheLumpName("WIPAR", PU_STATIC,patch_t.class);   

    // "killers" (vertical)
    killers = (patch_t)DC.W.CacheLumpName("WIKILRS", PU_STATIC,patch_t.class);

    // "victims" (horiz)
    victims = (patch_t)DC.W.CacheLumpName("WIVCTMS", PU_STATIC,patch_t.class);

    // "total"
    total = (patch_t)DC.W.CacheLumpName("WIMSTT", PU_STATIC,patch_t.class);   

    // your face
    star = (patch_t)DC.W.CacheLumpName("STFST01", PU_STATIC,patch_t.class);

    // dead face
    bstar = (patch_t)DC.W.CacheLumpName("STFDEAD0", PU_STATIC,patch_t.class);    

    PrintfFormat xx1=new PrintfFormat("STPB%d");
    PrintfFormat xx2=new PrintfFormat("WIBP%d");
    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	// "1,2,3,4"
	name= xx1.sprintf(i);      
	p[i] = (patch_t)DC.W.CacheLumpName(name, PU_STATIC,patch_t.class);;

	// "1,2,3,4"

	name=xx2.sprintf(i+1);     
	bp[i] = (patch_t)DC.W.CacheLumpName(name, PU_STATIC,patch_t.class);;
    }

}

/*

public void WI_unloadData()
{
    int		i;
    int		j;

    Z_ChangeTag(wiminus, PU_CACHE);

    for (i=0 ; i<10 ; i++)
	Z_ChangeTag(num[i], PU_CACHE);
    
    if (gamemode == commercial)
    {
  	for (i=0 ; i<NUMCMAPS ; i++)
	    Z_ChangeTag(lnames[i], PU_CACHE);
    }
    else
    {
	Z_ChangeTag(yah[0], PU_CACHE);
	Z_ChangeTag(yah[1], PU_CACHE);

	Z_ChangeTag(splat, PU_CACHE);

	for (i=0 ; i<NUMMAPS ; i++)
	    Z_ChangeTag(lnames[i], PU_CACHE);
	
	if (wbs.epsd < 3)
	{
	    for (j=0;j<NUMANIMS[wbs.epsd];j++)
	    {
		if (wbs.epsd != 1 || j != 8)
		    for (i=0;i<anims[wbs.epsd][j].nanims;i++)
			Z_ChangeTag(anims[wbs.epsd][j].p[i], PU_CACHE);
	    }
	}
    }
    
    Z_Free(lnames);

    Z_ChangeTag(percent, PU_CACHE);
    Z_ChangeTag(colon, PU_CACHE);
    Z_ChangeTag(finished, PU_CACHE);
    Z_ChangeTag(entering, PU_CACHE);
    Z_ChangeTag(kills, PU_CACHE);
    Z_ChangeTag(secret, PU_CACHE);
    Z_ChangeTag(sp_secret, PU_CACHE);
    Z_ChangeTag(items, PU_CACHE);
    Z_ChangeTag(frags, PU_CACHE);
    Z_ChangeTag(time, PU_CACHE);
    Z_ChangeTag(sucks, PU_CACHE);
    Z_ChangeTag(par, PU_CACHE);

    Z_ChangeTag(victims, PU_CACHE);
    Z_ChangeTag(killers, PU_CACHE);
    Z_ChangeTag(total, PU_CACHE);
    //  Z_ChangeTag(star, PU_CACHE);
    //  Z_ChangeTag(bstar, PU_CACHE);
    
    for (i=0 ; i<MAXPLAYERS ; i++)
	Z_ChangeTag(p[i], PU_CACHE);

    for (i=0 ; i<MAXPLAYERS ; i++)
	Z_ChangeTag(bp[i], PU_CACHE);
}

void WI_Drawer (void)
{
    switch (state)
    {
      case StatCount:
	if (deathmatch)
	    WI_drawDeathmatchStats();
	else if (netgame)
	    WI_drawNetgameStats();
	else
	    WI_drawStats();
	break;
	
      case ShowNextLoc:
	WI_drawShowNextLoc();
	break;
	
      case NoState:
	WI_drawNoState();
	break;
    }
}


void WI_initVariables(wbstartstruct_t* wbstartstruct)
{

    wbs = wbstartstruct;

#ifdef RANGECHECKING
    if (gamemode != commercial)
    {
      if ( gamemode == retail )
	RNGCHECK(wbs.epsd, 0, 3);
      else
	RNGCHECK(wbs.epsd, 0, 2);
    }
    else
    {
	RNGCHECK(wbs.last, 0, 8);
	RNGCHECK(wbs.next, 0, 8);
    }
    RNGCHECK(wbs.pnum, 0, MAXPLAYERS);
    RNGCHECK(wbs.pnum, 0, MAXPLAYERS);
#endif

    acceleratestage = 0;
    cnt = bcnt = 0;
    firstrefresh = 1;
    me = wbs.pnum;
    plrs = wbs.plyr;

    if (!wbs.maxkills)
	wbs.maxkills = 1;

    if (!wbs.maxitems)
	wbs.maxitems = 1;

    if (!wbs.maxsecret)
	wbs.maxsecret = 1;

    if ( gamemode != retail )
      if (wbs.epsd > 2)
	wbs.epsd -= 3;
}

public void Start(wbstartstruct_t wbstartstruct)
{

    initVariables(wbstartstruct);
    loadData();

    if (DC.DS.deathmatch)
	initDeathmatchStats();
    else if (DC.DS.netgame)
	initNetgameStats();
    else
	initStats();
}*/
}