package p;

import static data.Defines.acp1;
import static data.Defines.FLOATSPEED;
import static data.Defines.GRAVITY;
import static data.Defines.VIEWHEIGHT;
import static data.info.mobjinfo;
import static data.info.states;
import static p.MapUtils.AproxDistance;

import java.io.IOException;

import m.random;
import rr.subsector_t;
import w.DoomFile;
import w.IReadableDoomObject;
import data.Tables;
import data.doomdata;
import data.mapthing_t;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.spritenum_t;
import data.state_t;
import defines.*;
import doom.player_t;
import doom.thinker_t;

/**
 * 
 * NOTES: mobj_t
 * 
 * mobj_ts are used to tell the refresh where to draw an image,
 * tell the world simulation when objects are contacted,
 * and tell the sound driver how to position a sound.
 * 
 * The refresh uses the next and prev links to follow
 * lists of things in sectors as they are being drawn.
 * The sprite, frame, and angle elements determine which patch_t
 * is used to draw the sprite if it is visible.
 * The sprite and frame values are allmost allways set
 * from state_t structures.
 * The statescr.exe utility generates the states.h and states.c
 * files that contain the sprite/frame numbers from the
 * statescr.txt source file.
 * The xyz origin point represents a point at the bottom middle
 * of the sprite (between the feet of a biped).
 * This is the default origin position for patch_ts grabbed
 * with lumpy.exe.
 * A walking creature will have its z equal to the floor
 * it is standing on.
 * 
 * The sound code uses the x,y, and subsector fields
 * to do stereo positioning of any sound effited by the mobj_t.
 * 
 * The play simulation uses the blocklinks, x,y,z, radius, height
 * to determine when mobj_ts are touching each other,
 * touching lines in the map, or hit by trace lines (gunshots,
 * lines of sight, etc).
 * The mobj_t->flags element has various bit flags
 * used by the simulation.
 * 
 * Every mobj_t is linked into a single sector
 * based on its origin coordinates.
 * The subsector_t is found with R_PointInSubsector(x,y),
 * and the sector_t can be found with subsector->sector.
 * The sector links are only used by the rendering code,
 * the play simulation does not care about them at all.
 * 
 * Any mobj_t that needs to be acted upon by something else
 * in the play world (block movement, be shot, etc) will also
 * need to be linked into the blockmap.
 * If the thing has the MF_NOBLOCK flag set, it will not use
 * the block links. It can still interact with other things,
 * but only as the instigator (missiles will run into other
 * things, but nothing can run into a missile).
 * Each block in the grid is 128*128 units, and knows about
 * every line_t that it contains a piece of, and every
 * interactable mobj_t that has its origin contained.  
 * 
 * A valid mobj_t is a mobj_t that has the proper subsector_t
 * filled in for its xy coordinates and is linked into the
 * sector from which the subsector was made, or has the
 * MF_NOSECTOR flag set (the subsector_t needs to be valid
 * even if MF_NOSECTOR is set), and is linked into a blockmap
 * block or has the MF_NOBLOCKMAP flag set.
 * Links should only be modified by the P_[Un]SetThingPosition()
 * functions.
 * Do not change the MF_NO? flags while a thing is valid.
 * 
 * Any questions?
 * 
 * @author admin
 *
 */

public class mobj_t extends thinker_t implements Interceptable, IReadableDoomObject   {
   
    Actions A;    
    
        public mobj_t(){
            this.spawnpoint=new mapthing_t();
        }
    
        public mobj_t(Actions A){
            this.A=A;
            // A mobj_t is ALSO a thinker, as it always contains the struct.
            // Don't fall for C's trickery ;-)
            //this.thinker=new thinker_t();
        }

        /* List: thinker links. */
        //public thinker_t       thinker;
        
        

        /** (fixed_t) Info for drawing: position. */
        public int     x,y,z;

        /** More list: links in sector (if needed) */
        public thinker_t  snext, sprev;

        //More drawing info: to determine current sprite.
        /** orientation. This needs to be long or else certain checks will fail...but
         * I need to see it working in order to confirm */
        public long     angle;  
        
        /** used to find patch_t and flip value */
        public spritenum_t     sprite; 
        /** might be ORed with FF_FULLBRIGHT */
        public int         frame;  

        /** Interaction info, by BLOCKMAP. Links in blocks (if needed). */
        public thinker_t  bnext, bprev;
        
        /** MAES: was actually a pointer to a struct subsector_s */ 
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

        /** For nightmare respawn.*/
       public mapthing_t      spawnpoint; // struct 

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
    
    
    /* The following methods were for the most part "contextless" and instance-specific,
     * so they were implemented here rather that being scattered all over
     * the package.
     */
    
    /**
     * P_SetMobjState
     * Returns true if the mobj is still present.
     */

