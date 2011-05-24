package testers;

import hu.HU;
import m.DummyMenu;
import m.IDoomMenu;
import m.MenuMisc;
import demo.VanillaDoomDemo;
import doom.DoomStatus;
import w.*;

public class DefaultsTester {

    public static void main(String[] argv) {

    DoomStatus DS=new DoomStatus();
    IDoomMenu dummy= new DummyMenu();
    DS.M=dummy;
    DS.HU=new HU();
    
    MenuMisc.LoadDefaults(DS);
    
    MenuMisc.defaultfile="nigger.cfg";
    MenuMisc.SaveDefaults(DS);

    
    }
    
}
