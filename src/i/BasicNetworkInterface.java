package i;

import static data.Defines.*;
import static data.Limits.*;
import static doom.NetConsts.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;

import w.DoomBuffer;

import doom.DoomContext;
import doom.DoomMain;
import doom.doomcom_t;
import doom.doomdata_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: BasicNetworkInterface.java,v 1.2 2010/11/11 15:31:28 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// $Log: BasicNetworkInterface.java,v $
// Revision 1.2  2010/11/11 15:31:28  velktron
// Fixed "warped floor" error.
//
// Revision 1.1  2010/10/22 16:22:43  velktron
// Renderer works stably enough but a ton of bleeding. Started working on netcode.
//
//
// DESCRIPTION:
//
//-----------------------------------------------------------------------------

public class BasicNetworkInterface
        implements DoomNetworkInterface {


  public static final String rcsid = "$Id: BasicNetworkInterface.java,v 1.2 2010/11/11 15:31:28 velktron Exp $";

  ////////////// STATUS ///////////
  
  DoomSystemInterface I;
  DoomMain DM;
  
  // Mirror those in Doomstat.
  String[] myargv;
  int myargc;
  
  public void BasicNetworkInterface(DoomContext DC){
      this.DM=DC.DM;
      this.myargv=DM.myargv;
      this.myargc=DM.myargc;
      sw=new doomdata_t();
      // We can do that since the buffer is reused.
      // Note: this will effectively tie doomdata and the datapacket.
      swp=new DatagramPacket(sw.cached(),sw.cached().length);
      
      
  }
  
  // Bind it to the ones inside DN and DM;  
  doomdata_t netbuffer;
  doomcom_t doomcom;
  
  
  // For some odd reason...
  
  /** Changes endianness of a number */
  
  public static int ntohl(int x) {      
   return       ((((x & 0x000000ff) << 24) | 
                               ((x & 0x0000ff00) <<  8) | 
                               ((x & 0x00ff0000) >>>  8) | 
                               ((x & 0xff000000) >>> 24)));
      }

  public static short ntohs(short x) {
      return (short) (((x & 0x00ff) << 8) | ((x & 0xff00) >>> 8));
      }
        
  public static int htonl(int x) {
      return ntohl(x);
      }
  
  public static short htons(short x){
      return ntohs(x);
  }

  //void    NetSend ();
  //boolean NetListen ();


  //
  // NETWORKING
  //

  // Maes: come on, we all know it's 666.
  int DOOMPORT =  666;//(IPPORT_USERRESERVED +0x1d );

  DatagramSocket         sendsocket;
  DatagramSocket         insocket;

  /*
    struct sockaddr_in {
    short   sin_family;
    u_short sin_port;
    struct  in_addr sin_addr;
    char    sin_zero[8];
    };
   */
  
  
  // MAES: closest java equivalent
  Socket sendaddress[]=new Socket[MAXNETNODES];

//void    (*netget) (void);
//void    (*netsend) (void);

  
  interface NetFunction {
      public void invoke();
  }
  


  //
  // UDPsocket
  //
  private DatagramSocket UDPsocket ()
  {
      DatagramSocket s = null;
      String err=null;
      
      // allocate a socket
      try {
      s = new DatagramSocket();
      } catch (Exception e){
          err=e.getMessage();
      }
      //(PF_INET, SOCK_DGRAM, IPPROTO_UDP)
      if (s==null)
      I.Error ("can't create socket: %s",err);
          
      return s;
  }

  //
  // BindToLocalPort
  //
  private void
  BindToLocalPort
  ( DatagramSocket   s,
    int   port )
  {
      int         v;
      InetSocketAddress  address=InetSocketAddress.createUnresolved("Mochadoom", port);
      
     // memset (&address, 0, sizeof(address));
    //  address.sin_family = AF_INET;
    //  address.sin_addr.s_addr = INADDR_ANY;
              try{
                  s.bind(address);
              } catch (Exception e){
                  I.Error ("BindToPort: bind: %s", e.getMessage());
              }
  }


  // To use inside packetsend. Declare once and reuse to save on heap costs.
  private doomdata_t  sw;
  // We also reuse always the same DatagramPacket, "peged" to sw's byte buffer.
  private DatagramPacket swp;
  
  
  //
  // PacketSend
  //
  void PacketSend ()
  {
      int     c;
      
                  
      // byte swap: so this is transferred as little endian? Ugh
      sw.checksum = htonl(netbuffer.checksum);
      sw.player = netbuffer.player;
      sw.retransmitfrom = netbuffer.retransmitfrom;
      sw.starttic = netbuffer.starttic;
      sw.numtics = netbuffer.numtics;
      for (c=0 ; c< netbuffer.numtics ; c++)
      {
      sw.cmds[c].forwardmove = netbuffer.cmds[c].forwardmove;
      sw.cmds[c].sidemove = netbuffer.cmds[c].sidemove;
      sw.cmds[c].angleturn = htons(netbuffer.cmds[c].angleturn);
      sw.cmds[c].consistancy = htons(netbuffer.cmds[c].consistancy);
      sw.cmds[c].chatchar = netbuffer.cmds[c].chatchar;
      sw.cmds[c].buttons = netbuffer.cmds[c].buttons;
      }
          
      //printf ("sending %i\n",gametic);      
      
      // MAES: This will force the buffer to be refreshed.
      sw.pack();
      
      // The socket already contains the address it needs,
      // and the packet's buffer is already modified. Send away.
    
      try {
        sendsocket.send(swp);
    } catch (IOException e) {
        I.Error ("SendPacket error: %s",e.getMessage());
    }
      
      /*c = sendto (sendsocket , &sw, doomcom.datalength
          ,0,(void *)&sendaddress[doomcom.remotenode]
          ,sizeof(sendaddress[doomcom.remotenode]));*/
      
      //  if (c == -1)
      //      I_Error ("SendPacket error: %s",strerror(errno));
  }

  // Used inside PacketGet
  private boolean first=true;

  //
  // PacketGet
  //
  void PacketGet ()
  {
      int         i;
      int         c;
      Socket  fromaddress; // Only IPV4. Yeah.
      int         fromlen;
      doomdata_t      sw;
                  
      //fromlen = fromaddress.sizeof(fromaddress);
      fromlen = 4;
      // Receive back into swp.
      try {
      insocket.receive(swp);
      
//      c = recvfrom (insocket, &sw, sizeof(sw), 0
//            , (struct sockaddr *)&fromaddress, &fromlen );
      } catch (Exception e)
      { if (e.getClass()!=java.nio.channels.IllegalBlockingModeException.class){
          I.Error ("GetPacket: %s",e.getStackTrace());
          }
      // Not as bad, as it seems.
          doomcom.remotenode = -1;       // no packet
          return;
          }

      {
      //static int first=1;
      if (first){
          sb.setLength(0);
          sb.append("len=");
          sb.append(c);
          sb.append(":p=[0x");
          sb.append(Integer.toHexString(sw.checksum));
          sb.append(" 0x");
          sb.append(DoomBuffer.getBEInt(sw.retransmitfrom,sw.starttic,sw.player,sw.numtics));
          System.out.println(sb.toString());
      first = false;
      }
      }

      // find remote node number
      for (i=0 ; i<doomcom.numnodes ; i++)
      if ( fromaddress.getInetAddress().equals(sendaddress[i].getInetAddress()))
          break;

      if (i == doomcom.numnodes)
      {
      // packet is not from one of the players (new game broadcast)
      doomcom.remotenode = -1;       // no packet
      return;
      }
      
      doomcom.remotenode = (short) i;            // good packet from a game player
      doomcom.datalength = (short) c;
      
      // byte swap
      netbuffer.checksum = ntohl(sw.checksum);
      netbuffer.player = sw.player;
      netbuffer.retransmitfrom = sw.retransmitfrom;
      netbuffer.starttic = sw.starttic;
      netbuffer.numtics = sw.numtics;

      for (c=0 ; c< netbuffer.numtics ; c++)
      {
      netbuffer.cmds[c].forwardmove = sw.cmds[c].forwardmove;
      netbuffer.cmds[c].sidemove = sw.cmds[c].sidemove;
      netbuffer.cmds[c].angleturn = ntohs(sw.cmds[c].angleturn);
      netbuffer.cmds[c].consistancy = ntohs(sw.cmds[c].consistancy);
      netbuffer.cmds[c].chatchar = sw.cmds[c].chatchar;
      netbuffer.cmds[c].buttons = sw.cmds[c].buttons;
      }
  }


/* Doesn't seem to be used anywhere
  
  private int GetLocalAddress ()
  {
      String        hostname;
      HostEntry hostentry;  // host information entry
      int         v;

      // get local address
      v = gethostname (hostname);
      if (v == -1)
      I_Error ("GetLocalAddress : gethostname: errno %d",errno);
      
      hostentry = gethostbyname (hostname);
      if (!hostentry)
      I_Error ("GetLocalAddress : gethostbyname: couldn't get local host");
          
      return *(int *)hostentry.h_addr_list[0];
  }*/


  //
  // I_InitNetwork
  //
    
    @Override
    public void InitNetwork() {
        boolean     trueval = true;
        int         i;
        int         p;
        struct hostent* hostentry;  // host information entry
        
        doomcom = new doomcom_t();
                
        // set up for network
        i = DM.CheckParm ("-dup");
        if ((i!=0) && i< DM.myargc-1)
        {
        doomcom.ticdup = (short) (DM.myargv[i+1].charAt(0)-'0');
        if (doomcom.ticdup < 1)
            doomcom.ticdup = 1;
        if (doomcom.ticdup > 9)
            doomcom.ticdup = 9;
        }
        else
        doomcom. ticdup = 1;
        
        if (DM.CheckParm ("-extratic")!=0)
        doomcom. extratics = 1;
        else
        doomcom. extratics = 0;
            
        p = DM.CheckParm ("-port");
        if ((p!=0) && (p<DM.myargc-1))
        {
        DOOMPORT = Integer.parseInt(DM.myargv[p+1]);
        System.out.println ("using alternate port "+DOOMPORT);
        }
        
        // parse network game options,
        //  -net <consoleplayer> <host> <host> ...
        i = DM.CheckParm ("-net");
        if (i==0)
        {
        // single player game
        DM.netgame = false;
        doomcom.id = DOOMCOM_ID;
        doomcom.numplayers = doomcom.numnodes = 1;
        doomcom.deathmatch = 0; // false
        doomcom.consoleplayer = 0;
        return;
        }

        netsend = PacketSend;
        netget = PacketGet;
        DM.netgame = true;

        // parse player number and host list
        doomcom.consoleplayer = (short) (myargv[i+1].charAt(0)-'1');

        doomcom.numnodes = 1;  // this node for sure
        
        i++;
        while (++i < myargc && myargv[i].charAt(0) != '-')
        {
        sendaddress[doomcom.numnodes].sin_family = AF_INET;
        sendaddress[doomcom.numnodes].sin_port = htons(DOOMPORT);
        if (myargv[i][0] == '.')
        {
            sendaddress[doomcom.numnodes].sin_addr.s_addr 
            = inet_addr (myargv[i]+1);
        }
        else
        {
            hostentry = gethostbyname (myargv[i]);
            if (!hostentry)
            I_Error ("gethostbyname: couldn't find %s", myargv[i]);
            sendaddress[doomcom.numnodes].sin_addr.s_addr 
            = *(int *)hostentry.h_addr_list[0];
        }
        doomcom.numnodes++;
        }
        
        doomcom.id = DOOMCOM_ID;
        doomcom.numplayers = doomcom.numnodes;
        
        // build message to receive
        insocket = UDPsocket ();
        BindToLocalPort (insocket,htons(DOOMPORT));
        ioctl (insocket, FIONBIO, &trueval);

        sendsocket = UDPsocket ();

    }

    @Override
    public void NetCmd() {
        if (DM.doomcom.command == CMD_SEND)
        {
        netsend ();
        }
        else if (doomcom.command == CMD_GET)
        {
        netget ();
        }
        else
        I_Error ("Bad net cmd: %i\n",doomcom.command);

    }

    // Instance StringBuilder
    private StringBuilder sb=new StringBuilder();
    
}
