package z;

import w.CacheableDoomObject;

public interface IZone {

	/** Add a certain lump to the tracking mechanism.
	 *  This allows a way to trace back a cached object
	 *  to its disk lump.
	 * 
	 * @param lump
	 * @param index
	 */
	
	void Track(CacheableDoomObject lump, int index);
	
	/** Null the disk lump associated with a particular object,
	 *  in any. This will NOT induce a garbage collection, unless
	 *  you also null any references you have to that object.  
	 * 
	 * @param lump
	 */
	
	void Free(CacheableDoomObject lump);

	void setLumps(CacheableDoomObject[] lumps);
	
}


//-----------------------------------------------------------------------------
//
// $Log: IZone.java,v $
// Revision 1.1  2011/06/02 13:58:52  velktron
// Lame pseudo-zone system.
//
//
//-----------------------------------------------------------------------------
