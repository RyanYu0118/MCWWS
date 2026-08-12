package work.mcwws.axiomsurvival.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * 屏幕左下角的余额变动浮层。
 *
 * <p>服务端 {@code MCWWS_EconomyLedger} 每笔余额变动推一条已排版文本过来，这里按时间叠放，
 * 新的在下、旧的往上，超时自动消失。挂在原版聊天层之后绘制，配合半透明底衬保证压住聊天时仍可读。
 */
public final class BalanceHudOverlay implements HudElement {

    /** 单条提示存活时长 */
    private static final long LIFETIME_MS = 5000L;
    private static final int MAX_ENTRIES = 5;
    private static final int LEFT_MARGIN = 4;
    /** 从屏幕底部上抬的距离：让开原版聊天最近几行 */
    private static final int BOTTOM_OFFSET = 70;
    private static final int BACKDROP_COLOR = 0x90000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static final class Entry {
        private final String key;
        private String text;
        private long createdAt;

        private Entry(String key, String text, long createdAt) {
            this.key = key;
            this.text = text;
            this.createdAt = createdAt;
        }
    }

    /**
     * 推入一条提示。{@code replace} 为真且最新一条同属一个分组时原地替换，
     * 用于飞行每秒扣费、Axiom 逐包扣费这类累加显示。
     */
    public static void push(String key, String text, boolean replace) {
        if (text == null || text.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (ENTRIES) {
            prune(now);
            if (replace && !ENTRIES.isEmpty()) {
                Entry newest = ENTRIES.get(ENTRIES.size() - 1);
                if (newest.key.equals(key)) {
                    newest.text = text;
                    newest.createdAt = now;
                    return;
                }
            }
            ENTRIES.add(new Entry(key, text, now));
            while (ENTRIES.size() > MAX_ENTRIES) {
                ENTRIES.remove(0);
            }
        }
    }

    public static void clear() {
        synchronized (ENTRIES) {
            ENTRIES.clear();
        }
    }

    private static void prune(long now) {
        ENTRIES.removeIf(entry -> now - entry.createdAt > LIFETIME_MS);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        // 挂在聊天层之后，F1 隐藏 HUD 时由原版一并跳过，这里只需要确认在游戏里
        if (client.player == null) {
            return;
        }
        List<String> lines;
        synchronized (ENTRIES) {
            prune(System.currentTimeMillis());
            if (ENTRIES.isEmpty()) {
                return;
            }
            lines = new ArrayList<>(ENTRIES.size());
            for (Entry entry : ENTRIES) {
                lines.add(entry.text);
            }
        }

        Font font = client.font;
        int lineHeight = font.lineHeight + 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - lineHeight;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            int width = font.width(line);
            graphics.fill(
                    LEFT_MARGIN - 2,
                    y - 2,
                    LEFT_MARGIN + width + 2,
                    y + font.lineHeight + 1,
                    BACKDROP_COLOR
            );
            graphics.text(font, line, LEFT_MARGIN, y, TEXT_COLOR);
            y -= lineHeight;
        }
    }
}
