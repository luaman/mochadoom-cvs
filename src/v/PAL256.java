package v;

public class PAL256 {
    
    public int[] palette=new int[256];
    
    public int[] raster24;
    
    public byte[] raster8;
    
    public PAL256(byte[] r8, int[] r24){
        this.raster24=r24;
        this.raster8=r8;
    }
    
    public void convert(){
        
        for (int i=0;i<raster8.length;i++)
            raster24[i]=palette[raster8[i]];
        
    }
    

}
