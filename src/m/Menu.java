package m;

import static data.Defines.HU_FONTSIZE;
import static data.Defines.HU_FONTSTART;
import static data.Defines.KEY_BACKSPACE;
import static data.Defines.KEY_DOWNARROW;
import static data.Defines.KEY_ENTER;
import static data.Defines.KEY_EQUALS;
import static data.Defines.KEY_ESCAPE;
import static data.Defines.KEY_F1;
import static data.Defines.KEY_F10;
import static data.Defines.KEY_F11;
import static data.Defines.KEY_F2;
import static data.Defines.KEY_F3;
import static data.Defines.KEY_F4;
import static data.Defines.KEY_F5;
import static data.Defines.KEY_F6;
import static data.Defines.KEY_F7;
import static data.Defines.KEY_F8;
import static data.Defines.KEY_F9;
import static data.Defines.KEY_LEFTARROW;
import static data.Defines.KEY_MINUS;
import static data.Defines.KEY_RIGHTARROW;
import static data.Defines.KEY_UPARROW;
import static data.Defines.PU_CACHE;
import static data.Defines.SAVESTRINGSIZE;
import static data.Defines.SCREENWIDTH;
import static data.dstrings.NUM_QUITMESSAGES;
import static data.dstrings.SAVEGAMENAME;
import static data.dstrings.endmsg;
import static doom.englsh.DOSY;
import static doom.englsh.EMPTYSTRING;
import static doom.englsh.ENDGAME;
import static doom.englsh.LOADNET;
import static doom.englsh.MSGOFF;
import static doom.englsh.MSGON;
import static doom.englsh.NETEND;
import static doom.englsh.NEWGAME;
import static doom.englsh.NIGHTMARE;
import static doom.englsh.QLOADNET;
import static doom.englsh.QLPROMPT;
import static doom.englsh.QSAVESPOT;
import static doom.englsh.QSPROMPT;
import static doom.englsh.SAVEDEAD;
import static doom.englsh.SWSTRING;

import java.io.IOException;

import rr.patch_t;
import utils.C2JUtils;
import w.DoomFile;
import data.Defines;
import data.Defines.GameMode_t;
import data.Defines.Language_t;
import data.Defines.gamestate_t;
import data.Defines.skill_t;
import data.sounds.sfxenum_t;
import doom.DoomContext;
import doom.event_t;
import doom.evtype_t;

public class Menu extends MenuMisc implements DoomMenu{


    ////////////////// CONSTRUCTOR ////////////////
    
    public Menu(DoomContext DC){
        this.V=DC.V;
        this.DS=DC.DS;
        this.W=DC.W;
        this.initMenuItems();
        this.initMenuRoutines();
        this.initDrawRoutines();
    }
    
    

    /** The fonts  */
    patch_t[] hu_font = new patch_t[HU_FONTSIZE];

    /** WTF?! */

    boolean message_dontfuckwithme;

    /** in heads-up code */
    
    boolean chat_on; 

    /** defaulted values */
    
    int mouseSensitivity; // has default

    /** Show messages has default, 0 = off, 1 = on */

    private int showMessages;

    /**
     * showMessages can be read outside of Menu, but not modified. Menu has the
     * actual C definition (not declaration)
     */
    
    @Override
    public int getShowMessages() {
        return showMessages;
    }

    @Override
    public void setShowMessages(int val) {
        this.showMessages=val;
    }
    
    /** Blocky mode, has default, 0 = high, 1 = normal */
    int detailLevel;

    int screenblocks; // has default

    /** temp for screenblocks (0-9) */
    int screenSize;

    /** -1 = no quicksave slot picked! */
    int quickSaveSlot;

    /** 1 = message to be printed */
    boolean messageToPrint;

    /** ...and here is the message string! */
    String messageString;

    /** message x & y */
    int messx, messy;

    boolean messageLastMenuActive;

    /** timed message = no input from user */
    boolean messageNeedsInput;

    /** TODO: probably I need some MessageRoutine interface at this point? */
    public MenuRoutine messageRoutine;


    /** we are going to be entering a savegame string */
    boolean saveStringEnter;

    int saveSlot; // which slot to save in

    int saveCharIndex; // which char we're editing

    /** old save description before edit */
    char[] saveOldString = new char[SAVESTRINGSIZE];

    boolean inhelpscreens;

    //int menuactive;

    protected static final int SKULLXOFF = -32;

    protected static final int LINEHEIGHT = 16;

    char[][] savegamestrings = new char[10][SAVESTRINGSIZE];

    String endstring = new String();

    //
    // MENU TYPEDEFS
    //

    /** menu item skull is on */
    short itemOn;

    /** skull animation counter */
    short skullAnimCounter;

    /** which skull to draw */
    short whichSkull;

    /**
     * graphic name of skulls warning: initializer-string for array of chars is
     * too long
     */
    private static String[] skullName = { "M_SKULL1", "M_SKULL2" };

    /** current menudef */
    // MAES: pointer? array?
    menu_t currentMenu;

    //
    // DOOM MENU
    //

    // MAES: was an enum called "main_e" used purely as numerals. No need for
    // strong typing.

    /**
     * MenuRoutine class definitions, replacing "function pointers".
     */
    MenuRoutine ChangeDetail, ChangeMessages, ChangeSensitivity, ChooseSkill,
            EndGame, EndGameResponse, Episode, FinishReadThis, LoadGame,
            LoadSelect, MusicVol, NewGame, Options, VerifyNightmare,
            SaveSelect, SfxVol, SizeDisplay, SaveGame, Sound, QuitDOOM,
            QuitResponse, QuickLoadResponse, ReadThis, ReadThis2;

    /** DrawRoutine class definitions, replacing "function pointers". */

    DrawRoutine DrawEpisode, DrawReadThis1, DrawReadThis2, DrawLoad,
            DrawMainMenu, DrawNewGame, DrawOptions, DrawSave, DrawSound;

