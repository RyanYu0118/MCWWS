/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.util.Version
 *  org.skriptlang.skript.lang.script.Script
 */
package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Version;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import java.util.Objects;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.skript.lang.script.Script;

public class EventSyntaxInfo
extends CustomSyntaxStructure.SyntaxData {
    protected EventSyntaxInfo(Script script, String pattern, int matchedPattern) {
        super(script, pattern, matchedPattern);
    }

    public static EventSyntaxInfo create(Script script, String pattern, int matchedPattern) {
        if (Skript.getVersion().isSmallerThan(new Version(new int[]{2, 8}))) {
            pattern = "[on] " + (String)pattern + " [with priority (lowest|low|normal|high|highest|monitor)]";
        }
        return new EventSyntaxInfo(script, SkriptMirrorUtil.preprocessPattern((String)pattern), matchedPattern);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        EventSyntaxInfo that = (EventSyntaxInfo)o;
        return Objects.equals(this.getScript(), that.getScript()) && Objects.equals(this.getPattern(), that.getPattern());
    }

    public int hashCode() {
        return Objects.hash(this.getScript(), this.getPattern());
    }
}

