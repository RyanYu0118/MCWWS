/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.classes;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.util.common.AnyProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Deprecated(since="2.13", forRemoval=true)
public class AnyInfo<Type extends AnyProvider>
extends ClassInfo<Type> {
    public AnyInfo(Class<Type> c, String codeName) {
        super(c, codeName);
        this.user("(any )?" + codeName + " (thing|object)s?");
    }

    @Override
    public ClassInfo<Type> user(String ... userInputPatterns) throws PatternSyntaxException {
        if (this.userInputPatterns == null) {
            return super.user(userInputPatterns);
        }
        ArrayList<Pattern> list = new ArrayList<Pattern>(List.of(this.userInputPatterns));
        for (String pattern : userInputPatterns) {
            list.add(Pattern.compile(pattern));
        }
        this.userInputPatterns = list.toArray(new Pattern[0]);
        return this;
    }
}

