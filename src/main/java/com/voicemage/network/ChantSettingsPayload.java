package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record ChantSettingsPayload(boolean required) implements CustomPacketPayload {
   public static final Type<ChantSettingsPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "chant_settings"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ChantSettingsPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, ChantSettingsPayload::required, ChantSettingsPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
