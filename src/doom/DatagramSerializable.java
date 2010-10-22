package doom;

/** Meant to provide a more lightweight alternative to Java's serialization model,
 *  specifically for the purpose of sending
 *  Objects implementing this can return references to one same byte array, with minimal
 *  overhead. Since it's for send-only purposes, it won't matter if it's  modified.
 *   *  
 *  But don't use it in lieu of CacheableDoomObject! 
 * 
 * @author admin
 *
 */

public interface DatagramSerializable {
    
        /** Packs object into a byte array suitable to send over 
         * datagram networks.
         * 
         * @return
         */
    
        public byte[] pack();
        
        /** Packs object into a byte array suitable to send over 
         * datagram networks. The array is supplied externally 
         * (good for daisy-chaining stuff)
         * 
         * @return
         */
    
        public void pack(byte[] buf, int offset);
        
        /** Deserializes an object from a given byte buffer.
         *  Only the first (sizeof) bytes will be used.
         * 
         */
        
        public void unpack(byte[] buf);
        
        /** Deserializes an object from a given byte buffer.
         *  Only the first (sizeof) bytes will be used, starting
         *  from a specified offset. 
         */
        public void unpack(byte[] buf, int offset);
        
       
}
