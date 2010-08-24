package p;

import static data.Defines.*;
import static doom.items.*;
import static p.mobj.*;
import static m.fixed_t.*;
import static doom.englsh.*;
import m.random;
import automap.DoomAutoMap;
import rr.Renderer;
import st.DoomStatusBarInterface;
import v.DoomVideoRenderer;
import w.WadLoader;
import i.system;
import data.doomstat;
import data.mobjtype_t;
import data.sounds.sfxenum_t;
import doom.player_t;
import doom.weapontype_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Interaction.java,v 1.1 2010/08/24 14:57:42 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// $Log: Interaction.java,v $
// Revision 1.1  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
//
//
// DESCRIPTION:
//  Handling interactions (i.e., collisions).
//
//-----------------------------------------------------------------------------


public class Interaction {


  public static final String rcsid = "$Id: Interaction.java,v 1.1 2010/08/24 14:57:42 velktron Exp $";

/////////////////// STATUS ///////////////////
  
  WadLoader W;
  doomstat DS;
  DoomAutoMap AM;
  random RND;
  
/*
  // Data.
  #include "doomdef.h"
  #include "dstrings.h"
  #include "sounds.h"

  #include "doomstat.h"

  #include "m_random.h"
  #include "i_system.h"

  #include "am_map.h"

  #include "p_local.h"

  #include "s_sound.h"

  #ifdef __GNUG__
  #pragma implementation "p_inter.h"
  #endif
  #include "p_inter.h" */


  public static final int BONUSADD =   6;




  // a weapon is found with two clip loads,
  // a big item has five clip loads
  int[] maxammo = {200, 50, 300, 50};
  int[] clipammo = {10, 4, 20, 1};


  //
  // GET STUFF
  //


  /**
   *   //
  // P_GiveAmmo
  // Num is the number of clip loads,
  // not the individual count (0= 1/2 clip).
  // Returns false if the ammo can't be picked up at all
   * @param player
   * @param ammo intended to be ammotype_t.
   */
  public boolean
  GiveAmmo
  ( player_t player,
    ammotype_t    amm,
    int       num )
  {
      int     oldammo;
      int ammo=amm.ordinal();
      if (ammo == ammotype_t.am_noammo.ordinal())
      return false;
          
      if (ammo < 0 || ammo > NUMAMMO)
      system.Error ("P_GiveAmmo: bad type %i", ammo); 
          
      if ( player.ammo[ammo] == player.maxammo[ammo]  )
      return false;
          
      if (num!=0)
      num *= clipammo[ammo];
      else
      num = clipammo[ammo]/2;
      
      if (DS.gameskill == skill_t.sk_baby
      || DS.gameskill == skill_t.sk_nightmare)
      {
      // give double ammo in trainer mode,
      // you'll need in nightmare
      num <<= 1;
      }
      
          
      oldammo = player.ammo[ammo];
      player.ammo[ammo] += num;

      if (player.ammo[ammo] > player.maxammo[ammo])
      player.ammo[ammo] = player.maxammo[ammo];

      // If non zero ammo, 
      // don't change up weapons,
      // player was lower on purpose.
      if (oldammo!=0)
      return true;    

      // We were down to zero,
      // so select a new weapon.
      // Preferences are not user selectable.
      switch (ammotype_t.values()[ammo])
      {
        case am_clip:
      if (player.readyweapon == weapontype_t.wp_fist)
      {
          if (player.weaponowned[weapontype_t.wp_chaingun.ordinal()])
          player.pendingweapon = weapontype_t.wp_chaingun;
          else
          player.pendingweapon = weapontype_t.wp_pistol;
      }
      break;
      
        case am_shell:
      if (player.readyweapon == weapontype_t.wp_fist
          || player.readyweapon == weapontype_t.wp_pistol)
      {
          if (player.weaponowned[weapontype_t.wp_shotgun.ordinal()])
          player.pendingweapon = weapontype_t.wp_shotgun;
      }
      break;
      
        case am_cell:
      if (player.readyweapon == weapontype_t.wp_fist
          || player.readyweapon == weapontype_t.wp_pistol)
      {
          if (player.weaponowned[weapontype_t.wp_plasma.ordinal()])
          player.pendingweapon = weapontype_t.wp_plasma;
      }
      break;
      
        case am_misl:
      if (player.readyweapon == weapontype_t.wp_fist)
      {
          if (player.weaponowned[weapontype_t.wp_missile.ordinal()])
          player.pendingweapon = weapontype_t.wp_missile;
      }
        default:
      break;
      }
      
      return true;
  }


