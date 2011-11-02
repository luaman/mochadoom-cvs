package awt;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import i.DoomVideoInterface;
import i.IDoomSystem;
import i.InputListener;
import i.Strings;
import timing.ITicker;
import v.DoomVideoRenderer;
import doom.DoomMain;
import doom.ICommandLineManager;
import doom.event_t;

/** Common code for Doom's video frames */

public abstract class DoomFrame<K> extends JFrame implements DoomVideoInterface {

    public DoomFrame(DoomMain DM,DoomVideoRenderer<K> V) {
    	this.DM=DM;
    	this.CM=DM.CM;
    	this.TICK=DM.TICK;
    	this.I=DM.I;
    	this.V= V;
        // If these aren't set here, the init code won't size up windows properly.
    	// Of course, we must have made up our mind about the resolution
    	// already, at this point (TODO: command line parameters and multiplication
    	// should be handled BEFORE reaching this point).
    	this.width=V.getWidth();
    	this.height=V.getHeight();
    	this.center=new Point (width/2, height/2);
    	this.rect=new Rectangle((int)(width/10),(int)(0.1*height/10),9*width/10,9*height/10);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -4130528877723831825L;

	

	protected static final boolean D=false;
	
	// Must be aware of "Doom" so we can pass it event messages inside a crude queue.
	public DoomMain DM;            // Must be aware of general status.
	public ICommandLineManager CM; // Must be aware of command line interface.
	protected IDoomSystem I;         // Must be aware of some other shit like event handler
	protected DoomVideoRenderer<K> V;    // Must have a video renderer....
	protected ITicker TICK;          // Must be aware of the ticker/
	protected K RAWSCREEN;	   // RAW SCREEN DATA. Get from the Video Renderer.
	protected MochaEvents eventhandler; // Separate event handler a la _D_.
	                               // However I won't make it fully "eternity like" yet
	                               // also because it works quite flakey on Linux.
	

  	protected Robot robby;
	protected Canvas drawhere;
	/** This is the actual screen */
    protected Image screen;
    protected int palette=0;
    protected Dimension size;
    //InputListener in;
    protected Graphics2D g2d;
    
    protected Point center;
    protected Rectangle rect;
    
    /** Dimensions of the screen buffers */
    protected int width,height;
    protected int multiply=1;
    
    // This stuff should NOT get through in keyboard events.
    protected final int UNACCEPTABLE_MODIFIERS=(int) (InputEvent.ALT_GRAPH_DOWN_MASK+
    										 InputEvent.META_DOWN_MASK+
    										 InputEvent.META_MASK+
    										 InputEvent.WINDOW_EVENT_MASK+
    										 InputEvent.WINDOW_FOCUS_EVENT_MASK);
    
    public String processEvents(){
        StringBuffer tmp=new StringBuffer();
        event_t event;
        while ( (event=InputListener.nextEvent()) != null ) {
            tmp.append(event.type.ordinal()+"\n");
        }
        return tmp.toString();
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
	 *  
	 */
	
	@Override
	public void SetPalette (int palette)
	{
		V.setPalette(palette);
		this.screen=V.getCurrentScreen();      
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
	  if (this.getInputContext()==null || !this.getInputContext().selectInputMethod(java.util.Locale.US)){
		  System.err.println("Could not set the input context to US! Keyboard input will be glitchy!");
	  } else {
		  System.err.println("Input context successfully set to US.");
	  }

      Dimension size = new Dimension();
      size.width = this.width * multiply;
      size.height = height * multiply;
      
	  // check for command-line display name
	  if ( (pnum=CM.CheckParm("-disp"))!=0 ) // suggest parentheses around assignment
		displayname = CM.getArgv(pnum+1);
	  else
		displayname = null;

	  // check for command-line geometry
	  if ( (pnum=CM.CheckParm("-geom"))!=0 ) // suggest parentheses around assignment
	  {
		  try{
		  String eval=CM.getArgv(pnum+1).trim();
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
      //drawhere=new Canvas();
      // MAES: this method works even on "stubborn" Linux distros that 
      // fuck up the window size.
      drawhere.setPreferredSize(size);
      drawhere.setBounds(0, 0, drawhere.getWidth()-1,drawhere.getHeight()-1);
      drawhere.setBackground(Color.black);
      
      this.eventhandler=new MochaEvents(DM,drawhere);
      
      // AWT: Add listeners to CANVAS element.
      drawhere.addKeyListener(eventhandler);
      drawhere.addMouseListener(eventhandler);
      drawhere.addMouseMotionListener(eventhandler);
      addComponentListener(eventhandler);
      addWindowFocusListener(eventhandler);
      addWindowListener(eventhandler);
      
      
	  } catch (Exception e){
		  I.Error("Error creating AWTDoom frame. Exiting. Reason: %s",e.getMessage());
	  }
	 
	  // AWT: tab is a special case :-/
	  // We need to "peg" it to the JFrame, rather than the canvas,
	  // and the handler itself cannot auto-assign it.
	  
      final Component me=drawhere;
	  
      KeyboardFocusManager.
      getCurrentKeyboardFocusManager().
      addKeyEventDispatcher(new KeyEventDispatcher() {
    	  
    	  boolean press=false;
          public boolean dispatchKeyEvent(KeyEvent e) {    
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
            	// MAES: simulating a key type.
            	if (press)
                eventhandler.keyPressed(
                		new KeyEvent(me, e.getID(), System.nanoTime(),0 , KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));
            	else
                    eventhandler.keyReleased(
                    		new KeyEvent(me, e.getID(), System.nanoTime(),0 , KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));
            	press=!press;
            }  
            return false;
          }
      });
	  
	  this.add(drawhere);
	  this.getContentPane().setPreferredSize(drawhere.getPreferredSize());
	  this.pack();
	  this.setVisible(true);
      this.setResizable(false);
	  this.setTitle(Strings.MOCHA_DOOM_TITLE);
	  this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  // Gently tell the eventhandler to wake up and set itself.	  
	  this.requestFocus();
	  this.eventhandler.addEvent(MochaDoomInputEvent.GET_YOUR_ASS_OFF);
	  SetGamma(0); 
	  
	}
	
	@Override
	public void StartFrame() {
		// Dummy. Nothing special to do...yet.
		
	}

	@Override
	public void StartTic() {

		  if (!this.isActive()) return;

		//  System.out.println("Getting events...");
		  while (eventhandler.hasMoreEvents())
			eventhandler.GetEvent();
		      
		    //eventhandler.grabMouse();

		
	}

	protected int lasttic;
	protected int frames;

    @Override
    public void UpdateNoBlit() {
        // Quite pointless, no?
        
    }

    // Convenience, for testers.
	public void GetEvent() {
		this.eventhandler.GetEvent();		
	}

	@Override
	public void ShutdownGraphics() {
		this.dispose();
		((JFrame)this).dispose();
		
	}


	
}
