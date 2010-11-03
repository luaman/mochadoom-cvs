package testers;

import static data.Defines.PU_STATIC;
import static m.fixed_t.FRACBITS;
import hu.HU;

import java.io.IOException;
import java.nio.ByteBuffer;

import m.FixedFloat;

import rr.vertex_t;

import data.mapvertex_t;
import data.Defines.GameMission_t;
import doom.DoomContext;
import doom.DoomStatus;

import utils.C2JUtils;
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
   int lump=W.GetNumForName("VERTEXES");
   System.out.println("NUm for VERTEXES: "+W.GetNumForName("VERTEXES"));
    // We prepare a ByteBuffer to receive a "SECTORS" object. Deserializing it is
    // another matter.
    ByteBuffer bb=W.CacheLumpName("SECTORS", 0).getBuffer();
    System.out.println("Num for THINGS: "+W.GetNumForName("THINGS"));
    DoomStatus ds = new DoomStatus();
    ds.gameepisode=1;
    ds.gamemap=1;
    ds.gamemission=GameMission_t.doom;
    
    DoomContext DC=new DoomContext();
    //DC.DM=DC;
    DC.W=W;
    
    // Testing "Heads Up" clases. Notice how we pass "doomstate" and "WadLoader" as instances,
    // instead of globals. OO all the way, baby!
    
    HU hu=new HU(DC);
    hu.Init();
    
    // Make a lame-ass attempt at loading some vertexes.
    
    // Determine number of lumps:
    //  total lump length / vertex record length.
    int numvertexes = W.LumpLength (lump) / mapvertex_t.sizeOf();

    // Allocate zone memory for buffer.
    vertex_t[] vertexes = new vertex_t[numvertexes];
    // Init those "vertexes"
    C2JUtils.initArrayOfObjects(vertexes, vertex_t.class);

    // Load data into cache.
    // MAES: we now have a mismatch between memory/disk: in memory, we need an array.
    // On disk, we have a single lump/blob. Thus, we need to find a way to deserialize this...
     W.CacheLumpNumIntoArray(lump,PU_STATIC,vertexes,vertex_t.class);
     W.CacheLumpNumIntoArray(lump,PU_STATIC,vertexes,vertex_t.class);
   // Copy and convert vertex coordinates,
    // internal representation as fixed.
    for (int i=0 ; i<numvertexes ; i++)
    {
        //vertexes[i].x = ml[i].x<<FRACBITS;
        //vertexes[i].y = ml[i].y<<FRACBITS;
       System.out.println(vertexes[i].x+" , "+vertexes[i].y+" "+FixedFloat.toDouble(vertexes[i].x)+" , "+FixedFloat.toDouble(vertexes[i].y));
        //System.out.println(ml[i].x+" , "+ml[i].y+" "+FPTest.Value(vertexes[i].x)+" , "+FPTest.Value(vertexes[i].y));
    }

    
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
