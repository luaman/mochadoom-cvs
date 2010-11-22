package p;

import static m.fixed_t.FRACUNIT;

public final class ChaseDirections {

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
    
}
