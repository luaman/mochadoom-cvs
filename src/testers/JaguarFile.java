package testers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import demo.VanillaDoomDemo;
import utils.C2JUtils;
import w.*;

public class JaguarFile {
    
    static final String URI="E://jagdoom/doom.wad";

    public static void main(String[] argv) throws IOException {

    DataInputStream dis=new DataInputStream(new FileInputStream("E://jagdoom/doom.wad"));
    DataOutputStream dos=new DataOutputStream(new FileOutputStream("E://jagdoom/jagdoom.wad"));
        
    wadheader_t header=new wadheader_t();
    header.big_endian=true;
    header.read(dis);
    header.big_endian=false;
    header.write(dos);

    System.out.println(header.type);
    System.out.println(header.numentries);
    System.out.println(header.tablepos);
    
    filelump_t[] stuff=new filelump_t[header.numentries];
    C2JUtils.initArrayOfObjects(stuff);
    
    byte[] data=new byte[header.tablepos-wadheader_t.sizeof()];
    dis.read(data);
    dos.write(data);
        
    int marker=dis.readInt();
    dos.writeInt(marker);
    
    for (filelump_t f:stuff){
        f.big_endian=true;
        f.read(dis);
        System.out.printf("%s %s %d %d %s\n",f.name,f.actualname,f.filepos,f.size,f.compressed);
        f.big_endian=false;
        f.write(dos);
    }
    
    dis.close();
    dos.close();
    
    FileOutputStream fos;
    FileInputStream fis =new FileInputStream("E://jagdoom/doom.wad");
    long size= InputStreamSugar.getSizeEstimate(fis, null);
    
    
    int k=0;
    for (filelump_t f:stuff){
        
        //if (k>2) break;
        k++;
        InputStreamSugar.streamSeek(fis, f.filepos, size, URI, null, InputStreamSugar.FILE);
        byte[] input=new byte[(int) Math.min(f.size,fis.available())];
        byte[] output;
        fis.read(input);
        if (f.compressed){
            System.out.printf("Decompressing %s expecting %d\n",f.actualname,f.size);
            output=new byte[(int) f.size];
            JadDecompress.decode(input, output);            
            fos=new FileOutputStream(
                String.format("E://jagdoom/DEC_%s.lmp",f.actualname));
            } else {
                output=input;
                fos=new FileOutputStream(
                    String.format("E://jagdoom/%s.lmp",f.name));
            }
        fos.write(output);
            }
        
    }
    
}
