package g;

import static data.Defines.*;
import static data.Limits.*;
import static m.fixed_t.*;
import static data.Tables.*;
import static data.SineCosine.*;
import static data.dstrings.SAVEGAMENAME;
import i.system;

import java.util.Arrays;

import automap.DoomAutoMap;

import m.Menu;
import m.random;

import f.Finale;
import hu.HU;
import p.LevelLoader;
import p.mobj_t;
import rr.RendererData;
import rr.UnifiedRenderer;
import rr.subsector_t;
import st.StatusBar;
import utils.C2JUtils;
import w.WadLoader;
import data.doomstat;
import data.mapthing_t;
import data.Defines.gamestate_t;
import data.Defines.skill_t;
import doom.DoomContext;
import doom.event_t;
import doom.evtype_t;
import doom.gameaction_t;
import doom.player_t;
import doom.ticcmd_t;
import doom.wbstartstruct_t;
import doom.weapontype_t;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomGame.java,v 1.5 2010/09/07 16:23:00 velktron Exp $
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
// $Log: DoomGame.java,v $
// Revision 1.5  2010/09/07 16:23:00  velktron
// *** empty log message ***
//
// Revision 1.4  2010/09/01 15:53:42  velktron
// Graphics data loader implemented....still need to figure out how column caching works, though.
//
//
// DESCRIPTION:  none
//
//-----------------------------------------------------------------------------

public class DoomGame {

 
  public static final String rcsid = "$Id: DoomGame.java,v 1.5 2010/09/07 16:23:00 velktron Exp $";

  
  ////////////////////////// STATUS /////////////////////////
  
  doomstat DS;
  HU HU;
  RendererData RD;
  LevelLoader LL;
  Menu M;
  StatusBar ST;
  DoomAutoMap AM;
  Finale F;
  WadLoader W;
  UnifiedRenderer R;
  random RND;
  
 /* #include <string.h>
  #include <stdlib.h>

  #include "doomdef.h" 
  #include "doomstat.h"

  #include "z_zone.h"
  #include "f_finale.h"
  #include "m_argv.h"
  #include "m_misc.h"
  #include "m_menu.h"
  #include "m_random.h"
  #include "i_system.h"

  #include "p_setup.h"
  #include "p_saveg.h"
  #include "p_tick.h"

  #include "d_main.h"

  #include "wi_stuff.h"
  #include "hu_stuff.h"
  #include "st_stuff.h"
  #include "am_map.h"

  // Needs access to LFB.
  #include "v_video.h"

  #include "w_wad.h"

  #include "p_local.h" 

  #include "s_sound.h"

  // Data.
  #include "dstrings.h"
  #include "sounds.h"

  // SKY handling - still the wrong place.
  #include "r_data.h"
  #include "r_sky.h"



  #include "g_game.h"
*/



   
  // Fields specific to DoomGame.
  public gameaction_t    gameaction; 
  public boolean         sendpause;              // send a pause event next tic 
  
  // Most of these are actually in doomstat. Some can be "pegged" (those in arrays) but
  // in order to keep references correctly updated, we must use them in DS.
  // Objects not in arrays can be read correctly if "pegged", fields can be written to,  
  // but not if they have value semantics.
      
  boolean paused;
  boolean         sendsave; // send a save event next tic 
  int starttime;
  boolean         timingdemo;             // if true, exit with report on completion 
  /*
  public boolean     respawnmonsters;
  public int             gameepisode; 
  public int             gamemap; 
   
  public boolean         paused; 
  public boolean         usergame;               // ok to save / end game 
   
  
  public boolean         nodrawers;              // for comparative timing purposes 
  public boolean         noblit;                 // for comparative timing purposes 
  public int             starttime;              // for comparative timing purposes       
   
  public boolean         viewactive; 
   
  public boolean         deathmatch;             // only if started as net death 
  public boolean         netgame;                // only true if packets are broadcast 
  */
  
  public boolean getPaused() {
    return paused;
}

public void setPaused(boolean paused) {
    this.paused = paused;
}

// These can be pegged to doomstat, because they are arrays.
private boolean[]         playeringame; 
private player_t[]        players; 
private mapthing_t[] playerstarts; 
   
  /*
  int             consoleplayer;          // player taking events and displaying 
  int             displayplayer;          // view being displayed 
  int             gametic; 
  int             levelstarttic;          // gametic at level start 
  int             totalkills, totalitems, totalsecret;    // for intermission 
  */
  
  String            demoname; 
  boolean         demorecording; 
  public boolean         demoplayback; 
  boolean     netdemo; 
  byte[]       demobuffer;
  /** pointers */
  int       demo_p, demoend; 
  boolean         singledemo;             // quit after playing a demo from cmdline 
   
  boolean         precache = true;        // if true, load all graphics at start 
   
  /** parms for world map / intermission, peg to doomstat */
  wbstartstruct_t wminfo;                 
   
  short[][]       consistancy=new short[MAXPLAYERS][BACKUPTICS]; 
   
  byte[]       savebuffer;
   
   
  /** 
   * controls (have defaults) 
   */
  
  public int             key_right;
  public int     key_left;

  public int     key_up;
  public int     key_down; 
  public int             key_strafeleft;
  public int     key_straferight; 
  public int             key_fire;
  public int     key_use;
  public int     key_strafe;
  public int     key_speed; 
   
  public int             mousebfire; 
  public int             mousebstrafe; 
  public int             mousebforward; 
   
  public int             joybfire; 
  public int             joybstrafe; 
  public int             joybuse; 
  public int             joybspeed; 
   
   
   
  protected int MAXPLMOVE(){
      return forwardmove[1];
  }
   
  protected static final int TURBOTHRESHOLD = 0x32;

   /** fixed_t */
  int[]     forwardmove = {0x19, 0x32},
              sidemove = {0x18, 0x28}, 
   angleturn = {640, 1280, 320};    // + slow turn 

  protected static final int SLOWTURNTICS    =6; 
   
  protected static final int NUMKEYS    = 256; 

  boolean[]         gamekeydown=new boolean[NUMKEYS]; 
  int             turnheld;               // for accelerative turning 
   
  boolean[]     mousearray=new boolean[4];
  
  /** This is an alias for mousearray [1+i] */
  private boolean mousebuttons(int i){
      return mousearray[1+i];      // allow [-1]
  }
  
  private void mousebuttons(int i, boolean value){
      mousearray[1+i]=value;      // allow [-1]
  }
  
