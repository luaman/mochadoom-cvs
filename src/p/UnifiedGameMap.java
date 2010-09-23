package p;

import static data.Defines.ITEMQUESIZE;
import static data.Defines.MAPBLOCKSHIFT;
import static data.Defines.MELEERANGE;
import static data.Defines.ML_SOUNDBLOCK;
import static data.Defines.ML_TWOSIDED;
import static data.Defines.NF_SUBSECTOR;
import static data.Defines.NUMAMMO;
import static data.Defines.RANGECHECK;
import static data.Defines.acp1;
import static data.Defines.pw_allmap;
import static data.Defines.pw_infrared;
import static data.Defines.pw_invisibility;
import static data.Defines.pw_invulnerability;
import static data.Defines.pw_ironfeet;
import static data.Defines.pw_strength;
import static data.Limits.BUTTONTIME;
import static data.Limits.MAXANIMS;
import static data.Limits.MAXBUTTONS;
import static data.Limits.MAXINT;
import static data.Limits.MAXINTERCEPTS;
import static data.Limits.MAXLINEANIMS;
import static data.Limits.MAXPLATS;
import static data.Limits.MAXSPECIALCROSS;
import static data.Limits.MAXSWITCHES;
import static data.Limits.MAX_ADJOINING_SECTORS;
import static data.Limits.PLATSPEED;
import static data.Limits.PLATWAIT;
import static data.Tables.ANG270;
import static data.Tables.ANG90;
import static data.info.mobjinfo;
import static data.info.sprnames;
import static data.info.states;
import static doom.englsh.GOTARMBONUS;
import static doom.englsh.GOTARMOR;
import static doom.englsh.GOTBACKPACK;
import static doom.englsh.GOTBERSERK;
import static doom.englsh.GOTBFG9000;
import static doom.englsh.GOTBLUECARD;
import static doom.englsh.GOTBLUESKUL;
import static doom.englsh.GOTCELL;
import static doom.englsh.GOTCELLBOX;
import static doom.englsh.GOTCHAINGUN;
import static doom.englsh.GOTCHAINSAW;
import static doom.englsh.GOTCLIP;
import static doom.englsh.GOTCLIPBOX;
import static doom.englsh.GOTHTHBONUS;
import static doom.englsh.GOTINVIS;
import static doom.englsh.GOTINVUL;
import static doom.englsh.GOTLAUNCHER;
import static doom.englsh.GOTMAP;
import static doom.englsh.GOTMEDIKIT;
import static doom.englsh.GOTMEDINEED;
import static doom.englsh.GOTMEGA;
import static doom.englsh.GOTMSPHERE;
import static doom.englsh.GOTPLASMA;
import static doom.englsh.GOTREDCARD;
import static doom.englsh.GOTREDSKULL;
import static doom.englsh.GOTROCKBOX;
import static doom.englsh.GOTROCKET;
import static doom.englsh.GOTSHELLBOX;
import static doom.englsh.GOTSHELLS;
import static doom.englsh.GOTSHOTGUN;
import static doom.englsh.GOTSHOTGUN2;
import static doom.englsh.GOTSTIM;
import static doom.englsh.GOTSUIT;
import static doom.englsh.GOTSUPER;
import static doom.englsh.GOTVISOR;
import static doom.englsh.GOTYELWCARD;
import static doom.englsh.GOTYELWSKUL;
import static doom.items.weaponinfo;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static p.MapUtils.AproxDistance;
import static p.MapUtils.InterceptVector;
import static p.MapUtils.eval;
import static p.MapUtils.flags;
import static p.mobj.MF_COUNTITEM;
import static p.mobj.MF_DROPPED;
import static p.mobj.MF_NOBLOCKMAP;
import static p.mobj.MF_NOSECTOR;
import static p.mobj.MF_SPECIAL;
import static p.mobj_t.MF_JUSTHIT;
import static p.mobj_t.MF_MISSILE;
import automap.DoomAutoMap;
import hu.HU;
import i.DoomSystemInterface;
import m.random;
import rr.UnifiedRenderer;
import rr.line_t;
import rr.node_t;
import rr.sector_t;
import rr.side_t;
import rr.subsector_t;
import rr.vertex_t;
import st.StatusBar;
import w.WadLoader;
import data.mapthing_t;
import data.mobjtype_t;
import data.state_t;
import data.Defines.GameMode_t;
import data.Defines.ammotype_t;
import data.Defines.card_t;
import data.Defines.statenum_t;
import data.sounds.sfxenum_t;
import doom.DoomMain;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;
import doom.weapontype_t;

// // FROM SIGHT

public class UnifiedGameMap {

    // ///////////////// STATUS ///////////////////

    WadLoader W;

    DoomAutoMap AM;

    random RND;

    UnifiedRenderer R;

    LevelLoader LL;

    DoomMain DM;

    StatusBar ST;

    HU HU;
    
    DoomSystemInterface I;

    // //////////// Internal singletons //////////////
    public Actions A;

    Specials SPECS;

    // DoorsFloors EV;
    Plats PEV;

    Lights LEV;

    Switches SW;

    Sight See;

    Enemies EN;

    // ////////////////////////////////////////////

    public int topslope;

    public int bottomslope; // slopes to top and bottom of target

    int attackrange;

    //
    // UTILITIES
    //

    //
    // getSide()
    // Will return a side_t*
    // given the number of the current sector,
    // the line number, and the side (0/1) that you want.
    //
    side_t getSide(int currentSector, int line, int side) {
        return LL.sides[(LL.sectors[currentSector].lines[line]).sidenum[side]];
    }

    //
    // getSector()
    // Will return a sector_t
    // given the number of the current sector,
    // the line number and the side (0/1) that you want.
    //
    sector_t getSector(int currentSector, int line, int side) {
        return LL.sides[(LL.sectors[currentSector].lines[line]).sidenum[side]].sector;
    }

    //
    // twoSided()
    // Given the sector number and the line number,
    // it will tell you whether the line is two-sided or not.
    //
    boolean twoSided(int sector, int line) {
        return flags((LL.sectors[sector].lines[line]).flags, ML_TWOSIDED);
    }

    //
    // getNextSector()
    // Return sector_t * of sector next to current.
    // NULL if not two-sided line
    //
    sector_t getNextSector(line_t line, sector_t sec) {
        if (!flags(line.flags, ML_TWOSIDED))
            return null;

        if (line.frontsector == sec)
            return line.backsector;

        return line.frontsector;
    }

    //
    // P_FindLowestFloorSurrounding()
    // FIND LOWEST FLOOR HEIGHT IN SURROUNDING SECTORS
    //
    int FindLowestFloorSurrounding(sector_t sec) {
        int i;
        line_t check;
        sector_t other;
        int floor = sec.floorheight;

        for (i = 0; i < sec.linecount; i++) {
            check = sec.lines[i];
            other = getNextSector(check, sec);

            if (other == null)
                continue;

            if (other.floorheight < floor)
                floor = other.floorheight;
        }
        return floor;
    }

    /**
     * P_FindHighestFloorSurrounding() FIND HIGHEST FLOOR HEIGHT IN SURROUNDING
     * SECTORS
     * 
     * @param sec
     */
    int FindHighestFloorSurrounding(sector_t sec) {
        int i;
        line_t check;
        sector_t other;
        int floor = -500 * FRACUNIT;

        for (i = 0; i < sec.linecount; i++) {
            check = sec.lines[i];
            other = getNextSector(check, sec);

            if (other == null)
                continue;

            if (other.floorheight > floor)
                floor = other.floorheight;
        }
        return floor;
    }

    /**
     * P_FindNextHighestFloor FIND NEXT HIGHEST FLOOR IN SURROUNDING SECTORS
     * Note: this should be doable w/o a fixed array.
     * 
     * @param sec
     * @param currentheight
     * @return fixed
     */

    int FindNextHighestFloor(sector_t sec, int currentheight) {
        int i;
        int h;
        int min;
        line_t check;
        sector_t other;
        int height = currentheight;

        int heightlist[] = new int[MAX_ADJOINING_SECTORS];

        for (i = 0, h = 0; i < sec.linecount; i++) {
            check = sec.lines[i];
            other = getNextSector(check, sec);

            if (other == null)
                continue;

            if (other.floorheight > height)
                heightlist[h++] = other.floorheight;

            // Check for overflow. Exit.
            if (h >= MAX_ADJOINING_SECTORS) {
                System.err
                        .print("Sector with more than 20 adjoining sectors\n");
                break;
            }
        }

        // Find lowest height in list
        if (h == 0)
            return currentheight;

        min = heightlist[0];

        // Range checking?
        for (i = 1; i < h; i++)
            if (heightlist[i] < min)
                min = heightlist[i];

        return min;
    }

