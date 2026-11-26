import { webview } from '@kit.ArkWeb';
import { uiMaterial } from '@kit.ArkUI';

const SERVICE_ORIGIN: string = 'https://mcs.ryanstudio.work';
const DOCK_CLEARANCE: number = 76;

@Component
struct ServiceWeb {
  @Prop path: string = '';
  controller: webview.WebviewController = new webview.WebviewController();
  @State progress: number = 0;
  @State failed: boolean = false;

  build() {
    Stack({ alignContent: Alignment.Top }) {
      Web({ src: SERVICE_ORIGIN + this.path, controller: this.controller })
        .width('100%')
        .height('100%')
        .javaScriptAccess(true)
        .domStorageAccess(true)
        .fileAccess(false)
        .cacheMode(CacheMode.Default)
        .backgroundColor('#1D2B26')
        .onPageBegin(() => {
          this.progress = 0;
          this.failed = false;
        })
        .onProgressChange(event => {
          if (event) {
            this.progress = event.newProgress;
          }
        })
        .onErrorReceive(event => {
          if (event?.request.isMainFrame()) {
            this.failed = true;
          }
        })

      if (this.failed) {
        Column({ space: 14 }) {
          Text('页面无法加载')
            .fontSize(22)
            .fontWeight(FontWeight.Medium)
            .fontColor($r('sys.color.font_primary'))
          Text('请检查网络连接后重试。')
            .fontSize(14)
            .fontColor($r('sys.color.font_secondary'))
          Button('重新加载')
            .onClick(() => {
              this.failed = false;
              this.progress = 0;
              this.controller.refresh();
            })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor($r('sys.color.comp_background_primary'))
      } else if (this.progress < 100) {
        Progress({ value: this.progress, total: 100, type: ProgressType.Linear })
          .width('100%')
          .style({ strokeWidth: 2 })
          .hitTestBehavior(HitTestMode.None)
      }
    }
    .width('100%')
    .height('100%')
    .padding({ bottom: DOCK_CLEARANCE })
  }
}

@Entry
@Component
struct Index {
  private tabs: TabsController = new TabsController();
  private shop: webview.WebviewController = new webview.WebviewController();
  private map: webview.WebviewController = new webview.WebviewController();
  private manage: webview.WebviewController = new webview.WebviewController();
  private account: webview.WebviewController = new webview.WebviewController();
  @State selectedTab: number = 0;

  onBackPressed(): boolean {
    switch (this.selectedTab) {
      case 1:
        return this.webBack(this.shop);
      case 2:
        return this.webBack(this.map);
      case 3:
        return this.webBack(this.manage);
      case 4:
        return this.webBack(this.account);
      default:
        return false;
    }
  }

  private webBack(controller: webview.WebviewController): boolean {
    try {
      if (controller.accessBackward()) {
        controller.backward();
        return true;
      }
    } catch {
      // The Web component may still be attaching its controller.
    }
    this.tabs.changeIndex(0);
    return true;
  }

  @Builder
  home() {
    Stack() {
      Image($r('app.media.world_night'))
        .width('100%')
        .height('100%')
        .objectFit(ImageFit.Cover)
        .accessibilityLevel('no')

      Column()
        .width('100%')
        .height('100%')
        .linearGradient({
          direction: GradientDirection.Bottom,
          colors: [['#AA081210', 0], ['#18081210', 0.55], ['#87081210', 1]]
        })
        .hitTestBehavior(HitTestMode.None)

      Column() {
        Text('流浪世界服务')
          .fontColor(Color.White)
          .fontSize(36)
          .fontWeight(FontWeight.Bold)
        Button('打开地图')
          .margin({ top: 22 })
          .height(48)
          .padding({ left: 24, right: 24 })
          .fontColor(Color.White)
          .backgroundColor('#1D2B26')
          .onClick(() => this.tabs.changeIndex(2))
      }
      .width('100%')
      .height('100%')
      .padding({ top: 32, left: 24, right: 24, bottom: DOCK_CLEARANCE })
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    Tabs({ controller: this.tabs, barPosition: BarPosition.End }) {
      TabContent() {
        this.home()
      }.tabBar(BottomTabBarStyle.of($r('app.media.nav_home'), '主页'))
      TabContent() {
        ServiceWeb({ path: '/items.html', controller: this.shop })
      }.tabBar(BottomTabBarStyle.of($r('app.media.nav_shop'), '商店'))
      TabContent() {
        ServiceWeb({ path: '/map.html', controller: this.map })
      }.tabBar(BottomTabBarStyle.of($r('app.media.nav_map'), '地图'))
      TabContent() {
        ServiceWeb({ path: '/manage/shop-locations.html', controller: this.manage })
      }.tabBar(BottomTabBarStyle.of($r('app.media.nav_manage'), '管理'))
      TabContent() {
        ServiceWeb({ path: '/ledger.html', controller: this.account })
      }.tabBar(BottomTabBarStyle.of($r('app.media.nav_account'), '账户'))
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#1D2B26')
    .scrollable(false)
    .animationDuration(320)
    .barOverlap(true)
    .barFloatingStyle({
      adaptToHandedness: true,
      systemMaterial: new uiMaterial.ImmersiveMaterial({
        style: uiMaterial.ImmersiveStyle.THIN,
        applyShadow: true,
        interactive: true
      })
    })
    .onChange(index => {
      this.selectedTab = index;
    })
  }
}
