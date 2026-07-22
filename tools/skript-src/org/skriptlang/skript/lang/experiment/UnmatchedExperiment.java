/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.experiment;

import org.skriptlang.skript.lang.experiment.ConstantExperiment;
import org.skriptlang.skript.lang.experiment.LifeCycle;

class UnmatchedExperiment
extends ConstantExperiment {
    UnmatchedExperiment(String codeName) {
        super(codeName, LifeCycle.UNKNOWN);
    }

    @Override
    public LifeCycle phase() {
        return LifeCycle.UNKNOWN;
    }

    @Override
    public boolean isKnown() {
        return false;
    }
}

