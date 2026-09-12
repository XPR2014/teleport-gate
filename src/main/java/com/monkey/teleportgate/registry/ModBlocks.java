package com.monkey.teleportgate.registry;

import com.monkey.teleportgate.TeleportGate;
import com.monkey.teleportgate.block.TeleportBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 所有方块 + 对应 BlockItem 的注册都放在这里。
 */
public class ModBlocks {

    /** 传送方块的资源 key */
    public static final ResourceKey<Block> TELEPORT_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, TeleportGate.id("teleport_block"));

    /** 传送方块本体 */
    public static final Block TELEPORT_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            TELEPORT_BLOCK_KEY,
            new TeleportBlock(
                    BlockBehaviour.Properties.of()
                            .setId(TELEPORT_BLOCK_KEY)
                            // 硬度比黑曜石(50)略低，抗爆性仍很高
                            .strength(15.0f, 900.0f)
                            // 必须用正确工具（钻石镐及以上）挖掘才掉落
                            .requiresCorrectToolForDrops()
            )
    );

    /** 方块对应的物品 key */
    public static final ResourceKey<Item> TELEPORT_BLOCK_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, TeleportGate.id("teleport_block"));

    /** 方块对应的物品（创造栏里能拿到） */
    public static final Item TELEPORT_BLOCK_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            TELEPORT_BLOCK_ITEM_KEY,
            new BlockItem(
                    TELEPORT_BLOCK,
                    new Item.Properties().setId(TELEPORT_BLOCK_ITEM_KEY)
            )
    );

    /**
     * 这个方法只做一件事：触发本类的类加载，让上面的 static 字段完成注册。
     * 由主类 onInitialize() 调用。
     */
    public static void initialize() {
        // 加入“建筑方块”创造物品栏
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(entries -> entries.accept(TELEPORT_BLOCK_ITEM));

        TeleportGate.LOGGER.info("Registered teleport block: {}", TeleportGate.id("teleport_block"));
    }
}
