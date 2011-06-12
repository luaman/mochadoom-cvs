package s;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/** A class representing a sample in memory */
class DoomSound {
	byte[] bytes;
	AudioInputStream ais = null;

	public DoomSound(byte[] bytes) {
		this.bytes = bytes;

		try { 
			ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
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
