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
// $Id: SimpleRenderer.java,v 1.8 2010/08/23 14:36:08 velktron Exp $
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

public class SimpleRenderer implements DoomVideoRenderer{
	
static final String rcsid = "$Id: SimpleRenderer.java,v 1.8 2010/08/23 14:36:08 velktron Exp $";

private boolean RANGECHECK = true;
static byte[][] colbuf;
protected int CENTERY;//=			(SCREENHEIGHT/2);
protected int width;
protected int height;

// Each screen is [SCREENWIDTH*SCREENHEIGHT]; 
public byte[][]			screens=new byte[5][];	
 
//MAES: maybe this should be a bbox?

public BBox				dirtybox=new BBox(); 

public SimpleRenderer(){
    // Defaults
    width=SCREENWIDTH;
    height=SCREENHEIGHT;
    CENTERY=        (SCREENHEIGHT/2);
}

public SimpleRenderer(int w,int h){
    // Defaults
    width=w;
    height=h;
    CENTERY=        (h/2);
}

// Now where did these came from? /*[5][256]*/
public static short[][] gammatable =
{
    {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,
     17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,
     33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,
     49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,
     65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,
     81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,
     97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,
     113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,
     128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,
     144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,
     160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,
     176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,
     192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,
     208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,
     224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,
     240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255},

    {2,4,5,7,8,10,11,12,14,15,16,18,19,20,21,23,24,25,26,27,29,30,31,
     32,33,34,36,37,38,39,40,41,42,44,45,46,47,48,49,50,51,52,54,55,
     56,57,58,59,60,61,62,63,64,65,66,67,69,70,71,72,73,74,75,76,77,
     78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,
     99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,
     115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,129,
     130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,
     146,147,148,148,149,150,151,152,153,154,155,156,157,158,159,160,
     161,162,163,163,164,165,166,167,168,169,170,171,172,173,174,175,
     175,176,177,178,179,180,181,182,183,184,185,186,186,187,188,189,
     190,191,192,193,194,195,196,196,197,198,199,200,201,202,203,204,
     205,205,206,207,208,209,210,211,212,213,214,214,215,216,217,218,
     219,220,221,222,222,223,224,225,226,227,228,229,230,230,231,232,
     233,234,235,236,237,237,238,239,240,241,242,243,244,245,245,246,
     247,248,249,250,251,252,252,253,254,255},

    {4,7,9,11,13,15,17,19,21,22,24,26,27,29,30,32,33,35,36,38,39,40,42,
     43,45,46,47,48,50,51,52,54,55,56,57,59,60,61,62,63,65,66,67,68,69,
     70,72,73,74,75,76,77,78,79,80,82,83,84,85,86,87,88,89,90,91,92,93,
     94,95,96,97,98,100,101,102,103,104,105,106,107,108,109,110,111,112,
     113,114,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,
     129,130,131,132,133,133,134,135,136,137,138,139,140,141,142,143,144,
     144,145,146,147,148,149,150,151,152,153,153,154,155,156,157,158,159,
     160,160,161,162,163,164,165,166,166,167,168,169,170,171,172,172,173,
     174,175,176,177,178,178,179,180,181,182,183,183,184,185,186,187,188,
     188,189,190,191,192,193,193,194,195,196,197,197,198,199,200,201,201,
     202,203,204,205,206,206,207,208,209,210,210,211,212,213,213,214,215,
     216,217,217,218,219,220,221,221,222,223,224,224,225,226,227,228,228,
     229,230,231,231,232,233,234,235,235,236,237,238,238,239,240,241,241,
     242,243,244,244,245,246,247,247,248,249,250,251,251,252,253,254,254,
     255},

    {8,12,16,19,22,24,27,29,31,34,36,38,40,41,43,45,47,49,50,52,53,55,
     57,58,60,61,63,64,65,67,68,70,71,72,74,75,76,77,79,80,81,82,84,85,
     86,87,88,90,91,92,93,94,95,96,98,99,100,101,102,103,104,105,106,107,
     108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,
     125,126,127,128,129,130,131,132,133,134,135,135,136,137,138,139,140,
     141,142,143,143,144,145,146,147,148,149,150,150,151,152,153,154,155,
     155,156,157,158,159,160,160,161,162,163,164,165,165,166,167,168,169,
     169,170,171,172,173,173,174,175,176,176,177,178,179,180,180,181,182,
     183,183,184,185,186,186,187,188,189,189,190,191,192,192,193,194,195,
     195,196,197,197,198,199,200,200,201,202,202,203,204,205,205,206,207,
     207,208,209,210,210,211,212,212,213,214,214,215,216,216,217,218,219,
     219,220,221,221,222,223,223,224,225,225,226,227,227,228,229,229,230,
     231,231,232,233,233,234,235,235,236,237,237,238,238,239,240,240,241,
     242,242,243,244,244,245,246,246,247,247,248,249,249,250,251,251,252,
     253,253,254,254,255},

    {16,23,28,32,36,39,42,45,48,50,53,55,57,60,62,64,66,68,69,71,73,75,76,
     78,80,81,83,84,86,87,89,90,92,93,94,96,97,98,100,101,102,103,105,106,
     107,108,109,110,112,113,114,115,116,117,118,119,120,121,122,123,124,
     125,126,128,128,129,130,131,132,133,134,135,136,137,138,139,140,141,
     142,143,143,144,145,146,147,148,149,150,150,151,152,153,154,155,155,
     156,157,158,159,159,160,161,162,163,163,164,165,166,166,167,168,169,
     169,170,171,172,172,173,174,175,175,176,177,177,178,179,180,180,181,
     182,182,183,184,184,185,186,187,187,188,189,189,190,191,191,192,193,
     193,194,195,195,196,196,197,198,198,199,200,200,201,202,202,203,203,
     204,205,205,206,207,207,208,208,209,210,210,211,211,212,213,213,214,
     214,215,216,216,217,217,218,219,219,220,220,221,221,222,223,223,224,
     224,225,225,226,227,227,228,228,229,229,230,230,231,232,232,233,233,
     234,234,235,235,236,236,237,237,238,239,239,240,240,241,241,242,242,
     243,243,244,244,245,245,246,246,247,247,248,248,249,249,250,250,251,
     251,252,252,253,254,254,255,255}
};


public int	usegamma;
			 
//
// V_MarkRect 
// 

public int getUsegamma() {
	return usegamma;
}

public void setUsegamma(int usegamma) {
	this.usegamma = usegamma;
}

/** Apparently, video.c had its own "dirtybox" bbox, and this was a crude method for
 *  expanding it.
 * 
 */

@Override
public void MarkRect ( int		x,  int		y,  int		width,  int		height ) 
{ 
    dirtybox.AddToBox(x, y); 
    dirtybox.AddToBox(x+width-1, y+height-1); 
} 

@Override
public void Init () 
{ 
 int		i;
 //byte*	base;
		
 // stick these in low dos memory on PCs

 //base = I_AllocLow (this.width*SCREENHEIGHT*4);

 for (i=0 ; i<screens.length ; i++){
	screens[i] = new byte[this.getHeight()*this.getWidth()];
	}
     dirtybox=new BBox();
     
     colbuf=new byte[width][height];
}


//
//V_CopyRect 
//

@Override
public void CopyRect (int		srcx,
        int		srcy,
        int		srcscrn,
        int		width,
        int		height,
        int		destx,
        int		desty,
        int		destscrn ) 
{ 
    // These are pointers inside an array.
    byte[]	src=screens[srcscrn];
    byte[]	dest=screens[destscrn]; 

    if  (RANGECHECK) {
        if (srcx<0
                ||srcx+width >this.width
                || srcy<0
                || srcy+height>SCREENHEIGHT 
                ||destx<0||destx+width >this.width
                || desty<0
                || desty+height>SCREENHEIGHT 
                || srcscrn>4
                || destscrn>4)
        {
            system.Error ("Bad V_CopyRect");
        }
    } 
    this.MarkRect (destx, desty, width, height); 


    // MAES: these were pointers to a specific position inside the screen.
    int srcPos = this.width*srcy+srcx; 
    int destPos = this.width*desty+destx; 

    for ( ; height>0 ; height--) 
    { 
        System.arraycopy(src,srcPos, dest, destPos, width);
        //memcpy (dest, src, width); 
        srcPos += this.width; 
        destPos += this.width; 
    }

}
//
//V_DrawPatch
//Masks a column based masked pic to the screen. 
//

// desttop, dest and source were byte*

@Override
public void DrawPatch
( int		x,
int		y,
int		scrn,
patch_t	patch ) 
{ 

 column_t	column; 
 int	desttop;
 byte[]	dest=screens[scrn];
 int		w; 
	 
 y -= patch.topoffset; 
 x -= patch.leftoffset; 
if (RANGECHECK) 
 if (doRangeCheck(x,y,patch,scrn))
 {
   System.err.print("Patch at "+x+","+y+" exceeds LFB\n");
   // No I_Error abort - what is up with TNT.WAD?
   System.err.print("V_DrawPatch: bad patch (ignored)\n");
   return;
 }

 if (scrn==0)
	this.MarkRect (x, y, patch.width, patch.height); 

	 
 w = patch.width; 
 desttop = x+this.width*y; 
 int abspos=desttop;
 // For each column..
 int destPos;
 int ptr=0;
 for (int col=0 ; col<w ; desttop++, col++,x++)
 { 
	// This points at a "column" object.	 
	//column = (column_t *)((byte *)patch + LONG(patch->columnofs[col])); 
	column=patch.columns[col];
	// For each post...
	for (int i=0;i<column.posts;i++){
	    // Get pointer to post offset.
	    ptr=column.postofs[i];
	    // Get post delta
	    short delta=column.postdeltas[i];
	    // We skip delta, len and padding.
	    ptr+=3; 
	    
	    // Skip transparent rows...
	    if (delta==0xFF) break;

	    destPos = desttop + delta*this.width;  
	    
        // These lengths are already correct.
	    for (int j=0;j<column.postlen[i];j++){
	           dest[destPos] = column.data[ptr++];
	           destPos += this.width;
	    }
	}
 }
	
	
}

public void DrawPatchFast
( int       x,
int     y,
int     scrn,
patch_t patch ) 
{ 

 column_t   column; 
 int    desttop;
 byte[] dest=screens[scrn];
 int        w; 
     
 y -= patch.topoffset; 
 x -= patch.leftoffset; 
if (RANGECHECK) 
 if (doRangeCheck(x,y,patch,scrn))
 {
   System.err.print("Patch at "+x+","+y+" exceeds LFB\n");
   // No I_Error abort - what is up with TNT.WAD?
   System.err.print("V_DrawPatch: bad patch (ignored)\n");
   return;
 }

 if (scrn==0)
    this.MarkRect (x, y, patch.width, patch.height); 

     
 w = patch.width; 
 desttop = x+this.width*y; 
 int abspos=desttop;
 // For each column..
 int destPos;
 int ptr=0;
 for (int col=0 ; col<w ; desttop++, col++,x++)
 { 
    // This points at a "column" object.     
    //column = (column_t *)((byte *)patch + LONG(patch->columnofs[col])); 
    column=patch.columns[col];
    // For each post...
    for (int i=0;i<column.posts;i++){
        // Get pointer to post offset.
        ptr=column.postofs[i];
        // Get post delta
        short delta=column.postdeltas[i];
        // We skip delta, len and padding.
        ptr+=3; 
        
        // Skip transparent rows...
        if (delta==0xFF) break;

        //destPos = desttop + delta*this.width;
        //destPos = x*this.width+y + delta*this.width = (this.width+delta)  
        abspos=x*this.height+(y+delta);
        
        // These lengths are already correct.
        //System.arraycopy(column.data, ptr, dest,abspos, column.postlen[i]);
        /*for (int j=0;j<column.postlen[i];j++, destPos += this.width){
               dest[destPos] = column.data[ptr++];
        }*/

        
        for (int j=0;j<column.postlen[i];j++, abspos++){
               dest[abspos] = column.data[ptr+j];
        }


    }
 }
    
    
}   

public void DrawPatchFaster
( int       x,
int     y,
int     scrn,
patch_t patch ) 
{ 

 column_t   column; 
 int    desttop;
 byte[] dest=screens[scrn];
 int        w; 
     
 y -= patch.topoffset; 
 x -= patch.leftoffset; 
if (RANGECHECK) 
 if (doRangeCheck(x,y,patch,scrn))
 {
   System.err.print("Patch at "+x+","+y+" exceeds LFB\n");
   // No I_Error abort - what is up with TNT.WAD?
   System.err.print("V_DrawPatch: bad patch (ignored)\n");
   return;
 }

 if (scrn==0)
    this.MarkRect (x, y, patch.width, patch.height); 

     
 w = patch.width; 
 desttop = x+this.width*y; 
 int abspos=desttop;
 // For each column..
 int destPos;
 int ptr=0;
 for (int col=0 ; col<w ; desttop++, col++,x++)
 { 
    // This points at a "column" object.     
    //column = (column_t *)((byte *)patch + LONG(patch->columnofs[col])); 
    column=patch.columns[col];
    // For each post...
    for (int i=0;i<column.posts;i++){
        // Get pointer to post offset.
        ptr=column.postofs[i];
        // Get post delta
        short delta=column.postdeltas[i];
        // We skip delta, len and padding.
        ptr+=3; 
        
        // Skip transparent rows...
        if (delta==0xFF) break;

        //destPos = desttop + delta*this.width;
        //destPos = x*this.width+y + delta*this.width = (this.width+delta)  
        abspos=x*this.height+(y+delta);
        
        // These lengths are already correct.
        System.arraycopy(column.data, ptr, dest,abspos, column.postlen[i]);


    }
 }
 }

private boolean doRangeCheck(int x, int y,patch_t patch, int scrn){
  return      (x<0
                ||x+patch.width >this.width
                || y<0
                || y+patch.height>this.height 
                || scrn>4);
}

private boolean doRangeCheck(int x, int y, int scrn){
    return      (x<0
                  ||x>this.width
                  || y<0
                  || y>this.height 
                  || scrn>4);
  }

//
// V_DrawPatchFlipped 
// Masks a column based masked pic to the screen.
// Flips horizontally, e.g. to mirror face.
//

@Override
public void DrawPatchFlipped ( int		x,   int		y,    int		scrn,  patch_t	patch ) 
{ 

    column_t	column; 
    int	desttop;
    byte[]	dest=screens[scrn];
    int		w; 
	 
    y -= patch.topoffset; 
    x -= patch.leftoffset;
    

    if (RANGECHECK) 
        if (doRangeCheck(x,y,patch,scrn))
        {
    {
        System.err.print("Patch origin "+x+","+y +" exceeds LFB\n" );
        // No I_Error abort - what is up with TNT.WAD?
        system.Error("Bad V_DrawPatch in V_DrawPatchFlipped");
        }
    }
 
    if (scrn==0)
        this.MarkRect (x, y, patch.width, patch.height); 

 // Set x and y coords inside dest array.
  
      
    w = patch.width; 
    desttop = y*this.width+x;     
 // For each column..
    for (int col=0 ; col<w ; desttop++,col++)
    {         

        // This points at a "column" object.     
       // Notice the flipping on this one.
       // column = (column_t *)((byte *)patch + LONG(patch->columnofs[w-1-col]));  
       column=patch.columns[w-1-col];
       
       // For each post...
       System.out.println("Column"+(w-1-col));
       for (int i=0;i<column.posts;i++){
           // Get pointer to post offset.
           int ptr=column.postofs[i];
           // Get post delta
           int delta=C2JUtils.toUnsignedByte(column.data[ptr]);
           // We skip delta, len and padding.
           ptr+=3;
           if (delta==0xFF) break;
           int destPos = desttop+delta*this.width;
           //count = column.length; 

           // These lengths are already correct.
           for (int j=0;j<column.postlen[i];j++,   destPos += this.width){
                  dest[destPos] = column.data[ptr+j];
                  // next line
                
           }
       }
    }
    
} 
 

/*
//
// V_DrawPatchDirect
// Draws directly to the screen on the pc. 
//
void
V_DrawPatchDirect
( int		x,
  int		y,
  int		scrn,
  patch_t*	patch ) 
{
    V_DrawPatch (x,y,scrn, patch); 

//    MAES: This stuff was commented out.
 
    int		count;
    int		col; 
    column_t*	column; 
    byte*	desttop;
    byte*	dest;
    byte*	source; 
    int		w; 
	 
    y -= SHORT(patch->topoffset); 
    x -= SHORT(patch->leftoffset); 

#ifdef RANGECHECK 
    if (x<0
	||x+SHORT(patch->width) >this.width
	|| y<0
	|| y+SHORT(patch->height)>SCREENHEIGHT 
	|| (unsigned)scrn>4)
    {
	I_Error ("Bad V_DrawPatchDirect");
    }
#endif 
 
    //	V_MarkRect (x, y, SHORT(patch->width), SHORT(patch->height)); 
    desttop = destscreen + y*this.width/4 + (x>>2); 
	 
    w = SHORT(patch->width); 
    for ( col = 0 ; col<w ; col++) 
    { 
	outp (SC_INDEX+1,1<<(x&3)); 
	column = (column_t *)((byte *)patch + LONG(patch->columnofs[col])); 
 
	// step through the posts in a column 
	 
	while (column->topdelta != 0xff ) 
	{ 
	    source = (byte *)column + 3; 
	    dest = desttop + column->topdelta*this.width/4; 
	    count = column->length; 
 
	    while (count--) 
	    { 
		*dest = *source++; 
		dest += this.width/4; 
	    } 
	    column = (column_t *)(  (byte *)column + column->length 
				    + 4 ); 
	} 
	if ( ((++x)&3) == 0 ) 
	    desttop++;	// go to next byte, not next plane 
    } 
} 
*/


@Override
public void
DrawBlock
( int		x,
  int		y,
  int		scrn,
  int		width,
  int		height,
  byte[]		src ) 
{ 
    // This is "screens[scrn]"
    byte[]	dest=screens[scrn];  
	 
if (doRangeCheck(x, y, scrn))
    {
	system.Error("Bad V_DrawBlock");
    }

 
    this.MarkRect(x, y, width, height); 
 
    int destPos = /*screens[scrn] +*/ y*this.width +x;
    // MAES: making an assumption here. A BIIIIG one.
    int srcPos=0;
    while ((height--)>0) 
    { 
        //  memcpy (dest, src, width);
    System.arraycopy(src, srcPos, dest, destPos, width);
	srcPos += width; 
	destPos += this.width; 
    } 
} 

 

//
// V_GetBlock
// Gets a linear block of pixels from the view buffer.
//


public void
GetBlock
( int		x,
  int		y,
  int		scrn,
  int		width,
  int		height,
  byte[]		dest ) 
{ 
    byte[]	src=screens[scrn]; 
	 
if (RANGECHECK){
    if (doRangeCheck(x,y,scrn)){    
	system.Error ("Bad V_DrawBlock");
    }
    
 }
    int srcPos = y*this.width+x;
    int destPos=0;

    while ((height--)>0) 
    { 
	System.arraycopy(src, srcPos, dest, destPos, width);
	//memcpy (dest, src, width); 
	srcPos += width; 
	destPos += this.width; 
    } 
}

public int getHeight() {
    return this.height;
} 
public int getWidth() {
    return this.width;
} 

public void ColumnsFirstToRowsFirst(byte[] src, byte[] dest,int width,int height){
    for (int y=0;y<height;y++){
        //int tmp=x*height;
        int tmp2=y*width;
        for (int x=0;x<width;x++){    
            //System.arraycopy(src, y+x*height, dest, x+tmp2, 1);
            dest[x+tmp2]=src[y+x*height];
        }
    }
}

public void ColumnsFirstToRowsFirst2(byte[] src, byte[] dest,int width,int height){
    for (int x=0;x<width;x++){
      int tmp=x*height;
    for (int y=0;y<height;y++){
            //System.arraycopy(src, y+x*height, dest, x+tmp2, 1);
            dest[y+tmp]=src[x+y*width];
        }
    }
}

/** This has been EXTREEEEEEEEEEMELY optimized within Java's limits!!!
 *  Will only work correctly for square canvases though. It's still 
 *  about 50% slower than the normal renderer, so sometimes it *may*
 *  be worth using.
 * 
 * @param src
 * @param width
 * @param height
 */

public void InPlaceTranspose(byte[] src,int width,int height){
    int tmp2=0;
    int tmp;
    byte t;
    for (int y=0;y<height;y++){
        tmp=y;
        for (int x=0;x<width;x+=2){
            t=src[tmp2];
            src[tmp2]=src[tmp];
            src[tmp]=t;

            tmp+=height;
            tmp2++;
            
            t=src[tmp2];
            src[tmp2]=src[tmp];
            src[tmp]=t;

            tmp+=height;
            tmp2++;
        }
        
    }
}

public void Unscramble(int screen, byte[] dest){
    this.ColumnsFirstToRowsFirst2(screens[screen], dest, this.getWidth(),this.getHeight());

    }
public void Unscramble(int screen){
    this.InPlaceTranspose(screens[screen],this.getWidth(),this.getHeight());

    }


@Override
public void DrawPatchDirect(int x, int y, int scrn, patch_t patch) {
    this.DrawPatch(x, y, scrn, patch);
    
}

@Override
public byte[] getScreen(int index) {
   return screens[index];
}

@Override
public void takeScreenShot(int screen, String imagefile, IndexColorModel icm) throws IOException {
    
    BufferedImage b=new BufferedImage(this.getWidth(),this.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, icm);
    
    
    int[] tmp=new int[this.screens[screen].length];
    for (int i=0;i<this.screens[screen].length;i++){
        tmp[i]=this.screens[screen][i];
    }
    
    b.getRaster().setPixels(0, 0, this.getWidth(),this.getHeight(), tmp);
    
    File outputFile =
        new File(
            imagefile+".png");
    ImageIO.write(b, "PNG", outputFile);
    
}


}

