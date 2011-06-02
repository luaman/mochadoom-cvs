package savegame;
import static data.Limits.*;
import i.DoomStatusAware;
import i.IDoomSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
        return header;
    }

    @Override
    public void setHeader(IDoomSaveGameHeader header) {
        this.header=(VanillaDSGHeader) header;
        
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

     // State will have to be serialized when saving.
     DS.players[i].write(f);

     //System.out.printf("Player %d has mobj hashcode %d",(1+i),DS.players[i].mo.hashCode());
     }
 }

 //
//P_ArchiveWorld
//
protected void ArchiveWorld () throws IOException
{
  int         i;
  int         j;
  sector_t       sec;
  line_t     li;
  side_t     si;
  short      put;
  
  //put = (short *)save_p;
  
  // do sectors (allocate 14 bytes per sector)
  ByteBuffer buffer=ByteBuffer.allocate(LL.numsectors*14);
  buffer.order(ByteOrder.LITTLE_ENDIAN);
  
  deAdaptSectors();
  for (i=0; i<LL.numsectors ; i++)
  {
      sec=LL.sectors[i];
      // MAES: sectors are actually carefully
      // marshalled, so we don't just read/write
      // their entire memory footprint to disk.
      sec.pack(buffer);
  }
  
  adaptSectors();
  f.write(buffer.array(),0,buffer.position());
  
  // do lines 
  // Allocate for the worst-case scenario (6+20 per line)
  buffer=ByteBuffer.allocate(LL.numlines*(6+20));
  buffer.order(ByteOrder.LITTLE_ENDIAN);
  buffer.position(0); 
  
  //final side_t test1=new side_t(0x11111111,0x11111111,(short) 0x1111,(short)0x1111,(short)0x1111,null);
  //final side_t test2=new side_t(0x22222222,0x22222222,(short) 0x2222,(short)0x2222,(short)0x2222,null);
  
  for (i=0; i<LL.numlines ; i++)
  {
  li=LL.lines[i];
  li.pack(buffer);

  for (j=0 ; j<2 ; j++)
  {
      if (li.sidenum[j] == -1)
      continue;
      si = LL.sides[li.sidenum[j]];
      si.pack(buffer);
      //if (j==0) test1.pack(buffer);
      //else test2.pack(buffer);
      
  }
  }
  
  int write=buffer.position();
  f.write(buffer.array(),0,write);
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

  //List<sector_t> sectors=new ArrayList<sector_t>();
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
  adaptSectors();
  // do lines
  for (i=0 ; i<LL.numlines ; i++)
  {
  li=LL.lines[i];
  // MAES: something similar occurs with lines, too.
  li.read(f);
  //System.out.println("Line "+i+": "+li);
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
  //System.out.printf("Position at end of WORLD: %d\n",f.getFilePointer());
  }
  
}

/** Convert loaded sectors from vanilla savegames into the internal,
 *  continuous index progression, by intercepting breaks corresponding to markers.
 */

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

/** De-convert sectors from an absolute to a vanilla-like index
 *  progression, by adding proper skips
 */

