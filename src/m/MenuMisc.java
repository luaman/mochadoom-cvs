package m;

import hu.HU;
import i.DoomSystem;
import i.IDoomSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import doom.DoomStatus;
import doom.IDoomGame;
import rr.RendererState;
import s.IDoomSound;
import timing.ITicker;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import w.DoomFile;
import w.IWadLoader;
import w.IWritableDoomObject;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: MenuMisc.java,v 1.22 2011/05/31 09:57:45 velktron Exp $
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
//
// DESCRIPTION:
//	Main loop menu stuff.
//	Default Config File.
//	PCX Screenshots.
//
//-----------------------------------------------------------------------------

public abstract class MenuMisc{

    public static final String rcsid = "$Id: MenuMisc.java,v 1.22 2011/05/31 09:57:45 velktron Exp $";

    public static String defaultfile;
    public static String basedefault="default.cfg";

    //
    // M_SaveDefaults
    //
    public static void SaveDefaults (DoomStatus DS)
    {
        int     i;
        int     v;
        OutputStream f;
        PrintStream  ps;

        try {
            f = new FileOutputStream(defaultfile);
        } catch (FileNotFoundException e) {
            // can't write the file, but don't complain
            return;
        }

        ps=new PrintStream(f);
        for (Settings set :Settings.values())
        {
            if (set.defaultvalue > -0xfff
                    && set.defaultvalue < 0xfff)
            {

                if (set.numeric)
                    ps.printf("%s\t\t%d\n",set.name(),set.setvalue);
                else
                    ps.printf("%s\t\t\"%s\"\n",set.name(),set.setszvalue);             
                //* TODO: will need another big switch thing to 
                // properly save CURRENT settings to the file.
            }
        }
        try {
            f.close();}
        catch (IOException e) {
            // Well duh....
            return;
        }
    }

    //
    // M_LoadDefaults
    //

    public static void LoadDefaults (DoomStatus DS)
    {
        int		i;
        int		len;
        DoomFile	f;
        String	def;
        String	strparm;
        String	newstring;
        int		parm;
        boolean	isstring;

        // set everything to base values
        int numdefaults = Settings.values().length;
        for (i=0 ; i<numdefaults ; i++)
            Settings.values()[i].applySetting(DS);

        // check for a custom default file
        i = DS.CM.CheckParm("-config");
        if ((i>0) && i<DS.CM.getArgc()-1)
        {
            defaultfile = DS.CM.getArgv(i+1);
            System.out.printf("	default file: %s\n",defaultfile);
        }
        else
            defaultfile = basedefault;

        try {

            // read the file in, overriding any set defaults
            f = new DoomFile (defaultfile, "r");
            if (f!=null)
            {
                long endfile=f.length()-1;
                String name = null, value = null;

                while (f.getFilePointer()<endfile)
                {
                    // Let's make this the sane way...read a string.
                    String s=f.readLine();
                    isstring = false;
                    StringTokenizer tk=new StringTokenizer(s);

                    // They should be exactly two.
                    int tokens=tk.countTokens();
                    if (tokens==2){
                        name=tk.nextToken();
                        value=tk.nextToken();
                        String tmp=C2JUtils.unquote(value,'"');
                        if (tmp!=null) value=tmp;
                    } else if (tokens>2){
                        // String tokenizer can't recognize quoted strings
                        name=tk.nextToken();
                        value = C2JUtils.unquote(s,'"');
                        if (value==null) continue;

                    }

                        // All settings should be lower case.
                        name=name.toLowerCase();
                        System.out.printf("NAME: %s VALUE: %s\n",name,value);
                        Settings setme;
                        try {
                            setme=Enum.valueOf(Settings.class,name);
                        } catch (IllegalArgumentException e){
                            // Not a known type, nothing to set.
                            System.err.printf("Unknown setting: %s\n",name);
                            // Next while.
                            continue;
                        }

                        // OK, so it's a known setting. Value?

                        if (value.charAt(0)=='"'){

                            // get a string default
                            setme.setszvalue=value;
                            if (!setme.numeric)
                                setme.applySetting(DS);
                        } else {
                            
                            boolean fail=false;
                            try {
                                // Try it as an integer.
                                setme.setvalue= Integer.parseInt(value);
                            } catch (NumberFormatException e){
                                fail=true;
                            }
                            
                            if (fail)
                             try {    
                                  // Try it as a hex value. Must be preceded by 0x
                                 fail=false;
                                 int hexpos=value.toLowerCase().indexOf("0x");
                                 if (hexpos>-1){
                                     String substring=value.substring(hexpos+2);
                                     setme.setvalue= Integer.parseInt(substring,16);
                                 } else 
                                 fail=true;
                             } catch (NumberFormatException e){
                                 fail=true;
                             }
                             
                             if (fail)
                                 try {
                                      // Try it as a character. Must be quoted for
                                     // this to work, with a length of 3.
                                     fail=false;
                                     System.out.printf("Trying %s\n",value);
                                     if (value.length()==3 && 
                                         value.charAt(0)=='\'' &&
                                         value.charAt(2)=='\'') {
                                      setme.setvalue= value.charAt(1);
                                     System.out.printf("%s set to value %d or character %c\n",setme.name(),setme.setvalue,value.charAt(1));
                                     } else continue;
                                 } catch (NumberFormatException e){
                                     fail=true;
                                        }
                                 
                                 if (fail) {
                                System.err.printf("Cannot parse expected numerical setting: %s of %s\n",value,name);
                                // Next while.
                                continue;
                                 }
                            }
                            if (setme.numeric){
                                
                                setme.applySetting(DS);
                            }
                } // end-while


                f.close();
            } // not null
        }catch (IOException e){
            System.err.printf("I just can't read the settings file %s, will use defaults.\n",defaultfile);
        }
    }


