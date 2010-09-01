package m;

// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: MenuBarnacles.java,v 1.1 2010/09/01 15:53:42 velktron Exp $
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
//
//    
// -----------------------------------------------------------------------------
public interface MenuBarnacles {

    public boolean WriteFile(String name, byte[] source, int length);

    public int ReadFile(String name, byte[] buffer);

    public void ScreenShot();

    public void LoadDefaults();

    public void SaveDefaults();

    public int DrawText(int x, int y, boolean direct, String string);
}
