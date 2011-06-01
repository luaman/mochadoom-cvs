package awt;

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
import i.DoomEventInterface;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;

/** An alternate events class, more suited for handling the complex situations
 *  that might arise during daily use.
 * 
 * @author vepitrop
 *
 */

public class MochaEvents implements WindowListener,ComponentListener,KeyEventDispatcher,KeyListener,MouseListener,MouseMotionListener,WindowFocusListener,DoomEventInterface {

    // modifications of eventQueue must be thread safe!
    private LinkedList<MochaDoomInputEvent> eventQueue = new LinkedList<MochaDoomInputEvent>();

    //// STATUS STUFF ///////////
    public final DoomMain DM;
    public final Component canvas;
    Robot robby;
    Cursor hidden;
    Cursor normal;

    //////// CURRENT MOVEMENT AND INPUT STATUS //////////////

    protected static final int POINTER_WARP_COUNTDOWN = 1;
    protected int lastmousex;
    protected int lastmousey;
    protected Point lastmouse;
    protected int mousedx;
    protected int mousedy;
    protected boolean mousemoved;
    protected boolean ignorebutton;
    protected boolean grabMouse=true;
    protected int doPointerWarp=POINTER_WARP_COUNTDOWN;


    public MochaEvents(DoomMain DM, Component canvas) {
        this.DM = DM;
        this.canvas = canvas;

        // AWT: create cursors.
        this.normal=canvas.getCursor();
        this.hidden=this.createInvisibleCursor();

        // Create AWT Robot for forcing mouse
        try {
            robby=new Robot();
        } catch (Exception e){
            System.out.println("AWT Robot could not be created, mouse input focus will be loose!");
        }
        
    }

    
    ///////////////////// QUEUE HANDLING ///////////////////////

    public synchronized void addEvent(MochaDoomInputEvent ev) {
        synchronized (eventQueue) {
            eventQueue.addLast(ev);
        }
    }

    public MochaDoomInputEvent nextEvent() {
    	MochaDoomInputEvent ev;
        synchronized (eventQueue) {
            ev = (!eventQueue.isEmpty())?(MochaDoomInputEvent)eventQueue.removeFirst():null;
        }
        return ev;
    }

    // This event here is used as a static scratch copy. When sending out
    // messages, its contents are to be actually copied (struct-like).
    // This avoids the continuous object creation/destruction overhead,
    // And it also allows creating "sticky" status.

    final event_t event=new event_t();
    // Special FORCED and PAINFUL key and mouse cancel event.
    final event_t cancelkey=new event_t(evtype_t.ev_clear,0xFF,0,0);
    final event_t cancelmouse=new event_t(evtype_t.ev_mouse,0,0,0);
    int prevmousebuttons;
    
