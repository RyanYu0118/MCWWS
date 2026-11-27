import { webview } from '@kit.ArkWeb';
import { common } from '@kit.AbilityKit';
import { pasteboard } from '@kit.BasicServicesKit';
import { errorMessage, MAP_ORIGIN, ORIGIN, request, SESSION } from '../services/Api';
import { MAP_RUNTIME } from '../services/MapRuntime';
import { BOTTOM_INSET } from '../components/NativeUi';

interface Marker {
  id: string;
  label: string;
  description?: string;
  viewUrl: string;
  itemCount: number;
  tradeLabels?: string[];
  tradeItemIds?: string[];
}
interface MarkerResponse {
  markers: Marker[];
}
interface PlayerPoint {
  map: string;
  x: number;
  y: number;
  z: number;
}
interface MapState {
  ready: boolean;
  map: string;
  mode: string;
  night: boolean;
  locked: boolean;
  nightVision: boolean;
  following: boolean;
  canFly: boolean;
  editor: boolean;
  maps?: string[];
  hash: string;
}
interface LayerState {
  infoEnabled?: boolean;
  roadNames?: boolean;
  buildingNames?: boolean;
  above320?: boolean;
  residenceBounds?: boolean;
  simplified?: boolean;
}
interface CachedTiles {
  tiles: number;
  active: boolean;
  busy: boolean;
}
class MapEvents {
  private onMarker: (id: string) => void;
  constructor(onMarker: (id: string) => void) {
    this.onMarker = onMarker;
  }
  marker(id: string): void {
    if (/^[A-Za-z0-9_.:-]{1,150}$/.test(id)) this.onMarker(id);
  }
}

const MAP_URL: string = MAP_ORIGIN + '/';
const PANEL: string = '#171719';
const GLASS_TINT: string = '#2AFFFFFF';
const EDGE: string = '#70FFFFFF';
const INK: string = '#FFFFFF';
const FADED: string = '#E8FFFFFF';
const MAP_ACTION_SIZE: number = 46;
const MAP_ACTION_GAP: number = 8;
const MAP_ACTION_LIST_HEIGHT: number = 8 * MAP_ACTION_SIZE + 7 * MAP_ACTION_GAP;
const INITIAL_STATE: MapState = {
  ready: false, map: 'world', mode: '3D', night: false, locked: false, nightVision: false,
  following: false, canFly: false, editor: false, hash: ''
};

@Styles
function mapGlass() {
  .backgroundColor(Color.Transparent)
  .backgroundEffect({ radius: 20, saturation: 0.85, brightness: 0.76, color: GLASS_TINT })
  .border({ width: 0.75, color: EDGE })
}

@Component
struct MapAction {
  @Prop label: string = '';
  @Prop symbol: string = '';
  @Prop icon: Resource = $r('app.media.map_tools');
  @Prop selected: boolean = false;
  @Prop available: boolean = true;
  onAction: () => void = () => {};

  build() {
    Button({ type: ButtonType.Circle }) {
      if (this.symbol) {
        Text(this.symbol).fontSize(14).fontWeight(FontWeight.Medium).fontColor(INK)
      } else {
        Image(this.icon).width(21).height(21).accessibilityLevel('no')
      }
    }
    .width(MAP_ACTION_SIZE).height(MAP_ACTION_SIZE)
    .mapGlass()
    .borderColor(this.selected ? INK : EDGE)
    .hitTestBehavior(HitTestMode.BLOCK_HIERARCHY)
    .accessibilityText(this.label)
    .enabled(this.available)
    .onClick(() => this.onAction())
  }
}

@Component
export struct MapPage {
  @StorageLink(SESSION) @Watch('sessionChanged') token: string = '';
  @Prop @Watch('targetChanged') target: string = '';
  @Prop @Watch('activeChanged') active: boolean = false;
  @Prop clean: boolean = false;
  onToggleClean: () => void = () => {};
  onAccount: () => void = () => {};
  onShopSearch: (label: string) => void = () => {};

