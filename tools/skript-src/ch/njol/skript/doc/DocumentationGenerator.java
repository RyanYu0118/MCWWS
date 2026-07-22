/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.doc;

import java.io.File;

@Deprecated(forRemoval=true, since="2.13")
public abstract class DocumentationGenerator {
    protected File templateDir;
    protected File outputDir;

    public DocumentationGenerator(File templateDir, File outputDir) {
        this.templateDir = templateDir;
        this.outputDir = outputDir;
    }

    @Deprecated(forRemoval=true, since="2.13")
    public abstract void generate();
}

