package rr;

import static data.Limits.MAXINT;
import static data.Limits.MAX_ADJOINING_SECTORS;
import static m.fixed_t.FRACUNIT;
import static p.DoorDefines.*;

import java.io.IOException;
import java.nio.ByteOrder;

import m.fixed_t;
import m.random;
import data.Defines;
import data.Tables;
import doom.think_t;
import doom.thinker_t;
import p.ThinkerList;
import p.fireflicker_t;
import p.glow_t;
import p.lightflash_t;
import p.mobj_t;
import p.strobe_t;
import p.vldoor_e;
import p.vldoor_t;
import w.DoomFile;
import w.IReadableDoomObject;

/**
 * The SECTORS record, at runtime.
 * Stores things/mobjs. Can be archived/unarchived during
 * savegames.
 * @author admin
 *
 */
public class sector_t implements IReadableDoomObject{

    
    public ThinkerList TL;
    public random RND;
    
    public sector_t(){
        soundorg =new degenmobj_t();
        blockbox=new int[4];
    }
    
        /** (fixed_t) */
     public int floorheight, ceilingheight;
     public short   floorpic;
     public short   ceilingpic;
     public short   lightlevel;
     public short   special;
     public short   tag;

     /** 0 = untraversed, 1,2 = sndlines -1 */
     public int     soundtraversed;

     /** thing that made a sound (or null) (MAES: single pointer) */
     public mobj_t soundtarget;

     /** mapblock bounding box for height changes */
     public int[]  blockbox;

     /** origin for any sounds played by the sector */
     public degenmobj_t soundorg;

     /** if == validcount, already checked */
     public int     validcount;

     /** list of mobjs in sector (MAES: it's used as a linked list) */
     public mobj_t thinglist;

     /** thinker_t for reversable actions. This actually was a void*, 
      *  and in practice it could store doors, plats, floors and ceiling objects. */
     public SectorAction specialdata;

     public int         linecount;
     //struct line_s** lines;  // [linecount] size
     // MAES: make this line_t[] for now?
     public line_t[] lines;
     
     /** Use for internal identification */
     public int id;     
     
     public String toString(){
         sb.setLength(0);
         sb.append("Sector id: ");
         sb.append(id);
         sb.append('\t');
         sb.append("FloorHeight: ");
         sb.append(Integer.toHexString(this.floorheight));
         sb.append('\t');
         sb.append("Ceilingheight: ");
         sb.append(Integer.toHexString(this.ceilingheight));
         sb.append('\t');
         sb.append("Floorpic: ");
         sb.append(this.floorpic);
         sb.append('\t');
         sb.append("Ceilingpic: ");
         sb.append(this.ceilingpic);
         return sb.toString();
         
     }

     //
     // Find minimum light from an adjacent sector
     //
     public int FindMinSurroundingLight(int max) {
         int i;
         int min;
         line_t line;
         sector_t check;

         min = max;
         for (i = 0; i < this.linecount; i++) {
             line = this.lines[i];
             check = line.getNextSector( this);

             if (check == null)
                 continue;

             if (check.lightlevel < min)
                 min = check.lightlevel;
         }
         return min;
     }


     //
     // P_FindLowestFloorSurrounding()
     // FIND LOWEST FLOOR HEIGHT IN SURROUNDING SECTORS
     //
     public int FindLowestFloorSurrounding() {
         int i;
         line_t check;
         sector_t other;
         int floor = this.floorheight;

         for (i = 0; i < this.linecount; i++) {
             check = this.lines[i];
             other = check.getNextSector(this);

             if (other == null)
                 continue;

             if (other.floorheight < floor)
                 floor = other.floorheight;
         }
         return floor;
     }

     /**
      * P_FindHighestFloorSurrounding() FIND HIGHEST FLOOR HEIGHT IN SURROUNDING
      * SECTORS
      * 
      * @param sec
      */
     public int FindHighestFloorSurrounding() {
         int i;
         line_t check;
         sector_t other;
         int floor = -500 * FRACUNIT;

         for (i = 0; i < this.linecount; i++) {
             check = this.lines[i];
             other = check.getNextSector( this);

             if (other == null)
                 continue;

             if (other.floorheight > floor)
                 floor = other.floorheight;
         }
         return floor;
     }

     /**
      * P_FindNextHighestFloor FIND NEXT HIGHEST FLOOR IN SURROUNDING SECTORS
      * Note: this should be doable w/o a fixed array.
      * 
      * @param sec
      * @param currentheight
      * @return fixed
      */

     public int FindNextHighestFloor(int currentheight) {
         int i;
         int h;
         int min;
         line_t check;
         sector_t other;
         int height = currentheight;

         int heightlist[] = new int[MAX_ADJOINING_SECTORS];

         for (i = 0, h = 0; i < this.linecount; i++) {
             check = this.lines[i];
             other = check.getNextSector( this);

             if (other == null)
                 continue;

             if (other.floorheight > height)
                 heightlist[h++] = other.floorheight;

             // Check for overflow. Exit.
             if (h >= MAX_ADJOINING_SECTORS) {
                 System.err
                         .print("Sector with more than 20 adjoining sectors\n");
                 break;
             }
         }

         // Find lowest height in list
         if (h == 0)
             return currentheight;

         min = heightlist[0];

         // Range checking?
         for (i = 1; i < h; i++)
             if (heightlist[i] < min)
                 min = heightlist[i];

         return min;
     }

