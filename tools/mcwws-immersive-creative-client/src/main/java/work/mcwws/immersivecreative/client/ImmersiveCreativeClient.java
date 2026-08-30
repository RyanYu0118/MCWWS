package work.mcwws.immersivecreative.client;

public final class ImmersiveCreativeClient {

    private static volatile boolean enabled;

    private ImmersiveCreativeClient() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
