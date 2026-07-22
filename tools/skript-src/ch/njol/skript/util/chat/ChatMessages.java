/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.md_5.bungee.api.ChatColor
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.chat;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.localization.Language;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.chat.ChatCode;
import ch.njol.skript.util.chat.LinkParseMode;
import ch.njol.skript.util.chat.MessageComponent;
import ch.njol.skript.util.chat.SkriptChatCode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.15", forRemoval=true)
public class ChatMessages {
    public static LinkParseMode linkParseMode = LinkParseMode.DISABLED;
    public static boolean colorResetCodes = false;
    static final Map<String, ChatCode> codes = new HashMap<String, ChatCode>();
    static final Set<ChatCode> addonCodes = new HashSet<ChatCode>();
    static final ChatCode[] colorChars = new ChatCode[256];
    static final Pattern linkPattern = Pattern.compile("[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)");
    static final Gson gson;
    private static final Pattern HEX_COLOR_PATTERN;
    private static final Pattern ANY_COLOR_PATTERN;

    public static void registerListeners() {
        Language.addListener(() -> {
            codes.clear();
            Skript.debug("Parsing message style lang files");
            for (SkriptChatCode code : SkriptChatCode.values()) {
                assert (code != null);
                ChatMessages.registerChatCode(code);
            }
            for (ChatCode code : addonCodes) {
                assert (code != null);
                ChatMessages.registerChatCode(code);
            }
            ChatMessages.addColorChar('k', SkriptChatCode.obfuscated);
            ChatMessages.addColorChar('l', SkriptChatCode.bold);
            ChatMessages.addColorChar('m', SkriptChatCode.strikethrough);
            ChatMessages.addColorChar('n', SkriptChatCode.underlined);
            ChatMessages.addColorChar('o', SkriptChatCode.italic);
            ChatMessages.addColorChar('r', SkriptChatCode.reset);
        });
    }

    static void registerChatCode(ChatCode code) {
        String langName = code.getLangName();
        if (code.isLocalized()) {
            if (code.getColorCode() != null) {
                for (String name : Language.getList("colors." + langName + ".names")) {
                    codes.put(name, code);
                }
            } else {
                for (String name : Language.getList("chat styles." + langName)) {
                    codes.put(name, code);
                }
            }
        } else {
            codes.put(langName, code);
        }
        if (code.getColorChar() != '\u0000') {
            ChatMessages.addColorChar(code.getColorChar(), code);
        }
    }

    static void addColorChar(char code, ChatCode data) {
        ChatMessages.colorChars[code] = data;
        ChatMessages.colorChars[Character.toUpperCase((char)code)] = data;
    }

