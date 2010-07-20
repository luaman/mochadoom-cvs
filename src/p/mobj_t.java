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

        /** List: thinker links. */
        thinker_t       thinker;

        /** (fixed_t) Info for drawing: position. */
        public int     x,y,z;

        // More list: links in sector (if needed)
        thinker_t  snext;
        thinker_t  sprev;

        //More drawing info: to determine current sprite.
        /** orientation */
        int     angle;  
        
        /** used to find patch_t and flip value */
        spritenum_t     sprite; 
        /** might be ORed with FF_FULLBRIGHT */
        int         frame;  

        /** Interaction info, by BLOCKMAP. Links in blocks (if needed). */
        mobj_t  bnext, bprev;
        
        /** MAES: was actually struct subsector_s */ 
        subsector_t subsector;

        /** The closest interval over all contacted Sectors. */
        fixed_t     floorz;
        fixed_t     ceilingz;

        /** (fixed_t) For movement checking. */
        int     radius,  height; 

        /** (fixed_t) Momentums, used to update position. */
        int     momx , momy,  momz;

        // If == validcount, already checked.
        int         validcount;

        mobjtype_t      type;
        // MAES: was a pointer
        mobjinfo_t      info;   // &mobjinfo[mobj->type]
        
        int         tics;   // state tic counter
        // MAES: was a pointer
        state_t     state;
        int         flags;
        int         health;

        // Movement direction, movement generation (zig-zagging).
        int         movedir;    // 0-7
        int         movecount;  // when 0, select a new dir

        // Thing being chased/attacked (or NULL),
        // also the originator for missiles.
        // MAES: was a pointer
        mobj_t  target;

        // Reaction time: if non 0, don't attack yet.
        // Used by player to freeze a bit after teleporting.
        int         reactiontime;   

        // If >0, the target will be chased
        // no matter what (even if shot)
        int         threshold;

        // Additional info record for player avatars only.
        // Only valid if type == MT_PLAYER
        //struct player_s*    player;

        player_t    player;
        
        // Player number last looked for.
        int         lastlook;   

        // For nightmare respawn.
       mapthing_t      spawnpoint; 

        // Thing being chased/attacked for tracers.
        // MAES: was a pointer
        mobj_t  tracer; 
        
    }