  private void mousebuttons(int i, int value){
      mousearray[1+i]=value!=0;      // allow [-1]
  }

  /** mouse values are used once */ 
  int     mousex, mousey, dclicktime, dclickstate,
          dclicks,  dclicktime2, dclickstate2, dclicks2;

  /** joystick values are repeated */ 
  int             joyxmove, joyymove;
  boolean[]         joyarray=new boolean[5]; 

  protected boolean joybuttons(int i){
      return joyarray[1+i];      // allow [-1]
  }

  protected void joybuttons(int i, boolean value){
      joyarray[1+i]=value;      // allow [-1]
  }
  
  protected void joybuttons(int i, int value){
      joyarray[1+i]=value!=0;      // allow [-1]
  }
  
  int     savegameslot; 
  String        savedescription; 
   
   
  protected static final int BODYQUESIZE= 32;

  mobj_t[]     bodyque=new mobj_t[BODYQUESIZE]; 
  int     bodyqueslot; 
   
  //void*       statcopy;               // for statistics driver

  /**
  * G_BuildTiccmd
  * Builds a ticcmd from all of the available inputs
  * or reads it from the demo buffer. 
  * If recording a demo, write it out 
  */ 
  private void BuildTiccmd (ticcmd_t cmd) 
  { 
      int     i; 
      boolean strafe;
      boolean bstrafe; 
      int     speed;
      int     tspeed; 
      int     forward;
      int     side;
      
      ticcmd_t   base=new ticcmd_t();

      //base = I_BaseTiccmd ();     // empty, or external driver
            // memcpy (cmd,base,sizeof(*cmd));
      base.copyTo(cmd);
      
      cmd.consistancy = 
      consistancy[DS.consoleplayer][DS.maketic%BACKUPTICS]; 

   
      strafe = gamekeydown[key_strafe] || mousebuttons(mousebstrafe) 
      || joybuttons(joybstrafe); 
      speed = (gamekeydown[key_speed] || joybuttons(joybspeed))?1:0;
   
      forward = side = 0;
      
      // use two stage accelerative turning
      // on the keyboard and joystick
      if (joyxmove < 0
      || joyxmove > 0  
      || gamekeydown[key_right]
      || gamekeydown[key_left]) 
      turnheld += DS.ticdup; 
      else 
      turnheld = 0; 

      if (turnheld < SLOWTURNTICS) 
      tspeed = 2;             // slow turn 
      else 
      tspeed = speed;
      
      // let movement keys cancel each other out
      if (strafe) 
      { 
      if (gamekeydown[key_right]) 
      {
          // fprintf(stderr, "strafe right\n");
          side += sidemove[speed]; 
      }
      if (gamekeydown[key_left]) 
      {
          //  fprintf(stderr, "strafe left\n");
          side -= sidemove[speed]; 
      }
      if (joyxmove > 0) 
          side += sidemove[speed]; 
      if (joyxmove < 0) 
          side -= sidemove[speed]; 
   
      } 
      else 
      { 
      if (gamekeydown[key_right]) 
          cmd.angleturn -= angleturn[tspeed]; 
      if (gamekeydown[key_left]) 
          cmd.angleturn += angleturn[tspeed]; 
      if (joyxmove > 0) 
          cmd.angleturn -= angleturn[tspeed]; 
      if (joyxmove < 0) 
          cmd.angleturn += angleturn[tspeed]; 
      } 
   
      if (gamekeydown[key_up]) 
      {
      // fprintf(stderr, "up\n");
      forward += forwardmove[speed]; 
      }
      if (gamekeydown[key_down]) 
      {
      // fprintf(stderr, "down\n");
      forward -= forwardmove[speed]; 
      }
      if (joyymove < 0) 
      forward += forwardmove[speed]; 
      if (joyymove > 0) 
      forward -= forwardmove[speed]; 
      if (gamekeydown[key_straferight]) 
      side += sidemove[speed]; 
      if (gamekeydown[key_strafeleft]) 
      side -= sidemove[speed];
      
      // buttons
      cmd.chatchar = HU.dequeueChatChar(); 
   
      if (gamekeydown[key_fire] || mousebuttons(mousebfire) 
      || joybuttons(joybfire)) 
      cmd.buttons |= BT_ATTACK; 
   
      if (gamekeydown[key_use] || joybuttons(joybuse) ) 
      { 
      cmd.buttons |= BT_USE;
      // clear double clicks if hit use button 
      dclicks = 0;                   
      } 

      // chainsaw overrides 
      for (i=0 ; i<NUMWEAPONS-1 ; i++)        
      if (gamekeydown['1'+i]) 
      { 
          cmd.buttons |= BT_CHANGE; 
          cmd.buttons |= i<<BT_WEAPONSHIFT; 
          break; 
      }
      
      // mouse
      if (mousebuttons(mousebforward)) 
      forward += forwardmove[speed];
      
      // forward double click (operator precedence? && over >
      if (mousebuttons(mousebforward) != (dclickstate!=0) && (dclicktime > 1) ) 
      { 
      dclickstate = mousebuttons(mousebforward)?1:0; 
      if (dclickstate!=0) 
          dclicks++; 
      if (dclicks == 2) 
      { 
          cmd.buttons |= BT_USE; 
          dclicks = 0; 
      } 
      else 
          dclicktime = 0; 
      } 
      else 
      { 
      dclicktime += DS.ticdup; 
      if (dclicktime > 20) 
      { 
          dclicks = 0; 
          dclickstate = 0; 
      } 
      }
      
      // strafe double click
      bstrafe = mousebuttons(mousebstrafe) || joybuttons(joybstrafe); 
      if ((bstrafe != (dclickstate2!=0)) && dclicktime2 > 1 ) 
      { 
      dclickstate2 = bstrafe?1:0; 
      if (dclickstate2!=0) 
          dclicks2++; 
      if (dclicks2 == 2) 
      { 
          cmd.buttons |= BT_USE; 
          dclicks2 = 0; 
      } 
      else 
          dclicktime2 = 0; 
      } 
      else 
      { 
      dclicktime2 += DS.ticdup; 
      if (dclicktime2 > 20) 
      { 
          dclicks2 = 0; 
          dclickstate2 = 0; 
      } 
      } 
   
      forward += mousey; 
      if (strafe) 
      side += mousex*2; 
      else 
      cmd.angleturn -= mousex*0x8; 

      mousex = mousey = 0; 
       
      if (forward > MAXPLMOVE()) 
      forward = MAXPLMOVE(); 
      else if (forward < -MAXPLMOVE()) 
      forward = -MAXPLMOVE(); 
      if (side > MAXPLMOVE()) 
      side = MAXPLMOVE(); 
      else if (side < -MAXPLMOVE()) 
      side = -MAXPLMOVE(); 
   
      cmd.forwardmove += forward; 
      cmd.sidemove += side;
      
      // special buttons
      if (sendpause) 
      { 
      sendpause = false; 
      cmd.buttons = BT_SPECIAL | BTS_PAUSE; 
      } 
   
      if (sendsave) 
      { 
      sendsave = false; 
      cmd.buttons = (char) (BT_SPECIAL | BTS_SAVEGAME | (savegameslot<<BTS_SAVESHIFT)); 
      } 
  } 
   

