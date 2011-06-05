package s;

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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import p.mobj_t;
import s.AudioSystemSoundDriver2.DoomSound;
import w.DoomBuffer;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import data.sounds;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import doom.DoomMain;
import doom.DoomStatus;

/** David Martel's sound driver for Mocha Doom. Excellent work!
 * 
 * @author David
 *
 */

public class AudioSystemSoundDriver extends AbstractDoomAudio implements IDoomSound{

	Sequencer sequencer;
	
	HashMap<String,DoomSound> cachedSounds = new HashMap<String,DoomSound>();
	
	private final SoundWorker[] channels;
	private final Thread[] soundThread;
	
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
	
	/** FIXME Hmm... probably needs work?
	 *  
	 * @param value
	 */
	
	public void setVolume(int value) {  
		/*MidiChannel[] channels = synthesizer.getChannels();   
	     for (int i = 0; i < channels.length; i++)   {
	          channels[i].controlChange(7, 0);
	          channels[i].controlChange(39, 0);
	     }*/
	     
/*		try {  

			ShortMessage volumeMessage = new ShortMessage();  

			for (int i = 0; i < 16; i++) {  

				volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 7, value);  
				receiver.send(volumeMessage, -1);  

			}  

		} catch (Exception e) {  

			e.printStackTrace();  

		}  */

	}  

	Receiver receiver;
	Synthesizer synthesizer;

