package hu;

import static data.Defines.*;
import rr.patch_t;

public class hu_itext_t {

    // Input Text Line widget
    //  (child of Text Line widget)

    hu_textline_t   l;      // text line to input on

    // left margin past which I am not to delete characters
    int         lm;

    // pointer to boolean stating whether to update window
    boolean[]        on=new boolean[1]; 
    boolean     laston; // last value of *->on;

    public void
    initIText
    ( 
            int       x,
            int       y,
            patch_t[] font,
            int       startchar,
            boolean[]  on )
    {
        this.lm = 0; // default left margin is start of text
        this.on = on;
        this.laston = true;
        l=new hu_textline_t(x, y, font, startchar);
    }


    // The following deletion routines adhere to the left margin restriction
    public void delCharFromIText()
    {
        if (this.l.len != this.lm)
            this.l.delCharFromTextLine();
    }

    public void eraseLineFromIText()
    {
        while (this.lm != this.l.len)
            l.delCharFromTextLine();
    }

    // Resets left margin as well
    public void resetIText()
    {
        this.lm = 0;
        this.l.clearTextLine();
    }

    public void
    addPrefixToIText
    ( 
            char[]     str )
    {
        int ptr=0;
        while (str[ptr]>0){
            l.addCharToTextLine(str[ptr++]);
            this.lm=this.l.len;
        }
    }

    // Maes: String overload
    public void
    addPrefixToIText
    ( 
            String     str )
    {
        int ptr=0;
        while (str.charAt(ptr)>0){
            l.addCharToTextLine(str.charAt(ptr++));
            this.lm=this.l.len;
        }
    }

        // wrapper function for handling general keyed input.
        // returns true if it ate the key
        public boolean
        keyInIText(char ch )
        {

            if (ch >= ' ' && ch <= '_') 
                this.l.addCharToTextLine((char) ch);
            else 
                if (ch == KEY_BACKSPACE) 
                    this.delCharFromIText();
                else 
                    if (ch != KEY_ENTER) 
                        return false; // did not eat key

            return true; // ate the key

        }

        public void drawIText()
        {

            hu_textline_t l = this.l;

            if (!this.on[0])
                return;
            this.l.drawTextLine(true); // draw the line w/ cursor

        }

        void eraseIText()
        {
            if (this.laston && !this.on[0])
                this.l.needsupdate = 4;
                this.l.eraseTextLine();
                this.laston = this.on[0];
        }

    };
