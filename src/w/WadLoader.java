// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: WadLoader.java,v 1.6 2010/08/10 16:41:57 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This source is available for distribution and/or modification
// only under the terms of the DOOM Source Code License as
// published by id Software. All rights reserved.
//
// The source is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// FITNESS FOR A PARTICULAR PURPOSE. See the DOOM Source Code License
// for more details.
//
// $Log: WadLoader.java,v $
// Revision 1.6  2010/08/10 16:41:57  velktron
// Threw some work into map loading.
//
// Revision 1.5  2010/07/22 15:37:53  velktron
// MAJOR changes in Menu system.
//
// Revision 1.4  2010/07/15 14:01:49  velktron
// Added reflector Method stuff for function pointers.
//
// Revision 1.3  2010/07/06 15:20:23  velktron
// Several changes in the WAD loading routine. Now lumps are directly unpacked as "CacheableDoomObjects" and only defaulting will result in "raw" DoomBuffer reads.
//
// Makes caching more effective.
//
// Revision 1.2 2010/06/30 11:44:40 velktron
// Added a tester for patches (one of the most loosely-coupled structs in Doom!)
// and fixed some minor stuff all around.
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
// Handles WAD file header, directory, lump I/O.
//
// -----------------------------------------------------------------------------

package w;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import rr.patch_t;

import utils.C2JUtils;
import z.memblock_t;

import static data.Defines.*;
import i.*;

public class WadLoader {

    // #include "doomtype.h"
    // #include "m_swap.h"
    // #include "i_system.h"
    // #include "z_zone.h"

    //
    // GLOBALS
    //

    // Location of each lump on disk.
    public lumpinfo_t[] lumpinfo;

    public WadLoader() {
        lumpinfo = new lumpinfo_t[0];
    }

    public int numlumps;

    // MAES: probably array of byte[]???
    // void** lumpcache;

    // Actually, loaded objects will be deserialized there (in the worst case
    // they will be byte[] or ByteBuffer).
    private CacheableDoomObject[] lumpcache;

    private boolean[] preloaded;

    // #define strcmpi strcasecmp

    // MAES: this is just capitalization.
    // However we can't manipulate String object in Java directly like this,
    // so this must be a return type.

    public String strupr(String s) {
        return s.toUpperCase();
    }

    public void strupr(char[] s) {
        for (int i = 0; i < s.length; i++) {
            s[i] = Character.toUpperCase(s[i]);
        }
    }

    // Maes: File intead of "inthandle"

    public long filelength(File handle) {
        try {/*
              * stat fileinfo; if (fstat (handle,&fileinfo) == -1) I_Error
              * ("Error fstating");
              */

            return handle.length();
        } catch (Exception e) {
            system.Error("Error fstating");
            return -1;
        }

    }

    // MAES: same problem here. I'd rather return a new String than doing stupid
    // shit like passing an array of Strings as dest and modifying the first
    // item.

    public String ExtractFileBase(String path) {
        byte[] dest = new byte[8];

        int length = 0;
        int src = path.length() - 1;
        // Duh???

        // back up until a \ or the start
        // MAES: hideously not OO or cross-platform :-p
        /*
         * while ((src >=0) && path.charAt(src) != '\\' && path.charAt(src) !=
         * '/') { src--; }
         */

        // Maes: better use this, I think.

        src = path.lastIndexOf('\\');
        if (src < 0)
            src = path.lastIndexOf('/');
        if (src < 0)
            src = 0;

        // copy up to eight characters
        int pos = 0;
        while ((pos < path.length()) && (path.charAt(pos) != '.')) {
            if (++length == 9)
                system.Error("Filename base of %s >8 chars", path);

            dest[pos] = (byte) path.charAt(pos);
            pos++;
        }
        return new String(dest);
    }

    //
    // LUMP BASED ROUTINES.
    //

    //
    // W_AddFile
    // All files are optional, but at least one file must be
    // found (PWAD, if all required lumps are present).
    // Files with a .wad extension are wadlink files
    // with multiple lumps.
    // Other files are single lumps with the base filename
    // for the lump name.
    //
    // If filename starts with a tilde, the file is handled
    // specially to allow map reloads.
    // But: the reload feature is a fragile hack...