  @State view: MapState = INITIAL_STATE;
  @State progress: number = 0;
  @State actionAreaHeight: number = MAP_ACTION_LIST_HEIGHT;
  @State failed: boolean = false;
  @State sheetOpen: boolean = false;
  @State sheet: string = 'search';
  @State editing: boolean = false;
  @State query: string = '';
  @State tradeOnly: boolean = false;
  @State dimensionOnly: boolean = false;
  @State markers: Marker[] = [];
  @State markerError: string = '';
  @State markersLoading: boolean = false;
  @State marker: Marker | null = null;
  @State maps: string[] = ['world', 'world_nether', 'world_the_end', 'dimensionalhome'];
  @State layer: LayerState | null = null;
  @State layerBusy: boolean = false;
  @State tileCache: CachedTiles | null = null;
  @State cacheBusy: boolean = false;
  @State authBusy: boolean = false;
  private controller: webview.WebviewController = new webview.WebviewController();
  private events: MapEvents = new MapEvents(id => void this.showMarker(id));
  private watchTimer: number = -1;
  private sequence: number = 0;
  private syncedToken: string = '';
  private lightLockPending: boolean = false;

  aboutToDisappear(): void {
    clearTimeout(this.watchTimer);
    this.sequence++;
  }

  activeChanged(): void {
    if (this.active) void this.readState();
  }

  sessionChanged(): void {
    if (!this.view.ready || !this.syncedToken) return;
    this.syncedToken = '';
    void this.sendSession('').catch(() => {});
  }

  targetChanged(): void {
    if (this.view.ready && this.target) this.goTo(this.target);
  }

  private async script(action: string, value: string = '', flag: boolean = false): Promise<object | boolean> {
    const result = await this.controller.runJavaScript(
      'location.origin===' + JSON.stringify(MAP_ORIGIN) + ' && typeof window.MCWWSMobileRuntime==="function" && ' +
      'window.MCWWSMobileRuntime(' + JSON.stringify(action) + ',' + JSON.stringify(value) + ',' + String(flag) + ')');
    return JSON.parse(result) as object | boolean;
  }

