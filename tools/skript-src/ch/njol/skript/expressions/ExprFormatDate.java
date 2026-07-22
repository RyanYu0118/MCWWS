/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.util.Kleenean;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Formatted Date")
@Description(value={"Converts date to human-readable text format. By default, 'yyyy-MM-dd HH:mm:ss z' (e.g. '2018-03-30 16:03:12 +01') will be used. For reference, see this <a href=\"https://en.wikipedia.org/wiki/ISO_8601\">Wikipedia article</a>."})
@Example(value="command /date:\n\ttrigger:\n\t\tsend \"Full date: %now formatted human-readable%\" to sender\n\t\tsend \"Short date: %now formatted as \"yyyy-MM-dd\"%\" to sender\n")
@Since(value={"2.2-dev31, 2.7 (support variables in format)"})
public class ExprFormatDate
extends PropertyExpression<ch.njol.skript.util.Date, String> {
    private static final SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private SimpleDateFormat format;
    private Expression<String> customFormat;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean isSimpleString;
        this.setExpr(exprs[0]);
        this.customFormat = exprs[1];
        boolean bl = isSimpleString = this.customFormat instanceof VariableString && ((VariableString)this.customFormat).isSimple();
        if (this.customFormat instanceof Literal || isSimpleString) {
            String customFormatValue = isSimpleString ? ((VariableString)this.customFormat).toString(null) : (String)((Literal)this.customFormat).getSingle();
            if (customFormatValue != null) {
                try {
                    this.format = new SimpleDateFormat(customFormatValue);
                }
                catch (IllegalArgumentException e) {
                    Skript.error("Invalid date format: " + customFormatValue);
                    return false;
                }
            }
        } else if (this.customFormat == null) {
            this.format = DEFAULT_FORMAT;
        }
        return true;
    }

    protected String[] get(Event e, ch.njol.skript.util.Date[] source) {
        SimpleDateFormat format;
        if (this.customFormat != null && this.format == null) {
            String formatString = this.customFormat.getSingle(e);
            if (formatString == null) {
                return null;
            }
            try {
                format = new SimpleDateFormat(formatString);
            }
            catch (IllegalArgumentException ex) {
                return null;
            }
        } else {
            format = this.format;
        }
        return this.get(source, date -> format.format((Date)date));
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public Expression<? extends String> simplify() {
        if (this.getExpr() instanceof Literal && (this.customFormat == null || this.customFormat instanceof Literal)) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.getExpr().toString(e, debug) + " formatted as " + (this.customFormat != null ? this.customFormat.toString(e, debug) : (this.format != null ? this.format.toPattern() : DEFAULT_FORMAT.toPattern()));
    }

    static {
        Skript.registerExpression(ExprFormatDate.class, String.class, ExpressionType.PROPERTY, "%dates% formatted [human-readable] [(with|as) %-string%]", "[human-readable] formatted %dates% [(with|as) %-string%]");
    }
}

