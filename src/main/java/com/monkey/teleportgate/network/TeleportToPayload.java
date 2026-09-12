package com.monkey.teleportgate.network;

import com.monkey.teleportgate.TeleportGate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端→服务端：玩家选择传送到哪个传送方块。 */
public record TeleportToPayload(BlockPos target) implements CustomPacketPayload {
    public static final Type<TeleportToPayload> TYPE =
            new Type<>(TeleportGate.id("teleport_to"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportToPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    TeleportToPayload::target,
                    TeleportToPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
