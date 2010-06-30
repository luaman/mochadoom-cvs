package testers;

import utils.PrintfFormat;
import m.Swap;

public class TestSwap {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        short a=(short)0xBEEF;
        char c=(char)0xBABE;
        int b=0XDEADBABE;
        
        System.out.print(new PrintfFormat("%X\n").sprintf(a));
        System.out.print(new PrintfFormat("%X\n").sprintf(a));
        System.out.print(new PrintfFormat("%X\n").sprintf(b));
        System.out.print(new PrintfFormat("%X\n").sprintf(c));
        a=Swap.SHORT(a);
        b=Swap.LONG(b);
        c=(char)Swap.SHORT(c);
        System.out.print(new PrintfFormat("%X\n").sprintf(a));
        System.out.print(new PrintfFormat("%X\n").sprintf(b));
        System.out.print(new PrintfFormat("%X\n").sprintf(c));
        c=(char)Swap.SHORT(a);
        System.out.print(new PrintfFormat("%X\n").sprintf(c));
        char aa=Swap.USHORT((char)a);
        System.out.print(new PrintfFormat("%X\n").sprintf((char)a));
        System.out.print(new PrintfFormat("%X\n").sprintf(aa));

    }

}
