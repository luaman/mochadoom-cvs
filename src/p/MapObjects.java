package p;

import static data.Defines.BASETHRESHOLD;
import static data.Defines.ITEMQUESIZE;
import static data.Defines.MAPBLOCKSHIFT;
import static data.Defines.MELEERANGE;
import static data.Defines.MTF_AMBUSH;
import static data.Defines.NUMCARDS;
import static data.Defines.ONCEILINGZ;
import static data.Defines.ONFLOORZ;
import static data.Defines.PST_DEAD;
import static data.Defines.PST_LIVE;
import static data.Defines.PST_REBORN;
import static data.Defines.VIEWHEIGHT;
import static data.Limits.MAXMOVE;
import static data.Limits.MAXPLAYERS;
import static data.SineCosine.finecosine;
import static data.SineCosine.finesine;
import static data.Tables.ANG180;
import static data.Tables.ANGLETOFINESHIFT;
import static data.info.mobjinfo;
import static data.info.states;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedMul;
import static p.MapUtils.flags;
import static p.mobj.MF_AMBUSH;
import static p.mobj.MF_CORPSE;
import static p.mobj.MF_COUNTITEM;
import static p.mobj.MF_COUNTKILL;
import static p.mobj.MF_DROPOFF;
import static p.mobj.MF_DROPPED;
import static p.mobj.MF_FLOAT;
import static p.mobj.MF_JUSTHIT;
import static p.mobj.MF_NOBLOCKMAP;
import static p.mobj.MF_NOCLIP;
import static p.mobj.MF_NOGRAVITY;
import static p.mobj.MF_NOSECTOR;
import static p.mobj.MF_NOTDMATCH;
import static p.mobj.MF_SHADOW;
import static p.mobj.MF_SHOOTABLE;
import static p.mobj.MF_SKULLFLY;
import static p.mobj.MF_SOLID;
import static p.mobj.MF_SPAWNCEILING;
import static p.mobj.MF_SPECIAL;
import static p.mobj.MF_TRANSSHIFT;
import static p.mobj_t.MF_MISSILE;
import m.random;
import i.system;
import rr.sector_t;
import rr.subsector_t;
import data.mapthing_t;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.state_t;
import data.Defines.skill_t;
import data.Defines.statenum_t;
import doom.ActionType;
import doom.acp1;
import doom.player_t;
import doom.thinker_t;
import doom.weapontype_t;

public class MapObjects {

    private random RND;

    int test;

    //
    // P_SetMobjState
    // Returns true if the mobj is still present.
    //

    public boolean
    SetMobjState
    (mobj_t mobj, statenum_t    state )
    {
    state_t st;

    do
    {
    if (state == statenum_t.S_NULL)
    {
      mobj.state = null;
      // TODO:P_RemoveMobj (mobj);
      return false;
    }

    st = states[state.ordinal()];
    mobj.state = st;
    mobj.tics = st.tics;
    mobj.sprite = st.sprite;
    mobj.frame = (int) st.frame;

    // Modified handling.
    // Call action functions when the state is set
    if (st.action.getType()==ActionType.acp1)       
      ((acp1)st.action).invoke(mobj); 

    state = st.nextstate;
    } while (mobj.tics==0);
              
    return true;
    }






    //
    // P_ExplodeMissile  
    //
    public void ExplodeMissile (mobj_t mo)
    {
    mo.momx = mo.momy = mo.momz = 0;

    SetMobjState (mo, mobjinfo[mo.type.ordinal()].deathstate);

    mo.tics -= RND.P_Random()&3;

    if (mo.tics < 1)
    mo.tics = 1;

    mo.flags &= ~MF_MISSILE;

    if (mo.info.deathsound!=null) ;
    // TODO: ; // TODO: (mo, mo.info.deathsound);
    }


    //
    // P_XYMovement  
    //
    protected final static int STOPSPEED =      0x1000;
    protected final static int FRICTION =       0xe800;

