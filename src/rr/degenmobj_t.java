package rr;

import m.fixed_t;
import doom.thinker_t;

/**
 *  Each sector has a degenmobj_t in its center
 *  for sound origin purposes.
 *  I suppose this does not handle sound from
 *  moving objects (doppler), because
 *  position is prolly just buffered, not
 *  updated.
 * @author admin
 *
 */

public class degenmobj_t {
public thinker_t       thinker;    // not used for anything
/** (fixed_t) */
public int     x,y,z;

}