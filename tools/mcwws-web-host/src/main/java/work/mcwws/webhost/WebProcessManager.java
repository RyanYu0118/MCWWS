package work.mcwws.webhost;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

final class WebProcessManager {

    private final McwwsWebHostPlugin plugin;
    private Process process;
    private Thread stdoutThread;
    private Thread stderrThread;
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    WebProcessManager(McwwsWebHostPlugin plugin) {
        this.plugin = plugin;
    }

    boolean isRunning() {
        return process != null && process.isAlive();
    }

    Long pid() {
        if (!isRunning()) {
            return null;
        }
        return process.pid();
    }

    private volatile String lastStartError = "";
    private volatile Integer lastExitCode = null;

    String getLastStartError() {
        return lastStartError == null ? "" : lastStartError;
    }

    Integer getLastExitCode() {
        return lastExitCode;
    }

    synchronized boolean start() {
        stopInternal(false);
        stopping.set(false);
        lastStartError = "";
        lastExitCode = null;

        if (!plugin.getPluginConfig().getBoolean("enabled", true)) {
            lastStartError = "配置 enabled: false";
            plugin.getLogger().info("MCWWS 网页服务已在配置中禁用。");
            return false;
        }

        File webDir = plugin.resolveWebDirectory();
        File entry = new File(webDir, plugin.getPluginConfig().getString("entry-script", "server.js"));
        if (!webDir.isDirectory()) {
            lastStartError = "网页目录不存在: " + webDir.getAbsolutePath();
            plugin.getLogger().warning(lastStartError);
            return false;
        }
        if (!entry.isFile()) {
            lastStartError = "入口脚本不存在: " + entry.getAbsolutePath();
            plugin.getLogger().warning(lastStartError);
            return false;
        }

        String nodeBinary = resolveNodeBinary();
        if (nodeBinary == null || nodeBinary.isBlank()) {
            lastStartError = "未找到 Node，请在 config.yml 设置 node-binary 为绝对路径";
            plugin.getLogger().severe(lastStartError);
            return false;
        }
        List<String> command = new ArrayList<>();
        command.add(nodeBinary);
        if (plugin.getPluginConfig().getBoolean("watch-mode", false)) {
            command.add("--watch");
        }
        plugin.getPluginConfig().getStringList("extra-node-args").stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(command::add);
        command.add(entry.getName());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(webDir);
        builder.redirectErrorStream(false);

        int port = plugin.getPluginConfig().getInt("port", 8002);
        String host = plugin.getPluginConfig().getString("host", "0.0.0.0");
        File serverRoot = plugin.getServerRoot();
        if (serverRoot == null) {
            lastStartError = "无法解析服务器根目录";
            plugin.getLogger().severe(lastStartError);
            return false;
        }

        builder.environment().put("PORT", String.valueOf(port));
        builder.environment().put("MCWWS_WEB_PORT", String.valueOf(port));
        builder.environment().put("HOST", host);
        builder.environment().put("MCWWS_SERVER_ROOT", serverRoot.getAbsolutePath());
        builder.environment().put("NODE_ENV", "production");

        try {
            process = builder.start();
            attachLogPump(process);
            watchProcessExit(process);
            plugin.getLogger().info("已启动 MCWWS 网页 Node 进程 (pid=" + process.pid()
                    + ") → http://" + host + ":" + port + "  目录: " + webDir.getAbsolutePath());
            if (plugin.getPluginConfig().getBoolean("watch-mode", false)) {
                plugin.getLogger().info("watch-mode 已开启：修改 web 目录下 .js 将自动重启 Node。");
            }
            return true;
        } catch (Exception ex) {
            lastStartError = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            plugin.getLogger().log(Level.SEVERE, "启动 MCWWS 网页 Node 失败，请确认已安装 Node 且已在 web 目录执行 npm install", ex);
            process = null;
            return false;
        }
    }

    private String resolveNodeBinary() {
        String configured = plugin.getPluginConfig().getString("node-binary", "node");
        if (configured != null && !configured.isBlank()) {
            File asFile = new File(configured);
            if (asFile.isFile()) {
                return asFile.getAbsolutePath();
            }
            if (!configured.contains(File.separator) && !configured.contains("/")) {
                return configured.trim();
            }
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            for (String candidate : new String[]{
                    "D:\\Program Files\\nodejs\\node.exe",
                    System.getenv("ProgramFiles") + "\\nodejs\\node.exe",
                    System.getenv("ProgramFiles(x86)") + "\\nodejs\\node.exe"
            }) {
                if (candidate != null && new File(candidate).isFile()) {
                    return candidate;
                }
            }
        }
        return configured != null ? configured.trim() : "node";
    }

    private void watchProcessExit(Process proc) {
        Thread watcher = new Thread(() -> {
            try {
                int code = proc.waitFor();
                lastExitCode = code;
                if (!stopping.get()) {
                    lastStartError = "Node 进程已退出，exitCode=" + code + "（若端口占用或缺少 npm 依赖请查看上方 [MCWWS-Web] 日志）";
                    plugin.getLogger().warning("[MCWWS-Web] " + lastStartError);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }, "MCWWS-Web-exit-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    synchronized void stop() {
        stopInternal(true);
    }

    synchronized boolean restart() {
        plugin.getLogger().info("正在重启 MCWWS 网页 Node 进程…");
        return start();
    }

    private synchronized void stopInternal(boolean markStopping) {
        if (markStopping) {
            stopping.set(true);
        }
        Process current = process;
        process = null;
        if (current == null) {
            return;
        }
        if (current.isAlive()) {
            current.destroy();
            long grace = Math.max(plugin.getPluginConfig().getLong("shutdown-grace-ms", 8000L), 1000L);
            try {
                if (!current.waitFor(grace, TimeUnit.MILLISECONDS)) {
                    current.destroyForcibly();
                    current.waitFor(3, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                current.destroyForcibly();
            }
        }
        joinThread(stdoutThread);
        joinThread(stderrThread);
        stdoutThread = null;
        stderrThread = null;
        if (markStopping) {
            plugin.getLogger().info("MCWWS 网页 Node 进程已停止。");
        }
    }

    private void joinThread(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(1500L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void attachLogPump(Process proc) {
        if (!plugin.getPluginConfig().getBoolean("log-to-console", true)) {
            return;
        }
        stdoutThread = new Thread(() -> pumpLines(proc, false), "MCWWS-Web-stdout");
        stderrThread = new Thread(() -> pumpLines(proc, true), "MCWWS-Web-stderr");
        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();
    }

    private void pumpLines(Process proc, boolean err) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                err ? proc.getErrorStream() : proc.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (stopping.get()) {
                    continue;
                }
                if (err) {
                    plugin.getLogger().warning("[MCWWS-Web] " + line);
                } else {
                    plugin.getLogger().info("[MCWWS-Web] " + line);
                }
            }
        } catch (Exception ex) {
            if (!stopping.get()) {
                plugin.getLogger().log(Level.FINE, "Node 日志泵结束", ex);
            }
        }
    }
}
