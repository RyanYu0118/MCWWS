/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Method Exists")
@Description(value={"Checks if a method exists"})
@Example(value="if method \"org.bukkit.Bukkit#getPluginCommand(java.lang.String)\" exists")
@Since(value={"2.7"})
@NoDoc
public class CondMethodExists
extends PropertyCondition<String> {
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("(?<class>.+)#(?<name>.+)\\((?<params>.*)\\)");
    private Expression<String> signatures;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.signatures = exprs[0];
        this.setExpr(this.signatures);
        this.setNegated(parseResult.hasTag("dont"));
        return true;
    }

    @Override
    public boolean check(String signature) {
        Matcher sigMatcher = SIGNATURE_PATTERN.matcher(signature);
        if (!sigMatcher.matches()) {
            return false;
        }
        try {
            Class<?> clazz = Class.forName(sigMatcher.group("class"));
            ArrayList parameters = new ArrayList();
            String rawParameters = sigMatcher.group("params");
            if (!StringUtils.isBlank((String)rawParameters)) {
                for (String parameter : rawParameters.split(",")) {
                    parameters.add(CondMethodExists.parseClass(parameter.trim()));
                }
            }
            return Skript.methodExists(clazz, sigMatcher.group("name"), parameters.toArray(new Class[0]));
        }
        catch (ClassNotFoundException exception) {
            return false;
        }
    }

    @Override
    protected String getPropertyName() {
        return "method exists";
    }

    @Override
    public Condition simplify() {
        if (!(this.signatures instanceof Literal)) {
            return this;
        }
        return SimplifiedCondition.fromCondition(this);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method " + this.signatures.toString(event, debug) + " exists";
    }

    private static Class<?> parseClass(String clazz) throws ClassNotFoundException {
        if (clazz.endsWith("[]")) {
            Class<?> baseClass = CondMethodExists.parseClass(clazz.substring(0, clazz.length() - 2));
            return Array.newInstance(baseClass, 0).getClass();
        }
        switch (clazz) {
            case "byte": {
                return Byte.TYPE;
            }
            case "short": {
                return Short.TYPE;
            }
            case "int": {
                return Integer.TYPE;
            }
            case "long": {
                return Long.TYPE;
            }
            case "float": {
                return Float.TYPE;
            }
            case "double": {
                return Double.TYPE;
            }
            case "boolean": {
                return Boolean.TYPE;
            }
            case "char": {
                return Character.TYPE;
            }
        }
        return Class.forName(clazz);
    }

    static {
        Skript.registerCondition(CondMethodExists.class, "method[s] %strings% [dont:do(esn't|n't)] exist[s]");
    }
}