protected void deAdaptSectors(){
    sector_t sec;
    switch(DS.getGameMode()){
    case registered:
    case shareware:
    for (int i=0;i<LL.numsectors;i++){
        sec=LL.sectors[i];
        // Between the F1_START and F1_END mark (in vanilla)
        if (sec.floorpic<54){
            sec.floorpic+=1;
        } else {
        // Between the F2_START and F2_END mark (in vanilla)
            sec.floorpic+=3;
        }
        if (sec.ceilingpic<54){
            sec.ceilingpic+=1;
        } else {
        // Between the F2_START and F2_END mark (in vanilla)
            sec.ceilingpic+=3;
        }
        
    }
    break;
    case commercial:
    case pack_plut:
    case pack_tnt:
        
        for (int i=0;i<LL.numsectors;i++){
            sec=LL.sectors[i];
            // Between the F1_START and F1_END mark (in vanilla)
            if (sec.floorpic<54){
                sec.floorpic+=1;
            } else 
            if (sec.floorpic<99){
            // Between the F2_START and F2_END mark (in vanilla)
                sec.floorpic+=3;
            } else {
                sec.floorpic+=5;
            }
            
            if (sec.ceilingpic<54){
                sec.ceilingpic+=1;
            } else if (sec.ceilingpic<99){
            // Between the F2_START and F2_END mark (in vanilla)
                sec.ceilingpic+=3;
            } else {
                sec.ceilingpic+=5;
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
protected void ArchiveThinkers () throws IOException
{
    thinker_t      th;
    mobj_t     mobj;

// save off the current thinkers
for (th = A.getThinkerCap().next ; th != A.getThinkerCap(); th=th.next)
{
if (th.function!=null && th.function==think_t.P_MobjThinker)
{
    // Indicate valid thinker
    f.writeByte(thinkerclass_t.tc_mobj.ordinal());
    // Pad...
    PADSAVEP(f);
    mobj=(mobj_t)th;
    mobj.write(f);
   
    // MAES: state is explicit in state.id
   // save_p += sizeof(*mobj);
   // mobj->state = (state_t *)(mobj->state - states);
    
    // MAES: player is automatically generated at runtime and handled by the writer.
    //if (mobj->player)
    //mobj->player = (player_t *)((mobj->player-players) + 1);
    continue;
}
    
// I_Error ("P_ArchiveThinkers: Unknown thinker function");
}

// add a terminating marker
f.writeByte(thinkerclass_t.tc_end.ordinal());

}

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
       // That's how we know when to stop.
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
        
       // System.out.printf("Thinker with id %d has prev %x next %x\n",th.id,th.previd,th.nextid);
        if (th.player!=null){
       // System.out.printf("Thinker with id %d is player %d with mobj pointer %x\n",th.id,th.player.identify(),th.player.p_mobj);
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
    // We subtract -1 so it matches that inside the thinkers list.
    for (int i=(player-1);i<TL.size()-1;i++){
        // Get "next" pointer.
        curr=TL.get(i).nextid;
        pointindex.put(curr, TL.get(i+1));
    }
    
    // We also search backwards, in case player wasn't first object
    // (can this even happen, in vanilla?)
    // -1 so it matches that of the TL list.
    for (int i=(player-1);i>0;i--){
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
           // System.out.printf("Object %s has target %s\n",th.type.toString(),th.target.type.toString());
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
//P_UrchiveSpecials
//
protected void ArchiveSpecials () throws IOException
{
  specials_e        tclass;
ceiling_t      ceiling;
vldoor_t       door;
floormove_t    floor;
plat_t     plat;
lightflash_t   flash;
strobe_t       strobe;
glow_t     glow;
int i;

// Try estimating the number of thinkers. It doesn't have to be exact, just
// getting an upper bound will be OK.
int estimate=0;
for (thinker_t th = A.getThinkerCap().next ; th != A.getThinkerCap() ; th=th.next)
    estimate++;

// Most of these objects are quite hefty, but estimating 128 bytes tops
// for each should do (largest one is 56);
ByteBuffer buffer=ByteBuffer.allocate(128);
buffer.order(ByteOrder.LITTLE_ENDIAN);

// save off the current thinkers
for (thinker_t th = A.getThinkerCap().next ; th != A.getThinkerCap() ; th=th.next){
    
    // Write out any pending objects.
    if (buffer.position()>0){
        f.write(buffer.array(),0,buffer.position());
        //System.out.println("Wrote out "+buffer.position()+" bytes");
            
        }
    
    // Back to the beginning.
    buffer.position(0);
    
    // So ceilings don't think?
    if (th.function== null) {
        // i maintains status between iterations
        for (i = 0; i < MAXCEILINGS;i++)
        if ((th instanceof ceiling_t) && (A.getActiveceilings()[i] == (ceiling_t)th))
            break;
        
        if (i<MAXCEILINGS)
        {
        f.writeByte(specials_e.tc_ceiling.ordinal());
        PADSAVEP(f);        
        // Set id for saving        
        ceiling=(ceiling_t)th;
        ceiling.sectorid=ceiling.sector.id;
        ceiling.pack(buffer);        
        }
        continue;
    }

    // Well, apparently some do.
    if (th.function== think_t.T_MoveCeiling) {
        
        f.writeByte(specials_e.tc_ceiling.ordinal());
        PADSAVEP(f);        
        ceiling=(ceiling_t)th;
        ceiling.sectorid=ceiling.sector.id;
        ceiling.pack(buffer);
        continue;
        }
   
    // Well, apparently some do.
    if (th.function== think_t.T_VerticalDoor) {

        f.writeByte(specials_e.tc_door.ordinal());
        PADSAVEP(f);
        door=(vldoor_t)th;
        door.sectorid=door.sector.id;
        door.pack(buffer);
        continue;
        }
    
    // Well, apparently some do.
    if (th.function== think_t.T_MoveFloor) {
        f.writeByte(specials_e.tc_floor.ordinal());
        PADSAVEP(f);
        floor=(floormove_t)th;
        floor.sectorid=floor.sector.id;
        floor.pack(buffer);
        continue;
        }
    
    // Well, apparently some do.
    if (th.function== think_t.T_PlatRaise) {
        f.writeByte(specials_e.tc_plat.ordinal());
        PADSAVEP(f);
        plat=(plat_t)th;
        plat.sectorid=plat.sector.id;
        plat.pack(buffer);
        continue;
        }

    // Well, apparently some do.
    if (th.function== think_t.T_LightFlash) {
        f.writeByte(specials_e.tc_flash.ordinal());
        PADSAVEP(f);
        flash=(lightflash_t)th;
        flash.sectorid=flash.sector.id;
        flash.pack(buffer);
        continue;
        }

    // Well, apparently some do.
    if (th.function== think_t.T_StrobeFlash) {
        f.writeByte(specials_e.tc_strobe.ordinal());
        PADSAVEP(f);
        strobe=(strobe_t)th;
        strobe.sectorid=strobe.sector.id;
        strobe.pack(buffer);
        continue;
        }
    
    // Well, apparently some do.
    if (th.function== think_t.T_Glow) {
        f.writeByte(specials_e.tc_glow.ordinal());
        PADSAVEP(f);
        glow=(glow_t)th;
        glow.sectorid=glow.sector.id;
        glow.pack(buffer);
        continue;
        }
    
}

if (buffer.position()>0){
    f.write(buffer.array(),0,buffer.position());
    //System.out.println("Wrote out "+buffer.position()+" bytes");
        
    }


// Finito!
f.writeByte((byte) specials_e.tc_endspecials.ordinal());
}


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
 
 A.ClearPlatsBeforeLoading();
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

     if (ceiling.function!=null)
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

     if (plat.functionid!=0)
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
    
    //protected final int PADSAVEP(ByteBuffer b, int save_p){
    //    ByteBuffer
    //    return (save_p += (4 - ((int) save_p & 3)) & 3);
    //}

    protected final long PADSAVEP(DoomFile f) throws IOException{
        long save_p=f.getFilePointer();
        int padding =(4 - ((int) save_p & 3)) & 3;
        //System.out.printf("Current position %d Padding by %d bytes\n",save_p,padding);        
        f.seek(save_p+padding);
        return padding;        
        }

    @Override
    public void updateStatus(DoomStatus DS) {
        this.DS=DS;
        this.LL=DS.LL;
        this.A=DS.P;       
    	}


    @Override
    public boolean doSave(DoomFile f) {
            try {
             // The header must have been set, at this point.
                this.f=f;
                f.setLength(0); // Kill old info.
                header.write(f);
            
            
            //header.read(f);
            ArchivePlayers ();
            ArchiveWorld();
            ArchiveThinkers();  
            ArchiveSpecials();
            // TODO: the rest...
            f.write(0x1D);
            } catch (Exception e){
                e.printStackTrace();
                System.err.printf("Error while saving savegame! Cause: %s",e.getMessage());
                return false; // Needed to shut up compiler.
            }
        return true;
    }

}