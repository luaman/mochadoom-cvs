package v;

import static data.Defines.RANGECHECK;

import i.DoomStatusAware;
import i.DoomVideoInterface;
import i.IDoomSystem;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.Defines;
import doom.DoomStatus;

import m.BBox;

import rr.column_t;
import rr.patch_t;
import utils.C2JUtils;

public abstract class SoftwareVideoRenderer
        implements DoomVideoRenderer, IVideoScaleAware, DoomStatusAware{

    IDoomSystem I;
    DoomVideoInterface VI;
    
    public final void updateStatus(DoomStatus DM){
    	this.VI=DM.VI;
    	this.I=DM.I;
    }
    
 // Now where did these came from? /*[5][256]*/
    public static final short[][] gammatable =
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

    static byte[][] colbuf;
    protected int CENTERY;//=           (SCREENHEIGHT/2);
    protected int width;
    protected int height;

    /** Each screen is [SCREENWIDTH*SCREENHEIGHT]; */ 
    public byte[][]         screens=new byte[5][];  
     
    //MAES: maybe this should be a bbox?

    public BBox             dirtybox=new BBox(); 
    
    public SoftwareVideoRenderer(){
        // Defaults
        width=SCREENWIDTH;
        height=SCREENHEIGHT;
        CENTERY=        (SCREENHEIGHT/2);
    }

    public SoftwareVideoRenderer(int w,int h){
        // Defaults
        width=w;
        height=h;
        CENTERY=        (h/2);
    }
    
    public int  usegamma=0;
    
    public final int getUsegamma() {
        return usegamma;
    }

    public final void setUsegamma(int usegamma) {
        this.usegamma = usegamma;
        this.VI.setGamma(usegamma);
    }
    
    /** V_Markrect:
     *  Apparently, video.c had its own "dirtybox" bbox, and this was a crude method for 
     *  expanding it.
     * 
     */
    
    public final void MarkRect ( int      x,  int     y,  int     width,  int     height ) 
    { 
        dirtybox.AddToBox(x, y); 
        dirtybox.AddToBox(x+width-1, y+height-1); 
    } 
    
  /**
   *  V_CopyRect 
   */

  public final void CopyRect (int       srcx,
          int     srcy,
          int     srcscrn,
          int     width,
          int     height,
          int     destx,
          int     desty,
          int     destscrn ) 
  { 
      // These are pointers inside an array.
      byte[]  src=screens[srcscrn];
      byte[]  dest=screens[destscrn]; 

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
              I.Error ("Bad V_CopyRect");
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
  
  /**
   *  V_Fillrect 
   */
  @Override
  public void FillRect(int srcx, int srcy, int width,
          int height,int destscrn){
      // These are pointers inside an array.
      byte[]  dest=screens[destscrn]; 

      if  (RANGECHECK) {
          if (srcx<0
                  ||srcx+width >this.width
                  || srcy<0
                  || srcy+height>SCREENHEIGHT 
                  || destscrn>4)
          {
              I.Error ("Bad V_FillRect");
          }
      } 
      this.MarkRect (srcx, srcy, width, height); 


      // MAES: these were pointers to a specific position inside the screen.
      int srcPos = this.width*srcy+srcx; 

      for ( ; height>0 ; height--) 
      { 
    	  for (int i=0;i<width;i++){
          dest[srcPos+i]=0;
    	  }
          //memcpy (dest, src, width); 
          srcPos += this.width; 
      }

  }
  
  /** V_DrawPatch
   * Masks a column based masked pic to the screen. 
   *  desttop, dest and source were byte*
   */ 

  public final void DrawPatch
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
          // ptr+=3; NOT NEEDED< pre-skipped at parsing. 
          
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

  /** V_DrawPatchSolidScaled
   * Draws a SOLID (non-masked) patch to the screen with integer scaling
   * m and n.
   * Useful for stuff such as help screens, titlepic and status bar. Not 
   * very useful for menus, though.
   * desttop, dest and source were byte
   */ 

  public final void DrawPatchSolidScaled
  ( int       x,
  int     y,int m, int n,
  int     scrn,
  patch_t patch ) 
  { 

   if ((m==1)&&(n==1)){
       DrawPatch(x,y,scrn,patch);
       return;
   }
      
   column_t   column; 
   int    desttop;
   byte[] dest=screens[scrn];
   int        w; 
       
   y =y*n- patch.topoffset; 
   x =x*m- patch.leftoffset; 
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
   desttop = m*x+this.width*y; 
   // For each column..
   int destPos;
   int ptr=0;
   // x increases by m.
   
   // Some unrolling...
   
   if (m==2) {
       //desttop=2*desttop;
       for (int col=0 ; col<w ; desttop+=2, col++)

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
               //ptr+=3; 

               // Skip transparent rows...
               if (delta==0xFF) break;

               destPos = desttop+ n*delta*this.width;  

               // These lengths are already correct.
               for (int j=0;j<column.postlen[i];j++){
                   dest[destPos] = column.data[ptr++];
                   dest[destPos+1] = dest[destPos];
                   destPos += n*this.width;
               }
           }
       }
   } else  if (m==3) {
      // desttop=3*desttop;
       for (int col=0 ; col<w ; desttop+=3, col++)

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
               //ptr+=3; 

               // Skip transparent rows...
               if (delta==0xFF) break;

               destPos = desttop + n*delta*this.width;  

               // These lengths are already correct.
               for (int j=0;j<column.postlen[i];j++){
                   dest[destPos] = column.data[ptr++];
                   dest[destPos+1] = dest[destPos];
                   dest[destPos+2] = dest[destPos];
                   destPos += n*this.width;    
               }
               
           }
       }
   }else  if (m==4) {
           //desttop=4*desttop;
           for (int col=0 ; col<w ; desttop+=4, col++)

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
                   //ptr+=3; 

                   // Skip transparent rows...
                   if (delta==0xFF) break;

                   destPos = desttop + n*delta*this.width;  

                   // These lengths are already correct.
                   for (int j=0;j<column.postlen[i];j++){
                       dest[destPos] = column.data[ptr++];
                       dest[destPos+1] = dest[destPos];
                       dest[destPos+2] = dest[destPos];
                       dest[destPos+3] = dest[destPos];
                       destPos += n*this.width;
                   }
               }
           }
       }
           else  {
              // desttop=m*desttop;
               for (int col=0 ; col<w ; desttop+=m, col++)

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
                       //ptr+=3; 

                       // Skip transparent rows...
                       if (delta==0xFF) break;

                       destPos = desttop + n*delta*this.width;  

                       // These lengths are already correct.
                       for (int j=0;j<column.postlen[i];j++){
                           for (int k=0;k<m;k++)
                           dest[destPos+k] = column.data[ptr];
                           ptr++;
                           destPos += n*this.width;
                       }
                   }
               }
       
       
   }
   
   
    scaleSolid(m,n, scrn,m*patch.width);  
  }
  

  /** Pretty crude in-place scaling. It's fast, but only works full-screen 
   * Width needs to be specific, height is implied.
   *  */
  
  protected final void scaleSolid(int m, int n, int screen,int width){
      int height=screens[screen].length/width;
      for (int i=0;i<height;i+=n){
           
          for (int j=0;j<n-1;j++){

              System.arraycopy(screens[screen], (i+j)*width, screens[screen],(i+j+1)*width,width);
          }
      }
      
  }
  
  protected final boolean doRangeCheck(int x, int y,patch_t patch, int scrn){
    return      (x<0
                  ||x+patch.width >this.width
                  || y<0
                  || y+patch.height>this.height 
                  || scrn>4);
  }

  protected final boolean doRangeCheck(int x, int y, int scrn){
      return      (x<0
                    ||x>this.width
                    || y<0
                    || y>this.height 
                    || scrn>4);
    }

  public void DrawPatchSolidScaled ( int x, int y,  int scrn, IVideoScale vs, patch_t patch ){
      this.DrawPatchSolidScaled(x, y, vs.getScalingX(), vs.getScalingY(),scrn, patch);
  }
  
  /**
   * V_DrawPatchFlipped 
   * Masks a column based masked pic to the screen.
   * Flips horizontally, e.g. to mirror face.
   * 
   * Currently UNUSED, as any occurence to it is also scaled and
   * best served by another function.
   * 
   * 
   */

  public final void DrawPatchFlipped ( int      x,   int        y,    int       scrn,  patch_t  patch ) 
  { 

      column_t    column; 
      int desttop;
      byte[]  dest=screens[scrn];
      int     w; 
       
      y -= patch.topoffset; 
      x -= patch.leftoffset;
      

      if (RANGECHECK) 
          if (doRangeCheck(x,y,patch,scrn))
          {
      {
          System.err.print("Patch origin "+x+","+y +" exceeds LFB\n" );
          // No I_Error abort - what is up with TNT.WAD?
          I.Error("Bad V_DrawPatch in V_DrawPatchFlipped");
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
         //System.out.println("Column"+(w-1-col));
         for (int i=0;i<column.posts;i++){
             // Get pointer to post offset.
             int ptr=column.postofs[i];
             // Get post delta
             int delta=0xFF&column.data[ptr];
             // We skip delta, len and padding.
             //ptr+=3;
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
   
/**
 * V_DrawScaledPatch
 * like V_DrawPatch, but scaled 2,3,4 times the original size and position
 * this is used for menu and title screens, with high resolutions
 * 
 *added:05-02-98:
 *default params : scale patch and scale start
 *
 * Iniially implemented for Mocha Doom by _D_ (shamelessly ripped from 
 * Eternity Engine ;-), adapted to scale based on a scaling info object (VSI).
 * 
 * Unless overriden by flags, starting x and y are automatically
 * scaled (implied V_SCALESTART)
 *
 */
@Override
public void DrawScaledPatch(int x, int y, int scrn, IVideoScale VSI, patch_t patch)
{
    int col;
    column_t column;
    int desttop;
    byte[] dest = screens[scrn&0xFF];
    //byte[] source;

    int dupx, dupy;
    int colfrac, rowfrac;

   // System.out.printf("V_DrawScaledPatch %d %d \n",x,y);
    // draw an hardware converted patch
    /*#ifdef HWRENDER
  if (rendermode != render_soft)
  {
      HWR_DrawPatch((GlidePatch_t *) patch, x, y, scrn);
      return;
  }
#endif*/

    // A very common operation, eliminates the need to pre-divide.
    if (C2JUtils.flags(scrn,V_PREDIVIDE)){
        x/=vs.getScalingX();
        y/=vs.getScalingY();
    }
    
    if (C2JUtils.flags(scrn, V_NOSCALEPATCH))
        dupx = dupy = 1;
    else
    {
        dupx = VSI.getScalingX();
        dupy = VSI.getScalingY();
    }

    // Eliminates.
    // MAES: added this fix so that non-zero patch offsets can be 
    // taken into account, regardless of whether we use pre-scaled
    // coords or not. Only Doomguy's face needs this hack for now.
    
    if (C2JUtils.flags(scrn, V_SCALEOFFSET)) {
    y -= patch.topoffset*dupx;
    x -= patch.leftoffset*dupy;
    } else {
        y -= patch.topoffset;
        x -= patch.leftoffset;
    }
    

    colfrac = dupx;
    rowfrac = dupy;

    //desttop = screens[scrn & 0xFF];
    if (C2JUtils.flags(scrn, V_NOSCALESTART))
        desttop = (y * this.width) + x;
    else
        desttop = (y * dupy * this.width) + (x * dupx) /*+ scaledofs*/;
    //destend = desttop + /*SHORT(*/patch.width/*)*/ * dupx;

    int w = patch.width*dupx;

    int colInc = 1;
    col = 0;
    if (C2JUtils.flags(scrn, V_FLIPPEDPATCH))
    {
        colInc = -1;
        col = w-1;//(/*SHORT(*/patch.width/*)*/ << fixed_t.FRACBITS) + colfrac;
    }
    
    for (; col >= 0 && col<w/*; desttop < destend*/; col += colInc, desttop++)
    {
        //column = (column_t *) ((byte *) patch + LONG(patch.columnofs[col >> FRACBITS]));
        column=patch.columns[col/colfrac];

        int destPos;
        int ptr = 0;
        int ptrOfs;

        //while (column.topdelta != 0xff)
        for (int i=0;i<column.posts;i++){
            {
                ptrOfs = column.postofs[i];//+3;
                ptr = 0;
                short delta = column.postdeltas[i];

                // Skip transparent rows...
                if (delta==0xFF) break;

                destPos = desttop + delta*dupy*this.width;  
                //dest = desttop + column.topdelta * dupy * this.width;

                //ofs = 0;
                //while (count-- > 0)
                for (int j=0;j<column.postlen[i]*dupy;j++)
                {
                    dest[destPos] = column.data[ptrOfs+ptr/rowfrac];
                    destPos += this.width;

                    ptr++;
                    //ofs += rowfrac;
                }

                //column = (column_t *) ((byte *) column + column.length + 4);
            }
        }
    }
}

  @Override
  public final void
  DrawBlock
  ( int       x,
    int       y,
    int       scrn,
    int       width,
    int       height,
    byte[]        src ) 
  { 
      // This is "screens[scrn]"
      byte[]  dest=screens[scrn];  
       
  if (doRangeCheck(x, y, scrn))
      {
      I.Error("Bad V_DrawBlock");
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

   

  /**
   * V_GetBlock
   * Gets a linear block of pixels from the view buffer.
   */


  public final void
  GetBlock
  ( int       x,
    int       y,
    int       scrn,
    int       width,
    int       height,
    byte[]        dest ) 
  { 
      byte[]  src=screens[scrn]; 
       
  if (RANGECHECK){
      if (doRangeCheck(x,y,scrn)){    
      I.Error ("Bad V_DrawBlock");
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
  
  
  /** Replaces DrawPatchCol for bunny scrolled in Finale.
   * 
   * 
   */
  
  @Override
  public final void
  DrawPatchColScaled
  ( int       x, patch_t  patch,int col, 
          IVideoScale vs, int       screen )
  {
      column_t   column;
      int   source;
      final byte[]   dest;
      int   desttop;
      final int scale=vs.getScalingX();
      
      column = patch.columns[col];
      desttop = x*scale; // Scale X position.
      dest=screens[screen];
      // step through the posts in a column
      
      
      for (int i=0;i<column.posts;i++){
          // Get pointer to post offset.
          source=column.postofs[i];
          // Get post delta
          short delta=column.postdeltas[i];
          // We skip delta, len and padding.
          //source+=3;  NOT NEEDED ANYMORE, skipped by parsing.
          
          // Skip transparent rows...
          if (delta==0xFF) break;

            
          
          // Replicate each column scale times vertically,
          // with spaced pixels.
          final int startsource=source;
          for (int kl=0;kl<scale;kl++){
              int destPos = desttop + (delta+kl)*SCREENWIDTH;
          for (int j=0;j<column.postlen[i];j++){
              final byte data=column.data[source++];
              // replicate each column's pixel horizontally and vertically.
                  for (int k=0;k<scale;k++)
                  dest[destPos+k] =data; 
                 destPos += scale*SCREENWIDTH;
              }
          source=startsource;
          }
      }
  }

  public final int getHeight() {
      return this.height;
  } 
  public final int getWidth() {
      return this.width;
  } 
  
  public final void DrawPatchDirect(int x, int y, int scrn, patch_t patch) {
      this.DrawPatch(x, y, scrn, patch);
      
  }

  public final byte[] getScreen(int index) {
     return screens[index];
  }

  public void setScreen(int index, int width, int height){
      this.screens[index]=new byte[width*height];
  }
  
  public final void takeScreenShot(int screen, String imagefile, IndexColorModel icm) throws IOException {
      
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
  
  public final boolean isRasterNull(int screen){
      for (int i=0;i<screens[screen].length;i++){
          if (screens[screen][i]!=0) return false;
      }
      return true;
  }
  
////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

  protected int SCREENWIDTH;
  protected int SCREENHEIGHT;
  protected IVideoScale vs;


  @Override
  public void setVideoScale(IVideoScale vs) {
      this.vs=vs;
  }

  @Override
  public void initScaling() {
      this.SCREENHEIGHT=vs.getScreenHeight();
      this.SCREENWIDTH=vs.getScreenWidth();
  }

    
}
