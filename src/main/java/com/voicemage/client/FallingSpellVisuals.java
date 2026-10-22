package com.voicemage.client;

import com.voicemage.network.FallingSpellPayload;
import com.voicemage.network.SpellVisualPayload;
import com.voicemage.spell.Spell;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;

public final class FallingSpellVisuals {
   private static final Map<Integer, FallingSpellVisuals.Body> BODIES = new HashMap<>();
   private static int nextEntityId = Integer.MIN_VALUE;

   public static void accept(Minecraft client, FallingSpellPayload payload) {
      if (client.level != null && payload.duration() > 0) {
         if (payload.spellId() == Spell.METEOR.networkId() || payload.spellId() == Spell.EXPLOSION.networkId()) {
            FallingSpellVisuals.Body previous = BODIES.remove(payload.id());
            if (previous != null) {
               previous.discard();
            }

            FallingSpellVisuals.Body body = new FallingSpellVisuals.Body(client.level, payload);
            body.setId(nextEntityId++);
            BODIES.put(payload.id(), body);
            client.level.addEntity(body);
         }
      }
   }

   public static void impact(SpellVisualPayload payload) {
      if (payload.phaseTicks() == 0) {
         FallingSpellVisuals.Body body = BODIES.remove(payload.entityId());
         if (body != null) {
            body.discard();
         }
      }
   }

   public static void tick(Minecraft client) {
      if (client.level == null) {
         clear();
      } else {
         BODIES.values().removeIf(body -> {
            if (body.level() != client.level) {
               body.discard();
            }

            return body.isRemoved();
         });
      }
   }

   public static void clear() {
      BODIES.values().forEach(Entity::discard);
      BODIES.clear();
   }

   private static final class Body extends LargeFireball {
      private final FallingSpellPayload path;
      private int elapsed;

      private Body(ClientLevel level, FallingSpellPayload path) {
         super(EntityTypes.FIREBALL, level);
         this.path = path;
         this.elapsed = Math.clamp(level.getGameTime() - path.started(), 0, path.duration());
         this.noPhysics = true;
         this.setNoGravity(true);
         this.place();
         this.setOldPosAndRot();
      }

      private void place() {
         this.setPos(
            this.path.x(),
            this.path.y() + this.path.height() * (1.0 - (double)Math.min(this.elapsed, this.path.duration()) / this.path.duration()),
            this.path.z()
         );
      }

      public void tick() {
         this.setOldPosAndRot();
         this.elapsed++;
         this.place();
         if (this.elapsed > this.path.duration() + 40) {
            this.discard();
         } else if (this.elapsed <= this.path.duration()) {
            boolean nuke = this.path.spellId() == Spell.EXPLOSION.networkId();
            ClientLevel level = (ClientLevel)this.level();
            int count = nuke ? 10 : 5;
            double radius = nuke ? 1.8 : 0.85;

            for (int i = 0; i < count; i++) {
               double angle = this.elapsed * 0.18 + i * Math.PI * 2.0 / count;
               level.addParticle(
                  ParticleTypes.FLAME,
                  this.getX() + Math.cos(angle) * radius,
                  this.getY() + Math.sin(angle * 2.0) * radius * 0.5,
                  this.getZ() + Math.sin(angle) * radius,
                  0.0,
                  0.04,
                  0.0
               );
            }

            level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + radius, this.getZ(), 0.0, 0.1, 0.0);
         }
      }
   }
}
