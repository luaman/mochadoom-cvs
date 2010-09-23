package i;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: sound.java,v 1.3 2010/09/23 07:31:11 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// $Log: sound.java,v $
// Revision 1.3  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.2  2010/07/27 14:27:16  velktron
// Menu system is almost complete! Tester coming soon...
// Lots of additions to misc interfaces and C-like functions too.
//
// Revision 1.1  2010/07/04 01:42:38  velktron
// Some work on the "i" package...damn pointers.
//
//
// DESCRIPTION:
//	System interface for sound.
//
//-----------------------------------------------------------------------------

import static data.Defines.*;

import java.nio.ByteBuffer;

import w.WadLoader;

import data.sfxinfo_t;
import static data.sounds.*;
import data.sounds.sfxenum_t;
import doom.doomstat;

public class sound implements SystemSoundInterface{

	public WadLoader W;
	public doomstat ds;
	
public static final String rcsid = "$Id: sound.java,v 1.3 2010/09/23 07:31:11 velktron Exp $";


// UNIX hack, to be removed.
/*
#ifdef SNDSERV
// Separate sound server process.
FILE*	sndserver=0;
char*	sndserver_filename = "./sndserver ";
#elif SNDINTR*/

// Update all 30 millisecs, approx. 30fps synchronized.
// Linux resolution is allegedly 10 millisecs,
//  scale is microseconds.
public static final int SOUND_INTERVAL =    500;

// Get the interrupt. Set duration in millisecs.
//int I_SoundSetTimer( int duration_of_tick );
//void I_SoundDelTimer( void );


// A quick hack to establish a protocol between
// synchronous mix buffer updates and asynchronous
// audio writes. Probably redundant with gametic.
static int flag = 0;

// The number of internal mixing channels,
//  the samples calculated for each mixing step,
//  the size of the 16bit, 2 hardware channel (stereo)
//  mixing buffer, and the samplerate of the raw data.


// Needed for calling the actual sound output.
public static final int SAMPLECOUNT		=512;
public static final int NUM_CHANNELS	=	8;
// It is 2 for 16bit, and 2 for two channels.
public static final int BUFMUL            =      4;
public static final int MIXBUFFERSIZE		=(SAMPLECOUNT*BUFMUL);

public static final int SAMPLERATE	=	11025;	// Hz
public static final int SAMPLESIZE	=	2 ;  	// 16bit
public static final int NUMSFX	=	sfxenum_t.NUMSFX.ordinal() ; 

// The actual lengths of all sound effects.
int[] 		lengths=new int[NUMSFX];

// The actual output device.
int	audio_fd;

// The global mixing buffer.
// Basically, samples from all active internal channels
//  are modifed and added, and stored in the buffer
//  that is submitted to the audio device.
short[]	mixbuffer=new short[MIXBUFFERSIZE];

// The channel step amount...
int[]	channelstep= new int[NUM_CHANNELS];
// ... and a 0.16 bit remainder of last step.
int[]	channelstepremainder= new int[NUM_CHANNELS];


// The channel data pointers, start and end.
// MAES: they were arrays of char pointers...so signed bytes?
byte[][]	channels= new byte[NUM_CHANNELS][];
// MAES: we'll have to use this for actual pointing. channels[] holds just the data.
int[] channels_ptr=new int[NUM_CHANNELS];
// The second one is supposed to point at "the end", so I'll make it an int.
int[]	channelsend= new int[NUM_CHANNELS];


// Time/gametic that the channel started playing,
//  used to determine oldest, which automatically
//  has lowest priority.
// In case number of active sounds exceeds
//  available channels.
int[]		channelstart=new int[NUM_CHANNELS];

// The sound in channel handles,
//  determined on registration,
//  might be used to unregister/stop/modify,
//  currently unused.
int[] 		channelhandles=new int[NUM_CHANNELS];

// SFX id of the playing sound effect.
// Used to catch duplicates (like chainsaw).
int[]	channelids=new int[NUM_CHANNELS];			

// Pitch to stepping lookup, unused.
int[]		steptable=new int[256];

// Volume lookups.
int[]		vol_lookup=new int[128*256];

// Hardware left and right channel volume lookup.
int[][]		channelleftvol_lookup=new int[NUM_CHANNELS][];
int[][]		channelrightvol_lookup=new int[NUM_CHANNELS][];




//
// Safe ioctl, convenience.
//

/*
void
myioctl
( int	fd,
  int	command,
  int*	arg )
{   
    int		rc;
    extern int	errno;
    
    rc = ioctl(fd, command, arg);  
    if (rc < 0)
    {
	fprintf(stderr, "ioctl(dsp,%d,arg) failed\n", command);
	fprintf(stderr, "errno=%d\n", errno);
	exit(-1);
    }
}
*/




//
// This function loads the sound data from the WAD lump,
//  for single sound.
//
public byte[]
getsfx
( String         sfxname,
  int[]          len )
{
    byte[]     sfx;
    byte[]      paddedsfx;
    int                 i;
    int                 size;
    int                 paddedsize;
    String name="ds"+sfxname;
    int                 sfxlump;

    
    // Get the sound data from the WAD, allocate lump
    //  in zone memory.
    //System.out.println(name + "ds" + sfxname);

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
    if ( W.CheckNumForName(name) == -1 )
      sfxlump = W.GetNumForName("dspistol");
    else
      sfxlump = W.GetNumForName(name);
    
    size = W.LumpLength( sfxlump );

    // Debug.
    // fprintf( stderr, "." );
    //fprintf( stderr, " -loading  %s (lump %d, %d bytes)\n",
    //	     sfxname, sfxlump, size );
    //fflush( stderr );
    
    sfx = W.CacheLumpNum( sfxlump, PU_STATIC ).array();

    // Pads the sound effect out to the mixing buffer size.
    // The original realloc would interfere with zone memory.
    paddedsize = ((size-8 + (SAMPLECOUNT-1)) / SAMPLECOUNT) * SAMPLECOUNT;

    // Allocate from zone memory.
    paddedsfx = new byte[paddedsize+8];
    // ddt: (unsigned char *) realloc(sfx, paddedsize+8);
    // This should interfere with zone memory handling,
    //  which does not kick in in the soundserver.

    // Now copy and pad.
    System.arraycopy(sfx,0,paddedsfx, 0, size );
    for (i=size ; i<paddedsize+8 ; i++)
        paddedsfx[i] = (byte) 128;

    // Remove the cached lump.
    sfx=null;
    
    // Preserve padded length.
    len[0] = paddedsize;

    // Return allocated padded data.
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
    
    int		oldest = ds.gametic;
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
	for (i=0 ; i<NUM_CHANNELS ; i++)
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
    for (i=0; (i<NUM_CHANNELS) && (channels[i]!=null); i++)
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
    if (i == NUM_CHANNELS)
	slot = oldestnum;
    else
	slot = i;

    // Okay, in the less recent channel,
    //  we will handle the new SFX.
    // Set pointer to raw data.
    channels[slot] = S_sfx[sfxid].data;
    // Set pointer to end of raw data.
    channelsend[slot] = /*channels[slot] + */lengths[sfxid];

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
    channelstart[slot] = ds.gametic;

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
	system.Error("rightvol out of bounds");
    
    if (leftvol < 0 || leftvol > 127)
	system.Error("leftvol out of bounds");
    
    // Get the proper lookup table piece
    //  for this volume level???
    // TODO: more pointer magic...
    //channelleftvol_lookup[slot] = vol_lookup[leftvol*256];
    //channelrightvol_lookup[slot] = vol_lookup[rightvol*256];

    // Preserve sound SFX id,
    //  e.g. for avoiding duplicates of chainsaw.
    channelids[slot] = sfxid;

    // You tell me.
    return rc;
}





//
// SFX API
// Note: this was called by S_Init.
// However, whatever they did in the
// old DPMS based DOS version, this
// were simply dummies in the Linux
// version.
// See soundserver initdata().
//
public void I_SetChannels()
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
  for (i=0 ; i<128 ; i++)
    for (j=0 ; j<256 ; j++)
      vol_lookup[i*256+j] = (i*(j-128)*256)/127;
}	

 
void I_SetSfxVolume(int volume)
{
  // Identical to DOS.
  // Basically, this should propagate
  //  the menu/config file setting
  //  to the state variable used in
  //  the mixing.
  ds.snd_SfxVolume = volume;
}

