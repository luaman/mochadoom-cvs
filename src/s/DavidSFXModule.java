package s;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import p.mobj_t;
import w.DoomBuffer;

import data.sfxinfo_t;
import data.sounds;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** David Martel's sound driver for Mocha Doom. Excellent work!
 * 
 * Now sound effects are a separate concern from music.
 * 
 * @author David
 *
 */

public class DavidSFXModule implements ISound{

	
	public static final int IDLE_HANDLE = -1;
	HashMap<String,DoomSound> cachedSounds = new HashMap<String,DoomSound>();
	HashMap<Integer,Integer> channelhandles = new HashMap<Integer,Integer>();
	
	public final float[] linear2db;
	
	
	private final SoundWorker[] channels;
	private final Thread[] soundThread;
	private final int numChannels;
	private final DoomStatus DS;
	
	public DavidSFXModule(DoomStatus DS, int numChannels) {
		this.DS=DS;
		this.numChannels=numChannels;
		channels= new SoundWorker[numChannels];
		soundThread= new Thread[numChannels];
		
		linear2db=computeLinear2DB();
		
		}
	
	
    private float[] computeLinear2DB() {
    	
    	// Maximum volume is 0 db, minimum is ... -96 db.
    	// We rig this so that half-scale actually gives half power,
    	// and so is -3 dB.
    	float[] tmp=new float[VOLUME_STEPS];
    	
    	for (int i=0;i<VOLUME_STEPS;i++){
    		float linear=(float)(10*Math.log10((float)i/(float)VOLUME_STEPS));
    		// Hack. The minimum allowed value as of now is -80 db.
    		if (linear<-36.0) linear=-36.0f;
    		tmp[i]= linear;
    		
    	}
    		
    		
    		
		return tmp;
	}


	enum Position { 
        LEFT, RIGHT, NORMAL
    }

	@Override
	public void InitSound() {

		
		//SetSfxVolume(sfxVolume);
		//SetMusicVolume(musicVolume);
		
	}

	@Override
	public void UpdateSound() {
		// In theory, we should update volume + panning for each active channel.
		// Ouch. Ouch Ouch.
		
	}

	@Override
	public void SubmitSound() {
		// Sound should be submitted to the sound threads, which they pretty much
		// do themselves.
		
	}

