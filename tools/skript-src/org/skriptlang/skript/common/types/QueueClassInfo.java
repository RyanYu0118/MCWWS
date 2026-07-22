/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.types;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.lang.util.SkriptQueue;

@ApiStatus.Internal
public class QueueClassInfo
extends ClassInfo<SkriptQueue> {
    public QueueClassInfo() {
        super(SkriptQueue.class, "queue");
        this.user("queues?").name("Queue").description("A queued list of values. Entries are removed from a queue when they are queried.").examples("set {queue} to a new queue", "add \"hello\" to {queue}", "broadcast the 1st element of {queue}").since("2.10").changer(new QueueChanger()).parser(new QueueParser()).serializer(new QueueSerializer()).property(Property.AMOUNT, "The amount of elements in the queue.", Skript.instance(), new QueueAmountHandler()).property(Property.SIZE, "The size of the queue, in element count.", Skript.instance(), new QueueAmountHandler()).property(Property.IS_EMPTY, "Whether a queue is empty, i.e. whether there are no elements in the queue.", Skript.instance(), ConditionPropertyHandler.of(AbstractCollection::isEmpty));
    }

    private static class QueueChanger
    implements Changer<SkriptQueue> {
        private QueueChanger() {
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            Class[] classArray;
            switch (mode) {
                case ADD: 
                case REMOVE: 
                case DELETE: {
                    Class[] classArray2 = new Class[1];
                    classArray = classArray2;
                    classArray2[0] = Object.class;
                    break;
                }
                case RESET: {
                    classArray = new Class[]{};
                    break;
                }
                default: {
                    classArray = null;
                }
            }
            return classArray;
        }

        public void change(SkriptQueue[] what, Object @Nullable [] delta, Changer.ChangeMode mode) {
            block5: for (SkriptQueue queue : what) {
                switch (mode) {
                    case DELETE: 
                    case RESET: {
                        queue.clear();
                        continue block5;
                    }
                    case ADD: {
                        assert (delta != null);
                        queue.addAll(Arrays.asList(delta));
                        continue block5;
                    }
                    case REMOVE: {
                        assert (delta != null);
                        queue.removeAll(Arrays.asList(delta));
                    }
                }
            }
        }
    }

    private static class QueueParser
    extends Parser<SkriptQueue> {
        private QueueParser() {
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(SkriptQueue queue, int flags) {
            return Classes.toString(queue.toArray(), flags, true);
        }

        @Override
        public String toVariableNameString(SkriptQueue queue) {
            return this.toString(queue, 0);
        }
    }

    private static class QueueSerializer
    extends Serializer<SkriptQueue> {
        private QueueSerializer() {
        }

        @Override
        public Fields serialize(SkriptQueue queue) {
            Fields fields = new Fields();
            fields.putObject("contents", queue.toArray());
            return fields;
        }

        @Override
        public void deserialize(SkriptQueue queue, Fields fields) throws StreamCorruptedException {
            Object[] contents = fields.getObject("contents", Object[].class);
            queue.clear();
            if (contents != null) {
                queue.addAll(List.of(contents));
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }

        @Override
        protected boolean canBeInstantiated() {
            return true;
        }
    }

    private static class QueueAmountHandler
    implements ExpressionPropertyHandler<SkriptQueue, Integer> {
        private QueueAmountHandler() {
        }

        @Override
        public Integer convert(SkriptQueue propertyHolder) {
            return propertyHolder.size();
        }

        @Override
        @NotNull
        public Class<Integer> returnType() {
            return Integer.class;
        }
    }
}

