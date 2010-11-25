package p;

import static p.ChaseDirections.*;
import static p.DoorDefines.*;
import static data.Defines.BASETHRESHOLD;
import static data.Defines.BT_ATTACK;
import static data.Defines.FLOATSPEED;
import static data.Defines.ITEMQUESIZE;
import static data.Defines.MAPBLOCKSHIFT;
import static data.Defines.MAPBLOCKSIZE;
import static data.Defines.MAPBTOFRAC;
import static data.Defines.MELEERANGE;
import static data.Defines.MISSILERANGE;
import static data.Defines.ML_BLOCKING;
import static data.Defines.ML_BLOCKMONSTERS;
import static data.Defines.ML_SECRET;
import static data.Defines.ML_TWOSIDED;
import static data.Defines.MTF_AMBUSH;
import static data.Defines.NUMCARDS;
import static data.Defines.ONCEILINGZ;
import static data.Defines.ONFLOORZ;
import static data.Defines.PST_DEAD;
import static data.Defines.PST_LIVE;
import static data.Defines.PST_REBORN;
import static data.Defines.PT_ADDLINES;
import static data.Defines.PT_ADDTHINGS;
import static data.Defines.PT_EARLYOUT;
import static data.Defines.USERANGE;
import static data.Defines.VIEWHEIGHT;
import static data.Defines.acp1;
import static data.Defines.pw_invulnerability;
import static data.Defines.pw_strength;
import static data.Limits.CEILSPEED;
import static data.Limits.MAXBUTTONS;
import static data.Limits.MAXCEILINGS;
import static data.Limits.MAXINT;
import static data.Limits.MAXMOVE;
import static data.Limits.MAXPLATS;
import static data.Limits.MAXPLAYERS;
import static data.Limits.MAXRADIUS;
import static data.Limits.MAXSKULLS;
import static data.Limits.MAXSPECIALCROSS;
import static data.Limits.NUMBRAINTARGETS;
import static data.Limits.NUMMOBJTYPES;
import static data.Tables.*;
import static data.info.mobjinfo;
import static data.info.states;
import static doom.englsh.PD_BLUEK;
import static doom.englsh.PD_BLUEO;
import static doom.englsh.PD_REDK;
import static doom.englsh.PD_REDO;
import static doom.englsh.PD_YELLOWK;
import static doom.englsh.PD_YELLOWO;
import static doom.items.weaponinfo;
import static doom.player_t.LOWERSPEED;
import static doom.player_t.RAISESPEED;
import static doom.player_t.WEAPONBOTTOM;
import static doom.player_t.WEAPONTOP;
import static doom.player_t.ps_flash;
import static doom.player_t.ps_weapon;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static p.ChaseDirections.DI_EAST;
import static p.ChaseDirections.DI_NODIR;
import static p.ChaseDirections.DI_NORTH;
import static p.ChaseDirections.DI_SOUTH;
import static p.ChaseDirections.DI_SOUTHEAST;
import static p.ChaseDirections.DI_WEST;
import static p.ChaseDirections.diags;
import static p.ChaseDirections.opposite;
import static p.MapUtils.AproxDistance;
import static utils.C2JUtils.*;

import static p.mobj_t.*;
import p.UnifiedGameMap.Lights;
import i.DoomStatusAware;
import i.DoomSystemInterface;
import rr.line_t;
import rr.sector_t;
import rr.side_t;
import rr.subsector_t;
import st.StatusBar;
import data.Tables;
import data.mapthing_t;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.state_t;
import data.Defines.GameMode_t;
import data.Defines.card_t;
import data.Defines.skill_t;
import data.Defines.slopetype_t;
import data.Defines.statenum_t;
import data.sounds.sfxenum_t;
import doom.DoomContext;
import doom.DoomMain;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;
import doom.weapontype_t;

/** Action functions need to be aware of:
    *  Random number generator (RND)
    *  Broader gameplay context (DoomGame)
    *  Some door actions
    *  Playfield checks.
    *  etc. etc. so they COULD be made into a separate file
    * 
    * @author admin
    *
    */

public class Actions extends UnifiedGameMap implements DoomStatusAware{
  // plasma cells for a bfg attack
  private static int BFGCELLS      =  40;      

      
      //
      // CEILINGS
      //


      ceiling_t[]  activeceilings=new ceiling_t[MAXCEILINGS];


      /**
       * T_MoveCeiling
       */

      void MoveCeiling (ceiling_t ceiling)
      {
          result_e    res;
          
          switch(ceiling.direction)
          {
            case 0:
          // IN STASIS
          break;
            case 1:
          // UP
          res = MovePlane(ceiling.sector,
                    ceiling.speed,
                    ceiling.topheight,
                    false,1,ceiling.direction);
          
          if (!flags(DM.leveltime,7))
          {
              switch(ceiling.type)
              {
                case silentCrushAndRaise:
              break;
                default:
              S.StartSound(ceiling.sector.soundorg,sfxenum_t.sfx_stnmov);
                       
              // ?
              break;
              }
          }
          
          if (res == result_e.pastdest)
          {
              switch(ceiling.type)
              {
                case raiseToHighest:
              RemoveActiveCeiling(ceiling);
              break;
              
                case silentCrushAndRaise:
              ; // TODO:((mobj_t *)&ceiling.sector.soundorg, sfx_pstop);
                case fastCrushAndRaise:
                case crushAndRaise:
              ceiling.direction = -1;
              break;
              
                default:
              break;
              }
              
          }
          break;
          
            case -1:
          // DOWN
          res = MovePlane(ceiling.sector,
                    ceiling.speed,
                    ceiling.bottomheight,
                    ceiling.crush,1,ceiling.direction);
          
          if (!flags(DM.leveltime,7))
          {
              switch(ceiling.type)
              {
                case silentCrushAndRaise: break;
                default:
                	S.StartSound(ceiling.sector.soundorg,  sfxenum_t.sfx_stnmov);
                     
              }
          }
          
          if (res == result_e.pastdest)
          {
              switch(ceiling.type)
              {
                case silentCrushAndRaise:
                	S.StartSound(ceiling.sector.soundorg, sfxenum_t.sfx_pstop);
                case crushAndRaise:
              ceiling.speed = CEILSPEED;
                case fastCrushAndRaise:
              ceiling.direction = 1;
              break;

                case lowerAndCrush:
                case lowerToFloor:
              RemoveActiveCeiling(ceiling);
              break;

                default:
              break;
              }
          }
          else // ( res != result_e.pastdest )
          {
              if (res == result_e.crushed)
              {
              switch(ceiling.type)
              {
                case silentCrushAndRaise:
                case crushAndRaise:
                case lowerAndCrush:
                  ceiling.speed = CEILSPEED / 8;
                  break;

                default:
                  break;
              }
              }
          }
          break;
          }
      }


      

      //
      // Add an active ceiling
      //
      private void AddActiveCeiling(ceiling_t c)
      {
          int     i;
          
          for (i = 0; i < MAXCEILINGS;i++)
          {
          if (activeceilings[i] == null)
          {
              activeceilings[i] = c;
              return;
          }
          }
      }



      //
      // Remove a ceiling's thinker
      //
      void RemoveActiveCeiling(ceiling_t c)
      {
          int     i;
          
          for (i = 0;i < MAXCEILINGS;i++)
          {
          if (activeceilings[i] == c)
          {
              activeceilings[i].sector.specialdata = null;
              RemoveThinker (activeceilings[i]);
              activeceilings[i] = null;
              break;
          }
          }
      }



      //
      // Restart a ceiling that's in-stasis
      //
      void ActivateInStasisCeiling(line_t line)
      {
          int     i;
          
          for (i = 0;i < MAXCEILINGS;i++)
          {
          if (activeceilings[i]!=null
              && (activeceilings[i].tag == line.tag)
              && (activeceilings[i].direction == 0))
          {
              activeceilings[i].direction = activeceilings[i].olddirection;
              activeceilings[i].function
                = think_t.T_MoveCeiling;
          }
          }
      }
      
      

      /**
       * MOVE A FLOOR TO IT'S DESTINATION (UP OR DOWN)
       *
       */
      void MoveFloor(floormove_t floor)
      {
          result_e    res;
          
          res = MovePlane(floor.sector,
                    floor.speed,
                    floor.floordestheight,
                    floor.crush,0,floor.direction);
          
          if (!flags(DM.leveltime,7))
              
        	  S.StartSound(floor.sector.soundorg,  sfxenum_t.sfx_stnmov);
          
          if (res == result_e.pastdest)
          {
          floor.sector.specialdata = null;

          if (floor.direction == 1)
          {
              switch(floor.type)
              {
                case donutRaise:
              floor.sector.special = (short) floor.newspecial;
              floor.sector.floorpic = floor.texture;
                default:
              break;
              }
          }
          else if (floor.direction == -1)
          {
              switch(floor.type)
              {
                case lowerAndChange:
              floor.sector.special = (short) floor.newspecial;
              floor.sector.floorpic = floor.texture;
                default:
              break;
              }
          }
          RemoveThinker(floor);

          S.StartSound(floor.sector.soundorg, sfxenum_t.sfx_pstop);
          }

      }

      //
      // EV.DoCeiling
      // Move a ceiling up/down and all around!
      //
      boolean
      DoCeiling
      ( line_t   line,
        ceiling_e type )
      {
          int     secnum=-1;
          boolean     rtn=false;
          sector_t   sec;
          ceiling_t  ceiling;
                   
          //  Reactivate in-stasis ceilings...for certain types.
          switch(type)
          {
            case fastCrushAndRaise:
            case silentCrushAndRaise:
            case crushAndRaise:
          ActivateInStasisCeiling(line);
            default:
          break;
          }
          
          while ((secnum = FindSectorFromLineTag(line,secnum)) >= 0)
          {
          sec = LL.sectors[secnum];
          if (sec.specialdata!=null)
              continue;
          
          // new door thinker
          rtn = true;
          ceiling = new ceiling_t();
          AddThinker (ceiling);
          sec.specialdata = ceiling;
          ceiling.function = think_t.T_MoveCeiling;
          ceiling.sector = sec;
          ceiling.crush = false;
          
          switch(type)
          {
            case fastCrushAndRaise:
              ceiling.crush = true;
              ceiling.topheight = sec.ceilingheight;
              ceiling.bottomheight = sec.floorheight + (8*FRACUNIT);
              ceiling.direction = -1;
              ceiling.speed = CEILSPEED * 2;
              break;

            case silentCrushAndRaise:
            case crushAndRaise:
              ceiling.crush = true;
              ceiling.topheight = sec.ceilingheight;
            case lowerAndCrush:
            case lowerToFloor:
              ceiling.bottomheight = sec.floorheight;
              if (type != ceiling_e.lowerToFloor)
              ceiling.bottomheight += 8*FRACUNIT;
              ceiling.direction = -1;
              ceiling.speed = CEILSPEED;
              break;

            case raiseToHighest:
              ceiling.topheight = sec.FindHighestCeilingSurrounding();
              ceiling.direction = 1;
              ceiling.speed = CEILSPEED;
              break;
          }
              
          ceiling.tag = sec.tag;
          ceiling.type = type;
          AddActiveCeiling(ceiling);
          }
          return rtn;
      }

      
      /**
       * Special Stuff that can not be categorized
       * 
       * (I'm sure it has something to do with John Romero's obsession
       *  with fucking stuff and making them his bitches).
       *
       *@param line
       * 
       */
      boolean DoDonut(line_t  line)
      {
          sector_t       s1;
          sector_t       s2;
          sector_t       s3;
          int         secnum;
          boolean         rtn;
          int         i;
          floormove_t    floor;
          
          secnum = -1;
          rtn = false;
          while ((secnum = FindSectorFromLineTag(line,secnum)) >= 0)
          {
          s1 = LL.sectors[secnum];
              
          // ALREADY MOVING?  IF SO, KEEP GOING...
          if (s1.specialdata!=null)
              continue;
                  
          rtn = true;
          s2 = s1.lines[0].getNextSector(s1);
          for (i = 0;i < s2.linecount;i++)
          {
              if ((!flags(s2.lines[i].flags , ML_TWOSIDED)) ||
              (s2.lines[i].backsector == s1))
              continue;
              s3 = s2.lines[i].backsector;
              
              //  Spawn rising slime
              floor = new floormove_t();
              AddThinker (floor);
              s2.specialdata = floor;
              floor.function = think_t.T_MoveFloor;
              floor.type = floor_e.donutRaise;
              floor.crush = false;
              floor.direction = 1;
              floor.sector = s2;
              floor.speed = FLOORSPEED / 2;
              floor.texture = s3.floorpic;
              floor.newspecial = 0;
              floor.floordestheight = s3.floorheight;
              
              //  Spawn lowering donut-hole
              floor = new floormove_t();
              AddThinker (floor);
              s1.specialdata = floor;
              floor.function = think_t.T_MoveFloor;
              floor.type = floor_e.lowerFloor;
              floor.crush = false;
              floor.direction = -1;
              floor.sector = s1;
              floor.speed = FLOORSPEED / 2;
              floor.floordestheight = s3.floorheight;
              break;
          }
          }
          return rtn;
      }
      
      //
      // HANDLE FLOOR TYPES
      //
      boolean
      DoFloor
      ( line_t   line,
        floor_e   floortype )
      {
          int         secnum=-1;
          boolean         rtn=false;
          int         i;
          sector_t       sec;
          floormove_t    floor;

          while ((secnum = FindSectorFromLineTag(line,secnum)) >= 0)
          {
          sec = LL.sectors[secnum];
              
          // ALREADY MOVING?  IF SO, KEEP GOING...
          if (sec.specialdata!=null)
              continue;
          
          // new floor thinker
          rtn = true;
          floor = new floormove_t();
          AddThinker (floor);
          sec.specialdata = floor;
          floor.function = think_t.T_MoveFloor;
          floor.type = floortype;
          floor.crush = false;

          switch(floortype)
          {
            case lowerFloor:
              floor.direction = -1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = 
              sec.FindHighestFloorSurrounding();
              break;

            case lowerFloorToLowest:
              floor.direction = -1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = 
              sec.FindLowestFloorSurrounding();
              break;

            case turboLower:
              floor.direction = -1;
              floor.sector = sec;
              floor.speed = FLOORSPEED * 4;
              floor.floordestheight = 
              sec.FindHighestFloorSurrounding();
              if (floor.floordestheight != sec.floorheight)
              floor.floordestheight += 8*FRACUNIT;
              break;

            case raiseFloorCrush:
              floor.crush = true;
            case raiseFloor:
              floor.direction = 1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = 
              sec.FindLowestCeilingSurrounding();
              if (floor.floordestheight > sec.ceilingheight)
              floor.floordestheight = sec.ceilingheight;
              floor.floordestheight -= (8*FRACUNIT)*
              eval(floortype == floor_e.raiseFloorCrush);
              break;

            case raiseFloorTurbo:
              floor.direction = 1;
              floor.sector = sec;
              floor.speed = FLOORSPEED*4;
              floor.floordestheight = 
              sec.FindNextHighestFloor(sec.floorheight);
              break;

            case raiseFloorToNearest:
              floor.direction = 1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = 
              sec.FindNextHighestFloor(sec.floorheight);
              break;

            case raiseFloor24:
              floor.direction = 1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = floor.sector.floorheight +
              24 * FRACUNIT;
              break;
            case raiseFloor512:
              floor.direction = 1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = floor.sector.floorheight +
              512 * FRACUNIT;
              break;

            case raiseFloor24AndChange:
              floor.direction = 1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = floor.sector.floorheight +
              24 * FRACUNIT;
              sec.floorpic = line.frontsector.floorpic;
              sec.special = line.frontsector.special;
              break;

            case raiseToTexture:
            {
                int   minsize = MAXINT;
                side_t   side;
                      
                floor.direction = 1;
                floor.sector = sec;
                floor.speed = FLOORSPEED;
                for (i = 0; i < sec.linecount; i++)
                {
                if (twoSided (secnum, i) )
                {
                    side = getSide(secnum,i,0);
                    if (side.bottomtexture >= 0)
                    if (TM.getTextureheight(side.bottomtexture) < 
                        minsize)
                        minsize = 
                        TM.getTextureheight(side.bottomtexture);
                    side = getSide(secnum,i,1);
                    if (side.bottomtexture >= 0)
                    if (TM.getTextureheight(side.bottomtexture) < 
                        minsize)
                        minsize = 
                        TM.getTextureheight(side.bottomtexture);
                }
                }
                floor.floordestheight =
                floor.sector.floorheight + minsize;
            }
            break;
            
            case lowerAndChange:
              floor.direction = -1;
              floor.sector = sec;
              floor.speed = FLOORSPEED;
              floor.floordestheight = 
              sec.FindLowestFloorSurrounding();
              floor.texture = sec.floorpic;

              for (i = 0; i < sec.linecount; i++)
              {
              if ( twoSided(secnum, i) )
              {
                  if (getSide(secnum,i,0).sector.id == secnum)
                  {
                  sec = getSector(secnum,i,1);

                  if (sec.floorheight == floor.floordestheight)
                  {
                      floor.texture = sec.floorpic;
                      floor.newspecial = sec.special;
                      break;
                  }
                  }
                  else
                  {
                  sec = getSector(secnum,i,0);

                  if (sec.floorheight == floor.floordestheight)
                  {
                      floor.texture = sec.floorpic;
                      floor.newspecial = sec.special;
                      break;
                  }
                  }
              }
              }
            default:
              break;
          }
          }
          return rtn;
      }

      //
      // EV_CeilingCrushStop
      // Stop a ceiling from crushing!
      //
      int CeilingCrushStop(line_t  line)
      {
          int     i;
          int     rtn;
          
          rtn = 0;
          for (i = 0;i < MAXCEILINGS;i++)
          {
          if (activeceilings[i]!=null
              && (activeceilings[i].tag == line.tag)
              && (activeceilings[i].direction != 0))
          {
              activeceilings[i].olddirection = activeceilings[i].direction;
              activeceilings[i].function = null;
              activeceilings[i].direction = 0;       // in-stasis
              rtn = 1;
          }
          }
          

          return rtn;
      }


      //
      // BUILD A STAIRCASE!
      //
      boolean
      BuildStairs
      ( line_t   line,
        stair_e   type )
      {
          int         secnum;
          int         height;
          int         i;
          int         newsecnum;
          int         texture;
          boolean         ok;
          boolean         rtn;
          
          sector_t       sec;
          sector_t       tsec;

          floormove_t    floor;
          
          int     stairsize = 0; 
          int     speed=0; // shut up compiler

          secnum = -1;
          rtn = false;
          while ((secnum = FindSectorFromLineTag(line,secnum)) >= 0)
          {
          sec = LL.sectors[secnum];
              
          // ALREADY MOVING?  IF SO, KEEP GOING...
          if (sec.specialdata!=null)
              continue;
          
          // new floor thinker
          rtn = true;
          floor = new floormove_t ();
          AddThinker (floor);
          sec.specialdata = floor;
          floor.function = think_t.T_MoveFloor;
          floor.direction = 1;
          floor.sector = sec;
          switch(type)
          {
            case build8:
              speed = FLOORSPEED/4;
              stairsize = 8*FRACUNIT;
              break;
            case turbo16:
              speed = FLOORSPEED*4;
              stairsize = 16*FRACUNIT;
              break;
          }
          floor.speed = speed;
          height = sec.floorheight + stairsize;
          floor.floordestheight = height;
              
          texture = sec.floorpic;
          
          // Find next sector to raise
          // 1.   Find 2-sided line with same sector side[0]
          // 2.   Other side is the next sector to raise
          do
          {
              ok = false;
              for (i = 0;i < sec.linecount;i++)
              {
              if ( !flags((sec.lines[i]).flags , ML_TWOSIDED) )
                  continue;
                          
              tsec = (sec.lines[i]).frontsector;
              newsecnum = tsec.id;
              
              if (secnum != newsecnum)
                  continue;

              tsec = (sec.lines[i]).backsector;
              newsecnum = tsec.id;

              if (tsec.floorpic != texture)
                  continue;
                          
              height += stairsize;

              if (tsec.specialdata!=null)
                  continue;
                          
              sec = tsec;
              secnum = newsecnum;
              floor = new floormove_t();

              AddThinker (floor);

              sec.specialdata = floor;
              floor.function = think_t.T_MoveFloor;
              floor.direction = 1;
              floor.sector = sec;
              floor.speed = speed;
              floor.floordestheight = height;
              ok = true;
              break;
              }
          } while(ok);
          }
          return rtn;
      }
      
      
      /**
      //
      // Sliding door frame information
      //
      slidename_t    slideFrameNames[MAXSLIDEDOORS] =
      {
          {"GDOORF1","GDOORF2","GDOORF3","GDOORF4",  // front
           "GDOORB1","GDOORB2","GDOORB3","GDOORB4"}, // back
          
          {"\0","\0","\0","\0"}
      };
      */


      //
      // VERTICAL DOORS
      //

      //
      // T_VerticalDoor
      //
      public void VerticalDoor (vldoor_t door)
      {
          result_e   res;
         
          switch(door.direction)
          {
            case 0:
         // WAITING
         if (!eval(--door.topcountdown))
         {
             switch(door.type)
             {
               case blazeRaise:
             door.direction = -1; // time to go back down
             S.StartSound(door.sector.soundorg,sfxenum_t.sfx_bdcls);
             break;
             
               case normal:
             door.direction = -1; // time to go back down
             S.StartSound(door.sector.soundorg, sfxenum_t.sfx_dorcls);
             break;
             
               case close30ThenOpen:
             door.direction = 1;
             S.StartSound(door.sector.soundorg, sfxenum_t.sfx_doropn);
             break;
             
               default:
             break;
             }
         }
         break;
         
            case 2:
         //  INITIAL WAIT
         if (!eval(--door.topcountdown))
         {
             switch(door.type)
             {
               case raiseIn5Mins:
             door.direction = 1;
             door.type = vldoor_e.normal;
             S.StartSound(door.sector.soundorg, sfxenum_t.sfx_doropn);
             break;
             
               default:
             break;
             }
         }
         break;
         
            case -1:
         // DOWN
         res = MovePlane(door.sector,
                   door.speed,
                   door.sector.floorheight,
                   false,1,door.direction);
         if (res == result_e.pastdest)
         {
             switch(door.type)
             {
               case blazeRaise:
               case blazeClose:
             door.sector.specialdata = null;
             RemoveThinker (door);  // unlink and free
             S.StartSound(door.sector.soundorg, sfxenum_t.sfx_bdcls);
             break;
             
               case normal:
               case close:
             door.sector.specialdata = null;
             RemoveThinker (door);  // unlink and free
             break;
             
               case close30ThenOpen:
             door.direction = 0;
             door.topcountdown = 35*30;
             break;
             
               default:
             break;
             }
         }
         else if (res == result_e.crushed)
         {
             switch(door.type)
             {
               case blazeClose:
               case close:       // DO NOT GO BACK UP!
             break;
             
               default:
             door.direction = 1;
               S.StartSound(door.sector.soundorg, sfxenum_t.sfx_doropn);
             break;
             }
         }
         break;
         
            case 1:
         // UP
         res = MovePlane(door.sector,
                   door.speed,
                   door.topheight,
                   false,1,door.direction);
         
         if (res == result_e.pastdest)
         {
             switch(door.type)
             {
               case blazeRaise:
               case normal:
             door.direction = 0; // wait at top
             door.topcountdown = door.topwait;
             break;
             
               case close30ThenOpen:
               case blazeOpen:
               case open:
             door.sector.specialdata = null;
             RemoveThinker (door);  // unlink and free
             break;
             
               default:
             break;
             }
         }
         break;
          }
      }


      //
      // EV_DoLockedDoor
      // Move a locked door up/down
      //

