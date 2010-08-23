package w;

public class lumpinfo_t {
        public String    name;
        public DoomFile     handle;
        public int     position;
        public long     size;
        // A 32-bit hash which should be enough for searching through hashtables.
        public long hash;
        // A 64-bit hash that can be used for definitive comparisons.
        public long stringhash;
        public int next;
        public int index;
        
        public String toString(){
            return (name +" "+ Long.toHexString(hash));
        }
        
    }
