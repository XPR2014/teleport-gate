package com.monkey.teleportgate.block;

import com.monkey.teleportgate.TeleportGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TeleportBlock extends Block {
    public TeleportBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            TeleportGate.addPoint(sl, pos);
        }
    }
}