    //
    // FIND LOWEST CEILING IN THE SURROUNDING SECTORS
    //
    int FindLowestCeilingSurrounding(sector_t sec) {
        int i;
        line_t check;
        sector_t other;
        int height = MAXINT;

        for (i = 0; i < sec.linecount; i++) {
            check = sec.lines[i];
            other = getNextSector(check, sec);

            if (other == null)
                continue;

            if (other.ceilingheight < height)
                height = other.ceilingheight;
        }
        return height;
    }

    //
    // FIND HIGHEST CEILING IN THE SURROUNDING SECTORS
    //
    int FindHighestCeilingSurrounding(sector_t sec) {
        int i;
        line_t check;
        sector_t other;
        int height = 0;

        for (i = 0; i < sec.linecount; i++) {
            check = sec.lines[i];
            other = getNextSector(check, sec);

            if (other == null)
                continue;

            if (other.ceilingheight > height)
                height = other.ceilingheight;
        }
        return height;
    }

    //
    // RETURN NEXT SECTOR # THAT LINE TAG REFERS TO
    //
    int FindSectorFromLineTag(line_t line, int start) {
        int i;

        for (i = start + 1; i < LL.numsectors; i++)
            if (LL.sectors[i].tag == line.tag)
                return i;

        return -1;
    }

    //
    // Find minimum light from an adjacent sector
    //
    int FindMinSurroundingLight(sector_t sector, int max) {
        int i;
        int min;
        line_t line;
        sector_t check;

        min = max;
        for (i = 0; i < sector.linecount; i++) {
            line = sector.lines[i];
            check = getNextSector(line, sector);

            if (check == null)
                continue;

            if (check.lightlevel < min)
                min = check.lightlevel;
        }
        return min;
    }

    // //////////////////// FROM p_maputl.c ////////////////////

    /** fixed_t */
    protected int opentop, openbottom, openrange, lowfloor;

    /**
     * P_LineOpening Sets opentop and openbottom to the window through a two
     * sided line. OPTIMIZE: keep this precalculated
     */

    public void LineOpening(line_t linedef) {
        sector_t front;
        sector_t back;

        if (linedef.sidenum[1] == -1) {
            // single sided line
            openrange = 0;
            return;
        }

        front = linedef.frontsector;
        back = linedef.backsector;

        if (front.ceilingheight < back.ceilingheight)
            opentop = front.ceilingheight;
        else
            opentop = back.ceilingheight;

        if (front.floorheight > back.floorheight) {
            openbottom = front.floorheight;
            lowfloor = back.floorheight;
        } else {
            openbottom = back.floorheight;
            lowfloor = front.floorheight;
        }

        openrange = opentop - openbottom;
    }

    //
    // THING POSITION SETTING
    //

    /**
     * P_UnsetThingPosition Unlinks a thing from block map and sectors. On each
     * position change, BLOCKMAP and other lookups maintaining lists ot things
     * inside these structures need to be updated.
     */

    public void UnsetThingPosition(mobj_t thing) {
        int blockx;
        int blocky;

        if (!flags(thing.flags, MF_NOSECTOR)) {
            // inert things don't need to be in blockmap?
            // unlink from subsector
            if (thing.snext != null)
                ((mobj_t) thing.snext).sprev = thing.sprev;

            if (thing.sprev != null)
                ((mobj_t) thing.sprev).snext = thing.snext;
            else
                thing.subsector.sector.thinglist = (mobj_t) thing.snext;
        }

        if (!flags(thing.flags, MF_NOBLOCKMAP)) {
            // inert things don't need to be in blockmap
            // unlink from block map
            if (thing.bnext != null)
                ((mobj_t) thing.bnext).bprev = thing.bprev;

            if (thing.bprev != null)
                ((mobj_t) thing.bprev).bnext = thing.bnext;
            else {
                blockx = (thing.x - LL.bmaporgx) >> MAPBLOCKSHIFT;
                blocky = (thing.y - LL.bmaporgy) >> MAPBLOCKSHIFT;

                if (blockx >= 0 && blockx < LL.bmapwidth && blocky >= 0
                        && blocky < LL.bmapheight) {
                    LL.blocklinks[blocky * LL.bmapwidth + blockx] =
                        (mobj_t) thing.bnext;
                }
            }
        }
    }

    //
    // BLOCK MAP ITERATORS
    // For each line/thing in the given mapblock,
    // call the passed PIT_* function.
    // If the function returns false,
    // exit with false without checking anything else.
    //

    //
    // INTERCEPT ROUTINES
    //
    intercept_t[] intercepts = new intercept_t[MAXINTERCEPTS];

    // C2JUtils.initArrayOfObjects(intercepts,intercept_t.class);
    int intercept_p;

    public divline_t trace;

    boolean earlyout;

    int ptflags;

    //
    // PIT_AddLineIntercepts.
    // Looks for lines in the given block
    // that intercept the given trace
    // to add to the intercepts list.
    //
    // A line is crossed if its endpoints
    // are on opposite sides of the trace.
    // Returns true if earlyout and a solid line hit.
    //
    boolean AddLineIntercepts(line_t ld) {
        boolean s1;
        boolean s2;
        int frac;
        divline_t dl = new divline_t();

        // avoid precision problems with two routines
        if (trace.dx > FRACUNIT * 16 || trace.dy > FRACUNIT * 16
                || trace.dx < -FRACUNIT * 16 || trace.dy < -FRACUNIT * 16) {
            s1 = trace.PointOnDivlineSide(ld.v1x, ld.v1.y);
            s2 = trace.PointOnDivlineSide(ld.v2x, ld.v2y);
        } else {
            s1 = ld.PointOnLineSide(trace.x, trace.y);
            s2 = ld.PointOnLineSide(trace.x + trace.dx, trace.y + trace.dy);
        }

        if (s1 == s2)
            return true; // line isn't crossed

        // hit the line
        dl.MakeDivline(ld);
        frac = InterceptVector(trace, dl);

        if (frac < 0)
            return true; // behind source

        // try to early out the check
        if (earlyout && frac < FRACUNIT && ld.backsector == null) {
            return false; // stop checking
        }

        // "create" a new intercept in the static intercept pool.
        intercepts[intercept_p].frac = frac;
        intercepts[intercept_p].isaline = true;
        intercepts[intercept_p].line = ld;
        intercept_p++;

        return true; // continue
    }

    //
    // PIT_AddThingIntercepts
    //
    boolean AddThingIntercepts(mobj_t thing) {
        int x1, y1, x2, y2; // fixed_t

        boolean s1, s2;

        boolean tracepositive;

        // maybe make this a shared instance variable?
        divline_t dl = new divline_t();

        int frac; // fixed_t

        tracepositive = (trace.dx ^ trace.dy) > 0;

        // check a corner to corner crossection for hit
        if (tracepositive) {
            x1 = thing.x - thing.radius;
            y1 = thing.y + thing.radius;

            x2 = thing.x + thing.radius;
            y2 = thing.y - thing.radius;
        } else {
            x1 = thing.x - thing.radius;
            y1 = thing.y - thing.radius;

            x2 = thing.x + thing.radius;
            y2 = thing.y + thing.radius;
        }

        s1 = trace.PointOnDivlineSide(x1, y1);
        s2 = trace.PointOnDivlineSide(x2, y2);

        if (s1 == s2)
            return true; // line isn't crossed

        dl.x = x1;
        dl.y = y1;
        dl.dx = x2 - x1;
        dl.dy = y2 - y1;

        frac = InterceptVector(trace, dl);

        if (frac < 0)
            return true; // behind source

        // "create" a new intercept in the static intercept pool.
        intercepts[intercept_p].frac = frac;
        intercepts[intercept_p].isaline = false;
        intercepts[intercept_p].thing = thing;
        intercept_p++;

        return true; // keep going
    }

    // end class

    class Lights {
        public static final int GLOWSPEED = 8;

        public static final int STROBEBRIGHT = 5;

        public static final int FASTDARK = 15;

        public static final int SLOWDARK = 35;

        //
        // FIRELIGHT FLICKER
        //

        //
        // T_FireFlicker
        //
        void FireFlicker(fireflicker_t flick) {
            int amount;

            if (--flick.count != 0)
                return;

            amount = (RND.P_Random() & 3) * 16;

            if (flick.sector.lightlevel - amount < flick.minlight)
                flick.sector.lightlevel = (short) flick.minlight;
            else
                flick.sector.lightlevel = (short) (flick.maxlight - amount);

            flick.count = 4;
        }

        //
        // P_SpawnFireFlicker
        //
        void SpawnFireFlicker(sector_t sector) {
            fireflicker_t flick;

            // Note that we are resetting sector attributes.
            // Nothing special about it during gameplay.
            sector.special = 0;

            flick = new fireflicker_t();

            AddThinker(flick.thinker);

            flick.thinker.function = think_t.T_FireFlicker;
            flick.sector = sector;
            flick.maxlight = sector.lightlevel;
            flick.minlight =
                FindMinSurroundingLight(sector, sector.lightlevel) + 16;
            flick.count = 4;
        }

