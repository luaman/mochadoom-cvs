package i;

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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import v.BufferedRenderer;
import v.DoomVideoRenderer;

import data.Defines;
import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;

/** A simple Doom display & keyboard driver for AWT.
 *  Uses a Canvas for painting and implements some
 *  of the IVideo methods.
 * 
 *  Uses 8-bit buffered images and switchable IndexColorModels.
 * 
 * @author Velktron
 *
 */
public class AWTDoom extends JFrame implements KeyListener,MouseListener,MouseMotionListener, DoomVideoInterface,DoomEventInterface{

     /**
	 * 
	 */
	
	// Must be aware of "Doom" so we can pass it event messages inside a crude queue.
	public DoomMain DM;
	private DoomSystemInterface I; // Must be aware of some other shit like event handler
	private BufferedRenderer V;   // Must be able to "peg" itself to a raw byte array representing
									// Screen 0
	private byte[] RAWSCREEN;	  // RAW SCREEN DATA. Get from the Video Renderer.
	private IndexColorModel[] cmaps;
	
		Canvas drawhere;
		/** These are the actual screens */
        BufferedImage[] screens;
        int palette=0;
        Dimension size;
        Image crap;
        //InputListener in;
        Graphics2D g2d;
        Cursor hidden;
        Cursor normal;
        /** Dimensions of the screen buffers */
        protected int width,height;
        protected int lastmousex;

		protected int lastmousey;

		protected boolean mousemoved;

		protected boolean ignorebutton;

		protected int multiply=1;

		protected boolean grabMouse;
		protected AWTEvent AWTevent;
		protected byte[] cmap;
		private int maxpalettes;

		/** Gimme some raw palette RGB data.
		 *  I will do the rest
		 *  
		 *  (hint: read this from the PLAYPAL
		 *   lump in the IWAD!!!).
		 * 
		 */

        public AWTDoom(DoomMain DM, BufferedRenderer V, byte[] cmap) {
        	this.DM=DM;
        	this.V= V;
        	this.cmap=cmap;
        	this.width=V.getWidth();
        	this.height=V.getHeight();
        // Don't do anything yet until InitGraphics is called.
        }
        
     
        public void setPalette(int pal){
            this.palette=pal;            
        }
        
        public void update(Graphics g) {
           
           //Graphics2D g2d = (Graphics2D)drawhere.getGraphics();
           //voli.getGraphics().drawImage(bi,0,0,null);
           g2d.drawImage(screens[palette],0,0,this);
           
        }
        