  //
  // G_DoLoadLevel 
  //
  //extern  gamestate_t     wipegamestate; 
   
  public void DoLoadLevel () 
  { 
      int             i; 

      // Set the sky map.
      // First thing, we have a dummy sky texture name,
      //  a flat. The data is in the WAD only because
      //  we look for an actual index, instead of simply
      //  setting one.
      DS.skyflatnum = RD.FlatNumForName ( SKYFLATNAME );

      // DOOM determines the sky texture to be used
      // depending on the current episode, and the game version.
      if (( DS.gamemode == GameMode_t.commercial)
       || ( DS.gamemission == GameMission_t.pack_tnt )
       || ( DS.gamemission == GameMission_t.pack_plut ) )
      {
      skytexture = RD.TextureNumForName ("SKY3");
      if (DS.gamemap < 12)
          skytexture = RD.TextureNumForName ("SKY1");
      else
          if (DS.gamemap < 21)
          skytexture = RD.TextureNumForName ("SKY2");
      }

      DS.levelstarttic = DS.gametic;        // for time calculation
      
      if (DS.wipegamestate == gamestate_t.GS_LEVEL) 
      DS.wipegamestate = gamestate_t.GS_MINUS_ONE;             // force a wipe 

      DS.gamestate = gamestate_t.GS_LEVEL; 

      for (i=0 ; i<MAXPLAYERS ; i++) 
      { 
      if (playeringame[i] && players[i].playerstate == PST_DEAD) 
          players[i].playerstate = PST_REBORN; 
          // I don't give a shit if it's not super-duper optimal. 
      Arrays.fill(DS.players[i].frags, 0);
 
      } 
           
      LL.SetupLevel (DS.gameepisode, DS.gamemap, 0, DS.gameskill);    
      DS.displayplayer = DS.consoleplayer;      // view the guy you are playing    
      starttime = system.GetTime (); 
      gameaction = gameaction_t.ga_nothing; 
      //Z_CheckHeap ();
      
      // clear cmd building stuff
      Arrays.fill(gamekeydown, false); 
      joyxmove = joyymove = 0; 
      mousex = mousey = 0; 
      sendpause = sendsave = paused = false; 
      Arrays.fill (mousearray, false);
      Arrays.fill(joyarray, false); 
  } 
   
   
  /**
   * G_Responder  
   * Get info needed to make ticcmd_ts for the players.
   */
  
  public boolean Responder (event_t ev) 
  { 
      // allow spy mode changes even during the demo
      if (DS.gamestate == gamestate_t.GS_LEVEL && ev.type == evtype_t.ev_keydown 
      && ev.data1 == KEY_F12 && (singledemo || !DS.deathmatch) )
      {
      // spy mode 
      do 
      { 
          DS.displayplayer++; 
          if (DS.displayplayer == MAXPLAYERS) 
          DS.displayplayer = 0; 
      } while (!playeringame[DS.displayplayer] && DS.displayplayer != DS.consoleplayer); 
      return true; 
      }
      
      // any other key pops up menu if in demos
      if (gameaction == gameaction_t.ga_nothing && !singledemo && 
      (demoplayback || DS.gamestate == gamestate_t.GS_DEMOSCREEN) 
      ) 
      { 
      if (ev.type == evtype_t.ev_keydown ||  
          (ev.type == evtype_t.ev_mouse && ev.data1!=0) || 
          (ev.type == evtype_t.ev_joystick && ev.data1!=0) ) 
      { 
          M.StartControlPanel (); 
          return true; 
      } 
      return false; 
      } 
   
      if (DS.gamestate == gamestate_t.GS_LEVEL) 
      { 
  /** 
      if (devparm && ev.type == ev_keydown && ev.data1 == ';') 
      { 
          G_DeathMatchSpawnPlayer (0); 
          return true; 
      } 
  */ 
      if (HU.Responder (ev)) 
          return true;    // chat ate the event 
      if (ST.Responder (ev)) 
          return true;    // status window ate it 
      if (AM.Responder (ev)) 
          return true;    // automap ate it 
      } 
       
      if (DS.gamestate == gamestate_t.GS_FINALE) 
      { 
      if (F.Responder (ev)) 
          return true;    // finale ate the event 
      } 
       
      switch (ev.type) 
      { 
        case ev_keydown: 
      if (ev.data1 == KEY_PAUSE) 
      { 
          sendpause = true; 
          return true; 
      } 
      if (ev.data1 <NUMKEYS) 
          gamekeydown[ev.data1] = true; 
      return true;    // eat key down events 
   
        case ev_keyup: 
      if (ev.data1 <NUMKEYS) 
          gamekeydown[ev.data1] = false; 
      return false;   // always let key up events filter down 
           
        case ev_mouse: 
      mousebuttons(0, ev.data1 & 1); 
      mousebuttons(1, ev.data1 & 2); 
      mousebuttons(2, ev.data1 & 4); 
      mousex = ev.data2*(DS.mouseSensitivity+5)/10; 
      mousey = ev.data3*(DS.mouseSensitivity+5)/10; 
      return true;    // eat events 
   
        case ev_joystick: 
      joybuttons(0, ev.data1 & 1); 
      joybuttons(1, ev.data1 & 2); 
      joybuttons(2,ev.data1 & 4); 
      joybuttons(3,ev.data1 & 8); 
      joyxmove = ev.data2; 
      joyymove = ev.data3; 
      return true;    // eat events 
   
        default: 
      break; 
      } 
   
      return false; 
  } 
   
   
  private final String turbomessage="is turbo!"; 
  
  /**
   * G_Ticker
   * Make ticcmd_ts for the players.
   */
  
