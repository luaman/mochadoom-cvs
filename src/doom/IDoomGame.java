package doom;

/** Groups functions formerly in d_game, 
 * in case you want to provide a different implementation 
 */

public interface IDoomGame {

	 void ExitLevel ();
	 void WorldDone ();
     void setGameAction(gameaction_t action);
     boolean CheckDemoStatus();

}
