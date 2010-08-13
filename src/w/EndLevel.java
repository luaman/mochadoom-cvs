package w;

/* Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: EndLevel.java,v 1.2 2010/08/13 14:06:36 velktron Exp $
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
// Revision 1.2  2010/08/13 14:06:36  velktron
// Endlevel screen fully functional!
//
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
import data.doomstat;
import data.Defines.GameMode_t;
import data.sounds.sfxenum_t;
import doom.DoomContext;
import doom.event_t;
import doom.player_t;
import doom.wbplayerstruct_t;
import doom.wbstartstruct_t;
import m.random;
import rr.*;
import utils.C2JUtils;
import utils.PrintfFormat;
import v.DoomVideoRenderer;

/** This class (stuff.c) seems to implement the endlevel screens.
 *  If we want to go truly OO, it should be made non-.
 * 
 * @author Maes
 *
 */

public class EndLevel{

    ////////////////// STATUS ///////////////////
    private doomstat DS;
    private DoomVideoRenderer V;
    private WadLoader W;
    private random RND;
    
    private static int COUNT_KILLS=2;
    private static int COUNT_ITEMS=4;
    private static int COUNT_SECRETS=6;
    private static int COUNT_TIME=8;
    private static int COUNT_DONE=10;
    
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
public static final int FB= 0;
private static final boolean RANGECHECKING = true;


// States for single-player
protected static int SP_KILLS	=	0;
protected static int SP_ITEMS	=	2;
protected static int SP_SECRET	=	4;
protected static int SP_FRAGS	=	6 ;
protected static int SP_TIME		=	8 ;
protected static int SP_PAR		=	SP_TIME;

protected int SP_PAUSE	=	1;

// in seconds
protected  int SHOWNEXTLOCDELAY	=4;
protected  int SHOWLASTLOCDELAY	=SHOWNEXTLOCDELAY;


// used to accelerate or skip a stage
int		acceleratestage;

// wbs->pnum
int		me;

 // specifies current state )
int	state;

// contains information passed into intermission
public wbstartstruct_t	wbs;

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
int		cnt_par;
int		cnt_pause;

// # of commercial levels
int		NUMCMAPS; 


//
//	GRAPHICS
//

// background (map of levels).
patch_t		bg;

// You Are Here graphic
patch_t[]		yah=new patch_t[2]; 

// splat
patch_t		splat;

/** %, : graphics */
patch_t		percent,colon;

/** 0-9 graphic */
patch_t[]		num=new patch_t[10];

/** minus sign */
 patch_t	wiminus;

// "Finished!" graphics
 patch_t		finished;

// "Entering" graphic
 patch_t		entering; 

// "secret"
 patch_t		sp_secret;

 /** "Kills", "Scrt", "Items", "Frags" */
 patch_t		kills,	secret,items,frags;

/** Time sucks. */
 patch_t		time,	par,sucks;

/** "killers", "victims" */
 patch_t		killers,victims; 

/** "Total", your face, your dead face */
 patch_t		total,	star, bstar;

/** "red P[1..MAXPLAYERS]"*/
 patch_t[]		p=new patch_t[MAXPLAYERS];

/** "gray P[1..MAXPLAYERS]" */
 patch_t[] bp=new patch_t[MAXPLAYERS];

