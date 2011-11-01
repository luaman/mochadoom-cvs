package utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;

import p.Resettable;

/**
 * Some utilities that emulate C stlib methods or provide convenient functions
 * to do repetitive system and memory related stuff.
 * 
 * @author Maes
 */

public class C2JUtils {

    static public char[] strcpy(char[] s1, final char[] s2) {
        for (int i = 0; i < Math.min(s1.length, s2.length); i++) {
            s1[i] = s2[i];
        }
        return s1;
    }

    static public char[] strcpy(char[] s1, final char[] s2, int off, int len) {
        for (int i = 0; i < len; i++) {
            s1[i] = s2[i + off];
        }
        return s1;
    }

    static public char[] strcpy(char[] s1, final char[] s2, int off) {
        for (int i = 0; i < Math.min(s1.length, s2.length - off); i++) {
            s1[i] = s2[i + off];
        }
        return s1;
    }

    static public char[] strcpy(char[] s1, String s2) {
        for (int i = 0; i < Math.min(s1.length, s2.length()); i++) {
            s1[i] = s2.charAt(i);
        }
        return s1;
    }

    public static byte[] toByteArray(String str) {
        byte[] retour = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            retour[i] = (byte) (str.charAt(i) & 0xFF);
        }
        return retour;
    }

    /**
     * Finds index of first element of array matching key. Useful whenever an
     * "indexOf" property is required or you encounter the C-ism [pointer-
     * array_base] used to find array indices in O(1) time. However, since this
     * is just a dumb unsorted search, running time is O(n), so use this method
     * only sparingly and in scenarios where it won't occur very frequently
     * -once per level is probably OK-, but watch out for nested loops, and
     * cache the result whenever possible. Consider adding an index or ID type
     * of field to the searched type if you require to use this property too
     * often.
     * 
     * @param array
     * @param key
     * @return
     */

    public static int indexOf(Object[] array, Object key) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == key) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Emulates C-style "string comparison". "Strings" are considered
     * null-terminated, and comparison is performed only up to the smaller of
     * the two.
     * 
     * @param s1
     * @param s2
     * @return
     */

    static public boolean strcmp(char[] s1, final char[] s2) {
        boolean match = true;
        for (int i = 0; i < Math.min(s1.length, s2.length); i++) {
            if (s1[i] != s2[i]) {
                match = false;
                break;
            }
        }
        return match;
    }

    static public boolean strcmp(char[] s1, String s2) {
        return strcmp(s1, s2.toCharArray());
    }

    /**
     * C-like string length (null termination).
     * 
     * @param s1
     * @return
     */
    static public int strlen(char[] s1) {
        if (s1 == null)
            return 0;
        int len = 0;

        while (s1[len++] > 0) {
            if (len >= s1.length)
                break;
        }

        return len - 1;
    }

    /**
     * C-like string length (null termination).
     * 
     * @param s1
     * @return
     */
    static public String nullTerminatedString(char[] s) {
        if (s == null)
            return "";
        int len = 0;

        while (s[len++] > 0) {
            if (len >= s.length)
                break;
        }

        return new String(s, 0, len - 1);
    }

    /**
     * Automatically "initializes" arrays of objects with their default
     * constuctor. It's better than doing it by hand, IMO. If you have a better
     * way, be my guest.
     * 
     * @param os
     * @param c
     * @throws Exception
     * @throws
     */

    public static final void initArrayOfObjects(Object[] os, Class c) {
        try {
            for (int i = 0; i < os.length; i++) {
                os[i] = c.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failure to allocate " + os.length
                    + " objects of class" + c.getName() + "!");
            System.exit(-1);
        }
    }

    /**
     * Automatically "initializes" arrays of objects with their default
     * constuctor. It's better than doing it by hand, IMO. If you have a better
     * way, be my guest.
     * 
     * @param os
     * @throws Exception
     * @throws
     */

    public static final void initArrayOfObjects(Object[] os) {
        Class c = os.getClass().getComponentType();
        try {
            for (int i = 0; i < os.length; i++) {
                os[i] = c.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failure to allocate " + os.length
                    + " objects of class " + c.getName() + "!");

            System.exit(-1);
        }
    }

    /**
     * Uses reflection to automatically create and initialize an array of
     * objects of the specified class. Does not require casting on "reception".
     * 
     * @param <T>
     * @param c
     * @param num
     * @return
     * @return
     */

    @SuppressWarnings("unchecked")
    public static <T> T[] createArrayOfObjects(Class<T> c, int num) {
        T[] os = null;

        try {
            os = (T[]) Array.newInstance(c, num);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failure to allocate " + num
                    + " objects of class " + c.getName() + "!");
            System.exit(-1);
        }

        try {
            for (int i = 0; i < os.length; i++) {
                os[i] = (T) c.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failure to instantiate " + os.length
                    + " objects of class " + c.getName() + "!");
            System.exit(-1);
        }

        return os;
    }
    
    /**
     * Uses reflection to automatically create and initialize an array of
     * objects of the specified class. Does not require casting on "reception".
     * Requires an instance of the desired class. This allows getting around
     * determining the runtime type of parametrized types.
     * 
     * 
     * @param <T>
     * @param instance An instance of a particular class. 
     * @param num
     * @return
     * @return
     */

    @SuppressWarnings("unchecked")
    public static <T> T[] createArrayOfObjects(T instance, int num) {
        T[] os = null;
        
        Class<T> c=(Class<T>) instance.getClass();

        try {
            os = (T[]) Array.newInstance(c, num);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failure to allocate " + num
                    + " objects of class " + c.getName() + "!");
            System.exit(-1);
        }

        try {
            for (int i = 0; i < os.length; i++) {
                os[i] = (T) c.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failure to instantiate " + os.length
                    + " objects of class " + c.getName() + "!");
            System.exit(-1);
        }

        return os;
    }


    /**
     * Automatically "initializes" arrays of objects with their default
     * constuctor. It's better than doing it by hand, IMO. If you have a better
     * way, be my guest.
     * 
     * @param os
     * @throws Exception
     * @throws
     */

    public static final void initArrayOfObjects(Object[] os, int startpos,
            int endpos) {
        Class c = os.getClass().getComponentType();
        try {
            for (int i = startpos; i < endpos; i++) {
                os[i] = c.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failure to allocate " + os.length
                    + " objects of class " + c.getName() + "!");

            System.exit(-1);
        }
    }

    /** This method gets eventually inlined, becoming very fast */

    public static final int toUnsignedByte(byte b) {
        return (0x000000FF & b);
    }

    // Optimized array-fill methods designed to operate like C's memset.

    public static void memset(boolean[] array, boolean value, int len) {
        if (len > 0)
            array[0] = value;
        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, ((len - i) < i) ? (len - i)
                    : i);
        }
    }

    public static void memset(char[] array, char value, int len) {
        if (len > 0)
            array[0] = value;
        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, ((len - i) < i) ? (len - i)
                    : i);
        }
    }

    public static void memset(int[] array, int value, int len) {
        if (len > 0)
            array[0] = value;
        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, ((len - i) < i) ? (len - i)
                    : i);
        }
    }

    public static final long unsigned(int num) {
        return 0xFFFFFFFFL & num;
    }

    public static final char unsigned(short num) {
        return (char) num;
    }

    /**
     * Convenient alias for System.arraycopy(src, 0, dest, 0, length);
     * 
     * @param dest
     * @param src
     * @param length
     */
    public static final void memcpy(Object dest, Object src, int length) {
        System.arraycopy(src, 0, dest, 0, length);
    }

    public static final boolean testAccess(String filename, String mode) {

        // This is bullshit.
        if (filename == null)
            return false;
        if (filename.length() == 0)
            return false;

        boolean access = true;
        try {
            RandomAccessFile test = new RandomAccessFile(filename, mode);
        } catch (Exception e) {
            // Something went wrong. In any case, access isn't guaranteed.
            access = false;
        }
        return access;

    }

    /**
     * Returns true if flags are included in arg. Synonymous with (flags &
     * arg)!=0
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean flags(int flags, int arg) {
        return ((flags & arg) != 0);
    }

    /**
     * Returns 1 for true and 0 for false. Useful, given the amount of
     * "arithmetic" logical functions in legacy code. Synonymous with
     * (expr?1:0);
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final int eval(boolean expr) {
        return (expr ? 1 : 0);
    }

    /**
     * Returns 1 for non-null and 0 for null objects. Useful, given the amount
     * of "existential" logical functions in legacy code. Synonymous with
     * (expr!=null);
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean eval(Object expr) {
        return (expr != null);
    }

    /**
     * Returns true for expr!=0, false otherwise.
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean eval(int expr) {
        return expr != 0;
    }

    /**
     * Returns true for expr!=0, false otherwise.
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean eval(long expr) {
        return expr != 0;
    }

    public static final void resetAll(Resettable[] r) {
        for (int i = 0; i < r.length; i++) {
            r[i].reset();
        }
    }

    /**
     * Useful for unquoting strings, since StringTokenizer won't do it for us.
     * Returns null upon any failure.
     * 
     * @param s
     * @param c
     * @return
     */

    public static String unquote(String s, char c) {

        int firstq = s.indexOf(c);
        int lastq = s.lastIndexOf(c);
        // Indexes valid?
        if (firstq != -1 && lastq != -1) {
            if (firstq < lastq) {
                // Unquote string.
                return s.substring(firstq + 1, lastq);
            }
        }
        return null;
    }

    public static String unquoteIfQuoted(String s, char c) {

        String tmp = unquote(s, c);
        if (tmp != null)
            return tmp;
        return s;
    }

    /** Return either 0 or a hashcode */
    public static final int pointer(Object o) {
        if (o == null)
            return 0;
        else
            return o.hashCode();
    }

 
    public static boolean checkForExtension(String filename, String ext) {
        
        // Null filenames satisfy null extensions.
        if ((filename == null || filename.isEmpty())
                && (ext == null || ext.isEmpty()))
            return true;
        
        String separator = System.getProperty("file.separator");

        // Remove the path upto the filename.
        int lastSeparatorIndex = filename.lastIndexOf(separator);
        if (lastSeparatorIndex != -1) {
            filename = filename.substring(lastSeparatorIndex + 1);
        }

        String realext = null;

        // Get extension separator. It SHOULD be . on all platforms, right?
        int pos = filename.lastIndexOf('.');

        if (pos >= 0 && pos <= filename.length() - 2) { // Extension present
            realext = filename.substring(pos + 1);
            return realext.compareToIgnoreCase(ext) == 0;
        } else // No extension, and null/empty comparator
        if (ext == null || ext.isEmpty())
            return true;

        // No extension, and non-null/nonempty comparator.
        return false;
    }    
    
    public static String removeExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }

    
    /**
     * This method is supposed to return the "name" part of a filename. It was
     * intended to return length-limited (max 8 chars) strings to use as lump
     * indicators. There's normally no need to enforce this behavior, as there's
     * nothing preventing the engine from INTERNALLY using lump names with >8
     * chars. However, just to be sure...
     * 
     * @param path
     * @param limit  Set to any value >0 to enforce a length limit
     * @param whole keep extension if set to true
     * @return
     */

    public static String extractFileBase(String path, int limit, boolean whole) {
    	
    	if (path==null) return path;
    	
        int src = path.length() - 1;

        String separator = System.getProperty("file.separator");
        src = path.lastIndexOf(separator)+1;

        if (src < 0) // No separator
            src = 0;

        int len = path.lastIndexOf('.');
        if (whole || len<0 ) len=path.length()-src; // No extension.
        else  len-= src;        

        // copy UP to the specific number of characters, or all        
        if (limit > 0) len = Math.min(limit, len);
        
        return path.substring(src, src + len);
    }

    /** Maes: File intead of "inthandle" */

    public static long filelength(File handle) {
        try {
            return handle.length();
        } catch (Exception e) {
            System.err.println("Error fstating");
            return -1;
        }

    }

}
