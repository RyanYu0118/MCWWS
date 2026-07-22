/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.skriptlang.skript.common.function.FunctionReference;

final class FunctionArgumentParser {
    private static final Pattern PART_PATTERN = Pattern.compile("(?:\\s*(?<name>[_a-zA-Z0-9]+):)?(?<value>.+)");
    private final String args;
    private final List<FunctionReference.Argument<String>> arguments = new ArrayList<FunctionReference.Argument<String>>();
    private int index = 0;

    public FunctionArgumentParser(String args) {
        this.args = args;
        this.parse();
    }

    private void parse() {
        if (this.args.isEmpty()) {
            return;
        }
        int next = 0;
        while (next < this.args.length()) {
            if ((next = SkriptParser.next(this.args, next, ParseContext.DEFAULT)) == -1) {
                this.index = 0;
                next = this.args.length();
            }
            if (next < this.args.length() && this.args.charAt(next) != ',') continue;
            String part = this.args.substring(this.index, next);
            this.index = next + 1;
            Matcher matcher = PART_PATTERN.matcher(part);
            if (!matcher.matches()) continue;
            String name = matcher.group("name");
            String value = matcher.group("value");
            if (name == null) {
                this.arguments.add(new FunctionReference.Argument<String>(FunctionReference.ArgumentType.UNNAMED, null, value.trim(), value));
                continue;
            }
            this.arguments.add(new FunctionReference.Argument<String>(FunctionReference.ArgumentType.NAMED, name.trim(), value.trim(), name + ":" + value));
        }
    }

    public FunctionReference.Argument<String>[] getArguments() {
        return this.arguments.toArray(new FunctionReference.Argument[0]);
    }
}

