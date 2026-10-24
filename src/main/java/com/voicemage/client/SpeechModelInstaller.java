package com.voicemage.client;

import com.voicemage.Voicemage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.loader.api.FabricLoader;

final class SpeechModelInstaller {
   private static final String MODEL_NAME = "vosk-model-small-en-us-0.15";
   private static final URI MODEL_URI = URI.create("https://alphacephei.com/vosk/models/" + MODEL_NAME + ".zip");
   /** Size and SHA-256 of the official zip. A download that does not match is discarded and never extracted. */
   private static final long EXPECTED_SIZE = 41205931L;
   private static final String EXPECTED_SHA256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498";
   private static final long MAX_EXTRACTED_BYTES = 600L * 1024L * 1024L;
   private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
   private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(60);
   private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);
   private static final int ATTEMPTS = 2;

   private SpeechModelInstaller() {
   }

   static synchronized Path ensureInstalled(SpeechModelInstaller.StatusSink status) throws IOException, InterruptedException {
      Path parent = FabricLoader.getInstance().getGameDir().resolve("voicemage");
      Path model = parent.resolve(MODEL_NAME);
      if (valid(model)) {
         return model;
      }

      Files.createDirectories(parent);
      deleteTree(model);
      Path archive = parent.resolve(MODEL_NAME + ".zip.part");
      Path installing = parent.resolve(MODEL_NAME + ".installing");
      Files.deleteIfExists(archive);
      deleteTree(installing);
      Files.createDirectories(installing);

      try {
         download(archive, status);
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

   private static void download(Path archive, SpeechModelInstaller.StatusSink status) throws IOException, InterruptedException {
      IOException lastFailure = null;

      for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
         try {
            downloadOnce(archive, status);
            return;
         } catch (SpeechModelInstaller.IntegrityException integrity) {
            throw integrity;
         } catch (IOException failure) {
            lastFailure = failure;
            Voicemage.LOGGER.warn("Speech model download failed (attempt {} of {})", attempt, ATTEMPTS, failure);
            Files.deleteIfExists(archive);
            if (attempt < ATTEMPTS) {
               status.set("Download failed, retrying...");
               Thread.sleep(2000L);
            }
         }
      }

      throw lastFailure;
   }

   private static void downloadOnce(Path archive, SpeechModelInstaller.StatusSink status) throws IOException, InterruptedException {
      status.set("Downloading speech model (one time, ~40 MB)...");
      HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).followRedirects(Redirect.NORMAL).build();
      HttpRequest request = HttpRequest.newBuilder(MODEL_URI).timeout(RESPONSE_TIMEOUT).header("User-Agent", "voicemage/2.14").build();
      HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
      if (response.statusCode() / 100 != 2) {
         response.body().close();
         throw new IOException("Model download returned HTTP " + response.statusCode());
      }

      MessageDigest digest = newDigest();
      long total = 0L;
      long nextReport = 2L * 1024L * 1024L;
      long startedNanos = System.nanoTime();

      try (
         InputStream input = response.body();
         OutputStream output = Files.newOutputStream(archive);
      ) {
         byte[] buffer = new byte[65536];

         int read;
         while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > EXPECTED_SIZE + 1048576L) {
               throw new SpeechModelInstaller.IntegrityException("Speech model download is larger than expected; it was discarded");
            }

            if (System.nanoTime() - startedNanos > DOWNLOAD_TIMEOUT.toNanos()) {
               throw new IOException("Speech model download took too long");
            }

            digest.update(buffer, 0, read);
            output.write(buffer, 0, read);
            if (total >= nextReport) {
               nextReport += 2L * 1024L * 1024L;
               status.set("Downloading speech model... " + total / 1048576L + " / " + EXPECTED_SIZE / 1048576L + " MB");
            }
         }
      }

      String actual = HexFormat.of().formatHex(digest.digest());
      if (total != EXPECTED_SIZE || !actual.equalsIgnoreCase(EXPECTED_SHA256)) {
         Voicemage.LOGGER.error("Speech model failed verification: expected {} bytes / SHA-256 {}, got {} bytes / SHA-256 {}", EXPECTED_SIZE, EXPECTED_SHA256, total, actual);
         throw new SpeechModelInstaller.IntegrityException("Speech model failed its checksum; the download was discarded");
      }
   }

   private static MessageDigest newDigest() {
      try {
         return MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException impossible) {
         throw new IllegalStateException("SHA-256 is not available", impossible);
      }
   }

   private static void extractModel(Path archive, Path destination) throws IOException {
      long extracted = 0L;
      ZipEntry entry;
      try (
         InputStream input = Files.newInputStream(archive);
         ZipInputStream zip = new ZipInputStream(input);
      ) {
         while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName().replace('\\', '/');
            String prefix = MODEL_NAME + "/";
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

                     try (OutputStream out = Files.newOutputStream(output)) {
                        byte[] buffer = new byte[65536];

                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                           extracted += read;
                           if (extracted > MAX_EXTRACTED_BYTES) {
                              throw new IOException("Model archive is unexpectedly large");
                           }

                           out.write(buffer, 0, read);
                        }
                     }
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

   /** A failed integrity check is final: retrying the same download would not help. */
   private static final class IntegrityException extends IOException {
      private static final long serialVersionUID = 1L;

      private IntegrityException(String message) {
         super(message);
      }
   }

   @FunctionalInterface
   interface StatusSink {
      void set(String var1);
   }
}
