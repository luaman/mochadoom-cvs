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
// $Id: Interaction.java,v 1.4 2010/09/13 23:09:51 velktron Exp $
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
// Revision 1.4  2010/09/13 23:09:51  velktron
// More "unified game state" work
//
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


  public static final String rcsid = "$Id: Interaction.java,v 1.4 2010/09/13 23:09:51 velktron Exp $";

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


  




 


}
