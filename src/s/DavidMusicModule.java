package s;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/** Concern separated from David Martel's MIDI & MUS player
 *  for Mocha Doom
 *  
 * @author David Martel
 * @author velktron
 *
 */

public class DavidMusicModule implements IMusic {
	
	public static final int CHANGE_VOLUME=7;
	public static final int CHANGE_VOLUME_FINE=9;
	
	Synthesizer synthesizer;
	Sequencer sequencer;
	Receiver receiver;
	Transmitter transmitter;
	boolean songloaded;
	private int volume;
	private boolean looping;
	
	public DavidMusicModule(){

	}

	@Override
	public boolean InitMusic() {
		try {
			
			// MEGA HACK: if we don't "peg" to devices found in this list, and
			// just get the defaults, volume controls won't function properly.
			 int x=-1,y=-1;
			MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();   
		     for (int i = 0; i < info.length; i++)  {
		    	 MidiDevice mdev=MidiSystem.getMidiDevice(info[i]);
		    	 if (mdev instanceof Sequencer) x=i;
		    	 if (mdev instanceof Synthesizer) y=i;
		         System.out.println(info[i].getName()+"\t\t\t"+ mdev.isOpen()+"\t"+mdev.hashCode());
		          
		     }
		
		     System.out.printf("x %d y %d \n",x,y);
		     //--This sets the Sequencer and Synthesizer  
		     //--The indices x and y correspond to the correct entries for the  
		     //--default Sequencer and Synthesizer, as determined above  	       
		      
		    if (x!=-1)
		    	sequencer = (Sequencer) MidiSystem.getMidiDevice(info[x]);
		    else
		    	sequencer = (Sequencer) MidiSystem.getSequencer(true);
			sequencer.open();

			// Add looping controller for volume
			
			sequencer.addMetaEventListener(new MetaEventListener(){
				@Override
				public void meta(MetaMessage metamessage) {
				if ( metamessage.getType() == 47 ) { // end of stream			
					if (songloaded && looping){
					 System.out.println("Looping song");
				     //sequencer.stop();
				     sequencer.setTickPosition(0);
				     
				     sequencer.start();
				     try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				     SetMusicVolume(volume);
					}
				}
			}
			});
			
			System.err.printf("Sequencer %s %d\n",sequencer.getDeviceInfo().getName(), sequencer.hashCode());
			
		    //synthesizer = MidiSystem.getSynthesizer(); 
			if (y!=-1)
				synthesizer = (Synthesizer) MidiSystem.getMidiDevice(info[y]);
			else
				synthesizer = MidiSystem.getSynthesizer();
		
			synthesizer.open();
			System.err.printf("Synthesizer %s %d\n",synthesizer.getDeviceInfo().getName(), synthesizer.hashCode());
			
		    receiver = synthesizer.getReceiver();
		    transmitter = sequencer.getTransmitter();
		    transmitter.setReceiver(receiver);
		} catch (MidiUnavailableException e) {
			return false;
		}
		return true;
	}

	@Override
	public void ShutdownMusic() {
		sequencer.stop();
		sequencer.close();
	}

	@Override
	public void SetMusicVolume(int volume) {
		
		System.out.println("Midi volume set to "+volume);
		
		this.volume=volume;
		 // NOTE: variable 'midiVolume' is an int between 0 and 127
        if( synthesizer.getDefaultSoundbank() == null )
        {
            // HARDWARE SYNTHESIZER
            try
            {
                ShortMessage volumeMessage = new ShortMessage();
                for( int i = 0; i < synthesizer.getChannels().length; i++ )
                {
                    volumeMessage.setMessage( ShortMessage.CONTROL_CHANGE,
                        i, CHANGE_VOLUME, volume );
                    MidiSystem.getReceiver().send( volumeMessage, -1 );
                }
            }
            catch( InvalidMidiDataException imde )
            {
                System.err.println( "Invalid MIDI data." );
                return;
            }
            catch( MidiUnavailableException mue )
            {
                System.err.println( "MIDI unavailable." );
                return;
            }
        }
        else
        {
            // SOFTWARE SYNTHESIZER:
            MidiChannel[] channels = synthesizer.getChannels();
            for( int c = 0; channels != null && c < channels.length; c++ )
            {
                channels[c].controlChange( CHANGE_VOLUME, volume );
            }
        }

	}

	@Override
	public void PauseSong(int handle) {
		if (songloaded)
		sequencer.stop();
		}

	@Override
	public void ResumeSong(int handle) {		
		if (songloaded){
			System.out.println("Resuming song");
		sequencer.start();
		}

	}

	@Override
	public int RegisterSong(byte[] data) {
		try {
		
	        ByteArrayInputStream bis = new ByteArrayInputStream(data);
	        
	        QMusToMid q = new QMusToMid();
	        //OutputStream fos = new FileOutputStream("C:\\Users\\David\\Documents\\test.mid");

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        
	        q.convert(bis, baos, true, 0, 0, true, new QMusToMid.Ptr<Integer>(0));
	        	  
	        bis.close();
	        bis = new ByteArrayInputStream(baos.toByteArray());
	        
	        Sequence sequence = MidiSystem.getSequence(bis);
	        
	        sequencer.stop(); // stops current music if any
	        sequencer.setSequence(sequence); // Create a sequencer for the sequence
	        songloaded=true;
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	return -1;
	    } 
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void PlaySong(int handle, boolean looping) {
		if (songloaded){
			this.looping=looping;
       // if (looping)
       // 	sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
       // else
       // 	sequencer.setLoopCount(0);
        sequencer.start(); // Start playing
        
        // HACK Nasty hack to allow volume setting to actually work.
        // Calling SetMusicVolume immediately after looping or starting didn't
        // work as intended.
        try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.SetMusicVolume(volume);
		}
	}

	@Override
	public void StopSong(int handle) {
		sequencer.stop();

	}

	@Override
	public void UnRegisterSong(int handle) {
		// In theory, we should ask the sequencer to "forget" about the song.
		// However since we can register another without unregistering the first,
		// this is practically a dummy.
		
		songloaded=false;

	}

}
