package testers;

import hu.HU;

import java.io.IOException;
import java.nio.ByteBuffer;

import data.doomstat;
import data.Defines.GameMission_t;

import w.*;

/** This is a very simple tester for the WadLoader and HU modules.
 *  We use the same exact methods used in the C source code, only
 *  with a more OO approach.
 * 
 * 
 */

public class WadLoaderTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    System.out.println("NUm for E1M1: "+W.GetNumForName("E1M1"));
    System.out.println("NUm for SECTORS: "+W.GetNumForName("SECTORS"));
    System.out.println("NUm for SSECTORS: "+W.GetNumForName("SSECTORS"));
   
    // We prepare a ByteBuffer to receive a "SECTORS" object. Deserializing it is
    // another matter.
    ByteBuffer bb=W.CacheLumpName("SECTORS", 0);
    System.out.println("Num for THINGS: "+W.GetNumForName("THINGS"));
    doomstat ds = new doomstat();
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    
    // Testing "Heads Up" clases. Notice how we pass "doomstate" and "WadLoader" as instances,
    // instead of globals. OO all the way, baby!
    
    HU hu=new HU(ds,W);
    hu.Init();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
