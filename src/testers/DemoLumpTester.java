package testers;

import static data.Defines.PU_STATIC;
import hu.HU;

import java.nio.ByteBuffer;

import m.FixedFloat;

import rr.vertex_t;

import data.mapvertex_t;
import defines.*;
import demo.VanillaDoomDemo;
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

public class DemoLumpTester {

    public static void main(String[] argv) {

    WadLoader W=new WadLoader();
    try {
		W.InitMultipleFiles(new String[] {"doom1.wad"});
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    System.out.println("NUm for DEMO1: "+W.GetNumForName("DEMO1"));
    VanillaDoomDemo demo=(VanillaDoomDemo) W.CacheLumpName("DEMO1",0,VanillaDoomDemo.class);
    System.out.println(demo);
    
    }
    
}
