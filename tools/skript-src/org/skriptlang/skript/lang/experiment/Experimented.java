/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.experiment;

import ch.njol.skript.Skript;
import org.skriptlang.skript.lang.experiment.Experiment;

@FunctionalInterface
public interface Experimented {
    public boolean hasExperiment(Experiment var1);

    default public boolean hasExperiment(String featureName) {
        return Skript.experiments().find(featureName).isKnown();
    }
}

