package p;

import static utils.C2JUtils.flags;

import java.io.IOException;
import java.nio.ByteBuffer;

import rr.SectorAction;
import rr.sector_t;
import s.IDoomSound;
import w.DoomFile;
import w.IReadableDoomObject;
import data.sounds.sfxenum_t;
import doom.DoomStatus;
import doom.thinker_t;

public class floormove_t extends SectorAction implements IReadableDoomObject{

    private ThinkerList TL;
    private IDoomSound S;
    private DoomStatus DM;
    
    public floormove_t(){
        // MAES HACK: floors are implied to be at least of "lowerFloor" type
        // unless set otherwise, due to implicit zero-enum value.
        this.type=floor_e.lowerFloor;
    }
    
    public floormove_t(ThinkerList TL, IDoomSound S){
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

    @Override
    public void read(DoomFile f) throws IOException{

        super.read(f); // Call thinker reader first            
        type=floor_e.values()[f.readLEInt()];
        crush=f.readIntBoolean();
        super.sectorid=f.readLEInt(); // Sector index (or pointer?)
        direction=f.readLEInt();
        newspecial=f.readLEInt();
        texture=f.readLEShort();
        floordestheight=f.readLEInt();
        speed=f.readLEInt();        
        }
   
    @Override
    public void pack(ByteBuffer b) throws IOException{
        super.pack(b); //12            
        b.putInt(type.ordinal()); // 16
        b.putInt(crush?1:0); //20
        b.putInt(super.sectorid); // 24
        b.putInt(direction); // 28
        b.putInt(newspecial); // 32
        b.putShort(texture); // 34
        b.putInt(floordestheight); // 38
        b.putInt(speed); // 42
    }
    
}