package psort;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThreadSort<E extends Comparable<E> > {
    
    public static final int NUMTHREADS=2;
    Executor ste;
    CyclicBarrier sortbarrier;
    int limit;
    E[] stuff;
    SorterThread<E> h1,h2;
    
    public ThreadSort(E[] stuff){
        this.stuff=stuff;
        ste=Executors.newFixedThreadPool(NUMTHREADS);
        sortbarrier=new CyclicBarrier(NUMTHREADS+1);
        h1=new SorterThread<E>(stuff);
        h2=new SorterThread<E>(stuff);
    }
    
    public void sortLimit(int limit){
        this.limit=limit;
    }
    
    /** Setting tdepth to 1 will keep branching to two threads.
     * 
     * @param tdepth
     */
    public void sort(int tdepth) { 
        sort(0, this.limit, 0, tdepth); 
        return; 
     }
    
    /**
     * 
     * @param n1 Left bound
     * @param n2 Right bound
     * @param depth Current depth
     * @param tdepth Depth at which to initiate a parallel split. 1 means immediately.
     */
     protected void sort(int n1, int n2, int depth, int tdepth) {


       // System.out.printf("Trying %d to %d pivot %d\n",n1,n2,((n1 + n2) / 2));
        // Partitioning.
            int pivot=partition(n1,n2,(n1+n2)/2);
        
            h1.setParams(n1, pivot); 
            h2.setParams(pivot+1, n2); 
           // System.out.printf("1: From %d to %d\n",n1,r);
            ste.execute(h1); 
           // System.out.printf("2: From %d to %d\n",l,n2);
           ste.execute(h2);
           
           try { 
              sortbarrier.await();
              //System.out.printf("Done!\n");
             // sortbarrier.reset();
           } catch (Exception e) { 
              e.printStackTrace(); 
           } 
     } 
     
     private int partition(int lo, int hi, int pivotIndex)
     {
        E pivotValue = stuff[ pivotIndex ];
      
        swap(pivotIndex, hi); //send pivot item to the back
      
        int index = lo; //keep track of where the front ends
      
        for (int i = lo; i < hi; i++) //check from the front to the back
        {
           //swap if the current value is less than the pivot
           if ( (stuff[i]).compareTo(pivotValue) <= 0 )
           {
              swap( i, index);
              index++;
           }
        }
      
        swap( hi, index); //put pivot item in the middle
      
        return index;
     }
     
     protected final int split(int lower,int upper)
     {
        E i,t;
        int p,q;
        p=lower+1;
        q=upper;
        i=stuff[lower];
        while(q>=p)
          {
         while(stuff[p].compareTo(i)<0)
            p++;
         while(stuff[q].compareTo(i)>0)
            q--;
         if(q>p)
          {
            t=stuff[p];
            stuff[p]=stuff[q];
            stuff[q]=t;
          }
          }
        t=stuff[lower];
        stuff[lower]=stuff[q];
        stuff[q]=t;
        return q;
     }
        
        protected final void swap(int p,int q){
            tmp=stuff[p];
            stuff[p]=stuff[q];
            stuff[q]=tmp;
        }
        
        E tmp;

 
 class SorterThread<E> implements Runnable { 
     private int n1; 
     private int n2; 
    // private int depth; 
    // private int tdepth; 
     private E[] stuff;
     
     public SorterThread(E[] stuff){
         this.stuff=stuff;
     } 
     
     public void setParams(int n1, int n2) { 
         this.n1 = n1; 
         this.n2 = n2; 

      } 
     
     public void run() { 
         if (n1<=n2)
         Arrays.sort(stuff,n1, n2); 
        try {
            sortbarrier.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     } 
 } 

}
