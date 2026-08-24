function onPlace(event) {
    var player = event.getPlayer();
    sendMessage(player, "&a已放置外卖分配器。请在聊天输入 &e大编号&a（网页收货地址）。");
    sendMessage(player, "&7输入 &fcancel&7 或 &f取消 &7可跳过（未编号则网页无法选此地址）。");
}

function onBreak(event, itemStack, drops) {
    var player = event.getPlayer();
    sendMessage(player, "&e外卖分配器已拆除，大编号一并注销。同址外卖柜需重新对码。");
}

function onUse(event) {
    // 不要 event.cancel()：会连带取消 Bukkit 右键，Skript 无法打开分配器界面。
}

function tick(info) {
}
