package s;

import static data.sounds.S_sfx;
import static m.fixed_t.FRACBITS;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

import p.mobj_t;

import w.DoomBuffer;

import com.sun.corba.se.impl.ior.ByteBuffer;

import data.sfxinfo_t;
import data.sounds;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** A close recreation of the classic linux doom sound driver.
 * 
 * @author Kaptain Zyklon
 *
 */

public class ClassicDoomSoundDriver extends AbstractDoomAudio{

	public ClassicDoomSoundDriver(DoomStatus DS, int numChannels){
		super(DS,numChannels);
		channelstart=new int[numChannels];
		channelids=new int[numChannels];
		channelleftvol_lookup=new int[numChannels][];
		channelrightvol_lookup=new int[numChannels][];
		channelstep= new int[numChannels];
		channelstepremainder= new int[numChannels];
	}

	// Purpose?
	final char snd_prefixen[]
	                        = { 'P', 'P', 'A', 'S', 'S', 'S', 'M', 'M', 'M', 'S', 'S', 'S' };

	protected final static int MAX_VOLUME=		127;

	// when to clip out sounds
	// Does not fit the large outdoor areas.
	protected final static int  S_CLIPPING_DIST	=	(1200*0x10000);

	// Distance tp origin when sounds should be maxed out.
	// This should relate to movement clipping resolution
	// (see BLOCKMAP handling).
	// Originally: (200*0x10000).
	protected final static int  S_CLOSE_DIST	=	(160*0x10000);


	// Adjustable by menu.
	//protected final int NORM_VOLUME    		snd_MaxVolume

	protected final static int NORM_PITCH    = 		128;
	protected final static int NORM_PRIORITY	=	64;
	protected final static int NORM_SEP		=128;

	protected final static int S_PITCH_PERTURB	=	1;
	protected final static int S_STEREO_SWING	=	(96*0x10000);

	// percent attenuation from front to back
	protected final static int S_IFRACVOL	=	30;

	protected final static int NA	=		0;
	protected final static int S_NUMCHANNELS=		2;


	/** The actual lengths of all sound effects. */
	protected final int[] lengths=new int[NUMSFX];

	/** The global mixing buffer.
	 * Basically, samples from all active internal channels
	 *  are modifed and added, and stored in the buffer
	 *  that is submitted to the audio device.
	 *  This is a 16-bit stereo signed PCM mixbuffer.
	 *  Memory order is LSB (?) and channel order is L-R-L-R...
	 *  
	 *  
	 */

	protected final byte[]	mixbuffer=new byte[MIXBUFFERSIZE*2];

	/** Current pointer into mixbuffer */
	protected int p_mixbuffer;

	/** The channel step amount... */
	protected final int[]	channelstep;

	/** ... and a 0.16 bit remainder of last step. */
	protected final int[]	channelstepremainder;

	/** Time/gametic that the channel started playing,
    used to determine oldest, which automatically
    has lowest priority. In case number of active sounds exceeds
    available channels. */  
	int[]		channelstart=new int[NUM_CHANNELS];

	/** The sound in channel handles,
	   determined on registration,
	    might be used to unregister/stop/modify,
	   currently unused. */
	int[] 		channelhandles=new int[NUM_CHANNELS];

	// SFX id of the playing sound effect.
	// Used to catch duplicates (like chainsaw).
	int[]	channelids=new int[NUM_CHANNELS];			

	// Pitch to stepping lookup, unused.
	int[]		steptable=new int[256];

	/** The channel data pointers, start and end.
	 *  These were referred to as "channels" in two different source
	 *  files: s_sound.c and i_sound.c. In s_sound.c they are actually
	 *  channel_t (they are only informational).
	 *  In i_sound.c they are actual data channels. TODO: find out
	 *  where to place the boundary between the two.
	 *  
	 *  */
	byte[][]	dchannels= new byte[NUM_CHANNELS][];
	// MAES: we'll have to use this for actual pointing. channels[] holds just the data.
	int[] dchannels_ptr=new int[NUM_CHANNELS];
	// The second one is supposed to point at "the end", so I'll make it an int.
	int[]	dchannelsend= new int[NUM_CHANNELS];

