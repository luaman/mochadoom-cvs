package m;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import w.DoomFile;
import w.WritableDoomObject;

public class pcx_t implements WritableDoomObject{

	//
	// SCREEN SHOTS
	//

		// char -> byte Bytes.
		byte		manufacturer;
		byte		version;
		byte		encoding;
		byte		bits_per_pixel;
		
		// unsigned short -> char
		char	xmin;
		char	ymin;
		char	xmax;
		char	ymax;
	    
		char	hres;
		char	vres;

		char[]	palette=new char[48];
	    
	    byte		reserved;
	    byte		color_planes;
	 // unsigned short -> char
	    char	bytes_per_line;
	    char	palette_type;
	    
	    byte[]		filler=new byte[58];
	    //unsigned char	data;		// unbounded
	    ByteBuffer data;
	    
		@Override
		public void write(DoomFile f) throws IOException {
			// char -> byte Bytes.
			f.writeByte(manufacturer);
			f.writeByte(version);
			f.writeByte(encoding);
			f.writeByte(bits_per_pixel);
			
			// unsigned short -> char
			f.writeChar(xmin);
			f.writeChar(ymin);
			f.writeChar(xmax);
			f.writeChar(ymax);
		    
			f.writeChar(hres);
			f.writeChar(vres);
			f.writeCharArray(palette, palette.length);
		    
		    f.writeByte(reserved);
		    f.writeByte(color_planes);
		 // unsigned short -> char
		    f.writeChar(bytes_per_line);
		    f.writeChar(palette_type);
		    
		    f.write(filler);
		    //unsigned char	data;		// unbounded
		    f.write(data.array());
		}
	} pcx_t;
