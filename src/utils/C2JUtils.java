package utils;

import p.Resettable;
import w.DoomFile;

/** Some utilities that emulate C stlib methods or provide
 *  convenient functions to do repetitive system and 
 *  memory related stuff.
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
    
    static public char[] strcpy(char[] s1, final char[] s2,int off, int len) {
        for (int i=0;i<len;i++){
            s1[i]=s2[i+off];
        }
        return s1;
    }
    
    static public char[] strcpy(char[] s1, final char[] s2,int off) {
        for (int i=0;i<Math.min(s1.length,s2.length-off);i++){
            s1[i]=s2[i+off];
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
    
    /** C-like string length (null termination).
     * 
     * @param s1
     * @return 
     */
    static public int strlen(char[] s1) {
    	if (s1==null) return 0;
    	int len=0;
    	
    	while(s1[len++]>0){
    		if (len>=s1.length) break;
            }
    	
    	return len-1;
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

    public static final void initArrayOfObjects(Object[] os, Class c) {
        try {
        for (int i=0;i<os.length;i++){
            os[i]=c.newInstance();
        }
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Failure to allocate "+os.length+" objects of class" +c.getName()+ "!");
            System.exit(-1);
        }
    }
    /** Automatically "initializes" arrays of objects with their
     * default constuctor. It's better than doing it by hand, IMO.
     * If you have a better way, be my guest.
     * 
     * @param os
     * @throws Exception 
     * @throws
     */  

    public static final void initArrayOfObjects(Object[] os) {
        Class c=os.getClass().getComponentType();
        try {
        for (int i=0;i<os.length;i++){
            os[i]=c.newInstance();
        }
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Failure to allocate "+os.length+" objects of class " +c.getName()+ "!");
            
            System.exit(-1);
        }
    }
    
    /** Automatically "initializes" arrays of objects with their
     * default constuctor. It's better than doing it by hand, IMO.
     * If you have a better way, be my guest.
     * 
     * @param os
     * @throws Exception 
     * @throws
     */  

    public static final void initArrayOfObjects(Object[] os,int startpos, int endpos) {
        Class c=os.getClass().getComponentType();
        try {
        for (int i=startpos;i<endpos;i++){
            os[i]=c.newInstance();
        }
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Failure to allocate "+os.length+" objects of class " +c.getName()+ "!");
            
            System.exit(-1);
        }
    }
    
    
    /** This method gets eventually inlined, becoming very fast */
    
    public static final int toUnsignedByte(byte b){
        return (0x000000FF&b);
    }

    public static final void memset(char[] string, char val, int len) {
        for (int i=0;i<len;i++){
            string[i]=val;
        }        
    }


    /** Convenient alias for System.arraycopy(src, 0, dest, 0, length); 
     * 
     * @param dest
     * @param src
     * @param length
     */
    public static final void memcpy(Object dest, Object src, int length ) {
       System.arraycopy(src, 0, dest, 0, length);
    }

    public static final boolean testAccess(String filename, String mode) {
        
        // This is bullshit.
        if (filename==null) return false;
        if (filename.length()==0) return false;
        
        boolean access=true;
        try {
        DoomFile test=new DoomFile(filename,mode);
        } catch (Exception e){
            // Something went wrong. In any case, access isn't guaranteed.
            access=false;
        }
        return access;
        
    }

    /** Returns true if flags are included in arg.
     * Synonymous with (flags & arg)!=0
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean flags(int flags, int arg){
        return ((flags & arg)!=0);
    }
    
    /** Returns 1 for true and 0 for false. Useful, given 
     * the amount of "arithmetic" logical functions in 
     * legacy code.
     * 
     *  Synonymous with (expr?1:0);
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final int eval(boolean expr){
        return (expr?1:0);
    }
    
    /** Returns 1 for non-null and 0 for null objects.
     * Useful, given the amount of "existential" logical 
     * functions in legacy code.
     * 
     *  Synonymous with (expr!=null);
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean eval(Object expr){
        return (expr!=null);
    }
    
    /** Returns true for expr!=0, false otherwise.
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean eval(int expr){
        return expr!=0;
    }
    
    /** Returns true for expr!=0, false otherwise.
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean eval(long expr){
        return expr!=0;
    }
      
    public static final void resetAll(Resettable[] r){
        for (int i=0;i<r.length;i++){
            r[i].reset();
        }
    }

    /** Useful for unquoting strings, since StringTokenizer won't do it for us.
     *  Returns null upon any failure.
     *  
     * @param s
     * @param c
     * @return
     */
    
    public static String unquote(String s, char c) {
        System.out.printf("Unquoting %s \n",s);
        
        int firstq=s.indexOf(c);
        int lastq=s.lastIndexOf(c);
        // Indexes valid?
        if (firstq!=-1 && lastq!=-1){
            if (firstq<lastq){
                // Unquote string.
                return s.substring(firstq+1,lastq);
            }
        }
        return null;
    }
    
}