    public boolean
    SetMobjState
    (statenum_t    state )
    {
        state_t st;

        do
        {
        if (state == statenum_t.S_NULL)
        {
            state = null;
            // MAES/_D_: uncommented this as it should work by now (?).
            A.RemoveMobj (this);
            return false;
        }

        st = states[state.ordinal()];
        this.state = st;
        tics = st.tics;
        sprite = st.sprite;
        frame = (int) st.frame;

        // Modified handling.
        // Call action functions when the state is set
        //_D_: changed this, causing a LOT of action to work
        // MAES: workaround not needed since types
        // are now set correctly.
        if (st.action!=null && st.action.getType()==acp1)       
            {A.dispatch(st.action, this, null);} 
        
        state = st.nextstate;
        } while (tics==0);
                    
        return true;
    }
  
    /**
     * P_ZMovement
     */
    
    public void ZMovement ()
    {
        // fixed_t
        int dist;
        int delta;
        
        // check for smooth step up
        if ((player!=null) && z < floorz)
        {
        player.viewheight -= floorz-z;

        player.deltaviewheight
            = (VIEWHEIGHT - player.viewheight)>>3;
        }
        
        // adjust height
        z += momz;
        
        if ( ((flags & MF_FLOAT)!=0)
         && target!=null)
        {
        // float down towards target if too close
        if ( (flags & MF_SKULLFLY)==0
             && (flags & MF_INFLOAT)==0 )
        {
            dist = AproxDistance (x - target.x,
                        y - target.y);
            
            delta =(target.z + (height>>1)) - z;

            if (delta<0 && dist < -(delta*3) )
            z -= FLOATSPEED;
            else if (delta>0 && dist < (delta*3) )
            z += FLOATSPEED;            
        }
        
        }
        
        // clip movement
        if (z <= floorz)
        {
        // hit the floor

        // Note (id):
        //  somebody left this after the setting momz to 0,
        //  kinda useless there.
        if ((flags & MF_SKULLFLY)!=0)
        {
            // the skull slammed into something
            momz = -momz;
        }
        
        if (momz < 0)
        {
            if (player!=null
            && (momz < -GRAVITY*8))   
            {
            // Squat down.
            // Decrease viewheight for a moment
            // after hitting the ground (hard),
            // and utter appropriate sound.
            player.deltaviewheight = momz>>3;
           // TODO:  S_StartSound (mo, sfx_oof);
            }
            momz = 0;
        }
        z = floorz;

        if ( (flags & MF_MISSILE)!=0
             && (flags & MF_NOCLIP)==0 )
        {
            A.ExplodeMissile(this);
            return;
        }
        }
        else if ((flags & MF_NOGRAVITY)==0 )
        {
        if (momz == 0)
            momz = -GRAVITY*2;
        else
            momz -= GRAVITY;
        }
        
        if (z + height > ceilingz)
        {
        // hit the ceiling
        if (momz > 0)
            momz = 0;
        {
            z = ceilingz - height;
        }

        if ((flags & MF_SKULLFLY)!=0)
        {   // the skull slammed into something
            momz = -momz;
        }
        
        if ( (flags & MF_MISSILE)!=0
             && (flags & MF_NOCLIP)==0 )
        {
            A.ExplodeMissile(this);
            return;
        }
        }
    } 
    
    
  //_D_: to permit this object to save/load
    public void read(DoomFile f) throws IOException {
        super.read(f); // Read the head thinker.
        this.x=f.readLEInt();
        this.y=f.readLEInt();
        this.z=f.readLEInt();
        f.skipBytes(8); // TODO: snext, sprev. When are those set?
        this.angle=Tables.BITS32&f.readLEInt();
        this.sprite=spritenum_t.values()[f.readLEInt()];
        this.frame=f.readLEInt();
        f.skipBytes(8); // TODO: bnext, bprev. When are those set?
        f.skipBytes(4); // TODO: subsector
        this.floorz=f.readLEInt();
        this.ceilingz=f.readLEInt();
        this.radius=f.readLEInt();
        this.height=f.readLEInt();
        this.momx=f.readLEInt();
        this.momy=f.readLEInt();
        this.momz=f.readLEInt();
        this.validcount=f.readInt();
        this.type=mobjtype_t.values()[f.readInt()];
        this.tics=Tables.BITS32&f.readLEInt();
        System.out.println("State"+f.readLEInt());
        //this.state=data.info.states[f.readLEInt()]; // TODO: state OK?
        this.flags=f.readLEInt();
        this.health=f.readLEInt();
        this.movedir=f.readLEInt();
        this.movecount=f.readLEInt();
        f.skipBytes(4); // TODO: target
        this.reactiontime=f.readLEInt();        
        this.threshold=f.readLEInt();
        f.skipBytes(4); // TODO: player. Non null should mean that it IS a player.
        this.lastlook=f.readInt();
        spawnpoint.read(f);
        f.skipBytes(4); // TODO: tracer
     }
    
    public int         eflags; //DOOM LEGACY

    // TODO: a linked list of sectors where this object appears
    // public msecnode_t touching_sectorlist;
    }
