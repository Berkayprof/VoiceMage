package com.voicemage.spell;

import com.voicemage.Voicemage;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

/**
 * Clones and polymorph forms only exist for a few seconds. They are tagged so that, if one of them is ever saved with
 * its chunk (chunk unload, crash, kill -9), it is removed again as soon as it is loaded without an active spell.
 */
public final class TemporaryEntities {
   private static final String TAG = "voicemage_temporary";
   private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

   private TemporaryEntities() {
   }

   static void track(Entity entity) {
      entity.addTag(TAG);
      ACTIVE.add(entity.getUUID());
   }

   static void untrack(Entity entity) {
      ACTIVE.remove(entity.getUUID());
   }

   static void clear() {
      ACTIVE.clear();
   }

   /** Called for every entity that is loaded into a level. */
   public static void discardIfStray(Entity entity, MinecraftServer server) {
      // removeTag() returns true only if the entity had the tag, so this works without reading the tag set.
      if (!entity.removeTag(TAG)) {
         return;
      }

      if (ACTIVE.contains(entity.getUUID())) {
         entity.addTag(TAG);
         return;
      }

      server.execute(() -> {
         if (!entity.isRemoved()) {
            Voicemage.LOGGER.info("Removed a leftover voicemage entity ({})", entity.getUUID());
            entity.discard();
         }
      });
   }
}
