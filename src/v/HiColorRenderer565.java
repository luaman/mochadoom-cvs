package v;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;

import m.BBox;

/** Hi-Color (16-bit) renderer. Uses indexed palette LUTs for a RGB565
 * display. Slower than a RGB 888 display, for some reason :-/
 * 
 * @author velktron
 *
 */

public class HiColorRenderer565 extends SoftwareVideoRenderer {
	
static final String rcsid = "$Id: HiColorRenderer565.java,v 1.2 2011/10/23 20:40:44 velktron Exp $";


/* With a truecolour raster, some things are indeed easier */
protected short[][] palettes;
protected short[] raster;

public HiColorRenderer565(){
super();
}

public HiColorRenderer565(int w,int h){
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
     
  this.currentscreen=new BufferedImage(width,height, BufferedImage.TYPE_USHORT_565_RGB);
  this.mapInternalRasterToBufferedImage((BufferedImage) currentscreen);
}

@Override
public void setUsegamma(int gammalevel) {
	this.usegamma=gammalevel%maxgammas;
	
}

@Override
public void setPalette(int palette) {
	this.usepalette=palette%maxpalettes;
}

@Override
protected final void specificPaletteCreation(byte[] paldata,
		short[][] gammadata, 
		final int palettes, 
		final int colors,
		final int stride,
		final int gammalevels){

	  System.out.printf("Enough data for %d palettes",maxpalettes);
	  System.out.printf("Enough data for %d gamma levels",maxgammas);
	  
	  this.palettes=new short[maxgammas*maxpalettes][];
	  
	  for (int z=0;z<maxgammas;z++){
		  
		  // For each palette
		  for (int y=0;y<maxpalettes;y++){
			  this.palettes[z*maxpalettes+y]=new short[colors];
			  
			  for (int x=0;x<256;x++){
				  int r=gammadata[z][0xFF&paldata[y*colors*stride+stride*x]]; // R
				  int g=gammadata[z][0xFF&paldata[1+y*colors*stride+stride*x]]; // G
				  int b=gammadata[z][0xFF&paldata[2+y*colors*stride+stride*x]]; // B
				  r>>>=3;
		  			g>>>=2;
		  			b>>>=3;
				  short color=(short) ((r<<11)|(g<<5)|b);
				  this.palettes[z*maxpalettes+y][x]=color;
			  	}
	  		}
	  }
}

/** Hotlinks a 32-bit "canvas" (the raster int[] array) to an externally supplied
 *  buffered image. Now whatever we write into raster, will appear in the image as well,
 *  without using drawing primitives. Necessary for fast rendering.
 *  
 * @param b
 */

private void mapInternalRasterToBufferedImage(BufferedImage b){
    raster=((DataBufferUShort)(b.getRaster().getDataBuffer())).getData();    
}

/** Update "canvas" to one of the internal screens.
 *  
 * @param screen
 * @param b
 */

@Override
public  final void update()  {
    final byte[] scr=this.screens[usescreen];
    final int length=scr.length; 
    final short[] pal=this.palettes[usegamma*maxpalettes+usepalette];
    for (int i=0;i<length;i+=16){
        raster[i]=pal[0xFF&scr[i]];
        raster[i+1]=pal[0xFF&scr[i+1]];
        raster[i+2]=pal[0xFF&scr[i+2]];
        raster[i+3]=pal[0xFF&scr[i+3]];
        raster[i+4]=pal[0xFF&scr[i+4]];
        raster[i+5]=pal[0xFF&scr[i+5]];
        raster[i+6]=pal[0xFF&scr[i+6]];
        raster[i+7]=pal[0xFF&scr[i+7]];
        raster[i+8]=pal[0xFF&scr[i+8]];
        raster[i+9]=pal[0xFF&scr[i+9]];
        raster[i+10]=pal[0xFF&scr[i+10]];
        raster[i+11]=pal[0xFF&scr[i+11]];
        raster[i+12]=pal[0xFF&scr[i+12]];
        raster[i+13]=pal[0xFF&scr[i+13]];
        raster[i+14]=pal[0xFF&scr[i+14]];
        raster[i+15]=pal[0xFF&scr[i+15]];

    }
}


}

