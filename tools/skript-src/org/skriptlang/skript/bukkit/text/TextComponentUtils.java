/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.ChatColor
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.text;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;

public final class TextComponentUtils {
    private static final ConverterInfo<Object, Component> OBJECT_COMPONENT_CONVERTER = new ConverterInfo<Object, Component>(Object.class, Component.class, TextComponentUtils::from, 0);
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("[&\u00a7]x(?:[&\u00a7][a-f0-9]){6}");

    public static Component from(Object message) {
        Object object = message;
        Objects.requireNonNull(object);
        Object object2 = object;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Component.class, String.class}, (Object)object2, n)) {
            case 0 -> {
                Component component = (Component)object2;
                yield component;
            }
            case 1 -> {
                String string = (String)object2;
                yield TextComponentParser.instance().parseSafe(string);
            }
            default -> Component.text((String)Classes.toString(message));
        };
    }

    public static Component joinByNewLine(Component ... components) {
        Component combined = components[0];
        for (int i = 1; i < components.length; ++i) {
            combined = combined.appendNewline().append(components[i]);
        }
        return combined.compact();
    }

    public static Component appendToEnd(Component base, Component appendee) {
        return TextComponentUtils.appendToLastChild(base, appendee).compact();
    }

    private static Component appendToLastChild(Component base, Component appendee) {
        ArrayList<Component> baseChildren = base.children();
        if (baseChildren.isEmpty()) {
            return base.append(appendee);
        }
        baseChildren = new ArrayList<Component>(baseChildren);
        baseChildren.addLast(TextComponentUtils.appendToLastChild((Component)baseChildren.removeLast(), appendee));
        return base.children(baseChildren);
    }

    public static String replaceLegacyFormattingCodes(String text) {
        if (!text.contains("&") && !text.contains("\u00a7")) {
            return text;
        }
        text = LEGACY_HEX_PATTERN.matcher(text).replaceAll(result -> {
            String hex = result.group();
            StringBuilder replacement = new StringBuilder();
            replacement.append("<#");
            for (int i = 3; i <= 13; i += 2) {
                replacement.append(hex.charAt(i));
            }
            replacement.append('>');
            return Matcher.quoteReplacement(replacement.toString());
        });
        text = TextComponentParser.LEGACY_CODE_PATTERN.matcher(text).replaceAll(result -> {
            String backslashes = result.group(1);
            if (backslashes.length() % 2 == 1) {
                return Matcher.quoteReplacement(result.group().substring(1));
            }
            if (!backslashes.isEmpty()) {
                backslashes = backslashes.substring(1);
            }
            StringBuilder replacement = new StringBuilder(backslashes);
            ChatColor color = ChatColor.getByChar((char)result.group(2).charAt(1));
            assert (color != null);
            replacement.append('<').append(color.asBungee().getName()).append('>');
            return Matcher.quoteReplacement(replacement.toString());
        });
        return text;
    }

    @Nullable
    public static Expression<? extends Component> asComponentExpression(Expression<?> expression) {
        Expression componentExpression;
        if (!LiteralUtils.canInitSafely(expression = LiteralUtils.defendExpression(expression))) {
            return null;
        }
        boolean canReturnComponent = Arrays.stream(expression.possibleReturnTypes()).allMatch(type -> type != Object.class && Converters.converterExists(type, Component.class));
        if (canReturnComponent && (componentExpression = expression.getConvertedExpression(Component.class)) != null) {
            return componentExpression;
        }
        return new ConvertedExpression<Object, Component>(expression, Component.class, OBJECT_COMPONENT_CONVERTER);
    }

    private TextComponentUtils() {
    }
}

