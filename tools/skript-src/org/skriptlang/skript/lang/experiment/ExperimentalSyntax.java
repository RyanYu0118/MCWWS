/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.experiment;

import ch.njol.skript.lang.SyntaxElement;
import org.skriptlang.skript.lang.experiment.ExperimentSet;

public interface ExperimentalSyntax
extends SyntaxElement {
    public boolean isSatisfiedBy(ExperimentSet var1);
}

