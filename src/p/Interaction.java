package p;

import static data.Defines.*;
import static data.Limits.*;
import static p.MapUtils.flags;
import static doom.items.*;
import static p.mobj.*;
import static m.fixed_t.*;
import static data.Tables.*;
import static doom.englsh.*;
import static data.info.*;
import m.random;
import automap.DoomAutoMap;
import rr.Renderer;
import rr.RendererState;
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
// $Id: Interaction.java,v 1.3 2010/09/13 15:39:17 velktron Exp $
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
// Revision 1.3  2010/09/13 15:39:17  velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.2  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.1  2010/08/24 14:57:42  velktron
// A lot but inconclusive work today.
//
//
// DESCRIPTION:
//  Handling interactions (i.e., collisions).
//
//-----------------------------------------------------------------------------


public class Interaction {


  public static final String rcsid = "$Id: Interaction.java,v 1.3 2010/09/13 15:39:17 velktron Exp $";

/////////////////// STATUS ///////////////////
  
  WadLoader W;
  doomstat DS;
  DoomAutoMap AM;
  random RND;
  RendererState R;
  
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
      if (!player.GiveArmor (1))
          return;
      player.message = GOTARMOR;
      break;
          
        case SPR_ARM2:
      if (!player.GiveArmor (2))
          return;
      player.message = GOTMEGA;
      break;
      
      // bonus items
        case SPR_BON1:
      player.health[0]++;       // can go over 100%
      if (player.health[0] > 200)
          player.health[0] = 200;
      player.mo.health = player.health[0];
      player.message = GOTHTHBONUS;
      break;
      
        case SPR_BON2:
      player.armorpoints[0]++;      // can go over 100%
      if (player.armorpoints[0] > 200)
          player.armorpoints[0] = 200;
      if (player.armortype==0)
          player.armortype = 1;
      player.message = GOTARMBONUS;
      break;
      
        case SPR_SOUL:
      player.health[0] += 100;
      if (player.health[0] > 200)
          player.health[0] = 200;
      player.mo.health = player.health[0];
      player.message = GOTSUPER;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_MEGA:
      if (DS.gamemode != GameMode_t.commercial)
          return;
      player.health[0] = 200;
      player.mo.health = player.health[0];
      player.GiveArmor (2);
      player.message = GOTMSPHERE;
      sound = sfxenum_t.sfx_getpow;
      break;
      
      // cards
      // leave cards for everyone
        case SPR_BKEY:
      if (!player.cards[card_t.it_bluecard.ordinal()])
          player.message = GOTBLUECARD;
      player.GiveCard (card_t.it_bluecard);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_YKEY:
      if (!player.cards[card_t.it_yellowcard.ordinal()])
          player.message = GOTYELWCARD;
      player.GiveCard (card_t.it_yellowcard);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_RKEY:
      if (!player.cards[card_t.it_redcard.ordinal()])
          player.message = GOTREDCARD;
      player.GiveCard (card_t.it_redcard);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_BSKU:
      if (!player.cards[card_t.it_blueskull.ordinal()])
          player.message = GOTBLUESKUL;
      player.GiveCard (card_t.it_blueskull);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_YSKU:
      if (!player.cards[card_t.it_yellowskull.ordinal()])
          player.message = GOTYELWSKUL;
      player.GiveCard (card_t.it_yellowskull);
      if (!DS.netgame)
          break;
      return;
      
        case SPR_RSKU:
      if (!player.cards[card_t.it_redskull.ordinal()])
          player.message = GOTREDSKULL;
      player.GiveCard (card_t.it_redskull);
      if (!DS.netgame)
          break;
      return;
      
      // medikits, heals
        case SPR_STIM:
      if (!player.GiveBody (10))
          return;
      player.message = GOTSTIM;
      break;
      
        case SPR_MEDI:
      if (!player.GiveBody ( 25))
          return;

