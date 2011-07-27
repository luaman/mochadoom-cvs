package rr;

/** An interface used to ease the use of the GetCachedColumn by part
 *  of parallelized renderers.
 *  
 * @author Maes
 *
 */

public interface IGetColumn {

	byte[] GetCachedColumn(int tex, int col);

}