     //
     // FIND LOWEST CEILING IN THE SURROUNDING SECTORS
     //
     public int FindLowestCeilingSurrounding() {
         int i;
         line_t check;
         sector_t other;
         int height = MAXINT;

         for (i = 0; i < this.linecount; i++) {
             check = this.lines[i];
             other = check.getNextSector(this);

             if (other == null)
                 continue;

             if (other.ceilingheight < height)
                 height = other.ceilingheight;
         }
         return height;
     }

     //
     // FIND HIGHEST CEILING IN THE SURROUNDING SECTORS
     //
     public int FindHighestCeilingSurrounding() {
         int i;
         line_t check;
         sector_t other;
         int height = 0;

         for (i = 0; i < this.linecount; i++) {
             check = this.lines[i];
             other = check.getNextSector(this);

             if (other == null)
                 continue;

             if (other.ceilingheight > height)
                 height = other.ceilingheight;
         }
         return height;
     }
     
     //
     // P_SpawnFireFlicker
     //
     public void SpawnFireFlicker() {
         fireflicker_t flick;

         // Note that we are resetting sector attributes.
         // Nothing special about it during gameplay.
         this.special = 0;

         flick = new fireflicker_t(RND);

         TL.AddThinker(flick);

         flick.function = think_t.T_FireFlicker;
         flick.sector = this;
         flick.maxlight = this.lightlevel;
         flick.minlight =
         this.FindMinSurroundingLight(this.lightlevel) + 16;
         flick.count = 4;
     }
     
     /**
      * Spawn a door that opens after 5 minutes
      */
     
     public void
     SpawnDoorRaiseIn5Mins
     ( 
       int      secnum )
     {
         vldoor_t   door;
        
         door = new vldoor_t();
         
         TL.AddThinker (door);

         this.specialdata = door;
         this.special = 0;

         door.function = think_t.T_VerticalDoor;
         door.sector = this;
         door.direction = 2;
         door.type = vldoor_e.raiseIn5Mins;
         door.speed = VDOORSPEED;
         door.topheight = this.FindLowestCeilingSurrounding();
         door.topheight -= 4*FRACUNIT;
         door.topwait = VDOORWAIT;
         door.topcountdown = 5 * 60 * 35;
     }
     
     //
     // Spawn a door that closes after 30 seconds
     //
     public void SpawnDoorCloseIn30 ()
     {
         vldoor_t   door;
        
         door = new vldoor_t();

         TL.AddThinker (door);

         this.specialdata = door;
         this.special = 0;

         door.function = think_t.T_VerticalDoor;
         door.sector = this;
         door.direction = 0;
         door.type = vldoor_e.normal;
         door.speed = VDOORSPEED;
         door.topcountdown = 30 * 35;
     }
     
     //
     // P_SpawnStrobeFlash
     // After the map has been loaded, scan each sector
     // for specials that spawn thinkers
     //
     public void SpawnStrobeFlash(int fastOrSlow, int inSync) {
         strobe_t flash;

         flash = new strobe_t();

         TL.AddThinker(flash);

         flash.sector = this;
         flash.darktime = fastOrSlow;
         flash.brighttime = STROBEBRIGHT;
         flash.function = think_t.T_StrobeFlash;
         flash.maxlight = this.lightlevel;
         flash.minlight = this.FindMinSurroundingLight(this.lightlevel);

         if (flash.minlight == flash.maxlight)
             flash.minlight = 0;

         // nothing special about it during gameplay
         this.special = 0;

         if (inSync == 0)
             flash.count = (RND.P_Random() & 7) + 1;
         else
             flash.count = 1;
     }
     
     /**
      * P_SpawnLightFlash
      * After the map has been loaded, scan each sector
      * for specials that spawn thinkers
      */
     
     public void SpawnLightFlash() {
         lightflash_t flash;

         // nothing special about it during gameplay
         special = 0;

         flash = new lightflash_t(RND);

         TL.AddThinker((thinker_t)flash);

         flash.function = think_t.T_LightFlash;
         flash.sector = this;
         flash.maxlight = lightlevel;

         flash.minlight = FindMinSurroundingLight( lightlevel);
         flash.maxtime = 64;
         flash.mintime = 7;
         flash.count = (RND.P_Random() & flash.maxtime) + 1;
     }
     
     public void SpawnGlowingLight() {
         glow_t g;

         g = new glow_t();

         TL.AddThinker(g);

         g.sector = this;
         g.minlight = FindMinSurroundingLight(this.lightlevel);
         g.maxlight = lightlevel;
         g.function = think_t.T_Glow;
         g.direction = -1;

         this.special = 0;
     }
     
     
     
     static StringBuilder sb=new StringBuilder();

    @Override
    public void read(DoomFile f)
            throws IOException {
    	
    	// ACHTUNG: the only situation where we'd
    	// like to read memory-format sector_t's is from
    	// savegames, and in vanilla savegames, not all info
    	// is saved (or read) from disk.          
    	
        this.floorheight = f.readLEShort() << m.fixed_t.FRACBITS;
        this.ceilingheight = f.readLEShort() << m.fixed_t.FRACBITS;
        this.floorpic = f.readLEShort();
        this.ceilingpic = f.readLEShort();
        this.lightlevel = f.readLEShort();
        this.special = f.readLEShort();      // needed?
        this.tag =f.readLEShort();      // needed?
        this.soundtraversed=f.readLEShort();
    }
     
 }
