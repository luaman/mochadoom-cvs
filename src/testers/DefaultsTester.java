package testers;

import hu.HU;
import m.DummyMenu;
import m.IDoomMenu;
import m.MenuMisc;
import demo.VanillaDoomDemo;
import doom.CommandLine;
import doom.DoomStatus;
import w.*;

public class DefaultsTester {

    public static void main(String[] argv) {

    DoomStatus DS=new DoomStatus();
    m.AbstractDoomMenu dummy= new DummyMenu();
    DS.M=dummy;
    DS.HU=new HU();
    DS.CM=new CommandLine(argv);
    
    MenuMisc.LoadDefaults(DS);
    
    MenuMisc.defaultfile="ghetto.cfg";
    MenuMisc.SaveDefaults(DS);

    
    }
    
}
