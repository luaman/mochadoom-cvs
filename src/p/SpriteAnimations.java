package p;

import static m.fixed_t.*;
import static data.Tables.*;
import static data.SineCosine.*;
import static doom.items.*;
import static data.info.*;
import data.doomstat;
import data.state_t;
import data.Defines.GameMode_t;
import data.Defines.ammotype_t;
import data.Defines.statenum_t;
import data.sounds.sfxenum_t;
import doom.acp2;
import doom.actionf_t;
import doom.player_t;
import doom.weapontype_t;

public class SpriteAnimations {

	private doomstat DS;
	
	//
	// Overlay psprites are scaled shapes
	// drawn directly on the view screen,
	// coordinates are given for a 320*200 view screen.
	//
	
	/* psprnum_t enum */
	public static int ps_weapon=0,
	    ps_flash=1,
	    NUMPSPRITES=2;	
	
	private static int  LOWERSPEED	=	FRACUNIT*6;
	private static int  RAISESPEED	=	FRACUNIT*6;

	private static int  WEAPONBOTTOM	=128*FRACUNIT;
	private static int  WEAPONTOP		=32*FRACUNIT;


	// plasma cells for a bfg attack
	private static int  BFGCELLS		=40;		


	/**
	/* P_SetPsprite
	*/
	
