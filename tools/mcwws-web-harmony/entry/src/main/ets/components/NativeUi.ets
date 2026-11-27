@Component
export struct ScreenHeader {
  @Prop title: string = '';
  @Prop caption: string = '';

  build() {
    Column({ space: 4 }) {
      Text(this.title)
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor($r('sys.color.font_primary'))
      if (this.caption) {
        Text(this.caption)
          .fontSize(13)
          .fontColor($r('sys.color.font_secondary'))
      }
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    .padding({ left: 20, right: 20, top: 16, bottom: 12 })
  }
}

@Component
export struct LoadState {
  @Prop loading: boolean = false;
  @Prop message: string = '';
  @Prop actionLabel: string = '';
  onAction: () => void = () => {};

  build() {
    Column({ space: 12 }) {
      if (this.loading) {
        LoadingProgress().width(32).height(32)
      }
      Text(this.loading ? '正在加载' : this.message)
        .fontSize(15)
        .textAlign(TextAlign.Center)
        .fontColor($r('sys.color.font_secondary'))
      if (!this.loading && this.actionLabel) {
        Button(this.actionLabel)
          .onClick(() => this.onAction())
      }
    }
    .width('100%')
    .padding(24)
  }
}

export const BOTTOM_INSET: number = 76;
export const SERVICE_CARD_COLOR: string = '#84212D27';
export const SERVICE_SHEET_COLOR: string = '#EF202C26';
