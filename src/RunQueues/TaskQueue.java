package RunQueues;

import java.util.LinkedList;
import java.util.List;

public final class TaskQueue {

 private final List mQueue;

 private boolean mIsShutdown;

 public TaskQueue() {
  mQueue = new LinkedList();
 }

 public Runnable dequeue() throws InterruptedException {
  synchronized (mQueue) {
   while (!isShutdown() && mQueue.isEmpty()) {
    mQueue.wait();
   }
   if (mQueue.isEmpty()) {
    return null;
   }
   else {
    Runnable result = (Runnable) mQueue.get(0);
    mQueue.remove(result);
    return result;
   }
  }
 }

 public void enqueue(Runnable task) {
  synchronized (mQueue) {
   mQueue.add(task);
   mQueue.notifyAll();
  }
 }

 public boolean isShutdown() {
  synchronized (mQueue) {
   return mIsShutdown;
  }
 }

 public void shutdown() {
  synchronized (mQueue) {
   mIsShutdown = true;
   mQueue.notifyAll();
  }
 }

}
