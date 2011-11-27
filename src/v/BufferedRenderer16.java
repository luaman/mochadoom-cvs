package v;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import m.BBox;

/* Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: BufferedRenderer16.java,v 1.2.2.3 2011/11/27 18:19:58 velktron Exp $
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
// $Log: BufferedRenderer16.java,v $
// Revision 1.2.2.3  2011/11/27 18:19:58  velktron
// Added cache clearing to keep memory down.
//
// Revision 1.2.2.2  2011/11/18 21:36:55  velktron
// More 16-bit goodness.
//
// Revision 1.2.2.1  2011/11/14 00:27:11  velktron
// A barely functional HiColor branch. Most stuff broken. DO NOT USE
//
// DESCRIPTION:
//	Gamma correction LUT stuff.
//	Functions to draw patches (by post) directly to screen.
//	Functions to blit a block to the screen.
//
//-----------------------------------------------------------------------------
 * 
 */

public class BufferedRenderer16 extends SoftwareVideoRenderer16 {
	
static final String rcsid = "$Id: BufferedRenderer16.java,v 1.2.2.3 2011/11/27 18:19:58 velktron Exp $";

/** Buffered Renderer has a bunch of images "pegged" to the underlying arrays */

public BufferedImage[] screenbuffer=new BufferedImage[5];

public BufferedRenderer16(int w, int h) {
    super(w,h);
}

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

/** This implementation will "tie" a bufferedimage to the underlying byte raster.
 * 
 * NOTE: this relies on the ability to "tap" into a BufferedImage's backing array,
 * in order to have fast writes without setpixel/getpixel. If that is not possible,
 * then we'll need to use a special renderer.
 * 
 */
@Override
public final void setScreen(int index, int width, int height){

	if (screens[index]==null){
		screenbuffer[index]=new BufferedImage(width,height,BufferedImage.TYPE_USHORT_555_RGB);
    
    	screens[index]=((DataBufferUShort)screenbuffer[index].getRaster().getDataBuffer()).getData();
		}
}

public void setPalette(int palette){
	//this.currentpal=palette%maxpalettes;
	this.currentscreen=this.screenbuffer[0];
	
}

@Override
public void setUsegamma(int gamma) {
	//this.usegamma=gamma%maxgammas;
	// Changing gamma also "fixes" the screens!
	this.setCurrentScreen(0);
}

public void setCurrentScreen(int screen){
	  super.setCurrentScreen(screen);
	  this.currentscreen=this.screenbuffer[0];
}

@Override
public void createPalettes(byte[] paldata, short[][] gammadata, int palettes,
		int colors, int stride, int gammalevels) {
	// TODO Auto-generated method stub
	
}

public int getBaseColor(int color){
	return colormaps[0][color];
}

}
