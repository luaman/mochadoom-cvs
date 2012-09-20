package rr;

import static data.Tables.ANG90;
import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.BITS32;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static rr.Lights.LIGHTZSHIFT;
import static rr.Lights.MAXLIGHTZ;

import i.IDoomSystem;

import java.util.Hashtable;

import rr.RendererState.IPlaneDrawer;
import rr.drawfuns.SpanVars;
import utils.C2JUtils;
import v.IVideoScale;
import data.Limits;
import data.Tables;

public abstract class PlaneDrawer<T,V> implements IPlaneDrawer{

    private static final boolean DEBUG2=false;

    // /// FROM PLANES //////

    // HACK: An all zeroes array used for fast clearing of certain visplanes.
    protected int[] BLANKCACHEDHEIGHT;

    // initially.
    protected int MAXVISPLANES = Limits.MAXVISPLANES;
    /** visplane_t*, treat as indexes into visplanes */
    protected int lastvisplane, floorplane, ceilingplane;
    protected visplane_t[] visplanes = new visplane_t[MAXVISPLANES];

    /**
     * openings is supposed to show where "openings" in visplanes start and end
     * e.g. due to sprites, windows etc.
     */
    protected short[] openings;
    /** Maes: this is supposed to be a pointer inside openings */
    protected int lastopening;// =new Short((short) 0);
    protected final boolean RANGECHECK = false;

    protected int skyscale;
    
      /** To treat as fixed_t */
    protected int basexscale, baseyscale;
    
    //
    // spanstart holds the start of a plane span
    // initialized to 0 at start
    //
    protected int[] spanstart, spanstop;

    //
    // texture mapping
    //
    protected V[] planezlight; // The distance lighting effect you see
    /** To treat as fixed_t */
    protected int planeheight;
    /** To treat at fixed_t */
    protected int[] yslope;
    /** To treat as fixed_t */
    protected int[] distscale;

    /** To treat as fixed_t */
    protected int[] cachedheight, cacheddistance, cachedxstep, cachedystep;

    protected final ViewVars view;    
    protected final SpanVars<T,V> dsvars;
    protected final Colormaps<V> colormap;
    protected final TextureManager TexMan;
    protected final IDoomSystem I;
    
    protected PlaneDrawer(RendererState<V> R){
        this.view=R.view;
        this.dsvars=(SpanVars<T, V>) R.dsvars;
        this.colormap=R.colormap;
        this.TexMan=R.getTextureManager();
        this.I=R.I;
        
    }
    
    /**
     * Call only after visplanes have been properly resized for resolution.
     * In case of dynamic resolution changes, the old ones should just be
     * discarded, as they would be nonsensical.
     */

    protected void initVisplanes() {
        C2JUtils.initArrayOfObjects(visplanes);
        }

    /**
     * R_CheckPlane
     * 
     * Called from within StoreWallRange
     * 
     * Presumably decides if a visplane should be split or not?
     * 
     */

    public int CheckPlane(int index, int start, int stop) {

        if (DEBUG2)
            System.out.println("Checkplane " + index + " between " + start
                    + " and " + stop);

        // Interval ?
        int intrl;
        int intrh;

        // Union?
        int unionl;
        int unionh;
        // OK, so we check out ONE particular visplane.
        visplane_t pl = visplanes[index];

        if (DEBUG2)
            System.out.println("Checking out plane " + pl);

        int x;

        // If start is smaller than the plane's min...
        //
        // start minx maxx stop
        // | | | |
        // --------PPPPPPPPPPPPPP-----------
        //
        //
        if (start < pl.minx) {
            intrl = pl.minx;
            unionl = start;
            // Then we will have this:
            //
            // unionl intrl maxx stop
            // | | | |
            // --------PPPPPPPPPPPPPP-----------
            //

        } else {
            unionl = pl.minx;
            intrl = start;

            // else we will have this:
            //
            // union1 intrl maxx stop
            // | | | |
            // --------PPPPPPPPPPPPPP-----------
            //
            // unionl comes before intrl in any case.
            //
            //
        }

        // Same as before, for for stop and maxx.
        // This time, intrh comes before unionh.
        //

        if (stop > pl.maxx) {
            intrh = pl.maxx;
            unionh = stop;
        } else {
            unionh = pl.maxx;
            intrh = stop;
        }

        // An interval is now defined, which is entirely contained in the
        // visplane.
        //

        // If the value FF is NOT stored ANYWWHERE inside it, we bail out
        // early
        for (x = intrl; x <= intrh; x++)
            if (pl.getTop(x) != Character.MAX_VALUE)
                break;

        // This can only occur if the loop above completes,
        // else the visplane we were checking has non-visible/clipped
        // portions within that range: we must split.

        if (x > intrh) {
            // Merge the visplane
            pl.minx = unionl;
            pl.maxx = unionh;
            // System.out.println("Plane modified as follows "+pl);
            // use the same one
            return index;
        }

        // SPLIT: make a new visplane at "last" position, copying materials
        // and light.

        if (lastvisplane == visplanes.length) {
            //  visplane overflows could occur at this point.
            ResizeVisplanes();
        }

        visplanes[lastvisplane].height = pl.height;
        visplanes[lastvisplane].picnum = pl.picnum;
        visplanes[lastvisplane].lightlevel = pl.lightlevel;

        pl = visplanes[lastvisplane++];
        pl.minx = start;
        pl.maxx = stop;

        // memset (pl.top,0xff,sizeof(pl.top));
        pl.clearTop();

        // return pl;

        // System.out.println("New plane created: "+pl);
        return lastvisplane - 1;
    }

