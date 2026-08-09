package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

final class ChargingPacketHandlers {

    private ChargingPacketHandlers() {
    }

    static PacketHandler wrap(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService chargeService,
            PacketFeeEstimator estimator,
            PacketHandler delegate,
            String channel
    ) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("handleAsync".equals(method.getName())) {
                return delegate.handleAsync();
            }
            if ("onReceive".equals(method.getName()) && args != null && args.length == 2) {
                onReceive(plugin, chargeService, estimator, delegate, channel,
                        (Player) args[0], args[1]);
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
            PacketFeeEstimator estimator,
            PacketHandler delegate,
            String channel,
            Player player,
            Object friendlyByteBuf
    ) {
        if (!chargeService.shouldCharge(player)) {
            invokeDelegate(delegate, player, friendlyByteBuf);
            return;
        }
        try {
            if (delegate.handleAsync()) {
                if (!evaluateOnMainThread(plugin, chargeService, estimator, player, friendlyByteBuf, channel)) {
                    return;
                }
            } else if (!evaluateInline(chargeService, estimator, player, friendlyByteBuf, channel)) {
                return;
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.WARNING, "Axiom 扣费预估失败 (" + channel + "): " + ex.getMessage(), ex);
            return;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        invokeDelegate(delegate, player, friendlyByteBuf);
    }

    private static void invokeDelegate(PacketHandler delegate, Player player, Object buf) {
        try {
            Class<?> bufClass = Class.forName("net.minecraft.network.FriendlyByteBuf");
            Method onReceive = PacketHandler.class.getMethod("onReceive", Player.class, bufClass);
            onReceive.invoke(delegate, player, buf);
        } catch (ReflectiveOperationException ex) {
            McwwsAxiomSurvivalPlugin.getInstance().getLogger().log(Level.SEVERE, "Axiom 包转发失败", ex);
        }
    }

    private static boolean evaluateInline(
            ChargeService chargeService,
            PacketFeeEstimator estimator,
            Player player,
            Object buf,
            String channel
    ) throws ReflectiveOperationException {
        int mark = PacketBufs.readerIndex(buf);
        FeeAccumulator.Result estimate = estimate(estimator, player, buf, channel);
        PacketBufs.readerIndex(buf, mark);
        return chargeService.evaluate(player, channel, estimate).allowed();
    }

    private static boolean evaluateOnMainThread(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService chargeService,
            PacketFeeEstimator estimator,
            Player player,
            Object buf,
            String channel
    ) throws ReflectiveOperationException, InterruptedException {
        int mark = PacketBufs.readerIndex(buf);
        byte[] snapshot = PacketBufs.copyReadable(buf);
        PacketBufs.readerIndex(buf, mark);

        AtomicReference<ChargeService.ChargeDecision> decision =
                new AtomicReference<>(ChargeService.ChargeDecision.deny("timeout"));
        CountDownLatch latch = new CountDownLatch(1);
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Object clone = PacketBufs.fromBytes(player, snapshot);
                FeeAccumulator.Result estimate = estimate(estimator, player, clone, channel);
                decision.set(chargeService.evaluate(player, channel, estimate));
            } catch (ReflectiveOperationException ex) {
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

    private static FeeAccumulator.Result estimate(
            PacketFeeEstimator estimator,
            Player player,
            Object buf,
            String channel
    ) throws ReflectiveOperationException {
        return switch (channel) {
            case "set_block" -> estimator.estimateSetBlockPacket(player, buf);
            case "set_buffer" -> estimator.estimateSetBufferPacket(player, buf);
            default -> FeeAccumulator.Result.empty();
        };
    }
}
