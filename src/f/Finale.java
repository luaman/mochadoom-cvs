package f;

import static data.Defines.HU_FONTSIZE;
import static data.Defines.HU_FONTSTART;
import static data.Defines.PU_CACHE;
import static data.Defines.PU_LEVEL;
import static data.Defines.SCREENHEIGHT;
import static data.Defines.SCREENWIDTH;
import static data.Defines.FF_FRAMEMASK;
import static data.Limits.MAXPLAYERS;
import static data.info.mobjinfo;
import static data.info.states;
import static doom.englsh.*;
import static p.MapUtils.*;
import g.DoomGameInterface;
import hu.HU;

import java.io.IOException;

import rr.UnifiedRenderer;
import rr.column_t;
import rr.patch_t;
import rr.spritedef_t;
import rr.spriteframe_t;
import v.DoomVideoRenderer;
import w.WadLoader;
import data.doomstat;
import data.mobjtype_t;
import data.state_t;
import data.Defines.GameMode_t;
import data.Defines.gamestate_t;
import data.Defines.statenum_t;
import data.sounds.sfxenum_t;
import doom.event_t;
import doom.evtype_t;
import doom.gameaction_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Finale.java,v 1.4 2010/09/22 16:40:02 velktron Exp $
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
// DESCRIPTION:
//  Game completion, final screen animation.
//
//-----------------------------------------------------------------------------

public class Finale {

  public static final String rcsid = "$Id: Finale.java,v 1.4 2010/09/22 16:40:02 velktron Exp $";

  DoomGameInterface G;
  doomstat DS;
  DoomVideoRenderer V;
  HU HU;
  WadLoader W;
  UnifiedRenderer R;
  
  int     finalestage;

  int     finalecount;

  private static int TEXTSPEED   =3;
  private static int TEXTWAIT  =  250;

  String   e1text = E1TEXT;
  String   e2text = E2TEXT;
  String   e3text = E3TEXT;
  String   e4text = E4TEXT;

  String   c1text = C1TEXT;
  String   c2text = C2TEXT;
  String   c3text = C3TEXT;
  String   c4text = C4TEXT;
  String   c5text = C5TEXT;
  String   c6text = C6TEXT;

  String   p1text = P1TEXT;
  String   p2text = P2TEXT;
  String   p3text = P3TEXT;
  String   p4text = P4TEXT;
  String   p5text = P5TEXT;
  String   p6text = P6TEXT;

  String   t1text = T1TEXT;
  String   t2text = T2TEXT;
  String   t3text = T3TEXT;
  String   t4text = T4TEXT;
  String   t5text = T5TEXT;
  String   t6text = T6TEXT;

  String   finaletext;
  String   finaleflat;

  /**
  * F_StartFinale
  */
public void StartFinale ()
  {
      G.setGameAction(gameaction_t.ga_nothing);
      DS.gamestate = gamestate_t.GS_FINALE;
      DS.viewactive = false;
      DS.automapactive = false;

      // Okay - IWAD dependend stuff.
      // This has been changed severly, and
      //  some stuff might have changed in the process.
      switch ( DS.gamemode )
      {

        // DOOM 1 - E1, E3 or E4, but each nine missions
        case shareware:
        case registered:
        case retail:
        {
      // TODO: S_ChangeMusic(mus_victor, true);
      
      switch (DS.gameepisode)
      {
        case 1:
          finaleflat = "FLOOR4_8";
          finaletext = e1text;
          break;
        case 2:
          finaleflat = "SFLR6_1";
          finaletext = e2text;
          break;
        case 3:
          finaleflat = "MFLR8_4";
          finaletext = e3text;
          break;
        case 4:
          finaleflat = "MFLR8_3";
          finaletext = e4text;
          break;
        default:
          // Ouch.
          break;
      }
      break;
        }
        
        // DOOM II and missions packs with E1, M34
        case commercial:
        {
        //TODO: S_ChangeMusic(mus_read_m, true);

        switch (DS.gamemap)
        {
          case 6:
            finaleflat = "SLIME16";
            finaletext = c1text;
            break;
          case 11:
            finaleflat = "RROCK14";
            finaletext = c2text;
            break;
          case 20:
            finaleflat = "RROCK07";
            finaletext = c3text;
            break;
          case 30:
            finaleflat = "RROCK17";
            finaletext = c4text;
            break;
          case 15:
            finaleflat = "RROCK13";
            finaletext = c5text;
            break;
          case 31:
            finaleflat = "RROCK19";
            finaletext = c6text;
            break;
          default:
            // Ouch.
            break;
        }
        break;
        } 

     
        // Indeterminate.
        default:
      // TODO: S_ChangeMusic(mus_read_m, true);
      finaleflat = "F_SKY1"; // Not used anywhere else.
      finaletext = c1text;  // FIXME - other text, music?
      break;
      }
      
      finalestage = 0;
      finalecount = 0;
      
  }



