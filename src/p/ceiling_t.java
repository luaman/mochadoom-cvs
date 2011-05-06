package p;

import java.io.IOException;
import rr.SectorAction;
import w.DoomFile;
import w.IReadableDoomObject;

public class ceiling_t extends SectorAction implements IReadableDoomObject{

        public ceiling_e   type;
        /** fixed_t */
        public int bottomheight;
        /** fixed_t */
        public int topheight;
        /** fixed_t */
        public int speed;
        boolean crush;

        // 1 = up, 0 = waiting, -1 = down
        public int     direction;

        // ID
        public int     tag;                   
        public int     olddirection;
        
        // HACK for speed.
        public static final ceiling_e[] values=ceiling_e.values();
        
        @Override
        public void read(DoomFile f) throws IOException{
            // TODO: probably some sort of sanity check built into the type
            // itself would be nice.
            type=values[f.readInt()];
            bottomheight=f.readInt();
            topheight=f.readInt();
            speed=f.readInt();
            crush=f.readBoolean();
            direction=f.readInt();
            tag=f.readInt();
            olddirection=f.readInt();            
            }

        }