    int reloadlump;

    // MAES: was char*
    String reloadname;

    @SuppressWarnings("null")
    public void AddFile(String filename)
            throws Exception {
        wadinfo_t header = new wadinfo_t();
        int lump_p; // MAES: was lumpinfo_t* , but we can use it as an array
        // pointer.
        DoomFile handle;
        long length;
        int startlump;
        filelump_t[] fileinfo = new filelump_t[1]; // MAES: was *
        filelump_t singleinfo = new filelump_t();
        DoomFile storehandle;

        // open the file and add to directory

        // handle reload indicator.
        if (filename.charAt(0) == '~') {
            filename = filename.substring(1);
            reloadname = filename;
            reloadlump = numlumps;
        }

        try {
            handle = new DoomFile(filename, "r");
        } catch (Exception e) {
            system.Error(" couldn't open %s \n", filename);
            return;
        }

        //System.out.println(" adding " + filename + "\n");

        // We start at the number of lumps? :-S
        startlump = this.numlumps;

        if (filename.substring(filename.length() - 3)
                .compareToIgnoreCase("wad") != 0) {
            // single lump file
            fileinfo[0] = singleinfo;
            singleinfo.filepos = 0;
            singleinfo.size = (long) (handle.length());
            singleinfo.name = ExtractFileBase(filename);
            numlumps++;
        } else {
            // MAES: 14/06/10 this is historical, for this is the first time I
            // implement reading something
            // from RAF into Doom's structs. Kudos to the JAKE2 team who solved
            // this problem before me.
            // Check out how reading is delegated to each class's "load" method.
            // read (handle, &header, sizeof(header));

            header.load(handle);

            if (header.identification.compareTo("IWAD") != 0) {
                // Homebrew levels?
                if (header.identification.compareTo("PWAD") != 0) {
                    system.Error("Wad file %s doesn't have IWAD or PWAD id\n",
                        filename);
                }

                // TODO: modifiedgame = true;
            }

            // MAES: I don't think the following are needed. Casting to long?
            // :-S
            // header.numlumps = header.numlumps;
            // header.infotableofs = header.infotableofs;

            length = header.numlumps;
            fileinfo = new filelump_t[(int) length];
            // Init everything:
            C2JUtils.initArrayOfObjects(fileinfo, filelump_t.class);

            handle.seek(header.infotableofs);
            // MAES: we can't read raw structs here, and even less BLOCKS of
            // structs.

            handle.readObjectArray(fileinfo, (int) length);

            /*
             * for (int j=0;j<length;j++){ fileinfo[j].load (handle); }
             */
            numlumps += header.numlumps;

            // Fill in lumpinfo
            // MAES: this was a realloc(lumpinfo, numlumps*sizeof(lumpinfo_t)),
            // so we have to increase size and copy over. Maybe this should be
            // an ArrayList?

            int oldsize = lumpinfo.length;
            lumpinfo_t[] newlumpinfo = new lumpinfo_t[numlumps];

            try {
                C2JUtils.initArrayOfObjects(newlumpinfo, lumpinfo_t.class);
                System.arraycopy(lumpinfo, 0, newlumpinfo, 0, oldsize);
            } catch (Exception e) {
                // if (!lumpinfo)
                i.system.Error("Couldn't realloc lumpinfo");
            }

            // Bye bye, old lumpinfo!
            lumpinfo = newlumpinfo;

            // MAES: lump_p was an alias for lumpinfo[startlump]. I know it's a
            // bit crude as an approximation but heh...

            lump_p = startlump;

            // MAES: if reloadname is null, handle is stored...else an invalid
            // handle?
            storehandle = (reloadname != null) ? null : handle;

            int fileinfo_p = 0;

            for (int i = startlump; i < numlumps; i++, lump_p++, fileinfo_p++) {
                lumpinfo[lump_p].handle = storehandle;
                lumpinfo[lump_p].position = fileinfo[fileinfo_p].filepos;
                lumpinfo[lump_p].size = fileinfo[fileinfo_p].size;
                // strncpy (lumpinfo[lump_p].name, fileinfo[fileinfo_p].name,
                // 8);
                lumpinfo[lump_p].name =
                    fileinfo[fileinfo_p].name.substring(0, 8);
            }

            if (reloadname != null)
                handle.close();
        }
    }

