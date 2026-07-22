/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.effects.EffReturn
 *  org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos$Event
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 *  org.skriptlang.skript.registration.SyntaxRegistry$Key
 *  org.skriptlang.skript.util.Priority
 */
package com.btk5h.skriptmirror;

import ch.njol.skript.effects.EffReturn;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.EffExpressionStatement;
import com.btk5h.skriptmirror.skript.custom.ExprMatchedPattern;
import java.util.Optional;
import java.util.function.Predicate;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.condition.elements.CustomCondition;
import org.skriptlang.reflect.syntax.condition.elements.StructCustomCondition;
import org.skriptlang.reflect.syntax.effect.elements.CustomEffect;
import org.skriptlang.reflect.syntax.effect.elements.StructCustomEffect;
import org.skriptlang.reflect.syntax.expression.elements.CustomExpression;
import org.skriptlang.reflect.syntax.expression.elements.StructCustomExpression;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public class ParseOrderWorkarounds {
    private static final Priority POSITION = Priority.before((Priority)SyntaxInfo.PATTERN_MATCHES_EVERYTHING);
    private static final String[] PARSE_ORDER = new String[]{EffExpressionStatement.class.getCanonicalName(), CustomEffect.class.getCanonicalName(), CustomCondition.class.getCanonicalName(), CustomExpression.class.getCanonicalName(), "com.w00tmast3r.skquery.elements.conditions.CondBoolean", "com.pie.tlatoani.Miscellaneous.CondBoolean", "us.tlatoani.tablisknu.core.base.CondBoolean", "com.pie.tlatoani.CustomEvent.EvtCustomEvent", EffReturn.class.getCanonicalName(), ExprMatchedPattern.class.getCanonicalName(), "ch.njol.skript.effects.EffContinue", "com.ankoki.skjade.elements.conditions.CondBoolean"};

    public static void reorderSyntax() {
        for (String c : PARSE_ORDER) {
            ParseOrderWorkarounds.ensureLast(SyntaxRegistry.CONDITION, o -> o.type().getName().equals(c));
            ParseOrderWorkarounds.ensureLast(SyntaxRegistry.EFFECT, o -> o.type().getName().equals(c));
            ParseOrderWorkarounds.ensureLast(SyntaxRegistry.EXPRESSION, o -> o.type().getName().equals(c));
            ParseOrderWorkarounds.ensureLast(BukkitSyntaxInfos.Event.KEY, o -> o.type().getName().equals(c));
            ParseOrderWorkarounds.ensureLast(SyntaxRegistry.STRUCTURE, o -> o.type().getName().equals(c));
        }
    }

    private static <T> void ensureLast(SyntaxRegistry.Key<? extends SyntaxInfo<? extends T>> elementKey, Predicate<SyntaxInfo<? extends T>> checker) {
        SyntaxRegistry syntaxRegistry = SkriptMirror.getAddonInstance().syntaxRegistry();
        Optional<SyntaxInfo<SyntaxInfo>> optionalE = syntaxRegistry.syntaxes(elementKey).stream().filter(checker).findFirst();
        optionalE.ifPresent(value -> {
            syntaxRegistry.unregister(elementKey, value);
            SyntaxInfo newInfo = value.toBuilder().priority(POSITION).build();
            syntaxRegistry.register(elementKey, newInfo);
            CustomSyntaxStructure.DataTracker<CustomSyntaxStructure.SyntaxData> tracker = null;
            if (elementKey == SyntaxRegistry.EFFECT) {
                tracker = StructCustomEffect.dataTracker;
            } else if (elementKey == SyntaxRegistry.CONDITION) {
                tracker = StructCustomCondition.dataTracker;
            } else if (elementKey == SyntaxRegistry.EXPRESSION) {
                tracker = StructCustomExpression.dataTracker;
            }
            if (tracker != null && tracker.getInfo() == value) {
                tracker.setInfo(newInfo);
            }
        });
    }
}

