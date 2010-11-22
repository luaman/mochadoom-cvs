/*
Copyright (c) 2000, Thomas W. Christopher and George K. Thiruvathukal

Java and High Performance Computing (JHPC) Organzization
Tools of Computing LLC

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

The names Java and High-Performance Computing (JHPC) Organization,
Tools of Computing LLC, and/or the names of its contributors may not
be used to endorse or promote products derived from this software
without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

This license is based on version 2 of the BSD license. For more
information on Open Source licenses, please visit
http://opensource.org.
*/



package RunQueues;

// package jhpc.thread;
import java.util.*;

/**
A FIFO queue.
A get() [dequeue] will return immediately with a Future
if the queue is empty. The operation look() will return a Future for the
first element in the queue. Methods getSkip() and lookSkip() will return
the element itself, not a future, or null if the queue is empty.
Used in SharedTableOfQueues.
@author Thomas W. Christopher (Tools of Computing LLC)
@version 0.2 Beta
*/
public class FutureQueue {
/**
Is the queue of objects.
*/
	QueueComponent q=new QueueComponent();
/**
Is a queue of Futures each of which is being waited on by one thread
trying to get an object.
*/
	QueueComponent qf=new QueueComponent();
/**
Is a single Future being waited on by one or more threads trying to
do a lookup. If there are no threads waiting on a lookup,
lf==null.
*/
	Future lf=null;

/**
Is the future factory used to create the futures that will be returned.
It will be created to specify the RunQueue upon which runDelayed objects
will be placed.
*/
	FutureFactory ff=null;

/**
The empty constructor. The Futures use their own class RunQueue.
*/

public FutureQueue(){ff=new FutureFactory(Future.getClassRunQueue());}

/**
The constructor that specifies a RunQueue upon which runDelayed
objects will be placed.
@param r The RunQueue for runDelayed objects.
*/

public FutureQueue(RunQueue r){ff=new FutureFactory(r);}

/**
The constructor that specifies a FutureFactory to use to create
the futures.
@param f The FutureFactory.
*/

public FutureQueue(FutureFactory f){ff=f;}

/**
Put an object into the queue.
@param obj The object to place in the queue. If you put null into the
queue, the getSkip() and lookSkip() methods will not be able to
distinguish it from an empty queue.
*/
 	public synchronized void put(Object obj) {
		Future f;
		if (!qf.isEmpty()){
			f=(Future)qf.get();
			f.setValue(obj);
		} else {
			q.put(obj);
		}
	}
/**
See if the queue is not in use.
@return True if the queue is empty and there are no Futures outstanding.
I.e., it returns true if and only if there are no elements in the queue
and no threads are waiting for a get() or a look() method to complete.
*/
	public synchronized boolean isVacant() {
		return	(q.isEmpty()) &&
			(qf.isEmpty()) &&
			lf==null ;
	}
/**
Is the queue empty.
@return True if there are no objects (or nulls) in the queue.
*/
	public synchronized boolean isEmpty() {
		if (lf != null && lf.isSet()) return false;
		return	(q.isEmpty());
	}
/**
Get the next element in the queue, removing it.
@return A Future for the next element in the queue. If the
queue is not empty, the first element is removed and returned
in the Future. If the queue is empty, the value will be assigned
to the Future during a later put().
*/
	public synchronized Future get() {
		Object obj;
		Future f;
		if (lf!=null) {
			f=lf;
			lf=null;
			return f;
		}
		if (!q.isEmpty()) {
			obj = q.get();
			lf=null;
			return ff.make(obj);
		}
		f=ff.make();
		qf.put(f);
		return f;
	}
/**
Look at the next element in the queue. Don't remove it.
@return A Future for the next element in the queue. If the
queue is not empty, the first element is returned
in the Future. If the queue is empty, the value will be assigned
to the Future during a later put().
*/
	public synchronized Future look() {
		Object obj;
		if (lf!=null) return lf;
		lf = ff.make();
		if (!q.isEmpty()) {
			obj=q.get();
			lf.setValue(obj);
		} else {
			qf.put(lf);
		}
		return lf;
	}
/**
Run the Runnable object r as soon as the queue is not empty.
@param r The object to run.
*/
	public void runDelayed(Runnable r){
		look().runDelayed(r);
	}
/**
Get the next element in the queue, removing it. Return null if
the queue is empty. Cannot distinguish between an empty queue
and a null value in the queue.
@return The next element in the queue. If the
queue is not empty, remove and return the first element.
If the queue is empty, return null.
*/
	public synchronized Object getSkip() {
		Object obj=null;
		if (lf !=null && lf.isSet()) {
			try { obj=lf.getValue();
			} catch(InterruptedException ex){}
			lf=null;
			return obj;
		}
		if (q.isEmpty()) return null;
		obj = q.get();
		lf = null;
		return obj;
	}
/**
Look at the next element in the queue. Don't remove it.
Cannot distinguish between an empty queue
and a null value in the queue.
@return The first element in the queue or null. If the
queue is not empty, returns a reference to the first element.
If the queue is empty, returns null.
*/
	public synchronized Object lookSkip() {
		Object obj=null;
		if (lf != null && lf.isSet()) {
			try { obj=lf.getValue();
			} catch(InterruptedException ex){}
			return obj;
		}
		if (q.isEmpty()) return null;
		obj = q.firstElement();
		return obj;
	}
 public static class Test1 {
	public static void main(String args[]) {
	    try {
		Future[] f=new Future[6];
		FutureQueue q=new FutureQueue();
		int i=0;
		System.out.println("Should each time yield: aaaabb");
		q.put("a"); q.put("b");
		f[i++]=q.look();
		f[i++]=q.look();
		f[i++]=q.look();
		f[i++]=q.get();
		f[i++]=q.look();
		f[i++]=q.get();
		for (i=0;i<f.length;i++)
			System.out.print(f[i].getValue());
		System.out.println();

		i=0;
		f[i++]=q.look();
		q.put("a");
		f[i++]=q.look();
		q.put("b");
		f[i++]=q.look();
		f[i++]=q.get();
		f[i++]=q.look();
		f[i++]=q.get();
		for (i=0;i<f.length;i++)
			System.out.print(f[i].getValue());
		System.out.println();

		i=0;
		f[i++]=q.look();
		f[i++]=q.look();
		f[i++]=q.look();
		f[i++]=q.get();
		f[i++]=q.look();
		f[i++]=q.get();
		q.put("a");
		q.put("b");
		for (i=0;i<f.length;i++)
			System.out.print(f[i].getValue());
		System.out.println();

		i=0;
		q.put("a"); q.put("b");
		f[i++]=q.look();
		f[i++]=q.look();
		f[i++]=q.look();
		f[i++]=new Future(q.getSkip());
		f[i++]=q.look();
		f[i++]=new Future(q.getSkip());
		for (i=0;i<f.length;i++)
			System.out.print(f[i].getValue());
		System.out.println();

		i=0;
		f[i++]=q.look();
		q.put("a");
		f[i++]=q.look();
		q.put("b");
		f[i++]=q.look();
		f[i++]=new Future(q.getSkip());
		f[i++]=q.look();
		f[i++]=new Future(q.getSkip());
		for (i=0;i<f.length;i++)
			System.out.print(f[i].getValue());
		System.out.println();

		i=0;
		f[i++]=q.look();
		q.put("a");
		f[i++]=new Future(q.lookSkip());
		q.put("b");
		f[i++]=new Future(q.lookSkip());
		f[i++]=new Future(q.getSkip());
		f[i++]=new Future(q.lookSkip());
		f[i++]=new Future(q.getSkip());
		for (i=0;i<f.length;i++)
			System.err.print(f[i].isSet()?f[i].getValue():".");
		System.out.println();
	    } catch(Exception e){e.printStackTrace();}
	}
 }
}
