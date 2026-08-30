package work.mcwws.immersivecreative.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McwwsImmersiveCreativeClientMod implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("MCWWS_ImmersiveCreative");

    @Override
    public void onInitializeClient() {
        ImmersiveCreativeNetworking.register();
        LOGGER.info("MCWWS Immersive Creative 已加载，等待服务端同步开关。");
    }
}
