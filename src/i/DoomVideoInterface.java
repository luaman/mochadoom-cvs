package i;

/** Methods specific to Doom-System video interfacing */

public interface DoomVideoInterface {

	public void StartFrame();

	public void StartTic();

	public void SetPalette(int palette);

	public void InitGraphics();

	public void FinishUpdate();

	public void UpdateNoBlit();

	public void ShutdownGraphics();

	public void setGamma(int usegamma);
	
	public void ReadScreen(byte[] linear);

}