// MUSIC API - dummy. Some code from DOS version.
public void I_SetMusicVolume(int volume)
{
  // Internal state variable.
  ds.snd_MusicVolume = volume;
  // Now set volume on output device.
  // Whatever( snd_MusciVolume );
}


public //
// Retrieve the raw data lump index
//  for a given SFX name.
//
int I_GetSfxLumpNum(sfxinfo_t sfx)
{
    String namebuf="ds"+sfx.name;
    return W.GetNumForName(namebuf);
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
I_StartSound
( int		id,
  int		vol,
  int		sep,
  int		pitch,
  int		priority )
{

  // UNUSED
  priority = 0;
  /*
#ifdef SNDSERV 
    if (sndserver)
    {
	fprintf(sndserver, "p%2.2x%2.2x%2.2x%2.2x\n", id, pitch, vol, sep);
	fflush(sndserver);
    }
    // warning: control reaches end of non-void function.
    return id;
#else */
    // Debug.
    //fprintf( stderr, "starting sound %d", id );
    
    // Returns a handle (not used).
    id = addsfx( id, vol, steptable[pitch], sep );

    // fprintf( stderr, "/handle is %d\n", id );
    
    return id;
}



public void I_StopSound (int handle)
{
  // You need the handle returned by StartSound.
  // Would be looping all channels,
  //  tracking down the handle,
  //  an setting the channel to zero.
  
  // UNUSED.
  handle = 0;
}


public boolean I_SoundIsPlaying(int handle)
{
    // Ouch.
    return (ds.gametic < handle);
}




//
// This function loops all active (internal) sound
//  channels, retrieves a given number of samples
//  from the raw sound data, modifies it according
//  to the current (internal) channel parameters,
//  mixes the per channel samples into the global
//  mixbuffer, clamping it to the allowed range,
//  and sets up everything for transferring the
//  contents of the mixbuffer to the (two)
//  hardware channels (left and right, that is).
//
// This function currently supports only 16bit.
//
public void I_UpdateSound()
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
    
    // Left and right channel
    //  are in global mixbuffer, alternating.
    //leftout = mixbuffer;
    //rightout = mixbuffer+1;
	leftout = 0;
	rightout = 1;
	step = 2;

    // Determine end, for left channel only
    //  (right channel is implicit).
    leftend =/* mixbuffer +*/ SAMPLECOUNT*step;

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
	for ( chan = 0; chan < NUM_CHANNELS; chan++ )
	{
	    // Check channel, if active.
	    if (channels[ chan ]!=null)
	    {
		// Get the raw data from the channel.
	    // Maes: this is supposed to be an 8-bit value.
		sample = channels[ chan ][channels_ptr[chan]];
		// Add left and right part
		//  for this channel (sound)
		//  to the current data.
		// Adjust volume accordingly.
		dl += channelleftvol_lookup[ chan ][sample];
		dr += channelrightvol_lookup[ chan ][sample];
		// Increment index ???
		channelstepremainder[ chan ] += channelstep[ chan ];
		// MSB is next sample???
		channels[ chan ][channels_ptr[chan]] += channelstepremainder[ chan ] >> 16;
		// Limit to LSB???
		channelstepremainder[ chan ] &= 65536-1;

		// Check whether we are done.
		if (channels_ptr[ chan ] >= channelsend[ chan ])
			channels_ptr[ chan ] = 0;
	    }
	}
	
	// Clamp to range. Left hardware channel.
	// Has been char instead of short.
	// if (dl > 127) *leftout = 127;
	// else if (dl < -128) *leftout = -128;
	// else *leftout = dl;

	if (dl > 0x7fff)
	    leftout = 0x7fff;
	else if (dl < -0x8000)
	    leftout = -0x8000;
	else
	    leftout = dl;

	// Same for right hardware channel.
	if (dr > 0x7fff)
	    rightout = 0x7fff;
	else if (dr < -0x8000)
	    rightout = -0x8000;
	else
	    rightout = dr;

	// Increment current pointers in mixbuffer.
	leftout += step;
	rightout += step;
    }
