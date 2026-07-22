/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Variable
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.FunctionWrapper;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprProxy
extends SimpleExpression<Object> {
    public static boolean proxiesUsed = false;
    private Expression<JavaType> interfaces;
    private Variable<?> handler;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression var;
        proxiesUsed = true;
        this.interfaces = SkriptUtil.defendExpression(exprs[0]);
        if (this.interfaces instanceof Literal) {
            JavaType[] javaTypes;
            for (JavaType javaType : javaTypes = (JavaType[])((Literal)this.interfaces).getArray()) {
                if (javaType.getJavaClass().isInterface()) continue;
                Skript.warning((String)(String.valueOf(javaType) + " is not an interface"));
            }
        }
        if ((var = SkriptUtil.defendExpression(exprs[1])) instanceof Variable && ((Variable)var).isList()) {
            this.handler = (Variable)var;
            return true;
        }
        Skript.error((String)(String.valueOf(var) + " is not a list variable."));
        return false;
    }

    protected Object[] get(Event e) {
        HashMap<String, FunctionWrapper> handlers = new HashMap<String, FunctionWrapper>();
        HashMap<String, Section> sectionHandlers = new HashMap<String, Section>();
        this.handler.variablesIterator(e).forEachRemaining(pair -> {
            Object value = pair.getValue();
            if (value instanceof FunctionWrapper) {
                handlers.put((String)pair.getKey(), (FunctionWrapper)value);
            } else if (value instanceof Section) {
                sectionHandlers.put((String)pair.getKey(), (Section)value);
            }
        });
        return new Object[]{Proxy.newProxyInstance(LibraryLoader.getClassLoader(), (Class[])Arrays.stream((JavaType[])this.interfaces.getArray(e)).map(JavaType::getJavaClass).filter(Class::isInterface).toArray(Class[]::new), new VariableInvocationHandler(handlers, sectionHandlers))};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<?> getReturnType() {
        return Object.class;
    }

    public String toString(Event e, boolean debug) {
        return String.format("proxy of %s from %s", this.interfaces.toString(e, debug), this.handler.toString(e, debug));
    }

    static {
        Skript.registerExpression(ExprProxy.class, Object.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"[a] [new] proxy [instance] of %javatypes% (using|from) %objects%"});
    }

    private static class VariableInvocationHandler
    implements InvocationHandler {
        @Nullable
        private static final Method INVOKE_DEFAULT;
        private final Map<String, FunctionWrapper> handlers;
        private final Map<String, Section> sectionHandlers;

        public VariableInvocationHandler(Map<String, FunctionWrapper> handlers, Map<String, Section> sectionHandlers) {
            this.handlers = handlers;
            this.sectionHandlers = sectionHandlers;
        }

        /*
         * Exception decompiling
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] methodArgs) {
            /*
             * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
             * 
             * java.lang.UnsupportedOperationException
             *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray.getDimSize(NewAnonymousArray.java:142)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.isNewArrayLambda(LambdaRewriter.java:455)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:409)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:167)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:105)
             *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
             *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
             *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
             *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:87)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
             *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement.rewriteExpressions(StructuredExpressionStatement.java:70)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:88)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
             *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
             *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseInnerClassesPass1(ClassFile.java:923)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1035)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
             *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
             *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
             *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
             *     at org.benf.cfr.reader.Main.main(Main.java:54)
             */
            throw new IllegalStateException("Decompilation failed");
        }

        private static /* synthetic */ Object[][] lambda$invoke$2(int x$0) {
            return new Object[x$0][];
        }

        private static /* synthetic */ Object[] lambda$invoke$1(Object arg) {
            return new Object[]{arg};
        }

        static {
            Method method;
            try {
                method = InvocationHandler.class.getDeclaredMethod("invokeDefault", Object.class, Method.class, Object[].class);
            }
            catch (NoSuchMethodException e) {
                method = null;
            }
            INVOKE_DEFAULT = method;
        }
    }
}

