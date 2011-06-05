package s;

import i.system;
import p.mobj_t;
import data.musicinfo_t;
import data.sfxinfo_t;
import data.sounds;
import data.sounds.sfxenum_t;
import doom.DoomStatus;
import static m.fixed_t.*;
import static data.Tables.*;
import static data.sounds.S_sfx;

/** Some stuff that is not implementation dependant */


public abstract class AbstractDoomAudio implements IDoomSound{

	protected final DoomStatus DS;
	
	protected final int numChannels;
	protected channel_t[] channels;
	protected final int[] channelids;

	/** Time/gametic that the channel started playing,
        used to determine oldest, which automatically
        has lowest priority. In case number of active sounds exceeds
        available channels.
    */  
      
	protected final int[] channelstart;
	protected boolean mus_paused;
	protected musicinfo_t mus_playing;
	protected int nextcleanup;
	
	// Pitch to stepping lookup, unused.
	protected final int[]		steptable=new int[256];
	
	public AbstractDoomAudio(DoomStatus DS, int numChannels){
		this.DS = DS;
		this.numChannels=numChannels;
		channelstart=new int[numChannels];
		channelids=new int[numChannels];
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


	protected final static int S_ATTENUATOR(){
		return ((S_CLIPPING_DIST-S_CLOSE_DIST)>>FRACBITS);
	}

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
	
	protected class sndpars_t{
		int volume;
		int pitch;
		int sep;
		int priority;
	}
	
	protected sndpars_t spars=new sndpars_t();
	//
	// Changes volume, stereo-separation, and pitch variables
	//  from the norm of a sound effect to be played.
	// If the sound is not audible, returns a 0.
	// Otherwise, modifies parameters and returns 1.
	//
	public boolean 
	AdjustSoundParams
	( mobj_t	listener,
	  mobj_t	source,
	  sndpars_t pars)
	{
	    int	approx_dist;
	    int	adx;
	    int	ady;
	    long	angle;

	    // calculate the distance to sound origin
	    //  and clip it if necessary
	    adx = Math.abs(listener.x - source.x);
	    ady = Math.abs(listener.y - source.y);

	    // From _GG1_ p.428. Appox. eucledian distance fast.
	    approx_dist = adx + ady - ((adx < ady ? adx : ady)>>1);
	    
	    if (DS.gamemap != 8
		&& approx_dist > S_CLIPPING_DIST)
	    {
		return false;
	    }
	    
	    // angle of source to listener
	    angle = rr.RendererState.PointToAngle(listener.x,
				    listener.y,
				    source.x,
				    source.y);

	    if (angle > listener.angle)
		angle = angle - listener.angle;
	    else
		angle = angle + (0xffffffffL - listener.angle&BITS32);

	    angle&=BITS32;
	    angle >>= ANGLETOFINESHIFT;

	    // stereo separation
	    pars.sep = 128 - (FixedMul(S_STEREO_SWING,finesine[(int) angle])>>FRACBITS);

	    // volume calculation
	    if (approx_dist < S_CLOSE_DIST)
	    {
		pars.volume = DS.snd_SfxVolume-15;
	    }
	    else if (DS.gamemap == 8)
	    {
		if (approx_dist > S_CLIPPING_DIST)
		    approx_dist = S_CLIPPING_DIST;

		pars.volume = 15+ ((DS.snd_SfxVolume-15)
			    *((S_CLIPPING_DIST - approx_dist)>>FRACBITS))
		    / S_ATTENUATOR();
	    }
	    else
	    {
		// distance effect
	    	pars.volume = (DS.snd_SfxVolume
			* ((S_CLIPPING_DIST - approx_dist)>>FRACBITS))
		    / S_ATTENUATOR(); 
	    }
	    
	    return (pars.volume > 0);
	}




	//
	// S_getChannel :
	//   If none available, return -1.  Otherwise channel #.
	//
	public int 	getChannel( mobj_t origin,sfxinfo_t	sfxinfo )
	{
	    // channel number to use
	    int		cnum;
	    
	    channel_t	c;

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

	    return cnum;
	}

	public final void
	UpdateSoundParams
	( int	handle,
	  int	vol,
	  int	sep,
	  int	pitch)
	{
	  // I fail too see that this is used.
	  // Would be using the handle to identify
	  //  on which channel the sound might be active,
	  //  and resetting the channel parameters.

	  // UNUSED.
	  handle = vol = sep = pitch = 0;
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

			System.out.printf("Updating channel %d %s\n",cnum,c);
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
	
	/** Nice one. A sound should have a maximum duration in tics,
	 * and we can give it a handle proportional to the future tics
	 * it should play until. Ofc, this means the minimum timeframe
	 * for cutting a soung off is just 1 tic.
	 * 
	 * @param handle
	 * @return
	 */
	
	public boolean SoundIsPlaying(int handle)
	{
	    // Ouch.
	    return (DS.gametic < handle);
	}
	
	public final void StopChannel(int cnum)
	{

	    int		i;
	    channel_t	c = channels[cnum];

	    if (c.currentSound!=null)
	    {
		// stop the sound playing
		if (SoundIsPlaying(c.handle))
		{
		    StopSound(c.handle);
		}

		// check to see
		//  if other channels are playing the sound
		for (i=0 ; i<numChannels ; i++)
		{
		    if (cnum != i
			&& (c.sfxinfo!=null && (channels[i].sfxinfo!=null) && (c.sfxinfo == channels[i].sfxinfo)))		
		    {
			break;
		    }
		}
		
		// degrade usefulness of sound data
		c.sfxinfo.usefulness--;

		c.currentSound = null;
	    }
	}
	
	public void StopSound(mobj_t origin)
	{

	    int cnum;

	    for (cnum=0 ; cnum<numChannels ; cnum++)
	    {
		if (channels[cnum].currentSound!=null && channels[cnum].origin == origin)
		{
		    StopChannel(cnum);
		    break;
		}
	    }
	}
	
	public void StopSound (int handle)
	{
	  // You need the handle returned by StartSound.
	  // Would be looping all channels,
	  //  tracking down the handle,
	  //  an setting the channel to zero.
		
	    for (int cnum=0 ; cnum<numChannels ; cnum++)
	    {
		if (channels[cnum].currentSound!=null && channels[cnum].handle == handle)
		{
		    StopChannel(cnum);
		    break;
		}
	    }
	}
	
	
	/*
	@Override
	public void StopChannel(int channel){
		this.channels[channel].stopSound();
	} */

	
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

	  
	  // increase the usefulness
	  if (sfx.usefulness++ < 0)
	    sfx.usefulness = 1;
	  
	  // Assigns the handle to one of the channels in the
	  //  mix/output buffer.
	  channels[cnum].handle = StartSound(sfx_id,
					       spars,
					       priority);
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
	    //channels[slot].DoomSound = S_sfx[sfxid].data;

	    // Reset current handle number, limited to 0..100.
	    if (handlenums==0) // was !handlenums, so it's actually 1...100?
		handlenums = 100;

	    // Assign current handle number.
	    // Preserved so sounds could be stopped (unused).
	    //channelhandles[slot] = rc = handlenums++;

	    // Set stepping???
	    // Kinda getting the impression this is never used.
	    //channelstep[slot] = step;
	    // ???
	    //channelstepremainder[slot] = 0;
	    // Should be gametic, I presume.
	    channelstart[slot] = DS.gametic;

	    // Separation, that is, orientation/stereo.
	    //  range is: 1 - 256
	    seperation += 1;

	    // Per left/right channel.
	    //  x^2 seperation,
	    //  adjust volume properly.
	    /*leftvol =
		volume - ((volume*seperation*seperation) >> 16); ///(256*256);
	    seperation = seperation - 257;
	    rightvol =
		volume - ((volume*seperation*seperation) >> 16);	
		*/

	    // Sanity check, clamp volume.
	    /*
	    if (rightvol < 0 || rightvol > 127)
		system.Error("rightvol out of bounds");
	    
	    if (leftvol < 0 || leftvol > 127)
		system.Error("leftvol out of bounds"); */
	    
	    // Get the proper lookup table piece
	    //  for this volume level???
	    // TODO: more pointer magic...
	   // channelleftvol_lookup[slot] = vol_lookup[leftvol*256];
	   // channelrightvol_lookup[slot] = vol_lookup[rightvol*256];

	    // Preserve sound SFX id,
	    //  e.g. for avoiding duplicates of chainsaw.
	    channelids[slot] = sfxid;

	    // You tell me.
	    return rc;
	}
	
	//
	// Starting a sound means adding it
	//  to the current list of active sounds
	//  in the internal channels.
	// As the SFX info struct contains
	//  e.g. a pointer to the raw data,
	//  it is ignored.
	// As our sound handling does not handle
	//  priority, it is ignored.
	// Pitching (that is, increased speed of playback)
	//  is set, but currently not used by mixing.
	//
	public int
	StartSound
	( int		id,
	  sndpars_t pars,
	  int		priority )
	{
	    id = addsfx( id, pars.volume, steptable[pars.pitch], pars.sep );
	    return id;
	}
	
	// SFX API
	// Note: this was called by S_Init.
	// However, whatever they did in the
	// old DPMS based DOS version, this
	// were simply dummies in the Linux
	// version.
	// See soundserver initdata().
	//
	public void SetChannels()
	{
	  // Init internal lookups (raw data, mixing buffer, channels).
	  // This function sets up internal lookups used during
	  //  the mixing process. 
	  int		i;
	  int		j;
	    
	  int/**/	steptablemid = /*steptable +*/ 128;
	  
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
	  //for (i=0 ; i<128 ; i++)
	  //  for (j=0 ; j<256 ; j++)
	  //    vol_lookup[i*256+j] = (i*(j-128)*256)/127;
	}
	
}
