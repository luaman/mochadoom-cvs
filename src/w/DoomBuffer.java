package w;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/** Very similar to the concept of ReadableDoomObjects
 *  but made to work with byte buffers instead. 
 *  
 *  This is normally NOT used to pass data around: I am 
 *  using it as a workaround to store raw byte buffers
 *  into a "CacheableDoomObject" array, as Java
 *  doesn't seem to like storing both ByteBuffers and
 *  CacheableDoomObjects in the same array. WTF...
 * 
 * @author admin
 *
 */

public class DoomBuffer implements CacheableDoomObject  {

    public DoomBuffer(){

    }
    
    public DoomBuffer(ByteBuffer b){
        this.buffer=b;
    }

    private ByteBuffer buffer;

    public static void readObjectArray(ByteBuffer buf,CacheableDoomObject[] s,int len) throws IOException {

        if ((s==null)||(len==0)) return;
        
        for (int i=0;i<Math.min(len,s.length);i++){           
            s[i].unpack(buf);
        }
    }
    
    public static void readIntArray(ByteBuffer buf,int[] s,int len) throws IOException {

        if ((s==null)||(len==0)) return;
        
        for (int i=0;i<Math.min(len,s.length);i++){           
            s[i]=buf.getInt();
        }
    }
    
    /** Reads a length specified string from a buffer. */
    public static String readString(ByteBuffer buf) throws IOException {
        int len = buf.getInt();

        if (len == -1)
            return null;

        if (len == 0)
            return "";

        byte bb[] = new byte[len];

        buf.get(bb, 0, len);

        return new String(bb, 0, len);
    }

 /** MAES: Reads a specified number of bytes from a buffer into a new String.
  *  With many lengths being implicit, we need to actually take the loader by the hand.
  * 
  * @param buf
  * @param len
  * @return
  * @throws IOException
  */
    
    public static String getString(ByteBuffer buf, int len) throws IOException {

        if (len == -1)
            return null;

        if (len == 0)
            return "";

        byte bb[] = new byte[len];

        buf.get(bb, 0, len);

        return new String(bb, 0, len);
    }
    
    /** MAES: Reads a specified number of bytes from a buffer into a new String.
     *  With many lengths being implicit, we need to actually take the loader by the hand.
     * 
     * @param buf
     * @param len
     * @return
     * @throws IOException
     */
       
       public static char[] getCharSeq(ByteBuffer buf, int len) throws IOException {
           return (getString(buf,len)).toCharArray();
       }

    

    @Override
    public void unpack(ByteBuffer buf)
            throws IOException {
        this.buffer=buf;
        
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
    
    

}
