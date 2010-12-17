package rr;

import java.io.IOException;

/** All texture, flat and sprite management operations should be handled
 *  by an implementing class. As of now, the renderer does both, though it's
 *  not really the most ideal.
 *  
 * @author Velktron
 *
 */

public interface TextureManager {

	int TextureNumForName(String texname);
	
	int FlatNumForName(String flatname);
	
	void PrecacheLevel() throws IOException;
	
	int getTextureheight(int texnum);	
	
	void InitSprites(String[] sprnames);
	
	int getTextureTranslation(int texnum);
	
	int getFlatTranslation(int flatnum);
	
	void setTextureTranslation(int texnum, int amount);
	
	void setFlatTranslation(int flatnum,int amount);

	int CheckTextureNumForName(String texnamem);

    void InitTextureMapping();
		
}