    /**
     * W_Reload Flushes any of the reloadable lumps in memory and reloads the
     * directory.
     * @throws Exception 
     */
    @SuppressWarnings("null")
    public void Reload()
            throws Exception {
        wadinfo_t header = new wadinfo_t();
        int lumpcount;
        int lump_p; // Maes: same as in W_WADload
        int i;
        DoomFile handle = null;
        int length;
        filelump_t[] fileinfo;

        if (reloadname == null)
            return;

        try {
            handle = new DoomFile(reloadname, "r");
        } catch (Exception e) {
            system.Error("W_Reload: couldn't open %s", reloadname);
        }

        header.load(handle);
        // Actual number of lumps in file...
        lumpcount = (int) header.numlumps;
        header.infotableofs = header.infotableofs;
        length = lumpcount;
        fileinfo = new filelump_t[length];
        handle.seek(header.infotableofs);

        // MAES: we can't read raw structs here, and even less BLOCKS of
        // structs.

        handle.readObjectArrayWithReflection(fileinfo, (int) length);

        /*
         * for (int j=0;j<length;j++){ fileinfo[j].load (handle); }
         */

//        numlumps += header.numlumps;
        // read (handle, fileinfo, length);

        // Fill in lumpinfo
        lump_p = reloadlump;
        int fileinfo_p = 0;
        for (i = reloadlump; i < reloadlump + lumpcount; i++, lump_p++, fileinfo_p++) {
            if (lumpcache[i] != null) {
                // That's like "freeing" it, right?
                lumpcache[i] = null;
                preloaded[i] = false;
            }

            lumpinfo[lump_p].position = fileinfo[fileinfo_p].filepos;
            lumpinfo[lump_p].size = fileinfo[fileinfo_p].size;
        }

    }

    //
    // W_InitMultipleFiles
    // Pass a null terminated list of files to use.
    // All files are optional, but at least one file
    // must be found.
    // Files with a .wad extension are idlink files
    // with multiple lumps.
    // Other files are single lumps with the base filename
    // for the lump name.
    // Lump names can appear multiple times.
    // The name searcher looks backwards, so a later file
    // does override all earlier ones.
    //
    public void InitMultipleFiles(String[] filenames)
            throws Exception {
        int size;

        // open all the files, load headers, and count lumps
        numlumps = 0;

        // will be realloced as lumps are added
        lumpinfo = new lumpinfo_t[0];

        for (String s : filenames)
            this.AddFile(s);

        if (numlumps == 0)
            system.Error("W_InitFiles: no files found");

        // set up caching
        size = numlumps;
        lumpcache = new CacheableDoomObject[size];
        preloaded = new boolean[size];

        if (lumpcache == null)
            system.Error("Couldn't allocate lumpcache");

        // memset (lumpcache,0, size);
    }

    //
    // W_InitFile
    // Just initialize from a single file.
    //
    public void InitFile(String filename)
            throws Exception {
        String[] names = new String[1];

        names[0] = filename;
//        names[1] = null;
        InitMultipleFiles(names);
    }

    //
    // W_NumLumps
    //
    public int NumLumps() {
        return numlumps;
    }

    //
    // W_CheckNumForName
    // Returns -1 if name not found.
    //

    public int CheckNumForName(String name) {

        int v1;
        int v2;
        // lumpinfo_t lump_p;

        int lump_p;
        // make the name into two integers for easy compares
        // case insensitive
        name8 union = new name8(strupr(name));

        v1 = union.x[0];
        v2 = union.x[1];

        // scan backwards so patch lump files take precedence
        lump_p = numlumps;

        while (lump_p-- != 0) {
            int a = name8.stringToInt(lumpinfo[lump_p].name, 0);
            int b = name8.stringToInt(lumpinfo[lump_p].name, 4);
            if ((a == v1) && (b == v2)) {
                return lump_p;
            }
        }

        // TFB. Not found.
        return -1;
    }

