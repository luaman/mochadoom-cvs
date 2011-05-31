package i;

import v.VideoScaleInfo;
import doom.CommandLine;
import doom.DoomMain;
import doom.ICommandLineManager;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
//$Id: Main.java,v 1.8 2011/05/31 16:27:14 velktron Exp $
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
    static final String rcsid = "$Id: Main.java,v 1.8 2011/05/31 16:27:14 velktron Exp $";

    public static void main(String[] argv){

    	
    	  // These are the most essential
          DoomMain D=new DoomMain();
  
          
          ICommandLineManager CM=new CommandLine(argv);
          
          // 
          D.setCommandLineArgs(CM);
          
          // Create AWT frame, but don't start it yet.
          D.Init();
          
          D.Start (); 

          return;
        } 
    }