      if (player.health[0] < 25)
          player.message = GOTMEDINEED;
      else
          player.message = GOTMEDIKIT;
      break;

      
      // power ups
        case SPR_PINV:
      if (!player.GivePower(pw_invulnerability))
          return;
      player.message = GOTINVUL;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PSTR:
      if (!player.GivePower (pw_strength))
          return;
      player.message = GOTBERSERK;
      if (player.readyweapon != weapontype_t.wp_fist)
          player.pendingweapon = weapontype_t.wp_fist;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PINS:
      if (!player.GivePower (pw_invisibility))
          return;
      player.message = GOTINVIS;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_SUIT:
      if (!player.GivePower (pw_ironfeet))
          return;
      player.message = GOTSUIT;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PMAP:
      if (!player.GivePower (pw_allmap))
          return;
      player.message = GOTMAP;
      sound = sfxenum_t.sfx_getpow;
      break;
      
        case SPR_PVIS:
      if (!player.GivePower (pw_infrared))
          return;
      player.message = GOTVISOR;
      sound = sfxenum_t.sfx_getpow;
      break;
      
      // ammo
        case SPR_CLIP:
      if ((special.flags & MF_DROPPED) !=0)
      {
          if (!player.GiveAmmo (ammotype_t.am_clip,0))
          return;
      }
      else
      {
          if (!player.GiveAmmo (ammotype_t.am_clip,1))
          return;
      }
      player.message = GOTCLIP;
      break;
      
        case SPR_AMMO:
      if (!player.GiveAmmo ( ammotype_t.am_clip,5))
          return;
      player.message = GOTCLIPBOX;
      break;
      
        case SPR_ROCK:
      if (!player.GiveAmmo ( ammotype_t.am_misl,1))
          return;
      player.message = GOTROCKET;
      break;
      
        case SPR_BROK:
      if (!player.GiveAmmo ( ammotype_t.am_misl,5))
          return;
      player.message = GOTROCKBOX;
      break;
      
        case SPR_CELL:
      if (!player.GiveAmmo ( ammotype_t.am_cell,1))
          return;
      player.message = GOTCELL;
      break;
      
        case SPR_CELP:
      if (!player.GiveAmmo ( ammotype_t.am_cell,5))
          return;
      player.message = GOTCELLBOX;
      break;
      
        case SPR_SHEL:
      if (!player.GiveAmmo ( ammotype_t.am_shell,1))
          return;
      player.message = GOTSHELLS;
      break;
      
        case SPR_SBOX:
      if (!player.GiveAmmo ( ammotype_t.am_shell,5))
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
          player.GiveAmmo ( ammotype_t.values()[i], 1);
      player.message = GOTBACKPACK;
      break;
      
      // weapons
        case SPR_BFUG:
      if (!player.GiveWeapon (weapontype_t.wp_bfg, false) )
          return;
      player.message = GOTBFG9000;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_MGUN:
      if (!player.GiveWeapon (weapontype_t.wp_chaingun, (special.flags&MF_DROPPED)!=0 ))
          return;
      player.message = GOTCHAINGUN;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_CSAW:
      if (!player.GiveWeapon (weapontype_t.wp_chainsaw, false) )
          return;
      player.message = GOTCHAINSAW;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_LAUN:
      if (!player.GiveWeapon (weapontype_t.wp_missile, false) )
          return;
      player.message = GOTLAUNCHER;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_PLAS:
      if (!player.GiveWeapon (weapontype_t.wp_plasma, false) )
          return;
      player.message = GOTPLASMA;
      sound = sfxenum_t.sfx_wpnup;  
      break;
      
        case SPR_SHOT:
      if (!player.GiveWeapon (weapontype_t.wp_shotgun, (special.flags&MF_DROPPED) !=0  ) )
          return;
      player.message = GOTSHOTGUN;
      sound = sfxenum_t.sfx_wpnup;  
      break;
          
        case SPR_SGN2:
      if (!player.GiveWeapon (weapontype_t.wp_supershotgun, (special.flags&MF_DROPPED )!=0 ))
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
      player.bonuscount += player_t.BONUSADD;
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
          // TODO: some way to indentify which one of the 
          // four possiblelayers is the current player
          