    //
    // SCREEN SHOTS
    //

    /*
typedef struct
{
    char		manufacturer;
    char		version;
    char		encoding;
    char		bits_per_pixel;

    unsigned short	xmin;
    unsigned short	ymin;
    unsigned short	xmax;
    unsigned short	ymax;

    unsigned short	hres;
    unsigned short	vres;

    unsigned char	palette[48];

    char		reserved;
    char		color_planes;
    unsigned short	bytes_per_line;
    unsigned short	palette_type;

    char		filler[58];
    unsigned char	data;		// unbounded
} pcx_t;
     */

    //
    // WritePCXfile
    //

    /*
public void
WritePCXfile
( String		filename,
  byte[]		data,
  int		width,
  int		height,
  byte[]		palette )
{
    int		i;
    int		length;
    pcx_t	pcx;
    byte[]	pack=new byte[width*height*2];
    int packlen;

//    pcx = new byteZ_Malloc (width*height*2+1000, PU_STATIC, NULL);

    pcx.manufacturer = 0x0a;		// PCX id
    pcx.version = 5;			// 256 color
    pcx.encoding = 1;			// uncompressed
    pcx.bits_per_pixel = 8;		// 256 color
    pcx.xmin = 0;
    pcx.ymin = 0;
    pcx.xmax = (char) (width-1);
    pcx.ymax = (char) (height-1);
    pcx.hres = (char) width;
    pcx.vres = (char) height;
    Arrays.fill(pcx.palette, (char)0);
    //memset (pcx.palette,0,sizeof(pcx->palette));
    pcx.color_planes = 1;		// chunky image
    pcx.bytes_per_line = (char) width;
    pcx.palette_type = 2;	// not a grey scale
    Arrays.fill(pcx.filler,(char) 0);
    //memset (pcx.filler,0,sizeof(pcx->filler));


    // pack the image


    for (i=0 ; i<width*height ; i++)
    {
	if ( (data[i] & 0xc0) != 0xc0)
	    pack[packlen++] = data[i];
	else
	{
	    pack[packlen++] = (byte) 0xc1;
	    pack[packlen++] = data[i];
	}
    }

    // write the palette
    pack[packlen++] = 0x0c;	// palette ID byte
    for (i=0 ; i<768 ; i++)
        pack[packlen++] = palette[i];

    // write output file
    length = packlen;
    WriteFile (filename, pcx, length);

    //Z_Free (pcx);
}
     */

