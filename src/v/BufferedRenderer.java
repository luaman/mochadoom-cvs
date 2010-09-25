package v;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import m.BBox;
import utils.C2JUtils;

/* Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: BufferedRenderer.java,v 1.9 2010/09/25 17:37:13 velktron Exp $
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
	
static final String rcsid = "$Id: BufferedRenderer.java,v 1.9 2010/09/25 17:37:13 velktron Exp $";

/** Buffered Renderer has a bunch of images "pegged" to the underlying arrays */

public BufferedImage[] screenbuffer=new BufferedImage[5];


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


private IndexColorModel icm;

private int usepalette=0xFF;

@Override
public final void Init () 
{ 
 int		i;
 for (i=0 ; i<4 ; i++){
	//screens[i] = new byte[this.getHeight()*this.getWidth()];
     this.setScreen(i, this.width, this.height);
     
	}
     dirtybox=new BBox();
     
     colbuf=new byte[width][height];
}

/** This implementation will "tie" a bufferedimage to the underlying byte raster.
 * 
 * NOTE: thie relies on the ability to "tap" into a BufferedImage's backing array,
 * in order to have fast writes without setpixel/getpixel. If that is not possible,
 * then we'll need to use a special renderer.
 * 
 */
@Override
public final void setScreen(int index, int width, int height){

    // We must FIRST initialize the image, so that the (immutable) color model will be set.
    
    if (this.icm==null)
    screenbuffer[index]=new BufferedImage(width,height,BufferedImage.TYPE_BYTE_INDEXED);
    else
        screenbuffer[index]=new BufferedImage(width,height,BufferedImage.TYPE_BYTE_INDEXED,this.icm);    
    
    // Hack: hotwire the screenbuffers directly to the image. T3h h4x, d00d.
    // Now, HERE is where the magic happens. Once a BufferedImage is created, the internal raster is
    // immutable, but its backing data array is accessible for read/write.
    // Ergo, we can now "hardwire" the vlb to the raster's backing array. Whatever we write in there,
    // will also appear in the image.
    
    screens[index]=((DataBufferByte)screenbuffer[index].getRaster().getDataBuffer()).getData();

}

public BufferedImage mapBufferedImageToScreen(int screen, IndexColorModel icm){
    // Map databuffer to one of the screens.
    DataBufferByte dbb=new DataBufferByte(screens[screen],screens[screen].length);
    BufferedImage b=new BufferedImage(this.getWidth(),this.getHeight(),BufferedImage.TYPE_BYTE_INDEXED,icm);
    WritableRaster r=WritableRaster.createPackedRaster(dbb,b.getWidth(), b.getHeight(), 8,
        new Point(0,0));
    b.setData(r);
    
    return b;
    
}

public BufferedImage cloneScreen(int screen, IndexColorModel icm){
    BufferedImage b=new BufferedImage(this.getWidth(),this.getHeight(),BufferedImage.TYPE_BYTE_INDEXED,icm);
    b.setData(screenbuffer[0].getRaster());
    return b;
    
}

/** Hotlinks a 32-bit "canvas" (the raster int[] array) to an externally supplied
 *  buffered image. Now whatever we write into raster, will appear in the image as well,
 *  without using drawing primitives. Necessary for fast rendering.
 *  
 * @param b
 */

public void mapInternalRasterToBufferedImage(BufferedImage b){
    raster=new int[this.screens[0].length];
    raster=((DataBufferInt)(b.getRaster().getDataBuffer())).getData();
    
}


/** Creates a 256 color int palette
 * 
 * @param pal palette lump from IWAD
 */

public void setPalette(byte[] pal){

    palette=new int[256*pal.length/768];
    
    
    
    for(int i = 0; i < pal.length/3; i++) {
        System.out.print(Integer.toHexString(0xFF&pal[i*3])+" ");
        System.out.print(Integer.toHexString(0xFF&pal[1+i*3])+" ");
        System.out.print(Integer.toHexString(0xFF&pal[2+i*3])+" ");
        
        int r=C2JUtils.toUnsignedByte(pal[i * 3]);
        int g=C2JUtils.toUnsignedByte(pal[1+i * 3]);
        int b=C2JUtils.toUnsignedByte(pal[2+i * 3]);
        
        palette[i] = (r<<16|g<<8|b);
        System.out.println(Integer.toHexString(0x00FFFFFF&palette[i]));
    }

    int[] tmp= new int[128];
    
    for(int i = 0; i < palette.length/256; i++) {
        // Swap signed/unsigned.
        System.arraycopy(palette, 128+256*i, tmp,0, 128);
        System.arraycopy(palette, 0+256*i, palette,128+256*i, 128);
        System.arraycopy(tmp, 0, palette,256*i, 128);
        
    }
    
}


/** Bullshit. creates a new int array and "sticks" it inside a BufferedImage,
 *  using a palette.
 *  
 * @param screen
 * @param b
 */
