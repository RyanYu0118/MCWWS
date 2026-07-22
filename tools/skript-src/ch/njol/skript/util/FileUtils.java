/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.util;

import ch.njol.skript.SkriptConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.zip.GZIPOutputStream;
import org.skriptlang.skript.lang.converter.Converter;

public abstract class FileUtils {
    private static final SimpleDateFormat backupFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private FileUtils() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String getBackupSuffix() {
        SimpleDateFormat simpleDateFormat = backupFormat;
        synchronized (simpleDateFormat) {
            return backupFormat.format(System.currentTimeMillis());
        }
    }

    public static void backupPurge(File varFile, int toKeep) throws IOException, IllegalArgumentException {
        if (toKeep < 0) {
            throw new IllegalArgumentException("Called with invalid input, 'toKeep' can not be less than 0");
        }
        File backupDir = new File(varFile.getParentFile(), "backups" + File.separator);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            throw new IOException("Backup directory not found");
        }
        ArrayList<File> files = new ArrayList<File>(Arrays.asList(backupDir.listFiles()));
        if (files == null || files.size() <= toKeep) {
            return;
        }
        if (toKeep > 0) {
            files.sort(Comparator.comparingLong(File::lastModified));
        }
        int numberToRemove = files.size() - toKeep;
        for (int i = 0; i < numberToRemove; ++i) {
            files.get(i).delete();
        }
    }

    public static File backup(File file) throws IOException {
        Path backup;
        block17: {
            String ext;
            String name;
            String originalName;
            int dotIdx;
            Path source = file.toPath();
            Path backupFolder = source.getParent().resolve("backups");
            if (!Files.exists(backupFolder, new LinkOption[0])) {
                Files.createDirectories(backupFolder, new FileAttribute[0]);
            }
            if ((dotIdx = (originalName = file.getName()).lastIndexOf(46)) != -1) {
                name = originalName.substring(0, dotIdx);
                ext = originalName.substring(dotIdx);
            } else {
                name = originalName;
                ext = "";
            }
            String newFileName = name + "_" + FileUtils.getBackupSuffix() + ext;
            boolean compress = SkriptConfig.compressBackups.value();
            if (compress) {
                newFileName = newFileName + ".gz";
            }
            if (Files.exists(backup = backupFolder.resolve(newFileName), new LinkOption[0])) {
                throw new IOException("Backup file " + String.valueOf(backup.getFileName()) + " already exists");
            }
            if (compress) {
                class GZIPOutputStreamWithLevel
                extends GZIPOutputStream {
                    public GZIPOutputStreamWithLevel(OutputStream out, int level) throws IOException {
                        super(out);
                        this.def.setLevel(level);
                    }
                }
                try (OutputStream os = Files.newOutputStream(backup, new OpenOption[0]);
                     GZIPOutputStreamWithLevel gzipOs = new GZIPOutputStreamWithLevel(os, 9);){
                    Files.copy(source, gzipOs);
                    break block17;
                }
            }
            FileUtils.copy(source, backup);
        }
        return backup.toFile();
    }

    public static File move(File from, File to, boolean replace) throws IOException {
        if (!replace && to.exists()) {
            throw new IOException("Can't rename " + from.getName() + " to " + to.getName() + ": The target file already exists");
        }
        if (replace) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } else {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }
        return to;
    }

    public static void copy(File from, File to) throws IOException {
        FileUtils.copy(from.toPath(), to.toPath());
    }

    public static void copy(Path from, Path to) throws IOException {
        Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES);
    }

    public static Collection<File> renameAll(File directory, Converter<String, String> renamer) throws IOException {
        ArrayList<File> changed = new ArrayList<File>();
        for (File f : directory.listFiles()) {
            String newName;
            if (f.isDirectory()) {
                changed.addAll(FileUtils.renameAll(f, renamer));
                continue;
            }
            String name = f.getName();
            if (name == null || (newName = renamer.convert(name)) == null) continue;
            File newFile = new File(f.getParent(), newName);
            FileUtils.move(f, newFile, false);
            changed.add(newFile);
        }
        return changed;
    }

    public static void save(InputStream in, File file) throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file);){
            int read;
            byte[] buffer = new byte[16384];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }
}

