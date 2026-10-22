package com.voicemage.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.voicemage.Voicemage;
import com.voicemage.spell.Spell;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.DataLine.Info;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

public final class VoiceRecognitionService {
   private static final float SAMPLE_RATE = 16000.0F;
   private static final String GRAMMAR = "[\"fireball\",\"fire ball\",\"pitfall\",\"pit fall\",\"heal\",\"lightning\",\"lightening\",\"earth wall\",\"earthwall\",\"blink\",\"magic missile\",\"magic missle\",\"meteor\",\"wind blast\",\"windblast\",\"freeze\",\"clone\",\"polymorph\",\"poly morph\",\"shrink\",\"o ancient flame awaken from your endless slumber let the skys burn and turn my enemies to ash leaving nothing behind explosion\",\"o ancient flame awaken from your endless slumber let the skies burn and turn my enemies to ash leaving nothing behind explosion\",\"darkness blacker than black and darker than dark i beseech thee combine with my deep crimson return all creation to cinders and come from the abyss explosion\",\"[unk]\"]";
   private final BiConsumer<Spell, String> onSpell;
   private volatile boolean running;
   private volatile boolean explosionChantRequired = true;
   private volatile String status = "Voice: starting...";
   private volatile String partialText = "";
   private volatile TargetDataLine microphone;
   private volatile long generation;
   private Thread thread;

   public void setExplosionChantRequired(boolean required) {
      this.explosionChantRequired = required;
   }

   public boolean explosionChantRequired() {
      return this.explosionChantRequired;
   }

   public VoiceRecognitionService(BiConsumer<Spell, String> onSpell) {
      this.onSpell = onSpell;
   }

   public String status() {
      return this.status;
   }

   public String displayStatus() {
      String partial = this.partialText;
      return this.isListening() && !partial.isBlank() ? "Hearing: \"" + partial + "\"" : this.status;
   }

   public boolean isRunning() {
      return this.running;
   }

   public boolean isListening() {
      return this.running && "Voice: listening".equals(this.status);
   }

   public synchronized void start() {
      if (!this.running) {
         this.running = true;
         this.partialText = "";
         this.status = "Voice: starting...";
         long runId = ++this.generation;
         this.thread = new Thread(() -> this.run(runId), "voicemage speech recognition");
         this.thread.setDaemon(true);
         this.thread.start();
      }
   }

   public synchronized void stop() {
      this.generation++;
      this.running = false;
      TargetDataLine active = this.microphone;
      if (active != null) {
         active.stop();
         active.close();
      }

      if (this.thread != null) {
         this.thread.interrupt();
      }

      this.microphone = null;
      this.partialText = "";
      this.status = "Voice: muted (press V to resume)";
   }