 /** Name graphics of each level (centered) */
 patch_t[]	lnames;

//
// CODE
//

// slam background
// UNUSED  unsigned char *background=0;


public EndLevel(DoomContext DC) {
    this.DS=DC.DS;
    this.V=DC.V;
    this.W=DC.W;
    this.RND=DC.RND;
    this.wbs=new wbstartstruct_t();
    this.Start(DS.wminfo);
}

public void slamBackground()
{
    //    memcpy(screens[0], screens[1], SCREENWIDTH * SCREENHEIGHT);
    // Remember, the second arg is the source!
    System.arraycopy(V.getScreen(1), 0 ,V.getScreen(0),0, SCREENWIDTH * SCREENHEIGHT);
    V.MarkRect (0, 0, SCREENWIDTH, SCREENHEIGHT);
}

// The ticker is used to detect keys
//  because of timing issues in netgames.
public boolean Responder(event_t ev)
{
    return false;
}


/** Draws "<Levelname> Finished!" */
protected void drawLF()
{
    int y = WI_TITLEY;

    // draw <LevelName> 
    V.DrawPatch((SCREENWIDTH - lnames[wbs.last].width)/2,
		y, FB, lnames[wbs.last]);

    // draw "Finished!"
    y += (5*lnames[wbs.last].height)/4;
    
    V.DrawPatch((SCREENWIDTH - finished.width)/2,
		y, FB, finished);
}



/** Draws "Entering <LevelName>" */
protected void drawEL()
{
    int y = WI_TITLEY;

    // draw "Entering"
    V.DrawPatch((SCREENWIDTH - entering.width)/2,
		y, FB, entering);

    // draw level
    y += (5*lnames[wbs.next].height)/4;

    V.DrawPatch((SCREENWIDTH - lnames[wbs.next].width)/2,
		y, FB, lnames[wbs.next]);

}

/** For whatever fucked-up reason, it expects c to be an array of patches, 
 *  and may choose to draw from alternative ones...which however are never loaded,
 *  or are supposed to be "next" in memory or whatever. I kept this behavior, however 
 *  in Java it will NOT work as intended, if ever.
 * 
 * @param n
 * @param c
 */

protected void
drawOnLnode
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
	V.DrawPatch(lnodes[wbs.epsd][n].x, lnodes[wbs.epsd][n].y,
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

    if (DS.gamemode == GameMode_t.commercial)
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
	    a.nexttic = bcnt + 1 + (RND.M_Random()%a.period);
	else if (a.type == animenum_t.ANIM_RANDOM)
	    a.nexttic = bcnt + 1 + a.data2+(RND.M_Random()%a.data1);
	else if (a.type == animenum_t.ANIM_LEVEL)
	    a.nexttic = bcnt + 1;
    }

}

