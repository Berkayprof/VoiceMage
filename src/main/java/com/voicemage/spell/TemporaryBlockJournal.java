package com.voicemage.spell;

import com.google.gson.Gson;
import com.voicemage.Voicemage;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Remembers which temporary blocks (earth walls, ice cages) are in the world, so that they can still be removed after a
 * crash. The file is deleted on a clean shutdown, when everything has been restored.
 */
final class TemporaryBlockJournal {
   private static final Gson GSON = new Gson();

   private TemporaryBlockJournal() {
   }

   static final class Entry {
      String dimension;
      int x;
      int y;
      int z;
      String block;

      Entry() {
      }

      Entry(String dimension, int x, int y, int z, String block) {
         this.dimension = dimension;
         this.x = x;
         this.y = y;
         this.z = z;
         this.block = block;
      }
   }

   private static Path file(MinecraftServer server) {
      return server.getWorldPath(LevelResource.ROOT).resolve("voicemage_temporary_blocks.json");
   }

   static void write(MinecraftServer server, List<Entry> entries) {
      Path file = file(server);

      try {
         if (entries.isEmpty()) {
            Files.deleteIfExists(file);
            return;
         }

         Path temporary = file.resolveSibling(file.getFileName() + ".tmp");

         try (Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(entries.toArray(new Entry[0]), Entry[].class, writer);
         }

         try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException exception) {
         Voicemage.LOGGER.warn("Could not write {}", file, exception);
      }
   }

   static List<Entry> read(MinecraftServer server) {
      Path file = file(server);
      if (!Files.isRegularFile(file)) {
         return List.of();
      }

      try (Reader reader = Files.newBufferedReader(file)) {
         Entry[] entries = GSON.fromJson(reader, Entry[].class);
         return entries == null ? List.of() : List.of(entries);
      } catch (IOException | RuntimeException exception) {
         Voicemage.LOGGER.warn("Could not read {}", file, exception);
         return List.of();
      }
   }

   static void delete(MinecraftServer server) {
      try {
         Files.deleteIfExists(file(server));
      } catch (IOException ignored) {
      }
   }
}
