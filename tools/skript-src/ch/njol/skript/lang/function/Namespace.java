/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Signature;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class Namespace {
    private final Map<Info, Signature<?>> signatures = new HashMap();
    private final Map<Info, Function<?>> functions = new HashMap();

    @Nullable
    public Signature<?> getSignature(String name, boolean local) {
        return this.signatures.get(new Info(name, local));
    }

    @Nullable
    public Signature<?> getSignature(String name) {
        Signature<?> signature = this.getSignature(name, true);
        return signature == null ? this.getSignature(name, false) : signature;
    }

    public void addSignature(Signature<?> sign) {
        Info info = new Info(sign.getName(), sign.isLocal());
        if (this.signatures.containsKey(info)) {
            throw new IllegalArgumentException("function name already used");
        }
        this.signatures.put(info, sign);
    }

    public boolean removeSignature(Signature<?> sign) {
        Info info = new Info(sign.getName(), sign.isLocal());
        if (this.signatures.get(info) != sign) {
            return false;
        }
        this.signatures.remove(info);
        return true;
    }

    public Collection<Signature<?>> getSignatures() {
        return this.signatures.values();
    }

    @Nullable
    public Function<?> getFunction(String name, boolean local) {
        return this.functions.get(new Info(name, local));
    }

    @Nullable
    public Function<?> getFunction(String name) {
        Function<?> function = this.getFunction(name, true);
        return function == null ? this.getFunction(name, false) : function;
    }

    public void addFunction(Function<?> func) {
        Info info = new Info(func.getName(), func.getSignature().isLocal());
        assert (this.signatures.containsKey(info)) : "missing signature for function";
        this.functions.put(info, func);
    }

    public Collection<Function<?>> getFunctions() {
        return this.functions.values();
    }

    private static class Info {
        private final String name;
        private final boolean local;

        public Info(String name, boolean local) {
            this.name = name;
            this.local = local;
        }

        public String getName() {
            return this.name;
        }

        public boolean isLocal() {
            return this.local;
        }

        public int hashCode() {
            int result = this.getName().hashCode();
            result = 31 * result + (this.isLocal() ? 1 : 0);
            return result;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Info)) {
                return false;
            }
            Info info = (Info)o;
            if (this.isLocal() != info.isLocal()) {
                return false;
            }
            return this.getName().equals(info.getName());
        }
    }

    public static class Key {
        private final Origin origin;
        @Nullable
        private final String scriptName;

        public Key(Origin origin, @Nullable String scriptName) {
            this.origin = origin;
            this.scriptName = scriptName;
        }

        public Origin getOrigin() {
            return this.origin;
        }

        @Nullable
        public String getScriptName() {
            return this.scriptName;
        }

        public int hashCode() {
            int result = this.origin.hashCode();
            result = 31 * result + (this.scriptName != null ? this.scriptName.hashCode() : 0);
            return result;
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || this.getClass() != object.getClass()) {
                return false;
            }
            Key other = (Key)object;
            if (this.origin != other.origin) {
                return false;
            }
            return Objects.equals(this.scriptName, other.scriptName);
        }
    }

    public static enum Origin {
        JAVA,
        SCRIPT;

    }
}