  private async pageReady(): Promise<void> {
    if (this.failed) return;
    const sequence = ++this.sequence;
    try {
      if (await this.controller.runJavaScript(MAP_RUNTIME) !== 'true') return;
      for (let attempt = 0; attempt < 24 && sequence === this.sequence; attempt++) {
        await this.readState();
        if (this.view.ready) {
          this.targetChanged();
          return;
        }
        await this.delay(250);
      }
    } catch {
      if (sequence === this.sequence) this.failed = true;
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise<void>(resolve => { setTimeout(resolve, ms); });
  }

  private async readState(): Promise<void> {
    try {
      const response = await this.script('state');
      if (typeof response === 'boolean') return;
      const state = response as MapState;
      this.view = state;
      if (state.maps?.length) this.maps = state.maps;
      if (this.editing && !state.editor) this.editing = false;
    } catch {
      // Renderer navigation can invalidate pending JavaScript calls.
    }
  }

  private refreshState(ms: number = 360): void {
    clearTimeout(this.watchTimer);
    this.watchTimer = setTimeout(() => void this.readState(), ms);
  }

  private toast(message: string): void {
    try {
      this.getUIContext().getPromptAction().showToast({ message });
    } catch {
      // The application may have left the foreground.
    }
  }

  private async action(name: string): Promise<void> {
    if (!this.view.ready || (name === 'daynight' && (this.lightLockPending || this.view.locked))) return;
    try {
      const ok = await this.script('click', name);
      if (ok === true) {
        this.refreshState(name === 'night-vision' ? 0 : 360);
        return;
      }
    } catch {
      // Navigation can invalidate the control.
    }
    this.toast('此地图控件暂不可用');
  }

  private async lockLight(): Promise<void> {
    if (!this.view.ready || this.lightLockPending) return;
    this.lightLockPending = true;
    try {
      if (await this.script('dayLock') !== true) {
        this.toast('无法切换光照锁定');
        return;
      }
      await this.delay(860);
      await this.readState();
    } catch {
      this.toast('无法切换光照锁定');
    } finally {
      this.lightLockPending = false;
    }
  }

  private getHash(link: string): string {
    const offset = link.indexOf('#');
    const hash = offset >= 0 ? link.substring(offset) : '';
    return /^#[A-Za-z0-9_.:-]+$/.test(hash) ? hash : '';
  }

  private goTo(link: string): void {
    const hash = this.getHash(link);
    if (!hash) {
      this.toast('没有可用的地图坐标');
      return;
    }
    if (!this.view.ready) {
      this.toast('地图尚未就绪');
      return;
    }
    this.sheetOpen = false;
    void this.script('view', hash).then(() => this.refreshState(650)).catch(() => this.toast('定位失败'));
  }

  private coordinates(value: string): void {
    const parts = value.trim().split(/[ ,，]+/);
    if (parts.length !== 3 || !parts.every(part => /^-?\d+(?:\.\d+)?$/.test(part))) return;
    const mode = this.view.mode === '2D' ? 'flat' : 'perspective';
    this.goTo('#' + this.view.map + ':' + parts.join(':') + ':72:0:1.57:0:0:' + mode);
  }

  private switchDimension(id: string): void {
    const view = this.getHash(this.view.hash);
    const parts = view.slice(1).split(':');
    const location = parts.length >= 4 ? parts.slice(1).join(':') : '0:64:0:200:0:1.57:0:0:perspective';
    this.goTo('#' + id + ':' + location);
  }

  private mapLabel(id: string): string {
    switch (id) {
      case 'world': return '主世界';
      case 'world_nether': return '下界';
      case 'world_the_end': return '末地';
      case 'dimensionalhome': return '多维之家';
      default: return id;
    }
  }

  private openSheet(name: string): void {
    this.sheet = name;
    this.sheetOpen = true;
    if (name === 'search') void this.loadMarkers(false);
    if (name === 'layers') void this.loadLayer();
    if (name === 'tools') void this.readState();
    if (name === 'cache') void this.loadCache(false);
  }

  private async loadMarkers(reload: boolean): Promise<void> {
    if (this.markersLoading || (!reload && this.markers.length > 0)) return;
    this.markerError = '';
    this.markersLoading = true;
    try {
      this.markers = (await request<MarkerResponse>('/api/shop-map-markers')).markers;
    } catch (err) {
      this.markerError = errorMessage(err as Error);
    } finally {
      this.markersLoading = false;
    }
  }

  private async showMarker(id: string): Promise<void> {
    await this.loadMarkers(false);
    const marker = this.markers.find(item => item.id === id);
    if (marker) this.openMarker(marker);
  }

  private openMarker(marker: Marker): void {
    this.marker = marker;
    this.openSheet('marker');
  }

  private locateMarker(flat: boolean): void {
    const hash = this.getHash(this.marker?.viewUrl || '');
    const fields = hash.split(':');
    if (fields.length < 4) {
      this.toast('没有可用的地图坐标');
      return;
    }
    if (flat) {
      this.goTo(fields.slice(0, 4).join(':') + ':72:0:0:0:1:flat');
    } else {
      this.goTo(fields.slice(0, 4).join(':') + ':64:0:1.1:0:0:perspective');
    }
  }

  private matches(marker: Marker): boolean {
    const query = this.query.trim().toLowerCase();
    if (this.dimensionOnly && this.getHash(marker.viewUrl).slice(1).split(':')[0] !== this.view.map) return false;
    if (this.tradeOnly && !(marker.tradeItemIds?.length || marker.tradeLabels?.length)) return false;
    if (!query) return true;
    const trades = (marker.tradeLabels || []).concat(marker.tradeItemIds || []).join(' ').toLowerCase();
    return trades.includes(query) || (!this.tradeOnly &&
      (marker.label + ' ' + (marker.description || '')).toLowerCase().includes(query));
  }

  private async sendSession(token: string): Promise<void> {
    await this.controller.runJavaScript(
      'location.origin===' + JSON.stringify(MAP_ORIGIN) + ' && window.postMessage({type:"mcwws-auth",' +
      'authToken:' + JSON.stringify(token || null) + ',webApiUrl:' + JSON.stringify(ORIGIN) + '},location.origin)');
  }

  private async playerAction(follow: boolean): Promise<void> {
    if (!this.token) {
      this.sheetOpen = false;
      this.onAccount();
      return;
    }
    if (this.authBusy || !this.view.ready) return;
    this.authBusy = true;
    const token = this.token;
    try {
      if (!follow) {
        const point = await request<PlayerPoint>('/api/player-location', token);
        if (token === this.token) {
          this.goTo('#' + point.map + ':' + point.x + ':' + point.y + ':' + point.z + ':48:0:1.57:0:0:perspective');
        }
        return;
      }
      if (token !== this.syncedToken) {
        await request<object>('/api/profile', token);
        if (token !== this.token) return;
        await this.sendSession(token);
        this.syncedToken = token;
        await this.delay(500);
      }
      this.sheetOpen = false;
      await this.action('locate');
      this.refreshState(1000);
    } catch (err) {
      this.toast(errorMessage(err as Error));
    } finally {
      this.authBusy = false;
    }
  }

  private async loadLayer(): Promise<void> {
    await this.updateLayer('layerRead');
  }

  private async updateLayer(action: string, name: string = '', value: boolean = false): Promise<void> {
    if (!this.view.ready || this.layerBusy) return;
    this.layerBusy = true;
    try {
      let result = await this.script(action, name, value);
      if (result === false) {
        await this.delay(480);
        result = await this.script(action, name, value);
      }
      if (typeof result !== 'boolean') this.layer = result as LayerState;
      else this.toast('图层控件尚未就绪');
    } catch {
      this.toast('无法读取图层');
    } finally {
      this.layerBusy = false;
    }
  }

  private async launchEditor(): Promise<void> {
    if (!this.token) {
      this.sheetOpen = false;
      this.onAccount();
      return;
    }
    if (this.authBusy) return;
    this.authBusy = true;
    try {
      const token = this.token;
      await request<object>('/api/admin/access', token);
      if (token !== this.token) return;
      await this.sendSession(token);
      this.syncedToken = token;
      this.sheetOpen = false;
      await this.delay(580);
      const opened = await this.script('editor');
      if (opened === true) {
        this.editing = true;
      } else {
        this.toast('编辑器尚未就绪');
      }
    } catch (err) {
      this.toast(errorMessage(err as Error));
    } finally {
      this.authBusy = false;
    }
  }

  private async leaveEditor(): Promise<void> {
    try {
      await this.script('editorExit');
      this.editing = false;
    } catch {
      this.toast('无法关闭编辑器');
    }
  }

  private async loadCache(clear: boolean): Promise<void> {
    if (this.cacheBusy || !this.view.ready) return;
    this.cacheBusy = true;
    try {
      await this.script(clear ? 'clearCache' : 'cache');
      for (let i = 0; i < 10; i++) {
        await this.delay(200);
        const result = await this.script('cacheResult');
        if (typeof result === 'boolean') break;
        const info = result as CachedTiles;
        if (!info.busy) {
          this.tileCache = info;
          break;
        }
      }
    } catch {
      this.toast('无法读取地图缓存');
    } finally {
      this.cacheBusy = false;
    }
  }

  private async copyPosition(): Promise<void> {
    await this.readState();
    const hash = this.getHash(this.view.hash);
    if (!hash) {
      this.toast('当前视角还没有可分享的坐标');
      return;
    }
    try {
      const data = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, ORIGIN + '/map.html' + hash);
      await pasteboard.getSystemPasteboard().setData(data);
      this.toast('已复制地图链接');
    } catch {
      this.toast('无法复制地图链接');
    }
  }

