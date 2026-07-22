/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ClickEvent
 *  net.md_5.bungee.api.chat.ClickEvent$Action
 *  net.md_5.bungee.api.chat.HoverEvent
 *  net.md_5.bungee.api.chat.HoverEvent$Action
 *  net.md_5.bungee.api.chat.KeybindComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  net.md_5.bungee.api.chat.TranslatableComponent
 */
package ch.njol.skript.util.chat;

import ch.njol.skript.Skript;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.skript.util.chat.MessageComponent;
import java.util.Arrays;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.KeybindComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

@Deprecated(since="2.15", forRemoval=true)
public class BungeeConverter {
    private static boolean HAS_FONT_SUPPORT = Skript.methodExists(BaseComponent.class, "setFont", String.class);

    @Deprecated(since="2.15", forRemoval=true)
    public static BaseComponent convert(MessageComponent origin) {
        KeybindComponent base;
        if (origin.translation != null) {
            String[] strings = origin.translation.split(":");
            String key = strings[0];
            base = new TranslatableComponent(key, (Object[])Arrays.copyOfRange(strings, 1, strings.length, Object[].class));
            base.addExtra((BaseComponent)new TextComponent(origin.text));
        } else if (origin.keybind != null) {
            base = new KeybindComponent(origin.keybind);
            base.addExtra((BaseComponent)new TextComponent(origin.text));
        } else {
            base = new TextComponent(origin.text);
        }
        base.setBold(Boolean.valueOf(origin.bold));
        base.setItalic(Boolean.valueOf(origin.italic));
        base.setUnderlined(Boolean.valueOf(origin.underlined));
        base.setStrikethrough(Boolean.valueOf(origin.strikethrough));
        base.setObfuscated(Boolean.valueOf(origin.obfuscated));
        if (origin.color != null) {
            base.setColor(origin.color);
        }
        base.setInsertion(origin.insertion);
        if (origin.clickEvent != null) {
            base.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf((String)origin.clickEvent.action.spigotName), origin.clickEvent.value));
        }
        if (origin.hoverEvent != null) {
            base.setHoverEvent(new HoverEvent(HoverEvent.Action.valueOf((String)origin.hoverEvent.action.spigotName), BungeeConverter.convert(ChatMessages.parse(origin.hoverEvent.value))));
        }
        if (origin.font != null && HAS_FONT_SUPPORT) {
            base.setFont(origin.font);
        }
        return base;
    }

    @Deprecated(since="2.15", forRemoval=true)
    public static BaseComponent[] convert(List<MessageComponent> origins) {
        return BungeeConverter.convert(origins.toArray(new MessageComponent[0]));
    }

    @Deprecated(since="2.15", forRemoval=true)
    public static BaseComponent[] convert(MessageComponent[] origins) {
        BaseComponent[] bases = new BaseComponent[origins.length];
        for (int i = 0; i < origins.length; ++i) {
            bases[i] = BungeeConverter.convert(origins[i]);
        }
        return bases;
    }
}