        //
        // BROKEN LIGHT FLASHING
        //

        //
        // T_LightFlash
        // Do flashing lights.
        //
        void LightFlash(lightflash_t flash) {
            if (--flash.count != 0)
                return;

            if (flash.sector.lightlevel == flash.maxlight) {
                flash.sector.lightlevel = (short) flash.minlight;
                flash.count = (RND.P_Random() & flash.mintime) + 1;
            } else {
                flash.sector.lightlevel = (short) flash.maxlight;
                flash.count = (RND.P_Random() & flash.maxtime) + 1;
            }

        }

        //
        // P_SpawnLightFlash
        // After the map has been loaded, scan each sector
        // for specials that spawn thinkers
        //
        void SpawnLightFlash(sector_t sector) {
            lightflash_t flash;

            // nothing special about it during gameplay
            sector.special = 0;

            flash = new lightflash_t();

            AddThinker(flash.thinker);

            flash.thinker.function = think_t.T_LightFlash;
            flash.sector = sector;
            flash.maxlight = sector.lightlevel;

            flash.minlight = FindMinSurroundingLight(sector, sector.lightlevel);
            flash.maxtime = 64;
            flash.mintime = 7;
            flash.count = (RND.P_Random() & flash.maxtime) + 1;
        }

        //
        // STROBE LIGHT FLASHING
        //

        //
        // T_StrobeFlash
        //
        void StrobeFlash(strobe_t flash) {
            if (--flash.count != 0)
                return;

            if (flash.sector.lightlevel == flash.minlight) {
                flash.sector.lightlevel = (short) flash.maxlight;
                flash.count = flash.brighttime;
            } else {
                flash.sector.lightlevel = (short) flash.minlight;
                flash.count = flash.darktime;
            }

        }

        //
        // P_SpawnStrobeFlash
        // After the map has been loaded, scan each sector
        // for specials that spawn thinkers
        //
        void SpawnStrobeFlash(sector_t sector, int fastOrSlow, int inSync) {
            strobe_t flash;

            flash = new strobe_t();

            AddThinker(flash.thinker);

            flash.sector = sector;
            flash.darktime = fastOrSlow;
            flash.brighttime = STROBEBRIGHT;
            flash.thinker.function = think_t.T_StrobeFlash;
            flash.maxlight = sector.lightlevel;
            flash.minlight = FindMinSurroundingLight(sector, sector.lightlevel);

            if (flash.minlight == flash.maxlight)
                flash.minlight = 0;

            // nothing special about it during gameplay
            sector.special = 0;

            if (inSync == 0)
                flash.count = (RND.P_Random() & 7) + 1;
            else
                flash.count = 1;
        }

        //
        // Start strobing lights (usually from a trigger)
        //
        void StartLightStrobing(line_t line) {
            int secnum;
            sector_t sec;

            secnum = -1;
            while ((secnum = FindSectorFromLineTag(line, secnum)) >= 0) {
                sec = LL.sectors[secnum];
                if (sec.specialdata != null)
                    continue;

                SpawnStrobeFlash(sec, SLOWDARK, 0);
            }
        }

        //
        // TURN LINE'S TAG LIGHTS OFF
        //
        void TurnTagLightsOff(line_t line) {
            int i;
            int min;
            sector_t sector;
            sector_t tsec;
            line_t templine;

            for (int j = 0; j < LL.numsectors; j++) {
                sector = LL.sectors[j];
                if (sector.tag == line.tag) {

                    min = sector.lightlevel;
                    for (i = 0; i < sector.linecount; i++) {
                        templine = sector.lines[i];
                        tsec = getNextSector(templine, sector);
                        if (tsec == null)
                            continue;
                        if (tsec.lightlevel < min)
                            min = tsec.lightlevel;
                    }
                    sector.lightlevel = (short) min;
                }
            }
        }

        //
        // TURN LINE'S TAG LIGHTS ON
        //
        void LightTurnOn(line_t line, int bright) {

            sector_t sector;
            sector_t temp;
            line_t templine;

            for (int i = 0; i < LL.numsectors; i++) {
                sector = LL.sectors[i];
                if (sector.tag == line.tag) {
                    // bright = 0 means to search
                    // for highest light level
                    // surrounding sector
                    if (bright == 0) {
                        for (int j = 0; j < sector.linecount; j++) {
                            templine = sector.lines[j];
                            temp = getNextSector(templine, sector);

                            if (temp == null)
                                continue;

                            if (temp.lightlevel > bright)
                                bright = temp.lightlevel;
                        }
                    }
                    sector.lightlevel = (short) bright;
                }
            }
        }

        //
        // Spawn glowing light
        //

        void Glow(glow_t g) {
            switch (g.direction) {
            case -1:
                // DOWN
                g.sector.lightlevel -= GLOWSPEED;
                if (g.sector.lightlevel <= g.minlight) {
                    g.sector.lightlevel += GLOWSPEED;
                    g.direction = 1;
                }
                break;

            case 1:
                // UP
                g.sector.lightlevel += GLOWSPEED;
                if (g.sector.lightlevel >= g.maxlight) {
                    g.sector.lightlevel -= GLOWSPEED;
                    g.direction = -1;
                }
                break;
            }
        }

        void SpawnGlowingLight(sector_t sector) {
            glow_t g;

            g = new glow_t();

            AddThinker(g.thinker);

            g.sector = sector;
            g.minlight = FindMinSurroundingLight(sector, sector.lightlevel);
            g.maxlight = sector.lightlevel;
            g.thinker.function = think_t.T_Glow;
            g.direction = -1;

            sector.special = 0;
        }

    }

    public static final int DI_EAST = 0;

    public static final int DI_NORTHEAST = 1;

    public static final int DI_NORTH = 2;

    public static final int DI_NORTHWEST = 3;

    public static final int DI_WEST = 4;

    public static final int DI_SOUTHWEST = 5;

    public static final int DI_SOUTH = 6;

    public static final int DI_SOUTHEAST = 7;

    public static final int DI_NODIR = 8;

    public static final int NUMDIR = 9;

    //
    // P_NewChaseDir related LUT.
    //
    public final static int opposite[] =
        { DI_WEST, DI_SOUTHWEST, DI_SOUTH, DI_SOUTHEAST, DI_EAST, DI_NORTHEAST,
                DI_NORTH, DI_NORTHWEST, DI_NODIR };

    public final static int diags[] =
        { DI_NORTHWEST, DI_NORTHEAST, DI_SOUTHWEST, DI_SOUTHEAST };

    public final static int[] xspeed =
        { FRACUNIT, 47000, 0, -47000, -FRACUNIT, -47000, 0, 47000 }; // all
                                                                     // fixed

    public final static int[] yspeed =
        { 0, 47000, FRACUNIT, 47000, 0, -47000, -FRACUNIT, -47000 }; // all
                                                                     // fixed

    class Enemies {

        // void A_Fall (mobj_t *actor);

        //
        // ENEMY THINKING
        // Enemies are allways spawned
        // with targetplayer = -1, threshold = 0
        // Most monsters are spawned unaware of all players,
        // but some can be made preaware
        //

        /**
         * P_CheckMeleeRange
         */

        boolean CheckMeleeRange(mobj_t actor) {
            mobj_t pl;
            int dist; // fixed_t

            if (actor.target == null)
                return false;

            pl = actor.target;
            dist = AproxDistance(pl.x - actor.x, pl.y - actor.y);

            if (dist >= MELEERANGE - 20 * FRACUNIT + pl.info.radius)
                return false;

            if (!EN.CheckSight(actor, actor.target))
                return false;

            return true;
        }

        /**
         * P_CheckMissileRange
         */

        boolean CheckMissileRange(mobj_t actor) {
            int dist; // fixed_t

            if (!CheckSight(actor, actor.target))
                return false;

            if ((actor.flags & MF_JUSTHIT) != 0) {
                // the target just hit the enemy,
                // so fight back!
                actor.flags &= ~MF_JUSTHIT;
                return true;
            }

            if (actor.reactiontime != 0)
                return false; // do not attack yet

            // OPTIMIZE: get this from a global checksight
            dist =
                AproxDistance(actor.x - actor.target.x, actor.y
                        - actor.target.y)
                        - 64 * FRACUNIT;

            if (actor.info.meleestate == null)
                dist -= 128 * FRACUNIT; // no melee attack, so fire more

            dist >>= 16;

            if (actor.type == mobjtype_t.MT_VILE) {
                if (dist > 14 * 64)
                    return false; // too far away
            }

            if (actor.type == mobjtype_t.MT_UNDEAD) {
                if (dist < 196)
                    return false; // close for fist attack
                dist >>= 1;
            }

            if (actor.type == mobjtype_t.MT_CYBORG
                    || actor.type == mobjtype_t.MT_SPIDER
                    || actor.type == mobjtype_t.MT_SKULL) {
                dist >>= 1;
            }

            if (dist > 200)
                dist = 200;

            if (actor.type == mobjtype_t.MT_CYBORG && dist > 160)
                dist = 160;

            if (RND.P_Random() < dist)
                return false;

            return true;
        }

