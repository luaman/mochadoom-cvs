package p;

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


public class mobj_t {

        // List: thinker links.
        thinker_t       thinker;

        // Info for drawing: position.
        fixed_t     x;
        fixed_t     y;
        fixed_t     z;

        // More list: links in sector (if needed)
        thinker_t  snext;
        thinker_t  sprev;

        //More drawing info: to determine current sprite.
        //TODO angle_t     angle;  // orientation
        spritenum_t     sprite; // used to find patch_t and flip value
        int         frame;  // might be ORed with FF_FULLBRIGHT

        // Interaction info, by BLOCKMAP.
        // Links in blocks (if needed).
        mobj_t  bnext;
        mobj_t  bprev;
        
        //TODO: struct subsector_s* subsector;

        // The closest interval over all contacted Sectors.
        fixed_t     floorz;
        fixed_t     ceilingz;

        // For movement checking.
        fixed_t     radius;
        fixed_t     height; 

        // Momentums, used to update position.
        fixed_t     momx;
        fixed_t     momy;
        fixed_t     momz;

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
        //TODO: mapthing_t      spawnpoint; 

        // Thing being chased/attacked for tracers.
        // MAES: was a pointer
        mobj_t  tracer; 
        
    }
