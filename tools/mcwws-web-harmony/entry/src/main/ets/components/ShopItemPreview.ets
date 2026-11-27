import { ItemPreview } from '../services/ShopBrowser';

@Component
export struct ShopItemPreview {
  @Prop @Watch('load') itemId: string = '';
  @Prop @Watch('load') ready: boolean = false;
  @Prop iconSize: number = 46;
  requestPreview: (itemId: string) => Promise<ItemPreview | null> = () => Promise.resolve(null);
  @State preview: ItemPreview | null = null;
  private version: number = 0;

  aboutToAppear(): void {
    this.load();
  }

  aboutToDisappear(): void {
    this.version++;
  }

  load(): void {
    const version = ++this.version;
    this.preview = null;
    if (!this.ready || !this.itemId) return;
    void this.requestPreview(this.itemId).then(result => {
      if (version === this.version) this.preview = result;
    });
  }

  build() {
    Stack() {
      if (this.preview) {
        Image(this.preview.source)
          .width('100%').height('100%').objectFit(ImageFit.Contain)
          .interpolation(this.preview.smooth ? ImageInterpolation.Medium : ImageInterpolation.None)
          .scale({ x: this.preview.mirror ? -1 : 1 })
          .onError(() => this.preview = null)
      }
    }.width(this.iconSize).height(this.iconSize)
      .backgroundColor('#16FFFFFF').borderRadius(10)
      .accessibilityLevel('no')
  }
}