        /**
         * P_CheckSight Returns true if a straight line between t1 and t2 is
         * unobstructed. Uses REJECT.
         */

        boolean CheckSight(mobj_t t1, mobj_t t2) {
            int s1;
            int s2;
            int pnum;
            int bytenum;
            int bitnum;

            // First check for trivial rejection.

            // Determine subsector entries in REJECT table.
            s1 = t1.subsector.sector.id; // (t1.subsector.sector - sectors);
            s2 = t2.subsector.sector.id;// - sectors);
            pnum = s1 * LL.numsectors + s2;
            bytenum = pnum >> 3;
            bitnum = 1 << (pnum & 7);

            // Check in REJECT table.
            if (flags(LL.rejectmatrix[bytenum], bitnum)) {
                See.sightcounts[0]++;

                // can't possibly be connected
                return false;
            }

            // An unobstructed LOS is possible.
            // Now look from eyes of t1 to any part of t2.
            See.sightcounts[1]++;

            R.validcount++;

            See.sightzstart = t1.z + t1.height - (t1.height >> 2);
            topslope = (t2.z + t2.height) - See.sightzstart;
            bottomslope = (t2.z) - See.sightzstart;

            See.strace.x = t1.x;
            See.strace.y = t1.y;
            See.t2x = t2.x;
            See.t2y = t2.y;
            See.strace.dx = t2.x - t1.x;
            See.strace.dy = t2.y - t1.y;

            // the head node is the last node output
            return See.CrossBSPNode(LL.numnodes - 1);
        }

        //
        // Called by P_NoiseAlert.
        // Recursively traverse adjacent sectors,
        // sound blocking lines cut off traversal.
        //

        mobj_t soundtarget;

        private void RecursiveSound(sector_t sec, int soundblocks) {
            int i;
            line_t check;
            sector_t other;

            // wake up all monsters in this sector
            if (sec.validcount == R.validcount
                    && sec.soundtraversed <= soundblocks + 1) {
                return; // already flooded
            }

            sec.validcount = R.validcount;
            sec.soundtraversed = soundblocks + 1;
            sec.soundtarget = soundtarget;

            // "peg" to the level loader for syntactic sugar
            side_t[] sides = LL.sides;

            for (i = 0; i < sec.linecount; i++) {
                check = sec.lines[i];
                if ((check.flags & ML_TWOSIDED) == 0)
                    continue;

                LineOpening(check);

                if (openrange <= 0)
                    continue; // closed door

                if (sides[check.sidenum[0]].sector == sec)
                    other = sides[check.sidenum[1]].sector;
                else
                    other = sides[check.sidenum[0]].sector;

                if ((check.flags & ML_SOUNDBLOCK) != 0) {
                    if (soundblocks == 0)
                        RecursiveSound(other, 1);
                } else
                    RecursiveSound(other, soundblocks);
            }
        }

        //
        // P_NoiseAlert
        // If a monster yells at a player,
        // it will alert other monsters to the player.
        //
        void NoiseAlert(mobj_t target, mobj_t emmiter) {
            soundtarget = target;
            R.validcount++;
            RecursiveSound(emmiter.subsector.sector, 0);
        }

        /**
         * P_FireWeapon. Originally in pspr
         */
        public void FireWeapon(player_t player) {
            statenum_t newstate;

            if (!player.CheckAmmo())
                return;

            player.mo.SetMobjState(statenum_t.S_PLAY_ATK1);
            newstate = weaponinfo[player.readyweapon.ordinal()].atkstate;
            player.SetPsprite(player_t.ps_weapon, newstate);
            NoiseAlert(player.mo, player.mo);
        }

        //
        // P_Move
        // Move in the current direction,
        // returns false if the move is blocked.
        //

        // Peg to map movement
        line_t[] spechitp = new line_t[MAXSPECIALCROSS];

        int numspechit;

        /**
         * P_LookForPlayers If allaround is false, only look 180 degrees in
         * front. Returns true if a player is targeted.
         */

        boolean LookForPlayers(mobj_t actor, boolean allaround) {
            int c;
            int stop;
            player_t player;
            // sector_t sector;
            long an; // angle
            int dist; // fixed

            // sector = actor.subsector.sector;

            c = 0;
            stop = (actor.lastlook - 1) & 3;

            for (;; actor.lastlook = (actor.lastlook + 1) & 3) {
                if (!DM.playeringame[actor.lastlook])
                    continue;

                if (c++ == 2 || actor.lastlook == stop) {
                    // done looking
                    return false;
                }

                player = DM.players[actor.lastlook];

                if (player.health[0] <= 0)
                    continue; // dead

                if (!CheckSight(actor, player.mo))
                    continue; // out of sight

                if (!allaround) {
                    an =
                        R.PointToAngle2(actor.x, actor.y, player.mo.x,
                            player.mo.y)
                                - actor.angle;

                    if (an > ANG90 && an < ANG270) {
                        dist =
                            AproxDistance(player.mo.x - actor.x, player.mo.y
                                    - actor.y);
                        // if real close, react anyway
                        if (dist > MELEERANGE)
                            continue; // behind back
                    }
                }

                actor.target = player.mo;
                return true;
            }
            // The compiler complains that this is unreachable
            // return false;
        }

    }

    class Plats {

        public Plats() {
            activeplats = new plat_t[MAXPLATS];
        }

        plat_t[] activeplats;

        //
        // Do Platforms
        // "amount" is only used for SOME platforms.
        //
        boolean DoPlat(line_t line, plattype_e type, int amount) {
            plat_t plat;
            int secnum = -1;
            boolean rtn = false;
            sector_t sec;

            // Activate all <type> plats that are in_stasis
            switch (type) {
            case perpetualRaise:
                ActivateInStasis(line.tag);
                break;

            default:
                break;
            }

            while ((secnum = FindSectorFromLineTag(line, secnum)) >= 0) {
                sec = LL.sectors[secnum];

                if (sec.specialdata != null)
                    continue;

                // Find lowest & highest floors around sector
                rtn = true;
                plat = new plat_t();
                AddThinker(plat.thinker);

                plat.type = type;
                plat.sector = sec;
                plat.sector.specialdata = plat;
                plat.thinker.function = think_t.T_PlatRaise;
                plat.crush = false;
                plat.tag = line.tag;

                switch (type) {
                case raiseToNearestAndChange:
                    plat.speed = PLATSPEED / 2;
                    sec.floorpic = LL.sides[line.sidenum[0]].sector.floorpic;
                    plat.high = FindNextHighestFloor(sec, sec.floorheight);
                    plat.wait = 0;
                    plat.status = plat_e.up;
                    // NO MORE DAMAGE, IF APPLICABLE
                    sec.special = 0;

                    // TODO: S_StartSound((mobj_t *)&sec.soundorg,sfx_stnmov);
                    break;

                case raiseAndChange:
                    plat.speed = PLATSPEED / 2;
                    sec.floorpic = LL.sides[line.sidenum[0]].sector.floorpic;
                    plat.high = sec.floorheight + amount * FRACUNIT;
                    plat.wait = 0;
                    plat.status = plat_e.up;

                    // TODO: S_StartSound((mobj_t *)&sec.soundorg,sfx_stnmov);
                    break;

                case downWaitUpStay:
                    plat.speed = PLATSPEED * 4;
                    plat.low = FindLowestFloorSurrounding(sec);

                    if (plat.low > sec.floorheight)
                        plat.low = sec.floorheight;

                    plat.high = sec.floorheight;
                    plat.wait = 35 * PLATWAIT;
                    plat.status = plat_e.down;
                    // TODO: S_StartSound((mobj_t *)&sec.soundorg,sfx_pstart);
                    break;

                case blazeDWUS:
                    plat.speed = PLATSPEED * 8;
                    plat.low = FindLowestFloorSurrounding(sec);

                    if (plat.low > sec.floorheight)
                        plat.low = sec.floorheight;

                    plat.high = sec.floorheight;
                    plat.wait = 35 * PLATWAIT;
                    plat.status = plat_e.down;
                    // TODO: S_StartSound((mobj_t *)&sec.soundorg,sfx_pstart);
                    break;

                case perpetualRaise:
                    plat.speed = PLATSPEED;
                    plat.low = FindLowestFloorSurrounding(sec);

                    if (plat.low > sec.floorheight)
                        plat.low = sec.floorheight;

                    plat.high = FindHighestFloorSurrounding(sec);

                    if (plat.high < sec.floorheight)
                        plat.high = sec.floorheight;

                    plat.wait = 35 * PLATWAIT;
                    // Guaranteed to be 0 or 1.
                    plat.status = plat_e.values()[RND.P_Random() & 1];

                    // TODO: S_StartSound((mobj_t *)&sec.soundorg,sfx_pstart);
                    break;
                }
                AddActivePlat(plat);
            }
            return rtn;
        }

