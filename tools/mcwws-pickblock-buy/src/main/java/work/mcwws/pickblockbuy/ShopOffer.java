package work.mcwws.pickblockbuy;

import org.bukkit.Material;

public record ShopOffer(Material material, String shopId, String slot, double unitBuyPrice) {
}
