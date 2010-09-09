package testers;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

import javax.swing.JFrame;

public class CrappyDisplay extends Canvas{

        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		VolatileImage voli;
        BufferedImage[] bi;
        int palette=0;
        Dimension size;
        Image crap;

        public CrappyDisplay(BufferedImage[] bi) {
        	this.setBackground(Color.black);
        	JFrame bogus=new JFrame();
            GraphicsConfiguration gc=bogus.getGraphicsConfiguration();
            this.bi=bi;
            this.voli=gc.createCompatibleVolatileImage(bi[0].getWidth(), bi[0].getHeight());
            size = new Dimension();
            size.width = voli.getWidth();
            size.height = voli.getHeight();
            this.setSize(size);
        }
        
        public CrappyDisplay(BufferedImage bi) {
            JFrame bogus=new JFrame();
            GraphicsConfiguration gc=bogus.getGraphicsConfiguration();
            this.bi=new BufferedImage[]{bi};
            this.voli=gc.createCompatibleVolatileImage(bi.getWidth(), bi.getHeight());
            size = new Dimension();
            size.width = voli.getWidth();
            size.height = voli.getHeight();

        }
        
        public void setPalette(int pal){
            this.palette=pal;
        }
        
        public void update(Graphics g) {
           
           Graphics2D g2d = (Graphics2D)g;
           //voli.getGraphics().drawImage(bi,0,0,null);
           g2d.drawImage(bi[palette],0,0,this);
           
        }


    
}