  public void Ticker () 
  { 
      int     i;
      int     buf; 
      ticcmd_t   cmd;
      
      // do player reborns if needed
      for (i=0 ; i<MAXPLAYERS ; i++) 
      if (playeringame[i] && players[i].playerstate == PST_REBORN) 
          DoReborn (i);
      
      // do things to change the game state
      while (gameaction != gameaction_t.ga_nothing) 
      { 
      switch (gameaction) 
      { 
        case ga_loadlevel: 
          DoLoadLevel (); 
          break; 
        case ga_newgame: 
          DoNewGame (); 
          break; 
        case ga_loadgame: 
          DoLoadGame (); 
          break; 
        case ga_savegame: 
          DoSaveGame (); 
          break; 
        case ga_playdemo: 
          DoPlayDemo (); 
          break; 
        case ga_completed: 
          DoCompleted (); 
          break; 
        case ga_victory: 
          F.StartFinale (); 
          break; 
        case ga_worlddone: 
          DoWorldDone (); 
          break; 
        case ga_screenshot: 
          //TODO: M.ScreenShot (); 
          gameaction = gameaction_t.ga_nothing; 
          break; 
        case ga_nothing: 
          break; 
      } 
      }
      
      // get commands, check consistancy,
      // and build new consistancy check
      buf = (DS.gametic/DS.ticdup)%BACKUPTICS; 
   
      for (i=0 ; i<MAXPLAYERS ; i++)
      {
      if (playeringame[i]) 
      { 
          cmd = players[i].cmd; 
   
          //memcpy (cmd, &netcmds[i][buf], sizeof(ticcmd_t));
          DS.netcmds[i][buf].copyTo(cmd);
   
          if (demoplayback) 
          ReadDemoTiccmd (cmd); 
          if (demorecording) 
          WriteDemoTiccmd (cmd);
          
          // check for turbo cheats
          if (cmd.forwardmove > TURBOTHRESHOLD 
          && ((DS.gametic&31)==0) && ((DS.gametic>>5)&3) == i )
          {
          
          //extern char *player_names[4];
          //sprintf (turbomessage, "%s is turbo!",player_names[i]);
          players[DS.consoleplayer].message = HU.player_names[i]+turbomessage;
          }
              
          if (DS.netgame && !netdemo && !(DS.gametic%DS.ticdup) ) 
          { 
          if (DS.gametic > BACKUPTICS 
              && consistancy[i][buf] != cmd.consistancy) 
          { 
              system.Error ("consistency failure (%i should be %i)",
                   cmd.consistancy, consistancy[i][buf]); 
          } 
          if (players[i].mo!=null) 
              consistancy[i][buf] = (short) players[i].mo.x; 
          else 
              consistancy[i][buf] = RND.rndindex; 
          } 
      }
      }
      
      // check for special buttons
      for (i=0 ; i<MAXPLAYERS ; i++)
      {
      if (playeringame[i]) 
      { 
          if ((players[i].cmd.buttons & BT_SPECIAL)!=0) 
          { 
          switch (players[i].cmd.buttons & BT_SPECIALMASK) 
          { 
            case BTS_PAUSE: 
              DS.paused ^= paused; 
              if (paused) ;
              // TODO S_PauseSound (); ; 
              else ;
              // TODO: S_ResumeSound (); 
              break; 
                       
            case BTS_SAVEGAME: 
              if (savedescription!=null) 
              savedescription=new String( "NET GAME"); 
              savegameslot =  
              (players[i].cmd.buttons & BTS_SAVEMASK)>>BTS_SAVESHIFT; 
              gameaction = ga_savegame; 
              break; 
          } 
          } 
      }
      }
      
      // do main actions
      switch (gamestate) 
      { 
        case GS_LEVEL: 
      P_Ticker (); 
      ST_Ticker (); 
      AM_Ticker (); 
      HU_Ticker ();            
      break; 
       
        case GS_INTERMISSION: 
      WI_Ticker (); 
      break; 
               
        case GS_FINALE: 
      F_Ticker (); 
      break; 
   
        case GS_DEMOSCREEN: 
      D_PageTicker (); 
      break; 
      }        
  } 
   
   
  //
  // PLAYER STRUCTURE FUNCTIONS
  // also see P_SpawnPlayer in P_Things
  //

  /**
   * G_InitPlayer 
   * Called at the start.
   * Called by the game initialization functions.
   */
  
  private void InitPlayer (int player) 
  { 
      player_t   p; 
   
      // set up the saved info         
      p = players[player]; 
       
      // clear everything else to defaults 
      PlayerReborn (player); 
       
  } 
   
   

  //
  // G_PlayerFinishLevel
  // Can when a player completes a level.
  //
  private void PlayerFinishLevel (int player) 
  { 
      player_t   p; 
       
      p = players[player]; 
       
      Arrays.fill(p.powers, 0);
      Arrays.fill(p.cards,false);       
      p.mo.flags &= ~mobj_t.MF_SHADOW;     // cancel invisibility 
      p.extralight = 0;          // cancel gun flashes 
      p.fixedcolormap = 0;       // cancel ir gogles 
      p.damagecount = 0;         // no palette changes 
      p.bonuscount = 0; 
  } 
   

  //
  // G_PlayerReborn
  // Called after a player dies 
  // almost everything is cleared and initialized 
  //
  private void PlayerReborn (int player) 
  { 
      player_t   p; 
      int     i; 
      int[]     frags=new int [MAXPLAYERS]; 
      int     killcount;
      int     itemcount;
      int     secretcount; 
      
     // System.arraycopy(players[player].frags, 0, frags, 0, frags.length);
      C2JUtils.memcpy (frags,players[player].frags,frags.length); 
      killcount = players[player].killcount; 
      itemcount = players[player].itemcount; 
      secretcount = players[player].secretcount; 
      // C2JUtils.memcpy();
      
       players[player]=(player_t) player_t.nullplayer.clone();
      p=players[player];
      //p.reset();
      
      //memset (p, 0, sizeof(*p)); 
   
      C2JUtils.memcpy(players[player].frags, frags, players[player].frags.length); 
      
      players[player].killcount = killcount; 
      players[player].itemcount = itemcount; 
      players[player].secretcount = secretcount; 
   
      p.usedown = p.attackdown = true;  // don't do anything immediately 
      p.playerstate = PST_LIVE;       
      p.health[0] = MAXHEALTH; 
      p.readyweapon = p.pendingweapon = weapontype_t.wp_pistol; 
      p.weaponowned[weapontype_t.wp_fist.ordinal()] = true; 
      p.weaponowned[weapontype_t.wp_pistol.ordinal()] = true; 
      p.ammo[ammotype_t.am_clip.ordinal()] = 50; 
       
      for (i=0 ; i<NUMAMMO ; i++) 
      p.maxammo[i] = DS.maxammo[i]; 
           
  }