	@Override
	public void ShutdownSound() {
		 // Wait till all pending sounds are finished.
		  int done = 0;
		  int i;
		  

		  // FIXME (below).
		  //fprintf( stderr, "I_ShutdownSound: NOT finishing pending sounds\n");
		  //fflush( stderr );
		  
		  while ( done==0 )
		  {
		    for( i=0 ; i<numChannels && !(channels[i].isPlaying()) ; i++);
		    // FIXME. No proper channel output.
		    //if (i==8)
		    done=1;
		  }
		  
		  for( i=0 ; i<numChannels; i++){
			channels[i].terminate=true;  
			try {
				this.soundThread[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
		  // Done.
		  return;
		
	}

	@Override
	public void SetChannels() {
		
		// This is actually called from IDoomSound.
		for (int i = 0; i < numChannels; i++) {
			channels[i]=new SoundWorker(i);
			soundThread[i] = new Thread(channels[i]);
			soundThread[i].start();
		}
		
	}

	@Override
	public int GetSfxLumpNum(sfxinfo_t sfxinfo) {
		
		String id = "DS"+sfxinfo.name.toUpperCase();
		if (id.equals("DSNONE"))
			return -1;

		
		int lump;
		try {
		lump = DS.W.GetNumForName(id);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return lump;
	}

	/**
	 *  Starting a sound means adding it
     * to the current list of active sounds
     * in the internal channels.
     *  As the SFX info struct contains
     *  e.g. a pointer to the raw data,
     *  it is ignored.
     *  As our sound handling does not handle
     *  priority, it is ignored.
     *  Pitching (that is, increased speed of playback)
     *   is set, but currently not used by mixing.
     */
	@Override
	public int StartSound(int id, int vol, int sep, int pitch, int priority) {
		
		String sid = "DS"+sounds.S_sfx[id].name.toUpperCase();
		if (sid.equals("DSNONE"))
			return IDLE_HANDLE;
		
		// MAES: apparently, we need not worry about finding available channels here,
		// just assign it to something.

		DoomSound sound = retrieveSoundData(sid);
		if (sound == null)
			return IDLE_HANDLE;
		
		try {
			//sound.ais.reset();
			sound.ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(sound.bytes));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        catch (UnsupportedAudioFileException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        // PROBLEM: at this point we have no idea about the actual
        // status of the hardware channels. The channel manager should,
        // in theory, prevent clashes etc. but how do they two communicate
        // back and forth?

        // Find the first free channel. In theory, this method shouldn't
        // have been called if there were no free channels, riiiiight?
        int c;
        for (c=0;c<numChannels;c++)
        	if (!channels[c].isPlaying()) break;

        // Shouldn't happen, but no _actual_ channels were free. Tough cookie.
        if (c>=numChannels) return IDLE_HANDLE;
        
        System.out.println("Picked "+c);
        // Create a dataline for the "lucky" channel.
        if (channels[c].auline == null) {
        	AudioFormat format = sound.ais.getFormat();
        	DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        	try {
				channels[c].auline = (SourceDataLine) AudioSystem.getLine(info);
				channels[c].auline.open(format);
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        			// Add individual volume control.
        			if (channels[c].auline.isControlSupported(Type.MASTER_GAIN)){
        				channels[c].vc=(FloatControl) channels[c].auline
        				.getControl(Type.MASTER_GAIN);
        			}
        			// else 
        			//	if(channels[i].auline.isControlSupported(FloatControl.Type.MASTER_GAIN)){
            		//		channels[i].vc=(FloatControl) channels[i].auline
            		//		.getControl(FloatControl.Type.MASTER_GAIN);
        			//}
        			
        			// Add individual pan control (TODO: proper positioning).
        			if (channels[c].auline.isControlSupported(Type.BALANCE)){
        				channels[c].pc=(FloatControl) channels[c].auline
        				.getControl(FloatControl.Type.BALANCE);
        			}

        			channels[c].auline.start();
        		}

        // The handle is the current game time.
        int handle=DS.gametic;
		channels[c].setVolume(vol);
		channels[c].setPanning(sep);
		channels[c].addSound(sound, handle);
		this.channelhandles.put(handle,c);
        return handle;
		
	}

	@Override
	public void StopSound(int handle) {
		// Which channel has it?
		Integer hnd=this.channelhandles.get(handle);
		if (hnd!=null) 
			channels[hnd].stopSound();
	}

	@Override
	public boolean SoundIsPlaying(int handle) {
		return this.channelhandles.containsKey(handle);
	}

	
	@Override
	public void UpdateSoundParams(int handle, int vol, int sep, int pitch) {
		
		// This should be called on sounds that are ALREADY playing. We really need
		// to retrieve channels from their handles.
		
		//System.err.printf("Updating sound with handle %d vol %d sep %d pitch %d\n",handle,vol,sep,pitch);
		
		int i=getChannelFromHandle(handle);
		// None has it?
		if (i!=IDLE_HANDLE){
			//System.err.printf("Updating sound with handle %d in channel %d\n",handle,i);
			channels[i].setVolume(vol);
			//channels[i].setPanning(sep);
			channels[i].setPanning(sep);
			}
		
	}
	
	/** Internal use. 
	 * 
	 * @param handle
	 * @return the channel that has the handle, or -1 if none has it.
	 */
	private int getChannelFromHandle(int handle){
		// Which channel has it?
		Integer hnd=this.channelhandles.get(handle);
		if (hnd!=null) 
			return hnd;
		else return IDLE_HANDLE;
	}
	
	/** Get data for a particular lump, if not cached already */
	
	private final DoomSound retrieveSoundData(String id){
		
		DoomSound sound = cachedSounds.get(id);
		
		if (sound==null){
		// OK, so we load the sound...
		// If it's vanilla, we obviously need to handle only its
		// own sample format with DoomToWave.
		int lump=DS.W.CheckNumForName(id);
		byte[] bytes = this.DS.W.CacheLumpNumAsRawBytes(lump, data.Defines.PU_MUSIC);
		
        try {
  
        	// TODO: a cleaner way of handling this?
        	DoomToWave dtw = new DoomToWave();
        	byte[] baos= dtw.DMX2Wave(bytes);
	        sound = new DoomSound(baos);
	        cachedSounds.put(id, sound);

		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	return sound;
		
	}
	
	/** A Thread for playing digital sound effects.
	 * 
	 *  Obviously you need as many as channels?
	 *   
	 *  In order not to end up in a hell of effects,
	 *  certain types of sounds must be limited to 1 per object.
	 *
	 */


	private class SoundWorker implements Runnable {
			private static final int EXTERNAL_BUFFER_SIZE = 256*1024; 
			private static final int CHUNK = 1024; 
			
			byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

			FloatControl vc; // linear volume control
			FloatControl pc; // panning control
			DoomSound currentSoundSync;
			DoomSound currentSound;
			
			public SoundWorker(int id){
				this.id=id;
				this.handle=IDLE_HANDLE;;
			}
			
			int id;
			/** Used to find out whether the same object is continuously making
			 *  sounds. E.g. the player, ceilings etc. In that case, they must be
			 *  interrupted.
			 */
			int handle;
			public boolean terminate;
			SourceDataLine auline;
			
			/** This is how you tell the thread to play a sound,
			 * I suppose.  */
			
			public void addSound(DoomSound ds, int handle) {
				
				System.out.printf("Added handle %d to channel %d\n",handle,id);
				this.handle=handle;
				this.currentSound=ds;
				this.auline.stop();
				this.auline.start();
			}

			/** Accepts volume in "Doom" format (0-127).
			 * 
			 * @param volume
			 */
			public void setVolume(int volume){
				if (vc!=null){
				float vol = linear2db[volume];
				System.out.println("Actual volume" +vol) ;
				vc.setValue(vol);
				}
			}
			
			public void setPanning(int sep){
				// Q: how does Doom's sep map to stereo panning?
				// A: Apparently it's 0-255 L-R.
				if (pc!=null){
				float pan= pc.getMinimum()+(pc.getMaximum()-pc.getMinimum())*(float)sep/256;
				pc.setValue(pan);
				}
			}
			
			public void run() {
				System.err.printf("Sound thread %d started\n",id);
				while (!terminate) {
					currentSoundSync = currentSound;
					if (currentSoundSync != null) {
						int nBytesRead = 0;

						try {

							while (nBytesRead != -1) {
								nBytesRead = currentSoundSync.ais.read(abData, 0, abData.length);
								//System.out.printf("Channel %d Offset: %d Interrupted: %b\n",id,offset,force);
								if (nBytesRead >= 0){ 
									auline.write(abData, 0, nBytesRead);
								}
							}
						} catch (Exception e) { 
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
						// Remove its handle.
						
						//System.out.printf("Channel  %d with handle %d done. Marking as free\n",id,handle);
						channelhandles.remove(handle);
						this.handle=IDLE_HANDLE;
					}

					// If we don't sleep at least a bit here, busy waiting becomes
					// way too taxing. 
					// TODO: in theory the threads should only output "a bit" of each sound each time
					// and wait/sleep until they are called again or something, probably by updatesound,
					// instead of waiting an arbitrary amount of time.

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					} 
				}
			}

			public void stopSound() {
					auline.stop();
					auline.flush();
					//System.out.printf("Channel %d with handle %d interrupted. Marking as free\n",id,handle);
					channelhandles.remove(handle);
					this.handle=IDLE_HANDLE;
					currentSound = null;
					auline.start();
					}

			public boolean isPlaying() {
				//System.out.printf("Channel %d with handle %d queried\n",id,handle);
				return (this.handle!=IDLE_HANDLE||this.currentSound!=null);
			}

		}


}

