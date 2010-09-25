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

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.swing.JFrame;

import doom.event_t;
import doom.evtype_t;

public class CrappyDisplay extends JFrame implements KeyListener{

     /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		Canvas drawhere;
        BufferedImage[] bi;
        int palette=0;
        Dimension size;
        Image crap;
        //InputListener in;
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
            this.addKeyListener(this);
            //this.addComponentListener(in);
            this.setVisible(true);
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
            
            //in = new InputListener();
            //this.addComponentListener(in);
            this.setVisible(true);
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

          switch(rc =     e.getKeyCode())
          
              //Event.XKeycodeToKeysym(X_display, X_event.xkey.keycode, 0))

          {
            case KeyEvent.VK_LEFT:    rc = KEY_LEFTARROW; break;
            case KeyEvent.VK_RIGHT:   rc = KEY_RIGHTARROW;    break;
            case KeyEvent.VK_DOWN:    rc = KEY_DOWNARROW; break;
            case KeyEvent.VK_UP:  rc = KEY_UPARROW;   break;
            case KeyEvent.VK_ESCAPE:  rc = KEY_ESCAPE;    break;
            case KeyEvent.VK_ENTER:   rc = KEY_ENTER;     break;
            case KeyEvent.VK_TAB: rc = KEY_TAB;       break;
            case KeyEvent.VK_F1:  rc = KEY_F1;        break;
            case KeyEvent.VK_F2:  rc = KEY_F2;        break;
            case KeyEvent.VK_F3:  rc = KEY_F3;        break;
            case KeyEvent.VK_F4:  rc = KEY_F4;        break;
            case KeyEvent.VK_F5:  rc = KEY_F5;        break;
            case KeyEvent.VK_F6:  rc = KEY_F6;        break;
            case KeyEvent.VK_F7:  rc = KEY_F7;        break;
            case KeyEvent.VK_F8:  rc = KEY_F8;        break;
            case KeyEvent.VK_F9:  rc = KEY_F9;        break;
            case KeyEvent.VK_F10: rc = KEY_F10;       break;
            case KeyEvent.VK_F11: rc = KEY_F11;       break;
            case KeyEvent.VK_F12: rc = KEY_F12;       break;
            
            //case Event.BACK_SPACE:
            case KeyEvent.VK_DELETE:  rc = KEY_BACKSPACE; break;

            case KeyEvent.VK_PAUSE:   rc = KEY_PAUSE;     break;

            case KeyEvent.KEY_PRESSED:
                switch(e.getKeyCode()){
            case (KeyEvent.VK_EQUALS): 
                rc = KEY_EQUALS;    
                break;
                
            case KeyEvent.VK_SUBTRACT: 
            case KeyEvent.VK_MINUS:   
                rc = KEY_MINUS;     
                break;

            case KeyEvent.SHIFT_DOWN_MASK:
            rc = KEY_RSHIFT;
            break;
            
            case KeyEvent.CTRL_DOWN_MASK:
            rc = KEY_RCTRL;
            break;                           
           
            case KeyEvent.ALT_DOWN_MASK:
            rc = KEY_RALT;
            break;
            }
                
            default:
            /*if (rc >= KeyEvent.VK_SPACE && rc <= KeyEvent.VK_DEAD_TILDE)
                rc = (int) (rc - KeyEvent.FOCUS_EVENT_MASK + ' ');*/
            if (rc >= KeyEvent.VK_A && rc <= KeyEvent.VK_Z)
                rc = rc-KeyEvent.VK_A +'a';
            break;
          }
          
          System.out.println("Typed "+e.getKeyCode()+" char "+e.getKeyChar()+" mapped to "+Integer.toHexString(rc));

          return rc;

        }

        public void keyPressed(KeyEvent e) {
            if (!((e.getModifiersEx() & InputEvent.ALT_GRAPH_DOWN_MASK) != 0)) {
                addEvent(new event_t(evtype_t.ev_keydown, xlatekey(e)));
            }
        }

        public void keyReleased(KeyEvent e) {
          //  addEvent(new event_t(evtype_t.ev_keyup,xlatekey(e)));
        }

        public void keyTyped(KeyEvent e) {
         /*   if ((e.getModifiersEx() & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
                addEvent(new event_t(evtype_t.ev_keydown, xlatekey(e)));
                addEvent(new event_t(evtype_t.ev_keyup, xlatekey(e)));
            } */       
        }
        
        static void addEvent(event_t ev) {
            synchronized (eventQueue) {
                eventQueue.addLast(ev);
            }
        }

        public static event_t nextEvent() {
            event_t ev;
            synchronized (eventQueue) {
                ev = (!eventQueue.isEmpty())?(event_t)eventQueue.removeFirst():null;
            }
            return ev;
        }
        
     // modifications of eventQueue must be thread safe!
        private static LinkedList<event_t> eventQueue = new LinkedList<event_t>();
    
}
