/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.jetbrains.annotations.Nullable;

public class ConfigReader
extends BufferedReader {
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    @Nullable
    private String line;
    private boolean reset = false;
    private int ln = 0;
    private boolean hasNonEmptyLine = false;

    public ConfigReader(InputStream source) {
        super(new InputStreamReader(source, UTF_8));
    }

    @Override
    @Nullable
    public String readLine() throws IOException {
        if (this.reset) {
            this.reset = false;
        } else {
            this.line = this.stripUTF8BOM(super.readLine());
            ++this.ln;
        }
        return this.line;
    }

    @Nullable
    private final String stripUTF8BOM(@Nullable String line) {
        if (!this.hasNonEmptyLine && line != null && !line.isEmpty()) {
            this.hasNonEmptyLine = true;
            if (line.startsWith("\ufeff")) {
                return line.substring(1);
            }
        }
        return line;
    }

    @Override
    public void reset() {
        if (this.reset) {
            throw new IllegalStateException("reset was called twice without a readLine inbetween");
        }
        this.reset = true;
    }

    public int getLineNum() {
        return this.ln;
    }

    @Nullable
    public String getLine() {
        return this.line;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new UnsupportedOperationException();
    }
}

