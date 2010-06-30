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
    
    /** Automatically "initializes" arrays of objects. Better than doing it by hand!!!
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
    
}
