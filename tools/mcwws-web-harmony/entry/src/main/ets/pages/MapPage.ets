import { webview } from '@kit.ArkWeb';
import { common } from '@kit.AbilityKit';
import { errorMessage, MAP_ORIGIN, ORIGIN, request, SESSION } from '../services/Api';
import { BOTTOM_INSET, LoadState } from '../components/NativeUi';

interface MapMarker {
  id: string;
  label: string;
  description?: string;
  viewUrl: string;
  tradeLabels?: string[];
  tradeItemIds?: string[];
}
interface MapMarkersResponse {
  markers: MapMarker[];
}
interface PlayerLocation {
  map: string;
  x: number;
  y: number;
  z: number;
}

const MAP_URL: string = MAP_ORIGIN + '/';
const RENDERER_STYLE: string =
  '#mcwws-shop-panel,#mcwws-map-controls,#mcwws-gis-wrap,.mcwws-gis-layer-dialog,' +
  '.mcwws-auth-widget{display:none!important}' +
  '#app{visibility:hidden!important;pointer-events:none!important}' +
  '#mcwws-shop-pin-layer,#mcwws-shop-pin-layer *{pointer-events:none!important}';

@Component
export struct MapPage {
  @StorageLink(SESSION) token: string = '';
  @State query: string = '';
  @State failed: boolean = false;
  @State ready: boolean = false;
  @State progress: number = 0;
  @State sheetOpen: boolean = false;
  @State sheet: string = '';
  @State markers: MapMarker[] = [];
  @State markerError: string = '';
  @State markersLoading: boolean = false;
  @State is3d: boolean = true;
  @State nightVision: boolean = false;
  @State clean: boolean = false;
  @Prop @Watch('targetChanged') target: string = '';
  onAccount: () => void = () => {};
  private controller: webview.WebviewController = new webview.WebviewController();

  private async pageReady(): Promise<void> {
    if (this.failed) {
      return;
    }
    try {
      this.ready = await this.controller.runJavaScript(
        "(() => {" +
        "if(location.origin!==" + JSON.stringify(MAP_ORIGIN) + ")return false;" +
        "let node=document.getElementById('mcwws-native-surface');" +
        "if(!node){node=document.createElement('style');node.id='mcwws-native-surface';document.head.append(node);}" +
        "node.textContent=" + JSON.stringify(RENDERER_STYLE) + ";return true;})()"
      ) === 'true';
      this.targetChanged();
    } catch {
      this.ready = false;
    }
  }

  private async mapScript(code: string): Promise<string> {
    return this.controller.runJavaScript(
      '(() => { if(location.origin !== ' + JSON.stringify(MAP_ORIGIN) + ') return false; ' + code + ' })()');
  }

  private async openAdvancedMap(): Promise<void> {
    try {
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await context.openLink(ORIGIN + '/map.html');
    } catch (err) {
      this.getUIContext().getPromptAction().showToast({ message: '无法打开系统浏览器' });
    }
  }

  targetChanged(): void {
    if (this.ready && this.target) {
      this.goTo(this.target);
    }
  }

  private async locatePlayer(): Promise<void> {
    try {
      const point = await request<PlayerLocation>('/api/player-location', this.token);
      this.goTo('#' + point.map + ':' + point.x + ':' + point.y + ':' + point.z +
        ':48:0:1.57:0:0:perspective');
    } catch (err) {
      this.getUIContext().getPromptAction().showToast({ message: errorMessage(err as Error) });
    }
  }

  private async mapAction(selector: string): Promise<boolean> {
    if (!this.ready) {
      return false;
    }
    try {
      const result = await this.mapScript('const button=document.querySelector(' + JSON.stringify(selector) +
        '); if(!button || button.disabled)return false; button.click();return true;');
      if (result === 'true') {
        return true;
      }
    } catch {
      // The map may be changing dimensions.
    }
    this.getUIContext().getPromptAction().showToast({ message: '地图控件还未就绪' });
    return false;
  }

