/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.collect.ImmutableList
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.registration;

import ch.njol.skript.lang.SkriptEvent;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Supplier;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.util.Priority;

final class BukkitSyntaxInfosImpl {
    BukkitSyntaxInfosImpl() {
    }

    static final class EventImpl<E extends SkriptEvent>
    implements BukkitSyntaxInfos.Event<E> {
        private final SyntaxInfo<E> defaultInfo;
        private final SkriptEvent.ListeningBehavior listeningBehavior;
        private final String name;
        private final String id;
        @Nullable
        private final String documentationId;
        private final SequencedCollection<String> since;
        private final SequencedCollection<String> description;
        private final Collection<String> examples;
        private final Collection<String> keywords;
        private final Collection<String> requiredPlugins;
        private final Collection<Class<? extends Event>> events;

        EventImpl(SyntaxInfo<E> defaultInfo, SkriptEvent.ListeningBehavior listeningBehavior, String name, @Nullable String documentationId, Collection<String> since, Collection<String> description, Collection<String> examples, Collection<String> keywords, Collection<String> requiredPlugins, Collection<Class<? extends Event>> events) {
            this.defaultInfo = defaultInfo;
            this.listeningBehavior = listeningBehavior;
            this.name = name.startsWith("*") ? name.substring(1) : "On " + name;
            this.id = name.toLowerCase(Locale.ENGLISH).replaceAll("[#'\"<>/&]", "").replaceAll("\\s+", "_");
            this.documentationId = documentationId;
            this.since = ImmutableList.copyOf(since);
            this.description = ImmutableList.copyOf(description);
            this.examples = ImmutableList.copyOf(examples);
            this.keywords = ImmutableList.copyOf(keywords);
            this.requiredPlugins = ImmutableList.copyOf(requiredPlugins);
            this.events = ImmutableList.copyOf(events);
        }

        @Override
        public BukkitSyntaxInfos.Event.Builder<? extends BukkitSyntaxInfos.Event.Builder<?, E>, E> toBuilder() {
            BuilderImpl builder = new BuilderImpl(this.type(), "*" + this.name);
            this.defaultInfo.toBuilder().applyTo(builder);
            builder.listeningBehavior(this.listeningBehavior);
            builder.documentationId(this.id);
            if (this.documentationId != null) {
                builder.documentationId(this.documentationId);
            }
            builder.addSince(this.since);
            builder.addDescription(this.description);
            builder.addExamples(this.examples);
            builder.addKeywords(this.keywords);
            builder.addRequiredPlugins(this.requiredPlugins);
            builder.addEvents(this.events);
            return builder;
        }

        @Override
        public SkriptEvent.ListeningBehavior listeningBehavior() {
            return this.listeningBehavior;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public String id() {
            return this.id;
        }

        @Override
        @Nullable
        public String documentationId() {
            return this.documentationId;
        }

        @Override
        public SequencedCollection<String> since() {
            return this.since;
        }

        @Override
        public SequencedCollection<String> description() {
            return this.description;
        }

        @Override
        public Collection<String> examples() {
            return this.examples;
        }

        @Override
        public Collection<String> keywords() {
            return this.keywords;
        }

        @Override
        public Collection<String> requiredPlugins() {
            return this.requiredPlugins;
        }

        @Override
        public Collection<Class<? extends Event>> events() {
            return this.events;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BukkitSyntaxInfos.Event)) {
                return false;
            }
            BukkitSyntaxInfos.Event info = (BukkitSyntaxInfos.Event)other;
            if (other.getClass() != EventImpl.class && !other.equals(this)) {
                return false;
            }
            return this.type() == info.type() && Objects.equals(this.patterns(), info.patterns()) && Objects.equals(this.priority(), info.priority()) && Objects.equals(this.name(), info.name()) && Objects.equals(this.events(), info.events());
        }

