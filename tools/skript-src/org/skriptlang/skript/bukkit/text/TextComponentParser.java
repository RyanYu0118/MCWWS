/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextReplacementConfig
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.StyleBuilderApplicable
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.format.TextDecoration$State
 *  net.kyori.adventure.text.minimessage.Context
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.kyori.adventure.text.minimessage.ParsingException
 *  net.kyori.adventure.text.minimessage.tag.Inserting
 *  net.kyori.adventure.text.minimessage.tag.ParserDirective
 *  net.kyori.adventure.text.minimessage.tag.Tag
 *  net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
 *  net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
 *  net.kyori.adventure.text.minimessage.tag.standard.StandardTags
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.text;

import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.StyleBuilderApplicable;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.ParserDirective;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentUtils;

public final class TextComponentParser {
    private static final TextComponentParser INSTANCE;
    private static final Pattern MULTI_WORD_COLOR_PATTERN;
    private static final Pattern LEGACY_DOUBLE_HASHTAG_PATTERN;
    static final Pattern LEGACY_CODE_PATTERN;
    private final MiniMessage parser = MiniMessage.builder().strict(false).tags(TagResolver.builder().resolvers(new TagResolver[]{StandardTags.defaults()}).resolver(this.createSkriptTagResolver(false)).build()).build();
    private final MiniMessage safeParser = MiniMessage.builder().strict(false).tags(TagResolver.builder().resolver(new TagResolver(){

        @Nullable
        public Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
            return TextComponentParser.this.safeTagResolver.resolve(name, arguments, ctx);
        }

        public boolean has(@NotNull String name) {
            return TextComponentParser.this.safeTagResolver.has(name);
        }
    }).resolver(this.createSkriptTagResolver(true)).build()).build();
    private TagResolver safeTagResolver = TagResolver.empty();
    private final Map<String, SkriptTag> simplePlaceholders = new HashMap<String, SkriptTag>();
    private final Set<SkriptTagResolver> resolvers = new HashSet<SkriptTagResolver>();
    private LinkParseMode linkParseMode = LinkParseMode.DISABLED;
    private boolean colorsCauseReset = false;

    public static TextComponentParser instance() {
        return INSTANCE;
    }

    private static Tag disableUnsetStyling(Tag tag) {
        if (!(tag instanceof Inserting)) {
            return tag;
        }
        Inserting insertingTag = (Inserting)tag;
        Component content = insertingTag.value();
        HashMap<TextDecoration, TextDecoration.State> decorations = new HashMap<TextDecoration, TextDecoration.State>(content.decorations());
        decorations.replaceAll((decoration, state) -> state == TextDecoration.State.NOT_SET ? TextDecoration.State.FALSE : state);
        content = content.decorations(decorations);
        if (insertingTag.allowsChildren()) {
            return Tag.inserting((Component)content);
        }
        return Tag.selfClosingInserting((Component)content);
    }

    TextComponentParser() {
        this.registerCompatibilityTags();
    }

    private TagResolver createSkriptTagResolver(final boolean isSafeMode) {
        return new TagResolver(){
            final /* synthetic */ TextComponentParser this$0;
            {
                this.this$0 = this$0;
            }

            @Nullable
            public Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
                Tag colorTag;
                SkriptTag simple = this.this$0.simplePlaceholders.get(name);
                if (simple != null && this.isSafe(name, simple.parent)) {
                    if (this.this$0.colorsCauseReset && simple.reset) {
                        return TextComponentParser.disableUnsetStyling(simple.tag);
                    }
                    return simple.tag;
                }
                for (SkriptTagResolver skriptResolver : this.this$0.resolvers) {
                    if (!this.isSafe(name, skriptResolver.parent) || !skriptResolver.resolver.has(name)) continue;
                    return skriptResolver.resolver.resolve(name, arguments, ctx);
                }
                if (this.this$0.colorsCauseReset && StandardTags.color().has(name) && (colorTag = StandardTags.color().resolve(name, arguments, ctx)) != null) {
                    return TextComponentParser.disableUnsetStyling(colorTag);
                }
                return null;
            }

            public boolean has(@NotNull String name) {
                SkriptTag simple = this.this$0.simplePlaceholders.get(name);
                if (simple != null) {
                    return this.isSafe(name, simple.parent);
                }
                for (SkriptTagResolver skriptResolver : this.this$0.resolvers) {
                    if (!this.isSafe(name, skriptResolver.parent) || !skriptResolver.resolver.has(name)) continue;
                    return true;
                }
                return this.this$0.colorsCauseReset && StandardTags.color().has(name);
            }

            private boolean isSafe(String tag, @Nullable String parent) {
                return !isSafeMode || this.this$0.safeTagResolver.has(tag) || parent != null && this.this$0.safeTagResolver.has(parent);
            }
        };
    }

    public void setSafeTags(final String ... safeTags) {
        final ArrayList<TagResolver> resolvers = new ArrayList<TagResolver>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (String safeTag : safeTags) {
            try {
                MethodHandle handle = lookup.findStatic(StandardTags.class, safeTag, MethodType.methodType(TagResolver.class));
                resolvers.add(handle.invoke());
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        this.safeTagResolver = new TagResolver(){

            @Nullable
            public Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
                if (this.has(name)) {
                    return StandardTags.defaults().resolve(name, arguments, ctx);
                }
                return null;
            }

            public boolean has(@NotNull String name) {
                return resolvers.stream().anyMatch(resolver -> resolver.has(name)) || CollectionUtils.contains(safeTags, name);
            }
        };
    }

    public void registerPlaceholder(String name, Tag result) {
        this.registerPlaceholder(name, result, null, false);
    }

    public void registerPlaceholder(String name, Tag result, String parent) {
        this.registerPlaceholder(name, result, parent, false);
    }

    public void registerResettingPlaceholder(String name, Tag result) {
        this.registerPlaceholder(name, result, null, true);
    }

    public void registerResettingPlaceholder(String name, Tag result, String parent) {
        this.registerPlaceholder(name, result, parent, true);
    }

    private void registerPlaceholder(String name, Tag result, @Nullable String parent, boolean reset) {
        this.simplePlaceholders.put(name, new SkriptTag(result, parent, reset));
    }

    public void unregisterPlaceholder(String tag) {
        this.simplePlaceholders.remove(tag);
    }

    public void registerResolver(TagResolver resolver) {
        this.registerResolver(resolver, null);
    }

    public void registerResolver(TagResolver resolver, String parent) {
        this.resolvers.add(new SkriptTagResolver(resolver, parent));
    }

    public void unregisterResolver(TagResolver resolver) {
        this.resolvers.remove(new SkriptTagResolver(resolver, null));
    }

    public LinkParseMode linkParseMode() {
        return this.linkParseMode;
    }

    public void linkParseMode(LinkParseMode linkParseMode) {
        this.linkParseMode = linkParseMode;
    }

    public boolean colorsCauseReset() {
        return this.colorsCauseReset;
    }

    public void colorsCauseReset(boolean colorsCauseReset) {
        this.colorsCauseReset = colorsCauseReset;
    }

    public Component parse(Object message) {
        return this.parse(message, false);
    }

    public Component parseSafe(Object message) {
        return this.parse(message, true);
    }

    private Component parse(Object message, boolean safe) {
        Component component;
        String realMessage;
        String string = realMessage = message instanceof String ? (String)message : Classes.toString(message);
        if (realMessage.isEmpty()) {
            return Component.empty();
        }
        realMessage = this.reformatText(realMessage);
        Component component2 = component = safe ? this.safeParser.deserialize((Object)realMessage) : this.parser.deserialize((Object)realMessage);
        if (this.linkParseMode != LinkParseMode.DISABLED) {
            component = component.replaceText(this.linkParseMode.textReplacementConfig());
        }
        return component;
    }

    public String reformatText(String text) {
        text = MULTI_WORD_COLOR_PATTERN.matcher(text).replaceAll(result -> {
            if (result.group(1).length() % 2 == 1) {
                return Matcher.quoteReplacement(result.group());
            }
            String mappedTag = result.group(2).replace(" ", "_");
            if (this.simplePlaceholders.containsKey(mappedTag) || StandardTags.color().has(mappedTag)) {
                return Matcher.quoteReplacement(result.group(1) + "<" + mappedTag + ">");
            }
            return Matcher.quoteReplacement(result.group());
        });
        text = LEGACY_DOUBLE_HASHTAG_PATTERN.matcher(text).replaceAll(result -> {
            if (result.group(1).length() % 2 == 1) {
                return Matcher.quoteReplacement(result.group());
            }
            String mappedTag = result.group(2).substring(1);
            if (StandardTags.color().has(mappedTag)) {
                return Matcher.quoteReplacement("<" + mappedTag + ">");
            }
            return Matcher.quoteReplacement(result.group());
        });
        text = TextComponentUtils.replaceLegacyFormattingCodes(text);
        return text;
    }

    public String escape(String string) {
        if (string.contains("&") || string.contains("\u00a7")) {
            string = LEGACY_CODE_PATTERN.matcher(string).replaceAll(result -> {
                if (result.group(1).length() % 2 == 1) {
                    return Matcher.quoteReplacement(result.group());
                }
                return Matcher.quoteReplacement("\\" + result.group());
            });
        }
        string = LEGACY_DOUBLE_HASHTAG_PATTERN.matcher(string).replaceAll(result -> {
            if (result.group(1).length() % 2 == 1) {
                return Matcher.quoteReplacement(result.group());
            }
            String mappedTag = result.group(2).substring(1);
            if (StandardTags.color().has(mappedTag)) {
                return Matcher.quoteReplacement("\\<#" + mappedTag) + ">";
            }
            return Matcher.quoteReplacement(result.group());
        });
        return this.parser.escapeTags(string);
    }

    public String stripFormatting(String string) {
        return this.stripFormatting(string, false);
    }

    public String stripSafeFormatting(String string) {
        return this.stripFormatting(string, true);
    }

    private String stripFormatting(String string, boolean onlySafe) {
        String stripped;
        while (!string.equals(stripped = (onlySafe ? this.safeParser : this.parser).stripTags(this.reformatText(string)))) {
            string = stripped;
        }
        return string;
    }

    public String stripFormatting(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public String toString(Component component) {
        return (String)this.parser.serialize(component);
    }

    public String toLegacyString(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    private void registerCompatibilityTags() {
        this.registerResettingPlaceholder("dark_cyan", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.DARK_AQUA}), "dark_aqua");
        this.registerResettingPlaceholder("dark_turquoise", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.DARK_AQUA}), "dark_aqua");
        this.registerResettingPlaceholder("cyan", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.DARK_AQUA}), "dark_aqua");
        this.registerResettingPlaceholder("purple", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.DARK_PURPLE}), "dark_purple");
        this.registerResettingPlaceholder("dark_yellow", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GOLD}), "gold");
        this.registerResettingPlaceholder("orange", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GOLD}), "gold");
        this.registerResettingPlaceholder("light_grey", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GRAY}), "gray");
        this.registerResettingPlaceholder("light_gray", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GRAY}), "gray");
        this.registerResettingPlaceholder("silver", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GRAY}), "gray");
        this.registerResettingPlaceholder("dark_silver", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.DARK_GRAY}), "dark_gray");
        this.registerResettingPlaceholder("light_blue", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.BLUE}), "blue");
        this.registerResettingPlaceholder("indigo", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.BLUE}), "blue");
        this.registerResettingPlaceholder("light_green", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GREEN}), "green");
        this.registerResettingPlaceholder("lime_green", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GREEN}), "green");
        this.registerResettingPlaceholder("lime", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.GREEN}), "green");
        this.registerResettingPlaceholder("light_cyan", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.AQUA}), "aqua");
        this.registerResettingPlaceholder("light_aqua", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.AQUA}), "aqua");
        this.registerResettingPlaceholder("turquoise", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.AQUA}), "aqua");
        this.registerResettingPlaceholder("light_red", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.RED}), "red");
        this.registerResettingPlaceholder("pink", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.LIGHT_PURPLE}), "light_purple");
        this.registerResettingPlaceholder("magenta", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.LIGHT_PURPLE}), "light_purple");
        this.registerResettingPlaceholder("light_yellow", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{NamedTextColor.YELLOW}), "yellow");
        this.registerResettingPlaceholder("brown", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{TextColor.color((int)8606770)}), "color");
        this.registerPlaceholder("magic", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{TextDecoration.OBFUSCATED}), "obfuscated");
        this.registerPlaceholder("strike", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{TextDecoration.STRIKETHROUGH}), "strikethrough");
        this.registerPlaceholder("s", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{TextDecoration.STRIKETHROUGH}), "strikethrough");
        this.registerPlaceholder("underline", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{TextDecoration.UNDERLINED}), "underlined");
        this.registerPlaceholder("italics", Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{TextDecoration.ITALIC}), "italic");
        this.registerPlaceholder("r", ParserDirective.RESET, "reset");
        this.registerResolver(TagResolver.resolver(Set.of("open_url", "link", "url"), (argumentQueue, context) -> {
            String url = argumentQueue.popOr("A link tag must have an argument of the url").value();
            return Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{ClickEvent.openUrl((String)url)});
        }), "click");
        this.registerResolver(TagResolver.resolver(Set.of("run_command", "command", "cmd"), (argumentQueue, context) -> {
            String command = argumentQueue.popOr("A run command tag must have an argument of the command to execute").value();
            return Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{ClickEvent.runCommand((String)command)});
        }), "click");
        this.registerResolver(TagResolver.resolver(Set.of("suggest_command", "sgt"), (argumentQueue, context) -> {
            String command = argumentQueue.popOr("A suggest command tag must have an argument of the command to suggest").value();
            return Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{ClickEvent.suggestCommand((String)command)});
        }), "click");
        this.registerResolver(TagResolver.resolver(Set.of("change_page"), (argumentQueue, context) -> {
            int page;
            String rawPage = argumentQueue.popOr("A change page tag must have an argument of the page number").value();
            try {
                page = Integer.parseInt(rawPage);
            }
            catch (NumberFormatException e) {
                throw context.newException(e.getMessage(), argumentQueue);
            }
            return Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{ClickEvent.changePage((int)page)});
        }), "click");
        this.registerResolver(TagResolver.resolver(Set.of("copy_to_clipboard", "copy", "clipboard"), (argumentQueue, context) -> {
            String string = argumentQueue.popOr("A copy to clipboard tag must have an argument of the string to copy").value();
            return Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{ClickEvent.copyToClipboard((String)string)});
        }), "click");
        this.registerResolver(TagResolver.resolver(Set.of("show_text", "tooltip", "ttp"), (argumentQueue, context) -> {
            String tooltip = argumentQueue.popOr("A tooltip tag must have an argument of the message to show").value();
            return Tag.styling((StyleBuilderApplicable[])new StyleBuilderApplicable[]{HoverEvent.showText((Component)context.deserialize(tooltip))});
        }), "hover");
        this.registerResolver(TagResolver.resolver(Set.of("f"), (argumentQueue, context) -> StandardTags.font().resolve("font", argumentQueue, context)), "font");
        this.registerResolver(TagResolver.resolver(Set.of("insertion", "ins"), (argumentQueue, context) -> StandardTags.insertion().resolve("insert", argumentQueue, context)), "insert");
        this.registerResolver(TagResolver.resolver(Set.of("keybind"), (argumentQueue, context) -> StandardTags.keybind().resolve("key", argumentQueue, context)), "key");
        Pattern unicodePattern = Pattern.compile("[0-9a-f]{4,}");
        this.registerResolver(TagResolver.resolver(Set.of("unicode", "u"), (argumentQueue, context) -> {
            String argument = argumentQueue.popOr("A unicode tag must have an argument of the unicode").value();
            Matcher matcher = unicodePattern.matcher(argument.toLowerCase(Locale.ENGLISH));
            if (!matcher.matches()) {
                throw context.newException("Invalid unicode tag");
            }
            String unicode = Character.toString(Integer.parseInt(matcher.group(), 16));
            return Tag.selfClosingInserting((Component)Component.text((String)unicode));
        }));
    }

    static {
        MULTI_WORD_COLOR_PATTERN = Pattern.compile("(\\\\*)<([a-zA-Z]+ [a-zA-Z]+)>");
        LEGACY_DOUBLE_HASHTAG_PATTERN = Pattern.compile("(\\\\*)<(##[a-f0-9]{6})>");
        LEGACY_CODE_PATTERN = Pattern.compile("(\\\\*)([&\u00a7][a-f0-9klomnr])");
        INSTANCE = new TextComponentParser();
    }

    public static enum LinkParseMode {
        DISABLED(null),
        STRICT((TextReplacementConfig)TextReplacementConfig.builder().match(Pattern.compile("https?://[-\\w.]+\\.\\w{2,}(?:/\\S*)?")).replacement(url -> url.clickEvent(ClickEvent.openUrl((String)url.content()))).build()),
        LENIENT((TextReplacementConfig)TextReplacementConfig.builder().match(Pattern.compile("(?:https?://)?[-\\w.]+\\.\\w{2,}(?:/\\S*)?")).replacement(url -> url.clickEvent(ClickEvent.openUrl((String)url.content()))).build());

        private final TextReplacementConfig textReplacementConfig;

        private LinkParseMode(TextReplacementConfig textReplacementConfig) {
            this.textReplacementConfig = textReplacementConfig;
        }

        public TextReplacementConfig textReplacementConfig() {
            return this.textReplacementConfig;
        }
    }

    private record SkriptTag(Tag tag, @Nullable String parent, boolean reset) {
    }

    private record SkriptTagResolver(TagResolver resolver, @Nullable String parent) {
        @Override
        public boolean equals(Object obj) {
            return this.resolver.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.resolver.hashCode();
        }
    }
}

