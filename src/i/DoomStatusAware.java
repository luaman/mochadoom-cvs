package i;

import doom.DoomStatus;

public interface DoomStatusAware<T,V> {
	  public void updateStatus(DoomStatus<T,V> DC);
}
