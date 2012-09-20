package v;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;

import m.BBox;

/* Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: BufferedRenderer.java,v 1.17.2.2 2012/09/20 14:19:33 velktron Exp $
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
// $Log: BufferedRenderer.java,v $
// Revision 1.17.2.2  2012/09/20 14:19:33  velktron
// The good old 8-bit renderers are back!
//
// Revision 1.17  2011/10/23 20:40:44  velktron
// Added palette failsafe to all renderers, using shared code wtih SoftwareVideoRenderer.
//
// Revision 1.16  2011/10/23 19:18:52  velktron
// Major refactoring of raster & palette tricks. Much less cluttered, fixed some old fuckups.
//
// Revision 1.15  2011/10/23 16:42:55  velktron
// Minor rewriting of raster mangling code
//
// Revision 1.14  2011/10/11 13:22:58  velktron
// Now handles palette and gamma construction internally, as per new interface. Got rid of some truecolour remnants.
//
// Revision 1.13  2011/06/08 17:24:42  velktron
// Possible to set gamma values and request new buffered images during runtime.
//
// Revision 1.12  2011/05/31 12:24:52  velktron
// Fixed bogus +1 screen allocation.
//
// Revision 1.11  2010/11/15 17:15:54  velktron
// Fixed masked columns rendering, introduced unrolled span and column functions from Boom (thanks, Lee Killough :-)
//
// Revision 1.10  2010/11/11 15:31:28  velktron
// Fixed "warped floor" error.
//
// Revision 1.9  2010/09/25 17:37:13  velktron
// Lots of changes.
//
// The most important is the creation of the AWTDoom frame handling I/O.
//
// Revision 1.8  2010/09/24 17:58:39  velktron
// Menus and HU  functional -mostly.
//
// Revision 1.7  2010/09/13 15:39:17  velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.6  2010/09/10 17:35:49  velktron
// DoomGame, Menu, renderers
//
// Revision 1.5  2010/09/09 16:09:09  velktron
// Yer more enhancements to the display system...
//
// Revision 1.4  2010/09/09 01:13:19  velktron
// MUCH better rendering and testers.
//
// Revision 1.3  2010/09/06 16:02:59  velktron
// Implementation of palettes.
//
// Revision 1.2  2010/09/06 10:23:24  velktron
// Alternative rendering method
//
// Revision 1.1  2010/08/27 23:46:57  velktron
// Introduced Buffered renderer, which makes tapping directly into byte[] screen buffers mapped to BufferedImages possible.
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

public class BufferedRenderer extends SoftwareVideoRenderer {
	
static final String rcsid = "$Id: BufferedRenderer.java,v 1.17.2.2 2012/09/20 14:19:33 velktron Exp $";

/** Buffered Renderer has a bunch of images "pegged" to the underlying arrays */

private BufferedImage[] screenbuffer;

/** Indexed renderers keep separate color models for each colormap (intended as gamma levels) and 
 * palette levels */
private IndexColorModel[][] cmaps;

public BufferedRenderer(int w, int h, IndexColorModel icm) {
    super(w,h);
    this.setIcm(icm);
}

/** Normally, you only have the palettes available ONLY after you read the palette from disk.
 *  So use the super contructor, and then this when the palettes are available.
 *  
 * @param icm2
 */

public void setIcm(IndexColorModel icm2) {
    this.icm=icm2;
    
}

public BufferedRenderer(int w, int h) {
    super(w,h);
}

// Used only for internal status.
private IndexColorModel icm;

@Override
public final void Init () 
{ 
 int		i;
 for (i=0 ; i<4 ; i++){
	//screens[i] = new byte[this.getHeight()*this.getWidth()];
     this.setScreen(i, this.width, this.height);     
	}
     dirtybox=new BBox();
}

/** This actually creates a raster with a fixed underlying array, but NOT the images
 *  themselves. So it's possible to have "imageless" rasters (unless you specifically
 *  request to make them visible, of course).
 *  
 */

@Override
public final void setScreen(int index, int width, int height){
	
    // We must FIRST initialize the image, so that the (immutable) color model will be set.
    if (this.icm==null){
    	final byte[] dummy=new byte[256];
    	for (int i=0;i<dummy.length;i++)
    		dummy[i]=(byte) i;    	
    	icm=new IndexColorModel(8,256,dummy,dummy,dummy);
    }
    
    r[index]=icm.createCompatibleWritableRaster(width,height);
    
    // Only create non-visible data, pegged to the raster. Create visible images
    // only on-demand.
    
    screens[index]=((DataBufferByte)r[index].getDataBuffer()).getData();

}


/** We only call this once we have a stable WritableRaster, and we only want
 *  a different colormodel (e.g. after changing gamma). It's slower than keepings
 *  severerl BufferedImages ready, so it's only used when changing gamma. The
 *  backing screen, array etc. should not have changed at this moment.
 * 
 * @param index
 * @param r
 */

private final BufferedImage createScreen(int index,IndexColorModel icm, WritableRaster r){
    return new BufferedImage(icm,r,false,null);
}

