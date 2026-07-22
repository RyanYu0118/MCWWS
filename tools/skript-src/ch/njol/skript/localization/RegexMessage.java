/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.localization;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jetbrains.annotations.Nullable;

public class RegexMessage
extends Message {
    private final String prefix;
    private final String suffix;
    private final int flags;
    @Nullable
    private Pattern pattern = null;
    public static final Pattern nop = Pattern.compile("(?!)");

    public RegexMessage(String key, @Nullable String prefix, @Nullable String suffix, int flags) {
        super(key);
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
        this.flags = flags;
    }

    public RegexMessage(String key, String prefix, String suffix) {
        this(key, prefix, suffix, 0);
    }

    public RegexMessage(String key, int flags) {
        this(key, "", "", flags);
    }

    public RegexMessage(String key) {
        this(key, "", "", 0);
    }

    @Nullable
    public Pattern getPattern() {
        this.validate();
        return this.pattern;
    }

    public Matcher matcher(String s) {
        Pattern p = this.getPattern();
        return p == null ? nop.matcher(s) : p.matcher(s);
    }

    public boolean matches(String s) {
        Pattern p = this.getPattern();
        return p == null ? false : p.matcher(s).matches();
    }

    public boolean find(String s) {
        Pattern p = this.getPattern();
        return p == null ? false : p.matcher(s).find();
    }

    @Override
    public String toString() {
        this.validate();
        return this.prefix + this.getValue() + this.suffix;
    }

    @Override
    protected void onValueChange() {
        try {
            this.pattern = Pattern.compile(this.prefix + this.getValue() + this.suffix, this.flags);
        }
        catch (PatternSyntaxException e) {
            Skript.error("Invalid Regex pattern '" + this.getValue() + "' found at '" + this.key + "' in the " + Language.getName() + " language file: " + e.getLocalizedMessage());
        }
    }
}

