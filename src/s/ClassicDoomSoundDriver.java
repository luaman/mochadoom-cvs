package s;

import static data.sounds.S_sfx;
import static m.fixed_t.FRACBITS;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import w.DoomBuffer;

import data.sfxinfo_t;
import data.sounds;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** A close recreation of the classic linux doom sound mixer.
 * 
 * @author velktron
 *
 */

public class ClassicDoomSoundDriver implements ISound {
	
	private final byte[] MASTER_BUFFER;
	private final int BUFFER_CHUNKS=1;
	
	protected final static boolean D=true;
	
	private final DoomStatus DS;
	private final int numChannels;
	private volatile int active=0;
	private int chunk=0;

	// The one and only line
	private SourceDataLine line = null;
	
	HashMap<Integer,byte[]> cachedSounds=new HashMap<Integer,byte[]> ();
	
	public ClassicDoomSoundDriver(DoomStatus DS, int numChannels){
		this.DS=DS;
		this.numChannels=numChannels;
		channelstart=new int[numChannels];
		channelids=new int[numChannels];
		channelleftvol_lookup=new int[numChannels][];
		channelrightvol_lookup=new int[numChannels][];
		channelstep= new int[numChannels];
		channelstepremainder= new int[numChannels];
		channels=new byte[numChannels][];
		p_channels=new int[numChannels];
		channelsend=new int[numChannels];
		channelhandles=new int[numChannels];
		//chunk=Future.new Future<Integer>();
		MASTER_BUFFER=new byte[mixbuffer.length*BUFFER_CHUNKS];
	}

	/** The actual lengths of all sound effects. */
	private final int[] lengths=new int[NUMSFX];

	/** The global mixing buffer.
	 * Basically, samples from all active internal channels
	 *  are modifed and added, and stored in the buffer
	 *  that is submitted to the audio device.
	 *  This is a 16-bit stereo signed PCM mixbuffer.
	 *  Memory order is LSB (?) and channel order is L-R-L-R...
	 *  
	 *  
	 */

	private final byte[]	mixbuffer=new byte[MIXBUFFERSIZE*2];

	/** Current pointer into mixbuffer */
	private int p_mixbuffer;

	/** The channel step amount... */
	private final int[]	channelstep;

	/** ... and a 0.16 bit remainder of last step. */
	private final int[]	channelstepremainder;

	/** Time/gametic that the channel started playing,
    used to determine oldest, which automatically
    has lowest priority. In case number of active sounds exceeds
    available channels. */  
	private final int[]		channelstart;

	/** The sound in channel handles, determined on registration,
	    might be used to unregister/stop/modify, currently unused. */
	
	private final int[] channelhandles;

	// SFX id of the playing sound effect.
	// Used to catch duplicates (like chainsaw).
	private final int[]	channelids;			

	// Pitch to stepping lookup, unused.
	private final int[]		steptable=new int[256];

	/** The channel data pointers, start and end.
	 *  These were referred to as "channels" in two different source
	 *  files: s_sound.c and i_sound.c. In s_sound.c they are actually
	 *  channel_t (they are only informational).
	 *  In i_sound.c they are actual data channels. TODO: find out
	 *  where to place the boundary between the two.
	 *  
	 *  */
	byte[][]	channels;
	
	/** MAES: we'll have to use this for actual pointing. channels[] holds just the data. */
	int[] p_channels;
	
	/** The second one is supposed to point at "the end", so I'll make it an int. */
	int[]	channelsend;

	/** Volume lookups. 128 levels*/
	protected final int[][]	vol_lookup=new int[128][256];

	/** Hardware left and right channel volume lookup. */
	protected final int[][]	channelleftvol_lookup,channelrightvol_lookup;

	/**
	 * This function loops all active (internal) sound
	 *  channels, retrieves a given number of samples
	 *  from the raw sound data, modifies it according
	 *  to the current (internal) channel parameters,
	 *  mixes the per channel samples into the global
	 *  mixbuffer, clamping it to the allowed range,
	 *  and sets up everything for transferring the
	 *  contents of the mixbuffer to the (two)
	 *  hardware channels (left and right, that is).
	 *
	 * This function currently supports only 16bit.
	 */

