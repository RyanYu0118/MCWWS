import { dateText, money } from '../services/Api';
import { SERVICE_CARD_COLOR } from './NativeUi';

export interface PricePoint {
  timestamp: string;
  avgBuyPrice: number;
  avgSellPrice: number;
}

export interface ChartPoint extends PricePoint {
  time: number;
}

interface RangeOption {
  label: string;
  duration: number;
}

const DAY: number = 86400000;
const RANGES: RangeOption[] = [
  { label: '24小时', duration: DAY }, { label: '7天', duration: 7 * DAY },
  { label: '30天', duration: 30 * DAY }, { label: '全部', duration: 0 }
];
const BUY_COLOR: string = '#A0CEAA';
const SELL_COLOR: string = '#FFAB9E';
const PAD: number = 10;
const MIN_ZOOM_SPAN: number = 600000;

@Component
export struct PriceChart {
  @Prop @Watch('reset') points: ChartPoint[] = [];
  @State rangeIndex: number = 1;
  @State selected: number = -1;
  @State left: number = 0;
  @State span: number = 0;
  private initialLeft: number = 0;
  private initialSpan: number = 0;
  private canvas: CanvasRenderingContext2D = new CanvasRenderingContext2D(new RenderingContextSettings(true));

  aboutToAppear(): void {
    this.reset();
  }

  reset(): void {
    this.selectRange(1);
  }

  private firstTime(): number {
    return this.points[0]?.time ?? 0;
  }

  private fullSpan(): number {
    return Math.max(1, (this.points[this.points.length - 1]?.time ?? 0) - this.firstTime());
  }

  private selectRange(index: number): void {
    this.rangeIndex = index;
    const duration = RANGES[index].duration;
    this.span = duration ? Math.min(duration, this.fullSpan()) : this.fullSpan();
    this.left = this.firstTime() + this.fullSpan() - this.span;
    this.selected = -1;
    this.draw();
  }

  private setViewport(left: number, span: number): void {
    this.span = this.clampSpan(span);
    this.left = Math.max(this.firstTime(), Math.min(left, this.firstTime() + this.fullSpan() - this.span));
    this.rangeIndex = -1;
    this.selected = -1;
    this.draw();
  }

  private clampSpan(span: number): number {
    const full = this.fullSpan();
    return Math.max(Math.min(MIN_ZOOM_SPAN, full), Math.min(span, full));
  }

  private axisTime(time: number): string {
    const date = new Date(time);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return (date.getMonth() + 1) + '/' + date.getDate() + ' ' + hours + ':' + minutes;
  }

  private localRatio(x: number): number {
    return Math.max(0, Math.min(1, (x - PAD) / Math.max(1, this.canvas.width - 2 * PAD)));
  }

  private inspect(x: number): void {
    const time = this.left + this.span * this.localRatio(x);
    const nearest = this.lowerBound(time);
    const previous = Math.max(0, nearest - 1);
    this.selected = nearest === this.points.length ||
      time - this.points[previous].time < this.points[nearest].time - time ? previous : nearest;
    this.draw();
  }

  private lowerBound(time: number): number {
    let low = 0;
    let high = this.points.length;
    while (low < high) {
      const mid = (low + high) >>> 1;
      if (this.points[mid].time < time) low = mid + 1;
      else high = mid;
    }
    return low;
  }

  private positionX(point: ChartPoint, width: number): number {
    if (this.points.length === 1) return width / 2;
    return PAD + (point.time - this.left) / this.span * (width - 2 * PAD);
  }

  private draw(): void {
    const width = this.canvas.width;
    const height = this.canvas.height;
    if (width <= 2 * PAD || height <= 2 * PAD) return;
    this.canvas.clearRect(0, 0, width, height);
    if (!this.points.length || !this.span) return;

    const first = Math.max(0, this.lowerBound(this.left) - 1);
    const last = Math.min(this.points.length - 1, this.lowerBound(this.left + this.span));
    const visible = this.points.slice(first, last + 1);
    let min = Infinity;
    let max = -Infinity;
    for (const point of visible) {
      min = Math.min(min, point.avgBuyPrice, point.avgSellPrice);
      max = Math.max(max, point.avgBuyPrice, point.avgSellPrice);
    }
    const verticalPad = Math.max(0.01, (max - min) * 0.06);
    const yFor = (price: number): number =>
      height - PAD - (price - min + verticalPad) / (max - min + 2 * verticalPad) * (height - 2 * PAD);
    this.canvas.strokeStyle = '#41C6D8C9';
    this.canvas.lineWidth = 1;
    for (let row = 0; row < 4; row++) {
      const y = PAD + row * (height - 2 * PAD) / 3;
      this.canvas.beginPath();
      this.canvas.moveTo(PAD, y);
      this.canvas.lineTo(width - PAD, y);
      this.canvas.stroke();
    }
    this.canvas.save();
    this.canvas.beginPath();
    this.canvas.rect(PAD, 0, width - 2 * PAD, height);
    this.canvas.clip();
    this.plot(visible, width, yFor, BUY_COLOR, false);
    this.plot(visible, width, yFor, SELL_COLOR, true);
    const point = this.points[this.selected];
    if (point) {
      const x = Math.max(PAD, Math.min(width - PAD, this.positionX(point, width)));
      this.canvas.strokeStyle = '#CCFFFFFF';
      this.canvas.lineWidth = 1;
      this.canvas.beginPath();
      this.canvas.moveTo(x, PAD);
      this.canvas.lineTo(x, height - PAD);
      this.canvas.stroke();
      this.dot(x, yFor(point.avgBuyPrice), BUY_COLOR);
      this.dot(x, yFor(point.avgSellPrice), SELL_COLOR);
    }
    this.canvas.restore();
  }

