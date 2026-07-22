/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 */
package org.skriptlang.skript.lang.experiment;

import ch.njol.skript.patterns.SkriptPattern;
import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.lang.experiment.ConstantExperiment;
import org.skriptlang.skript.lang.experiment.LifeCycle;
import org.skriptlang.skript.lang.experiment.UnmatchedExperiment;

public interface Experiment {
    @ApiStatus.Internal
    public static Experiment unknown(String text) {
        return new UnmatchedExperiment(text);
    }

    public static Experiment constant(String codeName, LifeCycle phase, String ... patterns) {
        return new ConstantExperiment(codeName, phase, patterns);
    }

    public String codeName();

    public LifeCycle phase();

    default public boolean isKnown() {
        return this.phase() != LifeCycle.UNKNOWN;
    }

    public SkriptPattern pattern();

    default public boolean matches(String text) {
        return this.pattern().match(text) != null;
    }
}