        public int hashCode() {
            return Objects.hash(this.defaultInfo, this.name(), this.events());
        }

        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("origin", (Object)this.origin()).add("type", this.type()).add("patterns", this.patterns()).add("priority", (Object)this.priority()).add("name", (Object)this.name()).add("events", this.events()).toString();
        }

        @Override
        public Origin origin() {
            return this.defaultInfo.origin();
        }

        @Override
        public Class<E> type() {
            return this.defaultInfo.type();
        }

        @Override
        public E instance() {
            return (E)((SkriptEvent)this.defaultInfo.instance());
        }

        @Override
        public @Unmodifiable SequencedCollection<String> patterns() {
            return this.defaultInfo.patterns();
        }

        @Override
        public Priority priority() {
            return this.defaultInfo.priority();
        }

        static final class BuilderImpl<B extends BukkitSyntaxInfos.Event.Builder<B, E>, E extends SkriptEvent>
        implements BukkitSyntaxInfos.Event.Builder<B, E> {
            private final SyntaxInfo.Builder<?, E> defaultBuilder;
            private SkriptEvent.ListeningBehavior listeningBehavior = SkriptEvent.ListeningBehavior.UNCANCELLED;
            private final String name;
            @Nullable
            private String documentationId;
            private final List<String> since = new ArrayList<String>();
            private final List<String> description = new ArrayList<String>();
            private final List<String> examples = new ArrayList<String>();
            private final List<String> keywords = new ArrayList<String>();
            private final List<String> requiredPlugins = new ArrayList<String>();
            private final List<Class<? extends Event>> events = new ArrayList<Class<? extends Event>>();

            BuilderImpl(Class<E> type, String name) {
                this.defaultBuilder = SyntaxInfo.builder(type);
                this.name = name;
            }

            @Override
            public B listeningBehavior(SkriptEvent.ListeningBehavior listeningBehavior) {
                this.listeningBehavior = listeningBehavior;
                return (B)this;
            }

            @Override
            public B documentationId(String documentationId) {
                this.documentationId = documentationId;
                return (B)this;
            }

            @Override
            public B addSince(String since) {
                this.since.add(since);
                return (B)this;
            }

            @Override
            public B addSince(String ... since) {
                this.since.addAll(List.of(since));
                return (B)this;
            }

            @Override
            public B addSince(Collection<String> since) {
                this.since.addAll(since);
                return (B)this;
            }

            @Override
            public B clearSince() {
                this.since.clear();
                return (B)this;
            }

            @Override
            public B addDescription(String description) {
                this.description.add(description);
                return (B)this;
            }

            @Override
            public B addDescription(String ... description) {
                Collections.addAll(this.description, description);
                return (B)this;
            }

            @Override
            public B addDescription(Collection<String> description) {
                this.description.addAll(description);
                return (B)this;
            }

            @Override
            public B clearDescription() {
                this.description.clear();
                return (B)this;
            }

            @Override
            public B addExample(String example) {
                this.examples.add(example);
                return (B)this;
            }

            @Override
            public B addExamples(String ... examples) {
                Collections.addAll(this.examples, examples);
                return (B)this;
            }

            @Override
            public B addExamples(Collection<String> examples) {
                this.examples.addAll(examples);
                return (B)this;
            }

            @Override
            public B clearExamples() {
                this.examples.clear();
                return (B)this;
            }

            @Override
            public B addKeyword(String keyword) {
                this.keywords.add(keyword);
                return (B)this;
            }

            @Override
            public B addKeywords(String ... keywords) {
                Collections.addAll(this.keywords, keywords);
                return (B)this;
            }

            @Override
            public B addKeywords(Collection<String> keywords) {
                this.keywords.addAll(keywords);
                return (B)this;
            }

            @Override
            public B clearKeywords() {
                this.keywords.clear();
                return (B)this;
            }

            @Override
            public B addRequiredPlugin(String plugin) {
                this.requiredPlugins.add(plugin);
                return (B)this;
            }

            @Override
            public B addRequiredPlugins(String ... plugins) {
                Collections.addAll(this.requiredPlugins, plugins);
                return (B)this;
            }

            @Override
            public B addRequiredPlugins(Collection<String> plugins) {
                this.requiredPlugins.addAll(plugins);
                return (B)this;
            }

            @Override
            public B clearRequiredPlugins() {
                this.requiredPlugins.clear();
                return (B)this;
            }

            @Override
            public B addEvent(Class<? extends Event> event) {
                this.events.add(event);
                return (B)this;
            }

            @Override
            public B addEvents(Class<? extends Event> ... events) {
                Collections.addAll(this.events, events);
                return (B)this;
            }

            @Override
            public B addEvents(Collection<Class<? extends Event>> events) {
                this.events.addAll(events);
                return (B)this;
            }

            @Override
            public B clearEvents() {
                this.events.clear();
                return (B)this;
            }

            @Override
            public B origin(Origin origin) {
                this.defaultBuilder.origin(origin);
                return (B)this;
            }

            @Override
            public B supplier(Supplier<E> supplier) {
                this.defaultBuilder.supplier(supplier);
                return (B)this;
            }

            @Override
            public B addPattern(String pattern) {
                this.defaultBuilder.addPattern(pattern);
                return (B)this;
            }

            @Override
            public B addPatterns(String ... patterns) {
                this.defaultBuilder.addPatterns(patterns);
                return (B)this;
            }

            @Override
            public B addPatterns(Collection<String> patterns) {
                this.defaultBuilder.addPatterns(patterns);
                return (B)this;
            }

            @Override
            public B clearPatterns() {
                this.defaultBuilder.clearPatterns();
                return (B)this;
            }

            @Override
            public B priority(Priority priority) {
                this.defaultBuilder.priority(priority);
                return (B)this;
            }

            @Override
            public BukkitSyntaxInfos.Event<E> build() {
                return new EventImpl<E>(this.defaultBuilder.build(), this.listeningBehavior, this.name, this.documentationId, this.since, this.description, this.examples, this.keywords, this.requiredPlugins, this.events);
            }

            @Override
            public void applyTo(SyntaxInfo.Builder<?, ?> builder) {
                this.defaultBuilder.applyTo(builder);
                if (builder instanceof BukkitSyntaxInfos.Event.Builder) {
                    BukkitSyntaxInfos.Event.Builder eventBuilder = (BukkitSyntaxInfos.Event.Builder)builder;
                    eventBuilder.listeningBehavior(this.listeningBehavior);
                    if (this.documentationId != null) {
                        eventBuilder.documentationId(this.documentationId);
                    }
                    eventBuilder.addSince(this.since);
                    eventBuilder.addDescription(this.description);
                    eventBuilder.addExamples(this.examples);
                    eventBuilder.addKeywords(this.keywords);
                    eventBuilder.addRequiredPlugins(this.requiredPlugins);
                    eventBuilder.addEvents(this.events);
                }
            }
        }
    }
}

