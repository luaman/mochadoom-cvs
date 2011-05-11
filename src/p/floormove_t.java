package p;

import static utils.C2JUtils.flags;
import rr.SectorAction;
import rr.sector_t;
import s.DoomSoundInterface;
import data.sounds.sfxenum_t;
import doom.DoomStatus;
import doom.thinker_t;

public class floormove_t extends SectorAction{

    private ThinkerList TL;
    private DoomSoundInterface S;
    private DoomStatus DM;
    
    public floormove_t(){
        // MAES HACK: floors are implied to be at least of "lowerFloor" type
        // unless set otherwise, due to implicit zero-enum value.
        this.type=floor_e.lowerFloor;
    }
    
    public floormove_t(ThinkerList TL, DoomSoundInterface S){
        this.TL=TL;
        this.S=S;
    }
    
    public floor_e type;
    public boolean crush;

    public int     direction;
    public int     newspecial;
    public short   texture;
    /** fixed_t */
    public int floordestheight;
    /** fixed_t */
    public int speed;

   
    
}