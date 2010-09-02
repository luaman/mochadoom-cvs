package rr;

import static data.Defines.*;
import static m.fixed_t.*;
import g.DoomGame;
import i.system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import p.LevelLoader;

import doom.DoomContext;
import doom.thinker_t;

import w.DoomBuffer;
import w.WadLoader;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: RendererData.java,v 1.2 2010/09/02 15:56:54 velktron Exp $
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
// $Log: RendererData.java,v $
// Revision 1.2  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.1  2010/09/01 15:53:42  velktron
// Graphics data loader implemented....still need to figure out how column caching works, though.
//
//
// Revision 1.3  1997/01/29 20:10
// DESCRIPTION:
//  Preparation of data for rendering,
//  generation of lookups, caching, retrieval by name.
//
//-----------------------------------------------------------------------------

public class RendererData {
 
WadLoader W;
DoomGame DG;
LevelLoader LL;

  public static final String rcsid = "$Id: RendererData.java,v 1.2 2010/09/02 15:56:54 velktron Exp $";

  //
  // Graphics.
  // DOOM graphics for walls and sprites
  // is stored in vertical runs of opaque pixels (posts).
  // A column is composed of zero or more posts,
  // a patch or sprite is composed of zero or more columns.
  // 

  public int     firstflat;
  public int     lastflat;
  public int     numflats;
  /** HACK */
  public flat_t[] flats;

  
  public int     firstpatch;
  public int     lastpatch;
  public int     numpatches;

  public int     firstspritelump;
  public int     lastspritelump;
  public int     numspritelumps;

  public int     numtextures;
  public texture_t[] textures;


  int[]            texturewidthmask;
  /** fixed_t[] needed for texture pegging */
  int[]        textureheight;      
  int[]            texturecompositesize;
  /** Tells us which patch lump covers which column of which texture */
  short[][]         texturecolumnlump;
  /** This is supposed to store indexes into a patch_t lump which point to the columns themselves 
   *  Instead, we're going to return indexes to columns inside a particular patch.
   * */
  short[][]    texturecolumnofs;
  short[][]    texturecolumnindexes;
  byte[][]          texturecomposite;

  // for global animation
  int[]        flattranslation;
  int[]        texturetranslation;

  /** needed for pre rendering (fixed_t[]) */
  int[]    spritewidth,spriteoffset,spritetopoffset;

  /** The underlying data type would be unsigned bytes. Promote to char/short? */
  public byte[]    colormaps;


  /**
  // MAPTEXTURE_T CACHING
  // When a texture is first needed,
  //  it counts the number of composite columns
  //  required in the texture and allocates space
  //  for a column directory and any new columns.
  // The directory will simply point inside other patches
  //  if there is only one patch in a given column,
  //  but any columns with multiple patches
  //  will have new column_ts generated.
  //

  /**
   *  R_DrawColumnInCache
   *  Clip and draw a column
   *  from a patch into a cached post.
   *  
   *  This means that columns are effectively "uncompressed" into cache, here,
   *  and that composite textures are uncompressed...right?
   *  
   */
  
  public void
  DrawColumnInCache
  ( column_t patch,
    byte[]     cache,
    int offset,
    int       originy,
    int       cacheheight )
  {
      int     count;
      int     position;
      int  source=0; // treat as pointers
      int   dest=3; // Inside raw data cache
      
      // Iterate inside column
      for (int i=0;i<patch.posts;i++){

          source += 3; // Relative to patch's data.
          count = patch.postlen[i]; // in this particular post
      position = originy + patch.postdeltas[i];

      // Post would be drawn outside of screen. Fuck it.
      if (position < 0)
      {
          count += position;
          position = 0;
      }

      if (position + count > cacheheight)
          count = cacheheight - position;

      if (count > 0) // Draw this post.
          //memcpy (cache + position, source, count)
          System.arraycopy( patch.data, source, cache, offset+position,count);
          
      // patch = (column_t *)(  (byte *)patch + patch.length + 4);
          // This should position us at the beginning of the next post
          source=patch.postofs[i]+patch.postlen[i]+4;
      }
  }



  /**
   * R_GenerateComposite
   * Using the texture definition,
   *  the composite texture is created from the patches,
   *  and each column is cached.
 * @throws IOException 
   */
  
