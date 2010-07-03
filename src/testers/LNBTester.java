package testers;

import hu.HU;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

import rr.patch_t;

import data.doomstat;
import data.Defines.GameMission_t;

import v.SimpleRenderer;
import w.*;

/** Tests Video buffer. Don't misunderstand this, there's no graphics output yet.
 *  Just tests bitblts etc.
 */

public class LNBTester {

    public static void main(String[] argv) throws Exception {
    WadLoader W=new WadLoader();
    W.InitMultipleFiles(new String[] {"doom1.wad"});
    System.out.println("Total lumps read: "+W.numlumps);
   
    System.out.println("Num for TITLEPIC: "+W.GetNumForName("TITLEPIC"));
    ByteBuffer bb=W.CacheLumpName("TITLEPIC", 0);
    patch_t titlepic=new patch_t();
    titlepic.unpack(bb);
    bb=W.CacheLumpName("PISGC0", 0);
    patch_t pistol=new patch_t();
    pistol.unpack(bb);
    patch_t troll= new patch_t();
    troll.unpack(W.CacheLumpName("TROOA1", 0));
    SimpleRenderer V=new SimpleRenderer(640,640);
    V.Init();
    int count=0;
    long timea=System.nanoTime();
    for (int i=0;i<100;i++){
        for (int s=0;s<4;s++){
            for (int y=0;y<1/*V.getHeight()/titlepic.height*/;y++){
            for (int x=0;x<V.getWidth()/titlepic.width;x++){
                
                V.DrawPatchFaster(x*titlepic.width, y*titlepic.height,s, titlepic);
                    }
            }
            count++;
        }
    }
     
    long timeb=System.nanoTime();
    System.out.println("Time for single full-screen render:" +(timeb-timea)/(count*1e06)+ " ms");

    
    V.DrawPatchFaster(0, 0,0, pistol);
    V.DrawPatchFaster(20, 60,0, troll);
    byte[] dump=new byte[V.getHeight()*V.getWidth()];
    V.GetBlock(0, 0, 0, V.getWidth(), V.getHeight(), dump);
    
    System.out.println(dump[0]);
    
    BufferedImage b=new BufferedImage(V.getWidth(),V.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
    
    int[] tmp=new int[dump.length];
    for (int i=0;i<dump.length;i++){
        tmp[i]=dump[i];
    }

    b.getRaster().setPixels(0, 0, V.getWidth(),V.getHeight(), tmp);
    
    File outputFile =
        new File(
            "TITLEPIC.png");
    ImageIO.write(b, "PNG", outputFile);
  
    count=0;
    timea=System.nanoTime();
    for (int i=0;i<10;i++){
    V.Unscramble(0);
    V.Unscramble(1);
    V.Unscramble(2);
    V.Unscramble(3);
    count+=4;
    }
    timeb=System.nanoTime();

    
    V.GetBlock(0, 0, 0, V.getWidth(), V.getHeight(), dump);
    V.Unscramble(0, dump);
    
    System.out.println("Time for single transposing:" +(timeb-timea)/(count*1e06)+ " ms");
    for (int i=0;i<dump.length;i++){
        tmp[i]=dump[i];
    }

    b.getRaster().setPixels(0, 0, V.getWidth(),V.getHeight(), tmp);
    
    outputFile =
        new File(
            "U1_TITLEPIC.png");
    ImageIO.write(b, "PNG", outputFile);
    V.Unscramble(0);
    
    count=0;
    timea=System.nanoTime();
    for (int i=0;i<10;i++){
    V.Unscramble(0,dump);
    V.Unscramble(1,dump);
    V.Unscramble(2,dump);
    V.Unscramble(3,dump);
    count+=4;
    }
    timeb=System.nanoTime();     
    
    V.Unscramble(0,dump);
    V.GetBlock(0, 0, 0, V.getWidth(), V.getHeight(), dump);
    System.out.println("Time for single transposing (with copy):" +(timeb-timea)/(count*1e06)+ " ms");
    
    for (int i=0;i<dump.length;i++){
        tmp[i]=dump[i];
    }

    b.getRaster().setPixels(0, 0, V.getWidth(),V.getHeight(), tmp);
    
    outputFile =
        new File(
            "U2_TITLEPIC.png");
    ImageIO.write(b, "PNG", outputFile);
     
         }
}
    
   
    
