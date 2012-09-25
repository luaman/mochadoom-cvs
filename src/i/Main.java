package i;

import java.io.IOException;

import rr.LightsAndColors;

import v.VideoScaleInfo;
import doom.CommandLine;
import doom.DoomMain;
import doom.ICommandLineManager;

//Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
//$Id: Main.java,v 1.12 2012/09/25 16:35:38 velktron Exp $
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
//
//DESCRIPTION:
//Main program, simply calls D_DoomMain high level loop.
//
//-----------------------------------------------------------------------------



public class Main {
    static final String rcsid = "$Id: Main.java,v 1.12 2012/09/25 16:35:38 velktron Exp $";

    public static BppMode bpp;
    
    public static void main(String[] argv) throws IOException{

    	//  First, get the command line parameters.
            ICommandLineManager CM=new CommandLine(argv);
        
          bpp= BppMode.Indexed;
            
          if (CM.CheckParmBool("-hicolor")) bpp=BppMode.HiColor;
              else
          if (CM.CheckParmBool("-truecolor")) bpp=BppMode.TrueColor;
          
          // Here he create DOOM
          DoomMain<?, ?> DM=null;
          // Create a dummy. This will force static init to run.
          //LightsAndColors LAC=new LightsAndColors();
          
          
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