      boolean
      DoLockedDoor
      ( line_t   line,
        vldoor_e type,
        mobj_t   thing )
      {
          player_t   p;
         
          p = thing.player;
         
          if (p==null)
         return false;
             
          switch(line.special)
          {
            case 99: // Blue Lock
            case 133:
/*         if ( p==null )
             return false; */
         if (!p.cards[card_t.it_bluecard.ordinal()] && !p.cards[card_t.it_blueskull.ordinal()])
         {
             p.message = PD_BLUEO;
             S.StartSound(null,sfxenum_t.sfx_oof);
             return false;
         }
         break;
         
            case 134: // Red Lock
            case 135:
 /*        if ( p==null )
             return false; */
         if (!p.cards[card_t.it_redcard.ordinal()] && !p.cards[card_t.it_redskull.ordinal()])
         {
             p.message = PD_REDO;
             S.StartSound(null,sfxenum_t.sfx_oof);
             return false;
         }
         break;
         
            case 136:    // Yellow Lock
            case 137:
 /*        if ( p==null )
             return false; */
         if (!p.cards[card_t.it_yellowcard.ordinal()] &&
             !p.cards[card_t.it_yellowskull.ordinal()])
         {
             p.message = PD_YELLOWO;
             S.StartSound(null,sfxenum_t.sfx_oof);
             return false;
         }
         break;  
          }

          return DoDoor(line,type);
      }


      public boolean
      DoDoor
      ( line_t   line,
        vldoor_e type )
      {
          int        secnum;
          boolean rtn=false;
          sector_t   sec;
          vldoor_t   door;
         
          secnum = -1;
          
          while ((secnum = FindSectorFromLineTag(line,secnum)) >= 0)
          {
         sec = LL.sectors[secnum];
         if (sec.specialdata!=null)
             continue;
             
         
         // new door thinker
         rtn = true;
         door = new vldoor_t();
         AddThinker (door);
         sec.specialdata = door;

         door.function = think_t.T_VerticalDoor;
         door.sector = sec;
         door.type = type;
         door.topwait = VDOORWAIT;
         door.speed = VDOORSPEED;
             
         switch(type)
         {
           case blazeClose:
             door.topheight = sec.FindLowestCeilingSurrounding();
             door.topheight -= 4*FRACUNIT;
             door.direction = -1;
             door.speed = VDOORSPEED * 4;
             S.StartSound(door.sector.soundorg, sfxenum_t.sfx_bdcls);
             break;
             
           case close:
             door.topheight = sec.FindLowestCeilingSurrounding();
             door.topheight -= 4*FRACUNIT;
             door.direction = -1;
             S.StartSound(door.sector.soundorg, sfxenum_t.sfx_dorcls);
             break;
             
           case close30ThenOpen:
             door.topheight = sec.ceilingheight;
             door.direction = -1;
             S.StartSound(door.sector.soundorg, sfxenum_t.sfx_dorcls);
             break;
             
           case blazeRaise:
           case blazeOpen:
             door.direction = 1;
             door.topheight = sec.FindLowestCeilingSurrounding();
             door.topheight -= 4*FRACUNIT;
             door.speed = VDOORSPEED * 4;
             if (door.topheight != sec.ceilingheight)
            	 S.StartSound(door.sector.soundorg, sfxenum_t.sfx_bdopn);
             break;
             
           case normal:
           case open:
             door.direction = 1;
             door.topheight = sec.FindLowestCeilingSurrounding();
             door.topheight -= 4*FRACUNIT;
             if (door.topheight != sec.ceilingheight)
            	 S.StartSound(door.sector.soundorg, sfxenum_t.sfx_doropn);
             break;
             
           default:
             break;
         }
             
          }
          return rtn;
      }


      //
      // EV_VerticalDoor : open a door manually, no tag value
      //
      void
      VerticalDoor
      ( line_t   line,
        mobj_t   thing )
      {
          player_t   player;
          //int      secnum;
          sector_t   sec;
          vldoor_t   door;
          int        side;
         
          side = 0;  // only front sides can be used

          // Check for locks
          player = thing.player;
             
          switch(line.special)
          {
            case 26: // Blue Lock
            case 32:
         if ( player ==null)
             return;
         
         if (!player.cards[card_t.it_bluecard.ordinal()] && !player.cards[card_t.it_blueskull.ordinal()])
         {
             player.message = PD_BLUEK;
             S.StartSound(null,sfxenum_t.sfx_oof);
             return;
         }
         break;
         
            case 27: // Yellow Lock
            case 34:
         if ( player ==null)
             return;
         
         if (!player.cards[card_t.it_yellowcard.ordinal()] &&
             !player.cards[card_t.it_yellowskull.ordinal()])
         {
             player.message = PD_YELLOWK;
             S.StartSound(null,sfxenum_t.sfx_oof);
             return;
         }
         break;
         
            case 28: // Red Lock
            case 33:
         if ( player ==null)
             return;
         
         if (!player.cards[card_t.it_redcard.ordinal()] && !player.cards[card_t.it_redskull.ordinal()])
         {
             player.message = PD_REDK;
             S.StartSound(null,sfxenum_t.sfx_oof);
             return;
         }
         break;
          }
         
          // if the sector has an active thinker, use it
          sec = LL.sides[ line.sidenum[side^1]].sector;
         // secnum = sec.id;

          if (sec.specialdata!=null)
          {
         door = (vldoor_t) sec.specialdata;
         switch(line.special)
         {
           case  1: // ONLY FOR "RAISE" DOORS, NOT "OPEN"s
           case  26:
           case  27:
           case  28:
           case  117:
             if (door.direction == -1)
             door.direction = 1; // go back up
             else
             {
             if (thing.player==null)
                 return;     // JDC: bad guys never close doors
             
             door.direction = -1;    // start going down immediately
             }
             return;
         }
          }
         
          // for proper sound
          switch(line.special)
          {
            case 117:    // BLAZING DOOR RAISE
            case 118:    // BLAZING DOOR OPEN
            	S.StartSound(sec.soundorg,sfxenum_t.sfx_bdopn);
         break;
         
            case 1:  // NORMAL DOOR SOUND
            case 31:
            	S.StartSound(sec.soundorg,sfxenum_t.sfx_doropn);
         break;
         
            default: // LOCKED DOOR SOUND
            	S.StartSound(sec.soundorg,sfxenum_t.sfx_doropn);
         break;
          }
         
          
          // new door thinker
          door = new vldoor_t();
          AddThinker (door);
          sec.specialdata = door;
          door.function = think_t.T_VerticalDoor;
          door.sector = sec;
          door.direction = 1;
          door.speed = VDOORSPEED;
          door.topwait = VDOORWAIT;

          switch(line.special)
          {
            case 1:
            case 26:
            case 27:
            case 28:
         door.type = vldoor_e.normal;
         break;
         
            case 31:
            case 32:
            case 33:
            case 34:
         door.type = vldoor_e.open;
         line.special = 0;
         break;
         
            case 117:    // blazing door raise
         door.type = vldoor_e.blazeRaise;
         door.speed = VDOORSPEED*4;
         break;
            case 118:    // blazing door open
         door.type = vldoor_e.blazeOpen;
         line.special = 0;
         door.speed = VDOORSPEED*4;
         break;
          }
          
          // find the top and bottom of the movement range
          door.topheight = sec.FindLowestCeilingSurrounding();
          door.topheight -= 4*FRACUNIT;
      }


    

      



      // UNUSED
      // Separate into p_slidoor.c?

      /*
      // ABANDONED TO THE MISTS OF TIME!!!
      //
      // EV_SlidingDoor : slide a door horizontally
      // (animate midtexture, then set noblocking line)
      //


      slideframe_t slideFrames[MAXSLIDEDOORS];

      void P_InitSlidingDoorFrames(void)
      {
          int        i;
          int        f1;
          int        f2;
          int        f3;
          int        f4;
         
          // DOOM II ONLY...
          if ( gamemode != commercial)
         return;
         
          for (i = 0;i < MAXSLIDEDOORS; i++)
          {
         if (!slideFrameNames[i].frontFrame1[0])
             break;
                 
         f1 = R_TextureNumForName(slideFrameNames[i].frontFrame1);
         f2 = R_TextureNumForName(slideFrameNames[i].frontFrame2);
         f3 = R_TextureNumForName(slideFrameNames[i].frontFrame3);
         f4 = R_TextureNumForName(slideFrameNames[i].frontFrame4);

         slideFrames[i].frontFrames[0] = f1;
         slideFrames[i].frontFrames[1] = f2;
         slideFrames[i].frontFrames[2] = f3;
         slideFrames[i].frontFrames[3] = f4;
             
         f1 = R_TextureNumForName(slideFrameNames[i].backFrame1);
         f2 = R_TextureNumForName(slideFrameNames[i].backFrame2);
         f3 = R_TextureNumForName(slideFrameNames[i].backFrame3);
         f4 = R_TextureNumForName(slideFrameNames[i].backFrame4);

         slideFrames[i].backFrames[0] = f1;
         slideFrames[i].backFrames[1] = f2;
         slideFrames[i].backFrames[2] = f3;
         slideFrames[i].backFrames[3] = f4;
          }
      }


      //
      // Return index into "slideFrames" array
      // for which door type to use
      //
      int P_FindSlidingDoorType(line_t*  line)
      {
          int        i;
          int        val;
         
          for (i = 0;i < MAXSLIDEDOORS;i++)
          {
         val = sides[line.sidenum[0]].midtexture;
         if (val == slideFrames[i].frontFrames[0])
             return i;
          }
         
          return -1;
      }

      void T_SlidingDoor (slidedoor_t*   door)
      {
          switch(door.status)
          {
            case sd_opening:
         if (!door.timer--)
         {
             if (++door.frame == SNUMFRAMES)
             {
             // IF DOOR IS DONE OPENING...
             sides[door.line.sidenum[0]].midtexture = 0;
             sides[door.line.sidenum[1]].midtexture = 0;
             door.line.flags &= ML_BLOCKING^0xff;
                         
             if (door.type == sdt_openOnly)
             {
                 door.frontsector.specialdata = NULL;
                 P_RemoveThinker (&door.thinker);
                 break;
             }
                         
             door.timer = SDOORWAIT;
             door.status = sd_waiting;
             }
             else
             {
             // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
             door.timer = SWAITTICS;
                         
             sides[door.line.sidenum[0]].midtexture =
                 slideFrames[door.whichDoorIndex].
                 frontFrames[door.frame];
             sides[door.line.sidenum[1]].midtexture =
                 slideFrames[door.whichDoorIndex].
                 backFrames[door.frame];
             }
         }
         break;
                 
            case sd_waiting:
         // IF DOOR IS DONE WAITING...
         if (!door.timer--)
         {
             // CAN DOOR CLOSE?
             if (door.frontsector.thinglist != NULL ||
             door.backsector.thinglist != NULL)
             {
             door.timer = SDOORWAIT;
             break;
             }

             //door.frame = SNUMFRAMES-1;
             door.status = sd_closing;
             door.timer = SWAITTICS;
         }
         break;
                 
            case sd_closing:
         if (!door.timer--)
         {
             if (--door.frame < 0)
             {
             // IF DOOR IS DONE CLOSING...
             door.line.flags |= ML_BLOCKING;
             door.frontsector.specialdata = NULL;
             P_RemoveThinker (&door.thinker);
             break;
             }
             else
             {
             // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
             door.timer = SWAITTICS;
                         
             sides[door.line.sidenum[0]].midtexture =
                 slideFrames[door.whichDoorIndex].
                 frontFrames[door.frame];
             sides[door.line.sidenum[1]].midtexture =
                 slideFrames[door.whichDoorIndex].
                 backFrames[door.frame];
             }
         }
         break;
          }
      }



      void
      EV_SlidingDoor
      ( line_t*  line,
        mobj_t*  thing )
      {
          sector_t       sec;
          slidedoor_t*   door;
         
          // DOOM II ONLY...
          if (DM.gamemode != commercial)
         return;
          
          // Make sure door isn't already being animated
          sec = line.frontsector;
          door = NULL;
          if (sec.specialdata)
          {
         if (!thing.player)
             return;
                 
         door = sec.specialdata;
         if (door.type == sdt_openAndClose)
         {
             if (door.status == sd_waiting)
             door.status = sd_closing;
         }
         else
             return;
          }
          
          // Init sliding door vars
          if (!door)
          {
         door = Z_Malloc (sizeof(*door), PU_LEVSPEC, 0);
         P_AddThinker (&door.thinker);
         sec.specialdata = door;
             
         door.type = sdt_openAndClose;
         door.status = sd_opening;
         door.whichDoorIndex = P_FindSlidingDoorType(line);

         if (door.whichDoorIndex < 0)
             system.Error("EV_SlidingDoor: Can't use texture for sliding door!");
                 
         door.frontsector = sec;
         door.backsector = line.backsector;
         door.thinker.function = T_SlidingDoor;
         door.timer = SWAITTICS;
         door.frame = 0;
         door.line = line;
          }
      }
      */


  /** This acts as the "giant switch statement" for action functions.
   *  I guess you <i>could</i> implement this with callbacks too, but it won't result
   *  in any less effort, and to add new functions, you'll have to go through more
   *  or less the same effort, as action functions need to be aware of what's going
   *  on around them, so they cannot be defined elsewhere unless you also pass context
   *  around....
   *  
   * @param action
   * @param a
   * @param b
   */
  
  public void dispatch(think_t action, Object a, Object b){
      switch (action){
          case P_MobjThinker:
        	  P_MobjThinker((mobj_t)a);
        	  break;
          case A_Light0:
        	  A_Light0((player_t)a,(pspdef_t) b);
        	  break;
          case A_WeaponReady:
        	  A_WeaponReady((player_t)a,(pspdef_t) b);
        	  break;        	  
          case  A_Lower:
        	  A_Lower((player_t)a,(pspdef_t) b);
        	  break;
          case  A_Raise:
        	  A_Raise((player_t)a,(pspdef_t) b);
        	  break;
          case  A_Punch:
        	  A_Punch((player_t)a,(pspdef_t) b);
        	  break;
          case  A_ReFire:
        	  A_ReFire((player_t)a,(pspdef_t) b);
        	  break;
          case  A_FirePistol:
        	  A_FirePistol((player_t)a,(pspdef_t) b);
        	  break;
          case  A_Light1:
        	  A_Light1((player_t)a,(pspdef_t) b);
        	  break;
          case  A_FireShotgun:
        	  A_FireShotgun((player_t)a,(pspdef_t) b);
        	  break;
          case  A_Light2:
        	  A_Light2((player_t)a,(pspdef_t) b);
        	  break;
          case  A_FireShotgun2:
        	  A_FireShotgun2((player_t)a,(pspdef_t)b);
        	  break;
          case  A_CheckReload:
        	  A_CheckReload((player_t)a,(pspdef_t)b);
        	  break;
          case  A_OpenShotgun2:
        	  A_OpenShotgun2((player_t)a,(pspdef_t)b);
        	  break;
          case  A_LoadShotgun2:
        	  A_LoadShotgun2((player_t)a,(pspdef_t)b);
        	  break;
          case  A_CloseShotgun2:
        	  A_CloseShotgun2((player_t)a,(pspdef_t)b);
        	  break;
          case  A_FireCGun:
        	  A_FireCGun((player_t)a,(pspdef_t)b);
        	  break;
          case  A_GunFlash:
        	  A_GunFlash((player_t)a,(pspdef_t)b);
        	  break;
          case  A_FireMissile:
        	  A_FireMissile((player_t)a,(pspdef_t)b);
        	  break;
          case  A_Saw:
        	  A_Saw((player_t)a,(pspdef_t)b);
        	  break;
          case  A_FirePlasma:
        	  A_FirePlasma((player_t)a,(pspdef_t)b);
        	  break;
          case  A_BFGsound:
        	  A_BFGsound((player_t)a,(pspdef_t)b);
        	  break;
          case  A_FireBFG:
        	  A_FireBFG((player_t)a,(pspdef_t)b);
        	  break;
          case  A_BFGSpray:
        	  A_BFGSpray((mobj_t)a);
        	  break;
          case  A_Explode:
        	  A_Explode((mobj_t)a);
        	  break;
          case  A_Pain:
        	  A_Pain((mobj_t)a);
        	  break;
          case  A_PlayerScream:
        	  A_PlayerScream((mobj_t)a);
        	  break;
          case  A_Fall:
        	  A_Fall((mobj_t)a);
        	  break;
          case  A_XScream:
        	  A_XScream((mobj_t)a);
        	  break;
          case  A_Look:
        	  A_Look((mobj_t)a);
        	  break;
          case  A_Chase:
        	  A_Chase((mobj_t)a);
        	  break;
          case  A_FaceTarget:
        	  A_FaceTarget((mobj_t)a);
        	  break;
          case  A_PosAttack:
        	  A_PosAttack((mobj_t)a);
        	  break;
          case  A_Scream:
        	  A_Scream((mobj_t)a);
        	  break;
          case  A_SPosAttack:
        	  A_SPosAttack((mobj_t)a);
        	  break;
          case  A_VileChase:
        	  A_VileChase((mobj_t)a);
        	  break;
          case  A_VileStart:
        	  A_VileStart((mobj_t)a);
        	  break;
          case  A_VileTarget:
        	  A_VileTarget((mobj_t)a);
        	  break;
          case  A_VileAttack:
        	  A_VileAttack((mobj_t)a);
        	  break;
          case  A_StartFire:
        	  A_StartFire((mobj_t)a);
        	  break;
          case  A_Fire:
        	  A_Fire((mobj_t)a);
        	  break;
          case  A_FireCrackle:
        	  A_FireCrackle((mobj_t)a);
        	  break;
          case  A_Tracer:
        	  A_Tracer((mobj_t)a);
        	  break;
          case  A_SkelWhoosh:
        	  A_SkelWhoosh((mobj_t)a);
        	  break;
          case  A_SkelFist:
        	  A_SkelFist((mobj_t)a);
        	  break;
          case  A_SkelMissile:
        	  A_SkelMissile((mobj_t)a);
        	  break;
          case  A_FatRaise:
        	  A_FatRaise((mobj_t)a);
        	  break;
          case  A_FatAttack1:
        	  A_FatAttack1((mobj_t)a);
        	  break;
          case  A_FatAttack2:
        	  A_FatAttack1((mobj_t)a);
        	  break;
          case  A_FatAttack3:
        	  A_FatAttack1((mobj_t)a);
        	  break;
          case  A_BossDeath:
        	  A_BossDeath((mobj_t)a);
        	  break;
          case  A_CPosAttack:
        	  A_CPosAttack((mobj_t)a);
        	  break;
          case  A_CPosRefire:
        	  A_CPosRefire((mobj_t)a);
        	  break;
          case  A_TroopAttack:
        	  A_TroopAttack((mobj_t)a);
        	  break;
          case  A_SargAttack:
        	  A_SargAttack((mobj_t)a);
        	  break;
          case  A_HeadAttack:
        	  A_HeadAttack((mobj_t)a);
        	  break;
          case  A_BruisAttack:
        	  A_BruisAttack((mobj_t)a);
        	  break;
          case  A_SkullAttack:
        	  A_SkullAttack((mobj_t)a);
        	  break;
          case  A_Metal:
        	  A_Metal((mobj_t)a);
        	  break;
          case  A_SpidRefire:
        	  A_SpidRefire((mobj_t)a);
        	  break;
          case  A_BabyMetal:
        	  A_BabyMetal((mobj_t)a);
        	  break;
          case  A_BspiAttack:
        	  A_BspiAttack((mobj_t)a);
        	  break;
          case  A_Hoof:
        	  A_Hoof((mobj_t)a);
        	  break;
          case  A_CyberAttack:
        	  A_CyberAttack((mobj_t)a);
        	  break;
          case  A_PainAttack:
        	  A_PainAttack((mobj_t) a);
        	  break;
          case  A_PainDie:
        	  A_PainDie((mobj_t) a);
        	  break;
          case  A_KeenDie:
              A_KeenDie((mobj_t)a);
              break;
          case  A_BrainPain:
        	  break;
          case  A_BrainScream:
        	  break;
          case  A_BrainDie:
        	  break;
          case  A_BrainAwake:
        	  break;        	  
          case  A_BrainSpit:
        	  break;
          case  A_SpawnSound:
        	  break;
          case  A_SpawnFly:
        	  break;
          case  A_BrainExplode:
        	  break;
          case  T_FireFlicker:
              ((fireflicker_t) a).FireFlicker();
        	  break;
          case 	T_LightFlash:
              ((lightflash_t) a).LightFlash();
        	  break;
          case	T_StrobeFlash:
        	  ((strobe_t) a).StrobeFlash();
        	  break;
          case	T_Glow:
        	  ((glow_t) a).Glow();
        	  break;
          case	T_MoveCeiling:
        	  this.MoveCeiling((ceiling_t) a);
        	  break;
          case	T_MoveFloor:
        	  this.MoveFloor((floormove_t) a);
        	  break;
          case	T_VerticalDoor:
        	  this.VerticalDoor((line_t)a,(mobj_t) b);
        	  break;
          case	T_PlatRaise:
        	  this.PlatRaise((plat_t)a);
          	break;
      }
      
  }
  
  /**
   * A_WeaponReady
   * The player can fire the weapon
   * or change to another weapon at this time.
   * Follows after getting weapon up,
   * or after previous attack/fire sequence.
   */

  void
  A_WeaponReady
  ( player_t player,
    pspdef_t psp )
  {   
      statenum_t  newstate;
      int     angle;
      
      // get out of attack state
      if (player.mo.state == states[statenum_t.S_PLAY_ATK1.ordinal()]
      || player.mo.state == states[statenum_t.S_PLAY_ATK2.ordinal()] )
      {
      player.mo.SetMobjState (statenum_t.S_PLAY);
      }
      
      if (player.readyweapon == weapontype_t.wp_chainsaw
      && psp.state == states[statenum_t.S_SAW.ordinal()])
      {
    	  S.StartSound(player.mo, sfxenum_t.sfx_sawidl);
      }
      
      // check for change
      //  if player is dead, put the weapon away
      if (player.pendingweapon != weapontype_t.wp_nochange || !eval(player.health[0]))
      {
      // change weapon
      //  (pending weapon should allready be validated)
      newstate = weaponinfo[player.readyweapon.ordinal()].downstate;
      player.SetPsprite ( player_t.ps_weapon, newstate);
      return; 
      }
      
      // check for fire
      //  the missile launcher and bfg do not auto fire
      if (flags(player.cmd.buttons , BT_ATTACK))
      {
      if ( !player.attackdown
           || (player.readyweapon != weapontype_t.wp_missile
           && player.readyweapon != weapontype_t.wp_bfg) )
      {
          player.attackdown = true;
          EN.FireWeapon (player);      
          return;
      }
      }
      else
      player.attackdown = false;
      
      // bob the weapon based on movement speed
      angle = (128*DM.leveltime)&FINEMASK;
      psp.sx = FRACUNIT + FixedMul (player.bob, finecosine[angle]);
      angle &= FINEANGLES/2-1;
      psp.sy = player_t.WEAPONTOP + FixedMul (player.bob, finesine[angle]);
  }



  //
  // A_ReFire
  // The player can re-fire the weapon
  // without lowering it entirely.
  //
  void A_ReFire
  ( player_t player,
    pspdef_t psp )
  {
      
      // check for fire
      //  (if a weaponchange is pending, let it go through instead)
      if ( flags(player.cmd.buttons , BT_ATTACK) 
       && player.pendingweapon == weapontype_t.wp_nochange
       && eval(player.health[0]))
      {
      player.refire++;
      EN.FireWeapon (player);
      }
      else
      {
      player.refire = 0;
      player.CheckAmmo ();
      }
  }


