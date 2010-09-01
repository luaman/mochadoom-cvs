package rr;

import java.io.IOException;
import java.nio.ByteBuffer;

import utils.C2JUtils;
import w.CacheableDoomObject;
import w.DoomFile;
import w.ReadableDoomObject;

/** column_t is a list of 0 or more post_t, (byte)-1 terminated
 * typedef post_t  column_t;
 * For the sake of efficiency, "column_t" will store raw data, however I added
 * some stuff to make my life easier.
 * 
 */

public class column_t implements CacheableDoomObject, ReadableDoomObject{
    
    /** Static buffers used during I/O. 
     *  There's ABSO-FUCKING-LUTELY no reason to manipulate them externally!!!
     *  I'M NOT KIDDING!!!11!!
     */
    
    private static final int[] guesspostofs=new int[256];
    private static final short[] guesspostlens=new short[256];
    private static final short[] guesspostdeltas=new short[256];
    
    public short        topdelta;   // -1 is the last post in a column (actually 0xFF, since this was unsigned???)
    public short        length;     // length data bytes follows (actually add +2)
	//public column_t[]      posts;    // This is quite tricky to read.
    /** The RAW data (includes initial header and padding, because no post gets preferential treatment). */
    public byte[] data; 
    /** Actual number of posts inside this column. All guesswork is done while loading */
	public int posts;
	/** Positions of posts inside the raw data (point at headers) */
	public int[] postofs; 
	/** Posts lengths, intended as actual drawable pixels. Add +4 to get the whole post length */
	public short[] postlen;
	/** Vertical offset of each post. In theory it should be possible to quickly
	 *  clip to the next visible post when drawing a column */
	public short[] postdeltas;
	
    @Override
		public void unpack(ByteBuffer buf) throws IOException {
	        // Mark current position.
	        buf.mark();
	        int skipped=0;
	        short postlen=0;
	        int colheight=0;	        
	        int len=0; // How long is the WHOLE column, until the final FF?
	        int postno=0; // Actual number of posts.
	        int topdelta=0;
	        
	        // Scan every byte until we encounter an 0xFF which definitively marks the end of a column.
	        while((topdelta=C2JUtils.toUnsignedByte(buf.get()))!=0xFF){
	        
	        // First byte of a post should be its "topdelta"
            guesspostdeltas[postno]=(short)topdelta;
	        guesspostofs[postno]=skipped; // 0 for first post

	        // Read one more byte...this should be the post length.
	        postlen=(short)C2JUtils.toUnsignedByte(buf.get());
	        guesspostlens[postno++]=postlen;
	        
	        // So, we already read 2 bytes (topdelta + length)
	        // Two further bytes are padding so we can safely skip 2+2+postlen bytes until the next post
	        skipped+=4+postlen;
	        buf.position(buf.position()+2+postlen);
	        
	        // Obviously, this adds to the height of the column, which might not be equal to the patch that
	        // contains it.
	        colheight+=postlen;
	        }
	        
	        // Skip final padding byte ?
	        skipped++;
	        
	        len = finalizeStatus(skipped, colheight, postno);
	        
	        // Go back...and read the raw data. That's what will actually be used in the renderer.
	        buf.reset();
	        buf.get(data, 0, len);
	    }



    @Override
    public void read(DoomFile f)
            throws IOException {
        // Mark current position.
        long mark1=f.getFilePointer();
        int skipped=0;
        short postlen;
        int colheight=0;            
        int len=0; // How long is the WHOLE column, until the final FF?
        int postno=0; // Actual number of posts.
        int topdelta=0;
        
        
        // Did we read an FF?
        while((topdelta=(short)f.readUnsignedByte())!=0xFF){

            //if (postno==0){
                guesspostdeltas[postno]=(short)topdelta;
            //}
                // This is where this posts starts.
        guesspostofs[postno]=skipped;        
        postlen=(short)f.readUnsignedByte();
        guesspostlens[postno++]=postlen;
        // So, we already read 2 bytes (topdelta + length)
        // Two further bytes are padding so we can safely skip 2+length bytes.
        skipped+=2+f.skipBytes(this.length+2);
        colheight+=postlen;
        }
        
        skipped++;
        
        // That's the length.
        len=this.finalizeStatus(skipped, colheight, postno);
                
        // Go back...
        f.seek(mark1);
        f.read(data, 0, len);
    }
  
    /** This -almost- completes reading, by filling in the header information
     *  before the raw column data is read in.
     *  
     * @param skipped
     * @param colheight
     * @param postno
     * @return
     */
    
    private int finalizeStatus(int skipped, int colheight, int postno) {
        int len;
        // That's the TOTAL length including all padding.
        // This means we redundantly read some data
        len=(int) (skipped);
        this.data=new byte[len];
        
        this.postofs=new int[postno];
        this.postlen=new short[postno];
        this.length=(short) colheight;
        this.postdeltas=new short[postno];
        
        System.arraycopy(guesspostofs, 0,this.postofs, 0, postno);
        System.arraycopy(guesspostlens, 0,this.postlen, 0, postno);
        System.arraycopy(guesspostdeltas, 0,this.postdeltas, 0, postno);
        
        this.posts=postno;
        return len;
    }
}


