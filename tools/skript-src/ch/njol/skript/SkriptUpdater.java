/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 */
package ch.njol.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Message;
import ch.njol.skript.update.ReleaseManifest;
import ch.njol.skript.update.ReleaseStatus;
import ch.njol.skript.update.UpdateManifest;
import ch.njol.skript.update.Updater;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.bukkit.command.CommandSender;
import org.skriptlang.skript.bukkit.text.TextComponentParser;

public class SkriptUpdater
extends Updater {
    public static final Message m_not_started = new Message("updater.not started");
    public static final Message m_checking = new Message("updater.checking");
    public static final Message m_check_in_progress = new Message("updater.check in progress");
    public static final Message m_updater_disabled = new Message("updater.updater disabled");
    public static final ArgsMessage m_check_error = new ArgsMessage("updater.check error");
    public static final Message m_running_latest_version = new Message("updater.running latest version");
    public static final Message m_running_latest_version_beta = new Message("updater.running latest version (beta)");
    public static final ArgsMessage m_update_available = new ArgsMessage("updater.update available");
    public static final ArgsMessage m_downloading = new ArgsMessage("updater.downloading");
    public static final Message m_download_in_progress = new Message("updater.download in progress");
    public static final ArgsMessage m_download_error = new ArgsMessage("updater.download error");
    public static final ArgsMessage m_downloaded = new ArgsMessage("updater.downloaded");
    public static final Message m_internal_error = new Message("updater.internal error");
    public static final Message m_custom_version = new Message("updater.custom version");
    public static final Message m_nightly = new Message("updater.nightly build");

    SkriptUpdater() {
        super(SkriptUpdater.loadManifest());
    }

    private static ReleaseManifest loadManifest() {
        String manifest;
        try (InputStream is = Skript.getInstance().getResource("release-manifest.json");
             Scanner s = new Scanner(is);){
            s.useDelimiter("\\\\A");
            manifest = s.next();
        }
        catch (IOException e) {
            throw new IllegalStateException("Skript is missing release-manifest.json!");
        }
        assert (manifest != null);
        return ReleaseManifest.load(manifest);
    }

    public CompletableFuture<Void> updateCheck(CommandSender sender) {
        CompletionStage future = this.checkUpdates().thenAccept(none -> {
            ReleaseStatus status = this.getReleaseStatus();
            switch (status) {
                case CUSTOM: {
                    Skript.info(sender, String.valueOf(m_custom_version));
                    break;
                }
                case DEVELOPMENT: {
                    Skript.info(sender, String.valueOf(m_nightly));
                    break;
                }
                case LATEST: {
                    Skript.info(sender, String.valueOf(m_running_latest_version));
                    break;
                }
                case OUTDATED: {
                    UpdateManifest update = this.getUpdateManifest();
                    assert (update != null);
                    Skript.info(sender, m_update_available.toString(update.id, Skript.getVersion()));
                    sender.sendMessage(TextComponentParser.instance().parse("Download it at: <aqua><underlined><click:open_url:" + String.valueOf(update.downloadUrl) + ">" + String.valueOf(update.downloadUrl)));
                    break;
                }
                case UNKNOWN: {
                    if (this.isEnabled()) {
                        Skript.error(sender, String.valueOf(m_internal_error));
                        break;
                    }
                    Skript.info(sender, String.valueOf(m_updater_disabled));
                }
            }
        });
        assert (future != null);
        return future;
    }

    public CompletableFuture<Void> changesCheck(CommandSender sender) {
        CompletionStage future = this.updateCheck(sender).thenAccept(none -> {
            UpdateManifest update;
            if (this.getReleaseStatus() == ReleaseStatus.OUTDATED && (update = this.getUpdateManifest()) != null) {
                sender.sendMessage("");
                Skript.info(sender, "Patch notes:");
                for (String line : update.patchNotes.split("\\n")) {
                    String processed = line = line.replace("\r", "");
                    int start = line.indexOf(35);
                    while (start != -1) {
                        int c;
                        StringBuilder issue = new StringBuilder();
                        for (int i = start + 1; i < line.length() && Character.isDigit(c = line.codePointAt(i)); i += Character.charCount(c)) {
                            issue.appendCodePoint(c);
                        }
                        if (issue.length() > 0) {
                            processed = processed.replace("#" + String.valueOf(issue), "<aqua><underlined><click:open_url:https://github.com/SkriptLang/Skript/issues/" + String.valueOf(issue) + ">#" + String.valueOf(issue) + "<r>");
                        }
                        start = line.indexOf(35, start + 1);
                    }
                    line = processed;
                    assert (line != null);
                    sender.sendMessage(TextComponentParser.instance().parse(line));
                }
            }
        });
        assert (future != null);
        return future;
    }
}

