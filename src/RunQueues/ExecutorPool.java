package RunQueues;

import java.util.ArrayList;
import java.util.Collection;

public final class ExecutorPool {

 private final TaskQueue mQueue;
 private final Collection mPool;

 public ExecutorPool(int numThreads) {
  if (numThreads < 1) {
   throw new IllegalArgumentException("Must use at least one thread");
  }
  mQueue = new TaskQueue();
  mPool = new ArrayList(numThreads);
  for (int i = 0; i < numThreads; i++) {
   mPool.add(Executor.startRunner(mQueue));
  }
 }

 public void execute(Runnable task) {
  mQueue.enqueue(task);
 }

 public void shutdown() {
  mQueue.shutdown();
 }

}