  //
  // G_CheckSpot  
  // Returns false if the player cannot be respawned
  // at the given mapthing_t spot  
  // because something is occupying it 
  //
  //void P_SpawnPlayer (mapthing_t* mthing); 
   
  private boolean
  CheckSpot
  ( int       playernum,
    mapthing_t   mthing ) 
  { 
      int     x,y; // fixed_t 
      subsector_t    ss; 
      int        an; // angle 
      mobj_t     mo; 
      
      
      if (players[playernum].mo==null)
      {
      // first spawn of level, before corpses
      for (int i=0 ; i<playernum ; i++)
          if (players[i].mo.x == mthing.x << FRACBITS
          && players[i].mo.y == mthing.y << FRACBITS)
          return false;   
      return true;
      }
          
      x = mthing.x << FRACBITS; 
      y = mthing.y << FRACBITS; 
       
   //TODO:   if (!CheckPosition (players[playernum].mo, x, y) ) 
 //      return false; 
   // TODO: Requires implementation of Things
      // flush an old corpse if needed 
      if (bodyqueslot >= BODYQUESIZE) 
      // TODO: P_RemoveMobj (bodyque[bodyqueslot%BODYQUESIZE]); 
      bodyque[bodyqueslot%BODYQUESIZE] = players[playernum].mo; 
      bodyqueslot++; 
      
      // spawn a teleport fog 
      ss = R.PointInSubsector (x,y); 
      an = ( ANG45 * (mthing.angle/45) ) >> ANGLETOFINESHIFT; 
   
      mo = P_SpawnMobj (x+20*finecosine[an], y+20*finesine[an] 
                , ss.sector.floorheight 
                , MT_TFOG); 
       
      if (players[DS.consoleplayer].viewz != 1) ; 
      // TODO: S_StartSound (mo, sfx_telept);  // don't start sound on first frame 
   
      return true; 
  } 


  //
  // G_DeathMatchSpawnPlayer 
  // Spawns a player at one of the random death match spots 
  // called at level load and each death 
  //
  private void DeathMatchSpawnPlayer (int playernum) 
  { 
      int             i,j; 
      int             selections; 
       
      selections = deathmatch_p - deathmatchstarts; 
      if (selections < 4) 
      I_Error ("Only %i deathmatch spots, 4 required", selections); 
   
      for (j=0 ; j<20 ; j++) 
      { 
      i = P_Random() % selections; 
      if (G_CheckSpot (playernum, &deathmatchstarts[i]) ) 
      { 
          deathmatchstarts[i].type = playernum+1; 
          P_SpawnPlayer (&deathmatchstarts[i]); 
          return; 
      } 
      } 
   
      // no good spot, so the player will probably get stuck 
      // TODO: SpawnPlayer (playerstarts[playernum]); 
  } 

  //
  // G_DoReborn 
  // 
  
  public void DoReborn (int playernum) 
  { 
      int                             i; 
       
      if (!DS.netgame)
      {
      // reload the level from scratch
      gameaction = gameaction_t.ga_loadlevel;  
      }
      else 
      {
      // respawn at the start

      // first dissasociate the corpse 
      players[playernum].mo.player = null;   
           
      // spawn at random spot if in death match 
      if (DS.deathmatch) 
      { 
          DeathMatchSpawnPlayer (playernum); 
          return; 
      } 
           
      if (CheckSpot (playernum, DS.playerstarts[playernum]) ) 
      { 
          SpawnPlayer (DS.playerstarts[playernum]); 
          return; 
      }
      
      // try to spawn at one of the other players spots 
      for (i=0 ; i<MAXPLAYERS ; i++)
      {
          if (G_CheckSpot (playernum, DS.playerstarts[i]) ) 
          { 
          DS.playerstarts[i].type = playernum+1; // fake as other player 
          P_SpawnPlayer (&playerstarts[i]); 
          playerstarts[i].type = i+1;     // restore 
          return; 
          }       
          // he's going to be inside something.  Too bad.
      }
     // TODO P_SpawnPlayer (playerstarts[playernum]); 
      } 
  } 
   
   
  private void ScreenShot () 
  { 
      gameaction = gameaction_t.ga_screenshot; 
  } 
   


 /** DOOM Par Times [4][10] */
  final int[][] pars = 
  { 
      {0}, 
      {0,30,75,120,90,165,180,180,30,165}, 
      {0,90,90,90,120,90,360,240,30,170}, 
      {0,90,45,90,150,90,90,165,30,135} 
  }; 

  /** DOOM II Par Times */
  final int[] cpars =
  {
      30,90,120,120,90,150,120,120,270,90,    //  1-10
      210,150,150,150,210,150,420,150,210,150,    // 11-20
      240,150,180,150,150,300,330,420,300,180,    // 21-30
      120,30                  // 31-32
  };
   

  //
  // G_DoCompleted 
  //
  boolean     secretexit; 
   
  private void ExitLevel () 
  { 
      secretexit = false; 
      gameaction = gameaction_t.ga_completed; 
  } 

  // Here's for the german edition.
  private void SecretExitLevel () 
  { 
      // IF NO WOLF3D LEVELS, NO SECRET EXIT!
      if ( (DS.gamemode == GameMode_t.commercial)
        && (W.CheckNumForName("map31")<0))
      secretexit = false;
      else
      secretexit = true; 
      gameaction =  gameaction_t.ga_completed; 
  } 
   
