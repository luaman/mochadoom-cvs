package rr;

import p.mobj_t;
import m.fixed_t;
import doom.thinker_t;

/**
 *  Each sector has a degenmobj_t in its center
 *  for sound origin purposes.
 *  I suppose this does not handle sound from
 *  moving objects (doppler), because
 *  position is prolly just buffered, not
 *  updated.
 *
 * MAES: this is actually a cut down "degenerate" mobj_t, with
 * only the 4 first fields retained. The "thinker" field is a dummy
 * used for memory alignment, the x y z ones are the ones actually used.
 * In practice, this was "passed" whenever a mobj_t was expected.
 * Not a problem in C, but sucks to do that in Java. 
 *
 */

public class degenmobj_t extends mobj_t {
public thinker_t       thinker;    // not used for anything
/** (fixed_t) */
public int     x,y,z;

}