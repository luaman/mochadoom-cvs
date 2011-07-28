package testers;

import static data.Defines.PU_STATIC;
import hu.HU;

import java.nio.ByteBuffer;

import m.FixedFloat;

import rr.patch_t;
import rr.vertex_t;

import data.mapvertex_t;
import defines.*;
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

public class PatchLoaderTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    
    patch_t wall=W.CachePatchName("BAL1d0");
    System.out.println(wall.leftoffset);
    
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
