package testers;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class CrappyDisplay extends JPanel{

        VolatileImage voli;
        BufferedImage bi;
        Dimension size;
        Image crap;

        public CrappyDisplay(BufferedImage bi) {
        	JFrame bogus=new JFrame();
            GraphicsConfiguration gc=bogus.getGraphicsConfiguration();
            this.bi=bi;
            this.voli=gc.createCompatibleVolatileImage(bi.getWidth(), bi.getHeight());
            size = new Dimension();
            size.width = voli.getWidth();
            size.height = voli.getHeight();

        }
        
        public void paint(Graphics g) {
           
           Graphics2D g2d = (Graphics2D)g;
           //voli.getGraphics().drawImage(bi,0,0,null);
           g2d.drawImage(bi,0,0,null);
           
        }


    
}
