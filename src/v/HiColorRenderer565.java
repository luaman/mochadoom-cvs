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
	
static final String rcsid = "$Id: HiColorRenderer565.java,v 1.1 2011/10/11 21:01:31 velktron Exp $";


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
    	  
    	  // Create as gamma levels as specified.
    	  this.palettes=new short[maxgammas*maxpalettes][];
    	  
    	  // First set of palettes, normal gamma.
    	  //this.palettes[0]=new int[maxpalettes];
    
    	  // Now we have our palettes.
    	  // SUPER OPTIMIZATION: flat array access.
    	  // Normally it would be palettes[gammas][palettes][colors];
    	  // x + WIDTH * (y + DEPTH * z)]
    	  // x= colors y= palette z= gamma WIDTH= #color DEPTH= #palettes
    	  // For each gamma value...

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
    raster=((DataBufferUShort)(b.getRaster().getDataBuffer())).getData();
    
}

/** Creates a 256 color int palette
 * 
 * @param pal palette lump from IWAD
 */

public void setPalette(byte[] pal){
/*
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
        
    } */
    
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