    /**
     * Returns actual lumpinfo_t object for a given name. Useful if you want to
     * access something on a file, I guess?
     * 
     * @param name
     * @return
     */
    public lumpinfo_t GetLumpinfoForName(String name) {

        int v1;
        int v2;
        // lumpinfo_t lump_p;

        int lump_p;
        // make the name into two integers for easy compares
        // case insensitive
        name8 union = new name8(strupr(name));

        v1 = union.x[0];
        v2 = union.x[1];

        // scan backwards so patch lump files take precedence
        lump_p = numlumps;

        while (lump_p-- != 0) {
            int a = name8.stringToInt(lumpinfo[lump_p].name, 0);
            int b = name8.stringToInt(lumpinfo[lump_p].name, 4);
            if ((a == v1) && (b == v2)) {
                return lumpinfo[lump_p];
            }
        }

        // TFB. Not found.
        return null;
    }

    //
    // W_GetNumForName
    // Calls W_CheckNumForName, but bombs out if not found.
    //
    public int GetNumForName(String name) {
        int i;

        i = CheckNumForName(name);

        if (i == -1)
            system.Error("W_GetNumForName: %s not found!", name);

        return i;
    }

    //
    // W_LumpLength
    // Returns the buffer size needed to load the given lump.
    //
    public int LumpLength(int lump) {
        if (lump >= numlumps)
            i.system.Error("W_LumpLength: %i >= numlumps", lump);

        return (int) lumpinfo[lump].size;
    }

    /**
     * W_ReadLump Loads the lump into the given buffer, which must be >=
     * W_LumpLength().
     * 
     * @throws IOException
     */
    public void ReadLump(int lump, ByteBuffer dest)
            throws IOException {
        int c;
        lumpinfo_t l; // Maes: was *..probably not array.
        DoomFile handle = null;

        if (lump >= this.numlumps) {
            system.Error("W_ReadLump: %i >= numlumps", lump);
            return;
        }

        l = lumpinfo[lump];

        // ??? I_BeginRead ();

        if (l.handle == null) {
            // reloadable file, so use open / read / close
            try {
                handle = new DoomFile(this.reloadname, "r");
            } catch (Exception e) {
                system.Error("W_ReadLump: couldn't open %s", reloadname);
            }
        } else
            handle = l.handle;

        handle.seek(l.position);
        byte[] buf = new byte[(int) l.size];
        c = handle.read(buf);
        dest.put(buf);

        if (c < l.size)
            system.Error("W_ReadLump: only read %i of %i on lump %i", c,
                l.size, lump);

        if (l.handle == null)
            handle.close();

        // ??? I_EndRead ();

    }

    /**
    * W_CacheLumpNum
    */
    public Object CacheLumpNum(int lump, int tag, Class what)
            throws IOException {
        // byte* ptr;

        if (lump >= numlumps) {
            system.Error("W_CacheLumpNum: %i >= numlumps", lump);
        }

        // Nothing cached here...
        if ((lumpcache[lump] == null)) {

            // read the lump in

            System.out.println("cache miss on lump "+lump);
            // ptr = Z_Malloc (W_LumpLength (lump), tag, &lumpcache[lump]);
            // Read as a byte buffer anyway.
            ByteBuffer thebuffer = ByteBuffer.allocate(this.LumpLength(lump));
            ReadLump(lump, thebuffer);

            // Class type specified

            if (what != null) {
                try {
                    // Can it be uncached? If so, deserialize it.

                    if (implementsInterface(what, w.CacheableDoomObject.class)) {
                        lumpcache[lump] =
                            (CacheableDoomObject) what.newInstance();

                        ((CacheableDoomObject) lumpcache[lump])
                                .unpack((ByteBuffer) thebuffer);
                        // replace lump with parsed object.
                    } else {
                        lumpcache[lump] = (CacheableDoomObject) thebuffer;
                    }
                } catch (Exception e) {
                    System.err.println("Could not auto-instantiate lump "
                            + lump + " of class " + what);
                    e.printStackTrace();
                }

            }
        } else {
            System.out.println("cache hit on lump " + lump);
            // Z.ChangeTag (lumpcache[lump],tag);
        }

        return lumpcache[lump];
    }

