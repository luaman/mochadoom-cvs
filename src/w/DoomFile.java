package w;

/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/

//Created on 24.07.2004 by RST.

//$Id: DoomFile.java,v 1.15 2011/05/18 16:57:51 velktron Exp $

import java.io.*;
import java.nio.ByteOrder;
import java.util.List;

import m.Swap;

/**
* An extenstion of RandomAccessFile, which handles readString/WriteString specially 
* and offers several Doom related (and cross-OS) helper functions for reading/writing
* arrays of multiple objects or fixed-length strings from/to disk.
* 
* TO DEVELOPERS: this is the preferrered method of I/O for anything implemented.
* In addition, Doomfiles can be passed to objects implementing the IReadableDoomObject 
* and IWritableDoomObject interfaces, which will "autoread" or "autowrite" themselves
* to the implied stream.
* 
* TODO: in the light of greater future portabililty and compatibility in certain
* environments, PERHAPS this should have been implemented using Streams. Perhaps
* it's possible to change the underlying implementation without (?) changing too
* much of the exposed interface, but it's not a priority for me right now. 
* 
*/
public class DoomFile extends RandomAccessFile {

   /** Standard Constructor. */
   public DoomFile(String filename, String mode) throws FileNotFoundException {
       super(filename, mode);
   }

   /** Writes a Vector to a RandomAccessFile. */
   public void writeVector(float v[]) throws IOException {
       for (int n = 0; n < 3; n++)
           writeFloat(v[n]);
   }

   /** Writes a Vector to a RandomAccessFile. */
   public float[] readVector() throws IOException {
       float res[] = { 0, 0, 0 };
       for (int n = 0; n < 3; n++)
           res[n] = readFloat();

       return res;
   }

   /** Reads a length specified string from a file. */
   public String readString() throws IOException {
       int len = readInt();

       if (len == -1)
           return null;

       if (len == 0)
           return "";

       byte bb[] = new byte[len];

       super.read(bb, 0, len);

       return new String(bb, 0, len);
   }

/** MAES: Reads a specified number of bytes from a file into a new String.
 *  With many lengths being implicit, we need to actually take the loader by the hand.
 *  
 * @param len
 * @return
 * @throws IOException
 */
   
   public String readString(int len) throws IOException {

       if (len == -1)
           return null;

       if (len == 0)
           return "";

       byte bb[] = new byte[len];

       super.read(bb, 0, len);

       return new String(bb, 0, len);
   }
   
   /** MAES: Reads a specified number of bytes from a file into a new, NULL TERMINATED String.
    *  With many lengths being implicit, we need to actually take the loader by the hand.
    *  
    * @param len
    * @return
    * @throws IOException
    */
   
      public String readNullTerminatedString(int len) throws IOException {

          if (len == -1)
              return null;

          if (len == 0)
              return "";

          byte bb[] = new byte[len];
          int terminator=len;

          super.read(bb, 0, len);
          
          for (int i=0;i<bb.length;i++){
              if (bb[i]==0) {
                  terminator=i;
                  break; // stop on first null
              }
              
          }
          return new String(bb, 0, terminator);
      }
   
   /** MAES: Reads multiple strings with a specified number of bytes from a file.
    * If the array is not large enough, only partial reads will occur.
    *  
    * @param len
    * @return
    * @throws IOException
    */
      
      public String[] readMultipleFixedLengthStrings(String[] dest, int num, int len) throws IOException {

    	  // Some sanity checks...
          if (num<=0 || len < 0)
              return null;

          if (len == 0) {
        	  for (int i=0;i<dest.length;i++){
        		  dest[i]=new String("");
        	  }
        	  return dest;
          }        	  
          
          for (int i=0;i<num;i++){
        	  dest[i]=this.readString(len);
          }
          return dest;
      }

   
   /** Writes a length specified string (Pascal style) to a file. 
    * 
    * */
   public void writeString(String s) {
       try {
       if (s == null) {
           writeInt(-1);
           return;
       }

       writeInt(s.length());
       if (s.length() != 0)
           writeBytes(s);
       } catch (Exception e){
           System.err.println("writeString "+s+" to DoomFile failed!");
       }
   }

   /** Writes a String with a specified len to a file.
    *  This is useful for fixed-size String fields in 
    *  files. Any leftover space will be filled with 0x00s. 
    * 
    * @param s
    * @param len
    * @throws IOException
    */
    
   public void writeString(String s,int len) throws IOException {

       if (s==null) return;
       
       if (s.length() != 0){
           byte[] dest=s.getBytes();
           write(dest,0,Math.min(len,dest.length));
           // Fill in with 0s if something's left.
           if (dest.length<len){
               for (int i=0;i<len-dest.length;i++){
                   write((byte)0x00);
               }
           }
       }
   }

