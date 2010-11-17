package m;

import static data.Defines.*;
import static doom.englsh.*;
import doom.DoomContext;
import doom.DoomStatus;


/** default settings enum */

public enum Settings {

       
    mouse_sensitivity(5),
    sfx_volume(8),
   music_volume(8),
   show_messages(1),
   key_right(KEY_RIGHTARROW),
key_left(KEY_LEFTARROW),
key_up(KEY_UPARROW),
key_down(KEY_DOWNARROW),
key_strafeleft(','),
key_straferight('.'),

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

   screenblocks( 9),
   detaillevel( 0),

   snd_channels( 3),



   usegamma(0),

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
            this.defaultval=defaultval;
            this.sZdefaultval="";
            this.numeric=true;
        }

private Settings(String defaultval){
    this.defaultval=0;
    this.sZdefaultval=defaultval;
    this.numeric=false;
}

static public void applySetting(DoomContext DC, Settings s,String value){
    
    int val=0;
    if (s.numeric){
        try{
        val=Integer.parseInt(value);
    } catch (NumberFormatException e){
        // Imparseable or non-numeric
        val=s.defaultval;
    }
    }
    
    if (s.numeric){        
    
   switch(s){
   
   case sfx_volume:
       DC.DS.snd_SfxVolume=val;
       break;
   case music_volume:
       DC.DS.snd_MusicVolume=val;
   case show_messages: 
       DC.M.setShowMessages(val);
   }
    }

new default_t("key_right",&key_right, KEY_RIGHTARROW),
new default_t("key_left",&key_left, KEY_LEFTARROW),
new default_t("key_up",&key_up, KEY_UPARROW),
new default_t("key_down",&key_down, KEY_DOWNARROW),
new default_t("key_strafeleft",&key_strafeleft, ','),
new default_t("key_straferight",&key_straferight, '.'),

    new default_t("key_fire",&key_fire, KEY_RCTRL),
    new default_t("key_use",&key_use, ' '),
    new default_t("key_strafe",&key_strafe, KEY_RALT),
    new default_t("key_speed",&key_speed, KEY_RSHIFT),

// UNIX hack, to be removed. 
//#ifdef SNDSERV
    new default_t("sndserver", (int *) &sndserver_filename, (int) "sndserver"),
    new default_t("mb_used", &mb_used, 2),
//#endif
    
//#endif

//#ifdef LINUX
    new default_t("mousedev", (int*)&mousedev, (int)"/dev/ttyS0"),
    new default_t("mousetype", (int*)&mousetype, (int)"microsoft"),
//#endif

    new default_t("use_mouse",&usemouse, 1),
    new default_t("mouseb_fire",&mousebfire,0),
    new default_t("mouseb_strafe",&mousebstrafe,1),
    new default_t("mouseb_forward",&mousebforward,2),

    new default_t("use_joystick",&usejoystick, 0),
    new default_t("joyb_fire",&joybfire,0),
    new default_t("joyb_strafe",&joybstrafe,1),
    new default_t("joyb_use",&joybuse,3),
    new default_t("joyb_speed",&joybspeed,2),

    new default_t("screenblocks",&screenblocks, 9),
    new default_t("detaillevel",&detailLevel, 0),

    new default_t("snd_channels",&numChannels, 3),



    new default_t("usegamma",&usegamma, 0),

    new default_t("chatmacro0", (int *) &chat_macros[0], (int) HUSTR_CHATMACRO0 ),
    new default_t("chatmacro1", (int *) &chat_macros[1], (int) HUSTR_CHATMACRO1 ),
    new default_t("chatmacro2", (int *) &chat_macros[2], (int) HUSTR_CHATMACRO2 ),
    new default_t("chatmacro3", (int *) &chat_macros[3], (int) HUSTR_CHATMACRO3 ),
    new default_t("chatmacro4", (int *) &chat_macros[4], (int) HUSTR_CHATMACRO4 ),
    new default_t("chatmacro5", (int *) &chat_macros[5], (int) HUSTR_CHATMACRO5 ),
    new default_t("chatmacro6", (int *) &chat_macros[6], (int) HUSTR_CHATMACRO6 ),
    new default_t("chatmacro7", (int *) &chat_macros[7], (int) HUSTR_CHATMACRO7 ),
    new default_t("chatmacro8", (int *) &chat_macros[8], (int) HUSTR_CHATMACRO8 ),
    new default_t("chatmacro9", (int *) &chat_macros[9], (int) HUSTR_CHATMACRO9 )

};
    */

}
        public int defaultval;
        public String sZdefaultval;
        public boolean numeric=false;
        
        
    }
