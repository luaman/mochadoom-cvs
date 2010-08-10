package data;

// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: doomdata.java,v 1.2 2010/08/10 16:41:57 velktron Exp $
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// DESCRIPTION:
// all external data is defined here
// most of the data is loaded into different structures at run time
// some internal structures shared by many modules are here
//
// -----------------------------------------------------------------------------

// The most basic types we use, portability.
// #include "doomtype.h"

// Some global defines, that configure the game.

public class doomdata {
    //
    // Map level types.
    // The following data structures define the persistent format
    // used in the lumps of the WAD files.
    //

    // Lump order in a map WAD: each map needs a couple of lumps
    // to provide a complete scene geometry description.

    public static final int ML_LABEL = 0;

    /** A separator, name, ExMx or MAPxx */
    public static final int ML_THINGS = 1;

    /** Monsters, items.. */
    public static final int ML_LINEDEFS = 2;

    /** LineDefs, from editing */
    public static final int ML_SIDEDEFS = 3;

    /** SideDefs, from editing */
    public static final int ML_VERTEXES = 4;

    /** Vertices, edited and BSP splits generated */
    public static final int ML_SEGS = 5;

    /** LineSegs, from LineDefs split by BSP */
    public static final int ML_SSECTORS = 6;

    /** SubSectors, list of LineSegs */
    public static final int ML_NODES = 7;

    /** BSP nodes */
    public static final int ML_SECTORS = 8;

    /** Sectors, from editing */
    public static final int ML_REJECT = 9;

    /** LUT, sector-sector visibility */
    public static final int ML_BLOCKMAP = 10;

    /**
     * LUT, motion clipping, walls/grid element // // LineDef attributes. // /**
     * Solid, is an obstacle.
     */
    public static final int ML_BLOCKING = 1;

    /** Blocks monsters only. */
    public static final int ML_BLOCKMONSTERS = 2;

    /** Backside will not be present at all if not two sided. */
    public static final int ML_TWOSIDED = 4;

    // If a texture is pegged, the texture will have
    // the end exposed to air held constant at the
    // top or bottom of the texture (stairs or pulled
    // down things) and will move with a height change
    // of one of the neighbor sectors.
    // Unpegged textures allways have the first row of
    // the texture at the top pixel of the line for both
    // top and bottom textures (use next to windows).

    /** upper texture unpegged */
    public static final int ML_DONTPEGTOP = 8;

    /** lower texture unpegged */
    public static final int ML_DONTPEGBOTTOM = 16;

    /** In AutoMap: don't map as two sided: IT'S A SECRET! */
    public static final int ML_SECRET = 32;

    /** Sound rendering: don't let sound cross two of these. */
    public static final int ML_SOUNDBLOCK = 64;

    /** Don't draw on the automap at all. */
    public static final int ML_DONTDRAW = 128;

    /** Set if already seen, thus drawn in automap. */
    public static final int ML_MAPPED = 256;

    /** Indicate a leaf. */
    public static int NF_SUBSECTOR = 0x8000;

}