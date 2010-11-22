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
import java.util.*;
/**
A factory to create assign-once variables that allows consumers to
wait for a value to be produced.
@author Thomas W. Christopher (Tools of Computing LLC)
@version 0.2 Beta
*/

public class FutureFactory {


/**
The queue into which to put (runDelayed) runnables that are
waiting on this Future. The limit on the number
of threads that can be running the (runDelayed) objects is the
amount of memory available. They are placed in the queue when
the Future is given a value.
*/

 protected RunQueue runQueue = null;

/**
Create a FutureFactory.
@param runQueue The run queue into which all created futures
	will deposit Runnables.
*/

 public FutureFactory(RunQueue runQueue) {
	super();
	this.runQueue = runQueue;
 }

/**
Create a FutureFactory. Use Future's default RunQueue.

*/

 public FutureFactory() {
	super();
	runQueue = Future.getClassRunQueue();
 }

/**
Get the RunQueue for a FutureFactory object.
@return The RunQueue that objects runDelayed on a Future object
created by this factory will be placed in.
*/

 public RunQueue getRunQueue(){
	return runQueue;
 }

/**
Set the RunQueue for a FutureFactory object.
*/

 public void setRunQueue(RunQueue rq){
	runQueue = rq;
 }

/**
Create a Future.
*/

 public Future make(){
	Future f=new Future();
	f.setRunQueue(runQueue);
	return f;
 }

/**
Create a Future with a value already assigned.
*/

 public Future make(Object val){
	Future f=new Future(val);
	f.setRunQueue(runQueue);
	return f;
 }
}
