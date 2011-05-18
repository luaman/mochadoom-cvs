package i;

import doom.ticcmd_t;

public interface IDoomSystem {

	public int GetTime();

	public void AllocLow(int length);

	public void BeginRead();

	public void EndRead();

	public void WaitVBL(int count);

	public byte[] ZoneBase(int size);

	public int GetHeapSize();

	public void Tactile(int on, int off, int total);

	public void Quit();

	public ticcmd_t BaseTiccmd();

	public void Error(String error, Object ... args);

	void Error(String error);
	
	void Init();


}
