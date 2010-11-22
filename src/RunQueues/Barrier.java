package RunQueues;

public final class Barrier {
    public volatile int numThreads;

    public Barrier(int n) {
        numThreads = n;
// Initialise the IsDone array. The choice of initial value is
// arbitrary, but must be consistent!
        IsDone = new boolean[numThreads];
        for (int i = 0; i < numThreads; i++) {
            IsDone[i] = false;
        }
    }

    public void setMaxBusyIter(int b) {
        maxBusyIter = b;
    }

    public void DoBarrier(int myid) {
        int b;
        int roundmask = 3;
        boolean donevalue = !IsDone[myid];
        while (((myid & roundmask) == 0) && (roundmask < (numThreads << 2))) {
            int spacing = (roundmask + 1) >> 2;
            for (int i = 1; i <= 3 && myid + i * spacing < numThreads; i++) {
                b = maxBusyIter;
                while (IsDone[myid + i * spacing] != donevalue) {
                    b--;
                    if (b == 0) {
                         Thread.yield();
                        b = maxBusyIter;
                    }
                }
            }
            roundmask = (roundmask << 2) + 3;
        }

        IsDone[myid] = donevalue;
        b = maxBusyIter;
        while (IsDone[0] != donevalue) {
            b--;
            if (b == 0) {
                Thread.yield();
                b = maxBusyIter;
            }
        }

    }

// Array of flags indicating whether the given process and all those
// for which it is responsible have finished. The "sense" of this
// array alternates with each barrier, to prevent having to
// reinitialise.
    volatile boolean[] IsDone;
    public int maxBusyIter = 1;
}
