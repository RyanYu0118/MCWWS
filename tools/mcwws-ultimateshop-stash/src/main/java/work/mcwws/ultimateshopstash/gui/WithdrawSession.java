package work.mcwws.ultimateshopstash.gui;

import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;

public final class WithdrawSession {

    private final String shopName;
    private final String productId;
    private final String itemKey;
    private final ObjectItem objectItem;
    private int selectedAmount = 1;

    public WithdrawSession(String shopName, String productId, String itemKey, ObjectItem objectItem) {
        this.shopName = shopName;
        this.productId = productId;
        this.itemKey = itemKey;
        this.objectItem = objectItem;
    }

    public String shopName() {
        return shopName;
    }

    public String productId() {
        return productId;
    }

    public String itemKey() {
        return itemKey;
    }

    public ObjectItem objectItem() {
        return objectItem;
    }

    public int selectedAmount() {
        return selectedAmount;
    }

    public void selectedAmount(int selectedAmount) {
        this.selectedAmount = Math.max(1, selectedAmount);
    }
}
