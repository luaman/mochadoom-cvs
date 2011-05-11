package doom;

/** Groups functions formerly in d_game, 
 * in case you want to provide a different implementation 
 */

public interface IDoomGame {

	 public void ExitLevel ();
	 public void WorldDone ();
     public void setGameAction(gameaction_t action); 
}
