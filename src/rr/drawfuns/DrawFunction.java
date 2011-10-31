package rr.drawfuns;

/** Either draws a column or a span
 * 
 * @author velktron
 *
 */

public interface DrawFunction<T> {
    public void invoke();
    
	public void invoke(ColVars<T> dcvars);
}