    public static List<MessageComponent> parse(String msg) {
        char[] chars = msg.toCharArray();
        ArrayList<MessageComponent> components = new ArrayList<MessageComponent>();
        MessageComponent current = new MessageComponent();
        components.add(current);
        StringBuilder curStr = new StringBuilder();
        boolean lastWasColor = true;
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            ChatCode code = null;
            String param = "";
            if (c == '<') {
                int end = -1;
                int angleBrackets = 1;
                for (int j = i + 1; j < chars.length; ++j) {
                    char c2 = chars[j];
                    if (c2 == '<') {
                        ++angleBrackets;
                    } else if (c2 == '>') {
                        --angleBrackets;
                    }
                    if (angleBrackets != 0) continue;
                    end = j;
                    break;
                }
                if (end != -1) {
                    String name;
                    String tag = msg.substring(i + 1, end);
                    if (tag.contains(":")) {
                        String[] split = tag.split(":", 2);
                        name = split[0];
                        param = split[1];
                    } else {
                        name = tag;
                    }
                    name = name.toLowerCase(Locale.ENGLISH);
                    boolean tryHex = name.startsWith("#");
                    ChatColor chatColor = null;
                    if (tryHex) {
                        chatColor = Utils.parseHexColor(name);
                        boolean bl = tryHex = chatColor != null;
                    }
                    if ((code = codes.get(name)) != null || tryHex) {
                        String text = curStr.toString();
                        curStr = new StringBuilder();
                        assert (text != null);
                        current.text = text;
                        MessageComponent old = current;
                        current = new MessageComponent();
                        components.add(current);
                        if (tryHex) {
                            current.color = chatColor;
                        } else if (code.getColorCode() != null) {
                            current.color = ChatColor.getByChar((char)code.getColorChar());
                        } else {
                            assert (param != null);
                            code.updateComponent(current, param);
                        }
                        ChatMessages.copyStyles(old, current);
                        i = end;
                        lastWasColor = true;
                        continue;
                    }
                }
            } else if (c == '&' || c == '\u00a7') {
                if (i == chars.length - 1) {
                    curStr.append(c);
                    continue;
                }
                char color = chars[i + 1];
                boolean tryHex = color == 'x';
                ChatColor chatColor = null;
                if (tryHex && i + 14 < chars.length) {
                    chatColor = Utils.parseHexColor(msg.substring(i + 2, i + 14).replace("&", "").replace("\u00a7", ""));
                    boolean bl = tryHex = chatColor != null;
                }
                if (color >= colorChars.length) {
                    curStr.append(c);
                    continue;
                }
                code = colorChars[color];
                if (code == null && !tryHex) {
                    curStr.append(c).append(color);
                } else {
                    String text = curStr.toString();
                    curStr = new StringBuilder();
                    assert (text != null);
                    current.text = text;
                    MessageComponent old = current;
                    current = new MessageComponent();
                    components.add(current);
                    if (tryHex) {
                        current.color = chatColor;
                        i += 12;
                    } else if (code.getColorCode() != null) {
                        current.color = ChatColor.getByChar((char)code.getColorChar());
                    } else {
                        code.updateComponent(current, param);
                    }
                    ChatMessages.copyStyles(old, current);
                }
                ++i;
                lastWasColor = true;
                continue;
            }
            if ((linkParseMode == LinkParseMode.STRICT || linkParseMode == LinkParseMode.LENIENT) && c == 'h') {
                String rest = msg.substring(i);
                String link = null;
                if (rest.startsWith("http://") || rest.startsWith("https://")) {
                    link = rest.split(" ", 2)[0];
                }
                if (link != null && !link.isEmpty()) {
                    String text = curStr.toString();
                    curStr = new StringBuilder();
                    assert (text != null);
                    current.text = text;
                    MessageComponent old = current;
                    current = new MessageComponent();
                    ChatMessages.copyStyles(old, current);
                    components.add(current);
                    SkriptChatCode.open_url.updateComponent(current, link);
                    current.text = link;
                    i += link.length() - 1;
                    current = new MessageComponent();
                    components.add(current);
                    continue;
                }
            } else if (linkParseMode == LinkParseMode.LENIENT && (lastWasColor || i == 0 || chars[i - 1] == ' ')) {
                String rest = msg.substring(i);
                String link = null;
                String potentialLink = rest.split(" ", 2)[0];
                if (linkPattern.matcher(potentialLink).matches()) {
                    link = potentialLink;
                }
                if (link != null && !link.isEmpty()) {
                    Object url = !link.startsWith("http://") && !link.startsWith("https://") ? "https://" + link : link;
                    String text = curStr.toString();
                    curStr = new StringBuilder();
                    assert (text != null);
                    current.text = text;
                    MessageComponent old = current;
                    current = new MessageComponent();
                    ChatMessages.copyStyles(old, current);
                    components.add(current);
                    SkriptChatCode.open_url.updateComponent(current, (String)url);
                    current.text = link;
                    i += link.length() - 1;
                    current = new MessageComponent();
                    components.add(current);
                    continue;
                }
            }
            curStr.append(c);
            lastWasColor = false;
        }
        String text = curStr.toString();
        assert (text != null);
        current.text = text;
        return components;
    }

    public static MessageComponent[] parseToArray(String msg) {
        return ChatMessages.parse(msg).toArray(new MessageComponent[0]);
    }

    public static List<MessageComponent> fromParsedString(String msg) {
        char[] chars = msg.toCharArray();
        ArrayList<MessageComponent> components = new ArrayList<MessageComponent>();
        MessageComponent current = new MessageComponent();
        components.add(current);
        StringBuilder curStr = new StringBuilder();
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            String param = "";
            if (c == '\u00a7') {
                if (i == chars.length - 1) {
                    curStr.append(c);
                    continue;
                }
                char color = chars[i + 1];
                boolean tryHex = color == 'x';
                ChatColor chatColor = null;
                if (tryHex && i + 14 < chars.length) {
                    chatColor = Utils.parseHexColor(msg.substring(i + 2, i + 14).replace("&", "").replace("\u00a7", ""));
                    boolean bl = tryHex = chatColor != null;
                }
                if (color >= colorChars.length) {
                    curStr.append(c);
                    continue;
                }
                ChatCode code = colorChars[color];
                if (code == null && !tryHex) {
                    curStr.append(c).append(color);
                } else {
                    String text = curStr.toString();
                    curStr = new StringBuilder();
                    current.text = text;
                    MessageComponent old = current;
                    current = new MessageComponent();
                    components.add(current);
                    if (tryHex) {
                        current.color = chatColor;
                        i += 12;
                    } else if (code.getColorCode() != null) {
                        current.color = ChatColor.getByChar((char)code.getColorChar());
                    } else {
                        code.updateComponent(current, param);
                    }
                    ChatMessages.copyStyles(old, current);
                }
                ++i;
                continue;
            }
            curStr.append(c);
        }
        current.text = curStr.toString();
        return components;
    }

    public static String toJson(String msg) {
        ComponentList componentList = new ComponentList(ChatMessages.parse(msg));
        String json = gson.toJson((Object)componentList);
        assert (json != null);
        return json;
    }

    public static String toJson(List<MessageComponent> components) {
        ComponentList componentList = new ComponentList(components);
        String json = gson.toJson((Object)componentList);
        assert (json != null);
        return json;
    }

    public static void copyStyles(MessageComponent from, MessageComponent to) {
        if (to.reset) {
            return;
        }
        if (to.color == null || !colorResetCodes) {
            if (!to.bold) {
                to.bold = from.bold;
            }
            if (!to.italic) {
                to.italic = from.italic;
            }
            if (!to.underlined) {
                to.underlined = from.underlined;
            }
            if (!to.strikethrough) {
                to.strikethrough = from.strikethrough;
            }
            if (!to.obfuscated) {
                to.obfuscated = from.obfuscated;
            }
            if (to.color == null) {
                to.color = from.color;
            }
        }
        if (to.clickEvent == null) {
            to.clickEvent = from.clickEvent;
        }
        if (to.insertion == null) {
            to.insertion = from.insertion;
        }
        if (to.hoverEvent == null) {
            to.hoverEvent = from.hoverEvent;
        }
        if (to.font == null) {
            to.font = from.font;
        }
    }

    public static void shareStyles(MessageComponent[] components) {
        MessageComponent previous = null;
        for (MessageComponent c : components) {
            if (previous != null) {
                assert (c != null);
                ChatMessages.copyStyles(previous, c);
            }
            previous = c;
        }
    }

    public static MessageComponent plainText(String str) {
        MessageComponent component = new MessageComponent();
        component.text = str;
        return component;
    }

    public static void registerAddonCode(@Nullable SkriptAddon addon, @Nullable ChatCode code) {
        Objects.requireNonNull(addon);
        Objects.requireNonNull(code);
        addonCodes.add(code);
        ChatMessages.registerChatCode(code);
    }

    public static String stripStyles(String text) {
        String plain;
        String previous;
        String result = text;
        do {
            previous = result;
            List<MessageComponent> components = ChatMessages.parse(result);
            StringBuilder builder = new StringBuilder();
            for (MessageComponent component : components) {
                if (component.translation != null) {
                    builder.append(component.translation);
                }
                if (component.keybind != null) {
                    builder.append(component.keybind);
                }
                builder.append(component.text);
            }
            plain = builder.toString();
        } while (!previous.equals(result = ANY_COLOR_PATTERN.matcher(plain = HEX_COLOR_PATTERN.matcher(plain).replaceAll("")).replaceAll("")));
        return result;
    }

    static {
        Gson nullableGson = new GsonBuilder().registerTypeAdapter(Boolean.TYPE, (Object)new MessageComponent.BooleanSerializer()).create();
        assert (nullableGson != null);
        gson = nullableGson;
        HEX_COLOR_PATTERN = Pattern.compile("[\u00a7&]x");
        ANY_COLOR_PATTERN = Pattern.compile("(?i)[&\u00a7][0-9a-folkrnm]");
    }

    private static class ComponentList {
        @Deprecated(since="2.3.0", forRemoval=true)
        public String text = "";
        @Nullable
        public List<MessageComponent> extra;

        public ComponentList(List<MessageComponent> components) {
            this.extra = components;
        }

        public ComponentList(MessageComponent[] components) {
            this.extra = Arrays.asList(components);
        }
    }
}

