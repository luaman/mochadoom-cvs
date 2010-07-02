package rr;

import java.io.IOException;
import java.nio.ByteBuffer;

import utils.C2JUtils;
import w.CacheableDoomObject;
import w.DoomFile;
import w.ReadableDoomObject;

//column_t is a list of 0 or more post_t, (byte)-1 terminated
//typedef post_t  column_t;

/** For the sake of efficiency, "column_t" will store raw data.
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
	public byte[] data; // The RAW data.
	public int posts;
	public int[] postofs; // Posts relative offsets. Should help somehow.
	public short[] postlen; // Posts lengths
	public short[] postdeltas; // Posts lengths
	
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
	        
	        // Did we read an FF?
	        while((topdelta=C2JUtils.toUnsignedByte(buf.get()))!=0xFF){
	        // This is where this posts starts.
	            //if (postno==0){
	                guesspostdeltas[postno]=(short)topdelta;
	            //}
	        guesspostofs[postno]=skipped;
	        

	        //this.topdelta=(short)f.readUnsignedByte();
	        postlen=(short)C2JUtils.toUnsignedByte(buf.get());
	        guesspostlens[postno++]=postlen;
	        // So, we already read 2 bytes (topdelta + length)
	        // Two further bytes are padding so we can safely skip 2+length bytes.
	        skipped+=4+postlen;
	        buf.position(buf.position()+2+postlen);
	        colheight+=postlen;
	        }
	        
	        skipped++;
	        
	        len = finalizeStatus(skipped, colheight, postno);
	        
	        // Go back...
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
  
    private int finalizeStatus(int skipped, int colheight, int postno) {
        int len;
        // That's the length.
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


