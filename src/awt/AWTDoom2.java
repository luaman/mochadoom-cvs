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
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.IndexColorModel;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import timing.ITicker;
import v.BufferedRenderer;
import doom.DoomMain;
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
 *  
 * 
 * @author Velktron
 *
 */
public class AWTDoom2 extends JFrame implements DoomVideoInterface{

    private AWTEvents2 awtEvents;

    private static final long serialVersionUID = 3118508722502652276L;

    private static final int POINTER_WARP_COUNTDOWN = 1;
    
    // Must be aware of "Doom" so we can pass it event messages inside a crude queue.
    public DoomMain DM;
    private IDoomSystem I; // Must be aware of some other shit like event handler
    private BufferedRenderer V;   // Must be able to "peg" itself to a raw byte array representing
    private ITicker TICK;                                // Screen 0
    private byte[] RAWSCREEN;     // RAW SCREEN DATA. Get from the Video Renderer.
    private IndexColorModel[] cmaps;
      Robot robby;
        Canvas drawhere = new Canvas();
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

        protected boolean grabMouse=true;
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
     
        public AWTDoom2(DoomMain DM, BufferedRenderer V, byte[] cmap) {
            this.DM=DM;
            this.I=DM.I;
            this.V= V;
            this.cmap=cmap;
            this.TICK=DM.TICK;
            awtEvents = new AWTEvents2(DM, drawhere);
            // If these aren't set here, the init code will work with zero
            // values.
            this.width=V.getWidth();
            this.height=V.getHeight();
            

        // Don't do anything yet until InitGraphics is called.
        }
        
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
           g2d.drawImage(screens[palette],0,20,this);
           
        }
        
   /*     public void update() {
            
            Graphics2D g2d = (Graphics2D)drawhere.getGraphics();
            //voli.getGraphics().drawImage(bi,0,0,null);
            g2d.drawImage(screens[palette],0,0,this);
            
         } */
        
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

      String        displayname;
      String        d;
      int           n;
      int           pnum;
      int           x=0;
      int           y=0;
      
      // warning: char format, different type arg
      int       xsign=' ';
      int       ysign=' ';
      
      boolean           oktodraw;
      long  attribmask;

      // Try setting the locale the US, otherwise there will be problems
      // with non-US keyboards.
      if (!this.getInputContext().selectInputMethod(java.util.Locale.US)){
          System.err.println("Could not set the input context to US! Kayboard input will be glitchy!");
      } else {
          System.err.println("Input context successfully set to US.");
      }
      
      //signal(SIGINT, (void (*)(int)) I_Quit);

      if (DM.CM.CheckParm("-2")!=0)
        multiply = 2;

      if (DM.CM.CheckParm("-3")!=0)
        multiply = 3;

      if (DM.CM.CheckParm("-4")!=0)
        multiply = 4;

      Dimension size = new Dimension();
      size.width = 8+width * multiply;
      size.height = 54+(height * multiply);

      // Create AWT Robot for forcing mouse
      try {
      robby=new Robot();
      } catch (Exception e){
          System.out.println("AWT Robot could not be created, mouse input focus will be loose!");
      }
      
      // check for command-line display name
      if ( (pnum=DM.CM.CheckParm("-disp"))!=0 ) // suggest parentheses around assignment
        displayname = DM.CM.getArgv(pnum+1);
      
      else
        displayname = null;

      // check if the user wants to grab the mouse (quite unnice)
      // HMM...double !!...so if the user WANTS, it will be !!true=!false=true :-S
      grabMouse = true; // DM.CheckParm("-grabmouse")!=0;

      // check for command-line geometry
      if ( (pnum=DM.CM.CheckParm("-geom"))!=0 ) // suggest parentheses around assignment
      {
          try{
          String eval=DM.CM.getArgv(pnum+1).trim();
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
      drawhere.setSize(size);
      drawhere.setBounds(0, 0, drawhere.getWidth()-1,drawhere.getHeight()-1);
      drawhere.setBackground(Color.black);
      
      // AWT: Add canvas to component.
      this.add(drawhere);
      
      // AWT: Add listeners.
      drawhere.addKeyListener(awtEvents);
      drawhere.addMouseListener(awtEvents);
      drawhere.addMouseMotionListener(awtEvents);
      
      final Component me=this;
      
      // AWT: tab is a special case :-/
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {  
          public boolean dispatchKeyEvent(KeyEvent e) {    
            if (e.getKeyCode() == KeyEvent.VK_TAB) {   
                awtEvents.addEvent(new KeyEvent(me, e.getID(), System.nanoTime(),0 , KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));

            }  
            return false;
          }
        });

      
      //AWT: Make visible and "peg" the g2d Object to the Canvas,
      // else it will be nullified.
      this.setVisible(true);
      
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
      this.setBounds(x, y, width, height);
      this.setSize(size);
      this.setResizable(false);
        this.setTitle(Strings.MOCHA_DOOM_TITLE);
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
          while (!awtEvents.eventQueue.isEmpty())
            awtEvents.GetEvent();

          // Warp the pointer back to the middle of the window
          //  or it will wander off - that is, the game will
          //  loose input focus within X11.
          /*if (grabMouse)
          {
              if (doPointerWarp--<=0)
          {
              robby.mouseMove(this.getX()+this.getWidth()/2, this.getY()+this.getHeight()/2);
          } 
              doPointerWarp = POINTER_WARP_COUNTDOWN;
        }*/

         //awtEvents.mousemoved = false;
        
    }

    private int lasttic;
    private int frames;

    @Override
    public void FinishUpdate() {
        int     tics;
        int     i;
        
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
    
    /*
    @Override
    public void SetMap(byte[] cmap) {
        this.cmap = cmap;
    }

    @Override
    public void SetVideoRenderer(BufferedRenderer V) {
        this.V = V;
        this.width=V.getWidth();
        this.height=V.getHeight();
    } */

}