  private void DoCompleted () 
  { 
      int             i; 
       
      gameaction =  gameaction_t.ga_nothing; 
   
      for (i=0 ; i<MAXPLAYERS ; i++) 
      if (playeringame[i]) 
          PlayerFinishLevel (i);        // take away cards and stuff 
       
      if (DS.automapactive) 
      AM.Stop (); 
      
      if ( DS.gamemode != GameMode_t.commercial)
      switch(DS.gamemap)
      {
        case 8:
          gameaction =  gameaction_t.ga_victory;
          return;
        case 9: 
          for (i=0 ; i<MAXPLAYERS ; i++) 
          players[i].didsecret = true; 
          break;
      }
          
  //#if 0  Hmmm - why?
      if ( (DS.gamemap == 8)
       && (DS.gamemode != GameMode_t.commercial) ) 
      {
      // victory 
      gameaction =  gameaction_t.ga_victory; 
      return; 
      } 
       
      if ( (DS.gamemap == 9)
       && (DS.gamemode != GameMode_t.commercial) ) 
      {
      // exit secret level 
      for (i=0 ; i<MAXPLAYERS ; i++) 
          players[i].didsecret = true; 
      } 
  //#endif
      
       
      wminfo.didsecret = players[DS.consoleplayer].didsecret; 
      wminfo.epsd = DS.gameepisode -1; 
      wminfo.last = DS.gamemap -1;
      
      // wminfo.next is 0 biased, unlike gamemap
      if ( DS.gamemode == GameMode_t.commercial)
      {
      if (secretexit)
          switch(DS.gamemap)
          {
            case 15: wminfo.next = 30; break;
            case 31: wminfo.next = 31; break;
          }
      else
          switch(DS.gamemap)
          {
            case 31:
            case 32: wminfo.next = 15; break;
            default: wminfo.next = DS.gamemap;
          }
      }
      else
      {
      if (secretexit) 
          wminfo.next = 8;    // go to secret level 
      else if (DS.gamemap == 9) 
      {
          // returning from secret level 
          switch (DS.gameepisode) 
          { 
            case 1: 
          wminfo.next = 3; 
          break; 
            case 2: 
          wminfo.next = 5; 
          break; 
            case 3: 
          wminfo.next = 6; 
          break; 
            case 4:
          wminfo.next = 2;
          break;
          }                
      } 
      else 
          wminfo.next = gamemap;          // go to next level 
      }
           
      wminfo.maxkills = totalkills; 
      wminfo.maxitems = totalitems; 
      wminfo.maxsecret = totalsecret; 
      wminfo.maxfrags = 0; 
      if ( DS.gamemode == commercial )
      wminfo.partime = 35*cpars[gamemap-1]; 
      else
      wminfo.partime = 35*pars[gameepisode][gamemap]; 
      wminfo.pnum = consoleplayer; 
   
      for (i=0 ; i<MAXPLAYERS ; i++) 
      { 
      wminfo.plyr[i].in = playeringame[i]; 
      wminfo.plyr[i].skills = players[i].killcount; 
      wminfo.plyr[i].sitems = players[i].itemcount; 
      wminfo.plyr[i].ssecret = players[i].secretcount; 
      wminfo.plyr[i].stime = leveltime; 
      memcpy (wminfo.plyr[i].frags, players[i].frags 
          , sizeof(wminfo.plyr[i].frags)); 
      } 
   
      gamestate = GS_INTERMISSION; 
      viewactive = false; 
      automapactive = false; 
   
      if (statcopy)
      memcpy (statcopy, &wminfo, sizeof(wminfo));
      
      WI_Start (&wminfo); 
  } 


  //
  // G_WorldDone 
  //
  private void WorldDone () 
  { 
      gameaction = gameaction_t.ga_worlddone; 

      if (secretexit) 
      players[DS.consoleplayer].didsecret = true; 

      if ( DS.gamemode == GameMode_t.commercial )
      {
      switch (DS.gamemap)
      {
        case 15:
        case 31:
          if (!secretexit)
          break;
        case 6:
        case 11:
        case 20:
        case 30:
          F.StartFinale ();
          break;
      }
      }
  } 
   
  public void DoWorldDone () 
  {        
      DS.gamestate = gamestate_t.GS_LEVEL; 
      DS.gamemap = wminfo.next+1; 
      DoLoadLevel (); 
      gameaction = gameaction_t.ga_nothing; 
      DS.viewactive = true; 
  } 
   


  //
  // G_InitFromSavegame
  // Can be called by the startup code or the menu task. 
  //
  //extern boolean setsizeneeded;
  //void R_ExecuteSetViewSize (void);

  String    savename;

  private void LoadGame (String name) 
  { 
      savename=new String(name); 
      gameaction = gameaction_t.ga_loadgame; 
  } 
   
  

  /** This is fugly. Making a "savegame object" will make at least certain comparisons
   *  easier, and avoid writing code twice.
   */

  private void DoLoadGame () 
  { 
      int     length; 
      int     i; 
      int     a,b,c; 
      char[]    vcheck=new char[VERSIONSIZE]; 
      StringBuffer buf=new StringBuffer();
      DoomSaveGame dsg;
      
      gameaction = gameaction_t.ga_nothing; 
       
      length = M.ReadFile (savename, savebuffer); 
      save_p = SAVESTRINGSIZE;
      
      // skip the description field 
      //memset (vcheck,0,sizeof(vcheck));
      //sprintf (vcheck,"version %i",VERSION);
      buf.append("version ");
      buf.append(VERSION);
      buf.getChars(0, buf.length(), vcheck, 0);
 
      if (C2JUtils.strcmp (save_p, vcheck)) 
      return;             // bad version 
      save_p += VERSIONSIZE; 
               
      gameskill = *save_p++; 
      gameepisode = *save_p++; 
      gamemap = *save_p++; 
      for (i=0 ; i<MAXPLAYERS ; i++) 
      playeringame[i] = *save_p++; 

      // load a base level 
      G_InitNew (gameskill, gameepisode, gamemap); 
   
      // get the times 
      a = *save_p++; 
      b = *save_p++; 
      c = *save_p++; 
      leveltime = (a<<16) + (b<<8) + c; 
       
      // dearchive all the modifications
      P_UnArchivePlayers (); 
      P_UnArchiveWorld (); 
      P_UnArchiveThinkers (); 
      P_UnArchiveSpecials (); 
   
      if (*save_p != 0x1d) 
      I_Error ("Bad savegame");
      
      // done 
      Z_Free (savebuffer); 
   
      if (setsizeneeded)
      R_ExecuteSetViewSize ();
      
      // draw the pattern into the back screen
      R_FillBackScreen ();   
  } 
   

  //
  // G_SaveGame
  // Called by the menu task.
  // Description is a 24 byte text string 
  //
  private void
  SaveGame
  ( int   slot,
    String description ) 
  { 
      savegameslot = slot; 
      savedescription=new String(description); 
      sendsave = true; 
  } 
   
