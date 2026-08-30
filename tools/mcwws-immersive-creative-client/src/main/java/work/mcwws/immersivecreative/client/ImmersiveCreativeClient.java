package work.mcwws.immersivecreative.client;

public final class ImmersiveCreativeClient {

    /** 光标物品来自分类页，点 ❌ 时没有「原槽位」可退，物品留在光标上。 */
    public static final int SOURCE_CATALOG = -3;
    public static final int SOURCE_NONE = Integer.MIN_VALUE;

    private static volatile boolean enabled;
    private static volatile int lastSourceSlot = SOURCE_NONE;

    private ImmersiveCreativeClient() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            lastSourceSlot = SOURCE_NONE;
        }
    }

    public static int lastSourceSlot() {
        return lastSourceSlot;
    }

    public static void setLastSourceSlot(int slot) {
        lastSourceSlot = slot;
    }

    public static void setLastSourceCatalog() {
        lastSourceSlot = SOURCE_CATALOG;
    }

    public static void clearLastSourceSlot() {
        lastSourceSlot = SOURCE_NONE;
    }
}
