package rr;

import static data.Tables.FINEANGLES;
import static m.fixed_t.FRACUNIT;
import i.DoomStatusAware;
import v.IVideoScaleAware;
import doom.player_t;

public interface Renderer<T,V> extends IVideoScaleAware,DoomStatusAware{
	


	/** Fineangles in the SCREENWIDTH wide window. */
	public static final int FIELDOFVIEW = FINEANGLES / 4;
	
	public static final int MINZ = (FRACUNIT * 4);
	public static final int BASEYCENTER = 100;
	
	public static final int FUZZTABLE = 50;
	
	/**
	 * killough: viewangleoffset is a legacy from the pre-v1.2 days, when Doom
	 * had Left/Mid/Right viewing. +/-ANG90 offsets were placed here on each
	 * node, by d_net.c, to set up a L/M/R session.
	 */

	public static final long viewangleoffset = 0;
	
	
	public void Init();
	
	public void RenderPlayerView(player_t player);
	
	public void ExecuteSetViewSize();
	
	public void FillBackScreen();
	
	public void DrawViewBorder();
	
	public void SetViewSize(int size, int detaillevel);
	
	public void VideoErase(int offset, int width);
	
	public long PointToAngle2(int x1, int y1, int x2, int y2);
	
	public void PreCacheThinkers();
	
	public int getValidCount();
	
	public void increaseValidCount(int amount);

	public boolean isFullHeight();

	public void resetLimits();

	public boolean getSetSizeNeeded();

	public int getViewWindowX();

	public int getViewWindowY();
	
	public int getScaledViewWidth();

	public int getScaledViewHeight();
	
	public boolean isFullScreen();

	public TextureManager getTextureManager();
    
	
	//public subsector_t PointInSubsector(int x, int y);

}
