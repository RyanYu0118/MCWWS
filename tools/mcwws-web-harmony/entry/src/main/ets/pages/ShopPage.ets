import { CART, dateText, errorMessage, ListSource, money, request, SESSION } from '../services/Api';
import { BOTTOM_INSET, LoadState, ScreenHeader, SERVICE_CARD_COLOR, SERVICE_SHEET_COLOR } from '../components/NativeUi';

interface Offer {
  shopId: string;
  shopTitleResolved: string;
  shopTitle: string;
  slot: string;
  buyAmountResolved: number | null;
}
interface PriceRow {
  displayName?: string;
  customDisplayName?: string;
  buy?: number;
  sell?: number;
  inShop?: boolean;
  ultimateShopOffers?: Offer[];
  shopCategories?: string[];
}
interface Category {
  id: string;
  name: string;
}
interface CatalogCategories {
  categories: Category[];
}
interface Product {
  id: string;
  title: string;
  price: PriceRow;
}
interface BasketLine {
  itemId: string;
  shopId: string;
  slot: string;
  quantity: number;
  title: string;
  unitPrice: number;
}
interface CheckoutPayload {
  deliveryMajor: string;
  lines: BasketLine[];
}
interface Order {
  orderId: number;
  createdAt: string;
  status: string;
  pickupCode?: string;
  deliveryMajor?: string;
  failureReason?: string;
  totalFormatted: string;
  message?: string;
}
interface OrderList {
  orders: Order[];
}
interface AddressList {
  majors: string[];
}
interface ShopStats {
  total: number;
  totalMoney: number;
  buys: number;
  sells: number;
}
interface EconomyStats {
  uniqueItems: number;
  uniquePlayers: number;
  avgTransaction: number;
  netFlow: number;
}
interface PricePoint {
  timestamp: string;
  avgBuyPrice: number;
  avgSellPrice: number;
}
interface PriceHistory {
  priceHistory: PricePoint[];
}

const GAME_ASSETS: string = '/26.1.2/assets/minecraft/';

@Component
struct PriceChart {
  @Prop @Watch('draw') points: PricePoint[] = [];
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private canvas: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);

  draw(): void {
    const width = this.canvas.width;
    const height = this.canvas.height;
    if (width <= 0 || height <= 0 || this.points.length < 2) {
      return;
    }
    const lowest = Math.min(...this.points.map(point => Math.min(point.avgBuyPrice, point.avgSellPrice)));
    const highest = Math.max(...this.points.map(point => Math.max(point.avgBuyPrice, point.avgSellPrice)));
    const range = Math.max(0.01, highest - lowest);
    this.canvas.clearRect(0, 0, width, height);
    this.canvas.strokeStyle = '#33768596';
    this.canvas.lineWidth = 1;
    for (let row = 0; row < 3; row++) {
      const y = 8 + row * (height - 16) / 2;
      this.canvas.beginPath();
      this.canvas.moveTo(4, y);
      this.canvas.lineTo(width - 4, y);
      this.canvas.stroke();
    }
    this.plot(width, height, lowest, range, '#0A59F7', false);
    this.plot(width, height, lowest, range, '#768596', true);
  }

  private plot(width: number, height: number, lowest: number, range: number, color: string,
    sell: boolean): void {
    this.canvas.strokeStyle = color;
    this.canvas.lineWidth = 2;
    this.canvas.beginPath();
    this.points.forEach((point, index) => {
      const x = 4 + index * (width - 8) / (this.points.length - 1);
      const value = sell ? point.avgSellPrice : point.avgBuyPrice;
      const y = height - 8 - (value - lowest) / range * (height - 16);
      if (index === 0) {
        this.canvas.moveTo(x, y);
      } else {
        this.canvas.lineTo(x, y);
      }
    });
    this.canvas.stroke();
  }

  build() {
    Canvas(this.canvas)
      .width('100%').height(112)
      .accessibilityText('历史价格，蓝线为买入，灰线为卖出')
      .onReady(() => this.draw())
  }
}

