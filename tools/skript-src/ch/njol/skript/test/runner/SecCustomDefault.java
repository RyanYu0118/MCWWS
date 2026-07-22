/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.DefaultValueData;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class SecCustomDefault
extends Section {
    Literal<?> value;
    ClassInfo<?> type;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        this.value = (Literal)LiteralUtils.defendExpression(expressions[0]);
        this.type = (ClassInfo)((Literal)expressions[1]).getSingle();
        Class type = this.type.getC();
        if (!type.isAssignableFrom(this.value.getReturnType())) {
            Skript.error("The value expression returns an invalid type: expected " + type.getSimpleName() + ", got " + this.value.getReturnType().getSimpleName());
            return true;
        }
        DefaultValueData data = this.getParser().getData(DefaultValueData.class);
        data.addDefaultValue(type, (DefaultExpression)((Object)this.value));
        this.loadCode(sectionNode);
        data.removeDefaultValue(type);
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        return this.walk(event, true);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "run with custom default value " + this.value.toString(event, debug) + " for " + this.type.toString(event, debug);
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerSection(SecCustomDefault.class, "run with custom default value %*object% for %*classinfo%");
        }
    }
}

