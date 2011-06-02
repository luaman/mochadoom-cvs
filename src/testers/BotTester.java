package testers;

/** Tester for bots...heh goodnight.
 * 
 */

import static p.MapUtils.InterceptVector;

import java.awt.Point;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import n.BasicNetworkInterface;
import p.divline_t;
import p.intercept_t;
import p.mobj_t;
import rr.line_t;
import rr.sector_t;
import awt.OldAWTDoom;
import b.BotGame;
import b.Reachable;
import b.SearchNode_t;
import b.bot_t;
import b.BotGame.ObjKind;
import b.BotGame.ReachableGroup;
import b.BotGame.SeenNode;
import b.BotPaths.BotPath;
import b.SearchNode_t.BlockingSector;
import defines.card_t;
import doom.DoomContext;
import doom.DoomMain;
import doom.doomdata_t;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;
import doom.ticcmd_t;

public class BotTester {
	static DoomMain D4, D2, D1;
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
				receiver = D4;
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

	public static class DoomMainTester extends DoomMain {
		public player_t playerBot;
		
		public DoomMainTester(String[] args) {
		    this.Init();
		    this.VI = new OldAWTDoom(this,null,null);
		    this.DNI = new NetInterfaceTester(this);
		    this.myargv = args;
		    this.myargc = this.myargv.length; // Bump argcount +1 to maintain CheckParm behavior
		    this.Start();
		    this.bgame.BN.B_InitNodes();
		    playerBot = this.players[0];
		    playerBot.bot = new bot_t(this.bgame, playerBot);
		    playerBot.bot.walkReachable = new Reachable.WalkReachable(bgame, playerBot);
		}
		
		@Override
		public void DoomLoop() {
			System.out.println("Overriding DoomLoop");
		}
	}
	
	public static void testE1L2() {
		DoomMainTester DM=new DoomMainTester(new String[] {"", "-warp", "12"});
		
		line_t unlockLine = findLine(DM, new int[] {489, -1});
		sector_t sec = DM.bgame.BSL.sectorsTaggedBy(unlockLine).get(0);
		sec.ceilingheight = 20709376;

		
		// this this was a bug where a 2nd round bridge was chosen instead of a 1st round in FindReachableNodes
		// So B_Findpath could not find a path
		mobj_t mo = findThing(DM, -2097152, -56623104);
		DM.playerBot.mo.x = 100962571;
		DM.playerBot.mo.y = -19975541;
		
		DM.playerBot.cards[card_t.it_redcard.ordinal()] = true;
		        
		DM.bgame.FindReachableNodes(DM.playerBot);

		SearchNode_t srcNode = DM.bgame.botNodeArray[135][27];
		SearchNode_t destNode = DM.bgame.botNodeArray[58][44];
		ReachableGroup contained = (DM.playerBot.bot.botPaths.nodesContained(srcNode, destNode)); 

		BotPath path = DM.playerBot.bot.botPaths.B_FindPath(srcNode, destNode, DM.playerBot.bot.walkReachable, DM.playerBot.bot.walkReachable, new HashSet<BlockingSector>(), contained);
		assrt(contained != null && path != null);
		///////////////////
		
		
		
		
		
		
		
		
		/*SearchNode_t srcNode = DM.bgame.botNodeArray[135][27];
		SearchNode_t destNode = DM.bgame.botNodeArray[67][19];
		ReachableGroup contained = (DM.playerBot.bot.botPaths.nodesContained(srcNode, destNode)); */


		SeenNode SN = DM.bgame.new SeenNode(DM.playerBot, mo);
		SN.setKind(ObjKind.item);
		SN.findPath();
		
		DM.playerBot.bot.botPaths.B_FindPath(srcNode, destNode, DM.playerBot.bot.walkReachable, DM.playerBot.bot.walkReachable, new HashSet<BlockingSector>(), contained);
		
		ArrayList<SearchNode_t> srcNodes = DM.bgame.BN.findNodesFromPoint(DM.playerBot, new Point(DM.playerBot.mo.x, DM.playerBot.mo.y), DM.playerBot.bot.walkReachable /*finalReach*/);
		//if (srcNodes.size() == 0) // bot is really stuck!
		//	return null;

		ArrayList<SearchNode_t> destNodes = SN.getNodes();
		//if (destNodes.size() == 0) 
		//	return null;
		
		/*boolean contained = false;
		for (SearchNode_t destNode: destNodes) {
			
			//if (reachableNodes.contains(destNode)) {
				for (SearchNode_t srcNode: srcNodes) {
					if (DM.playerBot.bot.botPaths.nodesContained(srcNode, destNode)) {
						contained = true;
					}
				}
		}*/
		
		assrt((SN.path!=null) == (contained!=null) );
		System.out.println("test");

		
	}
	