    /**
     * R_ClearPlanes At begining of frame.
     * 
     */

    @Override
    public void ClearPlanes() {
        int angle;

        /*
         * View planes are cleared at the beginning of every plane, by
         * setting them "just outside" the borders of the screen (-1 and
         * viewheight).
         */

        // Point to #1 in visplane list? OK... ?!
        lastvisplane = 0;

        // We point back to the first opening of the list openings[0],
        // again.
        lastopening = 0;

        // texture calculation
        System.arraycopy(BLANKCACHEDHEIGHT, 0, cachedheight, 0,
                BLANKCACHEDHEIGHT.length);

        // left to right mapping
        // FIXME: If viewangle is ever < ANG90, you're fucked. How can this
        // be prevented?
        // Answer: 32-bit unsigned are supposed to roll over. You can & with
        // 0xFFFFFFFFL.
        angle = (int) Tables.toBAMIndex(view.angle - ANG90);

        // scale will be unit scale at SCREENWIDTH/2 distance
        basexscale = FixedDiv(finecosine[angle], view.centerxfrac);
        baseyscale = -FixedDiv(finesine[angle], view.centerxfrac);
    }



    /**
     * R_MapPlane
     * 
     * Called only by R_MakeSpans.
     * 
     * This is where the actual span drawing function is called.
     * 
     * Uses global vars: planeheight ds_source -> flat data has already been
     * set. basexscale -> actual drawing angle and position is computed from
     * these baseyscale viewx viewy
     * 
     * BASIC PRIMITIVE
     */

    public void MapPlane(int y, int x1, int x2) {
        // MAES: angle_t
        int angle;
        // fixed_t
        int distance;
        int length;
        int index;

        if (RANGECHECK) {
            rangeCheck(x1,x2,y);
        }

        if (planeheight != cachedheight[y]) {
            cachedheight[y] = planeheight;
            distance = cacheddistance[y] = FixedMul(planeheight, yslope[y]);
            dsvars.ds_xstep = cachedxstep[y] = FixedMul(distance, basexscale);
            dsvars.ds_ystep = cachedystep[y] = FixedMul(distance, baseyscale);
        } else {
            distance = cacheddistance[y];
            dsvars.ds_xstep = cachedxstep[y];
            dsvars.ds_ystep = cachedystep[y];
        }

        length = FixedMul(distance, distscale[x1]);
        angle = (int) (((view.angle + view.xtoviewangle[x1]) & BITS32) >>> ANGLETOFINESHIFT);
        dsvars.ds_xfrac = view.x + FixedMul(finecosine[angle], length);
        dsvars.ds_yfrac = -view.y - FixedMul(finesine[angle], length);

        if (colormap.fixedcolormap != null)
            dsvars.ds_colormap = colormap.fixedcolormap;
        else {
            index = distance >>> LIGHTZSHIFT;

            if (index >= MAXLIGHTZ)
                index = MAXLIGHTZ - 1;

            dsvars.ds_colormap = planezlight[index];
        }

        dsvars.ds_y = y;
        dsvars.ds_x1 = x1;
        dsvars.ds_x2 = x2;

        // high or low detail
        dsvars.spanfunc.invoke();
    }

