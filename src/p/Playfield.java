package p;

import static data.Defines.*;
import static data.doomtype.*;
import static m.fixed_t.*;

import java.nio.ByteBuffer;

import data.doomstat;
import static data.doomdata.*;
import data.mapsector_t;
import data.mapseg_t;
import data.mapsubsector_t;
import data.mapthing_t;
import data.mapvertex_t;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;
import rr.Renderer;
import rr.line_t;
import rr.node_t;
import rr.patch_t;
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
import m.BBox;
import m.FixedFloat;
import m.cheatseq_t;
import m.fixed_t;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Playfield.java,v 1.1 2010/08/10 16:41:57 velktron Exp $
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
// $Log: Playfield.java,v $
// Revision 1.1  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
//
// DESCRIPTION:
//  Do all the WAD I/O, get map description,
//  set up initial state and misc. LUTs.
//
//-----------------------------------------------------------------------------

public class Playfield {

/////////////////// Status objects ///////////////////
    
    DoomStatusBarInterface ST;
    WadLoader W;
    doomstat DS;
    DoomVideoRenderer V;
    Renderer R;

  public static final String  rcsid = "$Id: Playfield.java,v 1.1 2010/08/10 16:41:57 velktron Exp $";

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
  int     numvertexes;
  vertex_t[]   vertexes;

  int     numsegs;
  seg_t[]      segs;

  int     numsectors;
  sector_t[]   sectors;

  int     numsubsectors;
  subsector_t[]    subsectors;

  int     numnodes;
  node_t[]     nodes;

  int     numlines;
  line_t[]     lines;

