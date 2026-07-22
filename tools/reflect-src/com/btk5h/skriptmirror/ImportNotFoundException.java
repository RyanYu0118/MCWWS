/*
 * Decompiled with CFR 0.152.
 */
package com.btk5h.skriptmirror;

public class ImportNotFoundException
extends Exception {
    private final String userType;

    public ImportNotFoundException(String userType) {
        super("Import not found: " + userType);
        this.userType = userType;
    }

    public String getUserType() {
        return this.userType;
    }
}

