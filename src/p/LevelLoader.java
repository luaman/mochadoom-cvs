package p;

import static data.Defines.MAPBLOCKSHIFT;
import static data.Defines.ML_BLOCKMAP;
import static data.Defines.ML_LINEDEFS;
import static data.Defines.ML_NODES;
import static data.Defines.ML_SECTORS;
import static data.Defines.ML_SEGS;
import static data.Defines.ML_SIDEDEFS;
import static data.Defines.ML_SSECTORS;
import static data.Defines.ML_THINGS;
import static data.Defines.ML_TWOSIDED;
import static data.Defines.ML_VERTEXES;
import static data.Defines.PU_LEVEL;
import static data.Defines.PU_STATIC;
import static data.Limits.MAXPLAYERS;
import static data.Limits.MAXRADIUS;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FixedDiv;
import i.system;

import java.io.IOException;
import java.nio.ByteOrder;

import m.BBox;
import rr.Renderer;
import rr.line_t;
import rr.node_t;
import rr.sector_t;
import rr.seg_t;
import rr.side_t;
import rr.subsector_t;
import rr.vertex_t;
import st.DoomStatusBarInterface;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import w.DoomBuffer;
import w.WadLoader;
import data.doomstat;
import data.maplinedef_t;
import data.mapnode_t;
import data.mapsector_t;
import data.mapseg_t;
import data.mapsidedef_t;
import data.mapsubsector_t;
import data.mapthing_t;
import data.mapvertex_t;
import data.Defines.GameMode_t;
import data.Defines.skill_t;
import data.Defines.slopetype_t;
import doom.DoomContext;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: LevelLoader.java,v 1.2 2010/09/02 15:56:54 velktron Exp $
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
// $Log: LevelLoader.java,v $
// Revision 1.2  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.1  2010/09/01 15:53:42  velktron
// Graphics data loader implemented....still need to figure out how column caching works, though.
//
// Revision 1.4  2010/08/19 23:14:49  velktron
// Automap
//
// Revision 1.3  2010/08/13 14:06:36  velktron
// Endlevel screen fully functional!
//
// Revision 1.2  2010/08/11 16:31:34  velktron
// Map loading works! Check out LevelLoaderTester for more.
//
// Revision 1.1  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
//
// DESCRIPTION:
//  Do all the WAD I/O, get map description,
//  set up initial state and misc. LUTs.
//
//-----------------------------------------------------------------------------

public class LevelLoader {

/////////////////// Status objects ///////////////////
    
    DoomStatusBarInterface ST;
    WadLoader W;
    doomstat DS;
    DoomVideoRenderer V;
    Renderer R;

  public static final String  rcsid = "$Id: LevelLoader.java,v 1.2 2010/09/02 15:56:54 velktron Exp $";

/*
  #include <math.h>

  #include "z_zone.h"

  #include "m_swap.h"
  #include "m_bbox.h"

  #include "g_game.h"

  #include "i_system.h"
  #include "w_wad.h"

  #include "doomdef.h"
  #include "p_local.h"

  #include "s_sound.h"

  #include "doomstat.h"
*/

  public void    SpawnMapThing (mapthing_t    mthing){}


  //
  // MAP related Lookup tables.
  // Store VERTEXES, LINEDEFS, SIDEDEFS, etc.
  //
  public int     numvertexes;
  public vertex_t[]   vertexes;

  public int     numsegs;
  public seg_t[]      segs;

  public int     numsectors;
  public sector_t[]   sectors;

  public int     numsubsectors;
  public subsector_t[]    subsectors;

  public int     numnodes;
  public node_t[]     nodes;

  public int     numlines;
  public line_t[]     lines;

  public int     numsides;
  public side_t[]     sides;


  // BLOCKMAP
  // Created from axis aligned bounding box
  // of the map, a rectangular array of
  // blocks of size ...
  // Used to speed up collision detection
  // by spatial subdivision in 2D.
  //
  // Blockmap size.
  int     bmapwidth;
  int     bmapheight; // size in mapblocks
  short[]      blockmap;   // int for larger maps
  // offsets in blockmap are from here
  short[]      blockmaplump;       
  /** (fixed_t) origin of block map */
  public int     bmaporgx;
public int bmaporgy;
  /** for thing chains */
  mobj_t[]    blocklinks;     


