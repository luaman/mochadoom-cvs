package doom;

import p.mobj_t;

/** Action pointer with one argument */

public interface acp1 extends think_t{	
	public void invoke(mobj_t par1);
}
