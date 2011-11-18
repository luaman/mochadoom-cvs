package m;

import i.DoomSystem;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import doom.DoomStatus;
import utils.C2JUtils;
import w.IWritableDoomObject;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: MenuMisc.java,v 1.28.2.2 2011/11/18 21:37:59 velktron Exp $
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

    public static final String rcsid = "$Id: MenuMisc.java,v 1.28.2.2 2011/11/18 21:37:59 velktron Exp $";

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
        BufferedReader	in;
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
            //System.out.printf("	default file: %s\n",defaultfile);
        }
        else
            defaultfile = basedefault;

        try {

            // read the file in, overriding any set defaults
            in = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(defaultfile))));
            if (in!=null)
            {
                String name = null, value = null;
                String s;
                
                // Let's make this the sane way...read a string.
                while (( s=in.readLine())!=null)
                {

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
                        //System.out.printf("NAME: %s VALUE: %s\n",name,value);
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
                                     //System.out.printf("Trying %s\n",value);
                                     if (value.length()==3 && 
                                         value.charAt(0)=='\'' &&
                                         value.charAt(2)=='\'') {
                                      setme.setvalue= value.charAt(1);
                                     //System.out.printf("%s set to value %d or character %c\n",setme.name(),setme.setvalue,value.charAt(1));
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


                in.close();
            } // not null
        }catch (IOException e){
            System.err.printf("I just can't read the settings file %s, will use defaults.\n",defaultfile);
        }
    }


    //
    // SCREEN SHOTS
    //
  
    public static boolean WriteFile(String name, byte[] source, int length) {
        OutputStream handle;
        try {
            handle = new  FileOutputStream(name);
            handle.write(source, 0, length);
            handle.close();
        } catch (Exception e) {
            DoomSystem.MiscError("Couldn't write file %s (%s)", name, e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean WriteFile(String name, IWritableDoomObject source) {
        DataOutputStream handle;
        try {
            handle = new DataOutputStream(new FileOutputStream(name));
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
        BufferedInputStream handle;
        int count, length;
        // struct stat fileinfo;
        ByteBuffer buf;
        try {
            handle = new BufferedInputStream(new FileInputStream(name));
            length = (int) handle.available();
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
    	BufferedInputStream handle;
        int count, length;
        // struct stat fileinfo;
        byte[] buf;
        try {
            handle = new BufferedInputStream(new FileInputStream(name));
            length = (int) handle.available();
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
        System.arraycopy(buf, 0, buffer, 0, Math.min(count,buffer.length));
        return length;
    }

    //
 // WritePCXfile
 //
 public static void
 WritePCXfile
 ( String        filename,
   byte[]     data,
   int       width,
   int       height,
   byte[]     palette )
 {
     int     i;
     int     length;
     pcx_t  pcx;
     byte[]   pack;
     
     pcx = new pcx_t();
     pack=new byte[width*height*2]; // allocate that much data, just in case.

     pcx.manufacturer = 0x0a;       // PCX id
     pcx.version = 5;           // 256 color
     pcx.encoding = 1;          // uncompressed
     pcx.bits_per_pixel = 8;        // 256 color
     pcx.xmin = 0;
     pcx.ymin = 0;
     pcx.xmax = (char) (width-1);
     pcx.ymax = (char) (height-1);
     pcx.hres = (char) width;
     pcx.vres = (char) height;
     // memset (pcx->palette,0,sizeof(pcx->palette));
     pcx.color_planes = 1;      // chunky image
     pcx.bytes_per_line = (char) width;
     pcx.palette_type = 2;   // not a grey scale
     //memset (pcx->filler,0,sizeof(pcx->filler));


     // pack the image
     //pack = &pcx->data;
     int p_pack=0;
     int p_data=0;
     
     for (i=0 ; i<width*height ; i++)
     {
     if ( (data[i] & 0xc0) != 0xc0)
         pack[p_pack++] = data[i];
     else
     {
         pack[p_pack++] = (byte) 0xc1;
         pack[p_pack++] = data[i];
     }
     }
     
     // write the palette
     pack[p_pack++] = 0x0c; // palette ID byte
     for (i=0 ; i<768 ; i++)
         pack[p_pack++] = palette[i];
     
     // write output file
     length = p_pack;
     pcx.data=Arrays.copyOf(pack, length);
     
     DataOutputStream f=null;
    try {
        f = new DataOutputStream(new FileOutputStream(filename));
        
    } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
    try {
        //f.setLength(0);
        pcx.write(f);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
     }
    
    public abstract boolean getShowMessages();

    public abstract void setShowMessages(boolean val);

	public static void WritePNGfile(String imagename, short[] linear,
			int width, int height) {
		
		BufferedImage buf=new BufferedImage(width,height,BufferedImage.TYPE_USHORT_555_RGB);
		DataBufferUShort sh=(DataBufferUShort) buf.getRaster().getDataBuffer();
		short[] shd=sh.getData();
		System.arraycopy(linear,0,shd,0,Math.min(linear.length,shd.length));
		try {
			ImageIO.write(buf, "PNG",new File(imagename));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		
	}

}

// $Log: MenuMisc.java,v $
// Revision 1.28.2.2  2011/11/18 21:37:59  velktron
// Saves PNGs now.
//
// Revision 1.28.2.1  2011/11/14 00:27:11  velktron
// A barely functional HiColor branch. Most stuff broken. DO NOT USE
//
// Revision 1.28  2011/10/25 19:52:03  velktron
// Using buffered I/O when possible
//
// Revision 1.27  2011/10/24 02:11:27  velktron
// Stream compliancy
//
// Revision 1.26  2011/07/30 22:04:30  velktron
// Removed unused imports (including one that would cause problems compiling with OpenJDK).
//
// Revision 1.25  2011/07/15 13:53:52  velktron
// Implemented WritePCXFile, at last.
//
// Revision 1.24  2011/06/03 16:37:09  velktron
// Readfile will only read at most as much as the buffer allows.
//
// Revision 1.23  2011/05/31 13:33:54  velktron
// -verbosity
//
// Revision 1.22  2011/05/31 09:57:45  velktron
// Fixed broken parsing of unspaced strings. 
// It's never fun having to come up with your own function for string manipulation!
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