  public boolean Responder (event_t event)
  {
      if (finalestage == 2)
      return CastResponder (event);
      
      return false;
  }


  /**
   * F_Ticker
   */
  
  public void Ticker ()
  {
      int     i;
      
      // check for skipping
      if ( (DS.gamemode == GameMode_t.commercial)
        && ( finalecount > 50) )
      {
        // go on to the next level
        for (i=0 ; i<MAXPLAYERS ; i++)
      if (DS.players[i].cmd.buttons!=0)
        break;
                  
        if (i < MAXPLAYERS)
        { 
      if (DS.gamemap == 30)
        StartCast ();
      else
        G.setGameAction(gameaction_t.ga_worlddone);
        }
      }
      
      // advance animation
      finalecount++;
      
      if (finalestage == 2)
      {
      CastTicker ();
      return;
      }
      
      if ( DS.gamemode == GameMode_t.commercial)
      return;
          
      if ((finalestage!=0) && finalecount> finaletext.length()*TEXTSPEED + TEXTWAIT)
      {
      finalecount = 0;
      finalestage = 1;
      DS.wipegamestate = gamestate_t.GS_MINUS_ONE;     // force a wipe
      if (DS.gameepisode == 3)
          ;
      //TODO: S_StartMusic (mus_bunny);
      }
  }



  //
  // F_TextWrite
  //

  //#include "hu_stuff.h"
  patch_t[] hu_font=HU.getHUFonts();


  public void TextWrite ()
  {
      byte[]   src;
      byte[]   dest;      
      int w;
      int     count;
      char[]   ch;
      int     c;
      int     cx;
      int     cy;
      
      // erase the entire screen to a tiled background
      src = (byte[])(W.CacheLumpName ( finaleflat , PU_CACHE)).getBuffer().array();
      dest = V.getScreen(0);

      int destPos=0;
      for (int y=0 ; y<SCREENHEIGHT ; y++)
      {

      for (int x=0 ; x<SCREENWIDTH/64 ; x++)
      {
          System.arraycopy(src, ((y&63)<<6), dest, destPos, 64);
          destPos += 64;
      }
      
      if ((SCREENWIDTH&63)==0)
      {
          System.arraycopy(src, ((y&63)<<6), dest, destPos, SCREENWIDTH&63);
          destPos += (SCREENWIDTH&63);
      }
      }

      V.MarkRect (0, 0, SCREENWIDTH, SCREENHEIGHT);
      
      // draw some of the text onto the screen
      cx = 10;
      cy = 10;
      ch = finaletext.toCharArray();
      
      count = (finalecount - 10)/TEXTSPEED;
      if (count < 0)
      count = 0;
      for (int i=0;i<ch.length ;i++)
      {
      c = ch[i];
      if (c==0)
          break;
      if (c == '\n')
      {
          cx = 10;
          cy += 11;
          continue;
      }
          
      c = Character.toUpperCase(c) - HU_FONTSTART;
      if (c < 0 || c> HU_FONTSIZE)
      {
          cx += 4;
          continue;
      }
          
      w = hu_font[c].width;
      if (cx+w > SCREENWIDTH)
          break;
      V.DrawPatch(cx, cy, 0, hu_font[c]);
      cx+=w;
      }
      
  }

  /**
   * Final DOOM 2 animation
   * Casting by id Software.
   *   in order of appearance
   */
  
  class castinfo_t
  {
      String        name;
      mobjtype_t  type;
      
      public castinfo_t(String name, mobjtype_t type){
          this.name=name;
          this.type=type;          
      }
      
  } 

