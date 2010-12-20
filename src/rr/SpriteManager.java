package rr;

/** In case sprite functionality is ever cleanly split from the "jack of all trades" renderer.
 * 
 * @author admin
 *
 */

public interface SpriteManager {
    public void InitSprites(String[] sprnames);
    public spritedef_t[] getSprites();
    public int getFirstSpriteLump();


}
