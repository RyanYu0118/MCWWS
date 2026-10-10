package work.mcwws.axiomsurvival.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McwwsAxiomSurvivalClientMod implements ClientModInitializer {

    public static final String MOD_ID = "mcwws_axiom_survival_client";
    public static final Logger LOGGER = LoggerFactory.getLogger("MCWWS_AxiomSurvivalClient");

    @Override
    public void onInitializeClient() {
        SurvivalEditorNetworking.register();
        BalanceHudNetworking.register();
        // 拖放由 MouseHandlerDropMixin 拦截（26.3 SDL）；无需再挂原生回调
        LOGGER.info("MCWWS Axiom Survival Client 1.5.1 已加载（含 Vitrail 预览重挂），等待服务端 hello…");
    }
}
