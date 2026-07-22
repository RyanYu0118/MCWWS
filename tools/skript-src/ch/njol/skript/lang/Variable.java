/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Iterators
 *  com.google.common.collect.UnmodifiableIterator
 *  org.apache.commons.lang3.ArrayUtils
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyReceiverExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.structures.StructVariables;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.SingleItemIterator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.script.ScriptWarning;
import org.skriptlang.skript.util.IndexTrackingTreeMap;

public class Variable<T>
implements Expression<T>,
KeyReceiverExpression<T>,
KeyProviderExpression<T> {
    private static final String SINGLE_SEPARATOR_CHAR = ":";
    public static final String SEPARATOR = "::";
    public static final String LOCAL_VARIABLE_TOKEN = "_";
    public static final String EPHEMERAL_VARIABLE_TOKEN = "-";
    private static final char[] reservedTokens = new char[]{'~', '.', '+', '$', '!', '&', '^', '*'};
    @Nullable
    private final Script script;
    private final VariableString name;
    private final Class<T> superType;
    private final Class<? extends T>[] types;
    private final boolean local;
    private final boolean ephemeral;
    private final boolean list;
    @Nullable
    private final Variable<?> source;
    private final Map<Event, String[]> cache = Collections.synchronizedMap(new WeakHashMap());
    private ListProvider listProvider = new ShallowListProvider();

    private Variable(VariableString name, Class<? extends T>[] types, boolean local, boolean ephemeral, boolean list, @Nullable Variable<?> source) {
        assert (types.length > 0);
        assert (name.isSimple() || name.getMode() == StringMode.VARIABLE_NAME);
        ParserInstance parser = this.getParser();
        this.script = parser.isActive() ? parser.getCurrentScript() : null;
        this.local = local;
        this.ephemeral = ephemeral;
        this.list = list;
        this.name = name;
        this.types = types;
        this.superType = Classes.getSuperClassInfo(types).getC();
        this.source = source;
    }

    public static boolean isValidVariableName(String name, boolean allowListVariable, boolean printErrors) {
        assert (!name.isEmpty()) : "Variable name should not be empty";
        char first = name.charAt(0);
        for (char token : reservedTokens) {
            if (first != token || !printErrors) continue;
            Skript.warning("The character '" + token + "' is reserved at the start of variable names, and may be restricted in future versions");
        }
        String string = name = name.startsWith(LOCAL_VARIABLE_TOKEN) ? name.substring(LOCAL_VARIABLE_TOKEN.length()).trim() : name.trim();
        if (!allowListVariable && name.contains(SEPARATOR)) {
            if (printErrors) {
                Skript.error("List variables are not allowed here (error in variable {" + name + "})");
            }
            return false;
        }
        if (name.startsWith(SEPARATOR) || name.endsWith(SEPARATOR)) {
            if (printErrors) {
                Skript.error("A variable's name must neither start nor end with the separator '::' (error in variable {" + name + "})");
            }
            return false;
        }
        if (!(!name.contains("*") || allowListVariable && name.indexOf("*") == name.length() - 1 && name.endsWith("::*"))) {
            ArrayList<Integer> asterisks = new ArrayList<Integer>();
            ArrayList<Integer> percents = new ArrayList<Integer>();
            for (int i = 0; i < name.length(); ++i) {
                char character = name.charAt(i);
                if (character == '*') {
                    asterisks.add(i);
                    continue;
                }
                if (character != '%') continue;
                percents.add(i);
            }
            int count = asterisks.size();
            int index = 0;
            for (int i = 0; i < percents.size() && index != asterisks.size() && i + 1 != percents.size(); i += 2) {
                int lowerBound = (Integer)percents.get(i);
                int upperBound = (Integer)percents.get(i + 1);
                while (index < asterisks.size() && lowerBound < (Integer)asterisks.get(index) && (Integer)asterisks.get(index) < upperBound) {
                    --count;
                    ++index;
                }
            }
            if (!(count == 0 || count == 1 && name.endsWith("::*"))) {
                if (printErrors) {
                    Skript.error("A variable's name must not contain any asterisks except at the end after '::' to denote a list variable, e.g. {variable::*} (error in variable {" + name + "})");
                }
                return false;
            }
        } else {
            if (name.contains("::::")) {
                if (printErrors) {
                    Skript.error("A variable's name must not contain the separator '::' multiple times in a row (error in variable {" + name + "})");
                }
                return false;
            }
            if (name.replace(SEPARATOR, "").contains(SINGLE_SEPARATOR_CHAR) && printErrors) {
                Script currentScript;
                ParserInstance parser = ParserInstance.get();
                Script script = currentScript = parser.isActive() ? parser.getCurrentScript() : null;
                if (!(currentScript != null && currentScript.suppressesWarning(ScriptWarning.VARIABLE_CONTAINS_COLON) || SkriptConfig.disableColonInVariableWarnings.value().booleanValue())) {
                    Skript.warning("If you meant to make the variable {" + name + "} a list, its name should contain '::'. Having a single ':' does nothing!");
                }
            }
        }
        return true;
    }

    @Nullable
    public static <T> Variable<T> newInstance(String name, Class<? extends T>[] types) {
        Set<Class<?>> hints;
        Script currentScript;
        if (!Variable.isValidVariableName(name = name.trim(), true, true)) {
            return null;
        }
        VariableString variableString = VariableString.newInstance(name.startsWith(LOCAL_VARIABLE_TOKEN) ? name.substring(LOCAL_VARIABLE_TOKEN.length()).trim() : name, StringMode.VARIABLE_NAME);
        if (variableString == null) {
            return null;
        }
        boolean isLocal = name.startsWith(LOCAL_VARIABLE_TOKEN);
        boolean isEphemeral = name.startsWith(EPHEMERAL_VARIABLE_TOKEN);
        boolean isPlural = name.endsWith("::*");
        ParserInstance parser = ParserInstance.get();
        Script script = currentScript = parser.isActive() ? parser.getCurrentScript() : null;
        if (currentScript != null && !SkriptConfig.disableVariableStartingWithExpressionWarnings.value().booleanValue() && !currentScript.suppressesWarning(ScriptWarning.VARIABLE_STARTS_WITH_EXPRESSION)) {
            String strippedName = name;
            if (isLocal) {
                strippedName = strippedName.substring(LOCAL_VARIABLE_TOKEN.length());
            } else if (isEphemeral) {
                strippedName = strippedName.substring(EPHEMERAL_VARIABLE_TOKEN.length());
            }
            if (strippedName.startsWith("%")) {
                Skript.warning("Starting a variable's name with an expression is discouraged ({" + name + "}). You could prefix it with the script's name: {" + StringUtils.substring(currentScript.getConfig().getFileName(), 0, -3) + SEPARATOR + name + "}");
            }
        }
        if (isLocal && variableString.isSimple() && !(hints = parser.getHintManager().get(variableString.toString(null))).isEmpty()) {
            if (types[0] == Object.class) {
                return new Variable<T>(variableString, hints.toArray(new Class[0]), true, isEphemeral, isPlural, null);
            }
            ArrayList<Class<? extends T>> potentialTypes = new ArrayList<Class<? extends T>>();
            for (Class type : types) {
                if (!hints.stream().anyMatch(hint -> type.isAssignableFrom((Class<?>)hint) || Converters.converterExists(hint, type))) continue;
                potentialTypes.add(type);
            }
            if (!potentialTypes.isEmpty()) {
                return new Variable<T>(variableString, (Class[])potentialTypes.toArray(Class[]::new), true, isEphemeral, isPlural, null);
            }
            Object[] infos = new ClassInfo[types.length];
            for (int i = 0; i < types.length; ++i) {
                infos[i] = Classes.getSuperClassInfo(types[i]);
            }
            Object[] hintInfos = (ClassInfo[])hints.stream().map(Classes::getSuperClassInfo).toArray(ClassInfo[]::new);
            String isTypes = Utils.a(Classes.toString(hintInfos, false));
            String notTypes = Utils.a(Classes.toString(infos, false));
            Skript.error("Expected variable '{_" + variableString.toString(null) + "}' to be " + notTypes + ", but it is " + isTypes);
            return null;
        }
        return new Variable<T>(variableString, types, isLocal, isEphemeral, isPlural, null);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        throw new UnsupportedOperationException();
    }

    public boolean isLocal() {
        return this.local;
    }

    public boolean isEphemeral() {
        return this.ephemeral;
    }

    public boolean isList() {
        return this.list;
    }

    @Override
    public boolean isSingle() {
        return !this.list;
    }

    @Override
    public Class<? extends T> getReturnType() {
        return this.superType;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        return Arrays.copyOf(this.types, this.types.length);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        StringBuilder stringBuilder = new StringBuilder().append("{");
        if (this.local) {
            stringBuilder.append(LOCAL_VARIABLE_TOKEN);
        }
        stringBuilder.append(StringUtils.substring(this.name.toString(event, debug), 1, -1)).append("}");
        if (debug) {
            stringBuilder.append(" (");
            if (event != null) {
                stringBuilder.append(Classes.toString(this.get(event))).append(", ");
            }
            stringBuilder.append("as ").append(this.superType.getName()).append(")");
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return this.toString(null, false);
    }

    @Override
    public <R> Variable<R> getConvertedExpression(Class<R> ... to) {
        boolean converterExists;
        boolean bl = converterExists = this.superType == Object.class;
        if (!converterExists) {
            for (Class<? extends T> type : this.types) {
                if (!Converters.converterExists(type, to)) continue;
                converterExists = true;
                break;
            }
        }
        if (!converterExists) {
            return null;
        }
        return new Variable<R>(this.name, to, this.local, this.ephemeral, this.list, this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public Object getRaw(Event event) {
        StructVariables.DefaultVariables data;
        StructVariables.DefaultVariables defaultVariables = data = this.script == null ? null : this.script.getData(StructVariables.DefaultVariables.class);
        if (data != null) {
            data.enterScope();
        }
        try {
            Object value;
            String name = this.name.toString(event);
            if (name.endsWith("::*") != this.list) {
                Object var4_4 = null;
                return var4_4;
            }
            Object object = value = !this.list ? Variable.convertIfOldPlayer(name, this.local, event, Variables.getVariable(name, event, this.local)) : Variables.getVariable(name, event, this.local);
            if (value != null) {
                Object object2 = value;
                return object2;
            }
            if (data == null || !data.hasDefaultVariables()) {
                Object var5_7 = null;
                return var5_7;
            }
            for (String typeHint : this.name.getDefaultVariableNames(name, event)) {
                value = Variables.getVariable(typeHint, event, false);
                if (value == null) continue;
                Object object3 = value;
                return object3;
            }
        }
        finally {
            if (data != null) {
                data.exitScope();
            }
        }
        return null;
    }

    @Nullable
    private Object get(Event event) {
        Object rawValue = this.getRaw(event);
        if (!this.list) {
            return rawValue;
        }
        KeyedValue<?>[] values = this.listProvider.getValues(event);
        return KeyedValue.unzip(values).values().toArray();
    }

    @Nullable
    public static <T> T convertIfOldPlayer(String key, boolean local, Event event, @Nullable T object) {
        Player oldPlayer;
        if (SkriptConfig.enablePlayerVariableFix.value().booleanValue() && object instanceof Player && !(oldPlayer = (Player)object).isValid() && oldPlayer.isOnline()) {
            Player newPlayer = Bukkit.getPlayer((UUID)oldPlayer.getUniqueId());
            Variables.setVariable(key, newPlayer, event, local);
            return (T)newPlayer;
        }
        return object;
    }

    @Override
    public Iterator<KeyedValue<T>> keyedIterator(Event event) {
        if (!this.list) {
            throw new SkriptAPIException("Invalid call to keyedIterator");
        }
        UnmodifiableIterator iterator = Iterators.forArray((Object[])this.listProvider.getValues(event));
        Iterator transformed = Iterators.transform((Iterator)iterator, value -> {
            assert (value != null);
            T converted = Converters.convert(value.value(), this.types);
            if (converted == null) {
                return null;
            }
            return new KeyedValue<T>(value.key(), converted);
        });
        return Iterators.filter((Iterator)transformed, Objects::nonNull);
    }

    public Iterator<Pair<String, Object>> variablesIterator(Event event) {
        if (!this.list) {
            throw new SkriptAPIException("Looping a non-list variable");
        }
        return Variables.getVariableIterator(this.name.toString(event), this.local, event);
    }

    @Override
    @Nullable
    public Iterator<T> iterator(Event event) {
        if (!this.list) {
            T value = this.getSingle(event);
            return value != null ? new SingleItemIterator<T>(value) : null;
        }
        return Iterators.transform(this.keyedIterator(event), KeyedValue::value);
    }

    @Nullable
    private T getConverted(Event event) {
        assert (!this.list);
        return Converters.convert(this.get(event), this.types);
    }

    private T[] getConvertedArray(Event event) {
        assert (this.list);
        KeyedValue<?>[] values = this.listProvider.getValues(event);
        Object[] mappedValues = KeyedValue.map(values, value -> Converters.convert(value, this.types));
        mappedValues = (KeyedValue[])ArrayUtils.removeAllOccurrences((Object[])mappedValues, null);
        KeyedValue.UnzippedKeyValues unzipped = KeyedValue.unzip(mappedValues);
        this.cache.put(event, unzipped.keys().toArray(new String[0]));
        return unzipped.values().toArray((Object[])Array.newInstance(this.superType, 0));
    }

    private void set(Event event, @Nullable Object value) {
        Variables.setVariable(this.name.toString(event), value, event, this.local);
    }

    private void setIndex(Event event, String index, @Nullable Object value) {
        assert (this.list);
        String name = this.name.toString(event);
        assert (name.endsWith("::*")) : name + "; " + String.valueOf(this.name);
        Variables.setVariable(name.substring(0, name.length() - 1) + index, value, event, this.local);
    }

    public int size(Event event) {
        Preconditions.checkState((boolean)this.list, (Object)"Cannot get the size of a single variable");
        Map map = (Map)this.getRaw(event);
        if (map == null) {
            return 0;
        }
        int size = map.size();
        if (map.containsKey(null)) {
            --size;
        }
        if (!(map instanceof IndexTrackingTreeMap)) {
            for (Object value : map.values()) {
                Map sublist;
                if (!(value instanceof Map) || (sublist = (Map)value).containsKey(null)) continue;
                --size;
            }
            return size;
        }
        IndexTrackingTreeMap indexTrackingMap = (IndexTrackingTreeMap)map;
        Collection<String> sublistIndices = indexTrackingMap.mapIndices();
        for (String sublistIndex : sublistIndices) {
            if (((Map)map.get(sublistIndex)).containsKey(null)) continue;
            --size;
        }
        return size;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!this.list && mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Object.class);
        }
        return CollectionUtils.array(Object[].class);
    }

    @Override
    public void change(Event event, Object @NotNull [] delta, Changer.ChangeMode mode, @NotNull @NotNull String @NotNull [] keys) {
        if (!this.list) {
            this.change(event, delta, mode);
            return;
        }
        if (mode == Changer.ChangeMode.SET) {
            assert (delta.length == keys.length);
            this.set(event, null);
            int length = Math.min(delta.length, keys.length);
            for (int index = 0; index < length; ++index) {
                Object value = delta[index];
                String key = keys[index];
                if (value instanceof Object[]) {
                    Object[] array = (Object[])value;
                    for (int j = 0; j < array.length; ++j) {
                        this.setIndex(event, key + SEPARATOR + (j + 1), array[j]);
                    }
                    continue;
                }
                this.setIndex(event, key, value);
            }
            return;
        }
        this.change(event, delta, mode);
    }

    /*
     * WARNING - void declaration
     */
    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        switch (mode) {
            case DELETE: {
                this.set(event, null);
                break;
            }
            case SET: {
                assert (delta != null);
                if (this.list) {
                    this.set(event, null);
                    int i = 1;
                    for (Object value : delta) {
                        if (value instanceof Object[]) {
                            for (int j = 0; j < ((Object[])value).length; ++j) {
                                this.setIndex(event, i + SEPARATOR + (j + 1), ((Object[])value)[j]);
                            }
                        } else {
                            this.setIndex(event, "" + i, value);
                        }
                        ++i;
                    }
                    break;
                }
                if (delta.length <= 0) break;
                this.set(event, delta[0]);
                break;
            }
            case RESET: {
                Object rawValue = this.getRaw(event);
                if (rawValue == null) {
                    return;
                }
                for (Object v : rawValue instanceof Map ? ((Map)rawValue).values() : Arrays.asList(rawValue)) {
                    Class<?> type = v.getClass();
                    assert (type != null);
                    ClassInfo<?> classInfo = Classes.getSuperClassInfo(type);
                    Changer<?> changer = classInfo.getChanger();
                    if (changer == null || changer.acceptChange(Changer.ChangeMode.RESET) == null) continue;
                    Object[] valueArray = (Object[])Array.newInstance(v.getClass(), 1);
                    valueArray[0] = v;
                    changer.change(valueArray, null, Changer.ChangeMode.RESET);
                }
                break;
            }
            case ADD: 
            case REMOVE: 
            case REMOVE_ALL: {
                Class<?>[] classes;
                Operator operator;
                assert (delta != null);
                if (this.list) {
                    Map map = (Map)this.getRaw(event);
                    if (mode == Changer.ChangeMode.REMOVE) {
                        if (map == null) {
                            return;
                        }
                        HashSet<String> hashSet = new HashSet<String>();
                        block9: for (Object value : delta) {
                            for (Map.Entry entry : map.entrySet()) {
                                String key = (String)entry.getKey();
                                if (key == null || hashSet.contains(key) || !Relation.EQUAL.isImpliedBy(Comparators.compare(entry.getValue(), value))) continue;
                                hashSet.add(key);
                                continue block9;
                            }
                        }
                        for (String index : hashSet) {
                            assert (index != null);
                            this.setIndex(event, index, null);
                        }
                    } else if (mode == Changer.ChangeMode.REMOVE_ALL) {
                        if (map == null) {
                            return;
                        }
                        HashSet<String> hashSet = new HashSet<String>();
                        for (Map.Entry i : map.entrySet()) {
                            for (Object value : delta) {
                                if (!Relation.EQUAL.isImpliedBy(Comparators.compare(i.getValue(), value))) continue;
                                hashSet.add((String)i.getKey());
                            }
                        }
                        for (String index : hashSet) {
                            assert (index != null);
                            this.setIndex(event, index, null);
                        }
                    } else {
                        assert (mode == Changer.ChangeMode.ADD);
                        if (map instanceof IndexTrackingTreeMap) {
                            IndexTrackingTreeMap indexTrackingTreeMap = (IndexTrackingTreeMap)map;
                            for (Object value : delta) {
                                int index = indexTrackingTreeMap.nextOpenIndex();
                                this.setIndex(event, String.valueOf(index), value);
                            }
                            return;
                        }
                        boolean bl = true;
                        for (Object value : delta) {
                            void var6_16;
                            if (map != null) {
                                while (map.containsKey("" + (int)var6_16)) {
                                    ++var6_16;
                                }
                            }
                            this.setIndex(event, "" + (int)var6_16, value);
                            ++var6_16;
                        }
                    }
                    break;
                }
                Object originalValue = this.get(event);
                Class<?> clazz = originalValue == null ? null : originalValue.getClass();
                Operator operator2 = operator = mode == Changer.ChangeMode.ADD ? Operator.ADDITION : Operator.SUBTRACTION;
                if (clazz == null || !Arithmetics.getOperations(operator, clazz).isEmpty()) {
                    boolean changed = false;
                    Object[] index = delta;
                    int value = index.length;
                    for (int i = 0; i < value; ++i) {
                        Object value2;
                        Object newValue;
                        OperationInfo<?, ?, ?> info = Arithmetics.getOperationInfo(operator, clazz != null ? clazz : newValue.getClass(), (newValue = index[i]).getClass());
                        if (info == null) continue;
                        Object object = value2 = originalValue == null ? Arithmetics.getDefaultValue(info.left()) : originalValue;
                        if (value2 == null) continue;
                        originalValue = info.operation().calculate(value2, newValue);
                        changed = true;
                    }
                    if (!changed) break;
                    this.set(event, originalValue);
                    break;
                }
                Changer<?> changer = Classes.getSuperClassInfo(clazz).getChanger();
                if (changer == null || (classes = changer.acceptChange(mode)) == null) break;
                Object[] originalValueArray = (Object[])Array.newInstance(originalValue.getClass(), 1);
                originalValueArray[0] = originalValue;
                Class[] classes2 = new Class[classes.length];
                for (int i = 0; i < classes.length; ++i) {
                    classes2[i] = classes[i].isArray() ? classes[i].getComponentType() : classes[i];
                }
                ArrayList convertedDelta = new ArrayList();
                for (Object value : delta) {
                    Object convertedValue = Converters.convert(value, classes2);
                    if (convertedValue == null) continue;
                    convertedDelta.add(convertedValue);
                }
                Changer.ChangerUtils.change(changer, originalValueArray, convertedDelta.toArray(), mode);
            }
        }
    }

    @Override
    public <R> void changeInPlace(Event event, Function<T, R> changeFunction, boolean getAll) {
        this.changeInPlace(event, changeFunction);
    }

    @Override
    public <R> void changeInPlace(Event event, Function<T, R> changeFunction) {
        if (!this.list) {
            T value = this.getSingle(event);
            if (value == null) {
                return;
            }
            this.set(event, changeFunction.apply(value));
            return;
        }
        this.keyedIterator(event).forEachRemaining(keyedValue -> {
            String index = keyedValue.key();
            Object newValue = changeFunction.apply(keyedValue.value());
            this.setIndex(event, index, newValue);
        });
    }

    @Override
    @Nullable
    public T getSingle(Event event) {
        if (this.list) {
            throw new SkriptAPIException("Invalid call to getSingle");
        }
        return this.getConverted(event);
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws SkriptAPIException {
        if (!this.list) {
            throw new SkriptAPIException("Invalid call to getArrayKeys on non-list");
        }
        if (!this.cache.containsKey(event)) {
            throw new IllegalStateException();
        }
        return this.cache.remove(event);
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getAllKeys(Event event) {
        return this.getArrayKeys(event);
    }

    @Override
    public boolean canReturnKeys() {
        return this.list;
    }

    @Override
    public boolean areKeysRecommended() {
        return false;
    }

    @Override
    public boolean returnNestedStructures(boolean nested) {
        if (!this.canReturnKeys()) {
            return false;
        }
        this.listProvider = nested ? new RecursiveListProvider() : new ShallowListProvider();
        return true;
    }

    @Override
    public boolean returnsNestedStructures() {
        return this.listProvider.getClass() == RecursiveListProvider.class;
    }

    @Override
    public T[] getArray(Event event) {
        return this.getAll(event);
    }

    @Override
    public T[] getAll(Event event) {
        if (this.list) {
            return this.getConvertedArray(event);
        }
        T value = this.getConverted(event);
        if (value == null) {
            return (Object[])Array.newInstance(this.superType, 0);
        }
        Object[] valueArray = (Object[])Array.newInstance(this.superType, 1);
        valueArray[0] = value;
        return valueArray;
    }

    @Override
    public boolean isLoopOf(String input) {
        return KeyProviderExpression.super.isLoopOf(input) || input.equalsIgnoreCase("var") || input.equalsIgnoreCase("variable") || input.equalsIgnoreCase("value");
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker, boolean negated) {
        return SimpleExpression.check(this.getAll(event), checker, negated, this.getAnd());
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker) {
        return SimpleExpression.check(this.getAll(event), checker, false, this.getAnd());
    }

    public VariableString getName() {
        return this.name;
    }

    @Override
    public boolean getAnd() {
        return true;
    }

    @Override
    public boolean setTime(int time) {
        return false;
    }

    @Override
    public int getTime() {
        return 0;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public Expression<?> getSource() {
        Variable<?> source = this.source;
        return source == null ? this : source;
    }

    @Override
    public Expression<? extends T> simplify() {
        return this;
    }

    @Override
    public boolean supportsLoopPeeking() {
        return true;
    }

    class ShallowListProvider
    implements ListProvider {
        ShallowListProvider() {
        }

        @Override
        public KeyedValue<?>[] getValues(Event event) {
            if (!Variable.this.list) {
                throw new SkriptAPIException("Invalid call to getValues on non-list variable");
            }
            Object rawValue = Variable.this.getRaw(event);
            if (rawValue == null) {
                return new KeyedValue[0];
            }
            ArrayList keyedValues = new ArrayList();
            String name = StringUtils.substring(Variable.this.name.toString(event), 0, -1);
            for (Map.Entry variable : ((Map)rawValue).entrySet()) {
                Object value;
                if (variable.getKey() == null || variable.getValue() == null) continue;
                Object v = variable.getValue();
                if (v instanceof Map) {
                    Map sublist = (Map)v;
                    value = sublist.get(null);
                } else {
                    value = variable.getValue();
                }
                if ((value = Variable.convertIfOldPlayer(name + (String)variable.getKey(), Variable.this.local, event, value)) == null) continue;
                keyedValues.add(new KeyedValue((String)variable.getKey(), value));
            }
            return keyedValues.toArray(new KeyedValue[0]);
        }
    }

    private static interface ListProvider {
        public KeyedValue<?>[] getValues(Event var1);
    }

    class RecursiveListProvider
    implements ListProvider {
        RecursiveListProvider() {
        }

        @Override
        public KeyedValue<?>[] getValues(Event event) {
            if (!Variable.this.list) {
                throw new SkriptAPIException("Invalid call to getValues on non-list variable");
            }
            Object rawValue = Variable.this.getRaw(event);
            if (rawValue == null) {
                return new KeyedValue[0];
            }
            ArrayList keyedValues = new ArrayList();
            String name = StringUtils.substring(Variable.this.name.toString(event), 0, -1);
            this.getValuesRecursive(event, (Map)rawValue, name, "", keyedValues);
            return keyedValues.toArray(new KeyedValue[0]);
        }

        private void getValuesRecursive(Event event, Map<?, ?> variable, String root, String prefix, List<KeyedValue<?>> values) {
            for (Map.Entry<?, ?> entry : variable.entrySet()) {
                Object value;
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String relativeKey = prefix + (String)entry.getKey();
                String absoluteKey = root + relativeKey;
                Object obj = entry.getValue();
                if (obj instanceof Map) {
                    Map sublist = (Map)obj;
                    this.getValuesRecursive(event, (Map)entry.getValue(), root, relativeKey + Variable.SEPARATOR, values);
                    value = sublist.get(null);
                } else {
                    value = entry.getValue();
                }
                if ((value = Variable.convertIfOldPlayer(absoluteKey, Variable.this.local, event, value)) == null) continue;
                values.add(new KeyedValue(relativeKey, value));
            }
        }
    }
}

