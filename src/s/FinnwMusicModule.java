package s;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.RuntimeErrorException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

/** A music driver that bypasses Sequences and sends events from a MUS lump
 *  directly to a MIDI device.
 *
 *  Some songs (e.g. D_E1M8) vary individual channel volumes dynamically. This
 *  driver multiplies the dynamic volume by the music volume set in the menu.
 *  This does not work well with a {@link Sequence} because changes to events
 *  (e.g. channel volume change events) do not take effect while the sequencer
 *  is running.
 *  
 *  Disadvantages of this driver:
 *  <ul><li>Supports MID & MUS lumps only (no MOD, OGG etc.)</li>
 *      <li>Creates its own thread</li>
 *      <li>Pausing is not implemented yet</li>
 *      <li>It's too complicated</li></ul>
 *
 * @deprecated The most important functions (mus decoding & volume controls)
 *             are now in separate classes.  Use this only if we need our own
 *             sequencing thread for some reason.
 *
 * @author finnw
 */
@Deprecated
public class FinnwMusicModule implements IMusic {

    public FinnwMusicModule() {
        this.lock = new ReentrantLock();
        this.midiChannels = new ArrayList<Channel>(16);
        this.musChannels = new ArrayList<Channel>(16);
        this.songs = new ArrayList<Song>(1);
        for (int midiChan = 0; midiChan < 16; ++ midiChan) {
            Channel chan = new Channel(midiChan);
            midiChannels.add(chan);
            if (midiChan != 9) {
                musChannels.add(chan);
            }
        }
        musChannels.add(new Channel(9));
    }