        public void update() {
            
            Graphics2D g2d = (Graphics2D)drawhere.getGraphics();
            //voli.getGraphics().drawImage(bi,0,0,null);
            g2d.drawImage(screens[palette],0,0,this);
            
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
                addEvent(e);
            }
        }

        public void keyReleased(KeyEvent e) {
        	  addEvent(e);
        }

        public void keyTyped(KeyEvent e) {
            if ((e.getModifiersEx() & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
  				addEvent(e);
            }        
        }
        
        static void addEvent(AWTEvent ev) {
            synchronized (eventQueue) {
                eventQueue.addLast(ev);
            }
        }

        public static AWTEvent nextEvent() {
        	AWTEvent ev;
            synchronized (eventQueue) {
                ev = (!eventQueue.isEmpty())?(AWTEvent)eventQueue.removeFirst():null;
            }
            return ev;
        }
        
     // modifications of eventQueue must be thread safe!
        private static LinkedList<AWTEvent> eventQueue = new LinkedList<AWTEvent>();

	@Override
	public void GetEvent() {
		AWTEvent X_event;
		MouseEvent MEV;
		event_t event=new event_t();;
		  // put event-grabbing stuff in here
		if (eventQueue.isEmpty()) return;	
		X_event=nextEvent();
		//System.out.println("Event type:"+X_event.getID());
			
		// Check for Event.??? shit
		if (!ignorebutton){
		  switch (X_event.getID())
		  {
		    case Event.KEY_PRESS:
			event=new event_t(evtype_t.ev_keydown, xlatekey((KeyEvent)X_event));
			DM.PostEvent(event);
			//System.err.println("k");
			break;
		    case Event.KEY_RELEASE:
			event=new event_t(evtype_t.ev_keyup, xlatekey((KeyEvent)X_event));
			DM.PostEvent(event);
			//System.err.println( "ku");
			break;
		  }
		}
		
		switch (X_event.getID()){
		    // ButtonPress
		    case Event.MOUSE_DOWN:
		    MEV=(MouseEvent)X_event;
			event.data1 =
			    (MEV.getButton() & MouseEvent.BUTTON1)
			    | (flags(MEV.getButton() , MouseEvent.BUTTON2) ? 2 : 0)
			    | (flags(MEV.getButton() , MouseEvent.BUTTON3) ? 4 : 0)
			    | (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0)
			    | (MEV.getButton() == MouseEvent.BUTTON2 ? 2 : 0)
			    | (MEV.getButton() == MouseEvent.BUTTON3 ? 4 : 0);
			event.data2 = event.data3 = 0;
			event.type=evtype_t.ev_mouse;
			DM.PostEvent(event);
			//System.err.println( "b");
			break;
			// ButtonRelease
		    case Event.MOUSE_UP:
		    MEV=(MouseEvent)X_event;
			event.type = evtype_t.ev_mouse;
			event.data1 =
			    (MEV.getButton() & MouseEvent.BUTTON1)
			    | (flags(MEV.getButton() , MouseEvent.BUTTON2) ? 2 : 0)
			    | (flags(MEV.getButton() , MouseEvent.BUTTON3) ? 4 : 0);
			// suggest parentheses around arithmetic in operand of |
			event.data1 =
			    event.data1
			    ^ (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0)
			    ^ (MEV.getButton() == MouseEvent.BUTTON2 ? 2 : 0)
			    ^ (MEV.getButton() == MouseEvent.BUTTON3 ? 4 : 0);
			event.data2 = event.data3 = 0;
			DM.PostEvent(event);
			//System.err.println("bu");
			break;
		    // MotionNotify:
		    case Event.MOUSE_MOVE:
		    case Event.MOUSE_DRAG:
		    MEV=(MouseEvent)X_event;
			event.type = evtype_t.ev_mouse;
			// Get buttons, as usual.
			event.data1 =
			    (MEV.getButton() & MouseEvent.BUTTON1)
			    | (flags(MEV.getButton() , MouseEvent.BUTTON2) ? 2 : 0)
			    | (flags(MEV.getButton() , MouseEvent.BUTTON3) ? 4 : 0);
			event.data2 = (MEV.getX() - lastmousex) << 2;
			event.data3 = (lastmousey - MEV.getY()) << 2;

			if ((event.data2 | event.data3)!=0)
			{
			    lastmousex = MEV.getX();
			    lastmousey = MEV.getY();
			    if (MEV.getX() != this.getWidth()/2 &&
			    		MEV.getY() != this.getHeight()/2)
			    {
				DM.PostEvent(event);
				//System.err.println( "m");
				mousemoved = false;
			    } else
			    {
				mousemoved = true;
			    }
			}
			break;
		    case Event.MOUSE_ENTER:
		    	System.err.println("ACCEPTING keyboard input");
		    	this.setCursor(hidden);
		    	ignorebutton=false;
		    	break;
		    case Event.MOUSE_EXIT:
		    	System.err.println("IGNORING keyboard input");
		    	this.setCursor(normal);
		    	ignorebutton=true;
		    	break;
		    case Event.WINDOW_EXPOSE:
		    	// No real single equivalent for "ConfigureNotify"
		    case Event.WINDOW_MOVED:
		    case Event.WINDOW_DESTROY:
			break;
		    default:
			// NOT NEEDED in AWT if (doShm && X_event.type == X_shmeventtype) shmFinished = true;
			break;

		}
		
	}
	
	  /** Returns true if flags are included in arg.
	   * Synonymous with (flags & arg)!=0
	   * 
	   * @param flags
	   * @param arg
	   * @return
	   */
	  public static final boolean flags(int flags, int arg){
	      return ((flags & arg)!=0);
	  }

	@Override
	public void mouseClicked(MouseEvent mouseevent) {
		addEvent(mouseevent);
		
	}

	@Override
	public void mouseEntered(MouseEvent mouseevent) {
		addEvent(mouseevent);
		
	}

	@Override
	public void mouseExited(MouseEvent mouseevent) {
		addEvent(mouseevent);
		
	}

	@Override
	public void mousePressed(MouseEvent mouseevent) {
		addEvent(mouseevent);
		
	}

	@Override
	public void mouseReleased(MouseEvent mouseevent) {
		addEvent(mouseevent);
		
	}

	@Override
	public void mouseDragged(MouseEvent mouseevent) {
		addEvent(mouseevent);
		
	}

	@Override
	public void mouseMoved(MouseEvent mouseevent) {
		addEvent(mouseevent);
		
	}
    
	/**
	* NASTY hack to hide the cursor.
	* 
	* Create a 'hidden' cursor by using a transparent image
	*	* ...return the invisible cursor
	*/

	protected Cursor createInvisibleCursor()

	{

	Dimension bestCursorDim = Toolkit.getDefaultToolkit().getBestCursorSize(2, 2);

	BufferedImage transparentImage = new BufferedImage(bestCursorDim.width, bestCursorDim.height, BufferedImage.TYPE_INT_ARGB);

	Cursor hiddenCursor = Toolkit.getDefaultToolkit( ).createCustomCursor(transparentImage,

	new Point(1, 1),

	"HiddenCursor");

	return hiddenCursor;
	}

	/**
	 * I_SetPalette
	 * 
	 * IMPORTANT: unlike an actual "on the fly" palette-shifting
	 * system, here we must pass an index to the actual palette 
	 * (0-31), as limitations of AWT prevent loading new palettes 
	 * on-the-fly without a significant overhead.
	 * 
	 * The actual palettes and fixed IndexColorModels are created 
	 * upon graphics initialization and cannot be changed (however
	 * they can be switched between), unless I switch to an explicit 
	 * INT_RGB canvas, which however has only 80% of a BYTE_INDEXED
	 * one.
	 *  
	 *  So, actually, we just switch to the proper BufferedImage
	 *  for display (the raster data is shared, which allows
	 *  this hack to work).
	 */
	
	@Override
	public void SetPalette (int palette)
	{
		   this.palette=palette;      
	}
	
	@Override
	public void InitGraphics()
	{

	  String		displayname;
	  String		d;
	  int			n;
	  int			pnum;
	  int			x=0;
	  int			y=0;
	  
	  // warning: char format, different type arg
	  int		xsign=' ';
	  int		ysign=' ';
	  
	  boolean			oktodraw;
	  long	attribmask;

	  //signal(SIGINT, (void (*)(int)) I_Quit);

	  if (DM.CheckParm("-2")!=0)
		multiply = 2;

	  if (DM.CheckParm("-3")!=0)
		multiply = 3;

	  if (DM.CheckParm("-4")!=0)
		multiply = 4;

      Dimension size = new Dimension();
      size.width = width * multiply;
      size.height = height * multiply;


	  // check for command-line display name
	  if ( (pnum=DM.CheckParm("-disp"))!=0 ) // suggest parentheses around assignment
		displayname = DM.myargv[pnum+1];
	  else
		displayname = null;

	  // check if the user wants to grab the mouse (quite unnice)
	  // HMM...double !!...so if the user WANTS, it will be !!true=!false=true :-S
	  grabMouse = DM.CheckParm("-grabmouse")!=0;

	  // check for command-line geometry
	  if ( (pnum=DM.CheckParm("-geom"))!=0 ) // suggest parentheses around assignment
	  {
		  try{
		  String eval=DM.myargv[pnum+1].trim();
		// warning: char format, different type arg 3,5
		//n = sscanf(myargv[pnum+1], "%c%d%c%d", &xsign, &x, &ysign, &y);
		// OK, so we have to read a string that may contain
		// ' '/'+'/'-' and a number. Twice.
		StringTokenizer tk=new StringTokenizer(eval,"-+ ");
		// Signs. Consider positive.
		xsign=1;ysign=1;
		for (int i=0;i<eval.length();i++){
			if (eval.charAt(i)=='-'){
				// First '-' on trimmed string: negagive
				if (i==0)
					xsign=-1;
				else 
					ysign=-1;
				}
			}
		
		//this should parse two numbers.
		if (tk.countTokens()==2){
			x=xsign*Integer.parseInt(tk.nextToken());
			y=ysign*Integer.parseInt(tk.nextToken());
		}
		  

		  } catch (NumberFormatException e){
		    I.Error("bad -geom parameter");
		  }
	  }

	  // open the display
	  // AWT: create the canvas.
	  try{
      drawhere=new Canvas();
      drawhere.setSize(size);
      drawhere.setBounds(0, 0, drawhere.getWidth(),drawhere.getHeight());
      drawhere.setBackground(Color.black);
      
      // AWT: Add canvas to component.
      this.add(drawhere);
      
      // AWT: Add listeners.
      drawhere.addKeyListener(this);
      drawhere.addMouseListener(this);
      drawhere.addMouseMotionListener(this);
      
      // AWT: create cursors.
      this.normal=this.getCursor();
      this.hidden=this.createInvisibleCursor();
      
      //AWT: Make visible and "peg" the g2d Object to the Canvas,
      // else it will be nullified.
      this.setVisible(true);
      g2d = (Graphics2D)drawhere.getGraphics();
	  } catch (Exception e){
		  I.Error("Error creating AWTDoom frame. Exiting.");
	  }


	  // create the colormaps
	  
	  if (cmap!=null|| cmap.length<768) {
		  // As many as are likely contained
		  maxpalettes=cmap.length/768;
		  System.out.println(maxpalettes + " palettes read");
		  cmaps=new IndexColorModel[maxpalettes];
	
		  // Now we have our palettes.
	    for (int i=0;i<maxpalettes;i++){
	     cmaps[i]=new IndexColorModel(8, 256,cmap, i*768, false);
	    }
	  } else {
		  // Allow it to pull from some default location?
		  System.err.println("Palletes could not be set up. Bye");
		  System.exit(-1);
	  }
	    
	 /* this.s  pals=V.getBufferedScreens(0, icms);
	  cmaps = XCreateColormap(X_display, RootWindow(X_display,
							   X_screen), X_visual, AllocAll); */


	  // create the main window
	  this.setBounds(x, y, width, height);
      this.setSize(size);
	  this.setResizable(false);
	    this.setTitle("MochaDoom");
	    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  //this.setDefaultLookAndFeelDecorated(false);
	  
	  // wait until it is OK to draw

	  /*oktodraw = false;
	  while (!oktodraw)
	  {
		AWTevent=nextEvent();
		if (AWTevent!=null && AWTevent.getID() == Event.WINDOW_EXPOSE
		    && ((WindowEvent)(AWTevent)).getID()!=0)
		{
		    oktodraw = true;
		}
	  } */

	  // grabs the pointer so it is restricted to this window
	  // Err?
	  /*if (grabMouse)
		XGrabPointer(X_display, X_mainWindow, True,
			     ButtonPressMask|ButtonReleaseMask|PointerMotionMask,
			     GrabModeAsync, GrabModeAsync,
			     X_mainWindow, None, CurrentTime);*/

	 
	  
	 /*  Not bothering with this yet.
	  * if (multiply == 1){
		  
		screens[0] = (unsigned char *) (image->data);
	  else
		screens[0] = (unsigned char *) malloc (SCREENWIDTH * SCREENHEIGHT);*/

	  screens=V.getBufferedScreens(0, cmaps);
	} 

	@Override
	public void StartFrame() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void StartTic() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void FinishUpdate() {
		this.update(null);
		
	}
	

}

