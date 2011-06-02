package z;

import java.nio.ByteBuffer;

import w.CacheableDoomObject;

public class memblock_t {

        public int         size;   // including the header and possibly tiny fragments
        public CacheableDoomObject      user;   // NULL if a free block
        public int         tag;    // purgelevel
        public int         id; // should be ZONEID
        public memblock_t  next;
        public memblock_t  prev;
    }
