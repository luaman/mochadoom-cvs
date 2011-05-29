package g;

public class Keys {
	public static final int KEY_NULL        = 0;       // null key, triggers nothing

	public static final int KEY_ESCAPE      = 27;
	public static final int KEY_SPACE       = 32;

	public static final int KEY_NUMLOCK     = (0x80+69);
	public static final int KEY_SCROLLLOCK  = (0x80+70);

	public static final int KEY_MINUS       = 45;
	public static final int KEY_EQUALS      = 61;
	public static final int KEY_BACKSPACE   = 8;
	public static final int KEY_TAB         = 9;
	public static final int KEY_ENTER       = 13;

	//
	//  scancodes 71-83 (non-extended)
	//
	public static final int KEY_KEYPAD7     = (0x80+71);
	public static final int KEY_KEYPAD8     = (0x80+72);
	public static final int KEY_KEYPAD9     = (0x80+73);
	public static final int KEY_MINUSPAD    = (0x80+74);
	public static final int KEY_KEYPAD4     = (0x80+75);
	public static final int KEY_KEYPAD5     = (0x80+76);
	public static final int KEY_KEYPAD6     = (0x80+77);
	public static final int KEY_PLUSPAD     = (0x80+78);
	public static final int KEY_KEYPAD1     = (0x80+79);
	public static final int KEY_KEYPAD2     = (0x80+80);
	public static final int KEY_KEYPAD3     = (0x80+81);
	public static final int KEY_KEYPAD0     = (0x80+82);
	public static final int KEY_KPADDEL     = (0x80+83);

	//  windows95 keys...

	public static final int KEY_LEFTWIN     = (0x80+91);
	public static final int KEY_RIGHTWIN    = (0x80+92);
	public static final int KEY_MENU        = (0x80+93);

	//
	//  scancodes 71-83 EXTENDED are remapped
	//  to these by the keyboard handler (just add 30)
	//
	public static final int KEY_KPADSLASH   = (0x80+100);      //extended scancode 53 '/' remapped

	public static final int KEY_HOME        = (0x80+101);
	public static final int KEY_UPARROW     = (0x80+102);
	public static final int KEY_PGUP        = (0x80+103);
	public static final int KEY_LEFTARROW   = (0x80+105);
	public static final int KEY_RIGHTARROW  = (0x80+107);
	public static final int KEY_END         = (0x80+109);
	public static final int KEY_DOWNARROW   = (0x80+110);
	public static final int KEY_PGDN        = (0x80+111);
	public static final int KEY_INS         = (0x80+112);
	public static final int KEY_DEL         = (0x80+113);


	public static final int KEY_F1          = (0x80+0x3b);
	public static final int KEY_F2          = (0x80+0x3c);
	public static final int KEY_F3          = (0x80+0x3d);
	public static final int KEY_F4          = (0x80+0x3e);
	public static final int KEY_F5          = (0x80+0x3f);
	public static final int KEY_F6          = (0x80+0x40);
	public static final int KEY_F7          = (0x80+0x41);
	public static final int KEY_F8          = (0x80+0x42);
	public static final int KEY_F9          = (0x80+0x43);
	public static final int KEY_F10         = (0x80+0x44);
	public static final int KEY_F11         = (0x80+0x57);
	public static final int KEY_F12         = (0x80+0x58);

	public static final int KEY_PAUSE       = 255;

	// these ones must be non-extended scancodes (rctrl,rshift,lalt)
	public static final int KEY_SHIFT       = (0x80+54);
	public static final int KEY_CTRL        = (0x80+29);
	public static final int KEY_ALT         = (0x80+56);

	public static final int KEY_CAPSLOCK    = (0x80+58);
	public static final int KEY_CONSOLE     = (int)'`';

//	public static final int KEY_OPENBRACKETS
//	public static final int KEY_CLOSEBRACKETS

}