        void ActivateInStasis(int tag) {
            int i;

            for (i = 0; i < MAXPLATS; i++)
                if ((activeplats[i] != null) && (activeplats[i].tag == tag)
                        && (activeplats[i].status == plat_e.in_stasis)) {
                    (activeplats[i]).status = (activeplats[i]).oldstatus;
                    (activeplats[i]).thinker.function = think_t.T_PlatRaise;
                }
        }

        void StopPlat(line_t line) {
            int j;

            for (j = 0; j < MAXPLATS; j++)
                if ((activeplats[j] != null)
                        && (activeplats[j].status != plat_e.in_stasis)
                        && (activeplats[j].tag == line.tag)) {
                    (activeplats[j]).oldstatus = (activeplats[j]).status;
                    (activeplats[j]).status = plat_e.in_stasis;
                    (activeplats[j]).thinker.function = null;
                }
        }

        void AddActivePlat(plat_t plat) {
            int i;

            for (i = 0; i < MAXPLATS; i++)
                if (activeplats[i] == null) {
                    activeplats[i] = plat;
                    return;
                }
            I.Error("P_AddActivePlat: no more plats!");
        }

        void RemoveActivePlat(plat_t plat) {
            int i;
            for (i = 0; i < MAXPLATS; i++)
                if (plat == activeplats[i]) {
                    (activeplats[i]).sector.specialdata = null;
                    RemoveThinker((activeplats[i]).thinker);
                    activeplats[i] = null;

                    return;
                }
            I.Error("P_RemoveActivePlat: can't find plat!");
        }

    }

    class Sight {
        //
        // P_CheckSight
        //
        int sightzstart; // eye z of looker

        divline_t strace; // from t1 to t2

        int t2x;

        int t2y;

        int[] sightcounts = new int[2];

        /**
         * P_InterceptVector2 Returns the fractional intercept point along the
         * first divline. This is only called by the addthings and addlines
         * traversers.
         * 
         * @param v2
         * @param v1
         * @return
         */

        private int InterceptVector2(divline_t v2, divline_t v1) {
            int frac; // fixed_t
            int num; // fixed_t
            int den; // fixed_t

            den = FixedMul(v1.dy >> 8, v2.dx) - FixedMul(v1.dx >> 8, v2.dy);

            if (den == 0)
                return 0;
            // I_Error ("P_InterceptVector: parallel");

            num =
                FixedMul((v1.x - v2.x) >> 8, v1.dy)
                        + FixedMul((v2.y - v1.y) >> 8, v1.dx);
            frac = FixedDiv(num, den);

            return frac;
        }

        /**
         * P_CrossSubsector Returns true if strace crosses the given subsector
         * successfully.
         */

        boolean CrossSubsector(int num) {
            int seg; // pointer inside segs
            line_t line;
            int s1;
            int s2;
            int count;
            subsector_t sub;
            sector_t front;
            sector_t back;
            int opentop; // fixed_t
            int openbottom;
            divline_t divl = new divline_t();
            vertex_t v1;
            vertex_t v2;
            int frac; // fixed_t
            int slope;

            if (RANGECHECK) {
                if (num >= LL.numsubsectors)
                    I.Error("P_CrossSubsector: ss %i with numss = %i",
                        num, LL.numsubsectors);
            }

            sub = LL.subsectors[num];

            // check lines
            count = sub.numlines;
            seg = sub.firstline;// LL.segs[sub.firstline];

            for (; count > 0; seg++, count--) {
                line = LL.segs[seg].linedef;

                // allready checked other side?
                if (line.validcount == R.validcount)
                    continue;

                line.validcount = R.validcount;

                v1 = line.v1;
                v2 = line.v2;
                s1 = strace.DivlineSide(v1.x, v1.y);
                s2 = strace.DivlineSide(v2.x, v2.y);

                // line isn't crossed?
                if (s1 == s2)
                    continue;

                divl.x = v1.x;
                divl.y = v1.y;
                divl.dx = v2.x - v1.x;
                divl.dy = v2.y - v1.y;
                s1 = divl.DivlineSide(strace.x, strace.y);
                s2 = divl.DivlineSide(t2x, t2y);

                // line isn't crossed?
                if (s1 == s2)
                    continue;

                // stop because it is not two sided anyway
                // might do this after updating validcount?
                if (!flags(line.flags, ML_TWOSIDED))
                    return false;

                // crosses a two sided line
                front = LL.segs[seg].frontsector;
                back = LL.segs[seg].backsector;

                // no wall to block sight with?
                if (front.floorheight == back.floorheight
                        && front.ceilingheight == back.ceilingheight)
                    continue;

                // possible occluder
                // because of ceiling height differences
                if (front.ceilingheight < back.ceilingheight)
                    opentop = front.ceilingheight;
                else
                    opentop = back.ceilingheight;

                // because of ceiling height differences
                if (front.floorheight > back.floorheight)
                    openbottom = front.floorheight;
                else
                    openbottom = back.floorheight;

                // quick test for totally closed doors
                if (openbottom >= opentop)
                    return false; // stop

                frac = InterceptVector2(strace, divl);

                if (front.floorheight != back.floorheight) {
                    slope = FixedDiv(openbottom - sightzstart, frac);
                    if (slope > bottomslope)
                        bottomslope = slope;
                }

                if (front.ceilingheight != back.ceilingheight) {
                    slope = FixedDiv(opentop - sightzstart, frac);
                    if (slope < topslope)
                        topslope = slope;
                }

                if (topslope <= bottomslope)
                    return false; // stop
            }
            // passed the subsector ok
            return true;
        }

        /**
         * P_CrossBSPNode Returns true if strace crosses the given node
         * successfully.
         */

        boolean CrossBSPNode(int bspnum) {
            node_t bsp;
            int side;

            if (flags(bspnum, NF_SUBSECTOR)) {
                if (bspnum == -1)
                    return CrossSubsector(0);
                else
                    return CrossSubsector(bspnum & (~NF_SUBSECTOR));
            }

            bsp = LL.nodes[bspnum];

            // decide which side the start point is on
            side = bsp.DivlineSide(strace.x, strace.y);
            if (side == 2)
                side = 0; // an "on" should cross both sides

            // cross the starting side
            if (!CrossBSPNode(bsp.children[side]))
                return false;

            // the partition plane is crossed here
            if (side == bsp.DivlineSide(t2x, t2y)) {
                // the line doesn't touch the other side
                return true;
            }

            // cross the ending side
            return CrossBSPNode(bsp.children[side ^ 1]);
        }

    }

    //
    // P_InitPicAnims
    //

    /**
     * Floor/ceiling animation sequences, defined by first and last frame, i.e.
     * the flat (64x64 tile) name to be used. The full animation sequence is
     * given using all the flats between the start and end entry, in the order
     * found in the WAD file.
     */

    private final animdef_t[] animdefs =
        {
                new animdef_t(false, "NUKAGE3", "NUKAGE1", 8),
                new animdef_t(false, "FWATER4", "FWATER1", 8),
                new animdef_t(false, "SWATER4", "SWATER1", 8),
                new animdef_t(false, "LAVA4", "LAVA1", 8),
                new animdef_t(false, "BLOOD3", "BLOOD1", 8),

                // DOOM II flat animations.
                new animdef_t(false, "RROCK08", "RROCK05", 8),
                new animdef_t(false, "SLIME04", "SLIME01", 8),
                new animdef_t(false, "SLIME08", "SLIME05", 8),
                new animdef_t(false, "SLIME12", "SLIME09", 8),

                new animdef_t(true, "BLODGR4", "BLODGR1", 8),
                new animdef_t(true, "SLADRIP3", "SLADRIP1", 8),

                new animdef_t(true, "BLODRIP4", "BLODRIP1", 8),
                new animdef_t(true, "FIREWALL", "FIREWALA", 8),
                new animdef_t(true, "GSTFONT3", "GSTFONT1", 8),
                new animdef_t(true, "FIRELAVA", "FIRELAV3", 8),
                new animdef_t(true, "FIREMAG3", "FIREMAG1", 8),
                new animdef_t(true, "FIREBLU2", "FIREBLU1", 8),
                new animdef_t(true, "ROCKRED3", "ROCKRED1", 8),

                new animdef_t(true, "BFALL4", "BFALL1", 8),
                new animdef_t(true, "SFALL4", "SFALL1", 8),
                new animdef_t(true, "WFALL4", "WFALL1", 8),
                new animdef_t(true, "DBRAIN4", "DBRAIN1", 8),
                // Maes: what bullshit. {-1}? Really?!
                new animdef_t(false, "", "", 0) };

