package work.mcwws.axiomsurvival.client;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Axiom 只把 Iris 的 {@code isShaderPackInUse} 当成“正在用光影”。
 * 这边实际加载的是 Vitrail，包开着时也要走同一条兼容路径。
 */
public final class ShaderPacks {

    private static final Method vitrailNoPack;
    private static final Method vitrailSession;

    static {
        Method noPack = null;
        Method session = null;
        try {
            Class<?> choice = Class.forName("dev.vitrail.render.PackChoice");
            noPack = choice.getMethod("noPackWanted");
            session = choice.getMethod("session");
        } catch (Throwable ignored) {
            noPack = null;
            session = null;
        }
        vitrailNoPack = noPack;
        vitrailSession = session;
    }

    private ShaderPacks() {
    }

    public static boolean vitrailPackEnabled() {
        if (vitrailNoPack == null || vitrailSession == null) {
            return false;
        }
        try {
            if (Boolean.TRUE.equals(vitrailNoPack.invoke(null))) {
                return false;
            }
            Object session = vitrailSession.invoke(null);
            return session instanceof Optional<?> optional && optional.isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
