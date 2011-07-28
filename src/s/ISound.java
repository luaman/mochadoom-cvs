package s;

import data.sfxinfo_t;
import data.sounds.sfxenum_t;
import static data.Defines.TICRATE;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: ISound.java,v 1.2.2.3 2011/07/28 08:18:47 velktron Exp $
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
//
// DESCRIPTION:
// System interface, sound. Anything implementation-specific should
// implement this.
//
//-----------------------------------------------------------------------------

public interface ISound {

	
	public static final int VOLUME_STEPS = 128;
	public static final int PANNING_STEPS = 256;
	public static final int IDLE_HANDLE = -1;
	public static final int BUSY_HANDLE = -2;
	// Needed for calling the actual sound output
	// We mix 1024 samples each time, but we only call UpdateSound()
	// 1 time out of three.
	
	public static final int NUM_CHANNELS	=	8;
	// It is 2 for 16bit, and 2 for two channels.
	public static final int BUFMUL            =      4;

	public static final int SAMPLERATE	=	11025;	// Hz
	// Was 512, but if you mix that many samples per tic you will
	// eventually outrun the buffer :-/ I fail to see the mathematical
	// justification behind this, unless they simply wanted the buffer to 
	// be a nice round number in size.
	public static final int SAMPLECOUNT		=1+SAMPLERATE/TICRATE;
	public static final int MIXBUFFERSIZE		=(SAMPLECOUNT*BUFMUL);
	public static final int SAMPLESIZE	=	16 ;  	// 16bit
	public static final int NUMSFX	=	sfxenum_t.NUMSFX.ordinal() ; 
	public static final int MAXHANDLES = 100;
	// 1 chunk buffers 1 tic of events.
	public static final int BUFFER_CHUNKS=3;
	
	// Init at program start...
	void InitSound();

	// ... update sound buffer and audio device at runtime...
	void UpdateSound();
	void SubmitSound();

	// ... shut down and relase at program termination.
	void ShutdownSound();


	//
	//  SFX I/O
	//

	// Initialize channels?
	void SetChannels(int numChannels);

	// Get raw data lump index for sound descriptor.
	int GetSfxLumpNum (sfxinfo_t sfxinfo );


	// Starts a sound in a particular sound channel.
	int StartSound
	( int		id,
	  int		vol,
	  int		sep,
	  int		pitch,
	  int		priority );


	// Stops a sound channel.
	void StopSound(int handle);

	/** Called by S_*() functions to see if a channel is still playing.
	    Returns false if no longer playing, true if playing. This is 
	    a relatively "high level" function, so its accuracy relies on
	    what the "system specific" sound code reports back */
	boolean SoundIsPlaying(int handle);

	/* Updates the volume, separation,
	   and pitch of a sound channel. */
	void UpdateSoundParams
	( int		handle,
	  int		vol,
	  int		sep,
	  int		pitch );
	
}

//-----------------------------------------------------------------------------
//
// $Log: ISound.java,v $
// Revision 1.2.2.3  2011/07/28 08:18:47  velktron
// Merged in v1.5 release cleanups and some last-minute fixes.
//
// Revision 1.8  2011/07/08 17:03:57  velktron
// Set SAMPLECOUNT to SAMPLERATE/TICRATE+1
//
// Revision 1.7  2011/07/08 13:05:40  velktron
// BUFFER_CHUNKS now in ISound.
//
// Revision 1.6  2011/07/06 16:03:56  velktron
// Version used for testing.
//
// Revision 1.5  2011/07/05 18:38:51  velktron
// BUSY_HANDLE part of interface.
//
// Revision 1.4  2011/07/05 13:45:33  velktron
// BUSY_HANDLE
//
// Revision 1.3  2011/07/03 20:10:42  velktron
// Proper dummy implementation, interface javadocs.
//
// Revision 1.2  2011/06/14 20:58:10  velktron
// Modified so channel settings are now deferred (after start)
//
// Revision 1.1  2011/06/12 20:55:12  velktron
// Separated SFX playback into a stand-alone concern. This is roughly equivalent to i_sound.h & i_sound.c, for what regards the sfx part.
//
//
//-----------------------------------------------------------------------------
