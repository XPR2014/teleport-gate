package com.monkey.teleportgate.network;

import com.monkey.teleportgate.TeleportGate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端→服务端：玩家右键打开传送界面，带当前方块位置。 */
public record OpenTeleportScreenPayload(BlockPos current) implements CustomPacketPayload {
    public static final Type<OpenTeleportScreenPayload> TYPE =
            new Type<>(TeleportGate.id("open_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeleportScreenPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenTeleportScreenPayload::current,
                    OpenTeleportScreenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
