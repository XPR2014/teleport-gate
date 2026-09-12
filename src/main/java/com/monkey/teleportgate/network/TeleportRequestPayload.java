package com.monkey.teleportgate.network;

import com.monkey.teleportgate.TeleportGate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端 → 服务端：玩家在传送界面里点了某个传送按钮。
 * target: 0 = 出生点, 1 = 世界出生点, 2 = 自己的床/重生点
 */
public record TeleportRequestPayload(int target) implements CustomPacketPayload {

    public static final Type<TeleportRequestPayload> TYPE =
            new Type<>(TeleportGate.id("teleport_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    TeleportRequestPayload::target,
                    TeleportRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
