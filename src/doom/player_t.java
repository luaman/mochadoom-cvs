package doom;

import g.DoomGame;
import i.system;

import java.util.Arrays;

import m.random;

import data.doomstat;
import data.state_t;
import data.Defines.GameMode_t;
import data.Defines.ammotype_t;
import data.Defines.card_t;
import data.Defines.skill_t;
import data.Defines.statenum_t;
import static data.Defines.*;
import static data.SineCosine.finecosine;
import static data.SineCosine.finesine;
import static data.Tables.*;
import static m.fixed_t.*;
import static data.info.*;
import static p.mobj.*;
import p.UnifiedGameMap;
import static p.MapUtils.*;
import p.mobj_t;
import p.pspdef_t;
import rr.RendererState;
import rr.sector_t;
import static p.MapUtils.flags;
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

public class player_t /*extends mobj_t */
        implements Cloneable
        {
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
 


    //
    // P_PlayerInSpecialSector
    // Called every tic frame
    //  that the player origin is in a special sector
    //
    public void PlayerInSpecialSector ()
    {
        sector_t   sector;
        
        sector = mo.subsector.sector;

        // Falling, not all the way down yet?
        if (mo.z != sector.floorheight)
        return; 

        // Has hitten ground.
        switch (sector.special)
        {
          case 5:
        // HELLSLIME DAMAGE
        if (powers[pw_ironfeet]==0)
            if (!flags(DS.leveltime,0x1f))
                P.DamageMobj (mo,null, null, 10);
        break;
        
          case 7:
        // NUKAGE DAMAGE
        if (powers[pw_ironfeet]==0)
            if (!flags(DS.leveltime,0x1f))
                P.DamageMobj (mo, null, null, 5);
        break;
        
          case 16:
        // SUPER HELLSLIME DAMAGE
          case 4:
        // STROBE HURT
        if (!eval(powers[pw_ironfeet])
            || (RND.P_Random()<5) )
        {
            if (!flags(DS.leveltime,0x1f))
            P.DamageMobj (mo, null, null, 20);
        }
        break;
                
          case 9:
        // SECRET SECTOR
        secretcount++;
        sector.special = 0;
        break;
                
          case 11:
        // EXIT SUPER DAMAGE! (for E1M8 finale)
        cheats &= ~CF_GODMODE;

        if (!flags(DS.leveltime,0x1f))
            P.DamageMobj (mo, null, null, 20);

        if (health[0] <= 10)
            G.ExitLevel();
        break;
                
          default:
        system.Error ("P_PlayerInSpecialSector: unknown special %i", sector.special);
        break;
        };
    }    
   
 // Index of the special effects (INVUL inverse) map.
public static final int INVERSECOLORMAP		=32;
    
 /**
  * P_PlayerThink
  * 
  * Because, as we know, Doomguy THINKS, unlike the monsters that
  * maybe don't think.
  * 
  */

 public void PlayerThink ()
 {
     ticcmd_t       cmd=new ticcmd_t();
     weapontype_t    newweapon;
     
     // fixme: do this in the cheat code
     if (flags(cheats , CF_NOCLIP))
     mo.flags |= MF_NOCLIP;
     else
     mo.flags &= ~MF_NOCLIP;
     
     // chain saw run forward
     cmd = this.cmd;
     if (flags(mo.flags, MF_JUSTATTACKED))
     {
     cmd.angleturn = 0;
     cmd.forwardmove = 0xc800/512;
     cmd.sidemove = 0;
     mo.flags &= ~MF_JUSTATTACKED;
     }
             
     
     if (playerstate == PST_DEAD)
     {
     DeathThink ();
     return;
     }
     
     // Move around.
     // Reactiontime is used to prevent movement
     //  for a bit after a teleport.
     if (mo.reactiontime!=0)
     mo.reactiontime--;
     else
     MovePlayer ();
     
     CalcHeight ();

     if (eval(mo.subsector.sector.special))
     PlayerInSpecialSector ();
     
     // Check for weapon change.

     // A special event has no other buttons.
     if (flags(cmd.buttons ,BT_SPECIAL))
     cmd.buttons = 0;           
         
     if (flags(cmd.buttons , BT_CHANGE))
     {
     // The actual changing of the weapon is done
     //  when the weapon psprite can do it
     //  (read: not in the middle of an attack).
     newweapon = weapontype_t.values()[(cmd.buttons&BT_WEAPONMASK)>>BT_WEAPONSHIFT];
     
     if (newweapon == weapontype_t.wp_fist
         && weaponowned[weapontype_t.wp_chainsaw.ordinal()]
         && !(readyweapon == weapontype_t.wp_chainsaw
          && powers[pw_strength]!=0))
     {
         newweapon = weapontype_t.wp_chainsaw;
     }
     
     if ( (DS.gamemode == GameMode_t.commercial)
         && newweapon == weapontype_t.wp_shotgun 
         && weaponowned[weapontype_t.wp_supershotgun.ordinal()]
         && readyweapon != weapontype_t.wp_supershotgun)
     {
         newweapon = weapontype_t.wp_supershotgun;
     }
     

     if (weaponowned[newweapon.ordinal()]
         && newweapon != readyweapon)
     {
         // Do not go to plasma or BFG in shareware,
         //  even if cheated.
         if ((newweapon != weapontype_t.wp_plasma
          && newweapon != weapontype_t.wp_bfg)
         || (DS.gamemode != GameMode_t.shareware) )
         {
         pendingweapon = newweapon;
         }
     }
     }
     
     // check for use
     if (flags(cmd.buttons , BT_USE))
     {
     if (!usedown)
     {
         P.MV.UseLines (this);
         usedown = true;
     }
     }
     else
     usedown = false;
     
     // cycle psprites
     MovePsprites ();
     
     // Counters, time dependend power ups.

     // Strength counts up to diminish fade.
     if (eval(powers[pw_strength]))
     powers[pw_strength]++;  
         
     if (eval(powers[pw_invulnerability]))
     powers[pw_invulnerability]--;

     if (eval(powers[pw_invisibility]))
     if (!eval( --powers[pw_invisibility]) )
         mo.flags &= ~MF_SHADOW;
             
     if (eval(powers[pw_infrared]))
     powers[pw_infrared]--;
         
     if (eval(powers[pw_ironfeet]))
     powers[pw_ironfeet]--;
         
     if (eval(damagecount))
     damagecount--;
         
     if (eval(bonuscount))
     bonuscount--;

     
     // Handling colormaps.
     if (eval(powers[pw_invulnerability]))
     {
     if (powers[pw_invulnerability] > 4*32
         || flags(powers[pw_invulnerability],8) )
         fixedcolormap = INVERSECOLORMAP;
     else
         fixedcolormap = 0;
     }
     else if (eval(powers[pw_infrared]))   
     {
     if (powers[pw_infrared] > 4*32
         || flags(powers[pw_infrared],8) )
     {
         // almost full bright
         fixedcolormap = 1;
     }
     else
         fixedcolormap = 0;
     }
     else
     fixedcolormap = 0;
 }

 
 //
//P_CalcHeight
//Calculate the walking / running height adjustment
//
private void CalcHeight () 
{
  int     angle;
  int bob; // fixed
  
  // Regular movement bobbing
  // (needs to be calculated for gun swing
  // even if not on ground)
  // OPTIMIZE: tablify angle
  // Note: a LUT allows for effects
  //  like a ramp with low health.
  bob =
  FixedMul (mo.momx, mo.momx)
  + FixedMul (mo.momy,mo.momy);
  
  bob >>= 2;

  if (bob>MAXBOB)
  bob = MAXBOB;

  if (flags(cheats ,CF_NOMOMENTUM) || !onground)
  {
  viewz = mo.z + VIEWHEIGHT;

  if (viewz > mo.ceilingz-4*FRACUNIT)
      viewz = mo.ceilingz-4*FRACUNIT;

  viewz = mo.z + viewheight;
  return;
  }
      
  angle = (FINEANGLES/20*DS.leveltime)&FINEMASK;
  bob = FixedMul ( bob/2, finesine[angle]);

  
  // move viewheight
  if (playerstate == PST_LIVE)
  {
  viewheight += deltaviewheight;

  if (viewheight > VIEWHEIGHT)
  {
      viewheight = VIEWHEIGHT;
      deltaviewheight = 0;
  }

  if (viewheight < VIEWHEIGHT/2)
  {
      viewheight = VIEWHEIGHT/2;
      if (deltaviewheight <= 0)
      deltaviewheight = 1;
  }
  
  if (deltaviewheight!=0)    
  {
      deltaviewheight += FRACUNIT/4;
      if (deltaviewheight==0)
      deltaviewheight = 1;
  }
  }
  viewz = mo.z + viewheight + bob;

  if (viewz > mo.ceilingz-4*FRACUNIT)
  viewz = mo.ceilingz-4*FRACUNIT;
} 
 
 //
//P_DeathThink
//Fall on your face when dying.
//Decrease POV height to floor height.
//
private static final int ANG5   = (ANG90/18);

public void DeathThink ()
{
  int     angle; //angle_t
  int     delta;

  MovePsprites ();
  
  // fall to the ground
  if (viewheight > 6*FRACUNIT)
  viewheight -= FRACUNIT;

  if (viewheight < 6*FRACUNIT)
  viewheight = 6*FRACUNIT;

  deltaviewheight = 0;
  onground = (mo.z <= mo.floorz);
  CalcHeight ();
  
  if (attacker!=null && attacker != mo)
  {
  angle = R.PointToAngle2 (mo.x,
               mo.y,
               attacker.x,
               attacker.y);
  
  delta = angle - mo.angle;
  
  if (delta < ANG5 || delta > -ANG5)
  {
      // Looking at killer,
      //  so fade damage flash down.
      mo.angle = angle;

      if (damagecount!=0)
      damagecount--;
  }
  else if (delta < ANG180)
      mo.angle += ANG5;
  else
      mo.angle -= ANG5;
  }
  else if (damagecount!=0)
  damagecount--;
  

  if (flags(cmd.buttons ,BT_USE))
  playerstate = PST_REBORN;
}
 
//
// P_MovePsprites
// Called every tic by player thinking routine.
//
void MovePsprites () 
{

    pspdef_t    psp;
    state_t state;
    
 
    for (int i=0 ; i<NUMPSPRITES ; i++)
    {
    	   psp = psprites[i];
    // a null state means not active
    if ( (state = psp.state)!=null )  
    {
        // drop tic count and possibly change state

        // a -1 tic count never changes
        if (psp.tics != -1) 
        {
        psp.tics--;
        if (!eval(psp.tics))
            this.SetPsprite (i, psp.state.nextstate);
        }               
    }
    }
    
    psprites[ps_flash].sx = psprites[ps_weapon].sx;
    psprites[ps_flash].sy = psprites[ps_weapon].sy;
}

/**
/* P_SetPsprite
*/

public void
SetPsprite
( int       position,
  statenum_t    newstate ) 
{
    pspdef_t    psp;
    state_t state;
    
    psp = psprites[position];
    
    do
    {
    if (newstate==null)
    {
        // object removed itself
        psp.state = null;
        break;  
    }
    
    state = states[newstate.ordinal()];
    psp.state = state;
    psp.tics = (int) state.tics;    // could be 0

    if (state.misc1!=0)
    {
        // coordinate set
        psp.sx = (int) (state.misc1 << FRACBITS);
        psp.sy = (int) (state.misc2 << FRACBITS);
    }
    
    // Call action routine.
    // Modified handling.
    if (state.action.getType()==acp2)
    {
        P.A.dispatch(state.action,this, psp);
        if (psp.state==null)
        break;
    }
    
    newstate = psp.state.nextstate;
    
    } while (psp.tics==0);
    // an initial state of 0 could cycle through
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
    private UnifiedGameMap P;
    private RendererState R;
    private random RND;
    private DoomGame G;
    	
    /* psprnum_t enum */
    public static int ps_weapon=0,
        ps_flash=1,
        NUMPSPRITES=2;  
    
    private static int  LOWERSPEED  =   FRACUNIT*6;
    private static int  RAISESPEED  =   FRACUNIT*6;

    private static int  WEAPONBOTTOM    =128*FRACUNIT;
    private static int  WEAPONTOP       =32*FRACUNIT;


    // plasma cells for a bfg attack
    private static int  BFGCELLS        =40;        


    /**
    /* P_SetPsprite
    */
    
    public void
    SetPsprite
    ( player_t  player,
      int       position,
      statenum_t    newstate ) 
    {
        pspdef_t    psp;
        state_t state;
        
        psp = psprites[position];
        
        do
        {
        if (newstate==null)
        {
            // object removed itself
            psp.state = null;
            break;  
        }
        
        state = states[newstate.ordinal()];
        psp.state = state;
        psp.tics = (int) state.tics;    // could be 0

        if (state.misc1!=0)
        {
            // coordinate set
            psp.sx = (int) (state.misc1 << FRACBITS);
            psp.sy = (int) (state.misc2 << FRACBITS);
        }
        
        // Call action routine.
        // Modified handling.
        if (state.action.getType()==acp2)
        {
            P.A.dispatch(state.action,this, psp);
            if (psp.state==null)
            break;
        }
        
        newstate = psp.state.nextstate;
        
        } while (psp.tics==0);
        // an initial state of 0 could cycle through
    }
    
    /** fixed_t */
    int     swingx, swingy;

    /**P_CalcSwing
     * 
     * @param player
     */
    public void P_CalcSwing (player_t   player)
    {
        int swing; // fixed_t
        int     angle;
        
        // OPTIMIZE: tablify this.
        // A LUT would allow for different modes,
        //  and add flexibility.

        swing = bob;

        angle = (FINEANGLES/70*DS.leveltime)&FINEMASK;
        swingx = FixedMul ( swing, finesine[angle]);

        angle = (FINEANGLES/70*DS.leveltime+FINEANGLES/2)&FINEMASK;
        swingy = -FixedMul ( swingx, finesine[angle]);
    }



    //
    // P_BringUpWeapon
    // Starts bringing the pending weapon up
    // from the bottom of the screen.
    // Uses player
    //
    public void BringUpWeapon ()
    {
        statenum_t  newstate=statenum_t.S_NULL;
        
        if (pendingweapon == weapontype_t.wp_nochange)
        pendingweapon = readyweapon;
            
        if (pendingweapon == weapontype_t.wp_chainsaw)
        // TODO: S_StartSound (mo, sfxenum_t.sfx_sawup);
            
        newstate = weaponinfo[pendingweapon.ordinal()].upstate;

        pendingweapon = weapontype_t.wp_nochange;
        psprites[ps_weapon].sy = WEAPONBOTTOM;

        this.SetPsprite (player, ps_weapon, newstate);
    }

    //
    // P_CheckAmmo
    // Returns true if there is enough ammo to shoot.
    // If not, selects the next weapon to use.
    //
    public boolean CheckAmmo ()
    {
        ammotype_t      ammo;
        int         count;

        ammo = weaponinfo[readyweapon.ordinal()].ammo;

        // Minimal amount for one shot varies.
        if (readyweapon == weapontype_t.wp_bfg)
        count = BFGCELLS;
        else if (readyweapon == weapontype_t.wp_supershotgun)
        count = 2;  // Double barrel.
        else
        count = 1;  // Regular.

        // Some do not need ammunition anyway.
        // Return if current ammunition sufficient.
        if (ammo == ammotype_t.am_noammo || this.ammo[ammo.ordinal()] >= count)
        return true;
            
        // Out of ammo, pick a weapon to change to.
        // Preferences are set here.
        do
        {
        if (weaponowned[weapontype_t.wp_plasma.ordinal()]
            && (this.ammo[ammotype_t.am_cell.ordinal()]!=0)
            && (DS.gamemode != GameMode_t.shareware) )
        {
            pendingweapon = weapontype_t.wp_plasma;
        }
        else if (weaponowned[weapontype_t.wp_supershotgun.ordinal()] 
             && this.ammo[ammotype_t.am_shell.ordinal()]>2
             && (DS.gamemode == GameMode_t.commercial) )
        {
            pendingweapon = weapontype_t.wp_supershotgun;
        }
        else if (weaponowned[weapontype_t.wp_chaingun.ordinal()]
             && this.ammo[ammotype_t.am_clip.ordinal()]!=0)
        {
            pendingweapon = weapontype_t.wp_chaingun;
        }
        else if (weaponowned[weapontype_t.wp_shotgun.ordinal()]
             && this.ammo[ammotype_t.am_shell.ordinal()]!=0)
        {
            pendingweapon = weapontype_t.wp_shotgun;
        }
        else if (this.ammo[ammotype_t.am_clip.ordinal()]!=0)
        {
            pendingweapon = weapontype_t.wp_pistol;
        }
        else if (weaponowned[weapontype_t.wp_chainsaw.ordinal()])
        {
            pendingweapon = weapontype_t.wp_chainsaw;
        }
        else if (weaponowned[weapontype_t.wp_missile.ordinal()]
             && this.ammo[ammotype_t.am_misl.ordinal()]!=0)
        {
            pendingweapon = weapontype_t.wp_missile;
        }
        else if (weaponowned[weapontype_t.wp_bfg.ordinal()]
             && this.ammo[ammotype_t.am_cell.ordinal()]>40
             && (DS.gamemode != GameMode_t.shareware) )
        {
            pendingweapon = weapontype_t.wp_bfg;
        }
        else
        {
            // If everything fails.
            pendingweapon = weapontype_t.wp_fist;
        }
        
        } while (pendingweapon == weapontype_t.wp_nochange);

        // Now set appropriate weapon overlay.
        this.SetPsprite (
              ps_weapon,
              weaponinfo[readyweapon.ordinal()].downstate);

        return false;   
    }


    //
    // P_DropWeapon
    // Player died, so put the weapon away.
    //
    public void DropWeapon (player_t player)
    {
        SetPsprite (player,
              ps_weapon,
              weaponinfo[readyweapon.ordinal()].downstate);
    }

    //
    // A_WeaponReady
    // The player can fire the weapon
    // or change to another weapon at this time.
    // Follows after getting weapon up,
    // or after previous attack/fire sequence.
    //
    
    public void A_WeaponReady( player_t  player,
      pspdef_t  psp )
    {   
        statenum_t  newstate;
        int     angle;
        
        // get out of attack state
        if (mo.state == states[statenum_t.S_PLAY_ATK1.ordinal()]
        || mo.state == states[statenum_t.S_PLAY_ATK2.ordinal()] )
        {
        mo.SetMobjState (statenum_t.S_PLAY);
        }
        
        if (readyweapon == weapontype_t.wp_chainsaw
        && psp.state == states[statenum_t.S_SAW.ordinal()])
        {
        ; //TODO: S_StartSound (mo, sfx_sawidl);
        }
        
        // check for change
        //  if player is dead, put the weapon away
        if (pendingweapon != weapontype_t.wp_nochange || health[0]==0)
        {
        // change weapon
        //  (pending weapon should allready be validated)
        newstate = weaponinfo[readyweapon.ordinal()].downstate;
        SetPsprite (ps_weapon, newstate);
        return; 
        }
        
        // check for fire
        //  the missile launcher and bfg do not auto fire
        if (flags(cmd.buttons , BT_ATTACK))
        {
        if ( !attackdown
             || (readyweapon != weapontype_t.wp_missile
             && readyweapon != weapontype_t.wp_bfg) )
        {
            attackdown = true;
            FireWeapon ();      
            return;
        }
        }
        else
        attackdown = false;
        
        // bob the weapon based on movement speed
        angle = (128*DS.leveltime)&FINEMASK;
        psp.sx = FRACUNIT + FixedMul (bob, finecosine[angle]);
        angle &= FINEANGLES/2-1;
        psp.sy = WEAPONTOP + FixedMul (bob, finesine[angle]);
    }

    //
    // A_ReFire
    // The player can re-fire the weapon
    // without lowering it entirely.
    //
public void A_ReFire(pspdef_t psp )
    {
        
        // check for fire
        //  (if a weaponchange is pending, let it go through instead)
        if ( flags(cmd.buttons , BT_ATTACK) 
         && pendingweapon == weapontype_t.wp_nochange
         && health[0]!=0)
        {
        refire++;
        FireWeapon ();
        }
        else
        {
        refire = 0;
        CheckAmmo ();
        }
    }


//
// P_FireWeapon.
//
public void FireWeapon ()
{
    statenum_t  newstate;
    
    if (!this.CheckAmmo ())
    return;
    
    this.mo.SetMobjState (statenum_t.S_PLAY_ATK1);
    newstate = weaponinfo[readyweapon.ordinal()].atkstate;
    this.SetPsprite (ps_weapon, newstate);
    NoiseAlert (player.mo, player.mo);
}

public void A_CheckReload
    (  pspdef_t psp )
    {
        CheckAmmo ();
    /*  ????
        if (ammo[am_shell]<2)
        P_SetPsprite (player, ps_weapon, S_DSNR1);
    */
    }



    //
    // A_Lower
    // Lowers current weapon,
    //  and changes weapon at bottom.
    //
    void
    A_Lower
    (
      pspdef_t psp )
    {   
        psp.sy += LOWERSPEED;

        // Is already down.
        if (psp.sy < WEAPONBOTTOM )
        return;

        // Player is dead.
        if (playerstate == PST_DEAD)
        {
        psp.sy = WEAPONBOTTOM;

        // don't bring weapon back up
        return;     
        }
        
        // The old weapon has been lowered off the screen,
        // so change the weapon and start raising it
        if (!(health[0]==0))
        {
        // Player is dead, so keep the weapon off screen.
        this.SetPsprite(ps_weapon, statenum_t.S_NULL);
        return; 
        }
        
        readyweapon = pendingweapon; 

        BringUpWeapon ();
    }


    //
    // A_Raise
    //
    void
    A_Raise
    ( 
      pspdef_t* psp )
    {
        statenum_t  newstate;
        
        psp.sy -= RAISESPEED;

        if (psp.sy > WEAPONTOP )
        return;
        
        psp.sy = WEAPONTOP;
        
        // The weapon has been raised all the way,
        //  so change to the ready state.
        newstate = weaponinfo[readyweapon].readystate;

        P_SetPsprite (player, ps_weapon, newstate);
    }



    //
    // A_GunFlash
    //
    void
    A_GunFlash
    ( 
      pspdef_t* psp ) 
    {
        P_SetMobjState (mo, S_PLAY_ATK2);
        P_SetPsprite (player,ps_flash,weaponinfo[readyweapon].flashstate);
    }



    //
    // WEAPON ATTACKS
    //


    //
    // A_Punch
    //
    void
    A_Punch
    ( 
      pspdef_t* psp ) 
    {
        angle_t angle;
        int     damage;
        int     slope;
        
        damage = (P_Random ()%10+1)<<1;

        if (powers[pw_strength]) 
        damage *= 10;

        angle = mo.angle;
        angle += (P_Random()-P_Random())<<18;
        slope = P_AimLineAttack (mo, angle, MELEERANGE);
        P_LineAttack (mo, angle, MELEERANGE, slope, damage);

        // turn to face target
        if (linetarget)
        {
        S_StartSound (mo, sfx_punch);
        mo.angle = R_PointToAngle2 (mo.x,
                             mo.y,
                             linetarget.x,
                             linetarget.y);
        }
    }


    //
    // A_Saw
    //
    void
    A_Saw
    ( 
      pspdef_t* psp ) 
    {
        angle_t angle;
        int     damage;
        int     slope;

        damage = 2*(P_Random ()%10+1);
        angle = mo.angle;
        angle += (P_Random()-P_Random())<<18;
        
        // use meleerange + 1 se the puff doesn't skip the flash
        slope = P_AimLineAttack (mo, angle, MELEERANGE+1);
        P_LineAttack (mo, angle, MELEERANGE+1, slope, damage);

        if (!linetarget)
        {
        S_StartSound (mo, sfx_sawful);
        return;
        }
        S_StartSound (mo, sfx_sawhit);
        
        // turn to face target
        angle = R_PointToAngle2 (mo.x, mo.y,
                     linetarget.x, linetarget.y);
        if (angle - mo.angle > ANG180)
        {
        if (angle - mo.angle < -ANG90/20)
            mo.angle = angle + ANG90/21;
        else
            mo.angle -= ANG90/20;
        }
        else
        {
        if (angle - mo.angle > ANG90/20)
            mo.angle = angle - ANG90/21;
        else
            mo.angle += ANG90/20;
        }
        mo.flags |= MF_JUSTATTACKED;
    }



    //
    // A_FireMissile
    //
    void
    A_FireMissile
    ( 
      pspdef_t* psp ) 
    {
        ammo[weaponinfo[readyweapon].ammo]--;
        P_SpawnPlayerMissile (mo, MT_ROCKET);
    }


    //
    // A_FireBFG
    //
    void
    A_FireBFG
    ( 
      pspdef_t* psp ) 
    {
        ammo[weaponinfo[readyweapon].ammo] -= BFGCELLS;
        P_SpawnPlayerMissile (mo, MT_BFG);
    }



    //
    // A_FirePlasma
    //
    void
    A_FirePlasma
    ( 
      pspdef_t* psp ) 
    {
        ammo[weaponinfo[readyweapon].ammo]--;

        P_SetPsprite (player,
              ps_flash,
              weaponinfo[readyweapon].flashstate+(P_Random ()&1) );

        P_SpawnPlayerMissile (mo, MT_PLASMA);
    }



    //
    // P_BulletSlope
    // Sets a slope so a near miss is at aproximately
    // the height of the intended target
    //
    fixed_t     bulletslope;


    void P_BulletSlope (mobj_t* mo)
    {
        angle_t an;
        
        // see which target is to be aimed at
        an = mo.angle;
        bulletslope = P_AimLineAttack (mo, an, 16*64*FRACUNIT);

        if (!linetarget)
        {
        an += 1<<26;
        bulletslope = P_AimLineAttack (mo, an, 16*64*FRACUNIT);
        if (!linetarget)
        {
            an -= 2<<26;
            bulletslope = P_AimLineAttack (mo, an, 16*64*FRACUNIT);
        }
        }
    }


    //
    // P_GunShot
    //
    void
    P_GunShot
    ( mobj_t*   mo,
      boolean   accurate )
    {
        angle_t angle;
        int     damage;
        
        damage = 5*(P_Random ()%3+1);
        angle = mo.angle;

        if (!accurate)
        angle += (P_Random()-P_Random())<<18;

        P_LineAttack (mo, angle, MISSILERANGE, bulletslope, damage);
    }


    //
    // FirePistol
    //
    void
    A_FirePistol
    ( 
      pspdef_t* psp ) 
    {
        S_StartSound (mo, sfx_pistol);

        P_SetMobjState (mo, S_PLAY_ATK2);
        ammo[weaponinfo[readyweapon].ammo]--;

        P_SetPsprite (player,
              ps_flash,
              weaponinfo[readyweapon].flashstate);

        P_BulletSlope (mo);
        P_GunShot (mo, !refire);
    }


    //
    // FireShotgun
    //
    void
    FireShotgun
    ( 
      pspdef_t* psp ) 
    {
        int     i;
        
        S_StartSound (mo, sfx_shotgn);
        P_SetMobjState (mo, S_PLAY_ATK2);

        ammo[weaponinfo[readyweapon].ammo]--;

        P_SetPsprite (player,
              ps_flash,
              weaponinfo[readyweapon].flashstate);

        P_BulletSlope (mo);
        
        for (i=0 ; i<7 ; i++)
        P_GunShot (mo, false);
    }



    //
    // FireShotgun2
    //
    void
    FireShotgun2
    ( 
      pspdef_t* psp ) 
    {
        int     i;
        angle_t angle;
        int     damage;
            
        
        S_StartSound (mo, sfx_dshtgn);
        P_SetMobjState (mo, S_PLAY_ATK2);

        ammo[weaponinfo[readyweapon].ammo]-=2;

        P_SetPsprite (player,
              ps_flash,
              weaponinfo[readyweapon].flashstate);

        P_BulletSlope (mo);
        
        for (i=0 ; i<20 ; i++)
        {
        damage = 5*(P_Random ()%3+1);
        angle = mo.angle;
        angle += (P_Random()-P_Random())<<19;
        P_LineAttack (mo,
                  angle,
                  MISSILERANGE,
                  bulletslope + ((P_Random()-P_Random())<<5), damage);
        }
    }


    //
    // FireCGun
    //
    void
    FireCGun
    ( 
      pspdef_t* psp ) 
    {
        S_StartSound (mo, sfx_pistol);

        if (!ammo[weaponinfo[readyweapon].ammo])
        return;
            
        P_SetMobjState (mo, S_PLAY_ATK2);
        ammo[weaponinfo[readyweapon].ammo]--;

        P_SetPsprite (player,
              ps_flash,
              weaponinfo[readyweapon].flashstate
              + psp.state
              - &states[S_CHAIN1] );

        P_BulletSlope (mo);
        
        P_GunShot (mo, !refire);
    }



    //
    // ?
    //
    void Light0 (player_t *player, pspdef_t *psp)
    {
        extralight = 0;
    }

    void Light1 (player_t *player, pspdef_t *psp)
    {
        extralight = 1;
    }

    void Light2 (player_t *player, pspdef_t *psp)
    {
        extralight = 2;
    }


    //
    // BFGSpray
    // Spawn a BFG explosion on every monster in view
    //
    void BFGSpray (mobj_t* mo) 
    {
        int         i;
        int         j;
        int         damage;
        angle_t     an;
        
        // offset angles from its attack angle
        for (i=0 ; i<40 ; i++)
        {
        an = mo.angle - ANG90/2 + ANG90/40*i;

        // mo.target is the originator (player)
        //  of the missile
        P_AimLineAttack (mo.target, an, 16*64*FRACUNIT);

        if (!linetarget)
            continue;

        P_SpawnMobj (linetarget.x,
                 linetarget.y,
                 linetarget.z + (linetarget.height>>2),
                 MT_EXTRABFG);
        
        damage = 0;
        for (j=0;j<15;j++)
            damage += (P_Random()&7) + 1;

        P_DamageMobj (linetarget, mo.target,mo.target, damage);
        }
    }


    //
    // BFGsound
    //
    void
    BFGsound
    ( 
      pspdef_t psp )
    {
        S_StartSound (mo, sfx_bfg);
    }



    /**
     * P_SetupPsprites
     * Called at start of level for each 
     */
    
    public void SetupPsprites () 
    {
        int i;
        
        // remove all psprites
        for (i=0 ; i<NUMPSPRITES ; i++)
        psprites[i].state = null;
            
        // spawn the gun
        pendingweapon = readyweapon;
        BringUpWeapon (this);
    }




   
    
}