public void updateAnimatedBack()
{
    int		i;
    anim_t	a;

    if (DS.gamemode == GameMode_t.commercial)
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
		if (++anims[aaptr][i].ctr >= a.nanims) a.ctr = 0;
		a.nexttic = bcnt + a.period;
		break;

	      case ANIM_RANDOM:
		a.ctr++;
		if (a.ctr == a.nanims)
		{
		    a.ctr = -1;
		    a.nexttic = bcnt+a.data2+(RND.M_Random()%a.data1);
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

protected void drawAnimatedBack()
{
    int			i;
    anim_t		a;

    if (DS.gamemode==GameMode_t.commercial)
	return;

    if (wbs.epsd > 2)
	return;

    for (i=0 ; i<NUMANIMS[wbs.epsd] ; i++)
    {
	a = anims[wbs.epsd][i];

	if (a.ctr >= 0)
	    V.DrawPatch(a.loc.x, a.loc.y, FB, a.p[a.ctr]);
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
	V.DrawPatch(x, y, FB, num[ n % 10 ]);
	n /= 10;
    }

    // draw a minus sign if necessary
    if (neg)
	V.DrawPatch(x-=8, y, FB, wiminus);

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

    V.DrawPatch(x, y, FB, percent);
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
		V.DrawPatch(x, y, FB, colon);
	    
	} while ((t / div)>0);
    }
    else
    {
	// "sucks"
	V.DrawPatch(x - sucks.width, y, FB, sucks); 
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


public void initShowNextLoc()
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

    if ( DS.gamemode != GameMode_t.commercial)
    {
  	if (wbs.epsd > 2)
	{
	    drawEL();
	    return;
	}
	
	last = (wbs.last == 8) ? wbs.next - 1 : wbs.last;

	// draw a splat on taken cities.
	for (i=0 ; i<=last ; i++)
	    drawOnLnode(i, new patch_t[]{splat});

	// splat the secret level?
	if (wbs.didsecret)
	    drawOnLnode(8, new patch_t[]{splat});

	// draw flashing ptr
	if (snl_pointeron)
	    drawOnLnode(wbs.next, yah); 
    }

    // draws which level you are entering..
    if ( (DS.gamemode != GameMode_t.commercial)
	 || wbs.next != 30)
	drawEL();  

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
	if (DS.playeringame[i]
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
	if (DS.playeringame[i])
	{
	    for (j=0 ; j<MAXPLAYERS ; j++)
		if (DS.playeringame[j])
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
	    if (DS.playeringame[i])
	    {
		for (j=0 ; j<MAXPLAYERS ; j++)
		    if (DS.playeringame[j])
			dm_frags[i][j] = plrs[i].frags[j];

		dm_totals[i] = fragSum(i);
	    }
	}
	

	// TODO:S_StartSound(0, sfx_barexp);
	dm_state = 4;
    }

    
    if (dm_state == 2)
    {
	if ((bcnt&3)==0);
	    // TODO: S_StartSound(0, sfx_pistol);
	
	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (DS.playeringame[i])
	    {
		for (j=0 ; j<MAXPLAYERS ; j++)
		{
		    if (DS.playeringame[j]
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
		dm_totals[i] = fragSum(i);

		if (dm_totals[i] > 99)
		    dm_totals[i] = 99;
		
		if (dm_totals[i] < -99)
		    dm_totals[i] = -99;
	    }
	    
	}
	if (!stillticking)
	{
	 // TODO:   S_StartSound(0, sfx_barexp);
	    dm_state++;
	}

    }
    else if (dm_state == 4)
    {
	if (acceleratestage!=0)
	{
	   // TODO: S_StartSound(0, sfx_slop);

	    if ( DS.gamemode == GameMode_t.commercial)
		initNoState();
	    else
		initShowNextLoc();
	}
    }
    else if ((dm_state & 1)!=0)
    {
	if (--cnt_pause==0)
	{
	    dm_state++;
	    cnt_pause = TICRATE;
	}
    }
}



public void drawDeathmatchStats()
{

    int		i;
    int		j;
    int		x;
    int		y;
    int		w;
    
    int		lh;	// line height

    lh = WI_SPACINGY;

    slamBackground();
    
    // draw animated background
    drawAnimatedBack(); 
    drawLF();

    // draw stat titles (top line)
    V.DrawPatch(DM_TOTALSX-total.width/2,
		DM_MATRIXY-WI_SPACINGY+10,
		FB,
		total);
    
    V.DrawPatch(DM_KILLERSX, DM_KILLERSY, FB, killers);
    V.DrawPatch(DM_VICTIMSX, DM_VICTIMSY, FB, victims);

    // draw P?
    x = DM_MATRIXX + DM_SPACINGX;
    y = DM_MATRIXY;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (DS.playeringame[i])
	{
	    V.DrawPatch(x-p[i].width/2,
			DM_MATRIXY - WI_SPACINGY,
			FB,
			p[i]);
	    
	    V.DrawPatch(DM_MATRIXX-p[i].width/2,
			y,
			FB,
			p[i]);

	    if (i == me)
	    {
		V.DrawPatch(x-p[i].width/2,
			    DM_MATRIXY - WI_SPACINGY,
			    FB,
			    bstar);

		V.DrawPatch(DM_MATRIXX-p[i].width/2,
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
    w = num[0].width;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	x = DM_MATRIXX + DM_SPACINGX;

	if (DS.playeringame[i])
	{
	    for (j=0 ; j<MAXPLAYERS ; j++)
	    {
		if (DS.playeringame[j])
		    drawNum(x+w, y, dm_frags[i][j], 2);

		x += DM_SPACINGX;
	    }
	    drawNum(DM_TOTALSX+w, y, dm_totals[i], 2);
	}
	y += WI_SPACINGY;
    }
}

 int[]	cnt_frags=new int[MAXPLAYERS];
 int	dofrags;
 int	ng_state;

public void initNetgameStats()
{

    int i;

    state = StatCount;
    acceleratestage = 0;
    ng_state = 1;

    cnt_pause = TICRATE;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (!DS.playeringame[i])
	    continue;

	cnt_kills[i] = cnt_items[i] = cnt_secret[i] = cnt_frags[i] = 0;

	dofrags += fragSum(i);
    }

    dofrags = ~~dofrags;

    initAnimatedBack();
}



public void updateNetgameStats()
{

    int		i;
    int		fsum;
    
    boolean	stillticking;

    updateAnimatedBack();

    if (acceleratestage!=0 && ng_state != 10)
    {
	acceleratestage = 0;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!DS.playeringame[i])
		continue;

	    cnt_kills[i] = (plrs[i].skills * 100) / wbs.maxkills;
	    cnt_items[i] = (plrs[i].sitems * 100) / wbs.maxitems;
	    cnt_secret[i] = (plrs[i].ssecret * 100) / wbs.maxsecret;

	    if (dofrags!=0)
		cnt_frags[i] = fragSum(i);
	}
	// TODO:S_StartSound(0, sfx_barexp);
	ng_state = 10;
    }

    if (ng_state == 2)
    {
	if ((bcnt&3)==0);
	    // TODO:S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!DS.playeringame[i])
		continue;

	    cnt_kills[i] += 2;

	    if (cnt_kills[i] >= (plrs[i].skills * 100) / wbs.maxkills)
		cnt_kills[i] = (plrs[i].skills * 100) / wbs.maxkills;
	    else
		stillticking = true;
	}
	
	if (!stillticking)
	{
	  //TODO:S_StartSound(0, sfx_barexp);
	    ng_state++;
	}
    }
    else if (ng_state == 4)
    {
	if ((bcnt&3)==0);
	  //TODO: S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!DS.playeringame[i])
		continue;

	    cnt_items[i] += 2;
	    if (cnt_items[i] >= (plrs[i].sitems * 100) / wbs.maxitems)
		cnt_items[i] = (plrs[i].sitems * 100) / wbs.maxitems;
	    else
		stillticking = true;
	}
	if (!stillticking)
	{
	  //TODO: S_StartSound(0, sfx_barexp);
	    ng_state++;
	}
    }
    else if (ng_state == 6)
    {
	if ((bcnt&3)==0);
	  //TODO: S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!DS.playeringame[i])
		continue;

	    cnt_secret[i] += 2;

	    if (cnt_secret[i] >= (plrs[i].ssecret * 100) / wbs.maxsecret)
		cnt_secret[i] = (plrs[i].ssecret * 100) / wbs.maxsecret;
	    else
		stillticking = true;
	}
	
	if (!stillticking)
	{
	    //TODO:S_StartSound(0, sfx_barexp);
	    ng_state += 1 + 2*~dofrags;
	}
    }
    else if (ng_state == 8)
    {
	if ((bcnt&3)==0);
	  //TODO:S_StartSound(0, sfx_pistol);

	stillticking = false;

	for (i=0 ; i<MAXPLAYERS ; i++)
	{
	    if (!DS.playeringame[i])
		continue;

	    cnt_frags[i] += 1;

	    if (cnt_frags[i] >= (fsum = fragSum(i)))
		cnt_frags[i] = fsum;
	    else
		stillticking = true;
	}
	
	if (!stillticking)
	{
	  //TODO:S_StartSound(0, sfx_pldeth);
	    ng_state++;
	}
    }
    else if (ng_state == 10)
    {
	if (acceleratestage!=0)
	{
	  //TODO: S_StartSound(0, sfx_sgcock);
	    if ( DS.gamemode == GameMode_t.commercial )
		initNoState();
	    else
		initShowNextLoc();
	}
    }
    else if ((ng_state & 1)!=0)
    {
	if (--cnt_pause==0)
	{
	    ng_state++;
	    cnt_pause = TICRATE;
	}
    }
}