   private void run(long runId) {
      try {
         Path modelPath = SpeechModelInstaller.ensureInstalled(value -> this.setStatus(runId, value));
         if (this.isCurrent(runId)) {
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            AudioFormat format = new AudioFormat(16000.0F, 16, 1, true, false);
            Info info = new Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
               throw new IllegalStateException("No microphone supports 16 kHz mono audio");
            }

            Model model = new Model(modelPath.toString());

            label211: {
               try {
                  Recognizer recognizer = new Recognizer(
                     model,
                     16000.0F,
                     "[\"fireball\",\"fire ball\",\"pitfall\",\"pit fall\",\"heal\",\"lightning\",\"lightening\",\"earth wall\",\"earthwall\",\"blink\",\"magic missile\",\"magic missle\",\"meteor\",\"wind blast\",\"windblast\",\"freeze\",\"clone\",\"polymorph\",\"poly morph\",\"shrink\",\"o ancient flame awaken from your endless slumber let the skys burn and turn my enemies to ash leaving nothing behind explosion\",\"o ancient flame awaken from your endless slumber let the skies burn and turn my enemies to ash leaving nothing behind explosion\",\"darkness blacker than black and darker than dark i beseech thee combine with my deep crimson return all creation to cinders and come from the abyss explosion\",\"[unk]\"]"
                  );

                  label175: {
                     try {
                        TargetDataLine line = (TargetDataLine)AudioSystem.getLine(info);
                        if (this.isCurrent(runId)) {
                           this.microphone = line;
                           line.open(format);
                           line.start();
                           this.status = "Voice: listening";
                           this.recognizeLoop(runId, line, recognizer);
                           break label175;
                        }
                     } catch (Throwable var21) {
                        try {
                           recognizer.close();
                        } catch (Throwable var20) {
                           var21.addSuppressed(var20);
                        }

                        throw var21;
                     }

                     recognizer.close();
                     break label211;
                  }

                  recognizer.close();
               } catch (Throwable var22) {
                  try {
                     model.close();
                  } catch (Throwable var19) {
                     var22.addSuppressed(var19);
                  }

                  throw var22;
               }

               model.close();
               return;
            }

            model.close();
            return;
         }
      } catch (InterruptedException interrupted) {
         Thread.currentThread().interrupt();
         return;
      } catch (Exception error) {
         if (this.isCurrent(runId)) {
            this.status = "Voice error: " + concise(error.getMessage());
            Voicemage.LOGGER.error("Voice recognition stopped", error);
         }

         return;
      } finally {
         if (this.generation == runId) {
            TargetDataLine active = this.microphone;
            if (active != null) {
               active.close();
            }

            this.microphone = null;
            this.running = false;
         }
      }
   }

   private void recognizeLoop(long runId, TargetDataLine line, Recognizer recognizer) {
      byte[] buffer = new byte[4096];
      String stablePartial = "";
      int stableCount = 0;
      Spell lastSpell = null;
      long lastCastNanos = 0L;
      boolean activeChantRequired = true;

      while (this.isCurrent(runId) && !Thread.currentThread().isInterrupted()) {
         boolean required = this.explosionChantRequired;
         if (activeChantRequired != required) {
            recognizer.reset();
            recognizer.setGrammar(
               required
                  ? "[\"fireball\",\"fire ball\",\"pitfall\",\"pit fall\",\"heal\",\"lightning\",\"lightening\",\"earth wall\",\"earthwall\",\"blink\",\"magic missile\",\"magic missle\",\"meteor\",\"wind blast\",\"windblast\",\"freeze\",\"clone\",\"polymorph\",\"poly morph\",\"shrink\",\"o ancient flame awaken from your endless slumber let the skys burn and turn my enemies to ash leaving nothing behind explosion\",\"o ancient flame awaken from your endless slumber let the skies burn and turn my enemies to ash leaving nothing behind explosion\",\"darkness blacker than black and darker than dark i beseech thee combine with my deep crimson return all creation to cinders and come from the abyss explosion\",\"[unk]\"]"
                  : "[\"fireball\",\"fire ball\",\"pitfall\",\"pit fall\",\"heal\",\"lightning\",\"lightening\",\"earth wall\",\"earthwall\",\"blink\",\"magic missile\",\"magic missle\",\"meteor\",\"wind blast\",\"windblast\",\"freeze\",\"clone\",\"polymorph\",\"poly morph\",\"shrink\",\"o ancient flame awaken from your endless slumber let the skys burn and turn my enemies to ash leaving nothing behind explosion\",\"o ancient flame awaken from your endless slumber let the skies burn and turn my enemies to ash leaving nothing behind explosion\",\"darkness blacker than black and darker than dark i beseech thee combine with my deep crimson return all creation to cinders and come from the abyss explosion\",\"[unk]\"]"
                     .replace("\"[unk]\"", "\"explosion\",\"[unk]\"")
            );
            activeChantRequired = required;
            stablePartial = "";
            stableCount = 0;
            this.partialText = "";
         }

         int read = line.read(buffer, 0, buffer.length);
         if (read > 0) {
            if (recognizer.acceptWaveForm(buffer, read)) {
               String text = jsonText(recognizer.getResult(), "text");
               this.partialText = text;
               Spell spell = Spell.byPhrase(text, this.explosionChantRequired);
               if (spell != null && allowed(spell, lastSpell, lastCastNanos)) {
                  this.onSpell.accept(spell, text);
                  lastSpell = spell;
                  lastCastNanos = System.nanoTime();
               }

               stablePartial = "";
               stableCount = 0;
            } else {
               String partial = jsonText(recognizer.getPartialResult(), "partial");
               this.partialText = partial;
               if (partial.equals(stablePartial)) {
                  stableCount++;
               } else {
                  stablePartial = partial;
                  stableCount = 1;
               }

               Spell spell = Spell.byPhrase(partial, this.explosionChantRequired);
               if (spell != null && stableCount >= 2 && allowed(spell, lastSpell, lastCastNanos)) {
                  this.onSpell.accept(spell, partial);
                  lastSpell = spell;
                  lastCastNanos = System.nanoTime();
                  recognizer.reset();
                  this.partialText = "";
                  stablePartial = "";
                  stableCount = 0;
               }
            }
         }
      }
   }

   private boolean isCurrent(long runId) {
      return this.running && this.generation == runId;
   }

   private void setStatus(long runId, String value) {
      if (this.isCurrent(runId)) {
         this.status = value;
      }
   }

   private static boolean allowed(Spell current, Spell previous, long previousNanos) {
      long elapsed = System.nanoTime() - previousNanos;
      long debounce = current == previous ? 1500000000L : 350000000L;
      return elapsed >= debounce;
   }

   private static String jsonText(String json, String key) {
      try {
         JsonObject object = JsonParser.parseString(json).getAsJsonObject();
         return object.has(key) ? object.get(key).getAsString() : "";
      } catch (RuntimeException ignored) {
         return "";
      }
   }

   private static String concise(String message) {
      if (message != null && !message.isBlank()) {
         return message.length() <= 80 ? message : message.substring(0, 77) + "...";
      } else {
         return "unknown problem";
      }
   }
}
