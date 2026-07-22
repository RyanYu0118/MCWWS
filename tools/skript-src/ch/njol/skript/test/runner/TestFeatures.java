/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.patterns.SkriptPattern;
import ch.njol.skript.test.runner.TestMode;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.experiment.ExperimentRegistry;
import org.skriptlang.skript.lang.experiment.LifeCycle;

public enum TestFeatures implements Experiment
{
    EXAMPLE_FEATURE("example feature", LifeCycle.STABLE),
    DEPRECATED_FEATURE("deprecated feature", LifeCycle.DEPRECATED),
    TEST_FEATURE("test", LifeCycle.EXPERIMENTAL, "test[ing]", "fizz[ ]buzz");

    private final String codeName;
    private final LifeCycle phase;
    private final SkriptPattern compiledPattern;

    private TestFeatures(String codeName, LifeCycle phase, String ... patterns) {
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
                this.compiledPattern = PatternCompiler.compile("(" + String.join((CharSequence)"|", patterns) + ")");
            }
        }
    }

    private TestFeatures(String codeName, LifeCycle phase) {
        this(codeName, phase, codeName);
    }

    public static void registerAll(SkriptAddon addon, ExperimentRegistry manager) {
        for (TestFeatures value : TestFeatures.values()) {
            manager.register(addon, value);
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

    static {
        if (!TestMode.GEN_DOCS) {
            TestFeatures.registerAll(Skript.getAddonInstance(), Skript.experiments());
        }
    }
}