public void drawNetgameStats()
{
    int		i;
    int		x;
    int		y;
    int		pwidth = percent.width;

    slamBackground();
    
    // draw animated background
    drawAnimatedBack(); 

    drawLF();

    // draw stat titles (top line)
    V.DrawPatch(NG_STATSX()+NG_SPACINGX-kills.width,
		NG_STATSY, FB, kills);

    V.DrawPatch(NG_STATSX()+2*NG_SPACINGX-items.width,
		NG_STATSY, FB, items);

    V.DrawPatch(NG_STATSX()+3*NG_SPACINGX-secret.width,
		NG_STATSY, FB, secret);
    
    if (dofrags!=0)
	V.DrawPatch(NG_STATSX()+4*NG_SPACINGX-frags.width,
		    NG_STATSY, FB, frags);

    // draw stats
    y = NG_STATSY + kills.height;

    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	if (!DS.playeringame[i])
	    continue;

	x = NG_STATSX();
	V.DrawPatch(x-p[i].width, y, FB, p[i]);

	if (i == me)
	    V.DrawPatch(x-p[i].width, y, FB, star);

	x += NG_SPACINGX;
	drawPercent(x-pwidth, y+10, cnt_kills[i]);	x += NG_SPACINGX;
	drawPercent(x-pwidth, y+10, cnt_items[i]);	x += NG_SPACINGX;
	drawPercent(x-pwidth, y+10, cnt_secret[i]);	x += NG_SPACINGX;

	if (dofrags!=0)
	    drawNum(x, y+10, cnt_frags[i], -1);

	y += WI_SPACINGY;
    }

}


 int	sp_state;