    protected final void rangeCheck(int x1,int x2,int y) {
        if (x2 < x1 || x1 < 0 || x2 >= view.width || y > view.height)
            I.Error("%s: %d, %d at %d",this.getClass().getName(), x1, x2, y);
        }
    
    /**
     * R_FindPlane
     * 
     * Checks whether a visplane with the specified height, picnum and light
     * level exists among those already created. This looks like a half-assed
     * attempt at reusing already existing visplanes, rather than creating new
     * ones. The tricky part is understanding what happens if one DOESN'T exist.
     * Called only from within R_Subsector (so while we're still trasversing
     * stuff).
     * 
     * @param height
     *            (fixed_t)
     * @param picnum
     * @param lightlevel
     * @return was visplane_t*, returns index into visplanes[]
     */

    public final int FindPlane(int height, int picnum, int lightlevel) {
        // System.out.println("\tChecking for visplane merging...");
        int check = 0; // visplane_t*
        visplane_t chk = null;

        if (picnum == TexMan.getSkyFlatNum()) {
            height = 0; // all skys map together
            lightlevel = 0;
        }

        chk = visplanes[0];

        // Find visplane with the desired attributes
        for (check = 0; check < lastvisplane; check++) {

            chk = visplanes[check];
            if (height == chk.height && picnum == chk.picnum
                    && lightlevel == chk.lightlevel) {
                // Found a visplane with the desired specs.
                break;
            }
        }

        if (check < lastvisplane) {
            return check;
        }

        // Found a visplane, but we can't add anymore.
        if (lastvisplane == visplanes.length) {
            // I.Error ("R_FindPlane: no more visplanes");
            ResizeVisplanes();

        }

        /*
         * FIXED: we need to add this post-fix here because of the way the
         * original was structured (pointer hacks, too lengthy to explain). We
         * need to make sure that when no visplane is found a "failed check"
         * will actually result in a pointer to the next "free" visplane, and
         * that we always have a valid pointer to visplane 0, even if the loop
         * never ran. This fixes the "blinking visplane bug", which manifested
         * itself when sector lighting effects changed the light level
         */

        chk = visplanes[check];
        // Add a visplane
        lastvisplane++;
        chk.height = height;
        chk.picnum = picnum;
        chk.lightlevel = lightlevel;
        chk.minx = SCREENWIDTH;
        chk.maxx = -1;
        // memset (chk.top,0xff,sizeof(chk.top));
        chk.clearTop();

        return check;
    }

    /**
     * A hashtable used to retrieve planes with particular attributes faster
     * -hopefully-. The planes are still stored in the visplane array for
     * convenience, but we can search them in the hashtable too -as a bonus, we
     * can reuse previously created planes that match newer ones-.
     */
    Hashtable<visplane_t, Integer> planehash = new Hashtable<visplane_t, Integer>(
            128);
    visplane_t check = new visplane_t();

    protected final int FindPlane2(int height, int picnum, int lightlevel) {
        // System.out.println("\tChecking for visplane merging...");
        // int check=0; // visplane_t*
        visplane_t chk = null;
        Integer checknum;

        if (picnum == TexMan.getSkyFlatNum()) {
            height = 0; // all skys map together
            lightlevel = 0;
        }

        // Try and find this.
        check.lightlevel = lightlevel;
        check.picnum = picnum;
        check.height = height;
        check.updateHashCode();

        /*
         * chk=visplanes[0];
         * 
         * // Find visplane with the desired attributes for (check=0;
         * check<lastvisplane; check++) {
         * 
         * chk=visplanes[check]; if (height == chk.height && picnum ==
         * chk.picnum && lightlevel ==chk.lightlevel) { // Found a visplane with
         * the desired specs. break; } }
         */

        checknum = planehash.get(check);

        // Something found, get it.

        if (!(checknum == null)) {

            // Visplane exists and is within those allocated in the current tic.
            if (checknum < lastvisplane) {
                return checknum;
            }

            // Found a visplane, but we can't add anymore.
            // Resize right away. This shouldn't take too long.
            if (lastvisplane == MAXVISPLANES) {
                // I.Error ("R_FindPlane: no more visplanes");
                ResizeVisplanes();
            }
        }

        /*
         * FIXED: we need to add this post-fix here because of the way the
         * original was structured (pointer hacks, too lengthy to explain). We
         * need to make sure that when no visplane is found a "failed check"
         * will actually result in a pointer to the next "free" visplane, and
         * that we always have a valid pointer to visplane 0, even if the loop
         * never ran. This fixes the "blinking visplane bug", which manifested
         * itself when sector lighting effects changed the light level
         */

        // We found a visplane (possibly one allocated on a previous tic)
        // but we can't link directly to it, we need to copy its data
        // around.

        checknum = new Integer(Math.max(0, lastvisplane));

        chk = visplanes[checknum];
        // Add a visplane
        lastvisplane++;
        chk.height = height;
        chk.picnum = picnum;
        chk.lightlevel = lightlevel;
        chk.minx = SCREENWIDTH;
        chk.maxx = -1;
        chk.updateHashCode();
        planehash.put(chk, checknum);
        // memset (chk.top,0xff,sizeof(chk.top));
        chk.clearTop();

        return checknum;
    }

        
    /**
     * R_MakeSpans
     * 
     * Called only by DrawPlanes. If you wondered where the actual
     * boundaries for the visplane flood-fill are laid out, this is it.
     * 
     * The system of coords seems to be defining a sort of cone.
     * 
     * 
     * @param x
     *            Horizontal position
     * @param t1
     *            Top-left y coord?
     * @param b1
     *            Bottom-left y coord?
     * @param t2
     *            Top-right y coord ?
     * @param b2
     *            Bottom-right y coord ?
     * 
     */

