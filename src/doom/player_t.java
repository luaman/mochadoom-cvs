package doom;

import java.util.Arrays;

import p.mobj_t;
import p.pspdef_t;
import m.fixed_t;
import static data.Limits.*;
import static data.Defines.*;
import static p.SpriteAnimations.NUMPSPRITES;

/**
 * Extended player object info: player_t The player data structure depends on a
 * number of other structs: items (internal inventory), animation states
 * (closely tied to the sprites used to represent them, unfortunately). #include
 * "d_items.h" #include "p_pspr.h" In addition, the player is just a special
 * case of the generic moving object/actor. #include "p_mobj.h" Finally, for odd
 * reasons, the player input is buffered within the player data struct, as
 * commands per game tick. #include "d_ticcmd.h"
 */

public class player_t implements Cloneable
        /* extends mobj_t */ {
    public static player_t nullplayer;
    static {
        nullplayer=new player_t();
    }
    
    public player_t(){
        powers = new int[NUMPOWERS];
        frags = new int[MAXPLAYERS];
        ammo = new int[NUMAMMO];
        maxammo = new int[NUMAMMO];
        cards= new boolean[card_t.NUMCARDS.ordinal()];
        weaponowned = new boolean[NUMWEAPONS];
        psprites =new pspdef_t[NUMPSPRITES];
    }

    public final static int CF_NOCLIP = 1; // No damage, no health loss.

    public final static int CF_GODMODE = 2;

    public final static int CF_NOMOMENTUM = 4; // Not really a cheat, just a
                                               // debug aid.

    public mobj_t mo;

    /** playerstate_t */
    public int playerstate;

    public ticcmd_t cmd;

    /**
     * Determine POV, including viewpoint bobbing during movement. (fixed_t)
     * Focal origin above r.z
     */
    public int viewz;

    /** (fixed_t) Base height above floor for viewz. */
    public int viewheight;

    /** (fixed_t) Bob/squat speed. */
    public int deltaviewheight;

    /** (fixed_t) bounded/scaled total momentum. */
    public int bob;

    /**
     * This is only used between levels, mo->health is used during levels.
     * MAES: fugly hax, as even passing "Integers" won't work, as they are immutable.
     * Fuck that, I'm doing it the fugly MPI Java way!
     */
    public int[] health=new int[1];

    /** has to be passed around :-( */
    public int[] armorpoints=new int[1];

    /** Armor type is 0-2. */
    public int armortype;

    /** Power ups. invinc and invis are tic counters. */
    public int[] powers;

    public boolean[] cards;

    public boolean backpack;

    // Frags, kills of other players.
    public int[] frags;

    public weapontype_t readyweapon;

    // Is wp_nochange if not changing.
    public weapontype_t pendingweapon;

    public boolean[] weaponowned;

    public int[] ammo;

    public int[] maxammo;

    /** True if button down last tic. */
    public boolean attackdown;

    public boolean usedown;

    // Bit flags, for cheats and debug.
    // See cheat_t, above.
    public int cheats;

    // Refired shots are less accurate.
    public int refire;

    // For intermission stats.
    public int killcount;

    public int itemcount;

    public int secretcount;

    // Hint messages.
    public String message;

    // For screen flashing (red or bright).
    public int damagecount;

    public int bonuscount;

    // Who did damage (NULL for floors/ceilings).
    public mobj_t attacker;

    // So gun flashes light up areas.
    public int extralight;

    /** Current PLAYPAL, ???
        can be set to REDCOLORMAP for pain, etc.
        MAES: "int" my ass. It's yet another pointer alias into colormaps.
        Ergo, array and pointer.
        
    */
    
    
    //public byte[] fixedcolormap;
    public int fixedcolormap;
    

    // Player skin colorshift,
    // 0-3 for which color to draw player.
    public int colormap;

    // TODO: Overlay view sprites (gun, etc).
    public pspdef_t[] psprites;

    // True if secret level has been done.
    public boolean didsecret;

    /** It's probably faster to clone the null player */
    
    public void reset() {
        
        Arrays.fill(this.ammo,0);
        Arrays.fill(this.armorpoints,0);
        Arrays.fill(this.cards,false);
        Arrays.fill(this.frags, 0);
        Arrays.fill(this.health,0);
        Arrays.fill(this.maxammo,0);
        Arrays.fill(this.powers,0);
        Arrays.fill(this.weaponowned,false);
        Arrays.fill(this.psprites,null);
        this.armortype=0;
        this.attackdown=false;
        this.attacker=null;
        this.backpack=false;
        this.bob=0;
        
    }
    @Override
    public player_t clone() throws CloneNotSupportedException{
        return (player_t) super.clone();
    }

}