  private sendFlightKey(key: string, down: boolean): void {
    void this.script('key', key, down).catch(() => {});
  }

  @Builder
  heading(title: string, subtitle: string = '') {
    Column({ space: 6 }) {
      Text(title).fontSize(24).fontWeight(FontWeight.Bold).fontColor(INK)
      if (subtitle) Text(subtitle).fontSize(13).fontColor(FADED)
    }.width('100%').alignItems(HorizontalAlign.Start).padding({ bottom: 8 })
  }

  @Builder
  rowAction(label: string, hint: string, handler: () => void, available: boolean = true) {
    Button({ type: ButtonType.Normal }) {
      Row() {
        Column({ space: 4 }) {
          Text(label).fontSize(16).fontColor(INK)
          if (hint) Text(hint).fontSize(13).fontColor(FADED)
        }.layoutWeight(1).alignItems(HorizontalAlign.Start)
        Text('›').fontSize(22).fontColor(FADED)
      }.width('100%')
    }
    .width('100%').padding(13).borderRadius(16).backgroundColor(GLASS_TINT)
    .enabled(available)
    .onClick(handler)
  }

  @Builder
  layerToggle(label: string, name: string, state: boolean, enabled: boolean = true) {
    Row({ space: 10 }) {
      Text(label).fontSize(16).fontColor(INK).layoutWeight(1)
      Toggle({ type: ToggleType.Switch, isOn: state })
        .enabled(enabled && this.layer !== null && !this.layerBusy)
        .onChange(value => void this.updateLayer('layerSet', name, value))
    }.width('100%').padding(12).backgroundColor(GLASS_TINT).borderRadius(15)
  }

