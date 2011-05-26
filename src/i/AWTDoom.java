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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import v.BufferedRenderer;
import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;

/** A simple Doom display & keyboard driver for AWT.
 *  Uses a Canvas for painting and implements some
 *  of the IVideo methods.
 * 
 *  Uses 8-bit buffered images and switchable IndexColorModels.
 *  
 *  It's really basic, but allows testing and user interaction.
 *  Heavily based on the original LinuxDoom X11 implementation, and
 *  is similar in goals: just a functional, reference implementation
 *  to build upon whatever fancy extensions you like.
 *  
 *  The only "hitch" is that this implementation expects to be 
 *  initialized upon a BufferedRenderer with multiple images per 
 *  screen buffer in order to perform the palette switching trick.
 *  
 *  The images themselves don't have to be "BufferedImage",
 *  and in theory it could be possible to use e.g. MemoryImageSource
 *  for the same purpose . Oh well.
 *    
 *  
 * 
 * @author Velktron
 *
 */
public class AWTDoom extends JFrame implements WindowListener,KeyEventDispatcher,KeyListener,MouseListener,MouseMotionListener, DoomVideoInterface,DoomEventInterface{


    private static final long serialVersionUID = 3118508722502652276L;

    private static final int POINTER_WARP_COUNTDOWN = 1;
	
	// Must be aware of "Doom" so we can pass it event messages inside a crude queue.
	public DoomMain DM;
	private IDoomSystem I; // Must be aware of some other shit like event handler
	private BufferedRenderer V;   // Must be able to "peg" itself to a raw byte array representing
									// Screen 0
	private byte[] RAWSCREEN;	  // RAW SCREEN DATA. Get from the Video Renderer.
	private IndexColorModel[] cmaps;
	  Robot robby;
		Canvas drawhere;
		/** These are the actual screens */
        Image[] screens;
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

		protected boolean grabMouse=true;
		protected AWTEvent AWTevent;
		protected byte[] cmap;
		private int maxpalettes;
		private int SCREENHEIGHT;
		private int SCREENWIDTH;

        private int doPointerWarp;

		/** Gimme some raw palette RGB data.
		 *  I will do the rest
		 *  
		 *  (hint: read this from the PLAYPAL
		 *   lump in the IWAD!!!).
		 * 
		 */
     
        public AWTDoom(DoomMain DM, BufferedRenderer V, byte[] cmap) {
        	this.DM=DM;
        	this.I=DM.I;
        	this.V= V;
        	this.cmap=cmap;
        	this.width=V.getWidth();
        	this.height=V.getHeight();
        	this.center=new Point (width/2, height/2);
        	this.rect=new Rectangle((int)(width/10),(int)(0.1*height/10),9*width/10,9*height/10);
        // Don't do anything yet until InitGraphics is called.
        }
        
        Point center;
        Rectangle rect;
        public void setPalette(int pal){
            this.palette=pal;            
        }
        
        /** Modified update method: no context needs to passed.
         *  Will render only internal screens.
         * 
         */
        public void update(Graphics g) {
           // Techdemo v1.3: Mac OSX fix, compatible with Windows and Linux.
           // Should probably run just once. Overhead is minimal
           // compared to actually DRAWING the stuff.
           if (g2d==null) g2d = (Graphics2D)drawhere.getGraphics();
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
        
        
        /** FIXME: input must be made scancode dependent rather than VK_Dependent,
         *  else a lot of things just don't work. 
         * 
         * @param e
         * @return
         */

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
            case KeyEvent.VK_CONTROL: rc=KEY_RCTRL; break;
            case KeyEvent.VK_ALT: rc=KEY_RALT; break;
            case KeyEvent.VK_SHIFT: rc=KEY_RSHIFT; break;
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
            
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:  rc = KEY_BACKSPACE; break;

            case KeyEvent.VK_PAUSE:   rc = KEY_PAUSE;     break;

            case KeyEvent.KEY_RELEASED:
            case KeyEvent.VK_TAB: rc = KEY_TAB;       break;
            
            case KeyEvent.KEY_PRESSED:
            	switch(e.getKeyCode()){
            	
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS: 
                rc = KEY_EQUALS;    
                break;
            
            case (13):
            case KeyEvent.VK_SUBTRACT: 
            case KeyEvent.VK_MINUS:   
                rc = KEY_MINUS;     
                break;

            case KeyEvent.VK_SHIFT:
            rc = KEY_RSHIFT;
            break;
            
            case KeyEvent.VK_CONTROL:
            rc = KEY_RCTRL;
            break;                           
           
            case KeyEvent.VK_ALT:
            rc = KEY_RALT;
            break;

            case KeyEvent.VK_SPACE:
                rc = ' ';
                break;

                }
                
            default:
                
           /*if (rc >= KeyEvent.VK_SPACE && rc <= KeyEvent.VK_DEAD_TILDE)
                {
            	rc = (int) (rc - KeyEvent.FOCUS_EVENT_MASK + ' ');
                break;
                } */
            if (rc >= KeyEvent.VK_A && rc <= KeyEvent.VK_Z){
                rc = rc-KeyEvent.VK_A +'a';
            	break;
            	}
            // Unknown. Probably fucking up with the keyboard locale. Switch to be sure.
            // Sorry for this horrible hack, but Java can't read locale-free keycodes -_-
            // this.getInputContext().selectInputMethod(java.util.Locale.US);
            //if (rc>KEY_F12) rc=KEY_RSHIFT;
            break;
          }
          
