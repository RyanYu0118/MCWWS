import { webview } from '@kit.ArkWeb';
import { MAP_ORIGIN } from '../services/Api';
import { SERVICE_CARD_COLOR } from './NativeUi';

const CACHE_DOCUMENT: string = MAP_ORIGIN + '/.mcwws-native-cache';

@Component
export struct MapCachePanel {
  @State tiles: number = -1;
  @State busy: boolean = false;
  @State error: string = '';
  private controller: webview.WebviewController = new webview.WebviewController();
  private open: boolean = true;

  aboutToDisappear(): void {
    this.open = false;
  }

  private cacheDocument(event: OnInterceptRequestEvent): WebResourceResponse {
    const isCacheDocument = event.request.getRequestUrl() === CACHE_DOCUMENT;
    // Give the local page the map's storage origin without a network request.
    const response = new WebResourceResponse();
    response.setResponseData(isCacheDocument ? '<!doctype html><meta charset="utf-8">' : '');
    response.setResponseMimeType('text/html');
    response.setResponseEncoding('utf-8');
    response.setResponseCode(isCacheDocument ? 200 : 404);
    response.setReasonMessage(isCacheDocument ? 'OK' : 'Not Found');
    response.setResponseHeader([{ headerKey: 'Cache-Control', headerValue: 'no-store' }]);
    return response;
  }

  private async updateCache(clear: boolean): Promise<void> {
    if (this.busy || !this.open) return;
    this.busy = true;
    this.error = '';
    try {
      const started = await this.controller.runJavaScript(`
        (() => {
          if (window.origin !== ${JSON.stringify(MAP_ORIGIN)} || typeof caches === 'undefined') return false;
          window.mcwwsNativeTileCount = null;
          const clear = ${clear};
          void (async () => {
            const names = (await caches.keys()).filter(name => name.startsWith('mcwws-map-tiles-'));
            if (clear) {
              await Promise.all(names.map(name => caches.delete(name)));
              return 0;
            }
            const counts = await Promise.all(names.map(async name => (await (await caches.open(name)).keys()).length));
            return counts.reduce((total, count) => total + count, 0);
          })().then(count => { window.mcwwsNativeTileCount = count; }, () => { window.mcwwsNativeTileCount = -1; });
          return true;
        })()
      `);
      if (started !== 'true') throw new Error('Cache Storage unavailable');
      for (let attempt = 0; attempt < 40 && this.open; attempt++) {
        const result = await this.controller.runJavaScript('window.mcwwsNativeTileCount');
        if (result === '-1') break;
        if (/^\d+$/.test(result)) {
          this.tiles = Number(result);
          return;
        }
        await new Promise<void>(resolve => { setTimeout(resolve, 150); });
      }
    } catch {
      // The Web component can detach when leaving settings.
    } finally {
      if (this.open) this.busy = false;
    }
    if (this.open) this.error = clear ? '无法清除地图瓦片缓存' : '无法读取地图瓦片缓存';
  }

  private confirmClear(): void {
    this.getUIContext().getPromptAction().showDialog({
      title: '清除地图瓦片缓存',
      message: '下次查看这些区域时会重新加载瓦片。',
      buttons: [{ text: '取消', color: '#FFFFFF' }, { text: '清除', color: '#FFB6B1' }]
    }).then(answer => {
      if (answer.index === 1) void this.updateCache(true);
    }).catch(() => {});
  }

  build() {
    Column({ space: 10 }) {
      Text('地图缓存').fontSize(19).fontWeight(FontWeight.Medium)
      if (this.busy) {
        Text('正在处理瓦片缓存').fontSize(14)
      } else if (this.error) {
        Text(this.error).fontSize(14)
      } else if (this.tiles >= 0) {
        Text('已缓存 ' + this.tiles + ' 张瓦片').fontSize(15)
      }
      Text('离线时可查看已缓存区域，新区域仍需联网。')
        .fontSize(13).fontColor($r('sys.color.font_secondary'))
      Row({ space: 8 }) {
        Button('刷新统计', { buttonStyle: ButtonStyleMode.TEXTUAL })
          .enabled(!this.busy).onClick(() => void this.updateCache(false))
        Button('清除瓦片缓存', { buttonStyle: ButtonStyleMode.TEXTUAL })
          .enabled(!this.busy && this.tiles > 0).onClick(() => this.confirmClear())
      }
      Web({ src: CACHE_DOCUMENT, controller: this.controller })
        .width(1).height(1).opacity(0).accessibilityLevel('no')
        .hitTestBehavior(HitTestMode.None).javaScriptAccess(true).domStorageAccess(true).fileAccess(false)
        .onInterceptRequest(event => this.cacheDocument(event))
        .onPageEnd(() => void this.updateCache(false))
    }.width('100%').alignItems(HorizontalAlign.Start)
      .padding(16).backgroundColor(SERVICE_CARD_COLOR).borderRadius(20)
  }
}
