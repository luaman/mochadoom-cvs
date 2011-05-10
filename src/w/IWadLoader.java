package w;

import java.io.IOException;
import java.nio.ByteBuffer;

import rr.patch_t;

public interface IWadLoader {

	/**
	 * This is where lumps are actually read + loaded from a file.
	 * 
	 * @param filename
	 * @throws Exception
	 */

	public abstract void AddFile(String filename) throws Exception;

	/**
	 * W_Reload Flushes any of the reloadable lumps in memory and reloads the
	 * directory.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("null")
	public abstract void Reload() throws Exception;

	/**
	 * W_InitMultipleFiles
	 *
	 * Pass a null terminated list of files to use (actually
	 * a String[] array in Java).
	 * 
	 * All files are optional, but at least one file
	 * must be found. 
	 * 
	 * Files with a .wad extension are idlink files
	 * with multiple lumps.
	 * 
	 * Other files are single lumps with the base filename
	 * for the lump name.
	 * 
	 * Lump names can appear multiple times.
	 * The name searcher looks backwards, so a later file
	 * does override all earlier ones.
	 * 
	 * @param filenames
	 * 
	 */

	public abstract void InitMultipleFiles(String[] filenames) throws Exception;

	/**
	 * W_InitFile
	 * 
	 * Just initialize from a single file.
	 * 
	 * @param filename 
	 * 
	 */
	public abstract void InitFile(String filename) throws Exception;

	/**
	 * W_NumLumps
	 * 
	 * Returns the total number of lumps loaded in this Wad manager. Awesome. 
	 * 
	 */
	public abstract int NumLumps();

	/**
	 * Returns actual lumpinfo_t object for a given name. Useful if you want to
	 * access something on a file, I guess?
	 * 
	 * @param name
	 * @return
	 */

	public abstract lumpinfo_t GetLumpinfoForName(String name);

	/**
	 * W_GetNumForName
	 * Calls W_CheckNumForName, but bombs out if not found.
	 */

	public abstract int GetNumForName(String name);

	/**
	 *          
	 * @param lumpnum
	 * @return
	 */
	public abstract String GetNameForNum(int lumpnum);

	//
	// W_LumpLength
	// Returns the buffer size needed to load the given lump.
	//
	public abstract int LumpLength(int lump);

	/**
	 * W_ReadLump Loads the lump into the given buffer, which must be >=
	 * W_LumpLength().
	 * 
	 * @throws IOException
	 */

	public abstract void ReadLump(int lump, ByteBuffer dest);

	/**
	 * W_CacheLumpNum Modified to read a lump as a specific type of
	 * CacheableDoomObject. If the class is not identified or is null, then a
	 * generic DoomBuffer object is left in the lump cache and returned.
	 */
	public abstract CacheableDoomObject CacheLumpNum(int lump, int tag,
			Class what);

	/**
	 * Read a lump into an object array, if possible. The binary blob lump will
	 * still be cached as usual, but as a ByteBuffer this time, and
	 * deserialization will be performed into the given Object[] array.
	 * 
	 * Upon a cache hit however, the objects will be deserialized a second time,
	 * thus there will be some time penalty (this, unless I devise a container
	 * class for arrays of CacheableDoomObjects).
	 * 
	 * Helps keep syntax compact.
	 * 
	 */
	public abstract void CacheLumpNumIntoArray(int lump, int tag,
			Object[] array, Class what) throws IOException;

	/**
	 * Return a cached lump based on its name, as raw bytes, no matter what.
	 * It's rare, but has its uses.
	 * 
	 * @param name
	 * @param tag
	 * @param what
	 * @return
	 */

	public abstract byte[] CacheLumpNameAsRawBytes(String name, int tag);

	/**
	 * Return a cached lump based on its num, as raw bytes, no matter what.
	 * It's rare, but has its uses.
	 * 
	 * @param name
	 * @param tag
	 * @param what
	 * @return
	 */

	public abstract byte[] CacheLumpNumAsRawBytes(int num, int tag);

	/**
	 * Return a cached lump based on its name, and for a specificc class. This
	 * will autoload it too, and should be the preferred method of loading
	 * stuff.
	 * 
	 * @param name
	 * @param tag
	 * @param what
	 * @return
	 */

	public abstract DoomBuffer CacheLumpName(String name, int tag);

	/**
	 * Specific method for loading cached patches by name, since it's by FAR the
	 * most common operation.
	 * 
	 * @param name
	 * @return
	 */

	public abstract patch_t CachePatchName(String name);

	/**
	 * Specific method for loading cached patches, since it's by FAR the most
	 * common operation.
	 * 
	 * @param name
	 * @param tag
	 * @return
	 */

	public abstract patch_t CachePatchName(String name, int tag);

	/**
	 * Specific method for loading cached patches by number.
	 * 
	 * @param name
	 * @return
	 */

	public abstract patch_t CachePatchNum(int num, int tag);

	public abstract Object CacheLumpName(String name, int tag, Class what);

	/** A lump with size 0 is a marker. This means that it
	 *  can/must be skipped, and if we want actual data we must
	 *  read the next one. 
	 * 
	 * @param lump
	 * @return
	 */
	public abstract boolean isLumpMarker(int lump);

	public abstract String GetNameForLump(int lump);

	public abstract int CheckNumForName(String name/* , int namespace */);

	public abstract lumpinfo_t GetLumpInfo(int i);

}