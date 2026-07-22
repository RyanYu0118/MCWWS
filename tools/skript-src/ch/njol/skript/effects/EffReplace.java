/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Replace")
@Description(value={"Replaces all occurrences of a given text or regex with another text. Please note that you can only change variables and a few expressions, e.g. a <a href='/#ExprMessage'>message</a> or a line of a sign."})
@Example.Examples(value={@Example(value="replace \"<item>\" in {_msg} with \"[%name of player's tool%]\""), @Example(value="replace every \"&\" with \"\u00a7\" in line 1 of targeted block"), @Example(value="# Very simple chat censor\non chat:\n\treplace all \"idiot\" and \"noob\" with \"****\" in the message\n\tregex replace \"\b(idiot|noob)\b\" with \"****\" in the message # Regex version using word boundaries for better results\n"), @Example(value="replace all stone and dirt in player's inventory and player's top inventory with diamond")})
@Since(value={"2.0, 2.2-dev24 (multiple strings, items in inventory), 2.5 (replace first, case sensitivity), 2.10 (regex)"})
public class EffReplace
extends Effect {
    private Expression<?> haystack;
    private Expression<?> needles;
    private Expression<?> replacement;
    private boolean replaceString;
    private boolean replaceRegex;
    private boolean replaceFirst;
    private boolean caseSensitive = false;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.haystack = expressions[1 + matchedPattern % 2];
        this.replaceString = matchedPattern < 4;
        this.replaceFirst = parseResult.hasTag("first");
        boolean bl = this.replaceRegex = matchedPattern == 2 || matchedPattern == 3;
        if (this.replaceString) {
            Expression<?> newHaystack = Changer.ChangerUtils.acceptsChangeWithConverters(this.haystack, Changer.ChangeMode.SET, String.class);
            if (newHaystack == null) {
                Skript.error(this.haystack.toString(null, Skript.debug()) + " cannot be changed to a text and can thus not have parts replaced");
                return false;
            }
            this.haystack = newHaystack;
        }
        if (SkriptConfig.caseSensitive.value().booleanValue() || parseResult.hasTag("case")) {
            this.caseSensitive = true;
        }
        this.needles = expressions[0];
        this.replacement = expressions[2 - matchedPattern % 2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Object[] needles = this.needles.getAll(event);
        Expression<T>[] expressionArray = this.haystack;
        if (expressionArray instanceof ExpressionList) {
            ExpressionList list = (ExpressionList)expressionArray;
            for (Expression haystackExpr : list.getExpressions()) {
                this.replace(event, needles, haystackExpr);
            }
        } else {
            this.replace(event, needles, this.haystack);
        }
    }

    private void replace(Event event, Object[] needles, Expression<?> haystackExpr) {
        ?[] haystack = haystackExpr.getAll(event);
        Object replacement = this.replacement.getSingle(event);
        if (replacement == null || haystack == null || haystack.length == 0 || needles == null || needles.length == 0) {
            return;
        }
        if (this.replaceString) {
            Function<String, String> replaceFunction = this.getReplaceFunction(needles, (String)replacement);
            haystackExpr.changeInPlace(event, replaceFunction);
        } else {
            for (Inventory inventory : (Inventory[])haystack) {
                for (ItemType needle : (ItemType[])needles) {
                    for (Map.Entry entry : inventory.all(needle.getMaterial()).entrySet()) {
                        ItemStack newItemStack;
                        int slot = (Integer)entry.getKey();
                        ItemStack itemStack = (ItemStack)entry.getValue();
                        if (!new ItemType(itemStack).isSimilar(needle) || (newItemStack = ((ItemType)replacement).getRandom()) == null) continue;
                        newItemStack.setAmount(itemStack.getAmount());
                        inventory.setItem(slot, newItemStack);
                    }
                }
            }
        }
    }

    @NotNull
    private Function<String, String> getReplaceFunction(Object[] needles, String replacement) {
        Function<String, String> replaceFunction;
        if (this.replaceRegex) {
            ArrayList<Pattern> patterns = new ArrayList<Pattern>(needles.length);
            for (Object needle : needles) {
                try {
                    patterns.add(Pattern.compile((String)needle));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            replaceFunction = haystackString -> {
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher((CharSequence)haystackString);
                    if (this.replaceFirst) {
                        haystackString = matcher.replaceFirst(replacement);
                        continue;
                    }
                    haystackString = matcher.replaceAll(replacement);
                }
                return haystackString;
            };
        } else {
            replaceFunction = this.replaceFirst ? haystackString -> {
                for (Object needle : needles) {
                    assert (needle != null);
                    haystackString = StringUtils.replaceFirst(haystackString, (String)needle, Matcher.quoteReplacement(replacement), this.caseSensitive);
                }
                return haystackString;
            } : haystackString -> {
                for (Object needle : needles) {
                    assert (needle != null);
                    haystackString = StringUtils.replace(haystackString, (String)needle, replacement, this.caseSensitive);
                }
                return haystackString;
            };
        }
        return replaceFunction;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"replace");
        if (this.replaceFirst) {
            builder.append((Object)"the first");
        }
        if (this.replaceRegex) {
            builder.append((Object)"regex");
        }
        builder.append(this.needles, "in", this.haystack, "with", this.replacement);
        if (this.caseSensitive) {
            builder.append((Object)"with case sensitivity");
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffReplace.class, "replace [(all|every)|first:[the] first] %strings% in %strings% with %string% [case:with case sensitivity]", "replace [(all|every)|first:[the] first] %strings% with %string% in %strings% [case:with case sensitivity]", "(replace [with|using] regex|regex replace) %strings% in %strings% with %string%", "(replace [with|using] regex|regex replace) %strings% with %string% in %strings%", "replace [all|every] %itemtypes% in %inventories% with %itemtype%", "replace [all|every] %itemtypes% with %itemtype% in %inventories%");
    }
}

