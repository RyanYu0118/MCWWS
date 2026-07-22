/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.update;

import java.net.URL;

public class UpdateManifest {
    public final String id;
    public final String date;
    public final String patchNotes;
    public final URL downloadUrl;

    public UpdateManifest(String id, String date, String patchNotes, URL downloadUrl) {
        this.id = id;
        this.date = date;
        this.patchNotes = patchNotes;
        this.downloadUrl = downloadUrl;
    }
}