  @Builder
  searchBox() {
    Search({ value: this.query, placeholder: '商店、物品或 X Y Z' })
      .width('100%').height(48).fontColor(INK).placeholderColor(FADED)
      .backgroundColor(GLASS_TINT)
      .searchIcon({ color: FADED })
      .onChange(value => this.query = value)
      .onSubmit(value => this.coordinates(value))
  }

  @Builder
  searchSheet() {
    Column({ space: 10 }) {
      this.heading('地图搜索', this.mapLabel(this.view.map))
      this.searchBox()
      Row({ space: 8 }) {
        Text('仅匹配交易物品').fontSize(14).fontColor(FADED).layoutWeight(1)
        Toggle({ type: ToggleType.Switch, isOn: this.tradeOnly }).onChange(value => this.tradeOnly = value)
        Button('刷新', { buttonStyle: ButtonStyleMode.TEXTUAL }).fontColor(INK)
          .enabled(!this.markersLoading).onClick(() => void this.loadMarkers(true))
      }.width('100%')
      Row({ space: 8 }) {
        Text('仅查看当前维度').fontSize(14).fontColor(FADED).layoutWeight(1)
        Toggle({ type: ToggleType.Switch, isOn: this.dimensionOnly }).onChange(value => this.dimensionOnly = value)
      }.width('100%')
      if (this.markersLoading || this.markerError) {
        Text(this.markerError || '正在读取商店标记').fontSize(14).fontColor(FADED)
      } else if (!this.markers.some(marker => this.matches(marker))) {
        Text('没有匹配的商店或物品').fontSize(14).fontColor(FADED)
      }
      List({ space: 7 }) {
        ForEach(this.markers.filter(marker => this.matches(marker)), (marker: Marker) => {
          ListItem() {
            this.rowAction(marker.label, (marker.description ? marker.description + ' · ' : '') +
              marker.itemCount + ' 件商品', () => this.openMarker(marker))
          }
        }, (marker: Marker) => marker.id)
      }.width('100%').layoutWeight(1).cachedCount(4)
    }.width('100%').height('100%').padding(20)
  }

  @Builder
  markerSheet() {
    Scroll() {
      Column({ space: 10 }) {
        this.heading(this.marker?.label || '商店标记', this.marker?.description || '')
        this.rowAction('俯视定位', '', () => this.locateMarker(true), this.view.ready)
        this.rowAction('3D 定位', '', () => this.locateMarker(false), this.view.ready)
        Text((this.marker?.itemCount || 0) + ' 件商品').fontSize(16).fontColor(INK).width('100%')
        ForEach(this.marker?.tradeLabels || [], (label: string) => {
          this.rowAction(label, '', () => {
            this.sheetOpen = false;
            this.onShopSearch(label);
          })
        })
      }.width('100%').padding(20).alignItems(HorizontalAlign.Start)
    }
  }

