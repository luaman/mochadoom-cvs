package s;

import p.mobj_t;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;

/** Does nothing. Just allows me to code without
 *  commenting out ALL sound-related code. Hopefully
 *  it will be superseded by a real sound driver one day.
 * 
 * @author Velktron
 *
 */

public class DummySoundDriver implements DoomSoundInterface{

	@Override
	public void ChangeMusic(int music_id, boolean looping) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Init(int sfxVolume, int musicVolume) {
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
	public void Start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void StartMusic(int music_id) {
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
	public void StartSoundAtVolume(mobj_t origin, int sound_id, int volume) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void StopMusic() {
		// TODO Auto-generated method stub
		
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
	public void StartMusic(musicenum_t musicenum_t) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void ChangeMusic(musicenum_t musicid, boolean looping) {
        // TODO Auto-generated method stub
        
    }

}