	/** Volume lookups. 128 levels*/
	protected final int[][]		vol_lookup=new int[128][256];

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
		int	sample;
		int		dl;
		int		dr;

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
		rightout = 1;
		step = 2;

		// Determine end, for left channel only
		//  (right channel is implicit). 
		// MAES: this implies that the buffer will only mix 
		// that many samples at a time, and that the size is just right.
		// Thus, it must be flushed (p_mixbuffer=0) before reusing it.
		leftend =p_mixbuffer + SAMPLECOUNT*step;

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
			for ( chan = 0; chan < numChannels; chan++ )
			{
				// Check channel, if active.
				// MAES: this means that we must point to raw data here.
				if (dchannels[ chan ]!=null)
				{

					int channel_pointer=	dchannels_ptr[chan];
					// Get the raw data from the channel.
					// Maes: this is supposed to be an 8-bit unsigned value.
					sample = 0x00FF&dchannels[ chan ][channel_pointer];
					// Add left and right part  for this channel (sound)
					//  to the current data. Adjust volume accordingly.
					// TODO: where are those set?
					dl += channelleftvol_lookup[ chan ][sample];
					dr += channelrightvol_lookup[ chan ][sample];

					// This should increment the index inside a channel, but is
					// expressed in 16.16 fixed point arithmetic.
					channelstepremainder[ chan ] += channelstep[ chan ];

					// The actual channel pointer is increased here.	
					// The above trickery allows playing back different pitches.
					// The shifting retains only the integer part.
					channel_pointer+=channelstepremainder[ chan ] >> 16;

			// This limits it to the "decimal" part in order to
			// avoid undue accumulation.
			channelstepremainder[ chan ] &= 0xFFFF;

			// Check whether we are done. Also to avoid overflows.
			if (channel_pointer >= dchannelsend[ chan ])
				// Reset pointer for a channel.
				channel_pointer = 0;

			// Write pointer back, so we know where a certain channel
			// is the next time UpdateSounds is called.
			dchannels_ptr[chan]=channel_pointer;

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
			leftout += step*2;
			rightout += step*2;
		} // End leftend/leftout while

		// TODO how do we know whether the mixbuffer isn't entirely used 
		// and instead it has residual garbage samples in it?
		// TODO: what's the purpose of channelremainder etc?

	}

	/** SFX API  Note: this was called by S_Init.
	 * However, whatever they did in the old DPMS based DOS version, this
	 * were simply dummies in the Linux version.
	 * See soundserver initdata().
	 */

	public void SetChannels()
	{
		// Init internal lookups (raw data, mixing buffer, channels).
		// This function sets up internal lookups used during
		//  the mixing process. 
		int		i;
		int		j;

		int steptablemid =  128;

		// Okay, reset internal mixing channels to zero.
		/*for (i=0; i<NUM_CHANNELS; i++)
	  {
	    channels[i] = 0;
	  }*/

		// This table provides step widths for pitch parameters.
		// I fail to see that this is currently used.
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
	int GetSfxLumpNum(sfxinfo_t sfx)
	{
		String namebuf;
		namebuf=String.format("ds%s", sfx.name);
		return DS.W.GetNumForName(namebuf);
	}
	
	MixServer S0UNDSRV;
	Thread SOUNDTHREAD;

	protected void
	InitSound()
	{ 

		int i;

		// Secure and configure sound device first.
		System.err.println("I_InitSound: ");	  

		// PCM, signed, 16-bit, stereo, 11025 KHz, 2048 bytes per "frame", maximum of 44100/2048 "fps"
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				SAMPLERATE,
				SAMPLESIZE,
				2,
				4,
				11025,
				true);


		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		SourceDataLine line = null;

		if (AudioSystem.isLineSupported(info))
			try {
				line=  (SourceDataLine) AudioSystem.getSourceDataLine(format);
			} catch (Exception e){
				e.printStackTrace();
				//fprintf(stderr, "Could not play signed 16 data\n");
			}

			if (line!=null) System.err.print(" configured audio device\n" );
			
			S0UNDSRV=new MixServer(line);
			SOUNDTHREAD=new Thread(S0UNDSRV);			
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
			for ( i = 0; i< MIXBUFFERSIZE; i++ )
				mixbuffer[i] = 0;

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
		name=String.format("ds%s", sfxname);

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
		paddedsfx = new byte[paddedsize+8];

		// Now copy and pad.
		System.arraycopy(sfx,8, paddedsfx, 8,size-8 );
		// Hmm....silence?
		for (i=size ; i<paddedsize+8 ; i++)
			paddedsfx[i] = (byte) 128;

		// Remove the cached lump.
		DS.Z.Free( DS.W.CacheLumpNum (sfxlump,0,DoomBuffer.class) );

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
		dchannels[slot] = S_sfx[sfxid].data;

		// Reset current handle number, limited to 0..100.
		if (handlenums==0) // was !handlenums, so it's actually 1...100?
			handlenums = 100;

		// Assign current handle number.
		// Preserved so sounds could be stopped (unused).
		channelhandles[slot] = rc = handlenums++;

		// Set stepping???
		// Kinda getting the impression this is never used.
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

		// You tell me.
		return rc;
	}

