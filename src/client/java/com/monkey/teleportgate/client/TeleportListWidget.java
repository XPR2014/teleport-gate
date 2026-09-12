package com.monkey.teleportgate.client;

import com.monkey.teleportgate.network.TeleportPoint;
import com.monkey.teleportgate.network.TeleportToPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 原版风格可滚动传送点列表。
 */
public class TeleportListWidget extends AbstractSelectionList<TeleportListWidget.Entry> {

    private static final int ITEM_HEIGHT = 25;

    public TeleportListWidget(Minecraft mc, int width, int height, int y) {
        super(mc, width, height, y, ITEM_HEIGHT);
    }

    public void setPoints(List<TeleportPoint> points) {
        this.clearEntries();
        for (TeleportPoint p : points) {
            this.addEntry(new Entry(p));
        }
    }

    protected int getMaxPosition() {
        return this.getItemCount() * ITEM_HEIGHT;
    }

    public int getRowWidth() {
        return this.width - 12;
    }

    @Override
    protected void renderListItems(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int left = this.getRowLeft();
        for (int i = 0; i < this.getItemCount(); i++) {
            Entry entry = this.children().get(i);
            TeleportPoint p = entry.point;
            int y = this.getRowTop(i);
            boolean hover = mouseX >= left && mouseX <= left + this.getRowWidth()
                    && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

            if (p.sameDimension()) {
                g.fill(left, y, left + this.getRowWidth(), y + ITEM_HEIGHT - 2,
                        hover ? 0xCC8B5CF6 : 0xCC6D28D9);
                g.drawString(this.minecraft.font, p.name(), left + 6, y + 4, 0xFFFFFFFF, false);
                String coord = p.pos().getX() + ", " + p.pos().getY() + ", " + p.pos().getZ();
                g.drawString(this.minecraft.font, coord, left + 6, y + 13, 0xFFD8B4FE, false);
            } else {
                // 跨维度：灰色
                g.fill(left, y, left + this.getRowWidth(), y + ITEM_HEIGHT - 2, 0x55444444);
                g.drawString(this.minecraft.font, p.name() + " (其他维度)", left + 6, y + 4, 0xFF888888, false);
                String coord = p.pos().getX() + ", " + p.pos().getY() + ", " + p.pos().getZ();
                g.drawString(this.minecraft.font, coord, left + 6, y + 13, 0xFF666666, false);
            }
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
    }

    public class Entry extends AbstractSelectionList.Entry<Entry> {
        public final TeleportPoint point;

        public Entry(TeleportPoint point) {
            this.point = point;
        }

        @Override
        public void renderContent(GuiGraphics g, int mouseX, int mouseY, boolean hover, float delta) {
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                if (!point.sameDimension()) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("不能跨维度传送！"), true);
                    return true;
                }
                ClientPlayNetworking.send(new TeleportToPayload(point.pos()));
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            return false;
        }
    }
}
