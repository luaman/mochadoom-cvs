package testers;

import static data.Tables.*;
import static data.SineCosine.*;
import m.FixedFloat;



public class TestBAM {

    public static void main(String argv[]){
    System.out.println(ANG90>ANG45);
    System.out.println(ANG180>ANG90);
    System.out.println(ANG270>ANG180);
    System.out.println(ANG45>ANG270);

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
    
    for (int i=0;i<tantoangle.length;i++){
        System.out.println(FixedFloat.toFloat(tantoangle[i]));
    }
    }

    
    
}
