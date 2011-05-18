package savegame;
import static data.Limits.MAXPLAYERS;
import i.DoomStatusAware;
import i.IDoomSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import data.Defines;
import data.info;
import doom.DoomStatus;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;

import p.Actions;
import p.LevelLoader;
import p.ThinkerList;
import p.ceiling_t;
import p.floormove_t;
import p.glow_t;
import p.lightflash_t;
import p.mobj_t;
import p.plat_t;
import p.strobe_t;
import p.vldoor_t;
import rr.line_t;
import rr.sector_t;
import rr.side_t;
import utils.C2JUtils;
import w.DoomFile;
import w.IReadableDoomObject;

public class VanillaDSG implements IDoomSaveGame, IReadableDoomObject, DoomStatusAware {
    
    VanillaDSGHeader header;
    DoomStatus DS;
    LevelLoader LL;
    Actions A;
    IDoomSystem I;
    
    public VanillaDSG(){
        
    }
    

    @Override
    public void setThinkerList(ThinkerList li) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doSave() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doLoad() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public IDoomSaveGameHeader getHeader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setHeader(IDoomSaveGameHeader header) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void read(DoomFile f) throws IOException
            {
        this.header=new VanillaDSGHeader();
        header.read(f);
        P_UnArchivePlayers(f);
        P_UnArchiveWorld(f);
        P_UnArchiveThinkers(f);
        P_UnArchiveSpecials(f);
        
    }
    
    
 /**
  * P_UnArchivePlayers
 * @throws IOException 
  */
 protected void P_UnArchivePlayers (DoomFile f) throws IOException
 {
     int     i;
     int     j;
     
     for (i=0 ; i<MAXPLAYERS ; i++)
     {
     // Multiplayer savegames are different!
     if (!DS.playeringame[i])
         continue;
     PADSAVEP(f); // this will move us on the 52th byte, instead of 50th.
     DS.players[i].read(f);
     
     //memcpy (&players[i],save_p, sizeof(player_t));
     //save_p += sizeof(player_t);
     
     // will be set when unarc thinker
     DS.players[i].mo = null;   
     DS.players[i].message = null;
     DS.players[i].attacker = null;

     
     for (j=0 ; j<player_t.NUMPSPRITES ; j++)
     {
         if (C2JUtils.eval(DS.players[i].psprites[j].state))
         {
             // MAES HACK to accomoadate state_t type punning a-posteriori
             DS.players[i].psprites[j].state =
             info.states[DS.players[i].psprites[j].readstate];
         }
     }
     }
 }
    