	public static void testDoomI() {
		testE1L2();
	}
	
	public static void main(String[] args) {
		testDoomI();
		
		// tested with Doom II maps
		D1=new DoomMainTester(new String[] {"", "-warp", "1"});
		D2=new DoomMainTester(new String[] {"", "-warp", "2"});
		D4=new DoomMainTester(new String[] {"", "-warp", "4"});

	    int[] sh = BotGame.shiftDxDy(new Point(25,25), new Point(50,50), 5);
	    assrt(sh[0] == (int)(5/Math.sqrt(2)));
	    assrt(sh[1] == (int)(-5/Math.sqrt(2)));
	    
	    
	    

	    
	    
//	    
//	    //bot should not be able to traverse the two poles at startup of level 02
//	    Point inPoint = new Point(79352584, 120685114);
//	    Point outPoint = new Point(88774769, 121236663);
//	    D2.players[0].mo.x = inPoint.x;
//	    D2.players[0].mo.y = inPoint.y;
//	    LinkedList_t<SearchNode_t> path = new LinkedList_t<SearchNode_t>();
//	    
//		Reachable.WalkReachable wr2 = new Reachable.WalkReachable(D2.bgame, D2.players[0]);
//	    assrt(!D2.bgame.BN.B_FindPath(D2.players[0], outPoint, path, wr2, wr2));
//
//	    // make sure we cant make through the pole with this diagonal way
//	    SearchNode_t node1 = D2.bgame.botNodeArray[35][53];
//	    SearchNode_t node2 = D2.bgame.botNodeArray[36][54];
//	    assrt(!D2.bgame.BN.FindPathReachable(D2.players[0], node1, node2, true));
//	    assrt(!D2.bgame.BN.FindPathReachable(D2.players[0], D2.bgame.BN.nodeToPoint(node1), D2.bgame.BN.nodeToPoint(node2), true));
//	    
//	    // test if the first switch is reachable
//	    line_t switch1 = findLine(D2, new int[] {188, -1}); // special: 102 v1: (80740352, 77594624)
//	    SeenNode snSw1 = D2.bgame.new SeenNode(D2.players[0], switch1);
//	    snSw1.reachable = new Reachable.LineReachable(D2.bgame, D2.players[0], switch1);
//	    assrt(!snSw1.isReachable());
//	    inPoint = new Point(79181929, 81924198);
//	    D2.players[0].mo.x = inPoint.x;
//	    D2.players[0].mo.y = inPoint.y;
//	    assrt(snSw1.isReachable());
//	    
//
//	    
//	    inPoint = new Point(-48443906, 31553733);
//	    line_t switchL4 = findLine(D4, new int[] {317, -1}); // special: 102
//	    SeenNode snSW = D4.bgame.new SeenNode(D4.players[0], switchL4);
//	    snSW.reachable = new Reachable.LineReachable(D4.bgame, D4.players[0], switchL4);
//	    D4.players[0].mo.x = inPoint.x;
//	    D4.players[0].mo.y = inPoint.y;
//	    
//	    assrt(snSW.getNode() != null);
//	    
//	    assrt(snSW.isReachable());
//
//	    
//	    line_t floorL4 = findLine(D4, new int[] {733, -1}); // special: 18
//	    SeenNode snFloor = D4.bgame.new SeenNode(D4.players[0], floorL4);
//	    inPoint = new Point(-111892518, 71522245);
//	    D4.players[0].mo.x = inPoint.x;
//	    D4.players[0].mo.y = inPoint.y;
//	    snFloor.reachable = new Reachable.LineReachable(D4.bgame, D4.players[0], floorL4);
//	    assrt(snFloor.isReachable());
//	    assrt(snFloor.findPath());
//
//	    // in front of teleport
//	    inPoint = new Point(-93134091, 52253824);
//	    D4.players[0].mo.x = inPoint.x;
//	    D4.players[0].mo.y = inPoint.y;
//	    assrt(snFloor.findPath());
//
//	    
//	    line_t exitL4 = findLine(D4, new int[] {757, -1}); // special: 11
//	    SeenNode snExit = D4.bgame.new SeenNode(D4.players[0], exitL4);
//	    inPoint = new Point(-127891116, 86164298);
//	    D4.players[0].mo.x = inPoint.x;
//	    D4.players[0].mo.y = inPoint.y;
//	    snExit.reachable = new Reachable.LineReachable(D4.bgame, D4.players[0], exitL4);
//	    assrt(snExit.isReachable() /*D4.bgame.BN.seenSwitchReachable(D4.players[0], inPoint, exitL4)*/);
//	    assrt(snExit.findPath());
//	    //D4.bgame.BN.B_FindPath(D4.players[0], destPoint, path, walkReach, finalReach)
//
//	    //seems not to work if on the other side of the yellow door (because of the Demon?)
//	    /*inPoint = new Point(-120594720, 85885648);
//	    D4.players[0].mo.x = inPoint.x;
//	    D4.players[0].mo.y = inPoint.y;
//	    assrt(snExit.findPath());*/
//	    
//	    
//		DoomMain D12=new DoomMainTester(new String[] {"", "-warp", "12"});
//		inPoint = new Point(89169710, 117939298);
//		outPoint = new Point(87979765, 108184173);
//		Reachable.WalkReachable wr = new Reachable.WalkReachable(D12.bgame, D12.players[0]);
//		assrt(wr.isReachable(inPoint, outPoint));
//		line_t lineD12 = findLine(D12, new int[] {1053, 1054});
//		lineD12.frontsector.floorheight = 300000;
//		assrt(wr.isReachable(inPoint, outPoint));   // should still be considered as reachable because it is an elevator...
//	    
//	    
//		D2.P.PathTraverse(69206016, 125829120, 67108864, 125829120, 0, null); // seems OK
//		D2.P.PathTraverse(69206015, 125829119, 67108864, 125829120, 0, null); // seems to bug
//		D2.P.PathTraverse(69206017, 125829119, 67108864, 125829120, 0, null); // seems to bug
//
//		// a rare case when changeX and changeY are both true
//		D1.P.PathTraverse(132120575, -56623105, 132120576, -54525952, 0, null); // seems OK
//
//		//D2: Bug with Pathtraverse: (69206015,125829119) to (67108864,125829120)
//		//D2: Bug with Pathtraverse: (69206017,125829119) to (67108864,125829120)
//		//D2: Seems OK: (69206016,125829120) to (67108864,125829120)
//		
//		//D1: Bug with Pathtraverse: (132120575,-56623105) to (132120576,-54525952)
//	    
//	    traverseTester(D4);
//	    
//	    /*SearchNode_t srcNode = D4.bgame.BN.B_GetNodeAt(-29897546, 44775535,null);
//	    SearchNode_t destNode = D4.bgame.BN.B_GetNodeAt(-27247827, 42609240,null);
//	    LinkedList<intercept_t> q = D4.bgame.QueuePathTraverse(nodeToPoint(D4, srcNode), nodeToPoint(D4, destNode));
//	    assrt(!D4.bgame.BN.FindPathReachable(D4.players[0], srcNode, destNode, true));
//
//	    srcNode.x = 48;
//	    srcNode.y = 13;
//	    destNode.x = 47;
//	    destNode.y = 12;
//	    assrt(!D4.bgame.BN.FindPathReachable(D4.players[0], srcNode, destNode, true));
//
//	    D4.players[0].mo.x = -28202630;
//	    D4.players[0].mo.y = 48802275;
//	    
//	    path = new LinkedList_t<SearchNode_t>();
//	    assrt(!D4.bgame.BN.B_FindPath(D4.players[0], new Point(-28778704, 34686148), path, D4.bgame.BN.walkReachable, D4.bgame.BN.walkReachable)); //should not have a path from "lava" to "step too high"
//	    
//	    for (SearchNode_t sn: path) {
//	    	System.out.println(D4.bgame.BN.nodeInfo(sn));
//	    }*/
//	    
//	    /*new Thread() {
//	    	public void run() {
//	    	    D1.Start (); 		
//	    	}
//	    }.start();
//
//	    try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    
//		//D1.bgame.Got_AddBot(0,0);
//
//		/*D2=new DoomMain();
//	    D2.Init();
//	    D2.DNI = new NetInterfaceTester(D2);
//	    D2.myargv = new String[] {"", "-net", "2", "localhost"};
//	    D2.myargc = D2.myargv.length; // Bump argcount +1 to maintain CheckParm behavior
//
//	    new Thread() {
//	    	public void run() {
//	    	    D1.Start (); 		
//	    	}
//	    }.start();
//	    
//	    try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    
//	    new Thread() {
//	    	public void run() {
//	    	    D2.Start (); 		
//	    	}
//	    }.start();*/
//	    

	}
	
