package i;

import java.io.IOException;

import v.VideoScaleInfo;
import doom.CommandLine;
import doom.DoomMain;
import doom.ICommandLineManager;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
//$Id: Main.java,v 1.11 2012/09/24 17:16:22 velktron Exp $
//
//Copyright (C) 1993-1996 by id Software, Inc.
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//$Log: Main.java,v $
//Revision 1.11  2012/09/24 17:16:22  velktron
//Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
//Revision 1.10.2.2  2012/09/24 16:57:43  velktron
//Addressed generics warnings.
//
//Revision 1.10.2.1  2012/09/20 14:18:06  velktron
//NEW and ENHANCED dual-function main!!!
//
//Revision 1.10  2011/10/24 02:11:27  velktron
//Stream compliancy
//
//Revision 1.9  2011/06/01 00:08:35  velktron
//Downsized to essentiality.
//
//Revision 1.8  2011/05/31 16:27:14  velktron
//Resolution not handled here anymore.
//
//Revision 1.7  2011/05/30 15:51:32  velktron
//*** empty log message ***
//
//Revision 1.6  2011/05/26 13:39:26  velktron
//Now using ICommandLineManager
//
//Revision 1.5  2011/05/25 17:56:30  velktron
//just played a bit with resolutions.
//
//Revision 1.4  2011/05/23 16:59:59  velktron
//Migrated to VideoScaleInfo.
//
//Revision 1.3  2010/09/25 17:37:13  velktron
//Lots of changes.
//
//The most important is the creation of the AWTDoom frame handling I/O.
//
//Revision 1.2  2010/09/23 07:31:11  velktron
//fuck
//
//Revision 1.1  2010/09/22 16:40:02  velktron
//MASSIVE changes in the status passing model.
//DoomMain and DoomGame unified.
//Doomstat merged into DoomMain (now status and game functions are one).
//
//Most of DoomMain implemented. Possible to attempt a "classic type" start but will stop when reading sprites.
//
//
//DESCRIPTION:
//Main program, simply calls D_DoomMain high level loop.
//
//-----------------------------------------------------------------------------



public class Main {
    static final String rcsid = "$Id: Main.java,v 1.11 2012/09/24 17:16:22 velktron Exp $";

    public static void main(String[] argv) throws IOException{

    	//  First, get the command line parameters.
            ICommandLineManager CM=new CommandLine(argv);
        
          BppMode bpp=BppMode.Indexed;
            
          if (CM.CheckParmBool("-hicolor")) bpp=BppMode.HiColor;
              else
          if (CM.CheckParmBool("-truecolor")) bpp=BppMode.TrueColor;
          
          // Here he create DOOM
          DoomMain<?, ?> DM=null;
          
          switch(bpp){
          case Indexed:
              System.out.println("Indexed 8-bit mode selected...");
              DM=new DoomMain.Indexed();
              break;
          case HiColor:
              System.out.println("HiColor (Alpha) 16-bit mode selected...");
              DM=new DoomMain.HiColor();
              break;
          case TrueColor:
              System.out.println("TrueColor (extended colormaps) 24-bit mode selected...");
              DM=new DoomMain.TrueColor();
              break;

          }    

          DM.setCommandLineArgs(CM);
          DM.Init();
          DM.Start();

          return;
        } 
    }
