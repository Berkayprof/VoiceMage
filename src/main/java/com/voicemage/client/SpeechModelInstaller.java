package com.voicemage.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.loader.api.FabricLoader;

final class SpeechModelInstaller {
   private static final String MODEL_NAME = "vosk-model-small-en-us-0.15";
   private static final URI MODEL_URI = URI.create("https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");

   private SpeechModelInstaller() {
   }

   static synchronized Path ensureInstalled(SpeechModelInstaller.StatusSink status) throws IOException, InterruptedException {
      Path parent = FabricLoader.getInstance().getGameDir().resolve("voicemage");
      Path model = parent.resolve("vosk-model-small-en-us-0.15");
      if (valid(model)) {
         return model;
      }

      Files.createDirectories(parent);
      deleteTree(model);
      Path archive = parent.resolve("vosk-model-small-en-us-0.15.zip.part");
      Path installing = parent.resolve("vosk-model-small-en-us-0.15.installing");
      Files.deleteIfExists(archive);
      deleteTree(installing);
      Files.createDirectories(installing);

      try {
         status.set("Downloading speech model (one time, ~40 MB)...");
         HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
         HttpRequest request = HttpRequest.newBuilder(MODEL_URI).header("User-Agent", "voicemage/1.0").build();
         HttpResponse<Path> response = client.send(request, BodyHandlers.ofFile(archive));
         if (response.statusCode() / 100 != 2) {
            throw new IOException("Model download returned HTTP " + response.statusCode());
         }

         status.set("Installing speech model...");
         extractModel(archive, installing);
         if (!valid(installing)) {
            throw new IOException("Downloaded speech model is incomplete");
         }

         try {
            Files.move(installing, model, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(installing, model);
         }

         return model;
      } finally {
         Files.deleteIfExists(archive);
         deleteTree(installing);
      }
   }

   private static void extractModel(Path archive, Path destination) throws IOException {
      ZipEntry entry;
      try (
         InputStream input = Files.newInputStream(archive);
         ZipInputStream zip = new ZipInputStream(input);
      ) {
         while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName().replace('\\', '/');
            String prefix = "vosk-model-small-en-us-0.15/";
            if (name.startsWith(prefix)) {
               String relative = name.substring(prefix.length());
               if (!relative.isEmpty()) {
                  Path output = destination.resolve(relative).normalize();
                  if (!output.startsWith(destination)) {
                     throw new IOException("Unsafe model archive path");
                  }

                  if (entry.isDirectory()) {
                     Files.createDirectories(output);
                  } else {
                     Files.createDirectories(output.getParent());
                     Files.copy(zip, output, StandardCopyOption.REPLACE_EXISTING);
                  }
               }
            }
         }
      }
   }

   private static boolean valid(Path model) {
      return Files.isRegularFile(model.resolve("am/final.mdl")) && Files.isRegularFile(model.resolve("conf/model.conf"));
   }

   private static void deleteTree(Path root) throws IOException {
      if (Files.exists(root)) {
         try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
               Files.deleteIfExists(path);
            }
         }
      }
   }

   @FunctionalInterface
   interface StatusSink {
      void set(String var1);
   }
}
