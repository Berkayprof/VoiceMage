package com.voicemage.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voicemage.Voicemage;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Per-player settings that never go to the server, stored in config/voicemage-client.json. */
public final class ClientSettings {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static Data data;

   private ClientSettings() {
   }

   private static final class Data {
      String microphone = "";
   }

   /** Exact name of the chosen microphone, or an empty string for the system default. */
   public static synchronized String microphone() {
      return load().microphone == null ? "" : load().microphone;
   }

   public static synchronized void setMicrophone(String name) {
      load().microphone = name == null ? "" : name;
      save();
   }

   private static Path path() {
      return FabricLoader.getInstance().getConfigDir().resolve("voicemage-client.json");
   }

   private static Data load() {
      if (data == null) {
         data = new Data();
         Path path = path();
         if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
               Data read = GSON.fromJson(reader, Data.class);
               if (read != null) {
                  data = read;
               }
            } catch (Exception exception) {
               Voicemage.LOGGER.warn("Could not read {}; using the system default microphone", path, exception);
            }
         }
      }

      return data;
   }

   private static void save() {
      Path path = path();

      try {
         Files.createDirectories(path.getParent());

         try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(data, writer);
         }
      } catch (IOException exception) {
         Voicemage.LOGGER.warn("Could not write {}", path, exception);
      }
   }
}
