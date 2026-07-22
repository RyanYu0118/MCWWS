/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.text.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.skript.util.SkriptColor;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="String Colors")
@Description(value={"Retrieve the first, the last, or all of the color objects or color codes of a string.", "The retrieved color codes of the string will be formatted with the color symbol."})
@Example.Examples(value={@Example(value="set {_colors::*} to the string colors of \"<red>hey<blue>yo\""), @Example(value="set {_color} to the first string color code of \"&aGoodbye!\"\nsend \"%{_color}%Howdy!\" to all players\n")})
@Since(value={"2.11"})
public class ExprStringColor
extends PropertyExpression<String, Object> {
    private StringColor selectedState;
    private boolean getCodes;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprStringColor.class, Object.class).supplier(ExprStringColor::new)).priority(DEFAULT_PRIORITY)).addPatterns("[all [[of] the]|the] string colo[u]r[s] [code:code[s]] of %strings%", "[the] first string colo[u]r[s] [code:code[s]] of %strings%", "[the] last string colo[u]r[s] [code:code[s]] of %strings%")).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.selectedState = StringColor.values()[matchedPattern];
        this.getCodes = parseResult.hasTag("code");
        this.setExpr(exprs[0]);
        return true;
    }

    protected Object[] get(Event event, String[] source) {
        ArrayList<Object> colors = new ArrayList<Object>();
        for (String string : (String[])this.getExpr().getArray(event)) {
            colors.addAll(this.getColors(string));
        }
        return colors.toArray((Object[])Array.newInstance(this.getReturnType(), 0));
    }

    @Override
    public Class<?> getReturnType() {
        return this.getCodes ? String.class : Color.class;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        if (this.getCodes) {
            return CollectionUtils.array(String.class);
        }
        return CollectionUtils.array(SkriptColor.class, ColorRGB.class);
    }

    @Override
    public boolean isSingle() {
        return this.selectedState != StringColor.ALL && this.getExpr().isSingle();
    }

    @Override
    public Expression<?> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)(switch (this.selectedState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "all of the";
            case 1 -> "the first";
            case 2 -> "the last";
        }));
        if (this.getCodes) {
            builder.append((Object)"string color codes");
        } else {
            builder.append((Object)"string colors");
        }
        builder.append("of", this.getExpr());
        return builder.toString();
    }

    private List<Object> getColors(String string) {
        ArrayList<Object> colors = new ArrayList<Object>();
        int length = string.length();
        TextComponentParser textComponentParser = TextComponentParser.instance();
        for (int index = 0; index < length; ++index) {
            boolean isTag;
            boolean bl = isTag = string.charAt(index) == '<';
            if (!isTag && string.charAt(index) != '&' && string.charAt(index) != '\u00a7') continue;
            if (index + 1 == length) break;
            if (isTag) {
                SkriptColor color;
                String tag;
                int end = string.indexOf(62, index + 1);
                if (end == -1 || (tag = string.substring(index + 1, end)).isEmpty()) continue;
                if (tag.charAt(0) == '#') {
                    int tagLength = (tag = tag.substring(1)).length();
                    if (tagLength != 6 && tagLength != 8) continue;
                    colors.add(this.getCodes ? "<#" + tag + ">" : this.fromHex(tag));
                    if (this.selectedState == StringColor.FIRST) break;
                    index += tagLength + 2;
                    continue;
                }
                String enclosedTag = "<" + tag + ">skript";
                String parsed = textComponentParser.toLegacyString(textComponentParser.parseSafe(enclosedTag));
                if (parsed.equals(enclosedTag) || (color = SkriptColor.fromColorChar(parsed.charAt(1))) == null) continue;
                colors.add(this.getCodes ? "<" + tag + ">" : color);
                if (this.selectedState == StringColor.FIRST) break;
                index += tag.length() + 1;
                continue;
            }
            boolean checkHex = this.checkHex(string, index);
            SkriptColor checkChar = SkriptColor.fromColorChar(string.charAt(index + 1));
            if (checkHex) {
                String hexString = string.substring(index, index + 14);
                colors.add(this.getCodes ? hexString : this.fromHex(hexString));
                if (this.selectedState == StringColor.FIRST) break;
                index += 13;
                continue;
            }
            if (checkChar == null) continue;
            String colorString = string.substring(index, index + 2);
            colors.add(this.getCodes ? colorString : checkChar);
            if (this.selectedState == StringColor.FIRST) break;
            ++index;
        }
        if (this.selectedState == StringColor.LAST && !colors.isEmpty()) {
            Object last = colors.getLast();
            colors.clear();
            colors.add(last);
        }
        return colors;
    }

    private boolean checkHex(String string, int index) {
        int i;
        int length = string.length();
        if (length < index + 12) {
            return false;
        }
        if (string.charAt(index + 1) != 'x') {
            return false;
        }
        for (i = index + 2; i <= index; i += 2) {
            if (string.charAt(i) == '&' || string.charAt(i) == '\u00a7') continue;
            return false;
        }
        for (i = index + 3; i <= index; i += 2) {
            char toCheck = string.charAt(i);
            if (toCheck < '0' || toCheck > 'f') {
                return false;
            }
            if (toCheck > '9' && toCheck < 'A') {
                return false;
            }
            if (toCheck <= 'F' || toCheck >= 'a') continue;
            return false;
        }
        return true;
    }

    private ColorRGB fromHex(@NotNull String hex) {
        int blue;
        int green;
        int red;
        if (hex.startsWith("&x") || hex.startsWith("\u00a7x")) {
            hex = hex.substring(2);
        }
        hex = hex.replaceAll("[\u00a7&]", "");
        int length = hex.length();
        int alpha = 255;
        if (length == 6) {
            red = Integer.parseInt(hex.substring(0, 2), 16);
            green = Integer.parseInt(hex.substring(2, 4), 16);
            blue = Integer.parseInt(hex.substring(4, 6), 16);
        } else if (length == 8) {
            alpha = Integer.parseInt(hex.substring(0, 2), 16);
            red = Integer.parseInt(hex.substring(2, 4), 16);
            green = Integer.parseInt(hex.substring(4, 6), 16);
            blue = Integer.parseInt(hex.substring(6, 8), 16);
        } else {
            throw new UnsupportedOperationException("Unsupported hex format - requires #RRGGBB or #AARRGGBB");
        }
        return ColorRGB.fromRGBA(red, green, blue, alpha);
    }

    private static enum StringColor {
        ALL,
        FIRST,
        LAST;

    }
}