  private void DoSaveGame () 
  { 
      String    name; 
      char[]    name2=new char[VERSIONSIZE]; 
      String   description; 
      int     length; 
      int     i; 
      
      if (M_CheckParm("-cdrom"))
      name="c:\\doomdata\\"+SAVEGAMENAME+"%d.dsg");
      else
      sprintf (name,SAVEGAMENAME"%d.dsg",savegameslot); 
      description = savedescription; 
       
      save_p = savebuffer = screens[1]+0x4000; 
       
      memcpy (save_p, description, SAVESTRINGSIZE); 
      save_p += SAVESTRINGSIZE; 
      memset (name2,0,sizeof(name2)); 
      sprintf (name2,"version %i",VERSION); 
      memcpy (save_p, name2, VERSIONSIZE); 
      save_p += VERSIONSIZE; 
       
      *save_p++ = gameskill; 
      *save_p++ = gameepisode; 
      *save_p++ = gamemap; 
      for (i=0 ; i<MAXPLAYERS ; i++) 
      *save_p++ = playeringame[i]; 
      *save_p++ = leveltime>>16; 
      *save_p++ = leveltime>>8; 
      *save_p++ = leveltime; 
   
      P_ArchivePlayers (); 
      P_ArchiveWorld (); 
      P_ArchiveThinkers (); 
      P_ArchiveSpecials (); 
       
      *save_p++ = 0x1d;       // consistancy marker 
       
      length = save_p - savebuffer; 
      if (length > SAVEGAMESIZE) 
      I_Error ("Savegame buffer overrun"); 
      M_WriteFile (name, savebuffer, length); 
      gameaction = ga_nothing; 
      savedescription[0] = 0;      
       
      players[consoleplayer].message = GGSAVED; 

      // draw the pattern into the back screen
      R_FillBackScreen ();    
  } 
   

  //
  // G_InitNew
  // Can be called by the startup code or the menu task,
  // consoleplayer, displayplayer, playeringame[] should be set. 
  //
  skill_t d_skill; 
  int     d_episode; 
  int     d_map; 
   
  void
  G_DeferedInitNew
  ( skill_t   skill,
    int       episode,
    int       map) 
  { 
      d_skill = skill; 
      d_episode = episode; 
      d_map = map; 
      gameaction = ga_newgame; 
  } 


  void G_DoNewGame (void) 
  {
      demoplayback = false; 
      netdemo = false;
      netgame = false;
      deathmatch = false;
      playeringame[1] = playeringame[2] = playeringame[3] = 0;
      respawnparm = false;
      fastparm = false;
      nomonsters = false;
      consoleplayer = 0;
      G_InitNew (d_skill, d_episode, d_map); 
      gameaction = ga_nothing; 
  } 

  // The sky texture to be used instead of the F_SKY1 dummy.
  extern  int skytexture; 


  void
  G_InitNew
  ( skill_t   skill,
    int       episode,
    int       map ) 
  { 
      int             i; 
       
      if (paused) 
      { 
      paused = false; 
      S_ResumeSound (); 
      } 
      

      if (skill > sk_nightmare) 
      skill = sk_nightmare;


      // This was quite messy with SPECIAL and commented parts.
      // Supposedly hacks to make the latest edition work.
      // It might not work properly.
      if (episode < 1)
        episode = 1; 

      if ( DS.gamemode == retail )
      {
        if (episode > 4)
      episode = 4;
      }
      else if ( DS.gamemode == shareware )
      {
        if (episode > 1) 
         episode = 1; // only start episode 1 on shareware
      }  
      else
      {
        if (episode > 3)
      episode = 3;
      }
      

    
      if (map < 1) 
      map = 1;
      
      if ( (map > 9)
       && ( DS.gamemode != commercial) )
        map = 9; 
           
      M_ClearRandom (); 
       
      if (skill == sk_nightmare || respawnparm )
      respawnmonsters = true;
      else
      respawnmonsters = false;
          
      if (fastparm || (skill == sk_nightmare && gameskill != sk_nightmare) )
      { 
      for (i=S_SARG_RUN1 ; i<=S_SARG_PAIN2 ; i++) 
          states[i].tics >>= 1; 
      mobjinfo[MT_BRUISERSHOT].speed = 20*FRACUNIT; 
      mobjinfo[MT_HEADSHOT].speed = 20*FRACUNIT; 
      mobjinfo[MT_TROOPSHOT].speed = 20*FRACUNIT; 
      } 
      else if (skill != sk_nightmare && gameskill == sk_nightmare) 
      { 
      for (i=S_SARG_RUN1 ; i<=S_SARG_PAIN2 ; i++) 
          states[i].tics <<= 1; 
      mobjinfo[MT_BRUISERSHOT].speed = 15*FRACUNIT; 
      mobjinfo[MT_HEADSHOT].speed = 10*FRACUNIT; 
      mobjinfo[MT_TROOPSHOT].speed = 10*FRACUNIT; 
      } 
       
               
      // force players to be initialized upon first level load         
      for (i=0 ; i<MAXPLAYERS ; i++) 
      players[i].playerstate = PST_REBORN; 
   
      usergame = true;                // will be set false if a demo 
      paused = false; 
      demoplayback = false; 
      automapactive = false; 
      viewactive = true; 
      gameepisode = episode; 
      gamemap = map; 
      gameskill = skill; 
   
      viewactive = true;
      
      // set the sky map for the episode
      if ( DS.gamemode == commercial)
      {
      skytexture = R_TextureNumForName ("SKY3");
      if (gamemap < 12)
          skytexture = R_TextureNumForName ("SKY1");
      else
          if (gamemap < 21)
          skytexture = R_TextureNumForName ("SKY2");
      }
      else
      switch (episode) 
      { 
        case 1: 
          skytexture = R_TextureNumForName ("SKY1"); 
          break; 
        case 2: 
          skytexture = R_TextureNumForName ("SKY2"); 
          break; 
        case 3: 
          skytexture = R_TextureNumForName ("SKY3"); 
          break; 
        case 4:   // Special Edition sky
          skytexture = R_TextureNumForName ("SKY4");
          break;
      } 
   
      G_DoLoadLevel (); 
  } 
   

  //
  // DEMO RECORDING 
  // 
  protected static final int DEMOMARKER =0x80;