  @Builder
  layerSheet() {
    Scroll() {
      Column({ space: 8 }) {
        this.heading('维度与图层', this.mapLabel(this.view.map))
        Select(this.maps.map((id): SelectOption => ({ value: this.mapLabel(id) })))
          .selected(Math.max(0, this.maps.indexOf(this.view.map)))
          .value(this.mapLabel(this.view.map))
          .width('100%').onSelect(index => this.switchDimension(this.maps[index]))
        this.rowAction(this.layer?.simplified ? '切回原版地图' : '切换简化地图',
          this.layer?.simplified ? '恢复地形和区块细节' : '白色背景，只显示地理信息',
          () => void this.updateLayer('mapStyle', this.layer?.simplified ? 'original' : 'simplified'),
          this.layer !== null && !this.layerBusy)
        this.layerToggle('地理信息', 'info-toggle', this.layer?.infoEnabled ?? false, !this.layer?.simplified)
        this.layerToggle('道路名称', 'show-road-names', this.layer?.roadNames ?? false,
          this.layer?.infoEnabled === true)
        this.layerToggle('建筑名称', 'show-building-names', this.layer?.buildingNames ?? false,
          this.layer?.infoEnabled === true)
        this.layerToggle('领地范围', 'show-residence-bounds', this.layer?.residenceBounds ?? false)
        this.layerToggle('渲染 Y=320 以上方块', 'render-above-320', this.layer?.above320 ?? false)
        if (this.layerBusy) Text('正在应用图层').fontColor(FADED).fontSize(13)
        this.rowAction('专业 GIS 编辑', '编辑标注、道路、区域和三维体块', () => void this.launchEditor())
      }.padding(20).width('100%').alignItems(HorizontalAlign.Start)
    }
  }

  @Builder
  toolsSheet() {
    Scroll() {
      Column({ space: 8 }) {
        this.heading('地图工具', this.mapLabel(this.view.map) + ' · ' + this.view.mode)
        this.rowAction('复位朝北', '', () => void this.action('compass'))
        this.rowAction(this.view.mode === '2D' ? '切换到 3D' : '切换到 2D', '', () => void this.action('mode'))
        if (this.view.canFly) {
          this.rowAction(this.view.mode === '飞行' ? '退出自由飞行' : '自由飞行', '使用触屏方向键移动', () => {
            this.sheetOpen = false;
            void this.action('fly');
          })
        }
        this.rowAction(this.view.night ? '切换日景' : '切换夜景', '临时覆盖游戏时间',
          () => void this.action('daynight'), !this.view.locked)
        this.rowAction(this.view.locked ? '解锁，跟随游戏时间' : '锁定当前光照', '',
          () => void this.lockLight())
        Row() {
          Text('夜视').fontSize(16).fontColor(INK).layoutWeight(1)
          Toggle({ type: ToggleType.Switch, isOn: this.view.nightVision })
            .onChange(() => void this.action('night-vision'))
        }.width('100%').padding(12).backgroundColor(GLASS_TINT).borderRadius(15)
        this.rowAction('定位玩家', '查看当前位置或最后离线位置', () => void this.playerAction(false))
        this.rowAction(this.view.following ? '停止跟随' : '跟随玩家', '', () => void this.playerAction(true))
        this.rowAction('复制当前位置链接', '', () => void this.copyPosition())
        this.rowAction('地图缓存', '管理已浏览瓦片', () => this.openSheet('cache'))
        this.rowAction('全屏地图', '隐藏工具与应用底栏', () => {
          this.sheetOpen = false;
          this.onToggleClean();
        })
        this.rowAction('重新加载地图', '', () => {
          this.sheetOpen = false;
          this.controller.refresh();
        })
      }.width('100%').padding(20).alignItems(HorizontalAlign.Start)
    }
  }

  @Builder
  cacheSheet() {
    Column({ space: 10 }) {
      this.heading('地图缓存')
      if (this.cacheBusy) {
        Text('正在读取瓦片缓存').fontColor(FADED)
      } else {
        Text(this.tileCache?.active ? '已缓存 ' + this.tileCache.tiles + ' 张瓦片' : '瓦片缓存暂不可用')
          .fontColor(INK).fontSize(16)
      }
      Text('缓存覆盖已浏览的瓦片。离线查看新区域仍需网络。').fontSize(14).fontColor(FADED)
      this.rowAction('刷新缓存统计', '', () => void this.loadCache(false), !this.cacheBusy)
      this.rowAction('清除地图瓦片缓存', '', () => {
        this.getUIContext().getPromptAction().showDialog({
          title: '清除地图瓦片缓存',
          message: '下次查看这些区域时会重新加载瓦片。',
          buttons: [{ text: '取消', color: FADED }, { text: '清除', color: INK }]
        }).then(answer => {
          if (answer.index === 1) void this.loadCache(true);
        }).catch(() => {});
      }, !this.cacheBusy && (this.tileCache?.tiles || 0) > 0)
    }.width('100%').padding(20).alignItems(HorizontalAlign.Start)
  }

