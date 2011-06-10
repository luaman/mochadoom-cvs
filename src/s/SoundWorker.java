package s;

import javax.sound.sampled.FloatControl;
import data.sfxinfo_t;
import p.mobj_t;

/** A Thread for playing digital sound effects.
 * 
 *  Obviously you need as many as channels?
 *   
 *  In order not to end up in a hell of effects,
 *  certain types of sounds must be limited to 1 per object.
 *
 */


public class SoundWorker extends channel_t implements Runnable {
	// 	Play at least this before even forced interruptions.
		protected static final int MAX_PLAYBACK_CHUNK = 1024;
		protected static final int EXTERNAL_BUFFER_SIZE = 256*1024; 

		byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

		FloatControl vc;
		FloatControl pc;
		DoomSound currentSoundSync;
		
		int id;
		/** Used to find out whether the same object is continuously making
		 *  sounds. E.g. the player, ceilings etc. In that case, they must be
		 *  interrupted.
		 */
		mobj_t origin;
		public boolean terminate;

		public SoundWorker(int id){
			this.sfxinfo=new sfxinfo_t();
			this.id=id;
		}

		/** This is how you tell the thread to play a sound,
		 * I suppose.  */

		public void addSound(DoomSound ds, mobj_t origin) {
			this.currentSound = ds;
			this.origin=origin;
			this.auline.start();
		}

		public void forceStop(DoomSound ds) {
			//System.out.println("Forced signaled");
			if (auline.isActive()) auline.stop();
			this.auline.flush();
			this.currentSound = ds;
		}

		public void run() {

			while (!terminate) {
				currentSoundSync = currentSound;
				if (currentSoundSync != null) {
					int nBytesRead = 0;
					// Assuming 15 values at most?
					float vol = vc.getMinimum()+(vc.getMaximum()-vc.getMinimum())*(float)sfxVolume/127f;
					vc.setValue(vol);			        

					try {

						while (nBytesRead != -1) {
							nBytesRead = currentSoundSync.ais.read(abData, 0, abData.length);
							//System.out.printf("Channel %d Offset: %d Interrupted: %b\n",id,offset,force);
							if (nBytesRead >= 0){ 
								auline.write(abData, 0, nBytesRead);
							}
						}
					} catch (Exception e) { 
						System.out.println(sfxVolume);
						e.printStackTrace();
						return;
					} finally {
						// The previous steps are actually VERY fast.
						// However this one waits until the data has been
						// consumed, Interruptions/signals won't reach  here,
						// so it's pointless trying to interrupt the actual filling.
						//long a=System.nanoTime();
						auline.drain();
						//long b=System.nanoTime();
						//System.out.printf("Channel %d completed in %f.\n",id,(float)(b-a)/1000000000f);
						}
					// Report that this channel is free.
					currentSound = null;

				}

				// If we don't sleep at least a bit here, busy waiting become
				// way too taxing.

				try {
					Thread.sleep(10);
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

		public void addSound(DoomSound ds) {
			// TODO Auto-generated method stub
			
		}

		public boolean isPlaying() {
			return (this.currentSound!=null);
		}
	}
