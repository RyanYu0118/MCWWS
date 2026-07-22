/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.lang.experiment;

import java.util.Collection;
import java.util.LinkedHashSet;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.experiment.Experimented;
import org.skriptlang.skript.lang.script.ScriptData;

public class ExperimentSet
extends LinkedHashSet<Experiment>
implements ScriptData,
Experimented {
    public ExperimentSet(@NotNull Collection<? extends Experiment> collection) {
        super(collection);
    }

    public ExperimentSet() {
    }

    @Override
    public boolean hasExperiment(Experiment experiment) {
        return this.contains(experiment);
    }

    @Override
    public boolean hasExperiment(String featureName) {
        for (Experiment experiment : this) {
            if (!experiment.matches(featureName)) continue;
            return true;
        }
        return false;
    }
}

