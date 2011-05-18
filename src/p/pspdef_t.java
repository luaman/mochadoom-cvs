package p;

import java.io.IOException;

import w.DoomFile;
import w.IReadableDoomObject;
import w.statenum_t;
import data.state_t;

public class pspdef_t implements IReadableDoomObject{

    public pspdef_t(){
        state=new state_t();
    }

    /** a NULL state means not active */
    public state_t	state;	
    public int		tics;
    /** fixed_t */
    public int	sx, sy;
    // When read from disk.
    public int readstate;
    
    @Override
    public void read(DoomFile f) throws IOException {
        // TODO: where/when is state set?
        //state=data.info.states[f.readLEInt()];
        readstate=f.readLEInt();
        tics=f.readLEInt();
        sx=f.readLEInt();
        sy=f.readLEInt();
    }

}