	public static Point nodeToPoint(DoomMain DM, SearchNode_t node) {
		return new Point(DM.bgame.BN.posX2x(node.x), DM.bgame.BN.posY2y(node.y));
	}
	public static sector_t findSector(DoomMain DM, int id/*int special, int floorheight*/) {
		for (sector_t sec: DM.LL.sectors) {
			if (/*sec.special == special && sec.floorheight == floorheight*/sec.id == id) {
				return sec;
			}
		}
		return null;
	}
	
	public static line_t findLine(DoomMain DM, int[] sidenum /*int special, int floorheight*/) {
		for (line_t line: DM.LL.lines) {
			if (line.sidenum.length == 2 && sidenum[0]==line.sidenum[0] && sidenum[1]==line.sidenum[1]) {
				return line;
			}
		}
		return null;
	}
	
	public static mobj_t findThing(DoomMain DM, int x, int y) {
		thinker_t currentthinker = DM.P.thinkercap.next;
		while (currentthinker != DM.P.thinkercap) // search through the list
													// of all thinkers
		{
			if (currentthinker.function/* .acp1 */== think_t./* (actionf_p1) */P_MobjThinker) {
				mobj_t mo = (mobj_t) currentthinker;
				if (mo.x == x && mo.y == y)
					return mo;
			}
			currentthinker = currentthinker.next;
		}
		return null;
	}

	
	public static  void traverseTester(DoomMain DM) {
		sector_t secLava = findSector(DM, 24);//findSector(DM, 16, -1572864);
		line_t lineLava = findLine(DM, new int[] {111,112});//findSector(DM, 16, -1572864);

		Point p1 = new Point((lineLava.v1x + lineLava.v2x)/2, lineLava.v1y + 10); // in lava sector
		Point p2 = new Point((lineLava.v1x + lineLava.v2x)/2, lineLava.v1y - 10); // on step
		Point p3 = new Point((lineLava.v1x + lineLava.v2x)/2, lineLava.v1y);
		
	    D4.bgame.pointOnLine(p3, lineLava);
	    
	    divline_t trac=new divline_t();
	    trac.x = p1.x;
	    trac.y = p1.y;
	    trac.dx = p2.x-p1.x;
	    trac.dy = p2.y-p1.y;

	    divline_t trac2=new divline_t();
	    trac2.x = p2.x;
	    trac2.y = p2.y;
	    trac2.dx = p1.x-p2.x;
	    trac2.dy = p1.y-p2.y;

	    divline_t dl1 = new divline_t();
	    dl1.MakeDivline(lineLava);
	    
	    InterceptVector(trac, /*new divline_t(lineLava)*/dl1);
	    InterceptVector(trac2, /*new divline_t(lineLava)*/dl1);

	    LinkedList<intercept_t> q = D4.bgame.QueuePathTraverse(p1, p2);
	    assrt(q.size()==1 && q.get(0).line == lineLava);
	    LinkedList<intercept_t> q2 = D4.bgame.CorrectedQueuePathTraverse(p1, p2);
	    assrt(q2.size()==1 && q2.get(0).line == lineLava);

	    LinkedList<intercept_t> q3 = D4.bgame.QueuePathTraverse(p1, p3);
	    assrt(q3.size()==1 && q3.get(0).line == lineLava); // on one side we will see the line
	    LinkedList<intercept_t> q4 = D4.bgame.CorrectedQueuePathTraverse(p1, p3);
	    assrt(q4.size()==1 && q4.get(0).line == lineLava);

	    LinkedList<intercept_t> q5 = D4.bgame.QueuePathTraverse(p3, p1); 
	    assrt(q5.size()==1 && q5.get(0).line == lineLava); 
	    LinkedList<intercept_t> q6 = D4.bgame.CorrectedQueuePathTraverse(p3, p1);
	    assrt(q6.size()==0); // we want to ignore a line that srcPoint is on

	    LinkedList<intercept_t> q7 = D4.bgame.QueuePathTraverse(p2, p1);
	    assrt(q7.size()==1 && q7.get(0).line == lineLava);
	    LinkedList<intercept_t> q8 = D4.bgame.CorrectedQueuePathTraverse(p2, p1);
	    assrt(q8.size()==1 && q8.get(0).line == lineLava);

	    LinkedList<intercept_t> q9 = D4.bgame.QueuePathTraverse(p3, p2);
	    assrt(q9.size()==0 /*&& q9.get(0).line == lineLava*/); //on the other side we don't see the line
	    LinkedList<intercept_t> q10 = D4.bgame.CorrectedQueuePathTraverse(p3, p2);
	    assrt(q10.size()==0);

	    LinkedList<intercept_t> q11 = D4.bgame.QueuePathTraverse(p2, p3);
	    assrt(q11.size()==0 /*&& q11.get(0).line == lineLava*/); //on the other side we don't see the line
	    LinkedList<intercept_t> q12 = D4.bgame.CorrectedQueuePathTraverse(p2, p3);
	    assrt(q12.size()==1 && q12.get(0).line == lineLava); // we want to include the line destPoint is on

	    
	    LinkedList<BlockingSector> bs1 = D4.bgame.TraversedSecLines(p1, p2);
	    assrt(bs1.size() == 1);
	    LinkedList<BlockingSector> bs2 = D4.bgame.TraversedSecLines(p2, p1);
	    assrt(bs2.size() == 1);
	    assrt(bs1.get(0).srcSect == bs2.get(0).destSect);
	    assrt(bs2.get(0).srcSect == bs1.get(0).destSect); // bs1 should be bs2 reversed
	    
	    LinkedList<BlockingSector> bs3 = D4.bgame.TraversedSecLines(p1, p3);
	    assrt(bs3.size() == 1);
	    LinkedList<BlockingSector> bs4 = D4.bgame.TraversedSecLines(p3, p1);
	    assrt(bs4.size() == 0); // because we ignore the lines we start exactly on
	    assrt(bs1.get(0).srcSect == bs3.get(0).srcSect);
	    assrt(bs1.get(0).destSect == bs3.get(0).destSect); //bs3 should be the same as bs1

	}
	static void assrt(boolean b) {
		if (!b) {
			System.out.println("Assert failed");
		}
	}

}