public void initStats()
{
    state = StatCount;
    acceleratestage = 0;
    sp_state = 1;
    cnt_kills[0] = cnt_items[0] = cnt_secret[0] = -1;
    cnt_time = cnt_par = -1;
    cnt_pause = TICRATE;

    initAnimatedBack();
}

public void updateStats()
{

    updateAnimatedBack();
    
    //System.out.println("SP_State "+sp_state);

    if ((acceleratestage !=0) && sp_state != COUNT_DONE)
    {
	acceleratestage = 0;
	cnt_kills[0] = (plrs[me].skills * 100) / wbs.maxkills;
	cnt_items[0] = (plrs[me].sitems * 100) / wbs.maxitems;
	cnt_secret[0] = (plrs[me].ssecret * 100) / wbs.maxsecret;
	cnt_time = plrs[me].stime / TICRATE;
	cnt_par = wbs.partime / TICRATE;
	// TODO:S_StartSound(0, sfx_barexp);
	sp_state = 10;
    }

    if (sp_state == COUNT_KILLS)
    {
	cnt_kills[0] += 2;

	if ((bcnt&3)==0);
	    //TODO: S_StartSound(0, sfx_pistol);

	if (cnt_kills[0] >= (plrs[me].skills * 100) / wbs.maxkills)
	{
	    cnt_kills[0] = (plrs[me].skills * 100) / wbs.maxkills;
	   // TODO: S_StartSound(0, sfx_barexp);
	    sp_state++;
	}
    }
    else if (sp_state == COUNT_ITEMS)
    {
	cnt_items[0] += 2;

	if ((bcnt&3)==0);
	    // TODO: S_StartSound(0, sfx_pistol);

	if (cnt_items[0] >= (plrs[me].sitems * 100) / wbs.maxitems)
	{
	    cnt_items[0] = (plrs[me].sitems * 100) / wbs.maxitems;
	    // TODO: S_StartSound(0, sfx_barexp);
	    sp_state++;
	}
    }
    else if (sp_state == COUNT_SECRETS)
    {
	cnt_secret[0] += 2;

	if ((bcnt&3)==0);
	    // TODO: S_StartSound(0, sfx_pistol);

	if (cnt_secret[0] >= (plrs[me].ssecret * 100) / wbs.maxsecret)
	{
	    cnt_secret[0] = (plrs[me].ssecret * 100) / wbs.maxsecret;
	    // TODO: S_StartSound(0, sfx_barexp);
	    sp_state++;
	}
    }

    else if (sp_state == COUNT_TIME)
    {
	if ((bcnt&3)==0);
	   // TODO: S_StartSound(0, sfx_pistol);

	cnt_time += 3;

	if (cnt_time >= plrs[me].stime / TICRATE)
	    cnt_time = plrs[me].stime / TICRATE;

	cnt_par += 3;

	if (cnt_par >= wbs.partime / TICRATE)
	{
	    cnt_par = wbs.partime / TICRATE;

	    if (cnt_time >= plrs[me].stime / TICRATE)
	    {
		// TODO: S_StartSound(0, sfxenum_t.sfx_barexp);
		sp_state++;
	    }
	}
    }
    else if (sp_state == COUNT_DONE)
    {
	if (acceleratestage!=0)
	{
	    // TODO: S_StartSound(0, sfxenum_t.sfx_sgcock);

	    if (DS.gamemode == GameMode_t.commercial)
		initNoState();
	    else
		initShowNextLoc();
	}
    }
    // Non-drawing, pausing state. Any odd value introduces a 35 tic pause.
    else if ((sp_state & 1)>0)
    {
	if (--cnt_pause==0)
	{
	    sp_state++;
	    cnt_pause = TICRATE;
	}
    }

}

