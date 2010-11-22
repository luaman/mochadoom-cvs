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

/** Allows multiple threads and runnables to wait for a number of tasks
to be completed before proceeding.
@author Thomas W. Christopher (Tools of Computing LLC)
@version 0.2 Beta
*/
public class Accumulator implements RunDelayed {


/**
*/

 protected Future future;

/**
*/

 public int count;
 private int initCount;

/**
*/

 protected Object data;

/**
Creates an Accumulator which will wait for n completions.
@param n total number of threads that must gather.
*/

 public Accumulator(int n) {
    this(n,null,new Future());
 }

/**
Creates an Accumulator which will wait for n completions before
placing data in Future f.
@param n total number of completions required.
@param data value to be placed in the future. It can be updated.
@param f future to be set to data when the number of completions
	have occurred.
*/

 public Accumulator(int n, Object data, Future f) {
    count=n; this.data=data; future=f;
    initCount=count;
    if (count<=0) future.setValue(data);
 }

/**
Creates an Accumulator which will wait for n completions before
placing data in Future f.
@param n total number of completions required.
@param data value to be placed in the future. It can be updated.
*/

 public Accumulator(int n, Object data) {
    this(n,data,new Future());
 }

/**
Is called by a thread or chore to signal that it's operation
on the accumulator is complete. The nth of these signals
will place the contants of the Accumulator's data field in
its future.
*/
 public synchronized void signal(){
	if (--count==0) future.setValue(data);
 }

/**
Get the data object.
@return The data object that will be placed in the future upon
the proper number of completions.
*/

 public Object getData(){
	return data;
 }

/**
Get the Future to be set upon the correct number of signals.
@return The Future.
*/

 public Future getFuture(){
	return future;
 }

/**
Set the data object.
@param val A data object that will be placed in the future upon
the proper number of completions.
*/

 public void setData(Object val){
	data = val;
 }

/**
Delay the runnable r until all elements of the group
have terminated.
@param r The runnable to be delayed.
*/

 public void runDelayed(Runnable r) {
	future.runDelayed(r);
 }

 /**
  <P>Victor Epitropou:
  <P>Resets the Accumulator so we don't have to explicitly reassign it to any object
  reusing it. This method only works for Accumulators without a return type (barriers).
  */

 public void reset(){
     future=new Future();
     count=initCount;
 }
}