	public void
	StartSoundAtVolume
	( mobj_t		origin,
			int		sfx_id,
			int		volume )
	{

		boolean		rc;
		//int		sep;
		//int		pitch;
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

		// kill old sound
		StopSound(origin);

		// try to find a channel
		cnum = getChannel(origin, sfx);

		if (cnum<0)
			return;

		//
		// This is supposed to handle the loading/caching.
		// For some odd reason, the caching is done nearly
		//  each time the sound is needed?
		//

		// get lumpnum if necessary
		if (sfx.lumpnum < 0)
			sfx.lumpnum = GetSfxLumpNum(sfx);

		// increase the usefulness
		if (sfx.usefulness++ < 0)
			sfx.usefulness = 1;

		// Assigns the handle to one of the channels in the
		//  mix/output buffer.
		//  channels[cnum].handle = StartSound(sfx_id,
		//				       spars,
		//				       priority);
	}

	@Override
	public void Init(int sfxVolume, int musicVolume) {
		this.InitSound();
		// TODO Auto-generated method stub

	}

	@Override
	public void Start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void StartSound(mobj_t origin, int sound_id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void StartSound(mobj_t origin, sfxenum_t sound_id) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void StopSound(mobj_t origin)
	{

	    int cnum;

	    for (cnum=0 ; cnum<numChannels ; cnum++)
	    {
		if (dchannels[cnum]!=null && channels[cnum].origin == origin)
		{
		    StopChannel(cnum);
		    break;
		}
	    }
	}
	
	

	@Override
	public void StartMusic(musicenum_t musicid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ChangeMusic(musicenum_t musicid, boolean looping) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ChangeMusic(int musicid, boolean looping) {
		// TODO Auto-generated method stub

	}

	@Override
	public void StopMusic() {
		// TODO Auto-generated method stub

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

	}

	@Override
	public void SetSfxVolume(int volume) {
		// TODO Auto-generated method stub

	}

	@Override
	public void StartMusic(int music_id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ShutdownSound() {
		// TODO Auto-generated method stub

	}

	@Override
	public void ShutdownMusic() {
		// TODO Auto-generated method stub

	}

	private class MixServer implements Runnable {			

		public boolean terminate;

		public MixServer(SourceDataLine line){
			this.auline=line;
		}

		private SourceDataLine auline;

		public void run() {

			while (!terminate) {

				int active=0;

				do {
					System.out.println("Nothing playing. Waiting...");
					active=0;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					} 
					for (int i=0;i<numChannels;i++)
						if (dchannels[i]!=null) active++;
				} while (active==0);


				try{
					auline.write(mixbuffer, 0, mixbuffer.length);
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
			}
		}
	}
}
