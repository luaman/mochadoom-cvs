package s;

import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import p.mobj_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: IDoomSound.java,v 1.1 2011/05/18 16:57:08 velktron Exp $
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
// DESCRIPTION:
//	The not so system specific sound interface.
//
//-----------------------------------------------------------------------------

public interface IDoomSound {

	/**
	 * Initializes sound stuff, including volume Sets channels, SFX and music
	 * volume, allocates channel buffer, sets S_sfx lookup.
	 */

	void Init(int sfxVolume, int musicVolume);

	/**
	 * Per level startup code. Kills playing sounds at start of level,
	 * determines music if any, changes music.
	 */
	public void Start();

	/**
	 * Start sound for thing at <origin> using <sound_id> from sounds.h
	 */
	public void StartSound(mobj_t origin, int sound_id);

	/**
	 * Start sound for thing at <origin> using <sound_id> from sounds.h
	 */
	public void StartSound(mobj_t origin, sfxenum_t sound_id);
	
	/** Will start a sound at a given volume. */
	public void StartSoundAtVolume(mobj_t origin, int sound_id, int volume);

	/** Stop sound for thing at <origin> */
	public void StopSound(mobj_t origin);

	/** Start music using <music_id> from sounds.h */
	public void StartMusic(musicenum_t musicid);

	/**
	 * Start music using <music_id> from sounds.h, and set whether looping
	 * 
	 * @param musicid
	 * @param looping
	 */
	public void ChangeMusic(musicenum_t musicid, boolean looping);

	   /**
     * Start music using <music_id> from sounds.h, and set whether looping
     * 
     * @param musicid
     * @param looping
     */
    public void ChangeMusic(int musicid, boolean looping);
	
	/** Stops the music fer sure. */
	public void StopMusic();

	/** Stop and resume music, during game PAUSE. */
	public void PauseSound();

	public void ResumeSound();

	/**
	 * Updates music & sounds
	 * 
	 * @param listener
	 */
	public void UpdateSounds(mobj_t listener);

	public void SetMusicVolume(int volume);

	public void SetSfxVolume(int volume);

	public void StartMusic(int music_id);
}
