package testers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

import n.BasicNetworkInterface;
import doom.DoomContext;
import doom.DoomMain;
import doom.doomdata_t;
import doom.ticcmd_t;

/** Tester for Net included from DW's work
 * 
 * $Log
 *
 */

public class NetTester {
	
	static DoomMain D1, D2;
	static Object synchronizer = new Object();
	
	public static class NetInterfaceTester extends BasicNetworkInterface {
		DoomContext DC;
		
		public NetInterfaceTester(DoomContext DC) {
			super(DC);
			// TODO Auto-generated constructor stub
		}

	  private doomdata_t  sendData = new doomdata_t();
	  private doomdata_t  recvData = new doomdata_t();
		
		@Override  
		public void sendSocketPacket(DatagramSocket ds, DatagramPacket dp) throws IOException {
			DoomMain receiver;
			
			if (DM.consoleplayer == 0) {
				receiver = D2;
			}
			else {
				receiver = D1;
			}
			
			NetInterfaceTester N = (NetInterfaceTester)receiver.DNI;
			N.addReceivedPacket(dp);
			
			sendData.unpack(dp.getData());
			//System.out.println("Player "+DM.consoleplayer+" SENT: "+dataToStr(sendData));
		}
		
		/*class Elem<E> {
			E e;
			E next = null;
			public Elem(E e) {
				this.e = e;
			}
		}
		
		Elem<DatagramPacket> head = new Elem<DatagramPacket>(null);*/
		
		//ArrayList<DatagramPacket> al = new ArrayList<DatagramPacket>();
		LinkedList<DatagramPacket> al = new LinkedList<DatagramPacket>();
		
		public void addReceivedPacket(DatagramPacket dp) {
			synchronized(synchronizer) {
				al.addLast(dp);
			}
		}
		
		@Override  
		public void socketGetPacket(DatagramSocket ds, DatagramPacket dp) throws IOException {
			synchronized(synchronizer) {
				if (al.size() < 1)
					throw new SocketTimeoutException();
				DatagramPacket pop = al.removeFirst();
				dp.setData(pop.getData());
				dp.setLength(pop.getLength());
				dp.setSocketAddress(pop.getSocketAddress());
				
				recvData.unpack(dp.getData());
			}
			
			//System.out.println("Player "+DM.consoleplayer+" RECV: "+dataToStr(recvData));
		}
		
		public String dataToStr(doomdata_t dt) {
			StringBuffer sb = new StringBuffer();
			sb.append("STIC: "+dt.starttic+" NTIC: "+dt.numtics+" CMDS:\r\n");
			for (int i = 0; i < dt.numtics; i++) {
				ticcmd_t tc = dt.cmds[i];
				sb.append("    FMOVE: "+tc.forwardmove+" CONS: "+tc.consistancy+"\r\n");
			}
			return sb.toString();
				
		}
		
	}

	
	public static void main(String[] args) {
		D1=new DoomMain();
	    D1.Init();
	    D1.DNI = new NetInterfaceTester(D1);
	    D1.myargv = new String[] {"", "-net", "1", "localhost"};
	    D1.myargc = D1.myargv.length; // Bump argcount +1 to maintain CheckParm behavior

		D2=new DoomMain();
	    D2.Init();
	    D2.DNI = new NetInterfaceTester(D2);
	    D2.myargv = new String[] {"", "-net", "2", "localhost"};
	    D2.myargc = D2.myargv.length; // Bump argcount +1 to maintain CheckParm behavior

	    new Thread() {
	    	public void run() {
	    	    D1.Start (); 		
	    	}
	    }.start();
	    
	    try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    new Thread() {
	    	public void run() {
	    	    D2.Start (); 		
	    	}
	    }.start();

	}

}
