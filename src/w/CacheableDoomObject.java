package w;

import java.io.IOException;
import java.nio.ByteBuffer;

/** All objects that can be deserialized from raw byte buffers such as those
 *  read from WAD lumps should implement this method, so that the WadLoader 
 *  can cache them, and recursive calls to  sub-objects can be made.
 *  
 *  E.g. an object of type A consists of a header and a list of objects of type B.
 *  Calling A.unpack(buf) will cause A to unmarshal its own header, set the list of 
 *  B objects, and then call B.unpack() for each of them, by passing the same buffer
 *  along.
 *  
 *  This system works cleanly, and allows to simulate Doom's "cached memory" while
 *  returning proper objects of the correct type and keeping close to Java's
 *  "correct" way of doing things. 
 *   *  
 *  For example, if a patch_t is read from disk, the WadLoader uses its unpack() method
 *  to read it from a lump read from disk, and creates a new patch_t object, which is placed
 *  in the lump cache (which holds CacheableDoomObject, incidentally). The next time this
 *  same patch_t is requested, the reference to the already cached patch_t will be returned,
 *  if it hasn't been forcedly flushed from the cache. Voila', lump caching!
 *  
 *  The principle can be applied to ARRAYS of similar objects too: using the same buffer,
 *  iterative serial unpacking is possible, while still mantaining a "cached" reference
 *  to their array (TODO: actually, this needs to be implemented more efficiently. Look in
 *  WadLoader) 
 *  
 *  The opposite would be a "PackableDoomObject", aka objects that can pack themselves into
 *  a byte buffer for transmission purposes, although Doom doesn't really need to write as
 *  much as it needs reading. 
 *    
 * 
 * @author Velktron
 *
 */

public interface CacheableDoomObject {
    
    public void unpack(ByteBuffer buf) throws IOException ;
}
