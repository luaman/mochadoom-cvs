package testers;

import w.*;

/** This is a very simple tester for the End Level screen drawer.
 * 
 * 
 */

public class LumpGetterTester {

    public static void main(String[] argv) {
        try {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"C:\\iwads\\doom1.wad"});
    //W.AddFile("bitter.wad");
    System.out.println("Total lumps read: "+W.numlumps);
    int random;
    int TESTS=10000;
    int[] tests=new int[TESTS];
    int hash;
    
    long a=System.nanoTime();
    for (int i=0;i<TESTS;i++){
        random =(int) (Math.random()*W.numlumps);
        String what=W.lumpinfo[random].name;
        hash=W.lumpinfo[random].hash;
        tests[i] = W.CheckNumForName3(what);
        if (!W.lumpinfo[tests[i]].name.equalsIgnoreCase(what)) System.err.println("Mismatch");
    }
    
    long b=System.nanoTime();
    
    System.out.println(b-a);
           
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
