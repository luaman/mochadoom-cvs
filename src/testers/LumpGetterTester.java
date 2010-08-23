package testers;

import static data.Defines.PU_STATIC;
import static m.fixed_t.FRACBITS;
import hu.HU;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.nio.ByteBuffer;

import p.Playfield;

import m.FixedFloat;
import m.random;

import rr.vertex_t;

import data.doomstat;
import data.mapvertex_t;
import data.Defines.GameMission_t;
import data.Defines.GameMode_t;
import data.Defines.skill_t;
import doom.DoomContext;
import doom.event_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;

import utils.C2JUtils;
import v.SimpleRenderer;
import w.*;

/** This is a very simple tester for the End Level screen drawer.
 * 
 * 
 */

public class LumpGetterTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    int random;
    int TESTS=1000000;
    int[] tests=new int[TESTS];
    long hash;
    
    long a=System.nanoTime();
    for (int i=0;i<TESTS;i++){
        random =(int) (Math.random()*W.numlumps);
        String what=W.lumpinfo[random].name;
        hash=W.lumpinfo[random].hash;
        tests[i] = W.CheckNumForName(what);
        if (!W.lumpinfo[tests[i]].name.equalsIgnoreCase(what)) System.err.println("Mismatch");
    }
    
    long b=System.nanoTime();
    
    System.out.println(b-a);
           
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