    /**
     * These are NOT the same anims found in defines. Dunno why they fucked up
     * this one so bad
     */
    private anim_t[] anims = new anim_t[MAXANIMS];

    public void InitPicAnims() {
        anim_t lstanim = null;
        // Init animation. MAES: sneaky base pointer conversion ;-)
        int lastanim = 0;
        // MAES: for (i=0 ; animdefs[i].istexture != -1 ; i++)
        for (int i = 0; animdefs[i].istexture; i++) {

            if (animdefs[i].istexture) {
                // different episode ?
                if (R.CheckTextureNumForName(animdefs[i].startname) == -1)
                    continue;
                lstanim = anims[lastanim];
                lstanim.picnum = R.TextureNumForName(animdefs[i].endname);
                lstanim.basepic = R.TextureNumForName(animdefs[i].startname);
            } else {
                if (W.CheckNumForName(animdefs[i].startname) == -1)
                    continue;

                lstanim.picnum = R.FlatNumForName(animdefs[i].endname);
                lstanim.basepic = R.FlatNumForName(animdefs[i].startname);
            }

            lstanim.istexture = animdefs[i].istexture;
            lstanim.numpics = lstanim.picnum - lstanim.basepic + 1;

            if (lstanim.numpics < 2)
                I.Error("P_InitPicAnims: bad cycle from %s to %s",
                    animdefs[i].startname, animdefs[i].endname);

            lstanim.speed = animdefs[i].speed;
            lastanim++;
        }
    }

    //
    // SPECIAL SPAWNING
    //

    //

    line_t[] linespeciallist = new line_t[MAXLINEANIMS];

    class Specials {
        public static final int ok = 0, crushed = 1, pastdest = 2;

        public short numlinespecials;

        public anim_t[] anims = new anim_t[MAXANIMS];

        // MAES: was a pointer
        public int lastanim;

        //
        // P_UpdateSpecials
        // Animate planes, scroll walls, etc.
        //
        boolean levelTimer;

        int levelTimeCount;

        public void UpdateSpecials() {
            int pic;
            line_t line;
            anim_t anim;

            // LEVEL TIMER
            if (levelTimer == true) {
                levelTimeCount--;
                if (levelTimeCount == 0)
                    DM.ExitLevel();
            }

            // ANIMATE FLATS AND TEXTURES GLOBALLY

            for (int j = 0; j < lastanim; j++) {
                anim = anims[j];

                for (int i = anim.basepic; i < anim.basepic + anim.numpics; i++) {
                    pic =
                        anim.basepic
                                + ((DM.leveltime / anim.speed + i) % anim.numpics);
                    if (anim.istexture)
                        R.texturetranslation[i] = pic;
                    else
                        R.flattranslation[i] = pic;
                }
            }

            // ANIMATE LINE SPECIALS
            for (int i = 0; i < numlinespecials; i++) {
                line = linespeciallist[i];
                switch (line.special) {
                case 48:
                    // EFFECT FIRSTCOL SCROLL +
                    LL.sides[line.sidenum[0]].textureoffset += FRACUNIT;
                    break;
                }
            }

            // DO BUTTONS
            for (int i = 0; i < MAXBUTTONS; i++)
                if (eval(SW.buttonlist[i].btimer)) {
                    SW.buttonlist[i].btimer--;
                    if (!eval(SW.buttonlist[i].btimer)) {
                        switch (SW.buttonlist[i].where) {
                        case top:
                            LL.sides[SW.buttonlist[i].line.sidenum[0]].toptexture =
                                (short) SW.buttonlist[i].btexture;
                            break;

                        case middle:
                            LL.sides[SW.buttonlist[i].line.sidenum[0]].midtexture =
                                (short) SW.buttonlist[i].btexture;
                            break;

                        case bottom:
                            LL.sides[SW.buttonlist[i].line.sidenum[0]].bottomtexture =
                                (short) SW.buttonlist[i].btexture;
                            break;
                        }
                        ; // TODO:S_StartSound((mobj_t
                          // *)&buttonlist[i].soundorg,sfx_swtchn);
                        // TODO: memset(buttonlist[i],0,sizeof(button_t));
                    }
                }
        }

    }

    class Switches {

        //
        // CHANGE THE TEXTURE OF A WALL SWITCH TO ITS OPPOSITE
        //
        switchlist_t[] alphSwitchList =
            {
                    // Doom shareware episode 1 switches
                    new switchlist_t("SW1BRCOM", "SW2BRCOM", 1),
                    new switchlist_t("SW1BRN1", "SW2BRN1", 1),
                    new switchlist_t("SW1BRN2", "SW2BRN2", 1),
                    new switchlist_t("SW1BRNGN", "SW2BRNGN", 1),
                    new switchlist_t("SW1BROWN", "SW2BROWN", 1),
                    new switchlist_t("SW1COMM", "SW2COMM", 1),
                    new switchlist_t("SW1COMP", "SW2COMP", 1),
                    new switchlist_t("SW1DIRT", "SW2DIRT", 1),
                    new switchlist_t("SW1EXIT", "SW2EXIT", 1),
                    new switchlist_t("SW1GRAY", "SW2GRAY", 1),
                    new switchlist_t("SW1GRAY1", "SW2GRAY1", 1),
                    new switchlist_t("SW1METAL", "SW2METAL", 1),
                    new switchlist_t("SW1PIPE", "SW2PIPE", 1),
                    new switchlist_t("SW1SLAD", "SW2SLAD", 1),
                    new switchlist_t("SW1STARG", "SW2STARG", 1),
                    new switchlist_t("SW1STON1", "SW2STON1", 1),
                    new switchlist_t("SW1STON2", "SW2STON2", 1),
                    new switchlist_t("SW1STONE", "SW2STONE", 1),
                    new switchlist_t("SW1STRTN", "SW2STRTN", 1),

                    // Doom registered episodes 2&3 switches
                    new switchlist_t("SW1BLUE", "SW2BLUE", 2),
                    new switchlist_t("SW1CMT", "SW2CMT", 2),
                    new switchlist_t("SW1GARG", "SW2GARG", 2),
                    new switchlist_t("SW1GSTON", "SW2GSTON", 2),
                    new switchlist_t("SW1HOT", "SW2HOT", 2),
                    new switchlist_t("SW1LION", "SW2LION", 2),
                    new switchlist_t("SW1SATYR", "SW2SATYR", 2),
                    new switchlist_t("SW1SKIN", "SW2SKIN", 2),
                    new switchlist_t("SW1VINE", "SW2VINE", 2),
                    new switchlist_t("SW1WOOD", "SW2WOOD", 2),

                    // Doom II switches
                    new switchlist_t("SW1PANEL", "SW2PANEL", 3),
                    new switchlist_t("SW1ROCK", "SW2ROCK", 3),
                    new switchlist_t("SW1MET2", "SW2MET2", 3),
                    new switchlist_t("SW1WDMET", "SW2WDMET", 3),
                    new switchlist_t("SW1BRIK", "SW2BRIK", 3),
                    new switchlist_t("SW1MOD1", "SW2MOD1", 3),
                    new switchlist_t("SW1ZIM", "SW2ZIM", 3),
                    new switchlist_t("SW1STON6", "SW2STON6", 3),
                    new switchlist_t("SW1TEK", "SW2TEK", 3),
                    new switchlist_t("SW1MARB", "SW2MARB", 3),
                    new switchlist_t("SW1SKULL", "SW2SKULL", 3),

                    new switchlist_t("\0", "\0", 0) };

        int[] switchlist = new int[MAXSWITCHES * 2];

        int numswitches;

        button_t[] buttonlist = new button_t[MAXBUTTONS];

        //
        // P_InitSwitchList
        // Only called at game initialization.
        //
        public void InitSwitchList() {
            int i;
            int index;
            int episode;

            episode = 1;

            if (DM.gamemode == GameMode_t.registered)
                episode = 2;
            else if (DM.gamemode == GameMode_t.commercial)
                episode = 3;

            for (index = 0, i = 0; i < MAXSWITCHES; i++) {
                if (alphSwitchList[i].episode == 0) {
                    numswitches = index / 2;
                    switchlist[index] = -1;
                    break;
                }

                if (alphSwitchList[i].episode <= episode) {
                    /*
                     * // UNUSED - debug? int value; if
                     * (R_CheckTextureNumForName(alphSwitchList[i].name1) < 0) {
                     * system.Error("Can't find switch texture '%s'!",
                     * alphSwitchList[i].name1); continue; } value =
                     * R_TextureNumForName(alphSwitchList[i].name1);
                     */
                    switchlist[index++] =
                        R.TextureNumForName(alphSwitchList[i].name1);
                    switchlist[index++] =
                        R.TextureNumForName(alphSwitchList[i].name2);
                }
            }
        }