  //
  // P_GiveWeapon
  // The weapon name may have a MF_DROPPED flag ored in.
  //
  public boolean
  GiveWeapon
  ( player_t player,
    weapontype_t  weapn,
    boolean   dropped )
  {
      boolean gaveammo;
      boolean gaveweapon;
      int weapon=weapn.ordinal();
      
      if (DS.netgame
      && (DS.deathmatch!=true) // ???? was "2"
       && !dropped )
      {
      // leave placed weapons forever on net games
      if (player.weaponowned[weapon])
          return false;

      player.bonuscount += BONUSADD;
      player.weaponowned[weapon] = true;

      if (DS.deathmatch)
          GiveAmmo (player, weaponinfo[weapon].ammo, 5);
      else
          GiveAmmo (player, weaponinfo[weapon].ammo, 2);
      player.pendingweapon = weapn;

      if (player == DS.players[DS.consoleplayer])
          // TODO: S_StartSound (null, sfx_wpnup);
      return false;
      }
      
      if (weaponinfo[weapon].ammo != ammotype_t.am_noammo)
      {
      // give one clip with a dropped weapon,
      // two clips with a found weapon
      if (dropped)
          gaveammo = GiveAmmo (player, weaponinfo[weapon].ammo, 1);
      else
          gaveammo = GiveAmmo (player, weaponinfo[weapon].ammo, 2);
      }
      else
      gaveammo = false;
      
      if (player.weaponowned[weapon])
      gaveweapon = false;
      else
      {
      gaveweapon = true;
      player.weaponowned[weapon] = true;
      player.pendingweapon = weapn;
      }
      
      return (gaveweapon || gaveammo);
  }

   

  /**
   * P_GiveBody
   * Returns false if the body isn't needed at all
   */
  
  public  boolean
  GiveBody
  ( player_t player,
    int       num )
  {
      if (player.health >= MAXHEALTH)
      return false;
          
      player.health += num;
      if (player.health > MAXHEALTH)
      player.health = MAXHEALTH;
      player.mo.health = player.health;
      
      return true;
  }



  /**
   * P_GiveArmor
   * Returns false if the armor is worse
   * than the current armor.
   */
  
  public boolean
  GiveArmor
  ( player_t player,
    int       armortype )
  {
      int     hits;
      
      hits = armortype*100;
      if (player.armorpoints >= hits)
      return false;   // don't pick up
          
      player.armortype = armortype;
      player.armorpoints = hits;
      
      return true;
  }



  //
  // P_GiveCard
  //
  public void
  GiveCard
  ( player_t player,
    card_t    crd )
  {
      int card=crd.ordinal();
      if (player.cards[card])
      return;
      
      player.bonuscount = BONUSADD;
      player.cards[card] = true;
  }


  //
  // P_GivePower
  //
  public boolean
  GivePower
  ( player_t player,
    int /*powertype_t*/   power ) // MAES: I didn't change this!
  {
      if (power == pw_invulnerability)
      {
      player.powers[power] = INVULNTICS;
      return true;
      }
      
      if (power == pw_invisibility)
      {
      player.powers[power] = INVISTICS;
      player.mo.flags |= MF_SHADOW;
      return true;
      }
      
      if (power == pw_infrared)
      {
      player.powers[power] = INFRATICS;
      return true;
      }
      
      if (power == pw_ironfeet)
      {
      player.powers[power] = IRONTICS;
      return true;
      }
      
      if (power == pw_strength)
      {
      GiveBody (player, 100);
      player.powers[power] = 1;
      return true;
      }
      
      if (player.powers[power]!=0)
      return false;   // already got it
          
      player.powers[power] = 1;
      return true;
  }



