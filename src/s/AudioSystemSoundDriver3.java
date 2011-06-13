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
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.FloatControl.Type;

import p.mobj_t;
import w.DoomBuffer;

import data.sfxinfo_t;
import data.sounds;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** A variation of David Martel's sound driver for Mocha Doom, based
 *  on audio clips. In theory, sound threads are not required. Some 
 *  form of channel management must still be implemented, though.
 * 
 * @author David
 *
 */

public class AudioSystemSoundDriver3 implements ISound{

	DoomStatus DS;
	
	class soundClipChannel {
		Clip clip;
		sfxinfo_t sfx;
		int handle;
		
		/** Accepts volume in "Doom" format (0-127).
		 * 
		 * @param volume
		 */
		public void setVolume(int volume){
			if (clip!=null){
				if (clip.isControlSupported(Type.VOLUME)){
					FloatControl vc=(FloatControl) clip.getControl(Type.VOLUME);
					float vol = vc.getMinimum()+(vc.getMaximum()-vc.getMinimum())*(float)volume/127f;
					System.out.println("Actual volume" +vol) ;
					vc.setValue(vol);
				}
	
			}
		}
		
		public void setPanning(int sep){
			// Q: how does Doom's sep map to stereo panning?
			// A: Apparently it's 0-255 L-R.
			if (clip!=null){
				if (clip.isControlSupported(Type.PAN)){
					FloatControl vc=(FloatControl) clip.getControl(Type.PAN);
					float vol = vc.getMinimum()+(vc.getMaximum()-vc.getMinimum())*(float)sep/256;
					System.out.println("Actual volume" +vol) ;
					vc.setValue(vol);
				}
	
			}
		}
		
		public void stop(){
			clip.stop();
		}

		
		public boolean isPlaying(){
			if (clip==null) return false;
			return (clip.isRunning());
		}
		
	}
	
	Sequencer sequencer;
	
	HashMap<String,DoomSound> cachedSounds = new HashMap<String,DoomSound>();
	HashMap<Integer,Integer> channelhandles = new HashMap<Integer,Integer>();
	
	private final soundClipChannel[] channels;
	private final int numChannels;

	public AudioSystemSoundDriver3(DoomStatus DS, int numChannels) {
		this.DS=DS;
		this.numChannels=numChannels;
		channels= new soundClipChannel[numChannels];
	}

	
	public static Port getSoundPort() {
		Mixer.Info[] infos = AudioSystem.getMixerInfo();   
	    for (Mixer.Info info: infos)   
	    {   
	        Mixer mixer = AudioSystem.getMixer(info);   
	        if (mixer.isLineSupported(Port.Info.SPEAKER))   
	        {   
				try {
					Port port = (Port)mixer.getLine(Port.Info.SPEAKER);
		            port.open();   
		            if (port.isControlSupported(FloatControl.Type.VOLUME))   
		            {   
		                FloatControl volume = (FloatControl)port.getControl(FloatControl.Type.VOLUME);   
		                System.out.println(info);   
		                System.out.println("- " + Port.Info.SPEAKER);   
		                System.out.println("  - " + volume);   
		                
		                return port;
		            }   
		            port.close();   
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}   
	        }   
	    }   
	    
	    return null;
	}
	
	/*
	public void SetSfxVolume(int volume) {
		// TODO Auto-generated method stub
		this.sfxVolume = volume;
		
		Port port = getSoundPort();
		if (port != null) {
            FloatControl vol = (FloatControl)port. getControl(FloatControl.Type.VOLUME);   
            vol.setValue((float)volume/16f);
		}
			
	} */

    /** Locates a sound in the WAD manager, loads it, converts it (?)
     *  and caches it in the "sound driver" itself.
     *  
     *  TODO: it's pointless to cache it in two places, so this is a
     *  prime candidate for the "fake zone" system.
     * 
     * @param id
     * @return
     */
    
	public DoomSound findSound(String id) {
		DoomSound sound = cachedSounds.get(id);
		if (sound == null) {
			int lump;
			try {
			lump = DS.W.GetNumForName(id);
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			// OK, so we load the sound...
			// If it's vanilla, we obviously need to handle only its
			// own sample format with DoomToWave.
			byte[] bytes = ((DoomBuffer)DS.W.CacheLumpNum( lump, 0/*PU_MUSIC*/, DoomBuffer.class )).getBuffer().array();
			// Free it.
			DS.Z.Free(DS.W.CacheLumpNum( lump, 0/*PU_MUSIC*/, DoomBuffer.class ));
			
	        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
	        
	        try {
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        	
	        	// TODO: a cleaner way of handling this?
		        DoomToWave dtw = new DoomToWave();
		        dtw.SNDsaveSound(bis, baos);
		        sound = new DoomSound(baos.toByteArray());
		        cachedSounds.put(id, sound);
		        //bis.close();
		        //bis = new ByteArrayInputStream(baos.toByteArray(), baos.size());
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
		
        // Find the first free channel. In theory, this method shouldn't
        // have been called if there were no free channels, riiiiight?
        int c;
        for (c=0;c<numChannels;c++)
        	if (!channels[c].isPlaying()) break;

        // Shouldn't happen, but no _actual_ channels were free. Tough cookie.
        if (c>=numChannels) return IDLE_HANDLE;
        
        System.out.println("Picked "+c);
        // Create a dataline for the "lucky" channel.
		
		try {
			//sound.ais.reset();
			sound.ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(sound.bytes));
			Clip clip = AudioSystem.getClip();
			clip.open(/*audioInputStream*/sound.ais);
			FloatControl gainControl = 
			    (FloatControl) clip.getControl(Type.VOLUME.MASTER_GAIN);
			System.out.println(gainControl);
			gainControl.setValue(-10.0f); // Reduce volume by 10 decibels.
			clip.start();
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        catch (UnsupportedAudioFileException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        } catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        
        try {
   //     	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	
        	
        	// TODO: a cleaner way of handling this?
        	DoomToWave dtw = new DoomToWave();
        	byte[] baos= dtw.DMX2Wave(bytes);
	        sound = new DoomSound(baos);
	        cachedSounds.put(id, sound);
        	
        	// TODO: a cleaner way of handling this?
        	
        	//DoomToWave dtw = new DoomToWave();
	        //dtw.SNDsaveSound(bis, baos);
	        //sound = new DoomSound(baos.toByteArray());
	        //cachedSounds.put(id, sound);
	        //bis.close();
	        //bis = new ByteInputStream(baos.toByteArray(), baos.size());
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
	
	@Override
	public void ShutdownSound()
	{    

	  // Wait till all pending sounds are finished.
	  int done = 0;
	  int i;
	  

	  // FIXME (below).
	  //fprintf( stderr, "I_ShutdownSound: NOT finishing pending sounds\n");
	  //fflush( stderr );
	  
	  while ( done==0 )
	  {
	  //  for( i=0 ; i<numChannels && !(channels[i].isPlaying()) ; i++);
	    // FIXME. No proper channel output.
	    //if (i==8)
	    done=1;
	  }

	  // Done.
	  return;
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
}

