package w;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** As we know, Java can be a bit awkward when handling streams e.g. you can't really skip 
 *  at will without doing some nasty crud. This class helps doing such crud.
 *  
 *  E.g. if we are dealing with a stream that has an underlying file, we can try 
 *  and skip directly by using the file channel, otherwise we can try (eww) closing
 *  the stream, reopening it (ASSUMING WE KNOW THE SOURCE'S URI AND TYPE), and then
 *  skipping.
 *  
 *  
 * @author Maes
 *
 */

public class InputStreamSugar {
    
    
    // TODO
    public static enum StreamType{
        local_file, // Local file. Easiest case
        local_zip_file, // Zipped file
        network, // TODO: handling of network types. Provide caching?
        network_zip        
    }
    
    public static final InputStream createInputStreamFromURI(String URI){
        
        InputStream is = null;
        URL u;

        try { // Is it a net resource?
            u=new URL(URI);
            is=u.openStream();
        } catch (Exception e) {
            // OK, not a valid URL or no network. We don't care.
            // Try opening as a local file.
           try {
            is=new FileInputStream(URI);
        } catch (FileNotFoundException e1) {
            // Well, it's not that either.
            // At this point we run out of options
        }
           }
        
        // TODO: zip handling?
        
        return is;
    }
    
    /** Attempt to do the Holy Grail of Java Streams, aka seek
     *  to a particular position. With some types of stream, this is
     *  possible if you poke deep enough. With others, it's not, and 
     *  you can only close & reopen them (provided you know how to do that)
     *  and then skip to a particular position
     * 
     * @param is
     * @param pos The desired position
     * @peram knownpos If the size of the stream is known, it may be possible to simply skip. 0 = unknown
     * @param URI Information which can help reopen a stream, e.g. a filename, URL, or zip file.
     * @return the skipped stream. Might be a totally different object.
     * @throws IOException 
     */
    
    public static final InputStream streamSeek(InputStream is, long pos,long size, String URI) throws IOException{
        FileInputStream fis;
        
        if (is==null) return is;
        
        // If we know our actual position in the stream, we can aid seeking forward
        
            if (size>0){
                try{
                long available=is.available();
                long guesspos=size-available;
                if (guesspos>0 && guesspos<=pos){
                    is.skip(pos-guesspos);
                    return is;
                    }
                } catch (Exception e){
                    // We couldn't skip cleanly. Swallow up and try normally.
                }
            }
        
        // Cast succeeded
        if (is instanceof FileInputStream){
            try {                
                //long a=System.nanoTime();
                ((FileInputStream)is).getChannel().position(pos);
                //long b=System.nanoTime();
                //System.out.printf("Stream seeked WITHOUT closing %d\n",(b-a)/1000);
                return is;
            } catch (IOException e) {
                // Ouch. Do a dumb close & reopening.
               is.close();
               is=createInputStreamFromURI(URI);
               is.skip(pos);
               return is;
            }
        } 
          
        
        //if (is instanceof ZipInputStream){
        //    ((ZipInputStream)is).open("ah");
        //}
        
        try { // Is it a net resource? We have to reopen it :-/
            long a=System.nanoTime();
            URL u=new URL(URI);
            InputStream nis=u.openStream();
            nis.skip(pos);
            is.close();
            long b=System.nanoTime();
            System.out.printf("Network stream seeked WITH closing %d\n",(b-a)/1000);
            return  nis;
        } catch (Exception e) {
            
           }
        
        // TODO: zip handling?
        
        return is;
    }
    
    public static List<ZipEntry> getAllEntries(ZipInputStream zis) throws IOException{
        ArrayList<ZipEntry> zes=new ArrayList<ZipEntry>();
        
        ZipEntry z;
        
        while ((z=zis.getNextEntry())!=null){
            zes.add(z);
        }
        
        
        return zes;
    }

}
