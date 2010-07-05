package testers;

import v.FastTranspose;

public class TestFastTranspose {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int length=11;
        byte[] x=new byte[1<<(length<<1)];
        byte[] scratch=new byte[1<<(FastTranspose.BL<<1)];

        for(int i=0;i< 1<<(length<<1); i++ ) x[i]=(byte) ((i%256)-128);
        FastTranspose.blocktranspose(x, scratch, length);
    }

}