	public AudioSystemSoundDriver(DoomStatus DS, int numChannels, boolean nomusic, boolean nosound) {
		super(DS,numChannels);
		
		channels= new SoundWorker[numChannels];
		soundThread= new Thread[numChannels];
		super.channels=this.channels;
		
		if (!nomusic){
		try {
			MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();   
		     for (int i = 0; i < info.length; i++)   
		          System.out.println(info[i].getName()+"\t\t\t"/*+ mdev.isOpen()*/); 
			
			sequencer = MidiSystem.getSequencer(true);
		    sequencer.open();

		    /*synthesizer = MidiSystem.getSynthesizer();  
		    synthesizer.open();  */

		    /*receiver = synthesizer.getReceiver();
		    Transmitter transmitter = sequencer.getTransmitter();
		    transmitter.setReceiver(receiver); */
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
		}
	}
	
	@Override
	public void ChangeMusic(int music_id, boolean looping) {
		try {
			String id = "D_"+sounds.S_music[music_id].name.toUpperCase();
			int lump = DS.W.GetNumForName(id);

			byte[] bytes = ((DoomBuffer)DS.W.CacheLumpNum( lump, 0/*PU_MUSIC*/, DoomBuffer.class )).getBuffer().array();
			
	        ByteInputStream bis = new ByteInputStream(bytes, bytes.length);
	        
	        QMusToMid q = new QMusToMid();
	        //OutputStream fos = new FileOutputStream("C:\\Users\\David\\Documents\\test.mid");

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        
	        q.convert(bis, baos, true, 0, 0, true, new QMusToMid.Ptr<Integer>(0));
	        	  
	        bis.close();
	        bis = new ByteInputStream(baos.toByteArray(), baos.size());
	        
	        Sequence sequence = MidiSystem.getSequence(bis);
	        
	        sequencer.stop(); // stops current music if any
	        sequencer.setSequence(sequence); // Create a sequencer for the sequence
	        if (looping)
	        	sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
	        else
	        	sequencer.setLoopCount(0);
	        sequencer.start(); // Start playing
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } 
	}

	
	
	public void addSound(DoomSound ds, mobj_t origin) {
		boolean one=true;
		
		for (int i = 0; i < channels.length; i++) {
			// Force using the same origin, if available.
			// This effectively limits sounds to 2 for player,
			// menu, crushers etc.
			if (channels[i].currentSound != null && channels[i].origin==origin){
				one=!one; // First time, we self-deny.
				if (one){
				System.out.printf("Overriding channel %d for mobj %s\n",i,origin);
				channels[i].stopSound();
				channels[i].addSound(ds);
				return;
				}
				}
		}
		
		// Otherwise, fetch a fresh channel.
		
		for (int i = 0; i < channels.length; i++) {			
			
			if (channels[i].currentSound == null) {
				channels[i].addSound(ds,origin);
				System.out.printf("Sound added to channel %d for mobj %s\n",i,origin);
				//			forced=0;
				return;
			}
		}

		// Forcibly add to the oldest channel, if nothing else was available.
		{
			System.out.println("FORCING channel "+forced);

			channels[forced].stopSound();
			channels[forced].addSound(ds);

			forced++;
			forced%=channels.length;
		}
	}
	
	int forced=0;
	
	int sfxVolume;
	int musicVolume;
	
	@Override
	public void Init(int sfxVolume, int musicVolume) {
		for (int i = 0; i < soundThread.length; i++) {
			channels[i]=new SoundWorker();
			soundThread[i] = new Thread(channels[i]);
			soundThread[i].start();
		}
		
		SetSfxVolume(sfxVolume);
		SetMusicVolume(musicVolume);
	}

	@Override
	public void PauseSound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ResumeSound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void SetMusicVolume(int volume) {
		// TODO Auto-generated method stub
		this.musicVolume = volume;

		int[] extremes = new int[] {0, 15};
		int val = volume*100/15;
		setVolume(val);
	}

	@Override
	public void SetSfxVolume(int volume) {
		// TODO Auto-generated method stub
		this.sfxVolume = volume;
		
	}

	@Override
	public void Start() {
		int cnum;
		  int mnum;

		  // kill all playing sounds at start of level
		  //  (trust me - a good idea)
		  /*for (cnum=0 ; cnum<numChannels ; cnum++)
		    if (channels[cnum].sfxinfo)
		      S_StopChannel(cnum);
		  
		  // start new music for the level
		  mus_paused = 0;*/
		  
		  /*if (gamemode == commercial)
		    mnum = mus_runnin + gamemap - 1;
		  else
		  {*/
		  musicenum_t[] spmus =
		    {
		      // Song - Who? - Where?
		      
				  musicenum_t.mus_e3m4,	// American	e4m1
				  musicenum_t.mus_e3m2,	// Romero	e4m2
				  musicenum_t.mus_e3m3,	// Shawn	e4m3
				  musicenum_t.mus_e1m5,	// American	e4m4
				  musicenum_t.mus_e2m7,	// Tim 	e4m5
				  musicenum_t.mus_e2m4,	// Romero	e4m6
				  musicenum_t.mus_e2m6,	// J.Anderson	e4m7 CHIRON.WAD
				  musicenum_t.mus_e2m5,	// Shawn	e4m8
				  musicenum_t.mus_e1m9	// Tim		e4m9
		    };
		    
		    if (DS.gameepisode < 4)
		      mnum = musicenum_t.mus_e1m1.ordinal() + (DS.gameepisode-1)*9 + DS.gamemap-1;
		    else
		      mnum = spmus[DS.gamemap-1].ordinal();
		   // }	
		    
		    if (DS.isCommercial())
		    	mnum = 32+DS.gamemap; // for DOOM II
		    
		  // HACK FOR COMMERCIAL
		  //  if (commercial && mnum > mus_e3m9)	
		  //      mnum -= mus_e3m9;
		  
		  ChangeMusic(mnum, true);
		  
		  //nextcleanup = 15;

	}

	@Override
	public void StartMusic(int music_id) {
		ChangeMusic(music_id, false);
	}

	@Override
	public void StartSound(mobj_t origin, int sound_id) {
		StartSoundAtVolume(origin, sound_id, 1);
	}

	@Override
	public void StartSound(mobj_t origin, sfxenum_t sound_id) {
		StartSound(origin, sound_id.ordinal());
	}

	private Position curPosition;
	 
  
 
    enum Position { 
        LEFT, RIGHT, NORMAL
    };

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
			
	        ByteInputStream bis = new ByteInputStream(bytes, bytes.length);
	        
	        try {
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        	
	        	// TODO: a cleaner way of handling this?
		        DoomToWave dtw = new DoomToWave();
		        dtw.SNDsaveSound(bis, baos);
		        sound = new DoomSound(baos.toByteArray());
		        cachedSounds.put(id, sound);
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
	public void StartSoundAtVolume(mobj_t origin, int sound_id, int volume) {
		String id = "DS"+sounds.S_sfx[sound_id].name.toUpperCase();
		if (id.equals("DSNONE"))
			return;

		DoomSound sound = findSound(id);
		if (sound == null)
			return;
		
		try {
			//sound.ais.reset();
			sound.ais = AudioSystem.getAudioInputStream(new ByteInputStream(sound.bytes, sound.bytes.length));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        catch (UnsupportedAudioFileException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        if (channels[0].auline == null) {
        	AudioFormat format = sound.ais.getFormat();
        	DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        	try { 
        		for (int i = 0; i < soundThread.length; i++) {
        			channels[i].auline = (SourceDataLine) AudioSystem.getLine(info);
        			channels[i].auline.open(format);

        			
        			// Add individual volume control.
        			if (channels[i].auline.isControlSupported(FloatControl.Type.VOLUME)){
        				channels[i].vc=(FloatControl) channels[i].auline
        				.getControl(FloatControl.Type.VOLUME);
        			}
        			
        			// Add individual pan control (TODO: proper positioning).
        			if (channels[i].auline.isControlSupported(FloatControl.Type.PAN)){
        				channels[i].pc=(FloatControl) channels[i].auline
        				.getControl(FloatControl.Type.PAN);
        			}
        			channels[i].sfxVolume=this.sfxVolume;
        			channels[i].format=format;
        			channels[i].auline.start();
        		}
        	} catch (LineUnavailableException e) { 
        		e.printStackTrace();
        		return;
        	} catch (Exception e) { 
        		e.printStackTrace();
        		return;
        	} 
        }

        addSound(sound,origin);

	}

	@Override
	public void StopMusic() {
		sequencer.stop();
	}

	@Override
	public void StopSound(mobj_t origin) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void UpdateSounds(mobj_t listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void StartMusic(musicenum_t musicid) {
		StartMusic(musicid.ordinal());
	}

    @Override
    public void ChangeMusic(musicenum_t musicid, boolean looping) {
        ChangeMusic(musicid.ordinal(), looping);
    }

	@Override
	public void ShutdownMusic() {
		sequencer.stop();
		sequencer.close();		
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

}

