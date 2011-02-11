package hu;

import rr.patch_t;
import static data.Defines.*;
import m.Swap;

// Text Line widget
//(parent of Scrolling Text and Input Text widgets)

public class hu_textline_t {

    
    
    
    /** MAES: this was the only variable in HUlib.c, and only 
     *  instances of hu_textline_t ever use it. For this reason,
     *  it makes sense to have it common (?) between all instances
     *  of hu_textline_t and set it somewhere else. Of course,
     *  if could be made an instance method or a HUlib object could be defined.
     * 
     */
    protected static  boolean automapactive;  // in AM_map.c

    public static boolean isAutomapactive() {
        return automapactive;
    }

    public static void setAutomapactive(boolean automapactive) {
        hu_textline_t.automapactive = automapactive;
    }

    /** Same here.
     * 
     */

 // TODO: boolean : whether the screen is always erased
    protected static boolean noterased; //=viewwindowx;
    
    public static boolean isNoterased() {
        return noterased;
    }

    public static void setNoterased(boolean noterased) {
        hu_textline_t.noterased = noterased;
    }

    // left-justified position of scrolling text window
    int     x;
    int     y;
    
    //MAES: was **
    patch_t[]   f;                            // font
    int     sc;                             // start character
    char[] l=new char[HU_MAXLINELENGTH+1];  // line of text
    int     len;                             // current line length
                
    // whether this line needs to be udpated
    int     needsupdate;          

    
    public void clearTextLine()
    {
        this.len = 0;
        this.l[0] = 0;
        // It's actually used as a status, go figure.
        this.needsupdate = 1;
    }
    
    // Maes: this could as well be the contructor

    public  void
    initTextLine
    ( 
      int           x,
      int           y,
      patch_t[]     f,
      int           sc )
    {
        this.x = x;
        this.y = y;
        this.f = f;
        this.sc = sc;
        this.clearTextLine();
    }

    public hu_textline_t
    ( 
      int           x,
      int           y,
      patch_t[]     f,
      int           sc )
    {
        this.x = x;
        this.y = y;
        this.f = f;
        this.sc = sc;
        this.clearTextLine();
    }
    
    public hu_textline_t() {
        // TODO Auto-generated constructor stub
    }

    public boolean
   addCharToTextLine
    (char          ch )
    {

        if (this.len == HU_MAXLINELENGTH)
        return false;
        else
        {
        this.l[this.len++] = ch;
        this.l[this.len] = 0;
        // MAES: for some reason this is set as "4", so this is a status rather than a boolean.
        this.needsupdate = 4;
        return true;
        }

    }
    
    /**MAES: This is much better than cluttering up the syntax everytime a STRING must be added.
     * 
     * @param s
     * @return
     */
    
    public boolean
    addStringToTextLine
     (String          s )
     {
        int index=0;
         if (this.len == HU_MAXLINELENGTH)
         return false;
         else
         while (this.len < HU_MAXLINELENGTH){
             
         this.l[this.len++] = s.charAt(index);

         // MAES: for some reason this is set as "4", so this is a status rather than a boolean.
         this.needsupdate = 4;
         this.l[this.len] = 0; // final padding.
         }
         return true;
     }
    

    boolean delCharFromTextLine()
    {

        if (this.len==0) return false;
        else
        {
            this.l[--this.len] = 0;
            this.needsupdate = 4;
        return true;
        }

    }

    void drawTextLine( boolean       drawcursor )
    {

        int         i;
        int         w;
        int         x;
        char   c;

        // draw the new stuff
        x = this.x;
        for (i=0;i<this.len;i++)
        {
        c = Character.toUpperCase(this.l[i]);
        if (c != ' '
            && c >= this.sc
            && c <= '_')
        {
            w = Swap.SHORT(this.f[c - this.sc].width);
            if (x+w > SCREENWIDTH)
            break;
            // TODO: must implement renderer!
            //V_DrawPatchDirect(x, l->y, FG, l->f[c - l->sc]);
            x += w;
        }
        else
        {
            x += 4;
            if (x >= SCREENWIDTH)
            break;
        }
        }

        // draw the cursor if requested
        if (drawcursor
        && x +  Swap.SHORT(this.f['_' - this.sc].width) <= SCREENWIDTH)
        {
        // TODO: Implement V_ stuff!
        //V_DrawPatchDirect(x, l->y, FG, l->f['_' - l->sc]);
        }
    }

    // MAES: was "static" in C within HUlib. Which may mean it's instance specific or global-ish. Or both.
    protected boolean  lastautomapactive = true;
    
    // sorta called by HU_Erase and just better darn get things straight
    public void eraseTextLine()
    {
        int         lh;
        int         y;
        int         yoffset;


        // Only erases when NOT in automap and the screen is reduced,
        // and the text must either need updating or refreshing
        // (because of a recent change back from the automap)

        if (!automapactive &&
        /*viewwindowx &&*/ (this.needsupdate>0))
        {
        lh = m.Swap.SHORT(this.f[0].height) + 1;
        /*
        for (y=this.y,yoffset=y*SCREENWIDTH ; y<this.y+lh ; y++,yoffset+=SCREENWIDTH)
        {
             Stuff is probably in am_map??
            if (y < viewwindowy || y >= viewwindowy + viewheight)
            R_VideoErase(yoffset, SCREENWIDTH); // erase entire line
            else
            {
            R_VideoErase(yoffset, viewwindowx); // erase left border
            R_VideoErase(yoffset + viewwindowx + viewwidth, viewwindowx);
            // erase right border
            }
        }
        }

        lastautomapactive = automapactive;
        if (l->needsupdate) l->needsupdate--;*/

    }
    
}
    }