/*
#ifdef SNDINTR
    // Debug check.
    if ( flag )
    {
      misses += flag;
      flag = 0;
    }
    
    if ( misses > 10 )
    {
      fprintf( stderr, "I_SoundUpdate: missed 10 buffer writes\n");
      misses = 0;
    }
    
    // Increment flag for update.
    flag++;
#endif
*/
}


// 
// This would be used to write out the mixbuffer
//  during each game loop update.
// Updates sound buffer and audio device at runtime. 
// It is called during Timer interrupt with SNDINTR.
// Mixing now done synchronous, and
//  only output be done asynchronous?
//
public void
I_SubmitSound()
{
  // Write it to DSP device.
  //write(audio_fd, mixbuffer, SAMPLECOUNT*BUFMUL);
}



public void
I_UpdateSoundParams
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




public void I_ShutdownSound()
{    
	/*
#ifdef SNDSERV
  if (sndserver)
  {
    // Send a "quit" command.
    fprintf(sndserver, "q\n");
    fflush(sndserver);
  }
#else
*/
  // Wait till all pending sounds are finished.
  int done = 0;
  int i;
  

  // FIXME (below).
  //fprintf( stderr, "I_ShutdownSound: NOT finishing pending sounds\n");
  //fflush( stderr );
  
  while ( done==0 )
  {
    for( i=0 ; i<8 && !(channels[i][channels_ptr[i]]==0) ; i++);
    
    // FIXME. No proper channel output.
    //if (i==8)
    done=1;
  }
//#ifdef SNDINTR
//  I_SoundDelTimer();
//#endif
  
  // Cleaning up -releasing the DSP device.
 // close ( audio_fd );
//#endif

  // Done.
  return;
}






