/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.persistence.PersistentDataContainerView
 *  org.bukkit.NamespacedKey
 *  org.bukkit.block.Block
 *  org.bukkit.block.TileState
 *  org.bukkit.event.Event
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.pdc.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.NamespacedUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.Event;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.pdc.PDCSerializer;
import org.skriptlang.skript.bukkit.pdc.PDCUtils;
import org.skriptlang.skript.bukkit.pdc.SkriptDataType;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

/*
 * Uses jvm11+ dynamic constants - pseudocode provided - see https://www.benf.org/other/cfr/dynamic-constants.html
 */
@Name(value="Persistent Data Value")
@Description(value={"Provides access to the 'persistent data container' Bukkit provides on many objects. These values are stored on the chunk/world/item/entity directly, like custom NBT, but are much faster and reliable to access.\nPersistent values natively support numbers and text, but any Skript type that can be saved in a variable can also be stored in PDC via this expression. Lists of objects can also be saved.\nIf you attempt to save invalid types, runtime errors will be thrown.\n\nThe names of tags must be valid namespaced keys, i.e. a-z, 0-9, '_', '.', '/', and '-' are the allowed characters. If no namespace is provided, it will default to 'minecraft'.\n"})
@Example.Examples(value={@Example(value="set persistent data tag \"custom_damage\" of player's tool to 10"), @Example(value="on jump:\n\tif data tag \"boost\" of player's boots is set:\n\t\tpush player upwards\n"), @Example(value="on shoot:\n\tset {_strength} to number data tag \"strength\" of shooter's tool\n\tif {_strength} is set:\n\t\tset number data tag \"damage\" of projectile to {_strength}\n\non damage:\n\tset {_damage} to data tag \"damage\" of projectile\n\tif {_damage} is set:\n\t\tset damage to {_damage}\n"), @Example(value="set {_pet-uuids::*} to list data tag \"pets\" of player")})
@Since(value={"2.15"})
@Keywords(value={"pdc", "persistent data container", "custom data", "nbt"})
public class ExprPersistentData
extends PropertyExpression<Object, Object> {
    @Nullable
    private ClassInfoReference parsedType;
    private Expression<String> tag;
    private boolean plural;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprPersistentData.infoBuilder(ExprPersistentData.class, Object.class, "[persistent] [%-*classinfo%] [:list] data (value|tag) %string%", "chunks/worlds/entities/blocks/itemtypes/offlineplayers", false).supplier(ExprPersistentData::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.tag = expressions[matchedPattern + 1];
        Expression<ClassInfo<?>> classInfoExpression = expressions[matchedPattern];
        if (classInfoExpression != null) {
            Expression<ClassInfoReference> type = ClassInfoReference.wrap(classInfoExpression);
            this.parsedType = (ClassInfoReference)((Literal)type).getSingle();
            ClassInfo<?> classInfo = this.parsedType.getClassInfo();
            if (classInfo.getSerializer() == null) {
                Skript.error("Skript cannot serialize " + classInfo.getName().toString(true) + " as persistent data!");
                return false;
            }
        }
        this.plural = parseResult.hasTag("list") || this.parsedType != null && this.parsedType.isPlural().isTrue();
        this.setExpr(expressions[matchedPattern == 0 ? 2 : 0]);
        return true;
    }

    private ElementsResult getAllElements(PersistentDataContainerView container, NamespacedKey key) {
        ArrayList<Object> elements = new ArrayList<Object>();
        for (PersistentDataType<?, ?> candidateType : PDCSerializer.getRepresentablePDCTypes()) {
            if (!container.has(key, candidateType)) continue;
            Object value = container.get(key, candidateType);
            if (value != null) {
                elements.add(value);
            }
            return new ElementsResult(elements, false);
        }
        if (container.has(key, (PersistentDataType)SkriptDataType.get())) {
            Object value = container.get(key, (PersistentDataType)SkriptDataType.get());
            if (value != null) {
                elements.add(value);
            }
            return new ElementsResult(elements, false);
        }
        if (container.has(key, (PersistentDataType)PersistentDataType.LIST.dataContainers())) {
            List containers = (List)container.get(key, (PersistentDataType)PersistentDataType.LIST.dataContainers());
            if (containers != null) {
                for (PersistentDataContainer subContainer : containers) {
                    elements.add(PDCSerializer.deserialize(subContainer, container.getAdapterContext()));
                }
            }
            return new ElementsResult(elements, true);
        }
        return new ElementsResult(elements, false);
    }

    @Override
    protected Object[] get(Event event, Object[] source) {
        String tagName = this.tag.getSingle(event);
        if (tagName == null) {
            return new Object[0];
        }
        NamespacedKey key = NamespacedUtils.checkValidationAndSend(tagName.toLowerCase(Locale.ENGLISH), this);
        if (key == null) {
            return new Object[0];
        }
        ArrayList values = new ArrayList();
        for (Object holder : source) {
            PDCUtils.getPersistentDataContainer(holder, container -> {
                ElementsResult result = this.getAllElements((PersistentDataContainerView)container, key);
                List<Object> elements = result.elements();
                if (elements.isEmpty()) {
                    return;
                }
                if (this.plural && !result.storedAsList()) {
                    this.error("The data in tag '" + tagName + "' is a single value, not a list. Use 'data tag' instead of 'list data tag'.");
                    return;
                }
                if (!this.plural && result.storedAsList()) {
                    this.error("The data in tag '" + tagName + "' is a list, not a single value. Use 'list data tag' instead of 'data tag'.");
                    return;
                }
                if (this.parsedType != null) {
                    ClassInfo<?> classInfo = this.parsedType.getClassInfo();
                    if (this.plural) {
                        HashSet mismatches = new HashSet();
                        for (Object element : elements) {
                            if (classInfo.getC().isInstance(element)) {
                                values.add(element);
                                continue;
                            }
                            mismatches.add(element.getClass());
                        }
                        if (!mismatches.isEmpty()) {
                            this.warning(mismatches.size() + " element(s) in tag '" + tagName + "' were of type(s) " + Classes.toString(mismatches.stream().map(Classes::getSuperClassInfo).toArray(ClassInfo[]::new), true) + ", not the expected type " + Classes.toString(classInfo) + ". Skipping.");
                        }
                    } else {
                        Object first = elements.getFirst();
                        if (classInfo.getC().isInstance(first)) {
                            values.add(first);
                        } else {
                            this.error("The data in tag '" + tagName + "' was of type " + Classes.toString(Classes.getSuperClassInfo(first.getClass())) + ", not the expected type " + Classes.toString(classInfo) + ".");
                        }
                    }
                } else if (this.plural) {
                    values.addAll(elements);
                } else {
                    values.add(elements.getFirst());
                }
            });
        }
        return values.toArray(new Object[0]);
    }

    @Override
    public boolean isSingle() {
        return !this.plural;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.DELETE -> new Class[]{};
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> {
                if (this.parsedType != null) {
                    ClassInfo<?> type = this.parsedType.getClassInfo();
                    Changer<?> changer = type.getChanger();
                    if (changer != null) {
                        yield changer.acceptChange(mode);
                    }
                    TypeDescriptor.OfField<Class<?>> changeType = type.getC();
                    if (this.plural) {
                        changeType = changeType.arrayType();
                    }
                    if (mode == Changer.ChangeMode.SET) {
                        yield CollectionUtils.array(changeType);
                    }
                    if (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) {
                        if (this.plural) {
                            yield CollectionUtils.array(changeType);
                        }
                        yield Changer.ChangerUtils.getArithmeticChangeTypes(type.getC(), mode, operation -> type.getC().isAssignableFrom(operation.returnType()));
                    }
                    yield null;
                }
                yield CollectionUtils.array(this.plural ? Object[].class : Object.class);
            }
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        String tagName = this.tag.getSingle(event);
        if (tagName == null) {
            return;
        }
        NamespacedKey key = NamespacedUtils.checkValidationAndSend(tagName.toLowerCase(Locale.ENGLISH), this);
        if (key == null) {
            return;
        }
        ClassInfo<?> classInfo = null;
        if (mode == Changer.ChangeMode.SET || this.plural && (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE)) {
            assert (delta != null);
            for (Object deltaValue : delta) {
                classInfo = Classes.getSuperClassInfo(deltaValue.getClass());
                if (classInfo.getSerializer() != null) continue;
                this.error("Skript cannot serialize " + classInfo.getName().toString(true) + " as persistent data!");
                return;
            }
        } else if (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) {
            assert (delta != null);
            classInfo = Classes.getSuperClassInfo(delta[0].getClass());
        }
        HashSet<Block> invalidBlocks = new HashSet<Block>();
        ClassInfo<?> finalClassInfo = classInfo;
        for (Object holder : this.getExpr().getArray(event)) {
            Block block;
            if (holder instanceof Block && !((block = (Block)holder).getState() instanceof TileState)) {
                invalidBlocks.add(block);
                continue;
            }
            PDCUtils.editPersistentDataContainer(holder, container -> {
                /*
                 * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
                 * 
                 * org.benf.cfr.reader.util.ConfusedCFRException: Can't turn ConstantPoolEntry into Literal - got DynamicInfo value=15,770
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral.getConstantPoolEntry(TypedLiteral.java:340)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.getBootstrapArg(Op02WithProcessedDataAndRefs.java:538)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.getVarArgs(Op02WithProcessedDataAndRefs.java:671)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.buildInvokeBootstrapArgs(Op02WithProcessedDataAndRefs.java:630)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.buildInvokeDynamic(Op02WithProcessedDataAndRefs.java:411)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.buildInvokeDynamic(Op02WithProcessedDataAndRefs.java:392)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.createStatement(Op02WithProcessedDataAndRefs.java:1215)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.access$100(Op02WithProcessedDataAndRefs.java:57)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs$11.call(Op02WithProcessedDataAndRefs.java:2080)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs$11.call(Op02WithProcessedDataAndRefs.java:2077)
                 *     at org.benf.cfr.reader.util.graph.AbstractGraphVisitorFI.process(AbstractGraphVisitorFI.java:60)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.convertToOp03List(Op02WithProcessedDataAndRefs.java:2089)
                 *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:469)
                 *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
                 *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
                 *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
                 *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
                 *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1050)
                 *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
                 *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
                 *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
                 *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
                 *     at org.benf.cfr.reader.Main.main(Main.java:54)
                 */
                throw new IllegalStateException("Decompilation failed");
            });
        }
        if (!invalidBlocks.isEmpty()) {
            Object[] blocks = (Block[])invalidBlocks.stream().limit(3L).toArray(Block[]::new);
            this.warning("Could not set persistent data on blocks (" + Classes.toString(blocks, true) + ") as they are not tile entities (chests, furnaces, signs, etc.).");
        }
    }

    @Override
    public Class<?> getReturnType() {
        if (this.parsedType != null) {
            return this.parsedType.getClassInfo().getC();
        }
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder ssb = new SyntaxStringBuilder(event, debug);
        if (this.parsedType != null) {
            ssb.append((Object)this.parsedType.getClassInfo().getName().toString());
        }
        ssb.appendIf(this.plural, (Object)"list").append("data tag", this.tag, "of", this.getExpr());
        return ssb.toString();
    }

    private void addOrRemoveFromSingleValue(Object originalValue, Object[] delta, Changer.ChangeMode mode, Consumer<Object> setSingle) {
        Operator operator;
        Class<?> clazz = originalValue == null ? null : originalValue.getClass();
        Operator operator2 = operator = mode == Changer.ChangeMode.ADD ? Operator.ADDITION : Operator.SUBTRACTION;
        if (clazz == null || !Arithmetics.getOperations(operator, clazz).isEmpty()) {
            boolean changed = false;
            Object[] objectArray = delta;
            int n = objectArray.length;
            for (int i = 0; i < n; ++i) {
                Object value;
                Object newValue;
                OperationInfo<?, ?, ?> info = Arithmetics.getOperationInfo(operator, clazz != null ? clazz : newValue.getClass(), (newValue = objectArray[i]).getClass());
                if (info == null) continue;
                Object object = value = originalValue == null ? Arithmetics.getDefaultValue(info.left()) : originalValue;
                if (value == null || Classes.getSuperClassInfo(info.returnType()).getSerializer() == null) continue;
                originalValue = info.operation().calculate(value, newValue);
                changed = true;
            }
            if (changed) {
                setSingle.accept(originalValue);
            }
        } else {
            Class<?>[] acceptedClasses;
            Changer<?> changer = Classes.getSuperClassInfo(clazz).getChanger();
            if (changer != null && (acceptedClasses = changer.acceptChange(mode)) != null) {
                Object[] originalValueArray = (Object[])Array.newInstance(originalValue.getClass(), 1);
                originalValueArray[0] = originalValue;
                Class[] singularAcceptedClasses = new Class[acceptedClasses.length];
                for (int i = 0; i < acceptedClasses.length; ++i) {
                    singularAcceptedClasses[i] = acceptedClasses[i].isArray() ? acceptedClasses[i].getComponentType() : acceptedClasses[i];
                }
                Object[] convertedDelta = Converters.convert(delta, singularAcceptedClasses, Object.class);
                Changer.ChangerUtils.change(changer, originalValueArray, convertedDelta, mode);
            }
        }
    }

    private void addOrRemoveFromList(PersistentDataContainer container, NamespacedKey key, Object[] delta, Changer.ChangeMode mode) {
        ArrayList<PersistentDataContainer> containers = new ArrayList<PersistentDataContainer>();
        if (container.has(key, (PersistentDataType)PersistentDataType.LIST.dataContainers())) {
            List list = (List)container.get(key, (PersistentDataType)PersistentDataType.LIST.dataContainers());
            assert (list != null);
            containers.addAll(list);
        }
        if (mode == Changer.ChangeMode.ADD) {
            for (Object object : delta) {
                containers.add(SkriptDataType.get().toPrimitive(object, container.getAdapterContext()));
            }
        } else {
            Iterator iterator = containers.iterator();
            ArrayList<Object> toRemove = new ArrayList<Object>(delta.length);
            toRemove.addAll(Arrays.asList(delta));
            block1: while (iterator.hasNext()) {
                PersistentDataContainer toDeserialize = (PersistentDataContainer)iterator.next();
                Object value = PDCSerializer.deserialize(toDeserialize, toDeserialize.getAdapterContext());
                Iterator removeIterator = toRemove.iterator();
                while (removeIterator.hasNext()) {
                    Object removeCandidate = removeIterator.next();
                    if (!Relation.EQUAL.isImpliedBy(Comparators.compare(removeCandidate, value))) continue;
                    iterator.remove();
                    removeIterator.remove();
                    continue block1;
                }
            }
        }
        container.set(key, (PersistentDataType)PersistentDataType.LIST.dataContainers(), containers);
    }

    private static /* synthetic */ void lambda$change$1(PersistentDataContainer container, NamespacedKey key, Object value) {
        PersistentDataType<?, ?> resultType = PDCSerializer.getPDCType(Classes.getSuperClassInfo(value.getClass()));
        container.set(key, resultType, value);
    }

    private record ElementsResult(List<Object> elements, boolean storedAsList) {
    }
}

