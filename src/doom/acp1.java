package doom;

import p.mobj_t;

/** Action pointer with one argument */

public abstract class acp1 implements think_t{	
	public abstract void invoke(mobj_t par1);
}
