package w;

import java.io.IOException;

public interface IReadWriteDoomObject {
    public void read(DoomFile f) throws IOException ;
    public void write(DoomFile f) throws IOException ;
}