    /**
     * Read a lump into an object array, if possible. The binary blob lump will still be
     * cached as usual, but as a ByteBuffer this time, and deserialization
     * will be performed into the given Object[] array. Helps keep syntax compact. 
     * 
     */
     public void CacheLumpNumIntoArray(int lump, int tag, Object[] array, Class what)
             throws IOException {

         if (lump >= numlumps) {
             system.Error("W_CacheLumpNum: %i >= numlumps", lump);
         }

         // Nothing cached here...
         if ((lumpcache[lump] == null)) {

             // read the lump in

             System.out.println("cache miss on lump "+lump);
             // ptr = Z_Malloc (W_LumpLength (lump), tag, &lumpcache[lump]);
             // Read as a byte buffer anyway.
             ByteBuffer thebuffer = ByteBuffer.allocate(this.LumpLength(lump));
             ReadLump(lump, thebuffer);
             // Store the buffer anyway (as a DoomBuffer)
             lumpcache[lump] = new DoomBuffer(thebuffer);
            
         } else {
             System.out.println("cache hit on lump " + lump);
             // Z.ChangeTag (lumpcache[lump],tag);
         }

         // Class type specified. If the previously cached stuff is a "DoomBuffer" we can go on.

         if ((what != null)&&(lumpcache[lump].getClass()==DoomBuffer.class)) {
             try {
                 // Can it be uncached? If so, deserialize it. FOR EVERY OBJECT.
                 ByteBuffer b=((DoomBuffer)(lumpcache[lump])).getBuffer();
                 b.rewind();
                 
                 for (int i=0;i<array.length;i++){
                 if (implementsInterface(what, w.CacheableDoomObject.class)) {
                     ((CacheableDoomObject)array[i]).unpack(b);
                 }
                 }
             } catch (Exception e) {
                 System.err.println("Could not auto-unpack lump "
                         + lump + " into an array of objects of class " + what);
                 e.printStackTrace();
             }

         }
         
         return;
     }
    
    private boolean implementsInterface(Class what, Class which) {
        Class[] shit = what.getInterfaces();
        for (int i = 0; i < shit.length; i++) {
            if (shit[i].equals(which))
                return true;
        }
        // TODO Auto-generated method stub
        return false;
    }

    
    

    /** Return a cached lump based on its name, and for a specificc
     *  class. This will autoload it too, and should be the preferred
     *  method of loading stuff.
     * 
     * @param name
     * @param tag
     * @param what
     * @return
     */
    
    public DoomBuffer CacheLumpName(String name, int tag)
    {
try {
    return (DoomBuffer) this.CacheLumpNum(this.GetNumForName(name), tag, DoomBuffer.class);
} catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    return null;
}
}
    
    /** Specific method for loading cached patches, since it's by FAR the most common operation.
     * 
     * @param name
     * @return
     */
    
    public patch_t CachePatchName(String name)
    {
try {
    return (patch_t) this.CacheLumpNum(this.GetNumForName(name), PU_CACHE, patch_t.class);
} catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    return null;
}
}
    
    public Object CacheLumpName(String name, int tag, Class what)
            {
        try {
            return this.CacheLumpNum(this.GetNumForName(name), tag, what);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    //
    // W_Profile
    //
    char[][] info = new char[2500][10];

    int profilecount;

    void Profile()
            throws IOException {
        int i;
        memblock_t block = null;
        Object ptr;
        char ch;
        FileWriter f;
        int j;
        String name;

        for (i = 0; i < numlumps; i++) {
            ptr = lumpcache[i];
            if ((ptr == null)) {
                ch = ' ';
                continue;
            } else {
                // block = (memblock_t *) ( (byte *)ptr - sizeof(memblock_t));
                if (block.tag < PU_PURGELEVEL)
                    ch = 'S';
                else
                    ch = 'P';
            }
            info[i][profilecount] = ch;
        }
        profilecount++;

        f = new FileWriter(new File("waddump.txt"));
        // name[8] = 0;

        for (i = 0; i < numlumps; i++) {
            name = lumpinfo[i].name;

            f.write(name);

            for (j = 0; j < profilecount; j++)
                f.write("    " + info[i][j]);

            f.write("\n");
        }
        f.close();
    }

}