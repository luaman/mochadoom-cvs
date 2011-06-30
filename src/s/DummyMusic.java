package s;

public class DummyMusic implements IMusic {

	@Override
	public boolean InitMusic() {		
		// Unsurprisingly, we can guarantee NOT having music
		return true;

	}

	@Override
	public void ShutdownMusic() {
		// TODO Auto-generated method stub

	}

	@Override
	public void SetMusicVolume(int volume) {
		// TODO Auto-generated method stub

	}

	@Override
	public void PauseSong(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ResumeSong(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public int RegisterSong(byte[] data) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void PlaySong(int handle, boolean looping) {
		// TODO Auto-generated method stub

	}

	@Override
	public void StopSong(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void UnRegisterSong(int handle) {
		// TODO Auto-generated method stub

	}

}
