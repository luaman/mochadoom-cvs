package doom;

import p.mobj_t;
import m.fixed_t;
import static data.Defines.*;


/**
 * Extended player object info: player_t
*
*The player data structure depends on a number
*of other structs: items (internal inventory),
*animation states (closely tied to the sprites
*used to represent them, unfortunately).
*#include "d_items.h"
*#include "p_pspr.h"

*In addition, the player is just a special
*case of the generic moving object/actor.
*#include "p_mobj.h"
*Finally, for odd reasons, the player input
*is buffered within the player data struct,
*as commands per game tick.
*#include "d_ticcmd.h"
 */


public class player_t extends mobj_t{

    public final static int CF_NOCLIP=1; // No damage, no health loss.
    public final static int CF_GODMODE=2;
    public final static int CF_NOMOMENTUM=4;   // Not really a cheat, just a debug aid.

     public mobj_t     mo;
     public playerstate_t   playerstate;
     public ticcmd_t        cmd;

     // Determine POV,
     //  including viewpoint bobbing during movement.
     // Focal origin above r.z
     public fixed_t     viewz;
     // Base height above floor for viewz.
     public fixed_t     viewheight;
     // Bob/squat speed.
     public fixed_t             deltaviewheight;
     // bounded/scaled total momentum.
     public fixed_t             bob;    

     // This is only used between levels,
     // mo->health is used during levels.
     public int         health; 
     public int         armorpoints;
     // Armor type is 0-2.
     public int         armortype;  

     // Power ups. invinc and invis are tic counters.
     public int[]         powers=new int[powertype_t.NUMPOWERS.ordinal()];
     public boolean[]     cards=new boolean[card_t.NUMCARDS.ordinal()];
     public boolean     backpack;
     
     // Frags, kills of other players.
     public int[] frags=new int[MAXPLAYERS];
     public weapontype_t    readyweapon;
     
     // Is wp_nochange if not changing.
     public weapontype_t    pendingweapon;

     public boolean[]     weaponowned=new boolean[NUMWEAPONS];
     public int[]         ammo=new int[NUMAMMO];
     public int[]         maxammo=new int[NUMAMMO];

     // True if button down last tic.
     public int         attackdown;
     public int         usedown;

     // Bit flags, for cheats and debug.
     // See cheat_t, above.
     public int         cheats;     

     // Refired shots are less accurate.
     public int         refire;     

      // For intermission stats.
     public int         killcount;
     public int         itemcount;
     public int         secretcount;

     // Hint messages.
     public String      message;    
     
     // For screen flashing (red or bright).
     public int         damagecount;
     public int         bonuscount;

     // Who did damage (NULL for floors/ceilings).
     public mobj_t     attacker;
     
     // So gun flashes light up areas.
     public  int         extralight;

     // Current PLAYPAL, ???
     //  can be set to REDCOLORMAP for pain, etc.
     public int         fixedcolormap;

     // Player skin colorshift,
     //  0-3 for which color to draw player.
     public int         colormap;   

     // TODO: Overlay view sprites (gun, etc).
     // pspdef_t[] psprites=new pspdef_t[NUMPSPRITES];

     // True if secret level has been done.
     public boolean     didsecret;  

 }
