/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.lang.experiment;

import ch.njol.skript.Skript;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.experiment.ExperimentSet;

public class ExperimentData {
    private final @Unmodifiable Set<Experiment> required;
    private final @Unmodifiable Set<Experiment> disallowed;
    private final String errorMessage;

    public static Builder builder() {
        return new Builder();
    }

    public static ExperimentData createSingularData(Experiment experiment) {
        return ExperimentData.builder().required(experiment).build();
    }

    private ExperimentData(Set<Experiment> required, Set<Experiment> disallowed, @Nullable String errorMessage) {
        this.required = Collections.unmodifiableSet(required);
        this.disallowed = Collections.unmodifiableSet(disallowed);
        this.errorMessage = errorMessage != null ? errorMessage : this.constructError();
    }

    public @Unmodifiable Set<Experiment> getRequired() {
        return this.required;
    }

    public @Unmodifiable Set<Experiment> getDisallowed() {
        return this.disallowed;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public boolean checkRequirements(ExperimentSet experiments) {
        for (Experiment experiment : this.required) {
            if (experiments.hasExperiment(experiment)) continue;
            return false;
        }
        for (Experiment experiment : this.disallowed) {
            if (!experiments.hasExperiment(experiment)) continue;
            return false;
        }
        return true;
    }

    public boolean checkRequirementsAndError(ExperimentSet experiments) {
        if (!this.checkRequirements(experiments)) {
            Skript.error(this.errorMessage);
            return false;
        }
        return true;
    }

    public String constructError() {
        StringBuilder builder = new StringBuilder();
        builder.append("This element is experimental. To use this, ");
        if (!this.required.isEmpty()) {
            builder.append("enable ");
            builder.append(this.required.stream().map(experiment -> "'" + experiment.codeName() + "'").collect(Collectors.joining(", ")));
            if (!this.disallowed.isEmpty()) {
                builder.append(" and ");
            }
        }
        if (!this.disallowed.isEmpty()) {
            builder.append("disable ");
            builder.append(this.disallowed.stream().map(experiment -> "'" + experiment.codeName() + "'").collect(Collectors.joining(", ")));
        }
        builder.append(".");
        return builder.toString();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.required = new HashSet<Experiment>(this.required);
        builder.disallowed = new HashSet<Experiment>(this.disallowed);
        builder.errorMessage = this.errorMessage;
        return builder;
    }

    public static class Builder {
        private Set<Experiment> required = new HashSet<Experiment>();
        private Set<Experiment> disallowed = new HashSet<Experiment>();
        @Nullable
        private String errorMessage = null;

        private Builder() {
        }

        public Builder required(Experiment ... required) {
            this.required = Arrays.stream(required).collect(Collectors.toSet());
            return this;
        }

        public Builder disallowed(Experiment ... disallowed) {
            this.disallowed = Arrays.stream(disallowed).collect(Collectors.toSet());
            return this;
        }

        public Builder errorMessage(@Nullable String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ExperimentData build() {
            if (this.required.isEmpty() && this.disallowed.isEmpty()) {
                throw new IllegalArgumentException("Must have required and/or disallowed Experiments.");
            }
            if (!this.required.isEmpty() && !this.disallowed.isEmpty()) {
                for (Experiment req : this.required) {
                    if (!this.disallowed.contains(req)) continue;
                    throw new IllegalArgumentException("An Experiment can not be both required and disallowed: '" + String.valueOf(req) + "'");
                }
            }
            return new ExperimentData(this.required, this.disallowed, this.errorMessage);
        }
    }
}