         //System.out.println("Typed "+e.getKeyCode()+" char "+e.getKeyChar()+" mapped to "+Integer.toHexString(rc));

          // Sanitize. Nothing beyond F12 must pass through, else you will
          // get the "all cheats" bug.
          return rc;//Math.min(rc,KEY_F12);

        }
        
        // This stuff should NOT get through in keyboard events.
        protected final int UNACCEPTABLE_MODIFIERS=(int) (InputEvent.ALT_GRAPH_DOWN_MASK+
        										 InputEvent.META_DOWN_MASK+
        										 InputEvent.META_MASK+
        										 InputEvent.WINDOW_EVENT_MASK+
        										 InputEvent.WINDOW_FOCUS_EVENT_MASK);

        public void keyPressed(KeyEvent e) {
 
            e.consume();
        	if (e.getKeyCode()<=KeyEvent.VK_F12) {  
        	addEvent(e);        	
            }
        }

        public void keyReleased(KeyEvent e) {
            e.consume();
            
        	//if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0) {
        		if (e.getKeyCode()<=KeyEvent.VK_F12) {
        		addEvent(e);
            }
        }

        public void keyTyped(KeyEvent e) {
            e.consume();

            if (e.getKeyCode()<=KeyEvent.VK_F12) {
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

        // This event here is static, and may be overwritten by others.
        // However, when sending out messages, we create COPIES of it.
        event_t event=new event_t();
        
	@Override
	public void GetEvent() {
		AWTEvent X_event;
		MouseEvent MEV;
		
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
			    (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0) |
			    (MEV.getButton() == MouseEvent.BUTTON2 ? 2: 0)|
			    (MEV.getButton() == MouseEvent.BUTTON3 ? 4: 0);
			event.data2 = event.data3 = 0;
			event.type=evtype_t.ev_mouse;
			DM.PostEvent(event);
			//System.err.println( "b");
			break;
			
			// ButtonRelease
			// This must send out an amended event.
			
		    case Event.MOUSE_UP:
		    MEV=(MouseEvent)X_event;
			event.type = evtype_t.ev_mouse;
			event.data1 ^= 
		        (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0) |
                (MEV.getButton() == MouseEvent.BUTTON2 ? 2: 0)|
                (MEV.getButton() == MouseEvent.BUTTON3 ? 4: 0);
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
			    | (flags(MEV.getButton() , MouseEvent.BUTTON3) ? 2 : 0)
			    | (flags(MEV.getButton() , MouseEvent.BUTTON2) ? 4 : 0);
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
		    	//System.err.println("ACCEPTING keyboard input");
		    	this.setCursor(hidden);
		    	ignorebutton=false;
		    	break;
		    case Event.MOUSE_EXIT:
		    	//System.err.println("IGNORING keyboard input");
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
	* ...return the invisible cursor
	*/

	protected Cursor createInvisibleCursor()

	{
	Dimension bestCursorDim = Toolkit.getDefaultToolkit().getBestCursorSize(2, 2);
	BufferedImage transparentImage = new BufferedImage(bestCursorDim.width, bestCursorDim.height, BufferedImage.TYPE_INT_ARGB);
	Cursor hiddenCursor = Toolkit.getDefaultToolkit( ).createCustomCursor(transparentImage,	new Point(1, 1),	"HiddenCursor");
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
	 * INT_RGB canvas, which however has only 80% performence 
	 * of a BYTE_INDEXED one.
	 *  
	 *  So, actually, we just switch to the proper BufferedImage
	 *  for display (the raster data is shared, which allows
	 *  this hack to work with minimal overhead).
	 *  
	 *  TODO: implement GAMMA in this way?
	 *  
	 */
	
	@Override
	public void SetPalette (int palette)
	{
		   this.palette=palette;      
	}
	
	
	/** Call this before attempting to draw anything.
	 * This will create the window, the canvas and everything.
	 * Unlike a simple JFrame, this is not automatic because of the order
	 * Doom does things.
	 * 
	 */
	
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

	  // Try setting the locale the US, otherwise there will be problems
	  // with non-US keyboards.
	  if (!this.getInputContext().selectInputMethod(java.util.Locale.US)){
		  System.err.println("Could not set the input context to US! Kayboard input will be glitchy!");
	  } else {
		  System.err.println("Input context successfully set to US.");
	  }
	  
	  //signal(SIGINT, (void (*)(int)) I_Quit);

	  if (DM.CheckParm("-2")!=0)
		multiply = 2;

	  if (DM.CheckParm("-3")!=0)
		multiply = 3;

	  if (DM.CheckParm("-4")!=0)
		multiply = 4;

      Dimension size = new Dimension();
      size.width = this.width * multiply;
      size.height = height * multiply;

      // Create AWT Robot for forcing mouse
      try {
      robby=new Robot();
      } catch (Exception e){
    	  System.out.println("AWT Mouse Robot could not be created, mouse input focus will be loose!");
      }
      
	  // check for command-line display name
	  if ( (pnum=DM.CheckParm("-disp"))!=0 ) // suggest parentheses around assignment
		displayname = DM.myargv[pnum+1];
	  else
		displayname = null;

	  // check if the user wants to grab the mouse (quite unnice)
	  // HMM...double !!...so if the user WANTS, it will be !!true=!false=true :-S
	  grabMouse = true; // DM.CheckParm("-grabmouse")!=0;

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
      drawhere.setPreferredSize(size);
      drawhere.setBounds(0, 0, drawhere.getWidth()-1,drawhere.getHeight()-1);
      drawhere.setBackground(Color.black);
      
      // AWT: Add listeners.
      drawhere.addKeyListener(this);
      drawhere.addMouseListener(this);
      drawhere.addMouseMotionListener(this);
      
      final Component me=this;
      
      // AWT: tab is a special case :-/
      KeyboardFocusManager.
      getCurrentKeyboardFocusManager().
      addKeyEventDispatcher(new KeyEventDispatcher() {  
          public boolean dispatchKeyEvent(KeyEvent e) {    
            if (e.getKeyCode() == KeyEvent.VK_TAB) {      
                addEvent(new KeyEvent(me, e.getID(), System.nanoTime(),0 , KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));

            }  
            return false;
          }
      });
      
      // MAC OS X fix: let this be null, HERE, but set it to context
      // inside paint(). Should be Windows and Linux friendly, as well.
       // g2d = (Graphics2D)drawhere.getGraphics();
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
		  System.err.println("Palettes could not be set up. Bye");
		  System.exit(-1);
	  }
	    
	 /* this.s  pals=V.getBufferedScreens(0, icms);
	  cmaps = XCreateColormap(X_display, RootWindow(X_display,
							   X_screen), X_visual, AllocAll); */


	  // create the main window	  
      //AWT: Make visible and "peg" the g2d Object to the Canvas,
      // else it will be nullified.
	  
	  //this.setSize(0, 0);
      //this.setVisible(true);
      //System.out.println("Null size "+this.getSize());
      //System.out.println("Insets top "+this.getInsets().top);
      //System.out.println("Insets bottom "+this.getInsets().bottom);
      // THe ACTUAL size to set is a bit tricky, and can only
      // be determined after the window has actually been set,
      // and OS-dependent stuff like borders, title bar etc. have been drawn.
      // Jesus -_-
      
     // Dimension truesize=this.getSize();
   //   System.out.println("Desired size "+size);
	 // this.setSize(size.width+truesize.width,size.height+truesize.height);
	//  System.out.println("True size "+this.getSize());
	  this.add(drawhere);
	  this.getContentPane().setPreferredSize(drawhere.getPreferredSize());
	  this.pack();
	  this.setVisible(true);
	  
	  //this.width=this.getSize().width;
	  //this.height=this.getSize().height;
      //this.setBounds(x, y, width, height);
      this.setResizable(false);
	    this.setTitle(Strings.MOCHA_DOOM_TITLE);
	    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	      // AWT: create cursors.
	      this.normal=this.getCursor();
	      this.hidden=this.createInvisibleCursor();
	      
	      
	      // AWT: Add canvas to component.
	      
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
	  RAWSCREEN=V.getScreen(0);
	  SCREENHEIGHT=V.getHeight();
	  SCREENWIDTH=V.getWidth();
	} 

	@Override
	public void StartFrame() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void StartTic() {

		  if (!this.isActive())
			return;

		//  System.out.println("Getting events...");
		  while (!eventQueue.isEmpty())
			GetEvent();

		  // Warp the pointer back to the middle of the window
		  //  or it will wander off - that is, the game will
		  //  loose input focus within X11.
		  if (grabMouse)
		  {
		      if (doPointerWarp--<=0)
		  {
		      // Don't warp back if we deliberately alt-tabbed away.
			  Point p=this.getMousePosition();
			  if (p!=null){
		      robby.mouseMove(this.getX()+this.getWidth()/2, this.getY()+this.getHeight()/2);
			  //lastmousex=this.getX()+this.getWidth()/2;
			  //lastmousey=this.getY()+this.getHeight()/2;
			  }
			  doPointerWarp = POINTER_WARP_COUNTDOWN;
		  } 
		      
	    }

		 mousemoved = false;
		
	}

	private int lasttic;
	private int frames;

	@Override
	public void FinishUpdate() {
	    int		tics;
	    int		i;
	    
	    // draws little dots on the bottom of the screen
	    /*if (true)
	    {

		i = I.GetTime();
		tics = i - lasttic;
		lasttic = i;
		if (tics > 20) tics = 20;
		if (tics < 1) tics = 1;

		for (i=0 ; i<tics*2 ; i+=2)
		    RAWSCREEN[ (SCREENHEIGHT-1)*SCREENWIDTH + i] = (byte) 0xff;
		for ( ; i<20*2 ; i+=2)
			RAWSCREEN[ (SCREENHEIGHT-1)*SCREENWIDTH + i] = 0x0;
	    
	    } */

	    if (true)
        {

        i = I.GetTime();
        tics = i - lasttic;
        lasttic = i;
        if (tics<1) 
            frames++;
        else
        {
        //frames*=35;
        for (i=0 ; i<frames*2 ; i+=2)
            RAWSCREEN[ (SCREENHEIGHT-1)*SCREENWIDTH + i] = (byte) 0xff;
        for ( ; i<20*2 ; i+=2)
            RAWSCREEN[ (SCREENHEIGHT-1)*SCREENWIDTH + i] = 0x0;
        frames=0;
        }
        }

	    this.update(null);
	    //this.getInputContext().selectInputMethod(java.util.Locale.US);
		
	}


	
	
    @Override
    public void UpdateNoBlit() {
        //this.update(null);
        
    }
    
	@Override
    public boolean dispatchKeyEvent(KeyEvent e) {
	    
	return false;
	}

	@Override
	public void windowActivated(WindowEvent windowevent) {
	    System.out.println("Window activated");
		this.getInputContext().selectInputMethod(java.util.Locale.US);
		
	}
	
	@Override
	public void windowClosed(WindowEvent windowevent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent windowevent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent windowevent) {
	    // Clear the queue if focus is lost.
	    System.out.println("Eventqueue flushed!");
		eventQueue.clear();
		
	}

	@Override
	public void windowDeiconified(WindowEvent windowevent) {
		this.getInputContext().selectInputMethod(java.util.Locale.US);
		
	}

	@Override
	public void windowIconified(WindowEvent windowevent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent windowevent) {
		this.getInputContext().selectInputMethod(java.util.Locale.US);
		
	}

}

