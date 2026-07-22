/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionList
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptParser
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.skript.util.Utils
 *  ch.njol.util.Kleenean
 *  ch.njol.util.coll.iterator.ArrayIterator
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.skriptlang.skript.lang.converter.Converters
 *  org.skriptlang.skript.lang.script.Script
 *  org.skriptlang.skript.registration.DefaultSyntaxInfos$Expression
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 */
package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.Descriptor;
import com.btk5h.skriptmirror.ImportNotFoundException;
import com.btk5h.skriptmirror.JavaCallException;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Null;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.LRUCache;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import com.btk5h.skriptmirror.util.StringSimilarity;
import com.btk5h.skriptmirror.util.lookup.LookupGetter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprJavaCall<T>
implements Expression<T> {
    public static int javaCallsMade = 0;
    private static final MethodHandles.Lookup LOOKUP = LookupGetter.getLookup();
    private static final Object[] NO_ARGS = new Object[0];
    private static final Descriptor CONSTRUCTOR_DESCRIPTOR = new Descriptor(null, "<init>", null);
    private static final String LITE_DESCRIPTOR = "(\\[[\\w.$]*])?([^0-9. \\[\\]][^. \\[\\]]*\\b)(\\[[\\w.$, ]*])?";
    static final SyntaxInfo<?> SYNTAX_INFO = DefaultSyntaxInfos.Expression.builder(ExprJavaCall.class, Object.class).addPattern("[2:try] %object%..%string%[\\((1:[%-objects%])\\)]").addPattern("[2:try] %object%.<(\\[[\\w.$]*])?([^0-9. \\[\\]][^. \\[\\]]*\\b)(\\[[\\w.$, ]*])?>[\\((1:[%-objects%])\\)]").addPattern("[2:try] [a] new %javatype%\\([%-objects%]\\)").supplier(ExprJavaCall::new).priority(SkriptMirror.SHADOW_REALM).build();
    public static Throwable lastError;
    private final LRUCache<Descriptor, Collection<MethodHandle>> callSiteCache = new LRUCache(8);
    private Script script;
    private boolean suppressErrors;
    private CallType type;
    private Descriptor staticDescriptor;
    private Expression<String> dynamicDescriptor;
    private Expression<?> rawTarget;
    private Expression<Object> rawArgs;
    private final ExprJavaCall<?> source;
    private final Class<? extends T>[] types;
    private final Class<T> superType;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, (SyntaxInfo)((DefaultSyntaxInfos.Expression)SYNTAX_INFO));
    }

    @Nullable
    public static ExprJavaCall<?> parse(String rawInput) {
        return (ExprJavaCall)SkriptParser.parse((String)rawInput, List.of(SYNTAX_INFO).iterator(), null);
    }

    public ExprJavaCall() {
        this(null, Object.class);
    }

    @SafeVarargs
    private ExprJavaCall(ExprJavaCall<?> source, Class<? extends T> ... types) {
        this.source = source;
        if (source != null) {
            this.script = source.script;
            this.suppressErrors = source.suppressErrors;
            this.type = source.type;
            this.staticDescriptor = source.staticDescriptor;
            this.dynamicDescriptor = source.dynamicDescriptor;
            this.rawTarget = source.rawTarget;
            this.rawArgs = source.rawArgs;
        }
        this.types = types;
        this.superType = Utils.getSuperType((Class[])types);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.script = SkriptUtil.getCurrentScript();
        this.suppressErrors = (parseResult.mark & 2) == 2;
        this.rawTarget = SkriptUtil.defendExpression(exprs[0]);
        this.rawArgs = SkriptUtil.defendExpression(exprs[matchedPattern == 0 ? 2 : 1]);
        if (!SkriptUtil.canInitSafely(this.rawTarget, this.rawArgs)) {
            return false;
        }
        switch (matchedPattern) {
            case 0: {
                this.type = (parseResult.mark & 1) == 1 ? CallType.METHOD : CallType.FIELD;
                this.dynamicDescriptor = exprs[1];
                break;
            }
            case 1: {
                Literal literal;
                Object rawTargetValue;
                Expression<?> expression;
                this.type = (parseResult.mark & 1) == 1 ? CallType.METHOD : CallType.FIELD;
                String desc = ((MatchResult)parseResult.regexes.get(0)).group();
                try {
                    this.staticDescriptor = Descriptor.parse(desc, this.script);
                }
                catch (ImportNotFoundException e) {
                    Skript.error((String)("The class " + e.getUserType() + " could not be found."));
                    return false;
                }
                if (this.staticDescriptor == null) {
                    Skript.error((String)(desc + " is not a valid descriptor."));
                    return false;
                }
                if (this.staticDescriptor.getJavaClass() == null && (expression = this.rawTarget) instanceof Literal && (rawTargetValue = (literal = (Literal)expression).getSingle()) instanceof JavaType) {
                    this.staticDescriptor = this.staticDescriptor.orDefaultClass(((JavaType)rawTargetValue).getJavaClass());
                }
                if (this.staticDescriptor.getParameterTypes() != null && this.type.equals((Object)CallType.FIELD)) {
                    Skript.error((String)"You can't pass parameter types to a field call.");
                    return false;
                }
                if (this.staticDescriptor.getJavaClass() == null || this.getCallSite(this.staticDescriptor).size() != 0) break;
                String name = this.staticDescriptor.getName();
                if (!Stream.of(this.staticDescriptor.getJavaClass().getClasses()).map(Class::getSimpleName).noneMatch(simpleName -> simpleName.equals(name))) break;
                Skript.error((String)(desc + " refers to a non-existent " + (this.type.equals((Object)CallType.METHOD) ? "method" : "field")));
                return false;
            }
            case 2: {
                this.type = CallType.CONSTRUCTOR;
                this.staticDescriptor = CONSTRUCTOR_DESCRIPTOR;
            }
        }
        return true;
    }

    public T getSingle(Event e) {
        Object target = ObjectWrapper.unwrapIfNecessary(this.rawTarget.getSingle(e));
        if (target == null) {
            return null;
        }
        Object[] arguments = this.rawArgs != null ? (this.rawArgs instanceof ExpressionList && this.rawArgs.getAnd() ? Arrays.stream(((ExpressionList)this.rawArgs).getExpressions()).map(SkriptUtil.unwrapWithEvent(e)).map(SkriptMirrorUtil::reifyIfNull).toArray(Object[]::new) : (this.rawArgs.isSingle() ? new Object[]{SkriptMirrorUtil.reifyIfNull(this.rawArgs.getSingle(e))} : this.rawArgs.getArray(e))) : NO_ARGS;
        return this.invoke(target, arguments, this.getDescriptor(e));
    }

    public T[] getArray(Event e) {
        T returnValue = this.getSingle(e);
        if (returnValue == null) {
            return JavaUtil.newArray(this.superType, 0);
        }
        T[] arr = JavaUtil.newArray(this.superType, 1);
        arr[0] = returnValue;
        return arr;
    }

    public T[] getAll(Event e) {
        return this.getArray(e);
    }

    public boolean isSingle() {
        return true;
    }

    public boolean check(Event e, Predicate<? super T> c, boolean negated) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)negated, (boolean)this.getAnd());
    }

    public boolean check(Event e, Predicate<? super T> c) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)false, (boolean)this.getAnd());
    }

    @SafeVarargs
    public final <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        return new ExprJavaCall<R>(this, to);
    }

    public Class<? extends T> getReturnType() {
        return this.superType;
    }

    public boolean getAnd() {
        return true;
    }

    public boolean setTime(int time) {
        return false;
    }

    public int getTime() {
        return 0;
    }

    public boolean isDefault() {
        return false;
    }

    public Iterator<? extends T> iterator(Event e) {
        return new ArrayIterator((Object[])this.getAll(e));
    }

    public boolean isLoopOf(String s) {
        return false;
    }

    public Expression<?> getSource() {
        return this.source == null ? this : this.source;
    }

    public Expression<? extends T> simplify() {
        return this;
    }

    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.type == CallType.FIELD && (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE)) {
            return new Class[]{Object.class};
        }
        return null;
    }

    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
        Object target = ObjectWrapper.unwrapIfNecessary(this.rawTarget.getSingle(e));
        if (target == null) {
            return;
        }
        Object[] args = new Object[1];
        switch (mode) {
            case SET: {
                args[0] = delta[0];
                break;
            }
            case DELETE: {
                args[0] = Null.getInstance();
            }
        }
        this.invoke(target, args, this.getDescriptor(e));
    }

    private void error(Throwable error, String message) {
        lastError = error;
        this.directError(message);
    }

    private void error(String message) {
        lastError = new JavaCallException(message);
        this.directError(message);
    }

    private void directError(String message) {
        if (!this.suppressErrors) {
            Skript.warning((String)message);
        }
    }

    private synchronized Collection<MethodHandle> getCallSite(Descriptor e) {
        return this.callSiteCache.computeIfAbsent(e, this::createCallSite);
    }

    private Collection<MethodHandle> createCallSite(Descriptor descriptor) {
        Class<?> javaClass = descriptor.getJavaClass();
        switch (this.type.ordinal()) {
            case 0: {
                ArrayList methodHandles = new ArrayList();
                JavaUtil.fields(javaClass).filter(f -> f.getName().equals(descriptor.getName())).map(ExprJavaCall::getAccess).filter(Objects::nonNull).forEach(field -> {
                    block4: {
                        try {
                            methodHandles.add(LOOKUP.unreflectGetter((Field)field));
                        }
                        catch (IllegalAccessException ex) {
                            Skript.warning((String)String.format("skript-reflect encountered a %s: %s%nRun Skript with the verbosity 'very high' for the stack trace.", ex.getClass().getSimpleName(), ex.getMessage()));
                            if (!Skript.logVeryHigh()) break block4;
                            StringWriter errors = new StringWriter();
                            ex.printStackTrace(new PrintWriter(errors));
                            Skript.warning((String)errors.toString());
                        }
                    }
                    try {
                        methodHandles.add(LOOKUP.unreflectSetter((Field)field));
                    }
                    catch (IllegalAccessException illegalAccessException) {
                        // empty catch block
                    }
                });
                return methodHandles.stream().filter(Objects::nonNull).limit(2L).collect(Collectors.toList());
            }
            case 1: {
                Stream<Method> methodStream = JavaUtil.methods(javaClass).filter(m -> m.getName().equals(descriptor.getName()));
                if (descriptor.getParameterTypes() != null) {
                    methodStream = methodStream.filter(m -> Arrays.equals(m.getParameterTypes(), descriptor.getParameterTypes()));
                }
                return methodStream.map(ExprJavaCall::getAccess).filter(Objects::nonNull).map(JavaUtil.propagateErrors(LOOKUP::unreflect)).filter(Objects::nonNull).collect(Collectors.toList());
            }
            case 2: {
                return JavaUtil.constructors(javaClass).map(ExprJavaCall::getAccess).filter(Objects::nonNull).map(JavaUtil.propagateErrors(LOOKUP::unreflectConstructor)).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }
        throw new IllegalStateException();
    }

    private T invoke(Object target, Object[] arguments, Descriptor baseDescriptor) {
        Object converted;
        Optional<MethodHandle> method;
        ++javaCallsMade;
        if (baseDescriptor == null) {
            return null;
        }
        Object returnedValue = null;
        Class<?> targetClass = SkriptMirrorUtil.toClassUnwrapJavaTypes(target);
        Descriptor descriptor = baseDescriptor.orDefaultClass(targetClass);
        if (!descriptor.getJavaClass().isAssignableFrom(targetClass)) {
            this.error(String.format("Incompatible %s call: %s on %s", new Object[]{this.type, descriptor, SkriptMirrorUtil.getDebugName(targetClass)}));
            return null;
        }
        boolean isStatic = target instanceof JavaType;
        Object[] argumentsCopy = isStatic ? ExprJavaCall.createStaticArgumentsCopy(arguments) : ExprJavaCall.createInstanceArgumentsCopy(target, arguments);
        if (isStatic && this.type == CallType.FIELD) {
            Class<?>[] classes;
            for (Class<?> clazz : classes = targetClass.getClasses()) {
                if (!descriptor.getName().equals(clazz.getSimpleName())) continue;
                return (T)Converters.convert((Object)new JavaType(clazz), (Class[])this.types);
            }
        }
        if (!(method = this.findCompatibleMethod(descriptor, argumentsCopy)).isPresent()) {
            this.error(String.format("No matching %s %s: %s%s", new Object[]{isStatic ? "static" : "non-static", this.type, descriptor.toString(isStatic), this.argumentsMessage(arguments)}));
            this.suggestParameters(descriptor, isStatic);
            this.suggestTypo(descriptor, isStatic);
            return null;
        }
        MethodHandle mh = method.get();
        argumentsCopy = ExprJavaCall.convertTypes(mh, argumentsCopy);
        try {
            returnedValue = mh.invokeWithArguments(argumentsCopy);
        }
        catch (Throwable throwable) {
            this.error(throwable, String.format("%s %s%s threw a %s: %s%n", new Object[]{this.type, descriptor, this.argumentsMessage(arguments), throwable.getClass().getSimpleName(), throwable.getMessage()}));
        }
        if (returnedValue == null) {
            return null;
        }
        if (this.superType == Object.class || this.superType == ObjectWrapper.class) {
            returnedValue = ObjectWrapper.wrapIfNecessary(returnedValue, this.superType == ObjectWrapper.class);
        }
        if ((converted = Converters.convert((Object)returnedValue, (Class[])this.types)) == null) {
            String toClasses = Arrays.stream(this.types).map(SkriptMirrorUtil::getDebugName).collect(Collectors.joining(", "));
            this.error(String.format("%s %s%s returned %s, which could not be converted to %s", new Object[]{this.type, descriptor, this.argumentsMessage(arguments), ExprJavaCall.argumentsToString(returnedValue), toClasses}));
            return null;
        }
        lastError = null;
        return (T)converted;
    }

    private Descriptor getDescriptor(Event e) {
        Descriptor parsedDescriptor;
        if (this.staticDescriptor != null) {
            return this.staticDescriptor;
        }
        String desc = (String)this.dynamicDescriptor.getSingle(e);
        if (desc == null) {
            this.error(String.format("Dynamic descriptor %s returned null", this.dynamicDescriptor.toString(e, false)));
            return null;
        }
        try {
            parsedDescriptor = Descriptor.parse(desc, this.script);
        }
        catch (ImportNotFoundException ex) {
            this.error("The class" + ex.getUserType() + " could not be found.");
            return null;
        }
        if (parsedDescriptor == null) {
            this.error(String.format("Invalid dynamic descriptor %s (%s)", this.dynamicDescriptor.toString(e, false), desc));
            return null;
        }
        return parsedDescriptor;
    }

    private static Object[] createStaticArgumentsCopy(Object[] args) {
        return Arrays.copyOf(args, args.length);
    }

    private static Object[] createInstanceArgumentsCopy(Object target, Object[] arguments) {
        Object[] copy = new Object[arguments.length + 1];
        copy[0] = target;
        System.arraycopy(arguments, 0, copy, 1, arguments.length);
        return copy;
    }

    private Optional<MethodHandle> findCompatibleMethod(Descriptor descriptor, Object[] args) {
        return this.getCallSite(descriptor).stream().filter(mh -> ExprJavaCall.matchesArgs(args, mh)).min(ExprJavaCall::prioritizeMethodHandles);
    }

    private static boolean matchesArgs(Object[] args, MethodHandle mh) {
        MethodType mt = mh.type();
        Class<?>[] params = mt.parameterArray();
        int varargsIndex = params.length - 1;
        boolean hasVarargs = mh.isVarargsCollector();
        if (!(args.length == params.length || hasVarargs && args.length >= varargsIndex)) {
            return false;
        }
        for (int i = 0; i < args.length; ++i) {
            boolean loopAtVarargs = hasVarargs && i >= varargsIndex;
            Class<?> param = loopAtVarargs ? params[varargsIndex].getComponentType() : params[i];
            Object arg = ObjectWrapper.unwrapIfNecessary(args[i]);
            if (JavaUtil.canConvert(arg, param) || loopAtVarargs && args.length == params.length && JavaUtil.canConvert(arg, params[i])) continue;
            return false;
        }
        return true;
    }

    private static int prioritizeMethodHandles(MethodHandle mh1, MethodHandle mh2) {
        boolean isMh2Varargs;
        boolean isMh1Varargs = mh1.isVarargsCollector();
        if (isMh1Varargs ^ (isMh2Varargs = mh2.isVarargsCollector())) {
            return isMh1Varargs ? 1 : -1;
        }
        return 0;
    }

    private static Object[] convertTypes(MethodHandle mh, Object[] args) {
        Class<?>[] params = mh.type().parameterArray();
        int varargsIndex = params.length - 1;
        boolean hasVarargs = mh.isVarargsCollector();
        for (int i = 0; i < args.length; ++i) {
            boolean loopAtVarargs = hasVarargs && i >= varargsIndex;
            Class<?> param = loopAtVarargs ? params[varargsIndex].getComponentType() : params[i];
            args[i] = ObjectWrapper.unwrapIfNecessary(args[i]);
            if (loopAtVarargs && args.length == params.length && params[i].isInstance(args[i])) {
                Object varargsArray = args[i];
                int varargsLength = Array.getLength(varargsArray);
                args = Arrays.copyOf(args, args.length - 1 + varargsLength);
                System.arraycopy(varargsArray, 0, args, varargsIndex, varargsLength);
            }
            args[i] = JavaUtil.convert(args[i], param);
        }
        return args;
    }

    private void suggestParameters(Descriptor descriptor, boolean isStatic) {
        if (this.type != CallType.CONSTRUCTOR && this.type != CallType.METHOD) {
            return;
        }
        String guess = descriptor.getName();
        Class<?> javaClass = descriptor.getJavaClass();
        Stream<Executable> members = this.getExecutables(javaClass);
        List<String> matches = members.filter(e -> e.getName().equals(guess)).filter(e -> isStatic == ExprJavaCall.isStatic(e)).map(Executable::getParameters).map(params -> Arrays.stream(params).map(Parameter::getType).map(Class::getTypeName).collect(Collectors.joining(","))).collect(Collectors.toList());
        if (!matches.isEmpty()) {
            this.directError("Did you pass the wrong parameters? Here are the parameter signatures for the " + (isStatic ? "static" : "non-static") + " " + String.valueOf((Object)this.type) + " " + guess + ":");
            matches.forEach(parameterList -> this.directError(String.format("* %s(%s)", guess, parameterList)));
        }
    }

    private void suggestTypo(Descriptor descriptor, boolean isStatic) {
        String guess = descriptor.getName();
        Class<?> javaClass = descriptor.getJavaClass();
        Stream<Member> members = this.getMembers(javaClass);
        ArrayList<Member> matchingMembers = new ArrayList<Member>();
        block0: for (Member member : members.collect(Collectors.toList())) {
            String name = member.getName();
            if (name.equals(guess) && isStatic == ExprJavaCall.isStatic(member)) continue;
            for (Member loopMember : matchingMembers) {
                if (!loopMember.getName().equals(name)) continue;
                continue block0;
            }
            StringSimilarity.Result result = StringSimilarity.compare(guess, name, 3);
            if (result == null) continue;
            matchingMembers.add(member);
        }
        if (!matchingMembers.isEmpty()) {
            this.directError("Did you misspell the " + String.valueOf((Object)this.type) + "? You may have meant to type one of the following:");
            for (Member member : matchingMembers) {
                String className = SkriptMirrorUtil.getDebugName(javaClass);
                Object staticString = ExprJavaCall.isStatic(member) ? className : "%" + className + "%";
                this.directError("* " + (String)staticString + "." + member.getName());
            }
        }
    }

    private Stream<? extends Executable> getExecutables(Class<?> javaClass) {
        switch (this.type.ordinal()) {
            case 1: {
                return JavaUtil.methods(javaClass);
            }
            case 2: {
                return JavaUtil.constructors(javaClass);
            }
        }
        throw new IllegalStateException();
    }

    private Stream<? extends Member> getMembers(Class<?> javaClass) {
        switch (this.type.ordinal()) {
            case 0: {
                return JavaUtil.fields(javaClass);
            }
            case 1: {
                return JavaUtil.methods(javaClass);
            }
            case 2: {
                return JavaUtil.constructors(javaClass);
            }
        }
        throw new IllegalStateException();
    }

    private String argumentsMessage(Object ... arguments) {
        if (this.type == CallType.FIELD) {
            return "";
        }
        if (arguments.length == 0) {
            return " called without arguments";
        }
        return " called with (" + ExprJavaCall.argumentsToString(arguments) + ")";
    }

    private static String argumentsToString(Object ... arguments) {
        return Arrays.stream(arguments).map(arg -> String.format("%s (%s)", Classes.toString((Object)arg), SkriptMirrorUtil.getDebugName(SkriptMirrorUtil.getClass(arg)))).collect(Collectors.joining(", "));
    }

    @Nullable
    private static <T extends AccessibleObject> T getAccess(T member) {
        try {
            if (!member.isAccessible()) {
                member.setAccessible(true);
            }
            return member;
        }
        catch (RuntimeException e) {
            if (e instanceof SecurityException || e.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                Member superMember = ExprJavaCall.getSuperMember((Member)((Object)member));
                return (T)(superMember != null ? ExprJavaCall.getAccess((AccessibleObject)((Object)superMember)) : null);
            }
            throw e;
        }
    }

    @Nullable
    private static Member getSuperMember(Member member) {
        if (!(member instanceof Method)) {
            return null;
        }
        Method method = (Method)member;
        if (ExprJavaCall.isStatic(method)) {
            return null;
        }
        return ExprJavaCall.getSuperMember(method, method.getDeclaringClass());
    }

    /*
     * WARNING - void declaration
     */
    @Nullable
    private static Method getSuperMember(Method method, Class<?> declaringClass) {
        if (method.getDeclaringClass() != declaringClass) {
            void var4_6;
            Method[] methodArray = declaringClass.getDeclaredMethods();
            int n = methodArray.length;
            boolean bl = false;
            while (var4_6 < n) {
                Method loopMethod = methodArray[var4_6];
                if (method.getName().equals(loopMethod.getName()) && Arrays.equals(method.getParameterTypes(), loopMethod.getParameterTypes())) {
                    return loopMethod;
                }
                ++var4_6;
            }
        }
        ArrayList superClasses = new ArrayList();
        if (declaringClass.getSuperclass() != null) {
            superClasses.add(declaringClass.getSuperclass());
        }
        superClasses.addAll(Arrays.asList(declaringClass.getInterfaces()));
        for (Class clazz : superClasses) {
            Method superMethod = ExprJavaCall.getSuperMember(method, clazz);
            if (superMethod == null) continue;
            return superMethod;
        }
        return null;
    }

    private static boolean isStatic(Member member) {
        return (member.getModifiers() & 8) != 0;
    }

    public String toString(Event e, boolean debug) {
        switch (this.type.ordinal()) {
            case 0: {
                return this.rawTarget.toString(e, debug) + "." + this.staticDescriptor.getName();
            }
            case 1: {
                return this.rawTarget.toString(e, debug) + "." + this.staticDescriptor.getName() + "(" + (this.rawArgs == null ? "" : this.rawArgs.toString(e, debug)) + ")";
            }
            case 2: {
                return "new " + this.rawTarget.toString(e, debug) + "(" + (this.rawArgs == null ? "" : this.rawArgs.toString(e, debug)) + ")";
            }
        }
        return null;
    }

    public String toString() {
        return this.toString(null, false);
    }

    private static enum CallType {
        FIELD,
        METHOD,
        CONSTRUCTOR;


        public String toString() {
            return this.name().toLowerCase();
        }
    }
}