  // REJECT
  // For fast sight rejection.
  // Speeds up enemy AI by skipping detailed
  //  LineOf Sight calculation.
  // Without special effect, this could be
  //  used as a PVS lookup as well.
  //
  byte[]       rejectmatrix;


  // Maintain single and multi player starting spots.
  protected static final int MAX_DEATHMATCH_STARTS  = 10;

  mapthing_t[]  deathmatchstarts=new mapthing_t[MAX_DEATHMATCH_STARTS];
  //mapthing_t* deathmatch_p;
  int deathmatch_p;
  mapthing_t[]  playerstarts=new mapthing_t[MAXPLAYERS];





  /**
  * P_LoadVertexes
 * @throws IOException 
  */
  public void LoadVertexes (int lump) throws IOException
  {
      // Make a lame-ass attempt at loading some vertexes.
      
      // Determine number of lumps:
      //  total lump length / vertex record length.
      numvertexes = W.LumpLength (lump) / mapvertex_t.sizeOf();

      // Allocate zone memory for buffer.
      vertexes = new vertex_t[numvertexes];
      // Init those "vertexes"
      C2JUtils.initArrayOfObjects(vertexes, vertex_t.class);

      // Load data into cache.
      // MAES: we now have a mismatch between memory/disk: in memory, we need an array.
      // On disk, we have a single lump/blob. Thus, we need to find a way to deserialize this...
       W.CacheLumpNumIntoArray(lump,PU_STATIC,vertexes,vertex_t.class);
      
     // Copy and convert vertex coordinates,
    // MAES: not needed. Intermentidate mapvertex_t struct skipped.
  }



  /**
  * P_LoadSegs
 * @throws IOException 
  */
  public void LoadSegs (int lump) throws IOException
  {
   
      mapseg_t[] data;
      mapseg_t       ml;
      seg_t li;
      line_t     ldef;
      int         linedef;
      int         side;
      
      // Another disparity between disk/memory. Treat it the same as VERTEXES.
      numsegs = W.LumpLength (lump) / mapseg_t.sizeOf();
      segs = new seg_t[numsegs];
      C2JUtils.initArrayOfObjects(segs, seg_t.class);
      data = new mapseg_t[numsegs];
      C2JUtils.initArrayOfObjects(data, mapseg_t.class);
      
      // Read "mapsegs". 
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data,mapseg_t.class);
      

