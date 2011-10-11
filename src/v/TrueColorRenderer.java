package v;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import m.BBox;

/** Truecolor (24/32-bit) renderer. Uses indexed palette LUTs for a RGB888
 * display. BufferedImage.TYPE_INT_RGB seems to be the fastest, almost on part 
 * with built-in indexed, while ARGB has nearly half the performace. Go figure. 
 * 
 * @author velktron
 *
 */

public class TrueColorRenderer extends SoftwareVideoRenderer {
	
static final String rcsid = "$Id: TrueColorRenderer.java,v 1.3 2011/10/11 16:55:30 velktron Exp $";


/* With a truecolour raster, some things are indeed easier */
protected int[][] palettes;
protected int[] raster;

public TrueColorRenderer(){
super();
}

public TrueColorRenderer(int w,int h){
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
     
  // Using ARGB is half the speed, WTF? While RGB is almost as fast as indexed. Go figure.
  this.currentscreen=new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
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
public void createPalettes(byte[] paldata, short[][] gammadata, int palettes,
		int colors, int stride, int gammalevels) {
	// Sanity check on supplied data length. If there is not enough data to create the specified palettes,
	// their number will be limited.
	
	if (paldata!=null) 	// As many as are likely contained
		maxpalettes=paldata.length/(colors*stride);
	else
		maxpalettes=0; // Do some default action on null palette.

	if (gammadata!=null) 	// As many as are likely contained
		maxgammas=gammadata.length;
	else
		maxgammas=0; // Do some default action on null gamma tables.
	
	if (maxgammas==0){
		gammadata=GammaTables.gammatables;
		maxgammas=GammaTables.gammatables.length;
	}
	

	// Enough data for all palettes. 
	if (maxpalettes>0 && maxgammas>0){
		  System.out.printf("Enough data for %d palettes",maxpalettes);
		  System.out.printf("Enough data for %d gamma levels",maxgammas);
    	  
    	  // Create as many gamma levels as specified.
    	  this.palettes=new int[maxgammas*maxpalettes][];
   	  
    	  for (int z=0;z<maxgammas;z++){
    		  
    		  // For each palette
    		  for (int y=0;y<maxpalettes;y++){
    			  this.palettes[z*maxpalettes+y]=new int[colors];
    			  
    			  for (int x=0;x<256;x++){
    				  int r=gammadata[z][0xFF&paldata[y*colors*stride+stride*x]]; // R
    				  int g=gammadata[z][0xFF&paldata[1+y*colors*stride+stride*x]]; // G
    				  int b=gammadata[z][0xFF&paldata[2+y*colors*stride+stride*x]]; // B
    				  int color=0xFF000000|r<<16|g<<8|b;
    				  this.palettes[z*maxpalettes+y][x]=color;
    			  	}
    	  		}
    	  }

    	  
      } else {
    	  // TODO: Allow it to pull from some default location?
    	  System.err.println("Palette and colormaps could not be set up. Bye");
    	  System.exit(-1);
      }
	
}


/** Hotlinks a 32-bit "canvas" (the raster int[] array) to an externally supplied
 *  buffered image. Now whatever we write into raster, will appear in the image as well,
 *  without using drawing primitives. Necessary for fast rendering.
 *  
 * @param b
 */

private void mapInternalRasterToBufferedImage(BufferedImage b){
    raster=((DataBufferInt)(b.getRaster().getDataBuffer())).getData();
    
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
    final int[] pal=this.palettes[usegamma*maxpalettes+usepalette];
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

