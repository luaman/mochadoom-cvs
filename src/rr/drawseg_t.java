package rr;

import m.fixed_t;

//
// ?
//

public class drawseg_t {

     // MAES: was pointer. Not array?
     public  seg_t      curline;
     public int         x1;
     public int         x2;

     public fixed_t     scale1;
     public fixed_t     scale2;
     public fixed_t     scalestep;

     // 0=none, 1=bottom, 2=top, 3=both
     public  int         silhouette;

     // do not clip sprites above this
     public fixed_t     bsilheight;

     // do not clip sprites below this
     public fixed_t     tsilheight;
     
     // Pointers to lists for sprite clipping,
     //  all three adjusted so [x1] is first value.
     public short[]      sprtopclip;     
     public short[]      sprbottomclip;  
     public short[]      maskedtexturecol;
     
}
