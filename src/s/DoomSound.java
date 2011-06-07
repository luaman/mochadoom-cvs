package s;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

/** A class representing a sample in memory */
class DoomSound {
	byte[] bytes;
	AudioInputStream ais = null;

	public DoomSound(byte[] bytes) {
		this.bytes = bytes;

		try { 
			ais = AudioSystem.getAudioInputStream(new ByteInputStream(bytes, bytes.length));
			//audioInputStream.reset();
		} catch (UnsupportedAudioFileException e1) { 
			e1.printStackTrace();
			return;
		} catch (IOException e1) { 
			e1.printStackTrace();
			return;
		} 
	}
}
