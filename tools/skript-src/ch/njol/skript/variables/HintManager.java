/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.variables;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.util.slot.Slot;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

@ApiStatus.Experimental
public class HintManager {
    private final LinkedList<Scope> typeHints = new LinkedList();
    private boolean isActive;

    public HintManager(boolean active) {
        this.isActive = active;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isActive() {
        ParserInstance parser = ParserInstance.get();
        return this.isActive && parser.isActive() && parser.hasExperiment(Feature.TYPE_HINTS);
    }

    public void enterScope(boolean isSection) {
        this.typeHints.push(new Scope(new HashMap(), isSection));
        if (this.typeHints.size() > 1) {
            this.mergeScope(1, 0, false);
        }
    }

    public void exitScope() {
        if (this.typeHints.size() > 1) {
            this.mergeScope(0, 1, false);
        }
        this.typeHints.pop();
    }

    public void clearScope(int level, boolean sectionOnly) {
        if (level < 0 || level > this.typeHints.size() - 1) {
            throw new IndexOutOfBoundsException("Scope level " + level + " is out of bounds (expected 0-" + (this.typeHints.size() - 1) + ")");
        }
        if (!sectionOnly) {
            this.typeHints.get(level).hintMap().clear();
            return;
        }
        int currentLevel = 0;
        Iterator iterator = this.typeHints.iterator();
        while (iterator.hasNext()) {
            Scope scope = (Scope)iterator.next();
            if (!scope.isSection()) continue;
            if (currentLevel == level) {
                iterator.remove();
                return;
            }
            ++currentLevel;
        }
        throw new IndexOutOfBoundsException("Section scope level " + level + " is out of bounds (expected 0-" + currentLevel + ")");
    }

    public void mergeScope(int from, int to, boolean sectionOnly) {
        int expectedSize = this.typeHints.size() - 1;
        if (from < 0 || from > expectedSize) {
            throw new IndexOutOfBoundsException("'from' scope level " + from + " is out of bounds (expected 0-" + expectedSize + ")");
        }
        if (to < 0 || to > expectedSize) {
            throw new IndexOutOfBoundsException("'to' scope level " + to + " is out of bounds (expected 0-" + expectedSize + ")");
        }
        Scope fromScope = null;
        Scope toScope = null;
        if (sectionOnly) {
            int currentLevel = 0;
            for (Scope scope : this.typeHints) {
                if (!scope.isSection()) continue;
                if (currentLevel == from) {
                    fromScope = scope;
                }
                if (currentLevel == to) {
                    toScope = scope;
                }
                if (fromScope != null && toScope != null) break;
                ++currentLevel;
            }
            if (fromScope == null) {
                throw new IndexOutOfBoundsException("'from' section scope level " + from + " is out of bounds (expected 0-" + currentLevel + ")");
            }
            if (toScope == null) {
                throw new IndexOutOfBoundsException("'to' section scope level " + to + " is out of bounds (expected 0-" + currentLevel + ")");
            }
        } else {
            fromScope = this.typeHints.get(from);
            toScope = this.typeHints.get(to);
        }
        HintManager.mergeHints(fromScope.hintMap(), toScope.hintMap());
    }

    private static void mergeHints(Map<String, Set<Class<?>>> from, Map<String, Set<Class<?>>> to) {
        for (Map.Entry<String, Set<Class<?>>> entry : from.entrySet()) {
            to.computeIfAbsent(entry.getKey(), key -> new HashSet()).addAll((Collection)entry.getValue());
        }
    }

    public void set(Variable<?> variable, Class<?> ... hints) {
        HintManager.checkCanUseHints(variable);
        this.set(variable.getName().toString(null), hints);
    }

    public void set(String variableName, Class<?> ... hints) {
        if (this.areHintsUnavailable()) {
            return;
        }
        this.delete_i(variableName);
        if (hints.length != 0) {
            this.add_i(variableName, Set.of(hints));
        }
    }

    public void delete(Variable<?> variable) {
        HintManager.checkCanUseHints(variable);
        this.delete(variable.getName().toString(null));
    }

    public void delete(String variableName) {
        if (this.areHintsUnavailable()) {
            return;
        }
        this.delete_i(variableName);
    }

    private void delete_i(String variableName) {
        if (SkriptConfig.caseInsensitiveVariables.value().booleanValue()) {
            variableName = variableName.toLowerCase(Locale.ENGLISH);
        }
        this.typeHints.getFirst().hintMap().remove(variableName);
        if (variableName.endsWith("::*")) {
            String prefix = variableName.substring(0, variableName.length() - 1);
            this.typeHints.getFirst().hintMap().keySet().removeIf(key -> key.startsWith(prefix));
        }
    }

    public void add(Variable<?> variable, Class<?> ... hints) {
        HintManager.checkCanUseHints(variable);
        this.add(variable.getName().toString(null), hints);
    }

    public void add(String variableName, Class<?> ... hints) {
        if (this.areHintsUnavailable()) {
            return;
        }
        this.add_i(variableName, Set.of(hints));
    }

    private void add_i(String variableName, Set<Class<?>> hintSet) {
        int listEnd;
        if (SkriptConfig.caseInsensitiveVariables.value().booleanValue()) {
            variableName = variableName.toLowerCase(Locale.ENGLISH);
        }
        if (hintSet.contains(Slot.class)) {
            hintSet = new HashSet(hintSet);
            hintSet.add(ItemStack.class);
        }
        this.typeHints.getFirst().hintMap().computeIfAbsent(variableName, key -> new HashSet()).addAll(hintSet);
        if (!variableName.isEmpty() && variableName.charAt(variableName.length() - 1) != '*' && (listEnd = variableName.lastIndexOf("::")) != -1) {
            String listVariableName = variableName.substring(0, listEnd + "::".length()) + "*";
            this.typeHints.getFirst().hintMap().computeIfAbsent(listVariableName, key -> new HashSet()).addAll(hintSet);
        }
    }

    public void remove(Variable<?> variable, Class<?> ... hints) {
        HintManager.checkCanUseHints(variable);
        this.remove(variable.getName().toString(null), hints);
    }

    public void remove(String variableName, Class<?> ... hints) {
        Set<Class<?>> hintSet;
        if (this.areHintsUnavailable()) {
            return;
        }
        if (SkriptConfig.caseInsensitiveVariables.value().booleanValue()) {
            variableName = variableName.toLowerCase(Locale.ENGLISH);
        }
        if ((hintSet = this.typeHints.getFirst().hintMap().get(variableName)) != null) {
            for (Class<?> hint : hints) {
                hintSet.remove(hint);
            }
            if (hintSet.isEmpty()) {
                this.delete_i(variableName);
            }
        }
    }

    public @Unmodifiable Set<Class<?>> get(Variable<?> variable) {
        HintManager.checkCanUseHints(variable);
        return this.get(variable.getName().toString(null));
    }

    public @Unmodifiable Set<Class<?>> get(String variableName) {
        Set<Class<?>> hintSet;
        if (this.areHintsUnavailable()) {
            return ImmutableSet.of();
        }
        if (SkriptConfig.caseInsensitiveVariables.value().booleanValue()) {
            variableName = variableName.toLowerCase(Locale.ENGLISH);
        }
        if ((hintSet = this.typeHints.getFirst().hintMap().get(variableName)) != null) {
            return ImmutableSet.copyOf(hintSet);
        }
        return ImmutableSet.of();
    }

    public Backup backup() {
        return new Backup(this);
    }

    public void restore(Backup backup) {
        this.typeHints.set(0, backup.scope);
    }

    private boolean areHintsUnavailable() {
        if (!this.isActive()) {
            return true;
        }
        if (this.typeHints.isEmpty()) {
            if (SkriptLogger.debug()) {
                SkriptLogger.LOGGER.warning("Attempted to use type hints outside of any scope");
            }
            return true;
        }
        return false;
    }

    public static boolean canUseHints(Variable<?> variable) {
        return variable.isLocal() && variable.getName().isSimple();
    }

    private static void checkCanUseHints(Variable<?> variable) {
        if (!HintManager.canUseHints(variable)) {
            throw new IllegalArgumentException("Variables must be local and have a simple name to have hints");
        }
    }

    private record Scope(Map<String, Set<Class<?>>> hintMap, boolean isSection) {
    }

    public static final class Backup {
        private final Scope scope;

        private Backup(HintManager source) {
            this.scope = new Scope(new HashMap(), source.typeHints.getFirst().isSection);
            HintManager.mergeHints(source.typeHints.getFirst().hintMap(), this.scope.hintMap());
        }
    }
}

