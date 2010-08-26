package p;

import rr.subsector_t;
import static m.fixed_t.*;
import data.mobjinfo_t;
import data.state_t;
import data.mapthing_t;
import data.mobjtype_t;
import data.spritenum_t;
import data.Defines.statenum_t;
import doom.acp1;
import doom.acp2;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;
import static data.info.*;
import static data.Tables.*;
import static data.SineCosine.*;

// Map Object definition.
// typedef struct mobj_s


public class mobj_t implements Interceptable, thinker_t  {
    
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
        public mobjinfo_t      info;   // &mobjinfo[mobj.type]
        
        public long         tics;   // state tic counter
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

       public  player_t    player;
        
        /** Player number last looked for. */
        public int         lastlook;   

        /** For nightmare respawn. */
       public mapthing_t      spawnpoint; 

        /** Thing being chased/attacked for tracers. */

       public mobj_t  tracer;         // MAES: was a pointer 
     

       //// MF_ flags for mobjs.
    
     
 // Call P_SpecialThing when touched.
    public static int MF_SPECIAL        = 1;
    // Blocks.
    public static int MF_SOLID      = 2;
    // Can be hit.
    public static int MF_SHOOTABLE  = 4;
    // Don't use the sector links (invisible but touchable).
    public static int MF_NOSECTOR       = 8;
    // Don't use the blocklinks (inert but displayable)
    public static int MF_NOBLOCKMAP = 16;                    

    // Not to be activated by sound, deaf monster.
    public static int MF_AMBUSH     = 32;
    // Will try to attack right back.
    public static int MF_JUSTHIT        = 64;
    // Will take at least one step before attacking.
    public static int MF_JUSTATTACKED   = 128;
    // On level spawning (initial position),
    //  hang from ceiling instead of stand on floor.
    public static int MF_SPAWNCEILING   = 256;
    // Don't apply gravity (every tic),
    //  that is, object will float, keeping current height
    //  or changing it actively.
    public static int MF_NOGRAVITY  = 512;

    // Movement flags.
    // This allows jumps from high places.
    public static int MF_DROPOFF        = 0x400;
    // For players, will pick up items.
    public static int MF_PICKUP     = 0x800;
    // Player cheat. ???
    public static int MF_NOCLIP     = 0x1000;
    // Player: keep info about sliding along walls.
    public static int MF_SLIDE      = 0x2000;
    // Allow moves to any height, no gravity.
    // For active floaters, e.g. cacodemons, pain elementals.
    public static int MF_FLOAT      = 0x4000;
    // Don't cross lines
    //   ??? or look at heights on teleport.
    public static int MF_TELEPORT       = 0x8000;
    // Don't hit same species, explode on block.
    // Player missiles as well as fireballs of various kinds.
    public static int MF_MISSILE        = 0x10000;  
    // Dropped by a demon, not level spawned.
    // E.g. ammo clips dropped by dying former humans.
    public static int MF_DROPPED        = 0x20000;
    // Use fuzzy draw (shadow demons or spectres),
    //  temporary player invisibility powerup.
    public static int MF_SHADOW     = 0x40000;
    // Flag: don't bleed when shot (use puff),
    //  barrels and shootable furniture shall not bleed.
    public static int MF_NOBLOOD        = 0x80000;
    // Don't stop moving halfway off a step,
    //  that is, have dead bodies slide down all the way.
    public static int MF_CORPSE     = 0x100000;
    // Floating to a height for a move, ???
    //  don't auto float to target's height.
    public static int MF_INFLOAT        = 0x200000;

    // On kill, count this enemy object
    //  towards intermission kill total.
    // Happy gathering.
    public static int MF_COUNTKILL  = 0x400000;
    
    // On picking up, count this item object
    //  towards intermission item total.
    public static int MF_COUNTITEM  = 0x800000;

    // Special handling: skull in flight.
    // Neither a cacodemon nor a missile.
    public static int    MF_SKULLFLY        = 0x1000000;

    // Don't spawn this object
    //  in death match mode (e.g. key cards).
    public static int    MF_NOTDMATCH       = 0x2000000;

    // Player sprites in multiplayer modes are modified
    //  using an internal color lookup table for re-indexing.
    // If 0x4 0x8 or 0xc,
    //  use a translation table for player colormaps
    public static int    MF_TRANSLATION     = 0xc000000;
    // Hmm ???.
    public static int MF_TRANSSHIFT = 26;
    @Override
    public think_t getFunction() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public thinker_t getNext() {
        return this.snext;
    }
    @Override
    public thinker_t getPrev() {
        return this.sprev;
    }
    @Override
    public void setFunction(think_t acv) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void setNext(thinker_t t) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void setPrev(thinker_t t) {
        // TODO Auto-generated method stub
        
    }

   
    }