public int skytexture;      


 public  void ReadDemoTiccmd (ticcmd_t cmd) 
  { 
      if (demobuffer[demo_p] == DEMOMARKER) 
      {
      // end of demo data stream 
      CheckDemoStatus (); 
      return; 
      } 
      cmd.forwardmove = (demobuffer[demo_p++]); 
      cmd.sidemove = (demobuffer[demo_p++]); 
      cmd.angleturn = (short) (C2JUtils.toUnsignedByte(demobuffer[demo_p++])<<8); 
      cmd.buttons = (char) (C2JUtils.toUnsignedByte(demobuffer[demo_p++])); 
  } 


  public void WriteDemoTiccmd (ticcmd_t cmd) 
  { 
      if (gamekeydown['q'])           // press q to end demo recording 
      CheckDemoStatus (); 
      demobuffer[demo_p++] = cmd.forwardmove; 
      demobuffer[demo_p++] = cmd.sidemove; 
      demobuffer[demo_p++] = (byte) ((cmd.angleturn+128)>>8); 
      demobuffer[demo_p++] = (byte) cmd.buttons; 
      demo_p -= 4; 
      if (demo_p > demoend - 16)
      {
      // no more space 
      CheckDemoStatus (); 
      return; 
      } 
      
      ReadDemoTiccmd (cmd);         // make SURE it is exactly the same 
  } 
   
   
   
  /**
  * G_RecordDemo 
  */ 
  public void RecordDemo (String name) 
  { 
      int             i; 
      int             maxsize;
      
      StringBuffer buf=new StringBuffer();
      DS.usergame = false; 
      buf.append(name); 
      buf.append(".lmp");
      demoname=buf.toString();
      maxsize = 0x20000;
      i = M.CheckParm ("-maxdemo");
      if (i!=0 && i<M.myargc-1)
      maxsize = Integer.parseInt(M.myargv[i+1])*1024;
      demobuffer = new byte[maxsize]; 
      demoend = maxsize;
       
      demorecording = true; 
  } 
   
   
  public void G_BeginRecording () 
  { 
      int             i; 
          
      demo_p = 0;
      
      demobuffer[demo_p++] = (byte) VERSION;
      demobuffer[demo_p++] = (byte) DS.gameskill.ordinal(); 
      demobuffer[demo_p++] = (byte) DS.gameepisode; 
      demobuffer[demo_p++] = (byte) DS.gamemap; 
      demobuffer[demo_p++] = (byte) ((DS.deathmatch)?1:0); 
      demobuffer[demo_p++] = (byte) (DS.respawnparm?1:0);
      demobuffer[demo_p++] = (byte) (DS.fastparm?1:0);
      demobuffer[demo_p++] = (byte) (DS.nomonsters?1:0);
      demobuffer[demo_p++] = (byte) DS.consoleplayer;
       
      for (i=0 ; i<MAXPLAYERS ; i++) 
      demobuffer[demo_p++] = (byte) (playeringame[i]?1:0);         
  } 
   



  String   defdemoname;



   
  /**
   * G_PlayDemo 
   */
  
  public void DeferedPlayDemo (String name) 
  { 
      defdemoname = name; 
      gameaction = gameaction_t.ga_playdemo; 
  } 
   
  public void DoPlayDemo () 
  { 
      skill_t skill; 
      int             i, episode, map; 
       
      gameaction = gameaction_t.ga_nothing; 
      demobuffer = W.CacheLumpNameAsRawBytes(defdemoname, PU_STATIC);
      demo_p = 0;
      if ( demobuffer[demo_p]++ != VERSION)
      {
        System.err.println("Demo is from a different game version!\n");
        gameaction = gameaction_t.ga_nothing;
        return;
      }
      
      skill = skill_t.values()[demobuffer[demo_p++]]; 
      episode = demobuffer[demo_p++]; 
      map = demobuffer[demo_p++]; 
      DS.deathmatch = demobuffer[demo_p++]==0;
      DS.respawnparm = demobuffer[demo_p++]==0;
      DS.fastparm = demobuffer[demo_p++]==0;
      DS.nomonsters = demobuffer[demo_p++]==0;
      DS.consoleplayer = demobuffer[demo_p++];
      
      for (i=0 ; i<MAXPLAYERS ; i++) 
      playeringame[i] = demobuffer[demo_p++]==0; 
      if (playeringame[1]) 
      { 
      DS.netgame = true; 
      netdemo = true; 
      }

      // don't spend a lot of time in loadlevel 
      precache = false;
      InitNew (skill, episode, map); 
      precache = true; 

      usergame = false; 
      demoplayback = true; 
  } 

  //
  // G_TimeDemo 
  //
  public void TimeDemo (String name) 
  {    
      DS.nodrawers = M.CheckParm ("-nodraw")!=0; 
      DS.noblit = M.CheckParm ("-noblit")!=0; 
      timingdemo = true; 
      DS.singletics = true; 

      defdemoname = name; 
      gameaction = gameaction_t.ga_playdemo; 
  } 
   
   
  /**
  =================== 
  = 
  = G_CheckDemoStatus 
  = 
  = Called after a death or level completion to allow demos to be cleaned up 
  = Returns true if a new demo loop action will take place 
  =================== 
  */ 
   
  private boolean CheckDemoStatus () 
  { 
      int             endtime; 
       
      if (timingdemo) 
      { 
      endtime = system.GetTime (); 
      system.Error ("timed %i gametics in %i realtics",DS.gametic 
           , endtime-starttime); 
      } 
       
      if (demoplayback) 
      { 
      if (singledemo) 
          system.I_Quit (); 
               
     // Z_ChangeTag (demobuffer, PU_CACHE); 
      demoplayback = false; 
      netdemo = false;
      DS.netgame = false;
      DS.deathmatch = false;
      playeringame[1] = playeringame[2] = playeringame[3] = false;
      DS.respawnparm = false;
      DS.fastparm = false;
      DS.nomonsters = false;
      DS.consoleplayer = 0;
      AdvanceDemo (); 
      return true; 
      } 
   
      if (demorecording) 
      { 
      demobuffer[demo_p++] = DEMOMARKER; 
      M_WriteFile (demoname, demobuffer, demo_p - demobuffer); 
      Z_Free (demobuffer); 
      demorecording = false; 
      I_Error ("Demo %s recorded",demoname); 
      } 
       
      return false; 
  } 
   
   
   
public DoomGame(DoomContext DC){
    this.DS=DC.DS;
    // Pegged for convenience... or viceversa
    this.playeringame=DS.playeringame;
    this.players=DS.players;
    this.playerstarts=DS.playerstarts;
    this.de
    //this.DS.DS.gamemode=DS.DS.gamemode;
}
    
}
