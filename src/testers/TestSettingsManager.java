package testers;

import m.VarsManager;

public class TestSettingsManager {

    public static void main(String argv[]){
    
        VarsManager SM=new VarsManager();
        SM.LoadDefaults("default.cfg");
        
        System.out.println(SM.getSetting("use_mouse").getBoolean());
        
        SM.putSetting("jump_height", "56", false);
        
        System.out.println(SM.getSetting("jump_height").getInteger());
        
        System.out.println(SM.getSetting("use_mouse").getBoolean());
        
        SM.putSetting("crap_setting", "false", true);
        
        System.out.println(SM.getSetting("crap_setting").getInteger());
        System.out.println(SM.getSetting("key_left").getChar());
        
        SM.SaveDefaults("default.cfg");

        
    }
}
