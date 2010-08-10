package utils;
/** Some utilities that emulate C stlib methods.
 * 
 * @author Maes
 *
 */

public class C2JUtils {

    static public char[] strcpy(char[] s1, final char[] s2) {
        for (int i=0;i<Math.min(s1.length,s2.length);i++){
            s1[i]=s2[i];
        }
        return s1;
    }
 
    static public char[] strcpy(char[] s1, String s2) {
        for (int i=0;i<Math.min(s1.length,s2.length());i++){
            s1[i]=s2.charAt(i);
        }
        return s1;
    }
    
    /** Emulates C-style "string comparison".
     *  "Strings" are considered null-terminated,
     *  and comparison is performed only up to the smaller
     *  of the two.
     * 
     * @param s1
     * @param s2
     * @return
     */
    
    static public boolean strcmp(char[] s1, final char[] s2) {
    	boolean match=true;
        for (int i=0;i<Math.min(s1.length,s2.length);i++){
            if (s1[i]!=s2[i]) {
            	match=false;
            	break;
            }
        }
        return match;
    }
 
    static public boolean strcmp(char[] s1, String s2) {
    	return strcmp(s1,s2.toCharArray());
    }
    
    /** C-like string length.
     * 
     * @param s1
     * @return 
     */
    static public int strlen(char[] s1) {
    	if (s1==null) return 0;
    	int len=0;
    	
    	while(s1[len++]>0){
    		if (len>s1.length) break;
            }
    	
    	return len;
    }
    
    /** Automatically "initializes" arrays of objects with their
     * default constuctor. It's better than doing it by hand, IMO.
     * If you have a better way, be my guest.
     * 
     * @param os
     * @param c
     * @throws Exception 
     * @throws  
     */
    public static void initArrayOfObjects(Object[] os, Class c) {
        try {
        for (int i=0;i<os.length;i++){
            os[i]=c.newInstance();
        }
        } catch (Exception e){
            System.err.println("Failure to allocate "+os.length+" objects of class" +c.getName()+ "!");
            System.exit(-1);
        }
    }
    
    public static int toUnsignedByte(byte b){
        return (0x000000FF&b);
    }
    
}
