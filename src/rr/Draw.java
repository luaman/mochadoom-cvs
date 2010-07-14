package rr;

// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: Draw.java,v 1.2 2010/07/14 16:12:20 velktron Exp $
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
// $Log: Draw.java,v $
// Revision 1.2  2010/07/14 16:12:20  velktron
// A new direction has been taken for the Renderer: instead of making a single, omnipotent "Renderer" object, the functionality will remain split into at least Renderer, Things, Planes and Draw, with a new DoomRendererContext object keeping everything glued together.
//
// Revision 1.1 2010/07/02 14:26:16 velktron
// Now basic video rendering code should be fine (in SimpleRenderer)
// Defined DoomVideoSystem interface for SimpleRenderer.
//
// Revision 1.1 2010/06/30 08:58:50 velktron
// Let's see if this stuff will finally commit....
//
//
// Most stuff is still being worked on. For a good place to start and get an
// idea of what is being done, I suggest checking out the "testers" package.
//
// Revision 1.1 2010/06/29 11:07:34 velktron
// Release often, release early they say...
//
// Commiting ALL stuff done so far. A lot of stuff is still broken/incomplete,
// and there's still mixed C code in there. I suggest you load everything up in
// Eclpise and see what gives from there.
//
// A good place to start is the testers/ directory, where you can get an idea of
// how a few of the implemented stuff works.
//
//
// DESCRIPTION:
// The actual span/column drawing functions.
// Here find the main potential for optimization,
// e.g. inline assembly, different algorithms.
//
// -----------------------------------------------------------------------------

import static data.Defines.*;
import doom.DoomContext;
import v.DoomVideoRenderer;
import i.system;
import m.fixed_t;
import static m.fixed_t.*;

public class Draw {
    public static String rcsid =
        "$Id: Draw.java,v 1.2 2010/07/14 16:12:20 velktron Exp $";

    Renderer R;

    DoomVideoRenderer V;

    DoomContext DC;

    byte[] screen;

    /*
     * #include "doomdef.h" #include "i_system.h" #include "z_zone.h" #include
     * "w_wad.h" #include "r_local.h" // Needs access to LFB (guess what).
     * #include "v_video.h" // State. #include "doomstat.h"
     */
    // ?
    public static final int MAXWIDTH = 1120;

    public static final int MAXHEIGT = 832;

    // status bar height at bottom of screen
    public static final int SBARHEIGHT = 32;

    private static final boolean RANGECHECK = false;

    //
    // All drawing to the view buffer is accomplished in this file.
    // The other refresh files only know about ccordinates,
    // not the architecture of the frame buffer.
    // Conveniently, the frame buffer is a linear one,
    // and we need only the base address,
    // and the total size == width*height*depth/8.,
    //

    byte[] viewimage;

    int viewwidth;

    int scaledviewwidth;

    int viewheight;

    int viewwindowx;

    int viewwindowy;

    /** Probably a lookup inside a column to draw? */
    int[] ylookup = new int[MAXHEIGHT];

    /** Columns offset to set where?! */
    int[] columnofs = new int[MAXWIDTH];

    // Color tables for different players,
    // translate a limited part to another
    // (color ramps used for suit colors).
    //
    byte[][] translations = new byte[3][256];

    //
    // R_DrawColumn
    // Source is the top of the column to scale.
    //
    lighttable_t[] dc_colormap;

    int dc_x;

    int dc_yl;

    int dc_yh;

    fixed_t dc_iscale;

    /** fixed_t */
    int dc_texturemid;

    /** first pixel in a column (possibly virtual) */
    byte[] dc_source;

    // byte[] dc_data;

    // just for profiling
    int dccount;

    //
    // A column is a vertical slice/span from a wall texture that,
    // given the DOOM style restrictions on the view orientation,
    // will always have constant z depth.
    // Thus a special case loop for very fast rendering can
    // be used. It has also been used with Wolfenstein 3D.
    // 
    /**
     * A column is a vertical slice/span from a wall texture that, given the
     * DOOM style restrictions on the view orientation, will always have
     * constant z depth. Thus a special case loop for very fast rendering can be
     * used. It has also been used with Wolfenstein 3D. MAES: this is called
     * mostly from inside Draw and from an external "Renderer"
     */

