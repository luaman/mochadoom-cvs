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
A table of Futures that are automatically created on look-up (get).
@author Thomas W. Christopher (Tools of Computing LLC)
@version 0.2 Beta
*/

public class FutureTable {

/**
The FutureFactory to generate the futures.
*/
 protected FutureFactory ff;

/**
The table.
*/
 protected Hashtable tbl=new Hashtable();

/**
Default constructor. Use the default FutureFactory,
which uses Future's class run queue.
*/
 public FutureTable(){ff=new FutureFactory();}

/**
Constructor taking an explicit future factory.
@param f The future factory to use when creating futures.
*/
 public FutureTable(FutureFactory f){ff=f;}

/**
Constructor taking an explicit run queue.
@param f The run queue to use in the created futures.
*/
 public FutureTable(RunQueue rq){ff=new FutureFactory(rq);}


/**
Returns the Future associated with the key in this table.
Creates one if none existed previously.
@return The Future associated with the key.
*/
 public synchronized Future get(Object key){
	Future f=(Future)tbl.get(key);
	if (f==null) tbl.put(key,f=new Future());
	return f;
 }
/**
Removes any Future associated with key in the table.
Not that easy to use safely.
*/
 public synchronized void remove(Object key){
	tbl.remove(key);
 }

}
