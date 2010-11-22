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

/** A DynAlloc subclass to dynamicly allocate
blocks of numbers out of a contiguous range. This is used
by shared-memory parallel threads to allocate loop
indices to use, e.g. for processing elements of an array
in parallel.
@author Thomas W. Christopher (Tools of Computing LLC)
@version 0.2
*/
public class DynAllocShare extends DynAlloc{
 int range;int nt;int min;
 int zc; int current;
 /** Create a DynAllocShare object that will allocate
 blocks of numbers in the range [0..range) for nt
 parallel threads. It will allocate no fewer than min
 numbers in a block until the last allocation.
 It tries to allocate max(remaining/nt,min) numbers to
 each thread. After nt threads have been told that there
 are no numbers left to allocate, the DynAllocShare object
 will automatically reset and start allocating blocks of
 numbers out of the full range again. This is to tell each
 of the threads that they are done with one iteration of
 a loop so they can gather at a barrier and start the next
 iteration without having to create or explicitly reset the
 DynAllocShare object.
 @param range Non-inclusive upper bound. Numbers [0,range)
 are allocated.
 @param nt The number of threads allocating ranges from this
 DynAllocShare object.
 @param min The minimum number of numbers to allocate at a
 time, except the last allocation.
 */
 public DynAllocShare(int range,int nt,int min){
	this.range=range;
	this.nt=nt;
	this.min=min;
	zc=0;
	current=0;
 }
 
 public DynAllocShare(){

	 }
/** Allocate a new range. The information on the range of values
is filled into the range parameter, r.
@param r The Range object that has the bounds of the allocated
range filled in.
@return true if the range is non-empty, false if all the
range has been allocated.
*/
 public synchronized boolean alloc(Range r){
	if (current>=range){
		zc++;
		if (zc>=nt) {
			current=0;
			zc=0;
		}
		r.start=r.end=range;
		r.num=0;
		return false;
	}
	r.start=current;
	int rem=range-current;
	int num=(rem+nt-1)/nt;//ceiling(rem/nt)
	if (num<min) num=min;
	if (num>rem) num=rem;
	current+=num;
	r.end=current;
	r.num=num;
	return true;
 }
 
 public synchronized void reset(int range,int nt,int min){
		this.range=range;
		this.nt=nt;
		this.min=min;
		zc=0;
		current=0;
	 }
}
