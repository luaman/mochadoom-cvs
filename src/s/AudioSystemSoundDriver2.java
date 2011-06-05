package s;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import p.mobj_t;
import w.DoomBuffer;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import data.musicinfo_t;
import data.sfxinfo_t;
import data.sounds;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import doom.DoomMain;
import doom.DoomStatus;

/** David Martel's sound driver for Mocha Doom. Excellent work!
 * 
 *  TODO: sound positioning.
 *        exclusive player channel to avoid eating all channels up with CG
 *        or chainsaw.
 *        interruptible sound channel for the above
 *        master/separate volume.
 * 
 * @author David
 *
 */

public class AudioSystemSoundDriver2 extends AbstractDoomAudio implements IDoomSound, LineListener{

	Sequencer sequencer;

	HashMap<String,DoomSound> cachedSounds = new HashMap<String,DoomSound>();
	
	protected final SoundThread[] channels;
	protected final Thread[] sthreads;

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

		MidiChannel[] channels;

		if (synthesizer!=null) {
			channels = synthesizer.getChannels();   
			for (int i = 0; i < channels.length; i++)   {
				channels[i].controlChange(7, 0);
				channels[i].controlChange(39, 0);
			}

			try {  

				ShortMessage volumeMessage = new ShortMessage();  

				for (int i = 0; i < 16; i++) {  

					volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 7, value);  
					receiver.send(volumeMessage, -1);  

				}  

			} catch (Exception e) {  

				e.printStackTrace();  

			}
		}
	}  

	Receiver receiver;
	Synthesizer synthesizer;

	public AudioSystemSoundDriver2(DoomStatus DS, int numChannels, boolean nomusic, boolean nosound) {
		super(DS,numChannels);
		this.channels= new SoundThread[numChannels];
		super.channels=this.channels;
		this.sthreads= new Thread[numChannels];

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

	/** A Thread for playing digital sound effects.
	 * 
	 *  Obviously you need as many as channels?
	 *   
	 *  In order not to end up in a hell of effects,
	 *  certain types of sounds must be limited to 1 per object.
	 *
	 */

	public class SoundThread extends channel_t implements ISoundThread,Runnable {
		// Play at least this before even forced interruptions.
		protected static final int MAX_PLAYBACK_CHUNK = 1024;

		byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

		FloatControl fc;
		FloatControl pc;
		DoomSound currentSoundSync;
		
		int id;
		/** Used to find out whether the same object is continuously making
		 *  sounds. E.g. the player, ceilings etc. In that case, they must be
		 *  interrupted.
		 */
		mobj_t origin;
		public boolean terminate;

		public SoundThread(int id){
			this.sfxinfo=new sfxinfo_t();
			this.id=id;
		}

		/** This is how you tell the thread to play a sound,
		 * I suppose.  */

		@Override
		public void addSound(DoomSound ds, mobj_t origin) {
			this.currentSound = ds;
			this.origin=origin;
			this.auline.start();
		}

		@Override
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
					float vol = fc.getMinimum()+(fc.getMaximum()-fc.getMinimum())*(float)sfxVolume/15f;
					fc.setValue(vol);			        

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

		@Override
		public void addSound(DoomSound ds) {
			// TODO Auto-generated method stub
			
		}

		public boolean isPlaying() {
			return (this.currentSound!=null);
		}
	}

	public void addPlayerSound(DoomSound ds){
		// TODO: reserve one channel for player?
		//this.playerSounds.forceStop(ds);
		//this.playerSounds.addSound(ds);
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

		// Forcibly add to forced channel, if nothing else was available.
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
		SetChannels();
		
		for (int i = 0; i < channels.length; i++) {
			channels[i] = new SoundThread(i);
			this.sthreads[i]=new Thread(channels[i]);
			sthreads[i].start();
		}

		SetSfxVolume(sfxVolume);
		SetMusicVolume(musicVolume);
	}

	long musicposition;
	
	public void PauseSong(int handle){
		musicposition=sequencer.getTickPosition();
		this.sequencer.stop();
	}
	
	@Override
	public void PauseSound() {
	    if ((mus_playing!=null) && !mus_paused)
	    {
		PauseSong(mus_playing.handle);
		mus_paused = true;
	    }

	}

	@Override
	public void ResumeSound() {
	    if (mus_playing!=null && mus_paused)
	    {
		ResumeSong(mus_playing.handle);
		mus_paused = false;
	    }

	}

	public void ResumeSong(int handle) {
		sequencer.setTickPosition(this.musicposition);
		sequencer.start();
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
		for (cnum=0 ; cnum< numChannels ; cnum++)
		    if (channels[cnum].isPlaying())
		      StopChannel(cnum);

		  // start new music for the level
		  mus_paused = false;

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

	private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb 

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

			// Force the wad manager to "forget" it, since it's also cached here.
			DS.Z.Free(DS.W.CacheLumpNum( lump, 0/*PU_MUSIC*/, DoomBuffer.class));

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

	/** A sound with a specified volume, id and origin is played back.
	 *  In this implementation, SoundThread aulines are created 
	 *  when needed (and persist?).
	 *  
	 *  TODO positioning?
	 *  TODO channel management?
	 * 
	 * (non-Javadoc)
	 * @see s.IDoomSound#StartSoundAtVolume(p.mobj_t, int, int)
	 */

	@Override
	public void StartSoundAtVolume(mobj_t origin, int sfx_id, int volume) {

		  boolean		rc;
		  int		priority;
		  sfxinfo_t	sfx;
		  int		cnum;
		
			// check for bogus sound #
		  if (sfx_id < 1 || sfx_id > sfxenum_t.NUMSFX.ordinal())
		    DS.I.Error("Bad sfx #: %d", sfx_id);
		  
		  sfx = sounds.S_sfx[sfx_id];
		  
		  // Initialize sound parameters
		  if (sfx.link != null)
		  {
		    spars.pitch = sfx.pitch;
		    priority = sfx.priority;
		    spars.volume += sfx.volume;
		    
		    if (volume < 1)
		      return;
		    
		    if (volume > DS.snd_SfxVolume)
		      volume = DS.snd_SfxVolume;
		  }	
		  else
		  {
			 spars.pitch = NORM_PITCH;
		    priority = NORM_PRIORITY;
		  }
		  
		  // Check to see if it is audible,
		  //  and if not, modify the params
		  if (origin!=null && origin != DS.players[DS.consoleplayer].mo)
		  {
		    rc = AdjustSoundParams(DS.players[DS.consoleplayer].mo,
					     origin,
					     spars);
			
		    if ( origin.x == DS.players[DS.consoleplayer].mo.x
			 && origin.y == DS.players[DS.consoleplayer].mo.y)
		    {	
		    	spars.sep 	= NORM_SEP;
		    }
		    
		    if (!rc)
		      return;
		  }	
		  else
		  {
			  spars.sep = NORM_SEP;
		  }
		  
		  // hacks to vary the sfx pitches
		  if (sfx_id >= sfxenum_t.sfx_sawup.ordinal()
		      && sfx_id <= sfxenum_t.sfx_sawhit.ordinal())
		  {	
		    spars.pitch += 8 - (DS.RND.M_Random()&15);
		    
		    if (spars.pitch<0)
		    	spars.pitch = 0;
		    else if (spars.pitch>255)
		    	spars.pitch = 255;
		  }
		  else if (sfx_id != sfxenum_t.sfx_itemup.ordinal()
			   && sfx_id != sfxenum_t.sfx_tink.ordinal())
		  {
			  spars.pitch += 16 - (DS.RND.M_Random()&31);
		    
		    if (spars.pitch<0)
		    	spars.pitch = 0;
		    else if (spars.pitch>255)
		    	spars.pitch = 255;
		  }
		  
		  
		  // Get it from the lump, if it's not "DSNONE"
		  System.out.println(sfx_id+":"+sounds.S_sfx[sfx_id]);
		String id = "DS"+sounds.S_sfx[sfx_id].name.toUpperCase();
		if (id.equals("DSNONE"))
			return;

		
		// This corresponds roughly with finding the lump.
		DoomSound sound = findSound(id);
		if (sound == null)
			return;

		try {
			//sound.ais.reset();
			// TODO: why not cache ByteOutputStreams instead?
			sound.ais = AudioSystem.getAudioInputStream(new ByteInputStream(sound.bytes, sound.bytes.length));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// We now have the sample format.
		AudioFormat format = sound.ais.getFormat();
		
		  // kill old sound
		  StopSound(origin);

		  // Try to find a channel that's playing the 
		  // same sound, if possible.
		  cnum = getChannel(origin, sfx,format);
		  
		  System.out.println("Channel found "+cnum);
		  
		  if (cnum<0)
		    return;
		  
		  //
		  // This is supposed to handle the loading/caching.
		  // For some odd reason, the caching is done nearly
		  //  each time the sound is needed?
		  //

		// Search through all sound threads for a free one.

		  // increase the usefulness
		  if (sfx.usefulness++ < 0)
		    sfx.usefulness = 1;
		
		  // Assigns the handle to one of the channels in the
		  //  mix/output buffer.
		  channels[cnum].currentSound=sound;
		  channels[cnum].handle = StartSound(sfx_id,
						       spars,
						       priority);
		  }
	
	
	//
	// S_getChannel :
	//   If none available, return -1.  Otherwise channel #.
	//
	public int 	getChannel( mobj_t origin,sfxinfo_t	sfxinfo, AudioFormat format)
	{
	    // channel number to use
	    int		cnum;
	    
	    channel_t	c;
	    
	    // Make sure there's at least one of the proper audio format
	    for (cnum=0 ; cnum<numChannels ; cnum++)
	    {
	    	if (channels[cnum].format==null){
	    		if (getAudioLineForChannel(channels[cnum], format)) break;
	    	}
	    }

	    // Find an open channel
	    for (cnum=0 ; cnum<numChannels ; cnum++)
	    {
		if (channels[cnum].currentSound==null) 
		    break;
		else if (origin!=null &&  channels[cnum].origin ==  origin)
		{
		    StopChannel(cnum);
		    break;
		}
	    }

	    // None available
	    if (cnum == numChannels)
	    {
		// Look for lower priority
		for (cnum=0 ; cnum<numChannels ; cnum++)
		    if (channels[cnum].sfxinfo.priority >= sfxinfo.priority) break;

		if (cnum == numChannels)
		{
		    // FUCK!  No lower priority.  Sorry, Charlie.    
		    return -1;
		}
		else
		{
		    // Otherwise, kick out lower priority.
		    StopChannel(cnum);
		}
	    }

	    c = channels[cnum];

	    // channel is decided to be cnum.
	    c.sfxinfo = sfxinfo;
	    c.origin = origin;
	    
	    System.out.printf("Assigned %s to channel %d\n",c.sfxinfo.name,cnum);

	    return cnum;
	}
	
	private final boolean getAudioLineForChannel(SoundThread str,AudioFormat format){
		try {		

			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			// Question: what happens to audiolines when they are done playing?
			str.auline = (SourceDataLine) AudioSystem.getLine(info);
			str.auline.open(format);
			// TODO: set individual per-thread volume controls here.
			str.fc = (FloatControl)str.auline.getControl(FloatControl.Type.VOLUME);
			// TODO: proper positioning.
			if (str.auline.isControlSupported(FloatControl.Type.PAN)) {
				str.pc = (FloatControl) str.auline
				.getControl(FloatControl.Type.PAN);
			}
			str.format=format;
			/*	if (curPosition == Position.RIGHT) 
					str.pc.setValue(1.0f);
				else if (curPosition == Position.LEFT) 
					str.pc.setValue(-1.0f);
				} */
				
				System.out.printf("Initialized channel %d\n",str.id);

			// Register this thread as suitable for a certain format.
			str.auline.start();
			return true;
			//str.auline.addLineListener(this);
		} catch (LineUnavailableException e) { 
			System.err.println("Line unavailable!");
			return false; 
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void StopMusic() {
		sequencer.stop();
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
	public void update(LineEvent event) {
		System.out.println("LineEvent: "+event.getType().toString());
		
	}

	public boolean  SoundIsPlaying(channel_t c){
		return false;
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
			sthreads[i].join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	  
	  
	  
	  // Done.
	  return;
	}

	@Override
	public void ShutdownMusic() {
		sequencer.stop();
		sequencer.close();
		
	}
	
	@Override
	public void UpdateSounds(mobj_t listener) {
		    boolean		audible;
		    int		cnum;
		    sfxinfo_t	sfx;
		    channel_t	c;
		    
		    // Clean up unused data.
		    // This is currently not done for 16bit (sounds cached static).
		    // DOS 8bit remains. 
		    /*if (gametic.nextcleanup)
		    {
			for (i=1 ; i<NUMSFX ; i++)
			{
			    if (S_sfx[i].usefulness < 1
				&& S_sfx[i].usefulness > -1)
			    {
				if (--S_sfx[i].usefulness == -1)
				{
				    Z_ChangeTag(S_sfx[i].data, PU_CACHE);
				    S_sfx[i].data = 0;
				}
			    }
			}
			nextcleanup = gametic + 15;
		    }*/
		    
		    for (cnum=0 ; cnum<numChannels ; cnum++)
		    {
		    
			c = channels[cnum];

			//System.out.printf("Updating channel %d %s\n",cnum,c);
			if (c.currentSound !=null)
			{
			    if (SoundIsPlaying(c.handle))
			    {
				// initialize parameters
				spars.volume = DS.snd_SfxVolume;
				spars.pitch = NORM_PITCH;
				spars.sep = NORM_SEP;

				sfx=c.sfxinfo;
				
				if (sfx.link!=null)
				{
					spars.pitch = sfx.pitch;
					spars.volume += sfx.volume;
				    if (spars.volume < 1)
				    {
					StopChannel(cnum);
					continue;
				    }
				    else if (spars.volume > DS.snd_SfxVolume)
				    {
				    	spars.volume = DS.snd_SfxVolume;
				    }
				}

				// check non-local sounds for distance clipping
				//  or modify their params
				if (c.origin!=null && (listener != c.origin))
				{
				    audible = AdjustSoundParams(listener,
								  c.origin,
								  spars);
				    
				    if (!audible)
				    {
					StopChannel(cnum);
				    }
				    else ;
					//USELESS UpdateSoundParams(c.handle, spars);
				}
			    }
			    else
			    {
				// if channel is allocated but sound has stopped,
				//  free it
				StopChannel(cnum);
			    }
			}
		    }
		    // kill music if it is a single-play && finished
		    // if (	mus_playing
		    //      && !I_QrySongPlaying(mus_playing->handle)
		    //      && !mus_paused )
		    // S_StopMusic();
		}
	
}

