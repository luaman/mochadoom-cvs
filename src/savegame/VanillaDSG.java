package savegame;
import static data.Limits.MAXPLAYERS;
import i.DoomStatusAware;
import i.IDoomSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import data.Defines;
import data.info;
import defines.GameMode_t;
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

public class VanillaDSG implements IDoomSaveGame, DoomStatusAware {
    
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
    public IDoomSaveGameHeader getHeader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setHeader(IDoomSaveGameHeader header) {
        // TODO Auto-generated method stub
        
    }

    private DoomFile f;
    
    @Override
    public boolean doLoad(DoomFile f)
            {
        try {
        this.header=new VanillaDSGHeader();
        this.f=f;
        header.read(f);
        UnArchivePlayers();
        UnArchiveWorld();
        UnArchiveThinkers();
        UnArchiveSpecials();
        byte terminator=f.readByte();
        if (terminator != 0x1D) return false;
        else return true;
        } catch (Exception e){
            e.printStackTrace();
            System.err.printf("Error while loading savegame! Cause: %s",e.getMessage());
            return false; // Needed to shut up compiler.
        }
        
    }
    
    
 /**
  * P_UnArchivePlayers
 * @throws IOException 
  */
 protected void UnArchivePlayers () throws IOException
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
    
 /**
  * P_UnArchivePlayers
 * @throws IOException 
  */
 protected void ArchivePlayers () throws IOException
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
protected final void UnArchiveWorld () throws IOException
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
      System.out.println("Sector"+i+": "+sec);
      sec.specialdata = null;
      sec.soundtarget = null;
  }
  adaptSectors();
  // do lines
  for (i=0 ; i<LL.numlines ; i++)
  {
  li=LL.lines[i];
  // MAES: something similar occurs with lines, too.
  li.read(f);
  System.out.println("Line "+i+": "+li);
  //System.out.print(i+ " {");
  for (j=0 ; j<2 ; j++)
  {
    //  System.out.print(li.sidenum[j]);
    //  if (j<2) System.out.print(",");
   //   System.out.printf("Skipped sidenum %d for line %d\n",j,i);
      if (li.sidenum[j] == -1){
  //        System.out.printf("Skipped sidenum %d for line %d\n",j,i);
      continue;
      }
      // Similarly, sides also get a careful unmarshalling even
      // in vanilla. No "dumb" block reads here.
      si = LL.sides[li.sidenum[j]];
      si.read(f);

  }
  System.out.printf("Position at end of WORLD: %d\n",f.getFilePointer());
  }
  
}