        //
        // Start a button counting down till it turns off.
        //
        void StartButton(line_t line, bwhere_e w, int texture, int time) {
            int i;

            // See if button is already pressed
            for (i = 0; i < MAXBUTTONS; i++) {
                if (buttonlist[i].btimer != 0 && buttonlist[i].line == line) {

                    return;
                }
            }

            for (i = 0; i < MAXBUTTONS; i++) {
                if (buttonlist[i].btimer == 0) {
                    buttonlist[i].line = line;
                    buttonlist[i].where = w;
                    buttonlist[i].btexture = texture;
                    buttonlist[i].btimer = time;
                    buttonlist[i].soundorg = line.frontsector.soundorg;
                    return;
                }
            }

            I.Error("P_StartButton: no button slots left!");
        }

        //
        // Function that changes wall texture.
        // Tell it if switch is ok to use again (1=yes, it's a button).
        //
        void ChangeSwitchTexture(line_t line, int useAgain) {
            int texTop;
            int texMid;
            int texBot;
            int i;
            int sound;

            if (!eval(useAgain))
                line.special = 0;

            texTop = LL.sides[line.sidenum[0]].toptexture;
            texMid = LL.sides[line.sidenum[0]].midtexture;
            texBot = LL.sides[line.sidenum[0]].bottomtexture;

            sound = sfxenum_t.sfx_swtchn.ordinal();

            // EXIT SWITCH?
            if (line.special == 11)
                sound = sfxenum_t.sfx_swtchx.ordinal();

            for (i = 0; i < numswitches * 2; i++) {
                if (switchlist[i] == texTop) {
                    // TODO: S_StartSound(buttonlist[0].soundorg,sound);
                    LL.sides[line.sidenum[0]].toptexture =
                        (short) switchlist[i ^ 1];

                    if (eval(useAgain))
                        StartButton(line, bwhere_e.top, switchlist[i],
                            BUTTONTIME);

                    return;
                } else {
                    if (switchlist[i] == texMid) {
                        ; // TODO:(buttonlist.soundorg,sound);
                        LL.sides[line.sidenum[0]].midtexture =
                            (short) switchlist[i ^ 1];

                        if (eval(useAgain))
                            StartButton(line, bwhere_e.middle, switchlist[i],
                                BUTTONTIME);

                        return;
                    } else {
                        if (switchlist[i] == texBot) {
                            ; // TODO:(buttonlist.soundorg,sound);
                            LL.sides[line.sidenum[0]].bottomtexture =
                                (short) switchlist[i ^ 1];

                            if (eval(useAgain))
                                StartButton(line, bwhere_e.bottom,
                                    switchlist[i], BUTTONTIME);

                            return;
                        }
                    }
                }
            }
        }

    }

    //
    // MOVEMENT ITERATOR FUNCTIONS
    //

    interface PIT_LineFunction {
        public boolean invoke(line_t ld);
    }

    interface PIT_MobjFunction {
        public boolean invoke(mobj_t thing);
    }

    enum PIT {
        AddLineIntercepts,
        AddThingIntercepts,
        ChangeSector,
        CheckLine,
        CheckThing,
        StompThing,
        RadiusAttack,
        VileCheck
    }

    enum PTR {
        SlideTraverse,
        AimTraverse,
        ShootTraverse,
        UseTraverse
    }

    // ///////////////// BEGIN MAP OBJECT CODE, USE AS BASIC
    // ///////////////////////

    int test;

    //
    // P_SetMobjState
    // Returns true if the mobj is still present.
    //

    public boolean SetMobjState(mobj_t mobj, statenum_t state) {
        state_t st;

        do {
            if (state == statenum_t.S_NULL) {
                mobj.state = null;
                // TODO:P_RemoveMobj (mobj);
                return false;
            }

            st = states[state.ordinal()];
            mobj.state = st;
            mobj.tics = st.tics;
            mobj.sprite = st.sprite;
            mobj.frame = (int) st.frame;

            // Modified handling.
            // Call action functions when the state is set
            if (st.action.getType() == acp1)
                A.dispatch(st.action, mobj, null);

            state = st.nextstate;
        } while (mobj.tics == 0);

        return true;
    }

    //
    // P_ExplodeMissile
    //
    public void ExplodeMissile(mobj_t mo) {
        mo.momx = mo.momy = mo.momz = 0;

        SetMobjState(mo, mobjinfo[mo.type.ordinal()].deathstate);

        mo.tics -= RND.P_Random() & 3;

        if (mo.tics < 1)
            mo.tics = 1;

        mo.flags &= ~MF_MISSILE;

        if (mo.info.deathsound != null)
            ;
        // TODO: ; // TODO: (mo, mo.info.deathsound);
    }

    //
    // P_RemoveMobj
    //
    mapthing_t[] itemrespawnque = new mapthing_t[ITEMQUESIZE];

    int[] itemrespawntime = new int[ITEMQUESIZE];

    int iquehead;

    int iquetail;

    void RemoveMobj(mobj_t mobj) {
        if (flags(mobj.flags, MF_SPECIAL) && !flags(mobj.flags, MF_DROPPED)
                && (mobj.type != mobjtype_t.MT_INV)
                && (mobj.type != mobjtype_t.MT_INS)) {
            itemrespawnque[iquehead] = mobj.spawnpoint;
            itemrespawntime[iquehead] = DM.leveltime;
            iquehead = (iquehead + 1) & (ITEMQUESIZE - 1);

            // lose one off the end?
            if (iquehead == iquetail)
                iquetail = (iquetail + 1) & (ITEMQUESIZE - 1);
        }

        // unlink from sector and block lists
        UnsetThingPosition(mobj);

        // stop any playing sound
        // TODO: S_StopSound (mobj);

        // free block
        RemoveThinker((thinker_t) mobj);
    }

    //
    // GAME SPAWN FUNCTIONS
    //

    //
    // P_SpawnPuff
    //
    // TODO: extern fixed_t attackrange;

    /**
     * P_SetThingPosition Links a thing into both a block and a subsector based
     * on it's x y. Sets thing.subsector properly
     */

    public void SetThingPosition(mobj_t thing) {
        subsector_t ss;
        sector_t sec;
        int blockx;
        int blocky;
        mobj_t link;

        // link into subsector
        ss = R.PointInSubsector(thing.x, thing.y);
        thing.subsector = ss;

        if (!flags(thing.flags, MF_NOSECTOR)) {
            // invisible things don't go into the sector links
            sec = ss.sector;

            thing.sprev = null;
            thing.snext = sec.thinglist;

            if (sec.thinglist != null)
                sec.thinglist.sprev = thing;

            sec.thinglist = thing;
        }

        // link into blockmap
        if (!flags(thing.flags, MF_NOBLOCKMAP)) {
            // inert things don't need to be in blockmap
            blockx = (thing.x - LL.bmaporgx) >> MAPBLOCKSHIFT;
            blocky = (thing.y - LL.bmaporgy) >> MAPBLOCKSHIFT;

            if (blockx >= 0 && blockx < LL.bmapwidth && blocky >= 0
                    && blocky < LL.bmapheight) {

                link = LL.blocklinks[blocky * LL.bmapwidth + blockx];
                thing.bprev = null;
                thing.bnext = link; // FIXME: will this work?
                if (link != null)
                    // This will work
                    link.bprev = thing;

                // link=thing won't work, assignment should be made directly
                LL.blocklinks[blocky * LL.bmapwidth + blockx] = thing;
            } else {
                // thing is off the map
                thing.bnext = thing.bprev = null;
            }
        }

    }

    // //////////////////////////////// THINKER CODE, GLOBALLY VISIBLE
    // /////////////////

    //
    // THINKERS
    // All thinkers should be allocated by Z_Malloc
    // so they can be operated on uniformly.
    // The actual structures will vary in size,
    // but the first element must be thinker_t.
    //

    /** Both the head and the tail of the thinkers list */
    public thinker_t thinkercap;

    //
    // P_InitThinkers
    //
    public void InitThinkers() {
        thinkercap.next = thinkercap;
        thinkercap.prev = thinkercap;
    }

    /**
     * P_AddThinker Adds a new thinker at the end of the list.
     */

    public void AddThinker(thinker_t thinker) {
        thinkercap.prev.next = thinker;
        thinker.next = thinkercap;
        thinker.prev = thinkercap.prev;
        thinkercap.prev = thinker;
    }

    //
    // P_RemoveThinker
    // Deallocation is lazy -- it will not actually be freed
    // until its thinking turn comes up.
    //
    public void RemoveThinker(thinker_t thinker) {
        // FIXME: NOP.
        thinker.function = null;
    }

    //
    // P_AllocateThinker
    // Allocates memory and adds a new thinker at the end of the list.
    //
    public void AllocateThinker(thinker_t thinker) {
    }

