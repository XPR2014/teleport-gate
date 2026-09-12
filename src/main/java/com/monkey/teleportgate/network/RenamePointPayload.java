package com.monkey.teleportgate.network;

import com.monkey.teleportgate.TeleportGate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端→服务端：给某个传送方块改名。 */
public record RenamePointPayload(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<RenamePointPayload> TYPE =
            new Type<>(TeleportGate.id("rename_point"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenamePointPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RenamePointPayload::pos,
                    ByteBufCodecs.STRING_UTF8, RenamePointPayload::name,
                    RenamePointPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