    //
    // M_ScreenShot
    //
    /*

public void ScreenShot ()
{
    int		i;
    byte[]	linear;
    String	lbmname;

    // munge planar buffer to linear
    linear = V.screens[2];
    I_ReadScreen (linear);

    // find a file name to save it to
    strcpy(lbmname,"DOOM00.pcx");

    for (i=0 ; i<=99 ; i++)
    {
	lbmname[4] = i/10 + '0';
	lbmname[5] = i%10 + '0';
	if (access(lbmname,0) == -1)
	    break;	// file doesn't exist
    }
    if (i==100)
	I_Error ("M_ScreenShot: Couldn't create a PCX");

    // save the pcx file
    WritePCXfile (lbmname, linear,
		  SCREENWIDTH, SCREENHEIGHT,
		  W_CacheLumpName ("PLAYPAL",PU_CACHE));

    players[consoleplayer].message = "screen shot";
}
     */

    public static boolean WriteFile(String name, byte[] source, int length) {
        DoomFile handle;
        try {
            handle = new DoomFile(name, "rw");

            if (handle == null)
                return false;

            handle.write(source, 0, length);
            handle.close();
        } catch (Exception e) {
            DoomSystem.MiscError("Couldn't write file %s (%s)", name, e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean WriteFile(String name, IWritableDoomObject source) {
        DoomFile handle;
        try {
            handle = new DoomFile(name, "rw");
            source.write(handle);
            handle.close();
        } catch (Exception e) {
            DoomSystem.MiscError("Couldn't write file %s (%s)", name, e.getMessage());
            return false;
        }

        return true;
    }


    /** M_ReadFile 
     *  This version returns a variable-size ByteBuffer, so
     *  we don't need to know a-priori how much stuff to read.
     * 
     */
    public static ByteBuffer ReadFile(String name) {
        DoomFile handle;
        int count, length;
        // struct stat fileinfo;
        ByteBuffer buf;
        try {
            handle = new DoomFile(name, "r");
            length = (int) handle.length();
            buf = ByteBuffer.allocate(length);
            count = handle.read(buf.array());
            handle.close();
        } catch (Exception e) {
            DoomSystem.MiscError("Couldn't read file %s (%s)", name, e.getMessage());
            return null;
        }

        return buf;
    }

    /** M_ReadFile */
    public static int ReadFile(String name, byte[] buffer) {
        DoomFile handle;
        int count, length;
        // struct stat fileinfo;
        byte[] buf;
        try {
            handle = new DoomFile(name, "r");
            length = (int) handle.length();
            buf = new byte[length];
            count = handle.read(buf);
            handle.close();

            if (count < length)
                throw new Exception("Read only " + count + " bytes out of "
                    + length);

        } catch (Exception e) {
            DoomSystem.MiscError("Couldn't read file %s (%s)", name, e.getMessage());
            return -1;
        }
        System.arraycopy(buf, 0, buffer, 0, length);
        return length;
    }

    public abstract boolean getShowMessages();

    public abstract void setShowMessages(boolean val);

}

// $Log: MenuMisc.java,v $
// Revision 1.22  2011/05/31 09:57:45  velktron
// Fixed broken parsing of unspaced strings. It's never fun having to come up with your own function for string manipulation!
//
// Revision 1.21  2011/05/30 15:46:50  velktron
// AbstractDoomMenu implemented.
//
// Revision 1.20  2011/05/26 17:54:16  velktron
// Removed some Menu verbosity, better defaults functionality.
//
// Revision 1.19  2011/05/26 13:39:15  velktron
// Now using ICommandLineManager
//
// Revision 1.18  2011/05/24 17:46:03  velktron
// Added vanilla default.cfg loading.
//