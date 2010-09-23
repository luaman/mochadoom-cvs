package doom;

import utils.C2JUtils;

public class doomdata_t {

     // High bit is retransmit request.
     /** MAES: was "unsigned" */
     public int        checksum; 
     /** Only valid if NCMD_RETRANSMIT. */     
     public byte        retransmitfrom;
     
     public byte        starttic;
     public byte        player;
     public byte        numtics;
     public ticcmd_t[]        cmds;
     
    public doomdata_t(){
        cmds=new ticcmd_t[data.Defines.BACKUPTICS];
        C2JUtils.initArrayOfObjects(cmds);
    }

 }
