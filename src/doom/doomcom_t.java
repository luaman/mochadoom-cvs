package doom;

public class doomcom_t {
	
		public doomcom_t(){
			
		}

        // Supposed to be DOOMCOM_ID?
        // Maes: was "long", but they intend 32-bit "int" here. Hurray for C's consistency!
        int        id;
        
        // DOOM executes an int to execute commands.
        short       intnum;     
        // Communication between DOOM and the driver.
        // Is CMD_SEND or CMD_GET.
        short       command;
        // Is dest for send, set by get (-1 = no packet).
        short       remotenode;
        
        // Number of bytes in doomdata to be sent
        short       datalength;

        // Info common to all nodes.
        // Console is allways node 0.
        short       numnodes;
        // Flag: 1 = no duplication, 2-5 = dup for slow nets.
        short       ticdup;
        // Flag: 1 = send a backup tic in every packet.
        short       extratics;
        // Flag: 1 = deathmatch.
        short       deathmatch;
        // Flag: -1 = new game, 0-5 = load savegame
        short       savegame;
        short       episode;    // 1-3
        short       map;        // 1-9
        short       skill;      // 1-5

        // Info specific to this node.
        short       consoleplayer;
        short       numplayers;
        
        // These are related to the 3-display mode,
        //  in which two drones looking left and right
        //  were used to render two additional views
        //  on two additional computers.
        // Probably not operational anymore.
        // 1 = left, 0 = center, -1 = right
        short       angleoffset;
        // 1 = drone
        short       drone;      

        // The packet data to be sent.
        doomdata_t      data;
        
    }