    public void DrawColumn() {
        int count;
        // byte* dest;
        int dest; // As pointer
        // fixed_t
        int frac;
        int fracstep;

        count = dc_yh - dc_yl;

        // Zero length, column does not exceed a pixel.
        if (count < 0)
            return;

        if (RANGECHECK) {
            if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT)
                system
                        .Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
                            dc_x);
        }

        // Framebuffer destination address.
        // Use ylookup LUT to avoid multiply with ScreenWidth.
        // Use columnofs LUT for subwindows?
        dest = ylookup[dc_yl] + columnofs[dc_x];

        // Determine scaling,
        // which is the only mapping to be done.
        fracstep = dc_iscale.val;
        frac = dc_texturemid + (dc_yl - R.centery) * fracstep;

        // Inner loop that does the actual texture mapping,
        // e.g. a DDA-lile scaling.
        // This is as fast as it gets.
        do {
            // Re-map color indices from wall texture column
            // using a lighting/special effects LUT.
            // TODO: determine WHERE the fuck "*dest" is supposed to be
            // pointing.
            // dc_source was probably just a pointer to a decompressed
            // column...right?
            screen[dest] = dc_colormap[dc_source[(frac >> FRACBITS) & 127]].val;

            dest += SCREENWIDTH;
            frac += fracstep;

        } while (count-- > 0);
    }

    // UNUSED.
    // Loop unrolled.
    /*
     * #if 0 void R_DrawColumn (void) { int count; byte* source; byte* dest;
     * byte* colormap; unsigned frac; unsigned fracstep; unsigned fracstep2;
     * unsigned fracstep3; unsigned fracstep4; count = dc_yh - dc_yl + 1; source
     * = dc_source; colormap = dc_colormap; dest = ylookup[dc_yl] +
     * columnofs[dc_x]; fracstep = dc_iscale<<9; frac = (dc_texturemid +
     * (dc_yl-centery)*dc_iscale)<<9; fracstep2 = fracstep+fracstep; fracstep3 =
     * fracstep2+fracstep; fracstep4 = fracstep3+fracstep; while (count >= 8) {
     * dest[0] = colormap[source[frac>>25]]; dest[SCREENWIDTH] =
     * colormap[source[(frac+fracstep)>>25]]; dest[SCREENWIDTH*2] =
     * colormap[source[(frac+fracstep2)>>25]]; dest[SCREENWIDTH*3] =
     * colormap[source[(frac+fracstep3)>>25]]; frac += fracstep4;
     * dest[SCREENWIDTH*4] = colormap[source[frac>>25]]; dest[SCREENWIDTH*5] =
     * colormap[source[(frac+fracstep)>>25]]; dest[SCREENWIDTH*6] =
     * colormap[source[(frac+fracstep2)>>25]]; dest[SCREENWIDTH*7] =
     * colormap[source[(frac+fracstep3)>>25]]; frac += fracstep4; dest +=
     * SCREENWIDTH*8; count -= 8; } while (count > 0) {dest =
     * colormap[source[frac>>25]]; dest += SCREENWIDTH; frac += fracstep;
     * count--; } } #endif
     */

    public void DrawColumnLow() {
        int count;
        // MAES: were pointers. Of course...
        int dest;
        int dest2;
        // Maes: fixed_t never used as such.
        int frac;
        int fracstep;

        count = dc_yh - dc_yl;

        // Zero length.
        if (count < 0)
            return;

        if (RANGECHECK) {
            if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT) {

                system
                        .Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
                            dc_x);
            }
            // dccount++;
        }
        // Blocky mode, need to multiply by 2.
        dc_x <<= 1;

        dest = ylookup[dc_yl] + columnofs[dc_x];
        dest2 = ylookup[dc_yl] + columnofs[dc_x + 1];

        fracstep = dc_iscale.val;
        frac = dc_texturemid + (dc_yl - R.centery) * fracstep;

        do {
            // Hack. Does not work corretly.
            // MAES: that's good to know.
            screen[dest2] =
                screen[dest] =
                    dc_colormap[dc_source[(frac >> FRACBITS) & 127]].val;
            dest += SCREENWIDTH;
            dest2 += SCREENWIDTH;
            frac += fracstep;

        } while (count-- != 0);
    }

    //
    // Spectre/Invisibility.
    //
    public static final int FUZZTABLE = 50;

    public static final int FUZZOFF = SCREENWIDTH;

    int[] fuzzoffset =
        { FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF,
                -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF,
                -FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
                -FUZZOFF, -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF };

    int fuzzpos = 0;

    //
    // Framebuffer postprocessing.
    // Creates a fuzzy image by copying pixels
    // from adjacent ones to left and right.
    // Used with an all black colormap, this
    // could create the SHADOW effect,
    // i.e. spectres and invisible players.
    //
    public void DrawFuzzColumn() {
        int count;
        int dest;
        int frac;
        int fracstep;

        // Adjust borders. Low...
        if (dc_yl == 0)
            dc_yl = 1;

        // .. and high.
        if (dc_yh == viewheight - 1)
            dc_yh = viewheight - 2;

        count = dc_yh - dc_yl;

        // Zero length.
        if (count < 0)
            return;

        if (RANGECHECK) {
            if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT) {
                system.Error("R_DrawFuzzColumn: %i to %i at %i", dc_yl, dc_yh,
                    dc_x);
            }
        }

        // Keep till detailshift bug in blocky mode fixed,
        // or blocky mode removed.
        /*
         * WATCOM code if (detailshift) { if (dc_x & 1) { outpw
         * (GC_INDEX,GC_READMAP+(2<<8) ); outp (SC_INDEX+1,12); } else { outpw
         * (GC_INDEX,GC_READMAP); outp (SC_INDEX+1,3); } dest = destview +
         * dc_yl*80 + (dc_x>>1); } else { outpw
         * (GC_INDEX,GC_READMAP+((dc_x&3)<<8) ); outp (SC_INDEX+1,1<<(dc_x&3));
         * dest = destview + dc_yl*80 + (dc_x>>2); }
         */

        // Does not work with blocky mode.
        dest = ylookup[dc_yl] + columnofs[dc_x];

        // Looks familiar.
        fracstep = dc_iscale.val;
        frac = dc_texturemid + (dc_yl - R.centery) * fracstep;

        // Looks like an attempt at dithering,
        // using the colormap #6 (of 0-31, a bit
        // brighter than average).
        do {
            // Lookup framebuffer, and retrieve
            // a pixel that is either one column
            // left or right of the current one.
            // Add index from colormap to index.
            screen[dest] =
                R.colormaps[6 * 256 + screen[dest + fuzzoffset[fuzzpos]]].val;

            // Clamp table lookup index.
            if (++fuzzpos == FUZZTABLE)
                fuzzpos = 0;

            dest += SCREENWIDTH;

            frac += fracstep;
        } while (count-- != 0);
    }

    //
    // R_DrawTranslatedColumn
    // Used to draw player sprites
    // with the green colorramp mapped to others.
    // Could be used with different translation
    // tables, e.g. the lighter colored version
    // of the BaronOfHell, the HellKnight, uses
    // identical sprites, kinda brightened up.
    //
    byte[] dc_translation;

    byte[] translationtables;

    public void R_DrawTranslatedColumn() {
        int count;
        // MAES: you know the deal by now...
        int dest;
        int frac;
        int fracstep;

        count = dc_yh - dc_yl;
        if (count < 0)
            return;

        if (RANGECHECK) {
            if (dc_x >= SCREENWIDTH || dc_yl < 0 || dc_yh >= SCREENHEIGHT) {
                system
                        .Error("R_DrawColumn: %i to %i at %i", dc_yl, dc_yh,
                            dc_x);
            }
        }

        // WATCOM VGA specific.
        /*
         * Keep for fixing. if (detailshift) { if (dc_x & 1) outp
         * (SC_INDEX+1,12); else outp (SC_INDEX+1,3); dest = destview + dc_yl*80
         * + (dc_x>>1); } else { outp (SC_INDEX+1,1<<(dc_x&3)); dest = destview
         * + dc_yl*80 + (dc_x>>2); }
         */

        // FIXME. As above.
        dest = ylookup[dc_yl] + columnofs[dc_x];

        // Looks familiar.
        fracstep = dc_iscale.val;
        frac = dc_texturemid + (dc_yl - R.centery) * fracstep;

        // Here we do an additional index re-mapping.
        do {
            // Translation tables are used
            // to map certain colorramps to other ones,
            // used with PLAY sprites.
            // Thus the "green" ramp of the player 0 sprite
            // is mapped to gray, red, black/indigo.
            screen[dest] =
                dc_colormap[dc_translation[dc_source[frac >> FRACBITS]]].val;
            dest += SCREENWIDTH;

            frac += fracstep;
        } while (count-- != 0);
    }

    //
    // R_InitTranslationTables
    // Creates the translation tables to map
    // the green color ramp to gray, brown, red.
    // Assumes a given structure of the PLAYPAL.
    // Could be read from a lump instead.
    //
    void InitTranslationTables() {
        int i;

        // translationtables = Z_Malloc (256*3+255, PU_STATIC, 0);
        // translationtables = (byte *)(( (int)translationtables + 255 )& ~255);
        translationtables = new byte[256 * 3 + 255];

        // translate just the 16 green colors
        for (i = 0; i < 256; i++) {
            if (i >= 0x70 && i <= 0x7f) {
                // map green ramp to gray, brown, red
                translationtables[i] = (byte) (0x60 + (i & 0xf));
                translationtables[i + 256] = (byte) (0x40 + (i & 0xf));
                translationtables[i + 512] = (byte) (0x20 + (i & 0xf));
            } else {
                // Keep all other colors as is.
                translationtables[i] =
                    translationtables[i + 256] =
                        translationtables[i + 512] = (byte) i;
            }
        }
    }

    //
    // R_DrawSpan
    // With DOOM style restrictions on view orientation,
    // the floors and ceilings consist of horizontal slices
    // or spans with constant z depth.
    // However, rotation around the world z axis is possible,
    // thus this mapping, while simpler and faster than
    // perspective correct texture mapping, has to traverse
    // the texture at an angle in all but a few cases.
    // In consequence, flats are not stored by column (like walls),
    // and the inner loop has to step in texture space u and v.
    //
    int ds_y;

    int ds_x1;

    int ds_x2;

    lighttable_t[] ds_colormap;

    /** fixed_t */
    int ds_xfrac;

    /** fixed_t */
    int ds_yfrac;

    /** fixed_t */
    int ds_xstep;

    /** fixed_t */
    int ds_ystep;

    /** start of a 64*64 tile image (treat as pointer) */
    int ds_pointer;

    byte[] ds_source;

    /** just for profiling */
    int dscount;

    /**
     * Draws the actual span.
     */
    public void DrawSpan() {
        int f_xfrac; // fixed_t
        int f_yfrac; // fixed_t
        int dest;
        int count;
        int spot;

        if (RANGECHECK) {
            if (ds_x2 < ds_x1 || ds_x1 < 0 || ds_x2 >= SCREENWIDTH
                    || ds_y > SCREENHEIGHT) {
                system.Error("R_DrawSpan: %i to %i at %i", ds_x1, ds_x2, ds_y);
            }
            // dscount++;
        }

        f_xfrac = ds_xfrac;
        f_yfrac = ds_yfrac;

        dest = ylookup[ds_y] + columnofs[ds_x1];

        // We do not check for zero spans here?
        count = ds_x2 - ds_x1;

        do {
            // Current texture index in u,v.
            spot = ((f_yfrac >> (16 - 6)) & (63 * 64)) + ((f_xfrac >> 16) & 63);

            // Lookup pixel from flat texture tile,
            // re-index using light/colormap.
            screen[dest++] = ds_colormap[ds_source[spot]].val;

            // Next step in u,v.
            f_xfrac += ds_xstep;
            f_yfrac += ds_ystep;

        } while (count-- != 0);
    }

    // UNUSED.
    // Loop unrolled by 4.
    /*
     * #if 0 void R_DrawSpan (void) { unsigned position, step; byte* source;
     * byte* colormap; byte* dest; unsigned count; usingned spot; unsigned
     * value; unsigned temp; unsigned xtemp; unsigned ytemp; position =
     * ((ds_xfrac<<10)&0xffff0000) | ((ds_yfrac>>6)&0xffff); step =
     * ((ds_xstep<<10)&0xffff0000) | ((ds_ystep>>6)&0xffff); source = ds_source;
     * colormap = ds_colormap; dest = ylookup[ds_y] + columnofs[ds_x1]; count =
     * ds_x2 - ds_x1 + 1; while (count >= 4) { ytemp = position>>4; ytemp =
     * ytemp & 4032; xtemp = position>>26; spot = xtemp | ytemp; position +=
     * step; dest[0] = colormap[source[spot]]; ytemp = position>>4; ytemp =
     * ytemp & 4032; xtemp = position>>26; spot = xtemp | ytemp; position +=
     * step; dest[1] = colormap[source[spot]]; ytemp = position>>4; ytemp =
     * ytemp & 4032; xtemp = position>>26; spot = xtemp | ytemp; position +=
     * step; dest[2] = colormap[source[spot]]; ytemp = position>>4; ytemp =
     * ytemp & 4032; xtemp = position>>26; spot = xtemp | ytemp; position +=
     * step; dest[3] = colormap[source[spot]]; count -= 4; dest += 4; } while
     * (count > 0) { ytemp = position>>4; ytemp = ytemp & 4032; xtemp =
     * position>>26; spot = xtemp | ytemp; position += step;dest++ =
     * colormap[source[spot]]; count--; } } #endif
     */

    //
    // Again..
    //
    public void DrawSpanLow() {
        int f_xfrac;
        int f_yfrac;
        int dest;
        int count;
        int spot;

        if (RANGECHECK) {
            if ((ds_x2 < ds_x1) || (ds_x1 < 0) || ds_x2 >= SCREENWIDTH
                    || ds_y > SCREENHEIGHT) {
                system.Error("R_DrawSpan: %i to %i at %i", ds_x1, ds_x2, ds_y);
            }
            // dscount++;
        }

        f_xfrac = ds_xfrac;
        f_yfrac = ds_yfrac;

        // Blocky mode, need to multiply by 2.
        ds_x1 <<= 1;
        ds_x2 <<= 1;

        dest = ylookup[ds_y] + columnofs[ds_x1];

        count = ds_x2 - ds_x1;
        do {
            spot = ((f_yfrac >> (16 - 6)) & (63 * 64)) + ((f_xfrac >> 16) & 63);
            // Lowres/blocky mode does it twice,
            // while scale is adjusted appropriately.
            screen[dest++] = ds_colormap[ds_source[spot]].val;
            screen[dest++] = ds_colormap[ds_source[spot]].val;

            f_xfrac += ds_xstep;
            f_yfrac += ds_ystep;

        } while (count-- != 0);
    }

    //

    /**
     * R_InitBuffer Creates lookup tables that avoid multiplies and other
     * hazzles for getting the framebuffer address of a pixel to draw. MAES:
     * this is "pinned" to screen[0] of a Video Renderer. We will handle this
     * differently elsewhere...
     */

    public void InitBuffer(int width, int height) {
        int i;

        // Handle resize,
        // e.g. smaller view windows
        // with border and/or status bar.
        viewwindowx = (SCREENWIDTH - width) >> 1;

        // Column offset. For windows.
        for (i = 0; i < width; i++)
            columnofs[i] = viewwindowx + i;

        // Samw with base row offset.
        if (width == SCREENWIDTH)
            viewwindowy = 0;
        else
            viewwindowy = (SCREENHEIGHT - SBARHEIGHT - height) >> 1;

        // Preclaculate all row offsets.
        for (i = 0; i < height; i++)
            ylookup[i] = /* screens[0] + */(i + viewwindowy) * SCREENWIDTH;
    }

    //
    // R_FillBackScreen
    // Fills the back screen with a pattern
    // for variable screen sizes
    // Also draws a beveled edge.
    //
    public void FillBackScreen() {
        byte[] src;
        byte[] dest;
        int x;
        int y;
        patch_t patch;

        // DOOM border patch.
        String name1 = "FLOOR7_2";

        // DOOM II border patch.
        String name2 = "GRNROCK";

        String name;

        if (scaledviewwidth == 320)
            return;

        if (DC.DS.gamemode == GameMode_t.commercial)
            name = name2;
        else
            name = name1;

        // MAES: do a RAW get here? :-S
        src = DC.W.CacheLumpName(name, PU_CACHE).getBuffer().array();
        dest = V.getScreen(1);
        int destPos = 0;

        for (y = 0; y < SCREENHEIGHT - SBARHEIGHT; y++) {
            for (x = 0; x < SCREENWIDTH / 64; x++) {
                // memcpy (dest, src+((y&63)<<6), 64);
                System.arraycopy(src, ((y & 63) << 6), dest, destPos, 64);
                destPos += 64;
            }

            if ((SCREENWIDTH & 63) != 0) {
                // memcpy (dest, src+((y&63)<<6), SCREENWIDTH&63);
                System.arraycopy(src, ((y & 63) << 6), dest, destPos,
                    SCREENWIDTH & 63);

                destPos += (SCREENWIDTH & 63);
            }
        }

        patch = (patch_t) DC.W.CacheLumpName("brdr_t", PU_CACHE, patch_t.class);

        for (x = 0; x < scaledviewwidth; x += 8)
            V.DrawPatch(viewwindowx + x, viewwindowy - 8, 1, patch);
        patch = (patch_t) DC.W.CacheLumpName("brdr_b", PU_CACHE, patch_t.class);

        for (x = 0; x < scaledviewwidth; x += 8)
            V.DrawPatch(viewwindowx + x, viewwindowy + viewheight, 1, patch);
        patch = (patch_t) DC.W.CacheLumpName("brdr_l", PU_CACHE, patch_t.class);

        for (y = 0; y < viewheight; y += 8)
            V.DrawPatch(viewwindowx - 8, viewwindowy + y, 1, patch);
        patch = (patch_t) DC.W.CacheLumpName("brdr_r", PU_CACHE, patch_t.class);

        for (y = 0; y < viewheight; y += 8)
            V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy + y, 1,
                patch);

        // Draw beveled edge. Top-left
        V.DrawPatch(viewwindowx - 8, viewwindowy - 8, 1, (patch_t) DC.W
                .CacheLumpName("brdr_tl", PU_CACHE, patch_t.class));

        // Top-right.
        V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy - 8, 1,
            (patch_t) DC.W.CacheLumpName("brdr_tr", PU_CACHE, patch_t.class));

        // Bottom-left
        V.DrawPatch(viewwindowx - 8, viewwindowy + viewheight, 1,
            (patch_t) DC.W.CacheLumpName("brdr_bl", PU_CACHE, patch_t.class));
        // Bottom-right.
        V.DrawPatch(viewwindowx + scaledviewwidth, viewwindowy + viewheight, 1,
            (patch_t) DC.W.CacheLumpName("brdr_br", PU_CACHE, patch_t.class));
    }

    //
    // Copy a screen buffer.
    //
    public void VideoErase(int ofs, int count) {
        // LFB copy.
        // This might not be a good idea if memcpy
        // is not optiomal, e.g. byte by byte on
        // a 32bit CPU, as GNU GCC/Linux libc did
        // at one point.
        // memcpy (screens[0]+ofs, screens[1]+ofs, count);
        System.arraycopy(V.getScreen(0), ofs, V.getScreen(1), ofs, count);

    }

    //
    // R_DrawViewBorder
    // Draws the border around the view
    // for different size windows?
    //
    public void MarkRect(int x, int y, int width, int height) {

    }

    public void DrawViewBorder() {
        int top;
        int side;
        int ofs;
        int i;

        if (scaledviewwidth == SCREENWIDTH)
            return;

        top = ((SCREENHEIGHT - SBARHEIGHT) - viewheight) / 2;
        side = (SCREENWIDTH - scaledviewwidth) / 2;

        // copy top and one line of left side
        this.VideoErase(0, top * SCREENWIDTH + side);

        // copy one line of right side and bottom
        ofs = (viewheight + top) * SCREENWIDTH - side;
        this.VideoErase(ofs, top * SCREENWIDTH + side);

        // copy sides using wraparound
        ofs = top * SCREENWIDTH + SCREENWIDTH - side;
        side <<= 1;

        for (i = 1; i < viewheight; i++) {
            this.VideoErase(ofs, side);
            ofs += SCREENWIDTH;
        }

        // ?
        this.MarkRect(0, 0, SCREENWIDTH, SCREENHEIGHT - SBARHEIGHT);
    }

}