package work.mcwws.axiomsurvival.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McwwsAxiomSurvivalClientMod implements ClientModInitializer {

    public static final String MOD_ID = "mcwws_axiom_survival_client";
    public static final Logger LOGGER = LoggerFactory.getLogger("MCWWS_AxiomSurvivalClient");

    @Override
    public void onInitializeClient() {
        SurvivalEditorNetworking.register();
        BalanceHudNetworking.register();
        // 窗口在 CLIENT_STARTED 时才一定存在，GLFW 回调要挂在真实 handle 上
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> SchematicDropHandler.install());
        LOGGER.info("MCWWS Axiom Survival Client 已加载，等待服务端 hello…");
    }
}