    private void initMenuRoutines() {
        NewGame = new M_NewGame();
        Options = new M_Options();
        ChooseSkill = new M_ChooseSkill();
        Episode = new M_Episode();
        EndGame = new M_EndGame();
        EndGameResponse = new M_EndGameResponse();
        VerifyNightmare = new M_VerifyNightmare();
        EndGame = new M_EndGame();
        ChangeMessages = new M_ChangeMessages();
        ChangeDetail = new M_ChangeDetail();
        SizeDisplay = new M_SizeDisplay();
        ChangeSensitivity = new M_ChangeSensitivity();
        ReadThis = new M_ReadThis();
        ReadThis2 = new M_ReadThis2();
        Sound = new M_Sound();
        QuitDOOM = new M_QuitDOOM();
        QuickLoadResponse = new M_QuickLoadResponse();
        QuitResponse = new M_QuitResponse();
    }

    private void initDrawRoutines() {
        DrawEpisode = new M_DrawEpisode();
        DrawNewGame = new M_DrawNewGame();
        DrawReadThis1 = new M_DrawReadThis1();
        DrawReadThis2 = new M_DrawReadThis2();
        DrawOptions = new M_DrawOptions();

        DrawMainMenu = new M_DrawMainMenu();
    }

    /** Menuitem definitions. A "menu" can consist of multiple menuitems */
    menuitem_t[] MainMenu,EpisodeMenu,NewGameMenu, OptionsMenu,ReadMenu1,ReadMenu2,SoundMenu,LoadMenu,SaveMenu;
    
    /** Actual menus. Each can point to an array of menuitems */
    menu_t MainDef, EpiDef,NewDef,OptionsDef,ReadDef1, ReadDef2,SoundDef,LoadDef,SaveDef;
    
    private void initMenuItems(){
    
    MainMenu = new menuitem_t[]
        { new menuitem_t( 1, "M_NGAME", NewGame, 'n'),
                new menuitem_t(1, "M_OPTION", Options, 'o'),
                new menuitem_t(1, "M_LOADG", LoadGame, 'l'),
                new menuitem_t(1, "M_SAVEG", SaveGame, 's'),
                // Another hickup with Special edition.
                new menuitem_t(1, "M_RDTHIS", ReadThis, 'r'),
                new menuitem_t(1, "M_QUITG", QuitDOOM, 'q') };

    MainDef =
        new menu_t(main_end, null, MainMenu, DrawMainMenu, 97, 64, 0);

    //
    // EPISODE SELECT
    //

    EpisodeMenu = new menuitem_t[]
        { new menuitem_t(1, "M_EPI1", Episode, 'k'),
                new menuitem_t(1, "M_EPI2", Episode, 't'),
                new menuitem_t(1, "M_EPI3", Episode, 'i'),
                new menuitem_t(1, "M_EPI4", Episode, 't') };

    EpiDef = new menu_t(ep_end, // # of menu items
            MainDef, // previous menu
            EpisodeMenu, // menuitem_t ->
            DrawEpisode, // drawing routine ->
            48, 63, // x,y
            ep1 // lastOn
        );

    //
    // NEW GAME
    //


     NewGameMenu  = new menuitem_t[]
        { new menuitem_t(1, "M_JKILL", ChooseSkill, 'i'),
                new menuitem_t(1, "M_ROUGH", ChooseSkill, 'h'),
                new menuitem_t(1, "M_HURT", ChooseSkill, 'h'),
                new menuitem_t(1, "M_ULTRA", ChooseSkill, 'u'),
                new menuitem_t(1, "M_NMARE", ChooseSkill, 'n') };

     NewDef = new menu_t(newg_end, // # of menu items
            EpiDef, // previous menu
            NewGameMenu, // menuitem_t ->
            DrawNewGame, // drawing routine ->
            48, 63, // x,y
            hurtme // lastOn
        );

    //
    // OPTIONS MENU
    //

    OptionsMenu = new  menuitem_t[]
        { new menuitem_t(1, "M_ENDGAM", EndGame, 'e'),
                new menuitem_t(1, "M_MESSG", ChangeMessages, 'm'),
                new menuitem_t(1, "M_DETAIL", ChangeDetail, 'g'),
                new menuitem_t(2, "M_SCRNSZ", SizeDisplay, 's'),
                new menuitem_t(-1, "", null),
                new menuitem_t(2, "M_MSENS", ChangeSensitivity, 'm'),
                new menuitem_t(-1, "", null),
                new menuitem_t(1, "M_SVOL", Sound, 's') };

    OptionsDef =
        new menu_t(opt_end, this.MainDef, OptionsMenu, DrawOptions, 60, 37, 0);

    // Read This! MENU 1 

    ReadMenu1 = new menuitem_t[] { new menuitem_t(1, "", ReadThis2, (char) 0) };

    ReadDef1 =
        new menu_t(read1_end, MainDef, ReadMenu1, DrawReadThis1, 280, 185, 0);

    // Read This! MENU 1

    ReadMenu2 = new menuitem_t[] 
        { new menuitem_t(1, "", FinishReadThis, (char) 0) };

    ReadDef2 =
        new menu_t(read2_end, ReadDef1, ReadMenu2, DrawReadThis2, 330, 175, 0);

    //
    // SOUND VOLUME MENU
    //



    SoundMenu = new menuitem_t[]
        { new menuitem_t(2, "M_SFXVOL", SfxVol, 's'),
                new menuitem_t(-1, "", null),
                new menuitem_t(2, "M_MUSVOL", MusicVol, 'm'),
                new menuitem_t(-1, "", null) };

    SoundDef =
        new menu_t(sound_end, OptionsDef, SoundMenu, DrawSound, 80, 64, 0);

    //
    // LOAD GAME MENU
    //

    LoadMenu =new menuitem_t[]
        { new menuitem_t(1, "", LoadSelect, '1'),
                new menuitem_t(1, "", LoadSelect, '2'),
                new menuitem_t(1, "", LoadSelect, '3'),
                new menuitem_t(1, "", LoadSelect, '4'),
                new menuitem_t(1, "", LoadSelect, '5'),
                new menuitem_t(1, "", LoadSelect, '6') };

    LoadDef =
        new menu_t(load_end, MainDef, LoadMenu, DrawLoad, 80, 54, 0);

    //
    // SAVE GAME MENU
    //
    SaveMenu = new menuitem_t[]
        { new menuitem_t(1, "", SaveSelect, '1'),
                new menuitem_t(1, "", SaveSelect, '2'),
                new menuitem_t(1, "", SaveSelect, '3'),
                new menuitem_t(1, "", SaveSelect, '4'),
                new menuitem_t(1, "", SaveSelect, '5'),
                new menuitem_t(1, "", SaveSelect, '6') };

    SaveDef =
        new menu_t(load_end, MainDef, SaveMenu, DrawSave, 80, 54, 0);
    }
    
