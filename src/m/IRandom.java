package m;

import doom.think_t;

public interface IRandom {
	public int P_Random ();
	public int M_Random ();
	public void ClearRandom ();
	public int getIndex();
	public int P_Random(int caller);
	public int P_Random(think_t caller, int sequence);
}
