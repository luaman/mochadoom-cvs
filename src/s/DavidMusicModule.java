package s;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
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
	
	Synthesizer synthesizer;
	Sequencer sequencer;
	Receiver receiver;
	Transmitter transmitter;
	boolean songloaded;
	
	public DavidMusicModule(){

	}

	@Override
	public void InitMusic() {
		try {
			MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();   
		     for (int i = 0; i < info.length; i++)   
		          System.out.println(info[i].getName()+"\t\t\t"/*+ mdev.isOpen()*/); 
			
			sequencer = MidiSystem.getSequencer(true);
		    sequencer.open();

		    synthesizer = MidiSystem.getSynthesizer();  
		    synthesizer.open();

		    receiver = synthesizer.getReceiver();
		    transmitter = sequencer.getTransmitter();
		    transmitter.setReceiver(receiver);
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void ShutdownMusic() {
		sequencer.stop();
		sequencer.close();
	}

	@Override
	public void SetMusicVolume(int volume) {
		MidiChannel[] channels = synthesizer.getChannels();   
	     for (int i = 0; i < channels.length; i++)   {
	          channels[i].controlChange(7, 0);
	          channels[i].controlChange(39, 0);
	     }
	     
		try {  

			ShortMessage volumeMessage = new ShortMessage();  

			for (int i = 0; i < channels.length; i++) {
				volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 7, volume);  
				receiver.send(volumeMessage, -1);
			}  

		} catch (Exception e) {  

			e.printStackTrace();  

		}

	}

	@Override
	public void PauseSong(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ResumeSong(int handle) {
		// TODO Auto-generated method stub

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
        if (looping)
        	sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        else
        	sequencer.setLoopCount(0);
        sequencer.start(); // Start playing
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