protected void adaptSectors(){
	sector_t sec;
	switch(DS.getGameMode()){
	case registered:
	case shareware:
	for (int i=0;i<LL.numsectors;i++){
		sec=LL.sectors[i];
		// Between the F1_START and F1_END mark (in vanilla)
		if (sec.floorpic<=54){
			sec.floorpic-=1;
		} else {
		// Between the F2_START and F2_END mark (in vanilla)
			sec.floorpic-=3;
		}
		if (sec.ceilingpic<=54){
			sec.ceilingpic-=1;
		} else {
		// Between the F2_START and F2_END mark (in vanilla)
			sec.ceilingpic-=3;
		}
		
	}
	break;
	case commercial:
	case pack_plut:
	case pack_tnt:
		
		for (int i=0;i<LL.numsectors;i++){
			sec=LL.sectors[i];
			// Between the F1_START and F1_END mark (in vanilla)
			if (sec.floorpic<=54){
				sec.floorpic-=1;
			} else 
			if (sec.floorpic<=99){
			// Between the F2_START and F2_END mark (in vanilla)
				sec.floorpic-=3;
			} else {
				sec.floorpic-=5;
			}
			
			if (sec.ceilingpic<=54){
				sec.ceilingpic-=1;
			} else if (sec.ceilingpic<=99){
			// Between the F2_START and F2_END mark (in vanilla)
				sec.ceilingpic-=3;
			} else {
				sec.ceilingpic-=5;
			}
			
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
 
List<mobj_t> TL=new ArrayList<mobj_t>();

//
//P_UnArchiveThinkers
//
protected void UnArchiveThinkers () throws IOException
{
 thinkerclass_t        tclass; // was "byte", therefore unsigned
 thinker_t      currentthinker;
 thinker_t      next;
 mobj_t     mobj;
 int id=0;
 
 
 
 // remove all the current thinkers
 
 currentthinker = A.getThinkerCap().next;
 while (currentthinker!=null && currentthinker != A.getThinkerCap())
 {
 next = currentthinker.next;
 
 if (currentthinker.function ==  think_t.P_MobjThinker)
     A.RemoveMobj ((mobj_t)currentthinker);
 else{
     //currentthinker.next.prev=currentthinker.prev;
     //currentthinker.prev.next=currentthinker.next;
     currentthinker=null;
 }
     
 currentthinker = next;
 }
 
 A.InitThinkers ();
 
 // read in saved thinkers
 boolean end=false;
 while (!end)
 {
     int tmp=f.readUnsignedByte();
     tclass=thinkerclass_t.values()[tmp];
 switch (tclass)
 {
   case tc_end:
       end=true;
     break;     // end of list
         
   case tc_mobj:
     PADSAVEP(f);     
     mobj=new mobj_t(A);
     mobj.read(f);
     mobj.id=++id;
     TL.add(mobj);
     mobj.state = info.states[mobj.stateid];
     mobj.target = null;
     if (mobj.playerid!=0)
     {
     mobj.player = DS.players[mobj.playerid-1];
     mobj.player.mo = mobj;

     }
     LL.SetThingPosition (mobj);
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
 reconstructPointers();
 rewirePointers();
}

Hashtable<Integer,mobj_t> pointindex=new Hashtable<Integer,mobj_t> ();

/** Allows reconstructing infighting targets from stored pointers/indices.
 *  Works even with vanilla savegames as long as whatever it is that you
 *  store is unique. A good choice would be progressive indices or hash values.
 * 
 */

protected void reconstructPointers(){
    
    int player=0;
    
    for(mobj_t th: TL){
        
        System.out.printf("Thinker with id %d has prev %x next %x\n",th.id,th.previd,th.nextid);
        if (th.player!=null){
        System.out.printf("Thinker with id %d is player %d with mobj pointer %x\n",th.id,th.player.identify(),th.player.p_mobj);
        player=th.id;
        // Player found, so that's our first key.
        pointindex.put(th.player.p_mobj,th);
        }
    }
    
    if (player==0) {
        System.err.println("Player not found, cannot reconstruct pointers!");
        return;
    }
    
    int curr; // next or prev index
    
    
    // We start from the player's index, if found.
    // -1 so it matches that of the TL list.
    for (int i=(player-1);i<TL.size()-1;i++){
        // Get "next" pointer.
        curr=TL.get(i).nextid;
        pointindex.put(curr, TL.get(i+1));
    }
    
    // We also search backwards, in case player wasn't first object
    // (can this even happen, in vanilla?)
    // -1 so it matches that of the TL list.
    for (int i=(player-1);i>0;i++){
        // Get "prev" pointer.
        curr=TL.get(i).previd;
        pointindex.put(curr,TL.get(i-1));
    }
}

/** Allows reconstructing infighting targets from stored pointers/indices from
 * the hashtable created by reconstructPointers.
 * 
 */

protected void rewirePointers(){
    
    for(mobj_t th: TL){
        if (th.p_target!=0){
            th.target=pointindex.get(th.p_target);
            System.out.printf("Object %s has target %s\n",th.type.toString(),th.target.type.toString());
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
protected void UnArchiveSpecials () throws IOException
{
    specials_e        tclass;
 ceiling_t      ceiling;
 vldoor_t       door;
 floormove_t    floor;
 plat_t     plat;
 lightflash_t   flash;
 strobe_t       strobe;
 glow_t     glow;
 
 //List<thinker_t> A=new ArrayList<thinker_t>();
 
 
 // read in saved thinkers
 while (true)
 {
     int tmp=f.readUnsignedByte();
     //tmp&=0x00ff; // To "unsigned byte"
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
        System.out.printf("Current position %d Padding by %d bytes\n",save_p,((4 - ((int) save_p & 3)) & 3));
        
        f.seek(save_p+((4 - ((int) save_p & 3)) & 3));
    }


    @Override
    public void updateStatus(DoomStatus DS) {
        this.DS=DS;
        this.LL=DS.LL;
        this.A=DS.P;
        
    }

}