void
I_InitSound()
{ 
/*#ifdef SNDSERV
  char buffer[256];
  
  if (getenv("DOOMWADDIR"))
    sprintf(buffer, "%s/%s",
	    getenv("DOOMWADDIR"),
	    sndserver_filename);
  else
    sprintf(buffer, "%s", sndserver_filename);
  
  // start sound process
  if ( !access(buffer, X_OK) )
  {
    strcat(buffer, " -quiet");
    sndserver = popen(buffer, "w");
  }
  else
    fprintf(stderr, "Could not start sound server [%s]\n", buffer);
#else
    */
  int i;
  
  /*
#ifdef SNDINTR
  fprintf( stderr, "I_SoundSetTimer: %d microsecs\n", SOUND_INTERVAL );
  I_SoundSetTimer( SOUND_INTERVAL );
#endif
 */
    
  // Secure and configure sound device first.
 // fprintf( stderr, "I_InitSound: ");
  
  audio_fd = open("/dev/dsp", O_WRONLY);
  if (audio_fd<0)
    fprintf(stderr, "Could not open /dev/dsp\n");
  
                     
  i = 11 | (2<<16);                                           
  myioctl(audio_fd, SNDCTL_DSP_SETFRAGMENT, &i);
  myioctl(audio_fd, SNDCTL_DSP_RESET, 0);
  
  i=SAMPLERATE;
  
  myioctl(audio_fd, SNDCTL_DSP_SPEED, &i);
  
  i=1;
  myioctl(audio_fd, SNDCTL_DSP_STEREO, &i);
  
  myioctl(audio_fd, SNDCTL_DSP_GETFMTS, &i);
  
  if (i&=AFMT_S16_LE)    
    myioctl(audio_fd, SNDCTL_DSP_SETFMT, &i);
  else
    fprintf(stderr, "Could not play signed 16 data\n");

  fprintf(stderr, " configured audio device\n" );

    
  // Initialize external data (all sounds) at start, keep static.
  fprintf( stderr, "I_InitSound: ");
  
  for (i=1 ; i<NUMSFX ; i++)
  { 
    // Alias? Example is the chaingun sound linked to pistol.
    if (!S_sfx[i].link)
    {
      // Load data from WAD file.
      S_sfx[i].data = getsfx( S_sfx[i].name, &lengths[i] );
    }	
    else
    {
      // Previously loaded already?
      S_sfx[i].data = S_sfx[i].link->data;
      lengths[i] = lengths[(S_sfx[i].link - S_sfx)/sizeof(sfxinfo_t)];
    }
  }

  System.err.print(" pre-cached all sound data\n");
  
  // Now initialize mixbuffer with zero.
  for ( i = 0; i< MIXBUFFERSIZE; i++ )
    mixbuffer[i] = 0;
  
  // Finished initialization.
  System.err.print("I_InitSound: sound module ready\n");
    
#endif
}