  int     numsides;
  side_t[]     sides;


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
  fixed_t     bmaporgx,  bmaporgy;
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
  */
  public void LoadVertexes (int lump)
  {
      // Make a lame-ass attempt at loading some vertexes.
      
      // Determine number of lumps:
      //  total lump length / vertex record length.
      int numvertexes = W.LumpLength (lump) / mapvertex_t.sizeOf();

      // Allocate zone memory for buffer.
      vertex_t[] vertexes = new vertex_t[numvertexes];
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
  */
  public void LoadSegs (int lump)
  {
   
      mapseg_t[] msegs;
      mapseg_t       ml;
      seg_t li;
      line_t     ldef;
      int         linedef;
      int         side;
      
      // Another disparity between disk/memory. Treat it the same as VERTEXES.
      numsegs = W.LumpLength (lump) / mapseg_t.sizeOf();
      segs = new seg_t[numsegs];
      msegs = new mapseg_t[numsegs];
      
      // Read "mapsegs". 
      W.CacheLumpNumIntoArray(lump,PU_STATIC,msegs,mapseg_t.class);
      

      // We're not done yet!
      for (int i=0 ; i<numsegs ; i++)
      {
          li=segs[i];
          ml=msegs[i];
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
  */
  public void LoadSubsectors (int lump)
  {
      mapsubsector_t ms;
      subsector_t    ss;
      mapsubsector_t[] data;
      
      numsubsectors = W.LumpLength (lump) / mapsubsector_t.sizeOf();
      
      subsectors = new subsector_t[numsubsectors];
      data= new mapsubsector_t[numsubsectors];
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
   */
  public void LoadSectors (int lump)
  {
      mapsector_t[]       data;
      mapsector_t    ms;
      sector_t       ss;
      
      numsectors = W.LumpLength (lump) / mapsector_t.sizeOf();
      sectors = new sector_t[numsectors];    
      data=new mapsector_t[numsectors];
      W.CacheLumpNumIntoArray(lump,PU_STATIC,data,mapsector_t.class);
      

      for (int i=0 ; i<numsectors ; i++)
      {
          ms = data[i];
          ss = sectors[i];
      ss.floorheight = ms.floorheight<<FRACBITS;
      ss.ceilingheight = ms.ceilingheight<<FRACBITS;
      ss.floorpic = (short) R.FlatNumForName(ms.floorpic);
      ss.ceilingpic = (short) R.FlatNumForName(ms.ceilingpic);
      ss.lightlevel = ms.lightlevel;
      ss.special = ms.special;
      ss.tag = ms.tag;
      ss.thinglist = null;
      }

  }


  //
  // P_LoadNodes
  //
  /*
  void P_LoadNodes (int lump)
  {
      byte*   data;
      int     i;
      int     j;
      int     k;
      mapnode_t*  mn;
      node_t* no;
      
      numnodes = W_LumpLength (lump) / sizeof(mapnode_t);
      nodes = Z_Malloc (numnodes*sizeof(node_t),PU_LEVEL,0);  
      data = W_CacheLumpNum (lump,PU_STATIC);
      
      mn = (mapnode_t *)data;
      no = nodes;
      
      for (i=0 ; i<numnodes ; i++, no++, mn++)
      {
      no.x = SHORT(mn.x)<<FRACBITS;
      no.y = SHORT(mn.y)<<FRACBITS;
      no.dx = SHORT(mn.dx)<<FRACBITS;
      no.dy = SHORT(mn.dy)<<FRACBITS;
      for (j=0 ; j<2 ; j++)
      {
          no.children[j] = SHORT(mn.children[j]);
          for (k=0 ; k<4 ; k++)
          no.bbox[j][k] = SHORT(mn.bbox[j][k])<<FRACBITS;
      }
      }
      
      Z_Free (data);
  }


  //
  // P_LoadThings
  //
  void P_LoadThings (int lump)
  {
      byte*       data;
      int         i;
      mapthing_t*     mt;
      int         numthings;
      boolean     spawn;
      
      data = W_CacheLumpNum (lump,PU_STATIC);
      numthings = W_LumpLength (lump) / sizeof(mapthing_t);
      
      mt = (mapthing_t *)data;
      for (i=0 ; i<numthings ; i++, mt++)
      {
      spawn = true;

      // Do not spawn cool, new monsters if !commercial
      if ( gamemode != commercial)
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
      mt.x = SHORT(mt.x);
      mt.y = SHORT(mt.y);
      mt.angle = SHORT(mt.angle);
      mt.type = SHORT(mt.type);
      mt.options = SHORT(mt.options);
      
      P_SpawnMapThing (mt);
      }
      
      Z_Free (data);
  }


  //
  // P_LoadLineDefs
  // Also counts secret lines for intermissions.
  //
  void P_LoadLineDefs (int lump)
  {
      byte*       data;
      int         i;
      maplinedef_t*   mld;
      line_t*     ld;
      vertex_t*       v1;
      vertex_t*       v2;
      
      numlines = W_LumpLength (lump) / sizeof(maplinedef_t);
      lines = Z_Malloc (numlines*sizeof(line_t),PU_LEVEL,0);  
      memset (lines, 0, numlines*sizeof(line_t));
      data = W_CacheLumpNum (lump,PU_STATIC);
      
      mld = (maplinedef_t *)data;
      ld = lines;
      for (i=0 ; i<numlines ; i++, mld++, ld++)
      {
      ld.flags = SHORT(mld.flags);
      ld.special = SHORT(mld.special);
      ld.tag = SHORT(mld.tag);
      v1 = ld.v1 = &vertexes[SHORT(mld.v1)];
      v2 = ld.v2 = &vertexes[SHORT(mld.v2)];
      ld.dx = v2.x - v1.x;
      ld.dy = v2.y - v1.y;
      
      if (!ld.dx)
          ld.slopetype = ST_VERTICAL;
      else if (!ld.dy)
          ld.slopetype = ST_HORIZONTAL;
      else
      {
          if (FixedDiv (ld.dy , ld.dx) > 0)
          ld.slopetype = ST_POSITIVE;
          else
          ld.slopetype = ST_NEGATIVE;
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

      ld.sidenum[0] = SHORT(mld.sidenum[0]);
      ld.sidenum[1] = SHORT(mld.sidenum[1]);

      if (ld.sidenum[0] != -1)
          ld.frontsector = sides[ld.sidenum[0]].sector;
      else
          ld.frontsector = 0;

      if (ld.sidenum[1] != -1)
          ld.backsector = sides[ld.sidenum[1]].sector;
      else
          ld.backsector = 0;
      }
      
      Z_Free (data);
  }


  //
  // P_LoadSideDefs
  //
  void P_LoadSideDefs (int lump)
  {
      byte*       data;
      int         i;
      mapsidedef_t*   msd;
      side_t*     sd;
      
      numsides = W_LumpLength (lump) / sizeof(mapsidedef_t);
      sides = Z_Malloc (numsides*sizeof(side_t),PU_LEVEL,0);  
      memset (sides, 0, numsides*sizeof(side_t));
      data = W_CacheLumpNum (lump,PU_STATIC);
      
      msd = (mapsidedef_t *)data;
      sd = sides;
      for (i=0 ; i<numsides ; i++, msd++, sd++)
      {
      sd.textureoffset = SHORT(msd.textureoffset)<<FRACBITS;
      sd.rowoffset = SHORT(msd.rowoffset)<<FRACBITS;
      sd.toptexture = R_TextureNumForName(msd.toptexture);
      sd.bottomtexture = R_TextureNumForName(msd.bottomtexture);
      sd.midtexture = R_TextureNumForName(msd.midtexture);
      sd.sector = &sectors[SHORT(msd.sector)];
      }
      
      Z_Free (data);
  }


  //
  // P_LoadBlockMap
  //
  void P_LoadBlockMap (int lump)
  {
      int     i;
      int     count;
      
      blockmaplump = W_CacheLumpNum (lump,PU_LEVEL);
      blockmap = blockmaplump+4;
      count = W_LumpLength (lump)/2;

      for (i=0 ; i<count ; i++)
      blockmaplump[i] = SHORT(blockmaplump[i]);
          
      bmaporgx = blockmaplump[0]<<FRACBITS;
      bmaporgy = blockmaplump[1]<<FRACBITS;
      bmapwidth = blockmaplump[2];
      bmapheight = blockmaplump[3];
      
      // clear out mobj chains
      count = sizeof(*blocklinks)* bmapwidth*bmapheight;
      blocklinks = Z_Malloc (count,PU_LEVEL, 0);
      memset (blocklinks, 0, count);
  }
*/


  //
  // P_GroupLines
  // Builds sector line lists and subsector sector numbers.
  // Finds block bounding boxes for sectors.
  //
  public void GroupLines ()
  {
      line_t[]        linebuffer;
      int         i;
      int         j;
      int         total;
      line_t     li;
      sector_t       sector;
      subsector_t    ss;
      seg_t      seg;
      BBox     bbox=new BBox();
      int         block;
      
      // look up sector number for each subsector
      
      for (i=0 ; i<numsubsectors ; i++)
      {
      ss = subsectors[i];
      seg = segs[ss.firstline];
      ss.sector = seg.sidedef.sector;
      }

      // count number of lines in each sector

      total = 0;
      for (i=0 ; i<numlines ; i++)
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
      linebuffer = new line_t[total];
      int linebuffercount=0;
      
      for (i=0 ; i<numsectors ; i++)
      {
          sector = sectors[i];
     bbox.ClearBox();
      sector.lines = linebuffer[linebuffercount];
      for (j=0 ; j<numlines ; j++)
      {
          li=lines[i];
          if (li.frontsector == sector || li.backsector == sector)
          {
          linebuffer[linebuffercount++] = li;
          bbox.AddToBox ( li.v1.x, li.v1.y);
          bbox.AddToBox ( li.v2.x, li.v2.y);
          }
      }
      if (linebuffercount - sector.lines != sector.linecount)
          system.Error ("P_GroupLines: miscounted");
              
      // set the degenmobj_t to the middle of the bounding box
      sector.soundorg.x = (bbox[BOXRIGHT]+bbox[BOXLEFT])/2;
      sector.soundorg.y = (bbox[BOXTOP]+bbox[BOXBOTTOM])/2;
          
      // adjust bounding box to map blocks
      block = (bbox.[BOXTOP]-bmaporgy+MAXRADIUS)>>MAPBLOCKSHIFT;
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


  //
  // P_SetupLevel
  //
  void
  P_SetupLevel
  ( int       episode,
    int       map,
    int       playermask,
    skill_t   skill)
  {
      int     i;
      char    lumpname[9];
      int     lumpnum;
      
      totalkills = totalitems = totalsecret = wminfo.maxfrags = 0;
      wminfo.partime = 180;
      for (i=0 ; i<MAXPLAYERS ; i++)
      {
      players[i].killcount = players[i].secretcount 
          = players[i].itemcount = 0;
      }

      // Initial height of PointOfView
      // will be set by player think.
      players[consoleplayer].viewz = 1; 

      // Make sure all sounds are stopped before Z_FreeTags.
      S_Start ();         

      
  #if 0 // UNUSED
      if (debugfile)
      {
      Z_FreeTags (PU_LEVEL, MAXINT);
      Z_FileDumpHeap (debugfile);
      }
      else
  #endif
      Z_FreeTags (PU_LEVEL, PU_PURGELEVEL-1);


      // UNUSED W_Profile ();
      P_InitThinkers ();

      // if working with a devlopment map, reload it
      W_Reload ();            
         
      // find map name
      if ( gamemode == commercial)
      {
      if (map<10)
          sprintf (lumpname,"map0%i", map);
      else
          sprintf (lumpname,"map%i", map);
      }
      else
      {
      lumpname[0] = 'E';
      lumpname[1] = '0' + episode;
      lumpname[2] = 'M';
      lumpname[3] = '0' + map;
      lumpname[4] = 0;
      }

      lumpnum = W_GetNumForName (lumpname);
      
      leveltime = 0;
      
      // note: most of this ordering is important 
      P_LoadBlockMap (lumpnum+ML_BLOCKMAP);
      P_LoadVertexes (lumpnum+ML_VERTEXES);
      P_LoadSectors (lumpnum+ML_SECTORS);
      P_LoadSideDefs (lumpnum+ML_SIDEDEFS);

      P_LoadLineDefs (lumpnum+ML_LINEDEFS);
      P_LoadSubsectors (lumpnum+ML_SSECTORS);
      P_LoadNodes (lumpnum+ML_NODES);
      P_LoadSegs (lumpnum+ML_SEGS);
      
      rejectmatrix = W_CacheLumpNum (lumpnum+ML_REJECT,PU_LEVEL);
      P_GroupLines ();

      bodyqueslot = 0;
      deathmatch_p = deathmatchstarts;
      P_LoadThings (lumpnum+ML_THINGS);
      
      // if deathmatch, randomly spawn the active players
      if (deathmatch)
      {
      for (i=0 ; i<MAXPLAYERS ; i++)
          if (playeringame[i])
          {
          players[i].mo = NULL;
          G_DeathMatchSpawnPlayer (i);
          }
              
      }

      // clear special respawning que
      iquehead = iquetail = 0;        
      
      // set up world state
      P_SpawnSpecials ();
      
      // build subsector connect matrix
      //  UNUSED P_ConnectSubsectors ();

      // preload graphics
      if (precache)
      R_PrecacheLevel ();

      //printf ("free memory: 0x%x\n", Z_FreeMemory());

  }



  //
  // P_Init
  //
  void P_Init (void)
  {
      P_InitSwitchList ();
      P_InitPicAnims ();
      R_InitSprites (sprnames);
  }




}