@Component
export struct ShopPage {
  @Prop @Watch('mapSearchChanged') mapSearch: string = '';
  @StorageLink(SESSION) @Watch('sessionChanged') token: string = '';
  @StorageLink(CART) savedCart: string = '[]';
  @State query: string = '';
  @State loading: boolean = false;
  @State error: string = '';
  @State categoryIndex: number = 0;
  @State resultCount: number = 0;
  @State categories: Category[] = [{ id: '', name: '全部物品' }, { id: 'in_shop', name: '已上架' }];
  @State activeProduct: Product | null = null;
  @State activeOffers: Offer[] = [];
  @State offerIndex: number = 0;
  @State sheetOpen: boolean = false;
  @State sheet: string = 'detail';
  @State orders: Order[] = [];
  @State basket: BasketLine[] = [];
  @State orderError: string = '';
  @State checkoutMessage: string = '';
  @State addresses: string[] = [];
  @State address: string = '';
  @State qty: number = 1;
  @State sending: boolean = false;
  @State loadingOrders: boolean = false;
  @State shopStats: ShopStats | null = null;
  @State economyStats: EconomyStats | null = null;
  @State statsError: string = '';
  @State statsLoading: boolean = false;
  @State history: PricePoint[] = [];
  @State chartPoints: PricePoint[] = [];
  @State historyLoading: boolean = false;
  @State historyError: string = '';
  onAccount: () => void = () => {};
  private catalog: Product[] = [];
  private filtered: ListSource<Product> = new ListSource<Product>();
  private filterTimer: number = -1;
  private ordersGeneration: number = 0;
  private detailGeneration: number = 0;

  aboutToAppear(): void {
    this.mapSearchChanged();
    try {
      const restored = JSON.parse(this.savedCart) as BasketLine[];
      if (Array.isArray(restored)) {
        this.basket = restored;
      }
    } catch {
      this.savedCart = '[]';
    }
    void this.reload();
  }

  mapSearchChanged(): void {
    if (!this.mapSearch) return;
    this.query = this.mapSearch;
    this.categoryIndex = 0;
    this.filter();
  }

  aboutToDisappear(): void {
    clearTimeout(this.filterTimer);
  }

  sessionChanged(): void {
    this.ordersGeneration++;
    this.orders = [];
    this.orderError = '';
    this.checkoutMessage = '';
    this.loadingOrders = false;
  }

