package com.monkey.teleportgate.network;

import com.monkey.teleportgate.TeleportGate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** 服务端→客户端：所有传送点列表 + 当前方块的已有名字。 */
public record TeleportListPayload(List<TeleportPoint> points, String currentName) implements CustomPacketPayload {
    public static final Type<TeleportListPayload> TYPE =
            new Type<>(TeleportGate.id("teleport_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportListPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, TeleportPoint.CODEC),
                    TeleportListPayload::points,
                    ByteBufCodecs.STRING_UTF8,
                    TeleportListPayload::currentName,
                    TeleportListPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
