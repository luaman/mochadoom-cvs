package rr;

import java.io.IOException;

/** All texture, flat and sprite management operations should be handled
 *  by an implementing class. As of now, the renderer does both, though it's
 *  not really the most ideal.
 *  
 * @author Kaptain Zyklon
 *
 */

public interface TextureManager {

	public int TextureNumForName(String texname);
	
	public int FlatNumForName(String flatname);
	
	public void PrecacheLevel() throws IOException;
	
	public int getTextureheight(int texnum);	
	
	public void InitSprites(String[] sprnames);
	
	public int getTextureTranslation(int texnum);
	
	public int getFlatTranslation(int flatnum);
	
	public void setTextureTranslation(int texnum, int amount);
	
	public void setFlatTranslation(int flatnum,int amount);

	public int CheckTextureNumForName(String texnamem);

    public void InitTextureMapping();
		
}