/*
public BufferedImage mapBufferedImageToScreen(int screen, IndexColorModel icm){
    // Map databuffer to one of the screens.
    DataBufferByte dbb=new DataBufferByte(screens[screen],screens[screen].length);
    BufferedImage b=new BufferedImage(this.getWidth(),this.getHeight(),BufferedImage.TYPE_BYTE_INDEXED,icm);
    WritableRaster r=WritableRaster.createPackedRaster(dbb,b.getWidth(), b.getHeight(), 8,
        new Point(0,0));
    b.setData(r);
    
    return b;
    
} */

/*
public BufferedImage cloneScreen(int screen, IndexColorModel icm){
    BufferedImage b=new BufferedImage(this.getWidth(),this.getHeight(),BufferedImage.TYPE_BYTE_INDEXED,icm);
    b.setData(screenbuffer[0].getRaster());
    return b;
    
} */




public final void changePalette(int pal){
    this.usepalette=(pal<<8);//+0x00FF;
    //this.usepalette=/*(pal<<8)+*/0xFF;
    
}

/** Get a bunch of BufferedImages "pegged" on the same output screen of this
 *  Doom Video Renderer, but with different palettes, defined in icms[].
 *  This is VERY speed efficient assuming that an IndexedColorModel will be used,
 *  rather than a 32-bit canvas, and memory overhead is minimal. Call this ONLY
 *  ONCE when initializing the video renderer, else it will invalidate pretty much
 *  everything in an ongoing game.
 * 
 *  NOTE: this will actually CREATE a new byte array for the screen, so it's important
 *  that this is called BEFORE anything else taps into it.
 * 
 * @param screen
 * @param icms
 * @return
 */

private BufferedImage[] createScreenForPalettes(int screen,IndexColorModel[] icms) {
        
		// These screens represent a complete range of palettes for a specific gamma
		// and specific screen
        BufferedImage[] b=new BufferedImage[icms.length];
        
        
        // MEGA hack: all images share the same raster data as screenbuffer[screen]
        // If this is the first time we called this method, the actually backing array
        // will be actually created. If not...

        // Create the first of the screens.
        this.icm=icms[0];
        
        if (r[screen]==null){
        	// This will create the first buffered image (and its data array)/
            // as screenbuffer[0]	   	
        	setScreen(screen,this.getWidth(),this.getHeight());
       		}
        
        // This is the base image for this set of palettes (usually index 0).
       
       
        // Create the rest of the screens (with different palettes) on the same raster.
        for (int i=0;i<icms.length;i++){
        	 b[i]=createScreen(screen,icms[i],r[screen]);
        }
        
        return b;
        
    }

protected final void specificPaletteCreation(byte[] paldata,
		short[][] gammadata, 
		final int palettes, 
		final int colors,
		final int stride,
		final int gammalevels){

	  System.out.printf("Enough data for %d palettes",maxpalettes);
	  System.out.printf("Enough data for %d gamma levels",maxgammas);
	  
	  // Create as gamma levels as specified.
	  cmaps=new IndexColorModel[maxgammas][];
	  
	  // First set of palettes, normal gamma.
	  cmaps[0]=new IndexColorModel[maxpalettes];

	  // Now we have our palettes.
	  for (int i=0;i<maxpalettes;i++){
		  cmaps[0][i]=new IndexColorModel(8, colors,paldata, i*stride*colors, false);
	  		}
  
  // Wire the others according to the gamma table.
	  byte[] tmpcmap=new byte[colors*stride];
	  
	  // For each gamma value...
	  for (int j=1;j<maxgammas;j++){
		  
		  cmaps[j]=new IndexColorModel[maxpalettes];
		  
		  // For each palette
		  for (int i=0;i<maxpalettes;i++){
			  
			  for (int k=1;k<256;k++){
				  tmpcmap[3*k]=(byte) gammadata[j][0x00FF&paldata[i*colors*stride+stride*k]]; // R
				  tmpcmap[3*k+1]=(byte) gammadata[j][0x00FF&paldata[1+i*colors*stride+stride*k]]; // G
				  tmpcmap[3*k+2]=(byte) gammadata[j][0x00FF&paldata[2+i*colors*stride+stride*k]]; // B
			  	}

			  cmaps[j][i]=new IndexColorModel(8, 256,tmpcmap, 0, false);
	  		}
	  }

}

private WritableRaster[] r=new WritableRaster[5];

public void setPalette(int palette){
	this.currentpal=palette%maxpalettes;
	this.currentscreen=this.screenbuffer[currentpal];
}

@Override
public void setUsegamma(int gamma) {
	this.usegamma=gamma%maxgammas;
	// Changing gamma also "fixes" the screens!
	this.setCurrentScreen(0);
}

public void setCurrentScreen(int screen){
	  super.setCurrentScreen(screen);
	  this.screenbuffer=this.createScreenForPalettes(usescreen, cmaps[usegamma]);
	  this.currentscreen=this.screenbuffer[currentpal];
}


}
