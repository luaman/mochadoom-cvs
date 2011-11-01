package rr.drawfuns;

/** Either draws a column or a span
 * 
 * @author velktron
 *
 */

public interface SpanFunction<T> {
    public void invoke();
    
	public void invoke(SpanVars<T> dsvars);

}