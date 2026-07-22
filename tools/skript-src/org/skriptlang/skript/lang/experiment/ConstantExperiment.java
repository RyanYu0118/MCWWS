/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.experiment;

import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.patterns.SkriptPattern;
import java.util.Objects;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.experiment.LifeCycle;

class ConstantExperiment
implements Experiment {
    private final String codeName;
    private final SkriptPattern compiledPattern;
    private final LifeCycle phase;

    ConstantExperiment(String codeName, LifeCycle phase) {
        this(codeName, phase, new String[0]);
    }

    ConstantExperiment(String codeName, LifeCycle phase, String ... patterns) {
        this.codeName = codeName;
        this.phase = phase;
        switch (patterns.length) {
            case 0: {
                this.compiledPattern = PatternCompiler.compile(codeName);
                break;
            }
            case 1: {
                this.compiledPattern = PatternCompiler.compile(patterns[0]);
                break;
            }
            default: {
                this.compiledPattern = PatternCompiler.compile(String.join((CharSequence)"|", patterns));
            }
        }
    }

    @Override
    public String codeName() {
        return this.codeName;
    }

    @Override
    public LifeCycle phase() {
        return this.phase;
    }

    @Override
    public SkriptPattern pattern() {
        return this.compiledPattern;
    }

    @Override
    public boolean matches(String text) {
        return this.codeName.equals(text);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Experiment that = (Experiment)o;
        return Objects.equals(this.codeName(), that.codeName());
    }

    public int hashCode() {
        return this.codeName.hashCode();
    }
}