    /**
     * M_ReadSaveStrings
     * read the strings from the savegame files
     */
    
    public void ReadSaveStrings() {
        DoomFile handle;
        int count;
        int i;
        String name;

        for (i = 0; i < load_end; i++) {
            if (this.CheckParm("-cdrom") != 0)
                name = "c:\\doomdata\\" + SAVEGAMENAME + (i) + ".dsg";
            else
                name = SAVEGAMENAME + (i) + ".dsg";

            try {
                handle = new DoomFile(name, "r");
                savegamestrings[i] =
                    handle.readString(SAVESTRINGSIZE).toCharArray();
                handle.close();
                LoadMenu[i].status = 1;
            } catch (IOException e) {
                savegamestrings[i][0] = 0x00;
                LoadMenu[i].status = 0;
                continue;
            }

        }
    }

    /**
     * Draw border for the savegame description. This is special in that it's
     * not "invokable" like the other drawroutines, but standalone.
     */
    private void DrawSaveLoadBorder(int x, int y) {
        int i;

        V.DrawPatchDirect(x - 8, y + 7, 0, W.CachePatchName("M_LSLEFT"));

        for (i = 0; i < 24; i++) {
            V.DrawPatchDirect(x, y + 7, 0, W.CachePatchName("M_LSCNTR"));
            x += 8;
        }

        V.DrawPatchDirect(x, y + 7, 0, W.CachePatchName("M_LSRGHT"));
    }

    public void DrawThermo(int x, int y, int thermWidth, int thermDot) {
        int xx;
        int i;

        xx = x;
        V.DrawPatchDirect(xx, y, 0, W.CachePatchName("M_THERML"));
        xx += 8;
        for (i = 0; i < thermWidth; i++) {
            V.DrawPatchDirect(xx, y, 0, W.CachePatchName("M_THERMM"));
            xx += 8;
        }
        V.DrawPatchDirect(xx, y, 0, W.CachePatchName("M_THERMR"));

        V.DrawPatchDirect((x + 8) + thermDot * 8, y, 0, W
                .CachePatchName("M_THERMO"));
    }

    public void DrawEmptyCell(menu_t menu, int item) {
        V.DrawPatchDirect(menu.x - 10, menu.y + item * LINEHEIGHT - 1, 0,
            (patch_t) W.CacheLumpName("M_CELL1", PU_CACHE, patch_t.class));
    }

    public void DrawSelCell(menu_t menu, int item) {
        V.DrawPatchDirect(menu.x - 10, menu.y + item * LINEHEIGHT - 1, 0,
            (patch_t) W.CacheLumpName("M_CELL2", PU_CACHE, patch_t.class));
    }

    //
    // M_SaveGame & Cie.
    //
    public void DrawSave() {
        int i;

        V.DrawPatchDirect(72, 28, 0, W.CachePatchName("M_SAVEG"));
        for (i = 0; i < load_end; i++) {
            DrawSaveLoadBorder(LoadDef.x, LoadDef.y + LINEHEIGHT * i);
            WriteText(LoadDef.x, LoadDef.y + LINEHEIGHT * i, savegamestrings[i]);
        }

        if (saveStringEnter) {
            i = StringWidth(savegamestrings[saveSlot]);
            WriteText(LoadDef.x + i, LoadDef.y + LINEHEIGHT * saveSlot, "_");
        }
    }

    /**
     * M_Responder calls this when user is finished
     * 
     * @param slot
     */

    public void DoSave(int slot) {
        G.SaveGame(slot, new String(savegamestrings[slot]));
        ClearMenus();

        // PICK QUICKSAVE SLOT YET?
        if (quickSaveSlot == -2)
            quickSaveSlot = slot;
    }

    /**
     * User wants to save. Start string input for M_Responder
     */

    class SaveSelect
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            // we are going to be intercepting all chars
            saveStringEnter = true;

