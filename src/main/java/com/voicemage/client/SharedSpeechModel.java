package com.voicemage.client;

import java.io.IOException;
import java.nio.file.Path;
import org.vosk.Model;

/**
 * Keeps the (slow to load, ~300 MB) speech model in memory while voice recognition is muted and unmuted, so
 * pressing the mute key no longer reloads it. The model is freed when the player leaves the world or the game closes.
 */
public final class SharedSpeechModel {
   private static Model model;
   private static Path loadedFrom;
   private static int users;
   private static boolean freeWhenIdle;

   private SharedSpeechModel() {
   }

   static synchronized Model acquire(Path path) throws IOException {
      freeWhenIdle = false;
      if (model != null && !path.equals(loadedFrom) && users == 0) {
         closeModel();
      }

      if (model == null) {
         model = new Model(path.toString());
         loadedFrom = path;
      }

      users++;
      return model;
   }

   static synchronized void release() {
      if (users > 0) {
         users--;
      }

      if (users == 0 && freeWhenIdle) {
         closeModel();
      }
   }

   /** Frees the model as soon as nothing is using it (immediately if it is idle already). */
   public static synchronized void freeWhenIdle() {
      freeWhenIdle = true;
      if (users == 0) {
         closeModel();
      }
   }

   private static void closeModel() {
      if (model != null) {
         try {
            model.close();
         } catch (RuntimeException ignored) {
         }

         model = null;
         loadedFrom = null;
      }
   }
}