  /**
   * P_TouchSpecialThing
   */
  public void
  P_TouchSpecialThing
  ( mobj_t   special,
    mobj_t   toucher )
  {
      player_t   player;
      int     i;
      int delta;// fixed_t
      sfxenum_t     sound;
          
      delta = special.z - toucher.z;

      if (delta > toucher.height
      || delta < -8*FRACUNIT)
      {
      // out of reach
      return;
      }
      
      
      sound = sfxenum_t.sfx_itemup; 
      player = toucher.player;

      // Dead thing touching.
      // Can happen with a sliding player corpse.
      if (toucher.health <= 0)
      return;

      // Identify by sprite.
      switch (special.sprite)
      {
      // armor
        case SPR_ARM1:
      if (!GiveArmor (player, 1))
          return;
      player.message = GOTARMOR;
      break;
          
        case SPR_ARM2:
      if (!GiveArmor (player, 2))
          return;
      player.message = GOTMEGA;
      break;
      
      // bonus items
        case SPR_BON1:
      player.health++;       // can go over 100%
      if (player.health > 200)
          player.health = 200;
      player.mo.health = player.health;
      player.message = GOTHTHBONUS;
      break;
      
        case SPR_BON2:
      player.armorpoints++;      // can go over 100%
      if (player.armorpoints > 200)
          player.armorpoints = 200;
      if (player.armortype==0)
          player.armortype = 1;
      player.message = GOTARMBONUS;
      break;
      
        case SPR_SOUL:
      player.health += 100;
      if (player.health > 200)
          player.health = 200;
      player.mo.health = player.health;
      player.message = GOTSUPER;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_MEGA:
      if (DS.gamemode != GameMode_t.commercial)
          return;
      player.health = 200;
      player.mo.health = player.health;
      GiveArmor (player,2);
      player.message = GOTMSPHERE;
      sound = sfxenum_t.sfx_getpow;
      break;
      
      // cards
      // leave cards for everyone
        case SPR_BKEY:
      if (!player.cards[card_t.it_bluecard.ordinal()])
          player.message = GOTBLUECARD;
      GiveCard (player, card_t.it_bluecard);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_YKEY:
      if (!player.cards[card_t.it_yellowcard.ordinal()])
          player.message = GOTYELWCARD;
      GiveCard (player, card_t.it_yellowcard);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_RKEY:
      if (!player.cards[card_t.it_redcard.ordinal()])
          player.message = GOTREDCARD;
      GiveCard (player, card_t.it_redcard);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_BSKU:
      if (!player.cards[card_t.it_blueskull.ordinal()])
          player.message = GOTBLUESKUL;
      GiveCard (player, card_t.it_blueskull);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_YSKU:
      if (!player.cards[card_t.it_yellowskull.ordinal()])
          player.message = GOTYELWSKUL;
      GiveCard (player, card_t.it_yellowskull);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_RSKU:
      if (!player.cards[card_t.it_redskull.ordinal()])
          player.message = GOTREDSKULL;
      GiveCard (player, card_t.it_redskull);
      if (!DS.netgame)
          break;
      return;
      
      // medikits, heals
        case SPR_STIM:
      if (!GiveBody (player, 10))
          return;
      player.message = GOTSTIM;
      break;
      
        case SPR_MEDI:
      if (!GiveBody (player, 25))
          return;

      if (player.health < 25)
          player.message = GOTMEDINEED;
      else
          player.message = GOTMEDIKIT;
      break;

      
      // power ups
        case SPR_PINV:
      if (!GivePower (player, pw_invulnerability))
          return;
      player.message = GOTINVUL;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PSTR:
      if (!GivePower (player, pw_strength))
          return;
      player.message = GOTBERSERK;
      if (player.readyweapon != weapontype_t.wp_fist)
          player.pendingweapon = weapontype_t.wp_fist;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PINS:
      if (!GivePower (player, pw_invisibility))
          return;
      player.message = GOTINVIS;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_SUIT:
      if (!GivePower (player, pw_ironfeet))
          return;
      player.message = GOTSUIT;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PMAP:
      if (!GivePower (player, pw_allmap))
          return;
      player.message = GOTMAP;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PVIS:
      if (!GivePower (player, pw_infrared))
          return;
      player.message = GOTVISOR;
      sound = sfxenum_t.sfx_getpow;
      break;
      
      // ammo
        case SPR_CLIP:
      if ((special.flags & MF_DROPPED) !=0)
      {
          if (!GiveAmmo (player,ammotype_t.am_clip,0))
          return;
      }
      else
      {
          if (!GiveAmmo (player,ammotype_t.am_clip,1))
          return;
      }
      player.message = GOTCLIP;
      break;
      
        case SPR_AMMO:
      if (!GiveAmmo (player, ammotype_t.am_clip,5))
          return;
      player.message = GOTCLIPBOX;
      break;
      
        case SPR_ROCK:
      if (!GiveAmmo (player, ammotype_t.am_misl,1))
          return;
      player.message = GOTROCKET;
      break;
      
        case SPR_BROK:
      if (!GiveAmmo (player, ammotype_t.am_misl,5))
          return;
      player.message = GOTROCKBOX;
      break;
      
        case SPR_CELL:
      if (!GiveAmmo (player, ammotype_t.am_cell,1))
          return;
      player.message = GOTCELL;
      break;
      
        case SPR_CELP:
      if (!GiveAmmo (player, ammotype_t.am_cell,5))
          return;
      player.message = GOTCELLBOX;
      break;
      
        case SPR_SHEL:
      if (!GiveAmmo (player, ammotype_t.am_shell,1))
          return;
      player.message = GOTSHELLS;
      break;
      
        case SPR_SBOX:
      if (!GiveAmmo (player, ammotype_t.am_shell,5))
          return;
      player.message = GOTSHELLBOX;
      break;
      
        case SPR_BPAK:
      if (!player.backpack)
      {
          for (i=0 ; i<NUMAMMO ; i++)
          player.maxammo[i] *= 2;
          player.backpack = true;
      }
      for (i=0 ; i<NUMAMMO ; i++)
          GiveAmmo (player, ammotype_t.values()[i], 1);
      player.message = GOTBACKPACK;
      break;
      
      // weapons
        case SPR_BFUG:
      if (!GiveWeapon (player, weapontype_t.wp_bfg, false) )
          return;
      player.message = GOTBFG9000;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_MGUN:
      if (!GiveWeapon (player, weapontype_t.wp_chaingun, (special.flags&MF_DROPPED)!=0 ))
          return;
      player.message = GOTCHAINGUN;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_CSAW:
      if (!GiveWeapon (player, weapontype_t.wp_chainsaw, false) )
          return;
      player.message = GOTCHAINSAW;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_LAUN:
      if (!GiveWeapon (player, weapontype_t.wp_missile, false) )
          return;
      player.message = GOTLAUNCHER;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_PLAS:
      if (!GiveWeapon (player, weapontype_t.wp_plasma, false) )
          return;
      player.message = GOTPLASMA;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_SHOT:
      if (!GiveWeapon (player, weapontype_t.wp_shotgun, (special.flags&MF_DROPPED) !=0  ) )
          return;
      player.message = GOTSHOTGUN;
      sound = sfxenum_t.sfx_wpnup;  
      break;
          
        case SPR_SGN2:
      if (!GiveWeapon (player, weapontype_t.wp_supershotgun, (special.flags&MF_DROPPED )!=0 ))
          return;
      player.message = GOTSHOTGUN2;
      sound = sfxenum_t.sfx_wpnup;  
      break;
          
        default:
      system.Error ("P_SpecialThing: Unknown gettable thing");
      }
      
      if ((special.flags & MF_COUNTITEM)!=0)
      player.itemcount++;
      // TODO:RemoveMobj (special);
      player.bonuscount += BONUSADD;
      if (player == DS.players[DS.consoleplayer]) ;
      // TODO: S_StartSound (NULL, sound);
  }


