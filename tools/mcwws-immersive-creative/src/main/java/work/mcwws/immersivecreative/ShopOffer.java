package work.mcwws.immersivecreative;

import org.bukkit.Material;

public record ShopOffer(Material material, String shopId, String slot, double unitBuyPrice, double unitSellPrice) {
}
