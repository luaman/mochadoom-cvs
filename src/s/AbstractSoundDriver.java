package s;

import static data.sounds.S_sfx;
import w.DoomBuffer;
import data.sfxinfo_t;
import data.sounds;
import doom.DoomStatus;

public abstract class AbstractSoundDriver implements ISound{

	 protected final DoomStatus DS;
	
	 protected final int numChannels;
	 
	 /** The actual lengths of all sound effects. */
	 protected final int[] lengths = new int[NUMSFX];
	 
	    // SFX id of the playing sound effect.
	    // Used to catch duplicates (like chainsaw).
	protected final int[] channelids;
	 
    /** Pitch to stepping lookup, used in ClassicSoundDriver
     *  It's actually rigged to have a -/+ 400% pitch variation!
     */
    protected final int[] steptable = new int[256];
	
	
    /** Volume lookups. 128 levels */
    protected final int[][] vol_lookup = new int[128][256];
    
    
    // protected final static DataLine.Info info = new DataLine.Info(Clip.class, format);

	
	 public AbstractSoundDriver(DoomStatus DS,int numChannels){
		 this.DS=DS;
		 this.numChannels=numChannels;
		 channelids = new int[numChannels];
	 }
	
	protected byte[] getsfx(String sfxname, int[] len, int index) {
        byte[] sfx;
        byte[] paddedsfx;
        int i;
        int size;
        int paddedsize;
        String name;
        int sfxlump;

        // Get the sound data from the WAD, allocate lump
        // in zone memory.
        name = String.format("ds%s", sfxname).toUpperCase();

        // Now, there is a severe problem with the
        // sound handling, in it is not (yet/anymore)
        // gamemode aware. That means, sounds from
        // DOOM II will be requested even with DOOM
        // shareware.
        // The sound list is wired into sounds.c,
        // which sets the external variable.
        // I do not do runtime patches to that
        // variable. Instead, we will use a
        // default sound for replacement.
        if (DS.W.CheckNumForName(name) == -1)
            sfxlump = DS.W.GetNumForName("dspistol");
        else
            sfxlump = DS.W.GetNumForName(name);

        size = DS.W.LumpLength(sfxlump);

        sfx = DS.W.CacheLumpNumAsRawBytes(sfxlump, 0);

        // MAES: A-ha! So that's how they do it.
        // SOund effects are padded to the highest multiple integer of
        // the mixing buffer's size (with silence)

        paddedsize =
            ((size - 8 + (SAMPLECOUNT - 1)) / SAMPLECOUNT) * SAMPLECOUNT;

        // Allocate from zone memory.
        paddedsfx = new byte[paddedsize];

        // Now copy and pad. The first 8 bytes are header info, so we discard
        // them.
        System.arraycopy(sfx, 8, paddedsfx, 0, size - 8);

        for (i = size - 8; i < paddedsize; i++)
            paddedsfx[i] = (byte) 127;

        // Hmm....silence?
        for (i = size - 8; i < paddedsize; i++)
            paddedsfx[i] = (byte) 127;

        // Remove the cached lump.
        DS.Z.Free(DS.W.CacheLumpNum(sfxlump, 0, DoomBuffer.class));

        System.out.printf("SFX %d size %d padded to %d\n", index, size,
            paddedsize);
        // Preserve padded length.
        len[index] = paddedsize;

        // Return allocated padded data.
        // So the first 8 bytes are useless?
        return paddedsfx;
    }
	
