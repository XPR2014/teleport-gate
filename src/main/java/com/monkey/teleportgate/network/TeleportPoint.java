package com.monkey.teleportgate.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** 一个传送点：位置 + 名字 + 是否在玩家当前维度 */
public record TeleportPoint(BlockPos pos, String name, boolean sameDimension) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPoint> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TeleportPoint::pos,
            ByteBufCodecs.STRING_UTF8, TeleportPoint::name,
            ByteBufCodecs.BOOL, TeleportPoint::sameDimension,
            TeleportPoint::new
    );
}