  public void GenerateComposite (int texnum) throws IOException
  {
      byte[]       block;
      texture_t      texture;
      texpatch_t[]     patch;  
      patch_t        realpatch;
      int         x;
      int         x1;
      int         x2;
      column_t       patchcol;
      short[]      collump;
      short[] colofs; // unsigned short
      short[] colidxs; // unsigned short
      
      texture = textures[texnum];

      // Allocate both the composite and assign it to block.
      // Z_Malloc (texturecompositesize[texnum], PU_STATIC, &texturecomposite[texnum]);
      block = texturecomposite[texnum]=new byte[texturecompositesize[texnum]];
   
      collump = texturecolumnlump[texnum];
      colofs = texturecolumnofs[texnum];
      colidxs = texturecolumnindexes[texnum];
      
      // Composite the columns together.
      patch = texture.patches;
          
      for (int i=0 ;i<texture.patchcount; i++)
      {
      realpatch = (patch_t) W.CacheLumpNum(patch[i].patch, PU_CACHE, patch_t.class);
      x1 = patch[i].originx;
      x2 = x1 + realpatch.width;

      if (x1<0)
          x = 0;
      else
          x = x1;
      
      if (x2 > texture.width)
          x2 = texture.width;

      for ( ; x<x2 ; x++)
      {
          // Column does not have multiple patches?
          if (collump[x] >= 0)
          continue;
          
         // patchcol = (column_t *)((byte *)realpatch
          //            + LONG(realpatch.columnofs[x-x1]));
          
          patchcol=realpatch.columns[x-x1];
          DrawColumnInCache (patchcol,
                   block, colofs[x],
                   patch[i].originy,
                   texture.height);
      }
                          
      }
  }

  /**
   * R_GenerateLookup
 * @throws IOException 
   */
  
  protected void GenerateLookup (int texnum) throws IOException
  {
      texture_t      texture;
      short[]       patchcount; // patchcount[texture.width]
      texpatch_t[]     patch;  
      patch_t        realpatch;
      int         x;
      int         x1;
      int         x2;

      short[]      collump;
       short[] colofs;
      
      texture = textures[texnum];

      // Composited texture not created yet.
      texturecomposite[texnum] = null;
      
      texturecompositesize[texnum] = 0;
      collump = texturecolumnlump[texnum];
      colofs = texturecolumnofs[texnum];
      
      // Now count the number of columns
      //  that are covered by more than one patch.
      // Fill in the lump / offset, so columns
      //  with only a single patch are all done.

      patchcount = new short[texture.width];
      patch = texture.patches;
          
      // for each patch in a texture...
      for (int i=0; i<texture.patchcount;i++)
      {
      realpatch = (patch_t) W.CacheLumpNum (patch[i].patch, PU_CACHE,patch_t.class);
      x1 = patch[i].originx;
      x2 = x1 + realpatch.width;
      
      if (x1 < 0)
          x = 0;
      else
          x = x1;

      if (x2 > texture.width)
          x2 = texture.width;
      for ( ; x<x2 ; x++)
      {
          // Obviously, if a patch starts at x it does cover the x-th column
          // of a texture, even if transparent.
          patchcount[x]++;
          // Column "x" of composite texture "texnum" is covered by this patch.
          collump[x] = (short) patch[i].patch;
          // This is supposed to be a raw pointer to the beginning of the column
          // data, as it appears inside the PATCH.
          // 
          // Instead, we can return the actual column index (x-x1)
          colofs[x] = (short) (realpatch.columnofs[x-x1]+3);
          // This implies that colofs[x] is 0 for a void column?
              
      }
      }
      
      for ( x=0 ; x<texture.width ; x++)
      {
      if (patchcount[x]==0)
      {
          System.err.print ("R_GenerateLookup: column without a patch ("+texture.name+")\n");
          return;
      }
      // I_Error ("R_GenerateLookup: column without a patch");
      
      if (patchcount[x] > 1)
      {
          // Use the cached block.
          collump[x] = -1;    
          colofs[x] = (short) texturecompositesize[texnum];
          
          if (texturecompositesize[texnum] > 0x10000-texture.height)
          {
          system.Error ("R_GenerateLookup: texture %i is >64k",
               texnum);
          }
          
          texturecompositesize[texnum] += texture.height;
      }
      }   
  }




