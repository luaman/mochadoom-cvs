package w;


// CPhipps - changed wad init
// We _must_ have the wadfiles[] the same as those actually loaded, so there 
// is no point having these separate entities. This belongs here.

public class wadfile_info_t {
      String name;
      wad_source_t src;
      int handle;
    }
