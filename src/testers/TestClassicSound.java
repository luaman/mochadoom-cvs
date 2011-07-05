package testers;

import m.DoomRandom;
import m.IRandom;
import s.ClassicDoomSoundDriver;
import data.sounds.sfxenum_t;
import doom.DoomStatus;
import w.WadLoader;

public class TestClassicSound {
	public static void main(String[] argv) throws Exception{

	DoomStatus DS=new DoomStatus();
	
	
	WadLoader W=new WadLoader();
	IRandom RND=new DoomRandom();
	
	DS.W=W;
	DS.RND=RND;
	W.InitMultipleFiles(new String[]{"c:\\iwads\\doom1.wad"});
	
	ClassicDoomSoundDriver sound=new ClassicDoomSoundDriver(DS,3);
	
	sound.InitSound();
	sound.SetChannels(1);
	
	Thread.sleep(1000);
	//sound.StartSound(1, 127, 127, 127, 0);
	for (int i=0;i<350;i++){
	    
	    Thread.sleep(1000/35);

	    if (i%35==0) sound.StartSound(sfxenum_t.sfx_plpain.ordinal(), 127, 127, 127-6*i/35, 0);
	    if (DS.gametic%5!=2){
	    sound.UpdateSound();
	    }
	    sound.SubmitSound();
	    
	       DS.gametic++;
	}
	sound.ShutdownSound();
	}
	
}
