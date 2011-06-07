package s;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import p.mobj_t;


/** A Thread for playing digital sound effects. Obviously you need as many as channels?
 * 
 *
 */

public class SoundWorker extends channel_t implements Runnable {
	
	private final int EXTERNAL_BUFFER_SIZE = 128*1024; // 128Kb 
	
	byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
	FloatControl vc; // volume control 
	FloatControl pc; // pan control
	
	public boolean terminate=false;

	/** This is how you tell the thread to play a sound,
	 * I suppose.  */
	
	public void addSound(DoomSound ds) {
		this.currentSound = ds;
		this.auline.start();
	}
	
	public void addSound(DoomSound ds, mobj_t origin) {
		this.currentSound = ds;
		this.origin=origin;
		this.auline.start();
	}
	
	public void run() {
		DoomSound currentSoundSync;

		while (!terminate) {
			currentSoundSync = currentSound;
			if (currentSoundSync != null) {
		        int nBytesRead = 0;
		        
		        if (vc!=null){
		        float vol = vc.getMinimum()+(vc.getMaximum()-vc.getMinimum())*(float)sfxVolume/(float)s.AbstractDoomAudio.MAX_VOLUME;
		        vc.setValue(vol);
		        }
		        
		        try { 
		            while (nBytesRead != -1) { 
		                nBytesRead = currentSoundSync.ais.read(abData, 0, abData.length);
		                if (nBytesRead >= 0) 
		                    auline.write(abData, 0, nBytesRead);
		            } 
		        } catch (Exception e) { 
		            e.printStackTrace();
		            return;
		        } finally { 
		            auline.drain();
		        	} 
		        currentSound = null;
			}
			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void stopSound() {
		auline.stop();
		auline.flush();
		currentSound = null;
		auline.start();
		}


public boolean isPlaying() {
	return (this.currentSound!=null);
}
	
}
