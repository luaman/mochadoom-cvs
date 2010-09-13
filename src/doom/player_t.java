package doom;

import i.system;

import java.util.Arrays;

import data.doomstat;
import data.Defines.ammotype_t;
import data.Defines.card_t;
import data.Defines.skill_t;
import static data.Defines.*;
import static data.Tables.*;
import static m.fixed_t.*;
import static data.info.*;

import p.mobj_t;
import p.pspdef_t;
import static data.Limits.*;
import static doom.items.weaponinfo;
import static p.SpriteAnimations.NUMPSPRITES;
import static p.mobj.MF_SHADOW;

/**
 * Extended player object info: player_t The player data structure depends on a
 * number of other structs: items (internal inventory), animation states
 * (closely tied to the sprites used to represent them, unfortunately). 
 * 
 * #include "d_items.h" 
 * #include "p_pspr.h" 
 * 
 * In addition, the player is just a special
 * case of the generic moving object/actor. 
 * NOTE: this doesn't mean it needs to extend it, although it would be
 * possible.
 * 
 * #include "p_mobj.h" Finally, for odd
 * reasons, the player input is buffered within the player data struct, as
 * commands per game tick. 
 * 
 * #include "d_ticcmd.h"
 */

public class player_t extends mobj_t
        implements Cloneable
