package testers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import s.QMusToMid;
import s.QMusToMid.Track;

public class QMusToMidTester extends QMusToMid {

	//_D_: used by me to test and fix bugs
	//FileInputStream fisTest;
	//int nbTest = 0;

	public QMusToMidTester() {
		super();
		/*try {
			fisTest = new FileInputStream("C:\\Users\\David\\Desktop\\qmus2mid\\test.mid");
			//fisTest.read(new byte[106]); //debut track 0
			fisTest.read(new byte[6977]); //debut track 1
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}
	public static void fwrite(byte[] bytes, int offset, int size, int count, Object file) throws IOException {
		
		//nbTest += size;
		
		if (file instanceof OutputStream) {
			//.getClass(). . isAssignableFrom(OutputStream.class))
			((OutputStream)file).write(bytes, offset, size);
			
			
			for (int i = 0; i < size; i++) {
				//System.out.println(bytes[i]);
				
				//byte b = (byte)fisTest.read();
				//if (b != bytes[offset+i]) {
					//System.out.println("Woawww byte #: "+(offset+i)+" "+b+ " VS "+bytes[offset+i]);
				//}
				
				
			}
			

		}
		if (file instanceof Writer) {
//		if (file.getClass().isAssignableFrom(Writer.class))
			char[] ch = new char[bytes.length];
			for (int i = 0; i < bytes.length; i++) {
				//System.out.println(bytes[i]);
				ch[i] = (char)toUnsigned(bytes[i]);
				
				if ((byte)ch[i] == 0x3F) {
					System.out.println("testtt");
				}
				
				//char c = (char)fisTest.read();
				//if (c != ch[i]) {
				//	System.out.println("Woawww");
				//}
				
				
			}
			
			((Writer)file).write(ch, offset, size);
		}
	}
	
	public void TWriteByte( int MIDItrack, byte byte_, Track track[] ) {
		super.TWriteByte(MIDItrack, byte_, track);
		  //_D_: this was only added by me to test and fix bugs before this worked
		/*try {
			if (MIDItrack == 1) {
			  int b;
			b = fisTest.read();
			  if ((byte)b != byte_) {
				  //System.out.println("Woawww");
			  }
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
