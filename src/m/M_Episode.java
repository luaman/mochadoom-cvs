package m;

import data.doomstat;
import data.Defines.GameMode_t;
import doom.DoomContext;
import static doom.englsh.SWSTRING;


/* MAES: This might be one of the hardest aspects of "OO-ification" to grasp.
 * This used to be a pointable function with a specific calling convention.
 * Now, there are Method classes in Java...but these are allegedly very slow.
 * So we'll go with the traditional "invokable interface" drill.
 * Context needs to be passed along. 
 * 
 */
public class M_Episode implements MenuRoutine{

	doomstat DS;
	Menu M;
	
	/** Constructor. We must give it some context to play with.
	 *  Namely, "doom's state" and the Menu object it "belongs" to.
	 * 
	 * @param ds
	 * @param M
	 */
	
	public M_Episode(DoomContext DC, Menu M){
	this.DS=DC.DS;	
	this.M=M;
	}
	
	@Override
	public void invoke(int choice)
	    {
	    
	        if ( (DS.gamemode == GameMode_t.shareware)
	         && (choice!=0))
	        {
	        M.StartMessage(SWSTRING,null,false);
	        M.SetupNextMenu(M.ReadDef1);
	        return;
	        }

	        // Yet another hack...
	        if ((DS.gamemode == GameMode_t. registered)
	         && (choice > 2))
	        {
	          System.err.print("M_Episode: 4th episode requires UltimateDOOM\n");
	          choice = 0;
	        }
	         
	        M.epi = choice;
	        M.SetupNextMenu(M.NewDef);
	     //   M.SetupNextMenu(M.OptionsDef);
	    }

}
