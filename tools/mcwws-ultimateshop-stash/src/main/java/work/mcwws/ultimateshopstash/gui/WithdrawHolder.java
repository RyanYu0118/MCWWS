package work.mcwws.ultimateshopstash.gui;

import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class WithdrawHolder implements InventoryHolder {

    private final WithdrawSession session;
    private Inventory inventory;

    public WithdrawHolder(WithdrawSession session) {
        this.session = session;
    }

    public WithdrawSession session() {
        return session;
    }

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
