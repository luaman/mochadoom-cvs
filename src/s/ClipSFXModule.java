package s;

import static data.sounds.S_sfx;

import java.util.Collection;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.FloatControl.Type;

import w.DoomBuffer;
import data.sfxinfo_t;
import data.sounds;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** Experimental Clip based driver. Very rough around the edges.
 * WIP. Full of TODOs.
 * 
 * Namely: needs an efficient way of reusing multiple instances of the 
 * same clip, a way to efficiently access and set individual clip controls
 * on-the-fly, and a lot of other stuff. Dunno if, in the end, it's anymore
 * efficient than using a custom mixer. It sure is a lot less flexible.
 * 
 * KNOWN ISSUES:
 * 
 * a) Sounds are forcibly blown to be stereo, 16-bit otherwise
 *    I don't think we even get panning controls.
 * b) Sounds need to be converted to AudioInputStreams in order to pass
 *    them to clips.
 * c) While sounds can be cached within a HashMap, it's pointless
 *    to do the same with Clips, because if an instance of a clip
 *    is already playing, it can't be played "in parallel". So we need
 *    to keep a pool of same-data clips.
 * 
 * @author Velktron
 *
 */

public class ClipSFXModule implements ISound{

	protected final static boolean D=false;
	
	HashMap<Integer,Clip> cachedSounds = new HashMap<Integer,Clip>();
	int[] channelhandles;
	int[] channelids;
	int[] channelstart;
	
	// Either it's null (no clip is playing) or non-null (some clip is playing).
	Clip[] channels;
	
    /** Volume lookups. 128 levels */
    protected final int[][] vol_lookup = new int[128][256];
	
	public final float[] linear2db;

	private final int numChannels;
	private final DoomStatus DS;
	
	public ClipSFXModule(DoomStatus DS, int numChannels) {
		this.DS=DS;
		linear2db=computeLinear2DB();
		this.numChannels=numChannels;
		
		}
	
    private float[] computeLinear2DB() {
    	
    	// Maximum volume is 0 db, minimum is ... -96 db.
    	// We rig this so that half-scale actually gives quarter power,
    	// and so is -6 dB.
    	float[] tmp=new float[VOLUME_STEPS];
    	
    	for (int i=0;i<VOLUME_STEPS;i++){
    		float linear=(float)(20*Math.log10((float)i/(float)VOLUME_STEPS));
    		// Hack. The minimum allowed value as of now is -80 db.
    		if (linear<-36.0) linear=-36.0f;
    		tmp[i]= linear;
    		
    	}
    		
    		
    		
		return tmp;
	}



	@Override
	public void InitSound() {
		int i;

        // Secure and configure sound device first.
        System.err.println("I_InitSound: ");

        // We don't actually do this here (will happen only when we
        // create the first audio clip).

        // Initialize external data (all sounds) at start, keep static.

        for (i = 1; i < NUMSFX; i++) {
            // Alias? Example is the chaingun sound linked to pistol.
            if (sounds.S_sfx[i].link == null) {
                // Load data from WAD file.
                S_sfx[i].data = getsfx(S_sfx[i].name, i);
            } else {
                // Previously loaded already?
                S_sfx[i].data = S_sfx[i].link.data;
            }
        }

        System.err.print(" pre-cached all sound data\n");
        // Finished initialization.
        System.err.print("I_InitSound: sound module ready\n");

    }


/** Modified getsfx. The individual length of each sfx is not of interest.
 * However, they must be transformed into 16-bit, signed, stereo samples
 * beforehand, before being "fed" to the audio clips.
 * 
 * @param sfxname
 * @param index
 * @return
 */
	 protected byte[] getsfx(String sfxname,int index) {
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

	        // Return allocated padded data.
	        // So the first 8 bytes are useless?
	        return paddedsfx;
	    }

	@Override
	public void UpdateSound() {
		// We do nothing here, since the mixing is delegated to the OS
		// Just hope that it's more efficient that our own...
		
	}

	@Override
	public void SubmitSound() {
		// Dummy. Nothing actual to do here.
		
	}

	@Override
	public void ShutdownSound() {
		 // Wait till all pending sounds are finished.
		  boolean done = false;
		  int i;
		  

		  // FIXME (below).
		  //fprintf( stderr, "I_ShutdownSound: NOT finishing pending sounds\n");
		  //fflush( stderr );
		  
		  while ( !done)
		  {
		    for( i=0 ; i<numChannels && ((channels[i]==null)||(!channels[i].isActive())) ; i++);
		    // FIXME. No proper channel output.
		    if (i==numChannels)  done=true;
		  }
		  
		  for( i=0 ; i<numChannels; i++){
			  if (channels[i]!=null)
			channels[i].close();			
		  	}
		  
		  // Free up resources taken up by cached clips.
		  Collection<Clip> clips=this.cachedSounds.values();
		  for (Clip c:clips){
			  c.close();
		  }
		  
		  // Done.
		  return;
		
	}

