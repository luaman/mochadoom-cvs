package rr;

import doom.player_t;

public interface Renderer {
	
	public void Init();
	
	public void RenderPlayerView(player_t player);
	
	public void ExecuteSetViewSize();
	
	public void FillBackScreen();
	
	public void DrawViewBorder();
	
	public void SetViewSize(int size, int detaillevel);
	
	public void VideoErase(int offset, int width);
	
	public long PointToAngle2(int x1, int y1, int x2, int y2);
	
	public subsector_t PointInSubsector(int x, int y);

}