//
// MUSIC API.
// Still no music done.
// Remains. Dummies.
//

static int	looping=0;
static int	musicdies=-1;

public void I_PlaySong(int handle, int looping)
{
  // UNUSED.
  handle = looping = 0;
  musicdies = ds.gametic + TICRATE*30;
}

public void I_PauseSong (int handle)
{
  // UNUSED.
  handle = 0;
}

public void I_ResumeSong (int handle)
{
  // UNUSED.
  handle = 0;
}

public void I_StopSong(int handle)
{
  // UNUSED.
  handle = 0;
  
  looping = 0;
  musicdies = 0;
}

public void I_UnRegisterSong(int handle)
{
  // UNUSED.
  handle = 0;
}

public int I_RegisterSong(byte[] data)
{
  // UNUSED.
  data = null;
  
  return 1;
}

// Is the song playing?
int I_QrySongPlaying(int handle)
{
  // UNUSED.
  handle = 0;
  return looping || musicdies > ds.gametic;
}



//
// Experimental stuff.
// A Linux timer interrupt, for asynchronous
//  sound output.
// I ripped this out of the Timer class in
//  our Difference Engine, including a few
//  SUN remains...
//  

/*
#ifdef sun
    typedef     sigset_t        tSigSet;
#else    
    typedef     int             tSigSet;
#endif


// We might use SIGVTALRM and ITIMER_VIRTUAL, if the process
//  time independend timer happens to get lost due to heavy load.
// SIGALRM and ITIMER_REAL doesn't really work well.
// There are issues with profiling as well.
static int /*__itimer_which*/  itimer = ITIMER_REAL;

static int sig = SIGALRM;

// Interrupt handler.
void I_HandleSoundTimer( int ignore )
{
  // Debug.
  //fprintf( stderr, "%c", '+' ); fflush( stderr );
  
  // Feed sound device if necesary.
  if ( flag )
  {
    // See I_SubmitSound().
    // Write it to DSP device.
    write(audio_fd, mixbuffer, SAMPLECOUNT*BUFMUL);

    // Reset flag counter.
    flag = 0;
  }
  else
    return;
  
  // UNUSED, but required.
  ignore = 0;
  return;
}
*/

/*
// Get the interrupt. Set duration in millisecs.
int I_SoundSetTimer( int duration_of_tick )
{
  // Needed for gametick clockwork.
  struct itimerval    value;
  struct itimerval    ovalue;
  struct sigaction    act;
  struct sigaction    oact;

  int res;
  
  // This sets to SA_ONESHOT and SA_NOMASK, thus we can not use it.
  //     signal( _sig, handle_SIG_TICK );
  
  // Now we have to change this attribute for repeated calls.
  act.sa_handler = I_HandleSoundTimer;

  act.sa_flags = SA_RESTART;
  
  sigaction( sig, &act, &oact );

  value.it_interval.tv_sec    = 0;
  value.it_interval.tv_usec   = duration_of_tick;
  value.it_value.tv_sec       = 0;
  value.it_value.tv_usec      = duration_of_tick;

  // Error is -1.
  res = setitimer( itimer, &value, &ovalue );

  // Debug.
  if ( res == -1 )
	  System.err.print("I_SoundSetTimer: interrupt n.a.\n");
  
  return res;
}

*/

// Remove the interrupt. Set duration to zero.
void I_SoundDelTimer()
{
  // Debug.
  if ( I_SoundSetTimer( 0 ) == -1)
    System.err.print( "I_SoundDelTimer: failed to remove interrupt. Doh!\n");
}

}