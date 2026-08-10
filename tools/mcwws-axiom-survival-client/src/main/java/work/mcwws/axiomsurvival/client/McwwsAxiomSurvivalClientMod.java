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
        LOGGER.info("MCWWS Axiom Survival Client 已加载，等待服务端 hello…");
    }
}
