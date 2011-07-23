package s;

import w.DoomBuffer;
import doom.DoomStatus;

/** A variation of the Classic Sound Driver, decoding the DP-lumps
 *  instead of the DS. A better way would be to build-in an 
 *  automatic "WAV to SPEAKER" conversion, but that can wait...
 * 
 * @author velktron
 *
 */

public class SpeakerDoomSoundDriver extends ClassicDoomSoundDriver {

    public SpeakerDoomSoundDriver(DoomStatus DS, int numChannels) {
        super(DS, numChannels);
        // TODO Auto-generated constructor stub
    }
	
    /** Rigged so it gets SPEAKER sounds instead of regular ones */
    
    protected byte[] getsfx
    ( String         sfxname,
            int[]          len, int index )
    {
        byte[]      sfx;
        byte[]      paddedsfx;
        int                 i;
        int                 size;
        int                 paddedsize;
        String                name;
        int                 sfxlump;


        float f=SpeakerSound.f[0];
        
        // Get the sound data from the WAD, allocate lump
        //  in zone memory.
        name=String.format("dp%s", sfxname).toUpperCase();

        // Now, there is a severe problem with the
        //  sound handling, in it is not (yet/anymore)
        //  gamemode aware. That means, sounds from
        //  DOOM II will be requested even with DOOM
        //  shareware.
        // The sound list is wired into sounds.c,
        //  which sets the external variable.
        // I do not do runtime patches to that
        //  variable. Instead, we will use a
        //  default sound for replacement.
        if ( DS.W.CheckNumForName(name) == -1 )
            sfxlump = DS.W.GetNumForName("dppistol");
        else
            sfxlump = DS.W.GetNumForName(name);

        // We must first load and convert it to raw samples.
        
        SpeakerSound SP=(SpeakerSound) DS.W.CacheLumpNum(sfxlump, 0,SpeakerSound.class);
        
        sfx = SP.toRawSample();
        
        size = sfx.length;

        // MAES: A-ha! So that's how they do it.
        // SOund effects are padded to the highest multiple integer of 
        // the mixing buffer's size (with silence)

        paddedsize = ((size-8 + (SAMPLECOUNT-1)) / SAMPLECOUNT) * SAMPLECOUNT;

        // Allocate from zone memory.
        paddedsfx = new byte[paddedsize];

        // Now copy and pad. The first 8 bytes are header info, so we discard them.
        System.arraycopy(sfx,8, paddedsfx, 0,size-8 );
        
        for (i=size-8 ; i<paddedsize ; i++)
            paddedsfx[i] = (byte) 127;

        
        
        // Hmm....silence?
        for (i=size-8 ; i<paddedsize ; i++)
            paddedsfx[i] = (byte) 127;

        // Remove the cached lump.
        DS.Z.Free( DS.W.CacheLumpNum (sfxlump,0,DoomBuffer.class) );

        System.out.printf("SFX %d size %d padded to %d\n",index,size,paddedsize);
        // Preserve padded length.
        len[index] = paddedsize;

        // Return allocated padded data.
        // So the first 8 bytes are useless?
        return paddedsfx;
    }

}
