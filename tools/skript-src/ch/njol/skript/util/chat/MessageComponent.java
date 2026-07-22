/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonPrimitive
 *  com.google.gson.JsonSerializationContext
 *  com.google.gson.JsonSerializer
 *  net.md_5.bungee.api.ChatColor
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.chat;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Locale;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.15", forRemoval=true)
public class MessageComponent {
    public String text = "";
    public boolean reset = false;
    public boolean bold = false;
    public boolean italic = false;
    public boolean underlined = false;
    public boolean strikethrough = false;
    public boolean obfuscated = false;
    @Nullable
    public ChatColor color;
    @Nullable
    public String insertion;
    @Nullable
    public ClickEvent clickEvent;
    @Nullable
    public String font;
    @Nullable
    public String translation;
    @Nullable
    public String keybind;
    @Nullable
    public HoverEvent hoverEvent;

    public MessageComponent copy() {
        MessageComponent messageComponent = new MessageComponent();
        messageComponent.text = this.text;
        messageComponent.reset = this.reset;
        messageComponent.bold = this.bold;
        messageComponent.italic = this.italic;
        messageComponent.underlined = this.underlined;
        messageComponent.strikethrough = this.strikethrough;
        messageComponent.obfuscated = this.obfuscated;
        messageComponent.color = this.color;
        messageComponent.insertion = this.insertion;
        messageComponent.clickEvent = this.clickEvent;
        messageComponent.font = this.font;
        messageComponent.hoverEvent = this.hoverEvent;
        messageComponent.translation = this.translation;
        messageComponent.keybind = this.keybind;
        return messageComponent;
    }

    public static class ClickEvent {
        public Action action;
        public String value;

        public ClickEvent(Action action, String value) {
            this.action = action;
            this.value = value;
        }

        public static enum Action {
            open_url,
            run_command,
            suggest_command,
            change_page,
            copy_to_clipboard;

            public final String spigotName = this.name().toUpperCase(Locale.ENGLISH);
        }
    }

    public static class HoverEvent {
        public Action action;
        public String value;

        public HoverEvent(Action action, String value) {
            this.action = action;
            this.value = value;
        }

        public static enum Action {
            show_text,
            show_item,
            show_entity,
            show_achievement;

            public final String spigotName = this.name().toUpperCase(Locale.ENGLISH);
        }
    }

    public static class BooleanSerializer
    implements JsonSerializer<Boolean> {
        @Nullable
        public JsonElement serialize(@Nullable Boolean src, @Nullable Type typeOfSrc, @Nullable JsonSerializationContext context) {
            return src != false ? new JsonPrimitive(Boolean.valueOf(true)) : null;
        }
    }
}

