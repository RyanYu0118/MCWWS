/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.experiment;

import org.skriptlang.skript.lang.experiment.ExperimentData;
import org.skriptlang.skript.lang.experiment.ExperimentSet;
import org.skriptlang.skript.lang.experiment.ExperimentalSyntax;

public interface SimpleExperimentalSyntax
extends ExperimentalSyntax {
    @Override
    default public boolean isSatisfiedBy(ExperimentSet experimentSet) {
        return this.getExperimentData().checkRequirementsAndError(experimentSet);
    }

    public ExperimentData getExperimentData();
}

