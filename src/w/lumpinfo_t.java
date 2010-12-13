package w;

public class lumpinfo_t {
        public String    name;
        public DoomFile     handle;
        public int     position;
        public long     size;
        // A 32-bit hash which should be enough for searching through hashtables.
        public int hash;
        // A 64-bit hash that just maps an 8-char string to a long num, good for hashing
        // or for direct comparisons.
        public long stringhash;
        // Intepreting the first 32 bits of their name as an int.
        public int intname;
        public int next;
        public int index;
        
        public int hashCode(){
            return hash;
        }
        
        public String toString(){
            return (name +" "+ Integer.toHexString(hash));
        }
        
    }
