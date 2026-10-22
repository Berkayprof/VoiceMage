package com.voicemage;

import com.voicemage.config.VoicemageConfig;
import com.voicemage.network.CastSpellPayload;
import com.voicemage.network.ChantSettingsPayload;
import com.voicemage.network.FallingSpellPayload;
import com.voicemage.network.ManaSyncPayload;
import com.voicemage.network.SpellVisualPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class VoicemageNetworking {
   private static final double BROADCAST_DISTANCE_SQUARED = 65536.0;

   private VoicemageNetworking() {
   }

   public static void syncSettings(ServerPlayer player) {
      ServerPlayNetworking.send(player, new ChantSettingsPayload(VoicemageConfig.INSTANCE.explosionChantRequired));
   }

   public static void broadcastSettings(MinecraftServer server) {
      server.getPlayerList().getPlayers().forEach(VoicemageNetworking::syncSettings);
   }

   public static void register() {
      PayloadTypeRegistry.clientboundPlay().register(ChantSettingsPayload.TYPE, ChantSettingsPayload.STREAM_CODEC);
      PayloadTypeRegistry.serverboundPlay().register(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(ManaSyncPayload.TYPE, ManaSyncPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(SpellVisualPayload.TYPE, SpellVisualPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(FallingSpellPayload.TYPE, FallingSpellPayload.STREAM_CODEC);
      ServerPlayNetworking.registerGlobalReceiver(
         CastSpellPayload.TYPE, (payload, context) -> context.server().execute(() -> Voicemage.SPELLS.cast(context.player(), payload.spellId()))
      );
   }

   public static void sync(ServerPlayer player, double mana, int spellId, int result) {
      ServerPlayNetworking.send(player, new ManaSyncPayload((int)Math.round(mana * 10.0), (int)Math.round(Voicemage.SPELLS.maxMana() * 10.0), spellId, result));
   }

   public static void falling(ServerLevel level, FallingSpellPayload payload) {
      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         if (player.level() == level && player.distanceToSqr(payload.x(), payload.y(), payload.z()) <= 65536.0) {
            ServerPlayNetworking.send(player, payload);
         }
      }
   }

   public static void visual(ServerLevel level, int spellId, double x, double y, double z, int phaseTicks, int durationTicks, int entityId, double scale) {
      SpellVisualPayload payload = new SpellVisualPayload(spellId, x, y, z, phaseTicks, durationTicks, entityId, scale);
      Entity attached = entityId >= 0 ? level.getEntity(entityId) : null;
      double broadcastX = attached == null ? x : attached.getX();
      double broadcastY = attached == null ? y : attached.getY();
      double broadcastZ = attached == null ? z : attached.getZ();

      for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
         if (viewer.level() == level && viewer.distanceToSqr(broadcastX, broadcastY, broadcastZ) <= 65536.0) {
            ServerPlayNetworking.send(viewer, payload);
         }
      }
   }
}
