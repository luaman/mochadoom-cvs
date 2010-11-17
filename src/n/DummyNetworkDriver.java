package n;

import java.net.DatagramPacket;

import doom.DoomContext;
import doom.DoomGameNetworking;
import doom.DoomMain;
import doom.NetConsts;
import doom.doomcom_t;
import doom.doomdata_t;
import i.DoomStatusAware;
import i.DoomSystemInterface;

/** Does nothing.
 *  Allows running single-player games without an actual network.
 *  Hopefully, it will be replaced by a real UDP-based driver one day.
 *  
 * @author Kaptain Zyklon
 *
 */

public class DummyNetworkDriver implements NetConsts,DoomSystemNetworking, DoomStatusAware{

	////////////// STATUS ///////////

	DoomSystemInterface I;
	DoomMain DM;
	DoomGameNetworking DGN;

	// Mirror those in Doomstat.
	String[] myargv;
	int myargc;

	public DummyNetworkDriver(DoomContext DC){
		updateStatus(DC);
	}

	@Override
	public void InitNetwork() {
		doomcom_t doomcom =new doomcom_t();
		doomcom.id=DOOMCOM_ID;
		doomcom.ticdup=1;

		// single player game
		DM.netgame = false;
		doomcom.id = DOOMCOM_ID;
		doomcom.numplayers = doomcom.numnodes = 1;
		doomcom.deathmatch = 0;
		doomcom.consoleplayer = 0;
		DGN.setDoomCom(doomcom);
	}

	@Override
	public void NetCmd() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateStatus(DoomContext DC) {
		this.DM=DC.DM;
		this.DGN=DC.DM;
		this.myargv=DM.myargv;
		this.myargc=DM.myargc;
	}

}