  /**
   * R_GetColumn
 * @throws IOException 
   */
  public byte[] GetColumn
  ( int       tex,
    int       col ) throws IOException
  {
      int     lump;
      int     ofs,idx;
      
      col &= texturewidthmask[tex];
      lump = texturecolumnlump[tex][col];
      ofs = texturecolumnofs[tex][col];
      idx = texturecolumnindexes[tex][col];
      
      // So if this is zero, texture is not composite?
      if (lump > 0)
          // This will actually return a pointer to a patch's columns.
          // That is, to the ONE column exactly.
      return ((patch_t)W.CacheLumpNum(lump,PU_CACHE,patch_t.class)).columns[idx].data;

      if (texturecomposite[tex]==null)
      GenerateComposite (tex);

      // This implies that texturecomposite actually stores raw, compressed columns,
      // or else those "ofs" would go in-between.
      return null;// TODO: texturecomposite[tex] + ofs;
  }




  //
  // R_InitTextures
  // Initializes the texture list
  //  with the textures from the world map.
  //
  public void InitTextures () throws IOException
  {
      maptexture_t   mtexture=new maptexture_t();
      texture_t      texture;
      mappatch_t[]     mpatch;
      texpatch_t[]    patch;

      ByteBuffer        maptex, maptex2, maptex1;
      
      String        name;
      ByteBuffer       names;
      int       name_p;
      
      int[]        patchlookup;
      
      int         totalwidth;
      int         nummappatches;
      int         offset;
      int         maxoff;
      int         maxoff2;
      int         numtextures1;
      int         numtextures2;

      int        directory;
      
      int         temp1;
      int         temp2;
      int         temp3;

      
      // Load the patch names from pnames.lmp.
      //name[8] = 0;    
      names = W.CacheLumpName ("PNAMES", PU_STATIC).getBuffer();
      names.order(ByteOrder.LITTLE_ENDIAN);
      
      // Number of patches.
      names.rewind();
      nummappatches = names.getInt();
      
      patchlookup = new int[nummappatches];
      
      for (int i=0 ; i<nummappatches ; i++)
      {
      // Get a size limited string;
      name=DoomBuffer.getString(names, 8).toUpperCase();
      patchlookup[i] = W.CheckNumForName (name);
      }
      
      names=null;
      
      // Load the map texture definitions from textures.lmp.
      // The data is contained in one or two lumps,
      //  TEXTURE1 for shareware, plus TEXTURE2 for commercial.
      maptex = maptex1 = W.CacheLumpName ("TEXTURE1", PU_STATIC).getBuffer();
      maptex.rewind();
      maptex.order(ByteOrder.LITTLE_ENDIAN);
      numtextures1 = maptex.getInt();
      maxoff = W.LumpLength (W.GetNumForName ("TEXTURE1"));
      directory = 1;
      
      if (W.CheckNumForName ("TEXTURE2") != -1)
      {
      maptex2 = W.CacheLumpName ("TEXTURE2", PU_STATIC).getBuffer();
      maptex2.order(ByteOrder.LITTLE_ENDIAN);
      numtextures2 = maptex2.getInt();
      maxoff2 = W.LumpLength (W.GetNumForName ("TEXTURE2"));
      }
      else
      {
      maptex2 = null;
      numtextures2 = 0;
      maxoff2 = 0;
      }
      
      numtextures = numtextures1 + numtextures2;
      
      textures = new texture_t[numtextures];
      texturecolumnlump = new short[numtextures][];
      texturecolumnofs = new short[numtextures][];
      texturecolumnindexes = new short[numtextures][];
      texturecomposite = new byte[numtextures][];
      texturecompositesize = new int[numtextures];
      texturewidthmask = new int[numtextures];
      textureheight = new int[numtextures];

      totalwidth = 0;
      
      //  Really complex printing shit...
      temp1 = W.GetNumForName ("S_START");  // P_???????
      temp2 = W.GetNumForName ("S_END") - 1;
      temp3 = ((temp2-temp1+63)/64) + ((numtextures+63)/64);
      System.out.print("[");
      for (int i = 0; i < temp3; i++)
      System.out.print(" ");
      System.out.print("         ]");
      
      char BKSPC=0x08;
      for (int i = 0; i < temp3; i++)
          
      for (int bs=0;bs<11;bs++){
          System.out.print(BKSPC);
      }
      
      for (int i=0 ; i<numtextures ; i++,directory++)
      {
      if ((i&63)==0)
          System.out.print ('.');

      if (i == numtextures1)
      {
          // Start looking in second texture file.
          maptex = maptex2;
          maxoff = maxoff2;
          directory = 0; // offset "1" inside maptex buffer
      }
      //System.out.print("Directory "+directory);
      offset = maptex.getInt(directory*4);
      if (offset > maxoff)
          system.Error("R_InitTextures: bad texture directory");
     // System.out.print("offset "+offset+" \n");
      
      maptex.position(offset);
      // Read "maptexture", which is the on-disk form.
      mtexture.unpack(maptex);
      System.out.println(mtexture.name+ " @"+offset);
      
      // We don't need to manually copy trivial fields.
      textures[i]=new texture_t();
      textures[i].copyFromMapTexture(mtexture);
      texture = textures[i];
      //System.out.println("Patches: "+textures[i].patchcount);
      
      // However we do need to correct the "patch.patch" field through the patchlookup
      mpatch = mtexture.patches;
      patch = texture.patches;

      for (int j=0 ; j<texture.patchcount ; j++)
      {
          patch[j].patch = patchlookup[mpatch[j].patch];
          if (patch[j].patch == -1)
          {
          system.Error ("R_InitTextures: Missing patch in texture %s",
               texture.name);
          }
      }       
      
      // Columns and offsets of taxture = textures[i]
      texturecolumnlump[i] = new short[texture.width];
      //C2JUtils.initArrayOfObjects( texturecolumnlump[i], column_t.class);
      texturecolumnofs[i] = new short[texture.width];
      texturecolumnindexes[i] = new short[texture.width];
      
      int j = 1;
      while (j*2 <= texture.width)
          j<<=1;

      texturewidthmask[i] = j-1;
      textureheight[i] = texture.height<<FRACBITS;
          
      totalwidth += texture.width;
      }

      maptex1=null;
      if (maptex2!=null)
      maptex2=null;
      
      // Precalculate whatever possible.  
      for (int i=0 ; i<numtextures ; i++)
      GenerateLookup (i);
      
      // Create translation table for global animation.
      texturetranslation = new int[numtextures];
      
      for (int i=0 ; i<numtextures ; i++)
          texturetranslation[i] = i;
  }



