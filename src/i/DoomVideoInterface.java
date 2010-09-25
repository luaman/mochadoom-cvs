package i;

/** Methods specific to Doom-System video interfacing */

public interface DoomVideoInterface {

	void StartFrame();

	void StartTic();

	void SetPalette(int palette);

	void InitGraphics();

	void FinishUpdate();

}