  castinfo_t[]  castorder = {
      new castinfo_t(CC_ZOMBIE, mobjtype_t.MT_POSSESSED),
      new castinfo_t(CC_SHOTGUN, mobjtype_t.MT_SHOTGUY),
      new castinfo_t(CC_HEAVY, mobjtype_t.MT_CHAINGUY),
      new castinfo_t(CC_IMP, mobjtype_t.MT_TROOP),
      new castinfo_t(CC_DEMON, mobjtype_t.MT_SERGEANT),
      new castinfo_t(CC_LOST, mobjtype_t.MT_SKULL),
      new castinfo_t(CC_CACO, mobjtype_t.MT_HEAD),
      new castinfo_t(CC_HELL, mobjtype_t.MT_KNIGHT),
      new castinfo_t(CC_BARON, mobjtype_t.MT_BRUISER),
      new castinfo_t(CC_ARACH, mobjtype_t.MT_BABY),
      new castinfo_t(CC_PAIN, mobjtype_t.MT_PAIN),
      new castinfo_t(CC_REVEN, mobjtype_t.MT_UNDEAD),
      new castinfo_t(CC_MANCU, mobjtype_t.MT_FATSO),
      new castinfo_t(CC_ARCH, mobjtype_t.MT_VILE),
      new castinfo_t(CC_SPIDER, mobjtype_t.MT_SPIDER),
      new castinfo_t(CC_CYBER, mobjtype_t.MT_CYBORG),
      new castinfo_t(CC_HERO, mobjtype_t.MT_PLAYER),
      new castinfo_t(null,null)
  };

  int     castnum;
  int     casttics;
  state_t    caststate;
  boolean     castdeath;
  int     castframes;
  int     castonmelee;
  boolean     castattacking;


  //
  // F_StartCast
  //
  //extern  gamestate_t     wipegamestate;


  public void StartCast ()
  {
      DS.wipegamestate = gamestate_t.GS_MINUS_ONE;     // force a screen wipe
      castnum = 0;
      caststate = states[mobjinfo[castorder[castnum].type.ordinal()].seestate.ordinal()];
      casttics = (int) caststate.tics;
      castdeath = false;
      finalestage = 2;    
      castframes = 0;
      castonmelee = 0;
      castattacking = false;
      // TODO: S_ChangeMusic(mus_evil, true);
  }


  //
  // F_CastTicker
  //
  public void CastTicker ()
  {
      statenum_t     st;
      sfxenum_t     sfx;
      
      if (--casttics > 0)
      return;         // not time to change state yet
          
      if (caststate.tics == -1 || caststate.nextstate == statenum_t.S_NULL)
      {
      // switch from deathstate to next monster
      castnum++;
      castdeath = false;
      if (castorder[castnum].name == null)
          castnum = 0;
      if (mobjinfo[castorder[castnum].type.ordinal()].seesound.ordinal()!=0) ;
          // TODO: S_StartSound (NULL, mobjinfo[castorder[castnum].type].seesound);
      caststate = states[mobjinfo[castorder[castnum].type.ordinal()].seestate.ordinal()];
      castframes = 0;
      }
      else
      {
      // just advance to next state in animation
      if (caststate == states[statenum_t.S_PLAY_ATK1.ordinal()]) {
          stopattack();
          // Do exit stuff...
          casttics = (int) caststate.tics;
          if (casttics == -1)
          casttics = 15;
          
          //bye..
          return;          
      }
         // goto stopattack;    // Oh, gross hack!
      st = caststate.nextstate;
      caststate = states[st.ordinal()];
      castframes++;
      
      // sound hacks....
      switch (st)
      {
        case S_PLAY_ATK1: sfx = sfxenum_t.sfx_dshtgn; break;
        case S_POSS_ATK2: sfx = sfxenum_t.sfx_pistol; break;
        case S_SPOS_ATK2: sfx = sfxenum_t.sfx_shotgn; break;
        case S_VILE_ATK2: sfx = sfxenum_t.sfx_vilatk; break;
        case S_SKEL_FIST2:    sfx = sfxenum_t.sfx_skeswg; break;
        case S_SKEL_FIST4:    sfx = sfxenum_t.sfx_skepch; break;
        case S_SKEL_MISS2:    sfx = sfxenum_t.sfx_skeatk; break;
        case S_FATT_ATK8:
        case S_FATT_ATK5:
        case S_FATT_ATK2: sfx = sfxenum_t.sfx_firsht; break;
        case S_CPOS_ATK2:
        case S_CPOS_ATK3:
        case S_CPOS_ATK4: sfx = sfxenum_t.sfx_shotgn; break;
        case S_TROO_ATK3: sfx = sfxenum_t.sfx_claw; break;
        case S_SARG_ATK2: sfx = sfxenum_t.sfx_sgtatk; break;
        case S_BOSS_ATK2:
        case S_BOS2_ATK2:
        case S_HEAD_ATK2: sfx = sfxenum_t.sfx_firsht; break;
        case S_SKULL_ATK2:    sfx = sfxenum_t.sfx_sklatk; break;
        case S_SPID_ATK2:
        case S_SPID_ATK3: sfx = sfxenum_t.sfx_shotgn; break;
        case S_BSPI_ATK2: sfx = sfxenum_t.sfx_plasma; break;
        case S_CYBER_ATK2:
        case S_CYBER_ATK4:
        case S_CYBER_ATK6:    sfx = sfxenum_t.sfx_rlaunc; break;
        case S_PAIN_ATK3: sfx = sfxenum_t.sfx_sklatk; break;
        default: sfx = null; break;
      }
          
      if (sfx !=null) ;
          // TODO:S_StartSound (NULL, sfx);
      }
      
      if (castframes == 12)
      {
      // go into attack frame
      castattacking = true;
      if (castonmelee!=0)
          caststate=states[mobjinfo[castorder[castnum].type.ordinal()].meleestate.ordinal()];
      else
          caststate=states[mobjinfo[castorder[castnum].type.ordinal()].missilestate.ordinal()];
      castonmelee ^= 1;
      if (caststate == states[statenum_t.S_NULL.ordinal()])
      {
          if (castonmelee!=0)
          caststate=
              states[mobjinfo[castorder[castnum].type.ordinal()].meleestate.ordinal()];
          else
          caststate=
              states[mobjinfo[castorder[castnum].type.ordinal()].missilestate.ordinal()];
      }
      }
      
      if (castattacking)
      {
      if (castframes == 24
          ||  caststate == states[mobjinfo[castorder[castnum].type.ordinal()].seestate.ordinal()] )
      
        stopattack();      
      }
      
      casttics = (int) caststate.tics;
      if (casttics == -1)
      casttics = 15;
  }

protected void stopattack(){
    castattacking = false;
    castframes = 0;
    caststate = states[mobjinfo[castorder[castnum].type.ordinal()].seestate.ordinal()];
}
  
