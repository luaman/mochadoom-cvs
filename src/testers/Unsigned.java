package testers;

import m.random;

public class Unsigned {

    public static void main(String[] argv){
        int TESTS=100000000;
       //random r=new random();
        
       byte ub=(byte) 0xFF;
       char us=(char) ub;
       
       System.out.println((int)us);
       long a=System.nanoTime();
        for (int i=0;i<TESTS;i++) {
        ub=(byte)random.P_Random();
        us=(char) (0x00FF&ub);
    }
    long b=System.nanoTime();
        System.out.println("Time for "+TESTS+" byte to \"unsigned byte\" casts (with logical ops)"+((b-a)/1e09));

        a=System.nanoTime();
        for (int i=0;i<TESTS;i++) {
        ub=(byte)random.P_Random();
        us=unsigned(ub);
    }
    b=System.nanoTime();
        System.out.println("Time for "+TESTS+" byte to \"unsigned byte\" casts (with num. function) "+((b-a)/1e09));

        a=System.nanoTime();
        for (int i=0;i<TESTS;i++) {
        ub=(byte)random.P_Random();
        us=unsigned2(ub);
        }
    b=System.nanoTime();
        System.out.println("Time for "+TESTS+" byte to \"unsigned byte\" casts (with log. function)"+((b-a)/1e09));
        
        a=System.nanoTime();
        for (int i=0;i<TESTS;i++) {
        ub=(byte)random.P_Random();
        us=(char) ub;
    }
    b=System.nanoTime();
        System.out.println("Time for "+TESTS+" byte to \"unsigned byte\" casts (no casting)"+((b-a)/1e09));
        
}
    public static final char unsigned(byte b){
        return (char) ((b>0)?b:(-b+128));
    }
    
    public static char unsigned2(byte b){
        return (char) (0x00FF&b);
    }
    }
    