      // We're not done yet!
      for (int i=0 ; i<numsegs ; i++)
      {
          li=segs[i];
          ml=data[i];
      li.v1 = vertexes[ml.v1];
      li.v2 = vertexes[ml.v2];
                      
      li.angle = (ml.angle)<<16;
      li.offset = (ml.offset)<<16;
      linedef = ml.linedef;
      li.linedef = ldef= lines[linedef];
      side = ml.side;
      li.sidedef = sides[ldef.sidenum[side]];
      li.frontsector = sides[ldef.sidenum[side]].sector;
      if ((ldef. flags & ML_TWOSIDED)!=0)
          li.backsector = sides[ldef.sidenum[side^1]].sector;
      else
          li.backsector = null;
      }
      
  }


  /**
  * P_LoadSubsectors
 * @throws IOException 
  */
  public void LoadSubsectors (int lump) throws IOException
  {
      mapsubsector_t ms;
      subsector_t    ss;
      mapsubsector_t[] data;
      
      numsubsectors = W.LumpLength (lump) / mapsubsector_t.sizeOf();      
      subsectors = new subsector_t[numsubsectors];
      C2JUtils.initArrayOfObjects(subsectors, subsector_t.class);
            
      data= new mapsubsector_t[numsubsectors];
      C2JUtils.initArrayOfObjects(data, mapsubsector_t.class);
      
      // Read "mapsubsectors"
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data, mapsubsector_t.class);
      
      for (int i=0 ; i<numsubsectors ; i++)
      {
          ms=data[i];
          ss=subsectors[i];
      ss.numlines = ms.numsegs;
      ss.firstline = ms.firstseg;
      }

  }
  
  /**
   * P_LoadSectors
 * @throws IOException 
   */
  public void LoadSectors (int lump) throws IOException
  {
      mapsector_t[]       data;
      mapsector_t    ms;
      sector_t       ss;
      
      numsectors = W.LumpLength (lump) / mapsector_t.sizeOf();
      sectors = new sector_t[numsectors];
      C2JUtils.initArrayOfObjects(sectors, sector_t.class);
      
      data=new mapsector_t[numsectors];
      C2JUtils.initArrayOfObjects(data, mapsector_t.class);
      
      // Read "mapsectors"
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data,mapsector_t.class);
      

      for (int i=0 ; i<numsectors ; i++)
      {
          ms = data[i];
          ss = sectors[i];
      ss.floorheight = ms.floorheight<<FRACBITS;
      ss.ceilingheight = ms.ceilingheight<<FRACBITS;
     //TODO: ss.floorpic = (short) R.FlatNumForName(ms.floorpic);
     //TODO: ss.ceilingpic = (short) R.FlatNumForName(ms.ceilingpic);
      ss.lightlevel = ms.lightlevel;
      ss.special = ms.special;
      ss.tag = ms.tag;
      ss.thinglist = null;
      }

  }


  /**
  * P_LoadNodes
 * @throws IOException 
  */
  
  public void LoadNodes (int lump) throws IOException
  {
      mapnode_t[]   data;
      int     i;
      int     j;
      int     k;
      mapnode_t  mn;
      node_t no;
      
      numnodes = W.LumpLength (lump) / mapnode_t.sizeOf();
      nodes = new node_t[numnodes];
      C2JUtils.initArrayOfObjects(nodes, node_t.class);
      data = new mapnode_t[numnodes];  
      C2JUtils.initArrayOfObjects(data, mapnode_t.class);
      // Read "mapnodes"
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data,mapnode_t.class);
      
      
      for (i=0 ; i<numnodes ; i++)
      {
          mn=data[i];
          no=nodes[i];
      no.x = mn.x<<FRACBITS;
      no.y = mn.y<<FRACBITS;
      no.dx = mn.dx<<FRACBITS;
      no.dy = mn.dy<<FRACBITS;
      for (j=0 ; j<2 ; j++)
      {
          no.children[j] = mn.children[j];
          for (k=0 ; k<4 ; k++)
          no.bbox[j].set(k, mn.bbox[j][k]<<FRACBITS);
      }
      }
      
  }


  /**
   * P_LoadThings
 * @throws IOException 
   */
  public void LoadThings (int lump) throws IOException
  {
      mapthing_t[]       data;
      mapthing_t     mt;
      int         numthings;
      boolean     spawn;      
      
      numthings = W.LumpLength (lump) / mapthing_t.sizeOf();
      data=new mapthing_t[numthings];
      C2JUtils.initArrayOfObjects(data, mapthing_t.class);
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data,mapthing_t.class);
      
      
      
      for (int i=0 ; i<numthings ; i++)
      {
          mt = data[i];
      spawn = true;

      // Do not spawn cool, new monsters if !commercial
      if ( DS.gamemode != GameMode_t.commercial)
      {
          switch(mt.type)
          {
            case 68:  // Arachnotron
            case 64:  // Archvile
            case 88:  // Boss Brain
            case 89:  // Boss Shooter
            case 69:  // Hell Knight
            case 67:  // Mancubus
            case 71:  // Pain Elemental
            case 65:  // Former Human Commando
            case 66:  // Revenant
            case 84:  // Wolf SS
          spawn = false;
          break;
          }
      }
      if (spawn == false)
          break;

      // Do spawn all other stuff.
      // MAES: we have loaded the shit with the proper endianness, so no fucking around, bitch.
      /*mt.x = SHORT(mt.x);
      mt.y = SHORT(mt.y);
      mt.angle = SHORT(mt.angle);
      mt.type = SHORT(mt.type);
      mt.options = SHORT(mt.options);*/
      
      // TODO: P_SpawnMapThing (mt);
      }
      
  }


  /**
   * P_LoadLineDefs
   * Also counts secret lines for intermissions.
 * @throws IOException 
   */
  public void LoadLineDefs (int lump) throws IOException
  {
      maplinedef_t[]       data;
      maplinedef_t   mld;
      line_t     ld;
      vertex_t       v1;
      vertex_t       v2;
      
      numlines = W.LumpLength (lump) / maplinedef_t.sizeOf();
      lines = new line_t[numlines];
      C2JUtils.initArrayOfObjects(lines, line_t.class);
      data = new maplinedef_t[numlines];
      C2JUtils.initArrayOfObjects(data, maplinedef_t.class);

      // read "maplinedefs"
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data,maplinedef_t.class);
      
      for (int i=0 ; i<numlines ; i++)
      {
          mld = data[i];
          ld = lines[i];
    
          
      ld.flags = mld.flags;
      ld.special = mld.special;
      ld.tag = mld.tag;
      v1 = ld.v1 = vertexes[mld.v1];
      v2 = ld.v2 = vertexes[mld.v2];
      ld.dx = v2.x - v1.x;
      ld.dy = v2.y - v1.y;
      
      if (ld.dx==0)
          ld.slopetype = slopetype_t.ST_VERTICAL;
      else if (ld.dy==0)
          ld.slopetype = slopetype_t.ST_HORIZONTAL;
      else
      {
          if (FixedDiv (ld.dy , ld.dx) > 0)
          ld.slopetype = slopetype_t.ST_POSITIVE;
          else
          ld.slopetype = slopetype_t.ST_NEGATIVE;
      }
          
      if (v1.x < v2.x)
      {
          ld.bbox[BOXLEFT] = v1.x;
          ld.bbox[BOXRIGHT] = v2.x;
      }
      else
      {
          ld.bbox[BOXLEFT] = v2.x;
          ld.bbox[BOXRIGHT] = v1.x;
      }

      if (v1.y < v2.y)
      {
          ld.bbox[BOXBOTTOM] = v1.y;
          ld.bbox[BOXTOP] = v2.y;
      }
      else
      {
          ld.bbox[BOXBOTTOM] = v2.y;
          ld.bbox[BOXTOP] = v1.y;
      }

      ld.sidenum[0] = mld.sidenum[0];
      ld.sidenum[1] = mld.sidenum[1];

      if (ld.sidenum[0] != -1)
          ld.frontsector = sides[ld.sidenum[0]].sector;
      else
          ld.frontsector = null;

      if (ld.sidenum[1] != -1)
          ld.backsector = sides[ld.sidenum[1]].sector;
      else
          ld.backsector = null;
      }
  }


  /**
  * P_LoadSideDefs
  */
  public void LoadSideDefs (int lump) throws IOException
  {
      mapsidedef_t[]       data;
      mapsidedef_t   msd;
      side_t     sd;
      
      numsides = W.LumpLength (lump) / mapsidedef_t.sizeOf();
      sides = new side_t[numsides];  
      C2JUtils.initArrayOfObjects(sides, side_t.class);
      data= new mapsidedef_t[numsides];
      C2JUtils.initArrayOfObjects(data, mapsidedef_t.class);
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data,mapsidedef_t.class);
      
      for (int i=0 ; i<numsides ; i++)
      {
          msd = data[i];
          sd = sides[i];
          
      sd.textureoffset = (msd.textureoffset)<<FRACBITS;
      sd.rowoffset = (msd.rowoffset)<<FRACBITS;
      //TODO: sd.toptexture = R.TextureNumForName(msd.toptexture);
      //TODO: sd.bottomtexture = R.TextureNumForName(msd.bottomtexture);
      //TODO: sd.midtexture = R.TextureNumForName(msd.midtexture);
      sd.sector = sectors[msd.sector];
      }
  }


  /**
   * P_LoadBlockMap
 * @throws IOException 
   */
  public void LoadBlockMap (int lump) throws IOException
  {
      int     i;
      int     count;
      
      DoomBuffer data=(DoomBuffer)W.CacheLumpNum(lump,PU_LEVEL, DoomBuffer.class);
      count=W.LumpLength(lump)/2;
      blockmaplump=new short[count];
      blockmap=new short[count-4];
      data.setOrder(ByteOrder.LITTLE_ENDIAN);
      data.rewind();
      data.readShortArray(blockmaplump, count);

      //blockmap = blockmaplump+4;
      // Maes: skips first FOUR shorts?
      for (i=0 ; i<count-4 ; i++){
          blockmap[i]=blockmaplump[i+4];
      }

      for (i=0 ; i<count ; i++)
      // MAES: not needed
      //blockmaplump[i] = blockmaplump[i];
          
      bmaporgx = blockmaplump[0]<<FRACBITS;
      bmaporgy = blockmaplump[1]<<FRACBITS;
      bmapwidth = blockmaplump[2];
      bmapheight = blockmaplump[3];
      
      // clear out mobj chains
      count = bmapwidth*bmapheight;
      blocklinks = new mobj_t[count];
      C2JUtils.initArrayOfObjects(blocklinks, mobj_t.class);
  }



  /**
   * P_GroupLines
   * Builds sector line lists and subsector sector numbers.
   * Finds block bounding boxes for sectors.
  */

  
  public void GroupLines ()
  {
      int         total;
      line_t     li;
      sector_t       sector;
      subsector_t    ss;
      seg_t      seg;
      int[]     bbox=new int[4];
      int         block;
      
      // look up sector number for each subsector
      
      for (int i=0 ; i<numsubsectors ; i++)
      {
      ss = subsectors[i];
      seg = segs[ss.firstline];
      ss.sector = seg.sidedef.sector;
      }

      //linebuffer=new line_t[numsectors][0];
      // count number of lines in each sector
      
      
      total = 0;

      for (int i=0 ; i<numlines ; i++)
      {
      li = lines[i];
      total++;
      li.frontsector.linecount++;

      if ((li.backsector!=null) && (li.backsector != li.frontsector))
      {
          li.backsector.linecount++;
          total++;
      }
      
      
      }
      
      // build line tables for each sector    
      // MAES: we don't really need this in Java.
      // linebuffer = new line_t[total];
      // int linebuffercount=0;
      
      // We scan through ALL sectors.
      for (int i=0 ; i<numsectors ; i++)
      {
          sector = sectors[i];
          BBox.ClearBox(bbox);
          //sector->lines = linebuffer;
          // We can just construct line tables of the correct size
          // for each sector.
          int countlines=0;
      // We scan through ALL lines....
          
         // System.out.println(i+ ": looking for sector -> "+sector);
      for (int j=0 ; j<numlines ; j++)
      {
          li=lines[j];
          
          //System.out.println(j+ " front "+li.frontsector+ " back "+li.backsector);
          
          if (li.frontsector == sector || li.backsector == sector)
          {
              // This sector will have one more line.
              countlines++;
          // Expand bounding box...
          BBox.AddToBox(bbox, li.v1.x, li.v1.y);
          BBox.AddToBox (bbox, li.v2.x, li.v2.y);
          }
      }
      
      // So, this sector must have that many lines.
      sector.lines=new line_t[countlines];
      int pointline=0;

      int addedlines=0;
      
      // Add actual lines into sectors.
      for (int j=0 ; j<numlines ; j++)
      {
          li=lines[j];
          // If
          if (li.frontsector == sector || li.backsector == sector)
          {
              // This sector will have one more line.
              sectors[i].lines[pointline++]=lines[j];
              addedlines++;
          }
      }
      
      if (addedlines != sector.linecount)
          system.Error ("P_GroupLines: miscounted");
              
      // set the degenmobj_t to the middle of the bounding box
      sector.soundorg.x = (bbox[BOXRIGHT]+bbox[BOXLEFT])/2;
      sector.soundorg.y = (bbox[BOXTOP]+bbox[BOXBOTTOM])/2;
          
      // adjust bounding box to map blocks
      block = (bbox[BOXTOP]-bmaporgy+MAXRADIUS)>>MAPBLOCKSHIFT;
      block = block >= bmapheight ? bmapheight-1 : block;
      sector.blockbox[BOXTOP]=block;

      block = (bbox[BOXBOTTOM]-bmaporgy-MAXRADIUS)>>MAPBLOCKSHIFT;
      block = block < 0 ? 0 : block;
      sector.blockbox[BOXBOTTOM]=block;

      block = (bbox[BOXRIGHT]-bmaporgx+MAXRADIUS)>>MAPBLOCKSHIFT;
      block = block >= bmapwidth ? bmapwidth-1 : block;
      sector.blockbox[BOXRIGHT]=block;

      block = (bbox[BOXLEFT]-bmaporgx-MAXRADIUS)>>MAPBLOCKSHIFT;
      block = block < 0 ? 0 : block;
      sector.blockbox[BOXLEFT]=block;
      }
      
  }


  /**
   * P_SetupLevel
 * @throws Exception 
   */
  public void
  SetupLevel
  ( int       episode,
    int       map,
    int       playermask,
    skill_t   skill) throws Exception
  {
      int     i;
      String    lumpname;
      int     lumpnum;
      
      DS.totalkills = DS.totalitems = DS.totalsecret = DS.wminfo.maxfrags = 0;
      DS.wminfo.partime = 180;
      for (i=0 ; i<MAXPLAYERS ; i++)
      {
      DS.players[i].killcount = DS.players[i].secretcount 
          = DS.players[i].itemcount = 0;
      }

      // Initial height of PointOfView
      // will be set by player think.
      DS.players[DS.consoleplayer].viewz = 1; 

      // Make sure all sounds are stopped before Z_FreeTags.
      //TODO: S_Start ();         

  /*    
  #if 0 // UNUSED
      if (debugfile)
      {
      Z_FreeTags (PU_LEVEL, MAXINT);
      Z_FileDumpHeap (debugfile);
      }
      else
  #endif
  */
    //  Z_FreeTags (PU_LEVEL, PU_PURGELEVEL-1);


      // UNUSED W_Profile ();
      //TODO: P_InitThinkers ();

      // if working with a devlopment map, reload it
      W.Reload ();            
         
      // find map name
      if ( DS.gamemode == GameMode_t.commercial)
      {
      if (map<10)
          lumpname="map0"+map;
      else
          lumpname="map"+map;
      }
      else
      {
      lumpname = ("E"+
     (char)( '0' + episode)+
     "M"+
      (char)( '0' + map)
      );
      }

      lumpnum = W.GetNumForName (lumpname);
      
      DS.leveltime = 0;
      
      // note: most of this ordering is important 
      this.LoadBlockMap (lumpnum+ML_BLOCKMAP);
      this.LoadVertexes (lumpnum+ML_VERTEXES);
      this.LoadSectors (lumpnum+ML_SECTORS);
      this.LoadSideDefs (lumpnum+ML_SIDEDEFS);
      this.LoadLineDefs (lumpnum+ML_LINEDEFS);
      this.LoadSubsectors (lumpnum+ML_SSECTORS);
      this.LoadNodes (lumpnum+ML_NODES);
      this.LoadSegs (lumpnum+ML_SEGS);
      
      //rejectmatrix = W.CacheLumpNum (lumpnum+ML_REJECT,PU_LEVEL);
      this.GroupLines ();

      DS.bodyqueslot = 0;
      //TODO: deathmatch_p = deathmatchstarts;
      this.LoadThings (lumpnum+ML_THINGS);
      
      // if deathmatch, randomly spawn the active players
      if (DS.deathmatch)
      {
      for (i=0 ; i<MAXPLAYERS ; i++)
          if (DS.playeringame[i])
          {
          DS.players[i].mo = null;
          //TODO: G_DeathMatchSpawnPlayer (i);
          }
              
      }

      // clear special respawning que
      //TODO: iquehead = iquetail = 0;        
      
      // set up world state
      //TODO: P_SpawnSpecials ();
      
      // build subsector connect matrix
      //  UNUSED P_ConnectSubsectors ();

      // preload graphics
      if (DS.precache){
      //TODO: R_PrecacheLevel ();
      }
      //printf ("free memory: 0x%x\n", Z_FreeMemory());

  }



  //
  // P_Init
  //
  public void P_Init ()
  {
      //TODO:
      /*InitSwitchList ();
      InitPicAnims ();
      InitSprites (sprnames);*/
  }

  public LevelLoader(DoomContext DC){
      this.W=DC.W;
      this.DS=DC.DS;
   
  }


}