  void
  A_CheckReload
  ( player_t player,
    pspdef_t psp )
  {
      player.CheckAmmo ();
  /*
      if (player.ammo[am_shell]<2)
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
  ( player_t player,
    pspdef_t psp )
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
      if (!eval(player.health[0]))
      {
      // Player is dead, so keep the weapon off screen.
      player.SetPsprite (ps_weapon, statenum_t.S_NULL);
      return; 
      }
      
      player.readyweapon = player.pendingweapon; 

      player.BringUpWeapon ();
  }


  //
  // A_Raise
  //
  void
  A_Raise
  ( player_t player,
    pspdef_t psp )
  {
      statenum_t  newstate;
      
      psp.sy -= RAISESPEED;

      if (psp.sy > WEAPONTOP )
      return;
      
      psp.sy = WEAPONTOP;
      
      // The weapon has been raised all the way,
      //  so change to the ready state.
      newstate = weaponinfo[player.readyweapon.ordinal()].readystate;

      player.SetPsprite (ps_weapon, newstate);
  }



  //
  // A_GunFlash
  //
  void
  A_GunFlash
  ( player_t player,
    pspdef_t psp ) 
  {
      SetMobjState (player.mo, statenum_t.S_PLAY_ATK2);
      player.SetPsprite (ps_flash,weaponinfo[player.readyweapon.ordinal()].flashstate);
  }



  //
  // WEAPON ATTACKS
  //


  //
  // A_Punch
  //
  void
  A_Punch
  ( player_t player,
    pspdef_t psp ) 
  {
      long angle; //angle_t
      int     damage;
      int     slope;
      
      damage = (RND.P_Random ()%10+1)<<1;

      if (eval(player.powers[pw_strength]))    
      damage *= 10;

      angle = player.mo.angle;
      angle = (angle+(RND.P_Random()-RND.P_Random())<<18)&BITS32;
      slope = AimLineAttack (player.mo, angle, MELEERANGE);
      LineAttack (player.mo, angle, MELEERANGE, slope, damage);

      // turn to face target
      if (eval(linetarget))
      {
    	  S.StartSound(player.mo, sfxenum_t.sfx_punch);
      player.mo.angle = R.PointToAngle2 (player.mo.x,
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
  ( player_t player,
    pspdef_t psp ) 
  {
      long angle; // angle_t
      int     damage;
      int     slope;

      damage = 2*(RND.P_Random ()%10+1);
      angle = player.mo.angle;
      angle += (RND.P_Random()-RND.P_Random())<<18;
      angle&=BITS32;
      
      // use meleerange + 1 se the puff doesn't skip the flash
      slope = AimLineAttack (player.mo, angle, MELEERANGE+1);
      LineAttack (player.mo, angle, MELEERANGE+1, slope, damage);

      if (!eval(linetarget))
      {
    	  S.StartSound(player.mo, sfxenum_t.sfx_sawful);
      return;
      }
      S.StartSound(player.mo, sfxenum_t.sfx_sawhit);
      
      // turn to face target
      angle = R.PointToAngle2 (player.mo.x, player.mo.y,
                   linetarget.x, linetarget.y);
      /* FIXME: this comparison is going to fail.... or not?
       If e.g. angle = 359 degrees (which will be mapped to a small negative number),       
       and player.mo.angle = 160 degrees (a large, positive value), the result will be a 
       large negative value, which will still be "greater" than ANG180.
       
       It seems that *differences* between angles will always compare correctly, but
       not direct inequalities.
       
      */
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
  ( player_t player,
    pspdef_t psp ) 
  {
      player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;
      SpawnPlayerMissile (player.mo, mobjtype_t.MT_ROCKET);
  }


  //
  // A_FireBFG
  //
  void
  A_FireBFG
  ( player_t player,
    pspdef_t psp ) 
  {
      player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()] -= BFGCELLS;
      SpawnPlayerMissile (player.mo, mobjtype_t.MT_BFG);
  }



  //
  // A_FirePlasma
  //
  void
  A_FirePlasma
  ( player_t player,
    pspdef_t psp ) 
  {
      player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

      player.SetPsprite (
            ps_flash,
            weaponinfo[player.readyweapon.ordinal()].flashstate );

      SpawnPlayerMissile (player.mo, mobjtype_t.MT_PLASMA);
  }



  //
  // P_BulletSlope
  // Sets a slope so a near miss is at aproximately
  // the height of the intended target
  //
  int     bulletslope;


  void P_BulletSlope (mobj_t mo)
  {
      long an;
      
      // see which target is to be aimed at
      an = mo.angle;
      bulletslope = AimLineAttack (mo, an, 16*64*FRACUNIT);

      if (!eval(linetarget))
      {
      an += 1<<26;
      bulletslope = AimLineAttack (mo, an, 16*64*FRACUNIT);
      if (!eval(linetarget))
      {
          an -= 2<<26;
          bulletslope = AimLineAttack (mo, an, 16*64*FRACUNIT);
      }
      }
  }


  //
  // P_GunShot
  //
  void
  P_GunShot
  ( mobj_t   mo,
    boolean   accurate )
  {
      long angle;
      int     damage;
      
      damage = 5*(RND.P_Random ()%3+1);
      angle = mo.angle;

      if (!accurate)
      angle += (RND.P_Random()-RND.P_Random())<<18;

      LineAttack (mo, angle, MISSILERANGE, bulletslope, damage);
  }


  //
  // A_FirePistol
  //
  void
  A_FirePistol
  ( player_t player,
    pspdef_t psp ) 
  {
	  S.StartSound(player.mo, sfxenum_t.sfx_pistol);

      player.mo.SetMobjState ( statenum_t.S_PLAY_ATK2);
      player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

      player.SetPsprite (
            ps_flash,
            weaponinfo[player.readyweapon.ordinal()].flashstate);

      P_BulletSlope (player.mo);
      P_GunShot (player.mo, !eval(player.refire));
  }


  //
  // A_FireShotgun
  //
  void
  A_FireShotgun
  ( player_t player,
    pspdef_t psp ) 
  {
      int     i;
      
      S.StartSound(player.mo, sfxenum_t.sfx_shotgn);
      player.mo.SetMobjState ( statenum_t.S_PLAY_ATK2);

      player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

      player.SetPsprite (
            ps_flash,
            weaponinfo[player.readyweapon.ordinal()].flashstate);

      P_BulletSlope (player.mo);
      
      for (i=0 ; i<7 ; i++)
      P_GunShot (player.mo, false);
  }



  /**
   * A_FireShotgun2
   */
  
  void
  A_FireShotgun2
  ( player_t player,
    pspdef_t psp ) 
  {
      int     i;
      long angle;
      int     damage;
          
      
      S.StartSound (player.mo, sfxenum_t.sfx_dshtgn);
      player.mo.SetMobjState (statenum_t.S_PLAY_ATK2);

      player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]-=2;

      player.SetPsprite (
            ps_flash,
            weaponinfo[player.readyweapon.ordinal()].flashstate);

      P_BulletSlope (player.mo);
      
