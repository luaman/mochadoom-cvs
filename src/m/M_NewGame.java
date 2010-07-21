package m;

import data.doomstat;
import data.Defines.GameMode_t;
import doom.DoomContext;
import static doom.englsh.NEWGAME;


/* MAES: This might be one of the hardest aspects of "OO-ification" to grasp.
 * This used to be a pointable function with a specific calling convention.
 * Now, there are Method classes in Java...but these are allegedly very slow.
 * So we'll go with the traditional "invokable interface" drill.
 * Context needs to be passed along. 
 * 
 */
public class M_NewGame implements MenuRoutine{

	doomstat DS;
	Menu M;
	
	/** Constructor. We must give it some context to play with.
	 *  Namely, "doom's state" and the Menu object it "belongs" to.
	 * 
	 * @param ds
	 * @param M
	 */
	
	public M_NewGame(DoomContext DC, Menu M){
	this.DS=DC.DS;	
	this.M=M;
	}
	
	@Override
	public void invoke(int choice)
	{
	    if (DS.netgame && !DS.demoplayback)
	    {
		M.StartMessage(NEWGAME,null,false);
		return;
	    }
		
	    if ( DS.gamemode == GameMode_t.commercial )
		M.SetupNextMenu(M.NewDef);
	    else
		M.SetupNextMenu(M.EpiDef);
	}

}