	public void
	SetPsprite
	( player_t	player,
	  int		position,
	  statenum_t	newstate ) 
	{
	    pspdef_t	psp;
	    state_t	state;
		
	    psp = player.psprites[position];
		
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
		psp.tics = (int) state.tics;	// could be 0

		if (state.misc1!=0)
		{
		    // coordinate set
		    psp.sx = (int) (state.misc1 << FRACBITS);
		    psp.sy = (int) (state.misc2 << FRACBITS);
		}
		
		// Call action routine.
		// Modified handling.
		if (state.action.getType()==actionf_t.acp2)
		{
		    ((acp2)(state.action)).invoke(player, psp);
		    if (psp.state==null)
			break;
		}
		
		newstate = psp.state.nextstate;
		
	    } while (psp.tics==0);
	    // an initial state of 0 could cycle through
	}
	
	/** fixed_t */
	int		swingx,	swingy;

	/**P_CalcSwing
	 * 
	 * @param player
	 */
	public void P_CalcSwing (player_t	player)
	{
	    int	swing; // fixed_t
	    int		angle;
		
	    // OPTIMIZE: tablify this.
	    // A LUT would allow for different modes,
	    //  and add flexibility.

	    swing = player.bob;

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
	public void BringUpWeapon (player_t player)
	{
		statenum_t	newstate=statenum_t.S_NULL;
		
	    if (player.pendingweapon == weapontype_t.wp_nochange)
		player.pendingweapon = player.readyweapon;
			
	    if (player.pendingweapon == weapontype_t.wp_chainsaw)
		// TODO: S_StartSound (player.mo, sfxenum_t.sfx_sawup);
			
	    newstate = weaponinfo[player.pendingweapon.ordinal()].upstate;

	    player.pendingweapon = weapontype_t.wp_nochange;
	    player.psprites[ps_weapon].sy = WEAPONBOTTOM;

	    this.SetPsprite (player, ps_weapon, newstate);
	}

	//
	// P_CheckAmmo
	// Returns true if there is enough ammo to shoot.
	// If not, selects the next weapon to use.
	//
	public boolean CheckAmmo (player_t player)
	{
	    ammotype_t		ammo;
	    int			count;

	    ammo = weaponinfo[player.readyweapon.ordinal()].ammo;

	    // Minimal amount for one shot varies.
	    if (player.readyweapon == weapontype_t.wp_bfg)
		count = BFGCELLS;
	    else if (player.readyweapon == weapontype_t.wp_supershotgun)
		count = 2;	// Double barrel.
	    else
		count = 1;	// Regular.

	    // Some do not need ammunition anyway.
	    // Return if current ammunition sufficient.
	    if (ammo == ammotype_t.am_noammo || player.ammo[ammo.ordinal()] >= count)
		return true;
			
	    // Out of ammo, pick a weapon to change to.
	    // Preferences are set here.
	    do
	    {
		if (player.weaponowned[weapontype_t.wp_plasma.ordinal()]
		    && (player.ammo[ammotype_t.am_cell.ordinal()]!=0)
		    && (DS.gamemode != GameMode_t.shareware) )
		{
		    player.pendingweapon = weapontype_t.wp_plasma;
		}
		else if (player.weaponowned[weapontype_t.wp_supershotgun.ordinal()] 
			 && player.ammo[ammotype_t.am_shell.ordinal()]>2
			 && (DS.gamemode == GameMode_t.commercial) )
		{
		    player.pendingweapon = weapontype_t.wp_supershotgun;
		}
		else if (player.weaponowned[weapontype_t.wp_chaingun.ordinal()]
			 && player.ammo[ammotype_t.am_clip.ordinal()]!=0)
		{
		    player.pendingweapon = weapontype_t.wp_chaingun;
		}
		else if (player.weaponowned[weapontype_t.wp_shotgun.ordinal()]
			 && player.ammo[ammotype_t.am_shell.ordinal()]!=0)
		{
		    player.pendingweapon = weapontype_t.wp_shotgun;
		}
		else if (player.ammo[ammotype_t.am_clip.ordinal()]!=0)
		{
		    player.pendingweapon = weapontype_t.wp_pistol;
		}
		else if (player.weaponowned[weapontype_t.wp_chainsaw.ordinal()])
		{
		    player.pendingweapon = weapontype_t.wp_chainsaw;
		}
		else if (player.weaponowned[weapontype_t.wp_missile.ordinal()]
			 && player.ammo[ammotype_t.am_misl.ordinal()]!=0)
		{
		    player.pendingweapon = weapontype_t.wp_missile;
		}
		else if (player.weaponowned[weapontype_t.wp_bfg.ordinal()]
			 && player.ammo[ammotype_t.am_cell.ordinal()]>40
			 && (DS.gamemode != GameMode_t.shareware) )
		{
		    player.pendingweapon = weapontype_t.wp_bfg;
		}
		else
		{
		    // If everything fails.
		    player.pendingweapon = weapontype_t.wp_fist;
		}
		
	    } while (player.pendingweapon == weapontype_t.wp_nochange);

	    // Now set appropriate weapon overlay.
	    this.SetPsprite (player,
			  ps_weapon,
			  weaponinfo[player.readyweapon.ordinal()].downstate);

	    return false;	
	}


	//
	// P_FireWeapon.
	//
	public void FireWeapon (player_t player)
	{
	    statenum_t	newstate;
		
	    if (!this.CheckAmmo (player))
		return;
		
	    SetMobjState (player.mo, statenum_t.S_PLAY_ATK1);
	    newstate = weaponinfo[player.readyweapon.ordinal()].atkstate;
	    SetPsprite (player, ps_weapon, newstate);
	    NoiseAlert (player.mo, player.mo);
	}



	//
	// P_DropWeapon
	// Player died, so put the weapon away.
	//
	public void DropWeapon (player_t player)
	{
	    SetPsprite (player,
			  ps_weapon,
			  weaponinfo[player.readyweapon.ordinal()].downstate);
	}



	//
	// A_WeaponReady
	// The player can fire the weapon
	// or change to another weapon at this time.
	// Follows after getting weapon up,
	// or after previous attack/fire sequence.
	//
	
class ActionWeaponReady implements think_t {	
	
	public void
	A_WeaponReady
	( player_t	player,
	  pspdef_t	psp )
	{	
	    statenum_t	newstate;
	    int		angle;
	    
	    // get out of attack state
	    if (player.mo.state == states[S_PLAY_ATK1]
		|| player.mo.state == states[S_PLAY_ATK2] )
	    {
		P_SetMobjState (player.mo, S_PLAY);
	    }
	    
	    if (player.readyweapon == wp_chainsaw
		&& psp.state == states[S_SAW])
	    {
		S_StartSound (player.mo, sfx_sawidl);
	    }
	    
	    // check for change
	    //  if player is dead, put the weapon away
	    if (player.pendingweapon != wp_nochange || !player.health)
	    {
		// change weapon
		//  (pending weapon should allready be validated)
		newstate = weaponinfo[player.readyweapon].downstate;
		P_SetPsprite (player, ps_weapon, newstate);
		return;	
	    }
	    
	    // check for fire
	    //  the missile launcher and bfg do not auto fire
	    if (player.cmd.buttons & BT_ATTACK)
	    {
		if ( !player.attackdown
		     || (player.readyweapon != wp_missile
			 && player.readyweapon != wp_bfg) )
		{
		    player.attackdown = true;
		    P_FireWeapon (player);		
		    return;
		}
	    }
	    else
		player.attackdown = false;
	    
	    // bob the weapon based on movement speed
	    angle = (128*leveltime)&FINEMASK;
	    psp.sx = FRACUNIT + FixedMul (player.bob, finecosine[angle]);
	    angle &= FINEANGLES/2-1;
	    psp.sy = WEAPONTOP + FixedMul (player.bob, finesine[angle]);
	}

}

	//
	// A_ReFire
	// The player can re-fire the weapon
	// without lowering it entirely.
	//
/*
void A_ReFire
	( player_t*	player,
	  pspdef_t*	psp )
	{
	    
	    // check for fire
	    //  (if a weaponchange is pending, let it go through instead)
	    if ( (player.cmd.buttons & BT_ATTACK) 
		 && player.pendingweapon == wp_nochange
		 && player.health)
	    {
		player.refire++;
		P_FireWeapon (player);
	    }
	    else
	    {
		player.refire = 0;
		P_CheckAmmo (player);
	    }
	}

	/*

	void
	A_CheckReload
	( player_t*	player,
	  pspdef_t*	psp )
	{
	    P_CheckAmmo (player);
	#if 0
	    if (player.ammo[am_shell]<2)
		P_SetPsprite (player, ps_weapon, S_DSNR1);
	#endif
	}



	//
	// A_Lower
	// Lowers current weapon,
	//  and changes weapon at bottom.
	//
	void
	A_Lower
	( player_t*	player,
	  pspdef_t*	psp )
	{	
	    psp.sy += LOWERSPEED;

	    // Is already down.
	    if (psp.sy < WEAPONBOTTOM )
		return;

	    // Player is dead.
	    if (player.playerstate == PST_DEAD)
	    {
		psp.sy = WEAPONBOTTOM;

		// don't bring weapon back up
		return;		
	    }
	    
	    // The old weapon has been lowered off the screen,
	    // so change the weapon and start raising it
	    if (!player.health)
	    {
		// Player is dead, so keep the weapon off screen.
		P_SetPsprite (player,  ps_weapon, S_NULL);
		return;	
	    }
		
	    player.readyweapon = player.pendingweapon; 

	    P_BringUpWeapon (player);
	}


	//
	// A_Raise
	//
	void
	A_Raise
	( player_t*	player,
	  pspdef_t*	psp )
	{
	    statenum_t	newstate;
		
	    psp.sy -= RAISESPEED;

	    if (psp.sy > WEAPONTOP )
		return;
	    
	    psp.sy = WEAPONTOP;
	    
	    // The weapon has been raised all the way,
	    //  so change to the ready state.
	    newstate = weaponinfo[player.readyweapon].readystate;

	    P_SetPsprite (player, ps_weapon, newstate);
	}



	//
	// A_GunFlash
	//
	void
	A_GunFlash
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    P_SetMobjState (player.mo, S_PLAY_ATK2);
	    P_SetPsprite (player,ps_flash,weaponinfo[player.readyweapon].flashstate);
	}



	//
	// WEAPON ATTACKS
	//


	//
	// A_Punch
	//
	void
	A_Punch
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    angle_t	angle;
	    int		damage;
	    int		slope;
		
	    damage = (P_Random ()%10+1)<<1;

	    if (player.powers[pw_strength])	
		damage *= 10;

	    angle = player.mo.angle;
	    angle += (P_Random()-P_Random())<<18;
	    slope = P_AimLineAttack (player.mo, angle, MELEERANGE);
	    P_LineAttack (player.mo, angle, MELEERANGE, slope, damage);

	    // turn to face target
	    if (linetarget)
	    {
		S_StartSound (player.mo, sfx_punch);
		player.mo.angle = R_PointToAngle2 (player.mo.x,
						     player.mo.y,
						     linetarget.x,
						     linetarget.y);
	    }
	}


	//
	// A_Saw
	//
	void
	A_Saw
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    angle_t	angle;
	    int		damage;
	    int		slope;

	    damage = 2*(P_Random ()%10+1);
	    angle = player.mo.angle;
	    angle += (P_Random()-P_Random())<<18;
	    
	    // use meleerange + 1 se the puff doesn't skip the flash
	    slope = P_AimLineAttack (player.mo, angle, MELEERANGE+1);
	    P_LineAttack (player.mo, angle, MELEERANGE+1, slope, damage);

	    if (!linetarget)
	    {
		S_StartSound (player.mo, sfx_sawful);
		return;
	    }
	    S_StartSound (player.mo, sfx_sawhit);
		
	    // turn to face target
	    angle = R_PointToAngle2 (player.mo.x, player.mo.y,
				     linetarget.x, linetarget.y);
	    if (angle - player.mo.angle > ANG180)
	    {
		if (angle - player.mo.angle < -ANG90/20)
		    player.mo.angle = angle + ANG90/21;
		else
		    player.mo.angle -= ANG90/20;
	    }
	    else
	    {
		if (angle - player.mo.angle > ANG90/20)
		    player.mo.angle = angle - ANG90/21;
		else
		    player.mo.angle += ANG90/20;
	    }
	    player.mo.flags |= MF_JUSTATTACKED;
	}



	//
	// A_FireMissile
	//
	void
	A_FireMissile
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    player.ammo[weaponinfo[player.readyweapon].ammo]--;
	    P_SpawnPlayerMissile (player.mo, MT_ROCKET);
	}


	//
	// A_FireBFG
	//
	void
	A_FireBFG
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    player.ammo[weaponinfo[player.readyweapon].ammo] -= BFGCELLS;
	    P_SpawnPlayerMissile (player.mo, MT_BFG);
	}



	//
	// A_FirePlasma
	//
	void
	A_FirePlasma
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    player.ammo[weaponinfo[player.readyweapon].ammo]--;

	    P_SetPsprite (player,
			  ps_flash,
			  weaponinfo[player.readyweapon].flashstate+(P_Random ()&1) );

	    P_SpawnPlayerMissile (player.mo, MT_PLASMA);
	}



	//
	// P_BulletSlope
	// Sets a slope so a near miss is at aproximately
	// the height of the intended target
	//
	fixed_t		bulletslope;


	void P_BulletSlope (mobj_t*	mo)
	{
	    angle_t	an;
	    
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
	( mobj_t*	mo,
	  boolean	accurate )
	{
	    angle_t	angle;
	    int		damage;
		
	    damage = 5*(P_Random ()%3+1);
	    angle = mo.angle;

	    if (!accurate)
		angle += (P_Random()-P_Random())<<18;

	    P_LineAttack (mo, angle, MISSILERANGE, bulletslope, damage);
	}


	//
	// A_FirePistol
	//
	void
	A_FirePistol
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    S_StartSound (player.mo, sfx_pistol);

	    P_SetMobjState (player.mo, S_PLAY_ATK2);
	    player.ammo[weaponinfo[player.readyweapon].ammo]--;

	    P_SetPsprite (player,
			  ps_flash,
			  weaponinfo[player.readyweapon].flashstate);

	    P_BulletSlope (player.mo);
	    P_GunShot (player.mo, !player.refire);
	}


	//
	// A_FireShotgun
	//
	void
	A_FireShotgun
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    int		i;
		
	    S_StartSound (player.mo, sfx_shotgn);
	    P_SetMobjState (player.mo, S_PLAY_ATK2);

	    player.ammo[weaponinfo[player.readyweapon].ammo]--;

	    P_SetPsprite (player,
			  ps_flash,
			  weaponinfo[player.readyweapon].flashstate);

	    P_BulletSlope (player.mo);
		
	    for (i=0 ; i<7 ; i++)
		P_GunShot (player.mo, false);
	}



	//
	// A_FireShotgun2
	//
	void
	A_FireShotgun2
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    int		i;
	    angle_t	angle;
	    int		damage;
			
		
	    S_StartSound (player.mo, sfx_dshtgn);
	    P_SetMobjState (player.mo, S_PLAY_ATK2);

	    player.ammo[weaponinfo[player.readyweapon].ammo]-=2;

	    P_SetPsprite (player,
			  ps_flash,
			  weaponinfo[player.readyweapon].flashstate);

	    P_BulletSlope (player.mo);
		
	    for (i=0 ; i<20 ; i++)
	    {
		damage = 5*(P_Random ()%3+1);
		angle = player.mo.angle;
		angle += (P_Random()-P_Random())<<19;
		P_LineAttack (player.mo,
			      angle,
			      MISSILERANGE,
			      bulletslope + ((P_Random()-P_Random())<<5), damage);
	    }
	}


	//
	// A_FireCGun
	//
	void
	A_FireCGun
	( player_t*	player,
	  pspdef_t*	psp ) 
	{
	    S_StartSound (player.mo, sfx_pistol);

	    if (!player.ammo[weaponinfo[player.readyweapon].ammo])
		return;
			
	    P_SetMobjState (player.mo, S_PLAY_ATK2);
	    player.ammo[weaponinfo[player.readyweapon].ammo]--;

	    P_SetPsprite (player,
			  ps_flash,
			  weaponinfo[player.readyweapon].flashstate
			  + psp.state
			  - &states[S_CHAIN1] );

	    P_BulletSlope (player.mo);
		
	    P_GunShot (player.mo, !player.refire);
	}



	//
	// ?
	//
	void A_Light0 (player_t *player, pspdef_t *psp)
	{
	    player.extralight = 0;
	}

	void A_Light1 (player_t *player, pspdef_t *psp)
	{
	    player.extralight = 1;
	}

	void A_Light2 (player_t *player, pspdef_t *psp)
	{
	    player.extralight = 2;
	}


	//
	// A_BFGSpray
	// Spawn a BFG explosion on every monster in view
	//
	void A_BFGSpray (mobj_t* mo) 
	{
	    int			i;
	    int			j;
	    int			damage;
	    angle_t		an;
		
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
	// A_BFGsound
	//
	void
	A_BFGsound
	( player_t*	player,
	  pspdef_t*	psp )
	{
	    S_StartSound (player.mo, sfx_bfg);
	}



	//
	// P_SetupPsprites
	// Called at start of level for each player.
	//
	void P_SetupPsprites (player_t* player) 
	{
	    int	i;
		
	    // remove all psprites
	    for (i=0 ; i<NUMPSPRITES ; i++)
		player.psprites[i].state = NULL;
			
	    // spawn the gun
	    player.pendingweapon = player.readyweapon;
	    P_BringUpWeapon (player);
	}




	//
	// P_MovePsprites
	// Called every tic by player thinking routine.
	//
	void P_MovePsprites (player_t player) 
	{
	    int		i;
	    pspdef_t	psp;
	    state_t	state;
		
	    psp = player.psprites[0];
	    for (i=0 ; i<NUMPSPRITES ; i++, psp++)
	    {
		// a null state means not active
		if ( (state = psp.state) )	
		{
		    // drop tic count and possibly change state

		    // a -1 tic count never changes
		    if (psp.tics != -1)	
		    {
			psp.tics--;
			if (!psp.tics)
			    P_SetPsprite (player, i, psp.state.nextstate);
		    }				
		}
	    }
	    
	    player.psprites[ps_flash].sx = player.psprites[ps_weapon].sx;
	    player.psprites[ps_flash].sy = player.psprites[ps_weapon].sy;
	}


*/
	
}
