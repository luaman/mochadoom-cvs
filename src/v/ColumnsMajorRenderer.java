package v;

import static data.Defines.RANGECHECK;
import rr.column_t;
import rr.patch_t;

public class ColumnsMajorRenderer
        extends SoftwareVideoRenderer {

    @Override
    public void Init() {
        // TODO Auto-generated method stub

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
    
}
