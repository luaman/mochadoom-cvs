package hu;

import static data.Defines.*;
import m.Swap;
import rr.patch_t;

// Scrolling Text window widget
//(child of Text Line widget)

public class hu_stext_t {

    hu_textline_t[]   l=new hu_textline_t[HU_MAXLINES]; // text lines to draw
    int         h;      // height in lines
    int         cl;     // current line number

    // pointer to boolean stating whether to update window
    boolean[]        on=new boolean[1]; 
    boolean     laston;     // last value of *->on.

    public hu_stext_t    (
            int       x,
            int       y,
            int       h,
            patch_t[] font,
            int       startchar,
            boolean[]  on ){
        this.initSText(x, y, h, font, startchar, on);
    }
    
    
    public void
    initSText
    (
      int       x,
      int       y,
      int       h,
      patch_t[] font,
      int       startchar,
      boolean[]  on )
    {

        int i;

        this.h = h;
        this.on = on;
        this.laston = true;
        this.cl = 0;
        for (i=0;i<h;i++)
        this.l[i].initTextLine(x, y - i*(Swap.SHORT(font[0].height)+1),
                   font, startchar);

    }

    public void addLineToSText()
    {

        // add a clear line
        if (++this.cl == this.h)
        this.cl = 0;
        this.l[this.cl].clearTextLine();

        // everything needs updating
        for (int i=0 ; i<this.h ; i++)
        this.l[i].needsupdate = 4;

    }

    public void addMessageToSText
    ( 
      char[]     prefix,
      char[]     msg )
    {
        this.addLineToSText();
        if ((prefix!=null)&&(prefix.length>0)){
            int ptr=0;
        while ((ptr<prefix.length)&&(prefix[ptr]>0))
           this.l[this.cl].addCharToTextLine(prefix[ptr++]);
            ptr=0;
        while ((ptr<msg.length)&&(msg[ptr]>0))
            this.l[this.cl].addCharToTextLine(msg[ptr++]);        
            }        
    }

    public void addMessageToSText
    ( 
      String     prefix,
      String     msg )
    {
        this.addLineToSText();
        if ((prefix!=null)&&(prefix.length()>0)){
        for (int i=0;i<prefix.length();i++)
           this.l[this.cl].addCharToTextLine(prefix.charAt(i));
        for (int i=0;i<msg.length();i++)
            this.l[this.cl].addCharToTextLine(msg.charAt(i));        
            }        
    }
    
    public void drawSText()
    {
        int i, idx;
        hu_textline_t l;

        if (!this.on[0])
        return; // if not on, don't draw

        // draw everything
        for (i=0 ; i<this.h ; i++)
        {
        idx = this.cl - i;
        if (idx < 0)
            idx += this.h; // handle queue of lines
        
        l = this.l[idx];

        // need a decision made here on whether to skip the draw
        l.drawTextLine(false); // no cursor, please
        }

    }

    public void eraseSText()
    {
        for (int i=0 ; i<this.h ; i++)
        {
        if (laston && !on[0])
            l[i].needsupdate = 4;
        this.l[i].eraseTextLine();
        }
        laston=on[0];

    }
    
}
