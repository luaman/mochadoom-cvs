package savegame;

import p.ThinkerList;

public interface IDoomSaveGame {    
    void setThinkerList(ThinkerList li);
    void doSave();
    void doLoad();
    IDoomSaveGameHeader getHeader();
    void setHeader(IDoomSaveGameHeader header);
}
