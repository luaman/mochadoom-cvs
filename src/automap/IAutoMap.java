package automap;

import i.DoomStatusAware;
import v.IVideoScaleAware;
import doom.event_t;

public interface IAutoMap extends IVideoScaleAware, DoomStatusAware{
 // Used by ST StatusBar stuff.
    public final int AM_MSGHEADER =(('a'<<24)+('m'<<16));
    public final int AM_MSGENTERED= (AM_MSGHEADER | ('e'<<8));
    public final int AM_MSGEXITED= (AM_MSGHEADER | ('x'<<8));


    // Called by main loop.
    public boolean Responder (event_t ev);

    // Called by main loop.
    public void Ticker ();

    // Called by main loop,
    // called instead of view drawer if automap active.
    public void  Drawer ();

    // Called to force the automap to quit
    // if the level is completed while it is up.
    public void  Stop ();

    public void Start();

    // Should be called in order to set a proper scaled buffer.
    public void Init();
    
}
