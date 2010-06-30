package rr;

public class post_t {
    
    public post_t(byte topdelta, byte length) {
        this.topdelta = topdelta;
        this.length = length;
    }
    // MAES: were byte...not a good idea.
    public byte        topdelta;   // -1 is the last post in a column
    public byte        length;     // length data bytes follows
}


// MAES: This distinction is really pointless in Java. Stick to either type.
//column_t is a list of 0 or more post_t, (byte)-1 terminated
//typedef post_t  column_t;