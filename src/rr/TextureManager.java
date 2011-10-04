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
	
	
	/**The "num" expected here is the internal flat number,
	 * not the absolute lump number. So impement accordingly.
	 * 
	 * @param flatname
	 * @return
	 */
	int FlatNumForName(String flatname);
	
	void PrecacheLevel() throws IOException;
	
	void GenerateComposite(int tex);
	
	int getTextureheight(int texnum);	
		
	int getTextureTranslation(int texnum);
	
	int getFlatTranslation(int flatnum);
	
	void setTextureTranslation(int texnum, int amount);
	
	void setFlatTranslation(int flatnum,int amount);

	int CheckTextureNumForName(String texnamem);

	String CheckTextureNameForNum(int texnum);
	
    int getTexturewidthmask(int tex);
   
    int getTextureColumnLump(int tex, int col);
   
    char getTextureColumnOfs(int tex, int col);

    byte[][] getTextureComposite(int tex);

    byte[] getTextureComposite(int tex, int col);

    void InitFlats();

    void InitTextures() throws IOException;

    //int getFirstFlat();

    int getSkyTextureMid();

    int getSkyFlatNum();

    int getSkyTexture();

    void setSkyTexture(int skytexture);

    int InitSkyMap();

    void setSkyFlatNum(int skyflatnum);

    void GenerateLookup(int texnum)
            throws IOException;
    
    int getFlatLumpNum(int flatnum);


	byte[] getRogueColumn(int lump, int column);

    patch_t getMaskedComposite(int tex);


    void GenerateMaskedComposite(int texnum);
    
    }