            saveSlot = choice;
            C2JUtils.strcpy(saveOldString, savegamestrings[choice]);
            if (!C2JUtils.strcmp(savegamestrings[choice], EMPTYSTRING))
                savegamestrings[choice][0] = 0;
            saveCharIndex = C2JUtils.strlen(savegamestrings[choice]);
        }
    }

    /**
     * Selected from DOOM menu
     */
    class SaveGame
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            if (!DS.usergame) {
                StartMessage(SAVEDEAD, null, false);
                return;
            }

            if (DS.gamestate != gamestate_t.GS_LEVEL)
                return;

            SetupNextMenu(SaveDef);
            ReadSaveStrings();
        }
    }

    //
    // M_QuickSave
    //
    private String tempstring;

    class M_QuickSaveResponse
            implements MenuRoutine {
        @Override
        public void invoke(int ch) {
            if (ch == 'y') {
                DoSave(quickSaveSlot);
                S.StartSound(null, sfxenum_t.sfx_swtchx);
            }
        }
    }

    private void QuickSave() {
        if (!DS.usergame) {
            S.StartSound(null, sfxenum_t.sfx_oof);
            return;
        }

        if (DS.gamestate != gamestate_t.GS_LEVEL)
            return;

        if (quickSaveSlot < 0) {
            StartControlPanel();
            ReadSaveStrings();
            SetupNextMenu(SaveDef);
            quickSaveSlot = -2; // means to pick a slot now
            return;
        }
        tempstring = (QSPROMPT + savegamestrings[quickSaveSlot]);
        // TODO:M_StartMessage(tempstring,M_QuickSaveResponse,true);
    }

    //
    // M_QuickLoad
    //
    class M_QuickLoadResponse
            implements MenuRoutine {
        @Override
        public void invoke(int ch) {
            if (ch == 'y') {
                LoadSelect.invoke(quickSaveSlot);
                S.StartSound(null, sfxenum_t.sfx_swtchx);
            }
        }
    }

    class M_QuitResponse
            implements MenuRoutine {
        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;
            if (!DS.netgame) {
                if (DS.gamemode == GameMode_t.commercial)
                    S.StartSound(null, quitsounds2[(DS.gametic >> 2) & 7]);
                else
                    S.StartSound(null, quitsounds[(DS.gametic >> 2) & 7]);
                // TI.WaitVBL(105);
            }
            I.Quit();
        }
    }

    public void QuickLoad() {
        if (DS.netgame) {
            StartMessage(QLOADNET, null, false);
            return;
        }

        if (quickSaveSlot < 0) {
            StartMessage(QSAVESPOT, null, false);
            return;
        }
        tempstring = (QLPROMPT + savegamestrings[quickSaveSlot]);
        StartMessage(tempstring, QuickLoadResponse, true);
    }

    class M_Sound
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {

            SetupNextMenu(SoundDef);
        }
    }

    class M_SfxVol
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (DS.snd_SfxVolume != 0)
                    DS.snd_SfxVolume--;
                break;
            case 1:
                if (DS.snd_SfxVolume < 15)
                    DS.snd_SfxVolume++;
                break;
            }

            // TODO: S_SetSfxVolume(snd_SfxVolume /* *8 */);
        }
    }

    class M_MusicVol
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (DS.snd_MusicVolume != 0)
                    DS.snd_MusicVolume--;
                break;
            case 1:
                if (DS.snd_MusicVolume < 15)
                    DS.snd_MusicVolume++;
                break;
            }

            // TODO: S_SetMusicVolume(DS.snd_MusicVolume /* *8 */);
        }
    }

    //
    // M_Episode
    //
    private int epi;

    class M_VerifyNightmare
            implements MenuRoutine {

        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;

            G.DeferedInitNew(Defines.skill_t.sk_nightmare, epi + 1, 1);
            ClearMenus();
        }
    }

    /**
     * M_ReadThis
     */

    class M_ReadThis
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            SetupNextMenu(ReadDef1);
        }
    }

    class M_ReadThis2
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            SetupNextMenu(ReadDef2);
        }
    }

    class M_FinishReadThis
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            SetupNextMenu(MainDef);
        }
    }

    //
    // M_QuitDOOM
    //

    class M_QuitDOOM
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            // We pick index 0 which is language sensitive,
            // or one at random, between 1 and maximum number.
            if (DS.language != Language_t.english)
                endstring = endmsg[0] + "\n\n" + DOSY;
            else
                endstring =
                    endmsg[(DS.gametic % (NUM_QUITMESSAGES - 2)) + 1] + "\n\n"
                            + DOSY;

            StartMessage(endstring, QuitResponse, true);
        }
    }

    class M_QuitGame
            implements MenuRoutine {

        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;
            if (!DS.netgame) {
                if (DS.gamemode == GameMode_t.commercial)
                    ;
                // TODO:S_StartSound(NULL,quitsounds2[(gametic>>2)&7]);
                else
                    ;
                // TODO:S_StartSound(NULL,quitsounds[(gametic>>2)&7]);
                // TODO:I_WaitVBL(105);
            }
            // TODO:I_Quit ();
        }
    }

    class M_SizeDisplay
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (screenSize > 0) {
                    screenblocks--;
                    screenSize--;
                }
                break;
            case 1:
                if (screenSize < 8) {
                    screenblocks++;
                    screenSize++;
                }
                break;
            }

            // TODO: R.SetViewSize (screenblocks, detailLevel);
        }

    }

    class M_Options
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            SetupNextMenu(OptionsDef);
        }

    }

    class M_NewGame
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            if (DS.netgame && !DS.demoplayback) {
                StartMessage(NEWGAME, null, false);
                return;
            }

            if (DS.gamemode == GameMode_t.commercial)
                SetupNextMenu(NewDef);
            else
                SetupNextMenu(EpiDef);
        }

    }

    public void StartMessage(String string, MenuRoutine routine, boolean input) {
        messageLastMenuActive = DS.menuactive;
        messageToPrint = true;
        messageString = string;
        messageRoutine = routine;
        messageNeedsInput = input;
        DS.menuactive = true; // "true"
        return;
    }

    public void StopMessage() {
        DS.menuactive = messageLastMenuActive;
        messageToPrint = false;
    }

    /**
     * Find string width from hu_font chars
     */
    public int StringWidth(char[] string) {
        int i;
        int w = 0;
        int c;

        for (i = 0; i < C2JUtils.strlen(string); i++) {
            c = Character.toUpperCase(string[i]) - HU_FONTSTART;
            if (c < 0 || c >= HU_FONTSIZE)
                w += 4;
            else
                w += hu_font[c].width;
        }

        return w;
    }

    /**
     * Find string height from hu_font chars
     */
    private int StringHeight(char[] string) {
        int i;
        int h;
        int height = hu_font[0].height;

        h = height;
        for (i = 0; i < string.length; i++)
            if (string[i] == '\n')
                h += height;

        return h;
    }

    /**
     * Find string height from hu_font chars
     */
    private int StringHeight(String string) {
        return this.StringHeight(string.toCharArray());
    }

    /**
     * Write a string using the hu_font
     */

    private void WriteText(int x, int y, char[] string) {
        int w;
        char[] ch;
        int c;
        int cx;
        int cy;

        ch = string;
        int chptr = 0;
        cx = x;
        cy = y;

        while (true) {
            c = ch[chptr++];
            if (c == 0)
                break;
            if (c == '\n') {
                cx = x;
                cy += 12;
                continue;
            }

            c = Character.toUpperCase(c) - HU_FONTSTART;
            if (c < 0 || c >= HU_FONTSIZE) {
                cx += 4;
                continue;
            }

            w = hu_font[c].width;
            if (cx + w > SCREENWIDTH)
                break;
            V.DrawPatchDirect(cx, cy, 0, hu_font[c]);
            cx += w;
        }

    }

    private void WriteText(int x, int y, String string) {
        if (string == null || string.length() == 0)
            return;

        int w;
        char[] ch=string.toCharArray();
        int cx;
        int cy;

        int chptr = 0;
        char c;

        cx = x;
        cy = y;

        while (chptr<ch.length) {
            c = ch[chptr++];
            if (c == 0)
                break;
            if (c == '\n') {
                cx = x;
                cy += 12;
                continue;
            }

            c = (char) (Character.toUpperCase(c) - HU_FONTSTART);
            if (c < 0 || c >= HU_FONTSIZE) {
                cx += 4;
                continue;
            }

            w = hu_font[c].width;
            if (cx + w > SCREENWIDTH)
                break;
            V.DrawPatchDirect(cx, cy, 0, hu_font[c]);
            cx += w;
        }

    }

    // These belong to the responder.
    
    private int joywait = 0;

    private int mousewait = 0;

    private int mousey = 0;

    private int lasty = 0;

    private int mousex = 0;

    private int lastx = 0;

    public boolean Responder(event_t ev) {

        char ch;
        int i;
        ch = 0xFFFF;

        // Joystick input
        
        if (ev.type == evtype_t.ev_joystick && joywait < I.GetTime()) {
            if (ev.data3 == -1) {
                ch = KEY_UPARROW;
                joywait = I.GetTime() + 5;
            } else if (ev.data3 == 1) {
                ch = KEY_DOWNARROW;
                joywait = I.GetTime() + 5;
            }

            if (ev.data2 == -1) {
                ch = KEY_LEFTARROW;
                joywait = I.GetTime() + 2;
            } else if (ev.data2 == 1) {
                ch = KEY_RIGHTARROW;
                joywait = I.GetTime() + 2;
            }

            if ((ev.data1 & 1) != 0) {
                ch = KEY_ENTER;
                joywait = I.GetTime() + 5;
            }
            if ((ev.data1 & 2) != 0) {
                ch = KEY_BACKSPACE;
                joywait = I.GetTime() + 5;
            }
        } else 
        // Mouse input 
        {
            if (ev.type == evtype_t.ev_mouse && mousewait < I.GetTime()) {
                mousey += ev.data3;
                if (mousey < lasty - 30) {
                    ch = KEY_DOWNARROW;
                    mousewait = I.GetTime() + 5;
                    mousey = lasty -= 30;
                } else if (mousey > lasty + 30) {
                    ch = KEY_UPARROW;
                    mousewait = I.GetTime() + 5;
                    mousey = lasty += 30;
                }

                mousex += ev.data2;
                if (mousex < lastx - 30) {
                    ch = KEY_LEFTARROW;
                    mousewait = I.GetTime() + 5;
                    mousex = lastx -= 30;
                } else if (mousex > lastx + 30) {
                    ch = KEY_RIGHTARROW;
                    mousewait = I.GetTime() + 5;
                    mousex = lastx += 30;
                }

                if ((ev.data1 & 1) != 0) {
                    ch = KEY_ENTER;
                    mousewait = I.GetTime() + 15;
                }

                if ((ev.data1 & 2) != 0) {
                    ch = KEY_BACKSPACE;
                    mousewait = I.GetTime() + 15;
                }
            } else if (ev.type == evtype_t.ev_keydown) {
                ch = (char) ev.data1;
            }
        }

        if (ch == 0xFFFF)
            return false;

        // Save Game string input
        if (saveStringEnter) {
            switch (ch) {
            case KEY_BACKSPACE:
                if (saveCharIndex > 0) {
                    saveCharIndex--;
                    savegamestrings[saveSlot][saveCharIndex] = 0;
                }
                break;

            case KEY_ESCAPE:
                saveStringEnter = false;
                C2JUtils.strcpy(savegamestrings[saveSlot], saveOldString);
                break;

            case KEY_ENTER:
                saveStringEnter = false;
                if (savegamestrings[saveSlot][0] != 0)
                    DoSave(saveSlot);
                break;

            default:
                ch = Character.toUpperCase(ch);
                if (ch != 32)
                    if (ch - HU_FONTSTART < 0
                            || ch - HU_FONTSTART >= HU_FONTSIZE)
                        break;
                if (ch >= 32
                        && ch <= 127
                        && saveCharIndex < SAVESTRINGSIZE - 1
                        && StringWidth(savegamestrings[saveSlot]) < (SAVESTRINGSIZE - 2) * 8) {
                    savegamestrings[saveSlot][saveCharIndex++] = ch;
                    savegamestrings[saveSlot][saveCharIndex] = 0;
                }
                break;
            }
            return true;
        }

        // Take care of any messages that need input
        if (messageToPrint) {
            if (messageNeedsInput == true
                    && !(ch == ' ' || ch == 'n' || ch == 'y' || ch == KEY_ESCAPE))
                return false;

            DS.menuactive = messageLastMenuActive;
            messageToPrint = false;
            if (messageRoutine != null)
                messageRoutine.invoke(ch);

            DS.menuactive = false; // "false"
            S.StartSound(null, sfxenum_t.sfx_swtchx);
            return true;
        }

        if (DS.devparm && ch == KEY_F1) {
            G.ScreenShot();
            return true;
        }

        // F-Keys
        if (!DS.menuactive)
            switch (ch) {
            case KEY_MINUS: // Screen size down
                if (DS.automapactive || chat_on)
                    return false;
                SizeDisplay.invoke(0);
                S.StartSound(null, sfxenum_t.sfx_stnmov);
                return true;

            case KEY_EQUALS: // Screen size up
                if (DS.automapactive || chat_on)
                    return false;
                SizeDisplay.invoke(1);
                S.StartSound(null, sfxenum_t.sfx_stnmov);
                return true;

            case KEY_F1: // Help key
                StartControlPanel();

                if (DS.gamemode == GameMode_t.retail)
                    currentMenu = ReadDef2;
                else
                    currentMenu = ReadDef1;

                itemOn = 0;
                // TODO: S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F2: // Save
                StartControlPanel();
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                SaveGame.invoke(0);
                return true;

            case KEY_F3: // Load
                StartControlPanel();
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                LoadGame.invoke(0);
                return true;

            case KEY_F4: // Sound Volume
                StartControlPanel();
                currentMenu = SoundDef;
                itemOn = (short) sfx_vol;
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F5: // Detail toggle
                ChangeDetail.invoke(0);
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F6: // Quicksave
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                QuickSave();
                return true;

            case KEY_F7: // End game
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                EndGame.invoke(0);
                return true;

            case KEY_F8: // Toggle messages
                ChangeMessages.invoke(0);
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F9: // Quickload
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                QuickLoad();
                return true;

            case KEY_F10: // Quit DOOM
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                QuitDOOM.invoke(0);
                return true;

            case KEY_F11: // gamma toggle
                int usegamma = V.getUsegamma();
                if (usegamma > 4)
                    usegamma = 0;
                DS.players[DS.consoleplayer].message = gammamsg[usegamma];
                // TODO: I.SetPalette (W.CacheLumpName ("PLAYPAL",PU_CACHE));
                V.setUsegamma(usegamma);
                return true;

            }

        // Pop-up menu?
        if (!DS.menuactive) {
            if (ch == KEY_ESCAPE) {
                StartControlPanel();
             //TODO:   S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;
            }
            return false;
        }

        // Keys usable within menu
        switch (ch) {
        case KEY_DOWNARROW:
            do {
                if (itemOn + 1 > currentMenu.numitems - 1)
                    itemOn = 0;
                else
                    itemOn++;
                //TODO: S.StartSound(null, sfxenum_t.sfx_pstop);
            } while (currentMenu.menuitems[itemOn].status == -1);
            return true;

        case KEY_UPARROW:
            do {
                if (itemOn == 0)
                    itemOn = (short) (currentMenu.numitems - 1);
                else
                    itemOn--;
              //TODO:  S.StartSound(null, sfxenum_t.sfx_pstop);
            } while (currentMenu.menuitems[itemOn].status == -1);
            return true;

        case KEY_LEFTARROW:
            if ((currentMenu.menuitems[itemOn].routine != null)
                    && (currentMenu.menuitems[itemOn].status == 2)) {
             //TODO:   S.StartSound(null, sfxenum_t.sfx_stnmov);
                currentMenu.menuitems[itemOn].routine.invoke(0);
            }
            return true;

        case KEY_RIGHTARROW:
            if ((currentMenu.menuitems[itemOn].routine != null)
                    && (currentMenu.menuitems[itemOn].status == 2)) {
             //TODO:   S.StartSound(null, sfxenum_t.sfx_stnmov);
                currentMenu.menuitems[itemOn].routine.invoke(1);
            }
            return true;

        case KEY_ENTER:
            if ((currentMenu.menuitems[itemOn].routine != null)
                    && currentMenu.menuitems[itemOn].status != 0) {
                currentMenu.lastOn = itemOn;
                if (currentMenu.menuitems[itemOn].status == 2) {
                    currentMenu.menuitems[itemOn].routine.invoke(1); // right
                    // arrow
              //TODO:      S.StartSound(null, sfxenum_t.sfx_stnmov);
                } else {
                    currentMenu.menuitems[itemOn].routine.invoke(itemOn);
                    S.StartSound(null, sfxenum_t.sfx_pistol);
                }
            }
            return true;

        case KEY_ESCAPE:
            currentMenu.lastOn = itemOn;
            ClearMenus();
            // TODO: S.StartSound(null, sfxenum_t.sfx_swtchx);
            return true;

        case KEY_BACKSPACE:
            currentMenu.lastOn = itemOn;
            if (currentMenu.prevMenu != null) {
                currentMenu = currentMenu.prevMenu;
                itemOn = (short) currentMenu.lastOn;
                // TODO: S.StartSound(null, sfxenum_t.sfx_swtchn);
            }
            return true;

        default:
            for (i = itemOn + 1; i < currentMenu.numitems; i++)
                if (currentMenu.menuitems[i].alphaKey == ch) {
                    itemOn = (short) i;
                    S.StartSound(null, sfxenum_t.sfx_pstop);
                    return true;
                }
            for (i = 0; i <= itemOn; i++)
                if (currentMenu.menuitems[i].alphaKey == ch) {
                    itemOn = (short) i;
                    S.StartSound(null, sfxenum_t.sfx_pstop);
                    return true;
                }
            break;

        }

        return false;
    }

    /**
     * M_StartControlPanel
     */
    public void StartControlPanel() {
        // intro might call this repeatedly
        if (DS.menuactive)
            return;

        DS.menuactive = true;
        currentMenu = MainDef; // JDC
        itemOn = (short) currentMenu.lastOn; // JDC
    }

    /**
     * M_Drawer Called after the view has been rendered, but before it has been
     * blitted.
     */
    public void Drawer() {

        int x;
        int y;
        int max;
        char[] string = new char[40];
        char[] msstring;
        int start;
        inhelpscreens = false; // Horiz. & Vertically center string and print
        // it.
        if (messageToPrint) {
            start = 0;
            y = 100 - this.StringHeight(messageString) / 2;
            msstring = messageString.toCharArray();
            // while(*(messageString+start)) {
            for (int l = 0; l < messageString.length();l++) 
                for (int i = 0; i < messageString.length() - start; i++) {
                    if (msstring[start + i] == '\n') {
                        C2JUtils.memset(string, (char) 0, 40);
                        C2JUtils.strcpy(string, msstring, start, i);
                        start += i + 1;
                        break;
                    }
                    if (i == (messageString.length() - start)) {
                        C2JUtils.strcpy(string, messageString + start);
                        start += i;
                    }
                    x = 160 - this.StringWidth(string) / 2;
                    this.WriteText(x, y, string);
                    y += hu_font[0].height;
                }
                return;
            }
            if (!DS.menuactive)
                return;
            if (currentMenu.routine != null)
                currentMenu.routine.invoke(); // call Draw routine

            // DRAW MENU
            x = currentMenu.x;
            y = currentMenu.y;
            max = currentMenu.numitems;
            for (int i = 0; i < max; i++) {
                if (currentMenu.menuitems[i].name != null && currentMenu.menuitems[i].name!="")
                    V.DrawPatchDirect(x, y, 0, W.CachePatchName(
                        currentMenu.menuitems[i].name, PU_CACHE));
                y += LINEHEIGHT;
            }

            // DRAW SKULL
            V.DrawPatchDirect(x + SKULLXOFF, currentMenu.y - 5 + itemOn
                    * LINEHEIGHT, 0, W.CachePatchName(skullName[whichSkull],
                PU_CACHE));
    }

    //
    // M_ClearMenus
    //
    public void ClearMenus() {
        DS.menuactive = false;
        if (!DS.netgame && DS.usergame && DS.paused)
         G.setPaused(true);
    }

    /**
     * M_SetupNextMenu
     */
    public void SetupNextMenu(menu_t menudef) {
        currentMenu = menudef;
        itemOn = (short) currentMenu.lastOn;
    }

    /**
     * M_Ticker
     */
    public void Ticker() {
        if (--skullAnimCounter <= 0) {
            whichSkull ^= 1;
            skullAnimCounter = 8;
        }
    }

    /**
     * M_Init
     */
    public void Init() {
        currentMenu = MainDef;
        DS.menuactive = false;
        itemOn = (short) currentMenu.lastOn;
        whichSkull = 0;
        skullAnimCounter = 10;
        screenSize = screenblocks - 3;
        messageToPrint = false;
        messageString = null;
        messageLastMenuActive = DS.menuactive;
        quickSaveSlot = -1;

        // Here we could catch other version dependencies,
        // like HELP1/2, and four episodes.

        switch (DS.gamemode) {
        case commercial:
            // This is used because DOOM 2 had only one HELP
            // page. I use CREDIT as second page now, but
            // kept this hack for educational purposes.
            MainMenu[readthis] = MainMenu[quitdoom];
            MainDef.numitems--;
            MainDef.y += 8;
            NewDef.prevMenu = MainDef;
            ReadDef1.routine = DrawReadThis1;
            ReadDef1.x = 330;
            ReadDef1.y = 165;
            ReadMenu1[0].routine = FinishReadThis;
            break;
        case shareware:
            // Episode 2 and 3 are handled,
            // branching to an ad screen.
        case registered:
            // We need to remove the fourth episode.
            EpiDef.numitems--;
            break;
        case retail:
            // We are fine.
        default:
            break;
        }

    }

    public int myargc;

    public String[] myargv;

    /**
     * M_CheckParm Checks for the given parameter in the program's command line
     * arguments. Returns the argument number (1 to argc-1) or 0 if not present
     */
    public int CheckParm(String check) {
        int i;

        for (i = 1; i < myargc; i++) {
            if (check.compareToIgnoreCase(myargv[i]) == 0)
                return i;
        }

        return 0;
    }

    /**
     * M_DrawText Returns the final X coordinate HU_Init must have been called
     * to init the font
     * 
     * @param x
     * @param y
     * @param direct
     * @param string
     * @return
     */

    public int DrawText(int x, int y, boolean direct, char[] string) {
        int c;
        int w;
        int ptr = 0;

        while (string[ptr] > 0) {
            c = Character.toUpperCase(string[ptr]) - HU_FONTSTART;
            ptr++;
            if (c < 0 || c > HU_FONTSIZE) {
                x += 4;
                continue;
            }

            w = hu_font[c].width;
            if (x + w > SCREENWIDTH)
                break;
            if (direct)
                V.DrawPatchDirect(x, y, 0, hu_font[c]);
            else
                V.DrawPatch(x, y, 0, hu_font[c]);
            x += w;
        }

        return x;
    }

    

    // ////////////////////////// DRAWROUTINES
    // //////////////////////////////////

    class M_DrawEpisode
            implements DrawRoutine {

        @Override
        public void invoke() {
            V.DrawPatchDirect(54, 38, 0, W.CachePatchName("M_EPISOD"));
        }

    }

    /**
     * M_LoadGame & Cie.
     */

    class M_DrawLoad
            implements DrawRoutine {
        @Override
        public void invoke() {
            int i;

            V.DrawPatchDirect(72, 28, 0, W.CachePatchName("M_LOADG"));
            for (i = 0; i < load_end; i++) {
                DrawSaveLoadBorder(LoadDef.x, LoadDef.y + LINEHEIGHT * i);
                WriteText(LoadDef.x, LoadDef.y + LINEHEIGHT * i,
                    savegamestrings[i]);
            }

        }
    }

    class M_DrawMainMenu
            implements DrawRoutine {
        @Override
        public void invoke() {
            V.DrawPatchDirect(94, 2, 0, (patch_t) (W.CachePatchName("M_DOOM")));
        }
    }

    class M_DrawNewGame
            implements DrawRoutine {

        @Override
        public void invoke() {
            V.DrawPatchDirect(96, 14, 0, (patch_t) W.CachePatchName("M_NEWG"));
            V.DrawPatchDirect(54, 38, 0, (patch_t) W.CachePatchName("M_SKILL"));
        }
    }

    class M_DrawOptions
            implements DrawRoutine {

        private String detailNames[] = { "M_GDHIGH", "M_GDLOW" };

        private String msgNames[] = { "M_MSGOFF", "M_MSGON" };

        @Override
        public void invoke() {
            V.DrawPatchDirect(108, 15, 0, W.CachePatchName("M_OPTTTL"));

            V.DrawPatchDirect(OptionsDef.x + 175, OptionsDef.y + LINEHEIGHT
                    * detail, 0, W.CachePatchName(detailNames[detailLevel]));

            V.DrawPatchDirect(OptionsDef.x + 120, OptionsDef.y + LINEHEIGHT
                    * messages, 0, W.CachePatchName(msgNames[showMessages]));

            DrawThermo(OptionsDef.x, OptionsDef.y + LINEHEIGHT
                    * (mousesens + 1), 10, mouseSensitivity);

            DrawThermo(OptionsDef.x,
                OptionsDef.y + LINEHEIGHT * (scrnsize + 1), 9, screenSize);

        }

    }

    /**
     * Read This Menus Had a "quick hack to fix romero bug"
     */

    class M_DrawReadThis1
            implements DrawRoutine {

        public void invoke() {
            inhelpscreens = true;
            switch (DS.gamemode) {
            case commercial:
                V.DrawPatchDirect(0, 0, 0, W.CachePatchName("HELP"));
                break;
            case shareware:
            case registered:
            case retail:
                V.DrawPatchDirect(0, 0, 0, W.CachePatchName("HELP1"));
                break;
            default:
                break;
            }
            return;
        }
    }

    /**
     * Read This Menus - optional second page.
     */
    class M_DrawReadThis2
            implements DrawRoutine {

        public void invoke() {
            inhelpscreens = true;
            switch (DS.gamemode) {
            case retail:
            case commercial:
                // This hack keeps us from having to change menus.
                V.DrawPatchDirect(0, 0, 0, W.CachePatchName("CREDIT"));
                break;
            case shareware:
            case registered:
                V.DrawPatchDirect(0, 0, 0, W.CachePatchName("HELP2"));
                break;
            default:
                break;
            }
            return;
        }
    }

    /**
     * Change Sfx & Music volumes
     */
    class M_DrawSound
            implements DrawRoutine {

        public void invoke() {
            V.DrawPatchDirect(60, 38, 0, (patch_t) W.CacheLumpName("M_SVOL",
                PU_CACHE, patch_t.class));

            DrawThermo(SoundDef.x, SoundDef.y + LINEHEIGHT * (sfx_vol + 1), 16,
                DS.snd_SfxVolume);

            DrawThermo(SoundDef.x, SoundDef.y + LINEHEIGHT * (music_vol + 1),
                16, DS.snd_MusicVolume);
        }
    }

    // /////////////////////////// MENU ROUTINES
    // ///////////////////////////////////

    class M_ChangeDetail
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            detailLevel = 1 - detailLevel;

            // FIXME - does not work. Remove anyway?
            System.err.print("M_ChangeDetail: low detail mode n.a.\n");

            return;

            /*
             * TODO: R.SetViewSize (screenblocks, detailLevel); if
             * (detailLevel==0) DS.players[DS.consoleplayer].message = DETAILHI;
             * else DS.players[DS.consoleplayer].message = DETAILLO;
             */

        }
    }

    /**
     * Toggle messages on/off
     */
    class M_ChangeMessages
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            // warning: unused parameter `int choice'
            choice = 0;
            showMessages = 1 - showMessages;

            if (showMessages == 0)
                DS.players[DS.consoleplayer].message = MSGOFF;
            else
                DS.players[DS.consoleplayer].message = MSGON;

            message_dontfuckwithme = true;
        }
    }

    class M_ChangeSensitivity
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (mouseSensitivity != 0)
                    mouseSensitivity--;
                break;
            case 1:
                if (mouseSensitivity < 9)
                    mouseSensitivity++;
                break;
            }
        }
    }

    class M_ChooseSkill
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            if (choice == nightmare) {
                StartMessage(NIGHTMARE, VerifyNightmare, true);
                return;
            }

            G.DeferedInitNew(skill_t.values()[choice], epi + 1, 1);
            ClearMenus();
        }

    }

    /**
     * M_EndGame
     */

    class M_EndGame
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            if (!DS.usergame) {
                S.StartSound(null, sfxenum_t.sfx_oof);
                return;
            }

            if (DS.netgame) {
                StartMessage(NETEND, null, false);
                return;
            }

            StartMessage(ENDGAME, EndGameResponse, true);
        }
    }

    class M_EndGameResponse
            implements MenuRoutine {

        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;

            currentMenu.lastOn = itemOn;
            ClearMenus();
            D.StartTitle();
        }
    }

    class M_Episode
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {

            if ((DS.gamemode == GameMode_t.shareware) && (choice != 0)) {
                StartMessage(SWSTRING, null, false);
                SetupNextMenu(ReadDef1);
                return;
            }

            // Yet another hack...
            if ((DS.gamemode == GameMode_t.registered) && (choice > 2)) {
                System.err
                        .print("M_Episode: 4th episode requires UltimateDOOM\n");
                choice = 0;
            }

            epi = choice;
            SetupNextMenu(NewDef);
            SetupNextMenu(OptionsDef);
        }

    }

    /**
     * User wants to load this game
     */
    class M_LoadSelect
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            String name;

            if (CheckParm("-cdrom") != 0)
                name = ("c:\\doomdata\\" + SAVEGAMENAME + (choice) + ".dsg");
            else
                name = (SAVEGAMENAME + (choice) + ".dsg");
            G.LoadGame(name);
            ClearMenus();
        }
    }

    /**
     * Selected from DOOM menu
     */
    class M_LoadGame
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {

            if (DS.netgame) {
                StartMessage(LOADNET, null, false);
                return;
            }

            SetupNextMenu(LoadDef);
            ReadSaveStrings();
        }
    }

    // ////////////////////// VARIOUS CONSTS //////////////////////

    private static final sfxenum_t[] quitsounds =
        { sfxenum_t.sfx_pldeth, sfxenum_t.sfx_dmpain, sfxenum_t.sfx_popain,
                sfxenum_t.sfx_slop, sfxenum_t.sfx_telept, sfxenum_t.sfx_posit1,
                sfxenum_t.sfx_posit3, sfxenum_t.sfx_sgtatk };

    private static final sfxenum_t[] quitsounds2 =
        { sfxenum_t.sfx_vilact, sfxenum_t.sfx_getpow, sfxenum_t.sfx_boscub,
                sfxenum_t.sfx_slop, sfxenum_t.sfx_skeswg, sfxenum_t.sfx_kntdth,
                sfxenum_t.sfx_bspact, sfxenum_t.sfx_sgtatk };

    /** episodes_e enum */
    private static int ep1 = 0, ep2 = 1, ep3 = 2, ep4 = 3, ep_end = 4;

    /** load_e enum */
    private static int load1 = 0, load2 = 1, load3 = 2, load4 = 3, load5 = 4,
            load6 = 5, load_end = 6;

    /** options_e enum; */

    private static int endgame = 0, messages = 1, detail = 2, scrnsize = 3,
            option_empty1 = 4, mousesens = 5, option_empty2 = 6, soundvol = 7,
            opt_end = 8;

    /** main_e enum; */
    private static int newgame = 0, options = 1, loadgam = 2, savegame = 3,
            readthis = 4, quitdoom = 5, main_end = 6;

    /** read_e enum */
    private static int rdthsempty1 = 0, read1_end = 1;

    /** read_2 enum */
    private static int rdthsempty2 = 0, read2_end = 1;

    /**  newgame_e enum;*/
    static int killthings = 0, toorough = 1, hurtme = 2, violence = 3,
            nightmare = 4, newg_end = 5;
    
    private static String[] gammamsg = { "GAMMALVL0",

        "GAMMALVL1", "GAMMALVL2", "GAMMALVL3", "GAMMALVL4" };

    /** sound_e enum */
    static int sfx_vol = 0, sfx_empty1 = 1, music_vol = 2, sfx_empty2 = 3,
            sound_end = 4;
    
    

}