  //
  // KillMobj
  //
  public void
  KillMobj
  ( mobj_t   source,
    mobj_t   target )
  {
      mobjtype_t  item;
      mobj_t mo;
      
      target.flags &= ~(MF_SHOOTABLE|MF_FLOAT|MF_SKULLFLY);

      if (target.type != mobjtype_t.MT_SKULL)
      target.flags &= ~MF_NOGRAVITY;

      target.flags |= MF_CORPSE|MF_DROPOFF;
      target.height >>= 2;

      if (source!=null && source.player!=null)
      {
      // count for intermission
      if ((target.flags & MF_COUNTKILL)!=0)
          source.player.killcount++;    

      if (target.player!=null) ;
         // TODO: source.player.frags[target.player-DS.players]++;
         // It's probably intended to increment the frags of source player vs target player. Lookup? 
      }
      else if (!DS.netgame && ((target.flags & MF_COUNTKILL)!=0) )
      {
      // count all monster deaths,
      // even those caused by other monsters
      DS.players[0].killcount++;
      }
      
      if (target.player!=null)
      {
      // count environment kills against you
      if (source==null)    
          target.player.frags[target.player-players]++;
              
      target.flags &= ~MF_SOLID;
      target.player.playerstate = PST_DEAD;
      //TODO: DropWeapon (target.player); // in PSPR

      if (target.player == DS.players[DS.consoleplayer]
          && DS.automapactive)
      {
          // don't die in auto map,
          // switch view prior to dying
          AM.Stop ();
      }
      
      }

      if (target.health < -target.info.spawnhealth 
      && target.info.xdeathstate)
      {
      P_SetMobjState (target, target.info.xdeathstate);
      }
      else
      P_SetMobjState (target, target.info.deathstate);
      target.tics -= RND.P_Random()&3;

      if (target.tics < 1)
      target.tics = 1;
          
      //  I_StartSound (&actor.r, actor.info.deathsound);


      // Drop stuff.
      // This determines the kind of object spawned
      // during the death frame of a thing.
      switch (target.type)
      {
        case MT_WOLFSS:
        case MT_POSSESSED:
      item = MT_CLIP;
      break;
      
        case MT_SHOTGUY:
      item = MT_SHOTGUN;
      break;
      
        case MT_CHAINGUY:
      item = MT_CHAINGUN;
      break;
      
        default:
      return;
      }

      mo = P_SpawnMobj (target.x,target.y,ONFLOORZ, item);
      mo.flags |= MF_DROPPED;    // special versions of items
  }




