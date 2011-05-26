package savegame;

import java.io.IOException;

import i.DoomStatusAware;
import p.ThinkerList;
import w.DoomFile;

public interface IDoomSaveGame extends DoomStatusAware{    
    void setThinkerList(ThinkerList li);
    boolean doLoad(DoomFile f);
    IDoomSaveGameHeader getHeader();
    void setHeader(IDoomSaveGameHeader header);
    boolean doSave(DoomFile f);
}
