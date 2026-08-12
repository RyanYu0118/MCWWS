package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * 把 AxiomPaper 的包处理器包一层：先在主线程解析并决定是否收费/放行，
 * 只有放行时才把原包交给 Axiom 真正执行。
 */
final class ChargingPacketHandlers {

    /** 主线程内的放行判定；{@code clonedBuf} 是可随意读取的副本 */
    interface PacketGate {
        ChargeService.ChargeDecision decide(Player player, Object clonedBuf) throws ReflectiveOperationException;
    }

    private ChargingPacketHandlers() {
    }

    static PacketHandler wrap(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService chargeService,
            PacketHandler delegate,
            String channel,
            PacketGate gate
    ) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("handleAsync".equals(method.getName())) {
                return delegate.handleAsync();
            }
            if ("onReceive".equals(method.getName()) && args != null && args.length == 2) {
                onReceive(plugin, chargeService, delegate, channel, gate, (Player) args[0], args[1]);
                return null;
            }
            return method.invoke(delegate, args);
        };
        return (PacketHandler) Proxy.newProxyInstance(
                PacketHandler.class.getClassLoader(),
                new Class<?>[]{PacketHandler.class},
                handler
        );
    }

    private static void onReceive(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService chargeService,
            PacketHandler delegate,
            String channel,
            PacketGate gate,
            Player player,
            Object friendlyByteBuf
    ) {
        if (!chargeService.shouldCharge(player)) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("[debug] " + channel + " 免费放行: player=" + player.getName()
                        + ", 模式=" + player.getGameMode()
                        + ", bypass=" + BlockProtection.shouldBypass(player)
                        + ", use权限=" + player.hasPermission("mcwws.axiom.survival.use")
                        + ", Axiom会话=" + AxiomPaperHook.isAxiomSessionActive(player));
            }
            invokeDelegate(delegate, player, friendlyByteBuf);
            return;
        }
        if (plugin.isDebug()) {
            plugin.getLogger().info("[debug] " + channel + " 进入扣费判定: player=" + player.getName()
                    + ", 主线程=" + Bukkit.isPrimaryThread());
        }
        try {
            if (!decide(plugin, gate, player, friendlyByteBuf, channel)) {
                return;
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.WARNING, "Axiom 扣费预估失败 (" + channel + "): " + ex.getMessage(), ex);
            return;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (Throwable ex) {
            // Axiom 在处理器抛异常时会直接踢人，这里兜住任何意外，宁可放过一次编辑
            plugin.getLogger().log(Level.WARNING, "Axiom 扣费判定异常 (" + channel + ")，本次放行", ex);
        }
        invokeDelegate(delegate, player, friendlyByteBuf);
    }

    private static void invokeDelegate(PacketHandler delegate, Player player, Object buf) {
        PacketDelegate.invoke(delegate, player, buf);
    }

    /**
     * 经济判定必须在主线程做。小载荷通道由 Bukkit 在主线程投递，此时直接判定；
     * 大载荷通道跑在 Netty 线程，才需要调度到主线程并等结果。
     */
    private static boolean decide(
            McwwsAxiomSurvivalPlugin plugin,
            PacketGate gate,
            Player player,
            Object buf,
            String channel
    ) throws ReflectiveOperationException, InterruptedException {
        int mark = PacketBufs.readerIndex(buf);
        byte[] snapshot = PacketBufs.copyReadable(buf);
        PacketBufs.readerIndex(buf, mark);

        if (Bukkit.isPrimaryThread()) {
            boolean allowed = gate.decide(player, PacketBufs.fromBytes(player, snapshot)).allowed();
            PacketBufs.readerIndex(buf, mark);
            return allowed;
        }

        AtomicReference<ChargeService.ChargeDecision> decision =
                new AtomicReference<>(ChargeService.ChargeDecision.deny("timeout"));
        CountDownLatch latch = new CountDownLatch(1);
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Object clone = PacketBufs.fromBytes(player, snapshot);
                decision.set(gate.decide(player, clone));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "Axiom 异步扣费预估失败: " + ex.getMessage(), ex);
                decision.set(ChargeService.ChargeDecision.deny("estimate-failed"));
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5L, TimeUnit.SECONDS)) {
            plugin.getLogger().warning("Axiom 扣费主线程等待超时: " + channel);
            return false;
        }
        return decision.get().allowed();
    }
}
