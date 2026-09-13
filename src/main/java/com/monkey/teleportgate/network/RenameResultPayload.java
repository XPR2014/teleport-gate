package com.monkey.teleportgate.network;

import com.monkey.teleportgate.TeleportGate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 服务端→客户端：改名结果。成功 true，失败 false + 原因（key）。 */
public record RenameResultPayload(boolean success, String reasonKey) implements CustomPacketPayload {
    public static final Type<RenameResultPayload> TYPE =
            new Type<>(TeleportGate.id("rename_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenameResultPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, RenameResultPayload::success,
                    ByteBufCodecs.STRING_UTF8, RenameResultPayload::reasonKey,
                    RenameResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
