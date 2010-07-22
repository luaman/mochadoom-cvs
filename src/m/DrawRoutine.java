package m;

/** menuitem_t required a function pointer to a (routine)()
 *  So any class implementing them will implement this interface, and
 *  we can have a single class type for all of them.
 * 
 * @author MAES
 *
 */
public interface DrawRoutine {
	
public void invoke();
}