  /**
   * CastResponder
   */

  public boolean CastResponder (event_t ev)
  {
      if (ev.type != evtype_t.ev_keydown)
      return false;
          
      if (castdeath)
      return true;            // already in dying frames
          
      // go into death frame
      castdeath = true;
      caststate = states[mobjinfo[castorder[castnum].type.ordinal()].deathstate.ordinal()];
      casttics = (int) caststate.tics;
      castframes = 0;
      castattacking = false;
      if (mobjinfo[castorder[castnum].type.ordinal()].deathsound!=null) 
          ;
      // TODO:S_StartSound (null, mobjinfo[castorder[castnum].type].deathsound);
      
      return true;
  }


  public void CastPrint (String text)
  {
      char[]   ch;
      int     c;
      int     cx;
      int     w;
      int     width;
      
      // find width
      ch = text.toCharArray();
      width = 0;
      
      for (int i=0;i<ch.length;i++)
      {
      c = ch[i]++;
      if (c==0)
          break;
      c = Character.toUpperCase(c) - HU_FONTSTART;
      if (c < 0 || c> HU_FONTSIZE)
      {
          width += 4;
          continue;
      }
          
      w = hu_font[c].width;
      width += w;
      }
      
      // draw it
      cx = 160-width/2;
      //ch = text;
      for (int i=0;i<ch.length;i++)
      {
      c = ch[i];
      if (c==0)
          break;
      c = Character.toUpperCase(c) - HU_FONTSTART;
      if (c < 0 || c> HU_FONTSIZE)
      {
          cx += 4;
          continue;
      }
          
      w = hu_font[c].width;
      V.DrawPatch(cx, 180, 0, hu_font[c]);
      cx+=w;
      }
      
  }


  /**
   * F_CastDrawer
 * @throws IOException 
   */
  
  //public void V_DrawPatchFlipped (int x, int y, int scrn, patch_t patch);