public void drawStats()
{
    // line height
    int lh;	

    lh = (3*num[0].height)/2;

    slamBackground();

    // draw animated background
    drawAnimatedBack();
    
    drawLF();

    V.DrawPatch(SP_STATSX, SP_STATSY, FB, kills);
    drawPercent(SCREENWIDTH - SP_STATSX, SP_STATSY, cnt_kills[0]);

    V.DrawPatch(SP_STATSX, SP_STATSY+lh, FB, items);
    drawPercent(SCREENWIDTH - SP_STATSX, SP_STATSY+lh, cnt_items[0]);

    V.DrawPatch(SP_STATSX, SP_STATSY+2*lh, FB, sp_secret);
    drawPercent(SCREENWIDTH - SP_STATSX, SP_STATSY+2*lh, cnt_secret[0]);

    V.DrawPatch(SP_TIMEX, SP_TIMEY, FB, time);
    drawTime(SCREENWIDTH/2 - SP_TIMEX, SP_TIMEY, cnt_time);

    if (wbs.epsd < 3)
    {
	V.DrawPatch(SCREENWIDTH/2 + SP_TIMEX, SP_TIMEY, FB, par);
	drawTime(SCREENWIDTH - SP_TIMEX, SP_TIMEY, cnt_par);
    }

}