    public void XYMovement (mobj_t mo) 
    {   
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
      
    if (!P_TryMove (mo, ptryx, ptryy))
    {
      // blocked move
      if (mo.player)
      {   // try to slide along it
      P_SlideMove (mo);
      }
      else if (mo.flags & MF_MISSILE)
      {
      // explode a missile
      if (ceilingline &&
          ceilingline.backsector &&
          ceilingline.backsector.ceilingpic == skyflatnum)
      {
          // Hack to prevent missiles exploding
          // against the sky.
          // Does not handle sky floors.
          P_RemoveMobj (mo);
          return;
      }
      P_ExplodeMissile (mo);
      }
      else
      mo.momx = mo.momy = 0;
    }
    } while ((xmove | ymove)!=0);

    // slow down
    if (player && player.cheats & CF_NOMOMENTUM)
    {
    // debug option for no sliding at all
    mo.momx = mo.momy = 0;
    return;
    }

    if (mo.flags & (MF_MISSILE | MF_SKULLFLY) )
    return;     // no friction for missiles ever
      
    if (mo.z > mo.floorz)
    return;     // no friction when airborne

    if (mo.flags & MF_CORPSE)
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
    && (!player
      || (player.cmd.forwardmove== 0
      && player.cmd.sidemove == 0 ) ) )
    {
    // if in a walking frame, stop moving
    if ( player&&(unsigned)((player.mo.state - states)- S_PLAY_RUN1) < 4)
      P_SetMobjState (player.mo, S_PLAY);

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
    // P_NightmareRespawn
    //
    void
    NightmareRespawn (mobj_t mobj)
    {
    fixed_t     x;
    fixed_t     y;
    fixed_t     z; 
    subsector_t    ss; 
    mobj_t     mo;
    mapthing_t*     mthing;
      
    x = mobj.spawnpoint.x << FRACBITS; 
    y = mobj.spawnpoint.y << FRACBITS; 

    // somthing is occupying it's position?
    if (!P_CheckPosition (mobj, x, y) ) 
    return; // no respwan

    // spawn a teleport fog at old spot
    // because of removal of the body?
    mo = P_SpawnMobj (mobj.x,
            mobj.y,
            mobj.subsector.sector.floorheight , MT_TFOG); 
    // initiate teleport sound
    ; // TODO: (mo, sfx_telept);

    // spawn a teleport fog at the new spot
    ss = R_PointInSubsector (x,y); 

    mo = P_SpawnMobj (x, y, ss.sector.floorheight , MT_TFOG); 

    ; // TODO: (mo, sfx_telept);

    // spawn the new monster
    mthing = &mobj.spawnpoint;

    // spawn it
    if (mobj.info.flags & MF_SPAWNCEILING)
    z = ONCEILINGZ;
    else
    z = ONFLOORZ;

    // inherit attributes from deceased one
    mo = P_SpawnMobj (x,y,z, mobj.type);
    mo.spawnpoint = mobj.spawnpoint;  
    mo.angle = ANG45 * (mthing.angle/45);

    if (mthing.options & MTF_AMBUSH)
    mo.flags |= MF_AMBUSH;

    mo.reactiontime = 18;

    // remove the old monster,
    P_RemoveMobj (mobj);
    }






    class P_MobjThinker implements acp1 {
        
            @Override
            public void invoke (mobj_t mobj) {
    // momentum movement
    if (mobj.momx!=0
    || mobj.momy!=0
    || (flags(mobj.flags,MF_SKULLFLY)) )
    {
        mobj.XYMovement ();

    // FIXME: decent NOP/NULL/Nil function pointer please.
    if (mobj.thinker.function == null)
      return;     // mobj was removed
    }
    if ( (mobj.z != mobj.floorz)
    || mobj.momz!=0 )
    {
        mobj.ZMovement ();

    // FIXME: decent NOP/NULL/Nil function pointer please.
    if (mobj.thinker.function == null)
      return;     // mobj was removed
    }


    // cycle through states,
    // calling action functions at transitions
    if (mobj.tics != -1)
    {
    mobj.tics--;
      
    // you can cycle through multiple states in a tic
    if (!mobj.tics)
      if (!P_SetMobjState (mobj, mobj.state.nextstate) )
      return;     // freed itself
    }
    else
    {
    // check for nightmare respawn
    if (! (mobj.flags & MF_COUNTKILL) )
      return;

    if (!respawnmonsters)
      return;

    mobj.movecount++;

    if (mobj.movecount < 12*35)
      return;

    if ( leveltime&31 )
      return;

    if (P_Random () > 4)
      return;

    P_NightmareRespawn (mobj);
    }

    }

    @Override
    public ActionType getType() {
        return ActionType.acp1;
    }

    }


    //
    // P_RemoveMobj
    //
    mapthing_t[]  itemrespawnque=new mapthing_t[ITEMQUESIZE];
    int[]     itemrespawntime=new int[ITEMQUESIZE];
    int     iquehead;
    int     iquetail;


    void RemoveMobj (mobj_t mobj)
    {
    if ((mobj.flags & MF_SPECIAL)
    && !(mobj.flags & MF_DROPPED)
    && (mobj.type != MT_INV)
    && (mobj.type != MT_INS))
    {
    itemrespawnque[iquehead] = mobj.spawnpoint;
    itemrespawntime[iquehead] = leveltime;
    iquehead = (iquehead+1)&(ITEMQUESIZE-1);

    // lose one off the end?
    if (iquehead == iquetail)
      iquetail = (iquetail+1)&(ITEMQUESIZE-1);
    }

    // unlink from sector and block lists
    P_UnsetThingPosition (mobj);

    // stop any playing sound
    S_StopSound (mobj);

    // free block
    P_RemoveThinker ((thinker_t)mobj);
    }




    //
    // P_RespawnSpecials
    //
    void RespawnSpecials ()
    {
    fixed_t     x;
    fixed_t     y;
    fixed_t     z;

    subsector_t    ss; 
    mobj_t     mo;
    mapthing_t*     mthing;

    int         i;

    // only respawn items in deathmatch
    if (deathmatch != 2)
    return; // 

    // nothing left to respawn?
    if (iquehead == iquetail)
    return;     

    // wait at least 30 seconds
    if (leveltime - itemrespawntime[iquetail] < 30*35)
    return;         

    mthing = &itemrespawnque[iquetail];

    x = mthing.x << FRACBITS; 
    y = mthing.y << FRACBITS; 

    // spawn a teleport fog at the new spot
    ss = R_PointInSubsector (x,y); 
    mo = P_SpawnMobj (x, y, ss.sector.floorheight , MT_IFOG); 
    ; // TODO: (mo, sfx_itmbk);

    // find which type to spawn
    for (i=0 ; i< NUMMOBJTYPES ; i++)
    {
    if (mthing.type == mobjinfo[i].doomednum)
      break;
    }

    // spawn it
    if (flags(mobjinfo[i].flags ,MF_SPAWNCEILING))
    z = ONCEILINGZ;
    else
    z = ONFLOORZ;

    mo = P_SpawnMobj (x,y,z, i);
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
    void SpawnPlayer (mapthing_t mthing)
    {
    player_t       p;
    int     x;
    int     y;
    int     z;

    mobj_t     mobj;

    int         i;

    // not playing?
    if (!playeringame[mthing.type-1])
    return;                 
      
    p = &players[mthing.type-1];

    if (p.playerstate == PST_REBORN)
    G_PlayerReborn (mthing.type-1);

    x       = mthing.x << FRACBITS;
    y       = mthing.y << FRACBITS;
    z       = ONFLOORZ;
    mobj    = P_SpawnMobj (x,y,z, MT_PLAYER);

    // set color translations for player sprites
    if (mthing.type > 1)       
    mobj.flags |= (mthing.type-1)<<MF_TRANSSHIFT;
      
    mobj.angle = ANG45 * (mthing.angle/45);
    mobj.player = p;
    mobj.health = p.health;

    p.mo = mobj;
    p.playerstate = PST_LIVE;  
    p.refire = 0;
    p.message = NULL;
    p.damagecount = 0;
    p.bonuscount = 0;
    p.extralight = 0;
    p.fixedcolormap = 0;
    p.viewheight = VIEWHEIGHT;

    // setup gun psprite
    P_SetupPsprites (p);

    // give all cards in death match mode
    if (deathmatch)
    for (i=0 ; i<NUMCARDS ; i++)
      p.cards[i] = true;
          
    if (mthing.type-1 == consoleplayer)
    {
    // wake up the status bar
    ST_Start ();
    // wake up the heads up text
    HU_Start ();        
    }
    }


    //
    // P_SpawnMapThing
    // The fields of the mapthing should
    // already be in host byte order.
    //
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
    if (deathmatch_p < &deathmatchstarts[10])
    {
      memcpy (deathmatch_p, mthing, sizeof(*mthing));
      deathmatch_p++;
    }
    return;
    }

    // check for players specially
    if (mthing.type <= 4)
    {
    // save spots for respawning in network games
    playerstarts[mthing.type-1] = *mthing;
    if (!deathmatch)
      P_SpawnPlayer (mthing);

    return;
    }

    // check for apropriate skill level
    if (!netgame && (mthing.options & 16) )
    return;
      
    if (gameskill == sk_baby)
    bit = 1;
    else if (gameskill == sk_nightmare)
    bit = 4;
    else
    bit = 1<<(gameskill-1);

    if (!(mthing.options & bit) )
    return;

    // find which type to spawn
    for (i=0 ; i< NUMMOBJTYPES ; i++)
    if (mthing.type == mobjinfo[i].doomednum)
      break;

    if (i==NUMMOBJTYPES)
    system.Error ("P_SpawnMapThing: Unknown type %i at (%i, %i)",
       mthing.type,
       mthing.x, mthing.y);
      
    // don't spawn keycards and players in deathmatch
    if (deathmatch && mobjinfo[i].flags & MF_NOTDMATCH)
    return;
      
    // don't spawn any monsters if -nomonsters
    if (nomonsters
    && ( i == MT_SKULL
       || (mobjinfo[i].flags & MF_COUNTKILL)) )
    {
    return;
    }

    // spawn it
    x = mthing.x << FRACBITS;
    y = mthing.y << FRACBITS;

    if (mobjinfo[i].flags & MF_SPAWNCEILING)
    z = ONCEILINGZ;
    else
    z = ONFLOORZ;

    mobj = P_SpawnMobj (x,y,z, i);
    mobj.spawnpoint = *mthing;

    if (mobj.tics > 0)
    mobj.tics = 1 + (P_Random () % mobj.tics);
    if (mobj.flags & MF_COUNTKILL)
    totalkills++;
    if (mobj.flags & MF_COUNTITEM)
    totalitems++;
      
    mobj.angle = ANG45 * (mthing.angle/45);
    if (mthing.options & MTF_AMBUSH)
    mobj.flags |= MF_AMBUSH;
    }



    //
    // GAME SPAWN FUNCTIONS
    //


    //
    // P_SpawnPuff
    //
    // TODO: extern fixed_t attackrange;

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

    th = P_SpawnMobj (x,y,z, MT_PUFF);
    th.momz = FRACUNIT;
    th.tics -= P_Random()&3;

    if (th.tics < 1)
    th.tics = 1;

    // don't make punches spark on the wall
    if (attackrange == MELEERANGE)
        th.SetMobjState (statenum_t.S_PUFF3);
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

    z += ((P_Random()-P_Random())<<10);
    th = P_SpawnMobj (x,y,z, MT_BLOOD);
    th.momz = FRACUNIT*2;
    th.tics -= P_Random()&3;

    if (th.tics < 1)
    th.tics = 1;
      
    if (damage <= 12 && damage >= 9)
    P_SetMobjState (th,S_BLOOD2);
    else if (damage < 9)
    P_SetMobjState (th,S_BLOOD3);
    }



    /** P_CheckMissileSpawn
     * Moves the missile forward a bit
     * and possibly explodes it right there.
     * 
     * @param th
     */

    void CheckMissileSpawn (mobj_t th)
    {
    th.tics -= P_Random()&3;
    if (th.tics < 1)
    th.tics = 1;

    // move a little forward so an angle can
    // be computed if it immediately explodes
    th.x += (th.momx>>1);
    th.y += (th.momy>>1);
    th.z += (th.momz>>1);

    if (!P_TryMove (th, th.x, th.y))
    P_ExplodeMissile (th);
    }


    //
    // P_SpawnMissile
    //
    private mobj_t
    SpawnMissile
    ( mobj_t   source,
    mobj_t   dest,
    mobjtype_t    type )
    {
    mobj_t th;
    int an; // angle_t
    int     dist;

    th = P_SpawnMobj (source.x,
            source.y,
            source.z + 4*8*FRACUNIT, type);

    if (th.info.seesound)
    ; // TODO: (th, th.info.seesound);

    th.target = source;    // where it came from
    an = R_PointToAngle2 (source.x, source.y, dest.x, dest.y);  

    // fuzzy player
    if (dest.flags & MF_SHADOW)
    an += (P_Random()-P_Random())<<20;  

    th.angle = an;
    an >>= ANGLETOFINESHIFT;
    th.momx = FixedMul (th.info.speed, finecosine[an]);
    th.momy = FixedMul (th.info.speed, finesine[an]);

    dist = P_AproxDistance (dest.x - source.x, dest.y - source.y);
    dist = dist / th.info.speed;

    if (dist < 1)
    dist = 1;

    th.momz = (dest.z - source.z) / dist;
    P_CheckMissileSpawn (th);

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
    int an; // angle_t
    int x, y, z,slope; // think_t

    // see which target is to be aimed at
    an = source.angle;
    slope = AimLineAttack (source, an, 16*64*FRACUNIT);

    if (!linetarget)
    {
    an += 1<<26;
    slope = AimLineAttack (source, an, 16*64*FRACUNIT);

    if (!linetarget)
    {
      an -= 2<<26;
      slope = AimLineAttack (source, an, 16*64*FRACUNIT);
    }

    if (!linetarget)
    {
      an = source.angle;
      slope = 0;
    }
    }
      
    x = source.x;
    y = source.y;
    z = source.z + 4*8*FRACUNIT;

    th = P_SpawnMobj (x,y,z, type);

    if (th.info.seesound)
    ; // TODO: (th, th.info.seesound);

    th.target = source;
    th.angle = an;
    th.momx = FixedMul( th.info.speed,
           finecosine[an>>ANGLETOFINESHIFT]);
    th.momy = FixedMul( th.info.speed,
           finesine[an>>ANGLETOFINESHIFT]);
    th.momz = FixedMul( th.info.speed, slope);

    P_CheckMissileSpawn (th);
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
                  || player.powers[pW.invulnerability]!=0 ) 
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

     mobj = new mobj_t();
     info = mobjinfo[type.ordinal()];

     mobj.type = type;
     mobj.info = info;
     mobj.x = x;
     mobj.y = y;
     mobj.radius = info.radius;
     mobj.height = info.height;
     mobj.flags = info.flags;
     mobj.health = info.spawnhealth;

     if (DS.gameskill != skill_t.sk_nightmare)
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

     mobj.thinker.function=P_MobjThinker;

     AddThinker (mobj.thinker);

     return mobj;
     }
     
     /** P_SetThingPosition
      * Links a thing into both a block and a subsector
      * based on it's x y.
      * Sets thing.subsector properly
      */

     public void SetThingPosition (mobj_t thing)
     {
      subsector_t    ss;
      sector_t       sec;
      int         blockx;
      int         blocky;
      mobj_t        link;

      
      // link into subsector
      ss = R.PointInSubsector (thing.x,thing.y);
      thing.subsector = ss;
      
      if ( ! flags(thing.flags ,MF_NOSECTOR) )
      {
      // invisible things don't go into the sector links
      sec = ss.sector;
      
      thing.sprev = null;
      thing.snext = sec.thinglist;

      if (sec.thinglist!=null)
          sec.thinglist.sprev = thing;

      sec.thinglist = thing;
      }

      
      // link into blockmap
      if ( ! flags(thing.flags ,MF_NOBLOCKMAP) )
      {
      // inert things don't need to be in blockmap        
      blockx = (thing.x - LL.bmaporgx)>>MAPBLOCKSHIFT;
      blocky = (thing.y - LL.bmaporgy)>>MAPBLOCKSHIFT;

      if (blockx>=0
          && blockx < LL.bmapwidth
          && blocky>=0
          && blocky < LL.bmapheight)
      {
         
          link = LL.blocklinks[blocky*LL.bmapwidth+blockx];
          thing.bprev = null;
          thing.bnext = link; // FIXME: will this work?
          if (link!=null)
          // This will work
          link.bprev = thing;

          // link=thing won't work, assignment should be made directly
          LL.blocklinks[blocky*LL.bmapwidth+blockx]=thing;
      }
      else
      {
          // thing is off the map
          thing.bnext = thing.bprev = null;
      }
      }

        
     }
     }
