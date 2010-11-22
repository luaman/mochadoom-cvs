package RunQueues;

public final class FastBarrier {
    private int numThreads, maxBusyIter;
    private volatile boolean[] IsDone;

    public FastBarrier(int n) {
        numThreads = n;
        maxBusyIter = Math.max(numThreads-1,1);
        IsDone = new boolean[numThreads];
        for (int i = 0; i < numThreads; i++) {
            IsDone[i] = false;
        }

    }

    public void setMaxBusyIter(int b) {
        maxBusyIter = b;
    }

    public void gather(int ThreadID) {
        int b = maxBusyIter;
        int allOK=1;
        boolean donevalue = !IsDone[ThreadID];

//      System.out.println("Thread "+ThreadID+" entered barrier");
        // Thread 0 will have to wait for all the others.

          while ((ThreadID==0)&&(allOK<numThreads)){
              allOK=1;
            for (int i = 1; i < numThreads; i++) {
                if (IsDone[i] != donevalue) {
                    // No match? either retry immediately or yield.
                    b--;
                    if (b == 0) {
                        Thread.yield();
                        b = maxBusyIter;
                    }
                } else {allOK++;}
            }
        }
        // Don't leave just yet....

//      System.out.println("Thread "+ThreadID+" setting value");
        IsDone[ThreadID] = donevalue;
        while (IsDone[0] != donevalue) {
            b--;
            if (b == 0) {
                Thread.yield();
                b = maxBusyIter;
            }
        }
    }
}

