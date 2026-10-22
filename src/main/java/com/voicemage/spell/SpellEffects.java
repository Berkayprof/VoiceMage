package com.voicemage.spell;

import com.voicemage.VoicemageNetworking;
import com.voicemage.config.VoicemageConfig;
import com.voicemage.mixin.MannequinAccessor;
import com.voicemage.network.FallingSpellPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class SpellEffects {
   private static final Identifier SHRINK_MODIFIER = Identifier.fromNamespaceAndPath("voicemage", "shrink");
   private final Map<SpellEffects.TemporaryBlock, Long> temporaryBlocks = new HashMap<>();
   private final List<SpellEffects.FallingSpellTask> fallingSpells = new ArrayList<>();
   private int nextFallingId = -1;
   private final List<SpellEffects.ImpactVisualTask> impactVisuals = new ArrayList<>();
   private final List<SpellEffects.HealingVisualTask> healingVisuals = new ArrayList<>();
   private final List<SpellEffects.CraterTask> craters = new ArrayList<>();
   private final List<SpellEffects.CloneTask> clones = new ArrayList<>();
   private final List<SpellEffects.PolymorphTask> polymorphs = new ArrayList<>();
   private final List<SpellEffects.ShrinkTask> shrinks = new ArrayList<>();

   boolean cast(ServerPlayer player, Spell spell) {
      return switch (spell) {
         case FIREBALL -> this.fireball(player);
         case PITFALL -> this.pitfall(player);
         case HEAL -> this.heal(player);
         case LIGHTNING -> this.lightning(player);
         case EARTH_WALL -> this.earthWall(player);
         case BLINK -> this.blink(player);
         case MAGIC_MISSILE -> this.magicMissile(player);
         case METEOR -> this.meteor(player);
         case WIND_BLAST -> this.windBlast(player);
         case FREEZE -> this.freeze(player);
         case CLONE -> this.cloneSpell(player);
         case POLYMORPH -> this.polymorph(player);
         case SHRINK -> this.shrink(player);
         case EXPLOSION -> this.explosion(player);
      };
   }

   void tick(MinecraftServer server) {
      this.tickTemporaryBlocks(server);
      this.tickFallingSpells(server);
      this.tickCraters(server);
      this.tickImpactVisuals(server);
      this.tickHealingVisuals(server);
      this.tickClones();
      this.tickPolymorphs();
      this.tickShrinks();
   }

   void restoreAll(MinecraftServer server) {
      for (SpellEffects.TemporaryBlock temporary : this.temporaryBlocks.keySet()) {
         removeTemporary(server, temporary);
      }

      this.temporaryBlocks.clear();
      this.clones.forEach(task -> task.clone.discard());
      this.clones.clear();
      this.polymorphs.forEach(SpellEffects.PolymorphTask::finish);
      this.polymorphs.clear();
      this.shrinks.forEach(SpellEffects.ShrinkTask::finish);
      this.shrinks.clear();
      this.fallingSpells.clear();
      this.impactVisuals.clear();
      this.healingVisuals.clear();
      this.craters.clear();
   }

   private void tickTemporaryBlocks(MinecraftServer server) {
      Iterator<Entry<SpellEffects.TemporaryBlock, Long>> iterator = this.temporaryBlocks.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<SpellEffects.TemporaryBlock, Long> entry = iterator.next();
         ServerLevel level = server.getLevel(entry.getKey().dimension());
         if (level != null && level.getGameTime() >= entry.getValue()) {
            removeTemporary(server, entry.getKey());
            iterator.remove();
         }
      }
   }

   private static void removeTemporary(MinecraftServer server, SpellEffects.TemporaryBlock temporary) {
      ServerLevel level = server.getLevel(temporary.dimension());
      if (level != null && level.getBlockState(temporary.pos()).is(temporary.expected())) {
         level.setBlock(temporary.pos(), Blocks.AIR.defaultBlockState(), 3);
      }
   }

   private void tickFallingSpells(MinecraftServer server) {
      Iterator<SpellEffects.FallingSpellTask> iterator = this.fallingSpells.iterator();

      while (iterator.hasNext()) {
         SpellEffects.FallingSpellTask task = iterator.next();
         ServerLevel level = server.getLevel(task.dimension);
         if (level == null) {
            iterator.remove();
         } else {
            long remaining = task.impactTick - level.getGameTime();
            renderGroundSigil(level, task.target, task.cataclysm);
            if (remaining <= 0L) {
               Entity caster = level.getEntity(task.caster);
               level.explode(caster, task.target.x, task.target.y, task.target.z, task.power, true, ExplosionInteraction.TNT);
               VoicemageNetworking.visual(
                  level, (task.cataclysm ? Spell.EXPLOSION : Spell.METEOR).networkId(), task.target.x, task.target.y, task.target.z, 0, 10, task.id, task.power
               );
               if (task.cataclysm) {
                  this.craters
                     .add(
                        new SpellEffects.CraterTask(
                           task.dimension,
                           BlockPos.containing(task.target),
                           VoicemageConfig.INSTANCE.explosionCraterDepth,
                           VoicemageConfig.INSTANCE.explosionCraterRadius
                        )
                     );
               }

               renderImpactBurst(level, task.target, task.cataclysm);
               this.impactVisuals
                  .add(
                     new SpellEffects.ImpactVisualTask(
                        task.dimension, task.target, level.getGameTime(), level.getGameTime() + (task.cataclysm ? 70 : 45), task.cataclysm
                     )
                  );
               iterator.remove();
            }
         }
      }
   }

   private static void renderGroundSigil(ServerLevel level, Vec3 center, boolean cataclysm) {
      long interval = cataclysm ? 2L : 4L;
      if (level.getGameTime() % interval == 0L) {
         int rings = cataclysm ? 4 : 2;

         for (int ring = 1; ring <= rings; ring++) {
            double radius = ring * (cataclysm ? 2.2 : 1.35);
            int points = cataclysm ? 8 + ring * 6 : 6 + ring * 4;

            for (int i = 0; i < points; i++) {
               double angle = (Math.PI * 2) * i / points + level.getGameTime() * 0.035 * (ring % 2 == 0 ? -1 : 1);
               double x = center.x + Math.cos(angle) * radius;
               double z = center.z + Math.sin(angle) * radius;
               level.sendParticles(
                  cataclysm && ring % 2 == 0 ? ParticleTypes.WITCH : (ring % 2 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME),
                  x,
                  center.y + 0.12,
                  z,
                  1,
                  0.0,
                  0.0,
                  0.0,
                  0.0
               );
            }
         }

         double outer = rings * (cataclysm ? 2.2 : 1.35);
         int spokes = cataclysm ? 12 : 6;

         for (int spoke = 0; spoke < spokes; spoke++) {
            double angle = (Math.PI * 2) * spoke / spokes + level.getGameTime() * 0.012;

            for (double distance = 0.8; distance < outer; distance += cataclysm ? 0.85 : 1.2) {
               level.sendParticles(
                  cataclysm ? ParticleTypes.FLAME : ParticleTypes.ENCHANT,
                  center.x + Math.cos(angle) * distance,
                  center.y + 0.1,
                  center.z + Math.sin(angle) * distance,
                  1,
                  0.0,
                  0.0,
                  0.0,
                  0.0
               );
            }
         }

         level.sendParticles(
            ParticleTypes.END_ROD, center.x, center.y + 0.25, center.z, cataclysm ? 10 : 3, cataclysm ? 3.5 : 1.4, 0.05, cataclysm ? 3.5 : 1.4, 0.0
         );
      }
   }

   private void tickCraters(MinecraftServer server) {
      Iterator<SpellEffects.CraterTask> iterator = this.craters.iterator();

      while (iterator.hasNext()) {
         SpellEffects.CraterTask task = iterator.next();
         ServerLevel level = server.getLevel(task.dimension);
         if (level == null) {
            iterator.remove();
         } else {
            int layer = 0;

            while (true) {
               if (layer < 2 && task.depth < task.maximumDepth) {
                  int y = task.center.getY() - task.depth;
                  if (y >= level.getMinY()) {
                     double progress = (double)task.depth / Math.max(1, task.maximumDepth - 1);
                     double radius = 4.0 + (task.topRadius - 4.0) * Math.pow(1.0 - progress, 0.72);
                     int extent = (int)Math.ceil(radius);
                     double radiusSquared = radius * radius;

                     for (int x = -extent; x <= extent; x++) {
                        for (int z = -extent; z <= extent; z++) {
                           double roughness = ((x * 73428767L ^ z * 912931L ^ y * 42349L) & 7L) * 0.11;
                           if (!(x * x + z * z > radiusSquared - roughness)) {
                              BlockPos pos = new BlockPos(task.center.getX() + x, y, task.center.getZ() + z);
                              if (!level.getBlockState(pos).isAir()) {
                                 level.setBlock(pos, Blocks.AIR.defaultBlockState(), 290);
                              }
                           }
                        }
                     }

                     task.depth++;
                     layer++;
                     continue;
                  }

                  task.depth = task.maximumDepth;
               }

               if (task.depth >= task.maximumDepth) {
                  iterator.remove();
               }
               break;
            }
         }
      }
   }

   private void tickImpactVisuals(MinecraftServer server) {
      Iterator<SpellEffects.ImpactVisualTask> iterator = this.impactVisuals.iterator();

      while (iterator.hasNext()) {
         SpellEffects.ImpactVisualTask task = iterator.next();
         ServerLevel level = server.getLevel(task.dimension);
         if (level != null && level.getGameTime() < task.expires) {
            long age = level.getGameTime() - task.started;
            if (age < 18L) {
               double radius = (age + 1L) * (task.cataclysm ? 4.2 : 0.8);
               int points = task.cataclysm ? 52 : 30;

               for (int i = 0; i < points; i++) {
                  double angle = (Math.PI * 2) * i / points;
                  level.sendParticles(
                     age % 3L == 0L ? ParticleTypes.FLAME : ParticleTypes.CLOUD,
                     task.target.x + Math.cos(angle) * radius,
                     task.target.y + 0.35,
                     task.target.z + Math.sin(angle) * radius,
                     1,
                     0.0,
                     task.cataclysm ? 0.12 : 0.05,
                     0.0,
                     0.02
                  );
               }
            }

            if (age % 2L == 0L) {
               double spread = task.cataclysm ? 14.0 : 3.5;
               double height = Math.min(age * (task.cataclysm ? 1.0 : 0.4), task.cataclysm ? 38.0 : 12.0);
               level.sendParticles(
                  ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                  task.target.x,
                  task.target.y + 1.0 + height,
                  task.target.z,
                  task.cataclysm ? 20 : 8,
                  spread,
                  1.4,
                  spread,
                  0.035
               );
               level.sendParticles(
                  ParticleTypes.FLAME, task.target.x, task.target.y + 0.5, task.target.z, task.cataclysm ? 28 : 12, spread * 0.8, 0.8, spread * 0.8, 0.04
               );
            }

            if (task.cataclysm && age < 30L && age % 5L == 0L) {
               level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, task.target.x, task.target.y + 2.0, task.target.z, 3, 16.0, 5.0, 16.0, 0.0);
            }
         } else {
            iterator.remove();
         }
      }
   }

   private void tickHealingVisuals(MinecraftServer server) {
      Iterator<SpellEffects.HealingVisualTask> iterator = this.healingVisuals.iterator();

      while (iterator.hasNext()) {
         SpellEffects.HealingVisualTask task = iterator.next();
         ServerLevel level = server.getLevel(task.dimension);
         if ((level == null ? null : level.getEntity(task.player)) instanceof ServerPlayer player && level.getGameTime() < task.expires) {
            long age = level.getGameTime() - task.started;
            double phase = age * 0.38;

            for (int strand = 0; strand < 3; strand++) {
               double angle = phase + strand * Math.PI * 2.0 / 3.0;
               double radius = 0.72 - age * 0.008;
               double y = player.getY() + 0.12 + age % 30L * 0.075;
               level.sendParticles(
                  strand == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD,
                  player.getX() + Math.cos(angle) * radius,
                  y,
                  player.getZ() + Math.sin(angle) * radius,
                  1,
                  0.0,
                  0.025,
                  0.0,
                  0.0
               );
            }

            if (age % 3L == 0L) {
               double ringRadius = 1.05 - Math.min(age, 24L) * 0.025;

               for (int i = 0; i < 12; i++) {
                  double angle = (Math.PI * 2) * i / 12.0 - phase * 0.35;
                  level.sendParticles(
                     i % 2 == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD,
                     player.getX() + Math.cos(angle) * ringRadius,
                     player.getY() + 0.08,
                     player.getZ() + Math.sin(angle) * ringRadius,
                     1,
                     0.0,
                     0.02,
                     0.0,
                     0.0
                  );
               }
            }

            if (age == 24L) {
               level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.55, 0.95, 0.55, 0.08);
               level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.55, 0.95, 0.55, 0.06);
               level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.65F);
            }
         } else {
            iterator.remove();
         }
      }
   }

   private static void renderImpactBurst(ServerLevel level, Vec3 center, boolean cataclysm) {
      level.sendParticles(
         ParticleTypes.END_ROD,
         center.x,
         center.y + 1.0,
         center.z,
         cataclysm ? 80 : 28,
         cataclysm ? 6.0 : 2.0,
         cataclysm ? 3.0 : 1.0,
         cataclysm ? 6.0 : 2.0,
         cataclysm ? 0.25 : 0.12
      );
      level.sendParticles(
         ParticleTypes.EXPLOSION_EMITTER,
         center.x,
         center.y + 1.0,
         center.z,
         cataclysm ? 12 : 3,
         cataclysm ? 15.0 : 2.5,
         cataclysm ? 6.0 : 1.5,
         cataclysm ? 15.0 : 2.5,
         0.0
      );
      level.sendParticles(
         ParticleTypes.GUST_EMITTER_LARGE, center.x, center.y + 0.5, center.z, cataclysm ? 5 : 2, cataclysm ? 5.0 : 1.5, 0.5, cataclysm ? 5.0 : 1.5, 0.0
      );
      level.sendParticles(
         ParticleTypes.LAVA,
         center.x,
         center.y + 1.0,
         center.z,
         cataclysm ? 320 : 65,
         cataclysm ? 20.0 : 4.0,
         cataclysm ? 12.0 : 3.0,
         cataclysm ? 20.0 : 4.0,
         cataclysm ? 0.24 : 0.1
      );
      level.playSound(
         null,
         BlockPos.containing(center),
         (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(),
         SoundSource.PLAYERS,
         cataclysm ? 4.0F : 2.2F,
         cataclysm ? 0.35F : 0.55F
      );
   }

   private void tickClones() {
      Iterator<SpellEffects.CloneTask> iterator = this.clones.iterator();

      while (iterator.hasNext()) {
         SpellEffects.CloneTask task = iterator.next();
         Mannequin clone = task.clone;
         if (!clone.isRemoved() && clone.level().getGameTime() < task.expires) {
            task.age++;
            if (--task.turnCountdown <= 0 && clone.onGround()) {
               double wander = (clone.level().getRandom().nextDouble() - 0.5) * Math.toRadians(76.0);
               task.direction = rotateY(task.direction, wander);
               task.turnCountdown = 18 + clone.level().getRandom().nextInt(38);
            }

            task.direction = rotateY(task.direction, Math.sin((task.age + clone.getId() * 5) * 0.11) * 0.0065);
            Vec3 horizontal = task.direction.scale(0.21);
            double vertical = clone.getDeltaMovement().y;
            if (!clone.onGround()) {
               vertical = Math.max(-3.92, vertical - 0.08);
            } else if (vertical < 0.0) {
               vertical = 0.0;
            }

            boolean blocked = !clone.level().noCollision(clone, clone.getBoundingBox().move(horizontal));
            if (blocked && clone.onGround() && clone.level().noCollision(clone, clone.getBoundingBox().move(horizontal.x, 0.72, horizontal.z))) {
               vertical = 0.42;
            } else if (blocked) {
               double hardTurn = Math.toRadians(80.0 + clone.level().getRandom().nextDouble() * 100.0);
               if (clone.level().getRandom().nextBoolean()) {
                  hardTurn = -hardTurn;
               }

               task.direction = rotateY(task.direction, hardTurn);
               task.turnCountdown = 10 + clone.level().getRandom().nextInt(18);
               horizontal = task.direction.scale(0.21);
            }

            clone.move(MoverType.SELF, new Vec3(horizontal.x, vertical, horizontal.z));
            if (clone.verticalCollision) {
               vertical = 0.0;
            }

            boolean supported = !clone.level().noCollision(clone, clone.getBoundingBox().move(0.0, -0.09, 0.0));
            if (!supported && Math.abs(vertical) < 0.001) {
               vertical = -0.08;
            }

            clone.setDeltaMovement(0.0, vertical * 0.98, 0.0);
            clone.hurtMarked = true;
            float facing = (float)Math.toDegrees(Math.atan2(-task.direction.x, task.direction.z));
            clone.setYRot(facing);
            clone.setYHeadRot(facing);
            clone.setYBodyRot(facing);
         } else {
            clone.discard();
            iterator.remove();
         }
      }
   }

   private void tickPolymorphs() {
      Iterator<SpellEffects.PolymorphTask> iterator = this.polymorphs.iterator();

      while (iterator.hasNext()) {
         SpellEffects.PolymorphTask task = iterator.next();
         if (!task.target.isRemoved() && !task.form.isRemoved() && task.target.level().getGameTime() < task.expires) {
            task.form.setPos(task.target.position());
            task.form.setDeltaMovement(task.target.getDeltaMovement());
            task.form.setYRot(task.target.getYRot());
            task.form.hurtMarked = true;
         } else {
            task.finish();
            iterator.remove();
         }
      }
   }

   private void tickShrinks() {
      Iterator<SpellEffects.ShrinkTask> iterator = this.shrinks.iterator();

      while (iterator.hasNext()) {
         SpellEffects.ShrinkTask task = iterator.next();
         if (task.target.isRemoved() || task.target.level().getGameTime() >= task.expires) {
            task.finish();
            iterator.remove();
         }
      }
   }

   private boolean fireball(ServerPlayer player) {
      ServerLevel level = player.level();
      Vec3 look = player.getLookAngle().normalize();
      LargeFireball fireball = new LargeFireball(level, player, look, VoicemageConfig.INSTANCE.fireballExplosionPower);
      fireball.setPos(player.getEyePosition().add(look.scale(1.4)));
      level.addFreshEntity(fireball);
      level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
      return true;
   }

   private boolean pitfall(ServerPlayer player) {
      if (!VoicemageConfig.INSTANCE.pitfallEnabled) {
         return false;
      }

      ServerLevel level = player.level();
      Vec3 flatLook = flatLook(player);
      BlockPos center = player.blockPosition()
         .offset(
            (int)Math.round(flatLook.x * VoicemageConfig.INSTANCE.pitfallForwardDistance),
            -1,
            (int)Math.round(flatLook.z * VoicemageConfig.INSTANCE.pitfallForwardDistance)
         );
      BlockState air = Blocks.AIR.defaultBlockState();
      int minY = level.getMinY();
      int half = VoicemageConfig.INSTANCE.pitfallSize / 2;
      int removed = 0;

      for (int x = -half; x <= half; x++) {
         for (int z = -half; z <= half; z++) {
            for (int y = center.getY(); y >= minY; y--) {
               BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
               if (!level.getBlockState(pos).isAir()) {
                  level.setBlock(pos, air, 2);
                  removed++;
               }
            }
         }
      }

      level.playSound(null, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.65F);
      return removed > 0;
   }

   private boolean heal(ServerPlayer player) {
      if (player.getHealth() >= player.getMaxHealth()) {
         return false;
      }

      player.heal((float)VoicemageConfig.INSTANCE.healAmount);
      player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 35, 0, false, false));
      ServerLevel level = player.level();
      long now = level.getGameTime();
      this.healingVisuals.removeIf(task -> task.player.equals(player.getUUID()));
      this.healingVisuals.add(new SpellEffects.HealingVisualTask(level.dimension(), player.getUUID(), now, now + 32L));
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.1, player.getZ(), 18, 0.65, 0.8, 0.65, 0.06);
      level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.55, 0.9, 0.55, 0.045);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.15, player.getZ(), 24, 0.75, 0.08, 0.75, 0.02);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.45F);
      return true;
   }

   private boolean lightning(ServerPlayer player) {
      ServerLevel level = player.level();
      HitResult hit = aimedHit(player, VoicemageConfig.INSTANCE.lightningRange, true);
      Vec3 strikeAt = hit.getLocation();
      Entity target = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
      if (target instanceof Player targetPlayer && !player.canHarmPlayer(targetPlayer)) {
         target = null;
      }

      LightningBolt bolt = (LightningBolt)EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
      if (bolt == null) {
         return false;
      }

      bolt.setVisualOnly(true);
      bolt.setCause(player);
      bolt.setPos(strikeAt.x, strikeAt.y, strikeAt.z);
      level.addFreshEntity(bolt);
      if (target != null) {
         target.hurtServer(level, level.damageSources().lightningBolt(), (float)VoicemageConfig.INSTANCE.lightningDamage);
      }

      return true;
   }

   private boolean earthWall(ServerPlayer player) {
      ServerLevel level = player.level();
      Vec3 flatLook = flatLook(player);
      BlockPos base = player.blockPosition().offset((int)Math.round(flatLook.x * 3.0), 0, (int)Math.round(flatLook.z * 3.0));
      boolean wallRunsAlongZ = Math.abs(flatLook.x) >= Math.abs(flatLook.z);
      long expires = level.getGameTime() + VoicemageConfig.INSTANCE.earthWallLifetimeSeconds * 20L;
      int placed = 0;
      int halfWidth = VoicemageConfig.INSTANCE.earthWallWidth / 2;

      for (int horizontal = -halfWidth; horizontal <= halfWidth; horizontal++) {
         for (int y = 0; y < VoicemageConfig.INSTANCE.earthWallHeight; y++) {
            BlockPos pos = base.offset(wallRunsAlongZ ? 0 : horizontal, y, wallRunsAlongZ ? horizontal : 0);
            if (this.placeTemporary(level, pos, Blocks.PACKED_MUD.defaultBlockState(), expires)) {
               placed++;
            }
         }
      }

      level.sendParticles(
         ParticleTypes.CAMPFIRE_COSY_SMOKE,
         base.getX() + 0.5,
         base.getY() + 1.5,
         base.getZ() + 0.5,
         32,
         wallRunsAlongZ ? 0.25 : 2.2,
         1.5,
         wallRunsAlongZ ? 2.2 : 0.25,
         0.02
      );
      level.playSound(null, base, SoundEvents.GRAVEL_PLACE, SoundSource.PLAYERS, 1.2F, 0.7F);
      return placed > 0;
   }

   private boolean blink(ServerPlayer player) {
      ServerLevel level = player.level();
      Vec3 start = player.position();
      Vec3 look = player.getLookAngle().normalize();
      Vec3 eye = player.getEyePosition();
      double range = VoicemageConfig.INSTANCE.blinkRange;
      HitResult obstruction = level.clip(new ClipContext(eye, eye.add(look.scale(range)), Block.COLLIDER, Fluid.NONE, player));
      double maximum = obstruction.getType() == Type.MISS ? range : Math.max(1.5, eye.distanceTo(obstruction.getLocation()) - 0.8);

      for (double distance = maximum; distance >= 1.5; distance -= 0.35) {
         Vec3 candidate = start.add(look.scale(distance));
         AABB moved = player.getBoundingBox().move(candidate.subtract(start));
         if (level.noCollision(player, moved)) {
            Vec3 old = player.position();
            player.teleportTo(candidate.x, candidate.y, candidate.z);
            player.resetFallDistance();
            level.sendParticles(ParticleTypes.PORTAL, old.x, old.y + 1.0, old.z, 40, 0.45, 0.8, 0.45, 0.2);
            level.sendParticles(ParticleTypes.PORTAL, candidate.x, candidate.y + 1.0, candidate.z, 40, 0.45, 0.8, 0.45, 0.2);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.15F);
            return true;
         }
      }

      return false;
   }

   private boolean magicMissile(ServerPlayer player) {
      ServerLevel level = player.level();
      HitResult hit = aimedHit(player, VoicemageConfig.INSTANCE.magicMissileRange, true);
      Vec3 start = player.getEyePosition();
      Vec3 end = hit.getLocation();
      Vec3 ray = end.subtract(start);
      double length = ray.length();
      Vec3 direction = ray.normalize();

      for (double distance = 1.0; distance < length; distance++) {
         Vec3 point = start.add(direction.scale(distance));
         level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 1.15F);
      if (hit instanceof EntityHitResult entityHit) {
         Entity target = entityHit.getEntity();
         if (!(target instanceof Player targetPlayer && !player.canHarmPlayer(targetPlayer))) {
            target.hurtServer(level, level.damageSources().sonicBoom(player), (float)VoicemageConfig.INSTANCE.magicMissileDamage);
            target.push(direction.x * 1.8, 0.35, direction.z * 1.8);
            target.hurtMarked = true;
         }
      }

      return true;
   }

   private boolean meteor(ServerPlayer player) {
      HitResult hit = aimedHit(player, VoicemageConfig.INSTANCE.meteorRange, true);
      Vec3 target = hit.getLocation().add(0.0, 0.15, 0.0);
      return this.spawnFallingSpell(player, target, VoicemageConfig.INSTANCE.meteorFallTicks, VoicemageConfig.INSTANCE.meteorExplosionPower, false);
   }

   private boolean windBlast(ServerPlayer player) {
      ServerLevel level = player.level();
      Vec3 look = player.getLookAngle().normalize();
      double range = VoicemageConfig.INSTANCE.windBlastRange;
      AABB area = player.getBoundingBox().expandTowards(look.scale(range)).inflate(range * 0.65);
      int hit = 0;

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive())) {
         if (!(target instanceof Player targetPlayer && !player.canHarmPlayer(targetPlayer))) {
            Vec3 offset = target.getEyePosition().subtract(player.getEyePosition());
            double distance = offset.length();
            if (!(distance > range) && !(distance < 0.01) && !(offset.normalize().dot(look) < 0.58) && player.hasLineOfSight(target)) {
               Vec3 away = target.position().subtract(player.position()).normalize();
               double strength = VoicemageConfig.INSTANCE.windBlastStrength * (1.15 - distance / range * 0.35);
               target.setDeltaMovement(target.getDeltaMovement().add(away.x * strength, 0.55, away.z * strength));
               target.hurtMarked = true;
               if (VoicemageConfig.INSTANCE.windBlastDamage > 0.0) {
                  target.hurtServer(level, level.damageSources().magic(), (float)VoicemageConfig.INSTANCE.windBlastDamage);
               }

               hit++;
            }
         }
      }

      for (int i = 1; i <= 12; i++) {
         Vec3 point = player.getEyePosition().add(look.scale(i * range / 12.0));
         level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 3, i * 0.07, i * 0.04, i * 0.07, 0.08);
      }

      level.playSound(null, player.blockPosition(), (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.4F, 0.8F);
      return hit > 0;
   }

   private boolean freeze(ServerPlayer player) {
      ServerLevel level = player.level();
      Vec3 center = aimedHit(player, VoicemageConfig.INSTANCE.skillShotRange, true).getLocation();
      double radius = VoicemageConfig.INSTANCE.freezeRadius;
      AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
      int duration = VoicemageConfig.INSTANCE.freezeDurationTicks;
      int frozen = 0;

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive())) {
         if (!(target instanceof Player targetPlayer) || player.canHarmPlayer(targetPlayer)) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 255, false, true), player);
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 4, false, true), player);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            long expires = level.getGameTime() + duration;
            BlockPos feet = target.blockPosition();

            for (int y = -1; y <= 3; y++) {
               for (int x = -1; x <= 1; x++) {
                  for (int z = -1; z <= 1; z++) {
                     boolean shell = y == -1 || y == 3 || Math.abs(x) == 1 || Math.abs(z) == 1;
                     if (shell) {
                        this.placeTemporary(level, feet.offset(x, y, z), Blocks.ICE.defaultBlockState(), expires);
                     }
                  }
               }
            }

            frozen++;
         }
      }

      level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.8, center.z, 70, radius, radius * 0.6, radius, 0.02);
      level.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2F, 1.6F);
      return frozen > 0;
   }

   private boolean cloneSpell(ServerPlayer player) {
      ServerLevel level = player.level();
      Vec3 base = flatLook(player);
      int count = VoicemageConfig.INSTANCE.cloneCount;
      long expires = level.getGameTime() + VoicemageConfig.INSTANCE.cloneDurationTicks;
      int spawned = 0;

      for (int i = 0; i < count; i++) {
         Mannequin clone = (Mannequin)EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
         if (clone != null) {
            double angle = count == 2 ? (i == 0 ? -55.0 : 55.0) : (i - 1) * 65.0;
            Vec3 direction = rotateY(base, Math.toRadians(angle));
            clone.setPos(player.position().add(direction.scale(0.8)));
            float facing = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
            clone.setYRot(facing);
            clone.setYHeadRot(facing);
            clone.setYBodyRot(facing);
            ((MannequinAccessor)clone).voicemage$setProfile(ResolvableProfile.createResolved(player.getGameProfile()));
            ((MannequinAccessor)clone).voicemage$setImmovable(false);

            for (EquipmentSlot slot : List.of(
               EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
            )) {
               clone.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }

            clone.setInvulnerable(true);
            clone.setSilent(true);
            clone.setNoGravity(false);
            level.addFreshEntity(clone);
            this.clones.add(new SpellEffects.CloneTask(clone, direction, expires, 8 + i * 7));
            spawned++;
         }
      }

      level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.7, 0.9, 0.7, 0.08);
      level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 1.1F);
      return spawned > 0;
   }

   private boolean polymorph(ServerPlayer player) {
      LivingEntity target = skillTarget(player, VoicemageConfig.INSTANCE.skillShotRange);
      if (target != null && !this.polymorphs.stream().anyMatch(task -> task.target == target)) {
         ServerLevel level = player.level();
         Mob form = randomPolymorphForm(level);
         if (form == null) {
            return false;
         }

         form.setPos(target.position());
         form.setCustomName(target.getName());
         form.setCustomNameVisible(true);
         form.setPersistenceRequired();
         form.setInvulnerable(true);
         form.setNoAi(true);
         level.addFreshEntity(form);
         boolean alreadyInvisible = target.getEffect(MobEffects.INVISIBILITY) != null;
         int duration = VoicemageConfig.INSTANCE.polymorphDurationTicks;
         target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false), player);
         this.polymorphs.add(new SpellEffects.PolymorphTask(target, form, level.getGameTime() + duration, alreadyInvisible));
         level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 45, 0.7, 0.9, 0.7, 0.08);
         level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.1F, 1.5F);
         return true;
      } else {
         return false;
      }
   }

   private static Mob randomPolymorphForm(ServerLevel level) {
      return (Mob)(switch (level.getRandom().nextInt(8)) {
         case 0 -> (Sheep)EntityTypes.SHEEP.create(level, EntitySpawnReason.TRIGGERED);
         case 1 -> (Chicken)EntityTypes.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
         case 2 -> (Pig)EntityTypes.PIG.create(level, EntitySpawnReason.TRIGGERED);
         case 3 -> (Cow)EntityTypes.COW.create(level, EntitySpawnReason.TRIGGERED);
         case 4 -> (Rabbit)EntityTypes.RABBIT.create(level, EntitySpawnReason.TRIGGERED);
         case 5 -> (Frog)EntityTypes.FROG.create(level, EntitySpawnReason.TRIGGERED);
         case 6 -> (Slime)EntityTypes.SLIME.create(level, EntitySpawnReason.TRIGGERED);
         default -> (Bee)EntityTypes.BEE.create(level, EntitySpawnReason.TRIGGERED);
      });
   }

   private boolean shrink(ServerPlayer player) {
      LivingEntity target = skillTarget(player, VoicemageConfig.INSTANCE.skillShotRange);
      if (target == null) {
         return false;
      }

      AttributeInstance scale = target.getAttribute(Attributes.SCALE);
      if (scale == null) {
         return false;
      }

      this.shrinks.removeIf(task -> task.target == target);
      scale.addOrUpdateTransientModifier(new AttributeModifier(SHRINK_MODIFIER, VoicemageConfig.INSTANCE.shrinkScale - 1.0, Operation.ADD_MULTIPLIED_BASE));
      this.shrinks.add(new SpellEffects.ShrinkTask(target, target.level().getGameTime() + VoicemageConfig.INSTANCE.shrinkDurationTicks));
      player.level().sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 0.7, target.getZ(), 35, 0.5, 0.7, 0.5, 0.06);
      player.level().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);
      return true;
   }

   private boolean explosion(ServerPlayer player) {
      ServerLevel level = player.level();
      Vec3 target = aimedHit(player, VoicemageConfig.INSTANCE.explosionRange, true).getLocation().add(0.0, 0.15, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y + 1.0, target.z, 1, 0.0, 0.0, 0.0, 0.0);
      return this.spawnFallingSpell(player, target, VoicemageConfig.INSTANCE.explosionFallTicks, VoicemageConfig.INSTANCE.explosionPower, true);
   }

   private boolean spawnFallingSpell(ServerPlayer player, Vec3 target, int fallTicks, float power, boolean cataclysm) {
      ServerLevel level = player.level();
      double height = cataclysm ? VoicemageConfig.INSTANCE.explosionFallHeight : 56.0;
      int id = this.nextFallingId--;
      this.fallingSpells
         .add(new SpellEffects.FallingSpellTask(level.dimension(), target, player.getUUID(), level.getGameTime() + fallTicks, id, power, cataclysm));
      VoicemageNetworking.falling(
         level,
         new FallingSpellPayload(
            id, (cataclysm ? Spell.EXPLOSION : Spell.METEOR).networkId(), target.x, target.y, target.z, height, level.getGameTime(), fallTicks
         )
      );
      renderGroundSigil(level, target, cataclysm);
      level.playSound(
         null,
         BlockPos.containing(target),
         cataclysm ? SoundEvents.WITHER_SPAWN : SoundEvents.BEACON_DEACTIVATE,
         SoundSource.PLAYERS,
         cataclysm ? 2.5F : 1.4F,
         cataclysm ? 0.45F : 0.55F
      );
      return true;
   }

   private boolean placeTemporary(ServerLevel level, BlockPos pos, BlockState state, long expires) {
      if (!level.getBlockState(pos).isAir()) {
         return false;
      }

      if (!level.setBlock(pos, state, 3)) {
         return false;
      }

      this.temporaryBlocks.put(new SpellEffects.TemporaryBlock(level.dimension(), pos.immutable(), state.getBlock()), expires);
      return true;
   }

   private static Vec3 flatLook(ServerPlayer player) {
      Vec3 result = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z);
      return result.lengthSqr() < 0.001 ? new Vec3(0.0, 0.0, 1.0) : result.normalize();
   }

   private static Vec3 rotateY(Vec3 vector, double angle) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      return new Vec3(vector.x * cos - vector.z * sin, 0.0, vector.x * sin + vector.z * cos).normalize();
   }

   private static HitResult aimedHit(ServerPlayer player, double range, boolean entities) {
      return ProjectileUtil.getHitResultOnViewVector(player, entity -> entities && entity != player && entity.isPickable(), range);
   }

   private static LivingEntity skillTarget(ServerPlayer player, double range) {
      if (!(
         ProjectileUtil.getHitResultOnViewVector(player, entity -> entity != player && entity instanceof LivingEntity && entity.isPickable(), range) instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living
      )) {
         return null;
      } else {
         return living instanceof Player targetPlayer && !player.canHarmPlayer(targetPlayer) ? null : living;
      }
   }

   private static final class CloneTask {
      private final Mannequin clone;
      private Vec3 direction;
      private final long expires;
      private int turnCountdown;
      private int age;

      private CloneTask(Mannequin clone, Vec3 direction, long expires, int turnCountdown) {
         this.clone = clone;
         this.direction = direction;
         this.expires = expires;
         this.turnCountdown = turnCountdown;
      }
   }

   private static final class CraterTask {
      private final ResourceKey<Level> dimension;
      private final BlockPos center;
      private final int maximumDepth;
      private final int topRadius;
      private int depth;

      private CraterTask(ResourceKey<Level> dimension, BlockPos center, int maximumDepth, int topRadius) {
         this.dimension = dimension;
         this.center = center;
         this.maximumDepth = maximumDepth;
         this.topRadius = topRadius;
      }
   }

   private record FallingSpellTask(ResourceKey<Level> dimension, Vec3 target, UUID caster, long impactTick, int id, float power, boolean cataclysm) {
   }

   private record HealingVisualTask(ResourceKey<Level> dimension, UUID player, long started, long expires) {
   }

   private record ImpactVisualTask(ResourceKey<Level> dimension, Vec3 target, long started, long expires, boolean cataclysm) {
   }

   private static final class PolymorphTask {
      private final LivingEntity target;
      private final Mob form;
      private final long expires;
      private final boolean alreadyInvisible;

      private PolymorphTask(LivingEntity target, Mob form, long expires, boolean alreadyInvisible) {
         this.target = target;
         this.form = form;
         this.expires = expires;
         this.alreadyInvisible = alreadyInvisible;
      }

      private void finish() {
         if (!this.form.isRemoved()) {
            this.form.discard();
         }

         if (!this.target.isRemoved() && !this.alreadyInvisible) {
            this.target.removeEffect(MobEffects.INVISIBILITY);
         }
      }
   }

   private static final class ShrinkTask {
      private final LivingEntity target;
      private final long expires;

      private ShrinkTask(LivingEntity target, long expires) {
         this.target = target;
         this.expires = expires;
      }

      private void finish() {
         if (!this.target.isRemoved()) {
            AttributeInstance scale = this.target.getAttribute(Attributes.SCALE);
            if (scale != null) {
               scale.removeModifier(SpellEffects.SHRINK_MODIFIER);
            }
         }
      }
   }

   private record TemporaryBlock(ResourceKey<Level> dimension, BlockPos pos, net.minecraft.world.level.block.Block expected) {
   }
}
