package rr;

/**
 *
 * A SubSector.
 * References a Sector.
 * Basically, this is a list of LineSegs,
 * indicating the visible walls that define
 * (all or some) sides of a convex BSP leaf.
 *
 * @author admin
  */
public class subsector_t {
    
     public subsector_t(sector_t sector, short numlines, short firstline) {
        super();
        this.sector = sector;
        this.numlines = numlines;
        this.firstline = firstline;
    }
    // Maes: single pointer
    public sector_t   sector;
     public short   numlines;
     public short   firstline;
}    
 
