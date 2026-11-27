import { webview } from '@kit.ArkWeb';
import { ORIGIN } from './Api';

export const SHOP_BROWSER_URL: string = ORIGIN + '/native-shop.html?v=1';

export interface ItemPreview {
  source: string;
  mirror?: boolean;
  smooth?: boolean;
}

interface PreviewRequest {
  itemId: string;
  timer: number;
  resolve: (preview: ItemPreview | null) => void;
}

export class ShopBrowser {
  readonly controller: webview.WebviewController = new webview.WebviewController();
  private previews: Map<string, ItemPreview> = new Map();
  private waiting: Map<number, PreviewRequest> = new Map();
  private inflight: Map<string, Promise<ItemPreview | null>> = new Map();
  private ticket: number = 0;

  run(call: string): Promise<string> {
    return this.controller.runJavaScript(
      'location.href === ' + JSON.stringify(SHOP_BROWSER_URL) + ' && ' +
      'typeof window.MCWWSNativeShopPage === "object" && (' + call + ')');
  }

  preview(itemId: string): Promise<ItemPreview | null> {
    const cached = this.previews.get(itemId);
    if (cached) return Promise.resolve(cached);
    const pending = this.inflight.get(itemId);
    if (pending) return pending;
    const ticket = ++this.ticket;
    const promise = new Promise<ItemPreview | null>(resolve => {
      const timer = setTimeout(() => this.finish(ticket, null), 18000);
      this.waiting.set(ticket, { itemId, timer, resolve });
      this.run('window.MCWWSNativeShopPage.preview(' + ticket + ',' + JSON.stringify(itemId) + ')')
        .then(result => { if (result !== 'true') this.finish(ticket, null); })
        .catch(() => this.finish(ticket, null));
    });
    this.inflight.set(itemId, promise);
    return promise;
  }

  reset(): void {
    this.waiting.forEach((_, ticket) => this.finish(ticket, null));
  }

  previewReady(ticket: number, json: string): void {
    if (!this.waiting.has(ticket) || typeof json !== 'string' || json.length > 600000) return;
    try {
      const result = JSON.parse(json) as ItemPreview | null;
      if (result && typeof result.source === 'string' &&
        (result.source.startsWith('data:image/png;base64,') || result.source.startsWith(ORIGIN + '/') ||
          /^https:\/\/(?:[a-z0-9-]+\.)?minecraft\.wiki\//i.test(result.source))) {
        this.finish(ticket, result);
      } else {
        this.finish(ticket, null);
      }
    } catch {
      this.finish(ticket, null);
    }
  }

  private finish(ticket: number, preview: ItemPreview | null): void {
    const pending = this.waiting.get(ticket);
    if (!pending) return;
    clearTimeout(pending.timer);
    this.waiting.delete(ticket);
    this.inflight.delete(pending.itemId);
    if (preview) {
      const oldest = this.previews.keys().next();
      if (this.previews.size >= 160 && !oldest.done) this.previews.delete(oldest.value);
      this.previews.set(pending.itemId, preview);
    }
    pending.resolve(preview);
  }
}
