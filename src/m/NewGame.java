package m;

import data.doomstat;
import data.Defines.GameMode_t;


/* MAES: This might be one of the hardest aspects of "OO-ification" to grasp.
 * This used to be a pointable function with a specific calling convention.
 * Context needs to be passed long.
 * 
 */
public class NewGame implements MenuRoutine{

	doomstat ds;
	Menu M;
	
	/** Constructor. We must give it some context to play with.
	 *  Namely, "doom's state" and the Menu object it "belongs" to.
	 * 
	 * @param ds
	 * @param M
	 */
	
	public NewGame(doomstat ds, Menu M){
	this.ds=ds;	
	this.M=M;
	}
	
	@Override
	public void execute(int choice)
	{
	    if (ds.netgame && !ds.demoplayback)
	    {
		M.StartMessage(NEWGAME,null,false);
		return;
	    }
		
	    if ( ds.gamemode == GameMode_t.commercial )
		M.SetupNextMenu(M.NewDef);
	    else
		M.SetupNextMenu(M.EpiDef);
	}

}
