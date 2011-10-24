package w;

import java.io.DataInputStream;
import java.io.InputStream;


// CPhipps - changed wad init
// We _must_ have the wadfiles[] the same as those actually loaded, so there 
// is no point having these separate entities. This belongs here.

public class wadfile_info_t {
      public String name;
      public wad_source_t src;
      public InputStream handle;
    }
