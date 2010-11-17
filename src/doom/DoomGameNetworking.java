package doom;

/** Doom is actually tied to its networking module.
 *  Therefore, no matter where and how you implement it, these functions
 *  need to be callable from withing many modules.
 *  
 *  This is the so called "game networking" which is internal and game-specific,
 *  and not system networking which deals with the low level sockets and packet 
 *  stuff. You'll need DoomSystemNetworking for that one.
 *  
 * @author Velktron
 *
 */

public interface DoomGameNetworking {
	
	public void TryRunTics();
	
	/**
	 * NetUpdate
	 * Builds ticcmds for console player,
	 * sends out a packet
	 */

	public void NetUpdate ();
	
	public doomcom_t getDoomCom();
	
	public void setDoomCom(doomcom_t doomcom);
	
	public int getTicdup();

	public void setTicdup(int ticdup);

}
