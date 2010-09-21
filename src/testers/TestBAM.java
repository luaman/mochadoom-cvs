package testers;

import static data.Tables.*;
import static data.SineCosine.*;
import m.FixedFloat;

/** This is a class that tests what happens with BAM angles used in Doom
 *  Normally, they were supposed to be unsigned 32-bit types, mapping
 *  the [0...360) range to a continuum of 32-bit integer values.
 *  
 *  However, in Java we have no unsigned integer type. 
 *  A workaround is to declare angles as ints, and be careful about what operations we perform.
 *  The general rules are:
 *  
 *  a) Angles below ANG180 (1st and 2nd qdt) will compare just fine.
 *  b) Angles in the 3rd and 4th quadrant will compare OK too.
 *  c) However angles from different semiplanes won't compare correctly: they need to be extended to longs.
 *  d) Angle differences, as long a their difference is smaller than ANG180, will compare correctly.
 *  e) However larger ones will fail.
 *  f) So, the best thing would be to expand angles to longs and use the extra value range.
 *  
 * 
 * 
 *
 */

public class TestBAM {

    public static void main(String argv[]){
    System.out.println(ANG90>ANG45);
    System.out.println(ANG180>ANG90); // this resolves wrong
    System.out.println(toLong(ANG180)>ANG90); // this resolves correctly.
    System.out.println(ANG180>ANG90); // this resolves wrong, because ANG180 is "negative" in signed int notation.
    System.out.println(ANG270>ANG180); // but this resolves correctly, because ANG270 is negative but "larger".
    System.out.println(ANG45>ANG270); // this resolves wrong, too. It should be false.
    System.out.println((ANG45>toLong(ANG270))); // this resolves wrong, too. It should be false.
    System.out.println((ANG270-ANG45)>ANG180); // this resolves correctly.
    System.out.println("ZILCH "+((ANG270-ANG45)>ANG90)); // this, however, won't.
    System.out.println((0-ANG45)>ANG180); // this resolves correctly too.
    System.out.println((ANG270-ANG180)>ANG45); // correct.
    System.out.println((ANG270-ANG180)==ANG90); // This is true, and correct. 
    System.out.println((ANG270/2)>ANG90); // This won't work, because it will resolve to a negative.
    System.out.println((ANG270/2)>ANG180); // This won't work either.
    System.out.println((toLong(ANG270)/2)>toLong(ANG180)); // Only this will work.
    
    
    System.out.println(ANG45>>ANGLETOFINESHIFT);
    System.out.println(ANG90>>ANGLETOFINESHIFT);
    System.out.println(ANG90*3);
    System.out.println(ANG45*6);
    System.out.println(ANG270);
    
    
    System.out.println(FixedFloat.toFloat((int) (ANG45>>>ANGLETOFINESHIFT))+" "+finesine[(int) (ANG45>>>ANGLETOFINESHIFT)]);
    System.out.println(FixedFloat.toFloat((int) (ANG270>>>ANGLETOFINESHIFT))+" "+finesine[(int) (ANG270>>>ANGLETOFINESHIFT)]);
    System.out.println(FixedFloat.toFloat((int) ((ANG45*6)>>>ANGLETOFINESHIFT))+" "+finesine[(int) (ANG45*6)>>>ANGLETOFINESHIFT]);
    System.out.println(FixedFloat.toFloat((int) (ANG45>>>ANGLETOFINESHIFT))+" "+finesine[(int) (ANG45>>>ANGLETOFINESHIFT)]);
    System.out.println(FixedFloat.toFloat((int) (ANG270>>>ANGLETOFINESHIFT))+" "+finesine[(int) (ANG270>>>ANGLETOFINESHIFT)]);
    System.out.println(FixedFloat.toFloat((int) ((ANG45*6)>>>ANGLETOFINESHIFT))+" "+finesine[(int) (ANG45*6)>>>ANGLETOFINESHIFT]);
    
    /*for (int i=0;i<tantoangle.length;i++){
        System.out.println(FixedFloat.toFloat(tantoangle[i]));
    }*/
    }

    public static final long toLong(int a){
        return(0xFFFFFFFFL&a);
    }
    
}