/*  */{
    public static player_t nullplayer;
    static {
        nullplayer = new player_t();
    }

    public player_t() {
        powers = new int[NUMPOWERS];
        frags = new int[MAXPLAYERS];
        ammo = new int[NUMAMMO];
        maxammo = new int[NUMAMMO];
        cards = new boolean[card_t.NUMCARDS.ordinal()];
        weaponowned = new boolean[NUMWEAPONS];
        psprites = new pspdef_t[NUMPSPRITES];
    }

    public final static int CF_NOCLIP = 1; // No damage, no health loss.

    public final static int CF_GODMODE = 2;

    public final static int CF_NOMOMENTUM = 4; // Not really a cheat, just a debug aid.

    
    /** The "mobj state" of the player is stored here, even though he "inherits"
     *  all mobj_t properties (except being a thinker). However, for good or bad,
     *  his mobj properties are modified by accessing player.mo
     */
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
     * CORRECTION: this is also used by the automap widget.
     * MAES: fugly hax, as even passing "Integers" won't work, as they are immutable.
     * Fuck that, I'm doing it the fugly MPI Java way!
     */
    public int[] health = new int[1];

    /** has to be passed around :-( */
    public int[] armorpoints = new int[1];

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

    /**
     * Current PLAYPAL, ??? can be set to REDCOLORMAP for pain, etc. MAES: "int"
     * my ass. It's yet another pointer alias into colormaps. Ergo, array and
     * pointer.
     */

    // public byte[] fixedcolormap;
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

        Arrays.fill(this.ammo, 0);
        Arrays.fill(this.armorpoints, 0);
        Arrays.fill(this.cards, false);
        Arrays.fill(this.frags, 0);
        Arrays.fill(this.health, 0);
        Arrays.fill(this.maxammo, 0);
        Arrays.fill(this.powers, 0);
        Arrays.fill(this.weaponowned, false);
        Arrays.fill(this.psprites, null);
        this.armortype = 0;
        this.attackdown = false;
        this.attacker = null;
        this.backpack = false;
        this.bob = 0;

    }

    @Override
    public player_t clone()
            throws CloneNotSupportedException {
        return (player_t) super.clone();
    }

    /** 16 pixels of bob */
    private static int MAXBOB = 0x100000;

    /**
     * P_Thrust Moves the given origin along a given angle.
     * 
     * @param angle
     *        (angle_t)
     * @param move
     *        (fixed_t)
     */

    public void Thrust(int angle, int move) {
        angle >>= ANGLETOFINESHIFT;

        mo.momx += FixedMul(move, finecosine[angle]);
        mo.momy += FixedMul(move, finesine[angle]);
    }

    //
    // P_CalcHeight
    // Calculate the walking / running height adjustment
    //
    void P_CalcHeight() {
        int angle;
        int bob;

        // Regular movement bobbing
        // (needs to be calculated for gun swing
        // even if not on ground)
        // OPTIMIZE: tablify angle
        // Note: a LUT allows for effects
        // like a ramp with low health.
        bob = FixedMul(mo.momx, mo.momx) + FixedMul(mo.momy, mo.momy);

        bob >>= 2;

        if (bob > MAXBOB)
            bob = MAXBOB;

        if (((cheats & CF_NOMOMENTUM) != 0) || !onground) {
            viewz = mo.z + VIEWHEIGHT;

            if (viewz > mo.ceilingz - 4 * FRACUNIT)
                viewz = mo.ceilingz - 4 * FRACUNIT;

            viewz = mo.z + viewheight;
            return;
        }

        angle = (FINEANGLES / 20 * DS.leveltime) & FINEMASK;
        bob = FixedMul(bob / 2, finesine[angle]);

        // move viewheight
        if (playerstate == PST_LIVE) {
            viewheight += deltaviewheight;

            if (viewheight > VIEWHEIGHT) {
                viewheight = VIEWHEIGHT;
                deltaviewheight = 0;
            }

            if (viewheight < VIEWHEIGHT / 2) {
                viewheight = VIEWHEIGHT / 2;
                if (deltaviewheight <= 0)
                    deltaviewheight = 1;
            }

            if (deltaviewheight != 0) {
                deltaviewheight += FRACUNIT / 4;
                if (deltaviewheight == 0)
                    deltaviewheight = 1;
            }
        }
        viewz = mo.z + viewheight + bob;

        if (viewz > mo.ceilingz - 4 * FRACUNIT)
            viewz = mo.ceilingz - 4 * FRACUNIT;
    }

    /**
     * P_MovePlayer
     */
    public void MovePlayer() {
        ticcmd_t cmd;

        cmd = this.cmd;

        mo.angle += (cmd.angleturn << 16);

        // Do not let the player control movement
        // if not onground.
        onground = (mo.z <= mo.floorz);

        if (cmd.forwardmove != 0 && onground)
            Thrust(mo.angle, cmd.forwardmove * 2048);

        if (cmd.sidemove != 0 && onground)
            Thrust(mo.angle - ANG90, cmd.sidemove * 2048);

        if ((cmd.forwardmove != 0 || cmd.sidemove != 0)
                && mo.state == states[statenum_t.S_PLAY.ordinal()]) {
            this.mo.SetMobjState(statenum_t.S_PLAY_RUN1);
        }
    }


    //
    // GET STUFF
    //

    
    // a weapon is found with two clip loads,
    // a big item has five clip loads
    // public static final int[] maxammo = {200, 50, 300, 50};
    public static final int[] clipammo = { 10, 4, 20, 1 };

    /**
     * P_GiveAmmo Num is the number of clip loads, not the individual count (0=
     * 1/2 clip).
     * 
     * @return false if the ammo can't be picked up at all
     * @param ammo
     *        intended to be ammotype_t.
     */
        
    public boolean GiveAmmo(ammotype_t amm, int num) {
        int oldammo;
        int ammo = amm.ordinal();
        if (ammo == ammotype_t.am_noammo.ordinal())
            return false;

        if (ammo < 0 || ammo > NUMAMMO)
            system.Error("P_GiveAmmo: bad type %i", ammo);

        if (this.ammo[ammo] == maxammo[ammo])
            return false;

        if (num != 0)
            num *= clipammo[ammo];
        else
            num = clipammo[ammo] / 2;

        if (DS.gameskill == skill_t.sk_baby
                || DS.gameskill == skill_t.sk_nightmare) {
            // give double ammo in trainer mode,
            // you'll need in nightmare
            num <<= 1;
        }

        oldammo = this.ammo[ammo];
        this.ammo[ammo] += num;

        if (this.ammo[ammo] > maxammo[ammo])
            this.ammo[ammo] = maxammo[ammo];

        // If non zero ammo,
        // don't change up weapons,
        // player was lower on purpose.
        if (oldammo != 0)
            return true;

        // We were down to zero,
        // so select a new weapon.
        // Preferences are not user selectable.
        switch (ammotype_t.values()[ammo]) {
        case am_clip:
            if (readyweapon == weapontype_t.wp_fist) {
                if (weaponowned[weapontype_t.wp_chaingun.ordinal()])
                    pendingweapon = weapontype_t.wp_chaingun;
                else
                    pendingweapon = weapontype_t.wp_pistol;
            }
            break;

        case am_shell:
            if (readyweapon == weapontype_t.wp_fist
                    || readyweapon == weapontype_t.wp_pistol) {
                if (weaponowned[weapontype_t.wp_shotgun.ordinal()])
                    pendingweapon = weapontype_t.wp_shotgun;
            }
            break;

        case am_cell:
            if (readyweapon == weapontype_t.wp_fist
                    || readyweapon == weapontype_t.wp_pistol) {
                if (weaponowned[weapontype_t.wp_plasma.ordinal()])
                    pendingweapon = weapontype_t.wp_plasma;
            }
            break;

        case am_misl:
            if (readyweapon == weapontype_t.wp_fist) {
                if (weaponowned[weapontype_t.wp_missile.ordinal()])
                    pendingweapon = weapontype_t.wp_missile;
            }
        default:
            break;
        }

        return true;
    }

    public static final int BONUSADD = 6;

    //
    // P_GiveWeapon
    // The weapon name may have a MF_DROPPED flag ored in.
    //
    public boolean GiveWeapon(weapontype_t weapn, boolean dropped) {
        boolean gaveammo;
        boolean gaveweapon;
        int weapon = weapn.ordinal();

        if (DS.netgame && (DS.deathmatch != true) // ???? was "2"
                && !dropped) {
            // leave placed weapons forever on net games
            if (weaponowned[weapon])
                return false;

            bonuscount += BONUSADD;
            weaponowned[weapon] = true;

            if (DS.deathmatch)
                GiveAmmo(weaponinfo[weapon].ammo, 5);
            else
                GiveAmmo(weaponinfo[weapon].ammo, 2);
            pendingweapon = weapn;

            if (this == DS.players[DS.consoleplayer])
                // TODO: S_StartSound (null, sfx_wpnup);
                return false;
        }

        if (weaponinfo[weapon].ammo != ammotype_t.am_noammo) {
            // give one clip with a dropped weapon,
            // two clips with a found weapon
            if (dropped)
                gaveammo = GiveAmmo(weaponinfo[weapon].ammo, 1);
            else
                gaveammo = GiveAmmo(weaponinfo[weapon].ammo, 2);
        } else
            gaveammo = false;

        if (weaponowned[weapon])
            gaveweapon = false;
        else {
            gaveweapon = true;
            weaponowned[weapon] = true;
            pendingweapon = weapn;
        }

        return (gaveweapon || gaveammo);
    }

    /**
     * P_GiveBody Returns false if the body isn't needed at all
     */

    public boolean GiveBody(int num) {
        if (this.health[0] >= MAXHEALTH)
            return false;

        health[0] += num;
        if (health[0] > MAXHEALTH)
            health[0] = MAXHEALTH;
        mo.health = health[0];

        return true;
    }

    /**
     * P_GiveArmor Returns false if the armor is worse than the current armor.
     */

    public boolean GiveArmor(int armortype) {
        int hits;

        hits = armortype * 100;
        if (armorpoints[0] >= hits)
            return false; // don't pick up

        this.armortype = armortype;
        armorpoints[0] = hits;

        return true;
    }

    //
    // P_GiveCard
    //
    public void GiveCard(card_t crd) {
        int card = crd.ordinal();
        if (cards[card])
            return;

        bonuscount = BONUSADD;
        cards[card] = true;
    }

    //
    // P_GivePower
    //
    public boolean GivePower( int /* powertype_t */power) // MAES:
                                                                          // I
                                                                          // didn't
                                                                          // change
                                                                          // this!
    {
        if (power == pw_invulnerability) {
            powers[power] = INVULNTICS;
            return true;
        }

        if (power == pw_invisibility) {
            powers[power] = INVISTICS;
            mo.flags |= MF_SHADOW;
            return true;
        }

        if (power == pw_infrared) {
            powers[power] = INFRATICS;
            return true;
        }

        if (power == pw_ironfeet) {
            powers[power] = IRONTICS;
            return true;
        }

        if (power == pw_strength) {
            GiveBody(100);
            powers[power] = 1;
            return true;
        }

        if (powers[power] != 0)
            return false; // already got it

        powers[power] = 1;
        return true;
    }

    /** Accessory method to identify which "doomguy" we are.
     *  Because we can't use the [target.player-players] syntax
     *  in order to get an array index, in Java.
     *  
     *  If -1 is returned, then we have existential problems.
     *
     */    
    
    public int identify(){
        
        if (id>=0) return id;
            
        // Let's say that we don't know jack.
        id=-1;
            for (int i=0;i<DS.players.length;i++){
                if (this==DS.players[i]) id=i;
            }
        return id;
    }
    
    private int id;
    
    private boolean onground;

    /** Probably doomguy needs to know what the fuck is going on */
    private doomstat DS;

}
