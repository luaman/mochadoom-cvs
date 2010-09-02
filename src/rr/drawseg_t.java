package rr;

//
// ?
//

public class drawseg_t {

     /** MAES: was pointer. Not array? */
     public  seg_t      curline;
     public int         x1, x2;

     /** fixed_t */
     public int     scale1,  scale2,  scalestep;

     /** 0=none, 1=bottom, 2=top, 3=both */
     public  int         silhouette;

     /** do not clip sprites above this (fixed_t) */
     public int     bsilheight;

     /** do not clip sprites below this (fixed_t) */
     public int     tsilheight;
     
     /** Indexes to lists for sprite clipping, 
        all three adjusted so [x1] is first value. */
     public int      sprtopclip, sprbottomclip, maskedtexturecol;

     /** Pointers to the actual lists  */

     public short[]      l_sprtopclip, l_sprbottomclip, l_lmaskedtexturecol;
     
}
