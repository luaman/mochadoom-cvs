package testers;

import static data.Defines.KEY_BACKSPACE;
import static data.Defines.KEY_DOWNARROW;
import static data.Defines.KEY_ENTER;
import static data.Defines.KEY_EQUALS;
import static data.Defines.KEY_ESCAPE;
import static data.Defines.KEY_F1;
import static data.Defines.KEY_F10;
import static data.Defines.KEY_F11;
import static data.Defines.KEY_F12;
import static data.Defines.KEY_F2;
import static data.Defines.KEY_F3;
import static data.Defines.KEY_F4;
import static data.Defines.KEY_F5;
import static data.Defines.KEY_F6;
import static data.Defines.KEY_F7;
import static data.Defines.KEY_F8;
import static data.Defines.KEY_F9;
import static data.Defines.KEY_LEFTARROW;
import static data.Defines.KEY_MINUS;
import static data.Defines.KEY_PAUSE;
import static data.Defines.KEY_RALT;
import static data.Defines.KEY_RCTRL;
import static data.Defines.KEY_RIGHTARROW;
import static data.Defines.KEY_RSHIFT;
import static data.Defines.KEY_TAB;
import static data.Defines.KEY_UPARROW;

import i.InputListener;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import doom.event_t;

public class CrappyDisplay extends JFrame{

        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		Canvas drawhere;
        BufferedImage[] bi;
        int palette=0;
        Dimension size;
        Image crap;
        InputListener in;
        Graphics2D g2d;

        public CrappyDisplay(BufferedImage[] bi) {
        	this.setBackground(Color.black);
            this.bi=bi;            
            size = new Dimension();
            size.width = bi[0].getWidth();
            size.height = bi[0].getHeight();
            this.setSize(size);
            drawhere=new Canvas();
            drawhere.setSize(size);
            drawhere.setBounds(0, 0, drawhere.getWidth(),drawhere.getHeight());
            drawhere.setBackground(Color.black);            
            this.add(drawhere);
            in = new InputListener();
            this.addComponentListener(in);
            g2d = (Graphics2D)drawhere.getGraphics();
        }
        
        public CrappyDisplay(BufferedImage bi) {
            this.bi=new BufferedImage[]{bi};
            size = new Dimension();
            size.width = bi.getWidth();
            size.height = bi.getHeight();
            this.setSize(size);
            drawhere=new Canvas();
            drawhere.setSize(size);
            drawhere.setBounds(0, 0, drawhere.getWidth(),drawhere.getHeight());
            drawhere.setBackground(Color.black);
            this.add(drawhere);
            in = new InputListener();
            this.addComponentListener(in);
            g2d = (Graphics2D)drawhere.getGraphics();
        }
        
        public void setPalette(int pal){
            this.palette=pal;
            
        }
        
        public void update(Graphics g) {
           
           //Graphics2D g2d = (Graphics2D)drawhere.getGraphics();
           //voli.getGraphics().drawImage(bi,0,0,null);
           g2d.drawImage(bi[palette],0,0,this);
           
        }
        
        public void update() {
            
            Graphics2D g2d = (Graphics2D)drawhere.getGraphics();
            //voli.getGraphics().drawImage(bi,0,0,null);
            g2d.drawImage(bi[palette],0,0,this);
            
         }
        
        public String processEvents(){
            StringBuffer tmp=new StringBuffer();
            event_t event;
            while ( (event=InputListener.nextEvent()) != null ) {
                tmp.append(event.type.ordinal()+"\n");
            }
            return tmp.toString();
        }

        public int xlatekey(KeyEvent e)
        {

          int rc;

          switch(rc =     e.getID())
              //Event.XKeycodeToKeysym(X_display, X_event.xkey.keycode, 0))

          {
            case Event.LEFT:    rc = KEY_LEFTARROW; break;
            case Event.RIGHT:   rc = KEY_RIGHTARROW;    break;
            case Event.DOWN:    rc = KEY_DOWNARROW; break;
            case Event.UP:  rc = KEY_UPARROW;   break;
            case Event.ESCAPE:  rc = KEY_ESCAPE;    break;
            case Event.ENTER:   rc = KEY_ENTER;     break;
            case Event.TAB: rc = KEY_TAB;       break;
            case Event.F1:  rc = KEY_F1;        break;
            case Event.F2:  rc = KEY_F2;        break;
            case Event.F3:  rc = KEY_F3;        break;
            case Event.F4:  rc = KEY_F4;        break;
            case Event.F5:  rc = KEY_F5;        break;
            case Event.F6:  rc = KEY_F6;        break;
            case Event.F7:  rc = KEY_F7;        break;
            case Event.F8:  rc = KEY_F8;        break;
            case Event.F9:  rc = KEY_F9;        break;
            case Event.F10: rc = KEY_F10;       break;
            case Event.F11: rc = KEY_F11;       break;
            case Event.F12: rc = KEY_F12;       break;
            
            //case Event.BACK_SPACE:
            case Event.DELETE:  rc = KEY_BACKSPACE; break;

            case Event.PAUSE:   rc = KEY_PAUSE;     break;

            case Event.KEY_PRESS:
                switch(e.getKeyCode()){
            case (KeyEvent.VK_EQUALS): rc = KEY_EQUALS;    break;
            case (KeyEvent.VK_SUBTRACT): rc=KEY_MINUS; break;
            
                }
            /*case Event.KP_Subtract:
            case Event.minus:   rc = KEY_MINUS;     break;*/

            case Event.SHIFT_MASK:
            rc = KEY_RSHIFT;
            break;
            
            case Event.CTRL_MASK:
            rc = KEY_RCTRL;
            break;
            
            //case Event.ALT_MASK:
            case Event.ALT_MASK:
            rc = KEY_RALT;
            break;
            
            default:
            if (rc >= ' ' && rc <= '~')
                rc = rc - Event.GOT_FOCUS + ' ';
            if (rc >= 'A' && rc <= 'Z')
                rc = rc - 'A' + 'a';
            break;
          }

          return rc;

        }
    
}
