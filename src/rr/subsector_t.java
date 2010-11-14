package rr;

/**
 * 
 * A SubSector. References a Sector. Basically, this is a list of LineSegs,
 * indicating the visible walls that define (all or some) sides of a convex BSP
 * leaf.
 * 
 * @author admin
 */
public class subsector_t {

	public subsector_t() {
		this(null, (short) 0, (short) 0);
	}

	public subsector_t(sector_t sector, short numlines, short firstline) {
		this.sector = sector;
		this.numlines = numlines;
		this.firstline = firstline;
	}

	// Maes: single pointer
	public sector_t sector;
	public short numlines;
	public short firstline;
	
	public String toString(){
		sb.setLength(0);
		sb.append("Subsector");
		sb.append('\t');
		sb.append("Sector: ");
		sb.append(sector);
		sb.append('\t');
		sb.append("numlines ");
		sb.append(numlines);
		sb.append('\t');
		sb.append("firstline ");
		sb.append(firstline);
		return sb.toString();
		
		
	}
	
	private static StringBuilder sb=new StringBuilder();
	

}
