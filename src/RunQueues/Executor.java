package RunQueues;

public final class Executor extends Thread {

 private final TaskQueue mQueue;

 public static Executor startRunner(TaskQueue queue) {
  Executor runner = new Executor(queue);
  runner.start();
  return runner;
 }

 private Executor(TaskQueue queue) {
  mQueue = queue;
 }

 public void run() {
  while (!mQueue.isShutdown()) {
   try {
    Runnable task = mQueue.dequeue();
    if (task != null) {
     task.run();
    }
   }
   catch (Throwable e) {
    // NOTE:  tasks *must* handle their own errors
    e.printStackTrace();
   }
  }
 }

}
