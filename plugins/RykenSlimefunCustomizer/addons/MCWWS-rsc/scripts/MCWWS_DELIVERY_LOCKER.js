function onPlace(event) {
    var player = event.getPlayer();
    sendMessage(player, "&a已放置外卖柜。请在聊天输入 &e大编号&a（收货地址），再输入 &e小编号&a。");
    sendMessage(player, "&7输入 &fcancel&7 或 &f取消 &7可跳过编号（未编号的柜不会接收网页订单）。");
}

function onBreak(event, itemStack, drops) {
    var player = event.getPlayer();
    sendMessage(player, "&e外卖柜已拆除，柜内物品将掉落，编号一并注销。");
}

function onUse(event) {
    // 不要 event.cancel()：会连带取消 Bukkit 右键，Skript 无法打开背包式 GUI。
    // 让 Slimefun 打开隐藏后端菜单（标题 §6外卖柜），由 delivery_locker.sk 在 inventory open 时接管。
}

function tick(info) {
}
