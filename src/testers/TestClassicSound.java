package testers;

import m.DoomRandom;
import m.IRandom;
import s.ClassicDoomSoundDriver;
import doom.DoomStatus;
import w.WadLoader;

public class TestClassicSound {
	public static void main(String[] argv) throws Exception{

	DoomStatus DS=new DoomStatus();
	
	
	WadLoader W=new WadLoader();
	IRandom RND=new DoomRandom();
	
	DS.W=W;
	DS.RND=RND;
	W.InitMultipleFiles(new String[]{"doom1.wad"});
	
	ClassicDoomSoundDriver sound=new ClassicDoomSoundDriver(DS,4);
	
	sound.Init(127, 127);
	
	Thread.sleep(1000);
	sound.StartSoundAtVolume(null, 1, 127);
	
	}
}