    @Override
    public void InitMusic() {
        try {
            receiver = getReceiver();
            EventGroup genMidiEG = new EventGroup(1f);
            genMidiEG.generalMidi(1);
            genMidiEG.sendTo(receiver);
            sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        } catch (MidiUnavailableException ex) {
            System.err.println(ex);
            receiver = null;
        }
        exec = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl());
    }

    /** Not yet implemented */
    @Override
    public void PauseSong(int handle) {
    }

    @Override
    public void PlaySong(int handle, boolean looping) {
        lock.lock();
        try {
            if (currentTransmitter != null) {
                currentTransmitter.stop();
            }
            currentTransmitter = null;
            if (0 <= handle && handle < songs.size()) {
                prepare(receiver);
                Song song = songs.get(handle);
                currentTransmitter =
                    new ScheduledTransmitter(song.newEventStream(looping));
                currentTransmitter.setReceiver(receiver);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int RegisterSong(byte[] data) {
        return RegisterSong(ByteBuffer.wrap(data));
    }

    public int RegisterSong(ByteBuffer data) {
        Song song = new NullSong();
        if (hasMusMagic(data)) {
            song = new MusSong(data);
        } else {
            try {
                final InputStream midiIS;
                if (data.hasArray()) {
                    midiIS =
                        new ByteArrayInputStream(data.array(),
                                                 data.arrayOffset() + data.position(),
                                                 data.remaining());
                } else {
                    ByteArrayOutputStream copy =
                        new ByteArrayOutputStream(data.asReadOnlyBuffer().remaining());
                    Channels.newChannel(copy).write(data.asReadOnlyBuffer());
                    midiIS = new ByteArrayInputStream(copy.toByteArray());
                }
                try {
                    song = new MidiSong(MidiSystem.getSequence(midiIS));
                } catch (InvalidMidiDataException ex) {
                    System.err.println(ex);
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
        lock.lock();
        try {
            int result = songs.indexOf(null);
            if (result >= 0) {
                songs.set(result, song);
            } else {
                result = songs.size();
                songs.add(song);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /** Not yet implemented */
    @Override
    public void ResumeSong(int handle) {
    }

    @Override
    public void SetMusicVolume(int volume) {
        float fVol = volume * (1/127f);
        fVol = Math.max(0f, Math.min(fVol, 1f));
        lock.lock();
        try {
            this.volume = fVol;
            if (currentTransmitter != null) {
                currentTransmitter.volumeChanged();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void ShutdownMusic() {
        exec.shutdown();
    }

    @Override
    public void StopSong(int handle) {
        lock.lock();
        try {
            if (currentTransmitter != null) {
                currentTransmitter.stop();
                currentTransmitter = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void UnRegisterSong(int handle) {
        lock.lock();
        try {
            if (0 <= handle && handle < songs.size()) {
                songs.set(handle, null);
            }
        } finally {
            lock.unlock();
        }
    }

    static boolean hasMusMagic(ByteBuffer magicBuf) {
        return magicBuf.get(0) == 'M' &&
               magicBuf.get(1) == 'U' &&
               magicBuf.get(2) == 'S' &&
               magicBuf.get(3) == 0x1a;
    }

    static abstract class EventStream {
        abstract EventGroup nextEventGroup();
    }

    Channel getVolumeChangeChannel(MidiMessage message) {
        assert ((ReentrantLock) lock).isHeldByCurrentThread();
        if (message.getLength() >= 3) {
            byte[] mBytes = message.getMessage();
            if ((byte) 0xb0 <= mBytes[0] && mBytes[0] < (byte) 0xc0 &&
                mBytes[1] == 7) {
                return midiChannels.get(mBytes[0] & 15);
            }
        }
        return null;
    }

    boolean isControllerReset(MidiMessage message) {
        assert ((ReentrantLock) lock).isHeldByCurrentThread();
        if (message.getLength() >= 2) {
            byte[] mBytes = message.getMessage();
            if ((byte) 0xb0 <= mBytes[0] && mBytes[0] < (byte) 0xc0 &&
                mBytes[1] == 121) {
                return true;
            }
        }
        return false;
    }

    private class SequenceEventStream extends EventStream {

        SequenceEventStream(Track track, long nanosPerTick, boolean looping) {
            this.eventGroup = new EventGroup(volume);
            this.looping = looping;
            this.nanosPerTick = nanosPerTick;
            this.track = track;
        }

        @Override
        EventGroup nextEventGroup() {
            if (track == null) return null;
            EventGroup result = eventGroup;
            for (;;) {
                if (pos >= track.size()) {
                    if (looping) {
                        pos = 0;
                        prevTick = 0L;
                    } else {
                        return result.emptyToNull();
                    }
                }
                MidiEvent event = track.get(pos++);
                if (event.getTick() > prevTick) {
                    long ticks = event.getTick() - prevTick;
                    prevTick = event.getTick();
                    result.addDelay(ticks * nanosPerTick);
                    eventGroup = new EventGroup(volume);
                    addMessageTo(eventGroup, event.getMessage());
                    return result;
                }
                addMessageTo(result, event.getMessage());
            }
        }

        void addMessageTo(EventGroup eventGroup, MidiMessage message) {
            /*
            for (Channel channel: midiChannels) {
                channel.volume(127, eventGroup);
            }
            */
            Channel vcChan = getVolumeChangeChannel(message);
            if (vcChan == null) {
                eventGroup.addMessage(message);
                if (0x80 >= message.getStatus() || message.getStatus() > 0xa0) {
                    System.err.println(message.getStatus() + ": " + Arrays.toString(message.getMessage()));
                }
                if (isControllerReset(message)) {
                    System.err.println("reset");
                }
            } else {
                vcChan.volume(message.getMessage()[2] & 0xff, eventGroup);
            }
        }

        private EventGroup eventGroup;

        private final boolean looping;

        private long nanosPerTick;

        private int pos;

        private long prevTick;

        private final Track track;

    }

    private class MusEventStream extends EventStream {

        MusEventStream(ByteBuffer scoreBuffer, boolean looping) {
            this.looping = looping;
            this.scoreBuffer = scoreBuffer.asReadOnlyBuffer();
        }

        @Override
        EventGroup nextEventGroup() {
            EventGroup result = new EventGroup(volume);
            boolean last;
            do {
                if (! scoreBuffer.hasRemaining()) {
                    if (looping) {
                        scoreBuffer.flip();
                    } else {
                        return result.emptyToNull();
                    }
                }
                int descriptor = scoreBuffer.get() & 0xff;
                last = (descriptor & 0x80) != 0;
                int eventType = (descriptor >> 4) & 7;
                int chanIndex = descriptor & 15;
                Channel channel = musChannels.get(chanIndex);
                switch (eventType) {
                case 0:
                    {
                        int note = scoreBuffer.get() & 0xff;
                        if ((note & 0x80) != 0) {
                            throw new IllegalArgumentException("Invalid note byte");
                        }
                        checkChannelExists("note off", channel).noteOff(note, result);
                    }
                    break;
                case 1:
                    {
                        int note = scoreBuffer.get() & 0xff;
                        boolean hasVelocity = (note & 0x80) != 0;
                        if (hasVelocity) {
                            int velocity = scoreBuffer.get() & 0xff;
                            if ((velocity & 0x80) != 0) {
                                throw new IllegalArgumentException("Invalid velocity byte");
                            }
                            checkChannelExists("note on", channel).noteOn(note & 127, velocity, result);
                        } else {
                            checkChannelExists("note on", channel).noteOn(note, result);
                        }
                    }
                    break;
                case 2:
                    {
                        int wheelVal = scoreBuffer.get() & 0xff;
                        checkChannelExists("pitch bend", channel).pitchBend(wheelVal, result);
                    }
                    break;
                case 3:
                    {
                        int sysEvt = scoreBuffer.get() & 0xff;
                        switch (sysEvt) {
                        case 10:
                            checkChannelExists("all sounds off", channel).allSoundsOff(result);
                            break;
                        case 11:
                            checkChannelExists("all notes off", channel).allNotesOff(result);
                            break;
                        case 14:
                            checkChannelExists("reset all controllers", channel).resetAll(result);
                            break;
                        default:
                            String msg = String.format("Invalid system event (%d)", sysEvt);
                            throw new IllegalArgumentException(msg);
                        }
                    }
                    break;
                case 4:
                    int cNum = scoreBuffer.get() & 0xff;
                    if ((cNum & 0x80) != 0) {
                        throw new IllegalArgumentException("Invalid controller number ");
                    }
                    int cVal = scoreBuffer.get() & 0xff;
                    if (cNum == 3 && 133 <= cVal && cVal <= 135) {
                        // workaround for some TNT.WAD tracks
                        cVal = 127;
                    }
                    if ((cVal & 0x80) != 0) {
                        String msg = String.format("Invalid controller value (%d; cNum=%d)", cVal, cNum);
                        throw new IllegalArgumentException(msg);
                    }
                    switch (cNum) {
                    case 0:
                        checkChannelExists("patch change", channel).patchChange(cVal, result);
                        break;
                    case 1:
                        // Don't forward this to the MIDI device.  Some devices
                        // react badly to banks that are undefined in GM Level 1
                        checkChannelExists("bank switch", channel);
                        break;
                    case 2:
                        checkChannelExists("vibrato change", channel).vibratoChange(cVal, result);
                        break;
                    case 3:
                        checkChannelExists("volume", channel).volume(cVal, result);
                        break;
                    case 4:
                        checkChannelExists("pan", channel).pan(cVal, result);
                        break;
                    case 5:
                        checkChannelExists("expression", channel).expression(cVal, result);
                        break;
                    case 6:
                        checkChannelExists("reverb depth", channel).reverbDepth(cVal, result);
                        break;
                    case 7:
                        checkChannelExists("chorus depth", channel).chorusDepth(cVal, result);
                        break;
                    default:
                        throw new AssertionError("Controller number " + cNum + ": not yet implemented");
                    }
                    break;
                case 6:
                    if (looping) {
                        scoreBuffer.flip();
                    } else {
                        return result.emptyToNull();
                    }
                    break;
                default:
                    String msg = String.format("Unknown event type: last=%5s eventType=%d chanIndex=%d%n", last, eventType, chanIndex);
                    throw new IllegalArgumentException(msg);
                }
            } while (! last);
            int qTics = readVLV(scoreBuffer);
            result.addDelay(qTics * NANOS_PER_MUS_TICK);
            return result;
        }

        private final boolean looping;

        private final ByteBuffer scoreBuffer;

    }

    static class EventGroup {
        EventGroup(float volScale) {
            this.messages = new ArrayList<MidiMessage>();
            this.volScale = volScale;
        }
        public void addMessage(MidiMessage message) {
            messages.add(message);
        }
        void addDelay(long nanos) {
            delay += nanos;
        }
        void allNotesOff(int midiChan) {
            addControlChange(midiChan, CHM_ALL_NOTES_OFF, 0);
        }
        void allSoundsOff(int midiChan) {
            addControlChange(midiChan, CHM_ALL_SOUND_OFF, 0);
        }
        long appendTo(Sequence sequence, int trackNum, long pos) {
            Track track = sequence.getTracks()[trackNum];
            for (MidiMessage msg: messages) {
                track.add(new MidiEvent(msg, pos));
            }
            return pos + delay * 3;
        }
        long appendTo(Track track, long pos, int scale) {
            for (MidiMessage msg: messages) {
                track.add(new MidiEvent(msg, pos));
            }
            return pos + delay * scale;
        }
        void chorusDepth(int midiChan, int depth) {
            addControlChange(midiChan, CTRL_CHORUS_DEPTH, depth);
        }
        void generalMidi(int mode) {
             addSysExMessage(0xf0, (byte)0x7e, (byte)0x7f, (byte)9, (byte)mode, (byte)0xf7);
        }
        EventGroup emptyToNull() {
            if (messages.isEmpty()) {
                return null;
            } else {
                return this;
            }
        }
        void expression(int midiChan, int expr) {
            addControlChange(midiChan, CTRL_EXPRESSION_POT, expr);
        }
        long getDelay() {
            return delay;
        }
        void noteOn(int midiChan, int note, int velocity) {
            addShortMessage(midiChan, ShortMessage.NOTE_ON, note, velocity);
        }
        void noteOff(int midiChan, int note) {
            addShortMessage(midiChan, ShortMessage.NOTE_OFF, note, 0);
        }
        void pan(int midiChan, int pan) {
            addControlChange(midiChan, CTRL_PAN, pan);
        }
        void patchChange(int midiChan, int patchId) {
            addShortMessage(midiChan, ShortMessage.PROGRAM_CHANGE, patchId, 0);
        }
        void pitchBend(int midiChan, int wheelVal) {
            int pb14 = wheelVal * 64;
            addShortMessage(midiChan, ShortMessage.PITCH_BEND, pb14 % 128, pb14 / 128);
        }
        void pitchBendSensitivity(int midiChan, int semitones) {
            addRegParamChange(midiChan, RPM_PITCH_BEND_SENSITIVITY, RPL_PITCH_BEND_SENSITIVITY, semitones);
        }
        void resetAllControllers(int midiChan) {
            addControlChange(midiChan, CHM_RESET_ALL, 0);
        }
        void reverbDepth(int midiChan, int depth) {
            addControlChange(midiChan, CTRL_REVERB_DEPTH, depth);
        }
        void sendTo(Receiver receiver) {
            for (MidiMessage msg: messages) {
                receiver.send(msg, -1);
            }
        }
        void vibratoChange(int midiChan, int depth) {
            addControlChange(midiChan, CTRL_MODULATION_POT, depth);
        }
        void volume(int midiChan, int vol) {
            int adjVol = Math.max(0, Math.min((int) Math.round(vol * volScale), 127));
            addControlChange(midiChan, CTRL_VOLUME, adjVol);
        }
        private void addControlChange(int midiChan, int ctrlId, int ctrlVal) {
            addShortMessage(midiChan, ShortMessage.CONTROL_CHANGE, ctrlId, ctrlVal);
        }
        private void addRegParamChange(int midiChan, int paramMsb, int paramLsb, int valMsb) {
            addControlChange(midiChan, 101, paramMsb);
            addControlChange(midiChan, 100, paramLsb);
            addControlChange(midiChan, 6, valMsb);
        }
        private void addShortMessage(int midiChan, int cmd, int data1, int data2) {
            try {
                ShortMessage msg = new ShortMessage();
                msg.setMessage(cmd, midiChan, data1, data2);
                messages.add(msg);
            } catch (InvalidMidiDataException ex) {
                throw new RuntimeException(ex);
            }
        }
        private void addSysExMessage(int status, byte... data) {
            try {
                SysexMessage msg = new SysexMessage();
                msg.setMessage(status, data, data.length);
                messages.add(msg);
            } catch (InvalidMidiDataException ex) {
                throw new RuntimeException(ex);
            }
        }

        private static final int CHM_ALL_NOTES_OFF = 123;
        private static final int CHM_ALL_SOUND_OFF = 120;
        private static final int CTRL_CHORUS_DEPTH = 93;
        private static final int CTRL_EXPRESSION_POT = 11;
        private static final int CTRL_PAN = 10;
        private static final int RPM_PITCH_BEND_SENSITIVITY = 0;
        private static final int RPL_PITCH_BEND_SENSITIVITY = 0;
        private static final int CHM_RESET_ALL = 121;
        private static final int CTRL_REVERB_DEPTH = 91;
        private static final int CTRL_MODULATION_POT = 1;
        private static final int CTRL_VOLUME = 7;

        private long delay;
        private final List<MidiMessage> messages;
        private final float volScale;
    }

    static class ThreadFactoryImpl implements ThreadFactory {
        @Override
        public Thread newThread(final Runnable r) {
            Thread thread =
                new Thread(r, String.format("FinnwMusicModule-%d", NEXT_ID.getAndIncrement()));
            thread.setPriority(Thread.MAX_PRIORITY - 1);
            return thread;
        }
        private static final AtomicInteger NEXT_ID =
            new AtomicInteger(1);
    }

    final ReentrantLock lock;

    static final long NANOS_PER_MUS_TICK = 1000000000 / 140;

    /** Channels in standard MIDI order (0-8 and 10-15 = instruments, 9 = percussion) */
    final List<Channel> midiChannels;

    /** Channels in MUS order (0-14 = instruments, 15 = percussion) */
    final List<Channel> musChannels;

    ScheduledExecutorService exec;

    float volume;

    private static Receiver getReceiver() throws MidiUnavailableException {
        List<MidiDevice.Info> dInfos =
            new ArrayList<MidiDevice.Info>(Arrays.asList(MidiSystem.getMidiDeviceInfo()));
        for (Iterator<MidiDevice.Info> it = dInfos.iterator();
             it.hasNext();
             ) {
            MidiDevice.Info dInfo = it.next();
            MidiDevice dev = MidiSystem.getMidiDevice(dInfo);
            if (dev.getMaxReceivers() == 0) {
                // We cannot use input-only devices
                it.remove();
            }
        }
        if (dInfos.isEmpty()) return null;
        Collections.sort(dInfos, new VolumeScalingReceiver.MidiDeviceComparator());
        MidiDevice.Info dInfo = dInfos.get(0);
        MidiDevice dev = MidiSystem.getMidiDevice((MidiDevice.Info) dInfo);
        dev.open();
        return dev.getReceiver();
    }

    private void prepare(Receiver receiver) {
        EventGroup setupEG = new EventGroup(volume);
        for (Channel chan: musChannels) {
            chan.allSoundsOff(setupEG);
            chan.resetAll(setupEG);
            chan.pitchBendSensitivity(2, setupEG);
            chan.volume(127, setupEG);
        }
        setupEG.sendTo(receiver);
    }

    private static void sleepUninterruptibly(int timeout, TimeUnit timeUnit) {
        boolean interrupted = false;
        long now = System.nanoTime();
        final long expiry = now + timeUnit.toNanos(timeout);
        long remaining;
        while ((remaining = expiry - now) > 0L) {
            try {
                TimeUnit.NANOSECONDS.sleep(remaining);
            } catch (InterruptedException ex) {
                interrupted = true;
            } finally {
                now = System.nanoTime();
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
    static Channel checkChannelExists(String type, Channel channel)
            throws IllegalArgumentException {
        if (channel == null) {
            String msg = String.format("Invalid channel for %s message", type);
            throw new IllegalArgumentException(msg);
        } else {
            return channel;
        }
    }

    int readVLV(ByteBuffer scoreBuffer) {
        int result = 0;
        boolean last;
        do {
            int digit = scoreBuffer.get() & 0xff;
            last = (digit & 0x80) == 0;
            result <<= 7;
            result |= digit & 127;
        } while (! last);
        return result;
    }

    private static class Channel {
        Channel(int midiChan) {
            this.midiChan = midiChan;
        }
        void allNotesOff(EventGroup eventGroup) {
            eventGroup.allNotesOff(midiChan);
        }
        void allSoundsOff(EventGroup eventGroup) {
            eventGroup.allSoundsOff(midiChan);
        }
        void chorusDepth(int depth, EventGroup eventGroup) {
            eventGroup.chorusDepth(midiChan, depth);
        }
        void expression(int expr, EventGroup eventGroup) {
            eventGroup.expression(midiChan, expr);
        }
        void noteOff(int note, EventGroup eventGroup) {
            eventGroup.noteOff(midiChan, note);
        }
        void noteOn(int note, EventGroup eventGroup) {
            eventGroup.noteOn(midiChan, note, lastVelocity);
        }
        void noteOn(int note, int velocity, EventGroup eventGroup) {
            lastVelocity = velocity;
            noteOn(note, eventGroup);
        }
        void pan(int pan, EventGroup eventGroup) {
            eventGroup.pan(midiChan, pan);
        }
        void patchChange(int patchId, EventGroup eventGroup) {
            eventGroup.patchChange(midiChan, patchId);
        }
        void pitchBend(int wheelVal, EventGroup eventGroup) {
            eventGroup.pitchBend(midiChan, wheelVal);
        }
        void pitchBendSensitivity(int semitones, EventGroup eventGroup) {
            eventGroup.pitchBendSensitivity(midiChan, semitones);
        }
        void resetAll(EventGroup eventGroup) {
            eventGroup.resetAllControllers(midiChan);
        }
        void reverbDepth(int depth, EventGroup eventGroup) {
            eventGroup.reverbDepth(midiChan, depth);
        }
        void vibratoChange(int depth, EventGroup eventGroup) {
            eventGroup.vibratoChange(midiChan, depth);
        }
        void volume(int vol, EventGroup eventGroup) {
            eventGroup.volume(midiChan, vol);
            lastVolume = vol;
        }
        void volumeChanged(EventGroup eventGroup) {
            eventGroup.volume(midiChan, lastVolume);
        }
        private int lastVelocity;
        private int lastVolume;
        private final int midiChan;
    }

    private class ScheduledTransmitter implements Transmitter {

        @Override
        public void close() {
            stop();
        }

        @Override
        public Receiver getReceiver() {
            return receiver;
        }

        @Override
        public void setReceiver(Receiver receiver) {
            EventGroup currentGroup = null;
            lock.lock();
            try {
                if (this.receiver != null) {
                    if (this.future.cancel(false)) {
                        currentGroup = triggerTask.eventGroup;
                    }
                } else {
                    nextGroupTime = System.nanoTime();
                }
                this.receiver = receiver;
                scheduleIfRequired(receiver, currentGroup);
            } finally {
                lock.unlock();
            }
        }

        ScheduledTransmitter(EventStream eventStream) {
            this.eventStream = eventStream;
        }

        void scheduleIfRequired(Receiver receiver,
                                EventGroup currentGroup) {
            assert lock.isHeldByCurrentThread();
            if (currentGroup == null) {
                try {
                    currentGroup = eventStream.nextEventGroup();
                    if (currentGroup != null) {
                        triggerTask = new TriggerTask(currentGroup, receiver);
                        long delay = Math.max(0, nextGroupTime - System.nanoTime());
                        future =
                            exec.schedule(triggerTask, delay, TimeUnit.NANOSECONDS);
                        nextGroupTime += currentGroup.getDelay();
                    } else {
                        triggerTask = null;
                        future = null;
                    }
                } catch (RejectedExecutionException ex) {
                    // This is normal when shutting down
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            }
        }

        void stop() {
            assert lock.isHeldByCurrentThread();
            if (future != null) {
                future.cancel(false);
                try {
                    future.get();
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                } catch (CancellationException ex) {
                }
                future = null;
            }
            EventGroup cleanup = new EventGroup(0f);
            for (Channel chan: musChannels) {
                chan.allNotesOff(cleanup);
            }
            cleanup.sendTo(receiver);
        }

        void volumeChanged() {
            assert lock.isHeldByCurrentThread();
            EventGroup adjust = new EventGroup(volume);
            for (Channel chan: musChannels) {
                chan.volumeChanged(adjust);
            }
            adjust.sendTo(receiver);
        }
        TriggerTask triggerTask;

        private class TriggerTask implements Runnable {
            @Override
            public void run() {
                boolean shouldSend = false;
                lock.lock();
                try {
                    if (triggerTask == this) {
                        shouldSend = true;
                        scheduleIfRequired(receiver, null);
                    }
                } finally {
                    lock.unlock();
                }
                if (shouldSend) {
                    eventGroup.sendTo(receiver);
                }
            }
            TriggerTask(EventGroup eventGroup, Receiver receiver) {
                this.eventGroup = eventGroup;
                this.receiver = receiver;
            }

            final EventGroup eventGroup;
            final Receiver receiver;
        }

        private final EventStream eventStream;

        private ScheduledFuture<?> future;

        private long nextGroupTime;

        private Receiver receiver;
    }

    private static abstract class Song {
        Song() {}
        abstract EventStream newEventStream(boolean looping);
    }

    /** Null song (for when we cannot decode the music lump) */
    private class NullSong extends Song {

        NullSong() {
        }

        /** Create an empty, non-looping MUS stream */
        @Override
        EventStream newEventStream(boolean looping) {
            ByteBuffer buf = ByteBuffer.allocate(0);
            return new MusEventStream(buf, false);
        }

    }

    /** Extracts MIDI events from a Sequence, without using a Sequencer. */
    private class MidiSong extends Song {

        private static final double DEFAULT_BPM = 120.0;

        MidiSong(Sequence sequence) {
            float framesPerSec = sequence.getDivisionType();
            if (framesPerSec == Sequence.PPQ) {
                framesPerSec = (float) (DEFAULT_BPM / 60.0);
                smpteRes = sequence.getResolution();
            } else {
                smpteRes = 0;
            }
            this.nanosPerTick =
                Math.round(1000000000.0 / (framesPerSec * sequence.getResolution()));
            Track[] tracks = sequence.getTracks();
            System.err.println(Arrays.asList(tracks));
            if (tracks.length > 0) {
                Track mergedTrack = mergeTracks(sequence);
                this.track = mergedTrack;
            } else {
                this.track = null;
            }
        }

        @Override
        EventStream newEventStream(boolean looping) {
            return new SequenceEventStream(track, nanosPerTick, looping);
        }

        private Track mergeTracks(Sequence sequence) {
            ByteBuffer tBuf = ByteBuffer.allocate(4);
            Track[] tracks = sequence.getTracks();
            Track mergedTrack = sequence.createTrack();
            for (Track sourceTrack: tracks) {
                for (int i = 0; i < sourceTrack.size(); ++ i) {
                    MidiEvent event = sourceTrack.get(i);
                    MidiMessage msg = event.getMessage();
                    if (msg.getStatus() == 255) {
                        byte[] mBytes = msg.getMessage();
                        if (mBytes.length <= 1) continue;
                        int data1 = 0xff & mBytes[1];
                        switch (data1) {
                        case 81:
                            if (mBytes.length <= 2) continue;
                            if (mBytes[2] == 3) {
                                tBuf.clear();
                                tBuf.put((byte) 0);
                                tBuf.put(mBytes, 3, 3);
                                tBuf.flip();
                                setTempo(tBuf.getInt());
                            }
                            break;
                        }
                    } else {
                        mergedTrack.add(event);
                    }
                }
                sequence.deleteTrack(sourceTrack);
            }
            MetaMessage endOfSong = new MetaMessage();
            try {
                endOfSong.setMessage(47, new byte[] {0}, 1);
            } catch (InvalidMidiDataException ex) {
                ex.printStackTrace();
            }
            return mergedTrack;
        }

        void setTempo(int tempo) {
            if (smpteRes > 0) {
                nanosPerTick = Math.round((1000.0) * (tempo / smpteRes));
;           }
        }

        private long nanosPerTick;

        private final int smpteRes;

        private final Track track;

    }

    /** Contains unfiltered MUS data, decoded on-the-fly. */
    private class MusSong extends Song {

        MusSong(ByteBuffer data) {
            this.data = data.asReadOnlyBuffer();
            this.data.order(ByteOrder.LITTLE_ENDIAN);
            byte[] magic = new byte[4];
            this.data.get(magic);
            ByteBuffer magicBuf = ByteBuffer.wrap(magic);
            if (! hasMusMagic(magicBuf)) {
                throw new IllegalArgumentException("Expected magic string \"MUS\\x1a\" but found " + Arrays.toString(magic));
            }
            this.scoreLen = this.data.getShort() & 0xffff;
            this.scoreStart = this.data.getShort() & 0xffff;
        }

        /** Get only the score part of the data (skipping the header) */
        @Override
        EventStream newEventStream(boolean looping) {
            ByteBuffer scoreBuffer = this.data.duplicate();
            scoreBuffer.position(scoreStart);
            scoreBuffer.limit(scoreStart + scoreLen);
            ByteBuffer slice = scoreBuffer.slice();
            return new MusEventStream(slice, looping);
        }

        private final ByteBuffer data;
        private final int scoreLen;
        private final int scoreStart;

    }

    private ScheduledTransmitter currentTransmitter;

    private Receiver receiver;

    /** Songs indexed by handle */
    private final List<Song> songs;

}