	@Override
	public void SetChannels(int numChannels) {
		channels= new Clip[numChannels];
		channelids=new int[numChannels];
		channelhandles=new int[numChannels];
		channelstart=new int[numChannels];
		
        // Generates volume lookup tables
        // which also turn the unsigned samples
        // into signed samples.
        for (int i = 0; i < 128; i++)
            for (int j = 0; j < 256; j++)
                vol_lookup[i][j] = (i * (j - 128) * 256) / 127;

	}

	@Override
	public int GetSfxLumpNum(sfxinfo_t sfxinfo) {
		
		String id = "DS"+sfxinfo.name.toUpperCase();
		if (id.equals("DSNONE"))
			return -1;

		
		int lump;
		try {
		lump = DS.W.GetNumForName(id);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return lump;
	}

	/**
	 *  Starting a sound means adding it
     * to the current list of active sounds
     * in the internal channels.
     *  As the SFX info struct contains
     *  e.g. a pointer to the raw data,
     *  it is ignored.
     *  As our sound handling does not handle
     *  priority, it is ignored.
     *  Pitching (that is, increased speed of playback)
     *   is set, but currently not used by mixing.
     */
	@Override
	public int StartSound(int id, int vol, int sep, int pitch, int priority) {
        if (id < 1 || id > S_sfx.length - 1)
            return BUSY_HANDLE;

        // Find a free channel and get a timestamp/handle for the new sound.
        //long a=System.nanoTime();

        int handle = this.addsfx(id, vol, sep);

        //long b=System.nanoTime();
		//System.err.printf(" obtained in %d\n",(b-a));
        return handle;
	}

	private final static AudioFormat format=new AudioFormat(Encoding.PCM_SIGNED, ISound.SAMPLERATE, 16, 2, 4, ISound.SAMPLERATE, true);
	private final static DataLine.Info info = new DataLine.Info(Clip.class, format);
	
	private final void  getClipForChannel(int c, int sfxid){
		
		// Try to see if we already have such a clip.
		Clip clip=this.cachedSounds.get(sfxid);
		
		boolean exists=false;
		
		// Does it exist?
		if (clip!=null){
			
			// Well, it does, but we are not done yet.
			exists=true;
			// Is it NOT playing already?
			if (!clip.isActive()){
				// Assign it to the channel.
				channels[c]=clip;
				return;
			}
		}
		
		// Sorry, Charlie. Gotta make a new one.
	    try {
			clip = (Clip) AudioSystem.getLine(info);
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			clip.open(format, S_sfx[sfxid].data, 0, S_sfx[sfxid].data.length);
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!exists)
		this.cachedSounds.put(sfxid,clip);
		
	    channels[c]=clip;
	    
		
	   // Control[] cs=clip.getControls();
	   // 
	   // for (Control cc:cs){
	   // 	System.out.println("Control "+cc.getType().toString());
       // 		}
	}
	
	//
	// This function adds a sound to the
	//  list of currently active sounds,
	//  which is maintained as a given number
	//  (eight, usually) of internal channels.
	// Returns a handle.
	//
	protected short	handlenums = 0;

	private int addsfx 	( int sfxid,int		volume,int		seperation)
	{
		int		i;
		int		rc = -1;

		int		oldest = DS.gametic;
		int		oldestnum = 0;
		int		slot;

		// Chainsaw troubles.
		// Play these sound effects only one at a time.
		if ( sfxid == sfxenum_t.sfx_sawup.ordinal()
				|| sfxid == sfxenum_t.sfx_sawidl.ordinal()
				|| sfxid == sfxenum_t.sfx_sawful.ordinal()
				|| sfxid == sfxenum_t.sfx_sawhit.ordinal()
				|| sfxid == sfxenum_t.sfx_stnmov.ordinal()
				|| sfxid == sfxenum_t.sfx_pistol.ordinal()	 )
		{
			// Loop all channels, check.
			for (i=0 ; i<numChannels ; i++)
			{
				// Active, and using the same SFX?
				if (channels[i]!=null && channels[i].isRunning()
						&& channelids[i] == sfxid)
				{
					// Reset.
					channels[i].stop();
					// We are sure that iff,
					//  there will only be one.
					break;
				}
			}
		}

		// Loop all channels to find oldest SFX.
		for (i=0; (i<numChannels) && (channels[i]!=null); i++)
		{
			if (channelstart[i] < oldest)
			{
				oldestnum = i;
				oldest = channelstart[i];
			}
		}

		// Tales from the cryptic.
		// If we found a channel, fine.
		// If not, we simply overwrite the first one, 0.
		// Probably only happens at startup.
		if (i == numChannels)
			slot = oldestnum;
		else
			slot = i;

		// Okay, in the less recent channel,
		//  we will handle the new SFX.
		
		// We need to decide whether we can reuse an existing clip
		// or create a new one. In any case, when this method return 
		// we should have a valid clip assigned to channel "slot".

        getClipForChannel(slot,sfxid);

        
		// Reset current handle number, limited to 0..100.
		if (handlenums==0) // was !handlenums, so it's actually 1...100?
			handlenums = MAXHANDLES;

		// Assign current handle number.
		// Preserved so sounds could be stopped (unused).
		channelhandles[slot]= rc = handlenums--;

		// Should be gametic, I presume.
		channelstart[slot] = DS.gametic;

		// Get the proper lookup table piece
		//  for this volume level???
		//channelleftvol_lookup[slot] = vol_lookup[leftvol];
		//channelrightvol_lookup[slot] = vol_lookup[rightvol];

		// Preserve sound SFX id,
		//  e.g. for avoiding duplicates of chainsaw.
		channelids[slot] = sfxid;

		setVolume(slot,volume);
		setPanning(slot,seperation);
		//channels[slot].addSound(sound, handlenums);
		//channels[slot].setPitch(pitch);
		
		System.err.println(channelStatus());
        if(D) System.err.printf("Playing %d vol %d on channel %d\n",rc,volume,slot);
		// Well...play it.
      
        // FIXME VERY BIG PROBLEM: stop() is blocking!!!! WTF ?!
        //channels[slot].stop();
        //long  a=System.nanoTime();
        channels[slot].setFramePosition(0);
		channels[slot].start();
		// b=System.nanoTime();
		//System.err.printf("Sound playback completed in %d\n",(b-a));
        
        // You tell me.
		return rc;
	}
	
	
	/** Accepts volume in "Doom" format (0-127).
	 * 
	 * @param volume
	 */
	public void setVolume(int chan,int volume){
		Clip c=channels[chan];
		
		if (c.isControlSupported(Type.MASTER_GAIN)){
			FloatControl vc=(FloatControl) c.getControl(Type.MASTER_GAIN);
				float vol = linear2db[volume];
				vc.setValue(vol);
				}
			else if (c.isControlSupported(Type.VOLUME)){
				FloatControl vc=(FloatControl) c.getControl(Type.VOLUME);
				float vol = vc.getMinimum()+(vc.getMaximum()-vc.getMinimum())*(float)volume/127f;
				vc.setValue(vol);
			}
		}
	
	public void setPanning(int chan,int sep){
		Clip c=channels[chan];
		
		if (c.isControlSupported(Type.PAN)){
			FloatControl bc=(FloatControl) c.getControl(Type.PAN);
			// Q: how does Doom's sep map to stereo panning?
			// A: Apparently it's 0-255 L-R.
			float pan= bc.getMinimum()+(bc.getMaximum()-bc.getMinimum())*(float)sep/ISound.PANNING_STEPS;
			bc.setValue(pan);
			}
		}
	
	@Override
	public void StopSound(int handle) {
		// Which channel has it?
		int  hnd=getChannelFromHandle(handle);
		if (hnd>=0) {
			channels[hnd].stop();
			channels[hnd]=null;
		}
	}

	@Override
	public boolean SoundIsPlaying(int handle) {
		
		return getChannelFromHandle(handle)!=BUSY_HANDLE;
		}

	
	@Override
	public void UpdateSoundParams(int handle, int vol, int sep, int pitch) {
		
		// This should be called on sounds that are ALREADY playing. We really need
		// to retrieve channels from their handles.
		
		//System.err.printf("Updating sound with handle %d vol %d sep %d pitch %d\n",handle,vol,sep,pitch);
		
		int i=getChannelFromHandle(handle);
		// None has it?
		if (i!=BUSY_HANDLE){
			//System.err.printf("Updating sound with handle %d in channel %d\n",handle,i);
			setVolume(i,vol);
			setPanning(i,sep);
			//channels[i].setPanning(sep);
			}
		
	}
	
	
	/** Internal use. 
	 * 
	 * @param handle
	 * @return the channel that has the handle, or -2 if none has it.
	 */
	private int getChannelFromHandle(int handle){
		// Which channel has it?
		for (int i=0;i<numChannels;i++){
			if (channelhandles[i]==handle) return i;
		}
		
		return BUSY_HANDLE;
	}

		StringBuilder sb=new StringBuilder();
	
		public String channelStatus(){
			sb.setLength(0);
			for (int i=0;i<numChannels;i++){
				if (channels[i]!=null && channels[i].isActive())
				sb.append(i);
				else sb.append('-');
			}
			
			return sb.toString();
			
			
		}
	
}

