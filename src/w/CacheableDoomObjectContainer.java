package w;

import java.io.IOException;
import java.nio.ByteBuffer;

/** A container allowing for caching of arrays of CacheableDoomObjects 
 *  
 *  It's a massive improvement over the older system, allowing for proper 
 *  caching and auto-unpacking of arrays of CacheableDoomObjects and much 
 *  cleaner code throughout.
 *  
 *  The container itself is a CacheableDoomObject....can you feel the
 *  abuse? ;-)
 * 
 */

public class CacheableDoomObjectContainer implements CacheableDoomObject {
	
	private CacheableDoomObject[] stuff;
	
	public CacheableDoomObjectContainer(CacheableDoomObject[] stuff){
		this.stuff=stuff;
	}
	
	public CacheableDoomObject[] getStuff(){
		return stuff;
	}
	
	@Override
	public void unpack(ByteBuffer buf) throws IOException {
		for (int i = 0; i < stuff.length; i++) {
				stuff[i].unpack(buf);
			}
		}

}
