package com.voicemage.client;

import com.voicemage.Voicemage;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Opens a microphone and always delivers 16 kHz, mono, 16-bit little-endian PCM, which is what the speech model
 * needs. Devices that cannot record at 16 kHz mono are opened at a higher rate and/or in stereo and converted.
 */
final class MicrophoneCapture implements AutoCloseable {
   static final int OUTPUT_BUFFER_BYTES = 8192;
   private static final int TARGET_RATE = 16000;
   private static final int CHUNK_SAMPLES = 2048;
   /** Preferred first: native 16 kHz, then common rates that can be down-sampled. */
   private static final int[] CAPTURE_RATES = new int[]{16000, 48000, 44100, 32000, 22050};

   private final TargetDataLine line;
   private final String deviceName;
   private final int sourceRate;
   private final int channels;
   private final byte[] raw;
   private final short[] mono;
   private final short[] converted = new short[CHUNK_SAMPLES + 16];
   private final AudioResampler resampler;
   private volatile boolean closed;

   private MicrophoneCapture(TargetDataLine line, AudioFormat format, String deviceName) {
      this.line = line;
      this.deviceName = deviceName;
      this.sourceRate = (int)format.getSampleRate();
      this.channels = format.getChannels();
      double ratio = (double)this.sourceRate / TARGET_RATE;
      int frames = (int)Math.ceil(CHUNK_SAMPLES * ratio);
      this.raw = new byte[frames * this.channels * 2];
      this.mono = new short[frames];
      this.resampler = this.sourceRate == TARGET_RATE ? null : new AudioResampler(this.sourceRate, TARGET_RATE);
   }

   /** Every device that can record audio, in the order Java reports them. */
   static List<String> deviceNames() {
      List<String> names = new ArrayList<>();

      for (Mixer.Info info : AudioSystem.getMixerInfo()) {
         try {
            if (AudioSystem.getMixer(info).getTargetLineInfo().length > 0) {
               names.add(info.getName());
            }
         } catch (RuntimeException ignored) {
         }
      }

      return names;
   }

   /**
    * @param preferredName exact device name as listed by {@link #deviceNames()}, or empty for the system default
    */
   static MicrophoneCapture open(String preferredName) {
      Mixer mixer = null;
      String label = "system default";
      if (preferredName != null && !preferredName.isBlank()) {
         for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(preferredName)) {
               try {
                  Mixer candidate = AudioSystem.getMixer(info);
                  if (candidate.getTargetLineInfo().length > 0) {
                     mixer = candidate;
                     label = info.getName();
                     break;
                  }
               } catch (RuntimeException ignored) {
               }
            }
         }

         if (mixer == null) {
            Voicemage.LOGGER.warn("Microphone '{}' was not found; using the system default instead", preferredName);
         }
      }

      LineUnavailableException lastFailure = null;

      for (int rate : CAPTURE_RATES) {
         for (int channels = 1; channels <= 2; channels++) {
            AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            try {
               if (!(mixer == null ? AudioSystem.isLineSupported(info) : mixer.isLineSupported(info))) {
                  continue;
               }

               TargetDataLine line = (TargetDataLine)(mixer == null ? AudioSystem.getLine(info) : mixer.getLine(info));
               line.open(format);
               line.start();
               return new MicrophoneCapture(line, format, label);
            } catch (LineUnavailableException failure) {
               lastFailure = failure;
            } catch (IllegalArgumentException | SecurityException failure) {
               lastFailure = new LineUnavailableException(String.valueOf(failure.getMessage()));
            }
         }
      }

      String reason = lastFailure == null ? "it does not offer a usable recording format" : lastFailure.getMessage();
      throw new IllegalStateException("Microphone '" + label + "' cannot be opened: " + reason);
   }

   String deviceName() {
      return this.deviceName;
   }

   String describe() {
      String conversion = this.sourceRate == TARGET_RATE && this.channels == 1 ? "" : ", converted from " + this.sourceRate + " Hz/" + this.channels + " ch";
      return this.deviceName + conversion;
   }

   /**
    * Blocks until a chunk of audio is available and writes it to {@code out} (at least
    * {@link #OUTPUT_BUFFER_BYTES} bytes long) as 16 kHz mono PCM.
    *
    * @return number of bytes written, or -1 once the microphone has been closed
    */
   int read(byte[] out) {
      if (this.closed) {
         return -1;
      }

      int bytesRead;
      try {
         bytesRead = this.line.read(this.raw, 0, this.raw.length);
      } catch (RuntimeException closedWhileReading) {
         return -1;
      }

      if (this.closed || bytesRead < 0) {
         return -1;
      }

      int frameBytes = this.channels * 2;
      int frames = bytesRead / frameBytes;

      for (int frame = 0; frame < frames; frame++) {
         int index = frame * frameBytes;
         int left = (short)(this.raw[index + 1] << 8 | this.raw[index] & 255);
         if (this.channels == 2) {
            int right = (short)(this.raw[index + 3] << 8 | this.raw[index + 2] & 255);
            this.mono[frame] = (short)((left + right) / 2);
         } else {
            this.mono[frame] = (short)left;
         }
      }

      short[] samples = this.mono;
      int count = frames;
      if (this.resampler != null) {
         count = this.resampler.process(this.mono, frames, this.converted);
         samples = this.converted;
      }

      for (int i = 0; i < count; i++) {
         out[i * 2] = (byte)samples[i];
         out[i * 2 + 1] = (byte)(samples[i] >> 8);
      }

      return count * 2;
   }

   @Override
   public void close() {
      this.closed = true;

      try {
         this.line.stop();
         this.line.close();
      } catch (RuntimeException ignored) {
      }
   }
}