  private goTo(url: string): void {
    const hash = url.substring(url.indexOf('#') + 1);
    if (!url.includes('#') || !/^[A-Za-z0-9_:-]+(?:[.A-Za-z0-9_:-]+)*$/.test(hash)) {
      this.getUIContext().getPromptAction().showToast({ message: '该标记没有可用的地图坐标' });
      return;
    }
    this.sheetOpen = false;
    if (this.ready) {
      void this.mapScript('location.hash=' + JSON.stringify(hash) + ';return true;').catch(() => {});
    }
  }

  private async search(): Promise<void> {
    this.sheet = 'search';
    this.sheetOpen = true;
    if (this.markers.length || this.markersLoading) {
      return;
    }
    this.markerError = '';
    this.markersLoading = true;
    try {
      this.markers = (await request<MapMarkersResponse>('/api/shop-map-markers')).markers;
    } catch (err) {
      this.markerError = errorMessage(err as Error);
    } finally {
      this.markersLoading = false;
    }
  }

  private switchDimension(id: string): void {
    if (!this.ready) return;
    this.goTo('#' + id + ':0:64:0:200:0:1.57:0:0:perspective');
  }

  @Builder
  searchContent() {
    Column({ space: 8 }) {
      Search({ value: this.query, placeholder: '搜索商店或商品' })
        .width('100%').onChange(value => this.query = value)
        .onSubmit(value => {
          const coords = value.trim().split(/[ ,，]+/);
          if (coords.length === 3 && coords.every(part => /^-?\d+(?:\.\d+)?$/.test(part))) {
            this.goTo('#world:' + coords.join(':') + ':64:0:1.57:0:0:perspective');
          }
        })
      if (this.markersLoading || this.markerError) {
        LoadState({ loading: this.markersLoading, message: this.markerError,
          actionLabel: '重试', onAction: () => void this.search() })
      }
      List({ space: 8 }) {
        ForEach(this.markers.filter(marker => {
          const query = this.query.trim().toLowerCase();
          return !query || (marker.label + ' ' + (marker.description || '') + ' ' +
            (marker.tradeLabels || []).join(' ') + ' ' + (marker.tradeItemIds || []).join(' ')).toLowerCase().includes(query);
        }), (marker: MapMarker) => {
          ListItem() {
            Row({ space: 8 }) {
              Column({ space: 4 }) {
                Text(marker.label).fontSize(17).fontWeight(FontWeight.Medium)
                if (marker.description) {
                  Text(marker.description).fontSize(13).fontColor($r('sys.color.font_secondary'))
                }
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Button('定位', { buttonStyle: ButtonStyleMode.TEXTUAL })
                .enabled(this.ready).onClick(() => this.goTo(marker.viewUrl))
            }.padding(12)
          }
        }, (marker: MapMarker) => marker.id)
      }.layoutWeight(1).width('100%').cachedCount(4)
    }.padding(18)
  }

  @Builder
  options() {
    Scroll() {
      Column({ space: 14 }) {
        Text('地图工具').fontSize(22).fontWeight(FontWeight.Medium).width('100%')
        Row({ space: 8 }) {
          Button('朝北').layoutWeight(1).onClick(() => void this.mapAction('.mcwws-ctrl-compass'))
          Button(this.is3d ? '切到 2D' : '切到 3D').layoutWeight(1)
            .onClick(() => void this.mapAction('.mcwws-ctrl-mode').then(done => {
              if (done) this.is3d = !this.is3d;
            }))
          Button('昼夜切换').layoutWeight(1).onClick(() => void this.mapAction('.mcwws-ctrl-daynight'))
        }.width('100%')
        Row() {
          Text('夜视').layoutWeight(1)
          Toggle({ type: ToggleType.Switch, isOn: this.nightVision })
            .onChange(value => void this.mapAction('.mcwws-ctrl-night-vision').then(done => {
              if (done) this.nightVision = value;
            }))
        }.width('100%')
        Row() {
          Text('维度').layoutWeight(1)
          Select([{ value: '主世界' }, { value: '下界' }, { value: '末地' }, { value: '多维之家' }])
            .value('选择维度')
            .onSelect(index => {
              this.switchDimension(['world', 'world_nether', 'world_the_end', 'dimensionalhome'][index]);
              this.sheetOpen = false;
            })
        }.width('100%')
        Button(this.token ? '定位我的角色' : '登录后定位')
          .width('100%')
          .onClick(() => {
            this.sheetOpen = false;
            if (this.token) {
              void this.locatePlayer();
            } else {
              this.onAccount();
            }
          })
        Button('隐藏地图工具', { buttonStyle: ButtonStyleMode.TEXTUAL })
          .width('100%')
          .onClick(() => {
            this.sheetOpen = false;
            this.clean = true;
          })
        Button('在浏览器打开高级 GIS 编辑器', { buttonStyle: ButtonStyleMode.TEXTUAL })
          .width('100%').onClick(() => void this.openAdvancedMap())
      }.padding(20).width('100%').alignItems(HorizontalAlign.Start)
    }
  }

  @Builder
  sheetContent() {
    if (this.sheet === 'search') {
      this.searchContent()
    } else if (this.sheet === 'tools') {
      this.options()
    }
  }

  build() {
    Stack({ alignContent: Alignment.Top }) {
      Web({ src: MAP_URL, controller: this.controller })
        .width('100%').height('100%')
        .javaScriptAccess(true).domStorageAccess(true).fileAccess(false)
        .cacheMode(CacheMode.Default)
        .onPageBegin(() => {
          this.ready = false;
          this.failed = false;
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
        Column() {
          LoadState({ message: '无法连接 BlueMap。请检查网络或稍后重试。',
            actionLabel: '重新加载', onAction: () => this.controller.refresh() })
          Button('查看商店标记', { buttonStyle: ButtonStyleMode.TEXTUAL })
            .onClick(() => void this.search())
          Button('在浏览器打开地图', { buttonStyle: ButtonStyleMode.TEXTUAL })
            .onClick(() => void this.openAdvancedMap())
        }.width('100%').height('100%').justifyContent(FlexAlign.Center)
          .backgroundColor($r('sys.color.comp_background_primary'))
      } else {
        Column() {
          if (this.progress < 100) {
            Progress({ value: this.progress, total: 100, type: ProgressType.Linear })
              .width('100%').style({ strokeWidth: 2 }).hitTestBehavior(HitTestMode.None)
          }
          if (!this.clean) {
            Row({ space: 8 }) {
              Button('搜索地图').onClick(() => void this.search())
              Button('地图工具').enabled(this.ready).onClick(() => {
                this.sheet = 'tools';
                this.sheetOpen = true;
              })
            }.width('100%').padding({ top: 12, left: 16, right: 16 }).hitTestBehavior(HitTestMode.Transparent)
            Blank().hitTestBehavior(HitTestMode.None)
            Column({ space: 8 }) {
              Button('＋').width(48).height(48).accessibilityText('放大')
                .enabled(this.ready).onClick(() => void this.mapAction('.mcwws-ctrl-zoom-in'))
              Button('−').width(48).height(48).accessibilityText('缩小')
                .enabled(this.ready).onClick(() => void this.mapAction('.mcwws-ctrl-zoom-out'))
            }.padding({ right: 16, bottom: 12 }).alignSelf(ItemAlign.End)
          } else {
            Button('显示地图工具').margin(12).onClick(() => this.clean = false)
          }
        }.width('100%').height('100%').hitTestBehavior(HitTestMode.Transparent)
      }
    }
    .width('100%').height('100%').padding({ bottom: BOTTOM_INSET })
    .bindSheet($$this.sheetOpen, this.sheetContent(), { height: SheetSize.LARGE, dragBar: true, showClose: true })
  }
}
