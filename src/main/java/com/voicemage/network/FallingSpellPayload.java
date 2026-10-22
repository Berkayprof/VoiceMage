package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record FallingSpellPayload(int id, int spellId, double x, double y, double z, double height, long started, int duration) implements CustomPacketPayload {
   public static final Type<FallingSpellPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "falling_spell"));
   public static final StreamCodec<RegistryFriendlyByteBuf, FallingSpellPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      FallingSpellPayload::id,
      ByteBufCodecs.VAR_INT,
      FallingSpellPayload::spellId,
      ByteBufCodecs.DOUBLE,
      FallingSpellPayload::x,
      ByteBufCodecs.DOUBLE,
      FallingSpellPayload::y,
      ByteBufCodecs.DOUBLE,
      FallingSpellPayload::z,
      ByteBufCodecs.DOUBLE,
      FallingSpellPayload::height,
      ByteBufCodecs.VAR_LONG,
      FallingSpellPayload::started,
      ByteBufCodecs.VAR_INT,
      FallingSpellPayload::duration,
      FallingSpellPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