	public void UpdateSound()
	{
		/*
	#ifdef SNDINTR
	  // Debug. Count buffer misses with interrupt.
	  static int misses = 0;
	#endif*/


		// Mix current sound data.
		// Data, from raw sound, for right and left.
		int	sample = 0;
		int		dl;
		int		dr;
		boolean mixed=false;

		// Pointers in global mixbuffer, left, right, end.
		// Maes: those were explicitly signed short pointers...

		int		leftout;
		int		rightout;
		int		leftend;
		// Step in mixbuffer, left and right, thus two.
		int				step;

		// Mixing channel index.
		int				chan;

		// POINTERS to Left and right channel
		// which are in global mixbuffer, alternating.
		leftout = 0;
		rightout = 2;
		step = 4;
		p_mixbuffer=0;

		// Determine end, for left channel only
		//  (right channel is implicit). 
		// MAES: this implies that the buffer will only mix 
		// that many samples at a time, and that the size is just right.
		// Thus, it must be flushed (p_mixbuffer=0) before reusing it.
		leftend =SAMPLECOUNT*step;

		// Mix sounds into the mixing buffer.
		// Loop over step*SAMPLECOUNT,
		//  that is 512 values for two channels.
		while (leftout != leftend)
		{
			// Reset left/right value. 
			dl = 0;
			dr = 0;

			// Love thy L2 chache - made this a loop.
			// Now more channels could be set at compile time
			//  as well. Thus loop those  channels.
			
			active=0;
			
			for ( chan = 0; chan < numChannels; chan++ )
			{
				
				//if (D) System.err.printf("Checking channel %d\n",chan);
				// Check channel, if active.
				// MAES: this means that we must point to raw data here.
				if (channels[ chan ]!=null)
				{

					mixed=true;
					int channel_pointer=	p_channels[chan];
					
					//if (D && leftout%1024 ==0 ) System.err.printf("Mixing channel %d %d %d\n",chan,leftout,channel_pointer);
					// Get the raw data from the channel.
					// Maes: this is supposed to be an 8-bit unsigned value.
					try {
					sample = 0x00FF&channels[ chan ][channel_pointer];

					} catch (Exception e){
						System.err.println(channel_pointer +" vs "+channels[ chan].length);
					}
					// Add left and right part  for this channel (sound)
					//  to the current data. Adjust volume accordingly.

					dl += channelleftvol_lookup[ chan ][sample];
					dr += channelrightvol_lookup[ chan ][sample];

					// This should increment the index inside a channel, but is
					// expressed in 16.16 fixed point arithmetic.
					channelstepremainder[ chan ] += channelstep[ chan ];
					//if (D && leftout%256 ==0 ) System.err.printf("Channel pntl %x.%x\n",channel_pointer,channelstepremainder[ chan ]);
					// The actual channel pointer is increased here.	
					// The above trickery allows playing back different pitches.
					// The shifting retains only the integer part.
					channel_pointer+=channelstepremainder[ chan ] >> 16;
			
			// This limits it to the "decimal" part in order to
			// avoid undue accumulation.
			channelstepremainder[ chan ] &= 0xFFFF;

			// Check whether we are done. Also to avoid overflows.
			if (channel_pointer >= channelsend[ chan ]){
				// Reset pointer for a channel.
				//channels[chan] = channel_pointer0;
				//System.err.printf("Channel %d done, stopping\n",chan);
				channels[chan]=null;
				channel_pointer=0;
			}

			// Write pointer back, so we know where a certain channel
			// is the next time UpdateSounds is called.
			p_channels[chan]=channel_pointer;

				}
			} // for all channels.

			// MAES: at this point, the actual values for a single sample
			// (YIKES!) are in d1 and d2. We must use the leftout/rightout 
			// pointers to write them back into the mixbuffer.

			// Clamp to range. Left hardware channel.
			// Has been char instead of short.
			// if (dl > 127) *leftout = 127;
			// else if (dl < -128) *leftout = -128;
			// else *leftout = dl;

			if (dl > 0x7fff)
				dl = 0x7fff;
			else if (dl < -0x8000)
				dl = -0x8000;

			
			
			// Shouldn't this be always executed?
			mixbuffer[leftout] = (byte) ((dl&0xFF00)>>>8);
			mixbuffer[leftout+1] = (byte) (dl&0x00FF);

			// Same for right hardware channel.
			if (dr > 0x7fff)
				dr = 0x7fff;
			else if (dr < -0x8000)
				dr = -0x8000;

			// Shouldn't this be always executed?
			mixbuffer[rightout] = (byte) ((dr&0xFF00)>>>8);
			mixbuffer[rightout+1] = (byte) (dr&0x00FF);
			
			// Increment current pointers in mixbuffer.
			leftout += 4;
			rightout += 4;
		} // End leftend/leftout while

		// TODO how do we know whether the mixbuffer isn't entirely used 
		// and instead it has residual garbage samples in it?
		// ANSWER: DOOM kind of padded samples in memory, so presumably
		// they all played silence.
		// TODO: what's the purpose of channelremainder etc?
		// ANSWER: pitch variations were done with fractional pointers 16.16
		// style.
		if (mixed) {
		System.arraycopy(mixbuffer, 0,MASTER_BUFFER, chunk*mixbuffer.length, mixbuffer.length);
		this.SOUNDSRV.addChunk(chunk);
		chunk++;
		chunk%=BUFFER_CHUNKS;
		}
		
	}

