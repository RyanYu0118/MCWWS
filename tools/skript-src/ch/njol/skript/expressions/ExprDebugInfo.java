/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.registrations.Classes;
import org.jetbrains.annotations.Nullable;

@Name(value="Debug Info")
@Description(value={"Returns a string version of the given objects, but with their type attached:\n\tdebug info of 1, \"a\", 0.5 -> 1 (long), \"a\" (string), 0.5 (double)\nThis is intended to make debugging easier, not as a reliable method of getting the type of a value.\n"})
@Example(value="broadcast debug info of {list::*}")
@Since(value={"2.13"})
public class ExprDebugInfo
extends SimplePropertyExpression<Object, String> {
    @Override
    @Nullable
    public String convert(Object from) {
        Object toString = Classes.toString(from);
        ClassInfo<?> classInfo = Classes.getSuperClassInfo(from.getClass());
        String typeName = classInfo.getName().toString();
        if (from instanceof String) {
            toString = "\"" + (String)toString + "\"";
        }
        return (String)toString + " (" + typeName + ")";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "debug info";
    }

    static {
        ExprDebugInfo.register(ExprDebugInfo.class, String.class, "debug info[rmation]", "objects");
    }
}

