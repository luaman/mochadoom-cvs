package rr.drawfuns;

/** Either draws a column or a span
 * 
 * @author velktron
 *
 */

public interface ColumnFunction<T> {
    public void invoke();
    
	public void invoke(ColVars<T> dcvars);
}