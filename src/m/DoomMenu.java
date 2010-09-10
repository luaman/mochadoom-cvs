package m;

import doom.event_t;

// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: DoomMenu.java,v 1.2 2010/09/10 17:35:49 velktron Exp $
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
// Menu widget stuff, episode selection and such.
//    
// -----------------------------------------------------------------------------

/**
 * 
 */

public interface DoomMenu {

    //
    // MENUS
    //

    /**
     * Called by main loop, saves config file and calls I_Quit when user exits.
     * Even when the menu is not displayed, this can resize the view and change
     * game parameters. Does all the real work of the menu interaction.
     */
    public boolean Responder(event_t ev);

    /**
     * Called by main loop, only used for menu (skull cursor) animation.
     */
    public void Ticker();

    /**
     * Called by main loop, draws the menus directly into the screen buffer.
     */
    public void Drawer();

    /**
     * Called by D_DoomMain, loads the config file.
     */
    public void Init();

    /**
     * Called by intro code to force menu up upon a keypress, does nothing if
     * menu is already up.
     */
    public void StartControlPanel();

    int getShowMessages();

    void setShowMessages(int val);
}