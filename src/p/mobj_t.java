package p;

import rr.subsector_t;
import m.fixed_t;
import data.mobjinfo_t;
import data.state_t;
import data.mapthing_t;
import data.info.mobjtype_t;
import data.info.spritenum_t;
import doom.player_t;
import doom.thinker_t;

// Map Object definition.
// typedef struct mobj_s


public class mobj_t implements Interceptable {
    
        public mobj_t(){
            
        }

        /** List: thinker links. */
        public thinker_t       thinker;

        /** (fixed_t) Info for drawing: position. */
        public int     x,y,z;

        /** More list: links in sector (if needed) */
        public thinker_t  snext, sprev;

        //More drawing info: to determine current sprite.
        /** orientation */
        public int     angle;  
        
        /** used to find patch_t and flip value */
        public spritenum_t     sprite; 
        /** might be ORed with FF_FULLBRIGHT */
        int         frame;  

        /** Interaction info, by BLOCKMAP. Links in blocks (if needed). */
        public mobj_t  bnext, bprev;
        
        /** MAES: was actually struct subsector_s */ 
        public subsector_t subsector;

        /** (fixed_t) The closest interval over all contacted Sectors. */
        public int     floorz,ceilingz;

        /** (fixed_t) For movement checking. */
        public int     radius,  height; 

        /** (fixed_t) Momentums, used to update position. */
        public int     momx , momy,  momz;

        /** If == validcount, already checked. */
        public int         validcount;

        public mobjtype_t      type;
        // MAES: was a pointer
        public mobjinfo_t      info;   // &mobjinfo[mobj->type]
        
        public int         tics;   // state tic counter
        // MAES: was a pointer
        public state_t     state;
        public int         flags;
        public int         health;

        /** Movement direction, movement generation (zig-zagging). */
        public int         movedir;    // 0-7
        public int         movecount;  // when 0, select a new dir

        /** Thing being chased/attacked (or NULL),
         *also the originator for missiles.
         * MAES: was a pointer */
        public mobj_t  target;

        /** Reaction time: if non 0, don't attack yet.
           Used by player to freeze a bit after teleporting. */
        public int         reactiontime;   

        /** If >0, the target will be chased
          no matter what (even if shot) */
        public int         threshold;

        /** Additional info record for player avatars only.
           Only valid if type == MT_PLAYER
          struct player_s*    player;*/

        player_t    player;
        
        /** Player number last looked for. */
        public int         lastlook;   

        /** For nightmare respawn. */
       public mapthing_t      spawnpoint; 

        /** Thing being chased/attacked for tracers. */

        mobj_t  tracer;         // MAES: was a pointer 
        
    }
