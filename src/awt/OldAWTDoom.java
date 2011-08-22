package awt;

import i.DoomVideoInterface;
import i.IDoomSystem;
import i.InputListener;
import i.Strings;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.IndexColorModel;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import timing.ITicker;
import v.BufferedRenderer;
import v.SoftwareVideoRenderer;
import doom.DoomMain;
import doom.ICommandLineManager;
import doom.event_t;

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
 *  This implementation is deprecated, as it depends on the older AWTEvents
 *  class, which is not recommended for use anymore.
 * 
 * @author Velktron
 *
 */

@Deprecated
public class OldAWTDoom extends JFrame implements DoomVideoInterface{


    private static final long serialVersionUID = 3118508722502652276L;

    
	
	// Must be aware of "Doom" so we can pass it event messages inside a crude queue.
	public DoomMain DM;            // Must be aware of general status.
	public ICommandLineManager CM; // Must be aware of command line interface.
	private IDoomSystem I;         // Must be aware of some other shit like event handler
	private BufferedRenderer V;    // Must have a video renderer....
	private ITicker TICK;          // Must be aware of the ticker/
	private byte[] RAWSCREEN;	   // RAW SCREEN DATA. Get from the Video Renderer.
	private AWTEvents eventhandler; // Separate event handler a la _D_.
	                               // However I won't make it fully "eternity like" yet
	                               // also because it works quite flakey on Linux.
	
	
	private IndexColorModel[][] cmaps;
	  Robot robby;
		Canvas drawhere;
		/** These are the actual screens */
        Image[] screens;
        int palette=0;
        Dimension size;
        Image crap;
        //InputListener in;
        Graphics2D g2d;

        /** Dimensions of the screen buffers */
        protected int width,height;
        protected int multiply=1;
        protected byte[] cmap;
		private int maxpalettes;

		/** Gimme some raw palette RGB data.
		 *  I will do the rest
		 *  
		 *  (hint: read this from the PLAYPAL
		 *   lump in the IWAD!!!).
		 * 
		 */
     
        public OldAWTDoom(DoomMain DM, BufferedRenderer V, byte[] cmap) {
        	this.DM=DM;
        	this.CM=DM.CM;
        	this.TICK=DM.TICK;
        	this.I=DM.I;
        	this.V= V;
        	this.cmap=cmap;
            // If these aren't set here, the init code won't size up windows properly.
        	// Of course, we must have made up our mind about the resolution
        	// already, at this point (TODO: command line parameters and multiplication
        	// should be handled BEFORE reaching this point).
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
        
        // This stuff should NOT get through in keyboard events.
        protected final int UNACCEPTABLE_MODIFIERS=(int) (InputEvent.ALT_GRAPH_DOWN_MASK+
        										 InputEvent.META_DOWN_MASK+
        										 InputEvent.META_MASK+
        										 InputEvent.WINDOW_EVENT_MASK+
        										 InputEvent.WINDOW_FOCUS_EVENT_MASK);

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
		  System.err.println("Could not set the input context to US! Keyboard input will be glitchy!");
	  } else {
		  System.err.println("Input context successfully set to US.");
	  }
	  
	  //signal(SIGINT, (void (*)(int)) I_Quit);

	  if (CM.CheckParm("-2")!=0)
		multiply = 2;

	  if (CM.CheckParm("-3")!=0)
		multiply = 3;

	  if (CM.CheckParm("-4")!=0)
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
      drawhere=new Canvas();
      // MAES: this method works even on "stubborn" Linux distros that 
      // fuck up the window size.
      drawhere.setPreferredSize(size);
      drawhere.setBounds(0, 0, drawhere.getWidth()-1,drawhere.getHeight()-1);
      drawhere.setBackground(Color.black);
      
      this.eventhandler=new AWTEvents(DM,drawhere);
      
