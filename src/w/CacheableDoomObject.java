package w;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface CacheableDoomObject {
    
    public void unpack(ByteBuffer buf) throws IOException, Exception ;
}
