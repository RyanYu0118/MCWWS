package work.mcwws.economyledger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class LedgerQueueWriter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Path queuePath;
    private final Object lock = new Object();

    LedgerQueueWriter(Path queuePath) {
        this.queuePath = queuePath;
    }

    void append(
            String uuid,
            String playerId,
            String direction,
            String category,
            double amount,
            Double balanceAfter,
            String description,
            String refId
    ) {
        synchronized (lock) {
            try {
                if (queuePath.getParent() != null) {
                    Files.createDirectories(queuePath.getParent());
                }
                String line = String.join("|",
                        sanitize(uuid),
                        sanitize(playerId),
                        sanitize(direction),
                        sanitize(category),
                        formatMoney(amount),
                        balanceAfter == null ? "" : formatMoney(balanceAfter),
                        sanitize(description),
                        sanitize(refId),
                        LocalDateTime.now().format(TIME)
                ) + System.lineSeparator();
                Files.writeString(
                        queuePath,
                        line,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException ex) {
                throw new IllegalStateException("写入 ledger 队列失败: " + queuePath, ex);
            }
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }

    static String formatMoney(double amount) {
        double rounded = Math.round(amount * 100.0) / 100.0;
        long cents = Math.round(rounded * 100.0);
        long whole = cents / 100;
        long fraction = Math.abs(cents % 100);
        if (fraction == 0) {
            return Long.toString(whole);
        }
        if (fraction < 10) {
            return whole + ".0" + fraction;
        }
        return whole + "." + fraction;
    }

    static String directionForDelta(double delta) {
        return delta >= 0D ? "credit" : "debit";
    }

    static double absAmount(double delta) {
        return Math.abs(Math.round(delta * 100.0) / 100.0);
    }

    static String defaultRefId(String prefix, String uuid, double amount, String cause) {
        long bucket = System.currentTimeMillis() / 1000L;
        return String.format(Locale.ROOT, "%s-%s-%.2f-%s-%d", prefix, uuid, amount, cause, bucket);
    }
}