    protected void MakeSpans(int x, int t1, int b1, int t2, int b2) {

        // If t1 = [sentinel value] then this part won't be executed.
        while (t1 < t2 && t1 <= b1) {
            this.MapPlane(t1, spanstart[t1], x - 1);
            t1++;
        }
        while (b1 > b2 && b1 >= t1) {
            this.MapPlane(b1, spanstart[b1], x - 1);
            b1--;
        }

        // So...if t1 for some reason is < t2, we increase t2 AND store the
        // current x
        // at spanstart [t2] :-S
        while (t2 < t1 && t2 <= b2) {
            // System.out.println("Increasing t2");
            spanstart[t2] = x;
            t2++;
        }

        // So...if t1 for some reason b2 > b1, we decrease b2 AND store the
        // current x
        // at spanstart [t2] :-S

        while (b2 > b1 && b2 >= t2) {
            // System.out.println("Decreasing b2");
            spanstart[b2] = x;
            b2--;
        }
    }

    /**
     * R_InitPlanes Only at game startup.
     */

    public void InitPlanes() {
        // Doh!
    }

    // //////////////////////////VIDEO SCALE STUFF
    // ////////////////////////////////

    protected int SCREENWIDTH;
    protected int SCREENHEIGHT;
    protected IVideoScale vs;

    @Override
    public void setVideoScale(IVideoScale vs) {
        this.vs = vs;
    }

    @Override
    public void initScaling() {
        this.SCREENHEIGHT = vs.getScreenHeight();
        this.SCREENWIDTH = vs.getScreenWidth();

        // Pre-scale stuff.

        spanstart = new int[SCREENHEIGHT];
        spanstop = new int[SCREENHEIGHT];
        yslope = new int[SCREENHEIGHT];
        distscale = new int[SCREENWIDTH];
        cachedheight = new int[SCREENHEIGHT];
        cacheddistance = new int[SCREENHEIGHT];
        cachedxstep = new int[SCREENHEIGHT];
        cachedystep = new int[SCREENHEIGHT];

        // HACK: visplanes are initialized globally.
        visplane_t.setVideoScale(vs);
        visplane_t.initScaling();
        initVisplanes();

    }

    // ///////////// VARIOUS BORING GETTERS ////////////////////

    @Override
    public int[] getCachedHeight() {
        return this.cachedheight;
    }

    @Override
    public int[] getCachedDistance() {
        return this.cacheddistance;
    }

    @Override
    public int[] getCachedXStep() {
        return cachedxstep;
    }

    @Override
    public int[] getCachedYStep() {
        return cachedystep;
    }

    @Override
    public int[] getDistScale() {
        return distscale;
    }

    @Override
    public int[] getYslope() {
        return yslope;
    }

    @Override
    public int getBaseXScale() {
        return basexscale;
    }

    @Override
    public int getBaseYScale() {
        return baseyscale;
    }

    @Override
    public int getSkyScale(){
        return skyscale;
    }
    
    public void setSkyScale(int i) {
        skyscale=i;
        
    }
    
    @Override
    public final void ResizeVisplanes() {
        // Bye bye, old visplanes.
        visplanes = C2JUtils.resize(visplanes[0], visplanes, visplanes.length*2);
    }

}
