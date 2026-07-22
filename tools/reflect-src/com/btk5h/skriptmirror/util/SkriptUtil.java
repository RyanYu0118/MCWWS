/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.ScriptLoader
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.config.Config
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionList
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.UnparsedLiteral
 *  ch.njol.skript.lang.parser.ParserInstance
 *  ch.njol.skript.log.RetainingLogHandler
 *  ch.njol.skript.log.SkriptLogger
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.skript.registrations.DefaultClasses
 *  ch.njol.skript.util.Utils
 *  ch.njol.util.NonNullPair
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.skriptlang.skript.lang.script.Script
 */
package com.btk5h.skriptmirror.util;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.DefaultClasses;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.script.Script;

public class SkriptUtil {
    public static <T> Expression<T> defendExpression(Expression<?> expr) {
        if (expr instanceof UnparsedLiteral) {
            Literal parsed = ((UnparsedLiteral)expr).getConvertedExpression(new Class[]{Object.class});
            return parsed == null ? expr : parsed;
        }
        if (expr instanceof ExpressionList) {
            Expression[] exprs = ((ExpressionList)expr).getExpressions();
            for (int i = 0; i < exprs.length; ++i) {
                exprs[i] = SkriptUtil.defendExpression(exprs[i]);
            }
        }
        return expr;
    }

    public static boolean hasUnparsedLiteral(Expression<?> expr) {
        if (expr instanceof UnparsedLiteral) {
            return true;
        }
        if (expr instanceof ExpressionList) {
            Expression[] expressions;
            for (Expression expression : expressions = ((ExpressionList)expr).getExpressions()) {
                if (!(expression instanceof UnparsedLiteral)) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean canInitSafely(Expression<?> ... expressions) {
        return Arrays.stream(expressions).filter(Objects::nonNull).noneMatch(SkriptUtil::hasUnparsedLiteral);
    }

    public static List<TriggerItem> getItemsFromNode(SectionNode node) {
        RetainingLogHandler log = SkriptLogger.startRetainingLog();
        try {
            ArrayList arrayList = ScriptLoader.loadItems((SectionNode)node);
            return arrayList;
        }
        finally {
            log.printLog();
            ParserInstance.get().deleteCurrentEvent();
        }
    }

    public static void clearSectionNode(SectionNode node) {
        ArrayList subNodes = new ArrayList();
        node.forEach(subNodes::add);
        subNodes.forEach(Node::remove);
    }

    public static File getCurrentScriptFile() {
        return Optional.ofNullable(SkriptUtil.getCurrentScript()).map(Script::getConfig).map(Config::getFile).orElse(null);
    }

    public static Script getCurrentScript() {
        return Optional.of(ParserInstance.get()).filter(ParserInstance::isActive).map(ParserInstance::getCurrentScript).orElse(null);
    }

    @NotNull
    public static ClassInfo<?> getUserClassInfo(String name) {
        NonNullPair wordData = Utils.getEnglishPlural((String)name);
        ClassInfo ci = Classes.getClassInfoNoError((String)((String)wordData.getFirst()));
        if (ci == null) {
            ci = Classes.getClassInfoFromUserInput((String)((String)wordData.getFirst()));
        }
        if (ci == null) {
            Skript.warning((String)String.format("'%s' is not a valid Skript type. Using 'object' instead.", name));
            return DefaultClasses.OBJECT;
        }
        return ci;
    }

    public static NonNullPair<ClassInfo<?>, Boolean> getUserClassInfoAndPlural(String name) {
        NonNullPair wordData = Utils.getEnglishPlural((String)name);
        ClassInfo<?> ci = SkriptUtil.getUserClassInfo(name);
        return new NonNullPair(ci, (Object)((Boolean)wordData.getSecond()));
    }

    public static String replaceUserInputPatterns(String name) {
        NonNullPair wordData = Utils.getEnglishPlural((String)name);
        ClassInfo<?> ci = SkriptUtil.getUserClassInfo(name);
        return Utils.toEnglishPlural((String)ci.getCodeName(), (boolean)((Boolean)wordData.getSecond()));
    }

    public static Function<Expression<?>, Object> unwrapWithEvent(Event e) {
        return expr -> expr.isSingle() ? expr.getSingle(e) : expr.getArray(e);
    }
}