	/** Modified getsfx, which transforms samples into 16-bit, signed, 
	 * stereo beforehand, before being "fed" to the audio clips.
	 * 
	 * @param sfxname
	 * @param index
	 * @return
	 */
	protected final byte[] getsfx16(String sfxname,int[] len, int index) {
		        byte[] sfx;
		        byte[] paddedsfx;
		        int i;
		        int size;
		        int paddedsize;
		        String name;
		        int sfxlump;

		        // Get the sound data from the WAD, allocate lump
		        // in zone memory.
		        name = String.format("ds%s", sfxname).toUpperCase();

		        // Now, there is a severe problem with the
		        // sound handling, in it is not (yet/anymore)
		        // gamemode aware. That means, sounds from
		        // DOOM II will be requested even with DOOM
		        // shareware.
		        // The sound list is wired into sounds.c,
		        // which sets the external variable.
		        // I do not do runtime patches to that
		        // variable. Instead, we will use a
		        // default sound for replacement.
		        if (DS.W.CheckNumForName(name) == -1)
		            sfxlump = DS.W.GetNumForName("dspistol");
		        else
		            sfxlump = DS.W.GetNumForName(name);

		        size = DS.W.LumpLength(sfxlump);

		        sfx = DS.W.CacheLumpNumAsRawBytes(sfxlump, 0);

		        // Size blown up to accommodate two channels and 16 bits.
		        // Sampling rate stays the same.
		        
		        paddedsize = (size-8)*2*2;
		        // Allocate from zone memory.
		        paddedsfx = new byte[paddedsize];

		        // Skip first 8 bytes (header), blow up the data
		        // to stereo, BIG ENDIAN, SIGNED, 16 bit. Don't expect any fancy DSP here!

		        int sample=0;
		        for (i = 8; i < size; i++){
		        	// final short sam=(short) vol_lookup[127][0xFF&sfx[i]];
		        	final short sam=(short) ((0xFF&sfx[i]-128)<<8);
		            paddedsfx[sample++] = (byte) (0xFF&(sam>>8));
		            paddedsfx[sample++]=(byte) (0xFF&sam);
		            paddedsfx[sample++]=(byte) (0xFF&(sam>>8));
		            paddedsfx[sample++]=(byte) (0xFF&sam);
		        }
		        
		        // Remove the cached lump.
		        DS.Z.Free(DS.W.CacheLumpNum(sfxlump, 0, DoomBuffer.class));

		        // Preserve padded length.
		        len[index] = paddedsize;
		        
		        // Return allocated padded data.
		        // So the first 8 bytes are useless?
		        return paddedsfx;
		    }
	
	
	/**
	 *  Starting a sound means adding it to the current list of active sounds
     * in the internal channels. As the SFX info struct contains e.g. a pointer 
     * to the raw data it is ignored.
     * 
     *  As our sound handling does not handle priority, it is ignored.
     *  Pitching (that is, increased speed of playback) is set, but whether it's 
     *  used or not depends on the final implementation (e.g. classic mixer uses
     *  it, but AudioLine-based implementations are not guaranteed.
     */
	
    @Override
    public int StartSound(int id, int vol, int sep, int pitch, int priority) {

        if (id < 1 || id > S_sfx.length - 1)
            return BUSY_HANDLE;

        // Find a free channel and get a timestamp/handle for the new sound.
        int handle = this.addsfx(id, vol, steptable[pitch], sep);

        return handle;
    }
    
    /**
     * This function adds a sound to the  list of currently active sounds,
     * which is maintained as a given number (eight, usually) of internal channels.
     * Returns a handle.
     * 
     * @param sfxid
     * @param volume
     * @param step
     * @param seperation
     * @return
     */
    
    protected abstract int addsfx(int sfxid, int volume, int step, int seperation);
	
    protected short handlenums = 0;
    
    
    //
    // Retrieve the raw data lump index
    // for a given SFX name.
    //
    public final int GetSfxLumpNum(sfxinfo_t sfx) {
        String namebuf;
        namebuf = String.format("ds%s", sfx.name).toUpperCase();
		if (namebuf.equals("DSNONE"))
			return -1;
        
		int lump;
		try {
		lump = DS.W.GetNumForName(namebuf);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		return lump;
    }
    
    
    /** This is only the common part of InitSound that caches sound data
     *  in 16-bit, stereo format (used by Audiolines).
     *  INTO sfxenum_t.
     * 
     */

	protected final void initSound8() {
		int i;

        // Initialize external data (all sounds) at start, keep static.

        for (i = 1; i < NUMSFX; i++) {
            // Alias? Example is the chaingun sound linked to pistol.
            if (sounds.S_sfx[i].link == null) {
                // Load data from WAD file.
                S_sfx[i].data = getsfx(S_sfx[i].name,lengths, i);
            } else {
                // Previously loaded already?
                S_sfx[i].data = S_sfx[i].link.data;
            }
        }
    }
    
    /** This is only the common part of InitSound that caches sound data
     *  in 16-bit, stereo format (used by Audiolines).
     *  INTO sfxenum_t.
     * 
     */

	protected final void initSound16() {
		int i;

        // Initialize external data (all sounds) at start, keep static.

        for (i = 1; i < NUMSFX; i++) {
            // Alias? Example is the chaingun sound linked to pistol.
            if (sounds.S_sfx[i].link == null) {
                // Load data from WAD file.
                S_sfx[i].data = getsfx16(S_sfx[i].name,lengths, i);
            } else {
                // Previously loaded already?
                S_sfx[i].data = S_sfx[i].link.data;
            }
        }
    }
	
	

}
