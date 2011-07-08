package testers;

import p.Actions;
import p.mobj_t;
import pooling.AudioChunkPool;
import pooling.MobjPool;
import s.AudioChunk;

public class TestMobjPooling {
    public static final int TESTS=1000000;
    
    public static void main(String[] argv){
    
    mobj_t[] chunks=new mobj_t[TESTS];
    MobjPool chunkpool=new MobjPool(new Actions());
    
    long a=System.nanoTime();
    
    for (int i=0;i<TESTS;i++){
        chunks[i]=new mobj_t();
    }

    for (int i=0;i<TESTS;i++){
        chunks[i]=new mobj_t();
    }
    
    for (int i=0;i<TESTS;i++){
        chunks[i]=new mobj_t();
    }
    
    for (int i=0;i<TESTS;i++){
        chunks[i]=new mobj_t();
    }
    long b=System.nanoTime();
    
    System.out.println("Time: "+(float)(b-a)/1000000000f);
    
    a=System.nanoTime();
    
    for (int i=0;i<TESTS;i++){
        chunks[i]=chunkpool.checkOut();
    }

    for (int i=0;i<TESTS;i++){
        chunkpool.checkIn(chunks[i]);
        chunks[i]=chunkpool.checkOut();
    }
    
    for (int i=0;i<TESTS;i++){
        chunkpool.checkIn(chunks[i]);
        chunks[i]=chunkpool.checkOut();
    }
    
    for (int i=0;i<TESTS;i++){
        chunkpool.checkIn(chunks[i]);
        chunks[i]=chunkpool.checkOut();
    }
    b=System.nanoTime();
    
    System.out.println("Time: "+(float)(b-a)/1000000000f);
    
    
    }
    

}