          target.player.frags[target.player.identify()]++;
              
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
      && target.info.xdeathstate!=null)
      {
          target.SetMobjState(target.info.xdeathstate);
      }
      else
          target.SetMobjState (target.info.deathstate);
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
      item = mobjtype_t.MT_CLIP;
      break;
      
        case MT_SHOTGUY:
      item = mobjtype_t.MT_SHOTGUN;
      break;
      
        case MT_CHAINGUY:
      item = mobjtype_t.MT_CHAINGUN;
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
  public void
  P_DamageMobj
  ( mobj_t   target,
    mobj_t   inflictor,
    mobj_t   source,
    int       damage )
  {
      int    ang; // unsigned
      int     saved;
      player_t   player;
      int thrust; // fixed_t
      int     temp;
      
      if ( !flags(target.flags, MF_SHOOTABLE))
      return; // shouldn't happen...
          
      if (target.health <= 0)
      return;

      if ( flags(target.flags , MF_SKULLFLY ))
      {
      target.momx = target.momy = target.momz = 0;
      }
      
      player = target.player;
      if ((player!=null) && DS.gameskill == skill_t.sk_baby)
      damage >>= 1;   // take half damage in trainer mode
          

      // Some close combat weapons should not
      // inflict thrust and push the victim out of reach,
      // thus kick away unless using the chainsaw.
      if ((inflictor !=null)
      && !flags(target.flags, MF_NOCLIP)
      && (source==null
          || source.player==null
          || source.player.readyweapon != weapontype_t.wp_chainsaw))
      {
      ang = R.PointToAngle2 ( inflictor.x,
                  inflictor.y,
                  target.x,
                  target.y);
          
      thrust = damage*(FRACUNIT>>3)*100/target.info.mass;

      // make fall forwards sometimes
      if ( (damage < 40)
           && (damage > target.health)
           && (target.z - inflictor.z > 64*FRACUNIT)
           && flags(RND.P_Random(),1) )
      {
          ang += ANG180;
          thrust *= 4;
      }
          
      ang >>= ANGLETOFINESHIFT;
      target.momx += FixedMul (thrust, finecosine[ang]);
      target.momy += FixedMul (thrust, finesine[ang]);
      }
      
      // player specific
      if (player!=null)
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
           && ( flags(player.cheats,player_t.CF_GODMODE))
            || player.powers[pw_invulnerability]!=0 ) 
      {
          return;
      }
      
      if (player.armortype!=0)
      {
          if (player.armortype == 1)
          saved = damage/3;
          else
          saved = damage/2;
          
          if (player.armorpoints[0] <= saved)
          {
          // armor is used up
          saved = player.armorpoints[0];
          player.armortype = 0;
          }
          player.armorpoints[0] -= saved;
          damage -= saved;
      }
      player.health[0] -= damage;   // mirror mobj health here for Dave
      if (player.health[0] < 0)
          player.health[0] = 0;
      
      player.attacker = source;
      player.damagecount += damage;  // add damage after armor / invuln

      if (player.damagecount > 100)
          player.damagecount = 100;  // teleport stomp does 10k points...
      
      temp = damage < 100 ? damage : 100;

      if (player == DS.players[DS.consoleplayer]) ;
          // TODO: I_Tactile (40,10,40+temp*2);
      }
      
      // do the damage    
      target.health -= damage;   
      if (target.health <= 0)
      {
      KillMobj (source, target);
      return;
      }

      if ( (RND.P_Random () < target.info.painchance)
       && !flags(target.flags,MF_SKULLFLY) )
      {
      target.flags |= MF_JUSTHIT;    // fight back!
      
      target.SetMobjState (target.info.painstate);
      }
              
      target.reactiontime = 0;       // we're awake now...   

      if ( ((target.threshold==0) || (target.type == mobjtype_t.MT_VILE))
       && (source!=null) && (source != target)
       && (source.type != mobjtype_t.MT_VILE))
      {
      // if not intent on another player,
      // chase after this one
      target.target = source;
      target.threshold = BASETHRESHOLD;
      if (target.state == states[target.info.spawnstate.ordinal()]
          && target.info.seestate != statenum_t.S_NULL)
          target.SetMobjState (target.info.seestate);
      }
              
  }


}