public void checkForAccelerate()
{

    // check for button presses to skip delays
    for (int i=0 ; i<MAXPLAYERS ; i++)
    {
        player_t player = DS.players[i];
	if (DS.playeringame[i])
	{
	    if ((player.cmd.buttons & BT_ATTACK)!=0)
	    {
		if (!player.attackdown)
		    acceleratestage = 1;
		player.attackdown = true;
	    }
	    else
		player.attackdown = false;
	    if ((player.cmd.buttons & BT_USE)!=0)
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



/** Updates stuff each tick */
public void Ticker()
{
    // counter for general background animation
    bcnt++;  

    if (bcnt == 1)
    {
	// intermission music
  	if ( DS.gamemode == GameMode_t.commercial )
  	    ;
  	//TODO: S_ChangeMusic(mus_dm2int, true);
	else
	    ;
	  //TODO: S_ChangeMusic(mus_inter, true); 
    }

    checkForAccelerate();
//System.out.println("State "+state);
    
    switch (state)
    {
      case StatCount:
	if (DS.deathmatch) updateDeathmatchStats();
	else if (DS.netgame) updateNetgameStats();
	else updateStats();
	break;
	
      case ShowNextLoc:
	updateShowNextLoc();
	break;
	
      case NoState:
	updateNoState();
	break;
    }

}

public void loadData()
{
    int		i;
    int		j;
    String	name;
    anim_t	a;

    if (DS.gamemode == GameMode_t.commercial)
	name= "INTERPIC";
    else 
	//sprintf(name, "WIMAP%d", wbs.epsd);
        name=("WIMAP"+Integer.toString(wbs.epsd));
    
    if ( DS.gamemode == GameMode_t.retail )
    {
      if (wbs.epsd == 3)
          name= "INTERPIC";
    }

    // background
    bg = (patch_t) W.CacheLumpName(name, PU_CACHE,patch_t.class);    
    V.DrawPatch(0, 0, 1, bg);


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

    if (DS.gamemode == GameMode_t.commercial)
    {
	NUMCMAPS = 32;								
//	lnames = (patch_t **) Z_Malloc(sizeof(patch_t*) * NUMCMAPS,
//				       PU_, 0);
	
	lnames=new patch_t[NUMCMAPS];
    PrintfFormat xxx=new PrintfFormat("CWILV%2.2d");
    String buffer;
	for (i=0 ; i<NUMCMAPS ; i++)
	{								
	    name=xxx.sprintf(i);
	    lnames[i] = (patch_t) W.CacheLumpName(name, PU_STATIC,patch_t.class);
	}					
    }
    else
    {
	lnames = new patch_t[NUMMAPS];
    PrintfFormat xxx=new PrintfFormat("WILV%d%d");

	for (i=0 ; i<NUMMAPS ; i++)
	{
	    name=xxx.sprintf(new Object[]{wbs.epsd, i});
	    lnames[i] = (patch_t) W.CacheLumpName(name, PU_STATIC,patch_t.class);
	}

	// you are here
	yah[0] = (patch_t) W.CacheLumpName("WIURH0", PU_STATIC,patch_t.class);

	// you are here (alt.)
	yah[1] = (patch_t) W.CacheLumpName("WIURH1", PU_STATIC,patch_t.class);

	// splat
	splat = (patch_t) W.CacheLumpName("WISPLAT", PU_STATIC,patch_t.class); 
	
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
			a.p[i] = (patch_t)W.CacheLumpName(name, PU_STATIC,patch_t.class);
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
    wiminus = (patch_t)W.CacheLumpName("WIMINUS", PU_STATIC,patch_t.class); 

    PrintfFormat xxx=new PrintfFormat("WINUM%d");
    for (i=0;i<10;i++)
    {
	 // numbers 0-9
	name=xxx.sprintf(i);     
	num[i] = (patch_t)W.CacheLumpName(name, PU_STATIC,patch_t.class);
    }

    // percent sign
    percent = (patch_t)W.CacheLumpName("WIPCNT", PU_STATIC,patch_t.class);

    // "finished"
    finished = (patch_t)W.CacheLumpName("WIF", PU_STATIC,patch_t.class);

    // "entering"
    entering = (patch_t)W.CacheLumpName("WIENTER", PU_STATIC,patch_t.class);

    // "kills"
    kills = (patch_t)W.CacheLumpName("WIOSTK", PU_STATIC,patch_t.class);   

    // "scrt"
    secret = (patch_t)W.CacheLumpName("WIOSTS", PU_STATIC,patch_t.class);

     // "secret"
    sp_secret =(patch_t)W.CacheLumpName("WISCRT2", PU_STATIC,patch_t.class);

    // Yuck. 
    if (DS.language==Language_t.french)
    {
	// "items"
	if (DS.netgame && !DS.deathmatch)
	    items = (patch_t)W.CacheLumpName("WIOBJ", PU_STATIC,patch_t.class);    
  	else
	    items = (patch_t)W.CacheLumpName("WIOSTI", PU_STATIC,patch_t.class);
    } else
	items = (patch_t)W.CacheLumpName("WIOSTI", PU_STATIC,patch_t.class);

    // "frgs"
    frags = (patch_t)W.CacheLumpName("WIFRGS", PU_STATIC,patch_t.class);    

    // ":"
    colon = (patch_t)W.CacheLumpName("WICOLON", PU_STATIC,patch_t.class); 

    // "time"
    time = (patch_t)W.CacheLumpName("WITIME", PU_STATIC,patch_t.class);  

    // "sucks"
    sucks = (patch_t)W.CacheLumpName("WISUCKS", PU_STATIC,patch_t.class);  

    // "par"
    par = (patch_t)W.CacheLumpName("WIPAR", PU_STATIC,patch_t.class);   

    // "killers" (vertical)
    killers = (patch_t)W.CacheLumpName("WIKILRS", PU_STATIC,patch_t.class);

    // "victims" (horiz)
    victims = (patch_t)W.CacheLumpName("WIVCTMS", PU_STATIC,patch_t.class);

    // "total"
    total = (patch_t)W.CacheLumpName("WIMSTT", PU_STATIC,patch_t.class);   

    // your face
    star = (patch_t)W.CacheLumpName("STFST01", PU_STATIC,patch_t.class);

    // dead face
    bstar = (patch_t)W.CacheLumpName("STFDEAD0", PU_STATIC,patch_t.class);    

    PrintfFormat xx1=new PrintfFormat("STPB%d");
    PrintfFormat xx2=new PrintfFormat("WIBP%d");
    for (i=0 ; i<MAXPLAYERS ; i++)
    {
	// "1,2,3,4"
	name= xx1.sprintf(i);      
	p[i] = (patch_t)W.CacheLumpName(name, PU_STATIC,patch_t.class);;

	// "1,2,3,4"

	name=xx2.sprintf(i+1);     
	bp[i] = (patch_t)W.CacheLumpName(name, PU_STATIC,patch_t.class);;
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
*/


public void Drawer ()
{
    switch (state)
    {
      case StatCount:
	if (DS.deathmatch)
	    drawDeathmatchStats();
	else if (DS.netgame)
	    drawNetgameStats();
	else
	    drawStats();
	break;
	
      case ShowNextLoc:
	drawShowNextLoc();
	break;
	
      case NoState:
	drawNoState();
	break;
    }
}


void initVariables(wbstartstruct_t wbstartstruct)
{

    wbs = wbstartstruct;

if (RANGECHECKING){
    if (DS.gamemode != GameMode_t.commercial)
    {
      if ( DS.gamemode == GameMode_t.retail )
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
}

    acceleratestage = 0;
    cnt = bcnt = 0;
    firstrefresh = 1;
    me = wbs.pnum;
    plrs = wbs.plyr.clone();

    if (wbs.maxkills==0)
	wbs.maxkills = 1;

    if (wbs.maxitems==0)
	wbs.maxitems = 1;

    if (wbs.maxsecret==0)
	wbs.maxsecret = 1;

    if ( DS.gamemode != GameMode_t.retail )
      if (wbs.epsd > 2)
	wbs.epsd -= 3;
}

public void Start(wbstartstruct_t wbstartstruct)
{

    initVariables(wbstartstruct);
    loadData();

    if (DS.deathmatch)
	initDeathmatchStats();
    else if (DS.netgame)
	initNetgameStats();
    else
	initStats();
}


public int NG_STATSX(){
    return 32 + star.width/2 + 32*~dofrags;
}

public static boolean RNGCHECK(int what, int min, int max){
    return (what>=min && what <=max)?true:false;
}

}