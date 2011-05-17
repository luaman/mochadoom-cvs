package testers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import savegame.VanillaDSG;
import savegame.VanillaDSGHeader;

import m.MenuMisc;
import demo.VanillaDoomDemo;
import doom.DoomStatus;
import w.*;

public class SaveGameHeaderTester {

    public static void main(String[] argv) throws FileNotFoundException {

    ByteBuffer buf=MenuMisc.ReadFile("doomsav1.dsg");
    DoomFile f=new DoomFile("doomsav1.dsg","r");
    DoomStatus DS=new DoomStatus();
    
    VanillaDSGHeader vdsg=new VanillaDSGHeader();
    try {
        vdsg.read(f);
    
    
    f.seek(0);
    VanillaDSG DSG=new VanillaDSG();
    DSG.updateStatus(DS);
    DS.playeringame[0]=true;
    DSG.read(f);
    
    
    //    W.AddFile("bitter.wad");

    System.out.println(vdsg);
    
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    }
    
}