	/** SFX API  Note: this was called by S_Init.
	 * However, whatever they did in the old DPMS based DOS version, this
	 * were simply dummies in the Linux version.
	 * See soundserver initdata().
	 */

	@Override
	public void SetChannels(int numChannels) {
		// Init internal lookups (raw data, mixing buffer, channels).
		// This function sets up internal lookups used during
		//  the mixing process. 
		int		i;
		int		j;

		int steptablemid =  128;

		// Okay, reset internal mixing channels to zero.
		for (i=0; i<this.numChannels; i++)
	  {
	    channels[i] = null;
	  }

		// This table provides step widths for pitch parameters.
		// Values go from 16K to 256K roughly, with the middle of the table being
		// 64K, and presumably representing unitary pitch.
		// So the pitch variation can be quite extreme, allowing -/+ 400% stepping :-S
		for (i=-128 ; i<128 ; i++)
			steptable[steptablemid+i] = (int)(Math.pow(2.0, (i/64.0))*65536.0);


		// Generates volume lookup tables
		//  which also turn the unsigned samples
		//  into signed samples.
		for (i=0 ; i<128 ; i++)
			for (j=0 ; j<256 ; j++)
				vol_lookup[i][j] = (i*(j-128)*256)/127;
	}

	//
	// Retrieve the raw data lump index
	//  for a given SFX name.
	//
	public int GetSfxLumpNum(sfxinfo_t sfx)
	{
		String namebuf;
		namebuf=String.format("ds%s", sfx.name);
		return DS.W.GetNumForName(namebuf);
	}
	
	MixServer SOUNDSRV;
	Thread SOUNDTHREAD;

	@Override
	public void
	InitSound()
	{ 

		int i;

		// Secure and configure sound device first.
		System.err.println("I_InitSound: ");	  

		// We only need a single data line.
		// PCM, signed, 16-bit, stereo, 11025 KHz, 2048 bytes per "frame", maximum of 44100/2048 "fps"
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				SAMPLERATE,
				SAMPLESIZE,
				2,
				4,
				SAMPLERATE,
				true);


		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		

		if (AudioSystem.isLineSupported(info))
			try {
				line=  (SourceDataLine) AudioSystem.getSourceDataLine(format);
				line.open(format,this.MIXBUFFERSIZE);
			} catch (Exception e){
				e.printStackTrace();
				System.err.print( "Could not play signed 16 data\n");
			}

			if (line!=null) System.err.print(" configured audio device\n" );
			
			SOUNDSRV=new MixServer(line);
			SOUNDTHREAD=new Thread(SOUNDSRV);			
			SOUNDTHREAD.start();

			// Initialize external data (all sounds) at start, keep static.
			System.err.print("I_InitSound: ");

			for (i=1 ; i<NUMSFX ; i++)
			{ 
				// Alias? Example is the chaingun sound linked to pistol.
				if (sounds.S_sfx[i].link==null)
				{
					// Load data from WAD file.
					S_sfx[i].data = getsfx( S_sfx[i].name, lengths,i );
				}
				else
				{
					// Previously loaded already?
					S_sfx[i].data = S_sfx[i].link.data;
					lengths[i] = lengths[S_sfx[i].link.identify(S_sfx)];
				}
			}

