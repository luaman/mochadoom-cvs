package v;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

public class myBufferedImage
        extends BufferedImage {

    public myBufferedImage(int width, int height, int imageType,
            IndexColorModel cm) {
        super(width, height, imageType, cm);
        // TODO Auto-generated constructor stub
    }

    public ColorModel getRGB(){
        return getColorModel();
    }
    
    public void setColorModel(){
        super.getColorModel().
    }
    
    IndexColorModel
    
}
