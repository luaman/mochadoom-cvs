package demo;

import static data.Defines.PU_STATIC;
import static data.Defines.VERSION;
import static data.Limits.MAXPLAYERS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import utils.C2JUtils;
import w.CacheableDoomObject;
import w.DoomBuffer;
import defines.skill_t;
import doom.gameaction_t;
import doom.ticcmd_t;

public class VanillaDoomDemo implements IDoomDemo,CacheableDoomObject{

	 // This stuff is in the demo header, in the order it appears
	 // However everything is byte-sized when read from disk or to memory.
	 public int version; 
	 skill_t skill;
	 int episode;
	 int map;
	 boolean deathmatch;
	 boolean respawnparm;
     boolean fastparm;
     boolean nomonsters;
     int consoleplayer;
     boolean[] playeringame; // normally MAXPLAYERS (4) for vanilla.
	 
     //  After that, demos contain a sequence of ticcmd_t's to build dynamically at
     // load time or when recording. This abstraction allows arbitrary demo sizes
     // and easy per-step handling, and even changes/extensions. Just make sure
     // that ticcmd_t's are serializable!
     // Also, the format used in demo lumps is NOT the same as in datagrams/network
     // (e.g. there is no consistency) and their handling is modified.
     
     ticcmd_t[] commands;
     
	 public void unpack(ByteBuffer b){
	 
     int demo_p = b.position();
     int len=b.remaining();
     version=b.get();		 
     skill = skill_t.values()[b.get()]; 
     episode = b.get(); 
     map = b.get(); 
     deathmatch = b.get()!=0;
     respawnparm = b.get()!=0;
     fastparm = b.get()!=0;
     nomonsters = b.get()!=0;
     consoleplayer = b.get();
     
     playeringame=new boolean[MAXPLAYERS];
     
     for (int i=0 ; i<MAXPLAYERS ; i++) 
     playeringame[i] = b.get()!=0;
     
     // Header info for vanilla should be 9 bytes.
     // 1 byte at the end is the end-demo marker
     // So valid vanilla demos should have sizes that
     // fit the formula 10+4n, since each vanilla 
     // demo ticcmd_t is 4 bytes.
     int lens=(b.limit()-b.position()-1)/4;
     
     boolean vanilla=(b.limit()==(10+4*lens));
     
     this.commands=new ticcmd_t[lens];
     C2JUtils.initArrayOfObjects(this.commands, ticcmd_t.class);
     
     try {
		DoomBuffer.readObjectArray(b, this.commands, lens);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

     }
	
}
