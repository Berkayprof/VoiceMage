package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record CastSpellPayload(int spellId) implements CustomPacketPayload {
   public static final Type<CastSpellPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "cast_spell"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, CastSpellPayload::spellId, CastSpellPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
