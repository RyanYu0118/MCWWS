/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptEvent
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.parser.ParserInstance
 *  ch.njol.skript.lang.util.SimpleEvent
 *  ch.njol.util.StringUtils
 *  org.bukkit.event.Event
 *  org.skriptlang.skript.lang.entry.EntryContainer
 *  org.skriptlang.skript.lang.entry.EntryValidator
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 */
package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.util.StringUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.expression.ConstantGetEvent;
import org.skriptlang.reflect.syntax.expression.ConstantSyntaxInfo;
import org.skriptlang.reflect.syntax.expression.elements.CustomExpression;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class StructCustomConstant
extends CustomSyntaxStructure<ConstantSyntaxInfo> {
    private static final CustomSyntaxStructure.DataTracker<ConstantSyntaxInfo> dataTracker;

    @Override
    protected CustomSyntaxStructure.DataTracker<ConstantSyntaxInfo> getDataTracker() {
        return dataTracker;
    }

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        String option = ((MatchResult)parseResult.regexes.get(0)).group();
        SectionNode sectionNode = (SectionNode)entryContainer.get("get", SectionNode.class, false);
        this.getParser().setCurrentEvent("custom constant getter", new Class[]{ConstantGetEvent.class});
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
        Trigger getter = new Trigger(this.getParser().getCurrentScript(), "get @{" + option + "}", (SkriptEvent)new SimpleEvent(), items);
        StructCustomConstant.computeOption(option, getter);
        return true;
    }

    private static void computeOption(String option, Trigger getter) {
        ConstantGetEvent constantEvent = new ConstantGetEvent(0, null);
        getter.execute((Event)constantEvent);
        String result = StringUtils.join((Object[])constantEvent.getOutput());
        SkriptReflection.getOptions(ParserInstance.get().getCurrentScript()).put(option, result);
    }

    static {
        Skript.registerStructure(StructCustomConstant.class, (EntryValidator)EntryValidator.builder().addSection("get", false).missingRequiredEntryMessage(key -> "Computed options don't work without a get section").build(), (String[])new String[]{"option <.+>"});
        dataTracker = new CustomSyntaxStructure.DataTracker();
        Skript.registerExpression(CustomExpression.class, Object.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"this is here because at least one pattern is required"});
        Optional<SyntaxInfo> info = SkriptMirror.getAddonInstance().syntaxRegistry().elements().stream().filter(i -> Expression.class.isAssignableFrom(i.type())).filter(i -> i.type() == CustomExpression.class).findFirst();
        info.ifPresent(dataTracker::setInfo);
        dataTracker.setSyntaxKey(SyntaxRegistry.EXPRESSION);
    }
}

