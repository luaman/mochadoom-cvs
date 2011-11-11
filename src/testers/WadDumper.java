package testers;

import static data.Defines.PU_STATIC;
import hu.HU;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import m.FixedFloat;

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

public class WadDumper {

    public static void main(String[] argv) {
        try {
    IWadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    BufferedOutputStream bos;
    
    int numlumps=W.NumLumps();
    
    for (int i=0;i<numlumps;i++){
        byte[] crap=W.ReadLump(i);
        String name=W.GetNameForLump(i);
        File f=new File(String.format("%s.lmp",name));
        bos=new BufferedOutputStream(new FileOutputStream(f));
        bos.write(crap);
        bos.close();        
        }

    
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