 //
//P_UnArchiveWorld
//
protected final void P_UnArchiveWorld (DoomFile f) throws IOException
{
  int         i;
  int         j;
  sector_t       sec;
  line_t     li;
  side_t     si;
  // short      get;
  //get = (short *)save_p;

  List<sector_t> sectors=new ArrayList<sector_t>();
  // do sectors
  for (i=0; i<LL.numsectors ; i++)
  {
      sec=LL.sectors[i];
      // MAES: sectors were actually carefully
      // unmarshalled, so we don't just read/write
      // their entire memory footprint to disk.
      sec.read(f);
      sec.specialdata = null;
      sec.soundtarget = null;
  }
  
  // do lines
  for (i=0 ; i<LL.numlines ; i++)
  {
  li=LL.lines[i];
  // MAES: something similar occur with lines, too.
  li.read(f);

  for (j=0 ; j<2 ; j++)
  {
      if (li.sidenum[j] == -1)
      continue;
      // Similarly, sides also get a careful unmarshalling even
      // in vanilla. No "dumb" block reads here.
      si = LL.sides[li.sidenum[j]];
      si.read(f);

  }
  }
  
}

//
//Thinkers
//
protected enum thinkerclass_t 
{
 tc_end,
 tc_mobj;
}
  
//
//P_UnArchiveThinkers
//
protected void P_UnArchiveThinkers (DoomFile f) throws IOException
{
 thinkerclass_t        tclass; // was "byte", therefore unsigned
 thinker_t      currentthinker;
 thinker_t      next;
 mobj_t     mobj;
 
 // remove all the current thinkers
 /*
 currentthinker = A.getThinkerCap().next;
 while (currentthinker != A.getThinkerCap())
 {
 next = currentthinker.next;
 
 if (currentthinker.function ==  think_t.P_MobjThinker)
     A.RemoveMobj ((mobj_t)currentthinker);
 else{
     currentthinker.next.prev=currentthinker.prev;
     currentthinker.prev.next=currentthinker.next;
     //Z_Free (currentthinker);
 }
     
 currentthinker = next;
 }*/
 //A.InitThinkers ();
 
 // read in saved thinkers
 while (true)
 {
     int tmp=f.readLEInt();
     tmp&=0x00ff; // To "unsigned byte"
     tclass=thinkerclass_t.values()[tmp];
 switch (tclass)
 {
   case tc_end:
     return;     // end of list
         
   case tc_mobj:
     PADSAVEP(f);
     mobj=new mobj_t();
     mobj.read(f);
     mobj.state = info.states[mobj.state.id];
     mobj.target = null;
     if (mobj.player!=null)
     {
     mobj.player = DS.players[mobj.player.identify()];
     mobj.player.mo = mobj;
     }
     A.SetThingPosition (mobj);
     mobj.info = info.mobjinfo[mobj.type.ordinal()];
     mobj.floorz = mobj.subsector.sector.floorheight;
     mobj.ceilingz = mobj.subsector.sector.ceilingheight;
     mobj.function = think_t.P_MobjThinker;
     A.AddThinker (mobj);
     break;
         
   default:
     I.Error ("Unknown tclass %i in savegame",tclass);
 }
 
 }

}

protected enum specials_e
{
    tc_ceiling,
    tc_door,
    tc_floor,
    tc_plat,
    tc_flash,
    tc_strobe,
    tc_glow,
    tc_endspecials

} ;   

//
//P_UnArchiveSpecials
//
protected void P_UnArchiveSpecials (DoomFile f) throws IOException
{
    specials_e        tclass;
 ceiling_t      ceiling;
 vldoor_t       door;
 floormove_t    floor;
 plat_t     plat;
 lightflash_t   flash;
 strobe_t       strobe;
 glow_t     glow;
 
 
 // read in saved thinkers
 while (true)
 {
     int tmp=f.readLEInt();
     tmp&=0x00ff; // To "unsigned byte"
     tclass=specials_e.values()[tmp];
 switch (tclass)
 {
   case tc_endspecials:
     return; // end of list
         
   case tc_ceiling:
     PADSAVEP(f);
     ceiling = new ceiling_t();
     ceiling.read(f);
     ceiling.sector = LL.sectors[ceiling.sectorid];
     ceiling.sector.specialdata = ceiling;

     if (ceiling.function.getType()==Defines.acp1)
     ceiling.function = think_t.T_MoveCeiling;

     A.AddThinker (ceiling);
     A.AddActiveCeiling(ceiling);
     break;
             
   case tc_door:
     PADSAVEP(f);
     door=new vldoor_t();
     door.read(f);
     door.sector = LL.sectors[door.sectorid];
     door.sector.specialdata = door;
     door.function = think_t.T_VerticalDoor;
     A.AddThinker (door);
     break;
             
   case tc_floor:
     PADSAVEP(f);
     floor=new floormove_t();
     floor.read(f);
     floor.sector = LL.sectors[floor.sectorid];
     floor.sector.specialdata = floor;
     floor.function = think_t.T_MoveFloor;
     A.AddThinker (floor);
     break;
             
   case tc_plat:
     PADSAVEP(f);
     plat=new plat_t();
     plat.read(f);
     plat.sector = LL.sectors[plat.sectorid];
     plat.sector.specialdata = plat;

     if (plat.function!=null)
     plat.function =  think_t.T_PlatRaise;

     A.AddThinker (plat);
     A.AddActivePlat(plat);
     break;
             
   case tc_flash:
     PADSAVEP(f);
     flash=new lightflash_t(this.DS.RND);
     flash.read(f);
     
     flash.sector =LL.sectors[flash.sectorid];
     flash.function =  think_t.T_LightFlash;
     A.AddThinker (flash);
     break;
             
   case tc_strobe:
     PADSAVEP(f);
     
     strobe = new strobe_t();
     strobe.read(f);
     strobe.sector = LL.sectors[strobe.sectorid];
     strobe.function =  think_t.T_StrobeFlash;
     A.AddThinker (strobe);
     break;
             
   case tc_glow:
     PADSAVEP(f);
     glow = new glow_t();
     glow.read(f);
     glow.sector = LL.sectors[glow.sectorid];
     glow.function = think_t.T_Glow;
     A.AddThinker (glow);
     break;
             
   default:
     I.Error ("P_UnarchiveSpecials:Unknown tclass %d in savegame",tmp);
 }
 }
 
 }

    /**
     * Pads save_p to a 4-byte boundary
     * so that the load/save works on SGI&Gecko.
     * 
     * @param save_p
     */
    
    protected final int PADSAVEP(int save_p){
        return (save_p += (4 - ((int) save_p & 3)) & 3);
    }
    protected final void PADSAVEP(DoomFile f) throws IOException{
        long save_p=f.getFilePointer();
        f.seek(save_p+((4 - ((int) save_p & 3)) & 3));
    }


    @Override
    public void updateStatus(DoomStatus DS) {
        this.DS=DS;
        this.LL=DS.LL;
        
    }

}
