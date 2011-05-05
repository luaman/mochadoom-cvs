package p;

import java.io.IOException;

import w.DoomFile;
import w.ReadableDoomObject;
import data.state_t;

public class pspdef_t implements ReadableDoomObject{

    public pspdef_t(){
        state=new state_t();
    }

    /** a NULL state means not active */
    public state_t	state;	
    public int		tics;
    /** fixed_t */
    public int	sx, sy;

    @Override
    public void read(DoomFile f) throws IOException {
        state.read(f);
        tics=f.readInt();
        sx=f.readInt();
        sy=f.readInt();
    }

}