  private async reload(): Promise<void> {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      const results = await Promise.all([
        request<Record<string, PriceRow>>('/api/prices'),
        request<Record<string, string>>(GAME_ASSETS + 'lang/zh_cn.json').catch((): Record<string, string> => ({}))
      ]);
      const prices = results[0];
      const lang = results[1];
      this.catalog = Object.keys(prices).map((id): Product => {
        const price = prices[id];
        const title = price.displayName || price.customDisplayName || lang['item.minecraft.' + id] ||
          lang['block.minecraft.' + id] || id;
        return { id, title, price };
      }).sort((a, b) => a.title.localeCompare(b.title, 'zh-CN'));
      this.filter();
    } catch (err) {
      this.error = errorMessage(err as Error);
    } finally {
      this.loading = false;
    }
    try {
      const source = await request<CatalogCategories>('/api/shop/categories');
      this.categories = [{ id: '', name: '全部物品' }, { id: 'in_shop', name: '已上架' }, ...source.categories];
      if (this.categoryIndex >= this.categories.length) {
        this.categoryIndex = 0;
      }
      this.filter();
    } catch {
      // The catalog and the built-in filters remain available.
    }
  }

  private filter(): void {
    const query = this.query.trim().toLowerCase();
    const category = this.categories[this.categoryIndex]?.id || '';
    const items = this.catalog.filter(product => {
      const filtersMatch = !category || (category === 'in_shop' ? product.price.inShop === true :
        (product.price.shopCategories || []).some(value => value === category || value.startsWith(category + '__')));
      return filtersMatch && (!query || product.id.includes(query) || product.title.toLowerCase().includes(query));
    });
    this.filtered.set(items);
    this.resultCount = items.length;
  }

  private async showProduct(product: Product): Promise<void> {
    const generation = ++this.detailGeneration;
    this.activeProduct = product;
    this.activeOffers = (product.price.ultimateShopOffers || []).filter(offer =>
      typeof offer.buyAmountResolved === 'number' && Number.isFinite(offer.buyAmountResolved) &&
      offer.buyAmountResolved >= 0 && !!offer.slot && !!offer.shopId);
    this.offerIndex = 0;
    this.qty = 1;
    this.history = [];
    this.chartPoints = [];
    this.historyLoading = true;
    this.historyError = '';
    this.sheet = 'detail';
    this.sheetOpen = true;
    try {
      const data = await request<PriceHistory>('/api/shop/item-snapshot/' +
        encodeURIComponent(product.id) + '?range=7d');
      if (generation === this.detailGeneration) {
        const recent = (data.priceHistory || []).filter(point =>
          Number.isFinite(point.avgBuyPrice) && Number.isFinite(point.avgSellPrice)).slice(-48);
        this.chartPoints = recent;
        this.history = recent.slice(-8).reverse();
      }
    } catch (err) {
      if (generation === this.detailGeneration) {
        this.historyError = errorMessage(err as Error);
      }
    } finally {
      if (generation === this.detailGeneration) {
        this.historyLoading = false;
      }
    }
  }

  private async showStats(): Promise<void> {
    this.sheet = 'dashboard';
    this.sheetOpen = true;
    if (this.statsLoading) {
      return;
    }
    this.statsLoading = true;
    this.statsError = '';
    await Promise.all([
      request<ShopStats>('/api/stats').then(data => this.shopStats = data)
        .catch((error: Error) => this.statsError = errorMessage(error)),
      request<EconomyStats>('/api/analytics/economy').then(data => this.economyStats = data)
        .catch((error: Error) => this.statsError = errorMessage(error))
    ]);
    this.statsLoading = false;
  }

  private addToBasket(): void {
    const product = this.activeProduct;
    const offer = this.activeOffers[this.offerIndex];
    if (!product || !offer || offer.buyAmountResolved === null) {
      return;
    }
    const line: BasketLine = {
      itemId: product.id, shopId: offer.shopId, slot: offer.slot, quantity: this.qty,
      title: product.title, unitPrice: offer.buyAmountResolved
    };
    const existing = this.basket.findIndex(entry => this.lineKey(entry) === this.lineKey(line));
    if (existing >= 0) {
      line.quantity = Math.min(10000, this.basket[existing].quantity + line.quantity);
      this.updateBasket(this.basket.map((entry, index) => index === existing ? line : entry));
    } else if (this.basket.length < 50) {
      this.updateBasket([...this.basket, line]);
    } else {
      this.getUIContext().getPromptAction().showToast({ message: '购物车最多 50 种商品' });
      return;
    }
    this.sheetOpen = false;
    this.getUIContext().getPromptAction().showToast({ message: '已加入购物车' });
  }

  private lineKey(line: BasketLine): string {
    return line.itemId + '/' + line.shopId + '/' + line.slot;
  }

  private updateBasket(lines: BasketLine[]): void {
    this.basket = lines;
    this.savedCart = JSON.stringify(lines);
  }

  private async showBasket(): Promise<void> {
    this.sheet = 'basket';
    this.sheetOpen = true;
    this.checkoutMessage = '';
    try {
      const result = await request<AddressList>('/api/shop/delivery-addresses');
      this.addresses = result.majors;
    } catch {
      this.addresses = [];
    }
  }

  private async fetchOrders(): Promise<void> {
    this.sheet = 'orders';
    this.sheetOpen = true;
    const token = this.token;
    if (!token || this.loadingOrders) {
      return;
    }
    const generation = ++this.ordersGeneration;
    this.loadingOrders = true;
    this.orderError = '';
    try {
      const result = await request<OrderList>('/api/shop/orders?limit=20', token);
      if (generation === this.ordersGeneration) this.orders = result.orders;
    } catch (err) {
      if (generation === this.ordersGeneration) this.orderError = errorMessage(err as Error);
    } finally {
      if (generation === this.ordersGeneration) this.loadingOrders = false;
    }
  }

  private async checkout(): Promise<void> {
    const token = this.token;
    if (this.sending || !token || !this.address.trim() || !this.basket.length) {
      return;
    }
    this.sending = true;
    this.checkoutMessage = '';
    const payload: CheckoutPayload = { deliveryMajor: this.address.trim(), lines: this.basket };
    try {
      const order = await request<Order>('/api/shop/checkout', token, payload);
      this.updateBasket([]);
      this.checkoutMessage = order.message || '订单已提交，取件码 ' + order.pickupCode;
    } catch (err) {
      this.checkoutMessage = errorMessage(err as Error);
    } finally {
      this.sending = false;
    }
  }

  private summary(): string {
    return money(this.basket.reduce((sum, line) => sum + line.quantity * line.unitPrice, 0));
  }

  @Builder
  detail() {
    Scroll() {
      Column({ space: 14 }) {
        ScreenHeader({ title: this.activeProduct?.title || '物品详情', caption: this.activeProduct?.id || '' })
        Text('买入 ' + money(this.activeProduct?.price.buy) + '   ·   卖出 ' +
          money(this.activeProduct?.price.sell)).width('100%')
        if (this.activeOffers.length > 0) {
          Select(this.activeOffers.map((offer): SelectOption =>
            ({ value: offer.shopTitleResolved || offer.shopTitle || offer.shopId })))
            .selected(this.offerIndex)
            .value(this.activeOffers[this.offerIndex]?.shopTitleResolved ||
              this.activeOffers[this.offerIndex]?.shopTitle || this.activeOffers[this.offerIndex]?.shopId || '选择商店')
            .onSelect(index => this.offerIndex = index).width('100%')
          Text('上架单价 ' + money(this.activeOffers[this.offerIndex]?.buyAmountResolved)).width('100%')
          Row({ space: 12 }) {
            Text('数量').layoutWeight(1)
            Counter() {
              TextInput({ text: this.qty.toString() })
                .width(70).height(34).type(InputType.Number).textAlign(TextAlign.Center)
                .maxLength(5)
                .onChange(value => {
                  const qty = Number(value);
                  if (value && Number.isInteger(qty)) this.qty = Math.max(1, Math.min(10000, qty));
                })
            }
              .enableDec(this.qty > 1).enableInc(this.qty < 10000)
              .onDec(() => this.qty = Math.max(1, this.qty - 1))
              .onInc(() => this.qty = Math.min(10000, this.qty + 1))
          }.width('100%')
          Button('加入购物车').height(48).width('100%').onClick(() => this.addToBasket())
        } else {
          Text('当前没有可购买的上架位置').fontSize(14).fontColor($r('sys.color.font_secondary')).width('100%')
        }
        Divider()
        Text('近期价格').fontSize(18).fontWeight(FontWeight.Medium).width('100%')
        if (this.historyLoading || !this.history.length) {
          LoadState({ loading: this.historyLoading, message: this.historyError || '暂无价格记录' })
        } else {
          if (this.chartPoints.length > 1) {
            PriceChart({ points: this.chartPoints })
            Text('蓝线 买入  ·  灰线 卖出').fontSize(12)
              .fontColor($r('sys.color.font_secondary')).width('100%')
          }
          ForEach(this.history, (point: PricePoint) => {
            Row({ space: 12 }) {
              Text(dateText(point.timestamp)).layoutWeight(1).fontSize(13)
              Text(money(point.avgBuyPrice) + ' / ' + money(point.avgSellPrice)).fontSize(13)
            }.width('100%').padding({ top: 6, bottom: 6 })
          }, (point: PricePoint) => point.timestamp)
        }
      }.padding(20)
    }
  }

  @Builder
  cart() {
    Scroll() {
      Column({ space: 12 }) {
        ScreenHeader({ title: '购物车', caption: this.basket.length + ' 种商品' })
        if (!this.basket.length) {
          Text('购物车为空').fontColor($r('sys.color.font_secondary'))
        }
        ForEach(this.basket, (line: BasketLine) => {
          Row({ space: 8 }) {
            Column({ space: 4 }) {
              Text(line.title).fontSize(16)
              Text(line.quantity + ' 件  ·  ' + money(line.unitPrice * line.quantity))
                .fontSize(13).fontColor($r('sys.color.font_secondary'))
            }.alignItems(HorizontalAlign.Start).layoutWeight(1)
            Button('移除', { buttonStyle: ButtonStyleMode.TEXTUAL }).enabled(!this.sending)
              .onClick(() => this.updateBasket(this.basket.filter(item => this.lineKey(item) !== this.lineKey(line))))
          }.width('100%').padding(10)
        }, (line: BasketLine) => this.lineKey(line) + ':' + line.quantity)
        if (this.basket.length && this.token) {
          if (this.addresses.length) {
            Select(this.addresses.map((address): SelectOption => ({ value: address })))
              .value(this.address || '选择收货地址')
              .onSelect(index => this.address = this.addresses[index])
              .width('100%')
          }
          TextInput({ placeholder: '外卖分配器大编号', text: this.address })
            .onChange(value => this.address = value)
          Button(this.sending ? '正在提交' : '确认订单 ' + this.summary())
            .enabled(!this.sending && this.address.trim().length > 0).width('100%').height(48)
            .onClick(() => {
              this.getUIContext().getPromptAction().showDialog({
                title: '确认提交订单', message: '地址 ' + this.address.trim() + ' · 预计 ' + this.summary(),
                buttons: [{ text: '取消', color: $r('sys.color.font_primary') },
                  { text: '确认购买', color: $r('sys.color.ohos_id_color_activated') }]
              }).then(result => {
                if (result.index === 1) void this.checkout();
              }).catch(() => {});
            })
        } else if (this.basket.length) {
          Button('登录后结账').onClick(() => {
            this.sheetOpen = false;
            this.onAccount();
          })
        }
        if (this.checkoutMessage) {
          Text(this.checkoutMessage).fontSize(14).width('100%').copyOption(CopyOptions.LocalDevice)
        }
      }.padding(20).width('100%')
    }
  }

  @Builder
  orderList() {
    List({ space: 10 }) {
      ListItem() { ScreenHeader({ title: '订单' }) }
      if (!this.token) {
        ListItem() {
          LoadState({ message: '登录后查看订单', actionLabel: '前往账户', onAction: () => {
            this.sheetOpen = false;
            this.onAccount();
          } })
        }
      } else if (this.loadingOrders || this.orderError || !this.orders.length) {
        ListItem() {
          LoadState({ loading: this.loadingOrders, message: this.orderError || '暂无订单',
            actionLabel: this.orderError ? '重试' : '', onAction: () => void this.fetchOrders() })
        }
      }
      ForEach(this.orders, (order: Order) => {
        ListItem() {
          Column({ space: 6 }) {
            Text('订单 #' + order.orderId + '   ' + order.totalFormatted).fontSize(17).fontWeight(FontWeight.Medium)
            Text((order.status === 'pending' ? '待发货' : order.status === 'delivering' ? '配送中' :
              order.status === 'delivered' ? '已送达' : order.status === 'failed' ? '失败' : order.status) +
              ' · ' + dateText(order.createdAt)).fontSize(13)
            if (order.pickupCode) {
              Text('地址 ' + (order.deliveryMajor || '—') + '  ·  取件码 ' + order.pickupCode)
                .fontSize(14).copyOption(CopyOptions.LocalDevice)
            }
            if (order.failureReason) {
              Text(order.failureReason).fontSize(13)
            }
          }.alignItems(HorizontalAlign.Start).width('100%').padding(14)
            .borderRadius(16).backgroundColor(SERVICE_CARD_COLOR)
        }
      }, (order: Order) => order.orderId.toString())
    }.width('100%').padding({ top: 12, left: 16, right: 16 })
  }

  @Builder
  dashboard() {
    Scroll() {
      Column({ space: 14 }) {
        ScreenHeader({ title: '交易概览' })
        if (this.statsLoading || this.statsError) {
          LoadState({ loading: this.statsLoading, message: this.statsError,
            actionLabel: '重试', onAction: () => void this.showStats() })
        }
        if (this.shopStats) {
          Text('交易总额').fontSize(14).fontColor($r('sys.color.font_secondary'))
          Text(money(this.shopStats.totalMoney)).fontSize(30).fontWeight(FontWeight.Bold)
          Text(this.shopStats.total + ' 笔交易 · ' + this.shopStats.buys + ' 笔购入 · ' +
            this.shopStats.sells + ' 笔售出').fontSize(15)
        }
        if (this.economyStats) {
          Divider()
          Text('成交物品 ' + this.economyStats.uniqueItems + ' 种').fontSize(16)
          Text('参与玩家 ' + this.economyStats.uniquePlayers + ' 人').fontSize(16)
          Text('平均每笔 ' + money(this.economyStats.avgTransaction)).fontSize(16)
          Text('净流量 ' + money(this.economyStats.netFlow)).fontSize(16)
        }
      }.padding(20).width('100%').alignItems(HorizontalAlign.Start)
    }
  }

  @Builder
  sheetContent() {
    if (this.sheet === 'detail') {
      this.detail()
    } else if (this.sheet === 'basket') {
      this.cart()
    } else if (this.sheet === 'orders') {
      this.orderList()
    } else if (this.sheet === 'dashboard') {
      this.dashboard()
    }
  }

  @Builder
  catalogHeader() {
    Column() {
      ScreenHeader({ title: '商店', caption: this.resultCount + ' 件物品' })
      Search({ placeholder: '搜索物品名称或 ID', value: this.query })
        .margin({ left: 16, right: 16, bottom: 8 })
        .enableKeyboardOnFocus(false)
        .onChange(value => {
          this.query = value;
          clearTimeout(this.filterTimer);
          this.filterTimer = setTimeout(() => this.filter(), 120);
        })
      Row({ space: 4 }) {
        Select(this.categories.map((category): SelectOption => ({ value: category.name })))
          .selected(this.categoryIndex)
          .value(this.categories[this.categoryIndex]?.name || '全部物品')
          .onSelect(index => {
            this.categoryIndex = index;
            this.filter();
          }).layoutWeight(1)
        Button('订单', { buttonStyle: ButtonStyleMode.TEXTUAL }).onClick(() => void this.fetchOrders())
        Button(this.basket.length ? '购物车 ' + this.basket.length : '购物车',
          { buttonStyle: ButtonStyleMode.TEXTUAL }).onClick(() => void this.showBasket())
        Button('刷新', { buttonStyle: ButtonStyleMode.TEXTUAL }).enabled(!this.loading)
          .onClick(() => void this.reload())
      }.padding({ left: 12, right: 12, bottom: 8 }).width('100%')
      Button('交易概览', { buttonStyle: ButtonStyleMode.TEXTUAL })
        .alignSelf(ItemAlign.Start).margin({ left: 12 }).onClick(() => void this.showStats())
      if (!this.resultCount) {
        LoadState({ loading: this.loading, message: this.error || '没有匹配的物品',
          actionLabel: this.error ? '重试' : '', onAction: () => void this.reload() })
      }
    }.width('100%')
  }

  build() {
    Column() {
      List({ space: 8 }) {
        ListItem() { this.catalogHeader() }
        LazyForEach(this.filtered, (product: Product) => {
          ListItem() {
            Row({ space: 12 }) {
              Column({ space: 4 }) {
                Text(product.title).fontSize(17).fontWeight(FontWeight.Medium).maxLines(1)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                Text(product.id).fontSize(12).fontColor($r('sys.color.font_secondary')).maxLines(1)
              }.layoutWeight(1).alignItems(HorizontalAlign.Start)
              Column({ space: 4 }) {
                Text('买 ' + money(product.price.buy)).fontSize(14)
                Text('卖 ' + money(product.price.sell)).fontSize(12).fontColor($r('sys.color.font_secondary'))
              }.alignItems(HorizontalAlign.End)
            }.width('100%').padding(14).borderRadius(16).backgroundColor(SERVICE_CARD_COLOR)
          }.padding({ left: 16, right: 16 }).onClick(() => void this.showProduct(product))
        }, (product: Product) => product.id)
      }.width('100%').layoutWeight(1).cachedCount(5)
        .contentEndOffset(BOTTOM_INSET)
        .clipContent(ContentClipMode.SAFE_AREA)
        .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
    }
    .width('100%').height('100%')
    .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
    .bindSheet($$this.sheetOpen, this.sheetContent(), {
      height: SheetSize.LARGE, dragBar: true, showClose: true, backgroundColor: SERVICE_SHEET_COLOR
    })
  }
}