public final void toDevice(int screen, BufferedImage b)  {
    

    int[] tmp=new int[this.screens[screen].length];
    for (int i=0;i<this.screens[screen].length;i++){
        tmp[i]=palette[this.screens[screen][i]];
    }
    
    b.getRaster().setPixels(0, 0, this.getWidth(),this.getHeight(), tmp);
}

/** Update "canvas" to one of the internal screens.
 *  
 * @param screen
 * @param b
 */
public  final void remap(int screen)  {
    
    byte[] scr=this.screens[screen];
    //int i1=0,i2=0,i3=0,i4=0;
    for (int i=0;i<this.screens[screen].length;i+=16){

        raster[i]=palette[this.usepalette&scr[i]];
        raster[i+1]=palette[this.usepalette&scr[i+1]];
        raster[i+2]=palette[/*this.usepalette+*/0xFF&scr[i+2]];
        raster[i+3]=palette[/*this.usepalette+*/0xFF&scr[i+3]];
        raster[i+4]=palette[/*this.usepalette+*/0xFF&scr[i+4]];
        raster[i+5]=palette[/*this.usepalette+*/+0xFF&scr[i+5]];
        raster[i+6]=palette[/*this.usepalette+*/+0xFF&scr[i+6]];
        raster[i+7]=palette[/*this.usepalette+*/+0xFF&scr[i+7]];
        raster[i+8]=palette[/*this.usepalette+*/0xFF&scr[i+8]];
        raster[i+9]=palette[/*this.usepalette+*/0xFF&scr[i+9]];
        raster[i+10]=palette[/*this.usepalette+*/0xFF&scr[i+10]];
        raster[i+11]=palette[/*this.usepalette+*/+0xFF&scr[i+11]];
        raster[i+12]=palette[/*this.usepalette+*/+0xFF&scr[i+12]];
        raster[i+13]=palette[/*this.usepalette+*/+0xFF&scr[i+13]];
        raster[i+14]=palette[/*this.usepalette+*/0xFF&scr[i+14]];
        raster[i+15]=palette[/*this.usepalette+*/0xFF&scr[i+15]];

        /*
        raster[i]=palette[usepalette+scr[i]];
    raster[i1]=palette[usepalette+scr[i1]];
    raster[i2]=palette[usepalette+scr[i2]];
    raster[i3]=palette[usepalette+scr[i3]];
    raster[i4]=palette[usepalette+scr[i4]];
    raster[i+5]=palette[usepalette+scr[i+5]];
    raster[i+6]=palette[usepalette+scr[i+6]];
    raster[i+7]=palette[usepalette+scr[i+7]];
    /*
    raster[i+4]=palette[(short)0x00FF&scr[i+4]];
    raster[i+5]=palette[(short)0x00FF&scr[i+5]];
    raster[i+6]=palette[(short)0x00FF&scr[i+6]];
    raster[i+7]=palette[(short)0x00FF&scr[i+7]];*/

}
}

public final void changePalette(int pal){
    this.usepalette=(pal<<8);//+0x00FF;
    //this.usepalette=/*(pal<<8)+*/0xFF;
    
}

int[] palette;
int[] raster;

/** Get a bunch of BufferedImages "pegged" on the same output screen of this
 * Doom Video Renderer, but with a but with different palettes, defined in icms[]
 *  This is VERY speed efficient assuming that an IndexedColorModel will be used,
 *  rather than a 24-bit canvas, and memory overhead is minimal. 
 * 
 *  Only works with BufferedRenderer though.
 * 
 * @param screen
 * @param icms
 * @return
 */

public BufferedImage[] getBufferedScreens(int screen,IndexColorModel[] icms) {
        
        BufferedImage[] b=new BufferedImage[icms.length];
    // Map databuffer to one of the screens.

        // Create the first of the screens.
        this.icm=icms[screen];
        setScreen(0,this.getWidth(),this.getHeight());
        b[screen]=this.screenbuffer[screen];
            
        
        // MEGA hack: all images share the same raster data.
        WritableRaster r=    screenbuffer[screen].getRaster();
        
        
        
        for (int i=0;i<icms.length;i++){
            if (i!=screen)
            b[i]=new BufferedImage(icms[i],r, false,null);
            
        }
        
        this.report(b);
        
        
        return b;
        
    }

/** Returns a Raster of one of the internal screen BufferedImages.
 *  Call ONLY after V.Init() has been called!
 *  
 * @param screen
 */
public Raster getRaster(int screen){
	return this.screenbuffer[screen].getRaster();
}

public void report(BufferedImage[] b){
    System.out.println("Main video buffer "+screens[0]);
    for (int i=0;i<b.length;i++){
    System.out.println(((Object)b[i].getRaster()).toString()+" "+b[i].getRaster().hashCode()+" "+((DataBufferByte)(b[i].getRaster().getDataBuffer())).getData());
    }
}
}