  @Builder
  sheetContent() {
    if (this.sheet === 'search') this.searchSheet()
    else if (this.sheet === 'marker') this.markerSheet()
    else if (this.sheet === 'layers') this.layerSheet()
    else if (this.sheet === 'cache') this.cacheSheet()
    else this.toolsSheet()
  }

  @Builder
  flyingKey(label: string, key: string) {
    Button(label)
      .width(44).height(42).fontColor(INK).mapGlass()
      .hitTestBehavior(HitTestMode.BLOCK_HIERARCHY)
      .onClick(() => this.sendFlightKey(key, true))
  }

  @Builder
  dayNightControl() {
    Button({ type: ButtonType.Circle }) {
      Image(this.view.locked ? $r('app.media.map_light_lock') :
        this.view.night ? $r('app.media.map_moon') : $r('app.media.map_sun'))
        .width(21).height(21).accessibilityLevel('no')
    }
    .width(MAP_ACTION_SIZE).height(MAP_ACTION_SIZE).mapGlass()
    .borderColor(this.view.locked ? INK : EDGE)
    .hitTestBehavior(HitTestMode.BLOCK_HIERARCHY)
    .accessibilityText(this.view.locked ? '已锁定光照，长按解锁' :
      (this.view.night ? '切换日景' : '切换夜景') + '，长按锁定当前光照')
    .onClick(() => void this.action('daynight'))
    .priorityGesture(LongPressGesture({ duration: 600 }).onAction(() => void this.lockLight()))
  }

  @Builder
  searchOverlay() {
    Column({ space: 8 }) {
      Button({ type: ButtonType.Capsule }) {
        Row({ space: 9 }) {
          Image($r('app.media.map_search')).width(17).height(17).accessibilityLevel('no')
          Text('搜索商店、物品或坐标').fontSize(15).fontColor(INK).layoutWeight(1)
        }.width('100%').padding({ left: 13, right: 12 })
      }
      .width('100%').height(45).mapGlass()
      .hitTestBehavior(HitTestMode.BLOCK_HIERARCHY)
      .accessibilityText('搜索地图').onClick(() => this.openSheet('search'))
      if (this.progress < 100) {
        Progress({ value: this.progress, total: 100, type: ProgressType.Linear }).style({ strokeWidth: 2 })
      }
    }.width('100%').padding({ left: 16, right: 16, top: 12, bottom: 9 })
      .hitTestBehavior(HitTestMode.Transparent)
  }

