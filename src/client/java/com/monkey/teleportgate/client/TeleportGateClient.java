package com.monkey.teleportgate.client;

import com.monkey.teleportgate.network.OpenTeleportScreenPayload;
import com.monkey.teleportgate.network.RenamePointPayload;
import com.monkey.teleportgate.network.RenameResultPayload;
import com.monkey.teleportgate.network.TeleportListPayload;
import com.monkey.teleportgate.network.TeleportPoint;
import com.monkey.teleportgate.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TeleportGateClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("teleport-gate-client");
    public static BlockPos currentPos;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            BlockPos pos = hit.getBlockPos();
            if (level.getBlockState(pos).getBlock() == ModBlocks.TELEPORT_BLOCK) {
                if (level.isClientSide()) {
                    currentPos = pos.immutable();
                    ClientPlayNetworking.send(new OpenTeleportScreenPayload(currentPos));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ClientPlayNetworking.registerGlobalReceiver(TeleportListPayload.TYPE, (payload, context) -> {
            Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().setScreen(
                            new TeleportScreen(payload.points(), payload.currentName())));
        });

        ClientPlayNetworking.registerGlobalReceiver(RenameResultPayload.TYPE, (payload, context) -> {
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().screen instanceof TeleportScreen screen) {
                    screen.onRenameResult(payload.success(), payload.reasonKey());
                }
            });
        });
    }

    public static class TeleportScreen extends Screen {
        private static final int BG       = 0xF01A0430;
        private static final int BORDER   = 0xFFA855F7;
        private static final int ACCENT   = 0xFFE9D5FF;

        private List<TeleportPoint> points;
        private final String currentName;
        private EditBox nameBox;
        private EditBox searchBox;
        private TeleportListWidget list;
        private Button saveBtn;
        private int saveTick = 0;

        /** 把纯数字字符串转成翻译后的字面文字（给输入框用），其他原样返回 */
        private static String displayNameText(String raw) {
            if (raw != null && !raw.isEmpty() && raw.matches("\\d+")) {
                return Component.translatable("screen.teleport-gate.auto_name", Integer.parseInt(raw)).getString();
            }
            return raw == null ? "" : raw;
        }

        private boolean originalIsAuto;
        private String originalDisplayText;

        private int panelX, panelY, panelW, panelH;

        public TeleportScreen(List<TeleportPoint> points, String currentName) {
            super(Component.translatable("screen.teleport-gate.teleport"));
            this.points = points;
            this.currentName = currentName;
        }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int cy = this.height / 2;
            panelW = 240;
            panelH = 200;
            panelX = cx - panelW / 2;
            panelY = cy - panelH / 2;
            int bw = panelW - 28;

            // 名字输入框：如果是自动名 __auto__N，显示翻译后的文字
            nameBox = new EditBox(this.font, panelX + 14, panelY + 28, bw, 16,
                    Component.translatable("screen.teleport-gate.name"));
            nameBox.setMaxLength(24);
            originalIsAuto = currentName != null && currentName.matches("\\d+");
            originalDisplayText = currentName == null ? "" : displayNameText(currentName);
            nameBox.setValue(originalDisplayText);
            addRenderableWidget(nameBox);

            // 保存名字
            saveBtn = Button.builder(
                            Component.translatable("screen.teleport-gate.save_name"),
                            b -> {
                                if (currentPos == null) return;
                                String input = nameBox.getValue().trim();
                                if (input.isEmpty()) {
                                    saveBtn.setMessage(Component.translatable("screen.teleport-gate.save_empty"));
                                    saveTick = 40;
                                    return;
                                }
                                // 自动名没改 → 不发，提示未修改
                                if (originalIsAuto && input.equals(originalDisplayText)) {
                                    saveBtn.setMessage(Component.translatable("screen.teleport-gate.save_unchanged"));
                                    saveTick = 40;
                                    return;
                                }
                                ClientPlayNetworking.send(new RenamePointPayload(currentPos, input));
                                saveBtn.setMessage(Component.translatable("screen.teleport-gate.saved"));
                                saveTick = 40;
                            })
                    .bounds(panelX + 14, panelY + 48, bw, 16).build();
            addRenderableWidget(saveBtn);

            // 搜索框
            searchBox = new EditBox(this.font, panelX + 14, panelY + 70, bw, 14,
                    Component.translatable("screen.teleport-gate.search"));
            searchBox.setMaxLength(32);
            searchBox.setResponder(text -> applyFilter());
            addRenderableWidget(searchBox);

            // 可滚动列表
            int listY = panelY + 88;
            int listH = 82;
            list = new TeleportListWidget(this.minecraft, panelW - 28, listH, listY);
            list.setX(panelX + 14);
            list.setY(listY);
            list.setPoints(filtered(""));
            addRenderableWidget(list);

            // 取消
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.cancel"),
                            b -> this.onClose())
                    .bounds(panelX + 14, panelY + panelH - 26, bw, 18).build());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            g.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG);
            g.fill(panelX, panelY, panelX + panelW, panelY + 1, BORDER);
            g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, BORDER);
            g.fill(panelX, panelY, panelX + 1, panelY + panelH, BORDER);
            g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, BORDER);

            super.render(g, mouseX, mouseY, delta);

            g.drawCenteredString(this.font, this.title, panelX + panelW / 2, panelY + 10, ACCENT);
            g.drawString(this.font,
                    Component.translatable("screen.teleport-gate.name_label"),
                    panelX + 14, panelY + 18, 0xFFD8B4FE, false);
        }

        private void applyFilter() {
            list.setPoints(filtered(searchBox == null ? "" : searchBox.getValue()));
        }

        private List<TeleportPoint> filtered(String query) {
            if (query.isEmpty()) return points;
            String q = query.toLowerCase();
            return points.stream()
                    .filter(p -> p.name().toLowerCase().contains(q))
                    .toList();
        }

        @Override
        public void tick() {
            super.tick();
            if (saveTick > 0) {
                saveTick--;
                if (saveTick == 0) {
                    saveBtn.setMessage(Component.translatable("screen.teleport-gate.save_name"));
                }
            }
        }

        /** 服务端返回改名结果后更新按钮文字 */
        void onRenameResult(boolean success, String reasonKey) {
            if (success) {
                saveBtn.setMessage(Component.translatable("screen.teleport-gate.saved"));
            } else {
                saveBtn.setMessage(Component.translatable("screen.teleport-gate.save_failed"));
            }
            saveTick = 60; // 3秒
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
