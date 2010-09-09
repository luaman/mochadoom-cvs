package v;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import rr.column_t;
import rr.patch_t;
import utils.C2JUtils;
import i.system;
import m.BBox;
import static data.Defines.*;

/* Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: SimpleRenderer.java,v 1.10 2010/09/09 16:09:09 velktron Exp $
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
// $Log: SimpleRenderer.java,v $
// Revision 1.10  2010/09/09 16:09:09  velktron
// Yer more enhancements to the display system...
//
// Revision 1.9  2010/08/25 15:16:07  velktron
// Status bar functional.
//
// Revision 1.8  2010/08/23 14:36:08  velktron
// Menu mostly working, implemented Killough's fast hash-based GetNumForName, although it can probably be finetuned even more.
//
// Revision 1.7  2010/08/13 14:06:36  velktron
// Endlevel screen fully functional!
//
// Revision 1.6  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
// Revision 1.5  2010/07/29 15:29:00  velktron
// More work on menus...and digging some dependencies..
//
// Revision 1.4  2010/07/06 16:32:38  velktron
// Threw some work in WI, now EndLevel. YEAH THERE'S GONNA BE A SEPARATE EndLevel OBJECT THAT'S HOW PIMP THE PROJECT IS!!!!11!!!
//
// Revision 1.3  2010/07/05 13:24:10  velktron
// Added a "fast transpose" algorithm that does outperform the naive one even in Java (thanks to http://www.boo.net/~jasonp/ for it!).
//
// However, even with that optimization, it barely beats drawing columns directly and then only by forcing the canvas to be square powers of 2 and at very high resolutions (>1024 pixels per size). Worth it? Dunno, I'll stick it in there.
//
// Revision 1.2  2010/07/03 23:24:13  velktron
// Added a LOT of stuff, like Status bar code & objects. Now we're cooking with gas!
//
// Revision 1.1  2010/07/02 14:26:16  velktron
// Now basic video rendering code should be fine (in SimpleRenderer)
// Defined DoomVideoSystem interface for SimpleRenderer.
//
// Revision 1.3  2010/07/01 18:38:09  velktron
// Video "rendering" completed, columns_t parsing completed. Play around with testers :-p
//
// Revision 1.2  2010/06/30 15:47:43  velktron
// Still working on column_t...
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
//	Gamma correction LUT stuff.
//	Functions to draw patches (by post) directly to screen.
//	Functions to blit a block to the screen.
//
//-----------------------------------------------------------------------------
 * 
 */


/*
#include "i_system.h"
#include "r_local.h"

#include "doomdef.h"
#include "doomdata.h"

#include "m_bbox.h"
#include "m_swap.h"

#include "v_video.h"
*/

public class SimpleRenderer extends SoftwareVideoRenderer {
	
static final String rcsid = "$Id: SimpleRenderer.java,v 1.10 2010/09/09 16:09:09 velktron Exp $";



public SimpleRenderer(){
super();
}

public SimpleRenderer(int w,int h){
    // Defaults
    super(w,h);
}

public void Init () 
{ 
 int		i;
 for (i=0 ; i<screens.length ; i++){
	screens[i] = new byte[this.getHeight()*this.getWidth()];
	}
     dirtybox=new BBox();
     
     colbuf=new byte[width][height];
}


}