  @Builder
  controls() {
    Column() {
      if (this.editing || this.clean) {
        Row() {
          MapAction({ label: this.editing ? '退出编辑' : '退出全屏', icon: $r('app.media.map_close'),
            onAction: () => {
              if (this.editing) void this.leaveEditor();
              else this.onToggleClean();
            } })
        }.width('100%').justifyContent(FlexAlign.End).padding(12).hitTestBehavior(HitTestMode.Transparent)
      } else {
        this.searchOverlay()
        if (this.view.ready) {
          Row({ space: 10 }) {
            if (this.view.mode === '飞行') {
              Column({ space: 5 }) {
                Row({ space: 5 }) {
                  this.flyingKey('前', 'w')
                  this.flyingKey('升', ' ')
                }
                Row({ space: 5 }) {
                  this.flyingKey('左', 'a')
                  this.flyingKey('后', 's')
                  this.flyingKey('右', 'd')
                  this.flyingKey('降', 'Shift')
                }
              }.alignItems(HorizontalAlign.Start)
            }
            Blank().hitTestBehavior(HitTestMode.None)
            Scroll() {
              Column({ space: MAP_ACTION_GAP }) {
                MapAction({ label: '维度与图层', icon: $r('app.media.map_layers'),
                  onAction: () => this.openSheet('layers') })
                MapAction({ label: '地图工具', icon: $r('app.media.map_tools'),
                  onAction: () => this.openSheet('tools') })
                MapAction({ label: '复位朝北', icon: $r('app.media.map_north'), onAction: () => void this.action('compass') })
                MapAction({ label: '切换地图视角', symbol: this.view.mode, onAction: () => void this.action('mode') })
                this.dayNightControl()
                MapAction({ label: this.view.nightVision ? '关闭夜视' : '开启夜视', icon: $r('app.media.map_night_vision'),
                  selected: this.view.nightVision, onAction: () => void this.action('night-vision') })
                MapAction({ label: '定位玩家', icon: $r('app.media.map_locate'), selected: this.view.following,
                  onAction: () => void this.playerAction(false) })
                MapAction({ label: '全屏地图', icon: $r('app.media.map_fullscreen'), onAction: this.onToggleClean })
              }.hitTestBehavior(HitTestMode.Transparent)
            }
            .width(MAP_ACTION_SIZE).height(Math.min(this.actionAreaHeight, MAP_ACTION_LIST_HEIGHT))
            .enableScrollInteraction(this.actionAreaHeight < MAP_ACTION_LIST_HEIGHT)
            .scrollBar(BarState.Off)
            .hitTestBehavior(HitTestMode.Transparent)
          }
          .width('100%').layoutWeight(1).padding({ left: 16, right: 16 }).margin({ bottom: 12 })
          .alignItems(VerticalAlign.Bottom).hitTestBehavior(HitTestMode.Transparent)
          .onSizeChange((_, size) => {
            const height = size.height;
            if (typeof height === 'number' && height > 0) this.actionAreaHeight = height;
          })
        }
      }
    }.width('100%').height('100%')
      .padding({ bottom: this.clean ? 0 : BOTTOM_INSET })
      .hitTestBehavior(HitTestMode.Transparent)
  }

  build() {
    Stack({ alignContent: Alignment.Top }) {
      Web({ src: MAP_URL, controller: this.controller })
        .width('100%').height('100%')
        .backgroundColor(Color.Transparent)
        .visibility(this.failed ? Visibility.Hidden : Visibility.Visible)
        .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
        .javaScriptAccess(true).domStorageAccess(true).fileAccess(false)
        .cacheMode(CacheMode.Default)
        .javaScriptProxy({ object: this.events, name: 'MCWWSNativeMap', methodList: ['marker'],
          controller: this.controller })
        .onPageBegin(() => {
          this.sequence++;
          this.view = INITIAL_STATE;
          this.failed = false;
          this.syncedToken = '';
          this.progress = 0;
        })
        .onProgressChange(event => {
          if (event) this.progress = event.newProgress;
        })
        .onPageEnd(() => void this.pageReady())
        .onErrorReceive(event => {
          if (event?.request.isMainFrame()) this.failed = true;
        })
        .onHttpErrorReceive(event => {
          if (event?.request.isMainFrame()) this.failed = true;
        })
        .onLoadIntercept(event => {
          const url = event.data.getRequestUrl();
          return event.data.isMainFrame() && url !== MAP_ORIGIN && !url.startsWith(MAP_URL);
        })
      if (this.failed) {
        Column({ space: 12 }) {
          Text('地图无法连接').fontSize(22).fontColor(INK).fontWeight(FontWeight.Medium)
          Text('请检查网络后重试').fontSize(14).fontColor(FADED)
          Button('重新加载').fontColor(INK).mapGlass().onClick(() => this.controller.refresh())
          Button('查看商店标记', { buttonStyle: ButtonStyleMode.TEXTUAL }).fontColor(INK)
            .onClick(() => this.openSheet('search'))
        }.width('100%').height('100%').justifyContent(FlexAlign.Center)
      } else {
        this.controls()
      }
    }
    .width('100%').height('100%')
    .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
    .bindSheet($$this.sheetOpen, this.sheetContent(), { height: SheetSize.LARGE, dragBar: true,
      showClose: true, backgroundColor: PANEL })
  }
}