      // AWT: Add listeners to CANVAS element.
      drawhere.addKeyListener(eventhandler);
      drawhere.addMouseListener(eventhandler);
      drawhere.addMouseMotionListener(eventhandler);
      
	  } catch (Exception e){
		  I.Error("Error creating AWTDoom frame. Exiting.");
	  }

	  // create the colormaps
	  
   // create the colormaps
      
      createPalettes();
	    
	 /* this.s  pals=V.getBufferedScreens(0, icms);
	  cmaps = XCreateColormap(X_display, RootWindow(X_display,
							   X_screen), X_visual, AllocAll); */


	 
	  // AWT: tab is a special case :-/
	  // We need to "peg" it to the JFrame, rather than the canvas,
	  // and the handler itself cannot auto-assign it.
	  
      final Component me=drawhere;
	  
      KeyboardFocusManager.
      getCurrentKeyboardFocusManager().
      addKeyEventDispatcher(new KeyEventDispatcher() {  
          public boolean dispatchKeyEvent(KeyEvent e) {    
            if (e.getKeyCode() == KeyEvent.VK_TAB) {      
            	System.out.println("TAB pressed");
                eventhandler.addEvent(new KeyEvent(me, e.getID(), System.nanoTime(),0 , KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));

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
	  this.eventhandler.reposition();
      setGamma(0);
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
		  while (!AWTEvents.eventQueue.isEmpty())
			eventhandler.GetEvent();
		      
		    eventhandler.grabMouse();

		
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

        i = TICK.GetTime();
        tics = i - lasttic;
        lasttic = i;
        if (tics<1) 
            frames++;
        else
        {
        //frames*=35;
        for (i=0 ; i<frames*2 ; i+=2)
            RAWSCREEN[ (height-1)*width + i] = (byte) 0xff;
        for ( ; i<20*2 ; i+=2)
            RAWSCREEN[ (height-1)*width + i] = 0x0;
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

    // Convenience, for testers.
	public void GetEvent() {
		this.eventhandler.GetEvent();		
	}

	@Override
	public void ShutdownGraphics() {
		this.dispose();
		((JFrame)this).dispose();
		
	}

	 protected void createPalettes() {
	        if (cmap!=null|| cmap.length<768) {
	              // As many as are likely contained
	              maxpalettes=cmap.length/768;
	              System.out.println(maxpalettes + " palettes read");
	              
	              // As many as gammas.
	              cmaps=new IndexColorModel[SoftwareVideoRenderer.gammatable.length][];
	              
	              // First set of palettes, normal gamma.
	              cmaps[0]=new IndexColorModel[maxpalettes];
	        
	              // Now we have our palettes.
	              for (int i=0;i<maxpalettes;i++){
	                  cmaps[0][i]=new IndexColorModel(8, 256,cmap, i*768, false);
	                    }
	            
	            // Wire the others according to the gamma table.
	              byte[] tmpcmap=new byte[768];
	              
	              // For each gamma value...
	              for (int j=1;j<SoftwareVideoRenderer.gammatable.length;j++){
	                  
	                  cmaps[j]=new IndexColorModel[maxpalettes];
	                  
	                  // For each palette
	                  for (int i=0;i<maxpalettes;i++){
	                      
	                      for (int k=1;k<256;k++){
	                          tmpcmap[3*k]=(byte) SoftwareVideoRenderer.gammatable[j][0x00FF&cmap[i*768+3*k]]; // R
	                          tmpcmap[3*k+1]=(byte) SoftwareVideoRenderer.gammatable[j][0x00FF&cmap[1+i*768+3*k]]; // G
	                          tmpcmap[3*k+2]=(byte) SoftwareVideoRenderer.gammatable[j][0x00FF&cmap[2+i*768+3*k]]; // B
	                        }

	                      cmaps[j][i]=new IndexColorModel(8, 256,tmpcmap, 0, false);
	                    }
	              }

	              
	          } else {
	              // Allow it to pull from some default location?
	              System.err.println("Palette and colormaps could not be set up. Bye");
	              System.exit(-1);
	          }
	    } 
	    
	    public void setGamma(int level){
	        System.out.println("Setting gamma "+level);
	        screens=V.getBufferedScreens(0, cmaps[level]);
	        RAWSCREEN=V.getScreen(0);
	    }
	
	    @Override
	    public void ReadScreen(byte[] scr) {
	        System.arraycopy(this.RAWSCREEN, 0, scr, 0, RAWSCREEN.length);
	        }
	    
}

//$Log: OldAWTDoom.java,v $
//Revision 1.2.2.3  2011/08/22 15:34:45  velktron
//All new shitty stuff.
//
//Revision 1.3  2011/07/15 13:57:54  velktron
//Implement VI.ReadScreen as a future good practice.
//
//Revision 1.2  2011/06/14 09:54:20  velktron
//Separated palette generation/fixed OldAWTDoom
//
//Revision 1.1  2011/06/02 14:54:18  velktron
//Old AWTEvents deprecated. MochaEvents now default.
//
//Revision 1.5  2011/06/01 17:17:24  velktron
//New event system.
//
//Revision 1.4  2011/05/30 02:25:50  velktron
//Centering and offsetting on expose, proper exiting.
//
//Revision 1.3  2011/05/29 22:15:32  velktron
//Introduced IRandom interface.
//
//Revision 1.2  2011/05/29 20:58:58  velktron
//Added better mouse grabbing method, more reliable, more cross-OS.
//
//Revision 1.1  2011/05/27 13:26:56  velktron
//A slightly better, though not perfect, way to handle input, partially based on_D_'s work.
//