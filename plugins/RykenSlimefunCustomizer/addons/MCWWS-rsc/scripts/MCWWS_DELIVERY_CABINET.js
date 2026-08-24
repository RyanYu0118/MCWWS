function onPlace(event) {
    var player = event.getPlayer();
    sendMessage(player, "&a已放置外卖柜。请输入与分配器相同的 &e大编号&a，再输入 &e小编号&a。");
    sendMessage(player, "&7空闲木桶才会接收网页订单；取空前不可再分配。");
}

function onBreak(event, itemStack, drops) {
    var player = event.getPlayer();
    sendMessage(player, "&e外卖柜已拆除，柜内物品将掉落，编号与占用一并注销。");
}

function onUse(event) {
    // 不要 event.cancel()：上锁/开柜由 Skript 接管。
}

function tick(info) {
}
