function onPlace(event) {
    var player = event.getPlayer();
    sendMessage(player, "&a已放置外卖柜。请在聊天输入 &e大编号&a（收货地址），再输入 &e小编号&a。");
    sendMessage(player, "&7输入 &fcancel&7 或 &f取消 &7可跳过编号（未编号的柜不会接收网页订单）。");
}

function onBreak(event, itemStack, drops) {
    var player = event.getPlayer();
    sendMessage(player, "&e外卖柜已拆除，柜内物品将掉落，编号一并注销。");
}

function tick(info) {
}