			System.err.print(" pre-cached all sound data\n");

			// Now initialize mixbuffer with zero.
			for ( i = 0; i< MIXBUFFERSIZE; i+=4 ){
				mixbuffer[i] = (byte)(((int)( 0x8FFF*Math.sin((800*i)/MIXBUFFERSIZE))&0xff00)>>>8);
				mixbuffer[i+1] = (byte)((int)( 0x8FFF*Math.sin((800*i)/MIXBUFFERSIZE))&0xff);
				mixbuffer[i+2] = (byte)(((int)( 0x8FFF*Math.sin((800*i)/MIXBUFFERSIZE))&0xff00)>>>8);
				mixbuffer[i+3] = (byte)((int)( 0x8FFF*Math.sin((800*i)/MIXBUFFERSIZE))&0xff);

			}
			
			// Finished initialization.
			System.err.print("I_InitSound: sound module ready\n");

	}

	private byte[] getsfx
	( String         sfxname,
			int[]          len, int index )
	{
		byte[]      sfx;
		byte[]      paddedsfx;
		int                 i;
		int                 size;
		int                 paddedsize;
		String                name;
		int                 sfxlump;


		// Get the sound data from the WAD, allocate lump
		//  in zone memory.
		name=String.format("ds%s", sfxname).toUpperCase();

		// Now, there is a severe problem with the
		//  sound handling, in it is not (yet/anymore)
		//  gamemode aware. That means, sounds from
		//  DOOM II will be requested even with DOOM
		//  shareware.
		// The sound list is wired into sounds.c,
		//  which sets the external variable.
		// I do not do runtime patches to that
		//  variable. Instead, we will use a
		//  default sound for replacement.
		if ( DS.W.CheckNumForName(name) == -1 )
			sfxlump = DS.W.GetNumForName("dspistol");
		else
			sfxlump = DS.W.GetNumForName(name);

		size = DS.W.LumpLength( sfxlump );

		// Debug.
		// fprintf( stderr, "." );
		//fprintf( stderr, " -loading  %s (lump %d, %d bytes)\n",
		//	     sfxname, sfxlump, size );
		//fflush( stderr );

		sfx = DS.W.CacheLumpNumAsRawBytes( sfxlump ,0);

		// MAES: A-ha! So that's how they do it.
		// Pads the sound effect out to the mixing buffer size.
		// The original realloc would interfere with zone memory.
		paddedsize = ((size-8 + (SAMPLECOUNT-1)) / SAMPLECOUNT) * SAMPLECOUNT;

		// Allocate from zone memory.
		paddedsfx = new byte[paddedsize];

		// Now copy and pad.
		System.arraycopy(sfx,8, paddedsfx, 8,size-8 );
		// Hmm....silence?
		for (i=size ; i<paddedsize ; i++)
			paddedsfx[i] = (byte) 128;

		// Remove the cached lump.
		DS.Z.Free( DS.W.CacheLumpNum (sfxlump,0,DoomBuffer.class) );

		System.out.printf("SFX %d size %d padded to %d\n",index,size,paddedsize);
		// Preserve padded length.
		len[index] = paddedsize;

		// Return allocated padded data.
		// So the first 8 bytes are useless?
		return paddedsfx;
	}

	//
	// This function adds a sound to the
	//  list of currently active sounds,
	//  which is maintained as a given number
	//  (eight, usually) of internal channels.
	// Returns a handle.
	//
	protected short	handlenums = 0;

	int
	addsfx
	( int		sfxid,
			int		volume,
			int		step,
			int		seperation )
	{
		int		i;
		int		rc = -1;

		int		oldest = DS.gametic;
		int		oldestnum = 0;
		int		slot;

		int		rightvol;
		int		leftvol;

		// Chainsaw troubles.
		// Play these sound effects only one at a time.
		if ( sfxid == sfxenum_t.sfx_sawup.ordinal()
				|| sfxid == sfxenum_t.sfx_sawidl.ordinal()
				|| sfxid == sfxenum_t.sfx_sawful.ordinal()
				|| sfxid == sfxenum_t.sfx_sawhit.ordinal()
				|| sfxid == sfxenum_t.sfx_stnmov.ordinal()
				|| sfxid == sfxenum_t.sfx_pistol.ordinal()	 )
		{
			// Loop all channels, check.
			for (i=0 ; i<numChannels ; i++)
			{
				// Active, and using the same SFX?
				if ( (channels[i]!=null)
						&& (channelids[i] == sfxid) )
				{
					// Reset.
					channels[i] = null;
					// We are sure that iff,
					//  there will only be one.
					break;
				}
			}
		}

		// Loop all channels to find oldest SFX.
		for (i=0; (i<numChannels) && (channels[i]!=null); i++)
		{
			if (channelstart[i] < oldest)
			{
				oldestnum = i;
				oldest = channelstart[i];
			}
		}

		// Tales from the cryptic.
		// If we found a channel, fine.
		// If not, we simply overwrite the first one, 0.
		// Probably only happens at startup.
		if (i == numChannels)
			slot = oldestnum;
		else
			slot = i;

		// Okay, in the less recent channel,
		//  we will handle the new SFX.
		// Set pointer to raw data.
		channels[slot] = S_sfx[sfxid].data;
	    // Set pointer to end of raw data.
	    channelsend[slot] = lengths[sfxid];
		
		// Reset current handle number, limited to 0..100.
		if (handlenums==0) // was !handlenums, so it's actually 1...100?
			handlenums = 100;

		// Assign current handle number.
		// Preserved so sounds could be stopped (unused).
		// Maes: this should really be decreasing, otherwide handles 
		// should start at 0 and go towards 100. Just saying.
		channelhandles[slot] = rc = handlenums--;

		// Set stepping???
		// Kinda getting the impression this is never used.
		// MAES: you're wrong amigo.
		channelstep[slot] = step;
		// ???
		channelstepremainder[slot] = 0;
		// Should be gametic, I presume.
		channelstart[slot] = DS.gametic;

		// Separation, that is, orientation/stereo.
		//  range is: 1 - 256
		seperation += 1;

		// Per left/right channel.
		//  x^2 seperation,
		//  adjust volume properly.
		leftvol =
			volume - ((volume*seperation*seperation) >> 16); ///(256*256);
		seperation = seperation - 257;
		rightvol =
			volume - ((volume*seperation*seperation) >> 16);	


		// Sanity check, clamp volume.

		if (rightvol < 0 || rightvol > 127)
			DS.I.Error("rightvol out of bounds");

		if (leftvol < 0 || leftvol > 127)
			DS.I.Error("leftvol out of bounds"); 

		// Get the proper lookup table piece
		//  for this volume level???
		channelleftvol_lookup[slot] = vol_lookup[leftvol];
		channelrightvol_lookup[slot] = vol_lookup[rightvol];

		// Preserve sound SFX id,
		//  e.g. for avoiding duplicates of chainsaw.
		channelids[slot] = sfxid;

		System.err.println(channelStatus());
        if (D) System.err.printf("Playing sfxid %d handle %d length %d vol %d on channel %d\n",sfxid,rc,S_sfx[sfxid].data.length,volume,slot);
		
		// You tell me.
		return rc;
	}

	@Override
	public void ShutdownSound() {
		SOUNDSRV.terminate=true;
		
		try {
			SOUNDTHREAD.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private class MixServer implements Runnable {			

		public boolean terminate=false;

		public MixServer(SourceDataLine line){
			this.auline=line;
		}

		private SourceDataLine auline;
		

		private ArrayBlockingQueue<Integer> audiochunks=new ArrayBlockingQueue<Integer>(BUFFER_CHUNKS);
		
		public void addChunk(int chunk){
			audiochunks.offer(chunk);
		}
		
		public void run() {

			line.start();
			while (!terminate) {

				// Wait for a new sound chunk to be available.
				// Otherwise wait a bit.
				
				while (!audiochunks.isEmpty()){

				int chunk=0;
				try {
					chunk = audiochunks.take();
				} catch (InterruptedException e1) {
					// Should never happen.
				}
				int shit=0;
				try{
					//System.err.print("Writing audio out...");
					shit=line.write(MASTER_BUFFER, chunk*mixbuffer.length, mixbuffer.length);
					//System.err.print("..done writing\n");
				} catch (Exception e) { 
					System.err.println("Ehm...problem :-(");
					return;
				} finally {
					// The previous steps are actually VERY fast.
					// However this one waits until the data has been
					// consumed, Interruptions/signals won't reach  here,
					// so it's pointless trying to interrupt the actual filling.
					//System.err.print("Waiting on drain...");
					//long a=System.nanoTime();
					//auline.drain();					
					//long b=System.nanoTime();
					//double ms=(b-a)/1e6;
					//System.err.printf("Time: %f ms to play back %d bytes rate: %f\n",ms,shit,(1000.0*shit/ms));
					//System.err.printf(" %d bytes written",shit);
				}
			}
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}

	@Override
	public boolean SoundIsPlaying(int handle) {
		
		int c=getChannelFromHandle(handle);
		return (channels[c]==null);
		
		
		}

	/** Internal use. 
	 * 
	 * @param handle
	 * @return the channel that has the handle, or -2 if none has it.
	 */
	private int getChannelFromHandle(int handle){
		// Which channel has it?
		for (int i=0;i<numChannels;i++){
			if (channelhandles[i]==handle) return i;
		}
		
		return BUSY_HANDLE;
	}
	
	@Override
	public int StartSound(int id, int vol, int sep, int pitch, int priority) {
		
		if (id<1 || id>S_sfx.length-1) return BUSY_HANDLE;
	
		// MAES: apparently, we need not worry about finding available channels here,
		// just assign it to something.

		// Sound data not cached?
		
		if (S_sfx[id].data==null){
			S_sfx[id].data=retrieveSoundData(id);
		}
        
        // Find a free channel and get a timestamp/handle for the new sound.
        int handle=this.addsfx(id, vol, steptable[pitch], sep); 

        return handle;
	}

	/** Get raw sound data data for a particular lump, if not cached already.
	 * 
	 */
	
	private byte[] retrieveSoundData(int id) {
			
			byte[] sound = cachedSounds.get(id);
			
			if (sound==null){
			// OK, so we try to load the sound...
			// If it's vanilla, we obviously need to handle only its
			// own sample format with DoomToWave.
			
				String sid = "DS"+sounds.S_sfx[id].name.toUpperCase();
				// Shouldn't happen, but get null anyway.
				if (sid.equals("DSNONE")) return null;
				
				int lump=DS.W.CheckNumForName(sid);
				sound = this.DS.W.CacheLumpNumAsRawBytes(lump, data.Defines.PU_MUSIC);
			
	  
	        	// TODO: a cleaner way of handling this?
	        	
	        	// For now, just dump the raw data as it is. It will probably
	        	// sound bad...
	        	//DoomToWave dtw = new DoomToWave();
	        	//byte[] baos= dtw.DMX2Wave(bytes);
		        //sound = new DoomSound(baos);
		        cachedSounds.put(id, sound);

			}
		return sound;
			
		}

	@Override
	public void StopSound(int handle) {
		// Which channel has it?
		int  hnd=getChannelFromHandle(handle);
		if (hnd>=0) 
			channels[hnd]=null;
	}

	@Override
	public void SubmitSound() {

	}

	@Override
	public void UpdateSoundParams(int handle, int vol, int sep, int pitch) {
		
		int chan=this.getChannelFromHandle(handle);
		// Per left/right channel.
		//  x^2 seperation,
		//  adjust volume properly.
		int leftvol =
			vol - ((vol*sep*sep) >> 16); ///(256*256);
		sep = sep - 257;
		int rightvol =
			vol - ((vol*sep*sep) >> 16);	
		// TODO Auto-generated method stub
		
		// Sanity check, clamp volume.

		if (rightvol < 0 || rightvol > 127)
			DS.I.Error("rightvol out of bounds");

		if (leftvol < 0 || leftvol > 127)
			DS.I.Error("leftvol out of bounds"); 

		// Get the proper lookup table piece
		//  for this volume level???
		channelleftvol_lookup[chan] = vol_lookup[leftvol];
		channelrightvol_lookup[chan] = vol_lookup[rightvol];
		
		// Well, if you can get pitch to change too...
		this.channelstep[chan]=steptable[pitch];
		channelsend[chan] = this.lengths[this.channelids[chan]];
		
	}
	
	StringBuilder sb=new StringBuilder();
	
	public String channelStatus(){
		sb.setLength(0);
		for (int i=0;i<numChannels;i++){
			if (channels[i]!=null)
			sb.append(i);
			else sb.append('-');
		}
		
		return sb.toString();
		
		
	}
	
}