    //
    // P_RunThinkers
    //
    public void RunThinkers() {
        thinker_t currentthinker;

        currentthinker = thinkercap.next;
        while (currentthinker != thinkercap) {
            if (currentthinker.function == null) {
                // time to remove it
                currentthinker.next.prev = currentthinker.prev;
                currentthinker.prev.next = currentthinker.next;
            } else {
                if (currentthinker.function.getType() == acp1)
                    // Execute thinker's function.
                    A.dispatch(currentthinker.function, currentthinker, null);
            }
            currentthinker = currentthinker.next;
        }
    }

    //
    // P_Init
    //
    public void Init() {
        // TODO:
        SW.InitSwitchList();
        InitPicAnims();
        R.InitSprites(sprnames);
    }

    /**
     * P_TouchSpecialThing LIKE ROMERO's ASS!!!
     */
    public void TouchSpecialThing(mobj_t special, mobj_t toucher) {
        player_t player;
        int i;
        int delta;// fixed_t
        sfxenum_t sound;

        delta = special.z - toucher.z;

        if (delta > toucher.height || delta < -8 * FRACUNIT) {
            // out of reach
            return;
        }

        sound = sfxenum_t.sfx_itemup;
        player = toucher.player;

        // Dead thing touching.
        // Can happen with a sliding player corpse.
        if (toucher.health <= 0)
            return;

        // Identify by sprite.
        switch (special.sprite) {
        // armor
        case SPR_ARM1:
            if (!player.GiveArmor(1))
                return;
            player.message = GOTARMOR;
            break;

        case SPR_ARM2:
            if (!player.GiveArmor(2))
                return;
            player.message = GOTMEGA;
            break;

        // bonus items
        case SPR_BON1:
            player.health[0]++; // can go over 100%
            if (player.health[0] > 200)
                player.health[0] = 200;
            player.mo.health = player.health[0];
            player.message = GOTHTHBONUS;
            break;

        case SPR_BON2:
            player.armorpoints[0]++; // can go over 100%
            if (player.armorpoints[0] > 200)
                player.armorpoints[0] = 200;
            if (player.armortype == 0)
                player.armortype = 1;
            player.message = GOTARMBONUS;
            break;

        case SPR_SOUL:
            player.health[0] += 100;
            if (player.health[0] > 200)
                player.health[0] = 200;
            player.mo.health = player.health[0];
            player.message = GOTSUPER;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_MEGA:
            if (DM.gamemode != GameMode_t.commercial)
                return;
            player.health[0] = 200;
            player.mo.health = player.health[0];
            player.GiveArmor(2);
            player.message = GOTMSPHERE;
            sound = sfxenum_t.sfx_getpow;
            break;

        // cards
        // leave cards for everyone
        case SPR_BKEY:
            if (!player.cards[card_t.it_bluecard.ordinal()])
                player.message = GOTBLUECARD;
            player.GiveCard(card_t.it_bluecard);
            if (!DM.netgame)
                break;
            return;

        case SPR_YKEY:
            if (!player.cards[card_t.it_yellowcard.ordinal()])
                player.message = GOTYELWCARD;
            player.GiveCard(card_t.it_yellowcard);
            if (!DM.netgame)
                break;
            return;

        case SPR_RKEY:
            if (!player.cards[card_t.it_redcard.ordinal()])
                player.message = GOTREDCARD;
            player.GiveCard(card_t.it_redcard);
            if (!DM.netgame)
                break;
            return;

        case SPR_BSKU:
            if (!player.cards[card_t.it_blueskull.ordinal()])
                player.message = GOTBLUESKUL;
            player.GiveCard(card_t.it_blueskull);
            if (!DM.netgame)
                break;
            return;

        case SPR_YSKU:
            if (!player.cards[card_t.it_yellowskull.ordinal()])
                player.message = GOTYELWSKUL;
            player.GiveCard(card_t.it_yellowskull);
            if (!DM.netgame)
                break;
            return;

        case SPR_RSKU:
            if (!player.cards[card_t.it_redskull.ordinal()])
                player.message = GOTREDSKULL;
            player.GiveCard(card_t.it_redskull);
            if (!DM.netgame)
                break;
            return;

            // medikits, heals
        case SPR_STIM:
            if (!player.GiveBody(10))
                return;
            player.message = GOTSTIM;
            break;

        case SPR_MEDI:
            if (!player.GiveBody(25))
                return;

            if (player.health[0] < 25)
                player.message = GOTMEDINEED;
            else
                player.message = GOTMEDIKIT;
            break;

        // power ups
        case SPR_PINV:
            if (!player.GivePower(pw_invulnerability))
                return;
            player.message = GOTINVUL;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PSTR:
            if (!player.GivePower(pw_strength))
                return;
            player.message = GOTBERSERK;
            if (player.readyweapon != weapontype_t.wp_fist)
                player.pendingweapon = weapontype_t.wp_fist;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PINS:
            if (!player.GivePower(pw_invisibility))
                return;
            player.message = GOTINVIS;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_SUIT:
            if (!player.GivePower(pw_ironfeet))
                return;
            player.message = GOTSUIT;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PMAP:
            if (!player.GivePower(pw_allmap))
                return;
            player.message = GOTMAP;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PVIS:
            if (!player.GivePower(pw_infrared))
                return;
            player.message = GOTVISOR;
            sound = sfxenum_t.sfx_getpow;
            break;

        // ammo
        case SPR_CLIP:
            if ((special.flags & MF_DROPPED) != 0) {
                if (!player.GiveAmmo(ammotype_t.am_clip, 0))
                    return;
            } else {
                if (!player.GiveAmmo(ammotype_t.am_clip, 1))
                    return;
            }
            player.message = GOTCLIP;
            break;

        case SPR_AMMO:
            if (!player.GiveAmmo(ammotype_t.am_clip, 5))
                return;
            player.message = GOTCLIPBOX;
            break;

        case SPR_ROCK:
            if (!player.GiveAmmo(ammotype_t.am_misl, 1))
                return;
            player.message = GOTROCKET;
            break;

        case SPR_BROK:
            if (!player.GiveAmmo(ammotype_t.am_misl, 5))
                return;
            player.message = GOTROCKBOX;
            break;

        case SPR_CELL:
            if (!player.GiveAmmo(ammotype_t.am_cell, 1))
                return;
            player.message = GOTCELL;
            break;

        case SPR_CELP:
            if (!player.GiveAmmo(ammotype_t.am_cell, 5))
                return;
            player.message = GOTCELLBOX;
            break;

        case SPR_SHEL:
            if (!player.GiveAmmo(ammotype_t.am_shell, 1))
                return;
            player.message = GOTSHELLS;
            break;

        case SPR_SBOX:
            if (!player.GiveAmmo(ammotype_t.am_shell, 5))
                return;
            player.message = GOTSHELLBOX;
            break;

        case SPR_BPAK:
            if (!player.backpack) {
                for (i = 0; i < NUMAMMO; i++)
                    player.maxammo[i] *= 2;
                player.backpack = true;
            }
            for (i = 0; i < NUMAMMO; i++)
                player.GiveAmmo(ammotype_t.values()[i], 1);
            player.message = GOTBACKPACK;
            break;

        // weapons
        case SPR_BFUG:
            if (!player.GiveWeapon(weapontype_t.wp_bfg, false))
                return;
            player.message = GOTBFG9000;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_MGUN:
            if (!player.GiveWeapon(weapontype_t.wp_chaingun,
                (special.flags & MF_DROPPED) != 0))
                return;
            player.message = GOTCHAINGUN;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_CSAW:
            if (!player.GiveWeapon(weapontype_t.wp_chainsaw, false))
                return;
            player.message = GOTCHAINSAW;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_LAUN:
            if (!player.GiveWeapon(weapontype_t.wp_missile, false))
                return;
            player.message = GOTLAUNCHER;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_PLAS:
            if (!player.GiveWeapon(weapontype_t.wp_plasma, false))
                return;
            player.message = GOTPLASMA;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_SHOT:
            if (!player.GiveWeapon(weapontype_t.wp_shotgun,
                (special.flags & MF_DROPPED) != 0))
                return;
            player.message = GOTSHOTGUN;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_SGN2:
            if (!player.GiveWeapon(weapontype_t.wp_supershotgun,
                (special.flags & MF_DROPPED) != 0))
                return;
            player.message = GOTSHOTGUN2;
            sound = sfxenum_t.sfx_wpnup;
            break;

        default:
            I.Error("P_SpecialThing: Unknown gettable thing");
        }

        if ((special.flags & MF_COUNTITEM) != 0)
            player.itemcount++;
        RemoveMobj(special);
        player.bonuscount += player_t.BONUSADD;
        if (player == DM.players[DM.consoleplayer])
            ;
        // TODO: S_StartSound (NULL, sound);
    }

} // End unified map