   public void readObjectArray(IReadableDoomObject[] s,int len) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){           
           s[i].read(this);
       }
   }

   public void readObjectArrayWithReflection(IReadableDoomObject[] s,int len) throws Exception {

       if (len==0) return;
       Class c=s.getClass().getComponentType();
       
       for (int i=0;i<Math.min(len,s.length);i++){
           if (s[i]==null) s[i]=(IReadableDoomObject) c.newInstance();
           s[i].read(this);
       }
   }
   
   public void readObjectArray(IReadableDoomObject[] s,int len, Class c) throws Exception {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){
           if (s[i]==null) {
               s[i]=(IReadableDoomObject) c.newInstance();
           }
           s[i].read(this);
       }
   }
   
   public final void readIntArray(int[] s,int len, ByteOrder bo) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){           
           s[i]=this.readInt();
           if (bo==ByteOrder.LITTLE_ENDIAN){
               s[i]=Swap.LONG(s[i]);
           }
       }
   }
   
   public final void readShortArray(short[] s,int len, ByteOrder bo) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){           
           s[i]=this.readShort();
           if (bo==ByteOrder.LITTLE_ENDIAN){
               s[i]=Swap.SHORT(s[i]);
           }
       }
   }
   
   public final void readIntArray(int[] s,ByteOrder bo) throws IOException {
       readIntArray(s,s.length,bo);
   }
   
   public final void readShortArray(short[] s,ByteOrder bo) throws IOException {
       readShortArray(s,s.length,bo);
   }
   
   public void readBooleanArray(boolean[] s,int len) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){
           s[i]=this.readBoolean();
           }
   }
   
   
   
   /** Reads an array of "int booleans" into an array or
    * proper booleans. 4 bytes per boolean are used!
    * 
    * @param s
    * @param len
    * @throws IOException
    */
   
   public void readBooleanIntArray(boolean[] s,int len) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){
           s[i]=this.readIntBoolean();
           }
   }
   
   public void readBooleanIntArray(boolean[] s) throws IOException {
       readBooleanIntArray(s,s.length);
   }
   
   public void writeBoolean(boolean[] s,int len) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){
           this.writeBoolean(s[i]);
           }
   }
   
   public void writeObjectArray(IWritableDoomObject[] s,int len) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.length);i++){           
           s[i].write(this);
       }
   }
   
   public void writeListOfObjects(List<IWritableDoomObject> s,int len) throws IOException {

       if ((s==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,s.size());i++){           
           s.get(i).write(this);
       }
   }
   
   public final void readBooleanArray(boolean[] s) throws IOException {
       readBooleanArray(s,s.length);
       }
   
   public final void readIntBooleanArray(boolean[] s) throws IOException {
       readBooleanIntArray(s,s.length);
       }
  
   public void writeCharArray(char[] charr,int len) throws IOException {

       if ((charr==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,charr.length);i++){           
           this.writeChar(charr[i]);
       }
   }
   
   /** Will read an array of proper Unicode chars.
    * 
    * @param charr
    * @param len
    * @throws IOException
    */
   
   public void readCharArray(char[] charr,int len) throws IOException {

       if ((charr==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,charr.length);i++){           
           charr[i]=this.readChar();
       }
   }
   
   /** Will read a bunch of non-unicode chars into a char array.
    *  Useful when dealing with legacy text files.
    * 
    * @param charr
    * @param len
    * @throws IOException
    */
   
   public void readNonUnicodeCharArray(char[] charr,int len) throws IOException {

       if ((charr==null)||(len==0)) return;
       
       for (int i=0;i<Math.min(len,charr.length);i++){           
           charr[i]=(char) this.readUnsignedByte();
       }
   }
   
   /** Writes an item reference. 
   public void writeItem(gitem_t item) throws IOException {
       if (item == null)
           writeInt(-1);
       else
           writeInt(item.index);
   }
*/
   /** Reads the item index and returns the game item. 
   public gitem_t readItem() throws IOException {
       int ndx = readInt();
       if (ndx == -1)
           return null;
       else
           return GameItemList.itemlist[ndx];
   }
 * @throws IOException 
*/
   
   public int readLEInt() throws IOException{
       int tmp=readInt();
       return Swap.LONG(tmp);
   }
   
// 2-byte number
   public static int SHORT_little_endian_TO_big_endian(int i)
   {
       return ((i>>8)&0xff)+((i << 8)&0xff00);
   }

   // 4-byte number
   public static int INT_little_endian_TO_big_endian(int i)
   {
       return((i&0xff)<<24)+((i&0xff00)<<8)+((i&0xff0000)>>8)+((i>>24)&0xff);
   }

public short readLEShort() throws IOException {
    short tmp=readShort();
    return Swap.SHORT(tmp);
}

/** Reads a "big boolean" using 4 bytes.
 * 
 * @return
 * @throws IOException
 */
public boolean readIntBoolean() throws IOException {
    return (this.readInt()!=0);

}
   
}