      for (i=0 ; i<20 ; i++)
      {
      damage = 5*(RND.P_Random ()%3+1);
      angle = player.mo.angle;
      angle += (RND.P_Random()-RND.P_Random())<<19;
      LineAttack (player.mo,
                angle,
                MISSILERANGE,
                bulletslope + ((RND.P_Random()-RND.P_Random())<<5), damage);
      }
  }


  //
  // A_FireCGun
  //
  void
  A_FireCGun
  ( player_t player,
    pspdef_t psp ) 
  {
      S.StartSound (player.mo, sfxenum_t.sfx_pistol);

      if (!eval(player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]))
      return;
          
      player.mo.SetMobjState (statenum_t.S_PLAY_ATK2);
      player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

      player.SetPsprite (           ps_flash,
            weaponinfo[player.readyweapon.ordinal()+(psp.state.id - states[statenum_t.S_CHAIN1.ordinal()].id)].flashstate
            );

      P_BulletSlope (player.mo);
      
      P_GunShot (player.mo, !eval(player.refire));
  }



  //
  // ?
  //
  void A_Light0 (player_t player, pspdef_t psp)
  {
      player.extralight = 0;
  }

  void A_Light1 (player_t player, pspdef_t psp)
  {
      player.extralight = 1;
  }

  void A_Light2 (player_t player, pspdef_t psp)
  {
      player.extralight = 2;
  }


  //
  // A_BFGSpray
  // Spawn a BFG explosion on every monster in view
  //
  void A_BFGSpray (mobj_t mo) 
  {
      int         i;
      int         j;
      int         damage;
      long     an; // angle_t
      
      // offset angles from its attack angle
      for (i=0 ; i<40 ; i++)
      {
      an = (mo.angle - ANG90/2 + ANG90/40*i)&BITS32;

      // mo.target is the originator (player)
      //  of the missile
      AimLineAttack (mo.target, an, 16*64*FRACUNIT);

      if (!eval(linetarget))
          continue;

      SpawnMobj (linetarget.x,
          linetarget.y,
          linetarget.z + (linetarget.height>>2),
               mobjtype_t.MT_EXTRABFG);
      
      damage = 0;
      for (j=0;j<15;j++)
          damage += (RND.P_Random()&7) + 1;

      DamageMobj (linetarget, mo.target,mo.target, damage);
      }
  }


  //
  // A_BFGsound
  //
  public void
  A_BFGsound
  ( player_t player,
    pspdef_t psp )
  {
	  S.StartSound(player.mo, sfxenum_t.sfx_bfg);
  }

  //////////////////////////// MONSTER AND ENEMY FUNCTIONS /////////////
  

  //
  // ACTION ROUTINES
  //

 
  public void A_KeenDie (mobj_t mo)
    {
        thinker_t  th;
        mobj_t mo2;
        line_t  junk = null;

        A_Fall (mo);
        
        // scan the remaining thinkers
        // to see if all Keens are dead
        for (th = thinkercap.next ; th != thinkercap ; th=th.next)
        {
        if (th.function != think_t.P_MobjThinker)
            continue;

        mo2 = (mobj_t)th;
        if (mo2 != mo
            && mo2.type == mo.type
            && mo2.health > 0)
        {
            // other Keen not dead
            return;     
        }
        }

        junk.tag = 666;
        DoDoor(junk,vldoor_e.open);
    }



    //
    // A_Look
    // Stay in state until a player is sighted.
    //
    void A_Look (mobj_t actor)
    {
        mobj_t targ;
        boolean seeyou=true; // to avoid the fugly goto
        
        actor.threshold = 0;   // any shot will wake up
        targ = actor.subsector.sector.soundtarget;

        if (targ!=null
        && flags(targ.flags, MF_SHOOTABLE) )
        {
        actor.target = targ;

        if ( flags(actor.flags,MF_AMBUSH ))
        {
            seeyou= (EN.CheckSight (actor, actor.target));              
        }
        
        if (!seeyou){
        if (!EN.LookForPlayers (actor, false) )
        return;
        }
        }
        // go into chase state
      seeyou:
        if (actor.info.seesound!=null)
        {
        int     sound;
            
        switch (actor.info.seesound)
        {
          case sfx_posit1:
          case sfx_posit2:
          case sfx_posit3:
            sound = sfxenum_t.sfx_posit1.ordinal()+RND.P_Random()%3;
            break;

          case sfx_bgsit1:
          case sfx_bgsit2:
            sound = sfxenum_t.sfx_bgsit1.ordinal()+RND.P_Random()%2;
            break;

          default:
            sound = actor.info.seesound.ordinal();
            break;
        }

        if (actor.type==mobjtype_t.MT_SPIDER
            || actor.type == mobjtype_t.MT_CYBORG)
        {
            // full volume
        	S.StartSound(null, sound);
        }
        else
        	S.StartSound(actor, sound);
        }

        actor.SetMobjState(actor.info.seestate);
    }


    //
    // A_Chase
    // Actor has a melee attack,
    // so it tries to close as fast as possible
    //
    void A_Chase (mobj_t   actor)
    {
        int     delta;
        boolean nomissile=false; // for the fugly goto

        if (actor.reactiontime!=0)
        actor.reactiontime--;
                    

        // modify target threshold
        if  (actor.threshold!=0)
        {
        if (actor.target==null
            || actor.target.health <= 0)
        {
            actor.threshold = 0;
        }
        else
            actor.threshold--;
        }
        
        // turn towards movement direction if not there yet
        if (actor.movedir < 8)
        {
        actor.angle &= (7<<29);
        delta = (int) (actor.angle - (actor.movedir << 29));
        
        if (delta > 0)
            actor.angle -= ANG90/2;
        else if (delta < 0)
            actor.angle += ANG90/2;
        }

        if (actor.target==null
        || !flags(actor.target.flags,MF_SHOOTABLE))
        {
        // look for a new target
        if (EN.LookForPlayers(actor,true))
            return;     // got a new target
        
        actor.SetMobjState (actor.info.spawnstate);
        return;
        }
        
        // do not attack twice in a row
        if (flags(actor.flags , MF_JUSTATTACKED))
        {
        actor.flags &= ~MF_JUSTATTACKED;
        if (DM.gameskill != skill_t.sk_nightmare && !DM.fastparm)
            NewChaseDir (actor);
        return;
        }
        
        // check for melee attack
        if (actor.info.meleestate!=null
        && EN.CheckMeleeRange (actor))
        {
        if (actor.info.attacksound!=null)
           S.StartSound (actor, actor.info.attacksound);

        actor.SetMobjState(actor.info.meleestate);
        return;
        }
        
        // check for missile attack
        if (actor.info.missilestate != null)
        {
        if (DM.gameskill.ordinal() < skill_t.sk_nightmare.ordinal()
            && !DM.fastparm && actor.movecount!=0)
        {
            nomissile=true;
        }
        
        nomissile=!EN.CheckMissileRange (actor);
        
        if (nomissile){
        actor.SetMobjState ( actor.info.missilestate);
        actor.flags |= MF_JUSTATTACKED;
        return;
        }
        }

        // ?
      nomissile:
        // possibly choose another target
        if (DM.netgame
        && actor.threshold==0
        && !EN.CheckSight (actor, actor.target) )
        {
        if (EN.LookForPlayers(actor,true))
            return; // got a new target
        }
        
        // chase towards player
        if (--actor.movecount<0
        || !Move (actor))
        {
        NewChaseDir (actor);
        }
        
        // make active sound
        if (actor.info.activesound!=null
        && RND.P_Random() < 3)
        {
            S.StartSound (actor, actor.info.activesound);
        }
    }
    
    boolean Move (mobj_t actor)
    {
        // fixed_t
        int tryx;
        int tryy;
        
        line_t ld;
        
        // warning: 'catch', 'throw', and 'try'
        // are all C++ reserved words
        boolean try_ok;
        boolean good;
            
        if (actor.movedir == DI_NODIR)
        return false;
            
        if (actor.movedir >= 8)
        I.Error ("Weird actor.movedir!");
            
        tryx = actor.x + actor.info.speed*xspeed[actor.movedir];
        tryy = actor.y + actor.info.speed*yspeed[actor.movedir];

        try_ok = TryMove (actor, tryx, tryy);

        if (!try_ok)
        {
        // open any specials
        if (flags(actor.flags , MF_FLOAT) && floatok)
        {
            // must adjust height
            if (actor.z < tmfloorz)
            actor.z += FLOATSPEED;
            else
            actor.z -= FLOATSPEED;

            actor.flags |= MF_INFLOAT;
            return true;
        }
            
        if (numspechit==0)
            return false;
                
        actor.movedir = DI_NODIR;
        good = false;
        while ((numspechit--)>0)
        {
            ld = spechit[numspechit];
            // if the special is not a door
            // that can be opened,
            // return false
            if (UseSpecialLine (actor, ld,false))
            good = true;
        }
        return good;
        }
        else
        {
        actor.flags &= ~MF_INFLOAT;
        }
        
        
        if (! flags(actor.flags , MF_FLOAT) )   
        actor.z = actor.floorz;
        return true; 
    }

    private void NewChaseDir (mobj_t actor)
    {
        // fixed_t
        int deltax,deltay;
        
        //dirtype
        int   d[]=new int[3];
        
        int     tdir;
        int   olddir;
        // dirtypes
        int   turnaround;

        if (actor.target==null)
        I.Error ("P_NewChaseDir: called with no target");
            
        olddir = actor.movedir;
        turnaround=opposite[olddir];

        deltax = actor.target.x - actor.x;
        deltay = actor.target.y - actor.y;

        if (deltax>10*FRACUNIT)
        d[1]= DI_EAST;
        else if (deltax<-10*FRACUNIT)
        d[1]= DI_WEST;
        else
        d[1]=DI_NODIR;

        if (deltay<-10*FRACUNIT)
        d[2]= DI_SOUTH;
        else if (deltay>10*FRACUNIT)
        d[2]= DI_NORTH;
        else
        d[2]=DI_NODIR;

        // try direct route
        if (d[1] != DI_NODIR
        && d[2] != DI_NODIR)
        {
        actor.movedir = diags[(eval(deltay<0)<<1)+eval(deltax>0)];
        if (actor.movedir != turnaround && TryWalk(actor))
            return;
        }

        // try other directions
        if (RND.P_Random() > 200
        ||  Math.abs(deltay)>Math.abs(deltax))
        {
        tdir=d[1];
        d[1]=d[2];
        d[2]=tdir;
        }

        if (d[1]==turnaround)
        d[1]=DI_NODIR;
        if (d[2]==turnaround)
        d[2]=DI_NODIR;
        
        if (d[1]!=DI_NODIR)
        {
        actor.movedir = d[1];
        if (TryWalk(actor))
        {
            // either moved forward or attacked
            return;
        }
        }

        if (d[2]!=DI_NODIR)
        {
        actor.movedir =d[2];

        if (TryWalk(actor))
            return;
        }

        // there is no direct path to the player,
        // so pick another direction.
        if (olddir!=DI_NODIR)
        {
        actor.movedir =olddir;

        if (TryWalk(actor))
            return;
        }

        // randomly determine direction of search
        if (flags(RND.P_Random(),1))   
        {
        for ( tdir=DI_EAST;
              tdir<=DI_SOUTHEAST;
              tdir++ )
        {
            if (tdir!=turnaround)
            {
            actor.movedir =tdir;
            
            if ( TryWalk(actor) )
                return;
            }
        }
        }
        else
        {
        for ( tdir=DI_SOUTHEAST;
              tdir != (DI_EAST-1);
              tdir-- )
        {
            if (tdir!=turnaround)
            {
            actor.movedir =tdir;
            
            if ( TryWalk(actor) )
                return;
            }
        }
        }

        if (turnaround !=  DI_NODIR)
        {
        actor.movedir =turnaround;
        if ( TryWalk(actor) )
            return;
        }

        actor.movedir = DI_NODIR;  // can not move
    }
    

    /**
     * TryWalk
     * Attempts to move actor on
     * in its current (ob.moveangle) direction.
     * If blocked by either a wall or an actor
     * returns FALSE
     * If move is either clear or blocked only by a door,
     * returns TRUE and sets...
     * If a door is in the way,
     * an OpenDoor call is made to start it opening.
     */
    private boolean TryWalk (mobj_t actor)
    {   
        if (!Move (actor))
        {
        return false;
        }

        actor.movecount = RND.P_Random()&15;
        return true;
    }

    //
    // A_FaceTarget
    //
    void A_FaceTarget (mobj_t  actor)
    {   
        if (actor.target==null)
        return;
        
        actor.flags &= ~MF_AMBUSH;
        
        actor.angle = R.PointToAngle2 (actor.x,
                        actor.y,
                        actor.target.x,
                        actor.target.y);
        
        if (flags(actor.target.flags , MF_SHADOW))
        actor.angle += (RND.P_Random()-RND.P_Random())<<21;
    }


    //
    // A_PosAttack
    //
    void A_PosAttack (mobj_t  actor)
    {
        int     angle;
        int     damage;
        int     slope;
        
        if (actor.target==null)
        return;
        A_FaceTarget (actor);
        angle = (int) actor.angle;
        slope = AimLineAttack (actor, angle, MISSILERANGE);

        S.StartSound(actor, sfxenum_t.sfx_pistol);
        angle += (RND.P_Random()-RND.P_Random())<<20;
        damage = ((RND.P_Random()%5)+1)*3;
        LineAttack (actor, angle, MISSILERANGE, slope, damage);
    }

    void A_SPosAttack (mobj_t  actor)
    {
        int     i;
        long     angle;
        long     bangle;
        int     damage;
        int     slope;
        
        if (actor.target==null)
        return;

        S.StartSound(actor, sfxenum_t.sfx_shotgn);
        A_FaceTarget (actor);
        bangle = actor.angle;
        slope = AimLineAttack (actor, bangle, MISSILERANGE);

        for (i=0 ; i<3 ; i++)
        {
        angle = bangle + ((RND.P_Random()-RND.P_Random())<<20);
        damage = ((RND.P_Random()%5)+1)*3;
        LineAttack (actor, angle, MISSILERANGE, slope, damage);
        }
    }

    void A_CPosAttack (mobj_t  actor)
    {
        long     angle;
        long     bangle;
        int     damage;
        int     slope;
        
        if (actor.target==null)
        return;

        S.StartSound(actor, sfxenum_t.sfx_shotgn);
        A_FaceTarget (actor);
        bangle = actor.angle;
        slope = AimLineAttack (actor, bangle, MISSILERANGE);

        angle = bangle + ((RND.P_Random()-RND.P_Random())<<20);
        damage = ((RND.P_Random()%5)+1)*3;
        LineAttack (actor, angle, MISSILERANGE, slope, damage);
    }

    void A_CPosRefire (mobj_t  actor)
    {   
        // keep firing unless target got out of sight
        A_FaceTarget (actor);

        if (RND.P_Random () < 40)
        return;

        if (actor.target==null
        || actor.target.health <= 0
        || !EN.CheckSight (actor, actor.target) )
        {
        actor.SetMobjState ( actor.info.seestate);
        }
    }


    void A_SpidRefire (mobj_t  actor)
    {   
        // keep firing unless target got out of sight
        A_FaceTarget (actor);

        if (RND.P_Random () < 10)
        return;

        if (actor.target==null
        || actor.target.health <= 0
        || !EN.CheckSight (actor, actor.target) )
        {
        actor.SetMobjState ( actor.info.seestate);
        }
    }

    void A_BspiAttack (mobj_t actor)
    {   
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);

        // launch a missile
        SpawnMissile (actor, actor.target, mobjtype_t.MT_ARACHPLAZ);
    }


    //
    // A_TroopAttack
    //
    void A_TroopAttack (mobj_t  actor)
    {
        int     damage;
        
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        if (EN.CheckMeleeRange (actor))
        {
        	S.StartSound(actor, sfxenum_t.sfx_claw);
        damage = (RND.P_Random()%8+1)*3;
        DamageMobj (actor.target, actor, actor, damage);
        return;
        }

        
        // launch a missile
        SpawnMissile (actor, actor.target, mobjtype_t.MT_TROOPSHOT);
    }


    void A_SargAttack (mobj_t  actor)
    {
        int     damage;

        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        if (EN.CheckMeleeRange (actor))
        {
        damage = ((RND.P_Random()%10)+1)*4;
        DamageMobj (actor.target, actor, actor, damage);
        }
    }

    void A_HeadAttack (mobj_t  actor)
    {
        int     damage;
        
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        if (EN.CheckMeleeRange (actor))
        {
        damage = (RND.P_Random()%6+1)*10;
        DamageMobj (actor.target, actor, actor, damage);
        return;
        }
        
        // launch a missile
        SpawnMissile (actor, actor.target, mobjtype_t.MT_HEADSHOT);
    }

    void A_CyberAttack (mobj_t  actor)
    {   
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        SpawnMissile (actor, actor.target, mobjtype_t.MT_ROCKET);
    }


    void A_BruisAttack (mobj_t  actor)
    {
        int     damage;
        
        if (actor.target==null)
        return;
            
        if (EN.CheckMeleeRange (actor))
        {
        	S.StartSound(actor, sfxenum_t.sfx_claw);
        damage = (RND.P_Random()%8+1)*10;
        DamageMobj (actor.target, actor, actor, damage);
        return;
        }
        
        // launch a missile
        SpawnMissile (actor, actor.target, mobjtype_t.MT_BRUISERSHOT);
    }


    //
    // A_SkelMissile
    //
    void A_SkelMissile (mobj_t  actor)
    {   
        mobj_t  mo;
        
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        actor.z += 16*FRACUNIT;    // so missile spawns higher
        mo = SpawnMissile (actor, actor.target, mobjtype_t.MT_TRACER);
        actor.z -= 16*FRACUNIT;    // back to normal

        mo.x += mo.momx;
        mo.y += mo.momy;
        mo.tracer = actor.target;
    }

    private static final int TRACEANGLE = 0xc000000;

    void A_Tracer (mobj_t  actor)
    {
        long exact; //angle_t
        int dist,slope; // fixed
        mobj_t  dest;
        mobj_t  th;
            
        if (flags(DM.gametic ,3))
        return;
        
        // spawn a puff of smoke behind the rocket      
        SpawnPuff (actor.x, actor.y, actor.z);
        
        th = SpawnMobj (actor.x-actor.momx,
                  actor.y-actor.momy,
                  actor.z, mobjtype_t.MT_SMOKE);
        
        th.momz = FRACUNIT;
        th.tics -= RND.P_Random()&3;
        if (th.tics < 1)
        th.tics = 1;
        
        // adjust direction
        dest = actor.tracer;
        
        if (dest==null || dest.health <= 0)
        return;
        
        // change angle 
        exact = R.PointToAngle2 (actor.x,
                     actor.y,
                     dest.x,
                     dest.y);

        if (exact != actor.angle)
        {
        if (exact - actor.angle > 0x80000000)
        {
            actor.angle -= TRACEANGLE;
            if (exact - actor.angle < 0x80000000)
            actor.angle = exact;
        }
        else
        {
            actor.angle += TRACEANGLE;
            if (exact - actor.angle > 0x80000000)
            actor.angle = exact;
        }
        }
        
        exact = actor.angle>>ANGLETOFINESHIFT;
        actor.momx = FixedMul (actor.info.speed, finecosine[(int) exact]);
        actor.momy = FixedMul (actor.info.speed, finesine[(int) exact]);
        
        // change slope
        dist = AproxDistance (dest.x - actor.x,
                    dest.y - actor.y);
        
        dist = dist / actor.info.speed;

        if (dist < 1)
        dist = 1;
        slope = (dest.z+40*FRACUNIT - actor.z) / dist;

        if (slope < actor.momz)
        actor.momz -= FRACUNIT/8;
        else
        actor.momz += FRACUNIT/8;
    }


    void A_SkelWhoosh (mobj_t   actor)
    {
        if (actor.target==null)
        return;
        A_FaceTarget (actor);
        S.StartSound(actor,sfxenum_t.sfx_skeswg);
    }

    void A_SkelFist (mobj_t     actor)
    {
        int     damage;

        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        
        if (EN.CheckMeleeRange (actor))
        {
        damage = ((RND.P_Random()%10)+1)*6;
        S.StartSound(actor, sfxenum_t.sfx_skepch);
        DamageMobj (actor.target, actor, actor, damage);
        }
    }



    //
    // PIT_VileCheck
    // Detect a corpse that could be raised.
    //
    mobj_t      corpsehit;
    mobj_t      vileobj;
    int     viletryx;
    int     viletryy;

    boolean VileCheck (mobj_t   thing)
    {
        int     maxdist;
        boolean check;
        
        if (!flags(thing.flags ,MF_CORPSE) )
        return true;    // not a monster
        
        if (thing.tics != -1)
        return true;    // not lying still yet
        
        if (thing.info.raisestate == statenum_t.S_NULL)
        return true;    // monster doesn't have a raise state
        
        maxdist = thing.info.radius + mobjinfo[mobjtype_t.MT_VILE.ordinal()].radius;
        
        if ( Math.abs(thing.x - viletryx) > maxdist
         || Math.abs(thing.y - viletryy) > maxdist )
        return true;        // not actually touching
            
        corpsehit = thing;
        corpsehit.momx = corpsehit.momy = 0;
        corpsehit.height <<= 2;
        check = CheckPosition (corpsehit, corpsehit.x, corpsehit.y);
        corpsehit.height >>= 2;

        if (!check)
        return true;        // doesn't fit here
            
        return false;       // got one, so stop checking
    }



    //
    // A_VileChase
    // Check for ressurecting a body
    //
    void A_VileChase (mobj_t  actor)
    {
        int         xl;
        int         xh;
        int         yl;
        int         yh;
        
        int         bx;
        int         by;

        mobjinfo_t     info;
        mobj_t      temp;
        
        if (actor.movedir != DI_NODIR)
        {
        // check for corpses to raise
        viletryx =
            actor.x + actor.info.speed*xspeed[actor.movedir];
        viletryy =
            actor.y + actor.info.speed*yspeed[actor.movedir];

        xl = (viletryx - LL.bmaporgx - MAXRADIUS*2)>>MAPBLOCKSHIFT;
        xh = (viletryx - LL.bmaporgx + MAXRADIUS*2)>>MAPBLOCKSHIFT;
        yl = (viletryy - LL.bmaporgy - MAXRADIUS*2)>>MAPBLOCKSHIFT;
        yh = (viletryy - LL.bmaporgy + MAXRADIUS*2)>>MAPBLOCKSHIFT;
        
        vileobj = actor;
        for (bx=xl ; bx<=xh ; bx++)
        {
            for (by=yl ; by<=yh ; by++)
            {
            // Call PIT_VileCheck to check
            // whether object is a corpse
            // that canbe raised.
            if (!BlockThingsIterator(bx,by,PIT.VileCheck))
            {
                // got one!
                temp = actor.target;
                actor.target = corpsehit;
                A_FaceTarget (actor);
                actor.target = temp;
                        
                actor.SetMobjState ( statenum_t.S_VILE_HEAL1);
                S.StartSound(corpsehit, sfxenum_t.sfx_slop);
                info = corpsehit.info;
                
                corpsehit.SetMobjState (info.raisestate);
                corpsehit.height <<= 2;
                corpsehit.flags = info.flags;
                corpsehit.health = info.spawnhealth;
                corpsehit.target = null;

                return;
            }
            }
        }
        }

        // Return to normal attack.
        A_Chase (actor);
    }


    //
    // A_VileStart
    //
    void A_VileStart (mobj_t  actor)
    {
    	S.StartSound(actor, sfxenum_t.sfx_vilatk);
    }


    //
    // A_Fire
    // Keep fire in front of player unless out of sight
    //
    
    void A_StartFire (mobj_t  actor)
    {
    	S.StartSound(actor,sfxenum_t.sfx_flamst);
        A_Fire(actor);
    }

    void A_FireCrackle (mobj_t  actor)
    {
    	S.StartSound(actor,sfxenum_t.sfx_flame);
        A_Fire(actor);
    }

    void A_Fire (mobj_t  actor)
    {
        mobj_t  dest;
        //long    an;
            
        dest = actor.tracer;
        if (dest==null)
        return;
            
        // don't move it if the vile lost sight
        if (!EN.CheckSight (actor.target, dest) )
        return;

        // an = dest.angle >>> ANGLETOFINESHIFT;

        UnsetThingPosition (actor);
        actor.x = dest.x + FixedMul (24*FRACUNIT, finecosine(dest.angle));
        actor.y = dest.y + FixedMul (24*FRACUNIT, finesine(dest.angle));
        actor.z = dest.z;
        SetThingPosition (actor);
    }



    //
    // A_VileTarget
    // Spawn the hellfire
    //
    void A_VileTarget (mobj_t   actor)
    {
        mobj_t  fog;
        
        if (actor.target==null)
        return;

        A_FaceTarget (actor);

        fog = SpawnMobj (actor.target.x,
                   actor.target.x,
                   actor.target.z, mobjtype_t.MT_FIRE);
        
        actor.tracer = fog;
        fog.target = actor;
        fog.tracer = actor.target;
        A_Fire (fog);
    }




    //
    // A_VileAttack
    //
    void A_VileAttack (mobj_t  actor)
    {   
        mobj_t  fire;
        //int     an;
        
        if (actor.target==null)
        return;
        
        A_FaceTarget (actor);

        if (!EN.CheckSight (actor, actor.target) )
        return;

        S.StartSound(actor, sfxenum_t.sfx_barexp);
        DamageMobj (actor.target, actor, actor, 20);
        actor.target.momz = 1000*FRACUNIT/actor.target.info.mass;
        
       // an = actor.angle >> ANGLETOFINESHIFT;

        fire = actor.tracer;

        if (fire==null)
        return;
            
        // move the fire between the vile and the player
        fire.x = actor.target.x - FixedMul (24*FRACUNIT, finecosine(actor.angle));
        fire.y = actor.target.y - FixedMul (24*FRACUNIT, finesine(actor.angle));  
        RadiusAttack (fire, actor, 70 );
    }




    //
    // Mancubus attack,
    // firing three missiles (bruisers)
    // in three different directions?
    // Doesn't look like it. 
    //
    private static final long FATSPREAD =  (ANG90/8);

    void A_FatRaise (mobj_t actor)
    {
        A_FaceTarget (actor);
        S.StartSound(actor, sfxenum_t.sfx_manatk);
    }


    void A_FatAttack1 (mobj_t  actor)
    {
        mobj_t  mo;
        int     an;
        
        A_FaceTarget (actor);
        // Change direction  to ...
        actor.angle += FATSPREAD;
        SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);

        mo = SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle += FATSPREAD;
        an = (int) (mo.angle >> ANGLETOFINESHIFT);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);
    }

    void A_FatAttack2 (mobj_t  actor)
    {
        mobj_t  mo;
        int     an;

        A_FaceTarget (actor);
        // Now here choose opposite deviation.
        actor.angle -= FATSPREAD;
        SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);

        mo = SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle -= FATSPREAD*2;
        an = (int) (mo.angle >> ANGLETOFINESHIFT);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);
    }

    void A_FatAttack3 (mobj_t   actor)
    {
        mobj_t  mo;
        int     an;

        A_FaceTarget (actor);
        
        mo = SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle -= FATSPREAD/2;
        an = (int) (mo.angle >> ANGLETOFINESHIFT);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);

        mo = SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle += FATSPREAD/2;
        an = (int) (mo.angle >> ANGLETOFINESHIFT);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);
    }



    private static final int SKULLSPEED  =    (20*FRACUNIT);
    /**      
     * SkullAttack
     * Fly at the player like a missile.
     */
    void A_SkullAttack (mobj_t  actor)
    {
        mobj_t      dest;
        int     an;
        int         dist;

        if (actor.target==null)
        return;
            
        dest = actor.target;   
        actor.flags |= MF_SKULLFLY;

        S.StartSound(actor, actor.info.attacksound);
        A_FaceTarget (actor);
        an = (int) (actor.angle >> ANGLETOFINESHIFT);
        actor.momx = FixedMul (SKULLSPEED, finecosine[an]);
        actor.momy = FixedMul (SKULLSPEED, finesine[an]);
        dist = AproxDistance (dest.x - actor.x, dest.y - actor.y);
        dist = dist / SKULLSPEED;
        
        if (dist < 1)
        dist = 1;
        actor.momz = (dest.z+(dest.height>>1) - actor.z) / dist;
    }


    /**
     * A_PainShootSkull
     * Spawn a lost soul and launch it at the target
     */
    
    void
    A_PainShootSkull
    ( mobj_t    actor,
      long   angle )
    {
        int x,y, z; // fixed
        
        mobj_t  newmobj;
        int an; // angle
        int     prestep;
        int     count;
        thinker_t  currentthinker;

        // count total number of skull currently on the level
        count = 0;

        currentthinker = thinkercap.next;
        while (currentthinker != thinkercap)
        {
        if (   (currentthinker.function == think_t.P_MobjThinker)
            && ((mobj_t)currentthinker).type == mobjtype_t.MT_SKULL)
            count++;
        currentthinker = currentthinker.next;
        }

        // if there are allready 20 skulls on the level,
        // don't spit another one
        if (count > MAXSKULLS)
        return;


        // okay, there's playe for another one
        an = (int) (angle >> ANGLETOFINESHIFT);
        
        prestep =
        4*FRACUNIT
        + 3*(actor.info.radius + mobjinfo[mobjtype_t.MT_SKULL.ordinal()].radius)/2;
        
        x = actor.x + FixedMul (prestep, finecosine[an]);
        y = actor.y + FixedMul (prestep, finesine[an]);
        z = actor.z + 8*FRACUNIT;
            
        newmobj = SpawnMobj (x , y, z, mobjtype_t.MT_SKULL);

        // Check for movements.
        if (!TryMove (newmobj, newmobj.x, newmobj.y))
        {
        // kill it immediately
        DamageMobj (newmobj,actor,actor,10000);   
        return;
        }
            
        newmobj.target = actor.target;
        A_SkullAttack (newmobj);
    }


    //
    // A_PainAttack
    // Spawn a lost soul and launch it at the target
    // 
    void A_PainAttack (mobj_t  actor)
    {
        if (actor.target==null)
        return;

        A_FaceTarget (actor);
        A_PainShootSkull (actor, actor.angle);
    }


    void A_PainDie (mobj_t  actor)
    {
        A_Fall (actor);
        A_PainShootSkull (actor, actor.angle+ANG90);
        A_PainShootSkull (actor, actor.angle+ANG180);
        A_PainShootSkull (actor, actor.angle+ANG270);
    }






    void A_Scream (mobj_t  actor)
    {
        int     sound;
        
        switch (actor.info.deathsound)
        {
          case sfx_None:
        return;
            
          case sfx_podth1:
          case sfx_podth2:
          case sfx_podth3:
        sound = sfxenum_t.sfx_podth1.ordinal() + RND.P_Random ()%3;
        break;
            
          case sfx_bgdth1:
          case sfx_bgdth2:
        sound = sfxenum_t.sfx_bgdth1.ordinal() + RND.P_Random ()%2;
        break;
        
          default:
        sound = actor.info.deathsound.ordinal();
        break;
        }

        // Check for bosses.
        if (actor.type==mobjtype_t.MT_SPIDER
        || actor.type == mobjtype_t.MT_CYBORG)
        {
        // full volume
        S.StartSound (null, sound);
        }
        else
        S.StartSound (actor, sound);
    }


    void A_XScream (mobj_t  actor)
    {
        S.StartSound (actor, sfxenum_t.sfx_slop); 
    }

    void A_Pain (mobj_t  actor)
    {
        if (actor.info.painsound!=null)
        	S.StartSound(actor, actor.info.painsound);   
    }



    void A_Fall (mobj_t actor)
    {
        // actor is on ground, it can be walked over
        actor.flags &= ~MF_SOLID;

        // So change this if corpse objects
        // are meant to be obstacles.
    }


    //
    // A_Explode
    //
    void A_Explode (mobj_t  thingy)
    {
        RadiusAttack ( thingy, thingy.target, 128 );
    }


    //
    // A_BossDeath
    // Possibly trigger special effects
    // if on first boss level
    //
    void A_BossDeath (mobj_t  mo)
    {
        thinker_t  th;
        mobj_t  mo2;
        line_t  junk = new line_t();
        int     i;
            
        if ( DM.gamemode == GameMode_t.commercial)
        {
        if (DM.gamemap != 7)
            return;
            
        if ((mo.type != mobjtype_t.MT_FATSO)
            && (mo.type != mobjtype_t.MT_BABY))
            return;
        }
        else
        {
        switch(DM.gameepisode)
        {
          case 1:
            if (DM.gamemap != 8)
            return;

            if (mo.type != mobjtype_t.MT_BRUISER)
            return;
            break;
            
          case 2:
            if (DM.gamemap != 8)
            return;

            if (mo.type != mobjtype_t.MT_CYBORG)
            return;
            break;
            
          case 3:
            if (DM.gamemap != 8)
            return;
            
            if (mo.type != mobjtype_t.MT_SPIDER)
            return;
            
            break;
            
          case 4:
            switch(DM.gamemap)
            {
              case 6:
            if (mo.type != mobjtype_t.MT_CYBORG)
                return;
            break;
            
              case 8: 
            if (mo.type != mobjtype_t.MT_SPIDER)
                return;
            break;
            
              default:
            return;
            }
            break;
            
          default:
            if (DM.gamemap != 8)
            return;
            break;
        }
            
        }

        
        // make sure there is a player alive for victory
        for (i=0 ; i<MAXPLAYERS ; i++)
        if (DM.playeringame[i] && DM.players[i].health[0] > 0)
            break;
        
        if (i==MAXPLAYERS)
        return; // no one left alive, so do not end game
        
        // scan the remaining thinkers to see
        // if all bosses are dead
        for (th = thinkercap.next ; th != thinkercap ; th=th.next)
        {
        if (th.function != think_t.P_MobjThinker)
            continue;
        
        mo2 = (mobj_t)th;
        if (mo2 != mo
            && mo2.type == mo.type
            && mo2.health > 0)
        {
            // other boss not dead
            return;
        }
        }
        
        // victory!
        if ( DM.gamemode == GameMode_t.commercial)
        {
        if (DM.gamemap == 7)
        {
            if (mo.type == mobjtype_t.MT_FATSO)
            {
            junk.tag = 666;
            DoFloor(junk,floor_e.lowerFloorToLowest);
            return;
            }
            
            if (mo.type == mobjtype_t.MT_BABY)
            {
            junk.tag = 667;
            DoFloor(junk,floor_e.raiseToTexture);
            return;
            }
        }
        }
        else
        {
        switch(DM.gameepisode)
        {
          case 1:
            junk.tag = 666;
            DoFloor (junk, floor_e.lowerFloorToLowest);
            return;
            
          case 4:
            switch(DM.gamemap)
            {
              case 6:
            junk.tag = 666;
            DoDoor (junk, vldoor_e.blazeOpen);
            return;
            
              case 8:
            junk.tag = 666;
            DoFloor (junk, floor_e.lowerFloorToLowest);
            return;
            }
        }
        }
        
        DM.ExitLevel ();
    }


    void A_Hoof (mobj_t  mo)
    {
    	S.StartSound(mo, sfxenum_t.sfx_hoof);
        A_Chase (mo);
    }

    void A_Metal (mobj_t  mo)
    {
    	S.StartSound(mo, sfxenum_t.sfx_metal);
        A_Chase (mo);
    }

    void A_BabyMetal (mobj_t  mo)
    {
    	S.StartSound(mo, sfxenum_t.sfx_bspwlk);
        A_Chase (mo);
    }

    void
    A_OpenShotgun2
    ( player_t player,
      pspdef_t psp )
    {
    	S.StartSound(player.mo, sfxenum_t.sfx_dbopn);
    }

    void
    A_LoadShotgun2
    ( player_t player,
      pspdef_t psp )
    {
    	S.StartSound(player.mo, sfxenum_t.sfx_dbload);
    }

    void
    A_CloseShotgun2
    ( player_t player,
      pspdef_t psp )
    {
    	S.StartSound(player.mo, sfxenum_t.sfx_dbcls);
        A_ReFire(player,psp);
    }



    mobj_t[]      braintargets=new mobj_t[NUMBRAINTARGETS];
    int     numbraintargets;
    int     braintargeton;

    void A_BrainAwake (mobj_t  mo)
    {
        thinker_t  thinker;
        mobj_t  m;
        
        // find all the target spots
        numbraintargets = 0;
        braintargeton = 0;
        
        thinker = thinkercap.next;
        for (thinker = thinkercap.next ;
         thinker != thinkercap ;
         thinker = thinker.next)
        {
        if (thinker.function != think_t.P_MobjThinker)
            continue;   // not a mobj

        m = (mobj_t)thinker;

        if (m.type == mobjtype_t.MT_BOSSTARGET )
        {
            braintargets[numbraintargets] = m;
            numbraintargets++;
        }
        }
        
        S.StartSound(null,sfxenum_t.sfx_bossit);
    }


    void A_BrainPain (mobj_t    mo)
    {
    	S.StartSound(null,sfxenum_t.sfx_bospn);
    }


    void A_BrainScream (mobj_t  mo)
    {
        int     x;
        int     y;
        int     z;
        mobj_t  th;
        
        for (x=mo.x - 196*FRACUNIT ; x< mo.x + 320*FRACUNIT ; x+= FRACUNIT*8)
        {
        y = mo.y - 320*FRACUNIT;
        z = 128 + RND.P_Random()*2*FRACUNIT;
        th = SpawnMobj (x,y,z, mobjtype_t.MT_ROCKET);
        th.momz = RND.P_Random()*512;

        th.SetMobjState (statenum_t.S_BRAINEXPLODE1);

        th.tics -= RND.P_Random()&7;
        if (th.tics < 1)
            th.tics = 1;
        }
        
        S.StartSound(null,sfxenum_t.sfx_bosdth);
    }



    void A_BrainExplode (mobj_t  mo)
    {
        int     x;
        int     y;
        int     z;
        mobj_t  th;
        
        x = mo.x + (RND.P_Random () - RND.P_Random ())*2048;
        y = mo.y;
        z = 128 + RND.P_Random()*2*FRACUNIT;
        th = SpawnMobj (x,y,z, mobjtype_t.MT_ROCKET);
        th.momz = RND.P_Random()*512;

        th.SetMobjState (statenum_t.S_BRAINEXPLODE1);

        th.tics -= RND.P_Random()&7;
        if (th.tics < 1)
        th.tics = 1;
    }


    void A_BrainDie (mobj_t     mo)
    {
        DM.ExitLevel ();
    }

    private int  easy = 0;
    
    void A_BrainSpit (mobj_t    mo)
    {
        mobj_t  targ;
        mobj_t  newmobj;
        
        easy ^= 1;
        if (DM.gameskill.ordinal() <= skill_t.sk_easy.ordinal() && (easy==0))
        return;
            
        // shoot a cube at current target
        targ = braintargets[braintargeton];
        braintargeton = (braintargeton+1)%numbraintargets;

        // spawn brain missile
        newmobj = SpawnMissile (mo, targ, mobjtype_t.MT_SPAWNSHOT);
        newmobj.target = targ;
        newmobj.reactiontime =
        (int) (((targ.y - mo.y)/newmobj.momy) / newmobj.state.tics);

         S.StartSound(null, sfxenum_t.sfx_bospit);
    }


    // travelling cube sound
    void A_SpawnSound (mobj_t  mo)  
    {
        S.StartSound (mo,sfxenum_t.sfx_boscub);
        A_SpawnFly(mo);
    }

     
    void A_SpawnFly (mobj_t  mo)
    {
        mobj_t  newmobj;
        mobj_t  fog;
        mobj_t  targ;
        int     r;
        mobjtype_t  type;
        
        if (--mo.reactiontime!=0)
        return; // still flying
        
        targ = mo.target;

        // First spawn teleport fog.
        fog = SpawnMobj (targ.x, targ.y, targ.z, mobjtype_t.MT_SPAWNFIRE);
        S.StartSound (fog, sfxenum_t.sfx_telept);

        // Randomly select monster to spawn.
        r = RND.P_Random ();

        // Probability distribution (kind of :),
        // decreasing likelihood.
        if ( r<50 )
        type = mobjtype_t.MT_TROOP;
        else if (r<90)
        type = mobjtype_t.MT_SERGEANT;
        else if (r<120)
        type = mobjtype_t.MT_SHADOWS;
        else if (r<130)
        type = mobjtype_t.MT_PAIN;
        else if (r<160)
        type = mobjtype_t.MT_HEAD;
        else if (r<162)
        type = mobjtype_t.MT_VILE;
        else if (r<172)
        type = mobjtype_t.MT_UNDEAD;
        else if (r<192)
        type = mobjtype_t.MT_BABY;
        else if (r<222)
        type = mobjtype_t.MT_FATSO;
        else if (r<246)
        type = mobjtype_t.MT_KNIGHT;
        else
        type = mobjtype_t.MT_BRUISER;      

        newmobj = SpawnMobj (targ.x, targ.y, targ.z, type);
        if (EN.LookForPlayers (newmobj, true) )
        SetMobjState (newmobj, newmobj.info.seestate);
        
        // telefrag anything in this spot
        TeleportMove (newmobj, newmobj.x, newmobj.y);

        // remove self (i.e., cube).
        RemoveMobj (mo);
    }
  
