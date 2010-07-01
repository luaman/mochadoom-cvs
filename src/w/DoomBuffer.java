package w;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/** Very similar to the concept of ReadableDoomObjects, but made to work with byte buffers instead.
 * 
 * @author admin
 *
 */

public class DoomBuffer         {


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
    
    

}
