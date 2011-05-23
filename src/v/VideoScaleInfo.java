package v;

public class VideoScaleInfo
        implements IVideoScale {
    
   protected float scale;
   protected int width;
   protected int height;
   protected int bestScaleX;
   protected int bestScaleY;
   

   protected int bestSafeScale;
   
   public VideoScaleInfo(float scale){
       this.scale=scale;
       width=(int) (BASE_WIDTH*scale);
       height=(int) (scale*BASE_WIDTH*INV_ASPECT_RATIO);
       bestScaleX= (int) Math.floor((float)width/(float)BASE_WIDTH);
       bestScaleY= (int) Math.floor((float)height/(float)BASE_HEIGHT);
       bestSafeScale= Math.min(bestScaleX, bestScaleY);
       
   }
      
    @Override
    public int getScreenWidth() {
        return width;
    }

    @Override
    public int getScreenHeight() {      
        return height;
    }

    @Override
    public int getScalingX() {
        return bestScaleX;
    }

    @Override
    public int getScalingY() {
        return bestScaleY;
    }

    @Override
    public int getSafeScaling() {
        return bestSafeScale;
    }

    @Override
    public boolean changed() {
        return false;
    }

    @Override
    public float getScreenMul() {        
        return scale;
    }

}