  private dot(x: number, y: number, color: string): void {
    this.canvas.fillStyle = color;
    this.canvas.beginPath();
    this.canvas.arc(x, y, 4, 0, 2 * Math.PI);
    this.canvas.fill();
  }

  private plot(points: ChartPoint[], width: number, yFor: (price: number) => number, color: string, sell: boolean): void {
    this.canvas.strokeStyle = color;
    this.canvas.lineWidth = 2;
    this.canvas.beginPath();
    points.forEach((point, index) => {
      const x = this.positionX(point, width);
      const y = yFor(sell ? point.avgSellPrice : point.avgBuyPrice);
      if (index === 0) this.canvas.moveTo(x, y);
      else this.canvas.lineTo(x, y);
    });
    this.canvas.stroke();
    if (points.length === 1) {
      this.dot(this.positionX(points[0], width), yFor(sell ? points[0].avgSellPrice : points[0].avgBuyPrice), color);
    }
  }

  build() {
    Column({ space: 8 }) {
      Row({ space: 6 }) {
        ForEach(RANGES, (range: RangeOption, index: number) => {
          Button(range.label, { buttonStyle: ButtonStyleMode.TEXTUAL, type: ButtonType.Capsule })
            .height(40).layoutWeight(1).padding(0)
            .fontColor(this.rangeIndex === index ? '#15271B' : Color.White)
            .backgroundColor(this.rangeIndex === index ? BUY_COLOR : SERVICE_CARD_COLOR)
            .onClick(() => this.selectRange(index))
        }, (range: RangeOption) => range.label)
      }.width('100%')
      Canvas(this.canvas)
        .width('100%').height(158)
        .accessibilityText('价格曲线，点击查看价格，双指缩放，横向拖动')
        .onReady(() => this.draw())
        .gesture(GestureGroup(GestureMode.Exclusive,
          PinchGesture({ fingers: 2 })
            .onActionStart(() => {
              this.initialLeft = this.left;
              this.initialSpan = this.span;
            })
            .onActionUpdate(event => {
              if (event.scale <= 0) return;
              const ratio = this.localRatio(event.pinchCenterX);
              const span = this.clampSpan(this.initialSpan / event.scale);
              this.setViewport(this.initialLeft + (this.initialSpan - span) * ratio, span);
            }),
          PanGesture({ direction: PanDirection.Horizontal, distance: 8 })
            .onActionStart(() => { this.initialLeft = this.left; })
            .onActionUpdate(event => {
              if (this.span < this.fullSpan()) {
                this.setViewport(this.initialLeft - event.offsetX * this.span / Math.max(1, this.canvas.width - 2 * PAD),
                  this.span);
              } else if (event.fingerList[0]) {
                this.inspect(event.fingerList[0].localX);
              }
            }),
          TapGesture().onAction(event => {
            if (event.fingerList[0]) this.inspect(event.fingerList[0].localX);
          })
        ))
      Row() {
        Text(this.axisTime(this.left)).fontSize(11).fontColor('#B6C5BA')
        Blank()
        Text(this.axisTime(this.left + this.span)).fontSize(11).fontColor('#B6C5BA')
      }.width('100%').padding({ left: PAD, right: PAD })
      Text(this.points[this.selected] ? dateText(this.points[this.selected].timestamp) : '轻点查价 · 双指缩放 · 横拖浏览')
        .fontSize(13).width('100%').fontColor('#D9E3DB')
      Row({ space: 12 }) {
        Text('买入' + (this.selected >= 0 ? ' ' + money(this.points[this.selected].avgBuyPrice) : ''))
          .fontSize(14).fontColor(BUY_COLOR)
        Text('卖出' + (this.selected >= 0 ? ' ' + money(this.points[this.selected].avgSellPrice) : ''))
          .fontSize(14).fontColor(SELL_COLOR)
      }.width('100%')
    }.width('100%').padding(12).borderRadius(16).backgroundColor(SERVICE_CARD_COLOR)
  }
}
