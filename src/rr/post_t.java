package rr;

/** The difference with a column_t is that a post_t carries pixel data only,
 *  while a column_t may actuallyactually only groups
 * 
 * @author Maes
 *
 */

public class post_t {
    
    public post_t(byte topdelta, byte length) {
        this.topdelta = topdelta;
        this.length = length;
    }
    // MAES: were byte...not a good idea.
    public byte        topdelta;   // -1 is the last post in a column
    public byte        length;     // length data bytes follows
}