  //
  // R_InitFlats
  //
  protected void InitFlats ()
  {
      int     i;
      
      firstflat = W.GetNumForName ("F_START") + 1;
      lastflat = W.GetNumForName ("F_END") - 1;
      numflats = lastflat - firstflat + 1;
      
      // Create translation table for global animation.
      flattranslation = new int[numflats+1];
      
      for (i=0 ; i<numflats ; i++)
          flattranslation[i] = i;
  }


  //
  // R_InitSpriteLumps
  // Finds the width and hoffset of all sprites in the wad,
  //  so the sprite does not need to be cached completely
  //  just for having the header info ready during rendering.
  //
  protected void InitSpriteLumps () throws IOException
  {
      int     i;
      patch_t patch;
      
      firstspritelump = W.GetNumForName ("S_START") + 1;
      lastspritelump = W.GetNumForName ("S_END") - 1;
      
      numspritelumps = lastspritelump - firstspritelump + 1;
      spritewidth = new int[numspritelumps];
      spriteoffset = new int[numspritelumps];
      spritetopoffset = new int[numspritelumps];
      
      for (i=0 ; i< numspritelumps ; i++)
      {
      if ((i&63)==0)
          System.out.print (".");

      patch = (patch_t)W.CacheLumpNum (firstspritelump+i, PU_CACHE,patch_t.class);
      spritewidth[i] = patch.width<<FRACBITS;
      spriteoffset[i] = patch.leftoffset<<FRACBITS;
      spritetopoffset[i] = patch.topoffset<<FRACBITS;
      }
  }



  /**
  * R_InitColormaps
 * @throws IOException 
  */
  protected void InitColormaps () throws IOException
  {
      int lump, length;
      
      // Load in the light tables, 
      //  256 byte align tables.
      lump = W.GetNumForName("COLORMAP"); 
      length = W.LumpLength (lump) + 255; 
      colormaps = new byte[length];
      ByteBuffer b=ByteBuffer.wrap(colormaps);
      W.ReadLump (lump,b);
     // colormaps = (byte *)( ((int)colormaps + 255)&~0xff); 
       
  }



  /**
   * R_InitData
   * Locates all the lumps
   *  that will be used by all views
   * Must be called after W_Init.
   */
  