    @Override
    public void GetEvent() {
        MochaDoomInputEvent X_event;
        MouseEvent MEV;
        Point tmp;
        // put event-grabbing stuff in here
        if (eventQueue.isEmpty()) return;   
        X_event=nextEvent();
        //System.out.println("Event type:"+X_event.getID());

        // Keyboard events get priority vs mouse events.
        // In the case of combined input, however, we need
        if (!ignorebutton){
            switch (X_event.type)
            {
            case MochaDoomInputEvent.KeyPress: {
                event.type=evtype_t.ev_keydown;
                event.data1=xlatekey((KeyEvent)X_event.ev);
                DM.PostEvent(event);
                if (prevmousebuttons!=0){
                    
                // Allow combined mouse/keyboard events.
                event.data1=prevmousebuttons;
                event.type=evtype_t.ev_mouse;
                DM.PostEvent(event);
                }
                //System.err.println("k");
                break;
            }
            
            case MochaDoomInputEvent.KeyRelease:
                event.type=evtype_t.ev_keyup;
                event.data1=xlatekey((KeyEvent)X_event.ev);
                DM.PostEvent(event);
                
                if (prevmousebuttons!=0){
                    
                    // Allow combined mouse/keyboard events.
                    event.data1=prevmousebuttons;
                    event.type=evtype_t.ev_mouse;
                    DM.PostEvent(event);
                    }
                //System.err.println( "ku");
                break;
            
        case MochaDoomInputEvent.KeyType:
            event.type=evtype_t.ev_keyup;
            event.data1=xlatekey((KeyEvent)X_event.ev);
            DM.PostEvent(event);
            
            if (prevmousebuttons!=0){
                
                // Allow combined mouse/keyboard events.
                event.data1=prevmousebuttons;
                event.type=evtype_t.ev_mouse;
                DM.PostEvent(event);
                }
            //System.err.println( "ku");
            break;
        }
        }

        // Ignore ALL mouse events if we are moving the window.
        // Mouse events are also handled, but with secondary priority.
        switch (X_event.type){
        // ButtonPress
        case MochaDoomInputEvent.ButtonPress:
            MEV=(MouseEvent)X_event.ev;
            event.type=evtype_t.ev_mouse;
            event.data1 = prevmousebuttons=
                (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0) |
                (MEV.getButton() == MouseEvent.BUTTON2 ? 2: 0)|
                (MEV.getButton() == MouseEvent.BUTTON3 ? 4: 0);
            event.data2 = event.data3 = 0;

            DM.PostEvent(event);
            //System.err.println( "b");
            break;

            // ButtonRelease
            // This must send out an amended event.

        case MochaDoomInputEvent.ButtonRelease:
            MEV=(MouseEvent)X_event.ev;
            event.type = evtype_t.ev_mouse;
            event.data1 =prevmousebuttons^= 
                (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0) |
                (MEV.getButton() == MouseEvent.BUTTON2 ? 2: 0)|
                (MEV.getButton() == MouseEvent.BUTTON3 ? 4: 0);
            // A PURE mouse up event has no movement.
            event.data2 = event.data3 = 0;
            DM.PostEvent(event);
            //System.err.println("bu");
            break;
        case MochaDoomInputEvent.MotionNotify:
            MEV=(MouseEvent)X_event.ev;
            tmp=MEV.getPoint();
            //this.AddPoint(tmp,center);
            event.type = evtype_t.ev_mouse;
            this.mousedx=(tmp.x-win_w2);
            this.mousedy=(win_h2-tmp.y);
             
            // A pure move has no buttons.
            event.data1=prevmousebuttons=0;
            event.data2 = (mousedx) << 2;
            event.data3 = (mousedy) << 2;

           // System.out.printf("Mouse MOVED to %d %d\n", lastmousex, lastmousey);
            //System.out.println("Mouse moved without buttons: "+event.data1);
            if ((event.data2 | event.data3)!=0)
            {

                DM.PostEvent(event);
                //System.err.println( "m");
                mousemoved = true;
            } else
            {
                mousemoved = false;
            }
            break;

        case MochaDoomInputEvent.DragNotify:
            MEV=(MouseEvent)X_event.ev;
            tmp=MEV.getPoint();
            this.mousedx=(tmp.x-win_w2);
            this.mousedy=(win_h2-tmp.y);
            event.type = evtype_t.ev_mouse;
            
            // A drag means no change in button state.
            event.data1 = prevmousebuttons;
            event.data2 = (mousedx) << 2;
            event.data3 = (mousedy) << 2;

            if ((event.data2 | event.data3)!=0)
            {
                DM.PostEvent(event);
                //System.err.println( "m");
                mousemoved = true;
            } else
            {
                mousemoved = false;
            }
            break;

        }
        
        // Now for window events. This includes the mouse breaking the border.
        
        switch (X_event.type){
        case MochaDoomInputEvent.FocusLost:
        case MochaDoomInputEvent.MouseExited:
            // Forcibly clear events                 
            DM.PostEvent(cancelmouse);
            DM.PostEvent(cancelkey);
            canvas.setCursor(normal);             
            ignorebutton=true;
            break;
            
        case MochaDoomInputEvent.WindowMoving:
        	// Don't try to reposition immediately during a move
        	// event, wait for a mouse click.
        	we_are_moving=true;
        	ignorebutton=true;
            // Forcibly clear events                 
            DM.PostEvent(cancelmouse);
            DM.PostEvent(cancelkey);
        	move++;
        	break;
        case MochaDoomInputEvent.MouseEntered:
        case MochaDoomInputEvent.FocusGained:
        	we_are_moving=false;
        	reposition();
        case MochaDoomInputEvent.ConfigureNotify:
        case MochaDoomInputEvent.CreateNotify:
            // All events that have to do with the window being changed,
        	// moved etc. should go here. The most often result
        	// in focus being lost and position being changed, so we
        	// need to take charge.
            canvas.requestFocus();
            reposition();            			
        	ignorebutton=false;
			break;
        default:
            // NOT NEEDED in AWT if (doShm && X_event.type == X_shmeventtype) shmFinished = true;
            break;

        }
        
        
        // If the mouse moved, don't wait until it managed to get out of the 
        // window to bring it back.
		if (!we_are_moving && (mousedx != 0 || mousedy != 0)) {
			// move the mouse to the window center again
			robby.mouseMove(offset.x + win_w2, offset.y + win_h2);
			
		}
		
		mousedx=mousedy=0; // don't spaz.

    }
    
    private int  win_w2, win_h2;
    private int move=0;

