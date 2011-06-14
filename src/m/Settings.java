package m;

import static data.Defines.*;
import static doom.englsh.*;
import utils.C2JUtils;
import doom.DoomStatus;


/** An anumeration with all possible settings and their defaults 
 *  Given Doom's status as a parameter, they can "autoapply" themselves.
 *  The can also be modified by reading from a file, or be used to
 *  generate a string to be written to a file.
 *  
 */

public enum Settings {
    mouse_sensitivity(5),
    sfx_volume(8),
   music_volume(8),
   show_messages(1),
   key_right(KEY_RIGHTARROW),
   key_left(KEY_LEFTARROW),
   key_up('w'),
   key_down('s'),
   key_strafeleft('a'),
   key_straferight('d'),
   key_fire(KEY_RCTRL),
   key_use(' '),
   key_strafe(KEY_RALT),
   key_speed(KEY_RSHIFT),
   use_mouse(1),
   mouseb_fire(0),
   mouseb_strafe(1),
   mouseb_forward(2),
   use_joystick( 0),
   joyb_fire(0),
   joyb_strafe(1),
   joyb_use(3),
   joyb_speed(2),
   screenblocks(10),
   detaillevel(0),
   snd_channels(6),
   usegamma(0),
   mb_used(2),
   chatmacro0(HUSTR_CHATMACRO0 ),
   chatmacro1(HUSTR_CHATMACRO1 ),
   chatmacro2( HUSTR_CHATMACRO2 ),
   chatmacro3(HUSTR_CHATMACRO3 ),
   chatmacro4( HUSTR_CHATMACRO4 ),
   chatmacro5( HUSTR_CHATMACRO5 ),
   chatmacro6(HUSTR_CHATMACRO6 ),
   chatmacro7( HUSTR_CHATMACRO7 ),
   chatmacro8(HUSTR_CHATMACRO8 ),
   chatmacro9( HUSTR_CHATMACRO9 );

        private Settings(int defaultval){
            this.setvalue=defaultval;
            this.defaultvalue=defaultval;
            this.numeric=true;
            this.szdefaultval=Integer.toString(defaultvalue);
        }

private Settings(String defaultval){
    this.defaultvalue=-1;
    this.setszvalue=new String(defaultval);;
    this.szdefaultval=new String(defaultval);
    this.numeric=false;
}

final static char w='w';
final static char a='a';
final static char s='s';
final static char d='d';

/** Each setting "knows" where it is appliable. Hardcoded,
 *  mega-switch statement hack, unless you want to use reflection (ugh...)
 *  
 * @param DS
 */
public void applySetting(DoomStatus DS){
    
    int val=0;
    String Stringval;
    if (this.numeric)
        val=this.setvalue;
    
    if (this.numeric){
   switch(this){   
   case sfx_volume:
       DS.snd_SfxVolume=val;
       break;
   case music_volume:
       DS.snd_MusicVolume=val;
       break;
   case show_messages: 
       DS.M.setShowMessages(C2JUtils.eval(val));
       break;
   case key_right:
       DS.key_right=val;
       break;
   case key_left:
       DS.key_left=val;
       break;
   case key_up:
       DS.key_up=val;
       break;
   case key_down:
       DS.key_down=val;
       break;
   case key_strafeleft:
       DS.key_strafeleft=val;
       break;
   case key_straferight:
       DS.key_straferight=val;
       break;
   case key_fire:
       DS.key_fire=val;
       break;
   case key_use:
       DS.key_use=val;
       break;
   case key_strafe:
       DS.key_strafe=val;
       break;
   case key_speed:
       DS.key_speed=val;
       break;
   case use_mouse:
       DS.use_mouse=C2JUtils.eval(val);
       break;
   case mouseb_fire:
       //DS.mouseb_fire=val;
       break;
   case mouseb_strafe:
       //DS.mouseb_fire=val;
       break;
   case mouseb_forward:
       //DS.mouseb_fire=val;
       break;   
   case use_joystick:
       //DS.mouseb_fire=val;
       break;
   case joyb_fire:
       //DS.mouseb_fire=val;
       break;
   case joyb_strafe:
       //DS.mouseb_fire=val;
       break;   
   case joyb_use:
       //DS.mouseb_fire=val;
       break;
   case joyb_speed:
       //DS.mouseb_fire=val;
       break;
   case screenblocks:
       DS.M.setScreenBlocks(val);
       break;
   case detaillevel:
       //DS.mouseb_fire=val;
       break;   
   case snd_channels:
       // Defined in sound.c, originally.
       DS.numChannels=val;
       break;
   case mb_used:
       // Memory used. Any practical use?
       //TODO DS.mb_used=val;
       break;
   } // end mega-switch
    } // end-numeric
   else {
       switch (this){
       case chatmacro0:
           DS.HU.setChatMacro(0, setszvalue);
           break;
       case chatmacro1:
           DS.HU.setChatMacro(1, setszvalue);
           break;
       case chatmacro2:
           DS.HU.setChatMacro(2, setszvalue);
           break;
       case chatmacro3:
           DS.HU.setChatMacro(3, setszvalue);
           break;
       case chatmacro4:
           DS.HU.setChatMacro(4, setszvalue);
           break;
       case chatmacro5:
           DS.HU.setChatMacro(5, setszvalue);
           break;
       case chatmacro6:
           DS.HU.setChatMacro(6, setszvalue);
           break;
       case chatmacro7:
           DS.HU.setChatMacro(7, setszvalue);
           break;
       case chatmacro8:
           DS.HU.setChatMacro(8, setszvalue);
           break;
       case chatmacro9:
           DS.HU.setChatMacro(9, setszvalue);
           break;
           }
    } // end string
}
   
   


// UNIX hack, to be removed. 
//#ifdef SNDSERV
//    new default_t("sndserver", (int *) &sndserver_filename, (int) "sndserver"),    
//#endif    
//#endif

//#ifdef LINUX
//    new default_t("mousedev", (int*)&mousedev, (int)"/dev/ttyS0"),
//    new default_t("mousetype", (int*)&mousetype, (int)"microsoft"),
//#endif

        public int setvalue;
        public String setszvalue;
        public final int defaultvalue;
        public final String szdefaultval;
        public boolean numeric=false;
    }