//
//P_MobjThinker
//
  
      public void A_PlayerScream (mobj_t mo)
      {
          // Default death sound.
          sfxenum_t     sound = sfxenum_t.sfx_pldeth;
          
          if ( (DM.gamemode == GameMode_t.commercial)
          &&  (mo.health < -50))
          {
          // IF THE PLAYER DIES
          // LESS THAN -50% WITHOUT GIBBING
          sound =  sfxenum_t.sfx_pdiehi;
          }
          
          S.StartSound(mo, sound);
      }

/** Causes object to move and perform actions. \
 *  Can only be called through the Actions dispatcher.
 * 
 * @param mobj
 */
      
      
public void P_MobjThinker (mobj_t mobj) {
// momentum movement
if (mobj.momx!=0
|| mobj.momy!=0
|| (flags(mobj.flags,MF_SKULLFLY)) )
{
XYMovement(mobj);

// FIXME: decent NOP/NULL/Nil function pointer please.
if (mobj.function == null)
return;     // mobj was removed
}
if ( (mobj.z != mobj.floorz)
|| mobj.momz!=0 )
{
mobj.ZMovement ();

// FIXME: decent NOP/NULL/Nil function pointer please.
if (mobj.function == null)
return;     // mobj was removed
}


// cycle through states,
// calling action functions at transitions
if (mobj.tics != -1)
{
mobj.tics--;

// you can cycle through multiple states in a tic
if (!eval(mobj.tics))
if (!mobj.SetMobjState (mobj.state.nextstate) )
return;     // freed itself
}
else
{
// check for nightmare respawn
if (! flags(mobj.flags ,MF_COUNTKILL) )
return;

if (!DM.respawnmonsters)
return;

mobj.movecount++;

if (mobj.movecount < 12*35)
return;

if ( flags(DM.leveltime,31 ))
return;

if (RND.P_Random () > 4)
return;

NightmareRespawn (mobj);
}
}

/**
 * P_NightmareRespawn
 */
void
NightmareRespawn (mobj_t mobj)
{
int     x,y, z; // fixed 
subsector_t    ss; 
mobj_t     mo;
mapthing_t     mthing;
  
x = mobj.spawnpoint.x << FRACBITS; 
y = mobj.spawnpoint.y << FRACBITS; 

// somthing is occupying it's position?
if (!CheckPosition (mobj, x, y) ) 
return; // no respwan

// spawn a teleport fog at old spot
// because of removal of the body?
mo = SpawnMobj (mobj.x,
        mobj.y,
        mobj.subsector.sector.floorheight , mobjtype_t.MT_TFOG); 
// initiate teleport sound
S.StartSound(mo, sfxenum_t.sfx_telept);

// spawn a teleport fog at the new spot
ss = R.PointInSubsector (x,y); 

mo = SpawnMobj (x, y, ss.sector.floorheight , mobjtype_t.MT_TFOG); 

S.StartSound(mo, sfxenum_t.sfx_telept);

// spawn the new monster
mthing = mobj.spawnpoint;

// spawn it
if (flags(mobj.info.flags , MF_SPAWNCEILING))
z = ONCEILINGZ;
else
z = ONFLOORZ;

// inherit attributes from deceased one
mo = SpawnMobj (x,y,z, mobj.type);
mo.spawnpoint = mobj.spawnpoint;  
mo.angle = ANG45 * (mthing.angle/45);

if (flags(mthing.options , MTF_AMBUSH))
mo.flags |= MF_AMBUSH;

mo.reactiontime = 18;

// remove the old monster,
RemoveMobj (mobj);
}

/** P_SpawnMobj
 * 
 * @param x fixed
 * @param y fixed
 * @param z fixed
 * @param type
 * @return
 */

public mobj_t
SpawnMobj
( int   x,
        int   y,
        int   z,
mobjtype_t    type )
{
mobj_t mobj;
state_t    st;
mobjinfo_t info;

mobj = new mobj_t(this);
info = mobjinfo[type.ordinal()];

mobj.type = type;
mobj.info = info;
mobj.x = x;
mobj.y = y;
mobj.radius = info.radius;
mobj.height = info.height;
mobj.flags = info.flags;
mobj.health = info.spawnhealth;

if (DM.gameskill != skill_t.sk_nightmare)
mobj.reactiontime = info.reactiontime;

mobj.lastlook = RND.P_Random () % MAXPLAYERS;
// do not set the state with P_SetMobjState,
// because action routines can not be called yet
st = states[info.spawnstate.ordinal()];

mobj.state = st;
mobj.tics = st.tics;
mobj.sprite = st.sprite;
mobj.frame = st.frame;

// set subsector and/or block links
SetThingPosition (mobj);

mobj.floorz = mobj.subsector.sector.floorheight;
mobj.ceilingz = mobj.subsector.sector.ceilingheight;

if (z == ONFLOORZ)
mobj.z = mobj.floorz;
else if (z == ONCEILINGZ)
mobj.z = mobj.ceilingz - mobj.info.height;
else 
mobj.z = z;

mobj.function=think_t.P_MobjThinker;

AddThinker (mobj);

return mobj;
}

/**
 * P_RespawnSpecials
 */

void RespawnSpecials ()
{
int     x, y,z; // fixed

subsector_t    ss; 
mobj_t     mo;
mapthing_t     mthing;

int         i;

// only respawn items in deathmatch (deathmatch!=2)
if (!DM.altdeath)
return; // 

// nothing left to respawn?
if (iquehead == iquetail)
return;     

// wait at least 30 seconds
if (DM.leveltime - itemrespawntime[iquetail] < 30*35)
return;         

mthing = itemrespawnque[iquetail];

x = mthing.x << FRACBITS; 
y = mthing.y << FRACBITS; 

// spawn a teleport fog at the new spot
ss = R.PointInSubsector (x,y); 
mo = SpawnMobj (x, y, ss.sector.floorheight , mobjtype_t.MT_IFOG); 
S.StartSound(mo, sfxenum_t.sfx_itmbk);

// find which type to spawn
for (i=0 ; i< mobjtype_t.NUMMOBJTYPES.ordinal() ; i++)
{
if (mthing.type == mobjinfo[i].doomednum)
  break;
}

// spawn it
if (flags(mobjinfo[i].flags ,MF_SPAWNCEILING))
z = ONCEILINGZ;
else
z = ONFLOORZ;

mo = SpawnMobj (x,y,z, mobjtype_t.values()[i]);
mo.spawnpoint = mthing;   
mo.angle = ANG45 * (mthing.angle/45);

// pull it from the que
iquetail = (iquetail+1)&(ITEMQUESIZE-1);
}

/**
 * P_SpawnPlayer
 * Called when a player is spawned on the level.
 * Most of the player structure stays unchanged
 *  between levels.
 */
public void SpawnPlayer (mapthing_t mthing)
{
player_t       p;
int     x;
int     y;
int     z;

mobj_t     mobj;

int         i;

// not playing?
if (!DM.playeringame[mthing.type-1])
return;                 
  
p = DM.players[mthing.type-1];

if (p.playerstate == PST_REBORN)
DM.PlayerReborn (mthing.type-1);

x       = mthing.x << FRACBITS;
y       = mthing.y << FRACBITS;
z       = ONFLOORZ;
mobj    = SpawnMobj (x,y,z, mobjtype_t.MT_PLAYER);

// set color translations for player sprites
if (mthing.type > 1)       
mobj.flags |= (mthing.type-1)<<MF_TRANSSHIFT;
  
mobj.angle = ANG45 * (mthing.angle/45);
mobj.player = p;
mobj.health = p.health[0];

p.mo = mobj;
p.playerstate = PST_LIVE;  
p.refire = 0;
p.message = null;
p.damagecount = 0;
p.bonuscount = 0;
p.extralight = 0;
p.fixedcolormap = 0;
p.viewheight = VIEWHEIGHT;

// setup gun psprite
p.SetupPsprites ();

// give all cards in death match mode
if (DM.deathmatch)
for (i=0 ; i<NUMCARDS ; i++)
  p.cards[i] = true;
      
if (mthing.type-1 == DM.consoleplayer)
{
// wake up the status bar
ST.Start ();
// wake up the heads up text
HU.Start ();        
}
}

/**
 * P_SpawnMapThing
 * The fields of the mapthing should
 * already be in host byte order.
 */

void SpawnMapThing (mapthing_t mthing)
{
int         i;
int         bit;
mobj_t     mobj;
int     x;
int     y;
int     z;
  
// count deathmatch start positions
if (mthing.type == 11)
{
if (DM.deathmatch_p < 10/*DM.deathmatchstarts[10]*/)
{
 // memcpy (deathmatch_p, mthing, sizeof(*mthing));
    DM.deathmatchstarts[DM.deathmatch_p]=mthing.clone();
  DM.deathmatch_p++;
}
return;
}

// check for players specially
if (mthing.type <= 4)
{
// save spots for respawning in network games
DM.playerstarts[mthing.type-1] = mthing;
if (!DM.deathmatch)
  SpawnPlayer (mthing);

return;
}

// check for apropriate skill level
if (!DM.netgame && flags(mthing.options , 16) )
return;
  
if (DM.gameskill == skill_t.sk_baby)
bit = 1;
else if (DM.gameskill == skill_t.sk_nightmare)
bit = 4;
else
bit = 1<<(DM.gameskill.ordinal()-1);

if (!flags(mthing.options , bit) )
return;

// find which type to spawn
for (i=0 ; i< NUMMOBJTYPES ; i++)
if (mthing.type == mobjinfo[i].doomednum)
  break;

if (i==NUMMOBJTYPES)
I.Error ("P_SpawnMapThing: Unknown type %i at (%i, %i)",
   mthing.type,
   mthing.x, mthing.y);
  
// don't spawn keycards and players in deathmatch
if (DM.deathmatch && flags(mobjinfo[i].flags , MF_NOTDMATCH))
return;
  
// don't spawn any monsters if -nomonsters
if (DM.nomonsters
&& ( i == mobjtype_t.MT_SKULL.ordinal()
   || flags(mobjinfo[i].flags , MF_COUNTKILL)) )
{
return;
}

// spawn it
x = mthing.x << FRACBITS;
y = mthing.y << FRACBITS;

if (flags(mobjinfo[i].flags , MF_SPAWNCEILING))
z = ONCEILINGZ;
else
z = ONFLOORZ;

mobj = SpawnMobj (x,y,z, mobjtype_t.values()[i]);
mobj.spawnpoint = mthing;

if (mobj.tics > 0)
mobj.tics = 1 + (RND.P_Random () % mobj.tics);
if (flags(mobj.flags , MF_COUNTKILL))
DM.totalkills++;
if (flags(mobj.flags , MF_COUNTITEM))
DM.totalitems++;
  
mobj.angle = ANG45 * (mthing.angle/45);
if (flags(mthing.options , MTF_AMBUSH))
mobj.flags |= MF_AMBUSH;
}

/** P_SpawnBlood
 * 
 * @param x fixed
 * @param y fixed
 * @param z fixed
 * @param damage
 */

void
SpawnBlood
( int   x,
        int   y,
        int   z,
int       damage )
{
mobj_t th;

z += ((RND.P_Random()-RND.P_Random())<<10);
th = SpawnMobj (x,y,z, mobjtype_t.MT_BLOOD);
th.momz = FRACUNIT*2;
th.tics -= RND.P_Random()&3;

if (th.tics < 1)
th.tics = 1;
  
if (damage <= 12 && damage >= 9)
    th.SetMobjState (statenum_t.S_BLOOD2);
else if (damage < 9)
    th.SetMobjState (statenum_t.S_BLOOD3);
}

/** P_SpawnPuff
 * 
 * @param x fixed
 * @param y fixed
 * @param z fixed
 * 
 */

void
SpawnPuff
( int   x,
int   y,
int   z )
{
mobj_t th;

z += ((RND.P_Random()-RND.P_Random())<<10);

th = SpawnMobj (x,y,z, mobjtype_t.MT_PUFF);
th.momz = FRACUNIT;
th.tics -= RND.P_Random()&3;

if (th.tics < 1)
th.tics = 1;

// don't make punches spark on the wall
if (attackrange == MELEERANGE)
    th.SetMobjState (statenum_t.S_PUFF3);
}

/**
 * P_SpawnMissile
 */

protected mobj_t
SpawnMissile
( mobj_t   source,
mobj_t   dest,
mobjtype_t    type )
{
mobj_t th;
long an; // angle_t
int     dist;

th = SpawnMobj (source.x,
        source.y,
        source.z + 4*8*FRACUNIT, type);

if (th.info.seesound!=null)
	S.StartSound(th, th.info.seesound);

th.target = source;    // where it came from
an = R.PointToAngle2 (source.x, source.y, dest.x, dest.y);  

// fuzzy player
if (flags(dest.flags , MF_SHADOW))
an += (RND.P_Random()-RND.P_Random())<<20; 

th.angle = an&BITS32;;
//an >>= ANGLETOFINESHIFT;
th.momx = FixedMul (th.info.speed, finecosine(an));
th.momy = FixedMul (th.info.speed, finesine(an));

dist = AproxDistance (dest.x - source.x, dest.y - source.y);
dist = dist / th.info.speed;

if (dist < 1)
dist = 1;

th.momz = (dest.z - source.z) / dist;
CheckMissileSpawn (th);

return th;
}


/**
* P_SpawnPlayerMissile
* Tries to aim at a nearby monster
*/