	/** Update relative position offset, and force mouse there.
	 * 
	 */
	void reposition() {
		offset.x=(int) (canvas.getLocationOnScreen().x);
    	offset.y=(int) (canvas.getLocationOnScreen().y);
    	// Shamelessly ripped from Jake 2. Maybe it works better?
		Component c = this.canvas;
		//offset.x = 0;
		//offset.y = 0;
		win_w2 = c.getWidth() / 2;
		win_h2 = c.getHeight() / 2;
		
		robby.mouseMove(offset.x + win_w2, offset.y + win_h2);

        
        canvas.getInputContext().selectInputMethod(java.util.Locale.US);
        canvas.setCursor(hidden);
		//System.out.printf("Jake 2 method: offset MOVED to %d %d\n", offset.x, offset.y);
		
	}
    
    Point offset=new Point();

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


    /////////////////////    WINDOW STUFF //////////////////////


    @Override
    public void windowActivated(WindowEvent windowevent) {
    	System.out.println("Window activated");
    	eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.ConfigureNotify, null));
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
    	eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.ConfigureNotify, null));

    }

    @Override
    public void windowIconified(WindowEvent windowevent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowOpened(WindowEvent windowevent) {
    	eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.CreateNotify, null));
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
        Cursor hiddenCursor = Toolkit.getDefaultToolkit( ).createCustomCursor(transparentImage, new Point(1, 1),    "HiddenCursor");
        return hiddenCursor;
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
            break;
        }

        //System.out.println("Typed "+e.getKeyCode()+" char "+e.getKeyChar()+" mapped to "+Integer.toHexString(rc));
        return rc;//Math.min(rc,KEY_F12);

    }

    protected void AddPoint(Point A, Point B){
    	A.x+=B.x;
    	A.y+=B.y;
    }

    ////////////LISTENERS //////////

    
    ///////////////////////// KEYBOARD EVENTS ///////////////////////////////////


    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode()<=KeyEvent.VK_F12) {  
            addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.KeyPress,e));            
        }

        e.consume();
    }

    public void keyReleased(KeyEvent e) {

        //if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0) {
        if (e.getKeyCode()<=KeyEvent.VK_F12) {
        	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.KeyRelease,e));
        }
        e.consume();
    }

    public void keyTyped(KeyEvent e) {
        if (e.getKeyCode()<=KeyEvent.VK_F12) {
        	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.KeyType,e));
       }

        e.consume();        
    }

    //////////////////////////// MOUSE EVENTS   ////////////////////////////

    @Override
    public void mouseClicked(MouseEvent mouseevent) {
    	// Marks the end of a move. A press + release during a move will
    	// trigger a "click" event, which is handled specially.
    	if (we_are_moving) {
    		we_are_moving=false;
    		reposition();
    		ignorebutton=false;
    	}
    
    	}

    @Override
    public void mouseEntered(MouseEvent mouseevent) {
    	//System.out.println("Mouse entered");
    	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.MouseEntered,null));
    	}

    @Override
    public void mouseExited(MouseEvent mouseevent) {
    	//System.out.println("Mouse exited");
    	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.MouseExited,null));
    	}

    @Override
    public void mousePressed(MouseEvent mouseevent) {
    	if (!we_are_moving) // Don't let presses go through when moving.
    	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.ButtonPress,mouseevent));
    	}

    @Override
    public void mouseReleased(MouseEvent mouseevent) {
    	if (!we_are_moving) // Don't let presses go through when moving.
    	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.ButtonRelease,mouseevent));
    	}

    @Override
    public void mouseDragged(MouseEvent mouseevent) {
    	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.DragNotify,mouseevent));
    	}

    @Override
    public void mouseMoved(MouseEvent mouseevent) {
    	addEvent(new MochaDoomInputEvent(MochaDoomInputEvent.MotionNotify,mouseevent));
    	}

    //////////////// COMPONENT EVENTS //////////////////////////
    
    boolean we_are_moving=false;
        
	@Override
	public void componentHidden(ComponentEvent e) {
		// Do what, here? Pausing would be a good idea.
		
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.WindowMoving, null));
	}

	@Override
	public void componentResized(ComponentEvent e) {
		eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.ConfigureNotify, null));
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
		eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.CreateNotify, null));
	}

	@Override
	public void windowGainedFocus(WindowEvent arg0) {
		eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.FocusGained, null));
	}


	@Override
	public void windowLostFocus(WindowEvent arg0) {
		eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.FocusLost, null));
	}


	public  boolean hasMoreEvents() {
		return !this.eventQueue.isEmpty();
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		//if (e.getKeyCode() == KeyEvent.VK_TAB)
		//	eventQueue.add(new MochaDoomInputEvent(MochaDoomInputEvent.KeyRelease, e));
		return false;
	}
    
}