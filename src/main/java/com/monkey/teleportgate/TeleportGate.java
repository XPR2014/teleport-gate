package com.monkey.teleportgate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.monkey.teleportgate.network.OpenTeleportScreenPayload;
import com.monkey.teleportgate.network.RenamePointPayload;
import com.monkey.teleportgate.network.RenameResultPayload;
import com.monkey.teleportgate.network.TeleportListPayload;
import com.monkey.teleportgate.network.TeleportPoint;
import com.monkey.teleportgate.network.TeleportToPayload;
import com.monkey.teleportgate.registry.ModBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportGate implements ModInitializer {
	public static final String MOD_ID = "teleport-gate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** dimId -> (posKey -> name) */
	public static final Map<String, Map<String, String>> POINTS = new ConcurrentHashMap<>();
	private static boolean dirty = false;
	/** 玩家上次传送时间 tick */
	private static final Map<UUID, Long> LAST_TELEPORT = new ConcurrentHashMap<>();
	private static final long COOLDOWN_TICKS = 100; // 5秒
	private static final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
	private static int tickCounter = 0;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path saveFile;

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		ModBlocks.initialize();
		registerNetworking();
		registerServerEvents();
	}

	private static String dimId(ResourceKey<Level> dim) {
		return dim.identifier().toString();
	}

	private static String posKey(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	private static BlockPos parsePos(String key) {
		String[] p = key.split(",");
		return new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
	}

	public static void addPoint(ServerLevel level, BlockPos pos) {
		String dim = dimId(level.dimension());
		String key = posKey(pos);
		// 直接存数字字符串，客户端看到纯数字就翻译成"传送方块 N"
		POINTS.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
				.putIfAbsent(key, String.valueOf(counter.incrementAndGet()));
		dirty = true;
	}

	public static void registerServerEvents() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.getBlockState(hit.getBlockPos()).getBlock() == ModBlocks.TELEPORT_BLOCK) {
				return level.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (state.getBlock() == ModBlocks.TELEPORT_BLOCK && level instanceof ServerLevel sl) {
				Map<String, String> m = POINTS.get(dimId(sl.dimension()));
				if (m != null) {
					m.remove(posKey(pos));
					dirty = true;
				}
			}
		});

		// 服务器启动读文件
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			saveFile = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
					.resolve("teleport_gate_points.json");
			loadFromDisk();
		});

		// 服务器停止写文件
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveToDisk());

		// 每30秒自动保存（仅在有改动时写盘）
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!dirty) return;
			if (++tickCounter >= 600) {
				tickCounter = 0;
				saveToDisk();
				dirty = false;
			}
		});
	}

	private static void loadFromDisk() {
		if (saveFile == null || !Files.exists(saveFile)) return;
		try {
			String json = Files.readString(saveFile);
			Type t = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
			Map<String, Map<String, String>> loaded = GSON.fromJson(json, t);
			POINTS.clear();
			if (loaded != null) POINTS.putAll(loaded);
			// 恢复 counter：扫描已有纯数字名字，把 counter 推到最大值，避免重启重号
			int maxN = 0;
			for (var m : POINTS.values()) {
				for (String name : m.values()) {
					if (name != null && !name.isEmpty() && name.matches("\\d+")) {
						maxN = Math.max(maxN, Integer.parseInt(name));
					}
				}
			}
			counter.set(maxN);
			LOGGER.info("[TeleportGate] loaded {} dims, {} points, next number = {}",
					POINTS.size(),
					POINTS.values().stream().mapToInt(Map::size).sum(),
					maxN + 1);
		} catch (IOException e) {
			LOGGER.error("[TeleportGate] failed to load points", e);
		}
	}

	private static void saveToDisk() {
		if (saveFile == null) return;
		try {
			Files.writeString(saveFile, GSON.toJson(POINTS));
			LOGGER.info("[TeleportGate] saved points to {}", saveFile);
		} catch (IOException e) {
			LOGGER.error("[TeleportGate] failed to save points", e);
		}
	}

	private void registerNetworking() {
		PayloadTypeRegistry.playC2S().register(OpenTeleportScreenPayload.TYPE, OpenTeleportScreenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportToPayload.TYPE, TeleportToPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RenamePointPayload.TYPE, RenamePointPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportListPayload.TYPE, TeleportListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RenameResultPayload.TYPE, RenameResultPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(OpenTeleportScreenPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos current = payload.current();
			context.server().execute(() -> {
				String playerDim = dimId(player.level().dimension());
				List<TeleportPoint> list = new ArrayList<>();
				POINTS.forEach((dim, map) -> {
					boolean same = dim.equals(playerDim);
					map.forEach((key, name) -> list.add(new TeleportPoint(parsePos(key), name, same)));
				});
				String curName = POINTS.getOrDefault(playerDim, Map.of())
						.getOrDefault(posKey(current), "");
				ServerPlayNetworking.send(player, new TeleportListPayload(list, curName));
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(RenamePointPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				String name = payload.name().trim();
				if (name.isEmpty()) return;
				String dim = dimId(player.level().dimension());

				boolean dup = POINTS.values().stream()
						.flatMap(m -> m.values().stream())
						.anyMatch(n -> n.equalsIgnoreCase(name));
				if (dup) {
					ServerPlayNetworking.send(player, new RenameResultPayload(false, "screen.teleport-gate.full_of_name"));
					return;
				}
				POINTS.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
						.put(posKey(payload.pos()), name);
				dirty = true;
				ServerPlayNetworking.send(player, new RenameResultPayload(true, ""));
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(TeleportToPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos target = payload.target();
			context.server().execute(() -> {
				// 冷却检查
				long now = player.level().getGameTime();
				Long last = LAST_TELEPORT.get(player.getUUID());
				if (last != null && now - last < COOLDOWN_TICKS) {
					long left = (COOLDOWN_TICKS - (now - last)) / 20 + 1;
					player.displayClientMessage(Component.translatable("screen.teleport-gate.teleport_pause", left), true);
					return;
				}

				String playerDim = dimId(player.level().dimension());
				String targetKey = posKey(target);
				String targetDim = null;
				for (var e : POINTS.entrySet()) {
					if (e.getValue().containsKey(targetKey)) {
						targetDim = e.getKey();
						break;
					}
				}
				if (targetDim == null || !targetDim.equals(playerDim)) {
					player.displayClientMessage(Component.translatable("screen.teleport-gate.teleport_other_worlds"), true);
					return;
				}
				ServerLevel sl = (ServerLevel) player.level();
				double tx = target.getX() + 0.5, ty = target.getY() + 0.5, tz = target.getZ() + 0.5;
				player.teleportTo(sl, tx, ty, tz,
						Set.of(), player.getYRot(), player.getXRot(), true);
				LAST_TELEPORT.put(player.getUUID(), now);

				// 传送成功：粒子 + 音效
				sl.sendParticles(ParticleTypes.PORTAL, tx, ty + 1, tz, 50, 0.5, 1, 0.5, 0.1);
				sl.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
			});
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
