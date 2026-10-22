package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record SpellVisualPayload(int spellId, double x, double y, double z, int phaseTicks, int durationTicks, int entityId, double scale)
   implements CustomPacketPayload {
   public static final Type<SpellVisualPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "spell_visual"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpellVisualPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SpellVisualPayload::spellId,
      ByteBufCodecs.DOUBLE,
      SpellVisualPayload::x,
      ByteBufCodecs.DOUBLE,
      SpellVisualPayload::y,
      ByteBufCodecs.DOUBLE,
      SpellVisualPayload::z,
      ByteBufCodecs.VAR_INT,
      SpellVisualPayload::phaseTicks,
      ByteBufCodecs.VAR_INT,
      SpellVisualPayload::durationTicks,
      ByteBufCodecs.VAR_INT,
      SpellVisualPayload::entityId,
      ByteBufCodecs.DOUBLE,
      SpellVisualPayload::scale,
      SpellVisualPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
