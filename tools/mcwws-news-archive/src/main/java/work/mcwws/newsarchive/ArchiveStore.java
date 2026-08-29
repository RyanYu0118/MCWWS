package work.mcwws.newsarchive;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArchiveStore {
    private static final Pattern PUBLISH_LINE = Pattern.compile("发布[：:]\\s*(.+)");
    private static final Pattern TITLE_LINE = Pattern.compile("&0&l(.+)");
    private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final McwwsNewsArchivePlugin plugin;
    private final File versionsDir;
    private final File readsFile;
    private final Map<String, NewsVersion> versionsById = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> reads = new ConcurrentHashMap<>();
    private final Map<String, InteractiveWord> interactiveWords = new LinkedHashMap<>();

    public ArchiveStore(McwwsNewsArchivePlugin plugin) {
        this.plugin = plugin;
        this.versionsDir = new File(plugin.getDataFolder(), "versions");
        this.readsFile = new File(plugin.getDataFolder(), "reads.yml");
    }

    public synchronized void reload() {
        if (!versionsDir.exists() && !versionsDir.mkdirs()) {
            plugin.getLogger().warning("无法创建 versions 目录");
        }
        versionsById.clear();
        File[] files = versionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            List<File> sorted = new ArrayList<>(List.of(files));
            sorted.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
            for (File file : sorted) {
                try {
                    NewsVersion version = loadVersionFile(file);
                    versionsById.put(version.id(), version);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "读取留档失败: " + file.getName(), ex);
                }
            }
        }
        loadReads();
        reloadInteractiveWords();
    }

    public synchronized List<NewsVersion> listNewestFirst() {
        return List.copyOf(versionsById.values());
    }

    public synchronized NewsVersion latest() {
        return versionsById.isEmpty() ? null : versionsById.values().iterator().next();
    }

    public synchronized NewsVersion get(String id) {
        return versionsById.get(id);
    }

    public boolean hasRead(UUID uuid, String id) {
        Set<String> set = reads.get(uuid);
        return set != null && set.contains(id);
    }

    public void markRead(UUID uuid, String id) {
        if (uuid == null || id == null || id.isBlank()) {
            return;
        }
        reads.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet()).add(id);
        saveReads();
    }

    public Map<String, InteractiveWord> interactiveWords() {
        return Collections.unmodifiableMap(interactiveWords);
    }

    /**
     * @return newly created version, or null if content unchanged
     */
    public synchronized NewsVersion syncFromBookNews() {
        FileConfiguration booknews = loadBookNewsConfig();
        if (booknews == null) {
            return null;
        }
        reloadInteractiveWords(booknews);
        ConfigurationSection book = booknews.getConfigurationSection("book");
        if (book == null) {
            plugin.getLogger().warning("BookNews 配置缺少 book 段");
            return null;
        }
        Map<String, String> pages = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(book.getKeys(false));
        keys.sort((a, b) -> Integer.compare(parsePageKey(a), parsePageKey(b)));
        StringBuilder hashSource = new StringBuilder();
        for (String key : keys) {
            String text = book.getString(key, "");
            pages.put(key, text);
            hashSource.append(key).append('\n').append(text).append('\n');
        }
        String hash = sha256(hashSource.toString());
        NewsVersion latest = latest();
        if (latest != null && hash.equals(latest.contentHash())) {
            return null;
        }

        String published = extractPublished(pages.getOrDefault("1", pages.isEmpty() ? "" : pages.values().iterator().next()));
        String title = extractTitle(pages);
        String summary = extractSummary(pages);
        String id = buildId(published, hash);
        if (versionsById.containsKey(id)) {
            id = id + "-" + hash.substring(0, 8);
        }

        NewsVersion created = new NewsVersion(id, published, title, summary, hash, pages);
        saveVersionFile(created);
        Map<String, NewsVersion> rebuilt = new LinkedHashMap<>();
        rebuilt.put(created.id(), created);
        rebuilt.putAll(versionsById);
        versionsById.clear();
        versionsById.putAll(rebuilt);
        return created;
    }

    private void saveVersionFile(NewsVersion version) {
        File file = new File(versionsDir, version.id() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", version.id());
        yaml.set("published", version.published());
        yaml.set("title", version.title());
        yaml.set("summary", version.summary());
        yaml.set("content-hash", version.contentHash());
        for (Map.Entry<String, String> entry : version.pages().entrySet()) {
            yaml.set("pages." + entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "写入留档失败: " + file.getName(), ex);
        }
    }

    private NewsVersion loadVersionFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("id", file.getName().replace(".yml", ""));
        Map<String, String> pages = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("pages");
        if (section != null) {
            List<String> keys = new ArrayList<>(section.getKeys(false));
            keys.sort((a, b) -> Integer.compare(parsePageKey(a), parsePageKey(b)));
            for (String key : keys) {
                pages.put(key, section.getString(key, ""));
            }
        }
        return new NewsVersion(
                id,
                yaml.getString("published", ""),
                yaml.getString("title", id),
                yaml.getString("summary", ""),
                yaml.getString("content-hash", ""),
                pages);
    }

    private void loadReads() {
        reads.clear();
        if (!readsFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(readsFile);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Set<String> set = ConcurrentHashMap.newKeySet();
                set.addAll(players.getStringList(key));
                reads.put(uuid, set);
            } catch (IllegalArgumentException ignored) {
                // skip bad uuid
            }
        }
    }

    private void saveReads() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> entry : reads.entrySet()) {
            yaml.set("players." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        try {
            yaml.save(readsFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "写入已读状态失败", ex);
        }
    }

    private void reloadInteractiveWords() {
        FileConfiguration booknews = loadBookNewsConfig();
        if (booknews != null) {
            reloadInteractiveWords(booknews);
        }
    }

    private void reloadInteractiveWords(FileConfiguration booknews) {
        interactiveWords.clear();
        ConfigurationSection section = booknews.getConfigurationSection("Interactive-Word");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection wordSec = section.getConfigurationSection(key);
            if (wordSec == null) {
                continue;
            }
            String placeholder = wordSec.getString("placeholder", "");
            if (placeholder.isBlank()) {
                continue;
            }
            ConfigurationSection click = wordSec.getConfigurationSection("clickevent");
            ConfigurationSection hover = wordSec.getConfigurationSection("hoverevent");
            interactiveWords.put(placeholder, new InteractiveWord(
                    wordSec.getString("word", placeholder),
                    wordSec.getString("color", "black"),
                    wordSec.getBoolean("bold", false),
                    wordSec.getBoolean("italic", false),
                    wordSec.getBoolean("underlined", false),
                    wordSec.getBoolean("obfuscated", false),
                    click != null && click.getBoolean("enable", false),
                    click == null ? "" : click.getString("action", ""),
                    click == null ? "" : click.getString("value", ""),
                    hover != null && hover.getBoolean("enable", false),
                    hover == null ? "" : hover.getString("text", "")));
        }
    }

    private FileConfiguration loadBookNewsConfig() {
        String relative = plugin.getConfig().getString("booknews-config", "plugins/BookNews/config.yml");
        File file = plugin.resolveServerFile(relative);
        if (!file.isFile()) {
            plugin.getLogger().warning("找不到 BookNews 配置: " + file.getAbsolutePath());
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private static String extractPublished(String page1) {
        if (page1 == null) {
            return "";
        }
        for (String line : page1.split("\n")) {
            String plain = stripColors(line).trim();
            Matcher matcher = PUBLISH_LINE.matcher(plain);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return "";
    }

    private static String extractTitle(Map<String, String> pages) {
        String page2 = pages.get("2");
        if (page2 != null) {
            for (String line : page2.split("\n")) {
                Matcher matcher = TITLE_LINE.matcher(line.trim());
                if (matcher.find()) {
                    return stripColors(matcher.group(1)).trim();
                }
            }
        }
        String page1 = pages.get("1");
        if (page1 != null) {
            for (String line : page1.split("\n")) {
                Matcher matcher = TITLE_LINE.matcher(line.trim());
                if (matcher.find()) {
                    return stripColors(matcher.group(1)).trim();
                }
            }
        }
        return "服务器告示";
    }

    private static String extractSummary(Map<String, String> pages) {
        String page2 = pages.getOrDefault("2", pages.getOrDefault("1", ""));
        List<String> lines = new ArrayList<>();
        for (String line : page2.split("\n")) {
            String plain = stripColors(line).trim();
            if (plain.isEmpty() || plain.startsWith("─") || plain.contains("booknews_")) {
                continue;
            }
            if (plain.startsWith("本期") || plain.startsWith("流浪世界")) {
                continue;
            }
            lines.add(plain);
            if (lines.size() >= 3) {
                break;
            }
        }
        return String.join(" ", lines);
    }

    private static String buildId(String published, String hash) {
        String normalized = published
                .replace("年", "")
                .replace("月", "")
                .replace("日", "")
                .replace("：", "")
                .replace(":", "")
                .replace(" ", "")
                .replace("-", "");
        if (normalized.matches("\\d{14}")) {
            return normalized.substring(0, 8) + "-" + normalized.substring(8);
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            return now.format(ID_FMT) + "-" + hash.substring(0, 6);
        } catch (Exception ex) {
            return "v-" + hash.substring(0, 12);
        }
    }

    private static int parsePageKey(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    static String stripColors(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("(?i)#[0-9a-f]{6}", "").replaceAll("(?i)&[0-9a-fk-or]", "").trim();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public record InteractiveWord(
            String word,
            String color,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean obfuscated,
            boolean clickEnable,
            String clickAction,
            String clickValue,
            boolean hoverEnable,
            String hoverText) {
    }
}
