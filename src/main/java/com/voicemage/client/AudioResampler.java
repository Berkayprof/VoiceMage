package com.voicemage.client;

/**
 * Streaming down-sampler for mono 16-bit audio. Every output sample is the average of the input samples that
 * fall into its time slot (a box filter), which is cheap and keeps aliasing low enough for speech recognition.
 * Works for any ratio >= 1, e.g. 48000 -> 16000 (3.0) or 44100 -> 16000 (2.75625).
 */
final class AudioResampler {
   private final double ratio;
   private double accumulator;
   private double phase;

   AudioResampler(double inputRate, double outputRate) {
      if (inputRate < outputRate) {
         throw new IllegalArgumentException("Up-sampling is not supported");
      }

      this.ratio = inputRate / outputRate;
   }

   /** Converts {@code length} input samples and returns how many output samples were written to {@code out}. */
   int process(short[] in, int length, short[] out) {
      int written = 0;

      for (int i = 0; i < length; i++) {
         double remaining = this.ratio - this.phase;
         if (remaining > 1.0) {
            this.accumulator += in[i];
            this.phase += 1.0;
         } else {
            this.accumulator += in[i] * remaining;
            out[written++] = clamp(this.accumulator / this.ratio);
            this.accumulator = in[i] * (1.0 - remaining);
            this.phase = 1.0 - remaining;
         }
      }

      return written;
   }

   private static short clamp(double value) {
      long rounded = Math.round(value);
      return (short)Math.max(-32768L, Math.min(32767L, rounded));
   }
}