  //
  // P_DamageMobj
  // Damages both enemies and players
  // "inflictor" is the thing that caused the damage
  //  creature or missile, can be NULL (slime, etc)
  // "source" is the thing to target after taking damage
  //  creature or NULL
  // Source and inflictor are the same for melee attacks.
  // Source can be NULL for slime, barrel explosions
  // and other environmental stuff.
  //
  void
  P_DamageMobj
  ( mobj_t*   target,
    mobj_t*   inflictor,
    mobj_t*   source,
    int       damage )
  {
      unsigned    ang;
      int     saved;
      player_t*   player;
      fixed_t thrust;
      int     temp;
      
      if ( !(target.flags & MF_SHOOTABLE) )
      return; // shouldn't happen...
          
      if (target.health <= 0)
      return;

      if ( target.flags & MF_SKULLFLY )
      {
      target.momx = target.momy = target.momz = 0;
      }
      
      player = target.player;
      if (player && gameskill == sk_baby)
      damage >>= 1;   // take half damage in trainer mode
          

      // Some close combat weapons should not
      // inflict thrust and push the victim out of reach,
      // thus kick away unless using the chainsaw.
      if (inflictor
      && !(target.flags & MF_NOCLIP)
      && (!source
          || !source.player
          || source.player.readyweapon != weapontype_t.wp_chainsaw))
      {
      ang = R_PointToAngle2 ( inflictor.x,
                  inflictor.y,
                  target.x,
                  target.y);
          
      thrust = damage*(FRACUNIT>>3)*100/target.info.mass;

      // make fall forwards sometimes
      if ( damage < 40
           && damage > target.health
           && target.z - inflictor.z > 64*FRACUNIT
           && (P_Random ()&1) )
      {
          ang += ANG180;
          thrust *= 4;
      }
          
      ang >>= ANGLETOFINESHIFT;
      target.momx += FixedMul (thrust, finecosine[ang]);
      target.momy += FixedMul (thrust, finesine[ang]);
      }
      
      // player specific
      if (player)
      {
      // end of game hell hack
      if (target.subsector.sector.special == 11
          && damage >= target.health)
      {
          damage = target.health - 1;
      }
      

      // Below certain threshold,
      // ignore damage in GOD mode, or with INVUL power.
      if ( damage < 1000
           && ( (player.cheats&CF_GODMODE)
            || player.powers[pw_invulnerability] ) )
      {
          return;
      }
      
      if (player.armortype)
      {
          if (player.armortype == 1)
          saved = damage/3;
          else
          saved = damage/2;
          
          if (player.armorpoints <= saved)
          {
          // armor is used up
          saved = player.armorpoints;
          player.armortype = 0;
          }
          player.armorpoints -= saved;
          damage -= saved;
      }
      player.health -= damage;   // mirror mobj health here for Dave
      if (player.health < 0)
          player.health = 0;
      
      player.attacker = source;
      player.damagecount += damage;  // add damage after armor / invuln

      if (player.damagecount > 100)
          player.damagecount = 100;  // teleport stomp does 10k points...
      
      temp = damage < 100 ? damage : 100;

      if (player == &players[consoleplayer])
          I_Tactile (40,10,40+temp*2);
      }
      
      // do the damage    
      target.health -= damage;   
      if (target.health <= 0)
      {
      P_KillMobj (source, target);
      return;
      }

      if ( (P_Random () < target.info.painchance)
       && !(target.flags&MF_SKULLFLY) )
      {
      target.flags |= MF_JUSTHIT;    // fight back!
      
      P_SetMobjState (target, target.info.painstate);
      }
              
      target.reactiontime = 0;       // we're awake now...   

      if ( (!target.threshold || target.type == MT_VILE)
       && source && source != target
       && source.type != MT_VILE)
      {
      // if not intent on another player,
      // chase after this one
      target.target = source;
      target.threshold = BASETHRESHOLD;
      if (target.state == &states[target.info.spawnstate]
          && target.info.seestate != S_NULL)
          P_SetMobjState (target, target.info.seestate);
      }
              
  }


}
