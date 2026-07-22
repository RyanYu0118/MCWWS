/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.registration;

import ch.njol.skript.lang.SkriptEvent;
import java.util.Collection;
import java.util.SequencedCollection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfosImpl;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class BukkitSyntaxInfos {
    private BukkitSyntaxInfos() {
    }

    public static String fixPattern(String pattern) {
        char[] chars = pattern.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        boolean inType = false;
        for (int i = 0; i < chars.length; ++i) {
            char character = chars[i];
            stringBuilder.append(character);
            if (character == '%') {
                boolean bl = inType = !inType;
                if (!inType || i + 2 >= chars.length || chars[i + 1] == '-' || chars[i + 2] == '-') continue;
                stringBuilder.append('-');
                continue;
            }
            if (character != '\\' || i + 1 >= chars.length) continue;
            stringBuilder.append(chars[i + 1]);
            ++i;
        }
        return stringBuilder.toString();
    }

    public static interface Event<E extends SkriptEvent>
    extends SyntaxInfo<E> {
        public static final SyntaxRegistry.Key<Event<?>> KEY = SyntaxRegistry.Key.of("event");

        public static <E extends SkriptEvent> Builder<? extends Builder<?, E>, E> builder(Class<E> eventClass, String name) {
            return new BukkitSyntaxInfosImpl.EventImpl.BuilderImpl(eventClass, name);
        }

        @Override
        @Contract(value="-> new")
        public Builder<? extends Builder<?, E>, E> toBuilder();

        public SkriptEvent.ListeningBehavior listeningBehavior();

        public String name();

        public String id();

        @Nullable
        public String documentationId();

        public SequencedCollection<String> since();

        public SequencedCollection<String> description();

        public Collection<String> examples();

        public Collection<String> keywords();

        public Collection<String> requiredPlugins();

        public Collection<Class<? extends org.bukkit.event.Event>> events();

        public static interface Builder<B extends Builder<B, E>, E extends SkriptEvent>
        extends SyntaxInfo.Builder<B, E> {
            @Contract(value="_ -> this")
            public B listeningBehavior(SkriptEvent.ListeningBehavior var1);

            @Contract(value="_ -> this")
            public B documentationId(String var1);

            @Contract(value="_ -> this")
            public B addSince(String var1);

            @Contract(value="_ -> this")
            public B addSince(String ... var1);

            @Contract(value="_ -> this")
            public B addSince(Collection<String> var1);

            @Contract(value="_ -> this")
            public B clearSince();

            @Contract(value="_ -> this")
            public B addDescription(String var1);

            @Contract(value="_ -> this")
            public B addDescription(String ... var1);

            @Contract(value="_ -> this")
            public B addDescription(Collection<String> var1);

            @Contract(value="-> this")
            public B clearDescription();

            @Contract(value="_ -> this")
            public B addExample(String var1);

            @Contract(value="_ -> this")
            public B addExamples(String ... var1);

            @Contract(value="_ -> this")
            public B addExamples(Collection<String> var1);

            @Contract(value="-> this")
            public B clearExamples();

            @Contract(value="_ -> this")
            public B addKeyword(String var1);

            @Contract(value="_ -> this")
            public B addKeywords(String ... var1);

            @Contract(value="_ -> this")
            public B addKeywords(Collection<String> var1);

            @Contract(value="-> this")
            public B clearKeywords();

            @Contract(value="_ -> this")
            public B addRequiredPlugin(String var1);

            @Contract(value="_ -> this")
            public B addRequiredPlugins(String ... var1);

            @Contract(value="_ -> this")
            public B addRequiredPlugins(Collection<String> var1);

            public B clearRequiredPlugins();

            @Contract(value="_ -> this")
            public B addEvent(Class<? extends org.bukkit.event.Event> var1);

            @Contract(value="_ -> this")
            public B addEvents(Class<? extends org.bukkit.event.Event>[] var1);

            @Contract(value="_ -> this")
            public B addEvents(Collection<Class<? extends org.bukkit.event.Event>> var1);

            @Contract(value="-> this")
            public B clearEvents();

            @Override
            @Contract(value="-> new")
            public Event<E> build();
        }
    }
}

