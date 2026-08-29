package work.mcwws.newsarchive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BookRenderer {
    private static final Pattern PLACEHOLDER = Pattern.compile("%booknews_([a-zA-Z0-9_]+)%");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ArchiveStore store;

    public BookRenderer(ArchiveStore store) {
        this.store = store;
    }

    public void open(Player player, NewsVersion version) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(trimTitle(version.title()));
        meta.setAuthor("MCWWS");
        List<Component> pages = new ArrayList<>();
        for (Map.Entry<String, String> entry : version.pages().entrySet()) {
            pages.add(renderPage(player, entry.getValue()));
        }
        if (pages.isEmpty()) {
            pages.add(Component.text("（空）"));
        }
        meta.pages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    private Component renderPage(Player player, String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        String text = raw.replace("%player_name%", player.getName());
        TextComponent.Builder page = Component.text();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                page.append(Component.newline());
            }
            page.append(renderLine(lines[i]));
        }
        return page.build();
    }

    private Component renderLine(String line) {
        TextComponent.Builder builder = Component.text();
        Matcher matcher = PLACEHOLDER.matcher(line);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                builder.append(LEGACY.deserialize(line.substring(last, matcher.start())));
            }
            String placeholder = matcher.group(1);
            ArchiveStore.InteractiveWord word = store.interactiveWords().get(placeholder);
            if (word == null) {
                builder.append(Component.empty());
            } else if ("CHANGE_PAGE".equalsIgnoreCase(word.clickAction())) {
                // 原版书本自带翻页，交互跳页占位仅显示文字
                builder.append(styleWord(word));
            } else {
                builder.append(interactive(word));
            }
            last = matcher.end();
        }
        if (last < line.length()) {
            builder.append(LEGACY.deserialize(line.substring(last)));
        }
        return builder.build();
    }

    private Component interactive(ArchiveStore.InteractiveWord word) {
        Component base = styleWord(word);
        if (word.hoverEnable() && word.hoverText() != null && !word.hoverText().isBlank()) {
            base = base.hoverEvent(HoverEvent.showText(LEGACY.deserialize(word.hoverText())));
        }
        if (word.clickEnable()) {
            String action = word.clickAction() == null ? "" : word.clickAction().toUpperCase(Locale.ROOT);
            String value = word.clickValue() == null ? "" : word.clickValue();
            switch (action) {
                case "RUN_COMMAND" -> {
                    String cmd = value.startsWith("/") ? value : "/" + value;
                    base = base.clickEvent(ClickEvent.runCommand(cmd));
                }
                case "OPEN_URL" -> base = base.clickEvent(ClickEvent.openUrl(value));
                case "COPY_TO_CLIPBOARD" -> base = base.clickEvent(ClickEvent.copyToClipboard(value));
                default -> {
                }
            }
        }
        return base;
    }

    private Component styleWord(ArchiveStore.InteractiveWord word) {
        TextColor color = NamedTextColor.NAMES.value(word.color().toLowerCase(Locale.ROOT));
        if (color == null) {
            color = NamedTextColor.BLACK;
        }
        Component component = Component.text(word.word()).color(color);
        if (word.bold()) {
            component = component.decorate(TextDecoration.BOLD);
        }
        if (word.italic()) {
            component = component.decorate(TextDecoration.ITALIC);
        }
        if (word.underlined()) {
            component = component.decorate(TextDecoration.UNDERLINED);
        }
        if (word.obfuscated()) {
            component = component.decorate(TextDecoration.OBFUSCATED);
        }
        return component;
    }

    private static String trimTitle(String title) {
        String plain = ArchiveStore.stripColors(title);
        if (plain.length() > 32) {
            return plain.substring(0, 32);
        }
        return plain.isBlank() ? "服务器告示" : plain;
    }
}