  public void InitData ()
  {
      try {
        InitTextures ();
      System.out.print ("\nInitTextures");
      InitFlats ();
      System.out.print ("\nInitFlats");
      InitSpriteLumps ();
      System.out.print ("\nInitSprites");
      InitColormaps ();
      System.out.print ("\nInitColormaps");
      } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }

  }



    /**
     * R_FlatNumForName
     * Retrieval, get a flat number for a flat name.
     */
     
    public int FlatNumForName(String name) {
        int i;

        i = W.CheckNumForName(name);

        if (i == -1) {
            system.Error("R_FlatNumForName: %s not found", name);
        }
        return i - firstflat;
    }




    /**
     * R_CheckTextureNumForName Check whether texture is available. Filter out
     * NoTexture indicator. Seems shit-slow to me...
     */
    public int CheckTextureNumForName(String name) {
        int i;
        // "NoTexture" marker.
        if (name.charAt(0) == '-')
            return 0;

        for (i = 0; i < numtextures; i++)
            if (textures[i].name.compareToIgnoreCase(name) == 0)
                return i;

        return -1;
    }



    /**
     * R_TextureNumForName
     * Calls R_CheckTextureNumForName,
     * aborts with error message.
     */
  
    public int TextureNumForName(String name) {
        int i;

        i = CheckTextureNumForName(name);

        if (i == -1) {
            system.Error("R_TextureNumForName: %s not found", name);
        }
        return i;
    }




  //
  // R_PrecacheLevel
  // Preloads all relevant graphics for the level.
  //
  int     flatmemory;
  int     texturememory;
  int     spritememory;

  public void PrecacheLevel () throws IOException
  {
      boolean[]       flatpresent;
      boolean []      texturepresent;
      boolean []     spritepresent;

      int         i;
      int         j;
      int         k;
      int         lump;
      
      texture_t      texture;
      thinker_t      th;
      spriteframe_t  sf;

      if (DG.demoplayback)
      return;
      
      // Precache flats.
      flatpresent = new boolean[numflats];
      flats=new flat_t[numflats];
      
      for (i=0 ; i<LL.numsectors ; i++)
      {
      flatpresent[LL.sectors[i].floorpic] = true;
      flatpresent[LL.sectors[i].ceilingpic] = true;
      }
      
      flatmemory = 0;

      for (i=0 ; i<numflats ; i++)
      {
      if (flatpresent[i])
      {
          lump = firstflat + i;
          flatmemory += W.lumpinfo[lump].size;
          flats[i]=(flat_t) W.CacheLumpNum(lump, PU_CACHE,flat_t.class);
      }
      }
      
      // Precache textures.
      texturepresent = new boolean[numtextures];
      
      for (i=0 ; i<LL.numsides ; i++)
      {
      texturepresent[LL.sides[i].toptexture] = true;
      texturepresent[LL.sides[i].midtexture] = true;
      texturepresent[LL.sides[i].bottomtexture] = true;
      }

      // Sky texture is always present.
      // Note that F_SKY1 is the name used to
      //  indicate a sky floor/ceiling as a flat,
      //  while the sky texture is stored like
      //  a wall texture, with an episode dependend
      //  name.
      texturepresent[DG.skytexture] = true;
      
      texturememory = 0;
      for (i=0 ; i<numtextures ; i++)
      {
      if (!texturepresent[i])
          continue;

      texture = textures[i];
      
      for (j=0 ; j<texture.patchcount ; j++)
      {
          lump = texture.patches[j].patch;
          texturememory += W.lumpinfo[lump].size;
          W.CacheLumpNum(lump , PU_CACHE,patch_t.class);
      }
      }
      
      // TODO: Precache sprites.
      // spritepresent = new boolean[LL.numsprites];
      
      /*
      for (th = thinkercap.next ; th != &thinkercap ; th=th.next)
      {
      if (th.function.acp1 == (actionf_p1)P_MobjThinker)
          spritepresent[((mobj_t *)th).sprite] = 1;
      }
      
      spritememory = 0;
      for (i=0 ; i<numsprites ; i++)
      {
      if (!spritepresent[i])
          continue;

      for (j=0 ; j<sprites[i].numframes ; j++)
      {
          sf = &sprites[i].spriteframes[j];
          for (k=0 ; k<8 ; k++)
          {
          lump = firstspritelump + sf.lump[k];
          spritememory += lumpinfo[lump].size;
          W_CacheLumpNum(lump , PU_CACHE);
          }
      }
      }*/
  }

    public RendererData(DoomContext DC) {
        this.W = DC.W;
        this.LL = DC.LL;

    }



    
}