  public void CastDrawer () 
  {
      spritedef_t    sprdef;
      spriteframe_t  sprframe;
      int         lump;
      boolean     flip;
      patch_t        patch=null;
      
      // erase the entire screen to a background
      V.DrawPatch (0,0,0, W.CachePatchName ("BOSSBACK", PU_CACHE));

      this.CastPrint (castorder[castnum].name);
      
      // draw the current frame in the middle of the screen
      // TODO: Sprites are in Renderer;
      sprdef = R.sprites[caststate.sprite.ordinal()];
      sprframe = sprdef.spriteframes[ caststate.frame & FF_FRAMEMASK];
      lump = sprframe.lump[0];
      flip = eval(sprframe.flip[0]);
               flip=false;
              lump=0;
              
        patch = W.CachePatchNum(lump+R.firstspritelump, PU_CACHE);

      if (flip)
      V.DrawPatchFlipped (160,170,0,patch);
      else
      V.DrawPatch (160,170,0,patch);
  }


  /** Is there ANY good reason not to use the renderer function, instead?
   * F_DrawPatchCol
   */
  
  
  public void
  DrawPatchCol
  ( int       x,
    patch_t  patch,
    int       col )
  {
      column_t   column;
      int   source;
      byte[]   dest;
      int   desttop;
      int     count;
      
      column = patch.columns[col];
      desttop = x;
      dest=V.getScreen(0);
      // step through the posts in a column
      
      
      for (int i=0;i<column.posts;i++){
          // Get pointer to post offset.
          source=column.postofs[i];
          // Get post delta
          short delta=column.postdeltas[i];
          // We skip delta, len and padding.
          source+=3; 
          
          // Skip transparent rows...
          if (delta==0xFF) break;

          int destPos = desttop + delta*SCREENWIDTH;  
          
          // These lengths are already correct.
          for (int j=0;j<column.postlen[i];j++){
                 dest[destPos] = column.data[source++];
                 destPos += SCREENWIDTH;
          }
      }
   }

  
  protected int  laststage;

  /**
   * F_BunnyScroll
   */
  public void BunnyScroll ()
  {
      int     scrolled;
      int     x;
      patch_t   p1;
      patch_t   p2;
      String    name;
      int     stage;

          
      p1 = W.CachePatchName ("PFUB2", PU_LEVEL);
      p2 = W.CachePatchName ("PFUB1", PU_LEVEL);

      V.MarkRect (0, 0, SCREENWIDTH, SCREENHEIGHT);
      
      scrolled = 320 - (finalecount-230)/2;
      if (scrolled > 320)
      scrolled = 320;
      if (scrolled < 0)
      scrolled = 0;
          
      for ( x=0 ; x<SCREENWIDTH ; x++)
      {
      if (x+scrolled < 320)
          this.DrawPatchCol (x, p1, x+scrolled);
      else
          this.DrawPatchCol (x, p2, x+scrolled - 320);       
      }
      
      if (finalecount < 1130)
      return;
      if (finalecount < 1180)
      {
      V.DrawPatch ((SCREENWIDTH-13*8)/2,
               (SCREENHEIGHT-8*8)/2,0, W.CachePatchName ("END0",PU_CACHE));
      laststage = 0;
      return;
      }
      
      stage = (finalecount-1180) / 5;
      if (stage > 6)
      stage = 6;
      if (stage > laststage)
      {
      //TODO:          S_StartSound (NULL, sfx_pistol);
      laststage = stage;
      }
      
      name=("END"+stage);
      V.DrawPatch ((SCREENWIDTH-13*8)/2, (SCREENHEIGHT-8*8)/2,0, W.CachePatchName (name,PU_CACHE));
  }


  //
  // F_Drawer
  //
  public void Drawer ()
  {
      if (finalestage == 2)
      {
      CastDrawer();
      return;
      }

      if (finalestage==0)
      TextWrite ();
      else
      {
      switch (DS.gameepisode)
      {
        case 1:
          if ( DS.gamemode == GameMode_t.retail )
            V.DrawPatch (0,0,0,
               W.CachePatchName("CREDIT",PU_CACHE));
          else
            V.DrawPatch (0,0,0,
                W.CachePatchName("HELP2",PU_CACHE));
          break;
        case 2:
          V.DrawPatch(0,0,0,
              W.CachePatchName("VICTORY2",PU_CACHE));
          break;
        case 3:
          BunnyScroll ();
          break;
        case 4:
          V.DrawPatch (0,0,0,
               W.CachePatchName("ENDPIC",PU_CACHE));
          break;
      }
      }
              
  }
}



///$Log