public void
SpawnPlayerMissile
( mobj_t   source,
mobjtype_t    type )
{
mobj_t th;
long an; // angle_t
int x, y, z,slope; // think_t

// see which target is to be aimed at
an = source.angle;
slope = AimLineAttack (source, an, 16*64*FRACUNIT);

if (linetarget==null)
{
an += 1<<26;
an&=BITS32;
slope = AimLineAttack (source, an, 16*64*FRACUNIT);

if (linetarget==null)
{
  an -= 2<<26;
  an&=BITS32;
  slope = AimLineAttack (source, an, 16*64*FRACUNIT);
}

if (linetarget==null)
{
  an = source.angle;
  // angle should be "sane"..right?
  slope = 0;
}
}
  
x = source.x;
y = source.y;
z = source.z + 4*8*FRACUNIT;

th = SpawnMobj (x,y,z, type);

if (th.info.seesound!=null)
	S.StartSound(th, th.info.seesound);

th.target = source;
th.angle = an;
an>>>=ANGLETOFINESHIFT;
th.momx = FixedMul( th.info.speed,
       finecosine[(int) an]);
th.momy = FixedMul( th.info.speed,
       finesine[(int) an]);
th.momz = FixedMul( th.info.speed, slope);

CheckMissileSpawn (th);
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
    DamageMobj
    ( mobj_t   target,
      mobj_t   inflictor,
      mobj_t   source,
      int       damage )
    {
        long    ang; // unsigned
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
        if ((player!=null) && DM.gameskill == skill_t.sk_baby)
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
            
        //ang >>= ANGLETOFINESHIFT;
        target.momx += FixedMul (thrust, finecosine(ang));
        target.momy += FixedMul (thrust, finesine(ang));
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

        if (player == DM.players[DM.consoleplayer]) 
            I.Tactile (40,10,40+temp*2);
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
           source.player.frags[target.player.identify()]++;
           // It's probably intended to increment the frags of source player vs target player. Lookup? 
        }
        else if (!DM.netgame && ((target.flags & MF_COUNTKILL)!=0) )
        {
        // count all monster deaths,
        // even those caused by other monsters
        DM.players[0].killcount++;
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
        target.player.DropWeapon (); // in PSPR

        if (target.player == DM.players[DM.consoleplayer]
            && DM.automapactive)
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

        mo = SpawnMobj (target.x,target.y,ONFLOORZ, item);
        mo.flags |= MF_DROPPED;    // special versions of items
    }
    
    //
 // TELEPORTATION
 //
 int
 Teleport
 ( line_t    line,
   int       side,
   mobj_t    thing )
 {
     int     i;
     int     tag;
     mobj_t  m;     
     mobj_t  fog;
     int an;
     thinker_t   thinker;
     sector_t    sector;
     int oldx, oldy, oldz; // fixed_t

     // don't teleport missiles
     if ((thing.flags & MF_MISSILE)!=0)
     return 0;       

     // Don't teleport if hit back of line,
     //  so you can get out of teleporter.
     if (side == 1)      
     return 0;   

     
     tag = line.tag;
     for (i = 0; i < LL.numsectors; i++)
     {
     if (LL.sectors[ i ].tag == tag )
     {
         thinker = thinkercap.next;
         for (thinker = thinkercap.next;
          thinker != thinkercap;
          thinker = thinker.next)
         {
         // not a mobj
         if (thinker.function != think_t.P_MobjThinker)
             continue;   

         m = (mobj_t)thinker;
         
         // not a teleportman
         if (m.type != mobjtype_t.MT_TELEPORTMAN )
             continue;       

         sector = m.subsector.sector;
         // wrong sector
         if (sector.id != i )
             continue;   

         oldx = thing.x;
         oldy = thing.y;
         oldz = thing.z;
                 
         if (!TeleportMove (thing, m.x, m.y))
             return 0;
         
         thing.z = thing.floorz;  //fixme: not needed?
         if (thing.player!=null)
             thing.player.viewz = thing.z+thing.player.viewheight;
                 
         // spawn teleport fog at source and destination
         fog = SpawnMobj (oldx, oldy, oldz, mobjtype_t.MT_TFOG);
         S.StartSound( fog, sfxenum_t.sfx_telept);
         an = (int) (m.angle >> ANGLETOFINESHIFT);
         fog = SpawnMobj (m.x+20*finecosine[an], m.y+20*finesine[an]
                    , thing.z, mobjtype_t.MT_TFOG);

         // emit sound, where?
         S.StartSound (fog, sfxenum_t.sfx_telept);
         
         // don't move for a bit
         if (thing.player!=null)
             thing.reactiontime = 18;   

         thing.angle = m.angle;
         thing.momx = thing.momy = thing.momz = 0;
         return 1;
         }   
     }
     }
     return 0;
 }
 
 //
//EVENTS
//Events are operations triggered by using, crossing,
//or shooting special lines, or by timed thinkers.
//

/**
* P_CrossSpecialLine - TRIGGER
* Called every time a thing origin is about
*  to cross a line with a non 0 special.
*/

void
CrossSpecialLine
( line_t       line,
int       side,
mobj_t  thing )
{
  //line_t line;
  boolean     ok;

  //line = LL.lines[linenum];
  
  //  Triggers that other things can activate
  if (thing.player==null)
  {
  // Things that should NOT trigger specials...
  switch(thing.type)
  {
    case MT_ROCKET:
    case MT_PLASMA:
    case MT_BFG:
    case MT_TROOPSHOT:
    case MT_HEADSHOT:
    case MT_BRUISERSHOT:
      return;
     // break;
      
    default: break;
  }
      
  ok = false;
  switch(line.special)
  {
    case 39:  // TELEPORT TRIGGER
    case 97:  // TELEPORT RETRIGGER
    case 125: // TELEPORT MONSTERONLY TRIGGER
    case 126: // TELEPORT MONSTERONLY RETRIGGER
    case 4:   // RAISE DOOR
    case 10:  // PLAT DOWN-WAIT-UP-STAY TRIGGER
    case 88:  // PLAT DOWN-WAIT-UP-STAY RETRIGGER
      ok = true;
      break;
  }
  if (!ok)
      return;
  }

  
  // Note: could use some const's here.
  switch (line.special)
  {
  // TRIGGERS.
  // All from here to RETRIGGERS.
    case 2:
  // Open Door
  DoDoor(line,vldoor_e.open);
  line.special = 0;
  break;

    case 3:
  // Close Door
  DoDoor(line,vldoor_e.close);
  line.special = 0;
  break;

    case 4:
  // Raise Door
  DoDoor(line,vldoor_e.normal);
  line.special = 0;
  break;
  
    case 5:
  // Raise Floor
  DoFloor(line,floor_e.raiseFloor);
  line.special = 0;
  break;
  
    case 6:
  // Fast Ceiling Crush & Raise
  DoCeiling(line,ceiling_e.fastCrushAndRaise);
  line.special = 0;
  break;
  
    case 8:
  // Build Stairs
  BuildStairs(line,stair_e.build8);
  line.special = 0;
  break;
  
    case 10:
  // PlatDownWaitUp
  PEV.DoPlat(line,plattype_e.downWaitUpStay,0);
  line.special = 0;
  break;
  
    case 12:
  // Light Turn On - brightest near
  LEV.LightTurnOn(line,0);
  line.special = 0;
  break;
  
    case 13:
  // Light Turn On 255
  LEV.LightTurnOn(line,255);
  line.special = 0;
  break;
  
    case 16:
  // Close Door 30
  DoDoor(line,vldoor_e.close30ThenOpen);
  line.special = 0;
  break;
  
    case 17:
  // Start Light Strobing
  LEV.StartLightStrobing(line);
  line.special = 0;
  break;
  
    case 19:
  // Lower Floor
  DoFloor(line,floor_e.lowerFloor);
  line.special = 0;
  break;
  
    case 22:
  // Raise floor to nearest height and change texture
  PEV.DoPlat(line,plattype_e.raiseToNearestAndChange,0);
  line.special = 0;
  break;
  
    case 25:
  // Ceiling Crush and Raise
  DoCeiling(line,ceiling_e.crushAndRaise);
  line.special = 0;
  break;
  
    case 30:
  // Raise floor to shortest texture height
  //  on either side of lines.
  DoFloor(line,floor_e.raiseToTexture);
  line.special = 0;
  break;
  
    case 35:
  // Lights Very Dark
  LEV.LightTurnOn(line,35);
  line.special = 0;
  break;
  
    case 36:
  // Lower Floor (TURBO)
  DoFloor(line,floor_e.turboLower);
  line.special = 0;
  break;
  
    case 37:
  // LowerAndChange
  DoFloor(line,floor_e.lowerAndChange);
  line.special = 0;
  break;
  
    case 38:
  // Lower Floor To Lowest
  DoFloor( line, floor_e.lowerFloorToLowest );
  line.special = 0;
  break;
  
    case 39:
  // TELEPORT!
  Teleport( line, side, thing );
  line.special = 0;
  break;

    case 40:
  // RaiseCeilingLowerFloor
  DoCeiling( line, ceiling_e.raiseToHighest );
  DoFloor( line, floor_e.lowerFloorToLowest );
  line.special = 0;
  break;
  
    case 44:
  // Ceiling Crush
  DoCeiling( line, ceiling_e.lowerAndCrush );
  line.special = 0;
  break;
  
    case 52:
  // EXIT!
  DM.ExitLevel ();
  break;
  
    case 53:
  // Perpetual Platform Raise
  PEV.DoPlat(line,plattype_e.perpetualRaise,0);
  line.special = 0;
  break;
  
    case 54:
  // Platform Stop
  PEV.StopPlat(line);
  line.special = 0;
  break;

    case 56:
  // Raise Floor Crush
  DoFloor(line,floor_e.raiseFloorCrush);
  line.special = 0;
  break;

    case 57:
  // Ceiling Crush Stop
  CeilingCrushStop(line);
  line.special = 0;
  break;
  
    case 58:
  // Raise Floor 24
  DoFloor(line,floor_e.raiseFloor24);
  line.special = 0;
  break;

    case 59:
  // Raise Floor 24 And Change
  DoFloor(line,floor_e.raiseFloor24AndChange);
  line.special = 0;
  break;
  
    case 104:
  // Turn lights off in sector(tag)
  LEV.TurnTagLightsOff(line);
  line.special = 0;
  break;
  
    case 108:
  // Blazing Door Raise (faster than TURBO!)
  DoDoor (line,vldoor_e.blazeRaise);
  line.special = 0;
  break;
  
    case 109:
  // Blazing Door Open (faster than TURBO!)
        DoDoor (line,vldoor_e.blazeOpen);
  line.special = 0;
  break;
  
    case 100:
  // Build Stairs Turbo 16
  BuildStairs(line,stair_e.turbo16);
  line.special = 0;
  break;
  
    case 110:
  // Blazing Door Close (faster than TURBO!)
  DoDoor (line,vldoor_e.blazeClose);
  line.special = 0;
  break;

    case 119:
  // Raise floor to nearest surr. floor
  DoFloor(line,floor_e.raiseFloorToNearest);
  line.special = 0;
  break;
  
    case 121:
  // Blazing PlatDownWaitUpStay
  PEV.DoPlat(line,plattype_e.blazeDWUS,0);
  line.special = 0;
  break;
  
    case 124:
  // Secret EXIT
  DM.SecretExitLevel ();
  break;
      
    case 125:
  // TELEPORT MonsterONLY
  if (thing.player==null)
  {
      Teleport( line, side, thing );
      line.special = 0;
  }
  break;
  
    case 130:
  // Raise Floor Turbo
  DoFloor(line,floor_e.raiseFloorTurbo);
  line.special = 0;
  break;
  
    case 141:
  // Silent Ceiling Crush & Raise
  DoCeiling(line,ceiling_e.silentCrushAndRaise);
  line.special = 0;
  break;
  
  // RETRIGGERS.  All from here till end.
    case 72:
  // Ceiling Crush
  DoCeiling( line, ceiling_e.lowerAndCrush );
  break;

    case 73:
  // Ceiling Crush and Raise
  DoCeiling(line,ceiling_e.crushAndRaise);
  break;

    case 74:
  // Ceiling Crush Stop
  CeilingCrushStop(line);
  break;
  
    case 75:
  // Close Door
  DoDoor(line,vldoor_e.close);
  break;
  
    case 76:
  // Close Door 30
  DoDoor(line,vldoor_e.close30ThenOpen);
  break;
  
    case 77:
  // Fast Ceiling Crush & Raise
  DoCeiling(line,ceiling_e.fastCrushAndRaise);
  break;
  
    case 79:
  // Lights Very Dark
  LEV.LightTurnOn(line,35);
  break;
  
    case 80:
  // Light Turn On - brightest near
  LEV.LightTurnOn(line,0);
  break;
  
    case 81:
  // Light Turn On 255
  LEV.LightTurnOn(line,255);
  break;
  
    case 82:
  // Lower Floor To Lowest
  DoFloor( line, floor_e.lowerFloorToLowest );
  break;
  
    case 83:
  // Lower Floor
  DoFloor(line,floor_e.lowerFloor);
  break;

    case 84:
  // LowerAndChange
  DoFloor(line,floor_e.lowerAndChange);
  break;

    case 86:
  // Open Door
  DoDoor(line,vldoor_e.open);
  break;
  
    case 87:
  // Perpetual Platform Raise
  PEV.DoPlat(line,plattype_e.perpetualRaise,0);
  break;
  
    case 88:
  // PlatDownWaitUp
  PEV.DoPlat(line,plattype_e.downWaitUpStay,0);
  break;
  
    case 89:
  // Platform Stop
  PEV.StopPlat(line);
  break;
  
    case 90:
  // Raise Door
  DoDoor(line,vldoor_e.normal);
  break;
  
    case 91:
  // Raise Floor
  DoFloor(line,floor_e.raiseFloor);
  break;
  
    case 92:
  // Raise Floor 24
  DoFloor(line,floor_e.raiseFloor24);
  break;
  
    case 93:
  // Raise Floor 24 And Change
  DoFloor(line,floor_e.raiseFloor24AndChange);
  break;
  
    case 94:
  // Raise Floor Crush
  DoFloor(line,floor_e.raiseFloorCrush);
  break;
  
    case 95:
  // Raise floor to nearest height
  // and change texture.
  PEV.DoPlat(line,plattype_e.raiseToNearestAndChange,0);
  break;
  
    case 96:
  // Raise floor to shortest texture height
  // on either side of lines.
  DoFloor(line,floor_e.raiseToTexture);
  break;
  
    case 97:
  // TELEPORT!
  Teleport( line, side, thing );
  break;
  
    case 98:
  // Lower Floor (TURBO)
  DoFloor(line,floor_e.turboLower);
  break;

    case 105:
  // Blazing Door Raise (faster than TURBO!)
  DoDoor (line,vldoor_e.blazeRaise);
  break;
  
    case 106:
  // Blazing Door Open (faster than TURBO!)
  DoDoor (line,vldoor_e.blazeOpen);
  break;

    case 107:
  // Blazing Door Close (faster than TURBO!)
  DoDoor (line,vldoor_e.blazeClose);
  break;

    case 120:
  // Blazing PlatDownWaitUpStay.
  PEV.DoPlat(line,plattype_e.blazeDWUS,0);
  break;
  
    case 126:
  // TELEPORT MonsterONLY.
  if (thing.player==null)
      Teleport( line, side, thing );
  break;
  
    case 128:
  // Raise To Nearest Floor
  DoFloor(line,floor_e.raiseFloorToNearest);
  break;
  
    case 129:
  // Raise Floor Turbo
  DoFloor(line,floor_e.raiseFloorTurbo);
  break;
  }
}

///////////////// MOVEMENT'S ACTIONS ////////////////////////
    
    /** fixed_t */
    int[]       tmbbox=new int[4];
    mobj_t      tmthing;
    int     tmflags;
    /** fixed_t */
    int     tmx,    tmy;

    /** If "floatok" true, move would be ok
        if within "tmfloorz - tmceilingz". */
    public boolean     floatok;

    /** fixed_t */
    public int     tmfloorz, tmceilingz,   tmdropoffz;

    // keep track of the line that lowers the ceiling,
    // so missiles don't explode against sky hack walls
    public line_t      ceilingline;



    public line_t[]        spechit=new line_t[MAXSPECIALCROSS];
    public int     numspechit;
    
    /** Dispatch "PTR" Traverse function pointers */

    public boolean dispatch(PTR what, intercept_t arg){
        switch(what){
        case AimTraverse:
            return AimTraverse(arg);
        case ShootTraverse:
            return ShootTraverse(arg);
        case SlideTraverse:
            return SlideTraverse(arg);
        case UseTraverse:
            return UseTraverse(arg);                
        }
        // Shouldn't happen
        return false;
    }
    
    /** Dispatch "PIT" Iterators function pointers 
     *  Uglier than using callbacks, but since it's performance
     *  critical...
     *    
     */
    
    public boolean dispatch(PIT what, Object arg){
        switch(what){
        case VileCheck:
            return A.VileCheck((mobj_t)arg);
        case CheckThing:
            return CheckThing((mobj_t)arg);                
        case StompThing:
            return StompThing((mobj_t)arg);                
        case CheckLine:
            return CheckLine((line_t)arg);
        }
     // Shouldn't happen
        return false;
    }
    
    //
    // PIT_ChangeSector
    //
    public boolean ChangeSector (mobj_t   thing)
    {
        mobj_t mo;
        
        if (ThingHeightClip (thing))
        {
        // keep checking
        return true;
        }
        

        // crunch bodies to giblets
        if (thing.health <= 0)
        {
        thing.SetMobjState(statenum_t.S_GIBS);

        thing.flags &= ~MF_SOLID;
        thing.height = 0;
        thing.radius = 0;

        // keep checking
        return true;        
        }

        // crunch dropped items
        if (flags(thing.flags, MF_DROPPED))
        {
        RemoveMobj (thing);
        
        // keep checking
        return true;        
        }

        if (! flags(thing.flags , MF_SHOOTABLE) )
        {
        // assume it is bloody gibs or something
        return true;            
        }
        
        nofit = true;

        if (crushchange && !flags(DM.leveltime,3) )
        {
        DamageMobj( thing,null,null,10);

        // spray blood in a random direction
        mo = SpawnMobj (thing.x,
                  thing.y,
                  thing.z + thing.height/2, mobjtype_t.MT_BLOOD);
        
        mo.momx = (RND.P_Random() - RND.P_Random ())<<12;
        mo.momy = (RND.P_Random() - RND.P_Random ())<<12;
        }

        // keep checking (crush other things)   
        return true;    
    }
    
            
    /** PIT_CheckLine
     * Adjusts tmfloorz and tmceilingz as lines are contacted
     */

            public boolean CheckLine(line_t ld){
        if (tmbbox[BOXRIGHT] <= ld.bbox[BOXLEFT]
        || tmbbox[BOXLEFT] >= ld.bbox[BOXRIGHT]
        || tmbbox[BOXTOP] <= ld.bbox[BOXBOTTOM]
        || tmbbox[BOXBOTTOM] >= ld.bbox[BOXTOP] )
        return true;

        if (ld.BoxOnLineSide (tmbbox) != -1)
        return true;
            
        // A line has been hit
        
        // The moving thing's destination position will cross
        // the given line.
        // If this should not be allowed, return false.
        // If the line is special, keep track of it
        // to process later if the move is proven ok.
        // NOTE: specials are NOT sorted by order,
        // so two special lines that are only 8 pixels apart
        // could be crossed in either order.
        
        if (ld.backsector==null)
        return false;       // one sided line
            
        if (!flags(tmthing.flags, MF_MISSILE) )
        {
        if ( flags(ld.flags, ML_BLOCKING) )
            return false;   // explicitly blocking everything

        if ( (tmthing.player==null) && flags(ld.flags, ML_BLOCKMONSTERS ))
            return false;   // block monsters only
        }

        // set openrange, opentop, openbottom
        LineOpening (ld); 
        
        // adjust floor / ceiling heights
        if (opentop < tmceilingz)
        {
        tmceilingz = opentop;
        ceilingline = ld;
        }

        if (openbottom > tmfloorz)
        tmfloorz = openbottom;  

        if (lowfloor < tmdropoffz)
        tmdropoffz = lowfloor;
            
        // if contacted a special line, add it to the list
        if (ld.special!=0)
        {
        spechit[numspechit] = ld;
        numspechit++;
        }

        return true;
        }
            
    /**PIT_CheckThing  */

        public boolean CheckThing(mobj_t thing) {
        int     blockdist; // fixed_t
        boolean     solid;
        int         damage;
            
        if ((thing.flags & (MF_SOLID|MF_SPECIAL|MF_SHOOTABLE) )==0)
        return true;
        
        blockdist = thing.radius + tmthing.radius;

        if ( Math.abs(thing.x - tmx) >= blockdist
         || Math.abs(thing.y - tmy) >= blockdist )
        {
        // didn't hit it
        return true;    
        }
        
        // don't clip against self
        if (thing == tmthing)
        return true;
        
        // check for skulls slamming into things
        if ((tmthing.flags & MF_SKULLFLY)!=0)
        {
        damage = ((RND.P_Random()%8)+1)*tmthing.info.damage;
        
        DamageMobj (thing, tmthing, tmthing, damage);
        
        tmthing.flags &= ~MF_SKULLFLY;
        tmthing.momx = tmthing.momy = tmthing.momz = 0;
        
        SetMobjState (tmthing, tmthing.info.spawnstate);
        
        return false;       // stop moving
        }

        
        // missiles can hit other things
        if (flags(tmthing.flags , MF_MISSILE))
        {
        // see if it went over / under
        if (tmthing.z > thing.z + thing.height)
            return true;        // overhead
        if (tmthing.z+tmthing.height < thing.z)
            return true;        // underneath
            
        if (tmthing.target!=null && (
            tmthing.target.type == thing.type || 
            (tmthing.target.type == mobjtype_t.MT_KNIGHT && thing.type == mobjtype_t.MT_BRUISER)||
            (tmthing.target.type == mobjtype_t.MT_BRUISER && thing.type == mobjtype_t.MT_KNIGHT) ) )
        {
            // Don't hit same species as originator.
            if (thing == tmthing.target)
            return true;

            if (thing.type != mobjtype_t.MT_PLAYER)
            {
            // Explode, but do no damage.
            // Let players missile other players.
            return false;
            }
        }
        
        if (! flags(thing.flags ,MF_SHOOTABLE) )
        {
            // didn't do any damage
            return !flags(thing.flags , MF_SOLID);   
        }
        
        // damage / explode
        damage = ((RND.P_Random()%8)+1)*tmthing.info.damage;
        DamageMobj (thing, tmthing, tmthing.target, damage);

        // don't traverse any more
        return false;               
        }
        
        // check for special pickup
        if (flags(thing.flags , MF_SPECIAL))
        {
        solid = flags(thing.flags,MF_SOLID);
        if (flags(tmflags,MF_PICKUP))
        {
            // can remove thing
            TouchSpecialThing (thing, tmthing);
        }
        return !solid;
        }
        
        return !flags(thing.flags ,MF_SOLID);
    }


    /**
     *PIT_StompThing
     */

    public boolean StompThing (mobj_t thing)
    {
     int blockdist; // fixed_t
         
     if ((thing.flags & MF_SHOOTABLE)==0 )
     return true;
         
     blockdist = thing.radius + tmthing.radius;
     
     if ( Math.abs(thing.x - tmx) >= blockdist
      || Math.abs(thing.y - tmy) >= blockdist )
     {
     // didn't hit it
     return true;
     }
     
     // don't clip against self
     if (thing == tmthing)
     return true;
     
     // monsters don't stomp things except on boss level
     if ( (tmthing.player==null) && (DM.gamemap != 30))
     return false;   
         
     DamageMobj (thing, tmthing, tmthing, 10000); // in interaction
     
     return true;
    }
    
    //
    // PIT_RadiusAttack
    // "bombsource" is the creature
    // that caused the explosion at "bombspot".
    //
    public boolean RadiusAttack (mobj_t thing)
    {
        int dx,dy,dist; // fixed_t
        
        if (!flags(thing.flags , MF_SHOOTABLE) )
        return true;

        // Boss spider and cyborg
        // take no damage from concussion.
        if (thing.type == mobjtype_t.MT_CYBORG
        || thing.type == mobjtype_t.MT_SPIDER)
        return true;    
            
        dx = Math.abs(thing.x - bombspot.x);
        dy = Math.abs(thing.y - bombspot.y);
        
        dist = dx>dy ? dx : dy;
        dist = (dist - thing.radius) >> FRACBITS;

        if (dist < 0)
        dist = 0;

        if (dist >= bombdamage)
        return true;    // out of range

        if ( EN.CheckSight (thing, bombspot) )
        {
        // must be in direct path
        DamageMobj (thing, bombspot, bombsource, bombdamage - dist);
        }
        
        return true;
    }
    
    //
    // PTR_AimTraverse
    // Sets linetaget and aimslope when a target is aimed at.
    //
    boolean
    AimTraverse (intercept_t in)
    {
        line_t    li;
        mobj_t     th;
        int     slope;
        int     thingtopslope;
        int     thingbottomslope;
        int     dist;
            
        if (in.isaline)
        {
        li = (line_t) in.d();
        
        if ( !flags(li.flags , ML_TWOSIDED) )
            return false;       // stop
        
        // Crosses a two sided line.
        // A two sided line will restrict
        // the possible target ranges.
        LineOpening (li);
        
        if (openbottom >= opentop)
            return false;       // stop
        
        dist = FixedMul (attackrange, in.frac);

        if (li.frontsector.floorheight != li.backsector.floorheight)
        {
            slope = FixedDiv (openbottom - shootz , dist);
            if (slope > bottomslope)
            bottomslope = slope;
        }
            
        if (li.frontsector.ceilingheight != li.backsector.ceilingheight)
        {
            slope = FixedDiv (opentop - shootz , dist);
            if (slope < topslope)
            topslope = slope;
        }
            
        if (topslope <= bottomslope)
            return false;       // stop
                
        return true;            // shot continues
        }
        
        // shoot a thing
        th = (mobj_t) in.d();
        if (th == shootthing)
        return true;            // can't shoot self
        
        if (!flags(th.flags,MF_SHOOTABLE))
        return true;            // corpse or something

        // check angles to see if the thing can be aimed at
        dist = FixedMul (attackrange, in.frac);
        thingtopslope = FixedDiv (th.z+th.height - shootz , dist);

        if (thingtopslope < bottomslope)
        return true;            // shot over the thing

        thingbottomslope = FixedDiv (th.z - shootz, dist);

        if (thingbottomslope > topslope)
        return true;            // shot under the thing
        
        // this thing can be hit!
        if (thingtopslope > topslope)
        thingtopslope = topslope;
        
        if (thingbottomslope < bottomslope)
        thingbottomslope = bottomslope;

        aimslope = (thingtopslope+thingbottomslope)/2;
        linetarget = th;

        return false;           // don't go any farther
    }

    //
    //P_BlockThingsIterator
    //
    boolean
    BlockThingsIterator
    ( int           x,
    int           y,
    PIT func)
    {
     mobj_t     mobj;
     
     if ( x<0
      || y<0
      || x>=LL.bmapwidth
      || y>=LL.bmapheight)
     {
     return true;
     }
     

     for (mobj = LL.blocklinks[y*LL.bmapwidth+x] ; mobj!=null ;
      mobj = (mobj_t) mobj.bnext)
     {
     if (!dispatch(func, mobj ) )
         return false;
     }
     return true;
    }

    //
    // PTR_ShootTraverse
    //
    boolean ShootTraverse (intercept_t in)
    {
        int     x,y,z,frac; // fixed_t
        line_t    li;
        mobj_t     th;
        boolean hitline=false;
        
        int     slope,dist,thingtopslope,thingbottomslope; // fixed_t
            
        if (in.isaline)
        {
        li = (line_t) in.d();
        
        if (li.special!=0)
            ShootSpecialLine (shootthing, li);

        if ( !flags(li.flags, ML_TWOSIDED) ) {
            hitline=true;
            }
        
        // crosses a two sided line
        LineOpening (li);
            
        dist = FixedMul (attackrange, in.frac);

        if (li.frontsector.floorheight != li.backsector.floorheight)
        {
            slope = FixedDiv (openbottom - shootz , dist);
            if (slope > aimslope){      
                hitline=true;
            }
        }
            
        if (li.frontsector.ceilingheight != li.backsector.ceilingheight)
        {
            slope = FixedDiv (opentop - shootz , dist);
            if (slope < aimslope){      
                hitline=true;
            }
        }

        // shot continues
        if (!hitline)
        return true;            
        
        // hit line

        // position a bit closer
        frac = in.frac - FixedDiv (4*FRACUNIT,attackrange);
        x = trace.x + FixedMul (trace.dx, frac);
        y = trace.y + FixedMul (trace.dy, frac);
        z = shootz + FixedMul (aimslope, FixedMul(frac, attackrange));

        if (li.frontsector.ceilingpic == R.skyflatnum)
        {
            // don't shoot the sky!
            if (z > li.frontsector.ceilingheight)
            return false;
            
            // it's a sky hack wall
            if  (li.backsector!=null && li.backsector.ceilingpic == R.skyflatnum)
            return false;       
        }

        // Spawn bullet puffs.
        SpawnPuff (x,y,z);
        
        // don't go any farther
        return false;   
        }
        
        // shoot a thing
        th = (mobj_t) in.d();
        if (th == shootthing)
        return true;        // can't shoot self
        
        if (!flags(th.flags,MF_SHOOTABLE))
        return true;        // corpse or something
            
        // check angles to see if the thing can be aimed at
        dist = FixedMul (attackrange, in.frac);
        thingtopslope = FixedDiv (th.z+th.height - shootz , dist);

        if (thingtopslope < aimslope)
        return true;        // shot over the thing

        thingbottomslope = FixedDiv (th.z - shootz, dist);

        if (thingbottomslope > aimslope)
        return true;        // shot under the thing

        
        // hit thing
        // position a bit closer
        frac = in.frac - FixedDiv (10*FRACUNIT,attackrange);

        x = trace.x + FixedMul (trace.dx, frac);
        y = trace.y + FixedMul (trace.dy, frac);
        z = shootz + FixedMul (aimslope, FixedMul(frac, attackrange));

        // Spawn bullet puffs or blod spots,
        // depending on target type.
        if (flags(((mobj_t)in.d()).flags , MF_NOBLOOD))
        SpawnPuff (x,y,z);
        else
        SpawnBlood (x,y,z, la_damage);

        if (la_damage!=0)
        DamageMobj (th, shootthing, shootthing, la_damage);

        // don't go any farther
        return false;
        
    }

    //
    // TELEPORT MOVE
    // 

    //
    // P_TeleportMove
    //
    public boolean
    TeleportMove
    ( mobj_t    thing,
      int   x, //fixed
      int   y )
    {
        int         xl;
        int         xh;
        int         yl;
        int         yh;
        int         bx;
        int         by;
        
        subsector_t newsubsec;
        
        // kill anything occupying the position
        tmthing = thing;
        tmflags = thing.flags;
        
        tmx = x;
        tmy = y;
        
        tmbbox[BOXTOP] = y + tmthing.radius;
        tmbbox[BOXBOTTOM] = y - tmthing.radius;
        tmbbox[BOXRIGHT] = x + tmthing.radius;
        tmbbox[BOXLEFT] = x - tmthing.radius;

        newsubsec = R.PointInSubsector (x,y);
        ceilingline = null;
        
        // The base floor/ceiling is from the subsector
        // that contains the point.
        // Any contacted lines the step closer together
        // will adjust them.
        tmfloorz = tmdropoffz = newsubsec.sector.floorheight;
        tmceilingz = newsubsec.sector.ceilingheight;
                
        R.validcount++; // This is r_main's ?
        numspechit = 0;
        
        // stomp on any things contacted
        xl = (tmbbox[BOXLEFT] - LL.bmaporgx - MAXRADIUS)>>MAPBLOCKSHIFT;
        xh = (tmbbox[BOXRIGHT] - LL.bmaporgx + MAXRADIUS)>>MAPBLOCKSHIFT;
        yl = (tmbbox[BOXBOTTOM] - LL.bmaporgy - MAXRADIUS)>>MAPBLOCKSHIFT;
        yh = (tmbbox[BOXTOP] - LL.bmaporgy + MAXRADIUS)>>MAPBLOCKSHIFT;

        for (bx=xl ; bx<=xh ; bx++)
        for (by=yl ; by<=yh ; by++)
            if (!BlockThingsIterator(bx,by,PIT.StompThing))
            return false;
        
        // the move is ok,
        // so link the thing into its new position
        UnsetThingPosition (thing);

        thing.floorz = tmfloorz;
        thing.ceilingz = tmceilingz;    
        thing.x = x;
        thing.y = y;

        SetThingPosition (thing);
        
        return true;
    }

    //
    // MOVEMENT CLIPPING
    //

    /**
     * P_CheckPosition
     *  This is purely informative, nothing is modified
     *  (except things picked up).
     *  
     *  in:
     *   a mobj_t (can be valid or invalid)
     *   a position to be checked
     *    (doesn't need to be related to the mobj_t.x,y)
     * 
     *  during:
     *   special things are touched if MF_PICKUP
     *   early out on solid lines?
     * 
     *  out:
     *   newsubsec
     *   floorz
     *   ceilingz
     *   tmdropoffz
     *    the lowest point contacted
     *    (monsters won't move to a dropoff)
     *   speciallines[]
     *   numspeciallines
     * @param thing
     * @param x fixed_t
     * @param y fixed_t
    */
    public boolean
    CheckPosition
    ( mobj_t    thing,
      int   x,
      int   y )
    {
        int         xl;
        int         xh;
        int         yl;
        int         yh;
        int         bx;
        int         by;
        subsector_t newsubsec;

        tmthing = thing;
        tmflags = thing.flags;
        
        tmx = x;
        tmy = y;
        
        tmbbox[BOXTOP] = y + tmthing.radius;
        tmbbox[BOXBOTTOM] = y - tmthing.radius;
        tmbbox[BOXRIGHT] = x + tmthing.radius;
        tmbbox[BOXLEFT] = x - tmthing.radius;

        newsubsec = R.PointInSubsector (x,y);
        ceilingline = null;
        
        // The base floor / ceiling is from the subsector
        // that contains the point.
        // Any contacted lines the step closer together
        // will adjust them.
        tmfloorz = tmdropoffz = newsubsec.sector.floorheight;
        tmceilingz = newsubsec.sector.ceilingheight;
                
        R.validcount++;
        numspechit = 0;

        if ( flags(tmflags ,MF_NOCLIP ))
        return true;
        
        // Check things first, possibly picking things up.
        // The bounding box is extended by MAXRADIUS
        // because mobj_ts are grouped into mapblocks
        // based on their origin point, and can overlap
        // into adjacent blocks by up to MAXRADIUS units.
        xl = (tmbbox[BOXLEFT] - LL.bmaporgx - MAXRADIUS)>>>MAPBLOCKSHIFT;
        xh = (tmbbox[BOXRIGHT] - LL.bmaporgx + MAXRADIUS)>>>MAPBLOCKSHIFT;
        yl = (tmbbox[BOXBOTTOM] - LL.bmaporgy - MAXRADIUS)>>>MAPBLOCKSHIFT;
        yh = (tmbbox[BOXTOP] - LL.bmaporgy + MAXRADIUS)>>>MAPBLOCKSHIFT;

        for (bx=xl ; bx<=xh ; bx++)
        for (by=yl ; by<=yh ; by++)
            if (!BlockThingsIterator(bx,by,PIT.CheckThing))
            return false;
        
        // check lines
        xl = (tmbbox[BOXLEFT] - LL.bmaporgx)>>>MAPBLOCKSHIFT;
        xh = (tmbbox[BOXRIGHT] - LL.bmaporgx)>>>MAPBLOCKSHIFT;
        yl = (tmbbox[BOXBOTTOM] - LL.bmaporgy)>>>MAPBLOCKSHIFT;
        yh = (tmbbox[BOXTOP] - LL.bmaporgy)>>>MAPBLOCKSHIFT;

        for (bx=xl ; bx<=xh ; bx++)
        for (by=yl ; by<=yh ; by++)
            if (!BlockLinesIterator (bx,by,PIT.CheckLine))
            return false;

        return true;
    }


    /**
    // P_TryMove
    // Attempt to move to a new position,
    // crossing special lines unless MF_TELEPORT is set.
     * @param x fixed_t
     * @param y fixed_t
     * 
    */
    private boolean
    TryMove
    ( mobj_t    thing,
      int   x,
      int   y )
    {
        int oldx, oldy; // fixed_t    
        boolean     side, oldside; // both were int
        line_t  ld;

        floatok = false;
        if (!CheckPosition (thing, x, y))
        return false;       // solid wall or thing
        
        if ( !flags(thing.flags, MF_NOCLIP) )
        {
        if (tmceilingz - tmfloorz < thing.height)
            return false;   // doesn't fit

        floatok = true;
        
        if ( !flags(thing.flags,MF_TELEPORT) 
             &&tmceilingz - thing.z < thing.height)
            return false;   // mobj must lower itself to fit

        if ( !flags(thing.flags,MF_TELEPORT)
             && tmfloorz - thing.z > 24*FRACUNIT )
            return false;   // too big a step up

        if ( !flags(thing.flags,(MF_DROPOFF|MF_FLOAT))
             && tmfloorz - tmdropoffz > 24*FRACUNIT )
            return false;   // don't stand over a dropoff
        }
        
        // the move is ok,
        // so link the thing into its new position
        UnsetThingPosition (thing);

        oldx = thing.x;
        oldy = thing.y;
        thing.floorz = tmfloorz;
        thing.ceilingz = tmceilingz;    
        thing.x = x;
        thing.y = y;

        SetThingPosition (thing);
        
        // if any special lines were hit, do the effect
        if (! flags(thing.flags,(MF_TELEPORT|MF_NOCLIP)) )
        {
        while (numspechit-->0)
        {
            // see if the line was crossed
            ld = spechit[numspechit];
            side = ld.PointOnLineSide (thing.x, thing.y );
            oldside = ld.PointOnLineSide (oldx, oldy );
            if (side != oldside)
            {
            if (ld.special!=0)
                CrossSpecialLine (ld, oldside?1:0, thing);
            }
        }
        }

        return true;
    }


    //
    // P_ThingHeightClip
    // Takes a valid thing and adjusts the thing.floorz,
    // thing.ceilingz, and possibly thing.z.
    // This is called for all nearby monsters
    // whenever a sector changes height.
    // If the thing doesn't fit,
    // the z will be set to the lowest value
    // and false will be returned.
    //
    boolean ThingHeightClip (mobj_t thing)
    {
        boolean     onfloor;
        
        onfloor = (thing.z == thing.floorz);
        
        CheckPosition (thing, thing.x, thing.y);  
        // what about stranding a monster partially off an edge?
        
        thing.floorz = tmfloorz;
        thing.ceilingz = tmceilingz;
        
        if (onfloor)
        {
        // walking monsters rise and fall with the floor
        thing.z = thing.floorz;
        }
        else
        {
        // don't adjust a floating monster unless forced to
        if (thing.z+thing.height > thing.ceilingz)
            thing.z = thing.ceilingz - thing.height;
        }
        
        if (thing.ceilingz - thing.floorz < thing.height)
        return false;
            
        return true;
    }



    //
    // SLIDE MOVE
    // Allows the player to slide along any angled walls.
    //
    int     bestslidefrac; // fixed
    int     secondslidefrac;

    line_t     bestslideline;
    line_t     secondslideline;

    mobj_t     slidemo;

    int     tmxmove; //fixed_t
    int     tmymove;



    //
    // P_HitSlideLine
    // Adjusts the xmove / ymove
    // so that the next move will slide along the wall.
    //
    private void HitSlideLine (line_t ld)
    {
        boolean         side;

        // all angles
        long     lineangle, moveangle,deltaangle;
        
        // fixed_t
        int     movelen,  newlen;
        
        
        if (ld.slopetype == slopetype_t.ST_HORIZONTAL)
        {
        tmymove = 0;
        return;
        }
        
        if (ld.slopetype == slopetype_t.ST_VERTICAL)
        {
        tmxmove = 0;
        return;
        }
        
        side = ld.PointOnLineSide (slidemo.x, slidemo.y);
        
        lineangle = R.PointToAngle2 (0,0, ld.dx, ld.dy);

        if (side == true)
        lineangle += ANG180;

        moveangle = R.PointToAngle2 (0,0, tmxmove, tmymove);
        deltaangle = moveangle-lineangle;

        if (deltaangle > ANG180)
        deltaangle += ANG180;
        //  system.Error ("SlideLine: ang>ANG180");

        //lineangle >>>= ANGLETOFINESHIFT;
        //deltaangle >>>= ANGLETOFINESHIFT;
        
        movelen = AproxDistance (tmxmove, tmymove);
        newlen = FixedMul (movelen, finecosine(deltaangle));

        tmxmove = FixedMul (newlen, finecosine(lineangle)); 
        tmymove = FixedMul (newlen, finesine(lineangle));   
    }


    //
    // PTR_SlideTraverse
    //
    boolean SlideTraverse (intercept_t in)
    {
        line_t li;
        
        if (!in.isaline)
        I.Error ("PTR_SlideTraverse: not a line?");
            
        li = (line_t) in.d();
        
        if ( ! flags(li.flags ,ML_TWOSIDED) )
        {
        if (li.PointOnLineSide (slidemo.x, slidemo.y))
        {
            // don't hit the back side
            return true;        
        }
        return isblocking(in,li);
        }

        // set openrange, opentop, openbottom
        LineOpening (li);
        
        if ((openrange < slidemo.height)|| // doesn't fit
            (opentop - slidemo.z < slidemo.height)|| // mobj is too high
            (openbottom - slidemo.z > 24*FRACUNIT )) // too big a step up
        {
        if (in.frac < bestslidefrac)
        {
        secondslidefrac = bestslidefrac;
        secondslideline = bestslideline;
        bestslidefrac = in.frac;
        bestslideline = li;
        }
        
        return false;   // stop
    }
        
        else
        // this line doesn't block movement
        return true;        
        

    }

    private final boolean isblocking(intercept_t in, line_t li){
    // the line does block movement,
    // see if it is closer than best so far
 
    if (in.frac < bestslidefrac)
    {
    secondslidefrac = bestslidefrac;
    secondslideline = bestslideline;
    bestslidefrac = in.frac;
    bestslideline = li;
    }
    
    return false;   // stop
    }
    
    //
    // P_SlideMove
    // The momx / momy move is bad, so try to slide
    // along a wall.
    // Find the first line hit, move flush to it,
    // and slide along it
    //
    // This is a kludgy mess.
    //
    void SlideMove (mobj_t mo)
    {
        // fixed_t
        int     leadx,leady,trailx,traily,newx,newy;
        int         hitcount;
            
        slidemo = mo;
        hitcount = 0;
        boolean retry=true;
        
      while(retry){
        if (++hitcount == 3) {
            // goto stairstep
            if (!TryMove (mo, mo.x, mo.y + mo.momy))
                TryMove (mo, mo.x + mo.momx, mo.y);
            return;
        }     // don't loop forever

        
        // trace along the three leading corners
        if (mo.momx > 0)
        {
        leadx = mo.x + mo.radius;
        trailx = mo.x - mo.radius;
        }
        else
        {
        leadx = mo.x - mo.radius;
        trailx = mo.x + mo.radius;
        }
        
        if (mo.momy > 0)
        {
        leady = mo.y + mo.radius;
        traily = mo.y - mo.radius;
        }
        else
        {
        leady = mo.y - mo.radius;
        traily = mo.y + mo.radius;
        }
            
        bestslidefrac = FRACUNIT+1;
        
        PathTraverse ( leadx, leady, leadx+mo.momx, leady+mo.momy,
                 PT_ADDLINES, PTR.SlideTraverse );
        PathTraverse ( trailx, leady, trailx+mo.momx, leady+mo.momy,
                 PT_ADDLINES, PTR.SlideTraverse );
        PathTraverse ( leadx, traily, leadx+mo.momx, traily+mo.momy,
                 PT_ADDLINES, PTR.SlideTraverse );
        
        // move up to the wall
        if (bestslidefrac == FRACUNIT+1)
        {
        // the move most have hit the middle, so stairstep
              if (!TryMove (mo, mo.x, mo.y + mo.momy))
                  TryMove (mo, mo.x + mo.momx, mo.y);
              return;
          }     // don't loop forever

        // fudge a bit to make sure it doesn't hit
        bestslidefrac -= 0x800; 
        if (bestslidefrac > 0)
        {
        newx = FixedMul (mo.momx, bestslidefrac);
        newy = FixedMul (mo.momy, bestslidefrac);
        
        if (!TryMove (mo, mo.x+newx, mo.y+newy))
        {
            // goto stairstep
            if (!TryMove (mo, mo.x, mo.y + mo.momy))
                TryMove (mo, mo.x + mo.momx, mo.y);
            return;
        }     // don't loop forever
        }
        
        // Now continue along the wall.
        // First calculate remainder.
        bestslidefrac = FRACUNIT-(bestslidefrac+0x800);
        
        if (bestslidefrac > FRACUNIT)
        bestslidefrac = FRACUNIT;
        
        if (bestslidefrac <= 0)
        return;
        
        tmxmove = FixedMul (mo.momx, bestslidefrac);
        tmymove = FixedMul (mo.momy, bestslidefrac);

        HitSlideLine (bestslideline); // clip the moves

        mo.momx = tmxmove;
        mo.momy = tmymove;
            
        retry=!TryMove (mo, mo.x+tmxmove, mo.y+tmymove);
        }
    }

    //
    // P_XYMovement  
    //
    protected final static int STOPSPEED =      0x1000;
    protected final static int FRICTION =       0xe800;

    public void XYMovement (mobj_t mo) 
    {
    //System.out.println("XYMovement");
    int     ptryx, ptryy; // pointers to fixed_t ???
    player_t   player;
    int  xmove, ymove; // fixed_t
          
    if ((mo.momx==0) && (mo.momy==0))
    {
    if ((mo.flags & MF_SKULLFLY)!=0)
    {
      // the skull slammed into something
      mo.flags &= ~MF_SKULLFLY;
      mo.momx = mo.momy = mo.momz = 0;

      SetMobjState (mo, mo.info.spawnstate);
    }
    return;
    }

    player = mo.player;
      
    if (mo.momx > MAXMOVE)
    mo.momx = MAXMOVE;
    else if (mo.momx < -MAXMOVE)
    mo.momx = -MAXMOVE;

    if (mo.momy > MAXMOVE)
    mo.momy = MAXMOVE;
    else if (mo.momy < -MAXMOVE)
    mo.momy = -MAXMOVE;
      
    xmove = mo.momx;
    ymove = mo.momy;

    do
    {
    if (xmove > MAXMOVE/2 || ymove > MAXMOVE/2)
    {
      ptryx = mo.x + xmove/2;
      ptryy = mo.y + ymove/2;
      xmove >>= 1;
      ymove >>= 1;
    }
    else
    {
      ptryx = mo.x + xmove;
      ptryy = mo.y + ymove;
      xmove = ymove = 0;
    }
      
    if (!TryMove (mo, ptryx, ptryy))
    {
      // blocked move
      if (mo.player!=null)
      {   // try to slide along it
      SlideMove (mo);
      }
      else if (flags(mo.flags , MF_MISSILE))
      {
      // explode a missile
      if (ceilingline!=null &&
          ceilingline.backsector!=null &&
          ceilingline.backsector.ceilingpic == R.skyflatnum)
      {
          // Hack to prevent missiles exploding
          // against the sky.
          // Does not handle sky floors.
          RemoveMobj (mo);
          return;
      }
      ExplodeMissile (mo);
      }
      else
      mo.momx = mo.momy = 0;
    }
    } while ((xmove | ymove)!=0);

    // slow down
    if (player!=null && flags(player.cheats , player_t.CF_NOMOMENTUM))
    {
    // debug option for no sliding at all
    mo.momx = mo.momy = 0;
    return;
    }

    if (flags(mo.flags , (MF_MISSILE | MF_SKULLFLY)) )
    return;     // no friction for missiles ever
      
    if (mo.z > mo.floorz)
    return;     // no friction when airborne

    if (flags(mo.flags , MF_CORPSE))
    {
    // do not stop sliding
    //  if halfway off a step with some momentum
    if (mo.momx > FRACUNIT/4
      || mo.momx < -FRACUNIT/4
      || mo.momy > FRACUNIT/4
      || mo.momy < -FRACUNIT/4)
    {
      if (mo.floorz != mo.subsector.sector.floorheight)
      return;
    }
    }

    if (mo.momx > -STOPSPEED
    && mo.momx < STOPSPEED
    && mo.momy > -STOPSPEED
    && mo.momy < STOPSPEED
    && (player==null
      || (player.cmd.forwardmove== 0
      && player.cmd.sidemove == 0 ) ) )
    {
    // if in a walking frame, stop moving
    // TODO: we need a way to get state indexed inside of states[], to sim pointer arithmetic.
   // FIX: added an "id" field.
    if ( player!=null&&(int)(player.mo.state.id- statenum_t.S_PLAY_RUN1.ordinal()) < 4)
        player.mo.SetMobjState (statenum_t.S_PLAY);

    mo.momx = 0;
    mo.momy = 0;
    }
    else
    {
    mo.momx = FixedMul (mo.momx, FRICTION);
    mo.momy = FixedMul (mo.momy, FRICTION);
    }
    }
    //
    // P_LineAttack
    //
    /** who got hit (or NULL) */
    public mobj_t     linetarget;
    mobj_t     shootthing;

    // Height if not aiming up or down
    // ???: use slope for monsters?
    int     shootz; // fixed_t

    int     la_damage;
    int     attackrange; // fixed_t

    int     aimslope; // fixed_t

    


    //
    // 
    //
    /** P_AimLineAttack
     * @param t1
     * @param angle long
     * @param distance int
     */
   private int
   AimLineAttack
    ( mobj_t   t1,
      long   angle,
      int   distance )
    {
        int x2,y2;
        
        angle >>= ANGLETOFINESHIFT;
        shootthing = t1;
        
        x2 = t1.x + (distance>>FRACBITS)*finecosine[(int) angle];
        y2 = t1.y + (distance>>FRACBITS)*finesine[(int) angle];
        shootz = t1.z + (t1.height>>1) + 8*FRACUNIT;

        // can't shoot outside view angles
        topslope = 100*FRACUNIT/160;    
        bottomslope = -100*FRACUNIT/160;
        
        attackrange = distance;
        linetarget = null;
        
        PathTraverse ( t1.x, t1.y,
                 x2, y2,
                 PT_ADDLINES|PT_ADDTHINGS,
                 PTR.AimTraverse );
            
        if (linetarget!=null)
        return aimslope;

        return 0;
    }
     

    /**
     * P_LineAttack
     * If damage == 0, it is just a test trace
     * that will leave linetarget set.
     * 
     * @param t1
     * @param angle angle_t
     * @param distance fixed_t
     * @param slope fixed_t
     * @param damage
     */
   
    void
    LineAttack
    ( mobj_t   t1,
      long   angle,
      int   distance,
      int   slope,
      int       damage )
    {
        int x2,y2;
        
        angle >>>= ANGLETOFINESHIFT;
        shootthing = t1;
        la_damage = damage;
        x2 = t1.x + (distance>>FRACBITS)*finecosine[(int) angle];
        y2 = t1.y + (distance>>FRACBITS)*finesine[(int) angle];
        shootz = t1.z + (t1.height>>1) + 8*FRACUNIT;
        attackrange = distance;
        aimslope = slope;
            
        PathTraverse ( t1.x, t1.y,
                 x2, y2,
                 PT_ADDLINES|PT_ADDTHINGS,
                 PTR.ShootTraverse );
    }
     


    //
    // USE LINES
    //
    mobj_t     usething;

    boolean UseTraverse (intercept_t in)
    {
        boolean     side;
        // FIXME: some sanity check here?
        line_t line=(line_t) in.d();
        
        if (line.special==0)
        {
        LineOpening (line);
        if (openrange <= 0)
        {
        	 S.StartSound(usething, sfxenum_t.sfx_noway);
            
            // can't use through a wall
            return false;   
        }
        // not a special line, but keep checking
        return true ;       
        }
        
        side = false;
        if (line.PointOnLineSide (usething.x, usething.y))
        side = true;
        
        //  return false;       // don't use back side
        
        UseSpecialLine (usething, line, side);

        // can't use for than one special line in a row
        return false;
    }

    /**
     * P_UseSpecialLine
     * Called when a thing uses a special line.
     * Only the front sides of lines are usable.
     */
    
    public boolean
    UseSpecialLine
    ( mobj_t   thing,
      line_t   line,
      boolean      side )
    {               

        // Err...
        // Use the back sides of VERY SPECIAL lines...
        if (side)
        {
       switch(line.special)
       {
         case 124:
           // Sliding door open&close
           // UNUSED?
           break;

         default:
           return false;
           //break;
       }
        }

        
        // Switches that other things can activate.
        if (thing.player!=null)
        {
       // never open secret doors
       if (flags(line.flags, ML_SECRET))
           return false;
       
       switch(line.special)
       {
         case 1:   // MANUAL DOOR RAISE
         case 32:  // MANUAL BLUE
         case 33:  // MANUAL RED
         case 34:  // MANUAL YELLOW
           break;
           
         default:
           return false;
           //break;
       }
        }

        
        // do something  
        switch (line.special)
        {
       // MANUALS
          case 1:      // Vertical Door
          case 26:     // Blue Door/Locked
          case 27:     // Yellow Door /Locked
          case 28:     // Red Door /Locked

          case 31:     // Manual door open
          case 32:     // Blue locked door open
          case 33:     // Red locked door open
          case 34:     // Yellow locked door open

          case 117:        // Blazing door raise
          case 118:        // Blazing door open
       VerticalDoor (line, thing);
       break;
       
       //UNUSED - Door Slide Open&Close
       // case 124:
       // EV_SlidingDoor (line, thing);
       // break;

       // SWITCHES
          case 7:
       // Build Stairs
       if (BuildStairs(line,stair_e.build8))
           SW.ChangeSwitchTexture(line,0);
       break;

          case 9:
       // Change Donut
       if (DoDonut(line))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 11:
       // Exit level
              SW.ChangeSwitchTexture(line,0);
       DM.ExitLevel ();
       break;
       
          case 14:
       // Raise Floor 32 and change texture
       if (PEV.DoPlat(line,plattype_e.raiseAndChange,32))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 15:
       // Raise Floor 24 and change texture
       if (PEV.DoPlat(line,plattype_e.raiseAndChange,24))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 18:
       // Raise Floor to next highest floor
       if (DoFloor(line, floor_e.raiseFloorToNearest))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 20:
       // Raise Plat next highest floor and change texture
       if (PEV.DoPlat(line,plattype_e.raiseToNearestAndChange,0))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 21:
       // PlatDownWaitUpStay
       if (PEV.DoPlat(line,plattype_e.downWaitUpStay,0))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 23:
       // Lower Floor to Lowest
       if (DoFloor(line,floor_e.lowerFloorToLowest))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 29:
       // Raise Door
       if (DoDoor(line,vldoor_e.normal))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 41:
       // Lower Ceiling to Floor
       if (DoCeiling(line,ceiling_e.lowerToFloor))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 71:
       // Turbo Lower Floor
       if (DoFloor(line,floor_e.turboLower))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 49:
       // Ceiling Crush And Raise
       if (DoCeiling(line,ceiling_e.crushAndRaise))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 50:
       // Close Door
       if (DoDoor(line,vldoor_e.close))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 51:
       // Secret EXIT
              SW.ChangeSwitchTexture(line,0);
       DM.SecretExitLevel ();
       break;
       
          case 55:
       // Raise Floor Crush
       if (DoFloor(line,floor_e.raiseFloorCrush))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 101:
       // Raise Floor
       if (DoFloor(line,floor_e.raiseFloor))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 102:
       // Lower Floor to Surrounding floor height
       if (DoFloor(line,floor_e.lowerFloor))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 103:
       // Open Door
       if (DoDoor(line,vldoor_e.open))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 111:
       // Blazing Door Raise (faster than TURBO!)
       if (DoDoor (line,vldoor_e.blazeRaise))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 112:
       // Blazing Door Open (faster than TURBO!)
       if (DoDoor (line,vldoor_e.blazeOpen))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 113:
       // Blazing Door Close (faster than TURBO!)
       if (DoDoor (line,vldoor_e.blazeClose))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 122:
       // Blazing PlatDownWaitUpStay
       if (PEV.DoPlat(line,plattype_e.blazeDWUS,0))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 127:
       // Build Stairs Turbo 16
       if (BuildStairs(line,stair_e.turbo16))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 131:
       // Raise Floor Turbo
       if (DoFloor(line,floor_e.raiseFloorTurbo))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 133:
       // BlzOpenDoor BLUE
          case 135:
       // BlzOpenDoor RED
          case 137:
       // BlzOpenDoor YELLOW
       if (DoLockedDoor (line,vldoor_e.blazeOpen,thing))
           SW.ChangeSwitchTexture(line,0);
       break;
       
          case 140:
       // Raise Floor 512
       if (DoFloor(line,floor_e.raiseFloor512))
           SW.ChangeSwitchTexture(line,0);
       break;
       
       // BUTTONS
          case 42:
       // Close Door
       if (DoDoor(line,vldoor_e.close))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 43:
       // Lower Ceiling to Floor
       if (DoCeiling(line,ceiling_e.lowerToFloor))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 45:
       // Lower Floor to Surrounding floor height
       if (DoFloor(line,floor_e.lowerFloor))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 60:
       // Lower Floor to Lowest
       if (DoFloor(line,floor_e.lowerFloorToLowest))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 61:
       // Open Door
       if (DoDoor(line,vldoor_e.open))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 62:
       // PlatDownWaitUpStay
       if (PEV.DoPlat(line,plattype_e.downWaitUpStay,1))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 63:
       // Raise Door
       if (DoDoor(line,vldoor_e.normal))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 64:
       // Raise Floor to ceiling
       if (DoFloor(line,floor_e.raiseFloor))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 66:
       // Raise Floor 24 and change texture
       if (PEV.DoPlat(line,plattype_e.raiseAndChange,24))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 67:
       // Raise Floor 32 and change texture
       if (PEV.DoPlat(line,plattype_e.raiseAndChange,32))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 65:
       // Raise Floor Crush
       if (DoFloor(line,floor_e.raiseFloorCrush))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 68:
       // Raise Plat to next highest floor and change texture
       if (PEV.DoPlat(line,plattype_e.raiseToNearestAndChange,0))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 69:
       // Raise Floor to next highest floor
       if (DoFloor(line, floor_e.raiseFloorToNearest))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 70:
       // Turbo Lower Floor
       if (DoFloor(line,floor_e.turboLower))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 114:
       // Blazing Door Raise (faster than TURBO!)
       if (DoDoor (line,vldoor_e.blazeRaise))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 115:
       // Blazing Door Open (faster than TURBO!)
       if (DoDoor (line,vldoor_e.blazeOpen))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 116:
       // Blazing Door Close (faster than TURBO!)
       if (DoDoor (line,vldoor_e.blazeClose))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 123:
       // Blazing PlatDownWaitUpStay
       if (PEV.DoPlat(line,plattype_e.blazeDWUS,0))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 132:
       // Raise Floor Turbo
       if (DoFloor(line,floor_e.raiseFloorTurbo))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 99:
       // BlzOpenDoor BLUE
          case 134:
       // BlzOpenDoor RED
          case 136:
       // BlzOpenDoor YELLOW
       if (DoLockedDoor (line,vldoor_e.blazeOpen,thing))
           SW.ChangeSwitchTexture(line,1);
       break;
       
          case 138:
       // Light Turn On
       LEV.LightTurnOn(line,255);
       SW.ChangeSwitchTexture(line,1);
       break;
       
          case 139:
       // Light Turn Off
       LEV.LightTurnOn(line,35);
       SW.ChangeSwitchTexture(line,1);
       break;
               
        }
       
        return true;
    }
    

    /**
     * P_UseLines
     * Looks for special lines in front of the player to activate.
     */
    public void UseLines (player_t  player) 
    {
        int     angle;
        int x1,y1,x2,y2;
        
        usething = player.mo;
        
        // Normally this shouldn't cause problems?
        angle = Tables.toBAMIndex(player.mo.angle);

        x1 = player.mo.x;
        y1 = player.mo.y;
        x2 = x1 + (USERANGE>>>FRACBITS)*finecosine[angle];
        y2 = y1 + (USERANGE>>>FRACBITS)*finesine[angle];
        
        PathTraverse ( x1, y1, x2, y2, PT_ADDLINES, PTR.UseTraverse );
    }


    //
    // RADIUS ATTACK
    //
    mobj_t     bombsource;
    mobj_t     bombspot;
    int     bombdamage;


    


    /**
     * P_RadiusAttack
     * Source is the creature that caused the explosion at spot.
     */
    
    void
    RadiusAttack
    ( mobj_t   spot,
      mobj_t   source,
      int       damage )
    {
        int     x;
        int     y;
        
        int     xl;
        int     xh;
        int     yl;
        int     yh;
        
        int dist; // fixed_t
        
        dist = (damage+MAXRADIUS)<<FRACBITS;
        yh = (spot.y + dist - LL.bmaporgy)>>MAPBLOCKSHIFT;
        yl = (spot.y - dist - LL.bmaporgy)>>MAPBLOCKSHIFT;
        xh = (spot.x + dist - LL.bmaporgx)>>MAPBLOCKSHIFT;
        xl = (spot.x - dist - LL.bmaporgx)>>MAPBLOCKSHIFT;
        bombspot = spot;
        bombsource = source;
        bombdamage = damage;
        
        for (y=yl ; y<=yh ; y++)
        for (x=xl ; x<=xh ; x++)
            BlockThingsIterator (x, y, PIT.RadiusAttack );
    }



    //
    // SECTOR HEIGHT CHANGING
    // After modifying a sectors floor or ceiling height,
    // call this routine to adjust the positions
    // of all things that touch the sector.
    //
    // If anything doesn't fit anymore, true will be returned.
    // If crunch is true, they will take damage
    //  as they are being crushed.
    // If Crunch is false, you should set the sector height back
    //  the way it was and call P_ChangeSector again
    //  to undo the changes.
    //
    boolean     crushchange;
    boolean     nofit;


    



    //
    // P_ChangeSector
    //
    boolean
    ChangeSector
    ( sector_t sector,
      boolean   crunch )
    {
        int     x;
        int     y;
        
        nofit = false;
        crushchange = crunch;
        
        // re-check heights for all things near the moving sector
        for (x=sector.blockbox[BOXLEFT] ; x<= sector.blockbox[BOXRIGHT] ; x++)
        for (y=sector.blockbox[BOXBOTTOM];y<= sector.blockbox[BOXTOP] ; y++)
            BlockThingsIterator (x, y, PIT.ChangeSector);
        
        
        return nofit;
    }
    
    /**
     * P_BlockLinesIterator
     * The validcount flags are used to avoid checking lines
     * that are marked in multiple mapblocks,
     * so increment validcount before the first call
     * to P_BlockLinesIterator, then make one or more calls
     * to it.
     */

    public boolean BlockLinesIterator ( int           x,int           y,PIT func )
    {
     int         offset;
     int 		lineinblock;
     line_t     ld;
     
     if (x<0
     || y<0
     || x>=LL.bmapwidth
     || y>=LL.bmapheight)
     {
     return true;
     }
     
     // This gives us the index to look up
     
     offset = y*LL.bmapwidth+x;
     
     // The index contains yet another offset, but this time 
     // in "shorts".
     offset = LL.blockmap[offset];

     // for ( int list = blockmaplump[offset] ; *list != -1 ; list++)
     
         for (int list=offset;(lineinblock=LL.blockmaplump[list])!=-1;list++){
             ld = LL.lines[lineinblock];
     if (ld.validcount == R.validcount)
         continue;   // line has already been checked

     ld.validcount = R.validcount;
         
     if ( !dispatch(func,ld) )
         return false;
     }
     return true;    // everything was checked
    }

    //
    //P_TraverseIntercepts
    //Returns true if the traverser function returns true
    //for all lines.
    //
    boolean
    TraverseIntercepts
    ( PTR   func,
    int   maxfrac )
    {
     int         count;
     int     dist; //fixed_t
     intercept_t    in=null;  // shut up compiler warning
     
     count = intercept_p;

     while (count-->0)
     {
     dist = MAXINT;
     for (int scan = 0 ; scan<intercept_p ; scan++)
     {
         if (intercepts[scan].frac < dist)
         {
         dist = intercepts[scan].frac;
         in = intercepts[scan];
         }
     }
     
     if (dist > maxfrac)
         return true;    // checked everything in range      

    /*  // UNUSED
     {
     // don't check these yet, there may be others inserted
     in = scan = intercepts;
     for ( scan = intercepts ; scan<intercept_p ; scan++)
         if (scan.frac > maxfrac)
         *in++ = *scan;
     intercept_p = in;
     return false;
     }
    */

         if ( !dispatch(func,in) )
         return false;   // don't bother going farther

     in.frac = MAXINT;
     }
     
     return true;        // everything was traversed
    }
    
    /**
     * P_PathTraverse
     * Traces a line from x1,y1 to x2,y2,
     * calling the traverser function for each.
     * Returns true if the traverser function returns true
     * for all lines.
     */
    boolean
    PathTraverse
    ( int       x1,
            int       y1,
            int       x2,
            int       y2,
    int           flags,
    PTR trav)
    {
     int xt1,yt1;
     int xt2, yt2;
     
     int xstep,ystep;
     
     int partial;
     
     int xintercept, yintercept;
     
     int     mapx;
     int     mapy;
     
     int     mapxstep;
     int     mapystep;

     int     count;
         
     earlyout = flags(flags ,PT_EARLYOUT);
         
     R.validcount++;
     intercept_p = 0;
     
     if ( ((x1-LL.bmaporgx)&(MAPBLOCKSIZE-1)) == 0)
     x1 += FRACUNIT; // don't side exactly on a line
     
     if ( ((y1-LL.bmaporgy)&(MAPBLOCKSIZE-1)) == 0)
     y1 += FRACUNIT; // don't side exactly on a line

     trace.x = x1;
     trace.y = y1;
     trace.dx = x2 - x1;
     trace.dy = y2 - y1;

     x1 -= LL.bmaporgx;
     y1 -= LL.bmaporgy;
     xt1 = x1>>MAPBLOCKSHIFT;
     yt1 = y1>>MAPBLOCKSHIFT;

     x2 -= LL.bmaporgx;
     y2 -= LL.bmaporgy;
     xt2 = x2>>MAPBLOCKSHIFT;
     yt2 = y2>>MAPBLOCKSHIFT;

     if (xt2 > xt1)
     {
     mapxstep = 1;
     partial = FRACUNIT - ((x1>>MAPBTOFRAC)&(FRACUNIT-1));
     ystep = FixedDiv (y2-y1,Math.abs(x2-x1));
     }
     else if (xt2 < xt1)
     {
     mapxstep = -1;
     partial = (x1>>MAPBTOFRAC)&(FRACUNIT-1);
     ystep = FixedDiv (y2-y1,Math.abs(x2-x1));
     }
     else
     {
     mapxstep = 0;
     partial = FRACUNIT;
     ystep = 256*FRACUNIT;
     }   

     yintercept = (y1>>MAPBTOFRAC) + FixedMul (partial, ystep);

     
     if (yt2 > yt1)
     {
     mapystep = 1;
     partial = FRACUNIT - ((y1>>MAPBTOFRAC)&(FRACUNIT-1));
     xstep = FixedDiv (x2-x1,Math.abs(y2-y1));
     }
     else if (yt2 < yt1)
     {
     mapystep = -1;
     partial = (y1>>MAPBTOFRAC)&(FRACUNIT-1);
     xstep = FixedDiv (x2-x1,Math.abs(y2-y1));
     }
     else
     {
     mapystep = 0;
     partial = FRACUNIT;
     xstep = 256*FRACUNIT;
     }   
     xintercept = (x1>>MAPBTOFRAC) + FixedMul (partial, xstep);
     
     // Step through map blocks.
     // Count is present to prevent a round off error
     // from skipping the break.
     mapx = xt1;
     mapy = yt1;
     
     for (count = 0 ; count < 64 ; count++)
     {
     if (flags(flags ,PT_ADDLINES))
     {
         if (!BlockLinesIterator (mapx, mapy,PIT.AddLineIntercepts))
         return false;   // early out
     }
     
     if (flags(flags ,PT_ADDTHINGS))
     {
         if (!BlockThingsIterator (mapx, mapy,PIT.AddThingIntercepts))
         return false;   // early out
     }
         
     if (mapx == xt2
         && mapy == yt2)
     {
         break;
     }
     
     if ( (yintercept >> FRACBITS) == mapy)
     {
         yintercept += ystep;
         mapx += mapxstep;
     }
     else if ( (xintercept >> FRACBITS) == mapx)
     {
         xintercept += xstep;
         mapy += mapystep;
     }
         
     }
     // go through the sorted list
     return TraverseIntercepts ( trav, FRACUNIT );
    } // end method
    
    //
    // FLOORS
    //
    
    private static final int FLOORSPEED= FRACUNIT;

    /** Move a plane (floor or ceiling) and check for crushing
     *  @param sector
     *  @param speed fixed
     *  @param dest fixed
     *  @param crush
     *  @param floorOrCeiling
     *  @param direction
     */
    result_e
    MovePlane
    ( sector_t sector,
      int   speed,
      int   dest,
      boolean   crush,
      int       floorOrCeiling,
      int       direction )
    {
        boolean flag;
        int lastpos; // fixed_t
        
        switch(floorOrCeiling)
        {
          case 0:
        // FLOOR
        switch(direction)
        {
          case -1:
            // DOWN
            if (sector.floorheight - speed < dest)
            {
            lastpos = sector.floorheight;
            sector.floorheight = dest;
            flag = ChangeSector(sector,crush);
            if (flag == true)
            {
                sector.floorheight =lastpos;
                ChangeSector(sector,crush);
                //return crushed;
            }
            return result_e.pastdest;
            }
            else
            {
            lastpos = sector.floorheight;
            sector.floorheight -= speed;
            flag = ChangeSector(sector,crush);
            if (flag == true)
            {
                sector.floorheight = lastpos;
                ChangeSector(sector,crush);
                return result_e.crushed;
            }
            }
            break;
                            
          case 1:
            // UP
            if (sector.floorheight + speed > dest)
            {
            lastpos = sector.floorheight;
            sector.floorheight = dest;
            flag = ChangeSector(sector,crush);
            if (flag == true)
            {
                sector.floorheight = lastpos;
                ChangeSector(sector,crush);
                //return crushed;
            }
            return result_e.pastdest;
            }
            else
            {
            // COULD GET CRUSHED
            lastpos = sector.floorheight;
            sector.floorheight += speed;
            flag =ChangeSector(sector,crush);
            if (flag == true)
            {
                if (crush == true)
                return result_e.crushed;
                sector.floorheight = lastpos;
                ChangeSector(sector,crush);
                return result_e.crushed;
            }
            }
            break;
        }
        break;
                                        
          case 1:
        // CEILING
        switch(direction)
        {
          case -1:
            // DOWN
            if (sector.ceilingheight - speed < dest)
            {
            lastpos = sector.ceilingheight;
            sector.ceilingheight = dest;
            flag = ChangeSector(sector,crush);

            if (flag == true)
            {
                sector.ceilingheight = lastpos;
                ChangeSector(sector,crush);
                //return crushed;
            }
            return result_e.pastdest;
            }
            else
            {
            // COULD GET CRUSHED
            lastpos = sector.ceilingheight;
            sector.ceilingheight -= speed;
            flag = ChangeSector(sector,crush);

            if (flag == true)
            {
                if (crush == true)
                return result_e.crushed;
                sector.ceilingheight = lastpos;
                ChangeSector(sector,crush);
                return result_e.crushed;
            }
            }
            break;
                            
          case 1:
            // UP
            if (sector.ceilingheight + speed > dest)
            {
            lastpos = sector.ceilingheight;
            sector.ceilingheight = dest;
            flag =ChangeSector(sector,crush);
            if (flag == true)
            {
                sector.ceilingheight = lastpos;
                ChangeSector(sector,crush);
                //return crushed;
            }
            return result_e.pastdest;
            }
            else
            {
            lastpos = sector.ceilingheight;
            sector.ceilingheight += speed;
            flag = ChangeSector(sector,crush);
    // UNUSED
    /*
            if (flag == true)
            {
                sector.ceilingheight = lastpos;
                P_ChangeSector(sector,crush);
                return crushed;
            }
    */
            }
            break;
        }
        break;
            
        }
        return result_e.ok;
    }
    
    /** P_CheckMissileSpawn
     * Moves the missile forward a bit
     * and possibly explodes it right there.
     * 
     * @param th
     */

    void CheckMissileSpawn (mobj_t th)
    {
    th.tics -= RND.P_Random()&3;
    if (th.tics < 1)
    th.tics = 1;

    // move a little forward so an angle can
    // be computed if it immediately explodes
    th.x += (th.momx>>1);
    th.y += (th.momy>>1);
    th.z += (th.momz>>1);

    if (!TryMove (th, th.x, th.y))
    ExplodeMissile (th);
    }
    
    //
  //P_Ticker
  //

  public void Ticker ()
  {
     int     i;
     
     // run the tic
     if (DM.paused)
     return;
         
     // pause if in menu and at least one tic has been run
     if ( !DM.netgame
      && DM.menuactive
      && !DM.demoplayback
      && DM.players[DM.consoleplayer].viewz != 1)
     {
     return;
     }
     
         
     for (i=0 ; i<MAXPLAYERS ; i++)
     if (DM.playeringame[i])
         DM.players[i].PlayerThink ();
             
     RunThinkers ();
     SPECS.UpdateSpecials (); // In specials. Merge?
     RespawnSpecials ();

     // for par times
     DM.leveltime++;    
  }
  
  /**
   * P_ShootSpecialLine - IMPACT SPECIALS
   * Called when a thing shoots a special line.
   */
  public void
  ShootSpecialLine
  ( mobj_t   thing,
    line_t   line )
  {
      boolean     ok;
      
      //  Impacts that other things can activate.
      if (thing.player==null)
      {
      ok = false;
      switch(line.special)
      {
        case 46:
          // OPEN DOOR IMPACT
          ok = true;
          break;
      }
      if (!ok)
          return;
      }

      switch(line.special)
      {
        case 24:
      // RAISE FLOOR
      DoFloor(line,floor_e.raiseFloor);
      SW.ChangeSwitchTexture(line,0);
      break;
      
        case 46:
      // OPEN DOOR
      DoDoor(line,vldoor_e.open);
      SW.ChangeSwitchTexture(line,1);
      break;
      
        case 47:
      // RAISE FLOOR NEAR AND CHANGE
      PEV.DoPlat(line,plattype_e.raiseToNearestAndChange,0);
      SW.ChangeSwitchTexture(line,0);
      break;
      }
  }

  /** P_SpawnSpecials
   * After the map has been loaded, scan for specials
   * that spawn thinkers
  */

  void SpawnSpecials ()
  {
      sector_t   sector;
      int     i;
      //int     episode;

      //episode = 1;
      if (W.CheckNumForName("texture2") >= 0)
      //episode = 2;

      
      // See if -TIMER needs to be used.
      SPECS.levelTimer = false;
      
      i = DM.CheckParm("-avg");
      if (eval(i) && DM.deathmatch)
      {
          SPECS.levelTimer = true;
          SPECS.levelTimeCount = 20 * 60 * 35;
      }
      
      i = DM.CheckParm("-timer");
      if (eval(i) && DM.deathmatch)
      {
      int time;
      time = Integer.parseInt(DM.myargv[i+1]) * 60 * 35;
      SPECS.levelTimer = true;
      SPECS.levelTimeCount = time;
      }
      
      //  Init special SECTORs.
      //sector = LL.sectors;
      for (i=0 ; i<LL.numsectors ; i++)
      {
          sector=LL.sectors[i];
      if (!eval(sector.special))
          continue;
      
      switch (sector.special)
      {
        case 1:
          // FLICKERING LIGHTS
            sector.SpawnLightFlash ();
          break;

        case 2:
          // STROBE FAST
            sector.SpawnStrobeFlash(FASTDARK,0);
          break;
          
        case 3:
          // STROBE SLOW
            sector.SpawnStrobeFlash(SLOWDARK,0);
          break;
          
        case 4:
          // STROBE FAST/DEATH SLIME
            sector.SpawnStrobeFlash(FASTDARK,0);
          sector.special = 4;
          break;
          
        case 8:
          // GLOWING LIGHT
            sector.SpawnGlowingLight();
          break;
        case 9:
          // SECRET SECTOR
          DM.totalsecret++;
          break;
          
        case 10:
          // DOOR CLOSE IN 30 SECONDS
          sector.SpawnDoorCloseIn30 ();
          break;
          
        case 12:
          // SYNC STROBE SLOW
            sector.SpawnStrobeFlash (SLOWDARK, 1);
          break;

        case 13:
          // SYNC STROBE FAST
            sector.SpawnStrobeFlash ( FASTDARK, 1);
          break;

        case 14:
          // DOOR RAISE IN 5 MINUTES
            sector.SpawnDoorRaiseIn5Mins (i);
          break;
          
        case 17:
            sector.SpawnFireFlicker();
          break;
      }
      }

      
      //  Init line EFFECTs
      SPECS.numlinespecials = 0;
      for (i = 0;i < LL.numlines; i++)
      {
      switch(LL.lines[i].special)
      {
        case 48:
          // EFFECT FIRSTCOL SCROLL+
          linespeciallist[SPECS.numlinespecials] = LL.lines[i];
          SPECS.numlinespecials++;
          break;
      }
      }

      
      //  Init other misc stuff
      for (i = 0;i < MAXCEILINGS;i++)
      activeceilings[i] = null;

      for (i = 0;i < MAXPLATS;i++)
      PEV.activeplats[i] = null;
      
      for (i = 0;i < MAXBUTTONS;i++)
          SW.buttonlist[i].reset();

      // UNUSED: no horizonal sliders.
      //  P_InitSlidingDoorFrames();
  }
  
  
  
  /**
   * Move a plat up and down
   */
  void PlatRaise(plat_t plat)
  {
      result_e    res;
      
      switch(plat.status)
      {
        case up:
      res = MovePlane(plat.sector,
                plat.speed,
                plat.high,
                plat.crush,0,1);
                      
      if (plat.type == plattype_e.raiseAndChange
          || plat.type == plattype_e.raiseToNearestAndChange)
      {
          if (!flags(DM.leveltime,7))
          S.StartSound(plat.sector.soundorg, sfxenum_t.sfx_stnmov);
      }
      
                  
      if (res == result_e.crushed && (!plat.crush))
      {
          plat.count = plat.wait;
          plat.status = plat_e.down;
          S.StartSound(plat.sector.soundorg, sfxenum_t.sfx_pstart);
      }
      else
      {
          if (res == result_e.pastdest)
          {
          plat.count = plat.wait;
          plat.status = plat_e.waiting;
          S.StartSound(plat.sector.soundorg,  sfxenum_t.sfx_pstop);

          switch(plat.type)
          {
            case blazeDWUS:
            case downWaitUpStay:
              PEV.RemoveActivePlat(plat);
              break;
              
            case raiseAndChange:
            case raiseToNearestAndChange:
              PEV.RemoveActivePlat(plat);
              break;
              
            default:
              break;
          }
          }
      }
      break;
      
        case  down:
      res = MovePlane(plat.sector,plat.speed,plat.low,false,0,-1);

      if (res == result_e.pastdest)
      {
          plat.count = plat.wait;
          plat.status = plat_e.waiting;
          S.StartSound(plat.sector.soundorg,sfxenum_t.sfx_pstop);
      }
      break;
      
        case  waiting:
      if (--plat.count==0)
      {
          if (plat.sector.floorheight == plat.low)
          plat.status = plat_e.up;
          else
          plat.status = plat_e.down;
          S.StartSound(plat.sector.soundorg,sfxenum_t.sfx_pstart);
      }
        case  in_stasis:
      break;
      }
  }
  
  public Actions(DoomContext DC){
	  this.updateStatus(DC);
	  this.thinkercap=new thinker_t();
  }

  //
  // P_RunThinkers
  //
  public void RunThinkers() {
      thinker_t currentthinker;

      currentthinker = thinkercap.next;
      while (currentthinker != thinkercap) {
          if (currentthinker.function == null) {
              // time to remove it
              currentthinker.next.prev = currentthinker.prev;
              currentthinker.prev.next = currentthinker.next;
          } else {
              if (currentthinker.function.getType() == acp1)
                  // Execute thinker's function.
                  dispatch(currentthinker.function, currentthinker, null);
          }
          currentthinker = currentthinker.next;
      }
  }



@Override
public void updateStatus(DoomContext DC) {
		this.I=DC.I;
		this.DG=DC.DG;
		this.S=DC.S;
  		this.LL=DC.LL;
  		this.RND=DC.RND;
        this.DM=DC.DM;
        this.R=DC.R;
        this.W=DC.W;
        this.AM=DC.AM;
        this.SW=new Switches();
        this.LEV=new Lights();
        this.SPECS=new Specials();
        this.PEV=new Plats();
        this.ST= (StatusBar) DC.ST;
        this.AM=DC.AM;
        this.A=this;
        this.HU=DC.HU;
        this.TM=DC.TM;
		}
  
}

